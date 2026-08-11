import assert from 'node:assert/strict'
import test from 'node:test'

import { resolveRouterMode } from './router-mode.ts'

test('resolveRouterMode accepts configured hash and history modes', () => {
  assert.equal(resolveRouterMode('hash'), 'hash')
  assert.equal(resolveRouterMode('history'), 'history')
})

test('resolveRouterMode rejects unsupported modes instead of silently falling back', () => {
  assert.throws(() => resolveRouterMode('memory'), /VITE_ROUTER_MODE/)
  assert.throws(() => resolveRouterMode(''), /VITE_ROUTER_MODE/)
})
