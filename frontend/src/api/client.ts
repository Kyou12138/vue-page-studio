import type { PageDsl } from '../types/dsl'

const base = import.meta.env.VITE_API_BASE ?? ''

interface ApiEnvelope<T> {
  success: boolean
  data?: T
  code?: string
  message?: string
  details?: string[]
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const url = `${base}${path}`
  let res: Response
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw Object.assign(new Error(`网络请求失败：${msg}（${url}）`), { code: 'NETWORK_ERROR' })
  }

  const text = await res.text()
  let json: ApiEnvelope<T>
  try {
    json = JSON.parse(text) as ApiEnvelope<T>
  } catch {
    // 常见：CORS 失败时 Spring 返回纯文本 "Invalid CORS request"
    const snippet = text.trim().slice(0, 200) || res.statusText
    throw Object.assign(
      new Error(
        snippet.includes('CORS')
          ? `跨域被拒绝（CORS）。请确认后端允许当前前端源，或使用 Vite 代理访问 /api。原始响应：${snippet}`
          : `服务器返回非 JSON（HTTP ${res.status}）：${snippet}`,
      ),
      { code: 'BAD_RESPONSE' },
    )
  }

  if (!res.ok || !json.success) {
    const err = new Error(json.message || json.code || `request failed (${res.status})`) as Error & {
      code?: string
      details?: string[]
    }
    err.code = json.code
    err.details = json.details
    throw err
  }
  return json.data as T
}

/** 导出 Vue SFC 源码 */
export function exportVue(dsl: PageDsl) {
  return post<{ code: string }>('/api/export/vue', dsl).then((d) => d.code)
}

/** AI 根据描述生成 PageDSL */
export function generateDsl(body: { description: string; pageType?: string; style?: string }) {
  return post<PageDsl>('/api/ai/generate', body)
}

/** AI 按指令修改现有 PageDSL */
export function modifyDsl(dsl: PageDsl, instruction: string) {
  return post<PageDsl>('/api/ai/modify', { dsl, instruction })
}

/** 校验 PageDSL */
export function validateDsl(dsl: PageDsl) {
  return post<{ valid: boolean }>('/api/dsl/validate', dsl)
}
