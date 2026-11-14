package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_storage_config")
public class SysStorageConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String driver;

    private String config;

    private Integer enabled;

    private LocalDateTime lastTestAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}