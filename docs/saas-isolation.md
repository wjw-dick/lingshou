# 多租户隔离说明与自测

## 1. 隔离实现位置

| 机制 | 代码位置 | 说明 |
|---|---|---|
| 租户上下文 | `backend/src/main/java/com/retail/convenience/common/security/TenantContext.java` | ThreadLocal 保存 `tenantId/tenantCode`，请求结束清理 |
| 认证与端隔离 | `common/security/AuthFilter.java` | JWT 解析、会话校验、停用/到期拦截、平台/租户/门店/供应商/加盟商端入口互斥 |
| 租户插件 | `common/config/MybatisPlusConfig.java` | `TenantLineInnerInterceptor` 对全部非 `platform_*` 表自动追加 `tenant_id` |
| 平台表白名单 | 同上 `ignoreTable` | 仅 `platform_*` 不参与隔离；租户业务表禁止加入白名单 |
| 插入自动填充 | 同上 `MetaObjectHandler` | 新实体 tenantId 为空时写入当前租户 |
| 缓存前缀 | 权限缓存 `tenant:perm:{tenantId}:{role}`，租户状态 `tenant:guard:{tenantId}`，序号 `seq:{prefix}:{day}` | 业务缓存统一带租户前缀 |
| 存储前缀 | `modules/system/service/StorageService.java` | 对象路径 `tenants/{tenantId}/{bizType}/{yyyyMM}/...` |
| 平台聚合读取 | `modules/platform/service/PlatformQuotaService.java` | 仅在统计配额时临时切换租户上下文，读取完立即恢复，避免平台请求访问租户明细 |
| 审计 | `common/audit/AuditInterceptor.java` | 平台写 `platform_audit`，租户写 `sys_audit_log`，按租户隔离 |

## 2. 四条铁律

1. 所有业务表 `tenant_id NOT NULL`；唯一约束带租户维度（如 `UNIQUE(tenant_id, code)`）。
2. 任何查询由插件追加 `tenant_id`；手写 SQL（报表、结算）也必须显式包含 `tenant_id = ?`。
3. 缓存 key、对象存储路径、队列消息均带租户标识。
4. 定时任务按租户遍历，设置上下文后执行，单租户异常不影响其他租户。

## 3. 越权与生命周期回归证据

`tests/api_regression.py` 实际执行结果（2026-11-14，24/24 通过）：

| 用例 | 结果 |
|---|---|
| 租户 2（xgsg）门店列表仅包含 tenantId=2 的数据 | PASS |
| 租户 2 直接访问租户 1 门店 ID `/tenant/stores/1` | PASS（403/404） |
| 租户 2 直接访问租户 1 供货单 ID `/tenant/supply-orders/1` | PASS（403/404） |
| 平台 Token 访问 `/tenant/**` | PASS（403） |
| 租户 Token 访问 `/platform/**` | PASS（403） |
| 平台停用租户 2 后登录 | PASS（403） |
| 停用后旧 Token 再请求 | PASS（403） |
| 恢复为试用后重新登录 | PASS |
| 审计日志有记录 | PASS |
| 公众号回调有效签名回显 echostr、错误签名 403 | PASS |

## 4. 手工复测步骤

```powershell
python tests/api_regression.py
```

或在浏览器：

1. 使用 `xgsg / admin / Admin@123` 登录租户后台，只能看到「鲜果时光·浦江店」。
2. 在地址栏或接口工具访问 `/api/v1/tenant/stores/1`，应返回 404/403，且无跨租户数据。
3. 平台端登录后，租户管理仅展示汇总与配额，不提供订单/会员/库存明细入口。
4. 平台停用某租户后，该租户立即无法登录，旧 Token 请求返回 403。

## 5. 已知限制

- 平台侧为了统计套餐用量，会在服务端临时切换租户上下文读取聚合计数；接口只返回数字与配额，不返回业务明细。
- 当前采用共享库 + 行级隔离。租户 > 50、单租户 > 500 万行或单库 > 200GB 时需评估独立 schema/独立库，迁移方案需另立专项。
- 应用层没有物理行级安全（MySQL RLS），隔离依赖插件与代码纪律；新增表必须评审是否带 `tenant_id`，并补充越权回归用例。
