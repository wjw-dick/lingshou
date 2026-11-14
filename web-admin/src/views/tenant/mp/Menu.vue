<template>
  <div class="page">
    <PageHeader title="公众号菜单" subtitle="查看当前公众号菜单树，校验层级后一键发布到微信；菜单内容在「菜单可视化」中维护。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button :icon="Edit" @click="router.push('/system/menus')">去编辑菜单</el-button>
        <el-button type="primary" :icon="Promotion" :loading="publishing" @click="publishMenu">发布菜单</el-button>
        <el-button type="danger" plain :icon="Delete" :loading="clearing" @click="clearMenu">清空菜单</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="currentAccount && !currentAccount.appSecret"
      title="当前公众号未配置 AppSecret"
      description="可以查看与编辑本地菜单配置；发布或清空微信菜单需要微信接口凭证，请先在账号管理中补齐。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <el-alert
      v-if="actionError"
      :title="actionError"
      type="error"
      show-icon
      style="margin-bottom: 14px"
      @close="actionError = ''"
    />

    <el-alert
      title="微信菜单层级限制：一级菜单最多 3 个，每个一级菜单下二级菜单最多 5 个"
      description="菜单发布后需重新进入公众号会话才能看到（微信客户端存在缓存）；本地菜单配置不会因清空微信菜单而删除。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div class="summary-grid">
      <div class="card summary-card">
        <span class="summary-label">一级菜单</span>
        <strong :class="{ over: roots.length > 3 }">{{ roots.length }}<em>/ 3</em></strong>
        <StatusTag :status="roots.length > 3 ? 'FAILED' : 'SUCCESS'" :text="roots.length > 3 ? '超出上限' : '符合限制'" />
      </div>
      <div class="card summary-card">
        <span class="summary-label">二级菜单</span>
        <strong :class="{ over: childOverCount > 0 }">{{ childCount }}<em> 个</em></strong>
        <StatusTag :status="childOverCount > 0 ? 'FAILED' : 'SUCCESS'" :text="childOverCount > 0 ? `${childOverCount} 个一级超限` : '每级 ≤ 5'" />
      </div>
      <div class="card summary-card">
        <span class="summary-label">发布目标</span>
        <strong class="account-name">{{ currentAccount?.name || '未选择公众号' }}</strong>
        <span class="muted">{{ currentAccount?.appId || '-' }}</span>
      </div>
    </div>

    <div v-loading="loading" class="menu-grid">
      <div v-for="root in roots" :key="root.id" class="card menu-card">
        <div class="menu-card-head">
          <div class="menu-title">
            <strong>{{ root.title }}</strong>
            <span class="mono">{{ root.path || '无跳转地址' }}</span>
          </div>
          <div class="menu-tags">
            <el-tag size="small" effect="plain">排序 {{ root.sortNo ?? '-' }}</el-tag>
            <el-tag :type="Number(root.visible) === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ Number(root.visible) === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </div>
        </div>

        <div class="child-head">
          <span>二级菜单</span>
          <el-tag :type="childrenOf(root.id).length > 5 ? 'danger' : 'info'" size="small" effect="plain">
            {{ childrenOf(root.id).length }} / 5
          </el-tag>
        </div>

        <div v-if="childrenOf(root.id).length" class="child-list">
          <div v-for="child in childrenOf(root.id)" :key="child.id" class="child-item">
            <span class="child-dot"></span>
            <div class="child-main">
              <strong>{{ child.title }}</strong>
              <span class="mono">{{ child.path || '无跳转地址' }}</span>
            </div>
            <el-tag v-if="Number(child.visible) !== 1" size="small" type="info" effect="plain">隐藏</el-tag>
          </div>
        </div>
        <div v-else class="child-empty">未配置二级菜单，发布后该一级菜单直接跳转 path</div>
      </div>

      <div v-if="!loading && !roots.length" class="card empty-card">
        <el-empty description="暂无公众号菜单配置">
          <el-button type="primary" @click="router.push('/system/menus')">去菜单可视化配置</el-button>
        </el-empty>
      </div>
    </div>

    <div v-if="roots.length" class="card preview-card">
      <div class="preview-head">
        <h3>底部菜单预览</h3>
        <span class="muted">仅展示一级菜单标题，实际跳转以菜单配置中的 path 为准</span>
      </div>
      <div class="wechat-bar">
        <div v-for="root in roots.slice(0, 3)" :key="root.id" class="wechat-item">
          <el-icon><Menu /></el-icon>
          <span>{{ root.title }}</span>
        </div>
        <div v-if="!roots.length" class="muted">暂无菜单</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Promotion, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const menus = ref<any[]>([])
const loading = ref(false)
const publishing = ref(false)
const clearing = ref(false)
const actionError = ref('')

const roots = computed(() =>
  menus.value
    .filter((item) => Number(item.parentId || 0) === 0)
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0))
)

const childCount = computed(() =>
  roots.value.reduce((sum, root) => sum + childrenOf(root.id).length, 0)
)

const childOverCount = computed(() =>
  roots.value.filter((root) => childrenOf(root.id).length > 5).length
)

const currentAccount = computed(() => accounts.value.find((item) => item.id === accountId.value))

onMounted(async () => {
  await loadAccounts()
  await load()
})

function childrenOf(id: number) {
  return menus.value
    .filter((item) => Number(item.parentId) === Number(id))
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0))
}

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
  actionError.value = ''
}

async function load() {
  loading.value = true
  try {
    menus.value = await api.get('/tenant/mp/menu/current')
  } catch {
    menus.value = []
  } finally {
    loading.value = false
  }
}

async function publishMenu() {
  actionError.value = ''
  if (!accountId.value) {
    ElMessage.warning('请先选择公众号账号')
    return
  }
  if (!roots.value.length) {
    ElMessage.warning('请先在菜单可视化中配置公众号菜单')
    return
  }
  if (roots.value.length > 3) {
    ElMessage.error('一级菜单最多 3 个，请先在菜单可视化中调整')
    return
  }
  if (childOverCount.value > 0) {
    ElMessage.error('存在一级菜单下二级菜单超过 5 个，请先在菜单可视化中调整')
    return
  }
  if (!currentAccount.value?.appSecret) {
    actionError.value = '当前公众号未配置 AppSecret，无法发布菜单，请先在账号管理中补齐凭证。'
    ElMessage.error(actionError.value)
    return
  }
  try {
    await ElMessageBox.confirm(
      `将当前菜单配置发布到「${currentAccount.value.name}」？发布会覆盖微信端现有菜单。`,
      '发布菜单',
      { type: 'warning', confirmButtonText: '确认发布', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  publishing.value = true
  try {
    const result = await api.post('/tenant/mp/menu/publish', null, { params: { accountId: accountId.value } })
    await ElMessageBox.alert(
      result?.message || '菜单已发布，需重新进入公众号后可见（微信客户端有缓存）',
      '发布成功',
      { confirmButtonText: '知道了' }
    )
    await load()
  } catch (error: any) {
    actionError.value = error?.response?.data?.message || error?.message || '菜单发布失败'
  } finally {
    publishing.value = false
  }
}

async function clearMenu() {
  actionError.value = ''
  if (!accountId.value) {
    ElMessage.warning('请先选择公众号账号')
    return
  }
  if (!currentAccount.value?.appSecret) {
    actionError.value = '当前公众号未配置 AppSecret，无法清空微信菜单，请先在账号管理中补齐凭证。'
    ElMessage.error(actionError.value)
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认清空「${currentAccount.value.name}」在微信端的自定义菜单？本地菜单配置会保留，可重新发布。`,
      '清空菜单',
      { type: 'warning', confirmButtonText: '确认清空', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  clearing.value = true
  try {
    await api.post('/tenant/mp/menu/clear', null, { params: { accountId: accountId.value } })
    ElMessage.success('微信端菜单已清空，本地配置保留')
  } catch (error: any) {
    actionError.value = error?.response?.data?.message || error?.message || '菜单清空失败'
  } finally {
    clearing.value = false
  }
}
</script>

<style scoped lang="scss">
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1.4fr;
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 12px;

  .summary-label {
    font-size: 12.5px;
    color: var(--ink-2);
  }

  strong {
    font-size: 24px;
    font-weight: 680;
    letter-spacing: -0.02em;

    em { font-size: 12px; font-style: normal; font-weight: 400; color: var(--ink-3); margin-left: 4px; }

    &.over { color: #c0264a; }
  }

  .account-name { font-size: 15px; margin-left: auto; }
  .muted { margin-left: auto; font-size: 11.5px; }
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  min-height: 100px;
}

.menu-card { padding: 18px 20px; }

.menu-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #eef1f5;
}

.menu-title {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong { font-size: 14.5px; font-weight: 650; }
  .mono { font-size: 11px; }
}

.menu-tags {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.child-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 0 8px;
  font-size: 12px;
  color: var(--ink-2);
}

.child-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.child-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fbfcfe;
  border: 1px solid #eef1f5;
  border-radius: 8px;
}

.child-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--brand);
  flex-shrink: 0;
}

.child-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  strong { font-size: 12.5px; font-weight: 550; }
  .mono { font-size: 10.5px; color: var(--ink-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

.child-empty {
  padding: 12px;
  border: 1px dashed #e4e7ec;
  border-radius: 8px;
  font-size: 12px;
  color: var(--ink-3);
  text-align: center;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.empty-card {
  grid-column: 1 / -1;
  padding: 12px;
}

.preview-card {
  margin-top: 16px;
  padding: 18px 20px;
}

.preview-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 10px;

  h3 { margin: 0; font-size: 14px; font-weight: 600; }
  .muted { font-size: 11.5px; }
}

.wechat-bar {
  display: flex;
  border-top: 1px solid var(--line);
  background: #f7f8fa;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
}

.wechat-item {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 12.5px;
  color: #344054;
  border-right: 1px solid #eef1f5;

  &:last-child { border-right: none; }
  .el-icon { color: #667085; }
}

@media (max-width: 900px) {
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
