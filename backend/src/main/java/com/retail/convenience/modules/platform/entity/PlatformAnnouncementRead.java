package com.retail.convenience.modules.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_announcement_read")
public class PlatformAnnouncementRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long announcementId;

    private Long tenantId;

    private LocalDateTime readAt;

}