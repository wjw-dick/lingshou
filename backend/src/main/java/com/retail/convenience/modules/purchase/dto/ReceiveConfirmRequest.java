package com.retail.convenience.modules.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReceiveConfirmRequest {

    private BigDecimal coldChainTemp;
    private String receiver;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long receiveItemId;
        private Long supplyItemId;
        private Long skuId;
        private Integer actualQty;
        private LocalDate productionDate;
        private LocalDate expiryDate;
        private BigDecimal coldChainTemp;
        private String batchNo;
    }
}
