const api = require('../../utils/request')
const format = require('../../utils/format')
const cart = require('../../utils/cart')

Page({
  data: {
    loading: true,
    error: '',
    keyword: '',
    categoryId: '',
    categories: [],
    products: [],
    cartCount: 0
  },

  onLoad(options) {
    this.setData({
      categoryId: options.categoryId || '',
      cartCount: cart.count()
    })
    this.loadCategories()
    this.loadProducts()
  },

  onShow() {
    this.setData({ cartCount: cart.count() })
  },

  loadCategories() {
    const that = this
    api.get('/public/site/yxbl/categories', {}, { auth: false }).then(function (list) {
      that.setData({ categories: list || [] })
    }).catch(function () {
      that.setData({ categories: [] })
    })
  },

  loadProducts() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/public/site/yxbl/products', {
      categoryId: this.data.categoryId,
      keyword: this.data.keyword,
      limit: 50
    }, { auth: false }).then(function (list) {
      const products = (list || []).map(function (row) {
        const sku = row.sku || {}
        const spu = row.spu || {}
        return {
          skuId: sku.id,
          spuId: spu.id,
          name: spu.name || sku.skuCode || '商品',
          spec: sku.spec || spu.spec || '',
          unit: sku.unit || spu.unit || '',
          image: spu.mainImage || '',
          initial: (spu.name || '悦').slice(0, 1),
          price: format.money(sku.priceMember || sku.priceRetail),
          originPrice: format.money(sku.priceRetail),
          tags: spu.tags || ''
        }
      })
      that.setData({
        products: products,
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '商品加载失败'
      })
    })
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value })
  },

  search() {
    this.loadProducts()
  },

  selectCategory(event) {
    this.setData({ categoryId: event.currentTarget.dataset.id || '' })
    this.loadProducts()
  },

  goDetail(event) {
    wx.navigateTo({
      url: '/pages/product/detail?skuId=' + event.currentTarget.dataset.skuid
    })
  },

  addCart(event) {
    const skuId = event.currentTarget.dataset.skuid
    const product = this.data.products.filter(function (item) {
      return String(item.skuId) === String(skuId)
    })[0]
    if (!product) {
      return
    }
    cart.addToCart({
      sku: { id: product.skuId, spuId: product.spuId, spec: product.spec, unit: product.unit, priceMember: product.price, priceRetail: product.originPrice },
      spu: { id: product.spuId, name: product.name, mainImage: product.image }
    }, 1)
    this.setData({ cartCount: cart.count() })
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  },

  goCart() {
    wx.navigateTo({ url: '/pages/cart/index' })
  }
})
