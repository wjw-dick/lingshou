package com.retail.convenience.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.tenant.TenantGuardService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(1)
public class AuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final TenantGuardService tenantGuardService;
    private final ObjectMapper objectMapper;

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/v1/public/", "/api/v1/auth/login", "/api/v1/platform/auth/login",
            "/api/v1/app/auth/login", "/api/v1/mp/open/", "/admin-api/mp/open/", "/uploads/", "/swagger-ui",
            "/v3/api-docs", "/error", "/actuator/health"
    );

    public AuthFilter(JwtService jwtService, SessionService sessionService,
                      TenantGuardService tenantGuardService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.tenantGuardService = tenantGuardService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        boolean isPublic = PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
        String token = extractToken(request);
        try {
            if (token == null) {
                if (isPublic) {
                    chain.doFilter(request, response);
                    return;
                }
                throw new BizException(401, "未登录或登录已失效");
            }
            Claims claims = jwtService.parse(token);
            LoginUser user = jwtService.toLoginUser(claims);
            if (!sessionService.isActive(user)) {
                throw new BizException(401, "登录状态已失效，请重新登录");
            }
            if (user.getTenantId() != null) {
                tenantGuardService.findActive(user.getTenantId());
                TenantContext.set(user.getTenantId(), user.getTenantCode());
            }
            AuthContext.set(user);
            enforcePortal(user, path);
            if (isOverdueWrite(user, request)) {
                throw BizException.forbidden("租户已欠费，写操作受限，请联系平台续费");
            }
        } catch (BizException e) {
            AuthContext.clear();
            TenantContext.clear();
            writeError(response, e.getCode(), e.getMessage());
            return;
        } catch (Exception e) {
            AuthContext.clear();
            TenantContext.clear();
            writeError(response, 401, "登录状态无效");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
            TenantContext.clear();
        }
    }

    private void enforcePortal(LoginUser user, String path) {
        if (path.startsWith("/api/v1/platform/") && !user.isPlatform()) {
            throw BizException.forbidden("无权访问平台运营端");
        }
        if (path.startsWith("/api/v1/tenant/") && !user.isTenant()) {
            throw BizException.forbidden("无权访问租户后台");
        }
        if (path.startsWith("/api/v1/staff/")
                && (!user.isTenant() || user.getStoreId() == null)) {
            throw BizException.forbidden("仅门店员工可访问门店端");
        }
        if (path.startsWith("/api/v1/supplier/")
                && (!user.isTenant() || user.getSupplierId() == null)) {
            throw BizException.forbidden("仅供应商账号可访问供应商端");
        }
        if (path.startsWith("/api/v1/franchisee/")
                && (!user.isTenant() || user.getFranchiseeId() == null)) {
            throw BizException.forbidden("仅加盟商账号可访问加盟商端");
        }
        if (path.startsWith("/api/v1/app/") && !user.isMember() && !user.isTenant()) {
            throw BizException.forbidden("请先登录会员账号");
        }
    }

    private boolean isOverdueWrite(LoginUser user, HttpServletRequest request) {
        if (user.getTenantId() == null || "GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return "OVERDUE".equals(tenantGuardService.status(user.getTenantId()))
                && !request.getRequestURI().startsWith("/api/v1/platform/");
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return request.getHeader("X-Token");
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code == 401 ? 401 : code == 403 ? 403 : 400);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
    }
}
