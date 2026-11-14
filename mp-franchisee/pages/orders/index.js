const api = require('../../utils/request')
const format = require('../../utils/format')

function statusClass(status) {
  if (status === 'PENDING') {
    return 'orange'
  }
  if (status === 'CANCELLED') {
    return 'gray'
  }
  return 'green'
}

Page({
  data: {
    loading: true,
    error: '',
    tabs: [
      { label: '全部', value: '' },
      { label: '待处理', value: 'PENDING' },
      { label: '已接单', value: 'ACCEPTED' },
      { label: '已发货', value: 'SHIPPED' },
      { label: '已收货', value: 'RECEIVED' }
    ],
    status: '',
    page: 1,
    size: 10,
    total: 0,
    orders: [],
    showDetail: false,
    detail: null
  },

  onLoad() {
    this.loadData(true)
  },

  onPullDownRefresh() {
    this.loadData(true)
  },

  onReachBottom() {
    if (this.data.orders.length < this.data.total) {
      this.loadData(false)
    }
  },

  loadData(reset) {
    const that = this
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true, error: '', page: page })
    Promise.all([
      api.get('/franchisee/supply-orders', {
        page: page,
        size: this.data.size,
        status: this.data.status
      }),
      api.get('/public/site/yxbl/products', { limit: 100 }, { auth: false }).catch(function () {
        return []
      })
    ]).then(function (results) {
      const nameMap = {}
      ;(results[1] || []).forEach(function (row) {
        const sku = row.sku || {}
        const spu = row.spu || {}
        nameMap[sku.id] = spu.name || ('SKU' + sku.id)
      })
      const data = results[0] || {}
      const rows = (data.records || []).map(function (row) {
        const order = row.order || {}
        const items = (row.items || []).map(function (item) {
          return Object.assign({}, item, {
            name: nameMap[item.skuId] || ('SKU' + item.skuId),
            priceText: format.money(item.price),
            amountText: format.money(item.amount)
          })
        })
        return {
          key: order.id,
          order: order,
          items: items,
          statusText: format.statusText(order.status),
          statusClass: statusClass(order.status),
          createdText: format.formatDate(order.createdAt),
          totalText: format.money(order.totalAmount),
          itemCount: items.length
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
        error: err.message || '报单加载失败'
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
    const detail = this.data.orders.filter(function (row) {
      return String(row.order.id) === String(id)
    })[0]
    if (detail) {
      this.setData({ detail: detail, showDetail: true })
    }
  },

  closeDetail() {
    this.setData({ showDetail: false })
  },

  stopTap() {}
})
