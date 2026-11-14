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
    hasToken: false,
    loading: true,
    error: '',
    tabs: [
      { label: '全部', value: '' },
      { label: '待支付', value: 'PENDING_PAY' },
      { label: '待接单', value: 'PENDING_ACCEPT' },
      { label: '待自提', value: 'WAIT_PICKUP' },
      { label: '已完成', value: 'COMPLETED' }
    ],
    status: '',
    page: 1,
    size: 10,
    total: 0,
    orders: []
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({ hasToken: hasToken })
    if (hasToken) {
      this.loadOrders(true)
    } else {
      this.setData({ loading: false, orders: [] })
    }
  },

  onPullDownRefresh() {
    if (!this.data.hasToken) {
      wx.stopPullDownRefresh()
      return
    }
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
    this.setData({
      loading: true,
      error: '',
      page: page
    })
    api.get('/app/orders', {
      page: page,
      size: this.data.size,
      status: this.data.status
    }).then(function (data) {
      const rows = (data.records || []).map(function (row) {
        const order = row.order || {}
        const items = row.items || []
        const names = items.map(function (item) {
          return item.skuName || '商品'
        })
        return {
          key: order.id,
          order: order,
          items: items.map(function (item) {
            return Object.assign({}, item, {
              amountText: format.money(item.amount)
            })
          }),
          statusText: format.statusText(order.status),
          statusClass: statusClass(order.status),
          createdText: format.formatDate(order.createdAt),
          amountText: format.money(order.paidAmount || order.payableAmount || order.totalAmount),
          itemCount: items.reduce(function (total, item) {
            return total + (item.qty || 0)
          }, 0),
          summary: names.slice(0, 2).join('、') + (names.length > 2 ? ' 等' : '')
        }
      })
      const next = reset ? rows : that.data.orders.concat(rows)
      that.setData({
        orders: next,
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

  goDetail(event) {
    wx.navigateTo({
      url: '/pages/order/detail?id=' + event.currentTarget.dataset.id
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  }
})
