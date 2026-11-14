-- 零售便利连锁 SaaS 平台 预置数据 V1.0（幂等）
SET NAMES utf8mb4;

-- ---------- 套餐 ----------
INSERT IGNORE INTO platform_plan (plan_id, plan_code, name, price, billing_cycle, seat_limit, store_limit, mp_account_limit, storage_quota_gb, sms_quota, api_quota, trial_days, features) VALUES
(1, 'BASIC', '基础版', 12800.00, 'YEAR', 10, 5, 1, 10, 5000, 300000, 14, JSON_ARRAY('多端商城','基础报表','单仓管理')),
(2, 'STANDARD', '标准版', 29800.00, 'YEAR', 30, 30, 1, 50, 20000, 1200000, 14, JSON_ARRAY('多端商城','全量报表','加盟分账','供应商结算')),
(3, 'FLAGSHIP', '旗舰版', 59800.00, 'YEAR', 100, 200, 3, 200, 100000, 5000000, 14, JSON_ARRAY('多端商城','全量报表','加盟分账','供应商结算','独立域名','专属运维'));

-- ---------- 平台账号 ----------
INSERT IGNORE INTO platform_user (id, username, password, real_name, role_code, phone) VALUES
(1, 'admin', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '平台超级管理员', 'PLATFORM_ADMIN', '13800000000'),
(2, 'operator', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '平台运营', 'PLATFORM_OPERATOR', '13800000001'),
(3, 'finance', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '平台财务', 'PLATFORM_FINANCE', '13800000002');

-- ---------- 租户 ----------
INSERT IGNORE INTO platform_tenant (tenant_id, tenant_code, name, industry, contact, phone, plan_id, status, trial_end_at, expire_at, seat_limit, store_limit, mp_account_limit, storage_quota_gb, sms_quota, api_quota, created_at) VALUES
(1, 'yxbl', '悦享便利连锁', '连锁便利店', '李明', '021-60000001', 2, 'ACTIVE', NULL, '2027-12-31 23:59:59', 30, 30, 1, 50, 20000, 1200000, '2026-01-08 09:30:00'),
(2, 'xgsg', '鲜果时光便利', '连锁便利店', '王芳', '021-60000002', 1, 'TRIAL', '2026-11-28 23:59:59', NULL, 10, 5, 1, 10, 5000, 300000, '2026-11-14 09:00:00');

INSERT IGNORE INTO platform_tenant_admin (tenant_id, user_id, is_primary) VALUES (1, 1, 1), (2, 100, 1);

INSERT IGNORE INTO platform_tenant_quota_usage (tenant_id, seat_used, store_used, mp_used, storage_used_gb, sms_used, api_used, stat_date) VALUES
(1, 5, 2, 1, 1.26, 320, 18620, '2026-11-13'),
(2, 1, 1, 0, 0.10, 12, 860, '2026-11-13');

INSERT IGNORE INTO platform_bill (bill_id, tenant_id, bill_no, period, item, quantity, unit_price, amount, status, due_date, paid_at, created_at) VALUES
(1, 1, 'BILL2026100001', '2026-10', '标准版年费', 1, 29800.00, 29800.00, 'PAID', '2026-10-15', '2026-10-09 10:12:00', '2026-10-01 09:00:00'),
(2, 1, 'BILL2026110002', '2026-11', '增购存储 20GB', 20, 60.00, 1200.00, 'UNPAID', '2026-11-30', NULL, '2026-11-01 09:00:00'),
(3, 2, 'BILL2026110003', '2026-11', '基础版年费', 1, 12800.00, 12800.00, 'UNPAID', '2026-11-28', NULL, '2026-11-01 09:00:00');

INSERT IGNORE INTO platform_announcement (id, title, content, target_type, target_ids, status, published_at) VALUES
(1, '平台 V1.0 正式上线', '零售便利连锁 SaaS 平台正式上线，支持多租户、加盟分账、供应商结算与全端商城。', 'ALL', NULL, 'PUBLISHED', '2026-11-01 10:00:00'),
(2, '标准版存储扩容优惠', '11 月内标准版租户增购存储享 8 折优惠。', 'PLAN', JSON_ARRAY(2), 'PUBLISHED', '2026-11-10 09:00:00');

INSERT IGNORE INTO platform_audit (id, user_id, username, action, target_type, target_id, tenant_id, detail, ip, result, created_at) VALUES
(1, 1, 'admin', '开通租户', 'TENANT', '2', 2, '开通租户鲜果时光便利，套餐基础版', '127.0.0.1', 'SUCCESS', '2026-11-14 09:00:00'),
(2, 1, 'admin', '生成账单', 'BILL', '2', 1, '生成 2026-11 增购存储账单', '127.0.0.1', 'SUCCESS', '2026-11-01 09:00:00');

-- ---------- 租户用户 ----------
INSERT IGNORE INTO sys_user (id, tenant_id, username, password, real_name, phone, role_code, store_id, supplier_id, franchisee_id, status) VALUES
(1, 1, 'admin', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '李明', '13811110001', 'TENANT_ADMIN', NULL, NULL, NULL, 'ACTIVE'),
(2, 1, 'dianzhang', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '陈晓', '13811110002', 'STORE_MANAGER', 1, NULL, NULL, 'ACTIVE'),
(3, 1, 'shouyin', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '赵敏', '13811110003', 'CASHIER', 1, NULL, NULL, 'ACTIVE'),
(4, 1, 'gys01', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '周强', '13811110004', 'SUPPLIER', NULL, 1, NULL, 'ACTIVE'),
(5, 1, 'jm01', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '张明', '13811110005', 'FRANCHISEE', NULL, NULL, 1, 'ACTIVE'),
(100, 2, 'admin', '$2b$10$DOtCPLbPA1.YDWXLQI4psuucfolySKsGuRWfvE5B9belNK2NU3826', '王芳', '13900000001', 'TENANT_ADMIN', NULL, NULL, NULL, 'ACTIVE');

INSERT IGNORE INTO sys_role (tenant_id, role_code, name, permissions, data_scope) VALUES
(1, 'TENANT_ADMIN', '租户管理员', JSON_ARRAY('*'), 'ALL'),
(1, 'OPERATIONS', '运营', JSON_ARRAY('store:*','product:*','cms:*','mp:*','report:view','member:*'), 'ALL'),
(1, 'PURCHASER', '采购', JSON_ARRAY('product:view','purchase:*','supplier:*','inventory:*','report:view'), 'ALL'),
(1, 'PRODUCT_ADMIN', '商品管理员', JSON_ARRAY('product:*','report:view'), 'ALL'),
(1, 'FINANCE', '财务', JSON_ARRAY('report:*','settlement:*','bill:*','order:view','audit:view'), 'ALL'),
(1, 'CUSTOMER_SERVICE', '客服', JSON_ARRAY('order:*','member:*','feedback:*','report:view'), 'ALL'),
(1, 'STORE_MANAGER', '店长', JSON_ARRAY('store:view','product:view','purchase:*','inventory:*','sales:*','report:view','member:view'), 'STORE'),
(1, 'CASHIER', '收银员', JSON_ARRAY('sales:pos','sales:shift','sales:view','member:view'), 'STORE'),
(1, 'SUPPLIER', '供应商', JSON_ARRAY('supplier:self'), 'SELF'),
(1, 'FRANCHISEE', '加盟商', JSON_ARRAY('franchisee:self'), 'SELF'),
(2, 'TENANT_ADMIN', '租户管理员', JSON_ARRAY('*'), 'ALL'),
(2, 'STORE_MANAGER', '店长', JSON_ARRAY('store:view','product:view','inventory:*','sales:*'), 'STORE');

-- ---------- 菜单（PC 后台） ----------
INSERT IGNORE INTO sys_menu (id, tenant_id, end_type, parent_id, title, title_en, icon, path, component, permission, sort_no) VALUES
(1, 1, 'ADMIN', 0, '工作台', 'Dashboard', 'DataBoard', '/dashboard', 'dashboard/index', 'dashboard:view', 10),
(10, 1, 'ADMIN', 0, '门店管理', 'Stores', 'Shop', '/stores', 'store/index', 'store:view', 20),
(20, 1, 'ADMIN', 0, '商品中心', 'Products', 'Goods', '/products', NULL, 'product:view', 30),
(21, 1, 'ADMIN', 20, '商品分类', 'Categories', '', '/products/categories', 'product/category', 'product:view', 10),
(22, 1, 'ADMIN', 20, '商品档案', 'SPU / SKU', '', '/products/spus', 'product/spu', 'product:view', 20),
(30, 1, 'ADMIN', 0, '订货采购', 'Purchase', 'Van', '/purchase', NULL, 'purchase:view', 40),
(31, 1, 'ADMIN', 30, '采购单', 'Purchase Orders', '', '/purchase/orders', 'purchase/order', 'purchase:view', 10),
(32, 1, 'ADMIN', 30, '供货单', 'Supply Orders', '', '/purchase/supply', 'purchase/supply', 'purchase:view', 20),
(33, 1, 'ADMIN', 30, '收货与差异', 'Receiving', '', '/purchase/receive', 'purchase/receive', 'purchase:view', 30),
(40, 1, 'ADMIN', 0, '库存管理', 'Inventory', 'Box', '/inventory', NULL, 'inventory:view', 50),
(41, 1, 'ADMIN', 40, '库存查询', 'Stock', '', '/inventory/stocks', 'inventory/stock', 'inventory:view', 10),
(42, 1, 'ADMIN', 40, '调拨单', 'Transfers', '', '/inventory/transfers', 'inventory/transfer', 'inventory:view', 20),
(43, 1, 'ADMIN', 40, '盘点单', 'Stock Checks', '', '/inventory/checks', 'inventory/check', 'inventory:view', 30),
(44, 1, 'ADMIN', 40, '报损单', 'Losses', '', '/inventory/losses', 'inventory/loss', 'inventory:view', 40),
(50, 1, 'ADMIN', 0, '销售收银', 'Sales', 'ShoppingCart', '/sales', NULL, 'sales:view', 60),
(51, 1, 'ADMIN', 50, '订单中心', 'Orders', '', '/sales/orders', 'sales/order', 'sales:view', 10),
(52, 1, 'ADMIN', 50, '轻量收银', 'POS', '', '/sales/pos', 'sales/pos', 'sales:pos', 20),
(53, 1, 'ADMIN', 50, '交接班', 'Shifts', '', '/sales/shifts', 'sales/shift', 'sales:view', 30),
(54, 1, 'ADMIN', 50, '日结', 'Settlements', '', '/sales/settlements', 'sales/settlement', 'sales:view', 40),
(60, 1, 'ADMIN', 0, '会员营销', 'Members', 'User', '/members', NULL, 'member:view', 70),
(61, 1, 'ADMIN', 60, '会员列表', 'Members', '', '/members/list', 'member/list', 'member:view', 10),
(62, 1, 'ADMIN', 60, '优惠券', 'Coupons', '', '/members/coupons', 'member/coupon', 'member:view', 20),
(63, 1, 'ADMIN', 60, '营销活动', 'Promotions', '', '/members/promotions', 'member/promotion', 'member:view', 30),
(70, 1, 'ADMIN', 0, '供应链结算', 'Settlement', 'Money', '/suppliers', NULL, 'settlement:view', 80),
(71, 1, 'ADMIN', 70, '供应商', 'Suppliers', '', '/suppliers/list', 'supplier/list', 'supplier:view', 10),
(72, 1, 'ADMIN', 70, '供应商对账', 'Supplier Statements', '', '/suppliers/settlements', 'supplier/statement', 'settlement:view', 20),
(73, 1, 'ADMIN', 70, '加盟商与分账', 'Franchisee', '', '/franchise/list', 'franchise/list', 'franchise:view', 30),
(80, 1, 'ADMIN', 0, '报表看板', 'Reports', 'TrendCharts', '/reports', 'report/index', 'report:view', 90),
(90, 1, 'ADMIN', 0, '公众号管理', 'WeChat MP', 'ChatDotRound', '/mp', NULL, 'mp:view', 100),
(91, 1, 'ADMIN', 90, '账号管理', 'Accounts', '', '/mp/account', 'mp/account', 'mp:view', 10),
(92, 1, 'ADMIN', 90, '数据统计', 'Statistics', '', '/mp/statistics', 'mp/statistics', 'mp:view', 20),
(93, 1, 'ADMIN', 90, '标签管理', 'Tags', '', '/mp/tag', 'mp/tag', 'mp:view', 30),
(94, 1, 'ADMIN', 90, '粉丝管理', 'Fans', '', '/mp/user', 'mp/user', 'mp:view', 40),
(95, 1, 'ADMIN', 90, '消息管理', 'Messages', '', '/mp/message', 'mp/message', 'mp:view', 50),
(96, 1, 'ADMIN', 90, '模版消息', 'Templates', '', '/mp/message-template', 'mp/template', 'mp:view', 60),
(97, 1, 'ADMIN', 90, '菜单发布', 'Menu Publish', '', '/mp/menu', 'mp/menu', 'mp:view', 70),
(98, 1, 'ADMIN', 90, '自动回复', 'Auto Reply', '', '/mp/auto-reply', 'mp/autoReply', 'mp:view', 80),
(99, 1, 'ADMIN', 90, '素材管理', 'Materials', '', '/mp/material', 'mp/material', 'mp:view', 90),
(100, 1, 'ADMIN', 90, '图文草稿箱', 'Drafts', '', '/mp/draft', 'mp/draft', 'mp:view', 100),
(101, 1, 'ADMIN', 90, '发表记录', 'Publish Records', '', '/mp/free-publish', 'mp/publish', 'mp:view', 110),
(110, 1, 'ADMIN', 0, '内容管理', 'CMS', 'Document', '/cms', NULL, 'cms:view', 110),
(111, 1, 'ADMIN', 110, '文章管理', 'Articles', '', '/cms/articles', 'cms/article', 'cms:view', 10),
(112, 1, 'ADMIN', 110, 'Banner', 'Banners', '', '/cms/banners', 'cms/banner', 'cms:view', 20),
(113, 1, 'ADMIN', 110, '加盟申请', 'Franchise Applications', '', '/cms/franchise-applications', 'cms/franchise', 'cms:view', 30),
(120, 1, 'ADMIN', 0, '系统配置', 'System', 'Setting', '/system', NULL, 'system:view', 120),
(121, 1, 'ADMIN', 120, '全局参数', 'Settings', '', '/system/config', 'system/config', 'system:view', 10),
(122, 1, 'ADMIN', 120, '菜单可视化', 'Menu Builder', '', '/system/menus', 'system/menu', 'system:menu', 20),
(123, 1, 'ADMIN', 120, '消息配置', 'Message Config', '', '/system/messages', 'system/message', 'system:view', 30),
(124, 1, 'ADMIN', 120, '对象存储', 'Storage', '', '/system/storage', 'system/storage', 'system:view', 40),
(125, 1, 'ADMIN', 120, 'SSO 配置', 'SSO', '', '/system/sso', 'system/sso', 'system:view', 50),
(126, 1, 'ADMIN', 120, '用户与角色', 'Users & Roles', '', '/system/users', 'system/user', 'system:view', 60),
(127, 1, 'ADMIN', 120, '审计日志', 'Audit Logs', '', '/system/audit', 'system/audit', 'system:view', 70),
(128, 1, 'ADMIN', 120, '多语言', 'Languages', '', '/system/languages', 'system/language', 'system:view', 80);

-- 官网 / H5 / 小程序 tabBar 菜单
INSERT IGNORE INTO sys_menu (id, tenant_id, end_type, parent_id, title, title_en, icon, path, sort_no) VALUES
(201, 1, 'PORTAL', 0, '首页', 'Home', '', '/', 10),
(202, 1, 'PORTAL', 0, '门店查询', 'Stores', '', '/stores', 20),
(203, 1, 'PORTAL', 0, '鲜食与商品', 'Products', '', '/products', 30),
(204, 1, 'PORTAL', 0, '加盟合作', 'Franchise', '', '/franchise', 40),
(205, 1, 'PORTAL', 0, '品牌资讯', 'News', '', '/news', 50),
(206, 1, 'PORTAL', 0, '联系我们', 'Contact', '', '/contact', 60),
(301, 1, 'TABBAR', 0, '首页', 'Home', 'home', '/pages/home/index', 10),
(302, 1, 'TABBAR', 0, '找店', 'Stores', 'shop', '/pages/store/index', 20),
(303, 1, 'TABBAR', 0, '订单', 'Orders', 'order', '/pages/order/index', 30),
(304, 1, 'TABBAR', 0, '我的', 'Me', 'mine', '/pages/member/index', 40),
(401, 1, 'H5', 0, '限时折扣', 'Deals', '', '/h5/deals', 10),
(402, 1, 'H5', 0, '新人礼', 'New User', '', '/h5/newcomer', 20),
(403, 1, 'H5', 0, '加盟申请', 'Franchise', '', '/h5/franchise', 30);

-- ---------- 系统配置 ----------
INSERT IGNORE INTO sys_config (tenant_id, config_key, config_value, group_code, encrypted) VALUES
(1, 'site.name', '悦享便利', 'SITE', 0),
(1, 'site.logo', '/static/logo.png', 'SITE', 0),
(1, 'site.icp', '沪ICP备2026000001号', 'SITE', 0),
(1, 'site.service_phone', '400-880-0001', 'SITE', 0),
(1, 'site.business_hours', '07:00-23:00', 'SITE', 0),
(1, 'site.seo_title', '悦享便利 - 社区便利连锁品牌', 'SITE', 0),
(1, 'site.seo_keywords', '便利店,鲜食,社区零售,加盟', 'SITE', 0),
(1, 'payment.wechat_mch_id', '', 'PAYMENT', 0),
(1, 'payment.wechat_api_key', '', 'PAYMENT', 1),
(1, 'payment.alipay_app_id', '', 'PAYMENT', 0),
(1, 'delivery.min_order', '20', 'DELIVERY', 0),
(1, 'delivery.fee', '3', 'DELIVERY', 0),
(1, 'delivery.free_threshold', '49', 'DELIVERY', 0),
(1, 'delivery.radius_m', '3000', 'DELIVERY', 0),
(1, 'stock.safety_days', '2', 'INVENTORY', 0),
(1, 'expiry.near_days', '3', 'INVENTORY', 0),
(1, 'member.point_rate', '1', 'MEMBER', 0),
(1, 'commission.default_rate', '5', 'FRANCHISE', 0),
(1, 'i18n.default_lang', 'zh-CN', 'I18N', 0),
(1, 'i18n.enabled', 'true', 'I18N', 0),
(2, 'site.name', '鲜果时光便利', 'SITE', 0),
(2, 'site.service_phone', '400-880-0002', 'SITE', 0),
(2, 'stock.safety_days', '2', 'INVENTORY', 0);

INSERT IGNORE INTO sys_language (tenant_id, code, name, enabled, sort_no) VALUES
(1, 'zh-CN', '简体中文', 1, 1),
(1, 'en', 'English', 1, 2),
(2, 'zh-CN', '简体中文', 1, 1);

INSERT IGNORE INTO sys_message_channel (tenant_id, channel, enabled, config) VALUES
(1, 'MP_TEMPLATE', 1, JSON_OBJECT('accountId', 1)),
(1, 'MINI_SUBSCRIBE', 1, JSON_OBJECT('miniAppType', 'USER')),
(1, 'APP_PUSH', 0, NULL),
(1, 'INNER', 1, NULL),
(2, 'INNER', 1, NULL);

INSERT IGNORE INTO sys_sms_channel (tenant_id, provider, enabled, config) VALUES
(1, 'ALIYUN', 0, NULL),
(1, 'TENCENT', 0, NULL),
(1, 'HUAWEI', 0, NULL),
(1, 'JDCLOUD', 0, NULL),
(1, 'AWS_SNS', 0, NULL),
(1, 'LOCAL', 1, JSON_OBJECT('level', 'info')),
(2, 'LOCAL', 1, NULL);

INSERT IGNORE INTO sys_message_template (tenant_id, node, channel, template_id, title, content, field_mapping, enabled) VALUES
(1, 'ORDER_PAID', 'MP_TEMPLATE', 'TPL_ORDER_PAID', '订单支付成功通知', '您的订单{{orderNo}}已支付成功，金额{{amount}}元。', 'character_string1=orderNo,amount2=amount,time3=paidTime', 1),
(1, 'ORDER_PICKUP', 'MINI_SUBSCRIBE', 'TPL_PICKUP_READY', '待取货提醒', '您的订单{{orderNo}}已备货完成，请到{{storeName}}取货。', 'character_string1=orderNo,thing2=storeName', 1),
(1, 'RECHARGE_SUCCESS', 'MP_TEMPLATE', 'TPL_RECHARGE', '储值充值成功', '充值{{amount}}元成功，当前余额{{balance}}元。', 'amount1=amount,amount2=balance', 1),
(1, 'BIRTHDAY', 'MP_TEMPLATE', 'TPL_BIRTHDAY', '生日祝福', '{{nickname}}，生日快乐！送您一张生日券。', 'thing1=nickname', 0);

INSERT IGNORE INTO sys_sso_config (tenant_id, enabled, protocol, server_url, service_url, client_id, client_secret, account_mapping, auto_create) VALUES
(1, 0, 'OIDC', 'https://sso.example.com', 'https://admin.yxbl.example.com/sso/callback', 'retail-convenience', '', 'username', 0);

INSERT IGNORE INTO sys_storage_config (tenant_id, driver, config, enabled) VALUES
(1, 'LOCAL', JSON_OBJECT('rootPath', './data/uploads', 'publicBaseUrl', '/uploads'), 1),
(2, 'LOCAL', JSON_OBJECT('rootPath', './data/uploads', 'publicBaseUrl', '/uploads'), 1);

-- ---------- 门店 ----------
INSERT IGNORE INTO store (id, tenant_id, code, name, type, region, address, lng, lat, business_hours, phone, manager_name, manager_phone, delivery_radius_m, status, opened_at, license_expire_at, images) VALUES
(1, 1, 'S001', '悦享便利·静安旗舰店', 'DIRECT', '静安区', '上海市静安区南京西路1266号', 121.455100, 31.229000, '07:00-23:00', '021-62000001', '陈晓', '13811110002', 3000, 'OPEN', '2024-03-18', '2028-06-30', JSON_ARRAY('/static/store1.jpg')),
(2, 1, 'S002', '悦享便利·徐汇科技园店', 'FRANCHISE', '徐汇区', '上海市徐汇区宜山路700号', 121.414500, 31.174000, '07:30-22:30', '021-62000002', '张明', '13811110005', 2500, 'OPEN', '2025-05-20', '2028-05-19', JSON_ARRAY('/static/store2.jpg')),
(101, 2, 'FS001', '鲜果时光·浦江店', 'DIRECT', '闵行区', '上海市闵行区浦江镇联航路1688号', 121.506000, 31.090000, '08:00-22:00', '021-63000001', '王芳', '13900000002', 2000, 'OPEN', '2026-09-01', '2027-08-31', NULL);

INSERT IGNORE INTO store_license (tenant_id, store_id, license_type, name, file_url, expire_at, status) VALUES
(1, 1, 'FOOD', '食品经营许可证', '/static/license/food-s001.pdf', '2028-06-30', 'VALID'),
(1, 1, 'TOBACCO', '烟草专卖零售许可证', '/static/license/tobacco-s001.pdf', '2027-11-30', 'VALID'),
(1, 2, 'FOOD', '食品经营许可证', '/static/license/food-s002.pdf', '2028-05-19', 'VALID');

INSERT IGNORE INTO store_audit_record (tenant_id, store_id, supervisor, check_date, score, issues, photos) VALUES
(1, 1, '刘督导', '2026-11-08', 95, '冷柜温度记录完整，临期专区陈列规范', NULL),
(1, 2, '刘督导', '2026-11-09', 88, '后仓纸箱未及时清理，已现场整改', NULL);

-- ---------- 商品 ----------
INSERT IGNORE INTO product_category (id, tenant_id, parent_id, name, name_en, icon, sort_no, near_expiry_days) VALUES
(1, 1, 0, '鲜食', 'Fresh Food', 'Food', 10, 2),
(2, 1, 0, '饮料', 'Beverages', 'Coffee', 20, 7),
(3, 1, 0, '零食', 'Snacks', 'IceCream', 30, 7),
(4, 1, 0, '日用', 'Daily Goods', 'ShoppingBag', 40, 15),
(5, 1, 0, '烟酒', 'Tobacco & Alcohol', 'WarnTriangleFilled', 50, 30),
(101, 2, 0, '鲜食', 'Fresh Food', 'Food', 10, 2),
(102, 2, 0, '饮料', 'Beverages', 'Coffee', 20, 7);

INSERT IGNORE INTO brand (id, tenant_id, name, name_en, logo) VALUES
(1, 1, '悦享自有品牌', 'Yuexiang', NULL),
(2, 1, '可口可乐', 'Coca-Cola', NULL),
(3, 1, '农夫山泉', 'Nongfu Spring', NULL),
(4, 1, '三只松鼠', 'Three Squirrels', NULL),
(5, 1, '清风', 'Breeze', NULL);

INSERT IGNORE INTO product_spu (id, tenant_id, spu_code, name, name_en, category_id, brand_id, unit, spec, storage_condition, shelf_life_days, origin, ingredients, allergens, is_tobacco, online_sale, status, main_image, price_guide, price_retail, price_member, price_franchise) VALUES
(1, 1, 'SPU001', '悦享厚切三明治', 'Yuexiang Thick Sandwich', 1, 1, '份', '180g', 'COLD', 3, '上海', '小麦粉、鸡蛋、火腿、生菜、沙拉酱', '麸质、蛋类', 0, 1, 'ON_SALE', '/static/products/sandwich.jpg', 9.90, 9.90, 8.90, 7.50),
(2, 1, 'SPU002', '悦享冰美式', 'Yuexiang Iced Americano', 1, 1, '杯', '400ml', 'COLD', 1, '上海', '水、咖啡豆', NULL, 0, 1, 'ON_SALE', '/static/products/coffee.jpg', 12.00, 12.00, 10.00, 8.00),
(3, 1, 'SPU003', '可口可乐 500ml', 'Coca-Cola 500ml', 2, 2, '瓶', '500ml', 'NORMAL', 270, '中国', '水、果葡糖浆、白砂糖、二氧化碳、焦糖色、磷酸、咖啡因', NULL, 0, 1, 'ON_SALE', '/static/products/cola.jpg', 3.50, 3.50, 3.20, 2.80),
(4, 1, 'SPU004', '农夫山泉 550ml', 'Nongfu Spring 550ml', 2, 3, '瓶', '550ml', 'NORMAL', 365, '浙江千岛湖', '天然饮用水', NULL, 0, 1, 'ON_SALE', '/static/products/water.jpg', 2.00, 2.00, 1.90, 1.50),
(5, 1, 'SPU005', '东方树叶 500ml', 'Oriental Leaf 500ml', 2, 3, '瓶', '500ml', 'NORMAL', 270, '中国', '水、茶叶、维生素C、碳酸氢钠', NULL, 0, 1, 'ON_SALE', '/static/products/tea.jpg', 5.00, 5.00, 4.50, 3.80),
(6, 1, 'SPU006', '乐事薯片 75g', "Lay's Chips 75g", 3, NULL, '袋', '75g', 'NORMAL', 180, '中国', '马铃薯、植物油、食用盐', NULL, 0, 1, 'ON_SALE', '/static/products/chips.jpg', 7.50, 7.50, 7.00, 6.00),
(7, 1, 'SPU007', '三只松鼠每日坚果', 'Squirrel Daily Nuts', 3, 4, '盒', '25g×7', 'NORMAL', 180, '安徽芜湖', '巴旦木仁、腰果仁、核桃仁、蔓越莓干', '坚果', 0, 1, 'ON_SALE', '/static/products/nuts.jpg', 29.90, 29.90, 26.90, 23.00),
(8, 1, 'SPU008', '清风抽纸 3 层', 'Breeze Tissue', 4, 5, '包', '120抽', 'NORMAL', 1095, '中国', '原生木浆', NULL, 0, 1, 'ON_SALE', '/static/products/tissue.jpg', 6.50, 6.50, 6.00, 5.00),
(9, 1, 'SPU009', '中华香烟（软）', 'Chunghwa Cigarettes', 5, NULL, '包', '20支', 'NORMAL', 365, '上海', '烟叶', NULL, 1, 0, 'ON_SALE', '/static/products/cigarette.jpg', 55.00, 55.00, 55.00, 50.00),
(10, 1, 'SPU010', '关东煮组合', 'Oden Combo', 1, 1, '份', '5串', 'COLD', 1, '上海', '鱼丸、萝卜、海带结、豆腐、汤底', '鱼类', 0, 1, 'ON_SALE', '/static/products/oden.jpg', 15.00, 15.00, 13.50, 11.00),
(101, 2, 'FSPU001', '鲜果时光鲜切果盒', 'Fresh Fruit Box', 101, NULL, '盒', '250g', 'COLD', 2, '上海', '时令水果', NULL, 0, 1, 'ON_SALE', NULL, 15.00, 15.00, 13.80, 12.00);

INSERT IGNORE INTO product_sku (id, tenant_id, spu_id, sku_code, barcode, spec, unit, cost_price, price_guide, price_retail, price_member, price_franchise, status) VALUES
(1, 1, 1, 'SKU001', '6900000000001', '180g', '份', 5.50, 9.90, 9.90, 8.90, 7.50, 'ON_SALE'),
(2, 1, 2, 'SKU002', '6900000000002', '400ml', '杯', 4.00, 12.00, 12.00, 10.00, 8.00, 'ON_SALE'),
(3, 1, 3, 'SKU003', '6900000000003', '500ml', '瓶', 2.60, 3.50, 3.50, 3.20, 2.80, 'ON_SALE'),
(4, 1, 4, 'SKU004', '6900000000004', '550ml', '瓶', 1.20, 2.00, 2.00, 1.90, 1.50, 'ON_SALE'),
(5, 1, 5, 'SKU005', '6900000000005', '500ml', '瓶', 3.20, 5.00, 5.00, 4.50, 3.80, 'ON_SALE'),
(6, 1, 6, 'SKU006', '6900000000006', '75g', '袋', 5.20, 7.50, 7.50, 7.00, 6.00, 'ON_SALE'),
(7, 1, 7, 'SKU007', '6900000000007', '25g×7', '盒', 19.00, 29.90, 29.90, 26.90, 23.00, 'ON_SALE'),
(8, 1, 8, 'SKU008', '6900000000008', '120抽', '包', 4.10, 6.50, 6.50, 6.00, 5.00, 'ON_SALE'),
(9, 1, 9, 'SKU009', '6900000000009', '20支', '包', 45.00, 55.00, 55.00, 55.00, 50.00, 'ON_SALE'),
(10, 1, 10, 'SKU010', '6900000000010', '5串', '份', 8.00, 15.00, 15.00, 13.50, 11.00, 'ON_SALE'),
(101, 2, 101, 'FSKU001', '6900000000101', '250g', '盒', 8.00, 15.00, 15.00, 13.80, 12.00, 'ON_SALE');

INSERT IGNORE INTO product_barcode (tenant_id, sku_id, barcode) VALUES
(1, 1, '6900000000001'), (1, 1, '6900000000011'), (1, 3, '6900000000003'), (2, 101, '6900000000101');

INSERT IGNORE INTO store_sku_price (tenant_id, store_id, sku_id, price_retail, price_member) VALUES
(1, 1, 1, 9.90, 8.90), (1, 1, 2, 12.00, 10.00), (1, 2, 1, 10.50, 9.50);

-- ---------- 供应商 / 加盟商 ----------
INSERT IGNORE INTO supplier (id, tenant_id, code, name, contact, phone, address, category, settle_cycle_days, credit_limit, status) VALUES
(1, 1, 'SUP001', '上海鲜食供应链有限公司', '周强', '13811110004', '上海市松江区莘砖公路668号', '鲜食/冷链', 30, 200000.00, 'ACTIVE'),
(2, 1, 'SUP002', '华东饮料配送中心', '吴磊', '13811110006', '上海市嘉定区曹安公路3000号', '饮料/零食', 45, 150000.00, 'ACTIVE');

INSERT IGNORE INTO supplier_qualification (tenant_id, supplier_id, type, file_url, expire_at) VALUES
(1, 1, 'BUSINESS_LICENSE', '/static/qual/sup001-business.pdf', '2029-12-31'),
(1, 1, 'FOOD_PRODUCTION', '/static/qual/sup001-food.pdf', '2027-08-31'),
(1, 2, 'BUSINESS_LICENSE', '/static/qual/sup002-business.pdf', '2028-10-31');

INSERT IGNORE INTO supplier_product (tenant_id, supplier_id, sku_id, supply_price, audit_status) VALUES
(1, 1, 1, 7.50, 'APPROVED'), (1, 1, 2, 8.00, 'APPROVED'), (1, 1, 10, 11.00, 'APPROVED'),
(1, 2, 3, 2.80, 'APPROVED'), (1, 2, 4, 1.50, 'APPROVED'), (1, 2, 5, 3.80, 'APPROVED');

INSERT IGNORE INTO franchisee (id, tenant_id, code, name, contact, phone, level, commission_type, commission_value, credit_limit, used_credit, deposit, contract_no, contract_start, contract_end, status) VALUES
(1, 1, 'FR001', '张明加盟商', '张明', '13811110005', 'A', 'SALES', 5.00, 100000.00, 20000.00, 50000.00, 'FRAN-2025-001', '2025-05-20', '2030-05-19', 'ACTIVE');

INSERT IGNORE INTO franchisee_store (tenant_id, franchisee_id, store_id, join_date, status) VALUES
(1, 1, 2, '2025-05-20', 'ACTIVE');

INSERT IGNORE INTO franchisee_qualification (tenant_id, franchisee_id, type, file_url, expire_at) VALUES
(1, 1, 'CONTRACT', '/static/qual/fr001-contract.pdf', '2030-05-19'),
(1, 1, 'BUSINESS_LICENSE', '/static/qual/fr001-business.pdf', '2029-12-31');

-- ---------- 采购 / 供货 / 收货 ----------
INSERT IGNORE INTO purchase_order (id, tenant_id, order_no, supplier_id, status, total_amount, expected_at, remark, created_by, created_at) VALUES
(1, 1, 'PO20261101001', 1, 'APPROVED', 3750.00, '2026-11-15', '鲜食周度采购', '李明', '2026-11-12 09:30:00'),
(2, 1, 'PO20261101002', 2, 'SUBMITTED', 1820.00, '2026-11-16', '饮料补货', '李明', '2026-11-13 10:20:00');

INSERT IGNORE INTO purchase_order_item (tenant_id, purchase_id, sku_id, qty, received_qty, price, amount) VALUES
(1, 1, 1, 200, 0, 7.50, 1500.00), (1, 1, 2, 150, 0, 8.00, 1200.00), (1, 1, 10, 95, 0, 11.00, 1045.00),
(1, 2, 3, 300, 0, 2.80, 840.00), (1, 2, 4, 500, 0, 1.50, 750.00), (1, 2, 5, 60, 0, 3.80, 228.00);

INSERT IGNORE INTO supply_order (id, tenant_id, order_no, source_type, supplier_id, store_id, status, total_amount, expected_at, logistics_no, remark, created_at) VALUES
(1, 1, 'SO20261112001', 'SUPPLIER', 1, 1, 'SHIPPED', 1745.00, '2026-11-15', 'SF1234567890', '静安旗舰店鲜食补货', '2026-11-12 14:00:00'),
(2, 1, 'SO20261110002', 'HQ', NULL, 2, 'RECEIVED', 760.00, '2026-11-11', NULL, '徐汇店饮料补货', '2026-11-10 09:00:00');

INSERT IGNORE INTO supply_order_item (tenant_id, supply_id, sku_id, qty, shipped_qty, received_qty, price, amount) VALUES
(1, 1, 1, 100, 100, 0, 7.50, 750.00), (1, 1, 2, 80, 80, 0, 8.00, 640.00), (1, 1, 10, 30, 30, 0, 11.00, 330.00),
(1, 2, 3, 100, 100, 100, 2.80, 280.00), (1, 2, 4, 200, 200, 200, 1.50, 300.00), (1, 2, 5, 40, 40, 40, 4.50, 180.00);

INSERT IGNORE INTO receive_order (id, tenant_id, receive_no, supply_id, store_id, status, total_qty, diff_qty, receiver, received_at, remark, created_at) VALUES
(1, 1, 'RO20261111001', 2, 2, 'CONFIRMED', 340, 0, '张明', '2026-11-11 10:20:00', '数量一致，冷链正常', '2026-11-11 10:00:00'),
(2, 1, 'RO20261114001', 1, 1, 'PENDING', 0, 0, NULL, NULL, '待到货验收', '2026-11-14 08:30:00');

INSERT IGNORE INTO receive_order_item (tenant_id, receive_id, supply_item_id, sku_id, qty, actual_qty, diff_qty, production_date, expiry_date, cold_chain_temp, batch_no) VALUES
(1, 1, 4, 3, 100, 100, 0, '2026-10-20', '2027-07-18', NULL, 'B20261020A'),
(1, 1, 5, 4, 200, 200, 0, '2026-10-28', '2027-07-27', NULL, 'B20261028B'),
(1, 1, 6, 5, 40, 40, 0, '2026-10-25', '2027-07-22', NULL, 'B20261025C'),
(1, 2, 1, 1, 100, 0, 0, NULL, NULL, NULL, NULL),
(1, 2, 2, 2, 80, 0, 0, NULL, NULL, NULL, NULL),
(1, 2, 3, 10, 30, 0, 0, NULL, NULL, NULL, NULL);

INSERT IGNORE INTO difference_order (tenant_id, diff_no, receive_id, supply_id, store_id, type, content, status, created_at) VALUES
(1, 'DO20261110001', 1, 2, 2, 'OTHER', '送货单签收章缺失，已要求补传', 'PENDING', '2026-11-11 10:25:00');

-- ---------- 库存 ----------
INSERT IGNORE INTO stock (tenant_id, owner_type, owner_id, sku_id, qty, locked_qty, safety_stock) VALUES
(1, 'STORE', 1, 1, 46, 2, 20), (1, 'STORE', 1, 2, 32, 0, 15), (1, 'STORE', 1, 3, 118, 0, 30),
(1, 'STORE', 1, 4, 260, 0, 50), (1, 'STORE', 1, 5, 54, 0, 20), (1, 'STORE', 1, 6, 38, 0, 15),
(1, 'STORE', 1, 7, 22, 0, 10), (1, 'STORE', 1, 8, 76, 0, 20), (1, 'STORE', 1, 9, 12, 0, 5),
(1, 'STORE', 1, 10, 18, 1, 10),
(1, 'STORE', 2, 1, 18, 0, 15), (1, 'STORE', 2, 2, 9, 0, 10), (1, 'STORE', 2, 3, 96, 0, 20),
(1, 'STORE', 2, 4, 180, 0, 40), (1, 'STORE', 2, 5, 35, 0, 15), (1, 'STORE', 2, 6, 20, 0, 10),
(1, 'WAREHOUSE', 1, 1, 220, 0, 50), (1, 'WAREHOUSE', 1, 2, 160, 0, 40), (1, 'WAREHOUSE', 1, 3, 500, 0, 100),
(1, 'WAREHOUSE', 1, 4, 800, 0, 200), (1, 'WAREHOUSE', 1, 10, 80, 0, 30),
(2, 'STORE', 101, 101, 60, 0, 10);

INSERT IGNORE INTO stock_batch (tenant_id, owner_type, owner_id, sku_id, batch_no, production_date, expiry_date, qty) VALUES
(1, 'STORE', 1, 1, 'B20261112A', '2026-11-12', '2026-11-15', 46),
(1, 'STORE', 1, 2, 'B20261113A', '2026-11-13', '2026-11-14', 32),
(1, 'STORE', 1, 10, 'B20261113B', '2026-11-13', '2026-11-14', 18),
(1, 'STORE', 2, 1, 'B20261111C', '2026-11-11', '2026-11-14', 18),
(1, 'STORE', 2, 2, 'B20261112C', '2026-11-12', '2026-11-13', 9),
(1, 'WAREHOUSE', 1, 1, 'B20261110W', '2026-11-10', '2026-11-13', 20),
(1, 'WAREHOUSE', 1, 1, 'B20261112W', '2026-11-12', '2026-11-15', 200),
(1, 'WAREHOUSE', 1, 3, 'B20261020A', '2026-10-20', '2027-07-18', 500);

INSERT IGNORE INTO stock_flow (tenant_id, owner_type, owner_id, sku_id, biz_type, change_qty, before_qty, after_qty, ref_type, ref_id, remark, created_by, created_at) VALUES
(1, 'STORE', 2, 3, 'RECEIVE', 100, 0, 100, 'RECEIVE', 1, '收货入库', '张明', '2026-11-11 10:20:00'),
(1, 'STORE', 2, 4, 'RECEIVE', 200, 0, 200, 'RECEIVE', 1, '收货入库', '张明', '2026-11-11 10:20:00'),
(1, 'STORE', 1, 1, 'SALE', -4, 50, 46, 'ORDER', 1, '收银售出', '赵敏', '2026-11-13 12:05:00'),
(1, 'STORE', 1, 1, 'LOSS_PENDING', 0, 46, 46, 'LOSS', 1, '临期报损申请', '陈晓', '2026-11-14 08:40:00');

INSERT IGNORE INTO transfer_order (id, tenant_id, transfer_no, from_type, from_id, to_type, to_id, status, total_qty, remark, applied_by, created_at) VALUES
(1, 1, 'TR20261114001', 'WAREHOUSE', 1, 'STORE', 1, 'PENDING', 120, '总仓向静安店调拨饮料', '陈晓', '2026-11-14 08:10:00');

INSERT IGNORE INTO transfer_order_item (tenant_id, transfer_id, sku_id, qty, batch_no) VALUES
(1, 1, 3, 60, 'B20261020A'), (1, 1, 4, 60, 'B20261028B');

INSERT IGNORE INTO stock_check (id, tenant_id, check_no, store_id, check_type, status, total_diff_qty, remark, created_by, approved_by, created_at) VALUES
(1, 1, 'SC20261112001', 1, 'CYCLE', 'APPROVED', -2, '鲜食循环盘点', '陈晓', '李明', '2026-11-12 22:00:00');

INSERT IGNORE INTO stock_check_item (tenant_id, check_id, sku_id, book_qty, actual_qty, diff_qty, reason) VALUES
(1, 1, 1, 48, 46, -2, '试吃损耗未登记');

INSERT IGNORE INTO loss_order (id, tenant_id, loss_no, store_id, reason_type, status, total_qty, total_amount, photos, remark, created_by, created_at) VALUES
(1, 1, 'LS20261114001', 1, 'NEAR_EXPIRY', 'PENDING', 6, 59.40, JSON_ARRAY('/static/loss/1.jpg'), '三明治临期，申请报损', '陈晓', '2026-11-14 08:40:00'),
(2, 1, 'LS20261110002', 2, 'EXPIRED', 'APPROVED', 3, 36.00, NULL, '冰美式过期', '张明', '2026-11-10 21:30:00');

INSERT IGNORE INTO loss_order_item (tenant_id, loss_id, sku_id, batch_no, qty, amount) VALUES
(1, 1, 1, 'B20261112A', 6, 59.40), (1, 2, 2, 'B20261109C', 3, 36.00);

-- ---------- 会员 ----------
INSERT IGNORE INTO member_level (id, tenant_id, code, name, min_points, discount, benefits) VALUES
(1, 1, 'NORMAL', '普通会员', 0, 10.00, JSON_ARRAY('积分累计')),
(2, 1, 'SILVER', '银卡会员', 1000, 9.80, JSON_ARRAY('积分累计','会员价','生日券')),
(3, 1, 'GOLD', '金卡会员', 5000, 9.50, JSON_ARRAY('积分累计','会员价','生日券','专属客服'));

INSERT IGNORE INTO member (id, tenant_id, openid, unionid, phone, phone_mask, nickname, avatar, gender, birthday, level_id, points, wallet_balance, total_consume, order_count, tags, status, registered_at, last_active_at) VALUES
(1, 1, 'oUx1demo001', 'union_demo_001', '13800001111', '138****1111', '小悦', '/static/avatar/1.png', 1, '1994-06-18', 3, 5280, 268.50, 4860.70, 68, JSON_ARRAY('高频','鲜食爱好者'), 'ACTIVE', '2025-03-02 10:20:00', '2026-11-13 18:40:00'),
(2, 1, 'oUx1demo002', 'union_demo_002', '13900002222', '139****2222', '阿哲', '/static/avatar/2.png', 1, '1990-11-02', 2, 1580, 56.00, 1620.30, 24, JSON_ARRAY('饮料'), 'ACTIVE', '2025-08-11 09:10:00', '2026-11-12 12:30:00'),
(3, 1, 'oUx1demo003', 'union_demo_003', '13700003333', '137****3333', 'Lily', '/static/avatar/3.png', 2, '1998-02-14', 1, 320, 0.00, 286.50, 7, JSON_ARRAY('新客'), 'ACTIVE', '2026-10-05 16:20:00', '2026-11-10 20:00:00'),
(101, 2, 'oUx2demo001', 'union_demo_101', '13600004444', '136****4444', '果果', NULL, 2, NULL, NULL, 120, 0.00, 96.00, 3, NULL, 'ACTIVE', '2026-10-01 10:00:00', '2026-11-11 11:00:00');

INSERT IGNORE INTO member_point_flow (tenant_id, member_id, change_points, balance, biz_type, ref_id, remark, created_at) VALUES
(1, 1, 120, 5280, 'ORDER', 1, '消费获得积分', '2026-11-13 12:05:00'),
(1, 2, -500, 1580, 'EXCHANGE', NULL, '积分兑换商品', '2026-11-12 12:30:00'),
(1, 3, 50, 320, 'REGISTER', NULL, '新人注册赠送', '2026-10-05 16:20:00');

INSERT IGNORE INTO member_wallet_flow (tenant_id, member_id, change_amount, balance, biz_type, ref_id, remark, created_at) VALUES
(1, 1, 200.00, 268.50, 'RECHARGE', NULL, '充值 200 元赠 20 元', '2026-11-01 10:00:00'),
(1, 1, 20.00, 288.50, 'GIFT', NULL, '充值赠送', '2026-11-01 10:00:00'),
(1, 1, -20.00, 268.50, 'CONSUME', 3, '订单消费', '2026-11-05 18:20:00'),
(1, 2, 56.00, 56.00, 'RECHARGE', NULL, '充值', '2026-10-20 09:00:00');

INSERT IGNORE INTO member_address (tenant_id, member_id, name, phone, address, lng, lat, is_default) VALUES
(1, 1, '小悦', '13800001111', '上海市静安区南京西路1200弄8号501室', 121.454000, 31.228000, 1),
(1, 2, '阿哲', '13900002222', '上海市徐汇区宜山路800号2号楼', 121.415000, 31.175000, 1);

INSERT IGNORE INTO member_favorite (tenant_id, member_id, sku_id) VALUES (1, 1, 1), (1, 1, 2), (1, 2, 3);

INSERT IGNORE INTO member_tag (tenant_id, name, color) VALUES
(1, '高频', '#1f6feb'), (1, '鲜食爱好者', '#18a058'), (1, '饮料', '#f0a020'), (1, '新客', '#d03050');

INSERT IGNORE INTO coupon (id, tenant_id, name, type, threshold_amount, discount_amount, discount_rate, sku_id, total_qty, issued_qty, per_user_limit, valid_from, valid_to, status) VALUES
(1, 1, '满30减5元券', 'FULL_REDUCTION', 30.00, 5.00, 10.00, NULL, 1000, 320, 2, '2026-11-01 00:00:00', '2026-12-31 23:59:59', 'ACTIVE'),
(2, 1, '新人满20减3元券', 'FULL_REDUCTION', 20.00, 3.00, 10.00, NULL, 500, 120, 1, '2026-11-01 00:00:00', '2027-01-31 23:59:59', 'ACTIVE'),
(3, 1, '全场9折券', 'DISCOUNT', 0.00, 0.00, 9.00, NULL, 300, 80, 1, '2026-11-01 00:00:00', '2026-11-30 23:59:59', 'ACTIVE'),
(4, 1, '三明治立减2元', 'PRODUCT', 0.00, 2.00, 10.00, 1, 200, 45, 1, '2026-11-01 00:00:00', '2026-12-15 23:59:59', 'ACTIVE');

INSERT IGNORE INTO member_coupon (id, tenant_id, coupon_id, member_id, status, received_at, used_at, order_id) VALUES
(1, 1, 1, 1, 'UNUSED', '2026-11-01 10:05:00', NULL, NULL),
(2, 1, 2, 3, 'UNUSED', '2026-10-05 16:25:00', NULL, NULL),
(3, 1, 3, 2, 'USED', '2026-11-02 09:00:00', '2026-11-05 18:20:00', 3),
(4, 1, 4, 1, 'UNUSED', '2026-11-10 12:00:00', NULL, NULL);

INSERT IGNORE INTO promotion (id, tenant_id, name, type, start_at, end_at, rule_json, store_scope, status) VALUES
(1, 1, '鲜食限时 8 折', 'DISCOUNT', '2026-11-01 00:00:00', '2026-11-30 23:59:59', JSON_OBJECT('discount', 0.8, 'categoryId', 1), JSON_ARRAY(1, 2), 'ACTIVE'),
(2, 1, '饮料第二件半价', 'SECOND_HALF', '2026-11-01 00:00:00', '2026-11-30 23:59:59', JSON_OBJECT('secondHalf', true, 'categoryId', 2), JSON_ARRAY(1, 2), 'ACTIVE');

INSERT IGNORE INTO promotion_item (tenant_id, promotion_id, sku_id, original_price, promo_price, discount) VALUES
(1, 1, 1, 9.90, 7.90, 8.00), (1, 1, 2, 12.00, 9.60, 8.00), (1, 1, 10, 15.00, 12.00, 8.00);

-- ---------- 订单 / 收银 / 日结 ----------
INSERT IGNORE INTO sales_order (id, tenant_id, order_no, channel, store_id, member_id, delivery_type, status, pay_status, total_amount, discount_amount, freight_amount, payable_amount, paid_amount, member_coupon_id, points_used, points_discount, wallet_paid, payment_method, delivery_name, delivery_phone, delivery_address, expected_time, accept_deadline, remark, created_at, paid_at, completed_at) VALUES
(1, 1, 'POS202611130001', 'POS', 1, 1, 'PICKUP', 'COMPLETED', 'PAID', 39.60, 2.00, 0.00, 37.60, 37.60, 4, 0, 0.00, 0.00, 'WECHAT', NULL, NULL, NULL, NULL, NULL, '门店收银', '2026-11-13 12:05:00', '2026-11-13 12:05:00', '2026-11-13 12:05:00'),
(2, 1, 'ON202611130002', 'ONLINE', 1, 2, 'DELIVERY', 'COMPLETED', 'PAID', 23.50, 5.00, 3.00, 21.50, 21.50, 3, 0, 0.00, 0.00, 'WECHAT', '阿哲', '13900002222', '上海市徐汇区宜山路800号2号楼', '尽快送达', NULL, '少放冰块', '2026-11-13 18:20:00', '2026-11-13 18:21:00', '2026-11-13 19:05:00'),
(3, 1, 'ON202611140003', 'ONLINE', 1, 1, 'PICKUP', 'PENDING_ACCEPT', 'PAID', 45.90, 0.00, 0.00, 45.90, 45.90, NULL, 200, 2.00, 43.90, 'WALLET', NULL, NULL, NULL, '18:00-18:30', '2026-11-14 18:10:00', '下班取', '2026-11-14 17:52:00', '2026-11-14 17:53:00', NULL),
(4, 1, 'ON202611140004', 'ONLINE', 2, 3, 'PICKUP', 'PENDING_PAY', 'UNPAID', 18.00, 0.00, 0.00, 18.00, 0.00, NULL, 0, 0.00, 0.00, NULL, NULL, NULL, NULL, '19:00-19:30', NULL, NULL, '2026-11-14 18:30:00', NULL, NULL),
(5, 1, 'ON202611100005', 'ONLINE', 1, 1, 'DELIVERY', 'CANCELLED', 'REFUNDED', 62.00, 5.00, 3.00, 60.00, 0.00, NULL, 0, 0.00, 0.00, 'WECHAT', '小悦', '13800001111', '上海市静安区南京西路1200弄8号501室', '尽快送达', NULL, '超时未接单自动取消', '2026-11-10 20:10:00', '2026-11-10 20:11:00', NULL);

INSERT IGNORE INTO sales_order_item (tenant_id, order_id, sku_id, sku_name, barcode, price, qty, amount) VALUES
(1, 1, 1, '悦享厚切三明治', '6900000000001', 9.90, 2, 19.80),
(1, 1, 2, '悦享冰美式', '6900000000002', 12.00, 1, 12.00),
(1, 1, 6, '乐事薯片 75g', '6900000000006', 7.50, 1, 7.50),
(1, 2, 1, '悦享厚切三明治', '6900000000001', 9.90, 1, 9.90),
(1, 2, 5, '东方树叶 500ml', '6900000000005', 5.00, 1, 5.00),
(1, 2, 3, '可口可乐 500ml', '6900000000003', 3.50, 1, 3.50),
(1, 2, 2, '悦享冰美式', '6900000000002', 5.00, 1, 5.00),
(1, 3, 7, '三只松鼠每日坚果', '6900000000007', 29.90, 1, 29.90),
(1, 3, 2, '悦享冰美式', '6900000000002', 12.00, 1, 12.00),
(1, 3, 8, '清风抽纸 3 层', '6900000000008', 6.50, 1, 6.50),
(1, 4, 1, '悦享厚切三明治', '6900000000001', 9.90, 1, 9.90),
(1, 4, 3, '可口可乐 500ml', '6900000000003', 3.50, 1, 3.50),
(1, 4, 6, '乐事薯片 75g', '6900000000006', 7.50, 1, 7.50),
(1, 5, 7, '三只松鼠每日坚果', '6900000000007', 29.90, 2, 59.80);

INSERT IGNORE INTO refund_order (id, tenant_id, refund_no, order_id, store_id, type, amount, reason, status, items, created_at) VALUES
(1, 1, 'RF202611100001', 5, 1, 'FULL', 60.00, '超时未接单自动取消', 'REFUNDED', JSON_ARRAY(JSON_OBJECT('skuId', 7, 'qty', 2)), '2026-11-10 20:20:00');

INSERT IGNORE INTO order_evaluation (tenant_id, order_id, member_id, store_id, score, content) VALUES
(1, 2, 2, 1, 5, '配送很快，鲜食日期新鲜。'),
(1, 1, 1, 1, 5, '收银速度快。');

INSERT IGNORE INTO invoice_request (tenant_id, order_id, member_id, title, tax_no, email, amount, status) VALUES
(1, 2, 2, '上海某某科技有限公司', '91310000MA1FL8XX2X', 'finance@example.com', 21.50, 'PENDING');

INSERT IGNORE INTO pos_shift (id, tenant_id, shift_no, store_id, cashier_id, cashier_name, start_at, end_at, opening_cash, cash_sales, wechat_sales, alipay_sales, wallet_sales, expected_cash, actual_cash, diff_amount, status) VALUES
(1, 1, 'SH2026111301', 1, 3, '赵敏', '2026-11-13 08:00:00', '2026-11-13 16:00:00', 500.00, 320.50, 1280.30, 210.00, 268.50, 820.50, 820.50, 0.00, 'CLOSED'),
(2, 1, 'SH2026111401', 1, 3, '赵敏', '2026-11-14 08:00:00', NULL, 500.00, 0.00, 0.00, 0.00, 0.00, 500.00, 0.00, 0.00, 'OPEN');

INSERT IGNORE INTO day_settlement (tenant_id, store_id, settle_date, order_count, sales_amount, refund_amount, net_amount, cash_amount, wechat_amount, alipay_amount, wallet_amount, diff_amount, status) VALUES
(1, 1, '2026-11-13', 42, 2680.40, 0.00, 2680.40, 320.50, 1880.30, 210.00, 268.50, 0.00, 'CONFIRMED'),
(1, 2, '2026-11-13', 18, 980.60, 36.00, 944.60, 120.00, 620.60, 100.00, 104.00, 0.00, 'CONFIRMED');

-- ---------- 结算 ----------
INSERT IGNORE INTO settlement_statement (id, tenant_id, statement_no, type, partner_id, partner_name, period_start, period_end, total_amount, deduction_amount, payable_amount, status, remark, created_at) VALUES
(1, 1, 'SS2026100001', 'SUPPLIER', 1, '上海鲜食供应链有限公司', '2026-10-01', '2026-10-31', 12800.00, 300.00, 12500.00, 'PENDING', '10 月鲜食供货对账', '2026-11-01 09:00:00'),
(2, 1, 'FS2026100001', 'FRANCHISE', 1, '张明加盟商', '2026-10-01', '2026-10-31', 58600.00, 0.00, 2930.00, 'CONFIRMED', '10 月加盟分账（销售额 5%）', '2026-11-01 09:10:00');

INSERT IGNORE INTO settlement_item (tenant_id, statement_id, ref_type, ref_id, ref_no, amount, deduction, payable) VALUES
(1, 1, 'SUPPLY', 1, 'SO20261012001', 8500.00, 200.00, 8300.00),
(1, 1, 'SUPPLY', 2, 'SO20261025002', 4300.00, 100.00, 4200.00),
(1, 2, 'FRANCHISE', 1, 'COMM202610', 58600.00, 0.00, 2930.00);

INSERT IGNORE INTO settlement_invoice (tenant_id, statement_id, invoice_no, amount, file_url, status) VALUES
(1, 2, 'INV2026100001', 2930.00, '/static/invoice/fs202610.pdf', 'ISSUED');

INSERT IGNORE INTO franchise_commission (tenant_id, franchisee_id, store_id, period, sales_amount, commission_type, commission_value, commission_amount, status) VALUES
(1, 1, 2, '2026-10', 58600.00, 'SALES', 5.00, 2930.00, 'SETTLED'),
(1, 1, 2, '2026-11', 32680.00, 'SALES', 5.00, 1634.00, 'PENDING');

INSERT IGNORE INTO delivery_rule (tenant_id, store_id, min_order_amount, delivery_fee, free_threshold, radius_m, time_slots, enabled) VALUES
(1, 1, 20.00, 3.00, 49.00, 3000, JSON_ARRAY('09:00-12:00','12:00-18:00','18:00-22:00'), 1),
(1, 2, 20.00, 3.00, 59.00, 2500, JSON_ARRAY('09:00-12:00','14:00-22:00'), 1);

-- ---------- CMS ----------
INSERT IGNORE INTO cms_article (id, tenant_id, category, title, title_en, summary, cover, content, author, status, published_at, seo_keywords, seo_description) VALUES
(1, 1, 'BRAND', '悦享便利第 200 家门店落地徐汇', 'Yuexiang Opens 200th Store in Xuhui', '社区便利网络持续加密，鲜食与自有品牌成为增长引擎。', '/static/news/200-store.jpg', '<p>2026 年 10 月，悦享便利第 200 家门店在上海徐汇正式开业。围绕“社区 5 分钟生活圈”，悦享持续加码鲜食、短保与自有品牌。</p>', '品牌中心', 'PUBLISHED', '2026-10-28 10:00:00', '悦享便利,第200家门店,社区零售', '悦享便利第 200 家门店落地徐汇，社区便利网络持续加密。'),
(2, 1, 'FRESH', '冬季鲜食上新：热压三明治系列', 'Winter Fresh Food: Hot Pressed Sandwiches', '现烤热压，3 款口味同步上市。', '/static/news/sandwich.jpg', '<p>冬季鲜食系列上新，热压三明治采用当日现烤面包与低温锁鲜食材，门店每日限量供应。</p>', '商品中心', 'PUBLISHED', '2026-11-05 09:30:00', '鲜食,三明治,便利店新品', '悦享便利冬季鲜食上新，热压三明治系列三款口味同步上市。'),
(3, 1, 'FRANCHISE', '加盟政策说明会每月开放报名', 'Franchise Info Session', '面向上海及长三角区域，提供选址、供应链与数字化一体化支持。', '/static/news/franchise.jpg', '<p>悦享便利加盟说明会每月举办，覆盖选址评估、门店装修、商品结构与数字化系统培训。</p>', '加盟事业部', 'PUBLISHED', '2026-11-08 14:00:00', '便利店加盟,上海,社区零售', '悦享便利加盟说明会每月开放报名，提供一体化开店支持。');

INSERT IGNORE INTO cms_banner (tenant_id, position, title, image, link, sort_no) VALUES
(1, 'PORTAL', '社区 5 分钟生活圈', '/static/banner/portal-1.jpg', '/stores', 10),
(1, 'PORTAL', '冬季鲜食上新', '/static/banner/portal-2.jpg', '/products', 20),
(1, 'MINI', '鲜食 8 折', '/static/banner/mini-1.jpg', '/pages/product/index', 10),
(1, 'H5', '新人礼包', '/static/banner/h5-1.jpg', '/h5/newcomer', 10);

INSERT IGNORE INTO cms_franchise_application (tenant_id, company, contact, phone, city, amount, remark, status, created_at) VALUES
(1, '上海启程贸易有限公司', '刘先生', '13700009999', '上海', '50-100万', '希望在地铁沿线选址', 'PENDING', '2026-11-12 15:30:00');

INSERT IGNORE INTO tenant_notice (tenant_id, title, content, type, published_at) VALUES
(1, '11 月鲜食订货窗口调整', '自 11 月 15 日起，鲜食订货截止时间调整为每日 16:00。', 'NOTICE', '2026-11-13 09:00:00'),
(1, '临期商品专项检查', '请各门店于本周五前完成临期专区自查。', 'TASK', '2026-11-12 10:00:00');

-- ---------- 公众号 ----------
INSERT IGNORE INTO mp_account (id, tenant_id, name, app_id, app_secret, server_url, token, aes_key, oauth_domain, js_domain, status, api_quota_used) VALUES
(1, 1, '悦享便利服务号', 'wx_demo_yxbl_001', '', 'https://api.yxbl.example.com/admin-api/mp/open/wx_demo_yxbl_001', 'yxbl_token_2026', '', 'h5.yxbl.example.com', 'h5.yxbl.example.com', 'ACTIVE', 1280);

INSERT IGNORE INTO mp_mini_app (tenant_id, app_type, name, app_id, app_secret, request_domain, upload_domain, download_domain) VALUES
(1, 'USER', '悦享便利用户端', 'wx_mini_user_demo', '', 'https://api.yxbl.example.com', 'https://api.yxbl.example.com', 'https://cdn.yxbl.example.com'),
(1, 'STORE', '悦享便利门店端', 'wx_mini_store_demo', '', 'https://api.yxbl.example.com', 'https://api.yxbl.example.com', 'https://cdn.yxbl.example.com'),
(1, 'SUPPLIER', '悦享便利供应商端', 'wx_mini_supplier_demo', '', 'https://api.yxbl.example.com', 'https://api.yxbl.example.com', 'https://cdn.yxbl.example.com'),
(1, 'FRANCHISEE', '悦享便利加盟商端', 'wx_mini_franchisee_demo', '', 'https://api.yxbl.example.com', 'https://api.yxbl.example.com', 'https://cdn.yxbl.example.com');

INSERT IGNORE INTO mp_tag (tenant_id, account_id, wx_tag_id, name, fans_count, sync_status) VALUES
(1, 1, 101, '鲜食会员', 2, 'SYNCED'), (1, 1, 102, '高活跃', 1, 'SYNCED');

INSERT IGNORE INTO mp_fans (tenant_id, account_id, openid, unionid, nickname, avatar, gender, region, remark, subscribe_status, subscribe_time, tags, synced_at) VALUES
(1, 1, 'oUx1demo001', 'union_demo_001', '小悦', NULL, 1, '上海', '鲜食高频会员', 1, '2025-03-02 10:20:00', JSON_ARRAY('鲜食会员','高活跃'), '2026-11-14 08:00:00'),
(1, 1, 'oUx1demo002', 'union_demo_002', '阿哲', NULL, 1, '上海', '饮料偏好', 1, '2025-08-11 09:10:00', JSON_ARRAY('鲜食会员'), '2026-11-14 08:00:00'),
(1, 1, 'oUx1demo003', 'union_demo_003', 'Lily', NULL, 2, '上海', NULL, 0, '2026-10-05 16:20:00', NULL, '2026-11-14 08:00:00');

INSERT IGNORE INTO mp_message (tenant_id, account_id, openid, msg_type, direction, content, raw_payload, err_code, created_at) VALUES
(1, 1, 'oUx1demo001', 'text', 'IN', '今天有什么鲜食推荐？', JSON_OBJECT('MsgType','text','Content','今天有什么鲜食推荐？'), NULL, '2026-11-13 12:30:00'),
(1, 1, 'oUx1demo001', 'text', 'OUT', '热压三明治今日 8 折，欢迎到店～', JSON_OBJECT('reply','热压三明治今日 8 折'), NULL, '2026-11-13 12:30:05'),
(1, 1, 'oUx1demo002', 'event', 'IN', 'subscribe', JSON_OBJECT('Event','subscribe'), NULL, '2026-11-12 09:00:00');

INSERT IGNORE INTO mp_template (tenant_id, account_id, wx_template_id, title, content, example, primary_industry, secondary_industry) VALUES
(1, 1, 'TPL_ORDER_PAID', '订单支付成功通知', '订单编号：{{character_string1.DATA}}\n支付金额：{{amount2.DATA}}\n支付时间：{{time3.DATA}}', '订单编号：ON202611130002；支付金额：21.50元', 'IT科技/互联网|电子商务', '消费品/食品'),
(1, 1, 'TPL_PICKUP_READY', '待取货提醒', '订单编号：{{character_string1.DATA}}\n取货门店：{{thing2.DATA}}', '订单编号：ON202611140003；取货门店：静安旗舰店', 'IT科技/互联网|电子商务', '消费品/食品');

INSERT IGNORE INTO mp_auto_reply (tenant_id, account_id, scene, match_type, keyword, reply_type, content, enabled) VALUES
(1, 1, 'FOLLOW', 'EXACT', NULL, 'TEXT', '欢迎关注悦享便利！回复“门店”查询附近门店，回复“优惠”领取本周券包。', 1),
(1, 1, 'MESSAGE', 'EXACT', NULL, 'TEXT', '您好，客服会尽快回复。如需查询订单，请在会员中心查看。', 1),
(1, 1, 'KEYWORD', 'EXACT', '门店', 'TEXT', '点击菜单“门店查询”获取附近门店与营业时间。', 1),
(1, 1, 'KEYWORD', 'PART', '优惠', 'TEXT', '本周鲜食 8 折，满 30 减 5，详见小程序首页。', 1);

INSERT IGNORE INTO mp_material (tenant_id, account_id, media_type, name, url, thumb_url, media_id, permanent, size) VALUES
(1, 1, 'IMAGE', '冬季鲜食主视觉', '/static/mp/winter-fresh.jpg', NULL, 'MEDIA_IMG_001', 1, 512000),
(1, 1, 'IMAGE', '门店开业海报', '/static/mp/store-open.jpg', NULL, 'MEDIA_IMG_002', 1, 486000),
(1, 1, 'VIDEO', '品牌宣传片 15s', '/static/mp/brand.mp4', '/static/mp/brand-thumb.jpg', 'MEDIA_VIDEO_001', 1, 5200000);

INSERT IGNORE INTO mp_draft (tenant_id, account_id, title, author, source_url, cover_url, digest, content, status, created_at) VALUES
(1, 1, '冬季鲜食上新｜热压三明治系列', '品牌中心', '', '/static/mp/winter-fresh.jpg', '现烤热压，三款口味同步上市。', '<p>冬季鲜食系列上新，欢迎到店品尝。</p>', 'DRAFT', '2026-11-13 15:00:00');

INSERT IGNORE INTO mp_publish (tenant_id, account_id, draft_id, title, status, fail_reason, msg_id, publish_at, read_count, created_at) VALUES
(1, 1, 1, '第 200 家门店落地徐汇', 'SUCCESS', NULL, 'MSG20261028001', '2026-10-28 10:05:00', 3680, '2026-10-28 10:00:00'),
(1, 1, NULL, '双十一鲜食券包', 'FAILED', '素材图片未通过微信审核', NULL, NULL, 0, '2026-11-09 09:30:00');

INSERT IGNORE INTO mp_stat (tenant_id, account_id, stat_date, new_users, total_users, msg_count, api_count) VALUES
(1, 1, '2026-11-07', 12, 12860, 86, 1420),
(1, 1, '2026-11-08', 18, 12878, 102, 1560),
(1, 1, '2026-11-09', 9, 12887, 78, 1380),
(1, 1, '2026-11-10', 22, 12909, 120, 1720),
(1, 1, '2026-11-11', 35, 12944, 186, 2210),
(1, 1, '2026-11-12', 16, 12960, 96, 1580),
(1, 1, '2026-11-13', 14, 12974, 88, 1490);

INSERT IGNORE INTO mp_takeover_check (tenant_id, account_id, server_url_configured, fans_active, menu_snapshot, publish_snapshot, conclusion, remark) VALUES
(1, 1, 1, 1, JSON_OBJECT('menus', 3), JSON_OBJECT('latest', '2026-10-28'), 'OCCUPIED', '原服务商仍在接管，计划 11 月 20 日停旧回调后切换');

-- ---------- 租户 2 示例业务 ----------
INSERT IGNORE INTO product_category (id, tenant_id, parent_id, name, sort_no, near_expiry_days) VALUES (101, 2, 0, '鲜食', 10, 2), (102, 2, 0, '饮料', 20, 7);
INSERT IGNORE INTO product_spu (id, tenant_id, spu_code, name, category_id, unit, spec, storage_condition, shelf_life_days, online_sale, status, price_retail, price_member) VALUES
(101, 2, 'FSPU001', '鲜切果盒', 101, '盒', '250g', 'COLD', 2, 1, 'ON_SALE', 15.00, 13.80);
INSERT IGNORE INTO product_sku (id, tenant_id, spu_id, sku_code, barcode, spec, unit, price_retail, price_member, status) VALUES
(101, 2, 101, 'FSKU001', '6900000000101', '250g', '盒', 15.00, 13.80, 'ON_SALE');
INSERT IGNORE INTO stock (tenant_id, owner_type, owner_id, sku_id, qty, safety_stock) VALUES (2, 'STORE', 101, 101, 60, 10);

SET FOREIGN_KEY_CHECKS = 1;
