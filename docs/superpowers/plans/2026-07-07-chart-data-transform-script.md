# Chart Data Transform Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable the designer's component-level data processing script so every standard component receives script-transformed dataset data before `changeData`.

**Architecture:** Add one focused frontend transform module that executes `chart.dataset.script` with a single `bep` context object. Wire the module into both ordinary `useDrComponent` refreshes and realtime `useRealtimeDataset` pushes so chart components do not need per-component changes.

**Tech Stack:** Vue 3 Composition API, TypeScript, Element Plus `ElMessage`, existing `npx tsx` lightweight regression scripts, Vite/vue-tsc.

---

## File Structure

- Create `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.ts`
  - Owns `bep` creation, JavaScript function-body execution, fallback behavior, logging, and Element Plus notification.
- Create `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts`
  - Lightweight executable TypeScript regression test for the pure transform behavior.
- Create `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts`
  - Static contract test that locks both data entry points to `transformChartData`.
- Modify `dataRoomFront/src/dataRoom/hooks/use-dr-component/index.ts`
  - Runs `transformChartData` after `datasetApi.run4Chart()` and before component `changeData()`.
- Modify `dataRoomFront/src/dataRoom/hooks/use-realtime-dataset/index.ts`
  - Tracks chart-level params and runs `transformChartData` before scheduling realtime component updates.

## Task 1: Transform Module

**Files:**
- Create: `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.ts`
- Create: `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts`

- [ ] **Step 1: Write the failing transform spec**

Create `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts`:

```ts
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
const noReturnResult = await transformChartData({
  chart: createChart('const rows = bep.data.map(item => item.month)'),
  canvasInst,
  data: sourceData,
  params: {},
  logger: {
    warn: (...args: unknown[]) => warnings.push(args),
    error: () => {},
  },
  notifyError: () => {},
})
assert.deepEqual(noReturnResult, [], 'script without return should produce empty array')
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
```

- [ ] **Step 2: Run the transform spec and verify it fails**

Run:

```bash
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts
```

Expected: fails because `chart-data-transform.ts` does not exist.

- [ ] **Step 3: Implement the transform module**

Create `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.ts`:

```ts
import { ElMessage } from 'element-plus'
import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'
import type { CanvasInst } from '@/dataRoom/designer/types/CanvasInst.ts'

type TransformLogger = Pick<Console, 'error' | 'warn'>
type NotifyError = (message: string) => void

export interface ChartDataTransformBep {
  canvasInst: CanvasInst
  chart: ChartConfig<unknown>
  data: unknown
  params: Record<string, unknown>
  globals: {
    get: (name: string) => unknown
    set: (name: string, value: string) => void
  }
}

export interface ChartDataTransformOptions {
  chart: ChartConfig<unknown>
  canvasInst: CanvasInst
  data: unknown
  params: Record<string, unknown>
  logger?: TransformLogger
  notifyError?: NotifyError
}

const defaultNotifyError: NotifyError = (message) => {
  ElMessage.error(message)
}

const createTransformBep = (
  chart: ChartConfig<unknown>,
  canvasInst: CanvasInst,
  data: unknown,
  params: Record<string, unknown>,
): ChartDataTransformBep => ({
  canvasInst,
  chart,
  data,
  params,
  globals: {
    get: (name: string) => canvasInst.getGlobalVariableValue(name),
    set: (name: string, value: string) => canvasInst.updateGlobalVariableValue(name, value),
  },
})

export const transformChartData = async ({
  chart,
  canvasInst,
  data,
  params,
  logger = console,
  notifyError = defaultNotifyError,
}: ChartDataTransformOptions): Promise<unknown> => {
  const script = chart.dataset?.script?.trim()
  if (!script) {
    return data
  }

  const bep = createTransformBep(chart, canvasInst, data, params)

  try {
    const transformFunc = new Function('bep', script) as (bep: ChartDataTransformBep) => unknown | Promise<unknown>
    const result = await transformFunc(bep)
    if (result === undefined) {
      logger.warn(`组件 ${chart.type}: ${chart.id} 数据处理脚本未返回结果`)
      return []
    }
    return result
  } catch (error) {
    logger.error(`组件 ${chart.type}: ${chart.id} 数据处理脚本执行失败:`, error)
    notifyError('数据处理脚本执行失败')
    return []
  }
}
```

- [ ] **Step 4: Run the transform spec and verify it passes**

Run:

```bash
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts
```

Expected: command exits with status 0.

- [ ] **Step 5: Commit transform module**

Run:

```bash
git add dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.ts dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts
git commit -m "feat: add chart data transform script runtime"
```

## Task 2: Ordinary Data Refresh Integration

**Files:**
- Modify: `dataRoomFront/src/dataRoom/hooks/use-dr-component/index.ts`
- Create: `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts`

- [ ] **Step 1: Write the failing integration contract for ordinary refresh**

Create `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts`:

```ts
import { strict as assert } from 'node:assert'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const useDrComponentSource = readFileSync(resolve('src/dataRoom/hooks/use-dr-component/index.ts'), 'utf8')

assert(
  useDrComponentSource.includes("import { transformChartData } from './chart-data-transform.ts'"),
  'useDrComponent should import transformChartData',
)
assert(
  /const\s+transformedData\s*=\s*await\s+transformChartData\(\{\s*chart,\s*canvasInst,\s*data:\s*res\.data,\s*params:\s*paramMap,\s*\}\)/s.test(useDrComponentSource),
  'useDrComponent should transform run4Chart response data with chart, canvasInst, and paramMap',
)
assert(
  useDrComponentSource.includes('await changeData(transformedData)'),
  'useDrComponent should pass transformedData to changeData',
)
assert(
  !useDrComponentSource.includes('await changeData(res.data)'),
  'useDrComponent should no longer pass raw res.data directly to changeData',
)
```

- [ ] **Step 2: Run the integration contract and verify it fails**

Run:

```bash
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
```

Expected: fails because `useDrComponent` has not imported or called `transformChartData`.

- [ ] **Step 3: Wire ordinary refresh through `transformChartData`**

Modify `dataRoomFront/src/dataRoom/hooks/use-dr-component/index.ts`.

Add this import:

```ts
import { transformChartData } from './chart-data-transform.ts'
```

Replace the end of `autoRefreshData`:

```ts
const res = await datasetApi.run4Chart({
  datasetCode: chart.dataset.code,
  paramMap: paramMap
})
await changeData(res.data)
```

with:

```ts
const res = await datasetApi.run4Chart({
  datasetCode: chart.dataset.code,
  paramMap: paramMap
})
const transformedData = await transformChartData({
  chart,
  canvasInst,
  data: res.data,
  params: paramMap,
})
await changeData(transformedData)
```

- [ ] **Step 4: Run targeted tests**

Run:

```bash
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts
```

Expected: both commands exit with status 0.

- [ ] **Step 5: Commit ordinary refresh integration**

Run:

```bash
git add dataRoomFront/src/dataRoom/hooks/use-dr-component/index.ts dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
git commit -m "feat: transform ordinary chart dataset data"
```

## Task 3: Realtime Dataset Integration

**Files:**
- Modify: `dataRoomFront/src/dataRoom/hooks/use-realtime-dataset/index.ts`
- Modify: `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts`

- [ ] **Step 1: Extend the integration contract for realtime data**

Append this block to `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts`:

```ts
const realtimeSource = readFileSync(resolve('src/dataRoom/hooks/use-realtime-dataset/index.ts'), 'utf8')

assert(
  realtimeSource.includes("import { transformChartData } from '@/dataRoom/hooks/use-dr-component/chart-data-transform.ts'"),
  'useRealtimeDataset should import transformChartData',
)
assert(
  realtimeSource.includes('const datasetChartStateMap = new Map<string, DatasetChartState[]>()'),
  'useRealtimeDataset should track chart state per dataset, not only chart ids',
)
assert(
  /const\s+transformedData\s*=\s*await\s+transformChartData\(\{\s*chart:\s*chartState\.chart,\s*canvasInst,\s*data:\s*normalizedData,\s*params:\s*chartState\.paramMap,\s*\}\)/s.test(realtimeSource),
  'useRealtimeDataset should transform realtime data per chart before scheduling updates',
)
assert(
  realtimeSource.includes('scheduleChartUpdate(chartState.chart.id, transformedData)'),
  'useRealtimeDataset should schedule transformed realtime data',
)
```

- [ ] **Step 2: Run the integration contract and verify it fails on realtime assertions**

Run:

```bash
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
```

Expected: ordinary refresh assertions pass, realtime assertions fail because realtime integration is not wired yet.

- [ ] **Step 3: Replace realtime chart id map with chart state map**

Modify `dataRoomFront/src/dataRoom/hooks/use-realtime-dataset/index.ts`.

Add this import:

```ts
import { transformChartData } from '@/dataRoom/hooks/use-dr-component/chart-data-transform.ts'
```

Replace:

```ts
interface DatasetSubscription {
  datasetCode: string
  paramMap: Record<string, unknown>
}
```

with:

```ts
interface DatasetSubscription {
  datasetCode: string
  paramMap: Record<string, unknown>
}

interface DatasetChartState {
  chart: ChartConfig<unknown>
  paramMap: Record<string, unknown>
}
```

Replace:

```ts
const datasetChartMap = new Map<string, string[]>()
```

with:

```ts
const datasetChartStateMap = new Map<string, DatasetChartState[]>()
```

Inside `buildDatasetIndex`, replace:

```ts
datasetChartMap.clear()
```

with:

```ts
datasetChartStateMap.clear()
```

Inside the `walkCharts` callback, replace:

```ts
const chartIds = datasetChartMap.get(datasetCode) || []
chartIds.push(chart.id)
datasetChartMap.set(datasetCode, chartIds)

subscriptions.push({
  datasetCode,
  paramMap: canvasInst.fillDatasetParams(chart),
})
```

with:

```ts
const paramMap = canvasInst.fillDatasetParams(chart)
const chartStates = datasetChartStateMap.get(datasetCode) || []
chartStates.push({
  chart,
  paramMap,
})
datasetChartStateMap.set(datasetCode, chartStates)

subscriptions.push({
  datasetCode,
  paramMap,
})
```

In `sendSubscriptions`, replace:

```ts
datasetCodes: [...datasetChartMap.keys()],
```

with:

```ts
datasetCodes: [...datasetChartStateMap.keys()],
```

In `reload`, replace:

```ts
const datasetCodes = [...datasetChartMap.keys()]
```

with:

```ts
const datasetCodes = [...datasetChartStateMap.keys()]
```

- [ ] **Step 4: Transform realtime data before scheduling updates**

In `dataRoomFront/src/dataRoom/hooks/use-realtime-dataset/index.ts`, replace `dispatchDatasetData`:

```ts
const dispatchDatasetData = (datasetCode: string, data: unknown) => {
  const chartIds = datasetChartMap.get(datasetCode) || []
  const normalizedData = normalizeDatasetData(data)

  chartIds.forEach((chartId) => {
    scheduleChartUpdate(chartId, normalizedData)
  })
}
```

with:

```ts
const dispatchDatasetData = async (datasetCode: string, data: unknown) => {
  const chartStates = datasetChartStateMap.get(datasetCode) || []
  const normalizedData = normalizeDatasetData(data)

  for (const chartState of chartStates) {
    const transformedData = await transformChartData({
      chart: chartState.chart,
      canvasInst,
      data: normalizedData,
      params: chartState.paramMap,
    })
    scheduleChartUpdate(chartState.chart.id, transformedData)
  }
}
```

In `socket.onmessage`, replace:

```ts
dispatchDatasetData(message.datasetCode, message.data)
```

with:

```ts
void dispatchDatasetData(message.datasetCode, message.data)
```

- [ ] **Step 5: Run targeted tests**

Run:

```bash
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts
```

Expected: both commands exit with status 0.

- [ ] **Step 6: Commit realtime integration**

Run:

```bash
git add dataRoomFront/src/dataRoom/hooks/use-realtime-dataset/index.ts dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
git commit -m "feat: transform realtime chart dataset data"
```

## Task 4: Final Verification

**Files:**
- Verify: `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.ts`
- Verify: `dataRoomFront/src/dataRoom/hooks/use-dr-component/index.ts`
- Verify: `dataRoomFront/src/dataRoom/hooks/use-realtime-dataset/index.ts`

- [ ] **Step 1: Run targeted transform tests**

Run:

```bash
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts
cd dataRoomFront && npx tsx src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
```

Expected: both commands exit with status 0.

- [ ] **Step 2: Run frontend type-check**

Run:

```bash
cd dataRoomFront && npm run type-check
```

Expected: `vue-tsc --build` exits with status 0.

- [ ] **Step 3: Inspect style scope**

Run:

```bash
git diff --name-only HEAD~3..HEAD
```

Expected: changed files are TypeScript files under `dataRoomFront/src/dataRoom/hooks/` and no Vue, SCSS, or CSS files were modified. If this expectation holds, `npm run lint` is not required by the project style rule for this feature.

- [ ] **Step 4: Commit verification note if no code changed**

No commit is required when Step 1 and Step 2 pass without file changes. If a verification fix was needed, commit only the changed files:

```bash
git add dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.ts dataRoomFront/src/dataRoom/hooks/use-dr-component/index.ts dataRoomFront/src/dataRoom/hooks/use-realtime-dataset/index.ts dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.spec.ts dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.integration.contract.ts
git commit -m "fix: verify chart data transform script"
```

## Self-Review

- Spec coverage: Task 1 implements JavaScript function-body execution, `bep`, globals, params, warning, error notification, and `[]` fallback. Task 2 covers ordinary data refresh. Task 3 covers realtime dataset pushes. Task 4 covers targeted tests and `npm run type-check`.
- Placeholder scan: The plan contains concrete file paths, code blocks, commands, and expected outcomes. No unresolved placeholder remains.
- Type consistency: The plan consistently uses `transformChartData`, `ChartDataTransformOptions`, `DatasetChartState`, `datasetChartStateMap`, `paramMap`, and `transformedData`.
