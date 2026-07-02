/// <reference types="node" />

import test from 'node:test'
import assert from 'node:assert/strict'

import {
  EditorHistoryManager,
  captureChartLayoutState,
  cloneChartConfig,
  createReorderChartHistoryEntry,
  createReplaceChartChildrenHistoryEntry,
  reorderChartWithinParent,
  type ChartHistorySource,
} from './editor-history.ts'
import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'

const createChart = (id: string, overrides: Partial<ChartConfig<unknown>> = {}): ChartConfig<unknown> => ({
  id,
  i: id,
  type: 'DrText',
  title: `${id}-title`,
  w: 100,
  h: 60,
  x: 0,
  y: 0,
  z: 0,
  rotateX: 0,
  rotateY: 0,
  rotateZ: 0,
  props: {},
  dataset: {
    code: '',
    fields: {},
    script: '',
    params: {},
  },
  behaviors: {},
  ...overrides,
})

const createHistory = (chartList: ChartConfig<unknown>[], source: ChartHistorySource = 'visual-screen-designer') =>
  new EditorHistoryManager({
    source,
    getChartList: () => chartList,
  })

test('undo and redo replacing root sibling list', () => {
  const before = [createChart('a'), createChart('b')]
  const after = [
    createChart('group', {
      type: 'DrGroup',
      x: 10,
      y: 20,
      children: [createChart('a'), createChart('b')],
    }),
  ]
  const chartList = before.map((item) => cloneChartConfig(item))
  const history = createHistory(chartList, 'visual-screen-designer')

  chartList.splice(0, chartList.length, ...after.map((item) => cloneChartConfig(item)))
  history.record(createReplaceChartChildrenHistoryEntry('组合', 'visual-screen-designer', { parentType: 'root-chart-list' }, before, after))

  assert.deepEqual(chartList.map((item) => item.id), ['group'])
  history.undo()
  assert.deepEqual(chartList.map((item) => item.id), ['a', 'b'])
  history.redo()
  assert.deepEqual(chartList.map((item) => item.id), ['group'])
  assert.deepEqual(chartList[0]?.children?.map((item) => item.id), ['a', 'b'])
})

test('undo and redo replacing nested sibling list', () => {
  const parent = createChart('parent', {
    type: 'DrGroup',
    children: [createChart('child-a'), createChart('child-b')],
  })
  const chartList = [createChart('root'), parent]
  const before = parent.children!.map((item) => cloneChartConfig(item))
  const after = [
    createChart('nested-group', {
      type: 'DrGroup',
      children: [createChart('child-a'), createChart('child-b')],
    }),
  ]
  const history = createHistory(chartList, 'visual-screen-designer')

  parent.children!.splice(0, parent.children!.length, ...after.map((item) => cloneChartConfig(item)))
  history.record(
    createReplaceChartChildrenHistoryEntry('组合', 'visual-screen-designer', { parentType: 'chart-children', parentId: 'parent' }, before, after),
  )

  assert.deepEqual(parent.children?.map((item) => item.id), ['nested-group'])
  history.undo()
  assert.deepEqual(parent.children?.map((item) => item.id), ['child-a', 'child-b'])
  history.redo()
  assert.deepEqual(parent.children?.map((item) => item.id), ['nested-group'])
  assert.deepEqual(parent.children?.[0]?.children?.map((item) => item.id), ['child-a', 'child-b'])
})

test('undoes nested children replacement and parent layout as one history entry', () => {
  const parent = createChart('parent', {
    type: 'DrGroup',
    x: 100,
    y: 80,
    w: 200,
    h: 120,
    children: [
      createChart('nested-group', {
        type: 'DrGroup',
        x: -20,
        y: -10,
        w: 80,
        h: 40,
        children: [createChart('child-a', { x: 0, y: 0, w: 80, h: 40 })],
      }),
      createChart('child-b', { x: 160, y: 60, w: 40, h: 40 }),
    ],
  })
  const chartList = [parent]
  const beforeChildren = parent.children!.map((item) => cloneChartConfig(item))
  const beforeParentLayout = captureChartLayoutState(parent)
  const afterChildren = [
    createChart('child-a', { x: 0, y: 0, w: 80, h: 40 }),
    createChart('child-b', { x: 180, y: 70, w: 40, h: 40 }),
  ]
  const afterParentLayout = {
    x: 80,
    y: 70,
    w: 220,
    h: 110,
    rotateX: 0,
    rotateY: 0,
    rotateZ: 0,
  }
  const history = createHistory(chartList, 'visual-screen-designer')

  parent.children!.splice(0, parent.children!.length, ...afterChildren.map((item) => cloneChartConfig(item)))
  Object.assign(parent, afterParentLayout)
  history.record(
    createReplaceChartChildrenHistoryEntry(
      '取消组合',
      'visual-screen-designer',
      { parentType: 'chart-children', parentId: 'parent' },
      beforeChildren,
      afterChildren,
      new Map([[parent.id, beforeParentLayout]]),
      new Map([[parent.id, afterParentLayout]]),
    ),
  )

  history.undo()

  assert.deepEqual(parent.children?.map((item) => item.id), ['nested-group', 'child-b'])
  assert.deepEqual([parent.x, parent.y, parent.w, parent.h], [100, 80, 200, 120])
})

test('reorders sibling list and normalizes z order for visual stacking', () => {
  const chartList = [createChart('a', { z: 10 }), createChart('b', { z: 20 }), createChart('c', { z: 30 })]
  const history = createHistory(chartList, 'visual-screen-designer')

  const reordered = reorderChartWithinParent(chartList, {
    parent: { parentType: 'root-chart-list' },
    chartId: 'c',
    toIndex: 0,
  })

  assert.equal(reordered, true)
  assert.deepEqual(chartList.map((item) => item.id), ['c', 'a', 'b'])
  assert.deepEqual(chartList.map((item) => item.z), [2, 1, 0])

  history.record(createReorderChartHistoryEntry('图层置顶', 'visual-screen-designer', { parentType: 'root-chart-list' }, 'c', 2, 0))
  history.undo()

  assert.deepEqual(chartList.map((item) => item.id), ['a', 'b', 'c'])
  assert.deepEqual(chartList.map((item) => item.z), [2, 1, 0])
})
