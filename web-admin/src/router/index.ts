import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('@/views/tenant/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'stores', component: () => import('@/views/tenant/Stores.vue'), meta: { title: '门店管理' } },
      { path: 'products/categories', component: () => import('@/views/tenant/product/Categories.vue'), meta: { title: '商品分类' } },
      { path: 'products/spus', component: () => import('@/views/tenant/product/Spus.vue'), meta: { title: '商品档案' } },
      { path: 'purchase/orders', component: () => import('@/views/tenant/purchase/Orders.vue'), meta: { title: '采购单' } },
      { path: 'purchase/supply', component: () => import('@/views/tenant/purchase/Supply.vue'), meta: { title: '供货单' } },
      { path: 'purchase/receive', component: () => import('@/views/tenant/purchase/Receive.vue'), meta: { title: '收货与差异' } },
      { path: 'inventory/stocks', component: () => import('@/views/tenant/inventory/Stocks.vue'), meta: { title: '库存查询' } },
      { path: 'inventory/transfers', component: () => import('@/views/tenant/inventory/Transfers.vue'), meta: { title: '调拨单' } },
      { path: 'inventory/checks', component: () => import('@/views/tenant/inventory/Checks.vue'), meta: { title: '盘点单' } },
      { path: 'inventory/losses', component: () => import('@/views/tenant/inventory/Losses.vue'), meta: { title: '报损单' } },
      { path: 'sales/orders', component: () => import('@/views/tenant/sales/Orders.vue'), meta: { title: '订单中心' } },
      { path: 'sales/pos', component: () => import('@/views/tenant/sales/Pos.vue'), meta: { title: '轻量收银' } },
      { path: 'sales/shifts', component: () => import('@/views/tenant/sales/Shifts.vue'), meta: { title: '交接班' } },
      { path: 'sales/settlements', component: () => import('@/views/tenant/sales/Settlements.vue'), meta: { title: '日结' } },
      { path: 'members/list', component: () => import('@/views/tenant/member/List.vue'), meta: { title: '会员列表' } },
      { path: 'members/coupons', component: () => import('@/views/tenant/member/Coupons.vue'), meta: { title: '优惠券' } },
      { path: 'members/promotions', component: () => import('@/views/tenant/member/Promotions.vue'), meta: { title: '营销活动' } },
      { path: 'suppliers/list', component: () => import('@/views/tenant/supplier/List.vue'), meta: { title: '供应商' } },
      { path: 'suppliers/settlements', component: () => import('@/views/tenant/supplier/Statements.vue'), meta: { title: '供应商对账' } },
      { path: 'franchise/list', component: () => import('@/views/tenant/franchise/List.vue'), meta: { title: '加盟商与分账' } },
      { path: 'reports', component: () => import('@/views/tenant/report/Index.vue'), meta: { title: '报表看板' } },
      { path: 'mp/account', component: () => import('@/views/tenant/mp/Account.vue'), meta: { title: '账号管理' } },
      { path: 'mp/statistics', component: () => import('@/views/tenant/mp/Statistics.vue'), meta: { title: '数据统计' } },
      { path: 'mp/tag', component: () => import('@/views/tenant/mp/Tag.vue'), meta: { title: '标签管理' } },
      { path: 'mp/user', component: () => import('@/views/tenant/mp/User.vue'), meta: { title: '粉丝管理' } },
      { path: 'mp/message', component: () => import('@/views/tenant/mp/Message.vue'), meta: { title: '消息管理' } },
      { path: 'mp/message-template', component: () => import('@/views/tenant/mp/Template.vue'), meta: { title: '模版消息' } },
      { path: 'mp/menu', component: () => import('@/views/tenant/mp/Menu.vue'), meta: { title: '菜单管理' } },
      { path: 'mp/auto-reply', component: () => import('@/views/tenant/mp/AutoReply.vue'), meta: { title: '自动回复' } },
      { path: 'mp/material', component: () => import('@/views/tenant/mp/Material.vue'), meta: { title: '素材管理' } },
      { path: 'mp/draft', component: () => import('@/views/tenant/mp/Draft.vue'), meta: { title: '图文草稿箱' } },
      { path: 'mp/free-publish', component: () => import('@/views/tenant/mp/Publish.vue'), meta: { title: '发表记录' } },
      { path: 'cms/articles', component: () => import('@/views/tenant/cms/Articles.vue'), meta: { title: '文章管理' } },
      { path: 'cms/banners', component: () => import('@/views/tenant/cms/Banners.vue'), meta: { title: 'Banner' } },
      { path: 'cms/franchise-applications', component: () => import('@/views/tenant/cms/FranchiseApplications.vue'), meta: { title: '加盟申请' } },
      { path: 'system/config', component: () => import('@/views/tenant/system/Config.vue'), meta: { title: '全局参数' } },
      { path: 'system/menus', component: () => import('@/views/tenant/system/Menus.vue'), meta: { title: '菜单可视化' } },
      { path: 'system/messages', component: () => import('@/views/tenant/system/Messages.vue'), meta: { title: '消息配置' } },
      { path: 'system/storage', component: () => import('@/views/tenant/system/Storage.vue'), meta: { title: '对象存储' } },
      { path: 'system/sso', component: () => import('@/views/tenant/system/Sso.vue'), meta: { title: 'SSO 配置' } },
      { path: 'system/users', component: () => import('@/views/tenant/system/Users.vue'), meta: { title: '用户与角色' } },
      { path: 'system/audit', component: () => import('@/views/tenant/system/Audit.vue'), meta: { title: '审计日志' } },
      { path: 'system/languages', component: () => import('@/views/tenant/system/Languages.vue'), meta: { title: '多语言' } }
    ]
  },
  {
    path: '/platform',
    component: () => import('@/layout/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/platform/dashboard' },
      { path: 'dashboard', component: () => import('@/views/platform/Dashboard.vue'), meta: { title: '经营总览', platform: true } },
      { path: 'tenants', component: () => import('@/views/platform/Tenants.vue'), meta: { title: '租户管理', platform: true } },
      { path: 'plans', component: () => import('@/views/platform/Plans.vue'), meta: { title: '套餐与配额', platform: true } },
      { path: 'bills', component: () => import('@/views/platform/Bills.vue'), meta: { title: '账单与计费', platform: true } },
      { path: 'announcements', component: () => import('@/views/platform/Announcements.vue'), meta: { title: '平台公告', platform: true } },
      { path: 'audits', component: () => import('@/views/platform/Audits.vue'), meta: { title: '平台审计', platform: true } },
      { path: 'accounts', component: () => import('@/views/platform/Accounts.vue'), meta: { title: '平台账号', platform: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path === '/login') {
    if (auth.token) {
      return auth.isPlatform ? '/platform/dashboard' : '/dashboard'
    }
    return true
  }
  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path.startsWith('/platform') && !auth.isPlatform) {
    return '/dashboard'
  }
  if (!to.path.startsWith('/platform') && auth.isPlatform) {
    return '/platform/dashboard'
  }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '零售便利'} · 悦享便利 SaaS`
})

export default router
