package com.example.gothere.data

/**
 * Generic schema for real-world visa journeys derived from anonymized client
 * correspondence. Premium-only content, gated by `PurchaseManager.hasAllAccess`.
 * All personal/identifying detail is stripped at authoring time.
 *
 * Mirrors iOS `RealJourney.swift` field-for-field for content parity.
 */
data class RealJourney(
    val id: String,
    val visaId: String,
    val countryId: String,
    val title: String,
    val subtitle: String,
    val totalDuration: String,
    val feeSummary: String,
    val eligibilitySummary: List<String>,
    val phases: List<JourneyPhase>,
    val crossPhaseGotchas: List<JourneyGotcha>,
    val disclaimer: String
)

data class JourneyPhase(
    val id: String,
    val order: Int,
    val title: String,
    val timeframe: String,
    val summary: String,
    val documents: List<String>,
    val lawyerPatterns: List<LawyerPattern>,
    val gotchas: List<String>
)

data class LawyerPattern(
    val id: String,
    val situation: String,
    val phrasing: String
)

data class JourneyGotcha(
    val id: String,
    val title: String,
    val detail: String
)
