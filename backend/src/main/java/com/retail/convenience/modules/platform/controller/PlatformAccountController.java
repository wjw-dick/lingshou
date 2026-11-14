package com.retail.convenience.modules.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.platform.entity.PlatformUser;
import com.retail.convenience.modules.platform.mapper.PlatformUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform/accounts")
public class PlatformAccountController {

    private final PlatformUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PlatformAccountController(PlatformUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @RequirePerm("account:manage")
    public ApiResult<List<Map<String, Object>>> list() {
        return ApiResult.ok(userMapper.selectList(new LambdaQueryWrapper<PlatformUser>()
                        .orderByAsc(PlatformUser::getId))
                .stream().map(this::safe).toList());
    }

    @PostMapping
    @RequirePerm("account:manage")
    public ApiResult<Void> create(@RequestBody PlatformUser user) {
        if (userMapper.selectCount(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, user.getUsername())) > 0) {
            throw new BizException("账号已存在");
        }
        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword() == null ? "Admin@123" : user.getPassword()));
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @RequirePerm("account:manage")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody PlatformUser user) {
        PlatformUser exists = userMapper.selectById(id);
        if (exists == null) {
            throw BizException.notFound("账号不存在");
        }
        exists.setRealName(user.getRealName());
        exists.setRoleCode(user.getRoleCode());
        exists.setPhone(user.getPhone());
        exists.setStatus(user.getStatus());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            exists.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.updateById(exists);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("account:manage")
    public ApiResult<Void> delete(@PathVariable Long id) {
        if (AuthContext.require().getUserId().equals(id)) {
            throw new BizException("不能删除当前登录账号");
        }
        userMapper.deleteById(id);
        return ApiResult.ok();
    }

    private Map<String, Object> safe(PlatformUser user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("roleCode", user.getRoleCode());
        map.put("phone", user.getPhone());
        map.put("status", user.getStatus());
        map.put("lastLoginAt", user.getLastLoginAt());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }
}
