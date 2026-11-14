<template>
  <div class="page">
    <PageHeader title="采购单" subtitle="采购申请、审核与转供货单的全流程管理">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :icon="MagicStick" @click="openReplenish">补货建议</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建采购单</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.supplierId" placeholder="全部供应商" clearable filterable style="width: 200px" @change="search">
          <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <div class="spacer" />
        <span class="muted">共 {{ total }} 张采购单</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无采购单">
        <el-table-column prop="orderNo" label="采购单号" min-width="150">
          <template #default="{ row }"><strong>{{ row.orderNo }}</strong></template>
        </el-table-column>
        <el-table-column label="供应商" min-width="150">
          <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
        </el-table-column>
        <el-table-column label="预计到货" width="120">
          <template #default="{ row }">{{ fmtDate(row.expectedAt) }}</template>
        </el-table-column>
        <el-table-column label="采购金额" width="130" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canEdit(row)" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" size="small" @click="doSubmit(row)">提交</el-button>
            <el-button v-if="row.status === 'SUBMITTED'" link type="success" size="small" @click="doApprove(row)">审核</el-button>
            <el-button v-if="['SUBMITTED', 'APPROVED'].includes(row.status)" link type="primary" size="small" @click="openToSupply(row)">转供货</el-button>
            <el-button v-if="row.status !== 'DONE' && row.status !== 'CANCELLED'" link type="danger" size="small" @click="doCancel(row)">取消</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑采购单' : '新建采购单'" width="880px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="供应商">
              <el-select v-model="form.supplierId" clearable filterable placeholder="请选择供应商" style="width: 100%">
                <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计到货">
              <el-date-picker v-model="form.expectedAt" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="section-title">采购明细</div>
      <div class="toolbar-inline">
        <el-button :icon="Plus" size="small" @click="openPicker">添加商品</el-button>
        <span class="muted">{{ form.items.length }} 项 · 合计 <b class="money">¥{{ money(formTotal) }}</b></span>
      </div>
      <el-table :data="form.items" stripe empty-text="请添加采购商品" max-height="320">
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
            <el-input-number v-model="row.qty" :min="1" :max="999999" size="small" style="width: 110px" @change="recalc" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" :step="0.5" size="small" style="width: 110px" @change="recalc" />
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
          <el-table-column label="参考成本" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.costPrice) }}</template>
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

    <el-drawer v-model="replenishVisible" size="960px" title="补货建议">
      <div class="toolbar-inline">
        <el-select v-model="replenishStoreId" placeholder="请选择门店" filterable style="width: 200px" @change="loadReplenish">
          <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="replenishSupplierId" placeholder="供应商（选填）" clearable filterable style="width: 200px">
          <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <span class="muted">勾选商品并确认建议数量后生成单据</span>
      </div>
      <el-table
        v-loading="replenishLoading"
        :data="replenishRows"
        stripe
        max-height="420"
        empty-text="暂无补货建议"
        @selection-change="onReplenishSelect"
      >
        <el-table-column type="selection" width="46" :selectable="selectableReplenish" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="cell-main">
              <strong>{{ row.skuName }}</strong>
              <span>{{ row.skuCode }}{{ row.barcode ? ' · ' + row.barcode : '' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="现有库存" width="90" align="center" prop="onHand" />
        <el-table-column label="在途" width="80" align="center" prop="inTransit" />
        <el-table-column label="日均销量" width="95" align="right" prop="avgDaily" />
        <el-table-column label="安全库存" width="95" align="center" prop="safetyStock" />
        <el-table-column label="建议数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.pickQty" :min="0" :max="999999" size="small" style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="参考单价" width="100" align="right">
          <template #default="{ row }">¥{{ money(row.price) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="drawer-footer">
          <span class="muted" style="margin-right: auto">已选 {{ replenishSelection.length }} 项</span>
          <el-button @click="replenishVisible = false">取消</el-button>
          <el-button :loading="replenishSaving" @click="generateSupply">生成供货单</el-button>
          <el-button type="primary" :loading="replenishSaving" @click="generatePurchase">生成采购单</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="supplyVisible" title="转供货单" width="440px">
      <el-form label-width="90px">
        <el-form-item label="采购单">
          <span class="strong">{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="供货门店" required>
          <el-select v-model="supplyStoreId" placeholder="请选择门店" filterable style="width: 100%">
            <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplyVisible = false">取消</el-button>
        <el-button type="primary" :loading="supplySaving" @click="doToSupply">确认生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="720px" :title="detail?.order?.orderNo || '采购单详情'">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="采购单号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.order.status" /></el-descriptions-item>
            <el-descriptions-item label="供应商">{{ supplierName(detail.order.supplierId) }}</el-descriptions-item>
            <el-descriptions-item label="预计到货">{{ fmtDate(detail.order.expectedAt) }}</el-descriptions-item>
            <el-descriptions-item label="采购金额">¥{{ money(detail.order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ detail.order.createdBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ fmtTime(detail.order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="section-title" style="margin-top: 20px">采购明细</div>
          <el-table :data="detail.items" stripe empty-text="暂无明细">
            <el-table-column label="商品" min-width="190">
              <template #default="{ row }">
                <div class="cell-main">
                  <strong>{{ row.skuName }}</strong>
                  <span>{{ row.skuCode }}{{ row.barcode ? ' · ' + row.barcode : '' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="采购数量" width="90" align="center">
              <template #default="{ row }">{{ row.item.qty }}</template>
            </el-table-column>
            <el-table-column label="已收" width="80" align="center">
              <template #default="{ row }">{{ row.item.receivedQty || 0 }}</template>
            </el-table-column>
            <el-table-column label="单价" width="90" align="right">
              <template #default="{ row }">¥{{ money(row.item.price) }}</template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '待审核' },
  { value: 'APPROVED', label: '已审核' },
  { value: 'RECEIVING', label: '收货中' },
  { value: 'DONE', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const stores = ref<any[]>([])
const suppliers = ref<any[]>([])
const query = reactive<any>({ page: 1, size: 10, status: '', supplierId: undefined })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const formVisible = ref(false)
const form = reactive<any>({ id: null, supplierId: undefined, expectedAt: '', remark: '', items: [] })

const pickerVisible = ref(false)
const pickerKeyword = ref('')
const spuRows = ref<any[]>([])
const spuLoading = ref(false)
const selectedSpu = ref<any>(null)
const skuRows = ref<any[]>([])
const skuLoading = ref(false)

const replenishVisible = ref(false)
const replenishLoading = ref(false)
const replenishSaving = ref(false)
const replenishRows = ref<any[]>([])
const replenishSelection = ref<any[]>([])
const replenishStoreId = ref<number>()
const replenishSupplierId = ref<number>()

const supplyVisible = ref(false)
const supplySaving = ref(false)
const currentOrder = ref<any>(null)
const supplyStoreId = ref<number>()

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
    const result = await api.get('/tenant/purchase-orders', {
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      supplierId: query.supplierId
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

function supplierName(id?: number) {
  return suppliers.value.find((item) => item.id === id)?.name || '-'
}

function canEdit(row: any) {
  return row.status === 'DRAFT' || row.status === 'SUBMITTED'
}

async function openDetail(row: any) {
  detailVisible.value = true
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await api.get(`/tenant/purchase-orders/${row.id}`)
  } finally {
    detailLoading.value = false
  }
}

function openCreate() {
  form.id = null
  form.supplierId = undefined
  form.expectedAt = ''
  form.remark = ''
  form.items = []
  formVisible.value = true
}

async function openEdit(row: any) {
  const data = await api.get(`/tenant/purchase-orders/${row.id}`)
  form.id = data.order.id
  form.supplierId = data.order.supplierId
  form.expectedAt = data.order.expectedAt
  form.remark = data.order.remark
  form.items = (data.items || []).map((row: any) => ({
    skuId: row.item.skuId,
    skuCode: row.skuCode,
    skuName: row.skuName,
    barcode: row.barcode,
    qty: row.item.qty,
    price: Number(row.item.price || 0)
  }))
  formVisible.value = true
}

function recalc() {
  // 金额由数量 * 单价实时计算
}

async function save() {
  if (!form.items.length) {
    ElMessage.warning('请添加采购商品')
    return
  }
  const payload = {
    supplierId: form.supplierId,
    expectedAt: form.expectedAt || null,
    remark: form.remark,
    items: form.items.map((item: any) => ({
      skuId: item.skuId,
      qty: Number(item.qty || 0),
      price: Number(item.price || 0)
    }))
  }
  saving.value = true
  try {
    if (form.id) {
      await api.put(`/tenant/purchase-orders/${form.id}`, payload)
      ElMessage.success('采购单已更新')
    } else {
      await api.post('/tenant/purchase-orders', payload)
      ElMessage.success('采购单已创建')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doSubmit(row: any) {
  await ElMessageBox.confirm(`确认提交采购单 ${row.orderNo}？`, '提交确认', { type: 'warning' })
  await api.post(`/tenant/purchase-orders/${row.id}/submit`)
  ElMessage.success('已提交')
  load()
}

async function doApprove(row: any) {
  await ElMessageBox.confirm(`确认审核通过采购单 ${row.orderNo}？`, '审核确认', { type: 'warning' })
  await api.post(`/tenant/purchase-orders/${row.id}/approve`)
  ElMessage.success('审核通过')
  load()
}

async function doCancel(row: any) {
  await ElMessageBox.confirm(`确认取消采购单 ${row.orderNo}？`, '取消确认', { type: 'warning' })
  await api.post(`/tenant/purchase-orders/${row.id}/cancel`)
  ElMessage.success('已取消')
  load()
}

function openToSupply(row: any) {
  currentOrder.value = row
  supplyStoreId.value = undefined
  supplyVisible.value = true
}

async function doToSupply() {
  if (!supplyStoreId.value) {
    ElMessage.warning('请选择供货门店')
    return
  }
  supplySaving.value = true
  try {
    await api.post(`/tenant/purchase-orders/${currentOrder.value.id}/to-supply?storeId=${supplyStoreId.value}`)
    ElMessage.success('供货单已生成')
    supplyVisible.value = false
  } finally {
    supplySaving.value = false
  }
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
      price: Number(sku.costPrice ?? sku.priceFranchise ?? 0)
    })
  }
  ElMessage.success(`${selectedSpu.value?.name || sku.skuCode} 已添加`)
}

async function openReplenish() {
  replenishVisible.value = true
  replenishRows.value = []
  replenishSelection.value = []
  replenishSupplierId.value = undefined
  if (!replenishStoreId.value && stores.value.length) {
    replenishStoreId.value = stores.value[0].id
  }
  if (replenishStoreId.value) {
    await loadReplenish()
  }
}

async function loadReplenish() {
  if (!replenishStoreId.value) return
  replenishLoading.value = true
  try {
    const data = await api.get('/tenant/purchase-orders/replenishment', { storeId: replenishStoreId.value })
    replenishRows.value = (data || []).map((row: any) => ({ ...row, pickQty: Number(row.suggestion || 0) }))
    replenishSelection.value = []
  } catch {
    replenishRows.value = []
  } finally {
    replenishLoading.value = false
  }
}

function onReplenishSelect(rows: any[]) {
  replenishSelection.value = rows
}

function selectableReplenish(row: any) {
  return Number(row.suggestion || 0) > 0
}

function replenishItems() {
  return replenishSelection.value
    .filter((row) => Number(row.pickQty || 0) > 0)
    .map((row) => ({
      skuId: row.skuId,
      qty: Number(row.pickQty),
      price: Number(row.price || 0)
    }))
}

async function generatePurchase() {
  const items = replenishItems()
  if (!items.length) {
    ElMessage.warning('请勾选补货商品并填写数量')
    return
  }
  replenishSaving.value = true
  try {
    await api.post('/tenant/purchase-orders', {
      supplierId: replenishSupplierId.value,
      expectedAt: null,
      remark: '由补货建议生成',
      items
    })
    ElMessage.success('采购单已生成')
    replenishVisible.value = false
    search()
  } finally {
    replenishSaving.value = false
  }
}

async function generateSupply() {
  if (!replenishStoreId.value) {
    ElMessage.warning('请选择门店')
    return
  }
  const items = replenishItems()
  if (!items.length) {
    ElMessage.warning('请勾选补货商品并填写数量')
    return
  }
  replenishSaving.value = true
  try {
    await api.post('/tenant/supply-orders', {
      sourceType: replenishSupplierId.value ? 'SUPPLIER' : 'HQ',
      supplierId: replenishSupplierId.value,
      storeId: replenishStoreId.value,
      expectedAt: null,
      remark: '由补货建议生成',
      items
    })
    ElMessage.success('供货单已生成')
    replenishVisible.value = false
  } finally {
    replenishSaving.value = false
  }
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

.drawer-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
</style>
