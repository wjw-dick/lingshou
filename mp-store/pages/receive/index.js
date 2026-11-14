const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    loading: true,
    error: '',
    storeId: 1,
    tab: 'PENDING',
    page: 1,
    size: 10,
    total: 0,
    orders: [],
    showDetail: false,
    detailLoading: false,
    detail: null,
    editItems: [],
    form: {
      receiver: '',
      coldChainTemp: '',
      remark: ''
    },
    submitting: false
  },

  onShow() {
    const user = wx.getStorageSync('user') || {}
    this.setData({
      storeId: wx.getStorageSync('storeId') || 1,
      'form.receiver': user.realName || user.username || ''
    })
    this.loadOrders(true)
  },

  onPullDownRefresh() {
    this.loadOrders(true)
  },

  onReachBottom() {
    if (this.data.orders.length < this.data.total) {
      this.loadOrders(false)
    }
  },

  loadOrders(reset) {
    const that = this
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true, error: '', page: page })
    api.get('/tenant/receive-orders', {
      storeId: this.data.storeId,
      status: this.data.tab,
      page: page,
      size: this.data.size
    }).then(function (data) {
      const rows = (data.records || []).map(function (order) {
        return Object.assign({}, order, {
          statusText: format.statusText(order.status),
          createdText: format.formatDate(order.createdAt),
          receivedText: format.formatDate(order.receivedAt)
        })
      })
      that.setData({
        orders: reset ? rows : that.data.orders.concat(rows),
        total: data.total || 0,
        loading: false,
        error: ''
      })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '收货单加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  switchTab(event) {
    this.setData({ tab: event.currentTarget.dataset.value })
    this.loadOrders(true)
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    const that = this
    this.setData({ showDetail: true, detailLoading: true, detail: null, editItems: [] })
    api.get('/tenant/receive-orders/' + id).then(function (data) {
      const order = data.order || {}
      const items = (data.items || []).map(function (row) {
        const item = row.item || {}
        return {
          id: item.id,
          skuId: item.skuId,
          skuName: row.skuName || ('SKU' + item.skuId),
          skuCode: row.skuCode || '',
          barcode: row.barcode || '',
          qty: item.qty,
          actualQty: item.actualQty || item.qty,
          productionDate: item.productionDate || '',
          expiryDate: item.expiryDate || '',
          batchNo: item.batchNo || '',
          coldChainTemp: item.coldChainTemp === null || item.coldChainTemp === undefined ? '' : String(item.coldChainTemp)
        }
      })
      that.setData({
        detail: Object.assign({}, order, {
          statusText: format.statusText(order.status),
          createdText: format.formatDate(order.createdAt),
          receivedText: format.formatDate(order.receivedAt)
        }),
        editItems: items,
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

  onItemInput(event) {
    const index = Number(event.currentTarget.dataset.index)
    const field = event.currentTarget.dataset.field
    const items = this.data.editItems
    items[index][field] = event.detail.value
    this.setData({ editItems: items })
  },

  onDateChange(event) {
    const index = Number(event.currentTarget.dataset.index)
    const field = event.currentTarget.dataset.field
    const items = this.data.editItems
    items[index][field] = event.detail.value
    this.setData({ editItems: items })
  },

  onFormInput(event) {
    const field = event.currentTarget.dataset.field
    const form = Object.assign({}, this.data.form)
    form[field] = event.detail.value
    this.setData({ form: form })
  },

  confirmReceive() {
    const detail = this.data.detail
    if (!detail) {
      return
    }
    const items = this.data.editItems
    for (let i = 0; i < items.length; i += 1) {
      if (Number(items[i].actualQty) < 0 || items[i].actualQty === '') {
        wx.showToast({ title: '请填写正确实收数量', icon: 'none' })
        return
      }
    }
    const that = this
    const payload = {
      receiver: this.data.form.receiver,
      remark: this.data.form.remark,
      coldChainTemp: this.data.form.coldChainTemp === '' ? null : Number(this.data.form.coldChainTemp),
      items: items.map(function (item) {
        return {
          receiveItemId: item.id,
          actualQty: Number(item.actualQty),
          productionDate: item.productionDate || null,
          expiryDate: item.expiryDate || null,
          batchNo: item.batchNo,
          coldChainTemp: item.coldChainTemp === '' ? null : Number(item.coldChainTemp)
        }
      })
    }
    this.setData({ submitting: true })
    api.post('/tenant/receive-orders/' + detail.id + '/confirm', payload, {}, {
      loading: true,
      loadingText: '正在确认收货'
    }).then(function () {
      wx.showToast({ title: '收货成功', icon: 'success' })
      that.setData({ submitting: false, showDetail: false })
      that.loadOrders(true)
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
