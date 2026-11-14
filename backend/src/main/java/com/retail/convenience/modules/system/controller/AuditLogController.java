package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.util.ExcelUtil;
import com.retail.convenience.modules.system.entity.SysAuditLog;
import com.retail.convenience.modules.system.mapper.SysAuditLogMapper;
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

@RestController
@RequestMapping("/api/v1/tenant/system/audit-logs")
public class AuditLogController {

    private final SysAuditLogMapper auditMapper;

    public AuditLogController(SysAuditLogMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    @GetMapping
    @RequirePerm("system:view")
    public ApiResult<PageResult<SysAuditLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String result) {
        Page<SysAuditLog> pageResult = auditMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysAuditLog>()
                        .eq(module != null && !module.isBlank(), SysAuditLog::getModule, module)
                        .like(username != null && !username.isBlank(), SysAuditLog::getUsername, username)
                        .eq(result != null && !result.isBlank(), SysAuditLog::getResult, result)
                        .orderByDesc(SysAuditLog::getCreatedAt));
        return ApiResult.ok(PageResult.of(pageResult));
    }

    @GetMapping("/export")
    @RequirePerm("system:view")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String module,
                                         @RequestParam(required = false) String username) {
        List<SysAuditLog> rows = auditMapper.selectList(new LambdaQueryWrapper<SysAuditLog>()
                .eq(module != null && !module.isBlank(), SysAuditLog::getModule, module)
                .like(username != null && !username.isBlank(), SysAuditLog::getUsername, username)
                .orderByDesc(SysAuditLog::getCreatedAt).last("limit 10000"));
        byte[] bytes = ExcelUtil.write("审计日志", List.of("时间", "操作人", "模块", "动作", "对象", "结果", "IP", "详情"),
                rows.stream().map(a -> List.<Object>of(
                        nvl(a.getCreatedAt()), nvl(a.getUsername()), nvl(a.getModule()), nvl(a.getAction()),
                        nvl(a.getTargetType()) + " " + nvl(a.getTargetId()), nvl(a.getResult()),
                        nvl(a.getIp()), nvl(a.getDetail())
                )).toList());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("审计日志.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private Object nvl(Object v) {
        return v == null ? "" : v;
    }
}
