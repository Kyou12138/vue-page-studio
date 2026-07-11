/** 页面类型 */
export type PageType = 'list' | 'form' | 'detail' | 'dashboard'

/** 导出/预览风格 */
export type StyleType = 'element-plus' | 'plain'

/** 白名单组件类型（≥20） */
export type ComponentType =
  | 'PageHeader'
  | 'Breadcrumb'
  | 'AlertBanner'
  | 'SearchBar'
  | 'DataTable'
  | 'Pagination'
  | 'FormSection'
  | 'DescriptionList'
  | 'ActionBar'
  | 'StatCards'
  | 'Tabs'
  | 'Steps'
  | 'Timeline'
  | 'TagGroup'
  | 'TreeNav'
  | 'EmptyState'
  | 'ResultBlock'
  | 'ImageBlock'
  | 'Divider'
  | 'TextBlock'
  | 'Container'

/** DSL 树节点。仅 Container 允许 children。 */
export interface DslNode {
  id: string
  type: string
  props: Record<string, unknown>
  children?: DslNode[]
}

/** 页面 DSL 根模型（version 1） */
export interface PageDsl {
  version: '1'
  pageType: PageType
  style: StyleType
  title: string
  children: DslNode[]
  meta?: Record<string, unknown>
}
