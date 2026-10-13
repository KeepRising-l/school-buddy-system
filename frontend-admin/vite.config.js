import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  preview: {
    port: 4173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 分包：把体积大、更新频率低的第三方库拆成独立 chunk——
        // 业务代码改动不连累 vendor 缓存失效，且浏览器可并行加载。
        // 首次加载总量靠按需引入控制（echarts 已改按需，见 StatsView / Dashboard/Index）。
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts'
          if (id.includes('element-plus')) return 'vendor-element'
          if (id.includes('vue-router') || id.includes('pinia') || id.includes('/vue/') || id.includes('@vue/')) return 'vendor-vue'
        }
      }
    }
  }
})
