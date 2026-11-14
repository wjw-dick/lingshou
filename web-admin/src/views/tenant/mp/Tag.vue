<template>
  <div class="page">
    <PageHeader title="公众号标签" subtitle="维护粉丝标签；本地可先建标签，配置 AppSecret 后可同步微信标签库。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button :icon="RefreshRight" :loading="syncing" @click="syncTags">同步微信标签</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增标签</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="currentAccount && !currentAccount.appSecret"
      title="当前公众号未配置 AppSecret"
      description="可以继续在本地新增、删除标签；从微信同步标签会失败，请先在账号管理中补齐 AppSecret。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <el-alert
      v-if="syncError"
      :title="`微信标签同步失败：${syncError}`"
      type="error"
      show-icon
      style="margin-bottom: 14px"
      @close="syncError = ''"
    />

    <div class="table-card">
      <div class="toolbar">
        <div>
          <strong>标签列表</strong>
          <span class="muted">共 {{ tags.length }} 个标签</span>
        </div>
        <div class="spacer"></div>
        <span v-if="currentAccount" class="muted">当前公众号：{{ currentAccount.name }}</span>
      </div>
      <el-table v-loading="loading" :data="tags" style="width: 100%">
        <el-table-column prop="name" label="标签名称" min-width="180">
          <template #default="{ row }">
            <div class="tag-name">
              <el-icon><PriceTag /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="微信标签 ID" width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.wxTagId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="粉丝数" width="110" align="right">
          <template #default="{ row }">{{ Number(row.fansCount || 0).toLocaleString('zh-CN') }}</template>
        </el-table-column>
        <el-table-column label="同步状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.syncStatus" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeTag(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无标签，可新增本地标签或从微信同步" :image-size="64" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="新增标签" width="420px">
      <el-form label-position="top" @submit.prevent="createTag">
        <el-form-item label="标签名称" required>
          <el-input v-model="tagName" maxlength="32" show-word-limit placeholder="例如：鲜食会员" @keyup.enter="createTag" />
        </el-form-item>
        <div class="form-tip">
          标签会先写入本地；若当前账号已配置 AppSecret，将同时创建到微信标签库。未配置时状态显示为「本地」。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createTag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshRight } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const tags = ref<any[]>([])
const loading = ref(false)
const syncing = ref(false)
const syncError = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const tagName = ref('')

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
    tags.value = []
    return
  }
  loading.value = true
  try {
    tags.value = await api.get('/tenant/mp/tags', { accountId: accountId.value })
  } catch {
    tags.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (!accountId.value) {
    ElMessage.warning('请先在账号管理中新增公众号账号')
    return
  }
  tagName.value = ''
  dialogVisible.value = true
}

async function createTag() {
  if (!tagName.value.trim()) {
    ElMessage.warning('请填写标签名称')
    return
  }
  saving.value = true
  try {
    const result = await api.post('/tenant/mp/tags', {
      accountId: accountId.value,
      name: tagName.value.trim()
    })
    if (result?.syncStatus === 'LOCAL') {
      ElMessage.warning('标签已在本地创建，但同步到微信失败；配置 AppSecret 后可再同步')
    } else {
      ElMessage.success('标签已创建并同步到微信')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function syncTags() {
  if (!accountId.value) {
    ElMessage.warning('请先选择公众号账号')
    return
  }
  syncing.value = true
  syncError.value = ''
  try {
    const result = await api.post('/tenant/mp/tags/sync', null, { params: { accountId: accountId.value } })
    ElMessage.success(`同步完成，共处理 ${result?.synced || 0} 个微信标签`)
    await load()
  } catch (error: any) {
    syncError.value = error?.response?.data?.message || error?.message || '微信标签同步失败'
  } finally {
    syncing.value = false
  }
}

async function removeTag(row: any) {
  try {
    await ElMessageBox.confirm(`删除标签「${row.name}」？已打标的粉丝标签关系不会自动清理。`, '删除标签', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/mp/tags/${row.id}`)
  ElMessage.success('标签已删除')
  await load()
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

.tag-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 550;

  .el-icon { color: var(--brand); }
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}
</style>
