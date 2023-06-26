package com.autodesk.ece.bic.testsuites;

import com.autodesk.eceapp.testbase.ece.ECETestBase;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.dto.IProductDetails;
import com.autodesk.eceapp.dto.impl.ProductDetails;
import com.autodesk.eceapp.dto.impl.PurchaserDetails;
import com.autodesk.eceapp.fixtures.CustomerBillingDetails;
import com.autodesk.eceapp.fixtures.OxygenUser;
import com.autodesk.eceapp.testbase.EceCheckoutTestBase;
import com.autodesk.eceapp.testbase.EceDotcomTestBase;
import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import com.autodesk.testinghub.eseapp.constants.TestingHubConstants;
import io.restassured.path.json.JsonPath;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * A testsuite to hold tests related to validation of scenarios with direct order purchases
 */
public class DirectOrder extends ECETestBase {


  private static final String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);

  String productName;
  OxygenUser user = new OxygenUser();
  CustomerBillingDetails billingDetails;

  EceDotcomTestBase dotcomTestBase = new EceDotcomTestBase(getDriver(), getTestBase(), locale);
  EceCheckoutTestBase checkoutTestBase = new EceCheckoutTestBase(getDriver(), getTestBase(), locale);

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs(getDriver());
    loadYaml = ResourceFileLoader.getBicOrderYaml();
    localeConfigYaml = ResourceFileLoader.getLocaleConfigYaml();
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    testDataForEachMethod = (LinkedHashMap<String, String>) loadYaml
        .get("default");

    if (locale == null || locale.trim().isEmpty()) {
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale", locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);

    testDataForEachMethod.put("taxOptionEnabled", "undefined");
    testDataForEachMethod.put("productType", "flex");

    productName = "3ds-max";

    billingDetails = new CustomerBillingDetails(testDataForEachMethod, getBicTestBase());
  }

  @Test(groups = {"create-direct-order-test"}, description = "Validation of Create Direct O2P Order")
  public void createDirectOrderTest() throws MetadataException {
    com.autodesk.eceapp.testsuites.DirectOrder directOrderTestbase = new com.autodesk.eceapp.testsuites.DirectOrder(
        getDriver(),
        getTestBase());
    IProductDetails productDetails = new ProductDetails("3ds-max", "monthly", 4);
    IProductDetails productDetails2 = new ProductDetails("flex", "monthly", 296);
    IProductDetails productDetails3 = new ProductDetails("3ds-max", "yearly", 17);
    List<IProductDetails> productList = new LinkedList<>();
    productList.add(productDetails);
    productList.add(productDetails2);
    productList.add(productDetails3);
    PurchaserDetails purchaserDetails = new PurchaserDetails("thubstoreausxfluaysijltc@letscheck.pw", "Autodesk", "Foo",
        "Bar",
        "AutodeskAU@131 Abala Rd@Marrara@0812@397202088@Australia@NT", "EN", "5555555555");
    directOrderTestbase.createDirectOrder(productList, purchaserDetails, "en_AU", null);
  }

  @Test(groups = {"create-direct-order"}, description = "Validation of Create Direct O2P Order")
  public void createDirectOrder() throws MetadataException {

//    HashMap<String, String> foo = new HashMap<>(testDataForEachMethod);
//    foo.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID, "68736437139881");
//    subscriptionServiceV4Testbase.getSubscriptionById(foo);

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);

    //getDriver().navigate()
    //    .to("https://checkout-apollo.autodesk.com/en-AU/cart?offers=%5Bcountry:AU;currency:AUD;priceRegionCode:AH;offeringCode:3DSMAX;offeringName:3DSMax;offeringId:OD-000021;quantity:2;intendedUsageCode:COM;accessModelCode:S;termCode:A01;connectivityCode:C100;connectivityIntervalCode:C03;servicePlanIdCode:STND;billingBehaviorCode:A200;billingTypeCode:B100;billingFrequencyCode:B05%5D");
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
//    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
//        billingDetails.address);
//    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
//    getBicTestBase().submitOrder(testDataForEachMethod);
    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.putAll(getBicTestBase().placeFlexOrder(testDataForEachMethod));
    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    HashMap<String, String> testResults = new HashMap<String, String>();
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
  }

  @Test(groups = {"create-multiline-order"}, description = "Validation of Create Direct O2P Order")
  public void createMultilineDirectOrder() throws MetadataException {

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
    getBicTestBase().enterCustomerDetails(billingDetails.address);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber);
    results = pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(testDataForEachMethod));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    HashMap<String, String> testResults = new HashMap<String, String>();
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
  }

  @Test(groups = {"refund-multiline-order"}, description = "Validation of refund Direct O2P SUS Order")
  public void createRefundMultilineDirectOrder() throws MetadataException {
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
    getBicTestBase().enterCustomerDetails(billingDetails.address);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber);
    results = pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(testDataForEachMethod));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));


    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrderV4(results);

    // Adyen delays in IPN response is causing test failures. Until the issue is
    // resolved lets
    // add additional 6min sleep for the IPN message to come back.
    Util.sleep(360000);

    // Getting a PurchaseOrder details from pelican
    JsonPath jp = pelicantb.getRefundedPurchaseOrderV4WithPolling(results);

    results.put("refund_orderState", jp.get("orderState").toString());
    results.put("refund_fulfillmentStatus", jp.get("fulfillmentStatus"));

    // Verify that Order status is Refunded
    AssertUtils.assertEquals("Order status is NOT REFUNDED", results.get("refund_orderState"), "REFUNDED");

    //Get Subscription to check if Subscription is in Terminated status
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    AssertUtils.assertEquals("Subscription is NOT TERMINATED", results.get("response_status"),
        BICECEConstants.TERMINATED);

    HashMap<String, String> testResults = new HashMap<String, String>();
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
  }

  @Test(groups = {"refund-multiline-order-loc"}, description = "Validation of refund Direct O2P SUS Order with LOC")
  public void createRefundLOCMultilineDirectOrder() throws MetadataException {
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
    testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, BICECEConstants.CREDITCARD);
    getBicTestBase().enterCustomerDetails(billingDetails.address);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber);
    results = pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(testDataForEachMethod));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, BICECEConstants.LOC);
    testDataForEachMethod.put(BICECEConstants.IS_SAME_PAYER, BICECEConstants.TRUE);

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    results.put(BICECEConstants.orderNumber, orderNumber);
    results = pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(testDataForEachMethod));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrderV4(results);

    // Adyen delays in IPN response is causing test failures. Until the issue is
    // resolved lets
    // add additional 6min sleep for the IPN message to come back.
    Util.sleep(360000);

    // Getting a PurchaseOrder details from pelican
    JsonPath jp = pelicantb.getRefundedPurchaseOrderV4WithPolling(results);

    results.put("refund_orderState", jp.get("orderState").toString());
    results.put("refund_fulfillmentStatus", jp.get("fulfillmentStatus"));

    // Verify that Order status is Refunded
    AssertUtils.assertEquals("Order status is NOT REFUNDED", results.get("refund_orderState"), "REFUNDED");

    //Get Subscription to check if Subscription is in Terminated status
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    AssertUtils.assertEquals("Subscription is NOT TERMINATED", results.get("response_status"),
        BICECEConstants.TERMINATED);

    HashMap<String, String> testResults = new HashMap<String, String>();
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
  }

  @Test(groups = {"returning-user"}, description = "Validation of Create Direct O2P Order")
  public void createReturningUserDirectOrder() throws MetadataException {
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
    getBicTestBase().enterCustomerDetails(billingDetails.address);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber);
    results = pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(testDataForEachMethod));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber2 = getBicTestBase().getOrderNumber(testDataForEachMethod);

    results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber2);
    results = pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(testDataForEachMethod));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    HashMap<String, String> testResults = new HashMap<String, String>();
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
  }
}
