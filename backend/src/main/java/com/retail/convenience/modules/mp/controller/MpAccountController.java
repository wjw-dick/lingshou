package com.retail.convenience.modules.mp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.mp.entity.MpAccount;
import com.retail.convenience.modules.mp.entity.MpFans;
import com.retail.convenience.modules.mp.entity.MpMiniApp;
import com.retail.convenience.modules.mp.entity.MpPublish;
import com.retail.convenience.modules.mp.entity.MpTakeoverCheck;
import com.retail.convenience.modules.mp.mapper.MpAccountMapper;
import com.retail.convenience.modules.mp.mapper.MpFansMapper;
import com.retail.convenience.modules.mp.mapper.MpMiniAppMapper;
import com.retail.convenience.modules.mp.mapper.MpPublishMapper;
import com.retail.convenience.modules.mp.mapper.MpTakeoverCheckMapper;
import com.retail.convenience.modules.platform.service.PlatformQuotaService;
import com.retail.convenience.modules.system.entity.SysMenu;
import com.retail.convenience.modules.system.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/mp/accounts")
public class MpAccountController {

    private final MpAccountMapper accountMapper;
    private final MpMiniAppMapper miniAppMapper;
    private final MpFansMapper fansMapper;
    private final MpPublishMapper publishMapper;
    private final MpTakeoverCheckMapper takeoverMapper;
    private final SysMenuMapper menuMapper;
    private final PlatformQuotaService quotaService;
    private final String cryptoSecret;

    public MpAccountController(MpAccountMapper accountMapper, MpMiniAppMapper miniAppMapper,
                               MpFansMapper fansMapper, MpPublishMapper publishMapper,
                               MpTakeoverCheckMapper takeoverMapper, SysMenuMapper menuMapper,
                               PlatformQuotaService quotaService,
                               @Value("${app.jwt.secret}") String cryptoSecret) {
        this.accountMapper = accountMapper;
        this.miniAppMapper = miniAppMapper;
        this.fansMapper = fansMapper;
        this.publishMapper = publishMapper;
        this.takeoverMapper = takeoverMapper;
        this.menuMapper = menuMapper;
        this.quotaService = quotaService;
        this.cryptoSecret = cryptoSecret;
    }

    @GetMapping
    @RequirePerm("mp:view")
    public ApiResult<List<Map<String, Object>>> list() {
        return ApiResult.ok(accountMapper.selectList(new LambdaQueryWrapper<MpAccount>()
                        .orderByAsc(MpAccount::getId))
                .stream().map(this::masked).toList());
    }

    @PostMapping
    @RequirePerm("mp:update")
    public ApiResult<MpAccount> create(@RequestBody MpAccount account) {
        if (account.getAppId() == null || account.getAppId().isBlank()) {
            throw new BizException("AppID 不能为空");
        }
        quotaService.assertMpAccount(AuthContext.require().getTenantId());
        account.setId(null);
        account.setTenantId(AuthContext.require().getTenantId());
        encryptSecrets(account, false);
        account.setServerUrl(serverUrl(account.getAppId()));
        account.setStatus("ACTIVE");
        account.setApiQuotaUsed(0);
        accountMapper.insert(account);
        return ApiResult.ok(account);
    }

    @PutMapping("/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody MpAccount request) {
        MpAccount account = require(id);
        account.setName(request.getName());
        account.setAppId(request.getAppId());
        account.setOauthDomain(request.getOauthDomain());
        account.setJsDomain(request.getJsDomain());
        account.setStatus(request.getStatus());
        if (request.getAppSecret() != null && !request.getAppSecret().isBlank() && !request.getAppSecret().contains("*")) {
            account.setAppSecret(AesUtil.encrypt(request.getAppSecret(), cryptoSecret));
        }
        if (request.getToken() != null && !request.getToken().isBlank() && !request.getToken().contains("*")) {
            account.setToken(AesUtil.encrypt(request.getToken(), cryptoSecret));
        }
        if (request.getAesKey() != null && !request.getAesKey().isBlank() && !request.getAesKey().contains("*")) {
            account.setAesKey(AesUtil.encrypt(request.getAesKey(), cryptoSecret));
        }
        account.setServerUrl(serverUrl(request.getAppId() == null ? account.getAppId() : request.getAppId()));
        accountMapper.updateById(account);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> delete(@PathVariable Long id) {
        accountMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reset-quota")
    @RequirePerm("mp:update")
    public ApiResult<Void> resetQuota(@PathVariable Long id) {
        MpAccount account = require(id);
        account.setApiQuotaUsed(0);
        accountMapper.updateById(account);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/qr-content")
    @RequirePerm("mp:view")
    public ApiResult<Map<String, Object>> qrContent(@PathVariable Long id) {
        MpAccount account = require(id);
        return ApiResult.ok(Map.of("content", account.getServerUrl() == null
                ? "weixin://app/" + account.getAppId() : account.getServerUrl()));
    }

    @PostMapping("/{id}/takeover-check")
    @RequirePerm("mp:update")
    public ApiResult<MpTakeoverCheck> takeoverCheck(@PathVariable Long id) {
        MpAccount account = require(id);
        long fans = fansMapper.selectCount(new LambdaQueryWrapper<MpFans>()
                .eq(MpFans::getAccountId, id));
        long publishes = publishMapper.selectCount(new LambdaQueryWrapper<MpPublish>()
                .eq(MpPublish::getAccountId, id));
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getEndType, "MP"));
        MpTakeoverCheck check = new MpTakeoverCheck();
        check.setAccountId(id);
        check.setServerUrlConfigured(account.getServerUrl() != null && account.getToken() != null
                && !account.getToken().isBlank() ? 1 : 0);
        check.setFansActive(fans > 0 ? 1 : 0);
        check.setMenuSnapshot("{\"menuCount\":" + menus.size() + "}");
        check.setPublishSnapshot("{\"publishCount\":" + publishes + "}");
        check.setConclusion(fans > 0 || publishes > 0 ? "OCCUPIED" : "PENDING");
        check.setRemark(fans > 0 ? "检测到现有粉丝/发表记录，接管前需与现网系统确认"
                : "未检测到粉丝与发表记录，可继续确认服务器回调占用情况");
        takeoverMapper.insert(check);
        return ApiResult.ok(check);
    }

    @GetMapping("/mini-apps")
    @RequirePerm("mp:view")
    public ApiResult<List<MpMiniApp>> miniApps() {
        return ApiResult.ok(miniAppMapper.selectList(new LambdaQueryWrapper<MpMiniApp>()
                .orderByAsc(MpMiniApp::getAppType)));
    }

    @PostMapping("/mini-apps")
    @RequirePerm("mp:update")
    public ApiResult<Void> saveMiniApp(@RequestBody MpMiniApp miniApp) {
        if (miniApp.getAppType() == null) {
            throw new BizException("请选择小程序类型");
        }
        MpMiniApp exists = miniAppMapper.selectOne(new LambdaQueryWrapper<MpMiniApp>()
                .eq(MpMiniApp::getAppType, miniApp.getAppType()).last("limit 1"));
        if (exists == null) {
            miniApp.setId(null);
            miniApp.setTenantId(AuthContext.require().getTenantId());
            if (miniApp.getAppSecret() != null && !miniApp.getAppSecret().isBlank()) {
                miniApp.setAppSecret(AesUtil.encrypt(miniApp.getAppSecret(), cryptoSecret));
            }
            miniAppMapper.insert(miniApp);
        } else {
            exists.setName(miniApp.getName());
            exists.setAppId(miniApp.getAppId());
            exists.setRequestDomain(miniApp.getRequestDomain());
            exists.setUploadDomain(miniApp.getUploadDomain());
            exists.setDownloadDomain(miniApp.getDownloadDomain());
            if (miniApp.getAppSecret() != null && !miniApp.getAppSecret().isBlank()
                    && !miniApp.getAppSecret().contains("*")) {
                exists.setAppSecret(AesUtil.encrypt(miniApp.getAppSecret(), cryptoSecret));
            }
            miniAppMapper.updateById(exists);
        }
        return ApiResult.ok();
    }

    private Map<String, Object> masked(MpAccount account) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", account.getId());
        map.put("name", account.getName());
        map.put("appId", account.getAppId());
        map.put("serverUrl", account.getServerUrl());
        map.put("oauthDomain", account.getOauthDomain());
        map.put("jsDomain", account.getJsDomain());
        map.put("status", account.getStatus());
        map.put("apiQuotaUsed", account.getApiQuotaUsed());
        map.put("appSecret", maskSecret(account.getAppSecret()));
        map.put("token", maskSecret(account.getToken()));
        map.put("aesKey", maskSecret(account.getAesKey()));
        return map;
    }

    private String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String plain;
        try {
            plain = AesUtil.decrypt(value, cryptoSecret);
        } catch (Exception e) {
            plain = value;
        }
        return AesUtil.mask(plain);
    }

    private void encryptSecrets(MpAccount account, boolean keepBlank) {
        if (account.getAppSecret() != null && !account.getAppSecret().isBlank()) {
            account.setAppSecret(AesUtil.encrypt(account.getAppSecret(), cryptoSecret));
        }
        if (account.getToken() != null && !account.getToken().isBlank()) {
            account.setToken(AesUtil.encrypt(account.getToken(), cryptoSecret));
        }
        if (account.getAesKey() != null && !account.getAesKey().isBlank()) {
            account.setAesKey(AesUtil.encrypt(account.getAesKey(), cryptoSecret));
        }
    }

    private String serverUrl(String appId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/admin-api/mp/open/").path(appId).toUriString();
    }

    private MpAccount require(Long id) {
        MpAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BizException.notFound("公众号账号不存在");
        }
        return account;
    }
}
