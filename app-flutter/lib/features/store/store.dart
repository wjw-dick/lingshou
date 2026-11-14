import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/format.dart';
import '../../core/http_client.dart';
import '../../core/storage.dart';
import '../../core/theme.dart';
import '../../core/widgets.dart';
import '../../data/api.dart';
import '../../data/models.dart';

final storeListProvider =
    FutureProvider.autoDispose.family<List<Store>, String>((ref, keyword) {
  return ref.watch(appApiProvider).stores(keyword: keyword);
});

class StoreDetailData {
  const StoreDetailData({required this.store, required this.rule});

  final Store store;
  final DeliveryRule? rule;
}

final storeDetailProvider =
    FutureProvider.autoDispose.family<StoreDetailData, int>((ref, storeId) async {
  final api = ref.watch(appApiProvider);
  final stores = await api.stores();
  Store? store;
  for (final item in stores) {
    if (item.id == storeId) {
      store = item;
      break;
    }
  }
  if (store == null) {
    throw const ApiException('门店不存在或已下线');
  }
  DeliveryRule? rule;
  try {
    rule = await api.deliveryRule(storeId);
  } catch (_) {
    rule = null;
  }
  return StoreDetailData(store: store, rule: rule);
});

class SelectedStoreController extends StateNotifier<Store?> {
  SelectedStoreController(this._storage) : super(_storage.selectedStore);

  final StorageService _storage;

  Future<void> select(Store store) async {
    await _storage.saveSelectedStore(store);
    state = store;
  }
}

final selectedStoreProvider =
    StateNotifierProvider<SelectedStoreController, Store?>((ref) {
  return SelectedStoreController(ref.watch(storageProvider));
});

class StoreListPage extends ConsumerStatefulWidget {
  const StoreListPage({super.key});

  @override
  ConsumerState<StoreListPage> createState() => _StoreListPageState();
}

class _StoreListPageState extends ConsumerState<StoreListPage> {
  final TextEditingController _searchController = TextEditingController();
  String _keyword = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _search() {
    final keyword = _searchController.text.trim();
    if (keyword == _keyword) {
      return;
    }
    setState(() => _keyword = keyword);
  }

  Future<void> _refresh() async {
    ref.invalidate(storeListProvider(_keyword));
    try {
      await ref.read(storeListProvider(_keyword).future);
    } catch (_) {
      // 错误已由页面状态展示。
    }
  }

  Future<void> _selectStore(Store store) async {
    await ref.read(selectedStoreProvider.notifier).select(store);
    if (!mounted) {
      return;
    }
    showMessage(context, '已选择 ${store.name}');
    context.go('/home');
  }

  @override
  Widget build(BuildContext context) {
    final selected = ref.watch(selectedStoreProvider);
    final storesAsync = ref.watch(storeListProvider(_keyword));
    return Scaffold(
      appBar: AppBar(title: const Text('找店')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              textInputAction: TextInputAction.search,
              onSubmitted: (_) => _search(),
              decoration: InputDecoration(
                hintText: '搜索门店名称或地址',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: IconButton(
                  onPressed: _search,
                  icon: const Icon(Icons.arrow_forward),
                ),
              ),
            ),
          ),
          Expanded(
            child: storesAsync.when(
              loading: () => const LoadingView(),
              error: (error, stackTrace) => ErrorView(
                message: error.toString(),
                onRetry: () => ref.invalidate(storeListProvider(_keyword)),
              ),
              data: (stores) {
                if (stores.isEmpty) {
                  return const EmptyView(
                    text: '没有找到符合条件的门店',
                    icon: Icons.storefront_outlined,
                  );
                }
                return RefreshIndicator(
                  onRefresh: _refresh,
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                    itemCount: stores.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final store = stores[index];
                      return _StoreListTile(
                        store: store,
                        selected: selected?.id == store.id,
                        onTap: () => context.push('/stores/${store.id}'),
                        onSelect: () => _selectStore(store),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _StoreListTile extends StatelessWidget {
  const _StoreListTile({
    required this.store,
    required this.selected,
    required this.onTap,
    required this.onSelect,
  });

  final Store store;
  final bool selected;
  final VoidCallback onTap;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: AppTheme.primary.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      store.name.isEmpty ? '店' : store.name.substring(0, 1),
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                        color: AppTheme.primary,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          store.name,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          store.region.isEmpty ? '附近门店' : store.region,
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (selected)
                    const Icon(
                      Icons.check_circle,
                      color: AppTheme.primary,
                      size: 20,
                    ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  _Tag(
                    text: store.isOpen ? '营业中' : '休息中',
                    color: store.isOpen ? AppTheme.primary : AppTheme.textSecondary,
                  ),
                  const SizedBox(width: 6),
                  _Tag(
                    text: store.deliveryEnabled ? '支持外送' : '仅到店自提',
                    color: store.deliveryEnabled
                        ? const Color(0xFF2F7FD1)
                        : AppTheme.textSecondary,
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.location_on_outlined,
                    size: 16,
                    color: AppTheme.textSecondary,
                  ),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      store.address.isEmpty ? '暂无地址信息' : store.address,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppTheme.textSecondary,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
              if (store.businessHours.isNotEmpty) ...[
                const SizedBox(height: 4),
                Row(
                  children: [
                    const Icon(
                      Icons.schedule,
                      size: 16,
                      color: AppTheme.textSecondary,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      store.businessHours,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 10),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    onPressed: onSelect,
                    child: Text(selected ? '当前门店' : '选择该门店'),
                  ),
                  const SizedBox(width: 4),
                  OutlinedButton(
                    onPressed: onTap,
                    child: const Text('查看详情'),
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

class _Tag extends StatelessWidget {
  const _Tag({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(5),
      ),
      child: Text(
        text,
        style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w500),
      ),
    );
  }
}

class StoreDetailPage extends ConsumerWidget {
  const StoreDetailPage({super.key, required this.storeId});

  final int storeId;

  Future<void> _selectStore(BuildContext context, WidgetRef ref, Store store) async {
    await ref.read(selectedStoreProvider.notifier).select(store);
    if (!context.mounted) {
      return;
    }
    showMessage(context, '已选择 ${store.name}');
    context.go('/home');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(storeDetailProvider(storeId));
    return Scaffold(
      appBar: AppBar(title: const Text('门店详情')),
      body: detailAsync.when(
        loading: () => const LoadingView(),
        error: (error, stackTrace) => ErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(storeDetailProvider(storeId)),
        ),
        data: (detail) {
          final store = detail.store;
          final rule = detail.rule;
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
            children: [
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF12A15A), Color(0xFF3DBB78)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(18),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.18),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        store.name.isEmpty ? '店' : store.name.substring(0, 1),
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 26,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            store.name,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 20,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            '${store.isOpen ? '营业中' : '休息中'} · ${store.deliveryEnabled ? '支持外送' : '仅到店自提'}',
                            style: TextStyle(
                              color: Colors.white.withOpacity(0.88),
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              _InfoCard(
                children: [
                  _InfoRow(
                    icon: Icons.location_on_outlined,
                    label: '门店地址',
                    value: store.address.isEmpty ? '暂无' : store.address,
                  ),
                  _InfoRow(
                    icon: Icons.schedule,
                    label: '营业时间',
                    value: store.businessHours.isEmpty ? '暂无' : store.businessHours,
                  ),
                  _InfoRow(
                    icon: Icons.phone_outlined,
                    label: '联系电话',
                    value: store.phone.isEmpty ? '暂无' : store.phone,
                  ),
                  _InfoRow(
                    icon: Icons.map_outlined,
                    label: '所属区域',
                    value: store.region.isEmpty ? '暂无' : store.region,
                  ),
                ],
              ),
              const SizedBox(height: 14),
              _InfoCard(
                children: [
                  const Text(
                    '配送规则',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 10),
                  if (rule == null)
                    const Text(
                      '该门店暂未配置外送规则，支持到店自提。',
                      style: TextStyle(
                        fontSize: 13,
                        color: AppTheme.textSecondary,
                      ),
                    )
                  else ...[
                    _InfoRow(
                      icon: Icons.shopping_bag_outlined,
                      label: '起送金额',
                      value: '¥${money(rule.minOrderAmount)}',
                    ),
                    _InfoRow(
                      icon: Icons.delivery_dining_outlined,
                      label: '配送费',
                      value: '¥${money(rule.deliveryFee)}',
                    ),
                    _InfoRow(
                      icon: Icons.local_offer_outlined,
                      label: '免配送费',
                      value: rule.freeThreshold > 0
                          ? '满 ¥${money(rule.freeThreshold)}'
                          : '不设门槛',
                    ),
                    _InfoRow(
                      icon: Icons.explore_outlined,
                      label: '配送范围',
                      value: rule.radiusM > 0 ? '约 ${rule.radiusM} 米' : '暂无限制',
                    ),
                  ],
                ],
              ),
            ],
          );
        },
      ),
      bottomNavigationBar: detailAsync.maybeWhen(
        data: (detail) => SafeArea(
          child: Container(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
            decoration: const BoxDecoration(
              color: Colors.white,
              border: Border(top: BorderSide(color: Color(0xFFE8EEEA))),
            ),
            child: FilledButton(
              onPressed: () => _selectStore(context, ref, detail.store),
              child: const Text('选择该门店'),
            ),
          ),
        ),
        orElse: () => const SizedBox.shrink(),
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.children});

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

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: AppTheme.textSecondary),
          const SizedBox(width: 8),
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
              value,
              style: const TextStyle(fontSize: 13, height: 1.4),
            ),
          ),
        ],
      ),
    );
  }
}
