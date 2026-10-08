package com.recuriq.billing.dto.finance;

import com.recuriq.billing.dto.customer.InvoiceLineItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailDTO {
    // "INV-{year from issue_date}-{invoice_id}" e.g. "INV-2026-1001"
    private String invoiceNumber;

    private Long customerId;

    // total_minor converted: if currency=INR divide by 100, if USD multiply by 95 then divide by 100, if GBP multiply by 129 then divide by 100
    private double amount;

    // from invoice.created_at
    private LocalDateTime date;

    private LocalDate dueDate;

    // DRAFT / OPEN / PAID / VOID / UNCOLLECTIBLE
    private String status;

    // Line items for this invoice
    private List<InvoiceLineItemDTO> lineItems;
}
