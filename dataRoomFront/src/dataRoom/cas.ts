export interface CasConfig {
  enabled: boolean
  loginUrl: string
  logoutUrl: string
}

interface CasEnv {
  VITE_CAS_ENABLE?: string
  VITE_CAS_LOGIN_URL?: string
  VITE_CAS_LOGOUT_URL?: string
}

interface CasErrorPresentation {
  title: string
  description: string
}

type QueryValue = string | null | Array<string | null> | undefined

const DEFAULT_CAS_ERROR: CasErrorPresentation = {
  title: '单点登录失败',
  description: '无法完成单点登录，请返回登录页后重试。',
}

const CAS_ERROR_PRESENTATIONS: Record<string, CasErrorPresentation> = {
  disabled: {
    title: '单点登录未启用',
    description: '当前服务未开启 CAS 单点登录。',
  },
  ticketMissing: {
    title: '登录凭证缺失',
    description: 'CAS 回调中没有有效的登录凭证。',
  },
  ticketInvalid: {
    title: '登录凭证无效',
    description: 'CAS 登录凭证无效或已经过期。',
  },
  serviceUnavailable: {
    title: '认证服务不可用',
    description: '暂时无法连接 CAS 认证服务，请稍后重试。',
  },
  userNotFound: {
    title: '本地用户不存在',
    description: 'CAS 认证已通过，但 DataRoom 中没有对应账号。',
  },
  userUnavailable: {
    title: '当前账号不可用',
    description: '对应账号已被禁用、锁定或超过有效期。',
  },
  loginError: DEFAULT_CAS_ERROR,
}

export const readCasConfig = (env: CasEnv): CasConfig => ({
  enabled: env.VITE_CAS_ENABLE === 'true',
  loginUrl: env.VITE_CAS_LOGIN_URL?.trim() || '',
  logoutUrl: env.VITE_CAS_LOGOUT_URL?.trim() || '',
})

export const getCasConfig = (): CasConfig => readCasConfig(import.meta.env)

export const resolveCasError = (code: unknown): CasErrorPresentation => {
  if (typeof code !== 'string') {
    return DEFAULT_CAS_ERROR
  }
  return CAS_ERROR_PRESENTATIONS[code] || DEFAULT_CAS_ERROR
}

export const consumeDataRoomToken = (query: Record<string, QueryValue>, tokenKey: string) => {
  const tokenValue = query[tokenKey]
  const token = Array.isArray(tokenValue) ? tokenValue.find((value): value is string => typeof value === 'string') : tokenValue
  const cleanQuery = { ...query }
  delete cleanQuery[tokenKey]
  return {
    token: typeof token === 'string' ? token.trim() : '',
    query: cleanQuery,
  }
}

export const resolveLogoutTarget = (config: CasConfig): string => {
  if (config.enabled && config.logoutUrl) {
    return config.logoutUrl
  }
  return '/login'
}
