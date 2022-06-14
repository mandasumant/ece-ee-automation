package com.autodesk.ece.utilities;


import io.restassured.path.json.JsonPath;
import java.io.File;
import java.util.Objects;
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
  public String province;

  public Address(String localeMap) {
    String[] billingAddress = localeMap.split("@");
    this.company = getRandomCompanyName();
    this.addressLine1 = billingAddress[1];
    this.city = billingAddress[2];
    this.postalCode = billingAddress[3];
    this.phoneNumber = getRandomMobileNumber();
    this.country = billingAddress[5];

    if (billingAddress.length == 7) {
      this.province = billingAddress[6];
    } else {
      this.province = "";
    }

    ClassLoader classLoader = this.getClass().getClassLoader();
    String jsonFilePath = Objects.requireNonNull(
        classLoader.getResource("ece/payload/countryCodes.json")).getPath();
    File jsonFile = new File(jsonFilePath);
    JsonPath jp = new JsonPath(jsonFile);
    countryCode = jp.getString("find { it.Name == '" + this.country + "' }.Code");
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
