const api = require('../../utils/request')
const format = require('../../utils/format')

function couponValue(coupon) {
  if (coupon.type === 'DISCOUNT') {
    return format.money(coupon.discountRate) + '折'
  }
  return '¥' + format.money(coupon.discountAmount)
}

Page({
  data: {
    hasToken: false,
    loading: true,
    error: '',
    tab: 'mine',
    myCoupons: [],
    available: [],
    claiming: 0
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({ hasToken: hasToken })
    if (hasToken) {
      this.loadCoupons()
    } else {
      this.setData({ loading: false, myCoupons: [], available: [] })
    }
  },

  loadCoupons() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/app/coupons').then(function (data) {
      const myCoupons = (data.myCoupons || []).map(function (row) {
        const coupon = row.coupon || {}
        const memberCoupon = row.memberCoupon || {}
        return {
          key: memberCoupon.id,
          memberCoupon: memberCoupon,
          coupon: coupon,
          name: coupon.name,
          value: couponValue(coupon),
          threshold: Number(coupon.thresholdAmount) > 0
            ? '满 ' + format.money(coupon.thresholdAmount) + ' 元可用'
            : '无门槛',
          validity: format.formatDay(coupon.validFrom) + ' 至 ' + format.formatDay(coupon.validTo),
          statusText: format.statusText(memberCoupon.status),
          usable: memberCoupon.status === 'UNUSED'
        }
      })
      const available = (data.available || []).map(function (row) {
        const coupon = row.coupon || {}
        return {
          key: coupon.id,
          coupon: coupon,
          name: coupon.name,
          value: couponValue(coupon),
          threshold: Number(coupon.thresholdAmount) > 0
            ? '满 ' + format.money(coupon.thresholdAmount) + ' 元可用'
            : '无门槛',
          validity: format.formatDay(coupon.validFrom) + ' 至 ' + format.formatDay(coupon.validTo),
          claimable: !!row.claimable,
          owned: row.owned || 0
        }
      })
      that.setData({
        myCoupons: myCoupons,
        available: available,
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '优惠券加载失败'
      })
    })
  },

  switchTab(event) {
    this.setData({ tab: event.currentTarget.dataset.tab })
  },

  claim(event) {
    const couponId = event.currentTarget.dataset.id
    const that = this
    this.setData({ claiming: couponId })
    api.post('/app/coupons/' + couponId + '/claim', {}, {}, { loading: true }).then(function () {
      wx.showToast({ title: '领取成功', icon: 'success' })
      that.setData({ claiming: 0, tab: 'mine' })
      that.loadCoupons()
    }).catch(function () {
      that.setData({ claiming: 0 })
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  }
})
