const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    loading: true,
    error: '',
    rows: []
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
    api.get('/franchisee/stores').then(function (list) {
      const rows = (list || []).map(function (row) {
        const store = row.store || {}
        const relation = row.relation || {}
        return {
          key: store.id,
          store: store,
          initial: (store.name || '店').slice(0, 1),
          statusText: store.status === 'OPEN' ? '营业中' : '休息中',
          salesText: format.money(row.sales30d),
          stockQty: row.stockQty || 0,
          memberCount: row.memberCount || 0,
          joinText: format.formatDay(relation.joinDate)
        }
      })
      that.setData({ rows: rows, loading: false, error: '' })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '门店信息加载失败'
      })
      wx.stopPullDownRefresh()
    })
  }
})
