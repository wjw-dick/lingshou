/// 通用展示格式化：金额、日期、订单状态。
String money(num? value) {
  final number = (value ?? 0).toDouble();
  if (!number.isFinite) {
    return '0.00';
  }
  return number.toStringAsFixed(2);
}

String priceText(num? value) => '¥${money(value)}';

String formatDateTime(String? value) {
  if (value == null || value.isEmpty) {
    return '-';
  }
  final text = value.replaceFirst('T', ' ');
  return text.length > 16 ? text.substring(0, 16) : text;
}

String formatDay(String? value) {
  if (value == null || value.isEmpty) {
    return '-';
  }
  return value.length > 10 ? value.substring(0, 10) : value;
}

String orderStatusText(String? status) {
  return switch (status) {
    'PENDING_PAY' => '待支付',
    'PENDING_ACCEPT' => '待接单',
    'PICKING' => '拣货中',
    'WAIT_PICKUP' => '待自提',
    'DELIVERING' => '配送中',
    'COMPLETED' => '已完成',
    'CANCELLED' => '已取消',
    'REFUNDING' => '退款中',
    'REFUNDED' => '已退款',
    _ => status == null || status.isEmpty ? '-' : status,
  };
}

String deliveryTypeText(String? type) {
  return type == 'DELIVERY' ? '外送到家' : '到店自提';
}

String couponThresholdText(num threshold) {
  return threshold > 0 ? '满 ${money(threshold)} 元可用' : '无门槛';
}

String couponValueText({
  required String type,
  required num discountAmount,
  required num discountRate,
}) {
  if (type == 'DISCOUNT') {
    final rate = discountRate.toDouble();
    final text = rate == rate.roundToDouble()
        ? rate.toStringAsFixed(0)
        : rate.toStringAsFixed(1);
    return '$text折';
  }
  return priceText(discountAmount);
}
