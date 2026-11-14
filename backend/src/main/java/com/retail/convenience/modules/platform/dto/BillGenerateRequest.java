package com.retail.convenience.modules.platform.dto;

import lombok.Data;

@Data
public class BillGenerateRequest {

    private Long tenantId;
    private String period;
    private String item;
}
