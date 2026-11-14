<template>
  <div class="page">
    <PageHeader title="菜单可视化" subtitle="按端维护导航结构，支持拖拽排序与层级缩进，保存后整棵树立即生效。">
      <template #actions>
        <el-radio-group v-model="endType" size="default" @change="onEndChange">
          <el-radio-button v-for="item in endTypes" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存菜单</el-button>
      </template>
    </PageHeader>

    <div class="menu-workspace" v-loading="loading">
      <div class="card card-pad tree-panel">
        <div class="panel-head">
          <div>
            <h3>菜单结构</h3>
            <p>{{ endTypes.find((e) => e.value === endType)?.label }} · 共 {{ totalCount }} 项</p>
          </div>
          <el-button size="small" type="primary" plain :icon="Plus" @click="addTop">新增顶级</el-button>
        </div>

        <el-alert :title="ruleHint" type="info" :closable="false" show-icon class="rule-alert" />

        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          node-key="uid"
          highlight-current
          draggable
          default-expand-all
          :expand-on-click-node="false"
          :allow-drop="allowDrop"
          :current-node-key="selected?.uid || ''"
          class="menu-tree"
          @node-click="onNodeClick"
          @node-drop="onDrop"
        >
          <template #default="{ data }">
            <div class="tree-node" :class="{ muted: Number(data.visible) === 0 }">
              <el-icon class="node-icon">
                <component :is="iconComponent(data.icon)" v-if="iconComponent(data.icon)" />
                <IconGrid v-else />
              </el-icon>
              <span class="node-title">{{ data.title || '未命名菜单' }}</span>
              <span v-if="Number(data.visible) === 0" class="node-flag">隐藏</span>
              <span class="node-actions">
                <el-tooltip v-if="canNest(data)" content="新增子菜单" placement="top">
                  <el-button link type="primary" :icon="Plus" @click.stop="addChild(data)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" :icon="Delete" @click.stop="removeNode(data)" />
                </el-tooltip>
              </span>
            </div>
          </template>
        </el-tree>

        <el-empty v-if="!treeData.length" description="暂无菜单，点击「新增顶级」开始配置" :image-size="80" />
        <div class="tree-foot">拖拽节点可排序，横向拖动可调整层级</div>
      </div>

      <div class="card card-pad form-panel">
        <div v-if="selected" class="property-pane">
          <div class="panel-head">
            <div>
              <h3>属性配置</h3>
              <p>{{ levelOf(selected) }} · {{ selected.title || '未命名菜单' }}</p>
            </div>
            <StatusTag :status="Number(selected.visible) === 1 ? 'ACTIVE' : 'DISABLED'"
                       :text="Number(selected.visible) === 1 ? '显示中' : '已隐藏'" />
          </div>

          <el-form ref="formRef" :model="selected" :rules="nodeRules" label-position="top">
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="菜单标题" prop="title">
                  <el-input v-model="selected.title" placeholder="如 商品中心" maxlength="20" show-word-limit />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="英文标题" prop="titleEn">
                  <el-input v-model="selected.titleEn" placeholder="如 Products" maxlength="40" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="图标（Element Plus 图标名）">
              <el-select v-model="selected.icon" filterable clearable placeholder="选择图标" style="width: 100%">
                <template #prefix>
                  <el-icon v-if="iconComponent(selected.icon)">
                    <component :is="iconComponent(selected.icon)" />
                  </el-icon>
                </template>
                <el-option v-for="icon in iconNames" :key="icon" :label="icon" :value="icon">
                  <span class="icon-option">
                    <el-icon><component :is="icons[icon]" /></el-icon>
                    {{ icon }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="路径" prop="path">
              <el-input v-model="selected.path" placeholder="如 /products 或 /pages/home/index" />
            </el-form-item>

            <el-form-item label="组件路径">
              <el-input v-model="selected.component" placeholder="如 product/index，目录可为空">
                <template #prepend>@/views/</template>
              </el-input>
            </el-form-item>

            <el-form-item label="权限标识">
              <el-input v-model="selected.permission" placeholder="如 system:view，留空则不受权限控制" />
            </el-form-item>

            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="排序号">
                  <el-input-number v-model="selected.sortNo" :min="0" :max="9999" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否可见">
                  <div class="switch-line">
                    <el-switch v-model="selected.visible" :active-value="1" :inactive-value="0" />
                    <span>{{ Number(selected.visible) === 1 ? '显示在导航中' : '隐藏但仍可访问' }}</span>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <div class="form-actions">
            <el-button v-if="canNest(selected)" :icon="Plus" @click="addChild(selected)">新增子级</el-button>
            <el-button type="danger" plain :icon="Delete" @click="removeNode(selected)">删除该菜单</el-button>
          </div>
        </div>
        <el-empty v-else description="从左侧选择一个菜单进行配置" :image-size="90" />
      </div>

      <div class="card preview-panel">
        <div class="preview-head">
          <div>
            <h3>实时预览</h3>
            <p>未保存的调整会即时反映</p>
          </div>
          <el-radio-group v-model="previewMode" size="small">
            <el-radio-button value="PC">PC 外壳</el-radio-button>
            <el-radio-button value="PHONE">手机外壳</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="previewMode === 'PC'" class="pc-shell">
          <div class="pc-bar">
            <span class="dot red"></span><span class="dot yellow"></span><span class="dot green"></span>
            <span class="pc-url">admin.yuexiang.local{{ currentPath }}</span>
          </div>
          <div class="pc-body">
            <aside class="pc-side">
              <div class="pc-brand">
                <span class="brand-mark">悦</span>
                <div><strong>悦享便利</strong><em>管理后台</em></div>
              </div>
              <nav class="pc-nav">
                <template v-for="item in visibleTree" :key="item.uid">
                  <div class="pc-item" :class="{ active: previewActive === item.uid && !item.children.length }" @click="previewActive = item.uid">
                    <el-icon><component :is="iconComponent(item.icon)" v-if="iconComponent(item.icon)" /><IconGrid v-else /></el-icon>
                    <span>{{ item.title || '未命名' }}</span>
                  </div>
                  <div
                    v-for="child in item.children.filter((c) => Number(c.visible) === 1)"
                    :key="child.uid"
                    class="pc-item sub"
                    :class="{ active: previewActive === child.uid }"
                    @click="previewActive = child.uid"
                  >
                    <span>{{ child.title || '未命名' }}</span>
                  </div>
                </template>
              </nav>
            </aside>
            <div class="pc-main">
              <div class="pc-crumb">首页 / {{ activeTitle }}</div>
              <div class="pc-placeholder">
                <el-icon :size="26"><Document /></el-icon>
                <p>{{ activeTitle }}</p>
                <span>这是「{{ activeTitle }}」页面的内容区域</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="phone-shell">
          <div class="phone-screen">
            <div class="phone-status"><span>9:41</span><span>5G · 100%</span></div>
            <div class="phone-title">{{ phoneTitle }}</div>
            <div class="phone-body">
              <div v-if="phoneChildren.length" class="phone-grid">
                <div v-for="child in phoneChildren" :key="child.uid" class="phone-grid-item">
                  <el-icon><component :is="iconComponent(child.icon)" v-if="iconComponent(child.icon)" /><IconGrid v-else /></el-icon>
                  <span>{{ child.title || '未命名' }}</span>
                </div>
              </div>
              <div v-else class="phone-placeholder">
                <el-icon :size="24"><Document /></el-icon>
                <p>{{ phoneTitle }}</p>
                <span>{{ endType === 'MP' ? '公众号菜单最多两级' : '移动端页面内容' }}</span>
              </div>
            </div>
            <div class="phone-tabbar">
              <div
                v-for="item in phoneRoots"
                :key="item.uid"
                class="phone-tab"
                :class="{ active: phoneActive === item.uid }"
                @click="phoneActive = item.uid"
              >
                <el-icon><component :is="iconComponent(item.icon)" v-if="iconComponent(item.icon)" /><IconGrid v-else /></el-icon>
                <span>{{ item.title || '未命名' }}</span>
              </div>
              <div v-if="!phoneRoots.length" class="phone-tab-empty">暂无可见菜单</div>
            </div>
          </div>
        </div>

        <div class="preview-foot">
          <el-tag effect="plain" round>{{ endType }}</el-tag>
          <span>{{ ruleHint }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Document, Grid as IconGrid, Plus, Refresh } from '@element-plus/icons-vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'

interface MenuNode {
  uid: string
  id?: number
  title: string
  titleEn: string
  icon: string
  path: string
  component: string
  permission: string
  sortNo: number
  visible: number
  children: MenuNode[]
}

const endTypes = [
  { value: 'ADMIN', label: '管理后台', maxDepth: 3, hint: '后台左侧导航，建议不超过三级' },
  { value: 'PORTAL', label: '官网', maxDepth: 2, hint: '官网顶部导航，支持两级' },
  { value: 'TABBAR', label: '小程序 TabBar', maxDepth: 1, hint: '小程序底部导航需 2~5 个，不设子级' },
  { value: 'H5', label: 'H5 导航', maxDepth: 1, hint: 'H5 底部导航最多 5 个，不设子级' },
  { value: 'MP', label: '公众号菜单', maxDepth: 2, hint: '公众号一级最多 3 个，每个最多 5 个二级' }
]

const icons = ElementPlusIcons as unknown as Record<string, any>
const iconNames = Object.keys(icons)

const endType = ref('ADMIN')
const loadedEnd = ref('ADMIN')
const loading = ref(false)
const saving = ref(false)
const hydrating = ref(false)
const dirty = ref(false)
const treeData = ref<MenuNode[]>([])
const selected = ref<MenuNode | null>(null)
const treeRef = ref()
const formRef = ref()
const previewMode = ref<'PC' | 'PHONE'>('PC')
const previewActive = ref('')
const phoneActive = ref('')
let uidSeed = 0

const treeProps = { label: 'title', children: 'children' }

const nodeRules = {
  title: [{ required: true, message: '请输入菜单标题', trigger: 'blur' }],
  path: [{ required: true, message: '请输入菜单路径', trigger: 'blur' }]
}

const currentConfig = computed(() => endTypes.find((item) => item.value === endType.value) || endTypes[0])
const ruleHint = computed(() => currentConfig.value.hint)

const totalCount = computed(() => {
  let count = 0
  const walk = (nodes: MenuNode[]) => {
    for (const node of nodes) {
      count += 1
      if (node.children?.length) walk(node.children)
    }
  }
  walk(treeData.value)
  return count
})

const visibleTree = computed(() =>
  treeData.value
    .filter((node) => Number(node.visible) === 1)
    .map((node) => ({ ...node, children: (node.children || []).filter((child) => Number(child.visible) === 1) }))
)

const phoneRoots = computed(() => {
  if (endType.value === 'TABBAR' || endType.value === 'H5') return visibleTree.value.slice(0, 5)
  return visibleTree.value.slice(0, 5)
})

const phoneCurrent = computed(() => phoneRoots.value.find((item) => item.uid === phoneActive.value) || phoneRoots.value[0])
const phoneTitle = computed(() => phoneCurrent.value?.title || '未命名菜单')
const phoneChildren = computed(() => {
  if (endType.value === 'MP') return phoneCurrent.value?.children || []
  return []
})

const activeTitle = computed(() => {
  const walk = (nodes: MenuNode[]): string => {
    for (const node of nodes) {
      if (node.uid === previewActive.value) return node.title || '未命名菜单'
      if (node.children?.length) {
        const found = walk(node.children)
        if (found) return found
      }
    }
    return ''
  }
  return walk(treeData.value) || selected.value?.title || '工作台'
})

const currentPath = computed(() => selected.value?.path || '/dashboard')

watch(
  treeData,
  () => {
    if (!hydrating.value) dirty.value = true
  },
  { deep: true }
)

watch(phoneRoots, (roots) => {
  if (!roots.some((item) => item.uid === phoneActive.value)) phoneActive.value = roots[0]?.uid || ''
})

onMounted(load)
window.addEventListener('beforeunload', onBeforeUnload)
onBeforeUnmount(() => window.removeEventListener('beforeunload', onBeforeUnload))

onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('菜单有未保存的调整，确定离开吗？', '未保存提醒', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑'
    })
    return true
  } catch {
    return false
  }
})

function onBeforeUnload(event: BeforeUnloadEvent) {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

function newUid() {
  uidSeed += 1
  return `m_${Date.now().toString(36)}_${uidSeed}`
}

function normalize(node: any): MenuNode {
  return {
    uid: newUid(),
    id: node.id,
    title: node.title || '',
    titleEn: node.titleEn || '',
    icon: node.icon || '',
    path: node.path || '',
    component: node.component || '',
    permission: node.permission || '',
    sortNo: Number(node.sortNo) || 0,
    visible: node.visible === 0 || node.visible === '0' ? 0 : 1,
    children: []
  }
}

function buildTree(rows: any[]): MenuNode[] {
  const nodes = new Map<number, MenuNode>()
  const roots: MenuNode[] = []
  const sorted = [...rows].sort((a, b) => (Number(a.sortNo) || 0) - (Number(b.sortNo) || 0) || a.id - b.id)
  for (const row of sorted) nodes.set(row.id, normalize(row))
  for (const row of sorted) {
    const node = nodes.get(row.id)!
    const parent = row.parentId && nodes.get(row.parentId)
    if (parent) parent.children.push(node)
    else roots.push(node)
  }
  const sortDeep = (list: MenuNode[]) => {
    list.sort((a, b) => a.sortNo - b.sortNo)
    list.forEach((item) => sortDeep(item.children))
  }
  sortDeep(roots)
  return roots
}

async function load() {
  loading.value = true
  hydrating.value = true
  try {
    const rows = await api.get<any[]>('/tenant/system/menus', { endType: endType.value })
    treeData.value = buildTree(rows || [])
    loadedEnd.value = endType.value
    selected.value = treeData.value[0] || null
    previewActive.value = selected.value?.uid || ''
    phoneActive.value = treeData.value[0]?.uid || ''
    await nextTick()
    if (selected.value) treeRef.value?.setCurrentKey?.(selected.value.uid)
    dirty.value = false
  } catch {
    treeData.value = []
    selected.value = null
  } finally {
    hydrating.value = false
    loading.value = false
  }
}

async function onEndChange(value: any) {
  if (value === loadedEnd.value) return
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('切换端类型将丢弃当前未保存的调整，是否继续？', '切换确认', {
        type: 'warning',
        confirmButtonText: '切换',
        cancelButtonText: '取消'
      })
    } catch {
      endType.value = loadedEnd.value
      return
    }
  }
  await load()
}

function levelNumber(node: MenuNode) {
  const walk = (nodes: MenuNode[], depth: number): number => {
    for (const item of nodes) {
      if (item.uid === node.uid) return depth
      if (item.children?.length) {
        const found = walk(item.children, depth + 1)
        if (found) return found
      }
    }
    return 0
  }
  return walk(treeData.value, 1)
}

function levelOf(node: MenuNode) {
  const level = levelNumber(node)
  return level > 1 ? `第 ${level} 级菜单` : '一级菜单'
}

function canNest(node: MenuNode) {
  const level = levelNumber(node)
  return level > 0 && level < currentConfig.value.maxDepth
}

function iconComponent(name: string) {
  return name && icons[name] ? icons[name] : null
}

function onNodeClick(data: MenuNode) {
  selected.value = data
  previewActive.value = data.uid
  formRef.value?.clearValidate()
}

function addTop() {
  if (endType.value === 'MP' && treeData.value.length >= 3) {
    ElMessage.warning('公众号菜单一级最多 3 个')
    return
  }
  if ((endType.value === 'TABBAR' || endType.value === 'H5') && treeData.value.length >= 5) {
    ElMessage.warning(`${endType.value === 'TABBAR' ? 'TabBar' : 'H5'} 菜单最多 5 个`)
    return
  }
  const node: MenuNode = {
    uid: newUid(),
    title: '新菜单',
    titleEn: '',
    icon: '',
    path: '',
    component: '',
    permission: '',
    sortNo: (treeData.value.length + 1) * 10,
    visible: 1,
    children: []
  }
  treeData.value.push(node)
  selected.value = node
  previewActive.value = node.uid
  dirty.value = true
}

function addChild(parent: MenuNode) {
  if (!canNest(parent)) {
    ElMessage.warning(endType.value === 'MP' ? '公众号菜单最多两级' : '当前端类型不支持子菜单')
    return
  }
  if (endType.value === 'MP' && parent.children.length >= 5) {
    ElMessage.warning('公众号菜单二级最多 5 个')
    return
  }
  const node: MenuNode = {
    uid: newUid(),
    title: '新菜单',
    titleEn: '',
    icon: '',
    path: '',
    component: '',
    permission: '',
    sortNo: (parent.children.length + 1) * 10,
    visible: 1,
    children: []
  }
  parent.children.push(node)
  selected.value = node
  treeRef.value?.setCurrentKey?.(node.uid)
  previewActive.value = node.uid
  dirty.value = true
}

async function removeNode(node: MenuNode) {
  try {
    await ElMessageBox.confirm(
      `确定删除菜单「${node.title || '未命名菜单'}」吗？其下子菜单将一并删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const removeFrom = (list: MenuNode[]): boolean => {
    const index = list.findIndex((item) => item.uid === node.uid)
    if (index >= 0) {
      list.splice(index, 1)
      return true
    }
    return list.some((item) => removeFrom(item.children))
  }
  removeFrom(treeData.value)
  if (selected.value?.uid === node.uid) selected.value = treeData.value[0] || null
  dirty.value = true
}

function allowDrop(dragging: any, dropNode: any, type: 'prev' | 'inner' | 'next') {
  const maxDepth = currentConfig.value.maxDepth
  const resultDepth = type === 'inner' ? dropNode.level + 1 : dropNode.level
  if (resultDepth > maxDepth) return false
  if (type === 'inner' && endType.value === 'MP') {
    if (dropNode.level !== 1) return false
    if ((dropNode.childNodes?.length || 0) >= 5) return false
    if ((dragging.childNodes?.length || 0) > 0) return false
  }
  if (type === 'inner' && !['ADMIN', 'PORTAL', 'MP'].includes(endType.value)) return false
  return true
}

function onDrop() {
  resort(treeData.value)
  dirty.value = true
}

function resort(list: MenuNode[]) {
  list.forEach((node, index) => {
    node.sortNo = (index + 1) * 10
    resort(node.children)
  })
}

function collectMaxDepth(nodes: MenuNode[], depth = 1): number {
  let max = depth
  for (const node of nodes) {
    if (node.children?.length) max = Math.max(max, collectMaxDepth(node.children, depth + 1))
  }
  return max
}

function validateTree(): string {
  const top = treeData.value.length
  if (!top) return '请至少添加一个菜单'
  const walk = (nodes: MenuNode[], depth: number): string => {
    for (const node of nodes) {
      if (!node.title?.trim()) return '存在未填写标题的菜单'
      if (!node.path?.trim()) return `菜单「${node.title}」缺少路径`
      if (node.children?.length && depth >= currentConfig.value.maxDepth) {
        return `菜单「${node.title}」层级超出限制`
      }
      const childError = walk(node.children, depth + 1)
      if (childError) return childError
    }
    return ''
  }
  const error = walk(treeData.value, 1)
  if (error) return error

  if (endType.value === 'MP') {
    if (top > 3) return '公众号菜单一级最多 3 个'
    for (const node of treeData.value) {
      if (node.children.length > 5) return `「${node.title}」的二级菜单不能超过 5 个`
      if (collectMaxDepth(node.children) > 1) return '公众号菜单最多两级'
      if (node.children.length > 0) {
        for (const child of node.children) {
          if (!child.title?.trim() || !child.path?.trim()) return '二级菜单需填写标题与路径'
        }
      }
    }
  }
  if (endType.value === 'TABBAR' && (top < 2 || top > 5)) return '小程序 TabBar 需 2~5 个一级菜单'
  if (endType.value === 'H5' && (top > 5 || collectMaxDepth(treeData.value) > 1)) return 'H5 底部导航最多 5 个且不能有子级'
  if (endType.value === 'TABBAR' && collectMaxDepth(treeData.value) > 1) return 'TabBar 菜单不能有子级'
  return ''
}

function serialize(nodes: MenuNode[]): any[] {
  return nodes.map((node) => ({
    title: node.title.trim(),
    titleEn: node.titleEn,
    icon: node.icon,
    path: node.path,
    component: node.component,
    permission: node.permission,
    sortNo: node.sortNo,
    visible: node.visible,
    children: serialize(node.children || [])
  }))
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (selected.value && !valid) return
  const error = validateTree()
  if (error) {
    ElMessage.warning(error)
    return
  }
  resort(treeData.value)
  const keepPath = selected.value?.path
  saving.value = true
  try {
    await api.post('/tenant/system/menus/save', { endType: endType.value, menus: serialize(treeData.value) })
    ElMessage.success('菜单已保存并生效')
    await load()
    if (keepPath) {
      const walk = (nodes: MenuNode[]): MenuNode | null => {
        for (const item of nodes) {
          if (item.path === keepPath) return item
          const found = walk(item.children)
          if (found) return found
        }
        return null
      }
      const matched = walk(treeData.value)
      if (matched) {
        selected.value = matched
        previewActive.value = matched.uid
        await nextTick()
        treeRef.value?.setCurrentKey?.(matched.uid)
      }
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.menu-workspace {
  display: grid;
  grid-template-columns: 310px minmax(360px, 1fr) 390px;
  gap: 16px;
  align-items: start;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 650;
  }
  p {
    margin: 4px 0 0;
    font-size: 11.5px;
    color: var(--ink-3);
  }
}

.rule-alert {
  margin-bottom: 12px;
  :deep(.el-alert__title) {
    font-size: 11.5px;
    line-height: 1.5;
  }
}

.menu-tree {
  background: transparent;
  min-height: 200px;
  :deep(.el-tree-node__content) {
    height: 38px;
    border-radius: 7px;
    margin-bottom: 2px;
  }
  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: var(--brand-soft);
  }
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: 1;
  min-width: 0;
  padding-right: 4px;

  &.muted .node-title {
    color: var(--ink-3);
  }

  .node-icon {
    color: #667085;
    font-size: 15px;
  }
  .node-title {
    flex: 1;
    font-size: 12.5px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .node-flag {
    font-size: 10px;
    color: #b25e09;
    background: #fef3e2;
    border-radius: 4px;
    padding: 1px 5px;
  }
  .node-actions {
    display: none;
    gap: 2px;
  }
  &:hover .node-actions {
    display: inline-flex;
  }
}

.tree-foot {
  margin-top: 12px;
  font-size: 11px;
  color: var(--ink-3);
  text-align: center;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.property-pane {
  .el-form-item {
    margin-bottom: 14px;
  }
}

.icon-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.switch-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ink-3);
}

.form-actions {
  display: flex;
  gap: 8px;
  padding-top: 6px;
  border-top: 1px dashed var(--line);
}

.preview-panel {
  padding: 18px;
}

.preview-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 650;
  }
  p {
    margin: 4px 0 0;
    font-size: 11.5px;
    color: var(--ink-3);
  }
}

.pc-shell {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #d8e0ec;
  background: #fff;
  min-height: 420px;
}

.pc-bar {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 10px;
  background: #eef2f8;
  border-bottom: 1px solid #e1e7f0;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    &.red { background: #f97066; }
    &.yellow { background: #fdb022; }
    &.green { background: #32d583; }
  }
  .pc-url {
    flex: 1;
    margin-left: 8px;
    font-size: 10.5px;
    color: #667085;
    background: #fff;
    border-radius: 4px;
    padding: 2px 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.pc-body {
  display: grid;
  grid-template-columns: 128px 1fr;
  min-height: 376px;
}

.pc-side {
  background: linear-gradient(180deg, #0e1729, #16233d);
  padding: 12px 8px;
}

.pc-brand {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 4px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);

  .brand-mark {
    width: 24px;
    height: 24px;
    border-radius: 7px;
    background: var(--brand);
    color: #fff;
    display: grid;
    place-items: center;
    font-size: 12px;
    font-weight: 600;
  }
  strong {
    display: block;
    color: #fff;
    font-size: 11px;
    font-weight: 600;
  }
  em {
    display: block;
    font-style: normal;
    font-size: 9px;
    color: rgba(255, 255, 255, 0.45);
  }
}

.pc-nav {
  padding-top: 6px;
}

.pc-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &.sub {
    padding-left: 24px;
    font-size: 10.5px;
    color: rgba(255, 255, 255, 0.55);
  }
  &.active {
    background: rgba(31, 111, 235, 0.9);
    color: #fff;
  }
}

.pc-main {
  background: #f7f9fc;
  padding: 12px;
}

.pc-crumb {
  font-size: 10.5px;
  color: var(--ink-3);
  margin-bottom: 10px;
}

.pc-placeholder {
  background: #fff;
  border: 1px dashed #dfe5ee;
  border-radius: 8px;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--ink-3);

  p {
    margin: 0;
    font-size: 13px;
    color: var(--ink-2);
    font-weight: 600;
  }
  span {
    font-size: 10.5px;
  }
}

.phone-shell {
  display: flex;
  justify-content: center;
  padding: 4px 0 10px;
}

.phone-screen {
  width: 250px;
  min-height: 430px;
  border-radius: 26px;
  border: 8px solid #101828;
  background: #f7f9fc;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.phone-status {
  display: flex;
  justify-content: space-between;
  padding: 7px 12px 3px;
  font-size: 9px;
  color: #101828;
  font-weight: 600;
}

.phone-title {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 650;
  background: #fff;
  border-bottom: 1px solid #eef1f5;
}

.phone-body {
  flex: 1;
  padding: 10px;
  min-height: 300px;
}

.phone-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.phone-grid-item {
  background: #fff;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  padding: 10px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  font-size: 9.5px;
  color: #475467;

  .el-icon {
    font-size: 16px;
    color: var(--brand);
  }
}

.phone-placeholder {
  height: 100%;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: var(--ink-3);

  p {
    margin: 0;
    font-size: 12px;
    font-weight: 600;
    color: var(--ink-2);
  }
  span {
    font-size: 9.5px;
  }
}

.phone-tabbar {
  display: flex;
  background: #fff;
  border-top: 1px solid #eef1f5;
  padding: 5px 3px 8px;
  min-height: 46px;
}

.phone-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 8.5px;
  color: #98a2b3;
  cursor: pointer;

  .el-icon {
    font-size: 15px;
  }
  span {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 44px;
  }
  &.active {
    color: var(--brand);
  }
}

.phone-tab-empty {
  width: 100%;
  text-align: center;
  font-size: 9.5px;
  color: var(--ink-3);
  align-self: center;
}

.preview-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
  font-size: 10.5px;
  color: var(--ink-3);
  line-height: 1.5;
}

@media (max-width: 1500px) {
  .menu-workspace {
    grid-template-columns: 280px minmax(320px, 1fr) 360px;
  }
}

@media (max-width: 1240px) {
  .menu-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
