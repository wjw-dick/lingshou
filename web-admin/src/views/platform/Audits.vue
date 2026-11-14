<template>
  <div class="page">
    <PageHeader title="平台审计" subtitle="记录平台侧对租户、套餐、账单、公告与账号的关键操作，支持按租户与操作人追溯。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :icon="Download" :loading="exporting" @click="exportAudits">导出</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="filters.tenantId" placeholder="全部租户" clearable filterable style="width: 190px" @change="search">
          <el-option v-for="tenant in tenants" :key="tenant.tenantId" :label="tenant.name" :value="tenant.tenantId" />
        </el-select>
        <el-input
          v-model="filters.username"
          placeholder="操作人账号"
          clearable
          style="width: 180px"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.result" placeholder="全部结果" clearable style="width: 140px" @change="search">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" style="width: 100%">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ datetime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">
            <div class="operator">
              <div class="operator-avatar">{{ (row.username || 'S').slice(0, 1).toUpperCase() }}</div>
              <span>{{ row.username || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="动作" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作对象" width="140">
          <template #default="{ row }">
            <div class="stack">
              <strong>{{ row.targetType || '-' }}</strong>
              <span>{{ row.targetId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="租户 ID" width="100">
          <template #default="{ row }">{{ row.tenantId ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="86">
          <template #default="{ row }"><StatusTag :status="row.result" /></template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130">
          <template #default="{ row }">{{ row.ip || '-' }}</template>
        </el-table-column>
        <el-table-column label="详情" min-width="220">
          <template #default="{ row }">
            <div class="detail-cell">
              <span>{{ row.detail || '-' }}</span>
              <el-button v-if="row.detail" link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无审计记录" :image-size="80" />
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

    <el-drawer v-model="detailVisible" title="审计详情" size="480px">
      <div v-if="current" class="audit-detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="操作时间">{{ datetime(current.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ current.username || '-' }}（ID {{ current.userId ?? '-' }}）</el-descriptions-item>
          <el-descriptions-item label="动作">{{ current.action || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对象类型">{{ current.targetType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对象 ID">{{ current.targetId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="租户 ID">{{ current.tenantId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="结果"><StatusTag :status="current.result" /></el-descriptions-item>
          <el-descriptions-item label="IP">{{ current.ip || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="section-title" style="margin-top: 18px">操作详情</h4>
        <pre class="detail-pre">{{ current.detail || '无详情' }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const loading = ref(false)
const exporting = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const tenants = ref<any[]>([])
const filters = reactive<{ tenantId: number | undefined; username: string; result: string }>({
  tenantId: undefined,
  username: '',
  result: ''
})
const detailVisible = ref(false)
const current = ref<any>(null)

onMounted(() => {
  loadTenants()
  load()
})

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/platform/audits', {
      page: page.value,
      size: size.value,
      tenantId: filters.tenantId || undefined,
      username: filters.username || undefined,
      result: filters.result || undefined
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
  load()
}

function reset() {
  filters.tenantId = undefined
  filters.username = ''
  filters.result = ''
  search()
}

function openDetail(row: any) {
  current.value = row
  detailVisible.value = true
}

async function exportAudits() {
  exporting.value = true
  try {
    await api.download(
      '/platform/audits/export',
      { tenantId: filters.tenantId || undefined, username: filters.username || undefined },
      '平台审计日志.xlsx'
    )
    ElMessage.success('导出已开始')
  } finally {
    exporting.value = false
  }
}

function datetime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}
</script>

<style scoped lang="scss">
.toolbar .muted { font-size: 12px; }

.operator { display: flex; align-items: center; gap: 8px; }
.operator-avatar {
  width: 24px; height: 24px; border-radius: 7px; flex-shrink: 0;
  background: #eaf2ff; color: #1f6feb; display: grid; place-items: center;
  font-size: 11px; font-weight: 600;
}
.stack { display: flex; flex-direction: column; gap: 2px;
  strong { font-size: 12px; font-weight: 550; }
  span { font-size: 11px; color: var(--ink-3); }
}
.detail-cell { display: flex; align-items: center; gap: 6px; min-width: 0;
  span { flex: 1; font-size: 12px; color: var(--ink-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
}

.audit-detail { min-height: 200px; }
.detail-pre {
  margin: 0; padding: 14px; border-radius: 9px; background: #f8fafc; border: 1px solid var(--line);
  font-size: 12px; line-height: 1.7; color: var(--ink-2); white-space: pre-wrap; word-break: break-all;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
}
</style>
