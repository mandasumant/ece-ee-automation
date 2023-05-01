package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.EISTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EDUTestBase {

  private static final String EDU_SIGNUP_SUBMIT = "eduSignSubmit";
  private static final String EDU_GET_STARTED = "eduSignStarted";
  private static final String EDU_NEW_PASSWORD = "newPassword";

  private static final HashMap<EDUUserType, String> EDU_USER_CHOICES = new HashMap<EDUUserType, String>() {
    {
      put(EDUUserType.STUDENT, "Student");
      put(EDUUserType.EDUCATOR, "Educator");
      put(EDUUserType.ADMIN, "ITAdmin");
      put(EDUUserType.MENTOR, "Mentor");
    }
  };

  private final Page_ eduPage;
  private final WebDriver driver;
  private final Map<String, String> testData;
  BICTestBase bicTestBase;
  MailosaurAPIClient mailosaurAPIClient;

  public EDUTestBase(GlobalTestBase testbase, LinkedHashMap<String, String> testData) {
    Util.PrintInfo("EDUTestBase from ece");
    eduPage = testbase.createPage("PAGE_EDU");
    driver = testbase.getdriver();
    bicTestBase = new BICTestBase(driver, testbase);
    mailosaurAPIClient = new MailosaurAPIClient();
    this.testData = testData;
  }

  /**
   * Create and register a new EDU user
   *
   * @param userType - Whether we want to create a student or educator account
   * @return - Test step results
   */
  @Step("Register EDU User" + GlobalConstants.TAG_TESTINGHUB)
  public Map<String, String> registerUser(EDUUserType userType) {
    HashMap<String, String> results = new HashMap<>();
    int currentYear = Year.now().getValue();

    // Navigate to education site and click on "Get Started"
    eduPage.navigateToURL(testData.get("eduLandingPage"));
    eduPage.click("getStarted");
    eduPage.waitForField("createAccountEDU", true, 5000);
    eduPage.click("createAccountEDU");

    // Fill out country, role, education role
    pickSelectOption("eduCountry", "GB");
    pickSelectOption("eduType", "1"); // "1" is "High School/Secondary"

    switch (userType) {
      case STUDENT:
        pickSelectOption(BICECEConstants.EDU_ROLE, "1"); // "1" is "Student"

        // Fill out student date of birth
        pickSelectOption("dobMonth", "1");
        pickSelectOption("dobDay", "1");
        // Mock a 16 year old student
        pickSelectOption("dobYear", Integer.toString(currentYear - 16));
        break;
      case EDUCATOR:
        pickSelectOption(BICECEConstants.EDU_ROLE, "4"); // "4" is "Educator"
        break;
      case MENTOR:
        pickSelectOption(BICECEConstants.EDU_ROLE, "3"); // "3" is "Design Competition Mentor"
        break;
      case ADMIN:
        pickSelectOption(BICECEConstants.EDU_ROLE, "2"); // "2" is "School IT Administrator"
        break;
    }

    eduPage.click(BICECEConstants.EDU_SUBMIT);

    registerOxygenUser(results);
    Util.sleep(2500);

    if (userType != EDUUserType.MENTOR) {
      // Pick a school called "Broadway"
      eduPage.waitForField("eduSchool", true, 10000);
      try {
        eduPage.sendKeysInTextFieldSlowly("eduSchool", "Saint");
        Util.sleep(2500);
      } catch (Exception e) {
        AssertUtils.fail("Failed to search for school");
        e.printStackTrace();
      }
      eduPage.waitForField("eduSchoolOption", true, 5000);

      try {
        eduPage.clickUsingLowLevelActions("eduSchoolOption");
        Util.sleep(2500);
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

      eduPage.click(BICECEConstants.EDU_SUBMIT);
      Util.sleep(2500);
    }

    eduPage.waitForField("eduComplete", true, 10000);

    String oxygenId = driver.manage().getCookieNamed("identity-sso").getValue();
    results.put(BICConstants.oxid, oxygenId);

    eduPage.click("eduComplete");
    Util.sleep(2500);

    return results;
  }

  @Step("Register EDU User" + GlobalConstants.TAG_TESTINGHUB)
  public Map<String, String> registerUser2(EDUUserType userType) {
    HashMap<String, String> results = new HashMap<>();
    int currentYear = Year.now().getValue();

    // Navigate to education site and click on "Get Started"
    eduPage.navigateToURL(testData.get("eduLandingPage"));
    eduPage.click("getStarted");

    //pickSelectOption(BICECEConstants.EDU_ROLE, EDU_USER_CHOICES.get(userType));
    String choiceName = EDU_USER_CHOICES.get(userType);
    pickVSOS2Option("eduRoleSelection", choiceName);

    if (userType == EDUUserType.STUDENT) {
      // Fill out student date of birth
      pickVSOS2Option("dobMonth2", "1");
      pickVSOS2Option("dobDay2", "1");
      // Mock a 16 year old student
      pickVSOS2Option("dobYear2", Integer.toString(currentYear - 16));
    }

    eduPage.waitForField("eduRoleSubmit", true, 5000);
    eduPage.click("eduRoleSubmit");

    registerOxygenUser(results);
    verifyRegistrationEmail(results.get(BICConstants.emailid));

    eduPage.waitForField(EDU_GET_STARTED, true, 5000);

    String oxygenId = driver.manage().getCookieNamed("identity-sso").getValue();
    results.put(BICConstants.oxid, oxygenId);

    return results;
  }

  /**
   * From the main education landing page with a logged in user, click through the flow to accept VSOS (student
   * verification) terms
   */
  public void signUpUser(HashMap<String, String> results) {
    acceptVSOSTerms(results);

    int attempts = 0;

    while (attempts < 5) {
      // Clear session storage cache and login again
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("window.sessionStorage.clear()");
      eduPage.refresh();
      loginUser(results.get(BICConstants.emailid), results.get(BICECEConstants.PASSWORD));

      dismissSuccessPopup();
      Util.sleep(5000);
      eduPage.refresh();
      boolean success = verifyEducationStatus();

      if (!success) {
        attempts++;
        if (attempts == 5) {
          AssertUtils.fail("Failed to login with verified education account");
        }
        Util.printInfo("Logged in user still not verified, sleeping and trying again. Attempt: " + attempts);
        Util.sleep(30000L * attempts);
      } else {
        break;
      }
    }
  }

  @Step("Accept VSOS terms" + GlobalConstants.TAG_TESTINGHUB)
  private void acceptVSOSTerms(HashMap<String, String> results) {
    try {
      WebElement overviewPageHeader = driver.findElement(
          By.xpath(eduPage.getFirstFieldLocator("eduWelcomeHeader")));
      AssertUtils.assertTrue(
          overviewPageHeader.getText().contains("Hi " + results.get("firstName")));
      eduPage.waitForField(EDU_GET_STARTED, true, 5000);
      eduPage.click(EDU_GET_STARTED);
      eduPage.waitForField(EDU_SIGNUP_SUBMIT, true, 5000);
      eduPage.click(EDU_SIGNUP_SUBMIT);
      eduPage.click(EDU_SIGNUP_SUBMIT);
      Util.sleep(3000);

      validateSheerIDDocument(results, overviewPageHeader);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to sign VSOS terms");
    }
  }


  @Step("Accept VSOS terms" + GlobalConstants.TAG_TESTINGHUB)
  public void acceptVSOSTerms2(HashMap<String, String> results, EDUUserType userType) {
    try {
      eduPage.waitForField(EDU_GET_STARTED, true, 5000);
      WebElement overviewPageHeader = driver.findElement(
          By.xpath(eduPage.getFirstFieldLocator("eduWelcomeHeader")));
      AssertUtils.assertTrue(
          overviewPageHeader.getText().contains("Unlock educational access to Autodesk products"));
      eduPage.click(EDU_GET_STARTED);
      eduPage.waitForField(EDU_SIGNUP_SUBMIT, true, 5000);

      pickVSOS2OptionByLabel("vsosCountry", "United Kingdom");

      if (userType == EDUUserType.EDUCATOR || userType == EDUUserType.STUDENT || userType == EDUUserType.ADMIN) {
        pickVSOS2Option("vsosInstitutionType", "secondary");
        try {
          eduPage.clickUsingLowLevelActions("vsosInstitutionName");
          eduPage.populateField("vsosInstitutionName", "Saint");
        } catch (MetadataException e) {
          AssertUtils.fail("Could not click on role dropdown");
        }
        WebElement schoolOption = driver.findElement(
            By.xpath("//ul[@class=\"vsos-option-list\"]/li[2]"));
        schoolOption.click();
      }

      if (userType == EDUUserType.STUDENT) {
        int currentYear = Year.now().getValue();
        pickVSOS2Option("vsosEnrollmentMonth", "8"); // August
        pickVSOS2Option("vsosEnrollmentDay", "1");
        pickVSOS2Option("vsosEnrollmentYear", Integer.toString(currentYear - 5));
        pickVSOS2Option("vsosGraduationMonth", "6"); // June
        pickVSOS2Option("vsosGraduationDay", "1");
        pickVSOS2Option("vsosGraduationYear", Integer.toString(currentYear + 1));
      }

      eduPage.click(EDU_SIGNUP_SUBMIT);
      if (userType != EDUUserType.MENTOR) {
        eduPage.click(EDU_SIGNUP_SUBMIT);
      }

      Util.sleep(3000);

      if (userType == EDUUserType.MENTOR) {
        return;
      }

      validateSheerIDDocument(results, overviewPageHeader);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to sign VSOS terms");
    }
  }

  /**
   * Login as a user on the EDU landing page
   *
   * @param username - User's email
   * @param password - User's password
   */
  @Step("Login EDU User" + GlobalConstants.TAG_TESTINGHUB)
  public void loginUser(String username, String password) {
    // Navigate to education site and click on "Get Started"
    eduPage.navigateToURL(testData.get("eduLandingPage"));
    eduPage.click("getStarted");

    eduPage.waitForField("eduSignIn", true, 5000);
    eduPage.click("eduSignIn");

    // Enter the username and password
    eduPage.waitForField("eduUsername", true, 5000);
    eduPage.populateField("eduUsername", username);
    eduPage.click("eduUsernameNext");

    eduPage.waitForField(EDU_NEW_PASSWORD, true, 5000);
    eduPage.populateField(EDU_NEW_PASSWORD, password);
    eduPage.click("eduSubmit");

    eduPage.waitForField("eduSkipTFA", true, 5000);

    try {
      if (eduPage.checkIfElementExistsInPage("eduSkipTFA", 10)) {
        eduPage.click("eduSkipTFA");
      }
    } catch (MetadataException e) {
      e.printStackTrace();
    }
  }

  @Step("Dismiss registration success message" + GlobalConstants.TAG_TESTINGHUB)
  public void dismissSuccessPopup() {
    try {
      eduPage.waitForField("eduSignSuccess", true, 5000);
      if (eduPage.checkIfElementExistsInPage("eduSignSuccess", 10)) {
        eduPage.click("eduSignSuccess");
      }
    } catch (Exception e) {
      Util.printInfo("Dismissing Success popup is unsuccessful, no Action to be taken.");
    }
  }

  @Step("Verify Education Status" + GlobalConstants.TAG_TESTINGHUB)
  public boolean verifyEducationStatus() {
    String xPath = eduPage.getFirstFieldLocator("eduStatus");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath)));
    String status = driver.findElement(By.xpath(xPath)).getText();
    Util.printInfo("Current status: " + status);
    return status.contains("Your educational access to Autodesk products is valid");
  }

  /**
   * On an "Individual" education tab page, download Fusion 360
   */
  @Step("Download Fusion 360" + GlobalConstants.TAG_TESTINGHUB)
  public void downloadF360Product() {
    // Click on menus to download Fusion 360
    eduPage.click("eduFusionGet");
    eduPage.waitForField("eduFusionAccess", true, 10000);
    eduPage.click("eduFusionAccess");

    // Downloading opens a new browser tab, so save the current tab handle and determine the
    // handle of the new tab
    String currentTabHandle = driver.getWindowHandle();
    switchToNextTab();

    // Assert that we have successfully downloaded the product
    WebElement downloadTitle = driver.findElement(
        By.xpath(eduPage.getFirstFieldLocator("eduFusionStatus")));
    AssertUtils.assertTrue(
        downloadTitle.getText().contains("Thank you for downloading Fusion 360"));

    // Verify cards container does not blank out
    WebElement cardsContainer =
        driver.findElement(By.xpath(eduPage.getFirstFieldLocator("eduCardsContainer")));
    AssertUtils.assertTrue(
        cardsContainer.getText().contains("Download, sign-in, and start designing"));

    // Close the new tab and switch back to the old tab
    driver.close();
    driver.switchTo().window(currentTabHandle);
  }

  /**
   * Download an education product by websdk ID
   *
   * @param websdk - Product websdk ID
   */
  @Step("Download an education Product by plc " + GlobalConstants.TAG_TESTINGHUB)
  public void downloadProduct(String websdk) {
    // Activate the product
    WebElement getProductButton = driver.findElement(
        By.xpath("//*[@style=\"display: flex;\"]/a[@websdk-plc=\"" + websdk + "\"]"));
    AssertUtils.assertTrue(getProductButton.isDisplayed() && getProductButton.isEnabled(),
        "Ensuring that Get Product button is interactable");
    getProductButton.click();
    Util.sleep(2500);

    // Wait for the download button to appear
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id(websdk)));

    // Click on the download button
    WebElement downloadButton = driver.findElement(By.id(websdk))
        .findElement(By.cssSelector(".downloadWidget button.websdkButton"));
    AssertUtils.assertTrue(downloadButton.isDisplayed() && downloadButton.isEnabled(),
        "Ensuring that Download button is interactable");
    downloadButton.click();
    Util.sleep(2000);

    try {
      if (eduPage.checkIfElementExistsInPage("eduDownloadAccept", 10)) {
        Util.printInfo("Found the 'Accept' button during product download.");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.getElementsByClassName('websdkButton agreeButton')[0].click();");

        Util.printInfo("Clicked on 'Accept' button during product download.");
      } else {
        Util.printInfo("'Accept' button is NOT found during product download.");
      }
    } catch (Exception e) {
      Util.printInfo(e.getMessage());
    }

    // Wait a bit for downloads to start
    Util.sleep(1000);

    validateDownload();

    WebElement overviewPageHeader = driver.findElement(
        By.xpath(eduPage.getFirstFieldLocator("eduWelcomeHeader")));
    AssertUtils.assertTrue(
        overviewPageHeader.getText().contains("Hi "));
  }

  @Step("Activate an admin 3ds Max license " + GlobalConstants.TAG_TESTINGHUB)
  public void activateAdmin3dsLicense() {
    // Activate the product
    WebElement getProductButton = driver.findElement(
        By.xpath("//*[@style=\"display: flex;\"]/a[@websdk-plc=\"websdk-3ds-max\"]"));
    AssertUtils.assertTrue(getProductButton.isDisplayed() && getProductButton.isEnabled(),
        "Ensuring that Get Product button is interactable");
    getProductButton.click();
    Util.sleep(2500);

    // Select 3dsMax 2021
    eduPage.click("3dsMaxVersionButton");
    eduPage.click("3dsMax2021");

    // Click on the download button
    WebElement downloadButton = driver.findElement(By.id("websdk-3ds-max"))
        .findElement(By.cssSelector(".downloadWidget button.websdkButton"));
    AssertUtils.assertTrue(downloadButton.isDisplayed() && downloadButton.isEnabled(),
        "Ensuring that Download button is interactable");
    downloadButton.click();

    try {
      if (eduPage.checkIfElementExistsInPage("eduDownloadAccept", 10)) {
        Util.printInfo("Found the 'Accept' button during product download.");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.getElementsByClassName('websdkButton agreeButton')[0].click();");

        Util.printInfo("Clicked on 'Accept' button during product download.");
      } else {
        Util.printInfo("'Accept' button is NOT found during product download.");
      }
    } catch (Exception e) {
      Util.printInfo(e.getMessage());
    }

    // Wait a bit for downloads to start
    Util.sleep(1000);

    validateDownload();
  }

  @Step("Validate admin license " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> assertAdminLicense() {
    HashMap<String, String> results = new HashMap<>();
    try {
      Util.sleep(5000);
      eduPage.waitForFieldVisible("adminLicenseSerial", 5000);
      String serialNumber = eduPage.getMultipleTextValuesfromField("adminLicenseSerial")[0];
      String productKey = eduPage.getMultipleTextValuesfromField("adminProductKey")[0];

      AssertUtils.assertFalse(serialNumber.trim().isEmpty(), "Serial number should be valid");
      AssertUtils.assertFalse(productKey.trim().isEmpty(), "Product key should be valid");
      results.put(BICECEConstants.EDU_SERIAL_NUMBER, serialNumber);
      results.put(BICECEConstants.EDU_PRODUCT_KEY, productKey);
    } catch (MetadataException e) {
      AssertUtils.fail("Failed to validate admin license");
    }
    return results;
  }

  /**
   * Activate an educator subscription for a product and open the subscription in Portal. If the activation fails, it
   * will retry up to 5 times
   *
   * @param productLink - The anchor of the product to activate
   * @throws MetadataException
   */
  @Step("Activate Product Class Subscription" + GlobalConstants.TAG_TESTINGHUB)
  public void activateProductAndAssignUsers(String productLink) throws MetadataException {
    boolean assignmentSuccess = false;
    int assignmentAttemptCount = 0;

    while (!assignmentSuccess) {
      eduPage.clickUsingLowLevelActions("educationClassLabTab");
      eduPage.clickUsingLowLevelActions("subscriptionAcceptButton");
      Util.sleep(5000);

      try {
        // Assign user
        WebElement activateButton = driver.findElement(
            By.cssSelector(".edu-activate-cta [href=\"" + productLink + "\"]"));
        activateButton.click();
      } catch (Exception ex) {
        Util.printWarning("Failed to click on activate, product already activated");
      }
      eduPage.clickUsingLowLevelActions("educationConfirmButton");

      // Wait time because it takes up to 15 sec sometimes to load assignUsersButton
      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
      wait.until(
          ExpectedConditions.invisibilityOfElementLocated(By.className("model-body--loading")));

      if (eduPage.checkIfElementExistsInPage("eduAssignmentPending", 10)) {
        WebElement modalPendingMessage = eduPage.getMultipleWebElementsfromField("eduAssignmentPending").get(0);
        if (modalPendingMessage.getText().contains("It may take some time to complete this request")) {
          eduPage.clickUsingLowLevelActions("pendingConfirm");
          driver.navigate().to("https://stg-manage.autodesk.com/user-access/products/product-list");
          return;
        }
      }

      // Check if there was an error assigning users, and attempt to click on the retry button
      List<WebElement> errorModal = driver.findElements(
          By.xpath(eduPage.getFirstFieldLocator("eduAssignmentError")));
      if (!errorModal.isEmpty() && errorModal.get(0).isDisplayed()) {
        Util.printWarning("Seat assignment failed, retrying");
        WebElement closeButton = driver.findElement(
            By.cssSelector(".modal-footer--issues .close-btn"));
        closeButton.click();
        assignmentAttemptCount++;

        if (assignmentAttemptCount > 5) {
          AssertUtils.fail("Failed to assign seats to educator");
        }
      } else {
        Util.printInfo("Successfully assigned Seats to Educator");
        assignmentSuccess = true;
      }
    }

    switchToNextTab();
    eduPage.clickUsingLowLevelActions("assignUsersButton");
  }

  public void downloadF360LabPackage() {
    // Activate the product
    WebElement getProductButton = driver.findElement(
        By.xpath("//*[@style=\"display: flex;\"]/a[@websdk-plc=\"websdk-fusion-360\"]"));
    AssertUtils.assertTrue(getProductButton.isDisplayed() && getProductButton.isEnabled(),
        "Ensuring that Get Product button is interactable");
    getProductButton.click();
    Util.sleep(2500);

    eduPage.click("eduDownloadF360Lab");

    validateDownload();
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

  /**
   * Switch to a neighboring tab
   */
  public void switchToNextTab() {
    String currentTabHandle = driver.getWindowHandle();
    Set<String> windowHandles = driver.getWindowHandles();
    windowHandles.remove(currentTabHandle); // The new tab is the tab that isn't the current one
    String newTabHandle = windowHandles.iterator().next();
    driver.switchTo().window(newTabHandle);
  }

  private void registerOxygenUser(HashMap<String, String> results) {
    // Generate a new user email, name, and password
    String email = BICTestBase.generateMailosaurEmailID();
    results.put(BICConstants.emailid, email);
    String randomString = RandomStringUtils.random(6, true, false);
    String firstName = "FN" + randomString;
    String lastName = "LN" + randomString;

    eduPage.waitForField("firstname", true, 30000);
    eduPage.populateField("firstname", firstName);
    eduPage.populateField("lastname", lastName);
    eduPage.populateField("newEmail", email);
    eduPage.populateField("newConfirmEmail", email);

    String password = ProtectedConfigFile.decrypt(testData.get(BICECEConstants.EDU_PASSWORD));
    eduPage.populateField(EDU_NEW_PASSWORD, password);
    results.put("firstName", firstName);
    results.put("password", password);

    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("document.getElementById('privacypolicy_checkbox').click()");

    eduPage.click(BICECEConstants.EDU_SUBMIT);

    try {
      if (eduPage.checkIfElementExistsInPage("eduRegisterComplete", 5)) {
        // Opt out of marketing emails to avoid clogging up the mailosaur server
        eduPage.clickUsingLowLevelActions("eduCommunicationOptIn");
        Util.printInfo("Registration Complete shown, clicking on done button");
        eduPage.click("eduRegisterComplete");
      } else {
        Util.printInfo("Registration Complete not shown");
      }
    } catch (MetadataException e) {
      Util.printError("Failed to click on complete button: " + e.getMessage());
    }
  }

  /**
   * Find in mailosaur the account registration email sent to a specific user and click on the verification link
   *
   * @param email - Recipient email
   */
  private void verifyRegistrationEmail(String email) {
    String verifyAccountEmailBody = mailosaurAPIClient.getMessageBody(email);
    Document doc = Jsoup.parse(verifyAccountEmailBody);
    Element verifyButton = doc.body().select("a:contains(Verify email)").get(0);
    String verifyLink = verifyButton.attr("href");
    AssertUtils.assertTrue(verifyLink != null, "Email verification link must be defined");
    driver.navigate().to(verifyLink);
    try {
      eduPage.clickUsingLowLevelActions("eduCommunicationOptIn"); // Opt out of marketing emails
    } catch (MetadataException e) {
      AssertUtils.fail("Failed to unsubscribe from marketing emails");
      throw new RuntimeException(e);
    }
    eduPage.click("eduRegisterComplete");
  }

  /**
   * Automate selection of values in custom VSOS select dropdowns
   *
   * @param name  - Selector name of dropdown
   * @param value - Value to select
   */
  private void pickVSOS2Option(String name, String value) {
    try {
      eduPage.clickUsingLowLevelActions(name);
    } catch (MetadataException e) {
      AssertUtils.fail("Could not click on role dropdown");
    }
    WebElement option = driver.findElement(
        By.xpath("//ul[@class=\"vsos-option-list\"]/li[@data-name=\"" + value + "\"]"));
    option.click();
  }

  private void pickVSOS2OptionByLabel(String name, String label) {
    try {
      eduPage.clickUsingLowLevelActions(name);
    } catch (MetadataException e) {
      AssertUtils.fail("Could not click on role dropdown");
    }
    WebElement option = driver.findElement(
        By.xpath("//ul[@id=\"vsos-typeahead-ul\"]/li[text()=\"" + label + "\"]"));
    option.click();
  }

  /**
   * Validate document upload for SheerID
   */
  private void validateSheerIDDocument(HashMap<String, String> results, WebElement overviewPageHeader)
      throws URISyntaxException {
    String sheerUploadLocator = eduPage.getFirstFieldLocator("sheerUpload");
    String signupSuccessLocator = eduPage.getFirstFieldLocator("eduSignSuccess");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    wait.until(ExpectedConditions.or(
        ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(sheerUploadLocator)),
        ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(signupSuccessLocator))
    ));

    // If we're requested to upload a document, upload the testing ID for SheerID
    if (!driver.findElements(By.xpath(sheerUploadLocator)).isEmpty()) {
      ClassLoader classLoader = this.getClass().getClassLoader();
      String sheerIDImage = EISTestBase.getTestDataDir()
          + EISTestBase.getTestManifest().getProperty("EDU_SHEER_ID_APPROVE");
      URL resource = classLoader.getResource(sheerIDImage);
      assert resource != null;
      String sheerIDImagePath = Paths.get(resource.toURI()).toFile().getPath();

      // Make the upload button interactable
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("document.getElementById('vsos-file-upload-input').style.opacity = 1");

      // Upload the tesing ID file
      WebElement sheerUpload = driver.findElement(By.xpath(sheerUploadLocator));
      sheerUpload.sendKeys(sheerIDImagePath);
      Util.sleep(5000);

      // Assert that we are still in Overview page after upload
      overviewPageHeader = driver.findElement(
          By.xpath(eduPage.getFirstFieldLocator("eduPageHeader")));
      AssertUtils.assertTrue(
          overviewPageHeader.getText().contains("Additional documentation needed"));

      eduPage.isFieldPresent(EDU_SIGNUP_SUBMIT);
      eduPage.click(EDU_SIGNUP_SUBMIT);
      Util.sleep(3000);

      WebElement landingPageHeader = driver.findElement(
          By.xpath(eduPage.getFirstFieldLocator("eduPageHeader")));

      // Sometimes the Submit Action is not working, so retrying one last time
      if (!(landingPageHeader.getText().contains("Thank you"))) {

        overviewPageHeader = driver.findElement(
            By.xpath(eduPage.getFirstFieldLocator("eduPageHeader")));
        AssertUtils.assertTrue(
            overviewPageHeader.getText().contains("Additional documentation needed"));

        eduPage.click(EDU_SIGNUP_SUBMIT);
        Util.sleep(3000);

        landingPageHeader = driver.findElement(
            By.xpath(eduPage.getFirstFieldLocator("eduPageHeader")));
        AssertUtils.assertTrue(
            landingPageHeader.getText().contains("Thank you"));
      }

      eduPage.click("eduUploadClose");
      Util.sleep(3000);

      assertGreeting(results.get("firstName"));
    }
  }

  public void assertGreeting(String username) {
    WebElement overviewHomeHeader = driver.findElement(
        By.xpath(eduPage.getFirstFieldLocator("eduOverviewHomeHeader")));
    AssertUtils.assertTrue(
        overviewHomeHeader.getText().contains("Hi " + username));
  }

  private void validateDownload() {
    Path downloadPath = Paths.get(GlobalConstants.DEFAULT_DOWNLOAD_FOLDER_PATH);
    File downloadDirectory = downloadPath.toFile();
    try {
      int attempts = 0;
      File[] downloadingFiles = null;
      while (attempts < 5) {
        downloadingFiles = downloadDirectory.listFiles();
        if (downloadingFiles != null && downloadingFiles.length > 0) {
          break;
        }
        Util.printInfo("Polling download directory for files, attempt " + (attempts + 1));
        attempts++;
        Util.sleep(2500);
        downloadDirectory = downloadPath.toFile();
      }
      AssertUtils.assertTrue(downloadingFiles != null && downloadingFiles.length > 0,
          "Ensure there are downloading files");

      Util.printInfo("Downloading files:");
      for (int i = 0; i < Math.min(downloadingFiles.length, 10); i++) {
        Util.printInfo("   " + downloadingFiles[i].getName());
      }
      if (downloadingFiles.length > 10) {
        Util.printInfo("   ... and " + (downloadingFiles.length - 10) + " more file(s)");
      }
    } catch (NullPointerException exception) {
      AssertUtils.fail("Failed to locate download directory");
    }
  }

  public enum EDUUserType {
    STUDENT,
    EDUCATOR,
    ADMIN,
    MENTOR
  }
}
