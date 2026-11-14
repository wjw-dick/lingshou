<template>
  <div class="page">
    <PageHeader title="套餐与配额" subtitle="配置 SaaS 套餐的计费周期、资源配额、试用策略与功能清单。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建套餐</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="套餐总数" :value="plans.length" unit="个" icon="Tickets" hint="含已下架" color="#1f6feb" />
      <StatCard label="上架中" :value="activeCount" unit="个" icon="CircleCheck" hint="可用于开通租户" color="#14b8a6" />
      <StatCard label="最低价格" prefix="¥" :value="minPrice" icon="Money" hint="按套餐原价" color="#f0a020" />
      <StatCard label="最大门店配额" :value="maxStore" unit="家" icon="Shop" hint="单套餐门店上限" color="#7c3aed" />
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="套餐名称"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @keyup.enter="load"
          @clear="load"
        />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ plans.length }} 个套餐</span>
      </div>

      <el-table v-loading="loading" :data="plans" row-key="planId" style="width: 100%">
        <el-table-column label="套餐" min-width="180">
          <template #default="{ row }">
            <div class="plan-name">
              <strong>{{ row.name }}</strong>
              <span>{{ row.planCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="150">
          <template #default="{ row }">
            <div class="price-cell">
              <strong>¥{{ money(row.price) }}</strong>
              <span>/ {{ row.billingCycle === 'MONTH' ? '月' : '年' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="资源配额" min-width="200">
          <template #default="{ row }">
            <div class="limit-list">
              <span><em>席位</em>{{ row.seatLimit ?? 0 }}</span>
              <span><em>门店</em>{{ row.storeLimit ?? 0 }}</span>
              <span><em>公众号</em>{{ row.mpAccountLimit ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用量额度" min-width="200">
          <template #default="{ row }">
            <div class="limit-list">
              <span><em>存储</em>{{ row.storageQuotaGb ?? 0 }} GB</span>
              <span><em>短信</em>{{ number(row.smsQuota) }}</span>
              <span><em>API</em>{{ number(row.apiQuota) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="试用" width="86" align="center">
          <template #default="{ row }">{{ row.trialDays ?? 0 }} 天</template>
        </el-table-column>
        <el-table-column label="功能" min-width="200">
          <template #default="{ row }">
            <div class="feature-cell">
              <el-tag v-for="feat in featureList(row.features).slice(0, 3)" :key="feat" size="small" type="info" effect="plain">{{ feat }}</el-tag>
              <el-tag v-if="featureList(row.features).length > 3" size="small" type="info" effect="plain">
                +{{ featureList(row.features).length - 3 }}
              </el-tag>
              <span v-if="!featureList(row.features).length" class="muted">未配置</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <StatusTag :status="row.status" :text="row.status === 'DISABLED' ? '已下架' : '售卖中'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'DISABLED' ? 'success' : 'warning'" @click="toggleStatus(row)">
              {{ row.status === 'DISABLED' ? '上架' : '下架' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无套餐，点击右上角新建" :image-size="80" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.planId ? '编辑套餐' : '新建套餐'" width="720px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="套餐编码" prop="planCode">
            <el-input v-model="form.planCode" placeholder="如 BASIC / STANDARD" maxlength="32" />
          </el-form-item>
          <el-form-item label="套餐名称" prop="name">
            <el-input v-model="form.name" placeholder="如 基础版" maxlength="40" />
          </el-form-item>
          <el-form-item label="套餐价格" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="计费周期" prop="billingCycle">
            <el-select v-model="form.billingCycle" style="width: 100%">
              <el-option label="按年" value="YEAR" />
              <el-option label="按月" value="MONTH" />
            </el-select>
          </el-form-item>
          <el-form-item label="席位上限">
            <el-input-number v-model="form.seatLimit" :min="1" :max="9999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="门店上限">
            <el-input-number v-model="form.storeLimit" :min="1" :max="9999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="公众号上限">
            <el-input-number v-model="form.mpAccountLimit" :min="0" :max="999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="存储配额">
            <el-input-number v-model="form.storageQuotaGb" :min="1" :max="99999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="短信配额">
            <el-input-number v-model="form.smsQuota" :min="0" :max="99999999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="API 配额">
            <el-input-number v-model="form.apiQuota" :min="0" :max="999999999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="试用天数">
            <el-input-number v-model="form.trialDays" :min="0" :max="365" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="套餐状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="售卖中" value="ACTIVE" />
              <el-option label="已下架" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="功能清单">
          <el-input
            v-model="form.featuresText"
            type="textarea"
            :rows="3"
            placeholder="多个功能用换行或逗号分隔，也支持 JSON 数组"
          />
          <div class="form-tip">保存时统一转换为 JSON 数组字符串，例如 ["多端商城","全量报表"]。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const plans = ref<any[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const formRef = ref<any>()

const emptyForm = () => ({
  planId: undefined as number | undefined,
  planCode: '',
  name: '',
  price: 0,
  billingCycle: 'YEAR',
  seatLimit: 10,
  storeLimit: 5,
  mpAccountLimit: 1,
  storageQuotaGb: 10,
  smsQuota: 5000,
  apiQuota: 300000,
  trialDays: 14,
  featuresText: '',
  status: 'ACTIVE'
})
const form = reactive<any>(emptyForm())
const rules = {
  planCode: [{ required: true, message: '请输入套餐编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入套餐价格', trigger: 'blur' }]
}

const activeCount = computed(() => plans.value.filter((p) => p.status !== 'DISABLED').length)
const minPrice = computed(() => (plans.value.length ? Math.min(...plans.value.map((p) => Number(p.price || 0))) : 0))
const maxStore = computed(() => (plans.value.length ? Math.max(...plans.value.map((p) => Number(p.storeLimit || 0))) : 0))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/platform/plans', { keyword: keyword.value || undefined })
    plans.value = Array.isArray(data) ? data : []
  } catch {
    plans.value = []
  } finally {
    loading.value = false
  }
}

function reset() {
  keyword.value = ''
  load()
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, {
    planId: row.planId,
    planCode: row.planCode,
    name: row.name,
    price: Number(row.price || 0),
    billingCycle: row.billingCycle || 'YEAR',
    seatLimit: row.seatLimit ?? 0,
    storeLimit: row.storeLimit ?? 0,
    mpAccountLimit: row.mpAccountLimit ?? 0,
    storageQuotaGb: row.storageQuotaGb ?? 0,
    smsQuota: row.smsQuota ?? 0,
    apiQuota: row.apiQuota ?? 0,
    trialDays: row.trialDays ?? 0,
    featuresText: featureList(row.features).join('\n'),
    status: row.status || 'ACTIVE'
  })
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      planCode: form.planCode.trim(),
      name: form.name.trim(),
      price: form.price,
      billingCycle: form.billingCycle,
      seatLimit: form.seatLimit,
      storeLimit: form.storeLimit,
      mpAccountLimit: form.mpAccountLimit,
      storageQuotaGb: form.storageQuotaGb,
      smsQuota: form.smsQuota,
      apiQuota: form.apiQuota,
      trialDays: form.trialDays,
      features: JSON.stringify(featureList(form.featuresText)),
      status: form.status
    }
    if (form.planId) {
      await api.put(`/platform/plans/${form.planId}`, payload)
      ElMessage.success('套餐已更新')
    } else {
      await api.post('/platform/plans', payload)
      ElMessage.success('套餐已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: any) {
  const next = row.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED'
  try {
    await ElMessageBox.confirm(
      next === 'ACTIVE' ? `确认上架套餐「${row.name}」？` : `下架后新租户将无法选择「${row.name}」，是否继续？`,
      next === 'ACTIVE' ? '上架套餐' : '下架套餐',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await api.put(`/platform/plans/${row.planId}`, planPayload(row, next))
  ElMessage.success(next === 'ACTIVE' ? '套餐已上架' : '套餐已下架')
  load()
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`删除套餐「${row.name}」后不可恢复，是否继续？`, '删除套餐', { type: 'error' })
  } catch {
    return
  }
  await api.del(`/platform/plans/${row.planId}`)
  ElMessage.success('套餐已删除')
  load()
}

function planPayload(row: any, status: string) {
  return {
    planCode: row.planCode,
    name: row.name,
    price: row.price,
    billingCycle: row.billingCycle,
    seatLimit: row.seatLimit,
    storeLimit: row.storeLimit,
    mpAccountLimit: row.mpAccountLimit,
    storageQuotaGb: row.storageQuotaGb,
    smsQuota: row.smsQuota,
    apiQuota: row.apiQuota,
    trialDays: row.trialDays,
    features: row.features,
    status
  }
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
</script>

<style scoped lang="scss">
.toolbar .muted { font-size: 12px; }

.plan-name { display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 13.5px; font-weight: 600; }
  span { font-size: 11px; color: var(--ink-3); letter-spacing: 0.04em; }
}
.price-cell { display: flex; align-items: baseline; gap: 4px;
  strong { font-size: 15px; font-weight: 650; color: #101828; }
  span { font-size: 11px; color: var(--ink-3); }
}
.limit-list { display: flex; flex-direction: column; gap: 4px;
  span { font-size: 11.5px; color: var(--ink-2); }
  em { font-style: normal; color: var(--ink-3); margin-right: 6px; }
}
.feature-cell { display: flex; flex-wrap: wrap; gap: 4px; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
</style>
