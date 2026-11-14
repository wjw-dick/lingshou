<template>
  <div class="page">
    <PageHeader title="库存查询" subtitle="门店库存、批次流水与库存预警一屏掌握">
      <template #actions>
        <el-select v-model="storeId" placeholder="请选择门店" filterable style="width: 190px" @change="onStoreChange">
          <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="低库存商品" :value="alerts.lowStock.length" unit="项" icon="Warning" hint="低于安全库存，点击查看" color="#d03050" class="clickable" @click="alertTab = 'low'" />
      <StatCard label="临期批次" :value="alerts.nearExpiry.length" unit="批" icon="Clock" hint="临近保质期，点击查看" color="#f0a020" class="clickable" @click="alertTab = 'near'" />
      <StatCard label="滞销商品" :value="alerts.slowMoving.length" unit="项" icon="TrendCharts" hint="30 天无销量，点击查看" color="#7c3aed" class="clickable" @click="alertTab = 'slow'" />
      <StatCard label="超储商品" :value="alerts.overstock.length" unit="项" icon="Box" hint="库存高于安全线，点击查看" color="#0d9488" class="clickable" @click="alertTab = 'over'" />
    </div>

    <div class="card card-pad alert-card">
      <el-tabs v-model="alertTab">
        <el-tab-pane :label="`低库存 (${alerts.lowStock.length})`" name="low">
          <el-table v-loading="alertLoading" :data="alerts.lowStock" stripe max-height="260" empty-text="暂无低库存商品">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="cell-main">
                  <strong>{{ row.skuName }}</strong>
                  <span>{{ row.barcode || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="当前库存" width="100" align="center">
              <template #default="{ row }">{{ row.stock?.qty ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="锁定数量" width="100" align="center">
              <template #default="{ row }">{{ row.stock?.lockedQty ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="安全库存" width="100" align="center">
              <template #default="{ row }"><span class="warn-text">{{ row.stock?.safetyStock ?? 0 }}</span></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`临期 (${alerts.nearExpiry.length})`" name="near">
          <el-table v-loading="alertLoading" :data="alerts.nearExpiry" stripe max-height="260" empty-text="暂无临期批次">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="cell-main">
                  <strong>{{ row.skuName }}</strong>
                  <span>{{ row.barcode || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="批次号" width="140">
              <template #default="{ row }">{{ row.batch?.batchNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="生产日期" width="110">
              <template #default="{ row }">{{ fmtDate(row.batch?.productionDate) }}</template>
            </el-table-column>
            <el-table-column label="到期日" width="110">
              <template #default="{ row }">{{ fmtDate(row.batch?.expiryDate) }}</template>
            </el-table-column>
            <el-table-column label="剩余天数" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="danger" effect="plain" size="small">{{ row.daysLeft }} 天</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="80" align="center">
              <template #default="{ row }">{{ row.batch?.qty ?? 0 }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`滞销 (${alerts.slowMoving.length})`" name="slow">
          <el-table v-loading="alertLoading" :data="alerts.slowMoving" stripe max-height="260" empty-text="暂无滞销商品">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="cell-main">
                  <strong>{{ row.skuName }}</strong>
                  <span>{{ row.barcode || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="当前库存" width="100" align="center">
              <template #default="{ row }">{{ row.stock?.qty ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="安全库存" width="100" align="center">
              <template #default="{ row }">{{ row.stock?.safetyStock ?? 0 }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`超储 (${alerts.overstock.length})`" name="over">
          <el-table v-loading="alertLoading" :data="alerts.overstock" stripe max-height="260" empty-text="暂无超储商品">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="cell-main">
                  <strong>{{ row.skuName }}</strong>
                  <span>{{ row.barcode || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="当前库存" width="100" align="center">
              <template #default="{ row }">{{ row.stock?.qty ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="安全库存" width="100" align="center">
              <template #default="{ row }">{{ row.stock?.safetyStock ?? 0 }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="table-card" style="margin-top: 16px">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索商品名称 / 条码" clearable style="width: 260px" @keyup.enter="loadStocks" @clear="loadStocks">
          <template #append><el-button :icon="Search" @click="loadStocks" /></template>
        </el-input>
        <div class="spacer" />
        <span class="muted">当前门店共 {{ stocks.length }} 个商品库存</span>
      </div>

      <el-table v-loading="stockLoading" :data="pagedStocks" stripe empty-text="暂无库存数据" @row-click="openBatches">
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="cell-main">
              <strong>{{ row.skuName }}</strong>
              <span>{{ row.skuCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="条码" width="150" prop="barcode" />
        <el-table-column label="规格" width="120" prop="spec" />
        <el-table-column label="贮存条件" width="110">
          <template #default="{ row }">
            <el-tag :type="row.storageCondition === 'NORMAL' ? 'info' : 'warning'" effect="plain" size="small">
              {{ storageText(row.storageCondition) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="库存数量" width="110" align="center">
          <template #default="{ row }">
            <span :class="['stock-qty', { low: Number(row.stock?.qty || 0) <= Number(row.stock?.safetyStock || 0) }]">
              {{ row.stock?.qty ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="锁定" width="90" align="center">
          <template #default="{ row }">{{ row.stock?.lockedQty ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="安全库存" width="100" align="center">
          <template #default="{ row }">{{ row.stock?.safetyStock ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="零售价" width="110" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.price) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openBatches(row)">批次 / 流水</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          background
          layout="total, sizes, prev, pager, next"
          :total="stocks.length"
          :page-sizes="[10, 20, 50]"
        />
      </div>
    </div>

    <el-drawer v-model="drawerVisible" size="720px" :title="currentRow?.skuName || '库存详情'">
      <div class="drawer-summary">
        <span>{{ currentRow?.skuCode }}</span>
        <span class="muted">{{ currentRow?.barcode || '-' }}</span>
        <span class="muted">{{ storageText(currentRow?.storageCondition) }}</span>
      </div>
      <el-tabs v-model="drawerTab">
        <el-tab-pane label="批次库存" name="batch">
          <el-table v-loading="batchLoading" :data="batches" stripe empty-text="暂无批次">
            <el-table-column label="批次号" min-width="140" prop="batchNo" />
            <el-table-column label="生产日期" width="120">
              <template #default="{ row }">{{ fmtDate(row.productionDate) }}</template>
            </el-table-column>
            <el-table-column label="到期日" width="120">
              <template #default="{ row }">{{ fmtDate(row.expiryDate) }}</template>
            </el-table-column>
            <el-table-column label="数量" width="90" align="center" prop="qty" />
            <el-table-column label="剩余天数" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.expiryDate" :type="daysLeft(row.expiryDate) <= 3 ? 'danger' : 'info'" effect="plain" size="small">
                  {{ daysLeft(row.expiryDate) }} 天
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="库存流水" name="flow">
          <el-table v-loading="flowLoading" :data="flows" stripe empty-text="暂无流水">
            <el-table-column label="业务类型" width="120">
              <template #default="{ row }">{{ bizText(row.bizType) }}</template>
            </el-table-column>
            <el-table-column label="变动" width="90" align="center">
              <template #default="{ row }">
                <span :class="['flow-qty', Number(row.changeQty) >= 0 ? 'up' : 'down']">
                  {{ Number(row.changeQty) >= 0 ? '+' : '' }}{{ row.changeQty }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="变动前" width="90" align="center" prop="beforeQty" />
            <el-table-column label="变动后" width="90" align="center" prop="afterQty" />
            <el-table-column label="备注" min-width="130" prop="remark" />
            <el-table-column label="操作人" width="100" prop="createdBy" />
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const stores = ref<any[]>([])
const storeId = ref<number>()
const keyword = ref('')
const stocks = ref<any[]>([])
const stockLoading = ref(false)
const page = ref(1)
const size = ref(10)

const alertTab = ref('low')
const alertLoading = ref(false)
const alerts = reactive<any>({ lowStock: [], nearExpiry: [], slowMoving: [], overstock: [] })

const drawerVisible = ref(false)
const drawerTab = ref('batch')
const currentRow = ref<any>(null)
const batches = ref<any[]>([])
const flows = ref<any[]>([])
const batchLoading = ref(false)
const flowLoading = ref(false)

const pagedStocks = computed(() => {
  const start = (page.value - 1) * size.value
  return stocks.value.slice(start, start + size.value)
})

onMounted(async () => {
  await loadStores()
  if (storeId.value) {
    loadAll()
  }
})

async function loadStores() {
  try {
    stores.value = await api.get('/tenant/stores/options')
  } catch {
    stores.value = []
  }
  if (!storeId.value && stores.value.length) {
    storeId.value = stores.value[0].id
  }
}

function onStoreChange() {
  page.value = 1
  loadAll()
}

function loadAll() {
  loadStocks()
  loadAlerts()
}

async function loadStocks() {
  if (!storeId.value) return
  stockLoading.value = true
  try {
    const data = await api.get('/tenant/inventory/stocks', {
      storeId: storeId.value,
      ownerType: 'STORE',
      ownerId: storeId.value,
      keyword: keyword.value || undefined
    })
    stocks.value = data || []
    page.value = 1
  } catch {
    stocks.value = []
  } finally {
    stockLoading.value = false
  }
}

async function loadAlerts() {
  if (!storeId.value) return
  alertLoading.value = true
  try {
    const data = await api.get('/tenant/inventory/alerts', { storeId: storeId.value })
    alerts.lowStock = data?.lowStock || []
    alerts.nearExpiry = data?.nearExpiry || []
    alerts.slowMoving = data?.slowMoving || []
    alerts.overstock = data?.overstock || []
  } catch {
    alerts.lowStock = []
    alerts.nearExpiry = []
    alerts.slowMoving = []
    alerts.overstock = []
  } finally {
    alertLoading.value = false
  }
}

async function openBatches(row: any) {
  currentRow.value = row
  drawerVisible.value = true
  drawerTab.value = 'batch'
  batches.value = []
  flows.value = []
  batchLoading.value = true
  flowLoading.value = true
  try {
    const [batchData, flowData] = await Promise.all([
      api.get(`/tenant/inventory/stocks/${row.stock.skuId}/batches`, {
        storeId: storeId.value,
        ownerType: 'STORE',
        ownerId: storeId.value
      }),
      api.get('/tenant/inventory/flows', {
        storeId: storeId.value,
        skuId: row.stock.skuId,
        ownerType: 'STORE',
        ownerId: storeId.value,
        limit: 50
      })
    ])
    batches.value = batchData || []
    flows.value = flowData || []
  } catch {
    batches.value = []
    flows.value = []
  } finally {
    batchLoading.value = false
    flowLoading.value = false
  }
}

function storageText(value?: string) {
  if (value === 'COLD') return '冷藏'
  if (value === 'FROZEN') return '冷冻'
  return '常温'
}

function bizText(value?: string) {
  const map: Record<string, string> = {
    SALE: '销售出库',
    RECEIVE: '收货入库',
    LOSS: '报损出库',
    TRANSFER: '调拨',
    TRANSFER_IN: '调拨入库',
    TRANSFER_OUT: '调拨出库',
    CHECK: '盘点调整',
    ADJUST: '库存调整',
    RETURN: '退货入库'
  }
  return map[value || ''] || value || '-'
}

function daysLeft(expiryDate: string) {
  return dayjs(expiryDate).diff(dayjs().format('YYYY-MM-DD'), 'day')
}

function money(value: any) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtDate(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}

function fmtTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.clickable {
  cursor: pointer;
}

.alert-card {
  padding-bottom: 6px;
}

.cell-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong {
    font-size: 13px;
    font-weight: 550;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  span {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.warn-text {
  color: #c0264a;
  font-weight: 650;
}

.stock-qty {
  font-weight: 650;
  font-variant-numeric: tabular-nums;

  &.low {
    color: #c0264a;
  }
}

.flow-qty {
  font-weight: 650;
  font-variant-numeric: tabular-nums;

  &.up {
    color: #0e7a45;
  }

  &.down {
    color: #c0264a;
  }
}

.drawer-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 12.5px;
}
</style>
