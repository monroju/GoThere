/**
 * Server check of the `iapPurchases/{key}` lookup rows the apps write.
 *
 * The row only says "this store purchase belongs to uid X", and the store
 * notification handlers route state to that uid. A row is client-written, so
 * on create it is checked here:
 *
 *   • Android (key = purchaseToken): read back from the Play Developer API.
 *     Unknown tokens, and tokens bound to a different account through
 *     obfuscatedExternalAccountId, are marked `rejected`; the handlers then
 *     ignore the row. Valid ones get `verified: true` and the real productId,
 *     and the current Play state is applied to the user right away (the
 *     PURCHASED notification often arrives before the app writes the row).
 *   • iOS (key = originalTransactionId): there is no lookup without an App
 *     Store Server API key, so the row stays unverified here; apple.js checks
 *     the signed transaction's appAccountToken against the uid instead.
 *
 * Firestore rules stop clients from writing `verified`, `rejected` or any
 * other server field on these rows.
 */

const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { FieldValue } = require('firebase-admin/firestore');
const play = require('./lib/play');
const { applyStoreEvent } = require('./lib/firestore');

async function checkAndroidRow(row, purchaseToken, api = play) {
  const result = await api.getSubscription(purchaseToken);
  if (result.kind === 'ok') {
    const accountId = api.accountIdOf(result.data);
    if (accountId && accountId !== api.obfuscatedAccountId(row.uid)) {
      return { rejected: true, rejectReason: 'account_mismatch' };
    }
    const expiresAtMillis = api.latestExpiryMillis(result.data);
    return {
      verified: true,
      verifiedProductId: api.productIdOf(result.data),
      accountBound: Boolean(accountId),
      state: {
        status: api.statusFromSubscription(result.data),
        expiresAtSeconds: expiresAtMillis ? Math.floor(expiresAtMillis / 1000) : null
      }
    };
  }
  if (result.kind === 'invalid') return { rejected: true, rejectReason: 'unknown_token' };
  // forbidden (no Play access yet) or transient: leave unverified.
  return { verification: result.kind };
}

exports.handler = onDocumentCreated(
  { document: 'iapPurchases/{key}', region: 'us-central1', memory: '256MiB', timeoutSeconds: 30 },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const row = snap.data() || {};
    if (row.platform !== 'android') return;

    const { state, ...update } = await checkAndroidRow(row, event.params.key);
    if (update.rejected) {
      console.warn('[purchases] rejected iapPurchases row (%s) for uid %s', update.rejectReason, row.uid);
    }
    await snap.ref.set({ ...update, checkedAt: FieldValue.serverTimestamp() }, { merge: true });

    if (state && state.status && row.uid) {
      await applyStoreEvent({
        source: 'google',
        eventId: null,
        stateKey: event.params.key,
        orderMillis: Date.now(),
        uid: row.uid,
        status: state.status,
        expiresAtSeconds: state.expiresAtSeconds
      });
    }
  }
);

exports._internal = { checkAndroidRow };
