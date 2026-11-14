<template>
  <div class="page">
    <PageHeader title="商品分类" subtitle="按品类设置临期阈值，鲜食等短保品类可单独配置。">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate()">新增分类</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <el-table :data="tree" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column label="分类名称" min-width="220">
          <template #default="{ row }">
            <div class="cat-name">
              <span class="cat-dot"></span>
              <strong>{{ row.name }}</strong>
              <span v-if="row.nameEn" class="muted">{{ row.nameEn }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="90" />
        <el-table-column label="临期阈值" width="130">
          <template #default="{ row }"><el-tag effect="plain" type="warning">{{ row.nearExpiryDays }} 天</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子类</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!tree.length && !loading" description="暂无分类，先创建一级品类" :image-size="80">
        <el-button type="primary" @click="openCreate()">新增分类</el-button>
      </el-empty>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="顶级分类" :value="0" />
            <el-option v-for="cat in topCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="英文名称"><el-input v-model="form.nameEn" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="Element Plus 图标名，可选" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="临期阈值">
          <el-input-number v-model="form.nearExpiryDays" :min="1" :max="365" />
          <span class="unit">天</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const flat = ref<any[]>([])
const dialogVisible = ref(false)
const form = ref<any>({})

const tree = computed(() => {
  const map = new Map<number, any>()
  const roots: any[] = []
  flat.value.forEach((item) => map.set(item.id, { ...item, children: [] }))
  map.forEach((item) => {
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId).children.push(item)
    } else {
      roots.push(item)
    }
  })
  const sort = (list: any[]) => {
    list.sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
    list.forEach((item) => {
      if (item.children.length) sort(item.children)
      else delete item.children
    })
  }
  sort(roots)
  return roots
})

const topCategories = computed(() => flat.value.filter((item) => !item.parentId))

onMounted(load)

async function load() {
  loading.value = true
  try {
    flat.value = await api.get('/tenant/products/categories')
  } finally {
    loading.value = false
  }
}

function openCreate(parent?: any) {
  form.value = {
    parentId: parent?.id || 0,
    name: '',
    nameEn: '',
    icon: '',
    sortNo: 10,
    nearExpiryDays: parent?.nearExpiryDays || 3,
    status: 'ACTIVE'
  }
  dialogVisible.value = true
}

function openEdit(row: any) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name) {
    ElMessage.warning('请填写分类名称')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await api.put(`/tenant/products/categories/${form.value.id}`, form.value)
      ElMessage.success('分类已更新')
    } else {
      await api.post('/tenant/products/categories', form.value)
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」？存在子分类或商品时无法删除。`, '删除分类', { type: 'warning' })
  await api.del(`/tenant/products/categories/${row.id}`)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped lang="scss">
.cat-name { display: flex; align-items: center; gap: 8px; }
.cat-dot { width: 6px; height: 6px; border-radius: 2px; background: #1f6feb; }
.cat-name strong { font-weight: 550; font-size: 13px; }
.cat-name span.muted { font-size: 11px; }
.unit { margin-left: 8px; color: var(--ink-3); font-size: 12px; }
</style>
