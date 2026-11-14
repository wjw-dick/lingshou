<template>
  <div class="page">
    <PageHeader title="公众号粉丝" subtitle="查看粉丝资料、订阅状态与标签，可同步微信粉丝并维护备注。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="RefreshRight" :loading="syncing" @click="syncFans">同步粉丝</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="currentAccount && !currentAccount.appSecret"
      title="当前公众号未配置 AppSecret"
      description="粉丝列表、备注与标签可本地维护；从微信同步粉丝会失败，请先在账号管理中补齐凭证。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <el-alert
      v-if="syncError"
      :title="`粉丝同步失败：${syncError}`"
      type="error"
      show-icon
      style="margin-bottom: 14px"
      @close="syncError = ''"
    />

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="filters.openid"
          placeholder="openid"
          clearable
          style="width: 200px"
          @keyup.enter="search"
        />
        <el-input
          v-model="filters.nickname"
          placeholder="昵称"
          clearable
          style="width: 170px"
          @keyup.enter="search"
        />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 位粉丝</span>
      </div>

      <el-table v-loading="loading" :data="fans" style="width: 100%">
        <el-table-column label="粉丝" min-width="240">
          <template #default="{ row }">
            <div class="fan-cell">
              <el-avatar :size="34" :src="row.avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <div class="fan-main">
                <strong>{{ row.nickname || '未命名粉丝' }}</strong>
                <span class="openid-line">
                  <span class="mono">{{ row.openid }}</span>
                  <el-button link type="primary" :icon="CopyDocument" @click="copy(row.openid)" />
                </span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="地区" min-width="130">
          <template #default="{ row }">{{ row.region || '-' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="130">
          <template #default="{ row }">
            <span :class="{ muted: !row.remark }">{{ row.remark || '未设置' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <div v-if="tagNames(row.tags).length" class="tag-list">
              <el-tag v-for="name in tagNames(row.tags)" :key="name" size="small" effect="plain">{{ name }}</el-tag>
            </div>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="订阅状态" width="100">
          <template #default="{ row }">
            <StatusTag
              :status="row.subscribeStatus ? 'SUCCESS' : 'DISABLED'"
              :text="row.subscribeStatus ? '已关注' : '已取关'"
            />
          </template>
        </el-table-column>
        <el-table-column label="订阅时间" width="170">
          <template #default="{ row }">{{ formatTime(row.subscribeTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="right" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTagDialog(row)">设置标签</el-button>
            <el-button link type="primary" @click="openRemarkDialog(row)">备注</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无粉丝数据，可点击右上角同步粉丝" :image-size="64" />
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

    <el-dialog v-model="tagVisible" title="设置粉丝标签" width="460px">
      <div class="fan-summary" v-if="editingFan">
        <strong>{{ editingFan.nickname || '未命名粉丝' }}</strong>
        <span class="mono">{{ editingFan.openid }}</span>
      </div>
      <el-select v-model="selectedTagIds" multiple filterable placeholder="选择微信标签" style="width: 100%">
        <el-option
          v-for="tag in tags"
          :key="tag.id"
          :label="tag.wxTagId ? tag.name : `${tag.name}（仅本地）`"
          :value="tag.wxTagId"
          :disabled="!tag.wxTagId"
        />
      </el-select>
      <div class="form-tip">
        仅已同步到微信的标签可下发；本地标签需先在标签管理中配置 AppSecret 后同步。
      </div>
      <template #footer>
        <el-button @click="tagVisible = false">取消</el-button>
        <el-button type="primary" :loading="tagSaving" @click="saveFanTags">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkVisible" title="设置备注" width="420px">
      <el-form label-position="top">
        <el-form-item label="备注名称">
          <el-input v-model="remarkValue" maxlength="32" show-word-limit placeholder="便于识别的备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" :loading="remarkSaving" @click="saveRemark">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CopyDocument, Refresh, RefreshRight, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const fans = ref<any[]>([])
const tags = ref<any[]>([])
const loading = ref(false)
const syncing = ref(false)
const syncError = ref('')

const page = ref(1)
const size = ref(10)
const total = ref(0)
const filters = ref({ openid: '', nickname: '' })

const tagVisible = ref(false)
const tagSaving = ref(false)
const editingFan = ref<any>(null)
const selectedTagIds = ref<number[]>([])

const remarkVisible = ref(false)
const remarkSaving = ref(false)
const remarkValue = ref('')

const currentAccount = computed(() => accounts.value.find((item) => item.id === accountId.value))

onMounted(async () => {
  await loadAccounts()
  await Promise.all([load(), loadTags()])
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
  page.value = 1
  load()
  loadTags()
}

async function load() {
  if (!accountId.value) {
    fans.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await api.get('/tenant/mp/fans', {
      page: page.value,
      size: size.value,
      accountId: accountId.value,
      openid: filters.value.openid || undefined,
      nickname: filters.value.nickname || undefined
    })
    fans.value = result.records || []
    total.value = Number(result.total || 0)
  } catch {
    fans.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  if (!accountId.value) {
    tags.value = []
    return
  }
  try {
    tags.value = await api.get('/tenant/mp/tags', { accountId: accountId.value })
  } catch {
    tags.value = []
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.value = { openid: '', nickname: '' }
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

async function syncFans() {
  if (!accountId.value) {
    ElMessage.warning('请先选择公众号账号')
    return
  }
  syncing.value = true
  syncError.value = ''
  try {
    const result = await api.post('/tenant/mp/fans/sync', null, { params: { accountId: accountId.value } })
    ElMessage.success(`同步完成，共更新 ${result?.synced || 0} 位粉丝`)
    page.value = 1
    await load()
  } catch (error: any) {
    syncError.value = error?.response?.data?.message || error?.message || '粉丝同步失败'
  } finally {
    syncing.value = false
  }
}

function openTagDialog(row: any) {
  editingFan.value = row
  selectedTagIds.value = currentTagIds(row.tags)
  tagVisible.value = true
}

function openRemarkDialog(row: any) {
  editingFan.value = row
  remarkValue.value = row.remark || ''
  remarkVisible.value = true
}

async function saveFanTags() {
  if (!editingFan.value) return
  tagSaving.value = true
  try {
    await api.post(`/tenant/mp/fans/${editingFan.value.id}/tags`, selectedTagIds.value)
    if (!currentAccount.value?.appSecret) {
      ElMessage.warning('标签已保存到本地；当前未配置 AppSecret，未同步到微信')
    } else {
      ElMessage.success('粉丝标签已保存')
    }
    tagVisible.value = false
    await load()
  } finally {
    tagSaving.value = false
  }
}

async function saveRemark() {
  if (!editingFan.value) return
  remarkSaving.value = true
  try {
    await api.put(
      `/tenant/mp/fans/${editingFan.value.id}/remark?remark=${encodeURIComponent(remarkValue.value)}`,
      null
    )
    if (!currentAccount.value?.appSecret) {
      ElMessage.warning('备注已保存到本地；当前未配置 AppSecret，未同步到微信')
    } else {
      ElMessage.success('备注已保存')
    }
    remarkVisible.value = false
    await load()
  } finally {
    remarkSaving.value = false
  }
}

function parseTagList(value: any): string[] {
  if (!value) return []
  if (Array.isArray(value)) return value.map((item) => String(item))
  if (typeof value !== 'string') return [String(value)]
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map((item) => String(item)) : [String(value)]
  } catch {
    return value
      .replace(/^\[|\]$/g, '')
      .split(',')
      .map((item) => item.trim().replace(/^["']|["']$/g, ''))
      .filter(Boolean)
  }
}

function tagNames(value: any) {
  return parseTagList(value).map((item) => {
    const matched = tags.value.find(
      (tag) => String(tag.wxTagId) === String(item) || tag.name === item
    )
    return matched?.name || (Number.isNaN(Number(item)) ? item : `标签 #${item}`)
  })
}

function currentTagIds(value: any) {
  return parseTagList(value)
    .map((item) => {
      const matched = tags.value.find(
        (tag) => String(tag.wxTagId) === String(item) || tag.name === item
      )
      return matched?.wxTagId ? Number(matched.wxTagId) : null
    })
    .filter((item): item is number => item !== null)
}

function genderText(gender: any) {
  if (Number(gender) === 1) return '男'
  if (Number(gender) === 2) return '女'
  return '未知'
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
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.toolbar {
  strong { font-size: 14px; }
  .muted { font-size: 12px; }
}

.fan-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.fan-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  strong { font-size: 13px; font-weight: 600; }
}

.openid-line {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11.5px;
  color: var(--ink-3);
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.fan-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;

  strong { font-size: 13px; }
}
</style>
