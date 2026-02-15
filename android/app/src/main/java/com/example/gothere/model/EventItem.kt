package com.example.gothere.model

/** Calendar event stored per user in Firestore. */
data class EventItem(
    val id: String = "",
    val title: String = "",
    val dateMillis: Long = 0L,        // UTC millis for the event day/time
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)