package com.retail.convenience.modules.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicTenantController {

    private final PlatformTenantMapper tenantMapper;

    public PublicTenantController(PlatformTenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @GetMapping("/tenants")
    public ApiResult<List<Map<String, Object>>> tenants() {
        List<PlatformTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getDeleted, 0)
                .in(PlatformTenant::getStatus, "ACTIVE", "TRIAL", "OVERDUE")
                .orderByAsc(PlatformTenant::getTenantId));
        return ApiResult.ok(tenants.stream().map(t -> Map.<String, Object>of(
                "tenantCode", t.getTenantCode(),
                "name", t.getName(),
                "status", t.getStatus()
        )).toList());
    }

    @GetMapping("/tenants/{code}")
    public ApiResult<Map<String, Object>> tenant(@PathVariable String code) {
        PlatformTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getTenantCode, code)
                .eq(PlatformTenant::getDeleted, 0)
                .last("limit 1"));
        if (tenant == null) {
            return ApiResult.fail(404, "租户不存在");
        }
        return ApiResult.ok(Map.of(
                "tenantCode", tenant.getTenantCode(),
                "name", tenant.getName(),
                "status", tenant.getStatus()
        ));
    }
}
