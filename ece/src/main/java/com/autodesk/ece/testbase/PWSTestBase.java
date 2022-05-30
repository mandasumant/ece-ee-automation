package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.dto.AgentAccountDTO;
import com.autodesk.ece.dto.AgentContactDTO;
import com.autodesk.ece.dto.EndCustomerDTO;
import com.autodesk.ece.dto.LineItemDTO;
import com.autodesk.ece.dto.OfferDTO;
import com.autodesk.ece.dto.PurchaserDTO;
import com.autodesk.ece.dto.QuoteDTO;
import com.autodesk.ece.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
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
    c.add(Calendar.DATE, 5);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return sdf.format(c.getTime());
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

  private String createQuoteBody(LinkedHashMap<String, String> data, Address address) {
    EndCustomerDTO endCustomer = new EndCustomerDTO(data, address);
    PurchaserDTO purchaser = new PurchaserDTO(data);
    AgentContactDTO agentContact = new AgentContactDTO();
    AgentAccountDTO agentAccount = new AgentAccountDTO(data);
    OfferDTO offer = new OfferDTO(data);
    LineItemDTO lineItem = new LineItemDTO(data);
    List<LineItemDTO> lineItems = new ArrayList<>();
    lineItem.setOffer(offer);
    lineItems.add(lineItem);

    QuoteDTO quote =
        QuoteDTO.builder().lineItems(lineItems).purchaser(purchaser).endCustomer(endCustomer)
            .agentContact(agentContact).agentAccount(agentAccount).currency(data.get(BICECEConstants.currencyStore))
            .currency(data.get(BICECEConstants.currencyStore)).quoteNote(BICECEConstants.QUOTE_NOTES).build();

    return new Gson().toJson(quote);
  }

  @Step("Create and Finalize Quote" + GlobalConstants.TAG_TESTINGHUB)
  public String createAndFinalizeQuote(Address address, LinkedHashMap<String, String> data) {
    PWSAccessInfo access_token = getAccessToken();
    String signature = signString(access_token.token, clientSecret, access_token.timestamp);
    String quoteNumber = null;

    Map<String, String> pwsRequestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.token);
      put("CSN", "0070176510");
      put("signature", signature);
      put("timestamp", access_token.timestamp);
    }};

    String payloadBody = createQuoteBody(data, address);
    Util.printInfo("Create Quote Payload: " + payloadBody);
    Response response = given()
        .headers(pwsRequestHeaders)
        .body(payloadBody)
        .post("https://" + hostname + "/v1/quotes")
        .then().extract().response();
    response.prettyPrint();

    String transactionId = response.jsonPath().getString("transactionId");
    Util.printInfo("Quote requested, transactionId: " + transactionId);

    int attempts = 0;

    while (attempts < 20) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = getQuoteStatus(pwsRequestHeaders, transactionId);

      String status = response.jsonPath().getString("status");

      if (status.equals("CREATED")) {
        quoteNumber = response.jsonPath().getString("quoteNumber");
        Util.printInfo("Got quote in CREATED status, quote number: " + quoteNumber);
        break;
      } else if (attempts >= 19) {
        AssertUtils.fail("Retry exhausted: Failed to get quote in Created");
      } else {
        Util.printInfo("Quote not created yet, status: " + status);
      }
    }

    Util.sleep(30000);

    return finalizeQuote(quoteNumber, transactionId);
  }

  @Step("Finalize Quote" + GlobalConstants.TAG_TESTINGHUB)
  private String finalizeQuote(String quoteId, String transactionId) {
    PWSAccessInfo access_token = getAccessToken();
    String signature = signString(access_token.token, clientSecret, access_token.timestamp);

    Map<String, String> pwsRequestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.token);
      put("CSN", "0070176510");
      put("signature", signature);
      put("timestamp", access_token.timestamp);
    }};

    Util.printInfo("Finalizing quote");
    Response response = given()
        .headers(pwsRequestHeaders)
        .patch("https://" + hostname + "/v1/quotes/finalize/" + quoteId)
        .then().extract().response();

    if (response.getStatusCode() != 202) {
      AssertUtils.fail("Finalize Quote API returned a non 202 response. Response code: " + response.getStatusCode());
    }

    int attempts = 0;

    while (attempts < 10) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = getQuoteStatus(pwsRequestHeaders, transactionId);

      String status = response.jsonPath().getString("status");

      if (status.equals("QUOTED")) {
        Util.printInfo("Got quote in QUOTED state: " + quoteId);
        return quoteId;
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

  public static class PWSAccessInfo {
    public String timestamp;
    public String token;

    public PWSAccessInfo(String timestamp, String token) {
      this.timestamp = timestamp;
      this.token = token;
    }
  }
}
