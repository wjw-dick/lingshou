package com.retail.convenience.modules.supplier.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.supplier.entity.Supplier;
import com.retail.convenience.modules.supplier.entity.SupplierProduct;
import com.retail.convenience.modules.supplier.entity.SupplierQualification;
import com.retail.convenience.modules.supplier.mapper.SupplierMapper;
import com.retail.convenience.modules.supplier.mapper.SupplierProductMapper;
import com.retail.convenience.modules.supplier.mapper.SupplierQualificationMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RequestMapping("/api/v1/tenant/suppliers")
public class SupplierController {

    private final SupplierMapper supplierMapper;
    private final SupplierQualificationMapper qualificationMapper;
    private final SupplierProductMapper supplierProductMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;

    public SupplierController(SupplierMapper supplierMapper, SupplierQualificationMapper qualificationMapper,
                              SupplierProductMapper supplierProductMapper, ProductSkuMapper skuMapper,
                              ProductSpuMapper spuMapper) {
        this.supplierMapper = supplierMapper;
        this.qualificationMapper = qualificationMapper;
        this.supplierProductMapper = supplierProductMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
    }

    @GetMapping
    @RequirePerm("supplier:view")
    public ApiResult<PageResult<Supplier>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<Supplier> result = supplierMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Supplier>()
                        .eq(Supplier::getDeleted, 0)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Supplier::getName, keyword).or().like(Supplier::getCode, keyword))
                        .eq(status != null && !status.isBlank(), Supplier::getStatus, status)
                        .orderByAsc(Supplier::getCode));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/options")
    public ApiResult<List<Map<String, Object>>> options() {
        return ApiResult.ok(supplierMapper.selectList(new LambdaQueryWrapper<Supplier>()
                        .eq(Supplier::getDeleted, 0).eq(Supplier::getStatus, "ACTIVE"))
                .stream().map(s -> Map.<String, Object>of("id", s.getId(), "code", s.getCode(), "name", s.getName()))
                .toList());
    }

    @GetMapping("/{id}")
    @RequirePerm("supplier:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Supplier supplier = require(id);
        List<SupplierQualification> qualifications = qualificationMapper.selectList(
                new LambdaQueryWrapper<SupplierQualification>().eq(SupplierQualification::getSupplierId, id));
        List<SupplierProduct> products = supplierProductMapper.selectList(
                new LambdaQueryWrapper<SupplierProduct>().eq(SupplierProduct::getSupplierId, id));
        List<Long> skuIds = products.stream().map(SupplierProduct::getSkuId).toList();
        Map<Long, ProductSku> skus = skuIds.isEmpty() ? Map.of() : skuMapper.selectList(
                        new LambdaQueryWrapper<ProductSku>().in(ProductSku::getId, skuIds))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("supplier", supplier);
        map.put("qualifications", qualifications);
        map.put("products", products.stream().map(sp -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product", sp);
            ProductSku sku = skus.get(sp.getSkuId());
            ProductSpu spu = sku == null ? null : spuMapper.selectById(sku.getSpuId());
            row.put("skuCode", sku == null ? "" : sku.getSkuCode());
            row.put("skuName", spu == null ? "" : spu.getName());
            row.put("barcode", sku == null ? "" : sku.getBarcode());
            return row;
        }).toList());
        return ApiResult.ok(map);
    }

    @PostMapping
    @RequirePerm("supplier:create")
    public ApiResult<Supplier> create(@RequestBody Supplier supplier) {
        supplier.setId(null);
        supplier.setTenantId(AuthContext.require().getTenantId());
        supplier.setDeleted(0);
        supplierMapper.insert(supplier);
        return ApiResult.ok(supplier);
    }

    @PutMapping("/{id}")
    @RequirePerm("supplier:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        supplierMapper.updateById(supplier);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("supplier:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        supplierMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/qualifications")
    @RequirePerm("supplier:update")
    public ApiResult<Void> addQualification(@PathVariable Long id,
                                            @RequestBody SupplierQualification qualification) {
        require(id);
        qualification.setId(null);
        qualification.setSupplierId(id);
        qualificationMapper.insert(qualification);
        return ApiResult.ok();
    }

    @DeleteMapping("/qualifications/{id}")
    @RequirePerm("supplier:update")
    public ApiResult<Void> deleteQualification(@PathVariable Long id) {
        qualificationMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/products/{id}/audit")
    @RequirePerm("supplier:update")
    public ApiResult<Void> auditProduct(@PathVariable Long id, @RequestParam String status) {
        SupplierProduct product = supplierProductMapper.selectById(id);
        if (product == null) {
            throw BizException.notFound("供货商品不存在");
        }
        product.setAuditStatus(status);
        supplierProductMapper.updateById(product);
        return ApiResult.ok();
    }

    private Supplier require(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw BizException.notFound("供应商不存在");
        }
        return supplier;
    }
}
