<template>
  <div class="page">
    <PageHeader title="图文草稿箱" subtitle="编辑图文草稿并提交微信发表；封面建议 900×383，发表成功后不可撤回。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建图文</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="currentAccount && !currentAccount.appSecret"
      title="当前公众号未配置 AppSecret"
      description="草稿的新建、编辑与删除不受影响；提交微信发表会失败并在发表结果中显示原因。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div class="table-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索标题" clearable style="width: 220px" @keyup.enter="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 篇草稿</span>
      </div>

      <el-table v-loading="loading" :data="drafts" style="width: 100%">
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <el-image v-if="row.coverUrl" :src="row.coverUrl" fit="cover" class="cover-thumb">
              <template #error>
                <div class="cover-fallback"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-fallback"><el-icon><Picture /></el-icon></div>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <div class="title-cell">
              <strong>{{ row.title || '未命名草稿' }}</strong>
              <span>{{ row.digest || '暂无摘要' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="120">
          <template #default="{ row }">{{ row.author || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="right" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="publishDraft(row)">发表</el-button>
            <el-button link type="danger" @click="removeDraft(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无图文草稿，点击右上角新建图文" :image-size="64" />
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

    <el-drawer
      v-model="formVisible"
      :title="editingId ? '编辑图文草稿' : '新建图文草稿'"
      size="72%"
      :close-on-click-modal="false"
    >
      <el-form label-position="top" v-loading="formLoading">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="请输入图文标题" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="作者">
            <el-input v-model="form.author" maxlength="64" placeholder="例如：品牌中心" />
          </el-form-item>
          <el-form-item label="原文地址">
            <el-input v-model="form.sourceUrl" maxlength="512" placeholder="https://（可选）" />
          </el-form-item>
        </div>
        <el-form-item label="封面">
          <ImageUpload
            v-model="form.coverUrl"
            biz-type="MP"
            title="封面图"
            hint="建议尺寸 900×383，支持 JPG/PNG，≤5MB"
          />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input
            v-model="form.digest"
            type="textarea"
            :rows="2"
            maxlength="120"
            show-word-limit
            placeholder="用于分享卡片的摘要，留空则由微信抓取正文"
          />
        </el-form-item>
        <el-form-item label="正文">
          <RichEditor v-model="form.content" height="320px" placeholder="请输入图文正文…" />
        </el-form-item>
        <el-form-item label="多图文 articles（可选）">
          <el-input
            v-model="form.articles"
            type="textarea"
            :rows="5"
            class="json-input"
            placeholder='可选，填写 JSON 数组，例如：[{"title":"第二篇","content":"..."}]'
          />
          <div class="form-tip">
            仅保存本地草稿配置；发表时使用上方标题、摘要与正文生成图文。留空表示单图文。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDraft">保存草稿</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import RichEditor from '@/components/RichEditor.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const drafts = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')

const formVisible = ref(false)
const formLoading = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const form = ref<any>(emptyForm())

const currentAccount = computed(() => accounts.value.find((item) => item.id === accountId.value))

function emptyForm() {
  return {
    title: '',
    author: '',
    sourceUrl: '',
    coverUrl: '',
    digest: '',
    content: '',
    articles: ''
  }
}

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
    drafts.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await api.get('/tenant/mp/drafts', {
      page: page.value,
      size: size.value,
      accountId: accountId.value,
      keyword: keyword.value || undefined
    })
    drafts.value = result.records || []
    total.value = Number(result.total || 0)
  } catch {
    drafts.value = []
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
  keyword.value = ''
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

function openCreate() {
  if (!accountId.value) {
    ElMessage.warning('请先在账号管理中新增公众号账号')
    return
  }
  editingId.value = undefined
  form.value = emptyForm()
  formVisible.value = true
}

async function openEdit(row: any) {
  editingId.value = row.id
  form.value = { ...emptyForm(), ...row, articles: row.articles || '' }
  formVisible.value = true
  formLoading.value = true
  try {
    const detail = await api.get(`/tenant/mp/drafts/${row.id}`)
    form.value = {
      title: detail.title || '',
      author: detail.author || '',
      sourceUrl: detail.sourceUrl || '',
      coverUrl: detail.coverUrl || '',
      digest: detail.digest || '',
      content: detail.content || '',
      articles: detail.articles || ''
    }
  } finally {
    formLoading.value = false
  }
}

async function saveDraft() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写图文标题')
    return
  }
  let articles = (form.value.articles || '').trim()
  if (articles) {
    try {
      const parsed = JSON.parse(articles)
      if (!Array.isArray(parsed)) {
        ElMessage.error('articles 必须是 JSON 数组')
        return
      }
      articles = JSON.stringify(parsed)
    } catch {
      ElMessage.error('articles 不是合法的 JSON，请检查格式')
      return
    }
  }
  saving.value = true
  try {
    const payload = {
      accountId: accountId.value,
      title: form.value.title.trim(),
      author: form.value.author,
      sourceUrl: form.value.sourceUrl,
      coverUrl: form.value.coverUrl,
      digest: form.value.digest,
      content: form.value.content,
      articles: articles || null
    }
    if (editingId.value) {
      await api.put(`/tenant/mp/drafts/${editingId.value}`, payload)
      ElMessage.success('草稿已更新')
    } else {
      await api.post('/tenant/mp/drafts', payload)
      ElMessage.success('草稿已创建')
    }
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function publishDraft(row: any) {
  if (!currentAccount.value?.appSecret) {
    ElMessage.error('当前公众号未配置 AppSecret，无法提交微信发表，请先在账号管理中补齐凭证。')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认发表「${row.title}」？发表成功后将推送到公众号，且不可撤回。`,
      '发表图文',
      { type: 'warning', confirmButtonText: '确认发表', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await api.post(`/tenant/mp/drafts/${row.id}/publish`)
  if (result?.status === 'FAILED') {
    await ElMessageBox.alert(
      result.failReason || '微信返回发表失败，请检查封面素材与公众号配置后重试。',
      '发表失败',
      { type: 'error', confirmButtonText: '知道了' }
    )
  } else {
    await ElMessageBox.alert(
      '已提交微信发表。发表成功后不可撤回，可在「发表记录」中查看状态。',
      '发表成功',
      { confirmButtonText: '知道了' }
    )
  }
  await load()
}

async function removeDraft(row: any) {
  try {
    await ElMessageBox.confirm(`删除草稿「${row.title}」？删除后不可恢复。`, '删除草稿', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/mp/drafts/${row.id}`)
  ElMessage.success('草稿已删除')
  await load()
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.cover-thumb {
  width: 60px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  display: block;
}

.cover-fallback {
  width: 60px;
  height: 40px;
  border-radius: 6px;
  background: #f2f6fd;
  color: #98a2b3;
  display: grid;
  place-items: center;
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong { font-size: 13px; font-weight: 600; }
  span {
    font-size: 11.5px;
    color: var(--ink-3);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.json-input :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}

.muted { font-size: 12px; }

@media (max-width: 900px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
