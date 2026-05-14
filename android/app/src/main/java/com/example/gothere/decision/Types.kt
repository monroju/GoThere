package com.example.gothere.decision

import com.example.gothere.data.Destination

data class UserProfile(
    val household: Household,
    val budget: Budget,
    val climate: ClimatePref,
    val wantsCoastal: Boolean,
    val wantsBigCity: Boolean,
    val languageComfort: Language,
    val carOwner: Boolean,
    val airportProximityImportant: Boolean,
    val businessFocus: BusinessFocus?,
    val nightlifePreference: Nightlife,
    val expatDensityPref: Density?,
    val safetyPriority: Int, // 1..5
    val considerations: Set<PersonalConsideration> = emptySet()
)

enum class Household { Singles, Couple, FamilyKids, SingleParent, Retiree }
enum class PersonalConsideration { LGBTQ, Disabled, Veteran, Pregnant }
enum class Budget { Low, Medium, High }
enum class ClimatePref { WarmCoastal, Temperate, Mountain, Continental }
enum class Language { None, Basic, Intermediate }
enum class BusinessFocus { Tech, Tourism, Logistics, Agriculture, RemoteWork, Finance }
enum class Nightlife { Low, Medium, High }
enum class Density { Low, Medium, High }

data class RankedDestination(
    val destination: Destination,
    val score: Int,
    val reasons: List<String>
)
