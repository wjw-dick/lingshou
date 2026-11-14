package com.retail.convenience.modules.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.cms.entity.CmsBanner;
import com.retail.convenience.modules.cms.mapper.CmsBannerMapper;
import com.retail.convenience.modules.member.entity.Coupon;
import com.retail.convenience.modules.member.entity.Member;
import com.retail.convenience.modules.member.entity.MemberAddress;
import com.retail.convenience.modules.member.entity.MemberCoupon;
import com.retail.convenience.modules.member.entity.MemberFavorite;
import com.retail.convenience.modules.member.entity.MemberLevel;
import com.retail.convenience.modules.member.mapper.CouponMapper;
import com.retail.convenience.modules.member.mapper.MemberAddressMapper;
import com.retail.convenience.modules.member.mapper.MemberCouponMapper;
import com.retail.convenience.modules.member.mapper.MemberFavoriteMapper;
import com.retail.convenience.modules.member.mapper.MemberLevelMapper;
import com.retail.convenience.modules.member.mapper.MemberMapper;
import com.retail.convenience.modules.product.entity.ProductCategory;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductCategoryMapper;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.sales.dto.OnlineOrderRequest;
import com.retail.convenience.modules.sales.entity.InvoiceRequest;
import com.retail.convenience.modules.sales.entity.OrderEvaluation;
import com.retail.convenience.modules.sales.entity.RefundOrder;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.entity.SalesOrderItem;
import com.retail.convenience.modules.sales.mapper.InvoiceRequestMapper;
import com.retail.convenience.modules.sales.mapper.OrderEvaluationMapper;
import com.retail.convenience.modules.sales.mapper.RefundOrderMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderItemMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderMapper;
import com.retail.convenience.modules.sales.service.SalesOrderService;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.system.entity.TenantNotice;
import com.retail.convenience.modules.system.mapper.TenantNoticeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/app")
public class AppController {

    private final MemberMapper memberMapper;
    private final MemberLevelMapper levelMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final CouponMapper couponMapper;
    private final MemberAddressMapper addressMapper;
    private final MemberFavoriteMapper favoriteMapper;
    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final RefundOrderMapper refundMapper;
    private final OrderEvaluationMapper evaluationMapper;
    private final InvoiceRequestMapper invoiceMapper;
    private final SalesOrderService orderService;
    private final StoreMapper storeMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductSpuMapper spuMapper;
    private final ProductSkuMapper skuMapper;
    private final CmsBannerMapper bannerMapper;
    private final TenantNoticeMapper noticeMapper;
    private final String cryptoSecret;

    public AppController(MemberMapper memberMapper, MemberLevelMapper levelMapper,
                         MemberCouponMapper memberCouponMapper, CouponMapper couponMapper,
                         MemberAddressMapper addressMapper, MemberFavoriteMapper favoriteMapper,
                         SalesOrderMapper orderMapper, SalesOrderItemMapper orderItemMapper,
                         RefundOrderMapper refundMapper, OrderEvaluationMapper evaluationMapper,
                         InvoiceRequestMapper invoiceMapper, SalesOrderService orderService,
                         StoreMapper storeMapper, ProductCategoryMapper categoryMapper,
                         ProductSpuMapper spuMapper, ProductSkuMapper skuMapper,
                         CmsBannerMapper bannerMapper, TenantNoticeMapper noticeMapper,
                         @Value("${app.jwt.secret}") String cryptoSecret) {
        this.memberMapper = memberMapper;
        this.levelMapper = levelMapper;
        this.memberCouponMapper = memberCouponMapper;
        this.couponMapper = couponMapper;
        this.addressMapper = addressMapper;
        this.favoriteMapper = favoriteMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.refundMapper = refundMapper;
        this.evaluationMapper = evaluationMapper;
        this.invoiceMapper = invoiceMapper;
        this.orderService = orderService;
        this.storeMapper = storeMapper;
        this.categoryMapper = categoryMapper;
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
        this.bannerMapper = bannerMapper;
        this.noticeMapper = noticeMapper;
        this.cryptoSecret = cryptoSecret;
    }

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile() {
        Member member = currentMember();
        MemberLevel level = member.getLevelId() == null ? null : levelMapper.selectById(member.getLevelId());
        long unusedCoupons = memberCouponMapper.selectCount(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getMemberId, member.getId()).eq(MemberCoupon::getStatus, "UNUSED"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("member", member);
        map.put("level", level);
        map.put("unusedCoupons", unusedCoupons);
        return ApiResult.ok(map);
    }

    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@RequestBody Member request) {
        Member member = currentMember();
        if (request.getNickname() != null) {
            member.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            member.setAvatar(request.getAvatar());
        }
        if (request.getBirthday() != null) {
            member.setBirthday(request.getBirthday());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            member.setPhone(AesUtil.encrypt(request.getPhone(), cryptoSecret));
            member.setPhoneMask(request.getPhone().substring(0, 3) + "****"
                    + request.getPhone().substring(request.getPhone().length() - 4));
        }
        memberMapper.updateById(member);
        return ApiResult.ok();
    }

    @GetMapping("/home")
    public ApiResult<Map<String, Object>> home() {
        Member member = currentMember();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("member", Map.of("nickname", member.getNickname(), "points", member.getPoints(),
                "walletBalance", member.getWalletBalance()));
        map.put("banners", bannerMapper.selectList(new LambdaQueryWrapper<CmsBanner>()
                .eq(CmsBanner::getPosition, "MINI").eq(CmsBanner::getStatus, "ACTIVE")
                .orderByAsc(CmsBanner::getSortNo)));
        map.put("notices", noticeMapper.selectList(new LambdaQueryWrapper<TenantNotice>()
                .orderByDesc(TenantNotice::getPublishedAt).last("limit 5")));
        map.put("stores", storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getDeleted, 0).eq(Store::getStatus, "OPEN")
                .eq(Store::getOnlineEnabled, 1).orderByAsc(Store::getCode)));
        map.put("categories", categoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getDeleted, 0).eq(ProductCategory::getStatus, "ACTIVE")
                .orderByAsc(ProductCategory::getSortNo)));
        List<ProductSpu> spus = spuMapper.selectList(new LambdaQueryWrapper<ProductSpu>()
                .eq(ProductSpu::getDeleted, 0).eq(ProductSpu::getStatus, "ON_SALE")
                .eq(ProductSpu::getOnlineSale, 1).orderByDesc(ProductSpu::getCreatedAt).last("limit 20"));
        List<Map<String, Object>> products = new ArrayList<>();
        for (ProductSpu spu : spus) {
            ProductSku sku = skuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getSpuId, spu.getId()).eq(ProductSku::getDeleted, 0)
                    .eq(ProductSku::getStatus, "ON_SALE").last("limit 1"));
            if (sku != null) {
                products.add(Map.of("spu", spu, "sku", sku));
            }
        }
        map.put("products", products);
        return ApiResult.ok(map);
    }

    @GetMapping("/coupons")
    public ApiResult<Map<String, Object>> coupons() {
        Member member = currentMember();
        List<MemberCoupon> mine = memberCouponMapper.selectList(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getMemberId, member.getId()).orderByDesc(MemberCoupon::getReceivedAt));
        List<Long> couponIds = mine.stream().map(MemberCoupon::getCouponId).toList();
        Map<Long, Coupon> couponMap = couponIds.isEmpty() ? Map.of() : couponMapper.selectList(
                        new LambdaQueryWrapper<Coupon>().in(Coupon::getId, couponIds))
                .stream().collect(Collectors.toMap(Coupon::getId, Function.identity()));
        List<Map<String, Object>> myCoupons = mine.stream().map(mc -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("memberCoupon", mc);
            row.put("coupon", couponMap.get(mc.getCouponId()));
            return row;
        }).toList();
        List<Coupon> available = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, "ACTIVE")
                .le(Coupon::getValidFrom, LocalDateTime.now())
                .ge(Coupon::getValidTo, LocalDateTime.now()));
        List<Map<String, Object>> availableRows = available.stream().map(coupon -> {
            long owned = mine.stream().filter(mc -> mc.getCouponId().equals(coupon.getId())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("coupon", coupon);
            row.put("owned", owned);
            row.put("claimable", owned < coupon.getPerUserLimit()
                    && (coupon.getTotalQty() == null || coupon.getIssuedQty() < coupon.getTotalQty()));
            return row;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("myCoupons", myCoupons);
        result.put("available", availableRows);
        return ApiResult.ok(result);
    }

    @PostMapping("/coupons/{couponId}/claim")
    public ApiResult<Void> claimCoupon(@PathVariable Long couponId) {
        Member member = currentMember();
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !"ACTIVE".equals(coupon.getStatus())) {
            throw new BizException("优惠券不存在或已停用");
        }
        if (coupon.getValidTo().isBefore(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        long owned = memberCouponMapper.selectCount(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getMemberId, member.getId())
                .eq(MemberCoupon::getCouponId, couponId));
        if (owned >= coupon.getPerUserLimit()) {
            throw new BizException("已达到领取上限");
        }
        if (coupon.getTotalQty() != null && coupon.getIssuedQty() >= coupon.getTotalQty()) {
            throw new BizException("优惠券已领完");
        }
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setCouponId(couponId);
        memberCoupon.setMemberId(member.getId());
        memberCoupon.setStatus("UNUSED");
        memberCoupon.setReceivedAt(LocalDateTime.now());
        memberCouponMapper.insert(memberCoupon);
        coupon.setIssuedQty(coupon.getIssuedQty() + 1);
        couponMapper.updateById(coupon);
        return ApiResult.ok();
    }

    @GetMapping("/orders")
    public ApiResult<PageResult<Map<String, Object>>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Member member = currentMember();
        Page<SalesOrder> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getMemberId, member.getId())
                        .eq(status != null && !status.isBlank(), SalesOrder::getStatus, status)
                        .orderByDesc(SalesOrder::getCreatedAt));
        Map<Long, List<SalesOrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<SalesOrderItem>().in(SalesOrderItem::getOrderId,
                                result.getRecords().stream().map(SalesOrder::getId).toList()))
                .stream().collect(Collectors.groupingBy(SalesOrderItem::getOrderId));
        List<Map<String, Object>> rows = result.getRecords().stream().map(order -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", order);
            row.put("items", itemsByOrder.getOrDefault(order.getId(), List.of()));
            return row;
        }).toList();
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @GetMapping("/orders/{id}")
    public ApiResult<Map<String, Object>> orderDetail(@PathVariable Long id) {
        SalesOrder order = ownedOrder(id);
        List<SalesOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, id));
        List<RefundOrder> refunds = refundMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getOrderId, id));
        OrderEvaluation evaluation = evaluationMapper.selectOne(new LambdaQueryWrapper<OrderEvaluation>()
                .eq(OrderEvaluation::getOrderId, id).last("limit 1"));
        return ApiResult.ok(Map.of("order", order, "items", items, "refunds", refunds,
                "evaluation", evaluation == null ? Map.of() : evaluation));
    }

    @PostMapping("/orders")
    public ApiResult<SalesOrder> createOrder(@RequestBody OnlineOrderRequest request) {
        Member member = currentMember();
        return ApiResult.ok(orderService.createOnline(request));
    }

    @PostMapping("/orders/{id}/pay")
    public ApiResult<SalesOrder> payOrder(@PathVariable Long id,
                                          @RequestParam(defaultValue = "WECHAT") String method) {
        ownedOrder(id);
        return ApiResult.ok(orderService.pay(id, method));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResult<Void> cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        ownedOrder(id);
        orderService.cancel(id, reason);
        return ApiResult.ok();
    }

    @PostMapping("/orders/{id}/evaluate")
    public ApiResult<Void> evaluate(@PathVariable Long id, @RequestBody OrderEvaluation evaluation) {
        SalesOrder order = ownedOrder(id);
        evaluation.setId(null);
        evaluation.setOrderId(id);
        evaluation.setMemberId(currentMember().getId());
        evaluation.setStoreId(order.getStoreId());
        evaluationMapper.insert(evaluation);
        return ApiResult.ok();
    }

    @PostMapping("/orders/{id}/invoice")
    public ApiResult<Void> invoice(@PathVariable Long id, @RequestBody InvoiceRequest request) {
        SalesOrder order = ownedOrder(id);
        request.setId(null);
        request.setOrderId(id);
        request.setMemberId(currentMember().getId());
        request.setAmount(order.getPaidAmount());
        request.setStatus("PENDING");
        invoiceMapper.insert(request);
        return ApiResult.ok();
    }

    @GetMapping("/addresses")
    public ApiResult<List<MemberAddress>> addresses() {
        return ApiResult.ok(addressMapper.selectList(new LambdaQueryWrapper<MemberAddress>()
                .eq(MemberAddress::getMemberId, currentMember().getId())
                .orderByDesc(MemberAddress::getIsDefault)));
    }

    @PostMapping("/addresses")
    public ApiResult<Void> addAddress(@RequestBody MemberAddress address) {
        address.setId(null);
        address.setMemberId(currentMember().getId());
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            addressMapper.selectList(new LambdaQueryWrapper<MemberAddress>()
                            .eq(MemberAddress::getMemberId, address.getMemberId()))
                    .forEach(item -> {
                        item.setIsDefault(0);
                        addressMapper.updateById(item);
                    });
        }
        addressMapper.insert(address);
        return ApiResult.ok();
    }

    @PutMapping("/addresses/{id}")
    public ApiResult<Void> updateAddress(@PathVariable Long id, @RequestBody MemberAddress address) {
        address.setId(id);
        address.setMemberId(currentMember().getId());
        addressMapper.updateById(address);
        return ApiResult.ok();
    }

    @DeleteMapping("/addresses/{id}")
    public ApiResult<Void> deleteAddress(@PathVariable Long id) {
        addressMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/favorites")
    public ApiResult<List<Map<String, Object>>> favorites() {
        List<MemberFavorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<MemberFavorite>()
                .eq(MemberFavorite::getMemberId, currentMember().getId())
                .orderByDesc(MemberFavorite::getCreatedAt));
        List<Long> skuIds = favorites.stream().map(MemberFavorite::getSkuId).toList();
        if (skuIds.isEmpty()) {
            return ApiResult.ok(List.of());
        }
        Map<Long, ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .in(ProductSku::getId, skuIds))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        return ApiResult.ok(favorites.stream().map(f -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("favorite", f);
            ProductSku sku = skus.get(f.getSkuId());
            row.put("sku", sku);
            row.put("spu", sku == null ? null : spuMapper.selectById(sku.getSpuId()));
            return row;
        }).toList());
    }

    @PostMapping("/favorites/{skuId}")
    public ApiResult<Void> toggleFavorite(@PathVariable Long skuId) {
        Long memberId = currentMember().getId();
        MemberFavorite favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<MemberFavorite>()
                .eq(MemberFavorite::getMemberId, memberId)
                .eq(MemberFavorite::getSkuId, skuId).last("limit 1"));
        if (favorite == null) {
            favorite = new MemberFavorite();
            favorite.setMemberId(memberId);
            favorite.setSkuId(skuId);
            favoriteMapper.insert(favorite);
        } else {
            favoriteMapper.deleteById(favorite.getId());
        }
        return ApiResult.ok();
    }

    @GetMapping("/notices")
    public ApiResult<List<TenantNotice>> notices() {
        return ApiResult.ok(noticeMapper.selectList(new LambdaQueryWrapper<TenantNotice>()
                .orderByDesc(TenantNotice::getPublishedAt).last("limit 20")));
    }

    private Member currentMember() {
        LoginUser user = AuthContext.require();
        if (user.getMemberId() == null) {
            throw BizException.forbidden("请先登录会员账号");
        }
        Member member = memberMapper.selectById(user.getMemberId());
        if (member == null) {
            throw BizException.notFound("会员不存在");
        }
        return member;
    }

    private SalesOrder ownedOrder(Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null || !currentMember().getId().equals(order.getMemberId())) {
            throw BizException.notFound("订单不存在");
        }
        return order;
    }
}
