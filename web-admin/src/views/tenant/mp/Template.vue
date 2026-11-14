<template>
  <div class="page">
    <PageHeader title="模版消息" subtitle="同步并查看微信模板消息；此处仅同步查看，不提供发送与节点映射入口。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="RefreshRight" :loading="syncing" @click="syncTemplates">同步模板</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="currentAccount && !currentAccount.appSecret"
      title="当前公众号未配置 AppSecret"
      description="可查看已同步的模板记录；从微信同步模板会失败，请先在账号管理中补齐凭证。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <el-alert
      v-if="syncError"
      :title="`模板同步失败：${syncError}`"
      type="error"
      show-icon
      style="margin-bottom: 14px"
      @close="syncError = ''"
    />

    <el-alert
      title="模板消息仅做同步与查看"
      description="发送模板消息、业务节点映射不在本页维护；如需下发，请在对应的业务配置中完成。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div class="table-card">
      <div class="toolbar">
        <div>
          <strong>模板列表</strong>
          <span class="muted">共 {{ templates.length }} 个模板</span>
        </div>
        <div class="spacer"></div>
        <span v-if="currentAccount" class="muted">当前公众号：{{ currentAccount.name }}</span>
      </div>

      <el-table v-loading="loading" :data="templates" style="width: 100%">
        <el-table-column prop="title" label="模板标题" min-width="180">
          <template #default="{ row }">
            <strong class="title-cell">{{ row.title || '未命名模板' }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="模板 ID" min-width="200">
          <template #default="{ row }">
            <div class="id-cell">
              <span class="mono">{{ row.wxTemplateId }}</span>
              <el-button link type="primary" :icon="CopyDocument" @click="copy(row.wxTemplateId)" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="行业" min-width="180">
          <template #default="{ row }">
            <div class="industry-cell">
              <span>{{ row.primaryIndustry || '-' }}</span>
              <span class="muted">{{ row.secondaryIndustry || '' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="模板内容" min-width="260">
          <template #default="{ row }">
            <span class="content-cell" :title="row.content">{{ row.content || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无模板，配置凭证后可点击右上角同步模板" :image-size="64" />
        </template>
      </el-table>
    </div>

    <el-drawer v-model="detailVisible" title="模板详情" size="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="模板标题">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模板 ID"><span class="mono">{{ detail.wxTemplateId }}</span></el-descriptions-item>
        <el-descriptions-item label="主行业">{{ detail.primaryIndustry || '-' }}</el-descriptions-item>
        <el-descriptions-item label="副行业">{{ detail.secondaryIndustry || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模板内容">
          <pre class="pre-block">{{ detail.content || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="示例">
          <pre class="pre-block">{{ detail.example || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="同步时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CopyDocument, Refresh, RefreshRight } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const templates = ref<any[]>([])
const loading = ref(false)
const syncing = ref(false)
const syncError = ref('')

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
  syncError.value = ''
  load()
}

async function load() {
  if (!accountId.value) {
    templates.value = []
    return
  }
  loading.value = true
  try {
    templates.value = await api.get('/tenant/mp/templates', { accountId: accountId.value })
  } catch {
    templates.value = []
  } finally {
    loading.value = false
  }
}

async function syncTemplates() {
  if (!accountId.value) {
    ElMessage.warning('请先选择公众号账号')
    return
  }
  syncing.value = true
  syncError.value = ''
  try {
    const result = await api.post('/tenant/mp/templates/sync', null, { params: { accountId: accountId.value } })
    ElMessage.success(`同步完成，共处理 ${result?.synced || 0} 个模板`)
    await load()
  } catch (error: any) {
    syncError.value = error?.response?.data?.message || error?.message || '模板同步失败'
  } finally {
    syncing.value = false
  }
}

function openDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}

async function copy(text?: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('模板 ID 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.toolbar {
  strong { font-size: 14px; margin-right: 8px; }
  .muted { font-size: 12px; }
}

.title-cell { font-size: 13px; font-weight: 600; }

.id-cell {
  display: flex;
  align-items: center;
  gap: 2px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11.5px;
}

.industry-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}

.content-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #475467;
}

.pre-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-2);
}
</style>
