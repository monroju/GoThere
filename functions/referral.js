/**
 * GoThere referral loop — callable functions.
 *
 *   • getReferralCode — returns the caller's shareable code (creating it on first
 *     call) plus a share URL and the reward size.
 *   • redeemReferral  — a new member redeems a friend's code; both the member and
 *     the referrer get REWARD_DAYS added to their promoAccessUntil.
 *
 * Both are v2 callable (onCall) so Firebase Auth is enforced automatically:
 * request.auth.uid is the verified Firebase uid, the same key every premium
 * write is already keyed on.
 *
 * Anti-abuse (honest-client posture — the entitlement mirror is already
 * client-trusted app-wide; App Check is the real hardening, tracked separately):
 *   - one redemption per account (users/{uid}.referredBy, set transactionally)
 *   - no self-referral
 *   - members with an ACTIVE paid subscription can't redeem (reward is for
 *     bringing new people to premium, not discounting existing subscribers)
 *   - per-referrer cap (OWNER_REWARD_CAP) so one code can't farm unlimited months
 */

const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');
const { getOrCreateCode, grantPromoDays } = require('./lib/referrals');
const { logEvent } = require('./lib/firestore');
const { sendAlert } = require('./lib/telegram');

const REWARD_DAYS = 30;
const OWNER_REWARD_CAP = 24; // max referral months a single code can earn its owner

const OPTS = { region: 'us-central1', memory: '256MiB', timeoutSeconds: 30 };

exports.getReferralCode = onCall(OPTS, async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Sign in to get your referral code.');

  const code = await getOrCreateCode(uid);
  return {
    code,
    shareUrl: `https://getgothere.app/invite?code=${code}`,
    rewardDays: REWARD_DAYS
  };
});

exports.redeemReferral = onCall(OPTS, async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Sign in to redeem a code.');

  const code = String((request.data && request.data.code) || '').trim().toUpperCase();
  if (!code) throw new HttpsError('invalid-argument', 'Enter a referral code.');

  const db = admin.firestore();

  const codeSnap = await db.collection('referralCodes').doc(code).get();
  if (!codeSnap.exists) throw new HttpsError('not-found', 'That code doesn’t exist.');
  const ownerUid = codeSnap.get('ownerUid');
  if (!ownerUid) throw new HttpsError('not-found', 'That code is invalid.');
  if (ownerUid === uid) throw new HttpsError('failed-precondition', 'You can’t redeem your own code.');

  const redeemerRef = db.collection('users').doc(uid);

  // Claim the redemption atomically so a double-tap / race can't grant twice.
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(redeemerRef);
    if (snap.get('referredBy')) {
      throw new HttpsError('already-exists', 'You’ve already redeemed a referral code.');
    }
    const sub = snap.get('subscriptionStatus');
    if (sub && sub.state === 'active') {
      throw new HttpsError('failed-precondition',
        'Referral months are for new members — you already have an active subscription.');
    }
    tx.set(redeemerRef, {
      referredBy: code,
      referredByUid: ownerUid,
      referredAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
  });

  // Referrer reward, capped so a single code can't be farmed indefinitely.
  const ownerRef = db.collection('users').doc(ownerUid);
  const ownerSnap = await ownerRef.get();
  const ownerGrants = ownerSnap.get('referralGrantsCount') || 0;
  const ownerGetsReward = ownerGrants < OWNER_REWARD_CAP;

  const memberUntil = await grantPromoDays(uid, REWARD_DAYS);
  if (ownerGetsReward) {
    await grantPromoDays(ownerUid, REWARD_DAYS);
    await ownerRef.set(
      { referralGrantsCount: admin.firestore.FieldValue.increment(1) }, { merge: true });
  }

  await logEvent({
    source: 'referral', type: 'redeem', uid, productId: code,
    payload: { ownerUid, ownerGetsReward, rewardDays: REWARD_DAYS }
  });
  sendAlert(`\u{1F381} *GoThere referral redeemed*\nCode: \`${code}\`\nNew member: \`${uid}\`\n` +
            `Referrer: \`${ownerUid}\`${ownerGetsReward ? '' : ' (cap reached — no referrer reward)'}`);

  return { rewardDays: REWARD_DAYS, premiumUntil: memberUntil, referrerRewarded: ownerGetsReward };
});
