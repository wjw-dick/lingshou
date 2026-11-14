const api = require('../../utils/request')
const format = require('../../utils/format')
const cart = require('../../utils/cart')

const STORAGE_TEXT = {
  NORMAL: '常温保存',
  COLD: '冷藏保存（0-8℃）',
  FROZEN: '冷冻保存（-18℃以下）'
}

Page({
  data: {
    loading: true,
    error: '',
    skuId: '',
    product: null,
    qty: 1,
    totalText: '0.00',
    storageText: '-'
  },

  onLoad(options) {
    this.setData({ skuId: options.skuId || '' })
    this.loadDetail()
  },

  loadDetail() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/public/site/yxbl/products/' + this.data.skuId, {}, { auth: false }).then(function (data) {
      const sku = data.sku || {}
      const spu = data.spu || {}
      const product = {
        skuId: sku.id,
        spuId: spu.id,
        name: spu.name || sku.skuCode || '商品',
        initial: (spu.name || '悦').slice(0, 1),
        image: spu.mainImage || '',
        spec: sku.spec || spu.spec || '-',
        unit: sku.unit || spu.unit || '',
        barcode: sku.barcode || '-',
        skuCode: sku.skuCode || '-',
        price: format.money(sku.priceMember || sku.priceRetail),
        originPrice: format.money(sku.priceRetail),
        ingredients: spu.ingredients || '暂无配料信息',
        allergens: spu.allergens || '无',
        shelfLifeDays: spu.shelfLifeDays || '-',
        origin: spu.origin || '-',
        onlineSale: spu.onlineSale === 1,
        tags: spu.tags || ''
      }
      that.setData({
        product: product,
        storageText: STORAGE_TEXT[spu.storageCondition] || '常温保存',
        totalText: format.money(Number(sku.priceMember || sku.priceRetail || 0) * that.data.qty),
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '商品详情加载失败'
      })
    })
  },

  minus() {
    if (this.data.qty <= 1) {
      return
    }
    this.changeQty(this.data.qty - 1)
  },

  plus() {
    this.changeQty(this.data.qty + 1)
  },

  changeQty(qty) {
    const product = this.data.product
    if (!product) {
      return
    }
    this.setData({
      qty: qty,
      totalText: format.money(Number(product.price) * qty)
    })
  },

  toCart(qty) {
    const product = this.data.product
    if (!product) {
      return
    }
    cart.addToCart({
      sku: {
        id: product.skuId,
        spuId: product.spuId,
        spec: product.spec,
        unit: product.unit,
        priceMember: product.price,
        priceRetail: product.originPrice
      },
      spu: { id: product.spuId, name: product.name, mainImage: product.image }
    }, qty)
  },

  addCart() {
    this.toCart(this.data.qty)
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  },

  buyNow() {
    this.toCart(this.data.qty)
    wx.navigateTo({ url: '/pages/cart/index' })
  }
})
