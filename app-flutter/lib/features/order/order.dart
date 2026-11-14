import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/format.dart';
import '../../core/http_client.dart';
import '../../core/theme.dart';
import '../../core/widgets.dart';
import '../../data/api.dart';
import '../../data/models.dart';
import '../auth/auth.dart';

class OrderListState {
  const OrderListState({
    this.orders = const [],
    this.loading = false,
    this.loadingMore = false,
    this.error,
    this.page = 1,
    this.total = 0,
    this.status = '',
    this.loaded = false,
  });

  final List<OrderSummaryRow> orders;
  final bool loading;
  final bool loadingMore;
  final String? error;
  final int page;
  final int total;
  final String status;
  final bool loaded;
}

class OrderListController extends StateNotifier<OrderListState> {
  OrderListController(this._api) : super(const OrderListState());

  final AppApi _api;

  Future<void> load({bool reset = true, String? status}) async {
    if (!mounted) {
      return;
    }
    final nextStatus = status ?? state.status;
    if (!reset &&
        (state.loading ||
            state.loadingMore ||
            (state.loaded && state.orders.length >= state.total))) {
      return;
    }
    final page = reset ? 1 : state.page + 1;
    final previousOrders = reset ? const <OrderSummaryRow>[] : state.orders;
    state = OrderListState(
      orders: previousOrders,
      loading: reset,
      loadingMore: !reset,
      page: page,
      total: state.total,
      status: nextStatus,
      loaded: state.loaded,
    );
    try {
      final result = await _api.orders(
        page: page,
        size: 10,
        status: nextStatus,
      );
      if (!mounted) {
        return;
      }
      state = OrderListState(
        orders: reset ? result.records : [...state.orders, ...result.records],
        page: page,
        total: result.total,
        status: nextStatus,
        loaded: true,
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      state = OrderListState(
        orders: state.orders,
        error: error.toString(),
        page: state.page,
        total: state.total,
        status: nextStatus,
        loaded: state.loaded,
      );
    }
  }

  Future<void> refresh() => load(reset: true);

  Future<void> loadMore() => load(reset: false);

  Future<void> changeStatus(String status) => load(reset: true, status: status);
}

final orderListProvider =
    StateNotifierProvider<OrderListController, OrderListState>((ref) {
  final token = ref.watch(authProvider.select((state) => state.session?.token));
  final controller = OrderListController(ref.watch(appApiProvider));
  if (token != null) {
    Future.microtask(() => controller.load());
  }
  return controller;
});

class OrderListPage extends ConsumerWidget {
  const OrderListPage({super.key});

  static const List<({String label, String value})> _tabs = [
    (label: '全部', value: ''),
    (label: '待支付', value: 'PENDING_PAY'),
    (label: '待接单', value: 'PENDING_ACCEPT'),
    (label: '待自提', value: 'WAIT_PICKUP'),
    (label: '已完成', value: 'COMPLETED'),
  ];

  Future<void> _openDetail(BuildContext context, WidgetRef ref, int orderId) async {
    await context.push('/orders/$orderId');
    if (context.mounted) {
      await ref.read(orderListProvider.notifier).refresh();
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);
    if (!auth.isLoggedIn) {
      return Scaffold(
        appBar: AppBar(title: const Text('我的订单')),
        body: LoginRequiredView(
          onLogin: () => context.push('/login'),
          message: '登录后可查看订单状态与历史订单',
        ),
      );
    }
    final state = ref.watch(orderListProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('我的订单')),
      body: Column(
        children: [
          SizedBox(
            height: 52,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              children: _tabs
                  .map(
                    (tab) => Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(tab.label),
                        selected: state.status == tab.value,
                        showCheckmark: false,
                        selectedColor: AppTheme.primary.withOpacity(0.12),
                        labelStyle: TextStyle(
                          fontSize: 13,
                          color: state.status == tab.value
                              ? AppTheme.primary
                              : AppTheme.textSecondary,
                          fontWeight: state.status == tab.value
                              ? FontWeight.w600
                              : FontWeight.w400,
                        ),
                        side: BorderSide(
                          color: state.status == tab.value
                              ? AppTheme.primary.withOpacity(0.3)
                              : Colors.transparent,
                        ),
                        onSelected: (_) => ref
                            .read(orderListProvider.notifier)
                            .changeStatus(tab.value),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
          if (state.loading && state.orders.isNotEmpty)
            const LinearProgressIndicator(minHeight: 2),
          Expanded(
            child: state.loading && state.orders.isEmpty
                ? const LoadingView()
                : state.error != null && state.orders.isEmpty
                    ? ErrorView(
                        message: state.error!,
                        onRetry: () =>
                            ref.read(orderListProvider.notifier).refresh(),
                      )
                    : state.orders.isEmpty
                        ? EmptyView(
                            text: '暂无订单，去首页逛逛吧',
                            icon: Icons.receipt_long_outlined,
                            actionText: '去点单',
                            onAction: () => context.push('/products'),
                          )
                        : RefreshIndicator(
                            onRefresh: () =>
                                ref.read(orderListProvider.notifier).refresh(),
                            child: ListView.separated(
                              padding:
                                  const EdgeInsets.fromLTRB(16, 8, 16, 24),
                              itemCount: state.orders.length + 1,
                              separatorBuilder: (_, __) =>
                                  const SizedBox(height: 10),
                              itemBuilder: (context, index) {
                                if (index == state.orders.length) {
                                  return _buildFooter(context, ref, state);
                                }
                                final row = state.orders[index];
                                return _OrderCard(
                                  row: row,
                                  onTap: () => _openDetail(
                                    context,
                                    ref,
                                    row.order.id,
                                  ),
                                );
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildFooter(
    BuildContext context,
    WidgetRef ref,
    OrderListState state,
  ) {
    if (state.orders.length >= state.total) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(
          child: Text(
            '没有更多订单了',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Center(
        child: state.loadingMore
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : OutlinedButton(
                onPressed: () =>
                    ref.read(orderListProvider.notifier).loadMore(),
                child: const Text('加载更多'),
              ),
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  const _OrderCard({required this.row, required this.onTap});

  final OrderSummaryRow row;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final order = row.order;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      order.orderNo.isEmpty ? '订单 ${order.id}' : order.orderNo,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ),
                  OrderStatusChip(status: order.status),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: AppTheme.primary.withOpacity(0.08),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(
                      Icons.shopping_bag_outlined,
                      color: AppTheme.primary,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          row.summary,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '共 ${row.totalQty} 件 · ${deliveryTypeText(order.deliveryType)}',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      formatDateTime(order.createdAt),
                      style: const TextStyle(
                        fontSize: 11,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ),
                  Text(
                    '实付 ',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  PriceText(
                    value: order.paidAmount > 0
                        ? order.paidAmount
                        : order.payableAmount,
                    size: 17,
                  ),
                  const SizedBox(width: 8),
                  const Icon(
                    Icons.chevron_right,
                    size: 18,
                    color: AppTheme.textSecondary,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class OrderDetailPage extends ConsumerStatefulWidget {
  const OrderDetailPage({super.key, required this.orderId});

  final int orderId;

  @override
  ConsumerState<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends ConsumerState<OrderDetailPage> {
  OrderDetail? _detail;
  String? _storeName;
  String? _error;
  bool _loading = true;
  bool _acting = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    setState(() {
      _loading = _detail == null;
      _error = null;
    });
    try {
      final api = ref.read(appApiProvider);
      final detail = await api.orderDetail(widget.orderId);
      String? storeName;
      try {
        final stores = await api.stores();
        for (final store in stores) {
          if (store.id == detail.order.storeId) {
            storeName = store.name;
            break;
          }
        }
      } catch (_) {
        storeName = null;
      }
      if (!mounted) {
        return;
      }
      setState(() {
        _detail = detail;
        _storeName = storeName;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _loading = false;
        _error = error.toString();
      });
    }
  }

  Future<void> _pay() async {
    if (_acting) {
      return;
    }
    final method = await showModalBottomSheet<String>(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 14),
              child: Text(
                '选择支付方式',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
            ),
            ListTile(
              leading: const Icon(
                Icons.chat_bubble_outline,
                color: Color(0xFF07C160),
              ),
              title: const Text('微信支付'),
              onTap: () => Navigator.of(sheetContext).pop('WECHAT'),
            ),
            ListTile(
              leading: const Icon(Icons.account_balance_wallet_outlined),
              title: const Text('余额支付'),
              onTap: () => Navigator.of(sheetContext).pop('WALLET'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (method == null || !mounted) {
      return;
    }
    setState(() => _acting = true);
    try {
      await ref.read(appApiProvider).payOrder(widget.orderId, method);
      if (mounted) {
        showMessage(context, '支付成功');
      }
      await _load();
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
    } finally {
      if (mounted) {
        setState(() => _acting = false);
      }
    }
  }

  Future<void> _cancel() async {
    if (_acting) {
      return;
    }
    final confirmed = await showConfirmDialog(
      context,
      title: '取消订单',
      content: '确定取消该订单吗？',
      confirmText: '取消订单',
    );
    if (!confirmed || !mounted) {
      return;
    }
    setState(() => _acting = true);
    try {
      await ref.read(appApiProvider).cancelOrder(widget.orderId);
      if (mounted) {
        showMessage(context, '订单已取消');
      }
      await _load();
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
    } finally {
      if (mounted) {
        setState(() => _acting = false);
      }
    }
  }

  Future<void> _evaluate() async {
    if (_acting) {
      return;
    }
    final result = await showModalBottomSheet<_EvaluationResult>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const _EvaluationSheet(),
    );
    if (result == null || !mounted) {
      return;
    }
    setState(() => _acting = true);
    try {
      await ref.read(appApiProvider).evaluateOrder(
            widget.orderId,
            score: result.score,
            content: result.content,
          );
      if (mounted) {
        showMessage(context, '评价成功');
      }
      await _load();
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
    } finally {
      if (mounted) {
        setState(() => _acting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    return Scaffold(
      appBar: AppBar(title: const Text('订单详情')),
      body: StateView(
        loading: _loading,
        error: _error,
        onRetry: _load,
        child: detail == null ? const EmptyView(text: '订单不存在') : _buildBody(detail),
      ),
      bottomNavigationBar:
          detail == null ? null : _buildBottomBar(detail),
    );
  }

  Widget _buildBody(OrderDetail detail) {
    final order = detail.order;
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      children: [
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF12A15A), Color(0xFF3DBB78)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                orderStatusText(order.status),
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                order.status == 'PENDING_PAY' ? '请尽快完成支付' : '感谢您的惠顾',
                style: TextStyle(
                  color: Colors.white.withOpacity(0.88),
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                '订单号 ${order.orderNo.isEmpty ? order.id : order.orderNo}',
                style: TextStyle(
                  color: Colors.white.withOpacity(0.82),
                  fontSize: 12,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                '下单时间 ${formatDateTime(order.createdAt)}',
                style: TextStyle(
                  color: Colors.white.withOpacity(0.82),
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),
        _DetailSection(
          children: [
            Row(
              children: [
                const Icon(
                  Icons.storefront_outlined,
                  size: 18,
                  color: AppTheme.primary,
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    _storeName ?? '门店 #${order.storeId}',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
                Text(
                  deliveryTypeText(order.deliveryType),
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                ),
              ],
            ),
            if (order.deliveryType == 'DELIVERY') ...[
              const Divider(height: 20),
              _LabelValue(
                label: '收货人',
                value: '${order.deliveryName} ${order.deliveryPhone}'.trim(),
              ),
              _LabelValue(label: '收货地址', value: order.deliveryAddress),
            ],
            if (order.remark.isNotEmpty) ...[
              const Divider(height: 20),
              _LabelValue(label: '订单备注', value: order.remark),
            ],
          ],
        ),
        const SizedBox(height: 10),
        _DetailSection(
          children: [
            const Text(
              '商品明细',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 6),
            ...detail.items.map(
              (item) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 7),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.skuName.isEmpty ? '商品' : item.skuName,
                            style: const TextStyle(fontSize: 14),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '¥${money(item.price)} × ${item.qty}',
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      '¥${money(item.amount)}',
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        _DetailSection(
          children: [
            const Text(
              '金额明细',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            _AmountRow(label: '商品金额', value: order.totalAmount),
            if (order.discountAmount > 0)
              _AmountRow(label: '优惠券抵扣', value: -order.discountAmount),
            if (order.pointsDiscount > 0)
              _AmountRow(
                label: '积分抵扣（${order.pointsUsed} 积分）',
                value: -order.pointsDiscount,
              ),
            if (order.freightAmount > 0)
              _AmountRow(label: '配送费', value: order.freightAmount),
            const Divider(height: 20),
            Row(
              children: [
                const Expanded(
                  child: Text(
                    '应付金额',
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
                PriceText(value: order.payableAmount, size: 20),
              ],
            ),
            if (order.paidAmount > 0) ...[
              const SizedBox(height: 4),
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      '实付金额',
                      style: TextStyle(
                        fontSize: 12,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ),
                  Text(
                    '¥${money(order.paidAmount)}',
                    style: const TextStyle(fontSize: 12),
                  ),
                ],
              ),
            ],
          ],
        ),
        if (detail.evaluation != null) ...[
          const SizedBox(height: 10),
          _DetailSection(
            children: [
              const Text(
                '我的评价',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              Row(
                children: List.generate(
                  5,
                  (index) => Icon(
                    index < detail.evaluation!.score
                        ? Icons.star
                        : Icons.star_border,
                    size: 20,
                    color: AppTheme.warning,
                  ),
                ),
              ),
              const SizedBox(height: 6),
              Text(
                detail.evaluation!.content.isEmpty
                    ? '默认好评'
                    : detail.evaluation!.content,
                style: const TextStyle(fontSize: 13, height: 1.5),
              ),
            ],
          ),
        ],
      ],
    );
  }

  Widget _buildBottomBar(OrderDetail detail) {
    final order = detail.order;
    final canEvaluate = order.canEvaluate && detail.evaluation == null;
    if (!order.canPay && !canEvaluate) {
      return const SizedBox.shrink();
    }
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: Color(0xFFE8EEEA))),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            if (order.canPay)
              OutlinedButton(
                onPressed: _acting ? null : _cancel,
                child: const Text('取消订单'),
              ),
            if (order.canPay) const SizedBox(width: 10),
            if (order.canPay)
              FilledButton(
                onPressed: _acting ? null : _pay,
                child: _acting
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text('去支付'),
              ),
            if (!order.canPay && canEvaluate)
              FilledButton(
                onPressed: _acting ? null : _evaluate,
                child: const Text('评价订单'),
              ),
          ],
        ),
      ),
    );
  }
}

class _EvaluationResult {
  const _EvaluationResult({required this.score, required this.content});

  final int score;
  final String content;
}

class _EvaluationSheet extends StatefulWidget {
  const _EvaluationSheet();

  @override
  State<_EvaluationSheet> createState() => _EvaluationSheetState();
}

class _EvaluationSheetState extends State<_EvaluationSheet> {
  final TextEditingController _controller = TextEditingController();
  int _score = 5;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        18,
        20,
        20 + MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '评价订单',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 12),
          Row(
            children: List.generate(
              5,
              (index) => IconButton(
                onPressed: () => setState(() => _score = index + 1),
                icon: Icon(
                  index < _score ? Icons.star : Icons.star_border,
                  color: AppTheme.warning,
                  size: 30,
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _controller,
            maxLines: 3,
            maxLength: 200,
            decoration: const InputDecoration(
              hintText: '说说这次的购物体验吧（选填）',
            ),
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () {
                Navigator.of(context).pop(
                  _EvaluationResult(
                    score: _score,
                    content: _controller.text.trim(),
                  ),
                );
              },
              child: const Text('提交评价'),
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailSection extends StatelessWidget {
  const _DetailSection({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: children,
      ),
    );
  }
}

class _LabelValue extends StatelessWidget {
  const _LabelValue({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 66,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 13,
                color: AppTheme.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value.isEmpty ? '-' : value,
              style: const TextStyle(fontSize: 13, height: 1.4),
            ),
          ),
        ],
      ),
    );
  }
}

class _AmountRow extends StatelessWidget {
  const _AmountRow({required this.label, required this.value});

  final String label;
  final double value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 13,
                color: AppTheme.textSecondary,
              ),
            ),
          ),
          Text(
            value < 0 ? '-¥${money(-value)}' : '¥${money(value)}',
            style: const TextStyle(fontSize: 13),
          ),
        ],
      ),
    );
  }
}
