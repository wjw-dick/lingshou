import type { CmsArticle, ProductSpu } from '@/types'

const ENDPOINT = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image'

export function aiImage(prompt: string, size = 'landscape_16_9'): string {
  return `${ENDPOINT}?prompt=${encodeURIComponent(prompt)}&image_size=${size}`
}

export const IMAGES = {
  hero: aiImage('night'),
  street: aiImage('street'),
  city: aiImage('city'),
  fresh: aiImage('food'),
  coffee: aiImage('coffee'),
  icedCoffee: aiImage('iced coffee'),
  store: aiImage('coffee shop interior'),
  franchise: aiImage('business'),
  paper: aiImage('paper')
}

const usable = (value?: string | null): string | null => {
  if (!value) return null
  if (/^https?:\/\//i.test(value)) return value
  if (value.startsWith('/uploads/')) return value
  return null
}

export function productImage(spu?: ProductSpu | null): string | null {
  const remote = usable(spu?.mainImage)
  if (remote) return remote
  const name = spu?.name || ''
  if (/抽纸|纸巾|日用/.test(name)) return IMAGES.paper
  if (/咖啡|美式|拿铁|摩卡/.test(name)) return IMAGES.coffee
  if (/可乐|汽水/.test(name)) return IMAGES.icedCoffee
  if (/关东煮|便当|饭团/.test(name)) return IMAGES.fresh
  return null
}

export function articleImage(article?: CmsArticle | null): string | null {
  const remote = usable(article?.cover)
  if (remote) return remote
  if (article?.category === 'FRANCHISE') return IMAGES.franchise
  if (article?.category === 'FRESH') return IMAGES.fresh
  if (article?.category === 'BRAND') return IMAGES.street
  return IMAGES.city
}

export function categoryImage(categoryId?: number | null, name?: string): string {
  if (categoryId === 1 || /鲜食/.test(name || '')) return IMAGES.fresh
  if (categoryId === 2 || /饮料/.test(name || '')) return IMAGES.icedCoffee
  if (categoryId === 4 || /日用/.test(name || '')) return IMAGES.paper
  if (categoryId === 3 || /零食/.test(name || '')) return IMAGES.fresh
  return IMAGES.store
}

export function promoItemImage(name: string): string | null {
  if (/可乐|汽水/.test(name)) return IMAGES.icedCoffee
  if (/咖啡|美式|拿铁/.test(name)) return IMAGES.coffee
  if (/关东煮|便当|饭团/.test(name)) return IMAGES.fresh
  if (/抽纸|纸巾/.test(name)) return IMAGES.paper
  return null
}

export function miniProgramQr(target: string): string {
  return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=0&data=${encodeURIComponent(
    target
  )}`
}
