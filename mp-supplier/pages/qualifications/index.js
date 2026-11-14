const api = require('../../utils/request')
const format = require('../../utils/format')

const TYPES = [
  { value: 'LICENSE', label: '营业执照' },
  { value: 'FOOD_PERMIT', label: '食品经营许可证' },
  { value: 'INSPECTION', label: '质检报告' },
  { value: 'OTHER', label: '其他资质' }
]

Page({
  data: {
    loading: true,
    error: '',
    supplier: null,
    qualifications: [],
    showForm: false,
    types: TYPES,
    typeNames: TYPES.map(function (item) {
      return item.label
    }),
    typeName: TYPES[0].label,
    typeIndex: 0,
    form: {
      fileUrl: '',
      expireAt: ''
    },
    submitting: false
  },

  onLoad() {
    this.loadProfile()
  },

  loadProfile() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/supplier/profile').then(function (data) {
      that.setData({
        supplier: data.supplier || null,
        qualifications: (data.qualifications || []).map(function (item) {
          return Object.assign({}, item, {
            typeText: (TYPES.filter(function (type) {
              return type.value === item.type
            })[0] || {}).label || item.type,
            expireText: format.formatDay(item.expireAt),
            statusText: format.statusText(item.status)
          })
        }),
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '资质信息加载失败'
      })
    })
  },

  openForm() {
    this.setData({
      showForm: true,
      typeIndex: 0,
      form: { fileUrl: '', expireAt: '' }
    })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  stopTap() {},

  onTypeChange(event) {
    const index = Number(event.detail.value)
    this.setData({
      typeIndex: index,
      typeName: this.data.typeNames[index] || ''
    })
  },

  onDateChange(event) {
    this.setData({ 'form.expireAt': event.detail.value })
  },

  onFormInput(event) {
    const field = event.currentTarget.dataset.field
    const form = Object.assign({}, this.data.form)
    form[field] = event.detail.value
    this.setData({ form: form })
  },

  save() {
    if (!this.data.form.fileUrl) {
      wx.showToast({ title: '请填写资质文件地址', icon: 'none' })
      return
    }
    const that = this
    this.setData({ submitting: true })
    api.post('/supplier/qualifications', {
      type: TYPES[this.data.typeIndex].value,
      fileUrl: this.data.form.fileUrl,
      expireAt: this.data.form.expireAt || null
    }, {}, { loading: true, loadingText: '正在提交' }).then(function () {
      wx.showToast({ title: '资质已提交', icon: 'success' })
      that.setData({ submitting: false, showForm: false })
      that.loadProfile()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
