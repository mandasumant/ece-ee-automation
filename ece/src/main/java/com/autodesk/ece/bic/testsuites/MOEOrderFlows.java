package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.MOETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import io.restassured.path.json.JsonPath;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.util.Strings;


public class MOEOrderFlows extends ECETestBase {

  private static final String defaultLocale = "en_US";
  private static final String defaultTaxOption = "undefined";
  private static final String defaultPriceId = "24038";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);
  String optyName, stage, fulfillment, account, projectCloseDate, email, sku = "";
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  String priceId = System.getProperty(BICECEConstants.PRICE_ID);
  private String PASSWORD;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String localeConfigFile = "LOCALE_CONFIG_" + GlobalConstants.ENV.toUpperCase();
    localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());

    defaultvalues.putAll(testcasedata);
    testDataForEachMethod = defaultvalues;

    if (locale == null || locale.trim().isEmpty()) {
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale", locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    Util.printInfo(
        "Validating the store for the locale :" + locale + " Store: " + System.getProperty(
            BICECEConstants.STORE));

    boolean isValidStore = testDataForEachMethod.get(BICECEConstants.STORE_NAME)
        .equals(System.getProperty(BICECEConstants.STORE));

    if (!isValidStore) {
      AssertUtils.fail("The store  is not supported for the given country/locale : " + locale
          + ". Supported stores  are "
          + testDataForEachMethod.get(BICECEConstants.STORE_NAME));
    }

    if (testDataForEachMethod.get(BICECEConstants.ADDRESS) == null || testDataForEachMethod.get(BICECEConstants.ADDRESS)
        .isEmpty()) {
      Util.printTestFailedMessage("Address not found in the config for the locale: " + locale);
    }

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);

    if (Strings.isNullOrEmpty(System.getProperty("account"))) {
      account = testDataForEachMethod.get("account");
    } else {
      account = System.getProperty("account");
    }

    if (Strings.isNullOrEmpty(System.getProperty("projectCloseDate"))) {
      projectCloseDate = Util.getDateFirstDayNextMonth("MM/dd/yyyy");
      Util.printInfo("project close date : " + projectCloseDate);
    } else {
      projectCloseDate = System.getProperty("projectCloseDate");
    }

    if (Strings.isNullOrEmpty(System.getProperty("email"))) {
      email = testDataForEachMethod.get(TestingHubConstants.emailid);
      Util.printInfo("email : " + email);
    } else {
      email = System.getProperty("email");
    }

    optyName = "MOE Opty" + Util.generateRandom(3).toLowerCase();
    stage = "Stage 1";
    fulfillment = "Direct";
    sku = testDataForEachMethod.get("guacMoeSku");

    testDataForEachMethod.put("optyName", optyName);
    testDataForEachMethod.put("stage", stage);
    testDataForEachMethod.put("fulfillment", fulfillment);
    testDataForEachMethod.put("account", account.trim());
    testDataForEachMethod.put("projectCloseDate", projectCloseDate);
    testDataForEachMethod.put("guacMoeUserEmail", email);

    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));

    if (taxOptionEnabled == null || taxOptionEnabled.trim().isEmpty()) {
      taxOptionEnabled = defaultTaxOption;
    }
    testDataForEachMethod.put("taxOptionEnabled", taxOptionEnabled);

    if (priceId == null || priceId.trim().isEmpty()) {
      priceId = defaultPriceId;
    }
    testDataForEachMethod.put("priceId", priceId);
  }

  @AfterMethod(alwaysRun = true)
  public void afterTestMethod(Method name) {
    optyName = null;
    stage = null;
    fulfillment = null;
    account = null;
    projectCloseDate = null;
    email = null;
  }

  @Test(groups = {
      "bic-nativeorder-moe"}, description = "Validation of Create BIC Order from MOE")
  public void validateBicNativeOrderMoe() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    MOETestBase moetb = new MOETestBase(this.getTestBase(), testDataForEachMethod);

    HashMap<String, String> moeResults = moetb.createBicOrderMoe(testDataForEachMethod);
    moeResults.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    String pelicanResponse = pelicantb.retryGetPurchaseOrder(moeResults, true);

    moeResults.putAll(pelicantb.getPurchaseOrderDetails(pelicanResponse));

    validateTestResults(testResults, moeResults);

    AssertUtils.assertEquals("GUAC MOE Origin is not GUAC_MOE_DIRECT", moeResults.get("getPOReponse_origin"),
        BICECEConstants.GUAC_MOE_ORDER_ORIGIN);

    portaltb.validateBICOrderProductInCEP(moeResults.get(BICConstants.cepURL),
        moeResults.get(BICConstants.emailid), PASSWORD, moeResults.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    validateCreateOrder(testResults);
  }

  @Test(groups = {
      "bic-basicflow-moe"}, description = "Basic flow for MOE with Opportunity ID")
  public void validateMoeOpportunityFlow() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    MOETestBase moetb = new MOETestBase(this.getTestBase(), testDataForEachMethod);

    sfdctb.loginSfdcLightningView();
    sfdctb.clickOnCreateMOEOpty();
    HashMap<String, String> sfdcResults
        = moetb.createGUACOpty(optyName, account, stage, projectCloseDate, fulfillment, sku);
    testDataForEachMethod.put("guacMoeOptyId", sfdcResults.get("opportunityid"));

    HashMap<String, String> results = moetb.createBasicMoeOpptyOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    String pelicanResponse = pelicantb.retryGetPurchaseOrder(results, true);

    results.putAll(pelicantb.getPurchaseOrderDetails(pelicanResponse));

    AssertUtils.assertEquals("GUAC MOE Origin is not GUAC_MOE_DIRECT", results.get("getPOReponse_origin"),
        BICECEConstants.GUAC_MOE_ORDER_ORIGIN);

    validateTestResults(testResults, results);

    validateCreateOrder(testResults);
  }

  @Test(groups = {
      "bic-quotedtc-moe"}, description = "Sales agent sends quote from DTC page")
  public void validateMoeQuoteDtcFlow() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    MOETestBase moetb = new MOETestBase(this.getTestBase(), testDataForEachMethod);
    HashMap<String, String> results = moetb.createQuoteWithoutOppty(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));

    updateTestingHub(testResults);

    validateCreateOrder(testResults);
  }

  @Test(groups = {
      "bic-quoteFlow-moe"}, description = "Validation of Create BIC Order from MOE")
  public void validateMoeQuoteOrderFlow() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    MOETestBase moetb = new MOETestBase(this.getTestBase(), testDataForEachMethod);

    sfdctb.loginSfdcLightningView();
    sfdctb.clickOnCreateMOEOpty();
    HashMap<String, String> sfdcResults
        = moetb.createGUACOpty(optyName, account, stage, projectCloseDate, fulfillment, sku);
    testDataForEachMethod.put("guacMoeOptyId", sfdcResults.get("opportunityid"));

    HashMap<String, String> results = moetb.createBicOrderMoeWithQuote(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    String pelicanResponse = pelicantb.retryGetPurchaseOrder(results, true);

    results.putAll(pelicantb.getPurchaseOrderDetails(pelicanResponse));

    AssertUtils.assertEquals("GUAC MOE Origin is not GUAC_MOE_DIRECT",
        results.get("getPOReponse_origin"),
        BICECEConstants.GUAC_MOE_ORDER_ORIGIN);

    validateTestResultsQuoteId(testResults, results);

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    validateCreateOrder(testResults);
  }

  @Test(groups = {
      "bic-basicFlowDtc-moe"}, description = "Customer submits an order via Copy cart link generated on DTC page")
  public void validateMoeDtcFlow()
      throws MetadataException, IOException, UnsupportedFlavorException {
    HashMap<String, String> testResults = new HashMap<>();
    MOETestBase moetb = new MOETestBase(this.getTestBase(), testDataForEachMethod);

    HashMap<String, String> results = moetb.createBicOrderMoeDTC(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    String pelicanResponse = pelicantb.retryGetPurchaseOrder(results, true);

    results.putAll(pelicantb.getPurchaseOrderDetails(pelicanResponse));

    AssertUtils.assertEquals("GUAC MOE Origin is not GUAC_MOE_DTC", results.get("getPOReponse_origin"),
        BICECEConstants.GUAC_DTC_ORDER_ORIGIN);

    validateTestResults(testResults, results);

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    validateCreateOrder(testResults);
  }

  private void validateTestResults(HashMap<String, String> testResults,
      HashMap<String, String> results) {
    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));

    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put("orderNumber_SAP", results.get(BICConstants.orderNumberSAP));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults
          .put(BICConstants.fulfillmentStatus, results.get(BICECEConstants.FULFILLMENT_STATUS));
      testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICConstants.subscriptionPeriodStartDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_START_DATE));
      testResults.put(BICConstants.subscriptionPeriodEndDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_END_DATE));
      testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
      testResults
          .put(BICConstants.payment_ProfileId, results.get(BICECEConstants.PAYMENT_PROFILE_ID));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
    updateTestingHub(testResults);
  }

  private void validateTestResultsQuoteId(HashMap<String, String> testResults,
      HashMap<String, String> results) {
    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    testResults.put(BICECEConstants.BUYER_EXTERNAL_KEY,
        results.get(BICECEConstants.BUYER_EXTERNAL_KEY));

    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    // Get Finance Reports
    String result = pelicantb.postReportsFinancePelicanAPI(results);

    // Compare quoteId value from purchase order and finance reports
    JsonPath jp = new JsonPath(result);
    AssertUtils.assertEquals(jp.get("content[0].quoteId"), results.get("quoteId"));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put("orderNumber_SAP", results.get(BICConstants.orderNumberSAP));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults
          .put(BICConstants.fulfillmentStatus, results.get(BICECEConstants.FULFILLMENT_STATUS));
      testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICConstants.subscriptionPeriodStartDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_START_DATE));
      testResults.put(BICConstants.subscriptionPeriodEndDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_END_DATE));
      testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
      testResults
          .put(BICConstants.payment_ProfileId, results.get(BICECEConstants.PAYMENT_PROFILE_ID));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    updateTestingHub(testResults);
  }

  private void validateCreateOrder(HashMap<String, String> testResults) {
    long startTime, stopTime, executionTime;
    startTime = System.nanoTime();
    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }
}
