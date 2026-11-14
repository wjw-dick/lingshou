package com.retail.convenience.modules.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.ExcelUtil;
import com.retail.convenience.modules.cms.entity.CmsArticle;
import com.retail.convenience.modules.cms.entity.CmsBanner;
import com.retail.convenience.modules.cms.entity.CmsFranchiseApplication;
import com.retail.convenience.modules.cms.mapper.CmsArticleMapper;
import com.retail.convenience.modules.cms.mapper.CmsBannerMapper;
import com.retail.convenience.modules.cms.mapper.CmsFranchiseApplicationMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/cms")
public class CmsController {

    private final CmsArticleMapper articleMapper;
    private final CmsBannerMapper bannerMapper;
    private final CmsFranchiseApplicationMapper applicationMapper;

    public CmsController(CmsArticleMapper articleMapper, CmsBannerMapper bannerMapper,
                         CmsFranchiseApplicationMapper applicationMapper) {
        this.articleMapper = articleMapper;
        this.bannerMapper = bannerMapper;
        this.applicationMapper = applicationMapper;
    }

    @GetMapping("/articles")
    @RequirePerm("cms:view")
    public ApiResult<PageResult<CmsArticle>> articles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<CmsArticle> result = articleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CmsArticle>()
                        .like(keyword != null && !keyword.isBlank(), CmsArticle::getTitle, keyword)
                        .eq(status != null && !status.isBlank(), CmsArticle::getStatus, status)
                        .orderByDesc(CmsArticle::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/articles/{id}")
    @RequirePerm("cms:view")
    public ApiResult<CmsArticle> article(@PathVariable Long id) {
        CmsArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw BizException.notFound("文章不存在");
        }
        return ApiResult.ok(article);
    }

    @PostMapping("/articles")
    @RequirePerm("cms:update")
    public ApiResult<CmsArticle> createArticle(@RequestBody CmsArticle article) {
        article.setId(null);
        article.setTenantId(AuthContext.require().getTenantId());
        if (article.getStatus() == null) {
            article.setStatus("DRAFT");
        }
        articleMapper.insert(article);
        return ApiResult.ok(article);
    }

    @PutMapping("/articles/{id}")
    @RequirePerm("cms:update")
    public ApiResult<Void> updateArticle(@PathVariable Long id, @RequestBody CmsArticle article) {
        article.setId(id);
        articleMapper.updateById(article);
        return ApiResult.ok();
    }

    @PostMapping("/articles/{id}/publish")
    @RequirePerm("cms:update")
    public ApiResult<Void> publish(@PathVariable Long id) {
        CmsArticle article = new CmsArticle();
        article.setId(id);
        article.setStatus("PUBLISHED");
        article.setPublishedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return ApiResult.ok();
    }

    @DeleteMapping("/articles/{id}")
    @RequirePerm("cms:update")
    public ApiResult<Void> deleteArticle(@PathVariable Long id) {
        articleMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/banners")
    @RequirePerm("cms:view")
    public ApiResult<List<CmsBanner>> banners(@RequestParam(required = false) String position) {
        return ApiResult.ok(bannerMapper.selectList(new LambdaQueryWrapper<CmsBanner>()
                .eq(position != null && !position.isBlank(), CmsBanner::getPosition, position)
                .orderByAsc(CmsBanner::getSortNo)));
    }

    @PostMapping("/banners")
    @RequirePerm("cms:update")
    public ApiResult<Void> createBanner(@RequestBody CmsBanner banner) {
        banner.setId(null);
        banner.setTenantId(AuthContext.require().getTenantId());
        bannerMapper.insert(banner);
        return ApiResult.ok();
    }

    @PutMapping("/banners/{id}")
    @RequirePerm("cms:update")
    public ApiResult<Void> updateBanner(@PathVariable Long id, @RequestBody CmsBanner banner) {
        banner.setId(id);
        bannerMapper.updateById(banner);
        return ApiResult.ok();
    }

    @DeleteMapping("/banners/{id}")
    @RequirePerm("cms:update")
    public ApiResult<Void> deleteBanner(@PathVariable Long id) {
        bannerMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/franchise-applications")
    @RequirePerm("cms:view")
    public ApiResult<PageResult<CmsFranchiseApplication>> applications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<CmsFranchiseApplication> result = applicationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CmsFranchiseApplication>()
                        .eq(status != null && !status.isBlank(), CmsFranchiseApplication::getStatus, status)
                        .orderByDesc(CmsFranchiseApplication::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/franchise-applications/{id}/status")
    @RequirePerm("cms:update")
    public ApiResult<Void> updateApplicationStatus(@PathVariable Long id, @RequestParam String status) {
        CmsFranchiseApplication application = new CmsFranchiseApplication();
        application.setId(id);
        application.setStatus(status);
        applicationMapper.updateById(application);
        return ApiResult.ok();
    }

    @GetMapping("/franchise-applications/export")
    @RequirePerm("cms:view")
    public ResponseEntity<byte[]> exportApplications() {
        List<CmsFranchiseApplication> rows = applicationMapper.selectList(null);
        byte[] bytes = ExcelUtil.write("加盟申请", List.of("公司", "联系人", "电话", "城市", "资金", "备注", "状态", "提交时间"),
                rows.stream().map(a -> List.<Object>of(
                        nvl(a.getCompany()), nvl(a.getContact()), nvl(a.getPhone()), nvl(a.getCity()),
                        nvl(a.getAmount()), nvl(a.getRemark()), nvl(a.getStatus()), nvl(a.getCreatedAt())
                )).toList());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("加盟申请.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private Object nvl(Object v) {
        return v == null ? "" : v;
    }
}
