package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.dto.support.CustomerSearchResponse;
import com.recuriq.billing.dto.support.CustomerDetailResponse;
import com.recuriq.billing.dto.customer.SubscriptionDTO;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.entity.AuditLog;
import com.recuriq.billing.entity.BillingJob;
import com.recuriq.billing.entity.DunningRetryLog;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.UsageRecord;
import com.recuriq.billing.entity.MeteredComponent;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.UsageRecordRepository;
import com.recuriq.billing.repository.CreditNoteRepository;
import com.recuriq.billing.repository.AuditLogRepository;
import com.recuriq.billing.repository.BillingJobRepository;
import com.recuriq.billing.repository.DunningRetryLogRepository;
import com.recuriq.billing.repository.CancellationRequestRepository;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.repository.NotificationRepository;
import com.recuriq.billing.entity.CancellationRequest;
import com.recuriq.billing.entity.Notification;
import com.recuriq.billing.enums.CancellationRequestStatus;
import com.recuriq.billing.dto.customer.CancellationRequestDTO;
import com.recuriq.billing.dto.customer.CancellationResponse;
import com.recuriq.billing.request.ProcessCancellationRequestInput;

@ExtendWith(MockitoExtension.class)
class SupportServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private UsageRecordRepository usageRecordRepository;
    @Mock
    private CreditNoteRepository creditNoteRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private BillingJobRepository billingJobRepository;
    @Mock
    private DunningRetryLogRepository dunningRetryLogRepository;
    @Mock
    private CancellationRequestRepository cancellationRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerSubscriptionService customerSubscriptionService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuditLoggingService auditLoggingService;

    @InjectMocks
    private SupportServiceImpl supportService;

    private Customer customer;
    private User user;
    private Plan plan;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).fullName("John Doe").email("john@test.com").build();
        customer = Customer.builder().id(1L).user(user).status(Status.ACTIVE).build();
        plan = Plan.builder().id(1L).name("Premium").build();
        subscription = Subscription.builder()
                .id(1L)
                .customer(customer)
                .plan(plan)
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    void testSearchCustomers() {
        when(customerRepository.findByUser_FullNameContainingIgnoreCaseOrUser_EmailContainingIgnoreCase("John", "John"))
                .thenReturn(Arrays.asList(customer));

        List<CustomerSearchResponse> responses = supportService.searchCustomers("John");

        assertEquals(1, responses.size());
        assertEquals("John Doe", responses.get(0).getFullName());
    }

    @Test
    void testGetCustomerDetails() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(subscription));
        
        Invoice invoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-001")
                .subscription(subscription)
                .status(Status.ACTIVE)
                .billingReason(com.recuriq.billing.enums.BillingReason.SUBSCRIPTION_CYCLE)
                .build();
        
        when(invoiceRepository.findByCustomer_IdOrderByIssueDateDesc(1L)).thenReturn(Arrays.asList(invoice));

        MeteredComponent component = MeteredComponent.builder().id(1L).name("Storage").unitName("GB").build();
        UsageRecord usageRecord = new UsageRecord();
        usageRecord.setId(1L);
        usageRecord.setComponent(component);
        usageRecord.setQuantity(10L);
        
        when(usageRecordRepository.findBySubscription_Customer_Id(1L)).thenReturn(Arrays.asList(usageRecord));

        com.recuriq.billing.entity.CreditNote creditNote = new com.recuriq.billing.entity.CreditNote();
        creditNote.setId(1L);
        creditNote.setCreditNoteNumber("CN-001");
        creditNote.setInvoice(invoice);
        creditNote.setReason("Test refund");
        creditNote.setAmountMinor(1000L);
        creditNote.setStatus(Status.ACTIVE);
        creditNote.setCreatedAt(java.time.LocalDateTime.now());

        when(creditNoteRepository.findByInvoice_Customer_Id(1L)).thenReturn(Arrays.asList(creditNote));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        CustomerDetailResponse response = supportService.getCustomerDetails(1L);

        assertNotNull(response);
        assertEquals("John Doe", response.getCustomerProfile().getFullName());
        assertEquals(1, response.getSubscriptions().size());
        assertEquals("Premium", response.getSubscriptions().get(0).getPlanName());
        assertEquals(1, response.getInvoices().size());
        assertEquals(1, response.getUsageRecords().size());
        assertEquals(1, response.getCreditNotes().size());
        assertEquals("CN-001", response.getCreditNotes().get(0).getCreditNoteNumber());
    }

    @Test
    void testGetCustomerDetails_NotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> supportService.getCustomerDetails(1L));
    }

    @Test
    void testGetRecentAuditLogs() {
        AuditLog log = new AuditLog();
        when(auditLogRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(Arrays.asList(log));

        List<AuditLog> logs = supportService.getRecentAuditLogs();

        assertEquals(1, logs.size());
    }

    @Test
    void testGetBillingJobs() {
        BillingJob job = new BillingJob();
        when(billingJobRepository.findAll()).thenReturn(Arrays.asList(job));

        List<BillingJob> jobs = supportService.getBillingJobs();

        assertEquals(1, jobs.size());
    }

    @Test
    void testGetDunningLogs() {
        DunningRetryLog log = new DunningRetryLog();
        when(dunningRetryLogRepository.findAll()).thenReturn(Arrays.asList(log));

        List<DunningRetryLog> logs = supportService.getDunningLogs();

        assertEquals(1, logs.size());
    }

    @Test
    void testGetPastDueSubscriptions() {
        subscription.setStatus(Status.PAST_DUE);
        when(subscriptionRepository.findByStatusIn(List.of(Status.PAST_DUE, Status.ON_HOLD)))
                .thenReturn(Arrays.asList(subscription));

        List<SubscriptionDTO> dtos = supportService.getPastDueSubscriptions();

        assertEquals(1, dtos.size());
        assertEquals("Premium", dtos.get(0).getPlanName());
    }

    @Test
    void testGetPendingCancellationRequests() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);
        request.setReason("Too expensive");
        request.setAtPeriodEnd(true);

        when(cancellationRequestRepository.findByStatus(CancellationRequestStatus.PENDING))
                .thenReturn(Arrays.asList(request));

        List<CancellationRequestDTO> result = supportService.getPendingCancellationRequests();

        assertEquals(1, result.size());
        assertEquals("Too expensive", result.get(0).getReason());
        assertTrue(result.get(0).getAtPeriodEnd());
    }

    @Test
    void testApproveCancellationRequest_Immediate() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);
        request.setAtPeriodEnd(false);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes("Approved immediate cancellation");

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        
        CancellationResponse cancelResponse = new CancellationResponse(true, 500L, "USD", "ref123", "CN-001", "Refund processed");
        when(customerSubscriptionService.cancelSubscription("john@test.com", false)).thenReturn(cancelResponse);

        CancellationResponse response = supportService.approveCancellationRequest(1L, "agent@test.com", input);

        assertNotNull(response);
        assertTrue(response.isRefundIssued());
        assertEquals(500L, response.getRefundAmountMinor());
        assertEquals(CancellationRequestStatus.APPROVED, request.getStatus());
        assertEquals(agent, request.getProcessedBy());
        assertEquals("Approved immediate cancellation", request.getAgentNotes());
        
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testRejectCancellationRequest() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes("Rejected request");

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        supportService.rejectCancellationRequest(1L, "agent@test.com", input);

        assertEquals(CancellationRequestStatus.REJECTED, request.getStatus());
        assertEquals(agent, request.getProcessedBy());
        assertEquals("Rejected request", request.getAgentNotes());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGetCustomerDetails_CreditNoteInvoiceNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(subscription));
        
        Invoice invoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-001")
                .subscription(subscription)
                .status(Status.ACTIVE)
                .billingReason(com.recuriq.billing.enums.BillingReason.SUBSCRIPTION_CYCLE)
                .build();
        
        when(invoiceRepository.findByCustomer_IdOrderByIssueDateDesc(1L)).thenReturn(Arrays.asList(invoice));
        when(usageRecordRepository.findBySubscription_Customer_Id(1L)).thenReturn(Arrays.asList());

        com.recuriq.billing.entity.CreditNote creditNote = new com.recuriq.billing.entity.CreditNote();
        creditNote.setId(1L);
        creditNote.setCreditNoteNumber("CN-001");
        creditNote.setInvoice(invoice);
        creditNote.setReason("Test refund");
        creditNote.setAmountMinor(1000L);
        creditNote.setStatus(Status.ACTIVE);
        creditNote.setCreatedAt(java.time.LocalDateTime.now());

        when(creditNoteRepository.findByInvoice_Customer_Id(1L)).thenReturn(Arrays.asList(creditNote));
        // Return empty for invoice repository lookup
        when(invoiceRepository.findById(1L)).thenReturn(Optional.empty());

        CustomerDetailResponse response = supportService.getCustomerDetails(1L);

        assertNotNull(response);
        assertEquals(1, response.getCreditNotes().size());
        assertEquals("N/A", response.getCreditNotes().get(0).getInvoiceNumber());
    }

    @Test
    void testApproveCancellationRequest_NotPending() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setStatus(CancellationRequestStatus.APPROVED);

        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> supportService.approveCancellationRequest(1L, "agent@test.com", input));
        assertEquals("Cancellation request is not pending", ex.getMessage());
    }

    @Test
    void testApproveCancellationRequest_AgentNotFound() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setStatus(CancellationRequestStatus.PENDING);

        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> supportService.approveCancellationRequest(1L, "agent@test.com", input));
        assertEquals("Agent not found", ex.getMessage());
    }

    @Test
    void testApproveCancellationRequest_AtPeriodEnd() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);
        request.setAtPeriodEnd(true);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes("Approved period end");

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        
        CancellationResponse cancelResponse = new CancellationResponse(false, 0L, "USD", null, null, "Scheduled cancellation");
        when(customerSubscriptionService.cancelSubscription("john@test.com", true)).thenReturn(cancelResponse);

        CancellationResponse response = supportService.approveCancellationRequest(1L, "agent@test.com", input);

        assertNotNull(response);
        assertFalse(response.isRefundIssued());
        assertEquals(CancellationRequestStatus.APPROVED, request.getStatus());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testApproveCancellationRequest_ImmediateNoRefund() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);
        request.setAtPeriodEnd(false);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        
        CancellationResponse cancelResponse = new CancellationResponse(false, 0L, "USD", null, null, "Canceled immediately, no refund");
        when(customerSubscriptionService.cancelSubscription("john@test.com", false)).thenReturn(cancelResponse);

        CancellationResponse response = supportService.approveCancellationRequest(1L, "agent@test.com", input);

        assertNotNull(response);
        assertFalse(response.isRefundIssued());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testApproveCancellationRequest_InvalidCurrencyCatchBlock() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);
        request.setAtPeriodEnd(false);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        
        // "XYZ" triggers catch block for currency symbol lookup
        CancellationResponse cancelResponse = new CancellationResponse(true, 500L, "XYZ", "ref123", "CN-001", "Refund processed");
        when(customerSubscriptionService.cancelSubscription("john@test.com", false)).thenReturn(cancelResponse);

        CancellationResponse response = supportService.approveCancellationRequest(1L, "agent@test.com", input);

        assertNotNull(response);
        assertTrue(response.isRefundIssued());
        assertEquals("XYZ", response.getCurrency());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testRejectCancellationRequest_NotPending() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setStatus(CancellationRequestStatus.APPROVED);

        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> supportService.rejectCancellationRequest(1L, "agent@test.com", input));
        assertEquals("Cancellation request is not pending", ex.getMessage());
    }

    @Test
    void testRejectCancellationRequest_AgentNotFound() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setStatus(CancellationRequestStatus.PENDING);

        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> supportService.rejectCancellationRequest(1L, "agent@test.com", input));
        assertEquals("Agent not found", ex.getMessage());
    }

    @Test
    void testRejectCancellationRequest_EmptyNotes() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes(""); // empty notes trigger default notification message

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        supportService.rejectCancellationRequest(1L, "agent@test.com", input);

        assertEquals(CancellationRequestStatus.REJECTED, request.getStatus());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testApproveCancellationRequest_WithRefundValidCurrency() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);
        request.setAtPeriodEnd(false);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes("Approved with refund");

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        
        // Valid currency "USD" — should succeed in Currency.getInstance
        CancellationResponse cancelResponse = new CancellationResponse(true, 1500L, "USD", "ref_123", "CN-002", "Refund processed");
        when(customerSubscriptionService.cancelSubscription("john@test.com", false)).thenReturn(cancelResponse);

        CancellationResponse response = supportService.approveCancellationRequest(1L, "agent@test.com", input);

        assertNotNull(response);
        assertTrue(response.isRefundIssued());
        assertEquals(1500L, response.getRefundAmountMinor());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testRejectCancellationRequest_WithNotes() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes("Customer retention offer applied");

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        supportService.rejectCancellationRequest(1L, "agent@test.com", input);

        assertEquals(CancellationRequestStatus.REJECTED, request.getStatus());
        assertEquals("Customer retention offer applied", request.getAgentNotes());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testRejectCancellationRequest_NullNotes() {
        CancellationRequest request = new CancellationRequest();
        request.setId(1L);
        request.setSubscription(subscription);
        request.setStatus(CancellationRequestStatus.PENDING);

        User agent = User.builder().id(2L).email("agent@test.com").build();
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes(null); // null notes trigger default notification message

        when(cancellationRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        supportService.rejectCancellationRequest(1L, "agent@test.com", input);

        assertEquals(CancellationRequestStatus.REJECTED, request.getStatus());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}

