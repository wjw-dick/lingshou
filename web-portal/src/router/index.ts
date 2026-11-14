import { createRouter, createWebHistory } from 'vue-router'
import SiteLayout from '@/layouts/SiteLayout.vue'
import H5Layout from '@/layouts/H5Layout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: SiteLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/pages/HomePage.vue') },
        { path: 'stores', name: 'stores', component: () => import('@/pages/StoresPage.vue') },
        { path: 'products', name: 'products', component: () => import('@/pages/ProductsPage.vue') },
        { path: 'franchise', name: 'franchise', component: () => import('@/pages/FranchisePage.vue') },
        { path: 'news', name: 'news', component: () => import('@/pages/NewsPage.vue') },
        { path: 'news/:id', name: 'news-detail', component: () => import('@/pages/NewsDetailPage.vue') },
        { path: 'contact', name: 'contact', component: () => import('@/pages/ContactPage.vue') },
        { path: ':pathMatch(.*)*', name: 'not-found', component: () => import('@/pages/NotFoundPage.vue') }
      ]
    },
    {
      path: '/h5',
      component: H5Layout,
      children: [
        { path: '', redirect: '/h5/deals' },
        {
          path: 'deals',
          name: 'h5-deals',
          component: () => import('@/pages/h5/DealsPage.vue')
        },
        {
          path: 'newcomer',
          name: 'h5-newcomer',
          component: () => import('@/pages/h5/NewcomerPage.vue')
        },
        {
          path: 'franchise',
          name: 'h5-franchise',
          component: () => import('@/pages/h5/H5FranchisePage.vue')
        },
        {
          path: 'product/:skuId',
          name: 'h5-product',
          component: () => import('@/pages/h5/H5ProductPage.vue'),
          meta: { back: true }
        },
        {
          path: 'store/:storeId',
          name: 'h5-store',
          component: () => import('@/pages/h5/H5StorePage.vue'),
          meta: { back: true }
        },
        {
          path: 'pay-return',
          name: 'h5-pay-return',
          component: () => import('@/pages/h5/PayReturnPage.vue'),
          meta: { back: true }
        }
      ]
    }
  ],
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (typeof window === 'undefined') return true
  const tenant = new URLSearchParams(window.location.search).get('tenant')
  if (tenant && tenant !== 'yxbl' && !to.query.tenant) {
    return { path: to.path, query: { ...to.query, tenant }, hash: to.hash }
  }
  return true
})

export default router
