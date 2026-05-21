/**
 * Smoke tests for aiProxy.js — exercise the parts that don't require a live
 * Anthropic key or a deployed Cloud Function. Anything that hits the real
 * API is excluded from CI (no live API per Wave 2 brief).
 *
 * Run with: node aiProxy.test.js
 * (Plain assertions, no test framework — keeps `npm install` minimal.)
 */

const assert = require('node:assert');
const { _internal } = require('./aiProxy');

let failures = 0;
function test(name, fn) {
  try {
    fn();
    console.log(`ok  ${name}`);
  } catch (e) {
    failures += 1;
    console.error(`FAIL ${name}: ${e.message}`);
  }
}

test('v1 system prompt exists', () => {
  assert.ok(_internal.SYSTEM_PROMPTS.v1, 'v1 prompt must be defined');
  assert.ok(_internal.SYSTEM_PROMPTS.v1.length > 200, 'v1 prompt must be substantive (>200 chars)');
});

test('v1 system prompt includes informational-only framing', () => {
  const prompt = _internal.SYSTEM_PROMPTS.v1.toLowerCase();
  assert.ok(prompt.includes('informational only'),
            'v1 prompt must include the "informational only" frame');
  assert.ok(prompt.includes('lawyer') || prompt.includes('legal'),
            'v1 prompt must point users at a lawyer / legal counsel');
});

test('v1 system prompt mentions in-flux tracks by name', () => {
  const prompt = _internal.SYSTEM_PROMPTS.v1;
  assert.ok(prompt.includes('DL 36/2025'),
            'v1 prompt must reference Italy DL 36/2025 so the model surfaces the warning');
  assert.ok(prompt.includes('Bill C-3'),
            'v1 prompt must reference Canada Bill C-3 for the in-flux warning');
});

test('default model is the current best Sonnet at commit time', () => {
  assert.strictEqual(_internal.DEFAULT_MODEL, 'claude-sonnet-4-6',
                     'Default model pin must match the iOS client expectation');
});

test('default max_tokens is sane for a chat turn', () => {
  assert.ok(_internal.DEFAULT_MAX_TOKENS >= 512,
            'max_tokens must be high enough for a meaningful answer');
  assert.ok(_internal.DEFAULT_MAX_TOKENS <= 8192,
            'max_tokens must be low enough to keep per-turn cost predictable');
});

if (failures > 0) {
  console.error(`\n${failures} test(s) failed`);
  process.exit(1);
} else {
  console.log('\nall tests passed');
}
