package com.retail.convenience.modules.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cms_article")
public class CmsArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String category;

    private String title;

    private String titleEn;

    private String summary;

    private String summaryEn;

    private String cover;

    private String content;

    private String contentEn;

    private String author;

    private String status;

    private LocalDateTime publishedAt;

    private String seoKeywords;

    private String seoDescription;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}