package com.recuriq.billing.service;
import com.recuriq.billing.dto.finance.*;

public interface RevenueAnalyticsService {
        FinanceDashboardDTO getFinanceDashboard();
        MrrReportDTO getMrrReport();
        ArrReportDTO getArrReport();
        ChurnReportDTO getChurnReport();
        ArpuLtvReportDTO getArpuLtvReport();
        org.springframework.data.domain.Page<InvoiceRecordDTO> getAllInvoiceRecords(org.springframework.data.domain.Pageable pageable);
        InvoiceDetailDTO getInvoiceDetailById(Long invoiceId);
        org.springframework.data.domain.Page<PaymentRecordDTO> getAllPaymentRecords(org.springframework.data.domain.Pageable pageable);
        org.springframework.data.domain.Page<RefundCreditDTO> getAllRefundCredits(org.springframework.data.domain.Pageable pageable);
        org.springframework.data.domain.Page<RevenueSnapshotDTO> getAllRevenueSnapshots(org.springframework.data.domain.Pageable pageable);
}
