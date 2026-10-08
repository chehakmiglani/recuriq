package com.recuriq.billing.dto.admin;

import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class SubscriptionResponse {
   private Long id;
   private Long customerId;
   private String customerName;
   private Long planId;
   private String planName;
   private Status status;
   private String startDate;
   private String currentPeriodEnd;
   private String currency;

   public static SubscriptionResponse from(Subscription sub) {
       SubscriptionResponse r = new SubscriptionResponse();
       r.setId(sub.getId());
       r.setCustomerId(sub.getCustomer().getId());
       r.setCustomerName(sub.getCustomer().getUser().getFullName());
       r.setPlanId(sub.getPlan().getId());
       r.setPlanName(sub.getPlan().getName());
       r.setStatus(sub.getStatus());
       r.setStartDate(sub.getStartDate() != null ? sub.getStartDate().toString() : null);
       r.setCurrentPeriodEnd(sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd().toString() : null);
       r.setCurrency(sub.getCurrency());
       return r;
   }
}
