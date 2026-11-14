const api = require('../../utils/request')
const config = require('../../utils/config')

Page({
  data: {
    username: config.demo.username,
    password: config.demo.password,
    loading: false,
    baseUrl: config.baseUrl
  },

  onUsername(event) {
    this.setData({ username: event.detail.value })
  },

  onPassword(event) {
    this.setData({ password: event.detail.value })
  },

  login() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    const that = this
    this.setData({ loading: true })
    api.post('/auth/login', {
      tenantCode: config.tenantCode,
      username: this.data.username,
      password: this.data.password
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
  }
})
