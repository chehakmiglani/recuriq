package com.recuriq.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.request.ApplyCouponRequest;
import com.recuriq.billing.service.CustomerBillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerBillingController.class)
class CustomerBillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerBillingService billingService;

    private User authUser;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        authUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .fullName("Test Customer")
                .role(com.recuriq.billing.enums.UserRole.CUSTOMER)
                .status(Status.ACTIVE)
                .build();
        auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void testGetInvoices() throws Exception {
        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setInvoiceId(10L);
        when(billingService.getInvoices(anyString(), any(), any(), any())).thenReturn(List.of(invoice));

        mockMvc.perform(get("/api/customer/invoices")
                        .param("status", "PAID")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceId").value(10));

        verify(billingService, times(1)).getInvoices("test@test.com", "PAID", null, null);
    }

    @Test
    void testGetInvoiceDetail() throws Exception {
        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setInvoiceId(10L);
        when(billingService.getInvoiceDetail(anyString(), anyLong())).thenReturn(invoice);

        mockMvc.perform(get("/api/customer/invoices/10").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(10));

        verify(billingService, times(1)).getInvoiceDetail("test@test.com", 10L);
    }

    @Test
    void testDownloadInvoice() throws Exception {
        byte[] pdfBytes = "PDF Content".getBytes();
        when(billingService.generateInvoicePdf(anyString(), anyLong())).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/customer/invoices/10/download").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-10.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));

        verify(billingService, times(1)).generateInvoicePdf("test@test.com", 10L);
    }

    @Test
    void testGetPayments() throws Exception {
        PaymentDTO payment = new PaymentDTO();
        payment.setPaymentId(20L);
        when(billingService.getPayments(anyString())).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/customer/payments").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(20));

        verify(billingService, times(1)).getPayments("test@test.com");
    }

    @Test
    void testGetCreditNotes() throws Exception {
        CreditNoteDTO creditNote = new CreditNoteDTO();
        creditNote.setCreditNoteId(30L);
        when(billingService.getCreditNotes(anyString())).thenReturn(List.of(creditNote));

        mockMvc.perform(get("/api/customer/credit-notes").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creditNoteId").value(30));

        verify(billingService, times(1)).getCreditNotes("test@test.com");
    }

    @Test
    void testApplyCoupon() throws Exception {
        ApplyCouponRequest request = new ApplyCouponRequest();
        request.setCode("DISCOUNT50");
        
        CouponDTO coupon = new CouponDTO();
        coupon.setCode("DISCOUNT50");

        when(billingService.applyCoupon(anyString(), any())).thenReturn(coupon);

        mockMvc.perform(post("/api/customer/coupons/apply")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DISCOUNT50"));

        verify(billingService, times(1)).applyCoupon(eq("test@test.com"), any());
    }

    @Test
    void testValidateCoupon() throws Exception {
        ApplyCouponRequest request = new ApplyCouponRequest();
        request.setCode("DISCOUNT50");
        
        CouponDTO coupon = new CouponDTO();
        coupon.setCode("DISCOUNT50");

        when(billingService.validateCoupon(any())).thenReturn(coupon);

        mockMvc.perform(post("/api/customer/coupons/validate")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DISCOUNT50"));

        verify(billingService, times(1)).validateCoupon(any());
    }

    @Test
    void testGetCoupons() throws Exception {
        CouponDTO coupon = new CouponDTO();
        coupon.setCode("DISCOUNT50");
        when(billingService.getAvailableCoupons()).thenReturn(List.of(coupon));

        mockMvc.perform(get("/api/customer/coupons").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DISCOUNT50"));

        verify(billingService, times(1)).getAvailableCoupons();
    }
}
