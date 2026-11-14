package com.retail.convenience.modules.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.inventory.entity.TransferOrder;
import com.retail.convenience.modules.inventory.entity.TransferOrderItem;
import com.retail.convenience.modules.inventory.mapper.TransferOrderItemMapper;
import com.retail.convenience.modules.inventory.mapper.TransferOrderMapper;
import com.retail.convenience.modules.inventory.service.InventoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/transfers")
public class TransferController {

    private final TransferOrderMapper transferMapper;
    private final TransferOrderItemMapper itemMapper;
    private final InventoryService inventoryService;
    private final NoGenerator noGenerator;

    public TransferController(TransferOrderMapper transferMapper, TransferOrderItemMapper itemMapper,
                              InventoryService inventoryService, NoGenerator noGenerator) {
        this.transferMapper = transferMapper;
        this.itemMapper = itemMapper;
        this.inventoryService = inventoryService;
        this.noGenerator = noGenerator;
    }

    @GetMapping
    @RequirePerm("inventory:view")
    public ApiResult<PageResult<TransferOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        LoginUser user = AuthContext.require();
        Page<TransferOrder> result = transferMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<TransferOrder>()
                        .eq(status != null && !status.isBlank(), TransferOrder::getStatus, status)
                        .eq(("STORE_MANAGER".equals(user.getRoleCode()) || "CASHIER".equals(user.getRoleCode()))
                                        && user.getStoreId() != null,
                                TransferOrder::getToId, user.getStoreId())
                        .orderByDesc(TransferOrder::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("inventory:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        TransferOrder order = require(id);
        List<TransferOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<TransferOrderItem>()
                .eq(TransferOrderItem::getTransferId, id));
        return ApiResult.ok(Map.of("order", order, "items", items));
    }

    @PostMapping
    @RequirePerm("inventory:create")
    @Transactional
    public ApiResult<TransferOrder> create(@RequestBody TransferCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请添加调拨商品");
        }
        TransferOrder order = new TransferOrder();
        order.setTransferNo(noGenerator.next("TR"));
        order.setFromType(request.getFromType() == null ? "WAREHOUSE" : request.getFromType());
        order.setFromId(request.getFromId() == null ? 1L : request.getFromId());
        order.setToType(request.getToType() == null ? "STORE" : request.getToType());
        order.setToId(request.getToId());
        order.setStatus("PENDING");
        order.setTotalQty(0);
        order.setRemark(request.getRemark());
        order.setAppliedBy(AuthContext.require().getRealName());
        transferMapper.insert(order);
        int total = 0;
        for (TransferCreateRequest.Item req : request.getItems()) {
            if (req.getQty() == null || req.getQty() <= 0) {
                continue;
            }
            TransferOrderItem item = new TransferOrderItem();
            item.setTransferId(order.getId());
            item.setSkuId(req.getSkuId());
            item.setQty(req.getQty());
            item.setBatchNo(req.getBatchNo());
            itemMapper.insert(item);
            total += req.getQty();
        }
        order.setTotalQty(total);
        transferMapper.updateById(order);
        return ApiResult.ok(order);
    }

    @PostMapping("/{id}/approve")
    @RequirePerm("inventory:audit")
    public ApiResult<Void> approve(@PathVariable Long id) {
        TransferOrder order = require(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException("当前状态不可审核");
        }
        order.setStatus("APPROVED");
        order.setApprovedBy(AuthContext.require().getRealName());
        transferMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reject")
    @RequirePerm("inventory:audit")
    public ApiResult<Void> reject(@PathVariable Long id) {
        TransferOrder order = require(id);
        order.setStatus("REJECTED");
        order.setApprovedBy(AuthContext.require().getRealName());
        transferMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/complete")
    @RequirePerm("inventory:update")
    @Transactional
    public ApiResult<Void> complete(@PathVariable Long id) {
        TransferOrder order = require(id);
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException("仅审核通过的调拨单可执行");
        }
        List<TransferOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<TransferOrderItem>()
                .eq(TransferOrderItem::getTransferId, id));
        String operator = AuthContext.require().getUsername();
        for (TransferOrderItem item : items) {
            inventoryService.deduct(order.getFromType(), order.getFromId(), item.getSkuId(), item.getQty(),
                    "TRANSFER_OUT", "TRANSFER", order.getId(), "调拨出库", operator);
            inventoryService.inbound(order.getToType(), order.getToId(), item.getSkuId(), item.getQty(),
                    item.getBatchNo(), java.time.LocalDate.now(), java.time.LocalDate.now().plusYears(1),
                    "TRANSFER_IN", "TRANSFER", order.getId(), "调拨入库", operator);
        }
        order.setStatus("COMPLETED");
        transferMapper.updateById(order);
        return ApiResult.ok();
    }

    private TransferOrder require(Long id) {
        TransferOrder order = transferMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("调拨单不存在");
        }
        return order;
    }

    @lombok.Data
    public static class TransferCreateRequest {
        private String fromType = "WAREHOUSE";
        private Long fromId = 1L;
        private String toType = "STORE";
        private Long toId;
        private String remark;
        private List<Item> items;

        @lombok.Data
        public static class Item {
            private Long skuId;
            private Integer qty;
            private String batchNo;
        }
    }
}
