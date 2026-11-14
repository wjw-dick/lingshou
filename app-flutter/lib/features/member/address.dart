import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme.dart';
import '../../core/widgets.dart';
import '../../data/api.dart';
import '../../data/models.dart';
import '../auth/auth.dart';

class AddressListState {
  const AddressListState({
    this.addresses = const [],
    this.loading = false,
    this.error,
  });

  final List<MemberAddress> addresses;
  final bool loading;
  final String? error;
}

class AddressController extends StateNotifier<AddressListState> {
  AddressController(this._api) : super(const AddressListState(loading: true)) {
    Future.microtask(() => load());
  }

  final AppApi _api;

  Future<void> load() async {
    if (!mounted) {
      return;
    }
    state = AddressListState(addresses: state.addresses, loading: true);
    try {
      final addresses = await _api.addresses();
      if (!mounted) {
        return;
      }
      state = AddressListState(addresses: addresses);
    } catch (error) {
      if (!mounted) {
        return;
      }
      state = AddressListState(
        addresses: state.addresses,
        error: error.toString(),
      );
    }
  }

  Future<void> create({
    required String name,
    required String phone,
    required String address,
    required bool isDefault,
  }) async {
    await _api.createAddress(
      MemberAddress(
        id: 0,
        name: name,
        phone: phone,
        address: address,
        isDefault: isDefault,
        lng: 0,
        lat: 0,
      ),
    );
    await load();
  }

  Future<void> update(MemberAddress address) async {
    await _api.updateAddress(address);
    await load();
  }

  Future<void> remove(int id) async {
    await _api.deleteAddress(id);
    await load();
  }
}

final addressListProvider =
    StateNotifierProvider.autoDispose<AddressController, AddressListState>((ref) {
  return AddressController(ref.watch(appApiProvider));
});

class AddressPage extends ConsumerWidget {
  const AddressPage({super.key});

  Future<void> _openForm(
    BuildContext context,
    WidgetRef ref, {
    MemberAddress? existing,
  }) async {
    final state = ref.read(addressListProvider);
    final result = await showDialog<_AddressFormData>(
      context: context,
      builder: (_) => _AddressFormDialog(
        existing: existing,
        defaultForNew: state.addresses.isEmpty,
      ),
    );
    if (result == null || !context.mounted) {
      return;
    }
    try {
      if (existing == null) {
        await ref.read(addressListProvider.notifier).create(
              name: result.name,
              phone: result.phone,
              address: result.address,
              isDefault: result.isDefault,
            );
      } else {
        await ref.read(addressListProvider.notifier).update(
              MemberAddress(
                id: existing.id,
                name: result.name,
                phone: result.phone,
                address: result.address,
                isDefault: result.isDefault,
                lng: existing.lng,
                lat: existing.lat,
              ),
            );
      }
      if (context.mounted) {
        showMessage(context, '地址已保存');
      }
    } catch (error) {
      if (context.mounted) {
        showMessage(context, error.toString());
      }
    }
  }

  Future<void> _setDefault(
    BuildContext context,
    WidgetRef ref,
    MemberAddress address,
  ) async {
    try {
      await ref.read(addressListProvider.notifier).update(
            MemberAddress(
              id: address.id,
              name: address.name,
              phone: address.phone,
              address: address.address,
              isDefault: true,
              lng: address.lng,
              lat: address.lat,
            ),
          );
      if (context.mounted) {
        showMessage(context, '已设为默认地址');
      }
    } catch (error) {
      if (context.mounted) {
        showMessage(context, error.toString());
      }
    }
  }

  Future<void> _delete(
    BuildContext context,
    WidgetRef ref,
    MemberAddress address,
  ) async {
    final confirmed = await showConfirmDialog(
      context,
      title: '删除地址',
      content: '确定删除该收货地址吗？',
      confirmText: '删除',
    );
    if (!confirmed || !context.mounted) {
      return;
    }
    try {
      await ref.read(addressListProvider.notifier).remove(address.id);
      if (context.mounted) {
        showMessage(context, '地址已删除');
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
        appBar: AppBar(title: const Text('收货地址')),
        body: LoginRequiredView(onLogin: () => context.push('/login')),
      );
    }
    final state = ref.watch(addressListProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('收货地址'),
        actions: [
          TextButton(
            onPressed: () => _openForm(context, ref),
            child: const Text('新增'),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: StateView(
        loading: state.loading,
        error: state.error,
        onRetry: () => ref.read(addressListProvider.notifier).load(),
        empty: false,
        child: state.addresses.isEmpty
            ? EmptyView(
                text: '还没有收货地址',
                icon: Icons.location_off_outlined,
                actionText: '新增地址',
                onAction: () => _openForm(context, ref),
              )
            : RefreshIndicator(
                onRefresh: () => ref.read(addressListProvider.notifier).load(),
                child: ListView.separated(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                  itemCount: state.addresses.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    final address = state.addresses[index];
                    return _AddressCard(
                      address: address,
                      onEdit: () => _openForm(context, ref, existing: address),
                      onDelete: () => _delete(context, ref, address),
                      onSetDefault: () => _setDefault(context, ref, address),
                    );
                  },
                ),
              ),
      ),
    );
  }
}

class _AddressCard extends StatelessWidget {
  const _AddressCard({
    required this.address,
    required this.onEdit,
    required this.onDelete,
    required this.onSetDefault,
  });

  final MemberAddress address;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final VoidCallback onSetDefault;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                address.name,
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(width: 8),
              Text(
                address.phone,
                style: const TextStyle(
                  fontSize: 13,
                  color: AppTheme.textSecondary,
                ),
              ),
              const Spacer(),
              if (address.isDefault)
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                  decoration: BoxDecoration(
                    color: AppTheme.primary.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(5),
                  ),
                  child: const Text(
                    '默认',
                    style: TextStyle(
                      fontSize: 10,
                      color: AppTheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            address.address,
            style: const TextStyle(fontSize: 13, height: 1.5),
          ),
          const SizedBox(height: 6),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              if (!address.isDefault)
                TextButton(
                  onPressed: onSetDefault,
                  child: const Text('设为默认'),
                ),
              TextButton(
                onPressed: onEdit,
                child: const Text('编辑'),
              ),
              TextButton(
                onPressed: onDelete,
                style: TextButton.styleFrom(foregroundColor: AppTheme.price),
                child: const Text('删除'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _AddressFormData {
  const _AddressFormData({
    required this.name,
    required this.phone,
    required this.address,
    required this.isDefault,
  });

  final String name;
  final String phone;
  final String address;
  final bool isDefault;
}

class _AddressFormDialog extends StatefulWidget {
  const _AddressFormDialog({
    this.existing,
    required this.defaultForNew,
  });

  final MemberAddress? existing;
  final bool defaultForNew;

  @override
  State<_AddressFormDialog> createState() => _AddressFormDialogState();
}

class _AddressFormDialogState extends State<_AddressFormDialog> {
  late final TextEditingController _nameController;
  late final TextEditingController _phoneController;
  late final TextEditingController _addressController;
  late bool _isDefault;

  @override
  void initState() {
    super.initState();
    final existing = widget.existing;
    _nameController = TextEditingController(text: existing?.name ?? '');
    _phoneController = TextEditingController(text: existing?.phone ?? '');
    _addressController = TextEditingController(text: existing?.address ?? '');
    _isDefault = existing?.isDefault ?? widget.defaultForNew;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _addressController.dispose();
    super.dispose();
  }

  void _save() {
    final name = _nameController.text.trim();
    final phone = _phoneController.text.trim();
    final address = _addressController.text.trim();
    if (name.isEmpty || phone.isEmpty || address.isEmpty) {
      showMessage(context, '请填写完整的地址信息');
      return;
    }
    if (phone.length < 6) {
      showMessage(context, '请填写正确的联系电话');
      return;
    }
    Navigator.of(context).pop(
      _AddressFormData(
        name: name,
        phone: phone,
        address: address,
        isDefault: _isDefault,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.existing == null ? '新增地址' : '编辑地址'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: '收货人'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _phoneController,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: '手机号'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _addressController,
              maxLines: 2,
              decoration: const InputDecoration(labelText: '详细地址'),
            ),
            const SizedBox(height: 6),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('设为默认地址'),
              value: _isDefault,
              onChanged: (value) => setState(() => _isDefault = value),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('取消'),
        ),
        FilledButton(onPressed: _save, child: const Text('保存')),
      ],
    );
  }
}
