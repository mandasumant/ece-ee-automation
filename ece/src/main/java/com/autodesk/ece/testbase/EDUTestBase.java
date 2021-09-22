package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class EDUTestBase {

  public static Page_ eduPage = null;
  public WebDriver driver = null;
  LinkedHashMap<String, String> testData = null;

  public EDUTestBase(GlobalTestBase testbase, LinkedHashMap<String, String> testData) {
    Util.PrintInfo("EDUTestBase from ece");
    eduPage = testbase.createPage("PAGE_EDU");
    this.driver = testbase.getdriver();
    this.testData = testData;
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
    eduPage.navigateToURL(testData.get("eduLandingPage"));
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

    String oxygenId = driver.manage().getCookieNamed("identity-sso").getValue();
    results.put(BICConstants.oxid, oxygenId);

    eduPage.waitForField("eduComplete", true, 5000);
    eduPage.clickToSubmit("eduComplete");

    return results;
  }

  /**
   * Mark a user as an approved education user in the EDU database
   *
   * @param oxygenId - Oxygen ID of the user to approve
   */
  public void verifyUser(String oxygenId) {
    String baseUrl = testData.get("eduVerificationEndpoint").replace("{oxygenId}", oxygenId);
    Response response = given()
        .auth().basic(testData.get("eduAEMUser"), testData.get("eduAEMPassword"))
        .when().get(baseUrl);
  }

  /**
   * From the main education landing page with a logged in user, click through the flow to accept
   * VSOS (student verification) terms
   */
  public void signUpUser() {
    eduPage.waitForField("eduSignStarted", true, 5000);
    eduPage.click("eduSignStarted");
    eduPage.waitForField("eduSignSubmit", true, 5000);
    eduPage.click("eduSignSubmit");
    eduPage.waitForField("eduSignSuccess", true, 5000);
    eduPage.click("eduSignSuccess");
    Util.sleep(5000);
    eduPage.refresh();

    String xPath = eduPage.getFirstFieldLocator("eduStatus");
    String status = driver.findElement(By.xpath(xPath)).getText();
    AssertUtils.assertTrue(
        status.contains("Your educational access to Autodesk products is valid"));
  }

  /**
   * On an "Individual" education tab page, download Fusion 360
   */
  public void downloadF360Product() {
    // Click on menus to download Fusion 360
    eduPage.click("eduFusionGet");
    eduPage.waitForField("eduFusionAccess", true, 10000);
    eduPage.click("eduFusionAccess");

    // Downloading opens a new browser tab, so save the current tab handle and determine the
    // handle of the new tab
    String currentTabHandle = driver.getWindowHandle();
    Set<String> windowHandles = driver.getWindowHandles();
    windowHandles.remove(currentTabHandle); // The new tab is the tab that isn't the current one
    String newTabHandle = windowHandles.iterator().next();
    driver.switchTo().window(newTabHandle);

    // Assert that we have successfully downloaded the product
    WebElement downloadTitle = driver.findElement(
        By.xpath(eduPage.getFirstFieldLocator("eduFusionStatus")));
    AssertUtils.assertTrue(
        downloadTitle.getText().contains("Thank you for downloading Fusion 360"));

    // Close the new tab and switch back to the old tab
    driver.close();
    driver.switchTo().window(currentTabHandle);
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
    // Wait time because it takes up to 15 sec sometimes to load assignUsersButton
    Util.sleep(15000);
    eduPage.clickUsingLowLevelActions("assignUsersButton");
  }

  public void validateFusionActivation() throws MetadataException {
    // verify that Fusion is visible in a list of products
    eduPage.waitForField("eduFusionProduct", true, 10);
    Util.printInfo("Verify fusion");
    eduPage.checkIfElementExistsInPage("eduFusionProduct", 10);
  }

  public enum EDUUserType {
    STUDENT,
    EDUCATOR
  }
}
