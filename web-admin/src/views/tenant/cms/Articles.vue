<template>
  <div class="page">
    <PageHeader title="文章管理" subtitle="维护官网与小程序品牌资讯，支持中英文内容、草稿与发布。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建文章</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索文章标题"
          :prefix-icon="Search"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 篇</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="封面" width="86">
          <template #default="{ row }">
            <div class="cover">
              <img v-if="row.cover" :src="row.cover" alt="cover" />
              <el-icon v-else><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="260">
          <template #default="{ row }">
            <div class="title-cell">
              <strong>{{ row.title }}</strong>
              <span v-if="row.titleEn">{{ row.titleEn }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ categoryName(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120">
          <template #default="{ row }">{{ row.author || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status === 'PUBLISHED' ? 'PUBLISHED' : 'DRAFT'" />
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatTime(row.publishedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="publish(row)">发布</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无文章，点击右上角新建" :image-size="90" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑文章' : '新建文章'"
      width="920px"
      top="4vh"
      destroy-on-close
      class="article-dialog"
    >
      <div v-loading="detailLoading">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="分类" prop="category">
                <el-select v-model="form.category" filterable allow-create default-first-option placeholder="选择分类" style="width: 100%">
                  <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="作者">
                <el-input v-model="form.author" placeholder="如 品牌中心" maxlength="30" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="发布状态">
                <el-tag :type="form.status === 'PUBLISHED' ? 'success' : 'info'" effect="plain">
                  {{ form.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                </el-tag>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入中文标题" maxlength="80" show-word-limit />
          </el-form-item>
          <el-form-item label="英文标题">
            <el-input v-model="form.titleEn" placeholder="English title" maxlength="120" />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="摘要">
                <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="文章摘要" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="英文摘要">
                <el-input v-model="form.summaryEn" type="textarea" :rows="2" maxlength="300" placeholder="English summary" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="封面图">
            <ImageUpload v-model="form.cover" title="文章封面" biz-type="CMS" hint="建议 900×500，支持 JPG/PNG，≤5MB" />
          </el-form-item>

          <el-form-item label="正文" prop="content">
            <RichEditor v-model="form.content" height="260px" placeholder="请输入中文正文…" />
          </el-form-item>
          <el-form-item label="英文正文">
            <RichEditor v-model="form.contentEn" height="200px" placeholder="English content…" />
          </el-form-item>

          <el-form-item label="SEO 关键词">
            <el-input v-model="form.seoKeywords" placeholder="多个关键词用英文逗号分隔" maxlength="120" />
          </el-form-item>
          <el-form-item label="SEO 描述">
            <el-input v-model="form.seoDescription" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="搜索引擎展示的描述" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" @click="submit(false)">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="submit(true)">保存并发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import RichEditor from '@/components/RichEditor.vue'
import { api } from '@/api/http'

const categories = [
  { value: 'BRAND', label: '品牌资讯' },
  { value: 'FRESH', label: '鲜食商品' },
  { value: 'FRANCHISE', label: '加盟合作' },
  { value: 'STORE', label: '门店动态' },
  { value: 'ACTIVITY', label: '营销活动' }
]

const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = ref<any>({ page: 1, size: 10, keyword: '', status: '' })
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const form = ref<any>(emptyForm())

const rules: any = {
  category: [{ required: true, message: '请选择文章分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文章正文', trigger: 'blur' }]
}

function emptyForm() {
  return {
    category: 'BRAND',
    title: '',
    titleEn: '',
    summary: '',
    summaryEn: '',
    cover: '',
    content: '',
    contentEn: '',
    author: '',
    status: 'DRAFT',
    seoKeywords: '',
    seoDescription: ''
  }
}

onMounted(load)

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function categoryName(value: string) {
  return categories.find((item) => item.value === value)?.label || value || '-'
}

async function load() {
  loading.value = true
  try {
    const result = await api.get('/tenant/cms/articles', {
      page: query.value.page,
      size: query.value.size,
      keyword: query.value.keyword || undefined,
      status: query.value.status || undefined
    })
    rows.value = result?.records || []
    total.value = Number(result?.total) || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.value.page = 1
  load()
}

function reset() {
  query.value = { page: 1, size: query.value.size, keyword: '', status: '' }
  load()
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  dialogVisible.value = true
}

async function openEdit(row: any) {
  editingId.value = row.id
  form.value = emptyForm()
  dialogVisible.value = true
  detailLoading.value = true
  try {
    const detail = await api.get(`/tenant/cms/articles/${row.id}`)
    form.value = {
      ...emptyForm(),
      ...detail,
      summary: detail?.summary || '',
      titleEn: detail?.titleEn || '',
      summaryEn: detail?.summaryEn || '',
      contentEn: detail?.contentEn || '',
      seoKeywords: detail?.seoKeywords || '',
      seoDescription: detail?.seoDescription || ''
    }
  } catch {
    dialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function submit(publishAfter: boolean) {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (publishAfter) {
    try {
      await ElMessageBox.confirm('发布后文章将对官网与小程序可见，确定发布吗？', '发布确认', {
        type: 'info',
        confirmButtonText: '发布',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  saving.value = true
  try {
    const payload = { ...form.value, status: publishAfter ? 'PUBLISHED' : 'DRAFT' }
    let id = editingId.value
    if (id) {
      await api.put(`/tenant/cms/articles/${id}`, payload)
    } else {
      const created = await api.post('/tenant/cms/articles', payload)
      id = created?.id || null
    }
    if (publishAfter && id) {
      await api.post(`/tenant/cms/articles/${id}/publish`)
    }
    ElMessage.success(publishAfter ? '文章已发布' : '草稿已保存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function publish(row: any) {
  try {
    await ElMessageBox.confirm(`确定发布文章「${row.title}」吗？`, '发布确认', {
      type: 'info',
      confirmButtonText: '发布',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.post(`/tenant/cms/articles/${row.id}/publish`)
  ElMessage.success('文章已发布')
  await load()
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除文章「${row.title}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/cms/articles/${row.id}`)
  ElMessage.success('文章已删除')
  await load()
}
</script>

<style scoped lang="scss">
.total-tip {
  font-size: 12px;
}

.cover {
  width: 60px;
  height: 42px;
  border-radius: 6px;
  overflow: hidden;
  background: #f2f4f7;
  display: grid;
  place-items: center;
  color: #98a2b3;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong {
    font-size: 12.5px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  span {
    font-size: 11px;
    color: var(--ink-3);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.article-dialog {
  :deep(.el-dialog__body) {
    max-height: 76vh;
    overflow-y: auto;
    padding-top: 10px;
  }
  :deep(.image-upload-card) {
    width: 100%;
  }
}
</style>
