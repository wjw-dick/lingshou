import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('retail_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '请求处理失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (status === 401) {
      localStorage.removeItem('retail_token')
      localStorage.removeItem('retail_user')
      localStorage.removeItem('retail_menus')
      if (!location.hash.includes('/login')) {
        location.href = '/login'
      }
    } else if (status === 403) {
      ElMessage.error(message || '无权访问该资源')
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export const api = {
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return http.get(url, { params, ...config }) as unknown as Promise<T>
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return http.post(url, data, config) as unknown as Promise<T>
  },
  put<T = any>(url: string, data?: any): Promise<T> {
    return http.put(url, data) as unknown as Promise<T>
  },
  del<T = any>(url: string, params?: any): Promise<T> {
    return http.delete(url, { params }) as unknown as Promise<T>
  },
  async upload(file: File, bizType = 'GENERAL'): Promise<any> {
    const form = new FormData()
    form.append('file', file)
    form.append('bizType', bizType)
    return http.post('/tenant/files/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }) as unknown as Promise<any>
  },
  async postForm<T = any>(url: string, form: FormData): Promise<T> {
    return http.post(url, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }) as unknown as Promise<T>
  },
  async uploadMpMaterial(file: File, mediaType: string, name?: string, thumb?: File): Promise<any> {
    const form = new FormData()
    form.append('file', file)
    form.append('mediaType', mediaType)
    if (name) form.append('name', name)
    if (thumb) form.append('thumb', thumb)
    return http.post('/tenant/mp/materials', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }) as unknown as Promise<any>
  },
  async download(url: string, params?: any, fallback = 'export.xlsx') {
    const response = await http.get(url, { params, responseType: 'blob' })
    const blob = new Blob([response as any])
    const disposition = (response as any)?.headers?.['content-disposition'] || ''
    let filename = fallback
    const match = /filename\*?=(?:UTF-8'')?([^;]+)/i.exec(disposition)
    if (match) {
      filename = decodeURIComponent(match[1].replace(/"/g, ''))
    }
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    link.click()
    URL.revokeObjectURL(link.href)
  }
}

export default http
