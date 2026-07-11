<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import listUserFixture from '../../fixtures/list-user.json'
import { usePageStore } from '../../stores/pageStore'
import type { PageDsl } from '../../types/dsl'
import { PALETTE_MIME } from '../../utils/dnd'
import CanvasNodeList from './CanvasNodeList.vue'

const store = usePageStore()
const rootOver = ref(false)

function loadExample() {
  store.loadDsl(listUserFixture as PageDsl)
  store.addChat('system', '已加载示例：用户管理列表')
  ElMessage.success('已加载示例页面')
}

function clearCanvas() {
  if (!store.pageDsl) return
  store.replaceChildren([])
  store.select(null)
}

function ensurePage(): boolean {
  if (store.pageDsl) return true
  store.loadDsl({
    version: '1',
    pageType: 'list',
    style: 'element-plus',
    title: '未命名页面',
    children: [],
  })
  return true
}

function onRootDragOver(e: DragEvent) {
  if (store.aiLoading) return
  const types = e.dataTransfer?.types
  if (!types) return
  const ok =
    Array.from(types).includes(PALETTE_MIME) || Array.from(types).includes('text/plain')
  if (!ok) return
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy'
  rootOver.value = true
}

function onRootDragLeave() {
  rootOver.value = false
}

function onRootDrop(e: DragEvent) {
  rootOver.value = false
  if (store.aiLoading) return
  e.preventDefault()
  const type =
    e.dataTransfer?.getData(PALETTE_MIME) || e.dataTransfer?.getData('text/plain') || ''
  if (!type) return
  ensurePage()
  store.insertFromPalette(type, null, 'append')
}
</script>

<template>
  <div
    class="page-canvas"
    :class="{ 'is-drag-over': rootOver }"
    @dragover="onRootDragOver"
    @dragleave="onRootDragLeave"
    @drop="onRootDrop"
  >
    <div v-if="!store.pageDsl" class="canvas-empty">
      <h3>从零搭一页 Vue 中后台</h3>
      <p>低代码拖结构，AI 加速填字段；改完即可预览并导出可维护的 SFC。</p>
      <ol class="canvas-empty-steps">
        <li>
          <span class="step-num">1</span>
          <span>点「加载示例」立刻看到完整列表页，或用左侧 AI 描述生成</span>
        </li>
        <li>
          <span class="step-num">2</span>
          <span>打开「组件库」拖入区块；Container 支持内部 / 左右列投放</span>
        </li>
        <li>
          <span class="step-num">3</span>
          <span>点选节点在右侧改属性，底栏看预览与导出代码</span>
        </li>
      </ol>
      <div class="canvas-empty-actions">
        <el-button type="primary" @click="loadExample">加载示例</el-button>
      </div>
    </div>

    <template v-else>
      <div class="canvas-toolbar">
        <span class="canvas-toolbar-label">
          画布 · {{ store.pageDsl.children.length }} 个根节点
          <template v-if="store.selectedId"> · 已选中</template>
        </span>
        <el-button size="small" @click="loadExample">重载示例</el-button>
        <el-button size="small" :disabled="store.aiLoading" @click="clearCanvas">清空</el-button>
      </div>

      <CanvasNodeList
        :parent-id="null"
        drop-slot="children"
        :nodes="store.pageDsl.children"
        :depth="0"
      />
    </template>

    <div v-if="store.aiLoading" class="canvas-overlay">
      <div class="canvas-overlay-card">
        <span class="canvas-overlay-dot" />
        AI 处理中，画布已锁定…
      </div>
    </div>
  </div>
</template>
