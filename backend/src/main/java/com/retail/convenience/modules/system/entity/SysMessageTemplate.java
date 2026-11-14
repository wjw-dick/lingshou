package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message_template")
public class SysMessageTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String node;

    private String channel;

    private String templateId;

    private String title;

    private String content;

    private String fieldMapping;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}