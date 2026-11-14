<template>
  <div class="page">
    <PageHeader title="工作台" :subtitle="`欢迎回来，${auth.displayName}。这里是 ${tenantName} 的经营概览。`">
      <template #actions>
        <el-select v-model="storeId" placeholder="全部门店" clearable style="width: 180px" @change="load">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="ShoppingCart" @click="router.push('/sales/pos')">开始收银</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="今日销售额" prefix="¥" :value="data.todaySales || 0" icon="Money"
                hint="较昨日" :trend="salesTrend" color="#1f6feb" />
      <StatCard label="今日订单" :value="data.todayOrders || 0" unit="单" icon="ShoppingCart"
                hint="含线上与收银" color="#14b8a6" />
      <StatCard label="今日客单价" prefix="¥" :value="data.avgTicket || 0" icon="TrendCharts"
                hint="实付金额 / 订单数" color="#f0a020" />
      <StatCard label="今日新增会员" :value="data.todayNewMembers || 0" unit="人" icon="User"
                hint="UnionID 统一会员" color="#7c3aed" />
      <StatCard label="低库存商品" :value="data.lowStockCount || 0" unit="项" icon="Warning"
                hint="低于安全库存" color="#d03050" />
      <StatCard label="临期批次" :value="data.nearExpiryCount || 0" unit="批" icon="Clock"
                hint="3 天内到期" color="#d97706" />
    </div>

    <div class="grid-2">
      <div class="card card-pad">
        <div class="card-head">
          <div>
            <h3>销售趋势</h3>
            <p>近 7 天实付销售额与订单量</p>
          </div>
          <el-tag effect="plain" round>近 7 天</el-tag>
        </div>
        <div ref="trendRef" class="chart chart-lg"></div>
      </div>
      <div class="card card-pad">
        <div class="card-head">
          <div>
            <h3>门店排行</h3>
            <p>近 30 天销售额</p>
          </div>
          <el-button text type="primary" size="small" @click="router.push('/reports')">查看报表</el-button>
        </div>
        <div class="rank-list">
          <div v-for="(item, index) in data.storeRanking || []" :key="item.store_id" class="rank-item">
            <span class="rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <div class="rank-main">
              <strong>{{ item.store_name }}</strong>
              <span>{{ item.orders }} 单</span>
            </div>
            <span class="rank-val money">¥{{ format(item.sales) }}</span>
          </div>
          <el-empty v-if="!(data.storeRanking || []).length" description="暂无门店销售数据" :image-size="60" />
        </div>
      </div>
    </div>

    <div class="grid-3" style="margin-top: 16px">
      <div class="card card-pad">
        <div class="card-head">
          <div><h3>热销商品</h3><p>近 30 天销量 Top 8</p></div>
        </div>
        <div class="product-list">
          <div v-for="item in data.topProducts || []" :key="item.sku_name" class="product-item">
            <div class="product-art"><el-icon><Goods /></el-icon></div>
            <div class="product-main">
              <strong>{{ item.sku_name }}</strong>
              <span>销量 {{ item.qty }}</span>
            </div>
            <span class="money">¥{{ format(item.amount) }}</span>
          </div>
          <el-empty v-if="!(data.topProducts || []).length" description="暂无销售数据" :image-size="56" />
        </div>
      </div>

      <div class="card card-pad">
        <div class="card-head">
          <div><h3>最新订单</h3><p>实时订单动态</p></div>
          <el-button text type="primary" size="small" @click="router.push('/sales/orders')">全部</el-button>
        </div>
        <div class="order-list">
          <div v-for="order in data.recentOrders || []" :key="order.order_no" class="order-item">
            <div>
              <strong>{{ order.order_no }}</strong>
              <span>{{ formatTime(order.created_at) }} · {{ order.channel === 'POS' ? '收银' : '线上' }}</span>
            </div>
            <div class="order-right">
              <span class="money">¥{{ format(order.paid_amount) }}</span>
              <StatusTag :status="order.status" />
            </div>
          </div>
          <el-empty v-if="!(data.recentOrders || []).length" description="暂无订单" :image-size="56" />
        </div>
      </div>

      <div class="card card-pad">
        <div class="card-head">
          <div><h3>待办与公告</h3><p>门店执行提醒</p></div>
        </div>
        <div class="quick-grid">
          <button @click="router.push('/purchase/supply')">
            <el-icon><Van /></el-icon><span>供货单</span><em>{{ data.lowStockCount || 0 }} 项需补货</em>
          </button>
          <button @click="router.push('/inventory/stocks')">
            <el-icon><Box /></el-icon><span>临期处理</span><em>{{ data.nearExpiryCount || 0 }} 批待处理</em>
          </button>
          <button @click="router.push('/members/list')">
            <el-icon><User /></el-icon><span>会员运营</span><em>{{ data.memberTotal || 0 }} 位会员</em>
          </button>
          <button @click="router.push('/reports')">
            <el-icon><DataLine /></el-icon><span>经营报表</span><em>查看毛利与周转</em>
          </button>
        </div>
        <div class="notice-list">
          <div v-for="notice in notices" :key="notice.id" class="notice-item">
            <span class="dot"></span>
            <div>
              <strong>{{ notice.title }}</strong>
              <span>{{ notice.content }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, ShoppingCart } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useChart, chartPalette } from '@/composables/useChart'
import dayjs from 'dayjs'

const auth = useAuthStore()
const router = useRouter()
const tenantName = computed(() => auth.tenantName || '当前租户')
const data = ref<any>({})
const stores = ref<any[]>([])
const storeId = ref<number>()
const notices = ref<any[]>([])
const trendRef = ref<HTMLElement>()

const salesTrend = computed(() => {
  const today = Number(data.value.todaySales || 0)
  const yesterday = Number(data.value.yesterdaySales || 0)
  if (!yesterday) return today > 0 ? 100 : 0
  return Math.round(((today - yesterday) / yesterday) * 100)
})

const option = computed(() => {
  const trend = data.value.trend || []
  return {
    color: chartPalette,
    grid: { left: 8, right: 16, top: 40, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map((t: any) => dayjs(t.day).format('MM-DD')),
      axisLine: { lineStyle: { color: '#e4e7ec' } },
      axisLabel: { color: '#98a2b3' }
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { color: '#f2f4f7' } },
        axisLabel: { color: '#98a2b3' }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trend.map((t: any) => Number(t.sales || 0)),
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
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
        barWidth: 12,
        data: trend.map((t: any) => Number(t.orders || 0)),
        itemStyle: { color: 'rgba(20,184,166,0.65)', borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

useChart(trendRef, option)

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
  try {
    data.value = await api.get('/tenant/reports/dashboard', { storeId: storeId.value })
  } catch {
    data.value = {}
  }
  try {
    notices.value = await api.get('/tenant/notices')
  } catch {
    notices.value = []
  }
}

function format(value: any) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(value: any) {
  return value ? dayjs(value).format('MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;

  h3 { margin: 0; font-size: 15px; font-weight: 650; }
  p { margin: 4px 0 0; font-size: 11.5px; color: var(--ink-3); }
}

.chart { width: 100%; }
.chart-lg { height: 300px; }

.rank-list, .product-list, .order-list { margin-top: 8px; }

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px dashed #eef1f5;
  &:last-child { border-bottom: none; }
}

.rank-no {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 600;
  background: #f2f4f7;
  color: #667085;
  &.top { background: linear-gradient(135deg, #1f6feb, #14b8a6); color: #fff; }
}

.rank-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  strong { font-size: 13px; font-weight: 550; }
  span { font-size: 11px; color: var(--ink-3); }
}

.rank-val { font-size: 13px; }

.product-item, .order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed #eef1f5;
  &:last-child { border-bottom: none; }
}

.product-art {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #f2f6fd;
  color: #1f6feb;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.product-main, .order-item > div:first-child {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  strong { font-size: 12.5px; font-weight: 550; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  span { font-size: 11px; color: var(--ink-3); }
}

.order-item { justify-content: space-between; }
.order-right { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin: 10px 0 14px;

  button {
    border: 1px solid var(--line);
    background: #fbfcfe;
    border-radius: 9px;
    padding: 12px;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    cursor: pointer;
    text-align: left;
    transition: all 0.15s;

    &:hover { border-color: #b9d3fa; background: #f5f9ff; }
    .el-icon { color: #1f6feb; font-size: 18px; }
    span { font-size: 12px; font-weight: 600; color: #344054; }
    em { font-style: normal; font-size: 10.5px; color: #98a2b3; }
  }
}

.notice-list { border-top: 1px solid #f2f4f7; padding-top: 6px; }

.notice-item {
  display: flex;
  gap: 8px;
  padding: 7px 0;
  align-items: flex-start;
  .dot { width: 6px; height: 6px; border-radius: 50%; background: #1f6feb; margin-top: 6px; flex-shrink: 0; }
  div { display: flex; flex-direction: column; gap: 2px; }
  strong { font-size: 12px; font-weight: 550; }
  span { font-size: 11px; color: var(--ink-3); line-height: 1.5; }
}
</style>
