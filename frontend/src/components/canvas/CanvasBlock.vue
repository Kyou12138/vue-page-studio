<script setup lang="ts">
import type { DslNode } from '../../types/dsl'
import { colorForType, summarizeNode } from '../../utils/nodeSummary'

defineProps<{
  node: DslNode
  selected: boolean
}>()

const emit = defineEmits<{
  select: [id: string]
}>()
</script>

<template>
  <div
    class="canvas-block"
    :class="{ 'is-selected': selected }"
    @click.stop="emit('select', node.id)"
  >
    <div class="canvas-block-bar" :style="{ background: colorForType(node.type) }" />
    <div class="canvas-block-body">
      <div class="canvas-block-type">{{ node.type }}</div>
      <div class="canvas-block-summary">{{ summarizeNode(node) }}</div>
    </div>
    <div class="canvas-block-handle" title="拖拽排序">⋮⋮</div>
  </div>
</template>
