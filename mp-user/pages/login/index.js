const api = require('../../utils/request')
const config = require('../../utils/config')

Page({
  data: {
    loading: false,
    nickname: config.demo.nickname,
    openid: config.demo.openid,
    baseUrl: config.baseUrl
  },

  login() {
    const that = this
    this.setData({ loading: true })
    api.post('/app/auth/login', {
      tenantCode: config.tenantCode,
      openid: config.demo.openid,
      nickname: config.demo.nickname
    }, {}, { auth: false, loading: true, loadingText: '正在登录' }).then(function (data) {
      wx.setStorageSync('token', data.token)
      wx.setStorageSync('user', data.user || {})
      const app = getApp()
      if (app) {
        app.globalData.token = data.token
        app.globalData.user = data.user || null
      }
      that.setData({ loading: false })
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(function () {
        wx.switchTab({ url: '/pages/home/index' })
      }, 500)
    }).catch(function () {
      that.setData({ loading: false })
    })
  },

  skip() {
    wx.switchTab({ url: '/pages/home/index' })
  }
})
