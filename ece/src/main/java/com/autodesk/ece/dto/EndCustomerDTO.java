package com.autodesk.ece.dto;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.utilities.Address;
import java.util.LinkedHashMap;
import lombok.Data;

public @Data
class EndCustomerDTO {
  private String name;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String city;
  private String stateProvinceCode;
  private String postalCode;
  private String countryCode;
  private Boolean isIndividual = false;

  public EndCustomerDTO (LinkedHashMap<String, String> data, Address address) {
    this.name = data.get(BICECEConstants.FIRSTNAME) + " " +data.get(BICECEConstants.LASTNAME);
    this.addressLine1 = address.addressLine1;
    this.addressLine2 = address.addressLine2;
    this.addressLine3 = address.addressLine3;
    this.city = address.city;
    this.stateProvinceCode = address.province;
    this.postalCode = address.postalCode;
    this.countryCode = address.countryCode;
  }
}
