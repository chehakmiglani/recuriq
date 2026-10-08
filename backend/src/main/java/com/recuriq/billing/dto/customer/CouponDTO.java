package com.recuriq.billing.dto.customer;

import com.recuriq.billing.enums.CouponType;
import com.recuriq.billing.enums.Duration;
import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class CouponDTO {
   private Long couponId;
   private String code;
   private String name;
   private CouponType type;
   private Long amount;
   private String currency;
   private Duration duration;
   private Integer durationInMonths;
   private String validFrom;
   private String validTo;
   private Status status;
}