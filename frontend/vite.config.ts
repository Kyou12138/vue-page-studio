/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // 避开 Windows 保留端口段（本机 5141–5240 等会导致 5173 EACCES）
    port: 3000,
    strictPort: false,
    proxy: {
      '/api': 'http://localhost:8088',
    },
  },
  test: {
    environment: 'jsdom',
  },
})
