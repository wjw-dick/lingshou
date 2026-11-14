package com.retail.convenience.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.modules.system.entity.SysConfig;
import com.retail.convenience.modules.system.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigService {

    private final SysConfigMapper configMapper;

    public SystemConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public String get(Long tenantId, String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getTenantId, tenantId)
                .eq(SysConfig::getConfigKey, key)
                .last("limit 1"));
        return config == null || config.getConfigValue() == null ? defaultValue : config.getConfigValue();
    }

    public int getInt(Long tenantId, String key, int defaultValue) {
        try {
            return Integer.parseInt(get(tenantId, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(Long tenantId, String key, double defaultValue) {
        try {
            return Double.parseDouble(get(tenantId, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
