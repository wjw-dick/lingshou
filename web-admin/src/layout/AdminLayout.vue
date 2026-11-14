<template>
  <div class="admin-shell">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-mark">悦</div>
        <div v-show="!collapsed" class="brand-text">
          <strong>{{ auth.isPlatform ? '平台运营端' : '悦享便利' }}</strong>
          <span>{{ auth.isPlatform ? 'Retail SaaS Console' : '连锁零售 SaaS' }}</span>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#aebbd1"
          active-text-color="#ffffff"
          router
          unique-opened
        >
          <side-menu-item v-for="menu in menus" :key="menu.id" :item="menu" />
        </el-menu>
      </el-scrollbar>
      <div v-show="!collapsed" class="sidebar-foot">
        <el-icon><CircleCheckFilled /></el-icon>
        <span>服务运行正常</span>
      </div>
    </aside>

    <section class="main">
      <header class="topbar">
        <div class="topbar-left">
          <el-button text circle class="collapse-btn" @click="collapsed = !collapsed">
            <el-icon :size="18"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ auth.isPlatform ? '平台运营' : '租户后台' }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <span v-if="!auth.isPlatform" class="tenant-chip">
            <el-icon><Shop /></el-icon>{{ auth.tenantName || '当前租户' }}
          </span>
          <el-tag v-else type="warning" effect="light" round>平台侧</el-tag>
          <el-tooltip content="刷新当前页" placement="bottom">
            <el-button text circle @click="reload"><el-icon><Refresh /></el-icon></el-button>
          </el-tooltip>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-block">
              <div class="avatar">{{ (auth.displayName || 'A').slice(0, 1) }}</div>
              <div class="user-meta">
                <strong>{{ auth.displayName }}</strong>
                <span>{{ roleLabel }}</span>
              </div>
              <el-icon class="muted"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="refresh">刷新登录信息</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="tabs-bar">
        <div
          v-for="tab in tabs"
          :key="tab.path"
          class="tab-item"
          :class="{ active: tab.path === route.path }"
          @click="router.push(tab.path)"
        >
          <span>{{ tab.title }}</span>
          <el-icon v-if="tabs.length > 1" class="tab-close" @click.stop="closeTab(tab.path)">
            <Close />
          </el-icon>
        </div>
      </div>

      <el-scrollbar class="content-scroll">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </el-scrollbar>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore, type MenuNode } from '@/stores/auth'
import SideMenuItem from '@/components/SideMenuItem.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const tabs = ref<{ path: string; title: string }[]>([])

const platformFallback: MenuNode[] = [
  { id: 1, parentId: 0, title: '经营总览', icon: 'DataBoard', path: '/platform/dashboard' },
  { id: 2, parentId: 0, title: '租户管理', icon: 'OfficeBuilding', path: '/platform/tenants' },
  { id: 3, parentId: 0, title: '套餐与配额', icon: 'Tickets', path: '/platform/plans' },
  { id: 4, parentId: 0, title: '账单与计费', icon: 'Money', path: '/platform/bills' },
  { id: 5, parentId: 0, title: '平台公告', icon: 'Bell', path: '/platform/announcements' },
  { id: 6, parentId: 0, title: '平台审计', icon: 'Document', path: '/platform/audits' },
  { id: 7, parentId: 0, title: '平台账号', icon: 'User', path: '/platform/accounts' }
]

const menus = computed(() => {
  if (auth.isPlatform && (!auth.menus || auth.menus.length === 0)) {
    return platformFallback
  }
  return auth.menus || []
})

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    PLATFORM_ADMIN: '平台超管',
    PLATFORM_OPERATOR: '平台运营',
    PLATFORM_FINANCE: '平台财务',
    TENANT_ADMIN: '租户管理员',
    STORE_MANAGER: '店长',
    CASHIER: '收银员',
    OPERATIONS: '运营',
    PURCHASER: '采购',
    FINANCE: '财务',
    SUPPLIER: '供应商',
    FRANCHISEE: '加盟商'
  }
  return map[auth.user?.roleCode || ''] || auth.user?.roleCode || '成员'
})

function addTab() {
  const title = String(route.meta.title || '')
  if (!title) return
  if (!tabs.value.find((t) => t.path === route.path)) {
    tabs.value.push({ path: route.path, title })
  }
}

watch(() => route.path, addTab, { immediate: true })

function closeTab(path: string) {
  const index = tabs.value.findIndex((t) => t.path === path)
  tabs.value = tabs.value.filter((t) => t.path !== path)
  if (route.path === path) {
    const next = tabs.value[Math.max(0, index - 1)]
    router.push(next ? next.path : (auth.isPlatform ? '/platform/dashboard' : '/dashboard'))
  }
}

function reload() {
  location.reload()
}

async function onCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号？', '退出登录', { type: 'warning' })
    await auth.logout()
    router.push('/login')
    return
  }
  if (command === 'refresh') {
    try {
      await auth.refresh()
      location.reload()
    } catch {
      // interceptor already提示
    }
  }
}
</script>

<style scoped lang="scss">
.admin-shell {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.sidebar {
  width: 236px;
  background: linear-gradient(180deg, #0e1729 0%, #101d36 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  flex-shrink: 0;

  &.collapsed { width: 68px; }

  .brand {
    height: 68px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 18px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  .brand-mark {
    width: 38px;
    height: 38px;
    border-radius: 11px;
    background: linear-gradient(135deg, #2f7df6, #14b8a6);
    display: grid;
    place-items: center;
    font-weight: 700;
    font-size: 19px;
    box-shadow: 0 6px 16px rgba(31, 111, 235, 0.4);
    flex-shrink: 0;
  }

  .brand-text {
    display: flex;
    flex-direction: column;
    line-height: 1.25;
    overflow: hidden;
    white-space: nowrap;

    strong { font-size: 15px; letter-spacing: 0.04em; }
    span { font-size: 10px; color: #6f819d; letter-spacing: 0.08em; text-transform: uppercase; margin-top: 3px; }
  }

  .menu-scroll { flex: 1; }

  :deep(.el-menu) { border-right: none; padding: 10px 8px; }
  :deep(.el-menu-item), :deep(.el-sub-menu__title) {
    height: 44px;
    line-height: 44px;
    border-radius: 8px;
    margin-bottom: 4px;
    font-size: 13.5px;
  }
  :deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) {
    background: rgba(255, 255, 255, 0.06) !important;
  }
  :deep(.el-menu-item.is-active) {
    background: linear-gradient(90deg, rgba(47, 125, 246, 0.95), rgba(47, 125, 246, 0.65)) !important;
    box-shadow: 0 6px 16px rgba(47, 125, 246, 0.25);
  }
  :deep(.el-sub-menu .el-menu-item) { padding-left: 46px !important; }

  .sidebar-foot {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 14px 20px;
    font-size: 11.5px;
    color: #7e8fab;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    .el-icon { color: #30c48d; }
  }
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  height: 62px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.topbar-left { display: flex; align-items: center; gap: 10px; }
.collapse-btn { color: #667085; }

.topbar-right { display: flex; align-items: center; gap: 14px; }

.tenant-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #344054;
  background: #f2f5fa;
  border: 1px solid #e4e7ec;
  padding: 6px 10px;
  border-radius: 999px;
}

.user-block {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
  &:hover { background: #f5f7fb; }
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1f6feb, #14b8a6);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 600;
  font-size: 14px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  strong { font-size: 13px; }
  span { font-size: 11px; color: #98a2b3; margin-top: 2px; }
}

.tabs-bar {
  height: 40px;
  background: #fff;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 16px;
  overflow-x: auto;
  flex-shrink: 0;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 7px;
  font-size: 12.5px;
  color: #667085;
  cursor: pointer;
  white-space: nowrap;

  &.active {
    background: var(--brand-soft);
    color: var(--brand);
    font-weight: 600;
  }

  .tab-close { font-size: 12px; opacity: 0.6; &:hover { opacity: 1; } }
}

.content-scroll { flex: 1; background: var(--bg); }

.page-fade-enter-active, .page-fade-leave-active { transition: opacity 0.16s ease, transform 0.16s ease; }
.page-fade-enter-from { opacity: 0; transform: translateY(6px); }
.page-fade-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
