package com.autodesk.ece.bic.testsuites;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.testbase.ece.DatastoreClient;
import com.autodesk.eceapp.testbase.ece.ECETestBase;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BICRefundOrder extends ECETestBase {

  private static final String defaultTaxOption = "undefined";
  private static final String defaultPriceId = "24038";
  Map<?, ?> loadYaml = null;
  long startTime;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = null;
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  String priceId = System.getProperty(BICECEConstants.PRICE_ID);
  private String PASSWORD;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs(getDriver());
    loadYaml = ResourceFileLoader.getBicOrderYaml();
    localeConfigYaml = ResourceFileLoader.getLocaleConfigYaml();
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testCaseData = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());
    defaultValues.putAll(testCaseData);
    testDataForEachMethod = defaultValues;
    locale = System.getProperty(BICECEConstants.LOCALE);
    if (locale == null || locale.trim().isEmpty()) {
      locale = "en_US";
    }
    testDataForEachMethod.put("locale", locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    Util.printInfo(
        "Validating the store for the locale :" + locale + " Store: " + System.getProperty(BICECEConstants.STORE));

    boolean isValidStore = testDataForEachMethod.get(BICECEConstants.STORE_NAME)
        .equals(System.getProperty(BICECEConstants.STORE));

    if (!isValidStore) {
      AssertUtils
          .fail("The store  is not supported for the given country/locale : " + locale + ". Supported stores  are "
              + testDataForEachMethod.get(BICECEConstants.STORE_NAME));
    }
    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);

    if (taxOptionEnabled == null || taxOptionEnabled.trim().isEmpty()) {
      taxOptionEnabled = defaultTaxOption;
    }
    testDataForEachMethod.put("taxOptionEnabled", taxOptionEnabled);

    if (priceId == null || priceId.trim().isEmpty()) {
      priceId = defaultPriceId;
    }
    testDataForEachMethod.put("priceId", priceId);

    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));
  }

  @Test(groups = {"bic-RefundOrder"}, description = "BIC refund order")
  public void validateBicRefundOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();

    HashMap<String, String> results = getBicTestBase()
        .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
    testResults.put(TestingHubConstants.orderNumber, results.get(TestingHubConstants.orderNumber));
    updateTestingHub(testResults);

    Util.sleep(120000);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(results)));
    results.put(BICECEConstants.orderNumber, results.get(BICECEConstants.ORDER_ID));
    // Get find Subscription ById
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    // Validate Portal
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        results.get(BICConstants.emailid),
        PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
    if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
      portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
    }

    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrder(results);

    JsonPath jp = pelicantb.getRefundedPurchaseOrderWithPolling(results);
    results.put("refund_orderState", jp.get("content[0].orderState").toString());
    results.put("refund_fulfillmentStatus", jp.get("content[0].fulfillmentStatus"));
    results.put("refund_paymentMethodType", jp.get("content[0].billingInfo.paymentMethodType"));
    results.put("refund_finalExportControlStatus", jp.get("content[0].finalExportControlStatus"));
    results.put("refund_uiInitiatedGetOrders",
        jp.get("content[0].uiInitiatedGetOrders") != null ? Boolean.toString(jp.get("content[0].uiInitiatedGetOrders"))
            : null);
    results.put("refund_lineItemState", jp.get("content[0].lineItems[0].lineItemState"));

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
      updateTestingHub(testResults);
    }
  }

  @Test(groups = {"refundOrder-PSP"}, description = "Refund order PSP")
  public void validateRefundOrderPSP() {
    HashMap<String, String> results = new HashMap<>();
    DatastoreClient dsClient = new DatastoreClient();
    boolean flag = false;
    int count = 0;
    while (count < 3) {
      DatastoreClient.OrderData order = dsClient.grabOrder(DatastoreClient.OrderFilters.builder()
              .paymentType(System.getProperty("payment"))
              .name("REFUND_PSP")
              .locale(locale).build());

      results.putAll(testDataForEachMethod);
      results.put(BICECEConstants.orderNumber, order.getOrderNumber().toString());

      // Getting a PurchaseOrder details from pelican
      results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPurchaseOrder(results)));
      flag = results.get(BICECEConstants.REFUND_ORDER_STATUS).equals("CHARGED");
      count++;
      if (flag) {
        break;
      }
    }
    if (!flag) {
      AssertUtils.assertTrue(flag, " Pelican Order Status is " + results.get(BICECEConstants.REFUND_ORDER_STATUS) + " for Order Number:" + testDataForEachMethod.get(BICECEConstants.orderNumber));
    }

    // Refund PurchaseOrder details from pelican
    pelicantb.createRefundOrder(results);
    Util.sleep(660000);

    // Getting a PurchaseOrder details from pelican
    JsonPath jp = pelicantb.getRefundedPurchaseOrderWithPolling(results);
    results.put("refund_orderState", jp.get("content[0].orderState").toString());
    results.put("refund_fulfillmentStatus", jp.get("content[0].fulfillmentStatus"));
    results.put("refund_paymentMethodType", jp.get("content[0].billingInfo.paymentMethodType"));
    results.put("refund_uiInitiatedGetOrders",
        jp.get("content[0].uiInitiatedGetOrders") != null ? Boolean.toString(jp.get("content[0].uiInitiatedGetOrders"))
            : null);
    results.put("refund_finalExportControlStatus", jp.get("content[0].finalExportControlStatus"));
    results.put("refund_lineItemState", jp.get("content[0].lineItems[0].lineItemState"));

    // Verify that Order status is Refunded
    AssertUtils.assertEquals("Order status is NOT REFUNDED", results.get("refund_orderState"), "REFUNDED");

    // Get Subscription Status ById
    results.putAll(
        subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

    // Verify that Subscription status is Expired
    AssertUtils
            .assertEquals("Subscription Status is not Expired.", results.get(BICECEConstants.RESPONSE_STATUS),
                    "EXPIRED");

    updateTestingHub(results);

  }
}
