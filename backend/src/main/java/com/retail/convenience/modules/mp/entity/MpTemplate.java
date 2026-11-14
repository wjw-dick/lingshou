package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_template")
public class MpTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private String wxTemplateId;

    private String title;

    private String content;

    private String example;

    private String primaryIndustry;

    private String secondaryIndustry;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}