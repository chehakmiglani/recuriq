package com.recuriq.billing.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.config.SecurityConfig;
import com.recuriq.billing.dto.admin.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.service.AdminDashboardService;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@EnableMethodSecurity
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponse sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = ProductResponse.builder()
                .id(1L)
                .name("StreamFlix")
                .description("Premium streaming platform")
                .status(Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .plansCount(4)
                .build();
    }

    // ==================== DASHBOARD ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetDashboardStats_Success() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", 40);
        stats.put("activeSubscriptions", 25);
        stats.put("mrrMinor", 458700L);

        when(adminService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(40))
                .andExpect(jsonPath("$.activeSubscriptions").value(25));

        verify(adminService, times(1)).getDashboardStats();
    }

    @Test
    void testGetDashboardStats_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetDashboardStats_ForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isForbidden());
    }

    // ==================== PRODUCTS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllProducts_Success() throws Exception {
        when(adminService.getAllProducts()).thenReturn(List.of(sampleProduct));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("StreamFlix"))
                .andExpect(jsonPath("$[0].plansCount").value(4));

        verify(adminService, times(1)).getAllProducts();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProductById_Success() throws Exception {
        when(adminService.getProductById(1L)).thenReturn(sampleProduct);

        mockMvc.perform(get("/api/admin/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("StreamFlix"));

        verify(adminService, times(1)).getProductById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateProduct_Success() throws Exception {
        Product product = new Product();
        product.setName("New Product");
        product.setDescription("Description");

        when(adminService.createProduct(any(Product.class))).thenReturn(sampleProduct);

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("StreamFlix"));

        verify(adminService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateProduct_Success() throws Exception {
        Product product = new Product();
        product.setName("Updated Product");

        when(adminService.updateProduct(eq(1L), any(Product.class))).thenReturn(sampleProduct);

        mockMvc.perform(put("/api/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).updateProduct(eq(1L), any(Product.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testToggleProductStatus_Success() throws Exception {
        doNothing().when(adminService).toggleProductStatus(1L);

        mockMvc.perform(patch("/api/admin/products/1/toggle-status"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).toggleProductStatus(1L);
    }

    // ==================== PLANS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllPlans_Success() throws Exception {
        when(adminService.getAllPlans()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(adminService, times(1)).getAllPlans();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePlan_Success() throws Exception {
        Plan plan = new Plan();
        plan.setName("Basic Monthly");

        when(adminService.createPlan(any(Plan.class))).thenReturn(null);

        mockMvc.perform(post("/api/admin/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).createPlan(any(Plan.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdatePlan_Success() throws Exception {
        Plan plan = new Plan();
        plan.setName("Updated Plan");

        when(adminService.updatePlan(eq(1L), any(Plan.class))).thenReturn(null);

        mockMvc.perform(put("/api/admin/plans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).updatePlan(eq(1L), any(Plan.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeletePlan_Success() throws Exception {
        doNothing().when(adminService).deletePlan(1L);

        mockMvc.perform(delete("/api/admin/plans/1"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).deletePlan(1L);
    }



    // ==================== PRICE BOOK ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllPriceBooks_Success() throws Exception {
        when(adminService.getAllPriceBookEntries()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/pricebooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreatePriceBookEntry_Success() throws Exception {
        PriceBookEntry entry = new PriceBookEntry();

        when(adminService.createPriceBookEntry(any(PriceBookEntry.class))).thenReturn(null);

        mockMvc.perform(post("/api/admin/pricebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).createPriceBookEntry(any(PriceBookEntry.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdatePriceBookEntry_Success() throws Exception {
        PriceBookEntry entry = new PriceBookEntry();

        when(adminService.updatePriceBookEntry(eq(1L), any(PriceBookEntry.class))).thenReturn(null);

        mockMvc.perform(put("/api/admin/pricebooks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).updatePriceBookEntry(eq(1L), any(PriceBookEntry.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testArchivePriceBook_Success() throws Exception {
        doNothing().when(adminService).archivePriceBookEntry(1L);

        mockMvc.perform(put("/api/admin/pricebooks/1/archive"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).archivePriceBookEntry(1L);
    }

    // ==================== ADD-ONS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllAddOns_Success() throws Exception {
        when(adminService.getAllAddOns()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/addons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateAddOn_Success() throws Exception {
        AddOn addOn = new AddOn();
        addOn.setName("4K Streaming");

        when(adminService.createAddOn(any(AddOn.class))).thenReturn(null);

        mockMvc.perform(post("/api/admin/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addOn)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).createAddOn(any(AddOn.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateAddOn_Success() throws Exception {
        AddOn addOn = new AddOn();
        addOn.setName("Updated AddOn");

        when(adminService.updateAddOn(eq(1L), any(AddOn.class))).thenReturn(null);

        mockMvc.perform(put("/api/admin/addons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addOn)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).updateAddOn(eq(1L), any(AddOn.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testToggleAddOnStatus_Success() throws Exception {
        doNothing().when(adminService).toggleAddOnStatus(1L);

        mockMvc.perform(patch("/api/admin/addons/1/toggle-status"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).toggleAddOnStatus(1L);
    }

    // ==================== METERED COMPONENT ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllMeteredComponents_Success() throws Exception {
        when(adminService.getAllMeteredComponents()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/metered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateMeteredComponent_Success() throws Exception {
        MeteredComponent component = new MeteredComponent();
        component.setName("API Calls");

        when(adminService.createMeteredComponent(any(MeteredComponent.class))).thenReturn(null);

        mockMvc.perform(post("/api/admin/metered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(component)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).createMeteredComponent(any(MeteredComponent.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateMeteredComponent_Success() throws Exception {
        MeteredComponent component = new MeteredComponent();
        component.setName("Updated Component");

        when(adminService.updateMeteredComponent(eq(1L), any(MeteredComponent.class))).thenReturn(null);

        mockMvc.perform(put("/api/admin/metered/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(component)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).updateMeteredComponent(eq(1L), any(MeteredComponent.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testToggleMeteredComponentStatus_Success() throws Exception {
        doNothing().when(adminService).toggleMeteredComponentStatus(1L);

        mockMvc.perform(patch("/api/admin/metered/1/toggle-status"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).toggleMeteredComponentStatus(1L);
    }

    // ==================== TAX RATES ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllTaxRates_Success() throws Exception {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(1L);
        taxRate.setName("GST");
        taxRate.setRegion("IN");
        taxRate.setRatePercent(new BigDecimal("18.00"));

        when(adminService.getAllTaxRates()).thenReturn(List.of(taxRate));

        mockMvc.perform(get("/api/admin/taxrates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("GST"))
                .andExpect(jsonPath("$[0].region").value("IN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateTaxRate_Success() throws Exception {
        TaxRate taxRate = new TaxRate();
        taxRate.setName("VAT");
        taxRate.setRegion("GB");
        taxRate.setRatePercent(new BigDecimal("20.00"));
        taxRate.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        when(adminService.createTaxRate(any(TaxRate.class))).thenReturn(taxRate);

        mockMvc.perform(post("/api/admin/taxrates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taxRate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VAT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateTaxRate_Success() throws Exception {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(1L);
        taxRate.setName("Updated GST");
        taxRate.setRegion("IN");
        taxRate.setRatePercent(new BigDecimal("12.00"));

        when(adminService.updateTaxRate(eq(1L), any(TaxRate.class))).thenReturn(taxRate);

        mockMvc.perform(put("/api/admin/taxrates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taxRate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated GST"));

        verify(adminService, times(1)).updateTaxRate(eq(1L), any(TaxRate.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteTaxRate_Success() throws Exception {
        doNothing().when(adminService).deleteTaxRate(1L);

        mockMvc.perform(delete("/api/admin/taxrates/1"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).deleteTaxRate(1L);
    }

    // ==================== COUPONS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllCoupons_Success() throws Exception {
        when(adminService.getAllCoupons()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateCoupon_Success() throws Exception {
        Coupon coupon = new Coupon();
        coupon.setCode("SUMMER20");

        when(adminService.createCoupon(any(Coupon.class))).thenReturn(coupon);

        mockMvc.perform(post("/api/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUMMER20"));

        verify(adminService, times(1)).createCoupon(any(Coupon.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateCoupon_Success() throws Exception {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode("WINTER30");

        when(adminService.updateCoupon(eq(1L), any(Coupon.class))).thenReturn(coupon);

        mockMvc.perform(put("/api/admin/coupons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WINTER30"));

        verify(adminService, times(1)).updateCoupon(eq(1L), any(Coupon.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testToggleCouponStatus_Success() throws Exception {
        doNothing().when(adminService).toggleCouponStatus(1L);

        mockMvc.perform(patch("/api/admin/coupons/1/toggle-status"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).toggleCouponStatus(1L);
    }

    // ==================== CUSTOMERS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllCustomers_Success() throws Exception {
        when(adminService.getAllCustomers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ==================== STAFF ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllStaff_Success() throws Exception {
        when(adminService.getAllStaff()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateStaff_Success() throws Exception {
        // Build a complete User with all required fields to avoid validation errors
        String staffJson = """
                {
                    "email": "newstaff@streamflix.com",
                    "fullName": "New Staff",
                    "passwordHash": "encoded_password",
                    "role": "FINANCE",
                    "status": "ACTIVE"
                }
                """;

        StaffResponse staffResponse = new StaffResponse();
        staffResponse.setEmail("newstaff@streamflix.com");
        staffResponse.setFullName("New Staff");

        when(adminService.createStaff(any(User.class))).thenReturn(staffResponse);

        mockMvc.perform(post("/api/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newstaff@streamflix.com"));

        verify(adminService, times(1)).createStaff(any(User.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteStaff_Success() throws Exception {
        doNothing().when(adminService).deleteStaff(1L);

        mockMvc.perform(delete("/api/admin/staff/1"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).deleteStaff(1L);
    }

    // ==================== SUBSCRIPTIONS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllSubscriptions_Success() throws Exception {
        when(adminService.getAllSubscriptions()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSubscriptionById_Success() throws Exception {
        SubscriptionResponse subResponse = new SubscriptionResponse();
        subResponse.setId(1L);
        subResponse.setCustomerName("Aarav Sharma");
        subResponse.setPlanName("Basic Monthly");
        subResponse.setStatus(Status.ACTIVE);

        when(adminService.getSubscriptionById(1L)).thenReturn(subResponse);

        mockMvc.perform(get("/api/admin/subscriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Aarav Sharma"))
                .andExpect(jsonPath("$.planName").value("Basic Monthly"));
    }

    // ==================== ROLE-BASED ACCESS CONTROL ====================

    @Test
    @WithMockUser(roles = "FINANCE")
    void testAdminEndpoint_ForbiddenForFinance() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    void testAdminEndpoint_ForbiddenForSupport() throws Exception {
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testAdminEndpoint_ForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/admin/subscriptions"))
                .andExpect(status().isForbidden());
    }
}

