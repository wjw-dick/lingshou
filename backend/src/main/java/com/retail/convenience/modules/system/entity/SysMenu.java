package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String endType;

    private Long parentId;

    private String title;

    private String titleEn;

    private String icon;

    private String path;

    private String component;

    private String permission;

    private Integer sortNo;

    private Integer visible;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}