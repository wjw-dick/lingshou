package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long userId;

    private String username;

    private String module;

    private String action;

    private String targetType;

    private String targetId;

    private String detail;

    private String ip;

    private String userAgent;

    private String result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}