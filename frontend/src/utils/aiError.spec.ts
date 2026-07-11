import { describe, expect, it } from 'vitest'
import { normalizeAiError } from './aiError'

describe('normalizeAiError', () => {
  it('formats code + message for store', () => {
    const err = Object.assign(new Error('missing api key'), { code: 'CONFIG_MISSING' })
    const info = normalizeAiError(err)
    expect(info.storeMessage).toBe('CONFIG_MISSING: missing api key')
    expect(info.code).toBe('CONFIG_MISSING')
    expect(info.configTip).toBe('请配置后端环境变量 STUDIO_LLM_API_KEY')
  })

  it('omits code prefix when code absent', () => {
    const info = normalizeAiError(new Error('timeout'))
    expect(info.storeMessage).toBe('timeout')
    expect(info.configTip).toBeUndefined()
  })

  it('handles non-Error values', () => {
    const info = normalizeAiError('boom')
    expect(info.storeMessage).toBe('boom')
    expect(info.message).toBe('boom')
  })
})
