# -*- coding: utf-8 -*-
"""零售便利连锁 SaaS 平台 API 回归测试（隔离 / 收银 / 线上订单 / 生命周期）。

运行前：后端启动在 http://127.0.0.1:8080，MySQL/Redis 正常。
运行：python tests/api_regression.py
"""
import json
import sys
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080"
PASSED = []
FAILED = []


def parse(raw):
    if not raw:
        return {}
    try:
        return json.loads(raw)
    except Exception:
        return raw


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, parse(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        return e.code, parse(raw)


def login(path, body):
    status, data = call("POST", path, body)
    assert status == 200 and data.get("code") == 0, f"登录失败 {path}: {status} {data}"
    return data["data"]["token"]


def check(name, condition, detail=""):
    if condition:
        PASSED.append(name)
        print(f"PASS  {name}")
    else:
        FAILED.append(name)
        print(f"FAIL  {name} {detail}")


def ok(method, path, body=None, token=None):
    status, data = call(method, path, body, token)
    return status, data, status == 200 and isinstance(data, dict) and data.get("code") == 0


def main():
    admin = login("/api/v1/auth/login", {"tenantCode": "yxbl", "username": "admin", "password": "Admin@123"})
    platform = login("/api/v1/platform/auth/login", {"username": "admin", "password": "Admin@123"})
    tenant2 = login("/api/v1/auth/login", {"tenantCode": "xgsg", "username": "admin", "password": "Admin@123"})
    cashier = login("/api/v1/auth/login", {"tenantCode": "yxbl", "username": "shouyin", "password": "Admin@123"})
    app = login("/api/v1/app/auth/login", {"tenantCode": "yxbl", "openid": "oUx1demo001"})

    # ---------- 多租户隔离 ----------
    status, data, success = ok("GET", "/api/v1/tenant/stores?page=1&size=50", None, tenant2)
    stores = data.get("data", {}).get("records", []) if data.get("data") else []
    check("租户2门店列表仅含本租户数据", success and stores and all(s["tenantId"] == 2 for s in stores),
          f"stores={[(s.get('id'), s.get('tenantId')) for s in stores]}")

    status, data = call("GET", "/api/v1/tenant/stores/1", None, tenant2)
    check("租户2直连租户1门店ID被拒绝", status in (403, 404), f"status={status} data={data}")

    status, data = call("GET", "/api/v1/tenant/supply-orders/1", None, tenant2)
    check("租户2直连租户1供货单被拒绝", status in (403, 404), f"status={status} data={data}")

    status, data = call("GET", "/api/v1/tenant/stores", None, platform)
    check("平台账号禁止访问租户后台接口", status == 403, f"status={status}")

    status, data = call("GET", "/api/v1/platform/tenants", None, admin)
    check("租户账号禁止访问平台端接口", status == 403, f"status={status}")

    # ---------- 租户生命周期 ----------
    status, data, _ = ok("POST", "/api/v1/platform/tenants/2/status?status=SUSPENDED", None, platform)
    check("平台停用租户2", status == 200)
    status, data = call("POST", "/api/v1/auth/login",
                        {"tenantCode": "xgsg", "username": "admin", "password": "Admin@123"})
    check("停用租户登录被拒", status == 403, f"status={status} data={data}")
    status, data = call("GET", "/api/v1/auth/me", None, tenant2)
    check("停用租户旧 Token 失效", status == 403, f"status={status}")
    ok("POST", "/api/v1/platform/tenants/2/status?status=TRIAL", None, platform)
    status, data, success = ok("POST", "/api/v1/auth/login",
                               {"tenantCode": "xgsg", "username": "admin", "password": "Admin@123"})
    check("租户恢复后可重新登录", status == 200 and success)

    # ---------- 库存与 POS 收银 ----------
    _, stock_data, _ = ok("GET", "/api/v1/tenant/inventory/stocks?storeId=1", None, admin)
    before_qty = next((row["stock"]["qty"] for row in stock_data["data"] if row["stock"]["skuId"] == 1), None)
    check("读取收银前库存", before_qty is not None)

    _, shift_data, _ = ok("GET", "/api/v1/tenant/shifts/current?storeId=1", None, admin)
    shift = shift_data.get("data")
    if not shift:
        status, _, _ = ok("POST", "/api/v1/tenant/shifts/open?storeId=1&openingCash=500", None, admin)
        check("开班", status == 200)

    _, preview_data, success = ok("POST", "/api/v1/tenant/pos/preview",
                                  {"storeId": 1, "items": [{"skuId": 1, "qty": 1}]}, cashier)
    check("收银预览返回应收金额", success and float(preview_data["data"]["payableAmount"]) > 0,
          str(preview_data)[:300])

    status, checkout_data, success = ok("POST", "/api/v1/tenant/pos/checkout",
                                        {"storeId": 1, "paymentMethod": "CASH", "items": [{"skuId": 1, "qty": 1}]},
                                        cashier)
    order_no = checkout_data.get("data", {}).get("orderNo") if success else None
    check("收银结账成功并生成订单", success and checkout_data["data"]["status"] == "COMPLETED",
          f"status={status} data={str(checkout_data)[:300]}")

    _, stock_after, _ = ok("GET", "/api/v1/tenant/inventory/stocks?storeId=1", None, admin)
    after_qty = next((row["stock"]["qty"] for row in stock_after["data"] if row["stock"]["skuId"] == 1), None)
    check("收银后门店库存扣减", before_qty is not None and after_qty == before_qty - 1,
          f"{before_qty} -> {after_qty}")

    # ---------- 线上订单全链路 ----------
    status, order_data, success = ok("POST", "/api/v1/app/orders", {
        "storeId": 1, "deliveryType": "PICKUP", "pointsUsed": 0,
        "items": [{"skuId": 4, "qty": 2}]
    }, app)
    online = order_data.get("data") if success else {}
    check("用户端创建线上订单（库存预占）", success and online.get("status") == "PENDING_PAY",
          f"status={status} data={str(order_data)[:300]}")
    online_id = online.get("id") if success else None

    if online_id:
        status, pay_data, success = ok("POST", f"/api/v1/app/orders/{online_id}/pay?method=WALLET", None, app)
        check("会员储值支付成功", success and pay_data["data"]["payStatus"] == "PAID",
              f"status={status} data={str(pay_data)[:300]}")
        status, _, success = ok("POST", f"/api/v1/tenant/orders/{online_id}/accept", None, admin)
        check("门店接单", success, f"status={status}")
        status, _, success = ok("POST", f"/api/v1/tenant/orders/{online_id}/pick-complete", None, admin)
        check("拣货完成", success, f"status={status}")
        status, _, success = ok("POST", f"/api/v1/tenant/orders/{online_id}/complete", None, admin)
        check("订单完成", success, f"status={status}")
        _, detail, _ = ok("GET", f"/api/v1/app/orders/{online_id}", None, app)
        check("用户端订单状态为已完成", detail["data"]["order"]["status"] == "COMPLETED",
              str(detail)[:300])
        status, refund_data, success = ok("POST", f"/api/v1/tenant/orders/{online_id}/refund?reason=regression-refund",
                                          None, admin)
        check("退款并回库", success and refund_data["data"]["status"] == "REFUNDED",
              f"status={status} data={str(refund_data)[:300]}")

    status, data, _ = ok("POST", "/api/v1/app/orders", {
        "storeId": 1, "deliveryType": "PICKUP", "items": [{"skuId": 9, "qty": 1}]
    }, app)
    check("烟草商品线上禁售拦截", status == 400, f"status={status} data={data}")

    # ---------- 审计 ----------
    _, audit_data, success = ok("GET", "/api/v1/tenant/system/audit-logs?page=1&size=5", None, admin)
    check("租户审计日志有记录", success and audit_data["data"]["total"] > 0, str(audit_data)[:200])

    # ---------- 公众号回调验签 ----------
    import hashlib
    token, ts, nonce, echo = "yxbl_token_2026", "1770000000", "abc123", "hello-wechat"
    sig = hashlib.sha1("".join(sorted([token, ts, nonce])).encode()).hexdigest()
    status, data = call("GET", f"/admin-api/mp/open/wx_demo_yxbl_001?signature={sig}"
                               f"&timestamp={ts}&nonce={nonce}&echostr={echo}")
    check("公众号回调有效签名回显 echostr", status == 200 and data == echo,
          f"status={status} data={data}")
    status, data = call("GET", f"/admin-api/mp/open/wx_demo_yxbl_001?signature=bad"
                               f"&timestamp={ts}&nonce={nonce}&echostr={echo}")
    check("公众号回调错误签名被拒", status == 403, f"status={status}")

    print(f"\n{len(PASSED)} passed, {len(FAILED)} failed")
    if FAILED:
        print("失败项：" + "；".join(FAILED))
        sys.exit(1)


if __name__ == "__main__":
    main()
