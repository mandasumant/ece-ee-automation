package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.constants.BICConstants;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
  private static final String PASSWORD = "Password1";

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

  }

  @Test(groups = {"sap-bicindirect"}, description = "Validate BIC Indirect functionality")
  public void validateBICIndirectSAPOrder() {
  		ECETestBase tb = new ECETestBase();
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
		SAPTestBase saptb = new SAPTestBase();
		HashMap<String, String>  results = saptb.createMetaInitialOrderDynamo(testDataForEachMethod);
		updateTestingHub(results);
		
//	  results.put("orderNumber","7064390814");
//	  results.put("serialNumber", "581-05531602");
//	  results.put("price", "382.20");
//	  results.put("contractEndDate", "11/11/2022");
//	  results.put("enduserCSN", "5151567993");
//	  results.put("emailid", "thubsrd1629623425@letscheck.pw");
//	  results.put("tax", "14.75");
//	  results.put("agreementNumber", "110005478778");
//	  results.put("poNumber", "AutoTh_jlh9gvxczq");

		/* Below code checks Tibco, SAP, GBX and SFDC for validation. Lets keep this for future
		tibcotb.validateSyncOrderInTibcoForFirstOrder(results.get(TestingHubConstants.orderNumber));
		tibcotb.validatePublishOrderSAPForFirstOrder(results.get(TestingHubConstants.orderNumber));
		tibcotb.validatePublishOrderGreenboxForFirstOrder(results.get(TestingHubConstants.orderNumber));
		tibcotb.validateSyncEntitlementFirstOrder(results.get(TestingHubConstants.agreementNumber));
		dbValtb.validatedAgreementExistSfdc(results.get(TestingHubConstants.agreementNumber));
		*/

		// Initial order validation in Portal
		tb.getPortalTestBase().validateBICOrderProductInCEP(testDataForEachMethod.get(BICConstants.cepURL),
			  results.get(BICConstants.emailid), PASSWORD, results.get("agreementNumber"));
		updateTestingHub(results);
  }

}