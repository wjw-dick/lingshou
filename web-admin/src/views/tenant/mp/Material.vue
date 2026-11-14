<template>
  <div class="page">
    <PageHeader title="公众号素材" subtitle="管理图片、语音与视频素材；上传后保存本地地址，并尝试同步到微信永久素材库。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="UploadFilled" @click="openUpload">上传素材</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="currentAccount && !currentAccount.appSecret"
      title="当前公众号未配置 AppSecret"
      description="素材会正常保存到本地对象存储，但同步微信素材库会失败并返回提示；请先在账号管理中补齐凭证。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <el-alert
      title="素材规格：图片不超过 2MB，语音不超过 2MB，视频不超过 10MB 且必须同时上传缩略图"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div class="card card-pad material-card">
      <el-tabs v-model="mediaType" @tab-change="handleTabChange">
        <el-tab-pane label="图片" name="IMAGE" />
        <el-tab-pane label="语音" name="VOICE" />
        <el-tab-pane label="视频" name="VIDEO" />
      </el-tabs>

      <div v-loading="loading" class="material-grid">
        <div v-for="item in materials" :key="item.id" class="material-item">
          <div class="material-cover">
            <el-image
              v-if="item.mediaType === 'IMAGE' && item.url"
              :src="item.url"
              :preview-src-list="[item.url]"
              fit="cover"
              class="cover-img"
            >
              <template #error>
                <div class="cover-fallback"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <el-image
              v-else-if="item.mediaType === 'VIDEO' && item.thumbUrl"
              :src="item.thumbUrl"
              fit="cover"
              class="cover-img"
            >
              <template #error>
                <div class="cover-fallback"><el-icon><VideoPlay /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-fallback">
              <el-icon :size="30"><VideoPlay v-if="item.mediaType === 'VIDEO'" /><Microphone v-else /></el-icon>
            </div>
            <span v-if="item.mediaType === 'VIDEO'" class="play-badge"><el-icon><VideoPlay /></el-icon> 视频</span>
          </div>

          <div class="material-main">
            <div class="material-title">
              <strong :title="item.name">{{ item.name || '未命名素材' }}</strong>
              <el-tag :type="Number(item.permanent) === 1 ? 'success' : 'warning'" size="small" effect="light">
                {{ Number(item.permanent) === 1 ? '永久' : '临时' }}
              </el-tag>
            </div>
            <div class="material-meta">
              <span>{{ formatSize(item.size) }}</span>
              <span>{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="material-id">
              <span class="mono">{{ item.mediaId || '未同步微信（无 media_id）' }}</span>
              <el-button v-if="item.mediaId" link type="primary" :icon="CopyDocument" @click="copy(item.mediaId)">复制</el-button>
            </div>
            <div class="material-foot">
              <a v-if="item.url" :href="item.url" target="_blank" class="url-link">本地 URL</a>
              <span v-else class="muted">无本地 URL</span>
              <el-button link type="danger" @click="removeMaterial(item)">删除</el-button>
            </div>
          </div>
        </div>

        <div v-if="!loading && !materials.length" class="material-empty">
          <el-empty :description="`暂无${mediaTypeLabel}素材，点击右上角上传`" :image-size="64" />
        </div>
      </div>
    </div>

    <el-dialog v-model="uploadVisible" :title="`上传${mediaTypeLabel}素材`" width="560px">
      <el-form label-position="top">
        <el-form-item label="素材类型">
          <el-input :model-value="mediaTypeLabel" disabled />
        </el-form-item>
        <el-form-item label="素材名称">
          <el-input v-model="uploadForm.name" maxlength="64" :placeholder="`不填则使用文件名`" />
        </el-form-item>
        <el-form-item label="保存类型">
          <el-radio-group v-model="uploadForm.permanent">
            <el-radio :value="1">永久素材</el-radio>
            <el-radio :value="0">临时素材</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="`选择${mediaTypeLabel}文件`" required>
          <el-upload
            class="file-uploader"
            drag
            :auto-upload="false"
            :show-file-list="false"
            :accept="acceptType"
            :on-change="onFileChange"
          >
            <div class="upload-empty">
              <el-icon :size="24"><UploadFilled /></el-icon>
              <p v-if="uploadForm.file">{{ uploadForm.file.name }}（{{ formatSize(uploadForm.file.size) }}）</p>
              <p v-else>拖拽或点击选择{{ mediaTypeLabel }}文件</p>
              <span>{{ uploadHint }}</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="mediaType === 'VIDEO'" label="视频缩略图" required>
          <el-upload
            class="file-uploader"
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="onThumbChange"
          >
            <div class="upload-empty">
              <el-icon :size="24"><Picture /></el-icon>
              <p v-if="uploadForm.thumb">{{ uploadForm.thumb.name }}（{{ formatSize(uploadForm.thumb.size) }}）</p>
              <p v-else>点击选择缩略图，微信要求视频必须带封面</p>
              <span>建议 JPG/PNG，不超过 2MB</span>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div class="form-tip">
        上传后素材先写入本地对象存储，再尝试同步微信素材库；同步失败不会丢失本地文件，页面会给出提示。
      </div>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Refresh, UploadFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const mediaType = ref('IMAGE')
const materials = ref<any[]>([])
const loading = ref(false)

const uploadVisible = ref(false)
const uploading = ref(false)
const uploadForm = ref<any>({ name: '', permanent: 1, file: null, thumb: null })

const mediaLabels: Record<string, string> = { IMAGE: '图片', VOICE: '语音', VIDEO: '视频' }
const mediaTypeLabel = computed(() => mediaLabels[mediaType.value] || mediaType.value)
const acceptType = computed(() => {
  if (mediaType.value === 'IMAGE') return 'image/*'
  if (mediaType.value === 'VOICE') return 'audio/*'
  return 'video/*'
})
const uploadHint = computed(() => {
  if (mediaType.value === 'IMAGE') return '支持 JPG/PNG，≤2MB'
  if (mediaType.value === 'VOICE') return '支持 MP3/AMR，≤2MB'
  return '支持 MP4，≤10MB，需上传缩略图'
})

const currentAccount = computed(() => accounts.value.find((item) => item.id === accountId.value))

onMounted(async () => {
  await loadAccounts()
  await load()
})

async function loadAccounts() {
  try {
    accounts.value = await api.get('/tenant/mp/accounts')
    const queryId = Number(route.query.accountId)
    accountId.value = accounts.value.some((item) => item.id === queryId) ? queryId : accounts.value[0]?.id
  } catch {
    accounts.value = []
  }
}

function handleAccountChange(id: number) {
  router.replace({ query: { ...route.query, accountId: String(id) } })
  load()
}

function handleTabChange() {
  load()
}

async function load() {
  if (!accountId.value) {
    materials.value = []
    return
  }
  loading.value = true
  try {
    materials.value = await api.get('/tenant/mp/materials', {
      accountId: accountId.value,
      mediaType: mediaType.value
    })
  } catch {
    materials.value = []
  } finally {
    loading.value = false
  }
}

function openUpload() {
  if (!accountId.value) {
    ElMessage.warning('请先在账号管理中新增公众号账号')
    return
  }
  uploadForm.value = { name: '', permanent: 1, file: null, thumb: null }
  uploadVisible.value = true
}

function onFileChange(file: any) {
  uploadForm.value.file = file.raw || null
}

function onThumbChange(file: any) {
  uploadForm.value.thumb = file.raw || null
}

async function submitUpload() {
  const file: File | null = uploadForm.value.file
  if (!file) {
    ElMessage.warning(`请选择${mediaTypeLabel.value}文件`)
    return
  }
  if (mediaType.value !== 'VIDEO' && file.size > 2 * 1024 * 1024) {
    ElMessage.warning(`${mediaTypeLabel.value}素材不能超过 2MB`)
    return
  }
  if (mediaType.value === 'VIDEO' && file.size > 10 * 1024 * 1024) {
    ElMessage.warning('视频素材不能超过 10MB')
    return
  }
  if (mediaType.value === 'VIDEO' && !uploadForm.value.thumb) {
    ElMessage.warning('上传视频必须同时上传缩略图')
    return
  }
  uploading.value = true
  try {
    const result = await uploadMaterial(file)
    if (result?.warning) {
      ElMessage.warning(result.warning)
    } else {
      ElMessage.success('素材已上传并同步到微信')
    }
    uploadVisible.value = false
    await load()
  } finally {
    uploading.value = false
  }
}

async function uploadMaterial(file: File) {
  const name = uploadForm.value.name?.trim() || undefined
  const thumb: File | undefined = uploadForm.value.thumb || undefined
  if (Number(uploadForm.value.permanent) === 1) {
    return api.uploadMpMaterial(file, mediaType.value, name, thumb)
  }
  const form = new FormData()
  form.append('file', file)
  form.append('mediaType', mediaType.value)
  if (name) form.append('name', name)
  form.append('permanent', '0')
  if (thumb) form.append('thumb', thumb)
  return api.post('/tenant/mp/materials', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

async function removeMaterial(row: any) {
  try {
    await ElMessageBox.confirm(`删除素材「${row.name || '未命名素材'}」？微信端已同步的素材需在微信后台单独删除。`, '删除素材', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await api.del(`/tenant/mp/materials/${row.id}`)
  ElMessage.success('素材已删除')
  await load()
}

async function copy(text?: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('media_id 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function formatSize(size: any) {
  const value = Number(size || 0)
  if (!value) return '-'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(2)} MB`
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.material-card {
  padding-top: 8px;

  :deep(.el-tabs__header) { margin-bottom: 14px; }
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 14px;
  min-height: 120px;
}

.material-item {
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: box-shadow 0.2s;

  &:hover { box-shadow: 0 8px 22px rgba(16, 24, 40, 0.08); }
}

.material-cover {
  position: relative;
  height: 132px;
  background: #f5f7fb;
}

.cover-img { width: 100%; height: 100%; display: block; }

.cover-fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #98a2b3;
  background: linear-gradient(135deg, #f2f6fd, #eef7f6);
}

.play-badge {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.6);
  color: #fff;
  font-size: 10.5px;
}

.material-main { padding: 12px 14px 10px; }

.material-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  strong {
    font-size: 13px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.material-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 5px;
  font-size: 11px;
  color: var(--ink-3);
}

.material-id {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 7px;
  min-width: 0;

  .mono {
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 10.5px;
    color: var(--ink-2);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.material-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
  padding-top: 7px;
  border-top: 1px dashed #eef1f5;
  font-size: 11.5px;
}

.url-link { color: var(--brand); }

.material-empty {
  grid-column: 1 / -1;
  padding: 12px;
}

.file-uploader {
  width: 100%;

  :deep(.el-upload) { width: 100%; }
  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 16px;
    border-color: #cdd5e1;
    background: #fbfcfe;
  }
}

.upload-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--ink-3);

  p { margin: 0; font-size: 12.5px; color: var(--ink-2); word-break: break-all; }
  span { font-size: 11px; }
}
</style>
