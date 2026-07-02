/// <reference types="node" />

import assert from 'node:assert/strict'
import test from 'node:test'

import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'
import { getVisualScreenChartWrapperStyle } from './index.ts'

const chart = (overrides: Partial<ChartConfig<unknown>> = {}): ChartConfig<unknown> => ({
  id: 'chart-a',
  i: 'chart-a',
  type: 'DrText',
  title: 'chart-a-title',
  x: 120,
  y: 80,
  w: 300,
  h: 160,
  z: 7,
  rotateX: 0,
  rotateY: 0,
  rotateZ: 0,
  props: {},
  dataset: { code: '', fields: {}, script: '', params: {} },
  behaviors: {},
  ...overrides,
})

test('computes wrapper layout from visual screen chart geometry', () => {
  const style = getVisualScreenChartWrapperStyle(chart())

  assert.equal(style.position, 'absolute')
  assert.equal(style.transform, 'translate(120px,80px)')
  assert.equal(style.width, '300px')
  assert.equal(style.height, '160px')
  assert.equal(style.zIndex, 7)
})

test('keeps rotation in the wrapper transform', () => {
  const style = getVisualScreenChartWrapperStyle(chart({ rotateX: 10, rotateY: 20, rotateZ: 30 }))

  assert.equal(style.transform, 'translate(120px,80px) rotateX(10deg) rotateY(20deg) rotateZ(30deg)')
})
