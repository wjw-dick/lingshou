package com.retail.convenience.modules.store.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.modules.platform.service.PlatformQuotaService;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.entity.StoreAuditRecord;
import com.retail.convenience.modules.store.entity.StoreLicense;
import com.retail.convenience.modules.store.mapper.StoreAuditRecordMapper;
import com.retail.convenience.modules.store.mapper.StoreLicenseMapper;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/stores")
public class StoreController {

    private final StoreMapper storeMapper;
    private final StoreLicenseMapper licenseMapper;
    private final StoreAuditRecordMapper auditMapper;
    private final PlatformQuotaService quotaService;

    public StoreController(StoreMapper storeMapper, StoreLicenseMapper licenseMapper,
                           StoreAuditRecordMapper auditMapper, PlatformQuotaService quotaService) {
        this.storeMapper = storeMapper;
        this.licenseMapper = licenseMapper;
        this.auditMapper = auditMapper;
        this.quotaService = quotaService;
    }

    @GetMapping
    @RequirePerm("store:view")
    public ApiResult<PageResult<Store>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        LoginUser user = AuthContext.require();
        Long scoped = scopedStoreId(user);
        Page<Store> result = storeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getDeleted, 0)
                        .eq(scoped != null, Store::getId, scoped)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Store::getName, keyword).or().like(Store::getCode, keyword))
                        .eq(status != null && !status.isBlank(), Store::getStatus, status)
                        .eq(type != null && !type.isBlank(), Store::getType, type)
                        .orderByAsc(Store::getCode));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/options")
    public ApiResult<List<Map<String, Object>>> options() {
        Long scoped = scopedStoreId(AuthContext.require());
        return ApiResult.ok(storeMapper.selectList(new LambdaQueryWrapper<Store>()
                        .eq(Store::getDeleted, 0)
                        .eq(scoped != null, Store::getId, scoped)
                        .orderByAsc(Store::getCode))
                .stream().map(s -> Map.<String, Object>of(
                        "id", s.getId(), "code", s.getCode(), "name", s.getName(),
                        "type", s.getType(), "onlineEnabled", s.getOnlineEnabled()))
                .toList());
    }

    @GetMapping("/{id}")
    @RequirePerm("store:view")
    public ApiResult<Store> detail(@PathVariable Long id) {
        checkScope(id);
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw BizException.notFound("门店不存在");
        }
        return ApiResult.ok(store);
    }

    @PostMapping
    @RequirePerm("store:create")
    public ApiResult<Store> create(@RequestBody Store store) {
        if (storeMapper.selectCount(new LambdaQueryWrapper<Store>()
                .eq(Store::getTenantId, AuthContext.require().getTenantId())
                .eq(Store::getCode, store.getCode())
                .eq(Store::getDeleted, 0)) > 0) {
            throw new BizException("门店编码已存在");
        }
        quotaService.assertStore(AuthContext.require().getTenantId());
        store.setId(null);
        store.setTenantId(AuthContext.require().getTenantId());
        store.setDeleted(0);
        if (store.getStatus() == null) {
            store.setStatus("OPEN");
        }
        if (store.getOnlineEnabled() == null) {
            store.setOnlineEnabled(1);
        }
        if (store.getDeliveryEnabled() == null) {
            store.setDeliveryEnabled(1);
        }
        storeMapper.insert(store);
        return ApiResult.ok(store);
    }

    @PutMapping("/{id}")
    @RequirePerm("store:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Store store) {
        checkScope(id);
        store.setId(id);
        store.setTenantId(AuthContext.require().getTenantId());
        storeMapper.updateById(store);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("store:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        checkScope(id);
        storeMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/status")
    @RequirePerm("store:update")
    public ApiResult<Void> status(@PathVariable Long id, @RequestParam String status,
                                  @RequestParam(required = false) Integer onlineEnabled) {
        checkScope(id);
        Store store = new Store();
        store.setId(id);
        store.setStatus(status);
        if (onlineEnabled != null) {
            store.setOnlineEnabled(onlineEnabled);
        }
        storeMapper.updateById(store);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/licenses")
    @RequirePerm("store:view")
    public ApiResult<List<StoreLicense>> licenses(@PathVariable Long id) {
        checkScope(id);
        return ApiResult.ok(licenseMapper.selectList(new LambdaQueryWrapper<StoreLicense>()
                .eq(StoreLicense::getStoreId, id).orderByAsc(StoreLicense::getExpireAt)));
    }

    @PostMapping("/{id}/licenses")
    @RequirePerm("store:update")
    public ApiResult<Void> addLicense(@PathVariable Long id, @RequestBody StoreLicense license) {
        checkScope(id);
        license.setId(null);
        license.setTenantId(AuthContext.require().getTenantId());
        license.setStoreId(id);
        licenseMapper.insert(license);
        return ApiResult.ok();
    }

    @DeleteMapping("/licenses/{licenseId}")
    @RequirePerm("store:update")
    public ApiResult<Void> deleteLicense(@PathVariable Long licenseId) {
        licenseMapper.deleteById(licenseId);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/audits")
    @RequirePerm("store:view")
    public ApiResult<List<StoreAuditRecord>> audits(@PathVariable Long id) {
        checkScope(id);
        return ApiResult.ok(auditMapper.selectList(new LambdaQueryWrapper<StoreAuditRecord>()
                .eq(StoreAuditRecord::getStoreId, id)
                .orderByDesc(StoreAuditRecord::getCheckDate)));
    }

    @PostMapping("/{id}/audits")
    @RequirePerm("store:update")
    public ApiResult<Void> addAudit(@PathVariable Long id, @RequestBody StoreAuditRecord record) {
        checkScope(id);
        record.setId(null);
        record.setTenantId(AuthContext.require().getTenantId());
        record.setStoreId(id);
        auditMapper.insert(record);
        return ApiResult.ok();
    }

    private Long scopedStoreId(LoginUser user) {
        if (("STORE_MANAGER".equals(user.getRoleCode()) || "CASHIER".equals(user.getRoleCode()))
                && user.getStoreId() != null) {
            return user.getStoreId();
        }
        return null;
    }

    private void checkScope(Long storeId) {
        LoginUser user = AuthContext.require();
        if (("STORE_MANAGER".equals(user.getRoleCode()) || "CASHIER".equals(user.getRoleCode()))
                && user.getStoreId() != null && !user.getStoreId().equals(storeId)) {
            throw BizException.forbidden("仅可访问本门店数据");
        }
    }
}
