package com.recuriq.billing.dto.customer;

import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class PaymentMethodDTO {
   private Long paymentMethodId;
   private PaymentType paymentType;
   private String cardLast4;
   private String cardBrand;
   private String upiId;
   private Boolean isDefault;
   private Integer expiryMonth;
   private Integer expiryYear;
   private Status status;
}