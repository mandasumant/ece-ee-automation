package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.BICTestBase;
import com.autodesk.ece.testbase.BICTestBase.Names;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.PWSTestBase;
import com.autodesk.ece.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import io.restassured.path.json.JsonPath;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BICQuoteOrder extends ECETestBase {

  private static final String defaultLocale = "en_US";
  private static final String defaultTaxOption = "undefined";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  private String PASSWORD;
  private PWSTestBase pwsTestBase;

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
    pwsTestBase = new PWSTestBase(testDataForEachMethod.get("pwsClientId"),
        ProtectedConfigFile.decrypt(testDataForEachMethod.get("pwsClientSecret")),
        testDataForEachMethod.get("pwsHostname"));

    Names names = BICTestBase.generateFirstAndLastNames();
    testDataForEachMethod.put(BICECEConstants.FIRSTNAME, names.firstName);
    testDataForEachMethod.put(BICECEConstants.LASTNAME, names.lastName);
    testDataForEachMethod.put(BICECEConstants.emailid, BICTestBase.generateUniqueEmailID());

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
  }

  @Test(groups = {"bic-quoteonly"}, description = "Validation of Create BIC Quote Order")
  public void validateBicQuote() {
    String locale = "en_US";
    if (System.getProperty("locale") != null && !System.getProperty("locale").isEmpty()) {
      locale = System.getProperty("locale");
    }
    testDataForEachMethod.put(BICECEConstants.LOCALE, locale);

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
        PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"),
        testDataForEachMethod);

    HashMap<String, String> justQuoteDeails = new HashMap<String, String>();
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    justQuoteDeails.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
    justQuoteDeails.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

    updateTestingHub(justQuoteDeails);
    Util.printInfo("Final List " + justQuoteDeails.toString());
  }

  @Test(groups = {"bic-quoteorder"}, description = "Validation of Create BIC Quote Order")
  public void validateBicQuoteOrder() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      Names payerNames = BICTestBase.generateFirstAndLastNames();
      String payerEmail = BICTestBase.generateUniqueEmailID();
      Util.printInfo("Payer email: " + payerEmail);
      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(payerNames, payerEmail, PASSWORD, true);
      getBicTestBase().signOutUsingMeMenu();
      testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, payerEmail);
      testResults.put(BICECEConstants.PAYER_EMAIL, payerEmail);
    }

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
        PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"),
        testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().signOutUsingMeMenu();

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);

    if (testDataForEachMethod.get("taxOptionEnabled").equals("Y")) {
      AssertUtils.assertTrue(getBicTestBase().isTTRButtonPresentInCart());
    } else if (testDataForEachMethod.get("taxOptionEnabled").equals("N")) {
      AssertUtils.assertFalse(getBicTestBase().isTTRButtonPresentInCart());
    }

    // Setup test base for Tax Exemption Document submission
    if (Objects.equals(System.getProperty("submitTaxInfo"), BICECEConstants.TRUE)) {
      com.autodesk.testinghub.core.testbase.BICTestBase coreBicTestBase =
          new com.autodesk.testinghub.core.testbase.BICTestBase(getDriver(), getTestBase());

      HashMap<String, String> dataForTTR = new HashMap<String, String>(testDataForEachMethod) {{
        put(BICConstants.exemptFromSalesTax, "Yes");
        put(BICConstants.reasonForExempt, "Reseller");
        put(BICConstants.buyerAccountType, "Reseller");
        put("state", "Colorado");
        put(BICConstants.registeredAs, "Retailer");
        put(BICConstants.salesTaxType, "State Sales Tax");
        put(BICConstants.businessType, "Construction");
        put(BICConstants.certToSelect, "Uniform Sales and Use Tax Certificate - Multijurisdiction");
        put(BICConstants.buyerContactName,
            testDataForEachMethod.get(BICECEConstants.FIRSTNAME) + " " + testDataForEachMethod.get(
                BICECEConstants.LASTNAME));
      }};

      coreBicTestBase.uploadAndPunchOutFlow(dataForTTR, "Tax-Exempt Nonprofit");
      testDataForEachMethod.put("taxOptionEnabled", "N");
    }

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

      // Validate Quote Details with Pelican
      pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

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
        testResults.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));
      } catch (Exception e) {
        Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
      }

      updateTestingHub(testResults);

      if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
        String paymentType = System.getProperty("newPaymentType") != null ? System.getProperty("newPaymentType") :
            System.getProperty(BICECEConstants.STORE).equalsIgnoreCase("STORE-NAMER") ?
                BICECEConstants.VISA : BICECEConstants.CREDITCARD;
        testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, paymentType);
        portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
        portaltb.selectInvoiceAndValidateCreditMemo(results.get(BICECEConstants.ORDER_ID));
        portaltb.payInvoice(testDataForEachMethod);
        portaltb.verifyInvoiceStatus(results.get(BICECEConstants.ORDER_ID));
      } else {
        portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
            results.get(BICConstants.emailid),
            PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
        if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
          portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
        }
      }

      portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));

      if (getBicTestBase().shouldValidateSAP()) {
        portaltb.validateBICOrderTaxInvoice(results);
      }
      updateTestingHub(testResults);
    }
  }

  @Test(groups = {"bic-invoicenotpayment"}, description = "Validate Quote Invoice Not payment")
  public void validateQuoteInvoiceNotPayment() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
                    testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
            PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
            testDataForEachMethod.get("agentContactEmail"),
            testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().signOutUsingMeMenu();

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);


    // Setup test base for Tax Exemption Document submission
    if (Objects.equals(System.getProperty("submitTaxInfo"), BICECEConstants.TRUE)) {
      com.autodesk.testinghub.core.testbase.BICTestBase coreBicTestBase =
              new com.autodesk.testinghub.core.testbase.BICTestBase(getDriver(), getTestBase());

      HashMap<String, String> dataForTTR = new HashMap<String, String>(testDataForEachMethod) {{
        put(BICConstants.exemptFromSalesTax, "Yes");
        put(BICConstants.reasonForExempt, "Reseller");
        put(BICConstants.buyerAccountType, "Reseller");
        put("state", "Colorado");
        put(BICConstants.registeredAs, "Retailer");
        put(BICConstants.salesTaxType, "State Sales Tax");
        put(BICConstants.businessType, "Construction");
        put(BICConstants.certToSelect, "Uniform Sales and Use Tax Certificate - Multijurisdiction");
        put(BICConstants.buyerContactName,
                testDataForEachMethod.get(BICECEConstants.FIRSTNAME) + " " + testDataForEachMethod.get(
                        BICECEConstants.LASTNAME));
      }};

      coreBicTestBase.uploadAndPunchOutFlow(dataForTTR, "Tax-Exempt Nonprofit");
      testDataForEachMethod.put("taxOptionEnabled", "N");
    }

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

      // Validate Quote Details with Pelican
      pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

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
        testResults.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));
      } catch (Exception e) {
        Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
      }

      updateTestingHub(testResults);

      // Commerce api call
      pelicantb.CommerceNotPaymentAPI(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
              testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, true);

      // Getting a PurchaseOrder details from pelican
      JsonPath jp = new JsonPath(pelicantb.getPurchaseOrderV4(results));
      results.put("pelican_orderState", jp.get("orderState").toString());

      // Verify that Order status is not payment
      AssertUtils.assertEquals("Order status should change to Not Payment",
              results.get("pelican_orderState"), "NON_PAYMENT");
    }
  }

  @Test(groups = {"multiline-quoteorder"}, description = "Validation of Create Multiline item quote Order")
  public void validateMultiLineItemQuoteOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);

    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
        PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, true);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);

    // Signing out after quote creation
    getBicTestBase().signOutUsingMeMenu();

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

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

    portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));

    if (getBicTestBase().shouldValidateSAP()) {
      portaltb.validateBICOrderTaxInvoice(results);
    }
    updateTestingHub(testResults);
  }

  @Test(groups = {"quote-RefundOrder"}, description = "Refund Quote orders with and without Credit Memo use cases")
  public void validateQuoteRefundOrder() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
        PASSWORD, true);

    //Create Quote Code to refund

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);

    // Signing out after quote creation
    getBicTestBase().signOutUsingMeMenu();

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));
    results.put(BICECEConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));

    //Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
      portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICECEConstants.emailid),
          PASSWORD);
      if (System.getProperty("issueCreditMemo") != null || System.getProperty("newPaymentType") != null) {
        portaltb.selectInvoiceAndValidateCreditMemo(results.get(BICECEConstants.ORDER_ID));
        // If issueCreditMemo param is passed then we issue Credit memo before we refund
        if (System.getProperty("issueCreditMemo") != null) {
          //Issue Credit Memo to order before Refund
        }
      }

      // Validate Portal. If its LOC and if test has newPaymentType param then we pay the invoice before refund
      if (System.getProperty("newPaymentType") != null) {
        String paymentType = System.getProperty("newPaymentType") != null ? System.getProperty("newPaymentType") :
            System.getProperty(BICECEConstants.STORE).equalsIgnoreCase("STORE-NAMER") ?
                BICECEConstants.VISA : BICECEConstants.CREDITCARD;
        testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, paymentType);

        portaltb.payInvoice(testDataForEachMethod);
        portaltb.verifyInvoiceStatus(results.get(BICECEConstants.ORDER_ID));
      }
    } else {
      portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
          results.get(BICConstants.emailid),
          PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    }

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrderV4(results);

    //Adyen delays in IPN response is causing test failures. Until the issue is resolved lets
    // add additional 6min sleep for the IPN message to come back.
    Util.sleep(360000);

    // Getting a PurchaseOrder details from pelican
    JsonPath jp = new JsonPath(pelicantb.getPurchaseOrderV4(results));
    results.put("refund_orderState", jp.get("orderState").toString());
    results.put("refund_fulfillmentStatus", jp.get("fulfillmentStatus"));

    // Verify that Order status is Refunded
    AssertUtils.assertEquals("Order status is NOT REFUNDED",
        results.get("refund_orderState"), "REFUNDED");

    try {
      testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
      testResults
          .put(TestingHubConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
      testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    if (getBicTestBase().shouldValidateSAP()) {
      // Validate Credit Note for the order
      portaltb.validateBICOrderPDF(results, BICECEConstants.CREDIT_NOTE);
    }

    updateTestingHub(testResults);
  }

  @Test(groups = {"quote-accountportal"}, description = "Validation of Quote purchase from account portal")
  public void validateAccountPortalQuoteOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();

    Address address = getBillingAddress();

    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
        PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"),
        testDataForEachMethod);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);

    portaltb.purchaseQuoteInAccount(testDataForEachMethod.get(BICConstants.cepURL),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);

    getBicTestBase().setStorageData();
    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
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

    portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));

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
      portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    updateTestingHub(testResults);
  }


  @Test(groups = {"bic-quoteorder-multiinvoice"}, description = "Validation of Create BIC Quote Order with Common Payer")
  public void validateBicQuoteOrderMultiInvoice() throws MetadataException {
    LinkedHashMap<String, String> testResults = new LinkedHashMap<String, String>();

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      Names payerNames = BICTestBase.generateFirstAndLastNames();
      String payerEmail = BICTestBase.generateUniqueEmailID();
      Util.printInfo("Payer email: " + payerEmail);
      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(payerNames, payerEmail, PASSWORD, true);
      getBicTestBase().signOutUsingMeMenu();
      testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, payerEmail);
      testResults.put(BICECEConstants.PAYER_EMAIL, payerEmail);
    }

    Address firstAddress = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
        PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(firstAddress, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"),
        testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testDataForEachMethod.put(BICECEConstants.ORGANIZATION_NAME, firstAddress.company);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);
    testResults.putAll(testDataForEachMethod);
    // Signing out after quote creation
    getBicTestBase().signOutUsingMeMenu();

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.ORGANIZATION_NAME, firstAddress.company);
    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
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

      // Validate Quote Details with Pelican
      pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, firstAddress);

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
        testResults.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));
        testResults.put(BICECEConstants.PAYER_EMAIL,results.get(BICConstants.emailid));
      } catch (Exception e) {
        Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
      }

      updateTestingHub(testResults);

      //Sign out from first user session
      getBicTestBase().signOutUsingMeMenu();

      // Creating another order with same Payer
      testDataForEachMethod.put(BICECEConstants.IS_SAME_PAYER, BICECEConstants.TRUE);

      Names  secondUser = BICTestBase.generateFirstAndLastNames();
      String secondUserEmail = BICTestBase.generateUniqueEmailID();

      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(secondUser, secondUserEmail,
          PASSWORD, true);

       quoteId = pwsTestBase.createAndFinalizeQuote(firstAddress, testDataForEachMethod.get("quoteAgentCsnAccount"),
          testDataForEachMethod.get("agentContactEmail"),
          testDataForEachMethod);
      testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
      testResults.put(BICECEConstants.QUOTE_ID, quoteId);
      testResults.put(BICECEConstants.emailid,secondUserEmail);
      updateTestingHub(testResults);
      testResults.putAll(testDataForEachMethod);
      // Signing out after quote creation
      getBicTestBase().signOutUsingMeMenu();

      getBicTestBase().navigateToQuoteCheckout(testResults);
      // Re login during checkout
      getBicTestBase().loginToOxygen(testResults.get(BICECEConstants.emailid), PASSWORD);
      // Place second order with Same Payer
      results = getBicTestBase().placeFlexOrder(testResults);
      results.putAll(testResults);

      updateTestingHub(testResults);
    }
  }


  @Test(groups = {"bic-locnegative"}, description = "Validation LOC Negative Use cases")
  public void validateLocNegativeCases() throws MetadataException {
    String locale = "en_US";
    if (System.getProperty(BICECEConstants.LOCALE) != null && !System.getProperty(BICECEConstants.LOCALE).isEmpty()) {
      locale = System.getProperty(BICECEConstants.LOCALE);
    }
    testDataForEachMethod.put(BICECEConstants.LOCALE, locale);

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)), testDataForEachMethod.get(BICECEConstants.emailid),
        PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
            testDataForEachMethod.get("agentContactEmail"),
            testDataForEachMethod);

    HashMap<String, String> justQuoteDetails = new HashMap<String, String>();
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    AssertUtils.assertFalse(getBicTestBase().isLOCPresentInCart(), "Bug: LOC Payment option should Not be seen");

    justQuoteDetails.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
    justQuoteDetails.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

    updateTestingHub(justQuoteDetails);
  }

      private Address getBillingAddress() {
    String billingAddress;
    String addressViaParam = System.getProperty(BICECEConstants.ADDRESS);
    if (addressViaParam != null && !addressViaParam.isEmpty()) {
      Util.printInfo("The address is passed as parameter : " + addressViaParam);
      billingAddress = addressViaParam;
    } else {
      billingAddress = testDataForEachMethod.get(BICECEConstants.ADDRESS);
    }

    return new Address(billingAddress);
  }
}
