# StreamFlix — Dunning & Retry Architecture

> **Status:** Design Document — Not Yet Implemented  
> **Date:** May 11, 2026

---

## Overview

Dunning is the automated process of recovering failed subscription payments through scheduled retry attempts and customer notifications. When a customer's payment fails at renewal, the system enters a dunning cycle rather than immediately canceling the subscription.

---

## Core Actors

| Actor | Responsibility |
|---|---|
| **System (Billing Engine)** | Detects payment failures, executes retries per schedule, applies final action |
| **Admin** | Configures retry schedule, max attempts, and final action policy |
| **Customer** | Receives reminders and updates payment method |

---

## Data Model

### `DunningConfig` (Per-tenant / Global configuration)

```java
@Entity
public class DunningConfig {
    Long id;
    String retrySchedule;          // e.g., "1,3,7" (days after failure)
    Integer maxRetryAttempts;      // e.g., 3
    FinalAction finalAction;       // CANCEL or ON_HOLD
    Boolean sendReminderEmails;    // true
    Boolean sendReminderSms;       // false
}
```

### `DunningAttempt` (Audit log of each retry)

```java
@Entity
public class DunningAttempt {
    Long id;
    Subscription subscription;     // Which subscription
    Integer attemptNumber;         // 1st, 2nd, 3rd attempt
    LocalDateTime attemptedAt;     // When the retry happened
    AttemptResult result;          // SUCCESS or FAILED
    String failureReason;          // e.g., "insufficient_funds", "card_expired"
    Long amountMinor;              // Amount that was attempted
    String gatewayRef;             // Mock gateway reference
}
```

---

## Status Transitions

```
ACTIVE ──payment fails──> PAST_DUE ──retry 1 (day 1)──> [SUCCESS] ──> ACTIVE
                              │
                              └──retry 2 (day 3)──> [SUCCESS] ──> ACTIVE
                              │
                              └──retry 3 (day 7)──> [FAIL] ──> finalAction
                                                              │
                                                    CANCEL ──> CANCELED
                                                    ON_HOLD ──> ON_HOLD
```

---

## Scheduler Design

### Daily Dunning Job (`DunningService`)

```java
@Service
public class DunningService {

    @Scheduled(cron = "0 0 6 * * ?") // 6:00 AM daily
    public void processDunningCycles() {
        // 1. Find all PAST_DUE subscriptions with attempts < max
        // 2. For each, check if today matches the retry schedule
        // 3. If match: attempt payment via MockPaymentGateway
        // 4. On success: mark ACTIVE, send success notification
        // 5. On failure: log attempt, send reminder if configured
        // 6. If max attempts reached: apply finalAction (CANCEL or ON_HOLD)
    }
}
```

### Retry Schedule Example

| Failure Day | Retry Day | Days After Failure |
|---|---|---|
| Jan 1 | Jan 2 | 1 |
| Jan 2 | Jan 5 | 3 |
| Jan 5 | Jan 12 | 7 |

If all 3 fail and `finalAction = CANCEL`, the subscription is canceled on Jan 12.

---

## Customer Notifications

### Email Templates

1. **Payment Failed (First Attempt)**
   - Subject: "Payment failed for your StreamFlix subscription"
   - Content: Amount, reason, direct link to update payment method

2. **Payment Failed (Subsequent Attempts)**
   - Subject: "Reminder: Update your payment method"
   - Content: Attempt number, days until cancellation, update link

3. **Subscription Canceled (Final Action)**
   - Subject: "Your subscription has been canceled"
   - Content: Reason (max retries), reactivation instructions

### Notification Preferences

Customers should be able to configure:
- Email notifications: ON / OFF
- SMS notifications: ON / OFF
- Reminder frequency: all attempts or final only

---

## Admin Configuration UI

### Dunning Settings Page (`/admin/dunning`)

**Fields:**
- Retry Schedule: Input (comma-separated days, e.g., "1,3,7")
- Max Retry Attempts: Number (1–10)
- Final Action: Radio buttons (`CANCEL subscription` / `Place ON_HOLD`)
- Send Email Reminders: Toggle
- Send SMS Reminders: Toggle

### Dunning History Table

Displays all active dunning cycles:
- Customer name
- Plan
- Failure date
- Attempts made
- Next retry date
- Status (in progress / resolved / canceled)

---

## API Endpoints (Proposed)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/dunning/config` | Get current dunning config |
| PUT | `/api/admin/dunning/config` | Update dunning config |
| GET | `/api/admin/dunning/history` | List active dunning attempts |
| POST | `/api/admin/dunning/retry/{subId}` | Manual retry (admin override) |

---

## Open Questions

1. **Should dunning config be global or per-plan?**  
   Recommendation: Global for simplicity; per-plan adds unnecessary complexity at this stage.

2. **What happens to `ON_HOLD` subscriptions?**  
   They stop generating invoices but remain recoverable. Customer can update payment method and manually reactivate.

3. **Should we support manual retry by admin?**  
   Yes — add `POST /api/admin/dunning/retry/{subscriptionId}` for support scenarios.

---

## Implementation Priority

1. Create `DunningConfig` and `DunningAttempt` entities
2. Implement `DunningService` with `@Scheduled` daily job
3. Add admin configuration page
4. Add customer notification service integration
5. Add dunning history view in admin dashboard

---

*End of Document*
