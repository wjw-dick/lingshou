package com.retail.convenience.modules.franchise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("franchisee_qualification")
public class FranchiseeQualification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long franchiseeId;

    private String type;

    private String fileUrl;

    private LocalDate expireAt;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}