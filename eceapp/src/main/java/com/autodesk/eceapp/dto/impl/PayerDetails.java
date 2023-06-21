package com.autodesk.eceapp.dto.impl;

import com.autodesk.eceapp.dto.IPayerDetails;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PayerDetails implements IPayerDetails {
  private String payerCsn;
  private String email;
  private String address;
  private String completeAddress;
  private String city;
  private String stateProvinceCode;
  private String postalCode;
  private String countryCode;
  private String companyName;
  private String existingPayer;
  private String phone;
  private boolean payerSameAsPurchaser;

  public PayerDetails(String payerCsn, String email, String address, String completeAddress, String city,
      String stateProvinceCode, String postalCode, String countryCode, String companyName, String existingPayer,
      String phone, boolean payerSameAsPurchaser) {
    this.payerCsn = payerCsn;
    this.email = email;
    this.address = address;
    this.completeAddress = completeAddress;
    this.city = city;
    this.stateProvinceCode = stateProvinceCode;
    this.postalCode = postalCode;
    this.countryCode = countryCode;
    this.companyName = companyName;
    this.existingPayer = existingPayer;
    this.phone = phone;
    this.payerSameAsPurchaser = payerSameAsPurchaser;
  }

  public String getPayerCsn() {
    return payerCsn;
  }

  public void setPayerCsn(String payerCsn) {
    this.payerCsn = payerCsn;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCompleteAddress() {
    return completeAddress;
  }

  public void setCompleteAddress(String completeAddress) {
    this.completeAddress = completeAddress;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getStateProvinceCode() {
    return stateProvinceCode;
  }

  public void setStateProvinceCode(String stateProvinceCode) {
    this.stateProvinceCode = stateProvinceCode;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getExistingPayer() {
    return existingPayer;
  }

  public void setExistingPayer(String existingPayer) {
    this.existingPayer = existingPayer;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isPayerSameAsPurchaser() {
    return payerSameAsPurchaser;
  }

  public void setPayerSameAsPurchaser(boolean payerSameAsPurchaser) {
    this.payerSameAsPurchaser = payerSameAsPurchaser;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PayerDetails that = (PayerDetails) o;

    return new EqualsBuilder().append(payerSameAsPurchaser, that.payerSameAsPurchaser)
        .append(payerCsn, that.payerCsn).append(email, that.email).append(address, that.address)
        .append(completeAddress, that.completeAddress).append(city, that.city)
        .append(stateProvinceCode, that.stateProvinceCode).append(postalCode, that.postalCode)
        .append(countryCode, that.countryCode).append(companyName, that.companyName)
        .append(existingPayer, that.existingPayer).append(phone, that.phone).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(payerCsn).append(email).append(address).append(completeAddress)
        .append(city)
        .append(stateProvinceCode).append(postalCode).append(countryCode).append(companyName).append(existingPayer)
        .append(phone).append(payerSameAsPurchaser).toHashCode();
  }

  @Override
  public String toString() {
    return "PayerDetails{" +
        "payerCsn='" + payerCsn + '\'' +
        ", email='" + email + '\'' +
        ", address='" + address + '\'' +
        ", completeAddress='" + completeAddress + '\'' +
        ", city='" + city + '\'' +
        ", stateProvinceCode='" + stateProvinceCode + '\'' +
        ", postalCode='" + postalCode + '\'' +
        ", countryCode='" + countryCode + '\'' +
        ", companyName='" + companyName + '\'' +
        ", existingPayer='" + existingPayer + '\'' +
        ", phone='" + phone + '\'' +
        ", payerSameAsPurchaser=" + payerSameAsPurchaser +
        '}';
  }
}
