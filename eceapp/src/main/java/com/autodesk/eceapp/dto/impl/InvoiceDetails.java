package com.autodesk.eceapp.dto.impl;

import com.autodesk.eceapp.dto.IInvoiceDetails;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class InvoiceDetails implements IInvoiceDetails {

  private String invoiceNotes;
  private String invoicePoNumber;

  public InvoiceDetails(String invoiceNotes, String invoicePoNumber) {
    this.invoiceNotes = invoiceNotes;
    this.invoicePoNumber = invoicePoNumber;
  }

  public String getInvoiceNotes() {
    return invoiceNotes;
  }

  public void setInvoiceNotes(String invoiceNotes) {
    this.invoiceNotes = invoiceNotes;
  }

  public String getInvoicePoNumber() {
    return invoicePoNumber;
  }

  public void setInvoicePoNumber(String invoicePoNumber) {
    this.invoicePoNumber = invoicePoNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InvoiceDetails that = (InvoiceDetails) o;

    return new EqualsBuilder().append(invoiceNotes, that.invoiceNotes)
        .append(invoicePoNumber, that.invoicePoNumber).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(invoiceNotes).append(invoicePoNumber).toHashCode();
  }

  @Override
  public String toString() {
    return "InvoiceDetails{" +
        "invoiceNotes='" + invoiceNotes + '\'' +
        ", invoicePoNumber='" + invoicePoNumber + '\'' +
        '}';
  }
}
