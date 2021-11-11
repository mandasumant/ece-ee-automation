package com.autodesk.ece.bic.testsuites;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.testbase.SAPTestBase;
import com.autodesk.testinghub.core.utils.YamlUtil;

public class IndirectOrderCreation extends ECETestBase {

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
	String soldToParty = System.getProperty(TestingHubConstants.soldToParty);
  	String shipToParty = System.getProperty(TestingHubConstants.shipToParty);
  	String supportingReseller = System.getProperty(TestingHubConstants.supportingReseller);
  	
//	String userType = System.getProperty("usertype").toLowerCase();
	
//	testDataForEachMethod.put(TestingHubConstants.soldToParty, soldToParty);
//	testDataForEachMethod.put(TestingHubConstants.shipToParty, shipToParty);
//	testDataForEachMethod.put(TestingHubConstants.supportingReseller, supportingReseller);
//	testDataForEachMethod.put(TestingHubConstants.usertype, userType);

  }

  @Test(groups = {"sap-bicindirect"}, description = "Validate BIC Indirect functionality")
  public void validateBICIndirectOrder() {

	    String email = "thubsrd1629665792@letscheck.pw"; //call the getuser() method once its moved to core to fetch new/existing contacts
	  	String endusercsn = "5151567993";
	  	testDataForEachMethod.put(TestingHubConstants.emailid, email);
	  	testDataForEachMethod.put(TestingHubConstants.enduserCSN, endusercsn);
	    
	    
	    String sku = System.getProperty(TestingHubConstants.sku);
		String[] skuList = sku.split(":");
		String skus = skuList[0];
		String quantity = skuList[1];		
		String skuDetails = "sku=" + skus + ",qty=" + quantity + ",hgvlt=";
		
		testDataForEachMethod.put("skuDetails", skuDetails);
	
		if ( System.getProperty(TestingHubConstants.contractStartDate) != null)
			testDataForEachMethod.put(TestingHubConstants.contractStartDate, System.getProperty(TestingHubConstants.contractStartDate));
		else
			testDataForEachMethod.put(TestingHubConstants.contractStartDate, "");
		SAPTestBase saptb = new SAPTestBase();
		HashMap<String, String>  results = saptb.createMetaInitialOrderDynamo(testDataForEachMethod);
		updateTestingHub(results);	

  }

}