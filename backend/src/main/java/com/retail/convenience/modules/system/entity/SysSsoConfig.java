package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_sso_config")
public class SysSsoConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Integer enabled;

    private String protocol;

    private String serverUrl;

    private String serviceUrl;

    private String clientId;

    private String clientSecret;

    private String accountMapping;

    private Integer autoCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}