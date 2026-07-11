<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { generateDsl, modifyDsl } from '../../api/client'
import listUserFixture from '../../fixtures/list-user.json'
import { usePageStore } from '../../stores/pageStore'
import type { PageDsl, PageType, StyleType } from '../../types/dsl'
import { normalizeAiError } from '../../utils/aiError'

const store = usePageStore()

const pageType = ref<PageType>('list')
const style = ref<StyleType>('element-plus')
const description = ref('')
const instruction = ref('')
const chatListRef = ref<HTMLElement | null>(null)

const QUICK_PROMPTS = [
  { label: '用户列表', text: '做一个用户管理列表页，含搜索、表格和分页' },
  { label: '登录表单', text: '做一个登录表单页，含用户名、密码和提交按钮' },
  { label: '订单详情', text: '做一个订单详情页，展示订单号、状态、金额与操作按钮' },
]

const ROLE_LABEL: Record<string, string> = {
  user: '你',
  assistant: 'AI',
  system: '系统',
}

async function scrollChatToBottom() {
  await nextTick()
  const el = chatListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(
  () => store.chatMessages.length,
  () => {
    void scrollChatToBottom()
  },
)

function applyQuick(text: string) {
  if (store.aiLoading) return
  description.value = text
}

function loadExample() {
  if (store.aiLoading) return
  store.loadDsl(listUserFixture as PageDsl)
  store.addChat('system', '已加载示例：用户管理列表')
  ElMessage.success('已加载示例页面')
}

function handleAiError(e: unknown) {
  const info = normalizeAiError(e)
  store.setError(info.storeMessage)
  if (info.configTip) {
    ElMessage.warning(info.configTip)
  } else {
    ElMessage.error(info.message)
  }
}

async function onGenerate() {
  if (store.aiLoading) return
  const desc = description.value.trim()
  if (!desc) {
    ElMessage.warning('请填写页面描述')
    return
  }

  store.setAiLoading(true)
  store.setError(null)
  try {
    const dsl = await generateDsl({
      description: desc,
      pageType: pageType.value,
      style: style.value,
    })
    store.setDslFromAi(dsl)
    store.addChat('user', desc)
    store.addChat('assistant', `已生成页面：${dsl.title || '未命名'}`)
    await store.refreshExport()
    ElMessage.success('AI 生成成功')
  } catch (e) {
    handleAiError(e)
  } finally {
    store.setAiLoading(false)
  }
}

async function onModify() {
  if (store.aiLoading || !store.pageDsl) return
  const instr = instruction.value.trim()
  if (!instr) {
    ElMessage.warning('请填写修改指令')
    return
  }

  store.setAiLoading(true)
  store.setError(null)
  try {
    const dsl = await modifyDsl(store.pageDsl, instr)
    store.setDslFromAi(dsl)
    store.addChat('user', instr)
    store.addChat('assistant', `已按指令修改页面：${dsl.title || '未命名'}`)
    await store.refreshExport()
    instruction.value = ''
    ElMessage.success('AI 修改成功')
  } catch (e) {
    handleAiError(e)
  } finally {
    store.setAiLoading(false)
  }
}

defineExpose({ loadExample })
</script>

<template>
  <div class="panel-section ai-panel">
    <h3 class="panel-title">从描述生成</h3>
    <p class="panel-hint">无 API Key 时可先点「加载示例」体验画布与导出。</p>

    <div class="ai-quick">
      <button
        v-for="q in QUICK_PROMPTS"
        :key="q.label"
        type="button"
        class="ai-chip"
        :disabled="store.aiLoading"
        @click="applyQuick(q.text)"
      >
        {{ q.label }}
      </button>
    </div>

    <el-form label-position="top" size="small" @submit.prevent>
      <el-form-item label="页面类型">
        <el-select v-model="pageType" style="width: 100%" :disabled="store.aiLoading">
          <el-option label="列表 list" value="list" />
          <el-option label="表单 form" value="form" />
          <el-option label="详情 detail" value="detail" />
          <el-option label="看板 dashboard" value="dashboard" />
        </el-select>
      </el-form-item>
      <el-form-item label="风格">
        <el-select v-model="style" style="width: 100%" :disabled="store.aiLoading">
          <el-option label="Element Plus" value="element-plus" />
          <el-option label="Plain HTML" value="plain" />
        </el-select>
      </el-form-item>
      <el-form-item label="页面描述">
        <el-input
          v-model="description"
          type="textarea"
          :rows="3"
          placeholder="用一句话描述你要的中后台页面…"
          :disabled="store.aiLoading"
        />
      </el-form-item>
    </el-form>
    <div class="ai-actions">
      <el-button
        type="primary"
        size="small"
        :loading="store.aiLoading"
        :disabled="store.aiLoading"
        @click="onGenerate"
      >
        AI 生成
      </el-button>
      <el-button size="small" :disabled="store.aiLoading" @click="loadExample">
        加载示例
      </el-button>
    </div>

    <div class="ai-section">
      <h3 class="panel-title">迭代修改</h3>
      <el-input
        v-model="instruction"
        type="textarea"
        :rows="2"
        size="small"
        placeholder="例如：给表格加一列手机号"
        :disabled="store.aiLoading || !store.pageDsl"
      />
      <div class="ai-actions" style="margin-top: 10px">
        <el-button
          type="primary"
          plain
          size="small"
          :loading="store.aiLoading"
          :disabled="!store.pageDsl || store.aiLoading"
          @click="onModify"
        >
          应用到当前页
        </el-button>
      </div>
    </div>

    <div class="ai-section">
      <h3 class="panel-title">会话记录</h3>
      <div ref="chatListRef" class="ai-chat">
        <div v-if="store.chatMessages.length === 0" class="ai-chat-empty">
          生成或修改后，对话会出现在这里
        </div>
        <div
          v-for="(m, i) in store.chatMessages"
          :key="i"
          class="ai-chat-item"
          :class="`role-${m.role}`"
        >
          <span class="ai-chat-role">{{ ROLE_LABEL[m.role] ?? m.role }}</span>
          <span class="ai-chat-content">{{ m.content }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
