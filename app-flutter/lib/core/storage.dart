import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import '../data/models.dart';

/// 由 main() 在 Hive 初始化后覆盖，其余页面只读使用。
final storageProvider = Provider<StorageService>((ref) {
  throw UnimplementedError('StorageService 需在 main() 中初始化后覆盖');
});

/// 本地存储：登录态、购物车、当前门店（Hive 中以 JSON 字符串保存）。
class StorageService {
  static const String _boxName = 'retail_app_v1';
  static const String _sessionKey = 'auth_session';
  static const String _cartKey = 'cart_items';
  static const String _storeKey = 'selected_store';

  late Box<String> _box;

  Future<void> init() async {
    await Hive.initFlutter();
    _box = await Hive.openBox<String>(_boxName);
  }

  String? read(String key) => _box.get(key);

  Future<void> write(String key, String value) => _box.put(key, value);

  Future<void> remove(String key) => _box.delete(key);

  AuthSession? get session {
    final raw = read(_sessionKey);
    if (raw == null || raw.isEmpty) {
      return null;
    }
    try {
      return AuthSession.fromJson(
        Map<String, dynamic>.from(jsonDecode(raw) as Map),
      );
    } catch (_) {
      return null;
    }
  }

  String? get token => session?.token;

  Future<void> saveSession(AuthSession session) {
    return write(_sessionKey, jsonEncode(session.toJson()));
  }

  Future<void> clearSession() => remove(_sessionKey);

  Store? get selectedStore {
    final raw = read(_storeKey);
    if (raw == null || raw.isEmpty) {
      return null;
    }
    try {
      return Store.fromJson(
        Map<String, dynamic>.from(jsonDecode(raw) as Map),
      );
    } catch (_) {
      return null;
    }
  }

  Future<void> saveSelectedStore(Store store) {
    return write(_storeKey, jsonEncode(store.toJson()));
  }

  List<CartItem> get cart {
    final raw = read(_cartKey);
    if (raw == null || raw.isEmpty) {
      return const [];
    }
    try {
      final list = jsonDecode(raw) as List<dynamic>;
      return list
          .map(
            (item) => CartItem.fromJson(
              Map<String, dynamic>.from(item as Map),
            ),
          )
          .toList();
    } catch (_) {
      return const [];
    }
  }

  Future<void> saveCart(List<CartItem> items) {
    final data = items.map((item) => item.toJson()).toList();
    return write(_cartKey, jsonEncode(data));
  }

  Future<void> clearCart() => remove(_cartKey);
}
