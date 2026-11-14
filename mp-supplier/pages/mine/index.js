const api = require('../../utils/request')

Page({
  data: {
    user: null,
    supplier: null,
    initial: '供',
    loading: true,
    error: ''
  },

  onShow() {
    const user = wx.getStorageSync('user') || null
    this.setData({ user: user })
    if (user) {
      this.loadProfile()
    } else {
      this.setData({ loading: false })
    }
  },

  loadProfile() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/supplier/profile').then(function (data) {
      const supplier = data.supplier || null
      that.setData({
        supplier: supplier,
        initial: supplier && supplier.name ? supplier.name.slice(0, 1) : '供',
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '供应商信息加载失败'
      })
    })
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
        that.setData({ user: null, supplier: null })
        wx.reLaunch({ url: '/pages/login/index' })
      }
    })
  }
})
