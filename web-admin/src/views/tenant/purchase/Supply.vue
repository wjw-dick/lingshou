<template>
  <div class="page">
    <PageHeader title="供货单" subtitle="门店供货、接单发货与收货衔接">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建供货单</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.storeId" placeholder="全部门店" clearable filterable style="width: 180px" @change="search">
          <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.supplierId" placeholder="全部供应商" clearable filterable style="width: 180px" @change="search">
          <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索供货单号" clearable style="width: 220px" @keyup.enter="search" @clear="search">
          <template #append><el-button :icon="Search" @click="search" /></template>
        </el-input>
        <div class="spacer" />
        <span class="muted">共 {{ total }} 张供货单</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无供货单">
        <el-table-column label="供货单号" min-width="150">
          <template #default="{ row }"><strong>{{ row.order.orderNo }}</strong></template>
        </el-table-column>
        <el-table-column label="供货门店" min-width="140" prop="storeName" />
        <el-table-column label="供应商" min-width="140" prop="supplierName" />
        <el-table-column label="预计到货" width="120">
          <template #default="{ row }">{{ fmtDate(row.order.expectedAt) }}</template>
        </el-table-column>
        <el-table-column label="供货金额" width="130" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.order.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="物流单号" width="150">
          <template #default="{ row }">{{ row.order.logisticsNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.order.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.order.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.order.status === 'PENDING'" link type="success" size="small" @click="doAccept(row)">接单</el-button>
            <el-button v-if="['PENDING', 'ACCEPTED'].includes(row.order.status)" link type="primary" size="small" @click="openShip(row)">发货</el-button>
            <el-button v-if="canReceive(row.order.status)" link type="primary" size="small" @click="createReceive(row)">创建收货单</el-button>
            <el-button v-if="!['RECEIVED', 'CANCELLED'].includes(row.order.status)" link type="danger" size="small" @click="doCancel(row)">取消</el-button>
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

    <el-dialog v-model="formVisible" title="新建供货单" width="880px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="供货类型">
              <el-select v-model="form.sourceType" style="width: 100%" @change="onSourceChange">
                <el-option label="总部仓供货" value="HQ" />
                <el-option label="供应商直供" value="SUPPLIER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供货门店" required>
              <el-select v-model="form.storeId" placeholder="请选择门店" filterable style="width: 100%">
                <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商">
              <el-select v-model="form.supplierId" placeholder="供应商直供时选择" clearable filterable :disabled="form.sourceType === 'HQ'" style="width: 100%">
                <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计到货">
              <el-date-picker v-model="form.expectedAt" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="section-title">供货明细</div>
      <div class="toolbar-inline">
        <el-button :icon="Plus" size="small" @click="openPicker">添加商品</el-button>
        <span class="muted">{{ form.items.length }} 项 · 合计 <b class="money">¥{{ money(formTotal) }}</b></span>
      </div>
      <el-table :data="form.items" stripe empty-text="请添加供货商品" max-height="320">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="cell-main">
              <strong>{{ row.skuName }}</strong>
              <span>{{ row.skuCode }}{{ row.barcode ? ' · ' + row.barcode : '' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="1" :max="999999" size="small" style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="供货价" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" :step="0.5" size="small" style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.qty * row.price) }}</span></template>
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

    <el-dialog v-model="pickerVisible" title="选择商品" width="760px" append-to-body>
      <div class="toolbar-inline">
        <el-input v-model="pickerKeyword" placeholder="搜索商品名称 / 编码" clearable style="width: 280px" @keyup.enter="searchSpus" @clear="searchSpus">
          <template #append><el-button :icon="Search" @click="searchSpus" /></template>
        </el-input>
      </div>
      <el-table v-loading="spuLoading" :data="spuRows" stripe height="260" empty-text="暂无商品" highlight-current-row @row-click="pickSpu">
        <el-table-column label="商品名称" min-width="220">
          <template #default="{ row }"><strong>{{ row.spu.name }}</strong></template>
        </el-table-column>
        <el-table-column label="商品编码" width="140" prop="spu.spuCode" />
        <el-table-column label="品牌" width="120" prop="brandName" />
        <el-table-column label="SKU" width="70" align="center" prop="skuCount" />
      </el-table>
      <div v-if="selectedSpu" class="sku-pick">
        <div class="section-title">{{ selectedSpu.name }} · 选择规格</div>
        <el-table v-loading="skuLoading" :data="skuRows" stripe max-height="240" empty-text="暂无规格">
          <el-table-column label="SKU 编码" width="150" prop="skuCode" />
          <el-table-column label="条码" width="150" prop="barcode" />
          <el-table-column label="规格" min-width="110" prop="spec" />
          <el-table-column label="加盟价" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.priceFranchise) }}</template>
          </el-table-column>
          <el-table-column width="80" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="addSku(row)">添加</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="pickerVisible = false">完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="发货" width="440px">
      <el-form label-width="90px">
        <el-form-item label="供货单">
          <span class="strong">{{ currentRow?.order.orderNo }}</span>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="logisticsNo" placeholder="填写物流单号（选填）" maxlength="60" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSaving" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="720px" :title="detail?.order?.orderNo || '供货单详情'">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="供货单号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.order.status" /></el-descriptions-item>
            <el-descriptions-item label="供货门店">{{ storeName(detail.order.storeId) }}</el-descriptions-item>
            <el-descriptions-item label="供应商">{{ supplierName(detail.order.supplierId) || '总部仓' }}</el-descriptions-item>
            <el-descriptions-item label="供货类型">{{ detail.order.sourceType === 'HQ' ? '总部仓' : '供应商直供' }}</el-descriptions-item>
            <el-descriptions-item label="预计到货">{{ fmtDate(detail.order.expectedAt) }}</el-descriptions-item>
            <el-descriptions-item label="物流单号">{{ detail.order.logisticsNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="供货金额">¥{{ money(detail.order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="section-title" style="margin-top: 20px">供货明细</div>
          <el-table :data="detail.items" stripe empty-text="暂无明细">
            <el-table-column label="商品" min-width="190">
              <template #default="{ row }">
                <div class="cell-main">
                  <strong>{{ row.skuName }}</strong>
                  <span>{{ row.barcode || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="供货数量" width="90" align="center">
              <template #default="{ row }">{{ row.item.qty }}</template>
            </el-table-column>
            <el-table-column label="已发货" width="90" align="center">
              <template #default="{ row }">{{ row.item.shippedQty || 0 }}</template>
            </el-table-column>
            <el-table-column label="已收货" width="90" align="center">
              <template #default="{ row }">{{ row.item.receivedQty || 0 }}</template>
            </el-table-column>
            <el-table-column label="金额" width="110" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.item.amount) }}</span></template>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const router = useRouter()

const statusOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'ACCEPTED', label: '已接单' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'PARTIAL', label: '部分收货' },
  { value: 'RECEIVED', label: '已收货' },
  { value: 'CANCELLED', label: '已取消' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const stores = ref<any[]>([])
const suppliers = ref<any[]>([])
const query = reactive<any>({ page: 1, size: 10, storeId: undefined, supplierId: undefined, status: '', keyword: '' })

const formVisible = ref(false)
const form = reactive<any>({ sourceType: 'HQ', supplierId: undefined, storeId: undefined, expectedAt: '', remark: '', items: [] })

const pickerVisible = ref(false)
const pickerKeyword = ref('')
const spuRows = ref<any[]>([])
const spuLoading = ref(false)
const selectedSpu = ref<any>(null)
const skuRows = ref<any[]>([])
const skuLoading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const shipVisible = ref(false)
const shipSaving = ref(false)
const logisticsNo = ref('')
const currentRow = ref<any>(null)

const formTotal = computed(() =>
  form.items.reduce((sum: number, item: any) => sum + Number(item.qty || 0) * Number(item.price || 0), 0)
)

onMounted(() => {
  loadStores()
  loadSuppliers()
  load()
})

async function loadStores() {
  try {
    stores.value = await api.get('/tenant/stores/options')
  } catch {
    stores.value = []
  }
}

async function loadSuppliers() {
  try {
    suppliers.value = await api.get('/tenant/suppliers/options')
  } catch {
    suppliers.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const result = await api.get('/tenant/supply-orders', {
      page: query.page,
      size: query.size,
      storeId: query.storeId,
      supplierId: query.supplierId,
      status: query.status || undefined,
      keyword: query.keyword || undefined
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

function supplierName(id?: number) {
  return suppliers.value.find((item) => item.id === id)?.name || ''
}

function canReceive(status: string) {
  return ['PENDING', 'ACCEPTED', 'SHIPPED', 'PARTIAL'].includes(status)
}

function openCreate() {
  form.sourceType = 'HQ'
  form.supplierId = undefined
  form.storeId = undefined
  form.expectedAt = ''
  form.remark = ''
  form.items = []
  formVisible.value = true
}

function onSourceChange(value: string) {
  if (value === 'HQ') {
    form.supplierId = undefined
  }
}

async function save() {
  if (!form.storeId) {
    ElMessage.warning('请选择供货门店')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请添加供货商品')
    return
  }
  saving.value = true
  try {
    await api.post('/tenant/supply-orders', {
      sourceType: form.sourceType,
      supplierId: form.sourceType === 'HQ' ? null : form.supplierId,
      storeId: form.storeId,
      expectedAt: form.expectedAt || null,
      remark: form.remark,
      items: form.items.map((item: any) => ({
        skuId: item.skuId,
        qty: Number(item.qty || 0),
        price: Number(item.price || 0)
      }))
    })
    ElMessage.success('供货单已创建')
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
    detail.value = await api.get(`/tenant/supply-orders/${row.order.id}`)
  } finally {
    detailLoading.value = false
  }
}

async function doAccept(row: any) {
  await ElMessageBox.confirm(`确认接收供货单 ${row.order.orderNo}？`, '接单确认', { type: 'warning' })
  await api.post(`/tenant/supply-orders/${row.order.id}/accept`)
  ElMessage.success('已接单')
  load()
}

function openShip(row: any) {
  currentRow.value = row
  logisticsNo.value = row.order.logisticsNo || ''
  shipVisible.value = true
}

async function doShip() {
  shipSaving.value = true
  try {
    const id = currentRow.value.order.id
    const url = logisticsNo.value
      ? `/tenant/supply-orders/${id}/ship?logisticsNo=${encodeURIComponent(logisticsNo.value)}`
      : `/tenant/supply-orders/${id}/ship`
    await api.post(url)
    ElMessage.success('已发货')
    shipVisible.value = false
    load()
  } finally {
    shipSaving.value = false
  }
}

async function doCancel(row: any) {
  await ElMessageBox.confirm(`确认取消供货单 ${row.order.orderNo}？`, '取消确认', { type: 'warning' })
  await api.post(`/tenant/supply-orders/${row.order.id}/cancel`)
  ElMessage.success('已取消')
  load()
}

async function createReceive(row: any) {
  const data = await api.post(`/tenant/receive-orders?supplyId=${row.order.id}`)
  ElMessage.success(`收货单 ${data?.receiveNo || ''} 已创建`)
  router.push('/purchase/receive')
}

async function openPicker() {
  pickerVisible.value = true
  pickerKeyword.value = ''
  selectedSpu.value = null
  skuRows.value = []
  await searchSpus()
}

async function searchSpus() {
  spuLoading.value = true
  try {
    const result = await api.get('/tenant/products/spus', {
      page: 1,
      size: 50,
      keyword: pickerKeyword.value || undefined
    })
    spuRows.value = result?.records || []
  } catch {
    spuRows.value = []
  } finally {
    spuLoading.value = false
  }
}

async function pickSpu(row: any) {
  selectedSpu.value = row.spu
  skuLoading.value = true
  try {
    const result = await api.get(`/tenant/products/spus/${row.spu.id}`)
    skuRows.value = result?.skus || []
  } catch {
    skuRows.value = []
  } finally {
    skuLoading.value = false
  }
}

function addSku(sku: any) {
  const exists = form.items.find((item: any) => item.skuId === sku.id)
  if (exists) {
    exists.qty = Number(exists.qty || 0) + 1
  } else {
    form.items.push({
      skuId: sku.id,
      skuCode: sku.skuCode,
      skuName: selectedSpu.value?.name || sku.skuCode,
      barcode: sku.barcode,
      qty: 1,
      price: Number(sku.priceFranchise ?? sku.costPrice ?? 0)
    })
  }
  ElMessage.success(`${selectedSpu.value?.name || sku.skuCode} 已添加`)
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
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  span {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.sku-pick {
  margin-top: 16px;
  border-top: 1px solid var(--line);
  padding-top: 14px;
}
</style>
