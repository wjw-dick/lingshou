const api = require('../../utils/request')
const format = require('../../utils/format')

function tomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return date.getFullYear() + '-' + (month < 10 ? '0' + month : month) + '-' + (day < 10 ? '0' + day : day)
}

Page({
  data: {
    hasToken: false,
    loading: true,
    error: '',
    stores: [],
    storeNames: [],
    storeIndex: 0,
    storeId: '',
    storeName: '',
    keyword: '',
    products: [],
    expectedAt: tomorrow(),
    remark: '',
    selectedCount: 0,
    totalText: '0.00',
    submitting: false
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({ hasToken: hasToken })
    if (!hasToken) {
      this.setData({ loading: false })
      return
    }
    this.loadAll()
  },

  loadAll() {
    const that = this
    this.setData({ loading: true, error: '' })
    Promise.all([
      api.get('/franchisee/stores'),
      api.get('/public/site/yxbl/products', { keyword: this.data.keyword, limit: 50 }, { auth: false })
    ]).then(function (results) {
      const stores = (results[0] || []).map(function (row) {
        return row.store || {}
      })
      const storeNames = stores.map(function (store) {
        return store.name
      })
      let storeIndex = that.data.storeIndex
      if (storeIndex >= stores.length) {
        storeIndex = 0
      }
      const products = (results[1] || []).map(function (row) {
        const sku = row.sku || {}
        const spu = row.spu || {}
        return {
          skuId: sku.id,
          name: spu.name || sku.skuCode || '商品',
          initial: (spu.name || '悦').slice(0, 1),
          image: spu.mainImage || '',
          spec: sku.spec || spu.spec || '',
          unit: sku.unit || spu.unit || '',
          price: Number(sku.priceFranchise) || 0,
          priceText: format.money(sku.priceFranchise),
          qty: 0
        }
      })
      that.setData({
        stores: stores,
        storeNames: storeNames,
        storeIndex: storeIndex,
        storeId: stores.length ? stores[storeIndex].id : '',
        storeName: storeNames[storeIndex] || '',
        products: products,
        loading: false,
        error: ''
      })
      that.calcTotal()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '报单数据加载失败'
      })
    })
  },

  onStoreChange(event) {
    const index = Number(event.detail.value)
    const store = this.data.stores[index]
    this.setData({
      storeIndex: index,
      storeId: store ? store.id : '',
      storeName: store ? store.name : ''
    })
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value })
  },

  search() {
    this.loadAll()
  },

  onDateChange(event) {
    this.setData({ expectedAt: event.detail.value })
  },

  onRemarkInput(event) {
    this.setData({ remark: event.detail.value })
  },

  plus(event) {
    const index = Number(event.currentTarget.dataset.index)
    const products = this.data.products
    products[index].qty += 1
    this.setData({ products: products })
    this.calcTotal()
  },

  minus(event) {
    const index = Number(event.currentTarget.dataset.index)
    const products = this.data.products
    if (products[index].qty > 0) {
      products[index].qty -= 1
    }
    this.setData({ products: products })
    this.calcTotal()
  },

  calcTotal() {
    let total = 0
    let count = 0
    this.data.products.forEach(function (product) {
      total += product.price * product.qty
      count += product.qty
    })
    this.setData({
      totalText: format.money(total),
      selectedCount: count
    })
  },

  submit() {
    if (!this.data.hasToken) {
      this.goLogin()
      return
    }
    if (!this.data.storeId) {
      wx.showToast({ title: '请选择报单门店', icon: 'none' })
      return
    }
    const items = this.data.products.filter(function (product) {
      return product.qty > 0
    }).map(function (product) {
      return { skuId: product.skuId, qty: product.qty }
    })
    if (!items.length) {
      wx.showToast({ title: '请选择报单商品', icon: 'none' })
      return
    }
    const that = this
    this.setData({ submitting: true })
    api.post('/franchisee/supply-orders', {
      storeId: this.data.storeId,
      expectedAt: this.data.expectedAt,
      remark: this.data.remark,
      items: items
    }, {}, { loading: true, loadingText: '正在提交报单' }).then(function () {
      wx.showToast({ title: '报单成功', icon: 'success' })
      const products = that.data.products.map(function (product) {
        product.qty = 0
        return product
      })
      that.setData({
        submitting: false,
        products: products,
        remark: '',
        selectedCount: 0,
        totalText: '0.00'
      })
      setTimeout(function () {
        wx.navigateTo({ url: '/pages/orders/index' })
      }, 600)
    }).catch(function () {
      that.setData({ submitting: false })
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  }
})
