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
import com.google.common.base.Strings;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.ByteArrayInputStream;
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
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class PelicanTestBase {

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
      Util.PrintInfo("result :: " + result);
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

  public static Response getRestResponse(String baseUrl, Map<String, String> header) {
    Response response = null;
    try {
      Util.printInfo("Hitting the URL = " + baseUrl);
      RestAssured.baseURI = baseUrl;
      response = given().headers(header).when().get();
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
        data.get("getPOReponse_subscriptionId"));
    HashMap<String, String> results = new HashMap<String, String>();
    String baseURL = data.get("pelican_BaseUrl");
    Util.printInfo("getPriceDetails baseURL : " + subscriptionByIdUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
    String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

    String Content_Type = "application/vnd.api+json";

    Map<String, String> header = new HashMap<>();
    header.put("X-E2-HMAC-Signature", hmacSignature);
    header.put("X-E2-PartnerId", X_E2_PartnerId);
    header.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
    header.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
    header.put("Content-Type", Content_Type);
    header.put("Accept", Content_Type);

    Response response = getRestResponse(subscriptionByIdUrl, header);
    String result = response.getBody().asString();
    Util.PrintInfo("result :: " + result);
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
      results.put("response_currentBillingPriceId", js.get("data.priceId") != null ? Integer
          .toString(js.get("data.priceId")) : null);
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

    String getPurchaseOrderDetailsUrl = data.get("getPurchaseOrderDetailsUrl");
    getPurchaseOrderDetailsUrl = addTokenInResourceUrl(getPurchaseOrderDetailsUrl,
        data.get(BICConstants.orderNumber));
    Util.printInfo("getPriceDetails baseURL : " + getPurchaseOrderDetailsUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
    String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

    String Content_Type = "application/json";
    String accept = "application/vnd.api+json";
    HashMap<String, String> header = new HashMap<>();
    header.put("X-E2-HMAC-Signature", hmacSignature);
    header.put("X-E2-PartnerId", X_E2_PartnerId);
    header.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
    header.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
    header.put("X-Request-Ref", UUID.randomUUID().toString());
    header.put("Content-Type", Content_Type);
    header.put("accept", accept);

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
      Util.PrintInfo("Payload Auth:: " + inputPayload + "\n");
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
      String priceId = jp.get("data.priceId").toString();
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
    String Content_Type = "application/json";

    Map<String, String> header = new HashMap<>();
    header.put("Content-Type", Content_Type);

    Response response = getRestResponse(invoicePelicanAPIUrl, header);
    String result = response.getBody().asString();
    Util.PrintInfo("result :: " + result);
    JsonPath js = new JsonPath(result);
    Util.printInfo("js is:" + js);
  }

  // @Step("Validate BIC Order in Pelican" + GlobalConstants.TAG_TESTINGHUB)
  @SuppressWarnings("unused")
  public String getPelicanResponse(HashMap<String, String> data) {
    String getPurchaseOrderDetailsUrl = data.get("getPurchaseOrderDetailsUrl");
    getPurchaseOrderDetailsUrl = addTokenInResourceUrl(getPurchaseOrderDetailsUrl,
        data.get(BICConstants.orderNumber));
    data.put("pelican_getPurchaseOrderDetailsUrl", getPurchaseOrderDetailsUrl);
    HashMap<String, String> results = new HashMap<String, String>();
    Util.printInfo("getPurchaseOrderDetailsUrl : " + getPurchaseOrderDetailsUrl);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
    String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

    String Content_Type = "application/x-www-form-urlencoded; charset=UTF-8";

    Map<String, String> header = new HashMap<>();
    header.put("X-E2-HMAC-Signature", hmacSignature);
    header.put("X-E2-PartnerId", X_E2_PartnerId);
    header.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
    header.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
    header.put("Content-Type", Content_Type);
    // header.put("Accept", "application/xml");

    Response response = getRestResponse(getPurchaseOrderDetailsUrl, header);
    String result = response.getBody().asString();
    Util.PrintInfo("result :: " + result);

    return result;
  }

  public HashMap<String, String> createRefundOrder(HashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<String, String>();
    String baseURL = data.get("pelican_BaseUrl");
    Util.printInfo("putPelicanRefund details : " + baseURL);
    String sig_details = getPriceByPriceIdSignature(data);
    String hmacSignature = sig_details.split("::")[0];
    String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
    String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
    String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

    // String Content_Type="application/x-www-form-urlencoded; charset=UTF-8";
    String Content_Type = "application/json";

    Map<String, String> header = new HashMap<>();
    header.put("x-e2-hmac-signature", hmacSignature);
    header.put("x-e2-partnerid", X_E2_PartnerId);
    header.put("x-e2-appfamilyid", X_E2_AppFamilyId);
    header.put("x-e2-hmac-timestamp", X_E2_HMAC_Timestamp);
    header.put("Content-Type", Content_Type);
    header.put("Accept", Content_Type);

    Response response = createRefundOrder(baseURL, header);
    String result = response.getBody().asString();
    Util.PrintInfo("result :: " + result);
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

      String appFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");
      String partnerId = data.get("getPriceDetails_X_E2_PartnerId");

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
    authHeaders.put("Content-Type", "application/json");

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
      Util.PrintInfo("Payload Auth:: " + inputPayload + "\n");
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
          testDataForEachMethod.get("billingProcessor").equalsIgnoreCase("adyen") ? "adyen"
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
        if (processor.equals("adyen")) {
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
          Util.PrintInfo("Payload Auth:: " + inputPayload);
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
          Util.PrintInfo("Payload Auth:: " + inputPayload);
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
      authHeaders.put("Content-Type", "application/json");

      Response response = given().headers(authHeaders).body(inputPayload).when().post(baseUrl);
      String result = response.getBody().asString();
      if (response.getStatusCode() != 200) {
        Util.sleep(60000);
        response = given().headers(authHeaders).body(inputPayload).when().post(baseUrl);
        result = response.getBody().asString();
        if (response.getStatusCode() != 200) {
          Util.printInfo("result :: " + result);
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
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Disable external access to protect from SSRF vulnerabilities
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

      DocumentBuilder builder = factory.newDocumentBuilder();
      ByteArrayInputStream input = new ByteArrayInputStream(
          purchaseOrderAPIresponse.getBytes(StandardCharsets.UTF_8));
      Document doc = builder.parse(input);
      Element root = doc.getDocumentElement();
      Util.printInfo("Root element :" + doc.getDocumentElement().getNodeName());
      String origin = root.getAttribute("origin");
      Util.printInfo("origin : " + origin);
      Util.printInfo("storeExternalKey : " + root.getAttribute("storeExternalKey"));

      String orderState = doc.getElementsByTagName("orderState").item(0).getTextContent();
      Util.printInfo("orderState : " + orderState);

      String productLineCode = root.getElementsByTagName("offeringResponse").item(0).getAttributes()
          .getNamedItem("productLineCode").getTextContent();
      Util.printInfo("productLineCode : " + productLineCode);

      NamedNodeMap subscriptionAttributes = null;
      if (!productLineCode.contains(BICECEConstants.CLDCR_PLC)) {
        subscriptionAttributes = doc.getElementsByTagName("subscription").item(0)
            .getAttributes();
      }

      String subscriptionId = null;
      String subscriptionPeriodStartDate = null;
      String subscriptionPeriodEndDate = null;
      String fulfillmentDate = null;
      String taxCode = null;
      String promotionDiscount = null;
      String oxygenID = null;

      try {
        // Native order response
        subscriptionId = doc.getElementsByTagName("offeringResponse").item(0).getAttributes()
            .getNamedItem("subscriptionId").getTextContent();
        Util.printInfo("subscriptionId : " + subscriptionId);

        subscriptionPeriodStartDate = subscriptionAttributes.getNamedItem(
            "subscriptionPeriodStartDate").getTextContent();
        Util.printInfo("subscriptionPeriodStartDate : " + subscriptionPeriodStartDate);

        subscriptionPeriodEndDate = subscriptionAttributes
            .getNamedItem("subscriptionPeriodEndDate")
            .getTextContent();
        Util.printInfo("subscriptionPeriodEndDate : " + subscriptionPeriodEndDate);
      } catch (Exception e) {
        // Add seat order response
        if (!productLineCode.contains(BICECEConstants.CLDCR_PLC)) {
          try {
            NamedNodeMap subscriptionRequestAttributes = doc.getElementsByTagName(
                "subscriptionQuantityRequest").item(0).getAttributes();

            subscriptionId = subscriptionRequestAttributes.getNamedItem("subscriptionId")
                .getTextContent();
            Util.printInfo("subscriptionId : " + subscriptionId);

            subscriptionPeriodStartDate = subscriptionRequestAttributes.getNamedItem(
                "prorationStartDate").getTextContent();
            Util.printInfo("prorationStartDate : " + subscriptionPeriodStartDate);

            subscriptionPeriodEndDate = subscriptionRequestAttributes.getNamedItem(
                "prorationEndDate").getTextContent();
            Util.printInfo("prorationEndDate : " + subscriptionPeriodEndDate);
          } catch (Exception e1) {
            e1.printStackTrace();
          }
        }
      }

      if (!productLineCode.contains(BICECEConstants.CLDCR_PLC)) {
        if (Strings.isNullOrEmpty(subscriptionId)) {
          AssertUtils
              .fail("SubscriptionID is not available the Pelican response : " + subscriptionId);
        }

        fulfillmentDate = subscriptionAttributes.getNamedItem("fulfillmentDate")
            .getTextContent();
        Util.printInfo("fulfillmentDate : " + fulfillmentDate);
      }

      String storedPaymentProfileId = doc.getElementsByTagName("storedPaymentProfileId").item(0)
          .getTextContent();
      Util.printInfo("storedPaymentProfileId : " + storedPaymentProfileId);

      String fulfillmentStatus = root.getAttribute("fulfillmentStatus");
      Util.printInfo("fulfillmentStatus : " + root.getAttribute("fulfillmentStatus"));

      if (doc.getElementsByTagName("promotionDiscount") != null) {
        promotionDiscount = doc.getElementsByTagName("promotionDiscount").item(0)
            .getTextContent();
      }
      String paymentProcessor = root.getElementsByTagName("payment").item(0).getAttributes()
          .getNamedItem("paymentProcessor").getTextContent();

      String last4Digits = root.getElementsByTagName("last4Digits").item(0).getTextContent();

      taxCode = root.getElementsByTagName("additionalFee").item(0).getAttributes()
          .getNamedItem("feeCollectorExternalKey").getTextContent();

      oxygenID = root.getElementsByTagName("buyerUser").item(0).getAttributes()
          .getNamedItem("externalKey").getTextContent();

      results.put("getPOReponse_orderState", orderState);
      results.put("getPOReponse_storedPaymentProfileId", storedPaymentProfileId);
      results.put("getPOReponse_fulfillmentStatus", fulfillmentStatus);

      if (!productLineCode.contains(BICECEConstants.CLDCR_PLC)) {
        results.put("getPOReponse_subscriptionId", subscriptionId);
        results.put("getPOReponse_subscriptionPeriodStartDate", subscriptionPeriodStartDate);
        results.put("getPOReponse_subscriptionPeriodEndDate", subscriptionPeriodEndDate);
        results.put("getPOReponse_fulfillmentDate", fulfillmentDate);
        results.put("getPOResponse_promotionDiscount", promotionDiscount);
        results.put("getPOReponse_paymentProcessor", paymentProcessor);
        results.put("getPOReponse_last4Digits", last4Digits);
        results.put("getPOReponse_taxCode", taxCode);
        results.put("getPOReponse_oxygenID", oxygenID);
      } else {
        results.put("getPOReponse_subscriptionId", "NA");
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
    String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
    String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

    authHeaders.put("X-E2-PartnerId", X_E2_PartnerId);
    authHeaders.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
    authHeaders.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
    authHeaders.put("X-E2-HMAC-Signature", hmacSignature);
    authHeaders.put("X-Request-Ref", UUID.randomUUID().toString());
    authHeaders.put("aaccept", "application/vnd.api+json");
    authHeaders.put("Content-Type", "application/json");

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
