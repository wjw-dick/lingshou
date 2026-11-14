package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.system.entity.TenantNotice;
import com.retail.convenience.modules.system.mapper.TenantNoticeMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/notices")
public class NoticeController {

    private final TenantNoticeMapper noticeMapper;

    public NoticeController(TenantNoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @GetMapping
    @RequirePerm("dashboard:view")
    public ApiResult<List<TenantNotice>> list() {
        return ApiResult.ok(noticeMapper.selectList(new LambdaQueryWrapper<TenantNotice>()
                .orderByDesc(TenantNotice::getPublishedAt).last("limit 20")));
    }

    @PostMapping
    @RequirePerm("system:update")
    public ApiResult<Void> create(@RequestBody TenantNotice notice) {
        if (notice.getTitle() == null || notice.getTitle().isBlank()) {
            throw new BizException("公告标题不能为空");
        }
        notice.setId(null);
        notice.setTenantId(AuthContext.require().getTenantId());
        noticeMapper.insert(notice);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> delete(@PathVariable Long id) {
        noticeMapper.deleteById(id);
        return ApiResult.ok();
    }
}
