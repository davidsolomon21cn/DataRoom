import type { VisualScreenAlignmentCommand } from '../alignment'

export interface FloatingSelectionToolbarBounds {
  left: number
  top: number
  right: number
  bottom: number
}

export interface FloatingSelectionToolbarViewport {
  width: number
  height: number
}

export interface FloatingSelectionToolbarRect {
  left: number
  top: number
  right: number
  bottom: number
}

export interface FloatingSelectionToolbarStateOptions {
  selectionBounds: FloatingSelectionToolbarBounds | null
  viewport: FloatingSelectionToolbarViewport
  toolbarWidth: number
  toolbarHeight: number
  selectedChartCount: number
  selectedGroupChart: boolean
  canUngroup?: boolean
  alignableChartCount?: number
  groupableChartCount?: number
  distributableChartCount?: number
  interactionBlocked: boolean
  gap?: number
  viewportPadding?: number
}

export interface FloatingSelectionToolbarWidthOptions {
  selectedChartCount: number
  selectedGroupChart: boolean
  canUngroup: boolean
  alignableChartCount?: number
  groupableChartCount?: number
  distributableChartCount?: number
}

export interface FloatingSelectionToolbarActions {
  showAlignment: boolean
  showDistribution: boolean
  showGroup: boolean
  showUngroup: boolean
}

export interface FloatingSelectionToolbarCommandOptions extends FloatingSelectionToolbarWidthOptions {
  command: VisualScreenAlignmentCommand
}

export type FloatingSelectionToolbarLayoutKey = string

export interface FloatingSelectionToolbarState {
  visible: boolean
  left: number
  top: number
  placement: 'top' | 'bottom'
}

export const getFloatingSelectionToolbarLayoutKey = (options: FloatingSelectionToolbarWidthOptions): FloatingSelectionToolbarLayoutKey => {
  const actions = getFloatingSelectionToolbarActions(options)
  return [
    actions.showAlignment ? 'align' : 'no-align',
    actions.showDistribution ? 'distribute' : 'no-distribute',
    actions.showGroup ? 'group' : 'no-group',
    actions.showUngroup ? 'ungroup' : 'no-ungroup',
  ].join(':')
}

export const getFloatingSelectionToolbarActions = (options: FloatingSelectionToolbarWidthOptions): FloatingSelectionToolbarActions => {
  if (options.selectedGroupChart) {
    return {
      showAlignment: false,
      showDistribution: false,
      showGroup: false,
      showUngroup: options.canUngroup,
    }
  }

  const alignableChartCount = options.alignableChartCount ?? options.selectedChartCount
  const groupableChartCount = options.groupableChartCount ?? alignableChartCount
  const distributableChartCount = options.distributableChartCount ?? alignableChartCount
  const canAlign = alignableChartCount >= 2
  return {
    showAlignment: canAlign,
    showDistribution: distributableChartCount >= 3,
    showGroup: groupableChartCount >= 2,
    showUngroup: options.selectedChartCount >= 2 && options.canUngroup,
  }
}

export const canExecuteFloatingSelectionToolbarCommand = (options: FloatingSelectionToolbarCommandOptions) => {
  const actions = getFloatingSelectionToolbarActions(options)
  if (options.command === 'horizontal-distribute' || options.command === 'vertical-distribute') {
    return actions.showDistribution
  }
  return actions.showAlignment
}

export const getFloatingSelectionToolbarWidth = (options: FloatingSelectionToolbarWidthOptions) => {
  const actions = getFloatingSelectionToolbarActions(options)
  if (actions.showUngroup && !actions.showAlignment && !actions.showGroup) {
    return 48
  }
  if (actions.showDistribution && actions.showUngroup) {
    return 390
  }
  if (actions.showDistribution) {
    return 356
  }
  if (actions.showAlignment && actions.showGroup && actions.showUngroup) {
    return 310
  }
  if (actions.showAlignment && actions.showGroup) {
    return 276
  }
  return 0
}

const clamp = (value: number, min: number, max: number) => {
  if (max < min) {
    return min
  }
  return Math.min(Math.max(value, min), max)
}

export const getFloatingSelectionBoundsFromRects = (
  viewportRect: FloatingSelectionToolbarRect | null,
  selectedRects: FloatingSelectionToolbarRect[],
): FloatingSelectionToolbarBounds | null => {
  if (!viewportRect || selectedRects.length === 0) {
    return null
  }
  return {
    left: Math.min(...selectedRects.map((rect) => rect.left)) - viewportRect.left,
    top: Math.min(...selectedRects.map((rect) => rect.top)) - viewportRect.top,
    right: Math.max(...selectedRects.map((rect) => rect.right)) - viewportRect.left,
    bottom: Math.max(...selectedRects.map((rect) => rect.bottom)) - viewportRect.top,
  }
}

export const getFloatingSelectionToolbarState = (options: FloatingSelectionToolbarStateOptions): FloatingSelectionToolbarState => {
  const gap = options.gap ?? 12
  const viewportPadding = options.viewportPadding ?? 8
  const actions = getFloatingSelectionToolbarActions({
    selectedChartCount: options.selectedChartCount,
    selectedGroupChart: options.selectedGroupChart,
    canUngroup: Boolean(options.canUngroup),
    alignableChartCount: options.alignableChartCount,
    groupableChartCount: options.groupableChartCount,
    distributableChartCount: options.distributableChartCount,
  })
  const hasQuickAction = actions.showAlignment || actions.showDistribution || actions.showGroup || actions.showUngroup

  if (!options.selectionBounds || !hasQuickAction || options.interactionBlocked || options.viewport.width <= 0 || options.viewport.height <= 0) {
    return {
      visible: false,
      left: 0,
      top: 0,
      placement: 'top',
    }
  }

  const selectionCenterX = (options.selectionBounds.left + options.selectionBounds.right) / 2
  const minLeft = viewportPadding
  const maxLeft = options.viewport.width - options.toolbarWidth - viewportPadding
  const left = clamp(selectionCenterX - options.toolbarWidth / 2, minLeft, maxLeft)
  const topCandidate = options.selectionBounds.top - options.toolbarHeight - gap
  const placement = topCandidate >= viewportPadding ? 'top' : 'bottom'
  const nextTop = placement === 'top' ? topCandidate : options.selectionBounds.bottom + gap
  const maxTop = options.viewport.height - options.toolbarHeight - viewportPadding
  const top = clamp(nextTop, viewportPadding, maxTop)

  return {
    visible: true,
    left,
    top,
    placement,
  }
}
