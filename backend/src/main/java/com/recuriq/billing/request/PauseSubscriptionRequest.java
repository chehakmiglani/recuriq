package com.recuriq.billing.request;

import lombok.Data;

@Data
public class PauseSubscriptionRequest {
   private String pausedTo;
}