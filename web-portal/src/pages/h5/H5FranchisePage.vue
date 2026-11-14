<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import MiniProgramSheet from '@/components/MiniProgramSheet.vue'
import { applyFranchise } from '@/api/site'
import { IMAGES } from '@/lib/assets'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import { useSiteStore } from '@/composables/useSiteStore'
import type { FranchiseApplication } from '@/types'

const { t, isEn } = useLang()
const { config } = useSiteStore()

const form = reactive<FranchiseApplication>({
  company: '',
  contact: '',
  phone: '',
  city: '',
  amount: '',
  remark: ''
})
const errors = reactive<Record<string, string>>({})
const submitting = ref(false)
const submitted = ref(false)
const submitError = ref('')
const sheetOpen = ref(false)

const phone = computed(() => config('site.service_phone', '400-880-0001'))
const amountOptions = computed(() => [
  t('franchise.amount1'),
  t('franchise.amount2'),
  t('franchise.amount3'),
  t('franchise.amount4')
])
const highlights = computed(() => [
  t('franchise.support1'),
  t('franchise.support2'),
  t('franchise.support3')
])

function clearErrors() {
  for (const key of Object.keys(errors)) delete errors[key]
}

function validate() {
  clearErrors()
  if (!form.contact.trim()) errors.contact = t('franchise.required')
  if (!form.phone.trim()) {
    errors.phone = t('franchise.required')
  } else if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    errors.phone = t('franchise.phoneInvalid')
  }
  if (!form.city.trim()) errors.city = t('franchise.required')
  return Object.keys(errors).length === 0
}

async function submit() {
  submitError.value = ''
  if (!validate()) return
  submitting.value = true
  try {
    await applyFranchise({
      company: form.company.trim(),
      contact: form.contact.trim(),
      phone: form.phone.trim(),
      city: form.city.trim(),
      amount: form.amount,
      remark: form.remark.trim()
    })
    submitted.value = true
  } catch (e) {
    submitError.value = (e as Error).message
  } finally {
    submitting.value = false
  }
}

function again() {
  submitted.value = false
  clearErrors()
  form.contact = ''
  form.phone = ''
  form.city = ''
  form.amount = ''
  form.remark = ''
}

useSeo(() => ({
  title: isEn.value ? 'Franchise application — Yuexiang Mart' : '加盟申请 — 悦享便利',
  description: isEn.value
    ? 'Apply to open a Yuexiang Mart neighborhood store. Site selection, supply chain and digital support included.'
    : '申请加盟悦享便利社区便利店，获得选址、供应链、数字化系统与门店培训的一体化支持。'
}))
</script>

<template>
  <div class="h5-page h5franchise-page">
    <section class="h5-hero h5franchise-hero">
      <div class="h5-hero__media" :style="{ backgroundImage: `url(${IMAGES.franchise})` }"></div>
      <div class="h5franchise-hero__top">
        <span class="h5franchise-hero__badge">FRANCHISE</span>
        <span class="h5franchise-hero__edition">EST. 2018</span>
      </div>
      <h1>{{ t('h5.franchiseTitle') }}</h1>
      <p>{{ t('h5.franchiseSub') }}</p>
      <div class="h5franchise-hero__facts">
        <div>
          <strong>35<small>万起</small></strong>
          <span>{{ isEn ? 'Investment' : '单店投资' }}</span>
        </div>
        <div>
          <strong>40–120<small>㎡</small></strong>
          <span>{{ isEn ? 'Store size' : '建议面积' }}</span>
        </div>
        <div>
          <strong>5<small>年</small></strong>
          <span>{{ isEn ? 'Contract' : '合同期' }}</span>
        </div>
      </div>
    </section>

    <section class="h5-section h5franchise-intro">
      <p class="eyebrow">{{ isEn ? 'WHAT WE PROVIDE' : '我们提供' }}</p>
      <div class="h5franchise-tags">
        <span v-for="item in highlights" :key="item">{{ item }}</span>
      </div>
      <p class="h5franchise-desc">{{ t('franchise.desc') }}</p>
    </section>

    <section class="h5-section">
      <div class="h5franchise-form">
        <div v-if="submitted" class="h5franchise-success">
          <span class="h5franchise-success__icon">
            <svg width="34" height="34" viewBox="0 0 34 34" fill="none" aria-hidden="true">
              <circle cx="17" cy="17" r="16" stroke="currentColor" stroke-width="1.2" />
              <path d="m10 17 4.5 4.5L24 11" stroke="currentColor" stroke-width="1.5" />
            </svg>
          </span>
          <h2>{{ t('franchise.successTitle') }}</h2>
          <p>{{ t('franchise.successDesc') }}</p>
          <button type="button" class="btn btn--block" @click="sheetOpen = true">
            {{ t('common.miniProgram') }}
          </button>
          <button type="button" class="h5franchise-link" @click="again">
            {{ t('franchise.again') }}
          </button>
        </div>

        <form v-else class="h5franchise-fields" novalidate @submit.prevent="submit">
          <div class="h5franchise-head">
            <p class="eyebrow">APPLICATION</p>
            <h2>{{ t('franchise.formTitle') }}</h2>
            <p>{{ t('franchise.formDesc') }}</p>
          </div>

          <label class="field" :class="{ 'has-error': errors.contact }">
            <span>{{ t('franchise.contact') }} *</span>
            <input v-model="form.contact" class="input" type="text" autocomplete="name" />
            <small v-if="errors.contact" class="field-error">{{ errors.contact }}</small>
          </label>

          <label class="field" :class="{ 'has-error': errors.phone }">
            <span>{{ t('franchise.phone') }} *</span>
            <input
              v-model="form.phone"
              class="input"
              type="tel"
              inputmode="numeric"
              maxlength="11"
              autocomplete="tel"
            />
            <small v-if="errors.phone" class="field-error">{{ errors.phone }}</small>
          </label>

          <label class="field" :class="{ 'has-error': errors.city }">
            <span>{{ t('franchise.city') }} *</span>
            <input v-model="form.city" class="input" type="text" autocomplete="address-level2" />
            <small v-if="errors.city" class="field-error">{{ errors.city }}</small>
          </label>

          <label class="field">
            <span>{{ t('franchise.amount') }}</span>
            <select v-model="form.amount" class="select">
              <option value="">{{ t('franchise.amountPlaceholder') }}</option>
              <option v-for="option in amountOptions" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>{{ t('franchise.remark') }}</span>
            <textarea
              v-model="form.remark"
              class="textarea"
              :placeholder="t('franchise.remarkPlaceholder')"
            ></textarea>
          </label>

          <p v-if="submitError" class="h5franchise-error">{{ submitError }}</p>

          <button type="submit" class="btn btn--block" :disabled="submitting">
            {{ submitting ? t('franchise.submitting') : t('franchise.submit') }}
          </button>
          <p class="h5franchise-privacy">{{ t('franchise.privacy') }}</p>
        </form>
      </div>
    </section>

    <section class="h5-section h5franchise-contact">
      <span>{{ t('franchise.hotline') }}</span>
      <a :href="`tel:${phone.replace(/[^\d+]/g, '')}`">{{ phone }}</a>
      <p>{{ config('site.business_hours', '07:00–23:00') }}</p>
    </section>

    <MiniProgramSheet :open="sheetOpen" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
.h5franchise-page {
  background: var(--paper);
}

.h5franchise-hero h1 {
  font-size: 44px;
}

.h5franchise-hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.h5franchise-hero__badge {
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--tomato);
  color: var(--cream);
  font-size: 8px;
  letter-spacing: 0.16em;
}

.h5franchise-hero__edition {
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.18em;
  color: rgba(246, 240, 228, 0.5);
}

.h5franchise-hero__facts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(246, 240, 228, 0.18);
}

.h5franchise-hero__facts div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.h5franchise-hero__facts strong {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 400;
  line-height: 1;
}

.h5franchise-hero__facts small {
  font-family: var(--font-sans);
  font-size: 9px;
  margin-left: 2px;
  opacity: 0.7;
}

.h5franchise-hero__facts span {
  font-size: 8px;
  letter-spacing: 0.06em;
  color: rgba(246, 240, 228, 0.5);
}

.h5franchise-intro .eyebrow::before {
  display: none;
}

.h5franchise-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.h5franchise-tags span {
  padding: 6px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  font-size: 10px;
  color: var(--ink-soft);
  background: var(--white);
}

.h5franchise-desc {
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
}

.h5franchise-form {
  background: var(--white);
  border: 1px solid var(--line);
  border-radius: 22px;
  padding: 22px 18px;
}

.h5franchise-head {
  margin-bottom: 20px;
}

.h5franchise-head .eyebrow::before {
  display: none;
}

.h5franchise-head h2 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
  margin: 8px 0 8px;
}

.h5franchise-head p:last-child {
  font-size: 10px;
  line-height: 1.7;
  color: var(--muted);
}

.h5franchise-fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.h5franchise-fields .input,
.h5franchise-fields .select,
.h5franchise-fields .textarea {
  min-height: 46px;
  font-size: 14px;
  background: var(--paper);
}

.h5franchise-fields .textarea {
  min-height: 96px;
}

.h5franchise-fields .btn {
  margin-top: 8px;
  min-height: 46px;
  font-size: 12px;
}

.h5franchise-privacy {
  font-size: 9px;
  line-height: 1.6;
  color: var(--muted);
  text-align: center;
}

.h5franchise-error {
  font-size: 11px;
  color: var(--tomato);
}

.h5franchise-success {
  padding: 16px 4px;
  text-align: center;
}

.h5franchise-success__icon {
  display: inline-flex;
  color: var(--green-2);
  margin-bottom: 12px;
}

.h5franchise-success h2 {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 400;
}

.h5franchise-success p {
  margin: 8px 0 20px;
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
}

.h5franchise-link {
  display: block;
  margin: 14px auto 0;
  font-size: 10px;
  color: var(--muted);
  border-bottom: 1px solid var(--line);
  padding-bottom: 2px;
}

.h5franchise-contact {
  text-align: center;
  padding: 24px 0 10px;
  border-top: 1px solid var(--line);
}

.h5franchise-contact span {
  display: block;
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--muted);
}

.h5franchise-contact a {
  display: inline-block;
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 30px;
}

.h5franchise-contact p {
  font-size: 9px;
  color: var(--muted);
  margin-top: 2px;
}

.has-error .input {
  border-color: var(--tomato);
}
</style>
