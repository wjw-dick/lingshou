<script setup lang="ts">
import { computed } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { productImage } from '@/lib/assets'
import { money, shelfLifeLabel, storageLabel } from '@/lib/format'
import { useLang } from '@/i18n'
import type { ProductRow } from '@/types'

const props = defineProps<{
  row: ProductRow
  categoryName?: string
  compact?: boolean
}>()

const { t, lang } = useLang()

const spu = computed(() => props.row.spu)
const sku = computed(() => props.row.sku)
const title = computed(() => (lang.value === 'en' && spu.value.nameEn ? spu.value.nameEn : spu.value.name))
const subtitle = computed(() =>
  lang.value === 'zh' && spu.value.nameEn ? spu.value.nameEn : ''
)
const image = computed(() => productImage(spu.value))
const artText = computed(() => title.value.replace(/^悦享/, '').slice(0, 1) || '悦')
const retail = computed(() => sku.value.priceRetail ?? spu.value.priceRetail)
const member = computed(() => sku.value.priceMember ?? spu.value.priceMember)
const hasMember = computed(() => Number(member.value ?? 0) > 0 && Number(member.value) < Number(retail.value))
</script>

<template>
  <article class="product-card" :class="{ 'is-compact': compact }">
    <SmartImage
      :src="image"
      :alt="title"
      ratio="4 / 3"
      tone="green"
      :art="artText"
      :label="categoryName || 'YUEXIANG'"
    />
    <div class="product-card__body">
      <div class="product-card__tags">
        <span class="tag">{{ shelfLifeLabel(spu.shelfLifeDays) }}</span>
        <span class="tag">{{ storageLabel(spu.storageCondition) }}</span>
      </div>
      <h3>{{ title }}</h3>
      <p v-if="subtitle" class="product-card__en">{{ subtitle }}</p>
      <div class="product-card__foot">
        <div class="product-card__prices">
          <span class="price"><small>¥</small>{{ money(retail).replace('¥', '') }}</span>
          <span v-if="hasMember" class="product-card__member">
            {{ t('products.member') }} {{ money(member) }}
          </span>
        </div>
        <span class="product-card__sale">{{ t('products.notForSale') }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--white);
  border: 1px solid var(--line);
  border-radius: 18px;
  overflow: hidden;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.5s ease,
    border-color 0.5s ease;
}

.product-card:hover {
  transform: translateY(-6px);
  border-color: rgba(16, 37, 29, 0.26);
  box-shadow: var(--shadow);
}

.product-card :deep(.smart-image img) {
  transition: transform 1.2s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.6s ease;
}

.product-card:hover :deep(.smart-image img) {
  transform: scale(1.045);
}

.product-card__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 18px 18px 20px;
}

.product-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.product-card h3 {
  font-size: 17px;
  line-height: 1.4;
  letter-spacing: -0.01em;
}

.product-card__en {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 12px;
  font-style: italic;
  color: var(--muted);
}

.product-card__foot {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 20px;
}

.product-card__prices {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.product-card__member {
  font-size: 10px;
  letter-spacing: 0.04em;
  color: var(--tomato);
}

.product-card__sale {
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--muted);
  border-bottom: 1px solid var(--line);
  padding-bottom: 3px;
  white-space: nowrap;
}

.is-compact .product-card__body {
  padding: 14px 16px 16px;
}

.is-compact h3 {
  font-size: 15px;
}

.is-compact .price {
  font-size: 22px;
}
</style>
