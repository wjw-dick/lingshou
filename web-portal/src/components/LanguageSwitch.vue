<script setup lang="ts">
import { useLang } from '@/i18n'

defineProps<{ light?: boolean }>()
const { lang, setLang } = useLang()
</script>

<template>
  <div class="lang-switch" :class="{ 'is-light': light }" role="group" aria-label="Language">
    <button
      type="button"
      class="lang-switch__item"
      :class="{ 'is-active': lang === 'zh' }"
      @click="setLang('zh')"
    >
      中文
    </button>
    <span class="lang-switch__divider" aria-hidden="true"></span>
    <button
      type="button"
      class="lang-switch__item"
      :class="{ 'is-active': lang === 'en' }"
      @click="setLang('en')"
    >
      EN
    </button>
  </div>
</template>

<style scoped>
.lang-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 4px 12px;
  height: 36px;
  transition: border-color 0.3s ease;
}

.lang-switch__item {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--muted);
  transition: color 0.25s ease;
  line-height: 1;
}

.lang-switch__item.is-active {
  color: var(--ink);
  font-weight: 600;
}

.lang-switch__divider {
  width: 1px;
  height: 10px;
  background: var(--line);
}

.is-light {
  border-color: rgba(246, 240, 228, 0.32);
}

.is-light .lang-switch__item {
  color: rgba(246, 240, 228, 0.6);
}

.is-light .lang-switch__item.is-active {
  color: var(--cream);
}

.is-light .lang-switch__divider {
  background: rgba(246, 240, 228, 0.3);
}
</style>
