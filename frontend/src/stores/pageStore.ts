import { defineStore } from 'pinia'
import { exportVue } from '../api/client'
import { createNode } from '../dsl/defaultProps'
import {
  appendIntoSlot,
  insertNodeDeep,
  replaceSlotList,
  type DropSlot,
  type InsertPosition,
} from '../dsl/insertNode'
import type { DslNode, PageDsl, PageType, StyleType } from '../types/dsl'

/** 聊天消息 */
export type ChatRole = 'user' | 'assistant' | 'system'

export interface ChatMessage {
  role: ChatRole
  content: string
}

const HISTORY_CAP = 10

/**
 * 深拷贝。
 * Pinia state 为 reactive Proxy，structuredClone 无法直接克隆，
 * 故先 JSON 往返得到 plain object，再 structuredClone。
 */
export function deepClone<T>(value: T): T {
  return structuredClone(JSON.parse(JSON.stringify(value))) as T
}

/** 将 DSL 序列化为可比较的哈希字符串 */
export function hashDsl(dsl: PageDsl): string {
  return JSON.stringify(dsl)
}

function asNodeList(value: unknown): DslNode[] {
  if (!Array.isArray(value)) return []
  return value.filter((item): item is DslNode => {
    return (
      item != null &&
      typeof item === 'object' &&
      typeof (item as DslNode).id === 'string' &&
      typeof (item as DslNode).type === 'string'
    )
  })
}

/** 收集树中全部节点 id（含 Container.children / left / right） */
export function collectIds(nodes: DslNode[]): Set<string> {
  const ids = new Set<string>()

  function walk(list: DslNode[]) {
    for (const n of list) {
      ids.add(n.id)
      if (n.children?.length) walk(n.children)
      const left = asNodeList(n.props?.left)
      const right = asNodeList(n.props?.right)
      if (left.length) walk(left)
      if (right.length) walk(right)
    }
  }

  walk(nodes)
  return ids
}

/** 按 id 深更 props（不可变） */
function updatePropsDeep(
  nodes: DslNode[],
  id: string,
  props: Record<string, unknown>,
): DslNode[] {
  let changed = false
  const mapped = nodes.map((n) => {
    if (n.id === id) {
      changed = true
      return { ...n, props: { ...n.props, ...props } }
    }

    let next = n
    if (n.children?.length) {
      const children = updatePropsDeep(n.children, id, props)
      if (children !== n.children) {
        changed = true
        next = { ...next, children }
      }
    }

    const left = asNodeList(n.props?.left)
    const right = asNodeList(n.props?.right)
    if (left.length) {
      const newLeft = updatePropsDeep(left, id, props)
      if (newLeft !== left) {
        changed = true
        next = { ...next, props: { ...next.props, left: newLeft } }
      }
    }
    if (right.length) {
      const newRight = updatePropsDeep(right, id, props)
      if (newRight !== right) {
        changed = true
        next = { ...next, props: { ...next.props, right: newRight } }
      }
    }
    return next
  })
  return changed ? mapped : nodes
}

/** 按 id 删除节点（含嵌套，不可变） */
function removeNodeDeep(nodes: DslNode[], id: string): DslNode[] {
  const filtered = nodes.filter((n) => n.id !== id)
  let changed = filtered.length !== nodes.length

  const mapped = filtered.map((n) => {
    let next = n
    if (n.children?.length) {
      const children = removeNodeDeep(n.children, id)
      if (children !== n.children) {
        changed = true
        next = { ...next, children }
      }
    }

    const left = asNodeList(n.props?.left)
    const right = asNodeList(n.props?.right)
    if (left.length) {
      const newLeft = removeNodeDeep(left, id)
      if (newLeft !== left) {
        changed = true
        next = { ...next, props: { ...next.props, left: newLeft } }
      }
    }
    if (right.length) {
      const newRight = removeNodeDeep(right, id)
      if (newRight !== right) {
        changed = true
        next = { ...next, props: { ...next.props, right: newRight } }
      }
    }
    return next
  })

  return changed ? mapped : nodes
}

export const usePageStore = defineStore('page', {
  state: () => ({
    pageDsl: null as PageDsl | null,
    selectedId: null as string | null,
    chatMessages: [] as ChatMessage[],
    history: [] as PageDsl[],
    redoStack: [] as PageDsl[],
    aiLoading: false,
    exportCode: '',
    exportDslHash: '',
    errorMessage: null as string | null,
  }),

  getters: {
    /** 当前选中节点（根级 + 嵌套） */
    selectedNode(state): DslNode | null {
      if (!state.pageDsl || !state.selectedId) return null
      const idsWanted = state.selectedId
      const stack = [...state.pageDsl.children]
      while (stack.length) {
        const n = stack.shift()!
        if (n.id === idsWanted) return n
        if (n.children?.length) stack.push(...n.children)
        stack.push(...asNodeList(n.props?.left), ...asNodeList(n.props?.right))
      }
      return null
    },
    canUndo(state): boolean {
      return state.history.length > 0
    },
    canRedo(state): boolean {
      return state.redoStack.length > 0
    },
  },

  actions: {
    /** 将当前 DSL 压入 history（上限 10），并清空 redo */
    pushHistory() {
      if (this.pageDsl == null) return
      this.history.push(deepClone(this.pageDsl))
      if (this.history.length > HISTORY_CAP) {
        this.history.shift()
      }
      this.redoStack = []
    },

    undo() {
      if (this.aiLoading) return
      if (this.history.length === 0) return
      const prev = this.history.pop()!
      if (this.pageDsl != null) {
        this.redoStack.push(deepClone(this.pageDsl))
      }
      this.pageDsl = prev
      this.clearSelectedIfMissing()
    },

    redo() {
      if (this.aiLoading) return
      if (this.redoStack.length === 0) return
      const next = this.redoStack.pop()!
      if (this.pageDsl != null) {
        this.history.push(deepClone(this.pageDsl))
        if (this.history.length > HISTORY_CAP) {
          this.history.shift()
        }
      }
      this.pageDsl = next
      this.clearSelectedIfMissing()
    },

    /** AI 成功写回：pushHistory → 替换 DSL → 清理无效选中 → 清错误 */
    setDslFromAi(dsl: PageDsl) {
      this.pushHistory()
      this.pageDsl = deepClone(dsl)
      this.clearSelectedIfMissing()
      this.errorMessage = null
    },

    /** 加载 fixture / 模板（与 AI 写回相同逻辑，无 AI 前置条件） */
    loadDsl(dsl: PageDsl) {
      this.pushHistory()
      this.pageDsl = deepClone(dsl)
      this.clearSelectedIfMissing()
      this.errorMessage = null
    },

    select(id: string | null) {
      this.selectedId = id
    },

    updateProps(id: string, props: Record<string, unknown>) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      const children = updatePropsDeep(this.pageDsl.children, id, props)
      this.pageDsl = { ...this.pageDsl, children }
    },

    /** 替换根 children */
    replaceChildren(children: DslNode[]) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      this.pageDsl = { ...this.pageDsl, children: deepClone(children) }
      this.clearSelectedIfMissing()
    },

    /** 通用树变更：mutator 接收当前 children 深拷贝，返回新 children */
    applyTreeMutation(mutator: (children: DslNode[]) => DslNode[]) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      const next = mutator(deepClone(this.pageDsl.children))
      this.pageDsl = { ...this.pageDsl, children: next }
      this.clearSelectedIfMissing()
    },

    insertFromPalette(type: string, targetId: string | null, position: InsertPosition) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      const node = createNode(type)
      const children = insertNodeDeep(this.pageDsl.children, targetId, position, node)
      this.pageDsl = { ...this.pageDsl, children }
      this.selectedId = node.id
    },

    /**
     * 从组件库投放到 Container 槽位（stack children 或 two-column 左/右列）。
     */
    insertIntoSlot(type: string, parentId: string, slot: DropSlot) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      const node = createNode(type)
      const children = appendIntoSlot(this.pageDsl.children, parentId, slot, node)
      this.pageDsl = { ...this.pageDsl, children }
      this.selectedId = node.id
    },

    reorderRoot(newChildren: DslNode[]) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      this.pageDsl = { ...this.pageDsl, children: deepClone(newChildren) }
    },

    /** 嵌套列表排序写回（parentId=null 表示根 children） */
    reorderSlot(parentId: string | null, slot: DropSlot, newList: DslNode[]) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      if (parentId == null) {
        this.pageDsl = { ...this.pageDsl, children: deepClone(newList) }
        return
      }
      const children = replaceSlotList(this.pageDsl.children, parentId, slot, deepClone(newList))
      this.pageDsl = { ...this.pageDsl, children }
    },

    removeNode(id: string) {
      if (this.aiLoading) return
      if (!this.pageDsl) return
      this.pushHistory()
      const children = removeNodeDeep(this.pageDsl.children, id)
      this.pageDsl = { ...this.pageDsl, children }
      if (this.selectedId === id) {
        this.selectedId = null
      } else {
        this.clearSelectedIfMissing()
      }
    },

    /** 仅改 style，不调 AI */
    setStyle(style: StyleType) {
      if (!this.pageDsl) return
      this.pageDsl = { ...this.pageDsl, style }
    },

    setPageType(pageType: PageType) {
      if (!this.pageDsl) return
      this.pageDsl = { ...this.pageDsl, pageType }
    },

    setAiLoading(v: boolean) {
      this.aiLoading = v
    },

    setError(msg: string | null) {
      this.errorMessage = msg
    },

    addChat(role: ChatRole, content: string) {
      this.chatMessages.push({ role, content })
    },

    /**
     * 按 DSL 哈希缓存导出：未变且已有 code 则跳过；否则调后端 exportVue。
     */
    async refreshExport() {
      if (!this.pageDsl) return
      const hash = hashDsl(this.pageDsl)
      if (hash === this.exportDslHash && this.exportCode) return
      try {
        const code = await exportVue(this.pageDsl)
        this.exportCode = code
        this.exportDslHash = hash
        this.errorMessage = null
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e)
        this.errorMessage = msg
      }
    },

    /** 若 selectedId 不在树中则清空 */
    clearSelectedIfMissing() {
      if (!this.selectedId) return
      if (!this.pageDsl) {
        this.selectedId = null
        return
      }
      const ids = collectIds(this.pageDsl.children)
      if (!ids.has(this.selectedId)) {
        this.selectedId = null
      }
    },
  },
})
