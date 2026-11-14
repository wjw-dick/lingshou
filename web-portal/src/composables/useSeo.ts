import { watchEffect } from 'vue'
import { useLang } from '@/i18n'

interface SeoOptions {
  title: string
  description: string
  hreflang?: boolean
}

function ensureMeta(name: string): HTMLMetaElement {
  let el = document.head.querySelector<HTMLMetaElement>(`meta[name="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('name', name)
    document.head.appendChild(el)
  }
  return el
}

function setHreflang() {
  document.head.querySelectorAll('link[data-seo-hreflang]').forEach((node) => node.remove())
  const url = new URL(window.location.href)
  const links = [
    { lang: 'zh-CN', value: 'zh-CN' },
    { lang: 'en', value: 'en' }
  ]
  for (const item of links) {
    const link = document.createElement('link')
    link.rel = 'alternate'
    link.hreflang = item.lang
    const target = new URL(url.toString())
    target.searchParams.set('lang', item.value)
    link.href = target.toString()
    link.setAttribute('data-seo-hreflang', '1')
    document.head.appendChild(link)
  }
}

export function useSeo(getter: () => SeoOptions) {
  const { lang } = useLang()
  watchEffect(() => {
    lang.value
    const options = getter()
    if (options.title) document.title = options.title
    ensureMeta('description').setAttribute('content', options.description || '')
    if (options.hreflang) {
      setHreflang()
    } else {
      document.head.querySelectorAll('link[data-seo-hreflang]').forEach((node) => node.remove())
    }
  })
}
