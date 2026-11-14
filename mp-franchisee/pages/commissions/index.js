const api = require('../../utils/request')
const format = require('../../utils/format')

function statusClass(status) {
  if (status === 'PENDING' || status === 'PENDING_SETTLE') {
    return 'orange'
  }
  if (status === 'SETTLED' || status === 'PAID') {
    return 'green'
  }
  return 'gray'
}

Page({
  data: {
    loading: true,
    error: '',
    list: [],
    totalText: '0.00'
  },

  onShow() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData()
  },

  loadData() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/franchisee/commissions').then(function (list) {
      let total = 0
      const rows = (list || []).map(function (item) {
        total += Number(item.commissionAmount) || 0
        return Object.assign({}, item, {
          salesText: format.money(item.salesAmount),
          commissionText: format.money(item.commissionAmount),
          valueText: format.money(item.commissionValue),
          statusText: format.statusText(item.status),
          statusClass: statusClass(item.status),
          createdText: format.formatDate(item.createdAt)
        })
      })
      that.setData({
        list: rows,
        totalText: format.money(total),
        loading: false,
        error: ''
      })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '分账数据加载失败'
      })
      wx.stopPullDownRefresh()
    })
  }
})
