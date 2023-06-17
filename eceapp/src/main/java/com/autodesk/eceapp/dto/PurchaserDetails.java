package com.autodesk.eceapp.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PurchaserDetails {

  private String email;
  private String name;
  private String firstName;
  private String lastName;
  private String preferredLanguage;
  private String phone;

  public PurchaserDetails(String email, String name, String firstName, String lastName, String preferredLanguage,
      String phone) {
    this.email = email;
    this.name = name;
    this.firstName = firstName;
    this.lastName = lastName;
    this.preferredLanguage = preferredLanguage;
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
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

    return new EqualsBuilder().append(email, that.email).append(name, that.name)
        .append(firstName, that.firstName).append(lastName, that.lastName)
        .append(preferredLanguage, that.preferredLanguage).append(phone, that.phone).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(email).append(name).append(firstName).append(lastName)
        .append(preferredLanguage).append(phone).toHashCode();
  }
}
