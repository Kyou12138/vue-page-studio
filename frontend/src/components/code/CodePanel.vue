<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { usePageStore } from '../../stores/pageStore'

const store = usePageStore()

let timer: ReturnType<typeof setTimeout> | null = null

function scheduleRefresh() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    void store.refreshExport()
  }, 300)
}

watch(
  () => store.pageDsl,
  () => {
    if (!store.pageDsl) return
    scheduleRefresh()
  },
  { deep: true, immediate: true },
)

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})

async function onRefresh() {
  if (!store.pageDsl) {
    ElMessage.warning('请先加载示例或生成页面')
    return
  }
  store.exportDslHash = ''
  await store.refreshExport()
  if (store.exportCode) {
    ElMessage.success('导出已刷新')
  } else {
    ElMessage.error(store.errorMessage || '导出失败，请确认后端已启动')
  }
}

async function onCopy() {
  if (!store.exportCode) {
    await onRefresh()
  }
  if (!store.exportCode) return
  try {
    await navigator.clipboard.writeText(store.exportCode)
    ElMessage.success('代码已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择代码')
  }
}
</script>

<template>
  <div class="code-panel">
    <div class="code-panel-toolbar">
      <el-button size="small" type="primary" plain :disabled="!store.pageDsl" @click="onRefresh">
        刷新导出
      </el-button>
      <el-button size="small" :disabled="!store.exportCode" @click="onCopy">复制</el-button>
      <span v-if="!store.pageDsl" class="hint">加载页面后将显示 Vue SFC</span>
      <span
        v-else-if="store.errorMessage && !store.exportCode"
        class="hint"
        style="color: var(--wb-danger)"
      >
        {{ store.errorMessage }}
      </span>
      <span v-else-if="store.exportCode" class="hint">后端确定性导出 · 可复制到项目</span>
    </div>
    <pre class="code-panel-body"><code>{{
      store.exportCode || '// 加载页面并确保后端运行后，将自动导出…'
    }}</code></pre>
  </div>
</template>
