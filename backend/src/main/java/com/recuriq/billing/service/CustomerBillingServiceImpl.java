package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.Duration;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerBillingServiceImpl implements CustomerBillingService {

   private final InvoiceRepository invoiceRepository;
   private final InvoiceLineItemRepository invoiceLineItemRepository;
   private final PaymentRepository paymentRepository;
   private final CreditNoteRepository creditNoteRepository;
   private final CouponRepository couponRepository;
   private final SubscriptionRepository subscriptionRepository;
   private final CustomerRepository customerRepository;
   private final UserRepository userRepository;
   private final InvoicePdfService invoicePdfService;
   private final SubscriptionCouponRepository subscriptionCouponRepository;

   public List<InvoiceDTO> getInvoices(String email, String status, String from, String to) {
       Customer customer = getCustomerByEmail(email);
       List<Invoice> invoices;
       
       if (status != null) {
           invoices = invoiceRepository.findByCustomer_IdAndStatus(customer.getId(), status);
       } else {
           invoices = invoiceRepository.findByCustomer_IdOrderByIssueDateDesc(customer.getId());
       }
       
       return invoices.stream().map(this::mapToInvoiceDTO).toList();
   }

   public InvoiceDTO getInvoiceDetail(String email, Long invoiceId) {
       Customer customer = getCustomerByEmail(email);
       Invoice invoice = invoiceRepository.findById(invoiceId)
               .orElseThrow(() -> CustomException.notFound("Invoice not found"));
       
       if (!invoice.getCustomer().getId().equals(customer.getId())) {
           throw CustomException.forbidden("Unauthorized");
       }
       
       return mapToInvoiceDTO(invoice);
   }

   public byte[] generateInvoicePdf(String email, Long invoiceId) {
       Customer customer = getCustomerByEmail(email);
       Invoice invoice = invoiceRepository.findById(invoiceId)
               .orElseThrow(() -> CustomException.notFound("Invoice not found"));
       
       if (!invoice.getCustomer().getId().equals(customer.getId())) {
           throw CustomException.forbidden("Unauthorized");
       }
       
       return invoicePdfService.generatePdf(invoice);
   }

   public List<PaymentDTO> getPayments(String email) {
       Customer customer = getCustomerByEmail(email);
       return paymentRepository.findByInvoice_Customer_Id(customer.getId()).stream()
               .map(this::mapToPaymentDTO)
               .toList();
   }

   public List<CreditNoteDTO> getCreditNotes(String email) {
       Customer customer = getCustomerByEmail(email);
       return creditNoteRepository.findByInvoice_Customer_Id(customer.getId()).stream()
               .map(this::mapToCreditNoteDTO)
               .toList();
   }

   @Transactional
   public CouponDTO applyCoupon(String email, String code) {
       Customer customer = getCustomerByEmail(email);
       Coupon coupon = couponRepository.findByCodeAndStatus(code, Status.ACTIVE)
               .orElseThrow(() -> CustomException.badRequest("Invalid or expired coupon"));
       
       Subscription subscription = subscriptionRepository.findByCustomer_IdAndStatusIn(
               customer.getId(), List.of(Status.ACTIVE, Status.TRIALING)).stream()
               .findFirst()
               .orElseThrow(() -> CustomException.badRequest("No active subscription"));

       // Deactivate any currently active coupon on this subscription to enforce one active coupon at a time
       java.util.Optional<SubscriptionCoupon> activeCouponOpt = subscriptionCouponRepository
               .findBySubscription_IdAndStatus(subscription.getId(), Status.ACTIVE);
       if (activeCouponOpt.isPresent()) {
           SubscriptionCoupon activeSc = activeCouponOpt.get();
           if (activeSc.getCoupon().getId().equals(coupon.getId())) {
               throw CustomException.conflict("Coupon already active on this subscription");
           }
           activeSc.setStatus(Status.CANCELED);
           subscriptionCouponRepository.save(activeSc);
       }

       // Link and persist the new coupon to the subscription
       SubscriptionCoupon sc = new SubscriptionCoupon();
       sc.setSubscription(subscription);
       sc.setCoupon(coupon);
       sc.setAppliedAt(LocalDateTime.now());
       sc.setStatus(Status.ACTIVE);
       sc.setExpiresAt(calculateSubscriptionCouponExpiry(coupon, subscription));
       subscriptionCouponRepository.save(sc);

       // Increment redemption count
       coupon.setRedeemedCount(coupon.getRedeemedCount() + 1);
       couponRepository.save(coupon);
       
       return mapToCouponDTO(coupon);
   }

   private LocalDateTime calculateSubscriptionCouponExpiry(Coupon coupon, Subscription subscription) {
       if (coupon.getDuration() == Duration.REPEATING && coupon.getDurationInMonths() != null) {
           return LocalDateTime.now().plusMonths(coupon.getDurationInMonths());
       }
       if (coupon.getDuration() == Duration.ONCE) {
           return subscription.getCurrentPeriodEnd() != null
                   ? subscription.getCurrentPeriodEnd().atStartOfDay()
                   : LocalDateTime.now().plusDays(1);
       }
       return null;
   }

    public CouponDTO validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndStatus(code, Status.ACTIVE)
                .orElseThrow(() -> CustomException.badRequest("Invalid or expired coupon"));

        java.time.LocalDate today = java.time.LocalDate.now();

        // Check date validity
        if (coupon.getValidFrom() != null && today.isBefore(coupon.getValidFrom())) {
            throw CustomException.badRequest("Coupon is not yet valid");
        }
        if (coupon.getValidTo() != null && today.isAfter(coupon.getValidTo())) {
            throw CustomException.badRequest("Coupon has expired");
        }

        // Check max redemptions
        if (coupon.getMaxRedemptions() != null && coupon.getRedeemedCount() >= coupon.getMaxRedemptions()) {
            throw CustomException.badRequest("Coupon has reached maximum redemptions");
        }

        return mapToCouponDTO(coupon);
    }

    public List<CouponDTO> getAvailableCoupons() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return couponRepository.findAll().stream()
                .filter(c -> c.getStatus() == Status.ACTIVE)
                .filter(c -> c.getValidFrom() == null || !today.isBefore(c.getValidFrom()))
                .filter(c -> c.getValidTo() == null || !today.isAfter(c.getValidTo()))
                .filter(c -> c.getMaxRedemptions() == null || c.getRedeemedCount() < c.getMaxRedemptions())
                .map(this::mapToCouponDTO)
                .toList();
    }

   private Customer getCustomerByEmail(String email) {
       User user = userRepository.findByEmail(email)
               .orElseThrow(() -> CustomException.notFound("User not found"));
       return customerRepository.findByUser_Id(user.getId())
               .orElseThrow(() -> CustomException.notFound("Customer not found"));
   }

   private InvoiceDTO mapToInvoiceDTO(Invoice invoice) {
       InvoiceDTO dto = new InvoiceDTO();
       dto.setInvoiceId(invoice.getId());
       dto.setInvoiceNumber(invoice.getInvoiceNumber());
       dto.setSubscriptionId(invoice.getSubscription().getId());
       dto.setStatus(invoice.getStatus());
       dto.setBillingReason(invoice.getBillingReason());
       dto.setIssueDate(invoice.getIssueDate().toString());
       dto.setDueDate(invoice.getDueDate() != null ? invoice.getDueDate().toString() : null);
       dto.setSubtotalMinor(invoice.getSubtotalMinor());
       dto.setTaxMinor(invoice.getTaxMinor());
       dto.setDiscountMinor(invoice.getDiscountMinor());
       dto.setTotalMinor(invoice.getTotalMinor());
       dto.setBalanceMinor(invoice.getBalanceMinor());
       dto.setCurrency(invoice.getCurrency());
       
       List<InvoiceLineItem> items = invoiceLineItemRepository.findByInvoice_Id(invoice.getId());
       dto.setLineItems(items.stream().map(this::mapToLineItemDTO).toList());
       
       return dto;
   }

   private InvoiceLineItemDTO mapToLineItemDTO(InvoiceLineItem item) {
       InvoiceLineItemDTO dto = InvoiceLineItemDTO.builder().build();
       dto.setLineItemId(item.getId());
       dto.setDescription(item.getDescription());
       dto.setLineType(item.getLineType());
       dto.setQuantity(item.getQuantity());
       dto.setUnitPriceMinor(item.getUnitPriceMinor());
       dto.setAmountMinor(item.getAmountMinor());
       dto.setPeriodStart(item.getPeriodStart() != null ? item.getPeriodStart().toString() : null);
       dto.setPeriodEnd(item.getPeriodEnd() != null ? item.getPeriodEnd().toString() : null);
       return dto;
   }

   private PaymentDTO mapToPaymentDTO(Payment payment) {
       PaymentDTO dto = new PaymentDTO();
       dto.setPaymentId(payment.getId());
       if (payment.getInvoice() != null) {
           dto.setInvoiceId(payment.getInvoice().getId());
           Invoice inv = invoiceRepository.findById(payment.getInvoice().getId()).orElse(null);
           dto.setInvoiceNumber(inv != null ? inv.getInvoiceNumber() : "N/A");
       } else {
           dto.setInvoiceId(null);
           dto.setInvoiceNumber("N/A");
       }
       dto.setAmountMinor(payment.getAmountMinor());
       dto.setCurrency(payment.getCurrency());
       dto.setStatus(payment.getStatus());
       dto.setAttemptNo(payment.getAttemptNo());
       dto.setFailureReason(payment.getFailureReason());
       dto.setCreatedAt(payment.getCreatedAt().toString());
       return dto;
   }

   private CreditNoteDTO mapToCreditNoteDTO(CreditNote creditNote) {
       CreditNoteDTO dto = new CreditNoteDTO();
       dto.setCreditNoteId(creditNote.getId());
       dto.setCreditNoteNumber(creditNote.getCreditNoteNumber());
       if (creditNote.getInvoice() != null) {
           dto.setInvoiceId(creditNote.getInvoice().getId());
           Invoice inv = invoiceRepository.findById(creditNote.getInvoice().getId()).orElse(null);
           dto.setInvoiceNumber(inv != null ? inv.getInvoiceNumber() : "N/A");
       } else {
           dto.setInvoiceId(null);
           dto.setInvoiceNumber("N/A");
       }
       dto.setReason(creditNote.getReason());
       dto.setAmountMinor(creditNote.getAmountMinor());
       dto.setStatus(creditNote.getStatus());
       dto.setCreatedAt(creditNote.getCreatedAt().toString());
       return dto;
   }

   private CouponDTO mapToCouponDTO(Coupon coupon) {
       CouponDTO dto = new CouponDTO();
       dto.setCouponId(coupon.getId());
       dto.setCode(coupon.getCode());
       dto.setName(coupon.getName());
       dto.setType(coupon.getType());
       dto.setAmount(coupon.getAmount());
       dto.setCurrency(coupon.getCurrency());
       dto.setDuration(coupon.getDuration());
       dto.setDurationInMonths(coupon.getDurationInMonths());
       dto.setValidFrom(coupon.getValidFrom().toString());
       dto.setValidTo(coupon.getValidTo() != null ? coupon.getValidTo().toString() : null);
       dto.setStatus(coupon.getStatus());
       return dto;
   }
}
