package com.recuriq.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.recuriq.billing.dto.finance.*;

class ReportExportServiceImplTest {

    private ReportExportServiceImpl reportExportService;
    private FinanceDashboardDTO emptyData;
    private FinanceDashboardDTO fullData;

    @BeforeEach
    void setUp() {
        reportExportService = new ReportExportServiceImpl();
        emptyData = FinanceDashboardDTO.builder()
                .mrrMinor(1000L).arrMinor(12000L).arpuMinor(100L)
                .netChurnPercent(new BigDecimal("2.5")).ltvMinor(5000L).activeCustomers(10)
                .revenueByPlan(new ArrayList<>()).revenueByRegion(new ArrayList<>())
                .revenueTrend(new ArrayList<>()).build();

        fullData = FinanceDashboardDTO.builder()
                .mrrMinor(50000L).arrMinor(600000L).arpuMinor(500L)
                .netChurnPercent(new BigDecimal("1.5")).ltvMinor(20000L).activeCustomers(100)
                .revenueByPlan(Arrays.asList(
                        PlanRevenueDTO.builder().planId(1L).planName("Basic").revenueMinor(20000L).build(),
                        PlanRevenueDTO.builder().planId(2L).planName("Premium, Plus").revenueMinor(30000L).build()))
                .revenueByRegion(Arrays.asList(
                        RegionRevenueDTO.builder().region("India").revenueMinor(40000L).build(),
                        RegionRevenueDTO.builder().region("United States").revenueMinor(10000L).build()))
                .revenueTrend(Arrays.asList(
                        MonthlyTrendDTO.builder().month("January").year(2026).valueMinor(45000L).build(),
                        MonthlyTrendDTO.builder().month("February").year(2026).valueMinor(50000L).build()))
                .build();
    }

    // ==================== CSV EXPORT ====================
    @Test
    void testExportCsv_EmptyLists() {
        byte[] csv = reportExportService.exportDashboardAsCsv(emptyData);
        assertNotNull(csv);
        String csvStr = new String(csv);
        assertTrue(csvStr.contains("MRR"));
        assertTrue(csvStr.contains("ARR"));
        assertTrue(csvStr.contains("10.00")); // MRR = 1000/100
    }

    @Test
    void testExportCsv_WithData() {
        byte[] csv = reportExportService.exportDashboardAsCsv(fullData);
        String csvStr = new String(csv);
        assertTrue(csvStr.contains("Basic"));
        assertTrue(csvStr.contains("India"));
        assertTrue(csvStr.contains("January"));
        assertTrue(csvStr.contains("2026"));
    }

    @Test
    void testExportCsv_EscapesCommasInPlanName() {
        byte[] csv = reportExportService.exportDashboardAsCsv(fullData);
        String csvStr = new String(csv);
        // "Premium, Plus" should be quoted since it contains a comma
        assertTrue(csvStr.contains("\"Premium, Plus\""));
    }

    // ==================== PDF EXPORT ====================
    @Test
    void testExportPdf_EmptyLists() {
        byte[] pdf = reportExportService.exportDashboardAsPdf(emptyData);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // PDF starts with %PDF
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header);
    }

    @Test
    void testExportPdf_WithFullData() {
        byte[] pdf = reportExportService.exportDashboardAsPdf(fullData);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testExportPdf_NullRevenueTrend() {
        FinanceDashboardDTO nullTrend = FinanceDashboardDTO.builder()
                .mrrMinor(1000L).arrMinor(12000L).arpuMinor(100L)
                .netChurnPercent(new BigDecimal("0")).ltvMinor(2000L).activeCustomers(1)
                .revenueByPlan(new ArrayList<>()).revenueByRegion(new ArrayList<>())
                .revenueTrend(null).build();
        byte[] pdf = reportExportService.exportDashboardAsPdf(nullTrend);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testExportPdf_WithRevenueTrend() {
        byte[] pdf = reportExportService.exportDashboardAsPdf(fullData);
        assertNotNull(pdf);
        // PDF with trend data should be larger than without
        byte[] pdfEmpty = reportExportService.exportDashboardAsPdf(emptyData);
        assertTrue(pdf.length > pdfEmpty.length);
    }

    @Test
    void testExportCsv_NullAmountFormats() {
        FinanceDashboardDTO zeroData = FinanceDashboardDTO.builder()
                .mrrMinor(0L).arrMinor(0L).arpuMinor(0L)
                .netChurnPercent(BigDecimal.ZERO).ltvMinor(0L).activeCustomers(0)
                .revenueByPlan(new ArrayList<>()).revenueByRegion(new ArrayList<>())
                .revenueTrend(new ArrayList<>()).build();
        byte[] csv = reportExportService.exportDashboardAsCsv(zeroData);
        String csvStr = new String(csv);
        assertTrue(csvStr.contains("0.00"));
    }
}
