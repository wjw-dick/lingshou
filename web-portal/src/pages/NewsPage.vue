<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { getArticles } from '@/api/site'
import { articleImage } from '@/lib/assets'
import { articleCategoryLabel, formatDate } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import type { CmsArticle } from '@/types'

const { t, pick, isEn } = useLang()

const articles = ref<CmsArticle[]>([])
const loading = ref(true)
const error = ref('')
const activeCategory = ref('ALL')

const categories = computed(() => {
  const codes = [...new Set(articles.value.map((article) => article.category).filter(Boolean))]
  return codes
})

const filtered = computed(() => {
  if (activeCategory.value === 'ALL') return articles.value
  return articles.value.filter((article) => article.category === activeCategory.value)
})

const featured = computed(() => filtered.value[0] || null)
const rest = computed(() => filtered.value.slice(1))

function categoryLabel(code: string): string {
  if (isEn.value) {
    const map: Record<string, string> = {
      BRAND: 'Brand',
      FRESH: 'Fresh',
      FRANCHISE: 'Franchise',
      NOTICE: 'Notice'
    }
    return map[code] || code
  }
  return articleCategoryLabel(code)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    articles.value = await getArticles()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

useSeo(() => ({
  title: t('seo.newsTitle'),
  description: t('seo.newsDesc')
}))

onMounted(load)
</script>

<template>
  <div class="news-page">
    <section class="page-head">
      <div class="container page-head__inner">
        <div>
          <p class="eyebrow reveal" v-reveal>{{ t('news.eyebrow') }}</p>
          <h1 class="display display--lg reveal" v-reveal="60">{{ t('news.title') }}</h1>
        </div>
        <p class="page-head__desc reveal" v-reveal="120">{{ t('news.desc') }}</p>
      </div>
    </section>

    <section class="section--tight news-body">
      <div class="container">
        <div class="news-filter">
          <button
            type="button"
            class="chip"
            :class="{ 'is-active': activeCategory === 'ALL' }"
            @click="activeCategory = 'ALL'"
          >
            {{ t('products.all') }}
          </button>
          <button
            v-for="code in categories"
            :key="code"
            type="button"
            class="chip"
            :class="{ 'is-active': activeCategory === code }"
            @click="activeCategory = code"
          >
            {{ categoryLabel(code) }}
          </button>
          <span class="news-filter__count">
            {{ loading ? '—' : String(filtered.length).padStart(2, '0') }}
          </span>
        </div>

        <div v-if="error" class="error-state">
          <strong>{{ t('common.error') }}</strong>
          <p>{{ error }}</p>
          <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
        </div>

        <div v-else-if="loading" class="news-loading">
          <div class="skeleton news-loading__feature"></div>
          <div class="skeleton news-loading__card"></div>
          <div class="skeleton news-loading__card"></div>
        </div>

        <div v-else-if="!featured" class="empty-state">
          <strong>{{ t('news.empty') }}</strong>
        </div>

        <template v-else>
          <router-link :to="`/news/${featured.id}`" class="feature reveal" v-reveal>
            <div class="feature__media">
              <SmartImage
                :src="articleImage(featured)"
                :alt="pick(featured.title, featured.titleEn)"
                ratio="16 / 10"
                art="讯"
                label="JOURNAL"
              />
              <span class="feature__badge">{{ categoryLabel(featured.category) }}</span>
            </div>
            <div class="feature__body">
              <div class="feature__meta">
                <span>{{ formatDate(featured.publishedAt) }}</span>
                <span>{{ featured.author }}</span>
                <span>FEATURED</span>
              </div>
              <h2 class="display display--md">{{ pick(featured.title, featured.titleEn) }}</h2>
              <p>{{ pick(featured.summary, featured.summaryEn) }}</p>
              <span class="link-arrow feature__link">
                {{ t('news.read') }}
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                  <path d="M3 11 11 3M5 3h6v6" stroke="currentColor" stroke-width="1.3" />
                </svg>
              </span>
            </div>
          </router-link>

          <div class="news-grid">
            <router-link
              v-for="(article, index) in rest"
              :key="article.id"
              :to="`/news/${article.id}`"
              class="news-card reveal"
              v-reveal="index * 60"
            >
              <div class="news-card__media">
                <SmartImage
                  :src="articleImage(article)"
                  :alt="pick(article.title, article.titleEn)"
                  ratio="4 / 3"
                  art="讯"
                  label="JOURNAL"
                />
                <span class="news-card__category">{{ categoryLabel(article.category) }}</span>
              </div>
              <div class="news-card__body">
                <div class="news-card__meta">
                  <span>{{ formatDate(article.publishedAt) }}</span>
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
        </template>
      </div>
    </section>
  </div>
</template>

<style scoped>
.news-page {
  background: var(--paper);
}

.page-head {
  padding: 76px 0 60px;
  background: var(--cream);
  border-bottom: 1px solid var(--line);
}

.page-head__inner {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 70px;
}

.page-head h1 {
  margin-top: 18px;
}

.page-head__desc {
  max-width: 370px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--muted);
  padding-bottom: 8px;
}

.news-body {
  padding: 42px 0 110px;
}

.news-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.news-filter__count {
  margin-left: auto;
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--muted);
}

.feature {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 56px;
  align-items: center;
  padding: 40px 0 56px;
  border-bottom: 1px solid var(--line);
}

.feature__media {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
}

.feature__media :deep(.smart-image) {
  transition: transform 1.2s cubic-bezier(0.22, 1, 0.36, 1);
}

.feature:hover .feature__media :deep(.smart-image) {
  transform: scale(1.03);
}

.feature__badge {
  position: absolute;
  left: 18px;
  top: 18px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(246, 240, 228, 0.92);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--ink);
}

.feature__meta {
  display: flex;
  gap: 18px;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.feature h2 {
  margin: 20px 0 18px;
}

.feature__body > p {
  max-width: 520px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--muted);
}

.feature__link {
  margin-top: 30px;
  width: fit-content;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
  padding-top: 48px;
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
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--ink);
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

.news-loading {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 40px;
  padding-top: 40px;
}

.news-loading__feature {
  height: 440px;
  border-radius: 20px;
}

.news-loading__card {
  height: 200px;
  border-radius: 16px;
}

@media (max-width: 1000px) {
  .feature {
    grid-template-columns: 1fr;
    gap: 30px;
  }

  .news-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .news-loading {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .page-head__inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 24px;
  }

  .news-grid {
    grid-template-columns: 1fr;
  }

  .news-card h3,
  .news-card p {
    max-width: 100%;
  }
}

@media (max-width: 520px) {
  .page-head {
    padding: 54px 0 44px;
  }

  .news-body {
    padding: 30px 0 80px;
  }

  .feature {
    padding: 30px 0 40px;
  }

  .news-filter__count {
    display: none;
  }
}
</style>
