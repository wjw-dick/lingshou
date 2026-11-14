import { getTenant, request } from './http'
import type {
  CmsArticle,
  DeliveryRule,
  FranchiseApplication,
  HomeData,
  ProductCategory,
  ProductRow,
  PromotionRow,
  SiteInfo,
  Store
} from '@/types'

const base = () => `/site/${encodeURIComponent(getTenant())}`

export function getSite(): Promise<SiteInfo> {
  return request<SiteInfo>({ url: base(), method: 'GET' })
}

export function getHome(): Promise<HomeData> {
  return request<HomeData>({ url: `${base()}/home`, method: 'GET' })
}

export function getArticles(): Promise<CmsArticle[]> {
  return request<CmsArticle[]>({ url: `${base()}/articles`, method: 'GET' })
}

export function getArticle(id: number | string): Promise<CmsArticle> {
  return request<CmsArticle>({ url: `${base()}/articles/${id}`, method: 'GET' })
}

export function getStores(params?: { region?: string; keyword?: string }): Promise<Store[]> {
  return request<Store[]>({
    url: `${base()}/stores`,
    method: 'GET',
    params: {
      region: params?.region || undefined,
      keyword: params?.keyword || undefined
    }
  })
}

export function getCategories(): Promise<ProductCategory[]> {
  return request<ProductCategory[]>({ url: `${base()}/categories`, method: 'GET' })
}

export function getProducts(params?: {
  categoryId?: number | null
  keyword?: string
  limit?: number
}): Promise<ProductRow[]> {
  return request<ProductRow[]>({
    url: `${base()}/products`,
    method: 'GET',
    params: {
      categoryId: params?.categoryId || undefined,
      keyword: params?.keyword || undefined,
      limit: params?.limit ?? 50
    }
  })
}

export function getProduct(skuId: number | string): Promise<ProductRow> {
  return request<ProductRow>({ url: `${base()}/products/${skuId}`, method: 'GET' })
}

export function getPromotions(): Promise<PromotionRow[]> {
  return request<PromotionRow[]>({ url: `${base()}/promotions`, method: 'GET' })
}

export function getDeliveryRule(storeId: number | string): Promise<DeliveryRule | null> {
  return request<DeliveryRule | null>({
    url: `${base()}/stores/${storeId}/delivery-rule`,
    method: 'GET'
  })
}

export function applyFranchise(payload: FranchiseApplication): Promise<void> {
  return request<void>({
    url: `${base()}/franchise-applications`,
    method: 'POST',
    data: payload
  })
}
