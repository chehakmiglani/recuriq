package com.recuriq.billing.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancellationResponse {
    private boolean refundIssued;
    private long refundAmountMinor;
    private String currency;
    private String refundGatewayRef;
    private String creditNoteNumber;
    private String message;
}
