package com.retail.convenience.modules.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.inventory.entity.Stock;
import com.retail.convenience.modules.inventory.mapper.StockMapper;
import com.retail.convenience.modules.member.entity.Coupon;
import com.retail.convenience.modules.member.entity.Member;
import com.retail.convenience.modules.member.entity.MemberCoupon;
import com.retail.convenience.modules.member.mapper.CouponMapper;
import com.retail.convenience.modules.member.mapper.MemberCouponMapper;
import com.retail.convenience.modules.member.mapper.MemberMapper;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.sales.dto.OrderPriceResult;
import com.retail.convenience.modules.sales.dto.PosOrderRequest;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.service.SalesOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/pos")
public class PosController {

    private final SalesOrderService orderService;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final StockMapper stockMapper;
    private final MemberMapper memberMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final CouponMapper couponMapper;
    private final com.retail.convenience.modules.product.mapper.ProductCategoryMapper categoryMapper;

    public PosController(SalesOrderService orderService, ProductSkuMapper skuMapper,
                         ProductSpuMapper spuMapper, StockMapper stockMapper,
                         MemberMapper memberMapper, MemberCouponMapper memberCouponMapper,
                         CouponMapper couponMapper,
                         com.retail.convenience.modules.product.mapper.ProductCategoryMapper categoryMapper) {
        this.orderService = orderService;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.stockMapper = stockMapper;
        this.memberMapper = memberMapper;
        this.memberCouponMapper = memberCouponMapper;
        this.couponMapper = couponMapper;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/categories")
    @RequirePerm("sales:pos")
    public ApiResult<List<com.retail.convenience.modules.product.entity.ProductCategory>> categories() {
        return ApiResult.ok(categoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.retail.convenience.modules.product.entity.ProductCategory>()
                        .eq(com.retail.convenience.modules.product.entity.ProductCategory::getDeleted, 0)
                        .eq(com.retail.convenience.modules.product.entity.ProductCategory::getStatus, "ACTIVE")
                        .orderByAsc(com.retail.convenience.modules.product.entity.ProductCategory::getSortNo)));
    }

    @GetMapping("/products")
    @RequirePerm("sales:pos")
    public ApiResult<List<Map<String, Object>>> products(@RequestParam Long storeId,
                                                         @RequestParam(required = false) String keyword) {
        List<ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getDeleted, 0).eq(ProductSku::getStatus, "ON_SALE").last("limit 500"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductSku sku : skus) {
            ProductSpu spu = spuMapper.selectById(sku.getSpuId());
            if (spu == null) {
                continue;
            }
            if (keyword != null && !keyword.isBlank()
                    && !(spu.getName().contains(keyword) || (sku.getBarcode() != null && sku.getBarcode().contains(keyword)))) {
                continue;
            }
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getOwnerType, "STORE").eq(Stock::getOwnerId, storeId)
                    .eq(Stock::getSkuId, sku.getId()).last("limit 1"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("skuId", sku.getId());
            row.put("skuCode", sku.getSkuCode());
            row.put("barcode", sku.getBarcode());
            row.put("name", spu.getName());
            row.put("categoryId", spu.getCategoryId());
            row.put("spec", sku.getSpec());
            row.put("price", sku.getPriceRetail());
            row.put("memberPrice", sku.getPriceMember());
            row.put("stock", stock == null ? 0 : stock.getQty());
            row.put("image", spu.getMainImage());
            rows.add(row);
        }
        return ApiResult.ok(rows);
    }

    @GetMapping("/members")
    @RequirePerm("sales:pos")
    public ApiResult<List<Map<String, Object>>> members(@RequestParam String keyword) {
        String phone = keyword.replaceAll("\\D", "");
        List<Member> members = memberMapper.selectList(new LambdaQueryWrapper<Member>()
                .and(w -> w.like(Member::getNickname, keyword)
                        .or().like(Member::getPhoneMask, phone)
                        .or().eq(Member::getId, keyword.matches("\\d+") ? Long.valueOf(keyword) : -1L))
                .last("limit 20"));
        return ApiResult.ok(members.stream().map(m -> Map.<String, Object>of(
                "id", m.getId(), "nickname", m.getNickname(), "phoneMask", m.getPhoneMask(),
                "points", m.getPoints(), "walletBalance", m.getWalletBalance())).toList());
    }

    @GetMapping("/member-coupons")
    @RequirePerm("sales:pos")
    public ApiResult<List<Map<String, Object>>> memberCoupons(@RequestParam Long memberId) {
        List<MemberCoupon> memberCoupons = memberCouponMapper.selectList(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getMemberId, memberId).eq(MemberCoupon::getStatus, "UNUSED"));
        if (memberCoupons.isEmpty()) {
            return ApiResult.ok(List.of());
        }
        List<Long> couponIds = memberCoupons.stream().map(MemberCoupon::getCouponId).toList();
        Map<Long, Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                        .in(Coupon::getId, couponIds))
                .stream().collect(Collectors.toMap(Coupon::getId, c -> c));
        return ApiResult.ok(memberCoupons.stream().map(mc -> {
            Coupon coupon = coupons.get(mc.getCouponId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("memberCouponId", mc.getId());
            row.put("couponId", mc.getCouponId());
            row.put("name", coupon == null ? "-" : coupon.getName());
            row.put("type", coupon == null ? "" : coupon.getType());
            row.put("threshold", coupon == null ? 0 : coupon.getThresholdAmount());
            row.put("discount", coupon == null ? 0 : coupon.getDiscountAmount());
            return row;
        }).toList());
    }

    @PostMapping("/preview")
    @RequirePerm("sales:pos")
    public ApiResult<OrderPriceResult> preview(@RequestBody PosOrderRequest request) {
        return ApiResult.ok(orderService.previewPos(request));
    }

    @PostMapping("/checkout")
    @RequirePerm("sales:pos")
    public ApiResult<SalesOrder> checkout(@RequestBody PosOrderRequest request) {
        Member member = null;
        if (request.getUseWallet() != null && request.getUseWallet() && request.getMemberId() == null) {
            throw new BizException("使用储值需先识别会员");
        }
        return ApiResult.ok(orderService.createPos(request));
    }
}
