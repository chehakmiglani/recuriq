# StreamFlix Syntactical Code-Level Execution Flows
## Deep-Dive Code-Paths, Annotation Processors, and Database Mutations

This document provides a highly detailed, line-by-line syntactical execution flow of the four most critical application operations in StreamFlix: **Customer Registration**, **Customer Login**, **Subscription Checkout**, and **Active Prorated Upgrades**.

---

## 1. Customer Registration Flow (Step-by-Step Code-Path)

This flow maps how a guest enters their coordinates on the frontend and is registered securely in the MySQL database.

```
[React View] ────> [authService.ts] ────> [Tomcat Gateway] ────> [AuthController] ────> [AuthServiceImpl] ────> [MySQL DB]
```

### Step 1: Frontend Client Dispatch
*   **Source File:** `CustomerAuthPage.tsx`
*   **Event:** User fills in their Name, Email, and Password and clicks the "Sign Up" button.
*   **Syntactical Execution:**
    1.  The component validates fields (non-empty, email format matching).
    2.  Invokes the `register(email, name, password)` wrapper inside `authService.ts`.
    3.  `authService.ts` fires a native `fetch` query:
        ```typescript
        const res = await fetch('/api/customer/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, fullName: name, password })
        });
        ```

### Step 2: Tomcat Gateway & Web Security Check
*   **Source File:** `SecurityConfig.java`
*   **Event:** Request arrives at Tomcat's request handler.
*   **Syntactical Execution:**
    1.  The `DelegatingFilterProxy` catches the path `/api/customer/register`.
    2.  Spring Security matches this request path against the rule:
        ```java
        .requestMatchers("/api/customer/login", "/api/customer/register").permitAll()
        ```
    3.  Since it is permitted, the request is allowed to bypass the session filter and is dispatched to the standard MVC `DispatcherServlet`.

### Step 3: Controller Parameter Mapping & Validation
*   **Source File:** `AuthController.java`
*   **Event:** The REST controller parses the request.
*   **Syntactical Execution:**
    1.  The path matches the mapped endpoint:
        ```java
        @PostMapping("/customer/register")
        public ResponseEntity<UserDTO> registerCustomer(@Valid @RequestBody RegisterRequest request) {
            return ResponseEntity.ok(authService.registerCustomer(request));
        }
        ```
    2.  The `@Valid` annotation triggers Spring's validator to verify the fields inside `RegisterRequest` (annotated with `@NotBlank`, `@Email`, and `@Size(min=6)` on the password).
    3.  Jackson deserializes the JSON payload into a structured `RegisterRequest` object.

### Step 4: Transactional Service Persistence Logic
*   **Source File:** `AuthServiceImpl.java`
*   **Event:** Spring executes `registerCustomer` within transactional boundaries.
*   **Syntactical Execution:**
    1.  The service checks for duplicate emails:
        ```java
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw CustomException.badRequest("Email already registered");
        }
        ```
    2.  Encrypts the raw password:
        ```java
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        ```
    3.  Assembles and saves the core credential record:
        ```java
        User user = User.builder()
                .email(request.getEmail())
                .password(hashedPassword)
                .role(UserRole.ROLE_CUSTOMER)
                .build();
        User savedUser = userRepository.save(user); // SQL: INSERT INTO user ...
        ```
    4.  Assembles and saves the customer profile record linked to the user:
        ```java
        Customer customer = Customer.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .currency("INR")
                .status(Status.ACTIVE)
                .build();
        customerRepository.save(customer); // SQL: INSERT INTO customer ...
        ```
    5.  Maps `savedUser` to `UserDTO.from(savedUser)` and returns it. Spring serializes it to JSON with an HTTP 200 OK status.

---

## 2. Customer Login & Session Set Flow (Step-by-Step Code-Path)

This flow details how user credentials are verified and how a stateful session is created.

```
[React View] ────> [authService.ts] ────> [SecurityConfig] ────> [AuthController] ────> [DaoAuthProvider] ────> [Session Generated]
```

### Step 1: Login Request Dispatch
*   **Source File:** `CustomerAuthPage.tsx`
*   **Syntactical Execution:**
    1.  User enters credentials and clicks "Log In".
    2.  Fires `loginCustomer(email, password)` from `authService.ts` executing:
        ```typescript
        const res = await fetch('/api/customer/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        ```

### Step 2: Controller Entry & Authentication Manager Delegation
*   **Source File:** `AuthController.java`
*   **Syntactical Execution:**
    1.  Endpoint matches:
        ```java
        @PostMapping("/customer/login")
        public ResponseEntity<UserDTO> loginCustomer(@RequestBody LoginRequest request, HttpServletRequest req, HttpServletResponse res)
        ```
    2.  Delegates validation to Spring's core authentication engine:
        ```java
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        ```

### Step 3: DaoAuthenticationProvider Lookup
*   **Under-the-hood System Actions:**
    1.  Spring Security's `DaoAuthenticationProvider` retrieves the custom `UserDetailsService` implementation to locate the user:
        ```java
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        ```
    2.  It matches the password by comparing the raw login input with the stored hash using:
        ```java
        passwordEncoder.matches(rawPassword, hashedPassword)
        ```
    3.  If successful, it returns a valid, authenticated `Authentication` token. If the password matches, the runtime proceeds. If not, it throws a `BadCredentialsException`.

### Step 4: Session Generation & Context Binding
*   **Source File:** `AuthController.java`
*   **Syntactical Execution:**
    1.  The controller binds the authenticated user token to the thread context:
        ```java
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ```
    2.  Initializes a secure HTTP Session:
        ```java
        HttpSession session = req.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        ```
    3.  The browser receives a unique session ID cookie in the response headers:
        ```http
        Set-Cookie: JSESSIONID=ABCD1234EFGH; Path=/; HttpOnly; Secure
        ```
    4.  Returns the authenticated `UserDTO` to the frontend, updating the application's global authentication state to logged-in.

---

## 3. Subscription Checkout Flow (Step-by-Step Code-Path)

Tracks how a user sets up billing parameters and completes a subscription purchase.

```
[React Steps Page] ────> [customerService.ts] ────> [SubscriptionController] ────> [SubscriptionFlowServiceImpl] ────> [DB Commit]
```

### Step 1: Front-End Wizard Dispatch
*   **Source File:** `SubscriptionCheckoutPage.tsx`
*   **Syntactical Execution:**
    1.  The user completes the checkout fields:
        *   *Billing Address:* Saved in `AddressRequest` format.
        *   *Payment Method:* Tokenized credit card details.
        *   *Coupons:* Discount coupon string (e.g., `"SAVE10"`).
        *   *Addons:* Linked supplemental items list (e.g., `["ADDON_UHD"]`).
    2.  The customer service client dispatches a request to register the subscription details:
        ```typescript
        const res = await fetch('/api/customer/subscription/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ planId, paymentMethod, couponCode, addons })
        });
        ```

### Step 2: Entry into Transactional Checkout Service
*   **Source File:** `SubscriptionFlowServiceImpl.java`
*   **Syntactical Execution:**
    1.  The method starts inside a transactional context:
        ```java
        @Transactional
        public CheckoutResponse completeSubscriptionCheckout(String email, CheckoutRequest request)
        ```
        *If any runtime exception is thrown during execution, all changes are automatically rolled back.*
    2.  Loads the Customer profile and target subscription Plan:
        ```java
        Customer customer = customerRepository.findByUser_Email(email)
                .orElseThrow(() -> CustomException.notFound("Customer not found"));
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> CustomException.notFound("Plan not found"));
        ```

### Step 3: Payment Method Tokenization & Saving
*   **Source File:** `SubscriptionFlowServiceImpl.java`
*   **Syntactical Execution:**
    1.  Validates and saves the customer's card credentials:
        ```java
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .customer(customer)
                .type("CARD")
                .provider(request.getCardProvider())
                .lastFour(request.getLastFour())
                .isDefault(true)
                .build();
        paymentMethodRepository.save(paymentMethod);
        ```

### Step 4: Core Subscription Registration
*   **Source File:** `SubscriptionFlowServiceImpl.java`
*   **Syntactical Execution:**
    1.  Configures subscription start and end dates. If the plan includes trial days, the subscription status is set to `TRIALING`:
        ```java
        LocalDate today = LocalDate.now();
        LocalDate trialEnd = plan.getTrialPeriodDays() > 0 ? today.plusDays(plan.getTrialPeriodDays()) : null;
        Status subStatus = trialEnd != null ? Status.TRIALING : Status.ACTIVE;

        Subscription sub = Subscription.builder()
                .customer(customer)
                .plan(plan)
                .status(subStatus)
                .currentPeriodStart(today)
                .currentPeriodEnd(plan.getBillingPeriod() == BillingPeriod.YEARLY ? today.plusYears(1) : today.plusMonths(1))
                .trialStartDate(trialEnd != null ? today : null)
                .trialEndDate(trialEnd)
                .build();
        Subscription savedSub = subscriptionRepository.save(sub);
        ```

### Step 5: Item Line Configuration & Invoice Assembly
*   **Source File:** `SubscriptionFlowServiceImpl.java`
*   **Syntactical Execution:**
    1.  Adds the core Plan to the subscription items:
        ```java
        SubscriptionItem planItem = SubscriptionItem.builder()
                .subscription(savedSub)
                .itemType(ItemType.PLAN)
                .itemId(plan.getId())
                .priceMinor(plan.getPriceMinor())
                .build();
        subscriptionItemRepository.save(planItem);
        ```
    2.  Resolves and attaches active recurring AddOns:
        ```java
        for (Long addonId : request.getAddonIds()) {
            AddOn addon = addOnRepository.findById(addonId).orElseThrow();
            SubscriptionItem addonItem = SubscriptionItem.builder()
                    .subscription(savedSub)
                    .itemType(ItemType.ADDON)
                    .itemId(addon.getId())
                    .priceMinor(addon.getPriceMinor())
                    .build();
            subscriptionItemRepository.save(addonItem);
        }
        ```

### Step 6: Pricing and Tax Computations
*   **Source File:** `SubscriptionFlowServiceImpl.java`
*   **Syntactical Execution:**
    1.  Computes subtotals, applies coupon discounts, and calculates 18% GST:
        ```java
        long subtotal = plan.getPriceMinor() + addonsTotal;
        long discount = coupon != null ? calculateDiscount(subtotal, coupon) : 0;
        long taxableAmount = Math.max(0, subtotal - discount);
        long tax = Math.round(taxableAmount * 0.18);
        long grandTotal = taxableAmount + tax;
        ```
    2.  Generates the Invoice:
        ```java
        Invoice invoice = Invoice.builder()
                .subscription(savedSub)
                .amountMinor(taxableAmount)
                .discountMinor(discount)
                .taxMinor(tax)
                .totalMinor(grandTotal)
                .status(subStatus == Status.TRIALING ? Status.OPEN : Status.PAID)
                .dueDate(subStatus == Status.TRIALING ? trialEnd : today)
                .build();
        invoiceRepository.save(invoice);
        ```
    3.  If the subscription status is `ACTIVE` (not trialing), a payment log is saved to track the transaction:
        ```java
        Payment payment = Payment.builder()
                .customer(customer)
                .invoice(savedInvoice)
                .amountMinor(grandTotal)
                .status(Status.SUCCESS)
                .gatewayReference("GW_PAY_" + UUID.randomUUID().toString())
                .build();
        paymentRepository.save(payment);
        ```

---

## 4. Subscription Upgrade & Active Proration Flow (Step-by-Step Code-Path)

Tracks how subscription updates are applied and how the backend calculates plan updates and charges.

```
[Subscription Grid] ────> [customerService.ts] ────> [CustomerSubServiceImpl] ────> [Proration Math] ────> [Invoiced & Reset]
```

### Step 1: Upgrade Call Interception
*   **Source File:** `CustomerSubscriptionServiceImpl.java`
*   **Syntactical Execution:**
    1.  The customer upgrades their plan (e.g. from Monthly to Yearly).
    2.  The method retrieves the customer's subscription and active plan details:
        ```java
        Subscription subscription = subscriptionRepository.findByCustomer_User_Email(email)
                .orElseThrow(() -> CustomException.notFound("Subscription not found"));
        Plan newPlan = planRepository.findById(request.getNewPlanId())
                .orElseThrow(() -> CustomException.notFound("Plan not found"));
        Plan oldPlan = subscription.getPlan();
        ```

### Step 2: Branching Strategy Checks
*   **Source File:** `CustomerSubscriptionServiceImpl.java`
*   **Syntactical Execution:**

#### BRANCH A: If the Current Subscription is in `TRIALING` Status
1.  **Calculate Trial Days Used:**
    ```java
    long daysUsed = ChronoUnit.DAYS.between(subscription.getTrialStartDate(), LocalDate.now());
    ```
2.  **Calculate Adjusted Remaining Trial Days:**
    ```java
    long newTrialDays = Math.max(0, newPlan.getTrialPeriodDays() - daysUsed);
    ```
3.  **Adjust Subscription Parameters:**
    ```java
    subscription.setPlan(newPlan);
    subscription.setTrialEndDate(LocalDate.now().plusDays(newTrialDays));
    if (newTrialDays == 0) {
        subscription.setStatus(Status.ACTIVE);
    }
    ```
4.  **Update the Outstanding Open Trial Invoice:**
    - Deletes previous invoice line items:
      ```java
      invoiceLineItemRepository.deleteByInvoice_Id(openTrialInvoice.getId());
      ```
    - Adds the updated subscription plan line item to the invoice.
    - Applies coupons, adds active addons, and calculates the 18% GST tax.
    - Updates and saves the open invoice with the updated subtotal and due date.

#### BRANCH B: If the Current Subscription is in `ACTIVE` Status (Proration Math)
1.  **Calculate Remaining Days in the Current Cycle:**
    ```java
    LocalDate today = LocalDate.now();
    long totalDaysInPeriod = ChronoUnit.DAYS.between(subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd());
    long remainingDays = ChronoUnit.DAYS.between(today, subscription.getCurrentPeriodEnd());
    ```
2.  **Calculate Unused Credit Refund from the Previous Plan:**
    ```java
    long oldPaidTotal = oldInvoice.getTotalMinor(); // Plan price + addons + tax paid
    long creditRefund = Math.round(oldPaidTotal * ((double) remainingDays / totalDaysInPeriod));
    ```
3.  **Calculate New Plan Cost:**
    ```java
    long newPlanPrice = newPlan.getPriceMinor();
    long newTax = Math.round(newPlanPrice * 0.18);
    long newTotalCost = newPlanPrice + newTax;
    ```
4.  **Calculate Proration Fee:**
    ```java
    long immediateCharge = Math.max(0, newTotalCost - creditRefund);
    ```
5.  **Create and Pay the Proration Invoice:**
    - Generates a new invoice marked as `PAID`.
    - Automatically executes the transaction charge for the proration fee amount.
    - Saves the transaction details in `paymentRepository`.
6.  **Reset Subscription Dates starting today:**
    ```java
    subscription.setPlan(newPlan);
    subscription.setCurrentPeriodStart(today);
    subscription.setCurrentPeriodEnd(newPlan.getBillingPeriod() == BillingPeriod.YEARLY ? today.plusYears(1) : today.plusMonths(1));
    subscriptionRepository.save(subscription);
    ```
