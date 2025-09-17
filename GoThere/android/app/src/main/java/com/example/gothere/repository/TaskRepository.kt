// app/src/main/java/com/example/gothere/repository/TaskRepository.kt
package com.example.gothere.repository

import com.example.gothere.model.Link
import com.example.gothere.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaskRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun col() = db.collection("users")
        .document(auth.currentUser?.uid ?: "_no_user_")
        .collection("tasks")

    /** Realtime stream of tasks, ordered by category then title */
    fun tasksFlow(): Flow<List<Task>> = callbackFlow {
        val reg = col()
            .orderBy("category", Query.Direction.ASCENDING)
            .orderBy("title", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val items = snap?.documents?.mapNotNull {
                    it.toObject(Task::class.java)?.copy(id = it.id)
                }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun toggleCompleted(taskId: String, completed: Boolean) {
        if (taskId.isBlank()) return
        col().document(taskId).update("completed", completed).await()
    }

    /** Seed the visa journey checklist if user has no tasks yet */
    suspend fun seedIfEmpty() {
        val uid = auth.currentUser?.uid ?: return
        val existing = col().limit(1).get().await()
        if (!existing.isEmpty) return

        val p1 = "Phase 1: Research & Decision Making"
        val p2 = "Phase 2: Visa Application"
        val p3 = "Phase 3: Pre-Move Preparations (Post-Visa Approval)"
        val p4 = "Phase 4: Arrival & Settling In Spain"

        val seed = mutableListOf<Task>()

        // ---- Phase 1 (summary) ----
        seed += listOf(
            Task(
                title = "Research Spain visa options",
                description = "Start with Non-Lucrative, Work, Student, or Digital Nomad basics.",
                category = p1,
                links = listOf(
                    Link("Spain Consulate (US)", "https://www.exteriores.gob.es/Consulados/"),
                    Link("Schengen / Long-stay overview", "https://www.schengenvisainfo.com/long-stay-visas/")
                )
            ),
            Task(title = "Choose the appropriate visa type for your situation", category = p1),
            Task(
                title = "Research cost of living in target cities/regions",
                category = p1,
                links = listOf(Link("Numbeo Cost of Living", "https://www.numbeo.com/cost-of-living/"))
            ),
            Task(title = "Decide on a target city/region in Spain", category = p1),
            Task(
                title = "Begin Spanish learning plan",
                category = p1,
                links = listOf(
                    Link("Duolingo", "https://www.duolingo.com"),
                    Link("Instituto Cervantes", "https://www.cervantes.es/")
                )
            )
        )

        // ---- Phase 2 ----
        seed += listOf(
            Task(title = "Ensure passport is valid for at least 6 months beyond intended stay", category = p2),
            Task(
                title = "Complete visa application form (consulate-specific)",
                description = "Fill the long-stay visa form required by your consulate.",
                category = p2,
                links = listOf(Link("National Visa Form (Example) (PDF)", "https://www.exteriores.gob.es/Consulados/"))
            ),
            Task(title = "Obtain passport-sized photos (meeting specifications)", category = p2),
            Task(title = "Gather proof of financial means (bank statements, investments)", category = p2),
            Task(
                title = "Secure Spanish-compliant health insurance",
                category = p2,
                links = listOf(Link("Health Insurance Guide", "https://www.schengenvisainfo.com/visa-insurance/"))
            ),
            Task(title = "Obtain medical certificate (apostilled if required)", category = p2),
            Task(
                title = "Obtain police clearance certificate (FBI background check, apostilled)",
                category = p2,
                links = listOf(Link("FBI Background Check", "https://www.edo.cjis.gov/"))
            ),
            Task(title = "Arrange accommodation proof in Spain (rental/letter of invitation)", category = p2),
            Task(title = "Get sworn translations for non-Spanish documents", category = p2),
            Task(title = "Pay the visa application fee", category = p2),
            Task(title = "Schedule visa appointment at the Spanish consulate", category = p2),
            Task(title = "Submit visa application and attend interview (if required)", category = p2),
            Task(title = "Track visa application status", category = p2)
        )

        // ---- Phase 3 ----
        seed += listOf(
            Task(title = "Receive visa approval and collect passport with visa", category = p3),
            Task(title = "Book flights to Spain", category = p3),
            Task(
                title = "Arrange temporary accommodation in Spain for arrival (if needed)",
                category = p3,
                links = listOf(
                    Link("Booking.com", "https://www.booking.com"),
                    Link("Airbnb", "https://www.airbnb.com")
                )
            ),
            Task(
                title = "Secure long-term housing (if not done)",
                category = p3,
                links = listOf(
                    Link("Idealista", "https://www.idealista.com/en/"),
                    Link("Fotocasa", "https://www.fotocasa.es/en/"),
                    Link("Rental Contract Template (Example) (PDF)", "https://example.com/rental-contract.pdf")
                )
            ),
            Task(title = "Notify US banks and credit card companies of travel/move", category = p3),
            Task(title = "Set up mail forwarding in the US", category = p3),
            Task(title = "Sell, donate, or store belongings in the US", category = p3),
            Task(title = "Arrange for shipping of essential items to Spain", category = p3),
            Task(
                title = "Obtain an International Driving Permit (IDP)",
                category = p3,
                links = listOf(Link("AAA IDP Info", "https://www.aaa.com/vacation/idpf.html"))
            ),
            Task(title = "Gather important documents + apostilles if needed", category = p3),
            Task(title = "Plan cancellation of US utilities, phone, subscriptions", category = p3),
            Task(title = "Pack essentials for arrival and initial weeks", category = p3)
        )

        // ---- Phase 4 (FULL, per screenshot) ----
        seed += listOf(
            Task(title = "Arrive in Spain!", category = p4),

            Task(
                title = "Obtain NIE (Número de Identificación de Extranjero) if not issued with visa",
                description = "Often obtained alongside TIE process. Required for most admin tasks.",
                category = p4,
                links = listOf(
                    Link("Form EX-15 (NIE Application) (PDF)", "https://sede.policia.gob.es/portalCiudadano/extranjeria/ex15")
                )
            ),

            Task(
                title = "Obtain TIE (residency card) within 30 days of arrival — Schedule ‘Cita Previa’",
                category = p4,
                links = listOf(
                    Link("Cita Previa Portal", "https://sede.administracionespublicas.gob.es/icpplustie/")
                )
            ),
            Task(
                title = "Obtain TIE — Fill out EX-17 application form",
                category = p4,
                links = listOf(
                    Link("Form EX-17 (TIE Application) (PDF)", "https://sede.policia.gob.es/portalCiudadano/extranjeria/ex17")
                )
            ),
            Task(
                title = "Obtain TIE — Pay TASA 790-012 fee",
                category = p4,
                links = listOf(
                    Link("TASA 790-012 (PDF)", "https://sede.policia.gob.es/portalCiudadano/modelos790")
                )
            ),
            Task(
                title = "Attend TIE appointment with documents (passport, photos, EX-17, tasa receipt, padrón, visa copy)",
                category = p4
            ),

            Task(title = "Register on the Padrón (Empadronamiento) at your local town hall", category = p4),
            Task(title = "Open/finalize Spanish bank account", category = p4),
            Task(title = "Set up utilities (electricity, water, internet, gas)", category = p4),
            Task(title = "Get a Spanish phone number/SIM card", category = p4),
            Task(title = "Register for public healthcare (if eligible) or confirm private coverage", category = p4),
            Task(title = "Register with a local doctor (Centro de Salud)", category = p4),
            Task(
                title = "Exchange US driver's license for Spanish one (if applicable, within 6 months)",
                category = p4,
                links = listOf(
                    Link("DGT License Exchange", "https://sede.dgt.gob.es/es/permisos-de-conducir/canje-de-permisos/")
                )
            )
        )

        // Batch write (fast & atomic)
        val batch = db.batch()
        seed.forEach { t ->
            val doc = col().document()
            batch.set(doc, t.copy(id = null))
        }
        batch.commit().await()

        // Mark seeded (best-effort)
        db.collection("users").document(uid)
            .update("tasksSeededAt", FieldValue.serverTimestamp())
            .addOnFailureListener { /* ignore */ }
    }
}
