package com.retail.convenience.modules.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_announcement")
public class PlatformAnnouncement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String targetType;

    private String targetIds;

    private String status;

    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}