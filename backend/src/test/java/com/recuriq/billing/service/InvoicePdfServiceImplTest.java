package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.InvoiceLineItem;
import com.recuriq.billing.entity.Payment;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.BillingReason;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.repository.InvoiceLineItemRepository;
import com.recuriq.billing.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceImplTest {

    @Mock private InvoiceLineItemRepository lineItemRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private InvoicePdfServiceImpl invoicePdfService;

    private Invoice invoice;
    private Customer customer;
    private User user;
    private Subscription subscription;
    private Plan plan;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).fullName("Test User").email("test@test.com").build();
        customer = Customer.builder().id(1L).user(user).build();
        plan = Plan.builder().id(1L).name("Basic").billingPeriod(BillingPeriod.MONTHLY).build();
        subscription = Subscription.builder().id(1L).plan(plan).currentPeriodStart(LocalDate.now()).currentPeriodEnd(LocalDate.now().plusMonths(1)).build();
        invoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-1")
                .customer(customer)
                .subscription(subscription)
                .status(Status.PAID)
                .billingReason(BillingReason.SUBSCRIPTION_CYCLE)
                .issueDate(LocalDate.now())
                .currency("USD")
                .subtotalMinor(1000L)
                .totalMinor(1000L)
                .build();
    }

    @Test
    void testGeneratePdf() {
        InvoiceLineItem item = new InvoiceLineItem();
        item.setId(1L);
        item.setDescription("Item 1");
        item.setQuantity(1);
        item.setUnitPriceMinor(1000L);
        item.setAmountMinor(1000L);
        item.setLineType(InvoiceLineItem.LineType.PLAN);

        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList(item));
        when(paymentRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList());

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGeneratePdf_OpenStatus() {
        invoice.setStatus(Status.OPEN);
        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList());

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGeneratePdf_NullDueDate() {
        invoice.setDueDate(null);
        invoice.setIssueDate(null);
        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList());

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGeneratePdf_VoidStatus() {
        invoice.setStatus(Status.VOID);
        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList());

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGeneratePdf_DifferentCurrencies() {
        InvoiceLineItem item = new InvoiceLineItem();
        item.setId(1L);
        item.setDescription("Item INR");
        item.setQuantity(1);
        item.setUnitPriceMinor(50000L);
        item.setAmountMinor(-50000L); // Negative amount test
        item.setLineType(InvoiceLineItem.LineType.DISCOUNT);

        invoice.setCurrency("INR");
        invoice.setDiscountMinor(100L);
        invoice.setTaxMinor(50L);
        invoice.setBalanceMinor(950L);
        invoice.setStatus(Status.PAID);
        
        Payment payment = new Payment();
        payment.setGatewayRef("REF-123");
        payment.setCreatedAt(java.time.LocalDateTime.now());

        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList(item));
        when(paymentRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList(payment));

        byte[] pdfInr = invoicePdfService.generatePdf(invoice);
        assertNotNull(pdfInr);

        invoice.setCurrency("GBP");
        byte[] pdfGbp = invoicePdfService.generatePdf(invoice);
        assertNotNull(pdfGbp);

        invoice.setCurrency("UNKNOWN");
        byte[] pdfUnknown = invoicePdfService.generatePdf(invoice);
        assertNotNull(pdfUnknown);
    }

    @Test
    void testGeneratePdf_NullCustomerAndSubscription() {
        invoice.setCustomer(null);
        invoice.setSubscription(null);
        invoice.setBillingReason(null);
        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList());

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGeneratePdf_NullLineItemTypeAndNullAmount() {
        InvoiceLineItem item = new InvoiceLineItem();
        item.setId(1L);
        item.setDescription("Item nulls");
        item.setQuantity(1);
        item.setUnitPriceMinor(1000L);
        item.setAmountMinor(null);
        item.setLineType(null);

        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList(item));
        when(paymentRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList());

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGeneratePdf_CanceledStatus() {
        invoice.setStatus(Status.CANCELED);
        when(lineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList());

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGeneratePdf_ThrowsException() {
        // LineItem repository throwing an exception to trigger catch block
        when(lineItemRepository.findByInvoice_Id(1L)).thenThrow(new RuntimeException("DB Error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> invoicePdfService.generatePdf(invoice));
        assertEquals("Failed to generate invoice PDF", ex.getMessage());
    }
}
