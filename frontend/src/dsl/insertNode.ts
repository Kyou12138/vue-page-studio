import type { DslNode } from '../types/dsl'

export type InsertPosition = 'before' | 'after' | 'append' | 'into'

/** Container 投放槽：stack 用 children；two-column 用 left / right */
export type DropSlot = 'children' | 'left' | 'right'

/**
 * 在同一级 children 列表中插入节点（不可变：返回新数组）。
 * - targetId null + append → 追加到末尾
 * - before / after → 相对本列表中的 targetId 插入
 * - append 且带 targetId → 仍追加到本列表末尾
 * - before/after 找不到 target → 原样返回（浅拷贝）
 */
export function insertNode(
  children: DslNode[],
  targetId: string | null,
  position: InsertPosition,
  node: DslNode,
): DslNode[] {
  if (position === 'append' || targetId == null) {
    return [...children, node]
  }

  const idx = children.findIndex((c) => c.id === targetId)
  if (idx < 0) {
    return [...children]
  }

  const next = [...children]
  if (position === 'before') {
    next.splice(idx, 0, node)
  } else {
    // after
    next.splice(idx + 1, 0, node)
  }
  return next
}

/**
 * 将 node 追加到指定 Container 的槽位（children / left / right）。
 * 找不到 parentId 时返回原 children 浅拷贝。
 */
export function appendIntoSlot(
  children: DslNode[],
  parentId: string,
  slot: DropSlot,
  node: DslNode,
): DslNode[] {
  let changed = false
  const mapped = children.map((child) => {
    if (changed) return child
    if (child.id === parentId) {
      changed = true
      return appendSlotOnNode(child, slot, node)
    }
    if (child.children?.length) {
      const nextChildren = appendIntoSlot(child.children, parentId, slot, node)
      if (nextChildren !== child.children) {
        changed = true
        return { ...child, children: nextChildren }
      }
    }
    for (const key of ['left', 'right'] as const) {
      const list = asNodeList(child.props?.[key])
      if (!list.length) continue
      const nextList = appendIntoSlot(list, parentId, slot, node)
      if (nextList !== list) {
        changed = true
        return {
          ...child,
          props: { ...child.props, [key]: nextList },
        }
      }
    }
    return child
  })
  return changed ? mapped : [...children]
}

/**
 * 替换指定 Container 槽位的整表（用于嵌套拖拽排序写回）。
 */
export function replaceSlotList(
  children: DslNode[],
  parentId: string,
  slot: DropSlot,
  newList: DslNode[],
): DslNode[] {
  let changed = false
  const mapped = children.map((child) => {
    if (changed) return child
    if (child.id === parentId) {
      changed = true
      return setSlotOnNode(child, slot, newList)
    }
    if (child.children?.length) {
      const nextChildren = replaceSlotList(child.children, parentId, slot, newList)
      if (nextChildren !== child.children) {
        changed = true
        return { ...child, children: nextChildren }
      }
    }
    for (const key of ['left', 'right'] as const) {
      const list = asNodeList(child.props?.[key])
      if (list.length === 0) continue
      const nextList = replaceSlotList(list, parentId, slot, newList)
      if (nextList !== list) {
        changed = true
        return {
          ...child,
          props: { ...child.props, [key]: nextList },
        }
      }
    }
    return child
  })
  return changed ? mapped : [...children]
}

function appendSlotOnNode(host: DslNode, slot: DropSlot, node: DslNode): DslNode {
  if (slot === 'children') {
    const list = host.children ? [...host.children] : []
    list.push(node)
    return { ...host, children: list }
  }
  const list = asNodeList(host.props?.[slot])
  return {
    ...host,
    props: { ...host.props, [slot]: [...list, node] },
  }
}

function setSlotOnNode(host: DslNode, slot: DropSlot, newList: DslNode[]): DslNode {
  if (slot === 'children') {
    return { ...host, children: [...newList] }
  }
  return {
    ...host,
    props: { ...host.props, [slot]: [...newList] },
  }
}

/**
 * 在整棵树中查找 targetId 并插入（根 children、Container.children、props.left/right）。
 * - targetId 为 null 且 position 为 append 时，追加到根列表末尾。
 * - position === 'into'：target 须为 Container，追加到其 children（stack 槽）。
 */
export function insertNodeDeep(
  children: DslNode[],
  targetId: string | null,
  position: InsertPosition,
  node: DslNode,
): DslNode[] {
  // into：投入 Container 内部
  if (position === 'into' && targetId != null) {
    return appendIntoSlot(children, targetId, 'children', node)
  }

  // append 或无目标：挂到根列表末尾
  if (position === 'append' || targetId == null) {
    return insertNode(children, null, 'append', node)
  }

  // 先看本层
  const idx = children.findIndex((c) => c.id === targetId)
  if (idx >= 0) {
    return insertNode(children, targetId, position, node)
  }

  // 递归进入子树，命中则返回新树
  let changed = false
  const mapped = children.map((child) => {
    if (changed) return child
    const updated = tryInsertIntoNode(child, targetId, position, node)
    if (updated !== child) {
      changed = true
      return updated
    }
    return child
  })

  return changed ? mapped : [...children]
}

function tryInsertIntoNode(
  host: DslNode,
  targetId: string,
  position: InsertPosition,
  node: DslNode,
): DslNode {
  // Container stack: children
  if (host.children) {
    const inList = host.children.some((c) => c.id === targetId)
    if (inList) {
      return {
        ...host,
        children: insertNode(host.children, targetId, position, node),
      }
    }
    // 更深一层
    let childChanged = false
    const newChildren = host.children.map((c) => {
      if (childChanged) return c
      const u = tryInsertIntoNode(c, targetId, position, node)
      if (u !== c) {
        childChanged = true
        return u
      }
      return c
    })
    if (childChanged) {
      return { ...host, children: newChildren }
    }
  }

  // two-column: props.left / props.right
  const left = asNodeList(host.props?.left)
  const right = asNodeList(host.props?.right)

  if (left.some((c) => c.id === targetId)) {
    return {
      ...host,
      props: {
        ...host.props,
        left: insertNode(left, targetId, position, node),
      },
    }
  }
  if (right.some((c) => c.id === targetId)) {
    return {
      ...host,
      props: {
        ...host.props,
        right: insertNode(right, targetId, position, node),
      },
    }
  }

  // 列内再嵌套（MVP 一般不嵌套，但保持对称）
  for (const key of ['left', 'right'] as const) {
    const list = key === 'left' ? left : right
    if (list.length === 0) continue
    let colChanged = false
    const newList = list.map((c) => {
      if (colChanged) return c
      const u = tryInsertIntoNode(c, targetId, position, node)
      if (u !== c) {
        colChanged = true
        return u
      }
      return c
    })
    if (colChanged) {
      return {
        ...host,
        props: { ...host.props, [key]: newList },
      }
    }
  }

  return host
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
