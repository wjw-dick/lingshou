# API 清单（/api/v1）

统一返回体：`{ "code": 0, "message": "success", "data": ... }`；分页为 `{records,total,current,size}`。认证头：`Authorization: Bearer {token}`。错误码：400 参数/业务错误、401 未登录、403 越权/停用、404 不存在、429 配额超限、500 服务异常。

在线文档：后端启动后访问 `http://127.0.0.1:8080/swagger-ui.html`。

## 认证与公开站点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | 租户登录 `{tenantCode,username,password}` |
| POST | `/platform/auth/login` | 平台登录 |
| POST | `/app/auth/login` | 会员/小程序登录 |
| POST | `/auth/logout` | 退出 |
| GET | `/auth/me` | 当前用户 + 菜单树 |
| GET | `/public/tenants` | 可用租户列表 |
| GET | `/public/site/{tenant}` | 站点配置/语言/官网菜单/Banner |
| GET | `/public/site/{tenant}/home` | 小程序首页数据 |
| GET | `/public/site/{tenant}/stores` | 门店列表（region/keyword） |
| GET | `/public/site/{tenant}/products` | 线上可售商品（categoryId/keyword/limit） |
| GET | `/public/site/{tenant}/products/{skuId}` | 商品详情（配料/保质期/贮存/过敏原） |
| GET | `/public/site/{tenant}/categories` | 商品分类 |
| GET | `/public/site/{tenant}/promotions` | 有效活动与活动商品 |
| GET | `/public/site/{tenant}/articles` `/{id}` | 资讯 |
| GET | `/public/site/{tenant}/stores/{storeId}/delivery-rule` | 配送规则 |
| POST | `/public/site/{tenant}/franchise-applications` | 加盟申请 |

## 平台运营端 `/platform`

| 资源 | 接口 |
|---|---|
| 总览 | GET `/platform/dashboard` |
| 租户 | GET/POST `/platform/tenants`；GET/PUT/DELETE `/platform/tenants/{id}`；POST `/{id}/status`、`/{id}/renew`；GET `/{id}/usage` |
| 套餐 | GET/POST `/platform/plans`；PUT/DELETE `/{id}` |
| 账单 | GET `/platform/bills`；POST `/generate`、`/{id}/pay`；GET `/export` |
| 公告 | GET/POST `/platform/announcements`；PUT/DELETE `/{id}`；GET `/{id}/read-stats` |
| 审计 | GET `/platform/audits`、`/export` |
| 账号 | GET/POST `/platform/accounts`；PUT/DELETE `/{id}` |

## 租户后台 `/tenant`

| 模块 | 主要接口 |
|---|---|
| 门店 | `/tenant/stores` CRUD、`/options`、`/{id}/status`、`/{id}/licenses`、`/{id}/audits` |
| 商品 | `/tenant/products/categories`、`/brands`、`/spus`（含 detail/import/export/status）、`/skus/barcode/{barcode}` |
| 采购 | `/tenant/purchase-orders` CRUD、`submit/approve/cancel`、`{id}/to-supply`、`replenishment` |
| 供货 | `/tenant/supply-orders` CRUD、`accept/ship/cancel` |
| 收货 | `/tenant/receive-orders`、`?supplyId` 建单、`/{id}/confirm` |
| 库存 | `/tenant/inventory/stocks`、`stocks/{skuId}/batches`、`flows`、`alerts` |
| 调拨/盘点/报损 | `/tenant/transfers`、`/stock-checks`、`/loss-orders`（创建/审核/执行） |
| 订单 | `/tenant/orders`、`/{id}`、`accept/pick-complete/complete/cancel/refund`、`refunds`、`{id}/evaluation` |
| 收银 | `/tenant/pos/categories`、`products`、`members`、`member-coupons`、`preview`、`checkout` |
| 交接班/日结 | `/tenant/shifts`、`current/open/close`；`/tenant/day-settlements`、`generate/confirm` |
| 会员 | `/tenant/members` CRUD、`levels`、`tags`、`points`、`recharge` |
| 券/活动 | `/tenant/coupons`（含 issue）、`/tenant/promotions` |
| 供应商 | `/tenant/suppliers` CRUD、`qualifications`、`products/{id}/audit` |
| 加盟商 | `/tenant/franchisees` CRUD、`stores`、`qualifications`、`commissions` |
| 结算 | `/tenant/settlements`、`generate-supplier`、`generate-franchise`、`confirm/pay/dispute/invoices` |
| 报表 | `/tenant/reports/dashboard|sales|gross-profit|inventory|members|supplier-fulfillment|franchise-ranking|export` |
| 系统 | `/tenant/system/configs`、`languages`、`menus`（save）、`messages`、`storage`、`sso`、`users`、`roles`、`permissions`、`audit-logs` |
| 文件 | POST `/tenant/files/upload`、DELETE `/tenant/files?path=` |
| 公众号 | `/tenant/mp/accounts`（含 mini-apps、qr-content、takeover-check、reset-quota）、`statistics`、`tags`、`fans`、`messages`、`templates`、`menu/current|publish|clear`、`auto-replies`、`materials`、`drafts`（含 publish）、`publishes` |
| CMS | `/tenant/cms/articles`、`banners`、`franchise-applications` |
| 公告 | `/tenant/notices` |

## 门店端 `/staff`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/staff/workbench` | 待收货/拣货/盘点/临期/交接班/今日销售/流水 |

门店端可复用 `/tenant/orders`、`/tenant/receive-orders`、`/tenant/inventory/*`、`/tenant/shifts`（按 `storeId` 归属限权）。

## 供应商端 `/supplier`

`profile`、`supply-orders`（列表/接单/拒单/发货）、`statements`（列表/详情/确认/申诉）、`products`（列表/维护供货价）、`qualifications`、`messages`。

## 加盟商端 `/franchisee`

`profile`、`stores`（销售/库存/会员概览）、`supply-orders`（报单/列表）、`commissions`、`statements`（确认）、`qualifications`、`messages`。

## 会员端 `/app`

`profile`、`home`、`coupons`（领取）、`orders`（创建/详情/支付/取消/评价/发票）、`addresses` CRUD、`favorites`、`notices`。

## 微信回调

`GET/POST /admin-api/mp/open/{appId}`：GET 验签回显 echostr；POST 验签、关注/取关、文本消息落库与关键词/关注自动回复。AES 安全模式当前返回不支持提示。

## 权限校验

- 方法级 `@RequirePerm("模块:动作")`，租户管理员自动放行。
- 平台角色：超管全部；运营（租户/套餐/公告/审计）；财务（账单/审计）。
- 角色权限支持 `*`、`product:*` 通配；越权返回 403 并进入审计。
