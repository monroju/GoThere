# GoThere Firebase Functions

Server-side endpoints deployed alongside the iOS/Android apps:

- `appleIap`  — Apple App Store Server Notifications v2 receiver
- `googleIap` — Google Play Real-Time Developer Notifications subscriber
- `aiProxy`   — Anthropic API forwarder for the in-app AI chat ("I don't
                know where to start" entry point)

The IAP handlers reconcile each user's `users/{uid}.subscriptionStatus` field
on Firestore so the iOS / Android apps see accurate subscription state even
when the user isn't running the app at the moment a renewal / refund /
expiration happens.

The AI proxy is a stateless forwarder — it never touches Firestore and never
sends the API key to a client.

## Firebase project

- Project: `gothere-e5ea7`
- Region: `us-central1` (Functions default; same as Firestore `nam5`)
- Blaze plan required (Functions are not available on Spark)

## Endpoints after deploy

```
appleIap   →  https://us-central1-gothere-e5ea7.cloudfunctions.net/appleIap
googleIap  →  Pub/Sub trigger on topic `projects/gothere-e5ea7/topics/play-rtdn`
aiProxy    →  https://us-central1-gothere-e5ea7.cloudfunctions.net/aiProxy
```

The iOS app posts to `https://api.gothere.app/ai/messages` by default. Map
that hostname to `aiProxy` either via DNS + a Firebase Hosting `api-gothere`
site rewrite (recommended, see "AI proxy hostname" below) or by overriding
`ai_proxy_url_override` in the app's UserDefaults during early testing.

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

### 6. AI proxy (Anthropic API key + hostname)

```sh
# Add ANTHROPIC_API_KEY to functions/.env so the proxy can authenticate.
# Anthropic console → Settings → API Keys → generate a new key, scope to
# `messages:write`. Treat it like a production credential — never paste it
# into a commit or chat.
echo "ANTHROPIC_API_KEY=sk-ant-..." >> .env

# Optional overrides:
# echo "ANTHROPIC_MODEL=claude-sonnet-4-6" >> .env          # default model
# echo "ANTHROPIC_MAX_TOKENS=2048"          >> .env         # default ceiling
# echo "AI_PROXY_ALLOW_ORIGIN=https://getgothere.app" >> .env  # tighten CORS

# Deploy just the AI function (faster iteration than full deploy).
npm run deploy:ai
```

#### AI proxy hostname

The iOS client expects `https://api.gothere.app/ai/messages`. Two paths to
make that resolve to `aiProxy`:

**(a) Firebase Hosting rewrite (recommended).** Create a separate Firebase
Hosting site for the API hostname so it doesn't share `public/` with the
landing page:

```sh
firebase hosting:sites:create api-gothere --project gothere-e5ea7
```

Then add an entry to your Firebase Hosting config (or `firebase.json` if
you're managing multi-site there) like:

```json
{
  "hosting": [
    {
      "site": "api-gothere",
      "public": "public-api-stub",
      "rewrites": [
        { "source": "/ai/**", "function": "aiProxy", "region": "us-central1" }
      ]
    }
  ]
}
```

Add `api.gothere.app` as a custom domain on the `api-gothere` site and
follow Firebase's DNS verification steps (TXT + A records). Once the cert
provisions, the iOS client's hard-coded URL resolves to this function.

**(b) UserDefaults override (dev/QA only).** Without the DNS work, point the
client at the raw Cloud Function URL during testing:

```sh
xcrun simctl spawn booted defaults write com.gothere.ios ai_proxy_url_override \
  "https://us-central1-gothere-e5ea7.cloudfunctions.net/aiProxy"
```

The override is read on app launch in `AIService.proxyURL`.

#### AI proxy request shape

```http
POST /  HTTP/1.1
Content-Type: application/json
Accept: application/json     # or text/event-stream for SSE pass-through

{
  "system_prompt_version": "v1",
  "messages": [ { "id": "...", "role": "user", "content": [...] } ],
  "tools":    [ { "name": "...", "description": "...", "input_schema": {...} } ]
}
```

Non-streaming response:

```json
{
  "message":     { "id": "msg_...", "role": "assistant", "content": [...] },
  "stop_reason": "end_turn",
  "usage":       { "input_tokens": 234, "output_tokens": 47 }
}
```

Streaming response: Anthropic SSE events forwarded byte-for-byte; the iOS
`AIStreamHandler` parses them on the device.

System-prompt content lives in `aiProxy.js#SYSTEM_PROMPTS`. The current `v1`
prompt frames the assistant as "Informational only — verify with the
official source. Never give specific legal/tax/medical advice." Bump the
version + add a new entry to update; older app builds keep working because
unknown versions fall back to `v1` with an `X-System-Prompt-Fallback`
response header.

Tools advertised by the iOS app (executed on-device, never on this proxy):
`recommend_visas`, `list_cities_for_country`, `list_wizard_tracks_for_country`.
The proxy passes them through to Anthropic verbatim.

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
