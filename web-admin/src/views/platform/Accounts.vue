<template>
  <div class="page">
    <PageHeader title="平台账号" subtitle="管理平台运营端账号与角色权限，支持密码重置与账号停用。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建账号</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="账号总数" :value="accounts.length" unit="个" icon="User" hint="平台运营端账号" color="#1f6feb" />
      <StatCard label="超级管理员" :value="countRole('PLATFORM_ADMIN')" unit="个" icon="Star" hint="拥有全部平台权限" color="#d03050" />
      <StatCard label="运营人员" :value="countRole('PLATFORM_OPERATOR')" unit="个" icon="UserFilled" hint="租户与内容运营" color="#14b8a6" />
      <StatCard label="财务人员" :value="countRole('PLATFORM_FINANCE')" unit="个" icon="Wallet" hint="账单与回款管理" color="#f0a020" />
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="账号 / 姓名"
          clearable
          style="width: 200px"
          :prefix-icon="Search"
        />
        <el-select v-model="roleFilter" placeholder="全部角色" clearable style="width: 170px">
          <el-option v-for="role in roles" :key="role.value" :label="role.label" :value="role.value" />
        </el-select>
        <div class="spacer"></div>
        <span class="muted">共 {{ filtered.length }} 个账号</span>
      </div>

      <el-table v-loading="loading" :data="filtered" row-key="id" style="width: 100%">
        <el-table-column label="账号" min-width="200">
          <template #default="{ row }">
            <div class="account-cell">
              <div class="account-avatar">{{ (row.realName || row.username || 'A').slice(0, 1) }}</div>
              <div class="account-meta">
                <strong>{{ row.username }}</strong>
                <span>{{ row.realName || '未填写姓名' }}</span>
              </div>
              <el-tag v-if="isSelf(row)" size="small" type="success" effect="plain">当前登录</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-tag effect="plain" :type="roleTagType(row.roleCode)">{{ roleLabel(row.roleCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手机" width="150">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <StatusTag :status="row.status" :text="row.status === 'ACTIVE' ? '正常' : '已停用'" />
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">{{ datetime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ datetime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button link type="danger" :disabled="isSelf(row)" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无平台账号" :image-size="80" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑账号' : '新建账号'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录用户名" maxlength="32" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="form.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="联系手机号" maxlength="20" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" maxlength="32" />
        </el-form-item>
        <el-form-item v-if="form.id" label="账号状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="440px">
      <div class="reset-user">
        <strong>{{ current?.username }}</strong>
        <span>{{ current?.realName || '-' }} · {{ roleLabel(current?.roleCode) }}</span>
      </div>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" show-password placeholder="至少 6 位" maxlength="32" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="resetForm.confirm" type="password" show-password placeholder="再次输入新密码" maxlength="32" />
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" show-icon title="重置后原密码立即失效，请将新密码安全告知该账号使用人。" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const accounts = ref<any[]>([])
const keyword = ref('')
const roleFilter = ref('')

const roles = [
  { value: 'PLATFORM_ADMIN', label: '平台超级管理员' },
  { value: 'PLATFORM_OPERATOR', label: '平台运营' },
  { value: 'PLATFORM_FINANCE', label: '平台财务' }
]

const dialogVisible = ref(false)
const formRef = ref<any>()
const form = reactive<any>({
  id: undefined,
  username: '',
  realName: '',
  roleCode: 'PLATFORM_OPERATOR',
  phone: '',
  password: '',
  status: 'ACTIVE'
})
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

const resetVisible = ref(false)
const resetFormRef = ref<any>()
const current = ref<any>(null)
const resetForm = reactive({ password: '', confirm: '' })
const resetRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (value !== resetForm.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const filtered = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return accounts.value.filter((item) => {
    const matchKeyword = !key
      || String(item.username || '').toLowerCase().includes(key)
      || String(item.realName || '').toLowerCase().includes(key)
    const matchRole = !roleFilter.value || item.roleCode === roleFilter.value
    return matchKeyword && matchRole
  })
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/platform/accounts')
    accounts.value = Array.isArray(data) ? data : []
  } catch {
    accounts.value = []
  } finally {
    loading.value = false
  }
}

function countRole(role: string) {
  return accounts.value.filter((item) => item.roleCode === role).length
}

function isSelf(row: any) {
  return Number(row?.id) === Number(auth.user?.userId)
}

function roleLabel(code: string) {
  return roles.find((role) => role.value === code)?.label || code || '-'
}

function roleTagType(code: string) {
  if (code === 'PLATFORM_ADMIN') return 'danger'
  if (code === 'PLATFORM_FINANCE') return 'warning'
  return 'primary'
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    username: '',
    realName: '',
    roleCode: 'PLATFORM_OPERATOR',
    phone: '',
    password: '',
    status: 'ACTIVE'
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    realName: row.realName || '',
    roleCode: row.roleCode || 'PLATFORM_OPERATOR',
    phone: row.phone || '',
    password: '',
    status: row.status || 'ACTIVE'
  })
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await api.put(`/platform/accounts/${form.id}`, {
        realName: form.realName,
        roleCode: form.roleCode,
        phone: form.phone,
        status: form.status
      })
      ElMessage.success('账号信息已更新')
    } else {
      await api.post('/platform/accounts', {
        username: form.username.trim(),
        realName: form.realName,
        roleCode: form.roleCode,
        phone: form.phone,
        password: form.password
      })
      ElMessage.success('账号已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openReset(row: any) {
  current.value = row
  resetForm.password = ''
  resetForm.confirm = ''
  resetVisible.value = true
}

async function submitReset() {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid || !current.value) return
  resetting.value = true
  try {
    await api.put(`/platform/accounts/${current.value.id}`, {
      realName: current.value.realName,
      roleCode: current.value.roleCode,
      phone: current.value.phone,
      status: current.value.status,
      password: resetForm.password
    })
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    resetting.value = false
  }
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`删除账号「${row.username}」后不可恢复，是否继续？`, '删除账号', { type: 'error' })
  } catch {
    return
  }
  await api.del(`/platform/accounts/${row.id}`)
  ElMessage.success('账号已删除')
  load()
}

function datetime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.toolbar .muted { font-size: 12px; }

.account-cell { display: flex; align-items: center; gap: 10px; }
.account-avatar {
  width: 32px; height: 32px; border-radius: 9px; flex-shrink: 0;
  background: linear-gradient(135deg, #1f6feb, #14b8a6); color: #fff;
  display: grid; place-items: center; font-size: 13px; font-weight: 600;
}
.account-meta { display: flex; flex-direction: column; gap: 2px; min-width: 0;
  strong { font-size: 13px; font-weight: 600; }
  span { font-size: 11px; color: var(--ink-3); }
}

.reset-user {
  display: flex; flex-direction: column; gap: 3px; padding: 12px 14px; margin-bottom: 16px;
  border-radius: 9px; background: #f8fafc;
  strong { font-size: 13.5px; }
  span { font-size: 11.5px; color: var(--ink-3); }
}
</style>
