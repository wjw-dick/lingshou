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

final profileProvider = FutureProvider.autoDispose<MemberProfile>((ref) async {
  final token = ref.watch(authProvider.select((state) => state.session?.token));
  if (token == null) {
    throw const ApiException('请先登录');
  }
  return ref.watch(appApiProvider).profile();
});

final favoritesProvider =
    FutureProvider.autoDispose<List<ProductItem>>((ref) async {
  final token = ref.watch(authProvider.select((state) => state.session?.token));
  if (token == null) {
    throw const ApiException('请先登录');
  }
  return ref.watch(appApiProvider).favorites();
});

class MemberPage extends ConsumerWidget {
  const MemberPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);
    if (!auth.isLoggedIn) {
      return Scaffold(
        appBar: AppBar(title: const Text('我的')),
        body: LoginRequiredView(
          onLogin: () => context.push('/login'),
          message: '登录后享受会员价、积分与优惠券',
        ),
      );
    }
    final profileAsync = ref.watch(profileProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('我的')),
      body: profileAsync.when(
        loading: () => const LoadingView(),
        error: (error, stackTrace) => ErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(profileProvider),
        ),
        data: (profile) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(profileProvider);
            try {
              await ref.read(profileProvider.future);
            } catch (_) {
              // 错误由 AsyncValue 展示。
            }
          },
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 28),
            children: [
              _ProfileHeader(profile: profile),
              const SizedBox(height: 12),
              _StatsRow(profile: profile),
              if (profile.level != null &&
                  profile.level!.benefits.isNotEmpty) ...[
                const SizedBox(height: 12),
                _BenefitsCard(profile: profile),
              ],
              const SizedBox(height: 12),
              _MenuCard(
                children: [
                  _MenuTile(
                    icon: Icons.receipt_long_outlined,
                    title: '我的订单',
                    subtitle: '查看订单与评价',
                    onTap: () => context.go('/orders'),
                  ),
                  _MenuTile(
                    icon: Icons.local_offer_outlined,
                    title: '优惠券',
                    subtitle: '${profile.unusedCoupons} 张可用',
                    onTap: () => context.push('/member/coupons'),
                  ),
                  _MenuTile(
                    icon: Icons.location_on_outlined,
                    title: '收货地址',
                    subtitle: '管理配送地址',
                    onTap: () => context.push('/member/addresses'),
                  ),
                  _MenuTile(
                    icon: Icons.favorite_border,
                    title: '我的收藏',
                    subtitle: '收藏的心仪商品',
                    onTap: () => context.push('/member/favorites'),
                  ),
                  _MenuTile(
                    icon: Icons.settings_outlined,
                    title: '设置',
                    subtitle: '账号、推送与关于',
                    onTap: () => context.push('/settings'),
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

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({required this.profile});

  final MemberProfile profile;

  @override
  Widget build(BuildContext context) {
    final member = profile.member;
    final initial = member.nickname.isEmpty ? '悦' : member.nickname.substring(0, 1);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF0F8B4E), Color(0xFF4AC185)],
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
                width: 58,
                height: 58,
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                alignment: Alignment.center,
                child: Text(
                  initial,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 24,
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
                      member.nickname,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 5),
                    Text(
                      profile.level?.name ?? '普通会员',
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.86),
                        fontSize: 12,
                      ),
                    ),
                    if (member.phoneMask.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Text(
                        member.phoneMask,
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.7),
                          fontSize: 11,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              _HeaderValue(label: '积分', value: '${member.points}'),
              const SizedBox(width: 28),
              _HeaderValue(
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

class _HeaderValue extends StatelessWidget {
  const _HeaderValue({required this.label, required this.value});

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
            fontSize: 17,
            fontWeight: FontWeight.w700,
          ),
        ),
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

class _StatsRow extends StatelessWidget {
  const _StatsRow({required this.profile});

  final MemberProfile profile;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Expanded(
            child: _StatItem(
              value: '${profile.member.orderCount}',
              label: '累计订单',
            ),
          ),
          const _StatDivider(),
          Expanded(
            child: _StatItem(
              value: '¥${money(profile.member.totalConsume)}',
              label: '累计消费',
            ),
          ),
          const _StatDivider(),
          Expanded(
            child: _StatItem(
              value: '${profile.unusedCoupons}',
              label: '可用优惠券',
            ),
          ),
        ],
      ),
    );
  }
}

class _StatItem extends StatelessWidget {
  const _StatItem({required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          value,
          style: const TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: 3),
        Text(
          label,
          style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
        ),
      ],
    );
  }
}

class _StatDivider extends StatelessWidget {
  const _StatDivider();

  @override
  Widget build(BuildContext context) {
    return Container(width: 1, height: 26, color: const Color(0xFFE8EEEA));
  }
}

class _BenefitsCard extends StatelessWidget {
  const _BenefitsCard({required this.profile});

  final MemberProfile profile;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF7E6),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.workspace_premium_outlined,
            color: AppTheme.warning,
            size: 20,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${profile.level!.name}权益',
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF8A5A13),
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  profile.level!.benefits,
                  style: const TextStyle(
                    fontSize: 12,
                    color: Color(0xFF8A5A13),
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _MenuCard extends StatelessWidget {
  const _MenuCard({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(children: children),
    );
  }
}

class _MenuTile extends StatelessWidget {
  const _MenuTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      leading: Container(
        width: 38,
        height: 38,
        decoration: BoxDecoration(
          color: AppTheme.primary.withOpacity(0.08),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Icon(icon, color: AppTheme.primary, size: 20),
      ),
      title: Text(
        title,
        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
      ),
      subtitle: Text(
        subtitle,
        style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
      ),
      trailing: const Icon(
        Icons.chevron_right,
        size: 20,
        color: AppTheme.textSecondary,
      ),
    );
  }
}

class FavoritesPage extends ConsumerWidget {
  const FavoritesPage({super.key});

  Future<void> _remove(
    BuildContext context,
    WidgetRef ref,
    ProductItem product,
  ) async {
    try {
      await ref.read(appApiProvider).toggleFavorite(product.sku.id);
      ref.invalidate(favoritesProvider);
      if (context.mounted) {
        showMessage(context, '已取消收藏');
      }
    } catch (error) {
      if (context.mounted) {
        showMessage(context, error.toString());
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);
    if (!auth.isLoggedIn) {
      return Scaffold(
        appBar: AppBar(title: const Text('我的收藏')),
        body: LoginRequiredView(onLogin: () => context.push('/login')),
      );
    }
    final favoritesAsync = ref.watch(favoritesProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('我的收藏')),
      body: favoritesAsync.when(
        loading: () => const LoadingView(),
        error: (error, stackTrace) => ErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(favoritesProvider),
        ),
        data: (products) {
          if (products.isEmpty) {
            return EmptyView(
              text: '还没有收藏商品',
              icon: Icons.favorite_border,
              actionText: '去逛逛',
              onAction: () => context.push('/products'),
            );
          }
          return ListView.separated(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
            itemCount: products.length,
            separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (context, index) {
              final product = products[index];
              return Material(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                child: InkWell(
                  onTap: () => context.push('/products/${product.sku.id}'),
                  borderRadius: BorderRadius.circular(14),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      children: [
                        ProductImage(
                          image: product.spu.mainImage,
                          name: product.name,
                          size: 64,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                product.name,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                product.specText.isEmpty
                                    ? '默认规格'
                                    : product.specText,
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: AppTheme.textSecondary,
                                ),
                              ),
                              const SizedBox(height: 6),
                              PriceText(value: product.price, size: 16),
                            ],
                          ),
                        ),
                        IconButton(
                          onPressed: () => _remove(context, ref, product),
                          icon: const Icon(
                            Icons.favorite,
                            color: AppTheme.price,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  Future<void> _logout(BuildContext context, WidgetRef ref) async {
    final confirmed = await showConfirmDialog(
      context,
      title: '退出登录',
      content: '确定退出当前账号吗？',
      confirmText: '退出',
    );
    if (!confirmed || !context.mounted) {
      return;
    }
    await ref.read(authProvider.notifier).logout();
    if (context.mounted) {
      context.go('/login');
    }
  }

  void _showPushInfo(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('消息推送'),
        content: const Text(
          '当前版本未集成厂商推送 SDK，仅保留 PushService 接口与配置占位。\n\n'
          '正式接入需在 Android/iOS 工程中配置极光 JPush 或个推 Getui 的 AppKey，'
          '并在登录成功后上报设备 registrationId 到后端。',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('知道了'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);
    final session = auth.session;
    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 28),
        children: [
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(
                    Icons.person_outline,
                    color: AppTheme.primary,
                  ),
                  title: const Text('当前账号'),
                  subtitle: Text(
                    session == null
                        ? '未登录'
                        : '${session.nickname} · ${session.tenantCode}',
                  ),
                ),
                const Divider(indent: 16, endIndent: 16),
                ListTile(
                  leading: const Icon(
                    Icons.cloud_outlined,
                    color: AppTheme.primary,
                  ),
                  title: const Text('API 地址'),
                  subtitle: SelectableText(AppConfig.apiBaseUrl),
                ),
                const Divider(indent: 16, endIndent: 16),
                ListTile(
                  leading: const Icon(
                    Icons.notifications_none,
                    color: AppTheme.primary,
                  ),
                  title: const Text('消息推送'),
                  subtitle: const Text('占位配置，待接入极光/个推'),
                  trailing: const Icon(Icons.chevron_right, size: 20),
                  onTap: () => _showPushInfo(context),
                ),
                const Divider(indent: 16, endIndent: 16),
                const ListTile(
                  leading: Icon(
                    Icons.info_outline,
                    color: AppTheme.primary,
                  ),
                  title: Text('关于悦享便利'),
                  subtitle: Text('版本 1.0.0 · 用户端'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          if (session != null)
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: AppTheme.price,
              ),
              onPressed: () => _logout(context, ref),
              child: const Text('退出登录'),
            )
          else
            FilledButton(
              onPressed: () => context.push('/login'),
              child: const Text('去登录'),
            ),
          const SizedBox(height: 16),
          const Text(
            '用户协议与隐私政策以上架时法务确认版本为准。',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 11, color: AppTheme.textSecondary),
          ),
        ],
      ),
    );
  }
}
