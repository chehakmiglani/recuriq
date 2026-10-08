package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AddOnRepository addOnRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PriceBookEntryRepository priceBookEntryRepository;

    public CustomerProfileDTO getProfile(String email) {
        Customer customer = getCustomerByEmail(email);
        return mapToProfileDTO(customer);
    }

    @Transactional
    public CustomerProfileDTO updateProfile(String email, CustomerProfileDTO dto) {
        Customer customer = getCustomerByEmail(email);
        customer.setPhone(dto.getPhone());
        customer.setState(dto.getState());
        customer.setCity(dto.getCity());
        customer.setAddressLine1(dto.getAddressLine1());
        customer.setPostalCode(dto.getPostalCode());
        customerRepository.save(customer);
        return mapToProfileDTO(customer);
    }

    public List<PlanDTO> getAvailablePlans(String region) {
        // Fetch pricebook entries, filter by region if provided, and only active plans
        return priceBookEntryRepository.findAll().stream()
                .filter(entry -> entry.getPlan().getStatus() == Status.ACTIVE)
                .filter(entry -> region == null || entry.getRegion().equalsIgnoreCase(region))
                .map(this::mapPriceBookEntryToPlanDTO)
                .toList();
    }

    public List<PlanDTO> getFeaturedPlans(String region) {
        // Return pricebook entries for Basic Monthly (id=1) and Premium Monthly (id=3)
        List<Long> featuredPlanIds = java.util.Arrays.asList(1L, 3L);
        return featuredPlanIds.stream()
                .flatMap(planId -> priceBookEntryRepository.findByPlan_Id(planId).stream())
                .filter(entry -> entry.getPlan().getStatus() == Status.ACTIVE)
                .filter(entry -> region == null || entry.getRegion().equalsIgnoreCase(region))
                .map(this::mapPriceBookEntryToPlanDTO)
                .toList();
    }

    public List<PlanDTO> getAllActivePlans(String region) {
        return getAvailablePlans(region);
    }

    public List<AddOnDTO> getAvailableAddOns(String email) {
        Customer customer = getCustomerByEmail(email);
        // Find current subscription period (include DRAFT for checkout flow)
        final com.recuriq.billing.enums.BillingPeriod currentPeriod = subscriptionRepository.findByCustomer_IdAndStatusIn(
                customer.getId(),
                List.of(com.recuriq.billing.enums.Status.ACTIVE, com.recuriq.billing.enums.Status.TRIALING,
                        com.recuriq.billing.enums.Status.DRAFT, com.recuriq.billing.enums.Status.PAST_DUE,
                        com.recuriq.billing.enums.Status.PAUSED))
                .stream()
                .sorted((s1, s2) -> s2.getId().compareTo(s1.getId()))
                .findFirst()
                .map(s -> s.getPlan().getBillingPeriod())
                .orElse(com.recuriq.billing.enums.BillingPeriod.MONTHLY);
        return addOnRepository.findByStatus(com.recuriq.billing.enums.Status.ACTIVE).stream()
                .filter(a -> a.getBillingPeriod() == currentPeriod)
                .map(this::mapToAddOnDTO)
                .toList();
    }

    private Customer getCustomerByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> com.recuriq.billing.exception.CustomException.notFound("User not found"));
        return customerRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> com.recuriq.billing.exception.CustomException.notFound("Customer not found"));
    }

    private CustomerProfileDTO mapToProfileDTO(Customer customer) {
        User user = customer.getUser();
        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setCustomerId(customer.getId());
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setCurrency(customer.getCurrency());
        dto.setCountry(customer.getCountry());
        dto.setState(customer.getState());
        dto.setCity(customer.getCity());
        dto.setAddressLine1(customer.getAddressLine1());
        dto.setPostalCode(customer.getPostalCode());
        dto.setStatus(customer.getStatus());
        dto.setCreatedAt(customer.getCreatedAt());
        return dto;
    }

    private PlanDTO mapPriceBookEntryToPlanDTO(PriceBookEntry entry) {
        Plan plan = entry.getPlan();
        PlanDTO dto = new PlanDTO();
        dto.setPlanId(plan.getId());
        dto.setName(plan.getName());
        dto.setBillingPeriod(plan.getBillingPeriod());
        dto.setDefaultPriceMinor(entry.getPriceMinor());
        dto.setDefaultCurrency(entry.getCurrency());
        dto.setTrialDays(plan.getTrialDays());
        dto.setSetupFeeMinor(plan.getSetupFeeMinor());
        dto.setTaxMode(plan.getTaxMode());
        dto.setEffectiveFrom(entry.getEffectiveFrom().toString());
        dto.setEffectiveTo(entry.getEffectiveTo() != null ? entry.getEffectiveTo().toString() : null);
        dto.setStatus(plan.getStatus());
        dto.setProductName("StreamFlix");
        return dto;
    }

    private AddOnDTO mapToAddOnDTO(AddOn addOn) {
        AddOnDTO dto = new AddOnDTO();
        dto.setAddOnId(addOn.getId());
        dto.setName(addOn.getName());
        dto.setPriceMinor(addOn.getPriceMinor());
        dto.setCurrency(addOn.getCurrency());
        dto.setBillingPeriod(addOn.getBillingPeriod());
        dto.setStatus(addOn.getStatus());
        return dto;
    }
}