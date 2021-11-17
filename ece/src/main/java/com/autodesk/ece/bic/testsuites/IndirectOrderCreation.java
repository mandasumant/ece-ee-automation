package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.testbase.SAPTestBase;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IndirectOrderCreation extends ECETestBase {

  private static final String PASSWORD = "Password1";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "SAP_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
	LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml.get("default");
	LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml.get(name.getName());

	defaultvalues.putAll(testcasedata);
	testDataForEachMethod = defaultvalues;
  }

  @Test(groups = {"sap-bicindirect"}, description = "Validate BIC Indirect SAP Order in Portal functionality")
  public void validateBICIndirectSAPOrder() {
	ECETestBase tb = new ECETestBase();
	SAPTestBase saptb = new SAPTestBase();
	String email = "thubsrd1629665792@letscheck.pw"; //call the getuser() method once its moved to core to fetch new/existing contacts
	String endusercsn = "5151567993";
	String sku = System.getProperty(TestingHubConstants.sku);
	String[] skuList = sku.split(":");
	String skus = skuList[0];
	String quantity = skuList[1];
	String skuDetails = "sku=" + skus + ",qty=" + quantity + ",hgvlt=";
	testDataForEachMethod.put(TestingHubConstants.emailid, email);
	testDataForEachMethod.put(TestingHubConstants.enduserCSN, endusercsn);
	testDataForEachMethod.put("skuDetails", skuDetails);

	if ( System.getProperty(TestingHubConstants.contractStartDate) != null) {
	  testDataForEachMethod.put(TestingHubConstants.contractStartDate, System.getProperty(TestingHubConstants.contractStartDate));
	} else {
	  testDataForEachMethod.put(TestingHubConstants.contractStartDate, "");
	}

	HashMap<String, String>  results = saptb.createMetaInitialOrderDynamo(testDataForEachMethod);
	updateTestingHub(results);

		// In direct Order validation in Portal
	tb.getPortalTestBase().validateBICOrderProductInCEP(testDataForEachMethod.get(BICConstants.cepURL),
		results.get(BICConstants.emailid), PASSWORD, results.get("agreementNumber"));
	updateTestingHub(results);
  }

}