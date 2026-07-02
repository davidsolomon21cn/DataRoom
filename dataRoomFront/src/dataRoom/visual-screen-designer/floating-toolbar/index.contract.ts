import { strict as assert } from 'node:assert'
import { existsSync } from 'node:fs'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

import { applyVisualScreenAlignment } from '../alignment/index.ts'
import {
  floatingToolbarAlignmentActionDefinitions,
  floatingToolbarDistributionActionDefinitions,
  type FloatingSelectionToolbarIconKey,
} from './action-definitions.ts'
import {
  canExecuteFloatingSelectionToolbarCommand,
  getFloatingSelectionBoundsFromRects,
  getFloatingSelectionToolbarActions,
  getFloatingSelectionToolbarLayoutKey,
  getFloatingSelectionToolbarState,
  getFloatingSelectionToolbarWidth,
  type FloatingSelectionToolbarBounds,
  type FloatingSelectionToolbarViewport,
} from './index.ts'

const currentDir = dirname(fileURLToPath(import.meta.url))
const iconDir = resolve(currentDir, '../assets/icons')
const sourceIconDir = '/Users/liuchengbiao/Downloads/icon_svbhkgz4tdq'
const localIconFileByKey: Record<FloatingSelectionToolbarIconKey | 'group' | 'ungroup', string> = {
  'align-left': 'align-left.svg',
  'align-horizontal-center': 'align-horizontal-center.svg',
  'align-right': 'align-right.svg',
  'align-top': 'align-top.svg',
  'align-vertical-center': 'align-vertical-center.svg',
  'align-bottom': 'align-bottom.svg',
  'distribute-horizontal': 'distribute-horizontal.svg',
  'distribute-vertical': 'distribute-vertical.svg',
  group: 'group.svg',
  ungroup: 'ungroup.svg',
}
const sourceIconFileByKey: Record<FloatingSelectionToolbarIconKey, string> = {
  'align-left': 'jurassic_horizalign-left.svg',
  'align-horizontal-center': 'jurassic_horizalign-center.svg',
  'align-right': 'jurassic_horizalign-right.svg',
  'align-top': 'jurassic_verticalalign-top.svg',
  'align-vertical-center': 'jurassic_verticalalign-center.svg',
  'align-bottom': 'jurassic_verticalalign-bottom.svg',
  'distribute-horizontal': 'jurassic_HorFensan-align.svg',
  'distribute-vertical': 'jurassic_VerFensan-align.svg',
}

const viewport: FloatingSelectionToolbarViewport = {
  width: 800,
  height: 600,
}

const bounds: FloatingSelectionToolbarBounds = {
  left: 260,
  top: 240,
  right: 460,
  bottom: 340,
}

test('hides floating toolbar when the current selection has no quick action', () => {
  assert.equal(
    getFloatingSelectionToolbarState({
      selectionBounds: bounds,
      viewport,
      toolbarWidth: 320,
      toolbarHeight: 40,
      selectedChartCount: 1,
      selectedGroupChart: false,
      interactionBlocked: false,
    }).visible,
    false,
  )
})

test('places floating toolbar above a multi-selection when there is room', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: bounds,
    viewport,
    toolbarWidth: 320,
    toolbarHeight: 40,
    selectedChartCount: 2,
    selectedGroupChart: false,
    interactionBlocked: false,
  })

  assert.equal(state.visible, true)
  assert.equal(state.placement, 'top')
  assert.equal(state.left, 200)
  assert.equal(state.top, 188)
})

test('places floating toolbar below the selection when the top side has no room', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: {
      left: 260,
      top: 24,
      right: 460,
      bottom: 124,
    },
    viewport,
    toolbarWidth: 320,
    toolbarHeight: 40,
    selectedChartCount: 2,
    selectedGroupChart: false,
    interactionBlocked: false,
  })

  assert.equal(state.visible, true)
  assert.equal(state.placement, 'bottom')
  assert.equal(state.top, 136)
})

test('keeps floating toolbar inside the viewport horizontally', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: {
      left: 8,
      top: 240,
      right: 80,
      bottom: 340,
    },
    viewport,
    toolbarWidth: 320,
    toolbarHeight: 40,
    selectedChartCount: 2,
    selectedGroupChart: false,
    interactionBlocked: false,
  })

  assert.equal(state.visible, true)
  assert.equal(state.left, 8)
})

test('shows floating toolbar for a selected group only when ungroup is executable', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: bounds,
    viewport,
    toolbarWidth: 320,
    toolbarHeight: 40,
    selectedChartCount: 1,
    selectedGroupChart: true,
    canUngroup: true,
    interactionBlocked: false,
  })

  assert.equal(state.visible, true)
})

test('hides floating toolbar for a selected group when ungroup is not executable', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: bounds,
    viewport,
    toolbarWidth: 320,
    toolbarHeight: 40,
    selectedChartCount: 1,
    selectedGroupChart: true,
    canUngroup: false,
    interactionBlocked: false,
  })

  assert.equal(state.visible, false)
})

test('hides floating toolbar for a single non-group selection even when ungroup is present defensively', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: bounds,
    viewport,
    toolbarWidth: 320,
    toolbarHeight: 40,
    selectedChartCount: 1,
    selectedGroupChart: false,
    canUngroup: true,
    interactionBlocked: false,
  })

  assert.equal(state.visible, false)
})

test('computes selection bounds from real DOM rects relative to the viewport', () => {
  assert.deepEqual(
    getFloatingSelectionBoundsFromRects(
      { left: 100, top: 50, right: 900, bottom: 650 },
      [
        { left: 220, top: 160, right: 320, bottom: 240 },
        { left: 420, top: 260, right: 560, bottom: 360 },
      ],
    ),
    {
      left: 120,
      top: 110,
      right: 460,
      bottom: 310,
    },
  )
})

test('computes selection bounds from transformed viewport and selected rects', () => {
  assert.deepEqual(
    getFloatingSelectionBoundsFromRects(
      { left: 240, top: 180, right: 1040, bottom: 780 },
      [
        { left: 360, top: 300, right: 460, bottom: 380 },
        { left: 640, top: 420, right: 820, bottom: 540 },
      ],
    ),
    {
      left: 120,
      top: 120,
      right: 580,
      bottom: 360,
    },
  )
})

test('does not compute selection bounds without a viewport or selected rects', () => {
  assert.equal(getFloatingSelectionBoundsFromRects(null, [{ left: 0, top: 0, right: 1, bottom: 1 }]), null)
  assert.equal(getFloatingSelectionBoundsFromRects({ left: 0, top: 0, right: 1, bottom: 1 }, []), null)
})

test('hides floating toolbar while canvas interaction is blocked', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: bounds,
    viewport,
    toolbarWidth: 320,
    toolbarHeight: 40,
    selectedChartCount: 2,
    selectedGroupChart: false,
    interactionBlocked: true,
  })

  assert.equal(state.visible, false)
})

test('uses explicit executable chart counts when deciding whether toolbar state is visible', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: bounds,
    viewport,
    toolbarWidth: 48,
    toolbarHeight: 40,
    selectedChartCount: 4,
    selectedGroupChart: false,
    canUngroup: true,
    alignableChartCount: 1,
    groupableChartCount: 1,
    distributableChartCount: 1,
    interactionBlocked: false,
  })

  assert.equal(state.visible, true)
  assert.equal(state.left, 336)
})

test('hides toolbar state when explicit executable chart counts remove every quick action', () => {
  const state = getFloatingSelectionToolbarState({
    selectionBounds: bounds,
    viewport,
    toolbarWidth: 48,
    toolbarHeight: 40,
    selectedChartCount: 4,
    selectedGroupChart: false,
    canUngroup: false,
    alignableChartCount: 1,
    groupableChartCount: 1,
    distributableChartCount: 1,
    interactionBlocked: false,
  })

  assert.equal(state.visible, false)
})

test('does not expose alignment or grouping when only one selected chart is actually rendered', () => {
  const actions = getFloatingSelectionToolbarActions({
    selectedChartCount: 1,
    selectedGroupChart: false,
    canUngroup: false,
    alignableChartCount: 1,
    groupableChartCount: 1,
    distributableChartCount: 1,
  })

  assert.deepEqual(actions, {
    showAlignment: false,
    showDistribution: false,
    showGroup: false,
    showUngroup: false,
  })
  assert.equal(
    getFloatingSelectionToolbarState({
      selectionBounds: bounds,
      viewport,
      toolbarWidth: 48,
      toolbarHeight: 40,
      selectedChartCount: 1,
      selectedGroupChart: false,
      canUngroup: false,
      alignableChartCount: 1,
      groupableChartCount: 1,
      distributableChartCount: 1,
      interactionBlocked: false,
    }).visible,
    false,
  )
})

test('exposes every alignment and distribution command used by the floating toolbar', () => {
  assert.deepEqual(
    floatingToolbarAlignmentActionDefinitions.map((action) => action.command),
    ['left', 'horizontal-center', 'right', 'top', 'vertical-center', 'bottom'],
  )
  assert.deepEqual(
    floatingToolbarDistributionActionDefinitions.map((action) => action.command),
    ['horizontal-distribute', 'vertical-distribute'],
  )
  ;[...floatingToolbarAlignmentActionDefinitions, ...floatingToolbarDistributionActionDefinitions].forEach((action) => {
    assert.ok(action.label)
    assert.ok(action.iconKey)
  })
})

test('maps every floating toolbar button to a local svg icon', () => {
  const toolbarIconKeys: Array<FloatingSelectionToolbarIconKey | 'group' | 'ungroup'> = [
    ...floatingToolbarAlignmentActionDefinitions.map((action) => action.iconKey),
    ...floatingToolbarDistributionActionDefinitions.map((action) => action.iconKey),
    'group',
    'ungroup',
  ]
  toolbarIconKeys.forEach((iconKey) => {
    assert.equal(existsSync(resolve(iconDir, localIconFileByKey[iconKey])), true)
  })
})

test('renames downloaded toolbar svg icons to stable semantic asset names', () => {
  Object.entries(sourceIconFileByKey).forEach(([iconKey, sourceFileName]) => {
    const localFileName = localIconFileByKey[iconKey as FloatingSelectionToolbarIconKey]
    assert.equal(readFileSync(resolve(iconDir, localFileName), 'utf8'), readFileSync(resolve(sourceIconDir, sourceFileName), 'utf8'))
  })
})

test('floating toolbar commands are executable by the visual screen alignment engine', () => {
  ;[...floatingToolbarAlignmentActionDefinitions, ...floatingToolbarDistributionActionDefinitions].forEach((action) => {
    const charts = [
      { id: 'a', x: 0, y: 0, w: 20, h: 20 },
      { id: 'b', x: 80, y: 40, w: 20, h: 20 },
      { id: 'c', x: 180, y: 120, w: 20, h: 20 },
    ]
    const result = applyVisualScreenAlignment(charts, action.command)
    assert.equal(typeof result.changed, 'boolean')
    assert.ok(Array.isArray(result.changedIds))
  })
})

test('computes toolbar width from the visible quick actions', () => {
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 1,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    0,
  )
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 2,
      selectedGroupChart: false,
      canUngroup: false,
    }),
    276,
  )
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 2,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    310,
  )
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: false,
    }),
    356,
  )
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    390,
  )
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 1,
      selectedGroupChart: true,
      canUngroup: true,
    }),
    48,
  )
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 1,
      selectedGroupChart: true,
      canUngroup: false,
    }),
    0,
  )
})

test('computes visible toolbar actions from executable selection capabilities', () => {
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 1,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    {
      showAlignment: false,
      showDistribution: false,
      showGroup: false,
      showUngroup: false,
    },
  )
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 2,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    {
      showAlignment: true,
      showDistribution: false,
      showGroup: true,
      showUngroup: true,
    },
  )
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: false,
    }),
    {
      showAlignment: true,
      showDistribution: true,
      showGroup: true,
      showUngroup: false,
    },
  )
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 1,
      selectedGroupChart: true,
      canUngroup: true,
    }),
    {
      showAlignment: false,
      showDistribution: false,
      showGroup: false,
      showUngroup: true,
    },
  )
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 2,
      selectedGroupChart: false,
      canUngroup: false,
    }),
    {
      showAlignment: true,
      showDistribution: false,
      showGroup: true,
      showUngroup: false,
    },
  )
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 1,
      selectedGroupChart: true,
      canUngroup: false,
    }),
    {
      showAlignment: false,
      showDistribution: false,
      showGroup: false,
      showUngroup: false,
    },
  )
})

test('keeps batch ungroup executable for multi-selection that contains groups', () => {
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 4,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    {
      showAlignment: true,
      showDistribution: true,
      showGroup: true,
      showUngroup: true,
    },
  )
  assert.equal(
    getFloatingSelectionToolbarWidth({
      selectedChartCount: 4,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    390,
  )
})

test('derives visible actions from explicit executable chart counts when supplied', () => {
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 4,
      selectedGroupChart: false,
      canUngroup: true,
      alignableChartCount: 1,
      groupableChartCount: 1,
      distributableChartCount: 1,
    }),
    {
      showAlignment: false,
      showDistribution: false,
      showGroup: false,
      showUngroup: true,
    },
  )
})

test('uses rendered executable counts instead of raw selected counts for quick actions', () => {
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: false,
      alignableChartCount: 2,
      groupableChartCount: 2,
      distributableChartCount: 2,
    }),
    {
      showAlignment: true,
      showDistribution: false,
      showGroup: true,
      showUngroup: false,
    },
  )
  assert.deepEqual(
    getFloatingSelectionToolbarActions({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: false,
      alignableChartCount: 1,
      groupableChartCount: 1,
      distributableChartCount: 1,
    }),
    {
      showAlignment: false,
      showDistribution: false,
      showGroup: false,
      showUngroup: false,
    },
  )
})

test('validates commands from explicit executable chart counts when supplied', () => {
  assert.equal(
    canExecuteFloatingSelectionToolbarCommand({
      selectedChartCount: 4,
      selectedGroupChart: false,
      canUngroup: true,
      alignableChartCount: 1,
      groupableChartCount: 1,
      distributableChartCount: 1,
      command: 'left',
    }),
    false,
  )
  assert.equal(
    canExecuteFloatingSelectionToolbarCommand({
      selectedChartCount: 4,
      selectedGroupChart: false,
      canUngroup: true,
      alignableChartCount: 2,
      groupableChartCount: 2,
      distributableChartCount: 2,
      command: 'horizontal-distribute',
    }),
    false,
  )
  assert.equal(
    canExecuteFloatingSelectionToolbarCommand({
      selectedChartCount: 4,
      selectedGroupChart: false,
      canUngroup: true,
      alignableChartCount: 3,
      groupableChartCount: 3,
      distributableChartCount: 3,
      command: 'horizontal-distribute',
    }),
    true,
  )
})

test('validates alignment and distribution command executability from the same toolbar rules', () => {
  assert.equal(
    canExecuteFloatingSelectionToolbarCommand({
      selectedChartCount: 2,
      selectedGroupChart: false,
      canUngroup: false,
      command: 'left',
    }),
    true,
  )
  assert.equal(
    canExecuteFloatingSelectionToolbarCommand({
      selectedChartCount: 2,
      selectedGroupChart: false,
      canUngroup: false,
      command: 'horizontal-distribute',
    }),
    false,
  )
  assert.equal(
    canExecuteFloatingSelectionToolbarCommand({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: false,
      command: 'horizontal-distribute',
    }),
    true,
  )
  assert.equal(
    canExecuteFloatingSelectionToolbarCommand({
      selectedChartCount: 1,
      selectedGroupChart: true,
      canUngroup: true,
      command: 'left',
    }),
    false,
  )
})

test('computes a different layout key for each visible toolbar shape', () => {
  assert.notEqual(
    getFloatingSelectionToolbarLayoutKey({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: true,
    }),
    getFloatingSelectionToolbarLayoutKey({
      selectedChartCount: 1,
      selectedGroupChart: true,
      canUngroup: true,
    }),
  )
  assert.notEqual(
    getFloatingSelectionToolbarLayoutKey({
      selectedChartCount: 2,
      selectedGroupChart: false,
      canUngroup: false,
    }),
    getFloatingSelectionToolbarLayoutKey({
      selectedChartCount: 3,
      selectedGroupChart: false,
      canUngroup: false,
    }),
  )
})
