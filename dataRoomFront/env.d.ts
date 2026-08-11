/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_RESOURCE_BASE_URL: string
  readonly VITE_TOKEN_KEY: string
  readonly VITE_ROUTER_MODE: string
  readonly VITE_CAS_ENABLE?: string
  readonly VITE_CAS_LOGIN_URL?: string
  readonly VITE_CAS_LOGOUT_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
