package com.retail.convenience.modules.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_audit")
public class PlatformAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String action;

    private String targetType;

    private String targetId;

    private Long tenantId;

    private String detail;

    private String ip;

    private String result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}