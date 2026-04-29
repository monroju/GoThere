package com.example.gothere.model

/**
 * Data models for the Visa Wizard JSON configuration.
 * Parsed from assets/visa_wizard/wizard_config.json.
 */

data class WizardConfig(
    val wizardVersion: Int,
    val tracks: Map<String, WizardTrack>
)

data class WizardTrack(
    val displayName: String,
    val countryId: String,
    val shortName: String,
    val steps: List<WizardStep>,
    val taskRules: List<TaskRule>
)

data class WizardStep(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val questions: List<WizardQuestion>
)

data class WizardQuestion(
    val id: String,
    val type: String, // "single_choice", "boolean", "number"
    val label: String,
    val options: List<QuestionOption>? = null,
    val required: Boolean = false,
    val hint: String? = null,
    val showIf: Map<String, Any>? = null,
    val min: Int? = null,
    val max: Int? = null
)

data class QuestionOption(
    val id: String,
    val label: String
)

data class TaskRule(
    val taskTitle: String,
    val phase: String,
    val description: String? = null,
    val links: List<TaskRuleLink>? = null,
    val conditions: Map<String, Any>, // key -> single value or list of values
    val estimatedWeeks: Int = 0,
    val order: Int = 0
)

data class TaskRuleLink(
    val label: String,
    val url: String
)
