<template>
  <div class="rich-editor">
    <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" class="editor-toolbar" />
    <Editor
      v-model="valueHtml"
      :default-config="editorConfig"
      mode="default"
      class="editor-body"
      :style="{ height }"
      @on-created="handleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { api } from '@/api/http'

const props = withDefaults(defineProps<{ modelValue?: string; height?: string; placeholder?: string }>(), {
  modelValue: '',
  height: '320px',
  placeholder: '请输入正文内容…'
})
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const editorRef = shallowRef()
const valueHtml = ref(props.modelValue || '')

watch(() => props.modelValue, (value) => {
  if (value !== valueHtml.value) valueHtml.value = value || ''
})

watch(valueHtml, (value) => emit('update:modelValue', value))

const toolbarConfig = {
  excludeKeys: ['group-video', 'fullScreen']
}

const editorConfig: any = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        const result = await api.upload(file, 'RICH_TEXT')
        insertFn(result.url, result.name || '', result.url)
      }
    }
  }
}

function handleCreated(editor: any) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<style scoped lang="scss">
.rich-editor {
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;

  .editor-toolbar { border-bottom: 1px solid var(--line); background: #fbfcfe; }
  .editor-body { overflow-y: auto; }
  :deep(.w-e-text-container) { background: #fff; }
}
</style>
