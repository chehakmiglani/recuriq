package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.CustomerProfileDTO;
import com.recuriq.billing.dto.customer.SubscriptionDTO;
import com.recuriq.billing.dto.customer.InvoiceDTO;
import com.recuriq.billing.dto.customer.UsageRecordDTO;
import com.recuriq.billing.dto.customer.CreditNoteDTO;
import com.recuriq.billing.dto.support.CustomerSearchResponse;
import com.recuriq.billing.dto.support.CustomerDetailResponse;
import com.recuriq.billing.repository.CreditNoteRepository;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.UsageRecord;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.entity.AuditLog;
import com.recuriq.billing.entity.BillingJob;
import com.recuriq.billing.entity.DunningRetryLog;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.UsageRecordRepository;
import com.recuriq.billing.repository.AuditLogRepository;
import com.recuriq.billing.repository.BillingJobRepository;
import com.recuriq.billing.repository.DunningRetryLogRepository;
import com.recuriq.billing.repository.CancellationRequestRepository;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.entity.Notification;
import com.recuriq.billing.repository.NotificationRepository;
import com.recuriq.billing.dto.customer.CancellationRequestDTO;
import com.recuriq.billing.dto.customer.CancellationResponse;
import com.recuriq.billing.request.ProcessCancellationRequestInput;
import com.recuriq.billing.enums.CancellationRequestStatus;
import com.recuriq.billing.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {
 
     private final CustomerRepository customerRepository;
     private final SubscriptionRepository subscriptionRepository;
     private final InvoiceRepository invoiceRepository;
     private final UsageRecordRepository usageRecordRepository;
     private final AuditLogRepository auditLogRepository;
     private final BillingJobRepository billingJobRepository;
     private final DunningRetryLogRepository dunningRetryLogRepository;
     private final CreditNoteRepository creditNoteRepository;
     private final CancellationRequestRepository cancellationRequestRepository;
     private final UserRepository userRepository;
     private final CustomerSubscriptionService customerSubscriptionService;
     private final NotificationRepository notificationRepository;
     private final AuditLoggingService auditLoggingService;
 
     @Override
     public List<CustomerSearchResponse> searchCustomers(String query) {
         List<Customer> customers = customerRepository
                 .findByUser_FullNameContainingIgnoreCaseOrUser_EmailContainingIgnoreCase(query, query);
         return customers.stream()
                 .map(this::mapToSearchResponse)
                 .toList();
     }
 
     @Override
     public CustomerDetailResponse getCustomerDetails(Long customerId) {
         Customer customer = customerRepository.findById(customerId)
                 .orElseThrow(() -> CustomException.notFound("Customer not found"));
 
         CustomerProfileDTO profileDTO = mapToProfileDTO(customer);
 
         List<Subscription> subscriptions = subscriptionRepository.findByCustomer_Id(customerId);
         List<SubscriptionDTO> subscriptionDTOs = subscriptions.stream()
                 .map(this::mapToSubscriptionDTO)
                 .toList();
 
         List<Invoice> invoices = invoiceRepository.findByCustomer_IdOrderByIssueDateDesc(customerId);
         List<InvoiceDTO> invoiceDTOs = invoices.stream()
                 .map(this::mapToInvoiceDTO)
                 .toList();
 
         List<UsageRecord> usageRecords = usageRecordRepository.findBySubscription_Customer_Id(customerId);
         List<UsageRecordDTO> usageRecordDTOs = usageRecords.stream()
                 .map(this::mapToUsageRecordDTO)
                 .toList();
 
         List<com.recuriq.billing.entity.CreditNote> creditNotes = creditNoteRepository.findByInvoice_Customer_Id(customerId);
         List<CreditNoteDTO> creditNoteDTOs = creditNotes.stream()
                 .map(this::mapToCreditNoteDTO)
                 .toList();
 
         return new CustomerDetailResponse(profileDTO, subscriptionDTOs, invoiceDTOs, usageRecordDTOs, creditNoteDTOs);
     }

    private CustomerSearchResponse mapToSearchResponse(Customer customer) {
        User user = customer.getUser();
        return new CustomerSearchResponse(
                customer.getId(),
                user.getFullName(),
                user.getEmail(),
                customer.getStatus());
    }

    private CustomerProfileDTO mapToProfileDTO(Customer customer) {
        User user = customer.getUser();
        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setCustomerId(customer.getId());
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setCurrency(customer.getCurrency());
        dto.setCountry(customer.getCountry());
        dto.setState(customer.getState());
        dto.setCity(customer.getCity());
        dto.setAddressLine1(customer.getAddressLine1());
        dto.setPostalCode(customer.getPostalCode());
        dto.setStatus(customer.getStatus());
        dto.setCreatedAt(customer.getCreatedAt());
        return dto;
    }

    private SubscriptionDTO mapToSubscriptionDTO(Subscription sub) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(sub.getId());
        dto.setCustomerId(sub.getCustomer().getId());
        dto.setPlanId(sub.getPlan().getId());
        dto.setPlanName(sub.getPlan().getName());
        dto.setStatus(sub.getStatus());
        dto.setStartDate(sub.getStartDate() != null ? sub.getStartDate().toString() : null);
        dto.setCurrentPeriodEnd(sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd().toString() : null);
        dto.setCurrency(sub.getCurrency());
        return dto;
    }

    private InvoiceDTO mapToInvoiceDTO(Invoice inv) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceId(inv.getId());
        dto.setInvoiceNumber(inv.getInvoiceNumber());
        dto.setSubscriptionId(inv.getSubscription().getId());
        dto.setStatus(inv.getStatus());
        dto.setBillingReason(inv.getBillingReason());
        dto.setIssueDate(inv.getIssueDate() != null ? inv.getIssueDate().toString() : null);
        dto.setDueDate(inv.getDueDate() != null ? inv.getDueDate().toString() : null);
        dto.setTotalMinor(inv.getTotalMinor());
        dto.setCurrency(inv.getCurrency());
        return dto;
    }

    private UsageRecordDTO mapToUsageRecordDTO(UsageRecord ur) {
        UsageRecordDTO dto = new UsageRecordDTO();
        dto.setUsageId(ur.getId());
        dto.setComponentId(ur.getComponent().getId());
        dto.setComponentName(ur.getComponent().getName());
        dto.setQuantity(ur.getQuantity());
        dto.setUnitName(ur.getComponent().getUnitName());
        dto.setRecordedAt(ur.getRecordedAt() != null ? ur.getRecordedAt().toString() : null);
        return dto;
    }

    

    private CreditNoteDTO mapToCreditNoteDTO(com.recuriq.billing.entity.CreditNote creditNote) {
        CreditNoteDTO dto = new CreditNoteDTO();
        dto.setCreditNoteId(creditNote.getId());
        dto.setCreditNoteNumber(creditNote.getCreditNoteNumber());
        dto.setInvoiceId(creditNote.getInvoice().getId());
        Invoice inv = invoiceRepository.findById(creditNote.getInvoice().getId()).orElse(null);
        dto.setInvoiceNumber(inv != null ? inv.getInvoiceNumber() : "N/A");
        dto.setReason(creditNote.getReason());
        dto.setAmountMinor(creditNote.getAmountMinor());
        dto.setStatus(creditNote.getStatus());
        dto.setCreatedAt(creditNote.getCreatedAt().toString());
        return dto;
    }

    @Override
    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Override
    public List<BillingJob> getBillingJobs() {
        return billingJobRepository.findAll();
    }

    @Override
    public List<DunningRetryLog> getDunningLogs() {
        return dunningRetryLogRepository.findAll();
    }

    @Override
    public List<com.recuriq.billing.dto.customer.SubscriptionDTO> getPastDueSubscriptions() {
        return subscriptionRepository.findByStatusIn(List.of(Status.PAST_DUE, Status.ON_HOLD))
                .stream()
                .map(this::mapToSubscriptionDTO)
                .toList();
    }

    @Override
    public List<CancellationRequestDTO> getPendingCancellationRequests() {
        return cancellationRequestRepository.findByStatus(CancellationRequestStatus.PENDING)
                .stream()
                .map(this::mapToCancellationRequestDTO)
                .toList();
    }

    @Override
    public CancellationResponse approveCancellationRequest(Long requestId, String agentEmail, ProcessCancellationRequestInput input) {
        com.recuriq.billing.entity.CancellationRequest request = cancellationRequestRepository.findById(requestId)
                .orElseThrow(() -> CustomException.notFound("Cancellation request not found"));

        if (request.getStatus() != CancellationRequestStatus.PENDING) {
            throw CustomException.badRequest("Cancellation request is not pending");
        }

        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> CustomException.notFound("Agent not found"));

        request.setStatus(CancellationRequestStatus.APPROVED);
        request.setProcessedBy(agent);
        request.setProcessedAt(LocalDateTime.now());
        request.setAgentNotes(input.getAgentNotes());
        cancellationRequestRepository.save(request);

        // Perform actual cancellation
        String customerEmail = request.getSubscription().getCustomer().getUser().getEmail();
        CancellationResponse response = customerSubscriptionService.cancelSubscription(customerEmail, Boolean.TRUE.equals(request.getAtPeriodEnd()));

        // Create notification
        String subject = "Subscription Cancellation Approved";
        String body;
        if (Boolean.TRUE.equals(request.getAtPeriodEnd())) {
            body = "Your subscription cancellation request has been approved. Your subscription will remain active until the end of the current billing period on " + request.getSubscription().getCurrentPeriodEnd() + ".";
        } else {
            body = "Your subscription has been canceled immediately.";
            if (response.isRefundIssued()) {
                double refundAmt = response.getRefundAmountMinor() / 100.0;
                String symbol;
                try {
                    symbol = java.util.Currency.getInstance(response.getCurrency()).getSymbol();
                } catch (Exception e) {
                    symbol = response.getCurrency();
                }
                body += String.format(" A refund of %s%.2f has been processed to your payment method.", symbol, refundAmt);
            } else {
                body += " No refund was applicable.";
            }
        }

        Notification notification = Notification.builder()
                .customer(request.getSubscription().getCustomer())
                .type("CANCELLATION_APPROVED")
                .subject(subject)
                .body(body)
                .channel(com.recuriq.billing.enums.Channel.EMAIL)
                .status(com.recuriq.billing.enums.NotificationStatus.SENT)
                .scheduledAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);

        auditLoggingService.logAction("APPROVE_CANCELLATION", "CancellationRequest", request.getId(), null, request);

        return response;
    }

    @Override
    public void rejectCancellationRequest(Long requestId, String agentEmail, ProcessCancellationRequestInput input) {
        com.recuriq.billing.entity.CancellationRequest request = cancellationRequestRepository.findById(requestId)
                .orElseThrow(() -> CustomException.notFound("Cancellation request not found"));

        if (request.getStatus() != CancellationRequestStatus.PENDING) {
            throw CustomException.badRequest("Cancellation request is not pending");
        }

        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> CustomException.notFound("Agent not found"));

        request.setStatus(CancellationRequestStatus.REJECTED);
        request.setProcessedBy(agent);
        request.setProcessedAt(LocalDateTime.now());
        request.setAgentNotes(input.getAgentNotes());
        cancellationRequestRepository.save(request);

        // Create notification
        String subject = "Cancellation Request Declined";
        String body = "Your subscription cancellation request has been declined by support. " + 
                      (input.getAgentNotes() != null && !input.getAgentNotes().isBlank() 
                       ? "Agent notes: " + input.getAgentNotes() 
                       : "Please contact support for more details.");

        Notification notification = Notification.builder()
                .customer(request.getSubscription().getCustomer())
                .type("CANCELLATION_REJECTED")
                .subject(subject)
                .body(body)
                .channel(com.recuriq.billing.enums.Channel.EMAIL)
                .status(com.recuriq.billing.enums.NotificationStatus.SENT)
                .scheduledAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);

        auditLoggingService.logAction("REJECT_CANCELLATION", "CancellationRequest", request.getId(), null, request);
    }

    private CancellationRequestDTO mapToCancellationRequestDTO(com.recuriq.billing.entity.CancellationRequest request) {
        CancellationRequestDTO dto = new CancellationRequestDTO();
        dto.setRequestId(request.getId());
        dto.setSubscriptionId(request.getSubscription().getId());
        dto.setPlanName(request.getSubscription().getPlan().getName());
        dto.setCustomerEmail(request.getSubscription().getCustomer().getUser().getEmail());
        dto.setCustomerName(request.getSubscription().getCustomer().getUser().getFullName());
        dto.setReason(request.getReason());
        dto.setStatus(request.getStatus());
        dto.setAtPeriodEnd(request.getAtPeriodEnd());
        dto.setCreatedAt(request.getCreatedAt() != null ? request.getCreatedAt().toString() : null);
        dto.setProcessedByEmail(request.getProcessedBy() != null ? request.getProcessedBy().getEmail() : null);
        dto.setProcessedAt(request.getProcessedAt() != null ? request.getProcessedAt().toString() : null);
        dto.setAgentNotes(request.getAgentNotes());
        return dto;
    }
}
