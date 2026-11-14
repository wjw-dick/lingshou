package com.retail.convenience.modules.system.controller;

import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.system.entity.SysStorageConfig;
import com.retail.convenience.modules.system.mapper.SysStorageConfigMapper;
import com.retail.convenience.modules.system.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/system/storage")
public class StorageConfigController {

    private final SysStorageConfigMapper configMapper;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final String cryptoSecret;

    public StorageConfigController(SysStorageConfigMapper configMapper, StorageService storageService,
                                   ObjectMapper objectMapper, @Value("${app.jwt.secret}") String cryptoSecret) {
        this.configMapper = configMapper;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.cryptoSecret = cryptoSecret;
    }

    @GetMapping
    @RequirePerm("system:view")
    public ApiResult<Map<String, Object>> get() {
        SysStorageConfig config = storageService.currentConfig(AuthContext.require().getTenantId());
        return ApiResult.ok(storageService.maskedConfig(config));
    }

    @PostMapping("/test")
    @RequirePerm("system:view")
    public ApiResult<Map<String, Object>> test(@RequestBody StorageRequest request) {
        return ApiResult.ok(storageService.test(request.getDriver(), request.getConfig()));
    }

    @PostMapping("/save")
    @RequirePerm("system:update")
    public ApiResult<Void> save(@RequestBody StorageRequest request) {
        if (request.getDriver() == null) {
            throw new BizException("请选择存储驱动");
        }
        Long tenantId = AuthContext.require().getTenantId();
        SysStorageConfig config = configMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysStorageConfig>()
                        .eq(SysStorageConfig::getTenantId, tenantId).last("limit 1"));
        Map<String, Object> values = new LinkedHashMap<>(request.getConfig() == null ? Map.of() : request.getConfig());
        for (String key : values.keySet()) {
            String lower = key.toLowerCase();
            if (lower.contains("secret") || lower.contains("accesskey")) {
                String value = values.get(key) == null ? "" : String.valueOf(values.get(key));
                if (!value.contains("*")) {
                    values.put(key, AesUtil.encrypt(value, cryptoSecret));
                }
            }
        }
        try {
            String json = objectMapper.writeValueAsString(values);
            if (config == null) {
                config = new SysStorageConfig();
                config.setTenantId(tenantId);
                config.setDriver(request.getDriver());
                config.setConfig(json);
                config.setEnabled(1);
                config.setLastTestAt(LocalDateTime.now());
                configMapper.insert(config);
            } else {
                config.setDriver(request.getDriver());
                config.setConfig(json);
                config.setEnabled(1);
                config.setLastTestAt(LocalDateTime.now());
                configMapper.updateById(config);
            }
            return ApiResult.ok();
        } catch (Exception e) {
            throw new BizException("保存存储配置失败：" + e.getMessage());
        }
    }

    @lombok.Data
    public static class StorageRequest {
        private String driver;
        private Map<String, Object> config;
    }
}
