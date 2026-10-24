/**
 * Spend guard shared by aiProxy and analyzeDocument.
 *
 * Identity: the apps send `Authorization: Bearer <Firebase ID token>`. A valid
 * token keys the quota on the uid. Without one (older builds, or a signed-out
 * session) the request is keyed on the caller IP, with a smaller allowance.
 * AI_REQUIRE_AUTH=true rejects untokened requests outright; leave it off until
 * the builds that send the header are live on both stores.
 *
 * Quota: one Firestore counter per key per UTC day in `aiUsage/` (server-only,
 * no client rule matches it). Limits are per endpoint and overridable via env.
 */

const admin = require('firebase-admin');
const crypto = require('crypto');

const truthy = (v) => v === 'true' || v === '1' || v === 'yes';

const DEFAULT_LIMITS = {
  aiProxy: { user: 150, anon: 40 },
  analyzeDocument: { user: 25, anon: 5 }
};

function envLimit(endpoint, kind) {
  const name = `AI_LIMIT_${endpoint.toUpperCase()}_${kind.toUpperCase()}`;
  const v = Number(process.env[name]);
  return Number.isFinite(v) && v > 0 ? v : DEFAULT_LIMITS[endpoint][kind];
}

async function resolveUid(req) {
  const header = req.headers.authorization || '';
  const match = /^Bearer\s+(.+)$/i.exec(header);
  if (!match) return null;
  try {
    const decoded = await admin.auth().verifyIdToken(match[1]);
    return decoded.uid;
  } catch (e) {
    console.warn('[aiGuard] invalid ID token:', e.code || e.message);
    return null;
  }
}

function clientIp(req) {
  const fwd = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
  return fwd || req.ip || 'unknown';
}

/**
 * Returns true when the request may proceed. Otherwise it has already sent
 * the response (401 or 429) and the caller must return.
 */
async function guard(req, res, endpoint) {
  const uid = await resolveUid(req);
  if (!uid && truthy(process.env.AI_REQUIRE_AUTH)) {
    res.status(401).json({ error: 'Sign in to use this feature.', code: 'auth_required' });
    return false;
  }

  const kind = uid ? 'user' : 'anon';
  const limit = envLimit(endpoint, kind);
  // IPs are hashed so the collection never stores raw addresses.
  const who = uid
    ? `u_${uid}`
    : `ip_${crypto.createHash('sha256').update(clientIp(req)).digest('hex').slice(0, 24)}`;
  const day = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const ref = admin.firestore().collection('aiUsage').doc(`${endpoint}_${day}_${who}`);

  let allowed = true;
  try {
    allowed = await admin.firestore().runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      const count = snap.exists ? (snap.get('count') || 0) : 0;
      if (count >= limit) return false;
      tx.set(ref, {
        count: count + 1,
        endpoint,
        kind,
        day,
        // Lets a Firestore TTL policy on `expireAt` clean old counters up.
        expireAt: admin.firestore.Timestamp.fromMillis(Date.now() + 3 * 86400 * 1000)
      }, { merge: true });
      return true;
    });
  } catch (e) {
    // Never take the feature down because the counter failed; maxInstances
    // still bounds the worst case.
    console.error('[aiGuard] quota check failed, allowing:', e.message);
    return true;
  }

  if (!allowed) {
    console.warn('[aiGuard] daily limit hit endpoint=%s kind=%s', endpoint, kind);
    res.status(429).json({
      error: 'You have reached today’s limit for this feature. Try again tomorrow.',
      code: 'daily_limit'
    });
    return false;
  }
  return true;
}

module.exports = { guard, _internal: { resolveUid, clientIp, envLimit, DEFAULT_LIMITS } };
