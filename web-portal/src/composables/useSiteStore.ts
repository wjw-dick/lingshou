import { computed, reactive } from 'vue'
import { getSite } from '@/api/site'
import type { SiteInfo } from '@/types'

interface SiteState {
  site: SiteInfo | null
  loading: boolean
  loaded: boolean
  error: string
}

const state = reactive<SiteState>({
  site: null,
  loading: false,
  loaded: false,
  error: ''
})

let pending: Promise<SiteInfo | null> | null = null

async function load(force = false): Promise<SiteInfo | null> {
  if (state.loaded && !force) return state.site
  if (pending && !force) return pending
  state.loading = true
  state.error = ''
  pending = getSite()
    .then((site) => {
      state.site = site
      state.loaded = true
      return site
    })
    .catch((error: Error) => {
      state.error = error.message || '站点信息加载失败'
      return null
    })
    .finally(() => {
      state.loading = false
      pending = null
    })
  return pending
}

export function useSiteStore() {
  const site = computed(() => state.site)
  const configs = computed<Record<string, string>>(() => state.site?.configs || {})
  const config = (key: string, fallback = '') => configs.value[key] || fallback
  const menus = computed(() => state.site?.menus || [])
  return { state, site, configs, config, menus, load }
}
