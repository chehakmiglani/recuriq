package com.recuriq.billing.dto.customer;

import lombok.Data;
import java.util.List;

import com.recuriq.billing.enums.Status;

@Data
public class SubscriptionDTO {
   private Long subscriptionId;
   private Long customerId;
   private Long planId;
   private String planName;
   private Status status;
   private String startDate;
   private String trialEndDate;
   private String currentPeriodStart;
   private String currentPeriodEnd;
   private Boolean cancelAtPeriodEnd;
   private String canceledAt;
   private String pausedFrom;
   private String pausedTo;
   private String currency;
   private Long planPriceMinor;
   private String billingPeriod;
   private List<SubscriptionItemDTO> addOns;
   private List<MeteredUsageDTO> meteredUsage;
   private Long discountMinor;
   private Long totalDueMinor;
   private Long creditBalanceMinor;
}