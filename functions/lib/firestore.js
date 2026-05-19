/**
 * Firestore helpers shared by the Apple and Google notification handlers.
 *
 * Two collections drive the IAP server reconciliation:
 *
 *   • `iapPurchases/{key}` — written by iOS/Android PurchaseManager on every
 *     subscription purchase. Document body: { uid, productId, platform,
 *     createdAt }. The key is the Apple originalTransactionId (string form)
 *     or the Google purchaseToken. Functions look the user up here when a
 *     notification arrives.
 *
 *   • `users/{uid}` — the authoritative subscription state seen by the apps.
 *     Functions write only the subscriptionStatus + hasAllAccess fields
 *     (merge:true). Field shapes match the iOS / Android SubscriptionStatus
 *     types so both clients can decode without translation.
 *
 * Plus `crew-notifications/` for the audit log of every event processed.
 */

const admin = require('firebase-admin');

/**
 * Looks up a user UID from an Apple originalTransactionId or Google purchaseToken
 * by reading the `iapPurchases/{key}` document the client writes at purchase time.
 *
 * Returns null when no lookup row exists — that means the client never wrote
 * one (older client version, or write failed). The handler should log + skip,
 * not write to a guessed user document.
 */
async function lookupUidByPurchase(key) {
  if (!key) return null;
  const doc = await admin.firestore().collection('iapPurchases').doc(key).get();
  if (!doc.exists) return null;
  return doc.get('uid') || null;
}

/**
 * Atomic write of the new subscription state to the user document, plus a
 * convenience `hasAllAccess` boolean for server-side reads.
 *
 * `status` is one of: 'none' | 'active' | 'expired' | 'billingRetry' | 'gracePeriod'.
 * `expiresAtSeconds` is required when status is 'active'; ignored otherwise.
 * The wire format mirrors the iOS/Android `SubscriptionStatus.toFirestoreMap()`.
 */
async function writeSubscriptionStatus(uid, status, expiresAtSeconds = null) {
  if (!uid) return;
  const doc = { state: status };
  if (status === 'active' && expiresAtSeconds != null) {
    doc.expiresAt = expiresAtSeconds;
  }
  const payload = {
    subscriptionStatus: doc,
    hasAllAccess: status === 'active'
  };
  await admin.firestore().collection('users').doc(uid).set(payload, { merge: true });
}

/**
 * Appends an audit row to `crew-notifications/` describing the event we just
 * handled. Keys are chosen so a Mac Mini watcher can filter by `source` and
 * `type` without parsing free-form text. `payload` is the raw event for replay.
 */
async function logEvent({ source, type, uid, productId, payload }) {
  await admin.firestore().collection('crew-notifications').add({
    source,                          // 'apple' | 'google'
    type,                            // notification type or subtype string
    uid: uid || null,
    productId: productId || null,
    payload: payload || null,
    receivedAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

module.exports = {
  lookupUidByPurchase,
  writeSubscriptionStatus,
  logEvent
};
