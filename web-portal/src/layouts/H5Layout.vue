<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import H5BottomNav from '@/components/H5BottomNav.vue'
import MiniProgramSheet from '@/components/MiniProgramSheet.vue'
import { useSiteStore } from '@/composables/useSiteStore'
import { useLang } from '@/i18n'

const route = useRoute()
const router = useRouter()
const { load, config } = useSiteStore()
const { t } = useLang()
const sheetOpen = ref(false)

const showBack = computed(() => Boolean(route.meta.back))
const brand = computed(() => config('site.name', t('brand.name')))

function back() {
  if (window.history.length > 1) router.back()
  else router.push('/h5/deals')
}

onMounted(() => load())
</script>

<template>
  <div class="h5-shell">
    <header class="h5-topbar">
      <div class="h5-topbar__inner">
        <button v-if="showBack" type="button" class="h5-topbar__back" @click="back">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
            <path d="M11 3 5 9l6 6" stroke="currentColor" stroke-width="1.5" />
          </svg>
          <span>{{ t('common.back') }}</span>
        </button>
        <router-link v-else to="/h5/deals" class="h5-topbar__brand">
          <span class="h5-topbar__mark">Y</span>
          <span class="h5-topbar__name">{{ brand }}</span>
        </router-link>

        <button type="button" class="h5-topbar__mini" @click="sheetOpen = true">
          {{ t('common.miniProgram') }}
          <svg width="11" height="11" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </button>
      </div>
    </header>

    <main class="h5-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <H5BottomNav />
    <MiniProgramSheet :open="sheetOpen" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
.h5-shell {
  position: relative;
  width: min(560px, 100%);
  min-height: 100vh;
  margin-inline: auto;
  background: var(--paper);
  box-shadow: 0 0 80px rgba(16, 37, 29, 0.08);
}

.h5-topbar {
  position: sticky;
  top: 0;
  z-index: 95;
  padding-top: env(safe-area-inset-top);
  background: rgba(251, 248, 241, 0.94);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(18px);
}

.h5-topbar__inner {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 18px;
}

.h5-topbar__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-soft);
}

.h5-topbar__brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.h5-topbar__mark {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--green);
  color: var(--cream);
  font-family: var(--font-display);
  font-size: 15px;
}

.h5-topbar__name {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.h5-topbar__mini {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--green);
  color: var(--cream);
  font-size: 10px;
  letter-spacing: 0.04em;
}

.h5-main {
  min-height: calc(100vh - 56px);
  padding-bottom: calc(74px + env(safe-area-inset-bottom));
}
</style>
