package com.recuriq.billing.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.MeteredComponent;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.entity.PriceBookEntry;
import com.recuriq.billing.entity.Product;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.TaxRate;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.CouponType;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.TaxMode;
import com.recuriq.billing.enums.UserRole;
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
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private PriceBookEntryRepository priceBookEntryRepository;
    @Mock
    private AddOnRepository addOnRepository;
    @Mock
    private MeteredComponentRepository meteredComponentRepository;
    @Mock
    private TaxRateRepository taxRateRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLoggingService auditLoggingService;
    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;
    private Product product;
    private Plan plan;
    private AddOn addOn;
    private MeteredComponent meteredComponent;
    private TaxRate taxRate;
    private Coupon coupon;
    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Netflix");
        product.setDescription("Streaming");
        product.setStatus(Status.ACTIVE);
        plan = new Plan();
        plan.setId(1L);
        plan.setName("Premium");
        plan.setProduct(product);
        plan.setStatus(Status.ACTIVE);
        addOn = AddOn.builder()
                .id(1L)
                .name("4K")
                .priceMinor(500L)
                .currency("INR")
                .billingPeriod(BillingPeriod.MONTHLY)
                .taxMode(TaxMode.EXCLUSIVE)
                .status(Status.ACTIVE)
                .build();
        meteredComponent = new MeteredComponent();
        meteredComponent.setId(1L);
        meteredComponent.setName("API");
        meteredComponent.setUnitName("calls");
        meteredComponent.setPricePerUnitMinor(10L);
        meteredComponent.setFreeTierQuantity(100L);
        meteredComponent.setStatus(Status.ACTIVE);
        meteredComponent.setPlan(plan);
        taxRate = new TaxRate();
        taxRate.setId(1L);
        taxRate.setName("GST");
        taxRate.setRegion("IN");
        taxRate.setRatePercent(new BigDecimal("18"));
        coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode("SAVE20");
        coupon.setName("Save");
        coupon.setType(CouponType.PERCENT);
        coupon.setAmount(20L);
        coupon.setStatus(Status.ACTIVE);
    }
    // ================= DASHBOARD =================
    @Test
    void testGetDashboardStats() {
        when(productRepository.count()).thenReturn(5L);
        when(planRepository.count()).thenReturn(10L);
        when(couponRepository.countByStatus(Status.ACTIVE)).thenReturn(3L);
        when(addOnRepository.count()).thenReturn(4L);
        when(taxRateRepository.countActiveTaxRates()).thenReturn(2L);
        when(priceBookEntryRepository.count()).thenReturn(8L);
        when(customerRepository.count()).thenReturn(6L);
        User admin = User.builder()
                .id(1L)
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findAll()).thenReturn(List.of(admin));
        Map<String, Object> stats =
                adminDashboardService.getDashboardStats();
        assertEquals(5L, stats.get("totalProducts"));
        assertEquals(10L, stats.get("totalPlans"));
        assertEquals(3L, stats.get("activeCoupons"));
        assertEquals(4L, stats.get("totalAddOns"));
        assertEquals(2L, stats.get("activeTaxRates"));
        assertEquals(8L, stats.get("totalPriceBooks"));
        assertEquals(6L, stats.get("totalCustomers"));
        assertEquals(1L, stats.get("totalStaff"));
    }
    // ================= PRODUCT =================
    @Test
    void testGetAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(planRepository.countByProductId(1L)).thenReturn(2L);
        List<ProductResponse> result =
                adminDashboardService.getAllProducts();
        assertEquals(1, result.size());
    }
    @Test
    void testGetProductById() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(planRepository.countByProductId(1L))
                .thenReturn(2L);
        ProductResponse result =
                adminDashboardService.getProductById(1L);
        assertNotNull(result);
    }
    @Test
    void testGetProductById_NotFound() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.getProductById(1L));
    }
    @Test
    void testCreateProduct() {
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);
        ProductResponse result =
                adminDashboardService.createProduct(product);
        assertNotNull(result);
        verify(auditLoggingService)
                .logAction(anyString(), anyString(),
                        anyLong(), any(), any());
    }
    @Test
    void testUpdateProduct() {
        Product updated = new Product();
        updated.setName("Updated");
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);
        ProductResponse result =
                adminDashboardService.updateProduct(1L, updated);
        assertNotNull(result);
    }
    @Test
    void testToggleProductStatus() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        adminDashboardService.toggleProductStatus(1L);
        assertEquals(Status.INACTIVE, product.getStatus());
        verify(productRepository).save(product);
    }
    // ================= PLAN =================
    @Test
    void testGetAllPlans() {
        when(planRepository.findAll()).thenReturn(List.of(plan));
        List<PlanResponse> result =
                adminDashboardService.getAllPlans();
        assertEquals(1, result.size());
    }
    @Test
    void testCreatePlan() {
        when(planRepository.save(any(Plan.class)))
                .thenReturn(plan);
        PlanResponse result =
                adminDashboardService.createPlan(plan);
        assertNotNull(result);
    }
    @Test
    void testUpdatePlan() {
        Plan updated = new Plan();
        updated.setName("Updated");
        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class)))
                .thenReturn(plan);
        PlanResponse result =
                adminDashboardService.updatePlan(1L, updated);
        assertNotNull(result);
    }
    @Test
    void testDeletePlan_Success() {
        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan));
        when(subscriptionRepository.countByPlan_IdAndStatusIn(
                anyLong(), any()))
                .thenReturn(0L);
        when(priceBookEntryRepository.findByPlan_Id(1L))
                .thenReturn(List.of(new PriceBookEntry()));
        adminDashboardService.deletePlan(1L);
        verify(planRepository).deleteById(1L);
    }
    @Test
    void testDeletePlan_Conflict() {
        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan));
        when(subscriptionRepository.countByPlan_IdAndStatusIn(
                anyLong(), any()))
                .thenReturn(5L);
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.deletePlan(1L));
    }
    // ================= PRICE BOOK =================
    @Test
    void testGetAllPriceBookEntries() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setId(1L);
        entry.setPlan(plan);
        when(priceBookEntryRepository.findAll())
                .thenReturn(List.of(entry));
        List<PriceBookResponse> result =
                adminDashboardService.getAllPriceBookEntries();
        assertEquals(1, result.size());
    }
    @Test
    void testCreatePriceBookEntry() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setId(1L);
        entry.setPlan(plan);
        when(priceBookEntryRepository.save(any()))
                .thenReturn(entry);
        PriceBookResponse result =
                adminDashboardService.createPriceBookEntry(entry);
        assertNotNull(result);
    }
    @Test
    void testArchivePriceBookEntry() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setPlan(plan);
        when(priceBookEntryRepository.findById(1L))
                .thenReturn(Optional.of(entry));
        when(subscriptionRepository.countByPlan_IdAndStatusIn(
                anyLong(), any()))
                .thenReturn(0L);
        adminDashboardService.archivePriceBookEntry(1L);
        verify(priceBookEntryRepository).deleteById(1L);
    }
    // ================= ADDON =================
    @Test
    void testGetAllAddOns() {
        when(addOnRepository.findAll())
                .thenReturn(List.of(addOn));
        List<AddOnResponse> result =
                adminDashboardService.getAllAddOns();
        assertEquals(1, result.size());
    }
    @Test
    void testCreateAddOn() {
        when(addOnRepository.save(any(AddOn.class)))
                .thenReturn(addOn);
        AddOnResponse result =
                adminDashboardService.createAddOn(addOn);
        assertNotNull(result);
    }
    @Test
    void testToggleAddOnStatus() {
        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));
        adminDashboardService.toggleAddOnStatus(1L);
        assertEquals(Status.INACTIVE, addOn.getStatus());
    }
    // ================= METERED =================
    @Test
    void testGetAllMeteredComponents() {
        when(meteredComponentRepository.findAll())
                .thenReturn(List.of(meteredComponent));
        List<MeteredComponentResponse> result =
                adminDashboardService.getAllMeteredComponents();
        assertEquals(1, result.size());
    }
    @Test
    void testCreateMeteredComponent() {
        when(meteredComponentRepository.save(any()))
                .thenReturn(meteredComponent);
        MeteredComponentResponse result =
                adminDashboardService.createMeteredComponent(
                        meteredComponent);
        assertNotNull(result);
    }
    @Test
    void testToggleMeteredComponentStatus() {
        when(meteredComponentRepository.findById(1L))
                .thenReturn(Optional.of(meteredComponent));
        adminDashboardService.toggleMeteredComponentStatus(1L);
        assertEquals(Status.INACTIVE,
                meteredComponent.getStatus());
    }
    // ================= TAX =================
    @Test
    void testGetAllTaxRates() {
        when(taxRateRepository.findAll())
                .thenReturn(List.of(taxRate));
        List<TaxRate> result =
                adminDashboardService.getAllTaxRates();
        assertEquals(1, result.size());
    }
    @Test
    void testCreateTaxRate() {
        when(taxRateRepository.save(any()))
                .thenReturn(taxRate);
        TaxRate result =
                adminDashboardService.createTaxRate(taxRate);
        assertNotNull(result);
    }
    @Test
    void testDeleteTaxRate() {
        adminDashboardService.deleteTaxRate(1L);
        verify(taxRateRepository).deleteById(1L);
    }
    // ================= COUPON =================
    @Test
    void testGetAllCoupons() {
        when(couponRepository.findAll())
                .thenReturn(List.of(coupon));
        List<Coupon> result =
                adminDashboardService.getAllCoupons();
        assertEquals(1, result.size());
    }
    @Test
    void testCreateCoupon() {
        when(couponRepository.existsByCode("SAVE20"))
                .thenReturn(false);
        when(couponRepository.save(any()))
                .thenReturn(coupon);
        Coupon result =
                adminDashboardService.createCoupon(coupon);
        assertNotNull(result);
    }
    @Test
    void testCreateCoupon_Conflict() {
        when(couponRepository.existsByCode("SAVE20"))
                .thenReturn(true);
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.createCoupon(coupon));
    }
    @Test
    void testToggleCouponStatus() {
        when(couponRepository.findById(1L))
                .thenReturn(Optional.of(coupon));
        adminDashboardService.toggleCouponStatus(1L);
        assertEquals(Status.DISABLED,
                coupon.getStatus());
    }
    // ================= CUSTOMER =================
    @Test
    void testGetAllCustomers() {
        Customer customer = new Customer();
       User user=new User();
       user.setId(1L);
       user.setFullName("Test User");
       customer.setUser(user);
        when(customerRepository.findAll())
                .thenReturn(List.of(customer));
        List<CustomerResponse> result =
                adminDashboardService.getAllCustomers();
        assertEquals(1, result.size());
    }
    // ================= STAFF =================
    @Test
    void testGetAllStaff() {
        User admin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();
        User customer = User.builder()
                .id(2L)
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .build();
        when(userRepository.findAll())
                .thenReturn(Arrays.asList(admin, customer));
        List<StaffResponse> result =
                adminDashboardService.getAllStaff();
        assertEquals(1, result.size());
    }
    @Test
    void testCreateStaff() {
        User user = User.builder()
                .email("admin@test.com")
                .passwordHash("raw")
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.existsByEmail("admin@test.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("raw"))
                .thenReturn("encoded");
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        StaffResponse result =
                adminDashboardService.createStaff(user);
        assertNotNull(result);
        verify(passwordEncoder).encode("raw");
    }
    @Test
    void testDeleteStaff_Success() {
        User user = User.builder()
                .id(1L)
                .email("staff@test.com")
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        Authentication authentication =
                mock(Authentication.class);
        SecurityContext securityContext =
                mock(SecurityContext.class);
        when(authentication.getName())
                .thenReturn("other@test.com");
        when(securityContext.getAuthentication())
                .thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        adminDashboardService.deleteStaff(1L);
        verify(userRepository).deleteById(1L);
    }
    @Test
    void testDeleteStaff_SelfDeleteForbidden() {
        User user = User.builder()
                .id(1L)
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        Authentication authentication =
                mock(Authentication.class);
        SecurityContext securityContext =
                mock(SecurityContext.class);
        when(authentication.getName())
                .thenReturn("admin@test.com");
        when(securityContext.getAuthentication())
                .thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.deleteStaff(1L));
    }
    // ================= SUBSCRIPTION =================
    @Test
    void testGetAllSubscriptions() {
    	User user=new User();
    	user.setId(1L);
    	user.setFullName("Test User");
    	Customer customer=new Customer();
    	customer.setId(1L);
    	customer.setUser(user);
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setCustomer(customer);
        subscription.setPlan(plan);
        when(subscriptionRepository.findAll())
                .thenReturn(List.of(subscription));
        List<SubscriptionResponse> result =
                adminDashboardService.getAllSubscriptions();
        assertEquals(1, result.size());
    }
    @Test
    void testGetSubscriptionById() {
    	User user=new User();
    	user.setId(1L);
    	user.setFullName("Test User");
    	Customer customer=new Customer();
    	customer.setId(1L);
    	customer.setUser(user);
    	
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setCustomer(customer);
        subscription.setPlan(plan);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));
        SubscriptionResponse result =
                adminDashboardService.getSubscriptionById(1L);
        assertNotNull(result);
    }
    @Test
    void testGetSubscriptionById_NotFound() {
        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.getSubscriptionById(1L));
    }
    @Test
    void testUpdateAddOn() {
        AddOn updated = AddOn.builder()
                .name("Updated")
                .priceMinor(1000L)
                .currency("INR")
                .billingPeriod(BillingPeriod.YEARLY)
                .taxMode(TaxMode.EXCLUSIVE)
                .build();
        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));
        when(addOnRepository.save(any()))
                .thenReturn(addOn);
        AddOnResponse result =
                adminDashboardService.updateAddOn(1L, updated);
        assertNotNull(result);
    }
    @Test
    void testToggleAddOnStatus_NotFound() {
        when(addOnRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.toggleAddOnStatus(1L));
    }
    @Test
    void testUpdateMeteredComponent() {
        MeteredComponent updated = new MeteredComponent();
        updated.setName("Bandwidth");
        updated.setUnitName("GB");
        updated.setPricePerUnitMinor(50L);
        updated.setFreeTierQuantity(10L);
        when(meteredComponentRepository.findById(1L))
                .thenReturn(Optional.of(meteredComponent));
        when(meteredComponentRepository.save(any()))
                .thenReturn(meteredComponent);
        MeteredComponentResponse result =
                adminDashboardService.updateMeteredComponent(1L, updated);
        assertNotNull(result);
    }
    @Test
    void testToggleMeteredComponentStatus_NotFound() {
        when(meteredComponentRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> adminDashboardService
                        .toggleMeteredComponentStatus(1L));
    }
    @Test
    void testUpdateTaxRate() {
        TaxRate updated = new TaxRate();
        updated.setName("VAT");
        updated.setRegion("US");
        updated.setRatePercent(new BigDecimal("10"));
        when(taxRateRepository.findById(1L))
                .thenReturn(Optional.of(taxRate));
        when(taxRateRepository.save(any()))
                .thenReturn(taxRate);
        TaxRate result =
                adminDashboardService.updateTaxRate(1L, updated);
        assertNotNull(result);
    }
    @Test
    void testUpdateCoupon() {
        Coupon updated = new Coupon();
        updated.setName("Updated");
        updated.setAmount(50L);
        updated.setType(CouponType.FIXED);
        when(couponRepository.findById(1L))
                .thenReturn(Optional.of(coupon));
        when(couponRepository.save(any()))
                .thenReturn(coupon);
        Coupon result =
                adminDashboardService.updateCoupon(1L, updated);
        assertNotNull(result);
    }
    @Test
    void testToggleCouponStatus_NotFound() {
        when(couponRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.toggleCouponStatus(1L));
    }
    @Test
    void testUpdatePriceBookEntry() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setId(1L);
        entry.setRegion("IN");
        entry.setPlan(plan);
        PriceBookEntry updated = new PriceBookEntry();
        updated.setRegion("US");
        when(priceBookEntryRepository.findById(1L))
                .thenReturn(Optional.of(entry));
        when(priceBookEntryRepository.save(any()))
                .thenReturn(entry);
        PriceBookResponse result =
                adminDashboardService.updatePriceBookEntry(1L, updated);
        assertNotNull(result);
    }
    @Test
    void testCreateStaff_Conflict() {
        User user = User.builder()
                .email("admin@test.com")
                .build();
        when(userRepository.existsByEmail("admin@test.com"))
                .thenReturn(true);
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.createStaff(user));
    }
    @Test
    void testDeleteStaff_CustomerForbidden() {
        User customer = User.builder()
                .id(1L)
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(customer));
        Authentication authentication =
                mock(Authentication.class);
        SecurityContext securityContext =
                mock(SecurityContext.class);
        when(authentication.getName())
                .thenReturn("admin@test.com");
        when(securityContext.getAuthentication())
                .thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.deleteStaff(1L));
    }
    @Test
    void testUpdateAddOn_NotFound() {
        when(addOnRepository.findById(1L))
                .thenReturn(Optional.empty());
        AddOn updated = new AddOn();
        assertThrows(RuntimeException.class,
                () -> adminDashboardService.updateAddOn(1L, updated));
    }
    @Test
    void testUpdateMeteredComponent_NotFound() {
        when(meteredComponentRepository.findById(1L))
                .thenReturn(Optional.empty());
        MeteredComponent updated = new MeteredComponent();
        assertThrows(RuntimeException.class,
                () -> adminDashboardService
                        .updateMeteredComponent(1L, updated));
    }
    @Test
    void testUpdateTaxRate_NotFound() {
        when(taxRateRepository.findById(1L))
                .thenReturn(Optional.empty());
        TaxRate updated = new TaxRate();
        assertThrows(RuntimeException.class,
                () -> adminDashboardService
                        .updateTaxRate(1L, updated));
    }
    @Test
    void testUpdateCoupon_NotFound() {
        when(couponRepository.findById(1L))
                .thenReturn(Optional.empty());
        Coupon updated = new Coupon();
        assertThrows(RuntimeException.class,
                () -> adminDashboardService
                        .updateCoupon(1L, updated));
    }
    @Test
    void testUpdatePriceBookEntry_NotFound() {
        when(priceBookEntryRepository.findById(1L))
                .thenReturn(Optional.empty());
        PriceBookEntry updated = new PriceBookEntry();
        assertThrows(RuntimeException.class,
                () -> adminDashboardService
                        .updatePriceBookEntry(1L, updated));
    }
    @Test
    void testArchivePriceBookEntry_Conflict() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setPlan(plan);
        when(priceBookEntryRepository.findById(1L))
                .thenReturn(Optional.of(entry));
        when(subscriptionRepository.countByPlan_IdAndStatusIn(
                anyLong(), any()))
                .thenReturn(5L);
        assertThrows(RuntimeException.class,
                () -> adminDashboardService
                        .archivePriceBookEntry(1L));
    }
    @Test
    void testArchivePriceBookEntry_NotFound() {
        when(priceBookEntryRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> adminDashboardService
                        .archivePriceBookEntry(1L));
    }
}