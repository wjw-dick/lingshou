package com.retail.convenience.modules.franchise.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.franchise.entity.FranchiseCommission;
import com.retail.convenience.modules.franchise.entity.Franchisee;
import com.retail.convenience.modules.franchise.entity.FranchiseeQualification;
import com.retail.convenience.modules.franchise.entity.FranchiseeStore;
import com.retail.convenience.modules.franchise.mapper.FranchiseCommissionMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeQualificationMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeStoreMapper;
import com.retail.convenience.modules.product.entity.ProductSku;
import com.retail.convenience.modules.product.mapper.ProductSkuMapper;
import com.retail.convenience.modules.purchase.entity.SupplyOrder;
import com.retail.convenience.modules.purchase.entity.SupplyOrderItem;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderItemMapper;
import com.retail.convenience.modules.purchase.mapper.SupplyOrderMapper;
import com.retail.convenience.modules.settlement.entity.SettlementStatement;
import com.retail.convenience.modules.settlement.mapper.SettlementStatementMapper;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/franchisee")
public class FranchiseePortalController {

    private final FranchiseeMapper franchiseeMapper;
    private final FranchiseeStoreMapper storeRelMapper;
    private final FranchiseeQualificationMapper qualificationMapper;
    private final FranchiseCommissionMapper commissionMapper;
    private final FranchiseeStoreMapper franchiseeStoreMapper;
    private final StoreMapper storeMapper;
    private final SupplyOrderMapper supplyMapper;
    private final SupplyOrderItemMapper supplyItemMapper;
    private final ProductSkuMapper skuMapper;
    private final SettlementStatementMapper statementMapper;
    private final JdbcTemplate jdbc;
    private final NoGenerator noGenerator;

    public FranchiseePortalController(FranchiseeMapper franchiseeMapper,
                                      FranchiseeStoreMapper storeRelMapper,
                                      FranchiseeQualificationMapper qualificationMapper,
                                      FranchiseCommissionMapper commissionMapper,
                                      FranchiseeStoreMapper franchiseeStoreMapper,
                                      StoreMapper storeMapper, SupplyOrderMapper supplyMapper,
                                      SupplyOrderItemMapper supplyItemMapper, ProductSkuMapper skuMapper,
                                      SettlementStatementMapper statementMapper, JdbcTemplate jdbc,
                                      NoGenerator noGenerator) {
        this.franchiseeMapper = franchiseeMapper;
        this.storeRelMapper = storeRelMapper;
        this.qualificationMapper = qualificationMapper;
        this.commissionMapper = commissionMapper;
        this.franchiseeStoreMapper = franchiseeStoreMapper;
        this.storeMapper = storeMapper;
        this.supplyMapper = supplyMapper;
        this.supplyItemMapper = supplyItemMapper;
        this.skuMapper = skuMapper;
        this.statementMapper = statementMapper;
        this.jdbc = jdbc;
        this.noGenerator = noGenerator;
    }

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile() {
        Franchisee franchisee = current();
        return ApiResult.ok(Map.of("franchisee", franchisee,
                "qualifications", qualificationMapper.selectList(new LambdaQueryWrapper<FranchiseeQualification>()
                        .eq(FranchiseeQualification::getFranchiseeId, franchisee.getId()))));
    }

    @GetMapping("/stores")
    public ApiResult<List<Map<String, Object>>> stores() {
        Franchisee franchisee = current();
        List<FranchiseeStore> relations = storeRelMapper.selectList(new LambdaQueryWrapper<FranchiseeStore>()
                .eq(FranchiseeStore::getFranchiseeId, franchisee.getId()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FranchiseeStore relation : relations) {
            Store store = storeMapper.selectById(relation.getStoreId());
            if (store == null) {
                continue;
            }
            BigDecimal sales = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(paid_amount),0) FROM sales_order WHERE tenant_id = ? AND store_id = ? "
                            + "AND pay_status='PAID' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                    BigDecimal.class, franchisee.getTenantId(), store.getId());
            Integer members = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM member WHERE tenant_id = ?", Integer.class, franchisee.getTenantId());
            Integer stockQty = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(qty),0) FROM stock WHERE tenant_id = ? AND owner_type='STORE' AND owner_id = ?",
                    Integer.class, franchisee.getTenantId(), store.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("store", store);
            row.put("relation", relation);
            row.put("sales30d", sales);
            row.put("stockQty", stockQty);
            row.put("memberCount", members);
            rows.add(row);
        }
        return ApiResult.ok(rows);
    }

    @GetMapping("/supply-orders")
    public ApiResult<PageResult<Map<String, Object>>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Franchisee franchisee = current();
        Page<SupplyOrder> result = supplyMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SupplyOrder>()
                        .eq(SupplyOrder::getFranchiseeId, franchisee.getId())
                        .eq(status != null && !status.isBlank(), SupplyOrder::getStatus, status)
                        .orderByDesc(SupplyOrder::getCreatedAt));
        Map<Long, List<SupplyOrderItem>> items = supplyItemMapper.selectList(
                        new LambdaQueryWrapper<SupplyOrderItem>().in(SupplyOrderItem::getSupplyId,
                                result.getRecords().stream().map(SupplyOrder::getId).toList()))
                .stream().collect(java.util.stream.Collectors.groupingBy(SupplyOrderItem::getSupplyId));
        List<Map<String, Object>> rows = result.getRecords().stream().map(order -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", order);
            row.put("items", items.getOrDefault(order.getId(), List.of()));
            return row;
        }).toList();
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @PostMapping("/supply-orders")
    @Transactional
    public ApiResult<SupplyOrder> createOrder(@RequestBody CreateOrderRequest request) {
        Franchisee franchisee = current();
        if (request.getStoreId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("请选择门店并添加商品");
        }
        if (storeRelMapper.selectCount(new LambdaQueryWrapper<FranchiseeStore>()
                .eq(FranchiseeStore::getFranchiseeId, franchisee.getId())
                .eq(FranchiseeStore::getStoreId, request.getStoreId())) == 0) {
            throw BizException.forbidden("无权向该门店报单");
        }
        SupplyOrder order = new SupplyOrder();
        order.setOrderNo(noGenerator.next("SO"));
        order.setSourceType("FRANCHISE");
        order.setStoreId(request.getStoreId());
        order.setFranchiseeId(franchisee.getId());
        order.setStatus("PENDING");
        order.setExpectedAt(request.getExpectedAt());
        order.setRemark(request.getRemark());
        order.setTotalAmount(BigDecimal.ZERO);
        supplyMapper.insert(order);
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.Item req : request.getItems()) {
            ProductSku sku = skuMapper.selectById(req.getSkuId());
            if (sku == null || req.getQty() == null || req.getQty() <= 0) {
                continue;
            }
            SupplyOrderItem item = new SupplyOrderItem();
            item.setSupplyId(order.getId());
            item.setSkuId(sku.getId());
            item.setQty(req.getQty());
            item.setShippedQty(0);
            item.setReceivedQty(0);
            item.setPrice(sku.getPriceFranchise());
            item.setAmount(sku.getPriceFranchise().multiply(BigDecimal.valueOf(req.getQty())));
            supplyItemMapper.insert(item);
            total = total.add(item.getAmount());
        }
        if (franchisee.getCreditLimit() != null
                && franchisee.getUsedCredit().add(total).compareTo(franchisee.getCreditLimit()) > 0) {
            throw new BizException("授信额度不足，可用额度 "
                    + franchisee.getCreditLimit().subtract(franchisee.getUsedCredit()) + " 元");
        }
        order.setTotalAmount(total);
        supplyMapper.updateById(order);
        franchisee.setUsedCredit(franchisee.getUsedCredit().add(total));
        franchiseeMapper.updateById(franchisee);
        return ApiResult.ok(order);
    }

    @GetMapping("/commissions")
    public ApiResult<List<FranchiseCommission>> commissions() {
        Franchisee franchisee = current();
        return ApiResult.ok(commissionMapper.selectList(new LambdaQueryWrapper<FranchiseCommission>()
                .eq(FranchiseCommission::getFranchiseeId, franchisee.getId())
                .orderByDesc(FranchiseCommission::getCreatedAt)));
    }

    @GetMapping("/statements")
    public ApiResult<List<SettlementStatement>> statements() {
        Franchisee franchisee = current();
        return ApiResult.ok(statementMapper.selectList(new LambdaQueryWrapper<SettlementStatement>()
                .eq(SettlementStatement::getType, "FRANCHISE")
                .eq(SettlementStatement::getPartnerId, franchisee.getId())
                .orderByDesc(SettlementStatement::getCreatedAt)));
    }

    @PostMapping("/statements/{id}/confirm")
    public ApiResult<Void> confirmStatement(@PathVariable Long id) {
        Franchisee franchisee = current();
        SettlementStatement statement = statementMapper.selectById(id);
        if (statement == null || !franchisee.getId().equals(statement.getPartnerId())) {
            throw BizException.notFound("对账单不存在");
        }
        statement.setStatus("CONFIRMED");
        statement.setConfirmedAt(java.time.LocalDateTime.now());
        statementMapper.updateById(statement);
        return ApiResult.ok();
    }

    @PostMapping("/qualifications")
    public ApiResult<Void> addQualification(@RequestBody FranchiseeQualification qualification) {
        Franchisee franchisee = current();
        qualification.setId(null);
        qualification.setFranchiseeId(franchisee.getId());
        qualificationMapper.insert(qualification);
        return ApiResult.ok();
    }

    @GetMapping("/messages")
    public ApiResult<Map<String, Object>> messages() {
        Franchisee franchisee = current();
        long pendingOrders = supplyMapper.selectCount(new LambdaQueryWrapper<SupplyOrder>()
                .eq(SupplyOrder::getFranchiseeId, franchisee.getId())
                .eq(SupplyOrder::getStatus, "PENDING"));
        long pendingStatements = statementMapper.selectCount(new LambdaQueryWrapper<SettlementStatement>()
                .eq(SettlementStatement::getType, "FRANCHISE")
                .eq(SettlementStatement::getPartnerId, franchisee.getId())
                .eq(SettlementStatement::getStatus, "PENDING"));
        return ApiResult.ok(Map.of("pendingOrders", pendingOrders, "pendingStatements", pendingStatements,
                "creditLimit", franchisee.getCreditLimit(), "usedCredit", franchisee.getUsedCredit()));
    }

    private Franchisee current() {
        LoginUser user = AuthContext.require();
        if (user.getFranchiseeId() == null) {
            throw BizException.forbidden("当前账号未绑定加盟商");
        }
        Franchisee franchisee = franchiseeMapper.selectById(user.getFranchiseeId());
        if (franchisee == null) {
            throw BizException.notFound("加盟商不存在");
        }
        return franchisee;
    }

    @lombok.Data
    public static class CreateOrderRequest {
        private Long storeId;
        private java.time.LocalDate expectedAt;
        private String remark;
        private List<Item> items;

        @lombok.Data
        public static class Item {
            private Long skuId;
            private Integer qty;
        }
    }
}
