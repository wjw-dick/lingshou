<template>
  <div class="page">
    <PageHeader title="审计日志" subtitle="记录后台关键操作，支持按模块、操作人与结果筛选，日志只读不可修改。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Download" :loading="exporting" @click="exportLogs">导出 Excel</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-input v-model="query.module" placeholder="模块，如 system" clearable style="width: 180px" @keyup.enter="search" />
        <el-input v-model="query.username" placeholder="操作人账号" clearable style="width: 180px" @keyup.enter="search" />
        <el-select v-model="query.result" placeholder="全部结果" clearable style="width: 140px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="时间" width="165">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="130">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120">
          <template #default="{ row }">
            <span class="module-pill">{{ row.module || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="动作" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.action || '-' }}</template>
        </el-table-column>
        <el-table-column label="对象" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.targetType || row.targetId">{{ row.targetType || '-' }} {{ row.targetId || '' }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="95" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.result === 'SUCCESS' ? 'SUCCESS' : 'FAILED'" />
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="135">
          <template #default="{ row }">
            <span class="mono">{{ row.ip || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.detail || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无审计日志" :image-size="90" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="日志详情" size="480px">
      <div v-if="current" class="detail-list">
        <div class="detail-row"><span>操作时间</span><strong>{{ formatTime(current.createdAt) }}</strong></div>
        <div class="detail-row"><span>操作人</span><strong>{{ current.username || '-' }}</strong></div>
        <div class="detail-row"><span>用户 ID</span><strong>{{ current.userId || '-' }}</strong></div>
        <div class="detail-row"><span>模块</span><strong>{{ current.module || '-' }}</strong></div>
        <div class="detail-row"><span>动作</span><strong>{{ current.action || '-' }}</strong></div>
        <div class="detail-row"><span>对象</span><strong>{{ current.targetType || '-' }} {{ current.targetId || '' }}</strong></div>
        <div class="detail-row"><span>结果</span><StatusTag :status="current.result === 'SUCCESS' ? 'SUCCESS' : 'FAILED'" /></div>
        <div class="detail-row"><span>IP</span><strong class="mono">{{ current.ip || '-' }}</strong></div>
        <div class="detail-row column"><span>User Agent</span><p>{{ current.userAgent || '-' }}</p></div>
        <div class="detail-row column"><span>详情</span><pre>{{ prettyDetail(current.detail) }}</pre></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const loading = ref(false)
const exporting = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = ref<any>({ page: 1, size: 10, module: '', username: '', result: '' })
const detailVisible = ref(false)
const current = ref<any>(null)

onMounted(load)

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function prettyDetail(detail: string) {
  if (!detail) return '-'
  try {
    return JSON.stringify(JSON.parse(detail), null, 2)
  } catch {
    return detail
  }
}

async function load() {
  loading.value = true
  try {
    const result = await api.get('/tenant/system/audit-logs', {
      page: query.value.page,
      size: query.value.size,
      module: query.value.module || undefined,
      username: query.value.username || undefined,
      result: query.value.result || undefined
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
  query.value = { page: 1, size: query.value.size, module: '', username: '', result: '' }
  load()
}

function openDetail(row: any) {
  current.value = row
  detailVisible.value = true
}

async function exportLogs() {
  exporting.value = true
  try {
    await api.download(
      '/tenant/system/audit-logs/export',
      { module: query.value.module || undefined, username: query.value.username || undefined },
      '审计日志.xlsx'
    )
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

.module-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f2f4f7;
  color: #475467;
  font-size: 11.5px;
}

.mono {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11.5px;
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
  padding: 11px 0;
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
      word-break: break-all;
      line-height: 1.6;
    }
    pre {
      margin: 0;
      width: 100%;
      padding: 12px;
      border-radius: 8px;
      background: #f8fafc;
      font-size: 11.5px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-all;
      max-height: 320px;
      overflow: auto;
    }
  }
}
</style>
