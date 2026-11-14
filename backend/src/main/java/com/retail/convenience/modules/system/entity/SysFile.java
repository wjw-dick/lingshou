package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String path;

    private String url;

    private Long size;

    private String contentType;

    private String bizType;

    private String driver;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}