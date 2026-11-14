const api = require('../../utils/request')
const format = require('../../utils/format')

Page({
  data: {
    loading: true,
    error: '',
    storeId: '',
    store: null,
    rule: null,
    ruleTexts: {
      minOrder: '0.00',
      deliveryFee: '0.00',
      freeThreshold: '0.00'
    }
  },

  onLoad(options) {
    this.setData({ storeId: options.id || '' })
    this.loadDetail()
  },

  loadDetail() {
    const that = this
    const storeId = this.data.storeId
    this.setData({ loading: true, error: '' })
    Promise.all([
      api.get('/public/site/yxbl/stores', {}, { auth: false }),
      api.get('/public/site/yxbl/stores/' + storeId + '/delivery-rule', {}, { auth: false }).catch(function () {
        return null
      })
    ]).then(function (results) {
      const list = results[0] || []
      const store = list.filter(function (item) {
        return String(item.id) === String(storeId)
      })[0]
      if (!store) {
        that.setData({ loading: false, error: '门店不存在或已下线' })
        return
      }
      const rule = results[1]
      that.setData({
        store: Object.assign({}, store, {
          initial: (store.name || '店').slice(0, 1)
        }),
        rule: rule,
        ruleTexts: {
          minOrder: format.money(rule && rule.minOrderAmount),
          deliveryFee: format.money(rule && rule.deliveryFee),
          freeThreshold: format.money(rule && rule.freeThreshold)
        },
        loading: false,
        error: ''
      })
    }).catch(function (err) {
      that.setData({
        loading: false,
        error: err.message || '门店详情加载失败'
      })
    })
  },

  callStore() {
    const store = this.data.store
    if (store && (store.phone || store.managerPhone)) {
      wx.makePhoneCall({ phoneNumber: store.phone || store.managerPhone })
    } else {
      wx.showToast({ title: '暂无联系电话', icon: 'none' })
    }
  },

  chooseStore() {
    const store = this.data.store
    if (!store) {
      return
    }
    wx.setStorageSync('selectedStore', store)
    wx.showToast({ title: '已选择门店', icon: 'success' })
    setTimeout(function () {
      wx.switchTab({ url: '/pages/home/index' })
    }, 400)
  }
})
