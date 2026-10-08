package com.recuriq.billing.dto.customer;

import lombok.Data;

@Data
public class UsageRecordDTO {
   private Long usageId;
   private Long componentId;
   private String componentName;
   private Long quantity;
   private String unitName;
   private String recordedAt;
   private String billingPeriodStart;
   private String billingPeriodEnd;
}
