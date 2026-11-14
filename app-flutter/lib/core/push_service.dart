import 'package:flutter/foundation.dart';

/// 推送配置占位。当前工程不引入极光 / 个推等私有 SDK，避免无法编译。
///
/// 正式接入步骤示例：
/// 1. 在 pubspec.yaml 引入官方 Flutter 插件（如 jpush_flutter / getuiflut）；
/// 2. 在 Android 的 build.gradle 配置 JPUSH_APPKEY / 个推 AppID，在 iOS 开启
///    Push Notifications 与 Background Modes(remote-notification)；
/// 3. 在用户登录成功后调用 [PushService.bindMember] 上报 registrationId；
/// 4. 后端新增设备绑定接口，并在下单/支付/履约节点按 registrationId 推送。
class PushConfig {
  const PushConfig._();

  static const bool enabled = false;
  static const String appKey =
      String.fromEnvironment('PUSH_APP_KEY', defaultValue: '');
  static const String channel = 'default';
}

abstract interface class PushService {
  Future<void> initialize();

  Future<String?> getRegistrationId();

  Future<void> bindMember(int memberId);

  Future<void> unbindMember();
}

class PlaceholderPushService implements PushService {
  const PlaceholderPushService();

  @override
  Future<void> initialize() async {
    debugPrint('[Push] 占位实现：请接入极光或个推官方 Flutter 插件。');
  }

  @override
  Future<String?> getRegistrationId() async => null;

  @override
  Future<void> bindMember(int memberId) async {
    debugPrint('[Push] 占位：登录会员 $memberId 后应上报设备标识。');
  }

  @override
  Future<void> unbindMember() async {
    debugPrint('[Push] 占位：退出登录时应解绑设备。');
  }
}
