export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface Banner {
  id: number
  tenantId?: number
  position?: string
  title: string
  image: string
  link: string
  sortNo: number
  status?: string
}

export interface SiteMenu {
  id: number
  title: string
  titleEn: string
  path: string
  icon?: string
  sortNo: number
  visible?: number
  status?: string
}

export interface SiteLanguage {
  id?: number
  code: string
  name: string
  enabled: number
  sortNo?: number
}

export interface SiteInfo {
  tenantCode: string
  tenantName: string
  configs: Record<string, string>
  languages: SiteLanguage[]
  menus: SiteMenu[]
  banners: Banner[]
}

export type Numeric = number | string | null | undefined

export interface Store {
  id: number
  code: string
  name: string
  type: string
  region: string
  address: string
  lng: Numeric
  lat: Numeric
  businessHours: string
  phone: string
  managerName?: string
  managerPhone?: string
  deliveryRadiusM?: number
  deliveryEnabled?: number
  onlineEnabled?: number
  status: string
  openedAt?: string
  images?: string
}

export interface ProductCategory {
  id: number
  parentId: number
  name: string
  nameEn: string
  icon?: string
  sortNo: number
  nearExpiryDays?: number
  status?: string
}

export interface ProductSpu {
  id: number
  spuCode?: string
  name: string
  nameEn?: string
  categoryId: number
  brandId?: number
  unit?: string
  spec?: string
  storageCondition?: string
  shelfLifeDays?: number
  origin?: string
  ingredients?: string
  allergens?: string
  isTobacco?: number
  onlineSale?: number
  status?: string
  mainImage?: string
  images?: string
  detailHtml?: string
  generatedPriceGuide?: Numeric
  priceGuide?: Numeric
  priceRetail?: Numeric
  priceMember?: Numeric
  tags?: string
}

export interface ProductSku {
  id: number
  spuId: number
  skuCode?: string
  barcode?: string
  spec?: string
  unit?: string
  priceGuide?: Numeric
  priceRetail?: Numeric
  priceMember?: Numeric
  status?: string
}

export interface ProductRow {
  spu: ProductSpu
  sku: ProductSku
}

export interface CmsArticle {
  id: number
  category: string
  title: string
  titleEn?: string
  summary?: string
  summaryEn?: string
  cover?: string
  content?: string
  contentEn?: string
  author?: string
  status?: string
  publishedAt?: string
  seoKeywords?: string
  seoDescription?: string
}

export interface Coupon {
  id: number
  name: string
  type: string
  thresholdAmount: Numeric
  discountAmount: Numeric
  discountRate: Numeric
  skuId?: number | null
  totalQty?: number
  issuedQty?: number
  perUserLimit?: number
  validFrom: string
  validTo: string
  status?: string
}

export interface MemberLevel {
  id: number
  code: string
  name: string
  minPoints: number
  discount: Numeric
  benefits?: string
}

export interface HomeData {
  banners: Banner[]
  categories: ProductCategory[]
  stores: Store[]
  products: ProductRow[]
  coupons: Coupon[]
  levels: MemberLevel[]
}

export interface DeliveryRule {
  id: number
  storeId: number
  minOrderAmount: Numeric
  deliveryFee: Numeric
  freeThreshold: Numeric
  radiusM: number
  timeSlots?: string
  enabled: number
}

export interface Promotion {
  id: number
  name: string
  type: string
  startAt: string
  endAt: string
  ruleJson?: string
  storeScope?: string
  status?: string
}

export interface PromotionItemRow {
  skuId: number
  skuName: string
  image: string
  originalPrice: Numeric
  promoPrice: Numeric
}

export interface PromotionRow {
  promotion: Promotion
  items: PromotionItemRow[]
}

export interface FranchiseApplication {
  company: string
  contact: string
  phone: string
  city: string
  amount: string
  remark: string
}
