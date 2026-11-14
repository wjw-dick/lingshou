const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    hasToken: false,
    loading: true,
    error: '',
    member: null,
    levelName: '普通会员',
    unusedCoupons: 0,
    addresses: [],
    showAddressForm: false,
    addressForm: {
      name: '',
      phone: '',
      address: ''
    },
    saving: false,
    cartCount: 0
  },

  onShow() {
    const hasToken = !!wx.getStorageSync('token')
    this.setData({ hasToken: hasToken })
    if (!hasToken) {
      this.setData({ loading: false, member: null, addresses: [] })
      return
    }
    this.loadProfile()
    this.loadAddresses()
  },

  loadProfile() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/app/profile').then(function (data) {
      const member = data.member || {}
      const level = data.level || {}
      that.setData({
        member: Object.assign({}, member, {
          initial: (member.nickname || '微').slice(0, 1),
          walletText: format.money(member.walletBalance),
          consumeText: format.money(member.totalConsume)
        }),
        levelName: level.name || '普通会员',
        unusedCoupons: data.unusedCoupons || 0,
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '会员信息加载失败'
      })
    })
  },

  loadAddresses() {
    const that = this
    api.get('/app/addresses').then(function (list) {
      that.setData({ addresses: list || [] })
    }).catch(function () {
      that.setData({ addresses: [] })
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/order/index' })
  },

  goCoupons() {
    wx.navigateTo({ url: '/pages/coupon/index' })
  },

  goCart() {
    wx.navigateTo({ url: '/pages/cart/index' })
  },

  goStores() {
    wx.switchTab({ url: '/pages/store/index' })
  },

  toggleAddressForm() {
    this.setData({ showAddressForm: !this.data.showAddressForm })
  },

  onAddressInput(event) {
    const field = event.currentTarget.dataset.field
    const form = Object.assign({}, this.data.addressForm)
    form[field] = event.detail.value
    this.setData({ addressForm: form })
  },

  saveAddress() {
    const form = this.data.addressForm
    if (!form.name || !form.phone || !form.address) {
      wx.showToast({ title: '请填写完整地址信息', icon: 'none' })
      return
    }
    const that = this
    this.setData({ saving: true })
    api.post('/app/addresses', {
      name: form.name,
      phone: form.phone,
      address: form.address,
      isDefault: this.data.addresses.length ? 0 : 1
    }, {}, { loading: true }).then(function () {
      wx.showToast({ title: '地址已保存', icon: 'success' })
      that.setData({
        saving: false,
        showAddressForm: false,
        addressForm: { name: '', phone: '', address: '' }
      })
      that.loadAddresses()
    }).catch(function () {
      that.setData({ saving: false })
    })
  },

  deleteAddress(event) {
    const address = this.data.addresses[Number(event.currentTarget.dataset.index)]
    if (!address) {
      return
    }
    const that = this
    wx.showModal({
      title: '删除地址',
      content: '确定删除该地址吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        api.del('/app/addresses/' + address.id).then(function () {
          wx.showToast({ title: '已删除', icon: 'success' })
          that.loadAddresses()
        }).catch(function () {})
      }
    })
  },

  setDefaultAddress(event) {
    const address = this.data.addresses[Number(event.currentTarget.dataset.index)]
    if (!address || address.isDefault === 1) {
      return
    }
    const that = this
    api.put('/app/addresses/' + address.id, {
      name: address.name,
      phone: address.phone,
      address: address.address,
      isDefault: 1
    }).then(function () {
      wx.showToast({ title: '已设为默认', icon: 'success' })
      that.loadAddresses()
    }).catch(function () {})
  },

  logout() {
    const that = this
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      success: function (res) {
        if (!res.confirm) {
          return
        }
        api.post('/auth/logout', {}, {}, { auth: false }).catch(function () {})
        wx.removeStorageSync('token')
        wx.removeStorageSync('user')
        wx.removeStorageSync('selectedStore')
        that.setData({ hasToken: false, member: null, addresses: [] })
        wx.reLaunch({ url: '/pages/login/index' })
      }
    })
  }
})
