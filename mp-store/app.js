App({
  globalData: {
    token: '',
    user: null,
    storeId: 1
  },

  onLaunch() {
    this.globalData.token = wx.getStorageSync('token') || ''
    this.globalData.user = wx.getStorageSync('user') || null
    this.globalData.storeId = wx.getStorageSync('storeId') || 1
  }
})
