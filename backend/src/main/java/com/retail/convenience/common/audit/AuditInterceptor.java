package com.retail.convenience.common.audit;

import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.modules.platform.entity.PlatformAudit;
import com.retail.convenience.modules.platform.mapper.PlatformAuditMapper;
import com.retail.convenience.modules.system.entity.SysAuditLog;
import com.retail.convenience.modules.system.mapper.SysAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private final SysAuditLogMapper tenantAuditMapper;
    private final PlatformAuditMapper platformAuditMapper;

    public AuditInterceptor(SysAuditLogMapper tenantAuditMapper, PlatformAuditMapper platformAuditMapper) {
        this.tenantAuditMapper = tenantAuditMapper;
        this.platformAuditMapper = platformAuditMapper;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        String path = request.getRequestURI();
        if (path.contains("/auth/login") || path.startsWith("/api/v1/mp/open/")) {
            return;
        }
        LoginUser user = AuthContext.get();
        if (user == null) {
            return;
        }
        try {
            String module = segment(path, 3);
            String action = switch (method) {
                case "POST" -> "新增";
                case "PUT", "PATCH" -> "修改";
                case "DELETE" -> "删除";
                default -> method;
            };
            String targetType = segment(path, 3);
            String targetId = lastNumeric(path);
            String detail = method + " " + path + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
            String result = ex == null && response.getStatus() < 400 ? "SUCCESS" : "FAIL";
            if (user.isPlatform()) {
                PlatformAudit audit = new PlatformAudit();
                audit.setUserId(user.getUserId());
                audit.setUsername(user.getUsername());
                audit.setAction(action);
                audit.setTargetType(targetType);
                audit.setTargetId(targetId);
                audit.setTenantId(user.getTenantId());
                audit.setDetail(trim(detail));
                audit.setIp(clientIp(request));
                audit.setResult(result);
                platformAuditMapper.insert(audit);
            } else if (user.isTenant()) {
                SysAuditLog audit = new SysAuditLog();
                audit.setTenantId(user.getTenantId());
                audit.setUserId(user.getUserId());
                audit.setUsername(user.getUsername());
                audit.setModule(module);
                audit.setAction(action);
                audit.setTargetType(targetType);
                audit.setTargetId(targetId);
                audit.setDetail(trim(detail));
                audit.setIp(clientIp(request));
                audit.setUserAgent(trim(request.getHeader("User-Agent")));
                audit.setResult(result);
                tenantAuditMapper.insert(audit);
            }
        } catch (Exception e) {
            log.warn("写入审计日志失败: {}", e.getMessage());
        }
    }

    private String segment(String path, int index) {
        String[] parts = path.split("/");
        return parts.length > index ? parts[index] : "";
    }

    private String lastNumeric(String path) {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+")) {
                return parts[i];
            }
        }
        return null;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.length() > 60 ? ip.substring(0, 60) : ip;
    }
}
