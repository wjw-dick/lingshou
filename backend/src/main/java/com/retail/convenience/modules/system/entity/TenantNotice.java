package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tenant_notice")
public class TenantNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String title;

    private String content;

    private String type;

    private LocalDateTime publishedAt;

}