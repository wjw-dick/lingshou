<template>
  <div class="image-upload-card">
    <div class="upload-head">
      <div>
        <strong>{{ title }}</strong>
        <span v-if="hint">{{ hint }}</span>
      </div>
      <el-button v-if="modelValue" text type="danger" size="small" @click="remove">移除</el-button>
    </div>
    <el-upload
      class="uploader"
      drag
      :show-file-list="false"
      :http-request="doUpload"
      :accept="accept"
      :disabled="uploading"
    >
      <div v-if="modelValue" class="preview">
        <img :src="modelValue" alt="uploaded" />
        <div class="preview-mask">点击替换</div>
      </div>
      <div v-else class="empty">
        <el-icon :size="26"><UploadFilled /></el-icon>
        <p>{{ uploading ? '上传中…' : '拖拽文件到此处，或点击上传' }}</p>
        <span>{{ hint || '建议尺寸 900×383，支持 JPG/PNG，≤5MB' }}</span>
      </div>
      <el-progress v-if="uploading" :percentage="100" :indeterminate="true" :show-text="false" />
    </el-upload>
    <div v-if="allowFile && modelValue && !isImage" class="file-row">
      <el-icon><Document /></el-icon>
      <a :href="modelValue" target="_blank">查看已上传文件</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/http'

const props = withDefaults(defineProps<{
  modelValue?: string
  title?: string
  hint?: string
  bizType?: string
  accept?: string
  allowFile?: boolean
}>(), {
  title: '图片',
  bizType: 'GENERAL',
  accept: 'image/*',
  allowFile: false
})

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const uploading = ref(false)
const isImage = computed(() => !props.modelValue || /\.(png|jpe?g|gif|webp|bmp)(\?|$)/i.test(props.modelValue))

async function doUpload(options: any) {
  const file: File = options.file
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const result = await api.upload(file, props.bizType)
    emit('update:modelValue', result.url)
    ElMessage.success('上传成功')
  } finally {
    uploading.value = false
  }
}

function remove() {
  emit('update:modelValue', '')
}
</script>

<style scoped lang="scss">
.upload-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
  gap: 10px;

  strong { font-size: 13px; font-weight: 600; }
  span { display: block; font-size: 11.5px; color: var(--ink-3); margin-top: 4px; }
}

.uploader {
  :deep(.el-upload) { width: 100%; }
  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 0;
    border: none;
    background: transparent;
  }
}

.preview {
  position: relative;
  height: 180px;
  border-radius: 8px;
  overflow: hidden;
  background: #f2f5fa;

  img { width: 100%; height: 100%; object-fit: cover; display: block; }

  .preview-mask {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    background: rgba(15, 23, 42, 0.5);
    color: #fff;
    font-size: 13px;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .preview-mask { opacity: 1; }
}

.empty {
  border: 1px dashed #cdd5e1;
  border-radius: 8px;
  padding: 28px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--ink-3);
  background: #fbfcfe;

  p { margin: 0; font-size: 12.5px; color: var(--ink-2); }
  span { font-size: 11px; }
}

.file-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--brand);
}
</style>
