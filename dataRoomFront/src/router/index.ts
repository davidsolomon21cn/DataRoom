import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import config from '../../package.json'
import { getCookieName, setCookie } from '@/dataRoom/utils/cookie'
import { appRoutes } from './routes'
import { consumeDataRoomToken } from '@/dataRoom/cas'
import { resolveRouterMode } from './router-mode'

const routerMode = resolveRouterMode(import.meta.env.VITE_ROUTER_MODE)
const routerHistory =
  routerMode === 'hash'
    ? createWebHashHistory(import.meta.env.BASE_URL)
    : createWebHistory(import.meta.env.BASE_URL)

const router = createRouter({
  history: routerHistory,
  routes: appRoutes,
})

router.beforeEach((to) => {
  const { token, query } = consumeDataRoomToken(to.query, getCookieName())
  if (token) {
    setCookie(token)
    return {
      path: to.path,
      query,
      hash: to.hash,
      replace: true,
    }
  }
})

console.log(
  '%cDataRoom%cv%s%c 请给我一个Star %s',
  `font-size:24px;color:#3478f6;vertical-align: bottom;background:#ecf2fd;padding:0 10px;border-radius:8px;`,
  `font-size:18px;color:#666;vertical-align: bottom;margin-left:12px;`,
  config.version,
  `font-size:18px;color:#999;vertical-align: bottom;margin-left:15px;text-decoration: none;`,
  `https://gitee.com/gcpaas/DataRoom`,
)
export default router
