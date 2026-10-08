package com.recuriq.billing.dto.admin;

import com.recuriq.billing.entity.PriceBookEntry;
import lombok.Data;

@Data
public class PriceBookResponse {
   private Long id;
   private Long planId;
   private String planName;
   private String region;
   private String currency;
   private Long priceMinor;
   private String effectiveFrom;
   private String effectiveTo;

   public static PriceBookResponse from(PriceBookEntry entry) {
       PriceBookResponse r = new PriceBookResponse();
       r.setId(entry.getId());
       r.setPlanId(entry.getPlan().getId());
       r.setPlanName(entry.getPlan().getName());
       r.setRegion(entry.getRegion());
       r.setCurrency(entry.getCurrency());
       r.setPriceMinor(entry.getPriceMinor());
       r.setEffectiveFrom(entry.getEffectiveFrom() != null ? entry.getEffectiveFrom().toString() : null);
       r.setEffectiveTo(entry.getEffectiveTo() != null ? entry.getEffectiveTo().toString() : null);
       return r;
   }
}
