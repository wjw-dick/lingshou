<template>
  <div class="page">
    <PageHeader title="交接班" subtitle="管理收银班次的开启与关闭，核对备用金、现金销售与现金差异。">
      <template #actions>
        <el-select v-model="storeId" placeholder="请选择门店" style="width: 190px" @change="onStoreChange">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" :disabled="!storeId || !!current" @click="openShiftDialog">开启班次</el-button>
      </template>
    </PageHeader>

    <div v-loading="currentLoading" class="card card-pad shift-card">
      <template v-if="current">
        <div class="shift-head">
          <div class="shift-title">
            <span class="shift-badge"><span class="dot"></span>进行中</span>
            <div>
              <strong>{{ current.shiftNo }}</strong>
              <span>收银员 {{ current.cashierName || '-' }} · 开班 {{ datetime(current.startAt) }}</span>
            </div>
          </div>
          <el-button type="danger" plain :icon="SwitchButton" @click="openCloseDialog">关闭班次</el-button>
        </div>
        <div class="shift-stats">
          <div class="shift-stat"><span>备用金</span><strong>¥{{ money(current.openingCash) }}</strong></div>
          <div class="shift-stat"><span>现金销售</span><strong>¥{{ money(current.cashSales) }}</strong></div>
          <div class="shift-stat"><span>微信销售</span><strong>¥{{ money(current.wechatSales) }}</strong></div>
          <div class="shift-stat"><span>支付宝销售</span><strong>¥{{ money(current.alipaySales) }}</strong></div>
          <div class="shift-stat"><span>储值销售</span><strong>¥{{ money(current.walletSales) }}</strong></div>
          <div class="shift-stat highlight"><span>应有现金</span><strong>¥{{ money(expectedCash) }}</strong></div>
        </div>
      </template>
      <div v-else class="shift-empty">
        <div class="shift-empty-icon"><el-icon :size="26"><Clock /></el-icon></div>
        <div class="shift-empty-main">
          <strong>{{ storeId ? '当前门店暂无开启中的班次' : '请先选择门店' }}</strong>
          <span>{{ storeId ? '开启班次后即可开始收银并统计现金销售' : '选择门店后可查看当前班次或开启新班次' }}</span>
        </div>
        <el-button type="primary" :disabled="!storeId" :icon="Plus" @click="openShiftDialog">开启班次</el-button>
      </div>
    </div>

    <div class="table-card" style="margin-top: 16px">
      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option label="进行中" value="OPEN" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条交接班记录</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" style="width: 100%">
        <el-table-column label="班次号" min-width="160">
          <template #default="{ row }">
            <div class="shift-no">
              <strong>{{ row.shiftNo }}</strong>
              <span>{{ row.cashierName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="150">
          <template #default="{ row }">{{ datetime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="150">
          <template #default="{ row }">{{ datetime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column label="备用金" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.openingCash) }}</template>
        </el-table-column>
        <el-table-column label="现金销售" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.cashSales) }}</template>
        </el-table-column>
        <el-table-column label="电子支付" width="130" align="right">
          <template #default="{ row }">¥{{ money(electronic(row)) }}</template>
        </el-table-column>
        <el-table-column label="应有现金" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.expectedCash ?? Number(row.openingCash || 0) + Number(row.cashSales || 0)) }}</template>
        </el-table-column>
        <el-table-column label="实收现金" width="110" align="right">
          <template #default="{ row }">{{ row.actualCash == null ? '-' : `¥${money(row.actualCash)}` }}</template>
        </el-table-column>
        <el-table-column label="现金差异" width="110" align="right">
          <template #default="{ row }">
            <span :class="diffClass(row.diffAmount)">{{ row.diffAmount == null ? '-' : `¥${money(row.diffAmount)}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'OPEN'" link type="danger" @click="openCloseDialog(row)">关闭</el-button>
            <span v-else class="muted small">已关班</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无交接班记录" :image-size="80" />
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

    <el-dialog v-model="openVisible" title="开启班次" width="440px">
      <div class="dialog-store">
        <strong>{{ currentStoreName }}</strong>
        <span>开班时间 {{ dayjs().format('YYYY-MM-DD HH:mm') }}</span>
      </div>
      <el-form label-width="90px">
        <el-form-item label="备用金">
          <el-input-number v-model="openingCash" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon title="开启后请妥善保管备用金，关班时系统将按现金销售计算应有现金。" />
      <template #footer>
        <el-button @click="openVisible = false">取消</el-button>
        <el-button type="primary" :loading="opening" @click="submitOpen">确认开班</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeVisible" title="关闭班次" width="480px">
      <div v-if="closingShift" class="close-body">
        <div class="dialog-store">
          <strong>{{ closingShift.shiftNo }}</strong>
          <span>{{ closingShift.cashierName || '-' }} · {{ datetime(closingShift.startAt) }} 开班</span>
        </div>
        <div class="close-summary">
          <div><span>备用金</span><strong>¥{{ money(closingShift.openingCash) }}</strong></div>
          <div><span>现金销售</span><strong>¥{{ money(closingShift.cashSales) }}</strong></div>
          <div><span>应有现金</span><strong>¥{{ money(closeExpected) }}</strong></div>
          <div><span>微信 / 支付宝 / 储值</span><strong>¥{{ money(electronic(closingShift)) }}</strong></div>
        </div>
        <el-form label-width="90px">
          <el-form-item label="实收现金">
            <el-input-number v-model="closeForm.actualCash" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="现金差异">
            <div class="diff-value" :class="diffClass(closeDiff)">
              <strong>{{ closeDiff >= 0 ? '+' : '-' }}¥{{ money(Math.abs(closeDiff)) }}</strong>
              <span>{{ closeDiff === 0 ? '账实相符' : closeDiff > 0 ? '长款' : '短款' }}</span>
            </div>
          </el-form-item>
          <el-form-item v-if="closeDiff !== 0" label="差异原因">
            <el-input v-model="closeForm.diffReason" type="textarea" :rows="3" placeholder="请说明现金差异原因" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="danger" :loading="closing" @click="submitClose">确认关班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Plus, Refresh, Search, SwitchButton } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const stores = ref<any[]>([])
const storeId = ref<number>()
const current = ref<any>(null)
const currentLoading = ref(false)

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')

const openVisible = ref(false)
const opening = ref(false)
const openingCash = ref(0)

const closeVisible = ref(false)
const closing = ref(false)
const closingShift = ref<any>(null)
const closeForm = reactive({ actualCash: 0, diffReason: '' })

const expectedCash = computed(() => Number(current.value?.openingCash || 0) + Number(current.value?.cashSales || 0))
const closeExpected = computed(() => Number(closingShift.value?.openingCash || 0) + Number(closingShift.value?.cashSales || 0))
const closeDiff = computed(() => Number(closeForm.actualCash || 0) - closeExpected.value)
const currentStoreName = computed(() => stores.value.find((store) => store.id === storeId.value)?.name || '-')

onMounted(async () => {
  await loadStores()
  if (!storeId.value && stores.value.length) {
    storeId.value = stores.value[0].id
  }
  refreshAll()
})

async function loadStores() {
  try {
    const data: any = await api.get('/tenant/stores/options')
    stores.value = Array.isArray(data) ? data : []
  } catch {
    stores.value = []
  }
}

function refreshAll() {
  loadCurrent()
  load()
}

function onStoreChange() {
  page.value = 1
  refreshAll()
}

async function loadCurrent() {
  if (!storeId.value) {
    current.value = null
    return
  }
  currentLoading.value = true
  try {
    const data: any = await api.get('/tenant/shifts/current', { storeId: storeId.value })
    current.value = data || null
  } catch {
    current.value = null
  } finally {
    currentLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/tenant/shifts', {
      page: page.value,
      size: size.value,
      storeId: storeId.value || undefined,
      status: statusFilter.value || undefined
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

function openShiftDialog() {
  if (!storeId.value) {
    ElMessage.warning('请先选择门店')
    return
  }
  if (current.value) {
    ElMessage.warning('当前门店已有开启中的班次')
    return
  }
  openingCash.value = 0
  openVisible.value = true
}

async function submitOpen() {
  if (!storeId.value) return
  opening.value = true
  try {
    await api.post('/tenant/shifts/open', null, {
      params: { storeId: storeId.value, openingCash: openingCash.value }
    })
    ElMessage.success('班次已开启')
    openVisible.value = false
    refreshAll()
  } finally {
    opening.value = false
  }
}

function openCloseDialog(row?: any) {
  const shift = row || current.value
  if (!shift) {
    ElMessage.warning('当前没有可关闭的班次')
    return
  }
  closingShift.value = shift
  closeForm.actualCash = Number(shift.openingCash || 0) + Number(shift.cashSales || 0)
  closeForm.diffReason = ''
  closeVisible.value = true
}

async function submitClose() {
  const shift = closingShift.value
  if (!shift) return
  if (closeDiff.value !== 0 && !closeForm.diffReason.trim()) {
    ElMessage.warning('存在现金差异，请填写差异原因')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认关闭班次「${shift.shiftNo}」？${closeDiff.value !== 0 ? `当前现金差异 ¥${money(closeDiff.value)}。` : ''}`,
      '关闭班次',
      { type: 'warning', confirmButtonText: '确认关班', cancelButtonText: '再核对一下' }
    )
  } catch {
    return
  }
  closing.value = true
  try {
    await api.post(`/tenant/shifts/${shift.id}/close`, {
      actualCash: closeForm.actualCash,
      diffReason: closeForm.diffReason || undefined
    })
    ElMessage.success('班次已关闭')
    closeVisible.value = false
    refreshAll()
  } finally {
    closing.value = false
  }
}

function electronic(shift: any) {
  return Number(shift?.wechatSales || 0) + Number(shift?.alipaySales || 0) + Number(shift?.walletSales || 0)
}

function diffClass(value: any) {
  const diff = Number(value || 0)
  if (diff > 0) return 'diff-positive'
  if (diff < 0) return 'diff-negative'
  return 'diff-zero'
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function datetime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.shift-card { min-height: 132px; }
.shift-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.shift-title { display: flex; align-items: center; gap: 12px;
  div { display: flex; flex-direction: column; gap: 4px; }
  strong { font-size: 15px; font-weight: 650; }
  span { font-size: 11.5px; color: var(--ink-3); }
}
.shift-badge {
  display: inline-flex; align-items: center; gap: 6px; padding: 5px 10px; border-radius: 999px;
  background: #e8f7ef; color: #0e7a45; font-size: 11.5px; font-weight: 600;
  .dot { width: 6px; height: 6px; border-radius: 50%; background: #18a058; box-shadow: 0 0 0 3px rgba(24, 160, 88, 0.16); }
}
.shift-stats {
  display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; margin-top: 16px;
}
.shift-stat {
  padding: 10px 12px; border-radius: 9px; background: #f8fafc; border: 1px solid #eef1f5;
  display: flex; flex-direction: column; gap: 5px;
  span { font-size: 11px; color: var(--ink-3); }
  strong { font-size: 16px; font-weight: 650; letter-spacing: -0.01em; }
  &.highlight { background: #eaf2ff; border-color: #d6e6ff; strong { color: #1f6feb; } }
}
.shift-empty { display: flex; align-items: center; gap: 14px; padding: 8px 0;
  .shift-empty-icon { width: 48px; height: 48px; border-radius: 12px; background: #eaf2ff; color: #1f6feb; display: grid; place-items: center; flex-shrink: 0; }
  .shift-empty-main { flex: 1; display: flex; flex-direction: column; gap: 4px;
    strong { font-size: 13.5px; }
    span { font-size: 11.5px; color: var(--ink-3); }
  }
}

.toolbar .muted { font-size: 12px; }
.shift-no { display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 12.5px; font-weight: 600; }
  span { font-size: 11px; color: var(--ink-3); }
}
.small { font-size: 11.5px; }
.diff-positive { color: #18a058; font-weight: 600; }
.diff-negative { color: #d03050; font-weight: 600; }
.diff-zero { color: var(--ink-3); }

.dialog-store {
  display: flex; flex-direction: column; gap: 3px; padding: 12px 14px; border-radius: 9px; background: #f8fafc; margin-bottom: 16px;
  strong { font-size: 13.5px; }
  span { font-size: 11.5px; color: var(--ink-3); }
}
.close-body { display: flex; flex-direction: column; gap: 14px; }
.close-summary {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  div { padding: 10px 12px; border-radius: 9px; background: #f8fafc; display: flex; flex-direction: column; gap: 4px; }
  span { font-size: 11px; color: var(--ink-3); }
  strong { font-size: 14px; }
}
.diff-value { display: flex; align-items: baseline; gap: 8px;
  strong { font-size: 18px; }
  span { font-size: 11px; color: var(--ink-3); }
}

@media (max-width: 1200px) {
  .shift-stats { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
</style>
