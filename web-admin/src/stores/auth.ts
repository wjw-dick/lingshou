import { defineStore } from 'pinia'
import { api } from '@/api/http'

export interface MenuNode {
  id: number
  parentId: number
  title: string
  titleEn?: string
  icon?: string
  path?: string
  component?: string
  permission?: string
  sortNo?: number
  children?: MenuNode[]
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  userType: string
  roleCode: string
  tenantId?: number
  tenantCode?: string
  tenantName?: string
  storeId?: number
  supplierId?: number
  franchiseeId?: number
  memberId?: number
}

interface AuthState {
  token: string
  user: UserInfo | null
  menus: MenuNode[]
}

function readMenus(): MenuNode[] {
  try {
    return JSON.parse(localStorage.getItem('retail_menus') || '[]')
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('retail_token') || '',
    user: JSON.parse(localStorage.getItem('retail_user') || 'null'),
    menus: readMenus()
  }),
  getters: {
    isPlatform: (state) => state.user?.userType === 'PLATFORM',
    isTenant: (state) => state.user?.userType === 'TENANT',
    tenantName: (state) => state.user?.tenantName || '',
    displayName: (state) => state.user?.realName || state.user?.username || ''
  },
  actions: {
    persist() {
      localStorage.setItem('retail_token', this.token)
      localStorage.setItem('retail_user', JSON.stringify(this.user))
      localStorage.setItem('retail_menus', JSON.stringify(this.menus))
    },
    async loginTenant(payload: { tenantCode: string; username: string; password: string }) {
      const data: any = await api.post('/auth/login', payload)
      this.token = data.token
      this.user = data.user
      this.menus = data.menus || []
      this.persist()
      return data
    },
    async loginPlatform(payload: { username: string; password: string }) {
      const data: any = await api.post('/platform/auth/login', payload)
      this.token = data.token
      this.user = data.user
      this.menus = data.menus || []
      this.persist()
      return data
    },
    async refresh() {
      if (!this.token) return
      const data: any = await api.get('/auth/me')
      this.user = data.user
      this.menus = data.menus || []
      this.persist()
    },
    async logout() {
      try {
        if (this.token) await api.post('/auth/logout')
      } catch {
        // ignore
      }
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('retail_token')
      localStorage.removeItem('retail_user')
      localStorage.removeItem('retail_menus')
    }
  }
})
