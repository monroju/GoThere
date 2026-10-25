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
 * Plus `crew-notifications/` for the audit log of every event processed, and
 * two server-only collections that make handling idempotent and ordered:
 *
 *   • `iapEvents/{source}_{eventId}` — one row per notification already
 *     handled (Apple notificationUUID, Pub/Sub messageId), so store retries
 *     and duplicate deliveries are skipped. TTL on `expireAt`.
 *   • `iapState/{source}_{hash(key)}` — last applied state per purchase with
 *     the time it describes, so an older event arriving late cannot overwrite
 *     a newer one.
 */

const crypto = require('crypto');
const { getFirestore, FieldValue, Timestamp } = require('firebase-admin/firestore');

const EVENT_RETENTION_MS = 90 * 86400 * 1000;
// Replace these two fields whole (a plain merge would keep a stale
// subscriptionStatus.expiresAt after active -> expired), leave the rest alone.
const SUBSCRIPTION_FIELDS = { mergeFields: ['subscriptionStatus', 'hasAllAccess'] };

/**
 * Looks up a user UID from an Apple originalTransactionId or Google purchaseToken
 * by reading the `iapPurchases/{key}` document the client writes at purchase time.
 *
 * Returns null when no lookup row exists — that means the client never wrote
 * one (older client version, or write failed). The handler should log + skip,
 * not write to a guessed user document.
 */
async function lookupUidByPurchase(key) {
  const row = await lookupPurchase(key);
  return row ? row.uid : null;
}

/**
 * Same lookup, returning the whole row. Rows the server rejected (token not
 * valid for this app, or bound to a different account) are treated as absent.
 */
async function lookupPurchase(key) {
  if (!key) return null;
  const doc = await getFirestore().collection('iapPurchases').doc(key).get();
  if (!doc.exists || doc.get('rejected') === true) return null;
  const uid = doc.get('uid');
  return uid ? { uid, ...doc.data() } : null;
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
  await getFirestore().collection('users').doc(uid)
    .set(subscriptionPayload(status, expiresAtSeconds), SUBSCRIPTION_FIELDS);
}

function subscriptionPayload(status, expiresAtSeconds) {
  const doc = { state: status };
  if (status === 'active' && expiresAtSeconds != null) {
    doc.expiresAt = expiresAtSeconds;
  }
  return {
    subscriptionStatus: doc,
    hasAllAccess: status === 'active'
  };
}

/**
 * Applies one store notification exactly once and in order, in a single
 * transaction:
 *   • eventId already recorded       → 'duplicate', nothing written
 *   • orderMillis older than the last state applied for stateKey → 'stale'
 *   • otherwise the user's status is written → 'applied'
 *   • no status / no uid             → 'noop' (event still recorded)
 *
 * The event row is written in the same transaction as the status, so a crash
 * before commit leaves nothing behind and the store's retry is handled again.
 */
async function applyStoreEvent({ source, eventId, stateKey, orderMillis, uid, status, expiresAtSeconds = null }) {
  const db = getFirestore();
  const eventRef = eventId ? db.collection('iapEvents').doc(`${source}_${eventId}`) : null;
  const stateRef = stateKey
    ? db.collection('iapState').doc(`${source}_${crypto.createHash('sha256').update(String(stateKey)).digest('hex').slice(0, 40)}`)
    : null;

  return db.runTransaction(async (tx) => {
    const eventSnap = eventRef ? await tx.get(eventRef) : null;
    const stateSnap = stateRef ? await tx.get(stateRef) : null;
    if (eventSnap && eventSnap.exists) return 'duplicate';

    let result = 'noop';
    if (status && uid) {
      const last = stateSnap && stateSnap.exists ? (stateSnap.get('lastOrderMillis') || 0) : 0;
      if (orderMillis != null && orderMillis < last) {
        result = 'stale';
      } else {
        tx.set(db.collection('users').doc(uid), subscriptionPayload(status, expiresAtSeconds), SUBSCRIPTION_FIELDS);
        if (stateRef) {
          tx.set(stateRef, {
            uid,
            status,
            expiresAt: expiresAtSeconds,
            lastOrderMillis: orderMillis || Date.now(),
            updatedAt: FieldValue.serverTimestamp()
          }, { merge: true });
        }
        result = 'applied';
      }
    }
    if (eventRef) {
      tx.set(eventRef, {
        result,
        processedAt: FieldValue.serverTimestamp(),
        expireAt: Timestamp.fromMillis(Date.now() + EVENT_RETENTION_MS)
      });
    }
    return result;
  });
}

/**
 * Appends an audit row to `crew-notifications/` describing the event we just
 * handled. Keys are chosen so a Mac Mini watcher can filter by `source` and
 * `type` without parsing free-form text. `payload` is the raw event for replay.
 */
async function logEvent({ source, type, uid, productId, payload }) {
  await getFirestore().collection('crew-notifications').add({
    source,                          // 'apple' | 'google'
    type,                            // notification type or subtype string
    uid: uid || null,
    productId: productId || null,
    payload: payload || null,
    receivedAt: FieldValue.serverTimestamp()
  });
}

module.exports = {
  lookupUidByPurchase,
  lookupPurchase,
  writeSubscriptionStatus,
  applyStoreEvent,
  logEvent
};
