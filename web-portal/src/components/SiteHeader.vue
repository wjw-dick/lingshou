<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useLang } from '@/i18n'
import { useSiteStore } from '@/composables/useSiteStore'

const { state, menus, config, load } = useSiteStore()
const { t, menuTitle } = useLang()
const route = useRoute()

const scrolled = ref(false)
const open = ref(false)

const fallbackMenus = computed(() => [
  { id: -1, path: '/', title: t('nav.home'), titleEn: 'Home', sortNo: 0 },
  { id: -2, path: '/stores', title: t('nav.stores'), titleEn: 'Stores', sortNo: 1 },
  { id: -3, path: '/products', title: t('nav.products'), titleEn: 'Fresh & Goods', sortNo: 2 },
  { id: -4, path: '/franchise', title: t('nav.franchise'), titleEn: 'Franchise', sortNo: 3 },
  { id: -5, path: '/news', title: t('nav.news'), titleEn: 'Journal', sortNo: 4 },
  { id: -6, path: '/contact', title: t('nav.contact'), titleEn: 'Contact', sortNo: 5 }
])

const navItems = computed(() => {
  const list = menus.value.length ? menus.value : fallbackMenus.value
  return [...list].sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0))
})

const isHome = computed(() => route.path === '/')
const overlay = computed(() => isHome.value && !scrolled.value && !open.value)
const light = computed(() => overlay.value || open.value)
const logoName = computed(() => config('site.name', t('brand.name')))
const phone = computed(() => config('site.service_phone', '400-880-0001'))
const hours = computed(() => config('site.business_hours', '07:00–23:00'))

const isActive = (path: string) =>
  path === '/' ? route.path === '/' : route.path.startsWith(path)

function onScroll() {
  scrolled.value = window.scrollY > 36
}

function toggle() {
  open.value = !open.value
}

watch(
  () => route.fullPath,
  () => {
    open.value = false
  }
)

watch(open, (value) => {
  document.body.style.overflow = value ? 'hidden' : ''
})

onMounted(() => {
  load()
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  document.body.style.overflow = ''
})
</script>

<template>
  <header
    class="site-header"
    :class="{ 'is-overlay': overlay, 'is-solid': !overlay, 'is-open': open }"
  >
    <div class="site-header__inner">
      <router-link to="/" class="brand" :class="{ 'is-light': light }" aria-label="悦享便利首页">
        <span class="brand__mark" aria-hidden="true">
          <svg viewBox="0 0 44 44" fill="none">
            <circle cx="22" cy="22" r="20.5" stroke="currentColor" stroke-width="1.2" />
            <path d="M13.5 14.5 22 24.5l8.5-10" stroke="currentColor" stroke-width="1.6" />
            <path d="M22 24.5v7" stroke="currentColor" stroke-width="1.6" />
            <circle cx="32.5" cy="12.5" r="2.4" fill="var(--tomato)" />
          </svg>
        </span>
        <span class="brand__text">
          <strong>{{ logoName }}</strong>
          <small>YUEXIANG MART</small>
        </span>
      </router-link>

      <nav class="site-nav" aria-label="主导航">
        <router-link
          v-for="item in navItems"
          :key="item.id"
          :to="item.path"
          class="site-nav__link"
          :class="{ 'is-active': isActive(item.path) }"
        >
          {{ menuTitle(item) }}
        </router-link>
      </nav>

      <div class="site-header__actions">
        <LanguageSwitch :light="light" />
        <router-link
          to="/products"
          class="btn btn--sm header-cta"
          :class="light ? 'btn--ghost-light' : 'btn--ghost'"
        >
          {{ t('common.miniProgram') }}
          <svg width="13" height="13" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </router-link>
        <button
          class="menu-toggle"
          type="button"
          :aria-expanded="open"
          aria-label="打开菜单"
          @click="toggle"
        >
          <span></span>
          <span></span>
        </button>
      </div>
    </div>
  </header>

  <Transition name="drawer">
    <div v-if="open" class="mobile-drawer">
      <div class="mobile-drawer__inner">
        <p class="eyebrow eyebrow--light">{{ t('brand.tagline') }}</p>
        <nav class="mobile-drawer__nav" aria-label="移动端导航">
          <router-link
            v-for="(item, index) in navItems"
            :key="item.id"
            :to="item.path"
            class="mobile-drawer__link"
            :class="{ 'is-active': isActive(item.path) }"
          >
            <span class="mobile-drawer__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span>{{ menuTitle(item) }}</span>
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <path d="M4 10h12M11 5l5 5-5 5" stroke="currentColor" stroke-width="1.2" />
            </svg>
          </router-link>
        </nav>
        <div class="mobile-drawer__foot">
          <div>
            <span>{{ t('footer.service') }}</span>
            <a :href="`tel:${phone.replace(/[^\d+]/g, '')}`">{{ phone }}</a>
          </div>
          <div>
            <span>{{ t('footer.hours') }}</span>
            <strong>{{ hours }}</strong>
          </div>
          <LanguageSwitch light />
        </div>
        <p class="mobile-drawer__brand">{{ logoName }}<span>社区 5 分钟生活圈</span></p>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.site-header {
  position: fixed;
  inset: 0 0 auto;
  z-index: 120;
  height: var(--header-h);
  transition: background 0.45s ease, border-color 0.45s ease, box-shadow 0.45s ease,
    color 0.45s ease;
  border-bottom: 1px solid transparent;
}

.site-header.is-overlay {
  color: var(--cream);
}

.site-header.is-solid {
  background: rgba(246, 240, 228, 0.94);
  border-bottom-color: var(--line);
  backdrop-filter: blur(18px);
}

.site-header.is-open {
  background: var(--green);
  border-bottom-color: rgba(246, 240, 228, 0.12);
  color: var(--cream);
}

.site-header__inner {
  width: min(1440px, calc(100% - 72px));
  height: 100%;
  margin-inline: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--ink);
  transition: color 0.35s ease;
  flex-shrink: 0;
}

.site-header.is-overlay .brand,
.site-header.is-open .brand {
  color: var(--cream);
}

.brand__mark {
  width: 38px;
  height: 38px;
  display: block;
}

.brand__mark svg {
  width: 100%;
  height: 100%;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1;
  gap: 5px;
}

.brand__text strong {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.brand__text small {
  font-size: 8px;
  letter-spacing: 0.28em;
  opacity: 0.56;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 34px;
  margin-left: auto;
}

.site-nav__link {
  position: relative;
  font-size: 13px;
  letter-spacing: 0.05em;
  padding: 8px 0;
  opacity: 0.78;
  transition: opacity 0.25s ease;
  white-space: nowrap;
}

.site-nav__link::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: currentColor;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.site-nav__link:hover {
  opacity: 1;
}

.site-nav__link:hover::after,
.site-nav__link.is-active::after {
  transform: scaleX(1);
}

.site-nav__link.is-active {
  opacity: 1;
}

.site-header__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.header-cta {
  min-height: 38px;
  padding: 0 18px;
}

.menu-toggle {
  display: none;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
}

.menu-toggle span {
  display: block;
  width: 22px;
  height: 1px;
  background: currentColor;
  transition: transform 0.35s ease;
}

.is-open .menu-toggle span:first-child {
  transform: translateY(3.5px) rotate(45deg);
}

.is-open .menu-toggle span:last-child {
  transform: translateY(-3.5px) rotate(-45deg);
}

.mobile-drawer {
  position: fixed;
  inset: 0;
  z-index: 110;
  background: var(--green);
  color: var(--cream);
  padding-top: calc(var(--header-h) + 24px);
  overflow-y: auto;
}

.mobile-drawer__inner {
  width: min(560px, calc(100% - 48px));
  margin-inline: auto;
  padding-bottom: calc(48px + env(safe-area-inset-bottom));
}

.mobile-drawer__nav {
  display: flex;
  flex-direction: column;
  margin-top: 28px;
  border-top: 1px solid rgba(246, 240, 228, 0.14);
}

.mobile-drawer__link {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px 0;
  border-bottom: 1px solid rgba(246, 240, 228, 0.14);
  font-family: var(--font-display);
  font-size: 30px;
  letter-spacing: -0.02em;
  transition: color 0.25s ease, padding-left 0.3s ease;
}

.mobile-drawer__link:hover,
.mobile-drawer__link.is-active {
  color: var(--tomato-soft);
  padding-left: 8px;
}

.mobile-drawer__link svg {
  margin-left: auto;
  opacity: 0.4;
}

.mobile-drawer__index {
  font-family: var(--font-sans);
  font-size: 10px;
  letter-spacing: 0.12em;
  opacity: 0.45;
  min-width: 22px;
}

.mobile-drawer__foot {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 28px;
  margin-top: 36px;
  font-size: 12px;
}

.mobile-drawer__foot > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-drawer__foot span {
  color: rgba(246, 240, 228, 0.5);
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.mobile-drawer__foot a,
.mobile-drawer__foot strong {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0.02em;
}

.mobile-drawer__brand {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
  margin-top: 44px;
  padding-top: 20px;
  border-top: 1px solid rgba(246, 240, 228, 0.14);
  font-size: 13px;
  letter-spacing: 0.1em;
}

.mobile-drawer__brand span {
  font-size: 10px;
  color: rgba(246, 240, 228, 0.45);
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.35s ease, transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateY(-2%);
}

@media (max-width: 1180px) {
  .site-nav {
    gap: 22px;
  }

  .site-nav__link {
    font-size: 12px;
  }
}

@media (max-width: 1020px) {
  .site-nav {
    display: none;
  }

  .menu-toggle {
    display: flex;
  }

  .header-cta {
    display: none;
  }
}

@media (max-width: 720px) {
  .site-header__inner {
    width: calc(100% - 40px);
  }

  .brand__mark {
    width: 34px;
    height: 34px;
  }

  .brand__text strong {
    font-size: 15px;
  }

  .site-header__actions :deep(.lang-switch) {
    display: none;
  }
}
</style>
