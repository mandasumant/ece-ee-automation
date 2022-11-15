package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.BICTestBase;
import com.autodesk.ece.testbase.BICTestBase.Names;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.PWSTestBase;
import com.autodesk.ece.testbase.PelicanTestBase;
import com.autodesk.ece.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.EISTestBase;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.testbase.TestinghubUtil;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import io.restassured.path.json.JsonPath;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.util.Strings;

public class BICQuoteOrder extends ECETestBase {

  private static final String defaultLocale = "en_US";
  private static final String defaultTaxOption = "undefined";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  String creditMemo = System.getProperty(BICECEConstants.CREDIT_MEMO);

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

    if (creditMemo == null || creditMemo.isEmpty()) {
      testDataForEachMethod.put(BICECEConstants.CREDIT_MEMO, "500");
    } else {
      testDataForEachMethod.put(BICECEConstants.CREDIT_MEMO, System.getProperty("creditMemo"));
    }

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
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);

    HashMap<String, String> justQuoteDeails = new HashMap<String, String>();
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    justQuoteDeails.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
    justQuoteDeails.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

    updateTestingHub(justQuoteDeails);
    Util.printInfo("Final List " + justQuoteDeails);
  }

  @Test(groups = {"bic-quoteorder"}, description = "Validation of Create BIC Quote Order")
  public void validateBicQuoteOrder() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      Names payerNames = BICTestBase.generateFirstAndLastNames();
      String payerEmail = BICTestBase.generateUniqueEmailID();
      Util.printInfo("Payer email: " + payerEmail);
      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(payerNames, payerEmail, PASSWORD, true);
      getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
      getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
      testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, payerEmail);
      testResults.put(BICECEConstants.PAYER_EMAIL, payerEmail);
    }

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();

    if (Objects.equals(testDataForEachMethod.get("ttrEnabled"), "true")) {
      if (testDataForEachMethod.get("taxOptionEnabled").equals("Y")) {
        AssertUtils.assertTrue(getBicTestBase().isTTRButtonPresentInCart(), "TTR button should be present");
      } else if (testDataForEachMethod.get("taxOptionEnabled").equals("N")) {
        AssertUtils.assertFalse(getBicTestBase().isTTRButtonPresentInCart(), "TTR button should be hidden");
      }
    } else {
      AssertUtils.assertFalse(getBicTestBase().isTTRButtonPresentInCart(),
          "TTR button should not be present for this scenario");
    }

    // Setup test base for Tax Exemption Document submission
    if (Objects.equals(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO), BICECEConstants.TRUE)) {
      com.autodesk.testinghub.core.testbase.BICTestBase coreBicTestBase = new com.autodesk.testinghub.core.testbase.BICTestBase(
          getDriver(), getTestBase());

      HashMap<String, String> dataForTTR = new HashMap<String, String>(testDataForEachMethod) {
        {
          put(BICConstants.exemptFromSalesTax, "Yes");
          put(BICConstants.reasonForExempt, "Reseller");
          put(BICConstants.buyerAccountType, "Reseller");
          put(TestingHubConstants.state, address.provinceName);
          put(BICConstants.registeredAs, "Retailer");
          put(BICConstants.salesTaxType, "State Sales Tax");
          put(BICConstants.businessType, "Construction");
          put(BICConstants.certToSelect, "Uniform Sales and Use Tax Certificate - Multijurisdiction");
          put(BICConstants.buyerContactName, testDataForEachMethod.get(BICECEConstants.FIRSTNAME) + " "
              + testDataForEachMethod.get(BICECEConstants.LASTNAME));
          put(BICConstants.certificateName,
              EISTestBase.getTestManifest().getProperty("ECMS_TTR_TEST_DOCUMENT"));
        }
      };

      switch (address.country) {
        case "Canada":
          switch (address.province) {
            case "BC":
            case "MB":
            case "SK":
              dataForTTR.put(BICConstants.canadianTaxType, "Canada Goods and Services Tax (GST)");
              break;
          }
          dataForTTR.put(BICConstants.buyerAccountType, "Government of Canada");
          break;
        case "United States":
          switch (address.province) {
            case "MS":
            case "MA":
              dataForTTR.put(BICConstants.identityNumberLength, "9");
              break;
            case "MD":
              dataForTTR.put(BICConstants.identityNumberLength, "8");
          }
        default:
          dataForTTR.put(BICConstants.buyerAccountType, "Reseller");
          break;
      }

      coreBicTestBase.uploadAndPunchOutFlow(dataForTTR);
      testDataForEachMethod.put("taxOptionEnabled", "N");
    }

    results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      return;
    }

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    AssertUtils.assertEquals(Boolean.valueOf(results.get(BICECEConstants.IS_TAX_EXCEMPT)),
        Boolean.valueOf(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO)),
        "Pelican 'Tax Exempt' flag didnt match with Test Param");

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

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
      portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));

    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-loc-payinvoice"}, description = "Validation for Pay Invoice")
  public void validateLocPayInvoice() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Boolean isLoggedIn = true;
    Integer attempt = 0;

    String creditMemoAmount = testDataForEachMethod.get("creditMemo");
    Util.printInfo("The Credit Memo amount is: " + creditMemoAmount);

    loadPrevTransactionOut();
    results.putAll(testDataForEachMethod);
    if (results.containsKey(BICConstants.orderNumber) && results.get(BICConstants.orderNumber) != null) {
      results.put(BICECEConstants.ORDER_ID, results.get(BICConstants.orderNumber));
    }

    if (testDataForEachMethod.containsKey("Placing the Flex Order")
        && testDataForEachMethod.get("Placing the Flex Order").equalsIgnoreCase("Passed")) {
      if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
        String paymentType = System.getProperty("newPaymentType") != null ? System.getProperty("newPaymentType")
            : System.getProperty(BICECEConstants.STORE).equalsIgnoreCase("STORE-NAMER")
                ? BICECEConstants.VISA
                : BICECEConstants.CREDITCARD;
        testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, paymentType);
        System.setProperty(BICECEConstants.PAYMENT, paymentType);

        //For purchaser as Payer test cases we need to update the Payer email address for login.
        if (!testDataForEachMethod.containsKey(BICECEConstants.PAYER_EMAIL)
            || testDataForEachMethod.get(BICECEConstants.PAYER_EMAIL) == null) {
          testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, testDataForEachMethod.get(BICConstants.emailid));
        }
        while (isLoggedIn) {
          attempt++;
          if (attempt > 5) {
            Assert.fail("Retries Exhausted: Payment of Invoice failed because Session issues. Check Screenshots!");
          }

          portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICECEConstants.PAYER_EMAIL),
              PASSWORD);

          if (System.getProperty("issueCreditMemo") != null) {
            portaltb.navigateToInvoiceCreditMemos();
            portaltb.waitForInvoicePageLoadToVisible();

            // Issue Credit Memo to order before Refund
            if (Strings.isNotNullAndNotEmpty(creditMemoAmount)) {
              testDataForEachMethod.put(TestingHubConstants.creditMemoAmount, creditMemoAmount);
            }

            if (Strings.isNotNullAndNotEmpty(results.get(BICECEConstants.ORDER_ID))) {
              String somOrderNumber = getSOMOrderNumber(results.get(BICECEConstants.ORDER_ID));
              Util.printInfo("SOM order number found - " + somOrderNumber);

              HashMap<String, String> invoiceDetails = saptb.sapConnector.getInvoiceDetailsFromTableUsingSOM(
                  somOrderNumber);
              if (invoiceDetails.size() == 0) {
                AssertUtils.fail("Failed to get invoice details from SAP for the initial order : " + results.get(
                    BICECEConstants.ORDER_ID));
              }

              Util.printInfo("Found invoice details " + invoiceDetails);

              // Get credit memo invoice numbers list if credit memo is available from S4
              HashMap<String, LinkedList<String>> creditMemoDetails = saptb.sapConnector.getCMInvDetailsFromTableUsingSOM(
                  somOrderNumber);
              int cmInvB4Order = creditMemoDetails.size();
              Util.printInfo("Total credit memos available in S4 : " + cmInvB4Order);
              if (cmInvB4Order == 0) {
                Util.printInfo("There are no credit memo invoices in S4 yet.");
              }

              sapfioritb.loginToSAPFiori();
              String invoiceNumber = invoiceDetails.get(TestingHubConstants.invoiceNumber);
              String creditMemo = sapfioritb.createCreditMemoOrder(invoiceNumber, results.get(BICECEConstants.ORDER_ID),
                  creditMemoAmount);
              results.put(TestingHubConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
              results.put(TestingHubConstants.creditMemoOrderNumber, creditMemo);
              Util.printInfo("creditMemo return value :: " + creditMemo);
              results.put(TestingHubConstants.invoiceNumber, invoiceNumber);
              results.put(TestingHubConstants.somOrderNumber, somOrderNumber);

              // Get new credit memo invoice number (Below step often fail to return cm within 10min.)
              String creditMemoInvoiceNo = saptb.sapConnector.getCMInvNoForNewCMOrderUsingSOM(
                  results.get(BICECEConstants.ORDER_ID), somOrderNumber,
                  cmInvB4Order);
              results.put(TestingHubConstants.creditMemoInvoiceNumber, creditMemoInvoiceNo);
              updateTestingHub(results);
              Util.printInfo("creditMemoInvoiceNo return value :: " + creditMemoInvoiceNo);

              // Portal Validations of credit memo
              portaltb.validateNewCreditMemoInPortal(
                  results.get(TestingHubConstants.creditMemoInvoiceNumber));

            } else {
              AssertUtils.fail("Please provide the initial order. Order number is null or empty");
            }
          }

          double invoiceTotalBeforePayment = portaltb.selectInvoiceAndValidateCreditMemo(
              results.get(BICECEConstants.ORDER_ID), false);
          isLoggedIn = portaltb.payInvoice(testDataForEachMethod);
          portaltb.verifyInvoiceTotalAfterPayment(invoiceTotalBeforePayment);
        }
        portaltb.verifyInvoiceStatus(results.get(BICECEConstants.ORDER_ID));

        if (System.getProperty("issueCreditMemo") != null) {
          portaltb.verifyCreditMemoStatus("//*[@data-testid=\"credit-memo-list-empty-container\"]");
        }

      } else {
        Assert.fail("NON LOC Orders Do NOT have Pay Invoice Flow!!!");
      }

      if (getBicTestBase().shouldValidateSAP()) {
        portaltb.validateBICOrderTaxInvoice(results);
      }
      updateTestingHub(testResults);
    } else {
      Assert.fail("Pay By Invoice Step failed in the previous transaction, so failing Pay Invoice Testcase!!!");
    }
  }

  @Test(groups = {"bic-invoicenonpayment"}, description = "Validate Quote Invoice Non payment")
  public void validateQuoteInvoiceNonPayment() throws Exception {
    HashMap<String, String> testResults = new HashMap<>();
    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);

    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
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

      // Sleep for the Order orchestration SQS event to be processed in Pelican
      Util.sleep(120000);
      updateTestingHub(testResults);

      // Commerce api call
      PelicanTestBase.commerceNotPaymentAPI(results);

      // Getting a PurchaseOrder details from pelican
      JsonPath jp = new JsonPath(pelicantb.getPurchaseOrderV4(results));

      // Verify that Order status is Not Payment
      AssertUtils.assertEquals("Order status should change to Not Payment", jp.get("orderState").toString(),
          "NON_PAYMENT");
    }
    updateTestingHub(testResults);
  }

  @Test(groups = {"multiline-quoteorder"}, description = "Validation of Create Multiline item quote Order")
  public void validateMultiLineItemQuoteOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<>();

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);

    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, true);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);

    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
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
      testResults.put(BICECEConstants.ORDER_ID, results.get(BICECEConstants.ORDER_ID));
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
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));

    portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));

    if (getBicTestBase().shouldValidateSAP()) {
      portaltb.validateBICOrderTaxInvoice(results);
    }
    updateTestingHub(testResults);
  }

  @Test(groups = {"quote-RefundOrder"}, description = "Refund Quote order")
  public void validateQuoteRefundOrder() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    // Create Quote Code to refund

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);

    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
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

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrderV4(results);

    // Adyen delays in IPN response is causing test failures. Until the issue is
    // resolved lets
    // add additional 6min sleep for the IPN message to come back.
    Util.sleep(360000);

    // Getting a PurchaseOrder details from pelican
    JsonPath jp = new JsonPath(pelicantb.getPurchaseOrderV4(results));
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
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
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
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    updateTestingHub(testResults);
  }

  @Test(groups = {
      "bic-quoteorder-multiinvoice"}, description = "Validation of Create BIC Quote Order with Common Payer")
  public void validateBicQuoteOrderMultiInvoice() throws Exception {

    LinkedHashMap<String, String> testResults = new LinkedHashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();

    Address firstAddress = getBillingAddress();

    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String purchaser = testDataForEachMethod.get(BICECEConstants.emailid);
    String quoteId = pwsTestBase.createAndFinalizeQuote(firstAddress,
        testDataForEachMethod.get("quoteAgentCsnAccount"), testDataForEachMethod.get("agentContactEmail"),
        testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testDataForEachMethod.put(BICECEConstants.ORGANIZATION_NAME, firstAddress.company);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);
    testResults.putAll(testDataForEachMethod);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.ORGANIZATION_NAME, firstAddress.company);
    // Re login during checkout
    getBicTestBase().loginToOxygen(purchaser, PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    String multiOrders = results.get(BICECEConstants.orderNumber);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      Util.sleep(120000);
    }

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      return;
    }

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
      testResults.put(BICECEConstants.PAYER_EMAIL, results.get(BICConstants.emailid));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    updateTestingHub(testResults);

    // Sign out from first user session
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    // Creating another order with same Payer
    testDataForEachMethod.put(BICECEConstants.IS_SAME_PAYER, BICECEConstants.TRUE);
    testDataForEachMethod.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));
    testResults.put(BICECEConstants.IS_SAME_PAYER, BICECEConstants.TRUE);
    testResults.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));
    Names secondUser = BICTestBase.generateFirstAndLastNames();
    String secondUserEmail = BICTestBase.generateUniqueEmailID();

    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(secondUser, secondUserEmail, PASSWORD, true);
    testDataForEachMethod.put(BICECEConstants.emailid, secondUserEmail);
    testResults.put(BICECEConstants.PAYER_EMAIL, purchaser);

    quoteId = pwsTestBase.createAndFinalizeQuote(firstAddress, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.emailid, secondUserEmail);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    getBicTestBase().navigateToQuoteCheckout(testResults);
    // Re login during checkout
    getBicTestBase().loginToOxygen(testResults.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    // Place second order with Same Payer
    results = getBicTestBase().placeFlexOrder(testResults);
    multiOrders = multiOrders + "," + results.get(BICConstants.orderNumber);
    // Appending both the Purchase Orders for Multi Pay Invoice tests
    testResults.put(BICConstants.orderNumber, multiOrders);

    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-locnegative"}, description = "Validation LOC Negative Use cases")
  public void validateLocNegativeCases() throws MetadataException {
    String locale = "en_US";
    if (System.getProperty(BICECEConstants.LOCALE) != null
        && !System.getProperty(BICECEConstants.LOCALE).isEmpty()) {
      locale = System.getProperty(BICECEConstants.LOCALE);
    }
    testDataForEachMethod.put(BICECEConstants.LOCALE, locale);

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);

    HashMap<String, String> justQuoteDetails = new HashMap<String, String>();
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    AssertUtils.assertFalse(getBicTestBase().isLOCPresentInCart(), "Bug: LOC Payment option should Not be seen");

    justQuoteDetails.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
    justQuoteDetails.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

    updateTestingHub(justQuoteDetails);
  }

  @Test(groups = {"bic-sus-quote-orders"}, description = "Validation of Create BIC SUS  Order and Quote orders")
  public void validateBicSUSAndQuoteOrders() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = getBicTestBase().createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
    testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));

    //Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

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
      testResults.put(BICConstants.payment_ProfileId, results.get(BICECEConstants.PAYMENT_PROFILE_ID));
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

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      Names payerNames = BICTestBase.generateFirstAndLastNames();
      String payerEmail = BICTestBase.generateUniqueEmailID();
      Util.printInfo("Payer email: " + payerEmail);
      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(payerNames, payerEmail, PASSWORD, true);
      getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
      getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
      testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, payerEmail);
      testResults.put(BICECEConstants.PAYER_EMAIL, payerEmail);
    }

    testDataForEachMethod.put(BICECEConstants.PAYMENT, "LOC");
    testDataForEachMethod.put("productType", "flex");
    Address address = getBillingAddress();

    getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, "LOC");
    testDataForEachMethod.put("isReturningUser", "true");
    results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.putAll(results);
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    AssertUtils.assertEquals(Boolean.valueOf(results.get(BICECEConstants.IS_TAX_EXCEMPT)),
        Boolean.valueOf(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO)),
        "Pelican 'Tax Exempt' flag didnt match with Test Param");

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

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
      portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));

    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-returning-quote-user"}, description = "Validation Returning Quote User")
  public void validateReturningQuoteUser() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    boolean changeAddress = Boolean.parseBoolean(System.getProperty("changeAddress"));
    boolean submitTaxInfo = Boolean.parseBoolean(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO));

    Address address = getBillingAddress();
    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(
        new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
            testDataForEachMethod.get(BICECEConstants.LASTNAME)),
        testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();

    // Setup test base for Tax Exemption Document submission
    if (Objects.equals(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO), BICECEConstants.TRUE)) {
      com.autodesk.testinghub.core.testbase.BICTestBase coreBicTestBase = new com.autodesk.testinghub.core.testbase.BICTestBase(
          getDriver(), getTestBase());

      HashMap<String, String> dataForTTR = new HashMap<String, String>(testDataForEachMethod) {
        {
          put(BICConstants.exemptFromSalesTax, "Yes");
          put(BICConstants.reasonForExempt, "Reseller");
          put(BICConstants.buyerAccountType, "Reseller");
          put(BICConstants.registeredAs, "Retailer");
          put(BICConstants.salesTaxType, "State Sales Tax");
          put(BICConstants.businessType, "Construction");
          put(BICConstants.certToSelect, "Uniform Sales and Use Tax Certificate - Multijurisdiction");
          put(BICConstants.buyerContactName, testDataForEachMethod.get(BICECEConstants.FIRSTNAME) + " "
              + testDataForEachMethod.get(BICECEConstants.LASTNAME));
          put(BICConstants.certificateName,
              EISTestBase.getTestManifest().getProperty("ECMS_TTR_TEST_DOCUMENT"));
        }
      };

      dataForTTR.put("state", address.provinceName);

      switch (address.country) {
        case "Canada":
          switch (address.province) {
            case "BC":
            case "MB":
            case "SK":
              dataForTTR.put(BICConstants.canadianTaxType, "Canada Goods and Services Tax (GST)");
              break;
          }
          dataForTTR.put(BICConstants.buyerAccountType, "Government of Canada");
          break;
        case "United States":
        default:
          dataForTTR.put(BICConstants.buyerAccountType, "Reseller");
          break;
      }

      coreBicTestBase.uploadAndPunchOutFlow(dataForTTR);

      getBicTestBase().validateUserTaxExempt(true);
      testDataForEachMethod.put("taxOptionEnabled", "N");
    }

    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);
    testResults.put(BICECEConstants.orderNumber, results.get(BICConstants.orderNumber));

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    AssertUtils.assertEquals(Boolean.valueOf(results.get(BICECEConstants.IS_TAX_EXCEMPT)),
        Boolean.valueOf(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO)),
        "Pelican 'Tax Exempt' flag didnt match with Test Param");

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
      portaltb.validateBICOrderTotal(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    updateTestingHub(testResults);

    testDataForEachMethod.put("isReturningUser", BICECEConstants.TRUE);

    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    if (submitTaxInfo) {
      // Wait 7.5 minutes for returning tax-exempt user
      Util.sleep(450000);
    }

    if (changeAddress) {
      testDataForEachMethod.putAll(localeDataMap.get(defaultLocale));
      address = new Address(testDataForEachMethod.get(BICECEConstants.ADDRESS));
      testDataForEachMethod.put("taxOptionEnabled", "Y");
      System.setProperty(BICECEConstants.SUBMIT_TAX_INFO, "false");
    }

    String quote2Id = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quote2Id);

    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);

    // On the second visit to checkout validate that the user is still tax exempt
    getBicTestBase().validateUserTaxExempt(
        Objects.equals(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO), BICECEConstants.TRUE)
            && !changeAddress);

    results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    testResults.put(BICECEConstants.orderNumber + "_2", results.get(BICConstants.orderNumber));
    results.putAll(testDataForEachMethod);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    if (submitTaxInfo && !changeAddress) {
      AssertUtils.assertTrue(Objects.equals(results.get(BICECEConstants.IS_TAX_EXCEMPT), "null"),
          "Pelican 'Tax Exempt' flag should be null for returning tax exempt users");
    } else {
      AssertUtils.assertEquals(Boolean.valueOf(results.get(BICECEConstants.IS_TAX_EXCEMPT)),
          false, "Pelican 'Tax Exempt' flag didnt match with Test Param");
    }

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));

    updateTestingHub(testResults);
  }

  @Test(groups = {"pay-by-invoice-block"}, description = "Validate Pay By Invoice Block User")
  public void validatePayByInvoiceBlockUser() throws MetadataException {
    getPortalTestBase().navigateToLoginToPayByInvoiceURL(testDataForEachMethod.get("payByInvoicePageURL"));
    getBicTestBase().loginToOxygen(testDataForEachMethod.get("blockedUserEmailId"), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    getBicTestBase().validatePayByInvoiceTabPresence();
  }

  @Test(groups = {"bic-quoteorder-wrongCSN"}, description = "Validate wrong csn")
  public void validateWrongPayerCSNForExistingLOC() throws MetadataException {
    testDataForEachMethod.put(BICECEConstants.LOCALE, locale);
    getBicTestBase().getUrl(testDataForEachMethod.get("url"));
    getBicTestBase().setStorageData();
    getBicTestBase().loginToOxygen(testDataForEachMethod.get("purchaserEmailId"), PASSWORD);
    getBicTestBase().enterLOCEmailAndCSN(testDataForEachMethod);
    getBicTestBase().verifyIncorrectPayerDetailsAlertMessage();
    AssertUtils.assertFalse(getBicTestBase().isSubmitOrderEnabled(), "Submit Order option is disabled ");
  }

  @Test(groups = {"ttr-expired-certificate"}, description = "Validate expired TTR certificate")
  public void validateExpiredTTRCertificate() {
    HashMap<String, String> testResults = new HashMap<String, String>();

    Address address = getBillingAddress();
    testDataForEachMethod.put(BICConstants.emailid, testDataForEachMethod.get("existingUserEmail"));
    testDataForEachMethod.put(BICECEConstants.FIRSTNAME, testDataForEachMethod.get("existingUserFirstname"));
    testDataForEachMethod.put(BICECEConstants.LASTNAME, testDataForEachMethod.get("existingUserLastname"));

    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    try {
      getBicTestBase().refreshCartIfEmpty();
    } catch (MetadataException e) {
      throw new RuntimeException(e);
    }

    AssertUtils.assertTrue(getBicTestBase().isTTRButtonPresentInCart(), "Tax exception button should be present");
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

  @Test(groups = {"ttr-certificate-declined"}, description = "Validate expired TTR certificate")
  public void validateTTRCertificateDeclined() {
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
    updateTestingHub(testResults);
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    try {
      getBicTestBase().refreshCartIfEmpty();
    } catch (MetadataException e) {
      throw new RuntimeException(e);
    }

    AssertUtils.assertTrue(getBicTestBase().isTTRButtonPresentInCart(), "Tax exception button should be present");

    com.autodesk.testinghub.core.testbase.BICTestBase coreBicTestBase = new com.autodesk.testinghub.core.testbase.BICTestBase(
        getDriver(), getTestBase());
    coreBicTestBase.navigateToTTRPageForTaxExempt();

    getBicTestBase().exitECMS();
    getBicTestBase().validateTaxExemptionIneligibility();

    AssertUtils.assertFalse(getBicTestBase().isTTRButtonPresentInCart(), "Tax exception button not should be present");
  }

  private void loadPrevTransactionOut() {

    String transactiondetails = TestinghubUtil.getTransactionOutput();
    JsonPath js = new JsonPath(transactiondetails);
    String currentTestName = js.getString("name");
    String prvExecutionResponse = getTestingHubUtil().getAllTransactions(System.getProperty("prvexecutionid"));

    JSONArray jsonObject = new JSONObject(prvExecutionResponse).getJSONArray("items");

    jsonObject.forEach(item -> {
      JSONObject transactionJson = (JSONObject) item;
      if (transactionJson.get("name").equals(currentTestName)) {
        Util.printInfo("Found the matching Test cases for : " + currentTestName);
        HashMap<String, String> output = TestinghubUtil.getTransactionOutputObject(transactionJson.toString());
        HashMap<String, String> steps = TestinghubUtil.getTransactionSteps(transactionJson.toString());

        testDataForEachMethod.putAll(output);
        testDataForEachMethod.putAll(steps);
      }
    });
  }

}
