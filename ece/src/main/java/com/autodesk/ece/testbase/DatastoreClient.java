package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.eseapp.testbase.TestinghubUtil;
import com.autodesk.testinghub.core.utils.Util;
import com.google.gson.Gson;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class DatastoreClient {

  static final String TENANT = "PlatformAutomation";
  static final String DS_ENDPOINT = "615rpm126i.execute-api.us-west-2.amazonaws.com";
  private static final String defaultLocale = "en_US";
  Gson gson = new Gson();

  public static String getTestName() {
    String displayName = System.getProperty("displayName");

    if (Objects.isNull(displayName)) {
      String transactionDetails = TestinghubUtil.getTransactionOutput();
      JsonPath js = new JsonPath(transactionDetails);
      return js.getString("name");
    }

    return displayName;
  }

  public static String getLocale() {
    String locale = System.getProperty(BICECEConstants.LOCALE);
    if (locale == null || locale.trim().isEmpty()) {
      return defaultLocale;
    }
    return locale;
  }

  public OrderData queueOrder(NewQuoteOrder orderToQueue) {
    String body = gson.toJson(orderToQueue);
    Util.printInfo("Order body: " + body);
    Response response = given()
        .header("Content-Type", "application/json")
        .body(body)
        .when()
        .post("https://" + DS_ENDPOINT + "/queue")
        .then()
        .extract().response();
    Util.printInfo("Order response: " + response.getBody().asString());
    return gson.fromJson(response.getBody().asString(), OrderData.class);
  }

  public OrderData grabOrder(OrderFilters filters) {
    Response response = null;
    String getUrl = "https://" + DS_ENDPOINT + "/grab?" + filters.toURLParameters();
    Util.printInfo("Requesting: " + getUrl);
    response = given()
        .when()
        .get(getUrl)
        .then()
        .extract().response();

    Util.printInfo("Grab response: " + response.getBody().asString());
    return gson.fromJson(response.getBody().asString(), OrderData.class);
  }

  public boolean completeOrder(int orderId) {
    CompleteRequest request = new CompleteRequest(orderId);
    String body = gson.toJson(request);
    Response response = given()
        .header("Content-Type", "application/json")
        .body(body)
        .when()
        .patch("https://" + DS_ENDPOINT + "/complete")
        .then()
        .extract().response();

    Util.printInfo("Complete response: " + response.getBody().asString());
    return gson.fromJson(response.getBody().asString(), Boolean.class);
  }

  public static @Builder
  @Data
  class NewQuoteOrder {

    @Builder.Default
    public String environment = GlobalConstants.getENV();
    @Builder.Default
    String name = DatastoreClient.getTestName();
    @Builder.Default
    String tenant = TENANT;
    String emailId;
    BigInteger orderNumber;
    String quoteId;
    String paymentType;
    @Builder.Default
    String locale = DatastoreClient.getLocale();
    String address;
    String expiry;
    String scenario;
  }

  public static class OrderData extends NewQuoteOrder {

    @Getter
    Integer id;
    String createdAt;
    String status;

    OrderData(String name, String tenant, String environment, String emailId, BigInteger orderNumber, String quoteId,
        String paymentType, String locale, String address, String expiry, String scenario) {
      super(name, tenant, environment, emailId, orderNumber, quoteId, paymentType, locale, address, expiry, scenario);
    }
  }

  public static @Builder class OrderFilters {

    public String name;
    @Builder.Default
    public String tenant = TENANT;
    @Builder.Default
    public String environment = GlobalConstants.getENV();
    public String paymentType;
    public String locale;
    public String address;
    public String scenario;

    public String toURLParameters() {

      List<String> parameters = new LinkedList<>();

      for (Field field : OrderFilters.class.getFields()) {
        Object fieldValue = null;
        try {
          fieldValue = field.get(this);
          if (!Objects.isNull(fieldValue)) {
            String encodedString = URLEncoder.encode((String) fieldValue, "UTF-8").replaceAll("\\+", "%20");
            parameters.add(field.getName() + "=" + encodedString);
          }
        } catch (IllegalAccessException | UnsupportedEncodingException e) {
          Util.printWarning("Failed to encode field: " + field.getName());
        }
      }

      return String.join("&", parameters);
    }
  }

  public static class CompleteRequest {

    public String tenant = TENANT;
    public int id;

    public CompleteRequest(int id) {
      this.id = id;
    }
  }
}
