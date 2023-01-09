package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.testinghub.core.utils.Util;
import com.google.gson.Gson;
import io.restassured.response.Response;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class DatastoreClient {

  public OrderData queueOrder(NewQuoteOrder orderToQueue) {
    Gson gson = new Gson();
    String body = gson.toJson(orderToQueue);
    Util.printInfo("Order body: " + body);
    Response response = given()
        .header("Content-Type", "application/json")
        .body(body)
        .when()
        .post("https://615rpm126i.execute-api.us-west-2.amazonaws.com/queue")
        .then()
        .extract().response();
    Util.printInfo("Order response: " + response.getBody().asString());
    return gson.fromJson(response.getBody().asString(), OrderData.class);
  }

  public static @Builder @Data
  class NewQuoteOrder {

    String name;
    String tenant;
    String environment;
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
}
