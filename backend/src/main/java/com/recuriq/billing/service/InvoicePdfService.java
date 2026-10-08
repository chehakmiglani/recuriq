package com.recuriq.billing.service;

import com.recuriq.billing.entity.Invoice;

/**
 * Service for generating invoice PDFs.
 */
public interface InvoicePdfService {

    /**
     * Generate a PDF byte array for the given invoice.
     *
     * @param invoice the invoice entity (with customer, subscription, plan loaded)
     * @return PDF file as byte array
     */
    byte[] generatePdf(Invoice invoice);
}
