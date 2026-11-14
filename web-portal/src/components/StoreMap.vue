<script setup lang="ts">
import { computed } from 'vue'
import { amapUri, numberValue, telUri, typeLabel } from '@/lib/format'
import { useLang } from '@/i18n'
import type { Store } from '@/types'

const props = withDefaults(
  defineProps<{
    stores: Store[]
    selectedId?: number | null
    height?: string
    compact?: boolean
    showCard?: boolean
    showNote?: boolean
  }>(),
  {
    selectedId: null,
    height: '540px',
    compact: false,
    showCard: true,
    showNote: true
  }
)

const emit = defineEmits<{ (event: 'select', id: number): void }>()
const { t } = useLang()

const pinStores = computed(() =>
  props.stores.filter(
    (store) => Number.isFinite(numberValue(store.lng)) && Number.isFinite(numberValue(store.lat)) && Number(store.lng) !== 0 && Number(store.lat) !== 0
  )
)

const bounds = computed(() => {
  if (!pinStores.value.length) {
    return { minLng: 121.42, maxLng: 121.5, minLat: 31.15, maxLat: 31.26 }
  }
  const lngs = pinStores.value.map((store) => numberValue(store.lng))
  const lats = pinStores.value.map((store) => numberValue(store.lat))
  let minLng = Math.min(...lngs)
  let maxLng = Math.max(...lngs)
  let minLat = Math.min(...lats)
  let maxLat = Math.max(...lats)
  const lngRange = maxLng - minLng
  const latRange = maxLat - minLat
  if (lngRange < 0.008) {
    minLng -= 0.004
    maxLng += 0.004
  }
  if (latRange < 0.006) {
    minLat -= 0.003
    maxLat += 0.003
  }
  return { minLng, maxLng, minLat, maxLat }
})

function pinStyle(store: Store) {
  const { minLng, maxLng, minLat, maxLat } = bounds.value
  const lng = numberValue(store.lng)
  const lat = numberValue(store.lat)
  const x = 9 + ((lng - minLng) / (maxLng - minLng || 1)) * 82
  const y = 9 + (1 - (lat - minLat) / (maxLat - minLat || 1)) * 82
  return { left: `${x}%`, top: `${y}%` }
}

const selected = computed(
  () => pinStores.value.find((store) => store.id === props.selectedId) || null
)
</script>

<template>
  <div
    class="store-map"
    :class="{ 'is-compact': compact, 'has-card': showCard && selected }"
    :style="{ '--map-h': height }"
  >
    <div class="store-map__canvas">
      <svg class="store-map__water" viewBox="0 0 600 400" preserveAspectRatio="none" aria-hidden="true">
        <path
          d="M-40 300 C 90 250, 150 330, 260 290 S 430 200, 660 250"
          fill="none"
          stroke="rgba(93, 135, 148, 0.18)"
          stroke-width="46"
        />
      </svg>

      <div class="store-map__road store-map__road--a" aria-hidden="true"></div>
      <div class="store-map__road store-map__road--b" aria-hidden="true"></div>
      <div class="store-map__road store-map__road--c" aria-hidden="true"></div>
      <div class="store-map__block store-map__block--a" aria-hidden="true"></div>
      <div class="store-map__block store-map__block--b" aria-hidden="true"></div>
      <div class="store-map__block store-map__block--c" aria-hidden="true"></div>

      <span class="store-map__grid-label store-map__grid-label--tl">31°N</span>
      <span class="store-map__grid-label store-map__grid-label--br">121°E</span>

      <button
        v-for="store in pinStores"
        :key="store.id"
        type="button"
        class="store-map__pin"
        :class="{ 'is-active': store.id === selectedId }"
        :style="pinStyle(store)"
        :aria-label="store.name"
        @click="emit('select', store.id)"
      >
        <span class="store-map__pin-dot"></span>
        <span class="store-map__pin-label">{{ store.name }}</span>
      </button>

      <div class="store-map__compass" aria-hidden="true">
        <span>N</span>
        <svg width="18" height="26" viewBox="0 0 18 26" fill="none">
          <path d="M9 1 16 22 9 17 2 22Z" stroke="currentColor" stroke-width="1" />
        </svg>
      </div>

      <div v-if="!pinStores.length" class="store-map__empty">
        {{ t('stores.empty') }}
      </div>
    </div>

    <div v-if="showCard && selected" class="store-map__card">
      <div class="store-map__card-top">
        <span class="tag">{{ typeLabel(selected.type) }}</span>
        <span class="store-map__card-region">{{ selected.region }}</span>
      </div>
      <h3>{{ selected.name }}</h3>
      <p class="store-map__address">{{ selected.address }}</p>
      <div class="store-map__meta">
        <span>{{ selected.businessHours }}</span>
        <span>{{ selected.phone }}</span>
      </div>
      <div class="store-map__actions">
        <a
          :href="amapUri(selected)"
          target="_blank"
          rel="noopener noreferrer"
          class="btn btn--sm btn--ghost"
        >
          {{ t('common.navigate') }}
          <svg width="12" height="12" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </a>
        <a :href="telUri(selected.phone)" class="btn btn--sm btn--ghost">
          {{ t('common.call') }}
        </a>
        <router-link :to="`/h5/store/${selected.id}`" class="btn btn--sm">
          {{ t('common.more') }}
        </router-link>
      </div>
    </div>

    <div v-else-if="showNote" class="store-map__hint">
      <span class="store-map__hint-dot"></span>
      {{ t('stores.pickHint') }}
    </div>

    <span v-if="showNote" class="store-map__note">{{ t('stores.mapNote') }}</span>
  </div>
</template>

<style scoped>
.store-map {
  position: relative;
  height: var(--map-h);
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--line);
  background: #e9e1d2;
}

.store-map__canvas {
  position: absolute;
  inset: 0;
  background-color: #e9e1d2;
  background-image: linear-gradient(rgba(16, 37, 29, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16, 37, 29, 0.055) 1px, transparent 1px);
  background-size: 58px 58px;
}

.store-map__water {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.store-map__road {
  position: absolute;
  background: rgba(251, 248, 241, 0.9);
  box-shadow: 0 0 0 1px rgba(16, 37, 29, 0.06);
}

.store-map__road--a {
  width: 140%;
  height: 18px;
  left: -20%;
  top: 38%;
  transform: rotate(-9deg);
}

.store-map__road--b {
  width: 18px;
  height: 140%;
  left: 61%;
  top: -20%;
  transform: rotate(14deg);
}

.store-map__road--c {
  width: 120%;
  height: 10px;
  left: -10%;
  top: 72%;
  transform: rotate(6deg);
}

.store-map__block {
  position: absolute;
  background: rgba(251, 248, 241, 0.42);
  border: 1px solid rgba(16, 37, 29, 0.045);
  border-radius: 4px;
}

.store-map__block--a {
  width: 18%;
  height: 13%;
  left: 10%;
  top: 16%;
  transform: rotate(-9deg);
}

.store-map__block--b {
  width: 24%;
  height: 16%;
  right: 12%;
  top: 18%;
  transform: rotate(8deg);
}

.store-map__block--c {
  width: 20%;
  height: 12%;
  left: 24%;
  bottom: 12%;
  transform: rotate(-5deg);
}

.store-map__grid-label {
  position: absolute;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: rgba(16, 37, 29, 0.32);
}

.store-map__grid-label--tl {
  top: 18px;
  left: 20px;
}

.store-map__grid-label--br {
  bottom: 18px;
  right: 20px;
}

.store-map__pin {
  position: absolute;
  z-index: 3;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.store-map__pin-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--green);
  border: 3px solid var(--cream);
  box-shadow: 0 2px 8px rgba(16, 37, 29, 0.28);
  position: relative;
  transition: transform 0.3s ease, background 0.3s ease;
}

.store-map__pin-dot::after {
  content: "";
  position: absolute;
  inset: -9px;
  border-radius: 50%;
  border: 1px solid rgba(20, 61, 47, 0.35);
  animation: pin-pulse 2.8s infinite;
}

.store-map__pin:hover .store-map__pin-dot,
.store-map__pin.is-active .store-map__pin-dot {
  background: var(--tomato);
  transform: scale(1.14);
}

.store-map__pin.is-active .store-map__pin-dot::after {
  border-color: rgba(214, 82, 47, 0.5);
}

.store-map__pin-label {
  display: none;
}

.store-map__compass {
  position: absolute;
  right: 18px;
  top: 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  color: rgba(16, 37, 29, 0.38);
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.1em;
}

.store-map__card {
  position: absolute;
  right: 18px;
  bottom: 18px;
  width: min(300px, calc(100% - 36px));
  padding: 18px;
  border-radius: 16px;
  background: rgba(251, 248, 241, 0.96);
  backdrop-filter: blur(8px);
  box-shadow: 0 16px 44px rgba(16, 37, 29, 0.18);
  border: 1px solid rgba(16, 37, 29, 0.08);
  z-index: 5;
}

.store-map__card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.store-map__card-region {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.store-map__card h3 {
  font-size: 18px;
  margin: 10px 0 4px;
  line-height: 1.3;
}

.store-map__address {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.6;
}

.store-map__meta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 11px;
  color: var(--ink-soft);
}

.store-map__actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.store-map__actions .btn {
  flex: 1;
  min-height: 36px;
  padding: 0 12px;
  font-size: 11px;
}

.store-map__hint {
  position: absolute;
  left: 18px;
  bottom: 18px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(251, 248, 241, 0.88);
  font-size: 11px;
  color: var(--ink-soft);
  backdrop-filter: blur(6px);
}

.store-map__hint-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--tomato);
}

.store-map__note {
  position: absolute;
  right: 18px;
  bottom: 18px;
  z-index: 4;
  font-size: 9px;
  letter-spacing: 0.1em;
  color: rgba(16, 37, 29, 0.4);
  text-transform: uppercase;
}

.store-map__empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--muted);
  font-size: 13px;
}

.store-map.has-card .store-map__note {
  right: auto;
  left: 18px;
}

.is-compact {
  border-radius: 18px;
}

.is-compact .store-map__card,
.is-compact .store-map__hint,
.is-compact .store-map__note {
  display: none;
}

.is-compact .store-map__pin-label {
  display: none;
}

@keyframes pin-pulse {
  0% {
    transform: scale(0.7);
    opacity: 0.7;
  }
  70% {
    transform: scale(1.35);
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}

@media (max-width: 720px) {
  .store-map {
    height: min(var(--map-h), 420px);
  }

  .store-map__card {
    padding: 14px;
  }

  .store-map__actions .btn {
    padding: 0 8px;
  }
}
</style>
