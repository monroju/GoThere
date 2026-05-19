/**
 * GoThere IAP notification handlers — entry point.
 *
 * Two exports:
 *   • appleIap   — HTTPS function receiving Apple ASC Server Notifications v2 (JWS).
 *   • googleIap  — Pub/Sub function receiving Google Play RTDN messages.
 *
 * Both reconcile the user's `users/{uid}.subscriptionStatus` field on Firestore
 * and append an audit entry to `crew-notifications/`. REFUND events also fire
 * a Telegram alert to the operator (token + chat id in runtime config; see
 * README.md for `firebase functions:config:set` instructions).
 */

const admin = require('firebase-admin');

admin.initializeApp();

exports.appleIap = require('./apple').handler;
exports.googleIap = require('./google').handler;
