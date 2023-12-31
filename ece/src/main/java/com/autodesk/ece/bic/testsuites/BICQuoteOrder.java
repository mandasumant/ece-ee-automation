package com.autodesk.ece.bic.testsuites;

import com.autodesk.eceapp.constants.BICECEConstants;
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
import com.autodesk.eceapp.utilities.Address;
import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.eceapp.utilities.TaxExemptionMappings;
import com.autodesk.eceapp.utilities.TaxExemptionMappings.TaxOptions;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.EISTestBase;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import com.autodesk.testinghub.eseapp.constants.TestingHubConstants;
import io.restassured.path.json.JsonPath;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
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
  Map<?, ?> bankInformationByLocaleYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  String creditMemo = System.getProperty(BICECEConstants.CREDIT_MEMO);

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

    defaultvalues.putAll(Optional.ofNullable(testcasedata).orElse(new LinkedHashMap<>()));
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

    if (creditMemo == null || creditMemo.isEmpty()) {
      testDataForEachMethod.put(BICECEConstants.CREDIT_MEMO, "500");
    } else {
      testDataForEachMethod.put(BICECEConstants.CREDIT_MEMO, System.getProperty("creditMemo"));
    }

    // Load test data for wire transfer
    if (BICECEConstants.WIRE_TRANSFER_PAYMENT_METHOD.equals(System.getProperty(BICECEConstants.PAYMENT))
        || BICECEConstants.WIRE_TRANSFER_PAYMENT_METHOD.equals(System.getProperty(BICECEConstants.NEW_PAYMENT_TYPE))) {
      Util.printInfo(" Loading Bank Information for Payment Type: " + BICECEConstants.WIRE_TRANSFER_PAYMENT_METHOD
          + " From YAML File ");
      LinkedHashMap<String, Map<String, Map<String, String>>> bankInformationMap = (LinkedHashMap<String, Map<String, Map<String, String>>>) bankInformationByLocaleYaml.get(
          "BankInformationByLocale");
      testDataForEachMethod.putAll(bankInformationMap.get("customer").get(locale));
    }
  }

  @Test(groups = {"bic-quoteonly"}, description = "Validation of Create BIC Quote Order")
  public void validateBicQuote() {
    String locale = "en_US";
    Boolean shouldPushToDataStore =
        !Objects.isNull(System.getProperty(BICECEConstants.PROJECT78_PUSH_FLAG)) ? Boolean.valueOf(
            System.getProperty(BICECEConstants.PROJECT78_PUSH_FLAG)) : false;

    Boolean isMultiLineItem =
        !Objects.isNull(System.getProperty(BICECEConstants.IS_MULTILINE)) ? Boolean.valueOf(
            System.getProperty(BICECEConstants.IS_MULTILINE)) : false;

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

        if (isMultiLineItem) {
          builder.scenario(BICECEConstants.MULTI_LINE_ITEM);
        } else if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
          builder.scenario(BICECEConstants.DIFFERENT_PAYER);
        } else {
          builder.scenario(BICECEConstants.SAME_PAYER);
        }

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

  @Test(groups = {"bic-quoteorder"}, description = "Validation of Create BIC Quote Order")
  public void validateBicQuoteOrder() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Address address = getBillingAddress();

    Boolean shouldPullFromDataStore =
        !Objects.isNull(System.getProperty(BICECEConstants.PROJECT78_PULL_FLAG)) ? Boolean.valueOf(
            System.getProperty(BICECEConstants.PROJECT78_PULL_FLAG)) : false;

    if (shouldPullFromDataStore && loadQuoteDataFromP78()) {
      if (Strings.isNotNullAndNotEmpty(testDataForEachMethod.get(BICECEConstants.ADDRESS))) {
        address = new Address(testDataForEachMethod.get(BICECEConstants.ADDRESS));
      } else if (Strings.isNotNullAndNotEmpty(System.getProperty(BICECEConstants.ADDRESS))) {
        address = new Address(System.getProperty(BICECEConstants.ADDRESS));
      }
      address.company = testDataForEachMethod.get("company");
    } else {
      if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
        Names payerNames = EceBICTestBase.generateFirstAndLastNames();
        String payerEmail = EceBICTestBase.generateUniqueEmailID();
        Util.printInfo("Payer email: " + payerEmail);
        getBicTestBase().goToDotcomSignin(testDataForEachMethod);
        getBicTestBase().createBICAccount(payerNames, payerEmail, PASSWORD, true);
        //getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
        getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
        testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, payerEmail);
        testResults.put(BICECEConstants.PAYER_EMAIL, payerEmail);
      }

      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(
          new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
              testDataForEachMethod.get(BICECEConstants.LASTNAME)),
          testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

      //getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
      String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
          testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);

      //Wait for Quote to sync from CPQ/SFDC to S4.
      Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
      Util.sleep(600000);
      testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
      testResults.put(BICECEConstants.QUOTE_ID, quoteId);
      // Signing out after quote creation
      getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
    }

    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);

    // Re login during checkout
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    String oxygenId = getBicTestBase().driver.manage().getCookieNamed("identity-sso").getValue();
    testDataForEachMethod.put(BICConstants.oxid, oxygenId);

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

    String flexCode = null;
    // Setup test base for Tax Exemption Document submission
    if (Objects.equals(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO), BICECEConstants.TRUE)) {
      flexCode = submitECMSTaxExemption(testResults, address);
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

    if (GlobalConstants.getENV().equals(BICECEConstants.ENV_INT)) {
      String expectedExemptCode =
          Boolean.valueOf(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO)) ? flexCode : "null";
      AssertUtils.assertEquals("Pelican 'Tax Exempt' flag didnt match with Test Param",
          results.get(BICECEConstants.IS_TAX_EXEMPT), expectedExemptCode);
    }

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
      try {
        DatastoreClient dsClient = new DatastoreClient();
        NewQuoteOrder.NewQuoteOrderBuilder builder = NewQuoteOrder.builder()
            .name(BICECEConstants.LOC_TEST_NAME)
            .emailId(results.get(BICConstants.emailid))
            .orderNumber(new BigInteger(results.get(BICECEConstants.ORDER_ID)))
            .quoteId(testDataForEachMethod.get(BICECEConstants.QUOTE_ID))
            .paymentType(testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE))
            .address(getSerializedBillingAddress())
            .expiry(new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * 30).toInstant().toString());

        if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
          builder.scenario("Different Payer");
        }

        if (!Objects.isNull(System.getProperty(BICECEConstants.TENANT))) {
          builder.tenant(System.getProperty(BICECEConstants.TENANT));
        }

        OrderData orderData = dsClient.queueOrder(builder.build());
        testResults.put("Stored order data ID", orderData.getId().toString());
        updateTestingHub(testResults);
      } catch (Exception e) {
        Util.printWarning("Failed to push order data to data store");
      }
    }

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));
    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-loc-payinvoice"}, description = "Validation for Pay Invoice")
  public void validateLocPayInvoice() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Boolean isLoggedOut = true;
    Integer attempt = 0;

    loadInvoiceDataFromP78();

    results.putAll(testDataForEachMethod);
    if (results.containsKey(BICConstants.orderNumber) && results.get(BICConstants.orderNumber) != null) {
      results.put(BICECEConstants.ORDER_ID, results.get(BICConstants.orderNumber));
    }

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
      String paymentType = System.getProperty(BICECEConstants.NEW_PAYMENT_TYPE) != null ? System.getProperty(
          BICECEConstants.NEW_PAYMENT_TYPE)
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
      while (isLoggedOut) {
        attempt++;
        if (attempt > 5) {
          Assert.fail("Retries Exhausted: Payment of Invoice failed because Session issues. Check Screenshots!");
        }

        portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICECEConstants.PAYER_EMAIL),
            PASSWORD);

        double invoiceTotalBeforePayment = portaltb.selectInvoiceAndValidateCreditMemoWithoutPONumber(
            false, testDataForEachMethod.get(BICECEConstants.LOCALE));
        isLoggedOut = portaltb.payInvoice(testDataForEachMethod);

        if ((testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE)
            .equalsIgnoreCase(BICECEConstants.WIRE_TRANSFER_PAYMENT_METHOD))) {
          return;
        }

        if (!isLoggedOut) {
          portaltb.verifyInvoiceTotalAfterPayment(invoiceTotalBeforePayment);
        }

      }
      portaltb.verifyInvoiceStatus(results.get(BICECEConstants.ORDER_ID));

      if (System.getProperty(BICECEConstants.APPLY_CM) != null && System.getProperty(BICECEConstants.APPLY_CM)
          .equalsIgnoreCase("LOC")) {
        portaltb.verifyCreditMemoStatus("//*[@data-testid=\"credit-memo-list-empty-container\"]");
      }

    } else {
      Assert.fail("NON LOC Orders Do NOT have Pay Invoice Flow!!!");
    }

    if (getBicTestBase().shouldValidateSAP()) {
      portaltb.validateBICOrderTaxInvoice(results);
    }

    if (testDataForEachMethod.containsKey("DS_ORDER_ID")) {
      int orderId = Integer.parseInt(testDataForEachMethod.get("DS_ORDER_ID"));
      DatastoreClient dsClient = new DatastoreClient();
      dsClient.completeOrder(orderId);
    }

    updateTestingHub(testResults);
  }

  @Test(groups = {"bic-loc-create-credit-memo"}, description = "Create Credit Memo for Pay Invoice")
  public void validateLocCreateCreditMemo() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();

    String creditMemoAmount = testDataForEachMethod.get("creditMemo");
    Util.printInfo("The Credit Memo amount is: " + creditMemoAmount);

    loadInvoiceDataFromP78();
    results.putAll(testDataForEachMethod);

    if (!Strings.isNotNullAndNotEmpty(results.get(BICECEConstants.ORDER_ID))) {
      AssertUtils.fail("Please provide the initial order. Order number is null or empty");
    }

    //For purchaser as Payer test cases we need to update the Payer email address for login.
    if (!testDataForEachMethod.containsKey(BICECEConstants.PAYER_EMAIL)
        || testDataForEachMethod.get(BICECEConstants.PAYER_EMAIL) == null) {
      testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, testDataForEachMethod.get(BICConstants.emailid));
    }

    portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICECEConstants.PAYER_EMAIL),
        PASSWORD);

    portaltb.navigateToInvoiceCreditMemos();
    portaltb.waitForInvoicePageLoadToVisible();

    // Issue Credit Memo to order before Refund
    if (Strings.isNotNullAndNotEmpty(creditMemoAmount)) {
      testDataForEachMethod.put(TestingHubConstants.creditMemoAmount, creditMemoAmount);
    }

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

    if (testDataForEachMethod.containsKey("DS_ORDER_ID")) {
      int orderId = Integer.parseInt(testDataForEachMethod.get("DS_ORDER_ID"));
      DatastoreClient dsClient = new DatastoreClient();
      dsClient.completeOrder(orderId);
    }

    try {
      DatastoreClient dsClient = new DatastoreClient();
      OrderData orderData = dsClient.queueOrder(NewQuoteOrder.builder()
          .name("LOC_CREDITMEMO")
          .emailId(results.get(BICConstants.emailid))
          .orderNumber(new BigInteger(results.get(BICECEConstants.ORDER_ID)))
          .paymentType(testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE))
          .address(getSerializedBillingAddress()).build());
      testResults.put("Stored order data ID", orderData.getId().toString());
      updateTestingHub(testResults);
    } catch (Exception e) {
      Util.printWarning("Failed to push order data to data store");
    }

    updateTestingHub(testResults);
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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);

    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
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
      results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(
          results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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

    Boolean shouldPullFromDataStore =
        !Objects.isNull(System.getProperty(BICECEConstants.PROJECT78_PULL_FLAG)) ? Boolean.valueOf(
            System.getProperty(BICECEConstants.PROJECT78_PULL_FLAG)) : false;

    if (shouldPullFromDataStore && loadQuoteDataFromP78()) {
      if (Strings.isNotNullAndNotEmpty(testDataForEachMethod.get(BICECEConstants.ADDRESS))) {
        address = new Address(testDataForEachMethod.get(BICECEConstants.ADDRESS));
      } else if (Strings.isNotNullAndNotEmpty(System.getProperty(BICECEConstants.ADDRESS))) {
        address = new Address(System.getProperty(BICECEConstants.ADDRESS));
      }

      address.company = testDataForEachMethod.get("company");
    } else {
      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(
          new Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
              testDataForEachMethod.get(BICECEConstants.LASTNAME)),
          testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

      //getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
      String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
          testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);

      //Wait for Quote to sync from CPQ/SFDC to S4.
      Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
      Util.sleep(600000);
      testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
      testResults.put(BICECEConstants.QUOTE_ID, quoteId);
      // Signing out after quote creation
      getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
    }
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
      try {
        DatastoreClient dsClient = new DatastoreClient();
        OrderData orderData = dsClient.queueOrder(NewQuoteOrder.builder()
            .name(BICECEConstants.LOC_TEST_NAME)
            .emailId(results.get(BICConstants.emailid))
            .orderNumber(new BigInteger(results.get(BICECEConstants.ORDER_ID)))
            .quoteId(testDataForEachMethod.get(BICECEConstants.QUOTE_ID))
            .paymentType(testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE))
            .address(getSerializedBillingAddress())
            .scenario("Multi Line Item").build());
        testResults.put("Stored order data ID", orderData.getId().toString());
        updateTestingHub(testResults);
      } catch (Exception e) {
        Util.printWarning("Failed to push order data to data store");
      }
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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);

    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testDataForEachMethod.put(BICECEConstants.ORGANIZATION_NAME, firstAddress.company);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);
    testResults.putAll(testDataForEachMethod);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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
    Names secondUser = EceBICTestBase.generateFirstAndLastNames();
    String secondUserEmail = EceBICTestBase.generateUniqueEmailID();

    getBicTestBase().goToDotcomSignin(testDataForEachMethod);
    getBicTestBase().createBICAccount(secondUser, secondUserEmail, PASSWORD, true);
    testDataForEachMethod.put(BICECEConstants.emailid, secondUserEmail);
    testResults.put(BICECEConstants.PAYER_EMAIL, purchaser);

    quoteId = pwsTestBase.createAndFinalizeQuote(firstAddress, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.emailid, secondUserEmail);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testResults));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    // Re login during checkout
    getBicTestBase().loginToOxygen(testResults.get(BICECEConstants.emailid), PASSWORD);
    getBicTestBase().refreshCartIfEmpty();
    // Place second order with Same Payer
    results = getBicTestBase().placeFlexOrder(testResults);
    multiOrders = multiOrders + "," + results.get(BICConstants.orderNumber);
    // Appending both the Purchase Orders for Multi Pay Invoice tests
    testResults.put(BICConstants.orderNumber, multiOrders);

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
      try {
        DatastoreClient dsClient = new DatastoreClient();
        OrderData orderData = dsClient.queueOrder(NewQuoteOrder.builder()
            .name(BICECEConstants.LOC_TEST_NAME)
            .emailId(purchaser)
            .orderNumber(new BigInteger(results.get(BICECEConstants.orderNumber)))
            .quoteId(quoteId)
            .paymentType(testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE))
            .address(getSerializedBillingAddress())
            .scenario("Multi Invoice")
            .expiry(new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * 30).toInstant().toString()).build());
        testResults.put("Stored order data ID", orderData.getId().toString());
        updateTestingHub(testResults);
      } catch (Exception e) {
        Util.printWarning("Failed to push order data to data store" + e.getMessage());
      }
    }

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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    HashMap<String, String> justQuoteDetails = new HashMap<String, String>();
    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
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
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    updateTestingHub(testResults);

    if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      Names payerNames = EceBICTestBase.generateFirstAndLastNames();
      String payerEmail = EceBICTestBase.generateUniqueEmailID();
      Util.printInfo("Payer email: " + payerEmail);
      getBicTestBase().goToDotcomSignin(testDataForEachMethod);
      getBicTestBase().createBICAccount(payerNames, payerEmail, PASSWORD, true);
      //getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
      getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));
      testDataForEachMethod.put(BICECEConstants.PAYER_EMAIL, payerEmail);
      testResults.put(BICECEConstants.PAYER_EMAIL, payerEmail);
    }

    testDataForEachMethod.put(BICECEConstants.PAYMENT, "LOC");
    testDataForEachMethod.put("productType", "flex");
    Address address = getBillingAddress();

    //getBicTestBase().goToOxygenLanguageURL(testDataForEachMethod);
    String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
        testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod);

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.putAll(testDataForEachMethod);
    updateTestingHub(testResults);
    // Signing out after quote creation
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
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

    AssertUtils.assertEquals(Boolean.valueOf(results.get(BICECEConstants.IS_TAX_EXEMPT)),
        Boolean.valueOf(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO)),
        "Pelican 'Tax Exempt' flag didnt match with Test Param");

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

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

    // Setup test base for Tax Exemption Document submission
    String flexCode = null;
    if (Objects.equals(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO), BICECEConstants.TRUE)) {
      try {
        flexCode = submitECMSTaxExemption(testResults, address);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    HashMap<String, String> results = getBicTestBase().placeFlexOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);
    testResults.put(BICECEConstants.orderNumber, results.get(BICConstants.orderNumber));

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

    if (GlobalConstants.getENV().equals(BICECEConstants.ENV_INT)) {
      String expectedExemptCode =
          Boolean.valueOf(System.getProperty(BICECEConstants.SUBMIT_TAX_INFO)) ? flexCode : "null";
      AssertUtils.assertEquals("Pelican 'Tax Exempt' flag didnt match with Test Param",
          results.get(BICECEConstants.IS_TAX_EXEMPT), expectedExemptCode);
    }

    // Compare tax in Checkout and Pelican
    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));

    // Validate Quote Details with Pelican
    pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

    // Get find Subscription ById
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quote2Id);

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
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
      AssertUtils.assertTrue(Objects.equals(results.get(BICECEConstants.IS_TAX_EXEMPT), "null"),
          "Pelican 'Tax Exempt' flag should be null for returning tax exempt users");
    } else {
      AssertUtils.assertEquals(Boolean.valueOf(results.get(BICECEConstants.IS_TAX_EXEMPT)),
          false, "Pelican 'Tax Exempt' flag didnt match with Test Param");
    }

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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    try {
      getBicTestBase().refreshCartIfEmpty();
    } catch (MetadataException e) {
      throw new RuntimeException(e);
    }

    AssertUtils.assertTrue(getBicTestBase().isTTRButtonPresentInCart(), "Tax exception button should be present");
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

    //Wait for Quote to sync from CPQ/SFDC to S4.
    Util.printInfo("Keep calm, sleeping for 10min for Quote to sync to S4");
    Util.sleep(600000);

    testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
    testResults.put(BICECEConstants.QUOTE_ID, quoteId);
    updateTestingHub(testResults);
    getBicTestBase().getUrl(testDataForEachMethod.get("oxygenLogOut"));

    testDataForEachMethod.put("quote2OrderCartURL", getBicTestBase().getQuote2OrderCartURL(testDataForEachMethod));
    getBicTestBase().navigateToQuoteCheckout(testDataForEachMethod);
    getBicTestBase().loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
    try {
      getBicTestBase().refreshCartIfEmpty();
    } catch (MetadataException e) {
      throw new RuntimeException(e);
    }

    AssertUtils.assertTrue(getBicTestBase().isTTRButtonPresentInCart(), "Tax exception button should be present");

    com.autodesk.testinghub.eseapp.testbase.EseBICTestBase coreBicTestBase = new com.autodesk.testinghub.eseapp.testbase.EseBICTestBase(
        getDriver(), getTestBase());
    coreBicTestBase.navigateToTTRPageForTaxExempt();

    getBicTestBase().exitECMS();
    getBicTestBase().validateTaxExemptionIneligibility();

    AssertUtils.assertFalse(getBicTestBase().isTTRButtonPresentInCart(), "Tax exception button not should be present");
  }

  private void loadInvoiceDataFromP78() {
    DatastoreClient dsClient = new DatastoreClient();
    OrderFilters.OrderFiltersBuilder builder = OrderFilters.builder();
    String address = System.getProperty(BICECEConstants.ADDRESS);

    if (System.getProperty(BICECEConstants.APPLY_CM) != null && System.getProperty(BICECEConstants.APPLY_CM)
        .equalsIgnoreCase("LOC")) {
      builder
          .name("LOC_CREDITMEMO")
          .address(address)
          .locale(locale);

    } else {
      builder.name(BICECEConstants.LOC_TEST_NAME)
          .paymentType("LOC");

      if (address != null) {
        builder.address(address);
      } else {
        builder.locale(locale);
      }

      String scenario = System.getProperty("scenario");
      if (scenario != null) {
        builder.scenario(scenario);
      }
    }

    OrderData order = dsClient.grabOrder(builder.build());
    try {
      testDataForEachMethod.put(BICConstants.emailid, order.getEmailId());
      testDataForEachMethod.put(BICECEConstants.ORDER_ID, order.getOrderNumber().toString());
      testDataForEachMethod.put("Placing the Flex Order", "Passed");
      testDataForEachMethod.put("DS_ORDER_ID", order.getId().toString());
      if (order.getAddress() != null) {
        testDataForEachMethod.put(BICECEConstants.ADDRESS, order.getAddress());
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to fetch data from P78, for Pay Invoice");
    }
  }


  private boolean loadQuoteDataFromP78() {
    DatastoreClient dsClient = new DatastoreClient();
    OrderFilters.OrderFiltersBuilder builder = OrderFilters.builder();

    builder
        .name(BICECEConstants.QUOTE_TEST_NAME)
        .locale(locale);

    if (Objects.equals(System.getProperty(BICECEConstants.IS_MULTILINE), BICECEConstants.TRUE)) {
      builder.scenario(BICECEConstants.MULTI_LINE_ITEM);
    } else if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
      builder.scenario(BICECEConstants.DIFFERENT_PAYER);
    } else {
      builder.scenario(BICECEConstants.SAME_PAYER);
    }

    OrderData order = dsClient.grabOrder(builder.build());
    try {
      testDataForEachMethod.put(BICConstants.emailid, order.getEmailId());
      testDataForEachMethod.put(BICECEConstants.QUOTE_ID, order.getQuoteId());
      testDataForEachMethod.put("DS_ORDER_ID", order.getId().toString());

      QuoteDetails quoteDetails = pwsTestBase.getQuoteDetails(
          testDataForEachMethod.get("quoteAgentCsnAccount"), order.getQuoteId());

      testDataForEachMethod.put("tokens", String.valueOf(quoteDetails.getQuantity()));
      testDataForEachMethod.put("firstname", quoteDetails.getPurchaserFirstName());
      testDataForEachMethod.put("lastname", quoteDetails.getPurchaserLastName());
      testDataForEachMethod.put("company", quoteDetails.getEndCustomerName());
      testDataForEachMethod.put("address", order.getAddress());

    } catch (Exception e) {
      Util.printInfo("Failed to fetch data from P78, for Quote Orders. Creating via PWS");
      return false;
    }
    return true;
  }


  private String submitECMSTaxExemption(HashMap<String, String> testResults, Address address) throws IOException {
    String flexCode = null;
    com.autodesk.testinghub.eseapp.testbase.EseBICTestBase coreBicTestBase = new com.autodesk.testinghub.eseapp.testbase.EseBICTestBase(
        getDriver(), getTestBase());

    HashMap<String, String> dataForTTR = new HashMap<String, String>(testDataForEachMethod) {
      {
        put(BICConstants.exemptFromSalesTax, "Yes");
        put(BICConstants.reasonForExempt, "Reseller");
        put(BICConstants.buyerAccountType, "Reseller");
        put(TestingHubConstants.state, address.provinceName);
        put(TestingHubConstants.store, testDataForEachMethod.get("storeName"));
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

        String partialExemptionType = System.getProperty("partialExemptionType");
        partialExemptionType = partialExemptionType == null ? "" : partialExemptionType;

        switch (partialExemptionType) {
          case "GST":
            dataForTTR.put(BICConstants.buyerAccountType, "Provincial Government");
            break;
          case "PST":
            dataForTTR.put(BICConstants.canadianTaxType, "Provincial Sales Tax (PST)");
            dataForTTR.put(BICConstants.certToSelect, "Canada Provincial Sales Tax Certificate");
          default:
            dataForTTR.put(BICConstants.buyerAccountType, "Government of Canada");
        }

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

    TaxExemptionMappings taxMappings = ResourceFileLoader.getTaxExemptionMappings();

    TaxOptions taxOptions = null;
    switch (address.country) {
      case "Canada":
        String partialExemptionType =
            Objects.nonNull(System.getProperty("partialExemptionType")) ? System.getProperty("partialExemptionType")
                : "full";
        taxOptions = taxMappings.CA.get(address.province)
            .get(partialExemptionType);
        break;
      case "United States":
        taxOptions = taxMappings.US.get("full");
        break;
    }

    try {
      flexCode = getDriver().getCurrentUrl().split("flexCode=")[1];
      testDataForEachMethod.put(BICECEConstants.TAX_FLEX_CODE, flexCode);
      testResults.put(BICECEConstants.TAX_FLEX_CODE, flexCode);
      updateTestingHub(testResults);
    } catch (Exception ex) {
      AssertUtils.fail("Failed to read flex code from checkout URL");
    }

    AssertUtils.assertEquals("FlexCode URL parameter should match expected code", flexCode,
        String.valueOf(taxOptions.code));
    testDataForEachMethod.put("taxRate", taxOptions.rate.toString());
    return flexCode;
  }

}
