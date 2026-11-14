const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    loading: true,
    error: '',
    storeId: 1,
    shift: null,
    openingCash: '',
    actualCash: '',
    diffReason: '',
    submitting: false
  },

  onShow() {
    this.setData({ storeId: wx.getStorageSync('storeId') || 1 })
    this.loadShift()
  },

  loadShift() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/tenant/shifts/current', { storeId: this.data.storeId }).then(function (shift) {
      that.setData({
        shift: shift ? Object.assign({}, shift, {
          statusText: format.statusText(shift.status),
          startText: format.formatDate(shift.startAt),
          endText: format.formatDate(shift.endAt),
          openingText: format.money(shift.openingCash),
          cashText: format.money(shift.cashSales),
          wechatText: format.money(shift.wechatSales),
          alipayText: format.money(shift.alipaySales),
          walletText: format.money(shift.walletSales),
          expectedText: format.money(shift.expectedCash),
          actualText: format.money(shift.actualCash),
          diffText: format.money(shift.diffAmount)
        }) : null,
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '交接班信息加载失败'
      })
    })
  },

  onOpeningInput(event) {
    this.setData({ openingCash: event.detail.value })
  },

  onActualInput(event) {
    this.setData({ actualCash: event.detail.value })
  },

  onDiffReasonInput(event) {
    this.setData({ diffReason: event.detail.value })
  },

  openShift() {
    const that = this
    this.setData({ submitting: true })
    api.post('/tenant/shifts/open', {}, {
      storeId: this.data.storeId,
      openingCash: Number(this.data.openingCash) || 0
    }, { loading: true, loadingText: '正在开班' }).then(function () {
      wx.showToast({ title: '开班成功', icon: 'success' })
      that.setData({ submitting: false, openingCash: '' })
      that.loadShift()
    }).catch(function () {
      that.setData({ submitting: false })
    })
  },

  closeShift() {
    const shift = this.data.shift
    if (!shift) {
      return
    }
    const that = this
    wx.showModal({
      title: '确认交班',
      content: '交班后将统计本班次收款并关闭交接班，确定继续吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        that.setData({ submitting: true })
        api.post('/tenant/shifts/' + shift.id + '/close', {
          actualCash: that.data.actualCash === '' ? null : Number(that.data.actualCash),
          diffReason: that.data.diffReason
        }, {}, { loading: true, loadingText: '正在交班' }).then(function () {
          wx.showToast({ title: '交班成功', icon: 'success' })
          that.setData({ submitting: false, actualCash: '', diffReason: '' })
          that.loadShift()
        }).catch(function () {
          that.setData({ submitting: false })
        })
      }
    })
  }
})
