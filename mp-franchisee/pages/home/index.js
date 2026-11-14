const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    hasToken: false,
    loading: true,
    error: '',
    franchisee: null,
    pendingOrders: 0,
    pendingStatements: 0,
    creditLimitText: '0.00',
    usedCreditText: '0.00',
    availableText: '0.00',
    storeCount: 0
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
      api.get('/franchisee/messages'),
      api.get('/franchisee/profile'),
      api.get('/franchisee/stores')
    ]).then(function (results) {
      const messages = results[0] || {}
      const profile = results[1] || {}
      const creditLimit = Number(messages.creditLimit) || 0
      const usedCredit = Number(messages.usedCredit) || 0
      that.setData({
        franchisee: profile.franchisee || null,
        pendingOrders: messages.pendingOrders || 0,
        pendingStatements: messages.pendingStatements || 0,
        creditLimitText: format.money(creditLimit),
        usedCreditText: format.money(usedCredit),
        availableText: format.money(Math.max(0, creditLimit - usedCredit)),
        storeCount: (results[2] || []).length,
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

  goStores() {
    wx.navigateTo({ url: '/pages/stores/index' })
  },

  goCreate() {
    wx.switchTab({ url: '/pages/order/create' })
  },

  goOrders() {
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  goCommissions() {
    wx.switchTab({ url: '/pages/commissions/index' })
  },

  goStatements() {
    wx.navigateTo({ url: '/pages/statements/index' })
  }
})
