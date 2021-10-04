package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.listeners.TestingHubAPIClient;
import com.autodesk.testinghub.core.database.DBValidations;
import com.autodesk.testinghub.core.sap.SAPDriverFiori;
import com.autodesk.testinghub.core.testbase.*;
import com.autodesk.testinghub.core.utils.Util;
import java.util.HashMap;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterTest;

public class ECETestBase {

  protected static DBValidations dbValtb = null;
  protected SFDCTestBase sfdctb = null;
  protected TIBCOServiceTestBase tibcotb = null;
  protected SOAPTestBase soaptb = null;
  protected PortalTestBase portaltb = null;
  protected DynamoDBValidation dynamotb = null;
  protected ApigeeTestBase resttb = null;
  protected PelicanTestBase pelicantb = null;
  private WebDriver webdriver = null;
  private GlobalTestBase testbase = null;
  private AoeTestBase aoetb = null;
  private SiebelTestBase siebeltb = null;
  private BICTestBase bictb = null;
  private PWSTestBase pwstb = null;
  private RegonceTestBase regoncetb = null;

  public ECETestBase() {
    System.out.println("into the testing hub. core changes");
    testbase = new GlobalTestBase("ece", "ece", GlobalConstants.BROWSER);
    webdriver = testbase.getdriver();
    tibcotb = new TIBCOServiceTestBase();
    dbValtb = new DBValidations();
    sfdctb = new SFDCTestBase(webdriver);
    soaptb = new SOAPTestBase();
    portaltb = new PortalTestBase(testbase);
    resttb = new ApigeeTestBase();
    dynamotb = new DynamoDBValidation();
    pelicantb = new PelicanTestBase();
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

  @AfterTest(alwaysRun = true)
  public void afterTest() {
    try {
      Util.printInfo("Closing Webdriver after the end of the test");
      testbase.closeBrowser();
      tibcotb.tibcoConnectionClose();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
