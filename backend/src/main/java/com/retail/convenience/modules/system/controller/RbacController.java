package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.platform.service.PlatformQuotaService;
import com.retail.convenience.modules.store.entity.Store;
import com.retail.convenience.modules.store.mapper.StoreMapper;
import com.retail.convenience.modules.system.entity.SysRole;
import com.retail.convenience.modules.system.entity.SysUser;
import com.retail.convenience.modules.system.mapper.SysRoleMapper;
import com.retail.convenience.modules.system.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant/system")
public class RbacController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final StoreMapper storeMapper;
    private final PasswordEncoder passwordEncoder;
    private final PlatformQuotaService quotaService;

    public RbacController(SysUserMapper userMapper, SysRoleMapper roleMapper, StoreMapper storeMapper,
                          PasswordEncoder passwordEncoder, PlatformQuotaService quotaService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.storeMapper = storeMapper;
        this.passwordEncoder = passwordEncoder;
        this.quotaService = quotaService;
    }

    @GetMapping("/users")
    @RequirePerm("system:view")
    public ApiResult<PageResult<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Long storeId) {
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeleted, 0)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword))
                        .eq(roleCode != null && !roleCode.isBlank(), SysUser::getRoleCode, roleCode)
                        .eq(storeId != null, SysUser::getStoreId, storeId)
                        .orderByDesc(SysUser::getCreatedAt));
        Map<Long, Store> stores = storeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        List<Map<String, Object>> rows = result.getRecords().stream().map(user -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", user.getId());
            row.put("username", user.getUsername());
            row.put("realName", user.getRealName());
            row.put("phone", user.getPhone());
            row.put("email", user.getEmail());
            row.put("roleCode", user.getRoleCode());
            row.put("storeId", user.getStoreId());
            Store store = user.getStoreId() == null ? null : stores.get(user.getStoreId());
            row.put("storeName", store == null ? "-" : store.getName());
            row.put("status", user.getStatus());
            row.put("lastLoginAt", user.getLastLoginAt());
            row.put("createdAt", user.getCreatedAt());
            return row;
        }).toList();
        PageResult<Map<String, Object>> out = new PageResult<>();
        out.setRecords(rows);
        out.setTotal(result.getTotal());
        out.setCurrent(result.getCurrent());
        out.setSize(result.getSize());
        return ApiResult.ok(out);
    }

    @PostMapping("/users")
    @RequirePerm("system:update")
    public ApiResult<Void> createUser(@RequestBody UserSaveRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BizException("登录账号不能为空");
        }
        Long tenantId = AuthContext.require().getTenantId();
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getDeleted, 0)) > 0) {
            throw new BizException("登录账号已存在");
        }
        quotaService.assertSeat(tenantId);
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword() == null
                ? "Admin@123" : request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRoleCode(request.getRoleCode());
        user.setStoreId(request.getStoreId());
        user.setSupplierId(request.getSupplierId());
        user.setFranchiseeId(request.getFranchiseeId());
        user.setStatus("ACTIVE");
        user.setDeleted(0);
        userMapper.insert(user);
        return ApiResult.ok();
    }

    @PutMapping("/users/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> updateUser(@PathVariable Long id, @RequestBody UserSaveRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRoleCode(request.getRoleCode());
        user.setStoreId(request.getStoreId());
        user.setSupplierId(request.getSupplierId());
        user.setFranchiseeId(request.getFranchiseeId());
        user.setStatus(request.getStatus());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(user);
        return ApiResult.ok();
    }

    @DeleteMapping("/users/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> deleteUser(@PathVariable Long id) {
        if (AuthContext.require().getUserId().equals(id)) {
            throw new BizException("不能删除当前登录账号");
        }
        userMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/roles")
    @RequirePerm("system:view")
    public ApiResult<List<SysRole>> roles() {
        return ApiResult.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, "ACTIVE").orderByAsc(SysRole::getId)));
    }

    @PostMapping("/roles")
    @RequirePerm("system:update")
    public ApiResult<Void> createRole(@RequestBody SysRole role) {
        if (role.getRoleCode() == null || role.getRoleCode().isBlank()) {
            throw new BizException("角色编码不能为空");
        }
        role.setId(null);
        role.setTenantId(AuthContext.require().getTenantId());
        role.setStatus("ACTIVE");
        roleMapper.insert(role);
        return ApiResult.ok();
    }

    @PutMapping("/roles/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleMapper.updateById(role);
        return ApiResult.ok();
    }

    @GetMapping("/permissions")
    @RequirePerm("system:view")
    public ApiResult<List<Map<String, String>>> permissions() {
        return ApiResult.ok(List.of(
                Map.of("group", "门店", "code", "store:view", "name", "门店查看"),
                Map.of("group", "门店", "code", "store:*", "name", "门店全部"),
                Map.of("group", "商品", "code", "product:view", "name", "商品查看"),
                Map.of("group", "商品", "code", "product:create", "name", "商品新增"),
                Map.of("group", "商品", "code", "product:update", "name", "商品编辑"),
                Map.of("group", "商品", "code", "product:delete", "name", "商品删除"),
                Map.of("group", "订货", "code", "purchase:view", "name", "订货查看"),
                Map.of("group", "订货", "code", "purchase:create", "name", "订货创建"),
                Map.of("group", "订货", "code", "purchase:update", "name", "订货处理"),
                Map.of("group", "订货", "code", "purchase:audit", "name", "订货审核"),
                Map.of("group", "库存", "code", "inventory:view", "name", "库存查看"),
                Map.of("group", "库存", "code", "inventory:create", "name", "库存作业"),
                Map.of("group", "库存", "code", "inventory:update", "name", "库存处理"),
                Map.of("group", "库存", "code", "inventory:audit", "name", "库存审核"),
                Map.of("group", "销售", "code", "sales:view", "name", "订单查看"),
                Map.of("group", "销售", "code", "sales:update", "name", "订单处理"),
                Map.of("group", "销售", "code", "sales:refund", "name", "订单退款"),
                Map.of("group", "销售", "code", "sales:pos", "name", "收银"),
                Map.of("group", "销售", "code", "sales:shift", "name", "交接班"),
                Map.of("group", "销售", "code", "sales:settle", "name", "日结"),
                Map.of("group", "会员", "code", "member:view", "name", "会员查看"),
                Map.of("group", "会员", "code", "member:create", "name", "会员新增"),
                Map.of("group", "会员", "code", "member:update", "name", "会员编辑"),
                Map.of("group", "会员", "code", "member:delete", "name", "会员删除"),
                Map.of("group", "结算", "code", "settlement:view", "name", "结算查看"),
                Map.of("group", "结算", "code", "settlement:create", "name", "生成对账"),
                Map.of("group", "结算", "code", "settlement:update", "name", "结算处理"),
                Map.of("group", "供应商", "code", "supplier:view", "name", "供应商查看"),
                Map.of("group", "供应商", "code", "supplier:create", "name", "供应商新增"),
                Map.of("group", "供应商", "code", "supplier:update", "name", "供应商编辑"),
                Map.of("group", "加盟", "code", "franchise:view", "name", "加盟查看"),
                Map.of("group", "加盟", "code", "franchise:create", "name", "加盟新增"),
                Map.of("group", "加盟", "code", "franchise:update", "name", "加盟编辑"),
                Map.of("group", "报表", "code", "report:view", "name", "报表查看"),
                Map.of("group", "公众号", "code", "mp:view", "name", "公众号管理"),
                Map.of("group", "内容", "code", "cms:view", "name", "内容管理"),
                Map.of("group", "系统", "code", "system:view", "name", "系统查看"),
                Map.of("group", "系统", "code", "system:update", "name", "系统修改"),
                Map.of("group", "系统", "code", "system:menu", "name", "菜单编辑"),
                Map.of("group", "系统", "code", "*", "name", "全部权限")
        ));
    }

    @lombok.Data
    public static class UserSaveRequest {
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String email;
        private String roleCode;
        private Long storeId;
        private Long supplierId;
        private Long franchiseeId;
        private String status;
    }
}
