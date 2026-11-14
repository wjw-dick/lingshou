App({
  globalData: {
    token: '',
    user: null,
    store: null
  },

  onLaunch() {
    this.globalData.token = wx.getStorageSync('token') || ''
    this.globalData.user = wx.getStorageSync('user') || null
    this.globalData.store = wx.getStorageSync('selectedStore') || null
  }
})
