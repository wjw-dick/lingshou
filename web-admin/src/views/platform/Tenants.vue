<template>
  <div class="page">
    <PageHeader title="租户管理" subtitle="租户开通、续期与配额管控；平台侧只看资源用量，不穿透租户业务明细。">
      <template #actions>
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openWizard">开通租户</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="租户总数" :value="Number(kpi.tenantTotal || 0)" unit="家" icon="OfficeBuilding" hint="未删除租户" color="#1f6feb" />
      <StatCard label="活跃租户" :value="Number(kpi.activeTotal || 0)" unit="家" icon="CircleCheck" hint="正常 + 试用" color="#14b8a6" />
      <StatCard label="试用中" :value="Number(kpi.trialTotal || 0)" unit="家" icon="MagicStick" hint="需跟进转化" color="#7c3aed" />
      <StatCard label="30 天内到期" :value="Number(kpi.expiringTotal || 0)" unit="家" icon="AlarmClock" hint="续费风险" color="#f0a020" />
      <StatCard label="已停用" :value="Number(kpi.suspendedTotal || 0)" unit="家" icon="CircleClose" hint="停用状态" color="#d03050" />
      <StatCard label="累计回款" prefix="¥" :value="Number(kpi.revenue || 0)" icon="Money" hint="已支付账单" color="#0d9488" />
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="租户名称 / 编码"
          clearable
          style="width: 200px"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="试用中" value="TRIAL" />
          <el-option label="欠费" value="OVERDUE" />
          <el-option label="已停用" value="SUSPENDED" />
        </el-select>
        <el-select v-model="filters.planId" placeholder="全部套餐" clearable style="width: 160px" @change="search">
          <el-option v-for="plan in plans" :key="plan.planId" :label="plan.name" :value="plan.planId" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 家租户</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="tenantId" style="width: 100%">
        <el-table-column label="租户" min-width="210">
          <template #default="{ row }">
            <div class="tenant-cell">
              <div class="tenant-avatar">{{ (row.name || '租').slice(0, 1) }}</div>
              <div class="tenant-meta">
                <strong>{{ row.name }}</strong>
                <span>{{ row.tenantCode }} · {{ row.industry || '未填写行业' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系人" min-width="140">
          <template #default="{ row }">
            <div class="stack">
              <strong>{{ row.contact || '-' }}</strong>
              <span>{{ row.phone || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="套餐" width="120">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ row.planName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="有效期" width="150">
          <template #default="{ row }">
            <div class="stack">
              <span v-if="row.status === 'TRIAL'">试用至 {{ date(row.trialEndAt) }}</span>
              <span v-else>{{ row.expireAt ? `到期 ${date(row.expireAt)}` : '长期有效' }}</span>
              <em v-if="row.expireAt && isExpiringSoon(row.expireAt)" class="warn">即将到期</em>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="配额用量" min-width="220">
          <template #default="{ row }">
            <div class="quota-cell">
              <div class="quota-row">
                <span class="quota-label">席位</span>
                <el-progress :percentage="percent(row.usage && row.usage.seatUsed, row.seatLimit)" :stroke-width="6" :show-text="false" :color="progressColor" />
                <em>{{ row.usage?.seatUsed ?? 0 }}/{{ row.seatLimit ?? 0 }}</em>
              </div>
              <div class="quota-row">
                <span class="quota-label">门店</span>
                <el-progress :percentage="percent(row.usage && row.usage.storeUsed, row.storeLimit)" :stroke-width="6" :show-text="false" :color="progressColor" />
                <em>{{ row.usage?.storeUsed ?? 0 }}/{{ row.storeLimit ?? 0 }}</em>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openRenew(row)">续期</el-button>
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => onCommand(cmd, row)">
              <el-button link type="primary">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="usage">配额用量</el-dropdown-item>
                  <el-dropdown-item :command="row.status === 'SUSPENDED' ? 'resume' : 'suspend'">
                    {{ row.status === 'SUSPENDED' ? '恢复租户' : '停用租户' }}
                  </el-dropdown-item>
                  <el-dropdown-item divided command="delete">删除租户</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无租户，点击右上角开通租户" :image-size="80" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 开通向导 -->
    <el-dialog v-model="wizardVisible" title="开通租户" width="780px" :close-on-click-modal="false" @closed="resetWizard">
      <el-steps :active="step" finish-status="success" align-center class="wizard-steps">
        <el-step title="基本信息" />
        <el-step title="选择套餐" />
        <el-step title="配额确认" />
        <el-step title="初始化结果" />
      </el-steps>

      <div class="wizard-body">
        <el-form v-if="step === 0" ref="formRef" :model="form" :rules="rules" label-width="100px">
          <div class="form-grid">
            <el-form-item label="租户编码" prop="tenantCode">
              <el-input v-model="form.tenantCode" placeholder="如：yuexiang001，创建后不可修改" maxlength="32" />
            </el-form-item>
            <el-form-item label="租户名称" prop="name">
              <el-input v-model="form.name" placeholder="如：悦享便利（杭州）" maxlength="60" />
            </el-form-item>
            <el-form-item label="所属行业" prop="industry">
              <el-input v-model="form.industry" placeholder="默认连锁便利店" maxlength="30" />
            </el-form-item>
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="form.contact" placeholder="负责人姓名" maxlength="30" />
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
            <el-form-item label="管理员账号" prop="adminUsername">
              <el-input v-model="form.adminUsername" placeholder="租户后台登录账号" maxlength="32" />
            </el-form-item>
            <el-form-item label="初始密码" prop="adminPassword">
              <el-input v-model="form.adminPassword" type="password" show-password placeholder="至少 6 位" maxlength="32" />
            </el-form-item>
          </div>
          <div class="form-tip">开通后系统将自动复制菜单、角色、参数，创建默认门店、分类与配送规则，并绑定租户管理员。</div>
        </el-form>

        <div v-else-if="step === 1">
          <div class="plan-grid">
            <div
              v-for="plan in activePlans"
              :key="plan.planId"
              class="plan-card"
              :class="{ selected: form.planId === plan.planId }"
              @click="selectPlan(plan)"
            >
              <div class="plan-head">
                <strong>{{ plan.name }}</strong>
                <el-tag size="small" effect="plain">{{ plan.planCode }}</el-tag>
              </div>
              <div class="plan-price">
                <span>¥</span>{{ money(plan.price) }}<em>/{{ plan.billingCycle === 'MONTH' ? '月' : '年' }}</em>
              </div>
              <ul class="plan-features">
                <li>席位 {{ plan.seatLimit }} 个 · 门店 {{ plan.storeLimit }} 家</li>
                <li>公众号 {{ plan.mpAccountLimit }} 个 · 存储 {{ plan.storageQuotaGb }}GB</li>
                <li>短信 {{ number(plan.smsQuota) }} 条 · API {{ number(plan.apiQuota) }} 次</li>
                <li>试用 {{ plan.trialDays }} 天</li>
              </ul>
              <div class="plan-tags">
                <el-tag v-for="feat in featureList(plan.features).slice(0, 3)" :key="feat" size="small" type="info" effect="plain">{{ feat }}</el-tag>
              </div>
              <el-icon v-if="form.planId === plan.planId" class="plan-check"><CircleCheckFilled /></el-icon>
            </div>
            <el-empty v-if="!activePlans.length" description="暂无可售套餐，请先到套餐与配额中配置" :image-size="70" />
          </div>
        </div>

        <div v-else-if="step === 2 && selectedPlan">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="租户编码">{{ form.tenantCode }}</el-descriptions-item>
            <el-descriptions-item label="租户名称">{{ form.name }}</el-descriptions-item>
            <el-descriptions-item label="管理员账号">{{ form.adminUsername }}</el-descriptions-item>
            <el-descriptions-item label="所属套餐">{{ selectedPlan.name }}</el-descriptions-item>
          </el-descriptions>

          <div class="quota-confirm">
            <div class="quota-confirm-head">
              <h4>配额确认</h4>
              <span>套餐配额默认值可在开通时调整门店上限</span>
            </div>
            <div class="quota-confirm-grid">
              <div class="quota-confirm-item"><span>席位数</span><strong>{{ selectedPlan.seatLimit }}</strong></div>
              <div class="quota-confirm-item"><span>公众号</span><strong>{{ selectedPlan.mpAccountLimit }}</strong></div>
              <div class="quota-confirm-item"><span>存储</span><strong>{{ selectedPlan.storageQuotaGb }} GB</strong></div>
              <div class="quota-confirm-item"><span>短信</span><strong>{{ number(selectedPlan.smsQuota) }} 条</strong></div>
              <div class="quota-confirm-item"><span>API</span><strong>{{ number(selectedPlan.apiQuota) }} 次</strong></div>
              <div class="quota-confirm-item">
                <span>门店上限</span>
                <el-input-number v-model="form.storeLimit" :min="1" :max="9999" size="small" controls-position="right" />
              </div>
            </div>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            :title="`试用期 ${selectedPlan.trialDays} 天，试用到期后可续期转为正式租户。`"
            style="margin-top: 14px"
          />
        </div>

        <div v-else-if="step === 3 && result">
          <el-result icon="success" title="租户初始化完成" :sub-title="`租户 ${result.tenantCode} 已创建，管理员账号可立即登录。`" />
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="租户 ID">{{ result.tenantId }}</el-descriptions-item>
            <el-descriptions-item label="租户编码">{{ result.tenantCode }}</el-descriptions-item>
            <el-descriptions-item label="管理员账号">{{ result.adminUsername }}</el-descriptions-item>
            <el-descriptions-item label="初始密码">
              <div class="pwd-row">
                <el-input :model-value="result.initialPassword" readonly show-password size="small" />
                <el-button size="small" :icon="CopyDocument" @click="copyText(result.initialPassword)">复制</el-button>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="登录地址">{{ result.loginUrl }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="初始密码仅本次展示，请立即复制并转交租户管理员，关闭后无法再次查看。"
            style="margin-top: 12px"
          />
        </div>
      </div>

      <template #footer>
        <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
        <el-button v-if="step < 2" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="step === 2" type="primary" :loading="creating" @click="submitCreate">开始初始化</el-button>
        <el-button v-if="step === 3" type="primary" @click="wizardVisible = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- 详情 / 配额用量 -->
    <el-drawer v-model="detailVisible" title="租户详情与配额用量" size="520px">
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail">
          <h4 class="section-title">基本信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="租户编码">{{ detail.tenant?.tenantCode }}</el-descriptions-item>
            <el-descriptions-item label="租户名称">{{ detail.tenant?.name }}</el-descriptions-item>
            <el-descriptions-item label="行业">{{ detail.tenant?.industry || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ detail.tenant?.contact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.tenant?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="套餐">{{ detail.plan?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.tenant?.status" /></el-descriptions-item>
            <el-descriptions-item label="试用到期">{{ date(detail.tenant?.trialEndAt) }}</el-descriptions-item>
            <el-descriptions-item label="正式到期">{{ date(detail.tenant?.expireAt) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ datetime(detail.tenant?.createdAt) }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title" style="margin-top: 20px">配额用量</h4>
          <div class="usage-list">
            <div v-for="item in usageItems" :key="item.label" class="usage-item">
              <div class="usage-head">
                <span>{{ item.label }}</span>
                <em :class="{ danger: percent(item.used, item.limit) >= 90 }">{{ item.text }}</em>
              </div>
              <el-progress :percentage="percent(item.used, item.limit)" :stroke-width="8" :color="progressColor" />
            </div>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="平台端仅统计席位、门店、公众号、存储、短信与 API 用量，不展示租户订单、会员等业务明细。"
            style="margin-top: 14px"
          />
        </template>
      </div>
    </el-drawer>

    <!-- 续期 -->
    <el-dialog v-model="renewVisible" title="租户续期" width="440px">
      <div v-if="currentTenant" class="renew-body">
        <div class="renew-tenant">
          <strong>{{ currentTenant.name }}</strong>
          <span>{{ currentTenant.tenantCode }} · 当前到期 {{ date(currentTenant.expireAt) }}</span>
        </div>
        <el-form label-width="90px">
          <el-form-item label="续期时长">
            <el-radio-group v-model="renewMonths">
              <el-radio-button :value="1">1 个月</el-radio-button>
              <el-radio-button :value="3">3 个月</el-radio-button>
              <el-radio-button :value="6">6 个月</el-radio-button>
              <el-radio-button :value="12">1 年</el-radio-button>
              <el-radio-button :value="24">2 年</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <div class="renew-preview">
          <span>续期后到期时间</span>
          <strong>{{ renewPreview }}</strong>
        </div>
        <div class="form-tip">续期成功后租户状态将自动调整为「正常」。</div>
      </div>
      <template #footer>
        <el-button @click="renewVisible = false">取消</el-button>
        <el-button type="primary" :loading="renewing" @click="submitRenew">确认续期</el-button>
      </template>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="editVisible" title="编辑租户" width="640px">
      <el-form :model="editForm" label-width="100px">
        <div class="form-grid">
          <el-form-item label="租户名称"><el-input v-model="editForm.name" maxlength="60" /></el-form-item>
          <el-form-item label="所属行业"><el-input v-model="editForm.industry" maxlength="30" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model="editForm.contact" maxlength="30" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="editForm.phone" maxlength="20" /></el-form-item>
          <el-form-item label="所属套餐">
            <el-select v-model="editForm.planId" placeholder="请选择套餐" style="width: 100%">
              <el-option v-for="plan in plans" :key="plan.planId" :label="plan.name" :value="plan.planId" />
            </el-select>
          </el-form-item>
          <el-form-item label="到期时间">
            <el-date-picker
              v-model="editForm.expireAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="不填表示长期有效"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="席位上限"><el-input-number v-model="editForm.seatLimit" :min="1" :max="9999" controls-position="right" /></el-form-item>
          <el-form-item label="门店上限"><el-input-number v-model="editForm.storeLimit" :min="1" :max="9999" controls-position="right" /></el-form-item>
          <el-form-item label="公众号上限"><el-input-number v-model="editForm.mpAccountLimit" :min="0" :max="999" controls-position="right" /></el-form-item>
          <el-form-item label="存储配额"><el-input-number v-model="editForm.storageQuotaGb" :min="1" :max="99999" controls-position="right" /></el-form-item>
          <el-form-item label="短信配额"><el-input-number v-model="editForm.smsQuota" :min="0" :max="99999999" controls-position="right" /></el-form-item>
          <el-form-item label="API 配额"><el-input-number v-model="editForm.apiQuota" :min="0" :max="999999999" controls-position="right" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, CopyDocument, Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive<{ keyword: string; status: string; planId: number | undefined }>({
  keyword: '',
  status: '',
  planId: undefined
})
const kpi = ref<any>({})
const plans = ref<any[]>([])

const formRef = ref<any>()
const wizardVisible = ref(false)
const step = ref(0)
const creating = ref(false)
const result = ref<any>(null)
const form = reactive({
  tenantCode: '',
  name: '',
  industry: '连锁便利店',
  contact: '',
  phone: '',
  planId: undefined as number | undefined,
  adminUsername: '',
  adminPassword: '',
  storeLimit: undefined as number | undefined
})
const rules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  adminUsername: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  adminPassword: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const renewVisible = ref(false)
const renewing = ref(false)
const renewMonths = ref(12)
const currentTenant = ref<any>(null)

const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive<any>({})

const activePlans = computed(() => plans.value.filter((p) => p.status !== 'DISABLED'))
const selectedPlan = computed(() => plans.value.find((p) => p.planId === form.planId))
const renewPreview = computed(() => {
  const current = currentTenant.value
  if (!current) return '-'
  const base = current.expireAt && dayjs(current.expireAt).isAfter(dayjs()) ? dayjs(current.expireAt) : dayjs()
  return base.add(renewMonths.value, 'month').format('YYYY-MM-DD')
})
const usageItems = computed(() => {
  const usage = detail.value?.usage || {}
  return [
    { label: '席位', used: usage.seatUsed, limit: usage.seatLimit, text: `${usage.seatUsed ?? 0} / ${usage.seatLimit ?? 0}` },
    { label: '门店', used: usage.storeUsed, limit: usage.storeLimit, text: `${usage.storeUsed ?? 0} / ${usage.storeLimit ?? 0}` },
    { label: '公众号', used: usage.mpUsed, limit: usage.mpLimit, text: `${usage.mpUsed ?? 0} / ${usage.mpLimit ?? 0}` },
    { label: '存储', used: usage.storageUsedGb, limit: usage.storageLimitGb, text: `${usage.storageUsedGb ?? 0} / ${usage.storageLimitGb ?? 0} GB` },
    { label: '短信', used: usage.smsUsed, limit: usage.smsLimit, text: `${number(usage.smsUsed)} / ${number(usage.smsLimit)}` },
    { label: 'API', used: usage.apiUsed, limit: usage.apiLimit, text: `${number(usage.apiUsed)} / ${number(usage.apiLimit)}` }
  ]
})

onMounted(() => {
  load()
  loadKpi()
  loadPlans()
})

function refreshAll() {
  load()
  loadKpi()
  loadPlans()
}

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/platform/tenants', {
      page: page.value,
      size: size.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      planId: filters.planId || undefined
    })
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadKpi() {
  try {
    kpi.value = await api.get('/platform/dashboard')
  } catch {
    kpi.value = {}
  }
}

async function loadPlans() {
  try {
    const data: any = await api.get('/platform/plans')
    plans.value = Array.isArray(data) ? data : []
  } catch {
    plans.value = []
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.planId = undefined
  search()
}

function openWizard() {
  resetWizard()
  wizardVisible.value = true
}

function resetWizard() {
  step.value = 0
  result.value = null
  form.tenantCode = ''
  form.name = ''
  form.industry = '连锁便利店'
  form.contact = ''
  form.phone = ''
  form.planId = undefined
  form.adminUsername = ''
  form.adminPassword = ''
  form.storeLimit = undefined
}

function selectPlan(plan: any) {
  form.planId = plan.planId
  form.storeLimit = plan.storeLimit
}

async function nextStep() {
  if (step.value === 0) {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
    step.value = 1
    return
  }
  if (step.value === 1) {
    if (!form.planId || !selectedPlan.value) {
      ElMessage.warning('请选择一个套餐')
      return
    }
    if (!form.storeLimit) form.storeLimit = selectedPlan.value.storeLimit
    step.value = 2
  }
}

async function submitCreate() {
  if (!form.storeLimit || Number(form.storeLimit) < 1) {
    ElMessage.warning('门店上限至少为 1')
    return
  }
  creating.value = true
  try {
    result.value = await api.post('/platform/tenants', {
      tenantCode: form.tenantCode.trim(),
      name: form.name.trim(),
      industry: form.industry,
      contact: form.contact,
      phone: form.phone,
      planId: form.planId,
      adminUsername: form.adminUsername.trim(),
      adminPassword: form.adminPassword,
      storeLimit: form.storeLimit
    })
    step.value = 3
    load()
    loadKpi()
  } finally {
    creating.value = false
  }
}

async function openDetail(row: any) {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [d, usage] = await Promise.all([
      api.get(`/platform/tenants/${row.tenantId}`),
      api.get(`/platform/tenants/${row.tenantId}/usage`)
    ])
    detail.value = { ...d, usage }
  } catch {
    detail.value = { tenant: row, plan: { name: row.planName }, usage: row.usage }
  } finally {
    detailLoading.value = false
  }
}

function openRenew(row: any) {
  currentTenant.value = row
  renewMonths.value = 12
  renewVisible.value = true
}

async function submitRenew() {
  if (!currentTenant.value) return
  renewing.value = true
  try {
    await api.post(`/platform/tenants/${currentTenant.value.tenantId}/renew`, { months: renewMonths.value })
    ElMessage.success('续期成功')
    renewVisible.value = false
    refreshAll()
  } finally {
    renewing.value = false
  }
}

function openEdit(row: any) {
  Object.keys(editForm).forEach((key) => delete editForm[key])
  Object.assign(editForm, {
    tenantId: row.tenantId,
    name: row.name,
    industry: row.industry,
    contact: row.contact,
    phone: row.phone,
    planId: row.planId,
    expireAt: row.expireAt ? dayjs(row.expireAt).format('YYYY-MM-DDTHH:mm:ss') : null,
    seatLimit: row.seatLimit,
    storeLimit: row.storeLimit,
    mpAccountLimit: row.mpAccountLimit,
    storageQuotaGb: row.storageQuotaGb,
    smsQuota: row.smsQuota,
    apiQuota: row.apiQuota
  })
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.name) {
    ElMessage.warning('请填写租户名称')
    return
  }
  saving.value = true
  try {
    await api.put(`/platform/tenants/${editForm.tenantId}`, {
      name: editForm.name,
      industry: editForm.industry,
      contact: editForm.contact,
      phone: editForm.phone,
      planId: editForm.planId,
      expireAt: editForm.expireAt || undefined,
      seatLimit: editForm.seatLimit,
      storeLimit: editForm.storeLimit,
      mpAccountLimit: editForm.mpAccountLimit,
      storageQuotaGb: editForm.storageQuotaGb,
      smsQuota: editForm.smsQuota,
      apiQuota: editForm.apiQuota
    })
    ElMessage.success('租户信息已更新')
    editVisible.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

async function onCommand(command: string, row: any) {
  if (command === 'usage') {
    openDetail(row)
    return
  }
  if (command === 'suspend') {
    try {
      await ElMessageBox.confirm(`停用「${row.name}」后该租户将无法登录与写入数据，是否继续？`, '停用租户', {
        type: 'warning',
        confirmButtonText: '确认停用',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    await api.post(`/platform/tenants/${row.tenantId}/status`, null, { params: { status: 'SUSPENDED' } })
    ElMessage.success('租户已停用')
    refreshAll()
    return
  }
  if (command === 'resume') {
    try {
      await ElMessageBox.confirm(`确认恢复「${row.name}」？恢复后租户可正常登录。`, '恢复租户', { type: 'warning' })
    } catch {
      return
    }
    await api.post(`/platform/tenants/${row.tenantId}/status`, null, { params: { status: 'ACTIVE' } })
    ElMessage.success('租户已恢复')
    refreshAll()
    return
  }
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`删除租户「${row.name}」后不可恢复，是否继续？`, '删除租户', {
        type: 'error',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    await api.del(`/platform/tenants/${row.tenantId}`)
    ElMessage.success('租户已删除')
    refreshAll()
  }
}

async function copyText(value: string) {
  if (!value) return
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本')
  }
}

function percent(used: any, limit: any) {
  const l = Number(limit || 0)
  if (!l) return 0
  return Math.min(100, Math.round((Number(used || 0) / l) * 100))
}

function progressColor(percentage: number) {
  if (percentage >= 90) return '#d03050'
  if (percentage >= 70) return '#f0a020'
  return '#1f6feb'
}

function isExpiringSoon(value: any) {
  return dayjs(value).isBefore(dayjs().add(30, 'day'))
}

function featureList(value: any): string[] {
  if (!value) return []
  if (Array.isArray(value)) return value.map((v) => String(v))
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.map((v) => String(v))
  } catch {
    // 非 JSON 时按分隔符解析
  }
  return String(value).split(/[\n,，]/).map((v) => v.trim()).filter(Boolean)
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function number(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function date(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}

function datetime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.toolbar .muted { font-size: 12px; }

.tenant-cell { display: flex; align-items: center; gap: 10px; }
.tenant-avatar {
  width: 34px; height: 34px; border-radius: 10px; flex-shrink: 0;
  background: linear-gradient(135deg, #1f6feb, #14b8a6); color: #fff;
  display: grid; place-items: center; font-weight: 600; font-size: 14px;
}
.tenant-meta { display: flex; flex-direction: column; gap: 3px; min-width: 0;
  strong { font-size: 13px; font-weight: 600; }
  span { font-size: 11px; color: var(--ink-3); }
}
.stack { display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 12.5px; font-weight: 550; }
  span { font-size: 11.5px; color: var(--ink-2); }
  em { font-style: normal; font-size: 10.5px; color: #b25e09; }
}

.quota-cell { display: flex; flex-direction: column; gap: 6px; }
.quota-row { display: flex; align-items: center; gap: 8px;
  .quota-label { font-size: 11px; color: var(--ink-3); width: 24px; flex-shrink: 0; }
  em { font-style: normal; font-size: 11px; color: var(--ink-2); width: 48px; text-align: right; flex-shrink: 0; }
  :deep(.el-progress) { flex: 1; }
}

.wizard-steps { margin: 4px 0 22px; }
.wizard-body { min-height: 310px; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }

.plan-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.plan-card {
  position: relative; border: 1px solid var(--line); border-radius: 12px; padding: 16px;
  background: #fbfcfe; cursor: pointer; transition: all 0.15s;
  &:hover { border-color: #b9d3fa; background: #f5f9ff; }
  &.selected { border-color: #1f6feb; background: #f3f8ff; box-shadow: 0 0 0 3px rgba(31, 111, 235, 0.1); }
}
.plan-head { display: flex; justify-content: space-between; align-items: center; gap: 8px;
  strong { font-size: 14px; }
}
.plan-price { margin: 10px 0 8px; font-size: 24px; font-weight: 680; color: #101828;
  span { font-size: 13px; margin-right: 2px; }
  em { font-style: normal; font-size: 11px; color: var(--ink-3); font-weight: 400; margin-left: 2px; }
}
.plan-features { margin: 0 0 10px; padding: 0; list-style: none;
  li { font-size: 11.5px; color: var(--ink-2); line-height: 1.9; }
}
.plan-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.plan-check {
  position: absolute; right: 12px; top: 12px; color: #1f6feb; font-size: 20px;
  background: #fff; border-radius: 50%;
}

.quota-confirm { margin-top: 18px; border: 1px solid var(--line); border-radius: 10px; overflow: hidden; }
.quota-confirm-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: #f8fafc; border-bottom: 1px solid var(--line);
  h4 { margin: 0; font-size: 13px; }
  span { font-size: 11px; color: var(--ink-3); }
}
.quota-confirm-grid { display: grid; grid-template-columns: repeat(3, 1fr); }
.quota-confirm-item {
  padding: 12px 16px; border-right: 1px dashed #eef1f5; border-bottom: 1px dashed #eef1f5;
  display: flex; flex-direction: column; gap: 6px;
  span { font-size: 11px; color: var(--ink-3); }
  strong { font-size: 14px; }
  :deep(.el-input-number) { width: 120px; }
}

.pwd-row { display: flex; gap: 8px; align-items: center; }
.result-placeholder { height: 280px; }

.drawer-body { min-height: 320px; }
.usage-list { display: flex; flex-direction: column; gap: 14px; }
.usage-item {
  .usage-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; font-size: 12.5px; color: var(--ink-2); }
  em { font-style: normal; font-size: 12px; color: var(--ink-2); }
  em.danger { color: #d03050; font-weight: 600; }
}

.renew-body { display: flex; flex-direction: column; gap: 14px; }
.renew-tenant {
  display: flex; flex-direction: column; gap: 4px; padding: 12px 14px; border-radius: 9px; background: #f8fafc;
  strong { font-size: 13.5px; }
  span { font-size: 11.5px; color: var(--ink-3); }
}
.renew-preview {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border-radius: 9px; background: #eaf2ff; color: #1f6feb;
  span { font-size: 12px; }
  strong { font-size: 16px; }
}

@media (max-width: 900px) {
  .form-grid, .plan-grid, .quota-confirm-grid { grid-template-columns: 1fr; }
}
</style>
