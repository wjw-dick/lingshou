// 通用展示格式化：金额、日期、状态
// 后端金额字段为 DECIMAL(x,2)，单位：元，展示统一保留两位小数。
function money(value) {
  const num = Number(value)
  if (!isFinite(num)) {
    return '0.00'
  }
  return num.toFixed(2)
}

function fenToYuan(value) {
  const num = Number(value)
  if (!isFinite(num)) {
    return '0.00'
  }
  return (num / 100).toFixed(2)
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  const text = String(value).replace('T', ' ')
  return text.length > 16 ? text.slice(0, 16) : text
}

function formatDay(value) {
  if (!value) {
    return '-'
  }
  return String(value).slice(0, 10)
}

const STATUS_TEXT = {
  PENDING: '待处理',
  ACCEPTED: '已接单',
  SHIPPED: '已发货',
  PARTIAL: '部分收货',
  RECEIVED: '已收货',
  CANCELLED: '已取消',
  CONFIRMED: '已确认',
  DISPUTED: '有异议',
  PAID: '已付款',
  SETTLED: '已结算',
  PENDING_SETTLE: '待结算',
  ACTIVE: '有效',
  REJECTED: '已驳回',
  APPROVED: '已审核'
}

function statusText(status) {
  if (!status) {
    return '-'
  }
  return STATUS_TEXT[status] || status
}

module.exports = {
  money,
  fenToYuan,
  formatDate,
  formatDay,
  statusText
}
