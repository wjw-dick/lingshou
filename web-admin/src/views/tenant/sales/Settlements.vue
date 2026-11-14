<template>
  <div class="page">
    <PageHeader title="日结" subtitle="按门店与营业日汇总销售、退款与各支付方式金额，确认后完成当日结算。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openGenerate">生成日结</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="订单数" :value="summary.orders" unit="单" icon="ShoppingCart" hint="当前页合计" color="#1f6feb" />
      <StatCard label="销售额" prefix="¥" :value="summary.sales" icon="Money" hint="当前页合计" color="#14b8a6" />
      <StatCard label="退款金额" prefix="¥" :value="summary.refunds" icon="RefreshLeft" hint="当前页合计" color="#d03050" />
      <StatCard label="净销售额" prefix="¥" :value="summary.net" icon="TrendCharts" hint="销售额 - 退款" color="#f0a020" />
      <StatCard label="现金收入" prefix="¥" :value="summary.cash" icon="Wallet" hint="当前页合计" color="#7c3aed" />
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="storeId" placeholder="全部门店" clearable style="width: 190px" @change="search">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条日结记录</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" style="width: 100%">
        <el-table-column label="营业日" width="120">
          <template #default="{ row }">
            <div class="settle-date">
              <strong>{{ date(row.settleDate) }}</strong>
              <span>{{ weekday(row.settleDate) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="门店" min-width="150">
          <template #default="{ row }">{{ storeName(row.storeId) }}</template>
        </el-table-column>
        <el-table-column label="订单数" width="90" align="right">
          <template #default="{ row }">{{ row.orderCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="销售额" width="125" align="right">
          <template #default="{ row }">¥{{ money(row.salesAmount) }}</template>
        </el-table-column>
        <el-table-column label="退款" width="115" align="right">
          <template #default="{ row }"><span class="refund">¥{{ money(row.refundAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="净销售额" width="125" align="right">
          <template #default="{ row }"><span class="money">¥{{ money(row.netAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="现金" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.cashAmount) }}</template>
        </el-table-column>
        <el-table-column label="微信" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.wechatAmount) }}</template>
        </el-table-column>
        <el-table-column label="支付宝" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.alipayAmount) }}</template>
        </el-table-column>
        <el-table-column label="储值" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.walletAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <StatusTag :status="row.status" :text="row.status === 'DRAFT' ? '待确认' : undefined" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="confirm(row)">确认结算</el-button>
            <span v-else class="muted small">已确认</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无日结记录，点击右上角生成日结" :image-size="80" />
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

    <el-dialog v-model="generateVisible" title="生成日结" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="请选择门店" style="width: 100%">
            <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="营业日期" prop="date">
          <el-date-picker
            v-model="form.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择营业日"
            :disabled-date="disableFuture"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="系统按该门店当日已支付订单与退款重新汇总；同一营业日重复生成将覆盖待确认数据。"
      />
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
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const stores = ref<any[]>([])
const storeId = ref<number>()
const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const generateVisible = ref(false)
const generating = ref(false)
const formRef = ref<any>()
const form = reactive<{ storeId: number | undefined; date: string }>({
  storeId: undefined,
  date: dayjs().format('YYYY-MM-DD')
})
const rules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  date: [{ required: true, message: '请选择营业日期', trigger: 'change' }]
}

const summary = computed(() => {
  const list = rows.value
  return {
    orders: list.reduce((sum, row) => sum + Number(row.orderCount || 0), 0),
    sales: list.reduce((sum, row) => sum + Number(row.salesAmount || 0), 0),
    refunds: list.reduce((sum, row) => sum + Number(row.refundAmount || 0), 0),
    net: list.reduce((sum, row) => sum + Number(row.netAmount || 0), 0),
    cash: list.reduce((sum, row) => sum + Number(row.cashAmount || 0), 0)
  }
})

onMounted(async () => {
  await loadStores()
  load()
})

async function loadStores() {
  try {
    const data: any = await api.get('/tenant/stores/options')
    stores.value = Array.isArray(data) ? data : []
  } catch {
    stores.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/tenant/day-settlements', {
      page: page.value,
      size: size.value,
      storeId: storeId.value || undefined
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
  storeId.value = undefined
  search()
}

function openGenerate() {
  form.storeId = storeId.value || stores.value[0]?.id
  form.date = dayjs().format('YYYY-MM-DD')
  generateVisible.value = true
}

function disableFuture(value: Date) {
  return dayjs(value).isAfter(dayjs(), 'day')
}

async function submitGenerate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  generating.value = true
  try {
    const data: any = await api.post('/tenant/day-settlements/generate', null, {
      params: { storeId: form.storeId, date: form.date }
    })
    ElMessage.success(`日结已生成${data?.orderCount != null ? `，共 ${data.orderCount} 单` : ''}`)
    generateVisible.value = false
    page.value = 1
    load()
  } finally {
    generating.value = false
  }
}

async function confirm(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认 ${date(row.settleDate)} 的日结？确认后金额与状态将锁定。`,
      '确认日结',
      { type: 'warning', confirmButtonText: '确认结算', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await api.post(`/tenant/day-settlements/${row.id}/confirm`)
  ElMessage.success('日结已确认')
  load()
}

function storeName(id: number) {
  return stores.value.find((store) => store.id === id)?.name || '-'
}

function weekday(value: any) {
  if (!value) return '-'
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(value).day()]
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function date(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}
</script>

<style scoped lang="scss">
.toolbar .muted { font-size: 12px; }

.settle-date { display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 12.5px; font-weight: 600; }
  span { font-size: 10.5px; color: var(--ink-3); }
}
.refund { color: #d03050; }
.small { font-size: 11.5px; }
</style>
