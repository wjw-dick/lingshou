<template>
  <div class="page">
    <PageHeader title="公众号消息" subtitle="查看粉丝消息流水与原始回调报文，支持向指定 openid 发送客服文本消息。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="ChatDotRound" @click="openSend">发送消息</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="currentAccount && !currentAccount.appSecret"
      title="当前公众号未配置 AppSecret"
      description="消息列表与详情可正常查看；发送客服消息需要微信接口凭证，请先在账号管理中补齐。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="filters.msgType" placeholder="消息类型" clearable style="width: 140px">
          <el-option v-for="item in msgTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="filters.openid" placeholder="openid" clearable style="width: 190px" @keyup.enter="search" />
        <el-date-picker
          v-model="filters.range"
          type="datetimerange"
          range-separator="至"
          start-placeholder="创建开始时间"
          end-placeholder="创建结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 360px"
        />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="messages" style="width: 100%">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="openid" min-width="180">
          <template #default="{ row }">
            <div class="openid-cell">
              <span class="mono">{{ row.openid || '-' }}</span>
              <el-button v-if="row.openid" link type="primary" :icon="CopyDocument" @click="copy(row.openid)" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ msgTypeText(row.msgType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="90">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'OUT' ? 'success' : 'info'" size="small" effect="light">
              {{ row.direction === 'OUT' ? '发送' : '接收' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="260">
          <template #default="{ row }">
            <span class="content-cell" :title="row.content">{{ row.content || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="错误" width="150">
          <template #default="{ row }">
            <span v-if="row.errCode || row.errMsg" class="error-text">
              {{ row.errCode ? `[${row.errCode}]` : '' }} {{ row.errMsg || '' }}
            </span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无消息记录" :image-size="64" />
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

    <el-drawer v-model="detailVisible" title="消息详情" size="560px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="消息 ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="openid">
            <span class="mono">{{ detail.openid || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="消息类型">{{ msgTypeText(detail.msgType) }}（{{ detail.msgType }}）</el-descriptions-item>
          <el-descriptions-item label="方向">
            {{ detail.direction === 'OUT' ? '发送' : '接收' }}
          </el-descriptions-item>
          <el-descriptions-item label="内容">{{ detail.content || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="错误码">{{ detail.errCode ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息">{{ detail.errMsg || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原始报文">
            <pre class="json-pre">{{ prettyJson(detail.rawPayload) }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-dialog v-model="sendVisible" title="发送客服消息" width="480px">
      <el-form label-position="top">
        <el-form-item label="公众号">
          <el-input :model-value="currentAccount?.name || '-'" disabled />
        </el-form-item>
        <el-form-item label="接收人 openid" required>
          <el-input v-model="sendForm.openid" placeholder="粉丝 openid" />
        </el-form-item>
        <el-form-item label="文本内容" required>
          <el-input v-model="sendForm.content" type="textarea" :rows="4" maxlength="600" show-word-limit placeholder="请输入要发送的客服消息内容" />
        </el-form-item>
      </el-form>
      <el-alert v-if="sendError" :title="sendError" type="error" show-icon :closable="false" style="margin-bottom: 10px" />
      <div class="form-tip">客服消息通过微信接口实时下发，发送记录会写入消息流水。</div>
      <template #footer>
        <el-button @click="sendVisible = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, CopyDocument, Refresh, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const messages = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filters = ref<any>({ msgType: '', openid: '', range: [] })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const sendVisible = ref(false)
const sending = ref(false)
const sendError = ref('')
const sendForm = ref({ openid: '', content: '' })

const msgTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '图片', value: 'image' },
  { label: '语音', value: 'voice' },
  { label: '视频', value: 'video' },
  { label: '事件', value: 'event' },
  { label: '图文', value: 'news' },
  { label: '位置', value: 'location' },
  { label: '链接', value: 'link' },
  { label: '小视频', value: 'shortvideo' },
  { label: '小程序', value: 'miniprogram' }
]

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
    messages.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const range = filters.value.range || []
    const result = await api.get('/tenant/mp/messages', {
      page: page.value,
      size: size.value,
      accountId: accountId.value,
      msgType: filters.value.msgType || undefined,
      openid: filters.value.openid || undefined,
      start: range[0] || undefined,
      end: range[1] || undefined
    })
    messages.value = result.records || []
    total.value = Number(result.total || 0)
  } catch {
    messages.value = []
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
  filters.value = { msgType: '', openid: '', range: [] }
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

async function openDetail(row: any) {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await api.get(`/tenant/mp/messages/${row.id}`)
  } finally {
    detailLoading.value = false
  }
}

function openSend() {
  sendError.value = ''
  sendForm.value = { openid: filters.value.openid || '', content: '' }
  sendVisible.value = true
}

async function sendMessage() {
  sendError.value = ''
  if (!accountId.value) {
    ElMessage.warning('请先选择公众号账号')
    return
  }
  if (!sendForm.value.openid.trim()) {
    ElMessage.warning('请填写接收人 openid')
    return
  }
  if (!sendForm.value.content.trim()) {
    ElMessage.warning('请填写发送内容')
    return
  }
  if (!currentAccount.value?.appSecret) {
    sendError.value = '当前公众号未配置 AppSecret，无法调用微信客服消息接口，请先在账号管理中补齐凭证。'
    ElMessage.error(sendError.value)
    return
  }
  sending.value = true
  try {
    await api.post('/tenant/mp/messages/send', {
      accountId: accountId.value,
      openid: sendForm.value.openid.trim(),
      content: sendForm.value.content.trim()
    })
    ElMessage.success('客服消息已发送')
    sendVisible.value = false
    await load()
  } catch (error: any) {
    sendError.value = error?.response?.data?.message || error?.message || '消息发送失败'
  } finally {
    sending.value = false
  }
}

function msgTypeText(value?: string) {
  return msgTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

function prettyJson(value: any) {
  if (!value) return '-'
  try {
    return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2)
  } catch {
    return String(value)
  }
}

async function copy(text?: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('openid 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}
</script>

<style scoped lang="scss">
.muted { font-size: 12px; }

.openid-cell {
  display: flex;
  align-items: center;
  gap: 2px;

  .mono {
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 11.5px;
  }
}

.content-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #344054;
}

.error-text {
  color: #c0264a;
  font-size: 12px;
}

.json-pre {
  margin: 0;
  max-height: 320px;
  overflow: auto;
  font-size: 11.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--ink-2);
}
</style>
