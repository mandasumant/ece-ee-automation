package com.autodesk.eceapp.utilities;

import com.autodesk.testinghub.core.utils.Util;
import io.restassured.path.json.JsonPath;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;

public class Address {

  public String company;
  public String addressLine1;
  public String addressLine2 = "";
  public String addressLine3 = "";
  public String city;
  public String postalCode;
  public String phoneNumber;
  public String country;
  public String countryCode;
  public String province = "";
  public String provinceName;

  public Address(String localeMap) {
    Util.printInfo("Address being used: " + localeMap);
    String[] billingAddress = localeMap.split("@");

    this.company = getRandomCompanyName();
    this.addressLine1 = billingAddress[1];
    this.city = billingAddress[2];
    this.postalCode = billingAddress[3];
    this.phoneNumber = getRandomMobileNumber();

    if (billingAddress.length >= 6) {
      this.country = billingAddress[5];
    }

    if (billingAddress.length == 7) {
      this.province = billingAddress[6];
    }

    try {
      JsonPath jp = ResourceFileLoader.getCountryCodesJson();
      countryCode = jp.getString("find { it.Name == '" + this.country + "' }.Code");
    } catch (FileNotFoundException e) {
      Util.printError("Failed to load country codes file: " + e.getMessage());
    }

    if (!this.province.equals("")) {
      try {
        JsonPath jp = ResourceFileLoader.getProvincesJson();
        provinceName = jp.get(MessageFormat.format("\"{0}\"", this.province));
      } catch (FileNotFoundException e) {
        Util.printError("Failed to load country codes file: " + e.getMessage());
      }
    }
  }

  private String getRandomMobileNumber() {
    Random rnd = new Random();
    long number = rnd.nextInt(999999999);
    number = number + 1000000000;

    return String.format("%09d", number);
  }

  private String getRandomCompanyName() {
    return new RandomStringUtils().random(8, true, false).toUpperCase();
  }
}
