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
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.purchase.dto.SupplySaveRequest;
import com.retail.convenience.modules.purchase.entity.SupplyOrder;
import com.retail.convenience.modules.purchase.entity.SupplyOrderItem;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderItemMapper;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderMapper;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.supplier.entity.Supplier;
import com.retail.convenience.modules.supplier.mapper.SupplierMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/supply-orders")
public class SupplyOrderController {

    private final SupplyOrderMapper supplyMapper;
    private final SupplyOrderItemMapper itemMapper;
    private final ProductSkuMapper skuMapper;
    private final StoreMapper storeMapper;
    private final SupplierMapper supplierMapper;
    private final NoGenerator noGenerator;

    public SupplyOrderController(SupplyOrderMapper supplyMapper, SupplyOrderItemMapper itemMapper,
                                 ProductSkuMapper skuMapper, StoreMapper storeMapper,
                                 SupplierMapper supplierMapper, NoGenerator noGenerator) {
        this.supplyMapper = supplyMapper;
        this.itemMapper = itemMapper;
        this.skuMapper = skuMapper;
        this.storeMapper = storeMapper;
        this.supplierMapper = supplierMapper;
        this.noGenerator = noGenerator;
    }

    @GetMapping
    @RequirePerm("purchase:view")
    public ApiResult<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        LoginUser user = AuthContext.require();
        Long scoped = scopedStoreId(user, storeId);
        Page<SupplyOrder> result = supplyMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SupplyOrder>()
                        .eq(scoped != null, SupplyOrder::getStoreId, scoped)
                        .eq(supplierId != null, SupplyOrder::getSupplierId, supplierId)
                        .eq(status != null && !status.isBlank(), SupplyOrder::getStatus, status)
                        .like(keyword != null && !keyword.isBlank(), SupplyOrder::getOrderNo, keyword)
                        .orderByDesc(SupplyOrder::getCreatedAt));
        Map<Long, Store> stores = storeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        Map<Long, Supplier> suppliers = supplierMapper.selectList(null).stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SupplyOrder order : result.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", order);
            Store store = stores.get(order.getStoreId());
            row.put("storeName", store == null ? "-" : store.getName());
            Supplier supplier = order.getSupplierId() == null ? null : suppliers.get(order.getSupplierId());
            row.put("supplierName", supplier == null ? "总部仓" : supplier.getName());
            rows.add(row);
        }
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @GetMapping("/{id}")
    @RequirePerm("purchase:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        SupplyOrder order = supplyMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("供货单不存在");
        }
        checkStoreScope(order.getStoreId());
        List<SupplyOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SupplyOrderItem>()
                .eq(SupplyOrderItem::getSupplyId, id));
        Map<Long, ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .in(ProductSku::getId, items.stream().map(SupplyOrderItem::getSkuId).toList()))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        List<Map<String, Object>> itemRows = items.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("item", item);
            ProductSku sku = skus.get(item.getSkuId());
            row.put("skuName", sku == null ? "-" : sku.getSkuCode());
            row.put("barcode", sku == null ? "" : sku.getBarcode());
            return row;
        }).toList();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("order", order);
        map.put("items", itemRows);
        return ApiResult.ok(map);
    }

    @PostMapping
    @RequirePerm("purchase:create")
    @Transactional
    public ApiResult<SupplyOrder> create(@RequestBody SupplySaveRequest request) {
        if (request.getStoreId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请选择门店并添加供货商品");
        }
        LoginUser user = AuthContext.require();
        checkStoreScope(request.getStoreId());
        SupplyOrder order = new SupplyOrder();
        order.setOrderNo(noGenerator.next("SO"));
        order.setSourceType(request.getSourceType() == null ? "HQ" : request.getSourceType());
        order.setSupplierId(request.getSupplierId());
        order.setStoreId(request.getStoreId());
        order.setFranchiseeId(request.getFranchiseeId());
        order.setStatus("PENDING");
        order.setExpectedAt(request.getExpectedAt());
        order.setRemark(request.getRemark());
        order.setTotalAmount(BigDecimal.ZERO);
        supplyMapper.insert(order);

        BigDecimal total = BigDecimal.ZERO;
        for (SupplySaveRequest.Item requestItem : request.getItems()) {
            if (requestItem.getQty() == null || requestItem.getQty() <= 0) {
                continue;
            }
            ProductSku sku = skuMapper.selectById(requestItem.getSkuId());
            if (sku == null) {
                throw BizException.notFound("商品不存在：" + requestItem.getSkuId());
            }
            BigDecimal price = requestItem.getPrice() == null ? sku.getPriceFranchise() : requestItem.getPrice();
            SupplyOrderItem item = new SupplyOrderItem();
            item.setSupplyId(order.getId());
            item.setSkuId(sku.getId());
            item.setQty(requestItem.getQty());
            item.setShippedQty(0);
            item.setReceivedQty(0);
            item.setPrice(price);
            item.setAmount(price.multiply(BigDecimal.valueOf(requestItem.getQty())));
            itemMapper.insert(item);
            total = total.add(item.getAmount());
        }
        order.setTotalAmount(total);
        supplyMapper.updateById(order);
        return ApiResult.ok(order);
    }

    @PostMapping("/{id}/accept")
    @RequirePerm("purchase:update")
    public ApiResult<Void> accept(@PathVariable Long id) {
        SupplyOrder order = require(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException("当前状态不可接单");
        }
        order.setStatus("ACCEPTED");
        supplyMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/ship")
    @RequirePerm("purchase:update")
    public ApiResult<Void> ship(@PathVariable Long id, @RequestParam(required = false) String logisticsNo) {
        SupplyOrder order = require(id);
        if (!"ACCEPTED".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
            throw new BizException("当前状态不可发货");
        }
        order.setStatus("SHIPPED");
        order.setLogisticsNo(logisticsNo);
        supplyMapper.updateById(order);
        List<SupplyOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SupplyOrderItem>()
                .eq(SupplyOrderItem::getSupplyId, id));
        for (SupplyOrderItem item : items) {
            item.setShippedQty(item.getQty());
            itemMapper.updateById(item);
        }
        return ApiResult.ok();
    }

    @PostMapping("/{id}/cancel")
    @RequirePerm("purchase:update")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        SupplyOrder order = require(id);
        if ("RECEIVED".equals(order.getStatus())) {
            throw new BizException("已收货的供货单不可取消");
        }
        order.setStatus("CANCELLED");
        supplyMapper.updateById(order);
        return ApiResult.ok();
    }

    private SupplyOrder require(Long id) {
        SupplyOrder order = supplyMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("供货单不存在");
        }
        checkStoreScope(order.getStoreId());
        return order;
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
