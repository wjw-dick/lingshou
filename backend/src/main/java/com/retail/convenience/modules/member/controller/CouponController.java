package com.retail.convenience.modules.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.member.entity.Coupon;
import com.retail.convenience.modules.member.entity.Member;
import com.retail.convenience.modules.member.entity.MemberCoupon;
import com.retail.convenience.modules.member.mapper.CouponMapper;
import com.retail.convenience.modules.member.mapper.MemberCouponMapper;
import com.retail.convenience.modules.member.mapper.MemberMapper;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/coupons")
public class CouponController {

    private final CouponMapper couponMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final MemberMapper memberMapper;

    public CouponController(CouponMapper couponMapper, MemberCouponMapper memberCouponMapper,
                            MemberMapper memberMapper) {
        this.couponMapper = couponMapper;
        this.memberCouponMapper = memberCouponMapper;
        this.memberMapper = memberMapper;
    }

    @GetMapping
    @RequirePerm("member:view")
    public ApiResult<PageResult<Coupon>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        Page<Coupon> result = couponMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Coupon>()
                        .eq(status != null && !status.isBlank(), Coupon::getStatus, status)
                        .eq(type != null && !type.isBlank(), Coupon::getType, type)
                        .orderByDesc(Coupon::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping
    @RequirePerm("member:create")
    public ApiResult<Coupon> create(@RequestBody Coupon coupon) {
        validate(coupon);
        coupon.setId(null);
        coupon.setTenantId(AuthContext.require().getTenantId());
        coupon.setIssuedQty(0);
        if (coupon.getStatus() == null) {
            coupon.setStatus("ACTIVE");
        }
        couponMapper.insert(coupon);
        return ApiResult.ok(coupon);
    }

    @PutMapping("/{id}")
    @RequirePerm("member:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        validate(coupon);
        coupon.setId(id);
        couponMapper.updateById(coupon);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("member:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setStatus("DISABLED");
        couponMapper.updateById(coupon);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/issue")
    @RequirePerm("member:update")
    @Transactional
    public ApiResult<java.util.Map<String, Object>> issue(@PathVariable Long id,
                                                          @RequestBody IssueRequest request) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null || !"ACTIVE".equals(coupon.getStatus())) {
            throw new BizException("优惠券不存在或已停用");
        }
        List<Member> members;
        if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            if (!Boolean.TRUE.equals(request.getAll())) {
                throw new BizException("请选择发放会员");
            }
            members = memberMapper.selectList(new LambdaQueryWrapper<Member>()
                    .eq(Member::getStatus, "ACTIVE").last("limit 5000"));
        } else {
            members = memberMapper.selectList(new LambdaQueryWrapper<Member>()
                    .in(Member::getId, request.getMemberIds()));
        }
        int issued = 0;
        for (Member member : members) {
            long owned = memberCouponMapper.selectCount(new LambdaQueryWrapper<MemberCoupon>()
                    .eq(MemberCoupon::getMemberId, member.getId())
                    .eq(MemberCoupon::getCouponId, id));
            if (owned >= coupon.getPerUserLimit()) {
                continue;
            }
            MemberCoupon memberCoupon = new MemberCoupon();
            memberCoupon.setCouponId(id);
            memberCoupon.setMemberId(member.getId());
            memberCoupon.setStatus("UNUSED");
            memberCoupon.setReceivedAt(LocalDateTime.now());
            memberCouponMapper.insert(memberCoupon);
            issued++;
        }
        coupon.setIssuedQty((coupon.getIssuedQty() == null ? 0 : coupon.getIssuedQty()) + issued);
        couponMapper.updateById(coupon);
        return ApiResult.ok(java.util.Map.of("issued", issued));
    }

    private void validate(Coupon coupon) {
        if (coupon.getName() == null || coupon.getName().isBlank()) {
            throw new BizException("优惠券名称不能为空");
        }
        if (coupon.getType() == null) {
            throw new BizException("请选择优惠券类型");
        }
        if (coupon.getValidFrom() == null || coupon.getValidTo() == null
                || coupon.getValidTo().isBefore(coupon.getValidFrom())) {
            throw new BizException("有效期不合法");
        }
        if (("FULL_REDUCTION".equals(coupon.getType()) || "PRODUCT".equals(coupon.getType()))
                && (coupon.getDiscountAmount() == null || coupon.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            throw new BizException("请填写优惠金额");
        }
    }

    @lombok.Data
    public static class IssueRequest {
        private List<Long> memberIds;
        private Boolean all;
    }
}
