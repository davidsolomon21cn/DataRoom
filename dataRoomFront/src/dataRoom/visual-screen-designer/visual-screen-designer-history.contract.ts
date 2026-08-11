/// <reference types="node" />

import assert from 'node:assert/strict'
import test from 'node:test'

import type { VisualScreenPageBasicConfig } from '@/dataRoom/page-designer/type/VisualScreenPageBasicConfig.ts'
import type { VisualScreenPageConfig } from '@/dataRoom/page-designer/type/VisualScreenPageConfig.ts'
import type { PageStageEntity } from '@/dataRoom/page/type/PageStageEntity.ts'
import * as visualScreenHistory from './visual-screen-designer-history.ts'
import { createVisualScreenPageConfigPayload } from './visual-screen-designer-history.ts'

type VisualScreenHistoryModule = typeof visualScreenHistory & {
  normalizeVisualScreenIframeInteractionEnabled?: (value: unknown) => boolean
}

const basicConfigWithoutIframeInteraction = {
  background: { fill: 'color', color: '', url: '', opacity: 100, repeat: 'no-repeat' },
  size: { width: 1920, height: 1080, zoom: 'contain' },
} as VisualScreenPageBasicConfig

const pageStageEntity: PageStageEntity = {
  pageCode: 'screen-a',
  remark: '',
  pageType: 'visualScreen',
  pageStatus: 'design',
  pageConfig: {
    pageType: 'visualScreen',
    basicConfig: basicConfigWithoutIframeInteraction,
    globalVariableList: [],
    chartList: [],
  },
}

test('persists disabled iframe interaction for historical configs without the field', () => {
  const payload = createVisualScreenPageConfigPayload({
    pageStageEntity,
    chartList: [],
    basicConfig: basicConfigWithoutIframeInteraction,
    globalVariableList: [],
  })

  assert.equal((payload?.pageConfig as VisualScreenPageConfig).basicConfig.iframeInteractionEnabled, false)
})

test('preserves enabled iframe interaction in the saved page config', () => {
  const payload = createVisualScreenPageConfigPayload({
    pageStageEntity,
    chartList: [],
    basicConfig: {
      ...basicConfigWithoutIframeInteraction,
      iframeInteractionEnabled: true,
    },
    globalVariableList: [],
  })

  assert.equal((payload?.pageConfig as VisualScreenPageConfig).basicConfig.iframeInteractionEnabled, true)
})

test('normalizes persisted iframe interaction to an explicit boolean', () => {
  const normalize = (visualScreenHistory as VisualScreenHistoryModule).normalizeVisualScreenIframeInteractionEnabled

  assert.equal(typeof normalize, 'function')
  if (!normalize) {
    return
  }

  assert.equal(normalize(true), true)
  assert.equal(normalize(false), false)
  assert.equal(normalize(undefined), false)
  assert.equal(normalize('true'), false)
})
