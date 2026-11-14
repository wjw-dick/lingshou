import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'config.dart';
import 'storage.dart';

final apiProvider = Provider<ApiClient>((ref) {
  return ApiClient(storage: ref.watch(storageProvider));
});

class ApiException implements Exception {
  const ApiException(this.message, {this.code});

  final String message;
  final int? code;

  @override
  String toString() => message;
}

/// Dio 封装：统一注入 Token、解包 `{code, message, data}`、转换中文错误提示。
class ApiClient {
  ApiClient({
    required StorageService storage,
    String baseUrl = AppConfig.apiBaseUrl,
  })  : _storage = storage,
        _dio = Dio(
          BaseOptions(
            baseUrl: baseUrl,
            connectTimeout: const Duration(seconds: 15),
            receiveTimeout: const Duration(seconds: 20),
            headers: const {'Content-Type': 'application/json'},
          ),
        ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final needsAuth = options.extra['auth'] != false;
          final token = _storage.token;
          if (needsAuth && token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
      ),
    );
  }

  final StorageService _storage;
  final Dio _dio;

  /// 401 时由 AuthController 注册，用于清理登录态并跳转登录页。
  void Function()? onUnauthorized;

  Future<dynamic> get(
    String path, {
    Map<String, dynamic>? query,
    bool auth = true,
  }) {
    return _request('GET', path, query: query, auth: auth);
  }

  Future<dynamic> post(
    String path, {
    Object? data,
    Map<String, dynamic>? query,
    bool auth = true,
  }) {
    return _request('POST', path, data: data, query: query, auth: auth);
  }

  Future<dynamic> put(
    String path, {
    Object? data,
    Map<String, dynamic>? query,
    bool auth = true,
  }) {
    return _request('PUT', path, data: data, query: query, auth: auth);
  }

  Future<dynamic> delete(
    String path, {
    Map<String, dynamic>? query,
    bool auth = true,
  }) {
    return _request('DELETE', path, query: query, auth: auth);
  }

  Future<dynamic> _request(
    String method,
    String path, {
    Object? data,
    Map<String, dynamic>? query,
    required bool auth,
  }) async {
    try {
      final response = await _dio.request<dynamic>(
        path,
        data: data,
        queryParameters: _clean(query),
        options: Options(method: method, extra: {'auth': auth}),
      );
      return _unwrap(response.data);
    } on ApiException {
      rethrow;
    } on DioException catch (error) {
      final statusCode = error.response?.statusCode;
      final body = error.response?.data;
      var message = _fallbackMessage(error);
      if (body is Map) {
        final map = Map<String, dynamic>.from(body);
        final serverMessage = map['message']?.toString();
        if (serverMessage != null && serverMessage.isNotEmpty) {
          message = serverMessage;
        }
      }
      if (statusCode == 401) {
        await _storage.clearSession();
        onUnauthorized?.call();
      }
      throw ApiException(message, code: statusCode);
    } catch (error) {
      throw ApiException('请求失败：$error');
    }
  }

  dynamic _unwrap(dynamic body) {
    if (body is Map) {
      final map = Map<String, dynamic>.from(body);
      if (!map.containsKey('code')) {
        return map;
      }
      final code = map['code'];
      if (code == 0) {
        return map['data'];
      }
      final message = map['message']?.toString();
      throw ApiException(
        message == null || message.isEmpty ? '请求处理失败' : message,
        code: code is int ? code : null,
      );
    }
    return body;
  }

  Map<String, dynamic>? _clean(Map<String, dynamic>? params) {
    if (params == null) {
      return null;
    }
    final result = <String, dynamic>{};
    params.forEach((key, value) {
      if (value != null && value != '') {
        result[key] = value;
      }
    });
    return result;
  }

  String _fallbackMessage(DioException error) {
    if (error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.sendTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return '请求超时，请稍后重试';
    }
    if (error.response == null) {
      return '网络连接失败，请检查后端服务是否启动';
    }
    return '请求失败（HTTP ${error.response?.statusCode}）';
  }
}
