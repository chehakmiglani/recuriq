# RecurIQ - Backend

This is the backend server for RecurIQ, a Subscription Billing and Revenue Management System (demo brand: StreamFlix). The backend is designed as a secure, high-performance, and scalable core responsible for subscription lifecycle automation, billing schedules, invoicing engine computations, dunning retry policies, and real-time revenue analytics.

---

## Features and Characteristics

### Subscription Lifecycle State Machine
The system implements a robust state machine that strictly governs subscription status transitions. It manages subscription states including trialing, active, paused, canceled, and past due, ensuring consistency and preventing illegal transitions.

### Automated Billing Engine
Automated cron schedulers drive the periodic billing system. The billing engine performs batch runs to detect expiring subscriptions, generate detailed invoices, trigger payment transactions, and log invoice status transitions.

### Fair Proration Calculations
The server handles mid-cycle subscription plan adjustments through a dynamic proration engine. When a customer upgrades or downgrades, the engine calculates the remaining unused credit of the existing plan, computes the prorated fee for the new plan, and issues immediate adjustment charges or future credits. It prevents one-time coupons from automatically applying to proration adjustments while ensuring that paid discounts are correctly factored into refund calculations.

### Taxation and Discounts Core
Region-based tax configurations are automatically applied to line items in inclusive or exclusive modes. The discount sub-system validates and applies fixed-amount or percentage-based coupons with specific duration constraints, such as one-time use, repeating intervals, or unlimited durations.

### Revenue Analytics and Auditing
Every monitory and state action triggers secure audit logging. The reporting database service aggregates historical transactional data to compute and compile operational metrics including Monthly Recurring Revenue (MRR), Annual Recurring Revenue (ARR), Churn percentages, Average Revenue Per User (ARPU), and Lifetime Value (LTV).

---

## Core Workflows

### Subscription Creation and Activation
1. The server receives a plan selection and payment profile registration.
2. The discount module validates if a coupon is eligible and calculates the discounted rate.
3. The server computes taxes based on regional tax rules.
4. The subscription is initialized in the active or trialing state.
5. An initial invoice is generated, and a payment transaction is executed against the mock gateway.
6. A notification task is triggered to send the invoice receipt.

### Scheduled Renewal Billing Cycle
1. A cron job executes periodically to query for subscriptions due to renew in the next cycle.
2. The billing engine constructs draft renewal invoices listing plan base charges, active discounts, and calculated taxes.
3. The invoice status is finalized, and an idempotent charge request is sent to the mock payment processor.
4. On successful payment, the invoice is marked paid, the subscription's next billing cycle dates are extended, and a receipt is issued.
5. If payment fails, the invoice status changes to unpaid, the subscription moves to past due, and a dunning queue task is registered.

### Plan Upgrade or Downgrade with Proration
1. The server receives an upgrade or downgrade request mid-cycle.
2. The proration calculator computes the unused balance of the current plan from the last payment date to the change date.
3. It computes the cost of the new plan for the remaining days in the billing period.
4. One-time coupons are bypassed on the new plan fee to prevent duplicate discount application.
5. The difference between the prorated charge and the unused credit is computed.
6. A proration invoice is finalized, charging the net positive amount immediately or recording a negative balance as customer credit for the next cycle.

### Dunning and Recovery Management
1. When a recurring transaction fails, the server records a failed attempt log.
2. A dunning job schedules successive payment retries at configurable intervals.
3. Notifications are issued to prompt the customer to update their billing credentials.
4. If payment succeeds during dunning, the subscription is restored to active.
5. If all retry attempts are exhausted without success, the subscription is automatically canceled or held per administrative policy.
