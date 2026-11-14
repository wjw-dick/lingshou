import axios, { type AxiosRequestConfig } from 'axios'

export const http = axios.create({
  baseURL: '/api/v1/public',
  timeout: 20000
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        return Promise.reject(new Error(body.message || '请求失败，请稍后再试'))
      }
      return body.data
    }
    return body
  },
  (error) => {
    const message =
      error?.response?.data?.message ||
      (error?.code === 'ECONNABORTED' ? '请求超时，请检查网络' : '') ||
      error?.message ||
      '网络异常，请稍后再试'
    return Promise.reject(new Error(message))
  }
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request(config) as unknown as Promise<T>
}

export function getTenant(): string {
  if (typeof window === 'undefined') return 'yxbl'
  const value = new URLSearchParams(window.location.search).get('tenant')
  if (value && /^[A-Za-z0-9_-]{1,64}$/.test(value)) return value
  return 'yxbl'
}

export function withTenant(path: string): string {
  const tenant = getTenant()
  if (tenant === 'yxbl') return path
  const glue = path.includes('?') ? '&' : '?'
  return `${path}${glue}tenant=${encodeURIComponent(tenant)}`
}
