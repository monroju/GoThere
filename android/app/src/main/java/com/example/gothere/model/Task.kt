package com.example.gothere.model

/** Firestore-friendly Task model (must have no-arg defaults). */
data class Task(
    val id: String? = null,               // populated on read
    val title: String = "",
    val description: String? = null,
    val category: String? = "General",
    val completed: Boolean = false,
    val dueAt: Long? = null,              // UTC millis midnight (optional)
    val createdAt: Long? = System.currentTimeMillis(),
    val cityId: String? = null,
    val cityName: String? = null,
    val countryId: String? = null,        // spain, portugal, mexico - defaults to spain for legacy
    val links: List<Link>? = null         // serialized as [{label,url}]
)
