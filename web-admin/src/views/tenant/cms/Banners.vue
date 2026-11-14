<template>
  <div class="page">
    <PageHeader title="Banner 管理" subtitle="维护官网、小程序与 H5 轮播图，支持跳转链接与排序。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增 Banner</el-button>
      </template>
    </PageHeader>

    <div class="card card-pad filter-card">
      <div class="filter-row">
        <el-select v-model="position" placeholder="全部位置" clearable style="width: 170px" @change="load">
          <el-option v-for="item in positions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <span class="muted">共 {{ rows.length }} 张 Banner</span>
        <span class="spacer"></span>
        <span class="muted hint">按排序号升序展示，停用的 Banner 不在前台显示</span>
      </div>
    </div>

    <div class="banner-grid" v-loading="loading">
      <div v-for="row in rows" :key="row.id" class="banner-card card">
        <div class="banner-image">
          <img v-if="row.image" :src="row.image" :alt="row.title" />
          <div v-else class="image-empty">
            <el-icon :size="26"><Picture /></el-icon>
            <span>未设置图片</span>
          </div>
          <span class="position-badge">{{ positionName(row.position) }}</span>
          <span class="status-badge" :class="row.status === 'ACTIVE' ? 'on' : 'off'">
            {{ row.status === 'ACTIVE' ? '展示中' : '已停用' }}
          </span>
        </div>
        <div class="banner-body">
          <strong>{{ row.title || '未命名 Banner' }}</strong>
          <span class="banner-link">{{ row.link || '无跳转链接' }}</span>
          <div class="banner-meta">
            <span>排序 {{ row.sortNo ?? 0 }}</span>
            <span>{{ formatTime(row.createdAt) }}</span>
          </div>
          <div class="banner-actions">
            <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain :icon="Delete" @click="remove(row)">删除</el-button>
          </div>
        </div>
      </div>
      <div v-if="!loading && !rows.length" class="empty-wrap card">
        <el-empty description="暂无 Banner，点击右上角新增" :image-size="100" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 Banner' : '新增 Banner'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="展示位置" prop="position">
              <el-select v-model="form.position" style="width: 100%">
                <el-option v-for="item in positions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如 冬季鲜食上新" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.link" placeholder="如 /products 或 /pages/product/index" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <ImageUpload
            v-model="form.image"
            title="Banner 图片"
            biz-type="CMS"
            hint="官网建议 1920×640，小程序建议 750×360，≤5MB"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">展示</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
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
import { Delete, Edit, Picture, Plus, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { api } from '@/api/http'

const positions = [
  { value: 'PORTAL', label: '官网首页' },
  { value: 'MINI', label: '小程序首页' },
  { value: 'H5', label: 'H5 首页' }
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const position = ref('')
const rows = ref<any[]>([])
const formRef = ref()
const form = ref<any>(emptyForm())

const rules: any = {
  position: [{ required: true, message: '请选择展示位置', trigger: 'change' }],
  title: [{ required: true, message: '请输入 Banner 标题', trigger: 'blur' }],
  image: [{ required: true, message: '请上传 Banner 图片', trigger: 'change' }]
}

function emptyForm() {
  return { position: 'PORTAL', title: '', image: '', link: '', sortNo: 10, status: 'ACTIVE' }
}

onMounted(load)

function positionName(value: string) {
  return positions.find((item) => item.value === value)?.label || value || '-'
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}

async function load() {
  loading.value = true
  try {
    rows.value = (await api.get('/tenant/cms/banners', { position: position.value || undefined })) || []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { ...emptyForm(), position: position.value || 'PORTAL', sortNo: (rows.value.length + 1) * 10 }
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = {
    position: row.position || 'PORTAL',
    title: row.title || '',
    image: row.image || '',
    link: row.link || '',
    sortNo: Number(row.sortNo) || 0,
    status: row.status || 'ACTIVE'
  }
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingId.value) {
      await api.put(`/tenant/cms/banners/${editingId.value}`, form.value)
    } else {
      await api.post('/tenant/cms/banners', form.value)
    }
    ElMessage.success('Banner 已保存')
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title || '未命名 Banner'}」吗？删除后前台将立即移除。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/cms/banners/${row.id}`)
  ElMessage.success('Banner 已删除')
  await load()
}
</script>

<style scoped lang="scss">
.filter-card {
  padding: 14px 18px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .spacer {
    flex: 1;
  }
  .hint {
    font-size: 11.5px;
  }
}

.banner-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.banner-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.banner-image {
  position: relative;
  height: 150px;
  background: #eef2f8;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.image-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #98a2b3;
  font-size: 11.5px;
}

.position-badge,
.status-badge {
  position: absolute;
  top: 10px;
  font-size: 10.5px;
  padding: 2px 8px;
  border-radius: 999px;
  backdrop-filter: blur(4px);
}

.position-badge {
  left: 10px;
  background: rgba(14, 23, 41, 0.6);
  color: #fff;
}

.status-badge {
  right: 10px;
  &.on {
    background: #e8f7ef;
    color: #0e7a45;
  }
  &.off {
    background: rgba(16, 24, 40, 0.6);
    color: #fff;
  }
}

.banner-body {
  padding: 13px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 5px;

  strong {
    font-size: 13px;
    font-weight: 650;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.banner-link {
  font-size: 11.5px;
  color: var(--brand);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.banner-meta {
  display: flex;
  justify-content: space-between;
  font-size: 10.5px;
  color: var(--ink-3);
  padding: 4px 0 6px;
}

.banner-actions {
  display: flex;
  gap: 8px;
  padding-top: 9px;
  border-top: 1px dashed var(--line);
}

.empty-wrap {
  grid-column: 1 / -1;
  padding: 30px;
}
</style>
