<template>
  <div class="page">
    <PageHeader title="公众号账号" subtitle="维护服务号基础信息、服务器回调与四个小程序台账；凭证加密存储，列表仅展示掩码。">
      <template #actions>
        <el-select
          v-if="accounts.length"
          v-model="focusId"
          placeholder="选择公众号"
          style="width: 200px"
          @change="handleFocusChange"
        >
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增公众号</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="!loading && !accounts.length"
      title="尚未配置公众号账号"
      description="请先新增公众号并填写 AppID、AppSecret、Token 等凭证；账号管理、小程序台账等本地功能无需微信连通即可使用。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div v-loading="loading" class="account-grid">
      <div
        v-for="item in accounts"
        :key="item.id"
        class="card account-card"
        :class="{ focused: item.id === focusId }"
      >
        <div class="acc-head">
          <div class="acc-title">
            <div class="acc-avatar">{{ (item.name || '公').slice(0, 1) }}</div>
            <div class="acc-name">
              <strong>{{ item.name }}</strong>
              <span class="mono">{{ item.appId }}</span>
            </div>
          </div>
          <StatusTag :status="item.status" />
        </div>

        <div class="acc-meta">
          <div class="meta-row">
            <label>服务器 URL</label>
            <span class="mono ellipsis" :title="item.serverUrl">{{ item.serverUrl || '待生成' }}</span>
            <el-button v-if="item.serverUrl" link type="primary" :icon="CopyDocument" @click="copy(item.serverUrl)" />
          </div>
          <div class="meta-row">
            <label>OAuth 域名</label>
            <span class="ellipsis">{{ item.oauthDomain || '-' }}</span>
          </div>
          <div class="meta-row">
            <label>JS 安全域名</label>
            <span class="ellipsis">{{ item.jsDomain || '-' }}</span>
          </div>
          <div class="meta-row">
            <label>API 配额</label>
            <span>已用 {{ formatNumber(item.apiQuotaUsed) }} 次</span>
          </div>
        </div>

        <div class="cred-row">
          <span :class="{ on: !!item.appSecret }">AppSecret {{ item.appSecret ? '已配置' : '未配置' }}</span>
          <span :class="{ on: !!item.token }">Token {{ item.token ? '已配置' : '未配置' }}</span>
          <span :class="{ on: !!item.aesKey }">AESKey {{ item.aesKey ? '已配置' : '未配置' }}</span>
        </div>

        <div class="acc-actions">
          <el-button size="small" @click="openEdit(item)">编辑</el-button>
          <el-button size="small" @click="openQr(item)">二维码</el-button>
          <el-button size="small" @click="runTakeover(item)">接号检查</el-button>
          <el-button size="small" @click="resetQuota(item)">清空配额</el-button>
          <el-button size="small" type="danger" plain @click="removeAccount(item)">删除</el-button>
        </div>
      </div>

      <div v-if="!loading && !accounts.length" class="card empty-card">
        <el-empty description="暂无公众号账号">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增公众号</el-button>
        </el-empty>
      </div>
    </div>

    <div class="section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">小程序台账</h3>
          <p class="muted">用户端、门店端、供应商端、加盟商端各一条，保存后按类型覆盖更新；AppSecret 留空不修改。</p>
        </div>
        <el-button :icon="Refresh" :loading="miniLoading" @click="loadMiniApps">刷新台账</el-button>
      </div>

      <div class="table-card">
        <el-table v-loading="miniLoading" :data="miniRows" style="width: 100%">
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag effect="plain" size="small">{{ row.typeLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="小程序名称" min-width="150">
            <template #default="{ row }">{{ row.name || '未配置' }}</template>
          </el-table-column>
          <el-table-column prop="appId" label="AppID" min-width="180">
            <template #default="{ row }"><span class="mono">{{ row.appId || '-' }}</span></template>
          </el-table-column>
          <el-table-column label="服务器域名" min-width="220">
            <template #default="{ row }">
              <div class="domain-cell">
                <span class="ellipsis">request：{{ row.requestDomain || '-' }}</span>
                <span class="ellipsis">upload：{{ row.uploadDomain || '-' }}</span>
                <span class="ellipsis">download：{{ row.downloadDomain || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="凭证" width="100">
            <template #default="{ row }">
              <el-tag :type="row.appSecret ? 'success' : 'info'" size="small" effect="light">
                {{ row.appSecret ? '已配置' : '未配置' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMini(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-drawer v-model="formVisible" :title="editingId ? '编辑公众号' : '新增公众号'" size="520px">
      <el-form label-position="top" class="drawer-form">
        <el-form-item label="公众号名称" required>
          <el-input v-model="form.name" maxlength="64" placeholder="例如：悦享便利服务号" />
        </el-form-item>
        <el-form-item label="AppID" required>
          <el-input v-model="form.appId" maxlength="64" placeholder="wx 开头的公众号 AppID" />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input
            v-model="form.appSecret"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="editingId ? '已掩码回显，留空则不修改' : '填写公众号 AppSecret'"
          />
          <div class="form-tip">用于获取 access_token；未配置时粉丝/标签/菜单等微信接口不可用，本地管理不受影响。编辑时掩码仅供识别，如需修改请清空后重新输入完整值。</div>
        </el-form-item>
        <el-form-item label="Token">
          <el-input
            v-model="form.token"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="editingId ? '已掩码回显，留空则不修改' : '服务器配置中的 Token'"
          />
        </el-form-item>
        <el-form-item label="AESKey">
          <el-input
            v-model="form.aesKey"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="editingId ? '已掩码回显，留空则不修改' : '安全模式下的 EncodingAESKey'"
          />
          <div class="form-tip">仅安全模式需要；明文模式下可留空。</div>
        </el-form-item>
        <el-form-item label="服务器 URL（只读）">
          <el-input :model-value="serverUrlPreview" readonly>
            <template #append>
              <el-button v-if="serverUrlPreview.startsWith('http')" :icon="CopyDocument" @click="copy(serverUrlPreview)" />
            </template>
          </el-input>
          <div class="form-tip">由系统根据 AppID 自动生成，请复制到微信公众平台「服务器配置」。</div>
        </el-form-item>
        <el-form-item label="OAuth 授权域名">
          <el-input v-model="form.oauthDomain" maxlength="128" placeholder="例如：h5.example.com" />
        </el-form-item>
        <el-form-item label="JS 安全域名">
          <el-input v-model="form.jsDomain" maxlength="128" placeholder="例如：h5.example.com" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已停用" value="SUSPENDED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAccount">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="miniVisible" :title="`编辑${miniTypeLabel}小程序`" width="560px">
      <el-form label-position="top">
        <el-form-item label="小程序类型">
          <el-input :model-value="miniTypeLabel" disabled />
        </el-form-item>
        <el-form-item label="小程序名称" required>
          <el-input v-model="miniForm.name" maxlength="64" placeholder="例如：悦享便利用户端" />
        </el-form-item>
        <el-form-item label="AppID">
          <el-input v-model="miniForm.appId" maxlength="64" placeholder="小程序 AppID" />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input
            v-model="miniForm.appSecret"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="miniForm.hasSecret ? '已配置，留空则不修改' : '填写小程序 AppSecret'"
          />
        </el-form-item>
        <el-form-item label="request 合法域名">
          <el-input v-model="miniForm.requestDomain" maxlength="128" placeholder="https://api.example.com" />
        </el-form-item>
        <el-form-item label="uploadFile 合法域名">
          <el-input v-model="miniForm.uploadDomain" maxlength="128" placeholder="https://api.example.com" />
        </el-form-item>
        <el-form-item label="downloadFile 合法域名">
          <el-input v-model="miniForm.downloadDomain" maxlength="128" placeholder="https://cdn.example.com" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="miniVisible = false">取消</el-button>
        <el-button type="primary" :loading="miniSaving" @click="saveMiniApp">保存台账</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qrVisible" title="公众号二维码" width="380px" align-center>
      <div class="qr-box">
        <img v-if="qrUrl" :src="qrUrl" alt="公众号二维码" class="qr-img" />
        <p class="qr-name">{{ qrAccountName }}</p>
        <el-input :model-value="qrContent" readonly class="qr-content">
          <template #prepend>内容</template>
        </el-input>
        <a v-if="qrUrl" :href="qrUrl" :download="`${qrAccountName}-qr.png`" class="qr-download">下载二维码图片</a>
      </div>
    </el-dialog>

    <el-dialog v-model="takeoverVisible" title="接号占用检查结果" width="560px">
      <div v-loading="takeoverLoading" class="takeover-box">
        <el-descriptions v-if="takeover" :column="1" border>
          <el-descriptions-item label="公众号">{{ takeoverAccountName }}</el-descriptions-item>
          <el-descriptions-item label="检查时间">{{ formatTime(takeover.checkedAt) }}</el-descriptions-item>
          <el-descriptions-item label="服务器配置">
            <StatusTag :status="takeover.serverUrlConfigured ? 'SUCCESS' : 'PENDING'" :text="takeover.serverUrlConfigured ? '已配置' : '未配置'" />
          </el-descriptions-item>
          <el-descriptions-item label="粉丝活跃">
            <StatusTag :status="takeover.fansActive ? 'SUCCESS' : 'PENDING'" :text="takeover.fansActive ? '存在粉丝' : '暂无粉丝'" />
          </el-descriptions-item>
          <el-descriptions-item label="菜单快照">
            <pre class="json-pre">{{ prettyJson(takeover.menuSnapshot) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="发表快照">
            <pre class="json-pre">{{ prettyJson(takeover.publishSnapshot) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="接号结论">
            <StatusTag :status="takeover.conclusion" />
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ takeover.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button type="primary" @click="takeoverVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Plus, Refresh } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const loading = ref(false)
const accounts = ref<any[]>([])
const focusId = ref<number>()
const route = useRoute()
const router = useRouter()

const miniLoading = ref(false)
const miniApps = ref<any[]>([])
const miniTypes = [
  { type: 'USER', label: '用户端' },
  { type: 'STORE', label: '门店端' },
  { type: 'SUPPLIER', label: '供应商端' },
  { type: 'FRANCHISEE', label: '加盟商端' }
]

const formVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const form = ref<any>(emptyAccount())

const miniVisible = ref(false)
const miniSaving = ref(false)
const miniForm = ref<any>({})
const miniTypeLabel = ref('')

const qrVisible = ref(false)
const qrUrl = ref('')
const qrContent = ref('')
const qrAccountName = ref('')

const takeoverVisible = ref(false)
const takeoverLoading = ref(false)
const takeover = ref<any>(null)
const takeoverAccountName = ref('')

const miniRows = computed(() =>
  miniTypes.map((item) => {
    const exists = miniApps.value.find((app) => app.appType === item.type)
    return {
      ...emptyMini(item.type),
      ...(exists || {}),
      appType: item.type,
      typeLabel: item.label,
      hasSecret: !!exists?.appSecret
    }
  })
)

const serverUrlPreview = computed(() => {
  if (form.value.serverUrl) return form.value.serverUrl
  if (form.value.appId) return `保存后自动生成：/admin-api/mp/open/${form.value.appId}`
  return '保存后由系统根据 AppID 自动生成'
})

onMounted(loadAll)

async function loadAll() {
  await Promise.all([loadAccounts(), loadMiniApps()])
}

async function loadAccounts() {
  loading.value = true
  try {
    accounts.value = await api.get('/tenant/mp/accounts')
    const queryId = Number(route.query.accountId)
    if (queryId && accounts.value.some((item) => item.id === queryId)) {
      focusId.value = queryId
    } else if (!focusId.value || !accounts.value.some((item) => item.id === focusId.value)) {
      focusId.value = accounts.value[0]?.id
    }
  } catch {
    accounts.value = []
  } finally {
    loading.value = false
  }
}

function handleFocusChange(id: number) {
  router.replace({ query: { ...route.query, accountId: String(id) } })
}

async function loadMiniApps() {
  miniLoading.value = true
  try {
    miniApps.value = await api.get('/tenant/mp/accounts/mini-apps')
  } catch {
    miniApps.value = []
  } finally {
    miniLoading.value = false
  }
}

function emptyAccount() {
  return {
    name: '',
    appId: '',
    appSecret: '',
    token: '',
    aesKey: '',
    oauthDomain: '',
    jsDomain: '',
    status: 'ACTIVE',
    serverUrl: ''
  }
}

function emptyMini(appType: string) {
  return {
    appType,
    name: '',
    appId: '',
    appSecret: '',
    requestDomain: '',
    uploadDomain: '',
    downloadDomain: ''
  }
}

function openCreate() {
  editingId.value = undefined
  form.value = emptyAccount()
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = {
    name: row.name || '',
    appId: row.appId || '',
    appSecret: row.appSecret || '',
    token: row.token || '',
    aesKey: row.aesKey || '',
    oauthDomain: row.oauthDomain || '',
    jsDomain: row.jsDomain || '',
    status: row.status || 'ACTIVE',
    serverUrl: row.serverUrl || ''
  }
  formVisible.value = true
}

async function saveAccount() {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请填写公众号名称')
    return
  }
  if (!form.value.appId?.trim()) {
    ElMessage.warning('请填写 AppID')
    return
  }
  saving.value = true
  const payload: any = { ...form.value }
  if (!editingId.value) delete payload.serverUrl
  try {
    if (editingId.value) {
      await api.put(`/tenant/mp/accounts/${editingId.value}`, payload)
      ElMessage.success('公众号已更新')
    } else {
      await api.post('/tenant/mp/accounts', payload)
      ElMessage.success('公众号已创建')
    }
    formVisible.value = false
    await loadAccounts()
  } finally {
    saving.value = false
  }
}

function openMini(row: any) {
  miniTypeLabel.value = row.typeLabel
  miniForm.value = {
    appType: row.appType,
    name: row.name || '',
    appId: row.appId || '',
    appSecret: '',
    requestDomain: row.requestDomain || '',
    uploadDomain: row.uploadDomain || '',
    downloadDomain: row.downloadDomain || '',
    hasSecret: row.hasSecret
  }
  miniVisible.value = true
}

async function saveMiniApp() {
  if (!miniForm.value.name?.trim()) {
    ElMessage.warning('请填写小程序名称')
    return
  }
  miniSaving.value = true
  try {
    const { hasSecret, ...payload } = miniForm.value
    await api.post('/tenant/mp/accounts/mini-apps', payload)
    ElMessage.success(`${miniTypeLabel.value}小程序台账已保存`)
    miniVisible.value = false
    await loadMiniApps()
  } finally {
    miniSaving.value = false
  }
}

async function openQr(row: any) {
  qrAccountName.value = row.name
  qrContent.value = ''
  qrUrl.value = ''
  qrVisible.value = true
  try {
    const result = await api.get(`/tenant/mp/accounts/${row.id}/qr-content`)
    qrContent.value = result.content || ''
    qrUrl.value = await QRCode.toDataURL(qrContent.value, {
      width: 300,
      margin: 1,
      color: { dark: '#101828', light: '#ffffff' }
    })
  } catch {
    qrVisible.value = false
  }
}

async function runTakeover(row: any) {
  takeoverAccountName.value = row.name
  takeover.value = null
  takeoverVisible.value = true
  takeoverLoading.value = true
  try {
    takeover.value = await api.post(`/tenant/mp/accounts/${row.id}/takeover-check`)
    ElMessage.success('接号检查完成')
  } finally {
    takeoverLoading.value = false
  }
}

async function resetQuota(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认清空「${row.name}」的 API 配额用量？清空后无法恢复，不影响公众号实际调用能力。`,
      '清空 API 配额',
      { type: 'warning', confirmButtonText: '确认清空', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await api.post(`/tenant/mp/accounts/${row.id}/reset-quota`)
  ElMessage.success('API 配额已清空')
  await loadAccounts()
}

async function removeAccount(row: any) {
  try {
    await ElMessageBox.confirm(
      `删除公众号「${row.name}」后不可恢复，其粉丝、消息、草稿等历史数据不会被自动删除。确认删除？`,
      '删除公众号',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await api.del(`/tenant/mp/accounts/${row.id}`)
  ElMessage.success('公众号已删除')
  await loadAccounts()
}

async function copy(text?: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function prettyJson(value: any) {
  if (!value) return '-'
  try {
    return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2)
  } catch {
    return String(value)
  }
}

function formatNumber(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '刚刚'
}
</script>

<style scoped lang="scss">
.account-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 14px;
  min-height: 120px;
}

.account-card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;

  &.focused {
    border-color: #b9d3fa;
    box-shadow: 0 0 0 3px rgba(31, 111, 235, 0.08), var(--shadow);
  }
}

.acc-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.acc-title {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.acc-avatar {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1f6feb, #14b8a6);
  color: #fff;
  font-weight: 650;
  flex-shrink: 0;
}

.acc-name {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong { font-size: 15px; font-weight: 650; }
  span { font-size: 11.5px; color: var(--ink-3); overflow: hidden; text-overflow: ellipsis; }
}

.acc-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px dashed #eef1f5;
  border-bottom: 1px dashed #eef1f5;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;

  label {
    width: 88px;
    color: var(--ink-3);
    flex-shrink: 0;
  }

  span { color: #344054; min-width: 0; }
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11.5px;
}

.cred-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  span {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 999px;
    background: #f2f4f7;
    color: #98a2b3;

    &.on { background: #e8f7ef; color: #0e7a45; }
  }
}

.acc-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  .el-button + .el-button { margin-left: 0; }
}

.empty-card {
  grid-column: 1 / -1;
  padding: 10px;
}

.section-block { margin-top: 22px; }

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;

  p { margin: 4px 0 0; font-size: 12px; }
}

.domain-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11.5px;
  color: var(--ink-2);
}

.drawer-form { padding-right: 4px; }

.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-img {
  width: 240px;
  height: 240px;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 8px;
  background: #fff;
}

.qr-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.qr-content { width: 100%; }

.qr-download {
  font-size: 12.5px;
  color: var(--brand);
}

.takeover-box { min-height: 120px; }

.json-pre {
  margin: 0;
  max-height: 120px;
  overflow: auto;
  font-size: 11.5px;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--ink-2);
}
</style>
