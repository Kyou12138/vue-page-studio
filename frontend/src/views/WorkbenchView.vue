<script setup lang="ts">
import { ref } from 'vue'
import TopBar from '../components/workbench/TopBar.vue'
import AiPanel from '../components/ai/AiPanel.vue'
import ComponentPalette from '../components/canvas/ComponentPalette.vue'
import PageCanvas from '../components/canvas/PageCanvas.vue'
import PropsPanel from '../components/props/PropsPanel.vue'
import PagePreview from '../components/preview/PagePreview.vue'
import CodePanel from '../components/code/CodePanel.vue'

const leftTab = ref<'ai' | 'palette'>('ai')
const bottomTab = ref<'preview' | 'code'>('preview')
</script>

<template>
  <div class="workbench">
    <TopBar />
    <div class="workbench-body">
      <aside class="workbench-left">
        <div class="left-tabs" role="tablist">
          <button
            type="button"
            class="left-tab"
            role="tab"
            :class="{ 'is-active': leftTab === 'ai' }"
            :aria-selected="leftTab === 'ai'"
            @click="leftTab = 'ai'"
          >
            AI 助手
          </button>
          <button
            type="button"
            class="left-tab"
            role="tab"
            :class="{ 'is-active': leftTab === 'palette' }"
            :aria-selected="leftTab === 'palette'"
            @click="leftTab = 'palette'"
          >
            组件库
          </button>
        </div>
        <div class="left-pane">
          <AiPanel v-show="leftTab === 'ai'" />
          <ComponentPalette v-show="leftTab === 'palette'" />
        </div>
      </aside>
      <main class="workbench-center">
        <PageCanvas />
      </main>
      <aside class="workbench-right">
        <PropsPanel />
      </aside>
    </div>
    <div class="workbench-bottom">
      <div class="bottom-tabs">
        <div class="bottom-tab-bar" role="tablist">
          <button
            type="button"
            class="bottom-tab"
            :class="{ 'is-active': bottomTab === 'preview' }"
            @click="bottomTab = 'preview'"
          >
            页面预览
          </button>
          <button
            type="button"
            class="bottom-tab"
            :class="{ 'is-active': bottomTab === 'code' }"
            @click="bottomTab = 'code'"
          >
            Vue 代码
          </button>
        </div>
        <div class="bottom-tab-body">
          <PagePreview v-show="bottomTab === 'preview'" />
          <CodePanel v-show="bottomTab === 'code'" />
        </div>
      </div>
    </div>
  </div>
</template>
