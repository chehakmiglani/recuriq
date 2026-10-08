package com.recuriq.billing.controller;

import com.recuriq.billing.dto.admin.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminDashboardService adminService;

    // ==================== DASHBOARD ====================
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ==================== PRODUCT ====================
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getProductById(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(adminService.createProduct(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(adminService.updateProduct(id, product));
    }

    @PatchMapping("/products/{id}/toggle-status")
    public ResponseEntity<Void> toggleProductStatus(@PathVariable Long id) {
        adminService.toggleProductStatus(id);
        return ResponseEntity.ok().build();
    }

    // ==================== PLAN ====================
    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        return ResponseEntity.ok(adminService.getAllPlans());
    }

    @PostMapping("/plans")
    public ResponseEntity<PlanResponse> createPlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(adminService.createPlan(plan));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable Long id, @RequestBody Plan plan) {
        return ResponseEntity.ok(adminService.updatePlan(id, plan));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        adminService.deletePlan(id);
        return ResponseEntity.ok().build();
    }

    // ==================== PRICE BOOK ====================
    @GetMapping("/pricebooks")
    public ResponseEntity<List<PriceBookResponse>> getAllPriceBooks() {
        return ResponseEntity.ok(adminService.getAllPriceBookEntries());
    }

    @PostMapping("/pricebooks")
    public ResponseEntity<PriceBookResponse> createPriceBookEntry(@RequestBody PriceBookEntry entry) {
        return ResponseEntity.ok(adminService.createPriceBookEntry(entry));
    }

    @PutMapping("/pricebooks/{id}")
    public ResponseEntity<PriceBookResponse> updatePriceBookEntry(@PathVariable Long id,
            @RequestBody PriceBookEntry entry) {
        return ResponseEntity.ok(adminService.updatePriceBookEntry(id, entry));
    }

    @PutMapping("/pricebooks/{id}/archive")
    public ResponseEntity<Void> archivePriceBookEntry(@PathVariable Long id) {
        adminService.archivePriceBookEntry(id);
        return ResponseEntity.ok().build();
    }

    // ==================== ADD-ON ====================
    @GetMapping("/addons")
    public ResponseEntity<List<AddOnResponse>> getAllAddOns() {
        return ResponseEntity.ok(adminService.getAllAddOns());
    }

    @PostMapping("/addons")
    public ResponseEntity<AddOnResponse> createAddOn(@RequestBody AddOn addOn) {
        return ResponseEntity.ok(adminService.createAddOn(addOn));
    }

    @PutMapping("/addons/{id}")
    public ResponseEntity<AddOnResponse> updateAddOn(@PathVariable Long id, @RequestBody AddOn addOn) {
        return ResponseEntity.ok(adminService.updateAddOn(id, addOn));
    }

    @PatchMapping("/addons/{id}/toggle-status")
    public ResponseEntity<Void> toggleAddOnStatus(@PathVariable Long id) {
        adminService.toggleAddOnStatus(id);
        return ResponseEntity.ok().build();
    }

    // ==================== METERED COMPONENT ====================
    @GetMapping("/metered")
    public ResponseEntity<List<MeteredComponentResponse>> getAllMeteredComponents() {
        return ResponseEntity.ok(adminService.getAllMeteredComponents());
    }

    @PostMapping("/metered")
    public ResponseEntity<MeteredComponentResponse> createMeteredComponent(@RequestBody MeteredComponent component) {
        return ResponseEntity.ok(adminService.createMeteredComponent(component));
    }

    @PutMapping("/metered/{id}")
    public ResponseEntity<MeteredComponentResponse> updateMetered(@PathVariable Long id,
            @RequestBody MeteredComponent component) {
        return ResponseEntity.ok(adminService.updateMeteredComponent(id, component));
    }

    @PatchMapping("/metered/{id}/toggle-status")
    public ResponseEntity<Void> toggleMeteredStatus(@PathVariable Long id) {
        adminService.toggleMeteredComponentStatus(id);
        return ResponseEntity.ok().build();
    }

    // ==================== TAX RATE ====================
    @GetMapping("/taxrates")
    public ResponseEntity<List<TaxRate>> getAllTaxRates() {
        return ResponseEntity.ok(adminService.getAllTaxRates());
    }

    @PostMapping("/taxrates")
    public ResponseEntity<TaxRate> createTaxRate(@RequestBody TaxRate taxRate) {
        return ResponseEntity.ok(adminService.createTaxRate(taxRate));
    }

    @PutMapping("/taxrates/{id}")
    public ResponseEntity<TaxRate> updateTaxRate(@PathVariable Long id, @RequestBody TaxRate taxRate) {
        return ResponseEntity.ok(adminService.updateTaxRate(id, taxRate));
    }

    @DeleteMapping("/taxrates/{id}")
    public ResponseEntity<Void> deleteTaxRate(@PathVariable Long id) {
        adminService.deleteTaxRate(id);
        return ResponseEntity.ok().build();
    }

    // ==================== COUPON ====================
    @GetMapping("/coupons")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(adminService.getAllCoupons());
    }

    @PostMapping("/coupons")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(adminService.createCoupon(coupon));
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        return ResponseEntity.ok(adminService.updateCoupon(id, coupon));
    }

    @PatchMapping("/coupons/{id}/toggle-status")
    public ResponseEntity<Void> toggleCouponStatus(@PathVariable Long id) {
        adminService.toggleCouponStatus(id);
        return ResponseEntity.ok().build();
    }

    // ==================== CUSTOMERS ====================
    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(adminService.getAllCustomers());
    }

    // ==================== STAFF ====================
    @GetMapping("/staff")
    public ResponseEntity<List<StaffResponse>> getAllStaff() {
        return ResponseEntity.ok(adminService.getAllStaff());
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffResponse> createStaff(@RequestBody User user) {
        return ResponseEntity.ok(adminService.createStaff(user));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        adminService.deleteStaff(id);
        return ResponseEntity.ok().build();
    }

    // ==================== SUBSCRIPTIONS ====================
    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(adminService.getAllSubscriptions());
    }

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getSubscriptionById(id));
    }
}
