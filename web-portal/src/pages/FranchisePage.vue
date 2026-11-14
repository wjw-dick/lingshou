<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { applyFranchise } from '@/api/site'
import { IMAGES } from '@/lib/assets'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import { useSiteStore } from '@/composables/useSiteStore'
import type { FranchiseApplication } from '@/types'

const { t } = useLang()
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

const phone = computed(() => config('site.service_phone', '400-880-0001'))

const conditions = computed(() => [
  {
    title: t('franchise.condition1Title'),
    desc: t('franchise.condition1Desc')
  },
  {
    title: t('franchise.condition2Title'),
    desc: t('franchise.condition2Desc')
  },
  {
    title: t('franchise.condition3Title'),
    desc: t('franchise.condition3Desc')
  },
  {
    title: t('franchise.condition4Title'),
    desc: t('franchise.condition4Desc')
  }
])

const fees = computed(() => [
  { name: t('franchise.fee1'), amount: t('franchise.fee1Amount'), note: t('franchise.fee1Note') },
  { name: t('franchise.fee2'), amount: t('franchise.fee2Amount'), note: t('franchise.fee2Note') },
  { name: t('franchise.fee3'), amount: t('franchise.fee3Amount'), note: t('franchise.fee3Note') },
  { name: t('franchise.fee4'), amount: t('franchise.fee4Amount'), note: t('franchise.fee4Note') },
  { name: t('franchise.fee5'), amount: t('franchise.fee5Amount'), note: t('franchise.fee5Note') }
])

const supports = computed(() => [
  { title: t('franchise.support1'), desc: t('franchise.support1Desc') },
  { title: t('franchise.support2'), desc: t('franchise.support2Desc') },
  { title: t('franchise.support3'), desc: t('franchise.support3Desc') },
  { title: t('franchise.support4'), desc: t('franchise.support4Desc') }
])

const amountOptions = computed(() => [
  t('franchise.amount1'),
  t('franchise.amount2'),
  t('franchise.amount3'),
  t('franchise.amount4')
])

function clearErrors() {
  for (const key of Object.keys(errors)) delete errors[key]
}

function validate(): boolean {
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
    window.setTimeout(() => {
      document.getElementById('apply')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 60)
  } catch (e) {
    submitError.value = (e as Error).message
  } finally {
    submitting.value = false
  }
}

function again() {
  submitted.value = false
  clearErrors()
  submitError.value = ''
  form.company = ''
  form.contact = ''
  form.phone = ''
  form.city = ''
  form.amount = ''
  form.remark = ''
}

useSeo(() => ({
  title: t('seo.franchiseTitle'),
  description: t('seo.franchiseDesc')
}))
</script>

<template>
  <div class="franchise-page">
    <section class="franchise-hero">
      <div class="container franchise-hero__inner">
        <div class="franchise-hero__copy">
          <p class="eyebrow eyebrow--light reveal" v-reveal>{{ t('franchise.eyebrow') }}</p>
          <h1 class="display display--xl reveal" v-reveal="60">{{ t('franchise.title') }}</h1>
          <p class="franchise-hero__desc reveal" v-reveal="120">{{ t('franchise.desc') }}</p>
          <div class="franchise-hero__actions reveal" v-reveal="180">
            <a href="#apply" class="btn btn--light">
              {{ t('franchise.formTitle') }}
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M7 2v10M2 7l5 5 5-5" stroke="currentColor" stroke-width="1.3" />
              </svg>
            </a>
            <a :href="`tel:${phone.replace(/[^\d+]/g, '')}`" class="btn btn--ghost-light">
              {{ t('franchise.hotline') }} · {{ phone }}
            </a>
          </div>
          <div class="franchise-hero__facts reveal" v-reveal="240">
            <div>
              <strong>35<small>万起</small></strong>
              <span>{{ t('franchise.amount1') }}</span>
            </div>
            <div>
              <strong>40–120<small>㎡</small></strong>
              <span>{{ t('franchise.condition2Title') }}</span>
            </div>
            <div>
              <strong>5<small>年</small></strong>
              <span>{{ t('franchise.fee1Amount') }}</span>
            </div>
          </div>
        </div>

        <div class="franchise-hero__visual reveal" v-reveal="160">
          <SmartImage :src="IMAGES.franchise" alt="悦享便利加盟团队" ratio="4 / 5" art="合" label="PARTNER WITH US" />
          <div class="franchise-hero__stamp">
            <span>EST. 2018</span>
            <strong>JOIN<br />US</strong>
            <span>SHANGHAI</span>
          </div>
        </div>
      </div>
    </section>

    <section class="section conditions">
      <div class="container">
        <div class="section-head">
          <div class="section-head__title reveal" v-reveal>
            <p class="index-label">01 / PEOPLE</p>
            <h2 class="display display--md">{{ t('franchise.conditionsTitle') }}</h2>
          </div>
          <p class="section-head__desc reveal" v-reveal="80">{{ t('franchise.desc') }}</p>
        </div>

        <div class="condition-grid">
          <article
            v-for="(item, index) in conditions"
            :key="item.title"
            class="condition reveal"
            v-reveal="index * 70"
          >
            <span class="condition__index">0{{ index + 1 }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="section fees">
      <div class="container container--narrow">
        <div class="section-head">
          <div class="section-head__title reveal" v-reveal>
            <p class="index-label">02 / INVESTMENT</p>
            <h2 class="display display--md">{{ t('franchise.feeTitle') }}</h2>
          </div>
          <p class="section-head__desc reveal" v-reveal="80">{{ t('franchise.feeNote') }}</p>
        </div>

        <div class="fee-table reveal" v-reveal>
          <div class="fee-row fee-row--head">
            <span>{{ t('franchise.feeHeadItem') }}</span>
            <span>{{ t('franchise.feeHeadAmount') }}</span>
            <span>{{ t('franchise.feeHeadNote') }}</span>
          </div>
          <div v-for="(fee, index) in fees" :key="fee.name" class="fee-row">
            <span class="fee-row__name">
              <em>{{ String(index + 1).padStart(2, '0') }}</em>
              {{ fee.name }}
            </span>
            <span class="fee-row__amount">{{ fee.amount }}</span>
            <span class="fee-row__note">{{ fee.note }}</span>
          </div>
        </div>
        <p class="fee-disclaimer reveal" v-reveal="80">{{ t('franchise.feeNote') }}</p>
      </div>
    </section>

    <section class="support">
      <div class="container">
        <div class="support__head reveal" v-reveal>
          <p class="eyebrow eyebrow--light">03 / SUPPORT</p>
          <h2 class="display display--md">{{ t('franchise.supportTitle') }}</h2>
        </div>
        <div class="support-grid">
          <article
            v-for="(item, index) in supports"
            :key="item.title"
            class="support-card reveal"
            v-reveal="index * 70"
          >
            <span class="support-card__index">0{{ index + 1 }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </article>
        </div>
      </div>
    </section>

    <section id="apply" class="section apply">
      <div class="container apply__inner">
        <div class="apply__copy reveal" v-reveal>
          <p class="index-label">04 / APPLY</p>
          <h2 class="display display--lg">{{ t('franchise.formTitle') }}</h2>
          <p class="apply__desc">{{ t('franchise.formDesc') }}</p>

          <div class="apply__contact">
            <span>{{ t('franchise.hotline') }}</span>
            <a :href="`tel:${phone.replace(/[^\d+]/g, '')}`">{{ phone }}</a>
            <span>{{ t('footer.hours') }} · {{ config('site.business_hours', '07:00–23:00') }}</span>
          </div>

          <div class="apply__quote">
            <span>“</span>
            <p>{{ t('footer.slogan') }}</p>
          </div>
        </div>

        <div class="apply__form-wrap reveal" v-reveal="100">
          <Transition name="fade" mode="out-in">
            <div v-if="submitted" key="success" class="apply__success">
              <span class="apply__success-icon">
                <svg width="30" height="30" viewBox="0 0 30 30" fill="none" aria-hidden="true">
                  <circle cx="15" cy="15" r="14" stroke="currentColor" stroke-width="1.2" />
                  <path d="m9 15 4 4 8-9" stroke="currentColor" stroke-width="1.5" />
                </svg>
              </span>
              <p class="eyebrow">APPLICATION RECEIVED</p>
              <h3 class="display display--sm">{{ t('franchise.successTitle') }}</h3>
              <p>{{ t('franchise.successDesc') }}</p>
              <button type="button" class="btn btn--ghost" @click="again">
                {{ t('franchise.again') }}
              </button>
            </div>

            <form v-else key="form" class="apply__form" novalidate @submit.prevent="submit">
              <div class="form-grid">
                <label class="field span-2">
                  <span>{{ t('franchise.company') }}</span>
                  <input v-model="form.company" class="input" type="text" autocomplete="organization" />
                </label>

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

                <label class="field span-2">
                  <span>{{ t('franchise.remark') }}</span>
                  <textarea
                    v-model="form.remark"
                    class="textarea"
                    :placeholder="t('franchise.remarkPlaceholder')"
                  ></textarea>
                </label>
              </div>

              <p v-if="submitError" class="apply__error">{{ submitError }}</p>

              <div class="apply__submit">
                <button type="submit" class="btn" :disabled="submitting">
                  {{ submitting ? t('franchise.submitting') : t('franchise.submit') }}
                  <svg v-if="!submitting" width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                    <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
                  </svg>
                </button>
                <span>{{ t('franchise.privacy') }}</span>
              </div>
            </form>
          </Transition>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.franchise-page {
  background: var(--paper);
}

.franchise-hero {
  position: relative;
  padding: 86px 0 96px;
  background: var(--green);
  color: var(--cream);
  overflow: hidden;
  isolation: isolate;
}

.franchise-hero::before {
  content: "";
  position: absolute;
  left: -10%;
  bottom: -35%;
  width: 60vw;
  height: 60vw;
  border: 1px solid rgba(246, 240, 228, 0.07);
  border-radius: 50%;
  z-index: -1;
}

.franchise-hero__inner {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 90px;
  align-items: center;
}

.franchise-hero h1 {
  margin: 24px 0 28px;
  max-width: 780px;
  font-size: clamp(48px, 6.6vw, 104px);
}

.franchise-hero__desc {
  max-width: 520px;
  font-size: 15px;
  line-height: 1.95;
  color: rgba(246, 240, 228, 0.64);
}

.franchise-hero__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 34px;
}

.franchise-hero__facts {
  display: flex;
  gap: 48px;
  flex-wrap: wrap;
  margin-top: 52px;
  padding-top: 26px;
  border-top: 1px solid rgba(246, 240, 228, 0.16);
}

.franchise-hero__facts div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.franchise-hero__facts strong {
  font-family: var(--font-display);
  font-size: clamp(30px, 3vw, 44px);
  font-weight: 400;
  line-height: 1;
}

.franchise-hero__facts small {
  font-size: 12px;
  font-family: var(--font-sans);
  margin-left: 3px;
  opacity: 0.7;
}

.franchise-hero__facts span {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: rgba(246, 240, 228, 0.44);
}

.franchise-hero__visual {
  position: relative;
  max-width: 420px;
  margin-left: auto;
  width: 100%;
}

.franchise-hero__visual :deep(.smart-image) {
  border-radius: 210px 210px 16px 16px;
}

.franchise-hero__stamp {
  position: absolute;
  left: -44px;
  bottom: 34px;
  width: 132px;
  height: 132px;
  border-radius: 50%;
  background: var(--tomato);
  color: var(--cream);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  transform: rotate(-9deg);
  box-shadow: 0 18px 44px rgba(214, 82, 47, 0.3);
}

.franchise-hero__stamp span {
  font-size: 7px;
  letter-spacing: 0.2em;
  color: rgba(246, 240, 228, 0.7);
}

.franchise-hero__stamp strong {
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 400;
  line-height: 0.95;
  text-align: center;
  letter-spacing: 0.02em;
}

.conditions {
  background: var(--cream);
}

.condition-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  border-top: 1px solid var(--line);
  border-left: 1px solid var(--line);
}

.condition {
  padding: 30px 26px 34px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  transition: background 0.4s ease;
}

.condition:hover {
  background: rgba(239, 230, 213, 0.55);
}

.condition__index {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: var(--tomato);
}

.condition h3 {
  font-size: 20px;
  margin: 34px 0 10px;
}

.condition p {
  font-size: 12px;
  line-height: 1.8;
  color: var(--muted);
}

.fees {
  background: var(--paper);
}

.fee-table {
  border-top: 1px solid var(--ink);
  margin-top: 8px;
}

.fee-row {
  display: grid;
  grid-template-columns: 1.1fr 0.8fr 1.4fr;
  gap: 24px;
  align-items: center;
  padding: 20px 6px;
  border-bottom: 1px solid var(--line);
}

.fee-row--head {
  padding: 12px 6px;
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.fee-row__name {
  display: flex;
  align-items: baseline;
  gap: 14px;
  font-size: 16px;
  font-weight: 500;
}

.fee-row__name em {
  font-family: var(--font-display);
  font-size: 10px;
  font-style: normal;
  letter-spacing: 0.1em;
  color: var(--tomato);
}

.fee-row__amount {
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: -0.01em;
}

.fee-row__note {
  font-size: 12px;
  color: var(--muted);
  text-align: right;
}

.fee-disclaimer {
  margin-top: 16px;
  font-size: 10px;
  line-height: 1.7;
  color: var(--muted);
  text-align: right;
}

.support {
  background: var(--ink);
  color: var(--cream);
  padding: 100px 0;
}

.support__head h2 {
  margin-top: 18px;
}

.support-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  margin-top: 46px;
  border-top: 1px solid rgba(246, 240, 228, 0.16);
  border-left: 1px solid rgba(246, 240, 228, 0.16);
}

.support-card {
  padding: 30px 24px 38px;
  border-right: 1px solid rgba(246, 240, 228, 0.16);
  border-bottom: 1px solid rgba(246, 240, 228, 0.16);
  transition: background 0.35s ease;
}

.support-card:hover {
  background: rgba(246, 240, 228, 0.05);
}

.support-card__index {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: var(--gold);
}

.support-card h3 {
  font-size: 21px;
  margin: 30px 0 8px;
}

.support-card p {
  font-size: 12px;
  line-height: 1.8;
  color: rgba(246, 240, 228, 0.5);
}

.apply {
  background: var(--cream);
}

.apply__inner {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 90px;
  align-items: start;
}

.apply__copy h2 {
  margin: 20px 0 22px;
}

.apply__desc {
  max-width: 400px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--muted);
}

.apply__contact {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 34px;
  padding: 20px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.apply__contact span {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--muted);
  text-transform: uppercase;
}

.apply__contact a {
  font-family: var(--font-display);
  font-size: 34px;
  letter-spacing: 0.01em;
  transition: color 0.25s ease;
}

.apply__contact a:hover {
  color: var(--tomato);
}

.apply__quote {
  display: flex;
  gap: 12px;
  margin-top: 34px;
  color: var(--muted);
}

.apply__quote span {
  font-family: var(--font-display);
  font-size: 46px;
  line-height: 0.8;
  color: var(--tomato);
}

.apply__quote p {
  font-family: var(--font-display);
  font-size: 19px;
  font-style: italic;
  line-height: 1.6;
}

.apply__form-wrap {
  background: var(--white);
  border: 1px solid var(--line);
  border-radius: 22px;
  padding: 38px;
  box-shadow: 0 20px 60px rgba(16, 37, 29, 0.06);
}

.apply__form .form-grid {
  gap: 20px;
}

.apply__form .input,
.apply__form .select,
.apply__form .textarea {
  background: var(--paper);
}

.apply__submit {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 26px;
  flex-wrap: wrap;
}

.apply__submit span {
  font-size: 10px;
  line-height: 1.6;
  color: var(--muted);
  max-width: 260px;
}

.apply__error {
  margin-top: 16px;
  font-size: 12px;
  color: var(--tomato);
}

.apply__success {
  padding: 34px 10px;
  text-align: center;
}

.apply__success-icon {
  display: inline-flex;
  color: var(--green-2);
  margin-bottom: 20px;
}

.apply__success h3 {
  margin: 14px 0 12px;
}

.apply__success > p:last-of-type {
  max-width: 380px;
  margin: 0 auto 24px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--muted);
}

.has-error .input {
  border-color: var(--tomato);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1100px) {
  .franchise-hero__inner {
    gap: 56px;
  }

  .condition-grid,
  .support-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .apply__inner {
    gap: 50px;
  }
}

@media (max-width: 900px) {
  .franchise-hero__inner,
  .apply__inner {
    grid-template-columns: 1fr;
  }

  .franchise-hero__visual {
    max-width: 380px;
    margin-left: 0;
    margin-top: 20px;
  }

  .franchise-hero__stamp {
    left: auto;
    right: -18px;
  }

  .franchise-hero__facts {
    gap: 30px;
  }

  .apply__form-wrap {
    padding: 26px;
  }
}

@media (max-width: 640px) {
  .franchise-hero {
    padding: 60px 0 70px;
  }

  .franchise-hero h1 {
    font-size: clamp(42px, 12.5vw, 64px);
  }

  .condition-grid,
  .support-grid {
    grid-template-columns: 1fr;
  }

  .fee-row {
    grid-template-columns: 1fr auto;
    gap: 8px 16px;
    padding: 16px 4px;
  }

  .fee-row--head {
    display: none;
  }

  .fee-row__note {
    grid-column: span 2;
    text-align: left;
    font-size: 11px;
  }

  .fee-row__name {
    font-size: 15px;
  }

  .fee-row__amount {
    font-size: 20px;
    text-align: right;
  }

  .fee-disclaimer {
    text-align: left;
  }

  .support {
    padding: 72px 0;
  }

  .apply__form-wrap {
    padding: 22px 18px;
  }

  .apply__contact a {
    font-size: 28px;
  }
}
</style>
