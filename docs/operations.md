# 运维手册

## 1. 部署方式

### 1.1 Docker Compose（推荐生产/预发）

```bash
cp .env.example .env
docker compose up -d --build
```

访问：

| 入口 | 地址 |
|---|---|
| 品牌官网 / H5 | `http://服务器:8088/`、`/h5/deals` |
| 管理后台 | `http://服务器:8088/admin/` |
| 后端 API | `http://服务器:8088/api/v1` |
| Swagger | `http://服务器:8088/swagger-ui.html` |
| MinIO 控制台 | `http://服务器:9001` |

服务清单：mysql8、redis、rabbitmq、minio、backend、web-admin、web-portal、gateway(nginx)。

- MySQL 初始化脚本 `docker/mysql-init/01-database.sql` 自动建库授权；后端启动时自动执行 `schema.sql` + `data.sql`。
- 上传文件挂载 `upload-data`；数据库/Redis/MinIO/RabbitMQ 均有独立数据卷。
- 生产必须修改 `.env` 中所有密码与 `JWT_SECRET`，并在网关前置 HTTPS。

### 1.2 本地开发（当前验证环境）

- MySQL 独立实例：`127.0.0.1:3306`，库 `retail_saas`，账号 `retail / Retail@2026`。
- Redis：`127.0.0.1:6379`，通过 `REDIS_PASSWORD` 注入。
- 后端：`mvn spring-boot:run`（JDK 17），端口 `8080`。
- 管理后台：`npm run dev`，端口 `5173`。
- 官网/H5：`npm run dev`，端口 `5174`。

## 2. 关键配置

| 变量 | 默认 | 说明 |
|---|---|---|
| `SERVER_PORT` | 8080 | 后端端口 |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/retail_saas...` | 数据库连接 |
| `DB_USERNAME` / `DB_PASSWORD` | retail / Retail@2026 | 数据库账号 |
| `REDIS_HOST/PORT/PASSWORD/DB` | 127.0.0.1/6379/见 yml/3 | 缓存 |
| `JWT_SECRET` | 内置开发密钥 | 生产必须替换；同时用于敏感配置 AES 加密 |
| `STORAGE_ROOT` | ./data/uploads | 本地存储根路径 |

业务参数全部在租户后台「系统配置 → 全局参数」维护：站点、ICP、客服、SEO、支付、配送规则、库存阈值、临期天数、分账比例、i18n。保存实时生效，密钥字段加密存储且页面不明码展示。

## 3. 微信与支付接入

### 公众号
1. 后台「公众号管理 → 账号管理」填写已认证服务号 AppID/AppSecret/Token/AESKey；服务器 URL 自动生成 `{host}/admin-api/mp/open/{appId}`。
2. 微信公众平台配置同一 URL、Token、消息加解密方式选「明文模式」（当前版本不支持安全模式）、IP 白名单、网页授权域名、JS 安全域名。
3. 先执行「接号占用检查」并确认结论；如切换，按「停旧回调 → 改 URL → 校验 Token → 重新发布菜单」四步执行，前后做粉丝与菜单快照。
4. 模板消息模板 ID 在公众平台申请后，在「消息配置」绑定触发节点。

### 微信支付 / 支付宝
- 当前线上支付为模拟支付接口 `/app/orders/{id}/pay`，用于打通订单状态机。
- 上线前需商户号、API 证书、回调域名，替换 `SalesOrderService.pay` 为统一下单 + 回调验签 + 主动查单补偿；支付宝同理。
- 储值涉及资金，建议第三方支付托管或单独资金台账；平台不做资金二清。

### 短信
- 「消息配置 → 短信渠道」6 选 1，同租户只启用一个。当前仅 `LOCAL` 本地日志渠道可用；其他渠道保存配置时校验格式并标注未发起真实外呼。

### 对象存储
- 支持本地磁盘、MinIO/S3、阿里云 OSS、腾讯云 COS 四种驱动（页面 5 个入口）。
- 本地磁盘可直接使用；MinIO/S3、OSS、COS 需填写 endpoint、AK/SK、Bucket、region 等，先「连接测试」通过再保存。
- 外网驱动需服务器可访问对应 endpoint，Bucket 权限与跨域按厂商要求配置。

## 4. 备份与恢复

- MySQL：每日 02:00 全量，保留 7 份，每周异地副本。
  `mysqldump -h127.0.0.1 -P3306 -uretail -p retail_saas > backup_$(date +%F).sql`
- Redis：开启 AOF 或每日 RDB 备份。
- 对象存储：本地磁盘目录或 MinIO 桶做快照/跨区域复制。
- 恢复演练：在隔离库导入备份 → 启动后端 → 跑 `tests/api_regression.py` 与关键页面冒烟。

## 5. 监控与告警

- 健康检查：`GET /actuator/health`（当前未引入 actuator 依赖时可直接请求 `/api/v1/public/tenants` 判断进程存活）。
- 日志：`backend/logs/retail-convenience.log`，Docker 下用 `docker logs` 或集中采集。
- 关键指标：接口 5xx、登录失败、库存扣减失败、支付回调异常、定时任务异常、磁盘/数据库连接池。
- 单机部署需配进程守护（systemd/docker restart）、磁盘告警与备份告警。

## 6. 常见故障

| 现象 | 排查 |
|---|---|
| 启动报 3306 连接失败 | 检查 MySQL 端口、库、账号；`DB_URL` 是否覆盖 |
| 登录 401 | Token 过期/Redis 会话被清；重新登录 |
| 登录 403 | 租户停用/到期/欠费；平台端恢复或续期 |
| 接口 403 但角色有权限 | 角色权限缓存 5 分钟；可在平台/租户后台保存角色后重登 |
| 上传失败 | 检查存储驱动配置、Bucket、AK/SK；OSS/COS 当前未集成 |
| 公众号同步失败 | AppSecret/IP 白名单/接口权限；错误会原样展示 errcode/errmsg |
| 下单提示库存不足 | 检查 `stock.qty - locked_qty`、预占是否未释放、超时任务是否执行 |
| 定时任务未执行 | 检查应用是否单实例多副本重复执行；当前按单实例设计 |

## 7. 发布与回滚

1. 后端 `mvn -DskipTests package` 产出 jar，镜像构建后滚动替换；数据库变更走 `schema.sql` 的幂等 DDL 或新增迁移脚本。
2. 前端 `npm run build` 产出静态文件；管理后台构建需 `VITE_BASE=/admin/`。
3. 回滚：保留上一版本镜像与 jar；数据库变更必须向前兼容（先加列、后切代码）。
4. 上线前执行 `tests/api_regression.py`、管理后台/官网/小程序关键路径冒烟、备份恢复演练。
