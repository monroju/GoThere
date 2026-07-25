/**
 * Referral helpers — code allocation + time-boxed premium grants.
 *
 * The referral reward is a server-granted "free month" that is NOT a store
 * subscription. It lives in a dedicated `users/{uid}.promoAccessUntil` field
 * (unix seconds). The clients OR this into their local `hasAllAccess` gate and,
 * crucially, their launch-time StoreKit/Play reconcile must NOT clobber it (a
 * referral grant has no real store receipt behind it). This mirrors the existing
 * time-boxed FirstWeekTrialService pattern, just driven from the server.
 *
 *   • `referralCodes/{code}` — { ownerUid, createdAt }. Server-only (Admin SDK);
 *     locked in firestore.rules. Maps a shareable code back to its owner.
 *   • `users/{uid}.referralCode` — the owner's own code, mirrored so the client
 *     can display/share it by reading its own user doc (no extra round-trip).
 *
 * Grants extend from max(now, existing promoAccessUntil) so stacking two
 * referrals gives 60 days, never overwrites remaining time.
 */

const admin = require('firebase-admin');

// Unambiguous alphabet — no 0/O/1/I/L so codes are easy to read aloud / retype.
const CODE_ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
const CODE_LEN = 7;
const SECONDS_PER_DAY = 86400;

function randomCode() {
  let s = '';
  for (let i = 0; i < CODE_LEN; i++) {
    s += CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)];
  }
  return s;
}

/**
 * Returns the caller's referral code, creating one on first call. Idempotent and
 * race-safe: the read-existing + allocate happens inside a transaction on the
 * user doc, so two concurrent calls can't mint two codes for the same uid.
 */
async function getOrCreateCode(uid) {
  const db = admin.firestore();
  const userRef = db.collection('users').doc(uid);
  for (let attempt = 0; attempt < 6; attempt++) {
    const code = randomCode();
    const codeRef = db.collection('referralCodes').doc(code);
    const result = await db.runTransaction(async (tx) => {
      const userSnap = await tx.get(userRef);
      const existing = userSnap.get('referralCode');
      if (existing) return existing;
      const collision = await tx.get(codeRef);
      if (collision.exists) return null; // extremely rare — retry with a new code
      tx.set(codeRef, {
        ownerUid: uid,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
      tx.set(userRef, { referralCode: code }, { merge: true });
      return code;
    });
    if (result) return result;
  }
  throw new Error('could not allocate a unique referral code after retries');
}

/**
 * Extends `users/{uid}.promoAccessUntil` by `days`, starting from whichever is
 * later: now, or the user's current promo expiry. Also sets `hasAllAccess:true`
 * as a convenience for server-side reads (clients recompute it locally). Returns
 * the new expiry in unix seconds.
 */
async function grantPromoDays(uid, days) {
  const db = admin.firestore();
  const userRef = db.collection('users').doc(uid);
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(userRef);
    const nowSec = Math.floor(Date.now() / 1000);
    const currentRaw = snap.get('promoAccessUntil');
    const current = typeof currentRaw === 'number' ? currentRaw : 0;
    const until = Math.max(nowSec, current) + days * SECONDS_PER_DAY;
    tx.set(userRef, { promoAccessUntil: until, hasAllAccess: true }, { merge: true });
    return until;
  });
}

module.exports = { getOrCreateCode, grantPromoDays, randomCode, CODE_ALPHABET, CODE_LEN };
