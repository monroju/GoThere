# GoThere Android: paid to freemium plan (2026-09-24)

Plan only. Nothing has been changed in code or Play Console.

## Current state
- Live source: `android/android/app` builds `com.gothere.app`, versionCode 33 / 1.11.1 (matches Play). Kotlin namespace `com.example.gothere`.
- Billing: `billing-ktx:8.3.0`. All gating in `billing/PurchaseManager.kt` (`hasAllAccess()`, `isCountryUnlocked()` with the Portugal trial from `FirstWeekTrialService`, `FREE_COUNTRIES = {spain, canada}`).
- SKUs already coded: legacy `com.gothere.<country>_pack` + `com.gothere.all_countries`, and iOS-parity `com.gothere.all_access_monthly`, `all_access_annual`, `europe_bundle`, `americas_bundle`.
- Gate readers: `MainActivity.kt` (~265, ~421), `ResourcesScreen.kt:108,338`, `RealJourneyScreen.kt:51`, `AIViewModel.kt:61`, `DocumentScanViewModel.kt:50`, `ui/PaywallScreen.kt`.

### Problems found
1. **Portugal 7-day trial is broken on Android.** `MainActivity` and `ResourcesScreen` call `purchasedCountries.contains()` instead of `isCountryUnlocked()`.
2. No Android equivalent of iOS `LegacyEntitlementService`.
3. `firestore.rules` lets users write their own `hasAllAccess` / `ownedSKUs` / `promoAccessUntil`. Pre-existing; a `legacyPaidInstall` flag would be equally self-writable.
4. Paying $3.99 today only unlocks Spain + Canada, so grandfathering buyers to all-access is an upgrade.

## Recommendation: A, flip the existing listing with grandfathering
Once free, Play can't tell buyers apart, so grandfathering must ship while still paid.
1. **Primary signal:** `PackageInfo.firstInstallTime` < cutoff AND installer = `com.android.vending`.
2. **Firestore mirror:** `users/{uid}.legacyPaidInstall = true` (same field as iOS) so reinstalls and new devices keep access.
3. **Manual backstop:** buyer emails gabriel@getgothere.app with their GPA order ID, flag set by hand. Only about 2 devices exist.

Rejected, B (new free package): loses listing history, needs a new Firebase app and all SKUs, risks Play's repetitive-content policy, strands buyers.

## Implementation (v1.12.0, versionCode 34)
1. New `billing/LegacyEntitlementService.kt`: `FREEMIUM_CUTOFF_MS` = planned flip date + 1 day UTC; grant-only, cached in SharedPreferences; debug override.
2. `PurchaseManager.kt`: `_isLegacyPaidInstall` StateFlow feeding `hasAllAccess()` and `purchasedCountries`; persist + read `legacyPaidInstall` in Firestore, never clear; log `legacy_paid_install`.
3. Fix gates: `MainActivity.kt:265,421` and `ResourcesScreen.kt:108` use `isCountryUnlocked(id)`; observe the legacy flow.
4. `PaywallScreen.kt`: subscriptions + bundles first with renewal/cancel wording; hide buy button if no price loads.
5. `build.gradle.kts`: versionCode 34, versionName 1.12.0.

Play Console: create subs `all_access_monthly` (base plan `monthly`), `all_access_annual` (`annual`), in-app `europe_bundle`, `americas_bundle`, prices mirrored from ASC; keep legacy SKUs; wire RTDN Pub/Sub `projects/gothere-e5ea7/topics/play-rtdn`.

## Testing (internal track + license testers)
- Fresh install: Spain/Canada free, Portugal 7-day trial works, others paywalled.
- Buy every SKU (test cards incl. declined + pending), restore, sub expiry (5-min renewals).
- Legacy: install 1.11.1 from Play, update to 1.12, confirm all-access; second device same account keeps it via Firestore.

## Order (one-way step last)
1. Create SKUs. 2. Internal test 1.12. 3. Roll 1.12 to 100% **while still paid**. 4. Wait for live.
5. Update listing copy + Data safety.
6. **Go/no-go:** 1.12 live at 100%; today < cutoff; updated test device shows all-access + `legacyPaidInstall=true`; prices render live; support reply drafted.
7. **Set price to Free (irreversible).**
8. 48h check: fresh Play install is NOT legacy; watch Crashlytics + orders.
If the flip slips past the cutoff, ship 1.12.1 with a new cutoff first.

## Effort
Code 0.5 to 1 day, Play setup ~2h, testing 0.5 day, review 1 to 3 days. About 2 working days over ~1 week.
