package com.retail.convenience.modules.franchise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("franchisee_store")
public class FranchiseeStore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long franchiseeId;

    private Long storeId;

    private LocalDate joinDate;

    private String status;

}