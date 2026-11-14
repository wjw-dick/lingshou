package com.retail.convenience.common.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.security.TenantContext;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.inventory.entity.LossOrder;
import com.retail.convenience.modules.inventory.entity.LossOrderItem;
import com.retail.convenience.modules.inventory.entity.StockBatch;
import com.retail.convenience.modules.inventory.mapper.LossOrderItemMapper;
import com.retail.convenience.modules.inventory.mapper.LossOrderMapper;
import com.retail.convenience.modules.inventory.mapper.StockBatchMapper;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import com.retail.convenience.modules.platform.service.PlatformQuotaService;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.mapper.SalesOrderMapper;
import com.retail.convenience.modules.sales.service.SalesOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ScheduledTasks {

    private final PlatformTenantMapper tenantMapper;
    private final SalesOrderMapper orderMapper;
    private final SalesOrderService orderService;
    private final StockBatchMapper batchMapper;
    private final LossOrderMapper lossMapper;
    private final LossOrderItemMapper lossItemMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final PlatformQuotaService quotaService;
    private final NoGenerator noGenerator;

    public ScheduledTasks(PlatformTenantMapper tenantMapper, SalesOrderMapper orderMapper,
                          SalesOrderService orderService, StockBatchMapper batchMapper,
                          LossOrderMapper lossMapper, LossOrderItemMapper lossItemMapper,
                          ProductSkuMapper skuMapper, ProductSpuMapper spuMapper,
                          PlatformQuotaService quotaService, NoGenerator noGenerator) {
        this.tenantMapper = tenantMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.batchMapper = batchMapper;
        this.lossMapper = lossMapper;
        this.lossItemMapper = lossItemMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.quotaService = quotaService;
        this.noGenerator = noGenerator;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void cancelTimeoutOrders() {
        for (PlatformTenant tenant : activeTenants()) {
            TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
            try {
                List<SalesOrder> pendingPay = orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getStatus, "PENDING_PAY")
                        .lt(SalesOrder::getCreatedAt, LocalDateTime.now().minusMinutes(15)));
                for (SalesOrder order : pendingPay) {
                    try {
                        orderService.cancel(order.getId(), "超时未支付自动取消");
                    } catch (Exception e) {
                        log.warn("取消超时未支付订单失败 {}: {}", order.getOrderNo(), e.getMessage());
                    }
                }
                List<SalesOrder> pendingAccept = orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getStatus, "PENDING_ACCEPT")
                        .isNotNull(SalesOrder::getAcceptDeadline)
                        .lt(SalesOrder::getAcceptDeadline, LocalDateTime.now()));
                for (SalesOrder order : pendingAccept) {
                    try {
                        orderService.cancel(order.getId(), "支付成功但门店超时未接单，自动取消并退款");
                    } catch (Exception e) {
                        log.warn("超时未接单自动取消失败 {}: {}", order.getOrderNo(), e.getMessage());
                    }
                }
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void expiredStockToLoss() {
        for (PlatformTenant tenant : activeTenants()) {
            TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
            try {
                List<StockBatch> expired = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                        .eq(StockBatch::getOwnerType, "STORE")
                        .gt(StockBatch::getQty, 0)
                        .isNotNull(StockBatch::getExpiryDate)
                        .lt(StockBatch::getExpiryDate, LocalDate.now()));
                Map<Long, List<StockBatch>> byStore = new LinkedHashMap<>();
                for (StockBatch batch : expired) {
                    byStore.computeIfAbsent(batch.getOwnerId(), k -> new ArrayList<>()).add(batch);
                }
                for (Map.Entry<Long, List<StockBatch>> entry : byStore.entrySet()) {
                    boolean exists = lossMapper.selectCount(new LambdaQueryWrapper<LossOrder>()
                            .eq(LossOrder::getStoreId, entry.getKey())
                            .eq(LossOrder::getReasonType, "EXPIRED")
                            .eq(LossOrder::getStatus, "PENDING")
                            .ge(LossOrder::getCreatedAt, LocalDate.now().atStartOfDay())) > 0;
                    if (exists) {
                        continue;
                    }
                    LossOrder loss = new LossOrder();
                    loss.setLossNo(noGenerator.next("LS"));
                    loss.setStoreId(entry.getKey());
                    loss.setReasonType("EXPIRED");
                    loss.setStatus("PENDING");
                    loss.setRemark("系统自动生成：过期批次待处理");
                    loss.setCreatedBy("system");
                    loss.setTotalQty(0);
                    loss.setTotalAmount(java.math.BigDecimal.ZERO);
                    lossMapper.insert(loss);
                    int total = 0;
                    for (StockBatch batch : entry.getValue()) {
                        LossOrderItem item = new LossOrderItem();
                        item.setLossId(loss.getId());
                        item.setSkuId(batch.getSkuId());
                        item.setBatchNo(batch.getBatchNo());
                        item.setQty(batch.getQty());
                        item.setAmount(java.math.BigDecimal.ZERO);
                        lossItemMapper.insert(item);
                        total += batch.getQty();
                        ProductSku sku = new ProductSku();
                        sku.setId(batch.getSkuId());
                        sku.setStatus("OFF_SHELF");
                        skuMapper.updateById(sku);
                        ProductSku fullSku = skuMapper.selectById(batch.getSkuId());
                        if (fullSku != null) {
                            com.retail.convenience.modules.product.entity.ProductSpu spu =
                                    new com.retail.convenience.modules.product.entity.ProductSpu();
                            spu.setId(fullSku.getSpuId());
                            spu.setStatus("OFF_SHELF");
                            spuMapper.updateById(spu);
                        }
                    }
                    loss.setTotalQty(total);
                    lossMapper.updateById(loss);
                }
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(cron = "0 5 1 * * ?")
    public void refreshQuotaUsage() {
        for (PlatformTenant tenant : activeTenants()) {
            TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
            try {
                quotaService.refreshDailyUsage(tenant.getTenantId());
            } catch (Exception e) {
                log.warn("刷新租户配额失败 {}: {}", tenant.getTenantCode(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private List<PlatformTenant> activeTenants() {
        return tenantMapper.selectList(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getDeleted, 0)
                .in(PlatformTenant::getStatus, "ACTIVE", "TRIAL", "OVERDUE"));
    }
}
