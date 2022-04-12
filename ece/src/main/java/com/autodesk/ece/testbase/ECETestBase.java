package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.listeners.TestingHubAPIClient;
import com.autodesk.testinghub.core.database.DBValidations;
import com.autodesk.testinghub.core.testbase.AoeTestBase;
import com.autodesk.testinghub.core.testbase.DynamoDBValidation;
import com.autodesk.testinghub.core.testbase.PWSTestBase;
import com.autodesk.testinghub.core.testbase.RegonceTestBase;
import com.autodesk.testinghub.core.testbase.SAPTestBase;
import com.autodesk.testinghub.core.testbase.SFDCTestBase;
import com.autodesk.testinghub.core.testbase.SOAPTestBase;
import com.autodesk.testinghub.core.testbase.SiebelTestBase;
import com.autodesk.testinghub.core.testbase.TIBCOServiceTestBase;
import com.autodesk.testinghub.core.testbase.TestinghubUtil;
import com.autodesk.testinghub.core.utils.Util;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterTest;

public class ECETestBase {

  protected static DBValidations dbValtb = null;
  protected static TestinghubUtil thutil = null;
  protected SFDCTestBase sfdctb = null;
  protected SOAPTestBase soaptb = null;
  protected PortalTestBase portaltb = null;
  protected DynamoDBValidation dynamotb = null;
  protected ApigeeTestBase resttb = null;
  protected PelicanTestBase pelicantb = null;
  LinkedHashMap<String, String> localeConfig;
  private WebDriver webdriver = null;
  private GlobalTestBase testbase = null;
  private BICTestBase bictb = null;
  protected TIBCOServiceTestBase tibcotb = null;
  protected static SAPTestBase saptb = null;

  public ECETestBase() {
    System.out.println("into the testing hub. core changes");
    testbase = new GlobalTestBase("ece", "ece", GlobalConstants.BROWSER);
    webdriver = testbase.getdriver();
    dbValtb = new DBValidations();
    sfdctb = new SFDCTestBase(webdriver);
    soaptb = new SOAPTestBase();
    portaltb = new PortalTestBase(testbase);
    resttb = new ApigeeTestBase();
    dynamotb = new DynamoDBValidation();
    pelicantb = new PelicanTestBase();
    thutil = new TestinghubUtil(testbase);
    tibcotb = new TIBCOServiceTestBase();
    saptb = new SAPTestBase();
  }

  public static void updateTestingHub(HashMap<String, String> results) {
    TestinghubUtil.updateTestingHub(results);
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

  public String getSAPOrderNumberUsingPO(String poNumber){
    String orderNumber = "";
    if(saptb.sapConnector.isBAPIEnabled()) {
      saptb.sapConnector.connectSAPBAPI();
      orderNumber = saptb.sapConnector.getOrderNumberUsingPO(poNumber);
    } else {
      orderNumber = saptb.getOrderFromSAP(poNumber);
    }
    return orderNumber;
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
