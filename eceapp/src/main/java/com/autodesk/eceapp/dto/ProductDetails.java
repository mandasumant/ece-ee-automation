package com.autodesk.eceapp.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ProductDetails {

  private String productName;
  private String term;
  private int quantity;

  public ProductDetails(String productName, String term, int quantity) {
    this.productName = productName;
    this.term = term;
    this.quantity = quantity;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getTerm() {
    return term;
  }

  public void setTerm(String term) {
    this.term = term;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProductDetails that = (ProductDetails) o;

    return new EqualsBuilder().append(quantity, that.quantity)
        .append(productName, that.productName).append(term, that.term).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(productName).append(term).append(quantity).toHashCode();
  }
}