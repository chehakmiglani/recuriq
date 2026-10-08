package com.recuriq.billing.service;
import com.recuriq.billing.dto.customer.*;
import java.util.List;

public interface CustomerBillingService {
   List<InvoiceDTO> getInvoices(String email, String status, String from, String to);
   InvoiceDTO getInvoiceDetail(String email, Long invoiceId);
   byte[] generateInvoicePdf(String email, Long invoiceId);
   List<PaymentDTO> getPayments(String email);
   List<CreditNoteDTO> getCreditNotes(String email);
   CouponDTO applyCoupon(String email, String code);
   CouponDTO validateCoupon(String code);
   List<CouponDTO> getAvailableCoupons();
}
