<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import StoreMap from '@/components/StoreMap.vue'
import SmartImage from '@/components/SmartImage.vue'
import { getStores } from '@/api/site'
import { IMAGES } from '@/lib/assets'
import { amapUri, telUri, typeLabel } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import type { Store } from '@/types'

const route = useRoute()
const { t } = useLang()

const allStores = ref<Store[]>([])
const stores = ref<Store[]>([])
const regions = ref<string[]>([])
const region = ref((route.query.region as string) || '')
const keyword = ref('')
const loading = ref(true)
const error = ref('')
const selectedId = ref<number | null>(null)
let timer: number | undefined
let initialized = false

const selected = computed(() => stores.value.find((store) => store.id === selectedId.value) || null)
const totalLabel = computed(() => `${stores.value.length} ${t('stores.result')}`)

async function initialLoad() {
  loading.value = true
  error.value = ''
  try {
    const data = await getStores()
    allStores.value = data
    regions.value = [...new Set(data.map((store) => store.region).filter(Boolean))]
    stores.value = data
    selectedId.value = data[0]?.id ?? null
    initialized = true
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function applyFilters() {
  if (!initialized) return
  loading.value = true
  error.value = ''
  try {
    const data = await getStores({ region: region.value, keyword: keyword.value.trim() })
    stores.value = data
    if (!data.some((store) => store.id === selectedId.value)) {
      selectedId.value = data[0]?.id ?? null
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function scheduleApply() {
  if (timer) window.clearTimeout(timer)
  timer = window.setTimeout(applyFilters, 320)
}

function resetFilters() {
  region.value = ''
  keyword.value = ''
  applyFilters()
}

function selectStore(id: number) {
  selectedId.value = id
}

watch([region, keyword], scheduleApply)
watch(
  () => route.query.region,
  (value) => {
    if (typeof value === 'string' && value !== region.value) {
      region.value = value
    }
  }
)

useSeo(() => ({
  title: t('seo.storesTitle'),
  description: t('seo.storesDesc')
}))

onMounted(initialLoad)
</script>

<template>
  <div class="stores-page">
    <section class="page-head">
      <div class="container page-head__inner">
        <div>
          <p class="eyebrow reveal" v-reveal>{{ t('stores.eyebrow') }}</p>
          <h1 class="display display--lg reveal" v-reveal="60">{{ t('stores.title') }}</h1>
        </div>
        <div class="page-head__meta reveal" v-reveal="140">
          <p>{{ t('stores.desc') }}</p>
          <span class="page-head__count">
            <strong>{{ loading ? '—' : String(stores.length).padStart(2, '0') }}</strong>
            {{ t('stores.result') }}
          </span>
        </div>
      </div>
    </section>

    <section class="section--tight locator">
      <div class="container locator__grid">
        <aside class="filters reveal" v-reveal>
          <div class="filters__head">
            <p class="eyebrow">FILTER</p>
            <button type="button" class="filters__reset" @click="resetFilters">
              {{ t('stores.reset') }}
            </button>
          </div>

          <div class="filters__group">
            <span class="filters__label">{{ t('stores.city') }}</span>
            <div class="filters__chips">
              <button
                type="button"
                class="chip"
                :class="{ 'is-active': region === '' }"
                @click="region = ''"
              >
                {{ t('stores.allCities') }}
              </button>
              <button
                v-for="item in regions"
                :key="item"
                type="button"
                class="chip"
                :class="{ 'is-active': region === item }"
                @click="region = item"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div class="filters__group">
            <label class="filters__label" for="store-keyword">{{ t('stores.keyword') }}</label>
            <div class="filters__search">
              <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.2" />
                <path d="m11 11 3.4 3.4" stroke="currentColor" stroke-width="1.2" />
              </svg>
              <input
                id="store-keyword"
                v-model="keyword"
                class="input"
                type="search"
                :placeholder="t('stores.keywordPlaceholder')"
              />
            </div>
          </div>

          <div class="filters__note">
            <span class="filters__note-dot"></span>
            {{ t('stores.mapNote') }}
          </div>
        </aside>

        <div class="results">
          <div class="results__head">
            <span>{{ loading ? t('common.loading') : totalLabel }}</span>
            <span v-if="selected" class="results__selected">
              {{ t('stores.selected') }} · {{ selected.region }}
            </span>
          </div>

          <div v-if="error" class="error-state">
            <strong>{{ t('common.error') }}</strong>
            <p>{{ error }}</p>
            <button type="button" class="btn btn--sm" @click="initialLoad">{{ t('common.retry') }}</button>
          </div>

          <div v-else-if="loading" class="results__list">
            <div v-for="i in 3" :key="i" class="skeleton results__skeleton"></div>
          </div>

          <div v-else-if="!stores.length" class="empty-state">
            <strong>{{ t('stores.empty') }}</strong>
            <button type="button" class="btn btn--sm" @click="resetFilters">{{ t('stores.reset') }}</button>
          </div>

          <div v-else class="results__list">
            <article
              v-for="(store, index) in stores"
              :key="store.id"
              class="store-item"
              :class="{ 'is-active': store.id === selectedId }"
              @click="selectStore(store.id)"
            >
              <div class="store-item__media">
                <SmartImage :src="IMAGES.store" :alt="store.name" ratio="1 / 1" art="店" label="STORE" />
                <span class="store-item__index">{{ String(index + 1).padStart(2, '0') }}</span>
              </div>
              <div class="store-item__body">
                <div class="store-item__top">
                  <span class="tag">{{ typeLabel(store.type) }}</span>
                  <span class="store-item__region">{{ store.region }}</span>
                  <span v-if="store.deliveryEnabled" class="tag tag--tomato">{{ t('stores.delivery') }}</span>
                </div>
                <h2>{{ store.name }}</h2>
                <p class="store-item__address">{{ store.address }}</p>
                <div class="store-item__meta">
                  <span>{{ t('stores.hours') }} · {{ store.businessHours }}</span>
                  <span>{{ store.phone }}</span>
                </div>
                <div class="store-item__actions" @click.stop>
                  <a
                    :href="amapUri(store)"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="btn btn--sm btn--ghost"
                  >
                    {{ t('common.navigate') }}
                    <svg width="12" height="12" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                      <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
                    </svg>
                  </a>
                  <a :href="telUri(store.phone)" class="btn btn--sm btn--ghost">
                    {{ t('common.call') }}
                  </a>
                  <router-link :to="`/h5/store/${store.id}`" class="btn btn--sm">
                    {{ t('stores.share') }}
                  </router-link>
                </div>
              </div>
            </article>
          </div>
        </div>

        <aside class="map-col reveal" v-reveal="120">
          <StoreMap
            :stores="stores"
            :selected-id="selectedId"
            height="calc(100vh - 190px)"
            @select="selectStore"
          />
          <div class="map-col__foot">
            <span>LNG / LAT</span>
            <span>{{ selected?.lng || '—' }}, {{ selected?.lat || '—' }}</span>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stores-page {
  background: var(--paper);
}

.page-head {
  padding: 76px 0 64px;
  background: var(--cream);
  border-bottom: 1px solid var(--line);
}

.page-head__inner {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 70px;
}

.page-head h1 {
  margin-top: 18px;
}

.page-head__meta {
  max-width: 380px;
  padding-bottom: 6px;
}

.page-head__meta p {
  font-size: 13px;
  line-height: 1.9;
  color: var(--muted);
}

.page-head__count {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 20px;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--muted);
  border-top: 1px solid var(--line);
  padding-top: 12px;
}

.page-head__count strong {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 400;
  color: var(--ink);
  line-height: 1;
}

.locator {
  padding: 48px 0 100px;
}

.locator__grid {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) minmax(380px, 0.95fr);
  gap: 36px;
  align-items: start;
}

.filters {
  position: sticky;
  top: calc(var(--header-h) + 28px);
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--cream);
}

.filters__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.filters__head .eyebrow::before {
  display: none;
}

.filters__reset {
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--muted);
  border-bottom: 1px solid var(--line);
  padding-bottom: 2px;
  transition: color 0.25s ease;
}

.filters__reset:hover {
  color: var(--tomato);
}

.filters__group {
  padding: 20px 0;
  border-bottom: 1px solid var(--line);
}

.filters__label {
  display: block;
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 12px;
}

.filters__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filters__chips .chip {
  min-height: 32px;
  padding: 0 12px;
  font-size: 11px;
}

.filters__search {
  position: relative;
}

.filters__search svg {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--muted);
}

.filters__search .input {
  min-height: 44px;
  padding-left: 38px;
  font-size: 13px;
  background: var(--white);
}

.filters__note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 18px;
  font-size: 10px;
  line-height: 1.7;
  color: var(--muted);
}

.filters__note-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--tomato);
  margin-top: 6px;
  flex-shrink: 0;
}

.results__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--muted);
}

.results__selected {
  color: var(--tomato);
}

.results__list {
  display: flex;
  flex-direction: column;
}

.results__skeleton {
  height: 178px;
  margin-top: 16px;
  border-radius: 18px;
}

.store-item {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 20px;
  padding: 20px 0;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: background 0.35s ease, padding 0.35s ease;
}

.store-item:hover,
.store-item.is-active {
  background: rgba(239, 230, 213, 0.5);
}

.store-item__media {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
}

.store-item__index {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(251, 248, 241, 0.9);
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--ink);
}

.store-item__top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.store-item__region {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--muted);
}

.store-item h2 {
  font-size: 21px;
  margin: 8px 0 4px;
  line-height: 1.35;
}

.store-item__address {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.7;
}

.store-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--ink-soft);
}

.store-item__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.store-item__actions .btn {
  min-height: 34px;
  padding: 0 12px;
  font-size: 10px;
}

.map-col {
  position: sticky;
  top: calc(var(--header-h) + 28px);
}

.map-col :deep(.store-map) {
  min-height: 460px;
}

.map-col__foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--muted);
}

@media (max-width: 1280px) {
  .locator__grid {
    grid-template-columns: 230px minmax(0, 1fr);
  }

  .map-col {
    position: relative;
    top: 0;
    grid-column: span 2;
  }

  .map-col :deep(.store-map) {
    height: 420px !important;
    min-height: 0;
  }
}

@media (max-width: 860px) {
  .page-head__inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 26px;
  }

  .locator__grid {
    grid-template-columns: 1fr;
    gap: 26px;
  }

  .filters,
  .map-col {
    position: relative;
    top: 0;
    grid-column: auto;
  }

  .filters {
    padding: 18px;
  }

  .map-col {
    order: 2;
  }

  .results {
    order: 3;
  }

  .filters {
    order: 1;
  }

  .store-item {
    grid-template-columns: 96px minmax(0, 1fr);
    gap: 14px;
  }

  .store-item h2 {
    font-size: 18px;
  }
}

@media (max-width: 520px) {
  .page-head {
    padding: 54px 0 44px;
  }

  .store-item {
    grid-template-columns: 1fr;
  }

  .store-item__media {
    max-width: 140px;
  }
}
</style>
