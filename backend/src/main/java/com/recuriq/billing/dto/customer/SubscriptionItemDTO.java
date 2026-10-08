package com.recuriq.billing.dto.customer;

import lombok.Data;

@Data
public class SubscriptionItemDTO {
   private Long itemId;
   private Long addonId;
   private String addonName;
   private Long unitPriceMinor;
   private Integer quantity;
}