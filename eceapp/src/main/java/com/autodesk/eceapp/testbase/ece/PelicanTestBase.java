package com.autodesk.eceapp.testbase.ece;

import static io.restassured.RestAssured.given;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.constants.UpdateSubscriptionConstants;
import com.autodesk.eceapp.dto.purchaseOrder.v4.LineItemDTO;
import com.autodesk.eceapp.dto.subscription.v4.BatchUpdateSubscriptionDTO;
import com.autodesk.eceapp.dto.subscription.v4.UpdateSubscriptionDTO;
import com.autodesk.eceapp.utilities.Address;
import com.autodesk.eceapp.utilities.PelicanRequestSigner;
import com.autodesk.eceapp.utilities.PelicanRequestSigner.PelicanSignature;
import com.autodesk.platformautomation.ApiClient;
import com.autodesk.platformautomation.ApiException;
import com.autodesk.platformautomation.Configuration;
import com.autodesk.platformautomation.bilinsmpelicansubscriptionv4.client.models.SubscriptionSuccessV4;
import com.autodesk.platformautomation.bmse2pelicansubscriptionv3.SubscriptionControllerApi;
import com.autodesk.platformautomation.bmse2pelicansubscriptionv3.models.SubscriptionSuccess;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.bicapiModel.UpdateNextBilling;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import com.autodesk.testinghub.eseapp.constants.EseCommonConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.Assert;


public class PelicanTestBase {

  private final PelicanRequestSigner requestSigner = new PelicanRequestSigner();

  public PelicanTestBase() {
    Util.PrintInfo("PelicanTestBase from ece");
  }

  @SuppressWarnings("unchecked")
  @Step("Create refund order" + GlobalConstants.TAG_TESTINGHUB)
  public static Response createRefundOrder(String baseUrl, Map<String, String> header, List<Integer> lineItemIdsList) {

    JSONObject requestParams = new JSONObject();
    requestParams.put("requestedBy", "462719");
    requestParams.put("event", "REFUND_REQUEST");
    requestParams.put("operation", "ORDER_UPDATED");
    requestParams.put("subOperation", "REFUND_REQUEST_PROCESSED");
    if (lineItemIdsList.size() > 0) {
      requestParams.put("lineItemIds", lineItemIdsList);
    }
    Util.printInfo("Refund URL: " + baseUrl);
    Util.printInfo("Request Body: " + requestParams.toJSONString());
    Response response = RestAssured.given().headers(header).body(requestParams.toJSONString()).post(baseUrl);
    Util.printInfo(BICECEConstants.RESULT + response.asString());

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

  public static Response getRestResponse(String baseUrl, Map<String, String> header, String requestJson) {
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
        if (response.getStatusCode() < 399) {
          break;
        }
        Util.sleep(3000);
      }
      String result = response.getBody().asString();
      Util.printInfo("results from the url-" + result);
      if (response.getStatusCode() != 200) {
        Assert.fail("Response code must be 200 but the API return " + response.getStatusCode());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }

  @Step("Not Payment call to Commerce" + GlobalConstants.TAG_TESTINGHUB)
  public static void commerceNotPaymentAPI(HashMap<String, String> data) {
    String access_token = getForgeToken(data);
    Map<String, String> requestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token);
      put("Content-Type", "application/json");
    }};
    JSONObject requestParams = new JSONObject();
    requestParams.put("event", "NONPAYMENT");

    Util.printInfo(
        "Commerce NOT Payment endpoint: " + data.get("commerceNotPaymentUrl") + data.get(BICConstants.orderNumber));
    Util.printInfo("Commerce NOT Payment call Body: " + requestParams.toJSONString());
    Util.printInfo("Headers: " + requestHeaders);

    Response response = given()
        .headers(requestHeaders)
        .body(requestParams.toJSONString())
        .put(data.get("commerceNotPaymentUrl") + data.get(BICConstants.orderNumber))
        .then().extract().response();

    int statusCode = response.getStatusCode();
    Util.PrintInfo("Commerce Not Payment Response HTTP Status Code: " + statusCode);

    if (statusCode != 200) {
      String result = response.getBody().asString();
      Assert.fail("Commerce Not Payment gave none HTTP.OK response. Status Code: " + result);
    }
  }

  public static String getForgeToken(HashMap<String, String> data) {
    Response response = RestAssured.given()
        .config(RestAssured.config()
            .encoderConfig(EncoderConfig.encoderConfig()
                .encodeContentTypeAs("x-www-form-urlencoded",
                    ContentType.URLENC)))
        .contentType("application/x-www-form-urlencoded; charset=UTF-8")
        .formParam("grant_type", "client_credentials")
        .formParam("client_id", data.get("pelicanForgeClientId"))
        .formParam("client_secret", ProtectedConfigFile.decrypt(data.get("pelicanForgeClientSecret")))
        .post("https://" + data.get("pelicanForgeHostName") + "/authentication/v1/authenticate");
    int statusCode = response.getStatusCode();
    Util.PrintInfo("Forge token generation response status: " + statusCode);
    return response.jsonPath().getString("access_token");
  }

  @Step("Order Service : Order Capture" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getSubscriptionById(HashMap<String, String> data) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(data.get("getPelicanBaseUrl"));
    SubscriptionControllerApi apiInstance = new SubscriptionControllerApi(defaultClient);
    PelicanSignature signature = requestSigner.generateSignature();

    String testId = data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID);
    Long id = Long.parseLong(testId);

    HashMap<String, String> results = new HashMap<String, String>();

    boolean success = false;
    int attempt = 0;

    while (!success) {
      Util.printInfo("Attempt: " + attempt);
      Util.sleep(3000);
      attempt++;
      if (attempt > 3) {
        AssertUtils.fail("Unable to get a successful response for SubscriptionControllerApi#findSubscriptionById.");
        break;
      }
      try {
        SubscriptionSuccess result = apiInstance.findSubscriptionById(signature.xE2PartnerId,
            signature.xE2AppFamilyId, signature.xE2HMACTimestamp, signature.xE2HMACSignature, id,
            signature.xRequestRef);
        Util.PrintInfo(BICECEConstants.RESULT + result);
        results.put("response_renewalOrderNo",
            result.getData().getLastBillingInfo().getPurchaseOrderId() != null ? String.valueOf(
                result.getData().getLastBillingInfo().getPurchaseOrderId()) : null);
        results.put("response_nextBillingDate", result.getData().getNextBillingDate());
        results.put("response_subscriptionQuantity",
            String.valueOf(result.getData().getQuantity()));
        results.put("response_quantityToReduce",
            String.valueOf(result.getData().getQuantityToReduce()));
        results.put("response_offeringExternalKey", result.getData().getOfferingExternalKey());
        results.put("response_nextBillingUnitPrice",
            result.getData().getNextBillingInfo() != null ? String.valueOf(
                result.getData().getNextBillingInfo().getUnitPrice()) : null);
        results.put("response_nextBillingChargeAmount",
            result.getData().getNextBillingInfo() != null ? String.valueOf(
                result.getData().getNextBillingInfo().getChargeAmount()) : null);
        results.put("response_endDate", result.getData().getEndDate());
        results.put("response_autoRenewEnabled",
            Boolean.toString(result.getData().getAutoRenewEnabled()));
        results.put("response_expirationDate", result.getData().getExpirationDate());
        results.put("response_currentBillingPriceId",
            String.valueOf(result.getData().getPriceId() != null ? result.getData().getPriceId() : null));
        results.put("response_nextBillingPriceId",
            result.getData().getNextBillingInfo() != null ? String.valueOf(
                result.getData().getNextBillingInfo().getNextBillingPriceId())
                : null);
        results.put("response_switchTermPriceId",
            result.getData().getSwitchTermPriceId() != null ? String.valueOf(
                result.getData().getSwitchTermPriceId()) : null);
        results.put("response_status", String.valueOf(result.getData().getStatus()));

        success = true;
      } catch (ApiException e) {
        Util.printError("Exception when calling SubscriptionControllerApi#findSubscriptionById");
        Util.printError("Status code: " + e.getCode());
        Util.printError("Reason: " + e.getResponseBody());
        Util.printError("Response headers: " + e.getResponseHeaders());
        e.printStackTrace();
      }
    }

    return results;
  }

  @Step("Update next billing cycle with before date " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> forwardNextBillingCycleForRenewal(HashMap<String, String> data) {
    //Temporary sleep until we pick up story to address the retry logic to wait for Order charge.
    Util.sleep(240000);

    String getPurchaseOrderDetailsUrl = data.get("getSubscriptionByIdUrl");
    getPurchaseOrderDetailsUrl = addTokenInResourceUrl(getPurchaseOrderDetailsUrl,
        data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));
    Util.printInfo("getPriceDetails baseURL : " + getPurchaseOrderDetailsUrl);

    PelicanSignature signature = requestSigner.generateSignature();

    String Content_Type = BICECEConstants.APPLICATION_JSON;
    String accept = BICECEConstants.APPLICATION_VNDAPI_JSON;
    HashMap<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, signature.xE2HMACSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, signature.xE2PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, signature.xE2AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, signature.xE2HMACTimestamp);
    header.put("X-Request-Ref", UUID.randomUUID().toString());
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, accept);

    String contractStartDate;
    if (data.containsKey("desiredBillingDate")) {
      contractStartDate = data.get("desiredBillingDate");
    } else {
      contractStartDate = Util.customDate("MM/dd/yyyy", 0, -5, 0) + " 20:13:28 UTC";
    }
    String path = EseCommonConstants.APP_PAYLOAD_PATH + "BIC_Update_NextBilling.json";
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

  /**
   * If subscription status is not passed in data then nextRenewalDate will be updated.
   * Otherwise depending on status dates are updated.
   * @param data
   * @return
   * @throws Exception
   */
  @Step("Update next renewal,termination,expiration dates " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> updateO2PSubscriptionDates(HashMap<String, String> data) {

    String getSubscriptionV4Url = data.get("getSubscriptionByIdV4Url");
    Util.printInfo("Subscription id " + data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));
    getSubscriptionV4Url = addTokenInResourceUrl(getSubscriptionV4Url,
            data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));
    Util.printInfo("Get SubscriptionV4 baseURL : " + getSubscriptionV4Url);

    String updatedDate;
    if (data.containsKey("desiredBillingDate")) {
      updatedDate = data.get("desiredBillingDate");
    } else {
      updatedDate = Util.customDate("MM/dd/yyyy", 0, -5, 0) + " 20:13:28 UTC";
    }

    String inputPayload = "";

    UpdateSubscriptionDTO.Data updateSubscriptionData = null;

    if (StringUtils.trimToNull(data.get(BICECEConstants.SUBSCRIPTION_STATUS)) != null) {
      if (data.get(BICECEConstants.SUBSCRIPTION_STATUS).equals(SubscriptionSuccessV4.StatusEnum.SUSPENDED.toString())) {
        updateSubscriptionData = UpdateSubscriptionDTO.Data.builder()
            .suspensionDate(updatedDate)
            .status(SubscriptionSuccessV4.StatusEnum.ACTIVE.toString()).build();
      } else if (data.get(BICECEConstants.SUBSCRIPTION_STATUS)
          .equals(SubscriptionSuccessV4.StatusEnum.TERMINATED.toString())) {
        updateSubscriptionData = UpdateSubscriptionDTO.Data.builder()
            .terminationDate(updatedDate)
            .status(SubscriptionSuccessV4.StatusEnum.ACTIVE.toString()).build();
      } else if (data.get(BICECEConstants.SUBSCRIPTION_STATUS)
          .equals(SubscriptionSuccessV4.StatusEnum.EXPIRED.toString())) {
        updateSubscriptionData = UpdateSubscriptionDTO.Data.builder()
            .expirationDate(updatedDate)
            .status(SubscriptionSuccessV4.StatusEnum.ACTIVE.toString()).build();
      }
    } else {
      //updating next billing date for default
      updateSubscriptionData = UpdateSubscriptionDTO.Data.builder()
          .nextRenewalDate(updatedDate)
          .status(SubscriptionSuccessV4.StatusEnum.ACTIVE.toString()).build();

      if (Optional.ofNullable(StringUtils.trimToNull(System.getProperty(BICECEConstants.IS_RENEWAL_QUOTE)))
          .map(Boolean::parseBoolean).orElse(false)) {
        updateSubscriptionData.setExpirationDate(Util.customDate("MM/dd/yyyy", 0, 20, 0) + " 20:13:28 UTC");
      }
    }
    UpdateSubscriptionDTO updateSubscriptionDTO = UpdateSubscriptionDTO.builder().data(updateSubscriptionData).
        meta(UpdateSubscriptionDTO.Meta.builder().build()).build();
    inputPayload = new Gson().toJson(updateSubscriptionDTO);

    Util.PrintInfo(BICECEConstants.PAYLOAD_AUTH + inputPayload + "\n");
    return patchRestResponse(getSubscriptionV4Url, getHeaderForUpdateSubscription(), inputPayload);
  }

  private HashMap<String, String> getHeaderForUpdateSubscription(){
    PelicanSignature signature = requestSigner.generateSignature();
    String accept = BICECEConstants.APPLICATION_JSON;
    HashMap<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, signature.xE2HMACSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, signature.xE2PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, signature.xE2AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, signature.xE2HMACTimestamp);
    header.put("X-Request-Ref", UUID.randomUUID().toString());
    header.put(BICECEConstants.CONTENT_TYPE, accept);
    header.put(BICECEConstants.ACCEPT, accept);
    return header;
  }

  @Step("Expire, terminate and suspend subscription" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> updateO2PSubscriptionStatus(HashMap<String, String> data) throws Exception {
    String updateSubscriptionV4BatchUrl = data.get("updateSubscriptionV4BatchUrl");
    String subscriptionId = data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID);
    Util.printInfo("Subscription id " + subscriptionId);
    Util.printInfo("Update SubscriptionV4 baseURL : " + updateSubscriptionV4BatchUrl);

    String body = "";
    BatchUpdateSubscriptionDTO.Meta subMeta = null;

      if (data.get(BICECEConstants.SUBSCRIPTION_STATUS) != null) {
        if (data.get(BICECEConstants.SUBSCRIPTION_STATUS).equals(SubscriptionSuccessV4.StatusEnum.SUSPENDED.toString())) {
          updateO2PSubscriptionDates(data);
          subMeta = BatchUpdateSubscriptionDTO.Meta.builder().subscriptionIds(subscriptionId)
                  .context(UpdateSubscriptionConstants.SUSPEND).origin(UpdateSubscriptionConstants.SUSPENSION_JOB).build();
        } else if (data.get(BICECEConstants.SUBSCRIPTION_STATUS).equals(SubscriptionSuccessV4.StatusEnum.TERMINATED.toString())) {
          updateO2PSubscriptionDates(data);
          subMeta = BatchUpdateSubscriptionDTO.Meta.builder().subscriptionIds(subscriptionId)
                  .context(UpdateSubscriptionConstants.TERMINATE).origin(UpdateSubscriptionConstants.TERMINATION_JOB).build();
        } else if (data.get(BICECEConstants.SUBSCRIPTION_STATUS).equals(SubscriptionSuccessV4.StatusEnum.EXPIRED.toString())) {
          updateO2PSubscriptionDates(data);
          subMeta = BatchUpdateSubscriptionDTO.Meta.builder().subscriptionIds(subscriptionId)
                  .context(UpdateSubscriptionConstants.EXPIRE).origin(UpdateSubscriptionConstants.EXPIRATION_JOB).build();
        }
      } else {
        throw new Exception("Subscription status can not be null.");
      }
      BatchUpdateSubscriptionDTO batchUpdateSubscriptionDTO = BatchUpdateSubscriptionDTO.builder().meta(subMeta).
              data(BatchUpdateSubscriptionDTO.Data.builder().build()).build();
      body = new Gson().toJson(batchUpdateSubscriptionDTO);
      Util.PrintInfo(BICECEConstants.PAYLOAD_AUTH + body + "\n");
    return patchRestResponse(updateSubscriptionV4BatchUrl, getHeaderForUpdateSubscription(), body);
  }


  @Step("Update next billing cycle with before date " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> forwardNextBillingCycleForFinancingRenewal(HashMap<String, String> data) {
    //Temporary sleep until we pick up story to address the retry logic to wait for Order charge.
    Util.sleep(240000);

    String getPurchaseOrderDetailsUrl = data.get("getSubscriptionByIdUrl");
    getPurchaseOrderDetailsUrl = addTokenInResourceUrl(getPurchaseOrderDetailsUrl,
        data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));
    Util.printInfo("getPriceDetails baseURL : " + getPurchaseOrderDetailsUrl);

    PelicanSignature signature = requestSigner.generateSignature();

    String Content_Type = BICECEConstants.APPLICATION_JSON;
    String accept = BICECEConstants.APPLICATION_VNDAPI_JSON;
    HashMap<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, signature.xE2HMACSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, signature.xE2PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, signature.xE2AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, signature.xE2HMACTimestamp);
    header.put("X-Request-Ref", UUID.randomUUID().toString());
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, accept);

    String nextBillingDate;
    String endDate;
    String expirationDate;

    nextBillingDate = Util.customDate("MM/dd/yyyy", 0, +2, 0) + " 20:13:28 UTC";
    endDate = Util.customDate("MM/dd/yyyy", 0, +2, 0) + " 20:13:28 UTC";
    expirationDate = Util.customDate("MM/dd/yyyy", +1, +2, 0) + " 20:13:28 UTC";

    String path = EseCommonConstants.APP_PAYLOAD_PATH  + "BIC_Update_NextBilling.json";
    File rawPayload = new File(path);
    UpdateNextBilling nextBillingJson;
    ObjectMapper om = new ObjectMapper();
    String inputPayload = "";
    try {
      nextBillingJson = om.readValue(rawPayload, UpdateNextBilling.class);
      nextBillingJson.getData().setNextBillingDate(nextBillingDate);
      nextBillingJson.getData().setEndDate(endDate);
      nextBillingJson.getData().setExpirationDate(expirationDate);
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
      if (result != "") {
        Util.printInfo("" + result);
        JsonPath jp = new JsonPath(result);
        String purchaseOrderId = jp.get("purchaseOrderId").toString();
        String exportControlStatus = jp.get("exportControlStatus").toString();
        String nextRenewalDate = jp.get("nextRenewalDate").toString();

        results.put("purchaseOrderId", purchaseOrderId);
        results.put("exportControlStatus", exportControlStatus);
        results.put("nextRenewalDate", nextRenewalDate);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return results;
  }

  @Step("Order Service finance reports API " + GlobalConstants.TAG_TESTINGHUB)
  public String postReportsFinancePelicanAPI(HashMap<String, String> data) {
    String postReportsFinancePelicanAPIUrl = data.get("postReportsFinancePelicanAPIUrl");

    //Generate the input JSON
    JSONArray orderArray = new JSONArray();
    orderArray.put(data.get(BICECEConstants.ORDER_ID));

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
    Util.printInfo("postReportsFinancePelicanAPIUrl : " + postReportsFinancePelicanAPIUrl);

    PelicanSignature signature = requestSigner.generateSignature();
    String Content_Type = BICECEConstants.APPLICATION_JSON;

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, signature.xE2HMACSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, signature.xE2PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, signature.xE2AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);

    Response response = getRestResponse(postReportsFinancePelicanAPIUrl, header, requestJson);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);

    return result;
  }

  @Step("Get Pelican Response" + GlobalConstants.TAG_TESTINGHUB)
  public String getPurchaseOrder(HashMap<String, String> data) {
    String getPurchaseOrderDetailsUrl = data.get("getPurchaseOrderDetailsUrl");

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
    Util.printInfo("getPurchaseOrderDetailsUrl : " + getPurchaseOrderDetailsUrl);

    String Content_Type = BICECEConstants.APPLICATION_JSON;
    PelicanSignature signature = requestSigner.generateSignature();

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, signature.xE2HMACSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, signature.xE2PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, signature.xE2AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);

    Response response = getRestResponse(getPurchaseOrderDetailsUrl, header, requestJson);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);

    return result;
  }

  @Step("Get Pelican Response with polling" + GlobalConstants.TAG_TESTINGHUB)
  public JsonPath getRefundedPurchaseOrderWithPolling(HashMap<String, String> data) {
    //Adyen delays in IPN response is causing test failures. Until the issue is resolved lets
    // add additional 5 minutes sleep for the IPN message to come back.
    Util.sleep(300000);

    return new FluentWait<>(data)
        .withTimeout(Duration.ofMinutes(30L))
        .pollingEvery(Duration.ofMinutes(5L))
        .until(input -> {
          Util.printInfo("Polling purchase order API until order status is REFUNDED");
          return Optional.of(getPurchaseOrder(input))
              .map(JsonPath::new)
              .filter(jsonPath -> jsonPath.get("content[0].orderState").toString().equals("REFUNDED"))
              .orElse(null);
        });
  }

  @Step("Get Purchase Order V4 API" + GlobalConstants.TAG_TESTINGHUB)
  public String getPurchaseOrderV4(HashMap<String, String> data) {
    String Content_Type = BICECEConstants.APPLICATION_JSON;
    PelicanSignature signature = requestSigner.generateSignature();

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, signature.xE2HMACSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, signature.xE2PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, signature.xE2AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);

    String requestUrl;
    String body = null;
    if (data.get(BICConstants.orderNumber) != null) {
      requestUrl = addTokenInResourceUrl(data.get("getPurchaseOrderDetailsV4Url"),
          data.get(BICECEConstants.orderNumber));
    } else {
      requestUrl = data.get("getPurchaseOrderFiltersV4Url");

      //Generate the input JSON
      JSONObject filters = new JSONObject();
      JSONObject oxygenId = new JSONObject();
      oxygenId.put("oxygenId", data.get(BICConstants.oxid));
      filters.put("filters", oxygenId);
      body = filters.toJSONString();
    }

    Util.printInfo("Get Purchase Order V4 Request URL: " + requestUrl);
    Util.PrintInfo("Requesting order details with filter: " + body);
    Response response = getRestResponse(requestUrl, header, body);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);

    // Normalize filter vs order request
    if (data.get(BICConstants.orderNumber) == null) {
      JsonPath jp = new JsonPath(result);
      result = new JSONObject(jp.get("content[0]")).toJSONString();
    }

    return result;
  }

  @Step("Get Purchase Order V4 API with Polling" + GlobalConstants.TAG_TESTINGHUB)
  public JsonPath getRefundedPurchaseOrderV4WithPolling(HashMap<String, String> data) {
    // Adyen delays in IPN response is causing test failures. Until the issue is resolved lets add additional
    // 5 minutes sleep for the IPN message to come back.
    Util.sleep(300000);

    return new FluentWait<>(data)
        .withTimeout(Duration.ofMinutes(30L))
        .pollingEvery(Duration.ofMinutes(5L))
        .until(input -> {
          Util.printInfo("Polling purchase order V4 API until order status is REFUNDED or PARTIALLY_REFUNDED");
          return Optional.of(getPurchaseOrderV4(input))
              .map(JsonPath::new)
              .filter(jsonPath -> jsonPath.get("orderState").toString().contains(BICECEConstants.REFUNDED))
              .orElse(null);
        });
  }

  @Step("Create Refund Order" + GlobalConstants.TAG_TESTINGHUB)
  public void createRefundOrder(HashMap<String, String> data) {
    String purchaseOrderDetailsUrl = data.get("putPelicanRefundOrderUrl");
    String getPurchaseOrderDetailsUrl = addTokenInResourceUrl(purchaseOrderDetailsUrl,
        data.get(BICConstants.orderNumber));
    Util.printInfo("putPelicanRefund details Url : " + getPurchaseOrderDetailsUrl);

    String Content_Type = BICECEConstants.APPLICATION_JSON;
    PelicanSignature signature = requestSigner.generateSignature();

    Map<String, String> header = new HashMap<>();
    header.put("x-e2-hmac-signature", signature.xE2HMACSignature);
    header.put("x-e2-partnerid", signature.xE2PartnerId);
    header.put("x-e2-appfamilyid", signature.xE2AppFamilyId);
    header.put("x-e2-hmac-timestamp", signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, Content_Type);

    Response response = createRefundOrder(getPurchaseOrderDetailsUrl, header, new ArrayList<>());
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);
  }

  @Step("Refund O2P Orders" + GlobalConstants.TAG_TESTINGHUB)
  public void createRefundOrderV4(HashMap<String, String> data, List<Integer> lineItemIdsList) {
    String refundPurchaseOrderUrl = data.get("putPelicanRefundOrderV4Url");
    String refundPurchaseOrderV4Url = addTokenInResourceUrl(refundPurchaseOrderUrl,
        data.get(BICConstants.orderNumber));
    Util.printInfo("Order Service Refund Url : " + refundPurchaseOrderV4Url);
    String Content_Type = BICECEConstants.APPLICATION_JSON;
    PelicanSignature signature = requestSigner.generateSignature();

    Map<String, String> header = new HashMap<>();
    header.put("x-e2-hmac-signature", signature.xE2HMACSignature);
    header.put("x-e2-partnerid", signature.xE2PartnerId);
    header.put("x-e2-appfamilyid", signature.xE2AppFamilyId);
    header.put("x-e2-hmac-timestamp", signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, Content_Type);

    Response response = createRefundOrder(refundPurchaseOrderV4Url, header, lineItemIdsList);
    String result = response.getBody().asString();
  }

  @Step("Partial Refund O2P Orders" + GlobalConstants.TAG_TESTINGHUB)
  public void createPartialRefundOrderV4(HashMap<String, String> data) {
    String refundPurchaseOrderUrl = data.get("putPelicanRefundOrderV4Url");
    String refundPurchaseOrderV4Url = addTokenInResourceUrl(refundPurchaseOrderUrl,
            data.get(BICConstants.orderNumber));
    Util.printInfo("Order Service Refund Url : " + refundPurchaseOrderV4Url);
    String Content_Type = BICECEConstants.APPLICATION_JSON;
    PelicanSignature signature = requestSigner.generateSignature();

    Map<String, String> header = new HashMap<>();
    header.put("x-e2-hmac-signature", signature.xE2HMACSignature);
    header.put("x-e2-partnerid", signature.xE2PartnerId);
    header.put("x-e2-appfamilyid", signature.xE2AppFamilyId);
    header.put("x-e2-hmac-timestamp", signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, Content_Type);

    Response response = createRefundOrder(refundPurchaseOrderV4Url, header, new ArrayList<>());
    String result = response.getBody().asString();

  }

  @Step("Renew Pelican Subscription" + GlobalConstants.TAG_TESTINGHUB)
  public void renewSubscription(HashMap<String, String> data) {
    String pelicanRenewSubscriptionUrl = data.get("pelicanRenewalURL");
    String getRenewSubscriptionUrl = addTokenInResourceUrl(pelicanRenewSubscriptionUrl,
        data.get(BICConstants.subscriptionId));
    Util.printInfo("Pelican Renew Subscription Url : " + getRenewSubscriptionUrl);

    String Content_Type = BICECEConstants.APPLICATION_JSON;
    PelicanSignature signature = requestSigner.generateSignature();

    Map<String, String> header = new HashMap<>();
    header.put("x-e2-hmac-signature", signature.xE2HMACSignature);
    header.put("x-e2-partnerid", signature.xE2PartnerId);
    header.put("x-e2-appfamilyid", signature.xE2AppFamilyId);
    header.put("x-e2-hmac-timestamp", signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);
    header.put(BICECEConstants.ACCEPT, Content_Type);

    Response response = RestAssured.given().headers(header).post(getRenewSubscriptionUrl);

    int statusCode = response.getStatusCode();
    Util.PrintInfo("Renew Subscription Response HTTP Status Code: " + statusCode);

    if (statusCode != 200) {
      String result = response.getBody().asString();
      Assert.fail("Renew Subscription gave none HTTP.OK response. Status Code: " + result);
    }
  }

  @Step("Subscription : Getting purchase order details from Order Service " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getPurchaseOrderDetails(String purchaseOrderAPIresponse) {
    HashMap<String, String> results = new HashMap<>();
    JsonPath jp = new JsonPath(purchaseOrderAPIresponse);
    try {
      results.put("getPOReponse_origin", jp.get("content[0].origin").toString());
      results.put(BICECEConstants.ORDER_ID, jp.get("content[0].id").toString());
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
      results.put("getPOReponse_CreatedDate", jp.get("content[0].created").toString());
      results.put("getPOReponse_fulfillmentDate",
          jp.get("content[0].lineItems[0].fulfillmentDate").toString());
      results.put("getPOResponse_promotionDiscount",
          jp.get("content[0].lineItems[0].lineItemTotals.promotionDiscount").toString());
      results.put("getPOResponse_subtotalAfterPromotionsWithTax",
          jp.get("content[0].discountedSubtotalWithTax").toString());
      results.put("getPOResponse_subtotalAfterPromotions",
          jp.get("content[0].discountedSubtotal").toString());
      results.put("getPOReponse_paymentProcessor",
          jp.get("content[0].payments[0].paymentProcessor").toString());
      results.put("getPOReponse_firstName",
          jp.get("content[0].billingInfo.firstName"));
      results.put("getPOReponse_lastName",
          jp.get("content[0].billingInfo.lastName"));
      results.put("getPOReponse_street",
          jp.get("content[0].billingInfo.street"));
      results.put("getPOReponse_city",
          jp.get("content[0].billingInfo.city"));
      results.put("getPOReponse_last4Digits",
          jp.get("content[0].billingInfo.lastDigits"));
      results.put("getPOReponse_oxygenID", jp.get("content[0].buyerExternalKey").toString());
      results.put("language", jp.get("content[0].language").toString());
      results.put(BICConstants.emailid, jp.get("content[0].email").toString());
      results.put("refund_orderState", jp.get("content[0].orderState").toString());
    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to get Purchase Order Details from Order Service");
    }

    return results;
  }

  @Step("Order Service : Get Purchase order details from Order Service V4 API" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getPurchaseOrderV4Details(String poResponse) {
    HashMap<String, String> results = new HashMap<>();

    JsonPath jp = new JsonPath(poResponse);
    try {
      results.put("getPOResponse_origin", jp.get("origin").toString());
      results.put(BICECEConstants.ORDER_ID, jp.get("id").toString());
      results.put("getPOResponse_quoteId", jp.get("quoteId") == null ? "" : jp.get("quoteId").toString());
      results.put("getPOResponse_salesChannelType", jp.get("salesChannelType").toString());
      results.put("getPOResponse_orderState", jp.get("orderState").toString());
      results.put("getPOResponse_taxId", jp.get("taxId") != null ? jp.get("taxId").toString() : "");
      results.put("getPOResponse_countryCode", jp.get("price.country").toString());
      results.put(BICECEConstants.SUBTOTAL_WITH_TAX, jp.get("price.totalPrice").toString());

      results.put("getPOResponse_storedPaymentProfileId",
          jp.get("payment.paymentProfileId") != null ? jp.get("payment.paymentProfileId").toString() : "");
      results.put(BICECEConstants.IS_TAX_EXEMPT,
          jp.get("payment.isTaxExempt") != null ? jp.get("payment.isTaxExempt").toString() : "null");
      results.put("getPOResponse_productType", jp.get("lineItems[0].offering.name").toString());
      results.put("getPOResponse_quantity", jp.get("lineItems[0].quantity").toString());
      if ("flex".equalsIgnoreCase(results.get("getPOResponse_productType"))) {
        results.put(BICECEConstants.FLEX_TOKENS, jp.get("lineItems[0].quantity").toString());
      }
      results.put("getPOResponse_offeringId", jp.get("lineItems[0].offering.id").toString());
      results.put("getPOResponse_fulfillmentStatus", jp.get("lineItems[0].fulfillmentStatus").toString());
      results.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
          jp.get("lineItems[0].subscriptionInfo.subscriptionId").toString());
      results.put("getPOResponse_subscriptionPeriodStartDate",
          jp.get("lineItems[0].subscriptionInfo.subscriptionPeriodStartDate").toString());
      results.put("getPOResponse_subscriptionPeriodEndDate",
          jp.get("lineItems[0].subscriptionInfo.subscriptionPeriodEndDate").toString());
      results.put("getPOResponse_fulfillmentDate", jp.get("lineItems[0].fulfillmentDate").toString());
      results.put("getPOResponse_paymentProcessor",
          jp.get("payment.paymentProcessor") != null ? jp.get("payment.paymentProcessor").toString() : "");
      results.put("getPOResponse_endCustomer_company", jp.get("endCustomer.company"));
      results.put("getPOResponse_endCustomer_firstName", jp.get("endCustomer.firstName"));
      results.put("getPOResponse_endCustomer_lastName", jp.get("endCustomer.lastName"));
      results.put("getPOResponse_endCustomer_addressLine1", jp.get("endCustomer.addressLine1"));
      results.put("getPOResponse_endCustomer_city", jp.get("endCustomer.city"));
      results.put("getPOResponse_endCustomer_state", jp.get("endCustomer.state"));
      results.put("getPOReponse_firstName", jp.get("endCustomer.firstName"));
      results.put("getPOReponse_lastName", jp.get("endCustomer.lastName"));
      results.put("getPOReponse_street", jp.get("endCustomer.addressLine1"));
      results.put("getPOReponse_city", jp.get("endCustomer.city"));
      results.put("getPOResponse_subtotalAfterPromotions", jp.get("price.totalPrice").toString());

      results.put("getPOResponse_endCustomer_country", jp.get("endCustomer.country"));
      results.put("getPOResponse_endCustomer_postalCode", jp.get("endCustomer.postalCode"));
      results.put("getPOResponse_endCustomer_accountCsn", jp.get("endCustomer.accountCsn"));
      results.put("getPOResponse_endCustomer_contactCsn", jp.get("endCustomer.contactCsn"));

      results.put("getPOResponse_billingAddress_firstName", jp.get("billingAddress.firstName"));
      results.put("getPOResponse_billingAddress_lastName", jp.get("billingAddress.lastName"));
      results.put("getPOResponse_billingAddress_addressLine1", jp.get("billingAddress.addressLine1"));
      results.put("getPOResponse_billingAddress_city", jp.get("billingAddress.city"));
      results.put("getPOResponse_billingAddress_accountCsn",
          jp.get("billingAddress.accountCsn") != null ? jp.get("billingAddress.accountCsn").toString() : null);
      results.put("getPOResponse_billingAddress_contactCsn",
          jp.get("billingAddress.contactCsn") != null ? jp.get("billingAddress.contactCsn").toString() : null);

      if (jp.get("agentAccount") != null) {
        results.put("getPOResponse_agentAccount_firstName", jp.get("agentAccount.firstName"));
        results.put("getPOResponse_agentAccount_lastName", jp.get("agentAccount.lastName"));
        results.put("getPOResponse_agentAccount_addressLine1", jp.get("agentAccount.addressLine1"));
        results.put("getPOResponse_agentAccount_city", jp.get("agentAccount.city"));
        results.put("getPOResponse_agentAccount_accountCsn", jp.get("agentAccount.accountCsn"));
        results.put("getPOResponse_agentAccount_contactCsn", jp.get("agentAccount.contactCsn"));
      } else {
        results.put("getPOResponse_agentAccount", null);
      }

      if (jp.get("agentContact") != null) {
        results.put("getPOResponse_agentContact_firstName", jp.get("agentContact.firstName"));
        results.put("getPOResponse_agentContact_lastName", jp.get("agentContact.lastName"));
        results.put("getPOResponse_agentContact_addressLine1", jp.get("agentContact.addressLine1"));
        results.put("getPOResponse_agentContact_city", jp.get("agentContact.city"));
        results.put("getPOResponse_agentContact_accountCsn", jp.get("agentContact.accountCsn"));
        results.put("getPOResponse_agentContact_contactCsn", jp.get("agentContact.contactCsn"));
      } else {
        results.put("getPOResponse_agentContact", null);
      }

      results.put("getPOResponse_oxygenID", jp.get("purchaser.oxygenId").toString());
      results.put(BICECEConstants.PAYER_EMAIL, jp.get("purchaser.email").toString());
      results.put(BICECEConstants.PAYER_CSN,
          jp.get("payerAccount") != null ? jp.get("payerAccount.accountCsn").toString() : null);

    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to get Purchase Order Details from Order Service V4 API" + e.getMessage());
    }
    return results;
  }

  @Step("Order Service : Get line item details " + GlobalConstants.TAG_TESTINGHUB)
  public List<LineItemDTO> getLineItemDetailsForMultiLineOrder(String poResponse, int numberOfLineItems) {
    HashMap<String, String> results = new HashMap<>();
    JsonPath jsonPath = new JsonPath(poResponse);
    List<LineItemDTO> list = new ArrayList<>();
    try {
      results.put(BICECEConstants.ORDER_ID, jsonPath.get("id").toString());
      for (int i = 0; i < numberOfLineItems; i++) {
        String lineItem = String.format("lineItems[%s].", i);
        LineItemDTO dto = LineItemDTO.builder().lineItemid(jsonPath.get(lineItem + "id").toString())
                .subscriptionId(jsonPath.get(lineItem + "subscriptionInfo.subscriptionId").toString())
                .state(jsonPath.get(lineItem + "state").toString())
                .offeringId(jsonPath.get(lineItem + "offering.id").toString())
                .purchaseOrderId(jsonPath.get("id").toString()).build();
        list.add(dto);
      }
    } catch (NullPointerException e) {
      Util.printTestFailedMessage("Number of line items are not correct in po response." + e.getMessage());
      AssertUtils.fail("Number of line items are not correct in po response.");
    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to get Purchase Order Details from Order Service V4 API" + e.getMessage());
    }
    return list;
  }

  public String addTokenInResourceUrl(String resourceUrl, String tokenString) {
    return resourceUrl.replace("passtoken", tokenString);
  }

  /**
   * Retry a pelican call upto 20 times to have PO CHARGED
   *
   * @param results - Data hashmap
   * @param silent  - Whether a failure 20 times in a row causes the testcase to fail
   * @return - Pelican response
   */
  public String retryGetPurchaseOrder(HashMap<String, String> results, boolean silent, boolean isO2P) {
    String response = "";
    boolean isOrderCharged = false;
    for (int i = 1; i < 20; i++) {
      if (isO2P) {
        response = getPurchaseOrderV4(results);
      } else {
        response = getPurchaseOrder(results);
      }
      int intIndex = response.indexOf("CHARGED");
      if (intIndex == -1) {
        Util.printInfo("Purchase Order is not CHARGED yet. Retry #" + i);
        Util.sleep(30000);
      } else {
        Util.printInfo("BiC Purchase Order is Charged");
        isOrderCharged = true;
        break;
      }
    }
    if (!isOrderCharged && !silent) {
      AssertUtils.fail("Failed: Purchase Order not charged for PO # " + results.get(
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
    return retryGetPurchaseOrder(results, false, false);
  }

  /**
   * Retry a pelican call up to 3 times to find a subscription id
   *
   * @param results - Data hashmap
   * @return - Pelican response
   */
  public String retryO2PGetPurchaseOrder(HashMap<String, String> results) {
    return retryGetPurchaseOrder(results, false, true);
  }


  /**
   * @param quoteInputMap      - input Quote map
   * @param pelicanResponseMap - Pelican response V4
   */
  public void validateQuoteDetailsWithPelican(Map<String, String> quoteInputMap,
      Map<String, String> pelicanResponseMap, Address address) {

    AssertUtils.assertEquals("Quote Id should match.", quoteInputMap.get("quote_id"),
        pelicanResponseMap.get("getPOResponse_quoteId"));
    //AssertUtils.assertEquals("",quoteInputMap.get("OpportuntiyId"),pelcanResonseMap.get(""));
    AssertUtils.assertEquals("Quantity should match.", quoteInputMap.get("tokens"),
        pelicanResponseMap.get("tokens"));
    AssertUtils.assertEquals("EndCustomer First Name should match.", quoteInputMap.get("firstname"),
        pelicanResponseMap.get("getPOResponse_endCustomer_firstName"));
    AssertUtils.assertEquals("EndCustomer Last Name should match.", quoteInputMap.get("lastname"),
        pelicanResponseMap.get("getPOResponse_endCustomer_lastName"));
    AssertUtils.assertEquals("EndCustomer Company should match",
        pelicanResponseMap.get("getPOResponse_endCustomer_company").toUpperCase(), address.company.toUpperCase());

    if (Arrays.asList("STORE-NAMER", "STORE-CA", "STORE-AUS").contains(quoteInputMap.get(BICECEConstants.STORE_NAME))) {
      AssertUtils.assertEquals("EndCustomer Street Name should match.",
          pelicanResponseMap.get("getPOResponse_endCustomer_addressLine1").substring(0, 4).toUpperCase(),
          address.addressLine1.substring(0, 4).toUpperCase());
    }
    if (Arrays.asList("STORE-NAMER", "STORE-CA", "STORE-AUS").contains(quoteInputMap.get(BICECEConstants.STORE_NAME))) {
      AssertUtils.assertEquals("EndCustomer City should match.",
          pelicanResponseMap.get("getPOResponse_endCustomer_city").substring(0, 3).toUpperCase(),
          address.city.substring(0, 3).toUpperCase());
    }
    if (address.province != null && !address.province.isEmpty()) {
      AssertUtils.assertEquals("EndCustomer State should match.",
          pelicanResponseMap.get("getPOResponse_endCustomer_state").toUpperCase(), address.province.toUpperCase());
    }
    AssertUtils.assertEquals("EndCustomer Country should match.",
        pelicanResponseMap.get("getPOResponse_endCustomer_country").toUpperCase(), address.countryCode.toUpperCase());
    if (!address.postalCode.equals("N/A")) {

      AssertUtils.assertEquals("EndCustomer Postal Code should match.",
          pelicanResponseMap.get("getPOResponse_endCustomer_postalCode").substring(0, 3),
          address.postalCode.substring(0, 3));
    }

    if (System.getProperty("taxId") != null) {
      if (Arrays.asList("en_AU", "de_CH", "no_NO", "en_IS", "en_LI", "es_ES")
          .contains(quoteInputMap.get(BICECEConstants.LOCALE))) {
        AssertUtils.assertEquals("TaxId should match.",
            System.getProperty("taxId").toUpperCase(),
            pelicanResponseMap.get("getPOResponse_taxId").toUpperCase());
      } else {
        AssertUtils.assertEquals("TaxId should match.",
            pelicanResponseMap.get("getPOResponse_countryCode").concat(System.getProperty("taxId").toUpperCase()),
            pelicanResponseMap.get("getPOResponse_taxId").toUpperCase());
      }
    }

    AssertUtils.assertEquals("Product Type should match.", quoteInputMap.get("productType").toUpperCase(),
        pelicanResponseMap.get("getPOResponse_productType").toUpperCase());
    try {
      SimpleDateFormat sdfQuote = new SimpleDateFormat(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE);
      String quoteSubDate = quoteInputMap.get(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE);

      SimpleDateFormat pelicanFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      pelicanFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      Date pelicanDate = pelicanFormat.parse(pelicanResponseMap.get("getPOResponse_subscriptionPeriodStartDate"));
      sdfQuote.setTimeZone(TimeZone.getTimeZone(System.getProperty("timezone")));
      String pelicanSubDate = sdfQuote.format(pelicanDate);
      //AssertUtils.assertEquals("Subscription Start Date should match.", quoteSubDate, pelicanSubDate);

    } catch (Exception e) {
      Util.printInfo("Exception in validating subscription start date . " + e.getMessage());
    }
  }
}
