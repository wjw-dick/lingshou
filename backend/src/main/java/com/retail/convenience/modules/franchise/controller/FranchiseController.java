package com.retail.convenience.modules.franchise.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.franchise.entity.FranchiseCommission;
import com.retail.convenience.modules.franchise.entity.Franchisee;
import com.retail.convenience.modules.franchise.entity.FranchiseeQualification;
import com.retail.convenience.modules.franchise.entity.FranchiseeStore;
import com.retail.convenience.modules.franchise.mapper.FranchiseCommissionMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeQualificationMapper;
import com.retail.convenience.modules.franchise.mapper.FranchiseeStoreMapper;
import com.retail.convenience.modules.store.entity.Store;
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

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/franchisees")
public class FranchiseController {

    private final FranchiseeMapper franchiseeMapper;
    private final FranchiseeStoreMapper storeRelMapper;
    private final FranchiseCommissionMapper commissionMapper;
    private final FranchiseeQualificationMapper qualificationMapper;
    private final StoreMapper storeMapper;

    public FranchiseController(FranchiseeMapper franchiseeMapper, FranchiseeStoreMapper storeRelMapper,
                               FranchiseCommissionMapper commissionMapper,
                               FranchiseeQualificationMapper qualificationMapper, StoreMapper storeMapper) {
        this.franchiseeMapper = franchiseeMapper;
        this.storeRelMapper = storeRelMapper;
        this.commissionMapper = commissionMapper;
        this.qualificationMapper = qualificationMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    @RequirePerm("franchise:view")
    public ApiResult<PageResult<Franchisee>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<Franchisee> result = franchiseeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Franchisee>()
                        .eq(Franchisee::getDeleted, 0)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Franchisee::getName, keyword).or().like(Franchisee::getCode, keyword))
                        .eq(status != null && !status.isBlank(), Franchisee::getStatus, status)
                        .orderByAsc(Franchisee::getCode));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePerm("franchise:view")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Franchisee franchisee = require(id);
        List<FranchiseeStore> relations = storeRelMapper.selectList(new LambdaQueryWrapper<FranchiseeStore>()
                .eq(FranchiseeStore::getFranchiseeId, id));
        List<Long> storeIds = relations.stream().map(FranchiseeStore::getStoreId).toList();
        Map<Long, Store> stores = storeIds.isEmpty() ? Map.of() : storeMapper.selectList(
                        new LambdaQueryWrapper<Store>().in(Store::getId, storeIds))
                .stream().collect(Collectors.toMap(Store::getId, Function.identity()));
        List<FranchiseCommission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<FranchiseCommission>()
                        .eq(FranchiseCommission::getFranchiseeId, id)
                        .orderByDesc(FranchiseCommission::getCreatedAt));
        List<FranchiseeQualification> qualifications = qualificationMapper.selectList(
                new LambdaQueryWrapper<FranchiseeQualification>()
                        .eq(FranchiseeQualification::getFranchiseeId, id));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("franchisee", franchisee);
        map.put("stores", relations.stream().map(rel -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("relation", rel);
            Store store = stores.get(rel.getStoreId());
            row.put("storeName", store == null ? "-" : store.getName());
            row.put("storeCode", store == null ? "" : store.getCode());
            return row;
        }).toList());
        map.put("commissions", commissions);
        map.put("qualifications", qualifications);
        return ApiResult.ok(map);
    }

    @PostMapping
    @RequirePerm("franchise:create")
    public ApiResult<Franchisee> create(@RequestBody Franchisee franchisee) {
        franchisee.setId(null);
        franchisee.setTenantId(AuthContext.require().getTenantId());
        franchisee.setDeleted(0);
        if (franchisee.getUsedCredit() == null) {
            franchisee.setUsedCredit(java.math.BigDecimal.ZERO);
        }
        franchiseeMapper.insert(franchisee);
        return ApiResult.ok(franchisee);
    }

    @PutMapping("/{id}")
    @RequirePerm("franchise:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Franchisee franchisee) {
        franchisee.setId(id);
        franchiseeMapper.updateById(franchisee);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("franchise:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        franchiseeMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/stores")
    @RequirePerm("franchise:update")
    public ApiResult<Void> bindStore(@PathVariable Long id, @RequestParam Long storeId) {
        require(id);
        if (storeRelMapper.selectCount(new LambdaQueryWrapper<FranchiseeStore>()
                .eq(FranchiseeStore::getFranchiseeId, id)
                .eq(FranchiseeStore::getStoreId, storeId)) > 0) {
            throw new BizException("门店已绑定");
        }
        FranchiseeStore relation = new FranchiseeStore();
        relation.setFranchiseeId(id);
        relation.setStoreId(storeId);
        relation.setJoinDate(LocalDate.now());
        relation.setStatus("ACTIVE");
        storeRelMapper.insert(relation);
        return ApiResult.ok();
    }

    @DeleteMapping("/stores/{relationId}")
    @RequirePerm("franchise:update")
    public ApiResult<Void> unbindStore(@PathVariable Long relationId) {
        storeRelMapper.deleteById(relationId);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/qualifications")
    @RequirePerm("franchise:update")
    public ApiResult<Void> addQualification(@PathVariable Long id,
                                            @RequestBody FranchiseeQualification qualification) {
        require(id);
        qualification.setId(null);
        qualification.setFranchiseeId(id);
        qualification.setTenantId(AuthContext.require().getTenantId());
        qualificationMapper.insert(qualification);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/commissions")
    @RequirePerm("franchise:view")
    public ApiResult<List<FranchiseCommission>> commissions(@PathVariable Long id) {
        return ApiResult.ok(commissionMapper.selectList(new LambdaQueryWrapper<FranchiseCommission>()
                .eq(FranchiseCommission::getFranchiseeId, id)
                .orderByDesc(FranchiseCommission::getCreatedAt)));
    }

    private Franchisee require(Long id) {
        Franchisee franchisee = franchiseeMapper.selectById(id);
        if (franchisee == null) {
            throw BizException.notFound("加盟商不存在");
        }
        return franchisee;
    }
}
