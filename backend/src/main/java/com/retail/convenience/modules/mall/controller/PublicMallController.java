package com.retail.convenience.modules.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.TenantContext;
import com.retail.convenience.common.tenant.TenantGuardService;
import com.retail.convenience.modules.cms.entity.CmsBanner;
import com.retail.convenience.modules.cms.mapper.CmsBannerMapper;
import com.retail.convenience.modules.mall.entity.DeliveryRule;
import com.retail.convenience.modules.mall.mapper.DeliveryRuleMapper;
import com.retail.convenience.modules.member.entity.Coupon;
import com.retail.convenience.modules.member.entity.MemberLevel;
import com.retail.convenience.modules.member.mapper.CouponMapper;
import com.retail.convenience.modules.member.mapper.MemberLevelMapper;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.product.entity.ProductCategory;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductCategoryMapper;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/public/site/{tenantCode}")
public class PublicMallController {

    private final TenantGuardService tenantGuardService;
    private final ProductCategoryMapper categoryMapper;
    private final ProductSpuMapper spuMapper;
    private final ProductSkuMapper skuMapper;
    private final StoreMapper storeMapper;
    private final CmsBannerMapper bannerMapper;
    private final CouponMapper couponMapper;
    private final DeliveryRuleMapper deliveryRuleMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final com.retail.convenience.modules.member.mapper.PromotionMapper promotionMapper;
    private final com.retail.convenience.modules.member.mapper.PromotionItemMapper promotionItemMapper;

    public PublicMallController(TenantGuardService tenantGuardService, ProductCategoryMapper categoryMapper,
                                ProductSpuMapper spuMapper, ProductSkuMapper skuMapper,
                                StoreMapper storeMapper, CmsBannerMapper bannerMapper,
                                CouponMapper couponMapper, DeliveryRuleMapper deliveryRuleMapper,
                                MemberLevelMapper memberLevelMapper,
                                com.retail.convenience.modules.member.mapper.PromotionMapper promotionMapper,
                                com.retail.convenience.modules.member.mapper.PromotionItemMapper promotionItemMapper) {
        this.tenantGuardService = tenantGuardService;
        this.categoryMapper = categoryMapper;
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
        this.storeMapper = storeMapper;
        this.bannerMapper = bannerMapper;
        this.couponMapper = couponMapper;
        this.deliveryRuleMapper = deliveryRuleMapper;
        this.memberLevelMapper = memberLevelMapper;
        this.promotionMapper = promotionMapper;
        this.promotionItemMapper = promotionItemMapper;
    }

    @GetMapping("/home")
    public ApiResult<Map<String, Object>> home(@PathVariable String tenantCode) {
        return ApiResult.ok(withTenant(tenantCode, () -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("banners", bannerMapper.selectList(new LambdaQueryWrapper<CmsBanner>()
                    .eq(CmsBanner::getPosition, "MINI").eq(CmsBanner::getStatus, "ACTIVE")
                    .orderByAsc(CmsBanner::getSortNo)));
            map.put("categories", categoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                    .eq(ProductCategory::getDeleted, 0).eq(ProductCategory::getStatus, "ACTIVE")
                    .orderByAsc(ProductCategory::getSortNo)));
            map.put("stores", storeMapper.selectList(new LambdaQueryWrapper<Store>()
                    .eq(Store::getDeleted, 0).eq(Store::getStatus, "OPEN")
                    .eq(Store::getOnlineEnabled, 1).orderByAsc(Store::getCode)));
            map.put("products", onlineProducts(null, null, 20));
            map.put("coupons", couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                    .eq(Coupon::getStatus, "ACTIVE")
                    .le(Coupon::getValidFrom, LocalDateTime.now())
                    .ge(Coupon::getValidTo, LocalDateTime.now())
                    .orderByDesc(Coupon::getCreatedAt).last("limit 6")));
            map.put("levels", memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                    .orderByAsc(MemberLevel::getMinPoints)));
            return map;
        }));
    }

    @GetMapping("/categories")
    public ApiResult<List<ProductCategory>> categories(@PathVariable String tenantCode) {
        return ApiResult.ok(withTenant(tenantCode, () -> categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getDeleted, 0)
                        .eq(ProductCategory::getStatus, "ACTIVE")
                        .orderByAsc(ProductCategory::getSortNo))));
    }

    @GetMapping("/products")
    public ApiResult<List<Map<String, Object>>> products(@PathVariable String tenantCode,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "50") int limit) {
        return ApiResult.ok(withTenant(tenantCode, () -> onlineProducts(categoryId, keyword, limit)));
    }

    @GetMapping("/products/{skuId}")
    public ApiResult<Map<String, Object>> product(@PathVariable String tenantCode, @PathVariable Long skuId) {
        return ApiResult.ok(withTenant(tenantCode, () -> {
            ProductSku sku = skuMapper.selectById(skuId);
            if (sku == null || !"ON_SALE".equals(sku.getStatus())) {
                throw BizException.notFound("商品不存在或已下架");
            }
            ProductSpu spu = spuMapper.selectById(sku.getSpuId());
            if (spu == null || Integer.valueOf(0).equals(spu.getOnlineSale())) {
                throw new BizException("该商品仅支持线下门店购买");
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sku", sku);
            map.put("spu", spu);
            return map;
        }));
    }

    @GetMapping("/stores/{storeId}/delivery-rule")
    public ApiResult<DeliveryRule> deliveryRule(@PathVariable String tenantCode, @PathVariable Long storeId) {
        return ApiResult.ok(withTenant(tenantCode, () -> deliveryRuleMapper.selectOne(
                new LambdaQueryWrapper<DeliveryRule>()
                        .eq(DeliveryRule::getStoreId, storeId)
                        .eq(DeliveryRule::getEnabled, 1).last("limit 1"))));
    }

    @GetMapping("/promotions")
    public ApiResult<List<Map<String, Object>>> promotions(@PathVariable String tenantCode) {
        return ApiResult.ok(withTenant(tenantCode, () -> {
            List<com.retail.convenience.modules.member.entity.Promotion> promotions =
                    promotionMapper.selectList(new LambdaQueryWrapper<com.retail.convenience.modules.member.entity.Promotion>()
                            .eq(com.retail.convenience.modules.member.entity.Promotion::getStatus, "ACTIVE")
                            .le(com.retail.convenience.modules.member.entity.Promotion::getStartAt, LocalDateTime.now())
                            .ge(com.retail.convenience.modules.member.entity.Promotion::getEndAt, LocalDateTime.now()));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (com.retail.convenience.modules.member.entity.Promotion promotion : promotions) {
                List<com.retail.convenience.modules.member.entity.PromotionItem> items =
                        promotionItemMapper.selectList(new LambdaQueryWrapper<com.retail.convenience.modules.member.entity.PromotionItem>()
                                .eq(com.retail.convenience.modules.member.entity.PromotionItem::getPromotionId,
                                        promotion.getId()));
                List<Map<String, Object>> itemRows = new ArrayList<>();
                for (com.retail.convenience.modules.member.entity.PromotionItem item : items) {
                    ProductSku sku = skuMapper.selectById(item.getSkuId());
                    ProductSpu spu = sku == null ? null : spuMapper.selectById(sku.getSpuId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("skuId", item.getSkuId());
                    row.put("skuName", spu == null ? "" : spu.getName());
                    row.put("image", spu == null ? "" : spu.getMainImage());
                    row.put("originalPrice", item.getOriginalPrice());
                    row.put("promoPrice", item.getPromoPrice());
                    itemRows.add(row);
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("promotion", promotion);
                row.put("items", itemRows);
                rows.add(row);
            }
            return rows;
        }));
    }

    private List<Map<String, Object>> onlineProducts(Long categoryId, String keyword, int limit) {
        List<ProductSpu> spus = spuMapper.selectList(new LambdaQueryWrapper<ProductSpu>()
                .eq(ProductSpu::getDeleted, 0)
                .eq(ProductSpu::getStatus, "ON_SALE")
                .eq(ProductSpu::getOnlineSale, 1)
                .eq(categoryId != null, ProductSpu::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isBlank(), ProductSpu::getName, keyword)
                .orderByDesc(ProductSpu::getCreatedAt)
                .last("limit " + Math.max(1, Math.min(limit, 100))));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductSpu spu : spus) {
            List<ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getSpuId, spu.getId())
                    .eq(ProductSku::getDeleted, 0)
                    .eq(ProductSku::getStatus, "ON_SALE"));
            if (skus.isEmpty()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("spu", spu);
            row.put("sku", skus.get(0));
            rows.add(row);
        }
        return rows;
    }

    private <T> T withTenant(String tenantCode, Supplier<T> supplier) {
        PlatformTenant tenant = tenantGuardService.findByCode(tenantCode);
        if (tenant == null) {
            throw BizException.notFound("站点不存在");
        }
        tenantGuardService.findActive(tenant.getTenantId());
        TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
        try {
            return supplier.get();
        } finally {
            TenantContext.clear();
        }
    }
}
