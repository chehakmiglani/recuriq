package com.recuriq.billing.request;

import com.recuriq.billing.enums.PaymentType;

import lombok.Data;

@Data
public class AddPaymentMethodRequest {
   private PaymentType paymentType;
   private String cardNumber;
   private Integer expiryMonth;
   private Integer expiryYear;
   private String cvv;
   private String upiId;
   private Boolean isDefault;
}