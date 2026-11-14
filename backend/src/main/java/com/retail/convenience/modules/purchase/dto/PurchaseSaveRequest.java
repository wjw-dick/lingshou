package com.retail.convenience.modules.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseSaveRequest {

    private Long supplierId;
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
