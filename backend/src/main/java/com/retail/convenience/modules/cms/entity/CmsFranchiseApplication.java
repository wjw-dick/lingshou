package com.retail.convenience.modules.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cms_franchise_application")
public class CmsFranchiseApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String company;

    private String contact;

    private String phone;

    private String city;

    private String amount;

    private String remark;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}