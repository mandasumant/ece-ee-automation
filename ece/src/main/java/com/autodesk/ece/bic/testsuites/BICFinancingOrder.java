package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.utilities.NetworkLogs;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import io.restassured.path.json.JsonPath;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BICFinancingOrder extends ECETestBase {

  private static final String EMAIL = System.getProperty("email");
  private static final String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  long startTime;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = null;
  long stopTime, executionTime;
  private String PASSWORD;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs(getDriver());
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String localeConfigFile = "LOCALE_CONFIG";
    localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testCaseData = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());
    defaultValues.putAll(testCaseData);
    testDataForEachMethod = defaultValues;
    locale = System.getProperty(BICECEConstants.LOCALE);
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
    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));

  }

  @Test(groups = {
      "bic-financing-declined"}, description = "Validation of LiftForward Declined Order")
  public void validateBicNativeFinancingDeclinedOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);

    results.putAll(testDataForEachMethod);
    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    updateTestingHub(testResults);

    Util.sleep(120000);

    // Getting a PurchaseOrder details from pelican
    String purchaseOrderAPIResponse = pelicantb.getPurchaseOrder(results);
    JsonPath jp = new JsonPath(purchaseOrderAPIResponse);

    AssertUtils.assertTrue(jp.get("content[0].id") != null);
    AssertUtils.assertTrue(
        jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionId") == null);
    AssertUtils.assertEquals(jp.get("content[0].orderState"), "DECLINED");
    AssertUtils.assertEquals(jp.get("content[0].payments[0].paymentProcessor"), "LIFTFORWARD");

    updateTestingHub(testResults);

  }

  @Test(groups = {
      "bic-financing-canceled"}, description = "Validation of  LiftForward BIC Cancel Order")
  public void validateBicNativeFinancingCanceledOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);

    results.putAll(testDataForEachMethod);
    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    updateTestingHub(testResults);

    Util.sleep(120000);

    // Getting a PurchaseOrder details from pelican
    String purchaseOrderAPIResponse = pelicantb.getPurchaseOrder(results);
    JsonPath jp = new JsonPath(purchaseOrderAPIResponse);

    AssertUtils.assertTrue(jp.get("content[0].id") != null);
    AssertUtils.assertTrue(
        jp.get("content[0].lineItems[0].subscriptionInfo.subscriptionId") == null);
    AssertUtils.assertEquals(jp.get("content[0].orderState"), "DECLINED");
    AssertUtils.assertEquals(jp.get("content[0].payments[0].paymentProcessor"), "LIFTFORWARD");

    updateTestingHub(testResults);

  }

  @Test(groups = {"bic-financing-renew-order"}, description = "Validation of BIC Financing Renewal Order")
  public void validateBicNativeFinancingRenewalOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase().createGUACBICOrderDotCom(
        testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    Util.sleep(120000);

    // Getting a PurchaseOrder details
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));

    // Update the subscription NBD so that the current data become 2 days before the next billing date.
    pelicantb.forwardNextBillingCycleForFinancingRenewal(results);

    // Lookup the subscription in pelican to confirm its renewal date
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    try {
      String originalBillingDateString = results.get(BICECEConstants.NEXT_BILLING_DATE);
      Util.printInfo("Original Billing Date: " + originalBillingDateString);
      Date originalBillingDate = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
          originalBillingDateString);
      Date date = new Date();
      Assert.assertTrue(originalBillingDate.before(new Date(date.getTime() + (1000 * 60 * 60 * 72))),
          "Check that the subscription is ready to be renewed");
    } catch (ParseException e) {
      e.printStackTrace();
    }

    portaltb.renewFinancingSubscription(results.get(BICConstants.emailid), PASSWORD);

    results = getBicTestBase().renewFinancingOrder(
        testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    Util.sleep(120000);

    // Getting a PurchaseOrder details
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get the subscription in pelican to check if it has renewed
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    try {
      // Ensure that the subscription renews in the future
      String nextBillingDateString = results.get(BICECEConstants.NEXT_BILLING_DATE);
      Util.printInfo("New Billing Date: " + nextBillingDateString);
      Date newBillingDate = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
          nextBillingDateString);
      Assert.assertTrue(newBillingDate.after(new Date()),
          "Check that the subscription has been renewed");
    } catch (ParseException e) {
      e.printStackTrace();
    }

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put("renewalOrderNumber", results.get("response_renewalOrderNo"));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICConstants.subscriptionPeriodStartDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_START_DATE));
      testResults.put(BICConstants.subscriptionPeriodEndDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_END_DATE));
      testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));

    updateTestingHub(testResults);
  }

  @Test(groups = {
      "bic-addseats-financing"}, description = "Validation of BIC add seats with financing with changing payment method")
  public void validateBICAddSeatsFinancing() throws MetadataException {
    HashMap<String, String> results = null;
    HashMap<String, String> testResults = new HashMap<String, String>();
    String paymentType = System.getProperty("payment");

    results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);

    String emailID = results.get(BICConstants.emailid);

    updateTestingHub(results);
    results.putAll(testDataForEachMethod);

    Util.sleep(120000);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    results.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(results);

    Util.printInfo("Current Payment Type is : " + paymentType);
    String[] paymentTypes = localeDataMap.get(locale).get(BICECEConstants.PAYMENT_METHODS)
        .split(",");
    ArrayList<String> payments = new ArrayList<>(Arrays.asList(paymentTypes));
    payments.remove(paymentType);
    int index = (int) Util.randomNumber(payments.size());
    paymentType = payments.get(index);
    Util.printInfo("New Payment Type is : " + paymentType);
    testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, paymentType);

    portaltb.openPortalBICLaunch(testDataForEachMethod.get("cepURL"));

    if (!(Strings.isNullOrEmpty(EMAIL))) {
      portaltb.portalLogin(emailID, PASSWORD);
    }
    String[] paymentCardDetails = getBicTestBase().getPaymentDetails(paymentType.toUpperCase())
        .split("@");
    portaltb.changePaymentMethodAndValidate(testDataForEachMethod, paymentCardDetails);

    // Place add Seat order in Portal
    results.putAll(
        portaltb.createAndValidateAddSeatOrderInPortal(testDataForEachMethod.get(
                BICECEConstants.ADD_SEAT_QTY),
            testDataForEachMethod));
    testResults.put("addSeatOrderNumber", results.get("addSeatOrderNumber"));

    testResults.put(BICECEConstants.ADD_SEAT_QTY, results.get(BICECEConstants.ADD_SEAT_QTY));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults
          .put(BICConstants.fulfillmentStatus, results.get(BICECEConstants.FULFILLMENT_STATUS));
      testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put("subscriptionPeriodStartDate",
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_START_DATE));
      testResults
          .put(BICConstants.subscriptionPeriodEndDate, results.get(
              BICECEConstants.SUBSCRIPTION_PERIOD_END_DATE));
      testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
      testResults
          .put(BICConstants.payment_ProfileId, results.get(BICECEConstants.PAYMENT_PROFILE_ID));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
    updateTestingHub(testResults);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);

  }
}
