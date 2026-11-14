<template>
  <div class="page">
    <PageHeader title="对象存储" subtitle="选择文件存储驱动并完成连接测试，保存后全站上传立即切换。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="card card-pad" v-loading="loading">
      <div class="section-head">
        <div>
          <h3>存储驱动</h3>
          <p>同一时间仅启用一种驱动，切换驱动需二次确认并重新测试连接。</p>
        </div>
        <div class="head-meta">
          <el-tag v-if="savedDriver" effect="plain" round>当前生效：{{ driverName(savedDriver) }}</el-tag>
          <span v-if="lastTestAt" class="muted">最近保存 {{ formatTime(lastTestAt) }}</span>
        </div>
      </div>

      <div class="driver-grid">
        <button
          v-for="driver in drivers"
          :key="driver.code"
          type="button"
          class="driver-card"
          :class="{ active: currentDriver === driver.code }"
          @click="chooseDriver(driver.code)"
        >
          <div class="driver-top">
            <span class="driver-icon"><el-icon><component :is="driver.icon" /></el-icon></span>
            <el-icon v-if="currentDriver === driver.code" class="check"><CircleCheckFilled /></el-icon>
          </div>
          <strong>{{ driver.name }}</strong>
          <p>{{ driver.desc }}</p>
          <div class="driver-foot">
            <span class="driver-code">{{ driver.code }}</span>
            <el-tag v-if="savedDriver === driver.code" size="small" type="success" effect="plain">使用中</el-tag>
          </div>
        </button>
      </div>

      <div v-if="currentDriver" class="config-area">
        <div class="config-head">
          <div>
            <h3>{{ currentDriverMeta?.name }} 配置</h3>
            <p>{{ currentDriverMeta?.hint }}</p>
          </div>
          <StatusTag :status="tested ? 'SUCCESS' : 'PENDING'" :text="tested ? '连接测试通过' : '待测试'" />
        </div>

        <el-alert
          v-if="currentDriver === 'OSS' || currentDriver === 'COS'"
          type="info"
          show-icon
          :closable="false"
          class="driver-warning"
          :title="`已集成 ${currentDriver} 官方 SDK，需填写有效的 AK/SK、Bucket 与地域；连接测试会真实请求云厂商接口。`"
        />

        <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" class="storage-form">
          <el-row :gutter="20">
            <el-col v-for="field in currentFields" :key="field.key" :xs="24" :lg="12">
              <el-form-item :label="field.label" :prop="field.key">
                <el-input
                  v-if="field.secret"
                  v-model="form[field.key]"
                  type="password"
                  show-password
                  :placeholder="field.placeholder"
                  autocomplete="new-password"
                />
                <el-input v-else v-model="form[field.key]" :placeholder="field.placeholder" clearable />
                <div class="form-tip">{{ field.tip }}</div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <el-alert
          v-if="testResult"
          :title="testResult.message"
          :type="testResult.success ? 'success' : 'error'"
          :closable="false"
          show-icon
          class="test-result"
        />

        <div class="storage-actions">
          <el-button :icon="Connection" :loading="testing" @click="testConnection">测试连接</el-button>
          <el-tooltip :disabled="tested" content="请先通过连接测试后再保存" placement="top">
            <span>
              <el-button type="primary" :icon="Check" :disabled="!tested" :loading="saving" @click="save">
                保存并应用
              </el-button>
            </span>
          </el-tooltip>
          <span class="save-tip">{{ tested ? '连接正常，可以保存并切换驱动' : '修改配置后需重新测试' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Box, Check, CircleCheckFilled, Cloudy, Connection, Files, FolderOpened, Refresh, UploadFilled
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

interface StorageField {
  key: string
  label: string
  placeholder: string
  tip: string
  secret?: boolean
  required?: boolean
}

const drivers = [
  {
    code: 'LOCAL',
    name: '本地磁盘',
    desc: '文件保存到服务器本地目录，适合单机部署与快速上线。',
    icon: FolderOpened,
    hint: '本地存储无需外部依赖，请确保目录可写并配置可访问的公开地址。',
    fields: [
      { key: 'rootPath', label: '存储根目录', placeholder: './data/uploads', tip: '服务器绝对路径或相对路径', required: true },
      { key: 'publicBaseUrl', label: '公开访问地址', placeholder: '/uploads', tip: '前台访问文件的前缀，可由 Nginx 代理' }
    ] as StorageField[]
  },
  {
    code: 'S3',
    name: 'Amazon S3',
    desc: '兼容 S3 协议的对象存储，适合海外部署与跨区域容灾。',
    icon: UploadFilled,
    hint: '填写 S3 兼容端点、访问密钥与桶名称，Region 可按服务商要求填写。',
    fields: [
      { key: 'endpoint', label: 'Endpoint', placeholder: 'https://s3.amazonaws.com', tip: '包含协议，如 https://s3.ap-east-1.amazonaws.com', required: true },
      { key: 'accessKey', label: 'Access Key', placeholder: '请输入 Access Key', tip: '密钥以掩码展示，未变更可直接保存', secret: true, required: true },
      { key: 'secretKey', label: 'Secret Key', placeholder: '请输入 Secret Key', tip: '密钥仅保存在服务端', secret: true, required: true },
      { key: 'bucket', label: 'Bucket', placeholder: 'retail-files', tip: '桶需已存在或允许自动创建', required: true },
      { key: 'region', label: 'Region', placeholder: 'ap-east-1', tip: '部分服务商可留空' },
      { key: 'publicBaseUrl', label: '公开访问地址', placeholder: 'https://cdn.example.com', tip: 'CDN 或桶的公网访问前缀' }
    ] as StorageField[]
  },
  {
    code: 'OSS',
    name: '阿里云 OSS',
    desc: '阿里云对象存储，适合国内业务与按量计费场景。',
    icon: Cloudy,
    hint: '填写阿里云 OSS Endpoint、AccessKey、Bucket 与地域，连接测试会校验 Bucket 访问权限。',
    fields: [
      { key: 'endpoint', label: 'Endpoint', placeholder: 'https://oss-cn-shanghai.aliyuncs.com', tip: 'OSS 地域节点地址', required: true },
      { key: 'accessKey', label: 'AccessKey', placeholder: '请输入 AccessKey', tip: '密钥以掩码展示', secret: true, required: true },
      { key: 'secretKey', label: 'SecretKey', placeholder: '请输入 SecretKey', tip: '密钥仅保存在服务端', secret: true, required: true },
      { key: 'bucket', label: 'Bucket', placeholder: 'retail-files', tip: 'OSS Bucket 名称', required: true },
      { key: 'region', label: 'Region', placeholder: 'cn-shanghai', tip: '地域 ID' },
      { key: 'publicBaseUrl', label: '公开访问地址', placeholder: 'https://cdn.example.com', tip: 'CDN 或 Bucket 公网域名' }
    ] as StorageField[]
  },
  {
    code: 'COS',
    name: '腾讯云 COS',
    desc: '腾讯云对象存储，适合微信生态与华南业务部署。',
    icon: Files,
    hint: '填写腾讯云 COS 地域、SecretId/Key 与 Bucket（含 APPID），连接测试会校验 Bucket 访问权限。',
    fields: [
      { key: 'endpoint', label: 'Endpoint', placeholder: 'https://cos.ap-guangzhou.myqcloud.com', tip: 'COS 地域节点地址', required: true },
      { key: 'accessKey', label: 'SecretId', placeholder: '请输入 SecretId', tip: '密钥以掩码展示', secret: true, required: true },
      { key: 'secretKey', label: 'SecretKey', placeholder: '请输入 SecretKey', tip: '密钥仅保存在服务端', secret: true, required: true },
      { key: 'bucket', label: 'Bucket', placeholder: 'retail-1250000000', tip: '含 APPID 的 Bucket 名称', required: true },
      { key: 'region', label: 'Region', placeholder: 'ap-guangzhou', tip: '地域 ID' },
      { key: 'publicBaseUrl', label: '公开访问地址', placeholder: 'https://cdn.example.com', tip: 'CDN 加速域名' }
    ] as StorageField[]
  },
  {
    code: 'MINIO',
    name: 'MinIO',
    desc: '私有化对象存储，兼容 S3 协议，适合内网与信创部署。',
    icon: Box,
    hint: '填写 MinIO 服务地址与访问密钥，建议提供 HTTPS 端点。',
    fields: [
      { key: 'endpoint', label: 'Endpoint', placeholder: 'https://minio.example.com:9000', tip: 'MinIO 服务地址，包含协议与端口', required: true },
      { key: 'accessKey', label: 'Access Key', placeholder: '请输入 Access Key', tip: '密钥以掩码展示', secret: true, required: true },
      { key: 'secretKey', label: 'Secret Key', placeholder: '请输入 Secret Key', tip: '密钥仅保存在服务端', secret: true, required: true },
      { key: 'bucket', label: 'Bucket', placeholder: 'retail-files', tip: '桶需已存在或允许自动创建', required: true },
      { key: 'region', label: 'Region', placeholder: 'us-east-1', tip: 'MinIO 默认 us-east-1，可留空' },
      { key: 'publicBaseUrl', label: '公开访问地址', placeholder: 'https://files.example.com', tip: '前台文件访问前缀' }
    ] as StorageField[]
  }
]

const loading = ref(false)
const testing = ref(false)
const saving = ref(false)
const tested = ref(false)
const currentDriver = ref('LOCAL')
const savedDriver = ref('')
const lastTestAt = ref('')
const form = ref<Record<string, string>>({})
const testResult = ref<{ success: boolean; message: string } | null>(null)
const formRef = ref()
let hydrating = false

const currentDriverMeta = computed(() => drivers.find((item) => item.code === currentDriver.value))
const currentFields = computed(() => currentDriverMeta.value?.fields || [])

const rules = computed<Record<string, any[]>>(() => {
  const map: Record<string, any[]> = {}
  for (const field of currentFields.value) {
    if (field.required) {
      map[field.key] = [{ required: true, message: `请填写${field.label}`, trigger: 'blur' }]
    }
  }
  return map
})

watch(
  form,
  () => {
    if (hydrating) return
    tested.value = false
    testResult.value = null
  },
  { deep: true }
)

onMounted(load)

function driverName(code: string) {
  return drivers.find((item) => item.code === code)?.name || code
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function emptyForm(code: string) {
  const map: Record<string, string> = {}
  const meta = drivers.find((item) => item.code === code)
  for (const field of meta?.fields || []) map[field.key] = ''
  return map
}

async function load() {
  loading.value = true
  hydrating = true
  try {
    const data = await api.get('/tenant/system/storage')
    savedDriver.value = data?.driver || ''
    currentDriver.value = data?.driver || 'LOCAL'
    lastTestAt.value = data?.lastTestAt || ''
    form.value = { ...emptyForm(currentDriver.value), ...(data?.config || {}) }
    tested.value = false
    testResult.value = null
  } catch {
    currentDriver.value = 'LOCAL'
    form.value = emptyForm('LOCAL')
  } finally {
    hydrating = false
    loading.value = false
  }
}

async function chooseDriver(code: string) {
  if (code === currentDriver.value) return
  try {
    await ElMessageBox.confirm(
      `切换存储驱动后需重新测试连接，测试通过才能保存。确定切换到「${driverName(code)}」吗？`,
      '切换驱动',
      { type: 'warning', confirmButtonText: '切换', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  hydrating = true
  currentDriver.value = code
  form.value = emptyForm(code)
  tested.value = false
  testResult.value = null
  hydrating = false
  formRef.value?.clearValidate()
}

async function testConnection() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  testing.value = true
  tested.value = false
  testResult.value = null
  try {
    const result = await api.post('/tenant/system/storage/test', {
      driver: currentDriver.value,
      config: form.value
    })
    const success = !!result?.success
    tested.value = success
    testResult.value = {
      success,
      message: result?.message || (success ? '连接测试通过' : '连接测试未通过')
    }
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '连接测试失败'
    testResult.value = { success: false, message }
  } finally {
    testing.value = false
  }
}

async function save() {
  if (!tested.value) {
    ElMessage.warning('请先通过连接测试')
    return
  }
  if (savedDriver.value && savedDriver.value !== currentDriver.value) {
    try {
      await ElMessageBox.confirm(
        `保存后全站文件将切换为「${driverName(currentDriver.value)}」，确定应用吗？`,
        '切换存储驱动',
        { type: 'warning', confirmButtonText: '保存并应用', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  saving.value = true
  try {
    await api.post('/tenant/system/storage/save', {
      driver: currentDriver.value,
      config: form.value
    })
    ElMessage.success('存储配置已保存并应用')
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;

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

.head-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  font-size: 11.5px;
}

.driver-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.driver-card {
  border: 1px solid var(--line);
  background: #fbfcfe;
  border-radius: 10px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  min-height: 150px;
  display: flex;
  flex-direction: column;
  transition: all 0.18s;
  font-family: inherit;

  &:hover {
    border-color: #b9d3fa;
    transform: translateY(-1px);
  }

  &.active {
    border-color: var(--brand);
    background: #f5f9ff;
    box-shadow: 0 8px 20px rgba(31, 111, 235, 0.12);
  }

  strong {
    font-size: 13px;
    font-weight: 650;
    color: var(--ink);
  }
  p {
    flex: 1;
    margin: 6px 0 10px;
    font-size: 11px;
    line-height: 1.55;
    color: var(--ink-3);
  }
}

.driver-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.driver-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--brand-soft);
  color: var(--brand);
  display: grid;
  place-items: center;
  font-size: 17px;
}

.check {
  color: var(--brand);
  font-size: 17px;
}

.driver-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px dashed var(--line);
}

.driver-code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 10.5px;
  color: var(--ink-3);
  letter-spacing: 0.04em;
}

.config-area {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.config-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;

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

.driver-warning {
  margin-bottom: 16px;
}

.storage-form {
  max-width: 1060px;
}

.test-result {
  max-width: 1060px;
  margin-bottom: 14px;
}

.storage-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.save-tip {
  font-size: 11.5px;
  color: var(--ink-3);
}

@media (max-width: 1280px) {
  .driver-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .driver-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
