package com.retail.convenience.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.security.TenantContext;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
public class ApiQuotaFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final PlatformTenantMapper tenantMapper;
    private final ObjectMapper objectMapper;

    public ApiQuotaFilter(StringRedisTemplate redis, PlatformTenantMapper tenantMapper,
                          ObjectMapper objectMapper) {
        this.redis = redis;
        this.tenantMapper = tenantMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Long tenantId = TenantContext.getTenantId();
        String path = request.getRequestURI();
        boolean counted = tenantId != null && List.of("/api/v1/tenant/", "/api/v1/app/",
                        "/api/v1/staff/", "/api/v1/supplier/", "/api/v1/franchisee/")
                .stream().anyMatch(path::startsWith);
        if (counted) {
            long quota = apiQuota(tenantId);
            String key = "tenant:api:" + tenantId + ":"
                    + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            Long used = redis.opsForValue().increment(key);
            if (used != null && used == 1L) {
                redis.expire(key, 2, TimeUnit.DAYS);
            }
            if (used != null && quota > 0 && used > quota) {
                response.setStatus(429);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResult.fail(429, "租户 API 调用配额已用尽，请升级套餐")));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private long apiQuota(Long tenantId) {
        String cacheKey = "tenant:quota:api:" + tenantId;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        PlatformTenant tenant = tenantMapper.selectById(tenantId);
        long quota = tenant == null || tenant.getApiQuota() == null ? 0 : tenant.getApiQuota();
        redis.opsForValue().set(cacheKey, String.valueOf(quota), Duration.ofMinutes(1));
        return quota;
    }
}
