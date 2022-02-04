package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.MOETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
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


public class MOEOrderFlows extends ECETestBase {

  private static final String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  Map<?, ?> loadRestYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String restFileKey = "REST_" + GlobalConstants.ENV.toUpperCase();
    loadRestYaml = YamlUtil.loadYmlUsingTestManifest(restFileKey);
    String localeConfigFile = "LOCALE_CONFIG_" + GlobalConstants.ENV.toUpperCase();
    localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());
    LinkedHashMap<String, String> restdefaultvalues = (LinkedHashMap<String, String>) loadRestYaml
        .get("default");

    defaultvalues.putAll(testcasedata);
    defaultvalues.putAll(restdefaultvalues);
    testDataForEachMethod = defaultvalues;

    if (locale == null || locale.trim().isEmpty()) {
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale", locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    Util.printInfo(
        "Validating the store for the locale :" + locale + " Store: " + System.getProperty(
            BICECEConstants.STORE));

    boolean isValidStore = testDataForEachMethod.get(BICECEConstants.STORE_NAME)
        .equals(System.getProperty(BICECEConstants.STORE));

    if (!isValidStore) {
      AssertUtils.fail("The store  is not supported for the given country/locale : " + locale
          + ". Supported stores  are "
          + testDataForEachMethod.get(BICECEConstants.STORE_NAME));
    }

    LinkedHashMap<String, String> regionalData = (LinkedHashMap<String, String>) loadYaml
        .get(System.getProperty("store"));

    testDataForEachMethod.putAll(regionalData);

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);
  }

  @Test(groups = {
      "bic-nativeorder-moe"}, description = "Validation of Create BIC Order from MOE")
  public void validateBicNativeOrderMoe() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    MOETestBase moetb = new MOETestBase(this.getTestBase(), testDataForEachMethod);
    HashMap<String, String> results = moetb.createBicOrderMoe(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    moetb.validateTestResults(testResults, results);
  }

  @Test(groups = {
      "bic-basicflow-moe"}, description = "Basic flow for MOE with Opportunity ID")
  public void validateMoeOpportunityFlow() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    MOETestBase moetb = new MOETestBase(this.getTestBase(), testDataForEachMethod);
    HashMap<String, String> results = moetb.createBasicMoeOpptyOrder(testDataForEachMethod);
    results.putAll(testDataForEachMethod);

    moetb.validateTestResults(testResults, results);
  }

}
