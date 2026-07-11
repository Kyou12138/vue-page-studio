import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import type { PageDsl } from '../types/dsl'
import listUserFixture from '../fixtures/list-user.json'
import { hashDsl, usePageStore } from './pageStore'

vi.mock('../api/client', () => ({
  exportVue: vi.fn(),
}))

import { exportVue } from '../api/client'

const exportVueMock = vi.mocked(exportVue)

function minimalDsl(children: PageDsl['children']): PageDsl {
  return {
    version: '1',
    pageType: 'list',
    style: 'element-plus',
    title: 'test',
    children,
  }
}

describe('pageStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    exportVueMock.mockReset()
  })

  it('undo restores children order', () => {
    const store = usePageStore()
    const original = minimalDsl([
      { id: 'a', type: 'TextBlock', props: { content: 'a' } },
      { id: 'b', type: 'TextBlock', props: { content: 'b' } },
    ])
    store.loadDsl(original)

    // loadDsl 会 push 空 history 前的 null 不入栈；再 reorder
    store.reorderRoot([
      { id: 'b', type: 'TextBlock', props: { content: 'b' } },
      { id: 'a', type: 'TextBlock', props: { content: 'a' } },
    ])
    expect(store.pageDsl?.children.map((c) => c.id)).toEqual(['b', 'a'])

    store.undo()
    expect(store.pageDsl?.children.map((c) => c.id)).toEqual(['a', 'b'])
  })

  it('redo restores reordered children', () => {
    const store = usePageStore()
    store.loadDsl(
      minimalDsl([
        { id: 'a', type: 'TextBlock', props: { content: 'a' } },
        { id: 'b', type: 'TextBlock', props: { content: 'b' } },
      ]),
    )
    store.reorderRoot([
      { id: 'b', type: 'TextBlock', props: { content: 'b' } },
      { id: 'a', type: 'TextBlock', props: { content: 'a' } },
    ])
    store.undo()
    store.redo()
    expect(store.pageDsl?.children.map((c) => c.id)).toEqual(['b', 'a'])
  })

  it('aiLoading blocks mutations', () => {
    const store = usePageStore()
    store.loadDsl(
      minimalDsl([{ id: 'n1', type: 'PageHeader', props: { title: '旧标题', actions: [] } }]),
    )
    // 清掉 load 产生的 history，避免干扰
    store.history = []
    store.redoStack = []

    store.aiLoading = true
    store.updateProps('n1', { title: '新标题' })
    expect(store.pageDsl?.children[0].props.title).toBe('旧标题')
    expect(store.history).toHaveLength(0)

    store.reorderRoot([])
    expect(store.pageDsl?.children).toHaveLength(1)

    store.insertFromPalette('TextBlock', null, 'append')
    expect(store.pageDsl?.children).toHaveLength(1)

    store.removeNode('n1')
    expect(store.pageDsl?.children).toHaveLength(1)

    // undo/redo 也 no-op
    store.history = [
      minimalDsl([{ id: 'other', type: 'TextBlock', props: { content: 'x' } }]),
    ]
    store.undo()
    expect(store.pageDsl?.children[0].id).toBe('n1')
  })

  it('loadDsl from list-user fixture and clear missing selection', () => {
    const store = usePageStore()
    store.selectedId = 'gone'
    store.loadDsl(listUserFixture as PageDsl)
    expect(store.pageDsl?.title).toBe('用户管理')
    expect(store.pageDsl?.children.length).toBeGreaterThan(0)
    expect(store.selectedId).toBeNull()
  })

  it('setDslFromAi pushes history and clears error', () => {
    const store = usePageStore()
    store.loadDsl(minimalDsl([{ id: 'a', type: 'TextBlock', props: { content: 'a' } }]))
    store.selectedId = 'a'
    store.errorMessage = 'prev'
    store.history = []

    const next = minimalDsl([{ id: 'b', type: 'TextBlock', props: { content: 'b' } }])
    store.setDslFromAi(next)

    expect(store.pageDsl?.children[0].id).toBe('b')
    expect(store.selectedId).toBeNull()
    expect(store.errorMessage).toBeNull()
    expect(store.history).toHaveLength(1)
    expect(store.history[0].children[0].id).toBe('a')
  })

  it('updateProps merges props and supports undo', () => {
    const store = usePageStore()
    store.loadDsl(
      minimalDsl([{ id: 'h', type: 'PageHeader', props: { title: 'T', actions: [] } }]),
    )
    store.history = []
    store.updateProps('h', { title: 'New' })
    expect(store.pageDsl?.children[0].props.title).toBe('New')
    store.undo()
    expect(store.pageDsl?.children[0].props.title).toBe('T')
  })

  it('insertFromPalette appends node with default props', () => {
    const store = usePageStore()
    store.loadDsl(minimalDsl([]))
    store.history = []
    store.insertFromPalette('TextBlock', null, 'append')
    expect(store.pageDsl?.children).toHaveLength(1)
    expect(store.pageDsl?.children[0].type).toBe('TextBlock')
    expect(store.selectedId).toBe(store.pageDsl?.children[0].id)
  })

  it('removeNode drops node and clears selection', () => {
    const store = usePageStore()
    store.loadDsl(
      minimalDsl([
        { id: 'a', type: 'TextBlock', props: { content: 'a' } },
        { id: 'b', type: 'TextBlock', props: { content: 'b' } },
      ]),
    )
    store.selectedId = 'a'
    store.history = []
    store.removeNode('a')
    expect(store.pageDsl?.children.map((c) => c.id)).toEqual(['b'])
    expect(store.selectedId).toBeNull()
  })

  it('history capped at 10', () => {
    const store = usePageStore()
    store.loadDsl(minimalDsl([{ id: 'x', type: 'TextBlock', props: { content: '0' } }]))
    store.history = []
    for (let i = 0; i < 12; i++) {
      store.updateProps('x', { content: String(i) })
    }
    expect(store.history.length).toBeLessThanOrEqual(10)
  })

  it('setStyle updates style without AI', () => {
    const store = usePageStore()
    store.loadDsl(minimalDsl([]))
    store.setStyle('plain')
    expect(store.pageDsl?.style).toBe('plain')
  })

  it('refreshExport caches by hash', async () => {
    const store = usePageStore()
    const dsl = minimalDsl([{ id: 'a', type: 'TextBlock', props: { content: 'a' } }])
    store.loadDsl(dsl)
    exportVueMock.mockResolvedValueOnce('<template>ok</template>')

    await store.refreshExport()
    expect(exportVueMock).toHaveBeenCalledTimes(1)
    expect(store.exportCode).toBe('<template>ok</template>')
    expect(store.exportDslHash).toBe(hashDsl(store.pageDsl!))

    await store.refreshExport()
    expect(exportVueMock).toHaveBeenCalledTimes(1)
  })

  it('refreshExport sets error on failure', async () => {
    const store = usePageStore()
    store.loadDsl(minimalDsl([]))
    exportVueMock.mockRejectedValueOnce(new Error('export failed'))
    await store.refreshExport()
    expect(store.errorMessage).toBe('export failed')
  })

  it('addChat appends messages', () => {
    const store = usePageStore()
    store.addChat('user', 'hello')
    store.addChat('assistant', 'hi')
    expect(store.chatMessages).toEqual([
      { role: 'user', content: 'hello' },
      { role: 'assistant', content: 'hi' },
    ])
  })
})
