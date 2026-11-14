<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import MiniProgramSheet from '@/components/MiniProgramSheet.vue'
import { getHome, getPromotions } from '@/api/site'
import { IMAGES, promoItemImage } from '@/lib/assets'
import { discountText, formatDate, money, promoTypeLabel, thresholdText } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import type { Coupon, PromotionRow } from '@/types'

const { t, isEn } = useLang()

const promotions = ref<PromotionRow[]>([])
const coupons = ref<Coupon[]>([])
const loading = ref(true)
const promoError = ref('')
const sheetOpen = ref(false)
const now = ref(Date.now())
let timer: number | undefined

const validPromotions = computed(() =>
  promotions.value
    .map((row) => {
      const seen = new Map<number, PromotionRow['items'][number]>()
      for (const item of row.items || []) {
        if (!seen.has(item.skuId)) seen.set(item.skuId, item)
      }
      return { ...row, items: [...seen.values()] }
    })
    .filter((row) => row.items.length)
)

const soonestEnd = computed(() => {
  const ends = promotions.value
    .map((row) => new Date(row.promotion.endAt.replace(' ', 'T')).getTime())
    .filter((value) => Number.isFinite(value))
  return ends.length ? Math.min(...ends) : 0
})

const countdown = computed(() => {
  if (!soonestEnd.value) return ''
  const diff = soonestEnd.value - now.value
  if (diff <= 0) return '00:00:00'
  const pad = (value: number) => String(value).padStart(2, '0')
  const days = Math.floor(diff / 86400000)
  const hours = Math.floor((diff % 86400000) / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  const seconds = Math.floor((diff % 60000) / 1000)
  const dayLabel = isEn.value ? 'D' : '天'
  return days > 0
    ? `${days}${dayLabel} ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
    : `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
})

async function load() {
  loading.value = true
  promoError.value = ''
  const [promoResult, homeResult] = await Promise.allSettled([getPromotions(), getHome()])
  if (promoResult.status === 'fulfilled') {
    promotions.value = promoResult.value
  } else {
    promotions.value = []
    promoError.value = promoResult.reason?.message || t('common.error')
  }
  if (homeResult.status === 'fulfilled') {
    coupons.value = homeResult.value.coupons || []
  }
  loading.value = false
}

function openSheet() {
  sheetOpen.value = true
}

useSeo(() => ({
  title: isEn.value ? 'Limited-time deals — Yuexiang Mart' : '限时折扣 — 悦享便利',
  description: isEn.value
    ? 'Fresh food and everyday finds at limited-time prices. Claim coupons in the Yuexiang Mart mini program.'
    : '悦享便利今日鲜食与好物限时折扣，先领券再下单，微信小程序可选就近门店配送。'
}))

onMounted(() => {
  load()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="h5-page deals-page">
    <section class="h5-hero deals-hero">
      <div class="h5-hero__media" :style="{ backgroundImage: `url(${IMAGES.fresh})` }"></div>
      <div class="deals-hero__top">
        <span class="deals-hero__badge">LIMITED TIME</span>
        <span class="deals-hero__edition">NO. {{ String(promotions.length || 1).padStart(2, '0') }}</span>
      </div>
      <h1>{{ t('h5.dealsTitle') }}</h1>
      <p>{{ t('h5.dealsSub') }}</p>
      <div v-if="countdown" class="deals-hero__timer">
        <span>{{ t('h5.promoEnds') }}</span>
        <strong>{{ countdown }}</strong>
      </div>
    </section>

    <section v-if="coupons.length" class="h5-section">
      <div class="h5-section__head">
        <h2>{{ t('h5.coupons') }}</h2>
        <router-link to="/h5/newcomer" class="deals-more">{{ t('h5.navNewcomer') }} →</router-link>
      </div>
      <div class="coupon-scroll">
        <article v-for="coupon in coupons.slice(0, 5)" :key="coupon.id" class="coupon">
          <div class="coupon__value">
            <strong>{{ discountText(coupon) }}</strong>
            <span>{{ thresholdText(coupon) }}</span>
          </div>
          <div class="coupon__body">
            <h3>{{ coupon.name }}</h3>
            <p>{{ isEn ? 'Valid until' : '有效期至' }} {{ formatDate(coupon.validTo) }}</p>
            <button type="button" class="coupon__btn" @click="openSheet">
              {{ t('h5.claim') }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <div v-if="loading" class="deals-loading">
      <div class="skeleton deals-loading__hero"></div>
      <div class="skeleton deals-loading__row"></div>
      <div class="skeleton deals-loading__row"></div>
    </div>

    <section v-else-if="promoError" class="h5-section">
      <div class="deals-error">
        <span class="deals-error__mark">!</span>
        <h2>{{ t('common.error') }}</h2>
        <p>{{ promoError }}</p>
        <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
      </div>
      <p class="deals-error__hint">
        {{ isEn ? 'Coupons are still available above.' : '上方优惠券仍可查看，活动商品请稍后再试。' }}
      </p>
    </section>

    <section v-else-if="!validPromotions.length" class="h5-section">
      <div class="deals-empty">
        <strong>{{ t('common.empty') }}</strong>
        <p>{{ isEn ? 'New deals are on the way.' : '新一波活动正在准备中。' }}</p>
      </div>
    </section>

    <template v-else>
      <section v-for="row in validPromotions" :key="row.promotion.id" class="h5-section">
        <div class="promo-card">
          <header class="promo-card__head">
            <div>
              <span class="promo-card__type">{{ promoTypeLabel(row.promotion.type) }}</span>
              <h2>{{ row.promotion.name }}</h2>
              <p>
                {{ formatDate(row.promotion.startAt) }} — {{ formatDate(row.promotion.endAt) }}
              </p>
            </div>
            <span class="promo-card__seal">SALE</span>
          </header>

          <div class="promo-items">
            <article v-for="item in row.items" :key="item.skuId" class="promo-item">
              <div class="promo-item__media">
                <SmartImage
                  :src="promoItemImage(item.skuName)"
                  :alt="item.skuName"
                  ratio="1 / 1"
                  :art="item.skuName.replace(/^悦享/, '').slice(0, 1) || '鲜'"
                  label="DEAL"
                />
              </div>
              <div class="promo-item__info">
                <h3>{{ item.skuName }}</h3>
                <span class="tag tag--tomato">{{ isEn ? 'Limited price' : '限时价' }}</span>
              </div>
              <div class="promo-item__price">
                <strong>{{ money(item.promoPrice) }}</strong>
                <span>{{ money(item.originalPrice) }}</span>
              </div>
            </article>
          </div>

          <button type="button" class="btn btn--block promo-card__cta" @click="openSheet">
            {{ t('h5.claimTip') }}
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
            </svg>
          </button>
        </div>
      </section>

      <div class="deals-foot">
        <span>YUEXIANG DAILY DEALS</span>
        <span>{{ isEn ? 'Prices from live store data' : '价格来自门店实时活动数据' }}</span>
      </div>
    </template>

    <MiniProgramSheet :open="sheetOpen" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
.deals-page {
  background: var(--paper);
}

.deals-hero {
  padding-bottom: 24px;
}

.deals-hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.deals-hero__badge {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--tomato);
  color: var(--cream);
  font-size: 8px;
  letter-spacing: 0.16em;
}

.deals-hero__edition {
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.18em;
  color: rgba(246, 240, 228, 0.5);
}

.deals-hero h1 {
  font-size: 48px;
}

.deals-hero__timer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(246, 240, 228, 0.18);
}

.deals-hero__timer span {
  font-size: 9px;
  letter-spacing: 0.14em;
  color: rgba(246, 240, 228, 0.5);
}

.deals-hero__timer strong {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 400;
  letter-spacing: 0.04em;
}

.deals-more {
  font-size: 10px;
  color: var(--muted);
  letter-spacing: 0.06em;
}

.coupon-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 2px 0 10px;
  scrollbar-width: none;
  scroll-snap-type: x mandatory;
}

.coupon-scroll::-webkit-scrollbar {
  display: none;
}

.coupon {
  position: relative;
  flex: 0 0 236px;
  display: flex;
  background: var(--white);
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  scroll-snap-align: start;
}

.coupon::before,
.coupon::after {
  content: "";
  position: absolute;
  left: 84px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--paper);
  border: 1px solid var(--line);
  z-index: 2;
}

.coupon::before {
  top: -7px;
}

.coupon::after {
  bottom: -7px;
}

.coupon__value {
  width: 90px;
  flex-shrink: 0;
  background: var(--cream);
  border-right: 1px dashed var(--line);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 14px 6px;
}

.coupon__value strong {
  font-family: var(--font-display);
  font-size: 27px;
  font-weight: 400;
  color: var(--tomato);
  line-height: 1;
  white-space: nowrap;
}

.coupon__value span {
  font-size: 8px;
  color: var(--muted);
  text-align: center;
  line-height: 1.3;
}

.coupon__body {
  flex: 1;
  padding: 12px 12px 12px 14px;
  display: flex;
  flex-direction: column;
}

.coupon__body h3 {
  font-size: 13px;
  line-height: 1.35;
}

.coupon__body p {
  margin-top: 3px;
  font-size: 9px;
  color: var(--muted);
}

.coupon__btn {
  margin-top: auto;
  align-self: flex-start;
  font-size: 10px;
  color: var(--tomato);
  border-bottom: 1px solid currentColor;
  padding-bottom: 2px;
}

.deals-loading {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 30px;
}

.deals-loading__hero {
  height: 130px;
}

.deals-loading__row {
  height: 90px;
}

.deals-error,
.deals-empty {
  padding: 34px 24px;
  border: 1px dashed var(--line);
  border-radius: 18px;
  text-align: center;
}

.deals-error__mark {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(214, 82, 47, 0.1);
  color: var(--tomato);
  font-family: var(--font-display);
  font-size: 18px;
  margin-bottom: 10px;
}

.deals-error h2 {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 400;
}

.deals-error p {
  margin: 6px 0 16px;
  font-size: 11px;
  color: var(--muted);
  line-height: 1.7;
}

.deals-error__hint {
  margin-top: 10px;
  font-size: 10px;
  color: var(--muted);
  text-align: center;
}

.deals-empty strong {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 400;
}

.deals-empty p {
  margin-top: 4px;
  font-size: 11px;
  color: var(--muted);
}

.promo-card {
  background: var(--white);
  border: 1px solid var(--line);
  border-radius: 20px;
  overflow: hidden;
}

.promo-card__head {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  background: var(--cream);
  border-bottom: 1px solid var(--line);
  overflow: hidden;
}

.promo-card__type {
  display: inline-flex;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--green);
  color: var(--cream);
  font-size: 8px;
  letter-spacing: 0.1em;
}

.promo-card__head h2 {
  font-size: 20px;
  margin: 9px 0 4px;
  font-weight: 500;
}

.promo-card__head p {
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--muted);
}

.promo-card__seal {
  align-self: flex-start;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--tomato);
  border: 1px solid rgba(214, 82, 47, 0.35);
  border-radius: 999px;
  padding: 5px 9px;
  transform: rotate(6deg);
}

.promo-items {
  padding: 4px 20px;
}

.promo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
}

.promo-item:last-child {
  border-bottom: 0;
}

.promo-item__media {
  width: 62px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
}

.promo-item__info {
  flex: 1;
  min-width: 0;
}

.promo-item__info h3 {
  font-size: 14px;
  line-height: 1.4;
}

.promo-item__info .tag {
  margin-top: 5px;
}

.promo-item__price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.promo-item__price strong {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 400;
  color: var(--tomato);
}

.promo-item__price span {
  font-size: 10px;
  color: var(--muted);
  text-decoration: line-through;
}

.promo-card__cta {
  margin: 10px 20px 20px;
  width: calc(100% - 40px);
  min-height: 44px;
  font-size: 11px;
}

.deals-foot {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 30px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
  font-family: var(--font-display);
  font-size: 8px;
  letter-spacing: 0.14em;
  color: var(--muted);
}
</style>
