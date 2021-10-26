package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.bicapiModel.PayloadAddPaymentProfile;
import com.autodesk.testinghub.core.bicapiModel.PayloadSpocAUTHToken;
import com.autodesk.testinghub.core.bicapiModel.UpdateNextBilling;
import com.autodesk.testinghub.core.common.CommonConstants;
import com.autodesk.testinghub.core.common.services.ApigeeAuthenticationService;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ErrorEnum;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.testng.Assert;

public class PelicanTestBase {

  private static String productName = "";

  public PelicanTestBase() {
    Util.PrintInfo("PelicanTestBase from ece");
  }

  @SuppressWarnings("unchecked")
  public static Response createRefundOrder(String baseUrl, Map<String, String> header) {

    JSONObject requestParams = new JSONObject();
    requestParams.put("orderEvent", "REFUND");

    Response response = RestAssured.given().headers(header).body(requestParams).put(baseUrl);
    Util.printInfo(response.asString());

    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
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
      if(requestJson != null) {
        response = given().headers(header).body(requestJson).when().post();
      }else{
        response = given().headers(header).when().get();
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

    Response response = getRestResponse(invoicePelicanAPIUrl, header,"{}");
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);
    JsonPath js = new JsonPath(result);
    Util.printInfo("js is:" + js);
  }

  // @Step("Validate BIC Order in Pelican" + GlobalConstants.TAG_TESTINGHUB)
  @SuppressWarnings("unused")

  public String getPelicanResponse(HashMap<String, String> data) {
    String getPurchaseOrderDetailsUrl = data.get("getPurchaseOrderDetailsUrl");
    productName = data.get("productName");

    //Generate the input JSON
    JSONObject filters = new JSONObject();
    JSONArray array = new JSONArray();
    array.put(data.get(BICConstants.orderNumber));
    JSONObject orders = new JSONObject();
    orders.put("orderIds", array);
    filters.put("filters", orders);
    String requestJSon = filters.toJSONString();

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

    Response response = getRestResponse(getPurchaseOrderDetailsUrl, header, requestJSon);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);

    return result;
  }

  public HashMap<String, String> createRefundOrder(HashMap<String, String> data) {
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
    JsonPath js = new JsonPath(result);

    results.put("refund_orderState", js.get("orderState"));
    results.put("refund_fulfillmentStatus", js.get("fulfillmentStatus"));
    results.put("refund_paymentMethodType", js.get("billingInfo.paymentMethodType"));
    results.put("refund_finalExportControlStatus", js.get("finalExportControlStatus"));
    results.put("refund_uiInitiatedGetOrders", Boolean.toString(js.get("uiInitiatedGetOrders")));
    results.put("refund_lineItemState", js.get("lineItems[0].lineItemState"));

    return results;
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

  public String getSpocTimeStamp() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    return sdf.format(new Date());
  }

  public String getSpocSignature(String o2id, String email, String secretKey, String timeStamp) {
    String hash = null;
    String hashCode = null;
    hashCode = o2id + email + timeStamp;
    hash = ApigeeAuthenticationService.getSHA256Hash(secretKey, hashCode);

    return hash;
  }

  public String getSpocAuthToken(LinkedHashMap<String, String> testDataForEachMethod) {
    String hashedStr = "Basic ";
    RestAssured.baseURI = CommonConstants.spocAuthTokenUrl;
    File rawPayload = new File(Util.getCorePayloadPath() + "PayloadSpoc_AUTHToken.json");

    String timeStamp = getSpocTimeStamp();
    String email = testDataForEachMethod.get(TestingHubConstants.emailid);
    String o2id = testDataForEachMethod.get(TestingHubConstants.oxygenid);
    String secretKey = ProtectedConfigFile.decrypt(CommonConstants.spocSecretKey);
    String spocSignature = getSpocSignature(o2id, email, secretKey, timeStamp);
    HashMap<String, String> authHeaders = new HashMap<String, String>();
    authHeaders.put(BICECEConstants.CONTENT_TYPE, BICECEConstants.APPLICATION_JSON);

    PayloadSpocAUTHToken authJsonClass = null;

    ObjectMapper om = new ObjectMapper();
    String inputPayload = "";
    try {
      authJsonClass = om.readValue(rawPayload, PayloadSpocAUTHToken.class);

      authJsonClass.setEmail(email);
      authJsonClass.setSignature(spocSignature);
      authJsonClass.setTimestamp(timeStamp);
      authJsonClass.setUserExtKey(o2id);

      inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonClass);
      Util.PrintInfo(BICECEConstants.PAYLOAD_AUTH + inputPayload + "\n");
    } catch (IOException e1) {
      e1.printStackTrace();
      AssertUtils.fail("Failed to generate SPOC Authorization Token" + e1.getMessage());
    }

    Response response = given().headers(authHeaders).body(inputPayload).when().post();
    String result = response.getBody().asString();
    JsonPath jp = new JsonPath(result);
    String sessionId = jp.get("sessionId");
    String grantToken = jp.get("grantToken");

    String mixCode = sessionId + ":" + grantToken;
    try {
      hashedStr = hashedStr
          + java.util.Base64.getEncoder()
          .encodeToString(mixCode.getBytes(StandardCharsets.UTF_8.toString()));

    } catch (Exception e) {
      AssertUtils.fail("Failed to generate BIC Authorization Token");
    }
    return hashedStr.trim();
  }

  public String getPaymentProfileId(LinkedHashMap<String, String> testDataForEachMethod) {
    String paymentProfileID = "";
    try {
      ObjectMapper om = new ObjectMapper();
      String inputPayload = "";
      String processor =
          testDataForEachMethod.get("billingProcessor").equalsIgnoreCase(
              BICECEConstants.ADYEN) ? BICECEConstants.ADYEN
              : "bluesnap";

      File jsonFile = ApigeeTestBase.getFile(processor, "PayloadAddPaymentProfile.json");
      // getUserDetails() Keys => firstNameCEP, lastNameCEP, emailIdCEP, oxid
      String currency = testDataForEachMethod.get(TestingHubConstants.currencyStore);
      String city = testDataForEachMethod.get(TestingHubConstants.cityStore);
      String country = testDataForEachMethod.get(TestingHubConstants.countryStore);
      String postalCode = testDataForEachMethod.get(TestingHubConstants.postalCodeStore);
      String stateProvince = testDataForEachMethod.get(TestingHubConstants.stateProvinceStore);
      String streetAddress = testDataForEachMethod.get(TestingHubConstants.streetAddressStore);
      String email = testDataForEachMethod.get(TestingHubConstants.emailid);
      String firstName = testDataForEachMethod.get(TestingHubConstants.firstname);
      String lastName = testDataForEachMethod.get(TestingHubConstants.lastname);
      String companyName = "Holla " + lastName;
      String oxid = testDataForEachMethod.get(TestingHubConstants.oxygenid);

      try {
        if (processor.equals(BICECEConstants.ADYEN)) {
          PayloadAddPaymentProfile authJsonClass = om
              .readValue(jsonFile, PayloadAddPaymentProfile.class);
          authJsonClass.getUser().setCurrency(currency);
          authJsonClass.getUser().setEmail(email);
          authJsonClass.getUser().getPaymentProfile().getBillingInfo().setCity(city);
          authJsonClass.getUser().getPaymentProfile().getBillingInfo().setCountry(country);
          authJsonClass.getUser().getPaymentProfile().getBillingInfo().setPostalCode(postalCode);
          authJsonClass.getUser().getPaymentProfile().getBillingInfo()
              .setStateProvince(stateProvince);
          authJsonClass.getUser().getPaymentProfile().getBillingInfo()
              .setStreetAddress(streetAddress);
          authJsonClass.getUser().getPaymentProfile().getBillingInfo()
              .setFirstName(firstName.trim());
          authJsonClass.getUser().getPaymentProfile().getBillingInfo().setLastName(lastName);
          authJsonClass.getUser().getPaymentProfile().getBillingInfo().setCompanyName(companyName);
          inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonClass);
          Util.PrintInfo(BICECEConstants.PAYLOAD_AUTH + inputPayload);
        } else {
          PayloadAddPaymentProfile authJsonBluesnapClass = om.readValue(jsonFile,
              PayloadAddPaymentProfile.class);
          authJsonBluesnapClass.getUser().setEmail(email);
          authJsonBluesnapClass.getUser().getPaymentProfile().getBillingInfo()
              .setFirstName(firstName.trim());
          authJsonBluesnapClass.getUser().getPaymentProfile().getBillingInfo()
              .setLastName(lastName);
          authJsonBluesnapClass.getUser().getPaymentProfile().getBillingInfo()
              .setCompanyName(companyName);
          inputPayload = om.writerWithDefaultPrettyPrinter()
              .writeValueAsString(authJsonBluesnapClass);
          Util.PrintInfo(BICECEConstants.PAYLOAD_AUTH + inputPayload);
        }
      } catch (IOException e1) {
        e1.printStackTrace();
        AssertUtils.fail("Error while parsing PayloadAddPaymentProfile.json file");
      }

      String baseUrl =
          TestingHubConstants.getCartBaseURL + TestingHubConstants.postAddPaymentProfile + oxid;
      RestAssured.baseURI = baseUrl;

      HashMap<String, String> authHeaders = new HashMap<String, String>();
      authHeaders.put("Authorization", getSpocAuthToken(testDataForEachMethod));
      authHeaders.put(BICECEConstants.CONTENT_TYPE, BICECEConstants.APPLICATION_JSON);

      Response response = given().headers(authHeaders).body(inputPayload).when().post(baseUrl);
      String result = response.getBody().asString();
      if (response.getStatusCode() != 200) {
        Util.sleep(60000);
        response = given().headers(authHeaders).body(inputPayload).when().post(baseUrl);
        result = response.getBody().asString();
        if (response.getStatusCode() != 200) {
          Util.printInfo(BICECEConstants.RESULT + result);
          Util.printTestFailedMessage(
              "Response code must be 200 but Payment Profle API return " + response
                  .getStatusCode());
          AssertUtils.fail(ErrorEnum.GENERIC_RETRY_MSG.geterr());
        }
      }

      JsonPath jp = new JsonPath(result);
      try {
        paymentProfileID = jp.get("paymentProfiles[0].id") + ":" + companyName;
      } catch (Exception e) {
        AssertUtils.fail("Failed to generate payment Profile ID" + e.getMessage());
      }
      Util.PrintInfo(" paymentProfileID response :: " + paymentProfileID + "\n");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return paymentProfileID;
  }

  @Step("Subscription : subs Validation" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getPurchaseOrderDetails(String purchaseOrderAPIresponse) {
    HashMap<String, String> results = new HashMap<>();
    JsonPath jp = new JsonPath(purchaseOrderAPIresponse);
    try {

      results.put("getPOReponse_origin", jp.get("content[0].origin").toString());
      results
          .put("getPOReponse_storeExternalKey", jp.get("content[0].storeExternalKey").toString());
      results.put("getPOReponse_storedPaymentProfileId",
          jp.get("content[0].payments[0].paymentProfileId").toString());
      results.put("getPOReponse_fulfillmentStatus",
          jp.get("content[0].lineItems[0].fulfillmentStatus").toString());

      if (!productName.equals(BICECEConstants.CLDCR_PLC)) {
        results.put("getPOReponse_origin", jp.get("content[0].origin").toString());
        results.put("getPOReponse_storeExternalKey",
            jp.get("content[0].storeExternalKey").toString());
        results.put("getPOReponse_orderState", jp.get("content[0].orderState").toString());
        results.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
            jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionId").toString());
        results.put("getPOReponse_subscriptionPeriodStartDate",
            jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionPeriodStartDate")
                .toString());
        results.put("getPOReponse_subscriptionPeriodEndDate",
            jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionPeriodEndDate")
                .toString());
        results.put("getPOReponse_fulfillmentDate",
            jp.get("content[0].lineItems[0].fulfillmentDate").toString());
        results.put("getPOResponse_promotionDiscount",
            jp.get("content[0].lineItems[0].lineItemTotals.promotionDiscount").toString());
        results.put("getPOReponse_paymentProcessor",
            jp.get("content[0].payments[0].paymentProcessor").toString());
        results.put("getPOReponse_last4Digits",
            jp.get("content[0].billingInfo.lastDigits"));
        results.put("getPOReponse_taxCode",
            jp.get("content[0].lineItems[0].additionalFees[0].feeCollectorExternalKey")
                .toString());
        results.put("getPOReponse_oxygenID", jp.get("content[0].buyerExternalKey").toString());
      } else {
        results.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID, "NA");
        results.put("getPOReponse_subscriptionPeriodStartDate", "NA");
        results.put("getPOReponse_subscriptionPeriodEndDate", "NA");
        results.put("getPOReponse_fulfillmentDate", "NA");
      }

    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to get Purchase Order Details");
      e.printStackTrace();
    }
    return results;
  }

  public String addTokenInResourceUrl(String resourceUrl, String tokenString) {
    return resourceUrl.replace("passtoken", tokenString);
  }

  private String getDetails(SSLConfig config, String baseUrl, Map<String, String> header) {
    String result = null;
    try {
      Response response = null;
      RestAssured.config = RestAssured.config().sslConfig(config);
      RestAssured.baseURI = baseUrl;
      response = given().headers(header).when().get(baseUrl);
      result = response.getBody().asString();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  public HashMap<String, String> subscriptionsPelicanAPIHeaders(HashMap<String, String> data) {
    HashMap<String, String> authHeaders = new HashMap<String, String>();
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);
    String X_E2_AppFamilyId = data.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);

    authHeaders.put(BICECEConstants.X_E2_PARTNER_ID, X_E2_PartnerId);
    authHeaders.put(BICECEConstants.X_E2_APPFAMILY_ID, X_E2_AppFamilyId);
    authHeaders.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, X_E2_HMAC_Timestamp);
    authHeaders.put(BICECEConstants.X_E2_HMAC_SIGNATURE, hmacSignature);
    authHeaders.put("X-Request-Ref", UUID.randomUUID().toString());
    authHeaders.put(BICECEConstants.ACCEPT, BICECEConstants.APPLICATION_VNDAPI_JSON);
    authHeaders.put(BICECEConstants.CONTENT_TYPE, BICECEConstants.APPLICATION_JSON);

    return authHeaders;
  }

  public String retryPelicanResponse(HashMap<String, String> results) {
    String response = "";
    for (int i = 1; i < 4; i++) {
      response = getPelicanResponse(results);
      int intIndex = response.indexOf("subscriptionId");
      if (intIndex == -1) {
        Util.printInfo("SubscriptionId not found. Retry #" + i);
        Util.sleep(300000);
      } else {
        Util.printInfo("Found subscriptionId at index " + intIndex);
        break;
      }
    }
    return response;
  }
}
