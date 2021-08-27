package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BICOrderPromoCode extends ECETestBase {

  Map<?, ?> loadYaml = null;
  Map<?, ?> loadRestYaml = null;
  long startTime, stopTime, executionTime;
  LinkedHashMap<String, String> testDataForEachMethod = null;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String restFileKey = "REST_" + GlobalConstants.ENV.toUpperCase();
    loadRestYaml = YamlUtil.loadYmlUsingTestManifest(restFileKey);
  }

  @BeforeMethod(alwaysRun = true)

  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testCaseData = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());
    LinkedHashMap<String, String> restDefaultValues = (LinkedHashMap<String, String>) loadRestYaml
        .get("default");
    LinkedHashMap<String, String> regionalData = (LinkedHashMap<String, String>) loadYaml
        .get(System.getProperty("store"));
    defaultValues.putAll(regionalData);
    defaultValues.putAll(testCaseData);
    defaultValues.putAll(restDefaultValues);
    testDataForEachMethod = defaultValues;
    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);
  }

  @Test(groups = {"bic-promocode-order"}, description = "Validation of Create BIC Hybrid Order")
  public void promocodeBicOrder() {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBic_Orders_PromoCode(testDataForEachMethod);

    results.putAll(testDataForEachMethod);

    testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
    testResults.put(TestingHubConstants.orderNumber, results.get(TestingHubConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    String baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb
        .addTokenInResourceUrl(baseUrl, results.get(TestingHubConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    String promotionDiscount = results.get("getPOResponse_promotionDiscount");

    AssertUtils.assertTrue(Float.parseFloat(promotionDiscount) > 0, "Promotion was applied");

    // Trigger Invoice join
    baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    try {
      testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
      testResults
          .put(TestingHubConstants.orderNumber, results.get(TestingHubConstants.orderNumber));
      testResults.put("orderState", results.get("getPOReponse_orderState"));
      testResults.put("fulfillmentStatus", results.get("getPOReponse_fulfillmentStatus"));
      testResults.put("fulfillmentDate", results.get("getPOReponse_fulfillmentDate"));
      testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
      testResults.put("subscriptionPeriodStartDate",
          results.get("getPOReponse_subscriptionPeriodStartDate"));
      testResults
          .put("subscriptionPeriodEndDate", results.get("getPOReponse_subscriptionPeriodEndDate"));
      testResults.put("nextBillingDate", results.get("response_nextBillingDate"));
      testResults.put("payment_ProfileId", results.get("getPOReponse_storedPaymentProfileId"));
      testResults.put("priceBeforePromo", results.get("priceBeforePromo"));
      testResults.put("priceAfterPromo", results.get("priceAfterPromo"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);

    portaltb.validateBICOrderProductInCEP(results.get(TestingHubConstants.cepURL),
        results.get(TestingHubConstants.emailid), "Password1",
        results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

    // Validate Create Order
    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }
}
