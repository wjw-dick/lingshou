<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SmartImage from '@/components/SmartImage.vue'
import MiniProgramSheet from '@/components/MiniProgramSheet.vue'
import { getProduct } from '@/api/site'
import { productImage } from '@/lib/assets'
import { money, shelfLifeLabel, storageLabel } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import { showToast } from '@/composables/useToast'
import type { ProductRow } from '@/types'

const route = useRoute()
const { t, isEn, pick } = useLang()

const row = ref<ProductRow | null>(null)
const loading = ref(true)
const error = ref('')
const sheetOpen = ref(false)

const spu = computed(() => row.value?.spu || null)
const sku = computed(() => row.value?.sku || null)
const title = computed(() => (spu.value ? pick(spu.value.name, spu.value.nameEn) : ''))
const subtitle = computed(() =>
  spu.value && isEn.value === false && spu.value.nameEn ? spu.value.nameEn : ''
)
const image = computed(() => productImage(spu.value))
const artText = computed(() => title.value.replace(/^悦享/, '').slice(0, 1) || '鲜')
const retail = computed(() => sku.value?.priceRetail ?? spu.value?.priceRetail)
const member = computed(() => sku.value?.priceMember ?? spu.value?.priceMember)
const hasMember = computed(
  () => Number(member.value ?? 0) > 0 && Number(member.value) < Number(retail.value)
)

const infoRows = computed(() => {
  if (!spu.value) return []
  return [
    {
      label: t('h5.ingredients'),
      value: spu.value.ingredients || (isEn.value ? 'See packaging' : '见包装')
    },
    {
      label: t('h5.allergens'),
      value: spu.value.allergens || (isEn.value ? 'None declared' : '无明确标注')
    },
    {
      label: t('h5.origin'),
      value: spu.value.origin || '—'
    },
    {
      label: t('h5.storage'),
      value: storageLabel(spu.value.storageCondition)
    },
    {
      label: t('h5.shelf'),
      value: spu.value.shelfLifeDays
        ? `${spu.value.shelfLifeDays} ${isEn.value ? 'days' : '天'}`
        : '—'
    },
    {
      label: isEn.value ? 'Spec' : '规格',
      value: [spu.value.spec, spu.value.unit].filter(Boolean).join(' · ') || '—'
    }
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    row.value = await getProduct(String(route.params.skuId))
  } catch (e) {
    error.value = (e as Error).message
    row.value = null
  } finally {
    loading.value = false
  }
}

async function share() {
  const shareData = {
    title: title.value,
    text: `${title.value} · ${isEn.value ? 'Yuexiang Mart' : '悦享便利'}`,
    url: window.location.href
  }
  if (navigator.share) {
    try {
      await navigator.share(shareData)
      return
    } catch {
      // fall through to clipboard
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
  title: spu.value ? `${title.value} — 悦享便利` : t('h5.productNotFound'),
  description:
    spu.value
      ? `${title.value}${spu.value.spec ? ` · ${spu.value.spec}` : ''}，${shelfLifeLabel(
          spu.value.shelfLifeDays
        )}，${storageLabel(spu.value.storageCondition)}。悦享便利小程序可购。`
      : t('h5.productNotFound')
}))

watch(() => route.params.skuId, load)
onMounted(load)
</script>

<template>
  <div class="h5-page h5product-page">
    <div v-if="loading" class="h5product-loading">
      <div class="skeleton h5product-loading__media"></div>
      <div class="skeleton h5product-loading__line"></div>
      <div class="skeleton h5product-loading__line h5product-loading__line--short"></div>
    </div>

    <div v-else-if="error || !row" class="h5product-error">
      <span class="h5product-error__mark">!</span>
      <h1>{{ t('h5.productNotFound') }}</h1>
      <p>{{ error }}</p>
      <router-link to="/h5/deals" class="btn btn--sm">{{ t('h5.backHome') }}</router-link>
    </div>

    <template v-else>
      <section class="h5product-media">
        <SmartImage
          :src="image"
          :alt="title"
          ratio="1 / 1"
          :art="artText"
          :label="spu?.nameEn || 'YUEXIANG SELECT'"
          eager
        />
        <div class="h5product-media__top">
          <span class="tag tag--tomato">{{ isEn ? 'ONLINE' : '线上在售' }}</span>
          <button type="button" class="h5product-share" @click="share">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <circle cx="12" cy="3" r="2" stroke="currentColor" stroke-width="1.2" />
              <circle cx="4" cy="8" r="2" stroke="currentColor" stroke-width="1.2" />
              <circle cx="12" cy="13" r="2" stroke="currentColor" stroke-width="1.2" />
              <path d="m6 7 4-3M6 9l4 3" stroke="currentColor" stroke-width="1.2" />
            </svg>
            {{ t('common.share') }}
          </button>
        </div>
      </section>

      <section class="h5product-head">
        <div class="h5product-tags">
          <span class="tag">{{ storageLabel(spu?.storageCondition) }}</span>
          <span class="tag">{{ shelfLifeLabel(spu?.shelfLifeDays) }}</span>
          <span v-if="spu?.isTobacco" class="tag tag--tomato">
            {{ isEn ? '18+' : '未成年人禁售' }}
          </span>
        </div>
        <h1>{{ title }}</h1>
        <p v-if="subtitle" class="h5product-subtitle">{{ subtitle }}</p>
        <div class="h5product-price">
          <strong>{{ money(retail) }}</strong>
          <span v-if="hasMember">{{ t('products.member') }} {{ money(member) }}</span>
        </div>
      </section>

      <section class="h5product-info">
        <div class="h5-section__head">
          <h2>{{ t('h5.productInfo') }}</h2>
          <span class="h5-muted">{{ spu?.spec }}</span>
        </div>
        <dl class="h5product-table">
          <div v-for="item in infoRows" :key="item.label">
            <dt>{{ item.label }}</dt>
            <dd>{{ item.value }}</dd>
          </div>
        </dl>
      </section>

      <section class="h5product-guide">
        <span class="h5product-guide__mark">MINI</span>
        <div>
          <h2>{{ t('products.miniTitle') }}</h2>
          <p>{{ t('h5.orderTips') }}</p>
        </div>
        <button type="button" class="h5product-guide__btn" @click="sheetOpen = true">
          {{ t('common.more') }}
          <svg width="11" height="11" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </button>
      </section>

      <div class="h5product-foot">
        <span>SKU {{ sku?.skuCode || sku?.id }}</span>
        <span>{{ isEn ? 'Prices from live store data' : '价格来自门店实时数据' }}</span>
      </div>

      <div class="h5-actionbar">
        <div class="h5product-bar-price">
          <span>{{ hasMember ? t('products.member') : (isEn ? 'Price' : '价格') }}</span>
          <strong>{{ money(hasMember ? member : retail) }}</strong>
        </div>
        <button type="button" class="btn" @click="sheetOpen = true">
          {{ t('h5.openMini') }}
          <svg width="13" height="13" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
          </svg>
        </button>
      </div>
    </template>

    <MiniProgramSheet :open="sheetOpen" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
.h5product-page {
  background: var(--paper);
  padding-bottom: 130px;
}

.h5product-loading {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.h5product-loading__media {
  aspect-ratio: 1 / 1;
  border-radius: 22px;
}

.h5product-loading__line {
  height: 26px;
}

.h5product-loading__line--short {
  width: 60%;
}

.h5product-media {
  position: relative;
  border-radius: 22px;
  overflow: hidden;
}

.h5product-media :deep(.smart-image) {
  border-radius: 22px;
}

.h5product-media__top {
  position: absolute;
  top: 14px;
  left: 14px;
  right: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.h5product-media__top .tag {
  background: rgba(251, 248, 241, 0.92);
}

.h5product-share {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(251, 248, 241, 0.92);
  font-size: 9px;
  color: var(--ink);
}

.h5product-head {
  padding: 22px 2px 18px;
  border-bottom: 1px solid var(--line);
}

.h5product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.h5product-head h1 {
  font-size: 28px;
  line-height: 1.3;
  margin-top: 10px;
  font-weight: 500;
  letter-spacing: -0.02em;
}

.h5product-subtitle {
  font-family: var(--font-display);
  font-size: 13px;
  font-style: italic;
  color: var(--muted);
  margin-top: 3px;
}

.h5product-price {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-top: 14px;
}

.h5product-price strong {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 400;
  color: var(--tomato);
  line-height: 1;
}

.h5product-price span {
  font-size: 10px;
  color: var(--tomato);
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(214, 82, 47, 0.1);
}

.h5product-info {
  padding: 24px 0;
  border-bottom: 1px solid var(--line);
}

.h5product-table {
  margin: 0;
  border-top: 1px solid var(--line);
}

.h5product-table div {
  display: grid;
  grid-template-columns: 82px 1fr;
  gap: 16px;
  padding: 11px 0;
  border-bottom: 1px solid var(--line);
}

.h5product-table dt {
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--muted);
}

.h5product-table dd {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-soft);
}

.h5product-guide {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 22px;
  padding: 18px;
  border-radius: 18px;
  background: var(--green);
  color: var(--cream);
}

.h5product-guide__mark {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--gold);
  border: 1px solid rgba(246, 240, 228, 0.2);
  border-radius: 50%;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.h5product-guide h2 {
  font-size: 14px;
  font-weight: 500;
}

.h5product-guide p {
  margin-top: 3px;
  font-size: 9px;
  line-height: 1.6;
  color: rgba(246, 240, 228, 0.55);
}

.h5product-guide__btn {
  margin-left: auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 9px;
  color: rgba(246, 240, 228, 0.7);
}

.h5product-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 22px;
  font-family: var(--font-display);
  font-size: 8px;
  letter-spacing: 0.1em;
  color: var(--muted);
}

.h5product-bar-price {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0;
  min-width: 92px;
}

.h5product-bar-price span {
  font-size: 9px;
  color: var(--muted);
}

.h5product-bar-price strong {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 400;
  color: var(--tomato);
  line-height: 1.1;
}

.h5product-error {
  padding: 60px 20px;
  text-align: center;
}

.h5product-error__mark {
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

.h5product-error h1 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
}

.h5product-error p {
  margin: 8px 0 20px;
  font-size: 11px;
  color: var(--muted);
}
</style>
