/**
 * PostHog server-side capture for store lifecycle events (growth-plan 02-metrics.md):
 * subscription_renewed, subscription_cancelled, subscription_expired, refund.
 *
 * Renewals and refunds never pass through the app, so without this the PostHog
 * revenue view only ever sees first purchases. Events use the Firebase uid as
 * distinct id, the same id the apps pass to identify(), so they join the user's
 * app events. When the purchase lookup row is missing, a per-purchase id is used
 * so counts stay complete.
 *
 * Fire-and-forget like telegram.js: failures are logged, never thrown, so store
 * notification handling never fails because PostHog is down.
 */

// Public project key (the same one shipped in both apps); it can only write events.
const POSTHOG_KEY = 'phc_zdWSqHah9LiyNqn38H2i3E48XPv5acrWsXedfUWVGSLb';
const CAPTURE_URL = 'https://eu.i.posthog.com/capture/';

async function capture(event, { uid, fallbackId, platform, properties = {} }) {
  const distinctId = uid || (fallbackId ? `iap:${fallbackId}` : null);
  if (!distinctId) {
    console.warn('[posthog] no distinct id, skipped:', event);
    return;
  }
  try {
    const res = await fetch(CAPTURE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        api_key: POSTHOG_KEY,
        event,
        distinct_id: distinctId,
        properties: {
          app: 'gothere',
          platform,
          source: 'server',
          uid_known: Boolean(uid),
          ...properties
        },
        timestamp: new Date().toISOString()
      }),
      signal: AbortSignal.timeout(5000)
    });
    if (!res.ok) console.warn('[posthog] capture failed:', event, res.status);
  } catch (e) {
    console.warn('[posthog] capture error:', event, e && e.message);
  }
}

/** Apple App Store Server Notification v2 type (+subtype) → canonical event, or null. */
function appleEventName(notificationType, subtype) {
  switch (notificationType) {
    case 'DID_RENEW': return 'subscription_renewed';
    case 'DID_CHANGE_RENEWAL_STATUS':
      return subtype === 'AUTO_RENEW_DISABLED' ? 'subscription_cancelled' : null;
    case 'EXPIRED':
    case 'GRACE_PERIOD_EXPIRED': return 'subscription_expired';
    case 'REFUND':
    case 'REVOKE': return 'refund';
    default: return null;
  }
}

/** Google Play RTDN notificationType → canonical event, or null. */
function googleEventName(notificationType, isOneTime) {
  // One-time CANCELED (2) is a pending purchase that never completed, not a refund;
  // one-time refunds only show up in the Voided Purchases API.
  if (isOneTime) return null;
  switch (notificationType) {
    case 1:   // RECOVERED (paid after billing retry)
    case 2: return 'subscription_renewed';
    case 3: return 'subscription_cancelled';
    case 13: return 'subscription_expired';
    case 12: return 'refund';  // REVOKED
    default: return null;
  }
}

module.exports = { capture, appleEventName, googleEventName };
