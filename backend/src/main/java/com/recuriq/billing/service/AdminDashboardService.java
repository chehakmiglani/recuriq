package com.recuriq.billing.service;

import com.recuriq.billing.dto.admin.*;
import com.recuriq.billing.entity.*;
import java.util.List;
import java.util.Map;

public interface AdminDashboardService {

   // Dashboard
   Map<String, Object> getDashboardStats();

    // Product
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);
    ProductResponse createProduct(Product product);
    ProductResponse updateProduct(Long id, Product product);
    void toggleProductStatus(Long id);

   // Plan (returns DTO — has Product relationship)
   List<PlanResponse> getAllPlans();
   PlanResponse createPlan(Plan plan);
   PlanResponse updatePlan(Long id, Plan plan);
   void deletePlan(Long id);

   // Price Book (returns DTO — has Plan relationship)
   List<PriceBookResponse> getAllPriceBookEntries();
   PriceBookResponse createPriceBookEntry(PriceBookEntry entry);
   PriceBookResponse updatePriceBookEntry(Long id, PriceBookEntry entry);
   void archivePriceBookEntry(Long id);

   // Add-on (returns DTO)
   List<AddOnResponse> getAllAddOns();
   AddOnResponse createAddOn(AddOn addOn);
   AddOnResponse updateAddOn(Long id, AddOn addOn);
   void toggleAddOnStatus(Long id);

   // Metered Component (returns DTO — has Plan relationship)
   List<MeteredComponentResponse> getAllMeteredComponents();
   MeteredComponentResponse createMeteredComponent(MeteredComponent component);
   MeteredComponentResponse updateMeteredComponent(Long id, MeteredComponent component);
   void toggleMeteredComponentStatus(Long id);

   // Tax Rate (returns entity directly — no relationships)
   List<TaxRate> getAllTaxRates();
   TaxRate createTaxRate(TaxRate taxRate);
   TaxRate updateTaxRate(Long id, TaxRate taxRate);
   void deleteTaxRate(Long id);

   // Coupon (returns entity directly — no relationships)
   List<Coupon> getAllCoupons();
   Coupon createCoupon(Coupon coupon);
   Coupon updateCoupon(Long id, Coupon coupon);
   void toggleCouponStatus(Long id);

   // Users
   List<CustomerResponse> getAllCustomers();
   List<StaffResponse> getAllStaff();
   StaffResponse createStaff(User user);
   void deleteStaff(Long id);

   // Subscriptions
   List<SubscriptionResponse> getAllSubscriptions();
   SubscriptionResponse getSubscriptionById(Long id);
}
