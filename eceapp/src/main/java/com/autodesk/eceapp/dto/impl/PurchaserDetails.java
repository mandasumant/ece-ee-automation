package com.autodesk.eceapp.dto.impl;

import com.autodesk.eceapp.dto.IPurchaserDetails;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PurchaserDetails implements IPurchaserDetails {

  private String email;
  private String companyName;
  private String firstName;
  private String lastName;
  private String address;
  private String preferredLanguage;
  private String phone;

  public PurchaserDetails(String email, String companyName, String firstName, String lastName, String address,
      String preferredLanguage,
      String phone) {
    this.email = email;
    this.companyName = companyName;
    this.firstName = firstName;
    this.lastName = lastName;
    this.address = address;
    this.preferredLanguage = preferredLanguage;
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getAddress() {
    return address;
  }

  public String getPreferredLanguage() {
    return preferredLanguage;
  }

  public String getPhone() {
    return phone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PurchaserDetails that = (PurchaserDetails) o;

    return new EqualsBuilder().append(email, that.email).append(companyName, that.companyName)
        .append(firstName, that.firstName).append(lastName, that.lastName)
        .append(preferredLanguage, that.preferredLanguage).append(phone, that.phone).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(email).append(companyName).append(firstName).append(lastName)
        .append(preferredLanguage).append(phone).toHashCode();
  }
}
