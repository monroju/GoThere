package com.example.gothere.model

/**
 * Represents a visa track within a destination country.
 * Each destination can have multiple visa tracks (e.g., Digital Nomad, Passive Income, etc.)
 */
data class VisaTrack(
    val id: String,
    val destinationId: String,
    val name: String,
    val shortName: String,
    val description: String,
    val requirements: List<String> = emptyList(),
    val estimatedProcessingTime: String? = null,
    val officialUrl: String? = null
)
