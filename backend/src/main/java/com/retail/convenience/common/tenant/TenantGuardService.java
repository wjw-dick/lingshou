package com.retail.convenience.common.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class TenantGuardService {

    private final PlatformTenantMapper tenantMapper;
    private final StringRedisTemplate redis;

    public TenantGuardService(PlatformTenantMapper tenantMapper, StringRedisTemplate redis) {
        this.tenantMapper = tenantMapper;
        this.redis = redis;
    }

    public PlatformTenant findActive(Long tenantId) {
        String cached = redis.opsForValue().get(statusKey(tenantId));
        PlatformTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeleted() != null && tenant.getDeleted() == 1) {
            throw BizException.forbidden("租户不存在或已删除");
        }
        String status = tenant.getStatus();
        if ("TRIAL".equals(status) && tenant.getTrialEndAt() != null
                && tenant.getTrialEndAt().isBefore(LocalDateTime.now())) {
            status = "EXPIRED";
        }
        if ("ACTIVE".equals(status) && tenant.getExpireAt() != null
                && tenant.getExpireAt().isBefore(LocalDateTime.now())) {
            status = "EXPIRED";
        }
        if ("SUSPENDED".equals(status) || "EXPIRED".equals(status)) {
            throw BizException.forbidden("租户已停用或到期，禁止访问");
        }
        redis.opsForValue().set(statusKey(tenantId), status, Duration.ofSeconds(60));
        return tenant;
    }

    public String status(Long tenantId) {
        String cached = redis.opsForValue().get(statusKey(tenantId));
        if (cached != null) {
            return cached;
        }
        PlatformTenant tenant = tenantMapper.selectById(tenantId);
        return tenant == null ? "UNKNOWN" : tenant.getStatus();
    }

    public void evict(Long tenantId) {
        redis.delete(statusKey(tenantId));
    }

    private String statusKey(Long tenantId) {
        return "tenant:guard:" + tenantId;
    }

    public PlatformTenant findByCode(String tenantCode) {
        return tenantMapper.selectOne(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getTenantCode, tenantCode)
                .eq(PlatformTenant::getDeleted, 0)
                .last("limit 1"));
    }
}
