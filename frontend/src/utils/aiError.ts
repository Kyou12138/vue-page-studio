/** 带业务错误码的 API 异常（由 client.post 抛出） */
export type ApiErrorLike = Error & {
  code?: string
  details?: string[]
}

export interface AiErrorInfo {
  /** 展示给 store.errorMessage：code + message */
  storeMessage: string
  /** 原始 message */
  message: string
  code?: string
  /** CONFIG_MISSING 时的中文配置提示 */
  configTip?: string
}

/**
 * 将 generate/modify 失败的异常规范化为 UI 可用信息。
 */
export function normalizeAiError(e: unknown): AiErrorInfo {
  const err = e as ApiErrorLike
  const message =
    e instanceof Error ? e.message : e != null ? String(e) : 'request failed'
  const code = typeof err?.code === 'string' ? err.code : undefined
  const storeMessage = code ? `${code}: ${message}` : message
  const configTip =
    code === 'CONFIG_MISSING' ? '请配置后端环境变量 STUDIO_LLM_API_KEY' : undefined
  return { storeMessage, message, code, configTip }
}
