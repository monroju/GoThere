package com.example.gothere.data

/**
 * "Bring your job abroad" kit. Mirror of iOS RemoteWorkKit.swift. Copy-paste letter
 * templates + the tax-residency warnings remote workers miss. Informational only.
 */
object RemoteWorkKit {

    data class LetterTemplate(
        val id: String,
        val title: String,
        val subtitle: String,
        val body: String
    )

    val templates: List<LetterTemplate> = listOf(
        LetterTemplate(
            "employer_verification",
            "Employer Remote-Work Verification Letter",
            "For digital-nomad visa applications (Spain DNV, Portugal D8, etc.)",
            """
[Company Letterhead]

[Date]

To Whom It May Concern,

This letter confirms that [Employee Full Name] (passport no. [______]) has been employed by [Company Legal Name], registered in [Country], since [Start Date], in the position of [Job Title].

[Employee Name] is employed on a [full-time / contract] basis with gross annual compensation of [USD/EUR amount], paid [monthly/bi-weekly]. Their role is performed entirely remotely and can be carried out from any location. The company authorizes [Employee Name] to perform their duties remotely from [Destination Country] and has no objection to their relocation.

[Company Name] is a registered entity (registration no. [______]) and has been operating for more than [X] years. This employment relationship is ongoing with no planned end date.

For any verification, please contact [HR Name, title, email, phone].

Sincerely,
[Authorized Signatory Name]
[Title]
[Company Name]
""".trim()
        ),
        LetterTemplate(
            "freelance_income",
            "Self-Employed / Freelance Income Statement",
            "For DNV / self-employment tracks when you have multiple clients",
            """
[Your Name]
[Address]
[Date]

Statement of Self-Employment and Income

I, [Your Full Name], certify that I operate as an independent contractor / freelancer providing [type of services] to clients located outside of [Destination Country].

My business has been active since [Start Date]. Over the past 12 months my gross income from this activity has averaged [USD/EUR amount] per month, evidenced by the attached:
  • Client contracts / service agreements
  • Invoices for the last [6–12] months
  • Bank statements showing corresponding deposits
  • [Most recent US tax return / Schedule C]

Less than [20]% of my income derives from clients in [Destination Country], in compliance with the visa's local-client limit where applicable.

[Signature]
[Your Name]
""".trim()
        )
    )

    data class TaxWarning(val id: String, val title: String, val detail: String)

    val taxWarnings: List<TaxWarning> = listOf(
        TaxWarning("183_day", "The 183-day rule makes you a tax resident",
            "Spend more than ~183 days in a calendar year in most countries and you become a tax resident there — owing tax on (often) worldwide income, regardless of where your employer is. Plan your move date around the tax year."),
        TaxWarning("us_worldwide", "The US taxes you no matter where you live",
            "US citizens and green-card holders file US returns on worldwide income forever. The Foreign Earned Income Exclusion (~\$126k for 2024) and Foreign Tax Credit usually prevent double taxation — but you must file to claim them."),
        TaxWarning("double_tax_treaty", "Check the US tax treaty + totalization agreement",
            "Most GoThere destinations have a US tax treaty (avoids double income tax) and a Social Security totalization agreement (avoids paying into two systems). Mexico and Argentina have weaker coverage — verify before you rely on it."),
        TaxWarning("preferential_regimes", "You may qualify for a flat-tax regime — but the window is tight",
            "Spain's Beckham Law (24% flat) and Portugal's NHR-successor (IFICI) can slash your rate, but you must elect in within months of becoming resident. Italy and Hungary offer flat regimes too. Don't miss the election deadline."),
        TaxWarning("employer_risk", "Your employer may create a 'permanent establishment' risk",
            "If you work for a US employer from abroad, your presence can trigger local payroll/corporate obligations for them. Many companies say no to relocation for this reason — get written authorization (see the template) and warn your HR early.")
    )

    const val disclaimer = "Templates are starting points — adapt to your situation and the specific consulate's requirements. Tax notes are informational, not advice. Engage a cross-border accountant before you move."
}
