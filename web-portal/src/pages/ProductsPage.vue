<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import SmartImage from '@/components/SmartImage.vue'
import { getCategories, getProducts } from '@/api/site'
import { IMAGES, miniProgramQr } from '@/lib/assets'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import type { ProductCategory, ProductRow } from '@/types'

const route = useRoute()
const router = useRouter()
const { t, isEn } = useLang()

const categories = ref<ProductCategory[]>([])
const products = ref<ProductRow[]>([])
const activeCategory = ref<number | null>(
  route.query.categoryId && !Number.isNaN(Number(route.query.categoryId))
    ? Number(route.query.categoryId)
    : null
)
const keyword = ref('')
const loading = ref(true)
const error = ref('')
const qrFailed = ref(false)
let timer: number | undefined

const categoryMap = computed(() => {
  const map: Record<number, ProductCategory> = {}
  for (const category of categories.value) map[category.id] = category
  return map
})

const productCount = computed(() => products.value.length)
const qr = computed(() => miniProgramQr(`${window.location.origin}/h5/deals`))

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch {
    categories.value = []
  }
}

async function loadProducts() {
  loading.value = true
  error.value = ''
  try {
    products.value = await getProducts({
      categoryId: activeCategory.value,
      keyword: keyword.value.trim(),
      limit: 60
    })
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function chooseCategory(id: number | null) {
  activeCategory.value = id
  router.replace({
    path: '/products',
    query: { ...route.query, categoryId: id ? String(id) : undefined }
  })
}

function scheduleSearch() {
  if (timer) window.clearTimeout(timer)
  timer = window.setTimeout(loadProducts, 320)
}

watch(activeCategory, loadProducts)
watch(keyword, scheduleSearch)

useSeo(() => ({
  title: t('seo.productsTitle'),
  description: t('seo.productsDesc')
}))

onMounted(async () => {
  await Promise.all([loadCategories(), loadProducts()])
})
</script>

<template>
  <div class="products-page">
    <section class="products-head">
      <div class="container products-head__inner">
        <div class="products-head__copy">
          <p class="eyebrow reveal" v-reveal>{{ t('products.eyebrow') }}</p>
          <h1 class="display display--lg reveal" v-reveal="60">{{ t('products.title') }}</h1>
          <p class="products-head__desc reveal" v-reveal="120">{{ t('products.desc') }}</p>
        </div>
        <div class="products-head__visual reveal" v-reveal="160">
          <SmartImage :src="IMAGES.coffee" alt="悦享便利咖啡" ratio="4 / 5" art="食" label="FRESH DAILY" />
          <div class="products-head__badge">
            <span>DAILY</span>
            <strong>{{ loading ? '—' : String(productCount).padStart(2, '0') }}</strong>
            <span>{{ isEn ? 'ITEMS' : '件在售' }}</span>
          </div>
          <span class="products-head__caption">FRESH / GOODS</span>
        </div>
      </div>
    </section>

    <section class="catalog">
      <div class="container">
        <div class="catalog__toolbar">
          <div class="catalog__chips">
            <button
              type="button"
              class="chip"
              :class="{ 'is-active': activeCategory === null }"
              @click="chooseCategory(null)"
            >
              {{ t('products.all') }}
            </button>
            <button
              v-for="category in categories"
              :key="category.id"
              type="button"
              class="chip"
              :class="{ 'is-active': activeCategory === category.id }"
              @click="chooseCategory(category.id)"
            >
              {{ isEn && category.nameEn ? category.nameEn : category.name }}
            </button>
          </div>

          <div class="catalog__search">
            <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.2" />
              <path d="m11 11 3.4 3.4" stroke="currentColor" stroke-width="1.2" />
            </svg>
            <input
              v-model="keyword"
              class="input"
              type="search"
              :placeholder="t('products.searchPlaceholder')"
              :aria-label="t('products.search')"
            />
          </div>
        </div>

        <div class="catalog__meta">
          <span>{{ loading ? t('common.loading') : `${productCount} ${isEn ? 'items' : '件商品'}` }}</span>
          <span class="catalog__meta-note">
            {{ isEn ? 'Prices from live store data' : '价格来自门店实时商品数据' }}
          </span>
        </div>

        <div v-if="error" class="error-state">
          <strong>{{ t('common.error') }}</strong>
          <p>{{ error }}</p>
          <button type="button" class="btn btn--sm" @click="loadProducts">{{ t('common.retry') }}</button>
        </div>

        <div v-else-if="loading" class="catalog__grid">
          <div v-for="i in 6" :key="i" class="skeleton catalog__skeleton"></div>
        </div>

        <div v-else-if="!products.length" class="empty-state">
          <strong>{{ t('products.empty') }}</strong>
          <button type="button" class="btn btn--sm" @click="chooseCategory(null); keyword = ''; loadProducts()">
            {{ t('stores.reset') }}
          </button>
        </div>

        <div v-else class="catalog__grid">
          <ProductCard
            v-for="(row, index) in products"
            :key="row.sku.id"
            :row="row"
            :category-name="categoryMap[row.spu.categoryId]?.name"
            v-reveal="(index % 3) * 60"
          />
        </div>

        <div class="mini-order">
          <div class="mini-order__copy">
            <p class="eyebrow eyebrow--light">MINI PROGRAM</p>
            <h2 class="display display--md">{{ t('products.miniTitle') }}</h2>
            <p>{{ t('products.miniDesc') }}</p>
            <div class="mini-order__note">
              <span class="mini-order__dot"></span>
              {{ t('products.miniNote') }}
            </div>
          </div>
          <div class="mini-order__qr">
            <div class="mini-order__qr-box">
              <img v-if="!qrFailed" :src="qr" alt="悦享便利线上商城二维码" @error="qrFailed = true" />
              <span v-else class="mini-order__qr-fallback">悦享便利<br /><small>YUEXIANG</small></span>
            </div>
            <span>{{ isEn ? 'Scan to open the mobile store' : '扫码打开移动端商城' }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.products-page {
  background: var(--paper);
}

.products-head {
  padding: 76px 0 70px;
  background: var(--cream);
  border-bottom: 1px solid var(--line);
}

.products-head__inner {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 90px;
  align-items: center;
}

.products-head h1 {
  margin: 20px 0 24px;
  max-width: 900px;
}

.products-head__desc {
  max-width: 520px;
  font-size: 14px;
  line-height: 1.95;
  color: var(--muted);
}

.products-head__visual {
  position: relative;
  max-width: 420px;
  margin-left: auto;
  width: 100%;
}

.products-head__visual :deep(.smart-image) {
  border-radius: 200px 200px 16px 16px;
}

.products-head__badge {
  position: absolute;
  left: -34px;
  bottom: 30px;
  width: 118px;
  height: 118px;
  border-radius: 50%;
  background: var(--green);
  color: var(--cream);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
  box-shadow: 0 18px 40px rgba(16, 37, 29, 0.2);
}

.products-head__badge span {
  font-size: 8px;
  letter-spacing: 0.18em;
  color: rgba(246, 240, 228, 0.55);
}

.products-head__badge strong {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 400;
  line-height: 1.1;
}

.products-head__caption {
  position: absolute;
  top: 18px;
  right: 20px;
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 0.2em;
  color: rgba(246, 240, 228, 0.7);
}

.catalog {
  padding: 46px 0 110px;
}

.catalog__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  flex-wrap: wrap;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.catalog__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.catalog__search {
  position: relative;
  width: min(280px, 100%);
}

.catalog__search svg {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--muted);
}

.catalog__search .input {
  min-height: 42px;
  padding-left: 38px;
  font-size: 12px;
  border-radius: 999px;
  background: var(--white);
}

.catalog__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 0 28px;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--muted);
}

.catalog__meta-note {
  font-family: var(--font-display);
  font-style: italic;
  letter-spacing: 0.02em;
}

.catalog__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 26px;
}

.catalog__skeleton {
  height: 400px;
  border-radius: 18px;
}

.mini-order {
  position: relative;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 60px;
  align-items: center;
  margin-top: 90px;
  padding: 60px;
  border-radius: 26px;
  background: var(--green);
  color: var(--cream);
  overflow: hidden;
  isolation: isolate;
}

.mini-order::before {
  content: "";
  position: absolute;
  right: -8%;
  top: -40%;
  width: 45%;
  height: 180%;
  border: 1px solid rgba(246, 240, 228, 0.1);
  border-radius: 50%;
  z-index: -1;
}

.mini-order h2 {
  margin: 18px 0 18px;
}

.mini-order__copy > p:last-of-type {
  max-width: 480px;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(246, 240, 228, 0.65);
}

.mini-order__note {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: rgba(246, 240, 228, 0.55);
}

.mini-order__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--tomato);
}

.mini-order__qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: rgba(246, 240, 228, 0.5);
}

.mini-order__qr-box {
  width: 172px;
  height: 172px;
  padding: 12px;
  border-radius: 16px;
  background: var(--cream);
  display: grid;
  place-items: center;
}

.mini-order__qr-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 4px;
}

.mini-order__qr-fallback {
  font-family: var(--font-display);
  font-size: 17px;
  line-height: 1.3;
  color: var(--green);
  text-align: center;
}

.mini-order__qr-fallback small {
  font-family: var(--font-sans);
  font-size: 8px;
  letter-spacing: 0.2em;
}

@media (max-width: 1100px) {
  .products-head__inner {
    grid-template-columns: 1fr 0.7fr;
    gap: 50px;
  }
}

@media (max-width: 900px) {
  .products-head__inner {
    grid-template-columns: 1fr;
  }

  .products-head__visual {
    max-width: 340px;
    margin-left: 0;
  }

  .products-head__badge {
    left: auto;
    right: -10px;
  }

  .catalog__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mini-order {
    grid-template-columns: 1fr;
    gap: 36px;
    padding: 40px;
  }

  .mini-order__qr {
    align-items: flex-start;
  }
}

@media (max-width: 600px) {
  .products-head {
    padding: 54px 0 52px;
  }

  .products-head__badge {
    width: 100px;
    height: 100px;
  }

  .catalog {
    padding: 34px 0 76px;
  }

  .catalog__toolbar {
    align-items: flex-start;
  }

  .catalog__search {
    width: 100%;
  }

  .catalog__grid {
    grid-template-columns: 1fr;
  }

  .catalog__meta {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .mini-order {
    margin-top: 60px;
    padding: 30px 24px;
  }
}
</style>
