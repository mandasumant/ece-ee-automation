package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.LoadJsonWithValue;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
    LinkedHashMap<String, Object> dataSet = new LinkedHashMap<>();
    dataSet.put("email", data.get(BICECEConstants.emailid));
    dataSet.put("firstName", data.get(BICECEConstants.FIRSTNAME));
    dataSet.put("lastName", data.get(BICECEConstants.LASTNAME));
    dataSet.put("currency", data.get(BICECEConstants.currencyStore));
    dataSet.put("addressLine1", address.addressLine1);
    dataSet.put("city", address.city);
    dataSet.put("stateProvinceCode", address.province);
    dataSet.put("postalCode", address.postalCode);
    dataSet.put("countryCode", address.countryCode);

    ClassLoader classLoader = this.getClass().getClassLoader();
    String jsonFile = Objects.requireNonNull(
        classLoader.getResource("ece/payload/createQuoteRequest.json")).getPath();
    return LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
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
    Response response = given()
        .headers(pwsRequestHeaders)
        .body(payloadBody)
        .post("https://" + hostname + "/v1/quotes")
        .then().extract().response();

    String transactionId = response.jsonPath().getString("transactionId");
    Util.printInfo("Quote requested, transactionId: " + transactionId);

    int attempts = 0;

    while (attempts < 10) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = getQuoteStatus(pwsRequestHeaders, transactionId);

      String status = response.jsonPath().getString("status");

      if (status.equals("CREATED")) {
        quoteNumber = response.jsonPath().getString("quoteNumber");
        Util.printInfo("Got quote: " + quoteNumber);
        break;
      } else if (attempts > 10) {
        AssertUtils.fail("Failed to get quote Created");
      } else {
        Util.printInfo("Quote not ready yet, status: " + status);
      }
    }

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
        Util.printInfo("Quote not ready yet, status: " + status);
      }
    }
    AssertUtils.fail("Failed to change quote status to QUOTED");
    return "";
  }

  public static class PWSAccessInfo {
    public String timestamp;
    public String token;

    public PWSAccessInfo(String timestamp, String token) {
      this.timestamp = timestamp;
      this.token = token;
    }
  }

  private Response getQuoteStatus(Map<String, String> headers, String transactionId) {
    return given()
        .headers(headers)
        .get("https://" + hostname + "/v1/quotes/status?transactionId=" + transactionId)
        .then().extract().response();
  }
}
