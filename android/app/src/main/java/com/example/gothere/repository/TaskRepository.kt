// File: GoThere/android/app/src/main/java/com/example/gothere/repository/TaskRepository.kt
package com.example.gothere.repository

import android.content.Context
import android.util.Log
import com.example.gothere.model.Link
import com.example.gothere.model.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

class TaskRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun colFor(uid: String): CollectionReference =
        db.collection("users").document(uid).collection("tasks")

    /**
     * Emits UID whenever auth changes (including null).
     * Required so tasks load after login without restart.
     */
    private fun authUidFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val uid = fa.currentUser?.uid
            Log.d("AuthCheck", "TaskRepository auth changed -> uid=$uid")
            trySend(uid).isSuccess
        }
        auth.addAuthStateListener(listener)

        // Emit immediately
        trySend(auth.currentUser?.uid).isSuccess

        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    /**
     * Public flow used by ViewModels.
     * - Logged out -> builtInSeed()
     * - Logged in  -> Firestore listener
     */
    fun tasksFlow(): Flow<List<Task>> =
        authUidFlow().flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                Log.w("TaskRepository", "tasksFlow: uid=null -> returning builtInSeed()")
                flowOf(builtInSeed())
            } else {
                tasksFromFirestore(uid)
            }
        }

    private fun tasksFromFirestore(uid: String): Flow<List<Task>> = callbackFlow {
        val collection = colFor(uid)
        Log.d("TaskRepository", "Subscribing to Firestore path users/$uid/tasks")

        val reg = collection.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("FirestoreCheck", "users/$uid/tasks listener error -> falling back to seed", err)
                trySend(builtInSeed())
                return@addSnapshotListener
            }

            val docs = snap?.documents.orEmpty()
            if (docs.isEmpty()) {
                Log.w("TaskRepository", "users/$uid/tasks snapshot empty -> showing seed (temporary behavior)")
                trySend(builtInSeed())
                return@addSnapshotListener
            }

            val items = docs.mapNotNull { doc ->
                try {
                    val title =
                        firstNonBlank(
                            doc.getString("title"),
                            doc.getString("name"),
                            doc.getString("taskTitle"),
                            doc.getString("task")
                        ) ?: "(Untitled task)"

                    val description =
                        firstNonBlank(
                            doc.getString("description"),
                            doc.getString("details")
                        )

                    val category =
                        firstNonBlank(
                            doc.getString("category"),
                            doc.getString("phase"),
                            doc.getString("group")
                        ) ?: "General"

                    val completed = doc.getBoolean("completed") ?: false

                    // Read countryId - default to "spain" for legacy tasks
                    val countryId = doc.getString("countryId") ?: "spain"

                    val createdAt = when (val v = doc.get("createdAt")) {
                        is Number -> v.toLong()
                        is Timestamp -> v.toDate().time
                        else -> null
                    }

                    val dueAt = when (val v = doc.get("dueAt")) {
                        is Number -> v.toLong()
                        is Timestamp -> v.toDate().time
                        else -> null
                    }

                    val linksField = doc.get("links")
                    val links: List<Link>? = (linksField as? List<*>)?.mapNotNull { raw ->
                        val m = raw as? Map<*, *> ?: return@mapNotNull null
                        val label = (m["label"] as? String)?.trim().orEmpty()
                        val url = (m["url"] as? String)?.trim().orEmpty()
                        if (label.isNotEmpty() && url.isNotEmpty()) Link(label, url) else null
                    }

                    val notes = doc.getString("notes")

                    Task(
                        id = doc.id,
                        title = title,
                        description = description,
                        category = category,
                        completed = completed,
                        dueAt = dueAt,
                        createdAt = createdAt,
                        countryId = countryId,
                        links = links,
                        notes = notes
                    )
                } catch (t: Throwable) {
                    Log.e("TaskRepository", "Failed mapping task doc id=${doc.id}", t)
                    null
                }
            }

            Log.d("TaskRepository", "SNAPSHOT users/$uid/tasks docs=${docs.size} mapped=${items.size}")
            trySend(if (items.isEmpty()) builtInSeed() else items)
        }

        awaitClose {
            Log.d("TaskRepository", "Unsubscribing Firestore listener users/$uid/tasks")
            reg.remove()
        }
    }

    // ----------------------------
    // Writes
    // ----------------------------

    suspend fun addTask(task: Task): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val data = hashMapOf<String, Any?>(
                "title" to task.title,
                "description" to task.description,
                "category" to task.category,
                "completed" to false,
                "dueAt" to task.dueAt,
                "createdAt" to System.currentTimeMillis(),
                "countryId" to task.countryId,
            )
            colFor(uid).document().set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCompleted(taskId: String, completed: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        return try {
            colFor(uid).document(taskId).update("completed", completed).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setNotes(taskId: String, notes: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        return try {
            colFor(uid).document(taskId).update("notes", notes).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDue(taskId: String, dueAtMillisUtc: Long?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        return try {
            colFor(uid).document(taskId).update("dueAt", dueAtMillisUtc).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Needed by Decision Tree shim.
     * Adds tasks in one batch.
     */
    suspend fun insertTasks(tasks: List<Task>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val col = colFor(uid)
            val batch = db.batch()
            tasks.forEach { t ->
                val doc = col.document()
                batch.set(doc, toFirestoreMap(t))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedIfEmpty(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val col = colFor(uid)
            val existing = col.limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)

            val batch = db.batch()
            builtInSeed().forEach { task ->
                val doc = col.document()
                batch.set(doc, toFirestoreMap(task))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads assets/tasks_seed.json and inserts missing tasks.
     * Safe to run multiple times (idempotent).
     *
     * Returns Pair(added, skipped).
     *
     * Asset path expected:
     * GoThere/android/app/src/main/assets/tasks_seed.json
     */
    suspend fun importSeedFromAssetsIfMissing(context: Context): Result<Pair<Int, Int>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))

        return try {
            val json = context.assets.open("tasks_seed.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)

            fun keyOf(title: String, category: String, countryId: String): String {
                val t = title.trim()
                val c = category.trim().ifBlank { "General" }
                return "$countryId|$c|$t"
            }

            // Prefer "seedKey" if present; fall back to countryId|category|title if not.
            val existingDocs = colFor(uid).get().await().documents
            val existingKeys = existingDocs.mapNotNull { d ->
                val seedKey = d.getString("seedKey")?.trim()
                if (!seedKey.isNullOrBlank()) return@mapNotNull seedKey
                val t = d.getString("title")?.trim()
                val c = d.getString("category")?.trim()
                val country = d.getString("countryId")?.trim() ?: "spain"
                if (!t.isNullOrBlank()) keyOf(t, c ?: "General", country) else null
            }.toHashSet()

            var added = 0
            var skipped = 0

            val batch = db.batch()

            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue

                val title = o.optString("title").trim()
                if (title.isBlank()) continue

                val category = o.optString("category").trim().ifBlank { "General" }
                // Read countryId from seed JSON, default to "spain"
                val countryId = o.optString("countryId").trim().ifBlank { "spain" }
                val seedKey = keyOf(title, category, countryId)

                if (existingKeys.contains(seedKey)) {
                    skipped++
                    continue
                }

                val links = o.optJSONArray("links")?.let { linksArr ->
                    (0 until linksArr.length()).mapNotNull { j ->
                        val lo = linksArr.optJSONObject(j) ?: return@mapNotNull null
                        val lab = lo.optString("label").trim()
                        val url = lo.optString("url").trim()
                        if (lab.isNotBlank() && url.isNotBlank()) Link(lab, url) else null
                    }.takeIf { it.isNotEmpty() }
                }

                val task = Task(
                    title = title,
                    description = o.optString("description").trim().takeIf { it.isNotBlank() },
                    category = category,
                    completed = o.optBoolean("completed", false),
                    countryId = countryId,
                    links = links,
                    // don't set dueAt from seed; leave null
                    dueAt = null,
                    // createdAt present to avoid nulls in Firestore map
                    createdAt = System.currentTimeMillis()
                )

                val doc = colFor(uid).document()
                val data = toFirestoreMap(task).toMutableMap()
                data["seedKey"] = seedKey

                batch.set(doc, data)
                existingKeys.add(seedKey)
                added++
            }

            if (added > 0) batch.commit().await()

            Log.e("SeedImport", "importSeedFromAssetsIfMissing uid=$uid added=$added skipped=$skipped totalSeed=${arr.length()}")
            Result.success(added to skipped)
        } catch (e: Exception) {
            Log.e("SeedImport", "importSeedFromAssetsIfMissing FAILED", e)
            Result.failure(e)
        }
    }

    /**
     * One-shot cleanup: removes duplicate task docs that share the same effective
     * seedKey. A few users had two parallel seed imports run on the first sign-in
     * (before the import-marker landed), and Firestore ended up with each Spain
     * task duplicated. The seed-key dedupe in importSeedFromAssetsIfMissing prevents
     * NEW duplicates but doesn't clean up existing ones.
     *
     * Keeps the doc with completed=true if any; otherwise the oldest createdAt.
     */
    suspend fun dedupeSeedTasks(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))

        return try {
            val docs = colFor(uid).get().await().documents

            fun effectiveKey(d: com.google.firebase.firestore.DocumentSnapshot): String? {
                val seedKey = d.getString("seedKey")?.trim()
                if (!seedKey.isNullOrBlank()) return seedKey
                val title = d.getString("title")?.trim() ?: return null
                if (title.isBlank()) return null
                val category = d.getString("category")?.trim()?.ifBlank { "General" } ?: "General"
                val country = d.getString("countryId")?.trim()?.ifBlank { "spain" } ?: "spain"
                return "$country|$category|$title"
            }

            val groups = docs.groupBy { effectiveKey(it) }
            var removed = 0
            val batch = db.batch()

            for ((key, group) in groups) {
                if (key == null || group.size <= 1) continue
                // Prefer to keep a completed doc (preserves user progress); else oldest.
                val keep = group.firstOrNull { it.getBoolean("completed") == true }
                    ?: group.minByOrNull { it.getLong("createdAt") ?: Long.MAX_VALUE }
                    ?: group.first()
                for (d in group) {
                    if (d.id == keep.id) continue
                    batch.delete(d.reference)
                    removed++
                }
            }

            if (removed > 0) batch.commit().await()
            Log.i("SeedImport", "dedupeSeedTasks uid=$uid removed=$removed total=${docs.size}")
            Result.success(removed)
        } catch (e: Exception) {
            Log.e("SeedImport", "dedupeSeedTasks FAILED", e)
            Result.failure(e)
        }
    }

    /**
     * Import tasks for a specific country from the appropriate seed file.
     * Asset path expected:
     * - spain: assets/tasks_seed.json (existing)
     * - portugal: assets/portugal_tasks.json
     * - mexico: assets/mexico_tasks.json
     */
    suspend fun importCountrySeedFromAssets(context: Context, countryId: String): Result<Pair<Int, Int>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))

        val seedFileName = when (countryId) {
            "spain" -> "tasks_seed.json"
            "portugal" -> "portugal_tasks.json"
            "mexico" -> "mexico_tasks.json"
            else -> return Result.failure(Exception("Unknown country: $countryId"))
        }

        return try {
            // Check if the file exists
            val assetFiles = context.assets.list("") ?: emptyArray()
            if (!assetFiles.contains(seedFileName)) {
                Log.w("SeedImport", "Seed file not found: $seedFileName")
                return Result.failure(Exception("Seed file not found: $seedFileName"))
            }

            val json = context.assets.open(seedFileName).bufferedReader().use { it.readText() }
            val arr = JSONArray(json)

            fun keyOf(title: String, category: String): String {
                val t = title.trim()
                val c = category.trim().ifBlank { "General" }
                return "$countryId|$c|$t"
            }

            // Get existing keys for this country
            val existingDocs = colFor(uid).get().await().documents
            val existingKeys = existingDocs.mapNotNull { d ->
                val docCountry = d.getString("countryId") ?: "spain"
                if (docCountry != countryId) return@mapNotNull null
                
                val seedKey = d.getString("seedKey")?.trim()
                if (!seedKey.isNullOrBlank()) return@mapNotNull seedKey
                val t = d.getString("title")?.trim()
                val c = d.getString("category")?.trim()
                if (!t.isNullOrBlank()) keyOf(t, c ?: "General") else null
            }.toHashSet()

            var added = 0
            var skipped = 0

            val batch = db.batch()

            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue

                val title = o.optString("title").trim()
                if (title.isBlank()) continue

                val category = o.optString("category").trim().ifBlank { "General" }
                val seedKey = keyOf(title, category)

                if (existingKeys.contains(seedKey)) {
                    skipped++
                    continue
                }

                val links = o.optJSONArray("links")?.let { linksArr ->
                    (0 until linksArr.length()).mapNotNull { j ->
                        val lo = linksArr.optJSONObject(j) ?: return@mapNotNull null
                        val lab = lo.optString("label").trim()
                        val url = lo.optString("url").trim()
                        if (lab.isNotBlank() && url.isNotBlank()) Link(lab, url) else null
                    }.takeIf { it.isNotEmpty() }
                }

                val task = Task(
                    title = title,
                    description = o.optString("description").trim().takeIf { it.isNotBlank() },
                    category = category,
                    completed = o.optBoolean("completed", false),
                    countryId = countryId,
                    links = links,
                    dueAt = null,
                    createdAt = System.currentTimeMillis()
                )

                val doc = colFor(uid).document()
                val data = toFirestoreMap(task).toMutableMap()
                data["seedKey"] = seedKey

                batch.set(doc, data)
                existingKeys.add(seedKey)
                added++
            }

            if (added > 0) batch.commit().await()

            Log.e("SeedImport", "importCountrySeedFromAssets uid=$uid country=$countryId added=$added skipped=$skipped totalSeed=${arr.length()}")
            Result.success(added to skipped)
        } catch (e: Exception) {
            Log.e("SeedImport", "importCountrySeedFromAssets FAILED for $countryId", e)
            Result.failure(e)
        }
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private fun toFirestoreMap(task: Task): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "title" to task.title,
            "completed" to task.completed,
            "category" to (task.category ?: "General"),
            "countryId" to (task.countryId ?: "spain"),
            "createdAt" to (task.createdAt ?: System.currentTimeMillis())
        )
        task.description?.let { data["description"] = it }
        task.dueAt?.let { data["dueAt"] = it }
        task.links?.let { list ->
            data["links"] = list.map { mapOf("label" to it.label, "url" to it.url) }
        }
        return data
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private fun builtInSeed(): List<Task> = listOf(
        Task(title = "Define relocation budget", category = "Phase 1: Research & Planning", countryId = "spain"),
        Task(title = "Research visa types", category = "Phase 1: Research & Planning", countryId = "spain"),
        Task(title = "Gather birth/marriage certificates", category = "Phase 2: Documentation", countryId = "spain"),
        Task(title = "Submit visa application", category = "Phase 2: Documentation", countryId = "spain"),
        Task(title = "Book temporary accommodation", category = "Phase 3: Arrival", countryId = "spain"),
        Task(title = "Apply for NIE/TIE", category = "Phase 3: Arrival", countryId = "spain"),
        Task(title = "Find long-term housing", category = "Phase 4: Settlement", countryId = "spain"),
        Task(title = "Open local bank account", category = "Phase 4: Settlement", countryId = "spain")
    )
}
