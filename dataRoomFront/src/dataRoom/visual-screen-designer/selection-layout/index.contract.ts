/// <reference types="node" />

import assert from 'node:assert/strict'
import test from 'node:test'

import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'
import { createGroupChart } from '../grouping/index.ts'
import { applyVisualScreenSelectionLayout, normalizeVisualScreenSelectionContainerLayout } from './index.ts'

const chart = (id: string, overrides: Partial<ChartConfig<unknown>> = {}): ChartConfig<unknown> => ({
  id,
  i: id,
  type: 'DrText',
  title: `${id}-title`,
  x: 0,
  y: 0,
  w: 100,
  h: 60,
  z: 0,
  rotateX: 0,
  rotateY: 0,
  rotateZ: 0,
  props: {},
  dataset: { code: '', fields: {}, script: '', params: {} },
  behaviors: {},
  ...overrides,
})

test('normalizes the active group bounds after aligning children inside a group', () => {
  const group = createGroupChart({
    title: '组合',
    x: 100,
    y: 80,
    w: 200,
    h: 120,
    children: [
      chart('a', { x: 20, y: 20, w: 80, h: 40 }),
      chart('b', { x: 150, y: 60, w: 50, h: 60 }),
    ],
  })

  const result = applyVisualScreenSelectionLayout({
    selectedCharts: group.children,
    command: 'left',
    currentGroup: group,
  })

  assert.equal(result.changed, true)
  assert.equal(result.groupBoundsChanged, true)
  assert.deepEqual([group.x, group.y, group.w, group.h], [120, 100, 80, 100])
  assert.deepEqual(group.children.map((item) => [item.id, item.x, item.y]), [
    ['a', 0, 0],
    ['b', 0, 40],
  ])
})

test('normalizes the active group bounds after distributing children inside a group', () => {
  const group = createGroupChart({
    title: '组合',
    x: 100,
    y: 80,
    w: 260,
    h: 120,
    children: [
      chart('a', { x: 20, y: 20, w: 40, h: 40 }),
      chart('b', { x: 90, y: 60, w: 40, h: 40 }),
      chart('c', { x: 220, y: 30, w: 40, h: 40 }),
    ],
  })

  const result = applyVisualScreenSelectionLayout({
    selectedCharts: group.children,
    command: 'horizontal-distribute',
    currentGroup: group,
  })

  assert.equal(result.changed, true)
  assert.equal(result.groupBoundsChanged, true)
  assert.deepEqual([group.x, group.y, group.w, group.h], [120, 100, 240, 80])
  assert.deepEqual(group.children.map((item) => [item.id, item.x, item.y]), [
    ['a', 0, 0],
    ['b', 100, 40],
    ['c', 200, 10],
  ])
})

test('does not require a group when aligning root-level charts', () => {
  const charts = [chart('a', { x: 20, y: 0 }), chart('b', { x: 80, y: 30 })]

  const result = applyVisualScreenSelectionLayout({
    selectedCharts: charts,
    command: 'left',
  })

  assert.equal(result.changed, true)
  assert.equal(result.groupBoundsChanged, false)
  assert.deepEqual(charts.map((item) => item.x), [20, 20])
})

test('normalizes the active group bounds after selected children are moved by another interaction', () => {
  const group = createGroupChart({
    title: '组合',
    x: 100,
    y: 80,
    w: 200,
    h: 120,
    children: [
      chart('a', { x: -30, y: 20, w: 80, h: 40 }),
      chart('b', { x: 150, y: -10, w: 100, h: 90 }),
    ],
  })

  const result = normalizeVisualScreenSelectionContainerLayout(group)

  assert.equal(result.changed, true)
  assert.deepEqual([group.x, group.y, group.w, group.h], [70, 70, 280, 90])
  assert.deepEqual(group.children.map((item) => [item.id, item.x, item.y]), [
    ['a', 0, 30],
    ['b', 180, 0],
  ])
})
