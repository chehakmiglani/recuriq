package com.recuriq.billing.service;
import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.request.*;
import java.util.List;

public interface CustomerSubscriptionService {
   SubscriptionDTO getCurrentSubscription(String email);
   SubscriptionDTO createSubscription(String email, CreateSubscriptionRequest request);
   SubscriptionDTO upgradeSubscription(String email, UpgradeSubscriptionRequest request);
   CancellationResponse cancelSubscription(String email, boolean atPeriodEnd);
   SubscriptionDTO pauseSubscription(String email, PauseSubscriptionRequest request);
   SubscriptionDTO resumeSubscription(String email);
   SubscriptionDTO addAddOn(String email, Long addonId);
   SubscriptionDTO removeAddOn(String email, Long addonId);
   List<UsageRecordDTO> getMeteredUsage(String email, String startDate, String endDate);
   CancellationRequestDTO createCancellationRequest(String email, CancellationRequestInput input);
   CancellationRequestDTO withdrawCancellationRequest(String email);
   CancellationRequestDTO getPendingCancellationRequest(String email);
}
