<script setup lang="ts">
import { onMounted } from 'vue'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { useSiteStore } from '@/composables/useSiteStore'

const { load } = useSiteStore()
onMounted(() => load())
</script>

<template>
  <div class="site-layout">
    <SiteHeader />
    <main class="site-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.site-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.site-main {
  flex: 1;
  padding-top: var(--header-h);
}
</style>
