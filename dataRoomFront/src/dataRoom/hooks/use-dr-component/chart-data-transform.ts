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
      return data
    }
    return result
  } catch (error) {
    logger.error(`组件 ${chart.type}: ${chart.id} 数据处理脚本执行失败:`, error)
    notifyError('数据处理脚本执行失败')
    return []
  }
}
