package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.system.entity.SysConfig;
import com.retail.convenience.modules.system.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/system/configs")
public class SysConfigController {

    private final SysConfigMapper configMapper;
    private final String cryptoSecret;

    public SysConfigController(SysConfigMapper configMapper, @Value("${app.jwt.secret}") String cryptoSecret) {
        this.configMapper = configMapper;
        this.cryptoSecret = cryptoSecret;
    }

    @GetMapping
    @RequirePerm("system:view")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String group) {
        List<SysConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(group != null && !group.isBlank(), SysConfig::getGroupCode, group)
                .orderByAsc(SysConfig::getGroupCode).orderByAsc(SysConfig::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysConfig config : configs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", config.getId());
            row.put("configKey", config.getConfigKey());
            boolean hasValue = config.getConfigValue() != null && !config.getConfigValue().isBlank();
            row.put("configValue", Integer.valueOf(1).equals(config.getEncrypted()) && hasValue
                    ? AesUtil.mask(config.getConfigValue()) : config.getConfigValue());
            row.put("groupCode", config.getGroupCode());
            row.put("encrypted", config.getEncrypted());
            row.put("remark", config.getRemark());
            row.put("hasValue", hasValue);
            rows.add(row);
        }
        return ApiResult.ok(rows);
    }

    @PutMapping
    @RequirePerm("system:update")
    @Transactional
    public ApiResult<Void> save(@RequestBody List<ConfigItem> items) {
        Long tenantId = AuthContext.require().getTenantId();
        for (ConfigItem item : items) {
            if (item.getConfigKey() == null || item.getConfigKey().isBlank()) {
                continue;
            }
            SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getTenantId, tenantId)
                    .eq(SysConfig::getConfigKey, item.getConfigKey()).last("limit 1"));
            boolean encrypted = Integer.valueOf(1).equals(item.getEncrypted())
                    || item.getConfigKey().endsWith("_key") || item.getConfigKey().endsWith("_secret")
                    || item.getConfigKey().contains("password");
            if (config == null) {
                config = new SysConfig();
                config.setTenantId(tenantId);
                config.setConfigKey(item.getConfigKey());
                config.setGroupCode(item.getGroupCode() == null ? "GENERAL" : item.getGroupCode());
                config.setEncrypted(encrypted ? 1 : 0);
                config.setConfigValue(encrypted ? encryptIfNeeded(item.getConfigValue()) : item.getConfigValue());
                config.setRemark(item.getRemark());
                configMapper.insert(config);
            } else {
                if (encrypted && (item.getConfigValue() == null || item.getConfigValue().isBlank())) {
                    continue;
                }
                config.setConfigValue(encrypted ? encryptIfNeeded(item.getConfigValue()) : item.getConfigValue());
                config.setGroupCode(item.getGroupCode() == null ? config.getGroupCode() : item.getGroupCode());
                config.setRemark(item.getRemark());
                config.setEncrypted(encrypted ? 1 : 0);
                configMapper.updateById(config);
            }
        }
        return ApiResult.ok();
    }

    private String encryptIfNeeded(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.startsWith("ENC:")) {
            return value;
        }
        return AesUtil.encrypt(value, cryptoSecret);
    }

    @lombok.Data
    public static class ConfigItem {
        private String configKey;
        private String configValue;
        private String groupCode;
        private Integer encrypted;
        private String remark;
    }
}
