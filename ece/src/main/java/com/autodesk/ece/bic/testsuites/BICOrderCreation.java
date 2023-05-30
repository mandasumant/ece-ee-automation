package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.DatastoreClient;
import com.autodesk.ece.testbase.DatastoreClient.NewQuoteOrder;
import com.autodesk.ece.testbase.DatastoreClient.OrderData;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.NEWTAccessTestBase;
import com.autodesk.ece.testbase.PelicanTestBase;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import com.autodesk.testinghub.eseapp.constants.TestingHubConstants;
import com.google.common.base.Strings;
import io.restassured.path.json.JsonPath;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
    NetworkLogs.getObject().fetchLogs(getDriver());
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
      AssertUtils.fail("The locale configuration is not found for the given country/locale: " + locale);
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

    String newPaymentType = System.getProperty(BICECEConstants.NEW_PAYMENT_TYPE);

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
    testDataForEachMethod.put(BICECEConstants.MINI_CART_VALIDATE_PRICE, "true");
    HashMap<String, String> results = getBicTestBase().createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      // Getting a PurchaseOrder details from pelican
      results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

      //Compare tax in Checkout and Pelican
      getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
          results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

      // Get find Subscription ById
      results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

      NEWTAccessTestBase newtTb = new NEWTAccessTestBase();
      newtTb.getEntitlementsForUser(results.get(BICConstants.oxid));

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

      if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
        try {
          DatastoreClient dsClient = new DatastoreClient();
          NewQuoteOrder.NewQuoteOrderBuilder builder = NewQuoteOrder.builder()
              .name(BICECEConstants.BIC_TEST_NAME)
              .emailId(results.get(BICConstants.emailid))
              .orderNumber(new BigInteger(results.get(BICECEConstants.ORDER_ID)))
              .quoteId(results.get(BICECEConstants.SUBSCRIPTION_ID))
              .paymentType(testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE))
              .address(System.getProperty(BICECEConstants.ADDRESS));

          OrderData orderData = dsClient.queueOrder(builder.build());
          testResults.put("Stored order data ID", orderData.getId().toString());
          updateTestingHub(testResults);
        } catch (Exception e) {
          Util.printWarning("Failed to push order data to data store");
        }
      }

      updateTestingHub(testResults);
      portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
          results.get(BICConstants.emailid),
          PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
      if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
        portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
      }

      if (getBicTestBase().shouldValidateSAP()) {
        portaltb.validateBICOrderTaxInvoice(results);
        // Put the SAP Order number into results map
        testResults.put("SAPOrderNumber", getSAPOrderNumber(results.get(BICConstants.orderNumber)));
      }

      updateTestingHub(testResults);
    }
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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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
      results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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

      // Trigger the Pelican renewal job to renew the subscription
      triggerPelicanRenewalJob(results);

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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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

    if (getBicTestBase().shouldValidateSAP()) {
      portaltb.validateBICOrderTaxInvoice(results);
    }

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {
      "bic-guac-addseats"}, description = "Validation Add Seats in GUAC with existing user")
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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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
    HashMap<String, String> testResults = new HashMap<>();
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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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
    HashMap<String, String> testResults = new HashMap<>();
    startTime = System.nanoTime();
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    //Validating the tax amount with Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

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

    portaltb.validateBICOrderTaxInvoice(results);

    // Put the SAP Order number into results map
    testResults.put("SAPOrderNumber", getSAPOrderNumber(results.get(BICConstants.orderNumber)));

    updateTestingHub(testResults);

    stopTime = System.nanoTime();
    executionTime = ((stopTime - startTime) / 60000000000L);
    testResults.put(BICECEConstants.E2E_EXECUTION_TIME, String.valueOf(executionTime));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-flexorder-new"}, description = "Validation of Create BIC Flex Order New Cart")
  public void validateFlexOrderNewCart() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    startTime = System.nanoTime();
    getBicTestBase().navigateToFlexCartFromDotCom(testDataForEachMethod);

    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS) && !testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_KONBINI)) {
      // Getting a PurchaseOrder details from pelican
      results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

      // Compare tax in Checkout and Pelican
      getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
          results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

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
      if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
        portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
      }

      if (getBicTestBase().shouldValidateSAP()) {
        portaltb.validateBICOrderTaxInvoice(results);
      }
      updateTestingHub(testResults);
    }
  }

  @Test(groups = {"bic-flexdirect-new-refund"}, description = "Validation of Refund of BIC Flex Order New Cart")
  public void validateFlexOrderNewCartRefund() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    startTime = System.nanoTime();
    getBicTestBase()
        .navigateToFlexCartFromDotCom(testDataForEachMethod);

    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      // Getting a PurchaseOrder details from pelican
      results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

      // Compare tax in Checkout and Pelican
      getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
          results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

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

      // Validate Portal
      portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
          results.get(BICConstants.emailid),
          PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
      if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
        portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
      }

      // If the order number is null (such as from a financing order), use the order number from the pelican request
      if (results.get(BICConstants.orderNumber) == null) {
        results.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      }
      // Refund PurchaseOrder details from pelican
      pelicantb.createRefundOrderV4(results);

      //Adyen delays in IPN response is causing test failures. Until the issue is resolved lets
      // add additional 6min sleep for the IPN message to come back.
      Util.sleep(360000);

      // Hack to improve reliability, remove the order number to force the order api to use the oxygen id instead of the
      // order number since the latter is not officially supported
      results.remove(BICConstants.orderNumber);
      // Getting a PurchaseOrder details from pelican
      JsonPath jp = pelicantb.getRefundedPurchaseOrderV4WithPolling(results);
      results.put("refund_orderState", jp.get("orderState").toString());
      results.put("refund_fulfillmentStatus", jp.get("fulfillmentStatus"));

      // Verify that Order status is Refunded
      AssertUtils.assertEquals("Order status is NOT REFUNDED",
          results.get("refund_orderState"), "REFUNDED");

      if (getBicTestBase().shouldValidateSAP()) {
        // Validate Credit Note for the order
        portaltb.validateBICOrderPDF(results, BICECEConstants.CREDIT_NOTE);
      }

      updateTestingHub(testResults);
    }
  }

  @Test(groups = {"bic-flexdirect-returning"}, description = "Validation of Create BIC Flex Order Returning User")
  public void validateFlexOrderNewCartReturningUser() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    startTime = System.nanoTime();
    getBicTestBase()
        .navigateToFlexCartFromDotCom(testDataForEachMethod);

    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      // Getting a PurchaseOrder details from pelican
      results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

      // Compare tax in Checkout and Pelican
      getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
          results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

      // Get find Subscription ById
      results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    }

    updateTestingHub(testResults);

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    Util.printInfo("Placing Flex order for returning user");
    testDataForEachMethod.put("isReturningUser", "true");
    getBicTestBase().navigateToFlexCartFromDotCom(testDataForEachMethod);
    results.putAll(getBicTestBase().placeFlexOrder(testDataForEachMethod));

    updateTestingHub(testResults);

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      // Getting a PurchaseOrder details from pelican
      results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

      // Compare tax in Checkout and Pelican
      getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
          results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

      // Get find Subscription ById
      results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    }

    updateTestingHub(testResults);

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }
  }

  @Test(groups = {"flex-token-estimator"}, description = "Validation of Flex token estimator tool")
  public void validateFlexTokenEstimatorTool() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    startTime = System.nanoTime();
    getBicTestBase().estimateFlexTokenPrice(testDataForEachMethod);

    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      // Getting a PurchaseOrder details from pelican
      results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

      // Compare tax in Checkout and Pelican
      getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
          results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

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
      if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
        portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
      }

      if (getBicTestBase().shouldValidateSAP()) {
        portaltb.validateBICOrderTaxInvoice(results);
      }
      updateTestingHub(testResults);
    }
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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(testDataForEachMethod));
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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(testDataForEachMethod));
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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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
    portaltb.cancelSubscription(results.get(TestingHubConstants.emailid), PASSWORD);

    // The End Date of the subscription should be the same as the Next Billing Date
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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

    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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

    //Validating the tax amount with Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));

    // Update the subscription so that it is expired, which will allow us to renew it
    pelicantb.forwardNextBillingCycleForRenewal(results);

    // Lookup the subscription in pelican to confirm its renewal date
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

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

    // Trigger the pelican renewal job to renew the subscription
    triggerPelicanRenewalJob(results);

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

    // Validating Tax Invoice After renewal
    Util.printInfo("The Renewal Order No #" + results.get("response_renewalOrderNo"));
    results.put(BICConstants.orderNumber, results.get("response_renewalOrderNo"));
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
      testResults.put("renewalOrderNumber", results.get("response_renewalOrderNo"));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    if (getBicTestBase().shouldValidateSAP()) {
      portaltb.validateBICOrderTaxInvoice(results);
      // Put the SAP Order number into results map
      testResults.put("SAPOrderNumber", getSAPOrderNumber(results.get(BICConstants.orderNumber)));
    }

    updateTestingHub(testResults);
  }

  /**
   * This test is for Multi line item with multi quantity use case, with bundle promo
   *
   * @throws MetadataException
   */
  @Test(groups = {"bic-multiline-bicorder"}, description = "Validation of Create Multiline item BIC Order")
  public void validateMultiLineItemBicNativeOrder() throws MetadataException {
    getDriver().manage().timeouts().pageLoadTimeout(Duration.ofMinutes(3L));
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();

    if (testDataForEachMethod.get("promoCode") == null || testDataForEachMethod.isEmpty()) {
      testDataForEachMethod.put("promoCode", BICECEConstants.BUNDLE_PROMO);
    }
    HashMap<String, String> results = getBicTestBase()
        .createGUACBICMultilineItemOrderDotCom(testDataForEachMethod);
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
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put(BICConstants.orderNumberSAP, results.get(BICConstants.orderNumberSAP));
      testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
      testResults.put(BICConstants.fulfillmentStatus, results.get(BICECEConstants.FULFILLMENT_STATUS));
      testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
      testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
      testResults.put(BICConstants.subscriptionPeriodStartDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_START_DATE));
      testResults.put(BICConstants.subscriptionPeriodEndDate,
          results.get(BICECEConstants.SUBSCRIPTION_PERIOD_END_DATE));
      testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
      testResults.put(BICConstants.payment_ProfileId, results.get(BICECEConstants.PAYMENT_PROFILE_ID));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    updateTestingHub(testResults);
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid), PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));

    if (getBicTestBase().shouldValidateSAP()) {
      portaltb.validateBICOrderTaxInvoice(results);
      updateTestingHub(testResults);
    }
  }

  @Test(groups = {"bic-multiproduct-minicart"}, description = "Validation of Adding Multi Product to mini cart")
  public void validateAddingMultiProductMiniCart() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();

    HashMap<String, String> results = getBicTestBase()
            .createMultiProductOrderMiniCart(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);
  }

  private void triggerPelicanRenewalJob(HashMap<String, String> results) {
    PelicanTestBase pelicanTB = new PelicanTestBase();
    pelicanTB.renewSubscription(results);
    // Wait for the Pelican job to complete
    Util.sleep(600000);
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
    getBicTestBase().setStorageData();
    Util.sleep(2000);
    driver.get("chrome://settings/clearBrowserData");
    Util.sleep(5000);
    WebElement clearBtn = (WebElement) js.executeScript(
        "return document.querySelector(\"body > settings-ui\").shadowRoot.querySelector(\"#main\").shadowRoot.querySelector(\"settings-basic-page\").shadowRoot.querySelector(\"#basicPage > settings-section:nth-child(9) > settings-privacy-page\").shadowRoot.querySelector(\"settings-clear-browsing-data-dialog\").shadowRoot.querySelector(\"#clearBrowsingDataConfirm\")");
    clearBtn.click();
    Util.sleep(5000);
  }

  private String getSAPOrderNumber(String orderNumber) {
    //Tibco call to SAP, waits for Create Order call to be successful
    String orderNumberSAP = "null";
    try {
      boolean sapStatusSuccess = tibcotb.waitTillProcessCompletesStatus(orderNumber,
          TestingHubConstants.tibco_createorder);
      if (sapStatusSuccess) {
        //Returns SAP Order number
        orderNumberSAP = getSAPOrderNumberUsingPO(orderNumber);
        Util.printInfo("SAP order Number: " + orderNumberSAP);
      }
    } catch (Exception e) {
      Util.printInfo("Error while retrieving SAP order.");
      e.printStackTrace();
    }
    return orderNumberSAP;
  }
}
