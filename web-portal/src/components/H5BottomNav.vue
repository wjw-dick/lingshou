<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useLang } from '@/i18n'

const route = useRoute()
const { t } = useLang()

const tabs = computed(() => [
  { key: 'deals', path: '/h5/deals', label: t('h5.navDeals') },
  { key: 'newcomer', path: '/h5/newcomer', label: t('h5.navNewcomer') },
  { key: 'stores', path: '/stores', label: t('h5.navStores') },
  { key: 'franchise', path: '/h5/franchise', label: t('h5.navFranchise') }
])

function active(key: string): boolean {
  if (key === 'deals') return route.path.startsWith('/h5/deals')
  if (key === 'newcomer') return route.path.startsWith('/h5/newcomer')
  if (key === 'franchise') return route.path.startsWith('/h5/franchise')
  return route.path.startsWith('/stores') || route.path.startsWith('/h5/store')
}
</script>

<template>
  <nav class="h5-bottom" aria-label="H5 底部导航">
    <router-link
      v-for="tab in tabs"
      :key="tab.key"
      :to="tab.path"
      class="h5-bottom__item"
      :class="{ 'is-active': active(tab.key) }"
    >
      <span class="h5-bottom__icon">
        <svg v-if="tab.key === 'deals'" viewBox="0 0 24 24" fill="none">
          <path
            d="M4 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4V8Z"
            stroke="currentColor"
            stroke-width="1.4"
          />
          <path d="M14 7v10" stroke="currentColor" stroke-width="1.2" stroke-dasharray="2 3" />
        </svg>
        <svg v-else-if="tab.key === 'newcomer'" viewBox="0 0 24 24" fill="none">
          <path d="M4 9h16v4H4zM6 13h12v7H6z" stroke="currentColor" stroke-width="1.4" />
          <path d="M12 9v11M12 9C10 9 7.5 8.2 7.5 6.2 7.5 4.6 9.4 4 12 9Zm0 0c2 0 4.5-.8 4.5-2.8C16.5 4.6 14.6 4 12 9Z" stroke="currentColor" stroke-width="1.3" />
        </svg>
        <svg v-else-if="tab.key === 'stores'" viewBox="0 0 24 24" fill="none">
          <path d="M12 21s7-5.6 7-11a7 7 0 1 0-14 0c0 5.4 7 11 7 11Z" stroke="currentColor" stroke-width="1.4" />
          <circle cx="12" cy="10" r="2.4" stroke="currentColor" stroke-width="1.4" />
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none">
          <path d="M4 10 5.5 5h13L20 10M5 10v9h14v-9M4 10h16M9 19v-5h6v5" stroke="currentColor" stroke-width="1.4" />
        </svg>
      </span>
      <span>{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.h5-bottom {
  position: fixed;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  z-index: 90;
  width: min(560px, 100%);
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background: rgba(251, 248, 241, 0.96);
  border-top: 1px solid var(--line);
  backdrop-filter: blur(18px);
  padding-bottom: env(safe-area-inset-bottom);
}

.h5-bottom__item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-height: 58px;
  font-size: 10px;
  letter-spacing: 0.04em;
  color: var(--muted);
  transition: color 0.25s ease;
}

.h5-bottom__item.is-active {
  color: var(--ink);
}

.h5-bottom__item.is-active::before {
  content: "";
  position: absolute;
  top: 0;
  width: 18px;
  height: 2px;
  border-radius: 2px;
  background: var(--tomato);
}

.h5-bottom__icon svg {
  width: 22px;
  height: 22px;
}
</style>
