package com.autodesk.eceapp.testbase;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.constants.EceAppConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class EceCheckoutTestBase extends EceBICTestBase {

  LinkedHashMap<String, String> envDataConstants;
  Map<String, String> localeDataConstants;

  public EceCheckoutTestBase(WebDriver driver, GlobalTestBase testbase, String locale) {
    super(driver, testbase);

    Map<?, ?> loadYaml = YamlUtil.loadYmlWithFileLocation(EceAppConstants.APP_ENV_RESOURCE_PATH + "BicOrder.yml");
    envDataConstants = (LinkedHashMap<String, String>) loadYaml
        .get("default");

    Map<?, ?> localeConfigYaml = YamlUtil.loadYmlWithFileLocation(EceAppConstants.APP_MISC_RESOURCE_PATH + "LocaleConfig.yml");
    LinkedHashMap<String, Map<String, String>> localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    localeDataConstants = localeDataMap.get(locale);
  }

  public void clickOnContinueButton() {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("document.querySelector('button[data-testid=\"mfe-checkout-cart-continue-btn\"]')?.click()");
//    WebElement continueButton = driver.findElement(
//        By.xpath("//button[data-testid=\"mfe-checkout-cart-continue-btn\"]"));
//    continueButton.click();

//    try {
//      if (bicPage.checkIfElementExistsInPage("cartContinueButton", 15)) {
//        Util.printInfo("Clicking on Continue button");
//        bicPage.clickUsingLowLevelActions("cartContinueButton");
//      }
//    } catch (MetadataException e) {
//      Util.printWarning("Failed to click on continue button");
//    }
  }

  public void loginAccount(String username, String password) {
    switchToBICCartLoginPage();
    Util.sleep(5000);

    bicPage.waitForField(BICECEConstants.AUTODESK_ID, true, 30000);

    Util.printInfo(BICECEConstants.AUTODESK_ID + " is Present " + bicPage
        .isFieldPresent(BICECEConstants.AUTODESK_ID));
    bicPage.click(BICECEConstants.AUTODESK_ID);
    bicPage.populateField(BICECEConstants.AUTODESK_ID, username);

    bicPage.click(BICECEConstants.USER_NAME_NEXT_BUTTON);
    Util.sleep(3000);

    bicPage.waitForField(BICECEConstants.LOGIN_PASSWORD, true, 30000);
    bicPage.click(BICECEConstants.LOGIN_PASSWORD);
    bicPage.populateField(BICECEConstants.LOGIN_PASSWORD, password);

    bicPage.waitForField(BICECEConstants.LOGIN_BUTTON, true, 30000);
    bicPage.clickToSubmit(BICECEConstants.LOGIN_BUTTON, 10000);

    bicPage.waitForField(BICECEConstants.GET_STARTED_SKIP_LINK, true, 30000);
    boolean status = bicPage.isFieldPresent(BICECEConstants.GET_STARTED_SKIP_LINK);

    if (status) {
      bicPage.click(BICECEConstants.GET_STARTED_SKIP_LINK);
    }

    driver.switchTo().defaultContent();
    waitForLoadingSpinnerToComplete("loadingSpinner");
    Util.printInfo("Successfully logged into Bic");
  }

  public void updateLineItemQuantity(int quantity) {
    List<WebElement> editButton = driver.findElements(By.cssSelector("[data-wat-val=\"edit\"]"));
    if (editButton.size() > 0) {
      editButton.get(0).click();
      Util.sleep(5000); // Wait for slidedown animation to finish
    }
    WebElement spinner = driver.findElement(By.cssSelector("[data-testid=\"product-line-item-0\"] #quantity"));
    spinner.click();
    spinner.sendKeys(Keys.SHIFT, Keys.HOME);
    Util.sleep(1000);
//    spinner.sendKeys(Keys.DELETE);
//    Util.sleep(1000);
    spinner.sendKeys(Integer.toString(quantity));
    Util.sleep(1000); // Wait for react state to settle

    ((JavascriptExecutor) driver).executeScript(
        "document.querySelector('[data-testid=\"product-line-item-0\"] #quantity').dispatchEvent(new Event('change'))");
    Util.sleep(1000);
  }

}
