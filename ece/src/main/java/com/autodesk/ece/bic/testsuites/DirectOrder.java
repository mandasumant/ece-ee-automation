package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.testbase.EceBICTestBase.Names;
import com.autodesk.eceapp.testbase.EceDotcomTestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
  }

  @Test(groups = {"create-direct-order"}, description = "Validation of Create Direct O2P Order")
  public void createDirectOrder() throws MetadataException {
    String productName =
        System.getProperty(BICECEConstants.PRODUCT_NAME) != null ? System.getProperty(
            BICECEConstants.PRODUCT_NAME) : testDataForEachMethod.get(BICECEConstants.PRODUCT_NAME);

    String emailID = getBicTestBase().generateUniqueEmailID();
    Names names = getBicTestBase().generateFirstAndLastNames();
    String password = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));

    Map<String, String> address = getBicTestBase().getBillingAddress(testDataForEachMethod);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String[] paymentCardDetails = getBicTestBase().getCardPaymentDetails(paymentMethod);

    EceDotcomTestBase dotcomTestBase = new EceDotcomTestBase(getDriver(), getTestBase(), locale);

    dotcomTestBase.navigateToDotComPage(productName);
    dotcomTestBase.selectMonthlySubscription();
    dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);
    getBicTestBase().setStorageData();
    getBicTestBase().createBICAccount(names, emailID, password, false); // Rename to createOxygenAccount
    getBicTestBase().selectPaymentProfile(testDataForEachMethod, paymentCardDetails, address);
    getBicTestBase().clickOnContinueBtn(paymentMethod);
    getBicTestBase().submitOrder(testDataForEachMethod);
    String orderNumber = getBicTestBase().getOrderNumber(testDataForEachMethod);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    results.put(BICECEConstants.orderNumber, orderNumber);
    results = pelicantb.getPurchaseOrderDetails(pelicantb.retryGetPurchaseOrder(testDataForEachMethod));

    getBicTestBase().validatePelicanTaxWithCheckoutTax(results.get(BICECEConstants.FINAL_TAX_AMOUNT),
        results.get(BICECEConstants.SUBTOTAL_WITH_TAX));
    results.putAll(subscriptionServiceV4Testbase.getSubscriptionById(results));
    portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),
        emailID,
        password, results.get(BICECEConstants.SUBSCRIPTION_ID));

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

    System.out.println("Hello World");
  }
}
