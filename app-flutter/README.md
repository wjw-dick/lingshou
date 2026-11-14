# 悦享便利用户端 Flutter App

零售便利连锁 SaaS 平台用户端（Android / iOS），功能与 `mp-user` 微信小程序用户端保持一致，统一对接后端：

```
http://127.0.0.1:8080/api/v1
```

> **重要说明：当前环境无 Flutter SDK，未执行 `flutter pub get`、`flutter analyze`、`flutter build`，代码尚未经过编译与真机验证。请在具备 Flutter 3.x 的环境完成验证。**

## 技术栈

- Flutter 3.x（Dart 3）
- Riverpod 2（状态管理）
- Dio 5（网络请求，统一 Token、错误解包）
- Hive + hive_flutter（登录态、购物车、当前门店本地持久化）
- go_router 14（声明式路由、底部导航、StatefulShellRoute）
- Material 3，中文界面

## 运行方式

```bash
cd app-flutter
flutter pub get
flutter analyze
flutter run
```

如果当前目录缺少 Android/iOS 宿主工程（本仓库只提交 Dart 源码与配置），先执行：

```bash
flutter create . --platforms=android,ios
```

再根据实际包名修改 `applicationId` / `PRODUCT_BUNDLE_IDENTIFIER` 后运行。

### API 地址修改

默认地址在 `lib/core/config.dart`：

```dart
static const String apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://127.0.0.1:8080/api/v1',
);
```

也可以不改代码，通过编译参数覆盖：

```bash
# Android 模拟器访问宿主机
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1

# 真机访问局域网后端
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api/v1
```

注意：

- Android 9+ 默认禁止明文 HTTP，本地联调需在 `AndroidManifest.xml` 的 `application` 节点配置
  `android:usesCleartextTraffic="true"`，并确认已声明 `INTERNET` 权限；正式环境请使用 HTTPS。
- iOS 需在 `Info.plist` 配置 ATS 例外（`NSAppTransportSecurity`）才能访问本地 HTTP；上架版本应使用 HTTPS。
- 图片地址由 `AppConfig.resolveImage` 自动拼接服务根地址，兼容 `/uploads/...` 相对路径。

### 演示登录

登录页已预填演示账号，直接点击登录：

```json
{
  "tenantCode": "yxbl",
  "openid": "oUx1demo001",
  "nickname": "微信用户"
}
```

真实微信登录需要在 `AppApi.login` 前接入微信开放平台 SDK 获取 code，并由后端用 code 换取 openid；当前演示流程不依赖微信 SDK。

## 目录结构

```
lib/
├── main.dart                      # Hive 初始化 + ProviderScope
├── app.dart                       # MaterialApp.router、路由、底部导航
├── core/
│   ├── config.dart                # API 地址、租户、演示账号、图片地址
│   ├── http_client.dart           # Dio 封装、Token 注入、ApiException
│   ├── storage.dart               # Hive 登录态/购物车/门店
│   ├── theme.dart                 # Material 3 主题
│   ├── format.dart                # 金额、日期、订单状态格式化
│   ├── widgets.dart               # StateView/空态/错误态/商品卡/步进器
│   └── push_service.dart          # 极光/个推推送接口占位
├── data/
│   ├── models.dart                # 后端 JSON 领域模型
│   └── api.dart                   # 用户端 API 集合
└── features/
    ├── auth/auth.dart             # AuthController + 登录页
    ├── home/home.dart             # 首页（/app/home）
    ├── store/store.dart           # 门店列表/详情、当前门店
    ├── product/product.dart       # 商品列表/详情
    ├── cart/cart.dart             # 购物车、结算页
    ├── order/order.dart           # 订单列表/详情、支付/取消/评价
    ├── member/member.dart         # 会员中心、收藏、设置
    ├── member/coupon.dart         # 优惠券领取/我的券
    └── member/address.dart        # 地址 CRUD
```

按要求控制在约 20 个 Dart 文件：每个业务模块内按“Provider/Controller + 页面”组织，网络与模型集中在 `data/`，通用能力集中在 `core/`。

## 页面与路由

| 页面 | 路由 |
| --- | --- |
| 登录 | `/login` |
| 首页 | `/home` |
| 找店 | `/stores` |
| 门店详情 | `/stores/:id` |
| 商品列表 | `/products`（支持 `categoryId`、`keyword` 参数） |
| 商品详情 | `/products/:skuId` |
| 购物车 | `/cart` |
| 确认订单/结算 | `/checkout` |
| 订单列表 | `/orders` |
| 订单详情 | `/orders/:id` |
| 会员中心 | `/member` |
| 优惠券 | `/member/coupons` |
| 收货地址 | `/member/addresses` |
| 我的收藏 | `/member/favorites` |
| 设置 | `/settings` |

首页、找店、订单、我的为底部导航四个 Tab；其余为二级页面。

## 已覆盖接口

- 登录：`POST /app/auth/login`、`POST /auth/logout`
- 首页：`GET /app/home`
- 找店：`GET /public/site/yxbl/stores`、`GET /public/site/yxbl/stores/{storeId}/delivery-rule`
- 商品：`GET /public/site/yxbl/categories`、`GET /public/site/yxbl/products`、`GET /public/site/yxbl/products/{skuId}`
- 购物车：本地 Hive 持久化，不调用后端
- 下单：`POST /app/orders`
- 支付：`POST /app/orders/{id}/pay?method=WECHAT|WALLET`
- 订单：`GET /app/orders`、`GET /app/orders/{id}`、`POST /app/orders/{id}/cancel`、`POST /app/orders/{id}/evaluate`
- 会员：`GET /app/profile`
- 优惠券：`GET /app/coupons`、`POST /app/coupons/{couponId}/claim`
- 地址：`GET/POST /app/addresses`、`PUT/DELETE /app/addresses/{id}`
- 收藏：`GET /app/favorites`、`POST /app/favorites/{skuId}`

接口统一按后端 `{ code: 0, message, data }` 解包；401 会清理 Hive 登录态，页面提示重新登录。

## 推送接入（占位）

`lib/core/push_service.dart` 定义了 `PushService` 抽象与 `PlaceholderPushService`，**未引入任何私有推送 SDK**，不影响编译。

正式接入建议：

1. 选择极光 JPush 或个推 Getui 官方 Flutter 插件；
2. Android 配置 AppKey/厂商通道，iOS 开启 Push Notifications 与 Background Modes；
3. 登录成功后调用 `bindMember(memberId)`，把 registrationId 上报后端设备绑定接口；
4. 在支付成功、接单、待自提、配送等节点由后端下发推送；
5. 设置页的“消息推送”目前仅展示接入说明。

## 上架前置材料

1. **Apple 开发者账号**：Apple Developer Program（个人/公司，年费 99 美元）；准备 App 名称、Bundle ID、隐私政策 URL、App Store 截图与审核账号。
2. **软件著作权**：国内应用商店通常要求《计算机软件著作权登记证书》；提前准备源代码、说明书、申请表，预留 1-2 个月。
3. **Android 签名**：生成并妥善保管 release keystore，配置 `android/key.properties` 与 `signingConfigs`；记录 SHA-1/SHA-256，后续微信/推送/地图 SDK 需要。
4. **隐私合规**：隐私政策、用户协议、权限使用说明、个人信息收集清单；应用内需提供注销入口。
5. **备案与资质**：国内 App 备案、ICP/EDI（如涉及自营交易与配送）、食品经营相关资质展示。
6. **支付与登录**：真实微信支付、微信登录需开放平台审核与商户号配置；当前为演示 token 登录与模拟支付。
7. **应用商店素材**：图标、启动图、截图、应用描述、关键词、版本说明；iOS 需提供 6.7/6.1/5.5 英寸截图。

## 未验证事项

- 当前环境无 Flutter SDK，未执行 `flutter pub get` / `flutter analyze` / `flutter build`，未在 Android/iOS 真机或模拟器运行。
- 未生成 Android/iOS 原生宿主目录与签名配置；需 `flutter create . --platforms=android,ios` 后补齐包名、图标、启动图、权限与 ATS/明文 HTTP 配置。
- 依赖版本按 Flutter 3.x 稳定版本选型，实际以 `pub get` 解析结果为准；首次验证时如遇 API 弃用告警需按当前 Flutter 版本调整。
- 未接入真实微信登录、微信支付、极光/个推 SDK 与后端设备绑定接口。
- 后端接口字段以当前 `AppController` / `PublicMallController` 为准；后续字段变更需同步 `lib/data/models.dart`。
