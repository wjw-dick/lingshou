package com.retail.convenience.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.PermissionService;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.security.JwtService;
import com.retail.convenience.common.security.LoginUser;
import com.retail.convenience.common.security.SessionService;
import com.retail.convenience.common.security.TenantContext;
import com.retail.convenience.common.tenant.TenantGuardService;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.auth.dto.AppLoginRequest;
import com.retail.convenience.modules.auth.dto.LoginRequest;
import com.retail.convenience.modules.auth.dto.LoginResponse;
import com.retail.convenience.modules.auth.dto.PlatformLoginRequest;
import com.retail.convenience.modules.member.entity.Member;
import com.retail.convenience.modules.member.mapper.MemberMapper;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.entity.PlatformUser;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import com.retail.convenience.modules.platform.mapper.PlatformUserMapper;
import com.retail.convenience.modules.system.entity.SysMenu;
import com.retail.convenience.modules.system.entity.SysUser;
import com.retail.convenience.modules.system.mapper.SysMenuMapper;
import com.retail.convenience.modules.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final PlatformUserMapper platformUserMapper;
    private final PlatformTenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;
    private final SysMenuMapper menuMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final TenantGuardService tenantGuardService;
    private final PermissionService permissionService;
    private final String cryptoSecret;

    public AuthService(PlatformUserMapper platformUserMapper,
                       PlatformTenantMapper tenantMapper,
                       SysUserMapper sysUserMapper,
                       SysMenuMapper menuMapper,
                       MemberMapper memberMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SessionService sessionService,
                       TenantGuardService tenantGuardService,
                       PermissionService permissionService,
                       @Value("${app.jwt.secret}") String cryptoSecret) {
        this.platformUserMapper = platformUserMapper;
        this.tenantMapper = tenantMapper;
        this.sysUserMapper = sysUserMapper;
        this.menuMapper = menuMapper;
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.tenantGuardService = tenantGuardService;
        this.permissionService = permissionService;
        this.cryptoSecret = cryptoSecret;
    }

    public LoginResponse platformLogin(PlatformLoginRequest request) {
        PlatformUser user = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw BizException.forbidden("平台账号已停用");
        }
        user.setLastLoginAt(LocalDateTime.now());
        platformUserMapper.updateById(user);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setUserType("PLATFORM");
        loginUser.setRoleCode(user.getRoleCode());
        String token = jwtService.create(loginUser);
        sessionService.save(loginUser, token);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(toUserInfo(loginUser, null));
        response.setMenus(platformMenus(user.getRoleCode()));
        return response;
    }

    public LoginResponse tenantLogin(LoginRequest request) {
        String code = request.getTenantCode() == null || request.getTenantCode().isBlank()
                ? "yxbl" : request.getTenantCode().trim();
        PlatformTenant tenant = tenantGuardService.findByCode(code);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        tenantGuardService.findActive(tenant.getTenantId());
        TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
        try {
            SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, tenant.getTenantId())
                    .eq(SysUser::getUsername, request.getUsername())
                    .eq(SysUser::getDeleted, 0)
                    .last("limit 1"));
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            if (!"ACTIVE".equals(user.getStatus())) {
                throw BizException.forbidden("账号已停用");
            }
            user.setLastLoginAt(LocalDateTime.now());
            sysUserMapper.updateById(user);

            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(user.getId());
            loginUser.setUsername(user.getUsername());
            loginUser.setRealName(user.getRealName());
            loginUser.setUserType("TENANT");
            loginUser.setRoleCode(user.getRoleCode());
            loginUser.setTenantId(tenant.getTenantId());
            loginUser.setTenantCode(tenant.getTenantCode());
            loginUser.setStoreId(user.getStoreId());
            loginUser.setSupplierId(user.getSupplierId());
            loginUser.setFranchiseeId(user.getFranchiseeId());
            String token = jwtService.create(loginUser);
            sessionService.save(loginUser, token);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(toUserInfo(loginUser, tenant.getName()));
            response.setMenus(tenantMenus(loginUser));
            return response;
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public LoginResponse appLogin(AppLoginRequest request) {
        String code = request.getTenantCode() == null || request.getTenantCode().isBlank()
                ? "yxbl" : request.getTenantCode().trim();
        PlatformTenant tenant = tenantGuardService.findByCode(code);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        tenantGuardService.findActive(tenant.getTenantId());
        TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
        try {
            String openid = request.getOpenid() == null || request.getOpenid().isBlank()
                    ? "demo_openid_" + Math.abs(Objects.hash(request.getNickname(), System.currentTimeMillis()))
                    : request.getOpenid();
            Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                    .eq(Member::getTenantId, tenant.getTenantId())
                    .eq(Member::getOpenid, openid)
                    .last("limit 1"));
            if (member == null) {
                member = new Member();
                member.setTenantId(tenant.getTenantId());
                member.setOpenid(openid);
                member.setUnionid(request.getUnionid() == null || request.getUnionid().isBlank()
                        ? "union_" + openid : request.getUnionid());
                member.setNickname(request.getNickname() == null ? "微信用户" : request.getNickname());
                member.setLevelId(1L);
                member.setPoints(0);
                member.setWalletBalance(java.math.BigDecimal.ZERO);
                member.setTotalConsume(java.math.BigDecimal.ZERO);
                member.setOrderCount(0);
                member.setStatus("ACTIVE");
                member.setRegisteredAt(LocalDateTime.now());
                member.setLastActiveAt(LocalDateTime.now());
                if (request.getPhone() != null && !request.getPhone().isBlank()) {
                    member.setPhone(AesUtil.encrypt(request.getPhone(), cryptoSecret));
                    member.setPhoneMask(request.getPhone().substring(0, 3) + "****"
                            + request.getPhone().substring(request.getPhone().length() - 4));
                }
                memberMapper.insert(member);
            } else {
                member.setLastActiveAt(LocalDateTime.now());
                memberMapper.updateById(member);
            }

            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(member.getId());
            loginUser.setUsername(member.getNickname());
            loginUser.setRealName(member.getNickname());
            loginUser.setUserType("MEMBER");
            loginUser.setRoleCode("MEMBER");
            loginUser.setTenantId(tenant.getTenantId());
            loginUser.setTenantCode(tenant.getTenantCode());
            loginUser.setMemberId(member.getId());
            String token = jwtService.create(loginUser);
            sessionService.save(loginUser, token);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(toUserInfo(loginUser, tenant.getName()));
            response.setMenus(List.of());
            return response;
        } finally {
            TenantContext.clear();
        }
    }

    public void logout() {
        LoginUser user = AuthContext.get();
        if (user != null) {
            sessionService.remove(user);
        }
    }

    public LoginResponse current() {
        LoginUser user = AuthContext.require();
        LoginResponse response = new LoginResponse();
        String tenantName = null;
        if (user.getTenantId() != null) {
            PlatformTenant tenant = tenantMapper.selectById(user.getTenantId());
            tenantName = tenant == null ? null : tenant.getName();
        }
        response.setUser(toUserInfo(user, tenantName));
        if (user.isPlatform()) {
            response.setMenus(platformMenus(user.getRoleCode()));
        } else if (user.isTenant()) {
            TenantContext.set(user.getTenantId(), user.getTenantCode());
            try {
                response.setMenus(tenantMenus(user));
            } finally {
                TenantContext.clear();
            }
        }
        return response;
    }

    public List<LoginResponse.MenuNode> tenantMenus(LoginUser user) {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, user.getTenantId())
                .eq(SysMenu::getEndType, "ADMIN")
                .eq(SysMenu::getStatus, "ACTIVE")
                .eq(SysMenu::getVisible, 1)
                .orderByAsc(SysMenu::getSortNo));
        List<SysMenu> allowed = menus.stream()
                .filter(m -> m.getPermission() == null || m.getPermission().isBlank()
                        || permissionService.hasPermission(user, m.getPermission()))
                .toList();
        return buildTree(allowed);
    }

    public List<LoginResponse.MenuNode> buildTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return buildChildren(0L, byParent);
    }

    private List<LoginResponse.MenuNode> buildChildren(Long parentId, Map<Long, List<SysMenu>> byParent) {
        List<SysMenu> children = byParent.getOrDefault(parentId, List.of());
        List<LoginResponse.MenuNode> result = new ArrayList<>();
        for (SysMenu menu : children) {
            List<LoginResponse.MenuNode> sub = buildChildren(menu.getId(), byParent);
            if (!byParent.containsKey(menu.getId()) || !sub.isEmpty()) {
                LoginResponse.MenuNode node = new LoginResponse.MenuNode();
                node.setId(menu.getId());
                node.setParentId(menu.getParentId());
                node.setTitle(menu.getTitle());
                node.setTitleEn(menu.getTitleEn());
                node.setIcon(menu.getIcon());
                node.setPath(menu.getPath());
                node.setComponent(menu.getComponent());
                node.setPermission(menu.getPermission());
                node.setSortNo(menu.getSortNo());
                node.setChildren(sub);
                result.add(node);
            }
        }
        result.sort(Comparator.comparing(n -> n.getSortNo() == null ? 0 : n.getSortNo()));
        return result;
    }

    private LoginResponse.UserInfo toUserInfo(LoginUser user, String tenantName) {
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setUserId(user.getUserId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setUserType(user.getUserType());
        info.setRoleCode(user.getRoleCode());
        info.setTenantId(user.getTenantId());
        info.setTenantCode(user.getTenantCode());
        info.setTenantName(tenantName);
        info.setStoreId(user.getStoreId());
        info.setSupplierId(user.getSupplierId());
        info.setFranchiseeId(user.getFranchiseeId());
        info.setMemberId(user.getMemberId());
        return info;
    }

    private List<LoginResponse.MenuNode> platformMenus(String role) {
        List<String[]> defs = List.of(
                new String[]{"dashboard", "经营总览", "DataBoard", "/platform/dashboard", "platform:view"},
                new String[]{"tenants", "租户管理", "OfficeBuilding", "/platform/tenants", "tenant:manage"},
                new String[]{"plans", "套餐与配额", "Tickets", "/platform/plans", "plan:manage"},
                new String[]{"bills", "账单与计费", "Money", "/platform/bills", "bill:manage"},
                new String[]{"announcements", "平台公告", "Bell", "/platform/announcements", "announcement:manage"},
                new String[]{"audits", "平台审计", "Document", "/platform/audits", "audit:view"},
                new String[]{"accounts", "平台账号", "User", "/platform/accounts", "account:manage"}
        );
        List<LoginResponse.MenuNode> nodes = new ArrayList<>();
        long id = 1;
        for (String[] d : defs) {
            boolean visible = "PLATFORM_ADMIN".equals(role) || permissionService.match(role, d[4])
                    || role.equals("PLATFORM_OPERATOR") && !"accounts".equals(d[0])
                    || role.equals("PLATFORM_FINANCE") && ("dashboard".equals(d[0]) || "bills".equals(d[0]) || "audits".equals(d[0]));
            if (!visible) {
                continue;
            }
            LoginResponse.MenuNode node = new LoginResponse.MenuNode();
            node.setId(id++);
            node.setParentId(0L);
            node.setTitle(d[1]);
            node.setIcon(d[2]);
            node.setPath(d[3]);
            node.setPermission(d[4]);
            node.setSortNo((int) id * 10);
            node.setChildren(List.of());
            nodes.add(node);
        }
        return nodes;
    }
}
