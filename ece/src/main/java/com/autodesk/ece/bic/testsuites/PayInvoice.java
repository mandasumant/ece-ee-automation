package com.autodesk.ece.bic.testsuites;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.testbase.ece.ECETestBase;
import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.testinghub.core.utils.NetworkLogs;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PayInvoice extends ECETestBase {

  private static final String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  Map<?, ?> bankInformationByLocaleYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);

  private String PASSWORD;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs(getDriver());
    loadYaml = ResourceFileLoader.getBicOrderYaml();
    localeConfigYaml = ResourceFileLoader.getLocaleConfigYaml();
    bankInformationByLocaleYaml = ResourceFileLoader.getBankInformationByLocaleYaml();
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    testDataForEachMethod = (LinkedHashMap<String, String>) loadYaml.get("default");
    if (locale == null || locale.trim().isEmpty()) {
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale", locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml.get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);

    testDataForEachMethod.put("taxOptionEnabled", "undefined");

    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));

  }

  @Test(groups = {"pay-invoice-single-annual"}, description = "Validate Pay Invoice for single annual")
  public void validatePayInvoiceSingleAnnual() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Boolean isLoggedOut = true;
    Integer attempt = 0;

    //TODO: remove test data
    //Q2O SUS - Single Line Item - Annual - AU - LOC
    //scenario=SINGLE_ANNUAL
    //For testing: use data from Postgres to get Quote user credentials.
    //Postgres: environment='INT' and scenario='SINGLE_ANNUAL' and status='PENDING'
    results.put("orderNumber", "1000142901");
    testDataForEachMethod.put(BICConstants.emailid, "biz-thubstoreausztgxizdxvztt@letscheck.pw");

    results.putAll(testDataForEachMethod);
    if (results.containsKey(BICConstants.orderNumber) && results.get(BICConstants.orderNumber) != null) {
      results.put(BICECEConstants.ORDER_ID, results.get(BICConstants.orderNumber));
    }

    if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.LOC)) {
      String paymentType = System.getProperty(BICECEConstants.NEW_PAYMENT_TYPE) != null ? System.getProperty(
          BICECEConstants.NEW_PAYMENT_TYPE)
          : System.getProperty(BICECEConstants.STORE).equalsIgnoreCase("STORE-NAMER")
              ? BICECEConstants.VISA
              : BICECEConstants.CREDITCARD;
      testDataForEachMethod.put(BICECEConstants.PAYMENT_TYPE, paymentType);
      System.setProperty(BICECEConstants.PAYMENT, paymentType);

      while (isLoggedOut) {
        attempt++;
        if (attempt > 5) {
          Assert.fail("Retries Exhausted: Payment of Invoice failed because Session issues. Check Screenshots!");
        }

        portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICConstants.emailid),
            PASSWORD);

        isLoggedOut = false;

      }
    } else {
      Assert.fail("NON LOC Orders Do NOT have Pay Invoice Flow!!!");
    }

    updateTestingHub(testResults);
  }

}
