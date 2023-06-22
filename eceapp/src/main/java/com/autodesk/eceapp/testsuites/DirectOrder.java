package com.autodesk.eceapp.testsuites;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.dto.IProductDetails;
import com.autodesk.eceapp.dto.IPurchaserDetails;
import com.autodesk.eceapp.fixtures.CustomerBillingDetails;
import com.autodesk.eceapp.fixtures.OxygenUser;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EceCheckoutTestBase;
import com.autodesk.eceapp.testbase.EceDotcomTestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import org.testng.util.Strings;

public class DirectOrder {
  private static final String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;

  String productName;
  OxygenUser user = new OxygenUser();
  CustomerBillingDetails billingDetails;
  WebDriver driver;
  GlobalTestBase testbase;

  public DirectOrder(WebDriver driver, GlobalTestBase testbase) {
    this.driver = driver;
    this.testbase = testbase;
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String localeConfigFile = "LOCALE_CONFIG";
    localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
  }

  public HashMap<String, String> createDirectOrder(List<IProductDetails> productDetailsList,
      IPurchaserDetails purchaserDetails, String locale, String paymentMethod)
      throws MetadataException {
    EceDotcomTestBase dotcomTestBase = new EceDotcomTestBase(driver, testbase, locale);
    EceCheckoutTestBase checkoutTestBase = new EceCheckoutTestBase(driver, testbase, locale);
    EceBICTestBase bicTestBase = new EceBICTestBase(driver, testbase);

    testDataForEachMethod = (LinkedHashMap<String, String>) loadYaml
        .get("default");

    if (locale == null || locale.trim().isEmpty()) {
      locale = defaultLocale;
    }
    testDataForEachMethod.put("locale", locale);
    System.setProperty(BICECEConstants.LOCALE, locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    testDataForEachMethod.put("taxOptionEnabled", "undefined");
    testDataForEachMethod.put("productType", "flex");
    testDataForEachMethod.put(BICECEConstants.ADDRESS, purchaserDetails.getAddress());
    System.setProperty(BICECEConstants.ADDRESS, purchaserDetails.getAddress());

    for (IProductDetails product : productDetailsList) {
      if (product.getProductName().equals("flex")) {
        dotcomTestBase.navigateToDotComPage("autocad");
        dotcomTestBase.selectFlexTokens();
        dotcomTestBase.selectPurchaseFlexTokens();
      } else {
        dotcomTestBase.navigateToDotComPage(product.getProductName());

        switch (product.getTerm()) {
          case "monthly":
            dotcomTestBase.selectMonthlySubscription();
            break;
          case "1year":
            dotcomTestBase.selectYearlySubscription();
            break;
          case "3year":
            dotcomTestBase.selectThreeYearSubscription();
            break;
        }

        dotcomTestBase.subscribeAndAddToCart(testDataForEachMethod);

      }
      bicTestBase.setStorageData();
      checkoutTestBase.updateLineItemQuantity(product.getQuantity());
      checkoutTestBase.clickOnContinueButton();
      Util.sleep(5000); // Wait for quantity update to go through
    }





    checkoutTestBase.loginAccount(purchaserDetails.getEmail(), user.password);

    HashMap<String, String> results = new HashMap<>(testDataForEachMethod);
    if (Strings.isNullOrEmpty(paymentMethod)) {
      results.put(BICECEConstants.PAYMENT_TYPE, "somenonnullvaluesothatitdoesntcrash");
      bicTestBase.submitOrder(results, false);
      String orderNumber = bicTestBase.getOrderNumber(results);
      results.put(BICConstants.orderNumber, orderNumber);
    } else {
      testDataForEachMethod.put("paymentType", paymentMethod);
      System.setProperty(BICECEConstants.PAYMENT, paymentMethod);
      results.putAll(bicTestBase.placeFlexOrder(testDataForEachMethod));
    }

    return results;
  }
}
