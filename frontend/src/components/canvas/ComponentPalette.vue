<script setup lang="ts">
import { COMPONENT_TYPES } from '../../dsl/defaultProps'
import { colorForType, TYPE_LABELS } from '../../utils/nodeSummary'
import { PALETTE_MIME } from '../../utils/dnd'

function onDragStart(e: DragEvent, type: string) {
  if (!e.dataTransfer) return
  e.dataTransfer.setData(PALETTE_MIME, type)
  e.dataTransfer.setData('text/plain', type)
  e.dataTransfer.effectAllowed = 'copy'
}
</script>

<template>
  <div class="panel-section panel-scroll">
    <h3 class="panel-title">组件库</h3>
    <p class="panel-hint">拖到中间画布；可放入 Container 内部或左右列。</p>
    <div class="palette-grid">
      <div
        v-for="type in COMPONENT_TYPES"
        :key="type"
        class="palette-item"
        draggable="true"
        :title="type"
        @dragstart="onDragStart($event, type)"
      >
        <span class="palette-dot" :style="{ background: colorForType(type) }" />
        <span class="palette-item-name">{{ TYPE_LABELS[type] ?? type }}</span>
        <span class="palette-item-type">{{ type }}</span>
      </div>
    </div>
  </div>
</template>
