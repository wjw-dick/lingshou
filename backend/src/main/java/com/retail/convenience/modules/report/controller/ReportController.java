package com.retail.convenience.modules.report.controller;

import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.ExcelUtil;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/reports")
public class ReportController {

    private final JdbcTemplate jdbc;

    public ReportController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/dashboard")
    @RequirePerm("report:view")
    public ApiResult<Map<String, Object>> dashboard(@RequestParam(required = false) Long storeId) {
        Long tenantId = AuthContext.require().getTenantId();
        String storeCondition = storeId == null ? "" : " AND store_id = " + storeId;
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal todaySales = jdbc.queryForObject(
                "SELECT COALESCE(SUM(paid_amount),0) FROM sales_order WHERE tenant_id = ? AND pay_status='PAID'"
                        + storeCondition + " AND created_at >= CURDATE()", BigDecimal.class, tenantId);
        Integer todayOrders = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_order WHERE tenant_id = ? AND pay_status='PAID'"
                        + storeCondition + " AND created_at >= CURDATE()", Integer.class, tenantId);
        BigDecimal yesterdaySales = jdbc.queryForObject(
                "SELECT COALESCE(SUM(paid_amount),0) FROM sales_order WHERE tenant_id = ? AND pay_status='PAID'"
                        + storeCondition + " AND created_at >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND created_at < CURDATE()",
                BigDecimal.class, tenantId);
        Integer newMembers = jdbc.queryForObject(
                "SELECT COUNT(*) FROM member WHERE tenant_id = ? AND registered_at >= CURDATE()", Integer.class, tenantId);
        result.put("todaySales", todaySales);
        result.put("todayOrders", todayOrders);
        result.put("avgTicket", todayOrders == null || todayOrders == 0 ? BigDecimal.ZERO
                : todaySales.divide(BigDecimal.valueOf(todayOrders), 2, java.math.RoundingMode.HALF_UP));
        result.put("yesterdaySales", yesterdaySales);
        result.put("todayNewMembers", newMembers);
        result.put("trend", jdbc.queryForList(
                "SELECT DATE(created_at) AS day, COALESCE(SUM(paid_amount),0) AS sales, COUNT(*) AS orders "
                        + "FROM sales_order WHERE tenant_id = ? AND pay_status='PAID'" + storeCondition
                        + " AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
                        + "GROUP BY DATE(created_at) ORDER BY day", tenantId));
        result.put("storeRanking", jdbc.queryForList(
                "SELECT s.id AS store_id, s.name AS store_name, COALESCE(SUM(o.paid_amount),0) AS sales, COUNT(o.id) AS orders "
                        + "FROM store s LEFT JOIN sales_order o ON o.store_id = s.id AND o.tenant_id = s.tenant_id "
                        + "AND o.pay_status='PAID' AND o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) "
                        + "WHERE s.tenant_id = ? AND s.deleted = 0 " + (storeId == null ? "" : "AND s.id = " + storeId)
                        + " GROUP BY s.id, s.name ORDER BY sales DESC LIMIT 10", tenantId));
        result.put("topProducts", jdbc.queryForList(
                "SELECT i.sku_name, SUM(i.qty) AS qty, SUM(i.amount) AS amount FROM sales_order_item i "
                        + "JOIN sales_order o ON o.id = i.order_id AND o.tenant_id = i.tenant_id "
                        + "WHERE o.tenant_id = ? AND o.pay_status='PAID' AND o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)"
                        + (storeId == null ? "" : " AND o.store_id = " + storeId)
                        + " GROUP BY i.sku_name ORDER BY qty DESC LIMIT 8", tenantId));
        result.put("recentOrders", jdbc.queryForList(
                "SELECT order_no, channel, status, pay_status, paid_amount, created_at FROM sales_order "
                        + "WHERE tenant_id = ?" + storeCondition + " ORDER BY created_at DESC LIMIT 8", tenantId));
        result.put("memberTotal", jdbc.queryForObject(
                "SELECT COUNT(*) FROM member WHERE tenant_id = ? AND status='ACTIVE'", Integer.class, tenantId));
        result.put("lowStockCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock WHERE tenant_id = ? AND owner_type='STORE'"
                        + (storeId == null ? "" : " AND owner_id = " + storeId)
                        + " AND qty - locked_qty <= safety_stock", Integer.class, tenantId));
        result.put("nearExpiryCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_batch WHERE tenant_id = ? AND owner_type='STORE'"
                        + (storeId == null ? "" : " AND owner_id = " + storeId)
                        + " AND qty > 0 AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL 3 DAY)", Integer.class, tenantId));
        return ApiResult.ok(result);
    }

    @GetMapping("/sales")
    @RequirePerm("report:view")
    public ApiResult<List<Map<String, Object>>> sales(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "day") String dimension) {
        Long tenantId = AuthContext.require().getTenantId();
        String s = start == null ? LocalDate.now().minusDays(29).toString() : start;
        String e = end == null ? LocalDate.now().toString() : end;
        String store = storeId == null ? "" : " AND o.store_id = " + storeId;
        String sql;
        switch (dimension) {
            case "store" -> sql = "SELECT s.name AS dimension, COALESCE(SUM(o.paid_amount),0) AS sales, COUNT(o.id) AS orders, "
                    + "COALESCE(AVG(o.paid_amount),0) AS avg_amount FROM sales_order o JOIN store s ON s.id = o.store_id "
                    + "AND s.tenant_id = o.tenant_id WHERE o.tenant_id = ? AND o.pay_status='PAID' "
                    + "AND DATE(o.created_at) BETWEEN ? AND ?" + store + " GROUP BY s.name ORDER BY sales DESC";
            case "payment" -> sql = "SELECT COALESCE(payment_method,'UNKNOWN') AS dimension, COALESCE(SUM(paid_amount),0) AS sales, "
                    + "COUNT(*) AS orders, COALESCE(AVG(paid_amount),0) AS avg_amount FROM sales_order o "
                    + "WHERE o.tenant_id = ? AND o.pay_status='PAID' AND DATE(o.created_at) BETWEEN ? AND ?"
                    + store + " GROUP BY payment_method ORDER BY sales DESC";
            case "category" -> sql = "SELECT c.name AS dimension, COALESCE(SUM(i.amount),0) AS sales, "
                    + "COALESCE(SUM(i.qty),0) AS qty, 0 AS orders FROM sales_order_item i "
                    + "JOIN sales_order o ON o.id = i.order_id AND o.tenant_id = i.tenant_id "
                    + "JOIN product_sku sku ON sku.id = i.sku_id AND sku.tenant_id = i.tenant_id "
                    + "JOIN product_spu spu ON spu.id = sku.spu_id AND spu.tenant_id = sku.tenant_id "
                    + "JOIN product_category c ON c.id = spu.category_id AND c.tenant_id = spu.tenant_id "
                    + "WHERE o.tenant_id = ? AND o.pay_status='PAID' AND DATE(o.created_at) BETWEEN ? AND ?"
                    + store + " GROUP BY c.name ORDER BY sales DESC";
            default -> sql = "SELECT DATE(o.created_at) AS dimension, COALESCE(SUM(o.paid_amount),0) AS sales, "
                    + "COUNT(*) AS orders, COALESCE(AVG(o.paid_amount),0) AS avg_amount FROM sales_order o "
                    + "WHERE o.tenant_id = ? AND o.pay_status='PAID' AND DATE(o.created_at) BETWEEN ? AND ?"
                    + store + " GROUP BY DATE(o.created_at) ORDER BY dimension";
        }
        return ApiResult.ok(jdbc.queryForList(sql, tenantId, s, e));
    }

    @GetMapping("/gross-profit")
    @RequirePerm("report:view")
    public ApiResult<Map<String, Object>> grossProfit(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        Long tenantId = AuthContext.require().getTenantId();
        String s = start == null ? LocalDate.now().minusDays(29).toString() : start;
        String e = end == null ? LocalDate.now().toString() : end;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT c.name AS category, COALESCE(SUM(i.amount),0) AS sales, "
                        + "COALESCE(SUM(sku.cost_price * i.qty),0) AS cost, "
                        + "COALESCE(SUM(i.amount - sku.cost_price * i.qty),0) AS profit "
                        + "FROM sales_order_item i JOIN sales_order o ON o.id = i.order_id AND o.tenant_id = i.tenant_id "
                        + "JOIN product_sku sku ON sku.id = i.sku_id AND sku.tenant_id = i.tenant_id "
                        + "JOIN product_spu spu ON spu.id = sku.spu_id AND spu.tenant_id = sku.tenant_id "
                        + "JOIN product_category c ON c.id = spu.category_id AND c.tenant_id = spu.tenant_id "
                        + "WHERE o.tenant_id = ? AND o.pay_status='PAID' AND DATE(o.created_at) BETWEEN ? AND ? "
                        + "GROUP BY c.name ORDER BY profit DESC", tenantId, s, e);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("totalSales", rows.stream().map(r -> (BigDecimal) r.get("sales"))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("totalCost", rows.stream().map(r -> (BigDecimal) r.get("cost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("totalProfit", rows.stream().map(r -> (BigDecimal) r.get("profit"))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return ApiResult.ok(result);
    }

    @GetMapping("/inventory")
    @RequirePerm("report:view")
    public ApiResult<Map<String, Object>> inventory(@RequestParam(required = false) Long storeId) {
        Long tenantId = AuthContext.require().getTenantId();
        String store = storeId == null ? "" : " AND st.owner_id = " + storeId;
        List<Map<String, Object>> turnover = jdbc.queryForList(
                "SELECT spu.name AS sku_name, st.qty AS stock_qty, COALESCE(sales.qty,0) AS sales_30d, "
                        + "CASE WHEN COALESCE(sales.qty,0)=0 THEN 999 ELSE ROUND(st.qty / (sales.qty / 30.0), 1) END AS turnover_days "
                        + "FROM stock st JOIN product_sku sku ON sku.id = st.sku_id AND sku.tenant_id = st.tenant_id "
                        + "JOIN product_spu spu ON spu.id = sku.spu_id AND spu.tenant_id = sku.tenant_id "
                        + "LEFT JOIN (SELECT i.sku_id, SUM(i.qty) AS qty FROM sales_order_item i "
                        + "JOIN sales_order o ON o.id = i.order_id AND o.tenant_id = i.tenant_id "
                        + "WHERE o.tenant_id = ? AND o.pay_status='PAID' AND o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) "
                        + "GROUP BY i.sku_id) sales ON sales.sku_id = st.sku_id "
                        + "WHERE st.tenant_id = ? AND st.owner_type='STORE'" + store
                        + " ORDER BY turnover_days DESC LIMIT 100", tenantId, tenantId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("turnover", turnover);
        result.put("slowMovingCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock st WHERE st.tenant_id = ? AND st.owner_type='STORE' AND st.qty > 0"
                        + store + " AND NOT EXISTS (SELECT 1 FROM sales_order_item i JOIN sales_order o "
                        + "ON o.id = i.order_id AND o.tenant_id = i.tenant_id WHERE i.tenant_id = st.tenant_id "
                        + "AND i.sku_id = st.sku_id AND o.pay_status='PAID' AND o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY))",
                Integer.class, tenantId));
        result.put("stockoutCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock st WHERE st.tenant_id = ? AND st.owner_type='STORE' AND st.qty = 0"
                        + store, Integer.class, tenantId));
        result.put("nearExpiryCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_batch b WHERE b.tenant_id = ? AND b.owner_type='STORE' AND b.qty > 0 "
                        + "AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 3 DAY)"
                        + (storeId == null ? "" : " AND b.owner_id = " + storeId), Integer.class, tenantId));
        return ApiResult.ok(result);
    }

    @GetMapping("/members")
    @RequirePerm("report:view")
    public ApiResult<Map<String, Object>> members() {
        Long tenantId = AuthContext.require().getTenantId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", jdbc.queryForObject("SELECT COUNT(*) FROM member WHERE tenant_id = ?", Integer.class, tenantId));
        result.put("newThisMonth", jdbc.queryForObject(
                "SELECT COUNT(*) FROM member WHERE tenant_id = ? AND registered_at >= DATE_FORMAT(CURDATE(),'%Y-%m-01')",
                Integer.class, tenantId));
        result.put("active30d", jdbc.queryForObject(
                "SELECT COUNT(DISTINCT member_id) FROM sales_order WHERE tenant_id = ? AND member_id IS NOT NULL "
                        + "AND pay_status='PAID' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)", Integer.class, tenantId));
        result.put("repurchase", jdbc.queryForObject(
                "SELECT COUNT(*) FROM (SELECT member_id FROM sales_order WHERE tenant_id = ? AND member_id IS NOT NULL "
                        + "AND pay_status='PAID' GROUP BY member_id HAVING COUNT(*) > 1) t", Integer.class, tenantId));
        result.put("walletBalance", jdbc.queryForObject(
                "SELECT COALESCE(SUM(wallet_balance),0) FROM member WHERE tenant_id = ?", BigDecimal.class, tenantId));
        result.put("couponIssued", jdbc.queryForObject(
                "SELECT COALESCE(SUM(issued_qty),0) FROM coupon WHERE tenant_id = ?", Integer.class, tenantId));
        result.put("couponUsed", jdbc.queryForObject(
                "SELECT COUNT(*) FROM member_coupon WHERE tenant_id = ? AND status='USED'", Integer.class, tenantId));
        result.put("levelDistribution", jdbc.queryForList(
                "SELECT l.name AS level_name, COUNT(m.id) AS member_count FROM member_level l "
                        + "LEFT JOIN member m ON m.level_id = l.id AND m.tenant_id = l.tenant_id "
                        + "WHERE l.tenant_id = ? GROUP BY l.name", tenantId));
        return ApiResult.ok(result);
    }

    @GetMapping("/supplier-fulfillment")
    @RequirePerm("report:view")
    public ApiResult<List<Map<String, Object>>> supplierFulfillment() {
        Long tenantId = AuthContext.require().getTenantId();
        return ApiResult.ok(jdbc.queryForList(
                "SELECT sp.name AS supplier_name, COUNT(so.id) AS order_count, "
                        + "COALESCE(SUM(soi.qty),0) AS order_qty, COALESCE(SUM(soi.received_qty),0) AS received_qty, "
                        + "CASE WHEN COALESCE(SUM(soi.qty),0)=0 THEN 0 ELSE ROUND(SUM(soi.received_qty)/SUM(soi.qty)*100,1) END AS fulfillment_rate "
                        + "FROM supplier sp LEFT JOIN supply_order so ON so.supplier_id = sp.id AND so.tenant_id = sp.tenant_id "
                        + "LEFT JOIN supply_order_item soi ON soi.supply_id = so.id AND soi.tenant_id = so.tenant_id "
                        + "WHERE sp.tenant_id = ? AND sp.deleted = 0 GROUP BY sp.name ORDER BY fulfillment_rate DESC", tenantId));
    }

    @GetMapping("/franchise-ranking")
    @RequirePerm("report:view")
    public ApiResult<List<Map<String, Object>>> franchiseRanking() {
        Long tenantId = AuthContext.require().getTenantId();
        return ApiResult.ok(jdbc.queryForList(
                "SELECT f.name AS franchisee_name, COALESCE(SUM(fc.sales_amount),0) AS sales_amount, "
                        + "COALESCE(SUM(fc.commission_amount),0) AS commission_amount "
                        + "FROM franchisee f LEFT JOIN franchise_commission fc ON fc.franchisee_id = f.id AND fc.tenant_id = f.tenant_id "
                        + "WHERE f.tenant_id = ? AND f.deleted = 0 GROUP BY f.name ORDER BY sales_amount DESC", tenantId));
    }

    @GetMapping("/export")
    @RequirePerm("report:view")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "sales") String type,
                                         @RequestParam(required = false) Long storeId,
                                         @RequestParam(required = false) String start,
                                         @RequestParam(required = false) String end) {
        List<Map<String, Object>> rows;
        List<String> headers;
        String sheet;
        if ("inventory".equals(type)) {
            Map<String, Object> data = inventory(storeId).getData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> turnover = (List<Map<String, Object>>) data.get("turnover");
            rows = turnover;
            headers = List.of("商品", "库存", "30天销量", "周转天数");
            sheet = "库存报表";
        } else if ("members".equals(type)) {
            rows = new ArrayList<>();
            headers = List.of("指标", "数值");
            sheet = "会员报表";
            Map<String, Object> data = members().getData();
            data.forEach((k, v) -> {
                if (!(v instanceof List)) {
                    rows.add(Map.of("k", k, "v", v));
                }
            });
        } else {
            rows = sales(storeId, start, end, "day").getData();
            headers = List.of("日期", "销售额", "订单数", "客单价");
            sheet = "销售报表";
        }
        byte[] bytes = ExcelUtil.write(sheet, headers, rows.stream().map(row -> {
            List<Object> values = new ArrayList<>();
            for (String h : headers) {
                Object v = switch (h) {
                    case "商品" -> row.get("sku_name");
                    case "库存" -> row.get("stock_qty");
                    case "30天销量" -> row.get("sales_30d");
                    case "周转天数" -> row.get("turnover_days");
                    case "指标" -> row.get("k");
                    case "数值" -> row.get("v");
                    case "日期" -> row.get("dimension");
                    case "销售额" -> row.get("sales");
                    case "订单数" -> row.get("orders");
                    case "客单价" -> row.get("avg_amount");
                    default -> "";
                };
                values.add(v);
            }
            return values;
        }).toList());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        responseHeaders.setContentDisposition(ContentDisposition.attachment()
                .filename(sheet + ".xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(responseHeaders).body(bytes);
    }
}
