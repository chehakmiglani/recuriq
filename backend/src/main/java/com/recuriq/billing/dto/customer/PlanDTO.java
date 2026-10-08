package com.recuriq.billing.dto.customer;

import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.TaxMode;

import lombok.Data;

@Data
public class PlanDTO {
   private Long planId;
   private String name;
   private BillingPeriod billingPeriod;
   private Long defaultPriceMinor;
   private String defaultCurrency;
   private Integer trialDays;
   private Long setupFeeMinor;
   private TaxMode taxMode;
   private String effectiveFrom;
   private String effectiveTo;
   private Status status;
   private String productName;
}
