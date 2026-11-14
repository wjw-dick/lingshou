<template>
  <div class="page">
    <PageHeader title="账单与计费" subtitle="按租户与账期生成平台服务费账单，跟踪回款状态并导出对账明细。">
      <template #actions>
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button :icon="Download" :loading="exporting" @click="exportBills">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openGenerate">生成账单</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="筛选账单" :value="summary.count" unit="笔" icon="Tickets" hint="最近 200 条匹配记录" color="#1f6feb" />
      <StatCard label="已回款" prefix="¥" :value="summary.paid" icon="CircleCheck" hint="状态为已支付" color="#18a058" />
      <StatCard label="待收款" prefix="¥" :value="summary.unpaid" icon="AlarmClock" hint="待支付 + 逾期" color="#f0a020" />
      <StatCard label="逾期账单" :value="summary.overdue" unit="笔" icon="Warning" hint="已过应缴日" color="#d03050" />
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="filters.tenantId" placeholder="全部租户" clearable filterable style="width: 190px" @change="search">
          <el-option v-for="tenant in tenants" :key="tenant.tenantId" :label="tenant.name" :value="tenant.tenantId" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
          <el-option label="待支付" value="UNPAID" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已逾期" value="OVERDUE" />
        </el-select>
        <el-date-picker
          v-model="filters.period"
          type="month"
          value-format="YYYY-MM"
          placeholder="账期"
          clearable
          style="width: 150px"
          @change="search"
        />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 笔账单</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="billId" style="width: 100%">
        <el-table-column label="账单号" min-width="170">
          <template #default="{ row }">
            <div class="bill-no">
              <strong>{{ row.billNo }}</strong>
              <span>{{ datetime(row.createdAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="tenantName" label="租户" min-width="150" show-overflow-tooltip />
        <el-table-column label="账期" width="100">
          <template #default="{ row }">{{ row.period || '-' }}</template>
        </el-table-column>
        <el-table-column prop="item" label="计费项" min-width="160" show-overflow-tooltip />
        <el-table-column label="数量 × 单价" width="150" align="right">
          <template #default="{ row }">
            <span class="muted">{{ row.quantity ?? 1 }} × ¥{{ money(row.unitPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="应缴日" width="110">
          <template #default="{ row }">{{ date(row.dueDate) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" width="140">
          <template #default="{ row }">{{ row.paidAt ? datetime(row.paidAt) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'PAID'" link type="primary" @click="pay(row)">确认收款</el-button>
            <span v-else class="muted small">已结清</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无账单，点击右上角生成账单" :image-size="80" />
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

    <el-dialog v-model="generateVisible" title="生成账单" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="租户" prop="tenantId">
          <el-select v-model="form.tenantId" placeholder="请选择租户" filterable style="width: 100%">
            <el-option v-for="tenant in tenants" :key="tenant.tenantId" :label="`${tenant.name}（${tenant.tenantCode}）`" :value="tenant.tenantId" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期" prop="period">
          <el-date-picker v-model="form.period" type="month" value-format="YYYY-MM" placeholder="请选择账期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计费项">
          <el-input v-model="form.item" placeholder="留空则按租户套餐自动生成" maxlength="60" />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon title="系统按租户当前套餐价格生成待支付账单，应缴日为生成后 15 天。" />
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="submitGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const loading = ref(false)
const exporting = ref(false)
const generating = ref(false)
const rows = ref<any[]>([])
const summaryRows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive<{ tenantId: number | undefined; status: string; period: string | null }>({
  tenantId: undefined,
  status: '',
  period: null
})
const tenants = ref<any[]>([])

const generateVisible = ref(false)
const formRef = ref<any>()
const form = reactive<{ tenantId: number | undefined; period: string; item: string }>({
  tenantId: undefined,
  period: dayjs().format('YYYY-MM'),
  item: ''
})
const rules = {
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
  period: [{ required: true, message: '请选择账期', trigger: 'change' }]
}

const summary = computed(() => {
  const list = summaryRows.value
  return {
    count: list.length,
    paid: list.filter((b) => b.status === 'PAID').reduce((sum, b) => sum + Number(b.amount || 0), 0),
    unpaid: list.filter((b) => b.status !== 'PAID').reduce((sum, b) => sum + Number(b.amount || 0), 0),
    overdue: list.filter((b) => b.status === 'OVERDUE').length
  }
})

onMounted(() => {
  loadTenants()
  refreshAll()
})

function refreshAll() {
  load()
  loadSummary()
}

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/platform/bills', {
      page: page.value,
      size: size.value,
      tenantId: filters.tenantId || undefined,
      status: filters.status || undefined,
      period: filters.period || undefined
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

async function loadSummary() {
  try {
    const data: any = await api.get('/platform/bills', {
      page: 1,
      size: 200,
      tenantId: filters.tenantId || undefined,
      status: filters.status || undefined,
      period: filters.period || undefined
    })
    summaryRows.value = data?.records || []
  } catch {
    summaryRows.value = []
  }
}

async function loadTenants() {
  try {
    const data: any = await api.get('/platform/tenants', { page: 1, size: 200 })
    tenants.value = data?.records || []
  } catch {
    tenants.value = []
  }
}

function search() {
  page.value = 1
  refreshAll()
}

function reset() {
  filters.tenantId = undefined
  filters.status = ''
  filters.period = null
  search()
}

function openGenerate() {
  form.tenantId = filters.tenantId
  form.period = dayjs().format('YYYY-MM')
  form.item = ''
  generateVisible.value = true
}

async function submitGenerate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  generating.value = true
  try {
    const data: any = await api.post('/platform/bills/generate', {
      tenantId: form.tenantId,
      period: form.period,
      item: form.item || undefined
    })
    ElMessage.success(`账单 ${data?.billNo || ''} 已生成`)
    generateVisible.value = false
    page.value = 1
    refreshAll()
  } finally {
    generating.value = false
  }
}

async function pay(row: any) {
  try {
    await ElMessageBox.confirm(`确认账单「${row.billNo}」已收到 ¥${money(row.amount)}？`, '确认收款', {
      type: 'warning',
      confirmButtonText: '确认收款',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.post(`/platform/bills/${row.billId}/pay`)
  ElMessage.success('账单已标记为已支付')
  refreshAll()
}

async function exportBills() {
  exporting.value = true
  try {
    await api.download(
      '/platform/bills/export',
      {
        tenantId: filters.tenantId || undefined,
        status: filters.status || undefined,
        period: filters.period || undefined
      },
      '平台账单.xlsx'
    )
    ElMessage.success('导出已开始')
  } finally {
    exporting.value = false
  }
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function date(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}

function datetime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.toolbar .muted { font-size: 12px; }

.bill-no { display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 12.5px; font-weight: 600; letter-spacing: 0.02em; }
  span { font-size: 10.5px; color: var(--ink-3); }
}
.small { font-size: 11.5px; }
</style>
