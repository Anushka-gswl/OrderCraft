package com.hcl.Order_Management_System.DTO;

import java.time.LocalDate;

public class InvoiceRequest {

    /** Tax rate as a percentage, e.g. 18.0 means 18%. Defaults to 0 if null. */
    private Double taxRate;

    /** Optional due date for the invoice. */
    private LocalDate dueDate;

    /** Optional free-text notes to appear on the invoice. */
    private String notes;

    public InvoiceRequest() {
    }

    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
