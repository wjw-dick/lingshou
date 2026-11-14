package com.retail.convenience.common.security;

import com.retail.convenience.common.exception.BizException;

public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_CODE = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, String tenantCode) {
        TENANT_ID.set(tenantId);
        TENANT_CODE.set(tenantCode);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static String getTenantCode() {
        return TENANT_CODE.get();
    }

    public static Long requireTenantId() {
        Long id = TENANT_ID.get();
        if (id == null) {
            throw BizException.forbidden("缺少租户上下文，已阻断跨租户访问");
        }
        return id;
    }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_CODE.remove();
    }
}
