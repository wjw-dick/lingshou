<template>
  <div class="page">
    <PageHeader title="供应商对账" subtitle="按供应商与账期生成对账单，完成确认、开票与付款。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openGenerate">生成对账单</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.supplierId" placeholder="供应商" clearable filterable style="width: 220px">
          <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="对账状态" clearable style="width: 150px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 张对账单</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe :row-key="statementKey">
        <el-table-column label="对账单号" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ row.statementNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="供应商" min-width="200">
          <template #default="{ row }">
            <div class="two-line">
              <span>{{ row.partnerName }}</span>
              <small>合作伙伴 ID {{ row.partnerId }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="账期" width="190">
          <template #default="{ row }">{{ row.periodStart }} ~ {{ row.periodEnd }}</template>
        </el-table-column>
        <el-table-column label="供货金额" width="130" align="right">
          <template #default="{ row }"><span class="money">¥ {{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="扣款" width="110" align="right">
          <template #default="{ row }">¥ {{ money(row.deductionAmount) }}</template>
        </el-table-column>
        <el-table-column label="应付金额" width="130" align="right">
          <template #default="{ row }"><span class="money payable">¥ {{ money(row.payableAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt, 'YYYY-MM-DD') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" text type="primary" size="small" @click="confirmStatement(row)">确认</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" text type="primary" size="small" @click="payStatement(row)">付款</el-button>
            <el-button v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'" text type="danger" size="small" @click="disputeStatement(row)">异议</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无供应商对账单" :image-size="70" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="对账单详情" size="760px">
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <div class="statement-head">
            <div>
              <h3>{{ detail.statement.statementNo }}</h3>
              <p>{{ detail.statement.partnerName }} · {{ detail.statement.periodStart }} ~ {{ detail.statement.periodEnd }}</p>
            </div>
            <StatusTag :status="detail.statement.status" />
          </div>

          <div class="amount-panel">
            <div>
              <span>供货金额</span>
              <strong>¥ {{ money(detail.statement.totalAmount) }}</strong>
            </div>
            <div>
              <span>扣款金额</span>
              <strong>¥ {{ money(detail.statement.deductionAmount) }}</strong>
            </div>
            <div class="highlight">
              <span>应付金额</span>
              <strong>¥ {{ money(detail.statement.payableAmount) }}</strong>
            </div>
          </div>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="对账类型">供应商对账</el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ statusLabel(detail.statement.status) }}
            </el-descriptions-item>
            <el-descriptions-item label="确认时间">{{ formatTime(detail.statement.confirmedAt) }}</el-descriptions-item>
            <el-descriptions-item label="付款时间">{{ formatTime(detail.statement.paidAt) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.statement.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-head">
            <h3 class="section-title">对账明细</h3>
            <span class="muted">共 {{ (detail.items || []).length }} 条</span>
          </div>
          <el-table :data="detail.items || []" stripe size="small">
            <el-table-column label="来源单号" min-width="160">
              <template #default="{ row }">{{ row.refNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ refTypeLabel(row.refType) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }">¥ {{ money(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="扣款" width="110" align="right">
              <template #default="{ row }">¥ {{ money(row.deduction) }}</template>
            </el-table-column>
            <el-table-column label="应付" width="120" align="right">
              <template #default="{ row }"><span class="money">¥ {{ money(row.payable) }}</span></template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无对账明细" :image-size="60" />
            </template>
          </el-table>

          <div class="section-head" style="margin-top: 20px">
            <h3 class="section-title">发票记录</h3>
            <el-button type="primary" plain size="small" :icon="Plus" @click="openInvoice">登记发票</el-button>
          </div>
          <el-table :data="detail.invoices || []" stripe size="small">
            <el-table-column label="发票号" min-width="150">
              <template #default="{ row }">{{ row.invoiceNo }}</template>
            </el-table-column>
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }">¥ {{ money(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="附件" min-width="140">
              <template #default="{ row }">
                <el-link v-if="row.fileUrl" type="primary" :href="row.fileUrl" target="_blank">查看附件</el-link>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column label="登记时间" width="140">
              <template #default="{ row }">{{ formatTime(row.createdAt, 'YYYY-MM-DD') }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无发票记录" :image-size="60" />
            </template>
          </el-table>
        </template>
        <el-empty v-else-if="!detailLoading" description="对账单加载失败" :image-size="70" />
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="detail?.statement.status === 'PENDING' || detail?.statement.status === 'CONFIRMED'"
            type="danger"
            plain
            @click="disputeStatement(detail.statement)"
          >提交异议</el-button>
          <el-button
            v-if="detail?.statement.status === 'PENDING'"
            type="primary"
            plain
            @click="confirmStatement(detail.statement)"
          >确认对账</el-button>
          <el-button
            v-if="detail?.statement.status === 'CONFIRMED'"
            type="primary"
            @click="payStatement(detail.statement)"
          >确认付款</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="generateVisible" title="生成供应商对账单" width="480px">
      <el-form ref="generateRef" :model="generateForm" :rules="generateRules" label-width="90px">
        <el-form-item label="供应商" prop="supplierId">
          <el-select v-model="generateForm.supplierId" filterable placeholder="请选择供应商" style="width: 100%">
            <el-option v-for="item in suppliers" :key="item.id" :label="`${item.name}（${item.code}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="对账账期" prop="period">
          <el-date-picker v-model="generateForm.period" type="month" value-format="YYYY-MM" placeholder="请选择账期" style="width: 100%" />
          <div class="form-tip">按所选自然月汇总已收货的供货单，同一供应商同一账期仅生成一张对账单。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitGenerate">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invoiceVisible" title="登记发票" width="540px">
      <el-form ref="invoiceRef" :model="invoiceForm" :rules="invoiceRules" label-width="90px">
        <el-form-item label="发票号" prop="invoiceNo">
          <el-input v-model="invoiceForm.invoiceNo" maxlength="64" placeholder="请输入发票号码" />
        </el-form-item>
        <el-form-item label="发票金额" prop="amount">
          <el-input-number v-model="invoiceForm.amount" :min="0.01" :precision="2" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发票附件" prop="fileUrl">
          <div style="width: 100%">
            <ImageUpload v-model="invoiceForm.fileUrl" title="发票文件" hint="支持图片或 PDF，单个文件 ≤5MB" biz-type="SETTLEMENT_INVOICE" accept="image/*,.pdf" allow-file />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitInvoice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const suppliers = ref<any[]>([])
const query = reactive<{ supplierId?: number; status?: string }>({ supplierId: undefined, status: undefined })

const statusOptions = [
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '有异议', value: 'DISPUTED' },
  { label: '已支付', value: 'PAID' }
]

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const generateVisible = ref(false)
const generateRef = ref<any>()
const generateForm = reactive<any>({ supplierId: undefined, period: dayjs().subtract(1, 'month').format('YYYY-MM') })
const generateRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  period: [{ required: true, message: '请选择对账账期', trigger: 'change' }]
}

const invoiceVisible = ref(false)
const invoiceRef = ref<any>()
const invoiceForm = reactive<any>({ invoiceNo: '', amount: 0, fileUrl: '' })
const invoiceRules = {
  invoiceNo: [{ required: true, message: '请输入发票号', trigger: 'blur' }],
  amount: [
    { required: true, message: '请输入发票金额', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (!(Number(value) > 0)) {
          callback(new Error('发票金额必须大于 0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  load()
  loadSuppliers()
})

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
    const data = await api.get('/tenant/settlements', {
      page: page.value,
      size: size.value,
      type: 'SUPPLIER',
      partnerId: query.supplierId || undefined,
      status: query.status || undefined
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
  query.supplierId = undefined
  query.status = undefined
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

function statementKey(row: any) {
  return row.id
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await api.get(`/tenant/settlements/${row.id}`)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function reloadDetail() {
  if (!detail.value) return
  try {
    detail.value = await api.get(`/tenant/settlements/${detail.value.statement.id}`)
  } catch {
    // ignore
  }
}

function openGenerate() {
  generateForm.supplierId = undefined
  generateForm.period = dayjs().subtract(1, 'month').format('YYYY-MM')
  generateVisible.value = true
  nextTick(() => generateRef.value?.clearValidate())
}

function submitGenerate() {
  generateRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      const statement = await api.post('/tenant/settlements/generate-supplier', null, {
        params: { supplierId: generateForm.supplierId, period: generateForm.period }
      })
      ElMessage.success(`对账单 ${statement?.statementNo || ''} 已生成`)
      generateVisible.value = false
      load()
      if (statement?.id) {
        openDetail(statement)
      }
    } finally {
      saving.value = false
    }
  })
}

async function confirmStatement(row: any) {
  try {
    await ElMessageBox.confirm(`确认对账单「${row.statementNo}」金额无误？确认后进入待付款状态。`, '确认对账', {
      type: 'warning',
      confirmButtonText: '确认对账',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.post(`/tenant/settlements/${row.id}/confirm`)
    ElMessage.success('对账单已确认')
    load()
    reloadDetail()
  } catch {
    // handled by interceptor
  }
}

async function payStatement(row: any) {
  try {
    await ElMessageBox.confirm(`确认已向「${row.partnerName}」支付 ¥${money(row.payableAmount)}？`, '确认付款', {
      type: 'warning',
      confirmButtonText: '确认付款',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.post(`/tenant/settlements/${row.id}/pay`)
    ElMessage.success('对账单已标记为已支付')
    load()
    reloadDetail()
  } catch {
    // handled by interceptor
  }
}

async function disputeStatement(row: any) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请填写异议原因，提交后对账单状态将变更为「有异议」。', '提交异议', {
      inputType: 'textarea',
      inputPlaceholder: '如：8 月 12 日供货数量与实收不一致',
      inputValidator: (value: string) => (value && value.trim() ? true : '请填写异议原因'),
      confirmButtonText: '提交异议',
      cancelButtonText: '取消'
    })
    reason = result.value
  } catch {
    return
  }
  try {
    await api.post(`/tenant/settlements/${row.id}/dispute`, null, { params: { reason } })
    ElMessage.success('异议已提交')
    load()
    reloadDetail()
  } catch {
    // handled by interceptor
  }
}

function openInvoice() {
  invoiceForm.invoiceNo = ''
  invoiceForm.amount = Number(detail.value?.statement?.payableAmount || 0)
  invoiceForm.fileUrl = ''
  invoiceVisible.value = true
  nextTick(() => invoiceRef.value?.clearValidate())
}

function submitInvoice() {
  invoiceRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      await api.post(`/tenant/settlements/${detail.value.statement.id}/invoices`, null, {
        params: {
          invoiceNo: invoiceForm.invoiceNo,
          amount: Number(invoiceForm.amount),
          fileUrl: invoiceForm.fileUrl || undefined
        }
      })
      ElMessage.success('发票已登记')
      invoiceVisible.value = false
      reloadDetail()
    } finally {
      saving.value = false
    }
  })
}

function statusLabel(status: string) {
  return statusOptions.find((item) => item.value === status)?.label || status || '-'
}

function refTypeLabel(type: string) {
  const map: Record<string, string> = {
    SUPPLY: '供货单',
    FRANCHISE: '加盟门店',
    PURCHASE: '采购单'
  }
  return map[type] || type || '-'
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(value: any, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '-'
}
</script>

<style scoped lang="scss">
.two-line {
  display: flex;
  flex-direction: column;
  gap: 2px;

  span {
    font-size: 12.5px;
  }

  small {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.payable {
  color: #1454c8;
}

.detail-wrap {
  min-height: 300px;
}

.statement-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 650;
  }

  p {
    margin: 5px 0 0;
    font-size: 12px;
    color: var(--ink-3);
  }
}

.amount-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 16px;

  > div {
    border: 1px solid var(--line);
    border-radius: 9px;
    padding: 12px 14px;
    background: #fbfcfe;
    display: flex;
    flex-direction: column;
    gap: 6px;

    span {
      font-size: 11.5px;
      color: var(--ink-3);
    }

    strong {
      font-size: 17px;
      font-weight: 680;
      font-variant-numeric: tabular-nums;
    }
  }

  .highlight {
    background: #eef4fe;
    border-color: #c2d8fa;

    strong {
      color: #1454c8;
    }
  }
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 8px;

  .section-title {
    margin: 0;
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
