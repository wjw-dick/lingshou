package com.retail.convenience.modules.auth.controller;

import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.modules.auth.dto.AppLoginRequest;
import com.retail.convenience.modules.auth.dto.LoginRequest;
import com.retail.convenience.modules.auth.dto.LoginResponse;
import com.retail.convenience.modules.auth.dto.PlatformLoginRequest;
import com.retail.convenience.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/platform/auth/login")
    public ApiResult<LoginResponse> platformLogin(@Valid @RequestBody PlatformLoginRequest request) {
        return ApiResult.ok(authService.platformLogin(request));
    }

    @PostMapping("/auth/login")
    public ApiResult<LoginResponse> tenantLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.tenantLogin(request));
    }

    @PostMapping("/app/auth/login")
    public ApiResult<LoginResponse> appLogin(@RequestBody AppLoginRequest request) {
        return ApiResult.ok(authService.appLogin(request));
    }

    @PostMapping("/auth/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.ok();
    }

    @GetMapping("/auth/me")
    public ApiResult<LoginResponse> me() {
        return ApiResult.ok(authService.current());
    }
}
