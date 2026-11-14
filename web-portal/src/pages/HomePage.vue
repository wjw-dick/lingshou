<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import ProductCard from '@/components/ProductCard.vue'
import { getArticles, getHome } from '@/api/site'
import { IMAGES, articleImage } from '@/lib/assets'
import { formatShortDate } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import { useSiteStore } from '@/composables/useSiteStore'
import type { CmsArticle, HomeData } from '@/types'

const { t, isEn, pick } = useLang()
const { config } = useSiteStore()

const home = ref<HomeData | null>(null)
const articles = ref<CmsArticle[]>([])
const loading = ref(true)
const error = ref('')

const stores = computed(() => home.value?.stores || [])
const products = computed(() => home.value?.products || [])
const categories = computed(() => home.value?.categories || [])
const levels = computed(() => home.value?.levels || [])
const regions = computed(() => [...new Set(stores.value.map((store) => store.region).filter(Boolean))])
const freshProducts = computed(() => {
  const fresh = products.value.filter((row) => row.spu.categoryId === 1)
  const rest = products.value.filter((row) => row.spu.categoryId !== 1)
  return [...fresh, ...rest].slice(0, 3)
})
const freshCount = computed(() => products.value.filter((row) => row.spu.categoryId === 1).length)
const news = computed(() => articles.value.slice(0, 3))
const categoryTiles = computed(() => categories.value.slice(0, 4))
const heroTitle = computed(
  () => home.value?.banners?.[0]?.title || t('home.bannerTitle')
)
const servicePhone = computed(() => config('site.service_phone', '400-880-0001'))

const stats = computed(() => [
  { value: stores.value.length, label: t('home.statStores') },
  { value: regions.value.length, label: t('home.statRegions') },
  { value: freshCount.value, label: t('home.statFresh') },
  { value: levels.value.length, label: t('home.statLevels') }
])

function categoryCount(categoryId: number): number {
  return products.value.filter((row) => row.spu.categoryId === categoryId).length
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [homeData, articleList] = await Promise.all([
      getHome(),
      getArticles().catch(() => [])
    ])
    home.value = homeData
    articles.value = articleList
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

useSeo(() => ({
  title: isEn.value ? t('seo.homeTitle') : config('site.seo_title') || t('seo.homeTitle'),
  description: t('seo.homeDesc'),
  hreflang: true
}))

onMounted(load)
</script>

<template>
  <div class="home">
    <section class="hero">
      <div
        class="hero__media"
        :style="{ backgroundImage: `linear-gradient(115deg, rgba(16,37,29,.92) 8%, rgba(16,37,29,.58) 52%, rgba(16,37,29,.72) 100%), url(${IMAGES.hero})` }"
      ></div>
      <div class="hero__ring" aria-hidden="true"></div>
      <div class="hero__grain" aria-hidden="true"></div>

      <div class="container hero__inner">
        <div class="hero__copy">
          <p class="eyebrow eyebrow--light reveal" v-reveal>
            {{ config('site.name', '悦享便利') }} · NEIGHBORHOOD CONVENIENCE
          </p>
          <h1 class="hero__title" :class="{ 'is-en': isEn }">
            <span class="hero__line reveal" v-reveal="60">{{ t('home.line1') }}</span>
            <span class="hero__line hero__line--accent reveal" v-reveal="140">
              {{ t('home.line2') }}
              <em v-if="isEn">{{ t('home.line3') }}</em>
            </span>
          </h1>
          <p class="hero__desc reveal" v-reveal="220">{{ t('home.desc') }}</p>
          <div class="hero__actions reveal" v-reveal="300">
            <router-link to="/stores" class="btn btn--light">
              {{ t('home.explore') }}
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
              </svg>
            </router-link>
            <router-link to="/franchise" class="btn btn--ghost-light">
              {{ t('home.join') }}
            </router-link>
          </div>
        </div>

        <div class="hero__side">
          <span>EST. 2018</span>
          <span>SHANGHAI · 31.23°N</span>
          <span class="hero__side-line"></span>
          <span class="hero__side-vertical">{{ t('home.brandPromise') }}</span>
        </div>

        <div class="hero__foot">
          <a href="#fresh" class="hero__scroll">
            <span class="hero__scroll-line"></span>
            {{ t('home.scroll') }}
          </a>
          <div class="hero__pill">
            <span class="hero__pill-dot"></span>
            {{ heroTitle }}
          </div>
        </div>
      </div>
    </section>

    <section class="stat-band">
      <div class="container">
        <div v-if="error" class="stat-band__error">
          <span>{{ t('common.error') }} · {{ error }}</span>
          <button type="button" class="link-arrow" @click="load">{{ t('common.retry') }}</button>
        </div>
        <div class="stat-band__grid">
          <div v-for="(item, index) in stats" :key="item.label" class="stat reveal" v-reveal="index * 70">
            <strong>{{ loading ? '—' : String(item.value).padStart(2, '0') }}</strong>
            <span>{{ item.label }}</span>
          </div>
          <div class="stat stat--note reveal" v-reveal="280">
            <span class="stat__note-mark">*</span>
            <p>{{ t('home.brandPromise') }}</p>
            <small>{{ servicePhone }}</small>
          </div>
        </div>
      </div>
    </section>

    <section id="fresh" class="section fresh">
      <div class="container">
        <div class="fresh__intro">
          <div class="fresh__copy reveal" v-reveal>
            <p class="index-label">{{ t('home.freshIndex') }}</p>
            <h2 class="display display--lg">{{ t('home.freshTitle') }}</h2>
            <p class="fresh__desc">{{ t('home.freshDesc') }}</p>
            <div class="fresh__note">
              <span class="fresh__note-dot"></span>
              {{ t('home.freshNote') }}
            </div>
            <router-link to="/products" class="link-arrow fresh__link">
              {{ t('home.freshCta') }}
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
              </svg>
            </router-link>
          </div>

          <div class="fresh__visual reveal" v-reveal="120">
            <div class="fresh__main">
              <SmartImage :src="IMAGES.fresh" alt="悦享便利鲜食" ratio="4 / 5" art="鲜" label="FRESH DAILY" />
              <span class="fresh__caption">FRESH / 01</span>
            </div>
            <div class="fresh__small">
              <SmartImage :src="IMAGES.coffee" alt="悦享便利现磨咖啡" ratio="1 / 1" art="咖" label="COFFEE" />
            </div>
            <div class="fresh__badge">
              <span>DAILY</span>
              <strong>07:00</strong>
              <span>现制</span>
            </div>
          </div>
        </div>

        <div class="fresh__products">
          <ProductCard
            v-for="row in freshProducts"
            :key="row.sku.id"
            :row="row"
            :category-name="categories.find((item) => item.id === row.spu.categoryId)?.name"
          />
          <template v-if="loading">
            <div v-for="i in 3" :key="`s-${i}`" class="skeleton fresh__skeleton"></div>
          </template>
          <div v-if="!loading && !freshProducts.length" class="empty-state fresh__empty">
            <strong>{{ t('common.empty') }}</strong>
            <button type="button" class="link-arrow" @click="load">{{ t('common.retry') }}</button>
          </div>
        </div>
      </div>
    </section>

    <section class="section categories">
      <div class="container">
        <div class="section-head">
          <div class="section-head__title reveal" v-reveal>
            <p class="index-label">{{ t('home.categoriesIndex') }}</p>
            <h2 class="display display--md">{{ t('home.categoriesTitle') }}</h2>
          </div>
          <p class="section-head__desc reveal" v-reveal="90">{{ t('home.categoriesDesc') }}</p>
        </div>

        <div class="category-grid">
          <router-link
            v-for="(category, index) in categoryTiles"
            :key="category.id"
            :to="{ path: '/products', query: { categoryId: category.id } }"
            class="category-tile reveal"
            v-reveal="index * 80"
          >
            <SmartImage
              :src="IMAGES[category.id === 1 ? 'fresh' : category.id === 2 ? 'icedCoffee' : category.id === 4 ? 'paper' : 'fresh']"
              :alt="category.name"
              ratio="3 / 4"
              art="享"
              :label="category.nameEn || 'YUEXIANG'"
            />
            <div class="category-tile__overlay"></div>
            <div class="category-tile__content">
              <span class="category-tile__index">0{{ index + 1 }}</span>
              <div>
                <h3>{{ isEn && category.nameEn ? category.nameEn : category.name }}</h3>
                <p>{{ categoryCount(category.id) }} {{ isEn ? 'items' : '件商品' }}</p>
              </div>
              <span class="category-tile__arrow">
                <svg width="15" height="15" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                  <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
                </svg>
              </span>
            </div>
          </router-link>
        </div>
      </div>
    </section>

    <section class="store-section">
      <div
        class="store-section__bg"
        :style="{ backgroundImage: `url(${IMAGES.store})` }"
        aria-hidden="true"
      ></div>
      <div class="container store-section__inner">
        <div class="store-section__copy reveal" v-reveal>
          <p class="eyebrow eyebrow--light">{{ t('home.storesIndex') }}</p>
          <h2 class="display display--lg">{{ t('home.storesTitle') }}</h2>
          <p>{{ t('home.storesDesc') }}</p>
          <div class="store-section__regions">
            <router-link
              v-for="region in regions"
              :key="region"
              :to="{ path: '/stores', query: { region } }"
              class="store-region"
            >
              {{ region }}
            </router-link>
          </div>
          <router-link to="/stores" class="btn btn--light">
            {{ t('home.storesCta') }}
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
            </svg>
          </router-link>
        </div>

        <div class="store-list">
          <router-link
            v-for="(store, index) in stores.slice(0, 2)"
            :key="store.id"
            :to="`/h5/store/${store.id}`"
            class="store-card reveal"
            v-reveal="index * 100"
          >
            <div class="store-card__top">
              <span class="store-card__index">0{{ index + 1 }}</span>
              <span class="tag tag--light">{{ store.region }}</span>
            </div>
            <h3>{{ store.name }}</h3>
            <p>{{ store.address }}</p>
            <div class="store-card__meta">
              <span>{{ store.businessHours }}</span>
              <span>{{ store.phone }}</span>
            </div>
            <span class="store-card__arrow">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
                <path d="M4 14 14 4M6 4h8v8" stroke="currentColor" stroke-width="1.2" />
              </svg>
            </span>
          </router-link>
          <div v-if="!stores.length" class="store-list__empty">{{ t('stores.empty') }}</div>
        </div>
      </div>
    </section>

    <section class="section franchise">
      <div class="container franchise__inner">
        <div class="franchise__visual reveal" v-reveal>
          <SmartImage :src="IMAGES.franchise" alt="悦享便利加盟合作" ratio="4 / 5" art="合" label="PARTNER WITH US" />
          <div class="franchise__stamp">
            <span>JOIN US</span>
            <strong>35<small>万起</small></strong>
            <span>单店投资</span>
          </div>
        </div>

        <div class="franchise__copy reveal" v-reveal="100">
          <p class="index-label">{{ t('home.franchiseIndex') }}</p>
          <h2 class="display display--lg">{{ t('home.franchiseTitle') }}</h2>
          <p class="franchise__desc">{{ t('home.franchiseDesc') }}</p>
          <ul class="franchise__list">
            <li>
              <span>01</span>
              <div>
                <strong>{{ t('franchise.support1') }}</strong>
                <p>{{ t('franchise.support1Desc') }}</p>
              </div>
            </li>
            <li>
              <span>02</span>
              <div>
                <strong>{{ t('franchise.support2') }}</strong>
                <p>{{ t('franchise.support2Desc') }}</p>
              </div>
            </li>
            <li>
              <span>03</span>
              <div>
                <strong>{{ t('franchise.support3') }}</strong>
                <p>{{ t('franchise.support3Desc') }}</p>
              </div>
            </li>
          </ul>
          <router-link to="/franchise" class="btn">
            {{ t('home.franchiseCta') }}
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
            </svg>
          </router-link>
        </div>
      </div>
    </section>

    <section class="section news">
      <div class="container">
        <div class="section-head">
          <div class="section-head__title reveal" v-reveal>
            <p class="index-label">{{ t('home.newsIndex') }}</p>
            <h2 class="display display--md">{{ t('home.newsTitle') }}</h2>
          </div>
          <router-link to="/news" class="link-arrow reveal" v-reveal="80">
            {{ t('home.newsCta') }}
          </router-link>
        </div>

        <div class="news-grid">
          <router-link
            v-for="(article, index) in news"
            :key="article.id"
            :to="`/news/${article.id}`"
            class="news-card reveal"
            v-reveal="index * 80"
          >
            <div class="news-card__media">
              <SmartImage
                :src="articleImage(article)"
                :alt="pick(article.title, article.titleEn)"
                ratio="4 / 3"
                art="讯"
                label="JOURNAL"
              />
              <span class="news-card__category">{{ article.category }}</span>
            </div>
            <div class="news-card__body">
              <div class="news-card__meta">
                <span>{{ formatShortDate(article.publishedAt) }}</span>
                <span>{{ article.author }}</span>
              </div>
              <h3>{{ pick(article.title, article.titleEn) }}</h3>
              <p>{{ pick(article.summary, article.summaryEn) }}</p>
              <span class="news-card__arrow">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                  <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
                </svg>
              </span>
            </div>
          </router-link>
        </div>
      </div>
    </section>

    <section class="closing">
      <div class="container closing__inner">
        <p class="eyebrow">YUEXIANG MART</p>
        <h2 class="display display--lg">{{ t('footer.slogan') }}</h2>
        <div class="closing__actions">
          <router-link to="/stores" class="btn">{{ t('home.explore') }}</router-link>
          <router-link to="/contact" class="btn btn--ghost">{{ t('nav.contact') }}</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  overflow-x: clip;
}

.hero {
  position: relative;
  min-height: calc(100vh - var(--header-h));
  margin-top: calc(var(--header-h) * -1);
  padding-top: var(--header-h);
  display: flex;
  align-items: stretch;
  color: var(--cream);
  background: var(--ink);
  isolation: isolate;
}

.hero__media {
  position: absolute;
  inset: 0;
  z-index: -3;
  background-size: cover;
  background-position: center;
  filter: saturate(0.9) contrast(1.02);
  transform: scale(1.02);
}

.hero__grain {
  position: absolute;
  inset: 0;
  z-index: -2;
  background:
    radial-gradient(circle at 78% 18%, rgba(195, 163, 106, 0.18), transparent 32%),
    linear-gradient(180deg, rgba(16, 37, 29, 0.12) 0%, rgba(16, 37, 29, 0.45) 100%);
}

.hero__ring {
  position: absolute;
  z-index: -1;
  right: -12vw;
  top: 8%;
  width: 56vw;
  height: 56vw;
  border: 1px solid rgba(246, 240, 228, 0.1);
  border-radius: 50%;
  pointer-events: none;
}

.hero__ring::before,
.hero__ring::after {
  content: "";
  position: absolute;
  border: 1px solid rgba(246, 240, 228, 0.07);
  border-radius: 50%;
}

.hero__ring::before {
  inset: 8%;
}

.hero__ring::after {
  inset: 22%;
}

.hero__inner {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-top: 70px;
  padding-bottom: 132px;
  width: min(1340px, calc(100% - 112px));
}

.hero__copy {
  max-width: 980px;
}

.hero__title {
  margin: 26px 0 30px;
  font-size: clamp(58px, 8.6vw, 142px);
  font-weight: 500;
  line-height: 0.98;
  letter-spacing: -0.055em;
}

.hero__title.is-en {
  font-family: var(--font-display);
  font-weight: 400;
  letter-spacing: -0.035em;
  line-height: 0.9;
}

.hero__line {
  display: block;
}

.hero__line--accent {
  color: var(--cream);
}

.hero__line--accent em {
  display: block;
  font-family: var(--font-display);
  font-style: italic;
  font-weight: 400;
  font-size: 0.62em;
  letter-spacing: -0.02em;
  color: rgba(246, 240, 228, 0.62);
  margin-top: 0.08em;
}

.hero__desc {
  max-width: 460px;
  font-size: 15px;
  line-height: 1.95;
  color: rgba(246, 240, 228, 0.7);
}

.hero__actions {
  display: flex;
  gap: 14px;
  margin-top: 36px;
  flex-wrap: wrap;
}

.hero__side {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: rgba(246, 240, 228, 0.45);
  writing-mode: vertical-rl;
  text-orientation: mixed;
}

.hero__side-line {
  width: 1px;
  height: 52px;
  background: rgba(246, 240, 228, 0.25);
  display: block;
}

.hero__side-vertical {
  letter-spacing: 0.34em;
}

.hero__foot {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.hero__scroll {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(246, 240, 228, 0.52);
}

.hero__scroll-line {
  width: 54px;
  height: 1px;
  background: rgba(246, 240, 228, 0.35);
  position: relative;
  overflow: hidden;
}

.hero__scroll-line::after {
  content: "";
  position: absolute;
  inset: 0;
  background: var(--cream);
  animation: scroll-line 2.4s infinite;
}

.hero__pill {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 9px 16px;
  border: 1px solid rgba(246, 240, 228, 0.22);
  border-radius: 999px;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: rgba(246, 240, 228, 0.72);
  backdrop-filter: blur(8px);
}

.hero__pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--tomato);
}

.stat-band {
  background: var(--cream);
  border-bottom: 1px solid var(--line);
}

.stat-band__grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  border-left: 1px solid var(--line);
}

.stat {
  padding: 34px 28px;
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat strong {
  font-family: var(--font-display);
  font-size: clamp(38px, 4vw, 58px);
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.03em;
}

.stat span {
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.stat--note {
  position: relative;
  justify-content: space-between;
  background: var(--ink);
  color: var(--cream);
}

.stat--note p {
  font-family: var(--font-display);
  font-size: 17px;
  font-style: italic;
  line-height: 1.4;
  color: rgba(246, 240, 228, 0.82);
}

.stat--note small {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: rgba(246, 240, 228, 0.46);
}

.stat__note-mark {
  position: absolute;
  top: 12px;
  right: 18px;
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--tomato);
}

.stat-band__error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 0;
  font-size: 12px;
  color: var(--tomato);
  border-bottom: 1px solid var(--line);
}

.stat-band__error .link-arrow {
  border: 0;
}

.fresh {
  background: var(--paper);
}

.fresh__intro {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 90px;
  align-items: center;
}

.fresh__copy h2 {
  margin: 22px 0 26px;
  max-width: 560px;
}

.fresh__desc {
  max-width: 460px;
  color: var(--muted);
  font-size: 15px;
  line-height: 1.95;
}

.fresh__note {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--ink-soft);
}

.fresh__note-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--tomato);
}

.fresh__link {
  display: flex;
  width: fit-content;
  margin-top: 36px;
}

.fresh__visual {
  position: relative;
  padding-left: 46px;
}

.fresh__main {
  position: relative;
  border-radius: 220px 220px 18px 18px;
  overflow: hidden;
}

.fresh__caption {
  position: absolute;
  left: 20px;
  bottom: 18px;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 0.2em;
  color: rgba(246, 240, 228, 0.7);
}

.fresh__small {
  position: absolute;
  width: 190px;
  left: 0;
  bottom: -26px;
  border-radius: 14px;
  overflow: hidden;
  border: 6px solid var(--paper);
  box-shadow: var(--shadow);
}

.fresh__badge {
  position: absolute;
  top: 20px;
  right: 10px;
  width: 108px;
  height: 108px;
  border-radius: 50%;
  background: var(--cream);
  color: var(--ink);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  transform: rotate(10deg);
  box-shadow: 0 12px 30px rgba(16, 37, 29, 0.14);
}

.fresh__badge span {
  font-size: 8px;
  letter-spacing: 0.2em;
  color: var(--muted);
}

.fresh__badge strong {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.1;
}

.fresh__products {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-top: 90px;
}

.fresh__skeleton {
  height: 380px;
  border-radius: 18px;
}

.fresh__empty {
  grid-column: span 3;
}

.categories {
  background: var(--cream-2);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.category-tile {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  display: block;
}

.category-tile :deep(.smart-image) {
  transition: transform 1.1s cubic-bezier(0.22, 1, 0.36, 1);
}

.category-tile:hover :deep(.smart-image) {
  transform: scale(1.04);
}

.category-tile__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(16, 37, 29, 0.04) 18%, rgba(16, 37, 29, 0.82) 100%);
  z-index: 2;
}

.category-tile__content {
  position: absolute;
  inset: auto 0 0;
  z-index: 3;
  padding: 22px;
  color: var(--cream);
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.category-tile__index {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: rgba(246, 240, 228, 0.6);
  position: absolute;
  top: 18px;
  left: 20px;
}

.category-tile__content h3 {
  font-size: 23px;
  font-weight: 500;
}

.category-tile__content p {
  font-size: 11px;
  color: rgba(246, 240, 228, 0.6);
  margin-top: 2px;
}

.category-tile__arrow {
  margin-left: auto;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(246, 240, 228, 0.3);
  display: grid;
  place-items: center;
  transition: background 0.3s ease, border-color 0.3s ease;
}

.category-tile:hover .category-tile__arrow {
  background: var(--tomato);
  border-color: var(--tomato);
}

.store-section {
  position: relative;
  padding: 120px 0;
  background: var(--green);
  color: var(--cream);
  overflow: hidden;
  isolation: isolate;
}

.store-section__bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0.12;
  z-index: -1;
  filter: grayscale(0.3);
}

.store-section::after {
  content: "";
  position: absolute;
  left: -12%;
  bottom: -40%;
  width: 70vw;
  height: 70vw;
  border: 1px solid rgba(246, 240, 228, 0.06);
  border-radius: 50%;
  z-index: -1;
}

.store-section__inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 90px;
  align-items: center;
}

.store-section__copy h2 {
  margin: 22px 0 24px;
}

.store-section__copy > p {
  max-width: 440px;
  font-size: 15px;
  line-height: 1.95;
  color: rgba(246, 240, 228, 0.62);
}

.store-section__regions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 26px 0 34px;
}

.store-region {
  padding: 7px 14px;
  border: 1px solid rgba(246, 240, 228, 0.22);
  border-radius: 999px;
  font-size: 11px;
  color: rgba(246, 240, 228, 0.72);
  transition: background 0.3s ease, color 0.3s ease;
}

.store-region:hover {
  background: var(--cream);
  color: var(--ink);
}

.store-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.store-card {
  position: relative;
  padding: 26px 28px;
  border: 1px solid rgba(246, 240, 228, 0.16);
  border-radius: 18px;
  display: block;
  transition: background 0.4s ease, border-color 0.4s ease, transform 0.4s ease;
}

.store-card:hover {
  background: rgba(246, 240, 228, 0.06);
  border-color: rgba(246, 240, 228, 0.34);
  transform: translateX(6px);
}

.store-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.store-card__index {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: rgba(246, 240, 228, 0.44);
}

.store-card h3 {
  font-size: 22px;
  margin-bottom: 6px;
}

.store-card > p {
  font-size: 12px;
  color: rgba(246, 240, 228, 0.55);
  line-height: 1.6;
  max-width: 360px;
}

.store-card__meta {
  display: flex;
  gap: 20px;
  margin-top: 18px;
  font-size: 11px;
  color: rgba(246, 240, 228, 0.45);
}

.store-card__arrow {
  position: absolute;
  right: 24px;
  bottom: 24px;
  color: rgba(246, 240, 228, 0.5);
  transition: transform 0.35s ease, color 0.35s ease;
}

.store-card:hover .store-card__arrow {
  transform: translate(3px, -3px);
  color: var(--cream);
}

.store-list__empty {
  padding: 40px;
  border: 1px dashed rgba(246, 240, 228, 0.2);
  border-radius: 18px;
  text-align: center;
  font-size: 13px;
  color: rgba(246, 240, 228, 0.5);
}

.franchise {
  background: var(--paper);
}

.franchise__inner {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 100px;
  align-items: center;
}

.franchise__visual {
  position: relative;
}

.franchise__visual :deep(.smart-image) {
  border-radius: 200px 200px 18px 18px;
}

.franchise__stamp {
  position: absolute;
  right: -30px;
  bottom: 40px;
  width: 138px;
  height: 138px;
  border-radius: 50%;
  background: var(--tomato);
  color: var(--cream);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transform: rotate(-8deg);
  box-shadow: 0 20px 44px rgba(214, 82, 47, 0.28);
}

.franchise__stamp span {
  font-size: 8px;
  letter-spacing: 0.2em;
  color: rgba(246, 240, 228, 0.7);
}

.franchise__stamp strong {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 400;
  line-height: 1;
}

.franchise__stamp small {
  font-size: 10px;
  font-family: var(--font-sans);
  margin-left: 2px;
}

.franchise__copy h2 {
  margin: 22px 0 24px;
  max-width: 540px;
}

.franchise__desc {
  max-width: 480px;
  font-size: 15px;
  line-height: 1.95;
  color: var(--muted);
}

.franchise__list {
  list-style: none;
  margin: 36px 0;
  padding: 0;
  border-top: 1px solid var(--line);
}

.franchise__list li {
  display: flex;
  gap: 24px;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
}

.franchise__list > li > span {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--tomato);
  padding-top: 3px;
}

.franchise__list strong {
  font-size: 14px;
  font-weight: 600;
}

.franchise__list p {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.news {
  background: var(--cream);
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 26px;
}

.news-card {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--line);
  padding-top: 18px;
}

.news-card__media {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
}

.news-card__media :deep(.smart-image) {
  transition: transform 1.1s cubic-bezier(0.22, 1, 0.36, 1);
}

.news-card:hover .news-card__media :deep(.smart-image) {
  transform: scale(1.04);
}

.news-card__category {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(246, 240, 228, 0.92);
  color: var(--ink);
  font-size: 9px;
  letter-spacing: 0.1em;
}

.news-card__body {
  position: relative;
  padding: 18px 4px 0;
}

.news-card__meta {
  display: flex;
  gap: 16px;
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--muted);
  margin-bottom: 10px;
}

.news-card h3 {
  font-size: 20px;
  line-height: 1.45;
  max-width: 90%;
}

.news-card p {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--muted);
  max-width: 90%;
}

.news-card__arrow {
  position: absolute;
  right: 4px;
  bottom: 4px;
  color: var(--muted);
  transition: transform 0.3s ease, color 0.3s ease;
}

.news-card:hover .news-card__arrow {
  transform: translate(3px, -3px);
  color: var(--tomato);
}

.closing {
  background: var(--tomato);
  color: var(--cream);
  padding: 84px 0;
}

.closing__inner {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 40px;
  flex-wrap: wrap;
}

.closing__inner .eyebrow {
  color: rgba(246, 240, 228, 0.6);
}

.closing h2 {
  margin-top: 14px;
}

.closing__actions {
  display: flex;
  gap: 12px;
  padding-bottom: 6px;
}

.closing .btn {
  background: var(--cream);
  color: var(--ink);
}

.closing .btn:hover {
  background: var(--ink);
  color: var(--cream);
}

.closing .btn--ghost {
  background: transparent;
  border-color: rgba(246, 240, 228, 0.4);
  color: var(--cream);
}

.closing .btn--ghost:hover {
  background: var(--cream);
  color: var(--ink);
}

@keyframes scroll-line {
  0% {
    transform: translateX(-100%);
  }
  55% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(100%);
  }
}

@media (max-width: 1100px) {
  .hero__inner {
    width: calc(100% - 64px);
  }

  .fresh__intro,
  .store-section__inner,
  .franchise__inner {
    gap: 56px;
  }

  .stat-band__grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .stat--note {
    grid-column: span 2;
  }

  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 860px) {
  .hero__side {
    display: none;
  }

  .fresh__intro,
  .store-section__inner,
  .franchise__inner {
    grid-template-columns: 1fr;
  }

  .fresh__visual {
    padding-left: 0;
    margin-top: 30px;
  }

  .fresh__main :deep(.smart-image) {
    aspect-ratio: 4 / 3 !important;
  }

  .fresh__small {
    width: 140px;
    left: 14px;
    bottom: 14px;
  }

  .fresh__badge {
    width: 92px;
    height: 92px;
    top: 14px;
    right: 14px;
  }

  .fresh__products,
  .news-grid {
    grid-template-columns: 1fr;
  }

  .fresh__empty {
    grid-column: span 1;
  }

  .franchise__visual {
    max-width: 440px;
  }

  .franchise__stamp {
    right: 10px;
  }

  .news-card h3,
  .news-card p {
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .hero {
    min-height: calc(100svh - var(--header-h));
  }

  .hero__inner {
    width: calc(100% - 40px);
    padding-top: 50px;
    padding-bottom: 100px;
  }

  .hero__title {
    font-size: clamp(46px, 14vw, 70px);
  }

  .hero__desc {
    font-size: 13px;
  }

  .hero__foot {
    bottom: 22px;
  }

  .hero__pill {
    font-size: 9px;
  }

  .stat-band__grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat {
    padding: 24px 18px;
  }

  .stat--note {
    grid-column: span 2;
  }

  .fresh__products {
    margin-top: 64px;
  }

  .category-grid {
    grid-template-columns: 1fr;
  }

  .category-tile :deep(.smart-image) {
    aspect-ratio: 16 / 10 !important;
  }

  .store-section {
    padding: 76px 0;
  }

  .closing__inner {
    align-items: flex-start;
  }

  .closing__actions {
    flex-wrap: wrap;
  }
}
</style>
