package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BICOrderCreation extends ECETestBase {

  Map<?, ?> loadYaml = null;
  Map<?, ?> loadRestYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  long startTime, stopTime, executionTime;

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
    LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());
    LinkedHashMap<String, String> restdefaultvalues = (LinkedHashMap<String, String>) loadRestYaml
        .get("default");
    LinkedHashMap<String, String> regionalData = (LinkedHashMap<String, String>) loadYaml
        .get(System.getProperty("store"));
    defaultvalues.putAll(regionalData);
    defaultvalues.putAll(testcasedata);
    defaultvalues.putAll(restdefaultvalues);
    testDataForEachMethod = defaultvalues;
    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);
  }

  @Test(groups = {
      "bic-changePayment-US"}, description = "Validation of BIC change payment details functionality")
  public void validateBICChangePaymentProfile() {
    Util.printInfo("Gathering payment details...");
    String emailID = System.getProperty("email");
    String cepSSAP = System.getProperty("password");

    if (Strings.isNullOrEmpty(emailID)) {
      HashMap<String, String> results = getBicTestBase()
          .createGUACBICOrderUS(testDataForEachMethod);
      emailID = results.get(BICConstants.emailid);
      cepSSAP = "Password1";

      updateTestingHub(results);
      results.putAll(testDataForEachMethod);
      // Trigger Invoice join
      String baseUrl = results.get("postInvoicePelicanAPI");
      results.put("pelican_BaseUrl", baseUrl);
      pelicantb.postInvoicePelicanAPI(results);
      Util.sleep(180000);
    }

    ArrayList<String> payments = new ArrayList<String>();
    payments.add("VISA");
    payments.add("PAYPAL");
    payments.add("ACH");

    String paymentType = System.getProperty("payment");
    payments.remove(paymentType);
    Util.printInfo("Payment Type is : " + paymentType);

    int index = (int) Util.randomNumber(payments.size());

    paymentType = payments.get(index);
    testDataForEachMethod.put("paymentType", paymentType);

    portaltb.openPortalBICLaunch(testDataForEachMethod.get("cepURL"));

    if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
      portaltb.portalLogin(emailID, cepSSAP);
    }
    String[] paymentCardDetails = getBicTestBase().getPaymentDetails(paymentType.toUpperCase())
        .split("@");
    portaltb.changePaymentMethodAndValidate(testDataForEachMethod, paymentCardDetails);
  }

  @Test(groups = {"bic-nativeorder-US"}, description = "Validation of Create BIC Hybrid Order")
  public void validateBicNativeOrder() {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    Util.sleep(180000);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    String baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    // Trigger Invoice join
    baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put(BICConstants.orderNumberSAP, results.get("orderNumberSAP"));
      testResults.put(BICConstants.orderState, results.get("getPOReponse_orderState"));
      testResults
          .put(BICConstants.fulfillmentStatus, results.get("getPOReponse_fulfillmentStatus"));
      testResults.put(BICConstants.fulfillmentDate, results.get("getPOReponse_fulfillmentDate"));
      testResults.put(BICConstants.subscriptionId, results.get("getPOReponse_subscriptionId"));
      testResults.put(BICConstants.subscriptionPeriodStartDate,
          results.get("getPOReponse_subscriptionPeriodStartDate"));
      testResults.put(BICConstants.subscriptionPeriodEndDate,
          results.get("getPOReponse_subscriptionPeriodEndDate"));
      testResults.put(BICConstants.nextBillingDate, results.get("response_nextBillingDate"));
      testResults
          .put(BICConstants.payment_ProfileId, results.get("getPOReponse_storedPaymentProfileId"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        "Password1", results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

  }

  @Test(groups = {"bic-addseat-native-US"}, description = "Validation of BIC Add Seat Order")
  public void validateBicAddSeatNativeOrder() {
    System.out.println("Version 20th April 2021");
    testDataForEachMethod.put("productID", testDataForEachMethod.get("nativeproductID"));
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase().createGUACBICOrderUS(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    // Trigger Invoice join
    String baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    Util.sleep(180000);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Initial order validation in Portal
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        "Password1", results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

    // Place add Seat order in Portal
    results.putAll(
        portaltb.createAndValidateAddSeatOrderInPortal(testDataForEachMethod.get("addSeatQty"),
            testDataForEachMethod));
    testResults.put("addSeatOrderNumber", results.get("addSeatOrderNumber"));
    // testResults.put("addSeatPerSeatGrossAmount",
    // results.get("perSeatGrossAmount"));
    testResults.put("addSeatQty", results.get("addSeatQty"));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb
        .addTokenInResourceUrl(baseUrl, testResults.get(TestingHubConstants.addSeatOrderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    // Trigger Invoice join
    baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);
    Util.sleep(180000);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
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
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);
    Util.sleep(60000);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {
      "bic-guac-addseats"}, description = "Validation Add Seats in GAUC with existing user")
  public void validateBicAddSeats() {
    HashMap<String, String> testResults = new HashMap<String, String>();

    Util.printInfo("Placing initial order");

    HashMap<String, String> results = getBicTestBase()
        .createGUACBic_Orders_US(testDataForEachMethod);

    results.put(BICConstants.nativeOrderNumber + "1", results.get(BICConstants.orderNumber));
    results.remove(BICConstants.orderNumber);
    testDataForEachMethod.putAll(results);
    getBicTestBase().driver.quit();

    ECETestBase tb = new ECETestBase();
    testDataForEachMethod.put("bicNativePriceID", testDataForEachMethod.get("productID"));
    Util.printInfo("Placing second order for the returning user");

    results = tb.getBicTestBase().createBic_ReturningUserAddSeat(testDataForEachMethod);
    results.put(BICConstants.nativeOrderNumber + "2", results.get(BICConstants.orderNumber));
    results.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    String baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    // Verify that a seat was added
    AssertUtils.assertEquals("Subscription should have 2 seats",
        results.get("response_subscriptionQuantity"), "2");

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put("orderState", results.get("getPOReponse_orderState"));
      testResults.put("fulfillmentStatus", results.get("getPOReponse_fulfillmentStatus"));
      testResults.put("fulfillmentDate", results.get("getPOReponse_fulfillmentDate"));
      testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
      testResults.put("subscriptionQuantity", results.get("response_subscriptionQuantity"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-reduceseats-native-US"}, description = "Validation of BIC Reduce Seats")
  public void validateBicReduceSeats() throws MetadataException {
    testDataForEachMethod.put("productID", testDataForEachMethod.get("nativeproductID"));
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBic_Orders_US(testDataForEachMethod);
    Util.sleep(180000);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    String baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Initial order validation in Portal
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        "Password1", results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

    // Reduce seats in Portal
    results.putAll(portaltb.reduceSeatsInPortalAndValidate());
    testResults.put("reducedSeatQty", results.get("reducedSeatQty"));
    updateTestingHub(testResults);

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    // Verify that a seat was reduced
    AssertUtils.assertEquals("Subscription was reduced by 1 seat",
        results.get("response_quantityToReduce"), "1");

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
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
      testResults.put("quantityToReduce", results.get("response_quantityToReduce"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testing Hub");
    }
    updateTestingHub(testResults);
    Util.sleep(60000);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-flexorder-US"}, description = "Validation of Create BIC Flex Order")
  public void validateBicFlexOrder() {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase().createGUACBICOrderUS(testDataForEachMethod);
    Util.sleep(180000);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    String baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    // Trigger Invoice join
    baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put(BICConstants.orderState, results.get("getPOReponse_orderState"));
      testResults
          .put(BICConstants.fulfillmentStatus, results.get("getPOReponse_fulfillmentStatus"));
      testResults.put(BICConstants.fulfillmentDate, results.get("getPOReponse_fulfillmentDate"));
      testResults.put(BICConstants.subscriptionId, results.get("getPOReponse_subscriptionId"));
      testResults.put(BICConstants.subscriptionPeriodStartDate,
          results.get("getPOReponse_subscriptionPeriodStartDate"));
      testResults.put(BICConstants.subscriptionPeriodEndDate,
          results.get("getPOReponse_subscriptionPeriodEndDate"));
      testResults.put(BICConstants.nextBillingDate, results.get("response_nextBillingDate"));
      testResults
          .put(BICConstants.payment_ProfileId, results.get("getPOReponse_storedPaymentProfileId"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        "Password1", results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

    // Validate Sumbit Order
    tibcotb.validateSubmitOrder(results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Validate Create Order
    tibcotb.waitTillProcessCompletes(results.get(BICConstants.orderNumber), "CreateOrder");
    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"trialDownload-UI"}, description = "Testing Download Trial version")
  public void validateTrialDownloadUI() {
    HashMap<String, String> testResults = new HashMap<String, String>();

    try {
      testResults.put(BICConstants.emailid, System.getProperty("email"));
      testResults = getBicTestBase().testCjtTrialDownloadUI(testDataForEachMethod);
      updateTestingHub(testResults);
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo("Error " + e.getMessage());
      AssertUtils.fail("Unable to test trial downloads");
      testResults.put(BICECEConstants.DOWNLOAD_STATUS, "Failed");
      updateTestingHub(testResults);
    } finally {
      updateTestingHub(testResults);
    }
  }

  @Test(groups = {
      "bic-nativeorder-moe-US"}, description = "Validation of Create BIC Order from MOE")
  public void validateBicNativeOrderMoe() {
    long startTime, stopTime, executionTime;
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase().createBicOrderMoe(testDataForEachMethod);
    Util.sleep(180000);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    String baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    // Trigger Invoice join
    baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put("orderNumber_SAP", results.get("orderNumberSAP"));
      testResults.put("orderState", results.get("getPOReponse_orderState"));
      testResults.put("fulfillmentStatus", results.get("getPOReponse_fulfillmentStatus"));
      testResults.put("fulfillmentDate", results.get("getPOReponse_fulfillmentDate"));
      testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
      testResults.put("subscriptionPeriodStartDate",
          results.get("getPOReponse_subscriptionPeriodStartDate"));
      testResults.put("subscriptionPeriodEndDate",
          results.get("getPOReponse_subscriptionPeriodEndDate"));
      testResults.put("nextBillingDate", results.get("response_nextBillingDate"));
      testResults
          .put("payment_ProfileId", results.get("getPOReponse_storedPaymentProfileId"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        "Password1", results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

    // Validate Submit Order
    tibcotb.validateSubmitOrder(results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Validate Create Order
    tibcotb.waitTillProcessCompletes(results.get(BICConstants.orderNumber), "CreateOrder");
    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-metaorder-US"}, description = "Validation of Create BIC Meta Order")
  public void validateBicMetaOrder() {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();

    HashMap<String, String> results = getBicTestBase().createGUACBICOrderUS(testDataForEachMethod);
    updateTestingHub(results);
    results.putAll(testDataForEachMethod);

    String bicOrderO2ID = getPortalTestBase().getOxygenId(results).trim();
    results.put(BICConstants.oxid, bicOrderO2ID);
    Util.sleep(180000);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    String baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    // Trigger Invoice join
    baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    Util.sleep(180000);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
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
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testing hub.");
    }
    updateTestingHub(testResults);

    // Trigger Invoice join
    baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    // Validate Submit Order
    tibcotb.validateSubmitOrder(results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Portal
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        "Password1", results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

    // Validate Create Order
    tibcotb.waitTillProcessCompletes(results.get(BICConstants.orderNumber), "CreateOrder");
    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-indirectorder-JP"}, description = "Validation of Create BIC Indirect Order")
  public void validateBicIndirectOrder() {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICIndirectOrderJP(testDataForEachMethod);
    Util.sleep(60000);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
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
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);
    Util.sleep(60000);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-returningUser-US"}, description = "Validation of Create BIC Hybrid Order")
  public void validateBicReturningUser() {
    HashMap<String, String> testResults = new HashMap<String, String>();

    testDataForEachMethod.put("productID", testDataForEachMethod.get("productID"));
    Util.printInfo("Placing initial order.");

    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderUS(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    updateTestingHub(testResults);

    results.put(BICConstants.nativeOrderNumber + "1", results.get(BICConstants.orderNumber));
    results.remove(BICConstants.orderNumber);
    updateTestingHub(results);
    testDataForEachMethod.putAll(results);
    Util.sleep(600000);
    getBicTestBase().driver.quit();

    ECETestBase tb = new ECETestBase();
    testDataForEachMethod.put("bicNativePriceID", testDataForEachMethod.get("productID"));
    Util.printInfo("Placing second order for the returning user.");

    results = tb.getBicTestBase().createBICReturningUser(testDataForEachMethod);
    results.put(BICConstants.nativeOrderNumber + "2", results.get(BICConstants.orderNumber));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    updateTestingHub(results);
    results.putAll(testDataForEachMethod);

    String bicOrderO2ID = "";
    OxygenService os = new OxygenService();
    try {
      bicOrderO2ID = os
          .getOxygenID(results.get(BICConstants.emailid), results.get("password"));
      results.put(BICConstants.oxid, bicOrderO2ID);
    } catch (Exception e1) {
    }

    results.putAll(testDataForEachMethod);

    // trigger Invoice join
    String baseUrl = results.get("postInvoicePelicanAPI");
    results.put("pelican_BaseUrl", baseUrl);
    pelicantb.postInvoicePelicanAPI(results);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    baseUrl = results.get("getPurchaseOrderDetails");
    baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    baseUrl = results.get("getSubscriptionById");
    baseUrl = pelicantb
        .addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
    results.put("pelican_BaseUrl", baseUrl);
    results.putAll(pelicantb.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put("orderState", results.get("getPOReponse_orderState"));
      testResults.put("fulfillmentStatus", results.get("getPOReponse_fulfillmentStatus"));
      testResults.put("fulfillmentDate", results.get("getPOReponse_fulfillmentDate"));
      testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
      testResults.put("subscriptionPeriodStartDate",
          results.get("getPOReponse_subscriptionPeriodStartDate"));
      testResults.put("subscriptionPeriodEndDate",
          results.get("getPOReponse_subscriptionPeriodEndDate"));
      testResults.put("nextBillingDate", results.get("response_nextBillingDate"));
      testResults
          .put("payment_ProfileId", results.get("getPOReponse_storedPaymentProfileId"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testing hub.");
    }
    updateTestingHub(testResults);
    Util.sleep(60000);

    // Initial order validation in Portal
    tb.getPortalTestBase().validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid), "Password1",
        results.get("getPOReponse_subscriptionId"));
    updateTestingHub(testResults);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  /**
   * Test the align billing functionality in portal. Place two orders for the same product without
   * adding seats to create two separate subscriptions. Since the subscriptions are placed
   * sequentially, they will expire on the same day and you wouldn't be able to align them. After
   * the orders are placed, we use pelican's patch api to advance the renewal date of the second
   * subscription so that the 2 subscriptions are unaligned. Next, we automate portal's UI to
   * realign the billing dates and assert that the dates match.
   */
  @Test(groups = {"bic-align-billing"}, description = "Validation of align billing")
  public void validateAlignBilling() {
    HashMap<String, String> testResults = new HashMap<String, String>();

    Util.printInfo("Placing initial order");

    // Place the first order
    HashMap<String, String> results = getBicTestBase()
        .createGUACBic_Orders_US(testDataForEachMethod);

    results.put(BICConstants.nativeOrderNumber + "1", results.get(BICConstants.orderNumber));
    testDataForEachMethod.putAll(results);
    getBicTestBase().driver.manage().deleteAllCookies();

    // Get the subscription id for the first order
    String basePelicanOrderUrl = testDataForEachMethod.get("getPurchaseOrderDetails");
    String pelicanOrderUrl = pelicantb
        .addTokenInResourceUrl(basePelicanOrderUrl, results.get(BICConstants.orderNumber));
    testDataForEachMethod.put("pelican_BaseUrl", pelicanOrderUrl);
    results.putAll(getBicTestBase()
        .getPurchaseOrderDetails(pelicantb.getPelicanResponse(testDataForEachMethod)));
    results.put("sub1ID", results.get("getPOReponse_subscriptionId"));

    // Get the original billing date for the first subscription
    String basePelicanSubscriptionUrl = testDataForEachMethod.get("getSubscriptionById");
    String pelicanSubscriptionUrl = pelicantb.addTokenInResourceUrl(basePelicanSubscriptionUrl,
        results.get("getPOReponse_subscriptionId"));
    testDataForEachMethod.put("pelican_BaseUrl", pelicanSubscriptionUrl);
    results.putAll(pelicantb.getSubscriptionById(testDataForEachMethod));
    results.put("sub1NextBillingDate", results.get("response_nextBillingDate"));

    Util.printInfo("Placing second order for the returning user");

    // Placing the second order for the second subscription
    results.putAll(getBicTestBase().createBICReturningUserLoggedIn(testDataForEachMethod));
    results.put(BICConstants.nativeOrderNumber + "2", results.get(BICConstants.orderNumber));

    // Get the subscription id for the second subscription
    pelicanOrderUrl = pelicantb
        .addTokenInResourceUrl(basePelicanOrderUrl, results.get(BICConstants.orderNumber));
    testDataForEachMethod.put("pelican_BaseUrl", pelicanOrderUrl);
    results.putAll(getBicTestBase()
        .getPurchaseOrderDetails(pelicantb.getPelicanResponse(testDataForEachMethod)));
    results.put("sub2ID", results.get("getPOReponse_subscriptionId"));

    // Forcefully update the second subscription's billing date to make it unaligned from the first subscription
    pelicanOrderUrl = pelicantb.addTokenInResourceUrl(basePelicanSubscriptionUrl,
        results.get("getPOReponse_subscriptionId"));
    testDataForEachMethod.put("pelican_BaseUrl", pelicanOrderUrl);
    testDataForEachMethod
        .put("desiredBillingDate", Util.customDate("MM/dd/yyyy", 0, 180, 0) + " 20:13:28 UTC");
    pelicantb.patchNextBillSubscriptionById(testDataForEachMethod);

    // Open up portal UI and align billing between the 2 subscriptions
    portaltb.alignBillingInPortal(testDataForEachMethod.get(TestingHubConstants.cepURL),
        results.get(TestingHubConstants.emailid), "Password1", results.get("sub1ID"),
        results.get("sub2ID"));

    // Get the billing date of the aligned subscription
    pelicanSubscriptionUrl = pelicantb
        .addTokenInResourceUrl(basePelicanSubscriptionUrl, results.get("sub2ID"));
    testDataForEachMethod.put("pelican_BaseUrl", pelicanSubscriptionUrl);
    results.putAll(pelicantb.getSubscriptionById(testDataForEachMethod));
    results.put("sub2NextBillingDate", results.get("response_nextBillingDate"));

    AssertUtils.assertEquals("Billing Dates should be aligned",
        results.get("sub1NextBillingDate").split("\\s")[0],
        results.get("sub2NextBillingDate").split("\\s")[0]);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put("sub1ID", results.get("sub1ID"));
      testResults.put("sub2ID", results.get("sub2ID"));
      testResults.put("sub1NextBillingDate", results.get("sub1NextBillingDate"));
      testResults.put("sub2NextBillingDate", results.get("sub2NextBillingDate"));
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
    updateTestingHub(testResults);
  }

}
