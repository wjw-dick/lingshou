package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.TenantContext;
import com.retail.convenience.common.tenant.TenantGuardService;
import com.retail.convenience.modules.cms.entity.CmsArticle;
import com.retail.convenience.modules.cms.entity.CmsBanner;
import com.retail.convenience.modules.cms.entity.CmsFranchiseApplication;
import com.retail.convenience.modules.cms.mapper.CmsArticleMapper;
import com.retail.convenience.modules.cms.mapper.CmsBannerMapper;
import com.retail.convenience.modules.cms.mapper.CmsFranchiseApplicationMapper;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.system.entity.SysConfig;
import com.retail.convenience.modules.system.entity.SysLanguage;
import com.retail.convenience.modules.system.entity.SysMenu;
import com.retail.convenience.modules.system.mapper.SysConfigMapper;
import com.retail.convenience.modules.system.mapper.SysLanguageMapper;
import com.retail.convenience.modules.system.mapper.SysMenuMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicSiteController {

    private final TenantGuardService tenantGuardService;
    private final SysConfigMapper configMapper;
    private final SysLanguageMapper languageMapper;
    private final SysMenuMapper menuMapper;
    private final CmsBannerMapper bannerMapper;
    private final CmsArticleMapper articleMapper;
    private final CmsFranchiseApplicationMapper franchiseApplicationMapper;
    private final StoreMapper storeMapper;

    public PublicSiteController(TenantGuardService tenantGuardService, SysConfigMapper configMapper,
                                SysLanguageMapper languageMapper, SysMenuMapper menuMapper,
                                CmsBannerMapper bannerMapper, CmsArticleMapper articleMapper,
                                CmsFranchiseApplicationMapper franchiseApplicationMapper,
                                StoreMapper storeMapper) {
        this.tenantGuardService = tenantGuardService;
        this.configMapper = configMapper;
        this.languageMapper = languageMapper;
        this.menuMapper = menuMapper;
        this.bannerMapper = bannerMapper;
        this.articleMapper = articleMapper;
        this.franchiseApplicationMapper = franchiseApplicationMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping("/site/{tenantCode}")
    public ApiResult<Map<String, Object>> site(@PathVariable String tenantCode,
                                               @RequestParam(defaultValue = "PORTAL") String endType) {
        PlatformTenant tenant = tenantGuardService.findByCode(tenantCode);
        if (tenant == null) {
            throw BizException.notFound("站点不存在");
        }
        tenantGuardService.findActive(tenant.getTenantId());
        TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
        try {
            Map<String, Object> site = new LinkedHashMap<>();
            site.put("tenantCode", tenant.getTenantCode());
            site.put("tenantName", tenant.getName());
            Map<String, String> configs = new LinkedHashMap<>();
            List<SysConfig> list = configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getTenantId, tenant.getTenantId())
                    .eq(SysConfig::getEncrypted, 0));
            for (SysConfig config : list) {
                if (config.getConfigKey().startsWith("site.")
                        || config.getConfigKey().startsWith("seo.")
                        || config.getConfigKey().startsWith("i18n.")
                        || config.getConfigKey().startsWith("delivery.")) {
                    configs.put(config.getConfigKey(), config.getConfigValue());
                }
            }
            site.put("configs", configs);
            List<SysLanguage> languages = languageMapper.selectList(new LambdaQueryWrapper<SysLanguage>()
                    .eq(SysLanguage::getEnabled, 1).orderByAsc(SysLanguage::getSortNo));
            site.put("languages", languages);
            List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getEndType, endType)
                    .eq(SysMenu::getVisible, 1)
                    .eq(SysMenu::getStatus, "ACTIVE")
                    .orderByAsc(SysMenu::getSortNo));
            site.put("menus", menus);
            site.put("banners", bannerMapper.selectList(new LambdaQueryWrapper<CmsBanner>()
                    .eq(CmsBanner::getPosition, endType)
                    .eq(CmsBanner::getStatus, "ACTIVE")
                    .orderByAsc(CmsBanner::getSortNo)));
            return ApiResult.ok(site);
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/site/{tenantCode}/articles")
    public ApiResult<List<CmsArticle>> articles(@PathVariable String tenantCode) {
        return ApiResult.ok(withTenant(tenantCode, () -> articleMapper.selectList(
                new LambdaQueryWrapper<CmsArticle>()
                        .eq(CmsArticle::getStatus, "PUBLISHED")
                        .orderByDesc(CmsArticle::getPublishedAt))));
    }

    @GetMapping("/site/{tenantCode}/articles/{id}")
    public ApiResult<CmsArticle> article(@PathVariable String tenantCode, @PathVariable Long id) {
        return ApiResult.ok(withTenant(tenantCode, () -> {
            CmsArticle article = articleMapper.selectById(id);
            if (article == null || !"PUBLISHED".equals(article.getStatus())) {
                throw BizException.notFound("文章不存在");
            }
            return article;
        }));
    }

    @GetMapping("/site/{tenantCode}/stores")
    public ApiResult<List<Store>> stores(@PathVariable String tenantCode,
                                         @RequestParam(required = false) String region,
                                         @RequestParam(required = false) String keyword) {
        return ApiResult.ok(withTenant(tenantCode, () -> storeMapper.selectList(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getDeleted, 0)
                        .eq(Store::getStatus, "OPEN")
                        .eq(region != null && !region.isBlank(), Store::getRegion, region)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Store::getName, keyword).or().like(Store::getAddress, keyword))
                        .orderByAsc(Store::getCode))));
    }

    @PostMapping("/site/{tenantCode}/franchise-applications")
    public ApiResult<Void> apply(@PathVariable String tenantCode,
                                 @RequestBody CmsFranchiseApplication application) {
        PlatformTenant tenant = tenantGuardService.findByCode(tenantCode);
        if (tenant == null) {
            throw BizException.notFound("站点不存在");
        }
        tenantGuardService.findActive(tenant.getTenantId());
        TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
        try {
            if (application.getContact() == null || application.getPhone() == null) {
                throw new BizException("请填写联系人和电话");
            }
            application.setId(null);
            application.setTenantId(tenant.getTenantId());
            application.setStatus("PENDING");
            franchiseApplicationMapper.insert(application);
            return ApiResult.ok();
        } finally {
            TenantContext.clear();
        }
    }

    private <T> T withTenant(String tenantCode, java.util.function.Supplier<T> supplier) {
        PlatformTenant tenant = tenantGuardService.findByCode(tenantCode);
        if (tenant == null) {
            throw BizException.notFound("站点不存在");
        }
        tenantGuardService.findActive(tenant.getTenantId());
        TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
        try {
            return supplier.get();
        } finally {
            TenantContext.clear();
        }
    }
}
