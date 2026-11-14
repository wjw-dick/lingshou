const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    loading: true,
    error: '',
    rows: [],
    showForm: false,
    editing: false,
    form: {
      skuId: '',
      skuName: '',
      supplyPrice: ''
    },
    submitting: false
  },

  onShow() {
    this.loadProducts()
  },

  onPullDownRefresh() {
    this.loadProducts()
  },

  loadProducts() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/supplier/products').then(function (list) {
      const rows = (list || []).map(function (row) {
        const product = row.product || {}
        return {
          skuId: product.skuId,
          skuName: row.skuName || ('SKU' + product.skuId),
          skuCode: row.skuCode,
          barcode: row.barcode,
          supplyPrice: product.supplyPrice,
          priceText: format.money(product.supplyPrice),
          auditStatus: product.auditStatus || 'PENDING',
          auditText: product.auditStatus === 'APPROVED' ? '已审核' : (product.auditStatus === 'REJECTED' ? '已驳回' : '待审核')
        }
      })
      that.setData({ rows: rows, loading: false, error: '' })
      wx.stopPullDownRefresh()
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '供货商品加载失败'
      })
      wx.stopPullDownRefresh()
    })
  },

  openCreate() {
    this.setData({
      showForm: true,
      editing: false,
      form: { skuId: '', skuName: '', supplyPrice: '' }
    })
  },

  openEdit(event) {
    const index = Number(event.currentTarget.dataset.index)
    const row = this.data.rows[index]
    if (!row) {
      return
    }
    this.setData({
      showForm: true,
      editing: true,
      form: {
        skuId: row.skuId,
        skuName: row.skuName,
        supplyPrice: String(row.supplyPrice === null || row.supplyPrice === undefined ? '' : row.supplyPrice)
      }
    })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  stopTap() {},

  onFormInput(event) {
    const field = event.currentTarget.dataset.field
    const form = Object.assign({}, this.data.form)
    form[field] = event.detail.value
    this.setData({ form: form })
  },

  save() {
    const form = this.data.form
    if (!form.skuId) {
      wx.showToast({ title: '请输入商品 SKU ID', icon: 'none' })
      return
    }
    if (form.supplyPrice === '' || Number(form.supplyPrice) < 0) {
      wx.showToast({ title: '请输入正确供货价', icon: 'none' })
      return
    }
    const that = this
    this.setData({ submitting: true })
    api.post('/supplier/products', {
      skuId: Number(form.skuId),
      supplyPrice: Number(form.supplyPrice)
    }, {}, { loading: true, loadingText: '正在保存' }).then(function () {
      wx.showToast({ title: '已提交审核', icon: 'success' })
      that.setData({ submitting: false, showForm: false })
      that.loadProducts()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
