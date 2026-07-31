/**
 * GoThere document analysis — Claude vision.
 *
 * Takes a photo (or PDF) of an official document the user received abroad and
 * returns a structured, plain-English explanation: what it is, who sent it, any
 * time-sensitive DEADLINE (with a verbatim quote proving it), a summary, and the
 * single next step. Ported from the CartaClara core (which passed a 20/20
 * deadline-extraction + verbatim-quote eval), generalized from Spain-only to the
 * countries GoThere covers, and set to explain in English.
 *
 * Why a dedicated function (not the aiProxy chat path):
 *   - base64 images are 300KB-1MB, far over aiProxy's 256KB cap → this has its own.
 *   - output is FORCED-tool-use structured JSON, not a chat message; the schema +
 *     tool_choice live here so the client just sends an image and gets a result.
 *
 * Request (POST JSON):
 *   {
 *     "image":   { "media_type": "image/jpeg"|"image/png"|"image/webp"|"application/pdf",
 *                  "data": "<base64>" },
 *     "pages":   [ { media_type, data }, ... ],   // optional: multi-page, use INSTEAD of image
 *     "country": "spain"                            // optional hint, lowercased slug
 *   }
 * Response (200): the report_document tool input (see TOOL schema). On a model
 * that can't read the image, { "unreadable": true, ... }.
 *
 * Auth/abuse: same X-GoThere-App-Token soft-gate as aiProxy (shared
 * AI_PROXY_APP_TOKEN env). ANTHROPIC_API_KEY is read from the functions env.
 */

const { onRequest } = require('firebase-functions/v2/https');

const ANTHROPIC_ENDPOINT = 'https://api.anthropic.com/v1/messages';
const ANTHROPIC_VERSION = '2023-06-01';
const DEFAULT_MODEL = 'claude-sonnet-5';   // strong vision; override via DOC_ANALYSIS_MODEL
const MAX_TOKENS = 2000;

const APP_TOKEN_HEADER = 'x-gothere-app-token';
const MAX_BODY_BYTES = 8 * 1024 * 1024;    // 8 MB — base64 photos are big
const MAX_PAGES = 8;
const ALLOWED_MEDIA = new Set(['image/jpeg', 'image/png', 'image/webp', 'application/pdf']);

const truthy = (v) => v === 'true' || v === '1' || v === 'yes';

// --- The prompt IS the product. Safety rules ported verbatim-in-spirit from
// CartaClara: quote-or-don't-assert, anti-invention, refuse-when-unreadable,
// plain reading level, and a hard type-guard on the deadline field. ---
const SYSTEM_PROMPT = [
  "You are GoThere's document helper. Americans who are moving to or living in another country",
  "— Spain, Portugal, Italy, Germany, Ireland, Poland, Hungary, the UK, Canada, Argentina, Mexico and beyond —",
  "receive official letters and forms they cannot read: from immigration offices, tax authorities,",
  "landlords, banks, healthcare systems, utilities and courts, often in a language they don't speak.",
  "You read the document in the image and explain it in plain English by calling report_document.",
  "",
  "ABSOLUTE RULES:",
  "1. For every deadline you report, copy the exact wording from the document into 'source_quote',",
  "   in its ORIGINAL language — do not translate the quote. If you cannot quote it, do not report it.",
  "   Never guess or infer a date that is not written. A deadline is a point or period in TIME;",
  "   'date_as_written' must be a date or a time period, never a money amount, a reference/file number,",
  "   or other non-time text. If the document only shows an amount, or the image is cropped so the time",
  "   limit is not visible, report NO deadline rather than putting a non-time value in the deadline field.",
  "2. You do NOT give legal, tax, or immigration advice. You make the document understandable and point",
  "   out the time-sensitive action it asks for. Do not tell the person whether or how to respond — only",
  "   that a deadline exists and what the document asks for. Point them to the issuing office or a",
  "   qualified professional to act on it.",
  "3. If the image is unreadable, set unreadable=true and stop. A wrong answer can cost someone their",
  "   visa, their home, or a penalty; no answer is safer than a wrong one.",
  "4. Write 'summary' and 'next_step' in plain English at roughly a B1 reading level. Short sentences,",
  "   no legal jargon. If you must keep a foreign term, explain it in parentheses.",
  "",
  "Explain the meaning in clear English for summary, next_step and every deadline action. Keep each",
  "'source_quote' verbatim in the document's original language so the reader can find it on the page.",
].join('\n');

const TOOL = {
  name: 'report_document',
  description: "Report a structured, plain-English reading of the official document in the image.",
  input_schema: {
    type: 'object',
    properties: {
      unreadable: {
        type: 'boolean',
        description: 'True if the image is too blurry/dark/cropped to read reliably. If true, leave the rest empty.',
      },
      is_document: {
        type: 'boolean',
        description: 'False if this is not an official letter/form (e.g. a photo of a person, an envelope with no content, a screenshot of something unrelated).',
      },
      doc_type: {
        type: 'string',
        description: 'Short human label for the document, in English, e.g. "Residence permit renewal notice", "Traffic fine", "Tax assessment".',
      },
      category: {
        type: 'string',
        enum: ['immigration', 'tax', 'housing', 'healthcare', 'banking', 'utilities', 'legal', 'education', 'other'],
        description: 'Best-fit category for routing help.',
      },
      sender: {
        type: 'string',
        description: 'The issuing institution or company, as printed on the document.',
      },
      original_language: {
        type: 'string',
        description: 'The language the document is written in, e.g. "Spanish", "German".',
      },
      confidence: {
        type: 'string',
        enum: ['high', 'medium', 'low'],
        description: 'Your confidence in this reading.',
      },
      deadlines: {
        type: 'array',
        description: 'Every time-sensitive deadline the document states. Empty if none is written.',
        items: {
          type: 'object',
          properties: {
            date_iso: {
              type: 'string',
              description: 'The deadline as YYYY-MM-DD if an absolute date is written or can be computed from a stated period, else "".',
            },
            date_as_written: {
              type: 'string',
              description: 'The deadline exactly as written (a date or a time period like "15 business days"). MUST be a time expression, never an amount or reference number.',
            },
            source_quote: {
              type: 'string',
              description: 'The exact sentence/phrase from the document that states this deadline, VERBATIM, in the original language. Never translated.',
            },
            action: {
              type: 'string',
              description: 'In plain English: what the person must do by this deadline.',
            },
          },
          required: ['date_as_written', 'source_quote', 'action'],
        },
      },
      summary: {
        type: 'string',
        description: 'Plain-English explanation of what this document is and says, 3-5 short sentences, B1 reading level.',
      },
      next_step: {
        type: 'string',
        description: 'The single most important action the person should take, in plain English.',
      },
    },
    required: ['unreadable', 'is_document'],
  },
};

exports.handler = onRequest(
  {
    memory: '512MiB',
    timeoutSeconds: 60,
    region: 'us-central1',
    invoker: 'public',
    cors: false,
  },
  async (req, res) => {
    // CORS — native apps send no Origin and are always allowed; the app-token
    // check below gates non-browser callers. Mirror aiProxy's allowlist handling.
    const allowList = (process.env.AI_PROXY_ALLOW_ORIGIN || '*')
      .split(',').map(s => s.trim()).filter(Boolean);
    const reqOrigin = req.headers.origin;
    if (allowList.includes('*')) {
      res.setHeader('Access-Control-Allow-Origin', '*');
    } else if (reqOrigin && allowList.includes(reqOrigin)) {
      res.setHeader('Access-Control-Allow-Origin', reqOrigin);
      res.setHeader('Vary', 'Origin');
    }
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-GoThere-App-Token');

    if (req.method === 'OPTIONS') { res.status(204).send(''); return; }
    if (req.method !== 'POST') { res.status(405).json({ error: 'Method Not Allowed' }); return; }

    // App-token soft-gate (shared with aiProxy). Reject only when enforcement is on.
    const expectedToken = process.env.AI_PROXY_APP_TOKEN;
    const presentedToken = req.headers[APP_TOKEN_HEADER];
    if (expectedToken && presentedToken !== expectedToken) {
      if (truthy(process.env.AI_PROXY_REQUIRE_TOKEN)) {
        res.status(403).json({ error: 'Forbidden' });
        return;
      }
      console.warn('[analyzeDocument] untokened request (enforcement off)');
    }

    const declaredLen = Number(req.headers['content-length'] || 0);
    if (declaredLen > MAX_BODY_BYTES) {
      res.status(413).json({ error: 'Image too large. Compress or downscale it and try again.' });
      return;
    }

    const apiKey = process.env.ANTHROPIC_API_KEY;
    if (!apiKey) {
      console.error('[analyzeDocument] ANTHROPIC_API_KEY is not set');
      res.status(500).json({ error: 'Server is missing AI credentials' });
      return;
    }

    let body;
    try {
      body = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
    } catch (e) {
      res.status(400).json({ error: 'Invalid JSON body' });
      return;
    }
    if (!body || typeof body !== 'object') {
      res.status(400).json({ error: 'Body must be a JSON object' });
      return;
    }

    // Accept a single `image` or a `pages` array (consecutive pages of one doc).
    let pages = Array.isArray(body.pages) ? body.pages : (body.image ? [body.image] : []);
    pages = pages.filter(p => p && p.data && ALLOWED_MEDIA.has(p.media_type));
    if (pages.length === 0) {
      res.status(400).json({ error: 'Provide an image with a supported media_type (jpeg, png, webp, pdf).' });
      return;
    }
    if (pages.length > MAX_PAGES) pages = pages.slice(0, MAX_PAGES);

    const content = pages.map(p => (
      p.media_type === 'application/pdf'
        ? { type: 'document', source: { type: 'base64', media_type: p.media_type, data: p.data } }
        : { type: 'image', source: { type: 'base64', media_type: p.media_type, data: p.data } }
    ));
    const countryHint = typeof body.country === 'string' && body.country
      ? ` The reader is dealing with bureaucracy in ${body.country}.` : '';
    const multiPageNote = pages.length > 1
      ? ' These images are consecutive pages of the SAME document — read them together as one.' : '';
    content.push({
      type: 'text',
      text: 'Read this document and call report_document.' + countryHint + multiPageNote +
            ' Remember: every deadline needs its verbatim source_quote, and if you cannot read the image set unreadable=true.',
    });

    const upstreamBody = {
      model: process.env.DOC_ANALYSIS_MODEL || DEFAULT_MODEL,
      max_tokens: MAX_TOKENS,
      system: SYSTEM_PROMPT,
      tools: [TOOL],
      tool_choice: { type: 'tool', name: 'report_document' },
      messages: [{ role: 'user', content }],
    };

    try {
      const upstream = await fetch(ANTHROPIC_ENDPOINT, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-api-key': apiKey,
          'anthropic-version': ANTHROPIC_VERSION,
        },
        body: JSON.stringify(upstreamBody),
      });

      if (!upstream.ok) {
        const errText = await upstream.text().catch(() => '(no body)');
        console.error('[analyzeDocument] upstream error', upstream.status, errText.slice(0, 500));
        res.status(502).json({ error: 'The document reader is unavailable right now. Please try again.' });
        return;
      }

      const payload = await upstream.json();
      const toolBlock = (payload.content || []).find(
        b => b.type === 'tool_use' && b.name === 'report_document');
      if (!toolBlock || !toolBlock.input) {
        console.error('[analyzeDocument] no tool_use block in response', JSON.stringify(payload).slice(0, 400));
        res.status(502).json({ error: 'Could not read that document. Try a clearer, straight-on photo.' });
        return;
      }

      res.status(200).json(toolBlock.input);
    } catch (e) {
      console.error('[analyzeDocument] handler exception', e);
      res.status(502).json({ error: 'Document analysis failed. Please try again.' });
    }
  }
);
