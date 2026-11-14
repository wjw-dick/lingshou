# 零售便利连锁 SaaS 平台

面向社区便利店/便利连锁的多租户 SaaS 平台，覆盖平台运营端、租户连锁总部后台、PC 品牌官网、H5、4 个微信原生小程序与 Flutter App 源码，打通商品、订货、收货、库存、收银、会员、线上商城、供应商与加盟结算。

## 工程结构

| 目录 | 说明 |
|---|---|
| `backend/` | Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8 + Redis；平台/租户双端 API、租户插件隔离、RBAC、审计、定时任务 |
| `web-admin/` | Vue 3 + Vite + TS + Element Plus；平台运营端 + 租户后台（Vue Router / Pinia / ECharts / WangEditor） |
| `web-portal/` | Vue 3 + Vite；PC 品牌官网 + H5 分享页 |
| `mp-user/` | 微信原生小程序：会员、找店、商品、下单、订单、优惠券 |
| `mp-store/` | 微信原生小程序：店长/店员工作台、订货、收货、库存、交接班 |
| `mp-supplier/` | 微信原生小程序：供货单、对账、结算、资质 |
| `mp-franchisee/` | 微信原生小程序：报单、分账、额度、门店 |
| `app-flutter/` | Flutter 用户端源码（Riverpod + Dio + Hive），本机无 Flutter SDK，未编译 |
| `tests/api_regression.py` | 多租户隔离、平台/租户越权、停用、POS 收银、线上订单支付/退款、烟草线上禁售、审计回归 |
| `docker/` `docker-compose.yml` | MySQL/Redis/RabbitMQ/MinIO/后端/双前端/网关编排 |
| `docs/` | 架构、隔离、API、运维、验收证据 |
| `build/` | 交付文档生成脚本与开发辅助脚本（非运行必需） |

## 本地启动

### 1. 基础依赖
- JDK 17（当前机器 `C:\Program Files\RedHat\java-17-openjdk-17.0.19.0.10-1`）
- Maven 3.8+
- Node.js 20+
- MySQL 8、Redis（当前开发环境使用本机 MySQL `3306`，库 `retail_saas`，账号 `retail / Retail@2026`；Redis 默认读取环境变量）

### 2. 后端

```powershell
$env:JAVA_HOME='C:\Program Files\RedHat\java-17-openjdk-17.0.19.0.10-1'
cd backend
mvn spring-boot:run
```

默认端口 `8080`。首次启动会自动执行 `backend/src/main/resources/db/schema.sql` 与 `data.sql`（幂等），初始化 92 张表、平台/租户演示数据与演示账号。

数据库/Redis 可用环境变量覆盖：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DB`、`SERVER_PORT`、`JWT_SECRET`。本机 Redis 若启用了密码，请先设置 `REDIS_PASSWORD` 再启动后端。

### 3. 管理后台

```powershell
cd web-admin
npm install
npm run dev
```

访问 `http://localhost:5173`，Vite 已代理 `/api` 与 `/uploads` 到 `http://127.0.0.1:8080`。

### 4. 品牌官网 / H5

```powershell
cd web-portal
npm install
npm run dev
```

访问 `http://localhost:5174`，H5 路由为 `/h5/*`。

### 5. 微信小程序

用微信开发者工具分别导入 `mp-user`、`mp-store`、`mp-supplier`、`mp-franchisee` 四个目录，AppID 可按实际替换。接口地址在各自 `utils/config.js` 中配置。

### 6. Flutter App

`app-flutter/` 为源码工程，需要 Flutter 3.x 环境执行 `flutter pub get`、`flutter analyze`、`flutter build apk`。当前机器未安装 Flutter SDK，未编译验证。

## 演示账号

| 入口 | 租户编码 | 账号 | 密码 | 角色 |
|---|---|---|---|---|
| 平台运营端 | - | admin | Admin@123 | 平台超管 |
| 平台运营端 | - | operator / finance | Admin@123 | 平台运营 / 平台财务 |
| 租户后台 | yxbl | admin | Admin@123 | 租户管理员（悦享便利） |
| 租户后台 | yxbl | dianzhang | Admin@123 | 店长（静安旗舰店） |
| 租户后台 | yxbl | shouyin | Admin@123 | 收银员 |
| 租户后台 | yxbl | gys01 | Admin@123 | 供应商（上海鲜食供应链） |
| 租户后台 | yxbl | jm01 | Admin@123 | 加盟商（张明） |
| 租户后台 | xgsg | admin | Admin@123 | 另一个隔离租户（鲜果时光） |

Node 依赖已安装。若需要重置演示数据：清空 `retail_saas` 库后重启后端，SQL 初始化会自动重建。

## 回归测试

```powershell
python tests/api_regression.py
```

覆盖 24 项：租户 2 只能看到本租户门店、跨租户直连资源 ID 被拒、平台/租户接口互斥、租户停用后登录与旧 Token 失效、停用恢复、收银库存扣减、线上下单-储值支付-接单-拣货-完成-退款回库、烟草线上禁售、审计落库、公众号回调验签。

## 已验证

- 后端 66 个接口冒烟全部通过（登录、平台端、租户业务、公众号本地管理、CMS、官网公开接口、App、门店端、供应商端、加盟商端）。
- 24 项隔离/交易回归全部通过（含公众号回调验签）。
- 管理后台（租户工作台、门店、商品、采购/供货/库存、POS 加购与应收、公众号账号、对象存储、菜单可视化、平台总览/租户/套餐/账单）已在浏览器实测，无控制台错误。
- 品牌官网（首页、门店地图、商品、加盟表单）与 H5（限时折扣、新人礼）已在浏览器实测。
- 4 个小程序共 46 个 JSON、51 个 JS 通过解析/`node --check` 校验（未在微信开发者工具中运行）。
- 管理后台 `vue-tsc --noEmit` 通过；品牌官网 `npm run build` 通过。

## 未验证 / 外部依赖阻塞

- 微信公众号真实凭证、模板消息、素材/草稿发表、菜单发布、粉丝同步：需已认证服务号 AppID/AppSecret/IP 白名单；未配置时接口返回明确错误，不做假成功。
- 微信支付/支付宝：线上支付当前为模拟支付（`/app/orders/{id}/pay`），需商户号、证书、回调域名后接入真实网关。
- 短信 6 渠道：仅本地日志渠道可用；其余渠道完成配置校验但不发起真实外呼。
- 对象存储：本地磁盘、MinIO/S3、阿里云 OSS、腾讯云 COS 均已接入驱动；除本地外需客户 AK/SK/Bucket 联调，未验证真实上传。
- RabbitMQ：Compose 已编排，应用内消息通道以事件/同步方式实现，未做生产级 MQ 生产/消费验证。
- Docker Compose：当前机器未安装 Docker，编排文件未实际构建运行。
- Flutter App：无 Flutter SDK，未编译/上架。
- 域名、HTTPS、ICP 备案、食品经营许可/烟草专卖许可等资质需客户侧提供。

详细配置与验收证据见 `docs/`。
