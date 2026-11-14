package com.retail.convenience.modules.platform.controller;

import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.modules.platform.dto.RenewRequest;
import com.retail.convenience.modules.platform.dto.TenantCreateRequest;
import com.retail.convenience.modules.platform.dto.TenantUpdateRequest;
import com.retail.convenience.modules.platform.service.PlatformTenantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformTenantController {

    private final PlatformTenantService tenantService;

    public PlatformTenantController(PlatformTenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/dashboard")
    @RequirePerm("platform:view")
    public ApiResult<Map<String, Object>> dashboard() {
        return ApiResult.ok(tenantService.dashboard());
    }

    @GetMapping("/tenants")
    @RequirePerm("tenant:manage")
    public ApiResult<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long planId) {
        return ApiResult.ok(tenantService.page(page, size, keyword, status, planId));
    }

    @GetMapping("/tenants/{id}")
    @RequirePerm("tenant:manage")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResult.ok(tenantService.detail(id));
    }

    @PostMapping("/tenants")
    @RequirePerm("tenant:manage")
    public ApiResult<Map<String, Object>> create(@Valid @RequestBody TenantCreateRequest request) {
        return ApiResult.ok(tenantService.create(request));
    }

    @PutMapping("/tenants/{id}")
    @RequirePerm("tenant:manage")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody TenantUpdateRequest request) {
        tenantService.update(id, request);
        return ApiResult.ok();
    }

    @PostMapping("/tenants/{id}/status")
    @RequirePerm("tenant:manage")
    public ApiResult<Void> status(@PathVariable Long id, @RequestParam String status) {
        tenantService.changeStatus(id, status);
        return ApiResult.ok();
    }

    @PostMapping("/tenants/{id}/renew")
    @RequirePerm("tenant:manage")
    public ApiResult<Void> renew(@PathVariable Long id, @RequestBody(required = false) RenewRequest request) {
        tenantService.renew(id, request == null ? new RenewRequest() : request);
        return ApiResult.ok();
    }

    @DeleteMapping("/tenants/{id}")
    @RequirePerm("tenant:manage")
    public ApiResult<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return ApiResult.ok();
    }

    @GetMapping("/tenants/{id}/usage")
    @RequirePerm("tenant:manage")
    public ApiResult<Map<String, Object>> usage(@PathVariable Long id) {
        return ApiResult.ok(tenantService.usage(id));
    }
}
