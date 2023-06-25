package com.autodesk.eceapp.testbase.ece.service.quote;

import static io.restassured.RestAssured.given;
import com.autodesk.eceapp.dto.PWSAccessInfo;
import com.autodesk.eceapp.dto.QuoteDetails;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.utilities.Address;
import io.restassured.http.ContentType;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public interface QuoteService {
  String createAndFinalizeQuote(
      Address address,
      String csn,
      String agentContactEmail,
      LinkedHashMap<String, String> data,
      Boolean isMultiLineItem);
  QuoteDetails getQuoteDetails(String agentCSN, String quoteNo);

  static String getQuoteStartDateAsString() {
    String timezone = "UTC";
    if (System.getProperty("timezone") != null && !System.getProperty("timezone").isEmpty()) {
      timezone = System.getProperty("timezone");
    }
    final SimpleDateFormat sdf = new SimpleDateFormat(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE);
    Calendar c = Calendar.getInstance();
    c.setTime(new Date());
    c.add(Calendar.DATE, 1);

    sdf.setTimeZone(TimeZone.getTimeZone(timezone));
    return sdf.format(c.getTime());
  }

  static PWSAccessInfo getAccessToken(final String clientId, final String clientSecret, final String hostname) {
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

  static String signString(String string, String secret, String timestamp) {
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
}
