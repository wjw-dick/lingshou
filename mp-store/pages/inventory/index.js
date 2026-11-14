const api = require('../../utils/request')
const format = require('../../utils/format')

const STORAGE_TEXT = {
  NORMAL: '常温',
  COLD: '冷藏',
  FROZEN: '冷冻'
}

const REASONS = [
  { value: 'EXPIRED', label: '过期报损' },
  { value: 'DAMAGED', label: '破损报损' },
  { value: 'OTHER', label: '其他原因' }
]

Page({
  data: {
    storeId: 1,
    tab: 'stock',
    loading: true,
    error: '',
    stocks: [],
    alerts: {
      lowStock: [],
      nearExpiry: [],
      slowMoving: [],
      overstock: []
    },
    alertCount: 0,
    lossOrders: [],
    checks: [],
    showLoss: false,
    showCheck: false,
    submitting: false,
    stockNames: [],
    reasonNames: REASONS.map(function (item) {
      return item.label
    }),
    reasonName: REASONS[0].label,
    reasonIndex: 0,
    lossForm: {
      skuId: '',
      skuName: '',
      qty: '',
      remark: ''
    },
    checkForm: {
      skuId: '',
      skuName: '',
      actualQty: '',
      reason: ''
    }
  },

  onShow() {
    this.setData({ storeId: wx.getStorageSync('storeId') || 1 })
    this.loadAll()
  },

  onPullDownRefresh() {
    this.loadAll()
  },

  loadAll() {
    const that = this
    this.setData({ loading: true, error: '' })
    Promise.all([
      api.get('/tenant/inventory/stocks', {
        storeId: this.data.storeId,
        ownerType: 'STORE'
      }),
      api.get('/tenant/inventory/alerts', { storeId: this.data.storeId }),
      api.get('/tenant/loss-orders', {
        storeId: this.data.storeId,
        page: 1,
        size: 10
      }).catch(function () {
        return { records: [] }
      }),
      api.get('/tenant/stock-checks', {
        storeId: this.data.storeId,
        page: 1,
        size: 10
      }).catch(function () {
        return { records: [] }
      })
    ]).then(function (results) {
      const stocks = (results[0] || []).map(function (row) {
        const stock = row.stock || {}
        return {
          skuId: stock.skuId,
          skuName: row.skuName,
          skuCode: row.skuCode,
          barcode: row.barcode,
          spec: row.spec,
          qty: stock.qty || 0,
          lockedQty: stock.lockedQty || 0,
          safetyStock: stock.safetyStock || 0,
          storageText: STORAGE_TEXT[row.storageCondition] || '常温',
          price: Number(row.price) || 0,
          priceText: format.money(row.price)
        }
      })
      const rawAlerts = results[1] || {}
      const lowStock = (rawAlerts.lowStock || []).map(function (row) {
        const stock = row.stock || {}
        return {
          skuId: stock.skuId,
          skuName: row.skuName,
          barcode: row.barcode,
          qty: stock.qty || 0,
          safetyStock: stock.safetyStock || 0
        }
      })
      const nearExpiry = (rawAlerts.nearExpiry || []).map(function (row) {
        const batch = row.batch || {}
        return {
          skuId: batch.skuId,
          skuName: row.skuName,
          barcode: row.barcode,
          qty: batch.qty || 0,
          batchNo: batch.batchNo || '-',
          expiryDate: format.formatDay(batch.expiryDate),
          daysLeft: row.daysLeft
        }
      })
      const slowMoving = (rawAlerts.slowMoving || []).map(function (row) {
        const stock = row.stock || {}
        return {
          skuId: stock.skuId,
          skuName: row.skuName,
          barcode: row.barcode,
          qty: stock.qty || 0
        }
      })
      const overstock = (rawAlerts.overstock || []).map(function (row) {
        const stock = row.stock || {}
        return {
          skuId: stock.skuId,
          skuName: row.skuName,
          barcode: row.barcode,
          qty: stock.qty || 0
        }
      })
      const lossOrders = (results[2].records || []).map(function (order) {
        return Object.assign({}, order, {
          statusText: format.statusText(order.status),
          createdText: format.formatDate(order.createdAt),
          amountText: format.money(order.totalAmount)
        })
      })
      const checks = (results[3].records || []).map(function (order) {
        return Object.assign({}, order, {
          statusText: format.statusText(order.status),
          createdText: format.formatDate(order.createdAt)
        })
      })
      that.setData({
        stocks: stocks,
        stockNames: stocks.map(function (stock) {
          return stock.skuName + '（库存 ' + stock.qty + '）'
        }),
        alerts: {
          lowStock: lowStock,
          nearExpiry: nearExpiry,
          slowMoving: slowMoving,
          overstock: overstock
        },
        alertCount: lowStock.length + nearExpiry.length + slowMoving.length + overstock.length,
        lossOrders: lossOrders,
        checks: checks,
        loading: false,
        error: ''
      })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '库存数据加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  switchTab(event) {
    this.setData({ tab: event.currentTarget.dataset.tab })
  },

  stopTap() {},

  findStock(skuId) {
    return this.data.stocks.filter(function (stock) {
      return String(stock.skuId) === String(skuId)
    })[0]
  },

  openLoss() {
    this.setData({
      showLoss: true,
      reasonIndex: 0,
      lossForm: { skuId: '', skuName: '', qty: '', remark: '' }
    })
  },

  openCheck() {
    this.setData({
      showCheck: true,
      checkForm: { skuId: '', skuName: '', actualQty: '', reason: '' }
    })
  },

  closeModal() {
    this.setData({ showLoss: false, showCheck: false })
  },

  onLossStockChange(event) {
    const stock = this.data.stocks[Number(event.detail.value)]
    if (!stock) {
      return
    }
    this.setData({
      lossForm: {
        skuId: stock.skuId,
        skuName: stock.skuName,
        qty: '1',
        remark: this.data.lossForm.remark
      }
    })
  },

  onLossInput(event) {
    const field = event.currentTarget.dataset.field
    const form = Object.assign({}, this.data.lossForm)
    form[field] = event.detail.value
    this.setData({ lossForm: form })
  },

  onReasonChange(event) {
    const index = Number(event.detail.value)
    this.setData({
      reasonIndex: index,
      reasonName: this.data.reasonNames[index] || ''
    })
  },

  submitLoss() {
    const form = this.data.lossForm
    const qty = Number(form.qty)
    if (!form.skuId || !qty || qty <= 0) {
      wx.showToast({ title: '请选择商品并填写数量', icon: 'none' })
      return
    }
    const stock = this.findStock(form.skuId)
    const that = this
    this.setData({ submitting: true })
    api.post('/tenant/loss-orders', {
      storeId: this.data.storeId,
      reasonType: REASONS[this.data.reasonIndex].value,
      remark: form.remark,
      items: [
        {
          skuId: form.skuId,
          qty: qty,
          amount: Number(stock ? stock.price : 0) * qty
        }
      ]
    }, {}, { loading: true, loadingText: '正在提交报损' }).then(function () {
      wx.showToast({ title: '报损单已提交', icon: 'success' })
      that.setData({ submitting: false, showLoss: false })
      that.loadAll()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  },

  onCheckStockChange(event) {
    const stock = this.data.stocks[Number(event.detail.value)]
    if (!stock) {
      return
    }
    this.setData({
      checkForm: {
        skuId: stock.skuId,
        skuName: stock.skuName,
        actualQty: String(stock.qty),
        reason: this.data.checkForm.reason
      }
    })
  },

  onCheckInput(event) {
    const field = event.currentTarget.dataset.field
    const form = Object.assign({}, this.data.checkForm)
    form[field] = event.detail.value
    this.setData({ checkForm: form })
  },

  submitCheck() {
    const form = this.data.checkForm
    if (!form.skuId || form.actualQty === '') {
      wx.showToast({ title: '请选择商品并填写实盘数量', icon: 'none' })
      return
    }
    const that = this
    this.setData({ submitting: true })
    api.post('/tenant/stock-checks', {
      storeId: this.data.storeId,
      checkType: 'FULL',
      remark: '移动端盘点',
      items: [
        {
          skuId: form.skuId,
          actualQty: Number(form.actualQty),
          reason: form.reason
        }
      ]
    }, {}, { loading: true, loadingText: '正在提交盘点' }).then(function () {
      wx.showToast({ title: '盘点单已提交', icon: 'success' })
      that.setData({ submitting: false, showCheck: false })
      that.loadAll()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
