package com.retail.convenience.modules.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.member.entity.Member;
import com.retail.convenience.modules.member.mapper.MemberMapper;
import com.retail.convenience.modules.sales.dto.OrderPriceResult;
import com.retail.convenience.modules.sales.entity.OrderEvaluation;
import com.retail.convenience.modules.sales.entity.RefundOrder;
import com.retail.convenience.modules.sales.entity.SalesOrder;
import com.retail.convenience.modules.sales.entity.SalesOrderItem;
import com.retail.convenience.modules.sales.mapper.OrderEvaluationMapper;
import com.retail.convenience.modules.sales.mapper.RefundOrderMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderItemMapper;
import com.retail.convenience.modules.sales.mapper.SalesOrderMapper;
import com.retail.convenience.modules.sales.service.SalesOrderService;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/v1/tenant/orders")
public class OrderController {

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final RefundOrderMapper refundMapper;
    private final OrderEvaluationMapper evaluationMapper;
    private final StoreMapper storeMapper;
    private final MemberMapper memberMapper;
    private final SalesOrderService orderService;

    public OrderController(SalesOrderMapper orderMapper, SalesOrderItemMapper itemMapper,
                           RefundOrderMapper refundMapper, OrderEvaluationMapper evaluationMapper,
                           StoreMapper storeMapper, MemberMapper memberMapper,
                           SalesOrderService orderService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.refundMapper = refundMapper;
        this.evaluationMapper = evaluationMapper;
        this.storeMapper = storeMapper;
        this.memberMapper = memberMapper;
        this.orderService = orderService;
    }

    @GetMapping
    @RequirePerm("sales:view")
    public ApiResult<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Page<SalesOrder> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(storeId != null, SalesOrder::getStoreId, storeId)
                        .eq(channel != null && !channel.isBlank(), SalesOrder::getChannel, channel)
                        .eq(status != null && !status.isBlank(), SalesOrder::getStatus, status)
                        .eq(memberId != null, SalesOrder::getMemberId, memberId)
                        .like(keyword != null && !keyword.isBlank(), SalesOrder::getOrderNo, keyword)
                        .ge(start != null, SalesOrder::getCreatedAt, start)
                        .le(end != null, SalesOrder::getCreatedAt, end)
                        .orderByDesc(SalesOrder::getCreatedAt));
        Map<Long, Store> stores = storeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        Map<Long, Member> members = memberMapper.selectList(null).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalesOrder order : result.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", order);
            Store store = stores.get(order.getStoreId());
            row.put("storeName", store == null ? "-" : store.getName());
            Member member = order.getMemberId() == null ? null : members.get(order.getMemberId());
            row.put("memberName", member == null ? "散客" : member.getNickname());
            rows.add(row);
        }
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @GetMapping("/{id}")
    @RequirePerm("sales:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        List<SalesOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, id));
        List<RefundOrder> refunds = refundMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getOrderId, id));
        OrderEvaluation evaluation = evaluationMapper.selectOne(new LambdaQueryWrapper<OrderEvaluation>()
                .eq(OrderEvaluation::getOrderId, id).last("limit 1"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("order", order);
        map.put("items", items);
        map.put("refunds", refunds);
        map.put("evaluation", evaluation);
        return ApiResult.ok(map);
    }

    @PostMapping("/{id}/accept")
    @RequirePerm("sales:update")
    public ApiResult<Void> accept(@PathVariable Long id) {
        orderService.accept(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/pick-complete")
    @RequirePerm("sales:update")
    public ApiResult<Void> pickComplete(@PathVariable Long id) {
        orderService.pickComplete(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/complete")
    @RequirePerm("sales:update")
    public ApiResult<Void> complete(@PathVariable Long id) {
        orderService.complete(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/cancel")
    @RequirePerm("sales:update")
    public ApiResult<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        orderService.cancel(id, reason);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/refund")
    @RequirePerm("sales:refund")
    public ApiResult<RefundOrder> refund(@PathVariable Long id,
                                         @RequestParam(required = false) String reason,
                                         @RequestParam(required = false) java.math.BigDecimal amount) {
        return ApiResult.ok(orderService.refund(id, reason, amount));
    }

    @GetMapping("/refunds")
    @RequirePerm("sales:view")
    public ApiResult<PageResult<RefundOrder>> refunds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<RefundOrder> result = refundMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<RefundOrder>()
                        .eq(status != null && !status.isBlank(), RefundOrder::getStatus, status)
                        .orderByDesc(RefundOrder::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/{id}/evaluation")
    @RequirePerm("sales:view")
    public ApiResult<Void> evaluate(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestBody
                                    OrderEvaluation evaluation) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        evaluation.setId(null);
        evaluation.setOrderId(id);
        evaluation.setTenantId(AuthContext.require().getTenantId());
        if (evaluation.getMemberId() == null) {
            evaluation.setMemberId(order.getMemberId());
        }
        evaluation.setStoreId(order.getStoreId());
        evaluationMapper.insert(evaluation);
        return ApiResult.ok();
    }
}
