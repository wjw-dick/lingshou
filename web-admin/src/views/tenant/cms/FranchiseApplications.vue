<template>
  <div class="page">
    <PageHeader title="加盟申请" subtitle="汇总官网与小程序提交的加盟意向，跟进联系并完成审核。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Download" :loading="exporting" @click="exportApplications">导出 Excel</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 条申请</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="公司 / 申请人" min-width="220">
          <template #default="{ row }">
            <div class="company-cell">
              <strong>{{ row.company || '个人申请' }}</strong>
              <span>{{ row.contact || '-' }} · {{ row.phone || '未留电话' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="city" label="意向城市" width="120">
          <template #default="{ row }">{{ row.city || '-' }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="投资预算" width="130">
          <template #default="{ row }">{{ row.amount || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="updateStatus(row, 'CONTACTED')">标记已联系</el-button>
            <el-button v-if="row.status !== 'APPROVED'" link type="success" @click="updateStatus(row, 'APPROVED')">通过</el-button>
            <el-button v-if="row.status !== 'REJECTED'" link type="danger" @click="updateStatus(row, 'REJECTED')">驳回</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无加盟申请" :image-size="90" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="加盟申请详情" size="460px">
      <div v-if="current" class="detail-list">
        <div class="detail-row"><span>公司名称</span><strong>{{ current.company || '个人申请' }}</strong></div>
        <div class="detail-row"><span>联系人</span><strong>{{ current.contact || '-' }}</strong></div>
        <div class="detail-row"><span>联系电话</span><strong>{{ current.phone || '-' }}</strong></div>
        <div class="detail-row"><span>意向城市</span><strong>{{ current.city || '-' }}</strong></div>
        <div class="detail-row"><span>投资预算</span><strong>{{ current.amount || '-' }}</strong></div>
        <div class="detail-row">
          <span>当前状态</span>
          <el-tag size="small" :type="statusType(current.status)" effect="light">{{ statusText(current.status) }}</el-tag>
        </div>
        <div class="detail-row"><span>提交时间</span><strong>{{ formatTime(current.createdAt) }}</strong></div>
        <div class="detail-row column"><span>备注</span><p>{{ current.remark || '无' }}</p></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import { api } from '@/api/http'

const statusOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'CONTACTED', label: '已联系' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }
]

const loading = ref(false)
const exporting = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = ref<any>({ page: 1, size: 10, status: '' })
const detailVisible = ref(false)
const current = ref<any>(null)

onMounted(load)

function statusText(value: string) {
  return statusOptions.find((item) => item.value === value)?.label || value || '-'
}

function statusType(value: string) {
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  if (value === 'CONTACTED') return 'info'
  return 'warning'
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

async function load() {
  loading.value = true
  try {
    const result = await api.get('/tenant/cms/franchise-applications', {
      page: query.value.page,
      size: query.value.size,
      status: query.value.status || undefined
    })
    rows.value = result?.records || []
    total.value = Number(result?.total) || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.value.page = 1
  load()
}

function reset() {
  query.value = { page: 1, size: query.value.size, status: '' }
  load()
}

function openDetail(row: any) {
  current.value = row
  detailVisible.value = true
}

async function updateStatus(row: any, status: string) {
  const actionText = status === 'APPROVED' ? '通过' : status === 'REJECTED' ? '驳回' : '标记为已联系'
  try {
    await ElMessageBox.confirm(`确定将「${row.company || row.contact || '该申请'}」${actionText}吗？`, '状态变更', {
      type: status === 'REJECTED' ? 'warning' : 'info',
      confirmButtonText: actionText,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.post(`/tenant/cms/franchise-applications/${row.id}/status`, null, { params: { status } })
  ElMessage.success(`已${actionText}`)
  await load()
}

async function exportApplications() {
  exporting.value = true
  try {
    await api.download('/tenant/cms/franchise-applications/export', {}, '加盟申请.xlsx')
    ElMessage.success('导出已开始')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped lang="scss">
.total-tip {
  font-size: 12px;
}

.company-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;

  strong {
    font-size: 12.5px;
    font-weight: 600;
  }
  span {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.detail-list {
  display: flex;
  flex-direction: column;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed #eef1f5;
  font-size: 12.5px;

  span {
    color: var(--ink-3);
    flex-shrink: 0;
  }
  strong {
    font-weight: 550;
    text-align: right;
    word-break: break-all;
  }

  &.column {
    flex-direction: column;
    align-items: flex-start;
    gap: 7px;
    p {
      margin: 0;
      color: var(--ink-2);
      line-height: 1.7;
      word-break: break-all;
    }
  }
}
</style>
