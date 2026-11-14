package com.retail.convenience.modules.platform.controller;

import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.util.ExcelUtil;
import com.retail.convenience.modules.platform.dto.BillGenerateRequest;
import com.retail.convenience.modules.platform.entity.PlatformBill;
import com.retail.convenience.modules.platform.service.PlatformBillService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform/bills")
public class PlatformBillController {

    private final PlatformBillService billService;

    public PlatformBillController(PlatformBillService billService) {
        this.billService = billService;
    }

    @GetMapping
    @RequirePerm("bill:manage")
    public ApiResult<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String period) {
        return ApiResult.ok(billService.page(page, size, tenantId, status, period));
    }

    @PostMapping("/generate")
    @RequirePerm("bill:manage")
    public ApiResult<PlatformBill> generate(@RequestBody BillGenerateRequest request) {
        return ApiResult.ok(billService.generate(request));
    }

    @PostMapping("/{id}/pay")
    @RequirePerm("bill:manage")
    public ApiResult<Void> pay(@PathVariable Long id) {
        billService.markPaid(id);
        return ApiResult.ok();
    }

    @GetMapping("/export")
    @RequirePerm("bill:manage")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long tenantId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String period) {
        List<Map<String, Object>> rows = billService.exportRows(tenantId, status, period);
        byte[] bytes = ExcelUtil.write("账单", List.of("账单号", "租户", "账期", "计费项", "金额", "状态", "到期日"),
                rows.stream().map(r -> List.<Object>of(
                        nvl(r.get("billNo")), nvl(r.get("tenantName")), nvl(r.get("period")),
                        nvl(r.get("item")), r.get("amount"), nvl(r.get("status")), nvl(r.get("dueDate"))
                )).toList());
        return download(bytes, "平台账单.xlsx");
    }

    private Object nvl(Object v) {
        return v == null ? "" : v;
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(name, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
