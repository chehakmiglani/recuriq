package com.recuriq.billing.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordDTO {
    private Long paymentId;

    // "INV-{year from invoice.issue_date}-{invoice_id}" e.g. "INV-2026-1001"
    private String invoiceNumber;

    // amount_minor converted to INR rupees (÷100 after currency conversion)
    private double amount;

    // from payment_method table: "CARD ****1234" or "UPI user@upi"
    private String paymentMethod;

    // from payment.created_at
    private LocalDateTime date;

    // PENDING / SUCCESS / FAILED / REFUNDED / PARTIALLY_REFUNDED
    private String status;
}
