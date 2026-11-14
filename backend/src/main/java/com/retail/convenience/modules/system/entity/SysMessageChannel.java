package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message_channel")
public class SysMessageChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String channel;

    private Integer enabled;

    private String config;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}