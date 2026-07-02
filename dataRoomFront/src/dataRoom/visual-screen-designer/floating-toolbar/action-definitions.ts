import type { VisualScreenAlignmentCommand } from '../alignment'

export type FloatingSelectionToolbarIconKey =
  | 'align-left'
  | 'align-horizontal-center'
  | 'align-right'
  | 'align-top'
  | 'align-vertical-center'
  | 'align-bottom'
  | 'distribute-horizontal'
  | 'distribute-vertical'

export interface FloatingSelectionToolbarActionDefinition {
  command: VisualScreenAlignmentCommand
  label: string
  iconKey: FloatingSelectionToolbarIconKey
}

export const floatingToolbarAlignmentActionDefinitions: FloatingSelectionToolbarActionDefinition[] = [
  { command: 'left', label: '左对齐', iconKey: 'align-left' },
  { command: 'horizontal-center', label: '水平居中', iconKey: 'align-horizontal-center' },
  { command: 'right', label: '右对齐', iconKey: 'align-right' },
  { command: 'top', label: '顶端对齐', iconKey: 'align-top' },
  { command: 'vertical-center', label: '垂直居中', iconKey: 'align-vertical-center' },
  { command: 'bottom', label: '底端对齐', iconKey: 'align-bottom' },
]

export const floatingToolbarDistributionActionDefinitions: FloatingSelectionToolbarActionDefinition[] = [
  { command: 'horizontal-distribute', label: '横向分布', iconKey: 'distribute-horizontal' },
  { command: 'vertical-distribute', label: '纵向分布', iconKey: 'distribute-vertical' },
]

