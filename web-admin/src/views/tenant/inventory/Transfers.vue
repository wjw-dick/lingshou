<template>
  <div class="page">
    <PageHeader title="调拨单" subtitle="总部仓向门店调拨，审核后执行出库与入库">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建调拨单</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <div class="spacer" />
        <span class="muted">共 {{ total }} 张调拨单</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无调拨单">
        <el-table-column label="调拨单号" min-width="150">
          <template #default="{ row }"><strong>{{ row.transferNo }}</strong></template>
        </el-table-column>
        <el-table-column label="调出方" min-width="140">
          <template #default="{ row }">{{ row.fromType === 'WAREHOUSE' ? '总部仓' : storeName(row.fromId) }}</template>
        </el-table-column>
        <el-table-column label="调入方" min-width="140">
          <template #default="{ row }">{{ row.toType === 'STORE' ? storeName(row.toId) : row.toType }}</template>
        </el-table-column>
        <el-table-column label="调拨数量" width="100" align="center" prop="totalQty" />
        <el-table-column label="申请人" width="110">
          <template #default="{ row }">{{ row.appliedBy || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="doApprove(row)">审核通过</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click="doReject(row)">驳回</el-button>
            <el-button v-if="row.status === 'APPROVED'" link type="primary" size="small" @click="doComplete(row)">执行完成</el-button>
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

    <el-dialog v-model="formVisible" title="新建调拨单" width="860px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="调出方">
              <el-input model-value="总部仓" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调入门店" required>
              <el-select v-model="form.toId" placeholder="请选择门店" filterable style="width: 100%">
                <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="section-title">调拨明细</div>
      <div class="toolbar-inline">
        <el-select v-model="pickSkuId" placeholder="选择总仓商品" filterable clearable style="width: 300px">
          <el-option
            v-for="item in warehouseStocks"
            :key="item.stock.skuId"
            :label="`${item.skuName}（库存 ${item.stock.qty}）`"
            :value="item.stock.skuId"
          />
        </el-select>
        <el-button :icon="Plus" size="small" @click="addItem">添加</el-button>
        <span class="muted">合计 {{ formQty }} 件</span>
      </div>
      <el-table :data="form.items" stripe empty-text="请添加调拨商品" max-height="320">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="cell-main">
              <strong>{{ row.skuName }}</strong>
              <span>{{ row.skuCode }} · 总仓库存 {{ row.available }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="批次号" width="180">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" size="small" placeholder="选填" maxlength="40" />
          </template>
        </el-table-column>
        <el-table-column label="调拨数量" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="1" :max="row.available || 999999" size="small" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="form.items.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="680px" :title="detail?.order?.transferNo || '调拨单详情'">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="调拨单号">{{ detail.order.transferNo }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.order.status" /></el-descriptions-item>
            <el-descriptions-item label="调出方">{{ detail.order.fromType === 'WAREHOUSE' ? '总部仓' : storeName(detail.order.fromId) }}</el-descriptions-item>
            <el-descriptions-item label="调入方">{{ detail.order.toType === 'STORE' ? storeName(detail.order.toId) : detail.order.toType }}</el-descriptions-item>
            <el-descriptions-item label="调拨数量">{{ detail.order.totalQty }}</el-descriptions-item>
            <el-descriptions-item label="申请人">{{ detail.order.appliedBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ detail.order.approvedBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ fmtTime(detail.order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="section-title" style="margin-top: 20px">调拨明细</div>
          <el-table :data="detail.items" stripe empty-text="暂无明细">
            <el-table-column label="商品" min-width="180">
              <template #default="{ row }">{{ row.skuName || `SKU #${row.skuId}` }}</template>
            </el-table-column>
            <el-table-column label="数量" width="90" align="center" prop="qty" />
            <el-table-column label="批次号" width="160">
              <template #default="{ row }">{{ row.batchNo || '-' }}</template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else description="暂无数据" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已审核' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REJECTED', label: '已驳回' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const stores = ref<any[]>([])
const warehouseStocks = ref<any[]>([])
const pickSkuId = ref<number>()
const query = reactive<any>({ page: 1, size: 10, status: '' })

const formVisible = ref(false)
const form = reactive<any>({ toId: undefined, remark: '', items: [] })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const formQty = computed(() =>
  form.items.reduce((sum: number, item: any) => sum + Number(item.qty || 0), 0)
)

onMounted(() => {
  loadStores()
  loadWarehouseStocks()
  load()
})

async function loadStores() {
  try {
    stores.value = await api.get('/tenant/stores/options')
  } catch {
    stores.value = []
  }
}

async function loadWarehouseStocks() {
  try {
    warehouseStocks.value = await api.get('/tenant/inventory/stocks', {
      ownerType: 'WAREHOUSE',
      ownerId: 1
    })
  } catch {
    warehouseStocks.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const result = await api.get('/tenant/transfers', {
      page: query.page,
      size: query.size,
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

function openCreate() {
  form.toId = undefined
  form.remark = ''
  form.items = []
  pickSkuId.value = undefined
  loadWarehouseStocks()
  formVisible.value = true
}

function addItem() {
  if (!pickSkuId.value) {
    ElMessage.warning('请选择商品')
    return
  }
  const stockRow = warehouseStocks.value.find((item) => item.stock.skuId === pickSkuId.value)
  if (!stockRow) return
  if (form.items.some((item: any) => item.skuId === pickSkuId.value)) {
    ElMessage.warning('该商品已在明细中')
    return
  }
  form.items.push({
    skuId: stockRow.stock.skuId,
    skuName: stockRow.skuName,
    skuCode: stockRow.skuCode,
    available: stockRow.stock.qty,
    qty: 1,
    batchNo: ''
  })
  pickSkuId.value = undefined
}

async function save() {
  if (!form.toId) {
    ElMessage.warning('请选择调入门店')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请添加调拨商品')
    return
  }
  saving.value = true
  try {
    await api.post('/tenant/transfers', {
      fromType: 'WAREHOUSE',
      fromId: 1,
      toType: 'STORE',
      toId: form.toId,
      remark: form.remark,
      items: form.items.map((item: any) => ({
        skuId: item.skuId,
        qty: Number(item.qty || 0),
        batchNo: item.batchNo
      }))
    })
    ElMessage.success('调拨单已创建')
    formVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  detailVisible.value = true
  detail.value = null
  detailLoading.value = true
  try {
    const data = await api.get(`/tenant/transfers/${row.id}`)
    const skuMap: Record<number, string> = {}
    warehouseStocks.value.forEach((item) => {
      skuMap[item.stock.skuId] = item.skuName
    })
    data.items = (data.items || []).map((item: any) => ({
      ...item,
      skuName: skuMap[item.skuId] || `SKU #${item.skuId}`
    }))
    detail.value = data
  } finally {
    detailLoading.value = false
  }
}

async function doApprove(row: any) {
  await ElMessageBox.confirm(`确认审核通过调拨单 ${row.transferNo}？`, '审核确认', { type: 'warning' })
  await api.post(`/tenant/transfers/${row.id}/approve`)
  ElMessage.success('审核通过')
  load()
}

async function doReject(row: any) {
  await ElMessageBox.confirm(`确认驳回调拨单 ${row.transferNo}？`, '驳回确认', { type: 'warning' })
  await api.post(`/tenant/transfers/${row.id}/reject`)
  ElMessage.success('已驳回')
  load()
}

async function doComplete(row: any) {
  await ElMessageBox.confirm(`确认执行调拨单 ${row.transferNo}？执行后将同步出库与入库。`, '执行确认', { type: 'warning' })
  await api.post(`/tenant/transfers/${row.id}/complete`)
  ElMessage.success('调拨已完成')
  load()
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
  margin: 8px 0 12px;
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
</style>
