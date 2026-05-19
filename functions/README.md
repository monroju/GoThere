# GoThere IAP Notification Functions

Server-side handlers for Apple App Store Server Notifications v2 and Google
Play Real-Time Developer Notifications. Reconcile each user's
`users/{uid}.subscriptionStatus` field on Firestore so the iOS / Android apps
see accurate subscription state even when the user isn't running the app at
the moment a renewal / refund / expiration happens.

## Firebase project

- Project: `gothere-e5ea7`
- Region: `us-central1` (Functions default; same as Firestore `nam5`)
- Blaze plan required (Functions are not available on Spark)

## Endpoints after deploy

```
appleIap   →  https://us-central1-gothere-e5ea7.cloudfunctions.net/appleIap
googleIap  →  Pub/Sub trigger on topic `projects/gothere-e5ea7/topics/play-rtdn`
```

## Operator setup (one-time)

### 1. Install + initial deploy

```sh
cd C:/Users/JGM/Projects/GoThere/android/functions
npm install
firebase login          # if not already authenticated
firebase use gothere-e5ea7
firebase deploy --only functions
```

The deploy command prints the resolved URLs — paste them into the dashboards below.

### 2. Telegram alert credentials

```sh
firebase functions:config:set \
  telegram.token="<bot token from _credentials/>" \
  telegram.chat_id="<operator chat id>"

# Functions v2 alternative (env vars):
# Set TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID in functions/.env
firebase deploy --only functions     # redeploy to pick up the new config
```

### 3. Apple ASC Server Notifications

App Store Connect → My Apps → GoThere → App Information →
**App Store Server Notifications**:

- Production Server URL: `https://us-central1-gothere-e5ea7.cloudfunctions.net/appleIap`
- Sandbox Server URL:    same URL (the function honors the `APPLE_ENV` env var; set it to `SANDBOX` in a separate function deployment if you want truly isolated handling, otherwise leave default and use one URL)
- Version: **2**

Apple sends a verification test on save — the function returns `200` on
success. Check `firebase functions:log` to confirm the test arrived and
verified.

#### Apple env (optional overrides)

```
APPLE_BUNDLE_ID=com.gothere.ios   # default if unset
APPLE_APP_ID=6760248690           # default if unset
APPLE_ENV=PRODUCTION              # PRODUCTION or SANDBOX
```

### 4. Google Play RTDN

```sh
# Create the Pub/Sub topic the function subscribes to.
gcloud pubsub topics create play-rtdn --project=gothere-e5ea7

# Grant Play the publish permission on the topic.
gcloud pubsub topics add-iam-policy-binding play-rtdn \
  --member=serviceAccount:google-play-developer-notifications@system.gserviceaccount.com \
  --role=roles/pubsub.publisher \
  --project=gothere-e5ea7
```

Then in Play Console → Monetization setup → **Real-time developer notifications**:

- Topic name: `projects/gothere-e5ea7/topics/play-rtdn`
- Click **Send test notification** — should appear in `crew-notifications/`
  within a few seconds with `type: TEST`.

### 5. Firestore rules

The existing rules deny client writes to `users/{uid}` by anyone other than
the user. Functions use the Admin SDK which bypasses rules, so no changes
are needed.

The new `crew-notifications/` and `iapPurchases/` collections do NOT need
client read/write permission. They're server-only — clients write to
`iapPurchases/{key}` via the same Admin-bypass mechanism the existing
`users/{uid}` writes go through (PurchaseManager uses the user's auth, which
satisfies the per-uid rule when keyed by uid).

> Open question for Item 13: do we want `iapPurchases/` documents to be
> per-uid (writable by client) or admin-only? Per-uid is simpler — adding a
> rule like `match /iapPurchases/{key} { allow create: if request.auth != null
> && request.resource.data.uid == request.auth.uid; }` lets the client write
> the lookup row at purchase time without server involvement.

## Local development

```sh
cd functions
npm run logs     # tail Cloud Function logs
firebase emulators:start --only functions,firestore   # local test
```

Apple's signed notification format is hard to reproduce locally without
real signed payloads. Easiest path: use Apple's "Request a Test Notification"
endpoint in App Store Connect once the function is deployed to staging,
then watch `firebase functions:log` for the live test.

For Google: `gcloud pubsub topics publish play-rtdn --message='{...}'` to
inject a synthetic RTDN payload.

## Notification → SubscriptionStatus mapping

See `apple.js#mapToStatus` and `google.js#STATUS_BY_TYPE` for the
authoritative tables. Refund-like events (Apple REFUND, Google
SUBSCRIPTION_REVOKED, Google ONE_TIME_PRODUCT_CANCELED) also fire a
Telegram alert.

## How the user lookup works

Each subscription / one-time purchase writes a small lookup document at
`iapPurchases/{key}` where `{key}` is:

- iOS: the Apple `originalTransactionId` (string form). Apple's
  notifications reference this id, not the renewal-specific transaction id.
- Android: the Play `purchaseToken`.

Body:

```json
{ "uid": "<firebase-uid>", "productId": "com.gothere.all_access_annual",
  "platform": "ios" | "android", "createdAt": <Timestamp> }
```

The iOS `PurchaseManager.swift` writes this on every subscription
`updatePurchased`. The Android `PurchaseManager.kt` writes it on
`reconcileFromPurchases` for SUBS purchases. Without this lookup the
function can't map an incoming notification back to a Firebase user — it
logs to `crew-notifications/` with `uid: null` and skips the
`users/{uid}` write.
