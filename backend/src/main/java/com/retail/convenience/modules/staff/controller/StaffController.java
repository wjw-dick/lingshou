package com.retail.convenience.modules.staff.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.modules.inventory.entity.LossOrder;
import com.retail.convenience.modules.inventory.entity.StockCheck;
import com.retail.convenience.modules.inventory.entity.StockFlow;
import com.retail.convenience.modules.inventory.entity.TransferOrder;
import com.retail.convenience.modules.inventory.mapper.LossOrderMapper;
import com.retail.convenience.modules.inventory.mapper.StockCheckMapper;
import com.retail.convenience.modules.inventory.mapper.StockFlowMapper;
import com.retail.convenience.modules.inventory.mapper.TransferOrderMapper;
import com.retail.convenience.modules.inventory.service.InventoryService;
import com.retail.convenience.modules.purchase.entity.ReceiveOrder;
import com.retail.convenience.modules.purchase.mapper.ReceiveOrderMapper;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.mapper.SalesOrderMapper;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.system.entity.TenantNotice;
import com.retail.convenience.modules.system.mapper.TenantNoticeMapper;
import com.retail.convenience.modules.system.service.SystemConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final ReceiveOrderMapper receiveMapper;
    private final SalesOrderMapper orderMapper;
    private final TransferOrderMapper transferMapper;
    private final StockCheckMapper checkMapper;
    private final LossOrderMapper lossMapper;
    private final StockFlowMapper flowMapper;
    private final StoreMapper storeMapper;
    private final TenantNoticeMapper noticeMapper;
    private final InventoryService inventoryService;
    private final SystemConfigService configService;

    public StaffController(ReceiveOrderMapper receiveMapper, SalesOrderMapper orderMapper,
                           TransferOrderMapper transferMapper, StockCheckMapper checkMapper,
                           LossOrderMapper lossMapper, StockFlowMapper flowMapper, StoreMapper storeMapper,
                           TenantNoticeMapper noticeMapper, InventoryService inventoryService,
                           SystemConfigService configService) {
        this.receiveMapper = receiveMapper;
        this.orderMapper = orderMapper;
        this.transferMapper = transferMapper;
        this.checkMapper = checkMapper;
        this.lossMapper = lossMapper;
        this.flowMapper = flowMapper;
        this.storeMapper = storeMapper;
        this.noticeMapper = noticeMapper;
        this.inventoryService = inventoryService;
        this.configService = configService;
    }

    @GetMapping("/workbench")
    public ApiResult<Map<String, Object>> workbench(@RequestParam(required = false) Long storeId) {
        LoginUser user = AuthContext.require();
        Long actualStore = resolveStore(user, storeId);
        Store store = storeMapper.selectById(actualStore);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long pendingReceive = receiveMapper.selectCount(new LambdaQueryWrapper<ReceiveOrder>()
                .eq(ReceiveOrder::getStoreId, actualStore).eq(ReceiveOrder::getStatus, "PENDING"));
        long pendingPick = orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getStoreId, actualStore)
                .in(SalesOrder::getStatus, "PENDING_ACCEPT", "PICKING"));
        long waitPickup = orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getStoreId, actualStore).eq(SalesOrder::getStatus, "WAIT_PICKUP"));
        long delivering = orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getStoreId, actualStore).eq(SalesOrder::getStatus, "DELIVERING"));
        long pendingTransfers = transferMapper.selectCount(new LambdaQueryWrapper<TransferOrder>()
                .eq(TransferOrder::getToId, actualStore).eq(TransferOrder::getStatus, "PENDING"));
        long pendingChecks = checkMapper.selectCount(new LambdaQueryWrapper<StockCheck>()
                .eq(StockCheck::getStoreId, actualStore).eq(StockCheck::getStatus, "PENDING"));
        long pendingLosses = lossMapper.selectCount(new LambdaQueryWrapper<LossOrder>()
                .eq(LossOrder::getStoreId, actualStore).eq(LossOrder::getStatus, "PENDING"));
        List<SalesOrder> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getStoreId, actualStore)
                .eq(SalesOrder::getPayStatus, "PAID")
                .ge(SalesOrder::getPaidAt, todayStart));
        BigDecimal todaySales = todayOrders.stream().map(SalesOrder::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int nearDays = configService.getInt(user.getTenantId(), "expiry.near_days", 3);
        long nearExpiry = inventoryService.nearExpiry(actualStore, nearDays).size();
        List<StockFlow> recentFlows = flowMapper.selectList(new LambdaQueryWrapper<StockFlow>()
                .eq(StockFlow::getOwnerType, "STORE").eq(StockFlow::getOwnerId, actualStore)
                .orderByDesc(StockFlow::getCreatedAt).last("limit 10"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("store", store);
        map.put("pendingReceive", pendingReceive);
        map.put("pendingPick", pendingPick);
        map.put("waitPickup", waitPickup);
        map.put("delivering", delivering);
        map.put("pendingTransfers", pendingTransfers);
        map.put("pendingChecks", pendingChecks);
        map.put("pendingLosses", pendingLosses);
        map.put("nearExpiry", nearExpiry);
        map.put("todayOrderCount", todayOrders.size());
        map.put("todaySales", todaySales);
        map.put("recentFlows", recentFlows);
        map.put("notices", noticeMapper.selectList(new LambdaQueryWrapper<TenantNotice>()
                .orderByDesc(TenantNotice::getPublishedAt).last("limit 5")));
        return ApiResult.ok(map);
    }

    private Long resolveStore(LoginUser user, Long storeId) {
        if (user.getStoreId() != null) {
            return user.getStoreId();
        }
        if (storeId == null) {
            throw new BizException("请先绑定门店");
        }
        return storeId;
    }
}
