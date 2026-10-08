package com.recuriq.billing.service;

import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.InvoiceLineItemRepository;
import com.recuriq.billing.repository.PaymentRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of {@link InvoicePdfService} using OpenPDF.
 *
 * <p>Generates professional PDF invoices. Invoice type depends on status:</p>
 * <ul>
 *   <li>OPEN → "PROFORMA INVOICE" (payment pending)</li>
 *   <li>PAID → "TAX INVOICE" (payment complete)</li>
 *   <li>VOID → "VOID INVOICE" (canceled)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final InvoiceLineItemRepository lineItemRepository;
    private final PaymentRepository paymentRepository;

    // ── Color Palette ──
    private static final Color BRAND_COLOR = new Color(91, 79, 255);   // #5b4fff
    private static final Color HEADER_BG = new Color(249, 250, 251);   // #f9fafb
    private static final Color BORDER_COLOR = new Color(229, 231, 235); // #e5e7eb
    private static final Color TEXT_PRIMARY = new Color(31, 41, 55);    // #1f2937
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128); // #6b7280
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74); // #16a34a
    private static final Color DANGER_COLOR = new Color(220, 38, 38);  // #dc2626

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
    private static final Font STAMP_FONT = new Font(Font.HELVETICA, 28, Font.BOLD, SUCCESS_COLOR);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public byte[] generatePdf(Invoice invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();
            populateDocument(document, invoice);
            document.close();
        } catch (Exception e) {
            throw new CustomException("Failed to generate invoice PDF", HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED");
        }

        return out.toByteArray();
    }

    private void populateDocument(Document document, Invoice invoice) throws DocumentException {
        // 1. Company Header
        addCompanyHeader(document, invoice);
        document.add(Chunk.NEWLINE);

        // 2. Invoice Metadata + Bill To
        addInvoiceMetaAndBillTo(document, invoice);
        document.add(Chunk.NEWLINE);

        // 3. Subscription Info
        if (invoice.getSubscription() != null && invoice.getSubscription().getPlan() != null) {
            addSubscriptionInfo(document, invoice);
            document.add(Chunk.NEWLINE);
        }

        // 4. Line Items Table
        addLineItemsTable(document, invoice);
        document.add(Chunk.NEWLINE);

        // 5. Totals
        addTotals(document, invoice);
        document.add(Chunk.NEWLINE);

        // 6. Payment Status / Stamp
        addPaymentStatus(document, invoice);

        // 7. Footer
        addFooter(document);
    }

    // ────────────────────────────────────────────────────────────────
    // SECTION BUILDERS
    // ────────────────────────────────────────────────────────────────

    private void addCompanyHeader(Document document, Invoice invoice) throws DocumentException {
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

        // Invoice type
        String invoiceType = getInvoiceTypeLabel(invoice.getStatus());
        PdfPCell typeCell = new PdfPCell();
        typeCell.setBorder(0);
        typeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Font typeFont = new Font(Font.HELVETICA, 16, Font.BOLD, BRAND_COLOR);
        Paragraph typePara = new Paragraph(invoiceType, typeFont);
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

    private void addInvoiceMetaAndBillTo(Document document, Invoice invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});

        // Left: Invoice Details
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(0);
        metaCell.setPaddingRight(20);
        metaCell.addElement(new Paragraph("INVOICE DETAILS", LABEL_FONT));
        metaCell.addElement(createSpacing(6));
        metaCell.addElement(createLabelValue("Invoice #", invoice.getInvoiceNumber()));
        metaCell.addElement(createLabelValue("Issue Date", invoice.getIssueDate() != null ? invoice.getIssueDate().format(DATE_FMT) : "—"));
        metaCell.addElement(createLabelValue("Due Date", invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FMT) : "—"));
        metaCell.addElement(createLabelValue("Status", invoice.getStatus() != null ? invoice.getStatus().name() : "—"));
        metaCell.addElement(createLabelValue("Currency", invoice.getCurrency()));
        if (invoice.getBillingReason() != null) {
            metaCell.addElement(createLabelValue("Billing Reason", formatBillingReason(invoice.getBillingReason().name())));
        }
        table.addCell(metaCell);

        // Right: Bill To
        PdfPCell billToCell = new PdfPCell();
        billToCell.setBorder(0);
        billToCell.setPaddingLeft(20);
        billToCell.addElement(new Paragraph("BILL TO", LABEL_FONT));
        billToCell.addElement(createSpacing(6));

        Customer customer = invoice.getCustomer();
        if (customer != null) {
            billToCell.addElement(new Paragraph(customer.getUser().getFullName(), VALUE_BOLD_FONT));
            billToCell.addElement(new Paragraph(customer.getUser().getEmail(), VALUE_FONT));
            if (customer.getPhone() != null) {
                billToCell.addElement(new Paragraph(customer.getPhone(), VALUE_FONT));
            }
            billToCell.addElement(createSpacing(4));

            String address = buildCustomerAddress(customer);
            if (!address.isEmpty()) {
                billToCell.addElement(new Paragraph(address, VALUE_FONT));
            }
        }
        table.addCell(billToCell);

        document.add(table);
    }

    private void addSubscriptionInfo(Document document, Invoice invoice) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(1);
        infoTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(12);
        cell.setBackgroundColor(HEADER_BG);

        Paragraph title = new Paragraph("SUBSCRIPTION", LABEL_FONT);
        cell.addElement(title);
        cell.addElement(createSpacing(4));

        Subscription sub = invoice.getSubscription();
        Plan plan = sub.getPlan();

        PdfPTable subDetails = new PdfPTable(3);
        subDetails.setWidthPercentage(100);
        subDetails.setWidths(new float[]{2f, 1f, 1.5f});

        subDetails.addCell(createDetailCell("Plan", plan.getName()));
        subDetails.addCell(createDetailCell("Billing Period", plan.getBillingPeriod() != null ? plan.getBillingPeriod().name() : "—"));

        String period = "—";
        if (sub.getCurrentPeriodStart() != null && sub.getCurrentPeriodEnd() != null) {
            period = sub.getCurrentPeriodStart().format(DATE_FMT) + " → " + sub.getCurrentPeriodEnd().format(DATE_FMT);
        }
        subDetails.addCell(createDetailCell("Period", period));

        cell.addElement(subDetails);
        infoTable.addCell(cell);
        document.add(infoTable);
    }

    private void addLineItemsTable(Document document, Invoice invoice) throws DocumentException {
        List<InvoiceLineItem> lineItems = lineItemRepository.findByInvoice_Id(invoice.getId());

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.2f, 1.2f, 1.2f, 1.5f});

        // Header row
        addTableHeader(table, "Description");
        addTableHeader(table, "Type");
        addTableHeader(table, "Qty");
        addTableHeader(table, "Unit Price");
        addTableHeader(table, "Amount");

        // Data rows
        for (InvoiceLineItem item : lineItems) {
            addTableCell(table, item.getDescription(), false, Element.ALIGN_LEFT);
            addTableCell(table, item.getLineType() != null ? item.getLineType().name() : "—", false, Element.ALIGN_CENTER);
            addTableCell(table, String.valueOf(item.getQuantity()), false, Element.ALIGN_CENTER);
            addTableCell(table, formatCurrency(item.getUnitPriceMinor(), invoice.getCurrency()), false, Element.ALIGN_RIGHT);

            boolean isNegative = item.getAmountMinor() != null && item.getAmountMinor() < 0;
            String amountText = formatCurrency(item.getAmountMinor(), invoice.getCurrency());
            PdfPCell amountCell = new PdfPCell(new Phrase(amountText,
                    isNegative ? new Font(Font.HELVETICA, 9, Font.NORMAL, DANGER_COLOR) : TABLE_CELL_FONT));
            amountCell.setBorder(Rectangle.BOTTOM);
            amountCell.setBorderColor(BORDER_COLOR);
            amountCell.setPadding(8);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);
        }

        // Empty row if no items
        if (lineItems.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No line items", TABLE_CELL_FONT));
            emptyCell.setColspan(5);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setBorder(Rectangle.BOTTOM);
            emptyCell.setBorderColor(BORDER_COLOR);
            emptyCell.setPadding(16);
            table.addCell(emptyCell);
        }

        document.add(table);
    }

    private void addTotals(Document document, Invoice invoice) throws DocumentException {
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(50);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTotalRow(totals, "Subtotal", formatCurrency(invoice.getSubtotalMinor(), invoice.getCurrency()), false);

        if (invoice.getDiscountMinor() != null && invoice.getDiscountMinor() > 0) {
            addTotalRow(totals, "Discount", "−" + formatCurrency(invoice.getDiscountMinor(), invoice.getCurrency()), false);
        }

        if (invoice.getTaxMinor() != null && invoice.getTaxMinor() > 0) {
            addTotalRow(totals, "Tax", formatCurrency(invoice.getTaxMinor(), invoice.getCurrency()), false);
        }

        // Grand Total separator
        PdfPCell sepLeft = new PdfPCell();
        sepLeft.setBorder(Rectangle.TOP);
        sepLeft.setBorderColor(TEXT_PRIMARY);
        sepLeft.setBorderWidth(1.5f);
        sepLeft.setFixedHeight(4);
        totals.addCell(sepLeft);
        PdfPCell sepRight = new PdfPCell();
        sepRight.setBorder(Rectangle.TOP);
        sepRight.setBorderColor(TEXT_PRIMARY);
        sepRight.setBorderWidth(1.5f);
        sepRight.setFixedHeight(4);
        totals.addCell(sepRight);

        addTotalRow(totals, "TOTAL", formatCurrency(invoice.getTotalMinor(), invoice.getCurrency()), true);

        if (invoice.getBalanceMinor() != null && invoice.getBalanceMinor() > 0
                && !invoice.getBalanceMinor().equals(invoice.getTotalMinor())) {
            addTotalRow(totals, "Balance Due", formatCurrency(invoice.getBalanceMinor(), invoice.getCurrency()), true);
        }

        document.add(totals);
    }

    private void addPaymentStatus(Document document, Invoice invoice) throws DocumentException {
        PdfPTable statusTable = new PdfPTable(1);
        statusTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(16);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (invoice.getStatus() == Status.PAID) {
            addPaidStatus(cell, invoice);
        } else if (invoice.getStatus() == Status.VOID || invoice.getStatus() == Status.CANCELED) {
            addVoidStatus(cell);
        } else {
            addOpenStatus(cell, invoice);
        }

        statusTable.addCell(cell);
        document.add(statusTable);
    }

    private void addPaidStatus(PdfPCell cell, Invoice invoice) {
        cell.setBackgroundColor(new Color(240, 253, 244)); // green-50
        cell.setBorderColor(new Color(187, 247, 208));     // green-200

        Paragraph stamp = new Paragraph("✓  PAID", STAMP_FONT);
        stamp.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(stamp);

        // Show payment reference if available
        List<Payment> payments = paymentRepository.findByInvoice_Id(invoice.getId());
        if (!payments.isEmpty()) {
            Payment lastPayment = payments.get(payments.size() - 1);
            Paragraph ref = new Paragraph(
                    "Payment Ref: " + (lastPayment.getGatewayRef() != null ? lastPayment.getGatewayRef() : "—")
                    + "  •  " + (lastPayment.getCreatedAt() != null ? lastPayment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : ""),
                    FOOTER_FONT);
            ref.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(ref);
        }
    }

    private void addVoidStatus(PdfPCell cell) {
        cell.setBackgroundColor(new Color(254, 242, 242)); // red-50
        cell.setBorderColor(new Color(254, 202, 202));     // red-200

        Font voidFont = new Font(Font.HELVETICA, 28, Font.BOLD, DANGER_COLOR);
        Paragraph stamp = new Paragraph("VOID", voidFont);
        stamp.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(stamp);
    }

    private void addOpenStatus(PdfPCell cell, Invoice invoice) {
        cell.setBackgroundColor(new Color(255, 247, 237)); // amber-50
        cell.setBorderColor(new Color(254, 215, 170));     // amber-200

        Font openFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(194, 65, 12)); // orange-700
        Paragraph stamp = new Paragraph("PAYMENT PENDING", openFont);
        stamp.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(stamp);

        Paragraph note = new Paragraph(
                "This invoice is awaiting payment. Amount due: " + formatCurrency(invoice.getBalanceMinor(), invoice.getCurrency()),
                FOOTER_FONT);
        note.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(note);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        Paragraph footer1 = new Paragraph("This is a computer-generated invoice and does not require a signature.", FOOTER_FONT);
        footer1.setAlignment(Element.ALIGN_CENTER);
        document.add(footer1);

        Paragraph footer2 = new Paragraph("StreamFlix • Subscription Billing & Revenue Management System", FOOTER_FONT);
        footer2.setAlignment(Element.ALIGN_CENTER);
        document.add(footer2);
    }

    // ────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(1.5f);
        cell.setPadding(8);
        if ("Unit Price".equals(text) || "Amount".equals(text)) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        } else if ("Description".equals(text)) {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        } else {
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, boolean bold, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold ? TABLE_CELL_BOLD_FONT : TABLE_CELL_FONT));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean bold) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, bold ? VALUE_BOLD_FONT : VALUE_FONT));
        labelCell.setBorder(0);
        labelCell.setPadding(4);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        Font valFont = bold ? TOTAL_FONT : VALUE_FONT;
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valFont));
        valueCell.setBorder(0);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private PdfPCell createDetailCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.addElement(new Paragraph(label.toUpperCase(), LABEL_FONT));
        cell.addElement(new Paragraph(value, VALUE_BOLD_FONT));
        return cell;
    }

    private Paragraph createLabelValue(String label, String value) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(2);
        p.add(new Chunk(label + ": ", LABEL_FONT));
        p.add(new Chunk(value != null ? value : "—", VALUE_FONT));
        return p;
    }

    private Paragraph createSpacing(float points) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(points);
        return p;
    }

    private String buildCustomerAddress(Customer customer) {
        StringBuilder address = new StringBuilder();
        if (customer.getAddressLine1() != null) address.append(customer.getAddressLine1()).append("\n");
        if (customer.getCity() != null) address.append(customer.getCity());
        if (customer.getState() != null) address.append(", ").append(customer.getState());
        if (customer.getPostalCode() != null) address.append(" - ").append(customer.getPostalCode());
        if (customer.getCountry() != null) address.append("\n").append(customer.getCountry());
        return address.toString();
    }

    private String formatCurrency(Long amountMinor, String currencyCode) {
        if (amountMinor == null) return "—";
        double amount = amountMinor / 100.0;

        try {
            Locale locale;
            if ("INR".equals(currencyCode)) locale = Locale.forLanguageTag("en-IN");
            else if ("GBP".equals(currencyCode)) locale = Locale.UK;
            else if ("USD".equals(currencyCode)) locale = Locale.US;
            else locale = Locale.US;

            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
            fmt.setCurrency(Currency.getInstance(currencyCode));
            return fmt.format(amount);
        } catch (Exception e) {
            return currencyCode + " " + String.format("%.2f", amount);
        }
    }

    private String getInvoiceTypeLabel(Status status) {
        if (status == Status.PAID) return "TAX INVOICE";
        if (status == Status.VOID || status == Status.CANCELED) return "VOID INVOICE";
        return "PROFORMA INVOICE";
    }

    private String formatBillingReason(String reason) {
        if (reason == null) return "—";
        return reason.replace("_", " ").substring(0, 1).toUpperCase()
                + reason.replace("_", " ").substring(1).toLowerCase();
    }
}
