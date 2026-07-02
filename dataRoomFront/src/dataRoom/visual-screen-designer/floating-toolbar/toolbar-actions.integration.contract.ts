/// <reference types="node" />

import assert from 'node:assert/strict'
import test from 'node:test'

import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'
import { applyVisualScreenSelectionLayout } from '../selection-layout/index.ts'
import { canUngroupChart, createGroupChart, groupChartsInParent, ungroupChartsInParent } from '../grouping/index.ts'
import {
  canExecuteFloatingSelectionToolbarCommand,
  getFloatingSelectionToolbarActions,
} from './index.ts'
import {
  floatingToolbarAlignmentActionDefinitions,
  floatingToolbarDistributionActionDefinitions,
} from './action-definitions.ts'

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

test('every visible alignment toolbar action changes a valid two-chart selection when positions differ', () => {
  floatingToolbarAlignmentActionDefinitions.forEach((action) => {
    const charts = [
      chart('a', { x: 20, y: 30, w: 80, h: 40 }),
      chart('b', { x: 220, y: 170, w: 120, h: 90 }),
    ]
    const toolbarActions = getFloatingSelectionToolbarActions({
      selectedChartCount: charts.length,
      selectedGroupChart: false,
      canUngroup: false,
      alignableChartCount: charts.length,
      groupableChartCount: charts.length,
      distributableChartCount: charts.length,
    })

    assert.equal(toolbarActions.showAlignment, true)
    assert.equal(
      canExecuteFloatingSelectionToolbarCommand({
        selectedChartCount: charts.length,
        selectedGroupChart: false,
        canUngroup: false,
        alignableChartCount: charts.length,
        groupableChartCount: charts.length,
        distributableChartCount: charts.length,
        command: action.command,
      }),
      true,
    )

    const result = applyVisualScreenSelectionLayout({
      selectedCharts: charts,
      command: action.command,
    })

    assert.equal(result.changed, true, `${action.command} should change the selected chart layout`)
    assert.ok(result.changedIds.length > 0, `${action.command} should report changed chart ids`)
  })
})

test('every visible distribution toolbar action changes a valid three-chart selection', () => {
  floatingToolbarDistributionActionDefinitions.forEach((action) => {
    const charts = [
      chart('a', { x: 0, y: 0, w: 80, h: 40 }),
      chart('b', { x: 110, y: 100, w: 60, h: 70 }),
      chart('c', { x: 320, y: 260, w: 100, h: 80 }),
    ]
    const toolbarActions = getFloatingSelectionToolbarActions({
      selectedChartCount: charts.length,
      selectedGroupChart: false,
      canUngroup: false,
      alignableChartCount: charts.length,
      groupableChartCount: charts.length,
      distributableChartCount: charts.length,
    })

    assert.equal(toolbarActions.showDistribution, true)
    assert.equal(
      canExecuteFloatingSelectionToolbarCommand({
        selectedChartCount: charts.length,
        selectedGroupChart: false,
        canUngroup: false,
        alignableChartCount: charts.length,
        groupableChartCount: charts.length,
        distributableChartCount: charts.length,
        command: action.command,
      }),
      true,
    )

    const result = applyVisualScreenSelectionLayout({
      selectedCharts: charts,
      command: action.command,
    })

    assert.equal(result.changed, true, `${action.command} should change the selected chart layout`)
    assert.ok(result.changedIds.length > 0, `${action.command} should report changed chart ids`)
  })
})

test('visible group and ungroup toolbar actions mutate the same sibling list used by the designer', () => {
  const siblings = [
    chart('a', { x: 20, y: 30, w: 80, h: 40 }),
    chart('b', { x: 220, y: 170, w: 120, h: 90 }),
  ]
  const groupActions = getFloatingSelectionToolbarActions({
    selectedChartCount: 2,
    selectedGroupChart: false,
    canUngroup: false,
    alignableChartCount: 2,
    groupableChartCount: 2,
    distributableChartCount: 2,
  })

  assert.equal(groupActions.showGroup, true)
  const groupResult = groupChartsInParent(siblings, ['a', 'b'], '组合')

  assert.equal(groupResult.changed, true)
  assert.deepEqual(siblings.map((item) => item.id), [groupResult.group!.id])

  const group = siblings[0]!
  const ungroupActions = getFloatingSelectionToolbarActions({
    selectedChartCount: 1,
    selectedGroupChart: true,
    canUngroup: canUngroupChart(group),
    alignableChartCount: 0,
    groupableChartCount: 0,
    distributableChartCount: 0,
  })

  assert.equal(ungroupActions.showUngroup, true)
  const ungroupResult = ungroupChartsInParent(siblings, [group.id])

  assert.equal(ungroupResult.changed, true)
  assert.deepEqual(siblings.map((item) => item.id), ['a', 'b'])
  assert.deepEqual(ungroupResult.selectedIds, ['a', 'b'])
})

test('mixed group and plain selections expose and execute every relevant toolbar action', () => {
  const existingGroup = createGroupChart({
    title: '已有组合',
    x: 40,
    y: 50,
    w: 160,
    h: 100,
    children: [chart('inside', { x: 0, y: 0, w: 80, h: 60 })],
  })
  const plain = chart('plain', { x: 300, y: 180, w: 100, h: 70 })
  const siblings = [existingGroup, plain]
  const toolbarActions = getFloatingSelectionToolbarActions({
    selectedChartCount: 2,
    selectedGroupChart: false,
    canUngroup: true,
    alignableChartCount: 2,
    groupableChartCount: 2,
    distributableChartCount: 2,
  })

  assert.deepEqual(toolbarActions, {
    showAlignment: true,
    showDistribution: false,
    showGroup: true,
    showUngroup: true,
  })

  const alignmentResult = applyVisualScreenSelectionLayout({
    selectedCharts: siblings,
    command: 'left',
  })
  assert.equal(alignmentResult.changed, true)
  assert.deepEqual(siblings.map((item) => item.x), [40, 40])

  plain.x = 300
  const ungroupResult = ungroupChartsInParent(siblings, [existingGroup.id, plain.id])
  assert.equal(ungroupResult.changed, true)
  assert.deepEqual(siblings.map((item) => item.id), ['inside', 'plain'])
  assert.deepEqual(ungroupResult.selectedIds, ['inside', 'plain'])

  const regroupResult = groupChartsInParent(siblings, ungroupResult.selectedIds, '组合')
  assert.equal(regroupResult.changed, true)
  assert.deepEqual(siblings.map((item) => item.id), [regroupResult.group!.id])
})
