package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.EISTestBase;
import com.autodesk.testinghub.core.utils.LoadJsonWithValue;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * Client for the Payport service
 */
public class PayportTestBase {

  SSLConfig sslConfig;
  HashMap<String, String> testData;

  /**
   * Setup payport client
   *
   * @param testData - Test method configurations
   */
  public PayportTestBase(HashMap<String, String> testData) {
    Util.PrintInfo("PayportTestBase from ECE");

    this.testData = new HashMap<>(testData);

    String certificateConfig = "PAYPORT_CERT_" + GlobalConstants.ENV.toUpperCase();
    String pfxFile = EISTestBase.getTestDataDir()
        + EISTestBase.getTestManifest().getProperty(certificateConfig);
    sslConfig = loadPFXFile(pfxFile);
  }

  /**
   * Renew an expired subscription
   *
   * @param results - Results from order and subscription lookups
   */
  @Step("Renew Purchase" + GlobalConstants.TAG_TESTINGHUB)
  public void renewPurchase(HashMap<String, String> results) {
    String baseUrl = testData.get("payportRenewalURL").replace("USER_ID", results.get(
        "getPOReponse_oxygenID"));

    LinkedHashMap<String, Object> dataSet = new LinkedHashMap<>();
    dataSet.put("email", results.get("emailid"));
    dataSet.put("storeExternalKey", results.get("getPOReponse_storeExternalKey"));
    dataSet.put("paymentProfileId", results.get("getPOReponse_storedPaymentProfileId"));
    dataSet.put("paymentProcessor", results.get("getPOReponse_paymentProcessor"));
    dataSet.put("currency", results.get("currencyStore"));
    dataSet.put("subscriptionID", results.get("getPOReponse_subscriptionId"));
    dataSet.put("quantity", results.get("response_subscriptionQuantity"));
    dataSet.put("unitPrice", results.get("response_nextBillingUnitPrice"));
    dataSet.put("unitPriceWithVAT", results.get("response_nextBillingChargeAmount"));
    dataSet.put("taxCode", results.get("getPOReponse_taxCode"));
    dataSet.put("offerExternalKey", results.get("response_offeringExternalKey"));
    dataSet.put("priceId", results.get("response_nextBillingPriceId"));
    dataSet.put("userExternalKey", results.get("getPOReponse_oxygenID"));

    try {
      ClassLoader classLoader = this.getClass().getClassLoader();
      String jsonFile = Objects.requireNonNull(
          classLoader.getResource("ece/payload/payportRenewPurchase.json")).getPath();
      String payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
      Util.printInfo("The Payport Request Json " + payload);
      Map<String, String> header = new HashMap<>();
      header.put("Content-Type", "application/json");
      Response response = given()
          .config(RestAssured.config().sslConfig(sslConfig))
          .headers(header).body(payload).when().post(baseUrl);
      String result = response.getBody().asString();
      Util.printInfo("Result: " + result);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Load a pfx certificate file to interact with payport
   *
   * @param filename - PFX file path
   * @return - SSL configuration
   */
  private SSLConfig loadPFXFile(String filename) {
    SSLConfig config = null;
    ClassLoader classLoader = this.getClass().getClassLoader();
    try {
      KeyStore keystore = KeyStore.getInstance("PKCS12");
      String certificatePassphrase = ProtectedConfigFile.decrypt(
          testData.get("payportCertificatePassphrase"));
      keystore.load(classLoader.getResourceAsStream(filename), certificatePassphrase.toCharArray());
      SSLSocketFactory clientAuthFactory = new SSLSocketFactory(keystore, certificatePassphrase);
      config = new SSLConfig().with().sslSocketFactory(clientAuthFactory).and()
          .allowAllHostnames();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return config;
  }
}
