package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.PayportTestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BICOrderCreation extends ECETestBase {

  private static final String EMAIL = System.getProperty("email");
  private static final String defaultLocale = "en_US";
  private static final String defaultTaxOption = "undefined";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  long startTime, stopTime, executionTime;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  private String PASSWORD;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String localeConfigFile = "LOCALE_CONFIG";
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

    if (localeDataMap == null || localeDataMap.get(locale) == null) {
      AssertUtils.fail("The locale configuration is not found for  the given country/locale : " + locale);
    } else {
      testDataForEachMethod.putAll(localeDataMap.get(locale));
    }

    Util.printInfo(
        "Validating the store for the locale :" + locale + " Store : " + System.getProperty(
            BICECEConstants.STORE));

    boolean isValidStore = testDataForEachMethod.get(BICECEConstants.STORE_NAME)
        .equals(System.getProperty(BICECEConstants.STORE));

    if (!isValidStore) {
      AssertUtils.fail(
          "The store is not supported for the given country/locale : " + locale
              + ". Supported stores are "
              + testDataForEachMethod.get(BICECEConstants.STORE_NAME));
    }

    if (testDataForEachMethod.get(BICECEConstants.ADDRESS) == null || testDataForEachMethod.get(BICECEConstants.ADDRESS)
        .isEmpty()) {
      AssertUtils.fail("Address not found in the config for the locale : " + locale);
    }

    if (taxOptionEnabled == null || taxOptionEnabled.trim().isEmpty()) {
      taxOptionEnabled = defaultTaxOption;
    }
    testDataForEachMethod.put("taxOptionEnabled", taxOptionEnabled);

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);
    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));
  }

  @Test(groups = {
      "bic-changePayment"}, description = "Validation of BIC change payment details functionality")
  public void validateBICChangePaymentProfile() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);

    String emailID = results.get(BICConstants.emailid);

    updateTestingHub(results);
    results.putAll(testDataForEachMethod);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE)
        .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    results.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(results);

    String paymentType = System.getProperty("payment");
    Util.printInfo("Current Payment Type is : " + paymentType);

    String newPaymentType = System.getProperty("newPaymentType");

    if (newPaymentType == null || newPaymentType.isEmpty()) {
      String[] paymentTypes = localeDataMap.get(locale).get(BICECEConstants.PAYMENT_METHODS)
          .split(",");
      ArrayList<String> payments = new ArrayList<>(Arrays.asList(paymentTypes));
      payments.remove(paymentType);
      int index = (int) Util.randomNumber(payments.size());
      newPaymentType = payments.get(index);

      if (newPaymentType.equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
        newPaymentType = BICECEConstants.MASTERCARD;
      }
    }
    Util.printInfo("New Payment Type is : " + newPaymentType);

    testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, newPaymentType);

    portaltb.openPortalBICLaunch(testDataForEachMethod.get("cepURL"));

    if (!(Strings.isNullOrEmpty(EMAIL))) {
      portaltb.portalLogin(emailID, PASSWORD);
    }
    String[] paymentCardDetails = getBicTestBase().getPaymentDetails(newPaymentType.toUpperCase())
        .split("@");
    portaltb.changePaymentMethodAndValidate(testDataForEachMethod, paymentCardDetails);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
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
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-nativeorder"}, description = "Validation of Create BIC Hybrid Order")
  public void validateBicNativeOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICConstants.orderNumberSAP, results.get(BICConstants.orderNumberSAP));
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
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

  }

  @Test(groups = {
      "bic-nativeorder-switch-term"}, description = "Validation of Create BIC Hybrid Order")
  public void validateBicNativeOrderSwitchTerm() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICConstants.orderNumberSAP, results.get(BICConstants.orderNumberSAP));
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

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    portaltb.switchTermInUserPortal(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD);
    updateTestingHub(testResults);
    Util.sleep(120000);

    if (!System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      // Get find Subscription ById
      results.putAll(pelicantb.getSubscriptionById(results));

      Assert.assertNotNull(results.get("response_currentBillingPriceId"),
          "Current Billing PriceId  should not be null");
      Assert.assertNotNull(results.get("response_nextBillingPriceId"),
          "Next Billing PriceId  should not be null");
      Assert.assertNotNull(results.get("response_switchTermPriceId"),
          "Switch Term Billing PriceId  should not be null");
      Assert.assertNotEquals("Current and Next billing PriceIds  must be different",
          results.get("response_currentBillingPriceId"), results.get("response_nextBillingPriceId"));

      try {
        testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));

        testResults.put(BICConstants.subscriptionPeriodStartDate,
            results.get(BICECEConstants.SUBSCRIPTION_PERIOD_START_DATE));
        testResults.put(BICConstants.subscriptionPeriodEndDate,
            results.get(BICECEConstants.SUBSCRIPTION_PERIOD_END_DATE));
        testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
      } catch (Exception e) {
        Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
      }
      updateTestingHub(testResults);

      // Update the subscription so that it is expired, which will allow us to renew it
      pelicantb.forwardNextBillingCycleForRenewal(results);

      // Lookup the subscription in pelican to confirm its renewal date
      results.putAll(pelicantb.getSubscriptionById(results));

      // Verify that the subscription has actually moved to the past and is in a state to be renewed
      try {
        String originalBillingDateString = results.get(BICECEConstants.NEXT_BILLING_DATE);
        Util.printInfo("Original Billing Date: " + originalBillingDateString);
        Date originalBillingDate = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
            originalBillingDateString);
        Assert.assertTrue(originalBillingDate.before(new Date()),
            "Check that the subscription is ready to be renewed");
      } catch (ParseException e) {
        e.printStackTrace();
      }

      // Trigger the payport renewal job to renew the subscription
      triggerPayportRenewalJob(results);

      // Get the subscription in pelican to check if it has renewed
      results.putAll(pelicantb.getSubscriptionById(results));

      try {
        // Ensure that the subscription renews in the future
        String nextBillingDateString = results.get(BICECEConstants.NEXT_BILLING_DATE);
        Util.printInfo("New Billing Date: " + nextBillingDateString);
        Date newBillingDate = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
            nextBillingDateString);
        Assert.assertTrue(newBillingDate.after(new Date()),
            "Check that the subscription has been renewed");

        AssertUtils
            .assertEquals("The billing date has been updated to next cycle ",
                results.get(BICECEConstants.NEXT_BILLING_DATE).split("\\s")[0],
                Util.customDate("MM/dd/yyyy", 0, -5, +1));
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
  }

  @Test(groups = {"bic-addseat-native"}, description = "Validation of BIC Add Seat Order")
  public void validateBicAddSeatNativeOrder() throws MetadataException {
    testDataForEachMethod
        .put(BICECEConstants.PRODUCT_ID, testDataForEachMethod.get("nativeproductID"));
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Initial order validation in Portal
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    // Place add Seat order in Portal
    results.putAll(
        portaltb.createAndValidateAddSeatOrderInPortal(testDataForEachMethod.get(
            BICECEConstants.ADD_SEAT_QTY),
            testDataForEachMethod));
    testResults.put("addSeatOrderNumber", results.get("addSeatOrderNumber"));
    // testResults.put("addSeatPerSeatGrossAmount",
    // results.get("perSeatGrossAmount"));
    testResults.put(BICECEConstants.ADD_SEAT_QTY, results.get(BICECEConstants.ADD_SEAT_QTY));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults
          .put(BICConstants.fulfillmentStatus, results.get(BICECEConstants.FULFILLMENT_STATUS));
      testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICECEConstants.subscriptionPeriodStartDate,
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

  @Test(groups = {
      "bic-guac-addseats"}, description = "Validation Add Seats in GAUC with existing user")
  public void validateBicAddSeats() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();

    Util.printInfo("Placing initial order");

    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);

    results.put(BICConstants.nativeOrderNumber + "1", results.get(BICConstants.orderNumber));
    results.remove(BICConstants.orderNumber);
    testDataForEachMethod.putAll(results);

    resetDriver();

    testDataForEachMethod.put("bicNativePriceID", testDataForEachMethod.get(
        BICECEConstants.PRODUCT_ID));
    Util.printInfo("Placing second order for the returning user");

    results = getBicTestBase().createBic_ReturningUserAddSeat(testDataForEachMethod);
    results.put(BICConstants.nativeOrderNumber + "2", results.get(BICConstants.orderNumber));
    results.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    // Verify that a seat was added
    AssertUtils.assertEquals("Subscription should have 2 seats",
        results.get("response_subscriptionQuantity"), "2");

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults
          .put(BICConstants.fulfillmentStatus, results.get(BICECEConstants.FULFILLMENT_STATUS));
      testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put("subscriptionQuantity", results.get("response_subscriptionQuantity"));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-reduceseats-native"}, description = "Validation of BIC Reduce Seats")
  public void validateBicReduceSeats() throws MetadataException {
    testDataForEachMethod
        .put(BICECEConstants.PRODUCT_ID, testDataForEachMethod.get("nativeproductID"));
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    testDataForEachMethod.put(BICECEConstants.REDUCE_SEATS, BICECEConstants.TRUE);
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Initial order validation in Portal
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    // Reduce seats in Portal
    results.putAll(portaltb.reduceSeatsInPortalAndValidate());
    testResults.put("reducedSeatQty", results.get("reducedSeatQty"));
    updateTestingHub(testResults);

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    // Verify that a seat was reduced
    AssertUtils.assertEquals("Subscription was reduced by 1 seat",
        results.get("response_quantityToReduce"), "1");

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
      testResults.put("quantityToReduce", results.get("response_quantityToReduce"));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
    updateTestingHub(testResults);
    Util.sleep(60000);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-flexorder"}, description = "Validation of Create BIC Flex Order")
  public void validateBicFlexOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
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

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"trialDownload-UI"}, description = "Testing Download Trial version")
  public void validateTrialDownloadUI() {
    HashMap<String, String> testResults = new HashMap<String, String>();

    try {
      testResults.put(BICConstants.emailid, EMAIL);
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

  @Test(groups = {"bic-metaorder"}, description = "Validation of Create BIC Meta Order")
  public void validateBicMetaOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();

    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    updateTestingHub(results);
    results.putAll(testDataForEachMethod);

    if (System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    // Getting a PurchaseOrder details from pelican
    String pelicanResponse = pelicantb.retryGetPurchaseOrder(results, true);

    if (!pelicanResponse.contains("subscriptionId")) {
      Util.printWarning("Failed to get subscription for Meta order, skipping rest of test");
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put("Subscription Created", "False");
      updateTestingHub(testResults);
      return;
    }
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicanResponse));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    //Validate if this is Meta order
    if (!results.get(BICECEConstants.RESPONSE_OFFERING_TYPE)
        .equals(BICECEConstants.META_SUBSCRIPTION)) {
      AssertUtils.fail("The product is not a meta product . Offering type is  : " + results.get(
          BICECEConstants.RESPONSE_OFFERING_TYPE));
    }

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put("Subscription Created", "True");
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults.put(BICECEConstants.RESPONSE_OFFERING_TYPE,
          results.get(BICECEConstants.OFFERING_TYPE));
      testResults.put(BICConstants.fulfillmentStatus,
          results.get(BICECEConstants.FULFILLMENT_STATUS));
      testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICConstants.subscriptionPeriodStartDate,
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

    // Wait for subscription to show up in portal .
    Util.sleep(600000);

    // Portal
    portaltb.validateMetaOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    // Validate Create Order
    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-returningUser"}, description = "Validation of Create BIC Hybrid Order")
  public void validateBicReturningUser() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();

    testDataForEachMethod.put(
        BICECEConstants.PRODUCT_ID, testDataForEachMethod.get(BICECEConstants.PRODUCT_ID));
    Util.printInfo("Placing initial order.");

    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    results.put(BICConstants.nativeOrderNumber + "1", results.get(BICConstants.orderNumber));
    results.remove(BICConstants.orderNumber);
    updateTestingHub(results);
    testDataForEachMethod.putAll(results);

    resetDriver();

    testDataForEachMethod.put("bicNativePriceID", testDataForEachMethod.get(
        BICECEConstants.PRODUCT_ID));
    Util.printInfo("Placing second order for the returning user.");

    results = getBicTestBase().createBICReturningUser(testDataForEachMethod);
    results.put(BICConstants.nativeOrderNumber + "2", results.get(BICConstants.orderNumber));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    updateTestingHub(results);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
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
    Util.sleep(60000);

    // Initial order validation in Portal
    getPortalTestBase().validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid), PASSWORD,
        results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  /**
   * Test the align billing functionality in portal. Place two orders for the same product without adding seats to
   * create two separate subscriptions. Since the subscriptions are placed sequentially, they will expire on the same
   * day and you wouldn't be able to align them. After the orders are placed, we use pelican's patch api to advance the
   * renewal date of the second subscription so that the 2 subscriptions are unaligned. Next, we automate portal's UI to
   * realign the billing dates and assert that the dates match.
   */
  @Test(groups = {"bic-align-billing"}, description = "Validation of align billing")
  public void validateAlignBilling() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();

    Util.printInfo("Placing initial order");

    // Place the first order
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);

    results.put(BICConstants.nativeOrderNumber + "1", results.get(BICConstants.orderNumber));
    testDataForEachMethod.putAll(results);
    getBicTestBase().driver.manage().deleteAllCookies();

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    // Get the subscription id for the first order
    results.putAll(pelicantb
        .getPurchaseOrderDetails(pelicantb.getPurchaseOrder(testDataForEachMethod)));
    results.put(BICECEConstants.SUB1_ID, results.get(BICECEConstants.SUBSCRIPTION_ID));

    // Get the original billing date for the first subscription
    testDataForEachMethod
        .put(BICECEConstants.SUBSCRIPTION_ID, results.get(BICECEConstants.SUBSCRIPTION_ID));
    results.putAll(pelicantb.getSubscriptionById(testDataForEachMethod));
    results.put(
        BICECEConstants.SUB1_NEXT_BILLING_DATE, results.get(BICECEConstants.NEXT_BILLING_DATE));

    Util.printInfo("Placing second order for the returning user");

    // Placing the second order for the second subscription
    results.putAll(getBicTestBase().createBICReturningUserLoggedIn(testDataForEachMethod));
    results.put(BICConstants.nativeOrderNumber + "2", results.get(BICConstants.orderNumber));

    // Get the subscription id for the second subscription
    testDataForEachMethod.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    results.putAll(pelicantb
        .getPurchaseOrderDetails(pelicantb.getPurchaseOrder(testDataForEachMethod)));
    results.put(BICECEConstants.SUB2_ID, results.get(BICECEConstants.SUBSCRIPTION_ID));

    // Forcefully update the second subscription's billing date to make it unaligned from the first subscription
    testDataForEachMethod
        .put("desiredBillingDate", Util.customDate("MM/dd/yyyy", 0, 15, 0) + " 20:13:28 UTC");
    pelicantb.forwardNextBillingCycleForRenewal(testDataForEachMethod);

    // Open up portal UI and align billing between the 2 subscriptions
    portaltb.alignBillingInPortal(testDataForEachMethod.get(TestingHubConstants.cepURL),
        results.get(TestingHubConstants.emailid), PASSWORD, results.get(BICECEConstants.SUB1_ID),
        results.get(BICECEConstants.SUB2_ID));

    Util.sleep(240000);

    // Get the billing date of the aligned subscription
    results.putAll(pelicantb.getSubscriptionById(testDataForEachMethod));
    results.put(
        BICECEConstants.SUB2_NEXT_BILLING_DATE, results.get(BICECEConstants.NEXT_BILLING_DATE));

    AssertUtils.assertEquals("Billing Dates should be aligned",
        results.get(BICECEConstants.SUB1_NEXT_BILLING_DATE).split("\\s")[0],
        results.get(BICECEConstants.SUB2_NEXT_BILLING_DATE).split("\\s")[0]);

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICECEConstants.SUB1_ID, results.get(BICECEConstants.SUB1_ID));
      testResults.put(BICECEConstants.SUB2_ID, results.get(BICECEConstants.SUB2_ID));
      testResults.put(
          BICECEConstants.SUB1_NEXT_BILLING_DATE,
          results.get(BICECEConstants.SUB1_NEXT_BILLING_DATE));
      testResults.put(
          BICECEConstants.SUB2_NEXT_BILLING_DATE,
          results.get(BICECEConstants.SUB2_NEXT_BILLING_DATE));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
    updateTestingHub(testResults);
  }

  @Test(groups = {
      "bic-restart-subscription"}, description = "Cancel and restart subscription in Portal")
  public void validateRestartSubscription() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase().createGUACBICOrderDotCom(
        testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    // The End date of the subscription should be null and status Active
    results
        .put(BICECEConstants.SUBSCRIPTION_END_DATE, results.get(BICECEConstants.RESPONSE_END_DATE));
    results.put(BICECEConstants.STATUS, results.get(BICECEConstants.RESPONSE_STATUS));

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Assert.assertNull(results.get(BICECEConstants.RESPONSE_END_DATE), "End date is null.");
    }

    AssertUtils
        .assertEquals("Status is Active.", results.get(BICECEConstants.RESPONSE_STATUS),
            "ACTIVE");

    // Cancel Subscription in Portal
    portaltb.cancelSubscription(
        results.get(TestingHubConstants.emailid), PASSWORD);

    // The End Date of the subscription should be the same as the Next Billing Date
    results.putAll(pelicantb.getSubscriptionById(results));

    results
        .put(BICECEConstants.SUBSCRIPTION_END_DATE, results.get(BICECEConstants.RESPONSE_END_DATE));
    results.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
    results.put("autoRenewEnabled", results.get(BICECEConstants.RESPONSE_AUTORENEW_ENABLED));
    results.put("expirationDate", results.get("response_expirationDate"));
    results.put(BICECEConstants.STATUS, results.get(BICECEConstants.RESPONSE_STATUS));
    AssertUtils
        .assertEquals("End date should equal Next Billing Date.", results.get(
            BICECEConstants.RESPONSE_END_DATE),
            results.get(BICECEConstants.NEXT_BILLING_DATE));
    Assert.assertEquals(results.get(BICECEConstants.RESPONSE_AUTORENEW_ENABLED), "false",
        "Auto renew is off.");
    AssertUtils
        .assertEquals("Expiration date equals next billing date.",
            results.get("response_expirationDate"), results.get(BICECEConstants.NEXT_BILLING_DATE));
    AssertUtils
        .assertEquals("Status is Cancelled.", results.get(BICECEConstants.RESPONSE_STATUS),
            "CANCELLED");

    String originalNextBillingDate = results.get(BICECEConstants.NEXT_BILLING_DATE);

    // Restart Subscription in Portal
    portaltb.restartSubscription();

    // End date should be null, auto renew On, status Active and NBD the same

    results.putAll(pelicantb.getSubscriptionById(results));

    results
        .put(BICECEConstants.SUBSCRIPTION_END_DATE, results.get(BICECEConstants.RESPONSE_END_DATE));
    results.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
    results.put("autoRenewEnabled", results.get(BICECEConstants.RESPONSE_AUTORENEW_ENABLED));
    results.put(BICECEConstants.STATUS, results.get(BICECEConstants.RESPONSE_STATUS));

    testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
    updateTestingHub(testResults);

    Assert.assertNull(results.get(BICECEConstants.RESPONSE_END_DATE), "End date is null.");
    AssertUtils.assertEquals("Auto renew is on.", results.get(
        BICECEConstants.RESPONSE_AUTORENEW_ENABLED), "true");
    AssertUtils
        .assertEquals("Next billing date should be the same as before subscription was cancelled.",
            results.get(BICECEConstants.NEXT_BILLING_DATE), originalNextBillingDate);
    AssertUtils
        .assertEquals("Status is Active.", results.get(BICECEConstants.RESPONSE_STATUS),
            "ACTIVE");
  }

  /**
   * Validate the renewal functionality. Steps: 1. Place an order for a subscription product 2. Get the subscription for
   * the placed order 3. Manually update the subscription so that it is expired 4. Trigger the renewal job 5. Validate
   * that the subscription next renews in the future
   */
  @Test(groups = {"renew-bic-order"}, description = "Validation of BIC Renewal Order")
  public void validateRenewBicOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase().createGUACBICOrderDotCom(
        testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    // Update the subscription so that it is expired, which will allow us to renew it
    pelicantb.forwardNextBillingCycleForRenewal(results);

    // Lookup the subscription in pelican to confirm its renewal date
    results.putAll(pelicantb.getSubscriptionById(results));

    // Verify that the subscription has actually moved to the past and is in a state to be renewed
    try {
      String originalBillingDateString = results.get(BICECEConstants.NEXT_BILLING_DATE);
      Util.printInfo("Original Billing Date: " + originalBillingDateString);
      Date originalBillingDate = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
          originalBillingDateString);
      Assert.assertTrue(originalBillingDate.before(new Date()),
          "Check that the subscription is ready to be renewed");
    } catch (ParseException e) {
      e.printStackTrace();
    }

    // Trigger the payport renewal job to renew the subscription
    triggerPayportRenewalJob(results);

    // Get the subscription in pelican to check if it has renewed
    results.putAll(pelicantb.getSubscriptionById(results));

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
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
    updateTestingHub(testResults);

  }

  private void triggerPayportRenewalJob(
      HashMap<String, String> results) {
    PayportTestBase payportTB = new PayportTestBase(results);
    payportTB.renewPurchase(results);
    // Wait for the payport job to complete
    Util.sleep(300000);
  }

  /**
   * Delete all cookies, localStorage, and sessionStorage in the current driver
   */
  private void resetDriver() {
    Util.printInfo("Resetting browser data");
    WebDriver driver = getBicTestBase().driver;
    driver.manage().deleteAllCookies();
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("localStorage.clear();sessionStorage.clear();");
    Util.sleep(2000);
    driver.get("chrome://settings/clearBrowserData");
    driver.findElement(By.xpath("//settings-ui")).sendKeys(Keys.ENTER);
    WebDriverWait wait = new WebDriverWait(driver, 5);
    Util.sleep(2000);
  }
}
