package com.autodesk.eceapp.testbase.ece;

import com.autodesk.eceapp.testbase.ece.ApigeeTestBase;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EcePortalTestBase;
import com.autodesk.eceapp.testbase.ece.AutomationTibcoTestBase;
import com.autodesk.eceapp.testbase.ece.PelicanTestBase;
import com.autodesk.eceapp.testbase.ece.SubscriptionServiceV4TestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.constants.TestingHubConstants;
import com.autodesk.testinghub.eseapp.database.DBValidations;
import com.autodesk.testinghub.eseapp.sap.SAPDriverFiori;
import com.autodesk.testinghub.eseapp.testbase.DynamoDBValidation;
import com.autodesk.testinghub.eseapp.testbase.EseSAPTestBase;
import com.autodesk.testinghub.eseapp.testbase.EseSFDCTestBase;
import com.autodesk.testinghub.eseapp.testbase.EseSOAPTestBase;
import com.autodesk.testinghub.eseapp.testbase.EseTestBase;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterTest;
import org.testng.util.Strings;

public class ECETestBase {

  protected static DBValidations dbValtb = null;
  protected static EseTestBase thutil = null;
  protected static EseSAPTestBase saptb = null;
  protected EseSFDCTestBase sfdctb = null;
  protected EseSOAPTestBase soaptb = null;
  protected EcePortalTestBase portaltb = null;
  protected DynamoDBValidation dynamotb = null;
  protected ApigeeTestBase resttb = null;
  protected PelicanTestBase pelicantb = null;
  protected SubscriptionServiceV4TestBase subscriptionServiceV4Testbase = null;
  protected AutomationTibcoTestBase tibcotb = null;
  protected SAPDriverFiori sapfioritb = null;

  LinkedHashMap<String, String> localeConfig;
  private WebDriver webdriver = null;
  private GlobalTestBase testbase = null;
  private EceBICTestBase bictb = null;

  public ECETestBase() {
    System.out.println("into the testing hub. core changes");
    testbase = new GlobalTestBase("ece", "ece", GlobalConstants.BROWSER);
    webdriver = testbase.getdriver();
    dbValtb = new DBValidations();
    sfdctb = new EseSFDCTestBase(webdriver);
    soaptb = new EseSOAPTestBase();
    portaltb = new EcePortalTestBase(testbase);
    resttb = new ApigeeTestBase();
    dynamotb = new DynamoDBValidation();
    pelicantb = new PelicanTestBase();
    subscriptionServiceV4Testbase = new SubscriptionServiceV4TestBase();
    thutil = new EseTestBase(testbase);

//    tibcotb = new AutomationTibcoTestBase();
    saptb = new EseSAPTestBase();

    sapfioritb = new SAPDriverFiori(GlobalConstants.getTESTDATADIR(), webdriver);
  }

  public static void updateTestingHub(HashMap<String, String> results) {
    EseTestBase.updateTestingHub(results);
  }

  public GlobalTestBase getTestBase() {
    return testbase;
  }

  public WebDriver getDriver() {
    return testbase.getdriver();
  }

  public EcePortalTestBase getPortalTestBase() {
    return new EcePortalTestBase(testbase);
  }

  public EceBICTestBase getBicTestBase() {
    if (bictb == null) {
      bictb = new EceBICTestBase(webdriver, testbase);
    }
    return bictb;
  }

  public EseTestBase getTestingHubUtil() {
    return thutil;
  }

  public String getSAPOrderNumberUsingPO(String poNumber) {
    String orderNumber = "";
    String OS = System.getProperty("os.name").toLowerCase();

    try {
      if (saptb.sapConnector.isBAPIEnabled()) {
        saptb.sapConnector.connectSAPBAPI();
        orderNumber = saptb.sapConnector.getOrderNumberUsingPO(poNumber);
      } else {
        orderNumber = saptb.getOrderFromSAP(poNumber);
      }
    } catch (NoClassDefFoundError e) {
      Util.printWarning("SAP Initialization wont work with " + OS + ", so skipping SAP validation due to ,"
          + e.getMessage());
    }

    return orderNumber;
  }

  public String getSOMOrderNumber(String poNumber) {
    String somOrderNumber = "";
    long time = new Date().getTime();
    int waitForSOMOrderCreation = 1000 * 60 * 30;
    saptb.sapConnector.connectSAPBAPIS4();
    HashMap<String, String> somDetails = new HashMap<>();
    do {
      somDetails = saptb.sapConnector.getSOMOrderDetailsFromTableUsingPoNumber(poNumber);
      if (Strings.isNullOrEmpty(somDetails.get(TestingHubConstants.somOrderNumber))) {
        Util.printInfo("SOM order number not found for give PO number, will await for 10 seconds and try again");
        Util.sleep(10000);
      } else {
        Util.printInfo("somOrderNumber :: " + somDetails.get(TestingHubConstants.somOrderNumber));
      }

      if (new Date().getTime() > time + waitForSOMOrderCreation) {
        break;
      }
    } while (Strings.isNullOrEmpty(somDetails.get(TestingHubConstants.somOrderNumber)));

    if (Strings.isNullOrEmpty(somDetails.get(TestingHubConstants.somOrderNumber))) {
      AssertUtils.fail("SOM order not found for PO number " + poNumber + " even after waiting for "
          + (waitForSOMOrderCreation / 1000) / 60 + " minutes");
    }

    somOrderNumber = somDetails.get(TestingHubConstants.somOrderNumber);
    return somOrderNumber;

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