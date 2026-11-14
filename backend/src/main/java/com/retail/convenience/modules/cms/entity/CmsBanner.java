package com.retail.convenience.modules.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cms_banner")
public class CmsBanner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String position;

    private String title;

    private String image;

    private String link;

    private Integer sortNo;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}