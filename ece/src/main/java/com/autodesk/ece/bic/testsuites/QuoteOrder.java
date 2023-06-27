package com.autodesk.ece.bic.testsuites;

import static com.autodesk.eceapp.testbase.EceBICTestBase.generateInvoiceDetails;
import static com.autodesk.eceapp.testbase.EceBICTestBase.generatePayerDetails;
import static com.autodesk.eceapp.testbase.EceBICTestBase.generateProductList;
import static com.autodesk.eceapp.testbase.EceBICTestBase.generatePurchaserDetails;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.dto.IInvoiceDetails;
import com.autodesk.eceapp.dto.IPayerDetails;
import com.autodesk.eceapp.dto.IProductDetails;
import com.autodesk.eceapp.dto.IPurchaserDetails;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EceBICTestBase.Names;
import com.autodesk.eceapp.testbase.ece.ECETestBase;
import com.autodesk.eceapp.testbase.ece.PWSTestBase;
import com.autodesk.eceapp.testbase.ece.PelicanTestBase;
import com.autodesk.eceapp.utilities.Address;
import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import com.autodesk.testinghub.eseapp.constants.TestingHubConstants;
import io.restassured.path.json.JsonPath;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QuoteOrder extends ECETestBase {

  private static final String defaultLocale = "en_US";
  private static final String defaultTaxOption = "undefined";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  Map<?, ?> bankInformationByLocaleYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  private String PASSWORD;
  private PWSTestBase pwsTestBase;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs(getDriver());
    loadYaml = ResourceFileLoader.getBicOrderYaml();
    localeConfigYaml = ResourceFileLoader.getLocaleConfigYaml();
    bankInformationByLocaleYaml = ResourceFileLoader.getBankInformationByLocaleYaml();
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml.get("default");
    LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml.get(name.getName());

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

      // Limitation of our localeConfig, our locale is configured to support 1 store
      // with 1 currency setting
      // Workaround to above issue is to have this currency condition (a dirty fix)
      // for Quote and EURO only.
      if (!Arrays.stream(testDataForEachMethod.get(BICECEConstants.PAYMENT_METHODS).split(","))
          .equals(BICECEConstants.LOC)) {
        testDataForEachMethod.put(BICECEConstants.PAYMENT_METHODS,
            testDataForEachMethod.get(BICECEConstants.PAYMENT_METHODS) + "," + BICECEConstants.LOC);
      }
    }

    Util.printInfo("Validating the store for the locale :" + locale + " Store : "
        + System.getProperty(BICECEConstants.STORE));

    boolean isValidStore = testDataForEachMethod.get(BICECEConstants.STORE_NAME)
        .equals(System.getProperty(BICECEConstants.STORE));

    if (!isValidStore) {
      AssertUtils.fail("The store is not supported for the given country/locale : " + locale
          + ". Supported stores are " + testDataForEachMethod.get(BICECEConstants.STORE_NAME));
    }

    if (testDataForEachMethod.get(BICECEConstants.ADDRESS) == null
        || testDataForEachMethod.get(BICECEConstants.ADDRESS).isEmpty()) {
      AssertUtils.fail("Address not found in the config for the locale : " + locale);
    }

    if (taxOptionEnabled == null || taxOptionEnabled.trim().isEmpty()) {
      taxOptionEnabled = defaultTaxOption;
    }
    testDataForEachMethod.put("taxOptionEnabled", taxOptionEnabled);

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);
    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));
    pwsTestBase = new PWSTestBase(
        testDataForEachMethod.get("pwsClientId"),
        testDataForEachMethod.get("pwsClientSecret"),
        testDataForEachMethod.get("pwsClientId_v2"),
        testDataForEachMethod.get("pwsClientSecret_v2"),
        testDataForEachMethod.get("pwsHostname"));

    Names names = EceBICTestBase.generateFirstAndLastNames();
    testDataForEachMethod.put(BICECEConstants.FIRSTNAME, names.firstName);
    testDataForEachMethod.put(BICECEConstants.LASTNAME, names.lastName);
    testDataForEachMethod.put(BICECEConstants.emailid, EceBICTestBase.generateUniqueEmailID());

    if (System.getProperty("quantity1") != null) {
      testDataForEachMethod.put(BICECEConstants.FLEX_TOKENS, System.getProperty("quantity1"));
    } else {
      testDataForEachMethod.put(BICECEConstants.FLEX_TOKENS,
          String.valueOf((int) ((Math.random() * (15000 - 100)) + 1000)));
    }

    if (System.getProperty("currency") != null) {
      testDataForEachMethod.put("currencyStore", System.getProperty("currency"));
    }

    if (System.getProperty("agentCSN") != null) {
      testDataForEachMethod.put("quoteAgentCsnAccount", System.getProperty("agentCSN"));
    }

    if (System.getProperty("agentEmail") != null) {
      testDataForEachMethod.put("agentContactEmail", System.getProperty("agentEmail"));
    }

    testDataForEachMethod.put(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE,
        PWSTestBase.getQuoteStartDateAsString());

    if(System.getProperty("subscriptionStatus") !=null){
      testDataForEachMethod.put(BICECEConstants.SUBSCRIPTION_STATUS, System.getProperty("subscriptionStatus"));
    }

  }

  @Test(groups = {"quote-order"}, description = "Validation of Create BIC Quote Order")
  public void validateQuoteOrder() throws Exception {
    HashMap<String, String> testResults = new HashMap<>();
    HashMap<String, String> results;

    List<IProductDetails> productDetailsList = generateProductList();
    IPurchaserDetails purchaserDetails = generatePurchaserDetails();
    IPayerDetails payerDetails = generatePayerDetails();
    IInvoiceDetails InvoiceDetails = generateInvoiceDetails();

    results = getBicTestBase().createQuote2Order(testDataForEachMethod, productDetailsList, purchaserDetails,
        payerDetails, InvoiceDetails);

    results.putAll(testDataForEachMethod);

    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);
  }

  @Test(groups = {"quote-order-flex"}, description = "Validation of Create Quote Order with Annual Flex")
  public void validateQuoteOrderFlex() throws Exception {
    HashMap<String, String> testResults;

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:flex,offering_id:OD-000163,term:annual,usage:commercial,plan:standard,quantity:3000");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = createQuoteOrder(testDataForEachMethod);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-annual"}, description = "Validation of Create Quote Order with Annual SUS")
  public void validateQuoteOrderAnnual() throws Exception {
    HashMap<String, String> testResults;

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = createQuoteOrder(testDataForEachMethod);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-myab"}, description = "Validation of Create Quote Order with MYAB SUS")
  public void validateQuoteOrderMYAB() throws Exception {
    HashMap<String, String> testResults;

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:3_year,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = createQuoteOrder(testDataForEachMethod);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-annual-flex"}, description = "Validation of Create Quote Order with Annual SUS and Flex")
  public void validateQuoteOrderAnnualFlex() throws Exception {
    HashMap<String, String> testResults;

    System.setProperty(BICECEConstants.IS_MULTILINE, "true");

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard|" +
            "access_model:flex,offering_id:OD-000163,term:annual,usage:commercial,plan:standard,quantity:3000");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = createQuoteOrder(testDataForEachMethod);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-annual-myab"}, description = "Validation of Create Quote Order with Annual and MYAB SUS")
  public void validateQuoteOrderAnnualMYAB() throws Exception {
    HashMap<String, String> testResults;

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard|" +
            "access_model:sus,offering_id:OD-000021,term:3_year,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = createQuoteOrder(testDataForEachMethod);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-returning-user"}, description = "Validation of Create Quote Order for Returning user")
  public void validateQuoteOrderReturningUser() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();
    Address address = getBillingAddress();

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      testResults = getBicTestBase().createPayerAccount(testDataForEachMethod);
    }

    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, false);

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 5min for Quote to sync to S4");
    Util.sleep(300000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();

    HashMap<String, String> results = getBicTestBase().createQuoteOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);
    testResults.put(BICECEConstants.orderNumber, results.get(BICConstants.orderNumber));

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    updateTestingHub(testResults);

    testDataForEachMethod.put("isReturningUser", BICECEConstants.TRUE);

    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    String quote2Id = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, false);

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 5min for Quote to sync to S4");
    Util.sleep(300000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quote2Id);

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);

    results = getBicTestBase().createQuoteOrder(testDataForEachMethod);
    testResults.put(BICECEConstants.orderNumber + "_2", results.get(BICConstants.orderNumber));
    results.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    updateTestingHub(testResults);
  }

  @Test(groups = {"quote-refund-annual"}, description = "Validation of Refund of Quote Order with Annual SUS")
  public void validateQuoteRefundOrderAnnual() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Address address = getBillingAddress();

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      testResults = getBicTestBase().createPayerAccount(testDataForEachMethod);
    }

    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, false);

    // Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 5min for Quote to sync to S4");
    Util.sleep(300000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);

    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    testResults.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
    testResults.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    String oxygenId = getBicTestBase().driver.manage().getCookieNamed("identity-sso").getValue();
    testDataForEachMethod.put(BICConstants.oxid, oxygenId);

    testResults.putAll(results);
    updateTestingHub(testResults);

    results = getBicTestBase().createQuoteOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

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

    try {
      testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
      testResults.put(TestingHubConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    //Get Subscription to check if Subscription is in Terminated status
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    AssertUtils.assertEquals("Subscription is NOT TERMINATED", results.get("response_status"),
        BICECEConstants.TERMINATED);

    updateTestingHub(testResults);
  }

  @Test(groups = {"renew-quote-order-annual"}, description = "Validation of Renewal of Quote Order with Annual SUS")
  public void validateRenewQuoteOrderAnnual() throws Exception {
    HashMap<String, String> testResults;

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = createQuoteOrder(testDataForEachMethod);

    testDataForEachMethod.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
        testResults.get(BICConstants.subscriptionId));

    //Update Subscription Next renewal date
    pelicantb.updateO2PSubscriptionForRenewal(testDataForEachMethod);

    // Trigger the Pelican renewal job to renew the subscription
    triggerPelicanRenewalJob(testResults);

    // Get the subscription in pelican to check if it has renewed
    testResults.putAll(subscriptionServiceV4Testbase.getSubscriptionById(testResults));

    try {
      // Ensure that the subscription renews in the future
      String nextBillingDateString = testResults.get(BICECEConstants.NEXT_BILLING_DATE);
      Util.printInfo("New Billing Date: " + nextBillingDateString);
      Date newBillingDate = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
          nextBillingDateString);
      Assert.assertTrue(newBillingDate.after(new Date()),
          "Check that the O2P subscription has been renewed");

      AssertUtils
          .assertEquals("The billing date has been updated to next cycle ",
              testResults.get(BICECEConstants.NEXT_BILLING_DATE).split("\\s")[0],
              Util.customDate("MM/dd/yyyy", 0, -5, +1));
    } catch (ParseException e) {
      e.printStackTrace();
    }

    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-subscription-status"}, description = "Validation of Quote Order Subscription status")
  public void validateQuoteOrderSubscriptionStates() throws Exception {
    HashMap<String, String> testResults;

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = createQuoteOrder(testDataForEachMethod);

    testDataForEachMethod.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
        testResults.get(BICConstants.subscriptionId));

    //Update Subscription for Subscription dates
    pelicantb.updateO2PSubscriptionForRenewal(testDataForEachMethod);

    // Trigger the Pelican renewal job to renew the subscription
    triggerPelicanRenewalJob(testResults);

    // Get the subscription in pelican to check if it has renewed
    testResults.putAll(subscriptionServiceV4Testbase.getSubscriptionById(testResults));

    try {
      // Ensure that the subscription renews in the future
      String dateString = testResults.get(BICECEConstants.NEXT_BILLING_DATE);
      Date date = new SimpleDateFormat(BICECEConstants.DATE_FORMAT).parse(
          dateString);
      Assert.assertTrue(date.after(new Date()),
          "Check that the O2P subscription has been renewed");

      AssertUtils
          .assertEquals("The billing date has been updated to next cycle ",
              testResults.get(BICECEConstants.NEXT_BILLING_DATE).split("\\s")[0],
              Util.customDate("MM/dd/yyyy", 0, -5, +1));
    } catch (ParseException e) {
      e.printStackTrace();
    }

    updateTestingHub(testResults);

  }

  private String getSerializedBillingAddress() {
    String billingAddress;
    String addressViaParam = System.getProperty(BICECEConstants.ADDRESS);
    if (addressViaParam != null && !addressViaParam.isEmpty()) {
      Util.printInfo("The address is passed as parameter : " + addressViaParam);
      billingAddress = addressViaParam;
    } else {
      billingAddress = testDataForEachMethod.get(BICECEConstants.ADDRESS);
    }
    return billingAddress;
  }

  private Address getBillingAddress() {
    String billingAddress = getSerializedBillingAddress();
    return new Address(billingAddress);
  }

  private HashMap<String, String> createQuoteOrder(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Address address = getBillingAddress();

    Boolean isMultiLineItem =
        !Objects.isNull(System.getProperty(BICECEConstants.IS_MULTILINE)) ? Boolean.valueOf(
            System.getProperty(BICECEConstants.IS_MULTILINE)) : false;

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      testResults = getBicTestBase().createPayerAccount(testDataForEachMethod);
    }

    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, isMultiLineItem);

    // Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 5min for Quote to sync to S4");
    Util.sleep(300000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);

    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(
        testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    testResults.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
    testResults.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    String oxygenId = getBicTestBase().driver.manage().getCookieNamed("identity-sso").getValue();
    testDataForEachMethod.put(BICConstants.oxid, oxygenId);

    testResults.putAll(results);
    updateTestingHub(testResults);

    results = getBicTestBase().createQuoteOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICECEConstants.ORDER_ID, results.get(BICECEConstants.ORDER_ID));
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
      testResults.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));
    testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
    return testResults;
  }

  private void triggerPelicanRenewalJob(HashMap<String, String> results) {
    PelicanTestBase pelicanTB = new PelicanTestBase();
    pelicanTB.renewSubscription(results);
    // Wait for the Pelican job to complete
    Util.sleep(600000);
  }

}