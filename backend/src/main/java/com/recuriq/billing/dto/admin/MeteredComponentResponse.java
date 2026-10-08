package com.recuriq.billing.dto.admin;

import com.recuriq.billing.entity.MeteredComponent;
import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class MeteredComponentResponse {
   private Long id;
   private String name;
   private String unitName;
   private Long pricePerUnitMinor;
   private Long freeTierQuantity;
   private Status status;
   private Long planId;
   private String planName;

   public static MeteredComponentResponse from(MeteredComponent mc) {
       MeteredComponentResponse r = new MeteredComponentResponse();
       r.setId(mc.getId());
       r.setName(mc.getName());
       r.setUnitName(mc.getUnitName());
       r.setPricePerUnitMinor(mc.getPricePerUnitMinor());
       r.setFreeTierQuantity(mc.getFreeTierQuantity());
       r.setStatus(mc.getStatus());
       r.setPlanId(mc.getPlan().getId());
       r.setPlanName(mc.getPlan().getName());
       return r;
   }
}
