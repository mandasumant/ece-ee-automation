package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BICRefundOrder extends ECETestBase {

  Map<?, ?> loadYaml = null;
  Map<?, ?> loadRestYaml = null;
  long startTime;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  private static String defaultLocale = "en_US";
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String,String>> localeDataMap = null;
  String locale = null;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String restFileKey = "REST_" + GlobalConstants.ENV.toUpperCase();
    loadRestYaml = YamlUtil.loadYmlUsingTestManifest(restFileKey);
    String localeConfigFile= "LOCALE_CONFIG_" + GlobalConstants.ENV.toUpperCase();
    localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testCaseData = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());
    LinkedHashMap<String, String> restDefaultValues = (LinkedHashMap<String, String>) loadRestYaml
        .get("default");
    LinkedHashMap<String, String> regionalData = (LinkedHashMap<String, String>) loadYaml
        .get(System.getProperty("store"));
    defaultValues.putAll(regionalData);
    defaultValues.putAll(testCaseData);
    defaultValues.putAll(restDefaultValues);
    testDataForEachMethod = defaultValues;
    locale = System.getProperty(BICECEConstants.LOCALE);
    if(locale == null || locale.trim().isEmpty()){
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale",locale);

    localeDataMap = (LinkedHashMap<String, Map<String,String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    Util.printInfo("Validating the store for the locale :"+locale +" Store: "+System.getProperty(BICECEConstants.STORE));

    boolean isValidStore = false;
    if(testDataForEachMethod.get(BICECEConstants.STORE_NAME).equals(System.getProperty(BICECEConstants.STORE))){
      isValidStore = true;
    }

    if(!isValidStore){
      AssertUtils.fail("The store  is not supported for the given country/locale : "+ locale + ". Supported stores  are "
          + testDataForEachMethod.get(BICECEConstants.STORE_NAME));
    }

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);
  }

  @Test(groups = {"bic-RefundOrder"}, description = "BIC refund order")
  public void validateBicRefundOrder() {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();

    HashMap<String, String> results = getBicTestBase()
            .createGUACBICOrderDotCom(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
    testResults.put(TestingHubConstants.orderNumber, results.get(TestingHubConstants.orderNumber));
    updateTestingHub(testResults);

    // Getting a PurchaseOrder details from pelican
    results.putAll(pelicantb.getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

    // Get find Subscription ById
    results.putAll(pelicantb.getSubscriptionById(results));

    // Trigger Invoice join
    pelicantb.postInvoicePelicanAPI(results);

    Util.sleep(60000);
    // Refund PurchaseOrder details from pelican
    testResults.putAll(pelicantb.createRefundOrder(results));

    try {
      testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
      testResults
          .put(TestingHubConstants.orderNumber, results.get(TestingHubConstants.orderNumber));
      testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }

    updateTestingHub(testResults);
  }
}
