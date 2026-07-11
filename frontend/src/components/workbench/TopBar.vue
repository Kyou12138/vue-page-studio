<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { usePageStore } from '../../stores/pageStore'
import type { StyleType } from '../../types/dsl'

const store = usePageStore()

const pageTitle = computed(() => store.pageDsl?.title || '尚未加载页面')
const style = computed({
  get: (): StyleType => store.pageDsl?.style ?? 'element-plus',
  set: (v: StyleType) => store.setStyle(v),
})

async function onExportDownload() {
  if (!store.pageDsl) {
    ElMessage.warning('请先加载示例或生成页面')
    return
  }
  await store.refreshExport()
  if (!store.exportCode) {
    ElMessage.error(store.errorMessage || '导出失败，请确认后端已启动')
    return
  }
  const blob = new Blob([store.exportCode], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  const safe = (store.pageDsl.title || 'page').replace(/[\\/:*?"<>|]/g, '_')
  a.href = url
  a.download = `${safe}.vue`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已下载 .vue 文件')
}

async function onCopy() {
  if (!store.pageDsl) {
    ElMessage.warning('请先加载示例或生成页面')
    return
  }
  await store.refreshExport()
  if (!store.exportCode) {
    ElMessage.error(store.errorMessage || '导出失败，请确认后端已启动')
    return
  }
  try {
    await navigator.clipboard.writeText(store.exportCode)
    ElMessage.success('代码已复制')
  } catch {
    ElMessage.error('复制失败，请到「Vue 代码」面板手动选择')
  }
}
</script>

<template>
  <header class="workbench-top">
    <div class="topbar-brand">
      <div class="topbar-logo" aria-hidden="true">V</div>
      <div class="topbar-brand-text">
        <span class="topbar-product">VuePage Studio</span>
        <span class="topbar-title" :title="pageTitle">{{ pageTitle }}</span>
      </div>
    </div>
    <span class="topbar-badge">低代码 + AI</span>

    <div class="topbar-divider" />

    <el-select
      v-model="style"
      size="small"
      style="width: 148px"
      :disabled="!store.pageDsl"
      placeholder="导出风格"
    >
      <el-option label="Element Plus" value="element-plus" />
      <el-option label="Plain HTML" value="plain" />
    </el-select>

    <el-button-group>
      <el-button size="small" :disabled="!store.canUndo || store.aiLoading" @click="store.undo()">
        撤销
      </el-button>
      <el-button size="small" :disabled="!store.canRedo || store.aiLoading" @click="store.redo()">
        重做
      </el-button>
    </el-button-group>

    <div class="topbar-spacer" />

    <span v-if="store.errorMessage" class="topbar-error" :title="store.errorMessage">
      {{ store.errorMessage }}
    </span>

    <el-button size="small" :disabled="!store.pageDsl" @click="onCopy">复制代码</el-button>
    <el-button size="small" type="primary" :disabled="!store.pageDsl" @click="onExportDownload">
      导出 Vue
    </el-button>
  </header>
</template>
