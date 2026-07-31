/**
 * GoThere Firebase Functions — entry point.
 *
 * Exports:
 *   • appleIap   — HTTPS function receiving Apple ASC Server Notifications v2 (JWS).
 *   • googleIap  — Pub/Sub function receiving Google Play RTDN messages.
 *   • aiProxy    — HTTPS function proxying iOS/Android AI chat to Anthropic,
 *                  adding the API key + the stored system prompt server-side.
 *   • getReferralCode / redeemReferral — callable functions powering the
 *                  give-a-month/get-a-month referral loop. Grant a server-side
 *                  time-boxed premium window (users/{uid}.promoAccessUntil).
 *
 * The IAP handlers reconcile the user's `users/{uid}.subscriptionStatus`
 * field on Firestore and append an audit entry to `crew-notifications/`.
 * REFUND events also fire a Telegram alert to the operator (token + chat id
 * in runtime config; see README.md for `firebase functions:config:set`).
 *
 * The aiProxy function never touches Firestore — it's a stateless forwarder
 * to api.anthropic.com. The key is read from `ANTHROPIC_API_KEY` in the
 * functions environment.
 */

const admin = require('firebase-admin');

admin.initializeApp();

exports.appleIap = require('./apple').handler;
exports.googleIap = require('./google').handler;
exports.aiProxy = require('./aiProxy').handler;
exports.analyzeDocument = require('./analyzeDocument').handler;

const referral = require('./referral');
exports.getReferralCode = referral.getReferralCode;
exports.redeemReferral = referral.redeemReferral;
