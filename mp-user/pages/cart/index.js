const api = require('../../utils/request')
const format = require('../../utils/format')
const cart = require('../../utils/cart')

Page({
  data: {
    loading: false,
    error: '',
    hasToken: false,
    cartItems: [],
    selectedStore: null,
    deliveryType: 'PICKUP',
    deliveryName: '',
    deliveryPhone: '',
    deliveryAddress: '',
    remark: '',
    addresses: [],
    addressNames: [],
    couponNames: ['不使用优惠券'],
    selectedCouponName: '不使用优惠券',
    coupons: [],
    selectedCouponIndex: 0,
    memberPoints: 0,
    pointsUsed: '',
    subtotalText: '0.00',
    discountText: '0.00',
    pointsDiscountText: '0.00',
    freightText: '0.00',
    payableText: '0.00',
    submitting: false,
    deliveryRule: null
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({
      hasToken: hasToken,
      selectedStore: wx.getStorageSync('selectedStore') || null
    })
    this.refreshCart()
    if (hasToken) {
      this.loadProfile()
      this.loadCoupons()
      this.loadAddresses()
    }
  },

  refreshCart() {
    const raw = cart.getCart()
    const items = raw.map(function (item) {
      const price = Number(item.price) || 0
      return Object.assign({}, item, {
        initial: (item.name || '商').slice(0, 1),
        priceText: format.money(price),
        amountText: format.money(price * item.qty)
      })
    })
    this.setData({ cartItems: items })
    this.calcAmount()
  },

  loadProfile() {
    const that = this
    api.get('/app/profile').then(function (data) {
      const member = data.member || {}
      that.setData({ memberPoints: member.points || 0 })
      that.calcAmount()
    }).catch(function () {})
  },

  loadCoupons() {
    const that = this
    api.get('/app/coupons').then(function (data) {
      const list = (data.myCoupons || []).filter(function (row) {
        return row.memberCoupon && row.memberCoupon.status === 'UNUSED' && row.coupon
      }).map(function (row) {
        const coupon = row.coupon
        const threshold = Number(coupon.thresholdAmount) || 0
        let desc = '无门槛'
        if (threshold > 0) {
          desc = '满 ' + format.money(threshold) + ' 元可用'
        }
        if (coupon.type === 'DISCOUNT') {
          desc = '折扣券 · ' + format.money(coupon.discountRate) + ' 折'
        }
        return {
          memberCouponId: row.memberCoupon.id,
          couponId: coupon.id,
          name: coupon.name,
          desc: desc,
          type: coupon.type,
          threshold: threshold,
          discountAmount: Number(coupon.discountAmount) || 0,
          discountRate: Number(coupon.discountRate) || 0
        }
      })
      const names = ['不使用优惠券'].concat(list.map(function (item) {
        return item.name + '（' + item.desc + '）'
      }))
      that.setData({
        coupons: list,
        couponNames: names,
        selectedCouponName: names[0],
        selectedCouponIndex: 0
      })
      that.calcAmount()
    }).catch(function () {})
  },

  loadAddresses() {
    const that = this
    api.get('/app/addresses').then(function (list) {
      const addresses = list || []
      that.setData({
        addresses: addresses,
        addressNames: addresses.map(function (address) {
          return (address.name || '') + ' ' + (address.phone || '') + ' ' + (address.address || '')
        })
      })
    }).catch(function () {})
  },

  onAddressChange(event) {
    const index = Number(event.detail.value)
    const address = this.data.addresses[index]
    if (!address) {
      return
    }
    this.setData({
      deliveryName: address.name || '',
      deliveryPhone: address.phone || '',
      deliveryAddress: address.address || ''
    })
  },

  onCouponChange(event) {
    const index = Number(event.detail.value)
    this.setData({
      selectedCouponIndex: index,
      selectedCouponName: this.data.couponNames[index] || '不使用优惠券'
    })
    this.calcAmount()
  },

  onFieldInput(event) {
    const field = event.currentTarget.dataset.field
    const data = {}
    data[field] = event.detail.value
    this.setData(data)
  },

  selectType(event) {
    this.setData({ deliveryType: event.currentTarget.dataset.type })
    this.calcAmount()
    if (event.currentTarget.dataset.type === 'DELIVERY' && this.data.selectedStore) {
      this.loadDeliveryRule(this.data.selectedStore.id)
    }
  },

  loadDeliveryRule(storeId) {
    const that = this
    api.get('/public/site/yxbl/stores/' + storeId + '/delivery-rule', {}, { auth: false }).then(function (rule) {
      that.setData({ deliveryRule: rule || null })
      that.calcAmount()
    }).catch(function () {})
  },

  minusItem(event) {
    const skuId = event.currentTarget.dataset.skuid
    const item = this.data.cartItems.filter(function (row) {
      return String(row.skuId) === String(skuId)
    })[0]
    if (!item || item.qty <= 1) {
      return
    }
    cart.updateQty(skuId, item.qty - 1)
    this.refreshCart()
  },

  plusItem(event) {
    const skuId = event.currentTarget.dataset.skuid
    const item = this.data.cartItems.filter(function (row) {
      return String(row.skuId) === String(skuId)
    })[0]
    if (!item) {
      return
    }
    cart.updateQty(skuId, item.qty + 1)
    this.refreshCart()
  },

  removeItem(event) {
    const skuId = event.currentTarget.dataset.skuid
    const that = this
    wx.showModal({
      title: '移除商品',
      content: '确定从购物车移除该商品吗？',
      success: function (res) {
        if (res.confirm) {
          cart.removeItem(skuId)
          that.refreshCart()
        }
      }
    })
  },

  calcAmount() {
    const items = this.data.cartItems
    const subtotal = items.reduce(function (total, item) {
      return total + Number(item.price) * item.qty
    }, 0)
    let discount = 0
    const selected = this.data.coupons[this.data.selectedCouponIndex - 1]
    if (selected) {
      if (subtotal >= selected.threshold) {
        if (selected.type === 'DISCOUNT') {
          discount = subtotal - subtotal * selected.discountRate / 10
        } else {
          discount = selected.discountAmount
        }
      }
    }
    if (discount > subtotal) {
      discount = subtotal
    }
    let pointsUsed = Number(this.data.pointsUsed) || 0
    if (pointsUsed > this.data.memberPoints) {
      pointsUsed = this.data.memberPoints
    }
    if (pointsUsed < 0) {
      pointsUsed = 0
    }
    const pointsDiscount = pointsUsed / 100
    let freight = 0
    if (this.data.deliveryType === 'DELIVERY' && this.data.deliveryRule) {
      const rule = this.data.deliveryRule
      const threshold = Number(rule.freeThreshold) || 0
      if (!threshold || subtotal < threshold) {
        freight = Number(rule.deliveryFee) || 0
      }
    }
    let payable = subtotal - discount - pointsDiscount + freight
    if (payable < 0) {
      payable = 0
    }
    this.setData({
      subtotalText: format.money(subtotal),
      discountText: format.money(discount),
      pointsDiscountText: format.money(pointsDiscount),
      freightText: format.money(freight),
      payableText: format.money(payable)
    })
  },

  goStores() {
    wx.switchTab({ url: '/pages/store/index' })
  },

  goShopping() {
    wx.navigateTo({ url: '/pages/product/index' })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  submitOrder() {
    if (!this.data.hasToken) {
      this.goLogin()
      return
    }
    if (!this.data.cartItems.length) {
      wx.showToast({ title: '购物车为空', icon: 'none' })
      return
    }
    if (!this.data.selectedStore) {
      wx.showToast({ title: '请先选择门店', icon: 'none' })
      return
    }
    if (this.data.deliveryType === 'DELIVERY') {
      if (!this.data.deliveryName || !this.data.deliveryPhone || !this.data.deliveryAddress) {
        wx.showToast({ title: '请填写完整配送信息', icon: 'none' })
        return
      }
    }
    const selected = this.data.coupons[this.data.selectedCouponIndex - 1]
    const payload = {
      storeId: this.data.selectedStore.id,
      deliveryType: this.data.deliveryType,
      memberCouponId: selected ? selected.memberCouponId : null,
      pointsUsed: Number(this.data.pointsUsed) || 0,
      deliveryName: this.data.deliveryName,
      deliveryPhone: this.data.deliveryPhone,
      deliveryAddress: this.data.deliveryAddress,
      remark: this.data.remark,
      items: this.data.cartItems.map(function (item) {
        return { skuId: item.skuId, qty: item.qty }
      })
    }
    const that = this
    this.setData({ submitting: true })
    api.post('/app/orders', payload, {}, { loading: true, loadingText: '正在提交订单' }).then(function (order) {
      cart.clearCart()
      that.setData({ submitting: false })
      wx.redirectTo({ url: '/pages/order/detail?id=' + order.id + '&pay=1' })
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
