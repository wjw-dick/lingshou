package com.retail.convenience.modules.supplier.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.purchase.entity.SupplyOrder;
import com.retail.convenience.modules.purchase.entity.SupplyOrderItem;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderItemMapper;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderMapper;
import com.retail.convenience.modules.settlement.entity.SettlementInvoice;
import com.retail.convenience.modules.settlement.entity.SettlementItem;
import com.retail.convenience.modules.settlement.entity.SettlementStatement;
import com.retail.convenience.modules.settlement.mapper.SettlementInvoiceMapper;
import com.retail.convenience.modules.settlement.mapper.SettlementItemMapper;
import com.retail.convenience.modules.settlement.mapper.SettlementStatementMapper;
import com.retail.convenience.modules.supplier.entity.Supplier;
import com.retail.convenience.modules.supplier.entity.SupplierProduct;
import com.retail.convenience.modules.supplier.entity.SupplierQualification;
import com.retail.convenience.modules.supplier.mapper.SupplierMapper;
import com.retail.convenience.modules.supplier.mapper.SupplierProductMapper;
import com.retail.convenience.modules.supplier.mapper.SupplierQualificationMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/supplier")
public class SupplierPortalController {

    private final SupplierMapper supplierMapper;
    private final SupplierQualificationMapper qualificationMapper;
    private final SupplierProductMapper supplierProductMapper;
    private final SupplyOrderMapper supplyMapper;
    private final SupplyOrderItemMapper supplyItemMapper;
    private final SettlementStatementMapper statementMapper;
    private final SettlementItemMapper statementItemMapper;
    private final SettlementInvoiceMapper invoiceMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;

    public SupplierPortalController(SupplierMapper supplierMapper,
                                    SupplierQualificationMapper qualificationMapper,
                                    SupplierProductMapper supplierProductMapper,
                                    SupplyOrderMapper supplyMapper, SupplyOrderItemMapper supplyItemMapper,
                                    SettlementStatementMapper statementMapper,
                                    SettlementItemMapper statementItemMapper,
                                    SettlementInvoiceMapper invoiceMapper, ProductSkuMapper skuMapper,
                                    ProductSpuMapper spuMapper) {
        this.supplierMapper = supplierMapper;
        this.qualificationMapper = qualificationMapper;
        this.supplierProductMapper = supplierProductMapper;
        this.supplyMapper = supplyMapper;
        this.supplyItemMapper = supplyItemMapper;
        this.statementMapper = statementMapper;
        this.statementItemMapper = statementItemMapper;
        this.invoiceMapper = invoiceMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
    }

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile() {
        Supplier supplier = currentSupplier();
        return ApiResult.ok(Map.of("supplier", supplier,
                "qualifications", qualificationMapper.selectList(new LambdaQueryWrapper<SupplierQualification>()
                        .eq(SupplierQualification::getSupplierId, supplier.getId()))));
    }

    @GetMapping("/supply-orders")
    public ApiResult<PageResult<Map<String, Object>>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Supplier supplier = currentSupplier();
        Page<SupplyOrder> result = supplyMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SupplyOrder>()
                        .eq(SupplyOrder::getSupplierId, supplier.getId())
                        .eq(status != null && !status.isBlank(), SupplyOrder::getStatus, status)
                        .orderByDesc(SupplyOrder::getCreatedAt));
        Map<Long, List<SupplyOrderItem>> items = supplyItemMapper.selectList(
                        new LambdaQueryWrapper<SupplyOrderItem>().in(SupplyOrderItem::getSupplyId,
                                result.getRecords().stream().map(SupplyOrder::getId).toList()))
                .stream().collect(Collectors.groupingBy(SupplyOrderItem::getSupplyId));
        List<Map<String, Object>> rows = result.getRecords().stream().map(order -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", order);
            row.put("items", items.getOrDefault(order.getId(), List.of()));
            return row;
        }).toList();
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @PostMapping("/supply-orders/{id}/accept")
    public ApiResult<Void> accept(@PathVariable Long id) {
        SupplyOrder order = ownedOrder(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException("当前状态不可接单");
        }
        order.setStatus("ACCEPTED");
        supplyMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/supply-orders/{id}/reject")
    public ApiResult<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        SupplyOrder order = ownedOrder(id);
        order.setStatus("CANCELLED");
        order.setRemark("供应商拒单：" + reason);
        supplyMapper.updateById(order);
        return ApiResult.ok();
    }

    @PostMapping("/supply-orders/{id}/ship")
    public ApiResult<Void> ship(@PathVariable Long id, @RequestParam(required = false) String logisticsNo) {
        SupplyOrder order = ownedOrder(id);
        if (!"ACCEPTED".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
            throw new BizException("当前状态不可发货");
        }
        order.setStatus("SHIPPED");
        order.setLogisticsNo(logisticsNo);
        supplyMapper.updateById(order);
        for (SupplyOrderItem item : supplyItemMapper.selectList(new LambdaQueryWrapper<SupplyOrderItem>()
                .eq(SupplyOrderItem::getSupplyId, id))) {
            item.setShippedQty(item.getQty());
            supplyItemMapper.updateById(item);
        }
        return ApiResult.ok();
    }

    @GetMapping("/statements")
    public ApiResult<PageResult<SettlementStatement>> statements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Supplier supplier = currentSupplier();
        Page<SettlementStatement> result = statementMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SettlementStatement>()
                        .eq(SettlementStatement::getType, "SUPPLIER")
                        .eq(SettlementStatement::getPartnerId, supplier.getId())
                        .eq(status != null && !status.isBlank(), SettlementStatement::getStatus, status)
                        .orderByDesc(SettlementStatement::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/statements/{id}")
    public ApiResult<Map<String, Object>> statementDetail(@PathVariable Long id) {
        SettlementStatement statement = ownedStatement(id);
        return ApiResult.ok(Map.of("statement", statement,
                "items", statementItemMapper.selectList(new LambdaQueryWrapper<SettlementItem>()
                        .eq(SettlementItem::getStatementId, id)),
                "invoices", invoiceMapper.selectList(new LambdaQueryWrapper<SettlementInvoice>()
                        .eq(SettlementInvoice::getStatementId, id))));
    }

    @PostMapping("/statements/{id}/confirm")
    public ApiResult<Void> confirmStatement(@PathVariable Long id) {
        SettlementStatement statement = ownedStatement(id);
        statement.setStatus("CONFIRMED");
        statement.setConfirmedAt(java.time.LocalDateTime.now());
        statementMapper.updateById(statement);
        return ApiResult.ok();
    }

    @PostMapping("/statements/{id}/dispute")
    public ApiResult<Void> disputeStatement(@PathVariable Long id, @RequestParam String reason) {
        SettlementStatement statement = ownedStatement(id);
        statement.setStatus("DISPUTED");
        statement.setRemark(reason);
        statementMapper.updateById(statement);
        return ApiResult.ok();
    }

    @GetMapping("/products")
    public ApiResult<List<Map<String, Object>>> products() {
        Supplier supplier = currentSupplier();
        List<SupplierProduct> products = supplierProductMapper.selectList(
                new LambdaQueryWrapper<SupplierProduct>()
                        .eq(SupplierProduct::getSupplierId, supplier.getId()));
        List<Long> skuIds = products.stream().map(SupplierProduct::getSkuId).toList();
        Map<Long, ProductSku> skus = skuIds.isEmpty() ? Map.of() : skuMapper.selectList(
                        new LambdaQueryWrapper<ProductSku>().in(ProductSku::getId, skuIds))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        return ApiResult.ok(products.stream().map(sp -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product", sp);
            ProductSku sku = skus.get(sp.getSkuId());
            ProductSpu spu = sku == null ? null : spuMapper.selectById(sku.getSpuId());
            row.put("skuCode", sku == null ? "" : sku.getSkuCode());
            row.put("skuName", spu == null ? "" : spu.getName());
            row.put("barcode", sku == null ? "" : sku.getBarcode());
            return row;
        }).toList());
    }

    @PostMapping("/products")
    public ApiResult<Void> saveProduct(@RequestBody SupplierProduct request) {
        Supplier supplier = currentSupplier();
        SupplierProduct exists = supplierProductMapper.selectOne(new LambdaQueryWrapper<SupplierProduct>()
                .eq(SupplierProduct::getSupplierId, supplier.getId())
                .eq(SupplierProduct::getSkuId, request.getSkuId()).last("limit 1"));
        if (exists == null) {
            request.setId(null);
            request.setSupplierId(supplier.getId());
            request.setAuditStatus("PENDING");
            request.setStatus("ACTIVE");
            supplierProductMapper.insert(request);
        } else {
            exists.setSupplyPrice(request.getSupplyPrice());
            exists.setAuditStatus("PENDING");
            supplierProductMapper.updateById(exists);
        }
        return ApiResult.ok();
    }

    @PostMapping("/qualifications")
    public ApiResult<Void> addQualification(@RequestBody SupplierQualification qualification) {
        Supplier supplier = currentSupplier();
        qualification.setId(null);
        qualification.setSupplierId(supplier.getId());
        qualificationMapper.insert(qualification);
        return ApiResult.ok();
    }

    @GetMapping("/messages")
    public ApiResult<Map<String, Object>> messages() {
        Supplier supplier = currentSupplier();
        long pendingOrders = supplyMapper.selectCount(new LambdaQueryWrapper<SupplyOrder>()
                .eq(SupplyOrder::getSupplierId, supplier.getId())
                .eq(SupplyOrder::getStatus, "PENDING"));
        long pendingStatements = statementMapper.selectCount(new LambdaQueryWrapper<SettlementStatement>()
                .eq(SettlementStatement::getType, "SUPPLIER")
                .eq(SettlementStatement::getPartnerId, supplier.getId())
                .eq(SettlementStatement::getStatus, "PENDING"));
        return ApiResult.ok(Map.of("pendingOrders", pendingOrders, "pendingStatements", pendingStatements));
    }

    private Supplier currentSupplier() {
        LoginUser user = AuthContext.require();
        if (user.getSupplierId() == null) {
            throw BizException.forbidden("当前账号未绑定供应商");
        }
        Supplier supplier = supplierMapper.selectById(user.getSupplierId());
        if (supplier == null) {
            throw BizException.notFound("供应商不存在");
        }
        return supplier;
    }

    private SupplyOrder ownedOrder(Long id) {
        SupplyOrder order = supplyMapper.selectById(id);
        if (order == null || !currentSupplier().getId().equals(order.getSupplierId())) {
            throw BizException.notFound("供货单不存在");
        }
        return order;
    }

    private SettlementStatement ownedStatement(Long id) {
        SettlementStatement statement = statementMapper.selectById(id);
        if (statement == null || !currentSupplier().getId().equals(statement.getPartnerId())
                || !"SUPPLIER".equals(statement.getType())) {
            throw BizException.notFound("对账单不存在");
        }
        return statement;
    }
}
