const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    hasToken: false,
    loading: true,
    error: '',
    storeId: 1,
    storeName: '门店',
    todaySalesText: '0.00',
    todayOrderCount: 0,
    pendingReceive: 0,
    pendingPick: 0,
    waitPickup: 0,
    delivering: 0,
    nearExpiry: 0,
    pendingChecks: 0,
    pendingLosses: 0,
    notices: []
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({
      hasToken: hasToken,
      storeId: wx.getStorageSync('storeId') || 1
    })
    if (hasToken) {
      this.loadWorkbench()
    } else {
      this.setData({ loading: false })
    }
  },

  onPullDownRefresh() {
    if (!wx.getStorageSync('token')) {
      wx.stopPullDownRefresh()
      return
    }
    this.loadWorkbench()
  },

  loadWorkbench() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/staff/workbench', { storeId: this.data.storeId }).then(function (data) {
      const store = data.store || {}
      that.setData({
        storeId: store.id || that.data.storeId,
        storeName: store.name || '门店',
        todaySalesText: format.money(data.todaySales),
        todayOrderCount: data.todayOrderCount || 0,
        pendingReceive: data.pendingReceive || 0,
        pendingPick: data.pendingPick || 0,
        waitPickup: data.waitPickup || 0,
        delivering: data.delivering || 0,
        nearExpiry: data.nearExpiry || 0,
        pendingChecks: data.pendingChecks || 0,
        pendingLosses: data.pendingLosses || 0,
        notices: data.notices || [],
        loading: false,
        error: ''
      })
      if (store.id) {
        wx.setStorageSync('storeId', store.id)
      }
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '工作台加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  goReceive() {
    wx.navigateTo({ url: '/pages/receive/index' })
  },

  goReplenishment() {
    wx.navigateTo({ url: '/pages/replenishment/index' })
  },

  goShift() {
    wx.navigateTo({ url: '/pages/shift/index' })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/order/index' })
  },

  goInventory() {
    wx.switchTab({ url: '/pages/inventory/index' })
  }
})
