import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/config.dart';
import '../../core/format.dart';
import '../../core/http_client.dart';
import '../../core/theme.dart';
import '../../core/widgets.dart';
import '../../data/api.dart';
import '../../data/models.dart';
import '../auth/auth.dart';
import '../cart/cart.dart';
import '../store/store.dart';

final homeProvider = FutureProvider.autoDispose<HomeData>((ref) async {
  final token = ref.watch(authProvider.select((state) => state.session?.token));
  if (token == null) {
    throw const ApiException('请先登录');
  }
  return ref.watch(appApiProvider).home();
});

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  Future<void> _refresh(WidgetRef ref) async {
    ref.invalidate(homeProvider);
    try {
      await ref.read(homeProvider.future);
    } catch (_) {
      // 错误状态由 AsyncValue 展示。
    }
  }

  void _addToCart(BuildContext context, WidgetRef ref, ProductItem product) {
    ref.read(cartProvider.notifier).add(product);
    showMessage(context, '已加入购物车');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);
    final store = ref.watch(selectedStoreProvider);
    final cartCount = ref.watch(cartProvider).fold<int>(
          0,
          (sum, item) => sum + item.qty,
        );
    return Scaffold(
      appBar: AppBar(
        title: InkWell(
          onTap: () => context.go('/stores'),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.location_on,
                size: 18,
                color: AppTheme.primary,
              ),
              const SizedBox(width: 4),
              Flexible(
                child: Text(
                  store?.name ?? '选择门店',
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 15),
                ),
              ),
              const Icon(
                Icons.keyboard_arrow_down,
                size: 18,
                color: AppTheme.textSecondary,
              ),
            ],
          ),
        ),
        actions: [
          IconButton(
            onPressed: () => context.push('/cart'),
            icon: Badge(
              isLabelVisible: cartCount > 0,
              label: Text('$cartCount'),
              child: const Icon(Icons.shopping_cart_outlined),
            ),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: auth.isLoggedIn
          ? _HomeBody(
              onRefresh: () => _refresh(ref),
              onAddToCart: (product) => _addToCart(context, ref, product),
            )
          : LoginRequiredView(
              onLogin: () => context.push('/login'),
              message: '登录后查看会员权益、门店与专属商品',
            ),
    );
  }
}

class _HomeBody extends ConsumerWidget {
  const _HomeBody({required this.onRefresh, required this.onAddToCart});

  final Future<void> Function() onRefresh;
  final ValueChanged<ProductItem> onAddToCart;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final homeAsync = ref.watch(homeProvider);
    return homeAsync.when(
      loading: () => const LoadingView(),
      error: (error, stackTrace) => ErrorView(
        message: error.toString(),
        onRetry: () => ref.invalidate(homeProvider),
      ),
      data: (home) {
        final selectedStoreId = ref.watch(selectedStoreProvider)?.id;
        return RefreshIndicator(
          onRefresh: onRefresh,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 28),
            children: [
              _MemberHeader(member: home.member),
              const SizedBox(height: 14),
              _BannerList(banners: home.banners),
              const SizedBox(height: 14),
              _QuickActions(
                onStores: () => context.go('/stores'),
                onCoupons: () => context.push('/member/coupons'),
                onCart: () => context.push('/cart'),
                onOrders: () => context.go('/orders'),
              ),
              if (home.notices.isNotEmpty) ...[
                const SizedBox(height: 12),
                _NoticeBar(notice: home.notices.first),
              ],
              if (home.categories.isNotEmpty) ...[
                const SizedBox(height: 18),
                const SectionHeader(title: '商品分类'),
                const SizedBox(height: 8),
                SizedBox(
                  height: 38,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: home.categories.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 8),
                    itemBuilder: (context, index) {
                      final category = home.categories[index];
                      return ActionChip(
                        label: Text(category.name),
                        onPressed: () => context.push(
                          '/products?categoryId=${category.id}',
                        ),
                        backgroundColor: Colors.white,
                        side: const BorderSide(color: Color(0xFFE3EAE5)),
                        labelStyle: const TextStyle(
                          fontSize: 13,
                          color: AppTheme.textPrimary,
                        ),
                      );
                    },
                  ),
                ),
              ],
              const SizedBox(height: 18),
              SectionHeader(
                title: '附近门店',
                actionText: '查看全部',
                onAction: () => context.go('/stores'),
              ),
              const SizedBox(height: 8),
              if (home.stores.isEmpty)
                const _InlineEmpty(text: '暂无在线门店')
              else
                SizedBox(
                  height: 148,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: home.stores.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 10),
                    itemBuilder: (context, index) {
                      final store = home.stores[index];
                      return _StoreMiniCard(
                        store: store,
                        selected: selectedStoreId == store.id,
                        onTap: () => context.push('/stores/${store.id}'),
                      );
                    },
                  ),
                ),
              const SizedBox(height: 18),
              SectionHeader(
                title: '为你推荐',
                subtitle: '门店精选好物',
                actionText: '更多商品',
                onAction: () => context.push('/products'),
              ),
              const SizedBox(height: 8),
              if (home.products.isEmpty)
                const _InlineEmpty(text: '门店正在补货，敬请期待')
              else
                GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    crossAxisSpacing: 10,
                    mainAxisSpacing: 10,
                    childAspectRatio: 0.6,
                  ),
                  itemCount: home.products.length,
                  itemBuilder: (context, index) {
                    final product = home.products[index];
                    return ProductCard(
                      product: product,
                      onTap: () => context.push('/products/${product.sku.id}'),
                      onAdd: () => onAddToCart(product),
                    );
                  },
                ),
            ],
          ),
        );
      },
    );
  }
}

class _MemberHeader extends StatelessWidget {
  const _MemberHeader({required this.member});

  final HomeMember member;

  @override
  Widget build(BuildContext context) {
    final initial = member.nickname.isEmpty ? '悦' : member.nickname.substring(0, 1);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF12A15A), Color(0xFF42BD7C)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                alignment: Alignment.center,
                child: Text(
                  initial,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '你好，${member.nickname}',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '${AppConfig.appName} · 新鲜便利每一天',
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.82),
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              _HeaderStat(label: '积分', value: '${member.points}'),
              Container(
                width: 1,
                height: 28,
                color: Colors.white.withOpacity(0.2),
                margin: const EdgeInsets.symmetric(horizontal: 20),
              ),
              _HeaderStat(
                label: '余额',
                value: '¥${money(member.walletBalance)}',
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _HeaderStat extends StatelessWidget {
  const _HeaderStat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: TextStyle(
            color: Colors.white.withOpacity(0.75),
            fontSize: 11,
          ),
        ),
      ],
    );
  }
}

class _BannerList extends StatelessWidget {
  const _BannerList({required this.banners});

  final List<BannerItem> banners;

  @override
  Widget build(BuildContext context) {
    if (banners.isEmpty) {
      return Container(
        height: 118,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFF0E8F4F), Color(0xFF5BC98B)],
            begin: Alignment.centerLeft,
            end: Alignment.centerRight,
          ),
          borderRadius: BorderRadius.circular(16),
        ),
        child: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              '新会员专享礼',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.w800,
              ),
            ),
            SizedBox(height: 6),
            Text(
              '开门红好物，线上下单到店自提',
              style: TextStyle(color: Colors.white70, fontSize: 12),
            ),
          ],
        ),
      );
    }
    return SizedBox(
      height: 128,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: banners.length,
        separatorBuilder: (_, __) => const SizedBox(width: 10),
        itemBuilder: (context, index) {
          final banner = banners[index];
          final url = AppConfig.resolveImage(banner.image);
          return ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: SizedBox(
              width: 280,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  if (url.isEmpty)
                    const DecoratedBox(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [Color(0xFF0E8F4F), Color(0xFF5BC98B)],
                          begin: Alignment.centerLeft,
                          end: Alignment.centerRight,
                        ),
                      ),
                    )
                  else
                    Image.network(
                      url,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) =>
                          const DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [Color(0xFF0E8F4F), Color(0xFF5BC98B)],
                          ),
                        ),
                      ),
                    ),
                  DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          Colors.black.withOpacity(0.32),
                          Colors.transparent,
                        ],
                        begin: Alignment.bottomLeft,
                        end: Alignment.topRight,
                      ),
                    ),
                  ),
                  Positioned(
                    left: 16,
                    right: 16,
                    bottom: 14,
                    child: Text(
                      banner.title.isEmpty ? AppConfig.appName : banner.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _QuickActions extends StatelessWidget {
  const _QuickActions({
    required this.onStores,
    required this.onCoupons,
    required this.onCart,
    required this.onOrders,
  });

  final VoidCallback onStores;
  final VoidCallback onCoupons;
  final VoidCallback onCart;
  final VoidCallback onOrders;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _QuickAction(
          icon: Icons.storefront_outlined,
          label: '找店',
          onTap: onStores,
        ),
        _QuickAction(
          icon: Icons.local_offer_outlined,
          label: '优惠券',
          onTap: onCoupons,
        ),
        _QuickAction(
          icon: Icons.shopping_cart_outlined,
          label: '购物车',
          onTap: onCart,
        ),
        _QuickAction(
          icon: Icons.receipt_long_outlined,
          label: '我的订单',
          onTap: onOrders,
        ),
      ],
    );
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10),
          child: Column(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: AppTheme.primary, size: 22),
              ),
              const SizedBox(height: 6),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 12,
                  color: AppTheme.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NoticeBar extends StatelessWidget {
  const _NoticeBar({required this.notice});

  final NoticeItem notice;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF7E6),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.campaign_outlined,
            size: 18,
            color: AppTheme.warning,
          ),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              notice.title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 12, color: Color(0xFF8A5A13)),
            ),
          ),
        ],
      ),
    );
  }
}

class _StoreMiniCard extends StatelessWidget {
  const _StoreMiniCard({
    required this.store,
    required this.selected,
    required this.onTap,
  });

  final Store store;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Container(
          width: 210,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: selected ? AppTheme.primary : Colors.transparent,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      store.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  if (selected)
                    const Icon(
                      Icons.check_circle,
                      size: 17,
                      color: AppTheme.primary,
                    ),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                store.address.isEmpty ? '暂无地址' : store.address,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 12,
                  color: AppTheme.textSecondary,
                  height: 1.4,
                ),
              ),
              const Spacer(),
              Row(
                children: [
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: (store.isOpen ? AppTheme.primary : Colors.grey)
                          .withOpacity(0.1),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      store.isOpen ? '营业中' : '休息中',
                      style: TextStyle(
                        fontSize: 10,
                        color: store.isOpen
                            ? AppTheme.primary
                            : AppTheme.textSecondary,
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Text(
                    store.deliveryEnabled ? '支持外送' : '仅自提',
                    style: const TextStyle(
                      fontSize: 10,
                      color: AppTheme.textSecondary,
                    ),
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

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 24),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        text,
        style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
      ),
    );
  }
}
