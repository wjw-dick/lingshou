package com.retail.convenience.modules.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.modules.mp.entity.MpAccount;
import com.retail.convenience.modules.mp.mapper.MpAccountMapper;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.entity.PlatformTenantQuotaUsage;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import com.retail.convenience.modules.platform.mapper.PlatformTenantQuotaUsageMapper;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.system.entity.SysFile;
import com.retail.convenience.modules.system.entity.SysUser;
import com.retail.convenience.modules.system.mapper.SysFileMapper;
import com.retail.convenience.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PlatformQuotaService {

    private final PlatformTenantMapper tenantMapper;
    private final PlatformTenantQuotaUsageMapper usageMapper;
    private final SysUserMapper userMapper;
    private final StoreMapper storeMapper;
    private final MpAccountMapper mpAccountMapper;
    private final SysFileMapper fileMapper;

    public PlatformQuotaService(PlatformTenantMapper tenantMapper,
                                PlatformTenantQuotaUsageMapper usageMapper,
                                SysUserMapper userMapper,
                                StoreMapper storeMapper,
                                MpAccountMapper mpAccountMapper,
                                SysFileMapper fileMapper) {
        this.tenantMapper = tenantMapper;
        this.usageMapper = usageMapper;
        this.userMapper = userMapper;
        this.storeMapper = storeMapper;
        this.mpAccountMapper = mpAccountMapper;
        this.fileMapper = fileMapper;
    }

    public Map<String, Object> usage(Long tenantId) {
        PlatformTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        Long previousId = com.retail.convenience.common.security.TenantContext.getTenantId();
        String previousCode = com.retail.convenience.common.security.TenantContext.getTenantCode();
        boolean switched = previousId == null || !previousId.equals(tenantId);
        if (switched) {
            com.retail.convenience.common.security.TenantContext.set(tenantId, tenant.getTenantCode());
        }
        try {
            long seats = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, tenantId).eq(SysUser::getDeleted, 0));
            long stores = storeMapper.selectCount(new LambdaQueryWrapper<Store>()
                    .eq(Store::getTenantId, tenantId).eq(Store::getDeleted, 0));
            long mps = mpAccountMapper.selectCount(new LambdaQueryWrapper<MpAccount>()
                    .eq(MpAccount::getTenantId, tenantId));
            long fileBytes = fileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                            .eq(SysFile::getTenantId, tenantId))
                    .stream().mapToLong(f -> f.getSize() == null ? 0 : f.getSize()).sum();
            BigDecimal storageGb = BigDecimal.valueOf(fileBytes)
                    .divide(BigDecimal.valueOf(1024L * 1024 * 1024), 2, RoundingMode.HALF_UP);
            PlatformTenantQuotaUsage latest = usageMapper.selectOne(new LambdaQueryWrapper<PlatformTenantQuotaUsage>()
                    .eq(PlatformTenantQuotaUsage::getTenantId, tenantId)
                    .orderByDesc(PlatformTenantQuotaUsage::getStatDate)
                    .last("limit 1"));

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("seatUsed", seats);
            map.put("seatLimit", tenant.getSeatLimit());
            map.put("storeUsed", stores);
            map.put("storeLimit", tenant.getStoreLimit());
            map.put("mpUsed", mps);
            map.put("mpLimit", tenant.getMpAccountLimit());
            map.put("storageUsedGb", storageGb);
            map.put("storageLimitGb", tenant.getStorageQuotaGb());
            map.put("smsUsed", latest == null ? 0 : latest.getSmsUsed());
            map.put("smsLimit", tenant.getSmsQuota());
            map.put("apiUsed", latest == null ? 0 : latest.getApiUsed());
            map.put("apiLimit", tenant.getApiQuota());
            return map;
        } finally {
            if (switched) {
                if (previousId == null) {
                    com.retail.convenience.common.security.TenantContext.clear();
                } else {
                    com.retail.convenience.common.security.TenantContext.set(previousId, previousCode);
                }
            }
        }
    }

    public void assertSeat(Long tenantId) {
        PlatformTenant tenant = requireTenant(tenantId);
        long used = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId).eq(SysUser::getDeleted, 0));
        if (used >= tenant.getSeatLimit()) {
            throw BizException.quota("席位数已达上限（" + tenant.getSeatLimit() + "），请升级套餐");
        }
    }

    public void assertStore(Long tenantId) {
        PlatformTenant tenant = requireTenant(tenantId);
        long used = storeMapper.selectCount(new LambdaQueryWrapper<Store>()
                .eq(Store::getTenantId, tenantId).eq(Store::getDeleted, 0));
        if (used >= tenant.getStoreLimit()) {
            throw BizException.quota("门店数已达上限（" + tenant.getStoreLimit() + "），请升级套餐");
        }
    }

    public void assertMpAccount(Long tenantId) {
        PlatformTenant tenant = requireTenant(tenantId);
        long used = mpAccountMapper.selectCount(new LambdaQueryWrapper<MpAccount>()
                .eq(MpAccount::getTenantId, tenantId));
        if (used >= tenant.getMpAccountLimit()) {
            throw BizException.quota("公众号数量已达上限（" + tenant.getMpAccountLimit() + "），请升级套餐");
        }
    }

    public void assertStorage(Long tenantId, long addBytes) {
        PlatformTenant tenant = requireTenant(tenantId);
        long bytes = fileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                        .eq(SysFile::getTenantId, tenantId))
                .stream().mapToLong(f -> f.getSize() == null ? 0 : f.getSize()).sum();
        long limitBytes = (long) tenant.getStorageQuotaGb() * 1024 * 1024 * 1024;
        if (bytes + addBytes > limitBytes) {
            throw BizException.quota("存储配额不足（" + tenant.getStorageQuotaGb() + "GB），请升级套餐");
        }
    }

    public void refreshDailyUsage(Long tenantId) {
        Map<String, Object> usage = usage(tenantId);
        PlatformTenantQuotaUsage row = usageMapper.selectOne(new LambdaQueryWrapper<PlatformTenantQuotaUsage>()
                .eq(PlatformTenantQuotaUsage::getTenantId, tenantId)
                .eq(PlatformTenantQuotaUsage::getStatDate, LocalDate.now())
                .last("limit 1"));
        if (row == null) {
            row = new PlatformTenantQuotaUsage();
            row.setTenantId(tenantId);
            row.setStatDate(LocalDate.now());
        }
        row.setSeatUsed(((Number) usage.get("seatUsed")).intValue());
        row.setStoreUsed(((Number) usage.get("storeUsed")).intValue());
        row.setMpUsed(((Number) usage.get("mpUsed")).intValue());
        row.setStorageUsedGb((BigDecimal) usage.get("storageUsedGb"));
        row.setSmsUsed(((Number) usage.get("smsUsed")).intValue());
        row.setApiUsed(((Number) usage.get("apiUsed")).longValue());
        if (row.getId() == null) {
            usageMapper.insert(row);
        } else {
            usageMapper.updateById(row);
        }
    }

    private PlatformTenant requireTenant(Long tenantId) {
        PlatformTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        return tenant;
    }
}
