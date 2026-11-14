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
import com.retail.convenience.modules.inventory.service.InventoryService;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.purchase.dto.ReceiveConfirmRequest;
import com.retail.convenience.modules.purchase.entity.DifferenceOrder;
import com.retail.convenience.modules.purchase.entity.ReceiveOrder;
import com.retail.convenience.modules.purchase.entity.ReceiveOrderItem;
import com.retail.convenience.modules.purchase.entity.SupplyOrder;
import com.retail.convenience.modules.purchase.entity.SupplyOrderItem;
import com.retail.convenience.modules.purchase.mapper.DifferenceOrderMapper;
import com.retail.convenience.modules.purchase.mapper.ReceiveOrderItemMapper;
import com.retail.convenience.modules.purchase.mapper.ReceiveOrderMapper;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderItemMapper;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/receive-orders")
public class ReceiveOrderController {

    private final ReceiveOrderMapper receiveMapper;
    private final ReceiveOrderItemMapper itemMapper;
    private final SupplyOrderMapper supplyMapper;
    private final SupplyOrderItemMapper supplyItemMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final DifferenceOrderMapper differenceMapper;
    private final InventoryService inventoryService;
    private final NoGenerator noGenerator;

    public ReceiveOrderController(ReceiveOrderMapper receiveMapper, ReceiveOrderItemMapper itemMapper,
                                  SupplyOrderMapper supplyMapper, SupplyOrderItemMapper supplyItemMapper,
                                  ProductSkuMapper skuMapper, ProductSpuMapper spuMapper,
                                  DifferenceOrderMapper differenceMapper, InventoryService inventoryService,
                                  NoGenerator noGenerator) {
        this.receiveMapper = receiveMapper;
        this.itemMapper = itemMapper;
        this.supplyMapper = supplyMapper;
        this.supplyItemMapper = supplyItemMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.differenceMapper = differenceMapper;
        this.inventoryService = inventoryService;
        this.noGenerator = noGenerator;
    }

    @GetMapping
    @RequirePerm("purchase:view")
    public ApiResult<PageResult<ReceiveOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status) {
        LoginUser user = AuthContext.require();
        Long scoped = scopedStoreId(user, storeId);
        Page<ReceiveOrder> result = receiveMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ReceiveOrder>()
                        .eq(scoped != null, ReceiveOrder::getStoreId, scoped)
                        .eq(status != null && !status.isBlank(), ReceiveOrder::getStatus, status)
                        .orderByDesc(ReceiveOrder::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("purchase:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        ReceiveOrder receive = receiveMapper.selectById(id);
        if (receive == null) {
            throw BizException.notFound("收货单不存在");
        }
        checkStoreScope(receive.getStoreId());
        List<ReceiveOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<ReceiveOrderItem>()
                .eq(ReceiveOrderItem::getReceiveId, id));
        List<Long> skuIds = items.stream().map(ReceiveOrderItem::getSkuId).toList();
        Map<Long, ProductSku> skus = skuIds.isEmpty() ? Map.of() : skuMapper.selectList(
                        new LambdaQueryWrapper<ProductSku>().in(ProductSku::getId, skuIds))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        List<Map<String, Object>> rows = items.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("item", item);
            ProductSku sku = skus.get(item.getSkuId());
            row.put("skuCode", sku == null ? "" : sku.getSkuCode());
            row.put("barcode", sku == null ? "" : sku.getBarcode());
            row.put("skuName", skuName(sku));
            return row;
        }).toList();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("order", receive);
        map.put("items", rows);
        return ApiResult.ok(map);
    }

    @PostMapping
    @RequirePerm("purchase:create")
    @Transactional
    public ApiResult<ReceiveOrder> create(@RequestParam Long supplyId) {
        SupplyOrder supply = supplyMapper.selectById(supplyId);
        if (supply == null) {
            throw BizException.notFound("供货单不存在");
        }
        checkStoreScope(supply.getStoreId());
        if ("RECEIVED".equals(supply.getStatus()) || "CANCELLED".equals(supply.getStatus())) {
            throw new BizException("该供货单不可收货");
        }
        ReceiveOrder exists = receiveMapper.selectOne(new LambdaQueryWrapper<ReceiveOrder>()
                .eq(ReceiveOrder::getSupplyId, supplyId)
                .eq(ReceiveOrder::getStatus, "PENDING")
                .last("limit 1"));
        if (exists != null) {
            return ApiResult.ok(exists);
        }
        ReceiveOrder receive = new ReceiveOrder();
        receive.setReceiveNo(noGenerator.next("RO"));
        receive.setSupplyId(supplyId);
        receive.setStoreId(supply.getStoreId());
        receive.setStatus("PENDING");
        receive.setTotalQty(0);
        receive.setDiffQty(0);
        receiveMapper.insert(receive);
        for (SupplyOrderItem supplyItem : supplyItemMapper.selectList(new LambdaQueryWrapper<SupplyOrderItem>()
                .eq(SupplyOrderItem::getSupplyId, supplyId))) {
            ReceiveOrderItem item = new ReceiveOrderItem();
            item.setReceiveId(receive.getId());
            item.setSupplyItemId(supplyItem.getId());
            item.setSkuId(supplyItem.getSkuId());
            item.setQty(supplyItem.getQty());
            item.setActualQty(0);
            item.setDiffQty(0);
            itemMapper.insert(item);
        }
        return ApiResult.ok(receive);
    }

    @PostMapping("/{id}/confirm")
    @RequirePerm("purchase:update")
    @Transactional
    public ApiResult<Void> confirm(@PathVariable Long id, @RequestBody ReceiveConfirmRequest request) {
        ReceiveOrder receive = receiveMapper.selectById(id);
        if (receive == null) {
            throw BizException.notFound("收货单不存在");
        }
        checkStoreScope(receive.getStoreId());
        if (!"PENDING".equals(receive.getStatus())) {
            throw new BizException("收货单已确认");
        }
        List<ReceiveOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<ReceiveOrderItem>()
                .eq(ReceiveOrderItem::getReceiveId, id));
        if (items.isEmpty()) {
            throw new BizException("收货单无明细");
        }
        Map<Long, ReceiveOrderItem> byId = items.stream()
                .collect(Collectors.toMap(ReceiveOrderItem::getId, Function.identity()));
        Map<Long, ReceiveConfirmRequest.Item> requestItems = request.getItems() == null ? Map.of()
                : request.getItems().stream().collect(Collectors.toMap(ReceiveConfirmRequest.Item::getReceiveItemId,
                Function.identity(), (a, b) -> a));
        int totalQty = 0;
        int totalDiff = 0;
        StringBuilder diffContent = new StringBuilder();
        for (ReceiveOrderItem item : items) {
            ReceiveConfirmRequest.Item req = requestItems.get(item.getId());
            int actual = req == null || req.getActualQty() == null ? item.getQty() : req.getActualQty();
            int diff = item.getQty() - actual;
            item.setActualQty(actual);
            item.setDiffQty(diff);
            if (req != null) {
                item.setProductionDate(req.getProductionDate());
                item.setExpiryDate(req.getExpiryDate());
                item.setColdChainTemp(req.getColdChainTemp());
                item.setBatchNo(req.getBatchNo());
            }
            ProductSku sku = skuMapper.selectById(item.getSkuId());
            ProductSpu spu = sku == null ? null : spuMapper.selectById(sku.getSpuId());
            if (spu != null && ("COLD".equals(spu.getStorageCondition()) || "FROZEN".equals(spu.getStorageCondition()))) {
                BigDecimal temp = req != null && req.getColdChainTemp() != null
                        ? req.getColdChainTemp() : request.getColdChainTemp();
                if (temp == null) {
                    throw new BizException("冷藏/冷冻商品必须填写冷链温度：" + spu.getName());
                }
                item.setColdChainTemp(temp);
            }
            if (actual > 0 && item.getExpiryDate() == null && spu != null && spu.getShelfLifeDays() != null) {
                item.setExpiryDate((item.getProductionDate() == null ? java.time.LocalDate.now() : item.getProductionDate())
                        .plusDays(spu.getShelfLifeDays()));
            }
            itemMapper.updateById(item);
            totalQty += actual;
            totalDiff += diff;
            if (actual > 0) {
                inventoryService.inbound("STORE", receive.getStoreId(), item.getSkuId(), actual,
                        item.getBatchNo(), item.getProductionDate(), item.getExpiryDate(),
                        "RECEIVE", "RECEIVE", receive.getId(), "收货入库", AuthContext.require().getUsername());
            }
            SupplyOrderItem supplyItem = item.getSupplyItemId() == null
                    ? null : supplyItemMapper.selectById(item.getSupplyItemId());
            if (supplyItem != null) {
                supplyItem.setReceivedQty((supplyItem.getReceivedQty() == null ? 0 : supplyItem.getReceivedQty()) + actual);
                supplyItemMapper.updateById(supplyItem);
            }
            if (diff != 0) {
                diffContent.append("SKU").append(item.getSkuId()).append(" 差异 ").append(diff).append("；");
            }
        }
        receive.setStatus("CONFIRMED");
        receive.setTotalQty(totalQty);
        receive.setDiffQty(totalDiff);
        receive.setColdChainTemp(request.getColdChainTemp());
        receive.setReceiver(request.getReceiver() == null ? AuthContext.require().getRealName() : request.getReceiver());
        receive.setReceivedAt(LocalDateTime.now());
        receive.setRemark(request.getRemark());
        receiveMapper.updateById(receive);

        if (receive.getSupplyId() != null) {
            SupplyOrder supply = supplyMapper.selectById(receive.getSupplyId());
            if (supply != null) {
                List<SupplyOrderItem> supplyItems = supplyItemMapper.selectList(
                        new LambdaQueryWrapper<SupplyOrderItem>().eq(SupplyOrderItem::getSupplyId, supply.getId()));
                boolean allReceived = supplyItems.stream()
                        .allMatch(s -> s.getReceivedQty() != null && s.getReceivedQty() >= s.getQty());
                supply.setStatus(allReceived ? "RECEIVED" : "PARTIAL");
                supplyMapper.updateById(supply);
            }
        }
        if (totalDiff != 0) {
            DifferenceOrder diffOrder = new DifferenceOrder();
            diffOrder.setDiffNo(noGenerator.next("DO"));
            diffOrder.setReceiveId(receive.getId());
            diffOrder.setSupplyId(receive.getSupplyId());
            diffOrder.setStoreId(receive.getStoreId());
            diffOrder.setType("QTY");
            diffOrder.setContent(diffContent.toString());
            diffOrder.setStatus("PENDING");
            differenceMapper.insert(diffOrder);
        }
        return ApiResult.ok();
    }

    private String skuName(ProductSku sku) {
        if (sku == null) {
            return "-";
        }
        ProductSpu spu = spuMapper.selectById(sku.getSpuId());
        return spu == null ? sku.getSkuCode() : spu.getName();
    }

    private Long scopedStoreId(LoginUser user, Long requested) {
        if (("STORE_MANAGER".equals(user.getRoleCode()) || "CASHIER".equals(user.getRoleCode()))
                && user.getStoreId() != null) {
            return user.getStoreId();
        }
        return requested;
    }

    private void checkStoreScope(Long storeId) {
        LoginUser user = AuthContext.require();
        if (("STORE_MANAGER".equals(user.getRoleCode()) || "CASHIER".equals(user.getRoleCode()))
                && user.getStoreId() != null && !user.getStoreId().equals(storeId)) {
            throw BizException.forbidden("仅可操作本门店数据");
        }
    }
}
