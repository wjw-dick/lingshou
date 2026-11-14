<template>
  <div class="page">
    <PageHeader title="收货与差异" subtitle="到货验收、批次与冷链登记、数量差异记录">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Van" @click="openSupplyPick">从供货单创建</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.storeId" placeholder="全部门店" clearable filterable style="width: 180px" @change="search">
          <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option label="待收货" value="PENDING" />
          <el-option label="已确认" value="CONFIRMED" />
        </el-select>
        <div class="spacer" />
        <span class="muted">共 {{ total }} 张收货单</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无收货单">
        <el-table-column label="收货单号" min-width="150">
          <template #default="{ row }"><strong>{{ row.receiveNo }}</strong></template>
        </el-table-column>
        <el-table-column label="门店" min-width="140">
          <template #default="{ row }">{{ storeName(row.storeId) }}</template>
        </el-table-column>
        <el-table-column label="关联供货单" width="130">
          <template #default="{ row }">#{{ row.supplyId || '-' }}</template>
        </el-table-column>
        <el-table-column label="收货数量" width="100" align="center" prop="totalQty" />
        <el-table-column label="差异数量" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'diff-qty': Number(row.diffQty || 0) !== 0 }">{{ row.diffQty || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收货人" width="110">
          <template #default="{ row }">{{ row.receiver || '-' }}</template>
        </el-table-column>
        <el-table-column label="收货时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.receivedAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="openConfirm(row)">确认收货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="confirmVisible" title="确认收货" width="1080px" :close-on-click-modal="false">
      <div class="confirm-head">
        <span class="strong">{{ confirmOrder?.receiveNo }}</span>
        <span class="muted">{{ storeName(confirmOrder?.storeId) }} · 共 {{ confirmRows.length }} 项</span>
      </div>
      <el-form :model="confirmForm" label-width="90px" class="confirm-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="收货人">
              <el-input v-model="confirmForm.receiver" placeholder="请输入收货人" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="confirmForm.remark" placeholder="选填，可登记差异说明" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table :data="confirmRows" stripe max-height="440" empty-text="暂无待收货商品">
        <el-table-column label="商品" min-width="160">
          <template #default="{ row }">
            <div class="cell-main">
              <strong>{{ row.skuName }}</strong>
              <span>
                {{ row.item.skuCode || row.skuCode || '' }}
                <el-tag v-if="row.coldRequired" size="small" type="warning" effect="plain" style="margin-left: 6px">
                  {{ row.storageCondition === 'FROZEN' ? '冷冻' : '冷藏' }}
                </el-tag>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="应收" width="70" align="center">
          <template #default="{ row }">{{ row.expectedQty }}</template>
        </el-table-column>
        <el-table-column label="实收数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.actualQty" :min="0" :max="999999" size="small" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column label="生产日期" width="150">
          <template #default="{ row }">
            <el-date-picker v-model="row.productionDate" type="date" value-format="YYYY-MM-DD" size="small" placeholder="生产日期" style="width: 130px" />
          </template>
        </el-table-column>
        <el-table-column label="到期日" width="150">
          <template #default="{ row }">
            <el-date-picker v-model="row.expiryDate" type="date" value-format="YYYY-MM-DD" size="small" placeholder="到期日" style="width: 130px" />
          </template>
        </el-table-column>
        <el-table-column label="批次号" width="150">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" size="small" placeholder="批次号" maxlength="40" />
          </template>
        </el-table-column>
        <el-table-column label="冷链温度" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.coldChainTemp"
              :precision="1"
              :step="0.5"
              :min="-40"
              :max="30"
              size="small"
              :placeholder="row.coldRequired ? '必填 ℃' : '选填'"
              style="width: 100px"
            />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirmSaving" @click="doConfirm">确认收货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pickVisible" title="从供货单创建收货单" width="880px" append-to-body>
      <div class="toolbar-inline">
        <el-select v-model="pickStoreId" placeholder="全部门店" clearable filterable style="width: 200px" @change="loadPickSupplies">
          <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" @click="loadPickSupplies">刷新</el-button>
        <span class="muted">仅显示可收货的供货单</span>
      </div>
      <el-table v-loading="pickLoading" :data="pickRows" stripe height="380" empty-text="暂无可收货供货单">
        <el-table-column label="供货单号" min-width="150">
          <template #default="{ row }"><strong>{{ row.order.orderNo }}</strong></template>
        </el-table-column>
        <el-table-column label="门店" min-width="130" prop="storeName" />
        <el-table-column label="供应商" min-width="130" prop="supplierName" />
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.order.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.order.status" /></template>
        </el-table-column>
        <el-table-column width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="createFromSupply(row)">创建</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="760px" :title="detail?.order?.receiveNo || '收货单详情'">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="收货单号">{{ detail.order.receiveNo }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.order.status" /></el-descriptions-item>
            <el-descriptions-item label="门店">{{ storeName(detail.order.storeId) }}</el-descriptions-item>
            <el-descriptions-item label="关联供货单">#{{ detail.order.supplyId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货数量">{{ detail.order.totalQty || 0 }}</el-descriptions-item>
            <el-descriptions-item label="差异数量">
              <span :class="{ 'diff-qty': Number(detail.order.diffQty || 0) !== 0 }">{{ detail.order.diffQty || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="收货人">{{ detail.order.receiver || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货时间">{{ fmtTime(detail.order.receivedAt) }}</el-descriptions-item>
            <el-descriptions-item label="冷链温度">
              {{ detail.order.coldChainTemp === null || detail.order.coldChainTemp === undefined ? '-' : detail.order.coldChainTemp + ' ℃' }}
            </el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="section-title" style="margin-top: 20px">收货明细</div>
          <el-table :data="detail.items" stripe empty-text="暂无明细">
            <el-table-column label="商品" min-width="170">
              <template #default="{ row }">
                <div class="cell-main">
                  <strong>{{ row.skuName }}</strong>
                  <span>{{ row.skuCode }}{{ row.barcode ? ' · ' + row.barcode : '' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="应收" width="70" align="center">
              <template #default="{ row }">{{ row.item.qty }}</template>
            </el-table-column>
            <el-table-column label="实收" width="70" align="center">
              <template #default="{ row }">{{ row.item.actualQty }}</template>
            </el-table-column>
            <el-table-column label="差异" width="70" align="center">
              <template #default="{ row }">
                <span :class="{ 'diff-qty': Number(row.item.diffQty || 0) !== 0 }">{{ row.item.diffQty }}</span>
              </template>
            </el-table-column>
            <el-table-column label="生产日期" width="110">
              <template #default="{ row }">{{ fmtDate(row.item.productionDate) }}</template>
            </el-table-column>
            <el-table-column label="到期日" width="110">
              <template #default="{ row }">{{ fmtDate(row.item.expiryDate) }}</template>
            </el-table-column>
            <el-table-column label="批次号" width="130">
              <template #default="{ row }">{{ row.item.batchNo || '-' }}</template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else description="暂无数据" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Van } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

const auth = useAuthStore()

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const stores = ref<any[]>([])
const query = reactive<any>({ page: 1, size: 10, storeId: undefined, status: '' })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const confirmVisible = ref(false)
const confirmSaving = ref(false)
const confirmOrder = ref<any>(null)
const confirmRows = ref<any[]>([])
const confirmForm = reactive<any>({ receiver: '', remark: '' })

const pickVisible = ref(false)
const pickLoading = ref(false)
const pickRows = ref<any[]>([])
const pickStoreId = ref<number>()

onMounted(() => {
  loadStores()
  load()
})

async function loadStores() {
  try {
    stores.value = await api.get('/tenant/stores/options')
  } catch {
    stores.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const result = await api.get('/tenant/receive-orders', {
      page: query.page,
      size: query.size,
      storeId: query.storeId,
      status: query.status || undefined
    })
    rows.value = result?.records || []
    total.value = result?.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function storeName(id?: number) {
  return stores.value.find((item) => item.id === id)?.name || '-'
}

async function openDetail(row: any) {
  detailVisible.value = true
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await api.get(`/tenant/receive-orders/${row.id}`)
  } finally {
    detailLoading.value = false
  }
}

async function openConfirm(row: any) {
  confirmOrder.value = row
  confirmForm.receiver = auth.displayName || ''
  confirmForm.remark = ''
  confirmVisible.value = true
  confirmRows.value = []
  const data = await api.get(`/tenant/receive-orders/${row.id}`)
  const items = (data.items || []).map((line: any) => ({
    ...line,
    receiveItemId: line.item.id,
    supplyItemId: line.item.supplyItemId,
    skuId: line.item.skuId,
    expectedQty: line.item.qty,
    actualQty: line.item.actualQty || line.item.qty,
    productionDate: line.item.productionDate || '',
    expiryDate: line.item.expiryDate || '',
    batchNo: line.item.batchNo || '',
    coldChainTemp: line.item.coldChainTemp ?? undefined,
    storageCondition: '',
    coldRequired: false
  }))
  confirmRows.value = items
  await Promise.all(
    items.map(async (line: any) => {
      if (!line.barcode) return
      try {
        const product = await api.get(`/tenant/products/skus/barcode/${encodeURIComponent(line.barcode)}`)
        line.storageCondition = product?.spu?.storageCondition || 'NORMAL'
        line.coldRequired = ['COLD', 'FROZEN'].includes(line.storageCondition)
      } catch {
        line.storageCondition = 'NORMAL'
      }
    })
  )
}

async function doConfirm() {
  if (!confirmRows.value.length) {
    ElMessage.warning('收货单无明细')
    return
  }
  for (const line of confirmRows.value) {
    if (line.actualQty === null || line.actualQty === undefined) {
      ElMessage.warning(`请填写 ${line.skuName} 的实收数量`)
      return
    }
    if (Number(line.actualQty) > 0) {
      if (!line.productionDate) {
        ElMessage.warning(`请填写 ${line.skuName} 的生产日期`)
        return
      }
      if (!line.expiryDate) {
        ElMessage.warning(`请填写 ${line.skuName} 的到期日`)
        return
      }
      if (!line.batchNo) {
        ElMessage.warning(`请填写 ${line.skuName} 的批次号`)
        return
      }
    }
    if (line.coldRequired && (line.coldChainTemp === null || line.coldChainTemp === undefined || line.coldChainTemp === '')) {
      ElMessage.warning(`冷藏/冷冻商品 ${line.skuName} 必须填写冷链温度`)
      return
    }
  }
  const firstCold = confirmRows.value.find((line: any) => line.coldRequired)
  confirmSaving.value = true
  try {
    await api.post(`/tenant/receive-orders/${confirmOrder.value.id}/confirm`, {
      coldChainTemp: firstCold?.coldChainTemp ?? null,
      receiver: confirmForm.receiver,
      remark: confirmForm.remark,
      items: confirmRows.value.map((line: any) => ({
        receiveItemId: line.receiveItemId,
        supplyItemId: line.supplyItemId,
        skuId: line.skuId,
        actualQty: Number(line.actualQty),
        productionDate: line.productionDate || null,
        expiryDate: line.expiryDate || null,
        coldChainTemp: line.coldChainTemp ?? null,
        batchNo: line.batchNo
      }))
    })
    ElMessage.success('收货已确认')
    confirmVisible.value = false
    load()
  } finally {
    confirmSaving.value = false
  }
}

function openSupplyPick() {
  pickVisible.value = true
  pickStoreId.value = undefined
  loadPickSupplies()
}

async function loadPickSupplies() {
  pickLoading.value = true
  try {
    const result = await api.get('/tenant/supply-orders', {
      page: 1,
      size: 100,
      storeId: pickStoreId.value
    })
    pickRows.value = (result?.records || []).filter((row: any) => !['RECEIVED', 'CANCELLED'].includes(row.order.status))
  } catch {
    pickRows.value = []
  } finally {
    pickLoading.value = false
  }
}

async function createFromSupply(row: any) {
  const data = await api.post(`/tenant/receive-orders?supplyId=${row.order.id}`)
  ElMessage.success(`收货单 ${data?.receiveNo || ''} 已创建`)
  pickVisible.value = false
  search()
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
.toolbar-inline {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.confirm-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.confirm-form {
  margin-bottom: 4px;
}

.cell-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong {
    font-size: 13px;
    font-weight: 550;
  }

  span {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.diff-qty {
  color: #c0264a;
  font-weight: 650;
}
</style>
