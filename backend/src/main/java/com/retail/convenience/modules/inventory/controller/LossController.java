package com.retail.convenience.modules.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.inventory.entity.LossOrder;
import com.retail.convenience.modules.inventory.entity.LossOrderItem;
import com.retail.convenience.modules.inventory.mapper.LossOrderItemMapper;
import com.retail.convenience.modules.inventory.mapper.LossOrderMapper;
import com.retail.convenience.modules.inventory.service.InventoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/loss-orders")
public class LossController {

    private final LossOrderMapper lossMapper;
    private final LossOrderItemMapper itemMapper;
    private final InventoryService inventoryService;
    private final NoGenerator noGenerator;

    public LossController(LossOrderMapper lossMapper, LossOrderItemMapper itemMapper,
                          InventoryService inventoryService, NoGenerator noGenerator) {
        this.lossMapper = lossMapper;
        this.itemMapper = itemMapper;
        this.inventoryService = inventoryService;
        this.noGenerator = noGenerator;
    }

    @GetMapping
    @RequirePerm("inventory:view")
    public ApiResult<PageResult<LossOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reasonType) {
        Page<LossOrder> result = lossMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<LossOrder>()
                        .eq(storeId != null, LossOrder::getStoreId, storeId)
                        .eq(status != null && !status.isBlank(), LossOrder::getStatus, status)
                        .eq(reasonType != null && !reasonType.isBlank(), LossOrder::getReasonType, reasonType)
                        .orderByDesc(LossOrder::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("inventory:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        LossOrder order = require(id);
        List<LossOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<LossOrderItem>()
                .eq(LossOrderItem::getLossId, id));
        return ApiResult.ok(Map.of("order", order, "items", items));
    }

    @PostMapping
    @RequirePerm("inventory:create")
    @Transactional
    public ApiResult<LossOrder> create(@RequestBody LossCreateRequest request) {
        if (request.getStoreId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请选择门店并添加报损商品");
        }
        LossOrder order = new LossOrder();
        order.setLossNo(noGenerator.next("LS"));
        order.setStoreId(request.getStoreId());
        order.setReasonType(request.getReasonType() == null ? "OTHER" : request.getReasonType());
        order.setStatus("PENDING");
        order.setPhotos(request.getPhotos());
        order.setRemark(request.getRemark());
        order.setCreatedBy(AuthContext.require().getRealName());
        order.setTotalQty(0);
        order.setTotalAmount(BigDecimal.ZERO);
        lossMapper.insert(order);
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (LossCreateRequest.Item req : request.getItems()) {
            if (req.getQty() == null || req.getQty() <= 0) {
                continue;
            }
            LossOrderItem item = new LossOrderItem();
            item.setLossId(order.getId());
            item.setSkuId(req.getSkuId());
            item.setBatchNo(req.getBatchNo());
            item.setQty(req.getQty());
            item.setAmount(req.getAmount() == null ? BigDecimal.ZERO : req.getAmount());
            itemMapper.insert(item);
            totalQty += req.getQty();
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        lossMapper.updateById(order);
        return ApiResult.ok(order);
    }

    @PostMapping("/{id}/approve")
    @RequirePerm("inventory:audit")
    @Transactional
    public ApiResult<Void> approve(@PathVariable Long id) {
        LossOrder order = require(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException("当前状态不可审核");
        }
        List<LossOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<LossOrderItem>()
                .eq(LossOrderItem::getLossId, id));
        String operator = AuthContext.require().getUsername();
        for (LossOrderItem item : items) {
            inventoryService.deduct("STORE", order.getStoreId(), item.getSkuId(), item.getQty(),
                    "LOSS", "LOSS", order.getId(), "报损出库：" + order.getReasonType(), operator);
        }
        order.setStatus("APPROVED");
        order.setApprovedBy(AuthContext.require().getRealName());
        lossMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reject")
    @RequirePerm("inventory:audit")
    public ApiResult<Void> reject(@PathVariable Long id) {
        LossOrder order = require(id);
        order.setStatus("REJECTED");
        order.setApprovedBy(AuthContext.require().getRealName());
        lossMapper.updateById(order);
        return ApiResult.ok();
    }

    private LossOrder require(Long id) {
        LossOrder order = lossMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("报损单不存在");
        }
        return order;
    }

    @lombok.Data
    public static class LossCreateRequest {
        private Long storeId;
        private String reasonType;
        private String photos;
        private String remark;
        private List<Item> items;

        @lombok.Data
        public static class Item {
            private Long skuId;
            private String batchNo;
            private Integer qty;
            private BigDecimal amount;
        }
    }
}
