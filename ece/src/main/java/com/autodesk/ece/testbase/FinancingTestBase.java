package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.WebDriver;

public class FinancingTestBase {

  private final Page_ finacingPage;
  private final WebDriver driver;
  private Map<String, String> testData = new HashMap<>();

  public FinancingTestBase(GlobalTestBase testBase) {
    finacingPage = testBase.createPage("PAGE_FINANCING");
    this.driver = testBase.getdriver();
  }

  public void setTestData(Map<String, String> testData) {
    this.testData = testData;
  }

  @Step("Go through Financing APPLICATION" + GlobalConstants.TAG_TESTINGHUB)
  public void completeFinancingApplication() {
   try {
     Util.printInfo("current URL : " + driver.getCurrentUrl());
     finacingPage.clickUsingLowLevelActions("financingGetStarted");
     Util.sleep(2000);

     Util.printInfo("current URL : " + driver.getCurrentUrl());
     finacingPage.populateField("financingEmailId",testData.get(BICConstants.emailid));
     finacingPage.clickUsingLowLevelActions("financingApplicationContinue");
     Util.sleep(2000);

     Util.printInfo("current URL : " + driver.getCurrentUrl());
     finacingPage.populateField("financingVerificationCode","1234");
     finacingPage.clickUsingLowLevelActions("financingVerificationCodeContinue");
     Util.sleep(2000);

     Util.printInfo("current URL : " + driver.getCurrentUrl());
     finacingPage.waitForFieldPresent("financingSelectApplication", 1000);
     finacingPage.clickUsingLowLevelActions("financingSelectApplication");

     finacingPage.populateField("financingNumberOfEmployees","1000");
     finacingPage.populateField("financingLegalName","Infra Company Inc");

     finacingPage.populateField("financingDescriptionOfBusiness","Construction Company");
     finacingPage.populateField("financingCompanyPhoneNumber","8888888888");
     finacingPage.populateField("financingBusinessWebsite","www.construction.com");
     finacingPage.populateField("financingMonthOfIncorporation","12/1983");
     finacingPage.populateField("financingTaxId","12-1212122");
     finacingPage.populateField("financingLastYearsSales","12000");
     finacingPage.populateField("financingCurrentYearsProjectedSales","20000");

     Util.printInfo("Adding address details ");
     finacingPage.populateField("financingBusinessAddressStreet","111 Market St");
     finacingPage.populateField("financingBusinessAddressCity","San Francisco");
     finacingPage.clickUsingLowLevelActions("financingBusinessAddressState");
     finacingPage.populateField("financingBusinessAddressZipCode","94101");

     Util.printInfo("Adding Personal details ");
     finacingPage.populateField("financingContractTitle","Manager");
     finacingPage.populateField("financingContactGiven","John");
     finacingPage.populateField("financingContactFamily","Doe");
     finacingPage.clickUsingLowLevelActions("financingContactSuffix");
     finacingPage.populateField("financingPhone","8888888888");
     finacingPage.populateField("financingContactBirthDate","01/10/1991");
     finacingPage.clickUsingLowLevelActions("financingSubmit");
     Util.sleep(2000);
     Util.printInfo("current URL : " + driver.getCurrentUrl());
     AssertUtils.assertEquals(driver.getCurrentUrl().contains("thanks"),"Successfully submitted the application to liftForward");

     finacingPage.waitForFieldPresent("financingApplicationConfirmation", 5000);
     boolean downloadStarted = finacingPage.isFieldVisible("financingApplicationConfirmation");
     Util.sleep(2000);
     AssertUtils.assertEquals(downloadStarted, true, "Successfully submitted the application to liftForward");

   }catch (MetadataException e ){
     e.printStackTrace();
   }
  }

}
