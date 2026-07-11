import type { ComponentType, DslNode } from '../types/dsl'

/** 白名单组件（21 种） */
export const COMPONENT_TYPES: ComponentType[] = [
  'PageHeader',
  'Breadcrumb',
  'AlertBanner',
  'SearchBar',
  'DataTable',
  'Pagination',
  'FormSection',
  'DescriptionList',
  'ActionBar',
  'StatCards',
  'Tabs',
  'Steps',
  'Timeline',
  'TagGroup',
  'TreeNav',
  'EmptyState',
  'ResultBlock',
  'ImageBlock',
  'Divider',
  'TextBlock',
  'Container',
]

/**
 * 各组件类型的默认 props，与后端 DefaultProps 对齐。
 */
export function defaultPropsFor(type: string): Record<string, unknown> {
  switch (type) {
    case 'PageHeader':
      return { title: '页面标题', actions: [] }
    case 'Breadcrumb':
      return {
        items: [
          { id: 'bc1', label: '首页' },
          { id: 'bc2', label: '当前页' },
        ],
      }
    case 'AlertBanner':
      return {
        title: '提示信息',
        description: '这里是补充说明',
        type: 'info',
        closable: false,
      }
    case 'SearchBar':
      return {
        fields: [
          { id: 'f1', name: 'keyword', label: '关键词', control: 'input' },
        ],
      }
    case 'DataTable':
      return {
        columns: [
          { id: 'c1', prop: 'name', label: '名称' },
          { id: 'c2', prop: 'status', label: '状态' },
        ],
        rowActions: [],
        dataSource: 'mock',
      }
    case 'Pagination':
      return { pageSize: 10 }
    case 'FormSection':
      return {
        fields: [
          { id: 'f1', name: 'name', label: '名称', control: 'input' },
        ],
        columns: 1,
      }
    case 'DescriptionList':
      return {
        column: 2,
        items: [
          { id: 'd1', label: '状态', value: '已启用' },
          { id: 'd2', label: '创建时间', value: '2026-01-01' },
        ],
      }
    case 'ActionBar':
      return {
        actions: [
          { id: 'a1', label: '提交', type: 'primary' },
          { id: 'a2', label: '取消', type: 'default' },
        ],
      }
    case 'StatCards':
      return {
        items: [
          { id: 's1', label: '今日访问', value: '1,280', unit: '' },
          { id: 's2', label: '转化率', value: '12.5', unit: '%' },
        ],
      }
    case 'Tabs':
      return {
        active: 'tab1',
        items: [
          { id: 't1', name: 'tab1', label: '概览' },
          { id: 't2', name: 'tab2', label: '明细' },
        ],
      }
    case 'Steps':
      return {
        active: 1,
        items: [
          { id: 'st1', title: '填写信息', description: '基本资料' },
          { id: 'st2', title: '确认', description: '核对内容' },
          { id: 'st3', title: '完成', description: '提交成功' },
        ],
      }
    case 'Timeline':
      return {
        items: [
          { id: 'tl1', content: '创建订单', timestamp: '10:00' },
          { id: 'tl2', content: '支付完成', timestamp: '10:15' },
        ],
      }
    case 'TagGroup':
      return {
        tags: [
          { id: 'tg1', label: '标签A', type: '' },
          { id: 'tg2', label: '标签B', type: 'success' },
        ],
      }
    case 'TreeNav':
      return {
        data: [
          { id: 'n1', label: '目录一', children: [{ id: 'n1-1', label: '子项 A' }] },
          { id: 'n2', label: '目录二' },
        ],
      }
    case 'EmptyState':
      return { title: '暂无数据', description: '可以新建一条记录试试' }
    case 'ResultBlock':
      return {
        status: 'success',
        title: '操作成功',
        subTitle: '页面将在稍后跳转',
      }
    case 'ImageBlock':
      return {
        src: 'https://via.placeholder.com/640x200?text=Image',
        alt: '示意图片',
        fit: 'cover',
      }
    case 'Divider':
      return { content: '', contentPosition: 'center' }
    case 'TextBlock':
      return { content: '说明文本', variant: 'body' }
    case 'Container':
      return { layout: 'stack' }
    default:
      return {}
  }
}

/** 生成 `{type}_{8chars}` 形式的节点 id */
export function newId(type: string): string {
  return `${type}_${crypto.randomUUID().slice(0, 8)}`
}

/** 创建带默认 props 的新节点；Container 额外初始化 children */
export function createNode(type: string): DslNode {
  const node: DslNode = {
    id: newId(type),
    type,
    props: { ...defaultPropsFor(type) },
  }
  if (type === 'Container') {
    node.children = []
  }
  return node
}
