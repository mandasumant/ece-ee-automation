package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.testbase.TestinghubUtil;
import com.autodesk.testinghub.core.utils.Util;
import com.google.gson.Gson;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class DatastoreClient {

  static final String TENANT = "PlatformAutomation";
  static final String DS_ENDPOINT = "615rpm126i.execute-api.us-west-2.amazonaws.com";
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
    try {
      response = given()
          .when()
          .get(new URI("https", "//" + DS_ENDPOINT + "/grab?" + filters.toURLParameters(),
              null))
          .then()
          .extract().response();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    Util.printInfo("Grab response: " + response.getBody().asString());
    return gson.fromJson(response.getBody().asString(), OrderData.class);
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
    Integer orderNumber;
    String quoteId;
    String paymentType;
    String locale;
    String address;
  }

  public static class OrderData extends NewQuoteOrder {

    @Getter
    Integer id;
    String createdAt;
    String status;

    OrderData(String name, String tenant, String environment, String emailId, Integer orderNumber, String quoteId,
        String paymentType, String locale, String address) {
      super(name, tenant, environment, emailId, orderNumber, quoteId, paymentType, locale, address);
    }
  }

  public static @Builder class OrderFilters {

    @Builder.Default
    public String name = DatastoreClient.getTestName();
    @Builder.Default
    public String tenant = TENANT;
    @Builder.Default
    public String environment = GlobalConstants.getENV();
    public String paymentType;
    public String address;

    public String toURLParameters() {

      List<String> parameters = new LinkedList<>();

      for (Field field : OrderFilters.class.getFields()) {
        Object fieldValue = null;
        try {
          fieldValue = field.get(this);
          if (!Objects.isNull(fieldValue)) {

            parameters.add(field.getName() + "=" + ((String) fieldValue));
          }
        } catch (IllegalAccessException e) {
          Util.printWarning("Failed to encode field: " + field.getName());
        }
      }

      return String.join("&", parameters);
    }
  }
}
