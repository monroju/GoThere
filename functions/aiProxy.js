/**
 * GoThere AI conversational entry point — proxy.
 *
 * Implements the Wave 2 client contract documented in
 * `Projects/GoThere/ios/WAVE_PROMPTS.md` ("Wave 2 AI proxy — deployment
 * contract"). The iOS client (and a future Android client) POSTs a Messages-
 * API-shaped body here; this function adds the API key + the stored system
 * prompt server-side and forwards to Anthropic. The key NEVER leaves the
 * server.
 *
 * Endpoint after deploy:
 *   https://us-central1-gothere-e5ea7.cloudfunctions.net/aiProxy
 *
 * The iOS client expects `https://api.gothere.app/ai/messages` — operator
 * either:
 *   (a) Wires `api.gothere.app/ai` → this function via Firebase Hosting
 *       rewrite on a dedicated `api-gothere` site (see README), OR
 *   (b) Overrides the client URL via UserDefaults key `ai_proxy_url_override`
 *       during early testing (development builds only).
 *
 * Request shape:
 *   {
 *     "system_prompt_version": "v1",
 *     "messages": [ { id, role, content: [...] }, ... ],
 *     "tools":    [ { name, description, input_schema: {...} }, ... ]
 *   }
 *
 * Response shape (non-streaming, default):
 *   {
 *     "message": { id, role: "assistant", content: [...] },
 *     "stop_reason": "end_turn" | "tool_use" | "max_tokens" | ...,
 *     "usage": { input_tokens: N, output_tokens: M }
 *   }
 *
 * Streaming (Accept: text/event-stream):
 *   Anthropic SSE events are passed through unchanged. The iOS
 *   `AIStreamHandler` parses them. No buffering on this side.
 *
 * Tools: this proxy never executes tools. The model emits `tool_use`
 * content blocks; the iOS app executes them on-device and loops back with
 * matching `tool_result` blocks in the next user-role message.
 *
 * Environment:
 *   ANTHROPIC_API_KEY   required  — Anthropic API key (set in functions/.env)
 *   ANTHROPIC_MODEL     optional  — defaults to `claude-sonnet-4-6`
 *   ANTHROPIC_MAX_TOKENS optional — defaults to 2048
 *   AI_PROXY_ALLOW_ORIGIN optional — CORS allow-origin; default '*' for
 *     dev. Tighten to https://getgothere.app + the App Store webview origin
 *     in production once we know the exact value.
 */

const { onRequest } = require('firebase-functions/v2/https');

/**
 * Stored system prompts. Bump the key + add a new entry when the framing
 * changes. The iOS client pins `system_prompt_version: "v1"` today; the proxy
 * resolves to the matching entry here. Unknown versions fall back to V1 with
 * a warning header so older builds keep working.
 */
const SYSTEM_PROMPTS = {
  v1: [
    "You are GoThere's relocation assistant. GoThere is an iOS/Android app that helps people plan international moves with structured visa-by-visa checklists, document tracking, and cost-of-living calculators across Spain, Portugal, Mexico, Canada, Italy, Ireland, Germany, Poland, Hungary, Argentina, and the UK (Ancestry route).",
    "",
    "CORE BEHAVIOUR",
    "- The user opened the 'I don't know where to start' tile because they want a concrete next step, not a generic chatbot.",
    "- Use the provided tools (`recommend_visas`, `list_cities_for_country`, `list_wizard_tracks_for_country`) whenever the user mentions a destination, household shape, budget, or ancestry — they surface GoThere's bundled structured data. Don't speculate when a tool can answer.",
    "- Surface in-flux warnings prominently when a wizard track is marked `in_flux: true` (currently Italy Jure Sanguinis under DL 36/2025 + Law 74/2025 and Canada by-descent under Bill C-3). Quote the `in_flux_note` text when the user shows interest.",
    "",
    "FRAMING",
    "- Informational only. Never give specific legal, tax, or medical advice — frame as \"here's what GoThere has on file\" and route the user to a licensed lawyer / cross-border tax advisor / doctor for binding answers.",
    "- When citing an income threshold, visa duration, fee, or processing time, append a short \"Verify with the official source before relying on this\" reminder. Don't repeat it in every sentence — once per topic is enough.",
    "- Don't decide anyone's personal eligibility. Use the tools to surface the structured criteria; ask the user to self-check against them.",
    "",
    "TONE",
    "- Conversational, concrete, friendly. Not corporate. Not over-hedged — the disclaimer is in the banner above the conversation.",
    "- Short paragraphs and bullet lists when surfacing multiple options. Numbers and visa names in bold-ish prose; the chat view renders Markdown.",
    "",
    "WHAT YOU CAN'T DO",
    "- You don't have web access, current-day exchange rates, real-time consulate appointment availability, or the user's identity.",
    "- If the user asks \"is this still true today?\" remind them you can only speak from GoThere's bundled data and point them at the official source URL the app surfaces.",
    "- You don't process payments, sign documents, or contact consulates."
  ].join('\n')
};

const ANTHROPIC_ENDPOINT = 'https://api.anthropic.com/v1/messages';
const DEFAULT_MODEL = 'claude-sonnet-4-6';
const DEFAULT_MAX_TOKENS = 2048;
const ANTHROPIC_VERSION = '2023-06-01';

exports.handler = onRequest(
  {
    memory: '256MiB',
    timeoutSeconds: 60,
    region: 'us-central1',
    invoker: 'public',  // Open endpoint; rate-limit at the model level only
    cors: false          // We set headers manually for the SSE case
  },
  async (req, res) => {
    const allowOrigin = process.env.AI_PROXY_ALLOW_ORIGIN || '*';
    res.setHeader('Access-Control-Allow-Origin', allowOrigin);
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Accept, X-System-Prompt-Version');

    if (req.method === 'OPTIONS') {
      res.status(204).send('');
      return;
    }
    if (req.method !== 'POST') {
      res.status(405).json({ error: 'Method Not Allowed' });
      return;
    }

    const apiKey = process.env.ANTHROPIC_API_KEY;
    if (!apiKey) {
      console.error('[aiProxy] ANTHROPIC_API_KEY is not set');
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

    const promptVersion = body.system_prompt_version || 'v1';
    const systemPrompt = SYSTEM_PROMPTS[promptVersion] || SYSTEM_PROMPTS.v1;
    if (!SYSTEM_PROMPTS[promptVersion]) {
      res.setHeader('X-System-Prompt-Fallback', 'v1');
    }

    const messages = Array.isArray(body.messages) ? body.messages : null;
    if (!messages || messages.length === 0) {
      res.status(400).json({ error: 'messages array is required' });
      return;
    }
    const tools = Array.isArray(body.tools) ? body.tools : [];

    // Translate the iOS wire shape to the Anthropic Messages API shape.
    // The iOS payload already uses the Messages-API block layout (text /
    // tool_use / tool_result); we strip the iOS-specific message id and
    // forward the rest as-is. `tools` is passed through verbatim — its
    // input_schema field uses the same JSON Schema vocabulary Anthropic
    // expects.
    const upstreamMessages = messages.map(m => ({ role: m.role, content: m.content }));
    const upstreamTools = tools.map(t => ({
      name: t.name,
      description: t.description,
      input_schema: t.input_schema || t.inputSchema  // tolerate either casing
    }));

    const wantsStream = (req.headers.accept || '').toLowerCase().includes('text/event-stream');

    const upstreamBody = {
      model: process.env.ANTHROPIC_MODEL || DEFAULT_MODEL,
      max_tokens: Number(process.env.ANTHROPIC_MAX_TOKENS || DEFAULT_MAX_TOKENS),
      system: systemPrompt,
      messages: upstreamMessages,
      tools: upstreamTools.length ? upstreamTools : undefined,
      stream: wantsStream || undefined
    };

    try {
      const upstream = await fetch(ANTHROPIC_ENDPOINT, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-api-key': apiKey,
          'anthropic-version': ANTHROPIC_VERSION,
          'Accept': wantsStream ? 'text/event-stream' : 'application/json'
        },
        body: JSON.stringify(upstreamBody)
      });

      if (!upstream.ok) {
        const errText = await upstream.text().catch(() => '(no body)');
        console.error('[aiProxy] upstream error', upstream.status, errText.slice(0, 500));
        res.status(upstream.status).json({
          error: 'Upstream model error',
          status: upstream.status,
          detail: errText.slice(0, 1000)
        });
        return;
      }

      if (wantsStream) {
        // SSE pass-through. Anthropic emits Server-Sent Events; we forward
        // the byte stream untouched so the iOS AIStreamHandler can parse it.
        res.setHeader('Content-Type', 'text/event-stream');
        res.setHeader('Cache-Control', 'no-cache, no-transform');
        res.setHeader('Connection', 'keep-alive');
        res.setHeader('X-Accel-Buffering', 'no');
        res.flushHeaders && res.flushHeaders();

        const reader = upstream.body && upstream.body.getReader
          ? upstream.body.getReader()
          : null;
        if (!reader) {
          // Older Node/undici may expose body as a Node stream — fall back.
          upstream.body.on('data', chunk => res.write(chunk));
          upstream.body.on('end', () => res.end());
          upstream.body.on('error', e => {
            console.error('[aiProxy] stream error', e);
            res.end();
          });
          return;
        }
        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            res.write(Buffer.from(value));
          }
        } finally {
          res.end();
        }
        return;
      }

      // Non-streaming JSON path. Re-shape Anthropic's response into the
      // contract the iOS AIProxyResponse decoder expects.
      const upstreamJson = await upstream.json();
      const out = {
        message: {
          id: upstreamJson.id,
          role: upstreamJson.role || 'assistant',
          content: upstreamJson.content || []
        },
        stop_reason: upstreamJson.stop_reason || null,
        usage: upstreamJson.usage
          ? {
              input_tokens: upstreamJson.usage.input_tokens,
              output_tokens: upstreamJson.usage.output_tokens
            }
          : null
      };
      res.status(200).json(out);
    } catch (e) {
      console.error('[aiProxy] handler exception', e);
      res.status(502).json({
        error: 'AI proxy failed',
        message: (e && e.message) || String(e)
      });
    }
  }
);

// Exported for unit testing.
exports._internal = { SYSTEM_PROMPTS, DEFAULT_MODEL, DEFAULT_MAX_TOKENS };
