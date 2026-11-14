const api = require('../../utils/request')
const format = require('../../utils/format')

function statusClass(status) {
  if (status === 'PENDING') {
    return 'orange'
  }
  if (status === 'CONFIRMED' || status === 'PAID') {
    return 'green'
  }
  return 'gray'
}

Page({
  data: {
    loading: true,
    error: '',
    list: [],
    acting: false
  },

  onLoad() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData()
  },

  loadData() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/franchisee/statements').then(function (list) {
      const rows = (list || []).map(function (statement) {
        return Object.assign({}, statement, {
          statusText: format.statusText(statement.status),
          statusClass: statusClass(statement.status),
          periodText: format.formatDay(statement.periodStart) + ' 至 ' + format.formatDay(statement.periodEnd),
          totalText: format.money(statement.totalAmount),
          deductionText: format.money(statement.deductionAmount),
          payableText: format.money(statement.payableAmount),
          createdText: format.formatDate(statement.createdAt)
        })
      })
      that.setData({ list: rows, loading: false, error: '' })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '对账单加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  confirmStatement(event) {
    const index = Number(event.currentTarget.dataset.index)
    const statement = this.data.list[index]
    if (!statement) {
      return
    }
    const that = this
    wx.showModal({
      title: '确认对账',
      content: '确认后该对账单将进入结算流程，确定继续吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        that.setData({ acting: true })
        api.post('/franchisee/statements/' + statement.id + '/confirm', {}, {}, { loading: true }).then(function () {
          wx.showToast({ title: '已确认', icon: 'success' })
          that.setData({ acting: false })
          that.loadData()
        }).catch(function () {
          that.setData({ acting: false })
        })
      }
    })
  }
})
