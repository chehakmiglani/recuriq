# RecurIQ - Frontend

This is the web client for RecurIQ, a Subscription Billing and Revenue Management System (demo brand: StreamFlix). The application provides a modern and responsive user interface featuring dedicated, role-based dashboards that allow customers, administrators, finance managers, and support agents to interact with subscription cycles, catalogs, billing histories, and financial analytics.

---

## Features and Characteristics

### Role-Based Portal Access
The application organizes all functional capabilities into four independent workspaces based on permissions. This layout keeps sensitive financial records and catalog controls accessible only to authorized internal teams, while providing a clear interface for customer self-service.

### Subscription Onboarding Wizard
A guided wizard leads customers through the entire initial sign-up flow. The wizard supports step-by-step selection of plans and billing intervals, real-time validation of active coupon codes, payment details input, and final activation confirmation.

### Customer Self-Service Dashboard
The customer portal acts as a central hub where users can inspect their active subscription status and renewal dates. It allows customers to download past PDF invoices, update registered billing methods, pause or resume services, cancel subscriptions, and trigger immediate upgrades or downgrades.

### Administrative Catalog Management
Administrators have access to forms and tables configured to manage the product catalog. This includes creating or modifying subscription plans, setting active tax rules, and generating promotional coupons with specific redemptions limits, start dates, and end dates.

### Financial Monitoring and Reporting
The finance dashboard aggregates real-time operational records. It presents interactive graphical widgets displaying critical subscription metrics, including Monthly Recurring Revenue (MRR), Annual Recurring Revenue (ARR), Churn percentages, Average Revenue Per User (ARPU), and Customer Lifetime Value (LTV). It also logs recent automated billing cron cycles.

### Customer Support Console
Support agents are provided with lookup tools to retrieve customer accounts and review detailed histories. The console displays active subscription items, historical payments, dunning retry attempts, and allows agents to initiate partial or full refunds, which automatically trigger credit note generations.

---

## Core Workflows

### Guided Customer Registration and Subscription
1. The customer selects a product tier and preferred billing frequency.
2. The user enters a promotional coupon, and the interface queries the validation service to calculate the adjusted total price.
3. The customer proceeds to enter mock credit card details.
4. The frontend compiles and sends the registration data to the backend API to initialize the subscription, presenting a success confirmation view.

### Customer Plan Adjustment
1. From the self-service portal, the customer selects a new subscription plan to upgrade or downgrade.
2. The interface retrieves proration calculations from the server, displaying the unused plan credit, the new plan cost, and the net invoice adjustment.
3. The customer reviews and confirms the changes, which triggers an immediate payment check or credit update.

### Administrative Catalog Registration
1. An administrator creates a new product and registers various pricing intervals.
2. The administrator defines regional tax structures or creates promotional coupon codes.
3. The dashboard registers these changes with the catalog backend, making them immediately available for new customers.

### Support Agent Dispute Resolution
1. A support agent looks up a customer by email or subscription ID.
2. The agent reviews dunning logs or recent invoices.
3. If necessary, the agent initiates a refund request from the interface, prompting the system to adjust records and issue a credit note.
