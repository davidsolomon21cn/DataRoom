<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resolveCasError } from '@/dataRoom/cas'

defineOptions({
  name: 'DataRoomError',
})

const route = useRoute()
const router = useRouter()
const errorPresentation = computed(() => {
  const code = Array.isArray(route.query.code) ? route.query.code[0] : route.query.code
  return resolveCasError(code)
})

const returnToLogin = () => router.push('/login')
const returnToHome = () => router.push('/')
</script>

<template>
  <main class="error-page">
    <el-result icon="error" :title="errorPresentation.title" :sub-title="errorPresentation.description">
      <template #extra>
        <div class="error-page__actions">
          <el-button @click="returnToLogin">返回登录</el-button>
          <el-button type="primary" @click="returnToHome">返回首页</el-button>
        </div>
      </template>
    </el-result>
  </main>
</template>

<style scoped lang="scss">
.error-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  padding: 24px;
  background: var(--el-bg-color-page);
}

.error-page__actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
