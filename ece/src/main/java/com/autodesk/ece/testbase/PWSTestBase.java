package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.ece.dto.AgentAccountDTO;
import com.autodesk.ece.dto.AgentContactDTO;
import com.autodesk.ece.dto.EndCustomerDTO;
import com.autodesk.ece.dto.FinalizeQuoteDTO;
import com.autodesk.ece.dto.LineItemDTO;
import com.autodesk.ece.dto.OfferDTO;
import com.autodesk.ece.dto.PurchaserDTO;
import com.autodesk.ece.dto.QuoteDTO;
import com.autodesk.eceapp.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.eseapp.sfdc.SFDCAPI;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.google.gson.Gson;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PWSTestBase {

  private final String clientId;
  private final String clientSecret;
  private final String hostname;

  public PWSTestBase(String clientId, String clientSecret, String hostname) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.hostname = hostname;
  }

  public static String getQuoteStartDateAsString() {
    final SimpleDateFormat sdf = new SimpleDateFormat(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE);
    Calendar c = Calendar.getInstance();
    c.setTime(new Date());
    c.add(Calendar.DATE, 1);
    String timezone = "UTC";

    if (System.getProperty("timezone") != null && !System.getProperty("timezone").isEmpty()) {
      timezone = System.getProperty("timezone");
    }

    sdf.setTimeZone(TimeZone.getTimeZone(timezone));
    return sdf.format(c.getTime());
  }

  private String createQuoteBody(LinkedHashMap<String, String> data, Address address, String csn,
      String agentContactEmail, Boolean isMultiLineItem) {
    PurchaserDTO purchaser = new PurchaserDTO(data);

    if (System.getProperty(BICECEConstants.ENVIRONMENT).equals(BICECEConstants.ENV_STG)) {
      agentContactEmail = agentContactEmail.replace(BICECEConstants.ENV_INT.toLowerCase(),
          BICECEConstants.ENV_STG.toLowerCase()).replace("@", "_2@").replace("_1", "");
    }

    AgentContactDTO agentContact = new AgentContactDTO(agentContactEmail);
    AgentAccountDTO agentAccount = new AgentAccountDTO(csn);
    OfferDTO offer = new OfferDTO(data);
    LineItemDTO lineItem = new LineItemDTO(data);
    List<LineItemDTO> lineItems = new ArrayList<>();
    lineItem.setOffer(offer);
    lineItems.add(lineItem);
    if (isMultiLineItem) {
      if (System.getProperty("quantity2") != null) {
        data.put(BICECEConstants.FLEX_TOKENS, System.getProperty("quantity2"));
      } else {
        data.put(BICECEConstants.FLEX_TOKENS, "4000");
      }
      LineItemDTO lineItemTwo = new LineItemDTO(data);
      lineItemTwo.setOffer(offer);
      lineItems.add(lineItemTwo);
    }

    EndCustomerDTO endCustomer = null;
    if (System.getProperty("existingCSN") != null) {
      endCustomer = new EndCustomerDTO(System.getProperty("existingCSN"));
    } else if (data.get(BICECEConstants.PAYER_CSN) != null) {
      endCustomer = new EndCustomerDTO(data.get(BICECEConstants.PAYER_CSN));
    } else {
      endCustomer = new EndCustomerDTO(address);
    }

    QuoteDTO quote =
        QuoteDTO.builder().lineItems(lineItems).purchaser(purchaser).endCustomer(endCustomer)
            .agentContact(agentContact).agentAccount(agentAccount).currency(data.get(BICECEConstants.currencyStore))
            .quoteNote(BICECEConstants.QUOTE_NOTES).build();

    return new Gson().toJson(quote);
  }

  private String createQuoteFinalizeBody(String quoteNo, String agentCsn,
      String agentContactEmail) {
    AgentContactDTO agentContact = new AgentContactDTO(agentContactEmail);
    AgentAccountDTO agentAccount = new AgentAccountDTO(agentCsn);

    FinalizeQuoteDTO finalizeQuote =
        FinalizeQuoteDTO.builder().quoteNumber(quoteNo).agentContact(agentContact).agentAccount(agentAccount)
            .build();

    return new Gson().toJson(finalizeQuote);
  }

  public PWSAccessInfo getAccessToken() {
    String base64_header = new String(Base64.getEncoder().encode((clientId + ":" + clientSecret).getBytes()));
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    String timestamp = String.valueOf(cal.getTimeInMillis() / 1000);
    String signature = signString(clientId, clientSecret, timestamp);

    String access_token = given()
        .contentType(ContentType.JSON)
        .header("signature", signature)
        .header("timestamp", timestamp)
        .header("Authorization", "Basic " + base64_header)
        .header("timezone_city", System.getProperty("timezone"))
        .post("https://" + hostname + "/v2/oauth/generateaccesstoken?grant_type=client_credentials")
        .then().extract().response()
        .jsonPath().getString("access_token");

    return new PWSAccessInfo(timestamp, access_token);
  }

  private String signString(String string, String secret, String timestamp) {
    String callback = "www.autodesk.com";
    String base_str = callback + string + timestamp;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
      mac.init(keySpec);
      byte[] signatureBytes = mac.doFinal(base_str.getBytes(StandardCharsets.UTF_8));
      return new String(Base64.getEncoder().encode(signatureBytes));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      e.printStackTrace();
    }
    return "";
  }

  @Step("Create and Finalize Quote" + GlobalConstants.TAG_TESTINGHUB)
  public String createAndFinalizeQuote(Address address, String csn,
      String agentContactEmail, LinkedHashMap<String, String> data, Boolean isMultiLineItem) {
    PWSAccessInfo access_token = getAccessToken();
    String signature = signString(access_token.token, clientSecret, access_token.timestamp);
    String quoteNumber = null;

    Map<String, String> pwsRequestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.token);
      put("CSN", csn);
      put("signature", signature);
      put("timestamp", access_token.timestamp);
      put("timezone_city", System.getProperty("timezone"));
    }};

    String payloadBody = createQuoteBody(data, address, csn, agentContactEmail, isMultiLineItem);
    Util.printInfo("Create Quote Payload: " + payloadBody);
    Util.printInfo("Headers: " + pwsRequestHeaders);

    Response response = given()
        .headers(pwsRequestHeaders)
        .body(payloadBody)
        .post("https://" + hostname + "/v1/quotes")
        .then().extract().response();

    Util.printInfo("Quote Creation Response : " + response.prettyPrint());

    String transactionId = response.jsonPath().getString("transactionId");
    Util.printInfo("Quote requested, transactionId: " + transactionId);

    int attempts = 0;

    while (attempts < 20) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = getQuoteStatus(pwsRequestHeaders, transactionId);
      Util.printInfo("Quote Creation Response : " + response.prettyPrint());
      String status = response.jsonPath().getString("quoteStatus") == null ? "error" :
          response.jsonPath().getString("quoteStatus");

      if (status.equals("DRAFT-CREATED")) {
        quoteNumber = response.jsonPath().getString("quoteNumber");
        Util.printInfo("Got quote in DRAFT-CREATED status, quote number: " + quoteNumber);
        break;
      } else if (status.equals("FAILED")) {
        Util.printError(response.jsonPath().getJsonObject("error").toString());
        AssertUtils.fail("Quote creation failed, error: " + response.jsonPath().getString("error.message"));
      } else if (attempts >= 19) {
        AssertUtils.fail("Retry exhausted: Failed to get quote in Created");
      } else {
        Util.printInfo("Quote not created yet, status: " + status);
      }
    }

    Util.printInfo("Getting Quote Details");
    Response res = getQuoteDetails(csn, quoteNumber);
    Util.printInfo("The Quote Details : " + res.prettyPrint());
    Util.sleep(30000);
    return finalizeQuote(quoteNumber, transactionId, csn, agentContactEmail);
  }

  public String createAndFinalizeQuote(Address address, String csn, String agentContactEmail,
      LinkedHashMap<String, String> data) {
    return this.createAndFinalizeQuote(address, csn, agentContactEmail, data, false);
  }

  @Step("Finalize Quote" + GlobalConstants.TAG_TESTINGHUB)
  private String finalizeQuote(String quoteId, String transactionId, String agentCsn, String agentContactEmail) {
    PWSAccessInfo access_token = getAccessToken();
    String signature = signString(access_token.token, clientSecret, access_token.timestamp);

    Map<String, String> pwsRequestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.token);
      put("CSN", agentCsn);
      put("signature", signature);
      put("timestamp", access_token.timestamp);
      put("timezone_city", System.getProperty("timezone"));
    }};

    String finalizeBody = createQuoteFinalizeBody(quoteId, agentCsn, agentContactEmail);

    Util.sleep(60000);

    Response response = given()
        .body(finalizeBody)
        .headers(pwsRequestHeaders)
        .put("https://" + hostname + "/v1/quotes/finalize")
        .then().extract().response();

    Util.printInfo("The finalize response headers: " + response.getHeaders());

    if (response.getStatusCode() != 202) {
      AssertUtils.fail("Finalize Quote API returned a non 202 response. Response code: " + response.getStatusCode());
    }

    int attempts = 0;

    while (attempts < 10) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = getQuoteStatus(pwsRequestHeaders, transactionId);
      Util.printInfo("Quote Finalization Response :" + response.prettyPrint());
      String status = response.jsonPath().getString("quoteStatus");

      if (status.equals("QUOTED")) {
        Util.printInfo("Got quote in QUOTED state: " + quoteId);
        return quoteId;
      } else if (status.equals("FAILED") || status.equals("DRAFT-CREATED")) {
        if (response.jsonPath().getJsonObject("error") != null) {
          Util.printError(response.jsonPath().getJsonObject("error").toString());
          AssertUtils.fail("Quote finalization failed");
        }
      } else if (status.equals("UNDER-REVIEW") || (status.equals("FINALIZING") && attempts == 5)) {
        SFDCAPI sfdcApi = new SFDCAPI();
        Response quoteDetails = getQuoteDetails(agentCsn, quoteId);
        String accountName = quoteDetails.jsonPath().getString("endCustomer.name");
        String accountCSN = quoteDetails.jsonPath().getString("endCustomer.accountCsn");
        String streetAddress = quoteDetails.jsonPath().getString("endCustomer.addressLine1");
        String city = quoteDetails.jsonPath().getString("endCustomer.city");
        String countryCode = quoteDetails.jsonPath().getString("endCustomer.countryCode");
        String postalCode = quoteDetails.jsonPath().getString("endCustomer.postalCode");
        Util.printInfo("SFDC Input  : " + countryCode + " " + postalCode + " " + accountName + " " + streetAddress + " "
            + accountCSN);
        boolean publishAccountEC = sfdcApi.publishAccountEC(accountName, accountCSN, streetAddress, city, countryCode,
            postalCode);
        if (!publishAccountEC) {
          AssertUtils.fail("Failed to publish account to EC system.");
        }

        Util.sleep(10000); // Waiting for the status change

        Response statusRes = getQuoteStatus(pwsRequestHeaders, transactionId);
        Util.printInfo("Quote Status Response : " + statusRes.prettyPrint());
      } else {
        Util.printInfo("Quote not finalized yet, status: " + status);
      }
    }
    AssertUtils.fail("Failed to change quote status to QUOTED");
    return "";
  }

  private Response getQuoteStatus(Map<String, String> headers, String transactionId) {
    return given()
        .headers(headers)
        .get("https://" + hostname + "/v1/quotes/status?transactionId=" + transactionId)
        .then().extract().response();
  }
  @Step("Get Quote" + GlobalConstants.TAG_TESTINGHUB)
  public Response getQuoteDetails(String agentCSN, String quoteNo) {
    Util.printInfo("Calling Get Quote Details API");
    PWSAccessInfo access_token = getAccessToken();
    String signature = signString(access_token.token, clientSecret, access_token.timestamp);
    String quoteNumber = null;

    Map<String, String> newHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.token);
      put("CSN", agentCSN);
      put("signature", signature);
      put("timestamp", access_token.timestamp);
      put("timezone_city", System.getProperty("timezone"));
    }};

    return given()
        .headers(newHeaders)
        .get("https://" + hostname + "/v1/quotes?quoteNumber=" + quoteNo)
        .then().extract().response();
  }

  public static class PWSAccessInfo {

    public String timestamp;
    public String token;

    public PWSAccessInfo(String timestamp, String token) {
      this.timestamp = timestamp;
      this.token = token;
    }
  }
}
