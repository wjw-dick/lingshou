<template>
  <div class="stat-card">
    <div class="stat-top">
      <span>{{ label }}</span>
      <div class="stat-icon" :style="{ background: softColor, color }">
        <el-icon :size="18"><component :is="icon || 'DataLine'" /></el-icon>
      </div>
    </div>
    <div class="stat-value">
      <span v-if="prefix" class="prefix">{{ prefix }}</span>{{ formatted }}<em v-if="unit">{{ unit }}</em>
    </div>
    <div class="stat-foot">
      <span v-if="trend !== undefined" :class="['trend', trend >= 0 ? 'up' : 'down']">
        <el-icon><CaretTop v-if="trend >= 0" /><CaretBottom v-else /></el-icon>{{ Math.abs(trend) }}%
      </span>
      <span class="hint">{{ hint }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  value: number | string
  unit?: string
  prefix?: string
  hint?: string
  trend?: number
  icon?: string
  color?: string
}>()

const color = computed(() => props.color || '#1f6feb')
const softColor = computed(() => {
  const map: Record<string, string> = {
    '#1f6feb': '#eaf2ff',
    '#18a058': '#e7f7ef',
    '#f0a020': '#fef5e5',
    '#d03050': '#fdecef',
    '#7c3aed': '#f1eafe',
    '#0d9488': '#e6f7f5'
  }
  return map[color.value] || '#eaf2ff'
})

const formatted = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
  }
  return props.value
})
</script>

<style scoped lang="scss">
.stat-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 18px;
  box-shadow: var(--shadow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(16, 24, 40, 0.09); }
}

.stat-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--ink-2);
  font-size: 12.5px;
}

.stat-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: grid;
  place-items: center;
}

.stat-value {
  margin: 12px 0 8px;
  font-size: 28px;
  font-weight: 680;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;

  .prefix { font-size: 16px; margin-right: 2px; font-weight: 500; }
  em { font-size: 12px; font-style: normal; color: var(--ink-3); margin-left: 4px; font-weight: 400; }
}

.stat-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  min-height: 18px;
}

.trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-weight: 600;
  &.up { color: #18a058; }
  &.down { color: #d03050; }
}

.hint { color: var(--ink-3); }
</style>
