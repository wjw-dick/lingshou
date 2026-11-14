const api = require('../../utils/request')
const format = require('../../utils/format')

function statusClass(status) {
  if (status === 'PENDING_PAY') {
    return 'orange'
  }
  if (status === 'COMPLETED' || status === 'CANCELLED' || status === 'REFUNDED') {
    return 'gray'
  }
  return 'green'
}

Page({
  data: {
    loading: true,
    error: '',
    id: '',
    fromPay: false,
    order: null,
    items: [],
    storeName: '',
    canPay: false,
    canCancel: false,
    canEvaluate: false,
    scores: [1, 2, 3, 4, 5],
    evalScore: 5,
    evalContent: '',
    submitting: false
  },

  onLoad(options) {
    this.setData({
      id: options.id || '',
      fromPay: options.pay === '1'
    })
    this.loadDetail()
  },

  loadDetail() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/app/orders/' + this.data.id).then(function (data) {
      const order = data.order || {}
      const items = (data.items || []).map(function (item) {
        return Object.assign({}, item, {
          priceText: format.money(item.price),
          amountText: format.money(item.amount)
        })
      })
      const evaluation = data.evaluation && data.evaluation.id ? data.evaluation : null
      that.setData({
        order: Object.assign({}, order, {
          statusText: format.statusText(order.status),
          statusClass: statusClass(order.status),
          createdText: format.formatDate(order.createdAt),
          paidTimeText: order.paidAt ? format.formatDate(order.paidAt) : '',
          completedText: order.completedAt ? format.formatDate(order.completedAt) : '',
          totalText: format.money(order.totalAmount),
          discountText: format.money(order.discountAmount),
          pointsDiscountText: format.money(order.pointsDiscount),
          freightText: format.money(order.freightAmount),
          payableText: format.money(order.payableAmount),
          paidAmountText: format.money(order.paidAmount),
          deliveryTypeText: order.deliveryType === 'DELIVERY' ? '外送到家' : '到店自提',
          payStatusText: order.payStatus === 'PAID' ? '已支付' : '未支付'
        }),
        items: items,
        evaluation: evaluation,
        canPay: order.status === 'PENDING_PAY',
        canCancel: order.status === 'PENDING_PAY',
        canEvaluate: order.status === 'COMPLETED' && !evaluation,
        loading: false,
        error: ''
      })
      that.loadStoreName(order.storeId)
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '订单详情加载失败'
      })
    })
  },

  loadStoreName(storeId) {
    if (!storeId) {
      return
    }
    const that = this
    api.get('/public/site/yxbl/stores', {}, { auth: false }).then(function (list) {
      const store = (list || []).filter(function (item) {
        return String(item.id) === String(storeId)
      })[0]
      if (store) {
        that.setData({ storeName: store.name })
      }
    }).catch(function () {})
  },

  payNow() {
    const that = this
    this.setData({ submitting: true })
    api.post('/app/orders/' + this.data.id + '/pay', {}, { method: 'WECHAT' }, {
      loading: true,
      loadingText: '正在支付'
    }).then(function () {
      wx.showToast({ title: '支付成功', icon: 'success' })
      that.setData({ submitting: false, fromPay: false })
      that.loadDetail()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  },

  cancelOrder() {
    const that = this
    wx.showModal({
      title: '取消订单',
      content: '确定取消该订单吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        api.post('/app/orders/' + that.data.id + '/cancel', {}, { reason: '用户取消订单' }).then(function () {
          wx.showToast({ title: '订单已取消', icon: 'success' })
          that.loadDetail()
        }).catch(function () {})
      }
    })
  },

  selectScore(event) {
    this.setData({ evalScore: Number(event.currentTarget.dataset.score) })
  },

  onEvalInput(event) {
    this.setData({ evalContent: event.detail.value })
  },

  submitEvaluation() {
    const that = this
    this.setData({ submitting: true })
    api.post('/app/orders/' + this.data.id + '/evaluate', {
      score: this.data.evalScore,
      content: this.data.evalContent
    }, {}, { loading: true }).then(function () {
      wx.showToast({ title: '评价成功', icon: 'success' })
      that.setData({ submitting: false })
      that.loadDetail()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
