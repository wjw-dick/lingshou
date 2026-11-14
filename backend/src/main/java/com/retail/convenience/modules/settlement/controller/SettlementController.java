package com.retail.convenience.modules.settlement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.franchise.entity.FranchiseCommission;
import com.retail.convenience.modules.franchise.entity.Franchisee;
import com.retail.convenience.modules.franchise.entity.FranchiseeStore;
import com.retail.convenience.modules.franchise.mapper.FranchiseCommissionMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeStoreMapper;
import com.retail.convenience.modules.settlement.entity.SettlementInvoice;
import com.retail.convenience.modules.settlement.entity.SettlementItem;
import com.retail.convenience.modules.settlement.entity.SettlementStatement;
import com.retail.convenience.modules.settlement.mapper.SettlementInvoiceMapper;
import com.retail.convenience.modules.settlement.mapper.SettlementItemMapper;
import com.retail.convenience.modules.settlement.mapper.SettlementStatementMapper;
import com.retail.convenience.modules.supplier.entity.Supplier;
import com.retail.convenience.modules.supplier.mapper.SupplierMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/settlements")
public class SettlementController {

    private final SettlementStatementMapper statementMapper;
    private final SettlementItemMapper itemMapper;
    private final SettlementInvoiceMapper invoiceMapper;
    private final SupplierMapper supplierMapper;
    private final FranchiseeMapper franchiseeMapper;
    private final FranchiseeStoreMapper franchiseeStoreMapper;
    private final FranchiseCommissionMapper commissionMapper;
    private final JdbcTemplate jdbcTemplate;
    private final NoGenerator noGenerator;

    public SettlementController(SettlementStatementMapper statementMapper, SettlementItemMapper itemMapper,
                                SettlementInvoiceMapper invoiceMapper, SupplierMapper supplierMapper,
                                FranchiseeMapper franchiseeMapper, FranchiseeStoreMapper franchiseeStoreMapper,
                                FranchiseCommissionMapper commissionMapper, JdbcTemplate jdbcTemplate,
                                NoGenerator noGenerator) {
        this.statementMapper = statementMapper;
        this.itemMapper = itemMapper;
        this.invoiceMapper = invoiceMapper;
        this.supplierMapper = supplierMapper;
        this.franchiseeMapper = franchiseeMapper;
        this.franchiseeStoreMapper = franchiseeStoreMapper;
        this.commissionMapper = commissionMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.noGenerator = noGenerator;
    }

    @GetMapping
    @RequirePerm("settlement:view")
    public ApiResult<PageResult<SettlementStatement>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) String status) {
        Page<SettlementStatement> result = statementMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SettlementStatement>()
                        .eq(type != null && !type.isBlank(), SettlementStatement::getType, type)
                        .eq(partnerId != null, SettlementStatement::getPartnerId, partnerId)
                        .eq(status != null && !status.isBlank(), SettlementStatement::getStatus, status)
                        .orderByDesc(SettlementStatement::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("settlement:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        SettlementStatement statement = require(id);
        List<SettlementItem> items = itemMapper.selectList(new LambdaQueryWrapper<SettlementItem>()
                .eq(SettlementItem::getStatementId, id));
        List<SettlementInvoice> invoices = invoiceMapper.selectList(new LambdaQueryWrapper<SettlementInvoice>()
                .eq(SettlementInvoice::getStatementId, id));
        return ApiResult.ok(Map.of("statement", statement, "items", items, "invoices", invoices));
    }

    @PostMapping("/generate-supplier")
    @RequirePerm("settlement:create")
    @Transactional
    public ApiResult<SettlementStatement> generateSupplier(@RequestParam Long supplierId,
                                                           @RequestParam String period) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw BizException.notFound("供应商不存在");
        }
        YearMonth month = YearMonth.parse(period);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.id AS supply_id, s.order_no, SUM(ri.actual_qty * si.price) AS amount "
                        + "FROM receive_order r JOIN supply_order s ON s.id = r.supply_id AND s.tenant_id = r.tenant_id "
                        + "JOIN receive_order_item ri ON ri.receive_id = r.id AND ri.tenant_id = r.tenant_id "
                        + "JOIN supply_order_item si ON si.id = ri.supply_item_id AND si.tenant_id = ri.tenant_id "
                        + "WHERE r.tenant_id = ? AND s.supplier_id = ? AND r.status = 'CONFIRMED' "
                        + "AND r.received_at >= ? AND r.received_at < ? GROUP BY s.id, s.order_no",
                AuthContext.require().getTenantId(), supplierId, start, end);
        SettlementStatement exists = statementMapper.selectOne(new LambdaQueryWrapper<SettlementStatement>()
                .eq(SettlementStatement::getType, "SUPPLIER")
                .eq(SettlementStatement::getPartnerId, supplierId)
                .eq(SettlementStatement::getPeriodStart, month.atDay(1))
                .last("limit 1"));
        if (exists != null) {
            return ApiResult.ok(exists);
        }
        SettlementStatement statement = new SettlementStatement();
        statement.setStatementNo(noGenerator.next("SS"));
        statement.setType("SUPPLIER");
        statement.setPartnerId(supplierId);
        statement.setPartnerName(supplier.getName());
        statement.setPeriodStart(month.atDay(1));
        statement.setPeriodEnd(month.atEndOfMonth());
        statement.setStatus("PENDING");
        statement.setTotalAmount(BigDecimal.ZERO);
        statement.setDeductionAmount(BigDecimal.ZERO);
        statement.setPayableAmount(BigDecimal.ZERO);
        statement.setRemark(period + " 供应商对账单");
        statementMapper.insert(statement);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal amount = (BigDecimal) row.get("amount");
            SettlementItem item = new SettlementItem();
            item.setStatementId(statement.getId());
            item.setRefType("SUPPLY");
            item.setRefId(((Number) row.get("supply_id")).longValue());
            item.setRefNo((String) row.get("order_no"));
            item.setAmount(amount);
            item.setDeduction(BigDecimal.ZERO);
            item.setPayable(amount);
            itemMapper.insert(item);
            total = total.add(amount);
        }
        statement.setTotalAmount(total);
        statement.setPayableAmount(total.subtract(statement.getDeductionAmount()));
        statementMapper.updateById(statement);
        return ApiResult.ok(statement);
    }

    @PostMapping("/generate-franchise")
    @RequirePerm("settlement:create")
    @Transactional
    public ApiResult<SettlementStatement> generateFranchise(@RequestParam Long franchiseeId,
                                                            @RequestParam String period) {
        Franchisee franchisee = franchiseeMapper.selectById(franchiseeId);
        if (franchisee == null) {
            throw BizException.notFound("加盟商不存在");
        }
        YearMonth month = YearMonth.parse(period);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        List<FranchiseeStore> relations = franchiseeStoreMapper.selectList(
                new LambdaQueryWrapper<FranchiseeStore>().eq(FranchiseeStore::getFranchiseeId, franchiseeId));
        if (relations.isEmpty()) {
            throw new BizException("该加盟商未绑定门店");
        }
        SettlementStatement statement = new SettlementStatement();
        statement.setStatementNo(noGenerator.next("FS"));
        statement.setType("FRANCHISE");
        statement.setPartnerId(franchiseeId);
        statement.setPartnerName(franchisee.getName());
        statement.setPeriodStart(month.atDay(1));
        statement.setPeriodEnd(month.atEndOfMonth());
        statement.setStatus("PENDING");
        statement.setTotalAmount(BigDecimal.ZERO);
        statement.setDeductionAmount(BigDecimal.ZERO);
        statement.setPayableAmount(BigDecimal.ZERO);
        statement.setRemark(period + " 加盟分账对账单");
        statementMapper.insert(statement);
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        for (FranchiseeStore relation : relations) {
            BigDecimal sales = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(paid_amount),0) FROM sales_order WHERE tenant_id = ? AND store_id = ? "
                            + "AND pay_status = 'PAID' AND created_at >= ? AND created_at < ?",
                    BigDecimal.class, AuthContext.require().getTenantId(), relation.getStoreId(), start, end);
            sales = sales == null ? BigDecimal.ZERO : sales;
            BigDecimal commission = "FIXED".equals(franchisee.getCommissionType())
                    ? franchisee.getCommissionValue()
                    : sales.multiply(franchisee.getCommissionValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            SettlementItem item = new SettlementItem();
            item.setStatementId(statement.getId());
            item.setRefType("FRANCHISE");
            item.setRefId(relation.getStoreId());
            item.setRefNo("STORE-" + relation.getStoreId());
            item.setAmount(sales);
            item.setDeduction(BigDecimal.ZERO);
            item.setPayable(commission);
            itemMapper.insert(item);
            totalSales = totalSales.add(sales);
            totalCommission = totalCommission.add(commission);
            FranchiseCommission row = commissionMapper.selectOne(new LambdaQueryWrapper<FranchiseCommission>()
                    .eq(FranchiseCommission::getFranchiseeId, franchiseeId)
                    .eq(FranchiseCommission::getStoreId, relation.getStoreId())
                    .eq(FranchiseCommission::getPeriod, period).last("limit 1"));
            if (row == null) {
                row = new FranchiseCommission();
                row.setFranchiseeId(franchiseeId);
                row.setStoreId(relation.getStoreId());
                row.setPeriod(period);
                row.setSalesAmount(sales);
                row.setCommissionType(franchisee.getCommissionType());
                row.setCommissionValue(franchisee.getCommissionValue());
                row.setCommissionAmount(commission);
                row.setStatus("PENDING");
                commissionMapper.insert(row);
            }
        }
        statement.setTotalAmount(totalSales);
        statement.setPayableAmount(totalCommission);
        statementMapper.updateById(statement);
        return ApiResult.ok(statement);
    }

    @PostMapping("/{id}/confirm")
    @RequirePerm("settlement:update")
    public ApiResult<Void> confirm(@PathVariable Long id) {
        SettlementStatement statement = require(id);
        statement.setStatus("CONFIRMED");
        statement.setConfirmedAt(LocalDateTime.now());
        statementMapper.updateById(statement);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/pay")
    @RequirePerm("settlement:update")
    public ApiResult<Void> pay(@PathVariable Long id) {
        SettlementStatement statement = require(id);
        statement.setStatus("PAID");
        statement.setPaidAt(LocalDateTime.now());
        statementMapper.updateById(statement);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/dispute")
    @RequirePerm("settlement:update")
    public ApiResult<Void> dispute(@PathVariable Long id, @RequestParam(required = false) String reason) {
        SettlementStatement statement = require(id);
        statement.setStatus("DISPUTED");
        statement.setRemark(reason == null ? statement.getRemark() : reason);
        statementMapper.updateById(statement);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/invoices")
    @RequirePerm("settlement:update")
    public ApiResult<Void> addInvoice(@PathVariable Long id, @RequestParam String invoiceNo,
                                      @RequestParam BigDecimal amount,
                                      @RequestParam(required = false) String fileUrl) {
        require(id);
        SettlementInvoice invoice = new SettlementInvoice();
        invoice.setStatementId(id);
        invoice.setInvoiceNo(invoiceNo);
        invoice.setAmount(amount);
        invoice.setFileUrl(fileUrl);
        invoice.setStatus("ISSUED");
        invoiceMapper.insert(invoice);
        return ApiResult.ok();
    }

    private SettlementStatement require(Long id) {
        SettlementStatement statement = statementMapper.selectById(id);
        if (statement == null) {
            throw BizException.notFound("对账单不存在");
        }
        return statement;
    }
}
