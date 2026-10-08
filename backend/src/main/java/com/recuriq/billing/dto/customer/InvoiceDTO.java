package com.recuriq.billing.dto.customer;

import lombok.Data;
import java.util.List;

import com.recuriq.billing.enums.BillingReason;
import com.recuriq.billing.enums.Status;

@Data
public class InvoiceDTO {
   private Long invoiceId;
   private String invoiceNumber;
   private Long subscriptionId;
   private Status status;
   private BillingReason billingReason;
   private String issueDate;
   private String dueDate;
   private Long subtotalMinor;
   private Long taxMinor;
   private Long discountMinor;
   private Long totalMinor;
   private Long balanceMinor;
   private String currency;
   private List<InvoiceLineItemDTO> lineItems;
}