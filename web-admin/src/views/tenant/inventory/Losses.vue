<template>
  <div class="page">
    <PageHeader title="报损单" subtitle="临期、破损与过期商品报损审批">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建报损单</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.storeId" placeholder="全部门店" clearable filterable style="width: 190px" @change="search">
          <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.reasonType" placeholder="全部原因" clearable style="width: 150px" @change="search">
          <el-option v-for="item in reasonOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <div class="spacer" />
        <span class="muted">共 {{ total }} 张报损单</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无报损单">
        <el-table-column label="报损单号" min-width="150">
          <template #default="{ row }"><strong>{{ row.lossNo }}</strong></template>
        </el-table-column>
        <el-table-column label="门店" min-width="150">
          <template #default="{ row }">{{ storeName(row.storeId) }}</template>
        </el-table-column>
        <el-table-column label="报损原因" width="110">
          <template #default="{ row }">
            <el-tag :type="reasonTag(row.reasonType)" effect="plain" size="small">{{ reasonText(row.reasonType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报损数量" width="100" align="center" prop="totalQty" />
        <el-table-column label="报损金额" width="130" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="申请人" width="110">
          <template #default="{ row }">{{ row.createdBy || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="doApprove(row)">审核通过</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click="doReject(row)">驳回</el-button>
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

    <el-dialog v-model="formVisible" title="新建报损单" width="920px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="报损门店" required>
              <el-select v-model="form.storeId" placeholder="请选择门店" filterable style="width: 100%" @change="loadStoreStocks">
                <el-option v-for="item in stores" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报损原因" required>
              <el-select v-model="form.reasonType" style="width: 100%">
                <el-option v-for="item in reasonOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="现场照片">
          <ImageUpload v-model="photoUrl" title="报损照片" hint="拍照上传，支持 JPG/PNG，≤5MB" biz-type="LOSS" :allow-file="false" />
        </el-form-item>
      </el-form>

      <div class="section-title">报损明细</div>
      <div class="toolbar-inline">
        <el-select v-model="pickSkuId" placeholder="选择报损商品" filterable clearable style="width: 300px">
          <el-option
            v-for="item in storeStocks"
            :key="item.stock.skuId"
            :label="`${item.skuName}（库存 ${item.stock.qty}）`"
            :value="item.stock.skuId"
          />
        </el-select>
        <el-button :icon="Plus" size="small" @click="addItem">添加</el-button>
        <span class="muted">合计 <b class="money">¥{{ money(totalAmount) }}</b></span>
      </div>
      <el-table :data="form.items" stripe empty-text="请添加报损商品" max-height="320">
        <el-table-column label="商品" min-width="190">
          <template #default="{ row }">
            <div class="cell-main">
              <strong>{{ row.skuName }}</strong>
              <span>{{ row.skuCode }} · 库存 {{ row.available }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="批次号" width="170">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" size="small" placeholder="选填" maxlength="40" />
          </template>
        </el-table-column>
        <el-table-column label="报损数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="1" :max="row.available || 999999" size="small" style="width: 110px" @change="recalc(row)" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
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

    <el-drawer v-model="detailVisible" size="720px" :title="detail?.order?.lossNo || '报损单详情'">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="报损单号">{{ detail.order.lossNo }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.order.status" /></el-descriptions-item>
            <el-descriptions-item label="门店">{{ storeName(detail.order.storeId) }}</el-descriptions-item>
            <el-descriptions-item label="报损原因">{{ reasonText(detail.order.reasonType) }}</el-descriptions-item>
            <el-descriptions-item label="报损数量">{{ detail.order.totalQty || 0 }}</el-descriptions-item>
            <el-descriptions-item label="报损金额">¥{{ money(detail.order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="申请人">{{ detail.order.createdBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ detail.order.approvedBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ fmtTime(detail.order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="photos.length" class="photo-list">
            <el-image
              v-for="(photo, index) in photos"
              :key="index"
              :src="photo"
              :preview-src-list="photos"
              :initial-index="index"
              fit="cover"
              class="photo"
            />
          </div>

          <div class="section-title" style="margin-top: 20px">报损明细</div>
          <el-table :data="detail.items" stripe empty-text="暂无明细">
            <el-table-column label="商品" min-width="170">
              <template #default="{ row }">{{ row.skuName || `SKU #${row.skuId}` }}</template>
            </el-table-column>
            <el-table-column label="批次号" width="150">
              <template #default="{ row }">{{ row.batchNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="数量" width="80" align="center" prop="qty" />
            <el-table-column label="金额" width="110" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
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
import ImageUpload from '@/components/ImageUpload.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }
]

const reasonOptions = [
  { value: 'NEAR_EXPIRY', label: '临期' },
  { value: 'DAMAGE', label: '破损' },
  { value: 'EXPIRED', label: '过期' },
  { value: 'OTHER', label: '其他' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const stores = ref<any[]>([])
const query = reactive<any>({ page: 1, size: 10, storeId: undefined, status: '', reasonType: '' })

const formVisible = ref(false)
const form = reactive<any>({ storeId: undefined, reasonType: 'NEAR_EXPIRY', remark: '', items: [] })
const photoUrl = ref('')
const storeStocks = ref<any[]>([])
const pickSkuId = ref<number>()

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const photos = ref<string[]>([])

const totalAmount = computed(() =>
  form.items.reduce((sum: number, item: any) => sum + Number(item.amount || 0), 0)
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
    const result = await api.get('/tenant/loss-orders', {
      page: query.page,
      size: query.size,
      storeId: query.storeId,
      status: query.status || undefined,
      reasonType: query.reasonType || undefined
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

function reasonText(value?: string) {
  return reasonOptions.find((item) => item.value === value)?.label || value || '-'
}

function reasonTag(value?: string) {
  if (value === 'EXPIRED') return 'danger'
  if (value === 'DAMAGE') return 'warning'
  if (value === 'NEAR_EXPIRY') return 'primary'
  return 'info'
}

function openCreate() {
  form.storeId = undefined
  form.reasonType = 'NEAR_EXPIRY'
  form.remark = ''
  form.items = []
  photoUrl.value = ''
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
    ElMessage.warning('该商品已在报损明细中')
    return
  }
  const price = Number(row.price || 0)
  form.items.push({
    skuId: row.stock.skuId,
    skuName: row.skuName,
    skuCode: row.skuCode,
    available: row.stock.qty,
    batchNo: '',
    qty: 1,
    price,
    amount: price
  })
  pickSkuId.value = undefined
}

function recalc(row: any) {
  row.amount = Number((Number(row.qty || 0) * Number(row.price || 0)).toFixed(2))
}

async function save() {
  if (!form.storeId) {
    ElMessage.warning('请选择报损门店')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请添加报损商品')
    return
  }
  saving.value = true
  try {
    await api.post('/tenant/loss-orders', {
      storeId: form.storeId,
      reasonType: form.reasonType,
      photos: photoUrl.value ? JSON.stringify([photoUrl.value]) : '',
      remark: form.remark,
      items: form.items.map((item: any) => ({
        skuId: item.skuId,
        qty: Number(item.qty || 0),
        batchNo: item.batchNo,
        amount: Number(item.amount || 0)
      }))
    })
    ElMessage.success('报损单已创建')
    formVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  detailVisible.value = true
  detail.value = null
  photos.value = []
  detailLoading.value = true
  try {
    const data = await api.get(`/tenant/loss-orders/${row.id}`)
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
    photos.value = parsePhotos(data.order.photos)
  } finally {
    detailLoading.value = false
  }
}

function parsePhotos(value: any): string[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map((item) => String(item)) : []
  } catch {
    return String(value)
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
}

async function doApprove(row: any) {
  await ElMessageBox.confirm(`确认审核通过报损单 ${row.lossNo}？通过后将扣减门店库存。`, '审核确认', { type: 'warning' })
  await api.post(`/tenant/loss-orders/${row.id}/approve`)
  ElMessage.success('审核通过，库存已扣减')
  load()
}

async function doReject(row: any) {
  await ElMessageBox.confirm(`确认驳回报损单 ${row.lossNo}？`, '驳回确认', { type: 'warning' })
  await api.post(`/tenant/loss-orders/${row.id}/reject`)
  ElMessage.success('已驳回')
  load()
}

function money(value: any) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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

.photo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.photo {
  width: 120px;
  height: 90px;
  border-radius: 8px;
  border: 1px solid var(--line);
}
</style>
