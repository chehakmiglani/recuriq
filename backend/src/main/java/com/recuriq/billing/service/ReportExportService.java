package com.recuriq.billing.service;

import com.recuriq.billing.dto.finance.FinanceDashboardDTO;

public interface ReportExportService {
    byte[] exportDashboardAsCsv(FinanceDashboardDTO dashboardData);
    byte[] exportDashboardAsPdf(FinanceDashboardDTO dashboardData);
}
