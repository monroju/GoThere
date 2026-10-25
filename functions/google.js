/**
 * Google Play Real-Time Developer Notifications handler.
 *
 * Triggered by Pub/Sub topic published from Play Console. Operator setup:
 *   1. Create a Pub/Sub topic named `play-rtdn` in the same GCP project.
 *   2. In Play Console → Monetization setup → Real-time developer notifications,
 *      paste the topic name `projects/gothere-e5ea7/topics/play-rtdn`.
 *   3. Deploy functions; this handler subscribes automatically.
 *
 * Play sends a `DeveloperNotification` JSON payload on every subscription
 * lifecycle event. Notification types reference:
 *   1  SUBSCRIPTION_RECOVERED              → active
 *   2  SUBSCRIPTION_RENEWED                → active
 *   3  SUBSCRIPTION_CANCELED               → audit only (still active until expiration)
 *   4  SUBSCRIPTION_PURCHASED              → active
 *   5  SUBSCRIPTION_ON_HOLD                → billingRetry
 *   6  SUBSCRIPTION_IN_GRACE_PERIOD        → gracePeriod
 *   7  SUBSCRIPTION_RESTARTED              → active
 *   8  SUBSCRIPTION_PRICE_CHANGE_CONFIRMED → audit only
 *   9  SUBSCRIPTION_DEFERRED               → audit only (active continues)
 *   10 SUBSCRIPTION_PAUSED                 → expired
 *   11 SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED → audit only
 *   12 SUBSCRIPTION_REVOKED                → expired + Telegram alert (refund-like)
 *   13 SUBSCRIPTION_EXPIRED                → expired
 *   One-time products arrive as oneTimeProductNotification with their own numbering
 *   (1 PURCHASED, 2 CANCELED = pending purchase abandoned) → audit only.
 *
 * The RTDN body itself is not trusted for state: for subscriptions the handler
 * re-reads the purchase from the Play Developer API (lib/play.js), which
 * proves the token is ours and gives the real state and expiry. The mapping
 * above is only the fallback while the runtime service account has no Play
 * Console access (set PLAY_REQUIRE_VERIFIED=true to drop the fallback).
 *
 * Delivery is at-least-once and unordered: every message is recorded in
 * iapEvents/ by Pub/Sub message id and state writes are ordered per token
 * (lib/firestore.js#applyStoreEvent). Transient Play API failures throw so
 * Pub/Sub redelivers (retry: true).
 */

const { onMessagePublished } = require('firebase-functions/v2/pubsub');
const { lookupPurchase, applyStoreEvent, logEvent } = require('./lib/firestore');
const { sendAlert } = require('./lib/telegram');
const { capture, googleEventName } = require('./lib/posthog');
const play = require('./lib/play');

const STATUS_BY_TYPE = {
  1: 'active',          // RECOVERED
  2: 'active',          // RENEWED
  4: 'active',          // PURCHASED
  5: 'billingRetry',    // ON_HOLD
  6: 'gracePeriod',     // IN_GRACE_PERIOD
  7: 'active',          // RESTARTED
  10: 'expired',        // PAUSED
  12: 'expired',        // REVOKED
  13: 'expired'         // EXPIRED
};

const TYPE_NAMES = {
  1: 'SUBSCRIPTION_RECOVERED',
  2: 'SUBSCRIPTION_RENEWED',
  3: 'SUBSCRIPTION_CANCELED',
  4: 'SUBSCRIPTION_PURCHASED',
  5: 'SUBSCRIPTION_ON_HOLD',
  6: 'SUBSCRIPTION_IN_GRACE_PERIOD',
  7: 'SUBSCRIPTION_RESTARTED',
  8: 'SUBSCRIPTION_PRICE_CHANGE_CONFIRMED',
  9: 'SUBSCRIPTION_DEFERRED',
  10: 'SUBSCRIPTION_PAUSED',
  11: 'SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED',
  12: 'SUBSCRIPTION_REVOKED',
  13: 'SUBSCRIPTION_EXPIRED'
};

const ONE_TIME_TYPE_NAMES = {
  1: 'ONE_TIME_PRODUCT_PURCHASED',
  2: 'ONE_TIME_PRODUCT_CANCELED'
};

// Refund-like subscription events that need an operator Telegram ping.
const REFUND_LIKE_TYPES = new Set([12]);

const truthy = (v) => v === 'true' || v === '1' || v === 'yes';

/**
 * Works out the state to write for a subscription token.
 * Returns { status, expiresAtSeconds, productId, orderMillis, verification }.
 * Throws on transient Play API errors so Pub/Sub redelivers.
 */
async function resolveSubscriptionState({ notificationType, purchaseToken, packageName, eventTimeMillis, row }, api = play) {
  const fallback = {
    status: STATUS_BY_TYPE[notificationType] || null,
    expiresAtSeconds: null,
    productId: null,
    orderMillis: eventTimeMillis,
    verification: 'payload'
  };

  const result = await api.getSubscription(purchaseToken, packageName || api.PACKAGE_NAME);
  if (result.kind === 'transient') {
    throw new Error(`Play API transient failure (${result.status}): ${result.error || ''}`);
  }
  if (result.kind === 'forbidden') {
    console.warn('[google] Play API access denied (%s); grant the Functions service account Play Console access. Using payload.', result.status);
    return truthy(process.env.PLAY_REQUIRE_VERIFIED)
      ? { ...fallback, status: null, verification: 'forbidden' }
      : fallback;
  }
  if (result.kind === 'invalid') {
    console.warn('[google] Play API does not know this token (%s); ignoring notification.', result.status);
    return { ...fallback, status: null, verification: 'invalid_token' };
  }

  const data = result.data;
  const accountId = api.accountIdOf(data);
  if (row && accountId && accountId !== api.obfuscatedAccountId(row.uid)) {
    console.error('[google] purchase is bound to a different account than its iapPurchases row; not applying.');
    return { ...fallback, status: null, verification: 'account_mismatch' };
  }
  const expiresAtMillis = api.latestExpiryMillis(data);
  return {
    status: api.statusFromSubscription(data),
    expiresAtSeconds: expiresAtMillis ? Math.floor(expiresAtMillis / 1000) : null,
    productId: api.productIdOf(data),
    // The API answer describes "now", so it outranks any earlier event.
    orderMillis: Date.now(),
    verification: 'play_api'
  };
}

exports.handler = onMessagePublished(
  { topic: 'play-rtdn', memory: '256MiB', timeoutSeconds: 30, region: 'us-central1', retry: true },
  async (event) => {
    // Functions v2 wraps the Pub/Sub message under event.data.message.
    const message = event && event.data && event.data.message;
    const messageId = (message && message.messageId) || (event && event.id) || null;
    let payload;
    try {
      const raw = message && message.data
        ? Buffer.from(message.data, 'base64').toString('utf8')
        : '';
      payload = JSON.parse(raw);
    } catch (e) {
      // Not retryable: a malformed message stays malformed.
      console.error('[google] failed to decode RTDN payload:', e);
      return;
    }

    if (payload.packageName && payload.packageName !== play.PACKAGE_NAME) {
      console.warn('[google] RTDN for unexpected package %s; ignoring.', payload.packageName);
      return;
    }

    // Three possible notification kinds — only one is set per message.
    const subN = payload.subscriptionNotification;
    const oneTimeN = payload.oneTimeProductNotification;
    const testN = payload.testNotification;

    if (testN) {
      await logEvent({
        source: 'google',
        type: 'TEST',
        payload
      });
      return;
    }

    const notification = subN || oneTimeN;
    if (!notification) {
      console.warn('[google] RTDN payload missing both subscriptionNotification and oneTimeProductNotification:', payload);
      return;
    }

    const notificationType = notification.notificationType;
    // oneTimeProductNotification uses its own numbering (1 PURCHASED, 2 CANCELED), which
    // collides with the subscription types; never read it through TYPE_NAMES/STATUS_BY_TYPE.
    const typeName = oneTimeN
      ? (ONE_TIME_TYPE_NAMES[notificationType] || `ONE_TIME_TYPE_${notificationType}`)
      : (TYPE_NAMES[notificationType] || `TYPE_${notificationType}`);
    const purchaseToken = notification.purchaseToken;
    const eventTimeMillis = Number(payload.eventTimeMillis) || Date.now();

    const row = await lookupPurchase(purchaseToken);
    const uid = row ? row.uid : null;

    let state = { status: null, expiresAtSeconds: null, productId: null, orderMillis: eventTimeMillis, verification: 'one_time' };
    if (!oneTimeN && purchaseToken) {
      state = await resolveSubscriptionState({
        notificationType, purchaseToken, packageName: payload.packageName, eventTimeMillis, row
      });
    }
    const productId = state.productId || notification.subscriptionId || notification.sku;

    if (state.status && !uid) {
      console.warn('[google] no iapPurchases lookup row for purchaseToken:', purchaseToken);
    }

    const outcome = await applyStoreEvent({
      source: 'google',
      eventId: messageId,
      stateKey: purchaseToken,
      orderMillis: state.orderMillis,
      uid,
      status: state.status,
      expiresAtSeconds: state.expiresAtSeconds
    });
    if (outcome === 'duplicate') {
      console.log('[google] duplicate delivery %s skipped', messageId);
      return;
    }

    await logEvent({
      source: 'google',
      type: typeName,
      uid,
      productId,
      payload: {
        notificationType,
        purchaseToken,
        packageName: payload.packageName,
        eventTimeMillis: payload.eventTimeMillis,
        verification: state.verification,
        appliedStatus: state.status,
        expiresAtSeconds: state.expiresAtSeconds,
        outcome
      }
    });

    const phEvent = googleEventName(notificationType, Boolean(oneTimeN));
    if (phEvent) {
      await capture(phEvent, {
        uid,
        fallbackId: purchaseToken ? purchaseToken.slice(0, 32) : null,
        platform: 'android',
        properties: { product_id: productId, store_type: typeName }
      });
    }

    if (!oneTimeN && REFUND_LIKE_TYPES.has(notificationType)) {
      await sendAlert([
        `🚨 *GoThere Play ${typeName}*`,
        `Product: \`${productId || 'unknown'}\``,
        `User: \`${uid || 'unknown'}\``,
        `Token: \`${purchaseToken ? purchaseToken.slice(0, 16) + '…' : 'unknown'}\``
      ].join('\n'));
    }
  });

exports._internal = { resolveSubscriptionState, STATUS_BY_TYPE };
