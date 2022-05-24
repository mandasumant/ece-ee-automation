package com.autodesk.ece.utilities;

import java.util.Random;

public class Address {
  public String company;
  public String addressLine1;
  public String city;
  public String postalCode;
  public String phoneNumber;
  public String country;
  public String countryCode;
  public String province;

  public Address(String localeMap, String countryCode) {
    String[] billingAddress = localeMap.split("@");
    this.company = billingAddress[0];
    this.addressLine1 = billingAddress[1];
    this.city = billingAddress[2];
    this.postalCode = billingAddress[3];
    this.phoneNumber = getRandomMobileNumber();
    this.country = billingAddress[5];
    this.countryCode = countryCode;

    if (billingAddress.length == 7) {
      this.province = billingAddress[6];
    }
  }

  public Address(String localeMap) {
    this(localeMap, "");
  }

  private String getRandomMobileNumber() {
    Random rnd = new Random();
    long number = rnd.nextInt(999999999);
    number = number + 1000000000;

    return String.format("%09d", number);
  }
}
