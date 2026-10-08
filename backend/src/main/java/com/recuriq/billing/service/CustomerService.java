package com.recuriq.billing.service;
import com.recuriq.billing.dto.customer.*;
import java.util.List;

public interface CustomerService {
   CustomerProfileDTO getProfile(String email);
   CustomerProfileDTO updateProfile(String email, CustomerProfileDTO dto);
   List<PlanDTO> getAvailablePlans(String region);
   List<PlanDTO> getFeaturedPlans(String region);
   List<PlanDTO> getAllActivePlans(String region);
   List<AddOnDTO> getAvailableAddOns(String email);
}
