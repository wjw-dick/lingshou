<template>
  <div class="page">
    <PageHeader title="全局参数" subtitle="站点、支付、配送、库存等运行参数，保存后实时生效。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存当前分组</el-button>
      </template>
    </PageHeader>

    <div class="card config-card" v-loading="loading">
      <el-tabs v-model="activeGroup" tab-position="left" class="config-tabs" @tab-change="onTabChange">
        <el-tab-pane v-for="group in groups" :key="group.code" :name="group.code">
          <template #label>
            <span class="tab-label">
              {{ group.label }}
              <em>{{ countOf(group.code) }}</em>
            </span>
          </template>

          <div class="group-head">
            <div>
              <h3>{{ group.label }}</h3>
              <p>{{ group.desc }}</p>
            </div>
            <el-tag effect="plain" round>{{ countOf(group.code) }} 项参数</el-tag>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="190px" class="config-form">
            <el-row :gutter="20">
              <el-col v-for="item in groupItems(group.code)" :key="item.configKey" :xs="24" :lg="12">
                <el-form-item :label="labelOf(item)" :prop="fieldOf(item.configKey)">
                  <el-input
                    v-if="isEncrypted(item)"
                    v-model="form[fieldOf(item.configKey)]"
                    type="password"
                    show-password
                    :placeholder="item.hasValue ? '已配置，留空则保持不变' : '请输入密钥'"
                  />
                  <el-select
                    v-else-if="selectOptions[item.configKey]"
                    v-model="form[fieldOf(item.configKey)]"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="opt in selectOptions[item.configKey]"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                  <el-input
                    v-else
                    v-model="form[fieldOf(item.configKey)]"
                    :placeholder="placeholderOf(item)"
                    clearable
                  >
                    <template v-if="unitOf(item.configKey)" #append>{{ unitOf(item.configKey) }}</template>
                  </el-input>
                  <div class="field-hint">
                    <span>{{ item.remark || item.configKey }}</span>
                    <el-tag v-if="isEncrypted(item)" size="small" type="warning" effect="plain">加密存储</el-tag>
                    <el-tag v-else-if="item.hasValue" size="small" type="success" effect="plain">已配置</el-tag>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <el-empty v-if="!groupItems(group.code).length" description="该分组暂无参数" :image-size="90" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="foot-tip">
      <el-icon><InfoFilled /></el-icon>
      <span>加密参数以掩码展示，留空提交不会覆盖已保存的密钥；保存后配置立即生效，无需重启服务。</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, InfoFilled, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { api } from '@/api/http'

interface ConfigItem {
  id: number
  configKey: string
  configValue: string
  groupCode: string
  encrypted: number
  remark?: string
  hasValue?: boolean
}

interface ConfigGroup {
  code: string
  label: string
  desc: string
}

const groups: ConfigGroup[] = [
  { code: 'SITE', label: '站点', desc: '品牌名称、客服电话、SEO 等站点基础信息。' },
  { code: 'PAYMENT', label: '支付', desc: '微信、支付宝等支付渠道商户凭证，密钥加密保存。' },
  { code: 'DELIVERY', label: '配送', desc: '起送价、配送费、免运费门槛与配送半径。' },
  { code: 'INVENTORY', label: '库存', desc: '安全库存与临期预警策略。' },
  { code: 'MEMBER', label: '会员', desc: '会员积分与等级相关参数。' },
  { code: 'FRANCHISE', label: '加盟', desc: '加盟分账比例等结算参数。' },
  { code: 'I18N', label: '多语言', desc: '默认语言与多语言开关。' },
  { code: 'GENERAL', label: '通用', desc: '其他通用运行参数。' }
]

const groupLabels: Record<string, string> = {
  SITE: '站点信息',
  PAYMENT: '支付配置',
  DELIVERY: '配送规则',
  INVENTORY: '库存策略',
  MEMBER: '会员参数',
  FRANCHISE: '加盟参数',
  I18N: '多语言',
  GENERAL: '通用参数'
}

const labelMap: Record<string, string> = {
  'site.name': '站点名称',
  'site.logo': '站点 Logo',
  'site.icp': 'ICP 备案号',
  'site.service_phone': '客服电话',
  'site.business_hours': '营业时间',
  'site.seo_title': 'SEO 标题',
  'site.seo_keywords': 'SEO 关键词',
  'payment.wechat_mch_id': '微信支付商户号',
  'payment.wechat_api_key': '微信支付 API 密钥',
  'payment.alipay_app_id': '支付宝 AppID',
  'delivery.min_order': '起送金额',
  'delivery.fee': '配送费',
  'delivery.free_threshold': '免配送费门槛',
  'delivery.radius_m': '配送半径',
  'stock.safety_days': '安全库存天数',
  'expiry.near_days': '临期预警天数',
  'member.point_rate': '积分倍率',
  'commission.default_rate': '默认分账比例',
  'i18n.default_lang': '默认语言',
  'i18n.enabled': '启用多语言'
}

const unitMap: Record<string, string> = {
  'delivery.min_order': '元',
  'delivery.fee': '元',
  'delivery.free_threshold': '元',
  'delivery.radius_m': '米',
  'stock.safety_days': '天',
  'expiry.near_days': '天',
  'member.point_rate': '倍',
  'commission.default_rate': '%'
}

const selectOptions: Record<string, { label: string; value: string }[]> = {
  'i18n.default_lang': [
    { label: '简体中文 zh-CN', value: 'zh-CN' },
    { label: 'English en-US', value: 'en-US' },
    { label: '繁體中文 zh-TW', value: 'zh-TW' },
    { label: '日本語 ja-JP', value: 'ja-JP' }
  ],
  'i18n.enabled': [
    { label: '启用', value: 'true' },
    { label: '关闭', value: 'false' }
  ]
}

const loading = ref(false)
const saving = ref(false)
const activeGroup = ref('SITE')
const items = ref<ConfigItem[]>([])
const form = ref<Record<string, string>>({})
const origin = ref<Record<string, string>>({})
const formRef = ref()

const fieldOf = (key: string) => key.replace(/[.-]/g, '_')

const rules = computed<Record<string, any[]>>(() => {
  const map: Record<string, any[]> = {}
  for (const item of items.value) {
    if (['site.name', 'site.service_phone', 'i18n.default_lang'].includes(item.configKey)) {
      map[fieldOf(item.configKey)] = [{ required: true, message: `请填写${labelOf(item)}`, trigger: 'blur' }]
    }
    if (unitMap[item.configKey]) {
      map[fieldOf(item.configKey)] = [
        { pattern: /^-?\d+(\.\d{1,2})?$/, message: '请输入数字，最多两位小数', trigger: 'blur' }
      ]
    }
  }
  return map
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const rows = await api.get<ConfigItem[]>('/tenant/system/configs')
    items.value = rows || []
    const next: Record<string, string> = {}
    for (const item of items.value) {
      next[fieldOf(item.configKey)] = item.configValue ?? ''
    }
    form.value = next
    origin.value = { ...next }
    if (!groups.some((g) => g.code === activeGroup.value)) {
      activeGroup.value = groups[0].code
    }
  } catch {
    items.value = []
    form.value = {}
  } finally {
    loading.value = false
  }
}

function groupItems(code: string) {
  return items.value.filter((item) => (item.groupCode || 'GENERAL') === code)
}

function countOf(code: string) {
  return groupItems(code).length
}

function labelOf(item: ConfigItem) {
  return labelMap[item.configKey] || item.remark || item.configKey
}

function isEncrypted(item: ConfigItem) {
  return Number(item.encrypted) === 1
}

function placeholderOf(item: ConfigItem) {
  if (item.configKey === 'site.logo') return '请输入图片地址，或前往素材库上传'
  return `请输入${labelOf(item)}`
}

function unitOf(key: string) {
  return unitMap[key] || ''
}

function onTabChange() {
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const current = groupItems(activeGroup.value)
  if (!current.length) {
    ElMessage.info('当前分组没有可保存的参数')
    return
  }
  const payload = current.map((item) => {
    const key = fieldOf(item.configKey)
    const value = form.value[key] ?? ''
    const unchangedSecret = isEncrypted(item) && item.hasValue && value === (origin.value[key] ?? '')
    return {
      configKey: item.configKey,
      configValue: unchangedSecret ? '' : value,
      groupCode: item.groupCode || activeGroup.value,
      encrypted: Number(item.encrypted) || 0,
      remark: item.remark
    }
  })
  saving.value = true
  try {
    await api.put('/tenant/system/configs', payload)
    ElMessage.success('配置已保存，实时生效')
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.config-card {
  padding: 8px 8px 8px 0;
  min-height: 420px;
}

.config-tabs {
  :deep(.el-tabs__header) {
    width: 170px;
    margin-right: 8px;
  }
  :deep(.el-tabs__item) {
    justify-content: flex-start;
    height: 46px;
    padding: 0 18px;
    font-size: 13px;
  }
  :deep(.el-tabs__content) {
    padding: 20px 24px 28px;
  }
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  em {
    font-style: normal;
    font-size: 11px;
    color: var(--ink-3);
    background: #f2f4f7;
    border-radius: 999px;
    padding: 0 7px;
    line-height: 18px;
  }
}

.group-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  margin-bottom: 18px;
  border-bottom: 1px dashed var(--line);
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 650;
  }
  p {
    margin: 5px 0 0;
    font-size: 12px;
    color: var(--ink-3);
  }
}

.config-form {
  max-width: 1080px;
}

.field-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--ink-3);
  line-height: 1.4;
}

.foot-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f0f6ff;
  color: #475467;
  font-size: 12px;
  .el-icon {
    color: var(--brand);
    font-size: 16px;
  }
}
</style>
