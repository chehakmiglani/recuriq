package com.recuriq.billing.dto.customer;

import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class CreditNoteDTO {
   private Long creditNoteId;
   private String creditNoteNumber;
   private Long invoiceId;
   private String invoiceNumber;
   private String reason;
   private Long amountMinor;
   private Status status;
   private String createdAt;
}