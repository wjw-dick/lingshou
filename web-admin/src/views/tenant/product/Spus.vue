<template>
  <div class="page">
    <PageHeader title="商品档案" subtitle="SPU / SKU、一品多码、多价格体系、保质期与线上可售控制。">
      <template #actions>
        <el-upload :show-file-list="false" :http-request="importExcel" accept=".xlsx,.xls" style="display:inline-block">
          <el-button :icon="Upload">批量导入</el-button>
        </el-upload>
        <el-button :icon="Download" @click="exportExcel">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索商品名称 / 编码" clearable style="width: 220px" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.categoryId" placeholder="商品分类" clearable style="width: 160px" @change="search">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
          <el-option label="在售" value="ON_SALE" />
          <el-option label="已下架" value="OFF_SHELF" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 个商品</span>
      </div>

      <el-table :data="rows" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="商品" min-width="260" fixed>
          <template #default="{ row }">
            <div class="product-cell">
              <div class="thumb">
                <img v-if="row.spu.mainImage" :src="row.spu.mainImage" alt="" />
                <el-icon v-else><Goods /></el-icon>
              </div>
              <div>
                <strong>{{ row.spu.name }}</strong>
                <span>{{ row.spu.spuCode }} · {{ row.spu.spec || '标准规格' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="brandName" label="品牌" width="110" />
        <el-table-column label="零售价" width="110">
          <template #default="{ row }"><span class="money">¥{{ money(row.spu.priceRetail) }}</span></template>
        </el-table-column>
        <el-table-column label="会员价" width="110">
          <template #default="{ row }"><span class="money muted">¥{{ money(row.spu.priceMember) }}</span></template>
        </el-table-column>
        <el-table-column label="保质期" width="100">
          <template #default="{ row }">{{ row.spu.shelfLifeDays }} 天</template>
        </el-table-column>
        <el-table-column label="贮存" width="90">
          <template #default="{ row }">
            <el-tag :type="row.spu.storageCondition === 'COLD' ? 'primary' : row.spu.storageCondition === 'FROZEN' ? 'info' : 'success'"
                    effect="plain" size="small">
              {{ storageLabel(row.spu.storageCondition) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="线上" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.spu.onlineSale === 1" type="success" effect="plain" size="small">可售</el-tag>
            <el-tag v-else type="info" effect="plain" size="small">线下</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.spu.status" /></template>
        </el-table-column>
        <el-table-column label="SKU" width="80">
          <template #default="{ row }">{{ row.skuCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row.spu)">编辑</el-button>
            <el-button link type="primary" @click="toggleStatus(row.spu)">
              {{ row.spu.status === 'ON_SALE' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="remove(row.spu)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="spu.id ? '编辑商品' : '新增商品'" width="980px" top="4vh" destroy-on-close>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息" name="base">
          <el-form :model="spu" label-width="100px" label-position="right">
            <div class="form-grid">
              <el-form-item label="商品编码" required><el-input v-model="spu.spuCode" placeholder="SPU001" /></el-form-item>
              <el-form-item label="商品名称" required><el-input v-model="spu.name" /></el-form-item>
              <el-form-item label="英文名称"><el-input v-model="spu.nameEn" /></el-form-item>
              <el-form-item label="商品分类" required>
                <el-select v-model="spu.categoryId" style="width: 100%">
                  <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="品牌">
                <el-select v-model="spu.brandId" clearable style="width: 100%">
                  <el-option v-for="brand in brands" :key="brand.id" :label="brand.name" :value="brand.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="单位"><el-input v-model="spu.unit" placeholder="件 / 份 / 瓶" /></el-form-item>
              <el-form-item label="规格"><el-input v-model="spu.spec" placeholder="如 500ml" /></el-form-item>
              <el-form-item label="销售状态">
                <el-radio-group v-model="spu.status">
                  <el-radio value="ON_SALE">在售</el-radio>
                  <el-radio value="OFF_SHELF">下架</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>
            <el-form-item label="商品主图">
              <div style="width: 100%">
                <ImageUpload v-model="spu.mainImage" title="商品主图" hint="建议 1:1，≤5MB" biz-type="PRODUCT" />
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="价格与 SKU" name="price">
          <el-form :model="spu" label-width="100px">
            <div class="form-grid">
              <el-form-item label="指导价"><el-input-number v-model="spu.priceGuide" :precision="2" :min="0" style="width:100%" /></el-form-item>
              <el-form-item label="零售价"><el-input-number v-model="spu.priceRetail" :precision="2" :min="0" style="width:100%" /></el-form-item>
              <el-form-item label="会员价"><el-input-number v-model="spu.priceMember" :precision="2" :min="0" style="width:100%" /></el-form-item>
              <el-form-item label="加盟供货价"><el-input-number v-model="spu.priceFranchise" :precision="2" :min="0" style="width:100%" /></el-form-item>
            </div>
          </el-form>
          <div class="section-head">
            <span class="section-title">SKU 列表</span>
            <el-button size="small" type="primary" plain :icon="Plus" @click="addSku">新增 SKU</el-button>
          </div>
          <el-table :data="skus" size="small" border>
            <el-table-column label="SKU 编码" min-width="120">
              <template #default="{ row }"><el-input v-model="row.skuCode" size="small" /></template>
            </el-table-column>
            <el-table-column label="条码" min-width="140">
              <template #default="{ row }"><el-input v-model="row.barcode" size="small" /></template>
            </el-table-column>
            <el-table-column label="规格" width="100">
              <template #default="{ row }"><el-input v-model="row.spec" size="small" /></template>
            </el-table-column>
            <el-table-column label="成本价" width="120">
              <template #default="{ row }"><el-input-number v-model="row.costPrice" :precision="2" :controls="false" size="small" style="width:100%" /></template>
            </el-table-column>
            <el-table-column label="零售价" width="120">
              <template #default="{ row }"><el-input-number v-model="row.priceRetail" :precision="2" :controls="false" size="small" style="width:100%" /></template>
            </el-table-column>
            <el-table-column label="会员价" width="120">
              <template #default="{ row }"><el-input-number v-model="row.priceMember" :precision="2" :controls="false" size="small" style="width:100%" /></template>
            </el-table-column>
            <el-table-column label="加盟价" width="120">
              <template #default="{ row }"><el-input-number v-model="row.priceFranchise" :precision="2" :controls="false" size="small" style="width:100%" /></template>
            </el-table-column>
            <el-table-column width="60" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="skus.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="商品属性" name="attrs">
          <el-form :model="spu" label-width="110px">
            <div class="form-grid">
              <el-form-item label="贮存条件">
                <el-select v-model="spu.storageCondition" style="width: 100%">
                  <el-option label="常温" value="NORMAL" />
                  <el-option label="冷藏" value="COLD" />
                  <el-option label="冷冻" value="FROZEN" />
                </el-select>
              </el-form-item>
              <el-form-item label="保质期" required>
                <el-input-number v-model="spu.shelfLifeDays" :min="1" :max="3650" style="width: 100%" />
              </el-form-item>
              <el-form-item label="产地"><el-input v-model="spu.origin" /></el-form-item>
              <el-form-item label="过敏原"><el-input v-model="spu.allergens" placeholder="如 麸质、蛋类" /></el-form-item>
              <el-form-item label="称重商品">
                <el-switch v-model="spu.isWeight" :active-value="1" :inactive-value="0" />
              </el-form-item>
              <el-form-item label="线上可售">
                <el-switch v-model="spu.onlineSale" :active-value="1" :inactive-value="0" />
              </el-form-item>
              <el-form-item label="烟草类">
                <el-switch v-model="spu.isTobacco" :active-value="1" :inactive-value="0" @change="onTobaccoChange" />
                <div class="form-tip">烟草类商品强制线下门店购买，线上不可售。</div>
              </el-form-item>
            </div>
            <el-form-item label="配料表">
              <el-input v-model="spu.ingredients" type="textarea" :rows="3" placeholder="用于商品详情与合规展示" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="商品详情" name="detail">
          <RichEditor v-model="spu.detailHtml" height="360px" placeholder="填写商品详情、食用方式、贮存提示等" />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存商品</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Upload } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import RichEditor from '@/components/RichEditor.vue'
import { api } from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const categories = ref<any[]>([])
const brands = ref<any[]>([])
const query = reactive({ page: 1, size: 10, keyword: '', categoryId: '', status: '' })
const dialogVisible = ref(false)
const activeTab = ref('base')
const spu = ref<any>({})
const skus = ref<any[]>([])

onMounted(async () => {
  await Promise.all([loadOptions(), load()])
})

async function loadOptions() {
  try {
    const [cats, brandRows] = await Promise.all([
      api.get('/tenant/products/categories'),
      api.get('/tenant/products/brands')
    ])
    categories.value = cats || []
    brands.value = brandRows || []
  } catch {
    // 提示由拦截器处理
  }
}

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/tenant/products/spus', query)
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', categoryId: '', status: '' })
  load()
}

function defaultSku(index: number) {
  return {
    id: null,
    skuCode: `${spu.value.spuCode || 'SKU'}-${index + 1}`,
    barcode: '',
    spec: spu.value.spec || '',
    unit: spu.value.unit || '件',
    isWeight: spu.value.isWeight || 0,
    costPrice: 0,
    priceGuide: spu.value.priceGuide || 0,
    priceRetail: spu.value.priceRetail || 0,
    priceMember: spu.value.priceMember || 0,
    priceFranchise: spu.value.priceFranchise || 0,
    status: 'ON_SALE'
  }
}

function openCreate() {
  spu.value = {
    spuCode: '', name: '', nameEn: '', categoryId: categories.value[0]?.id || null, brandId: null,
    unit: '件', spec: '', isWeight: 0, storageCondition: 'NORMAL', shelfLifeDays: 365,
    origin: '', ingredients: '', allergens: '', isTobacco: 0, onlineSale: 1, status: 'ON_SALE',
    mainImage: '', detailHtml: '', priceGuide: 0, priceRetail: 0, priceMember: 0, priceFranchise: 0
  }
  skus.value = [defaultSku(0)]
  activeTab.value = 'base'
  dialogVisible.value = true
}

async function openEdit(row: any) {
  const data: any = await api.get(`/tenant/products/spus/${row.id}`)
  spu.value = { ...data.spu }
  skus.value = (data.skus || []).map((item: any) => ({ ...item }))
  if (!skus.value.length) skus.value = [defaultSku(0)]
  activeTab.value = 'base'
  dialogVisible.value = true
}

function addSku() {
  skus.value.push(defaultSku(skus.value.length))
}

function onTobaccoChange(value: number) {
  if (value === 1) {
    spu.value.onlineSale = 0
  }
}

async function save() {
  if (!spu.value.spuCode || !spu.value.name || !spu.value.categoryId) {
    ElMessage.warning('请填写商品编码、名称和分类')
    activeTab.value = 'base'
    return
  }
  if (!skus.value.length) {
    ElMessage.warning('请至少配置一个 SKU')
    activeTab.value = 'price'
    return
  }
  if (spu.value.isTobacco === 1) spu.value.onlineSale = 0
  saving.value = true
  try {
    const payload = { spu: spu.value, skus: skus.value }
    if (spu.value.id) {
      await api.put(`/tenant/products/spus/${spu.value.id}`, payload)
      ElMessage.success('商品已更新')
    } else {
      await api.post('/tenant/products/spus', payload)
      ElMessage.success('商品已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: any) {
  const next = row.status === 'ON_SALE' ? 'OFF_SHELF' : 'ON_SALE'
  await api.post(`/tenant/products/spus/${row.id}/status`, null, { params: { status: next } })
  ElMessage.success(next === 'ON_SALE' ? '已上架' : '已下架')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」？`, '删除商品', { type: 'warning' })
  await api.del(`/tenant/products/spus/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function importExcel(options: any) {
  const form = new FormData()
  form.append('file', options.file)
  try {
    const result: any = await api.postForm('/tenant/products/spus/import', form)
    ElMessage.success(`导入完成：成功 ${result.success} 条${result.errors?.length ? `，${result.errors.length} 条异常` : ''}`)
    if (result.errors?.length) {
      ElMessageBox.alert(result.errors.slice(0, 12).join('<br/>'), '导入异常明细', { dangerouslyUseHTMLString: true })
    }
    load()
  } finally {
    options.onSuccess?.()
  }
}

function exportExcel() {
  api.download('/tenant/products/spus/export', {}, '商品档案.xlsx')
}

function money(value: any) {
  return Number(value || 0).toFixed(2)
}

function storageLabel(value: string) {
  return { NORMAL: '常温', COLD: '冷藏', FROZEN: '冷冻' }[value] || value
}
</script>

<style scoped lang="scss">
.product-cell { display: flex; align-items: center; gap: 10px; }
.thumb {
  width: 42px; height: 42px; border-radius: 9px; overflow: hidden; background: #eef4fe; color: #1f6feb;
  display: grid; place-items: center; flex-shrink: 0;
  img { width: 100%; height: 100%; object-fit: cover; }
}
.product-cell div:last-child { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.product-cell strong { font-size: 13px; font-weight: 550; }
.product-cell span { font-size: 11px; color: var(--ink-3); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin: 6px 0 10px; }
</style>
