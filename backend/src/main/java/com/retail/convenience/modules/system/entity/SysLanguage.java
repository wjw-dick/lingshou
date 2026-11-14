package com.retail.convenience.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_language")
public class SysLanguage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private Integer enabled;

    private Integer sortNo;

}