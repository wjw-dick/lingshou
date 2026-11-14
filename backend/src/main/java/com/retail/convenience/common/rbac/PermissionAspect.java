package com.retail.convenience.common.rbac;

import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private final PermissionService permissionService;

    public PermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Before("@annotation(requirePerm)")
    public void check(RequirePerm requirePerm) {
        LoginUser user = AuthContext.require();
        if (!permissionService.hasPermission(user, requirePerm.value())) {
            throw BizException.forbidden("无权执行该操作：" + requirePerm.value());
        }
    }
}
