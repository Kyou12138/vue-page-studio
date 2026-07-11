<script setup lang="ts">
import type { DslNode } from '../../types/dsl'

export type PreviewCol = { id?: string; prop: string; label: string }
export type PreviewField = {
  id?: string
  name: string
  label: string
  control?: string
  options?: string[]
}
export type PreviewAction = { id?: string; label: string; type?: string }
export type PreviewStat = { id?: string; label: string; value: string; unit?: string }

const props = defineProps<{
  node: DslNode
  plain?: boolean
}>()

function asCols(p: Record<string, unknown>): PreviewCol[] {
  return Array.isArray(p.columns) ? (p.columns as PreviewCol[]) : []
}

function asFields(p: Record<string, unknown>): PreviewField[] {
  return Array.isArray(p.fields) ? (p.fields as PreviewField[]) : []
}

function asActions(p: Record<string, unknown>): PreviewAction[] {
  return Array.isArray(p.actions) ? (p.actions as PreviewAction[]) : []
}

function asItems(p: Record<string, unknown>): PreviewStat[] {
  return Array.isArray(p.items) ? (p.items as PreviewStat[]) : []
}

function asNodes(value: unknown): DslNode[] {
  if (!Array.isArray(value)) return []
  return value.filter(
    (item): item is DslNode =>
      item != null &&
      typeof item === 'object' &&
      typeof (item as DslNode).id === 'string' &&
      typeof (item as DslNode).type === 'string',
  )
}

function mockRows(columns: PreviewCol[]): Record<string, string>[] {
  return [1, 2, 3].map((i) => {
    const row: Record<string, string> = {}
    for (const c of columns) {
      row[c.prop] = `${c.label || c.prop}-${i}`
    }
    return row
  })
}

function actionBtnType(t?: string): 'primary' | 'danger' | 'default' {
  if (t === 'primary') return 'primary'
  if (t === 'danger') return 'danger'
  return 'default'
}

function asLabelItems(v: unknown): { id?: string; label: string }[] {
  return Array.isArray(v) ? (v as { id?: string; label: string }[]) : []
}
function asDescItems(v: unknown): { id?: string; label: string; value: string }[] {
  return Array.isArray(v) ? (v as { id?: string; label: string; value: string }[]) : []
}
function asTabItems(v: unknown): { id?: string; name?: string; label: string }[] {
  return Array.isArray(v) ? (v as { id?: string; name?: string; label: string }[]) : []
}
function asStepItems(v: unknown): { id?: string; title: string; description?: string }[] {
  return Array.isArray(v) ? (v as { id?: string; title: string; description?: string }[]) : []
}
function asTimelineItems(v: unknown): { id?: string; content: string; timestamp?: string }[] {
  return Array.isArray(v) ? (v as { id?: string; content: string; timestamp?: string }[]) : []
}
function asTagItems(v: unknown): { id?: string; label: string; type?: string }[] {
  return Array.isArray(v) ? (v as { id?: string; label: string; type?: string }[]) : []
}
function asTreeData(v: unknown): { id?: string; label: string; children?: unknown[] }[] {
  return Array.isArray(v) ? (v as { id?: string; label: string; children?: unknown[] }[]) : []
}

const p = () => props.node.props ?? {}
</script>

<template>
  <!-- PageHeader -->
  <div v-if="node.type === 'PageHeader'" class="preview-block">
    <div class="preview-title">
      <span>{{ String(p().title ?? '') }}</span>
      <span style="display: flex; gap: 8px">
        <template v-if="plain">
          <button v-for="a in asActions(p())" :key="a.id" style="margin-left: 4px">
            {{ a.label }}
          </button>
        </template>
        <template v-else>
          <el-button
            v-for="a in asActions(p())"
            :key="a.id"
            size="small"
            :type="actionBtnType(a.type)"
          >
            {{ a.label }}
          </el-button>
        </template>
      </span>
    </div>
  </div>

  <!-- SearchBar -->
  <div v-else-if="node.type === 'SearchBar'" class="preview-block">
    <template v-if="plain">
      <div v-for="f in asFields(p())" :key="f.id" style="margin-bottom: 8px">
        <label style="margin-right: 8px">{{ f.label }}</label>
        <input :placeholder="f.name" />
      </div>
    </template>
    <el-form v-else inline>
      <el-form-item v-for="f in asFields(p())" :key="f.id" :label="f.label">
        <el-select
          v-if="f.control === 'select'"
          model-value=""
          :placeholder="f.label"
          style="width: 160px"
        >
          <el-option v-for="o in f.options ?? []" :key="o" :label="o" :value="o" />
        </el-select>
        <el-input v-else model-value="" :placeholder="f.label" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary">查询</el-button>
        <el-button>重置</el-button>
      </el-form-item>
    </el-form>
  </div>

  <!-- FormSection -->
  <div v-else-if="node.type === 'FormSection'" class="preview-block">
    <template v-if="plain">
      <div v-for="f in asFields(p())" :key="f.id" style="margin-bottom: 8px">
        <label style="margin-right: 8px">{{ f.label }}</label>
        <input :placeholder="f.name" />
      </div>
    </template>
    <el-form v-else label-width="80px">
      <el-form-item v-for="f in asFields(p())" :key="f.id" :label="f.label">
        <el-select
          v-if="f.control === 'select'"
          model-value=""
          :placeholder="f.label"
          style="width: 240px"
        >
          <el-option v-for="o in f.options ?? []" :key="o" :label="o" :value="o" />
        </el-select>
        <el-input
          v-else-if="f.control === 'textarea'"
          type="textarea"
          model-value=""
          :placeholder="f.label"
        />
        <el-input v-else model-value="" :placeholder="f.label" style="width: 240px" />
      </el-form-item>
    </el-form>
  </div>

  <!-- DataTable -->
  <div v-else-if="node.type === 'DataTable'" class="preview-block">
    <template v-if="plain">
      <table border="1" cellpadding="6" style="width: 100%; border-collapse: collapse">
        <thead>
          <tr>
            <th v-for="c in asCols(p())" :key="c.prop">{{ c.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in mockRows(asCols(p()))" :key="i">
            <td v-for="c in asCols(p())" :key="c.prop">{{ row[c.prop] }}</td>
          </tr>
        </tbody>
      </table>
    </template>
    <el-table v-else :data="mockRows(asCols(p()))" border size="small" style="width: 100%">
      <el-table-column
        v-for="c in asCols(p())"
        :key="c.id || c.prop"
        :prop="c.prop"
        :label="c.label"
      />
      <el-table-column
        v-if="Array.isArray(p().rowActions) && (p().rowActions as PreviewAction[]).length"
        label="操作"
        width="160"
      >
        <template #default>
          <el-button
            v-for="a in (p().rowActions as PreviewAction[])"
            :key="a.id"
            link
            type="primary"
            size="small"
          >
            {{ a.label }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <!-- Pagination -->
  <div
    v-else-if="node.type === 'Pagination'"
    class="preview-block"
    :style="plain ? 'text-align:right' : 'display:flex;justify-content:flex-end'"
  >
    <span v-if="plain">分页 · 每页 {{ Number(p().pageSize ?? 10) }}</span>
    <el-pagination
      v-else
      background
      small
      layout="total, sizes, prev, pager, next"
      :total="100"
      :page-size="Number(p().pageSize ?? 10)"
    />
  </div>

  <!-- ActionBar -->
  <div
    v-else-if="node.type === 'ActionBar'"
    class="preview-block"
    style="display: flex; gap: 8px"
  >
    <template v-if="plain">
      <button v-for="a in asActions(p())" :key="a.id">{{ a.label }}</button>
    </template>
    <template v-else>
      <el-button
        v-for="a in asActions(p())"
        :key="a.id"
        size="small"
        :type="actionBtnType(a.type)"
      >
        {{ a.label }}
      </el-button>
    </template>
  </div>

  <!-- StatCards -->
  <div
    v-else-if="node.type === 'StatCards'"
    class="preview-block"
    style="display: flex; gap: 12px; flex-wrap: wrap"
  >
    <div
      v-for="it in asItems(p())"
      :key="it.id"
      style="
        min-width: 120px;
        padding: 12px 16px;
        border: 1px solid #e4e7ed;
        border-radius: 8px;
        background: #fff;
      "
    >
      <div style="font-size: 12px; color: #909399">{{ it.label }}</div>
      <div style="font-size: 22px; font-weight: 600; margin-top: 4px">
        {{ it.value }}{{ it.unit ? ' ' + it.unit : '' }}
      </div>
    </div>
  </div>

  <!-- TextBlock -->
  <div
    v-else-if="node.type === 'TextBlock'"
    class="preview-block"
    :style="p().variant === 'hint' ? 'color:#909399;font-size:13px' : ''"
  >
    {{ String(p().content ?? '') }}
  </div>

  <!-- Breadcrumb -->
  <div v-else-if="node.type === 'Breadcrumb'" class="preview-block" style="font-size: 13px; color: #606266">
    <template v-for="(it, i) in asLabelItems(p().items)" :key="it.id || i">
      <span v-if="i > 0"> / </span>
      <span>{{ it.label }}</span>
    </template>
  </div>

  <!-- AlertBanner -->
  <el-alert
    v-else-if="node.type === 'AlertBanner' && !plain"
    class="preview-block"
    :title="String(p().title ?? '')"
    :type="(String(p().type ?? 'info') as any)"
    :description="String(p().description ?? '')"
    :closable="Boolean(p().closable)"
    show-icon
  />
  <div
    v-else-if="node.type === 'AlertBanner'"
    class="preview-block"
    style="padding: 10px 12px; border-radius: 6px; background: #fdf6ec; color: #e6a23c"
  >
    <strong>{{ String(p().title ?? '') }}</strong>
    {{ String(p().description ?? '') }}
  </div>

  <!-- DescriptionList -->
  <el-descriptions
    v-else-if="node.type === 'DescriptionList' && !plain"
    class="preview-block"
    :column="Number(p().column ?? 2)"
    border
  >
    <el-descriptions-item
      v-for="(it, i) in asDescItems(p().items)"
      :key="it.id || i"
      :label="it.label"
    >
      {{ it.value }}
    </el-descriptions-item>
  </el-descriptions>
  <dl v-else-if="node.type === 'DescriptionList'" class="preview-block">
    <div v-for="(it, i) in asDescItems(p().items)" :key="it.id || i">
      <dt style="font-weight: 600">{{ it.label }}</dt>
      <dd style="margin: 0 0 8px">{{ it.value }}</dd>
    </div>
  </dl>

  <!-- Tabs -->
  <el-tabs v-else-if="node.type === 'Tabs' && !plain" class="preview-block" :model-value="String(p().active ?? '')">
    <el-tab-pane
      v-for="(it, i) in asTabItems(p().items)"
      :key="it.id || i"
      :label="it.label"
      :name="it.name || it.id || String(i)"
    >
      {{ it.label }} 内容
    </el-tab-pane>
  </el-tabs>
  <div v-else-if="node.type === 'Tabs'" class="preview-block">
    <span v-for="(it, i) in asTabItems(p().items)" :key="i" style="margin-right: 12px">{{ it.label }}</span>
  </div>

  <!-- Steps -->
  <el-steps
    v-else-if="node.type === 'Steps' && !plain"
    class="preview-block"
    :active="Number(p().active ?? 0)"
    finish-status="success"
  >
    <el-step
      v-for="(it, i) in asStepItems(p().items)"
      :key="it.id || i"
      :title="it.title"
      :description="it.description"
    />
  </el-steps>
  <div v-else-if="node.type === 'Steps'" class="preview-block">
    <div v-for="(it, i) in asStepItems(p().items)" :key="i">{{ i + 1 }}. {{ it.title }}</div>
  </div>

  <!-- Timeline -->
  <el-timeline v-else-if="node.type === 'Timeline' && !plain" class="preview-block">
    <el-timeline-item
      v-for="(it, i) in asTimelineItems(p().items)"
      :key="it.id || i"
      :timestamp="it.timestamp"
    >
      {{ it.content }}
    </el-timeline-item>
  </el-timeline>
  <ul v-else-if="node.type === 'Timeline'" class="preview-block">
    <li v-for="(it, i) in asTimelineItems(p().items)" :key="i">
      {{ it.timestamp }} — {{ it.content }}
    </li>
  </ul>

  <!-- TagGroup -->
  <div v-else-if="node.type === 'TagGroup'" class="preview-block">
    <el-tag
      v-for="(t, i) in asTagItems(p().tags)"
      :key="t.id || i"
      :type="(t.type as any) || undefined"
      style="margin-right: 8px"
    >
      {{ t.label }}
    </el-tag>
  </div>

  <!-- TreeNav -->
  <el-tree
    v-else-if="node.type === 'TreeNav' && !plain"
    class="preview-block"
    :data="asTreeData(p().data)"
    :props="{ label: 'label', children: 'children' }"
    default-expand-all
  />
  <ul v-else-if="node.type === 'TreeNav'" class="preview-block">
    <li v-for="(n, i) in asTreeData(p().data)" :key="i">{{ n.label }}</li>
  </ul>

  <!-- EmptyState -->
  <el-empty
    v-else-if="node.type === 'EmptyState' && !plain"
    class="preview-block"
    :description="String(p().title ?? '暂无数据')"
  >
    <p style="color: #909399">{{ String(p().description ?? '') }}</p>
  </el-empty>
  <div v-else-if="node.type === 'EmptyState'" class="preview-block" style="text-align: center; color: #909399">
    <h3>{{ String(p().title ?? '') }}</h3>
    <p>{{ String(p().description ?? '') }}</p>
  </div>

  <!-- ResultBlock -->
  <el-result
    v-else-if="node.type === 'ResultBlock' && !plain"
    class="preview-block"
    :icon="(String(p().status ?? 'success') as any)"
    :title="String(p().title ?? '')"
    :sub-title="String(p().subTitle ?? '')"
  />
  <div v-else-if="node.type === 'ResultBlock'" class="preview-block" style="text-align: center">
    <h2>{{ String(p().title ?? '') }}</h2>
    <p>{{ String(p().subTitle ?? '') }}</p>
  </div>

  <!-- ImageBlock -->
  <div v-else-if="node.type === 'ImageBlock'" class="preview-block">
    <img
      :src="String(p().src ?? '')"
      :alt="String(p().alt ?? '')"
      style="max-width: 100%; max-height: 200px; object-fit: cover; border-radius: 8px"
    />
  </div>

  <!-- Divider -->
  <el-divider v-else-if="node.type === 'Divider' && !plain" :content-position="(String(p().contentPosition ?? 'center') as any)">
    {{ String(p().content ?? '') }}
  </el-divider>
  <hr v-else-if="node.type === 'Divider'" class="preview-block" />

  <!-- Container -->
  <div
    v-else-if="node.type === 'Container' && String(p().layout ?? 'stack') === 'two-column'"
    class="preview-block"
    style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px"
  >
    <div>
      <PreviewNode
        v-for="c in asNodes(p().left)"
        :key="c.id"
        :node="c"
        :plain="plain"
      />
    </div>
    <div>
      <PreviewNode
        v-for="c in asNodes(p().right)"
        :key="c.id"
        :node="c"
        :plain="plain"
      />
    </div>
  </div>
  <div v-else-if="node.type === 'Container'" class="preview-block">
    <PreviewNode
      v-for="c in node.children ?? []"
      :key="c.id"
      :node="c"
      :plain="plain"
    />
  </div>

  <div v-else class="preview-block" style="color: #909399">未知类型: {{ node.type }}</div>
</template>
