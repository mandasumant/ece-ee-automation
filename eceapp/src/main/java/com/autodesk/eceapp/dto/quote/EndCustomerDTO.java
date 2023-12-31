package com.autodesk.eceapp.dto.quote;

import com.autodesk.eceapp.utilities.Address;
import lombok.Data;

public @Data
class EndCustomerDTO {

  private String accountCsn;
  private String name;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String city;
  private String stateProvinceCode;
  private String postalCode;
  private String countryCode;
  private Boolean isIndividual = false;

  public EndCustomerDTO(Address address) {
    this.name = address.company;
    this.addressLine1 = address.addressLine1;
    this.addressLine2 = address.addressLine2;
    this.addressLine3 = address.addressLine3;
    this.city = address.city;
    this.stateProvinceCode = address.province;
    this.postalCode = address.postalCode;
    this.countryCode = address.countryCode;
  }

  public EndCustomerDTO(String endCustomerCsn) {
    this.accountCsn = endCustomerCsn;
  }
}
