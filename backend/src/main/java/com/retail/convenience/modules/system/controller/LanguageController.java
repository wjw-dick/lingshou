package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.system.entity.SysLanguage;
import com.retail.convenience.modules.system.mapper.SysLanguageMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/system/languages")
public class LanguageController {

    private final SysLanguageMapper languageMapper;

    public LanguageController(SysLanguageMapper languageMapper) {
        this.languageMapper = languageMapper;
    }

    @GetMapping
    @RequirePerm("system:view")
    public ApiResult<List<SysLanguage>> list() {
        return ApiResult.ok(languageMapper.selectList(new LambdaQueryWrapper<SysLanguage>()
                .orderByAsc(SysLanguage::getSortNo)));
    }

    @PostMapping
    @RequirePerm("system:update")
    public ApiResult<Void> create(@RequestBody SysLanguage language) {
        if (language.getCode() == null || language.getCode().isBlank()) {
            throw new BizException("语言代码不能为空");
        }
        language.setId(null);
        language.setTenantId(AuthContext.require().getTenantId());
        if (language.getEnabled() == null) {
            language.setEnabled(1);
        }
        languageMapper.insert(language);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody SysLanguage language) {
        language.setId(id);
        languageMapper.updateById(language);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> delete(@PathVariable Long id) {
        languageMapper.deleteById(id);
        return ApiResult.ok();
    }
}
