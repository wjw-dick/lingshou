const api = require('../../utils/request')

Page({
  data: {
    user: null,
    initial: '店',
    storeName: '门店',
    storeId: 1,
    loading: true,
    error: ''
  },

  onShow() {
    const user = wx.getStorageSync('user') || null
    this.setData({
      user: user,
      initial: user ? (user.realName || user.username || '店').slice(0, 1) : '店',
      storeId: wx.getStorageSync('storeId') || 1
    })
    if (user) {
      this.loadStore()
    } else {
      this.setData({ loading: false })
    }
  },

  loadStore() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/staff/workbench', { storeId: this.data.storeId }).then(function (data) {
      const store = data.store || {}
      that.setData({
        storeName: store.name || '门店',
        storeId: store.id || that.data.storeId,
        loading: false,
        error: ''
      })
      if (store.id) {
        wx.setStorageSync('storeId', store.id)
      }
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '门店信息加载失败'
      })
    })
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

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  logout() {
    const that = this
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        api.post('/auth/logout', {}, {}, { auth: false }).catch(function () {})
        wx.removeStorageSync('token')
        wx.removeStorageSync('user')
        that.setData({ user: null })
        wx.reLaunch({ url: '/pages/login/index' })
      }
    })
  }
})
