const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    user: null,
    franchisee: null,
    qualifications: [],
    initial: '盟',
    creditText: '0.00',
    usedText: '0.00',
    depositText: '0.00',
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
    api.get('/franchisee/profile').then(function (data) {
      const franchisee = data.franchisee || null
      that.setData({
        franchisee: franchisee,
        qualifications: (data.qualifications || []).map(function (item) {
          return Object.assign({}, item, {
            expireText: format.formatDay(item.expireAt),
            statusText: format.statusText(item.status)
          })
        }),
        initial: franchisee && franchisee.name ? franchisee.name.slice(0, 1) : '盟',
        creditText: format.money(franchisee ? franchisee.creditLimit : 0),
        usedText: format.money(franchisee ? franchisee.usedCredit : 0),
        depositText: format.money(franchisee ? franchisee.deposit : 0),
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '加盟商信息加载失败'
      })
    })
  },

  goStores() {
    wx.navigateTo({ url: '/pages/stores/index' })
  },

  goOrders() {
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  goStatements() {
    wx.navigateTo({ url: '/pages/statements/index' })
  },

  goCommissions() {
    wx.switchTab({ url: '/pages/commissions/index' })
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
        that.setData({ user: null, franchisee: null })
        wx.reLaunch({ url: '/pages/login/index' })
      }
    })
  }
})
