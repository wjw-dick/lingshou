<template>
  <div class="status-tag" :class="`tone-${tone}`">{{ text }}</div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status?: string; text?: string }>()

const map: Record<string, { text: string; tone: string }> = {
  ACTIVE: { text: '正常', tone: 'green' },
  TRIAL: { text: '试用中', tone: 'blue' },
  OVERDUE: { text: '欠费', tone: 'orange' },
  SUSPENDED: { text: '已停用', tone: 'red' },
  EXPIRED: { text: '已到期', tone: 'red' },
  OPEN: { text: '营业中', tone: 'green' },
  PAUSED: { text: '暂停营业', tone: 'orange' },
  RENOVATING: { text: '装修中', tone: 'gray' },
  ON_SALE: { text: '在售', tone: 'green' },
  OFF_SHELF: { text: '已下架', tone: 'gray' },
  DRAFT: { text: '草稿', tone: 'gray' },
  SUBMITTED: { text: '待审核', tone: 'orange' },
  APPROVED: { text: '已审核', tone: 'blue' },
  RECEIVING: { text: '收货中', tone: 'blue' },
  DONE: { text: '已完成', tone: 'green' },
  CANCELLED: { text: '已取消', tone: 'gray' },
  PENDING: { text: '待处理', tone: 'orange' },
  ACCEPTED: { text: '已接单', tone: 'blue' },
  SHIPPED: { text: '已发货', tone: 'blue' },
  PARTIAL: { text: '部分收货', tone: 'orange' },
  RECEIVED: { text: '已收货', tone: 'green' },
  CONFIRMED: { text: '已确认', tone: 'green' },
  COMPLETED: { text: '已完成', tone: 'green' },
  REJECTED: { text: '已驳回', tone: 'red' },
  PAID: { text: '已支付', tone: 'green' },
  UNPAID: { text: '待支付', tone: 'orange' },
  REFUNDED: { text: '已退款', tone: 'purple' },
  PARTIAL_REFUND: { text: '部分退款', tone: 'purple' },
  PENDING_PAY: { text: '待支付', tone: 'orange' },
  PENDING_ACCEPT: { text: '待接单', tone: 'orange' },
  PICKING: { text: '拣货中', tone: 'blue' },
  WAIT_PICKUP: { text: '待自提', tone: 'blue' },
  DELIVERING: { text: '配送中', tone: 'blue' },
  UNUSED: { text: '未使用', tone: 'green' },
  USED: { text: '已使用', tone: 'gray' },
  DISABLED: { text: '已停用', tone: 'gray' },
  LOCAL: { text: '本地', tone: 'gray' },
  SYNCED: { text: '已同步', tone: 'green' },
  PUBLISHING: { text: '发表中', tone: 'blue' },
  SUCCESS: { text: '成功', tone: 'green' },
  FAILED: { text: '失败', tone: 'red' },
  PUBLISHED: { text: '已发表', tone: 'green' },
  DISPUTED: { text: '有异议', tone: 'red' },
  SETTLED: { text: '已结算', tone: 'green' },
  VALID: { text: '有效', tone: 'green' },
  CLOSED: { text: '已关闭', tone: 'gray' },
  OCCUPIED: { text: '已被接管', tone: 'red' },
  FREE: { text: '未被占用', tone: 'green' }
}

const text = computed(() => props.text || map[props.status || '']?.text || props.status || '-')
const tone = computed(() => map[props.status || '']?.tone || 'gray')
</script>

<style scoped lang="scss">
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 550;
  white-space: nowrap;

  &.tone-green { background: #e8f7ef; color: #0e7a45; }
  &.tone-blue { background: #eaf2ff; color: #1f6feb; }
  &.tone-orange { background: #fef3e2; color: #b25e09; }
  &.tone-red { background: #fdecef; color: #c0264a; }
  &.tone-purple { background: #f2ecfe; color: #6d28d9; }
  &.tone-gray { background: #f2f4f7; color: #667085; }
}
</style>
