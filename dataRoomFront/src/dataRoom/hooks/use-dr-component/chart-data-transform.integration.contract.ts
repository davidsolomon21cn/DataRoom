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
  /const\s+transformedData\s*=\s*await\s+transformChartData\(\{\s*chart:\s*chartState\.chart,\s*canvasInst:\s*canvasInst\s+as\s+CanvasInst,\s*data:\s*normalizedData,\s*params:\s*chartState\.paramMap,\s*\}\)/s.test(realtimeSource),
  'useRealtimeDataset should transform realtime data per chart before scheduling updates',
)
assert(
  realtimeSource.includes('scheduleChartUpdate(chartState.chart.id, transformedData)'),
  'useRealtimeDataset should schedule transformed realtime data',
)
