<template>
  <div class="page">
    <PageHeader title="多语言" subtitle="维护前台与小程序可用语言，默认语言在全局参数中配置。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增语言</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="code" label="语言代码" min-width="140">
          <template #default="{ row }">
            <span class="code-pill">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="语言名称" min-width="160" />
        <el-table-column prop="sortNo" label="排序" width="100" align="center" />
        <el-table-column label="启用状态" width="120" align="center">
          <template #default="{ row }">
            <StatusTag :status="Number(row.enabled) === 1 ? 'ACTIVE' : 'DISABLED'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="toggle(row)">
              {{ Number(row.enabled) === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无语言配置，点击右上角新增" :image-size="90" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑语言' : '新增语言'" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="语言代码" prop="code">
          <el-input v-model="form.code" placeholder="如 zh-CN / en-US" :disabled="!!editing" maxlength="20" />
        </el-form-item>
        <el-form-item label="语言名称" prop="name">
          <el-input v-model="form.name" placeholder="如 简体中文 / English" maxlength="40" />
        </el-form-item>
        <el-form-item label="排序" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editing = ref<any>(null)
const rows = ref<any[]>([])
const formRef = ref()
const form = ref<any>({ code: '', name: '', sortNo: 10, enabled: 1 })

const rules = {
  code: [
    { required: true, message: '请输入语言代码', trigger: 'blur' },
    { pattern: /^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?$/, message: '格式如 zh-CN、en-US', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入语言名称', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    rows.value = (await api.get('/tenant/system/languages')) || []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { code: '', name: '', sortNo: (rows.value.length + 1) * 10, enabled: 1 }
  dialogVisible.value = true
}

function openEdit(row: any) {
  editing.value = row
  form.value = {
    code: row.code,
    name: row.name,
    sortNo: Number(row.sortNo) || 0,
    enabled: Number(row.enabled) ?? 1
  }
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editing.value) {
      await api.put(`/tenant/system/languages/${editing.value.id}`, form.value)
    } else {
      await api.post('/tenant/system/languages', form.value)
    }
    ElMessage.success('语言配置已保存')
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggle(row: any) {
  const next = Number(row.enabled) === 1 ? 0 : 1
  await api.put(`/tenant/system/languages/${row.id}`, { ...row, enabled: next })
  ElMessage.success(next ? '已启用' : '已停用')
  await load()
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除语言「${row.name}」吗？删除后前台将不再提供该语言。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/system/languages/${row.id}`)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped lang="scss">
.code-pill {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 6px;
  background: #f2f4f7;
  color: #475467;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}
</style>
