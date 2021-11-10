package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.PayportTestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IndirectOrderCreation extends ECETestBase {

  Map<?, ?> loadYaml = null;
  Map<?, ?> loadRestYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "SAP_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    
  }

  @BeforeMethod(alwaysRun = true)
  public void beforeTestMethod(Method name) {
	LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml.get("default");
	LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml.get(name.getName());

	defaultvalues.putAll(testcasedata);
	testDataForEachMethod = defaultvalues;

  }

  @Test(groups = {"bic-changePayment-US"}, description = "Validate BIC Indirect functionality")
  public void validateBICIndirectOrder() {

	  	String soldToParty = System.getProperty(TestingHubConstants.soldToParty);
	  	String shipToParty = System.getProperty(TestingHubConstants.shipToParty);
	  	String ponumber = System.getProperty(TestingHubConstants.ponumber);
	  	String email= System.getProperty(TestingHubConstants.emailid);
	  	String supportingReseller = System.getProperty(TestingHubConstants.supportingReseller);
	  	String enduserForFTrade = System.getProperty(TestingHubConstants.enduserForFTrade);
		String salesOrg = System.getProperty("salesorg");
		String channel = System.getProperty(TestingHubConstants.channel);
		String userType = System.getProperty("usertype").toLowerCase();
		String startDate = System.getProperty(TestingHubConstants.contractStartDate);
		
		testDataForEachMethod.put(TestingHubConstants.contractStartDate, startDate);
		testDataForEachMethod.put(TestingHubConstants.soldToParty, soldToParty);
		testDataForEachMethod.put(TestingHubConstants.shipToParty, shipToParty);
		testDataForEachMethod.put(TestingHubConstants.supportingReseller, supportingReseller);
		testDataForEachMethod.put(TestingHubConstants.salesOrg, salesOrg);
		testDataForEachMethod.put(TestingHubConstants.channel, channel);
		testDataForEachMethod.put(TestingHubConstants.contractEndDate, "");
		testDataForEachMethod.put(TestingHubConstants.usertype, userType);
		
		
		if(Strings.isNullOrEmpty(email)) {
			userDetails = getUserDetails(testDataForEachMethod);
			testDataForEachMethod.putAll(userDetails);
			Util.printInfo("Enduser CSN Fetched from dynamo : " + testDataForEachMethod.get(TestingHubConstants.enduserCSN));
			Util.printInfo("Contact Fetched from dynamo : " + testDataForEachMethod.get(TestingHubConstants.emailid));
			email = testDataForEachMethod.get(TestingHubConstants.emailid); 
		}
		else {  //email is not empty/ provided by user
			boolean matchAccountContact=true;
			testDataForEachMethod = associateAccountContact(testDataForEachMethod, email, shipToAccount, matchAccountContact);
			email = testDataForEachMethod.get(TestingHubConstants.emailid); 
		} 
		
		

  }

}