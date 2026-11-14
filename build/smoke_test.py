# -*- coding: utf-8 -*-
"""Backend API smoke test for retail-convenience."""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080"


def call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            parsed = json.loads(raw)
        except Exception:
            parsed = raw
        return e.code, parsed


def login(path, body):
    status, data = call("POST", path, body)
    if status != 200 or data.get("code") != 0:
        print("LOGIN FAIL", path, status, data)
        return None
    return data["data"]["token"]


tenant = login("/api/v1/auth/login", {"tenantCode": "yxbl", "username": "admin", "password": "Admin@123"})
platform = login("/api/v1/platform/auth/login", {"username": "admin", "password": "Admin@123"})
staff = login("/api/v1/auth/login", {"tenantCode": "yxbl", "username": "dianzhang", "password": "Admin@123"})
supplier = login("/api/v1/auth/login", {"tenantCode": "yxbl", "username": "gys01", "password": "Admin@123"})
franchisee = login("/api/v1/auth/login", {"tenantCode": "yxbl", "username": "jm01", "password": "Admin@123"})
app = login("/api/v1/app/auth/login", {"tenantCode": "yxbl", "openid": "oUx1demo001"})

checks = [
    ("platform dashboard", "GET", "/api/v1/platform/dashboard", None, platform),
    ("platform tenants", "GET", "/api/v1/platform/tenants?page=1&size=5", None, platform),
    ("platform plans", "GET", "/api/v1/platform/plans", None, platform),
    ("platform bills", "GET", "/api/v1/platform/bills?page=1&size=5", None, platform),
    ("tenant me", "GET", "/api/v1/auth/me", None, tenant),
    ("stores", "GET", "/api/v1/tenant/stores?page=1&size=5", None, tenant),
    ("store options", "GET", "/api/v1/tenant/stores/options", None, tenant),
    ("categories", "GET", "/api/v1/tenant/products/categories", None, tenant),
    ("spus", "GET", "/api/v1/tenant/products/spus?page=1&size=5", None, tenant),
    ("purchase orders", "GET", "/api/v1/tenant/purchase-orders?page=1&size=5", None, tenant),
    ("supply orders", "GET", "/api/v1/tenant/supply-orders?page=1&size=5", None, tenant),
    ("receive orders", "GET", "/api/v1/tenant/receive-orders?page=1&size=5", None, tenant),
    ("stocks", "GET", "/api/v1/tenant/inventory/stocks?storeId=1", None, tenant),
    ("alerts", "GET", "/api/v1/tenant/inventory/alerts?storeId=1", None, tenant),
    ("transfers", "GET", "/api/v1/tenant/transfers", None, tenant),
    ("checks", "GET", "/api/v1/tenant/stock-checks", None, tenant),
    ("losses", "GET", "/api/v1/tenant/loss-orders", None, tenant),
    ("orders", "GET", "/api/v1/tenant/orders?page=1&size=5", None, tenant),
    ("shift", "GET", "/api/v1/tenant/shifts", None, tenant),
    ("settlements", "GET", "/api/v1/tenant/day-settlements", None, tenant),
    ("members", "GET", "/api/v1/tenant/members?page=1&size=5", None, tenant),
    ("coupons", "GET", "/api/v1/tenant/coupons", None, tenant),
    ("promotions", "GET", "/api/v1/tenant/promotions", None, tenant),
    ("suppliers", "GET", "/api/v1/tenant/suppliers", None, tenant),
    ("franchisees", "GET", "/api/v1/tenant/franchisees", None, tenant),
    ("settlement statements", "GET", "/api/v1/tenant/settlements", None, tenant),
    ("reports dashboard", "GET", "/api/v1/tenant/reports/dashboard?storeId=1", None, tenant),
    ("reports sales", "GET", "/api/v1/tenant/reports/sales", None, tenant),
    ("reports inventory", "GET", "/api/v1/tenant/reports/inventory", None, tenant),
    ("reports members", "GET", "/api/v1/tenant/reports/members", None, tenant),
    ("system configs", "GET", "/api/v1/tenant/system/configs", None, tenant),
    ("system users", "GET", "/api/v1/tenant/system/users", None, tenant),
    ("system roles", "GET", "/api/v1/tenant/system/roles", None, tenant),
    ("system menus", "GET", "/api/v1/tenant/system/menus?endType=ADMIN", None, tenant),
    ("messages", "GET", "/api/v1/tenant/system/messages", None, tenant),
    ("storage", "GET", "/api/v1/tenant/system/storage", None, tenant),
    ("sso", "GET", "/api/v1/tenant/system/sso", None, tenant),
    ("audit", "GET", "/api/v1/tenant/system/audit-logs", None, tenant),
    ("mp accounts", "GET", "/api/v1/tenant/mp/accounts", None, tenant),
    ("mp stats", "GET", "/api/v1/tenant/mp/statistics?days=7", None, tenant),
    ("mp tags", "GET", "/api/v1/tenant/mp/tags", None, tenant),
    ("mp fans", "GET", "/api/v1/tenant/mp/fans", None, tenant),
    ("mp messages", "GET", "/api/v1/tenant/mp/messages", None, tenant),
    ("mp templates", "GET", "/api/v1/tenant/mp/templates", None, tenant),
    ("mp menu", "GET", "/api/v1/tenant/mp/menu/current", None, tenant),
    ("mp auto replies", "GET", "/api/v1/tenant/mp/auto-replies", None, tenant),
    ("mp materials", "GET", "/api/v1/tenant/mp/materials", None, tenant),
    ("mp drafts", "GET", "/api/v1/tenant/mp/drafts", None, tenant),
    ("mp publishes", "GET", "/api/v1/tenant/mp/publishes", None, tenant),
    ("cms articles", "GET", "/api/v1/tenant/cms/articles", None, tenant),
    ("cms banners", "GET", "/api/v1/tenant/cms/banners", None, tenant),
    ("cms franchise", "GET", "/api/v1/tenant/cms/franchise-applications", None, tenant),
    ("public site", "GET", "/api/v1/public/site/yxbl", None, None),
    ("public mall home", "GET", "/api/v1/public/site/yxbl/home", None, None),
    ("public products", "GET", "/api/v1/public/site/yxbl/products", None, None),
    ("public stores", "GET", "/api/v1/public/site/yxbl/stores", None, None),
    ("public articles", "GET", "/api/v1/public/site/yxbl/articles", None, None),
    ("app profile", "GET", "/api/v1/app/profile", None, app),
    ("app home", "GET", "/api/v1/app/home", None, app),
    ("app orders", "GET", "/api/v1/app/orders", None, app),
    ("app coupons", "GET", "/api/v1/app/coupons", None, app),
    ("staff workbench", "GET", "/api/v1/staff/workbench", None, staff),
    ("supplier profile", "GET", "/api/v1/supplier/profile", None, supplier),
    ("supplier orders", "GET", "/api/v1/supplier/supply-orders", None, supplier),
    ("franchisee profile", "GET", "/api/v1/franchisee/profile", None, franchisee),
    ("franchisee stores", "GET", "/api/v1/franchisee/stores", None, franchisee),
]

failed = 0
for name, method, path, body, token in checks:
    status, data = call(method, path, body, token)
    ok = status == 200 and isinstance(data, dict) and data.get("code") == 0
    if not ok:
        failed += 1
        print("FAIL", name, status, str(data)[:400])
    else:
        print("OK  ", name)
print(f"\n{len(checks) - failed}/{len(checks)} passed")
