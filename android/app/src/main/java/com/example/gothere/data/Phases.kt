package com.example.gothere.data

/**
 * Standard phases used across all destinations.
 * UI displays phases in order by their numeric prefix.
 */
object Phases {
    const val PHASE_1_PLAN = "phase_1_plan"
    const val PHASE_2_DOCUMENTS = "phase_2_documents"
    const val PHASE_3_APPLICATION = "phase_3_application"
    const val PHASE_4_ARRIVAL = "phase_4_arrival_setup"
    const val PHASE_5_FOLLOWUP = "phase_5_residency_followup"

    data class PhaseInfo(
        val id: String,
        val displayName: String,
        val shortName: String,
        val order: Int
    )

    val allPhases = listOf(
        PhaseInfo(PHASE_1_PLAN, "Phase 1: Research & Planning", "Plan", 1),
        PhaseInfo(PHASE_2_DOCUMENTS, "Phase 2: Documentation", "Docs", 2),
        PhaseInfo(PHASE_3_APPLICATION, "Phase 3: Application", "Apply", 3),
        PhaseInfo(PHASE_4_ARRIVAL, "Phase 4: Arrival & Setup", "Arrive", 4),
        PhaseInfo(PHASE_5_FOLLOWUP, "Phase 5: Residency Follow-up", "Follow-up", 5)
    )

    fun getDisplayName(phaseId: String): String {
        return allPhases.find { it.id == phaseId }?.displayName ?: phaseId
    }

    fun getOrder(phaseId: String): Int {
        return allPhases.find { it.id == phaseId }?.order ?: Int.MAX_VALUE
    }

    /**
     * Convert legacy category string to phase ID
     * e.g., "Phase 1: Research & Planning" -> "phase_1_plan"
     */
    fun categoryToPhaseId(category: String?): String {
        if (category.isNullOrBlank()) return PHASE_1_PLAN
        
        return when {
            category.contains("Phase 1", ignoreCase = true) || 
            category.contains("Research", ignoreCase = true) || 
            category.contains("Planning", ignoreCase = true) -> PHASE_1_PLAN
            
            category.contains("Phase 2", ignoreCase = true) || 
            category.contains("Documentation", ignoreCase = true) || 
            category.contains("Visa Application", ignoreCase = true) -> PHASE_2_DOCUMENTS
            
            category.contains("Phase 3", ignoreCase = true) || 
            category.contains("Pre-Move", ignoreCase = true) -> PHASE_3_APPLICATION
            
            category.contains("Phase 4", ignoreCase = true) || 
            category.contains("Arrival", ignoreCase = true) || 
            category.contains("Settling", ignoreCase = true) -> PHASE_4_ARRIVAL
            
            category.contains("Phase 5", ignoreCase = true) || 
            category.contains("Long-Term", ignoreCase = true) || 
            category.contains("Compliance", ignoreCase = true) -> PHASE_5_FOLLOWUP
            
            else -> PHASE_1_PLAN
        }
    }
}
