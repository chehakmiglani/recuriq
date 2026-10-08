package com.recuriq.billing.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recuriq.billing.dto.ai.ChurnRiskDTO;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.Payment;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.UsageRecord;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.CancellationRequestStatus;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.CancellationRequestRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.PaymentRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import com.recuriq.billing.repository.UsageRecordRepository;

import lombok.RequiredArgsConstructor;

/**
 * Explainable churn-risk scoring.
 *
 * Each live subscription is scored 0-100 from billing signals that usually come
 * before a cancellation (failed payments, overdue invoices, pending cancellations,
 * pauses, falling usage). Every point added or removed is recorded as a reason,
 * so support and finance can see why a customer was flagged.
 */
@Service
@RequiredArgsConstructor
public class ChurnRiskServiceImpl implements ChurnRiskService {

    static final int HIGH_THRESHOLD = 60;
    static final int MEDIUM_THRESHOLD = 30;

    private static final List<Status> LIVE_STATUSES =
            List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final CancellationRequestRepository cancellationRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public ChurnRiskSummaryDTO getChurnRisk(String level) {
        LocalDateTime now = LocalDateTime.now();

        Map<Long, List<Payment>> paymentsByCustomer = paymentRepository.findAll().stream()
                .filter(p -> p.getInvoice() != null && p.getInvoice().getCustomer() != null)
                .collect(Collectors.groupingBy(p -> p.getInvoice().getCustomer().getId()));

        Map<Long, List<Invoice>> invoicesByCustomer = invoiceRepository.findAll().stream()
                .filter(i -> i.getCustomer() != null)
                .collect(Collectors.groupingBy(i -> i.getCustomer().getId()));

        Map<Long, List<UsageRecord>> usageBySubscription = usageRecordRepository.findAll().stream()
                .filter(u -> u.getSubscription() != null && u.getRecordedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getSubscription().getId()));

        Set<Long> pendingCancellationSubs = cancellationRequestRepository
                .findByStatus(CancellationRequestStatus.PENDING).stream()
                .map(r -> r.getSubscription().getId())
                .collect(Collectors.toSet());

        List<ChurnRiskDTO> scored = subscriptionRepository.findByStatusIn(LIVE_STATUSES).stream()
                .filter(s -> s.getCustomer() != null && s.getPlan() != null)
                .map(s -> score(s, now,
                        paymentsByCustomer.getOrDefault(s.getCustomer().getId(), List.of()),
                        invoicesByCustomer.getOrDefault(s.getCustomer().getId(), List.of()),
                        usageBySubscription.getOrDefault(s.getId(), List.of()),
                        pendingCancellationSubs.contains(s.getId())))
                .sorted(Comparator.comparingInt(ChurnRiskDTO::getScore).reversed())
                .toList();

        List<ChurnRiskDTO> filtered = (level == null || level.isBlank())
                ? scored
                : scored.stream().filter(r -> r.getLevel().equalsIgnoreCase(level)).toList();

        return ChurnRiskSummaryDTO.builder()
                .highRisk(count(scored, "HIGH"))
                .mediumRisk(count(scored, "MEDIUM"))
                .lowRisk(count(scored, "LOW"))
                .monthlyRevenueAtRiskMinor(scored.stream()
                        .filter(r -> "HIGH".equals(r.getLevel()))
                        .mapToLong(ChurnRiskDTO::getMonthlyValueMinor).sum())
                .customers(filtered)
                .build();
    }

    ChurnRiskDTO score(Subscription sub, LocalDateTime now, List<Payment> payments,
                       List<Invoice> invoices, List<UsageRecord> usage, boolean pendingCancellation) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        LocalDate today = now.toLocalDate();

        if (pendingCancellation) {
            score += 35;
            reasons.add("Has a pending cancellation request");
        }
        if (Boolean.TRUE.equals(sub.getCancelAtPeriodEnd())) {
            score += 35;
            reasons.add("Set to cancel at the end of the current period");
        }
        if (sub.getStatus() == Status.PAST_DUE) {
            score += 30;
            reasons.add("Subscription is past due");
        }
        if (sub.getStatus() == Status.PAUSED) {
            score += 20;
            reasons.add("Subscription is paused");
        }

        long recentFailures = payments.stream()
                .filter(p -> p.getStatus() == Status.FAILED)
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(now.minusDays(90)))
                .count();
        if (recentFailures > 0) {
            score += (int) Math.min(36, recentFailures * 12);
            reasons.add(recentFailures + " failed payment" + (recentFailures > 1 ? "s" : "") + " in the last 90 days");
        }

        long overdue = invoices.stream()
                .filter(i -> i.getStatus() == Status.OPEN && i.getDueDate() != null && i.getDueDate().isBefore(today))
                .count();
        if (overdue > 0) {
            score += 15;
            reasons.add(overdue + " overdue invoice" + (overdue > 1 ? "s" : ""));
        }

        if (sub.getStatus() == Status.TRIALING && sub.getTrialEndDate() != null
                && !sub.getTrialEndDate().isAfter(today.plusDays(7))) {
            score += 10;
            reasons.add("Trial ends within 7 days");
        }

        long lastMonthUsage = sumUsage(usage, now.minusDays(30), now);
        long previousMonthUsage = sumUsage(usage, now.minusDays(60), now.minusDays(30));
        if (previousMonthUsage > 0 && lastMonthUsage * 2 < previousMonthUsage) {
            score += 15;
            int drop = (int) Math.round(100.0 * (previousMonthUsage - lastMonthUsage) / previousMonthUsage);
            reasons.add("Usage dropped " + drop + "% compared with the previous 30 days");
        }

        long tenureDays = sub.getStartDate() == null ? 0 : ChronoUnit.DAYS.between(sub.getStartDate(), today);
        if (tenureDays < 60) {
            score += 5;
            reasons.add("New customer (under 60 days)");
        } else if (tenureDays > 365) {
            score -= 10;
            reasons.add("Loyal customer (over a year)");
        }

        long recentSuccesses = payments.stream()
                .filter(p -> p.getStatus() == Status.SUCCESS)
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(now.minusDays(120)))
                .count();
        if (recentFailures == 0 && recentSuccesses >= 3) {
            score -= 10;
            reasons.add("Consistent on-time payments");
        }

        score = Math.max(0, Math.min(100, score));
        if (reasons.isEmpty()) {
            reasons.add("No risk signals");
        }

        long price = Objects.requireNonNullElse(sub.getPlan().getDefaultPriceMinor(), 0L);
        long monthly = sub.getPlan().getBillingPeriod() == BillingPeriod.YEARLY ? price / 12 : price;

        return ChurnRiskDTO.builder()
                .customerId(sub.getCustomer().getId())
                .customerName(sub.getCustomer().getUser() != null ? sub.getCustomer().getUser().getFullName() : null)
                .email(sub.getCustomer().getUser() != null ? sub.getCustomer().getUser().getEmail() : null)
                .country(sub.getCustomer().getCountry())
                .planName(sub.getPlan().getName())
                .subscriptionStatus(sub.getStatus().name())
                .monthlyValueMinor(monthly)
                .currency(sub.getPlan().getDefaultCurrency())
                .score(score)
                .level(score >= HIGH_THRESHOLD ? "HIGH" : score >= MEDIUM_THRESHOLD ? "MEDIUM" : "LOW")
                .reasons(reasons)
                .build();
    }

    private static long sumUsage(List<UsageRecord> usage, LocalDateTime from, LocalDateTime to) {
        return usage.stream()
                .filter(u -> u.getRecordedAt().isAfter(from) && !u.getRecordedAt().isAfter(to))
                .mapToLong(u -> Objects.requireNonNullElse(u.getQuantity(), 0L))
                .sum();
    }

    private static int count(List<ChurnRiskDTO> list, String level) {
        return (int) list.stream().filter(r -> level.equals(r.getLevel())).count();
    }
}
