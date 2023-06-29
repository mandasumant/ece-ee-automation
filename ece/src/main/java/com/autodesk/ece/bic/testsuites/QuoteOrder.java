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
import com.autodesk.eceapp.dto.QuoteDetails;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EceBICTestBase.Names;
import com.autodesk.eceapp.testbase.ece.DatastoreClient;
import com.autodesk.eceapp.testbase.ece.DatastoreClient.NewQuoteOrder;
import com.autodesk.eceapp.testbase.ece.DatastoreClient.OrderData;
import com.autodesk.eceapp.testbase.ece.DatastoreClient.OrderFilters;
import com.autodesk.eceapp.testbase.ece.ECETestBase;
import com.autodesk.eceapp.testbase.ece.PWSTestBase;
import com.autodesk.eceapp.testbase.ece.PelicanTestBase;
import com.autodesk.eceapp.testbase.ece.QuoteOrderTestBase;
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
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.util.Strings;

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

  QuoteOrderTestBase quoteOrderTestBase;

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

    final String pwsClientId = testDataForEachMethod.get("pwsClientId");
    final String pwsClientSecret = testDataForEachMethod.get("pwsClientSecret");
    final String pwsClientId_v2 = testDataForEachMethod.get("pwsClientId_v2");
    final String pwsClientSecret_v2 = testDataForEachMethod.get("pwsClientSecret_v2");
    final String pwsHostname = testDataForEachMethod.get("pwsHostname");

    pwsTestBase = new PWSTestBase(pwsClientId, pwsClientSecret, pwsClientId_v2, pwsClientSecret_v2, pwsHostname);
    quoteOrderTestBase = new QuoteOrderTestBase(pwsClientId, pwsClientSecret, pwsClientId_v2, pwsClientSecret_v2, pwsHostname);

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

  @Test(groups = {"quote-only"}, description = "Validation of Create BIC Quote Order")
  public void createQuote() {
    String locale = "en_US";
    Boolean shouldPushToDataStore =
        !Objects.isNull(System.getProperty(BICECEConstants.PROJECT78_PUSH_FLAG)) ? Boolean.valueOf(
            System.getProperty(BICECEConstants.PROJECT78_PUSH_FLAG)) : false;

    Boolean isMultiLineItem =
        !Objects.isNull(System.getProperty(BICECEConstants.IS_MULTILINE)) ? Boolean.valueOf(
            System.getProperty(BICECEConstants.IS_MULTILINE)) : false;

    String scenario = !Objects.isNull(System.getProperty(BICECEConstants.SCENARIO)) ?
            System.getProperty(BICECEConstants.SCENARIO) : "";

    if (System.getProperty("locale") != null && !System.getProperty("locale").isEmpty()) {
      locale = System.getProperty("locale");
    }
    testDataForEachMethod.put(BICECEConstants.LOCALE, locale);

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, isMultiLineItem);

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    HashMap<String, String> justQuoteDetails = new HashMap<String, String>();
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    justQuoteDetails.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
    justQuoteDetails.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

    if (shouldPushToDataStore) {
      try {
        DatastoreClient dsClient = new DatastoreClient();
        NewQuoteOrder.NewQuoteOrderBuilder builder = NewQuoteOrder.builder()
            .name(BICECEConstants.QUOTE_TEST_NAME)
            .emailId(testDataForEachMethod.get(BICECEConstants.emailid))
            .quoteId(quoteId)
            .orderNumber(BigInteger.valueOf(0))
            .paymentType("")
            .locale(locale)
            .address(getSerializedBillingAddress())
            .expiry(new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * 30).toInstant().toString());

        builder.scenario(scenario);

        if (!Objects.isNull(System.getProperty(BICECEConstants.TENANT))) {
          builder.tenant(System.getProperty(BICECEConstants.TENANT));
        }

        OrderData orderData = dsClient.queueOrder(builder.build());
      } catch (Exception e) {
        Util.printWarning("Failed to push order data to data store");
      }
    }

    updateTestingHub(justQuoteDetails);
    Util.printInfo("Final List " + justQuoteDetails);
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

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.SINGLE_FLEX);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:flex,offering_id:OD-000163,term:annual,usage:commercial,plan:standard,quantity:3000");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-annual"}, description = "Validation of Create Quote Order with Annual SUS")
  public void validateQuoteOrderAnnual() throws Exception {
    HashMap<String, String> testResults;

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.SINGLE_ANNUAL);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-myab"}, description = "Validation of Create Quote Order with MYAB SUS")
  public void validateQuoteOrderMYAB() throws Exception {
    HashMap<String, String> testResults;

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.SINGLE_MYAB);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:3_year,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-annual-flex"}, description = "Validation of Create Quote Order with Annual SUS and Flex")
  public void validateQuoteOrderAnnualFlex() throws Exception {
    HashMap<String, String> testResults;

    System.setProperty(BICECEConstants.IS_MULTILINE, "true");

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.MULTI_ANNUAL_FLEX);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard|" +
            "access_model:flex,offering_id:OD-000163,term:annual,usage:commercial,plan:standard,quantity:3000");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-annual-myab"}, description = "Validation of Create Quote Order with Annual and MYAB SUS")
  public void validateQuoteOrderAnnualMYAB() throws Exception {
    HashMap<String, String> testResults;

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.MULTI_ANNUAL_MYAB);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard|" +
            "access_model:sus,offering_id:OD-000021,term:3_year,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-premium"}, description = "Validation of Create Quote Order with Premium SUS")
  public void validateQuoteOrderPremium() throws Exception {
    HashMap<String, String> testResults;

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.SINGLE_PREMIUM);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000321,term:annual,usage:commercial,plan:premium");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
    updateTestingHub(testResults);

  }

  @Test(groups = {"quote-order-annual-flex-myab-premium"}, description = "Validation of Create Quote Order with Annual, Flex, MYAB and Premium SUS")
  public void validateQuoteOrderAnnualMYABFLEXPremium() throws Exception {
    HashMap<String, String> testResults;

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.MULTI_ANNUAL_FLEX_MYAB_PREMIUM);

    String quoteLineItems = System.setProperty("quoteLineItems",
            "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard|" +
                    "access_model:sus,offering_id:OD-000021,term:3_year,usage:commercial,plan:standard|" +
                    "access_model:flex,offering_id:OD-000163,term:annual,usage:commercial,plan:standard,quantity:1000|" +
                    "access_model:sus,offering_id:OD-000321,term:annual,usage:commercial,plan:premium");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
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

    HashMap<String, String> results = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);
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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    testDataForEachMethod.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    updateTestingHub(testResults);

    testDataForEachMethod.put("isReturningUser", BICECEConstants.TRUE);

    if (System.getProperty(BICECEConstants.IS_SAME_PAYER) != null || false) {
      testDataForEachMethod.put(BICECEConstants.IS_SAME_PAYER, System.getProperty(BICECEConstants.IS_SAME_PAYER));
    }

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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    updateTestingHub(testResults);
  }

  @Test(groups = {"quote-refund-annual"}, description = "Validation of Refund of Quote Order with Annual SUS")
  public void validateQuoteRefundOrderAnnual() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Address address = getBillingAddress();

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.SINGLE_ANNUAL);

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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));
    AssertUtils.assertEquals("Subscription is NOT TERMINATED", results.get("response_status"),
        BICECEConstants.TERMINATED);

    updateTestingHub(testResults);
  }

  @Test(groups = {"renew-quote-order-annual"}, description = "Validation of Renewal of Quote Order with Annual SUS")
  public void validateRenewQuoteOrderAnnual() throws Exception {
    HashMap<String, String> testResults;

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.SINGLE_ANNUAL);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);

    testDataForEachMethod.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
        testResults.get(BICConstants.subscriptionId));

    //Update Subscription Next renewal date
    pelicantb.updateO2PSubscriptionForRenewal(testDataForEachMethod);

    // Trigger the Pelican renewal job to renew the subscription
    triggerPelicanRenewalJob(testResults);

    // Get the subscription in pelican to check if it has renewed
    testResults.putAll(subscriptionServiceV4Testbase.getSubscriptionById(
        testResults.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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

    testDataForEachMethod.put(BICECEConstants.SCENARIO, BICECEConstants.SINGLE_ANNUAL);

    String quoteLineItems = System.setProperty("quoteLineItems",
        "access_model:sus,offering_id:OD-000021,term:annual,usage:commercial,plan:standard");
    testDataForEachMethod.put(BICECEConstants.QUOTE_LINE_ITEMS, quoteLineItems);

    testResults = quoteOrderTestBase.createQuoteOrder(
            testDataForEachMethod,
            portaltb, getBicTestBase(),
            pelicantb,
            subscriptionServiceV4Testbase,
            ECETestBase::updateTestingHub);

    testDataForEachMethod.put(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID,
        testResults.get(BICConstants.subscriptionId));

    //Update Subscription for Subscription dates
    pelicantb.updateO2PSubscriptionForRenewal(testDataForEachMethod);

    // Trigger the Pelican renewal job to renew the subscription
    triggerPelicanRenewalJob(testResults);

    // Get the subscription in pelican to check if it has renewed
    testResults.putAll(subscriptionServiceV4Testbase.getSubscriptionById(
        testResults.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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

  private void triggerPelicanRenewalJob(HashMap<String, String> results) {
    PelicanTestBase pelicanTB = new PelicanTestBase();
    pelicanTB.renewSubscription(results);
    // Wait for the Pelican job to complete
    Util.sleep(600000);
  }

}