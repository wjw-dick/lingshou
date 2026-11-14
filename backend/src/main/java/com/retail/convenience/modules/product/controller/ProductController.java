package com.retail.convenience.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.ExcelUtil;
import com.retail.convenience.modules.product.entity.Brand;
import com.retail.convenience.modules.product.entity.ProductBarcode;
import com.retail.convenience.modules.product.entity.ProductCategory;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.BrandMapper;
import com.retail.convenience.modules.product.mapper.ProductBarcodeMapper;
import com.retail.convenience.modules.product.mapper.ProductCategoryMapper;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/products")
public class ProductController {

    private final ProductCategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final ProductSpuMapper spuMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductBarcodeMapper barcodeMapper;

    public ProductController(ProductCategoryMapper categoryMapper, BrandMapper brandMapper,
                             ProductSpuMapper spuMapper, ProductSkuMapper skuMapper,
                             ProductBarcodeMapper barcodeMapper) {
        this.categoryMapper = categoryMapper;
        this.brandMapper = brandMapper;
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
        this.barcodeMapper = barcodeMapper;
    }

    @GetMapping("/categories")
    @RequirePerm("product:view")
    public ApiResult<List<ProductCategory>> categories() {
        return ApiResult.ok(categoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getDeleted, 0)
                .orderByAsc(ProductCategory::getSortNo)));
    }

    @PostMapping("/categories")
    @RequirePerm("product:create")
    public ApiResult<Void> createCategory(@RequestBody ProductCategory category) {
        category.setId(null);
        category.setTenantId(AuthContext.require().getTenantId());
        category.setDeleted(0);
        categoryMapper.insert(category);
        return ApiResult.ok();
    }

    @PutMapping("/categories/{id}")
    @RequirePerm("product:update")
    public ApiResult<Void> updateCategory(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return ApiResult.ok();
    }

    @DeleteMapping("/categories/{id}")
    @RequirePerm("product:delete")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        if (categoryMapper.selectCount(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, id).eq(ProductCategory::getDeleted, 0)) > 0) {
            throw new BizException("存在子分类，不能删除");
        }
        if (spuMapper.selectCount(new LambdaQueryWrapper<ProductSpu>()
                .eq(ProductSpu::getCategoryId, id).eq(ProductSpu::getDeleted, 0)) > 0) {
            throw new BizException("分类下存在商品，不能删除");
        }
        categoryMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/brands")
    @RequirePerm("product:view")
    public ApiResult<List<Brand>> brands() {
        return ApiResult.ok(brandMapper.selectList(new LambdaQueryWrapper<Brand>()
                .orderByAsc(Brand::getSortNo)));
    }

    @PostMapping("/brands")
    @RequirePerm("product:create")
    public ApiResult<Void> createBrand(@RequestBody Brand brand) {
        brand.setId(null);
        brand.setTenantId(AuthContext.require().getTenantId());
        brandMapper.insert(brand);
        return ApiResult.ok();
    }

    @PutMapping("/brands/{id}")
    @RequirePerm("product:update")
    public ApiResult<Void> updateBrand(@PathVariable Long id, @RequestBody Brand brand) {
        brand.setId(id);
        brandMapper.updateById(brand);
        return ApiResult.ok();
    }

    @DeleteMapping("/brands/{id}")
    @RequirePerm("product:delete")
    public ApiResult<Void> deleteBrand(@PathVariable Long id) {
        brandMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/spus")
    @RequirePerm("product:view")
    public ApiResult<PageResult<Map<String, Object>>> spus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {
        Page<ProductSpu> result = spuMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getDeleted, 0)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(ProductSpu::getName, keyword)
                                .or().like(ProductSpu::getSpuCode, keyword))
                        .eq(categoryId != null, ProductSpu::getCategoryId, categoryId)
                        .eq(status != null && !status.isBlank(), ProductSpu::getStatus, status)
                        .orderByDesc(ProductSpu::getCreatedAt));
        Map<Long, ProductCategory> categories = new HashMap<>();
        categoryMapper.selectList(null).forEach(c -> categories.put(c.getId(), c));
        Map<Long, Brand> brands = new HashMap<>();
        brandMapper.selectList(null).forEach(b -> brands.put(b.getId(), b));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductSpu spu : result.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("spu", spu);
            ProductCategory category = categories.get(spu.getCategoryId());
            row.put("categoryName", category == null ? "-" : category.getName());
            Brand brand = spu.getBrandId() == null ? null : brands.get(spu.getBrandId());
            row.put("brandName", brand == null ? "-" : brand.getName());
            row.put("skuCount", skuMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getSpuId, spu.getId()).eq(ProductSku::getDeleted, 0)));
            rows.add(row);
        }
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @GetMapping("/spus/{id}")
    @RequirePerm("product:view")
    public ApiResult<Map<String, Object>> spuDetail(@PathVariable Long id) {
        ProductSpu spu = spuMapper.selectById(id);
        if (spu == null) {
            throw BizException.notFound("商品不存在");
        }
        List<ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getSpuId, id).eq(ProductSku::getDeleted, 0));
        List<ProductBarcode> barcodes = barcodeMapper.selectList(new LambdaQueryWrapper<ProductBarcode>()
                .in(ProductBarcode::getSkuId, skus.stream().map(ProductSku::getId).toList()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("spu", spu);
        map.put("skus", skus);
        map.put("barcodes", barcodes);
        return ApiResult.ok(map);
    }

    @PostMapping("/spus")
    @RequirePerm("product:create")
    @Transactional
    public ApiResult<ProductSpu> createSpu(@RequestBody SpuSaveRequest request) {
        ProductSpu spu = request.getSpu();
        spu.setId(null);
        spu.setTenantId(AuthContext.require().getTenantId());
        spu.setDeleted(0);
        if (spu.getStatus() == null) {
            spu.setStatus("ON_SALE");
        }
        validateSpu(spu);
        spuMapper.insert(spu);
        saveSkus(spu, request.getSkus());
        return ApiResult.ok(spu);
    }

    @PutMapping("/spus/{id}")
    @RequirePerm("product:update")
    @Transactional
    public ApiResult<Void> updateSpu(@PathVariable Long id, @RequestBody SpuSaveRequest request) {
        ProductSpu spu = request.getSpu();
        spu.setId(id);
        spu.setTenantId(AuthContext.require().getTenantId());
        validateSpu(spu);
        spuMapper.updateById(spu);
        if (request.getSkus() != null) {
            List<ProductSku> existing = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getSpuId, id));
            Map<Long, ProductSku> existingById = new HashMap<>();
            existing.forEach(item -> existingById.put(item.getId(), item));
            List<Long> keptIds = new ArrayList<>();
            for (ProductSku sku : request.getSkus()) {
                if (sku.getId() != null && existingById.containsKey(sku.getId())) {
                    ProductSku current = existingById.get(sku.getId());
                    current.setSkuCode(sku.getSkuCode());
                    current.setBarcode(sku.getBarcode());
                    current.setSpec(sku.getSpec());
                    current.setUnit(sku.getUnit());
                    current.setIsWeight(sku.getIsWeight());
                    current.setCostPrice(sku.getCostPrice());
                    current.setPriceGuide(sku.getPriceGuide());
                    current.setPriceRetail(sku.getPriceRetail());
                    current.setPriceMember(sku.getPriceMember());
                    current.setPriceFranchise(sku.getPriceFranchise());
                    current.setStatus(sku.getStatus());
                    skuMapper.updateById(current);
                    keptIds.add(current.getId());
                    upsertBarcode(spu.getTenantId(), current.getId(), current.getBarcode());
                } else {
                    sku.setId(null);
                    sku.setTenantId(spu.getTenantId());
                    sku.setSpuId(id);
                    sku.setStatus(sku.getStatus() == null ? "ON_SALE" : sku.getStatus());
                    sku.setDeleted(0);
                    skuMapper.insert(sku);
                    keptIds.add(sku.getId());
                    upsertBarcode(spu.getTenantId(), sku.getId(), sku.getBarcode());
                }
            }
            for (ProductSku old : existing) {
                if (!keptIds.contains(old.getId())) {
                    skuMapper.deleteById(old.getId());
                    barcodeMapper.delete(new LambdaQueryWrapper<ProductBarcode>()
                            .eq(ProductBarcode::getSkuId, old.getId()));
                }
            }
        }
        return ApiResult.ok();
    }

    private void upsertBarcode(Long tenantId, Long skuId, String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return;
        }
        ProductBarcode exists = barcodeMapper.selectOne(new LambdaQueryWrapper<ProductBarcode>()
                .eq(ProductBarcode::getTenantId, tenantId)
                .eq(ProductBarcode::getBarcode, barcode)
                .last("limit 1"));
        if (exists == null) {
            ProductBarcode row = new ProductBarcode();
            row.setTenantId(tenantId);
            row.setSkuId(skuId);
            row.setBarcode(barcode);
            barcodeMapper.insert(row);
        } else if (!skuId.equals(exists.getSkuId())) {
            throw new BizException("条码已被其他 SKU 使用：" + barcode);
        }
    }

    @DeleteMapping("/spus/{id}")
    @RequirePerm("product:delete")
    @Transactional
    public ApiResult<Void> deleteSpu(@PathVariable Long id) {
        spuMapper.deleteById(id);
        skuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getSpuId, id));
        return ApiResult.ok();
    }

    @PostMapping("/spus/{id}/status")
    @RequirePerm("product:update")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        ProductSpu spu = new ProductSpu();
        spu.setId(id);
        spu.setStatus(status);
        spuMapper.updateById(spu);
        return ApiResult.ok();
    }

    @GetMapping("/skus/barcode/{barcode}")
    @RequirePerm("product:view")
    public ApiResult<Map<String, Object>> byBarcode(@PathVariable String barcode) {
        ProductSku sku = skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getBarcode, barcode)
                .eq(ProductSku::getDeleted, 0).last("limit 1"));
        if (sku == null) {
            ProductBarcode extra = barcodeMapper.selectOne(new LambdaQueryWrapper<ProductBarcode>()
                    .eq(ProductBarcode::getBarcode, barcode).last("limit 1"));
            if (extra != null) {
                sku = skuMapper.selectById(extra.getSkuId());
            }
        }
        if (sku == null) {
            throw BizException.notFound("未找到该条码商品");
        }
        ProductSpu spu = spuMapper.selectById(sku.getSpuId());
        return ApiResult.ok(Map.of("sku", sku, "spu", spu));
    }

    @PostMapping("/spus/import")
    @RequirePerm("product:import")
    @Transactional
    public ApiResult<Map<String, Object>> importExcel(@RequestPart("file") MultipartFile file) {
        int success = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                try {
                    String spuCode = text(row.getCell(0));
                    String name = text(row.getCell(1));
                    if (spuCode.isBlank() || name.isBlank()) {
                        continue;
                    }
                    ProductSpu spu = new ProductSpu();
                    spu.setSpuCode(spuCode);
                    spu.setName(name);
                    spu.setCategoryId((long) number(row.getCell(2), 1));
                    spu.setBrandId(null);
                    spu.setUnit(text(row.getCell(3), "件"));
                    spu.setSpec(text(row.getCell(4)));
                    spu.setShelfLifeDays((int) number(row.getCell(5), 365));
                    spu.setStorageCondition(text(row.getCell(6), "NORMAL"));
                    spu.setPriceRetail(BigDecimal.valueOf(number(row.getCell(7), 0)));
                    spu.setPriceMember(BigDecimal.valueOf(number(row.getCell(8), 0)));
                    spu.setPriceFranchise(BigDecimal.valueOf(number(row.getCell(9), 0)));
                    spu.setOnlineSale(1);
                    spu.setIsTobacco(0);
                    spu.setStatus("ON_SALE");
                    spu.setTenantId(AuthContext.require().getTenantId());
                    spu.setDeleted(0);
                    if (spuMapper.selectCount(new LambdaQueryWrapper<ProductSpu>()
                            .eq(ProductSpu::getSpuCode, spuCode).eq(ProductSpu::getDeleted, 0)) > 0) {
                        errors.add("第 " + (i + 1) + " 行：商品编码重复 " + spuCode);
                        continue;
                    }
                    spuMapper.insert(spu);
                    ProductSku sku = new ProductSku();
                    sku.setTenantId(AuthContext.require().getTenantId());
                    sku.setSpuId(spu.getId());
                    sku.setSkuCode(spuCode + "-1");
                    sku.setBarcode(text(row.getCell(10)));
                    sku.setSpec(spu.getSpec());
                    sku.setUnit(spu.getUnit());
                    sku.setCostPrice(BigDecimal.valueOf(number(row.getCell(11), 0)));
                    sku.setPriceRetail(spu.getPriceRetail());
                    sku.setPriceMember(spu.getPriceMember());
                    sku.setPriceFranchise(spu.getPriceFranchise());
                    sku.setStatus("ON_SALE");
                    sku.setDeleted(0);
                    skuMapper.insert(sku);
                    success++;
                } catch (Exception e) {
                    errors.add("第 " + (i + 1) + " 行：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BizException("Excel 解析失败：" + e.getMessage());
        }
        return ApiResult.ok(Map.of("success", success, "errors", errors));
    }

    @GetMapping("/spus/export")
    @RequirePerm("product:view")
    public ResponseEntity<byte[]> exportExcel() {
        List<ProductSpu> spus = spuMapper.selectList(new LambdaQueryWrapper<ProductSpu>()
                .eq(ProductSpu::getDeleted, 0));
        List<ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getDeleted, 0));
        Map<Long, ProductSku> skuBySpu = new HashMap<>();
        skus.forEach(s -> skuBySpu.put(s.getSpuId(), s));
        byte[] bytes = ExcelUtil.write("商品档案",
                List.of("商品编码", "商品名称", "分类ID", "单位", "规格", "保质期天", "贮存条件", "零售价", "会员价", "加盟价", "条码", "成本价"),
                spus.stream().map(spu -> {
                    ProductSku sku = skuBySpu.get(spu.getId());
                    return List.<Object>of(spu.getSpuCode(), spu.getName(), spu.getCategoryId(), spu.getUnit(),
                            nvl(spu.getSpec()), spu.getShelfLifeDays(), spu.getStorageCondition(),
                            spu.getPriceRetail(), spu.getPriceMember(), spu.getPriceFranchise(),
                            sku == null ? "" : nvl(sku.getBarcode()), sku == null ? "" : nvl(sku.getCostPrice()));
                }).toList());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("商品档案.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private void validateSpu(ProductSpu spu) {
        if (spu.getName() == null || spu.getName().isBlank()) {
            throw new BizException("商品名称不能为空");
        }
        if (spu.getCategoryId() == null) {
            throw new BizException("商品分类不能为空");
        }
        if (spu.getShelfLifeDays() == null || spu.getShelfLifeDays() <= 0) {
            throw new BizException("请填写保质期天数");
        }
        if (Integer.valueOf(1).equals(spu.getIsTobacco())) {
            spu.setOnlineSale(0);
        }
    }

    private void saveSkus(ProductSpu spu, List<ProductSku> skus) {
        if (skus == null || skus.isEmpty()) {
            ProductSku sku = new ProductSku();
            sku.setTenantId(spu.getTenantId());
            sku.setSpuId(spu.getId());
            sku.setSkuCode(spu.getSpuCode() + "-1");
            sku.setSpec(spu.getSpec());
            sku.setUnit(spu.getUnit());
            sku.setIsWeight(spu.getIsWeight());
            sku.setPriceGuide(spu.getPriceGuide());
            sku.setPriceRetail(spu.getPriceRetail());
            sku.setPriceMember(spu.getPriceMember());
            sku.setPriceFranchise(spu.getPriceFranchise());
            sku.setCostPrice(BigDecimal.ZERO);
            sku.setStatus("ON_SALE");
            sku.setDeleted(0);
            skuMapper.insert(sku);
            return;
        }
        for (ProductSku sku : skus) {
            sku.setId(null);
            sku.setTenantId(spu.getTenantId());
            sku.setSpuId(spu.getId());
            sku.setStatus(sku.getStatus() == null ? "ON_SALE" : sku.getStatus());
            sku.setDeleted(0);
            skuMapper.insert(sku);
            if (sku.getBarcode() != null && !sku.getBarcode().isBlank()) {
                ProductBarcode barcode = new ProductBarcode();
                barcode.setTenantId(spu.getTenantId());
                barcode.setSkuId(sku.getId());
                barcode.setBarcode(sku.getBarcode());
                barcodeMapper.insert(barcode);
            }
        }
    }

    private String text(Cell cell) {
        return text(cell, "");
    }

    private String text(Cell cell, String defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        cell.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        String value = cell.getStringCellValue();
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private double number(Cell cell, double defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        try {
            return cell.getNumericCellValue();
        } catch (Exception e) {
            try {
                return Double.parseDouble(cell.getStringCellValue());
            } catch (Exception ex) {
                return defaultValue;
            }
        }
    }

    private Object nvl(Object v) {
        return v == null ? "" : v;
    }

    @lombok.Data
    public static class SpuSaveRequest {
        private ProductSpu spu;
        private List<ProductSku> skus;
    }
}
