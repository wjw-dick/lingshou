package com.retail.convenience.modules.mp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.mp.entity.MpAccount;
import com.retail.convenience.modules.mp.entity.MpDraft;
import com.retail.convenience.modules.mp.entity.MpMaterial;
import com.retail.convenience.modules.mp.entity.MpPublish;
import com.retail.convenience.modules.mp.entity.MpStat;
import com.retail.convenience.modules.mp.mapper.MpAccountMapper;
import com.retail.convenience.modules.mp.mapper.MpDraftMapper;
import com.retail.convenience.modules.mp.mapper.MpMaterialMapper;
import com.retail.convenience.modules.mp.mapper.MpPublishMapper;
import com.retail.convenience.modules.mp.mapper.MpStatMapper;
import com.retail.convenience.modules.mp.service.WeChatMpClient;
import com.retail.convenience.modules.system.entity.SysFile;
import com.retail.convenience.modules.system.service.StorageService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/mp")
public class MpContentController {

    private final MpAccountMapper accountMapper;
    private final MpStatMapper statMapper;
    private final MpMaterialMapper materialMapper;
    private final MpDraftMapper draftMapper;
    private final MpPublishMapper publishMapper;
    private final StorageService storageService;
    private final WeChatMpClient weChatClient;
    private final ObjectMapper objectMapper;

    public MpContentController(MpAccountMapper accountMapper, MpStatMapper statMapper,
                               MpMaterialMapper materialMapper, MpDraftMapper draftMapper,
                               MpPublishMapper publishMapper, StorageService storageService,
                               WeChatMpClient weChatClient, ObjectMapper objectMapper) {
        this.accountMapper = accountMapper;
        this.statMapper = statMapper;
        this.materialMapper = materialMapper;
        this.draftMapper = draftMapper;
        this.publishMapper = publishMapper;
        this.storageService = storageService;
        this.weChatClient = weChatClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/statistics")
    @RequirePerm("mp:view")
    public ApiResult<Map<String, Object>> statistics(@RequestParam(required = false) Long accountId,
                                                     @RequestParam(defaultValue = "7") int days) {
        int range = days == 15 || days == 30 ? days : 7;
        LocalDate start = LocalDate.now().minusDays(range - 1);
        List<MpStat> stats = statMapper.selectList(new LambdaQueryWrapper<MpStat>()
                .eq(accountId != null, MpStat::getAccountId, accountId)
                .ge(MpStat::getStatDate, start)
                .orderByAsc(MpStat::getStatDate));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", range);
        result.put("stats", stats);
        result.put("newUsers", stats.stream().mapToInt(s -> s.getNewUsers() == null ? 0 : s.getNewUsers()).sum());
        result.put("totalUsers", stats.isEmpty() ? 0
                : stats.get(stats.size() - 1).getTotalUsers());
        result.put("msgCount", stats.stream().mapToInt(s -> s.getMsgCount() == null ? 0 : s.getMsgCount()).sum());
        result.put("apiCount", stats.stream().mapToInt(s -> s.getApiCount() == null ? 0 : s.getApiCount()).sum());
        return ApiResult.ok(result);
    }

    @GetMapping("/materials")
    @RequirePerm("mp:view")
    public ApiResult<List<MpMaterial>> materials(@RequestParam(required = false) Long accountId,
                                                 @RequestParam(required = false) String mediaType) {
        return ApiResult.ok(materialMapper.selectList(new LambdaQueryWrapper<MpMaterial>()
                .eq(accountId != null, MpMaterial::getAccountId, accountId)
                .eq(mediaType != null && !mediaType.isBlank(), MpMaterial::getMediaType, mediaType)
                .orderByDesc(MpMaterial::getCreatedAt)));
    }

    @PostMapping("/materials")
    @RequirePerm("mp:update")
    public ApiResult<Map<String, Object>> uploadMaterial(@RequestPart("file") MultipartFile file,
                                                         @RequestParam(defaultValue = "IMAGE") String mediaType,
                                                         @RequestParam(required = false) String name,
                                                         @RequestParam(defaultValue = "1") Integer permanent,
                                                         @RequestPart(required = false) MultipartFile thumb) {
        long max = "IMAGE".equals(mediaType) ? 2 * 1024 * 1024
                : "VOICE".equals(mediaType) ? 2 * 1024 * 1024 : 10 * 1024 * 1024;
        if (file.getSize() > max) {
            throw new BizException("素材超过大小限制");
        }
        if ("VIDEO".equals(mediaType) && (thumb == null || thumb.isEmpty())) {
            throw new BizException("上传视频必须同时上传缩略图");
        }
        Long accountId = resolveAccountId(null);
        SysFile local = storageService.upload(file, "mp");
        String thumbUrl = null;
        if (thumb != null && !thumb.isEmpty()) {
            thumbUrl = storageService.upload(thumb, "mp").getUrl();
        }
        MpMaterial material = new MpMaterial();
        material.setAccountId(accountId);
        material.setTenantId(AuthContext.require().getTenantId());
        material.setMediaType(mediaType);
        material.setName(name == null || name.isBlank() ? local.getName() : name);
        material.setUrl(local.getUrl());
        material.setThumbUrl(thumbUrl);
        material.setPermanent(permanent);
        material.setSize(file.getSize());
        String warning = null;
        try {
            JsonNode node = weChatClient.postFile(accountMapper.selectById(accountId),
                    "material/add_material?type=" + mediaType.toLowerCase(), file);
            material.setMediaId(node.path("media_id").asText());
        } catch (BizException e) {
            warning = "素材已保存到本地对象存储；同步到微信失败：" + e.getMessage();
        }
        materialMapper.insert(material);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("material", material);
        result.put("warning", warning);
        return ApiResult.ok(result);
    }

    @DeleteMapping("/materials/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> deleteMaterial(@PathVariable Long id) {
        MpMaterial material = materialMapper.selectById(id);
        if (material == null) {
            return ApiResult.ok();
        }
        materialMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/drafts")
    @RequirePerm("mp:view")
    public ApiResult<PageResult<MpDraft>> drafts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String keyword) {
        Page<MpDraft> result = draftMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MpDraft>()
                        .eq(accountId != null, MpDraft::getAccountId, accountId)
                        .like(keyword != null && !keyword.isBlank(), MpDraft::getTitle, keyword)
                        .orderByDesc(MpDraft::getUpdatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/drafts/{id}")
    @RequirePerm("mp:view")
    public ApiResult<MpDraft> draft(@PathVariable Long id) {
        MpDraft draft = draftMapper.selectById(id);
        if (draft == null) {
            throw BizException.notFound("草稿不存在");
        }
        return ApiResult.ok(draft);
    }

    @PostMapping("/drafts")
    @RequirePerm("mp:update")
    public ApiResult<MpDraft> saveDraft(@RequestBody MpDraft draft) {
        if (draft.getTitle() == null || draft.getTitle().isBlank()) {
            throw new BizException("草稿标题不能为空");
        }
        draft.setId(null);
        draft.setAccountId(resolveAccountId(draft.getAccountId()));
        draft.setTenantId(AuthContext.require().getTenantId());
        draft.setStatus("DRAFT");
        draftMapper.insert(draft);
        return ApiResult.ok(draft);
    }

    @PutMapping("/drafts/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> updateDraft(@PathVariable Long id, @RequestBody MpDraft draft) {
        draft.setId(id);
        draftMapper.updateById(draft);
        return ApiResult.ok();
    }

    @DeleteMapping("/drafts/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> deleteDraft(@PathVariable Long id) {
        draftMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/drafts/{id}/publish")
    @RequirePerm("mp:update")
    @Transactional
    public ApiResult<MpPublish> publishDraft(@PathVariable Long id) {
        MpDraft draft = draftMapper.selectById(id);
        if (draft == null) {
            throw BizException.notFound("草稿不存在");
        }
        MpPublish publish = new MpPublish();
        publish.setAccountId(draft.getAccountId());
        publish.setDraftId(draft.getId());
        publish.setTitle(draft.getTitle());
        publish.setStatus("PUBLISHING");
        publish.setPublishAt(LocalDateTime.now());
        publishMapper.insert(publish);
        try {
            MpAccount account = accountMapper.selectById(draft.getAccountId());
            MpMaterial thumb = materialMapper.selectOne(new LambdaQueryWrapper<MpMaterial>()
                    .eq(MpMaterial::getAccountId, draft.getAccountId())
                    .eq(MpMaterial::getMediaType, "IMAGE")
                    .isNotNull(MpMaterial::getMediaId)
                    .last("limit 1"));
            if (thumb == null) {
                throw new BizException("请先上传并同步一张图片素材作为封面（thumb_media_id）");
            }
            Map<String, Object> article = new LinkedHashMap<>();
            article.put("title", draft.getTitle());
            article.put("author", draft.getAuthor() == null ? "" : draft.getAuthor());
            article.put("digest", draft.getDigest() == null ? "" : draft.getDigest());
            article.put("content", draft.getContent() == null ? "" : draft.getContent());
            article.put("content_source_url", draft.getSourceUrl() == null ? "" : draft.getSourceUrl());
            article.put("thumb_media_id", thumb.getMediaId());
            article.put("need_open_comment", 0);
            article.put("only_fans_can_comment", 0);
            JsonNode draftNode = weChatClient.post(account, "draft/add",
                    Map.of("articles", List.of(article)));
            String mediaId = draftNode.path("media_id").asText();
            JsonNode publishNode = weChatClient.post(account, "freepublish/submit",
                    Map.of("media_id", mediaId));
            publish.setMsgId(publishNode.path("publish_id").asText());
            publish.setStatus("SUCCESS");
            draft.setStatus("PUBLISHED");
            draftMapper.updateById(draft);
        } catch (BizException e) {
            publish.setStatus("FAILED");
            publish.setFailReason(e.getMessage());
        }
        publishMapper.updateById(publish);
        return ApiResult.ok(publish);
    }

    @GetMapping("/publishes")
    @RequirePerm("mp:view")
    public ApiResult<PageResult<MpPublish>> publishes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String status) {
        Page<MpPublish> result = publishMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MpPublish>()
                        .eq(accountId != null, MpPublish::getAccountId, accountId)
                        .eq(status != null && !status.isBlank(), MpPublish::getStatus, status)
                        .orderByDesc(MpPublish::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/publishes/{id}/retry")
    @RequirePerm("mp:update")
    public ApiResult<MpPublish> retry(@PathVariable Long id) {
        MpPublish publish = publishMapper.selectById(id);
        if (publish == null) {
            throw BizException.notFound("发表记录不存在");
        }
        if (publish.getDraftId() == null) {
            throw new BizException("该发表记录没有关联草稿，无法重试");
        }
        return publishDraft(publish.getDraftId());
    }

    private Long resolveAccountId(Long accountId) {
        if (accountId != null) {
            return accountId;
        }
        MpAccount account = accountMapper.selectOne(new LambdaQueryWrapper<MpAccount>()
                .orderByAsc(MpAccount::getId).last("limit 1"));
        if (account == null) {
            throw new BizException("请先配置公众号账号");
        }
        return account.getId();
    }
}
