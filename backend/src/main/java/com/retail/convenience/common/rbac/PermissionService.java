package com.retail.convenience.common.rbac;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.modules.system.entity.SysRole;
import com.retail.convenience.modules.system.mapper.SysRoleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class PermissionService {

    private final SysRoleMapper roleMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public PermissionService(SysRoleMapper roleMapper, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.roleMapper = roleMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public boolean hasPermission(LoginUser user, String required) {
        if (user == null) {
            return false;
        }
        if (user.isPlatform()) {
            return switch (user.getRoleCode() == null ? "" : user.getRoleCode()) {
                case "PLATFORM_ADMIN" -> true;
                case "PLATFORM_OPERATOR" -> List.of("platform:view", "tenant:manage", "plan:manage",
                        "announcement:manage", "audit:view").stream().anyMatch(p -> match(p, required));
                case "PLATFORM_FINANCE" -> List.of("platform:view", "bill:manage",
                        "audit:view").stream().anyMatch(p -> match(p, required));
                default -> false;
            };
        }
        if (!user.isTenant()) {
            return false;
        }
        if ("TENANT_ADMIN".equals(user.getRoleCode())) {
            return true;
        }
        return permissions(user.getTenantId(), user.getRoleCode()).stream()
                .anyMatch(p -> match(p, required));
    }

    public boolean hasAnyPermission(LoginUser user, String required) {
        return hasPermission(user, required);
    }

    public List<String> permissions(Long tenantId, String roleCode) {
        String key = "tenant:perm:" + tenantId + ":" + roleCode;
        String cached = redis.opsForValue().get(key);
        try {
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {
                });
            }
            SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getTenantId, tenantId)
                    .eq(SysRole::getRoleCode, roleCode)
                    .last("limit 1"));
            if (role == null || role.getPermissions() == null) {
                return List.of();
            }
            List<String> list = objectMapper.readValue(role.getPermissions(), new TypeReference<>() {
            });
            redis.opsForValue().set(key, objectMapper.writeValueAsString(list), Duration.ofMinutes(5));
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    public boolean match(String granted, String required) {
        if (granted == null || required == null) {
            return false;
        }
        if ("*".equals(granted) || granted.equals(required)) {
            return true;
        }
        if (granted.endsWith(":*")) {
            String prefix = granted.substring(0, granted.length() - 2);
            return required.equals(prefix) || required.startsWith(prefix + ":");
        }
        return false;
    }
}
