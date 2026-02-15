package com.example.gothere.repository

import android.content.Context
import android.util.Log
import com.example.gothere.data.DestinationConfig
import com.example.gothere.data.Phases
import com.example.gothere.model.DestinationTask
import com.example.gothere.model.Link
import com.example.gothere.model.UserDestination
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for managing user destinations, visa track selection, and destination-scoped seeding.
 * 
 * Firestore structure:
 * /users/{uid}
 *   - activeDestinationId: string
 * /users/{uid}/destinations/{destinationId}
 *   - UserDestination fields
 * /users/{uid}/destinations/{destinationId}/tasks/{taskId}
 *   - DestinationTask fields
 */
class DestinationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "DestinationRepository"
        private const val USERS = "users"
        private const val DESTINATIONS = "destinations"
        private const val TASKS = "tasks"
        private const val ACTIVE_DESTINATION_ID = "activeDestinationId"
    }

    // ==================== USER DOCUMENT ====================

    private fun userDoc() = db.collection(USERS).document(uid())

    private fun uid(): String = auth.currentUser?.uid 
        ?: throw IllegalStateException("Not signed in")

    // ==================== ACTIVE DESTINATION ====================

    /**
     * Get the user's active destination ID.
     * Returns "spain" as default if not set.
     */
    suspend fun getActiveDestinationId(): String {
        return try {
            val doc = userDoc().get().await()
            doc.getString(ACTIVE_DESTINATION_ID) ?: DestinationConfig.SPAIN
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active destination", e)
            DestinationConfig.SPAIN
        }
    }

    /**
     * Set the user's active destination and ensure it's seeded.
     */
    suspend fun setActiveDestination(destinationId: String, context: Context): Result<Unit> {
        return try {
            // Update user document
            userDoc().set(
                mapOf(ACTIVE_DESTINATION_ID to destinationId),
                SetOptions.merge()
            ).await()

            // Ensure destination exists and is seeded
            ensureDestinationSeeded(destinationId, context)

            Log.d(TAG, "Set active destination to $destinationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set active destination", e)
            Result.failure(e)
        }
    }

    /**
     * Flow of active destination ID changes
     */
    fun activeDestinationFlow(): Flow<String> = callbackFlow {
        val listener = userDoc().addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(DestinationConfig.SPAIN)
                return@addSnapshotListener
            }
            val destId = snapshot?.getString(ACTIVE_DESTINATION_ID) ?: DestinationConfig.SPAIN
            trySend(destId)
        }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    // ==================== DESTINATION DOCUMENTS ====================

    private fun destinationDoc(destinationId: String) = 
        userDoc().collection(DESTINATIONS).document(destinationId)

    private fun tasksCollection(destinationId: String) = 
        destinationDoc(destinationId).collection(TASKS)

    /**
     * Get or create the UserDestination document
     */
    suspend fun getOrCreateDestination(destinationId: String): UserDestination {
        val doc = destinationDoc(destinationId).get().await()
        
        return if (doc.exists()) {
            doc.toObject(UserDestination::class.java) ?: createDestinationDoc(destinationId)
        } else {
            createDestinationDoc(destinationId)
        }
    }

    private suspend fun createDestinationDoc(destinationId: String): UserDestination {
        val config = DestinationConfig.getDestination(destinationId)
            ?: throw IllegalArgumentException("Unknown destination: $destinationId")

        val userDest = UserDestination(
            destinationId = destinationId,
            countryName = config.name,
            activeVisaTrackId = config.defaultVisaTrackId,
            createdAt = System.currentTimeMillis(),
            seedVersion = 0 // Will be updated when seeded
        )

        destinationDoc(destinationId).set(userDest).await()
        Log.d(TAG, "Created destination doc for $destinationId")
        return userDest
    }

    /**
     * Update the active visa track for a destination
     */
    suspend fun setActiveVisaTrack(destinationId: String, visaTrackId: String): Result<Unit> {
        return try {
            destinationDoc(destinationId).update("activeVisaTrackId", visaTrackId).await()
            Log.d(TAG, "Set active visa track to $visaTrackId for $destinationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set active visa track", e)
            Result.failure(e)
        }
    }

    /**
     * Get the active visa track ID for a destination
     */
    suspend fun getActiveVisaTrackId(destinationId: String): String {
        return try {
            val doc = destinationDoc(destinationId).get().await()
            doc.getString("activeVisaTrackId") 
                ?: DestinationConfig.getDestination(destinationId)?.defaultVisaTrackId
                ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active visa track", e)
            DestinationConfig.getDestination(destinationId)?.defaultVisaTrackId ?: ""
        }
    }

    // ==================== SEEDING ====================

    /**
     * Ensure destination tasks are seeded.
     * Only seeds if seedVersion is outdated or destination doesn't exist.
     */
    suspend fun ensureDestinationSeeded(destinationId: String, context: Context): Result<Unit> {
        return try {
            val userDest = getOrCreateDestination(destinationId)
            val config = DestinationConfig.getDestination(destinationId)
                ?: return Result.failure(IllegalArgumentException("Unknown destination: $destinationId"))

            if (userDest.seedVersion >= config.seedVersion) {
                Log.d(TAG, "Destination $destinationId already seeded at version ${userDest.seedVersion}")
                return Result.success(Unit)
            }

            Log.d(TAG, "Seeding $destinationId from version ${userDest.seedVersion} to ${config.seedVersion}")
            
            // Import seed data from assets
            val result = importSeedFromAssets(context, destinationId)
            
            if (result.isSuccess) {
                // Update seed version
                destinationDoc(destinationId).update(
                    mapOf(
                        "seedVersion" to config.seedVersion,
                        "lastSeededAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            result.map { }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed destination $destinationId", e)
            Result.failure(e)
        }
    }

    /**
     * Import seed tasks from assets JSON file.
     * Uses seedKey for deduplication - only inserts missing tasks.
     */
    private suspend fun importSeedFromAssets(
        context: Context,
        destinationId: String
    ): Result<Pair<Int, Int>> {
        return try {
            val fileName = "seeds/${destinationId}_tasks.json"
            
            // Try to read from assets
            val json = try {
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.w(TAG, "No seed file found at $fileName, using empty seed")
                return Result.success(0 to 0)
            }

            val arr = JSONArray(json)
            val tasksCol = tasksCollection(destinationId)

            // Get existing seed keys
            val existingDocs = tasksCol.get().await().documents
            val existingKeys = existingDocs.mapNotNull { it.getString("seedKey") }.toHashSet()

            var added = 0
            var skipped = 0
            val batch = db.batch()

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                
                val title = obj.optString("title").trim()
                if (title.isBlank()) continue

                val visaTrackId = obj.optString("visaTrackId", "").ifBlank { 
                    DestinationConfig.getDestination(destinationId)?.defaultVisaTrackId ?: ""
                }
                val phaseId = obj.optString("phaseId", "").ifBlank {
                    Phases.categoryToPhaseId(obj.optString("category"))
                }

                val seedKey = DestinationTask.generateSeedKey(
                    destinationId = destinationId,
                    visaTrackId = visaTrackId,
                    phaseId = phaseId,
                    title = title
                )

                if (existingKeys.contains(seedKey)) {
                    skipped++
                    continue
                }

                // Parse links
                val links = obj.optJSONArray("links")?.let { linksArr ->
                    (0 until linksArr.length()).mapNotNull { j ->
                        val linkObj = linksArr.optJSONObject(j) ?: return@mapNotNull null
                        val label = linkObj.optString("label").trim()
                        val url = linkObj.optString("url").trim()
                        if (label.isNotBlank() && url.isNotBlank()) Link(label, url) else null
                    }
                }

                val task = mapOf(
                    "destinationId" to destinationId,
                    "visaTrackId" to visaTrackId,
                    "phaseId" to phaseId,
                    "title" to title,
                    "description" to obj.optString("description").trim().ifBlank { null },
                    "category" to Phases.getDisplayName(phaseId), // For backward compatibility
                    "completed" to false,
                    "seeded" to true,
                    "seedKey" to seedKey,
                    "createdAt" to System.currentTimeMillis(),
                    "order" to obj.optInt("order", i),
                    "links" to links?.map { mapOf("label" to it.label, "url" to it.url) }
                )

                val docRef = tasksCol.document()
                batch.set(docRef, task.filterValues { it != null })
                existingKeys.add(seedKey)
                added++
            }

            if (added > 0) {
                batch.commit().await()
            }

            Log.d(TAG, "Seed import for $destinationId: added=$added, skipped=$skipped")
            Result.success(added to skipped)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import seed for $destinationId", e)
            Result.failure(e)
        }
    }

    // ==================== TASK OPERATIONS ====================

    /**
     * Flow of tasks for the active destination
     */
    fun tasksFlow(destinationId: String): Flow<List<DestinationTask>> = callbackFlow {
        val listener = tasksCollection(destinationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Tasks listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        DestinationTask(
                            id = doc.id,
                            destinationId = doc.getString("destinationId") ?: destinationId,
                            visaTrackId = doc.getString("visaTrackId") ?: "",
                            phaseId = doc.getString("phaseId") ?: "",
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description"),
                            category = doc.getString("category"),
                            completed = doc.getBoolean("completed") ?: false,
                            dueAt = doc.getLong("dueAt"),
                            createdAt = doc.getLong("createdAt"),
                            updatedAt = doc.getLong("updatedAt"),
                            seeded = doc.getBoolean("seeded") ?: false,
                            seedKey = doc.getString("seedKey"),
                            order = doc.getLong("order")?.toInt() ?: 0,
                            links = parseLinks(doc.get("links"))
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse task ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(tasks.sortedWith(
                    compareBy(
                        { Phases.getOrder(it.phaseId) },
                        { it.order },
                        { it.title }
                    )
                ))
            }

        awaitClose { listener.remove() }
    }

    private fun parseLinks(raw: Any?): List<Link>? {
        val list = raw as? List<*> ?: return null
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val label = map["label"] as? String ?: return@mapNotNull null
            val url = map["url"] as? String ?: return@mapNotNull null
            Link(label, url)
        }.takeIf { it.isNotEmpty() }
    }

    /**
     * Toggle task completion status
     */
    suspend fun toggleTaskCompleted(
        destinationId: String,
        taskId: String,
        completed: Boolean
    ): Result<Unit> {
        return try {
            tasksCollection(destinationId).document(taskId).update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle task $taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Update task due date
     */
    suspend fun setTaskDueDate(
        destinationId: String,
        taskId: String,
        dueAt: Long?
    ): Result<Unit> {
        return try {
            tasksCollection(destinationId).document(taskId).update(
                mapOf(
                    "dueAt" to dueAt,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set due date for task $taskId", e)
            Result.failure(e)
        }
    }

    // ==================== MIGRATION ====================

    /**
     * Migrate existing flat tasks to the Spain destination.
     * Call this once for existing users who have tasks in the old structure.
     */
    suspend fun migrateExistingTasks(context: Context): Result<Int> {
        return try {
            val oldTasksCol = userDoc().collection("tasks")
            val oldTasks = oldTasksCol.get().await().documents

            if (oldTasks.isEmpty()) {
                Log.d(TAG, "No existing tasks to migrate")
                return Result.success(0)
            }

            // Ensure Spain destination exists
            val spainDest = getOrCreateDestination(DestinationConfig.SPAIN)
            val newTasksCol = tasksCollection(DestinationConfig.SPAIN)

            // Get existing seed keys in new location
            val existingKeys = newTasksCol.get().await().documents
                .mapNotNull { it.getString("seedKey") }
                .toHashSet()

            var migrated = 0
            val batch = db.batch()

            for (doc in oldTasks) {
                val title = doc.getString("title") ?: continue
                val category = doc.getString("category")
                val phaseId = Phases.categoryToPhaseId(category)
                
                val seedKey = DestinationTask.generateSeedKey(
                    destinationId = DestinationConfig.SPAIN,
                    visaTrackId = DestinationConfig.SPAIN_NON_LUCRATIVE,
                    phaseId = phaseId,
                    title = title
                )

                if (existingKeys.contains(seedKey)) {
                    // Already migrated or seeded
                    continue
                }

                val newData = doc.data?.toMutableMap() ?: mutableMapOf()
                newData["destinationId"] = DestinationConfig.SPAIN
                newData["visaTrackId"] = DestinationConfig.SPAIN_NON_LUCRATIVE
                newData["phaseId"] = phaseId
                newData["seedKey"] = seedKey
                newData["seeded"] = doc.getBoolean("seeded") ?: (doc.getString("seedKey") != null)

                val newDocRef = newTasksCol.document()
                batch.set(newDocRef, newData)
                existingKeys.add(seedKey)
                migrated++
            }

            if (migrated > 0) {
                batch.commit().await()
            }

            // Set Spain as active destination if not set
            val currentActive = getActiveDestinationId()
            if (currentActive == DestinationConfig.SPAIN) {
                setActiveDestination(DestinationConfig.SPAIN, context)
            }

            Log.d(TAG, "Migrated $migrated tasks to Spain destination")
            Result.success(migrated)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate existing tasks", e)
            Result.failure(e)
        }
    }
}
