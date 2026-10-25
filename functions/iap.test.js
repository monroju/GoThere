// Offline checks for the store notification logic: Play state mapping, the
// RTDN verification fallbacks, the iapPurchases row check and the account
// binding derivations. Run with `node iap.test.js`.

const assert = require('assert');
const play = require('./lib/play');
const { _internal: google } = require('./google');
const { _internal: purchases } = require('./purchases');
const { _internal: apple } = require('./apple');

let failed = 0;
async function test(name, fn) {
  try {
    await fn();
    console.log('ok ', name);
  } catch (e) {
    failed++;
    console.error('FAIL', name, '\n ', e.message);
  }
}

const NOW = Date.parse('2026-10-25T12:00:00Z');
const sub = (state, expiry, extra = {}) => ({
  subscriptionState: state,
  lineItems: [{ productId: 'all_access_annual', expiryTime: expiry }],
  ...extra
});

// A Play API stand-in that answers with a fixed result.
const fakeApi = (result) => ({ ...play, getSubscription: async () => result });

(async () => {
  await test('active maps to active', () => {
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_ACTIVE', '2027-01-01T00:00:00Z'), NOW), 'active');
  });
  await test('canceled stays active until expiry, then expired', () => {
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_CANCELED', '2026-11-01T00:00:00Z'), NOW), 'active');
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_CANCELED', '2026-10-01T00:00:00Z'), NOW), 'expired');
  });
  await test('grace, hold, paused, expired, pending', () => {
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_IN_GRACE_PERIOD'), NOW), 'gracePeriod');
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_ON_HOLD'), NOW), 'billingRetry');
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_PAUSED'), NOW), 'expired');
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_EXPIRED'), NOW), 'expired');
    assert.strictEqual(play.statusFromSubscription(sub('SUBSCRIPTION_STATE_PENDING'), NOW), null);
  });
  await test('latest expiry across line items', () => {
    const data = { lineItems: [{ expiryTime: '2026-11-01T00:00:00Z' }, { expiryTime: '2027-02-01T00:00:00Z' }, {}] };
    assert.strictEqual(play.latestExpiryMillis(data), Date.parse('2027-02-01T00:00:00Z'));
    assert.strictEqual(play.latestExpiryMillis({}), null);
  });

  const base = { notificationType: 2, purchaseToken: 'tok', packageName: 'com.gothere.app', eventTimeMillis: 1000, row: { uid: 'u1' } };

  await test('RTDN uses Play API state and expiry when available', async () => {
    const r = await google.resolveSubscriptionState(base, fakeApi({ kind: 'ok', data: sub('SUBSCRIPTION_STATE_EXPIRED', '2026-10-01T00:00:00Z') }));
    assert.strictEqual(r.status, 'expired'); // API wins over the RENEWED payload
    assert.strictEqual(r.expiresAtSeconds, Date.parse('2026-10-01T00:00:00Z') / 1000);
    assert.strictEqual(r.verification, 'play_api');
    assert.strictEqual(r.productId, 'all_access_annual');
  });
  await test('RTDN falls back to payload without Play access', async () => {
    delete process.env.PLAY_REQUIRE_VERIFIED;
    const r = await google.resolveSubscriptionState(base, fakeApi({ kind: 'forbidden', status: 403 }));
    assert.strictEqual(r.status, 'active');
    assert.strictEqual(r.verification, 'payload');
    assert.strictEqual(r.orderMillis, 1000);
  });
  await test('PLAY_REQUIRE_VERIFIED drops the payload fallback', async () => {
    process.env.PLAY_REQUIRE_VERIFIED = 'true';
    const r = await google.resolveSubscriptionState(base, fakeApi({ kind: 'forbidden', status: 403 }));
    delete process.env.PLAY_REQUIRE_VERIFIED;
    assert.strictEqual(r.status, null);
  });
  await test('RTDN ignores tokens Play does not know', async () => {
    const r = await google.resolveSubscriptionState(base, fakeApi({ kind: 'invalid', status: 404 }));
    assert.strictEqual(r.status, null);
    assert.strictEqual(r.verification, 'invalid_token');
  });
  await test('RTDN throws on transient errors so Pub/Sub retries', async () => {
    await assert.rejects(google.resolveSubscriptionState(base, fakeApi({ kind: 'transient', status: 503 })));
  });
  await test('RTDN refuses a purchase bound to another account', async () => {
    const data = sub('SUBSCRIPTION_STATE_ACTIVE', '2027-01-01T00:00:00Z', {
      externalAccountIdentifiers: { obfuscatedExternalAccountId: play.obfuscatedAccountId('someone-else') }
    });
    const r = await google.resolveSubscriptionState(base, fakeApi({ kind: 'ok', data }));
    assert.strictEqual(r.status, null);
    assert.strictEqual(r.verification, 'account_mismatch');
  });
  await test('RTDN accepts a purchase bound to the same account', async () => {
    const data = sub('SUBSCRIPTION_STATE_ACTIVE', '2027-01-01T00:00:00Z', {
      externalAccountIdentifiers: { obfuscatedExternalAccountId: play.obfuscatedAccountId('u1') }
    });
    const r = await google.resolveSubscriptionState(base, fakeApi({ kind: 'ok', data }));
    assert.strictEqual(r.status, 'active');
  });

  await test('row check: valid token is verified with its state', async () => {
    const r = await purchases.checkAndroidRow({ uid: 'u1' }, 'tok', fakeApi({ kind: 'ok', data: sub('SUBSCRIPTION_STATE_ACTIVE', '2027-01-01T00:00:00Z') }));
    assert.strictEqual(r.verified, true);
    assert.strictEqual(r.state.status, 'active');
  });
  await test('row check: unknown token is rejected', async () => {
    const r = await purchases.checkAndroidRow({ uid: 'u1' }, 'tok', fakeApi({ kind: 'invalid', status: 400 }));
    assert.strictEqual(r.rejected, true);
  });
  await test('row check: other account is rejected', async () => {
    const data = sub('SUBSCRIPTION_STATE_ACTIVE', '2027-01-01T00:00:00Z', {
      externalAccountIdentifiers: { obfuscatedExternalAccountId: 'x' }
    });
    const r = await purchases.checkAndroidRow({ uid: 'u1' }, 'tok', fakeApi({ kind: 'ok', data }));
    assert.strictEqual(r.rejectReason, 'account_mismatch');
  });
  await test('row check: no Play access leaves the row alone', async () => {
    const r = await purchases.checkAndroidRow({ uid: 'u1' }, 'tok', fakeApi({ kind: 'forbidden', status: 403 }));
    assert.deepStrictEqual(r, { verification: 'forbidden' });
  });

  await test('appAccountToken is a stable RFC 4122 UUID per uid', () => {
    const a = apple.appAccountTokenFor('u1');
    assert.match(a, /^[0-9a-f]{8}-[0-9a-f]{4}-5[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    assert.strictEqual(a, apple.appAccountTokenFor('u1'));
    assert.notStrictEqual(a, apple.appAccountTokenFor('u2'));
  });
  await test('obfuscated account id is 64 hex chars (Play limit)', () => {
    assert.match(play.obfuscatedAccountId('u1'), /^[0-9a-f]{64}$/);
  });

  console.log(failed ? `\n${failed} failed` : '\nall iap tests passed');
  process.exit(failed ? 1 : 0);
})();
