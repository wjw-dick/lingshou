package com.retail.convenience.modules.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.util.NoGenerator;
import com.retail.convenience.modules.platform.dto.BillGenerateRequest;
import com.retail.convenience.modules.platform.entity.PlatformBill;
import com.retail.convenience.modules.platform.entity.PlatformPlan;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.mapper.PlatformBillMapper;
import com.retail.convenience.modules.platform.mapper.PlatformPlanMapper;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlatformBillService {

    private final PlatformBillMapper billMapper;
    private final PlatformTenantMapper tenantMapper;
    private final PlatformPlanMapper planMapper;
    private final NoGenerator noGenerator;

    public PlatformBillService(PlatformBillMapper billMapper, PlatformTenantMapper tenantMapper,
                               PlatformPlanMapper planMapper, NoGenerator noGenerator) {
        this.billMapper = billMapper;
        this.tenantMapper = tenantMapper;
        this.planMapper = planMapper;
        this.noGenerator = noGenerator;
    }

    public PageResult<Map<String, Object>> page(int page, int size, Long tenantId, String status, String period) {
        Page<PlatformBill> result = billMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PlatformBill>()
                        .eq(tenantId != null, PlatformBill::getTenantId, tenantId)
                        .eq(status != null && !status.isBlank(), PlatformBill::getStatus, status)
                        .eq(period != null && !period.isBlank(), PlatformBill::getPeriod, period)
                        .orderByDesc(PlatformBill::getCreatedAt));
        Map<Long, PlatformTenant> tenants = tenantMapper.selectList(null).stream()
                .collect(Collectors.toMap(PlatformTenant::getTenantId, Function.identity()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PlatformBill bill : result.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("billId", bill.getBillId());
            row.put("billNo", bill.getBillNo());
            row.put("tenantId", bill.getTenantId());
            PlatformTenant tenant = tenants.get(bill.getTenantId());
            row.put("tenantName", tenant == null ? "-" : tenant.getName());
            row.put("period", bill.getPeriod());
            row.put("item", bill.getItem());
            row.put("quantity", bill.getQuantity());
            row.put("unitPrice", bill.getUnitPrice());
            row.put("amount", bill.getAmount());
            row.put("status", bill.getStatus());
            row.put("dueDate", bill.getDueDate());
            row.put("paidAt", bill.getPaidAt());
            row.put("createdAt", bill.getCreatedAt());
            rows.add(row);
        }
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return out;
    }

    public PlatformBill generate(BillGenerateRequest request) {
        PlatformTenant tenant = tenantMapper.selectById(request.getTenantId());
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        PlatformPlan plan = planMapper.selectById(tenant.getPlanId());
        String period = request.getPeriod() == null ? YearMonth.now().toString() : request.getPeriod();
        String item = request.getItem() == null || request.getItem().isBlank()
                ? (plan == null ? "平台服务费" : plan.getName() + "服务费") : request.getItem();
        BigDecimal unitPrice = plan == null ? BigDecimal.ZERO : plan.getPrice();
        PlatformBill bill = new PlatformBill();
        bill.setTenantId(tenant.getTenantId());
        bill.setBillNo(noGenerator.next("BILL"));
        bill.setPeriod(period);
        bill.setItem(item);
        bill.setQuantity(BigDecimal.ONE);
        bill.setUnitPrice(unitPrice);
        bill.setAmount(unitPrice);
        bill.setStatus("UNPAID");
        bill.setDueDate(LocalDate.now().plusDays(15));
        billMapper.insert(bill);
        return bill;
    }

    public void markPaid(Long billId) {
        PlatformBill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw BizException.notFound("账单不存在");
        }
        bill.setStatus("PAID");
        bill.setPaidAt(LocalDateTime.now());
        billMapper.updateById(bill);
    }

    public List<Map<String, Object>> exportRows(Long tenantId, String status, String period) {
        return page(1, 10000, tenantId, status, period).getRecords();
    }

    public BigDecimal paidRevenue() {
        return billMapper.selectList(new LambdaQueryWrapper<PlatformBill>()
                        .eq(PlatformBill::getStatus, "PAID"))
                .stream().map(PlatformBill::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Scheduled(cron = "0 10 1 * * ?")
    public void markOverdue() {
        LocalDate today = LocalDate.now();
        List<PlatformBill> bills = billMapper.selectList(new LambdaQueryWrapper<PlatformBill>()
                .eq(PlatformBill::getStatus, "UNPAID")
                .lt(PlatformBill::getDueDate, today));
        for (PlatformBill bill : bills) {
            bill.setStatus("OVERDUE");
            billMapper.updateById(bill);
        }
    }
}
