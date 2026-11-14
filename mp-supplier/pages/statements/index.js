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
    tabs: [
      { label: '全部', value: '' },
      { label: '待确认', value: 'PENDING' },
      { label: '已确认', value: 'CONFIRMED' },
      { label: '有异议', value: 'DISPUTED' }
    ],
    status: '',
    page: 1,
    size: 10,
    total: 0,
    statements: [],
    showDetail: false,
    detailLoading: false,
    detail: null,
    detailItems: [],
    detailInvoices: [],
    acting: false
  },

  onShow() {
    this.loadData(true)
  },

  onPullDownRefresh() {
    this.loadData(true)
  },

  onReachBottom() {
    if (this.data.statements.length < this.data.total) {
      this.loadData(false)
    }
  },

  loadData(reset) {
    const that = this
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true, error: '', page: page })
    api.get('/supplier/statements', {
      page: page,
      size: this.data.size,
      status: this.data.status
    }).then(function (data) {
      const rows = (data.records || []).map(function (statement) {
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
      that.setData({
        statements: reset ? rows : that.data.statements.concat(rows),
        total: data.total || 0,
        loading: false,
        error: ''
      })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '对账单加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  switchTab(event) {
    this.setData({ status: event.currentTarget.dataset.value })
    this.loadData(true)
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    const that = this
    this.setData({ showDetail: true, detailLoading: true, detail: null, detailItems: [], detailInvoices: [] })
    api.get('/supplier/statements/' + id).then(function (data) {
      const statement = data.statement || {}
      that.setData({
        detail: Object.assign({}, statement, {
          statusText: format.statusText(statement.status),
          statusClass: statusClass(statement.status),
          periodText: format.formatDay(statement.periodStart) + ' 至 ' + format.formatDay(statement.periodEnd),
          totalText: format.money(statement.totalAmount),
          deductionText: format.money(statement.deductionAmount),
          payableText: format.money(statement.payableAmount),
          createdText: format.formatDate(statement.createdAt)
        }),
        detailItems: (data.items || []).map(function (item) {
          return Object.assign({}, item, {
            amountText: format.money(item.amount),
            deductionText: format.money(item.deduction),
            payableText: format.money(item.payable)
          })
        }),
        detailInvoices: (data.invoices || []).map(function (invoice) {
          return Object.assign({}, invoice, {
            amountText: format.money(invoice.amount)
          })
        }),
        detailLoading: false
      })
    }).catch(function () {
      that.setData({ detailLoading: false })
    })
  },

  closeDetail() {
    this.setData({ showDetail: false })
  },

  stopTap() {},

  confirmStatement() {
    const that = this
    const detail = this.data.detail
    if (!detail) {
      return
    }
    wx.showModal({
      title: '确认对账',
      content: '确认后该对账单将进入结算流程，确定继续吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        that.setData({ acting: true })
        api.post('/supplier/statements/' + detail.id + '/confirm', {}, {}, { loading: true }).then(function () {
          wx.showToast({ title: '已确认', icon: 'success' })
          that.setData({ acting: false, showDetail: false })
          that.loadData(true)
        }).catch(function () {
          that.setData({ acting: false })
        })
      }
    })
  },

  dispute() {
    const that = this
    const detail = this.data.detail
    if (!detail) {
      return
    }
    wx.showModal({
      title: '申诉原因',
      editable: true,
      placeholderText: '请填写差异原因与金额',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        const reason = res.content || '对账金额有异议'
        that.setData({ acting: true })
        api.post('/supplier/statements/' + detail.id + '/dispute', {}, { reason: reason }, { loading: true }).then(function () {
          wx.showToast({ title: '已提交申诉', icon: 'success' })
          that.setData({ acting: false, showDetail: false })
          that.loadData(true)
        }).catch(function () {
          that.setData({ acting: false })
        })
      }
    })
  }
})
