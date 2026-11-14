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
    storeId: 1,
    tabs: [
      { label: '全部', value: '' },
      { label: '待接单', value: 'PENDING_ACCEPT' },
      { label: '拣货中', value: 'PICKING' },
      { label: '待自提', value: 'WAIT_PICKUP' },
      { label: '配送中', value: 'DELIVERING' },
      { label: '已完成', value: 'COMPLETED' }
    ],
    status: '',
    page: 1,
    size: 10,
    total: 0,
    orders: [],
    detailLoading: false,
    detail: null,
    detailItems: [],
    showDetail: false,
    acting: false
  },

  onShow() {
    this.setData({ storeId: wx.getStorageSync('storeId') || 1 })
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
    api.get('/tenant/orders', {
      storeId: this.data.storeId,
      status: this.data.status,
      page: page,
      size: this.data.size
    }).then(function (data) {
      const rows = (data.records || []).map(function (row) {
        const order = row.order || {}
        return {
          key: order.id,
          order: order,
          memberName: row.memberName || '散客',
          storeName: row.storeName || '',
          statusText: format.statusText(order.status),
          statusClass: statusClass(order.status),
          createdText: format.formatDate(order.createdAt),
          amountText: format.money(order.payableAmount || order.totalAmount),
          deliveryTypeText: order.deliveryType === 'DELIVERY' ? '外送' : '自提'
        }
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
        error: err.message || '订单加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  switchTab(event) {
    this.setData({ status: event.currentTarget.dataset.value })
    this.loadOrders(true)
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    const that = this
    this.setData({ showDetail: true, detailLoading: true, detail: null, detailItems: [] })
    api.get('/tenant/orders/' + id).then(function (data) {
      const order = data.order || {}
      that.setData({
        detail: Object.assign({}, order, {
          statusText: format.statusText(order.status),
          statusClass: statusClass(order.status),
          createdText: format.formatDate(order.createdAt),
          totalText: format.money(order.totalAmount),
          payableText: format.money(order.payableAmount),
          paidText: format.money(order.paidAmount),
          discountText: format.money(order.discountAmount),
          deliveryTypeText: order.deliveryType === 'DELIVERY' ? '外送到家' : '到店自提'
        }),
        detailItems: (data.items || []).map(function (item) {
          return Object.assign({}, item, {
            priceText: format.money(item.price),
            amountText: format.money(item.amount)
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

  accept() {
    this.act('accept', '确认接单')
  },

  pickComplete() {
    this.act('pick-complete', '确认拣货完成')
  },

  complete() {
    this.act('complete', '确认完成订单')
  },

  act(action, title) {
    const detail = this.data.detail
    if (!detail) {
      return
    }
    const that = this
    wx.showModal({
      title: title,
      content: '确定执行该操作吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        that.setData({ acting: true })
        api.post('/tenant/orders/' + detail.id + '/' + action, {}, {}, { loading: true }).then(function () {
          wx.showToast({ title: '操作成功', icon: 'success' })
          that.setData({ acting: false })
          that.loadOrders(true)
          that.openDetail({ currentTarget: { dataset: { id: detail.id } } })
        }).catch(function () {
          that.setData({ acting: false })
        })
      }
    })
  }
})
