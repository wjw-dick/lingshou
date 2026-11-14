<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SmartImage from '@/components/SmartImage.vue'
import { getArticle, getArticles } from '@/api/site'
import { articleImage } from '@/lib/assets'
import { articleCategoryLabel, formatDate } from '@/lib/format'
import { useLang } from '@/i18n'
import { useSeo } from '@/composables/useSeo'
import type { CmsArticle } from '@/types'

const route = useRoute()
const { t, pick, isEn } = useLang()

const article = ref<CmsArticle | null>(null)
const related = ref<CmsArticle[]>([])
const loading = ref(true)
const error = ref('')

const categoryLabel = computed(() => {
  if (!article.value) return ''
  if (isEn.value) {
    const map: Record<string, string> = {
      BRAND: 'Brand',
      FRESH: 'Fresh',
      FRANCHISE: 'Franchise',
      NOTICE: 'Notice'
    }
    return map[article.value.category] || article.value.category
  }
  return articleCategoryLabel(article.value.category)
})

const htmlContent = computed(() => {
  if (!article.value) return ''
  if (isEn.value && article.value.contentEn) return article.value.contentEn
  return article.value.content || ''
})

async function load() {
  const id = Number(route.params.id)
  loading.value = true
  error.value = ''
  try {
    const [detail, all] = await Promise.all([
      getArticle(id),
      getArticles().catch(() => [])
    ])
    article.value = detail
    related.value = all.filter((item) => item.id !== detail.id).slice(0, 3)
  } catch (e) {
    error.value = (e as Error).message
    article.value = null
  } finally {
    loading.value = false
  }
}

useSeo(() => ({
  title: article.value
    ? `${pick(article.value.title, article.value.titleEn)} — 悦享便利`
    : t('seo.newsTitle'),
  description:
    article.value?.seoDescription ||
    pick(article.value?.summary, article.value?.summaryEn) ||
    t('seo.newsDesc')
}))

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="article-page">
    <div class="container">
      <router-link to="/news" class="article-back">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
          <path d="M11 7H3M6 3 2 7l4 4" stroke="currentColor" stroke-width="1.2" />
        </svg>
        {{ t('news.back') }}
      </router-link>

      <div v-if="loading" class="article-loading">
        <div class="skeleton article-loading__title"></div>
        <div class="skeleton article-loading__meta"></div>
        <div class="skeleton article-loading__media"></div>
        <div class="skeleton article-loading__text"></div>
      </div>

      <div v-else-if="error || !article" class="error-state article-error">
        <strong>{{ t('common.error') }}</strong>
        <p>{{ error }}</p>
        <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
      </div>

      <article v-else class="article">
        <header class="article__head">
          <div class="article__meta">
            <span class="tag">{{ categoryLabel }}</span>
            <span>{{ formatDate(article.publishedAt) }}</span>
            <span v-if="article.author">{{ t('news.by') }} {{ article.author }}</span>
          </div>
          <h1 class="display display--lg">{{ pick(article.title, article.titleEn) }}</h1>
          <p v-if="article.summary" class="article__summary">
            {{ pick(article.summary, article.summaryEn) }}
          </p>
        </header>

        <div class="article__cover">
          <SmartImage
            :src="articleImage(article)"
            :alt="pick(article.title, article.titleEn)"
            ratio="16 / 9"
            art="讯"
            label="JOURNAL"
            eager
          />
        </div>

        <div class="article__body article-body" v-html="htmlContent"></div>

        <footer class="article__foot">
          <div class="article__foot-line"></div>
          <div class="article__foot-meta">
            <span>{{ formatDate(article.publishedAt) }}</span>
            <span>{{ article.author }}</span>
            <span>YUEXIANG JOURNAL</span>
          </div>
        </footer>
      </article>

      <section v-if="related.length" class="related">
        <div class="related__head">
          <p class="eyebrow">{{ t('news.related') }}</p>
          <router-link to="/news" class="link-arrow">{{ t('news.back') }}</router-link>
        </div>
        <div class="related__grid">
          <router-link
            v-for="item in related"
            :key="item.id"
            :to="`/news/${item.id}`"
            class="related-card"
          >
            <SmartImage
              :src="articleImage(item)"
              :alt="pick(item.title, item.titleEn)"
              ratio="4 / 3"
              art="讯"
              label="JOURNAL"
            />
            <div class="related-card__body">
              <span>{{ formatDate(item.publishedAt) }}</span>
              <h3>{{ pick(item.title, item.titleEn) }}</h3>
            </div>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.article-page {
  padding: 44px 0 110px;
  background: var(--paper);
}

.article-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--muted);
  transition: color 0.25s ease;
}

.article-back:hover {
  color: var(--tomato);
}

.article {
  width: min(980px, 100%);
  margin: 48px auto 0;
}

.article__head {
  text-align: center;
  max-width: 900px;
  margin-inline: auto;
}

.article__meta {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.article__head h1 {
  margin: 28px 0 22px;
  font-size: clamp(38px, 5.2vw, 74px);
}

.article__summary {
  max-width: 680px;
  margin-inline: auto;
  font-family: var(--font-display);
  font-size: 19px;
  font-style: italic;
  line-height: 1.7;
  color: var(--muted);
}

.article__cover {
  margin: 48px 0 56px;
  border-radius: 20px;
  overflow: hidden;
}

.article__body {
  max-width: 760px;
  margin-inline: auto;
  font-size: 17px;
  line-height: 2.05;
}

.article__foot {
  max-width: 760px;
  margin: 60px auto 0;
}

.article__foot-line {
  height: 1px;
  background: var(--line);
  margin-bottom: 16px;
}

.article__foot-meta {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--muted);
  text-transform: uppercase;
}

.article-loading {
  width: min(980px, 100%);
  margin: 48px auto 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
}

.article-loading__title {
  width: 70%;
  height: 84px;
}

.article-loading__meta {
  width: 30%;
  height: 18px;
}

.article-loading__media {
  width: 100%;
  height: 420px;
  border-radius: 20px;
  margin-top: 20px;
}

.article-loading__text {
  width: 80%;
  height: 240px;
}

.article-error {
  margin-top: 60px;
}

.related {
  margin-top: 100px;
  border-top: 1px solid var(--line);
  padding-top: 32px;
}

.related__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.related__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 26px;
}

.related-card {
  display: block;
}

.related-card :deep(.smart-image) {
  border-radius: 14px;
  overflow: hidden;
}

.related-card__body {
  padding-top: 12px;
}

.related-card__body span {
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.related-card h3 {
  font-size: 17px;
  line-height: 1.5;
  margin-top: 6px;
  transition: color 0.25s ease;
}

.related-card:hover h3 {
  color: var(--tomato);
}

@media (max-width: 800px) {
  .related__grid {
    grid-template-columns: 1fr;
  }

  .article__cover {
    margin: 34px 0 36px;
  }

  .article__body {
    font-size: 16px;
  }
}

@media (max-width: 520px) {
  .article-page {
    padding: 28px 0 80px;
  }

  .article {
    margin-top: 32px;
  }

  .article__head h1 {
    font-size: clamp(34px, 10vw, 48px);
  }

  .article__summary {
    font-size: 16px;
  }

  .article__foot-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
