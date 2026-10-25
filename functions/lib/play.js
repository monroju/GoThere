/**
 * Google Play Developer API reads for subscription state.
 *
 * RTDN messages only say "something happened to this token". The current state
 * and expiry come from purchases.subscriptionsv2.get, which is also what proves
 * the token is real and belongs to our package.
 *
 * Access: the Functions runtime service account must be added in Play Console
 * → Users and permissions with "View app information" and "View financial
 * data" for GoThere. Until then every call returns 401/403 and callers fall
 * back to the notification payload.
 */

const { GoogleAuth } = require('google-auth-library');
const crypto = require('crypto');

const PACKAGE_NAME = process.env.PLAY_PACKAGE_NAME || 'com.gothere.app';
const API = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications';
const TIMEOUT_MS = 10000;

const auth = new GoogleAuth({ scopes: ['https://www.googleapis.com/auth/androidpublisher'] });

/**
 * Fetches one subscription purchase.
 * Returns { kind: 'ok', data } | { kind: 'invalid', status } (unknown or
 * malformed token) | { kind: 'forbidden', status } (no Play access) |
 * { kind: 'transient', status, error } (retry later).
 */
async function getSubscription(purchaseToken, packageName = PACKAGE_NAME) {
  const url = `${API}/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
  let res;
  try {
    const client = await auth.getClient();
    const headers = await client.getRequestHeaders(url);
    res = await fetch(url, { headers, signal: AbortSignal.timeout(TIMEOUT_MS) });
  } catch (e) {
    return { kind: 'transient', status: 0, error: e.message };
  }
  if (res.ok) return { kind: 'ok', data: await res.json() };
  const body = await res.text().catch(() => '');
  if (res.status === 401 || res.status === 403) return { kind: 'forbidden', status: res.status, error: body.slice(0, 300) };
  if (res.status === 400 || res.status === 404 || res.status === 410) return { kind: 'invalid', status: res.status };
  return { kind: 'transient', status: res.status, error: body.slice(0, 300) };
}

// subscriptionsv2 state → our SubscriptionStatus. null = leave state alone.
function statusFromSubscription(data, nowMillis = Date.now()) {
  const expiresAtMillis = latestExpiryMillis(data);
  switch (data && data.subscriptionState) {
    case 'SUBSCRIPTION_STATE_ACTIVE':
      return 'active';
    case 'SUBSCRIPTION_STATE_CANCELED':
      // Auto-renew turned off: still entitled until the paid period ends.
      return expiresAtMillis && expiresAtMillis > nowMillis ? 'active' : 'expired';
    case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
      return 'gracePeriod';
    case 'SUBSCRIPTION_STATE_ON_HOLD':
      return 'billingRetry';
    case 'SUBSCRIPTION_STATE_PAUSED':
    case 'SUBSCRIPTION_STATE_EXPIRED':
      return 'expired';
    default:
      // PENDING, PENDING_PURCHASE_CANCELED, UNSPECIFIED
      return null;
  }
}

function latestExpiryMillis(data) {
  const items = (data && data.lineItems) || [];
  let max = null;
  for (const item of items) {
    const t = Date.parse(item.expiryTime);
    if (Number.isFinite(t) && (max == null || t > max)) max = t;
  }
  return max;
}

function productIdOf(data) {
  const item = ((data && data.lineItems) || [])[0];
  return (item && item.productId) || null;
}

/**
 * The account id the app passes to Play at purchase time
 * (BillingFlowParams.setObfuscatedAccountId). Same derivation on both sides.
 */
function obfuscatedAccountId(uid) {
  return crypto.createHash('sha256').update(`gothere:${uid}`).digest('hex');
}

function accountIdOf(data) {
  const ids = data && data.externalAccountIdentifiers;
  return (ids && ids.obfuscatedExternalAccountId) || null;
}

module.exports = {
  PACKAGE_NAME,
  getSubscription,
  statusFromSubscription,
  latestExpiryMillis,
  productIdOf,
  obfuscatedAccountId,
  accountIdOf
};
