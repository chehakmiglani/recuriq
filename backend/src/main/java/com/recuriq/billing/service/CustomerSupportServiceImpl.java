package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.SupportMessageDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomerSupportServiceImpl implements CustomerSupportService {

   private static final String KEY_QUESTION = "question";
   private static final String KEY_ANSWER = "answer";
   private static final String KEY_CATEGORY = "category";
   private static final String CATEGORY_BILLING = "BILLING";

   public List<Map<String, String>> getFAQs() {
       return List.of(
               Map.of(KEY_QUESTION, "How do I change my plan?",
                      KEY_ANSWER, "Go to the Subscription page and click 'Change Plan' to upgrade or downgrade.",
                      KEY_CATEGORY, "ACCOUNT"),
               Map.of(KEY_QUESTION, "Can I cancel anytime?",
                      KEY_ANSWER, "Yes! You can cancel at any time. Access continues until the end of your billing period.",
                      KEY_CATEGORY, CATEGORY_BILLING),
               Map.of(KEY_QUESTION, "How do I update my payment method?",
                      KEY_ANSWER, "Visit Payment Methods page to add, edit, or remove payment options.",
                      KEY_CATEGORY, "PAYMENT"),
               Map.of(KEY_QUESTION, "What happens when my trial ends?",
                      KEY_ANSWER, "Your subscription will automatically start billing based on your selected plan.",
                      KEY_CATEGORY, CATEGORY_BILLING),
               Map.of(KEY_QUESTION, "How do I download my invoices?",
                      KEY_ANSWER, "Go to the Billing page, find your invoice, and click the download button.",
                      KEY_CATEGORY, CATEGORY_BILLING),
               Map.of(KEY_QUESTION, "What is the Ad-Free add-on?",
                      KEY_ANSWER, "The Ad-Free add-on removes all advertisements for ₹99/month.",
                      KEY_CATEGORY, "FEATURES"),
               Map.of(KEY_QUESTION, "How is download storage calculated?",
                      KEY_ANSWER, "You get 5-10GB free depending on your plan. Additional storage is ₹20/GB.",
                      KEY_CATEGORY, "FEATURES"),
               Map.of(KEY_QUESTION, "Can I pause my subscription?",
                      KEY_ANSWER, "Yes, you can pause for up to 1 month. You won't be charged during the pause period.",
                      KEY_CATEGORY, "ACCOUNT")
       );
   }

   public void sendSupportMessage(String email, SupportMessageDTO message) {
       // Support message logic removed
   }
}
