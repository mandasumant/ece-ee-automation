package com.autodesk.eceapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuoteDetails {
  private String purchaserFirstName;
  private String purchaserLastName;

  private String endCustomerName;
  private String endCustomerAccountCsn;
  private String endCustomerAddressLine1;
  private String endCustomerCity;
  private String endCustomerCountryCode;
  private String endCustomerPostalCode;
}
