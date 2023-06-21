package com.autodesk.eceapp.dto;

public interface IPayerDetails {
  String getPayerCsn();
  String getEmail();
  String getAddress();
  String getCompleteAddress();
  String getCity();
  String getStateProvinceCode();
  String getPostalCode();
  String getCountryCode();
  String getCompanyName();
  String getExistingPayer();
  String getPhone();
  boolean isPayerSameAsPurchaser();
}
