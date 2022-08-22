package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.utilities.Address;
import com.autodesk.ece.utilities.PelicanRequestSigner;
import com.autodesk.ece.utilities.PelicanRequestSigner.PelicanSignature;
import com.autodesk.platformautomation.ApiClient;
import com.autodesk.platformautomation.ApiException;
import com.autodesk.platformautomation.Configuration;
import com.autodesk.platformautomation.bmse2pelicansubscriptionv3.SubscriptionControllerApi;
import com.autodesk.platformautomation.bmse2pelicansubscriptionv3.models.SubscriptionSuccess;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.testng.Assert;

public class PelicanTestBase {

  private final PelicanRequestSigner requestSigner = new PelicanRequestSigner();

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
        results.put("response_offeringType", String.valueOf(result.getData().getOfferingType()));
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

  @Step("Get Purchase Order V4 API" + GlobalConstants.TAG_TESTINGHUB)
  public String getPurchaseOrderV4(HashMap<String, String> data) {
    String getPurchaseOrderDetailsUrl = data.get("getPurchaseOrderDetailsV4Url");
    getPurchaseOrderDetailsUrl = addTokenInResourceUrl(getPurchaseOrderDetailsUrl,
        data.get(BICECEConstants.orderNumber));
    Util.printInfo("Get Purchase Order V4 Request URL: " + getPurchaseOrderDetailsUrl);

    String Content_Type = BICECEConstants.APPLICATION_JSON;
    PelicanSignature signature = requestSigner.generateSignature();

    Map<String, String> header = new HashMap<>();
    header.put(BICECEConstants.X_E2_HMAC_SIGNATURE, signature.xE2HMACSignature);
    header.put(BICECEConstants.X_E2_PARTNER_ID, signature.xE2PartnerId);
    header.put(BICECEConstants.X_E2_APPFAMILY_ID, signature.xE2AppFamilyId);
    header.put(BICECEConstants.X_E2_HMAC_TIMESTAMP, signature.xE2HMACTimestamp);
    header.put(BICECEConstants.CONTENT_TYPE, Content_Type);

    Response response = getRestResponse(getPurchaseOrderDetailsUrl, header, null);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);

    return result;
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

    Response response = createRefundOrder(getPurchaseOrderDetailsUrl, header);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);
  }

  @Step("Refund O2P Orders" + GlobalConstants.TAG_TESTINGHUB)
  public void createRefundOrderV4(HashMap<String, String> data) {
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

    Response response = createRefundOrder(refundPurchaseOrderV4Url, header);
    String result = response.getBody().asString();
    Util.PrintInfo(BICECEConstants.RESULT + result);
  }

  @Step("Renew Pelican Subscription" + GlobalConstants.TAG_TESTINGHUB)
  public void renewSubscription(HashMap<String, String> data) {
    String pelicanRenewSubscriptionUrl = data.get("pelicanRenewalURL");
    String getRenewSubscriptionUrl = addTokenInResourceUrl(pelicanRenewSubscriptionUrl,
        data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));
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
    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to get Purchase Order Details from Order Service");
    }

    return results;
  }

  @Step("Subscription : Getting purchase order details from Order Service V4 API" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getPurchaseOrderV4Details(String poResponse) {
    HashMap<String, String> results = new HashMap<>();
    JsonPath jp = new JsonPath(poResponse);
    try {
      results.put("getPOResponse_origin", jp.get("origin").toString());
      results.put(BICECEConstants.ORDER_ID, jp.get("id").toString());
      results.put("getPOResponse_quoteId", jp.get("quoteId") == null ? "" : jp.get("quoteId").toString());
      results.put("getPOResponse_salesChannelType", jp.get("salesChannelType").toString());
      results.put("getPOResponse_orderState", jp.get("orderState").toString());

      results.put("getPOResponse_storedPaymentProfileId",
          jp.get("payment.paymentProfileId").toString());
      results.put("getPOResponse_productType", jp.get("lineItems[0].offering.name").toString());
      results.put("getPOResponse_quantity", jp.get("lineItems[0].quantity").toString());
      results.put("getPOResponse_offeringId", jp.get("lineItems[0].offering.id").toString());

      results.put("getPOResponse_fulfillmentStatus",
          jp.get("lineItems[0].fulfillmentStatus").toString());
      results.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
          jp.get("lineItems[0].subscriptionInfo.subscriptionId").toString());
      results.put("getPOResponse_subscriptionPeriodStartDate",
          jp.get("lineItems[0].subscriptionInfo.subscriptionPeriodStartDate").toString());
      results.put("getPOResponse_subscriptionPeriodEndDate",
          jp.get("lineItems[0].subscriptionInfo.subscriptionPeriodEndDate").toString());
      results.put("getPOResponse_fulfillmentDate",
          jp.get("lineItems[0].fulfillmentDate").toString());
      results.put("getPOResponse_paymentProcessor",
          jp.get("payment.paymentProcessor").toString());

      results.put("getPOResponse_endCustomer_company",
          jp.get("endCustomer.company"));
      results.put("getPOResponse_endCustomer_firstName",
          jp.get("endCustomer.firstName"));
      results.put("getPOResponse_endCustomer_lastName",
          jp.get("endCustomer.lastName"));
      results.put("getPOResponse_endCustomer_addressLine1",
          jp.get("endCustomer.addressLine1"));
      results.put("getPOResponse_endCustomer_city",
          jp.get("endCustomer.city"));
      results.put("getPOResponse_endCustomer_state",
          jp.get("endCustomer.state"));
      results.put("getPOResponse_endCustomer_country",
          jp.get("endCustomer.country"));
      results.put("getPOResponse_endCustomer_postalCode",
          jp.get("endCustomer.postalCode"));
      results.put("getPOResponse_endCustomer_accountCsn",
          jp.get("endCustomer.accountCsn"));
      results.put("getPOResponse_endCustomer_contactCsn",
          jp.get("endCustomer.contactCsn"));

      results.put("getPOResponse_billingAddress_firstName",
          jp.get("billingAddress.firstName"));
      results.put("getPOResponse_billingAddress_lastName",
          jp.get("billingAddress.lastName"));
      results.put("getPOResponse_billingAddress_addressLine1",
          jp.get("billingAddress.addressLine1"));
      results.put("getPOResponse_billingAddress_city",
          jp.get("billingAddress.city"));
      results.put("getPOResponse_billingAddress_accountCsn",
          jp.get("billingAddress.accountCsn") != null ? jp.get("billingAddress.accountCsn").toString() : null);
      results.put("getPOResponse_billingAddress_contactCsn",
          jp.get("billingAddress.contactCsn") != null ? jp.get("billingAddress.contactCsn").toString() : null);

      if (jp.get("agentAccount") != null) {
        results.put("getPOResponse_agentAccount_firstName",
            jp.get("agentAccount.firstName"));
        results.put("getPOResponse_agentAccount_lastName",
            jp.get("agentAccount.lastName"));
        results.put("getPOResponse_agentAccount_addressLine1",
            jp.get("agentAccount.addressLine1"));
        results.put("getPOResponse_agentAccount_city",
            jp.get("agentAccount.city"));
        results.put("getPOResponse_agentAccount_accountCsn",
            jp.get("agentAccount.accountCsn"));
        results.put("getPOResponse_agentAccount_contactCsn",
            jp.get("agentAccount.contactCsn"));
      } else {
        results.put("getPOResponse_agentAccount", null);
      }

      if (jp.get("agentContact") != null) {
        results.put("getPOResponse_agentContact_firstName",
            jp.get("agentContact.firstName"));
        results.put("getPOResponse_agentContact_lastName",
            jp.get("agentContact.lastName"));
        results.put("getPOResponse_agentContact_addressLine1",
            jp.get("agentContact.addressLine1"));
        results.put("getPOResponse_agentContact_city",
            jp.get("agentContact.city"));
        results.put("getPOResponse_agentContact_accountCsn",
            jp.get("agentContact.accountCsn"));
        results.put("getPOResponse_agentContact_contactCsn",
            jp.get("agentContact.contactCsn"));
      } else {
        results.put("getPOResponse_agentContact", null);
      }

      results.put("getPOResponse_oxygenID", jp.get("purchaser.oxygenId").toString());
      results.put(BICECEConstants.SUBTOTAL_WITH_TAX, jp.get("price.totalPrice").toString());
    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to get Purchase Order Details from Order Service V4 API" + e.getMessage());
    }
    return results;
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
          pelicanResponseMap.get("getPOResponse_endCustomer_city").substring(0,3).toUpperCase(), address.city.substring(0,3).toUpperCase());
    }
    if (address.province != null && !address.province.isEmpty()) {
      AssertUtils.assertEquals("EndCustomer State should match.",
          pelicanResponseMap.get("getPOResponse_endCustomer_state").toUpperCase(), address.province.toUpperCase());
    }
    AssertUtils.assertEquals("EndCustomer Country should match.",
        pelicanResponseMap.get("getPOResponse_endCustomer_country").toUpperCase(), address.countryCode.toUpperCase());
   if(!address.postalCode.equals("N/A")) {
     AssertUtils.assertEquals("EndCustomer Postal Code should match.",
         pelicanResponseMap.get("getPOResponse_endCustomer_postalCode").substring(0, 3),
         address.postalCode.substring(0, 3));
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
      AssertUtils.assertEquals("Subscription Start Date should should match.", quoteSubDate, pelicanSubDate);

    } catch (Exception e) {
      Util.printInfo("Exception in validating subscription start date . " + e.getMessage());
    }
  }
}
