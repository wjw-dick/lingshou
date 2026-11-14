<template>
  <div class="page">
    <PageHeader title="发表记录" subtitle="查看图文发表状态与失败原因；成功后不可撤回，失败记录可在有草稿关联时重试。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button :icon="Document" @click="router.push('/mp/draft')">去草稿箱</el-button>
      </template>
    </PageHeader>

    <el-alert
      title="发表成功后不可撤回"
      description="已成功的图文会出现在公众号历史消息中，无法通过后台撤回；失败记录会展示微信返回的原因，可修正后重试。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option label="发表中" value="PUBLISHING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条发表记录</span>
      </div>

      <el-table v-loading="loading" :data="publishes" style="width: 100%">
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <strong class="title-cell">{{ row.title || '未命名图文' }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="220">
          <template #default="{ row }">
            <span v-if="row.failReason" class="fail-text" :title="row.failReason">{{ row.failReason }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="发表消息 ID" min-width="160">
          <template #default="{ row }">
            <span class="mono">{{ row.msgId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发表时间" width="170">
          <template #default="{ row }">{{ formatTime(row.publishAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="阅读数" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.readCount) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-tooltip v-if="row.status === 'FAILED'" :disabled="!!row.draftId" content="该记录没有关联草稿，无法重试">
              <span>
                <el-button link type="success" :disabled="!row.draftId" @click="retry(row)">重试</el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无发表记录" :image-size="64" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="发表详情" size="500px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="标题">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="关联草稿">{{ detail.draftId || '无' }}</el-descriptions-item>
        <el-descriptions-item label="发表消息 ID"><span class="mono">{{ detail.msgId || '-' }}</span></el-descriptions-item>
        <el-descriptions-item label="发表时间">{{ formatTime(detail.publishAt || detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="阅读数">{{ formatNumber(detail.readCount) }}</el-descriptions-item>
        <el-descriptions-item label="失败原因">
          <span :class="{ 'fail-text': !!detail.failReason }">{{ detail.failReason || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="detail?.status === 'SUCCESS'"
        title="该图文已发表成功，不可撤回"
        type="success"
        show-icon
        :closable="false"
        style="margin-top: 14px"
      />
      <el-alert
        v-else-if="detail?.status === 'FAILED'"
        title="发表失败，请根据失败原因修正封面素材或公众号配置后重试"
        type="error"
        show-icon
        :closable="false"
        style="margin-top: 14px"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Refresh, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const publishes = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref('')

const detailVisible = ref(false)
const detail = ref<any>(null)

const currentAccount = computed(() => accounts.value.find((item) => item.id === accountId.value))

onMounted(async () => {
  await loadAccounts()
  await load()
})

async function loadAccounts() {
  try {
    accounts.value = await api.get('/tenant/mp/accounts')
    const queryId = Number(route.query.accountId)
    accountId.value = accounts.value.some((item) => item.id === queryId) ? queryId : accounts.value[0]?.id
  } catch {
    accounts.value = []
  }
}

function handleAccountChange(id: number) {
  router.replace({ query: { ...route.query, accountId: String(id) } })
  page.value = 1
  load()
}

async function load() {
  if (!accountId.value) {
    publishes.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await api.get('/tenant/mp/publishes', {
      page: page.value,
      size: size.value,
      accountId: accountId.value,
      status: status.value || undefined
    })
    publishes.value = result.records || []
    total.value = Number(result.total || 0)
  } catch {
    publishes.value = []
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
  status.value = ''
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

function openDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}

async function retry(row: any) {
  if (!row.draftId) {
    ElMessage.warning('该发表记录没有关联草稿，无法重试')
    return
  }
  if (!currentAccount.value?.appSecret) {
    ElMessage.error('当前公众号未配置 AppSecret，无法重试发表，请先在账号管理中补齐凭证。')
    return
  }
  try {
    await ElMessageBox.confirm(
      `重新发表「${row.title}」？会重新提交微信发表，成功后同样不可撤回。`,
      '重试发表',
      { type: 'warning', confirmButtonText: '确认重试', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await api.post(`/tenant/mp/publishes/${row.id}/retry`)
  if (result?.status === 'FAILED') {
    await ElMessageBox.alert(result.failReason || '重试发表失败，请检查配置', '重试失败', {
      type: 'error',
      confirmButtonText: '知道了'
    })
  } else {
    await ElMessageBox.alert('已重新提交发表，请在列表中查看最新状态。', '重试成功', {
      confirmButtonText: '知道了'
    })
  }
  await load()
}

function formatNumber(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.title-cell {
  font-size: 13px;
  font-weight: 600;
}

.fail-text {
  color: #c0264a;
  font-size: 12px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11.5px;
}

.muted { font-size: 12px; }
</style>
