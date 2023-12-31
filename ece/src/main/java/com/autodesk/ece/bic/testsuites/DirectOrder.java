package com.autodesk.ece.bic.testsuites;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.dto.IProductDetails;
import com.autodesk.eceapp.dto.impl.ProductDetails;
import com.autodesk.eceapp.dto.impl.PurchaserDetails;
import com.autodesk.eceapp.dto.purchaseOrder.v4.LineItemDTO;
import com.autodesk.eceapp.fixtures.CustomerBillingDetails;
import com.autodesk.eceapp.fixtures.OxygenUser;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EceCheckoutTestBase;
import com.autodesk.eceapp.testbase.EceDotcomTestBase;
import com.autodesk.eceapp.testbase.ece.ECETestBase;
import com.autodesk.eceapp.testbase.ece.MOETestBase;
import com.autodesk.eceapp.testbase.ece.PWSTestBase;
import com.autodesk.eceapp.testbase.ece.QuoteOrderTestBase;
import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import io.restassured.path.json.JsonPath;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * A testsuite to hold tests related to validation of scenarios with direct order purchases
 */
public class DirectOrder extends ECETestBase {

  private static final String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = new LinkedHashMap<>();
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);

  String productName;
  OxygenUser user = new OxygenUser();
  CustomerBillingDetails billingDetails;

  EceDotcomTestBase dotcomTestBase = new EceDotcomTestBase(getDriver(), getTestBase(), locale);
  EceCheckoutTestBase checkoutTestBase = new EceCheckoutTestBase(getDriver(), getTestBase(), locale);

  MOETestBase moeTestBase = new MOETestBase(this.getTestBase(), testDataForEachMethod);
  QuoteOrderTestBase quoteOrderTestBase;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs(getDriver());
    loadYaml = ResourceFileLoader.getBicOrderYaml();
    localeConfigYaml = ResourceFileLoader.getLocaleConfigYaml();
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    testDataForEachMethod = (LinkedHashMap<String, String>) loadYaml.get("default");
    if (locale == null || locale.trim().isEmpty()) {
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale", locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml.get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);

    testDataForEachMethod.put("taxOptionEnabled", "undefined");
    testDataForEachMethod.put("productType", "flex");
    productName = "3ds-max";
    Optional.ofNullable(StringUtils.trimToNull(System.getProperty("currency")))
        .ifPresent(currency -> testDataForEachMethod.put("currencyStore", currency));
    Optional.ofNullable(StringUtils.trimToNull(System.getProperty("agentCSN")))
        .ifPresent(agentCSN -> testDataForEachMethod.put("quoteAgentCsnAccount", agentCSN));
    Optional.ofNullable(StringUtils.trimToNull(System.getProperty("agentEmail")))
        .ifPresent(agentEmail -> testDataForEachMethod.put("agentContactEmail", agentEmail));
    Optional.ofNullable(StringUtils.trimToNull(System.getProperty("subscriptionStatus")))
        .ifPresent(
            subscriptionStatus -> testDataForEachMethod.put(BICECEConstants.SUBSCRIPTION_STATUS, subscriptionStatus));
    testDataForEachMethod.put(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE,
        PWSTestBase.getQuoteStartDateAsString());
    quoteOrderTestBase = new QuoteOrderTestBase(
        testDataForEachMethod.get("pwsClientId"),
        testDataForEachMethod.get("pwsClientSecret"),
        testDataForEachMethod.get("pwsClientId_v2"),
        testDataForEachMethod.get("pwsClientSecret_v2"),
        testDataForEachMethod.get("pwsHostname"));
    billingDetails = new CustomerBillingDetails(testDataForEachMethod, getBicTestBase());

    if (System.getProperty("subscriptionStatus") != null) {
      testDataForEachMethod.put(BICECEConstants.SUBSCRIPTION_STATUS, System.getProperty("subscriptionStatus"));
    }
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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    populateTestResultsForTestingHub(results);
  }

  @Test(groups = {"create-multiline-order"}, description = "Validation of Create Direct O2P Order")
  public void createMultilineDirectOrder() throws MetadataException {

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectYearlySubscription();
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
    testDataForEachMethod.put(BICECEConstants.orderNumber, orderNumber);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod)));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    populateTestResultsForTestingHub(results);
  }

  @Test(groups = {"refund-multiline-order"}, description = "Validation of refund Direct O2P SUS Order")
  public void createRefundMultilineDirectOrder() throws MetadataException {
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectYearlySubscription();
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
    testDataForEachMethod.put(BICECEConstants.orderNumber, orderNumber);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod)));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrderV4(results, new ArrayList<>());

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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    AssertUtils.assertEquals("Subscription is NOT TERMINATED", results.get("response_status"),
        BICECEConstants.TERMINATED);

    HashMap<String, String> testResults = new HashMap<>();
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
    EceBICTestBase.Names names = EceBICTestBase.generateFirstAndLastNames();
    testDataForEachMethod.put(BICECEConstants.FIRSTNAME, names.firstName);
    testDataForEachMethod.put(BICECEConstants.LASTNAME, names.lastName);
    testDataForEachMethod.put(BICECEConstants.emailid, EceBICTestBase.generateUniqueEmailID());

    String quoteLineItems = "access_model:flex,offering_id:OD-000163,term:annual,usage:commercial,plan:standard,quantity:13000";
    testDataForEachMethod.put(BICECEConstants.FLEX_TOKENS, "13000");
    System.setProperty("quoteLineItems", quoteLineItems);
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    HashMap<String, String> quoteResults = quoteOrderTestBase.createQuoteOrder(
        testDataForEachMethod, portaltb, getBicTestBase(), pelicantb, subscriptionServiceV4Testbase,
        ECETestBase::updateTestingHub);
    testDataForEachMethod.putAll(quoteResults);

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectYearlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();

    testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, BICECEConstants.CREDITCARD);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().populateBillingAddress(billingDetails.address, testDataForEachMethod);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber);
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod)));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, BICECEConstants.LOC);
    testDataForEachMethod.put(BICECEConstants.IS_SAME_PAYER, BICECEConstants.TRUE);

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectYearlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    getBicTestBase().clickCartContinueButton();
    getBicTestBase().checkIfCustomerPaymentDetailsComplete();
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    results.put(BICECEConstants.orderNumber, orderNumber);
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod)));

    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrderV4(results, new ArrayList<>());

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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
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
    testDataForEachMethod.put(BICECEConstants.orderNumber, orderNumber);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod)));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    getBicTestBase().driver.manage().deleteAllCookies();
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().loginFromOxygenFrame(user.emailID, user.password);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber2 = getBicTestBase().getOrderNumber(testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.orderNumber, orderNumber2);

    results = new HashMap<>(testDataForEachMethod);
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod)));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    populateTestResultsForTestingHub(results);
  }

  @Test(groups = {"direct-order-subscription-status"}, description = "Validation of Subscription status O2P Order")
  public void validateDirectOrderSubscriptionStatus() throws Exception {
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
    getBicTestBase().enterCustomerDetails(billingDetails.address);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    moeTestBase.submitPayment();
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber);
    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    //Get find Subscription ById
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    //Update subscription date using update subscription api and then call BATCH update subscription to update status
    pelicantb.updateO2PSubscriptionStatus(results);
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    AssertUtils.assertEquals("Subscription status is NOT updated", results.get("response_status"),
        testDataForEachMethod.get(BICECEConstants.SUBSCRIPTION_STATUS));
    populateTestResultsForTestingHub(results);
  }

  private void populateTestResultsForTestingHub(HashMap<String, String> results) {
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

  @Test(groups = {"direct-order-renew"}, description = "Validation of Create Direct O2P Order")
  public void directOrderRenew() throws Exception {
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

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Get find Subscription ById
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
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

    //Update Subscription Next renewal date
    pelicantb.updateO2PSubscriptionDates(results);

    results.put(BICConstants.subscriptionId,
        results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID));

    // Trigger the Pelican renewal job to renew the subscription
    pelicantb.renewSubscription(results);
    // Wait for the Pelican job to complete
    Util.sleep(600000);

    // Get the subscription in pelican to check if it has renewed
    testResults.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    try {
      // Ensure that the subscription renews in the future
      String nextBillingDateString = testResults.get("response_nextRenewalDate");
      Util.printInfo("New Billing Date: " + nextBillingDateString);
      Date newBillingDate = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
          nextBillingDateString);
      Assert.assertTrue(newBillingDate.after(new Date()),
          "Check that the O2P subscription has been renewed");

      AssertUtils
          .assertEquals("The billing date has been updated to next cycle ",
              testResults.get("response_nextRenewalDate").split("\\s")[0],
              Util.customDate("MM/dd/yyyy", +1, -5, 0));
    } catch (ParseException e) {
      e.printStackTrace();
    }

    updateTestingHub(testResults);
  }

  @Test(groups = {"create-multiline-order-sus-flex"}, description = "Validation of Create Direct O2P Order SUS Flex")
  public void createMultilineDirectOrderSUSFlex() throws MetadataException {
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectYearlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectFlexTokens();
    dotcomTestBase.selectPurchaseFlexTokens();

    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false); // Rename to createOxygenAccount
    getBicTestBase().enterCustomerDetails(billingDetails.address);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
        billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.orderNumber, orderNumber);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod)));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        user.emailID,
        user.password, results.get(BICECEConstants.SUBSCRIPTION_ID));

    HashMap<String, String> testResults = new HashMap<>();
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

  @Test(groups = {"partial-refund-multiline-order"}, description = "Validation of refund Direct O2P SUS Order")
  public void validatePartialRefundMultilineDirectOrder() throws MetadataException {
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectThreeYearSubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectYearlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);

    getBicTestBase().setStorageData();
    checkoutTestBase.clickOnContinueButton();
    getBicTestBase().createBICAccount(user.names, user.emailID, user.password, false);
    getBicTestBase().enterCustomerDetails(billingDetails.address);
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, billingDetails.paymentCardDetails,
            billingDetails.address);
    getBicTestBase().clickOnContinueBtn(billingDetails.paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    //get purchase order number
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.orderNumber, orderNumber);

    //Get line item details along with subscription id
    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    List<LineItemDTO> lineItemDetailsBeforeRefund = pelicantb.getLineItemDetailsForMultiLineOrder(pelicantb.retryO2PGetPurchaseOrder(testDataForEachMethod), 2);

    // Refund one line item from multi line order
    List<Integer> lineItemRefundedList = new ArrayList<>();
    String refundedLineItemId = lineItemDetailsBeforeRefund.get(0).getLineItemid();
    String refundedSubscriptionId = lineItemDetailsBeforeRefund.get(0).getSubscriptionId();
    String nonRefundedSubscriptionId = lineItemDetailsBeforeRefund.get(1).getSubscriptionId();
    lineItemRefundedList.add(Integer.valueOf(refundedLineItemId));
    pelicantb.createRefundOrderV4(results, lineItemRefundedList);

    // Getting PurchaseOrder details after refund from pelican
    JsonPath poAfterRefundJson = pelicantb.getRefundedPurchaseOrderV4WithPolling(results);
    //Get line item details after refund
    List<LineItemDTO> lineItemDetailsAfterRefund = pelicantb.getLineItemDetailsForMultiLineOrder(poAfterRefundJson.prettify(), 2);

    results.put("refund_orderState", poAfterRefundJson.get("orderState").toString());
    results.put("refund_fulfillmentStatus", poAfterRefundJson.get("fulfillmentStatus"));

    // Verify that Order status is Partially Refunded
    AssertUtils.assertEquals("Order status is NOT PARTIALLY REFUNDED", results.get("refund_orderState"), BICECEConstants.PARTIALLY_REFUNDED);

    //Refunded Line item validation
    //Get Subscription to check if refunded Subscription is in Terminated status
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(refundedSubscriptionId));
    //verify that subscription with refunded line item id is terminated
    AssertUtils.assertEquals("Subscription is TERMINATED", results.get("response_status"),
            BICECEConstants.TERMINATED);
    //Verify that refunded line item status is refunded
    AssertUtils.assertEquals("Line item is Refunded", lineItemDetailsAfterRefund.get(0).getState(),
            BICECEConstants.REFUNDED);

    //Non Refunded Line item validation
    //verify that subscription with non refunded line item id is Active
    AssertUtils.assertEquals("Subscription is ACTIVE", subscriptionServiceV4Testbase.getSubscriptionById(nonRefundedSubscriptionId).get("response_status"),
            BICECEConstants.ACTIVE);
    //Verify that non refunded line item status is CHARGED
    AssertUtils.assertEquals("Line item is Charged", lineItemDetailsAfterRefund.get(1).getState(),
            BICECEConstants.CHARGED);

    HashMap<String, String> testResults = new HashMap<>();
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

}
