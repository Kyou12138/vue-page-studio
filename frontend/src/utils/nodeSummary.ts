import type { DslNode } from '../types/dsl'

/** 各类型低保真卡片的色条颜色 */
export const TYPE_COLORS: Record<string, string> = {
  PageHeader: '#f59e0b',
  Breadcrumb: '#a8a29e',
  AlertBanner: '#fbbf24',
  SearchBar: '#34d399',
  DataTable: '#fb923c',
  Pagination: '#78716c',
  FormSection: '#f87171',
  DescriptionList: '#c084fc',
  ActionBar: '#a78bfa',
  StatCards: '#2dd4bf',
  Tabs: '#60a5fa',
  Steps: '#38bdf8',
  Timeline: '#818cf8',
  TagGroup: '#e879f9',
  TreeNav: '#4ade80',
  EmptyState: '#94a3b8',
  ResultBlock: '#22c55e',
  ImageBlock: '#f472b6',
  Divider: '#57534e',
  TextBlock: '#a8a29e',
  Container: '#14b8a6',
}

/** 组件库展示名 */
export const TYPE_LABELS: Record<string, string> = {
  PageHeader: '页头',
  Breadcrumb: '面包屑',
  AlertBanner: '提示条',
  SearchBar: '搜索栏',
  DataTable: '数据表',
  Pagination: '分页',
  FormSection: '表单区',
  DescriptionList: '描述列表',
  ActionBar: '操作栏',
  StatCards: '统计卡',
  Tabs: '标签页',
  Steps: '步骤条',
  Timeline: '时间线',
  TagGroup: '标签组',
  TreeNav: '树导航',
  EmptyState: '空状态',
  ResultBlock: '结果页',
  ImageBlock: '图片',
  Divider: '分割线',
  TextBlock: '文本块',
  Container: '容器',
}

function asArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

/** 根据节点 props 生成摘要文案 */
export function summarizeNode(node: DslNode): string {
  const p = node.props ?? {}
  switch (node.type) {
    case 'PageHeader':
      return String(p.title ?? '无标题')
    case 'Breadcrumb':
      return `${asArray(p.items).length} 级`
    case 'AlertBanner':
      return `${String(p.type ?? 'info')} · ${String(p.title ?? '')}`
    case 'SearchBar':
      return `${asArray(p.fields).length} 个筛选项`
    case 'DataTable':
      return `${asArray(p.columns).length} 列`
    case 'Pagination':
      return `每页 ${String(p.pageSize ?? 10)} 条`
    case 'FormSection':
      return `${asArray(p.fields).length} 个字段`
    case 'DescriptionList':
      return `${asArray(p.items).length} 项 · ${String(p.column ?? 2)} 列`
    case 'ActionBar':
      return `${asArray(p.actions).length} 个按钮`
    case 'StatCards':
      return `${asArray(p.items).length} 张卡片`
    case 'Tabs':
      return `${asArray(p.items).length} 个标签`
    case 'Steps':
      return `${asArray(p.items).length} 步 · 当前 ${String(p.active ?? 0)}`
    case 'Timeline':
      return `${asArray(p.items).length} 条动态`
    case 'TagGroup':
      return `${asArray(p.tags).length} 个标签`
    case 'TreeNav':
      return `${asArray(p.data).length} 个根节点`
    case 'EmptyState':
      return String(p.title ?? '暂无数据')
    case 'ResultBlock':
      return `${String(p.status ?? '')} · ${String(p.title ?? '')}`
    case 'ImageBlock':
      return String(p.alt || p.src || '图片')
    case 'Divider':
      return p.content ? String(p.content) : '分割线'
    case 'TextBlock': {
      const c = String(p.content ?? '')
      return c ? (c.length > 40 ? `${c.slice(0, 40)}…` : c) : '(空文本)'
    }
    case 'Container': {
      const layout = String(p.layout ?? 'stack')
      if (layout === 'two-column') {
        return `双列 · 左 ${asArray(p.left).length} / 右 ${asArray(p.right).length}`
      }
      return `堆叠 · ${node.children?.length ?? 0} 子节点`
    }
    default:
      return node.id
  }
}

export function colorForType(type: string): string {
  return TYPE_COLORS[type] ?? '#78716c'
}
