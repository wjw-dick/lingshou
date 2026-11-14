package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_draft")
public class MpDraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private String title;

    private String author;

    private String sourceUrl;

    private String coverUrl;

    private String digest;

    private String content;

    private String articles;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}