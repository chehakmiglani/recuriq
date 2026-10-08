package com.recuriq.billing.scheduler;

import com.recuriq.billing.service.RevenueAnalyticsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnapshotScheduler {
    private final RevenueAnalyticsServiceImpl revenueService;
   @Scheduled(cron = "0 59 23 * * ?")
    public void generateDailySnapshot() {
        revenueService.generateDailySnapshot();    
    }
}