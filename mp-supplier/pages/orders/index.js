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
      { label: '待接单', value: 'PENDING' },
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
    detail: null,
    acting: false
  },

  onShow() {
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
      api.get('/supplier/supply-orders', {
        page: page,
        size: this.data.size,
        status: this.data.status
      }),
      api.get('/supplier/products')
    ]).then(function (results) {
      const productMap = {}
      ;(results[1] || []).forEach(function (row) {
        const product = row.product || {}
        productMap[product.skuId] = row.skuName || ('SKU' + product.skuId)
      })
      const data = results[0] || {}
      const rows = (data.records || []).map(function (row) {
        const order = row.order || {}
        const items = (row.items || []).map(function (item) {
          return Object.assign({}, item, {
            name: productMap[item.skuId] || ('SKU' + item.skuId),
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
          itemCount: items.length,
          summary: items.slice(0, 2).map(function (item) {
            return item.name
          }).join('、') + (items.length > 2 ? ' 等' : '')
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
        error: err.message || '供货单加载失败'
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
    if (!detail) {
      return
    }
    this.setData({ detail: detail, showDetail: true })
  },

  closeDetail() {
    this.setData({ showDetail: false })
  },

  stopTap() {},

  accept() {
    this.act('接单', '确定接受该供货单吗？', function (id, that) {
      api.post('/supplier/supply-orders/' + id + '/accept', {}, {}, { loading: true })
    })
  },

  reject() {
    const that = this
    const detail = this.data.detail
    if (!detail) {
      return
    }
    wx.showModal({
      title: '拒单原因',
      editable: true,
      placeholderText: '请输入拒单原因',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        const reason = res.content || '缺货'
        that.setData({ acting: true })
        api.post('/supplier/supply-orders/' + detail.order.id + '/reject', {}, { reason: reason }, { loading: true }).then(function () {
          wx.showToast({ title: '已拒单', icon: 'success' })
          that.setData({ acting: false, showDetail: false })
          that.loadData(true)
        }).catch(function () {
          that.setData({ acting: false })
        })
      }
    })
  },

  ship() {
    const that = this
    const detail = this.data.detail
    if (!detail) {
      return
    }
    wx.showModal({
      title: '填写物流单号',
      editable: true,
      placeholderText: '物流单号（选填）',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        that.setData({ acting: true })
        api.post('/supplier/supply-orders/' + detail.order.id + '/ship', {}, { logisticsNo: res.content || '' }, { loading: true }).then(function () {
          wx.showToast({ title: '发货成功', icon: 'success' })
          that.setData({ acting: false, showDetail: false })
          that.loadData(true)
        }).catch(function () {
          that.setData({ acting: false })
        })
      }
    })
  },

  act(title, content, requestFn) {
    const that = this
    const detail = this.data.detail
    if (!detail) {
      return
    }
    wx.showModal({
      title: title,
      content: content,
      success: function (res) {
        if (!res.confirm) {
          return
        }
        that.setData({ acting: true })
        requestFn(detail.order.id, that).then(function () {
          wx.showToast({ title: '操作成功', icon: 'success' })
          that.setData({ acting: false, showDetail: false })
          that.loadData(true)
        }).catch(function () {
          that.setData({ acting: false })
        })
      }
    })
  }
})
