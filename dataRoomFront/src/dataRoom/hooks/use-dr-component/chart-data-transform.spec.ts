import { strict as assert } from 'node:assert'
import { transformChartData } from './chart-data-transform.ts'
import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'
import type { CanvasInst } from '@/dataRoom/designer/types/CanvasInst.ts'

const createChart = (script: string): ChartConfig<unknown> => ({
  id: 'chart-1',
  i: 'chart-1',
  type: 'DrBarChart',
  title: '测试柱状图',
  w: 100,
  h: 100,
  x: 0,
  y: 0,
  z: 1,
  rotateX: 0,
  rotateY: 0,
  rotateZ: 0,
  props: {},
  dataset: {
    code: 'sales',
    datasetType: 'json',
    fields: {},
    script,
    params: {},
  },
})

const globalValues = new Map<string, unknown>([
  ['region', '华东'],
])

const canvasInst = {
  getGlobalVariableValue: (name: string) => globalValues.get(name) ?? '',
  updateGlobalVariableValue: (name: string, value: string) => {
    globalValues.set(name, value)
  },
} as CanvasInst

const sourceData = [
  { month: '一月', amount: '12', region: '华东' },
  { month: '二月', amount: '8', region: '华南' },
]

const passthrough = await transformChartData({
  chart: createChart(''),
  canvasInst,
  data: sourceData,
  params: { year: 2026 },
  logger: console,
  notifyError: () => {},
})
assert.equal(passthrough, sourceData, 'empty script should pass through original data')

const transformed = await transformChartData({
  chart: createChart(`
const region = bep.globals.get('region')
return bep.data
  .filter(item => item.region === region)
  .map(item => ({
    time: item.month,
    value: Number(item.amount),
    year: bep.params.year,
  }))
`),
  canvasInst,
  data: sourceData,
  params: { year: 2026 },
  logger: console,
  notifyError: () => {},
})
assert.deepEqual(transformed, [
  { time: '一月', value: 12, year: 2026 },
], 'script should transform data and read globals/params through bep')

await transformChartData({
  chart: createChart(`
bep.globals.set('region', '华北')
return bep.globals.get('region')
`),
  canvasInst,
  data: sourceData,
  params: {},
  logger: console,
  notifyError: () => {},
})
assert.equal(globalValues.get('region'), '华北', 'script should update static global variables through bep.globals.set')

const warnings: unknown[] = []
const mutableData = [
  { month: '三月', amount: '21' },
]
const noReturnResult = await transformChartData({
  chart: createChart('bep.data.forEach(item => { item.value = Number(item.amount) })'),
  canvasInst,
  data: mutableData,
  params: {},
  logger: {
    warn: (...args: unknown[]) => warnings.push(args),
    error: () => {},
  },
  notifyError: () => {},
})
assert.equal(noReturnResult, mutableData, 'script without return should use original data so in-place changes are preserved')
assert.deepEqual(mutableData, [
  { month: '三月', amount: '21', value: 21 },
], 'script without return should preserve in-place field updates')
assert.equal(warnings.length, 1, 'script without return should warn once')

const errors: unknown[] = []
const notifications: string[] = []
const errorResult = await transformChartData({
  chart: createChart('throw new Error("boom")'),
  canvasInst,
  data: sourceData,
  params: {},
  logger: {
    warn: () => {},
    error: (...args: unknown[]) => errors.push(args),
  },
  notifyError: (message: string) => notifications.push(message),
})
assert.deepEqual(errorResult, [], 'script error should produce empty array')
assert.equal(errors.length, 1, 'script error should be logged once')
assert.deepEqual(notifications, ['数据处理脚本执行失败'], 'script error should notify with Element Plus message text')
