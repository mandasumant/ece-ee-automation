package com.autodesk.eceapp.testbase;

import static org.testng.util.Strings.isNotNullAndNotEmpty;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.constants.EceAppConstants;
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class EceFinancingTestBase {

  private final Page_ financingPage;
  private final WebDriver driver;
  private Map<String, String> testData = new HashMap<>();

  public EceFinancingTestBase(GlobalTestBase testBase) {
    financingPage = testBase.createPageForApp("PAGE_FINANCING", EceAppConstants.APP_NAME);
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

      if (!isNotNullAndNotEmpty(data.get("isReturningUser"))) {
        WebElement applicationType = driver.findElement(By.xpath("//*[@id=\"applicantType\"]/div"));
        applicationType.click();
        financingPage.clickUsingLowLevelActions("financingBusinessCorporation");

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
        financingPage.clickUsingLowLevelActions("financingStateAlabama");

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
