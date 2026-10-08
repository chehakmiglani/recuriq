package com.recuriq.billing.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.awt.Color;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.recuriq.billing.dto.finance.FinanceDashboardDTO;
import com.recuriq.billing.dto.finance.PlanRevenueDTO;
import com.recuriq.billing.dto.finance.RegionRevenueDTO;
import com.recuriq.billing.exception.CustomException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    // ── Color Palette ──
    private static final Color BRAND_COLOR = new Color(91, 79, 255);   // #5b4fff
    private static final Color HEADER_BG = new Color(249, 250, 251);   // #f9fafb
    private static final Color BORDER_COLOR = new Color(229, 231, 235); // #e5e7eb
    private static final Color TEXT_PRIMARY = new Color(31, 41, 55);    // #1f2937
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128); // #6b7280

    // ── Fonts ──
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND_COLOR);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, TEXT_SECONDARY);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_SECONDARY);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_PRIMARY);
    private static final Font VALUE_BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_PRIMARY);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_SECONDARY);
    private static final Font TABLE_CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_PRIMARY);
    private static final Font TABLE_CELL_BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_PRIMARY);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, TEXT_PRIMARY);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_SECONDARY);

    private static final String MRR_CONTRIBUTION_LABEL = "MRR Contribution";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // CSV Export
    @Override
    public byte[] exportDashboardAsCsv(FinanceDashboardDTO data) {
        StringBuilder csv = new StringBuilder();

        csv.append("Metric,Value\n");
        csv.append("MRR,").append(formatAmount(data.getMrrMinor())).append("\n");
        csv.append("ARR,").append(formatAmount(data.getArrMinor())).append("\n");
        csv.append("ARPU,").append(formatAmount(data.getArpuMinor())).append("\n");
        csv.append("Net Churn %,").append(data.getNetChurnPercent()).append("\n");
        csv.append("LTV,").append(formatAmount(data.getLtvMinor())).append("\n");
        csv.append("Active Customers,").append(data.getActiveCustomers()).append("\n");

        csv.append("\nRevenue by Plan (MRR)\n");
        csv.append("Plan ID,Plan Name,").append(MRR_CONTRIBUTION_LABEL).append(" (INR)\n");
        for (PlanRevenueDTO p : data.getRevenueByPlan()) {
            csv.append(p.getPlanId()).append(",")
               .append(escapeCsv(p.getPlanName())).append(",")
               .append(formatAmount(p.getRevenueMinor())).append("\n");
        }

        csv.append("\nRevenue by Region (MRR)\n");
        csv.append("Region,").append(MRR_CONTRIBUTION_LABEL).append(" (INR)\n");
        for (RegionRevenueDTO r : data.getRevenueByRegion()) {
            csv.append(escapeCsv(r.getRegion())).append(",")
               .append(formatAmount(r.getRevenueMinor())).append("\n");
        }

        csv.append("\nMonthly Revenue Trend\n");
        csv.append("Month,Year,MRR (INR)\n");
        data.getRevenueTrend().forEach(t ->
            csv.append(t.getMonth()).append(",")
               .append(t.getYear()).append(",")
               .append(formatAmount(t.getValueMinor())).append("\n")
        );

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // PDF Export
    @Override
    public byte[] exportDashboardAsPdf(FinanceDashboardDTO data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Company & Document Header
            addCompanyHeader(document);
            document.add(Chunk.NEWLINE);

            // 2. Report Metadata
            addReportMetadata(document);
            document.add(Chunk.NEWLINE);

            // 3. KPI Highlights Grid
            Paragraph kpiTitle = new Paragraph("KEY PERFORMANCE INDICATORS", VALUE_BOLD_FONT);
            kpiTitle.setSpacingAfter(4f);
            document.add(kpiTitle);
            addKpiHighlights(document, data);
            document.add(Chunk.NEWLINE);

            // 4. Revenue by Plan Table
            Paragraph planHeading = new Paragraph("REVENUE BY PLAN (" + MRR_CONTRIBUTION_LABEL.toUpperCase() + ")", VALUE_BOLD_FONT);
            planHeading.setSpacingBefore(10f);
            planHeading.setSpacingAfter(6f);
            document.add(planHeading);

            PdfPTable planTable = new PdfPTable(3);
            planTable.setWidthPercentage(100);
            planTable.setWidths(new float[]{1f, 2.5f, 1.5f});
            planTable.setSpacingAfter(15f);
            addTableHeader(planTable, "Plan ID", "Plan Name", MRR_CONTRIBUTION_LABEL);

            for (PlanRevenueDTO p : data.getRevenueByPlan()) {
                addTableCell(planTable, String.valueOf(p.getPlanId()), false, Element.ALIGN_CENTER);
                addTableCell(planTable, p.getPlanName(), false, Element.ALIGN_LEFT);
                addTableCell(planTable, formatCurrency(p.getRevenueMinor()), false, Element.ALIGN_RIGHT);
            }
            if (data.getRevenueByPlan().isEmpty()) {
                addEmptyRow(planTable, 3, "No plan revenue recorded");
            }
            document.add(planTable);
            document.add(Chunk.NEWLINE);

            // 5. Revenue by Region Table
            Paragraph regionHeading = new Paragraph("REVENUE BY REGION (" + MRR_CONTRIBUTION_LABEL.toUpperCase() + ")", VALUE_BOLD_FONT);
            regionHeading.setSpacingBefore(10f);
            regionHeading.setSpacingAfter(6f);
            document.add(regionHeading);

            PdfPTable regionTable = new PdfPTable(2);
            regionTable.setWidthPercentage(100);
            regionTable.setWidths(new float[]{2f, 1.5f});
            regionTable.setSpacingAfter(15f);
            addTableHeader(regionTable, "Region", MRR_CONTRIBUTION_LABEL);

            for (RegionRevenueDTO r : data.getRevenueByRegion()) {
                addTableCell(regionTable, r.getRegion(), false, Element.ALIGN_LEFT);
                addTableCell(regionTable, formatCurrency(r.getRevenueMinor()), false, Element.ALIGN_RIGHT);
            }
            if (data.getRevenueByRegion().isEmpty()) {
                addEmptyRow(regionTable, 2, "No regional revenue recorded");
            }
            document.add(regionTable);
            document.add(Chunk.NEWLINE);

            // 6. Monthly Revenue Trend Section
            if (data.getRevenueTrend() != null && !data.getRevenueTrend().isEmpty()) {
                Paragraph trendHeading = new Paragraph("MONTHLY REVENUE TREND", VALUE_BOLD_FONT);
                trendHeading.setSpacingBefore(10f);
                trendHeading.setSpacingAfter(6f);
                document.add(trendHeading);

                PdfPTable trendTable = new PdfPTable(3);
                trendTable.setWidthPercentage(100);
                trendTable.setWidths(new float[]{1.5f, 1.5f, 2f});
                trendTable.setSpacingAfter(15f);
                addTableHeader(trendTable, "Month", "Year", MRR_CONTRIBUTION_LABEL);

                data.getRevenueTrend().forEach(t -> {
                    addTableCell(trendTable, t.getMonth(), false, Element.ALIGN_LEFT);
                    addTableCell(trendTable, String.valueOf(t.getYear()), false, Element.ALIGN_CENTER);
                    addTableCell(trendTable, formatCurrency(t.getValueMinor()), false, Element.ALIGN_RIGHT);
                });
                document.add(trendTable);
            }

            // 7. Footer
            addFooter(document);

            document.close();
        } catch (Exception e) {
            throw new CustomException("Error generating PDF report", HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED");
        }

        return out.toByteArray();
    }

    // ────────────────────────────────────────────────────────────────
    // SECTION BUILDERS
    // ────────────────────────────────────────────────────────────────

    private void addCompanyHeader(Document document) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.5f, 1f});

        // Company name
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(0);
        brandCell.setPaddingBottom(8);
        Paragraph brand = new Paragraph("STREAMFLIX", TITLE_FONT);
        brandCell.addElement(brand);
        Paragraph tagline = new Paragraph("Subscription Billing & Revenue Management", SUBTITLE_FONT);
        brandCell.addElement(tagline);
        header.addCell(brandCell);

        // Report type
        PdfPCell typeCell = new PdfPCell();
        typeCell.setBorder(0);
        typeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Font typeFont = new Font(Font.HELVETICA, 16, Font.BOLD, BRAND_COLOR);
        Paragraph typePara = new Paragraph("REVENUE REPORT", typeFont);
        typePara.setAlignment(Element.ALIGN_RIGHT);
        typeCell.addElement(typePara);
        header.addCell(typeCell);

        document.add(header);

        // Divider line
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell line = new PdfPCell();
        line.setBorder(Rectangle.BOTTOM);
        line.setBorderColor(BRAND_COLOR);
        line.setBorderWidth(2);
        line.setFixedHeight(4);
        divider.addCell(line);
        document.add(divider);
    }

    private void addReportMetadata(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1f});

        // Left Details
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(0);
        leftCell.addElement(new Paragraph("REPORT DETAILS", LABEL_FONT));
        leftCell.addElement(createSpacing(4));
        leftCell.addElement(createLabelValue("Scope", "Global Revenue & Analytics"));
        leftCell.addElement(createLabelValue("Generated At", LocalDateTime.now().format(DATE_FMT)));
        table.addCell(leftCell);

        // Right Details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(0);
        rightCell.addElement(new Paragraph("SYSTEM METRICS", LABEL_FONT));
        rightCell.addElement(createSpacing(4));
        rightCell.addElement(createLabelValue("Generator", "StreamFlix Financial Engine"));
        rightCell.addElement(createLabelValue("Status", "CONFIDENTIAL / INTERNAL"));
        table.addCell(rightCell);

        document.add(table);
    }

    private void addKpiHighlights(Document document, FinanceDashboardDTO data) throws DocumentException {
        PdfPTable grid = new PdfPTable(3);
        grid.setWidthPercentage(100);
        grid.setWidths(new float[]{1f, 1f, 1f});
        grid.setSpacingBefore(6f);
        grid.setSpacingAfter(10f);

        // Row 1
        grid.addCell(createKpiCard("MRR", formatCurrency(data.getMrrMinor())));
        grid.addCell(createKpiCard("ARR", formatCurrency(data.getArrMinor())));
        grid.addCell(createKpiCard("ARPU", formatCurrency(data.getArpuMinor())));

        // Row 2
        grid.addCell(createKpiCard("NET CHURN %", String.format("%.2f%%", data.getNetChurnPercent())));
        grid.addCell(createKpiCard("LTV", formatCurrency(data.getLtvMinor())));
        grid.addCell(createKpiCard("ACTIVE CUSTOMERS", String.valueOf(data.getActiveCustomers())));

        document.add(grid);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        Paragraph footer1 = new Paragraph("This is a computer-generated revenue report compiled directly from internal billing ledgers.", FOOTER_FONT);
        footer1.setAlignment(Element.ALIGN_CENTER);
        document.add(footer1);

        Paragraph footer2 = new Paragraph("StreamFlix Financial Engine • Confidential Internal Document", FOOTER_FONT);
        footer2.setAlignment(Element.ALIGN_CENTER);
        document.add(footer2);
    }

    // ────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TABLE_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(BORDER_COLOR);
            cell.setBorderWidth(1.5f);
            cell.setPadding(8);
            if (header.contains("Contribution") || header.contains(MRR_CONTRIBUTION_LABEL)) {
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            } else if (header.equals("Plan Name") || header.equals("Region") || header.equals("Month")) {
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            } else {
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            }
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text, boolean bold, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold ? TABLE_CELL_BOLD_FONT : TABLE_CELL_FONT));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addEmptyRow(PdfPTable table, int colspan, String message) {
        PdfPCell emptyCell = new PdfPCell(new Phrase(message, TABLE_CELL_FONT));
        emptyCell.setColspan(colspan);
        emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        emptyCell.setBorder(Rectangle.BOTTOM);
        emptyCell.setBorderColor(BORDER_COLOR);
        emptyCell.setPadding(16);
        table.addCell(emptyCell);
    }

    private PdfPCell createKpiCard(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(1.2f);
        cell.setPadding(12);

        Paragraph labelPara = new Paragraph(label.toUpperCase(), LABEL_FONT);
        labelPara.setSpacingAfter(4f);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value, TOTAL_FONT);
        cell.addElement(valuePara);

        return cell;
    }

    private Paragraph createSpacing(float points) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(points);
        return p;
    }

    private Paragraph createLabelValue(String label, String value) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(2);
        p.add(new Chunk(label + ": ", LABEL_FONT));
        p.add(new Chunk(value != null ? value : "—", VALUE_FONT));
        return p;
    }

    private String formatCurrency(Long amountMinor) {
        if (amountMinor == null) return "—";
        double amount = amountMinor / 100.0;

        try {
            Locale locale = Locale.forLanguageTag("en-IN"); // Default Indian Rupee format
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
            fmt.setCurrency(Currency.getInstance("INR"));
            return fmt.format(amount);
        } catch (Exception e) {
            return "₹ " + String.format("%.2f", amount);
        }
    }

    private String formatAmount(long minor) {
        return String.format("%.2f", minor / 100.0);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
