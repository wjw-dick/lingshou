package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("promotion")
public class Promotion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String type;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String ruleJson;

    private String storeScope;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}