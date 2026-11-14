<template>
  <div class="page">
    <PageHeader title="营销活动" subtitle="管理折扣、第二件半价等促销活动与活动商品价格。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建活动</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="活动状态" clearable style="width: 150px">
          <el-option label="进行中" value="ACTIVE" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 个活动</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe :row-key="promotionKey">
        <el-table-column label="活动名称" min-width="200">
          <template #default="{ row }">
            <div class="promo-cell">
              <strong>{{ row.name }}</strong>
              <span>{{ rulePreview(row.ruleJson) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="活动类型" width="130">
          <template #default="{ row }">
            <el-tag effect="plain" round size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="210">
          <template #default="{ row }">
            {{ formatTime(row.startAt) }} ~ {{ formatTime(row.endAt) }}
          </template>
        </el-table-column>
        <el-table-column label="门店范围" min-width="180">
          <template #default="{ row }">{{ storeScopeText(row.storeScope) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt, 'YYYY-MM-DD') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无营销活动" :image-size="70" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="活动详情" size="660px">
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="活动名称" :span="2">{{ detail.promotion.name }}</el-descriptions-item>
            <el-descriptions-item label="活动类型">{{ typeLabel(detail.promotion.type) }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :status="detail.promotion.status" /></el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatTime(detail.promotion.startAt) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatTime(detail.promotion.endAt) }}</el-descriptions-item>
            <el-descriptions-item label="门店范围" :span="2">{{ storeScopeText(detail.promotion.storeScope) }}</el-descriptions-item>
            <el-descriptions-item label="规则参数" :span="2">
              <code class="rule-code">{{ detail.promotion.ruleJson || '-' }}</code>
            </el-descriptions-item>
          </el-descriptions>

          <h3 class="section-title" style="margin-top: 20px">活动商品</h3>
          <el-table :data="detail.items || []" stripe size="small">
            <el-table-column label="SKU 编号" width="120">
              <template #default="{ row }">#{{ row.skuId }}</template>
            </el-table-column>
            <el-table-column label="原价" width="120" align="right">
              <template #default="{ row }">¥ {{ money(row.originalPrice) }}</template>
            </el-table-column>
            <el-table-column label="活动价" width="120" align="right">
              <template #default="{ row }">¥ {{ money(row.promoPrice) }}</template>
            </el-table-column>
            <el-table-column label="折扣" align="right">
              <template #default="{ row }">{{ rate(row.discount) }} 折</template>
            </el-table-column>
            <template #empty>
              <el-empty description="该活动未配置商品" :image-size="60" />
            </template>
          </el-table>
        </template>
        <el-empty v-else-if="!detailLoading" description="活动信息加载失败" :image-size="70" />
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" :disabled="!detail" @click="openEdit(detail.promotion)">编辑活动</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="formVisible" :title="form.id ? '编辑活动' : '新建活动'" size="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如：鲜食限时 8 折" />
        </el-form-item>
        <el-form-item label="活动类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="适用门店" prop="storeIds">
          <el-select v-model="form.storeIds" multiple collapse-tags placeholder="不选则默认全部门店" style="width: 100%">
            <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则参数" prop="ruleJson">
          <el-input v-model="form.ruleJson" type="textarea" :rows="3" placeholder='JSON 规则，如 {"discount": 0.8, "categoryId": 1}' />
          <div class="form-tip">用于活动引擎识别折扣比例、分类等规则，需填写合法 JSON 对象。</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">进行中</el-radio>
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div class="item-head">
        <h3 class="section-title">活动商品</h3>
        <el-button type="primary" plain size="small" :icon="Plus" @click="addItem">添加商品</el-button>
      </div>
      <el-table :data="items" stripe size="small">
        <el-table-column label="SKU 编号" width="130">
          <template #default="{ row, $index }">
            <el-input-number v-model="row.skuId" :min="1" :precision="0" :controls="false" placeholder="SKU ID" style="width: 100%" @change="row.skuId = Math.floor(row.skuId || 0) || undefined" />
          </template>
        </el-table-column>
        <el-table-column label="原价" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.originalPrice" :min="0" :precision="2" :controls="false" style="width: 100%" @change="syncDiscount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="活动价" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.promoPrice" :min="0" :precision="2" :controls="false" style="width: 100%" @change="syncDiscount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="折扣" width="90" align="right">
          <template #default="{ row }">{{ rate(row.discount) }} 折</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button text type="danger" size="small" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂未添加活动商品" :image-size="56" />
        </template>
      </el-table>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">保存活动</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<{ status?: string }>({ status: undefined })
const stores = ref<any[]>([])

const typeOptions = [
  { label: '折扣活动', value: 'DISCOUNT' },
  { label: '第二件半价', value: 'SECOND_HALF' },
  { label: '满减活动', value: 'FULL_REDUCTION' },
  { label: '组合套餐', value: 'BUNDLE' },
  { label: '赠品活动', value: 'GIFT' }
]

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const formVisible = ref(false)
const formRef = ref<any>()
const form = reactive<any>({
  id: undefined,
  name: '',
  type: 'DISCOUNT',
  timeRange: [] as string[],
  storeIds: [] as number[],
  ruleJson: '{}',
  status: 'ACTIVE'
})
const items = ref<any[]>([])

const rules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择活动状态', trigger: 'change' }],
  timeRange: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (!value || value.length !== 2) {
          callback(new Error('请选择活动时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  ruleJson: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (!value) {
          callback()
          return
        }
        try {
          const parsed = JSON.parse(value)
          if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
            callback(new Error('规则参数需为 JSON 对象'))
            return
          }
          callback()
        } catch {
          callback(new Error('规则参数不是合法的 JSON'))
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  load()
  loadStores()
})

async function loadStores() {
  try {
    stores.value = await api.get('/tenant/stores/options')
  } catch {
    stores.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const data = await api.get('/tenant/promotions', {
      page: page.value,
      size: size.value,
      status: query.status || undefined
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

function search() {
  page.value = 1
  load()
}

function reset() {
  query.status = undefined
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

function promotionKey(row: any) {
  return row.id
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await api.get(`/tenant/promotions/${row.id}`)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function openCreate() {
  Object.assign(form, {
    id: undefined,
    name: '',
    type: 'DISCOUNT',
    timeRange: [],
    storeIds: [],
    ruleJson: '{}',
    status: 'ACTIVE'
  })
  items.value = []
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function openEdit(row: any) {
  detailVisible.value = false
  try {
    const data = await api.get(`/tenant/promotions/${row.id}`)
    const promotion = data.promotion
    Object.assign(form, {
      id: promotion.id,
      name: promotion.name,
      type: promotion.type,
      timeRange: promotion.startAt && promotion.endAt ? [promotion.startAt, promotion.endAt] : [],
      storeIds: parseStoreScope(promotion.storeScope),
      ruleJson: promotion.ruleJson || '{}',
      status: promotion.status || 'ACTIVE'
    })
    items.value = (data.items || []).map((item: any) => ({
      skuId: item.skuId,
      originalPrice: Number(item.originalPrice || 0),
      promoPrice: Number(item.promoPrice || 0),
      discount: Number(item.discount || 0)
    }))
    formVisible.value = true
    nextTick(() => formRef.value?.clearValidate())
  } catch {
    // handled by interceptor
  }
}

function addItem() {
  items.value.push({ skuId: undefined, originalPrice: 0, promoPrice: 0, discount: 0 })
}

function removeItem(index: number) {
  items.value.splice(index, 1)
}

function syncDiscount(item: any) {
  const original = Number(item.originalPrice || 0)
  const promo = Number(item.promoPrice || 0)
  item.discount = original > 0 ? Number(((promo / original) * 10).toFixed(2)) : 0
}

function submitForm() {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    if (items.value.some((item) => !item.skuId)) {
      ElMessage.warning('请填写活动商品的 SKU 编号')
      return
    }
    if (items.value.some((item) => Number(item.originalPrice) <= 0 || Number(item.promoPrice) <= 0)) {
      ElMessage.warning('活动商品原价与活动价需大于 0')
      return
    }
    if (items.value.some((item) => Number(item.promoPrice) > Number(item.originalPrice))) {
      ElMessage.warning('活动价不能高于原价')
      return
    }
    saving.value = true
    try {
      const payload = {
        promotion: {
          name: form.name,
          type: form.type,
          startAt: form.timeRange?.[0],
          endAt: form.timeRange?.[1],
          ruleJson: form.ruleJson || '{}',
          storeScope: JSON.stringify(form.storeIds || []),
          status: form.status
        },
        items: items.value.map((item) => ({
          skuId: Number(item.skuId),
          originalPrice: Number(item.originalPrice || 0),
          promoPrice: Number(item.promoPrice || 0),
          discount: Number(item.discount || 0)
        }))
      }
      if (form.id) {
        await api.put(`/tenant/promotions/${form.id}`, payload)
        ElMessage.success('活动已更新')
      } else {
        await api.post('/tenant/promotions', payload)
        ElMessage.success('活动已创建')
      }
      formVisible.value = false
      detailVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function parseStoreScope(value: any): number[] {
  if (!value) return []
  if (Array.isArray(value)) return value.map(Number)
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map(Number) : []
  } catch {
    return []
  }
}

function storeScopeText(value: any) {
  const ids = parseStoreScope(value)
  if (!ids.length) return '全部门店'
  const names = ids.map((id) => stores.value.find((store) => store.id === id)?.name || `门店 #${id}`)
  return names.join('、')
}

function rulePreview(value: any) {
  if (!value) return '未配置规则参数'
  return String(value).length > 48 ? `${String(value).slice(0, 48)}…` : String(value)
}

function typeLabel(type: string) {
  return typeOptions.find((item) => item.value === type)?.label || type || '-'
}

function rate(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(value: any, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '-'
}
</script>

<style scoped lang="scss">
.promo-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;

  strong {
    font-size: 12.5px;
    font-weight: 600;
  }

  span {
    font-size: 11px;
    color: var(--ink-3);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.detail-wrap {
  min-height: 260px;
}

.rule-code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11.5px;
  color: #475467;
  background: #f8fafc;
  border-radius: 4px;
  padding: 2px 5px;
}

.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 18px 0 8px;

  .section-title {
    margin: 0;
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
