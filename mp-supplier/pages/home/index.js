const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    hasToken: false,
    loading: true,
    error: '',
    supplier: null,
    pendingOrders: 0,
    pendingStatements: 0,
    recent: []
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({ hasToken: hasToken })
    if (hasToken) {
      this.loadHome()
    } else {
      this.setData({ loading: false })
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
    Promise.all([
      api.get('/supplier/messages'),
      api.get('/supplier/profile'),
      api.get('/supplier/supply-orders', { page: 1, size: 5 })
    ]).then(function (results) {
      const messages = results[0] || {}
      const profile = results[1] || {}
      const recent = ((results[2] || {}).records || []).map(function (row) {
        const order = row.order || {}
        return {
          id: order.id,
          orderNo: order.orderNo,
          statusText: format.statusText(order.status),
          createdText: format.formatDate(order.createdAt),
          amountText: format.money(order.totalAmount),
          itemCount: (row.items || []).length
        }
      })
      that.setData({
        supplier: profile.supplier || null,
        pendingOrders: messages.pendingOrders || 0,
        pendingStatements: messages.pendingStatements || 0,
        recent: recent,
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

  goOrders() {
    wx.switchTab({ url: '/pages/orders/index' })
  },

  goStatements() {
    wx.switchTab({ url: '/pages/statements/index' })
  },

  goProducts() {
    wx.navigateTo({ url: '/pages/products/index' })
  },

  goQualifications() {
    wx.navigateTo({ url: '/pages/qualifications/index' })
  }
})
