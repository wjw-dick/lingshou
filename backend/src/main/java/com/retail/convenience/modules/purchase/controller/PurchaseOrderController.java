package com.retail.convenience.modules.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.inventory.entity.Stock;
import com.retail.convenience.modules.inventory.mapper.StockMapper;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.purchase.dto.PurchaseSaveRequest;
import com.retail.convenience.modules.purchase.dto.SupplySaveRequest;
import com.retail.convenience.modules.purchase.entity.PurchaseOrder;
import com.retail.convenience.modules.purchase.entity.PurchaseOrderItem;
import com.retail.convenience.modules.purchase.entity.SupplyOrder;
import com.retail.convenience.modules.purchase.mapper.PurchaseOrderItemMapper;
import com.retail.convenience.modules.purchase.mapper.PurchaseOrderMapper;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderMapper;
import com.retail.convenience.modules.system.service.SystemConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderMapper purchaseMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final SupplyOrderMapper supplyMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final StockMapper stockMapper;
    private final JdbcTemplate jdbcTemplate;
    private final NoGenerator noGenerator;
    private final SystemConfigService configService;

    public PurchaseOrderController(PurchaseOrderMapper purchaseMapper, PurchaseOrderItemMapper itemMapper,
                                   SupplyOrderMapper supplyMapper, ProductSkuMapper skuMapper,
                                   ProductSpuMapper spuMapper, StockMapper stockMapper,
                                   JdbcTemplate jdbcTemplate, NoGenerator noGenerator,
                                   SystemConfigService configService) {
        this.purchaseMapper = purchaseMapper;
        this.itemMapper = itemMapper;
        this.supplyMapper = supplyMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.stockMapper = stockMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.noGenerator = noGenerator;
        this.configService = configService;
    }

    @GetMapping
    @RequirePerm("purchase:view")
    public ApiResult<PageResult<PurchaseOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long supplierId) {
        Page<PurchaseOrder> result = purchaseMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(status != null && !status.isBlank(), PurchaseOrder::getStatus, status)
                        .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                        .orderByDesc(PurchaseOrder::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("purchase:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        PurchaseOrder order = purchaseMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("采购单不存在");
        }
        List<PurchaseOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getPurchaseId, id));
        Map<Long, ProductSku> skus = items.isEmpty() ? Map.of() : skuMapper.selectList(
                        new LambdaQueryWrapper<ProductSku>().in(ProductSku::getId,
                                items.stream().map(PurchaseOrderItem::getSkuId).toList()))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        List<Map<String, Object>> rows = items.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("item", item);
            ProductSku sku = skus.get(item.getSkuId());
            row.put("skuCode", sku == null ? "" : sku.getSkuCode());
            row.put("barcode", sku == null ? "" : sku.getBarcode());
            row.put("skuName", sku == null ? "" : skuName(sku));
            return row;
        }).toList();
        return ApiResult.ok(Map.of("order", order, "items", rows));
    }

    @PostMapping
    @RequirePerm("purchase:create")
    @Transactional
    public ApiResult<PurchaseOrder> create(@RequestBody PurchaseSaveRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请添加采购商品");
        }
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(noGenerator.next("PO"));
        order.setSupplierId(request.getSupplierId());
        order.setStatus("DRAFT");
        order.setExpectedAt(request.getExpectedAt());
        order.setRemark(request.getRemark());
        order.setCreatedBy(AuthContext.require().getRealName());
        order.setTotalAmount(BigDecimal.ZERO);
        purchaseMapper.insert(order);
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseSaveRequest.Item req : request.getItems()) {
            ProductSku sku = skuMapper.selectById(req.getSkuId());
            if (sku == null || req.getQty() == null || req.getQty() <= 0) {
                continue;
            }
            BigDecimal price = req.getPrice() == null ? sku.getCostPrice() : req.getPrice();
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseId(order.getId());
            item.setSkuId(sku.getId());
            item.setQty(req.getQty());
            item.setReceivedQty(0);
            item.setPrice(price);
            item.setAmount(price.multiply(BigDecimal.valueOf(req.getQty())));
            itemMapper.insert(item);
            total = total.add(item.getAmount());
        }
        order.setTotalAmount(total);
        purchaseMapper.updateById(order);
        return ApiResult.ok(order);
    }

    @PutMapping("/{id}")
    @RequirePerm("purchase:update")
    @Transactional
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody PurchaseSaveRequest request) {
        PurchaseOrder order = require(id);
        if (!"DRAFT".equals(order.getStatus()) && !"SUBMITTED".equals(order.getStatus())) {
            throw new BizException("当前状态不可修改");
        }
        order.setSupplierId(request.getSupplierId());
        order.setExpectedAt(request.getExpectedAt());
        order.setRemark(request.getRemark());
        purchaseMapper.updateById(order);
        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getPurchaseId, id));
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseSaveRequest.Item req : request.getItems()) {
            ProductSku sku = skuMapper.selectById(req.getSkuId());
            if (sku == null || req.getQty() == null || req.getQty() <= 0) {
                continue;
            }
            BigDecimal price = req.getPrice() == null ? sku.getCostPrice() : req.getPrice();
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseId(id);
            item.setSkuId(sku.getId());
            item.setQty(req.getQty());
            item.setReceivedQty(0);
            item.setPrice(price);
            item.setAmount(price.multiply(BigDecimal.valueOf(req.getQty())));
            itemMapper.insert(item);
            total = total.add(item.getAmount());
        }
        order.setTotalAmount(total);
        purchaseMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/submit")
    @RequirePerm("purchase:update")
    public ApiResult<Void> submit(@PathVariable Long id) {
        PurchaseOrder order = require(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BizException("仅草稿可提交");
        }
        order.setStatus("SUBMITTED");
        purchaseMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/approve")
    @RequirePerm("purchase:audit")
    public ApiResult<Void> approve(@PathVariable Long id) {
        PurchaseOrder order = require(id);
        if (!"SUBMITTED".equals(order.getStatus())) {
            throw new BizException("仅已提交采购单可审核");
        }
        order.setStatus("APPROVED");
        purchaseMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/cancel")
    @RequirePerm("purchase:update")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        PurchaseOrder order = require(id);
        if ("DONE".equals(order.getStatus())) {
            throw new BizException("已完成采购单不可取消");
        }
        order.setStatus("CANCELLED");
        purchaseMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/to-supply")
    @RequirePerm("purchase:create")
    @Transactional
    public ApiResult<SupplyOrder> toSupply(@PathVariable Long id, @RequestParam Long storeId) {
        PurchaseOrder purchase = require(id);
        if (!"APPROVED".equals(purchase.getStatus()) && !"SUBMITTED".equals(purchase.getStatus())) {
            throw new BizException("采购单未审核通过");
        }
        List<PurchaseOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getPurchaseId, id));
        SupplyOrder supply = new SupplyOrder();
        supply.setOrderNo(noGenerator.next("SO"));
        supply.setSourceType(purchase.getSupplierId() == null ? "HQ" : "SUPPLIER");
        supply.setSupplierId(purchase.getSupplierId());
        supply.setStoreId(storeId);
        supply.setStatus("PENDING");
        supply.setExpectedAt(purchase.getExpectedAt());
        supply.setTotalAmount(BigDecimal.ZERO);
        supply.setRemark("由采购单 " + purchase.getOrderNo() + " 生成");
        supplyMapper.insert(supply);
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            com.retail.convenience.modules.purchase.entity.SupplyOrderItem supplyItem =
                    new com.retail.convenience.modules.purchase.entity.SupplyOrderItem();
            supplyItem.setSupplyId(supply.getId());
            supplyItem.setSkuId(item.getSkuId());
            supplyItem.setQty(item.getQty());
            supplyItem.setShippedQty(0);
            supplyItem.setReceivedQty(0);
            supplyItem.setPrice(item.getPrice());
            supplyItem.setAmount(item.getAmount());
            // 复用 Mapper 需控制器注入，改为直接使用 MyBatis-Plus 通用方法不可行，故通过 JDBC 批量插入
            jdbcTemplate.update("INSERT INTO supply_order_item(tenant_id, supply_id, sku_id, qty, shipped_qty, received_qty, price, amount) "
                            + "VALUES (?,?,?,?,?,?,?,?)", AuthContext.require().getTenantId(), supply.getId(),
                    item.getSkuId(), item.getQty(), 0, 0, item.getPrice(), item.getAmount());
            total = total.add(item.getAmount());
        }
        supply.setTotalAmount(total);
        supplyMapper.updateById(supply);
        return ApiResult.ok(supply);
    }

    @GetMapping("/replenishment")
    @RequirePerm("purchase:view")
    public ApiResult<List<Map<String, Object>>> replenishment(@RequestParam Long storeId) {
        LoginUser user = AuthContext.require();
        if (("STORE_MANAGER".equals(user.getRoleCode()) || "CASHIER".equals(user.getRoleCode()))
                && user.getStoreId() != null && !user.getStoreId().equals(storeId)) {
            throw BizException.forbidden("仅可查看本门店补货建议");
        }
        Long tenantId = user.getTenantId();
        int safetyDays = configService.getInt(tenantId, "stock.safety_days", 2);
        double coefficient = configService.getDouble(tenantId, "stock.replenish_coefficient", 1.2);
        Map<Long, Double> avgSales = new HashMap<>();
        jdbcTemplate.query("SELECT i.sku_id, SUM(i.qty) / 30.0 AS avg_qty FROM sales_order_item i "
                        + "JOIN sales_order o ON o.id = i.order_id AND o.tenant_id = i.tenant_id "
                        + "WHERE o.tenant_id = ? AND o.store_id = ? AND o.status = 'COMPLETED' "
                        + "AND o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY i.sku_id",
                rs -> {
                    avgSales.put(rs.getLong("sku_id"), rs.getDouble("avg_qty"));
                }, tenantId, storeId);
        Map<Long, Integer> inTransit = new HashMap<>();
        jdbcTemplate.query("SELECT si.sku_id, SUM(si.qty - si.received_qty) AS transit FROM supply_order_item si "
                        + "JOIN supply_order s ON s.id = si.supply_id AND s.tenant_id = si.tenant_id "
                        + "WHERE s.tenant_id = ? AND s.store_id = ? AND s.status IN ('PENDING','ACCEPTED','SHIPPED','PARTIAL') "
                        + "GROUP BY si.sku_id",
                rs -> {
                    inTransit.put(rs.getLong("sku_id"), rs.getInt("transit"));
                }, tenantId, storeId);
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getOwnerType, "STORE").eq(Stock::getOwnerId, storeId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Stock stock : stocks) {
            ProductSku sku = skuMapper.selectById(stock.getSkuId());
            if (sku == null) {
                continue;
            }
            ProductSpu spu = spuMapper.selectById(sku.getSpuId());
            double avg = avgSales.getOrDefault(stock.getSkuId(), 0d);
            int transit = inTransit.getOrDefault(stock.getSkuId(), 0);
            int safety = Math.max(stock.getSafetyStock() == null ? 10 : stock.getSafetyStock(),
                    (int) Math.ceil(avg * safetyDays));
            int targetDays = Math.min(7, Math.max(1, (int) Math.round((spu == null || spu.getShelfLifeDays() == null
                    ? 7 : spu.getShelfLifeDays()) * 0.7)));
            int suggestion = (int) Math.ceil(avg * targetDays * coefficient + safety - stock.getQty() - transit);
            if (suggestion < 0) {
                suggestion = 0;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("skuId", stock.getSkuId());
            row.put("skuCode", sku.getSkuCode());
            row.put("barcode", sku.getBarcode());
            row.put("skuName", spu == null ? sku.getSkuCode() : spu.getName());
            row.put("onHand", stock.getQty());
            row.put("locked", stock.getLockedQty());
            row.put("inTransit", transit);
            row.put("avgDaily", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            row.put("safetyStock", safety);
            row.put("targetDays", targetDays);
            row.put("suggestion", suggestion);
            row.put("price", sku.getPriceFranchise());
            row.put("storageCondition", spu == null ? "NORMAL" : spu.getStorageCondition());
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt(r -> -((Number) r.get("suggestion")).intValue()));
        return ApiResult.ok(rows);
    }

    private PurchaseOrder require(Long id) {
        PurchaseOrder order = purchaseMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("采购单不存在");
        }
        return order;
    }

    private String skuName(ProductSku sku) {
        ProductSpu spu = spuMapper.selectById(sku.getSpuId());
        return spu == null ? sku.getSkuCode() : spu.getName();
    }
}
