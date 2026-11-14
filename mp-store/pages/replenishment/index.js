const api = require('../../utils/request')
const format = require('../../utils/format')

const STORAGE_TEXT = {
  NORMAL: '常温',
  COLD: '冷藏',
  FROZEN: '冷冻'
}

function tomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return date.getFullYear() + '-' + (month < 10 ? '0' + month : month) + '-' + (day < 10 ? '0' + day : day)
}

Page({
  data: {
    loading: true,
    error: '',
    storeId: 1,
    rows: [],
    expectedAt: tomorrow(),
    remark: '门店补货建议',
    totalText: '0.00',
    submitting: false
  },

  onShow() {
    this.setData({ storeId: wx.getStorageSync('storeId') || 1 })
    this.loadRows()
  },

  loadRows() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/tenant/purchase-orders/replenishment', {
      storeId: this.data.storeId
    }).then(function (list) {
      const rows = (list || []).map(function (row) {
        return Object.assign({}, row, {
          qty: row.suggestion || 0,
          checked: (row.suggestion || 0) > 0,
          priceText: format.money(row.price),
          onHand: row.onHand || 0,
          safetyStock: row.safetyStock || 0,
          inTransit: row.inTransit || 0,
          avgDailyText: format.money(row.avgDaily),
          storageText: STORAGE_TEXT[row.storageCondition] || '常温'
        })
      })
      that.setData({ rows: rows, loading: false, error: '' })
      that.calcTotal()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '补货建议加载失败'
      })
    })
  },

  toggleRow(event) {
    const index = Number(event.currentTarget.dataset.index)
    const rows = this.data.rows
    rows[index].checked = !rows[index].checked
    if (rows[index].checked && Number(rows[index].qty) <= 0) {
      rows[index].qty = rows[index].suggestion || 1
    }
    this.setData({ rows: rows })
    this.calcTotal()
  },

  stopTap() {},

  onQtyInput(event) {
    const index = Number(event.currentTarget.dataset.index)
    const rows = this.data.rows
    rows[index].qty = event.detail.value
    rows[index].checked = Number(event.detail.value) > 0
    this.setData({ rows: rows })
    this.calcTotal()
  },

  onDateChange(event) {
    this.setData({ expectedAt: event.detail.value })
  },

  onRemarkInput(event) {
    this.setData({ remark: event.detail.value })
  },

  calcTotal() {
    const total = this.data.rows.reduce(function (sum, row) {
      if (!row.checked) {
        return sum
      }
      return sum + Number(row.price || 0) * Number(row.qty || 0)
    }, 0)
    this.setData({ totalText: format.money(total) })
  },

  submit() {
    const items = this.data.rows.filter(function (row) {
      return row.checked && Number(row.qty) > 0
    }).map(function (row) {
      return {
        skuId: row.skuId,
        qty: Number(row.qty),
        price: Number(row.price)
      }
    })
    if (!items.length) {
      wx.showToast({ title: '请选择补货商品', icon: 'none' })
      return
    }
    const that = this
    this.setData({ submitting: true })
    api.post('/tenant/supply-orders', {
      storeId: this.data.storeId,
      sourceType: 'HQ',
      expectedAt: this.data.expectedAt,
      remark: this.data.remark,
      items: items
    }, {}, { loading: true, loadingText: '正在提交补货单' }).then(function () {
      wx.showToast({ title: '补货单已提交', icon: 'success' })
      that.setData({ submitting: false })
      that.loadRows()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
