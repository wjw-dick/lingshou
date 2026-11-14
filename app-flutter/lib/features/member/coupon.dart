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
import 'member.dart';

final couponCenterProvider = FutureProvider.autoDispose<CouponCenter>((ref) async {
  final token = ref.watch(authProvider.select((state) => state.session?.token));
  if (token == null) {
    throw const ApiException('请先登录');
  }
  return ref.watch(appApiProvider).coupons();
});

class CouponPage extends ConsumerStatefulWidget {
  const CouponPage({super.key});

  @override
  ConsumerState<CouponPage> createState() => _CouponPageState();
}

class _CouponPageState extends ConsumerState<CouponPage> {
  int _claimingId = 0;

  Future<void> _claim(int couponId) async {
    if (_claimingId != 0) {
      return;
    }
    setState(() => _claimingId = couponId);
    try {
      await ref.read(appApiProvider).claimCoupon(couponId);
      ref.invalidate(couponCenterProvider);
      ref.invalidate(profileProvider);
      if (mounted) {
        showMessage(context, '领取成功');
      }
    } catch (error) {
      if (mounted) {
        showMessage(context, error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _claimingId = 0);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authProvider);
    if (!auth.isLoggedIn) {
      return Scaffold(
        appBar: AppBar(title: const Text('优惠券')),
        body: LoginRequiredView(onLogin: () => context.push('/login')),
      );
    }
    final couponsAsync = ref.watch(couponCenterProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('优惠券')),
      body: couponsAsync.when(
        loading: () => const LoadingView(),
        error: (error, stackTrace) => ErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(couponCenterProvider),
        ),
        data: (center) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(couponCenterProvider);
            try {
              await ref.read(couponCenterProvider.future);
            } catch (_) {
              // 错误由 AsyncValue 展示。
            }
          },
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
            children: [
              const SectionHeader(
                title: '我的优惠券',
                subtitle: '未使用的券可在结算时抵扣',
              ),
              const SizedBox(height: 8),
              if (center.myCoupons.isEmpty)
                const _CouponEmpty(text: '暂未领取优惠券')
              else
                ...center.myCoupons.map(
                  (row) => Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _MyCouponCard(row: row),
                  ),
                ),
              const SizedBox(height: 18),
              const SectionHeader(
                title: '可领取优惠券',
                subtitle: '先领券，再下单更划算',
              ),
              const SizedBox(height: 8),
              if (center.available.isEmpty)
                const _CouponEmpty(text: '暂无可领取优惠券')
              else
                ...center.available.map(
                  (row) => Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _AvailableCouponCard(
                      row: row,
                      claiming: _claimingId == row.coupon.id,
                      onClaim: () => _claim(row.coupon.id),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CouponEmpty extends StatelessWidget {
  const _CouponEmpty({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 28),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Text(
        text,
        style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
      ),
    );
  }
}

class _CouponShell extends StatelessWidget {
  const _CouponShell({
    required this.coupon,
    required this.active,
    required this.trailing,
  });

  final Coupon coupon;
  final bool active;
  final Widget trailing;

  @override
  Widget build(BuildContext context) {
    final color = active ? AppTheme.primary : const Color(0xFF9AA69F);
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Container(
            width: 96,
            padding: const EdgeInsets.symmetric(vertical: 20),
            decoration: BoxDecoration(
              color: color.withOpacity(0.08),
              borderRadius: const BorderRadius.horizontal(
                left: Radius.circular(14),
              ),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  couponValueText(
                    type: coupon.type,
                    discountAmount: coupon.discountAmount,
                    discountRate: coupon.discountRate,
                  ),
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    color: color,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  couponThresholdText(coupon.thresholdAmount),
                  style: TextStyle(fontSize: 10, color: color),
                ),
              ],
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    coupon.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 5),
                  Text(
                    '${formatDay(coupon.validFrom)} 至 ${formatDay(coupon.validTo)}',
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    coupon.type == 'DISCOUNT' ? '折扣券' : '满减券',
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: trailing,
          ),
        ],
      ),
    );
  }
}

class _MyCouponCard extends StatelessWidget {
  const _MyCouponCard({required this.row});

  final MemberCouponRow row;

  @override
  Widget build(BuildContext context) {
    final active = row.memberCoupon.status == 'UNUSED';
    return _CouponShell(
      coupon: row.coupon,
      active: active,
      trailing: Text(
        active ? '可使用' : '已使用',
        style: TextStyle(
          fontSize: 12,
          color: active ? AppTheme.primary : AppTheme.textSecondary,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class _AvailableCouponCard extends StatelessWidget {
  const _AvailableCouponCard({
    required this.row,
    required this.claiming,
    required this.onClaim,
  });

  final AvailableCouponRow row;
  final bool claiming;
  final VoidCallback onClaim;

  @override
  Widget build(BuildContext context) {
    return _CouponShell(
      coupon: row.coupon,
      active: row.claimable,
      trailing: row.claimable
          ? FilledButton(
              onPressed: claiming ? null : onClaim,
              style: FilledButton.styleFrom(
                minimumSize: const Size(64, 34),
                padding: const EdgeInsets.symmetric(horizontal: 12),
              ),
              child: claiming
                  ? const SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text('领取'),
            )
          : Text(
              row.owned > 0 ? '已领取' : '不可领取',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textSecondary,
              ),
            ),
    );
  }
}
