package com.retail.convenience.modules.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.TenantContext;
import com.retail.convenience.common.tenant.TenantGuardService;
import com.retail.convenience.modules.platform.dto.RenewRequest;
import com.retail.convenience.modules.platform.dto.TenantCreateRequest;
import com.retail.convenience.modules.platform.dto.TenantUpdateRequest;
import com.retail.convenience.modules.platform.entity.PlatformPlan;
import com.retail.convenience.modules.platform.entity.PlatformBill;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.entity.PlatformTenantAdmin;
import com.retail.convenience.modules.platform.entity.PlatformTenantQuotaUsage;
import com.retail.convenience.modules.platform.mapper.PlatformPlanMapper;
import com.retail.convenience.modules.platform.mapper.PlatformBillMapper;
import com.retail.convenience.modules.platform.mapper.PlatformTenantAdminMapper;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import com.retail.convenience.modules.platform.mapper.PlatformTenantQuotaUsageMapper;
import com.retail.convenience.modules.product.entity.ProductCategory;
import com.retail.convenience.modules.product.mapper.ProductCategoryMapper;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.mall.entity.DeliveryRule;
import com.retail.convenience.modules.mall.mapper.DeliveryRuleMapper;
import com.retail.convenience.modules.member.entity.MemberLevel;
import com.retail.convenience.modules.member.mapper.MemberLevelMapper;
import com.retail.convenience.modules.mp.entity.MpAccount;
import com.retail.convenience.modules.mp.mapper.MpAccountMapper;
import com.retail.convenience.modules.system.entity.SysConfig;
import com.retail.convenience.modules.system.entity.SysFile;
import com.retail.convenience.modules.system.entity.SysLanguage;
import com.retail.convenience.modules.system.entity.SysMenu;
import com.retail.convenience.modules.system.entity.SysMessageChannel;
import com.retail.convenience.modules.system.entity.SysRole;
import com.retail.convenience.modules.system.entity.SysSmsChannel;
import com.retail.convenience.modules.system.entity.SysStorageConfig;
import com.retail.convenience.modules.system.entity.SysUser;
import com.retail.convenience.modules.system.mapper.SysConfigMapper;
import com.retail.convenience.modules.system.mapper.SysFileMapper;
import com.retail.convenience.modules.system.mapper.SysLanguageMapper;
import com.retail.convenience.modules.system.mapper.SysMenuMapper;
import com.retail.convenience.modules.system.mapper.SysMessageChannelMapper;
import com.retail.convenience.modules.system.mapper.SysRoleMapper;
import com.retail.convenience.modules.system.mapper.SysSmsChannelMapper;
import com.retail.convenience.modules.system.mapper.SysStorageConfigMapper;
import com.retail.convenience.modules.system.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlatformTenantService {

    private final PlatformTenantMapper tenantMapper;
    private final PlatformPlanMapper planMapper;
    private final PlatformTenantQuotaUsageMapper usageMapper;
    private final PlatformTenantAdminMapper tenantAdminMapper;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysConfigMapper configMapper;
    private final SysLanguageMapper languageMapper;
    private final SysMessageChannelMapper messageChannelMapper;
    private final SysSmsChannelMapper smsChannelMapper;
    private final SysStorageConfigMapper storageConfigMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final StoreMapper storeMapper;
    private final ProductCategoryMapper categoryMapper;
    private final DeliveryRuleMapper deliveryRuleMapper;
    private final MpAccountMapper mpAccountMapper;
    private final SysFileMapper fileMapper;
    private final PlatformBillMapper billMapper;
    private final PlatformQuotaService quotaService;
    private final TenantGuardService tenantGuardService;
    private final PasswordEncoder passwordEncoder;

    public PlatformTenantService(PlatformTenantMapper tenantMapper,
                                 PlatformPlanMapper planMapper,
                                 PlatformTenantQuotaUsageMapper usageMapper,
                                 PlatformTenantAdminMapper tenantAdminMapper,
                                 SysUserMapper userMapper,
                                 SysMenuMapper menuMapper,
                                 SysRoleMapper roleMapper,
                                 SysConfigMapper configMapper,
                                 SysLanguageMapper languageMapper,
                                 SysMessageChannelMapper messageChannelMapper,
                                 SysSmsChannelMapper smsChannelMapper,
                                 SysStorageConfigMapper storageConfigMapper,
                                 MemberLevelMapper memberLevelMapper,
                                 StoreMapper storeMapper,
                                 ProductCategoryMapper categoryMapper,
                                 DeliveryRuleMapper deliveryRuleMapper,
                                 MpAccountMapper mpAccountMapper,
                                 SysFileMapper fileMapper,
                                 PlatformBillMapper billMapper,
                                 PlatformQuotaService quotaService,
                                 TenantGuardService tenantGuardService,
                                 PasswordEncoder passwordEncoder) {
        this.tenantMapper = tenantMapper;
        this.planMapper = planMapper;
        this.usageMapper = usageMapper;
        this.tenantAdminMapper = tenantAdminMapper;
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.configMapper = configMapper;
        this.languageMapper = languageMapper;
        this.messageChannelMapper = messageChannelMapper;
        this.smsChannelMapper = smsChannelMapper;
        this.storageConfigMapper = storageConfigMapper;
        this.memberLevelMapper = memberLevelMapper;
        this.storeMapper = storeMapper;
        this.categoryMapper = categoryMapper;
        this.deliveryRuleMapper = deliveryRuleMapper;
        this.mpAccountMapper = mpAccountMapper;
        this.fileMapper = fileMapper;
        this.billMapper = billMapper;
        this.quotaService = quotaService;
        this.tenantGuardService = tenantGuardService;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<Map<String, Object>> page(int page, int size, String keyword, String status, Long planId) {
        LambdaQueryWrapper<PlatformTenant> query = new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getDeleted, 0)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(PlatformTenant::getName, keyword)
                        .or().like(PlatformTenant::getTenantCode, keyword))
                .eq(status != null && !status.isBlank(), PlatformTenant::getStatus, status)
                .eq(planId != null, PlatformTenant::getPlanId, planId)
                .orderByDesc(PlatformTenant::getCreatedAt);
        Page<PlatformTenant> result = tenantMapper.selectPage(new Page<>(page, size), query);
        Map<Long, PlatformPlan> plans = planMapper.selectList(null).stream()
                .collect(Collectors.toMap(PlatformPlan::getPlanId, Function.identity()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PlatformTenant tenant : result.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenant.getTenantId());
            row.put("tenantCode", tenant.getTenantCode());
            row.put("name", tenant.getName());
            row.put("industry", tenant.getIndustry());
            row.put("contact", tenant.getContact());
            row.put("phone", tenant.getPhone());
            row.put("planId", tenant.getPlanId());
            PlatformPlan plan = plans.get(tenant.getPlanId());
            row.put("planName", plan == null ? "-" : plan.getName());
            row.put("status", tenant.getStatus());
            row.put("trialEndAt", tenant.getTrialEndAt());
            row.put("expireAt", tenant.getExpireAt());
            row.put("seatLimit", tenant.getSeatLimit());
            row.put("storeLimit", tenant.getStoreLimit());
            row.put("mpAccountLimit", tenant.getMpAccountLimit());
            row.put("storageQuotaGb", tenant.getStorageQuotaGb());
            row.put("smsQuota", tenant.getSmsQuota());
            row.put("apiQuota", tenant.getApiQuota());
            row.put("createdAt", tenant.getCreatedAt());
            row.put("usage", usage(tenant.getTenantId()));
            rows.add(row);
        }
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return out;
    }

    public Map<String, Object> detail(Long tenantId) {
        PlatformTenant tenant = require(tenantId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tenant", tenant);
        map.put("plan", planMapper.selectById(tenant.getPlanId()));
        map.put("usage", usage(tenantId));
        return map;
    }

    @Transactional
    public Map<String, Object> create(TenantCreateRequest request) {
        long startedAt = System.currentTimeMillis();
        if (tenantMapper.selectCount(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getTenantCode, request.getTenantCode())
                .eq(PlatformTenant::getDeleted, 0)) > 0) {
            throw new BizException("租户编码已存在");
        }
        PlatformPlan plan = planMapper.selectById(request.getPlanId());
        if (plan == null) {
            throw BizException.notFound("套餐不存在");
        }
        PlatformTenant tenant = new PlatformTenant();
        tenant.setTenantCode(request.getTenantCode().trim());
        tenant.setName(request.getName().trim());
        tenant.setIndustry(request.getIndustry());
        tenant.setContact(request.getContact());
        tenant.setPhone(request.getPhone());
        tenant.setPlanId(plan.getPlanId());
        tenant.setStatus("TRIAL");
        tenant.setTrialEndAt(LocalDateTime.now().plusDays(plan.getTrialDays() == null ? 14 : plan.getTrialDays()));
        tenant.setSeatLimit(plan.getSeatLimit());
        tenant.setStoreLimit(request.getStoreLimit() == null ? plan.getStoreLimit() : request.getStoreLimit());
        tenant.setMpAccountLimit(plan.getMpAccountLimit());
        tenant.setStorageQuotaGb(plan.getStorageQuotaGb());
        tenant.setSmsQuota(plan.getSmsQuota());
        tenant.setApiQuota(plan.getApiQuota());
        tenant.setOverdueStrategy("LIMIT_WRITE");
        tenant.setDeleted(0);
        tenantMapper.insert(tenant);

        Long newId = tenant.getTenantId();
        PlatformTenant template = tenantMapper.selectOne(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getTenantCode, "yxbl")
                .eq(PlatformTenant::getDeleted, 0)
                .last("limit 1"));
        Long templateId = template == null ? null : template.getTenantId();
        try {
            if (templateId != null) {
                TenantContext.set(templateId, template.getTenantCode());
                copyMenus(templateId, newId);
                copyRoles(templateId, newId);
                copyConfigs(templateId, newId, tenant.getName());
                copyLanguages(templateId, newId);
                copyMessageChannels(templateId, newId);
                copySmsChannels(templateId, newId);
                copyStorage(templateId, newId);
                copyMemberLevels(templateId, newId);
            }
            TenantContext.set(newId, tenant.getTenantCode());
            createAdmin(newId, request);
            Store store = createDefaultStore(newId, tenant.getName());
            createDefaultCategories(newId);
            createDeliveryRule(newId, store.getId());
            PlatformTenantAdmin tenantAdmin = new PlatformTenantAdmin();
            tenantAdmin.setTenantId(newId);
            tenantAdmin.setUserId(userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, newId)
                    .eq(SysUser::getUsername, request.getAdminUsername())
                    .last("limit 1")).getId());
            tenantAdmin.setIsPrimary(1);
            tenantAdminMapper.insert(tenantAdmin);
            quotaService.refreshDailyUsage(newId);
            tenantGuardService.evict(newId);
        } finally {
            TenantContext.clear();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", newId);
        result.put("tenantCode", tenant.getTenantCode());
        result.put("adminUsername", request.getAdminUsername());
        result.put("initialPassword", request.getAdminPassword());
        result.put("loginUrl", "/login?tenantCode=" + tenant.getTenantCode());
        result.put("initializedInSeconds", Math.round((System.currentTimeMillis() - startedAt) / 10.0) / 100.0);
        return result;
    }

    public void update(Long tenantId, TenantUpdateRequest request) {
        PlatformTenant tenant = require(tenantId);
        if (request.getName() != null) tenant.setName(request.getName());
        if (request.getIndustry() != null) tenant.setIndustry(request.getIndustry());
        if (request.getContact() != null) tenant.setContact(request.getContact());
        if (request.getPhone() != null) tenant.setPhone(request.getPhone());
        if (request.getPlanId() != null) tenant.setPlanId(request.getPlanId());
        if (request.getSeatLimit() != null) tenant.setSeatLimit(request.getSeatLimit());
        if (request.getStoreLimit() != null) tenant.setStoreLimit(request.getStoreLimit());
        if (request.getMpAccountLimit() != null) tenant.setMpAccountLimit(request.getMpAccountLimit());
        if (request.getStorageQuotaGb() != null) tenant.setStorageQuotaGb(request.getStorageQuotaGb());
        if (request.getSmsQuota() != null) tenant.setSmsQuota(request.getSmsQuota());
        if (request.getApiQuota() != null) tenant.setApiQuota(request.getApiQuota());
        if (request.getExpireAt() != null) tenant.setExpireAt(request.getExpireAt());
        if (request.getRemark() != null) tenant.setRemark(request.getRemark());
        tenantMapper.updateById(tenant);
        tenantGuardService.evict(tenantId);
    }

    public void changeStatus(Long tenantId, String status) {
        PlatformTenant tenant = require(tenantId);
        if (!List.of("ACTIVE", "SUSPENDED", "TRIAL", "OVERDUE").contains(status)) {
            throw new BizException("非法的租户状态");
        }
        tenant.setStatus(status);
        if ("ACTIVE".equals(status) && tenant.getExpireAt() == null) {
            tenant.setExpireAt(LocalDateTime.now().plusYears(1));
        }
        tenantMapper.updateById(tenant);
        tenantGuardService.evict(tenantId);
    }

    public void renew(Long tenantId, RenewRequest request) {
        PlatformTenant tenant = require(tenantId);
        int months = request.getMonths() == null || request.getMonths() <= 0 ? 12 : request.getMonths();
        LocalDateTime base = tenant.getExpireAt() == null || tenant.getExpireAt().isBefore(LocalDateTime.now())
                ? LocalDateTime.now() : tenant.getExpireAt();
        tenant.setExpireAt(base.plusMonths(months));
        tenant.setStatus("ACTIVE");
        tenantMapper.updateById(tenant);
        tenantGuardService.evict(tenantId);
    }

    public void delete(Long tenantId) {
        PlatformTenant tenant = require(tenantId);
        tenant.setDeleted(1);
        tenant.setStatus("SUSPENDED");
        tenantMapper.updateById(tenant);
        tenantGuardService.evict(tenantId);
    }

    public Map<String, Object> dashboard() {
        List<PlatformTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getDeleted, 0));
        Map<String, Long> byStatus = tenants.stream()
                .collect(Collectors.groupingBy(PlatformTenant::getStatus, Collectors.counting()));
        BigDecimal revenue = billMapper.selectList(new LambdaQueryWrapper<PlatformBill>()
                        .eq(PlatformBill::getStatus, "PAID"))
                .stream().map(PlatformBill::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("revenue", revenue);
        map.put("tenantTotal", tenants.size());
        map.put("activeTotal", byStatus.getOrDefault("ACTIVE", 0L) + byStatus.getOrDefault("TRIAL", 0L));
        map.put("trialTotal", byStatus.getOrDefault("TRIAL", 0L));
        map.put("suspendedTotal", byStatus.getOrDefault("SUSPENDED", 0L));
        long expiring = tenants.stream().filter(t -> t.getExpireAt() != null
                        && t.getExpireAt().isBefore(LocalDateTime.now().plusDays(30)))
                .count();
        map.put("expiringTotal", expiring);
        map.put("statusDistribution", byStatus);
        map.put("recentTenants", tenants.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(t -> Map.of("name", t.getName(), "code", t.getTenantCode(),
                        "status", t.getStatus(), "createdAt", t.getCreatedAt()))
                .toList());
        return map;
    }

    public Map<String, Object> usage(Long tenantId) {
        return quotaService.usage(tenantId);
    }

    private PlatformTenant require(Long tenantId) {
        PlatformTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || (tenant.getDeleted() != null && tenant.getDeleted() == 1)) {
            throw BizException.notFound("租户不存在");
        }
        return tenant;
    }

    private void copyMenus(Long from, Long to) {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, from).orderByAsc(SysMenu::getId));
        Map<Long, Long> idMap = new HashMap<>();
        for (SysMenu source : menus) {
            SysMenu copy = new SysMenu();
            copy.setTenantId(to);
            copy.setEndType(source.getEndType());
            copy.setParentId(0L);
            copy.setTitle(source.getTitle());
            copy.setTitleEn(source.getTitleEn());
            copy.setIcon(source.getIcon());
            copy.setPath(source.getPath());
            copy.setComponent(source.getComponent());
            copy.setPermission(source.getPermission());
            copy.setSortNo(source.getSortNo());
            copy.setVisible(source.getVisible());
            copy.setStatus(source.getStatus());
            menuMapper.insert(copy);
            idMap.put(source.getId(), copy.getId());
        }
        for (SysMenu source : menus) {
            Long newParent = idMap.get(source.getParentId());
            if (newParent != null && newParent > 0) {
                SysMenu update = new SysMenu();
                update.setId(idMap.get(source.getId()));
                update.setParentId(newParent);
                menuMapper.updateById(update);
            }
        }
    }

    private void copyRoles(Long from, Long to) {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, from));
        for (SysRole source : roles) {
            SysRole copy = new SysRole();
            copy.setTenantId(to);
            copy.setRoleCode(source.getRoleCode());
            copy.setName(source.getName());
            copy.setPermissions(source.getPermissions());
            copy.setDataScope(source.getDataScope());
            copy.setStatus(source.getStatus());
            roleMapper.insert(copy);
        }
    }

    private void copyConfigs(Long from, Long to, String tenantName) {
        List<SysConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getTenantId, from));
        for (SysConfig source : configs) {
            SysConfig copy = new SysConfig();
            copy.setTenantId(to);
            copy.setConfigKey(source.getConfigKey());
            copy.setConfigValue("site.name".equals(source.getConfigKey()) ? tenantName : source.getConfigValue());
            copy.setGroupCode(source.getGroupCode());
            copy.setEncrypted(source.getEncrypted());
            copy.setRemark(source.getRemark());
            configMapper.insert(copy);
        }
    }

    private void copyLanguages(Long from, Long to) {
        for (SysLanguage source : languageMapper.selectList(new LambdaQueryWrapper<SysLanguage>()
                .eq(SysLanguage::getTenantId, from))) {
            SysLanguage copy = new SysLanguage();
            copy.setTenantId(to);
            copy.setCode(source.getCode());
            copy.setName(source.getName());
            copy.setEnabled(source.getEnabled());
            copy.setSortNo(source.getSortNo());
            languageMapper.insert(copy);
        }
    }

    private void copyMessageChannels(Long from, Long to) {
        for (SysMessageChannel source : messageChannelMapper.selectList(new LambdaQueryWrapper<SysMessageChannel>()
                .eq(SysMessageChannel::getTenantId, from))) {
            SysMessageChannel copy = new SysMessageChannel();
            copy.setTenantId(to);
            copy.setChannel(source.getChannel());
            copy.setEnabled(source.getEnabled());
            copy.setConfig(source.getConfig());
            messageChannelMapper.insert(copy);
        }
    }

    private void copySmsChannels(Long from, Long to) {
        for (SysSmsChannel source : smsChannelMapper.selectList(new LambdaQueryWrapper<SysSmsChannel>()
                .eq(SysSmsChannel::getTenantId, from))) {
            SysSmsChannel copy = new SysSmsChannel();
            copy.setTenantId(to);
            copy.setProvider(source.getProvider());
            copy.setEnabled(source.getEnabled());
            copy.setConfig(source.getConfig());
            smsChannelMapper.insert(copy);
        }
    }

    private void copyStorage(Long from, Long to) {
        SysStorageConfig source = storageConfigMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getTenantId, from).last("limit 1"));
        if (source != null) {
            SysStorageConfig copy = new SysStorageConfig();
            copy.setTenantId(to);
            copy.setDriver(source.getDriver());
            copy.setConfig(source.getConfig());
            copy.setEnabled(source.getEnabled());
            storageConfigMapper.insert(copy);
        }
    }

    private void copyMemberLevels(Long from, Long to) {
        for (MemberLevel source : memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getTenantId, from))) {
            MemberLevel copy = new MemberLevel();
            copy.setTenantId(to);
            copy.setCode(source.getCode());
            copy.setName(source.getName());
            copy.setMinPoints(source.getMinPoints());
            copy.setDiscount(source.getDiscount());
            copy.setBenefits(source.getBenefits());
            memberLevelMapper.insert(copy);
        }
    }

    private void createAdmin(Long tenantId, TenantCreateRequest request) {
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(request.getAdminUsername());
        user.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        user.setRealName(request.getContact() == null ? "租户管理员" : request.getContact());
        user.setPhone(request.getPhone());
        user.setRoleCode("TENANT_ADMIN");
        user.setStatus("ACTIVE");
        user.setDeleted(0);
        userMapper.insert(user);
    }

    private Store createDefaultStore(Long tenantId, String tenantName) {
        Store store = new Store();
        store.setTenantId(tenantId);
        store.setCode("S001");
        store.setName(tenantName + "·总部店");
        store.setType("DIRECT");
        store.setRegion("总部");
        store.setBusinessHours("07:00-23:00");
        store.setDeliveryRadiusM(3000);
        store.setDeliveryEnabled(1);
        store.setOnlineEnabled(1);
        store.setStatus("OPEN");
        store.setDeleted(0);
        storeMapper.insert(store);
        return store;
    }

    private void createDefaultCategories(Long tenantId) {
        for (String[] def : List.of(new String[]{"鲜食", "2"}, new String[]{"饮料", "7"},
                new String[]{"零食", "7"}, new String[]{"日用", "15"})) {
            ProductCategory category = new ProductCategory();
            category.setTenantId(tenantId);
            category.setParentId(0L);
            category.setName(def[0]);
            category.setSortNo(10);
            category.setNearExpiryDays(Integer.parseInt(def[1]));
            category.setStatus("ACTIVE");
            category.setDeleted(0);
            categoryMapper.insert(category);
        }
    }

    private void createDeliveryRule(Long tenantId, Long storeId) {
        DeliveryRule rule = new DeliveryRule();
        rule.setTenantId(tenantId);
        rule.setStoreId(storeId);
        rule.setMinOrderAmount(new BigDecimal("20"));
        rule.setDeliveryFee(new BigDecimal("3"));
        rule.setFreeThreshold(new BigDecimal("49"));
        rule.setRadiusM(3000);
        rule.setTimeSlots("[\"09:00-12:00\",\"12:00-18:00\",\"18:00-22:00\"]");
        rule.setEnabled(1);
        deliveryRuleMapper.insert(rule);
    }

    private BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : v;
    }
}
