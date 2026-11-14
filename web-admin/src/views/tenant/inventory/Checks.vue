<template>
  <div class="page">
    <PageHeader title="盘点单" subtitle="全盘与循环盘点，审核后自动调整库存差异">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建盘点单</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.storeId" placeholder="全部门店" clearable filterable style="width: 190px" @change="search">
          <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <div class="spacer" />
        <span class="muted">共 {{ total }} 张盘点单</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无盘点单">
        <el-table-column label="盘点单号" min-width="150">
          <template #default="{ row }"><strong>{{ row.checkNo }}</strong></template>
        </el-table-column>
        <el-table-column label="门店" min-width="150">
          <template #default="{ row }">{{ storeName(row.storeId) }}</template>
        </el-table-column>
        <el-table-column label="盘点类型" width="110">
          <template #default="{ row }">{{ row.checkType === 'FULL' ? '全盘' : '循环盘' }}</template>
        </el-table-column>
        <el-table-column label="差异数量" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'diff-qty': Number(row.totalDiffQty || 0) !== 0 }">{{ row.totalDiffQty || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建人" width="110">
          <template #default="{ row }">{{ row.createdBy || '-' }}</template>
        </el-table-column>
        <el-table-column label="审核人" width="110">
          <template #default="{ row }">{{ row.approvedBy || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="doApprove(row)">审核</el-button>
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

    <el-dialog v-model="formVisible" title="新建盘点单" width="900px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="盘点门店" required>
              <el-select v-model="form.storeId" placeholder="请选择门店" filterable style="width: 100%" @change="loadStoreStocks">
                <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="盘点类型">
              <el-select v-model="form.checkType" style="width: 100%">
                <el-option label="全盘" value="FULL" />
                <el-option label="循环盘" value="CYCLE" />
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

      <div class="section-title">盘点明细</div>
      <div class="toolbar-inline">
        <el-select v-model="pickSkuId" placeholder="选择盘点商品" filterable clearable style="width: 300px">
          <el-option
            v-for="item in storeStocks"
            :key="item.stock.skuId"
            :label="`${item.skuName}（账面 ${item.stock.qty}）`"
            :value="item.stock.skuId"
          />
        </el-select>
        <el-button :icon="Plus" size="small" @click="addItem">添加</el-button>
        <span class="muted">差异合计 <b :class="{ 'diff-qty': totalDiff !== 0 }">{{ totalDiff }}</b></span>
      </div>
      <el-table :data="form.items" stripe empty-text="请添加盘点商品" max-height="340">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="cell-main">
              <strong>{{ row.skuName }}</strong>
              <span>{{ row.skuCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="账面数量" width="100" align="center">
          <template #default="{ row }">{{ row.bookQty }}</template>
        </el-table-column>
        <el-table-column label="实盘数量" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.actualQty" :min="0" :max="999999" size="small" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="差异" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'diff-qty': Number(row.actualQty || 0) - Number(row.bookQty || 0) !== 0 }">
              {{ Number(row.actualQty || 0) - Number(row.bookQty || 0) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异原因" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.reason" size="small" placeholder="差异时填写原因" maxlength="60" />
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

    <el-drawer v-model="detailVisible" size="720px" :title="detail?.order?.checkNo || '盘点单详情'">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="盘点单号">{{ detail.order.checkNo }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.order.status" /></el-descriptions-item>
            <el-descriptions-item label="门店">{{ storeName(detail.order.storeId) }}</el-descriptions-item>
            <el-descriptions-item label="盘点类型">{{ detail.order.checkType === 'FULL' ? '全盘' : '循环盘' }}</el-descriptions-item>
            <el-descriptions-item label="差异数量">{{ detail.order.totalDiffQty || 0 }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ detail.order.createdBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ detail.order.approvedBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ fmtTime(detail.order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="section-title" style="margin-top: 20px">盘点明细</div>
          <el-table :data="detail.items" stripe empty-text="暂无明细">
            <el-table-column label="商品" min-width="180">
              <template #default="{ row }">{{ row.skuName || `SKU #${row.skuId}` }}</template>
            </el-table-column>
            <el-table-column label="账面" width="80" align="center" prop="bookQty" />
            <el-table-column label="实盘" width="80" align="center" prop="actualQty" />
            <el-table-column label="差异" width="80" align="center">
              <template #default="{ row }">
                <span :class="{ 'diff-qty': Number(row.diffQty || 0) !== 0 }">{{ row.diffQty }}</span>
              </template>
            </el-table-column>
            <el-table-column label="原因" min-width="160" prop="reason" />
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
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已审核' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const stores = ref<any[]>([])
const query = reactive<any>({ page: 1, size: 10, storeId: undefined, status: '' })

const formVisible = ref(false)
const form = reactive<any>({ storeId: undefined, checkType: 'FULL', remark: '', items: [] })
const storeStocks = ref<any[]>([])
const pickSkuId = ref<number>()

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const totalDiff = computed(() =>
  form.items.reduce((sum: number, item: any) => sum + (Number(item.actualQty || 0) - Number(item.bookQty || 0)), 0)
)

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
    const result = await api.get('/tenant/stock-checks', {
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

function openCreate() {
  form.storeId = undefined
  form.checkType = 'FULL'
  form.remark = ''
  form.items = []
  storeStocks.value = []
  pickSkuId.value = undefined
  formVisible.value = true
}

async function loadStoreStocks() {
  pickSkuId.value = undefined
  if (!form.storeId) {
    storeStocks.value = []
    return
  }
  try {
    storeStocks.value = await api.get('/tenant/inventory/stocks', {
      storeId: form.storeId,
      ownerType: 'STORE',
      ownerId: form.storeId
    })
  } catch {
    storeStocks.value = []
  }
}

function addItem() {
  if (!pickSkuId.value) {
    ElMessage.warning('请选择商品')
    return
  }
  const row = storeStocks.value.find((item) => item.stock.skuId === pickSkuId.value)
  if (!row) return
  if (form.items.some((item: any) => item.skuId === pickSkuId.value)) {
    ElMessage.warning('该商品已在盘点明细中')
    return
  }
  form.items.push({
    skuId: row.stock.skuId,
    skuName: row.skuName,
    skuCode: row.skuCode,
    bookQty: row.stock.qty,
    actualQty: row.stock.qty,
    reason: ''
  })
  pickSkuId.value = undefined
}

async function save() {
  if (!form.storeId) {
    ElMessage.warning('请选择盘点门店')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请添加盘点商品')
    return
  }
  const invalid = form.items.find(
    (item: any) => Number(item.actualQty || 0) !== Number(item.bookQty || 0) && !item.reason
  )
  if (invalid) {
    ElMessage.warning(`请填写 ${invalid.skuName} 的差异原因`)
    return
  }
  saving.value = true
  try {
    await api.post('/tenant/stock-checks', {
      storeId: form.storeId,
      checkType: form.checkType,
      remark: form.remark,
      items: form.items.map((item: any) => ({
        skuId: item.skuId,
        actualQty: Number(item.actualQty || 0),
        reason: item.reason
      }))
    })
    ElMessage.success('盘点单已创建')
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
    const data = await api.get(`/tenant/stock-checks/${row.id}`)
    const stocks = await api.get('/tenant/inventory/stocks', {
      storeId: data.order.storeId,
      ownerType: 'STORE',
      ownerId: data.order.storeId
    })
    const skuMap: Record<number, string> = {}
    ;(stocks || []).forEach((item: any) => {
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
  await ElMessageBox.confirm(
    `确认审核盘点单 ${row.checkNo}？审核后将按实盘数量调整库存（差异 ${row.totalDiffQty || 0}）。`,
    '审核确认',
    { type: 'warning' }
  )
  await api.post(`/tenant/stock-checks/${row.id}/approve`)
  ElMessage.success('审核通过，库存已调整')
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

.diff-qty {
  color: #c0264a;
  font-weight: 650;
}
</style>
