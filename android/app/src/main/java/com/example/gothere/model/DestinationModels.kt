package com.example.gothere.model

/**
 * Represents a destination country for relocation.
 * This is the top-level entity that contains visa tracks and tasks.
 */
data class DestinationCountry(
    val id: String,                    // "spain", "portugal", "mexico"
    val name: String,                  // "Spain", "Portugal", "Mexico"
    val flagEmoji: String,             // "🇪🇸", "🇵🇹", "🇲🇽"
    val region: String,                // "Europe", "North America"
    val description: String,
    val visaTracks: List<VisaTrack>,
    val defaultVisaTrackId: String,
    val seedVersion: Int = 1,          // Increment when seed data changes
    val officialImmigrationUrl: String? = null
)

/**
 * User's destination selection stored in Firestore.
 * Path: /users/{uid}/destinations/{destinationId}
 */
data class UserDestination(
    val destinationId: String = "",
    val countryName: String = "",
    val activeVisaTrackId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeededAt: Long? = null,
    val seedVersion: Int = 0
)

/**
 * Extension of the existing Task model with destination scoping.
 * This replaces the existing Task model to add destination context.
 */
data class DestinationTask(
    val id: String? = null,
    val destinationId: String = "",
    val visaTrackId: String = "",
    val phaseId: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String? = "General",  // Kept for backward compatibility
    val completed: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val seeded: Boolean = false,
    val seedKey: String? = null,        // Deterministic key for deduplication
    val links: List<Link>? = null,
    val order: Int = 0                  // For sorting within phase
) {
    /**
     * Convert to legacy Task format for backward compatibility
     */
    fun toTask(): Task = Task(
        id = id,
        title = title,
        description = description,
        category = category,
        completed = completed,
        dueAt = dueAt,
        createdAt = createdAt,
        links = links
    )

    companion object {
        /**
         * Generate a deterministic seed key for deduplication
         */
        fun generateSeedKey(
            destinationId: String,
            visaTrackId: String,
            phaseId: String,
            title: String
        ): String {
            val slug = title.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), "")
                .replace(Regex("\\s+"), "_")
                .take(50)
            return "${destinationId}::${visaTrackId}::${phaseId}::${slug}"
        }
    }
}
