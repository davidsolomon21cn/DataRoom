export type RouterMode = 'hash' | 'history'

export const resolveRouterMode = (value: string): RouterMode => {
  if (value === 'hash' || value === 'history') {
    return value
  }
  throw new Error(`VITE_ROUTER_MODE 配置无效: ${value}`)
}
