<script setup lang="ts">
import { computed } from 'vue'

import type { VisualScreenAlignmentCommand } from '../alignment'
import {
  floatingToolbarAlignmentActions,
  floatingToolbarDistributionActions,
  floatingToolbarGroupAction,
  floatingToolbarUngroupAction,
} from '../floating-toolbar/actions'
import { getFloatingSelectionToolbarActions } from '../floating-toolbar'

const props = defineProps<{
  selectedChartCount: number
  selectedGroupChart: boolean
  canUngroup: boolean
  alignableChartCount?: number
  groupableChartCount?: number
  distributableChartCount?: number
}>()

const emit = defineEmits<{
  align: [command: VisualScreenAlignmentCommand]
  group: []
  ungroup: []
}>()

let skipNextClick = false

const stopToolbarPointerEvent = (event: Event) => {
  event.stopPropagation()
}

const stopToolbarDragEvent = (event: Event) => {
  event.preventDefault()
  event.stopPropagation()
}

const runToolbarReleaseAction = (event: Event, action: () => void) => {
  event.preventDefault()
  event.stopPropagation()
  skipNextClick = true
  window.setTimeout(() => {
    skipNextClick = false
  }, 0)
  action()
}

const runToolbarClickAction = (event: Event, action: () => void) => {
  event.preventDefault()
  event.stopPropagation()
  if (skipNextClick) {
    skipNextClick = false
    return
  }
  action()
}

const toolbarActions = computed(() =>
  getFloatingSelectionToolbarActions({
    selectedChartCount: props.selectedChartCount,
    selectedGroupChart: props.selectedGroupChart,
    canUngroup: props.canUngroup,
    alignableChartCount: props.alignableChartCount,
    groupableChartCount: props.groupableChartCount,
    distributableChartCount: props.distributableChartCount,
  }),
)
</script>

<template>
  <div class="floating-selection-toolbar" role="toolbar" aria-label="选中组件快捷操作">
    <template v-if="!selectedGroupChart">
      <div v-if="toolbarActions.showAlignment" class="floating-selection-toolbar__group" role="group" aria-label="对齐">
        <el-tooltip v-for="action in floatingToolbarAlignmentActions" :key="action.command" :content="action.label" placement="top">
          <button
            class="floating-selection-toolbar__button"
            type="button"
            :aria-label="action.label"
            @pointerdown="stopToolbarPointerEvent"
            @pointerup="runToolbarReleaseAction($event, () => emit('align', action.command))"
            @pointercancel="stopToolbarPointerEvent"
            @mousedown="stopToolbarPointerEvent"
            @mouseup="stopToolbarPointerEvent"
            @touchstart="stopToolbarPointerEvent"
            @touchend="stopToolbarPointerEvent"
            @touchcancel="stopToolbarPointerEvent"
            @dragstart="stopToolbarDragEvent"
            @click="runToolbarClickAction($event, () => emit('align', action.command))"
          >
            <img class="floating-selection-toolbar__icon" :src="action.icon" :alt="action.label" draggable="false" />
          </button>
        </el-tooltip>
      </div>
      <div v-if="toolbarActions.showDistribution" class="floating-selection-toolbar__separator" aria-hidden="true"></div>
      <div v-if="toolbarActions.showDistribution" class="floating-selection-toolbar__group" role="group" aria-label="分布">
        <el-tooltip v-for="action in floatingToolbarDistributionActions" :key="action.command" :content="action.label" placement="top">
          <button
            class="floating-selection-toolbar__button"
            type="button"
            :aria-label="action.label"
            @pointerdown="stopToolbarPointerEvent"
            @pointerup="runToolbarReleaseAction($event, () => emit('align', action.command))"
            @pointercancel="stopToolbarPointerEvent"
            @mousedown="stopToolbarPointerEvent"
            @mouseup="stopToolbarPointerEvent"
            @touchstart="stopToolbarPointerEvent"
            @touchend="stopToolbarPointerEvent"
            @touchcancel="stopToolbarPointerEvent"
            @dragstart="stopToolbarDragEvent"
            @click="runToolbarClickAction($event, () => emit('align', action.command))"
          >
            <img class="floating-selection-toolbar__icon" :src="action.icon" :alt="action.label" draggable="false" />
          </button>
        </el-tooltip>
      </div>
      <div v-if="toolbarActions.showGroup || toolbarActions.showUngroup" class="floating-selection-toolbar__separator" aria-hidden="true"></div>
      <div v-if="toolbarActions.showGroup || toolbarActions.showUngroup" class="floating-selection-toolbar__group" role="group" aria-label="组合">
        <el-tooltip v-if="toolbarActions.showGroup" content="组合" placement="top">
          <button
            class="floating-selection-toolbar__button"
            type="button"
            aria-label="组合"
            @pointerdown="stopToolbarPointerEvent"
            @pointerup="runToolbarReleaseAction($event, () => emit('group'))"
            @pointercancel="stopToolbarPointerEvent"
            @mousedown="stopToolbarPointerEvent"
            @mouseup="stopToolbarPointerEvent"
            @touchstart="stopToolbarPointerEvent"
            @touchend="stopToolbarPointerEvent"
            @touchcancel="stopToolbarPointerEvent"
            @dragstart="stopToolbarDragEvent"
            @click="runToolbarClickAction($event, () => emit('group'))"
          >
            <img
              class="floating-selection-toolbar__icon"
              :src="floatingToolbarGroupAction.icon"
              :alt="floatingToolbarGroupAction.label"
              draggable="false"
            />
          </button>
        </el-tooltip>
        <el-tooltip v-if="toolbarActions.showUngroup" content="取消组合" placement="top">
          <button
            class="floating-selection-toolbar__button"
            type="button"
            aria-label="取消组合"
            @pointerdown="stopToolbarPointerEvent"
            @pointerup="runToolbarReleaseAction($event, () => emit('ungroup'))"
            @pointercancel="stopToolbarPointerEvent"
            @mousedown="stopToolbarPointerEvent"
            @mouseup="stopToolbarPointerEvent"
            @touchstart="stopToolbarPointerEvent"
            @touchend="stopToolbarPointerEvent"
            @touchcancel="stopToolbarPointerEvent"
            @dragstart="stopToolbarDragEvent"
            @click="runToolbarClickAction($event, () => emit('ungroup'))"
          >
            <img
              class="floating-selection-toolbar__icon"
              :src="floatingToolbarUngroupAction.icon"
              :alt="floatingToolbarUngroupAction.label"
              draggable="false"
            />
          </button>
        </el-tooltip>
      </div>
    </template>
    <template v-else>
      <div v-if="toolbarActions.showUngroup" class="floating-selection-toolbar__group" role="group" aria-label="组合">
        <el-tooltip content="取消组合" placement="top">
          <button
            class="floating-selection-toolbar__button"
            type="button"
            aria-label="取消组合"
            @pointerdown="stopToolbarPointerEvent"
            @pointerup="runToolbarReleaseAction($event, () => emit('ungroup'))"
            @pointercancel="stopToolbarPointerEvent"
            @mousedown="stopToolbarPointerEvent"
            @mouseup="stopToolbarPointerEvent"
            @touchstart="stopToolbarPointerEvent"
            @touchend="stopToolbarPointerEvent"
            @touchcancel="stopToolbarPointerEvent"
            @dragstart="stopToolbarDragEvent"
            @click="runToolbarClickAction($event, () => emit('ungroup'))"
          >
            <img
              class="floating-selection-toolbar__icon"
              :src="floatingToolbarUngroupAction.icon"
              :alt="floatingToolbarUngroupAction.label"
              draggable="false"
            />
          </button>
        </el-tooltip>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.floating-selection-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  box-sizing: border-box;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background-color: var(--el-fill-color-blank);
  box-shadow: var(--el-box-shadow-light);
  pointer-events: auto;
}

.floating-selection-toolbar__group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.floating-selection-toolbar__separator {
  width: 1px;
  height: 20px;
  background-color: var(--el-border-color-light);
}

.floating-selection-toolbar__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background-color: var(--el-fill-color-blank);
  color: var(--el-text-color-regular);
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background-color: var(--el-fill-color-lighter);
    color: var(--el-color-primary);
    outline: none;
  }

  &:disabled {
    color: var(--el-text-color-disabled);
    cursor: not-allowed;
  }
}

.floating-selection-toolbar__icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
  display: block;
}

.floating-selection-toolbar__button:disabled .floating-selection-toolbar__icon {
  opacity: 0.45;
}
</style>
