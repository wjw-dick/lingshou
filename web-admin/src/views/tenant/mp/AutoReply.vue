<template>
  <div class="page">
    <PageHeader title="自动回复" subtitle="配置关注时、收到消息与关键词触发的自动回复；配置保存在本地，不依赖微信接口凭证。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">{{ createLabel }}</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="!loading && !accounts.length"
      title="尚未配置公众号账号"
      description="自动回复需归属到某个公众号账号，请先在账号管理中新增账号。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div class="card card-pad tabs-card">
      <el-tabs v-model="scene" @tab-change="handleTabChange">
        <el-tab-pane label="关注时" name="FOLLOW" />
        <el-tab-pane label="消息" name="MESSAGE" />
        <el-tab-pane label="关键词" name="KEYWORD" />
      </el-tabs>

      <div class="scene-tip">
        <template v-if="scene === 'FOLLOW'">粉丝关注公众号后立即回复，建议配置欢迎语与引导。</template>
        <template v-else-if="scene === 'MESSAGE'">收到粉丝消息且未命中关键词时回复，可作为客服兜底话术。</template>
        <template v-else>按关键词触发回复，可选择完全匹配或半匹配（包含即可）。</template>
      </div>

      <el-table v-loading="loading" :data="replies" style="width: 100%">
        <el-table-column v-if="scene === 'KEYWORD'" label="关键词" min-width="120">
          <template #default="{ row }">
            <strong>{{ row.keyword || '-' }}</strong>
          </template>
        </el-table-column>
        <el-table-column v-if="scene === 'KEYWORD'" label="匹配方式" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.matchType === 'EXACT' ? 'primary' : 'warning'" effect="plain">
              {{ row.matchType === 'EXACT' ? '完全匹配' : '半匹配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回复类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ replyTypeText(row.replyType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回复内容" min-width="280">
          <template #default="{ row }">
            <div class="reply-content">
              <span class="content-text" :title="row.content">{{ row.content || '-' }}</span>
              <span v-if="row.mediaId" class="mono">media_id：{{ row.mediaId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="toggleEnabled(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeReply(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="emptyText" :image-size="64" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑自动回复' : createLabel" width="520px">
      <el-form label-position="top">
        <el-form-item label="回复场景">
          <el-input :model-value="sceneLabel" disabled />
        </el-form-item>
        <template v-if="scene === 'KEYWORD'">
          <el-form-item label="关键词" required>
            <el-input v-model="form.keyword" maxlength="64" placeholder="例如：门店" />
          </el-form-item>
          <el-form-item label="匹配方式">
            <el-radio-group v-model="form.matchType">
              <el-radio value="EXACT">完全匹配</el-radio>
              <el-radio value="PART">半匹配</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
        <el-form-item label="回复类型">
          <el-select v-model="form.replyType" style="width: 100%">
            <el-option v-for="item in replyTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.replyType !== 'TEXT'" label="素材 media_id" required>
          <el-input v-model="form.mediaId" placeholder="公众号素材 media_id，可在素材管理中获取" />
        </el-form-item>
        <el-form-item :label="form.replyType === 'TEXT' ? '文本内容' : '补充内容'" :required="form.replyType === 'TEXT'">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="600"
            show-word-limit
            :placeholder="form.replyType === 'TEXT' ? '请输入自动回复文本' : '可填写说明或标题，非文本素材以 media_id 为准'"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveReply">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const scene = ref('FOLLOW')
const replies = ref<any[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const form = ref<any>({})

const replyTypes = [
  { label: '文本', value: 'TEXT' },
  { label: '图片', value: 'IMAGE' },
  { label: '语音', value: 'VOICE' },
  { label: '视频', value: 'VIDEO' },
  { label: '图文', value: 'NEWS' },
  { label: '音乐', value: 'MUSIC' }
]

const sceneLabel = computed(() => {
  const map: Record<string, string> = { FOLLOW: '关注时自动回复', MESSAGE: '消息自动回复', KEYWORD: '关键词自动回复' }
  return map[scene.value] || scene.value
})

const createLabel = computed(() => {
  const map: Record<string, string> = { FOLLOW: '新增关注回复', MESSAGE: '新增消息回复', KEYWORD: '新增关键词回复' }
  return map[scene.value] || '新增回复'
})

const emptyText = computed(() => {
  const map: Record<string, string> = {
    FOLLOW: '尚未配置关注时回复，粉丝关注后将收不到欢迎语',
    MESSAGE: '尚未配置消息自动回复',
    KEYWORD: '尚未配置关键词回复'
  }
  return map[scene.value] || '暂无自动回复'
})

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
  load()
}

function handleTabChange() {
  load()
}

async function load() {
  if (!accountId.value) {
    replies.value = []
    return
  }
  loading.value = true
  try {
    replies.value = await api.get('/tenant/mp/auto-replies', { accountId: accountId.value, scene: scene.value })
  } catch {
    replies.value = []
  } finally {
    loading.value = false
  }
}

function defaultForm() {
  return {
    scene: scene.value,
    matchType: 'EXACT',
    keyword: '',
    replyType: 'TEXT',
    content: '',
    mediaId: '',
    enabled: 1
  }
}

function openCreate() {
  if (!accountId.value) {
    ElMessage.warning('请先在账号管理中新增公众号账号')
    return
  }
  editingId.value = undefined
  form.value = defaultForm()
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = {
    scene: row.scene,
    matchType: row.matchType || 'EXACT',
    keyword: row.keyword || '',
    replyType: row.replyType || 'TEXT',
    content: row.content || '',
    mediaId: row.mediaId || '',
    enabled: row.enabled ?? 1
  }
  dialogVisible.value = true
}

async function saveReply() {
  if (scene.value === 'KEYWORD' && !form.value.keyword?.trim()) {
    ElMessage.warning('关键词回复必须填写关键词')
    return
  }
  if (form.value.replyType === 'TEXT' && !form.value.content?.trim()) {
    ElMessage.warning('请填写文本回复内容')
    return
  }
  if (form.value.replyType !== 'TEXT' && !form.value.mediaId?.trim()) {
    ElMessage.warning('请填写素材 media_id')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form.value,
      accountId: accountId.value,
      scene: scene.value,
      keyword: scene.value === 'KEYWORD' ? form.value.keyword.trim() : null,
      matchType: scene.value === 'KEYWORD' ? form.value.matchType : 'EXACT'
    }
    if (editingId.value) {
      await api.put(`/tenant/mp/auto-replies/${editingId.value}`, payload)
      ElMessage.success('自动回复已更新')
    } else {
      await api.post('/tenant/mp/auto-replies', payload)
      ElMessage.success('自动回复已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row: any, value: any) {
  try {
    await api.put(`/tenant/mp/auto-replies/${row.id}`, { ...row, enabled: value ? 1 : 0 })
    ElMessage.success(value ? '已启用' : '已停用')
  } catch {
    await load()
  }
}

async function removeReply(row: any) {
  try {
    await ElMessageBox.confirm('删除后该自动回复将立即失效，确认删除？', '删除自动回复', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/mp/auto-replies/${row.id}`)
  ElMessage.success('自动回复已删除')
  await load()
}

function replyTypeText(value?: string) {
  return replyTypes.find((item) => item.value === value)?.label || value || '-'
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.tabs-card {
  padding-top: 8px;

  :deep(.el-tabs__header) { margin-bottom: 4px; }
}

.scene-tip {
  font-size: 12px;
  color: var(--ink-3);
  padding: 4px 0 12px;
}

.reply-content {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.content-text {
  color: #344054;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  color: var(--ink-3);
}
</style>
