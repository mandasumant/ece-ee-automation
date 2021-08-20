package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.listeners.TestingHubAPIClient;
import com.autodesk.testinghub.core.database.DBValidations;
import com.autodesk.testinghub.core.sap.SAPDriverFiori;
import com.autodesk.testinghub.core.testbase.*;
import com.autodesk.testinghub.core.utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterTest;

import java.util.HashMap;
import java.util.Set;

public class ECETestBase {

    protected static SAPTestBase saptb = null;
    protected static DBValidations dbValtb = null;
    protected SFDCTestBase sfdctb = null;
    protected TIBCOServiceTestBase tibcotb = null;
    protected SOAPTestBase soaptb = null;
    protected PortalTestBase portaltb = null;
    protected SAPDriverFiori sapfioritb = null;
    protected DynamoDBValidation dynamotb = null;
    protected ApigeeTestBase resttb = null;
    protected PelicanTestBase pelicantb = null;
    protected HerokuTestBase herokutb = null;
    protected LemTestBase lemtb = null;
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
        saptb = new SAPTestBase();
        soaptb = new SOAPTestBase();
        portaltb = new PortalTestBase(testbase);
        resttb = new ApigeeTestBase();
        sapfioritb = new SAPDriverFiori(GlobalConstants.getTESTDATADIR(), webdriver);
        dynamotb = new DynamoDBValidation();
        pelicantb = new PelicanTestBase();
        herokutb = new HerokuTestBase();
        lemtb = new LemTestBase();

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

    public SiebelTestBase getSiebelTestBase() {
        if (siebeltb == null) {
            siebeltb = new SiebelTestBase();
        }
        return siebeltb;
    }

    public SFDCTestBase getSFDCTestBase() {
        return new SFDCTestBase(webdriver);
    }

    public AoeTestBase getAOETestBase() {
        if (aoetb == null) {
            aoetb = new AoeTestBase(webdriver);
        }
        return aoetb;
    }

    public SAPTestBase getSAPTestBase() {
        return new SAPTestBase();
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

    public SAPDriverFiori getSAPFioriTestBase() {
        return new SAPDriverFiori("appBaseDir", webdriver);
    }

    public RegonceTestBase getRegonceTestBase() {
        if (regoncetb == null) {
            regoncetb = new RegonceTestBase(webdriver);
        }
        return regoncetb;
    }

    public PWSTestBase getPWSTestBase() {
        if (pwstb == null) {
            pwstb = new PWSTestBase();
        }
        return pwstb;
    }

    @AfterTest(alwaysRun = true)
    public void afterTest() {
        try {
            Util.printInfo("Closing Webdriver after the end of the test");
            testbase.closeBrowser();
            tibcotb.tibcoConnectionClose();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (saptb.loginSAPStatus == true) {
                Util.printInfo("Closing SAPDriver after the end of the test");
                saptb.logoffSAP();
                saptb.closeSAP();
            }
        }
    }
}

