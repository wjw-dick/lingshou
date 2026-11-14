<template>
  <div class="page">
    <PageHeader title="用户与角色" subtitle="管理后台账号、门店归属与角色权限，权限变更下次登录生效。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="reloadAll">刷新</el-button>
      </template>
    </PageHeader>

    <div class="card">
      <el-tabs v-model="activeTab" class="main-tabs" @tab-change="onTabChange">
        <el-tab-pane label="用户列表" name="users">
          <div class="toolbar">
            <el-input
              v-model="query.keyword"
              placeholder="账号 / 姓名"
              :prefix-icon="Search"
              clearable
              style="width: 200px"
              @keyup.enter="searchUsers"
            />
            <el-select v-model="query.roleCode" placeholder="全部角色" clearable style="width: 160px">
              <el-option v-for="role in roles" :key="role.roleCode" :label="role.name" :value="role.roleCode" />
            </el-select>
            <el-select v-model="query.storeId" placeholder="全部门店" clearable filterable style="width: 190px">
              <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="searchUsers">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
            <span class="spacer"></span>
            <el-button type="primary" :icon="Plus" @click="openUser()">新增用户</el-button>
          </div>

          <el-table v-loading="loading" :data="users" stripe>
            <el-table-column label="账号" min-width="150">
              <template #default="{ row }">
                <div class="user-cell">
                  <span class="avatar">{{ (row.realName || row.username || '?').slice(0, 1) }}</span>
                  <div>
                    <strong>{{ row.username }}</strong>
                    <span>{{ row.realName || '未填写姓名' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="130">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">{{ row.email || '-' }}</template>
            </el-table-column>
            <el-table-column label="角色" width="130">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ roleName(row.roleCode) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="storeName" label="所属门店" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.storeName || '总部 / 不限' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag :status="row.status === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'" :text="row.status === 'ACTIVE' ? '正常' : '已停用'" />
              </template>
            </el-table-column>
            <el-table-column label="最近登录" width="150">
              <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openUser(row)">编辑</el-button>
                <el-button link type="primary" @click="resetPassword(row)">重置密码</el-button>
                <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggleUser(row)">
                  {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                </el-button>
                <el-button link type="danger" @click="removeUser(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="没有符合条件的用户" :image-size="90" />
            </template>
          </el-table>

          <div class="table-footer">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.size"
              :total="userTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="loadUsers"
              @size-change="searchUsers"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="角色与权限" name="roles">
          <div class="toolbar">
            <div>
              <h3 class="toolbar-title">角色列表</h3>
              <p class="toolbar-sub">权限以 JSON 数组保存，角色变更后用户下次登录生效。</p>
            </div>
            <span class="spacer"></span>
            <el-button type="primary" :icon="Plus" @click="openRole()">新建角色</el-button>
          </div>

          <el-table v-loading="roleLoading" :data="roles" stripe>
            <el-table-column prop="roleCode" label="角色编码" width="160">
              <template #default="{ row }">
                <span class="mono">{{ row.roleCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="角色名称" min-width="140" />
            <el-table-column label="权限" min-width="340">
              <template #default="{ row }">
                <div class="perm-tags">
                  <el-tag v-for="code in parsePermissions(row.permissions).slice(0, 6)" :key="code" size="small" effect="plain">
                    {{ code }}
                  </el-tag>
                  <el-tag v-if="parsePermissions(row.permissions).length > 6" size="small" type="info" effect="plain">
                    +{{ parsePermissions(row.permissions).length - 6 }}
                  </el-tag>
                  <span v-if="!parsePermissions(row.permissions).length" class="muted">未分配权限</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="数据范围" width="120">
              <template #default="{ row }">{{ scopeName(row.dataScope) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag :status="row.status === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openRole(row)">编辑</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无角色" :image-size="90" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="userVisible" :title="editingUser ? '编辑用户' : '新增用户'" width="620px" destroy-on-close>
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="userForm.username" :disabled="!!editingUser" placeholder="3~20 位字母数字下划线" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="editingUser ? '新密码' : '初始密码'" prop="password">
              <el-input
                v-model="userForm.password"
                type="password"
                show-password
                :placeholder="editingUser ? '留空则不修改密码' : '至少 6 位'"
                autocomplete="new-password"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" placeholder="请输入姓名" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" placeholder="11 位手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" placeholder="name@example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleCode">
              <el-select v-model="userForm.roleCode" placeholder="选择角色" style="width: 100%">
                <el-option v-for="role in roles" :key="role.roleCode" :label="role.name" :value="role.roleCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属门店">
              <el-select v-model="userForm.storeId" placeholder="总部 / 不限门店" clearable filterable style="width: 100%">
                <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-select v-model="userForm.status" style="width: 100%">
                <el-option label="正常" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商 ID">
              <el-input-number v-model="userForm.supplierId" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加盟商 ID">
              <el-input-number v-model="userForm.franchiseeId" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="userVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSubmitting" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" :title="editingRole ? '编辑角色' : '新建角色'" width="680px" destroy-on-close>
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="角色编码" prop="roleCode">
              <el-input v-model="roleForm.roleCode" :disabled="!!editingRole" placeholder="如 STORE_MANAGER" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="name">
              <el-input v-model="roleForm.name" placeholder="如 店长" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据范围">
              <el-select v-model="roleForm.dataScope" style="width: 100%">
                <el-option v-for="item in scopes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="roleForm.status" style="width: 100%">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="权限">
          <el-select v-model="roleForm.permissionList" multiple filterable collapse-tags collapse-tags-tooltip placeholder="选择权限" style="width: 100%">
            <el-option-group v-for="group in permissionGroups" :key="group.group" :label="group.group">
              <el-option v-for="item in group.items" :key="item.code" :label="`${item.name}（${item.code}）`" :value="item.code" />
            </el-option-group>
          </el-select>
          <div class="form-tip">已选择 {{ roleForm.permissionList.length }} 项权限，保存后以 JSON 数组写入角色。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const activeTab = ref('users')
const loading = ref(false)
const roleLoading = ref(false)
const userSubmitting = ref(false)
const roleSubmitting = ref(false)
const users = ref<any[]>([])
const userTotal = ref(0)
const roles = ref<any[]>([])
const stores = ref<any[]>([])
const permissions = ref<any[]>([])
const query = ref<any>({ page: 1, size: 10, keyword: '', roleCode: '', storeId: undefined })

const userVisible = ref(false)
const editingUser = ref<any>(null)
const userFormRef = ref()
const userForm = ref<any>({})

const roleVisible = ref(false)
const editingRole = ref<any>(null)
const roleFormRef = ref()
const roleForm = ref<any>({})

const scopes = [
  { value: 'ALL', label: '全部数据' },
  { value: 'STORE', label: '本门店' },
  { value: 'SELF', label: '仅本人' }
]

const permissionGroups = computed(() => {
  const map = new Map<string, any[]>()
  for (const item of permissions.value) {
    if (!map.has(item.group)) map.set(item.group, [])
    map.get(item.group)!.push(item)
  }
  return Array.from(map.entries()).map(([group, items]) => ({ group, items }))
})

const userRules: any = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,20}$/, message: '3~20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    {
      validator(_rule: any, value: string, callback: (error?: Error) => void) {
        if (!editingUser.value && !value) return callback(new Error('请设置初始密码'))
        if (value && value.length < 6) return callback(new Error('密码至少 6 位'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [{ pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const roleRules: any = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{2,39}$/, message: '大写字母开头，可含数字与下划线', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadOptions()])
  await loadUsers()
})

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function roleName(code: string) {
  return roles.value.find((item) => item.roleCode === code)?.name || code || '-'
}

function scopeName(value: string) {
  return scopes.find((item) => item.value === value)?.label || value || '全部数据'
}

function parsePermissions(value: any): string[] {
  if (!value) return []
  try {
    const list = JSON.parse(value)
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await api.get('/tenant/system/users', {
      page: query.value.page,
      size: query.value.size,
      keyword: query.value.keyword || undefined,
      roleCode: query.value.roleCode || undefined,
      storeId: query.value.storeId || undefined
    })
    users.value = result?.records || []
    userTotal.value = Number(result?.total) || 0
  } catch {
    users.value = []
    userTotal.value = 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  roleLoading.value = true
  try {
    roles.value = (await api.get('/tenant/system/roles')) || []
  } catch {
    roles.value = []
  } finally {
    roleLoading.value = false
  }
}

async function loadOptions() {
  try {
    const [permissionRows, storeRows] = await Promise.all([
      api.get('/tenant/system/permissions'),
      api.get('/tenant/stores/options')
    ])
    permissions.value = permissionRows || []
    stores.value = storeRows || []
  } catch {
    permissions.value = []
    stores.value = []
  }
}

function searchUsers() {
  query.value.page = 1
  loadUsers()
}

function resetQuery() {
  query.value = { page: 1, size: query.value.size, keyword: '', roleCode: '', storeId: undefined }
  loadUsers()
}

function reloadAll() {
  loadRoles()
  loadOptions()
  loadUsers()
}

function onTabChange() {
  if (activeTab.value === 'roles') loadRoles()
}

function openUser(row?: any) {
  editingUser.value = row || null
  userForm.value = row
    ? {
        username: row.username,
        password: '',
        realName: row.realName,
        phone: row.phone,
        email: row.email,
        roleCode: row.roleCode,
        storeId: row.storeId,
        supplierId: row.supplierId,
        franchiseeId: row.franchiseeId,
        status: row.status || 'ACTIVE'
      }
    : {
        username: '',
        password: '',
        realName: '',
        phone: '',
        email: '',
        roleCode: '',
        storeId: undefined,
        supplierId: undefined,
        franchiseeId: undefined,
        status: 'ACTIVE'
      }
  userVisible.value = true
}

async function submitUser() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return
  userSubmitting.value = true
  try {
    const payload = { ...userForm.value }
    if (!payload.password) delete payload.password
    if (editingUser.value) {
      await api.put(`/tenant/system/users/${editingUser.value.id}`, payload)
    } else {
      await api.post('/tenant/system/users', payload)
    }
    ElMessage.success('用户已保存')
    userVisible.value = false
    await loadUsers()
  } finally {
    userSubmitting.value = false
  }
}

async function toggleUser(row: any) {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  if (next === 'DISABLED') {
    try {
      await ElMessageBox.confirm(`停用「${row.username}」后该账号将无法登录，确定停用吗？`, '停用账号', {
        type: 'warning',
        confirmButtonText: '停用',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  await api.put(`/tenant/system/users/${row.id}`, { ...row, status: next })
  ElMessage.success(next === 'ACTIVE' ? '账号已启用' : '账号已停用')
  await loadUsers()
}

async function resetPassword(row: any) {
  let value = ''
  try {
    const result = await ElMessageBox.prompt(`为「${row.username}」设置新密码`, '重置密码', {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /^.{6,}$/,
      inputErrorMessage: '密码至少 6 位'
    })
    value = result.value
  } catch {
    return
  }
  await api.put(`/tenant/system/users/${row.id}`, { ...row, password: value })
  ElMessage.success('密码已重置')
}

async function removeUser(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/system/users/${row.id}`)
  ElMessage.success('用户已删除')
  await loadUsers()
}

function openRole(row?: any) {
  editingRole.value = row || null
  roleForm.value = row
    ? {
        roleCode: row.roleCode,
        name: row.name,
        dataScope: row.dataScope || 'ALL',
        status: row.status || 'ACTIVE',
        permissionList: parsePermissions(row.permissions)
      }
    : {
        roleCode: '',
        name: '',
        dataScope: 'ALL',
        status: 'ACTIVE',
        permissionList: []
      }
  roleVisible.value = true
}

async function submitRole() {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  const { permissionList, ...rest } = roleForm.value
  const payload = { ...rest, permissions: JSON.stringify(permissionList || []) }
  roleSubmitting.value = true
  try {
    if (editingRole.value) {
      await api.put(`/tenant/system/roles/${editingRole.value.id}`, payload)
    } else {
      await api.post('/tenant/system/roles', payload)
    }
    ElMessage.success('角色已保存')
    roleVisible.value = false
    await loadRoles()
  } finally {
    roleSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
.main-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 18px;
    border-bottom: 1px solid var(--line);
  }
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  :deep(.el-tabs__content) {
    overflow: visible;
  }
}

.toolbar-title {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
}

.toolbar-sub {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--ink-3);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;

  .avatar {
    width: 30px;
    height: 30px;
    border-radius: 9px;
    background: var(--brand-soft);
    color: var(--brand);
    display: grid;
    place-items: center;
    font-size: 12px;
    font-weight: 650;
    flex-shrink: 0;
  }
  div {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  strong {
    font-size: 12.5px;
    font-weight: 600;
  }
  span {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.mono {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #475467;
}
</style>
