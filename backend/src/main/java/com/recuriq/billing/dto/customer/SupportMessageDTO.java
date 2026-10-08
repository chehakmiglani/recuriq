package com.recuriq.billing.dto.customer;

import lombok.Data;

@Data
public class SupportMessageDTO {
   private String subject;
   private String message;
   private String category;
}