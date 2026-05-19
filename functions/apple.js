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

const functions = require('firebase-functions');
const { lookupUidByPurchase, writeSubscriptionStatus, logEvent } = require('./lib/firestore');
const { sendAlert } = require('./lib/telegram');

// Lazy-loaded so the library is only required at first invocation — keeps cold
// starts for unrelated functions in this codebase fast.
let verifierPromise = null;
async function getVerifier() {
  if (verifierPromise) return verifierPromise;
  verifierPromise = (async () => {
    const lib = require('@apple/app-store-server-library');
    const { SignedDataVerifier, Environment } = lib;

    // Apple bundles the root certs alongside the library; they're returned as
    // Uint8Array buffers ready for the SignedDataVerifier constructor.
    const rootCerts = lib.AppleRootCertificates
      ? lib.AppleRootCertificates.map(c => Buffer.from(c))
      : [];

    const bundleId = process.env.APPLE_BUNDLE_ID || 'com.gothere.ios';
    const appAppleId = Number(process.env.APPLE_APP_ID || 6760248690);
    const environment = (process.env.APPLE_ENV || 'PRODUCTION').toUpperCase() === 'SANDBOX'
      ? Environment.SANDBOX
      : Environment.PRODUCTION;

    // enableOnlineChecks=true validates the cert chain freshness against
    // Apple's OCSP responder. Slower per request but defends against revoked
    // intermediate certs.
    return new SignedDataVerifier(rootCerts, true, environment, bundleId, appAppleId);
  })();
  return verifierPromise;
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

async function processNotification(verified) {
  // The verified payload contains data: { signedTransactionInfo, signedRenewalInfo }.
  // Re-verify each inner JWS via the same verifier so we trust the embedded fields.
  const verifier = await getVerifier();
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

  const uid = await lookupUidByPurchase(originalTransactionId);
  const newStatus = mapToStatus(notificationType, subtype);

  if (newStatus) {
    if (uid) {
      await writeSubscriptionStatus(uid, newStatus, expiresAtSeconds);
    } else {
      console.warn('[apple] no iapPurchases lookup row for originalTransactionId:', originalTransactionId);
    }
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
      renewalInfo: renewalInfo ? {
        autoRenewStatus: renewalInfo.autoRenewStatus,
        autoRenewProductId: renewalInfo.autoRenewProductId,
        expirationIntent: renewalInfo.expirationIntent
      } : null
    }
  });

  if (notificationType === 'REFUND') {
    await sendAlert([
      '🚨 *GoThere refund processed*',
      `Product: \`${productId || 'unknown'}\``,
      `User: \`${uid || 'unknown'}\``,
      `Original tx: \`${originalTransactionId || 'unknown'}\``
    ].join('\n'));
  }
}

exports.handler = functions
  .runWith({ memory: '256MB', timeoutSeconds: 30 })
  .https.onRequest(async (req, res) => {
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
      const verifier = await getVerifier();
      const verified = await verifier.verifyAndDecodeNotification(signedPayload);
      await processNotification(verified);
      // Apple expects a 200 OK with no body on success.
      res.status(200).send('');
    } catch (e) {
      console.error('[apple] notification handling failed:', e);
      // Returning non-2xx asks Apple to retry. Bound the retry blast radius by
      // returning 500 only for transient errors; 422 for malformed/unverifiable
      // payloads so Apple doesn't keep retrying a bad signature.
      const msg = (e && e.message) || '';
      const isVerificationFailure = /verif|decode|signature|invalid/i.test(msg);
      res.status(isVerificationFailure ? 422 : 500).send(msg);
    }
  });
