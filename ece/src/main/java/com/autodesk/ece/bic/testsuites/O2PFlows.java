package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.BICTestBase;
import com.autodesk.ece.testbase.BICTestBase.Names;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.PWSTestBase;
import com.autodesk.ece.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class O2PFlows extends ECETestBase {

  private static final String defaultLocale = "en_US";
  LinkedHashMap<String, String> testDataForEachMethod = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    Map<?, ?> loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String localeConfigFile = "LOCALE_CONFIG_" + GlobalConstants.ENV.toUpperCase();
    Map<?, ?> localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);

    testDataForEachMethod = (LinkedHashMap<String, String>) loadYaml
        .get("default");

    if (locale == null || locale.trim().isEmpty()) {
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale", locale);

    LinkedHashMap<String, Map<String, String>> localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);

    if (localeDataMap == null || localeDataMap.get(locale) == null) {
      AssertUtils.fail("The locale configuration is not found for the given country/locale: " + locale);
    } else {
      testDataForEachMethod.putAll(localeDataMap.get(locale));
    }
  }

  @Test(groups = {"validate-quote-creation"}, description = "Stub to automate PWS quote creation")
  public void validateQuoteCreation() {
    PWSTestBase tb = new PWSTestBase(testDataForEachMethod.get("pwsClientId"),
        ProtectedConfigFile.decrypt(testDataForEachMethod.get("pwsClientSecret")),
        testDataForEachMethod.get("pwsHostname"));

    Address address = new Address(testDataForEachMethod.get(BICECEConstants.ADDRESS),
        testDataForEachMethod.get("addressCountryCode"));
    Names names = BICTestBase.generateFirstAndLastNames();
    String email = BICTestBase.generateUniqueEmailID();
    tb.createQuote(email, names, address, testDataForEachMethod.get(BICECEConstants.currencyStore));
  }
}
