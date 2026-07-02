import type { CSSProperties } from 'vue'
import type { ChartConfig } from '@/dataRoom/components/type/ChartConfig.ts'

export const getVisualScreenChartWrapperStyle = (chart: ChartConfig<unknown>): CSSProperties => {
  let transform = `translate(${chart.x}px,${chart.y}px)`
  if (chart.rotateX) {
    transform += ` rotateX(${chart.rotateX}deg)`
  }
  if (chart.rotateY) {
    transform += ` rotateY(${chart.rotateY}deg)`
  }
  if (chart.rotateZ) {
    transform += ` rotateZ(${chart.rotateZ}deg)`
  }

  return {
    position: 'absolute',
    transform,
    width: `${chart.w}px`,
    height: `${chart.h}px`,
    zIndex: chart.z,
  }
}
