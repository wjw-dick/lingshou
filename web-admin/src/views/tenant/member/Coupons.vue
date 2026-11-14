<template>
  <div class="page">
    <PageHeader title="优惠券" subtitle="配置满减、折扣、商品与运费券，并按会员精准发放。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建优惠券</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.type" placeholder="优惠券类型" clearable style="width: 150px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="生效中" value="ACTIVE" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 张优惠券</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe :row-key="(row: any) => row.id">
        <el-table-column label="优惠券" min-width="190">
          <template #default="{ row }">
            <div class="coupon-cell">
              <strong>{{ row.name }}</strong>
              <span>{{ ruleText(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" round size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠规则" min-width="170">
          <template #default="{ row }">
            <span class="muted">{{ ruleText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发放进度" width="170">
          <template #default="{ row }">
            <div class="issue-cell">
              <span>{{ number(row.issuedQty) }} / {{ number(row.totalQty) }}</span>
              <el-progress :percentage="issuePercent(row)" :stroke-width="6" :show-text="false" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="每人限领" width="100" align="center">
          <template #default="{ row }">{{ number(row.perUserLimit) }} 张</template>
        </el-table-column>
        <el-table-column label="有效期" width="190">
          <template #default="{ row }">
            {{ formatDate(row.validFrom) }} ~ {{ formatDate(row.validTo) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text type="primary" size="small" :disabled="row.status !== 'ACTIVE'" @click="openIssue(row)">发放</el-button>
            <el-button text type="danger" size="small" :disabled="row.status !== 'ACTIVE'" @click="disable(row)">停用</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无优惠券" :image-size="70" />
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑优惠券' : '新建优惠券'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如：满 30 减 5 元券" />
        </el-form-item>
        <el-form-item label="券类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button value="FULL_REDUCTION">满减券</el-radio-button>
            <el-radio-button value="DISCOUNT">折扣券</el-radio-button>
            <el-radio-button value="PRODUCT">商品券</el-radio-button>
            <el-radio-button value="FREIGHT">运费券</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type !== 'PRODUCT'" label="使用门槛" prop="thresholdAmount">
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="10" style="width: 100%" />
          <div class="form-tip">订单实付金额达到该门槛方可使用，填 0 表示无门槛。</div>
        </el-form-item>
        <el-form-item v-if="form.type === 'FULL_REDUCTION' || form.type === 'PRODUCT'" label="优惠金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.type === 'DISCOUNT'" label="折扣" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.1" :max="10" :precision="2" :step="0.5" style="width: 100%" />
          <div class="form-tip">按折填写，如 9 表示 9 折，9.5 表示 9.5 折。</div>
        </el-form-item>
        <el-form-item v-if="form.type === 'PRODUCT'" label="商品 SKU" prop="skuId">
          <el-input-number v-model="form.skuId" :min="1" :precision="0" placeholder="请输入商品 SKU 编号" style="width: 100%" />
          <div class="form-tip">商品券仅对指定 SKU 生效。</div>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalQty">
          <el-input-number v-model="form.totalQty" :min="1" :precision="0" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每人限领" prop="perUserLimit">
          <el-input-number v-model="form.perUserLimit" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期" prop="validRange">
          <el-date-picker
            v-model="form.validRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            start-placeholder="生效时间"
            end-placeholder="失效时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">生效中</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueVisible" title="发放优惠券" width="520px">
      <el-form label-width="90px">
        <el-form-item label="优惠券">
          <div class="issue-coupon">
            <strong>{{ issueCoupon?.name }}</strong>
            <span>{{ ruleText(issueCoupon || {}) }}</span>
          </div>
        </el-form-item>
        <el-form-item label="发放范围">
          <el-radio-group v-model="issueForm.mode">
            <el-radio value="ALL">全部有效会员</el-radio>
            <el-radio value="SELECTED">指定会员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="issueForm.mode === 'SELECTED'" label="选择会员">
          <el-select
            v-model="issueForm.memberIds"
            multiple
            filterable
            remote
            reserve-keyword
            :remote-method="searchMembers"
            :loading="memberSearching"
            placeholder="搜索昵称 / 手机号"
            style="width: 100%"
          >
            <el-option
              v-for="member in memberOptions"
              :key="member.id"
              :label="`${member.nickname || '未命名'}（${member.phoneMask || '无手机'}）`"
              :value="member.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitIssue">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const query = reactive<{ type?: string; status?: string }>({ type: undefined, status: undefined })

const typeOptions = [
  { label: '满减券', value: 'FULL_REDUCTION' },
  { label: '折扣券', value: 'DISCOUNT' },
  { label: '商品券', value: 'PRODUCT' },
  { label: '运费券', value: 'FREIGHT' }
]

const formVisible = ref(false)
const formRef = ref<any>()
const form = reactive<any>({
  id: undefined,
  name: '',
  type: 'FULL_REDUCTION',
  thresholdAmount: 0,
  discountAmount: 0,
  discountRate: 10,
  skuId: undefined,
  totalQty: 100,
  perUserLimit: 1,
  validRange: [] as string[],
  status: 'ACTIVE'
})

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠券类型', trigger: 'change' }],
  totalQty: [{ required: true, message: '请输入发放总量', trigger: 'blur' }],
  perUserLimit: [{ required: true, message: '请输入每人限领数量', trigger: 'blur' }],
  skuId: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (form.type === 'PRODUCT' && !value) {
          callback(new Error('请填写商品 SKU 编号'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  discountAmount: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if ((form.type === 'FULL_REDUCTION' || form.type === 'PRODUCT') && !(Number(value) > 0)) {
          callback(new Error('请填写大于 0 的优惠金额'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  discountRate: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (form.type === 'DISCOUNT' && (!(Number(value) > 0) || Number(value) > 10)) {
          callback(new Error('折扣需在 0.1 ~ 10 之间'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  validRange: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (!value || value.length !== 2) {
          callback(new Error('请选择有效期'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const issueVisible = ref(false)
const issueCoupon = ref<any>(null)
const issueForm = reactive<{ mode: string; memberIds: number[] }>({ mode: 'ALL', memberIds: [] })
const memberOptions = ref<any[]>([])
const memberSearching = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await api.get('/tenant/coupons', {
      page: page.value,
      size: size.value,
      type: query.type || undefined,
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
  query.type = undefined
  query.status = undefined
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    name: '',
    type: 'FULL_REDUCTION',
    thresholdAmount: 0,
    discountAmount: 0,
    discountRate: 10,
    skuId: undefined,
    totalQty: 100,
    perUserLimit: 1,
    validRange: [],
    status: 'ACTIVE'
  })
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    thresholdAmount: Number(row.thresholdAmount || 0),
    discountAmount: Number(row.discountAmount || 0),
    discountRate: Number(row.discountRate || 10),
    skuId: row.skuId || undefined,
    totalQty: Number(row.totalQty || 1),
    perUserLimit: Number(row.perUserLimit || 1),
    validRange: row.validFrom && row.validTo ? [row.validFrom, row.validTo] : [],
    status: row.status || 'ACTIVE'
  })
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function submitForm() {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        name: form.name,
        type: form.type,
        thresholdAmount: Number(form.thresholdAmount || 0),
        discountAmount: Number(form.discountAmount || 0),
        discountRate: Number(form.discountRate || 0),
        skuId: form.type === 'PRODUCT' ? form.skuId : null,
        totalQty: Number(form.totalQty || 0),
        perUserLimit: Number(form.perUserLimit || 1),
        validFrom: form.validRange?.[0],
        validTo: form.validRange?.[1],
        status: form.status
      }
      if (form.id) {
        await api.put(`/tenant/coupons/${form.id}`, payload)
        ElMessage.success('优惠券已更新')
      } else {
        await api.post('/tenant/coupons', payload)
        ElMessage.success('优惠券已创建')
      }
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function openIssue(row: any) {
  issueCoupon.value = row
  issueForm.mode = 'ALL'
  issueForm.memberIds = []
  issueVisible.value = true
  searchMembers('')
}

async function searchMembers(keyword: string) {
  memberSearching.value = true
  try {
    const data = await api.get('/tenant/members', {
      page: 1,
      size: 20,
      keyword: keyword || undefined,
      status: 'ACTIVE'
    })
    const list = (data?.records || []).map((item: any) => item.member)
    const map = new Map(memberOptions.value.map((item) => [item.id, item]))
    list.forEach((item: any) => map.set(item.id, item))
    memberOptions.value = Array.from(map.values())
  } catch {
    if (!memberOptions.value.length) memberOptions.value = []
  } finally {
    memberSearching.value = false
  }
}

async function submitIssue() {
  if (!issueCoupon.value) return
  if (issueForm.mode === 'SELECTED' && !issueForm.memberIds.length) {
    ElMessage.warning('请选择发放会员')
    return
  }
  saving.value = true
  try {
    const result = await api.post(`/tenant/coupons/${issueCoupon.value.id}/issue`, {
      all: issueForm.mode === 'ALL' ? true : undefined,
      memberIds: issueForm.mode === 'SELECTED' ? issueForm.memberIds : undefined
    })
    ElMessage.success(`已发放 ${Number(result?.issued || 0)} 张优惠券`)
    issueVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function disable(row: any) {
  try {
    await ElMessageBox.confirm(`停用后「${row.name}」不可再发放，已领取的优惠券不受影响。`, '停用优惠券', {
      type: 'warning',
      confirmButtonText: '确认停用',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.del(`/tenant/coupons/${row.id}`)
    ElMessage.success('优惠券已停用')
    load()
  } catch {
    // handled by interceptor
  }
}

function typeLabel(type: string) {
  return typeOptions.find((item) => item.value === type)?.label || type || '-'
}

function ruleText(row: any) {
  switch (row?.type) {
    case 'FULL_REDUCTION':
      return `满 ¥${money(row.thresholdAmount)} 减 ¥${money(row.discountAmount)}`
    case 'DISCOUNT':
      return `${Number(row.thresholdAmount) > 0 ? `满 ¥${money(row.thresholdAmount)} ` : ''}享 ${rate(row.discountRate)} 折`
    case 'PRODUCT':
      return `指定商品减 ¥${money(row.discountAmount)}`
    case 'FREIGHT':
      return `满 ¥${money(row.thresholdAmount)} 免运费`
    default:
      return '-'
  }
}

function issuePercent(row: any) {
  const totalQty = Number(row?.totalQty || 0)
  if (!totalQty) return 0
  return Math.min(100, Math.round((Number(row?.issuedQty || 0) / totalQty) * 100))
}

function formatDate(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}

function rate(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function number(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped lang="scss">
.coupon-cell {
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
  }
}

.issue-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;

  span {
    font-size: 11.5px;
    color: var(--ink-2);
    font-variant-numeric: tabular-nums;
  }
}

.issue-coupon {
  display: flex;
  flex-direction: column;
  gap: 3px;

  strong {
    font-size: 13px;
    font-weight: 600;
  }

  span {
    font-size: 11.5px;
    color: var(--ink-3);
  }
}
</style>
