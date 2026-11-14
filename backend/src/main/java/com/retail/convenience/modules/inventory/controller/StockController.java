package com.retail.convenience.modules.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.modules.inventory.entity.Stock;
import com.retail.convenience.modules.inventory.entity.StockBatch;
import com.retail.convenience.modules.inventory.entity.StockFlow;
import com.retail.convenience.modules.inventory.mapper.StockBatchMapper;
import com.retail.convenience.modules.inventory.mapper.StockMapper;
import com.retail.convenience.modules.inventory.service.InventoryService;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.system.service.SystemConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/inventory")
public class StockController {

    private final StockMapper stockMapper;
    private final StockBatchMapper batchMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final InventoryService inventoryService;
    private final SystemConfigService configService;
    private final JdbcTemplate jdbcTemplate;

    public StockController(StockMapper stockMapper, StockBatchMapper batchMapper,
                           ProductSkuMapper skuMapper, ProductSpuMapper spuMapper,
                           InventoryService inventoryService, SystemConfigService configService,
                           JdbcTemplate jdbcTemplate) {
        this.stockMapper = stockMapper;
        this.batchMapper = batchMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.inventoryService = inventoryService;
        this.configService = configService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/stocks")
    @RequirePerm("inventory:view")
    public ApiResult<List<Map<String, Object>>> stocks(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "STORE") String ownerType,
            @RequestParam(defaultValue = "1") Long ownerId,
            @RequestParam(required = false) String keyword) {
        Long actualOwner = "STORE".equals(ownerType) ? resolveStore(storeId) : ownerId;
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getOwnerType, ownerType)
                .eq(Stock::getOwnerId, actualOwner)
                .orderByDesc(Stock::getQty));
        Map<Long, ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getDeleted, 0))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Map<Long, ProductSpu> spus = spuMapper.selectList(new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getDeleted, 0))
                .stream().collect(Collectors.toMap(ProductSpu::getId, Function.identity()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Stock stock : stocks) {
            ProductSku sku = skus.get(stock.getSkuId());
            if (sku == null) {
                continue;
            }
            ProductSpu spu = spus.get(sku.getSpuId());
            String name = spu == null ? sku.getSkuCode() : spu.getName();
            if (keyword != null && !keyword.isBlank()
                    && !(name.contains(keyword) || (sku.getBarcode() != null && sku.getBarcode().contains(keyword)))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stock", stock);
            row.put("skuCode", sku.getSkuCode());
            row.put("barcode", sku.getBarcode());
            row.put("skuName", name);
            row.put("spec", sku.getSpec());
            row.put("storageCondition", spu == null ? "NORMAL" : spu.getStorageCondition());
            row.put("price", sku.getPriceRetail());
            rows.add(row);
        }
        return ApiResult.ok(rows);
    }

    @GetMapping("/stocks/{skuId}/batches")
    @RequirePerm("inventory:view")
    public ApiResult<List<StockBatch>> batches(@PathVariable Long skuId,
                                               @RequestParam(required = false) Long storeId,
                                               @RequestParam(defaultValue = "STORE") String ownerType,
                                               @RequestParam(defaultValue = "1") Long ownerId) {
        Long actualOwner = "STORE".equals(ownerType) ? resolveStore(storeId) : ownerId;
        return ApiResult.ok(batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getOwnerType, ownerType)
                .eq(StockBatch::getOwnerId, actualOwner)
                .eq(StockBatch::getSkuId, skuId)
                .gt(StockBatch::getQty, 0)
                .orderByAsc(StockBatch::getExpiryDate)));
    }

    @GetMapping("/flows")
    @RequirePerm("inventory:view")
    public ApiResult<List<StockFlow>> flows(@RequestParam(required = false) Long storeId,
                                            @RequestParam(required = false) Long skuId,
                                            @RequestParam(defaultValue = "STORE") String ownerType,
                                            @RequestParam(defaultValue = "1") Long ownerId,
                                            @RequestParam(defaultValue = "50") int limit) {
        Long actualOwner = "STORE".equals(ownerType) ? resolveStore(storeId) : ownerId;
        return ApiResult.ok(inventoryService.flows(ownerType, actualOwner, skuId, limit));
    }

    @GetMapping("/alerts")
    @RequirePerm("inventory:view")
    public ApiResult<Map<String, Object>> alerts(@RequestParam(required = false) Long storeId) {
        Long actualStore = resolveStore(storeId);
        int nearDays = configService.getInt(AuthContext.require().getTenantId(), "expiry.near_days", 3);
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getOwnerType, "STORE").eq(Stock::getOwnerId, actualStore));
        Map<Long, Integer> sales30 = new HashMap<>();
        jdbcTemplate.query("SELECT i.sku_id, SUM(i.qty) AS qty FROM sales_order_item i "
                        + "JOIN sales_order o ON o.id = i.order_id AND o.tenant_id = i.tenant_id "
                        + "WHERE o.tenant_id = ? AND o.store_id = ? AND o.status = 'COMPLETED' "
                        + "AND o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY i.sku_id",
                rs -> {
                    sales30.put(rs.getLong("sku_id"), rs.getInt("qty"));
                }, AuthContext.require().getTenantId(), actualStore);
        List<Map<String, Object>> low = new ArrayList<>();
        List<Map<String, Object>> slow = new ArrayList<>();
        List<Map<String, Object>> over = new ArrayList<>();
        Map<Long, ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getDeleted, 0))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        for (Stock stock : stocks) {
            ProductSku sku = skus.get(stock.getSkuId());
            if (sku == null) {
                continue;
            }
            ProductSpu spu = spuMapper.selectById(sku.getSpuId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stock", stock);
            row.put("skuName", spu == null ? sku.getSkuCode() : spu.getName());
            row.put("barcode", sku.getBarcode());
            int available = stock.getQty() - (stock.getLockedQty() == null ? 0 : stock.getLockedQty());
            if (available <= (stock.getSafetyStock() == null ? 10 : stock.getSafetyStock())) {
                low.add(row);
            }
            int sold = sales30.getOrDefault(stock.getSkuId(), 0);
            if (stock.getQty() > 0 && sold == 0) {
                slow.add(row);
            }
            if (stock.getQty() > (stock.getSafetyStock() == null ? 10 : stock.getSafetyStock()) * 5) {
                over.add(row);
            }
        }
        List<Map<String, Object>> near = inventoryService.nearExpiry(actualStore, nearDays).stream()
                .map(batch -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    ProductSku sku = skus.get(batch.getSkuId());
                    ProductSpu spu = sku == null ? null : spuMapper.selectById(sku.getSpuId());
                    row.put("batch", batch);
                    row.put("skuName", spu == null ? "-" : spu.getName());
                    row.put("barcode", sku == null ? "" : sku.getBarcode());
                    row.put("daysLeft", batch.getExpiryDate() == null ? null
                            : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate()));
                    return row;
                })
                .sorted(Comparator.comparingLong(r -> ((Number) r.get("daysLeft")).longValue()))
                .toList();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("lowStock", low);
        map.put("nearExpiry", near);
        map.put("slowMoving", slow);
        map.put("overstock", over);
        return ApiResult.ok(map);
    }

    private Long resolveStore(Long storeId) {
        LoginUser user = AuthContext.require();
        if (("STORE_MANAGER".equals(user.getRoleCode()) || "CASHIER".equals(user.getRoleCode()))
                && user.getStoreId() != null) {
            return user.getStoreId();
        }
        if (storeId == null) {
            throw new BizException("请选择门店");
        }
        return storeId;
    }
}
