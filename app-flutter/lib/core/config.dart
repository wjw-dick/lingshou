/// 全局配置：后端地址、演示租户与演示会员。
///
/// 修改 API 地址的两种方式：
/// 1. 直接修改 [apiBaseUrl] 的 defaultValue；
/// 2. 运行/打包时传入：`--dart-define=API_BASE_URL=http://192.168.1.10:8080/api/v1`
class AppConfig {
  const AppConfig._();

  static const String appName = '悦享便利';
  static const String tenantCode = 'yxbl';
  static const String demoOpenid = 'oUx1demo001';
  static const String demoNickname = '微信用户';

  /// 后端 API 根地址。Android 模拟器访问宿主机请改用 10.0.2.2。
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://127.0.0.1:8080/api/v1',
  );

  /// 图片等静态资源所在的服务根地址（去掉 /api/v1）。
  static String get imageOrigin {
    final uri = Uri.tryParse(apiBaseUrl);
    if (uri == null || !uri.hasScheme || uri.host.isEmpty) {
      return '';
    }
    final port = uri.hasPort ? ':${uri.port}' : '';
    return '${uri.scheme}://${uri.host}$port';
  }

  /// 兼容后端返回相对路径（如 /uploads/xxx.jpg）与完整 URL。
  static String resolveImage(String? path) {
    if (path == null || path.trim().isEmpty) {
      return '';
    }
    final value = path.trim();
    if (value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }
    if (value.startsWith('//')) {
      return 'https:$value';
    }
    final prefix = value.startsWith('/') ? '' : '/';
    return '$imageOrigin$prefix$value';
  }
}
