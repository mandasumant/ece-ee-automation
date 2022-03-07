package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.bicapiModel.UpdateNextBilling;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.testng.Assert;

public class PelicanTestBase {

  private String productName = "";

  public PelicanTestBase() {
    Util.PrintInfo("PelicanTestBase from ece");
  }

  @SuppressWarnings("unchecked")
  @Step("Create refund order" + GlobalConstants.TAG_TESTINGHUB)
  public static Response createRefundOrder(String baseUrl, Map<String, String> header) {

    JSONObject requestParams = new JSONObject();
    requestParams.put("requestedBy", "462719");
    requestParams.put("event", "REFUND_REQUEST");
    requestParams.put("operation", "ORDER_UPDATED");
    requestParams.put("subOperation", "REFUND_REQUEST_PROCESSED");
    Util.printInfo("Refund URL: " + baseUrl);
    Util.printInfo("Request Body: " + requestParams.toJSONString());
    Response response = RestAssured.given().headers(header).body(requestParams.toJSONString()).post(baseUrl);
    Util.printInfo(response.asString());

    int statusCode = response.getStatusCode();
    if (statusCode != 201) {
      String result = response.getBody().asString();
      Util.PrintInfo(BICECEConstants.RESULT + result);
      JsonPath js = new JsonPath(result);
      String message = js.get("message");
      AssertUtils.fail("Error while Refunding the Order -" + message);
    }
    return response;
  }

  public static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte aByte : bytes) {
      result.append(String.format("%02x", aByte));
    }
    return result.toString();
  }

  public static Response getRestResponse(String baseUrl, Map<String, String> header,String requestJson) {
    Response response = null;
    try {
      Util.printInfo("Hitting the URL = " + baseUrl);
      RestAssured.baseURI = baseUrl;
      for (int i = 0; i <= 2; i++) {
        if (requestJson != null) {
          response = given().headers(header).body(requestJson).when().post();
        } else {
          response = given().headers(header).when().get();
        }

        //few Pelican API return 302 for POST/PUT hence 399
        if(response.getStatusCode() < 399) {
          break;
        }
        Util.sleep(3000);
      }
      String result = response.getBody().asString();
      Util.printInfo("results from the url-" + result);
      if (response.getStatusCode() != 200) {
        Assert.assertTrue(false,
            "Response code must be 200 but the API return " + response.getStatusCode());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }

  @Step("Order Service : Order Capture" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getSubscriptionById(HashMap<String, String> data) {
    String subscriptionByIdUrl = data.get("getSubscriptionByIdUrl");

    subscriptionByIdUrl = addTokenInResourceUrl(subscriptionByIdUrl,
        data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));
    HashMap<String, String> results = new HashMap<String, String>();
    String baseURL = data.get("pelican_BaseUrl");
    Util.printInfo("getPriceDetails baseURL : " + subscriptionByIdUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);
    String X_E2_AppFamilyId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);

    String Content_Type = BICECEConstants.APPLICATION_VNDAPI_JSON;

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, hmacSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, X_E2_PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, X_E2_AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, X_E2_HMAC_Timestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, Content_Type);

    Response response = getRestResponse(subscriptionByIdUrl, header, null);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);
    JsonPath js = new JsonPath(result);
    Util.printInfo("js is:" + js);

    try {
      results.put("response_priceID",
          js.get("data.lastBillingInfo.purchaseOrderId") != null ? Integer
              .toString(js.get("data.lastBillingInfo.purchaseOrderId")) : null);
      results.put("response_nextBillingDate", js.get("data.nextBillingDate"));
      results.put("response_offeringType",js.get("data.offeringType"));
      results.put("response_subscriptionQuantity", Integer.toString(js.get("data.quantity")));
      results.put("response_quantityToReduce", Integer.toString(js.get("data.quantityToReduce")));
      results.put("response_offeringExternalKey", js.get("data.offeringExternalKey"));
      results.put("response_nextBillingUnitPrice", js.get("data.nextBillingInfo.unitPrice"));
      results.put("response_nextBillingChargeAmount", js.get("data.nextBillingInfo.chargeAmount"));
      results.put("response_endDate", js.get("data.endDate"));
      results.put("response_autoRenewEnabled", Boolean.toString(js.get("data.autoRenewEnabled")));
      results.put("response_expirationDate", js.get("data.expirationDate"));
      results.put("response_currentBillingPriceId",
          js.get(BICECEConstants.DATA_PRICE_ID) != null ? Integer
              .toString(js.get(BICECEConstants.DATA_PRICE_ID)) : null);
      results.put("response_nextBillingPriceId",
          js.get("data.nextBillingInfo.nextBillingPriceId") != null ? Integer
              .toString(js.get("data.nextBillingInfo.nextBillingPriceId")) : null);
      results.put("response_switchTermPriceId", js.get("data.switchTermPriceId") != null ? Integer
          .toString(js.get("data.switchTermPriceId")) : null);
      results.put("response_status", js.get("data.status"));

    } catch (Exception e) {

      e.printStackTrace();
    }
    return results;
  }

  @Step("Update next billing cycle with before date " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> forwardNextBillingCycleForRenewal(HashMap<String, String> data) {

    String getPurchaseOrderDetailsUrl = data.get("getSubscriptionByIdUrl");
    getPurchaseOrderDetailsUrl = addTokenInResourceUrl(getPurchaseOrderDetailsUrl,
        data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));
    Util.printInfo("getPriceDetails baseURL : " + getPurchaseOrderDetailsUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);
    String X_E2_AppFamilyId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);

    String Content_Type = BICECEConstants.APPLICATION_JSON;
    String accept = BICECEConstants.APPLICATION_VNDAPI_JSON;
    HashMap<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, hmacSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, X_E2_PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, X_E2_AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, X_E2_HMAC_Timestamp);
    header.put("X-Request-Ref", UUID.randomUUID().toString());
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, accept);

    String contractStartDate;
    if (data.containsKey("desiredBillingDate")) {
      contractStartDate = data.get("desiredBillingDate");
    } else {
      contractStartDate = Util.customDate("MM/dd/yyyy", 0, -5, 0) + " 20:13:28 UTC";
    }
    String path = Util.getCorePayloadPath() + "BIC_Update_NextBilling.json";
    File rawPayload = new File(path);
    UpdateNextBilling nextBillingJson;
    ObjectMapper om = new ObjectMapper();
    String inputPayload = "";
    try {
      nextBillingJson = om.readValue(rawPayload, UpdateNextBilling.class);
      nextBillingJson.getData().setNextBillingDate(contractStartDate);
      inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(nextBillingJson);
      Util.PrintInfo(BICECEConstants.PAYLOAD_AUTH + inputPayload + "\n");
    } catch (IOException e1) {
      e1.printStackTrace();
      AssertUtils.fail("Failed to generate SPOC Authorization Token" + e1.getMessage());
    }
    return patchRestResponse(getPurchaseOrderDetailsUrl, header, inputPayload);
  }

  @Step("Sending Patch Response")
  public HashMap<String, String> patchRestResponse(String baseUrl, HashMap<String, String> header,
      String body) {
    Util.printInfo("Hitting the URL = " + baseUrl);
    HashMap<String, String> results = new HashMap<String, String>();
    try {
      Response response = given().headers(header).and().body(body).when().patch(baseUrl).then()
          .extract()
          .response();

      int responseStatusCode = response.getStatusCode();
      Util.printInfo("Response code : " + responseStatusCode);
      if (responseStatusCode != 200) {
        AssertUtils.assertTrue(false,
            "Response code must be 200 but the API return " + responseStatusCode);
      }

      String result = response.getBody().asString();
      Util.printInfo("" + result);
      JsonPath jp = new JsonPath(result);
      String purchaseOrderId = jp.get("data.purchaseOrderId").toString();
      String exportControlStatus = jp.get("data.exportControlStatus").toString();
      String priceId = jp.get(BICECEConstants.DATA_PRICE_ID).toString();
      String nextBillingDate = jp.get("data.nextBillingInfo.nextBillingDate").toString();

      results.put("purchaseOrderId", purchaseOrderId);
      results.put("exportControlStatus", exportControlStatus);
      results.put("priceId", priceId);
      results.put("nextBillingDate", nextBillingDate);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return results;
  }

  @SuppressWarnings("unused")
  @Step("Create invoice for PO " + GlobalConstants.TAG_TESTINGHUB)
  public void postInvoicePelicanAPI(HashMap<String, String> data) {
    String invoicePelicanAPIUrl = data.get("postInvoicePelicanAPIUrl");
    HashMap<String, String> results = new HashMap<String, String>();
    Util.printInfo("invoicePelicanAPIUrl  : " + invoicePelicanAPIUrl);
    String Content_Type = BICECEConstants.APPLICATION_JSON;

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);

    Response response = getRestResponse(invoicePelicanAPIUrl, header, "{}");
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);
    JsonPath js = new JsonPath(result);
    Util.printInfo("js is:" + js);
  }

  @Step("Order Service finance reports API " + GlobalConstants.TAG_TESTINGHUB)
  public String postReportsFinancePelicanAPI(HashMap<String, String> data) {
    String postReportsFinancePelicanAPIUrl = data.get("postReportsFinancePelicanAPIUrl");

    //Generate the input JSON
    JSONArray orderArray = new JSONArray();
    orderArray.put(data.get(BICConstants.orderNumber));

    JSONArray buyerArray = new JSONArray();
    buyerArray.put(data.get(BICECEConstants.BUYER_EXTERNAL_KEY));

    JSONObject requestParams = new JSONObject();
    requestParams.put("orderIds", orderArray);
    requestParams.put("buyerExternalKeys", buyerArray);

    JSONObject filters = new JSONObject();
    filters.put("filters", requestParams);

    Util.printInfo("Finance Reports Request Body: " + filters.toJSONString());

    String requestJson = filters.toJSONString();
    data.put("pelican_postReportsFinancePelicanAPIUrl", postReportsFinancePelicanAPIUrl);
    HashMap<String, String> results = new HashMap<String, String>();
    Util.printInfo("postReportsFinancePelicanAPIUrl : " + postReportsFinancePelicanAPIUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);
    String X_E2_AppFamilyId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);

    String Content_Type = BICECEConstants.APPLICATION_JSON;

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, hmacSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, X_E2_PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, X_E2_AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, X_E2_HMAC_Timestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);

    Response response = getRestResponse(postReportsFinancePelicanAPIUrl, header, requestJson);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);

    return result;
  }

  @Step("Get Pelican Response" + GlobalConstants.TAG_TESTINGHUB)
  public String getPurchaseOrder(HashMap<String, String> data) {
    String getPurchaseOrderDetailsUrl = data.get("getPurchaseOrderDetailsUrl");
    productName = data.get("productName");

    //Generate the input JSON
    JSONObject filters = new JSONObject();
    JSONArray array = new JSONArray();
    if (data.get(BICConstants.orderNumber) != null) {
      array.put(data.get(BICConstants.orderNumber));
      JSONObject orders = new JSONObject();
      orders.put("orderIds", array);
      filters.put("filters", orders);
    } else {
      JSONObject emailId = new JSONObject();
      emailId.put("email", data.get(BICECEConstants.emailid));
      filters.put("filters", emailId);
    }

    String requestJson = filters.toJSONString();
    data.put("pelican_getPurchaseOrderDetailsUrl", getPurchaseOrderDetailsUrl);
    HashMap<String, String> results = new HashMap<String, String>();
    Util.printInfo("getPurchaseOrderDetailsUrl : " + getPurchaseOrderDetailsUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);
    String X_E2_AppFamilyId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);

    String Content_Type = BICECEConstants.APPLICATION_JSON;

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, hmacSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, X_E2_PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, X_E2_AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, X_E2_HMAC_Timestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);

    Response response = getRestResponse(getPurchaseOrderDetailsUrl, header, requestJson);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);

    return result;
  }

  @Step("Create Refund Order" + GlobalConstants.TAG_TESTINGHUB)
  public void createRefundOrder(HashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<String, String>();

    String purchaseOrderDetailsUrl = data.get("putPelicanRefundOrderUrl");
    String getPurchaseOrderDetailsUrl = addTokenInResourceUrl(purchaseOrderDetailsUrl,
        data.get(BICConstants.orderNumber));
    Util.printInfo("putPelicanRefund details Url : " + getPurchaseOrderDetailsUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);
    String X_E2_AppFamilyId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);

    // String Content_Type="application/x-www-form-urlencoded; charset=UTF-8";
    String Content_Type = BICECEConstants.APPLICATION_JSON;

    Map<String, String> header = new HashMap<>();
    header.put("x-e2-hmac-signature", hmacSignature);
    header.put("x-e2-partnerid", X_E2_PartnerId);
    header.put("x-e2-appfamilyid", X_E2_AppFamilyId);
    header.put("x-e2-hmac-timestamp", X_E2_HMAC_Timestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, Content_Type);

    Response response = createRefundOrder(getPurchaseOrderDetailsUrl, header);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);
  }

  private String getPriceByPriceIdSignature(HashMap<String, String> data) {
    String signature = "";
    String timestamp = "";
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      String terceS = data.get("getPriceDetails_terceS_ssap");
      SecretKeySpec keySpec = new SecretKeySpec(terceS.getBytes(), "HmacSHA256");
      mac.init(keySpec);

      String appFamilyId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);
      String partnerId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);

      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      timestamp = String.valueOf(cal.getTimeInMillis() / 1000);

      String message = new StringBuilder().append(partnerId).append(appFamilyId).append(timestamp)
          .toString();

      byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
      // signature = getSHA256Hash(secret, message);
      signature = hex(signatureBytes);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return signature + "::" + timestamp;
  }

  @Step("Subscription : Getting purchase order details from Order Service " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getPurchaseOrderDetails(String purchaseOrderAPIresponse) {
    HashMap<String, String> results = new HashMap<>();
    JsonPath jp = new JsonPath(purchaseOrderAPIresponse);
    try {
      results.put("getPOReponse_origin", jp.get("content[0].origin").toString());
      results.put("getPOReponse_orderId", jp.get("content[0].id").toString());
      results
          .put("getPOReponse_storeExternalKey", jp.get("content[0].storeExternalKey").toString());
      results
          .put(BICECEConstants.BUYER_EXTERNAL_KEY, jp.get("content[0].buyerExternalKey").toString());
      results.put("getPOReponse_storedPaymentProfileId",
          jp.get("content[0].payments[0].paymentProfileId").toString());
      results.put("getPOReponse_fulfillmentStatus",
          jp.get("content[0].lineItems[0].fulfillmentStatus").toString());
      results.put("getPOReponse_orderState", jp.get("content[0].orderState").toString());
      results.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
          jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionId").toString());
      results.put("getPOReponse_subscriptionPeriodStartDate",
          jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionPeriodStartDate").toString());
      results.put("getPOReponse_subscriptionPeriodEndDate",
          jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionPeriodEndDate").toString());
      results.put("getPOReponse_fulfillmentDate",
          jp.get("content[0].lineItems[0].fulfillmentDate").toString());
      results.put("getPOResponse_promotionDiscount",
          jp.get("content[0].lineItems[0].lineItemTotals.promotionDiscount").toString());
      results.put("getPOReponse_paymentProcessor",
          jp.get("content[0].payments[0].paymentProcessor").toString());
      results.put("getPOReponse_last4Digits",
          jp.get("content[0].billingInfo.lastDigits"));
      results.put("getPOReponse_taxCode",
          jp.get("content[0].lineItems[0].additionalFees[0].feeCollectorExternalKey").toString());
      results.put("getPOReponse_oxygenID", jp.get("content[0].buyerExternalKey").toString());

    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to get Purchase Order Details for Order Service");
    }
    return results;
  }

  public String addTokenInResourceUrl(String resourceUrl, String tokenString) {
    return resourceUrl.replace("passtoken", tokenString);
  }

  /**
   * Retry a pelican call upto 3 times to find a subscription id
   *
   * @param results - Data hashmap
   * @param silent  - Whether a failure 3 times in a row causes the testcase to fail
   * @return - Pelican response
   */
  public String retryGetPurchaseOrder(HashMap<String, String> results, boolean silent) {
    String response = "";
    boolean subscriptionIdFound = false;
    for (int i = 1; i < 4; i++) {
      response = getPurchaseOrder(results);
      int intIndex = response.indexOf("subscriptionId");
      if (intIndex == -1) {
        Util.printInfo("SubscriptionId not found. Retry #" + i);
        Util.sleep(300000);
      } else {
        Util.printInfo("Found subscriptionId at index " + intIndex);
        subscriptionIdFound = true;
        break;
      }
    }
    if (!subscriptionIdFound && !silent) {
      AssertUtils.fail("Failed: Could not find the subscription id for Order # " + results.get(
          BICConstants.orderNumber) + ".");
    }
    return response;
  }

  /**
   * Retry a pelican call up to 3 times to find a subscription id
   *
   * @param results - Data hashmap
   * @return - Pelican response
   */
  public String retryGetPurchaseOrder(HashMap<String, String> results) {
    return retryGetPurchaseOrder(results, false);
  }
}
