package com.example.gothere.data

import com.example.gothere.model.Task
import com.example.gothere.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

/**
 * Compatibility wrapper so UI/features can call TasksRepository.*
 */
object TasksRepository {
    private val real = TaskRepository()

    fun tasksFlow(): Flow<List<Task>> = real.tasksFlow()

    suspend fun insertTasks(tasks: List<Task>): Result<Unit> = real.insertTasks(tasks)

    // Primary function - used by DecisionTreeScreen
    fun starterTasksForCity(cityId: String, cityName: String, countryId: String): List<Task> {
        val countryLabel = when (countryId) {
            "portugal" -> "Portugal"
            "mexico" -> "Mexico"
            else -> "Spain"
        }

        return listOf(
            Task(
                title = "Research neighborhoods in $cityName",
                category = "Phase 1: Research & Planning",
                countryId = countryId,
                cityId = cityId,
                cityName = cityName
            ),
            Task(
                title = "Compare rentals and schools in $cityName",
                category = "Phase 1: Research & Planning",
                countryId = countryId,
                cityId = cityId,
                cityName = cityName
            ),
            Task(
                title = "Shortlist 10 apartments in $cityName",
                category = "Phase 3: Pre-Move Preparations",
                countryId = countryId,
                cityId = cityId,
                cityName = cityName
            ),
            Task(
                title = "Book 3–5 viewings in $cityName (or request video tours)",
                category = "Phase 3: Pre-Move Preparations",
                countryId = countryId,
                cityId = cityId,
                cityName = cityName
            ),
            Task(
                title = "Research visa requirements for $countryLabel",
                category = "Phase 2: Documentation",
                countryId = countryId,
                cityId = cityId,
                cityName = cityName
            )
        )
    }

    // Backward compatible - single param version
    fun starterTasksForCity(cityName: String): List<Task> {
        return listOf(
            Task(title = "Research neighborhoods in $cityName", category = "Phase 1: Research & Planning"),
            Task(title = "Compare rentals and schools in $cityName", category = "Phase 1: Research & Planning"),
            Task(title = "Shortlist 10 apartments in $cityName", category = "Phase 3: Pre-Move Preparations"),
            Task(title = "Book 3–5 viewings in $cityName (or request video tours)", category = "Phase 3: Pre-Move Preparations")
        )
    }

    // Backward compatible - two param version (cityName + country label)
    fun starterTasksForCityLegacy(cityName: String, country: String?): List<Task> {
        val place = listOfNotNull(cityName.takeIf { it.isNotBlank() }, country?.takeIf { it.isNotBlank() })
            .joinToString(", ")

        val label = if (place.isBlank()) cityName else place

        return listOf(
            Task(title = "Research neighborhoods in $label", category = "Phase 1: Research & Planning"),
            Task(title = "Compare rentals and schools in $label", category = "Phase 1: Research & Planning"),
            Task(title = "Shortlist 10 apartments in $label", category = "Phase 3: Pre-Move Preparations"),
            Task(title = "Book 3–5 viewings in $label (or request video tours)", category = "Phase 3: Pre-Move Preparations")
        )
    }
}