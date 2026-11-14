<template>
  <div class="page">
    <PageHeader title="消息配置" subtitle="统一管理消息通道、短信服务商与触发节点模板，配置保存后立即生效。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="card card-pad section-card" v-loading="loading">
      <div class="section-head">
        <div>
          <h3>消息通道</h3>
          <p>关闭通道后，对应节点的消息将不再发送，模板保持不变。</p>
        </div>
        <el-tag effect="plain" round>{{ enabledChannelCount }} / {{ channelMeta.length }} 已启用</el-tag>
      </div>
      <div class="channel-grid">
        <div v-for="meta in channelMeta" :key="meta.code" class="channel-card" :class="{ on: channelEnabled(meta.code) }">
          <div class="channel-top">
            <span class="channel-icon">
              <el-icon><component :is="meta.icon" /></el-icon>
            </span>
            <el-switch
              :model-value="channelEnabled(meta.code)"
              :loading="channelLoading === meta.code"
              @change="(val: any) => toggleChannel(meta.code, val)"
            />
          </div>
          <strong>{{ meta.name }}</strong>
          <p>{{ meta.desc }}</p>
          <div class="channel-foot">
            <StatusTag :status="channelEnabled(meta.code) ? 'ACTIVE' : 'DISABLED'" />
            <span>{{ meta.code }}</span>
          </div>
        </div>
        <el-empty v-if="!channelMeta.length" description="暂无通道" :image-size="60" />
      </div>
    </div>

    <div class="card card-pad section-card">
      <div class="section-head">
        <div>
          <h3>短信渠道</h3>
          <p>同一租户仅允许启用一个短信服务商；LOCAL 渠道将验证码写入服务端日志，用于开发联调。</p>
        </div>
      </div>

      <el-tabs v-model="activeProvider" tab-position="left" class="sms-tabs" @tab-change="onProviderChange">
        <el-tab-pane v-for="provider in smsMeta" :key="provider.code" :name="provider.code">
          <template #label>
            <span class="provider-label">
              {{ provider.name }}
              <el-tag v-if="smsEnabled(provider.code)" size="small" type="success" effect="plain">启用中</el-tag>
            </span>
          </template>

          <div class="provider-head">
            <div>
              <h4>{{ provider.name }}</h4>
              <p>{{ provider.desc }}</p>
            </div>
            <el-tag v-if="lastTestAt(provider.code)" effect="plain" type="info" round>
              最近测试 {{ formatTime(lastTestAt(provider.code)) }}
            </el-tag>
          </div>

          <el-form label-position="top" class="sms-form">
            <el-form-item label="渠道配置（JSON）">
              <el-input
                v-model="smsForm.config"
                type="textarea"
                :rows="7"
                spellcheck="false"
                placeholder='请输入 JSON 配置，例如 {"accessKeyId":"","accessKeySecret":"","signName":""}'
                @input="clearTestResult"
              />
              <div class="form-tip">按服务商要求填写，密钥仅保存在服务端，请勿在浏览器中缓存。</div>
            </el-form-item>
            <el-form-item label="启用该渠道">
              <div class="switch-line">
                <el-switch v-model="smsForm.enable" />
                <span>启用后将自动停用其他短信服务商</span>
              </div>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="testResult"
            :title="testResult.message"
            :type="testResult.success ? 'success' : 'error'"
            :closable="false"
            show-icon
            class="test-alert"
          />

          <div class="sms-actions">
            <el-button :loading="testingProvider" :icon="Connection" @click="testSms">测试连接</el-button>
            <el-button type="primary" :loading="savingSms" :icon="Check" @click="saveSms">保存配置</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="card section-card">
      <div class="toolbar">
        <div>
          <h3 class="toolbar-title">消息模板</h3>
          <p class="toolbar-sub">字段映射格式：thing1=xxx,time2=xxx，需要与厂商模板字段一致。</p>
        </div>
        <span class="spacer"></span>
        <el-button type="primary" :icon="Plus" @click="openTemplate()">新增模板</el-button>
      </div>
      <el-table :data="templates" stripe>
        <el-table-column prop="node" label="触发节点" min-width="140">
          <template #default="{ row }">
            <span class="node-pill">{{ nodeLabel(row.node) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="通道" width="150">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ channelName(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="模板标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="templateId" label="厂商模板 ID" min-width="150" show-overflow-tooltip />
        <el-table-column prop="fieldMapping" label="字段映射" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.fieldMapping" class="mono">{{ row.fieldMapping }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="Number(row.enabled) === 1 ? 'ACTIVE' : 'DISABLED'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="testTemplate(row)">测试</el-button>
            <el-button link type="primary" @click="openTemplate(row)">编辑</el-button>
            <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无消息模板，点击右上角新增" :image-size="90" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="templateVisible" :title="editingTemplate ? '编辑消息模板' : '新增消息模板'" width="560px" destroy-on-close>
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="108px">
        <el-form-item label="触发节点" prop="node">
          <el-select v-model="templateForm.node" filterable allow-create default-first-option placeholder="选择或输入节点" style="width: 100%">
            <el-option v-for="node in nodeOptions" :key="node.value" :label="`${node.label}（${node.value}）`" :value="node.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息通道" prop="channel">
          <el-select v-model="templateForm.channel" placeholder="选择通道" style="width: 100%">
            <el-option v-for="meta in channelMeta" :key="meta.code" :label="meta.name" :value="meta.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板标题" prop="title">
          <el-input v-model="templateForm.title" placeholder="如 订单支付成功通知" maxlength="60" />
        </el-form-item>
        <el-form-item label="厂商模板 ID">
          <el-input v-model="templateForm.templateId" placeholder="微信模板 ID / 短信模板 CODE" maxlength="80" />
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="templateForm.content" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="支持 {{thing1}} 变量" />
        </el-form-item>
        <el-form-item label="字段映射" prop="fieldMapping">
          <el-input v-model="templateForm.fieldMapping" placeholder="thing1=orderNo,time2=paidAt" />
          <div class="form-tip">多个字段用英文逗号分隔，等号左侧必须为字母、数字或下划线。</div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="templateForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, ChatLineSquare, Check, Connection, Iphone, Message, Plus, Refresh
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const channelMeta = [
  { code: 'MP_TEMPLATE', name: '公众号模板消息', desc: '服务号模板消息，适合订单状态与售后通知。', icon: ChatLineSquare },
  { code: 'MINI_SUBSCRIBE', name: '小程序订阅消息', desc: '用户授权后下发，需提前配置订阅模板。', icon: Bell },
  { code: 'APP_PUSH', name: 'App 推送', desc: '面向 App 用户的系统级推送通道。', icon: Iphone },
  { code: 'INNER', name: '站内信', desc: '后台与用户中心的站内消息，不依赖外部服务。', icon: Message }
]

const smsMeta = [
  { code: 'ALIYUN', name: '阿里云', desc: '阿里云短信服务，填写 AccessKey 与签名模板。' },
  { code: 'TENCENT', name: '腾讯云', desc: '腾讯云短信 SMS，填写 SecretId / SecretKey。' },
  { code: 'HUAWEI', name: '华为云', desc: '华为云消息通知服务，填写 AK / SK 与签名。' },
  { code: 'JDCLOUD', name: '京东云', desc: '京东云短信服务，填写 AccessKey 与模板。' },
  { code: 'AWS_SNS', name: 'AWS SNS', desc: 'AWS 简单通知服务，适用于海外短信场景。' },
  { code: 'LOCAL', name: '本地日志', desc: '开发联调渠道，验证码写入服务端日志，不真实发送。' }
]

const nodeOptions = [
  { label: '订单创建', value: 'ORDER_CREATED' },
  { label: '支付成功', value: 'ORDER_PAID' },
  { label: '订单发货', value: 'ORDER_SHIPPED' },
  { label: '待取货提醒', value: 'ORDER_PICKUP' },
  { label: '订单完成', value: 'ORDER_COMPLETED' },
  { label: '退款成功', value: 'REFUND_SUCCESS' },
  { label: '会员注册', value: 'MEMBER_REGISTER' },
  { label: '会员生日', value: 'BIRTHDAY' },
  { label: '充值成功', value: 'RECHARGE_SUCCESS' },
  { label: '优惠券到期', value: 'COUPON_EXPIRE' },
  { label: '库存预警', value: 'STOCK_WARNING' }
]

const loading = ref(false)
const channelLoading = ref('')
const testingProvider = ref(false)
const savingSms = ref(false)
const submitting = ref(false)
const channels = ref<any[]>([])
const smsChannels = ref<any[]>([])
const templates = ref<any[]>([])
const activeProvider = ref('ALIYUN')
const smsForm = ref({ config: '', enable: false })
const testResult = ref<{ success: boolean; message: string } | null>(null)
const templateVisible = ref(false)
const editingTemplate = ref<any>(null)
const templateFormRef = ref()
const templateForm = ref<any>({
  node: '',
  channel: 'MP_TEMPLATE',
  templateId: '',
  title: '',
  content: '',
  fieldMapping: '',
  enabled: 1
})

const enabledChannelCount = computed(() => channelMeta.filter((item) => channelEnabled(item.code)).length)

const templateRules: any = {
  node: [{ required: true, message: '请选择触发节点', trigger: 'change' }],
  channel: [{ required: true, message: '请选择消息通道', trigger: 'change' }],
  title: [{ required: true, message: '请输入模板标题', trigger: 'blur' }],
  fieldMapping: [
    {
      validator(_rule: any, value: string, callback: (error?: Error) => void) {
        if (!value) return callback()
        const ok = /^[A-Za-z0-9_]+=.+(,[A-Za-z0-9_]+=.+)*$/.test(value)
        callback(ok ? undefined : new Error('格式错误，示例：thing1=xxx,time2=xxx'))
      },
      trigger: 'blur'
    }
  ]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await api.get('/tenant/system/messages')
    channels.value = data?.channels || []
    smsChannels.value = data?.smsChannels || []
    templates.value = data?.templates || []
    const enabled = smsChannels.value.find((item) => Number(item.enabled) === 1)
    if (enabled) activeProvider.value = enabled.provider
    fillProviderForm()
  } catch {
    channels.value = []
    smsChannels.value = []
    templates.value = []
  } finally {
    loading.value = false
  }
}

function channelEnabled(code: string) {
  return Number(channels.value.find((item) => item.channel === code)?.enabled) === 1
}

function channelName(code: string) {
  return channelMeta.find((item) => item.code === code)?.name || code || '-'
}

function nodeLabel(node: string) {
  return nodeOptions.find((item) => item.value === node)?.label || node || '-'
}

async function toggleChannel(code: string, value: any) {
  const enabled = value ? 1 : 0
  if (!enabled) {
    try {
      await ElMessageBox.confirm(`关闭「${channelName(code)}」后，该通道的消息将不再发送，确定关闭吗？`, '关闭通道', {
        type: 'warning',
        confirmButtonText: '关闭',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  channelLoading.value = code
  try {
    await api.put(`/tenant/system/messages/channels/${code}?enabled=${enabled}`, null)
    const target = channels.value.find((item) => item.channel === code)
    if (target) target.enabled = enabled
    else channels.value.push({ channel: code, enabled })
    ElMessage.success(enabled ? '通道已启用' : '通道已关闭')
  } finally {
    channelLoading.value = ''
  }
}

function smsEnabled(code: string) {
  return Number(smsChannels.value.find((item) => item.provider === code)?.enabled) === 1
}

function lastTestAt(code: string) {
  return smsChannels.value.find((item) => item.provider === code)?.lastTestAt
}

function formatTime(value: any) {
  return value ? dayjs(value).format('MM-DD HH:mm') : ''
}

function fillProviderForm() {
  const target = smsChannels.value.find((item) => item.provider === activeProvider.value)
  smsForm.value = {
    config: target?.config || '',
    enable: Number(target?.enabled) === 1
  }
  testResult.value = null
}

function onProviderChange() {
  fillProviderForm()
}

function clearTestResult() {
  testResult.value = null
}

function parseConfig(): string | null {
  const text = smsForm.value.config.trim()
  if (!text) return ''
  try {
    JSON.parse(text)
    return text
  } catch {
    ElMessage.warning('渠道配置需为合法 JSON')
    return null
  }
}

async function testSms() {
  const config = parseConfig()
  if (config === null) return
  testingProvider.value = true
  testResult.value = null
  try {
    const result = await api.post(`/tenant/system/messages/sms/${activeProvider.value}/test`, { config })
    testResult.value = { success: !!result?.success, message: result?.message || '测试完成' }
    if (result?.success) ElMessage.success('测试通过')
  } catch (error: any) {
    testResult.value = {
      success: false,
      message: error?.response?.data?.message || error?.message || '测试失败，请检查配置'
    }
  } finally {
    testingProvider.value = false
  }
}

async function saveSms() {
  const config = parseConfig()
  if (config === null) return
  if (smsForm.value.enable && !config.trim()) {
    ElMessage.warning('启用短信渠道前请先填写渠道配置')
    return
  }
  if (smsForm.value.enable) {
    try {
      await ElMessageBox.confirm('同一租户只能启用一个短信渠道，启用后将自动停用其他渠道，是否继续？', '启用确认', {
        type: 'warning',
        confirmButtonText: '启用',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  savingSms.value = true
  try {
    await api.put(`/tenant/system/messages/sms/${activeProvider.value}`, {
      config,
      enable: smsForm.value.enable
    })
    ElMessage.success('短信渠道配置已保存')
    await load()
  } finally {
    savingSms.value = false
  }
}

function openTemplate(row?: any) {
  editingTemplate.value = row || null
  templateForm.value = row
    ? { ...row }
    : { node: '', channel: 'MP_TEMPLATE', templateId: '', title: '', content: '', fieldMapping: '', enabled: 1 }
  templateVisible.value = true
}

async function submitTemplate() {
  const valid = await templateFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingTemplate.value) {
      await api.put(`/tenant/system/messages/templates/${editingTemplate.value.id}`, templateForm.value)
    } else {
      await api.post('/tenant/system/messages/templates', templateForm.value)
    }
    ElMessage.success('模板已保存')
    templateVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function testTemplate(row: any) {
  try {
    await ElMessageBox.confirm(
      `将向当前通道发送一条「${row.title || row.node}」测试消息，是否继续？`,
      '测试消息',
      { type: 'info', confirmButtonText: '发送', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await api.post(`/tenant/system/messages/templates/${row.id}/test`)
  ElMessage.success(result?.message || '测试消息已提交')
}

async function removeTemplate(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.title || row.node}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/system/messages/templates/${row.id}`)
  ElMessage.success('模板已删除')
  await load()
}
</script>

<style scoped lang="scss">
.section-card {
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 650;
  }
  p {
    margin: 4px 0 0;
    font-size: 11.5px;
    color: var(--ink-3);
  }
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
}

.channel-card {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 14px;
  background: #fbfcfe;
  transition: all 0.18s;

  &.on {
    border-color: #b9d3fa;
    background: #f5f9ff;
    box-shadow: 0 6px 16px rgba(31, 111, 235, 0.08);
  }

  strong {
    display: block;
    margin-top: 10px;
    font-size: 13px;
    font-weight: 650;
  }
  p {
    margin: 5px 0 10px;
    font-size: 11.5px;
    color: var(--ink-3);
    line-height: 1.55;
    min-height: 36px;
  }
}

.channel-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.channel-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--brand-soft);
  color: var(--brand);
  display: grid;
  place-items: center;
}

.channel-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px dashed var(--line);
  span {
    font-size: 10.5px;
    color: var(--ink-3);
    font-family: 'SFMono-Regular', Consolas, monospace;
  }
}

.sms-tabs {
  :deep(.el-tabs__header) {
    width: 190px;
    margin-right: 10px;
  }
  :deep(.el-tabs__item) {
    justify-content: flex-start;
    height: 44px;
    padding: 0 14px;
  }
  :deep(.el-tabs__content) {
    padding: 4px 8px 4px 16px;
  }
}

.provider-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.provider-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 12px;
  margin-bottom: 14px;
  border-bottom: 1px dashed var(--line);

  h4 {
    margin: 0;
    font-size: 15px;
    font-weight: 650;
  }
  p {
    margin: 4px 0 0;
    font-size: 11.5px;
    color: var(--ink-3);
  }
}

.sms-form {
  max-width: 620px;
  :deep(textarea) {
    font-family: 'SFMono-Regular', Consolas, monospace;
    font-size: 12px;
  }
}

.switch-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ink-3);
}

.test-alert {
  max-width: 620px;
  margin-bottom: 14px;
}

.sms-actions {
  display: flex;
  gap: 8px;
}

.toolbar-title {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
}

.toolbar-sub {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--ink-3);
}

.node-pill {
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
  color: #475467;
}
</style>
