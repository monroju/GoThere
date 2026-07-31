package com.example.gothere.repository

import android.content.Context
import com.example.gothere.decision.Household
import com.example.gothere.decision.PersonalConsideration

/**
 * Lightweight persistence for the wizard's PersonalConsiderations + household.
 * Mirror of iOS `UserConsiderationsStore`. Backed by SharedPreferences — small,
 * low-churn, no Room dependency needed.
 */
object UserConsiderationsStore {
    private const val PREFS = "gothere.user"
    private const val KEY_CONS = "considerations"
    private const val KEY_HOUSEHOLD = "household"

    data class State(
        val considerations: Set<PersonalConsideration>,
        val isSingleParent: Boolean
    )

    fun save(context: Context, considerations: Set<PersonalConsideration>, household: Household) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_CONS, considerations.map { it.name }.toSet())
            .putString(KEY_HOUSEHOLD, household.name)
            .apply()
    }

    fun load(context: Context): State {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rawCons = prefs.getStringSet(KEY_CONS, emptySet()) ?: emptySet()
        val parsed = rawCons.mapNotNull { name ->
            runCatching { PersonalConsideration.valueOf(name) }.getOrNull()
        }.toSet()
        val rawHousehold = prefs.getString(KEY_HOUSEHOLD, null)
        val isSingleParent = rawHousehold == Household.SingleParent.name
        return State(parsed, isSingleParent)
    }
}
