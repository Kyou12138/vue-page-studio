<script setup lang="ts">
import { computed } from 'vue'
import { usePageStore } from '../../stores/pageStore'
import PreviewNode from './PreviewNode.vue'

const store = usePageStore()
const dsl = computed(() => store.pageDsl)
const isPlain = computed(() => dsl.value?.style === 'plain')
</script>

<template>
  <div v-if="!dsl" class="preview-pane" style="color: #909399">
    暂无页面，请先生成或加载示例。
  </div>
  <div v-else class="preview-pane" :class="{ plain: isPlain }">
    <PreviewNode v-for="node in dsl.children" :key="node.id" :node="node" :plain="isPlain" />
  </div>
</template>
