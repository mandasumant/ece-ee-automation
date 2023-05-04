package com.autodesk.ece.bic.testsuites;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import com.autodesk.testinghub.eseapp.constants.TestingHubConstants;
import com.autodesk.testinghub.eseapp.testbase.EseSAPTestBase;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IndirectOrderCreation extends ECETestBase {

  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  private String PASSWORD;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs(getDriver());
    String testFileKey = "SAP_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml.get(
        "default");
    LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml.get(
        name.getName());

    defaultvalues.putAll(testcasedata);
    testDataForEachMethod = defaultvalues;
    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));
  }

  @Test(groups = {
      "sap-bicindirect"}, description = "Validate BIC Indirect SAP Order in Portal functionality")
  public void validateBICIndirectSAPOrder() {
    ECETestBase tb = new ECETestBase();
    EseSAPTestBase saptb = new EseSAPTestBase();

    String sku = System.getProperty(TestingHubConstants.sku);
    String[] skuList = sku.split(":");
    String skus = skuList[0];
    String quantity = skuList[1];
    String skuDetails = "sku=" + skus + ",qty=" + quantity + ",hgvlt=";
    testDataForEachMethod.put("skuDetails", skuDetails);

    saptb.sapConnector.connectSAPBAPI();
    if (System.getProperty(TestingHubConstants.contractStartDate) != null) {
      testDataForEachMethod.put(TestingHubConstants.contractStartDate,
          System.getProperty(TestingHubConstants.contractStartDate));
    } else {
      testDataForEachMethod.put(TestingHubConstants.contractStartDate, "");
    }

    HashMap<String, String> userDetails = thutil.getUserDetailsNew(testDataForEachMethod);
    testDataForEachMethod.putAll(userDetails);

    HashMap<String, String> results = saptb.createMetaInitialOrder(testDataForEachMethod);
    updateTestingHub(results);

    // In direct Order validation in Portal
    tb.getPortalTestBase()
        .validateBICOrderProductInCEP(testDataForEachMethod.get(BICConstants.cepURL),
            results.get(BICConstants.emailid), PASSWORD, results.get("agreementNumber"));
    updateTestingHub(results);
  }

}