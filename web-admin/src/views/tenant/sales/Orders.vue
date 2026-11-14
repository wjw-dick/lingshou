<template>
  <div class="page">
    <PageHeader title="订单中心" subtitle="统一处理线上与收银订单：接单、拣货、完成、取消与售后退款。">
      <template #actions>
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" class="order-tabs" @tab-change="onTabChange">
      <el-tab-pane label="订单列表" name="orders">
        <div class="table-card">
          <div class="toolbar">
            <el-select v-model="filters.storeId" placeholder="全部门店" clearable style="width: 160px" @change="search">
              <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
            </el-select>
            <el-select v-model="filters.channel" placeholder="全部渠道" clearable style="width: 130px" @change="search">
              <el-option label="线上商城" value="ONLINE" />
              <el-option label="收银" value="POS" />
            </el-select>
            <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-input
              v-model="filters.keyword"
              placeholder="订单号"
              clearable
              style="width: 170px"
              :prefix-icon="Search"
              @keyup.enter="search"
              @clear="search"
            />
            <el-input-number
              v-model="filters.memberId"
              :min="1"
              :controls="false"
              placeholder="会员 ID"
              style="width: 110px"
            />
            <el-date-picker
              v-model="filters.dates"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 250px"
              @change="search"
            />
            <el-button type="primary" :icon="Search" @click="search">查询</el-button>
            <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
          </div>

          <el-table v-loading="loading" :data="rows" :row-key="rowKey" style="width: 100%">
            <el-table-column label="订单号 / 下单时间" min-width="190">
              <template #default="{ row }">
                <div class="order-no">
                  <strong>{{ row.order.orderNo }}</strong>
                  <span>{{ datetime(row.order.createdAt) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="渠道" width="96">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="row.order.channel === 'POS' ? 'info' : 'primary'">
                  {{ row.order.channel === 'POS' ? '收银' : '线上' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="storeName" label="门店" min-width="130" show-overflow-tooltip />
            <el-table-column prop="memberName" label="会员" width="110" show-overflow-tooltip />
            <el-table-column label="金额" width="130" align="right">
              <template #default="{ row }">
                <div class="amount-cell">
                  <strong>¥{{ money(row.order.paidAmount) }}</strong>
                  <span v-if="Number(row.order.totalAmount || 0) !== Number(row.order.paidAmount || 0)">
                    原价 ¥{{ money(row.order.totalAmount) }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="支付" width="130">
              <template #default="{ row }">
                <div class="pay-cell">
                  <span>{{ payMethodLabel(row.order.paymentMethod) }}</span>
                  <StatusTag :status="row.order.payStatus" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="订单状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.order.status" /></template>
            </el-table-column>
            <el-table-column label="操作" width="270" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button v-if="row.order.status === 'PENDING_ACCEPT'" link type="primary" @click="accept(row.order)">接单</el-button>
                  <el-button v-if="row.order.status === 'PICKING'" link type="primary" @click="pickComplete(row.order)">拣货完成</el-button>
                  <el-button v-if="canComplete(row.order)" link type="success" @click="complete(row.order)">完成</el-button>
                  <el-button v-if="canCancel(row.order)" link type="warning" @click="cancel(row.order)">取消</el-button>
                  <el-button v-if="canRefund(row.order)" link type="danger" @click="openRefund(row.order)">退款</el-button>
                  <el-button link @click="openDetail(row.order.id, row.storeName, row.memberName)">详情</el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无订单" :image-size="80" />
            </template>
          </el-table>

          <div class="table-footer">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="size"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="load"
              @current-change="load"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="退款记录" name="refunds">
        <div class="table-card">
          <div class="toolbar">
            <el-select v-model="refundStatus" placeholder="全部退款状态" clearable style="width: 160px" @change="searchRefunds">
              <el-option label="退款成功" value="REFUNDED" />
              <el-option label="退款中" value="PENDING" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="searchRefunds">查询</el-button>
            <div class="spacer"></div>
            <span class="muted">共 {{ refundTotal }} 条退款</span>
          </div>

          <el-table v-loading="refundLoading" :data="refundRows" row-key="id" style="width: 100%">
            <el-table-column label="退款单号" min-width="170">
              <template #default="{ row }">
                <div class="order-no">
                  <strong>{{ row.refundNo }}</strong>
                  <span>{{ datetime(row.createdAt) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="原订单" width="100">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row.orderId)">#{{ row.orderId }}</el-button>
              </template>
            </el-table-column>
            <el-table-column label="退款类型" width="110">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="row.type === 'FULL' ? 'danger' : 'warning'">
                  {{ row.type === 'FULL' ? '全额退款' : '部分退款' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="退款金额" width="130" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" label="退款原因" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无退款记录" :image-size="80" />
            </template>
          </el-table>

          <div class="table-footer">
            <el-pagination
              v-model:current-page="refundPage"
              v-model:page-size="refundSize"
              :total="refundTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadRefunds"
              @current-change="loadRefunds"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 订单详情 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="680px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <strong>{{ detail.order.orderNo }}</strong>
              <span>{{ datetime(detail.order.createdAt) }} · {{ detail.order.channel === 'POS' ? '收银' : '线上商城' }}</span>
            </div>
            <StatusTag :status="detail.order.status" />
          </div>

          <h4 class="section-title">订单信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="门店">{{ detailMeta.storeName }}</el-descriptions-item>
            <el-descriptions-item label="会员">{{ detailMeta.memberName }}</el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ payMethodLabel(detail.order.paymentMethod) }}</el-descriptions-item>
            <el-descriptions-item label="支付状态"><StatusTag :status="detail.order.payStatus" /></el-descriptions-item>
            <el-descriptions-item label="商品原价">¥{{ money(detail.order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="优惠金额">-¥{{ money(detail.order.discountAmount) }}</el-descriptions-item>
            <el-descriptions-item label="运费">¥{{ money(detail.order.freightAmount) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额"><span class="money">¥{{ money(detail.order.paidAmount) }}</span></el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ datetime(detail.order.paidAt) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ datetime(detail.order.completedAt) }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title" style="margin-top: 18px">配送信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="配送方式">{{ detail.order.deliveryType === 'DELIVERY' ? '配送到家' : '到店自提' }}</el-descriptions-item>
            <el-descriptions-item label="预计时间">{{ detail.order.expectedTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货人">{{ detail.order.deliveryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.order.deliveryPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">{{ detail.order.deliveryAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title" style="margin-top: 18px">商品明细</h4>
          <el-table :data="detail.items || []" size="small" border>
            <el-table-column prop="skuName" label="商品" min-width="170" show-overflow-tooltip />
            <el-table-column prop="barcode" label="条码" width="120" />
            <el-table-column label="单价" width="90" align="right">
              <template #default="{ row }">¥{{ money(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="qty" label="数量" width="70" align="center" />
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
            </el-table-column>
            <template #empty><el-empty description="无商品明细" :image-size="50" /></template>
          </el-table>

          <h4 class="section-title" style="margin-top: 18px">退款记录</h4>
          <el-table :data="detail.refunds || []" size="small" border>
            <el-table-column prop="refundNo" label="退款单号" min-width="150" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ row.type === 'FULL' ? '全额' : '部分' }}</template>
            </el-table-column>
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ money(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
            <el-table-column label="时间" width="140">
              <template #default="{ row }">{{ datetime(row.createdAt) }}</template>
            </el-table-column>
            <template #empty><el-empty description="无退款记录" :image-size="50" /></template>
          </el-table>

          <h4 class="section-title" style="margin-top: 18px">订单评价</h4>
          <div v-if="detail.evaluation" class="evaluation">
            <div class="evaluation-head">
              <el-rate :model-value="detail.evaluation.score || 0" disabled />
              <span>{{ datetime(detail.evaluation.createdAt) }}</span>
            </div>
            <p>{{ detail.evaluation.content || '用户未填写文字评价' }}</p>
          </div>
          <el-empty v-else description="暂无评价" :image-size="50" />
        </template>
      </div>
    </el-drawer>

    <!-- 退款 -->
    <el-dialog v-model="refundVisible" title="订单退款" width="460px">
      <div v-if="refundTarget" class="refund-body">
        <div class="refund-order">
          <strong>{{ refundTarget.orderNo }}</strong>
          <span>实付 ¥{{ money(refundTarget.paidAmount) }} · {{ payMethodLabel(refundTarget.paymentMethod) }}</span>
        </div>
        <el-form label-width="90px">
          <el-form-item label="退款金额">
            <el-input-number v-model="refundForm.amount" :min="0.01" :max="Number(refundTarget.paidAmount || 0)" :precision="2" :step="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="退款原因">
            <el-input v-model="refundForm.reason" type="textarea" :rows="3" placeholder="请填写退款原因" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
        <el-alert type="warning" :closable="false" show-icon title="退款后商品将回库；全额退款时订单支付状态将变为已退款。" />
      </div>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="danger" :loading="refundSubmitting" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const tab = ref('orders')
const stores = ref<any[]>([])

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive<{
  storeId: number | undefined
  channel: string
  status: string
  memberId: number | undefined
  keyword: string
  dates: any
}>({
  storeId: undefined,
  channel: '',
  status: '',
  memberId: undefined,
  keyword: '',
  dates: null
})
const statusOptions = [
  { value: 'PENDING_PAY', label: '待支付' },
  { value: 'PENDING_ACCEPT', label: '待接单' },
  { value: 'PICKING', label: '拣货中' },
  { value: 'WAIT_PICKUP', label: '待自提' },
  { value: 'DELIVERING', label: '配送中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REFUNDED', label: '已退款' }
]

const refundLoading = ref(false)
const refundRows = ref<any[]>([])
const refundTotal = ref(0)
const refundPage = ref(1)
const refundSize = ref(10)
const refundStatus = ref('')

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const detailMeta = reactive<{ storeName: string; memberName: string }>({ storeName: '-', memberName: '-' })

const refundVisible = ref(false)
const refundSubmitting = ref(false)
const refundTarget = ref<any>(null)
const refundForm = reactive({ amount: 0, reason: '' })
const storeMap = ref<Record<number, string>>({})

onMounted(() => {
  loadStores()
  load()
})

async function loadStores() {
  try {
    const data: any = await api.get('/tenant/stores/options')
    stores.value = Array.isArray(data) ? data : []
    const map: Record<number, string> = {}
    stores.value.forEach((store) => { map[store.id] = store.name })
    storeMap.value = map
  } catch {
    stores.value = []
    storeMap.value = {}
  }
}

function rowKey(row: any) {
  return row?.order?.id
}

function refreshAll() {
  if (tab.value === 'orders') {
    load()
  } else {
    loadRefunds()
  }
}

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/tenant/orders', {
      page: page.value,
      size: size.value,
      storeId: filters.storeId || undefined,
      channel: filters.channel || undefined,
      status: filters.status || undefined,
      memberId: filters.memberId || undefined,
      keyword: filters.keyword || undefined,
      start: filters.dates?.[0] ? `${filters.dates[0]}T00:00:00` : undefined,
      end: filters.dates?.[1] ? `${filters.dates[1]}T23:59:59` : undefined
    })
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.storeId = undefined
  filters.channel = ''
  filters.status = ''
  filters.memberId = undefined
  filters.keyword = ''
  filters.dates = null
  search()
}

function onTabChange(name: any) {
  if (name === 'refunds' && !refundRows.value.length) loadRefunds()
}

async function loadRefunds() {
  refundLoading.value = true
  try {
    const data: any = await api.get('/tenant/orders/refunds', {
      page: refundPage.value,
      size: refundSize.value,
      status: refundStatus.value || undefined
    })
    refundRows.value = data?.records || []
    refundTotal.value = Number(data?.total || 0)
  } catch {
    refundRows.value = []
    refundTotal.value = 0
  } finally {
    refundLoading.value = false
  }
}

function searchRefunds() {
  refundPage.value = 1
  loadRefunds()
}

function canComplete(order: any) {
  return ['WAIT_PICKUP', 'DELIVERING'].includes(order?.status)
}

function canCancel(order: any) {
  return !['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(order?.status)
}

function canRefund(order: any) {
  return ['PAID', 'PARTIAL_REFUND'].includes(order?.payStatus) && order?.status !== 'REFUNDED'
}

async function accept(order: any) {
  try {
    await ElMessageBox.confirm(`确认接单「${order.orderNo}」？接单后订单进入拣货流程。`, '接单确认', { type: 'info' })
  } catch {
    return
  }
  await api.post(`/tenant/orders/${order.id}/accept`)
  ElMessage.success('已接单')
  load()
}

async function pickComplete(order: any) {
  try {
    await ElMessageBox.confirm(`确认「${order.orderNo}」已完成拣货？`, '拣货完成', { type: 'info' })
  } catch {
    return
  }
  await api.post(`/tenant/orders/${order.id}/pick-complete`)
  ElMessage.success('拣货已完成')
  load()
}

async function complete(order: any) {
  try {
    await ElMessageBox.confirm(`确认完成「${order.orderNo}」？`, '完成订单', { type: 'info' })
  } catch {
    return
  }
  await api.post(`/tenant/orders/${order.id}/complete`)
  ElMessage.success('订单已完成')
  load()
}

async function cancel(order: any) {
  const result: any = await ElMessageBox.prompt('请输入取消原因', `取消订单 ${order.orderNo}`, {
    type: 'warning',
    confirmButtonText: '确认取消',
    cancelButtonText: '返回',
    inputPlaceholder: '如：商品缺货 / 用户申请取消',
    inputValidator: (value: string) => (value && value.trim() ? true : '请填写取消原因')
  }).catch(() => null)
  if (!result) return
  await api.post(`/tenant/orders/${order.id}/cancel`, null, { params: { reason: result.value } })
  ElMessage.success('订单已取消')
  load()
  if (refundRows.value.length) loadRefunds()
}

function openRefund(order: any) {
  refundTarget.value = order
  refundForm.amount = Number(order.paidAmount || 0)
  refundForm.reason = ''
  refundVisible.value = true
}

async function submitRefund() {
  const order = refundTarget.value
  if (!order) return
  if (!refundForm.amount || refundForm.amount <= 0) {
    ElMessage.warning('请输入退款金额')
    return
  }
  if (refundForm.amount > Number(order.paidAmount || 0)) {
    ElMessage.warning('退款金额不能超过实付金额')
    return
  }
  if (!refundForm.reason.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refundSubmitting.value = true
  try {
    await api.post(`/tenant/orders/${order.id}/refund`, null, {
      params: { reason: refundForm.reason.trim(), amount: refundForm.amount }
    })
    ElMessage.success('退款已完成')
    refundVisible.value = false
    load()
    loadRefunds()
  } finally {
    refundSubmitting.value = false
  }
}

async function openDetail(orderId: number, storeName?: string, memberName?: string) {
  detail.value = null
  detailMeta.storeName = storeName || storeMap.value[orderId] || '-'
  detailMeta.memberName = memberName || '-'
  detailVisible.value = true
  detailLoading.value = true
  try {
    const data: any = await api.get(`/tenant/orders/${orderId}`)
    detail.value = data
    if (!storeName) {
      const store = stores.value.find((s) => s.id === data?.order?.storeId)
      detailMeta.storeName = store?.name || '-'
    }
    if (!memberName) {
      detailMeta.memberName = data?.order?.memberId ? `会员 #${data.order.memberId}` : '散客'
    }
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

function payMethodLabel(method: string) {
  const map: Record<string, string> = {
    CASH: '现金',
    WECHAT: '微信支付',
    ALIPAY: '支付宝',
    WALLET: '储值余额',
    UNIONPAY: '银联'
  }
  return map[method || ''] || method || '-'
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function datetime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.order-tabs {
  :deep(.el-tabs__header) { margin-bottom: 14px; }
  :deep(.el-tabs__nav-wrap::after) { height: 1px; background: var(--line); }
}

.toolbar .muted { font-size: 12px; }

.order-no { display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 12.5px; font-weight: 600; letter-spacing: 0.01em; }
  span { font-size: 10.5px; color: var(--ink-3); }
}
.amount-cell { display: flex; flex-direction: column; align-items: flex-end; gap: 2px;
  strong { font-size: 13px; font-weight: 650; }
  span { font-size: 10.5px; color: var(--ink-3); }
}
.pay-cell { display: flex; flex-direction: column; align-items: flex-start; gap: 4px;
  span { font-size: 11.5px; color: var(--ink-2); }
}
.row-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 2px 6px; }

.detail-body { min-height: 300px; }
.detail-head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
  padding: 14px 16px; border-radius: 10px; background: #f8fafc; margin-bottom: 16px;
  div { display: flex; flex-direction: column; gap: 4px; }
  strong { font-size: 15px; font-weight: 650; }
  span { font-size: 11.5px; color: var(--ink-3); }
}
.evaluation {
  border: 1px solid var(--line); border-radius: 9px; padding: 12px 14px; background: #fbfcfe;
  .evaluation-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
  .evaluation-head span { font-size: 11px; color: var(--ink-3); }
  p { margin: 8px 0 0; font-size: 12.5px; color: var(--ink-2); line-height: 1.7; }
}

.refund-body { display: flex; flex-direction: column; gap: 14px; }
.refund-order {
  display: flex; flex-direction: column; gap: 3px; padding: 12px 14px; border-radius: 9px; background: #f8fafc;
  strong { font-size: 13px; }
  span { font-size: 11.5px; color: var(--ink-3); }
}

@media (max-width: 1200px) {
  .toolbar :deep(.el-date-editor) { width: 230px !important; }
}
</style>
