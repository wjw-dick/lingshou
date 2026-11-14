<template>
  <div class="page">
    <PageHeader title="公众号数据统计" subtitle="按日查看新增用户、累计用户、消息量与接口调用趋势。">
      <template #actions>
        <el-select v-if="accounts.length" v-model="accountId" placeholder="选择公众号" style="width: 200px" @change="handleAccountChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-radio-group v-model="days" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="15">近 15 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="!loading && !accounts.length"
      title="尚未配置公众号账号"
      description="统计数据来自公众号账号，请先完成账号配置后查看趋势。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div v-loading="loading">
      <div class="kpi-grid">
        <StatCard label="新增用户" :value="summary.newUsers" unit="人" icon="User" hint="所选周期内累计关注" color="#1f6feb" />
        <StatCard label="累计用户" :value="summary.totalUsers" unit="人" icon="UserFilled" hint="周期末总粉丝数" color="#14b8a6" />
        <StatCard label="消息总量" :value="summary.msgCount" unit="条" icon="ChatDotRound" hint="收发消息合计" color="#f0a020" />
        <StatCard label="接口调用" :value="summary.apiCount" unit="次" icon="Connection" hint="微信接口调用量" color="#7c3aed" />
      </div>

      <div class="chart-grid">
        <div class="card card-pad chart-card">
          <div class="chart-head">
            <div>
              <h3>用户增减</h3>
              <p>每日新增关注用户数</p>
            </div>
            <span class="chart-total">合计 {{ formatNumber(summary.newUsers) }} 人</span>
          </div>
          <div class="chart-wrap">
            <div ref="newUsersRef" class="chart"></div>
            <div v-if="!hasData" class="chart-empty">
              <el-empty description="暂无新增用户数据" :image-size="56" />
            </div>
          </div>
        </div>

        <div class="card card-pad chart-card">
          <div class="chart-head">
            <div>
              <h3>累计用户</h3>
              <p>每日末累计粉丝数</p>
            </div>
            <span class="chart-total">期末 {{ formatNumber(summary.totalUsers) }} 人</span>
          </div>
          <div class="chart-wrap">
            <div ref="totalUsersRef" class="chart"></div>
            <div v-if="!hasData" class="chart-empty">
              <el-empty description="暂无累计用户数据" :image-size="56" />
            </div>
          </div>
        </div>

        <div class="card card-pad chart-card">
          <div class="chart-head">
            <div>
              <h3>消息概况</h3>
              <p>每日收发消息条数</p>
            </div>
            <span class="chart-total">合计 {{ formatNumber(summary.msgCount) }} 条</span>
          </div>
          <div class="chart-wrap">
            <div ref="msgCountRef" class="chart"></div>
            <div v-if="!hasData" class="chart-empty">
              <el-empty description="暂无消息数据" :image-size="56" />
            </div>
          </div>
        </div>

        <div class="card card-pad chart-card">
          <div class="chart-head">
            <div>
              <h3>接口分析</h3>
              <p>每日微信接口调用次数</p>
            </div>
            <span class="chart-total">合计 {{ formatNumber(summary.apiCount) }} 次</span>
          </div>
          <div class="chart-wrap">
            <div ref="apiCountRef" class="chart"></div>
            <div v-if="!hasData" class="chart-empty">
              <el-empty description="暂无接口调用数据" :image-size="56" />
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { api } from '@/api/http'
import { useChart, chartPalette } from '@/composables/useChart'

const route = useRoute()
const router = useRouter()
const accounts = ref<any[]>([])
const accountId = ref<number>()
const days = ref(7)
const loading = ref(false)
const stats = ref<any[]>([])
const summary = ref<any>({ newUsers: 0, totalUsers: 0, msgCount: 0, apiCount: 0 })

const newUsersRef = ref<HTMLElement>()
const totalUsersRef = ref<HTMLElement>()
const msgCountRef = ref<HTMLElement>()
const apiCountRef = ref<HTMLElement>()

const hasData = computed(() => stats.value.length > 0)

const axisX = computed(() => stats.value.map((item) => dayjs(item.statDate).format('MM-DD')))

const newUsersOption = computed(() => baseOption(axisX.value, [
  {
    name: '新增用户',
    type: 'bar',
    barWidth: stats.value.length > 20 ? 8 : 14,
    data: stats.value.map((item) => Number(item.newUsers || 0)),
    itemStyle: { color: 'rgba(31,111,235,0.78)', borderRadius: [4, 4, 0, 0] }
  }
]))

const totalUsersOption = computed(() => baseOption(axisX.value, [
  {
    name: '累计用户',
    type: 'line',
    smooth: true,
    symbolSize: 6,
    data: stats.value.map((item) => Number(item.totalUsers || 0)),
    lineStyle: { width: 2.5 },
    itemStyle: { color: chartPalette[1] },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(20,184,166,0.24)' },
          { offset: 1, color: 'rgba(20,184,166,0.01)' }
        ]
      }
    }
  }
]))

const msgCountOption = computed(() => baseOption(axisX.value, [
  {
    name: '消息量',
    type: 'bar',
    barWidth: stats.value.length > 20 ? 8 : 14,
    data: stats.value.map((item) => Number(item.msgCount || 0)),
    itemStyle: { color: 'rgba(240,160,32,0.82)', borderRadius: [4, 4, 0, 0] }
  }
]))

const apiCountOption = computed(() => baseOption(axisX.value, [
  {
    name: '接口调用',
    type: 'line',
    smooth: true,
    symbolSize: 6,
    data: stats.value.map((item) => Number(item.apiCount || 0)),
    lineStyle: { width: 2.5 },
    itemStyle: { color: chartPalette[3] },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(124,58,237,0.22)' },
          { offset: 1, color: 'rgba(124,58,237,0.01)' }
        ]
      }
    }
  }
]))

useChart(newUsersRef, newUsersOption)
useChart(totalUsersRef, totalUsersOption)
useChart(msgCountRef, msgCountOption)
useChart(apiCountRef, apiCountOption)

onMounted(async () => {
  await loadAccounts()
  await load()
})

function baseOption(x: string[], series: any[]) {
  return {
    color: chartPalette,
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: series[0]?.type === 'bar',
      data: x,
      axisLine: { lineStyle: { color: '#e4e7ec' } },
      axisLabel: { color: '#98a2b3', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f4f7' } },
      axisLabel: { color: '#98a2b3', fontSize: 11 }
    },
    series
  }
}

async function loadAccounts() {
  try {
    accounts.value = await api.get('/tenant/mp/accounts')
    const queryId = Number(route.query.accountId)
    accountId.value = accounts.value.some((item) => item.id === queryId)
      ? queryId
      : accounts.value[0]?.id
  } catch {
    accounts.value = []
  }
}

function handleAccountChange(id: number) {
  router.replace({ query: { ...route.query, accountId: String(id) } })
  load()
}

async function load() {
  if (!accountId.value) {
    stats.value = []
    return
  }
  loading.value = true
  try {
    const result = await api.get('/tenant/mp/statistics', { accountId: accountId.value, days: days.value })
    stats.value = Array.isArray(result.stats) ? result.stats : []
    summary.value = {
      newUsers: result.newUsers || 0,
      totalUsers: result.totalUsers || 0,
      msgCount: result.msgCount || 0,
      apiCount: result.apiCount || 0
    }
  } catch {
    stats.value = []
    summary.value = { newUsers: 0, totalUsers: 0, msgCount: 0, apiCount: 0 }
  } finally {
    loading.value = false
  }
}

function formatNumber(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  min-height: 300px;

  .chart { width: 100%; height: 240px; }
}

.chart-wrap {
  position: relative;
  min-height: 240px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: #fff;
}

.chart-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;

  h3 { margin: 0; font-size: 15px; font-weight: 650; }
  p { margin: 4px 0 0; font-size: 11.5px; color: var(--ink-3); }
}

.chart-total {
  font-size: 12px;
  color: var(--ink-2);
  background: #f5f7fb;
  border-radius: 999px;
  padding: 3px 10px;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .chart-grid { grid-template-columns: 1fr; }
}
</style>
