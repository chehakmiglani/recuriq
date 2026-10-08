package com.recuriq.billing.dto.customer;

import lombok.Data;
import java.time.LocalDateTime;

import com.recuriq.billing.enums.Status;

@Data
public class CustomerProfileDTO {
   private Long customerId;
   private Long userId;
   private String fullName;
   private String email;
   private String phone;
   private String currency;
   private String country;
   private String state;
   private String city;
   private String addressLine1;
   private String postalCode;
   private Status status;
   private Long creditBalanceMinor;
   private LocalDateTime createdAt;
}