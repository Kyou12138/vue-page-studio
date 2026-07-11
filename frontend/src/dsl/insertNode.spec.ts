import { describe, expect, it } from 'vitest'
import type { DslNode } from '../types/dsl'
import { appendIntoSlot, insertNode, insertNodeDeep, replaceSlotList } from './insertNode'

function node(id: string, type = 'TextBlock', extra: Partial<DslNode> = {}): DslNode {
  return {
    id,
    type,
    props: { content: id },
    ...extra,
  }
}

describe('insertNode', () => {
  it('append + targetId null → 追加到末尾', () => {
    const list = [node('a'), node('b')]
    const n = node('c')
    const result = insertNode(list, null, 'append', n)
    expect(result.map((x) => x.id)).toEqual(['a', 'b', 'c'])
    // 不可变
    expect(list).toHaveLength(2)
    expect(result).not.toBe(list)
  })

  it('before → 插入到目标之前', () => {
    const list = [node('a'), node('b'), node('c')]
    const result = insertNode(list, 'b', 'before', node('x'))
    expect(result.map((x) => x.id)).toEqual(['a', 'x', 'b', 'c'])
  })

  it('after → 插入到目标之后', () => {
    const list = [node('a'), node('b'), node('c')]
    const result = insertNode(list, 'b', 'after', node('x'))
    expect(result.map((x) => x.id)).toEqual(['a', 'b', 'x', 'c'])
  })

  it('append 带 targetId 仍追加到末尾', () => {
    const list = [node('a'), node('b')]
    const result = insertNode(list, 'a', 'append', node('z'))
    expect(result.map((x) => x.id)).toEqual(['a', 'b', 'z'])
  })

  it('before/after 找不到 target 时不插入', () => {
    const list = [node('a')]
    const result = insertNode(list, 'missing', 'before', node('x'))
    expect(result.map((x) => x.id)).toEqual(['a'])
    expect(result).not.toBe(list)
  })

  it('空列表 append', () => {
    const result = insertNode([], null, 'append', node('only'))
    expect(result.map((x) => x.id)).toEqual(['only'])
  })
})

describe('insertNodeDeep', () => {
  it('根级 before 与 insertNode 一致', () => {
    const list = [node('a'), node('b')]
    const result = insertNodeDeep(list, 'a', 'after', node('x'))
    expect(result.map((x) => x.id)).toEqual(['a', 'x', 'b'])
  })

  it('在 Container.children 中插入', () => {
    const container = node('box', 'Container', {
      props: { layout: 'stack' },
      children: [node('inner-a'), node('inner-b')],
    })
    const root = [node('header'), container]
    const result = insertNodeDeep(root, 'inner-a', 'after', node('inner-x'))

    expect(result).toHaveLength(2)
    expect(result[0].id).toBe('header')
    const box = result[1]
    expect(box.children?.map((c) => c.id)).toEqual(['inner-a', 'inner-x', 'inner-b'])
    // 原树不可变
    expect(container.children?.map((c) => c.id)).toEqual(['inner-a', 'inner-b'])
  })

  it('在 two-column 的 props.left 中插入', () => {
    const container = node('cols', 'Container', {
      props: {
        layout: 'two-column',
        left: [node('L1'), node('L2')],
        right: [node('R1')],
      },
    })
    const root = [container]
    const result = insertNodeDeep(root, 'L1', 'before', node('Lx'))

    const left = result[0].props.left as DslNode[]
    const right = result[0].props.right as DslNode[]
    expect(left.map((c) => c.id)).toEqual(['Lx', 'L1', 'L2'])
    expect(right.map((c) => c.id)).toEqual(['R1'])
  })

  it('append 到根末尾', () => {
    const root = [node('a')]
    const result = insertNodeDeep(root, null, 'append', node('b'))
    expect(result.map((c) => c.id)).toEqual(['a', 'b'])
  })

  it('into → 追加到 Container.children', () => {
    const container = node('box', 'Container', {
      props: { layout: 'stack' },
      children: [node('inner-a')],
    })
    const root = [container]
    const result = insertNodeDeep(root, 'box', 'into', node('inner-b'))
    expect(result[0].children?.map((c) => c.id)).toEqual(['inner-a', 'inner-b'])
  })
})

describe('appendIntoSlot / replaceSlotList', () => {
  it('appendIntoSlot children', () => {
    const container = node('box', 'Container', {
      props: { layout: 'stack' },
      children: [node('a')],
    })
    const root = [container]
    const result = appendIntoSlot(root, 'box', 'children', node('b'))
    expect(result[0].children?.map((c) => c.id)).toEqual(['a', 'b'])
  })

  it('appendIntoSlot left column', () => {
    const container = node('cols', 'Container', {
      props: { layout: 'two-column', left: [], right: [node('R1')] },
    })
    const result = appendIntoSlot([container], 'cols', 'left', node('L1'))
    expect((result[0].props.left as DslNode[]).map((c) => c.id)).toEqual(['L1'])
    expect((result[0].props.right as DslNode[]).map((c) => c.id)).toEqual(['R1'])
  })

  it('replaceSlotList reorders nested children', () => {
    const container = node('box', 'Container', {
      props: { layout: 'stack' },
      children: [node('a'), node('b')],
    })
    const result = replaceSlotList([container], 'box', 'children', [node('b'), node('a')])
    expect(result[0].children?.map((c) => c.id)).toEqual(['b', 'a'])
  })
})
