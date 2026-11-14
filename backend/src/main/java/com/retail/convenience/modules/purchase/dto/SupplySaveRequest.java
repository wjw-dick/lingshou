package com.retail.convenience.modules.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SupplySaveRequest {

    private String sourceType = "HQ";
    private Long supplierId;
    private Long storeId;
    private Long franchiseeId;
    private LocalDate expectedAt;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long skuId;
        private Integer qty;
        private BigDecimal price;
    }
}
