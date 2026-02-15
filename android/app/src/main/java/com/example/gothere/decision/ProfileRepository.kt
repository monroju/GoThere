package com.example.gothere.decision

import com.example.gothere.util.FirebaseUtil
import kotlinx.coroutines.tasks.await

object ProfileRepository {
    suspend fun saveProfile(profile: UserProfile): String {
        val data = hashMapOf(
            "household" to profile.household.name,
            "budget" to profile.budget.name,
            "climate" to profile.climate.name,
            "wantsCoastal" to profile.wantsCoastal,
            "wantsBigCity" to profile.wantsBigCity,
            "language" to profile.languageComfort.name,
            "carOwner" to profile.carOwner,
            "airportImportant" to profile.airportProximityImportant,
            "businessFocus" to profile.businessFocus?.name,
            "nightlife" to profile.nightlifePreference.name,
            "expatDensityPref" to profile.expatDensityPref?.name,
            "safetyPriority" to profile.safetyPriority,
            "timestamp" to System.currentTimeMillis()
        )
        val ref = FirebaseUtil.profilesCollection().add(data).await()
        return ref.id
    }
}
