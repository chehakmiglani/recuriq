package com.recuriq.billing.dto.customer;

import com.recuriq.billing.entity.InvoiceLineItem.LineType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceLineItemDTO {
   private Long lineItemId;
   private String description;
   private LineType lineType;
   private Integer quantity;
   private Long unitPriceMinor;
   private Long amountMinor;
   private String periodStart;
   private String periodEnd;
}