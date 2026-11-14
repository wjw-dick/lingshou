<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import MiniProgramSheet from '@/components/MiniProgramSheet.vue'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'

const route = useRoute()
const { t, isEn } = useLang()
const sheetOpen = ref(false)

const orderNo = computed(() => {
  const value = route.query.orderNo
  return Array.isArray(value) ? value[0] : value || ''
})
const rawStatus = computed(() => {
  const value = route.query.status
  return String(Array.isArray(value) ? value[0] : value || '').toLowerCase()
})
const failed = computed(() => ['fail', 'failed', 'cancel', 'cancelled'].includes(rawStatus.value))
const missing = computed(() => !orderNo.value)

const title = computed(() => {
  if (missing.value) return t('h5.payPending')
  return failed.value
    ? isEn.value
      ? 'Payment not completed'
      : '支付未完成'
    : t('h5.paySuccess')
})

useSeo(() => ({
  title: `${title.value} — 悦享便利`,
  description: isEn.value
    ? 'Check your Yuexiang Mart payment result and view your order in the mini program.'
    : '查看悦享便利支付结果，并可前往小程序订单中心查看订单详情。'
}))
</script>

<template>
  <div class="h5-page pay-page">
    <section class="pay-card">
      <span class="pay-card__mark" :class="{ 'is-fail': failed || missing }">
        <svg v-if="!failed && !missing" width="42" height="42" viewBox="0 0 42 42" fill="none" aria-hidden="true">
          <circle cx="21" cy="21" r="20" stroke="currentColor" stroke-width="1.2" />
          <path d="m12 21 6 6 12-13" stroke="currentColor" stroke-width="1.6" />
        </svg>
        <svg v-else width="42" height="42" viewBox="0 0 42 42" fill="none" aria-hidden="true">
          <circle cx="21" cy="21" r="20" stroke="currentColor" stroke-width="1.2" />
          <path d="M14 14l14 14M28 14 14 28" stroke="currentColor" stroke-width="1.5" />
        </svg>
      </span>

      <p class="eyebrow">PAYMENT RETURN</p>
      <h1>{{ title }}</h1>
      <p class="pay-card__desc">{{ t('h5.payNote') }}</p>

      <div class="pay-order">
        <span>{{ t('h5.orderNo') }}</span>
        <strong>{{ orderNo || '—' }}</strong>
        <em>{{ failed ? (isEn ? 'FAILED' : '未完成') : missing ? (isEn ? 'UNKNOWN' : '待确认') : (isEn ? 'RECEIVED' : '已受理') }}</em>
      </div>

      <div class="pay-actions">
        <button type="button" class="btn btn--block" :disabled="missing" @click="sheetOpen = true">
          {{ t('h5.viewOrder') }}
        </button>
        <router-link to="/h5/deals" class="btn btn--ghost btn--block">
          {{ t('h5.backHome') }}
        </router-link>
      </div>

      <div class="pay-card__foot">
        <span>YUEXIANG MART</span>
        <span>{{ isEn ? 'WeChat mini program orders' : '微信小程序订单' }}</span>
      </div>
    </section>

    <section class="pay-help">
      <h2>{{ isEn ? 'Need help?' : '需要帮助？' }}</h2>
      <p>
        {{ isEn
          ? 'If the payment was deducted but the order status has not updated, please contact our service team.'
          : '如已扣款但订单状态未更新，请联系悦享便利客服，我们会尽快为你处理。' }}
      </p>
      <router-link to="/contact" class="link-arrow">{{ t('nav.contact') }}</router-link>
    </section>

    <MiniProgramSheet :open="sheetOpen" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
.pay-page {
  position: relative;
  background:
    radial-gradient(circle at 50% 0%, rgba(195, 163, 106, 0.14), transparent 50%),
    var(--paper);
  padding-top: 40px;
}

.pay-card {
  position: relative;
  padding: 44px 24px 26px;
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--white);
  text-align: center;
  overflow: hidden;
}

.pay-card::before {
  content: "";
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  width: 180px;
  height: 180px;
  border: 1px solid rgba(16, 37, 29, 0.06);
  border-radius: 50%;
}

.pay-card__mark {
  position: relative;
  display: inline-flex;
  color: var(--green-2);
  margin-bottom: 18px;
}

.pay-card__mark.is-fail {
  color: var(--tomato);
}

.pay-card .eyebrow::before {
  display: none;
}

.pay-card h1 {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 400;
  letter-spacing: -0.02em;
  margin: 8px 0 12px;
}

.pay-card__desc {
  max-width: 330px;
  margin-inline: auto;
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
}

.pay-order {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 26px 0 24px;
  padding: 18px;
  border-radius: 14px;
  background: var(--cream);
  text-align: left;
}

.pay-order span {
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.pay-order strong {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 400;
  letter-spacing: 0.04em;
  word-break: break-all;
}

.pay-order em {
  position: absolute;
  top: 14px;
  right: 14px;
  font-style: normal;
  font-size: 8px;
  letter-spacing: 0.1em;
  color: var(--tomato);
}

.pay-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pay-actions .btn {
  min-height: 46px;
  font-size: 12px;
}

.pay-card__foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 26px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
  font-family: var(--font-display);
  font-size: 8px;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.pay-help {
  margin-top: 22px;
  padding: 20px;
  border: 1px dashed var(--line);
  border-radius: 16px;
}

.pay-help h2 {
  font-size: 14px;
  font-weight: 500;
}

.pay-help p {
  margin: 6px 0 10px;
  font-size: 10px;
  line-height: 1.8;
  color: var(--muted);
}

.pay-help .link-arrow {
  font-size: 10px;
}
</style>
