package com.autodesk.ece.bic.testsuites;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EceBICTestBase.Names;
import com.autodesk.eceapp.testbase.ece.DatastoreClient;
import com.autodesk.eceapp.testbase.ece.DatastoreClient.OrderData;
import com.autodesk.eceapp.testbase.ece.DatastoreClient.OrderFilters;
import com.autodesk.eceapp.testbase.ece.ECETestBase;
import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PayInvoice extends ECETestBase {

  private static final String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  Map<?, ?> bankInformationByLocaleYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);

  private String PASSWORD;

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

    Names names = EceBICTestBase.generateFirstAndLastNames();
    testDataForEachMethod.put(BICECEConstants.FIRSTNAME, names.firstName);
    testDataForEachMethod.put(BICECEConstants.LASTNAME, names.lastName);

    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));

  }

  @Test(groups = {"pay-invoice-single-annual"}, description = "Validate Pay Invoice for single annual")
  public void validatePayInvoiceSingleAnnual() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Boolean isLoggedOut = true;
    Integer attempt = 0;

    //TODO: remove test data
    //Q2O SUS - Single Line Item - Annual - AU - LOC
    //scenario=SINGLE_ANNUAL
    //For testing: use data from Postgres to get Quote user credentials.
    //Postgres: environment='INT' and scenario='SINGLE_ANNUAL' and name='LOC_ORDER'
    results.put("orderNumber", "1000142901");
    testDataForEachMethod.put(BICConstants.emailid, "biz-thubstoreausztgxizdxvztt@letscheck.pw");

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

      while (isLoggedOut) {
        attempt++;
        if (attempt > 5) {
          Assert.fail("Retries Exhausted: Payment of Invoice failed because Session issues. Check Screenshots!");
        }

        portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICConstants.emailid),
            PASSWORD);

        isLoggedOut = false;

      }
    } else {
      Assert.fail("NON LOC Orders Do NOT have Pay Invoice Flow!!!");
    }

    updateTestingHub(testResults);
  }

  @Test(groups = {"pay-invoice"}, description = "Validate Pay Invoice")
  public void validatePayInvoice() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Boolean isLoggedOut = true;
    Integer attempt = 0;

    String scenario = "";
    if ((StringUtils.trimToNull(System.getProperty(BICECEConstants.SCENARIO)) != null)) {
      scenario = System.getProperty(BICECEConstants.SCENARIO).trim();
    } else {
      AssertUtils.fail("Scenario is required to run Pay Invoice flow.");
    }

    loadInvoiceDataFromP78(scenario, testDataForEachMethod);

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

  private void loadInvoiceDataFromP78(String scenario, LinkedHashMap<String, String> testDataForEachMethod) {
    DatastoreClient dsClient = new DatastoreClient();
    OrderFilters.OrderFiltersBuilder builder = OrderFilters.builder();
    String address = System.getProperty(BICECEConstants.ADDRESS);

    if (System.getProperty(BICECEConstants.APPLY_CM) != null && System.getProperty(BICECEConstants.APPLY_CM)
        .equalsIgnoreCase("LOC")) {
      builder
          .name("LOC_CREDITMEMO")
          .address(address)
          .locale(locale)
          .scenario(scenario);

    } else {
      builder.name(BICECEConstants.LOC_TEST_NAME)
          .paymentType("LOC");

      if (address != null) {
        builder.address(address);
      } else {
        builder.locale(locale);
      }

      if (scenario != null) {
        builder.scenario(scenario);
      }
    }

    OrderData order = dsClient.grabOrder(builder.build());
    try {
      testDataForEachMethod.put(BICConstants.emailid, order.getEmailId());
      testDataForEachMethod.put(BICECEConstants.ORDER_ID, order.getOrderNumber().toString());
      testDataForEachMethod.put("DS_ORDER_ID", order.getId().toString());
      if (order.getAddress() != null) {
        testDataForEachMethod.put(BICECEConstants.ADDRESS, order.getAddress());
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to fetch data from P78, for Pay Invoice");
    }

  }


}
