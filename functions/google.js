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
 *   20 ONE_TIME_PRODUCT_PURCHASED          → audit only (handled by client)
 *   21 ONE_TIME_PRODUCT_CANCELED           → audit only + Telegram alert (refund-like)
 *
 * The mapping from notificationType → SubscriptionStatus mirrors the Apple
 * mapping (`apple.js#mapToStatus`) where the semantics overlap.
 */

const functions = require('firebase-functions');
const { lookupUidByPurchase, writeSubscriptionStatus, logEvent } = require('./lib/firestore');
const { sendAlert } = require('./lib/telegram');

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
  13: 'SUBSCRIPTION_EXPIRED',
  20: 'ONE_TIME_PRODUCT_PURCHASED',
  21: 'ONE_TIME_PRODUCT_CANCELED'
};

// Refund-like events that need an operator Telegram ping.
const REFUND_LIKE_TYPES = new Set([12, 21]);

exports.handler = functions
  .runWith({ memory: '256MB', timeoutSeconds: 30 })
  .pubsub.topic('play-rtdn').onPublish(async (message) => {
    let payload;
    try {
      payload = JSON.parse(Buffer.from(message.data, 'base64').toString('utf8'));
    } catch (e) {
      console.error('[google] failed to decode RTDN payload:', e);
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
    const typeName = TYPE_NAMES[notificationType] || `TYPE_${notificationType}`;
    const purchaseToken = notification.purchaseToken;
    const productId = notification.subscriptionId || notification.sku;

    const uid = await lookupUidByPurchase(purchaseToken);
    const newStatus = STATUS_BY_TYPE[notificationType];

    if (newStatus) {
      if (uid) {
        // Play doesn't include expiration in the RTDN body — would need a
        // Play Developer API call to get it. Until that's wired (separate
        // follow-up), write Active without an expiresAt; the client will
        // surface as Active(expiresAtSeconds=null) which is fine for gating.
        await writeSubscriptionStatus(uid, newStatus, null);
      } else {
        console.warn('[google] no iapPurchases lookup row for purchaseToken:', purchaseToken);
      }
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
        eventTimeMillis: payload.eventTimeMillis
      }
    });

    if (REFUND_LIKE_TYPES.has(notificationType)) {
      await sendAlert([
        `🚨 *GoThere Play ${typeName}*`,
        `Product: \`${productId || 'unknown'}\``,
        `User: \`${uid || 'unknown'}\``,
        `Token: \`${purchaseToken ? purchaseToken.slice(0, 16) + '…' : 'unknown'}\``
      ].join('\n'));
    }
  });
