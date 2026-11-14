<script setup lang="ts">
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'

const { t, isEn } = useLang()

useSeo(() => ({
  title: isEn.value ? 'Page not found — Yuexiang Mart' : '页面不存在 — 悦享便利',
  description: isEn.value
    ? 'The page you are looking for does not exist.'
    : '你访问的页面不存在，返回悦享便利首页继续浏览。'
}))
</script>

<template>
  <div class="not-found">
    <div class="container not-found__inner">
      <p class="eyebrow">404 / LOST IN THE NEIGHBORHOOD</p>
      <h1 class="display display--xl">404</h1>
      <p class="not-found__desc">
        {{ isEn ? 'This page has wandered off. Let’s get you back to the neighborhood.' : '这个页面走丢了，回到社区里继续逛逛吧。' }}
      </p>
      <div class="not-found__actions">
        <router-link to="/" class="btn">{{ t('nav.home') }}</router-link>
        <router-link to="/stores" class="btn btn--ghost">{{ t('nav.stores') }}</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: calc(100vh - var(--header-h));
  display: flex;
  align-items: center;
  background: var(--cream);
}

.not-found__inner {
  padding: 80px 0 120px;
}

.not-found h1 {
  font-size: clamp(120px, 26vw, 360px);
  line-height: 0.8;
  margin: 30px 0 20px;
  letter-spacing: -0.06em;
}

.not-found__desc {
  max-width: 420px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--muted);
}

.not-found__actions {
  display: flex;
  gap: 12px;
  margin-top: 34px;
}
</style>
