package com.recuriq.billing.service;

import com.recuriq.billing.dto.support.CustomerSearchResponse;
import com.recuriq.billing.dto.customer.SubscriptionDTO;
import com.recuriq.billing.dto.support.CustomerDetailResponse;
import com.recuriq.billing.entity.AuditLog;
import com.recuriq.billing.entity.BillingJob;
import com.recuriq.billing.entity.DunningRetryLog;

import com.recuriq.billing.dto.customer.CancellationRequestDTO;
import com.recuriq.billing.dto.customer.CancellationResponse;
import com.recuriq.billing.request.ProcessCancellationRequestInput;

import java.util.List;

public interface SupportService {
    List<CustomerSearchResponse> searchCustomers(String query);

    CustomerDetailResponse getCustomerDetails(Long customerId);

    List<AuditLog> getRecentAuditLogs();

    List<BillingJob> getBillingJobs();

    List<DunningRetryLog> getDunningLogs();

    List<SubscriptionDTO> getPastDueSubscriptions();

    List<CancellationRequestDTO> getPendingCancellationRequests();

    CancellationResponse approveCancellationRequest(Long requestId, String agentEmail, ProcessCancellationRequestInput input);

    void rejectCancellationRequest(Long requestId, String agentEmail, ProcessCancellationRequestInput input);
}
