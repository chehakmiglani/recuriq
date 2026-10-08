package com.recuriq.billing.dto.customer;

import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class AddOnDTO {
   private Long addOnId;
   private String name;
   private Long priceMinor;
   private String currency;
   private BillingPeriod billingPeriod;
   private Status status;
}