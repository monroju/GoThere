/**
 * Telegram alert helper.
 *
 * Token + chat id come from Firebase Functions runtime config:
 *   firebase functions:config:set telegram.token="..." telegram.chat_id="..."
 *
 * Used by the IAP handlers to surface REFUND events to the operator —
 * refunds need eyes on them (chargeback risk, fraud signal, or a bug in the
 * paywall that misled the user). Other notification types just go to the
 * Firestore audit log.
 *
 * The helper is fire-and-forget — failures are logged but never thrown so
 * notification handling never blocks on Telegram availability.
 */

const functions = require('firebase-functions');

const API_BASE = 'https://api.telegram.org';

function readConfig() {
  // Prefer the v2 runtime env vars when set; fall back to legacy
  // `functions.config()` for backward compat with older deployments.
  const token = process.env.TELEGRAM_BOT_TOKEN
    || (functions.config().telegram && functions.config().telegram.token);
  const chatId = process.env.TELEGRAM_CHAT_ID
    || (functions.config().telegram && functions.config().telegram.chat_id);
  return { token, chatId };
}

async function sendAlert(message) {
  const { token, chatId } = readConfig();
  if (!token || !chatId) {
    console.warn('[telegram] missing token/chat_id — alert skipped:', message);
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/bot${token}/sendMessage`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chat_id: chatId,
        text: message,
        parse_mode: 'Markdown',
        disable_web_page_preview: true
      })
    });
    if (!res.ok) {
      console.warn('[telegram] non-OK response:', res.status, await res.text());
    }
  } catch (e) {
    console.warn('[telegram] send failed:', e.message);
  }
}

module.exports = { sendAlert };
