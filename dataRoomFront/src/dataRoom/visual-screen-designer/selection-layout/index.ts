import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'
import type { VisualScreenAlignmentCommand } from '../alignment'
import { applyVisualScreenAlignment } from '../alignment'
import { normalizeGroupBounds } from '../grouping'

export interface VisualScreenSelectionLayoutOptions {
  selectedCharts: ChartConfig<unknown>[]
  command: VisualScreenAlignmentCommand
  currentGroup?: ChartConfig<unknown> | null
}

export interface VisualScreenSelectionLayoutResult {
  changed: boolean
  changedIds: string[]
  groupBoundsChanged: boolean
}

export const normalizeVisualScreenSelectionContainerLayout = (currentGroup?: ChartConfig<unknown> | null) => {
  return currentGroup ? normalizeGroupBounds(currentGroup) : { changed: false }
}

export const applyVisualScreenSelectionLayout = ({
  selectedCharts,
  command,
  currentGroup,
}: VisualScreenSelectionLayoutOptions): VisualScreenSelectionLayoutResult => {
  const alignmentResult = applyVisualScreenAlignment(selectedCharts, command)
  const groupBoundsResult = normalizeVisualScreenSelectionContainerLayout(currentGroup)

  return {
    changed: alignmentResult.changed || groupBoundsResult.changed,
    changedIds: alignmentResult.changedIds,
    groupBoundsChanged: groupBoundsResult.changed,
  }
}
