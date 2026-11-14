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
      const user = data.user || {}
      wx.setStorageSync('token', data.token)
      wx.setStorageSync('user', user)
      wx.setStorageSync('storeId', user.storeId || 1)
      const app = getApp()
      if (app) {
        app.globalData.token = data.token
        app.globalData.user = user
        app.globalData.storeId = user.storeId || 1
      }
      that.setData({ loading: false })
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(function () {
        wx.switchTab({ url: '/pages/workbench/index' })
      }, 500)
    }).catch(function () {
      that.setData({ loading: false })
    })
  }
})
