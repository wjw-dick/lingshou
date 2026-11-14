<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { applyFranchise, getStores } from '@/api/site'
import { IMAGES } from '@/lib/assets'
import { amapUri } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import { useSiteStore } from '@/composables/useSiteStore'
import type { Store } from '@/types'

const { t } = useLang()
const { config } = useSiteStore()

const stores = ref<Store[]>([])
const loading = ref(true)
const form = reactive({ name: '', phone: '', message: '' })
const errors = reactive<Record<string, string>>({})
const submitting = ref(false)
const submitted = ref(false)
const submitError = ref('')

const phone = computed(() => config('site.service_phone', '400-880-0001'))
const hours = computed(() => config('site.business_hours', '07:00–23:00'))
const icp = computed(() => config('site.icp', '沪ICP备2026000001号'))
const tenantName = computed(() => config('site.name', '悦享便利连锁'))

async function loadStores() {
  loading.value = true
  try {
    stores.value = (await getStores()).slice(0, 3)
  } catch {
    stores.value = []
  } finally {
    loading.value = false
  }
}

function clearErrors() {
  for (const key of Object.keys(errors)) delete errors[key]
}

function validate() {
  clearErrors()
  if (!form.name.trim()) errors.name = t('franchise.required')
  if (!form.phone.trim()) {
    errors.phone = t('franchise.required')
  } else if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    errors.phone = t('franchise.phoneInvalid')
  }
  if (!form.message.trim()) errors.message = t('franchise.required')
  return Object.keys(errors).length === 0
}

async function submit() {
  submitError.value = ''
  if (!validate()) return
  submitting.value = true
  try {
    await applyFranchise({
      company: '官网留言',
      contact: form.name.trim(),
      phone: form.phone.trim(),
      city: '在线留言',
      amount: '',
      remark: form.message.trim()
    })
    submitted.value = true
    form.name = ''
    form.phone = ''
    form.message = ''
  } catch (e) {
    submitError.value = (e as Error).message
  } finally {
    submitting.value = false
  }
}

useSeo(() => ({
  title: t('seo.contactTitle'),
  description: t('seo.contactDesc')
}))

onMounted(loadStores)
</script>

<template>
  <div class="contact-page">
    <section class="contact-hero">
      <div class="container contact-hero__inner">
        <div class="contact-hero__copy">
          <p class="eyebrow reveal" v-reveal>{{ t('contact.eyebrow') }}</p>
          <h1 class="display display--lg reveal" v-reveal="60">{{ t('contact.title') }}</h1>
          <p class="contact-hero__desc reveal" v-reveal="120">{{ t('contact.desc') }}</p>
        </div>

        <div class="contact-card reveal" v-reveal="160">
          <span class="contact-card__label">{{ t('contact.hotline') }}</span>
          <a :href="`tel:${phone.replace(/[^\d+]/g, '')}`" class="contact-card__phone">{{ phone }}</a>
          <div class="contact-card__rows">
            <div>
              <span>{{ t('contact.hours') }}</span>
              <strong>{{ hours }}</strong>
            </div>
            <div>
              <span>{{ t('footer.icp') }}</span>
              <strong>{{ icp }}</strong>
            </div>
            <div>
              <span>OPERATOR</span>
              <strong>{{ tenantName }}</strong>
            </div>
          </div>
          <div class="contact-card__mark" aria-hidden="true">Y</div>
        </div>
      </div>
    </section>

    <section class="section--tight contact-channels">
      <div class="container">
        <div class="channel-grid">
          <a :href="`tel:${phone.replace(/[^\d+]/g, '')}`" class="channel reveal" v-reveal>
            <span class="channel__index">01</span>
            <h2>{{ t('contact.hotline') }}</h2>
            <p>{{ t('contact.hotlineNote') }}</p>
            <span class="channel__value">{{ phone }}</span>
            <span class="channel__arrow">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M3 13 13 3M5 3h8v8" stroke="currentColor" stroke-width="1.2" />
              </svg>
            </span>
          </a>
          <div class="channel reveal" v-reveal="70">
            <span class="channel__index">02</span>
            <h2>{{ t('contact.hours') }}</h2>
            <p>{{ t('contact.hoursNote') }}</p>
            <span class="channel__value">{{ hours }}</span>
          </div>
          <router-link to="/franchise" class="channel reveal" v-reveal="140">
            <span class="channel__index">03</span>
            <h2>{{ t('contact.franchise') }}</h2>
            <p>{{ t('contact.franchiseNote') }}</p>
            <span class="channel__value">{{ t('franchise.formTitle') }}</span>
            <span class="channel__arrow">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M3 13 13 3M5 3h8v8" stroke="currentColor" stroke-width="1.2" />
              </svg>
            </span>
          </router-link>
        </div>
      </div>
    </section>

    <section class="section contact-stores">
      <div class="container contact-stores__inner">
        <div class="contact-stores__copy reveal" v-reveal>
          <p class="index-label">VISIT US</p>
          <h2 class="display display--md">{{ t('contact.storesTitle') }}</h2>
          <p>{{ t('home.storesDesc') }}</p>
          <router-link to="/stores" class="link-arrow">
            {{ t('home.storesCta') }}
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
            </svg>
          </router-link>
        </div>

        <div class="store-mini-list">
          <div v-if="loading" class="skeleton store-mini-skeleton"></div>
          <a
            v-for="store in stores"
            :key="store.id"
            :href="amapUri(store)"
            target="_blank"
            rel="noopener noreferrer"
            class="store-mini reveal"
            v-reveal="80"
          >
            <div class="store-mini__top">
              <span class="tag">{{ store.region }}</span>
              <span>{{ store.businessHours }}</span>
            </div>
            <h3>{{ store.name }}</h3>
            <p>{{ store.address }}</p>
            <span class="store-mini__phone">{{ store.phone }}</span>
          </a>
          <div v-if="!loading && !stores.length" class="empty-state">
            <strong>{{ t('stores.empty') }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="section message">
      <div class="container message__inner">
        <div class="message__visual reveal" v-reveal>
          <SmartImage :src="IMAGES.store" alt="悦享便利门店" ratio="4 / 5" art="店" label="NEIGHBORHOOD" />
          <div class="message__quote">
            <p>“{{ t('footer.slogan') }}”</p>
            <span>YUEXIANG MART</span>
          </div>
        </div>

        <div class="message__form-wrap reveal" v-reveal="100">
          <p class="eyebrow">MESSAGE</p>
          <h2 class="display display--md">{{ t('contact.messageTitle') }}</h2>
          <p class="message__desc">{{ t('contact.messageDesc') }}</p>

          <div v-if="submitted" class="message__success">
            <span class="message__success-icon">
              <svg width="26" height="26" viewBox="0 0 26 26" fill="none" aria-hidden="true">
                <circle cx="13" cy="13" r="12" stroke="currentColor" stroke-width="1.1" />
                <path d="m8 13 3.5 3.5L18 9" stroke="currentColor" stroke-width="1.4" />
              </svg>
            </span>
            <p>{{ t('contact.success') }}</p>
            <button type="button" class="link-arrow" @click="submitted = false">
              {{ t('contact.messageTitle') }}
            </button>
          </div>

          <form v-else class="message__form" novalidate @submit.prevent="submit">
            <div class="form-grid">
              <label class="field" :class="{ 'has-error': errors.name }">
                <span>{{ t('contact.name') }} *</span>
                <input v-model="form.name" class="input" type="text" autocomplete="name" />
                <small v-if="errors.name" class="field-error">{{ errors.name }}</small>
              </label>
              <label class="field" :class="{ 'has-error': errors.phone }">
                <span>{{ t('contact.phone') }} *</span>
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
              <label class="field span-2" :class="{ 'has-error': errors.message }">
                <span>{{ t('contact.message') }} *</span>
                <textarea
                  v-model="form.message"
                  class="textarea"
                  :placeholder="t('contact.messagePlaceholder')"
                ></textarea>
                <small v-if="errors.message" class="field-error">{{ errors.message }}</small>
              </label>
            </div>

            <p v-if="submitError" class="message__error">{{ submitError }}</p>

            <button type="submit" class="btn" :disabled="submitting">
              {{ submitting ? t('franchise.submitting') : t('contact.submit') }}
              <svg v-if="!submitting" width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
              </svg>
            </button>
          </form>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.contact-page {
  background: var(--paper);
}

.contact-hero {
  padding: 80px 0;
  background: var(--cream);
  border-bottom: 1px solid var(--line);
}

.contact-hero__inner {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 90px;
  align-items: center;
}

.contact-hero h1 {
  margin: 22px 0 24px;
  max-width: 780px;
}

.contact-hero__desc {
  max-width: 480px;
  font-size: 14px;
  line-height: 1.95;
  color: var(--muted);
}

.contact-card {
  position: relative;
  overflow: hidden;
  padding: 36px;
  border-radius: 22px;
  background: var(--green);
  color: var(--cream);
  isolation: isolate;
}

.contact-card__label {
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(246, 240, 228, 0.5);
}

.contact-card__phone {
  display: block;
  font-family: var(--font-display);
  font-size: clamp(34px, 4vw, 52px);
  letter-spacing: 0.01em;
  margin: 8px 0 26px;
  transition: color 0.25s ease;
}

.contact-card__phone:hover {
  color: var(--tomato-soft);
}

.contact-card__rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-top: 1px solid rgba(246, 240, 228, 0.16);
  padding-top: 20px;
}

.contact-card__rows div {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  font-size: 11px;
}

.contact-card__rows span {
  color: rgba(246, 240, 228, 0.44);
  letter-spacing: 0.08em;
}

.contact-card__rows strong {
  font-weight: 500;
  color: rgba(246, 240, 228, 0.82);
}

.contact-card__mark {
  position: absolute;
  right: -10px;
  bottom: -0.28em;
  font-family: var(--font-display);
  font-size: 190px;
  line-height: 1;
  color: rgba(246, 240, 228, 0.05);
  z-index: -1;
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--line);
  border-left: 1px solid var(--line);
}

.channel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 30px 28px 34px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  transition: background 0.35s ease;
}

.channel:hover {
  background: rgba(239, 230, 213, 0.5);
}

.channel__index {
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--tomato);
}

.channel h2 {
  font-size: 21px;
  margin: 34px 0 8px;
}

.channel p {
  font-size: 12px;
  line-height: 1.8;
  color: var(--muted);
  max-width: 260px;
}

.channel__value {
  margin-top: 24px;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0.01em;
}

.channel__arrow {
  position: absolute;
  right: 24px;
  bottom: 26px;
  color: var(--muted);
  transition: transform 0.3s ease, color 0.3s ease;
}

.channel:hover .channel__arrow {
  transform: translate(3px, -3px);
  color: var(--tomato);
}

.contact-stores {
  background: var(--paper);
}

.contact-stores__inner {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 90px;
  align-items: start;
}

.contact-stores__copy h2 {
  margin: 20px 0 20px;
}

.contact-stores__copy > p {
  max-width: 380px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--muted);
}

.contact-stores__copy .link-arrow {
  margin-top: 28px;
}

.store-mini-list {
  display: flex;
  flex-direction: column;
}

.store-mini {
  display: block;
  padding: 20px 0;
  border-bottom: 1px solid var(--line);
  transition: padding 0.3s ease;
}

.store-mini:first-child {
  border-top: 1px solid var(--line);
}

.store-mini:hover {
  padding-left: 8px;
}

.store-mini__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--muted);
}

.store-mini h3 {
  font-size: 20px;
  margin: 10px 0 4px;
}

.store-mini p {
  font-size: 12px;
  color: var(--muted);
}

.store-mini__phone {
  display: inline-block;
  margin-top: 8px;
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--ink-soft);
}

.store-mini-skeleton {
  height: 120px;
}

.message {
  background: var(--cream);
}

.message__inner {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 90px;
  align-items: center;
}

.message__visual {
  position: relative;
}

.message__visual :deep(.smart-image) {
  border-radius: 200px 200px 18px 18px;
}

.message__quote {
  position: absolute;
  left: 24px;
  right: 24px;
  bottom: 22px;
  color: var(--cream);
}

.message__quote p {
  font-family: var(--font-display);
  font-size: 22px;
  font-style: italic;
  line-height: 1.4;
}

.message__quote span {
  display: block;
  margin-top: 8px;
  font-size: 8px;
  letter-spacing: 0.22em;
  color: rgba(246, 240, 228, 0.5);
}

.message__form-wrap h2 {
  margin: 16px 0 14px;
}

.message__desc {
  max-width: 440px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--muted);
  margin-bottom: 28px;
}

.message__form .form-grid {
  gap: 18px;
}

.message__form .btn {
  margin-top: 24px;
}

.message__success {
  padding: 40px 0;
}

.message__success-icon {
  display: inline-flex;
  color: var(--green-2);
  margin-bottom: 14px;
}

.message__success p {
  font-family: var(--font-display);
  font-size: 24px;
  margin-bottom: 18px;
}

.message__error {
  margin-top: 14px;
  font-size: 12px;
  color: var(--tomato);
}

.has-error .input,
.has-error .textarea {
  border-color: var(--tomato);
}

@media (max-width: 1000px) {
  .contact-hero__inner,
  .contact-stores__inner,
  .message__inner {
    grid-template-columns: 1fr;
    gap: 46px;
  }

  .contact-card {
    max-width: 520px;
  }

  .channel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .contact-hero {
    padding: 56px 0;
  }

  .contact-card {
    padding: 26px;
  }

  .message__visual {
    max-width: 400px;
  }

  .message__quote p {
    font-size: 18px;
  }
}
</style>
