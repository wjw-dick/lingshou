package com.retail.convenience.modules.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.modules.platform.entity.PlatformPlan;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.mapper.PlatformPlanMapper;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/plans")
public class PlatformPlanController {

    private final PlatformPlanMapper planMapper;
    private final PlatformTenantMapper tenantMapper;

    public PlatformPlanController(PlatformPlanMapper planMapper, PlatformTenantMapper tenantMapper) {
        this.planMapper = planMapper;
        this.tenantMapper = tenantMapper;
    }

    @GetMapping
    @RequirePerm("platform:view")
    public ApiResult<List<PlatformPlan>> list(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(planMapper.selectList(new LambdaQueryWrapper<PlatformPlan>()
                .like(keyword != null && !keyword.isBlank(), PlatformPlan::getName, keyword)
                .orderByAsc(PlatformPlan::getPrice)));
    }

    @PostMapping
    @RequirePerm("plan:manage")
    public ApiResult<Void> create(@RequestBody PlatformPlan plan) {
        plan.setPlanId(null);
        planMapper.insert(plan);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @RequirePerm("plan:manage")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody PlatformPlan plan) {
        plan.setPlanId(id);
        planMapper.updateById(plan);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("plan:manage")
    public ApiResult<Void> delete(@PathVariable Long id) {
        if (tenantMapper.selectCount(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getPlanId, id).eq(PlatformTenant::getDeleted, 0)) > 0) {
            throw new BizException("套餐已被租户使用，不能删除");
        }
        planMapper.deleteById(id);
        return ApiResult.ok();
    }
}
