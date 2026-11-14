const api = require('../../utils/request')

Page({
  data: {
    loading: true,
    error: '',
    keyword: '',
    stores: []
  },

  onLoad() {
    this.loadStores()
  },

  onPullDownRefresh() {
    this.loadStores()
  },

  loadStores() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/public/site/yxbl/stores', {
      keyword: this.data.keyword
    }, { auth: false }).then(function (list) {
      const stores = (list || []).map(function (store) {
        return Object.assign({}, store, {
          initial: (store.name || '店').slice(0, 1),
          regionText: store.region || '附近门店',
          openText: store.status === 'OPEN' ? '营业中' : '休息中',
          deliveryText: store.deliveryEnabled ? '支持外送' : '仅到店自提'
        })
      })
      that.setData({
        stores: stores,
        loading: false,
        error: ''
      })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '门店加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value })
  },

  search() {
    this.loadStores()
  },

  goDetail(event) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/store/detail?id=' + id })
  }
})
