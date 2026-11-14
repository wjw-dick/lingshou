const CART_KEY = 'cart'

function getCart() {
  return wx.getStorageSync(CART_KEY) || []
}

function saveCart(list) {
  wx.setStorageSync(CART_KEY, list)
}

function addToCart(product, qty) {
  const count = qty || 1
  const list = getCart()
  const sku = product.sku || {}
  const spu = product.spu || {}
  const exist = list.filter(function (item) {
    return String(item.skuId) === String(sku.id)
  })[0]
  if (exist) {
    exist.qty += count
  } else {
    list.push({
      skuId: sku.id,
      spuId: sku.spuId || spu.id,
      name: spu.name || sku.skuCode || '商品',
      spec: sku.spec || spu.spec || '',
      price: sku.priceMember || sku.priceRetail || 0,
      image: spu.mainImage || '',
      qty: count
    })
  }
  saveCart(list)
  return list
}

function updateQty(skuId, qty) {
  const list = getCart()
  const next = list.map(function (item) {
    if (String(item.skuId) === String(skuId)) {
      item.qty = qty < 1 ? 1 : qty
    }
    return item
  })
  saveCart(next)
  return next
}

function removeItem(skuId) {
  const list = getCart().filter(function (item) {
    return String(item.skuId) !== String(skuId)
  })
  saveCart(list)
  return list
}

function clearCart() {
  wx.removeStorageSync(CART_KEY)
}

function count() {
  return getCart().reduce(function (total, item) {
    return total + item.qty
  }, 0)
}

module.exports = {
  getCart: getCart,
  saveCart: saveCart,
  addToCart: addToCart,
  updateQty: updateQty,
  removeItem: removeItem,
  clearCart: clearCart,
  count: count
}
