package com.recuriq.billing.dto.customer;

import lombok.Data;

@Data
public class MeteredUsageDTO {
   private Long componentId;
   private String componentName;
   private String unitName;
   private Long pricePerUnitMinor;
   private Long freeTierQuantity;
   private Long quantityUsed;
   private Long costMinor;
}