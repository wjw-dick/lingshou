<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SmartImage from '@/components/SmartImage.vue'
import StoreMap from '@/components/StoreMap.vue'
import { getDeliveryRule, getStores } from '@/api/site'
import { IMAGES } from '@/lib/assets'
import { amapUri, money, parseJsonArray, telUri, typeLabel } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import { showToast } from '@/composables/useToast'
import type { DeliveryRule, Store } from '@/types'

const route = useRoute()
const { t, isEn } = useLang()

const store = ref<Store | null>(null)
const rule = ref<DeliveryRule | null>(null)
const loading = ref(true)
const error = ref('')

const timeSlots = computed(() => parseJsonArray<string>(rule.value?.timeSlots))
const storeList = computed(() => (store.value ? [store.value] : []))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const list = await getStores()
    const found = list.find((item) => item.id === Number(route.params.storeId)) || null
    store.value = found
    if (found) {
      try {
        rule.value = await getDeliveryRule(found.id)
      } catch {
        rule.value = null
      }
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function share() {
  if (!store.value) return
  const shareData = {
    title: store.value.name,
    text: `${store.value.name} · ${store.value.address}`,
    url: window.location.href
  }
  if (navigator.share) {
    try {
      await navigator.share(shareData)
      return
    } catch {
      // fall through
    }
  }
  try {
    await navigator.clipboard.writeText(window.location.href)
    showToast(t('h5.shareCopied'))
  } catch {
    showToast(window.location.href)
  }
}

useSeo(() => ({
  title: store.value ? `${store.value.name} — 悦享便利` : t('h5.storeNotFound'),
  description: store.value
    ? `${store.value.name}，${store.value.address}，营业时间 ${store.value.businessHours}，电话 ${store.value.phone}。`
    : t('h5.storeNotFound')
}))

watch(() => route.params.storeId, load)
onMounted(load)
</script>

<template>
  <div class="h5-page h5store-page">
    <div v-if="loading" class="h5store-loading">
      <div class="skeleton h5store-loading__media"></div>
      <div class="skeleton h5store-loading__line"></div>
      <div class="skeleton h5store-loading__line"></div>
    </div>

    <div v-else-if="error || !store" class="h5store-error">
      <span class="h5store-error__mark">!</span>
      <h1>{{ t('h5.storeNotFound') }}</h1>
      <p>{{ error }}</p>
      <router-link to="/stores" class="btn btn--sm">{{ t('nav.stores') }}</router-link>
    </div>

    <template v-else>
      <section class="h5store-media">
        <SmartImage :src="IMAGES.store" :alt="store.name" ratio="16 / 10" art="店" label="YUEXIANG STORE" eager />
        <div class="h5store-media__overlay">
          <div class="h5store-media__tags">
            <span class="tag tag--light">{{ typeLabel(store.type) }}</span>
            <span class="tag tag--light">{{ store.region }}</span>
          </div>
          <h1>{{ store.name }}</h1>
          <div class="h5store-media__foot">
            <span>{{ store.code }}</span>
            <button type="button" @click="share">
              <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <circle cx="12" cy="3" r="2" stroke="currentColor" stroke-width="1.2" />
                <circle cx="4" cy="8" r="2" stroke="currentColor" stroke-width="1.2" />
                <circle cx="12" cy="13" r="2" stroke="currentColor" stroke-width="1.2" />
                <path d="m6 7 4-3M6 9l4 3" stroke="currentColor" stroke-width="1.2" />
              </svg>
              {{ t('common.share') }}
            </button>
          </div>
        </div>
      </section>

      <section class="h5store-info">
        <div class="h5store-info__row">
          <span>{{ t('stores.city') }}</span>
          <strong>{{ store.address }}</strong>
        </div>
        <div class="h5store-info__row">
          <span>{{ t('stores.hours') }}</span>
          <strong>{{ store.businessHours }}</strong>
        </div>
        <div class="h5store-info__row">
          <span>{{ t('common.call') }}</span>
          <a :href="telUri(store.phone)">{{ store.phone }}</a>
        </div>
        <div class="h5store-info__row">
          <span>{{ isEn ? 'Delivery' : '配送' }}</span>
          <strong>
            {{ store.deliveryEnabled ? (isEn ? 'Available' : '支持配送') : (isEn ? 'Pickup only' : '仅到店购买') }}
            <template v-if="store.deliveryRadiusM">
              · {{ store.deliveryRadiusM }}m
            </template>
          </strong>
        </div>
      </section>

      <section class="h5store-map">
        <div class="h5-section__head">
          <h2>{{ isEn ? 'Location' : '门店位置' }}</h2>
          <a :href="amapUri(store)" target="_blank" rel="noopener noreferrer" class="h5store-nav-link">
            {{ t('common.navigate') }} →
          </a>
        </div>
        <StoreMap :stores="storeList" :selected-id="store.id" height="220px" compact :show-card="false" :show-note="false" />
        <p class="h5store-map__note">{{ t('stores.mapNote') }}</p>
      </section>

      <section class="h5store-delivery">
        <div class="h5-section__head">
          <h2>{{ t('h5.storeDelivery') }}</h2>
          <span class="h5-muted">{{ rule ? (isEn ? 'Live rule' : '实时规则') : (isEn ? 'Not configured' : '未配置') }}</span>
        </div>

        <div v-if="rule" class="h5store-delivery__grid">
          <div>
            <span>{{ t('h5.minOrder') }}</span>
            <strong>{{ money(rule.minOrderAmount) }}</strong>
          </div>
          <div>
            <span>{{ t('h5.deliveryFee') }}</span>
            <strong>{{ money(rule.deliveryFee) }}</strong>
          </div>
          <div>
            <span>{{ t('h5.freeThreshold') }}</span>
            <strong>{{ money(rule.freeThreshold) }}</strong>
          </div>
          <div>
            <span>{{ t('h5.radius') }}</span>
            <strong>{{ rule.radiusM }} m</strong>
          </div>
        </div>

        <div v-if="timeSlots.length" class="h5store-slots">
          <span>{{ t('h5.timeSlots') }}</span>
          <div>
            <em v-for="slot in timeSlots" :key="slot">{{ slot }}</em>
          </div>
        </div>

        <div v-else-if="!rule" class="h5store-delivery__empty">
          {{ t('h5.noDelivery') }}
        </div>
      </section>

      <section class="h5store-cta">
        <router-link to="/products" class="h5store-cta__link">
          <span>{{ isEn ? 'Browse fresh food' : '看看今日鲜食' }}</span>
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </router-link>
      </section>

      <div class="h5-actionbar">
        <a :href="telUri(store.phone)" class="btn btn--ghost">{{ t('common.call') }}</a>
        <a :href="amapUri(store)" target="_blank" rel="noopener noreferrer" class="btn">
          {{ t('common.navigate') }}
          <svg width="13" height="13" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </a>
      </div>
    </template>
  </div>
</template>

<style scoped>
.h5store-page {
  background: var(--paper);
  padding-bottom: 130px;
}

.h5store-loading {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.h5store-loading__media {
  aspect-ratio: 16 / 10;
  border-radius: 22px;
}

.h5store-loading__line {
  height: 54px;
}

.h5store-media {
  position: relative;
  border-radius: 22px;
  overflow: hidden;
  isolation: isolate;
}

.h5store-media :deep(.smart-image) {
  border-radius: 22px;
}

.h5store-media__overlay {
  position: absolute;
  inset: auto 0 0;
  padding: 18px;
  background: linear-gradient(180deg, transparent, rgba(16, 37, 29, 0.88));
  color: var(--cream);
}

.h5store-media__tags {
  display: flex;
  gap: 6px;
}

.h5store-media__overlay h1 {
  font-size: 27px;
  line-height: 1.3;
  margin-top: 10px;
  font-weight: 500;
}

.h5store-media__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: rgba(246, 240, 228, 0.55);
}

.h5store-media__foot button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 9px;
  color: rgba(246, 240, 228, 0.75);
}

.h5store-info {
  margin-top: 18px;
  border-top: 1px solid var(--line);
}

.h5store-info__row {
  display: grid;
  grid-template-columns: 74px 1fr;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.h5store-info__row span {
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--muted);
}

.h5store-info__row strong,
.h5store-info__row a {
  font-size: 12px;
  font-weight: 500;
  line-height: 1.7;
  color: var(--ink-soft);
}

.h5store-info__row a {
  color: var(--tomato);
}

.h5store-map {
  margin-top: 24px;
}

.h5store-nav-link {
  font-size: 10px;
  color: var(--tomato);
}

.h5store-map :deep(.store-map) {
  margin-top: 10px;
}

.h5store-map__note {
  margin-top: 8px;
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--muted);
}

.h5store-delivery {
  margin-top: 26px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--white);
}

.h5store-delivery__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.h5store-delivery__grid div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.h5store-delivery__grid span {
  font-size: 9px;
  color: var(--muted);
}

.h5store-delivery__grid strong {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 400;
}

.h5store-slots {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.h5store-slots > span {
  font-size: 9px;
  color: var(--muted);
}

.h5store-slots > div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.h5store-slots em {
  font-style: normal;
  font-size: 10px;
  padding: 4px 9px;
  border-radius: 999px;
  background: var(--cream);
  color: var(--ink-soft);
}

.h5store-delivery__empty {
  padding-top: 12px;
  font-size: 10px;
  color: var(--muted);
  border-top: 1px solid var(--line);
}

.h5store-cta {
  margin-top: 20px;
}

.h5store-cta__link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 14px;
  background: var(--green);
  color: var(--cream);
  font-size: 12px;
}

.h5store-error {
  padding: 60px 20px;
  text-align: center;
}

.h5store-error__mark {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(214, 82, 47, 0.1);
  color: var(--tomato);
  font-family: var(--font-display);
  font-size: 20px;
  margin-bottom: 14px;
}

.h5store-error h1 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
}

.h5store-error p {
  margin: 8px 0 20px;
  font-size: 11px;
  color: var(--muted);
}
</style>
