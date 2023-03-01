package com.autodesk.ece.testbase;

import static org.testng.util.Strings.isNotNullAndNotEmpty;
import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.sikuli.script.Key;

public class FinancingTestBase {

  private final Page_ financingPage;
  private final WebDriver driver;
  private Map<String, String> testData = new HashMap<>();

  public FinancingTestBase(GlobalTestBase testBase) {
    financingPage = testBase.createPage("PAGE_FINANCING");
    this.driver = testBase.getdriver();
  }

  public void setTestData(Map<String, String> testData) {
    this.testData = testData;
  }

  @Step("Go through Financing Application" + GlobalConstants.TAG_TESTINGHUB)
  public void completeFinancingApplication(LinkedHashMap<String, String> data) {
    try {
      Util.printInfo("current URL : " + driver.getCurrentUrl());
      financingPage.clickUsingLowLevelActions("financingGetStarted");
      Util.sleep(2000);

      Util.printInfo("current URL : " + driver.getCurrentUrl());
      financingPage.populateField("financingEmailId", testData.get(BICECEConstants.emailid));
      financingPage.clickUsingLowLevelActions("financingApplicationContinue");
      Util.sleep(2000);

      Util.printInfo("current URL : " + driver.getCurrentUrl());
      financingPage.populateField("financingVerificationCode", "9999");

      financingPage.clickUsingLowLevelActions("financingVerificationCodeContinue");
      Util.sleep(2000);
      Util.printInfo("current URL : " + driver.getCurrentUrl());

      if (!isNotNullAndNotEmpty(data.get("isFinancingRenewal"))) {
        WebElement applicationType = driver.findElement(By.xpath("//*[@id=\"applicantType\"]/div"));
        applicationType.click();
        Actions a = new Actions(driver);
        a.sendKeys(Keys.chord(Key.DOWN, Key.ENTER)).perform();

        financingPage.populateField("financingNumberOfEmployees", "1000");
        financingPage.populateField("financingLegalName", "Infra Company Inc");
        financingPage.populateField("financingDescriptionOfBusiness", "Construction Company");
        financingPage.populateField("financingCompanyPhoneNumber", "8888888888");
        financingPage.populateField("financingBusinessWebsite", "www.construction.com");
        financingPage.populateField("financingMonthOfIncorporation", "12/1983");
        financingPage.populateField("financingTaxId", "12-1212122");
        financingPage.populateField("financingLastYearsSales", "12000");
        financingPage.populateField("financingCurrentYearsProjectedSales", "20000");

        Util.printInfo("Adding address details");
        financingPage.populateField("financingBusinessAddressStreet", "111 Market St");
        financingPage.populateField("financingBusinessAddressCity", "San Francisco");
        WebElement state = driver.findElement(By.xpath("//*[@id=\"address.stateCode\"]/div"));
        state.click();
        Actions stateAction = new Actions(driver);
        stateAction.sendKeys(Keys.chord(Key.DOWN, Key.ENTER)).perform();
        financingPage.populateField("financingBusinessAddressZipCode", "94101");

        Util.printInfo("Adding Personal details");
        financingPage.populateField("financingContactTitle", "Manager");
        financingPage.populateField("financingContactBirthDate", "01/10/1991");
      }

      financingPage.clickUsingLowLevelActions("financingSubmit");
      Util.sleep(2000);
      Util.printInfo("current URL : " + driver.getCurrentUrl());
      AssertUtils.assertTrue(driver.getCurrentUrl().contains("thanks"),
          "Successfully submitted the application to LiftForward");

      financingPage.waitForFieldPresent("financingApplicationConfirmation", 5000);
      boolean financingApplicationConfirmation = financingPage.isFieldVisible("financingApplicationConfirmation");
      Util.sleep(2000);
      AssertUtils.assertEquals(financingApplicationConfirmation, true,
          "Successfully submitted the application to LiftForward");

    } catch (MetadataException e) {
      e.printStackTrace();
    }
  }

}
