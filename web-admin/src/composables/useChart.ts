import { onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'
import * as echarts from 'echarts'

export function useChart(el: Ref<HTMLElement | undefined>, option: Ref<any>) {
  let chart: echarts.ECharts | null = null
  let observer: ResizeObserver | null = null

  function render() {
    if (!el.value) return
    if (!chart) {
      chart = echarts.init(el.value)
    }
    chart.setOption(option.value, true)
  }

  onMounted(() => {
    render()
    if (el.value) {
      observer = new ResizeObserver(() => chart?.resize())
      observer.observe(el.value)
    }
  })

  watch(option, render, { deep: true })

  onBeforeUnmount(() => {
    observer?.disconnect()
    chart?.dispose()
    chart = null
  })

  return { render }
}

export const chartPalette = ['#1f6feb', '#14b8a6', '#f0a020', '#7c3aed', '#d03050', '#0ea5e9']
