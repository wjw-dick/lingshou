import type { Numeric } from '@/types'

export function money(value: Numeric, symbol = '¥'): string {
  const n = Number(value ?? 0)
  if (!Number.isFinite(n)) return `${symbol}0.00`
  return `${symbol}${n.toFixed(2)}`
}

export function numberValue(value: Numeric): number {
  const n = Number(value ?? 0)
  return Number.isFinite(n) ? n : 0
}

export function formatDate(value?: string | null, withTime = false): string {
  if (!value) return '—'
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  const datePart = `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())}`
  return withTime ? `${datePart} ${pad(date.getHours())}:${pad(date.getMinutes())}` : datePart
}

export function formatShortDate(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getDate())} / ${pad(date.getMonth() + 1)}`
}

export function storageLabel(code?: string): string {
  const map: Record<string, string> = {
    COLD: '冷藏',
    FROZEN: '冷冻',
    NORMAL: '常温',
    ROOM: '常温'
  }
  return map[code || ''] || '常温'
}

export function shelfLifeLabel(days?: number): string {
  if (!days) return '短保'
  return days >= 365 ? '保质期 12 个月' : `保质期 ${days} 天`
}

export function typeLabel(code: string): string {
  const map: Record<string, string> = {
    DIRECT: '直营',
    FRANCHISE: '加盟',
    COOPERATION: '合作'
  }
  return map[code] || code
}

export function promoTypeLabel(code: string): string {
  const map: Record<string, string> = {
    DISCOUNT: '折扣',
    SECOND_HALF: '第二件半价',
    GROUP: '组合优惠',
    POINTS_EXCHANGE: '积分兑换',
    FULL_REDUCTION: '满减券',
    PRODUCT: '单品券'
  }
  return map[code] || code
}

export function articleCategoryLabel(code: string): string {
  const map: Record<string, string> = {
    BRAND: '品牌动态',
    FRESH: '鲜食上新',
    FRANCHISE: '加盟资讯',
    NOTICE: '门店公告'
  }
  return map[code] || '品牌资讯'
}

export function parseJsonArray<T = unknown>(value?: string | null): T[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? (parsed as T[]) : []
  } catch {
    return []
  }
}

export function amapUri(store: { lng: Numeric; lat: Numeric; name: string }): string {
  const lng = numberValue(store.lng)
  const lat = numberValue(store.lat)
  return `https://uri.amap.com/marker?position=${lng},${lat}&name=${encodeURIComponent(
    store.name
  )}&src=yuexiang-portal&coordinate=gaode&callnative=1`
}

export function telUri(phone?: string | null): string {
  return `tel:${(phone || '').replace(/[^\d+]/g, '')}`
}

export function discountText(coupon: {
  type: string
  thresholdAmount: Numeric
  discountAmount: Numeric
  discountRate: Numeric
}): string {
  if (coupon.type === 'DISCOUNT') {
    const rate = numberValue(coupon.discountRate)
    const normalized = rate > 10 ? rate / 10 : rate
    return `${Number(normalized.toFixed(1))} 折`
  }
  return money(coupon.discountAmount, '¥')
}

export function thresholdText(coupon: { thresholdAmount: Numeric }): string {
  const threshold = numberValue(coupon.thresholdAmount)
  return threshold > 0 ? `满 ${threshold.toFixed(0)} 元可用` : '无门槛'
}
