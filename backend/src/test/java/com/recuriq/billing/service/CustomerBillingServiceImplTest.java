package com.recuriq.billing.service;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.BillingReason;
import com.recuriq.billing.enums.CouponType;
import com.recuriq.billing.repository.*;
@ExtendWith(MockitoExtension.class)
class CustomerBillingServiceImplTest {
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private InvoicePdfService invoicePdfService;
    @Mock private SubscriptionCouponRepository subscriptionCouponRepository;
    @InjectMocks
    private CustomerBillingServiceImpl customerBillingService;
    private Customer customer;
    private User user;
    private Invoice invoice;
    private Subscription subscription;
    private Coupon coupon;
    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com").fullName("Test User").build();
        customer = Customer.builder().id(1L).user(user).status(Status.ACTIVE).build();
        subscription = Subscription.builder().id(1L).customer(customer).status(Status.ACTIVE).build();
        invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-1").customer(customer).subscription(subscription)
                .status(Status.OPEN).billingReason(BillingReason.SUBSCRIPTION_CYCLE)
                .issueDate(LocalDate.now()).totalMinor(1000L).balanceMinor(1000L).build();
        coupon = Coupon.builder()
                .id(1L).code("DISCOUNT10").name("10% Off").type(CouponType.PERCENT)
                .amount(10L).status(Status.ACTIVE).validFrom(LocalDate.now().minusDays(1))
                .validTo(LocalDate.now().plusDays(10)).redeemedCount(0).maxRedemptions(100).build();
    }
    // ==================== GET INVOICES ====================
    @Test
    void testGetInvoices_NoStatusFilter() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findByCustomer_IdOrderByIssueDateDesc(1L)).thenReturn(Arrays.asList(invoice));
        List<InvoiceDTO> result = customerBillingService.getInvoices("test@test.com", null, null, null);
        assertEquals(1, result.size());
        assertEquals("INV-1", result.get(0).getInvoiceNumber());
    }
    @Test
    void testGetInvoices_WithStatusFilter() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findByCustomer_IdAndStatus(1L, "OPEN")).thenReturn(Arrays.asList(invoice));
        List<InvoiceDTO> result = customerBillingService.getInvoices("test@test.com", "OPEN", null, null);
        assertEquals(1, result.size());
    }
    // ==================== GET INVOICE DETAIL ====================
    @Test
    void testGetInvoiceDetail_Success() {
        invoice.setDueDate(LocalDate.now().plusDays(15));
        InvoiceLineItem item1 = new InvoiceLineItem();
        item1.setId(1L);
        item1.setDescription("Item 1");
        item1.setQuantity(1);
        item1.setUnitPriceMinor(100L);
        item1.setAmountMinor(100L);
        item1.setPeriodStart(LocalDate.now());
        item1.setPeriodEnd(LocalDate.now().plusDays(30));
        InvoiceLineItem item2 = new InvoiceLineItem();
        item2.setId(2L);
        item2.setDescription("Item 2");
        item2.setQuantity(1);
        item2.setUnitPriceMinor(100L);
        item2.setAmountMinor(100L);
        item2.setPeriodStart(null);
        item2.setPeriodEnd(null);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceLineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList(item1, item2));
        InvoiceDTO dto = customerBillingService.getInvoiceDetail("test@test.com", 1L);
        assertEquals("INV-1", dto.getInvoiceNumber());
        assertEquals(LocalDate.now().plusDays(15).toString(), dto.getDueDate());
        assertEquals(2, dto.getLineItems().size());
        assertEquals(LocalDate.now().toString(), dto.getLineItems().get(0).getPeriodStart());
        assertNull(dto.getLineItems().get(1).getPeriodStart());
    }
    @Test
    void testGetInvoiceDetail_NotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerBillingService.getInvoiceDetail("test@test.com", 1L));
    }
    @Test
    void testGetInvoiceDetail_Unauthorized() {
        Customer otherCustomer = Customer.builder().id(2L).build();
        invoice.setCustomer(otherCustomer);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        assertThrows(RuntimeException.class, () -> customerBillingService.getInvoiceDetail("test@test.com", 1L));
    }
    // ==================== GENERATE INVOICE PDF ====================
    @Test
    void testGenerateInvoicePdf_Success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoicePdfService.generatePdf(invoice)).thenReturn(new byte[]{1, 2, 3});
        byte[] pdf = customerBillingService.generateInvoicePdf("test@test.com", 1L);
        assertEquals(3, pdf.length);
    }
    @Test
    void testGenerateInvoicePdf_NotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerBillingService.generateInvoicePdf("test@test.com", 1L));
    }
    @Test
    void testGenerateInvoicePdf_Unauthorized() {
        Customer otherCustomer = Customer.builder().id(2L).build();
        invoice.setCustomer(otherCustomer);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        assertThrows(RuntimeException.class, () -> customerBillingService.generateInvoicePdf("test@test.com", 1L));
    }
    // ==================== GET PAYMENTS ====================


    // ==================== APPLY COUPON ====================
    @Test
    void testApplyCoupon_Success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Arrays.asList(subscription));
        when(subscriptionCouponRepository.findBySubscription_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Optional.empty());
        CouponDTO dto = customerBillingService.applyCoupon("test@test.com", "DISCOUNT10");
        assertEquals("DISCOUNT10", dto.getCode());
        verify(subscriptionCouponRepository).save(any(SubscriptionCoupon.class));
        verify(couponRepository).save(coupon);
    }
    @Test
    void testApplyCoupon_InvalidCoupon() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(couponRepository.findByCodeAndStatus("INVALID", Status.ACTIVE)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerBillingService.applyCoupon("test@test.com", "INVALID"));
    }
    @Test
    void testApplyCoupon_NoActiveSubscription() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());
        assertThrows(RuntimeException.class, () -> customerBillingService.applyCoupon("test@test.com", "DISCOUNT10"));
    }
    @Test
    void testApplyCoupon_SameCouponAlreadyActive() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Arrays.asList(subscription));
        SubscriptionCoupon sc = new SubscriptionCoupon();
        sc.setCoupon(coupon); // Same coupon!
        when(subscriptionCouponRepository.findBySubscription_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Optional.of(sc));
        assertThrows(RuntimeException.class, () -> customerBillingService.applyCoupon("test@test.com", "DISCOUNT10"));
    }
    @Test
    void testApplyCoupon_ReplacesExistingCoupon() {
        Coupon otherCoupon = Coupon.builder().id(2L).code("OTHER").name("Other").type(CouponType.FIXED)
                .amount(500L).status(Status.ACTIVE).validFrom(LocalDate.now().minusDays(1))
                .redeemedCount(0).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Arrays.asList(subscription));
        SubscriptionCoupon oldSc = new SubscriptionCoupon();
        oldSc.setCoupon(otherCoupon); // Different coupon
        oldSc.setStatus(Status.ACTIVE);
        when(subscriptionCouponRepository.findBySubscription_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Optional.of(oldSc));
        CouponDTO dto = customerBillingService.applyCoupon("test@test.com", "DISCOUNT10");
        assertEquals("DISCOUNT10", dto.getCode());
        assertEquals(Status.CANCELED, oldSc.getStatus()); // Old coupon deactivated
        verify(subscriptionCouponRepository, times(2)).save(any(SubscriptionCoupon.class));
    }
    // ==================== VALIDATE COUPON ====================
    @Test
    void testValidateCoupon_Success() {
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        CouponDTO dto = customerBillingService.validateCoupon("DISCOUNT10");
        assertEquals("DISCOUNT10", dto.getCode());
    }
    @Test
    void testValidateCoupon_InvalidCode() {
        when(couponRepository.findByCodeAndStatus("INVALID", Status.ACTIVE)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerBillingService.validateCoupon("INVALID"));
    }
    @Test
    void testValidateCoupon_NotYetValid() {
        coupon.setValidFrom(LocalDate.now().plusDays(5));
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        assertThrows(RuntimeException.class, () -> customerBillingService.validateCoupon("DISCOUNT10"));
    }
    @Test
    void testValidateCoupon_Expired() {
        coupon.setValidTo(LocalDate.now().minusDays(1));
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        assertThrows(RuntimeException.class, () -> customerBillingService.validateCoupon("DISCOUNT10"));
    }
    @Test
    void testValidateCoupon_MaxRedemptions() {
        coupon.setRedeemedCount(100);
        coupon.setMaxRedemptions(100);
        when(couponRepository.findByCodeAndStatus("DISCOUNT10", Status.ACTIVE)).thenReturn(Optional.of(coupon));
        assertThrows(RuntimeException.class, () -> customerBillingService.validateCoupon("DISCOUNT10"));
    }
    // ==================== GET AVAILABLE COUPONS ====================
    @Test
    void testGetAvailableCoupons() {
        when(couponRepository.findAll()).thenReturn(Arrays.asList(coupon));
        List<CouponDTO> result = customerBillingService.getAvailableCoupons();
        assertEquals(1, result.size());
        assertEquals("DISCOUNT10", result.get(0).getCode());
    }
    @Test
    void testGetAvailableCoupons_FiltersExpired() {
        Coupon expired = Coupon.builder().id(2L).code("EXPIRED").name("Expired").type(CouponType.FIXED)
                .amount(100L).status(Status.ACTIVE).validFrom(LocalDate.now().minusDays(30))
                .validTo(LocalDate.now().minusDays(1)).redeemedCount(0).build();
        when(couponRepository.findAll()).thenReturn(Arrays.asList(coupon, expired));
        List<CouponDTO> result = customerBillingService.getAvailableCoupons();
        assertEquals(1, result.size()); // Only the valid coupon
    }
    @Test
    void testGetAvailableCoupons_FiltersInactive() {
        Coupon inactive = Coupon.builder().id(3L).code("INACTIVE").name("Inactive").type(CouponType.FIXED)
                .amount(100L).status(Status.DISABLED).validFrom(LocalDate.now().minusDays(1))
                .redeemedCount(0).build();
        when(couponRepository.findAll()).thenReturn(Arrays.asList(coupon, inactive));
        List<CouponDTO> result = customerBillingService.getAvailableCoupons();
        assertEquals(1, result.size());
    }
    // ==================== HELPER: USER NOT FOUND ====================
    @Test
    void testGetInvoices_UserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerBillingService.getInvoices("unknown@test.com", null, null, null));
    }
    @Test
    void testGetInvoices_CustomerNotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerBillingService.getInvoices("test@test.com", null, null, null));
    }




    @Test
    void testValidateCoupon_AllNullFields() {
        Coupon nullCoupon = Coupon.builder()
                .id(99L).code("NULLFIELDS").name("Null Coupon").type(CouponType.PERCENT)
                .amount(10L).status(Status.ACTIVE).validFrom(LocalDate.now().minusDays(1)).validTo(null)
                .maxRedemptions(null).redeemedCount(0).build();
        when(couponRepository.findByCodeAndStatus("NULLFIELDS", Status.ACTIVE)).thenReturn(Optional.of(nullCoupon));
        CouponDTO dto = customerBillingService.validateCoupon("NULLFIELDS");
        assertEquals("NULLFIELDS", dto.getCode());
        assertNull(dto.getValidTo());
    }
    @Test
    void testGetAvailableCoupons_AllNullFields() {
        Coupon nullCoupon = Coupon.builder()
                .id(99L).code("NULLFIELDS").name("Null Coupon").type(CouponType.PERCENT)
                .amount(10L).status(Status.ACTIVE).validFrom(LocalDate.now().minusDays(1)).validTo(null)
                .maxRedemptions(null).redeemedCount(0).build();
        when(couponRepository.findAll()).thenReturn(Arrays.asList(nullCoupon));
        List<CouponDTO> result = customerBillingService.getAvailableCoupons();
        assertEquals(1, result.size());
        assertEquals("NULLFIELDS", result.get(0).getCode());
    }
    @Test
    void testGetPayments_WithInvoice() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvoice(invoice);
        payment.setAmountMinor(1000L);
        payment.setCurrency("USD");
        payment.setStatus(Status.SUCCESS);
        payment.setAttemptNo(1);
        payment.setFailureReason(null);
        payment.setCreatedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(customer));
        when(paymentRepository.findByInvoice_Customer_Id(1L))
                .thenReturn(List.of(payment));
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(invoice));
        List<PaymentDTO> result =
                customerBillingService.getPayments("test@test.com");
        assertEquals(1, result.size());
        assertEquals("INV-1", result.get(0).getInvoiceNumber());
    }
    @Test
    void testGetPayments_WithoutInvoice() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvoice(null);
        payment.setAmountMinor(1000L);
        payment.setCurrency("USD");
        payment.setStatus(Status.SUCCESS);
        payment.setAttemptNo(1);
        payment.setCreatedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(customer));
        when(paymentRepository.findByInvoice_Customer_Id(1L))
                .thenReturn(List.of(payment));
        List<PaymentDTO> result =
                customerBillingService.getPayments("test@test.com");
        assertEquals(1, result.size());
        assertEquals("N/A", result.get(0).getInvoiceNumber());
        assertNull(result.get(0).getInvoiceId());
    }
    @Test
    void testGetPayments_InvoiceLookupReturnsNull() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvoice(invoice);
        payment.setAmountMinor(1000L);
        payment.setCurrency("USD");
        payment.setStatus(Status.SUCCESS);
        payment.setAttemptNo(1);
        payment.setCreatedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(customer));
        when(paymentRepository.findByInvoice_Customer_Id(1L))
                .thenReturn(List.of(payment));
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.empty());
        List<PaymentDTO> result =
                customerBillingService.getPayments("test@test.com");
        assertEquals("N/A", result.get(0).getInvoiceNumber());
    }
    // ==================== GET CREDIT NOTES ====================
    @Test
    void testGetCreditNotes_WithInvoice() {
        CreditNote creditNote = new CreditNote();
        creditNote.setId(1L);
        creditNote.setCreditNoteNumber("CN-1");
        creditNote.setInvoice(invoice);
        creditNote.setReason("Refund");
        creditNote.setAmountMinor(500L);
        creditNote.setStatus(Status.SUCCESS);
        creditNote.setCreatedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(customer));
        when(creditNoteRepository.findByInvoice_Customer_Id(1L))
                .thenReturn(List.of(creditNote));
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(invoice));
        List<CreditNoteDTO> result =
                customerBillingService.getCreditNotes("test@test.com");
        assertEquals(1, result.size());
        assertEquals("INV-1", result.get(0).getInvoiceNumber());
    }
    @Test
    void testGetCreditNotes_WithoutInvoice() {
        CreditNote creditNote = new CreditNote();
        creditNote.setId(1L);
        creditNote.setCreditNoteNumber("CN-1");
        creditNote.setInvoice(null);
        creditNote.setReason("Refund");
        creditNote.setAmountMinor(500L);
        creditNote.setStatus(Status.SUCCESS);
        creditNote.setCreatedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(customer));
        when(creditNoteRepository.findByInvoice_Customer_Id(1L))
                .thenReturn(List.of(creditNote));
        List<CreditNoteDTO> result =
                customerBillingService.getCreditNotes("test@test.com");
        assertEquals(1, result.size());
        assertEquals("N/A", result.get(0).getInvoiceNumber());
        assertNull(result.get(0).getInvoiceId());
    }
    @Test
    void testGetCreditNotes_InvoiceLookupReturnsNull() {
        CreditNote creditNote = new CreditNote();
        creditNote.setId(1L);
        creditNote.setCreditNoteNumber("CN-1");
        creditNote.setInvoice(invoice);
        creditNote.setReason("Refund");
        creditNote.setAmountMinor(500L);
        creditNote.setStatus(Status.SUCCESS);
        creditNote.setCreatedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(customer));
        when(creditNoteRepository.findByInvoice_Customer_Id(1L))
                .thenReturn(List.of(creditNote));
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.empty());
        List<CreditNoteDTO> result =
                customerBillingService.getCreditNotes("test@test.com");
        assertEquals("N/A", result.get(0).getInvoiceNumber());
    }
    // ==================== AVAILABLE COUPON FILTERS ====================
    @Test
    void testGetAvailableCoupons_NotYetValid() {
        Coupon futureCoupon = Coupon.builder()
                .id(5L)
                .code("FUTURE")
                .name("Future")
                .type(CouponType.PERCENT)
                .amount(10L)
                .status(Status.ACTIVE)
                .validFrom(LocalDate.now().plusDays(5))
                .redeemedCount(0)
                .build();
        when(couponRepository.findAll())
                .thenReturn(List.of(coupon, futureCoupon));
        List<CouponDTO> result =
                customerBillingService.getAvailableCoupons();
        assertEquals(1, result.size());
        assertEquals("DISCOUNT10", result.get(0).getCode());
    }
    @Test
    void testGetAvailableCoupons_MaxRedemptionsReached() {
        Coupon maxedCoupon = Coupon.builder()
                .id(6L)
                .code("MAXED")
                .name("Maxed")
                .type(CouponType.PERCENT)
                .amount(10L)
                .status(Status.ACTIVE)
                .validFrom(LocalDate.now().minusDays(1))
                .redeemedCount(10)
                .maxRedemptions(10)
                .build();
        when(couponRepository.findAll())
                .thenReturn(List.of(coupon, maxedCoupon));
        List<CouponDTO> result =
                customerBillingService.getAvailableCoupons();
        assertEquals(1, result.size());
        assertEquals("DISCOUNT10", result.get(0).getCode());
    }
}