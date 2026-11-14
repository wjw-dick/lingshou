<template>
  <div class="page">
    <PageHeader title="供应商" subtitle="维护供应商档案、资质证照与供货商品审核。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增供应商</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="供应商名称 / 编码"
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
        <span class="muted">共 {{ total }} 家供应商</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe :row-key="supplierKey">
        <el-table-column label="供应商编码" width="120">
          <template #default="{ row }"><span class="strong">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="供应商名称" min-width="210">
          <template #default="{ row }">
            <div class="supplier-cell">
              <strong>{{ row.name }}</strong>
              <span>{{ row.category || '未分类' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系人" width="150">
          <template #default="{ row }">
            <div class="two-line">
              <span>{{ row.contact || '-' }}</span>
              <small>{{ row.phone || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结算周期" width="110" align="right">
          <template #default="{ row }">{{ number(row.settleCycleDays) }} 天</template>
        </el-table-column>
        <el-table-column label="授信额度" width="130" align="right">
          <template #default="{ row }"><span class="money">¥ {{ money(row.creditLimit) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt, 'YYYY-MM-DD') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无供应商" :image-size="70" />
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

    <el-drawer v-model="detailVisible" title="供应商详情" size="760px">
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <div class="profile">
            <div class="profile-logo">{{ (detail.supplier.name || '供').slice(0, 1) }}</div>
            <div class="profile-main">
              <h3>{{ detail.supplier.name }}</h3>
              <p>{{ detail.supplier.code }} · {{ detail.supplier.category || '未分类' }}</p>
            </div>
            <StatusTag :status="detail.supplier.status" />
          </div>

          <el-descriptions :column="2" border size="small" style="margin-top: 16px">
            <el-descriptions-item label="联系人">{{ detail.supplier.contact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.supplier.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结算周期">{{ number(detail.supplier.settleCycleDays) }} 天</el-descriptions-item>
            <el-descriptions-item label="授信额度">¥ {{ money(detail.supplier.creditLimit) }}</el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">{{ detail.supplier.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ formatTime(detail.supplier.createdAt) }}</el-descriptions-item>
          </el-descriptions>

          <el-tabs v-model="detailTab" style="margin-top: 12px">
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
                <el-table-column label="操作" width="80" align="center">
                  <template #default="{ row }">
                    <el-button text type="danger" size="small" @click="removeQualification(row)">删除</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无资质证照" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="供货商品" name="products">
              <el-table :data="detail.products || []" stripe size="small">
                <el-table-column label="SKU 编码" width="110">
                  <template #default="{ row }">{{ row.skuCode || '-' }}</template>
                </el-table-column>
                <el-table-column label="商品名称" min-width="150">
                  <template #default="{ row }">{{ row.skuName || '-' }}</template>
                </el-table-column>
                <el-table-column label="条码" width="140">
                  <template #default="{ row }">{{ row.barcode || '-' }}</template>
                </el-table-column>
                <el-table-column label="供货价" width="110" align="right">
                  <template #default="{ row }">¥ {{ money(row.product.supplyPrice) }}</template>
                </el-table-column>
                <el-table-column label="审核状态" width="100">
                  <template #default="{ row }"><StatusTag :status="row.product.auditStatus" /></template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button
                      text
                      type="primary"
                      size="small"
                      :disabled="row.product.auditStatus === 'APPROVED'"
                      @click="auditProduct(row, 'APPROVED')"
                    >通过</el-button>
                    <el-button
                      text
                      type="danger"
                      size="small"
                      :disabled="row.product.auditStatus === 'REJECTED'"
                      @click="auditProduct(row, 'REJECTED')"
                    >驳回</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无供货商品" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else-if="!detailLoading" description="供应商信息加载失败" :image-size="70" />
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" :disabled="!detail" @click="openEdit(detail.supplier)">编辑供应商</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑供应商' : '新增供应商'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="供应商编码" prop="code">
          <el-input v-model="form.code" maxlength="32" placeholder="如：SUP003" />
        </el-form-item>
        <el-form-item label="供应商名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="请输入供应商全称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" maxlength="32" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="20" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="主营品类" prop="category">
          <el-input v-model="form.category" maxlength="64" placeholder="如：鲜食/冷链" />
        </el-form-item>
        <el-form-item label="联系地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" maxlength="128" placeholder="请输入联系地址" />
        </el-form-item>
        <el-form-item label="结算周期" prop="settleCycleDays">
          <el-input-number v-model="form.settleCycleDays" :min="0" :precision="0" :step="5" style="width: 100%" />
          <span class="form-tip">单位：天，0 表示现结。</span>
        </el-form-item>
        <el-form-item label="授信额度" prop="creditLimit">
          <el-input-number v-model="form.creditLimit" :min="0" :precision="2" :step="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">合作中</el-radio>
            <el-radio value="DISABLED">已停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
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
            <ImageUpload v-model="qualForm.fileUrl" title="资质文件" hint="支持图片或 PDF，单个文件 ≤5MB" biz-type="SUPPLIER_QUAL" accept="image/*,.pdf" allow-file />
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
import { nextTick, onMounted, reactive, ref } from 'vue'
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
const query = reactive<{ keyword: string; status?: string }>({ keyword: '', status: undefined })

const formVisible = ref(false)
const formRef = ref<any>()
const form = reactive<any>({
  id: undefined,
  code: '',
  name: '',
  contact: '',
  phone: '',
  address: '',
  category: '',
  settleCycleDays: 30,
  creditLimit: 0,
  status: 'ACTIVE'
})

const rules = {
  code: [
    { required: true, message: '请输入供应商编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{2,32}$/, message: '编码仅支持字母、数字、下划线和中划线', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
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
  creditLimit: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (Number(value) < 0) {
          callback(new Error('授信额度不能为负数'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const detailTab = ref('qualifications')

const qualVisible = ref(false)
const qualRef = ref<any>()
const qualForm = reactive<any>({ type: 'BUSINESS_LICENSE', fileUrl: '', expireAt: '', status: 'VALID' })

const qualRules = {
  type: [{ required: true, message: '请选择资质类型', trigger: 'change' }],
  fileUrl: [{ required: true, message: '请上传资质文件', trigger: 'change' }]
}

const qualificationTypes = [
  { label: '营业执照', value: 'BUSINESS_LICENSE' },
  { label: '食品生产许可证', value: 'FOOD_PRODUCTION' },
  { label: '食品经营许可证', value: 'FOOD_BUSINESS' },
  { label: '检验检测报告', value: 'INSPECTION' },
  { label: '其他资质', value: 'OTHER' }
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await api.get('/tenant/suppliers', {
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

function supplierKey(row: any) {
  return row.id
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailTab.value = 'qualifications'
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await api.get(`/tenant/suppliers/${row.id}`)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function reloadDetail() {
  if (!detail.value) return
  try {
    detail.value = await api.get(`/tenant/suppliers/${detail.value.supplier.id}`)
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
    address: '',
    category: '',
    settleCycleDays: 30,
    creditLimit: 0,
    status: 'ACTIVE'
  })
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(supplier: any) {
  Object.assign(form, {
    id: supplier.id,
    code: supplier.code || '',
    name: supplier.name || '',
    contact: supplier.contact || '',
    phone: supplier.phone || '',
    address: supplier.address || '',
    category: supplier.category || '',
    settleCycleDays: Number(supplier.settleCycleDays || 0),
    creditLimit: Number(supplier.creditLimit || 0),
    status: supplier.status || 'ACTIVE'
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
        address: form.address,
        category: form.category,
        settleCycleDays: Number(form.settleCycleDays || 0),
        creditLimit: Number(form.creditLimit || 0),
        status: form.status
      }
      if (form.id) {
        await api.put(`/tenant/suppliers/${form.id}`, payload)
        ElMessage.success('供应商已更新')
      } else {
        await api.post('/tenant/suppliers', payload)
        ElMessage.success('供应商已创建')
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
    await ElMessageBox.confirm(`删除后「${row.name}」将不再显示在供应商列表中，确认删除？`, '删除供应商', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.del(`/tenant/suppliers/${row.id}`)
    ElMessage.success('供应商已删除')
    load()
  } catch {
    // handled by interceptor
  }
}

function openQualification() {
  Object.assign(qualForm, { type: 'BUSINESS_LICENSE', fileUrl: '', expireAt: '', status: 'VALID' })
  qualVisible.value = true
  nextTick(() => qualRef.value?.clearValidate())
}

function submitQualification() {
  qualRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      await api.post(`/tenant/suppliers/${detail.value.supplier.id}/qualifications`, {
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

async function removeQualification(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该资质证照？', '删除资质', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.del(`/tenant/suppliers/qualifications/${row.id}`)
    ElMessage.success('资质已删除')
    reloadDetail()
  } catch {
    // handled by interceptor
  }
}

async function auditProduct(row: any, status: string) {
  const action = status === 'APPROVED' ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(`确认${action}商品「${row.skuName}」的供货审核？`, '商品审核', {
      type: 'warning',
      confirmButtonText: `确认${action}`,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await api.post(`/tenant/suppliers/products/${row.product.id}/audit`, null, { params: { status } })
    ElMessage.success(`商品已${action}`)
    reloadDetail()
  } catch {
    // handled by interceptor
  }
}

function qualificationTypeLabel(type: string) {
  return qualificationTypes.find((item) => item.value === type)?.label || type || '-'
}

function fileName(url: string) {
  const name = String(url).split('/').pop() || '查看文件'
  return name.length > 26 ? `${name.slice(0, 26)}…` : name
}

function number(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(value: any, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '-'
}
</script>

<style scoped lang="scss">
.supplier-cell {
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

.detail-wrap {
  min-height: 300px;
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
    background: linear-gradient(135deg, #1f6feb, #14b8a6);
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
