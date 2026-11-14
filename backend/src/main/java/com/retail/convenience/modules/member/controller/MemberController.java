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
import com.retail.convenience.modules.member.entity.MemberLevel;
import com.retail.convenience.modules.member.entity.MemberPointFlow;
import com.retail.convenience.modules.member.entity.MemberTag;
import com.retail.convenience.modules.member.entity.MemberWalletFlow;
import com.retail.convenience.modules.member.mapper.CouponMapper;
import com.retail.convenience.modules.member.mapper.MemberCouponMapper;
import com.retail.convenience.modules.member.mapper.MemberLevelMapper;
import com.retail.convenience.modules.member.mapper.MemberMapper;
import com.retail.convenience.modules.member.mapper.MemberPointFlowMapper;
import com.retail.convenience.modules.member.mapper.MemberTagMapper;
import com.retail.convenience.modules.member.mapper.MemberWalletFlowMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/members")
public class MemberController {

    private final MemberMapper memberMapper;
    private final MemberLevelMapper levelMapper;
    private final MemberPointFlowMapper pointFlowMapper;
    private final MemberWalletFlowMapper walletFlowMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final CouponMapper couponMapper;
    private final MemberTagMapper tagMapper;

    public MemberController(MemberMapper memberMapper, MemberLevelMapper levelMapper,
                            MemberPointFlowMapper pointFlowMapper, MemberWalletFlowMapper walletFlowMapper,
                            MemberCouponMapper memberCouponMapper, CouponMapper couponMapper,
                            MemberTagMapper tagMapper) {
        this.memberMapper = memberMapper;
        this.levelMapper = levelMapper;
        this.pointFlowMapper = pointFlowMapper;
        this.walletFlowMapper = walletFlowMapper;
        this.memberCouponMapper = memberCouponMapper;
        this.couponMapper = couponMapper;
        this.tagMapper = tagMapper;
    }

    @GetMapping
    @RequirePerm("member:view")
    public ApiResult<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) String status) {
        Page<Member> result = memberMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Member>()
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Member::getNickname, keyword)
                                .or().like(Member::getPhoneMask, keyword))
                        .eq(levelId != null, Member::getLevelId, levelId)
                        .eq(status != null && !status.isBlank(), Member::getStatus, status)
                        .orderByDesc(Member::getRegisteredAt));
        Map<Long, MemberLevel> levels = levelMapper.selectList(null).stream()
                .collect(Collectors.toMap(MemberLevel::getId, Function.identity()));
        var rows = result.getRecords().stream().map(member -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("member", member);
            MemberLevel level = member.getLevelId() == null ? null : levels.get(member.getLevelId());
            row.put("levelName", level == null ? "普通会员" : level.getName());
            return row;
        }).toList();
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @GetMapping("/levels")
    @RequirePerm("member:view")
    public ApiResult<List<MemberLevel>> levels() {
        return ApiResult.ok(levelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .orderByAsc(MemberLevel::getMinPoints)));
    }

    @GetMapping("/tags")
    @RequirePerm("member:view")
    public ApiResult<List<MemberTag>> tags() {
        return ApiResult.ok(tagMapper.selectList(null));
    }

    @GetMapping("/{id}")
    @RequirePerm("member:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Member member = require(id);
        List<MemberPointFlow> points = pointFlowMapper.selectList(new LambdaQueryWrapper<MemberPointFlow>()
                .eq(MemberPointFlow::getMemberId, id).orderByDesc(MemberPointFlow::getCreatedAt).last("limit 50"));
        List<MemberWalletFlow> wallet = walletFlowMapper.selectList(new LambdaQueryWrapper<MemberWalletFlow>()
                .eq(MemberWalletFlow::getMemberId, id).orderByDesc(MemberWalletFlow::getCreatedAt).last("limit 50"));
        List<MemberCoupon> memberCoupons = memberCouponMapper.selectList(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getMemberId, id).orderByDesc(MemberCoupon::getReceivedAt));
        List<Long> couponIds = memberCoupons.stream().map(MemberCoupon::getCouponId).toList();
        Map<Long, Coupon> coupons = couponIds.isEmpty() ? Map.of() : couponMapper.selectList(
                        new LambdaQueryWrapper<Coupon>().in(Coupon::getId, couponIds))
                .stream().collect(Collectors.toMap(Coupon::getId, Function.identity()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("member", member);
        map.put("pointFlows", points);
        map.put("walletFlows", wallet);
        map.put("coupons", memberCoupons.stream().map(mc -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("memberCoupon", mc);
            Coupon coupon = coupons.get(mc.getCouponId());
            row.put("couponName", coupon == null ? "-" : coupon.getName());
            row.put("type", coupon == null ? "" : coupon.getType());
            return row;
        }).toList());
        return ApiResult.ok(map);
    }

    @PutMapping("/{id}")
    @RequirePerm("member:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Member request) {
        Member member = require(id);
        member.setNickname(request.getNickname());
        member.setLevelId(request.getLevelId());
        member.setBirthday(request.getBirthday());
        member.setTags(request.getTags());
        member.setStatus(request.getStatus());
        memberMapper.updateById(member);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/points")
    @RequirePerm("member:update")
    @Transactional
    public ApiResult<Void> adjustPoints(@PathVariable Long id, @RequestParam int points,
                                        @RequestParam(required = false) String remark) {
        Member member = require(id);
        int newBalance = member.getPoints() + points;
        if (newBalance < 0) {
            throw new BizException("积分不足");
        }
        member.setPoints(newBalance);
        memberMapper.updateById(member);
        MemberPointFlow flow = new MemberPointFlow();
        flow.setMemberId(id);
        flow.setChangePoints(points);
        flow.setBalance(newBalance);
        flow.setBizType("ADMIN");
        flow.setRemark(remark == null ? "后台调整积分" : remark);
        pointFlowMapper.insert(flow);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/recharge")
    @RequirePerm("member:update")
    @Transactional
    public ApiResult<Void> recharge(@PathVariable Long id, @RequestParam BigDecimal amount,
                                    @RequestParam(required = false) BigDecimal giftAmount,
                                    @RequestParam(required = false) String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("充值金额必须大于 0");
        }
        Member member = require(id);
        BigDecimal gift = giftAmount == null ? BigDecimal.ZERO : giftAmount;
        member.setWalletBalance(member.getWalletBalance().add(amount).add(gift));
        memberMapper.updateById(member);
        MemberWalletFlow flow = new MemberWalletFlow();
        flow.setMemberId(id);
        flow.setChangeAmount(amount);
        flow.setBalance(member.getWalletBalance());
        flow.setBizType("RECHARGE");
        flow.setRemark(remark == null ? "后台充值" : remark);
        walletFlowMapper.insert(flow);
        if (gift.compareTo(BigDecimal.ZERO) > 0) {
            MemberWalletFlow giftFlow = new MemberWalletFlow();
            giftFlow.setMemberId(id);
            giftFlow.setChangeAmount(gift);
            giftFlow.setBalance(member.getWalletBalance());
            giftFlow.setBizType("GIFT");
            giftFlow.setRemark("充值赠送");
            walletFlowMapper.insert(giftFlow);
        }
        return ApiResult.ok();
    }

    private Member require(Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw BizException.notFound("会员不存在");
        }
        return member;
    }
}
