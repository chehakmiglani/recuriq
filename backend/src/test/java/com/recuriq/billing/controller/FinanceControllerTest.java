package com.recuriq.billing.controller;

import com.recuriq.billing.dto.finance.*;
import com.recuriq.billing.service.ReportExportService;
import com.recuriq.billing.service.RevenueAnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceControllerTest {

    @Mock
    private RevenueAnalyticsService revenueAnalyticsService;

    @Mock
    private ReportExportService reportExportService;

    @InjectMocks
    private FinanceController financeController;

    @Test
    void getDashboard_shouldReturnDashboardData() {
        FinanceDashboardDTO mockDashboard = FinanceDashboardDTO.builder()
                .mrrMinor(2250000L)
                .arrMinor(27000000L)
                .arpuMinor(750000L)
                .ltvMinor(95000L)
                .netChurnPercent(BigDecimal.valueOf(2.0))
                .activeCustomers(3)
                .failedPaymentsCount(10L)
                .refundAmountMinor(129500L)
                .build();

        when(revenueAnalyticsService.getFinanceDashboard()).thenReturn(mockDashboard);

        ResponseEntity<FinanceDashboardDTO> response = financeController.getDashboard();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockDashboard, response.getBody());
        verify(revenueAnalyticsService, times(1)).getFinanceDashboard();
    }

    @Test
    void getMrrReport_shouldReturnMrrReport() {
        MrrReportDTO mockReport = MrrReportDTO.builder()
                .mrrMinor(10000L)
                .revenueTrend(Collections.emptyList())
                .build();

        when(revenueAnalyticsService.getMrrReport()).thenReturn(mockReport);

        ResponseEntity<MrrReportDTO> response = financeController.getMrrReport();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockReport, response.getBody());
        verify(revenueAnalyticsService, times(1)).getMrrReport();
    }

    @Test
    void getArrReport_shouldReturnArrReport() {
        ArrReportDTO mockReport = ArrReportDTO.builder()
                .arrMinor(120000L)
                .revenueByPlan(Collections.emptyList())
                .revenueByRegion(Collections.emptyList())
                .arrTrend(Collections.emptyList())
                .build();

        when(revenueAnalyticsService.getArrReport()).thenReturn(mockReport);

        ResponseEntity<ArrReportDTO> response = financeController.getArrReport();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockReport, response.getBody());
        verify(revenueAnalyticsService, times(1)).getArrReport();
    }

    @Test
    void getChurnReport_shouldReturnChurnReport() {
        ChurnReportDTO mockReport = ChurnReportDTO.builder()
                .netChurnPercent(BigDecimal.valueOf(1.5))
                .revenueChurnPercent(BigDecimal.valueOf(20.0))
                .churnedRevenueMinor(950000L)
                .churnTrend(Collections.emptyList())
                .reasons(Collections.emptyList())
                .build();

        when(revenueAnalyticsService.getChurnReport()).thenReturn(mockReport);

        ResponseEntity<ChurnReportDTO> response = financeController.getChurnReport();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockReport, response.getBody());
        verify(revenueAnalyticsService, times(1)).getChurnReport();
    }

    @Test
    void getArpuLtvReport_shouldReturnArpuLtvReport() {
        ArpuLtvReportDTO mockReport = ArpuLtvReportDTO.builder()
                .arpuMinor(10000L)
                .ltvMinor(666666L)
                .cacLtvRatio("1:3.0")
                .arpuTrend(Collections.emptyList())
                .build();

        when(revenueAnalyticsService.getArpuLtvReport()).thenReturn(mockReport);

        ResponseEntity<ArpuLtvReportDTO> response = financeController.getArpuLtvReport();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockReport, response.getBody());
        verify(revenueAnalyticsService, times(1)).getArpuLtvReport();
    }

    @Test
    void exportReport_pdf_shouldReturnPdfFile() {
        FinanceDashboardDTO mockDashboard = FinanceDashboardDTO.builder().mrrMinor(1000L).build();
        byte[] pdfBytes = "MOCK PDF CONTENT".getBytes();

        when(revenueAnalyticsService.getFinanceDashboard()).thenReturn(mockDashboard);
        when(reportExportService.exportDashboardAsPdf(mockDashboard)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = financeController.exportReport("pdf");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(pdfBytes, response.getBody());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(Objects.requireNonNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=\"financial_report.pdf\""));

        verify(revenueAnalyticsService, times(1)).getFinanceDashboard();
        verify(reportExportService, times(1)).exportDashboardAsPdf(mockDashboard);
        verify(reportExportService, never()).exportDashboardAsCsv(any());
    }

    @Test
    void exportReport_pdfCaseInsensitive_shouldReturnPdfFile() {
        FinanceDashboardDTO mockDashboard = FinanceDashboardDTO.builder().mrrMinor(1000L).build();
        byte[] pdfBytes = "MOCK PDF CONTENT".getBytes();

        when(revenueAnalyticsService.getFinanceDashboard()).thenReturn(mockDashboard);
        when(reportExportService.exportDashboardAsPdf(mockDashboard)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = financeController.exportReport("PdF");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(pdfBytes, response.getBody());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(Objects.requireNonNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=\"financial_report.pdf\""));

        verify(revenueAnalyticsService, times(1)).getFinanceDashboard();
        verify(reportExportService, times(1)).exportDashboardAsPdf(mockDashboard);
        verify(reportExportService, never()).exportDashboardAsCsv(any());
    }

    @Test
    void exportReport_csv_shouldReturnCsvFile() {
        FinanceDashboardDTO mockDashboard = FinanceDashboardDTO.builder().mrrMinor(1000L).build();
        byte[] csvBytes = "MOCK CSV CONTENT".getBytes();

        when(revenueAnalyticsService.getFinanceDashboard()).thenReturn(mockDashboard);
        when(reportExportService.exportDashboardAsCsv(mockDashboard)).thenReturn(csvBytes);

        ResponseEntity<byte[]> response = financeController.exportReport("csv");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(csvBytes, response.getBody());
        assertEquals(MediaType.parseMediaType("text/csv"), response.getHeaders().getContentType());
        assertTrue(Objects.requireNonNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=\"financial_report.csv\""));

        verify(revenueAnalyticsService, times(1)).getFinanceDashboard();
        verify(reportExportService, times(1)).exportDashboardAsCsv(mockDashboard);
        verify(reportExportService, never()).exportDashboardAsPdf(any());
    }

    @Test
    void exportReport_defaultFormat_shouldReturnCsvFile() {
        FinanceDashboardDTO mockDashboard = FinanceDashboardDTO.builder().mrrMinor(1000L).build();
        byte[] csvBytes = "MOCK CSV CONTENT".getBytes();

        when(revenueAnalyticsService.getFinanceDashboard()).thenReturn(mockDashboard);
        when(reportExportService.exportDashboardAsCsv(mockDashboard)).thenReturn(csvBytes);

        ResponseEntity<byte[]> response = financeController.exportReport("excel");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(csvBytes, response.getBody());
        assertEquals(MediaType.parseMediaType("text/csv"), response.getHeaders().getContentType());
        assertTrue(Objects.requireNonNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=\"financial_report.csv\""));

        verify(revenueAnalyticsService, times(1)).getFinanceDashboard();
        verify(reportExportService, times(1)).exportDashboardAsCsv(mockDashboard);
        verify(reportExportService, never()).exportDashboardAsPdf(any());
    }

    @Test
    void getInvoices_shouldReturnPagedInvoices() {
        InvoiceRecordDTO invoice = InvoiceRecordDTO.builder()
                .invoiceNumber("INV-2026-1")
                .customerId(2L)
                .amount(190.0)
                .date(LocalDateTime.now())
                .dueDate(LocalDate.now())
                .status("OPEN")
                .build();
        Page<InvoiceRecordDTO> mockPage = new PageImpl<>(List.of(invoice));

        when(revenueAnalyticsService.getAllInvoiceRecords(PageRequest.of(1, 5))).thenReturn(mockPage);

        ResponseEntity<Page<InvoiceRecordDTO>> response = financeController.getInvoices(1, 5);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockPage, response.getBody());
        verify(revenueAnalyticsService, times(1)).getAllInvoiceRecords(PageRequest.of(1, 5));
    }

    @Test
    void getPayments_shouldReturnPagedPayments() {
        PaymentRecordDTO payment = PaymentRecordDTO.builder()
                .paymentId(777L)
                .invoiceNumber("INV-2026-1")
                .amount(475.0)
                .paymentMethod("CARD ****4321")
                .date(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        Page<PaymentRecordDTO> mockPage = new PageImpl<>(List.of(payment));

        when(revenueAnalyticsService.getAllPaymentRecords(PageRequest.of(2, 20))).thenReturn(mockPage);

        ResponseEntity<Page<PaymentRecordDTO>> response = financeController.getPayments(2, 20);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockPage, response.getBody());
        verify(revenueAnalyticsService, times(1)).getAllPaymentRecords(PageRequest.of(2, 20));
    }

    @Test
    void getRefunds_shouldReturnPagedRefunds() {
        RefundCreditDTO refund = RefundCreditDTO.builder()
                .refundId("REF-666")
                .paymentId(444L)
                .amount(258.0)
                .reason("Service issues")
                .date(LocalDateTime.now())
                .status("ISSUED")
                .build();
        Page<RefundCreditDTO> mockPage = new PageImpl<>(List.of(refund));

        when(revenueAnalyticsService.getAllRefundCredits(PageRequest.of(0, 10))).thenReturn(mockPage);

        ResponseEntity<Page<RefundCreditDTO>> response = financeController.getRefunds(0, 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockPage, response.getBody());
        verify(revenueAnalyticsService, times(1)).getAllRefundCredits(PageRequest.of(0, 10));
    }

    @Test
    void getSnapshots_shouldReturnPagedSnapshots() {
        RevenueSnapshotDTO snapshot = RevenueSnapshotDTO.builder()
                .activeCustomers(10)
                .newCustomers(2)
                .netChurnPercent(BigDecimal.ONE)
                .build();
        Page<RevenueSnapshotDTO> mockPage = new PageImpl<>(List.of(snapshot));

        when(revenueAnalyticsService.getAllRevenueSnapshots(PageRequest.of(3, 15))).thenReturn(mockPage);

        ResponseEntity<Page<RevenueSnapshotDTO>> response = financeController.getSnapshots(3, 15);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockPage, response.getBody());
        verify(revenueAnalyticsService, times(1)).getAllRevenueSnapshots(PageRequest.of(3, 15));
    }

    @Test
    void getInvoiceDetail_shouldReturnInvoiceDetail() {
        InvoiceDetailDTO mockDetail = InvoiceDetailDTO.builder()
                .invoiceNumber("INV-2026-1")
                .customerId(2L)
                .amount(190.0)
                .status("OPEN")
                .lineItems(Collections.emptyList())
                .build();

        when(revenueAnalyticsService.getInvoiceDetailById(1L)).thenReturn(mockDetail);

        ResponseEntity<InvoiceDetailDTO> response = financeController.getInvoiceDetail(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockDetail, response.getBody());
        verify(revenueAnalyticsService, times(1)).getInvoiceDetailById(1L);
    }
}
