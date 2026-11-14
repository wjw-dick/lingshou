<template>
  <div class="pos-page">
    <div class="pos-top">
      <div class="pos-title">
        <el-icon :size="20"><ShoppingCart /></el-icon>
        <div>
          <strong>轻量收银</strong>
          <span>{{ storeName || '请选择门店' }} · {{ shift ? `班次 ${shift.shiftNo}` : '未开班' }}</span>
        </div>
      </div>
      <div class="pos-actions">
        <el-select v-model="storeId" placeholder="选择门店" style="width: 210px" @change="onStoreChange">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
        <el-button v-if="!shift" :icon="Timer" @click="openShift">开班</el-button>
        <el-button v-else :icon="SwitchButton" @click="shiftDialog = true">交班</el-button>
        <el-button :icon="Box" @click="holdDialog = true">挂单 {{ holds.length ? `(${holds.length})` : '' }}</el-button>
        <el-button :icon="Back" @click="router.push('/dashboard')">返回</el-button>
      </div>
    </div>

    <div class="pos-body">
      <section class="product-area">
        <div class="search-row">
          <el-input
            ref="barcodeInput"
            v-model="keyword"
            size="large"
            placeholder="扫描条码 / 搜索商品，回车快速加购"
            clearable
            @keyup.enter="scanOrSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button size="large" @click="scanOrSearch">查询</el-button>
        </div>

        <div class="category-row">
          <button :class="{ active: !categoryId }" @click="categoryId = null">全部</button>
          <button
            v-for="cat in categories"
            :key="cat.id"
            :class="{ active: categoryId === cat.id }"
            @click="categoryId = cat.id"
          >{{ cat.name }}</button>
        </div>

        <div v-loading="productsLoading" class="product-grid">
          <button v-for="product in filteredProducts" :key="product.skuId" class="product-card" @click="addToCart(product)">
            <div class="product-img">
              <img v-if="product.image" :src="product.image" alt="" />
              <div v-else class="product-placeholder">{{ product.name?.slice(0, 2) }}</div>
              <span v-if="product.stock <= 5" class="stock-warning">库存 {{ product.stock }}</span>
            </div>
            <div class="product-info">
              <strong>{{ product.name }}</strong>
              <span>{{ product.spec || product.skuCode }}</span>
              <div class="product-price">
                <em>¥{{ money(product.memberPrice || product.price) }}</em>
                <small v-if="member">会员价</small>
                <small v-else>¥{{ money(product.price) }}</small>
              </div>
            </div>
          </button>
          <div v-if="!productsLoading && !filteredProducts.length" class="product-empty">
            <el-empty description="没有匹配的商品" :image-size="80" />
          </div>
        </div>
      </section>

      <aside class="cart-area">
        <div class="cart-head">
          <div class="cart-member">
            <template v-if="member">
              <div class="member-avatar">{{ member.nickname?.slice(0, 1) }}</div>
              <div>
                <strong>{{ member.nickname }}</strong>
                <span>{{ member.phoneMask }} · {{ member.points }} 积分 · ¥{{ money(member.walletBalance) }}</span>
              </div>
              <el-button link type="danger" size="small" @click="clearMember">移除</el-button>
            </template>
            <template v-else>
              <div class="member-avatar guest"><el-icon><User /></el-icon></div>
              <div>
                <strong>散客</strong>
                <span>识别会员可享会员价与积分</span>
              </div>
              <el-button size="small" type="primary" plain @click="memberDialog = true">识别会员</el-button>
            </template>
          </div>
        </div>

        <div class="cart-items">
          <div v-for="item in cart" :key="item.skuId" class="cart-item">
            <div class="cart-item-main">
              <strong>{{ item.name }}</strong>
              <span>{{ item.spec || item.skuCode }} · ¥{{ money(item.price) }}</span>
            </div>
            <div class="qty-control">
              <button @click="changeQty(item, -1)">−</button>
              <span>{{ item.qty }}</span>
              <button @click="changeQty(item, 1)">+</button>
            </div>
            <span class="cart-amount">¥{{ money(item.price * item.qty) }}</span>
            <el-button link type="danger" size="small" @click="removeItem(item)"><el-icon><Delete /></el-icon></el-button>
          </div>
          <el-empty v-if="!cart.length" description="购物车为空，点击商品加购" :image-size="70" />
        </div>

        <div class="cart-summary">
          <div class="summary-row">
            <span>商品金额</span><span>¥{{ money(preview?.grossAmount ?? localGross) }}</span>
          </div>
          <div class="summary-row">
            <span>优惠券</span>
            <el-select v-model="memberCouponId" size="small" placeholder="选择优惠券" clearable style="width: 150px" @change="refreshPreview">
              <el-option v-for="coupon in coupons" :key="coupon.memberCouponId" :label="coupon.name" :value="coupon.memberCouponId" />
            </el-select>
          </div>
          <div class="summary-row" v-if="member">
            <span>积分抵扣</span>
            <el-input-number v-model="pointsUsed" :min="0" :max="member.points" :step="100" size="small" controls-position="right" style="width: 120px" @change="refreshPreview" />
          </div>
          <div class="summary-row discount" v-if="Number(preview?.couponDiscount || 0) > 0 || Number(preview?.pointsDiscount || 0) > 0">
            <span>优惠合计</span>
            <span>-¥{{ money(Number(preview?.couponDiscount || 0) + Number(preview?.pointsDiscount || 0)) }}</span>
          </div>
          <div class="summary-total">
            <span>应收</span>
            <strong>¥{{ money(preview?.payableAmount ?? localGross) }}</strong>
          </div>
          <div class="pay-methods">
            <button v-for="method in payMethods" :key="method.value"
                    :class="{ active: paymentMethod === method.value }"
                    @click="paymentMethod = method.value">
              <el-icon><component :is="method.icon" /></el-icon>{{ method.label }}
            </button>
          </div>
          <div v-if="member" class="wallet-row">
            <el-checkbox v-model="useWallet" @change="refreshPreview">使用储值余额（¥{{ money(member.walletBalance) }}）</el-checkbox>
          </div>
          <el-input v-model="remark" placeholder="备注（可选）" size="small" style="margin: 8px 0" />
          <el-button type="primary" size="large" class="checkout-btn" :disabled="!cart.length" @click="checkout">
            结算 ¥{{ money(preview?.payableAmount ?? localGross) }}
          </el-button>
          <el-button text class="clear-btn" @click="clearCart">清空购物车</el-button>
        </div>
      </aside>
    </div>

    <el-dialog v-model="memberDialog" title="识别会员" width="520px">
      <el-input v-model="memberKeyword" placeholder="输入手机号后四位 / 昵称 / 会员ID，回车搜索" @keyup.enter="searchMembers">
        <template #append><el-button @click="searchMembers">搜索</el-button></template>
      </el-input>
      <div class="member-results">
        <button v-for="m in memberResults" :key="m.id" class="member-result" @click="selectMember(m)">
          <div class="member-avatar">{{ m.nickname?.slice(0, 1) }}</div>
          <div>
            <strong>{{ m.nickname }}</strong>
            <span>{{ m.phoneMask }} · {{ m.points }} 积分 · 储值 ¥{{ money(m.walletBalance) }}</span>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </button>
        <el-empty v-if="!memberResults.length" description="输入信息搜索会员" :image-size="60" />
      </div>
    </el-dialog>

    <el-dialog v-model="cashDialog" title="现金收款" width="420px">
      <div class="cash-box">
        <span>应收金额</span>
        <strong>¥{{ money(preview?.payableAmount ?? localGross) }}</strong>
      </div>
      <el-form label-width="90px">
        <el-form-item label="实收现金">
          <el-input-number v-model="cashReceived" :min="0" :precision="2" :controls="false" style="width: 100%" size="large" />
        </el-form-item>
        <el-form-item label="找零">
          <span class="change">¥{{ money(Math.max(0, cashReceived - Number(preview?.payableAmount ?? localGross))) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cashDialog = false">取消</el-button>
        <el-button type="primary" :loading="checkingOut" @click="confirmCheckout">确认收款</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shiftDialog" :title="shift ? '交班核对' : '开班'" width="460px">
      <el-form label-width="100px" v-if="!shift">
        <el-form-item label="备用金">
          <el-input-number v-model="openingCash" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-form label-width="100px" v-else>
        <el-form-item label="班次"><span>{{ shift.shiftNo }}</span></el-form-item>
        <el-form-item label="现金销售"><span>¥{{ money(shift.cashSales) }}</span></el-form-item>
        <el-form-item label="应有现金"><span>¥{{ money(shift.expectedCash) }}</span></el-form-item>
        <el-form-item label="实点现金">
          <el-input-number v-model="actualCash" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="差异说明">
          <el-input v-model="diffReason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shiftDialog = false">取消</el-button>
        <el-button type="primary" @click="submitShift">{{ shift ? '确认交班' : '开班' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="holdDialog" title="挂单管理" width="560px">
      <div style="display:flex;justify-content:flex-end;margin-bottom:10px">
        <el-button size="small" type="primary" :disabled="!cart.length" @click="saveHold">将当前购物车挂起</el-button>
      </div>
      <div class="hold-list">
        <div v-for="(hold, index) in holds" :key="hold.id" class="hold-item">
          <div>
            <strong>{{ hold.label }}</strong>
            <span>{{ hold.items.length }} 件商品 · {{ hold.time }}</span>
          </div>
          <div>
            <el-button size="small" type="primary" plain @click="resumeHold(index)">取单</el-button>
            <el-button size="small" type="danger" plain @click="deleteHold(index)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!holds.length" description="暂无挂单" :image-size="70" />
      </div>
    </el-dialog>

    <el-dialog v-model="successDialog" title="收款成功" width="460px" class="success-dialog">
      <div class="success-box">
        <div class="success-icon"><el-icon :size="30"><Check /></el-icon></div>
        <h3>¥{{ money(lastOrder?.paidAmount) }}</h3>
        <p>订单号 {{ lastOrder?.orderNo }}</p>
        <p v-if="cashReceived > 0">实收 ¥{{ money(cashReceived) }}，找零 ¥{{ money(Math.max(0, cashReceived - Number(lastOrder?.paidAmount || 0))) }}</p>
      </div>
      <div ref="receiptRef" class="receipt">
        <h4>{{ storeName }}</h4>
        <p>订单：{{ lastOrder?.orderNo }}</p>
        <p>时间：{{ lastOrder?.paidAt || nowText }}</p>
        <div class="receipt-line" v-for="item in cartSnapshot" :key="item.skuId">
          <span>{{ item.name }} × {{ item.qty }}</span><span>¥{{ money(item.price * item.qty) }}</span>
        </div>
        <div class="receipt-line total"><span>实付</span><span>¥{{ money(lastOrder?.paidAmount) }}</span></div>
        <p class="receipt-thanks">谢谢惠顾，欢迎再次光临</p>
      </div>
      <template #footer>
        <el-button @click="printReceipt">打印小票</el-button>
        <el-button type="primary" @click="closeSuccess">继续收银</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Box, Delete, Timer, SwitchButton } from '@element-plus/icons-vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const router = useRouter()
const stores = ref<any[]>([])
const storeId = ref<number>()
const storeName = computed(() => stores.value.find((s) => s.id === storeId.value)?.name || '')
const categories = ref<any[]>([])
const categoryId = ref<number | null>(null)
const products = ref<any[]>([])
const productsLoading = ref(false)
const keyword = ref('')
const cart = ref<any[]>([])
const member = ref<any>(null)
const memberDialog = ref(false)
const memberKeyword = ref('')
const memberResults = ref<any[]>([])
const coupons = ref<any[]>([])
const memberCouponId = ref<number | null>(null)
const pointsUsed = ref(0)
const useWallet = ref(false)
const paymentMethod = ref('CASH')
const remark = ref('')
const preview = ref<any>(null)
const previewTimer = ref<any>(null)
const shift = ref<any>(null)
const shiftDialog = ref(false)
const openingCash = ref(500)
const actualCash = ref(0)
const diffReason = ref('')
const cashDialog = ref(false)
const cashReceived = ref(0)
const checkingOut = ref(false)
const successDialog = ref(false)
const lastOrder = ref<any>(null)
const cartSnapshot = ref<any[]>([])
const receiptRef = ref<HTMLElement>()
const holdDialog = ref(false)
const holds = ref<any[]>([])
const nowText = ref('')
const barcodeInput = ref()

const payMethods = [
  { label: '现金', value: 'CASH', icon: 'Money' },
  { label: '微信', value: 'WECHAT', icon: 'ChatDotRound' },
  { label: '支付宝', value: 'ALIPAY', icon: 'Wallet' }
]

const filteredProducts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return products.value.filter((product) => {
    if (categoryId.value && product.categoryId !== categoryId.value) return false
    if (!kw) return true
    return product.name?.toLowerCase().includes(kw) || product.barcode?.includes(kw) || product.skuCode?.includes(kw)
  })
})

const localGross = computed(() => cart.value.reduce((sum, item) => sum + item.price * item.qty, 0))

onMounted(async () => {
  nowText.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  try {
    stores.value = await api.get('/tenant/stores/options')
    if (stores.value.length) {
      storeId.value = stores.value[0].id
      await onStoreChange()
    }
  } catch {
    // 拦截器提示
  }
})

async function onStoreChange() {
  if (!storeId.value) return
  localStorage.setItem('pos_store', String(storeId.value))
  clearCart()
  await Promise.all([loadProducts(), loadCategories(), loadShift()])
  loadHolds()
}

async function loadCategories() {
  try {
    categories.value = await api.get('/tenant/pos/categories')
  } catch {
    categories.value = []
  }
}

async function loadProducts() {
  if (!storeId.value) return
  productsLoading.value = true
  try {
    products.value = await api.get('/tenant/pos/products', { storeId: storeId.value })
  } finally {
    productsLoading.value = false
  }
}

async function loadShift() {
  try {
    shift.value = await api.get('/tenant/shifts/current', { storeId: storeId.value })
  } catch {
    shift.value = null
  }
}

function scanOrSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  const matched = products.value.find((p) => p.barcode === kw)
  if (matched) {
    addToCart(matched, kw)
    keyword.value = ''
    return
  }
  const list = filteredProducts.value
  if (list.length === 1) {
    addToCart(list[0])
    keyword.value = ''
  }
}

function addToCart(product: any, _barcode?: string) {
  if (product.stock !== undefined && product.stock <= 0) {
    ElMessage.warning('该商品库存不足')
    return
  }
  const price = member.value ? Number(product.memberPrice || product.price) : Number(product.price)
  const exists = cart.value.find((item) => item.skuId === product.skuId)
  if (exists) {
    if (exists.qty + 1 > product.stock) {
      ElMessage.warning('超出可售库存')
      return
    }
    exists.qty += 1
  } else {
    cart.value.push({
      skuId: product.skuId, name: product.name, spec: product.spec,
      skuCode: product.skuCode, price, qty: 1, stock: product.stock
    })
  }
  refreshPreview()
  barcodeInput.value?.focus?.()
}

function changeQty(item: any, delta: number) {
  const next = item.qty + delta
  if (next <= 0) {
    removeItem(item)
    return
  }
  if (item.stock !== undefined && next > item.stock) {
    ElMessage.warning('超出可售库存')
    return
  }
  item.qty = next
  refreshPreview()
}

function removeItem(item: any) {
  cart.value = cart.value.filter((row) => row.skuId !== item.skuId)
  refreshPreview()
}

function clearCart() {
  cart.value = []
  preview.value = null
  memberCouponId.value = null
  pointsUsed.value = 0
  useWallet.value = false
  remark.value = ''
}

function clearMember() {
  member.value = null
  memberCouponId.value = null
  coupons.value = []
  pointsUsed.value = 0
  useWallet.value = false
  cart.value.forEach((item) => {
    const product = products.value.find((p) => p.skuId === item.skuId)
    if (product) item.price = Number(product.price)
  })
  refreshPreview()
}

function refreshPreview() {
  if (previewTimer.value) clearTimeout(previewTimer.value)
  if (!cart.value.length) {
    preview.value = null
    return
  }
  previewTimer.value = setTimeout(async () => {
    try {
      preview.value = await api.post('/tenant/pos/preview', buildPayload())
      if (member.value && useWallet.value) {
        // 储值抵扣在结算时计算，这里不改变应收
      }
    } catch {
      preview.value = null
    }
  }, 180)
}

function buildPayload() {
  return {
    storeId: storeId.value,
    memberId: member.value?.id || null,
    memberCouponId: memberCouponId.value,
    pointsUsed: pointsUsed.value,
    useWallet: useWallet.value,
    paymentMethod: paymentMethod.value,
    remark: remark.value,
    items: cart.value.map((item) => ({ skuId: item.skuId, qty: item.qty }))
  }
}

async function searchMembers() {
  if (!memberKeyword.value.trim()) return
  memberResults.value = await api.get('/tenant/pos/members', { keyword: memberKeyword.value.trim() })
}

async function selectMember(m: any) {
  member.value = m
  memberDialog.value = false
  memberKeyword.value = ''
  memberResults.value = []
  coupons.value = await api.get('/tenant/pos/member-coupons', { memberId: m.id })
  cart.value.forEach((item) => {
    const product = products.value.find((p) => p.skuId === item.skuId)
    if (product) item.price = Number(product.memberPrice || product.price)
  })
  refreshPreview()
}

function openShift() {
  shiftDialog.value = true
  openingCash.value = 500
}

async function submitShift() {
  if (!shift.value) {
    shift.value = await api.post('/tenant/shifts/open', null, {
      params: { storeId: storeId.value, openingCash: openingCash.value }
    })
    ElMessage.success('已开班')
  } else {
    shift.value = await api.post(`/tenant/shifts/${shift.value.id}/close`, {
      actualCash: actualCash.value, diffReason: diffReason.value
    })
    ElMessage.success('已交班')
    shift.value = null
  }
  shiftDialog.value = false
}

async function checkout() {
  if (!storeId.value) return
  if (!shift.value) {
    ElMessage.warning('请先开班再收银')
    shiftDialog.value = true
    return
  }
  if (useWallet.value && member.value) {
    const payable = Number(preview.value?.payableAmount ?? localGross.value)
    if (Number(member.value.walletBalance) >= payable) {
      await doCheckout('WALLET')
      return
    }
  }
  if (paymentMethod.value === 'CASH') {
    cashReceived.value = Number(preview.value?.payableAmount ?? localGross.value)
    cashDialog.value = true
    return
  }
  await doCheckout(paymentMethod.value)
}

async function confirmCheckout() {
  const payable = Number(preview.value?.payableAmount ?? localGross.value)
  if (cashReceived.value < payable) {
    ElMessage.warning('实收现金不足')
    return
  }
  await doCheckout('CASH')
}

async function doCheckout(method: string) {
  checkingOut.value = true
  try {
    const order: any = await api.post('/tenant/pos/checkout', { ...buildPayload(), paymentMethod: method })
    lastOrder.value = order
    cartSnapshot.value = cart.value.map((item) => ({ ...item }))
    nowText.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
    cashDialog.value = false
    successDialog.value = true
    clearCart()
    await loadProducts()
  } finally {
    checkingOut.value = false
  }
}

function closeSuccess() {
  successDialog.value = false
  lastOrder.value = null
  cashReceived.value = 0
  cashDialog.value = false
}

function printReceipt() {
  window.print()
}

function loadHolds() {
  try {
    holds.value = JSON.parse(localStorage.getItem(holdKey()) || '[]')
  } catch {
    holds.value = []
  }
}

function holdKey() {
  return `pos_holds_${storeId.value || 0}`
}

function saveHolds() {
  localStorage.setItem(holdKey(), JSON.stringify(holds.value))
}

function saveHold() {
  if (!cart.value.length) return
  holds.value.unshift({
    id: Date.now(),
    label: member.value ? `${member.value.nickname} 的挂单` : '散客挂单',
    time: dayjs().format('MM-DD HH:mm'),
    items: cart.value.map((item) => ({ ...item })),
    member: member.value,
    memberCouponId: memberCouponId.value,
    pointsUsed: pointsUsed.value
  })
  saveHolds()
  clearCart()
  ElMessage.success('已挂单')
}

function resumeHold(index: number) {
  const hold = holds.value[index]
  cart.value = hold.items.map((item: any) => ({ ...item }))
  member.value = hold.member || null
  memberCouponId.value = hold.memberCouponId || null
  pointsUsed.value = hold.pointsUsed || 0
  if (member.value) {
    api.get('/tenant/pos/member-coupons', { memberId: member.value.id }).then((list) => { coupons.value = list || [] })
  }
  holds.value.splice(index, 1)
  saveHolds()
  holdDialog.value = false
  refreshPreview()
}

function deleteHold(index: number) {
  holds.value.splice(index, 1)
  saveHolds()
}

function money(value: any) {
  return Number(value || 0).toFixed(2)
}

window.addEventListener('beforeunload', () => {
  if (cart.value.length) {
    // 刷新前自动挂单，避免收银数据丢失
    const existing = JSON.parse(localStorage.getItem(holdKey()) || '[]')
    existing.unshift({
      id: Date.now(), label: '自动挂单', time: dayjs().format('MM-DD HH:mm'),
      items: cart.value.map((item) => ({ ...item })), member: member.value,
      memberCouponId: memberCouponId.value, pointsUsed: pointsUsed.value
    })
    localStorage.setItem(holdKey(), JSON.stringify(existing))
  }
})
</script>

<style scoped lang="scss">
.pos-page { display: flex; flex-direction: column; height: calc(100vh - 102px); background: #f5f7fb; }
.pos-top {
  display: flex; justify-content: space-between; align-items: center; padding: 14px 20px;
  background: #fff; border-bottom: 1px solid var(--line); flex-shrink: 0;
}
.pos-title { display: flex; align-items: center; gap: 10px; color: #1f6feb;
  div { display: flex; flex-direction: column; gap: 2px; }
  strong { font-size: 17px; color: var(--ink); }
  span { font-size: 11.5px; color: var(--ink-3); }
}
.pos-actions { display: flex; gap: 8px; align-items: center; }
.pos-body { flex: 1; display: grid; grid-template-columns: 1fr 390px; gap: 14px; padding: 14px 20px; overflow: hidden; }
.product-area { display: flex; flex-direction: column; overflow: hidden; }
.search-row { display: flex; gap: 8px; margin-bottom: 12px; }
.category-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px;
  button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 14px; cursor: pointer; font-size: 12px; color: #475467; }
  button.active { background: #1f6feb; border-color: #1f6feb; color: #fff; font-weight: 600; }
}
.product-grid { flex: 1; overflow-y: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(158px, 1fr)); gap: 12px; align-content: start; padding-right: 4px; }
.product-card {
  border: 1px solid var(--line); background: #fff; border-radius: 10px; padding: 0; overflow: hidden;
  cursor: pointer; text-align: left; transition: all 0.15s;
  &:hover { border-color: #b9d3fa; box-shadow: 0 8px 20px rgba(31,111,235,0.1); transform: translateY(-2px); }
}
.product-img { height: 110px; background: #f2f6fd; position: relative; overflow: hidden;
  img { width: 100%; height: 100%; object-fit: cover; }
}
.product-placeholder { height: 100%; display: grid; place-items: center; font-size: 24px; color: #7ba4e8; font-weight: 600; }
.stock-warning { position: absolute; right: 6px; top: 6px; background: #fdecef; color: #c0264a; font-size: 10px; padding: 2px 6px; border-radius: 999px; }
.product-info { padding: 10px; display: flex; flex-direction: column; gap: 4px;
  strong { font-size: 12.5px; line-height: 1.4; height: 35px; overflow: hidden; }
  span { font-size: 10.5px; color: var(--ink-3); }
}
.product-price { display: flex; justify-content: space-between; align-items: baseline; margin-top: 2px;
  em { font-style: normal; color: #d03050; font-weight: 650; font-size: 14px; }
  small { font-size: 10px; color: var(--ink-3); }
}
.product-empty { grid-column: 1 / -1; }
.cart-area { background: #fff; border: 1px solid var(--line); border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.cart-head { padding: 14px; border-bottom: 1px solid var(--line); }
.cart-member { display: flex; align-items: center; gap: 10px;
  div:nth-child(2) { flex: 1; display: flex; flex-direction: column; gap: 3px; min-width: 0; }
  strong { font-size: 13px; } span { font-size: 11px; color: var(--ink-3); }
}
.member-avatar { width: 38px; height: 38px; border-radius: 50%; background: linear-gradient(135deg, #1f6feb, #14b8a6); color: #fff; display: grid; place-items: center; font-weight: 600; flex-shrink: 0;
  &.guest { background: #eef2f7; color: #98a2b3; }
}
.member-results { margin-top: 14px; display: flex; flex-direction: column; gap: 6px; max-height: 360px; overflow-y: auto; }
.member-result { border: 1px solid var(--line); background: #fff; border-radius: 9px; padding: 10px; display: flex; align-items: center; gap: 10px; cursor: pointer; text-align: left;
  div:nth-child(2) { flex: 1; display: flex; flex-direction: column; gap: 3px; }
  strong { font-size: 13px; } span { font-size: 11px; color: var(--ink-3); }
  &:hover { border-color: #b9d3fa; background: #f5f9ff; }
}
.cart-items { flex: 1; overflow-y: auto; padding: 8px 14px; }
.cart-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px dashed #eef1f5; }
.cart-item-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 12.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  span { font-size: 10.5px; color: var(--ink-3); }
}
.qty-control { display: flex; align-items: center; border: 1px solid var(--line); border-radius: 6px; overflow: hidden;
  button { width: 24px; height: 24px; border: none; background: #f8fafc; cursor: pointer; color: #475467; }
  span { width: 28px; text-align: center; font-size: 12px; font-weight: 600; }
}
.cart-amount { font-size: 12.5px; font-weight: 600; font-variant-numeric: tabular-nums; width: 62px; text-align: right; }
.cart-summary { border-top: 1px solid var(--line); padding: 14px; background: #fbfcfe; }
.summary-row { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #475467; padding: 5px 0;
  &.discount { color: #d03050; }
}
.summary-total { display: flex; justify-content: space-between; align-items: baseline; border-top: 1px dashed #e4e7ec; margin-top: 6px; padding-top: 10px;
  span { font-size: 13px; } strong { font-size: 26px; color: #d03050; font-weight: 700; letter-spacing: -0.02em; }
}
.pay-methods { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; margin-top: 10px;
  button { border: 1px solid var(--line); background: #fff; border-radius: 7px; padding: 8px 4px; display: flex; align-items: center; justify-content: center; gap: 4px; font-size: 12px; cursor: pointer; color: #475467;
    &.active { border-color: #1f6feb; background: #eaf2ff; color: #1f6feb; font-weight: 600; }
  }
}
.wallet-row { margin-top: 6px; }
.checkout-btn { width: 100%; height: 46px; font-size: 16px; font-weight: 650; border-radius: 9px; background: linear-gradient(90deg, #1f6feb, #2563eb); border: none;
  &:disabled { background: #c7d7f0; }
}
.clear-btn { width: 100%; margin-top: 4px; color: #98a2b3; }
.cash-box { display: flex; justify-content: space-between; align-items: baseline; background: #f8fafc; border-radius: 9px; padding: 16px; margin-bottom: 14px;
  span { color: var(--ink-3); font-size: 12.5px; } strong { font-size: 30px; color: #d03050; }
}
.change { font-size: 22px; font-weight: 650; color: #18a058; }
.hold-list { display: flex; flex-direction: column; gap: 8px; max-height: 420px; overflow-y: auto; }
.hold-item { display: flex; justify-content: space-between; align-items: center; border: 1px solid var(--line); border-radius: 9px; padding: 10px 12px;
  div:first-child { display: flex; flex-direction: column; gap: 3px; }
  strong { font-size: 12.5px; } span { font-size: 11px; color: var(--ink-3); }
}
.success-box { text-align: center; padding: 6px 0 14px;
  h3 { margin: 10px 0 4px; font-size: 30px; color: #18a058; }
  p { margin: 3px 0; font-size: 12px; color: var(--ink-3); }
}
.success-icon { width: 62px; height: 62px; border-radius: 50%; background: #e8f7ef; color: #18a058; display: grid; place-items: center; margin: 0 auto; }
.receipt { display: none; font-family: 'Courier New', monospace; color: #111; width: 280px; padding: 10px; }
.receipt h4 { text-align: center; margin: 0 0 8px; }
.receipt p { margin: 3px 0; font-size: 12px; }
.receipt-line { display: flex; justify-content: space-between; font-size: 12px; margin: 3px 0; }
.receipt-line.total { border-top: 1px dashed #333; margin-top: 8px; padding-top: 6px; font-weight: 700; }
.receipt-thanks { text-align: center; margin-top: 12px !important; }

@media print {
  :global(body *) { visibility: hidden; }
  .receipt, .receipt * { visibility: visible; display: block; }
  .receipt { position: fixed; left: 0; top: 0; }
}
</style>
