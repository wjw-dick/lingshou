-- 零售便利连锁 SaaS 平台 数据库结构 V1.0
-- MySQL 8.0 / utf8mb4 / 所有租户业务表强制 tenant_id
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 平台侧表（无 tenant_id，租户插件白名单）
-- ============================================================
CREATE TABLE IF NOT EXISTS platform_plan (
  plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  price DECIMAL(12,2) NOT NULL DEFAULT 0,
  billing_cycle VARCHAR(16) NOT NULL DEFAULT 'YEAR',
  seat_limit INT NOT NULL DEFAULT 20,
  store_limit INT NOT NULL DEFAULT 10,
  mp_account_limit INT NOT NULL DEFAULT 1,
  storage_quota_gb INT NOT NULL DEFAULT 20,
  sms_quota INT NOT NULL DEFAULT 10000,
  api_quota INT NOT NULL DEFAULT 1000000,
  trial_days INT NOT NULL DEFAULT 14,
  features JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐';

CREATE TABLE IF NOT EXISTS platform_tenant (
  tenant_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  industry VARCHAR(64) DEFAULT '连锁便利店',
  contact VARCHAR(64),
  phone VARCHAR(32),
  plan_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'TRIAL' COMMENT 'TRIAL/ACTIVE/OVERDUE/SUSPENDED/EXPIRED',
  trial_end_at DATETIME NULL,
  expire_at DATETIME NULL,
  seat_limit INT NOT NULL DEFAULT 20,
  store_limit INT NOT NULL DEFAULT 10,
  mp_account_limit INT NOT NULL DEFAULT 1,
  storage_quota_gb INT NOT NULL DEFAULT 20,
  sms_quota INT NOT NULL DEFAULT 10000,
  api_quota INT NOT NULL DEFAULT 1000000,
  overdue_strategy VARCHAR(16) NOT NULL DEFAULT 'LIMIT_WRITE',
  remark VARCHAR(255),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_status (status),
  KEY idx_plan (plan_id),
  KEY idx_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

CREATE TABLE IF NOT EXISTS platform_tenant_quota_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  seat_used INT NOT NULL DEFAULT 0,
  store_used INT NOT NULL DEFAULT 0,
  mp_used INT NOT NULL DEFAULT 0,
  storage_used_gb DECIMAL(12,2) NOT NULL DEFAULT 0,
  sms_used INT NOT NULL DEFAULT 0,
  api_used BIGINT NOT NULL DEFAULT 0,
  stat_date DATE NOT NULL,
  UNIQUE KEY uk_tenant_date (tenant_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户配额使用';

CREATE TABLE IF NOT EXISTS platform_bill (
  bill_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  bill_no VARCHAR(32) NOT NULL UNIQUE,
  period VARCHAR(16) NOT NULL,
  item VARCHAR(64) NOT NULL,
  quantity DECIMAL(12,2) NOT NULL DEFAULT 1,
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/OVERDUE',
  due_date DATE NULL,
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant (tenant_id, created_at),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单';

CREATE TABLE IF NOT EXISTS platform_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(64),
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64),
  target_id VARCHAR(64),
  tenant_id BIGINT,
  detail TEXT,
  ip VARCHAR(64),
  result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant (tenant_id, created_at),
  KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台审计';

CREATE TABLE IF NOT EXISTS platform_tenant_admin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  is_primary TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户主管理员';

CREATE TABLE IF NOT EXISTS platform_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(64),
  role_code VARCHAR(32) NOT NULL DEFAULT 'PLATFORM_OPERATOR' COMMENT 'PLATFORM_ADMIN/PLATFORM_OPERATOR/PLATFORM_FINANCE',
  phone VARCHAR(32),
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台账号';

CREATE TABLE IF NOT EXISTS platform_announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  content TEXT,
  target_type VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/TENANT/PLAN',
  target_ids JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台公告';

CREATE TABLE IF NOT EXISTS platform_announcement_read (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  announcement_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ann_tenant (announcement_id, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告已读';

-- ============================================================
-- 租户系统表（tenant_id 强制）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(64),
  phone VARCHAR(32),
  email VARCHAR(64),
  role_code VARCHAR(32) NOT NULL DEFAULT 'STAFF',
  store_id BIGINT NULL,
  supplier_id BIGINT NULL,
  franchisee_id BIGINT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_tenant_role (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户用户';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  permissions JSON NULL,
  data_scope VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/STORE/SELF',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_role (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  end_type VARCHAR(16) NOT NULL DEFAULT 'ADMIN' COMMENT 'ADMIN/PORTAL/TABBAR/H5/MP',
  parent_id BIGINT NOT NULL DEFAULT 0,
  title VARCHAR(64) NOT NULL,
  title_en VARCHAR(64),
  icon VARCHAR(64),
  path VARCHAR(128),
  component VARCHAR(128),
  permission VARCHAR(64),
  sort_no INT NOT NULL DEFAULT 0,
  visible TINYINT NOT NULL DEFAULT 1,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_end (tenant_id, end_type, parent_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可视化菜单';

CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  config_key VARCHAR(64) NOT NULL,
  config_value TEXT,
  group_code VARCHAR(32) NOT NULL DEFAULT 'GENERAL',
  encrypted TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(255),
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_key (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数';

CREATE TABLE IF NOT EXISTS sys_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT,
  username VARCHAR(64),
  module VARCHAR(64),
  action VARCHAR(64),
  target_type VARCHAR(64),
  target_id VARCHAR(64),
  detail TEXT,
  ip VARCHAR(64),
  user_agent VARCHAR(255),
  result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_created (tenant_id, created_at),
  KEY idx_tenant_module (tenant_id, module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户审计日志';

CREATE TABLE IF NOT EXISTS sys_language (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(16) NOT NULL,
  name VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_no INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_lang (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语言';

CREATE TABLE IF NOT EXISTS sys_message_channel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  config JSON NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_channel (tenant_id, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通道';

CREATE TABLE IF NOT EXISTS sys_sms_channel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  provider VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  config JSON NULL,
  last_test_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_provider (tenant_id, provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信渠道';

CREATE TABLE IF NOT EXISTS sys_message_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  node VARCHAR(64) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  template_id VARCHAR(128),
  title VARCHAR(128),
  content TEXT,
  field_mapping VARCHAR(512),
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_node_channel (tenant_id, node, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板';

CREATE TABLE IF NOT EXISTS sys_sso_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  protocol VARCHAR(16) NOT NULL DEFAULT 'OIDC',
  server_url VARCHAR(255),
  service_url VARCHAR(255),
  client_id VARCHAR(128),
  client_secret VARCHAR(255),
  account_mapping VARCHAR(64) DEFAULT 'username',
  auto_create TINYINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_sso (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSO 配置';

CREATE TABLE IF NOT EXISTS sys_storage_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  driver VARCHAR(16) NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/S3/OSS/COS/MINIO',
  config JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  last_test_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_storage (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象存储配置';

CREATE TABLE IF NOT EXISTS sys_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  path VARCHAR(512) NOT NULL,
  url VARCHAR(512) NOT NULL,
  size BIGINT NOT NULL DEFAULT 0,
  content_type VARCHAR(128),
  biz_type VARCHAR(32) DEFAULT 'GENERAL',
  driver VARCHAR(16) DEFAULT 'LOCAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件';

-- ============================================================
-- 公众号管理（11 子页）
-- ============================================================
CREATE TABLE IF NOT EXISTS mp_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  app_id VARCHAR(64) NOT NULL,
  app_secret VARCHAR(255),
  server_url VARCHAR(255),
  token VARCHAR(128),
  aes_key VARCHAR(128),
  oauth_domain VARCHAR(128),
  js_domain VARCHAR(128),
  qr_url VARCHAR(512),
  api_quota_used INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_appid (tenant_id, app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号账号';

CREATE TABLE IF NOT EXISTS mp_mini_app (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  app_type VARCHAR(16) NOT NULL COMMENT 'USER/STORE/SUPPLIER/FRANCHISEE',
  name VARCHAR(64) NOT NULL,
  app_id VARCHAR(64),
  app_secret VARCHAR(255),
  request_domain VARCHAR(128),
  upload_domain VARCHAR(128),
  download_domain VARCHAR(128),
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_app_type (tenant_id, app_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序台账';

CREATE TABLE IF NOT EXISTS mp_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  wx_tag_id BIGINT,
  name VARCHAR(64) NOT NULL,
  fans_count INT NOT NULL DEFAULT 0,
  sync_status VARCHAR(16) NOT NULL DEFAULT 'SYNCED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_account_name (tenant_id, account_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号标签';

CREATE TABLE IF NOT EXISTS mp_fans (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  openid VARCHAR(64) NOT NULL,
  unionid VARCHAR(64),
  nickname VARCHAR(128),
  avatar VARCHAR(512),
  gender INT DEFAULT 0,
  region VARCHAR(128),
  remark VARCHAR(255),
  subscribe_status INT NOT NULL DEFAULT 1,
  subscribe_time DATETIME NULL,
  tags JSON NULL,
  synced_at DATETIME NULL,
  UNIQUE KEY uk_tenant_account_openid (tenant_id, account_id, openid),
  KEY idx_unionid (tenant_id, unionid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号粉丝';

CREATE TABLE IF NOT EXISTS mp_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  openid VARCHAR(64),
  msg_type VARCHAR(32) NOT NULL DEFAULT 'text',
  direction VARCHAR(16) NOT NULL DEFAULT 'IN' COMMENT 'IN/OUT',
  content TEXT,
  raw_payload JSON NULL,
  err_code INT,
  err_msg VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号消息流水';

CREATE TABLE IF NOT EXISTS mp_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  wx_template_id VARCHAR(128) NOT NULL,
  title VARCHAR(128),
  content TEXT,
  example TEXT,
  primary_industry VARCHAR(64),
  secondary_industry VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_account_tpl (tenant_id, account_id, wx_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号模板（只读同步）';

CREATE TABLE IF NOT EXISTS mp_auto_reply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  scene VARCHAR(16) NOT NULL COMMENT 'FOLLOW/MESSAGE/KEYWORD',
  match_type VARCHAR(16) NOT NULL DEFAULT 'EXACT' COMMENT 'EXACT/PART',
  keyword VARCHAR(64),
  reply_type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
  content TEXT,
  media_id VARCHAR(128),
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_scene (tenant_id, account_id, scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动回复';

CREATE TABLE IF NOT EXISTS mp_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  media_type VARCHAR(16) NOT NULL COMMENT 'IMAGE/VOICE/VIDEO',
  name VARCHAR(128) NOT NULL,
  url VARCHAR(512),
  thumb_url VARCHAR(512),
  media_id VARCHAR(128),
  permanent TINYINT NOT NULL DEFAULT 1,
  size BIGINT DEFAULT 0,
  expires_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_type (tenant_id, account_id, media_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号素材';

CREATE TABLE IF NOT EXISTS mp_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  author VARCHAR(64),
  source_url VARCHAR(512),
  cover_url VARCHAR(512),
  digest VARCHAR(255),
  content MEDIUMTEXT,
  articles JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图文草稿';

CREATE TABLE IF NOT EXISTS mp_publish (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  draft_id BIGINT,
  title VARCHAR(128),
  status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHING' COMMENT 'PUBLISHING/SUCCESS/FAILED',
  fail_reason VARCHAR(255),
  msg_id VARCHAR(128),
  publish_at DATETIME NULL,
  read_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图文发表记录';

CREATE TABLE IF NOT EXISTS mp_stat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  stat_date DATE NOT NULL,
  new_users INT NOT NULL DEFAULT 0,
  total_users INT NOT NULL DEFAULT 0,
  msg_count INT NOT NULL DEFAULT 0,
  api_count INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_account_date (tenant_id, account_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号数据统计';

CREATE TABLE IF NOT EXISTS mp_takeover_check (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  server_url_configured TINYINT NOT NULL DEFAULT 0,
  fans_active TINYINT NOT NULL DEFAULT 0,
  menu_snapshot JSON NULL,
  publish_snapshot JSON NULL,
  conclusion VARCHAR(32) NOT NULL COMMENT 'FREE/OCCUPIED/PENDING',
  remark VARCHAR(255),
  checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接号占用检查';

-- ============================================================
-- 门店 / 商品 / 供应链
-- ============================================================
CREATE TABLE IF NOT EXISTS store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(16) NOT NULL DEFAULT 'DIRECT' COMMENT 'DIRECT/FRANCHISE',
  region VARCHAR(64),
  address VARCHAR(255),
  lng DECIMAL(10,6),
  lat DECIMAL(10,6),
  business_hours VARCHAR(64) DEFAULT '07:00-23:00',
  phone VARCHAR(32),
  manager_name VARCHAR(64),
  manager_phone VARCHAR(32),
  delivery_radius_m INT NOT NULL DEFAULT 3000,
  delivery_enabled TINYINT NOT NULL DEFAULT 1,
  online_enabled TINYINT NOT NULL DEFAULT 1,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PAUSED/RENOVATING',
  opened_at DATE NULL,
  license_expire_at DATE NULL,
  images JSON NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_code (tenant_id, code),
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店';

CREATE TABLE IF NOT EXISTS store_license (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  license_type VARCHAR(32) NOT NULL,
  name VARCHAR(128),
  file_url VARCHAR(512),
  expire_at DATE,
  status VARCHAR(16) NOT NULL DEFAULT 'VALID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_store (tenant_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店证照';

CREATE TABLE IF NOT EXISTS store_audit_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  supervisor VARCHAR(64),
  check_date DATE,
  score INT,
  issues TEXT,
  photos JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店稽核';

CREATE TABLE IF NOT EXISTS product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(64) NOT NULL,
  name_en VARCHAR(64),
  icon VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 0,
  near_expiry_days INT NOT NULL DEFAULT 3,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_parent (tenant_id, parent_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

CREATE TABLE IF NOT EXISTS brand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  name_en VARCHAR(64),
  logo VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_brand (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌';

CREATE TABLE IF NOT EXISTS product_spu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  spu_code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  name_en VARCHAR(128),
  category_id BIGINT NOT NULL,
  brand_id BIGINT,
  unit VARCHAR(16) NOT NULL DEFAULT '件',
  spec VARCHAR(64),
  is_weight TINYINT NOT NULL DEFAULT 0,
  storage_condition VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/COLD/FROZEN',
  shelf_life_days INT NOT NULL DEFAULT 365,
  origin VARCHAR(128),
  ingredients TEXT,
  allergens VARCHAR(255),
  is_tobacco TINYINT NOT NULL DEFAULT 0,
  online_sale TINYINT NOT NULL DEFAULT 1,
  status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE',
  main_image VARCHAR(512),
  images JSON NULL,
  detail_html MEDIUMTEXT,
  detail_html_en MEDIUMTEXT,
  price_guide DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_retail DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_member DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_franchise DECIMAL(10,2) NOT NULL DEFAULT 0,
  tags JSON NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_spu_code (tenant_id, spu_code),
  KEY idx_tenant_category (tenant_id, category_id),
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SPU';

CREATE TABLE IF NOT EXISTS product_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  spu_id BIGINT NOT NULL,
  sku_code VARCHAR(32) NOT NULL,
  barcode VARCHAR(64),
  spec VARCHAR(64),
  unit VARCHAR(16) NOT NULL DEFAULT '件',
  is_weight TINYINT NOT NULL DEFAULT 0,
  cost_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_guide DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_retail DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_member DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_franchise DECIMAL(10,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_sku_code (tenant_id, sku_code),
  KEY idx_tenant_spu (tenant_id, spu_id),
  KEY idx_tenant_barcode (tenant_id, barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SKU';

CREATE TABLE IF NOT EXISTS product_barcode (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  barcode VARCHAR(64) NOT NULL,
  UNIQUE KEY uk_tenant_barcode (tenant_id, barcode),
  KEY idx_tenant_sku (tenant_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一品多码';

CREATE TABLE IF NOT EXISTS store_sku_price (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  price_retail DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_member DECIMAL(10,2) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_store_sku (tenant_id, store_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店差异价';

CREATE TABLE IF NOT EXISTS promotion (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL COMMENT 'DISCOUNT/SECOND_HALF/GROUP/POINTS_EXCHANGE',
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  rule_json JSON NULL,
  store_scope JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_time (tenant_id, start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动';

CREATE TABLE IF NOT EXISTS promotion_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  promotion_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  original_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  promo_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount DECIMAL(5,2) NOT NULL DEFAULT 10,
  KEY idx_tenant_promo (tenant_id, promotion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动商品';

CREATE TABLE IF NOT EXISTS supplier (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  contact VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(255),
  category VARCHAR(64),
  settle_cycle_days INT NOT NULL DEFAULT 30,
  credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_supplier_code (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商';

CREATE TABLE IF NOT EXISTS supplier_qualification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  supplier_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  file_url VARCHAR(512),
  expire_at DATE,
  status VARCHAR(16) NOT NULL DEFAULT 'VALID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_supplier (tenant_id, supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商资质';

CREATE TABLE IF NOT EXISTS supplier_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  supplier_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  supply_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  audit_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_tenant_supplier_sku (tenant_id, supplier_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供货商品';

CREATE TABLE IF NOT EXISTS franchisee (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  contact VARCHAR(64),
  phone VARCHAR(32),
  level VARCHAR(16) NOT NULL DEFAULT 'A',
  commission_type VARCHAR(16) NOT NULL DEFAULT 'SALES' COMMENT 'SALES/PROFIT/FIXED',
  commission_value DECIMAL(10,2) NOT NULL DEFAULT 0,
  credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0,
  used_credit DECIMAL(12,2) NOT NULL DEFAULT 0,
  deposit DECIMAL(12,2) NOT NULL DEFAULT 0,
  contract_no VARCHAR(64),
  contract_start DATE,
  contract_end DATE,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_fr_code (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟商';

CREATE TABLE IF NOT EXISTS franchisee_store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  franchisee_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  join_date DATE,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_tenant_fr_store (tenant_id, franchisee_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟商门店';

CREATE TABLE IF NOT EXISTS franchisee_qualification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  franchisee_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  file_url VARCHAR(512),
  expire_at DATE,
  status VARCHAR(16) NOT NULL DEFAULT 'VALID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟商资质';

CREATE TABLE IF NOT EXISTS purchase_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  supplier_id BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/RECEIVING/DONE/CANCELLED',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  expected_at DATE,
  remark VARCHAR(255),
  created_by VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_po_no (tenant_id, order_no),
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单';

CREATE TABLE IF NOT EXISTS purchase_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  purchase_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL DEFAULT 0,
  received_qty INT NOT NULL DEFAULT 0,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  KEY idx_tenant_purchase (tenant_id, purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单明细';

CREATE TABLE IF NOT EXISTS supply_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  source_type VARCHAR(16) NOT NULL DEFAULT 'HQ' COMMENT 'HQ/SUPPLIER/FRANCHISE',
  supplier_id BIGINT,
  store_id BIGINT NOT NULL,
  franchisee_id BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/SHIPPED/PARTIAL/RECEIVED/CANCELLED',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  expected_at DATE,
  logistics_no VARCHAR(64),
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_so_no (tenant_id, order_no),
  KEY idx_tenant_store (tenant_id, store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供货单';

CREATE TABLE IF NOT EXISTS supply_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  supply_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL DEFAULT 0,
  shipped_qty INT NOT NULL DEFAULT 0,
  received_qty INT NOT NULL DEFAULT 0,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  KEY idx_tenant_supply (tenant_id, supply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供货单明细';

CREATE TABLE IF NOT EXISTS receive_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  receive_no VARCHAR(32) NOT NULL,
  supply_id BIGINT,
  store_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED',
  total_qty INT NOT NULL DEFAULT 0,
  diff_qty INT NOT NULL DEFAULT 0,
  cold_chain_temp DECIMAL(5,2),
  receiver VARCHAR(64),
  received_at DATETIME NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_ro_no (tenant_id, receive_no),
  KEY idx_tenant_store (tenant_id, store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货单';

CREATE TABLE IF NOT EXISTS receive_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  receive_id BIGINT NOT NULL,
  supply_item_id BIGINT,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL DEFAULT 0,
  actual_qty INT NOT NULL DEFAULT 0,
  diff_qty INT NOT NULL DEFAULT 0,
  production_date DATE,
  expiry_date DATE,
  cold_chain_temp DECIMAL(5,2),
  batch_no VARCHAR(64),
  KEY idx_tenant_receive (tenant_id, receive_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货单明细';

CREATE TABLE IF NOT EXISTS difference_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  diff_no VARCHAR(32) NOT NULL,
  receive_id BIGINT,
  supply_id BIGINT,
  store_id BIGINT,
  type VARCHAR(32) NOT NULL DEFAULT 'QTY' COMMENT 'QTY/DATE/TEMPERATURE/OTHER',
  content VARCHAR(512),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_diff_no (tenant_id, diff_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='差异单';

CREATE TABLE IF NOT EXISTS stock (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  owner_type VARCHAR(16) NOT NULL DEFAULT 'STORE' COMMENT 'STORE/WAREHOUSE',
  owner_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL DEFAULT 0,
  locked_qty INT NOT NULL DEFAULT 0,
  safety_stock INT NOT NULL DEFAULT 10,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_owner_sku (tenant_id, owner_type, owner_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存';

CREATE TABLE IF NOT EXISTS stock_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  owner_type VARCHAR(16) NOT NULL DEFAULT 'STORE',
  owner_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NOT NULL,
  production_date DATE,
  expiry_date DATE,
  qty INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_expiry (tenant_id, owner_type, owner_id, expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存批次';

CREATE TABLE IF NOT EXISTS stock_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  owner_type VARCHAR(16) NOT NULL DEFAULT 'STORE',
  owner_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  biz_type VARCHAR(32) NOT NULL COMMENT 'RECEIVE/SALE/TRANSFER_IN/TRANSFER_OUT/CHECK/LOSS/REFUND',
  change_qty INT NOT NULL DEFAULT 0,
  before_qty INT NOT NULL DEFAULT 0,
  after_qty INT NOT NULL DEFAULT 0,
  ref_type VARCHAR(32),
  ref_id BIGINT,
  remark VARCHAR(255),
  created_by VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_owner (tenant_id, owner_type, owner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';

CREATE TABLE IF NOT EXISTS transfer_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  transfer_no VARCHAR(32) NOT NULL,
  from_type VARCHAR(16) NOT NULL DEFAULT 'WAREHOUSE',
  from_id BIGINT NOT NULL,
  to_type VARCHAR(16) NOT NULL DEFAULT 'STORE',
  to_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/COMPLETED/REJECTED',
  total_qty INT NOT NULL DEFAULT 0,
  remark VARCHAR(255),
  applied_by VARCHAR(64),
  approved_by VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_tr_no (tenant_id, transfer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨单';

CREATE TABLE IF NOT EXISTS transfer_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  transfer_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL DEFAULT 0,
  batch_no VARCHAR(64),
  KEY idx_tenant_transfer (tenant_id, transfer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨明细';

CREATE TABLE IF NOT EXISTS stock_check (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  check_no VARCHAR(32) NOT NULL,
  store_id BIGINT NOT NULL,
  check_type VARCHAR(16) NOT NULL DEFAULT 'FULL' COMMENT 'FULL/SAMPLE/CYCLE',
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED',
  total_diff_qty INT NOT NULL DEFAULT 0,
  remark VARCHAR(255),
  created_by VARCHAR(64),
  approved_by VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_sc_no (tenant_id, check_no),
  KEY idx_tenant_store (tenant_id, store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单';

CREATE TABLE IF NOT EXISTS stock_check_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  check_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  book_qty INT NOT NULL DEFAULT 0,
  actual_qty INT NOT NULL DEFAULT 0,
  diff_qty INT NOT NULL DEFAULT 0,
  reason VARCHAR(255),
  KEY idx_tenant_check (tenant_id, check_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细';

CREATE TABLE IF NOT EXISTS loss_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  loss_no VARCHAR(32) NOT NULL,
  store_id BIGINT NOT NULL,
  reason_type VARCHAR(16) NOT NULL COMMENT 'NEAR_EXPIRY/DAMAGE/EXPIRED/OTHER',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  total_qty INT NOT NULL DEFAULT 0,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  photos JSON NULL,
  remark VARCHAR(255),
  created_by VARCHAR(64),
  approved_by VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_loss_no (tenant_id, loss_no),
  KEY idx_tenant_store (tenant_id, store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报损单';

CREATE TABLE IF NOT EXISTS loss_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  loss_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  batch_no VARCHAR(64),
  qty INT NOT NULL DEFAULT 0,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  KEY idx_tenant_loss (tenant_id, loss_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报损明细';

-- ============================================================
-- 销售 / 会员 / 营销
-- ============================================================
CREATE TABLE IF NOT EXISTS sales_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  channel VARCHAR(16) NOT NULL DEFAULT 'ONLINE' COMMENT 'ONLINE/POS',
  store_id BIGINT NOT NULL,
  member_id BIGINT,
  delivery_type VARCHAR(16) NOT NULL DEFAULT 'PICKUP' COMMENT 'PICKUP/DELIVERY',
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING_PAY' COMMENT 'PENDING_PAY/PENDING_ACCEPT/PICKING/WAIT_PICKUP/DELIVERING/COMPLETED/CANCELLED/REFUNDED',
  pay_status VARCHAR(16) NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/REFUNDED/PARTIAL_REFUND',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  freight_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  member_coupon_id BIGINT,
  points_used INT NOT NULL DEFAULT 0,
  points_discount DECIMAL(10,2) NOT NULL DEFAULT 0,
  wallet_paid DECIMAL(12,2) NOT NULL DEFAULT 0,
  payment_method VARCHAR(16) COMMENT 'WECHAT/ALIPAY/WALLET/CASH',
  delivery_name VARCHAR(64),
  delivery_phone VARCHAR(32),
  delivery_address VARCHAR(255),
  expected_time VARCHAR(32),
  accept_deadline DATETIME NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at DATETIME NULL,
  completed_at DATETIME NULL,
  UNIQUE KEY uk_tenant_order_no (tenant_id, order_no),
  KEY idx_tenant_store_created (tenant_id, store_id, created_at),
  KEY idx_tenant_member (tenant_id, member_id, created_at),
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单';

CREATE TABLE IF NOT EXISTS sales_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  sku_name VARCHAR(128) NOT NULL,
  barcode VARCHAR(64),
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  qty INT NOT NULL DEFAULT 1,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  KEY idx_tenant_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

CREATE TABLE IF NOT EXISTS refund_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  refund_no VARCHAR(32) NOT NULL,
  order_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL DEFAULT 'FULL' COMMENT 'FULL/PARTIAL/OUT_OF_STOCK',
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  reason VARCHAR(255),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/REFUNDED',
  items JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_refund_no (tenant_id, refund_no),
  KEY idx_tenant_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单';

CREATE TABLE IF NOT EXISTS pos_shift (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shift_no VARCHAR(32) NOT NULL,
  store_id BIGINT NOT NULL,
  cashier_id BIGINT,
  cashier_name VARCHAR(64),
  start_at DATETIME NOT NULL,
  end_at DATETIME NULL,
  opening_cash DECIMAL(12,2) NOT NULL DEFAULT 0,
  cash_sales DECIMAL(12,2) NOT NULL DEFAULT 0,
  wechat_sales DECIMAL(12,2) NOT NULL DEFAULT 0,
  alipay_sales DECIMAL(12,2) NOT NULL DEFAULT 0,
  wallet_sales DECIMAL(12,2) NOT NULL DEFAULT 0,
  expected_cash DECIMAL(12,2) NOT NULL DEFAULT 0,
  actual_cash DECIMAL(12,2) NOT NULL DEFAULT 0,
  diff_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  diff_reason VARCHAR(255),
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  UNIQUE KEY uk_tenant_shift_no (tenant_id, shift_no),
  KEY idx_tenant_store (tenant_id, store_id, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交接班';

CREATE TABLE IF NOT EXISTS day_settlement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  settle_date DATE NOT NULL,
  order_count INT NOT NULL DEFAULT 0,
  sales_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  net_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  cash_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  wechat_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  alipay_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  wallet_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  diff_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  diff_reason VARCHAR(255),
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_store_date (tenant_id, store_id, settle_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日结';

CREATE TABLE IF NOT EXISTS invoice_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  member_id BIGINT,
  title VARCHAR(128),
  tax_no VARCHAR(64),
  email VARCHAR(128),
  amount DECIMAL(12,2),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ISSUED/REJECTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票申请';

CREATE TABLE IF NOT EXISTS order_evaluation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  member_id BIGINT,
  store_id BIGINT,
  score INT NOT NULL DEFAULT 5,
  content VARCHAR(512),
  images JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_order_eval (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单评价';

CREATE TABLE IF NOT EXISTS member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  openid VARCHAR(64),
  unionid VARCHAR(64),
  phone VARCHAR(255),
  phone_mask VARCHAR(32),
  nickname VARCHAR(64),
  avatar VARCHAR(512),
  gender INT DEFAULT 0,
  birthday DATE,
  level_id BIGINT,
  points INT NOT NULL DEFAULT 0,
  wallet_balance DECIMAL(12,2) NOT NULL DEFAULT 0,
  total_consume DECIMAL(12,2) NOT NULL DEFAULT 0,
  order_count INT NOT NULL DEFAULT 0,
  tags JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_active_at DATETIME NULL,
  UNIQUE KEY uk_tenant_unionid (tenant_id, unionid),
  UNIQUE KEY uk_tenant_openid (tenant_id, openid),
  KEY idx_tenant_phone (tenant_id, phone_mask)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员';

CREATE TABLE IF NOT EXISTS member_level (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(16) NOT NULL,
  name VARCHAR(64) NOT NULL,
  min_points INT NOT NULL DEFAULT 0,
  discount DECIMAL(5,2) NOT NULL DEFAULT 10,
  benefits JSON NULL,
  UNIQUE KEY uk_tenant_level (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级';

CREATE TABLE IF NOT EXISTS member_point_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  change_points INT NOT NULL DEFAULT 0,
  balance INT NOT NULL DEFAULT 0,
  biz_type VARCHAR(32) NOT NULL,
  ref_id BIGINT,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_member (tenant_id, member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

CREATE TABLE IF NOT EXISTS member_wallet_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  change_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  balance DECIMAL(12,2) NOT NULL DEFAULT 0,
  biz_type VARCHAR(32) NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND/GIFT',
  ref_id BIGINT,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_member (tenant_id, member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='储值流水';

CREATE TABLE IF NOT EXISTS member_address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  name VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(255),
  lng DECIMAL(10,6),
  lat DECIMAL(10,6),
  is_default TINYINT NOT NULL DEFAULT 0,
  KEY idx_tenant_member (tenant_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员地址';

CREATE TABLE IF NOT EXISTS member_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_member_sku (tenant_id, member_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员收藏';

CREATE TABLE IF NOT EXISTS coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(16) NOT NULL COMMENT 'FULL_REDUCTION/DISCOUNT/PRODUCT/FREIGHT',
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_rate DECIMAL(5,2) NOT NULL DEFAULT 10,
  sku_id BIGINT,
  total_qty INT NOT NULL DEFAULT 0,
  issued_qty INT NOT NULL DEFAULT 0,
  per_user_limit INT NOT NULL DEFAULT 1,
  valid_from DATETIME NOT NULL,
  valid_to DATETIME NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

CREATE TABLE IF NOT EXISTS member_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at DATETIME NULL,
  order_id BIGINT,
  KEY idx_tenant_member (tenant_id, member_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员优惠券';

CREATE TABLE IF NOT EXISTS member_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  color VARCHAR(16),
  UNIQUE KEY uk_tenant_tag (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员标签';

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT,
  type VARCHAR(16) NOT NULL DEFAULT 'SUGGESTION',
  content VARCHAR(1024),
  images JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  reply VARCHAR(1024),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈';

-- ============================================================
-- 结算 / 配送 / CMS
-- ============================================================
CREATE TABLE IF NOT EXISTS settlement_statement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  statement_no VARCHAR(32) NOT NULL,
  type VARCHAR(16) NOT NULL COMMENT 'SUPPLIER/FRANCHISE',
  partner_id BIGINT NOT NULL,
  partner_name VARCHAR(128),
  period_start DATE,
  period_end DATE,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  deduction_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'DRAFT/PENDING/CONFIRMED/PAID/DISPUTED',
  confirmed_at DATETIME NULL,
  paid_at DATETIME NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_st_no (tenant_id, statement_no),
  KEY idx_tenant_partner (tenant_id, type, partner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账单';

CREATE TABLE IF NOT EXISTS settlement_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  statement_id BIGINT NOT NULL,
  ref_type VARCHAR(16) NOT NULL,
  ref_id BIGINT,
  ref_no VARCHAR(32),
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  deduction DECIMAL(12,2) NOT NULL DEFAULT 0,
  payable DECIMAL(12,2) NOT NULL DEFAULT 0,
  KEY idx_tenant_statement (tenant_id, statement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账明细';

CREATE TABLE IF NOT EXISTS settlement_invoice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  statement_id BIGINT NOT NULL,
  invoice_no VARCHAR(64),
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  file_url VARCHAR(512),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_statement (tenant_id, statement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开票记录';

CREATE TABLE IF NOT EXISTS franchise_commission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  franchisee_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  period VARCHAR(16) NOT NULL,
  sales_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  commission_type VARCHAR(16) NOT NULL DEFAULT 'SALES',
  commission_value DECIMAL(10,2) NOT NULL DEFAULT 0,
  commission_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_fr (tenant_id, franchisee_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟分账';

CREATE TABLE IF NOT EXISTS delivery_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 20,
  delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 3,
  free_threshold DECIMAL(10,2) NOT NULL DEFAULT 49,
  radius_m INT NOT NULL DEFAULT 3000,
  time_slots JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_tenant_store (tenant_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送规则';

CREATE TABLE IF NOT EXISTS cms_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL DEFAULT 'NEWS',
  title VARCHAR(128) NOT NULL,
  title_en VARCHAR(128),
  summary VARCHAR(255),
  summary_en VARCHAR(255),
  cover VARCHAR(512),
  content MEDIUMTEXT,
  content_en MEDIUMTEXT,
  author VARCHAR(64),
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
  published_at DATETIME NULL,
  seo_keywords VARCHAR(255),
  seo_description VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_status (tenant_id, status, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CMS 文章';

CREATE TABLE IF NOT EXISTS cms_banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  position VARCHAR(16) NOT NULL DEFAULT 'PORTAL' COMMENT 'PORTAL/MINI/H5',
  title VARCHAR(128),
  image VARCHAR(512),
  link VARCHAR(512),
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Banner';

CREATE TABLE IF NOT EXISTS cms_franchise_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  company VARCHAR(128),
  contact VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  city VARCHAR(64),
  amount VARCHAR(32),
  remark VARCHAR(512),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONTACTED/APPROVED/REJECTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟申请';

CREATE TABLE IF NOT EXISTS tenant_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content TEXT,
  type VARCHAR(16) NOT NULL DEFAULT 'NOTICE',
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_time (tenant_id, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户公告';

SET FOREIGN_KEY_CHECKS = 1;
