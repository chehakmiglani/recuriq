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
public class RefundCreditDTO {
    // "REF-{credit_note_id}" e.g. "REF-1"
    private String refundId;

    // from invoice → most recent payment on that invoice (nullable if no payment exists)
    private Long paymentId;

    // amount_minor converted to INR rupees (÷100 after currency conversion)
    private double amount;

    // from credit_note.reason — free text
    private String reason;

    // from credit_note.created_at
    private LocalDateTime date;

    // DRAFT / ISSUED / APPLIED / VOIDED
    private String status;
}
