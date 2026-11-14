<template>
  <div class="page" v-loading="loading">
    <PageHeader title="报表看板" subtitle="销售、毛利、库存与会员多维度经营分析。">
      <template #actions>
        <el-select v-model="storeId" placeholder="全部门店" clearable style="width: 170px" @change="load">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
          style="width: 250px"
          @change="load"
        />
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :icon="Download" @click="exportReport('sales')">导出销售</el-button>
        <el-dropdown @command="exportReport">
          <el-button :icon="Download">
            更多导出<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="inventory">导出库存报表</el-dropdown-item>
              <el-dropdown-item command="members">导出会员报表</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </PageHeader>

    <div class="today-strip card">
      <div class="today-item">
        <span>今日销售额</span>
        <strong>¥ {{ money(dashboard.todaySales) }}</strong>
      </div>
      <div class="today-item">
        <span>今日订单</span>
        <strong>{{ number(dashboard.todayOrders) }} 单</strong>
      </div>
      <div class="today-item">
        <span>今日客单价</span>
        <strong>¥ {{ money(dashboard.avgTicket) }}</strong>
      </div>
      <div class="today-item">
        <span>今日新增会员</span>
        <strong>{{ number(dashboard.todayNewMembers) }} 人</strong>
      </div>
      <div class="today-item">
        <span>低库存商品</span>
        <strong>{{ number(dashboard.lowStockCount) }} 项</strong>
      </div>
    </div>

    <div class="kpi-grid">
      <StatCard label="区间销售额" prefix="¥" :value="kpi.sales" icon="Money" :hint="`${rangeDays} 天已支付金额`" color="#1f6feb" />
      <StatCard label="订单量" :value="kpi.orders" unit="单" icon="ShoppingCart" hint="区间内已支付订单" color="#14b8a6" />
      <StatCard label="客单价" prefix="¥" :value="kpi.avgTicket" icon="TrendCharts" hint="销售额 / 订单量" color="#f0a020" />
      <StatCard label="毛利" prefix="¥" :value="grossProfit.totalProfit || 0" icon="DataLine" :hint="`毛利率 ${kpi.margin}%`" color="#18a058" />
      <StatCard label="会员总数" :value="members.total || 0" unit="人" icon="User" :hint="`本月新增 ${number(members.newThisMonth)} 人`" color="#7c3aed" />
      <StatCard label="临期批次" :value="inventory.nearExpiryCount || 0" unit="批" icon="Warning" :hint="`慢动销 ${number(inventory.slowMovingCount)} · 缺货 ${number(inventory.stockoutCount)}`" color="#d03050" />
    </div>

    <div class="card card-pad">
      <div class="card-head">
        <div>
          <h3>销售趋势</h3>
          <p>按日汇总实付销售额与订单量</p>
        </div>
        <el-tag effect="plain" round>{{ rangeText }}</el-tag>
      </div>
      <div class="chart-wrap">
        <div ref="trendRef" class="chart chart-lg"></div>
        <div v-if="!salesRows.length" class="chart-empty"><el-empty description="暂无销售数据" :image-size="56" /></div>
      </div>
    </div>

    <div class="grid-2" style="margin-top: 16px">
      <div class="card card-pad">
        <div class="card-head">
          <div><h3>品类销售</h3><p>区间内各品类销售额占比</p></div>
        </div>
        <div class="chart-wrap">
          <div ref="categoryRef" class="chart"></div>
          <div v-if="!categoryRows.length" class="chart-empty"><el-empty description="暂无品类数据" :image-size="56" /></div>
        </div>
      </div>
      <div class="card card-pad">
        <div class="card-head">
          <div><h3>支付方式</h3><p>区间内支付结构分布</p></div>
        </div>
        <div class="chart-wrap">
          <div ref="paymentRef" class="chart"></div>
          <div v-if="!paymentRows.length" class="chart-empty"><el-empty description="暂无支付数据" :image-size="56" /></div>
        </div>
      </div>
    </div>

    <div class="grid-2" style="margin-top: 16px">
      <div class="card card-pad">
        <div class="card-head">
          <div><h3>品类毛利</h3><p>销售额、成本与毛利对比</p></div>
        </div>
        <div class="chart-wrap">
          <div ref="profitRef" class="chart"></div>
          <div v-if="!grossProfit.rows?.length" class="chart-empty"><el-empty description="暂无毛利数据" :image-size="56" /></div>
        </div>
      </div>
      <div class="card card-pad">
        <div class="card-head">
          <div><h3>会员等级分布</h3><p>当前会员等级人数结构</p></div>
        </div>
        <div class="chart-wrap">
          <div ref="memberRef" class="chart"></div>
          <div v-if="!levelDistribution.length" class="chart-empty"><el-empty description="暂无会员数据" :image-size="56" /></div>
        </div>
        <div class="mini-metrics">
          <div><span>30 天活跃</span><strong>{{ number(members.active30d) }}</strong></div>
          <div><span>复购会员</span><strong>{{ number(members.repurchase) }}</strong></div>
          <div><span>储值余额</span><strong>¥ {{ money(members.walletBalance) }}</strong></div>
          <div><span>券核销</span><strong>{{ number(members.couponUsed) }} / {{ number(members.couponIssued) }}</strong></div>
        </div>
      </div>
    </div>

    <div class="grid-2" style="margin-top: 16px">
      <div class="card card-pad table-block">
        <div class="card-head">
          <div><h3>品类毛利明细</h3><p>按品类核算销售成本与毛利</p></div>
          <span class="muted">毛利率 {{ kpi.margin }}%</span>
        </div>
        <el-table :data="grossProfit.rows || []" stripe size="small" max-height="320">
          <el-table-column label="品类" min-width="110" prop="category" />
          <el-table-column label="销售额" width="110" align="right">
            <template #default="{ row }">¥ {{ money(row.sales) }}</template>
          </el-table-column>
          <el-table-column label="成本" width="110" align="right">
            <template #default="{ row }">¥ {{ money(row.cost) }}</template>
          </el-table-column>
          <el-table-column label="毛利" width="110" align="right">
            <template #default="{ row }"><span class="money">¥ {{ money(row.profit) }}</span></template>
          </el-table-column>
          <el-table-column label="毛利率" width="90" align="right">
            <template #default="{ row }">{{ rowMargin(row) }}%</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无毛利明细" :image-size="60" />
          </template>
        </el-table>
      </div>

      <div class="card card-pad table-block">
        <div class="card-head">
          <div><h3>库存周转</h3><p>库存量与近 30 天周转天数</p></div>
          <span class="muted">慢动销 {{ number(inventory.slowMovingCount) }} 项</span>
        </div>
        <el-table :data="inventory.turnover || []" stripe size="small" max-height="320">
          <el-table-column label="商品" min-width="150" prop="sku_name" show-overflow-tooltip />
          <el-table-column label="库存" width="80" align="right">
            <template #default="{ row }">{{ number(row.stock_qty) }}</template>
          </el-table-column>
          <el-table-column label="30 天销量" width="100" align="right">
            <template #default="{ row }">{{ number(row.sales_30d) }}</template>
          </el-table-column>
          <el-table-column label="周转天数" width="100" align="right">
            <template #default="{ row }">
              <span :class="{ 'text-danger': Number(row.turnover_days) >= 999 }">{{ turnoverText(row.turnover_days) }}</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无库存数据" :image-size="60" />
          </template>
        </el-table>
      </div>
    </div>

    <div class="grid-2" style="margin-top: 16px">
      <div class="card card-pad table-block">
        <div class="card-head">
          <div><h3>供应商履约</h3><p>供货单收货完成率</p></div>
        </div>
        <el-table :data="suppliers" stripe size="small" max-height="300">
          <el-table-column label="供应商" min-width="160" prop="supplier_name" show-overflow-tooltip />
          <el-table-column label="订单数" width="80" align="right">
            <template #default="{ row }">{{ number(row.order_count) }}</template>
          </el-table-column>
          <el-table-column label="订货量" width="80" align="right">
            <template #default="{ row }">{{ number(row.order_qty) }}</template>
          </el-table-column>
          <el-table-column label="收货量" width="80" align="right">
            <template #default="{ row }">{{ number(row.received_qty) }}</template>
          </el-table-column>
          <el-table-column label="履约率" width="120" align="right">
            <template #default="{ row }">
              <span class="rate-text">{{ Number(row.fulfillment_rate || 0).toFixed(1) }}%</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无供应商履约数据" :image-size="60" />
          </template>
        </el-table>
      </div>

      <div class="card card-pad table-block">
        <div class="card-head">
          <div><h3>加盟商排行</h3><p>累计销售额与分账金额</p></div>
        </div>
        <el-table :data="franchisees" stripe size="small" max-height="300">
          <el-table-column label="加盟商" min-width="160" prop="franchisee_name" show-overflow-tooltip />
          <el-table-column label="销售额" width="130" align="right">
            <template #default="{ row }"><span class="money">¥ {{ money(row.sales_amount) }}</span></template>
          </el-table-column>
          <el-table-column label="分账金额" width="130" align="right">
            <template #default="{ row }">¥ {{ money(row.commission_amount) }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无加盟商数据" :image-size="60" />
          </template>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Download, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { api } from '@/api/http'
import { useChart, chartPalette } from '@/composables/useChart'
import dayjs from 'dayjs'

const stores = ref<any[]>([])
const storeId = ref<number>()
const range = ref<string[]>([dayjs().subtract(29, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const loading = ref(false)

const dashboard = ref<any>({})
const salesRows = ref<any[]>([])
const categoryRows = ref<any[]>([])
const paymentRows = ref<any[]>([])
const grossProfit = ref<any>({ rows: [], totalSales: 0, totalCost: 0, totalProfit: 0 })
const inventory = ref<any>({ turnover: [], slowMovingCount: 0, stockoutCount: 0, nearExpiryCount: 0 })
const members = ref<any>({})
const suppliers = ref<any[]>([])
const franchisees = ref<any[]>([])

const trendRef = ref<HTMLElement>()
const categoryRef = ref<HTMLElement>()
const paymentRef = ref<HTMLElement>()
const profitRef = ref<HTMLElement>()
const memberRef = ref<HTMLElement>()

const kpi = computed(() => {
  const sales = salesRows.value.reduce((sum, row) => sum + Number(row.sales || 0), 0)
  const orders = salesRows.value.reduce((sum, row) => sum + Number(row.orders || 0), 0)
  const totalSales = Number(grossProfit.value.totalSales || 0)
  const totalProfit = Number(grossProfit.value.totalProfit || 0)
  return {
    sales,
    orders,
    avgTicket: orders ? sales / orders : 0,
    margin: totalSales ? ((totalProfit / totalSales) * 100).toFixed(1) : '0.0'
  }
})

const rangeDays = computed(() => {
  if (!range.value?.[0] || !range.value?.[1]) return 30
  return dayjs(range.value[1]).diff(dayjs(range.value[0]), 'day') + 1
})

const rangeText = computed(() => `${range.value?.[0] || '-'} ~ ${range.value?.[1] || '-'}`)

const levelDistribution = computed<any[]>(() => members.value.levelDistribution || [])

const trendOption = computed(() => {
  const rows = salesRows.value
  return {
    color: chartPalette,
    grid: { left: 8, right: 16, top: 42, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: rows.map((row) => dayjs(row.dimension).format('MM-DD')),
      axisLine: { lineStyle: { color: '#e4e7ec' } },
      axisLabel: { color: '#98a2b3' }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        splitLine: { lineStyle: { color: '#f2f4f7' } },
        axisLabel: { color: '#98a2b3' }
      },
      {
        type: 'value',
        name: '订单量',
        splitLine: { show: false },
        axisLabel: { color: '#98a2b3' }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: rows.map((row) => Number(row.sales || 0)),
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(31,111,235,0.22)' },
              { offset: 1, color: 'rgba(31,111,235,0.01)' }
            ]
          }
        },
        lineStyle: { width: 2.5 }
      },
      {
        name: '订单量',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 12,
        data: rows.map((row) => Number(row.orders || 0)),
        itemStyle: { color: 'rgba(20,184,166,0.65)', borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

const categoryOption = computed(() => ({
  color: chartPalette,
  tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c}（{d}%）' },
  legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#667085', fontSize: 11 } },
  series: [
    {
      type: 'pie',
      radius: ['44%', '68%'],
      center: ['36%', '52%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, formatter: '{b}\n{d}%', fontSize: 12, fontWeight: 600 } },
      data: categoryRows.value.map((row) => ({ name: row.dimension || '未分类', value: Number(row.sales || 0) }))
    }
  ]
}))

const paymentOption = computed(() => ({
  color: chartPalette,
  tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c}（{d}%）' },
  legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#667085', fontSize: 11 } },
  series: [
    {
      type: 'pie',
      radius: ['44%', '68%'],
      center: ['36%', '52%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, formatter: '{b}\n{d}%', fontSize: 12, fontWeight: 600 } },
      data: paymentRows.value.map((row) => ({ name: paymentLabel(row.dimension), value: Number(row.sales || 0) }))
    }
  ]
}))

const profitOption = computed(() => {
  const rows = grossProfit.value.rows || []
  return {
    color: chartPalette,
    grid: { left: 8, right: 16, top: 42, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { right: 0, top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    xAxis: {
      type: 'category',
      data: rows.map((row: any) => row.category || '-'),
      axisLine: { lineStyle: { color: '#e4e7ec' } },
      axisLabel: { color: '#98a2b3', interval: 0, hideOverlap: true }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f4f7' } },
      axisLabel: { color: '#98a2b3' }
    },
    series: [
      { name: '销售额', type: 'bar', barWidth: 16, data: rows.map((row: any) => Number(row.sales || 0)), itemStyle: { borderRadius: [4, 4, 0, 0] } },
      { name: '毛利', type: 'bar', barWidth: 16, data: rows.map((row: any) => Number(row.profit || 0)), itemStyle: { borderRadius: [4, 4, 0, 0] } }
    ]
  }
})

const memberOption = computed(() => ({
  color: chartPalette,
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 人（{d}%）' },
  legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#667085', fontSize: 11 } },
  series: [
    {
      type: 'pie',
      radius: ['44%', '68%'],
      center: ['36%', '52%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, formatter: '{b}\n{c} 人', fontSize: 12, fontWeight: 600 } },
      data: levelDistribution.value.map((row: any) => ({ name: row.level_name || '未分级', value: Number(row.member_count || 0) }))
    }
  ]
}))

useChart(trendRef, trendOption)
useChart(categoryRef, categoryOption)
useChart(paymentRef, paymentOption)
useChart(profitRef, profitOption)
useChart(memberRef, memberOption)

onMounted(() => {
  loadStores()
  load()
})

async function loadStores() {
  try {
    stores.value = await api.get('/tenant/stores/options')
  } catch {
    stores.value = []
  }
}

async function load() {
  loading.value = true
  const params = {
    storeId: storeId.value || undefined,
    start: range.value?.[0],
    end: range.value?.[1]
  }
  try {
    const results = await Promise.all([
      api.get('/tenant/reports/dashboard', { storeId: storeId.value || undefined }).catch(() => ({})),
      api.get('/tenant/reports/sales', { ...params, dimension: 'day' }).catch(() => []),
      api.get('/tenant/reports/sales', { ...params, dimension: 'category' }).catch(() => []),
      api.get('/tenant/reports/sales', { ...params, dimension: 'payment' }).catch(() => []),
      api.get('/tenant/reports/gross-profit', { start: params.start, end: params.end }).catch(() => ({ rows: [], totalSales: 0, totalCost: 0, totalProfit: 0 })),
      api.get('/tenant/reports/inventory', { storeId: storeId.value || undefined }).catch(() => ({ turnover: [], slowMovingCount: 0, stockoutCount: 0, nearExpiryCount: 0 })),
      api.get('/tenant/reports/members').catch(() => ({})),
      api.get('/tenant/reports/supplier-fulfillment').catch(() => []),
      api.get('/tenant/reports/franchise-ranking').catch(() => [])
    ])
    dashboard.value = results[0] || {}
    salesRows.value = results[1] || []
    categoryRows.value = results[2] || []
    paymentRows.value = results[3] || []
    grossProfit.value = results[4] || { rows: [], totalSales: 0, totalCost: 0, totalProfit: 0 }
    inventory.value = results[5] || { turnover: [], slowMovingCount: 0, stockoutCount: 0, nearExpiryCount: 0 }
    members.value = results[6] || {}
    suppliers.value = results[7] || []
    franchisees.value = results[8] || []
  } finally {
    loading.value = false
  }
}

async function exportReport(type: string) {
  const names: Record<string, string> = {
    sales: '销售报表',
    inventory: '库存报表',
    members: '会员报表'
  }
  try {
    await api.download(
      '/tenant/reports/export',
      {
        type,
        storeId: storeId.value || undefined,
        start: range.value?.[0],
        end: range.value?.[1]
      },
      `${names[type] || '经营报表'}.xlsx`
    )
    ElMessage.success('导出任务已完成')
  } catch {
    // handled by interceptor
  }
}

function paymentLabel(type: string) {
  const map: Record<string, string> = {
    CASH: '现金',
    WECHAT: '微信支付',
    ALIPAY: '支付宝',
    MEMBER: '会员储值',
    CARD: '银行卡',
    MIXED: '混合支付',
    UNKNOWN: '未知方式'
  }
  return map[type] || type || '未知方式'
}

function rowMargin(row: any) {
  const sales = Number(row.sales || 0)
  if (!sales) return '0.0'
  return ((Number(row.profit || 0) / sales) * 100).toFixed(1)
}

function turnoverText(value: any) {
  const days = Number(value || 0)
  if (days >= 999) return '无动销'
  return `${days.toLocaleString('zh-CN', { maximumFractionDigits: 1 })} 天`
}

function number(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped lang="scss">
.today-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  margin-bottom: 16px;
}

.today-item {
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;

  span {
    font-size: 11.5px;
    color: var(--ink-3);
  }

  strong {
    font-size: 17px;
    font-weight: 680;
    font-variant-numeric: tabular-nums;
  }
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;

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

.chart-wrap {
  position: relative;
}

.chart {
  width: 100%;
  height: 280px;
}

.chart-lg {
  height: 320px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.76);
}

.table-block {
  min-width: 0;
}

.mini-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 6px;

  > div {
    background: #fbfcfe;
    border: 1px solid var(--line);
    border-radius: 8px;
    padding: 9px 10px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    span {
      font-size: 10.5px;
      color: var(--ink-3);
    }

    strong {
      font-size: 13.5px;
      font-weight: 650;
      font-variant-numeric: tabular-nums;
    }
  }
}

.rate-text {
  color: #1454c8;
  font-weight: 600;
}

.text-danger {
  color: #d03050;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .today-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
