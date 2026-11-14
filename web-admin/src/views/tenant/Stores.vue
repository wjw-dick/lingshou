<template>
  <div class="page">
    <PageHeader title="门店管理" subtitle="直营与加盟门店档案、营业状态、线上可售、证照与督导稽核。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增门店</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索门店名称 / 编码" clearable style="width: 220px" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.type" placeholder="门店类型" clearable style="width: 140px" @change="search">
          <el-option label="直营" value="DIRECT" />
          <el-option label="加盟" value="FRANCHISE" />
        </el-select>
        <el-select v-model="query.status" placeholder="营业状态" clearable style="width: 140px" @change="search">
          <el-option label="营业中" value="OPEN" />
          <el-option label="暂停营业" value="PAUSED" />
          <el-option label="装修中" value="RENOVATING" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 家门店</span>
      </div>

      <el-table :data="rows" v-loading="loading" stripe row-key="id" style="width: 100%">
        <el-table-column label="门店" min-width="220" fixed>
          <template #default="{ row }">
            <div class="store-cell">
              <div class="store-logo"><el-icon><Shop /></el-icon></div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.code }} · {{ row.type === 'DIRECT' ? '直营' : '加盟' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="区域" width="100" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="managerName" label="店长" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="营业" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="线上可售" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.onlineEnabled === 1"
              :disabled="row.status !== 'OPEN'"
              @change="(v: boolean) => toggleOnline(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="外送" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.deliveryEnabled === 1" type="success" effect="plain" size="small">支持</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="证照到期" width="120">
          <template #default="{ row }">
            <span :class="{ warn: isExpiring(row.licenseExpireAt) }">{{ row.licenseExpireAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openLicenses(row)">证照</el-button>
            <el-button link type="primary" @click="openAudits(row)">稽核</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => onCommand(cmd, row)">
              <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="PAUSED" v-if="row.status === 'OPEN'">暂停营业</el-dropdown-item>
                  <el-dropdown-item command="OPEN" v-else>恢复营业</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除门店</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑门店' : '新增门店'" width="760px" destroy-on-close>
      <el-form :model="form" label-width="96px" label-position="right">
        <div class="form-grid">
          <el-form-item label="门店编码" required>
            <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 S003" />
          </el-form-item>
          <el-form-item label="门店名称" required>
            <el-input v-model="form.name" placeholder="如 悦享便利·人民广场店" />
          </el-form-item>
          <el-form-item label="门店类型">
            <el-radio-group v-model="form.type">
              <el-radio value="DIRECT">直营</el-radio>
              <el-radio value="FRANCHISE">加盟</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="所属区域">
            <el-input v-model="form.region" placeholder="如 静安区" />
          </el-form-item>
          <el-form-item label="营业时间">
            <el-input v-model="form.businessHours" placeholder="07:00-23:00" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item label="店长">
            <el-input v-model="form.managerName" />
          </el-form-item>
          <el-form-item label="店长电话">
            <el-input v-model="form.managerPhone" />
          </el-form-item>
          <el-form-item label="经度">
            <el-input-number v-model="form.lng" :precision="6" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item label="纬度">
            <el-input-number v-model="form.lat" :precision="6" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item label="配送半径">
            <el-input-number v-model="form.deliveryRadiusM" :min="0" :step="500" style="width: 100%" />
            <span class="unit">米</span>
          </el-form-item>
          <el-form-item label="证照到期">
            <el-date-picker v-model="form.licenseExpireAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="营业状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="营业中" value="OPEN" />
              <el-option label="暂停营业" value="PAUSED" />
              <el-option label="装修中" value="RENOVATING" />
            </el-select>
          </el-form-item>
          <el-form-item label="线上可售">
            <el-switch v-model="form.onlineEnabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="支持外送">
            <el-switch v-model="form.deliveryEnabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>
        <el-form-item label="门店地址">
          <el-input v-model="form.address" placeholder="详细地址" />
        </el-form-item>
        <el-form-item label="门店图片">
          <div style="width: 100%">
            <ImageUpload v-model="coverImage" title="门店实拍" hint="建议 16:9 横图，≤5MB" biz-type="STORE" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="licenseDrawer" :title="`证照管理 · ${currentStore?.name || ''}`" size="520px">
      <div class="drawer-actions">
        <el-button type="primary" :icon="Plus" size="small" @click="licenseDialog = true">新增证照</el-button>
      </div>
      <el-table :data="licenses" v-loading="licenseLoading" size="small">
        <el-table-column prop="licenseType" label="类型" width="110">
          <template #default="{ row }">{{ licenseLabel(row.licenseType) }}</template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="expireAt" label="到期日" width="110" />
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="danger" @click="deleteLicense(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!licenses.length && !licenseLoading" description="暂未上传证照" :image-size="70" />
    </el-drawer>

    <el-dialog v-model="licenseDialog" title="新增证照" width="520px" append-to-body>
      <el-form :model="licenseForm" label-width="90px">
        <el-form-item label="证照类型">
          <el-select v-model="licenseForm.licenseType" style="width: 100%">
            <el-option label="食品经营许可证" value="FOOD" />
            <el-option label="烟草专卖许可证" value="TOBACCO" />
            <el-option label="营业执照" value="BUSINESS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="licenseForm.name" /></el-form-item>
        <el-form-item label="到期日">
          <el-date-picker v-model="licenseForm.expireAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="文件">
          <ImageUpload v-model="licenseForm.fileUrl" title="证照文件" hint="支持图片或 PDF，≤5MB" biz-type="STORE_LICENSE" accept="image/*,.pdf" allow-file />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="licenseDialog = false">取消</el-button>
        <el-button type="primary" @click="saveLicense">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="auditDrawer" :title="`稽核记录 · ${currentStore?.name || ''}`" size="620px">
      <div class="drawer-actions">
        <el-button type="primary" :icon="Plus" size="small" @click="auditDialog = true">新增稽核</el-button>
      </div>
      <div class="timeline">
        <div v-for="record in audits" :key="record.id" class="audit-item">
          <div class="audit-head">
            <strong>{{ record.checkDate }}</strong>
            <el-tag :type="record.score >= 90 ? 'success' : 'warning'" effect="plain" size="small">{{ record.score }} 分</el-tag>
          </div>
          <p>督导：{{ record.supervisor || '-' }}</p>
          <p class="muted">{{ record.issues || '无异常记录' }}</p>
        </div>
        <el-empty v-if="!audits.length" description="暂无稽核记录" :image-size="70" />
      </div>
    </el-drawer>

    <el-dialog v-model="auditDialog" title="新增稽核记录" width="520px" append-to-body>
      <el-form :model="auditForm" label-width="90px">
        <el-form-item label="督导"><el-input v-model="auditForm.supervisor" /></el-form-item>
        <el-form-item label="稽核日期">
          <el-date-picker v-model="auditForm.checkDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="得分"><el-input-number v-model="auditForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="问题记录"><el-input v-model="auditForm.issues" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAudit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', type: '', status: '' })
const dialogVisible = ref(false)
const form = ref<any>({})
const coverImage = ref('')

const currentStore = ref<any>(null)
const licenseDrawer = ref(false)
const licenseDialog = ref(false)
const licenseLoading = ref(false)
const licenses = ref<any[]>([])
const licenseForm = reactive<any>({ licenseType: 'FOOD', name: '', expireAt: '', fileUrl: '' })

const auditDrawer = ref(false)
const auditDialog = ref(false)
const audits = ref<any[]>([])
const auditForm = reactive<any>({ supervisor: '', checkDate: dayjs().format('YYYY-MM-DD'), score: 95, issues: '' })

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/tenant/stores', query)
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', type: '', status: '' })
  load()
}

function openCreate() {
  form.value = {
    code: '', name: '', type: 'DIRECT', region: '', address: '', lng: 121.45, lat: 31.23,
    businessHours: '07:00-23:00', phone: '', managerName: '', managerPhone: '',
    deliveryRadiusM: 3000, deliveryEnabled: 1, onlineEnabled: 1, status: 'OPEN', licenseExpireAt: ''
  }
  coverImage.value = ''
  dialogVisible.value = true
}

function openEdit(row: any) {
  form.value = { ...row }
  try {
    const images = row.images ? JSON.parse(row.images) : []
    coverImage.value = images[0] || ''
  } catch {
    coverImage.value = ''
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('请填写门店编码和名称')
    return
  }
  const payload = { ...form.value, images: coverImage.value ? JSON.stringify([coverImage.value]) : null }
  saving.value = true
  try {
    if (payload.id) {
      await api.put(`/tenant/stores/${payload.id}`, payload)
      ElMessage.success('门店已更新')
    } else {
      await api.post('/tenant/stores', payload)
      ElMessage.success('门店已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleOnline(row: any, value: boolean) {
  if (!value) {
    await ElMessageBox.confirm('关闭后该门店将不在小程序/官网展示，确认关闭？', '线上可售', { type: 'warning' })
  }
  await api.post(`/tenant/stores/${row.id}/status`, null, {
    params: { status: row.status, onlineEnabled: value ? 1 : 0 }
  })
  ElMessage.success(value ? '已开启线上可售' : '已关闭线上可售')
  load()
}

async function onCommand(command: string, row: any) {
  if (command === 'delete') {
    await ElMessageBox.confirm(`确认删除门店「${row.name}」？历史业务数据不会被物理删除。`, '删除门店', { type: 'warning' })
    await api.del(`/tenant/stores/${row.id}`)
    ElMessage.success('已删除')
    load()
    return
  }
  await api.post(`/tenant/stores/${row.id}/status`, null, { params: { status: command } })
  ElMessage.success(command === 'PAUSED' ? '已暂停营业' : '已恢复营业')
  load()
}

async function openLicenses(row: any) {
  currentStore.value = row
  licenseDrawer.value = true
  await loadLicenses()
}

async function loadLicenses() {
  if (!currentStore.value) return
  licenseLoading.value = true
  try {
    licenses.value = await api.get(`/tenant/stores/${currentStore.value.id}/licenses`)
  } finally {
    licenseLoading.value = false
  }
}

async function saveLicense() {
  if (!licenseForm.fileUrl) {
    ElMessage.warning('请上传证照文件')
    return
  }
  await api.post(`/tenant/stores/${currentStore.value.id}/licenses`, { ...licenseForm })
  ElMessage.success('证照已保存')
  licenseDialog.value = false
  loadLicenses()
}

async function deleteLicense(row: any) {
  await ElMessageBox.confirm('确认删除该证照记录？', '删除证照', { type: 'warning' })
  await api.del(`/tenant/stores/licenses/${row.id}`)
  ElMessage.success('已删除')
  loadLicenses()
}

async function openAudits(row: any) {
  currentStore.value = row
  auditDrawer.value = true
  audits.value = await api.get(`/tenant/stores/${row.id}/audits`)
}

async function saveAudit() {
  await api.post(`/tenant/stores/${currentStore.value.id}/audits`, { ...auditForm })
  ElMessage.success('稽核记录已保存')
  auditDialog.value = false
  audits.value = await api.get(`/tenant/stores/${currentStore.value.id}/audits`)
}

function isExpiring(date: string) {
  return date && dayjs(date).diff(dayjs(), 'day') <= 30
}

function licenseLabel(type: string) {
  return { FOOD: '食品经营', TOBACCO: '烟草专卖', BUSINESS: '营业执照', OTHER: '其他' }[type] || type
}
</script>

<style scoped lang="scss">
.store-cell { display: flex; align-items: center; gap: 10px; }
.store-logo {
  width: 36px; height: 36px; border-radius: 9px; background: #eef4fe; color: #1f6feb;
  display: grid; place-items: center; flex-shrink: 0;
}
.store-cell div:last-child { display: flex; flex-direction: column; gap: 3px; }
.store-cell strong { font-size: 13px; font-weight: 550; }
.store-cell span { font-size: 11px; color: var(--ink-3); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.unit { margin-left: 8px; color: var(--ink-3); font-size: 12px; }
.warn { color: #d03050; font-weight: 550; }
.drawer-actions { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.timeline { display: flex; flex-direction: column; gap: 12px; }
.audit-item { border: 1px solid var(--line); border-radius: 9px; padding: 14px; background: #fbfcfe; }
.audit-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.audit-item p { margin: 4px 0; font-size: 12px; }
</style>
