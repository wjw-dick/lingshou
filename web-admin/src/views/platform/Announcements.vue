<template>
  <div class="page">
    <PageHeader title="平台公告" subtitle="向全部租户、指定租户或指定套餐发布平台通知，并跟踪阅读情况。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">发布公告</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <span class="muted">共 {{ total }} 条公告</span>
        <div class="spacer"></div>
        <el-tag effect="plain" round>发布后立即生效</el-tag>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" style="width: 100%">
        <el-table-column label="公告标题" min-width="220">
          <template #default="{ row }">
            <div class="notice-title">
              <strong>{{ row.title }}</strong>
              <span>{{ row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发布范围" min-width="180">
          <template #default="{ row }">
            <div class="target-cell">
              <el-tag size="small" effect="plain" :type="row.targetType === 'ALL' ? 'success' : 'primary'">
                {{ targetTypeLabel(row.targetType) }}
              </el-tag>
              <span v-if="row.targetType !== 'ALL'" class="muted">{{ targetsText(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="发布时间" width="150">
          <template #default="{ row }">{{ datetime(row.publishedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReadStats(row)">阅读统计</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无公告，点击右上角发布" :image-size="80" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="660px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告正文" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="发布范围" prop="targetType">
          <el-radio-group v-model="form.targetType" @change="onTargetTypeChange">
            <el-radio-button value="ALL">全部租户</el-radio-button>
            <el-radio-button value="TENANT">指定租户</el-radio-button>
            <el-radio-button value="PLAN">指定套餐</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'TENANT'" label="选择租户" prop="targetIds">
          <el-select v-model="form.targetIds" multiple filterable placeholder="请选择租户" style="width: 100%">
            <el-option v-for="tenant in tenants" :key="tenant.tenantId" :label="`${tenant.name}（${tenant.tenantCode}）`" :value="tenant.tenantId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'PLAN'" label="选择套餐" prop="targetIds">
          <el-select v-model="form.targetIds" multiple filterable placeholder="请选择套餐" style="width: 100%">
            <el-option v-for="plan in plans" :key="plan.planId" :label="plan.name" :value="plan.planId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ form.id ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statsVisible" title="阅读统计" width="420px">
      <div v-loading="statsLoading" class="stats-panel">
        <div class="stats-ring">
          <strong>{{ stats.readCount ?? 0 }}</strong>
          <span>累计阅读次数</span>
        </div>
        <div class="stats-meta">
          <div><span>公告标题</span><strong>{{ currentNotice?.title }}</strong></div>
          <div><span>发布范围</span><strong>{{ currentNotice ? targetTypeLabel(currentNotice.targetType) : '-' }}</strong></div>
          <div><span>发布时间</span><strong>{{ currentNotice ? datetime(currentNotice.publishedAt) : '-' }}</strong></div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="statsVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const loading = ref(false)
const saving = ref(false)
const statsLoading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const tenants = ref<any[]>([])
const plans = ref<any[]>([])

const dialogVisible = ref(false)
const formRef = ref<any>()
const form = reactive<{ id: number | undefined; title: string; content: string; targetType: string; targetIds: number[] }>({
  id: undefined,
  title: '',
  content: '',
  targetType: 'ALL',
  targetIds: []
})
const rules = computed(() => ({
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择发布范围', trigger: 'change' }],
  targetIds: form.targetType === 'ALL' ? [] : [{ required: true, message: '请选择发布对象', trigger: 'change' }]
}))

const statsVisible = ref(false)
const stats = ref<any>({})
const currentNotice = ref<any>(null)

const tenantMap = computed(() => {
  const map: Record<number, string> = {}
  tenants.value.forEach((t) => { map[t.tenantId] = t.name })
  return map
})
const planMap = computed(() => {
  const map: Record<number, string> = {}
  plans.value.forEach((p) => { map[p.planId] = p.name })
  return map
})

onMounted(() => {
  load()
  loadTargets()
})

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/platform/announcements', { page: page.value, size: size.value })
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadTargets() {
  try {
    const [tenantData, planData] = await Promise.all([
      api.get('/platform/tenants', { page: 1, size: 200 }),
      api.get('/platform/plans')
    ])
    tenants.value = tenantData?.records || []
    plans.value = Array.isArray(planData) ? planData : []
  } catch {
    tenants.value = []
    plans.value = []
  }
}

function openCreate() {
  form.id = undefined
  form.title = ''
  form.content = ''
  form.targetType = 'ALL'
  form.targetIds = []
  dialogVisible.value = true
}

function openEdit(row: any) {
  form.id = row.id
  form.title = row.title
  form.content = row.content
  form.targetType = row.targetType || 'ALL'
  form.targetIds = parseIds(row.targetIds)
  dialogVisible.value = true
}

function onTargetTypeChange() {
  form.targetIds = []
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      content: form.content.trim(),
      targetType: form.targetType,
      targetIds: form.targetType === 'ALL' ? '[]' : JSON.stringify(form.targetIds)
    }
    if (form.id) {
      await api.put(`/platform/announcements/${form.id}`, payload)
      ElMessage.success('公告已更新')
    } else {
      await api.post('/platform/announcements', payload)
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openReadStats(row: any) {
  currentNotice.value = row
  stats.value = {}
  statsVisible.value = true
  statsLoading.value = true
  try {
    stats.value = await api.get(`/platform/announcements/${row.id}/read-stats`)
  } catch {
    stats.value = {}
  } finally {
    statsLoading.value = false
  }
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`删除公告「${row.title}」后不可恢复，是否继续？`, '删除公告', { type: 'error' })
  } catch {
    return
  }
  await api.del(`/platform/announcements/${row.id}`)
  ElMessage.success('公告已删除')
  if (rows.value.length === 1 && page.value > 1) page.value--
  load()
}

function parseIds(value: any): number[] {
  if (!value) return []
  if (Array.isArray(value)) return value.map((v) => Number(v))
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map((v) => Number(v)) : []
  } catch {
    return []
  }
}

function targetsText(row: any) {
  const ids = parseIds(row.targetIds)
  if (!ids.length) return '未指定对象'
  const names = ids.map((id) => (row.targetType === 'TENANT' ? tenantMap.value[id] : planMap.value[id])).filter(Boolean)
  if (names.length > 2) return `${names.slice(0, 2).join('、')} 等 ${names.length} 个`
  return names.join('、') || `${ids.length} 个对象`
}

function targetTypeLabel(type: string) {
  if (type === 'TENANT') return '指定租户'
  if (type === 'PLAN') return '指定套餐'
  return '全部租户'
}

function datetime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.toolbar .muted { font-size: 12px; }

.notice-title { display: flex; flex-direction: column; gap: 4px;
  strong { font-size: 13px; font-weight: 600; }
  span { font-size: 11.5px; color: var(--ink-3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
}
.target-cell { display: flex; flex-direction: column; align-items: flex-start; gap: 4px;
  span { font-size: 11px; }
}

.stats-panel { display: flex; flex-direction: column; gap: 16px; min-height: 180px; }
.stats-ring {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; padding: 18px; border-radius: 12px; background: #f3f8ff;
  strong { font-size: 34px; font-weight: 700; color: #1f6feb; letter-spacing: -0.03em; }
  span { font-size: 11.5px; color: var(--ink-3); }
}
.stats-meta { display: flex; flex-direction: column; gap: 10px;
  div { display: flex; flex-direction: column; gap: 3px; }
  span { font-size: 11px; color: var(--ink-3); }
  strong { font-size: 12.5px; font-weight: 550; }
}
</style>
