<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MiniProgramSheet from '@/components/MiniProgramSheet.vue'
import { getHome } from '@/api/site'
import { IMAGES } from '@/lib/assets'
import { discountText, formatDate, parseJsonArray, thresholdText } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import type { Coupon, MemberLevel } from '@/types'

const { t, isEn } = useLang()

const coupons = ref<Coupon[]>([])
const levels = ref<MemberLevel[]>([])
const loading = ref(true)
const error = ref('')
const sheetOpen = ref(false)

const newcomerCoupons = computed(() => {
  const matched = coupons.value.filter((coupon) => /新人|新客/.test(coupon.name))
  return matched.length ? matched : coupons.value.slice(0, 2)
})

const bestCoupon = computed(() => {
  const list = newcomerCoupons.value
  if (!list.length) return null
  return [...list].sort((a, b) => Number(b.discountAmount) - Number(a.discountAmount))[0]
})

const heroValue = computed(() => (bestCoupon.value ? discountText(bestCoupon.value) : '¥0'))

const steps = computed(() => [
  {
    title: isEn.value ? 'Open the mini program' : '打开微信小程序',
    desc: isEn.value ? 'Search “Yuexiang Mart” in WeChat.' : '微信搜索「悦享便利」小程序。'
  },
  {
    title: isEn.value ? 'Join as a member' : '注册成为会员',
    desc: isEn.value ? 'Sign in with your phone number in seconds.' : '手机号一键注册，立即成为会员。'
  },
  {
    title: isEn.value ? 'Claim your gift' : '领取新人券包',
    desc: isEn.value ? 'Coupons appear in your wallet automatically.' : '券包自动进入账户，下单时直接抵扣。'
  }
])

function benefits(level: MemberLevel): string[] {
  return parseJsonArray<string>(level.benefits)
}

function levelRate(level: MemberLevel): string {
  const rate = Number(level.discount)
  if (!Number.isFinite(rate) || rate >= 10) return isEn.value ? 'Full price' : '原价'
  return `${Number(rate.toFixed(1))} 折`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const home = await getHome()
    coupons.value = home.coupons || []
    levels.value = home.levels || []
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

useSeo(() => ({
  title: isEn.value ? 'Newcomer gift — Yuexiang Mart' : '新人礼 — 悦享便利',
  description: isEn.value
    ? 'Join Yuexiang Mart and claim your newcomer coupon pack for online orders or in-store use.'
    : '悦享便利新人专享券包，注册会员即可领取，线上点单或到店核销。'
}))

onMounted(load)
</script>

<template>
  <div class="h5-page newcomer-page">
    <section class="h5-hero newcomer-hero">
      <div class="h5-hero__media" :style="{ backgroundImage: `url(${IMAGES.coffee})` }"></div>
      <div class="newcomer-hero__top">
        <span class="newcomer-hero__badge">WELCOME GIFT</span>
        <span class="newcomer-hero__edition">FIRST VISIT</span>
      </div>
      <h1>{{ t('h5.newcomerTitle') }}</h1>
      <p>{{ t('h5.newcomerSub') }}</p>
      <div class="newcomer-hero__value">
        <span>{{ t('h5.newcomerHero') }}</span>
        <strong>{{ loading ? '···' : heroValue }}</strong>
      </div>
    </section>

    <div v-if="loading" class="newcomer-loading">
      <div class="skeleton newcomer-loading__card"></div>
      <div class="skeleton newcomer-loading__card"></div>
    </div>

    <section v-else-if="error" class="h5-section">
      <div class="newcomer-error">
        <strong>{{ t('common.error') }}</strong>
        <p>{{ error }}</p>
        <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
      </div>
    </section>

    <template v-else>
      <section class="h5-section">
        <div class="h5-section__head">
          <h2>{{ isEn ? 'Your coupon pack' : '新人券包' }}</h2>
          <span class="h5-muted">{{ newcomerCoupons.length }} {{ isEn ? 'coupons' : '张券' }}</span>
        </div>

        <div v-if="newcomerCoupons.length" class="gift-pack">
          <article v-for="coupon in newcomerCoupons" :key="coupon.id" class="gift-coupon">
            <div class="gift-coupon__value">
              <strong>{{ discountText(coupon) }}</strong>
              <span>{{ thresholdText(coupon) }}</span>
            </div>
            <div class="gift-coupon__body">
              <h3>{{ coupon.name }}</h3>
              <p>{{ isEn ? 'Valid until' : '有效期至' }} {{ formatDate(coupon.validTo) }}</p>
              <p v-if="coupon.totalQty" class="gift-coupon__qty">
                {{ isEn ? 'Limited' : '限量' }} {{ coupon.totalQty }} ·
                {{ isEn ? 'per user' : '每人限领' }} {{ coupon.perUserLimit || 1 }}
              </p>
            </div>
            <button type="button" class="gift-coupon__btn" @click="sheetOpen = true">
              {{ t('h5.claim') }}
            </button>
          </article>
        </div>

        <div v-else class="newcomer-empty">
          <strong>{{ t('common.empty') }}</strong>
          <p>{{ isEn ? 'Newcomer coupons are being prepared.' : '新人券正在准备中，请稍后再来。' }}</p>
        </div>

        <p class="newcomer-note">
          {{ t('h5.newcomerDesc') }}
        </p>

        <button type="button" class="btn btn--block newcomer-cta" @click="sheetOpen = true">
          {{ t('h5.claimTip') }}
          <svg width="13" height="13" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </button>
      </section>

      <section class="h5-section">
        <div class="h5-section__head">
          <h2>{{ isEn ? 'How to claim' : '领取方式' }}</h2>
        </div>
        <ol class="steps">
          <li v-for="(step, index) in steps" :key="step.title">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <h3>{{ step.title }}</h3>
              <p>{{ step.desc }}</p>
            </div>
          </li>
        </ol>
      </section>

      <section v-if="levels.length" class="h5-section">
        <div class="h5-section__head">
          <h2>{{ isEn ? 'Member tiers' : '会员等级' }}</h2>
          <span class="h5-muted">{{ isEn ? 'More visits, more perks' : '常来，常新' }}</span>
        </div>
        <div class="level-list">
          <article v-for="level in levels" :key="level.id" class="level-card">
            <div class="level-card__top">
              <span class="level-card__code">{{ level.code }}</span>
              <strong>{{ level.name }}</strong>
              <span class="level-card__discount">{{ levelRate(level) }}</span>
            </div>
            <p v-if="level.minPoints" class="level-card__points">
              {{ isEn ? 'From' : '满' }} {{ level.minPoints }} {{ isEn ? 'points' : '积分' }}
            </p>
            <div class="level-card__benefits">
              <span v-for="item in benefits(level)" :key="item">{{ item }}</span>
            </div>
          </article>
        </div>
      </section>

      <section class="h5-section newcomer-foot">
        <p>YUEXIANG MART · WELCOME</p>
        <span>{{ isEn ? 'One gift per new member.' : '每位新会员限领一次，具体以小程序规则为准。' }}</span>
      </section>
    </template>

    <MiniProgramSheet :open="sheetOpen" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
.newcomer-page {
  background: var(--paper);
}

.newcomer-hero h1 {
  font-size: 46px;
}

.newcomer-hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.newcomer-hero__badge {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--tomato);
  color: var(--cream);
  font-size: 8px;
  letter-spacing: 0.16em;
}

.newcomer-hero__edition {
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.18em;
  color: rgba(246, 240, 228, 0.5);
}

.newcomer-hero__value {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(246, 240, 228, 0.18);
}

.newcomer-hero__value span {
  font-size: 9px;
  letter-spacing: 0.14em;
  color: rgba(246, 240, 228, 0.5);
}

.newcomer-hero__value strong {
  font-family: var(--font-display);
  font-size: 40px;
  font-weight: 400;
  line-height: 0.9;
  color: var(--cream);
}

.newcomer-loading {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 24px;
}

.newcomer-loading__card {
  height: 100px;
}

.newcomer-error,
.newcomer-empty {
  padding: 34px 22px;
  border: 1px dashed var(--line);
  border-radius: 18px;
  text-align: center;
}

.newcomer-error strong,
.newcomer-empty strong {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 400;
}

.newcomer-error p,
.newcomer-empty p {
  margin: 6px 0 14px;
  font-size: 11px;
  color: var(--muted);
}

.gift-pack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gift-coupon {
  position: relative;
  display: flex;
  align-items: stretch;
  background: var(--white);
  border: 1px solid var(--line);
  border-radius: 16px;
  overflow: hidden;
}

.gift-coupon::before,
.gift-coupon::after {
  content: "";
  position: absolute;
  left: 96px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--paper);
  border: 1px solid var(--line);
}

.gift-coupon::before {
  top: -7px;
}

.gift-coupon::after {
  bottom: -7px;
}

.gift-coupon__value {
  width: 104px;
  flex-shrink: 0;
  background: var(--cream);
  border-right: 1px dashed var(--line);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 16px 8px;
}

.gift-coupon__value strong {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 400;
  color: var(--tomato);
  line-height: 1;
  white-space: nowrap;
}

.gift-coupon__value span {
  font-size: 8px;
  color: var(--muted);
}

.gift-coupon__body {
  flex: 1;
  padding: 14px 12px;
}

.gift-coupon__body h3 {
  font-size: 14px;
  line-height: 1.35;
}

.gift-coupon__body p {
  margin-top: 3px;
  font-size: 9px;
  color: var(--muted);
}

.gift-coupon__qty {
  opacity: 0.8;
}

.gift-coupon__btn {
  align-self: center;
  margin-right: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--green);
  color: var(--cream);
  font-size: 10px;
  white-space: nowrap;
}

.newcomer-note {
  margin-top: 14px;
  font-size: 10px;
  line-height: 1.8;
  color: var(--muted);
}

.newcomer-cta {
  margin-top: 20px;
  min-height: 46px;
  font-size: 11px;
}

.steps {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--line);
}

.steps li {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.steps li > span {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 0.1em;
  color: var(--tomato);
  padding-top: 2px;
}

.steps h3 {
  font-size: 14px;
  font-weight: 500;
}

.steps p {
  margin-top: 3px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--muted);
}

.level-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.level-card {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--white);
}

.level-card__top {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.level-card__code {
  font-family: var(--font-display);
  font-size: 8px;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.level-card__top strong {
  font-size: 15px;
  font-weight: 500;
}

.level-card__discount {
  margin-left: auto;
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--tomato);
}

.level-card__points {
  font-size: 9px;
  color: var(--muted);
  margin-top: 2px;
}

.level-card__benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.level-card__benefits span {
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(16, 37, 29, 0.06);
  font-size: 9px;
  color: var(--ink-soft);
}

.newcomer-foot {
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.newcomer-foot p {
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--muted);
}

.newcomer-foot span {
  display: block;
  margin-top: 4px;
  font-size: 9px;
  color: var(--muted);
}
</style>
