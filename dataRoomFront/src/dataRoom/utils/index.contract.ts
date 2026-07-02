/// <reference types="node" />

import assert from 'node:assert/strict'
import test from 'node:test'

import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'
import { getChartByElement } from './index.ts'

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

test('resolves a chart from a nested element inside the chart wrapper', () => {
  const handle = {
    getAttribute: () => null,
    closest: () => ({
      getAttribute: (name: string) => (name === 'data-dr-id' ? 'chart-a' : null),
    }),
  } as unknown as HTMLElement

  assert.equal(getChartByElement(handle, [chart('chart-a')]).id, 'chart-a')
})
