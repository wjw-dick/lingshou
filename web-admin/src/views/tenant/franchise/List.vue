<template>
  <div class="page">
    <PageHeader title="加盟商与分账" subtitle="维护加盟商档案、门店绑定、资质与分账记录。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增加盟商</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="加盟商名称 / 编码"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="合作中" value="ACTIVE" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 家加盟商</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe :row-key="franchiseeKey">
        <el-table-column label="加盟商编码" width="120">
          <template #default="{ row }"><span class="strong">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="加盟商" min-width="190">
          <template #default="{ row }">
            <div class="two-line">
              <span class="strong">{{ row.name }}</span>
              <small>{{ row.contact || '-' }} · {{ row.phone || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" round size="small">{{ row.level || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分账方式" width="150">
          <template #default="{ row }">
            <div class="two-line">
              <span>{{ commissionTypeLabel(row.commissionType) }}</span>
              <small>{{ commissionValueText(row.commissionType, row.commissionValue) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="授信使用" width="160">
          <template #default="{ row }">
            <div class="credit-cell">
              <span>¥ {{ money(row.usedCredit) }} / ¥ {{ money(row.creditLimit) }}</span>
              <el-progress :percentage="creditPercent(row)" :stroke-width="6" :show-text="false" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="合同" min-width="190">
          <template #default="{ row }">
            <div class="two-line">
              <span>{{ row.contractNo || '-' }}</span>
              <small>{{ row.contractStart || '-' }} ~ {{ row.contractEnd || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无加盟商" :image-size="70" />
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

    <el-drawer v-model="detailVisible" title="加盟商详情" size="780px">
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <div class="profile">
            <div class="profile-logo">{{ (detail.franchisee.name || '加').slice(0, 1) }}</div>
            <div class="profile-main">
              <h3>{{ detail.franchisee.name }}</h3>
              <p>{{ detail.franchisee.code }} · {{ detail.franchisee.contact || '-' }} · {{ detail.franchisee.phone || '-' }}</p>
            </div>
            <StatusTag :status="detail.franchisee.status" />
          </div>

          <el-descriptions :column="2" border size="small" style="margin-top: 16px">
            <el-descriptions-item label="加盟等级">{{ detail.franchisee.level || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分账方式">
              {{ commissionTypeLabel(detail.franchisee.commissionType) }}（{{ commissionValueText(detail.franchisee.commissionType, detail.franchisee.commissionValue) }}）
            </el-descriptions-item>
            <el-descriptions-item label="授信额度">¥ {{ money(detail.franchisee.creditLimit) }}</el-descriptions-item>
            <el-descriptions-item label="已用授信">¥ {{ money(detail.franchisee.usedCredit) }}</el-descriptions-item>
            <el-descriptions-item label="保证金">¥ {{ money(detail.franchisee.deposit) }}</el-descriptions-item>
            <el-descriptions-item label="合同编号">{{ detail.franchisee.contractNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同期限" :span="2">
              {{ detail.franchisee.contractStart || '-' }} ~ {{ detail.franchisee.contractEnd || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-tabs v-model="detailTab" style="margin-top: 12px">
            <el-tab-pane label="绑定门店" name="stores">
              <div class="tab-head">
                <span class="muted">共 {{ (detail.stores || []).length }} 家门店</span>
                <el-button type="primary" plain size="small" :icon="Plus" @click="openBind">绑定门店</el-button>
              </div>
              <el-table :data="detail.stores || []" stripe size="small">
                <el-table-column label="门店编码" width="120">
                  <template #default="{ row }">{{ row.storeCode || '-' }}</template>
                </el-table-column>
                <el-table-column label="门店名称" min-width="160">
                  <template #default="{ row }">{{ row.storeName || '-' }}</template>
                </el-table-column>
                <el-table-column label="加盟日期" width="120">
                  <template #default="{ row }">{{ row.relation.joinDate || '-' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }"><StatusTag :status="row.relation.status" /></template>
                </el-table-column>
                <el-table-column label="操作" width="90" align="center">
                  <template #default="{ row }">
                    <el-button text type="danger" size="small" @click="unbindStore(row.relation)">解绑</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂未绑定门店" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="分账记录" name="commissions">
              <el-table :data="commissions" stripe size="small" max-height="380">
                <el-table-column label="账期" width="100">
                  <template #default="{ row }">{{ row.period }}</template>
                </el-table-column>
                <el-table-column label="销售额" width="130" align="right">
                  <template #default="{ row }">¥ {{ money(row.salesAmount) }}</template>
                </el-table-column>
                <el-table-column label="分账方式" width="150">
                  <template #default="{ row }">
                    {{ commissionTypeLabel(row.commissionType) }} {{ commissionValueText(row.commissionType, row.commissionValue) }}
                  </template>
                </el-table-column>
                <el-table-column label="分账金额" width="130" align="right">
                  <template #default="{ row }"><span class="money">¥ {{ money(row.commissionAmount) }}</span></template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }"><StatusTag :status="row.status" /></template>
                </el-table-column>
                <el-table-column label="生成时间" width="140">
                  <template #default="{ row }">{{ formatTime(row.createdAt, 'YYYY-MM-DD') }}</template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无分账记录" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="资质证照" name="qualifications">
              <div class="tab-head">
                <span class="muted">共 {{ (detail.qualifications || []).length }} 份资质</span>
                <el-button type="primary" plain size="small" :icon="Plus" @click="openQualification">添加资质</el-button>
              </div>
              <el-table :data="detail.qualifications || []" stripe size="small">
                <el-table-column label="资质类型" width="150">
                  <template #default="{ row }">{{ qualificationTypeLabel(row.type) }}</template>
                </el-table-column>
                <el-table-column label="文件" min-width="180">
                  <template #default="{ row }">
                    <el-link v-if="row.fileUrl" type="primary" :href="row.fileUrl" target="_blank">{{ fileName(row.fileUrl) }}</el-link>
                    <span v-else class="muted">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="到期日" width="120">
                  <template #default="{ row }">{{ row.expireAt || '-' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }"><StatusTag :status="row.status" /></template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无资质证照" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else-if="!detailLoading" description="加盟商信息加载失败" :image-size="70" />
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" :disabled="!detail" @click="openEdit(detail.franchisee)">编辑加盟商</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑加盟商' : '新增加盟商'" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="加盟商编码" prop="code">
              <el-input v-model="form.code" maxlength="32" placeholder="如：FR002" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加盟商名称" prop="name">
              <el-input v-model="form.name" maxlength="64" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="form.contact" maxlength="32" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" maxlength="20" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加盟等级" prop="level">
              <el-select v-model="form.level" placeholder="请选择等级" style="width: 100%">
                <el-option label="A 级" value="A" />
                <el-option label="B 级" value="B" />
                <el-option label="C 级" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio value="ACTIVE">合作中</el-radio>
                <el-radio value="DISABLED">已停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分账方式" prop="commissionType">
              <el-select v-model="form.commissionType" style="width: 100%">
                <el-option label="按销售额比例" value="SALES" />
                <el-option label="固定金额" value="FIXED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分账数值" prop="commissionValue">
              <el-input-number v-model="form.commissionValue" :min="0" :precision="2" :step="1" style="width: 100%" />
              <div class="form-tip">{{ form.commissionType === 'FIXED' ? '单位：元' : '单位：百分比（如 5 表示 5%）' }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授信额度" prop="creditLimit">
              <el-input-number v-model="form.creditLimit" :min="0" :precision="2" :step="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保证金" prop="deposit">
              <el-input-number v-model="form.deposit" :min="0" :precision="2" :step="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同编号" prop="contractNo">
              <el-input v-model="form.contractNo" maxlength="64" placeholder="请输入合同编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同开始" prop="contractStart">
              <el-date-picker v-model="form.contractStart" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同结束" prop="contractEnd">
              <el-date-picker v-model="form.contractEnd" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindVisible" title="绑定门店" width="460px">
      <el-form label-width="80px">
        <el-form-item label="门店">
          <el-select v-model="bindStoreId" filterable placeholder="请选择门店" style="width: 100%">
            <el-option
              v-for="store in stores"
              :key="store.id"
              :label="`${store.name}（${store.code}）`"
              :value="store.id"
              :disabled="boundStoreIds.includes(store.id)"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitBind">确认绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qualVisible" title="添加资质" width="560px">
      <el-form ref="qualRef" :model="qualForm" :rules="qualRules" label-width="90px">
        <el-form-item label="资质类型" prop="type">
          <el-select v-model="qualForm.type" style="width: 100%">
            <el-option v-for="item in qualificationTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="资质文件" prop="fileUrl">
          <div style="width: 100%">
            <ImageUpload v-model="qualForm.fileUrl" title="资质文件" hint="支持图片或 PDF，单个文件 ≤5MB" biz-type="FRANCHISE_QUAL" accept="image/*,.pdf" allow-file />
          </div>
        </el-form-item>
        <el-form-item label="到期日期" prop="expireAt">
          <el-date-picker v-model="qualForm.expireAt" type="date" value-format="YYYY-MM-DD" placeholder="请选择到期日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="qualForm.status">
            <el-radio value="VALID">有效</el-radio>
            <el-radio value="EXPIRED">已过期</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qualVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitQualification">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const stores = ref<any[]>([])
const query = reactive<{ keyword: string; status?: string }>({ keyword: '', status: undefined })

const formVisible = ref(false)
const formRef = ref<any>()
const form = reactive<any>({
  id: undefined,
  code: '',
  name: '',
  contact: '',
  phone: '',
  level: 'A',
  commissionType: 'SALES',
  commissionValue: 5,
  creditLimit: 0,
  deposit: 0,
  contractNo: '',
  contractStart: '',
  contractEnd: '',
  status: 'ACTIVE'
})

const rules = {
  code: [
    { required: true, message: '请输入加盟商编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{2,32}$/, message: '编码仅支持字母、数字、下划线和中划线', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入加盟商名称', trigger: 'blur' }],
  phone: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value && !/^1[3-9]\d{9}$/.test(value)) {
          callback(new Error('请输入正确的手机号'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  commissionType: [{ required: true, message: '请选择分账方式', trigger: 'change' }],
  commissionValue: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (!(Number(value) > 0)) {
          callback(new Error('分账数值必须大于 0'))
          return
        }
        if (form.commissionType === 'SALES' && Number(value) > 100) {
          callback(new Error('分账比例不能超过 100%'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  contractEnd: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value && form.contractStart && dayjs(value).isBefore(dayjs(form.contractStart), 'day')) {
          callback(new Error('合同结束日期不能早于开始日期'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const detailTab = ref('stores')
const commissions = ref<any[]>([])

const bindVisible = ref(false)
const bindStoreId = ref<number>()
const boundStoreIds = computed<number[]>(() => (detail.value?.stores || []).map((item: any) => item.relation.storeId))

const qualVisible = ref(false)
const qualRef = ref<any>()
const qualForm = reactive<any>({ type: 'CONTRACT', fileUrl: '', expireAt: '', status: 'VALID' })
const qualRules = {
  type: [{ required: true, message: '请选择资质类型', trigger: 'change' }],
  fileUrl: [{ required: true, message: '请上传资质文件', trigger: 'change' }]
}

const qualificationTypes = [
  { label: '加盟合同', value: 'CONTRACT' },
  { label: '营业执照', value: 'BUSINESS_LICENSE' },
  { label: '食品经营许可证', value: 'FOOD_LICENSE' },
  { label: '其他资质', value: 'OTHER' }
]

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
    const data = await api.get('/tenant/franchisees', {
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
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
  query.keyword = ''
  query.status = undefined
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

function franchiseeKey(row: any) {
  return row.id
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailTab.value = 'stores'
  detailLoading.value = true
  detail.value = null
  commissions.value = []
  try {
    detail.value = await api.get(`/tenant/franchisees/${row.id}`)
    await loadCommissions(row.id)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function loadCommissions(id: number) {
  try {
    commissions.value = await api.get(`/tenant/franchisees/${id}/commissions`)
  } catch {
    commissions.value = []
  }
}

async function reloadDetail() {
  if (!detail.value) return
  const id = detail.value.franchisee.id
  try {
    detail.value = await api.get(`/tenant/franchisees/${id}`)
    await loadCommissions(id)
  } catch {
    // ignore
  }
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    code: '',
    name: '',
    contact: '',
    phone: '',
    level: 'A',
    commissionType: 'SALES',
    commissionValue: 5,
    creditLimit: 0,
    deposit: 0,
    contractNo: '',
    contractStart: '',
    contractEnd: '',
    status: 'ACTIVE'
  })
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(franchisee: any) {
  Object.assign(form, {
    id: franchisee.id,
    code: franchisee.code || '',
    name: franchisee.name || '',
    contact: franchisee.contact || '',
    phone: franchisee.phone || '',
    level: franchisee.level || 'A',
    commissionType: franchisee.commissionType || 'SALES',
    commissionValue: Number(franchisee.commissionValue || 0),
    creditLimit: Number(franchisee.creditLimit || 0),
    deposit: Number(franchisee.deposit || 0),
    contractNo: franchisee.contractNo || '',
    contractStart: franchisee.contractStart || '',
    contractEnd: franchisee.contractEnd || '',
    status: franchisee.status || 'ACTIVE'
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
        code: form.code,
        name: form.name,
        contact: form.contact,
        phone: form.phone,
        level: form.level,
        commissionType: form.commissionType,
        commissionValue: Number(form.commissionValue || 0),
        creditLimit: Number(form.creditLimit || 0),
        deposit: Number(form.deposit || 0),
        contractNo: form.contractNo,
        contractStart: form.contractStart || null,
        contractEnd: form.contractEnd || null,
        status: form.status
      }
      if (form.id) {
        await api.put(`/tenant/franchisees/${form.id}`, payload)
        ElMessage.success('加盟商已更新')
      } else {
        await api.post('/tenant/franchisees', payload)
        ElMessage.success('加盟商已创建')
      }
      formVisible.value = false
      load()
      reloadDetail()
    } finally {
      saving.value = false
    }
  })
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`删除后「${row.name}」将不再显示在加盟商列表中，确认删除？`, '删除加盟商', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.del(`/tenant/franchisees/${row.id}`)
    ElMessage.success('加盟商已删除')
    load()
  } catch {
    // handled by interceptor
  }
}

function openBind() {
  bindStoreId.value = undefined
  bindVisible.value = true
}

async function submitBind() {
  if (!bindStoreId.value) {
    ElMessage.warning('请选择要绑定的门店')
    return
  }
  saving.value = true
  try {
    await api.post(`/tenant/franchisees/${detail.value.franchisee.id}/stores`, null, {
      params: { storeId: bindStoreId.value }
    })
    ElMessage.success('门店绑定成功')
    bindVisible.value = false
    reloadDetail()
  } finally {
    saving.value = false
  }
}

async function unbindStore(relation: any) {
  try {
    await ElMessageBox.confirm('解绑后该门店将不再参与此加盟商的分账，确认解绑？', '解绑门店', {
      type: 'warning',
      confirmButtonText: '确认解绑',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.del(`/tenant/franchisees/stores/${relation.id}`)
    ElMessage.success('门店已解绑')
    reloadDetail()
  } catch {
    // handled by interceptor
  }
}

function openQualification() {
  Object.assign(qualForm, { type: 'CONTRACT', fileUrl: '', expireAt: '', status: 'VALID' })
  qualVisible.value = true
  nextTick(() => qualRef.value?.clearValidate())
}

function submitQualification() {
  qualRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      await api.post(`/tenant/franchisees/${detail.value.franchisee.id}/qualifications`, {
        type: qualForm.type,
        fileUrl: qualForm.fileUrl,
        expireAt: qualForm.expireAt || null,
        status: qualForm.status
      })
      ElMessage.success('资质已添加')
      qualVisible.value = false
      reloadDetail()
    } finally {
      saving.value = false
    }
  })
}

function commissionTypeLabel(type: string) {
  const map: Record<string, string> = { SALES: '按销售额比例', FIXED: '固定金额' }
  return map[type] || type || '-'
}

function commissionValueText(type: string, value: any) {
  if (type === 'FIXED') return `¥ ${money(value)}`
  return `${Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}%`
}

function creditPercent(row: any) {
  const limit = Number(row.creditLimit || 0)
  if (!limit) return 0
  return Math.min(100, Math.round((Number(row.usedCredit || 0) / limit) * 100))
}

function qualificationTypeLabel(type: string) {
  return qualificationTypes.find((item) => item.value === type)?.label || type || '-'
}

function fileName(url: string) {
  const name = String(url).split('/').pop() || '查看文件'
  return name.length > 26 ? `${name.slice(0, 26)}…` : name
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(value: any, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '-'
}
</script>

<style scoped lang="scss">
.two-line {
  display: flex;
  flex-direction: column;
  gap: 2px;

  span {
    font-size: 12.5px;
  }

  small {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.credit-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;

  span {
    font-size: 11.5px;
    color: var(--ink-2);
    font-variant-numeric: tabular-nums;
  }
}

.detail-wrap {
  min-height: 320px;
}

.profile {
  display: flex;
  align-items: center;
  gap: 14px;

  .profile-logo {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    font-size: 20px;
    font-weight: 650;
    color: #fff;
    background: linear-gradient(135deg, #7c3aed, #1f6feb);
    flex-shrink: 0;
  }

  .profile-main {
    flex: 1;
    min-width: 0;

    h3 {
      margin: 0;
      font-size: 17px;
      font-weight: 650;
    }

    p {
      margin: 5px 0 0;
      font-size: 12px;
      color: var(--ink-3);
    }
  }
}

.tab-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
