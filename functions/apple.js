/**
 * Apple App Store Server Notifications v2 handler.
 *
 * Endpoint URL after deploy (Firebase will print it):
 *   https://us-central1-gothere-e5ea7.cloudfunctions.net/appleIap
 *
 * Operator pastes this URL into App Store Connect → App Information →
 * App Store Server Notifications (Production AND Sandbox).
 *
 * Apple POSTs a JSON body with one field, `signedPayload`, containing a
 * signed JWS. The official `@apple/app-store-server-library` package
 * verifies the JWS chain against Apple's root certificates and decodes the
 * nested transactionInfo / renewalInfo payloads.
 *
 * Notification type → SubscriptionStatus mapping (per brief Item 12):
 *   • DID_RENEW                                      → active
 *   • DID_FAIL_TO_RENEW + subtype GRACE_PERIOD       → gracePeriod
 *   • DID_FAIL_TO_RENEW + subtype BILLING_RETRY      → billingRetry
 *   • DID_FAIL_TO_RENEW (no subtype)                 → billingRetry
 *   • EXPIRED                                        → expired
 *   • GRACE_PERIOD_EXPIRED                           → expired
 *   • REFUND                                         → expired + Telegram alert
 *   • DID_CHANGE_RENEWAL_STATUS                      → no state change, audit only
 *   • SUBSCRIBED (initial purchase)                  → active
 *   • RENEWAL_EXTENDED                               → active (new expiresAt)
 *   • REVOKE                                         → expired
 *
 * Other notification types are logged to crew-notifications/ but don't change
 * subscriptionStatus — they're informational (price increase, refund declined,
 * test pings, etc.).
 */

const { onRequest } = require('firebase-functions/v2/https');
const crypto = require('crypto');
const { lookupPurchase, applyStoreEvent, logEvent } = require('./lib/firestore');
const { sendAlert } = require('./lib/telegram');
const { capture, appleEventName } = require('./lib/posthog');

// Lazy-loaded so the library is only required at first invocation — keeps cold
// starts for unrelated functions in this codebase fast.
const verifierPromises = {};
// One verifier per environment, so TestFlight / App Review (Sandbox) payloads
// verify as well as Production ones. The verifier itself enforces that the
// payload's environment matches, so picking by the unverified field is safe.
async function getVerifier(envName) {
  const key = String(envName || process.env.APPLE_ENV || 'PRODUCTION').toUpperCase() === 'SANDBOX'
    ? 'SANDBOX'
    : 'PRODUCTION';
  if (verifierPromises[key]) return verifierPromises[key];
  verifierPromises[key] = (async () => {
    const fs = require('fs');
    const path = require('path');
    const lib = require('@apple/app-store-server-library');
    const { SignedDataVerifier, Environment } = lib;

    // The library does NOT ship Apple's root certs (there is no
    // AppleRootCertificates export), and with an empty list every payload
    // fails verification. They are bundled in certs/, downloaded from
    // https://www.apple.com/certificateauthority/ (G3 sha256 63:34:3A:BF:...:91:79).
    const certDir = path.join(__dirname, 'certs');
    const rootCerts = fs.readdirSync(certDir)
      .filter(f => f.endsWith('.cer'))
      .map(f => fs.readFileSync(path.join(certDir, f)));

    const bundleId = process.env.APPLE_BUNDLE_ID || 'com.gothere.ios';
    const appAppleId = Number(process.env.APPLE_APP_ID || 6760248690);
    const environment = key === 'SANDBOX' ? Environment.SANDBOX : Environment.PRODUCTION;

    // enableOnlineChecks=true validates the cert chain freshness against
    // Apple's OCSP responder. Slower per request but defends against revoked
    // intermediate certs.
    return new SignedDataVerifier(rootCerts, true, environment, bundleId, appAppleId);
  })();
  return verifierPromises[key];
}

// Reads data.environment from the JWS body WITHOUT trusting it; only used to
// choose which verifier runs the real check.
function peekEnvironment(signedPayload) {
  try {
    const body = JSON.parse(Buffer.from(String(signedPayload).split('.')[1], 'base64url').toString('utf8'));
    return (body && body.data && body.data.environment) || null;
  } catch (e) {
    return null;
  }
}

/**
 * The appAccountToken the app passes to StoreKit at purchase time
 * (Product.PurchaseOption.appAccountToken): a UUID made from the uid, so the
 * signed transaction names the account it was bought for.
 */
function appAccountTokenFor(uid) {
  const h = crypto.createHash('sha256').update(`gothere:${uid}`).digest();
  h[6] = (h[6] & 0x0f) | 0x50; // version 5 style
  h[8] = (h[8] & 0x3f) | 0x80; // RFC 4122 variant
  const x = h.subarray(0, 16).toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

/**
 * Maps a notificationType + subtype pair to our SubscriptionStatus string.
 * Returns null when the event is informational and shouldn't change state.
 */
function mapToStatus(notificationType, subtype) {
  switch (notificationType) {
    case 'SUBSCRIBED':
    case 'DID_RENEW':
    case 'RENEWAL_EXTENDED':
      return 'active';
    case 'DID_FAIL_TO_RENEW':
      if (subtype === 'GRACE_PERIOD') return 'gracePeriod';
      return 'billingRetry';  // includes explicit BILLING_RETRY subtype and no-subtype case
    case 'GRACE_PERIOD_EXPIRED':
    case 'EXPIRED':
    case 'REVOKE':
    case 'REFUND':
      return 'expired';
    case 'DID_CHANGE_RENEWAL_STATUS':
    case 'PRICE_INCREASE':
    case 'REFUND_DECLINED':
    case 'CONSUMPTION_REQUEST':
    case 'TEST':
    default:
      return null;
  }
}

async function processNotification(verified, verifier) {
  // The verified payload contains data: { signedTransactionInfo, signedRenewalInfo }.
  // Re-verify each inner JWS via the same verifier so we trust the embedded fields.
  const data = verified.data || {};
  const txInfo = data.signedTransactionInfo
    ? await verifier.verifyAndDecodeTransaction(data.signedTransactionInfo)
    : null;
  const renewalInfo = data.signedRenewalInfo
    ? await verifier.verifyAndDecodeRenewalInfo(data.signedRenewalInfo)
    : null;

  const notificationType = verified.notificationType;
  const subtype = verified.subtype;
  const originalTransactionId = txInfo && (txInfo.originalTransactionId || String(txInfo.originalTransactionId));
  const productId = txInfo && txInfo.productId;
  const expiresAtSeconds = txInfo && txInfo.expiresDate
    ? Math.floor(Number(txInfo.expiresDate) / 1000)  // Apple returns ms since epoch
    : null;

  const row = await lookupPurchase(originalTransactionId);
  const uid = row ? row.uid : null;
  let newStatus = mapToStatus(notificationType, subtype);

  // Purchases made by builds that set appAccountToken name their account; a
  // lookup row claiming a different uid is not applied.
  const accountToken = txInfo && txInfo.appAccountToken;
  if (newStatus && uid && accountToken && String(accountToken).toLowerCase() !== appAccountTokenFor(uid)) {
    console.error('[apple] transaction appAccountToken does not match iapPurchases uid; not applying.');
    newStatus = null;
  }
  if (newStatus && !uid) {
    console.warn('[apple] no iapPurchases lookup row for originalTransactionId:', originalTransactionId);
  }

  // Apple retries until it gets a 200 and does not guarantee order:
  // notificationUUID dedupes, signedDate orders per original transaction.
  const outcome = await applyStoreEvent({
    source: 'apple',
    eventId: verified.notificationUUID,
    stateKey: originalTransactionId,
    orderMillis: Number(verified.signedDate) || null,
    uid,
    status: newStatus,
    expiresAtSeconds
  });
  if (outcome === 'duplicate') {
    console.log('[apple] duplicate notification %s skipped', verified.notificationUUID);
    return;
  }

  await logEvent({
    source: 'apple',
    type: subtype ? `${notificationType}:${subtype}` : notificationType,
    uid,
    productId,
    payload: {
      notificationType,
      subtype,
      originalTransactionId,
      expiresAtSeconds,
      notificationUUID: verified.notificationUUID || null,
      outcome,
      renewalInfo: renewalInfo ? {
        autoRenewStatus: renewalInfo.autoRenewStatus,
        autoRenewProductId: renewalInfo.autoRenewProductId,
        expirationIntent: renewalInfo.expirationIntent
      } : null
    }
  });

  const phEvent = appleEventName(notificationType, subtype);
  if (phEvent) {
    const properties = { product_id: productId, store_type: subtype ? `${notificationType}:${subtype}` : notificationType };
    // price is in milliunits on newer transaction payloads.
    if (txInfo && txInfo.price != null) properties.price = Number(txInfo.price) / 1000;
    if (txInfo && txInfo.currency) properties.currency = txInfo.currency;
    if (txInfo && txInfo.environment) properties.environment = txInfo.environment;
    await capture(phEvent, { uid, fallbackId: originalTransactionId, platform: 'ios', properties });
  }

  if (notificationType === 'REFUND') {
    await sendAlert([
      '🚨 *GoThere refund processed*',
      `Product: \`${productId || 'unknown'}\``,
      `User: \`${uid || 'unknown'}\``,
      `Original tx: \`${originalTransactionId || 'unknown'}\``
    ].join('\n'));
  }
}

exports._internal = { appAccountTokenFor, mapToStatus };

exports.handler = onRequest(
  {
    memory: '256MiB',
    timeoutSeconds: 30,
    region: 'us-central1',
    invoker: 'public'  // Apple's webhook POSTs without auth headers; must be public
  },
  async (req, res) => {
    if (req.method !== 'POST') {
      res.status(405).send('Method Not Allowed');
      return;
    }
    const signedPayload = req.body && req.body.signedPayload;
    if (!signedPayload) {
      res.status(400).send('Missing signedPayload');
      return;
    }

    try {
      const verifier = await getVerifier(peekEnvironment(signedPayload));
      const verified = await verifier.verifyAndDecodeNotification(signedPayload);
      await processNotification(verified, verifier);
      // Apple expects a 200 OK with no body on success.
      res.status(200).send('');
    } catch (e) {
      console.error('[apple] notification handling failed:', e);
      // Returning non-2xx asks Apple to retry. Bound the retry blast radius by
      // returning 500 only for transient errors; 422 for malformed/unverifiable
      // payloads so Apple doesn't keep retrying a bad signature.
      const msg = (e && e.message) || '';
      // The library throws VerificationException (numeric `status`) for bad payloads.
      const isVerificationFailure = typeof (e && e.status) === 'number' || /verif|decode|signature|invalid/i.test(msg);
      // Detail stays in the log above; callers only get a generic reason.
      res.status(isVerificationFailure ? 422 : 500).send(isVerificationFailure ? 'Unverifiable payload' : 'Internal error');
    }
  }
);
