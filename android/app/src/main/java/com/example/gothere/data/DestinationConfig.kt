package com.example.gothere.data

import com.example.gothere.model.DestinationCountry
import com.example.gothere.model.VisaTrack

/**
 * Static configuration for all supported destinations and their visa tracks.
 * This is the single source of truth for destination metadata.
 */
object DestinationConfig {

    // ==================== DESTINATION IDs ====================
    const val SPAIN = "spain"
    const val PORTUGAL = "portugal"
    const val MEXICO = "mexico"

    // ==================== VISA TRACK IDs ====================
    // Spain
    const val SPAIN_NON_LUCRATIVE = "es_non_lucrative"
    const val SPAIN_DIGITAL_NOMAD = "es_digital_nomad"

    // Portugal
    const val PORTUGAL_D7 = "pt_d7_passive_income"
    const val PORTUGAL_D8 = "pt_d8_digital_nomad"

    // Mexico
    const val MEXICO_TEMP_RESIDENT = "mx_temp_resident_economic_solvency"
    const val MEXICO_TEMP_REMOTE = "mx_temp_resident_remote_work"

    // ==================== CURRENT SEED VERSION ====================
    // Increment this when seed data changes to trigger re-seeding
    const val CURRENT_SEED_VERSION = 1

    // ==================== DESTINATIONS ====================

    val spain = DestinationCountry(
        id = SPAIN,
        name = "Spain",
        flagEmoji = "🇪🇸",
        region = "Europe",
        description = "Popular destination for Americans with excellent healthcare, rich culture, and multiple visa pathways.",
        visaTracks = listOf(
            VisaTrack(
                id = SPAIN_NON_LUCRATIVE,
                destinationId = SPAIN,
                name = "Non-Lucrative Visa",
                shortName = "NLV",
                description = "For those with passive income who won't work in Spain. Requires proof of sufficient funds and health insurance.",
                requirements = listOf(
                    "Proof of passive income or savings",
                    "Health insurance valid in Spain",
                    "Clean criminal record",
                    "No intention to work in Spain"
                ),
                estimatedProcessingTime = "2-3 months",
                officialUrl = "https://www.exteriores.gob.es/Consulados/"
            ),
            VisaTrack(
                id = SPAIN_DIGITAL_NOMAD,
                destinationId = SPAIN,
                name = "Digital Nomad Visa",
                shortName = "DNV",
                description = "For remote workers employed by non-Spanish companies. Requires proof of remote employment and income.",
                requirements = listOf(
                    "Remote employment contract with non-Spanish company",
                    "Minimum income threshold",
                    "Health insurance",
                    "Clean criminal record"
                ),
                estimatedProcessingTime = "2-4 months",
                officialUrl = "https://www.exteriores.gob.es/Consulados/"
            )
        ),
        defaultVisaTrackId = SPAIN_NON_LUCRATIVE,
        seedVersion = CURRENT_SEED_VERSION,
        officialImmigrationUrl = "https://www.exteriores.gob.es/Consulados/"
    )

    val portugal = DestinationCountry(
        id = PORTUGAL,
        name = "Portugal",
        flagEmoji = "🇵🇹",
        region = "Europe",
        description = "English-friendly European destination with strong expat community, mild climate, and streamlined visa processes.",
        visaTracks = listOf(
            VisaTrack(
                id = PORTUGAL_D7,
                destinationId = PORTUGAL,
                name = "D7 Passive Income Visa",
                shortName = "D7",
                description = "For retirees and those with passive income (pension, investments, rental income). Most popular pathway for Americans.",
                requirements = listOf(
                    "Proof of passive income (pension, dividends, rental)",
                    "Minimum monthly income threshold",
                    "Health insurance valid in Portugal",
                    "Clean criminal record with apostille",
                    "Proof of accommodation"
                ),
                estimatedProcessingTime = "2-4 months",
                officialUrl = "https://www.vistos.mne.gov.pt/"
            ),
            VisaTrack(
                id = PORTUGAL_D8,
                destinationId = PORTUGAL,
                name = "D8 Digital Nomad Visa",
                shortName = "D8",
                description = "For remote workers with employment or self-employment income from outside Portugal. Newer visa type with growing popularity.",
                requirements = listOf(
                    "Remote employment contract or self-employment proof",
                    "Minimum monthly income (4x Portuguese minimum wage)",
                    "Health insurance valid in Portugal",
                    "Clean criminal record with apostille",
                    "Proof of accommodation"
                ),
                estimatedProcessingTime = "2-4 months",
                officialUrl = "https://www.vistos.mne.gov.pt/"
            )
        ),
        defaultVisaTrackId = PORTUGAL_D7,
        seedVersion = CURRENT_SEED_VERSION,
        officialImmigrationUrl = "https://www.vistos.mne.gov.pt/"
    )

    val mexico = DestinationCountry(
        id = MEXICO,
        name = "Mexico",
        flagEmoji = "🇲🇽",
        region = "North America",
        description = "Close to the US with lower cost of living, no time zone challenges, and welcoming expat communities.",
        visaTracks = listOf(
            VisaTrack(
                id = MEXICO_TEMP_RESIDENT,
                destinationId = MEXICO,
                name = "Temporary Resident - Economic Solvency",
                shortName = "Temp Res",
                description = "For those with sufficient savings or income to support themselves. Most common pathway for Americans relocating.",
                requirements = listOf(
                    "Proof of economic solvency (bank statements)",
                    "Minimum balance or monthly income threshold",
                    "Valid passport",
                    "Application at Mexican consulate"
                ),
                estimatedProcessingTime = "1-3 weeks at consulate, then INM exchange",
                officialUrl = "https://consulmex.sre.gob.mx/"
            ),
            VisaTrack(
                id = MEXICO_TEMP_REMOTE,
                destinationId = MEXICO,
                name = "Temporary Resident - Remote Work",
                shortName = "Remote",
                description = "Similar requirements to economic solvency, but emphasizing remote work income. Note: Mexico doesn't have a dedicated 'digital nomad' visa.",
                requirements = listOf(
                    "Proof of remote employment income",
                    "Bank statements showing consistent deposits",
                    "Valid passport",
                    "Application at Mexican consulate"
                ),
                estimatedProcessingTime = "1-3 weeks at consulate, then INM exchange",
                officialUrl = "https://consulmex.sre.gob.mx/"
            )
        ),
        defaultVisaTrackId = MEXICO_TEMP_RESIDENT,
        seedVersion = CURRENT_SEED_VERSION,
        officialImmigrationUrl = "https://www.inm.gob.mx/"
    )

    // ==================== ALL DESTINATIONS ====================

    val allDestinations: List<DestinationCountry> = listOf(spain, portugal, mexico)

    fun getDestination(id: String): DestinationCountry? = allDestinations.find { it.id == id }

    fun getVisaTrack(destinationId: String, trackId: String): VisaTrack? {
        return getDestination(destinationId)?.visaTracks?.find { it.id == trackId }
    }

    fun getDefaultVisaTrack(destinationId: String): VisaTrack? {
        val destination = getDestination(destinationId) ?: return null
        return destination.visaTracks.find { it.id == destination.defaultVisaTrackId }
    }
}
