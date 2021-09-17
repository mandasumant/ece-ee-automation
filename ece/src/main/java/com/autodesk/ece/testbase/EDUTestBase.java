package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.time.Year;
import java.util.HashMap;
import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class EDUTestBase extends ECETestBase {

  public static Page_ eduPage = null;
  public WebDriver driver = null;

  public EDUTestBase(WebDriver driver, GlobalTestBase testbase) {
    Util.PrintInfo("EDUTestBase from ece");
    eduPage = testbase.createPage("PAGE_EDU");
    this.driver = testbase.getdriver();
  }

  /**
   * Create and register a new EDU user
   *
   * @param userType - Whether we want to create a student or educator account
   * @return - Test step results
   */
  @Step("Register EDU User")
  public HashMap<String, String> registerUser(EDUUserType userType) {
    HashMap<String, String> results = new HashMap<String, String>();
    int currentYear = Year.now().getValue();

    // Navigate to education site and click on "Get Started"
    eduPage.navigateToURL(
        "https://www-pt.autodesk.com/education/edu-software/overview?sorting=featured&filters=individual");
    eduPage.click("getStarted");
    eduPage.waitForField("createAccountEDU", true, 5000);
    eduPage.click("createAccountEDU");

    // Fill out country, role, education role
    pickSelectOption("eduCountry", "US");
    pickSelectOption("eduType", "1"); // "1" is "High School/Secondary"

    switch (userType) {
      case STUDENT:
        pickSelectOption("eduRole", "1"); // "1" is "Student"

        // Fill out student date of birth
        pickSelectOption("dobMonth", "1");
        pickSelectOption("dobDay", "1");
        // Mock a 16 year old student
        pickSelectOption("dobYear", Integer.toString(currentYear - 16));
        break;
      case EDUCATOR:
        pickSelectOption("eduRole", "4"); // "4" is "Educator"
        break;
    }

    eduPage.clickToSubmit("eduSubmit");

    // Generate a new user email, name, and password
    String email = BICTestBase.generateUniqueEmailID();
    results.put(BICConstants.emailid, email);
    String randomString = RandomStringUtils.random(6, true, false);
    String firstName = "FN" + randomString;
    String lastName = "LN" + randomString;

    eduPage.waitForField("firstname", true, 30000);
    eduPage.populateField("firstname", firstName);
    eduPage.populateField("lastname", lastName);
    eduPage.populateField("newEmail", email);
    eduPage.populateField("newConfirmEmail", email);
    eduPage.populateField("newPassword", "Password1");

    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("document.getElementById('privacypolicy_checkbox').click()");

    eduPage.clickToSubmit("eduSubmit");

    // Pick a school called "Broadway"
    eduPage.waitForField("eduSchool", true, 5000);
    eduPage.populateField("eduSchool", "Broadway");
    eduPage.waitForField("eduSchoolOption", true, 5000);

    try {
      eduPage.clickUsingLowLevelActions("eduSchoolOption");
    } catch (MetadataException e) {
      e.printStackTrace();
    }

    // For students, specify the enrollment dates for a student in grade 11
    if (userType == EDUUserType.STUDENT) {
      pickSelectOption("enrollStartMonth", "8"); // August
      pickSelectOption("enrollStartYear", Integer.toString(currentYear - 5));
      pickSelectOption("enrollEndMonth", "6"); // June
      pickSelectOption("enrollEndYear", Integer.toString(currentYear + 1));
    }

    eduPage.clickToSubmit("eduSubmit");
    eduPage.waitForField("eduComplete", true, 5000);
    eduPage.clickToSubmit("eduComplete");

    return results;
  }

  /**
   * Select a value from a select element
   *
   * @param field - Page field name
   * @param value - Value to select
   */
  private void pickSelectOption(String field, String value) {
    String xPath = eduPage.getFirstFieldLocator(field);
    WebElement element = driver.findElement(By.xpath(xPath));
    Select selectElement = new Select(element);
    selectElement.selectByValue(value);
  }

  public void activateFusionAndAssignUsers() throws MetadataException {
    // Activate new subscription model for Fusion 360
    eduPage.clickUsingLowLevelActions("educationClassLabTab");
    eduPage.clickUsingLowLevelActions("educationFusionGetStartedButton");
    eduPage.clickUsingLowLevelActions("vsosSubmitButton");
    eduPage.clickUsingLowLevelActions("getAutodeskSoftwareButton");
    eduPage.clickUsingLowLevelActions("subscriptionAcceptButton");

    // Assign user
    eduPage.clickUsingLowLevelActions("activateFusionClassButton");
    eduPage.clickUsingLowLevelActions("educationConfirmButton");
    // Wait time because it takes up to 15 sec somtimes to load assignUsersButton
    Util.sleep(15000);
    eduPage.clickUsingLowLevelActions("assignUsersButton");
  }

  public void validateFusionActivation() throws MetadataException {
    // Wait time to load the page
    Util.sleep(10000);
    // verify that Fusion is visible in a list of products
    Util.printInfo("Verify fusion");
    eduPage.checkIfElementExistsInPage("eduFusionProduct", 10);
  }

  public enum EDUUserType {
    STUDENT,
    EDUCATOR
  }
}
