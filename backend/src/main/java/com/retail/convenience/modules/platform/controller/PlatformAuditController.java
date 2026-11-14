package com.retail.convenience.modules.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.util.ExcelUtil;
import com.retail.convenience.modules.platform.entity.PlatformAudit;
import com.retail.convenience.modules.platform.mapper.PlatformAuditMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform/audits")
public class PlatformAuditController {

    private final PlatformAuditMapper auditMapper;

    public PlatformAuditController(PlatformAuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    @GetMapping
    @RequirePerm("audit:view")
    public ApiResult<PageResult<PlatformAudit>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String result) {
        Page<PlatformAudit> pageResult = auditMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PlatformAudit>()
                        .eq(tenantId != null, PlatformAudit::getTenantId, tenantId)
                        .like(username != null && !username.isBlank(), PlatformAudit::getUsername, username)
                        .eq(result != null && !result.isBlank(), PlatformAudit::getResult, result)
                        .orderByDesc(PlatformAudit::getCreatedAt));
        return ApiResult.ok(PageResult.of(pageResult));
    }

    @GetMapping("/export")
    @RequirePerm("audit:view")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long tenantId,
                                         @RequestParam(required = false) String username) {
        List<PlatformAudit> rows = auditMapper.selectList(new LambdaQueryWrapper<PlatformAudit>()
                .eq(tenantId != null, PlatformAudit::getTenantId, tenantId)
                .like(username != null && !username.isBlank(), PlatformAudit::getUsername, username)
                .orderByDesc(PlatformAudit::getCreatedAt).last("limit 10000"));
        byte[] bytes = ExcelUtil.write("平台审计", List.of("时间", "操作人", "动作", "对象", "租户ID", "结果", "IP", "详情"),
                rows.stream().map(a -> List.<Object>of(
                        nvl(a.getCreatedAt()), nvl(a.getUsername()), nvl(a.getAction()),
                        nvl(a.getTargetType()) + " " + nvl(a.getTargetId()), nvl(a.getTenantId()),
                        nvl(a.getResult()), nvl(a.getIp()), nvl(a.getDetail())
                )).toList());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("平台审计日志.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private Object nvl(Object v) {
        return v == null ? "" : v;
    }
}
