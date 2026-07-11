<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { newId } from '../../dsl/defaultProps'
import { usePageStore } from '../../stores/pageStore'

const store = usePageStore()

const node = computed(() => store.selectedNode)
const draft = ref<Record<string, unknown>>({})

watch(
  node,
  (n) => {
    draft.value = n ? structuredClone(JSON.parse(JSON.stringify(n.props))) : {}
  },
  { immediate: true, deep: true },
)

const type = computed(() => node.value?.type ?? '')

const titleModel = computed({
  get: () => String(draft.value.title ?? ''),
  set: (v: string) => {
    draft.value = { ...draft.value, title: v }
  },
})

const contentModel = computed({
  get: () => String(draft.value.content ?? ''),
  set: (v: string) => {
    draft.value = { ...draft.value, content: v }
  },
})

const pageSizeModel = computed({
  get: () => Number(draft.value.pageSize ?? 10),
  set: (v: number | undefined) => {
    draft.value = { ...draft.value, pageSize: v ?? 10 }
  },
})

const layoutModel = computed({
  get: () => String(draft.value.layout ?? 'stack'),
  set: (v: string) => {
    draft.value = { ...draft.value, layout: v }
  },
})

const formColumnsModel = computed({
  get: () => Number(draft.value.columns ?? 1),
  set: (v: number) => {
    draft.value = { ...draft.value, columns: v }
  },
})

function apply() {
  if (!node.value || store.aiLoading) return
  store.updateProps(node.value.id, draft.value)
  ElMessage.success('属性已应用')
}

async function removeSelected() {
  if (!node.value || store.aiLoading) return
  try {
    await ElMessageBox.confirm(`删除节点 ${node.value.type}？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    store.removeNode(node.value.id)
  } catch {
    /* 取消 */
  }
}

type ColRow = { id: string; prop: string; label: string; width?: number }
type FieldRow = {
  id: string
  name: string
  label: string
  control: string
  options?: string[]
  required?: boolean
}
type ActionRow = { id: string; label: string; type?: string }
type StatRow = { id: string; label: string; value: string; unit?: string }

const columns = computed({
  get: () => (Array.isArray(draft.value.columns) ? (draft.value.columns as ColRow[]) : []),
  set: (v) => {
    draft.value = { ...draft.value, columns: v }
  },
})

const fields = computed({
  get: () => (Array.isArray(draft.value.fields) ? (draft.value.fields as FieldRow[]) : []),
  set: (v) => {
    draft.value = { ...draft.value, fields: v }
  },
})

const actions = computed({
  get: () => (Array.isArray(draft.value.actions) ? (draft.value.actions as ActionRow[]) : []),
  set: (v) => {
    draft.value = { ...draft.value, actions: v }
  },
})

const items = computed({
  get: () => (Array.isArray(draft.value.items) ? (draft.value.items as StatRow[]) : []),
  set: (v) => {
    draft.value = { ...draft.value, items: v }
  },
})

function addColumn() {
  columns.value = [...columns.value, { id: newId('col'), prop: 'field', label: '新列' }]
}

function removeColumn(i: number) {
  columns.value = columns.value.filter((_, idx) => idx !== i)
}

function addField() {
  fields.value = [
    ...fields.value,
    { id: newId('field'), name: 'field', label: '新字段', control: 'input' },
  ]
}

function removeField(i: number) {
  fields.value = fields.value.filter((_, idx) => idx !== i)
}

function addAction() {
  actions.value = [...actions.value, { id: newId('act'), label: '按钮', type: 'default' }]
}

function removeAction(i: number) {
  actions.value = actions.value.filter((_, idx) => idx !== i)
}

function addStat() {
  items.value = [...items.value, { id: newId('stat'), label: '指标', value: '0', unit: '' }]
}

function removeStat(i: number) {
  items.value = items.value.filter((_, idx) => idx !== i)
}

const jsonText = ref('')
const genericJson = ref('')
watch(
  () => [type.value, node.value?.id],
  () => {
    if (type.value === 'ActionBar' || type.value === 'StatCards') {
      const key = type.value === 'ActionBar' ? 'actions' : 'items'
      jsonText.value = JSON.stringify(draft.value[key] ?? [], null, 2)
    }
    genericJson.value = JSON.stringify(draft.value ?? {}, null, 2)
  },
  { immediate: true },
)

function applyGenericJson() {
  try {
    const parsed = JSON.parse(genericJson.value) as Record<string, unknown>
    if (parsed == null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('须为 JSON 对象')
    }
    draft.value = parsed
    apply()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : 'JSON 无效')
  }
}

function applyJsonList() {
  try {
    const parsed = JSON.parse(jsonText.value) as unknown
    if (!Array.isArray(parsed)) throw new Error('须为 JSON 数组')
    if (type.value === 'ActionBar') {
      draft.value = { ...draft.value, actions: parsed }
    } else {
      draft.value = { ...draft.value, items: parsed }
    }
    apply()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : 'JSON 无效')
  }
}

function onLayoutChange(v: string) {
  layoutModel.value = v
  apply()
}
</script>

<template>
  <div class="props-panel">
    <div v-if="!node" class="props-empty">
      <strong>属性面板</strong>
      在画布中点选一个区块，即可编辑字段、列与布局。
    </div>
    <template v-else>
      <div class="props-header">
        <span class="props-header-type">{{ node.type }}</span>
        <span class="props-header-id">{{ node.id }}</span>
      </div>
      <div class="props-body">
        <!-- PageHeader -->
        <template v-if="type === 'PageHeader'">
          <div class="props-field">
            <label class="props-field-label">标题</label>
            <el-input v-model="titleModel" size="small" @blur="apply" />
          </div>
          <div class="props-field">
            <label class="props-field-label">操作按钮</label>
            <div v-for="(a, i) in actions" :key="a.id" class="props-list-row">
              <el-input v-model="a.label" size="small" placeholder="label" />
              <el-select v-model="a.type" size="small" style="width: 100px">
                <el-option label="primary" value="primary" />
                <el-option label="default" value="default" />
              </el-select>
              <el-button size="small" text type="danger" @click="removeAction(i)">删</el-button>
            </div>
            <el-button size="small" @click="addAction">添加按钮</el-button>
          </div>
        </template>

        <!-- DataTable -->
        <template v-else-if="type === 'DataTable'">
          <div class="props-field">
            <label class="props-field-label">列</label>
            <div v-for="(c, i) in columns" :key="c.id" class="props-list-row">
              <el-input v-model="c.label" size="small" placeholder="label" />
              <el-input v-model="c.prop" size="small" placeholder="prop" />
              <el-button size="small" text type="danger" @click="removeColumn(i)">删</el-button>
            </div>
            <el-button size="small" @click="addColumn">添加列</el-button>
          </div>
        </template>

        <!-- SearchBar / FormSection -->
        <template v-else-if="type === 'SearchBar' || type === 'FormSection'">
          <div v-if="type === 'FormSection'" class="props-field">
            <label class="props-field-label">布局列数</label>
            <el-select v-model="formColumnsModel" size="small" style="width: 100%">
              <el-option :value="1" label="1 列" />
              <el-option :value="2" label="2 列" />
            </el-select>
          </div>
          <div class="props-field">
            <label class="props-field-label">字段</label>
            <div v-for="(f, i) in fields" :key="f.id" class="props-list-row" style="flex-wrap: wrap">
              <el-input v-model="f.label" size="small" placeholder="label" style="width: 40%" />
              <el-input v-model="f.name" size="small" placeholder="name" style="width: 35%" />
              <el-select v-model="f.control" size="small" style="width: 90px">
                <el-option label="input" value="input" />
                <el-option label="select" value="select" />
                <el-option v-if="type === 'FormSection'" label="textarea" value="textarea" />
              </el-select>
              <el-button size="small" text type="danger" @click="removeField(i)">删</el-button>
            </div>
            <el-button size="small" @click="addField">添加字段</el-button>
          </div>
        </template>

        <!-- TextBlock -->
        <template v-else-if="type === 'TextBlock'">
          <div class="props-field">
            <label class="props-field-label">内容</label>
            <el-input v-model="contentModel" type="textarea" :rows="4" size="small" @blur="apply" />
          </div>
        </template>

        <!-- Pagination -->
        <template v-else-if="type === 'Pagination'">
          <div class="props-field">
            <label class="props-field-label">每页条数</label>
            <el-input-number
              v-model="pageSizeModel"
              :min="1"
              :max="200"
              size="small"
              @change="apply"
            />
          </div>
        </template>

        <!-- Container -->
        <template v-else-if="type === 'Container'">
          <div class="props-field">
            <label class="props-field-label">布局</label>
            <el-select
              :model-value="layoutModel"
              size="small"
              style="width: 100%"
              @update:model-value="onLayoutChange"
            >
              <el-option label="堆叠 stack" value="stack" />
              <el-option label="双列 two-column" value="two-column" />
            </el-select>
          </div>
          <p style="font-size: 12px; color: #6b7280; margin: 0">
            双列内容在 props.left / props.right；MVP 画布以堆叠投放为主。
          </p>
        </template>

        <!-- ActionBar -->
        <template v-else-if="type === 'ActionBar'">
          <div class="props-field">
            <label class="props-field-label">按钮列表</label>
            <div v-for="(a, i) in actions" :key="a.id" class="props-list-row">
              <el-input v-model="a.label" size="small" placeholder="label" />
              <el-select v-model="a.type" size="small" style="width: 100px">
                <el-option label="primary" value="primary" />
                <el-option label="default" value="default" />
                <el-option label="danger" value="danger" />
              </el-select>
              <el-button size="small" text type="danger" @click="removeAction(i)">删</el-button>
            </div>
            <el-button size="small" @click="addAction">添加</el-button>
          </div>
          <div class="props-field">
            <label class="props-field-label">或 JSON 编辑</label>
            <el-input v-model="jsonText" type="textarea" :rows="5" size="small" />
            <el-button size="small" style="margin-top: 6px" @click="applyJsonList">应用 JSON</el-button>
          </div>
        </template>

        <!-- StatCards -->
        <template v-else-if="type === 'StatCards'">
          <div class="props-field">
            <label class="props-field-label">指标卡片</label>
            <div v-for="(it, i) in items" :key="it.id" class="props-list-row" style="flex-wrap: wrap">
              <el-input v-model="it.label" size="small" placeholder="label" style="width: 30%" />
              <el-input v-model="it.value" size="small" placeholder="value" style="width: 30%" />
              <el-input v-model="it.unit" size="small" placeholder="unit" style="width: 20%" />
              <el-button size="small" text type="danger" @click="removeStat(i)">删</el-button>
            </div>
            <el-button size="small" @click="addStat">添加</el-button>
          </div>
          <div class="props-field">
            <label class="props-field-label">或 JSON 编辑</label>
            <el-input v-model="jsonText" type="textarea" :rows="5" size="small" />
            <el-button size="small" style="margin-top: 6px" @click="applyJsonList">应用 JSON</el-button>
          </div>
        </template>

        <!-- 通用：新组件用 JSON 编辑 props（简单可靠） -->
        <template v-else>
          <div class="props-field">
            <label class="props-field-label">标题 / 主文案</label>
            <el-input
              v-if="'title' in draft"
              v-model="titleModel"
              size="small"
              placeholder="title"
              @blur="apply"
            />
            <el-input
              v-else-if="'content' in draft"
              v-model="contentModel"
              type="textarea"
              :rows="2"
              size="small"
              @blur="apply"
            />
          </div>
          <div class="props-field">
            <label class="props-field-label">完整 props（JSON）</label>
            <el-input v-model="genericJson" type="textarea" :rows="10" size="small" />
            <el-button size="small" style="margin-top: 6px" type="primary" plain @click="applyGenericJson">
              应用 JSON
            </el-button>
          </div>
        </template>
      </div>
      <div class="props-footer">
        <el-button size="small" type="danger" plain :disabled="store.aiLoading" @click="removeSelected">
          删除
        </el-button>
        <el-button size="small" type="primary" :disabled="store.aiLoading" @click="apply">
          应用
        </el-button>
      </div>
    </template>
  </div>
</template>
