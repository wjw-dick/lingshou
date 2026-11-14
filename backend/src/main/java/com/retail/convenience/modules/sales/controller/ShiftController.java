package com.retail.convenience.modules.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.sales.entity.PosShift;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.mapper.PosShiftMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/shifts")
public class ShiftController {

    private final PosShiftMapper shiftMapper;
    private final SalesOrderMapper orderMapper;
    private final NoGenerator noGenerator;

    public ShiftController(PosShiftMapper shiftMapper, SalesOrderMapper orderMapper, NoGenerator noGenerator) {
        this.shiftMapper = shiftMapper;
        this.orderMapper = orderMapper;
        this.noGenerator = noGenerator;
    }

    @GetMapping
    @RequirePerm("sales:view")
    public ApiResult<PageResult<PosShift>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status) {
        Page<PosShift> result = shiftMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PosShift>()
                        .eq(storeId != null, PosShift::getStoreId, storeId)
                        .eq(status != null && !status.isBlank(), PosShift::getStatus, status)
                        .orderByDesc(PosShift::getStartAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/current")
    @RequirePerm("sales:view")
    public ApiResult<PosShift> current(@RequestParam Long storeId) {
        PosShift shift = shiftMapper.selectOne(new LambdaQueryWrapper<PosShift>()
                .eq(PosShift::getStoreId, storeId)
                .eq(PosShift::getStatus, "OPEN")
                .orderByDesc(PosShift::getStartAt)
                .last("limit 1"));
        return ApiResult.ok(shift);
    }

    @PostMapping("/open")
    @RequirePerm("sales:shift")
    public ApiResult<PosShift> open(@RequestParam Long storeId,
                                    @RequestParam(required = false) BigDecimal openingCash) {
        PosShift exists = shiftMapper.selectOne(new LambdaQueryWrapper<PosShift>()
                .eq(PosShift::getStoreId, storeId).eq(PosShift::getStatus, "OPEN").last("limit 1"));
        if (exists != null) {
            throw new BizException("当前门店已有开启中的交接班");
        }
        PosShift shift = new PosShift();
        shift.setShiftNo(noGenerator.next("SH"));
        shift.setStoreId(storeId);
        shift.setCashierId(AuthContext.require().getUserId());
        shift.setCashierName(AuthContext.require().getRealName());
        shift.setStartAt(LocalDateTime.now());
        shift.setOpeningCash(openingCash == null ? BigDecimal.ZERO : openingCash);
        shift.setStatus("OPEN");
        shiftMapper.insert(shift);
        return ApiResult.ok(shift);
    }

    @PostMapping("/{id}/close")
    @RequirePerm("sales:shift")
    public ApiResult<PosShift> close(@PathVariable Long id, @RequestBody CloseRequest request) {
        PosShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw BizException.notFound("交接班不存在");
        }
        if (!"OPEN".equals(shift.getStatus())) {
            throw new BizException("交接班已关闭");
        }
        LocalDateTime end = LocalDateTime.now();
        List<SalesOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getStoreId, shift.getStoreId())
                .eq(SalesOrder::getChannel, "POS")
                .eq(SalesOrder::getPayStatus, "PAID")
                .ge(SalesOrder::getPaidAt, shift.getStartAt())
                .le(SalesOrder::getPaidAt, end));
        BigDecimal cash = sum(orders, "CASH");
        BigDecimal wechat = sum(orders, "WECHAT");
        BigDecimal alipay = sum(orders, "ALIPAY");
        BigDecimal wallet = sum(orders, "WALLET");
        shift.setEndAt(end);
        shift.setCashSales(cash);
        shift.setWechatSales(wechat);
        shift.setAlipaySales(alipay);
        shift.setWalletSales(wallet);
        BigDecimal expected = shift.getOpeningCash().add(cash);
        shift.setExpectedCash(expected);
        BigDecimal actual = request.getActualCash() == null ? expected : request.getActualCash();
        shift.setActualCash(actual);
        shift.setDiffAmount(actual.subtract(expected));
        shift.setDiffReason(request.getDiffReason());
        shift.setStatus("CLOSED");
        shiftMapper.updateById(shift);
        return ApiResult.ok(shift);
    }

    private BigDecimal sum(List<SalesOrder> orders, String method) {
        return orders.stream()
                .filter(o -> method.equals(o.getPaymentMethod()))
                .map(SalesOrder::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @lombok.Data
    public static class CloseRequest {
        private BigDecimal actualCash;
        private String diffReason;
    }
}
