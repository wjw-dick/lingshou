package com.retail.convenience.modules.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.modules.platform.entity.PlatformAnnouncement;
import com.retail.convenience.modules.platform.entity.PlatformAnnouncementRead;
import com.retail.convenience.modules.platform.mapper.PlatformAnnouncementMapper;
import com.retail.convenience.modules.platform.mapper.PlatformAnnouncementReadMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform/announcements")
public class PlatformAnnouncementController {

    private final PlatformAnnouncementMapper announcementMapper;
    private final PlatformAnnouncementReadMapper readMapper;

    public PlatformAnnouncementController(PlatformAnnouncementMapper announcementMapper,
                                          PlatformAnnouncementReadMapper readMapper) {
        this.announcementMapper = announcementMapper;
        this.readMapper = readMapper;
    }

    @GetMapping
    @RequirePerm("announcement:manage")
    public ApiResult<PageResult<PlatformAnnouncement>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PlatformAnnouncement> result = announcementMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PlatformAnnouncement>()
                        .orderByDesc(PlatformAnnouncement::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping
    @RequirePerm("announcement:manage")
    public ApiResult<Void> create(@RequestBody PlatformAnnouncement announcement) {
        announcement.setId(null);
        announcement.setStatus("PUBLISHED");
        announcement.setPublishedAt(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @RequirePerm("announcement:manage")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody PlatformAnnouncement announcement) {
        announcement.setId(id);
        announcementMapper.updateById(announcement);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("announcement:manage")
    public ApiResult<Void> delete(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/read-stats")
    @RequirePerm("announcement:manage")
    public ApiResult<Map<String, Object>> readStats(@PathVariable Long id) {
        long reads = readMapper.selectCount(new LambdaQueryWrapper<PlatformAnnouncementRead>()
                .eq(PlatformAnnouncementRead::getAnnouncementId, id));
        return ApiResult.ok(Map.of("readCount", reads));
    }
}
