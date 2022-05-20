package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.ece.testbase.BICTestBase.Names;
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

  private String createQuoteBody(String email, Names names, Address address, String currency) {
    LinkedHashMap<String, Object> dataSet = new LinkedHashMap<>();
    dataSet.put("email", email);
    dataSet.put("firstName", names.firstName);
    dataSet.put("lastName", names.lastName);
    dataSet.put("currency", currency);
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

  @Step("Create quote" + GlobalConstants.TAG_TESTINGHUB)
  public String createQuote(String email, Names names, Address address, String currency) {
    PWSAccessInfo access_token = getAccessToken();
    String signature = signString(access_token.token, clientSecret, access_token.timestamp);

    Map<String, String> pwsRequestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.token);
      put("CSN", "0070176510");
      put("signature", signature);
      put("timestamp", access_token.timestamp);
    }};

    String payloadBody = createQuoteBody(email, names, address, currency);
    Response response = given()
        .headers(pwsRequestHeaders)
        .body(payloadBody)
        .post("https://" + hostname + "/v1/quotes")
        .then().extract().response();

    String transactionId = response.jsonPath().getString("transactionId");
    Util.printInfo("Quote requested, transactionId: " + transactionId);

    int attempts = 0;

    while (attempts < 8) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = given()
          .headers(pwsRequestHeaders)
          .get("https://" + hostname + "/v1/quotes/status?transactionId=" + transactionId)
          .then().extract().response();

      String status = response.jsonPath().getString("status");

      if (status.equals("CREATED")) {
        String quoteNumber = response.jsonPath().getString("quoteNumber");
        Util.printInfo("Got quote: " + quoteNumber);
        return quoteNumber;
      } else {
        Util.printInfo("Quote not ready yet, status: " + status);
      }
    }
    AssertUtils.fail("Failed to get quote number");
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
}
