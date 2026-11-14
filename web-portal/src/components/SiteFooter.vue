<script setup lang="ts">
import { computed } from 'vue'
import { useLang } from '@/i18n'
import { useSiteStore } from '@/composables/useSiteStore'

const { config, menus } = useSiteStore()
const { t, menuTitle } = useLang()

const logoName = computed(() => config('site.name', t('brand.name')))
const tenantName = computed(() => config('site.name', '悦享便利连锁'))
const phone = computed(() => config('site.service_phone', '400-880-0001'))
const hours = computed(() => config('site.business_hours', '07:00–23:00'))
const icp = computed(() => config('site.icp', '沪ICP备2026000001号'))
const year = new Date().getFullYear()

const navItems = computed(() => {
  const list = menus.value.length ? menus.value : []
  return [...list].sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0))
})
</script>

<template>
  <footer class="site-footer">
    <div class="container">
      <div class="site-footer__top">
        <div class="site-footer__brand">
          <p class="eyebrow eyebrow--light">YUEXIANG MART · SHANGHAI</p>
          <h2 class="display display--md">{{ logoName }}</h2>
          <p class="site-footer__slogan">{{ t('footer.slogan') }}</p>
          <router-link to="/franchise" class="btn btn--light site-footer__cta">
            {{ t('nav.franchise') }}
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
            </svg>
          </router-link>
        </div>

        <div class="site-footer__columns">
          <div class="site-footer__col">
            <span class="site-footer__label">{{ t('footer.explore') }}</span>
            <router-link v-for="item in navItems" :key="item.id" :to="item.path">
              {{ menuTitle(item) }}
            </router-link>
            <router-link to="/h5/deals">H5 · Deals</router-link>
          </div>
          <div class="site-footer__col">
            <span class="site-footer__label">{{ t('footer.partner') }}</span>
            <router-link to="/franchise">{{ t('nav.franchise') }}</router-link>
            <router-link to="/contact">{{ t('nav.contact') }}</router-link>
            <router-link to="/news">{{ t('nav.news') }}</router-link>
          </div>
          <div class="site-footer__col site-footer__col--contact">
            <span class="site-footer__label">{{ t('footer.contact') }}</span>
            <a :href="`tel:${phone.replace(/[^\d+]/g, '')}`" class="site-footer__phone">{{ phone }}</a>
            <span class="site-footer__meta">{{ t('footer.hours') }} · {{ hours }}</span>
            <span class="site-footer__meta">{{ t('footer.icp') }} · {{ icp }}</span>
            <span class="site-footer__meta">{{ tenantName }}</span>
          </div>
        </div>
      </div>

      <div class="site-footer__rule"></div>

      <div class="site-footer__bottom">
        <span>© {{ year }} {{ logoName }}. {{ t('footer.rights') }}.</span>
        <span class="site-footer__made">Neighborhood, considered.</span>
      </div>
    </div>
    <div class="site-footer__watermark" aria-hidden="true">{{ logoName }}</div>
  </footer>
</template>

<style scoped>
.site-footer {
  position: relative;
  overflow: hidden;
  background: var(--ink);
  color: var(--cream);
  padding: 100px 0 40px;
}

.site-footer__top {
  display: grid;
  grid-template-columns: 1.05fr 1.2fr;
  gap: 80px;
  position: relative;
  z-index: 2;
}

.site-footer__brand h2 {
  margin: 26px 0 14px;
  letter-spacing: 0.02em;
}

.site-footer__slogan {
  font-family: var(--font-display);
  font-size: 19px;
  font-style: italic;
  color: rgba(246, 240, 228, 0.58);
  margin-bottom: 32px;
}

.site-footer__cta {
  min-height: 44px;
}

.site-footer__columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 36px;
}

.site-footer__col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13px;
}

.site-footer__col a {
  color: rgba(246, 240, 228, 0.62);
  transition: color 0.25s ease, transform 0.25s ease;
  width: fit-content;
}

.site-footer__col a:hover {
  color: var(--cream);
  transform: translateX(3px);
}

.site-footer__label {
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(246, 240, 228, 0.38);
  margin-bottom: 8px;
}

.site-footer__phone {
  font-family: var(--font-display);
  font-size: 30px !important;
  color: var(--cream) !important;
  letter-spacing: 0.01em;
}

.site-footer__meta {
  color: rgba(246, 240, 228, 0.42);
  font-size: 11px;
  line-height: 1.9;
}

.site-footer__rule {
  height: 1px;
  background: rgba(246, 240, 228, 0.14);
  margin: 60px 0 24px;
  position: relative;
  z-index: 2;
}

.site-footer__bottom {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: rgba(246, 240, 228, 0.38);
  position: relative;
  z-index: 2;
}

.site-footer__made {
  font-family: var(--font-display);
  font-style: italic;
  font-size: 12px;
  color: rgba(246, 240, 228, 0.52);
}

.site-footer__watermark {
  position: absolute;
  left: 50%;
  bottom: -0.24em;
  transform: translateX(-50%);
  font-family: var(--font-display);
  font-size: clamp(140px, 22vw, 340px);
  line-height: 0.8;
  letter-spacing: -0.06em;
  white-space: nowrap;
  color: rgba(246, 240, 228, 0.035);
  pointer-events: none;
  z-index: 0;
}

@media (max-width: 980px) {
  .site-footer__top {
    grid-template-columns: 1fr;
    gap: 48px;
  }
}

@media (max-width: 640px) {
  .site-footer {
    padding: 72px 0 32px;
  }

  .site-footer__columns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 32px 24px;
  }

  .site-footer__bottom {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
