<template>
  <div class="page">
    <PageHeader title="SSO 配置" subtitle="对接企业统一身份认证，支持 CAS / OAuth2 / OIDC / Sa-Token。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存配置</el-button>
      </template>
    </PageHeader>

    <div class="sso-grid" v-loading="loading">
      <div class="card card-pad">
        <div class="section-head">
          <div>
            <h3>认证参数</h3>
            <p>启用后登录页将显示统一认证入口，Secret 以掩码展示。</p>
          </div>
          <StatusTag :status="form.enabled === 1 ? 'ACTIVE' : 'DISABLED'" :text="form.enabled === 1 ? '已启用' : '未启用'" />
        </div>

        <el-alert
          type="info"
          show-icon
          :closable="false"
          class="degrade-alert"
          title="SSO 服务异常时自动降级为账号密码登录，保障后台可进入；请保留至少一个可用的超级管理员账号。"
        />

        <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
          <el-form-item label="启用 SSO">
            <div class="switch-line">
              <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
              <span>{{ form.enabled === 1 ? '登录页展示 SSO 入口' : '仅使用账号密码登录' }}</span>
            </div>
          </el-form-item>

          <el-form-item label="认证协议" prop="protocol">
            <el-select v-model="form.protocol" style="width: 100%">
              <el-option v-for="item in protocols" :key="item.value" :label="item.label" :value="item.value">
                <div class="protocol-option">
                  <span>{{ item.label }}</span>
                  <em>{{ item.desc }}</em>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="服务器根地址" prop="serverUrl">
            <el-input v-model="form.serverUrl" placeholder="https://sso.example.com" clearable />
            <div class="form-tip">认证服务器地址，启用 SSO 时必填。</div>
          </el-form-item>

          <el-form-item label="回调 / 服务地址" prop="serviceUrl">
            <el-input v-model="form.serviceUrl" placeholder="https://admin.example.com/api/v1/auth/sso/callback" clearable />
            <div class="form-tip">在身份提供方登记的回调地址，需与后台保持一致。</div>
          </el-form-item>

          <el-form-item label="Client ID" prop="clientId">
            <el-input v-model="form.clientId" placeholder="客户端标识" clearable />
          </el-form-item>

          <el-form-item label="Client Secret">
            <el-input
              v-model="form.clientSecret"
              type="password"
              show-password
              placeholder="已配置，留空则保持不变"
              autocomplete="new-password"
            />
            <div class="form-tip">留空提交不会覆盖已保存的 Secret。</div>
          </el-form-item>

          <el-form-item label="账号字段映射" prop="accountMapping">
            <el-input
              v-model="form.accountMapping"
              type="textarea"
              :rows="3"
              placeholder="username=preferred_username,email=email,realName=name"
            />
            <div class="form-tip">使用 字段=声明 的形式，多个字段用英文逗号分隔。</div>
          </el-form-item>

          <el-form-item label="自动创建账号">
            <div class="switch-line">
              <el-switch v-model="form.autoCreate" :active-value="1" :inactive-value="0" />
              <span>首次登录时按映射自动创建后台账号</span>
            </div>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存配置</el-button>
        </div>
      </div>

      <div class="side-col">
        <div class="card card-pad">
          <h3 class="side-title">接入步骤</h3>
          <ol class="step-list">
            <li><span>1</span>在身份提供方创建应用，获取 Client ID 与 Secret。</li>
            <li><span>2</span>登记回调地址，与上方「回调 / 服务地址」一致。</li>
            <li><span>3</span>配置账号字段映射，建议包含工号或邮箱。</li>
            <li><span>4</span>开启 SSO 并保存，登录页即刻显示统一认证入口。</li>
          </ol>
        </div>

        <div class="card card-pad">
          <h3 class="side-title">协议说明</h3>
          <div class="protocol-list">
            <div v-for="item in protocols" :key="item.value" class="protocol-item">
              <strong>{{ item.label }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </div>
        </div>

        <div class="card card-pad tip-card">
          <el-icon :size="18"><WarningFilled /></el-icon>
          <div>
            <strong>故障降级</strong>
            <p>SSO 不可用时系统自动回落到账号密码登录，请确保本地管理员账号可用，并定期校验映射规则。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh, WarningFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const protocols = [
  { value: 'OIDC', label: 'OIDC', desc: '基于 OAuth2 的身份层协议，推荐新接入使用。' },
  { value: 'OAuth2', label: 'OAuth2', desc: '通用授权码模式，兼容多数企业网关。' },
  { value: 'CAS', label: 'CAS', desc: '传统单点登录协议，适合高校与政企内网。' },
  { value: 'Sa-Token', label: 'Sa-Token', desc: '轻量鉴权框架，适合自建统一认证中心。' }
]

const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const defaultForm = () => ({
  enabled: 0,
  protocol: 'OIDC',
  serverUrl: '',
  serviceUrl: '',
  clientId: '',
  clientSecret: '',
  accountMapping: '',
  autoCreate: 0
})

const form = ref<any>(defaultForm())

const rules: any = {
  protocol: [{ required: true, message: '请选择认证协议', trigger: 'change' }],
  serverUrl: [
    {
      validator(_rule: any, value: string, callback: (error?: Error) => void) {
        if (form.value.enabled === 1 && !value) return callback(new Error('启用 SSO 时必须填写服务器根地址'))
        if (value && !/^https?:\/\/.+/.test(value)) return callback(new Error('请输入以 http:// 或 https:// 开头的地址'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  serviceUrl: [
    {
      validator(_rule: any, value: string, callback: (error?: Error) => void) {
        if (value && !/^https?:\/\/.+/.test(value)) return callback(new Error('请输入以 http:// 或 https:// 开头的地址'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  accountMapping: [
    {
      validator(_rule: any, value: string, callback: (error?: Error) => void) {
        if (!value) return callback()
        const ok = /^[A-Za-z0-9_]+=.+(,[A-Za-z0-9_]+=.+)*$/.test(value)
        callback(ok ? undefined : new Error('格式错误，示例：username=sub,email=email'))
      },
      trigger: 'blur'
    }
  ]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await api.get('/tenant/system/sso')
    form.value = data ? { ...defaultForm(), ...data } : defaultForm()
  } catch {
    form.value = defaultForm()
  } finally {
    loading.value = false
  }
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await api.post('/tenant/system/sso', form.value)
    ElMessage.success('SSO 配置已保存')
    await load()
  } finally {
    saving.value = false
  }
}

function reset() {
  load()
}
</script>

<style scoped lang="scss">
.sso-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 16px;
  align-items: start;
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

.degrade-alert {
  margin-bottom: 18px;
}

.switch-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ink-3);
}

.protocol-option {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  em {
    font-style: normal;
    font-size: 11px;
    color: var(--ink-3);
  }
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
}

.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 650;
}

.step-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;

  li {
    display: flex;
    gap: 9px;
    font-size: 12px;
    line-height: 1.6;
    color: var(--ink-2);
  }
  span {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
    border-radius: 50%;
    background: var(--brand-soft);
    color: var(--brand);
    display: grid;
    place-items: center;
    font-size: 11px;
    font-weight: 600;
  }
}

.protocol-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.protocol-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eef1f5;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
  strong {
    font-size: 12px;
    font-weight: 600;
  }
  span {
    font-size: 11px;
    line-height: 1.55;
    color: var(--ink-3);
  }
}

.tip-card {
  display: flex;
  gap: 10px;
  background: #fffbeb;
  border-color: #fdecc8;
  color: #b25e09;

  strong {
    font-size: 12.5px;
  }
  p {
    margin: 5px 0 0;
    font-size: 11.5px;
    line-height: 1.6;
    color: #9a5b0a;
  }
}

@media (max-width: 1080px) {
  .sso-grid {
    grid-template-columns: 1fr;
  }
}
</style>
