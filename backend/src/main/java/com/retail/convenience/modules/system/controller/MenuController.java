package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.system.entity.SysMenu;
import com.retail.convenience.modules.system.mapper.SysMenuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/system/menus")
public class MenuController {

    private final SysMenuMapper menuMapper;

    public MenuController(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @GetMapping
    @RequirePerm("system:view")
    public ApiResult<List<SysMenu>> list(@RequestParam(defaultValue = "ADMIN") String endType) {
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getEndType, endType)
                .orderByAsc(SysMenu::getSortNo)));
    }

    @PostMapping("/save")
    @RequirePerm("system:menu")
    @Transactional
    public ApiResult<List<SysMenu>> save(@RequestBody MenuSaveRequest request) {
        if (request.getEndType() == null || request.getMenus() == null) {
            throw new BizException("菜单数据不完整");
        }
        validate(request.getEndType(), request.getMenus());
        menuMapper.delete(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getEndType, request.getEndType()));
        for (MenuNode node : request.getMenus()) {
            insert(node, 0L, request.getEndType(), 0);
        }
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getEndType, request.getEndType())
                .orderByAsc(SysMenu::getSortNo)));
    }

    private void insert(MenuNode node, Long parentId, String endType, int depth) {
        SysMenu menu = new SysMenu();
        menu.setTenantId(AuthContext.require().getTenantId());
        menu.setEndType(endType);
        menu.setParentId(parentId);
        menu.setTitle(node.getTitle());
        menu.setTitleEn(node.getTitleEn());
        menu.setIcon(node.getIcon());
        menu.setPath(node.getPath());
        menu.setComponent(node.getComponent());
        menu.setPermission(node.getPermission());
        menu.setSortNo(node.getSortNo() == null ? 0 : node.getSortNo());
        menu.setVisible(node.getVisible() == null ? 1 : node.getVisible());
        menu.setStatus("ACTIVE");
        menuMapper.insert(menu);
        if (node.getChildren() != null) {
            for (MenuNode child : node.getChildren()) {
                insert(child, menu.getId(), endType, depth + 1);
            }
        }
    }

    private void validate(String endType, List<MenuNode> menus) {
        int top = menus.size();
        if ("MP".equals(endType)) {
            if (top > 3) {
                throw new BizException("公众号菜单一级最多 3 个");
            }
            for (MenuNode node : menus) {
                if (node.getChildren() != null && node.getChildren().size() > 5) {
                    throw new BizException("公众号菜单二级最多 5 个");
                }
                if (node.getChildren() != null) {
                    for (MenuNode child : node.getChildren()) {
                        if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                            throw new BizException("公众号菜单最多两级");
                        }
                    }
                }
            }
        }
        if ("TABBAR".equals(endType) && (top < 2 || top > 5)) {
            throw new BizException("小程序 tabBar 需 2~5 个");
        }
        if ("H5".equals(endType) && top > 5) {
            throw new BizException("H5 底部导航最多 5 个");
        }
    }

    @lombok.Data
    public static class MenuSaveRequest {
        private String endType;
        private List<MenuNode> menus;
    }

    @lombok.Data
    public static class MenuNode {
        private String title;
        private String titleEn;
        private String icon;
        private String path;
        private String component;
        private String permission;
        private Integer sortNo;
        private Integer visible;
        private List<MenuNode> children;
    }
}
