import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/format.dart';
import '../../core/http_client.dart';
import '../../core/storage.dart';
import '../../core/theme.dart';
import '../../core/widgets.dart';
import '../../data/api.dart';
import '../../data/models.dart';
import '../auth/auth.dart';
import '../store/store.dart';

class CartController extends StateNotifier<List<CartItem>> {
  CartController(this._storage) : super(_storage.cart);

  final StorageService _storage;

  void _save(List<CartItem> items) {
    state = List.unmodifiable(items);
    _storage.saveCart(state);
  }

  void add(ProductItem product, {int qty = 1}) {
    final count = qty < 1 ? 1 : qty;
    final next = [...state];
    final index = next.indexWhere((item) => item.skuId == product.sku.id);
    if (index >= 0) {
      next[index] = next[index].copyWith(qty: next[index].qty + count);
    } else {
      next.add(
        CartItem(
          skuId: product.sku.id,
          spuId: product.spu.id,
          name: product.name,
          spec: product.specText,
          price: product.price,
          image: product.spu.mainImage,
          qty: count,
        ),
      );
    }
    _save(next);
  }

  void changeQty(int skuId, int qty) {
    final next = state.map((item) {
      if (item.skuId == skuId) {
        return item.copyWith(qty: qty < 1 ? 1 : qty);
      }
      return item;
    }).toList();
    _save(next);
  }

  void remove(int skuId) {
    _save(state.where((item) => item.skuId != skuId).toList());
  }

  void clear() {
    state = const [];
    _storage.clearCart();
  }
}

final cartProvider = StateNotifierProvider<CartController, List<CartItem>>((ref) {
  return CartController(ref.watch(storageProvider));
});

class CartPage extends ConsumerWidget {
  const CartPage({super.key});

  Future<void> _clearCart(BuildContext context, WidgetRef ref) async {
    final confirmed = await showConfirmDialog(
      context,
      title: '清空购物车',
      content: '确定移除购物车中的全部商品吗？',
      confirmText: '清空',
    );
    if (confirmed && context.mounted) {
      ref.read(cartProvider.notifier).clear();
    }
  }

  void _checkout(BuildContext context, WidgetRef ref, List<CartItem> items) {
    if (items.isEmpty) {
      return;
    }
    if (!ref.read(authProvider).isLoggedIn) {
      showMessage(context, '请先登录');
      context.push('/login');
      return;
    }
    if (ref.read(selectedStoreProvider) == null) {
      showMessage(context, '请先选择门店');
      context.go('/stores');
      return;
    }
    context.push('/checkout');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final items = ref.watch(cartProvider);
    final store = ref.watch(selectedStoreProvider);
    final subtotal = items.fold<double>(
      0,
      (sum, item) => sum + item.price * item.qty,
    );
    return Scaffold(
      appBar: AppBar(
        title: const Text('购物车'),
        actions: [
          if (items.isNotEmpty)
            TextButton(
              onPressed: () => _clearCart(context, ref),
              child: const Text('清空'),
            ),
          const SizedBox(width: 4),
        ],
      ),
      body: items.isEmpty
          ? EmptyView(
              text: '购物车还是空的，去挑点好物吧',
              icon: Icons.shopping_cart_outlined,
              actionText: '去逛逛',
              onAction: () => context.push('/products'),
            )
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                  child: _SelectedStoreCard(
                    store: store,
                    onTap: () => context.go('/stores'),
                  ),
                ),
                Expanded(
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                    itemCount: items.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final item = items[index];
                      return _CartTile(
                        item: item,
                        onChanged: (qty) =>
                            ref.read(cartProvider.notifier).changeQty(item.skuId, qty),
                        onRemove: () =>
                            ref.read(cartProvider.notifier).remove(item.skuId),
                      );
                    },
                  ),
                ),
                _CartBottomBar(
                  subtotal: subtotal,
                  itemCount: items.fold<int>(0, (sum, item) => sum + item.qty),
                  onCheckout: () => _checkout(context, ref, items),
                ),
              ],
            ),
    );
  }
}

class _SelectedStoreCard extends StatelessWidget {
  const _SelectedStoreCard({required this.store, required this.onTap});

  final Store? store;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(
            children: [
              const Icon(
                Icons.storefront_outlined,
                size: 20,
                color: AppTheme.primary,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      store == null ? '尚未选择门店' : store!.name,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    if (store != null) ...[
                      const SizedBox(height: 2),
                      Text(
                        store!.address.isEmpty ? '点击可切换门店' : store!.address,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              Text(
                store == null ? '去选择' : '切换',
                style: const TextStyle(
                  fontSize: 12,
                  color: AppTheme.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const Icon(
                Icons.chevron_right,
                size: 18,
                color: AppTheme.textSecondary,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CartTile extends StatelessWidget {
  const _CartTile({
    required this.item,
    required this.onChanged,
    required this.onRemove,
  });

  final CartItem item;
  final ValueChanged<int> onChanged;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ProductImage(image: item.image, name: item.name, size: 72),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        item.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          height: 1.35,
                        ),
                      ),
                    ),
                    InkWell(
                      onTap: onRemove,
                      child: const Padding(
                        padding: EdgeInsets.only(left: 6),
                        child: Icon(
                          Icons.close,
                          size: 18,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  item.spec.isEmpty ? '默认规格' : item.spec,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(child: PriceText(value: item.price, size: 16)),
                    QuantityStepper(value: item.qty, onChanged: onChanged),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CartBottomBar extends StatelessWidget {
  const _CartBottomBar({
    required this.subtotal,
    required this.itemCount,
    required this.onCheckout,
  });

  final double subtotal;
  final int itemCount;
  final VoidCallback onCheckout;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: Color(0xFFE8EEEA))),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      const Text('合计 ', style: TextStyle(fontSize: 13)),
                      PriceText(value: subtotal, size: 22),
                    ],
                  ),
                  Text(
                    '共 $itemCount 件，优惠券与积分在结算页抵扣',
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            FilledButton(
              onPressed: onCheckout,
              child: const Text('去结算'),
            ),
          ],
        ),
      ),
    );
  }
}

class CheckoutPage extends ConsumerStatefulWidget {
  const CheckoutPage({super.key});

  @override
  ConsumerState<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends ConsumerState<CheckoutPage> {
  final TextEditingController _pointsController = TextEditingController();
  final TextEditingController _remarkController = TextEditingController();

  bool _loading = true;
  String? _error;
  bool _submitting = false;
  String _deliveryType = 'PICKUP';
  int _memberCouponId = 0;
  int _addressId = 0;
  MemberProfile? _profile;
  CouponCenter? _coupons;
  List<MemberAddress> _addresses = const [];
  DeliveryRule? _deliveryRule;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _pointsController.dispose();
    _remarkController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final api = ref.read(appApiProvider);
      final profile = await api.profile();
      final coupons = await api.coupons();
      final addresses = await api.addresses();
      if (!mounted) {
        return;
      }
      var selectedAddressId = _addressId;
      if (!addresses.any((address) => address.id == selectedAddressId)) {
        selectedAddressId = 0;
        for (final address in addresses) {
          if (address.isDefault) {
            selectedAddressId = address.id;
            break;
          }
        }
        if (selectedAddressId == 0 && addresses.isNotEmpty) {
          selectedAddressId = addresses.first.id;
        }
      }
      setState(() {
        _profile = profile;
        _coupons = coupons;
        _addresses = addresses;
        _addressId = selectedAddressId;
        _loading = false;
      });
      await _loadRule();
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

  Future<void> _loadRule() async {
    final store = ref.read(selectedStoreProvider);
    if (store == null) {
      if (mounted) {
        setState(() => _deliveryRule = null);
      }
      return;
    }
    try {
      final rule = await ref.read(appApiProvider).deliveryRule(store.id);
      if (mounted) {
        setState(() => _deliveryRule = rule);
      }
    } catch (_) {
      if (mounted) {
        setState(() => _deliveryRule = null);
      }
    }
  }

  Future<void> _loadAddresses() async {
    try {
      final addresses = await ref.read(appApiProvider).addresses();
      if (!mounted) {
        return;
      }
      setState(() {
        _addresses = addresses;
        if (!addresses.any((address) => address.id == _addressId)) {
          _addressId = addresses.isEmpty ? 0 : addresses.first.id;
        }
      });
    } catch (_) {
      // 地址刷新失败不阻断结算。
    }
  }

  Future<void> _openAddressPage() async {
    await context.push('/member/addresses');
    if (mounted) {
      await _loadAddresses();
    }
  }

  MemberAddress? get _selectedAddress {
    for (final address in _addresses) {
      if (address.id == _addressId) {
        return address;
      }
    }
    return null;
  }

  List<MemberCouponRow> get _usableCoupons {
    return (_coupons?.myCoupons ?? const <MemberCouponRow>[])
        .where((row) => row.memberCoupon.status == 'UNUSED')
        .toList();
  }

  ({double subtotal, double discount, double pointsDiscount, double freight, double payable, int pointsUsed}) _calc(
    List<CartItem> items,
  ) {
    final subtotal = items.fold<double>(
      0,
      (sum, item) => sum + item.price * item.qty,
    );
    Coupon? coupon;
    for (final row in _usableCoupons) {
      if (row.memberCoupon.id == _memberCouponId) {
        coupon = row.coupon;
        break;
      }
    }
    var discount = 0.0;
    if (coupon != null && subtotal >= coupon.thresholdAmount) {
      discount = coupon.type == 'DISCOUNT'
          ? subtotal - subtotal * coupon.discountRate / 10
          : coupon.discountAmount;
    }
    if (discount > subtotal) {
      discount = subtotal;
    }
    final typedPoints = int.tryParse(_pointsController.text.trim()) ?? 0;
    final maxPoints = _profile?.member.points ?? 0;
    final pointsUsed = typedPoints < 0
        ? 0
        : (typedPoints > maxPoints ? maxPoints : typedPoints);
    var pointsDiscount = pointsUsed / 100;
    final afterCoupon = subtotal - discount;
    if (pointsDiscount > afterCoupon) {
      pointsDiscount = afterCoupon < 0 ? 0.0 : afterCoupon;
    }
    var freight = 0.0;
    if (_deliveryType == 'DELIVERY' && _deliveryRule != null) {
      final rule = _deliveryRule!;
      if (rule.freeThreshold <= 0 || subtotal < rule.freeThreshold) {
        freight = rule.deliveryFee;
      }
    }
    var payable = subtotal - discount - pointsDiscount + freight;
    if (payable < 0) {
      payable = 0.0;
    }
    return (
      subtotal: subtotal,
      discount: discount,
      pointsDiscount: pointsDiscount.toDouble(),
      freight: freight,
      payable: payable,
      pointsUsed: pointsUsed,
    );
  }

  Future<void> _submit(List<CartItem> items) async {
    if (_submitting) {
      return;
    }
    final store = ref.read(selectedStoreProvider);
    if (store == null) {
      showMessage(context, '请先选择门店');
      return;
    }
    if (items.isEmpty) {
      showMessage(context, '购物车为空');
      return;
    }
    final amounts = _calc(items);
    final rule = _deliveryRule;
    if (_deliveryType == 'DELIVERY' &&
        rule != null &&
        rule.minOrderAmount > 0 &&
        amounts.subtotal < rule.minOrderAmount) {
      showMessage(context, '外送需满 ¥${money(rule.minOrderAmount)}');
      return;
    }
    final address = _selectedAddress;
    if (_deliveryType == 'DELIVERY' && address == null) {
      showMessage(context, '请选择收货地址');
      return;
    }
    setState(() => _submitting = true);
    try {
      final order = await ref.read(appApiProvider).createOrder({
        'storeId': store.id,
        'deliveryType': _deliveryType,
        'memberCouponId': _memberCouponId == 0 ? null : _memberCouponId,
        'pointsUsed': amounts.pointsUsed,
        'deliveryName': _deliveryType == 'DELIVERY' ? address!.name : '',
        'deliveryPhone': _deliveryType == 'DELIVERY' ? address!.phone : '',
        'deliveryAddress': _deliveryType == 'DELIVERY' ? address!.address : '',
        'remark': _remarkController.text.trim(),
        'items': items
            .map((item) => {'skuId': item.skuId, 'qty': item.qty})
            .toList(),
      });
      ref.read(cartProvider.notifier).clear();
      if (!mounted) {
        return;
      }
      showMessage(context, '下单成功，请完成支付');
      context.go('/orders/${order.id}');
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
    } catch (error) {
      if (mounted) {
        showMessage(context, '下单失败：$error');
      }
    } finally {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = ref.watch(cartProvider);
    final store = ref.watch(selectedStoreProvider);
    final amounts = _calc(items);
    return Scaffold(
      appBar: AppBar(title: const Text('确认订单')),
      body: StateView(
        loading: _loading,
        error: _error,
        onRetry: _load,
        child: items.isEmpty
            ? EmptyView(
                text: '购物车为空，无法结算',
                icon: Icons.shopping_cart_outlined,
                actionText: '去逛逛',
                onAction: () => context.push('/products'),
              )
            : ListView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                children: [
                  _SelectedStoreCard(
                    store: store,
                    onTap: () => context.go('/stores'),
                  ),
                  const SizedBox(height: 10),
                  _CheckoutSection(
                    children: [
                      const Text(
                        '履约方式',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 10),
                      SegmentedButton<String>(
                        segments: const [
                          ButtonSegment(
                            value: 'PICKUP',
                            label: Text('到店自提'),
                            icon: Icon(Icons.store_outlined, size: 18),
                          ),
                          ButtonSegment(
                            value: 'DELIVERY',
                            label: Text('外送到家'),
                            icon: Icon(Icons.delivery_dining_outlined, size: 18),
                          ),
                        ],
                        selected: {_deliveryType},
                        onSelectionChanged: (values) {
                          setState(() => _deliveryType = values.first);
                          if (values.first == 'DELIVERY' && _deliveryRule == null) {
                            _loadRule();
                          }
                        },
                      ),
                      if (_deliveryType == 'DELIVERY') ...[
                        const SizedBox(height: 14),
                        if (_addresses.isEmpty)
                          Row(
                            children: [
                              const Expanded(
                                child: Text(
                                  '暂无收货地址',
                                  style: TextStyle(
                                    fontSize: 13,
                                    color: AppTheme.textSecondary,
                                  ),
                                ),
                              ),
                              TextButton(
                                onPressed: _openAddressPage,
                                child: const Text('去添加'),
                              ),
                            ],
                          )
                        else
                          DropdownButtonFormField<int>(
                            value: _addresses.any(
                              (address) => address.id == _addressId,
                            )
                                ? _addressId
                                : null,
                            hint: const Text('请选择收货地址'),
                            decoration: const InputDecoration(
                              labelText: '收货地址',
                              prefixIcon: Icon(Icons.location_on_outlined),
                            ),
                            items: _addresses
                                .map(
                                  (address) => DropdownMenuItem<int>(
                                    value: address.id,
                                    child: Text(
                                      '${address.name} ${address.phone}',
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                )
                                .toList(),
                            onChanged: (value) =>
                                setState(() => _addressId = value ?? 0),
                          ),
                        if (_selectedAddress != null) ...[
                          const SizedBox(height: 8),
                          Text(
                            _selectedAddress!.address,
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                              height: 1.4,
                            ),
                          ),
                        ],
                        if (_deliveryRule != null) ...[
                          const SizedBox(height: 8),
                          Text(
                            '起送 ¥${money(_deliveryRule!.minOrderAmount)} · 配送费 ¥${money(_deliveryRule!.deliveryFee)}'
                            '${_deliveryRule!.freeThreshold > 0 ? ' · 满 ¥${money(_deliveryRule!.freeThreshold)} 免配送费' : ''}',
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                        ],
                        const SizedBox(height: 8),
                        Align(
                          alignment: Alignment.centerRight,
                          child: TextButton(
                            onPressed: _openAddressPage,
                            child: const Text('管理收货地址'),
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 10),
                  _CheckoutSection(
                    children: [
                      const Text(
                        '优惠与积分',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<int>(
                        value: _memberCouponId,
                        decoration: const InputDecoration(
                          labelText: '优惠券',
                          prefixIcon: Icon(Icons.local_offer_outlined),
                        ),
                        items: [
                          const DropdownMenuItem<int>(
                            value: 0,
                            child: Text('不使用优惠券'),
                          ),
                          ..._usableCoupons.map(
                            (row) => DropdownMenuItem<int>(
                              value: row.memberCoupon.id,
                              child: Text(
                                '${row.coupon.name} · ${couponThresholdText(row.coupon.thresholdAmount)}',
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ),
                        ],
                        onChanged: (value) =>
                            setState(() => _memberCouponId = value ?? 0),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _pointsController,
                        keyboardType: TextInputType.number,
                        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                        onChanged: (_) => setState(() {}),
                        decoration: InputDecoration(
                          labelText: '使用积分',
                          hintText: '100 积分抵 1 元',
                          prefixIcon: const Icon(Icons.stars_outlined),
                          helperText:
                              '当前可用 ${_profile?.member.points ?? 0} 积分',
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _remarkController,
                        maxLines: 2,
                        maxLength: 100,
                        decoration: const InputDecoration(
                          labelText: '订单备注',
                          hintText: '选填，例如：不要塑料袋',
                          prefixIcon: Icon(Icons.edit_note_outlined),
                          counterText: '',
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  _CheckoutSection(
                    children: [
                      const Text(
                        '金额明细',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 10),
                      _AmountRow(label: '商品金额', value: amounts.subtotal),
                      _AmountRow(
                        label: '优惠券抵扣',
                        value: -amounts.discount,
                      ),
                      _AmountRow(
                        label: '积分抵扣',
                        value: -amounts.pointsDiscount,
                      ),
                      if (amounts.freight > 0)
                        _AmountRow(label: '配送费', value: amounts.freight),
                      const Divider(height: 20),
                      Row(
                        children: [
                          const Expanded(
                            child: Text(
                              '应付金额',
                              style: TextStyle(fontWeight: FontWeight.w600),
                            ),
                          ),
                          PriceText(value: amounts.payable, size: 22),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
      ),
      bottomNavigationBar: _loading || _error != null
          ? null
          : SafeArea(
              child: Container(
                padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  border: Border(top: BorderSide(color: Color(0xFFE8EEEA))),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Row(
                        children: [
                          const Text('合计 ', style: TextStyle(fontSize: 13)),
                          PriceText(value: amounts.payable, size: 22),
                        ],
                      ),
                    ),
                    FilledButton(
                      onPressed: _submitting || items.isEmpty
                          ? null
                          : () => _submit(items),
                      child: _submitting
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Text('提交订单'),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}

class _CheckoutSection extends StatelessWidget {
  const _CheckoutSection({required this.children});

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
