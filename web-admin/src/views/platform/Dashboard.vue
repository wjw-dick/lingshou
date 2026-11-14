<template>
  <div class="page">
    <PageHeader title="经营总览" subtitle="平台侧只看租户汇总与计费数据，不穿透租户业务明细。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/platform/tenants')">开通租户</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <StatCard label="租户总数" :value="data.tenantTotal || 0" unit="家" icon="OfficeBuilding" hint="含试用与正式" color="#1f6feb" />
      <StatCard label="活跃租户" :value="data.activeTotal || 0" unit="家" icon="CircleCheck" hint="正常 + 试用" color="#14b8a6" />
      <StatCard label="试用转化" :value="data.trialTotal || 0" unit="家" icon="MagicStick" hint="试用期租户" color="#7c3aed" />
      <StatCard label="30 天内到期" :value="data.expiringTotal || 0" unit="家" icon="AlarmClock" hint="需跟进续费" color="#f0a020" />
      <StatCard label="累计回款" prefix="¥" :value="data.revenue || 0" icon="Money" hint="已支付账单" color="#18a058" />
      <StatCard label="已停用" :value="data.suspendedTotal || 0" unit="家" icon="CircleClose" hint="停用或到期" color="#d03050" />
    </div>

    <div class="grid-2">
      <div class="card card-pad">
        <div class="card-head">
          <div><h3>租户状态分布</h3><p>按生命周期状态统计</p></div>
        </div>
        <div ref="pieRef" class="chart"></div>
      </div>
      <div class="card card-pad">
        <div class="card-head">
          <div><h3>最近开通</h3><p>最新进入平台的租户</p></div>
          <el-button text type="primary" size="small" @click="router.push('/platform/tenants')">全部租户</el-button>
        </div>
        <div class="tenant-list">
          <div v-for="tenant in data.recentTenants || []" :key="tenant.code" class="tenant-item">
            <div class="tenant-avatar">{{ tenant.name?.slice(0, 1) }}</div>
            <div class="tenant-main">
              <strong>{{ tenant.name }}</strong>
              <span>{{ tenant.code }} · {{ formatTime(tenant.createdAt) }}</span>
            </div>
            <StatusTag :status="tenant.status" />
          </div>
          <el-empty v-if="!(data.recentTenants || []).length" description="暂无租户" :image-size="60" />
        </div>
        <div class="quick-links">
          <button @click="router.push('/platform/plans')"><el-icon><Tickets /></el-icon><span>套餐与配额</span></button>
          <button @click="router.push('/platform/bills')"><el-icon><Money /></el-icon><span>账单与计费</span></button>
          <button @click="router.push('/platform/audits')"><el-icon><Document /></el-icon><span>平台审计</span></button>
        </div>
      </div>
    </div>

    <div class="card card-pad" style="margin-top: 16px">
      <div class="card-head">
        <div><h3>平台运营提示</h3><p>租户生命周期与配额风险</p></div>
      </div>
      <div class="tips-grid">
        <div class="tip" v-for="tip in tips" :key="tip.title">
          <div class="tip-icon" :style="{ color: tip.color, background: tip.bg }"><el-icon><component :is="tip.icon" /></el-icon></div>
          <div><strong>{{ tip.title }}</strong><span>{{ tip.desc }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import { useChart } from '@/composables/useChart'
import dayjs from 'dayjs'

const router = useRouter()
const data = ref<any>({})
const pieRef = ref<HTMLElement>()

const tips = computed(() => [
  { title: '试用到期跟进', desc: `${data.value.trialTotal || 0} 家租户处于试用期，建议提前 3 天触达`, icon: 'Bell', color: '#1f6feb', bg: '#eaf2ff' },
  { title: '续费风险', desc: `${data.value.expiringTotal || 0} 家租户 30 天内到期`, icon: 'AlarmClock', color: '#b25e09', bg: '#fef3e2' },
  { title: '配额使用', desc: '席位、门店、存储与 API 配额由服务端强校验', icon: 'DataLine', color: '#0d9488', bg: '#e6f7f5' },
  { title: '租户隔离', desc: '平台端不提供租户订单/会员/库存明细下钻', icon: 'Lock', color: '#6d28d9', bg: '#f2ecfe' }
])

const option = computed(() => {
  const dist = data.value.statusDistribution || {}
  const labelMap: Record<string, string> = {
    ACTIVE: '正常', TRIAL: '试用中', OVERDUE: '欠费', SUSPENDED: '已停用', EXPIRED: '已到期'
  }
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#667085' } },
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: Object.keys(dist).map((key) => ({
          name: labelMap[key] || key,
          value: dist[key]
        }))
      }
    ],
    color: ['#1f6feb', '#14b8a6', '#f0a020', '#d03050', '#98a2b3']
  }
})

useChart(pieRef, option)

onMounted(load)

async function load() {
  try {
    data.value = await api.get('/platform/dashboard')
  } catch {
    data.value = {}
  }
}

function formatTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}
</script>

<style scoped lang="scss">
.card-head {
  display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;
  h3 { margin: 0; font-size: 15px; font-weight: 650; }
  p { margin: 4px 0 0; font-size: 11.5px; color: var(--ink-3); }
}
.chart { height: 300px; width: 100%; }
.tenant-item {
  display: flex; align-items: center; gap: 12px; padding: 11px 0; border-bottom: 1px dashed #eef1f5;
  &:last-child { border-bottom: none; }
}
.tenant-avatar {
  width: 36px; height: 36px; border-radius: 10px; background: linear-gradient(135deg, #1f6feb, #14b8a6);
  color: #fff; display: grid; place-items: center; font-weight: 600;
}
.tenant-main { flex: 1; display: flex; flex-direction: column; gap: 3px;
  strong { font-size: 13px; }
  span { font-size: 11px; color: var(--ink-3); }
}
.quick-links {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 14px;
  button {
    border: 1px solid var(--line); background: #fbfcfe; border-radius: 9px; padding: 12px 8px;
    display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: pointer;
    color: #475467; font-size: 11.5px; transition: all 0.15s;
    &:hover { border-color: #b9d3fa; color: #1f6feb; background: #f5f9ff; }
    .el-icon { font-size: 18px; }
  }
}
.tips-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 12px; }
.tip {
  display: flex; gap: 12px; padding: 14px; border: 1px solid var(--line); border-radius: 9px; background: #fbfcfe;
  div:last-child { display: flex; flex-direction: column; gap: 4px; }
  strong { font-size: 12.5px; }
  span { font-size: 11px; color: var(--ink-3); line-height: 1.6; }
}
.tip-icon { width: 34px; height: 34px; border-radius: 9px; display: grid; place-items: center; flex-shrink: 0; }
</style>
