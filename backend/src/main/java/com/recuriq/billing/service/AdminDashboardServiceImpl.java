package com.recuriq.billing.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recuriq.billing.dto.admin.AddOnResponse;
import com.recuriq.billing.dto.admin.CustomerResponse;
import com.recuriq.billing.dto.admin.MeteredComponentResponse;
import com.recuriq.billing.dto.admin.PlanResponse;
import com.recuriq.billing.dto.admin.PriceBookResponse;
import com.recuriq.billing.dto.admin.ProductResponse;
import com.recuriq.billing.dto.admin.StaffResponse;
import com.recuriq.billing.dto.admin.SubscriptionResponse;
import com.recuriq.billing.entity.AddOn;
import com.recuriq.billing.entity.Coupon;
import com.recuriq.billing.entity.MeteredComponent;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.entity.PriceBookEntry;
import com.recuriq.billing.entity.Product;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.TaxRate;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.AddOnRepository;
import com.recuriq.billing.repository.CouponRepository;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.MeteredComponentRepository;
import com.recuriq.billing.repository.PlanRepository;
import com.recuriq.billing.repository.PriceBookEntryRepository;
import com.recuriq.billing.repository.ProductRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import com.recuriq.billing.repository.TaxRateRepository;
import com.recuriq.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private static final String PRODUCT_NOT_FOUND_MSG = "Product not found with id: ";
    private static final String ENTITY_PRODUCT = "Product";
    private static final String ENTITY_ADD_ON = "AddOn";
    private static final String ENTITY_METERED_COMPONENT = "MeteredComponent";
    private static final String ENTITY_COUPON = "Coupon";
    private final ProductRepository productRepository;
    private final PlanRepository planRepository;
    private final PriceBookEntryRepository priceBookEntryRepository;
    private final AddOnRepository addOnRepository;
    private final MeteredComponentRepository meteredComponentRepository;
    private final TaxRateRepository taxRateRepository;
    private final CouponRepository couponRepository;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLoggingService auditLoggingService;

    // ==================== DASHBOARD ====================
    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        // Catalog Management Stats
        stats.put("totalProducts", productRepository.count());
        stats.put("totalPlans", planRepository.count());
        stats.put("activeCoupons", couponRepository.countByStatus(Status.ACTIVE));
        stats.put("totalAddOns", addOnRepository.count());
        stats.put("activeTaxRates", taxRateRepository.countActiveTaxRates());
        stats.put("totalPriceBooks", priceBookEntryRepository.count());
        stats.put("totalCustomers", customerRepository.count());
        stats.put("totalStaff",
                userRepository.findAll().stream().filter(u -> u.getRole() != UserRole.CUSTOMER).count());
        return stats;
    }

    // ==================== PRODUCT ====================
    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> ProductResponse.from(p, planRepository.countByProductId(p.getId())))
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound(PRODUCT_NOT_FOUND_MSG + id));
        return ProductResponse.from(p, planRepository.countByProductId(p.getId()));
    }

    @Override
    public ProductResponse createProduct(Product product) {
        Product saved = productRepository.save(product);
        auditLoggingService.logAction("CREATE_PRODUCT", ENTITY_PRODUCT, saved.getId(), null, saved);
        return ProductResponse.from(saved, 0);
    }

    @Override
    public ProductResponse updateProduct(Long id, Product updated) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound(PRODUCT_NOT_FOUND_MSG + id));
        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        Product saved = productRepository.save(product);
        auditLoggingService.logAction("UPDATE_PRODUCT", ENTITY_PRODUCT, saved.getId(), null, saved);
        return ProductResponse.from(saved, planRepository.countByProductId(saved.getId()));
    }

    @Override
    public void toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound(PRODUCT_NOT_FOUND_MSG + id));
        product.setStatus(product.getStatus().equals(Status.ACTIVE) ? Status.INACTIVE : Status.ACTIVE);
        productRepository.save(product);
        auditLoggingService.logAction("TOGGLE_PRODUCT_STATUS", ENTITY_PRODUCT, product.getId(), null, product);
    }

    // ==================== PLAN ====================
    @Override
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAll().stream().map(PlanResponse::from).toList();
    }

    @Override
    public PlanResponse createPlan(Plan plan) {
        Plan saved = planRepository.save(plan);
        auditLoggingService.logAction("CREATE_PLAN", "Plan", saved.getId(), null, saved);
        return PlanResponse.from(saved);
    }

    @Override
    public PlanResponse updatePlan(Long id, Plan updated) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Plan not found"));
        plan.setName(updated.getName());
        plan.setBillingPeriod(updated.getBillingPeriod());
        plan.setDefaultPriceMinor(updated.getDefaultPriceMinor());
        plan.setDefaultCurrency(updated.getDefaultCurrency());
        plan.setTrialDays(updated.getTrialDays());
        plan.setSetupFeeMinor(updated.getSetupFeeMinor());
        plan.setTaxMode(updated.getTaxMode());
        plan.setEffectiveFrom(updated.getEffectiveFrom());
        plan.setEffectiveTo(updated.getEffectiveTo());
        Plan saved = planRepository.save(plan);
        auditLoggingService.logAction("UPDATE_PLAN", "Plan", saved.getId(), null, saved);
        return PlanResponse.from(saved);
    }

    @Override
    public void deletePlan(Long id) {
        planRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Plan not found"));
        long activeSubs = subscriptionRepository.countByPlan_IdAndStatusIn(
                id,
                List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED));
        if (activeSubs > 0) {
            throw CustomException.conflict(
                    "Cannot delete: " + activeSubs
                            + " active subscription(s) use this plan. Make it inactive instead if needed.");
        }
        List<PriceBookEntry> entries = priceBookEntryRepository.findByPlan_Id(id);
        priceBookEntryRepository.deleteAll(entries);
        planRepository.deleteById(id);
        auditLoggingService.logAction("DELETE_PLAN", "Plan", id, null, null);
    }

    // ==================== PRICE BOOK ====================
    @Override
    public List<PriceBookResponse> getAllPriceBookEntries() {
        return priceBookEntryRepository.findAll().stream().map(PriceBookResponse::from).toList();
    }

    @Override
    public PriceBookResponse createPriceBookEntry(PriceBookEntry entry) {
        PriceBookEntry saved = priceBookEntryRepository.save(entry);
        auditLoggingService.logAction("CREATE_PRICE_BOOK", "PriceBookEntry", saved.getId(), null, saved);
        return PriceBookResponse.from(saved);
    }

    @Override
    public PriceBookResponse updatePriceBookEntry(Long id, PriceBookEntry updated) {
        PriceBookEntry entry = priceBookEntryRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Price book entry not found"));
        entry.setRegion(updated.getRegion());
        entry.setCurrency(updated.getCurrency());
        entry.setPriceMinor(updated.getPriceMinor());
        entry.setEffectiveFrom(updated.getEffectiveFrom());
        entry.setEffectiveTo(updated.getEffectiveTo());
        PriceBookEntry saved = priceBookEntryRepository.save(entry);
        auditLoggingService.logAction("UPDATE_PRICE_BOOK", "PriceBookEntry", saved.getId(), null, saved);
        return PriceBookResponse.from(saved);
    }

    @Override
    public void archivePriceBookEntry(Long id) {
        PriceBookEntry entry = priceBookEntryRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Price book entry not found"));
        long activeSubs = subscriptionRepository.countByPlan_IdAndStatusIn(
                entry.getPlan().getId(),
                List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED));
        if (activeSubs > 0) {
            throw CustomException.conflict(
                    "Cannot delete: " + activeSubs
                            + " active subscription(s) use this plan. Archive the plan instead.");
        }
        priceBookEntryRepository.deleteById(id);
    }

    // ==================== ADD-ON ====================
    @Override
    public List<AddOnResponse> getAllAddOns() {
        return addOnRepository.findAll().stream().map(AddOnResponse::from).toList();
    }

    @Override
    public AddOnResponse createAddOn(AddOn addOn) {
        AddOn saved = addOnRepository.save(addOn);
        auditLoggingService.logAction("CREATE_ADDON", ENTITY_ADD_ON, saved.getId(), null, saved);
        return AddOnResponse.from(saved);
    }

    @Override
    public AddOnResponse updateAddOn(Long id, AddOn updated) {
        AddOn addOn = addOnRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Add-on not found"));
        addOn.setName(updated.getName());
        addOn.setPriceMinor(updated.getPriceMinor());
        addOn.setCurrency(updated.getCurrency());
        addOn.setBillingPeriod(updated.getBillingPeriod());
        addOn.setTaxMode(updated.getTaxMode());
        AddOn saved = addOnRepository.save(addOn);
        auditLoggingService.logAction("UPDATE_ADDON", ENTITY_ADD_ON, saved.getId(), null, saved);
        return AddOnResponse.from(saved);
    }

    @Override
    public void toggleAddOnStatus(Long id) {
        AddOn addOn = addOnRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Add-on not found"));
        addOn.setStatus(addOn.getStatus().equals(Status.ACTIVE) ? Status.INACTIVE : Status.ACTIVE);
        addOnRepository.save(addOn);
        auditLoggingService.logAction("TOGGLE_ADDON_STATUS", ENTITY_ADD_ON, addOn.getId(), null, addOn);
    }

    // ==================== METERED COMPONENT ====================
    @Override
    public List<MeteredComponentResponse> getAllMeteredComponents() {
        return meteredComponentRepository.findAll().stream().map(MeteredComponentResponse::from).toList();
    }

    @Override
    public MeteredComponentResponse createMeteredComponent(MeteredComponent component) {
        MeteredComponent saved = meteredComponentRepository.save(component);
        auditLoggingService.logAction("CREATE_METERED_COMPONENT", ENTITY_METERED_COMPONENT, saved.getId(), null, saved);
        return MeteredComponentResponse.from(saved);
    }

    @Override
    public MeteredComponentResponse updateMeteredComponent(Long id, MeteredComponent updated) {
        MeteredComponent component = meteredComponentRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Metered component not found"));
        component.setName(updated.getName());
        component.setUnitName(updated.getUnitName());
        component.setPricePerUnitMinor(updated.getPricePerUnitMinor());
        component.setFreeTierQuantity(updated.getFreeTierQuantity());
        MeteredComponent saved = meteredComponentRepository.save(component);
        auditLoggingService.logAction("UPDATE_METERED_COMPONENT", ENTITY_METERED_COMPONENT, saved.getId(), null, saved);
        return MeteredComponentResponse.from(saved);
    }

    @Override
    public void toggleMeteredComponentStatus(Long id) {
        MeteredComponent component = meteredComponentRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Metered component not found"));
        component.setStatus(component.getStatus().equals(Status.ACTIVE) ? Status.INACTIVE : Status.ACTIVE);
        meteredComponentRepository.save(component);
        auditLoggingService.logAction("TOGGLE_METERED_COMPONENT_STATUS", ENTITY_METERED_COMPONENT, component.getId(),
                null, component);
    }

    // ==================== TAX RATE ====================
    @Override
    public List<TaxRate> getAllTaxRates() {
        return taxRateRepository.findAll();
    }

    @Override
    public TaxRate createTaxRate(TaxRate taxRate) {
        TaxRate saved = taxRateRepository.save(taxRate);
        auditLoggingService.logAction("CREATE_TAX_RATE", "TaxRate", saved.getId(), null, saved);
        return saved;
    }

    @Override
    public TaxRate updateTaxRate(Long id, TaxRate updated) {
        TaxRate taxRate = taxRateRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Tax rate not found"));
        taxRate.setName(updated.getName());
        taxRate.setRegion(updated.getRegion());
        taxRate.setRatePercent(updated.getRatePercent());
        taxRate.setInclusive(updated.getInclusive());
        taxRate.setEffectiveFrom(updated.getEffectiveFrom());
        taxRate.setEffectiveTo(updated.getEffectiveTo());
        TaxRate saved = taxRateRepository.save(taxRate);
        auditLoggingService.logAction("UPDATE_TAX_RATE", "TaxRate", saved.getId(), null, saved);
        return saved;
    }

    @Override
    public void deleteTaxRate(Long id) {
        taxRateRepository.deleteById(id);
    }

    // ==================== COUPON ====================
    @Override
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.existsByCode(coupon.getCode())) {
            throw CustomException.conflict("Coupon code already exists: " + coupon.getCode());
        }
        Coupon saved = couponRepository.save(coupon);
        auditLoggingService.logAction("CREATE_COUPON", ENTITY_COUPON, saved.getId(), null, saved);
        return saved;
    }

    @Override
    public Coupon updateCoupon(Long id, Coupon updated) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Coupon not found"));
        coupon.setName(updated.getName());
        coupon.setType(updated.getType());
        coupon.setAmount(updated.getAmount());
        coupon.setCurrency(updated.getCurrency());
        coupon.setDuration(updated.getDuration());
        coupon.setDurationInMonths(updated.getDurationInMonths());
        coupon.setMaxRedemptions(updated.getMaxRedemptions());
        coupon.setValidFrom(updated.getValidFrom());
        coupon.setValidTo(updated.getValidTo());
        Coupon saved = couponRepository.save(coupon);
        auditLoggingService.logAction("UPDATE_COUPON", ENTITY_COUPON, saved.getId(), null, saved);
        return saved;
    }

    @Override
    public void toggleCouponStatus(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Coupon not found"));
        coupon.setStatus(coupon.getStatus().equals(Status.ACTIVE) ? Status.DISABLED : Status.ACTIVE);
        couponRepository.save(coupon);
        auditLoggingService.logAction("TOGGLE_COUPON_STATUS", ENTITY_COUPON, coupon.getId(), null, coupon);
    }

    // ==================== CUSTOMERS ====================
    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(CustomerResponse::from).toList();
    }

    // ==================== STAFF ====================
    @Override
    public List<StaffResponse> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != UserRole.CUSTOMER)
                .map(StaffResponse::from)
                .toList();
    }

    @Override
    public StaffResponse createStaff(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw CustomException.conflict("Email already exists: " + user.getEmail());
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setStatus(Status.ACTIVE);
        return StaffResponse.from(userRepository.save(user));
    }

    @Override
    public void deleteStaff(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Staff not found"));
        // Prevent self-deletion
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentUserEmail)) {
            throw CustomException.forbidden("You cannot delete your own account.");
        }
        if (user.getRole() == UserRole.CUSTOMER) {
            throw CustomException.forbidden("Cannot delete a customer from staff management.");
        }
        userRepository.deleteById(id);
    }

    // ==================== SUBSCRIPTIONS ====================
    @Override
    public List<SubscriptionResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream().map(SubscriptionResponse::from).toList();
    }

    @Override
    public SubscriptionResponse getSubscriptionById(Long id) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("Subscription not found"));
        return SubscriptionResponse.from(sub);
    }
}