package com.retail.convenience.modules.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.common.util.GeoUtil;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.inventory.service.InventoryService;
import com.retail.convenience.modules.mall.entity.DeliveryRule;
import com.retail.convenience.modules.mall.mapper.DeliveryRuleMapper;
import com.retail.convenience.modules.member.entity.Coupon;
import com.retail.convenience.modules.member.entity.Member;
import com.retail.convenience.modules.member.entity.MemberCoupon;
import com.retail.convenience.modules.member.entity.MemberPointFlow;
import com.retail.convenience.modules.member.entity.MemberWalletFlow;
import com.retail.convenience.modules.member.mapper.CouponMapper;
import com.retail.convenience.modules.member.mapper.MemberCouponMapper;
import com.retail.convenience.modules.member.mapper.MemberMapper;
import com.retail.convenience.modules.member.mapper.MemberPointFlowMapper;
import com.retail.convenience.modules.member.mapper.MemberWalletFlowMapper;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.entity.ProductSpu;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.product.mapper.ProductSpuMapper;
import com.retail.convenience.modules.member.entity.Promotion;
import com.retail.convenience.modules.member.entity.PromotionItem;
import com.retail.convenience.modules.member.mapper.PromotionItemMapper;
import com.retail.convenience.modules.member.mapper.PromotionMapper;
import com.retail.convenience.modules.sales.dto.OnlineOrderRequest;
import com.retail.convenience.modules.sales.dto.OrderPriceResult;
import com.retail.convenience.modules.sales.dto.PosOrderRequest;
import com.retail.convenience.modules.sales.entity.RefundOrder;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.entity.SalesOrderItem;
import com.retail.convenience.modules.sales.mapper.RefundOrderMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderItemMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderMapper;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.system.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesOrderService {

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final RefundOrderMapper refundMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final MemberMapper memberMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final CouponMapper couponMapper;
    private final MemberPointFlowMapper pointFlowMapper;
    private final MemberWalletFlowMapper walletFlowMapper;
    private final PromotionMapper promotionMapper;
    private final PromotionItemMapper promotionItemMapper;
    private final DeliveryRuleMapper deliveryRuleMapper;
    private final StoreMapper storeMapper;
    private final InventoryService inventoryService;
    private final SystemConfigService configService;
    private final NoGenerator noGenerator;

    public SalesOrderService(SalesOrderMapper orderMapper, SalesOrderItemMapper itemMapper,
                             RefundOrderMapper refundMapper, ProductSkuMapper skuMapper,
                             ProductSpuMapper spuMapper, MemberMapper memberMapper,
                             MemberCouponMapper memberCouponMapper, CouponMapper couponMapper,
                             MemberPointFlowMapper pointFlowMapper, MemberWalletFlowMapper walletFlowMapper,
                             PromotionMapper promotionMapper, PromotionItemMapper promotionItemMapper,
                             DeliveryRuleMapper deliveryRuleMapper, StoreMapper storeMapper,
                             InventoryService inventoryService, SystemConfigService configService,
                             NoGenerator noGenerator) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.refundMapper = refundMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.memberMapper = memberMapper;
        this.memberCouponMapper = memberCouponMapper;
        this.couponMapper = couponMapper;
        this.pointFlowMapper = pointFlowMapper;
        this.walletFlowMapper = walletFlowMapper;
        this.promotionMapper = promotionMapper;
        this.promotionItemMapper = promotionItemMapper;
        this.deliveryRuleMapper = deliveryRuleMapper;
        this.storeMapper = storeMapper;
        this.inventoryService = inventoryService;
        this.configService = configService;
        this.noGenerator = noGenerator;
    }

    public OrderPriceResult previewPos(PosOrderRequest request) {
        return price(request.getStoreId(), request.getMemberId(), "PICKUP", toLines(request.getItems()),
                request.getMemberCouponId(), request.getPointsUsed() == null ? 0 : request.getPointsUsed(), false);
    }

    @Transactional
    public SalesOrder createPos(PosOrderRequest request) {
        if (request.getStoreId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请选择门店并添加商品");
        }
        LoginUser user = AuthContext.require();
        SalesOrder order = buildOrder(request.getStoreId(), request.getMemberId(), "POS", "PICKUP",
                toLines(request.getItems()), request.getMemberCouponId(), request.getPointsUsed(),
                false, null, null, null, null, null, request.getRemark());
        BigDecimal walletPaid = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getUseWallet()) && request.getMemberId() != null) {
            Member member = requireMember(request.getMemberId());
            walletPaid = order.getPayableAmount().min(member.getWalletBalance());
            if (walletPaid.compareTo(BigDecimal.ZERO) > 0) {
                deductWallet(member, walletPaid, "CONSUME", order.getId(), "收银储值支付");
            }
        }
        order.setWalletPaid(walletPaid);
        order.setPaidAmount(order.getPayableAmount());
        order.setPaymentMethod(order.getPayableAmount().subtract(walletPaid).compareTo(BigDecimal.ZERO) == 0
                ? "WALLET" : (request.getPaymentMethod() == null ? "CASH" : request.getPaymentMethod()));
        order.setPayStatus("PAID");
        order.setStatus("COMPLETED");
        order.setPaidAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        for (OrderPriceResult.Line line : priceResultOf(order).getLines()) {
            inventoryService.deduct("STORE", order.getStoreId(), line.getSkuId(), line.getQty(),
                    "SALE", "ORDER", order.getId(), "收银售出", user.getUsername());
        }
        if (request.getMemberId() != null) {
            addMemberConsume(request.getMemberId(), order.getPaidAmount());
            earnPoints(request.getMemberId(), order.getPaidAmount(), order.getId());
        }
        return order;
    }

    @Transactional
    public SalesOrder createOnline(OnlineOrderRequest request) {
        if (request.getStoreId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请选择门店并添加商品");
        }
        LoginUser user = AuthContext.require();
        if (user.getMemberId() == null) {
            throw BizException.forbidden("请先登录会员账号");
        }
        Store store = storeMapper.selectById(request.getStoreId());
        if (store == null || !"OPEN".equals(store.getStatus()) || !Integer.valueOf(1).equals(store.getOnlineEnabled())) {
            throw new BizException("门店未营业或暂不支持线上购买");
        }
        boolean delivery = "DELIVERY".equalsIgnoreCase(request.getDeliveryType());
        if (delivery) {
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
                throw new BizException("请填写配送地址");
            }
            DeliveryRule rule = deliveryRuleMapper.selectOne(new LambdaQueryWrapper<DeliveryRule>()
                    .eq(DeliveryRule::getStoreId, request.getStoreId()).eq(DeliveryRule::getEnabled, 1)
                    .last("limit 1"));
            if (rule != null && request.getDeliveryLng() != null && request.getDeliveryLat() != null) {
                double distance = GeoUtil.distanceMeters(store.getLng(), store.getLat(),
                        request.getDeliveryLng(), request.getDeliveryLat());
                if (distance > rule.getRadiusM()) {
                    throw new BizException("超出配送范围，距离约 " + Math.round(distance / 100) / 10.0 + " 公里");
                }
            }
        }
        SalesOrder order = buildOrder(request.getStoreId(), user.getMemberId(), "ONLINE",
                delivery ? "DELIVERY" : "PICKUP", toLines(request.getItems(), true),
                request.getMemberCouponId(), request.getPointsUsed(), delivery,
                request.getDeliveryName(), request.getDeliveryPhone(), request.getDeliveryAddress(),
                request.getDeliveryLng(), request.getDeliveryLat(), request.getRemark());
        order.setExpectedTime(request.getExpectedTime());
        order.setStatus("PENDING_PAY");
        order.setPayStatus("UNPAID");
        orderMapper.updateById(order);
        for (OrderPriceResult.Line line : priceResultOf(order).getLines()) {
            inventoryService.reserve(order.getStoreId(), line.getSkuId(), line.getQty());
        }
        return order;
    }

    @Transactional
    public SalesOrder pay(Long orderId, String paymentMethod) {
        SalesOrder order = require(orderId);
        if ("PAID".equals(order.getPayStatus()) || "PARTIAL_REFUND".equals(order.getPayStatus())) {
            // 重复支付回调幂等返回，避免重复扣库存/积分
            return order;
        }
        if (!"PENDING_PAY".equals(order.getStatus())) {
            throw new BizException("订单当前状态不可支付");
        }
        String method = paymentMethod == null ? "WECHAT" : paymentMethod;
        if ("WALLET".equals(method)) {
            Member member = requireMember(order.getMemberId());
            if (member.getWalletBalance().compareTo(order.getPayableAmount()) < 0) {
                throw new BizException("储值余额不足");
            }
            deductWallet(member, order.getPayableAmount(), "CONSUME", order.getId(), "订单储值支付");
            order.setWalletPaid(order.getPayableAmount());
        }
        List<SalesOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
        for (SalesOrderItem item : items) {
            inventoryService.commitReserved(order.getStoreId(), item.getSkuId(), item.getQty(),
                    "ORDER", order.getId(), AuthContext.require().getUsername());
        }
        order.setPaymentMethod(method);
        order.setPaidAmount(order.getPayableAmount());
        order.setPayStatus("PAID");
        order.setStatus("PENDING_ACCEPT");
        order.setPaidAt(LocalDateTime.now());
        order.setAcceptDeadline(LocalDateTime.now().plusMinutes(5));
        orderMapper.updateById(order);
        addMemberConsume(order.getMemberId(), order.getPaidAmount());
        earnPoints(order.getMemberId(), order.getPaidAmount(), order.getId());
        return order;
    }

    @Transactional
    public void accept(Long orderId) {
        SalesOrder order = require(orderId);
        if (!"PENDING_ACCEPT".equals(order.getStatus())) {
            throw new BizException("订单当前状态不可接单");
        }
        order.setStatus("PICKING");
        order.setAcceptDeadline(null);
        orderMapper.updateById(order);
    }

    @Transactional
    public void pickComplete(Long orderId) {
        SalesOrder order = require(orderId);
        if (!"PICKING".equals(order.getStatus())) {
            throw new BizException("订单当前状态不可完成拣货");
        }
        order.setStatus("DELIVERY".equals(order.getDeliveryType()) ? "DELIVERING" : "WAIT_PICKUP");
        orderMapper.updateById(order);
    }

    @Transactional
    public void complete(Long orderId) {
        SalesOrder order = require(orderId);
        if (!List.of("WAIT_PICKUP", "DELIVERING").contains(order.getStatus())) {
            throw new BizException("订单当前状态不可完成");
        }
        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void cancel(Long orderId, String reason) {
        SalesOrder order = require(orderId);
        if (List.of("COMPLETED", "CANCELLED", "REFUNDED").contains(order.getStatus())) {
            throw new BizException("订单当前状态不可取消");
        }
        boolean paid = "PAID".equals(order.getPayStatus());
        List<SalesOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
        if (paid) {
            for (SalesOrderItem item : items) {
                inventoryService.inbound("STORE", order.getStoreId(), item.getSkuId(), item.getQty(),
                        "REFUND" + order.getOrderNo(), LocalDateTime.now().toLocalDate(),
                        LocalDateTime.now().toLocalDate().plusYears(1),
                        "REFUND", "ORDER", order.getId(), "取消订单回库", "system");
            }
            createRefund(order, "FULL", order.getPaidAmount(), reason == null ? "订单取消退款" : reason, "REFUNDED");
            if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
                refundWallet(order.getMemberId(), order.getWalletPaid(), order.getId(), "订单取消退回储值");
            }
            order.setPayStatus("REFUNDED");
            order.setStatus("CANCELLED");
        } else {
            for (SalesOrderItem item : items) {
                inventoryService.release(order.getStoreId(), item.getSkuId(), item.getQty());
            }
            order.setStatus("CANCELLED");
        }
        restoreBenefits(order);
        orderMapper.updateById(order);
    }

    @Transactional
    public RefundOrder refund(Long orderId, String reason, BigDecimal amount) {
        SalesOrder order = require(orderId);
        if (!"PAID".equals(order.getPayStatus()) && !"PARTIAL_REFUND".equals(order.getPayStatus())) {
            throw new BizException("订单未支付或已退款");
        }
        BigDecimal refundAmount = amount == null ? order.getPaidAmount() : amount;
        if (refundAmount.compareTo(order.getPaidAmount()) > 0) {
            throw new BizException("退款金额不能超过实付金额");
        }
        List<SalesOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
        for (SalesOrderItem item : items) {
            inventoryService.inbound("STORE", order.getStoreId(), item.getSkuId(), item.getQty(),
                    "REFUND" + order.getOrderNo(), LocalDateTime.now().toLocalDate(),
                    LocalDateTime.now().toLocalDate().plusYears(1),
                    "REFUND", "ORDER", order.getId(), "售后退款回库", AuthContext.require().getUsername());
        }
        if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
            refundWallet(order.getMemberId(), order.getWalletPaid().min(refundAmount), order.getId(), "售后退款退回储值");
        }
        RefundOrder refund = createRefund(order, refundAmount.compareTo(order.getPaidAmount()) == 0 ? "FULL" : "PARTIAL",
                refundAmount, reason == null ? "用户申请退款" : reason, "REFUNDED");
        order.setPayStatus(refundAmount.compareTo(order.getPaidAmount()) == 0 ? "REFUNDED" : "PARTIAL_REFUND");
        order.setStatus("REFUNDED");
        orderMapper.updateById(order);
        restoreBenefits(order);
        return refund;
    }

    public OrderPriceResult price(Long storeId, Long memberId, String channel,
                                  List<PosOrderRequest.Item> requestItems, Long memberCouponId,
                                  int pointsUsed, boolean delivery) {
        if (requestItems == null || requestItems.isEmpty()) {
            throw new BizException("请添加商品");
        }
        OrderPriceResult result = new OrderPriceResult();
        List<OrderPriceResult.Line> lines = new ArrayList<>();
        BigDecimal gross = BigDecimal.ZERO;
        Map<Long, BigDecimal> promoPrices = activePromotionPrices();
        for (PosOrderRequest.Item requestItem : requestItems) {
            if (requestItem.getSkuId() == null || requestItem.getQty() == null || requestItem.getQty() <= 0) {
                continue;
            }
            ProductSku sku = skuMapper.selectById(requestItem.getSkuId());
            if (sku == null || !"ON_SALE".equals(sku.getStatus())) {
                throw new BizException("商品已下架：" + requestItem.getSkuId());
            }
            ProductSpu spu = spuMapper.selectById(sku.getSpuId());
            if (spu != null && spu.getOnlineSale() != null && spu.getOnlineSale() == 0
                    && !"POS".equals(channel)) {
                throw new BizException("该商品仅支持线下购买：" + spu.getName());
            }
            BigDecimal price = memberId != null && sku.getPriceMember() != null
                    && sku.getPriceMember().compareTo(BigDecimal.ZERO) > 0
                    ? sku.getPriceMember() : sku.getPriceRetail();
            if (requestItem.getPrice() != null && requestItem.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                price = requestItem.getPrice();
            }
            BigDecimal promo = promoPrices.get(sku.getId());
            if (promo != null && promo.compareTo(price) < 0) {
                price = promo;
            }
            OrderPriceResult.Line line = new OrderPriceResult.Line();
            line.setSkuId(sku.getId());
            line.setSkuName(spu == null ? sku.getSkuCode() : spu.getName());
            line.setBarcode(sku.getBarcode());
            line.setPrice(price);
            line.setQty(requestItem.getQty());
            line.setAmount(price.multiply(BigDecimal.valueOf(requestItem.getQty())));
            lines.add(line);
            gross = gross.add(line.getAmount());
        }
        if (lines.isEmpty()) {
            throw new BizException("请添加有效商品");
        }
        result.setLines(lines);
        result.setGrossAmount(gross);

        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (memberCouponId != null && memberId != null) {
            MemberCoupon memberCoupon = memberCouponMapper.selectById(memberCouponId);
            if (memberCoupon == null || !memberId.equals(memberCoupon.getMemberId())) {
                throw new BizException("优惠券不存在");
            }
            if (!"UNUSED".equals(memberCoupon.getStatus())) {
                throw new BizException("优惠券已使用或已过期");
            }
            Coupon coupon = couponMapper.selectById(memberCoupon.getCouponId());
            if (coupon == null || !"ACTIVE".equals(coupon.getStatus())
                    || LocalDateTime.now().isBefore(coupon.getValidFrom())
                    || LocalDateTime.now().isAfter(coupon.getValidTo())) {
                throw new BizException("优惠券不在有效期内");
            }
            if (gross.compareTo(coupon.getThresholdAmount()) < 0) {
                throw new BizException("未满足优惠券使用门槛");
            }
            if ("FULL_REDUCTION".equals(coupon.getType())) {
                couponDiscount = coupon.getDiscountAmount();
            } else if ("DISCOUNT".equals(coupon.getType())) {
                couponDiscount = gross.multiply(BigDecimal.ONE.subtract(
                        coupon.getDiscountRate().divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP)));
            } else if ("PRODUCT".equals(coupon.getType()) && coupon.getSkuId() != null) {
                for (OrderPriceResult.Line line : lines) {
                    if (line.getSkuId().equals(coupon.getSkuId())) {
                        couponDiscount = coupon.getDiscountAmount().min(line.getAmount());
                    }
                }
            }
            couponDiscount = couponDiscount.min(gross);
            result.setMemberCouponId(memberCouponId);
        }
        result.setCouponDiscount(couponDiscount);

        BigDecimal afterCoupon = gross.subtract(couponDiscount);
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (pointsUsed > 0 && memberId != null) {
            Member member = requireMember(memberId);
            if (pointsUsed > member.getPoints()) {
                throw new BizException("积分不足");
            }
            BigDecimal maxDiscount = afterCoupon.multiply(new BigDecimal("0.5"));
            pointsDiscount = BigDecimal.valueOf(pointsUsed).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
                    .min(maxDiscount);
            result.setPointsUsed(pointsUsed);
        }
        result.setPointsDiscount(pointsDiscount);

        BigDecimal freight = BigDecimal.ZERO;
        if (delivery) {
            DeliveryRule rule = deliveryRuleMapper.selectOne(new LambdaQueryWrapper<DeliveryRule>()
                    .eq(DeliveryRule::getStoreId, storeId).eq(DeliveryRule::getEnabled, 1).last("limit 1"));
            BigDecimal freeThreshold = rule == null ? new BigDecimal("49") : rule.getFreeThreshold();
            BigDecimal fee = rule == null ? new BigDecimal("3") : rule.getDeliveryFee();
            BigDecimal minOrder = rule == null ? new BigDecimal("20") : rule.getMinOrderAmount();
            if (afterCoupon.compareTo(minOrder) < 0) {
                throw new BizException("未达到起送金额 " + minOrder + " 元");
            }
            if (afterCoupon.compareTo(freeThreshold) < 0) {
                freight = fee;
            }
        }
        result.setFreightAmount(freight);
        BigDecimal payable = afterCoupon.subtract(pointsDiscount).add(freight);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }
        result.setPayableAmount(payable);
        return result;
    }

    private SalesOrder buildOrder(Long storeId, Long memberId, String channel, String deliveryType,
                                  List<PosOrderRequest.Item> items, Long memberCouponId, Integer pointsUsed,
                                  boolean delivery, String deliveryName, String deliveryPhone,
                                  String deliveryAddress, BigDecimal lng, BigDecimal lat, String remark) {
        OrderPriceResult price = price(storeId, memberId, channel, items, memberCouponId,
                pointsUsed == null ? 0 : pointsUsed, delivery);
        SalesOrder order = new SalesOrder();
        order.setOrderNo(noGenerator.next("POS".equals(channel) ? "POS" : "ON"));
        order.setChannel(channel);
        order.setStoreId(storeId);
        order.setMemberId(memberId);
        order.setDeliveryType(deliveryType);
        order.setTotalAmount(price.getGrossAmount());
        order.setDiscountAmount(price.getCouponDiscount().add(price.getPointsDiscount()));
        order.setFreightAmount(price.getFreightAmount());
        order.setPayableAmount(price.getPayableAmount());
        order.setPaidAmount(BigDecimal.ZERO);
        order.setWalletPaid(BigDecimal.ZERO);
        order.setMemberCouponId(price.getMemberCouponId());
        order.setPointsUsed(price.getPointsUsed());
        order.setPointsDiscount(price.getPointsDiscount());
        order.setDeliveryName(deliveryName);
        order.setDeliveryPhone(deliveryPhone);
        order.setDeliveryAddress(deliveryAddress);
        order.setRemark(remark);
        orderMapper.insert(order);
        for (OrderPriceResult.Line line : price.getLines()) {
            SalesOrderItem item = new SalesOrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(line.getSkuId());
            item.setSkuName(line.getSkuName());
            item.setBarcode(line.getBarcode());
            item.setPrice(line.getPrice());
            item.setQty(line.getQty());
            item.setAmount(line.getAmount());
            itemMapper.insert(item);
        }
        if (price.getMemberCouponId() != null) {
            MemberCoupon memberCoupon = memberCouponMapper.selectById(price.getMemberCouponId());
            memberCoupon.setStatus("USED");
            memberCoupon.setUsedAt(LocalDateTime.now());
            memberCoupon.setOrderId(order.getId());
            memberCouponMapper.updateById(memberCoupon);
        }
        if (price.getPointsUsed() != null && price.getPointsUsed() > 0 && memberId != null) {
            Member member = requireMember(memberId);
            member.setPoints(member.getPoints() - price.getPointsUsed());
            memberMapper.updateById(member);
            pointFlow(memberId, -price.getPointsUsed(), member.getPoints(), "ORDER", order.getId(), "下单抵扣");
        }
        return order;
    }

    private OrderPriceResult priceResultOf(SalesOrder order) {
        List<SalesOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, order.getId()));
        OrderPriceResult result = new OrderPriceResult();
        result.setLines(items.stream().map(item -> {
            OrderPriceResult.Line line = new OrderPriceResult.Line();
            line.setSkuId(item.getSkuId());
            line.setSkuName(item.getSkuName());
            line.setBarcode(item.getBarcode());
            line.setPrice(item.getPrice());
            line.setQty(item.getQty());
            line.setAmount(item.getAmount());
            return line;
        }).toList());
        return result;
    }

    private Map<Long, BigDecimal> activePromotionPrices() {
        List<Promotion> promotions = promotionMapper.selectList(new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, "ACTIVE")
                .le(Promotion::getStartAt, LocalDateTime.now())
                .ge(Promotion::getEndAt, LocalDateTime.now()));
        if (promotions.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> prices = new HashMap<>();
        List<PromotionItem> items = promotionItemMapper.selectList(new LambdaQueryWrapper<PromotionItem>()
                .in(PromotionItem::getPromotionId, promotions.stream().map(Promotion::getId).toList()));
        for (PromotionItem item : items) {
            if (item.getPromoPrice() == null) {
                continue;
            }
            prices.merge(item.getSkuId(), item.getPromoPrice(), BigDecimal::min);
        }
        return prices;
    }

    private void restoreBenefits(SalesOrder order) {
        if (order.getMemberCouponId() != null) {
            MemberCoupon memberCoupon = memberCouponMapper.selectById(order.getMemberCouponId());
            if (memberCoupon != null && order.getId().equals(memberCoupon.getOrderId())) {
                memberCoupon.setStatus("UNUSED");
                memberCoupon.setOrderId(null);
                memberCoupon.setUsedAt(null);
                memberCouponMapper.updateById(memberCoupon);
            }
        }
        if (order.getMemberId() != null && order.getPointsUsed() != null && order.getPointsUsed() > 0) {
            Member member = memberMapper.selectById(order.getMemberId());
            if (member != null) {
                member.setPoints(member.getPoints() + order.getPointsUsed());
                memberMapper.updateById(member);
                pointFlow(member.getId(), order.getPointsUsed(), member.getPoints(), "REFUND", order.getId(), "订单取消退回积分");
            }
        }
    }

    private void addMemberConsume(Long memberId, BigDecimal amount) {
        if (memberId == null || amount == null) {
            return;
        }
        Member member = memberMapper.selectById(memberId);
        if (member != null) {
            member.setTotalConsume(member.getTotalConsume().add(amount));
            member.setOrderCount((member.getOrderCount() == null ? 0 : member.getOrderCount()) + 1);
            member.setLastActiveAt(LocalDateTime.now());
            memberMapper.updateById(member);
        }
    }

    private void earnPoints(Long memberId, BigDecimal amount, Long orderId) {
        if (memberId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return;
        }
        double rate = configService.getDouble(member.getTenantId(), "member.point_rate", 1);
        int earned = amount.multiply(BigDecimal.valueOf(rate)).setScale(0, RoundingMode.DOWN).intValue();
        if (earned <= 0) {
            return;
        }
        member.setPoints(member.getPoints() + earned);
        memberMapper.updateById(member);
        pointFlow(memberId, earned, member.getPoints(), "ORDER", orderId, "消费获得积分");
    }

    private void deductWallet(Member member, BigDecimal amount, String bizType, Long refId, String remark) {
        if (member.getWalletBalance().compareTo(amount) < 0) {
            throw new BizException("储值余额不足");
        }
        member.setWalletBalance(member.getWalletBalance().subtract(amount));
        memberMapper.updateById(member);
        MemberWalletFlow flow = new MemberWalletFlow();
        flow.setMemberId(member.getId());
        flow.setChangeAmount(amount.negate());
        flow.setBalance(member.getWalletBalance());
        flow.setBizType(bizType);
        flow.setRefId(refId);
        flow.setRemark(remark);
        walletFlowMapper.insert(flow);
    }

    private void refundWallet(Long memberId, BigDecimal amount, Long orderId, String remark) {
        if (memberId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return;
        }
        member.setWalletBalance(member.getWalletBalance().add(amount));
        memberMapper.updateById(member);
        MemberWalletFlow flow = new MemberWalletFlow();
        flow.setMemberId(memberId);
        flow.setChangeAmount(amount);
        flow.setBalance(member.getWalletBalance());
        flow.setBizType("REFUND");
        flow.setRefId(orderId);
        flow.setRemark(remark);
        walletFlowMapper.insert(flow);
    }

    private void pointFlow(Long memberId, int change, int balance, String bizType, Long refId, String remark) {
        MemberPointFlow flow = new MemberPointFlow();
        flow.setMemberId(memberId);
        flow.setChangePoints(change);
        flow.setBalance(balance);
        flow.setBizType(bizType);
        flow.setRefId(refId);
        flow.setRemark(remark);
        pointFlowMapper.insert(flow);
    }

    private RefundOrder createRefund(SalesOrder order, String type, BigDecimal amount, String reason, String status) {
        RefundOrder refund = new RefundOrder();
        refund.setRefundNo(noGenerator.next("RF"));
        refund.setOrderId(order.getId());
        refund.setStoreId(order.getStoreId());
        refund.setType(type);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setStatus(status);
        refundMapper.insert(refund);
        return refund;
    }

    private Member requireMember(Long memberId) {
        Member member = memberId == null ? null : memberMapper.selectById(memberId);
        if (member == null) {
            throw BizException.notFound("会员不存在");
        }
        return member;
    }

    private SalesOrder require(Long orderId) {
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        return order;
    }

    private List<PosOrderRequest.Item> toLines(List<PosOrderRequest.Item> items) {
        return items == null ? List.of() : items;
    }

    private List<PosOrderRequest.Item> toLines(List<OnlineOrderRequest.Item> items, boolean unused) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(item -> {
            PosOrderRequest.Item line = new PosOrderRequest.Item();
            line.setSkuId(item.getSkuId());
            line.setQty(item.getQty());
            return line;
        }).toList();
    }
}
