import type { VisualScreenAlignmentCommand } from '../alignment'
import {
  floatingToolbarAlignmentActionDefinitions,
  floatingToolbarDistributionActionDefinitions,
  type FloatingSelectionToolbarIconKey,
} from './action-definitions'

import alignBottomIcon from '../assets/icons/align-bottom.svg'
import alignHorizontalCenterIcon from '../assets/icons/align-horizontal-center.svg'
import alignLeftIcon from '../assets/icons/align-left.svg'
import alignRightIcon from '../assets/icons/align-right.svg'
import alignTopIcon from '../assets/icons/align-top.svg'
import alignVerticalCenterIcon from '../assets/icons/align-vertical-center.svg'
import distributeHorizontalIcon from '../assets/icons/distribute-horizontal.svg'
import distributeVerticalIcon from '../assets/icons/distribute-vertical.svg'
import groupIcon from '../assets/icons/group.svg'
import ungroupIcon from '../assets/icons/ungroup.svg'

export interface FloatingSelectionToolbarAction {
  command: VisualScreenAlignmentCommand
  label: string
  icon: string
}

const toolbarIconByKey: Record<FloatingSelectionToolbarIconKey, string> = {
  'align-left': alignLeftIcon,
  'align-horizontal-center': alignHorizontalCenterIcon,
  'align-right': alignRightIcon,
  'align-top': alignTopIcon,
  'align-vertical-center': alignVerticalCenterIcon,
  'align-bottom': alignBottomIcon,
  'distribute-horizontal': distributeHorizontalIcon,
  'distribute-vertical': distributeVerticalIcon,
}

const withIcon = (definition: { command: VisualScreenAlignmentCommand; label: string; iconKey: FloatingSelectionToolbarIconKey }) => {
  return {
    command: definition.command,
    label: definition.label,
    icon: toolbarIconByKey[definition.iconKey],
  }
}

export const floatingToolbarAlignmentActions: FloatingSelectionToolbarAction[] = floatingToolbarAlignmentActionDefinitions.map(withIcon)

export const floatingToolbarDistributionActions: FloatingSelectionToolbarAction[] = floatingToolbarDistributionActionDefinitions.map(withIcon)

export const floatingToolbarGroupAction = {
  label: '组合',
  icon: groupIcon,
}

export const floatingToolbarUngroupAction = {
  label: '取消组合',
  icon: ungroupIcon,
}
