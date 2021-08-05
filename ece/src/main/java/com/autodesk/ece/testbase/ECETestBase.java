package com.autodesk.ece.testbase;

import java.util.HashMap;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterTest;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.listeners.TestingHubAPIClient;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.testbase.ApigeeTestBase;
import com.autodesk.testinghub.core.testbase.BICTestBase;
import com.autodesk.testinghub.core.testbase.DynamoDBValidation;
import com.autodesk.testinghub.core.testbase.PelicanTestBase;
import com.autodesk.testinghub.core.testbase.PortalTestBase;
import com.autodesk.testinghub.core.utils.Util;

public class ECETestBase {

	private WebDriver webdriver = null;
	private GlobalTestBase testbase = null;
	private BICTestBase bictb = null;
	protected PortalTestBase portaltb = null;
	protected DynamoDBValidation dynamotb = null;
	protected ApigeeTestBase resttb = null;
	protected PelicanTestBase pelicantb = null;

	public ECETestBase() {
		System.out.println("into the testing hub. core changes");
		testbase = new GlobalTestBase("ece", "ece", GlobalConstants.BROWSER);
		webdriver = testbase.getdriver();
		resttb = new ApigeeTestBase();
		pelicantb = new PelicanTestBase();
		portaltb = new PortalTestBase(testbase);
		
		// Example to create the page object
		Page_ cartpage = testbase.createPage("PAGE_BICCART");

		cartpage.populateField("autodeskId", "sudarsan");
		
	}

	public GlobalTestBase getTestBase() {
		return testbase;
	}

	public WebDriver getDriver() {
		return testbase.getdriver();
	}


	public PortalTestBase getPortalTestBase() {
		return new PortalTestBase(testbase);
	}

	public BICTestBase getBicTestBase() {
		if (bictb == null) {
			bictb = new BICTestBase(webdriver, testbase);
		}
		return bictb;
	}



	public static void updateTestingHub(HashMap<String, String> results) {
		Set<String> keySet = results.keySet();
		JSONArray data = new JSONArray();
		for (String key : keySet) {
			JSONObject newValidation = new JSONObject();
			newValidation.put("name", key);
			newValidation.put("value", results.get(key));
			data.add(newValidation);
		}
		TestingHubAPIClient.updateTestData(data);
	}

	@AfterTest(alwaysRun = true)
	public void afterTest() {
		try {
			Util.printInfo("Closing Webdriver after the end of the test");
			testbase.closeBrowser();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}


}

	