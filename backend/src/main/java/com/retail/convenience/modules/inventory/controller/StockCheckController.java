package com.retail.convenience.modules.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.inventory.entity.Stock;
import com.retail.convenience.modules.inventory.entity.StockCheck;
import com.retail.convenience.modules.inventory.entity.StockCheckItem;
import com.retail.convenience.modules.inventory.mapper.StockCheckItemMapper;
import com.retail.convenience.modules.inventory.mapper.StockCheckMapper;
import com.retail.convenience.modules.inventory.mapper.StockMapper;
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
@RequestMapping("/api/v1/tenant/stock-checks")
public class StockCheckController {

    private final StockCheckMapper checkMapper;
    private final StockCheckItemMapper itemMapper;
    private final StockMapper stockMapper;
    private final InventoryService inventoryService;
    private final NoGenerator noGenerator;

    public StockCheckController(StockCheckMapper checkMapper, StockCheckItemMapper itemMapper,
                                StockMapper stockMapper, InventoryService inventoryService,
                                NoGenerator noGenerator) {
        this.checkMapper = checkMapper;
        this.itemMapper = itemMapper;
        this.stockMapper = stockMapper;
        this.inventoryService = inventoryService;
        this.noGenerator = noGenerator;
    }

    @GetMapping
    @RequirePerm("inventory:view")
    public ApiResult<PageResult<StockCheck>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status) {
        Page<StockCheck> result = checkMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<StockCheck>()
                        .eq(storeId != null, StockCheck::getStoreId, storeId)
                        .eq(status != null && !status.isBlank(), StockCheck::getStatus, status)
                        .orderByDesc(StockCheck::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("inventory:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        StockCheck check = require(id);
        List<StockCheckItem> items = itemMapper.selectList(new LambdaQueryWrapper<StockCheckItem>()
                .eq(StockCheckItem::getCheckId, id));
        return ApiResult.ok(Map.of("order", check, "items", items));
    }

    @PostMapping
    @RequirePerm("inventory:create")
    @Transactional
    public ApiResult<StockCheck> create(@RequestBody CheckCreateRequest request) {
        if (request.getStoreId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请选择门店并录入盘点商品");
        }
        StockCheck check = new StockCheck();
        check.setCheckNo(noGenerator.next("SC"));
        check.setStoreId(request.getStoreId());
        check.setCheckType(request.getCheckType() == null ? "FULL" : request.getCheckType());
        check.setStatus("PENDING");
        check.setRemark(request.getRemark());
        check.setCreatedBy(AuthContext.require().getRealName());
        check.setTotalDiffQty(0);
        checkMapper.insert(check);
        int totalDiff = 0;
        for (CheckCreateRequest.Item req : request.getItems()) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getOwnerType, "STORE").eq(Stock::getOwnerId, request.getStoreId())
                    .eq(Stock::getSkuId, req.getSkuId()).last("limit 1"));
            int book = stock == null ? 0 : stock.getQty();
            int actual = req.getActualQty() == null ? book : req.getActualQty();
            StockCheckItem item = new StockCheckItem();
            item.setCheckId(check.getId());
            item.setSkuId(req.getSkuId());
            item.setBookQty(book);
            item.setActualQty(actual);
            item.setDiffQty(actual - book);
            item.setReason(req.getReason());
            itemMapper.insert(item);
            totalDiff += actual - book;
        }
        check.setTotalDiffQty(totalDiff);
        checkMapper.updateById(check);
        return ApiResult.ok(check);
    }

    @PostMapping("/{id}/approve")
    @RequirePerm("inventory:audit")
    @Transactional
    public ApiResult<Void> approve(@PathVariable Long id) {
        StockCheck check = require(id);
        if (!"PENDING".equals(check.getStatus())) {
            throw new BizException("当前状态不可审核");
        }
        List<StockCheckItem> items = itemMapper.selectList(new LambdaQueryWrapper<StockCheckItem>()
                .eq(StockCheckItem::getCheckId, id));
        String operator = AuthContext.require().getUsername();
        for (StockCheckItem item : items) {
            if (item.getDiffQty() != null && item.getDiffQty() != 0) {
                inventoryService.adjust("STORE", check.getStoreId(), item.getSkuId(), item.getActualQty(),
                        "CHECK", "CHECK", check.getId(), "盘点差异调整", operator);
            }
        }
        check.setStatus("APPROVED");
        check.setApprovedBy(AuthContext.require().getRealName());
        checkMapper.updateById(check);
        return ApiResult.ok();
    }

    private StockCheck require(Long id) {
        StockCheck check = checkMapper.selectById(id);
        if (check == null) {
            throw BizException.notFound("盘点单不存在");
        }
        return check;
    }

    @lombok.Data
    public static class CheckCreateRequest {
        private Long storeId;
        private String checkType;
        private String remark;
        private List<Item> items;

        @lombok.Data
        public static class Item {
            private Long skuId;
            private Integer actualQty;
            private String reason;
        }
    }
}
