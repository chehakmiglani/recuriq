package com.recuriq.billing.dto.support;

import com.recuriq.billing.dto.customer.CustomerProfileDTO;
import com.recuriq.billing.dto.customer.SubscriptionDTO;
import com.recuriq.billing.dto.customer.InvoiceDTO;
import com.recuriq.billing.dto.customer.UsageRecordDTO;
import com.recuriq.billing.dto.customer.CreditNoteDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailResponse {
    private CustomerProfileDTO customerProfile;
    private List<SubscriptionDTO> subscriptions;
    private List<InvoiceDTO> invoices;
    private List<UsageRecordDTO> usageRecords;
    private List<CreditNoteDTO> creditNotes;
}
