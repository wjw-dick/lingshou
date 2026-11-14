package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.system.entity.SysSsoConfig;
import com.retail.convenience.modules.system.mapper.SysSsoConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant/system/sso")
public class SsoController {

    private final SysSsoConfigMapper ssoMapper;
    private final String cryptoSecret;

    public SsoController(SysSsoConfigMapper ssoMapper, @Value("${app.jwt.secret}") String cryptoSecret) {
        this.ssoMapper = ssoMapper;
        this.cryptoSecret = cryptoSecret;
    }

    @GetMapping
    @RequirePerm("system:view")
    public ApiResult<SysSsoConfig> get() {
        SysSsoConfig config = current();
        if (config != null && config.getClientSecret() != null && !config.getClientSecret().isBlank()) {
            config.setClientSecret(AesUtil.mask(config.getClientSecret()));
        }
        return ApiResult.ok(config);
    }

    @PostMapping
    @RequirePerm("system:update")
    public ApiResult<Void> save(@RequestBody SysSsoConfig request) {
        if (Integer.valueOf(1).equals(request.getEnabled()) && request.getServerUrl() == null) {
            throw new BizException("启用 SSO 必须填写服务器根地址");
        }
        SysSsoConfig config = current();
        if (config == null) {
            config = new SysSsoConfig();
            config.setTenantId(AuthContext.require().getTenantId());
        }
        config.setEnabled(request.getEnabled() == null ? 0 : request.getEnabled());
        config.setProtocol(request.getProtocol() == null ? "OIDC" : request.getProtocol());
        config.setServerUrl(request.getServerUrl());
        config.setServiceUrl(request.getServiceUrl());
        config.setClientId(request.getClientId());
        if (request.getClientSecret() != null && !request.getClientSecret().isBlank()
                && !request.getClientSecret().contains("*")) {
            config.setClientSecret(AesUtil.encrypt(request.getClientSecret(), cryptoSecret));
        }
        config.setAccountMapping(request.getAccountMapping());
        config.setAutoCreate(request.getAutoCreate() == null ? 0 : request.getAutoCreate());
        if (config.getId() == null) {
            ssoMapper.insert(config);
        } else {
            ssoMapper.updateById(config);
        }
        return ApiResult.ok();
    }

    private SysSsoConfig current() {
        return ssoMapper.selectOne(new LambdaQueryWrapper<SysSsoConfig>()
                .eq(SysSsoConfig::getTenantId, AuthContext.require().getTenantId()).last("limit 1"));
    }
}
