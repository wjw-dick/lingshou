package com.retail.convenience.common.security;

import com.retail.convenience.common.exception.BizException;

public final class AuthContext {

    private static final ThreadLocal<LoginUser> USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(LoginUser user) {
        USER.set(user);
    }

    public static LoginUser get() {
        return USER.get();
    }

    public static LoginUser require() {
        LoginUser user = USER.get();
        if (user == null) {
            throw new BizException(401, "未登录或登录已失效");
        }
        return user;
    }

    public static void clear() {
        USER.remove();
    }
}
