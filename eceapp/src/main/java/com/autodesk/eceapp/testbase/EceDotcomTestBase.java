package com.autodesk.eceapp.testbase;

import java.util.LinkedHashMap;
import java.util.Map;

import com.autodesk.eceapp.utilities.ResourceFileLoader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;

public class EceDotcomTestBase extends EceBICTestBase {

  public static Page_ dotcomPage = null;
  LinkedHashMap<String, String> envDataConstants;
  Map<String, String> localeDataConstants;

  public EceDotcomTestBase(WebDriver driver, GlobalTestBase testbase, String locale) {
    super(driver, testbase);

    Map<?, ?> loadYaml = ResourceFileLoader.getBicOrderYaml();
    envDataConstants = (LinkedHashMap<String, String>) loadYaml.get("default");


    Map<?, ?> localeConfigYaml = ResourceFileLoader.getLocaleConfigYaml();
    LinkedHashMap<String, Map<String, String>> localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    localeDataConstants = localeDataMap.get(locale);
  }

  public void navigateToDotComPage(String productName) {
    String constructDotComURL =
        envDataConstants.get("guacDotComBaseURL") + localeDataConstants.get(BICECEConstants.COUNTRY_DOMAIN)
            + envDataConstants.get(BICECEConstants.PRODUCTS_PATH) + productName + BICECEConstants.OVERVIEW;

    Util.printInfo("constructDotComURL " + constructDotComURL);
    getUrl(constructDotComURL);

    setStorageData();
  }

  public void selectFlexTokens() {
    bicPage.waitForFieldPresent("flexTab", 5000);
    try {
      bicPage.clickUsingLowLevelActions("flexTab");
    } catch (MetadataException e) {
      AssertUtils.fail("Failed to select flex tab");
    }
  }

  public void selectPurchaseFlexTokens() {
    bicPage.waitForFieldPresent("buyTokensButton", 5000);
    Util.sleep(3000);

    closeGetHelpPopup();

    try {
      bicPage.clickUsingLowLevelActions("buyTokensButton");
    } catch (WebDriverException | MetadataException e) {
      Util.printInfo(e.getMessage());
    }
  }
}
