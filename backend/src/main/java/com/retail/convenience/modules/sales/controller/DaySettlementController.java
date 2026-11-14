package com.retail.convenience.modules.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.modules.sales.entity.DaySettlement;
import com.retail.convenience.modules.sales.entity.RefundOrder;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.mapper.DaySettlementMapper;
import com.retail.convenience.modules.sales.mapper.RefundOrderMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/day-settlements")
public class DaySettlementController {

    private final DaySettlementMapper settlementMapper;
    private final SalesOrderMapper orderMapper;
    private final RefundOrderMapper refundMapper;

    public DaySettlementController(DaySettlementMapper settlementMapper, SalesOrderMapper orderMapper,
                                   RefundOrderMapper refundMapper) {
        this.settlementMapper = settlementMapper;
        this.orderMapper = orderMapper;
        this.refundMapper = refundMapper;
    }

    @GetMapping
    @RequirePerm("sales:view")
    public ApiResult<PageResult<DaySettlement>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId) {
        Page<DaySettlement> result = settlementMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<DaySettlement>()
                        .eq(storeId != null, DaySettlement::getStoreId, storeId)
                        .orderByDesc(DaySettlement::getSettleDate));
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/generate")
    @RequirePerm("sales:settle")
    public ApiResult<DaySettlement> generate(@RequestParam Long storeId, @RequestParam String date) {
        LocalDate settleDate = LocalDate.parse(date);
        LocalDateTime start = settleDate.atStartOfDay();
        LocalDateTime end = settleDate.plusDays(1).atStartOfDay();
        List<SalesOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getStoreId, storeId)
                .eq(SalesOrder::getPayStatus, "PAID")
                .ge(SalesOrder::getPaidAt, start).lt(SalesOrder::getPaidAt, end));
        List<RefundOrder> refunds = refundMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getStoreId, storeId)
                .ge(RefundOrder::getCreatedAt, start).lt(RefundOrder::getCreatedAt, end));
        DaySettlement settlement = settlementMapper.selectOne(new LambdaQueryWrapper<DaySettlement>()
                .eq(DaySettlement::getStoreId, storeId)
                .eq(DaySettlement::getSettleDate, settleDate).last("limit 1"));
        if (settlement == null) {
            settlement = new DaySettlement();
            settlement.setStoreId(storeId);
            settlement.setSettleDate(settleDate);
            settlement.setStatus("DRAFT");
        }
        settlement.setOrderCount(orders.size());
        settlement.setSalesAmount(sum(orders));
        settlement.setRefundAmount(refunds.stream().map(RefundOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        settlement.setNetAmount(settlement.getSalesAmount().subtract(settlement.getRefundAmount()));
        settlement.setCashAmount(sumMethod(orders, "CASH"));
        settlement.setWechatAmount(sumMethod(orders, "WECHAT"));
        settlement.setAlipayAmount(sumMethod(orders, "ALIPAY"));
        settlement.setWalletAmount(sumMethod(orders, "WALLET"));
        settlement.setDiffAmount(BigDecimal.ZERO);
        if (settlement.getId() == null) {
            settlementMapper.insert(settlement);
        } else {
            settlementMapper.updateById(settlement);
        }
        return ApiResult.ok(settlement);
    }

    @PostMapping("/{id}/confirm")
    @RequirePerm("sales:settle")
    public ApiResult<Void> confirm(@PathVariable Long id) {
        DaySettlement settlement = settlementMapper.selectById(id);
        if (settlement == null) {
            throw BizException.notFound("日结记录不存在");
        }
        settlement.setStatus("CONFIRMED");
        settlementMapper.updateById(settlement);
        return ApiResult.ok();
    }

    private BigDecimal sum(List<SalesOrder> orders) {
        return orders.stream().map(SalesOrder::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumMethod(List<SalesOrder> orders, String method) {
        return orders.stream().filter(o -> method.equals(o.getPaymentMethod()))
                .map(SalesOrder::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
