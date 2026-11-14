const api = require('../../utils/request')
const format = require('../../utils/format')
const cart = require('../../utils/cart')

Page({
  data: {
    hasToken: false,
    loading: true,
    error: '',
    member: {
      nickname: '微信用户',
      points: 0,
      wallet: '0.00'
    },
    banners: [],
    stores: [],
    categories: [],
    products: [],
    selectedStore: null,
    cartCount: 0
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({
      hasToken: hasToken,
      selectedStore: wx.getStorageSync('selectedStore') || null,
      cartCount: cart.count()
    })
    if (hasToken) {
      this.loadHome()
    } else {
      this.setData({ loading: false, error: '' })
    }
  },

  onPullDownRefresh() {
    if (!wx.getStorageSync('token')) {
      wx.stopPullDownRefresh()
      return
    }
    this.loadHome()
  },

  loadHome() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/app/home').then(function (data) {
      const member = data.member || {}
      const stores = (data.stores || []).map(function (store) {
        return Object.assign({}, store, {
          initial: (store.name || '店').slice(0, 1)
        })
      })
      const products = (data.products || []).map(function (row) {
        const sku = row.sku || {}
        const spu = row.spu || {}
        return {
          skuId: sku.id,
          spuId: spu.id,
          name: spu.name || sku.skuCode || '商品',
          spec: sku.spec || spu.spec || '',
          image: spu.mainImage || '',
          initial: (spu.name || '悦').slice(0, 1),
          price: format.money(sku.priceMember || sku.priceRetail),
          originPrice: format.money(sku.priceRetail),
          unit: sku.unit || spu.unit || ''
        }
      })
      let banners = (data.banners || []).map(function (banner) {
        return Object.assign({}, banner, {
          title: banner.title || '悦享便利'
        })
      })
      if (!banners.length) {
        banners = [
          { id: 0, title: '新会员专享礼' },
          { id: -1, title: '门店新鲜直达' },
          { id: -2, title: '积分好礼兑不停' }
        ]
      }
      let selectedStore = wx.getStorageSync('selectedStore') || null
      if (!selectedStore && stores.length) {
        selectedStore = stores[0]
        wx.setStorageSync('selectedStore', selectedStore)
      }
      that.setData({
        member: {
          nickname: member.nickname || '微信用户',
          points: member.points || 0,
          wallet: format.money(member.walletBalance)
        },
        banners: banners,
        stores: stores,
        categories: (data.categories || []).map(function (category) {
          return Object.assign({}, category, {
            initial: (category.name || '类').slice(0, 1)
          })
        }),
        products: products,
        selectedStore: selectedStore,
        loading: false,
        error: ''
      })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '首页加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  goMember() {
    wx.switchTab({ url: '/pages/member/index' })
  },

  goStores() {
    wx.switchTab({ url: '/pages/store/index' })
  },

  goCoupon() {
    wx.navigateTo({ url: '/pages/coupon/index' })
  },

  goCart() {
    wx.navigateTo({ url: '/pages/cart/index' })
  },

  goProductList() {
    wx.navigateTo({ url: '/pages/product/index' })
  },

  goCategory(event) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/product/index?categoryId=' + id })
  },

  goProduct(event) {
    const skuId = event.currentTarget.dataset.skuid
    wx.navigateTo({ url: '/pages/product/detail?skuId=' + skuId })
  },

  goStoreDetail(event) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/store/detail?id=' + id })
  },

  chooseStore(event) {
    const store = this.data.stores[Number(event.currentTarget.dataset.index)]
    if (!store) {
      return
    }
    wx.setStorageSync('selectedStore', store)
    this.setData({ selectedStore: store })
    wx.showToast({ title: '已切换门店', icon: 'success' })
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
      sku: { id: product.skuId, spuId: product.spuId, spec: product.spec, priceMember: product.price, priceRetail: product.originPrice },
      spu: { id: product.spuId, name: product.name, mainImage: product.image }
    }, 1)
    this.setData({ cartCount: cart.count() })
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  }
})
