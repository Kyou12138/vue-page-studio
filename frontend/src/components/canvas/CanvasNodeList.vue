<script setup lang="ts">
import { ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { usePageStore } from '../../stores/pageStore'
import type { DslNode } from '../../types/dsl'
import type { DropSlot } from '../../dsl/insertNode'
import { PALETTE_MIME } from '../../utils/dnd'
import CanvasBlock from './CanvasBlock.vue'

const props = defineProps<{
  /** null = 根列表 */
  parentId: string | null
  /** 投放槽：children | left | right（避免命名 slot，与 Vue 插槽冲突） */
  dropSlot: DropSlot
  nodes: DslNode[]
  /** 嵌套深度，用于缩进样式 */
  depth?: number
}>()

const store = usePageStore()
const dragList = ref<DslNode[]>([])
const dragging = ref(false)
const slotOver = ref(false)

watch(
  () => props.nodes,
  (nodes) => {
    if (dragging.value) return
    dragList.value = nodes ? nodes.map((c) => c) : []
  },
  { immediate: true, deep: true },
)

function onDragStart() {
  dragging.value = true
}

function onDragEnd() {
  dragging.value = false
  if (!store.pageDsl || store.aiLoading) return
  store.reorderSlot(props.parentId, props.dropSlot, dragList.value)
}

function acceptPalette(e: DragEvent): boolean {
  if (store.aiLoading) return false
  const types = e.dataTransfer?.types
  if (!types) return false
  return (
    Array.from(types).includes(PALETTE_MIME) || Array.from(types).includes('text/plain')
  )
}

function onSlotDragOver(e: DragEvent) {
  if (!acceptPalette(e)) return
  e.preventDefault()
  e.stopPropagation()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy'
  slotOver.value = true
}

function onSlotDragLeave() {
  slotOver.value = false
}

function onSlotDrop(e: DragEvent) {
  slotOver.value = false
  if (store.aiLoading) return
  e.preventDefault()
  e.stopPropagation()
  const type =
    e.dataTransfer?.getData(PALETTE_MIME) || e.dataTransfer?.getData('text/plain') || ''
  if (!type) return
  if (props.parentId == null) {
    store.insertFromPalette(type, null, 'append')
  } else {
    store.insertIntoSlot(type, props.parentId, props.dropSlot)
  }
}

function onBlockDragOver(e: DragEvent) {
  if (!acceptPalette(e)) return
  e.preventDefault()
  e.stopPropagation()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy'
}

function onBlockDrop(e: DragEvent, targetId: string) {
  if (store.aiLoading) return
  e.preventDefault()
  e.stopPropagation()
  const type =
    e.dataTransfer?.getData(PALETTE_MIME) || e.dataTransfer?.getData('text/plain') || ''
  if (!type) return
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const mid = rect.top + rect.height / 2
  const position = e.clientY < mid ? 'before' : 'after'
  store.insertFromPalette(type, targetId, position)
}

function childNodes(node: DslNode): DslNode[] {
  return node.children ?? []
}

function columnNodes(node: DslNode, side: 'left' | 'right'): DslNode[] {
  const v = node.props?.[side]
  if (!Array.isArray(v)) return []
  return v as DslNode[]
}

function isContainer(node: DslNode) {
  return node.type === 'Container'
}

function layoutOf(node: DslNode): string {
  return String(node.props?.layout ?? 'stack')
}
</script>

<template>
  <div
    class="canvas-node-list"
    :class="{ 'is-slot-over': slotOver, [`depth-${depth ?? 0}`]: true }"
    @dragover="onSlotDragOver"
    @dragleave="onSlotDragLeave"
    @drop="onSlotDrop"
  >
    <draggable
      v-if="dragList.length > 0"
      v-model="dragList"
      class="canvas-stack"
      item-key="id"
      handle=".canvas-block-handle"
      :animation="180"
      :disabled="store.aiLoading"
      @start="onDragStart"
      @end="onDragEnd"
    >
      <template #item="{ element }">
        <div class="canvas-item-wrap">
          <div
            @dragover="onBlockDragOver"
            @drop="onBlockDrop($event, element.id)"
          >
            <CanvasBlock
              :node="element"
              :selected="store.selectedId === element.id"
              @select="store.select($event)"
            />
          </div>

          <!-- Container stack：内部可投放 / 排序 -->
          <div
            v-if="isContainer(element) && layoutOf(element) === 'stack'"
            class="nested-slot"
            @click.stop
          >
            <div class="nested-slot-label">内部 · stack（可拖入组件）</div>
            <CanvasNodeList
              :parent-id="element.id"
              drop-slot="children"
              :nodes="childNodes(element)"
              :depth="(depth ?? 0) + 1"
            />
          </div>

          <!-- Container two-column -->
          <div
            v-else-if="isContainer(element) && layoutOf(element) === 'two-column'"
            class="nested-two-col"
            @click.stop
          >
            <div class="nested-col">
              <div class="nested-slot-label">左列</div>
              <CanvasNodeList
                :parent-id="element.id"
                drop-slot="left"
                :nodes="columnNodes(element, 'left')"
                :depth="(depth ?? 0) + 1"
              />
            </div>
            <div class="nested-col">
              <div class="nested-slot-label">右列</div>
              <CanvasNodeList
                :parent-id="element.id"
                drop-slot="right"
                :nodes="columnNodes(element, 'right')"
                :depth="(depth ?? 0) + 1"
              />
            </div>
          </div>
        </div>
      </template>
    </draggable>

    <div v-else class="slot-empty-hint">
      拖入组件到此{{ parentId ? '槽位' : '画布' }}
    </div>
  </div>
</template>

<style scoped>
.canvas-node-list {
  min-height: 48px;
  border-radius: 8px;
  transition: background 0.15s, outline 0.15s;
}

.canvas-node-list.is-slot-over {
  outline: 2px dashed #10b981;
  background: rgba(16, 185, 129, 0.08);
}

.canvas-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.canvas-item-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nested-slot {
  margin-left: 12px;
  padding: 8px;
  border: 1px dashed #3f4458;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.15);
}

.nested-two-col {
  margin-left: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.nested-col {
  padding: 8px;
  border: 1px dashed #3f4458;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.15);
  min-height: 64px;
}

.nested-slot-label {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.slot-empty-hint {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: #64748b;
  border: 1px dashed #3f4458;
  border-radius: 8px;
}
</style>
