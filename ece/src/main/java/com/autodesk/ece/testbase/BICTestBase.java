package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.EISTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.testbase.SAPTestBase;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.JsonParser;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BICTestBase {

  public static Page_ bicPage = null;
  public WebDriver driver;
  ZipPayTestBase zipTestBase;
  FinancingTestBase financingTestBase;


  public BICTestBase(WebDriver driver, GlobalTestBase testbase) {
    Util.PrintInfo("BICTestBase from ece");
    this.driver = driver;
    bicPage = testbase.createPage("PAGE_BIC_CART");
    zipTestBase = new ZipPayTestBase(testbase);
    financingTestBase = new FinancingTestBase(testbase);
  }

  public static void clearTextInputValue(WebElement element) {
    String inputText = element.getAttribute("value");
    if (inputText != null) {
      for (int i = 0; i < inputText.length(); i++) {
        element.sendKeys(Keys.BACK_SPACE);
      }
    }

  }

  public static Names generateFirstAndLastNames() {
    String randomString = RandomStringUtils.random(6, true, false);
    String firstName = "FN" + randomString;
    Util.printInfo(BICECEConstants.FIRST_NAME1 + firstName);
    String lastName = "LN" + randomString;
    Util.printInfo(BICECEConstants.LAST_NAME1 + lastName);
    return new Names(firstName, lastName);
  }

  @Step("Generate email id")
  public static String generateUniqueEmailID() {
    String storeKey = System.getProperty("store").replace("-", "");
    String emailType = System.getProperty("emailType");

    String sourceName = "thub";
    String emailDomain = "letscheck.pw";
    if(emailType != null && !emailType.isEmpty()) {
      if (Arrays.asList("biz", "edu", "gov", "org").contains(emailType)) {
        sourceName = emailType + "-" + sourceName;
      }
    }

    String timeStamp = new RandomStringUtils().random(12, true, false);
    String strDate = null;
    String stk = storeKey.replace("-", "");
    if (storeKey.contains("NAMER")) {
      stk = "NAMER";
    }
    strDate = sourceName + stk + timeStamp + "@" + emailDomain;

    return strDate.toLowerCase();
  }

  @Step("get billing address")
  public Map<String, String> getBillingAddress(String address) {
    Map<String, String> ba = null;

    Address newAddress = new Address(address);

    // TODO: Replace address maps with address object (ECEEPLT-2724)
    ba = new HashMap<String, String>();
    ba.put(BICECEConstants.ORGANIZATION_NAME, newAddress.company);
    ba.put(BICECEConstants.FULL_ADDRESS, newAddress.addressLine1);
    ba.put(BICECEConstants.CITY, newAddress.city);
    ba.put(BICECEConstants.ZIPCODE, newAddress.postalCode);
    ba.put(BICECEConstants.PHONE_NUMBER, newAddress.phoneNumber);
    ba.put(BICECEConstants.COUNTRY, newAddress.country);

    if (newAddress.province != null) {
      ba.put(BICECEConstants.STATE_PROVINCE, newAddress.province);
    }
    Util.printInfo("The address being used :" + address);

    return ba;
  }

  @Step("Create BIC account")
  public void createBICAccount(Names names, String emailID, String password, Boolean skipIframe) {
    if (!skipIframe) {
      switchToBICCartLoginPage();
    }
    Util.printInfo("Url is loaded and we were able to switch to iFrame");

    try {
      bicPage.waitForFieldPresent("createNewUserGUAC", 30000);
      bicPage.clickUsingLowLevelActions("createNewUserGUAC");
      bicPage.waitForFieldPresent(BICECEConstants.BIC_FN, 30000);
      bicPage.populateField(BICECEConstants.BIC_FN, names.firstName);
      bicPage.waitForFieldPresent("bic_LN", 30000);
      bicPage.populateField("bic_LN", names.lastName);
      bicPage.populateField("bic_New_Email", emailID);
      bicPage.populateField("bic_New_ConfirmEmail", emailID);
      bicPage.waitForFieldPresent("bic_Password", 30000);
      bicPage.populateField("bic_Password", password);
    } catch (MetadataException e) {
      e.printStackTrace();
      Assert.fail("Failed to populate values in Create Account");
    }

    try {

      Util.printInfo("Checked bic_Agree is visible - " + bicPage.isFieldVisible(
          BICECEConstants.BIC_AGREE));
      Util.printInfo("Checked box status for bic_Agree - " + bicPage.isChecked(
          BICECEConstants.BIC_AGREE));

      if (!bicPage.isFieldVisible(BICECEConstants.BIC_AGREE)) {
        bicPage.waitForField(BICECEConstants.BIC_AGREE, true, 30000);
        Util.printInfo("Checkbox bic_Agree is visible - " + bicPage.isFieldVisible(
            BICECEConstants.BIC_AGREE));
        Util.printWarning(
            "Checkbox bic_Agree is present - " + bicPage.isFieldPresent(BICECEConstants.BIC_AGREE));
        Util.printWarning(
            "Checkbox bic_Agree field exists - " + bicPage.checkFieldExistence(
                BICECEConstants.BIC_AGREE));
      }

      checkboxTickJS();
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to click on Create account button in BIC-Cart application");
    }

    Util.printInfo("Successfully clicked on Create user button");

    if (bicPage.isFieldVisible("createAutodeskAccount")) {
      AssertUtils.fail("Cart login page is redirection failure");
    } else {
      Util.PrintInfo("Created account successfully");
    }

    driver.switchTo().defaultContent();

    waitForLoadingSpinnerToComplete("loadingSpinner");
  }

  private void checkboxTickJS() {
    try {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("document.getElementById('privacypolicy_checkbox').click()");

      js.executeScript("document.getElementById('btnSubmit').click()");
      bicPage.waitForField("verifyAccount", true, 5000);
      if (bicPage.checkIfElementExistsInPage("verifyAccount", 5)) {
        if (driver.findElement(By.xpath("//label[@id='optin_checkbox']")).getAttribute("class")
            .contains("checked")) {
          Util.printInfo("Option checkbox is already selected..");
        } else {
          js.executeScript("document.getElementById('optin_checkbox').click()");
        }

        js.executeScript("document.getElementById('bttnAccountVerified').click()");
        bicPage.waitForFieldAbsent("signInSection", 5000);
      }
    } catch (Exception e) {
      AssertUtils.fail("Application Loading issue : Unable to click on privacypolicy_checkbox");
    }
  }

  private void switchToBICCartLoginPage() {
    String elementXpath = bicPage.getFirstFieldLocator("createNewUseriFrame");
    Util.waitForElement(elementXpath, "Create New User iFrame");
    WebElement element = driver.findElement(By.xpath(elementXpath));
    Util.printInfo("Switching to User login frame");
    driver.switchTo().frame(element);
  }

  /**
   * Skip the adding of seats in the "Adds Seats" modal
   */
  @Step("Skip add seats modal")
  public void skipAddSeats() {
    Boolean isSkipAddSeatsDisplayed = true;
    Integer attempt = 0;

    Util.printInfo("Finding the 'Skip' button");
    while (isSkipAddSeatsDisplayed) {
      Util.sleep(3000);
      attempt++;

      if (attempt > 4) {
        Util.printInfo("Retry logic: Failed to find or click on 'Skip' button, attempt, #" + attempt);
        Util.printInfo("Session Storage: set 'nonsensitiveHasAddSeatsBeenProcessed' to true.");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.sessionStorage.setItem(\"nonsensitiveHasAddSeatsBeenProcessed\",\"true\");");
        driver.navigate().refresh();
        waitForLoadingSpinnerToComplete("loadingSpinner");
        break;
      }

      try {
        Util.printInfo("Attempt: " + attempt);
        bicPage.waitForField("addSeatsReturnUser", true, 30000);
        isSkipAddSeatsDisplayed = driver.findElement(
            By.xpath(bicPage.getFirstFieldLocator("addSeatsReturnUser"))).isDisplayed();
        Util.printInfo("The 'Skip' button from add seats modal is displayed. Attempt no " + attempt + " to close it.");
        bicPage.clickUsingLowLevelActions("addSeatsReturnUser");
        Util.sleep(2000);
        Util.printInfo("Click action performed on 'Skip' button from add seats modal.");
      } catch (Exception e) {
        Util.printInfo("The 'Skip' button from add seats modal is not present.");
        isSkipAddSeatsDisplayed = false;
      }
    }

    List<WebElement> submitButton = driver.findElements(By.cssSelector(
        "[data-testid=\"order-summary-section\"] .checkout--order-summary-section--submit-order .checkout--order-summary-section--submit-order--button-container button"));
    if (submitButton.size() == 0) {
      AssertUtils.fail("The 'Submit order' button is not displayed.");
    }
  }

  @Step("Wait for loading spinner to complete")
  public void waitForLoadingSpinnerToComplete(String elementXPath) {
    Util.sleep(3000);
    try {
      int count = 0;
      while (driver.findElement(By.xpath(elementXPath))
          .isDisplayed()) {
        count++;
        Util.sleep(1000);
        if (count > 20) {
          break;
        }
        Util.printInfo("Loading spinner visible: " + count + " second(s)");
      }
    } catch (Exception e) {
      Util.printInfo("There is no loading spinner element.");
    }
    Util.sleep(2000);
  }

  @Step("Login to an existing BIC account")
  public void loginAccount(HashMap<String, String> data) {
    switchToBICCartLoginPage();

    bicPage.waitForField(BICECEConstants.AUTODESK_ID, true, 30000);

    Util.printInfo(BICECEConstants.AUTODESK_ID + " is Present " + bicPage
        .isFieldPresent(BICECEConstants.AUTODESK_ID));
    bicPage.click(BICECEConstants.AUTODESK_ID);
    bicPage.populateField(BICECEConstants.AUTODESK_ID, data.get(BICConstants.emailid));

    bicPage.click(BICECEConstants.USER_NAME_NEXT_BUTTON);
    Util.sleep(3000);

    bicPage.waitForField(BICECEConstants.LOGIN_PASSWORD, true, 30000);
    bicPage.click(BICECEConstants.LOGIN_PASSWORD);
    bicPage.populateField(BICECEConstants.LOGIN_PASSWORD,
        ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD)));

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

  @Step("Add a seat from the existing subscription popup")
  public void existingSubscriptionAddSeat(HashMap<String, String> data) {
    // Wait for add seats popup
    Util.printInfo("Wait for add seats popup.");
    bicPage.waitForField("guacAddSeats", true, 3000);
    bicPage.clickToSubmit("guacAddSeats", 3000);
    bicPage.waitForPageToLoad();
  }

  @Step("Selecting Monthly Subscription")
  public void selectMonthlySubscription(WebDriver driver) {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    WebElement element;
    try {
      element = driver
          .findElement(By.xpath("//terms-container/div/div[3]/term-element[3]"));
    } catch (Exception e) {
      element = driver
          .findElement(By.xpath("//terms-container/div/div[4]/term-element[3]"));
    }
    executor.executeScript("arguments[0].click();", element);
    Util.sleep(2000);
  }

  @Step("Selecting Yearly Subscription")
  public void selectYearlySubscription(WebDriver driver) {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    WebElement element = driver
        .findElement(By.xpath("//terms-container/div/div[4]/term-element[2]"));
    executor.executeScript("arguments[0].click();", element);
    Util.sleep(2000);
  }

  @Step("Adding Product to Cart from DotCom")
  public String subscribeAndAddToCart() {
    String url = null;

    Util.sleep(5000);
    bicPage.waitForField("guacAddToCart", true, 3000);
    try {
      url = bicPage.getMultipleWebElementsfromField("guacAddToCart")
          .get(0).getAttribute("href");
    } catch (MetadataException e) {
      Assert.fail("Failed to get Cart URL from DotCom.");
    }

    bicPage.clickToSubmit("guacAddToCart", 3000);

    try {
      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id=\"checkout\"]")));
    } catch (Exception e) {
      AssertUtils.fail("Unable to redirect to checkout page.");
    }
    return url;
  }

  @Step("Populate billing address")
  public boolean populateBillingAddress(Map<String, String> address, Map<String, String> data) {
    boolean status = false;
    try {
      String paymentType = System.getProperty(BICECEConstants.PAYMENT);
      String dataPaymentType = data.get(BICECEConstants.PAYMENT_TYPE);
      String paymentProfile = "";
      Util.sleep(5000);

      switch (dataPaymentType.toUpperCase()) {
        case BICConstants.paymentTypePayPal:
        case BICConstants.paymentTypeDebitCard:
        case BICECEConstants.PAYMENT_BACS:
        case BICECEConstants.PAYMENT_TYPE_SEPA:
        case BICECEConstants.PAYMENT_TYPE_GIROPAY:
        case BICECEConstants.PAYMENT_TYPE_FINANCING:
          paymentProfile = dataPaymentType.toLowerCase();
          break;
        case BICECEConstants.PAYMENT_TYPE_ZIP:
          paymentProfile = BICECEConstants.ALTERNATE_PAYMENT_METHODS;
          break;
        default:
          paymentProfile = BICECEConstants.CREDIT_CARD;
          break;
      }

      // Temporary solution because currently it does not allow to submit an order with the address from Customer details section
      if (data.get("isNonQuoteFlexOrder") != null) {
        Util.sleep(20000);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(BICECEConstants.DOCUMENT_GETELEMENTBYID_MANDATE_AGREEMENT_CLICK);
      }

      String firstNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentProfile);
      String lastNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentProfile);

      clearTextInputValue(driver.findElement(By.xpath(firstNameXpath)));
      driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get(BICECEConstants.FIRSTNAME));

      Util.sleep(1000);
      clearTextInputValue(driver.findElement(By.xpath(lastNameXpath)));
      driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get(BICECEConstants.LASTNAME));
      status = populateBillingDetails(address, paymentType);

      try {
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        WebElement element = driver
            .findElement(By.xpath("//*[@id=\"usi_content\"]"));
        executor.executeScript("arguments[0].click();", element);
      } catch (Exception e) {
        Util.printInfo("Can not find the skip button and click");
      } finally {
        driver.manage().timeouts()
            .implicitlyWait(EISTestBase.getDefaultPageWaitTimeout(), TimeUnit.MILLISECONDS);
      }

      clickOnContinueBtn(paymentType);

    } catch (Exception e) {
      e.printStackTrace();
      debugPageUrl(e.getMessage());
      AssertUtils.fail("Unable to populate the Billing Address details");
    }
    return status;
  }

  public void debugPageUrl(String message) {
    Util.printInfo("-------------" + message + "----------------" +
        "\n" + " URL :            " + driver.getCurrentUrl() + "\n" +
        "\n" + " Page Title :     " + driver.getTitle() + "\n" +
        "\n" + "-----------------------------");
  }

  public void clickOnContinueBtn(String paymentType) {
    Util.sleep(2000);
    Util.printInfo("Clicking on Save button...");

    String tabKey = paymentType.toLowerCase();
    if (paymentType.equalsIgnoreCase(BICECEConstants.VISA) || paymentType.equalsIgnoreCase(BICECEConstants.CREDITCARD)
        || paymentType.equalsIgnoreCase(BICECEConstants.MASTERCARD)) {
      tabKey = "credit-card";
    }

    WebElement paymentTab = driver.findElement(By.cssSelector("[data-testid=\"tabs-panel-" + tabKey + "\"]"));
    WebElement continueButton = paymentTab.findElement(By.cssSelector("[data-testid=\"save-payment-profile\"]"));

    int attempts = 0;

    while (attempts < 5) {
      try {
        continueButton.click();

        waitForLoadingSpinnerToComplete("loadingSpinner");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.invisibilityOf(continueButton));

        Util.printInfo("Save button no longer present");
        break;
      } catch (TimeoutException e) {
        e.printStackTrace();
        Util.printInfo("Save button still present, retrying");
        attempts++;
        if (attempts == 5) {
          AssertUtils.fail("Failed to click on Save button on billing details page...");
        }
        Util.sleep(2000);
      }
    }

  }

  private void populateTaxIdForFlex() {
    String taxId = System.getProperty(BICECEConstants.TAX_ID);

    try {
      if (taxId != null && !taxId.isEmpty()) {
        if (bicPage.checkIfElementExistsInPage("taxIdForFlex", 5)) {
          Util.printInfo("Populating tax id: " + taxId);
          bicPage.populateField("taxIdForFlex", taxId);
          waitForLoadingSpinnerToComplete("loadingSpinner");
        }
      }
    } catch (Exception e) {
      Util.printTestFailedMessage("populateTaxIdForFlex");
      AssertUtils.fail("Unable to Populate Tax Id ");
    }
  }

  @Step("Populate Billing Details")
  @SuppressWarnings("static-access")
  private boolean populateBillingDetails(Map<String, String> address, String paymentType) {
    boolean status = false;
    try {
      Util.printInfo("Adding billing details...");
      Util.printInfo("Address Details :" + address);
      String isBusinessOrgNoSelection = "", orgNameXpath = "", fullAddrXpath = "", cityXpath = "", zipXpath = "", phoneXpath = "", countryXpath = "",
          stateXpath = "";
      String paymentTypeToken = null;
      switch (paymentType.toUpperCase()) {
        case BICConstants.paymentTypePayPal:
        case BICConstants.paymentTypeDebitCard:
        case BICECEConstants.PAYMENT_BACS:
        case BICECEConstants.PAYMENT_TYPE_SEPA:
        case BICECEConstants.PAYMENT_TYPE_GIROPAY:
        case BICECEConstants.PAYMENT_TYPE_FINANCING:
          paymentTypeToken = paymentType.toLowerCase();
          break;
        case BICECEConstants.PAYMENT_TYPE_ZIP:
          paymentTypeToken = BICECEConstants.ALTERNATE_PAYMENT_METHODS;
          break;
        default:
          paymentTypeToken = BICECEConstants.CREDIT_CARD;
          break;
      }

      isBusinessOrgNoSelection = bicPage.getFirstFieldLocator(BICECEConstants.IS_BUSINESS_ORG_NO_SELECTION)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      orgNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      fullAddrXpath = bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      cityXpath = bicPage.getFirstFieldLocator(BICECEConstants.CITY).replace(
          BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      zipXpath = bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE).replace(
          BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      phoneXpath = bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      countryXpath = bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      stateXpath = bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);

      clearTextInputValue(driver.findElement(By.xpath(fullAddrXpath)));
      driver.findElement(By.xpath(fullAddrXpath))
          .sendKeys(address.get(BICECEConstants.FULL_ADDRESS));

      WebElement countryEle = driver.findElement(By.xpath(countryXpath));
      Select selCountry = new Select(countryEle);
      WebElement countryOption = selCountry.getFirstSelectedOption();
      String defaultCountry = countryOption.getText();
      Util.printInfo("The Country selected by default : " + defaultCountry);
      selCountry.selectByVisibleText(address.get(BICECEConstants.COUNTRY));

      clearTextInputValue(driver.findElement(By.xpath(cityXpath)));
      driver.findElement(By.xpath(cityXpath)).sendKeys(address.get(BICECEConstants.CITY));

      if (!address.get(BICECEConstants.ZIPCODE).equals(BICECEConstants.NA)) {
        clearTextInputValue(driver.findElement(By.xpath(zipXpath)));
        driver.findElement(By.xpath(zipXpath)).sendKeys(address.get(BICECEConstants.ZIPCODE));
      }
      clearTextInputValue(driver.findElement(By.xpath(phoneXpath)));
      driver.findElement(By.xpath(phoneXpath)).sendKeys("2333422112");

      if (address.get(BICECEConstants.STATE_PROVINCE) != null && !address
          .get(BICECEConstants.STATE_PROVINCE).isEmpty()) {
        driver.findElement(By.xpath(stateXpath))
            .sendKeys(address.get(BICECEConstants.STATE_PROVINCE));
      }

      String taxId = System.getProperty(BICECEConstants.TAX_ID);

      if (taxId != null && !taxId.isEmpty()) {
        String numberKey;
        switch (System.getProperty(BICECEConstants.STORE)) {
          case "STORE-AUS":
            numberKey = BICECEConstants.ABN_NUMBER;
            break;
          default:
            numberKey = BICECEConstants.VAT_NUMBER;
            break;
        }

        try {
          driver.findElement(By.xpath(orgNameXpath))
              .sendKeys(address.get(
                  BICECEConstants.ORGANIZATION_NAME) + " " + RandomStringUtils.random(6, true, false));
        } catch (NoSuchElementException e) {
          // Catching no such element exception
          String element = bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
              .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
          Util.printInfo("Unable to locate: " + element + e.getMessage());
        }

        if (bicPage.checkIfElementExistsInPage(numberKey, 5)) {
          Util.printInfo("Populating" + numberKey + ": " + taxId);
          bicPage.populateField(numberKey, taxId);
          driver.findElement(By.xpath("//input[@name=\"" + numberKey + "\"]"))
              .sendKeys(Keys.TAB);
          waitForLoadingSpinnerToComplete("loadingSpinner");
        }
      } else {
        try {
          Util.printInfo("Toggling off company name field");
          driver.findElement(By.xpath(isBusinessOrgNoSelection)).click();
        } catch (Exception e) {
          Util.printInfo("Company name field optional");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      Util.printTestFailedMessage("populateBillingDetails");
      AssertUtils.fail("Unable to Populate Billing Details");
    }
    return status;
  }

  public String getPaymentDetails(String paymentMethod) {

    String paymentDetails;

    switch (paymentMethod.toUpperCase()) {
      case "MASTERCARD":
        paymentDetails = "2222400010000008@03 - Mar@30@737";
        break;
      case "AMEX":
        paymentDetails = "374251018720018@03 - Mar@30@7373";
        break;
      case "DISCOVER":
        paymentDetails = "6011601160116611@03 - Mar@30@737";
        break;
      case "JCB":
        paymentDetails = "3569990010095841@03 - Mar@30@737";
        break;
      case "ACH":
        paymentDetails = "123456789@011000138@ACH";
        break;
      case "BACS":
        paymentDetails = "40308669@560036@BACS";
        break;
      case "SEPA":
        paymentDetails = "DE87123456781234567890@SEPA";
        break;
      case "GIROPAY":
        paymentDetails = "Testbank Fiducia@10@4000@GIROPAY USER@DE36444488881234567890@GIROPAY";
        break;
      default:
        paymentDetails = "4000020000000000@03 - Mar@30@737";
    }
    return paymentDetails;
  }

  @Step("Populate payment details")
  public void populatePaymentDetails(String[] paymentCardDetails) {

    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 30000);

    try {
      WebElement creditCardNumberFrame = bicPage
          .getMultipleWebElementsfromField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME).get(0);
      WebElement expiryDateFrame = bicPage.getMultipleWebElementsfromField("expiryDateFrame")
          .get(0);
      WebElement securityCodeFrame = bicPage.getMultipleWebElementsfromField("securityCodeFrame")
          .get(0);

      driver.switchTo().frame(creditCardNumberFrame);
      Util.printInfo("Entering card number : " + paymentCardDetails[0]);
      Util.sleep(2000);
      bicPage.populateField("CardNumber", paymentCardDetails[0]);
      driver.switchTo().defaultContent();
      Util.sleep(2000);

      driver.switchTo().frame(expiryDateFrame);
      Util.printInfo(
          "Entering Expiry date : " + paymentCardDetails[1] + "/" + paymentCardDetails[2]);
      Util.sleep(2000);
      bicPage.populateField("expirationPeriod", paymentCardDetails[1] + paymentCardDetails[2]);
      driver.switchTo().defaultContent();
      Util.sleep(2000);

      driver.switchTo().frame(securityCodeFrame);
      Util.printInfo("Entering security code : " + paymentCardDetails[3]);
      Util.sleep(2000);
      bicPage.populateField("PAYMENTMETHOD_SECURITY_CODE", paymentCardDetails[3]);
      driver.switchTo().defaultContent();
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Card details to make payment");
    }
  }

  @Step("Populate Direct Debit payment details")
  public void populateACHPaymentDetails(String[] paymentCardDetails) {

    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 30000);

    try {
      Util.printInfo("Clicking on Direct Debit ACH tab...");
      bicPage.clickUsingLowLevelActions("directDebitACHTab");

      Util.printInfo("Waiting for Direct Debit ACH Header...");
      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("directDebitHead").get(0), 10);

      Util.printInfo("Entering Direct Debit ACH Account Number : " + paymentCardDetails[0]);
      bicPage.populateField("achAccNumber", paymentCardDetails[0]);

      Util.printInfo("Entering Direct Debit ACH Routing Number : " + paymentCardDetails[0]);
      bicPage.populateField("achRoutingNumber", paymentCardDetails[1]);
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Direct Debit details to make payment");
    }
    Util.sleep(20000);
  }

  @Step("Populate BACS payment details")
  public void populateBACSPaymentDetails(String[] paymentCardDetails, Map<String, String> address,
      Map<String, String> data) {

    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 30000);

    try {
      Util.printInfo("Clicking on Direct Debit BACS tab...");
      bicPage.clickUsingLowLevelActions("directDebitBACSTab");
      populateBillingAddress(address, data);

      Util.printInfo("Entering Direct Debit BACS Account Number : " + paymentCardDetails[0]);
      bicPage.populateField("bacsAccNumber", paymentCardDetails[0]);

      Util.printInfo("Entering Direct Debit BACS Sort Code : " + paymentCardDetails[0]);
      bicPage.populateField("bacsAccSortCode", paymentCardDetails[1]);

      bicPage.clickUsingLowLevelActions("bacsAgreementCheckbox1");
      bicPage.clickUsingLowLevelActions("bacsAgreementCheckbox2");

    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Direct Debit (BACS) details to make payment");
    }
  }

  @Step("Populate Sepa payment details")
  public void populateSepaPaymentDetails(String[] paymentCardDetails) {
    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 60000);

    try {
      Util.printInfo("Clicking on Sepa tab.");
      JavascriptExecutor js = (JavascriptExecutor) driver;
      String sepaTab = bicPage.getFirstFieldLocator("sepaPaymentTab");
      js.executeScript("arguments[0].click();", driver.findElement(By.xpath(sepaTab)));

      Util.printInfo("Waiting for Sepa header.");
      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("sepaHeader").get(0), 10);

      Util.printInfo("Entering IBAN number : " + paymentCardDetails[0]);
      bicPage.clickUsingLowLevelActions("sepaIbanNumber");
      bicPage.populateField("sepaIbanNumber", paymentCardDetails[0]);

      Util.printInfo("Entering SEPA profile name : " + paymentCardDetails[0]);
      bicPage.populateField("sepaProfileName", paymentCardDetails[1]);
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter SEPA payment information to make payment");
    }
    Util.sleep(20000);
  }

  @Step("Populate GiroPay payment details")
  public void populateGiroPayPaymentDetails(String[] paymentCardDetails,
      Map<String, String> address,
      Map<String, String> data) {
    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 30000);

    try {
      Util.printInfo("Clicking on GIROPAY tab.");
      bicPage.clickUsingLowLevelActions("giroPaymentTab");

      populateBillingAddress(address, data);
      Util.sleep(20000);

      bicPage.waitForFieldPresent(BICECEConstants.SUBMIT_ORDER_BUTTON, 20000);
      bicPage.clickUsingLowLevelActions(BICECEConstants.SUBMIT_ORDER_BUTTON);
      Util.sleep(40000);

      Util.printInfo("Entering Giropay bank name : " + paymentCardDetails[0]);
      bicPage.populateField("giroPayBankName", paymentCardDetails[0]);
      Util.sleep(2000);
      Util.printInfo("Selecting the bank name :");
      bicPage.clickUsingLowLevelActions("giroPayBankNameSelection");

      Util.printInfo("Clicking Continue ");
      bicPage.clickUsingLowLevelActions("giroPayContinue");

      Robot rb = new Robot();

      driver.manage().window().maximize();
      org.openqa.selenium.Dimension dimension = driver.manage().window().getSize();

      int x = (dimension.getWidth() / 2) + 20;
      int y = (dimension.getHeight() / 10) + 50;

      Util.sleep(2000);
      if (bicPage.checkIfElementExistsInPage("giroPayAssume", 10)) {
        Util.printInfo("Clicking on the assume button");
        bicPage.clickUsingLowLevelActions("giroPayAssume");
      }

      Util.sleep(4000);
      rb.mouseMove(x, y);
      rb.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      rb.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      Util.sleep(2000);
      rb.keyPress(KeyEvent.VK_ENTER);
      rb.keyRelease(KeyEvent.VK_ENTER);
      Util.sleep(10000);

      bicPage.populateField("giroPaySc", paymentCardDetails[1]);
      bicPage.populateField("giroPayScExtension", paymentCardDetails[2]);
      bicPage.populateField("giroPayCustomerName", paymentCardDetails[3]);
      bicPage.populateField("giroPayCustomerAban", paymentCardDetails[4]);
      bicPage.clickUsingLowLevelActions("giroPaySubmit");
    } catch (MetadataException | AWTException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter GIROPAY payment information to make payment");
    }
  }

  @Step("Populate Financing payment details")
  public void populateFinancingPaymentDetails(Map<String, String> address,
      Map<String, String> data) {
    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 30000);

    try {
      Util.printInfo("Clicking on Financing tab.");
      bicPage.clickUsingLowLevelActions("financingTab");

      populateBillingAddress(address, data);
      Util.sleep(20000);

      bicPage.waitForFieldPresent(BICECEConstants.SUBMIT_ORDER_BUTTON, 20000);
      bicPage.clickUsingLowLevelActions(BICECEConstants.SUBMIT_ORDER_BUTTON);

      Util.sleep(30000);

    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Financing  information to make payment");
    }
  }

  @Step("Add Paypal Payment Details")
  public void populatePaypalPaymentDetails(HashMap<String, String> data) {
    Util.printInfo("Switching to latest window...");
    String parentWindow = driver.getWindowHandle();

    try {
      Util.printInfo("Clicking on Paypal payments tab...");
      bicPage.clickUsingLowLevelActions("paypalPaymentTab");
      Util.printInfo("Clicking on Paypal checkout tab...");
      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("paypalPaymentHead").get(0), 10);
      Util.printInfo("Clicking on Paypal checkout frame...");

      bicPage.selectFrame("paypalCheckoutOptionFrame");
      Util.printInfo("Clicking on Paypal checkout button...");

      bicPage.clickUsingLowLevelActions("paypalCheckoutBtn");

      Set<String> windows = driver.getWindowHandles();
      for (String window : windows) {
        driver.switchTo().window(window);
      }

      driver.manage().window().maximize();
      bicPage.waitForPageToLoad();
      bicPage.waitForElementToDisappear("paypalPageLoader", 30);

      Util.sleep(10000);
      String title = driver.getTitle();

      AssertUtils.assertTrue(title.toUpperCase().contains("Log In".toUpperCase()),
          "Current title [" + title + "] does not contains keyword : PayPal Login");

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (bicPage.checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 10)) {
        bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
      }

      if (bicPage.checkIfElementExistsInPage(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON, 10)) {
        bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON);
      }

      Util.printInfo("Entering paypal user name [" + data.get("paypalUser") + "]...");
      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("paypalUsernameField").get(0), 10);

      bicPage.populateField("paypalUsernameField", data.get("paypalUser"));
      bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_NEXT_BUTTON);

      Util.printInfo("Entering paypal password...");
      bicPage.populateField("paypalPasswordField",
          ProtectedConfigFile.decrypt(data.get("paypalSsap")));

      Util.printInfo("Clicking on login button...");
      bicPage.clickUsingLowLevelActions("paypalLoginBtn");
      bicPage.waitForElementToDisappear("paypalPageLoader", 30);
      Util.sleep(5000);

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (bicPage.checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 10)) {
        bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
      }

      Util.printInfo("Selecting paypal payment option " + data.get("paypalPaymentType"));
      String paymentTypeXpath = bicPage.getFirstFieldLocator("paypalPaymentOption")
          .replace("<PAYMENTOPTION>",
              data.get("paypalPaymentType"));
      driver.findElement(By.xpath(paymentTypeXpath)).click();

      bicPage.executeJavascript("window.scrollBy(0,1000);");
      if (bicPage.checkFieldExistence("paypalContinueButton")) {
        bicPage.clickUsingLowLevelActions("paypalContinueButton");
      }

      driver.switchTo().window(parentWindow);

      Util.sleep(5000);
      Util.printInfo(
          "Paypal Payment success msg : " + bicPage.getTextFromLink("paypalPaymentConfirmation"));

      if (bicPage.checkIfElementExistsInPage("paypalPaymentConfirmation", 10)) {
        Util.printInfo("Paypal Payment is successfully added...");
      } else {
        AssertUtils.fail("Failed to add paypal payment profile...");
      }
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter paypal details to make payment...");
    }
    Util.sleep(20000);
  }

  @Step("Populate pay by invoice details")
  public void populatePayByInvoiceDetails(Map<String, String> payByInvoiceDetails, Map<String, String> address) {
    int count = 0;

    bicPage.waitForField("payByInvoiceButton", true, 30000);
    while (!bicPage.waitForField("payByInvoiceButton", true, 30000)) {
      Util.sleep(3000);
      count++;
      if (count > 3) {
        break;
      }
      if (count > 2) {
        driver.navigate().refresh();
      }
    }

    try {
      Util.printInfo("Clicking on pay By Invoice tab...");
      bicPage.clickUsingLowLevelActions("payByInvoiceButton");

      if (payByInvoiceDetails.containsKey(BICECEConstants.ORDER_NUMBER)) {
        if (!payByInvoiceDetails.get(BICECEConstants.ORDER_NUMBER).equals("")) {
          bicPage.clickUsingLowLevelActions("yesPurchaseOrderOption");
          Util.printInfo("Entering Purchase order number : " +payByInvoiceDetails.get("orderNumber") );
          bicPage.populateField("portalPurchaseOrder", payByInvoiceDetails.get("orderNumber"));
        }
      } else {
        bicPage.clickUsingLowLevelActions("noPurchaseOrderOption");
      }

      if (payByInvoiceDetails.containsKey(BICECEConstants.PURCHASE_ORDER_DOCUMENT_PATH)) {
        if (!payByInvoiceDetails.get(BICECEConstants.PURCHASE_ORDER_DOCUMENT_PATH).equals("")) {
          bicPage.populateField("portalPurchaseOrderDocument", payByInvoiceDetails.get(BICECEConstants.PURCHASE_ORDER_DOCUMENT_PATH));
        }
      }

      if (payByInvoiceDetails.containsKey(BICECEConstants.INVOICE_NOTES)) {
        if (!payByInvoiceDetails.get(BICECEConstants.INVOICE_NOTES).equals("")) {
          bicPage.clickUsingLowLevelActions("portalAddinvoiceLink");
          bicPage.waitForElementVisible(
                  bicPage.getMultipleWebElementsfromField("portalAddInvoiceNotesTextArea").get(0), 10);
          bicPage.populateField("portalAddInvoiceNotesTextArea", payByInvoiceDetails.get("invoiceNotes"));
        }
      }

      if (payByInvoiceDetails.containsKey(BICECEConstants.PAYER_EMAIL)) {
        bicPage.clickUsingLowLevelActions("payerSameAsCustomer");
        bicPage.waitForElementVisible(
            bicPage.getMultipleWebElementsfromField("cartEmailAddress").get(0), 10);
        bicPage.populateField("cartEmailAddress", payByInvoiceDetails.get(BICECEConstants.PAYER_EMAIL));

        enterCustomerDetails(address);
      }
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Pay By invoice payment details");
    }

    bicPage.click("reviewLOCOrder");
  }


  @Step("Click on Zip tab")
  public void populateZipPaymentDetails() {
    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 30000);
    try {
      Util.printInfo("Clicking on Zip tab.");
      bicPage.waitForFieldPresent("zipPaymentTab", 10000);
      bicPage.clickUsingLowLevelActions("zipPaymentTab");

    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Zip details to make payment...");
    }
  }

  @Step("Selecting payment profile" + GlobalConstants.TAG_TESTINGHUB)
  public void selectPaymentProfile(HashMap<String, String> data, String[] paymentCardDetails,
      Map<String, String> address) {
    try {

      Util.printInfo("Selecting payment profile : " + data.get(BICECEConstants.PAYMENT_TYPE));

      String[] paymentMethods = data.get(BICECEConstants.PAYMENT_METHODS).split(",");
      boolean isValidPaymentType = false;

      for (String paymentMethod : paymentMethods) {
        if (paymentMethod.equals(data.get(BICECEConstants.PAYMENT_TYPE))) {
          isValidPaymentType = true;
          break;
        }
      }
      if (isValidPaymentType) {
        switch (data.get(BICECEConstants.PAYMENT_TYPE).toUpperCase()) {
          case BICConstants.paymentTypePayPal:
            populatePaypalPaymentDetails(data);
            break;
          case BICConstants.paymentTypeDebitCard:
            populateACHPaymentDetails(paymentCardDetails);
            break;
          case BICECEConstants.PAYMENT_BACS:
            populateBACSPaymentDetails(paymentCardDetails, address, data);
            data.put(BICECEConstants.BILLING_DETAILS_ADDED, BICECEConstants.TRUE);
            break;
          case BICECEConstants.PAYMENT_TYPE_SEPA:
            populateSepaPaymentDetails(paymentCardDetails);
            break;
          case BICECEConstants.PAYMENT_TYPE_GIROPAY:
            populateGiroPayPaymentDetails(paymentCardDetails, address, data);
            data.put(BICECEConstants.BILLING_DETAILS_ADDED, BICECEConstants.TRUE);
            break;
          case BICECEConstants.PAYMENT_TYPE_FINANCING:
            populateFinancingPaymentDetails(address, data);
            data.put(BICECEConstants.BILLING_DETAILS_ADDED, BICECEConstants.TRUE);
            break;
          case BICECEConstants.PAYMENT_TYPE_ZIP:
            populateZipPaymentDetails();
            break;
          case BICECEConstants.LOC:
            populatePayByInvoiceDetails(data, address);
            break;
          default:
            populatePaymentDetails(paymentCardDetails);
            break;
        }
      } else {
        AssertUtils.fail(
            "The payment method is not supported for the given country/locale : " + data
                .get(BICECEConstants.LOCALE) + ". Supported payment methods are "
                + data.get(BICECEConstants.PAYMENT_METHODS));
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to select payment profile...");
    }
  }

  @Step("Submit Order on Checkout page")
  public void submitOrder(HashMap<String, String> data) {
    // Check if tax amount calculated properly
    checkIfTaxValueIsCorrect(data);

    // Get total order value from checkout page
    String orderTotalCheckout = driver
        .findElement(By.xpath("//*[@data-testid='checkout--order-summary-section--total']")).getText();
    data.put("orderTotalCheckout", orderTotalCheckout);

    clickMandateAgreementCheckbox();
    int count = 0;
    debugPageUrl("Step 1: Wait for submit order button.");
    while (!bicPage.waitForField(BICECEConstants.SUBMIT_ORDER_BUTTON, true, 60000)) {
      Util.sleep(20000);
      count++;
      if (count > 3) {
        break;
      }
      if (count > 2) {
        driver.navigate().refresh();
      }
    }

    debugPageUrl("Step 2: Wait for submit order button.");
    try {
      int countModal = 0;
      while (driver.findElement(By.xpath("//*[text()='CONTINUE CHECKOUT']")).isDisplayed()) {
        Util.printInfo(" CONTINUE CHECKOUT Modal is present");
        driver.findElement(By.xpath("//*[text()='CONTINUE CHECKOUT']")).click();
        Util.sleep(5000);
        countModal++;
        if (countModal > 3) {
          AssertUtils.fail("Unexpected Pop up in the cart - Please contact TestingHub");
          break;
        }
      }
    } catch (Exception e) {
      Util.printInfo("CONTINUE_CHECKOUT_Modal is not present");
    }

    // Zip Pay Verification
    if (data.get(BICECEConstants.PAYMENT_TYPE).equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_ZIP)) {
      String amountDueXPath = bicPage.getFirstFieldLocator("guacAmountTotal");
      WebElement amountDueElement = driver.findElement(By.xpath(amountDueXPath));
      zipTestBase.setTestData(data);
      zipTestBase.verifyZipBalance(amountDueElement.getText());
    }

    try {
      if (bicPage.checkIfElementExistsInPage(BICECEConstants.SUBMIT_ORDER_BUTTON, 10)) {
        bicPage.clickUsingLowLevelActions(BICECEConstants.SUBMIT_ORDER_BUTTON);
      }
    } catch (Exception e) {
      e.printStackTrace();
      debugPageUrl(e.getMessage());
      AssertUtils.fail("Failed to click on Submit button.");
    }

    // Zip Pay Checkout
    if (data.get(BICECEConstants.PAYMENT_TYPE).equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_ZIP)) {
      zipTestBase.setTestData(data);
      zipTestBase.zipPayCheckout();
    }

    debugPageUrl("Step 3: Check order number is Null");
    bicPage.waitForPageToLoad();

    try {
      if (driver.findElement(By.xpath("//*[(text()='Order Processing Problem')]")).isDisplayed()) {
        Util.printInfo("Order Processing Problem");
      }
      AssertUtils.fail("Unable to place BIC order : " + "Order Processing Problem");
    } catch (Exception e) {
      Util.printInfo("Great! Export Compliance issue is not present");
    }
  }

  @Step("Retrieving Order Number")
  public String getOrderNumber(HashMap<String, String> data) {
    String orderNumber = null;

    try {
      debugPageUrl("Step 1: Get Purchase Order number for Confirmation page");
      orderNumber = driver.findElement(By.xpath(
                      "//*[@data-testid='checkout--order-confirmation--invoice-details--order-number']"))
              .getText();
    } catch (Exception e) {
      debugPageUrl("Step 2: Check order number is Null");
    }

    if (orderNumber == null) {
      try {
        if (driver.findElement(By.xpath(
                        "//*[@class='checkout--order-confirmation--invoice-details--export-compliance--label wd-uppercase']"))
                .isDisplayed()) {
          Util.printWarning(
                  "Export compliance issue is present. Checking for order number in the Pelican response");
          JavascriptExecutor executor = (JavascriptExecutor) driver;
          String response = (String) executor
                  .executeScript("return sessionStorage.getItem('purchase')");
          JSONObject jsonObject = JsonParser.getJsonObjectFromJsonString(response);
          JSONObject purchaseOrder = (JSONObject) jsonObject.get("purchaseOrder");
          orderNumber = purchaseOrder.get("id").toString();
          if (orderNumber != null && !orderNumber.isEmpty()) {
            Util.printInfo("Yay! Found the Order Number. Proceeding to next steps...");
          }
        }
      } catch (Exception e) {
        Util.printMessage("Great! Export Compliance issue is not present.");
      }
    }

    if (orderNumber == null) {
      try {
        orderNumber = driver.findElement(By.xpath(BICECEConstants.JP_ORDER_NUMBER)).getText();
      } catch (Exception e) {
        debugPageUrl("Step 3: Check order number is Null for JP");
      }
    }

    if (orderNumber == null) {
      debugPageUrl("Step 3a: Check order number is Null");
      bicPage.waitForPageToLoad();
      try {
        orderNumber = driver.findElement(By.xpath(
                "//*[@data-testid='checkout--order-confirmation--invoice-details--order-number']"))
            .getText();
      } catch (Exception e) {
        debugPageUrl("Step 4: Check order number is Null");
      }
    }

    validateBicOrderNumber(orderNumber);

    if (!System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
      Util.printInfo("Asserting that order total equals the total amount from checkout page.");
      String orderTotal = driver
          .findElement(By.xpath("//p[@data-testid='checkout--order-confirmation--invoice-details--order-total']"))
          .getText();

      orderTotal = orderTotal.replaceAll("[^0-9]", "");
      String orderTotalCheckout = data.get("orderTotalCheckout").replaceAll("[^0-9]", "");
      Util.printInfo("The total amount in Checkout page :" + Double.valueOf(orderTotalCheckout) / 100);
      Util.printInfo("The total amount in Confirmation page :" + Double.valueOf(orderTotal) / 100);

      data.put(BICECEConstants.FINAL_TAX_AMOUNT, orderTotal);
      AssertUtils.assertTrue(orderTotal.equals(orderTotalCheckout),
          "The checkout page total and confirmation page total do not match.");

    }
    else {
      String orderTotal = driver
              .findElement(By.xpath("//p[@data-testid='checkout--order-confirmation--invoice-details--order-total']"))
              .getText();
      orderTotal = orderTotal.replaceAll("[^0-9]", "");
      data.put(BICECEConstants.FINAL_TAX_AMOUNT, orderTotal);
    }

    return orderNumber;
  }

  public void printConsole(String OrderNumber, LinkedHashMap<String, String> data, Map<String, String> address) {
    Util.printInfo("*************************************************************" + "\n");
    Util.printAssertingMessage("Email Id for the account :: " + data.get(BICECEConstants.emailid));
    Util.printAssertingMessage("First name of the account :: " + data.get(BICECEConstants.FIRSTNAME));
    Util.printAssertingMessage("Last name of the account  :: " + data.get(BICECEConstants.LASTNAME));
    Util.printAssertingMessage("Address used to place order :: " + address);
    Util.printAssertingMessage("paymentMethod used to place order :: " + System.getProperty(BICECEConstants.PAYMENT));
    Util.printAssertingMessage("Order placed successfully :: " + OrderNumber + "\n");
    Util.printInfo("*************************************************************");
  }

  public String navigateToCart(LinkedHashMap<String, String> data) throws MetadataException {
    String productType = data.get("productType");
    String constructGuacURL;
    String priceId = null;

    // Starting from dot com page for STG or INT environment
    if (System.getProperty(BICECEConstants.ENVIRONMENT).equalsIgnoreCase(BICECEConstants.ENV_STG) ||
        System.getProperty(BICECEConstants.ENVIRONMENT).equalsIgnoreCase(BICECEConstants.ENV_INT)) {
      navigateToDotComPage(data);

      // Selecting monthly for Non-Flex, Non-Financing
      if (productType.equals("flex")) {
        bicPage.waitForFieldPresent("flexTab", 5000);
        bicPage.clickUsingLowLevelActions("flexTab");

        bicPage.waitForFieldPresent("buyTokensButton", 5000);
        Util.sleep(3000);

        closeGetHelpPopup();

        try {
          bicPage.clickUsingLowLevelActions("buyTokensButton");
        } catch (WebDriverException e) {
          Util.printInfo(e.getMessage());
        }
      } else {
        if (System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
          selectYearlySubscription(driver);
        } else {
          selectMonthlySubscription(driver);
        }
        constructGuacURL = subscribeAndAddToCart();
        priceId = StringUtils.substringBetween(constructGuacURL, "priceIds=", "&");
      }
    } else {
      Assert.fail("Environment is neither STG or INT in Maven parameter.");
    }

    setStorageData();

    return priceId;
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place GUAC Dot Com Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createGUACBICOrderDotCom(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String term = "";
    String quantity = "";
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);

    String orderNumber = createBICOrderDotCom(data, false);

    if (data.get(BICECEConstants.PAYMENT_TYPE)
        .equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      financingTestBase.setTestData(data);
      financingTestBase.completeFinancingApplication();
    }

    results.put(BICConstants.emailid, data.get(BICECEConstants.emailid));
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place GUAC Multi Line item Dot Com Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createGUACBICMultilineItemOrderDotCom(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String term = "";
    String quantity = "";
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);
    String password = ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD));
    String promoCode1 = data.get(BICECEConstants.PROMO_CODE);

    String emailID = generateUniqueEmailID();
    data.put(BICECEConstants.emailid, emailID);

    String priceId = navigateToCart(data);

    Names names = generateFirstAndLastNames();
    createBICAccount(names, emailID, password, false);
    data.putAll(names.getMap());

    updateQuantity(priceId, BICECEConstants.MULTI_LINE_ITEM_QUANTITY_2);
    driver.switchTo().newWindow(WindowType.TAB);

    data.put(BICECEConstants.QUANTITY, BICECEConstants.MULTI_LINE_ITEM_QUANTITY_1);
    data.put(BICECEConstants.PRODUCT_NAME, data.get(BICECEConstants.PRODUCT_NAME_2));
    data.put(BICECEConstants.PRICE_ID, data.get(BICECEConstants.PRICE_ID_2));

    String orderNumber = createBICOrderDotCom(data, true);

    if (data.get(BICECEConstants.PAYMENT_TYPE)
        .equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      financingTestBase.setTestData(data);
      financingTestBase.completeFinancingApplication();
    }

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @Step("Quote2Order: Navigate to quote checkout " + GlobalConstants.TAG_TESTINGHUB)
  public void navigateToQuoteCheckout(LinkedHashMap<String, String> data) {
    String language = "?lang=" + data.get(BICECEConstants.LOCALE).substring(0, 2);
    String country = "&country=" + data.get(BICECEConstants.LOCALE).substring(3);
    String currency = "&currency=" + data.get(BICECEConstants.currencyStore);
    String url = data.get("Quote2OrderBaseURL") + data.get(BICECEConstants.QUOTE_ID) + language + country
        + currency;

    Util.printInfo("Quote URL: " + url);

    data.put("checkoutUrl", url);

    getUrl(url);
    setStorageData();
  }

  @Step("Placing the Flex Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> placeFlexOrder(LinkedHashMap<String, String> data) throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String orderNumber = null;

    Map<String, String> address = getBillingAddress(data);

    if (data.get("isNonQuoteFlexOrder") != null) {
      enterCustomerDetails(address);
      data.put(BICECEConstants.BILLING_DETAILS_ADDED, BICECEConstants.TRUE);
    } else {
      populateTaxIdForFlex();
    }

    if (bicPage.checkFieldExistence("customerDetailsContinue")) {
      bicPage.waitForFieldPresent("customerDetailsContinue", 10000);
      Util.sleep(5000);
      bicPage.clickUsingLowLevelActions("customerDetailsContinue");
    }

    if (bicPage.checkFieldExistence("customerDetailsContinue2")) {
      bicPage.waitForFieldPresent("customerDetailsContinue2", 10000);
      Util.sleep(5000);
      bicPage.clickUsingLowLevelActions("customerDetailsContinue2");
    }

    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);

    if (data.get("isReturningUser") == null) {
      enterBillingDetails(data, address, paymentMethod);
    }

    if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
        submitOrder(data);
      }
      orderNumber = getOrderNumber(data);

      printConsole(orderNumber, data, address);
    }

    results.put(BICConstants.emailid, data.get(BICConstants.emailid));
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Dot Com: Estimate price via Flex Token Estimator tool " + GlobalConstants.TAG_TESTINGHUB)
  public void estimateFlexTokenPrice(LinkedHashMap<String, String> data) throws MetadataException {
    String flexTokensEstimatorUrl =
        data.get("guacDotComBaseURL") + data.get(BICECEConstants.COUNTRY_DOMAIN) + data.get("flexBenefitsPath");
    getUrl(flexTokensEstimatorUrl);

    if (bicPage.checkIfElementExistsInPage("estimatorAcceptCookies", 5)) {
      bicPage.click("estimatorAcceptCookies");
    }

    Util.printInfo("Click on 'Estimate tokens needed' button");
    bicPage.waitForFieldPresent("estimateTokensButton", 5000);
    bicPage.clickUsingLowLevelActions("estimateTokensButton");
    bicPage.clickUsingLowLevelActions("estimatorProductsDropdown");
    bicPage.clickUsingLowLevelActions("estimator3dsmaxProduct");
    bicPage.clickUsingLowLevelActions("estimatorAutocadProduct");
    bicPage.clickUsingLowLevelActions("productsDropDownClose");

    Util.printInfo("Making sure that we can see selected products on the page");
    List<WebElement> adskProducts = driver.findElements(By.xpath("//div[@class=\"fe-tablerow-product-name\"]"));
    AssertUtils.assertEquals("3ds Max is present on the page", adskProducts.get(0).getText(), "3ds Max");
    AssertUtils.assertTrue("AutoCAD is present on the page", adskProducts.get(1).getText().contains("AutoCAD"));

    Util.printInfo("Updating users and days");
    List<WebElement> usersInputPaths = driver.findElements(By.xpath("//div[@data-testid=\"fe-users-input\"]/input"));
    List<WebElement> daysInputPaths = driver.findElements(By.xpath("//div[@data-testid=\"fe-days-input\"]/input"));
    updateEstimatorToolInput(usersInputPaths.get(0), 4);
    updateEstimatorToolInput(daysInputPaths.get(0), 7);
    updateEstimatorToolInput(usersInputPaths.get(1), 2);
    updateEstimatorToolInput(daysInputPaths.get(1), 7);

    bicPage.waitForPageToLoad();

    String recommendedTokens = driver
        .findElement(By.xpath("//*[@data-testid=\"fe-summary-tokens\"]/div[@class=\"fe-rec-totals-fadeIn\"]"))
        .getText();

    String estimatedPrice = driver
        .findElement(By.xpath("//*[@data-testid=\"fe-summary-price\"]/div[@class=\"fe-rec-totals-fadeIn\"]")).getText();
    estimatedPrice = estimatedPrice.replaceAll("[^0-9 .]", "");
    double estimatedPriceDouble = Double.parseDouble(estimatedPrice);

    Util.printInfo("Clicking on Buy tokens button");
    bicPage.waitForFieldPresent("buyTokensButtonFlex", 5000);
    try {
      bicPage.clickUsingLowLevelActions("buyTokensButtonFlex");
    } catch (Exception e) {
      // Catching exception to continue the test
    }

    setStorageData();

    signInIframe(data);

    Util.printInfo("Asserting that estimated amounts match amounts on Checkout page");
    int tokensQuantity = Integer.parseInt(driver
        .findElement(
            By.xpath("//input[@id=\"quantity\"]")).getAttribute("value"));
    AssertUtils
        .assertEquals("Estimated tokens amount should match total amount of tokens on Checkout page",
            tokensQuantity, Integer.parseInt(recommendedTokens.substring(0, 4))
        );

    String totalCostOrderSummary = driver
        .findElement(By.xpath("//h3[@data-testid=\"checkout--order-summary-section--total\"]")).getText();
    totalCostOrderSummary = totalCostOrderSummary.replaceAll("[^0-9 .]", "");
    double totalCostOrderSummaryDouble = Math.round(Double.parseDouble(totalCostOrderSummary));

    AssertUtils.assertEquals("Estimated total price should match total price on Checkout page",
        (int) totalCostOrderSummaryDouble,
        (int) estimatedPriceDouble);
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Dot Com: Navigate to Flex Cart from DotCom " + GlobalConstants.TAG_TESTINGHUB)
  public void navigateToFlexCartFromDotCom(LinkedHashMap<String, String> data) throws MetadataException {
    String priceId = navigateToCart(data);

    // Sign in
    if (data.get("isReturningUser") == null) {
      signInIframe(data);
    } else {
      boolean isLoggedIn = bicPage.checkIfElementExistsInPage("signInSectionUserProfile", 20);
      if (!isLoggedIn) {
        loginAccount(data);
      }
    }

    if ((!data.get("productType").equals("flex")) && data.containsKey(BICECEConstants.QUANTITY)) {
      updateQuantity(priceId, data.get(BICECEConstants.QUANTITY));
    }
  }

  private void signInIframe(LinkedHashMap<String, String> data) {
    Util.printInfo("Signing to iframe");
    Names names = generateFirstAndLastNames();
    String emailID = generateUniqueEmailID();
    data.put(BICECEConstants.emailid, emailID);
    createBICAccount(names, data.get(BICECEConstants.emailid),
        ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD)), false);
    data.putAll(names.getMap());
  }

  private void updateEstimatorToolInput(WebElement webElement, int input) {
    webElement.click();
    Util.sleep(2500);
    webElement.sendKeys(Keys.BACK_SPACE);
    Util.sleep(2500);
    webElement.sendKeys(String.valueOf(input));
  }

  private void updateQuantity(String priceId, String quantity) {
    String paymentTypeXpath = bicPage.getFirstFieldLocator("cartQuantity").replace("<PRICEID>", priceId);
    clearTextInputValue(driver.findElement(By.xpath(paymentTypeXpath)));
    driver.findElement(By.xpath(paymentTypeXpath)).sendKeys(quantity);
    waitForLoadingSpinnerToComplete("loadingSpinner");
  }

  public void setStorageData() {
    try {
      JavascriptExecutor js = (JavascriptExecutor) driver;

      Util.printInfo("Cookie: add 'OPTOUTMULTI_TYPE=A'");
      js.executeScript("document.cookie=\"OPTOUTMULTI_TYPE=A\";");

      Util.printInfo("Local Storage: set 'usi_launched' to true.");
      js.executeScript("window.localStorage.setItem(\"usi_launched\",\"true\");");

      Util.printInfo("Session Storage: set 'nonsensitiveHasNonLocalModalLaunched' to true.");
      js.executeScript("window.sessionStorage.setItem(\"nonsensitiveHasNonLocalModalLaunched\",\"true\");");

      driver.navigate().refresh();
    } catch (Exception e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
  }

  private void clickMandateAgreementCheckbox() {
    try {
      if (System.getProperty(BICECEConstants.PAYMENT)
          .equalsIgnoreCase(BICConstants.paymentTypeDebitCard) || System
          .getProperty(BICECEConstants.PAYMENT)
          .equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_SEPA)) {
        Util.printInfo(
            BICECEConstants.CHECKED_MANDATE_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.MANDATE_CHECKBOX_HEADER));
        Util.printInfo(BICECEConstants.CHECKED_BOX_STATUS_FOR_MANDATE_CHECKBOX + bicPage.isChecked(
            BICECEConstants.MANDATE_AGREEMENT_CHECKBOX));

        List<WebElement> mandateAgreementElement = driver.findElements(By.xpath(
            BICECEConstants.ID_MANDATE_AGREEMENT));

        Util.printInfo(
            BICECEConstants.CHECKED_MANDATE_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.MANDATE_CHECKBOX_HEADER));

        if (mandateAgreementElement.size() > 1) {
          mandateAgreementElement.get(1).click();
          Util.printInfo(
              BICECEConstants.CHECKED_BOX_STATUS_FOR_MANDATE_CHECKBOX + mandateAgreementElement.get(1)
                  .isEnabled());

        } else {
          mandateAgreementElement.get(0).click();
          Util.printInfo(
              BICECEConstants.CHECKED_BOX_STATUS_FOR_MANDATE_CHECKBOX + mandateAgreementElement.get(0)
                  .isEnabled());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String createBICOrderDotCom(LinkedHashMap<String, String> data, Boolean isLoggedIn) throws MetadataException {
    String orderNumber = null;
    Map<String, String> address = null;
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String priceId = navigateToCart(data);
    address = getBillingAddress(data);

    if (!(isLoggedIn)) {
      signInIframe(data);
    }

    if ((!data.get("productType").equals("flex")) && data.containsKey(BICECEConstants.QUANTITY)) {
      updateQuantity(priceId, data.get(BICECEConstants.QUANTITY));
    }

    String promoCode = data.get(BICECEConstants.PROMO_CODE);
    // Apply promo if exists
    if (promoCode != null && !promoCode.isEmpty()) {
      populatePromoCode(promoCode, data);
    }

    enterBillingDetails(data, address, paymentMethod);

    if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
        submitOrder(data);
      }
      orderNumber = getOrderNumber(data);

      printConsole(orderNumber, data, address);
    }

    return orderNumber;
  }

  public Map<String, String> getBillingAddress(LinkedHashMap<String, String> data) {
    String billingAddress;
    String addressViaParam = System.getProperty(BICECEConstants.ADDRESS);
    if (addressViaParam != null && !addressViaParam.isEmpty()) {
      Util.printInfo("The address is passed as parameter : " + addressViaParam);
      billingAddress = addressViaParam;
    } else {
      billingAddress = data.get(BICECEConstants.ADDRESS);
    }
    return getBillingAddress(billingAddress);
  }

  public void enterBillingDetails(LinkedHashMap<String, String> data,
      Map<String, String> address, String paymentMethod) {
    String[] paymentCardDetails = getCardPaymentDetails(paymentMethod);
    selectPaymentProfile(data, paymentCardDetails, address);

    // handling the chat popup
    try {
      Util.printInfo("Checking if Chat Popup Present");
      if (bicPage.checkIfElementExistsInPage("chatHelpPopupButton", 10)) {
        Util.printInfo("Clicking on the chatHelpPopupButton button");
        bicPage.clickUsingLowLevelActions("chatHelpPopupButton");
      }
    } catch (Exception e) {
      Util.printInfo("No Chat Popup found. Continuing...");
    }
    Util.printInfo("Checking if Chat Popup Present. Done");

    if (null != data.get(BICECEConstants.QUOTE_ID) && !paymentMethod.equalsIgnoreCase(BICECEConstants.PAYPAL)
        && !paymentMethod.equalsIgnoreCase(BICECEConstants.LOC) ) {
      clickOnContinueBtn(System.getProperty(BICECEConstants.PAYMENT));
    } else if (data.get("isNonQuoteFlexOrder") != null &&
        data.get(BICECEConstants.BILLING_DETAILS_ADDED).equalsIgnoreCase(BICECEConstants.TRUE) &&
        data.get(BICECEConstants.USER_TYPE).equalsIgnoreCase("newUser") && !paymentMethod.equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
      clickOnContinueBtn(System.getProperty(BICECEConstants.PAYMENT));
    } else if ((data.get(BICECEConstants.BILLING_DETAILS_ADDED) == null || !data
        .get(BICECEConstants.BILLING_DETAILS_ADDED).equals(BICECEConstants.TRUE))
        && !paymentMethod.equalsIgnoreCase(BICECEConstants.LOC)) {
      debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
      populateBillingAddress(address, data);
      debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }
  }

  public void enterCustomerDetails(Map<String, String> address)
      throws MetadataException {

    bicPage.waitForFieldPresent("companyNameField", 10000);
    bicPage.populateField("companyNameField", address.get(BICECEConstants.ORGANIZATION_NAME));

    if (bicPage.checkIfElementExistsInPage("selectCountryField", 10)) {
      bicPage.clickUsingLowLevelActions("selectCountryField");
      String selectCountryOption = bicPage.getFirstFieldLocator("selectCountryOption")
          .replace("<COUNTRY>", System.getProperty(BICECEConstants.LOCALE).substring(3));
      driver.findElement(By.xpath(selectCountryOption)).click();
    }

    if (bicPage.checkIfElementExistsInPage("address1Field", 10)) {
      bicPage.populateField("address1Field", address.get(BICECEConstants.FULL_ADDRESS));
    } else {
      bicPage.populateField("addressAutocomplete", address.get(BICECEConstants.FULL_ADDRESS));
    }
    Util.sleep(2000);

    bicPage.populateField("cityField", address.get(BICECEConstants.CITY));
    Util.sleep(2000);

    if (bicPage.checkIfElementExistsInPage("selectStateField", 10)) {
      bicPage.clickUsingLowLevelActions("selectStateField");
      String selectStateOption = bicPage.getFirstFieldLocator("selectStateOption")
          .replace("<STATE_PROVINCE>", address.get(BICECEConstants.STATE_PROVINCE));
      driver.findElement(By.xpath(selectStateOption)).click();
    }
    Util.sleep(2000);

    if (bicPage.checkIfElementExistsInPage("postalCodeField", 10)) {
      bicPage.waitForFieldPresent("postalCodeField", 5000);
      bicPage.populateField("postalCodeField", address.get(BICECEConstants.ZIPCODE));
    }
    Util.sleep(2000);

    bicPage.populateField("phoneNumberField", address.get(BICECEConstants.PHONE_NUMBER));

    populateTaxIdForFlex();

    if (bicPage.checkIfElementExistsInPage("customerDetailsContinue", 10)) {
      bicPage.waitForFieldPresent("customerDetailsContinue", 10000);
      Util.printInfo("Clicking on Continue in Customer Details section.");
      bicPage.clickUsingLowLevelActions("customerDetailsContinue");
      Util.sleep(2000);
    }

    Util.sleep(5000);

    if (bicPage.checkIfElementExistsInPage("confirmCustomerAddressAlert", 10)) {
      if (bicPage.checkIfElementExistsInPage("customerDetailsAddress", 10)) {
        Util.printInfo("Two or more suggested addresses. Clicking on radio button to choose one.");
        bicPage.clickUsingLowLevelActions("customerDetailsAddress");
      }

      if (bicPage.checkIfElementExistsInPage("customerDetailsContinue2", 10)) {
        Util.printInfo("Address confirmation requested, Clicking on Continue2 button.");
        bicPage.clickUsingLowLevelActions("customerDetailsContinue2");
        waitForLoadingSpinnerToComplete("loadingSpinner");
      }
    }

    Util.sleep(5000);

    boolean isCustomerDetailsComplete = bicPage.waitForFieldPresent("customerDetailsComplete", 1000);
    if (isCustomerDetailsComplete) {
      Util.printInfo("Customer details address saved successfully!");
    } else {
      AssertUtils.fail("Customer details section is still open. Could not save the address.");
    }
  }

  private void populatePromoCode(String promoCode, LinkedHashMap<String, String> data) {
    String priceBeforePromo = null;
    String priceAfterPromo = null;

    try {
      if (driver.findElement(By.xpath("//h2[contains(text(),\"just have a question\")]"))
          .isDisplayed()) {
        bicPage.clickUsingLowLevelActions("promoCodePopUpThanksButton");
      }

      priceBeforePromo = bicPage.getValueFromGUI("promoCodeBeforeDiscountPrice").trim();
      Util.printInfo("Step : Entering promo code " + promoCode + "\n" + " priceBeforePromo : "
          + priceBeforePromo);

      driver.findElement(By.linkText("Promotion code")).click();
      bicPage.waitForFieldPresent("promoCodeInput", 10000);
      bicPage.clickUsingLowLevelActions("promoCodeInput");
      bicPage.populateField("promoCodeInput", promoCode);
      bicPage.clickUsingLowLevelActions("promoCodeSubmit");
      waitForLoadingSpinnerToComplete("loadingSpinner");
      priceAfterPromo = bicPage.getValueFromGUI("promoCodeAfterDiscountPrice").trim();

      Util.printInfo("----------------------------------------------------------------------");
      Util.printInfo(
          "\n" + " priceBeforePromo :  " + priceBeforePromo + "\n" + " priceAfterPromo : "
              + priceAfterPromo);
      Util.printInfo("----------------------------------------------------------------------");

    } catch (Exception e) {
      Util.printTestFailedMessage(
          "Unable to enter the promo code : " + promoCode + "\n" + e.getMessage());
    } finally {
      if (priceAfterPromo.equalsIgnoreCase(priceBeforePromo)) {
        AssertUtils
            .fail("Even after applying the promo code, there is not change in the Pricing" + "\n"
                + "priceBeforePromo :  " + priceBeforePromo + "\n"
                + "priceAfterPromo : " + priceAfterPromo);
      } else {
        data.put("priceBeforePromo", priceBeforePromo);
        data.put("priceAfterPromo", priceAfterPromo);
      }
    }
  }

  @Step("Assert that tax value matches the tax parameter.")
  private void checkIfTaxValueIsCorrect(HashMap<String, String> data) {
    Util.sleep(5000);
    String nonZeroTaxState = data.get("taxOptionEnabled");
    if (nonZeroTaxState.equals("undefined")) {
      return;
    }

    String taxValue = null;
    if (data.get("productType").equals("flex")) {

      bicPage.waitForField("orderSummarySection", true, 30000);
      Util.printInfo("Scroll to order summary section");
      JavascriptExecutor js = (JavascriptExecutor) driver;
      WebElement Element = driver.findElement(
          By.xpath(bicPage.getFirstFieldLocator("orderSummarySection")));
      js.executeScript("arguments[0].scrollIntoView();", Element);
      Util.sleep(5000);

      try {
        if (bicPage.checkIfElementExistsInPage("orderSummaryTax", 60)) {
          Util.printInfo("Flex tax condition for Tax/GST");
          taxValue = driver.findElement(
              By.xpath(bicPage.getFirstFieldLocator("orderSummaryTax"))).getText();
        } else if (bicPage.checkIfElementExistsInPage("orderSummaryVat", 60)) {
          Util.printInfo("Flex tax condition for Vat");
          taxValue = driver.findElement(
              By.xpath(bicPage.getFirstFieldLocator("orderSummaryVat"))).getText();
        } else {
          Util.printInfo("Flex tax condition - default");
          taxValue = driver
              .findElement(
                  By.xpath(
                      "//div[@class='checkout--order-summary-section--products-total']/div[2]/p[2][@data-pricing-source='C']"))
              .getText();
        }
      } catch (MetadataException e) {
        e.printStackTrace();
        Assert.fail("Can not find tax in Order Summary section");
      }
    } else {
      Util.printInfo("Tax condition for non Flex product");
      taxValue = driver
          .findElement(By.xpath("//p[@data-testid='checkout--cart-section--tax'][@data-pricing-source='PQ']"))
          .getText();
    }

    taxValue = taxValue.replaceAll("[^0-9]", "");
    double taxValueAmount = Double.parseDouble(taxValue);
    data.put(BICECEConstants.FINAL_TAX_AMOUNT, String.valueOf(taxValueAmount));
    Util.printInfo("The final Tax Amount : " + taxValueAmount);
    if (nonZeroTaxState.equals("Y")) {
      Util.printInfo("This state collects tax.");
      AssertUtils.assertTrue(taxValueAmount / 100 > 0, "Tax value is greater than zero");
    } else if (nonZeroTaxState.equals("N")) {
      Util.printInfo("This state does not collect tax.");
      AssertUtils.assertEquals(taxValueAmount / 100, 0.00, "Tax value is equal to zero");
    } else {
      Util.printInfo("Entered isTaxed value is not valid. Can not assert if tax is displayed properly. Should be Y/N.");
    }
  }

  private void validateBicOrderNumber(String orderNumber) {
    if (orderNumber != null) {
      Util.printInfo("Order No: " + orderNumber);
      if (!((orderNumber.equalsIgnoreCase("EXPORT COMPLIANCE")) || (orderNumber
          .equalsIgnoreCase("輸出コンプライアンス")))
          || (orderNumber.equalsIgnoreCase("null"))) {
        orderNumber = orderNumber.trim();
      } else {
        Util.printTestFailedMessage(" Cart order " + orderNumber);
        AssertUtils.fail("Cart order " + orderNumber);
      }
    } else {
      AssertUtils.fail("Can not place the order at the moment. Please contact #bic_estore.");
    }
  }

  @SuppressWarnings("unused")
  @Step("Create BIC Existing User Order Creation via Cart " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBICReturningUser(LinkedHashMap<String, String> data) throws MetadataException {
    String orderNumber;
    HashMap<String, String> results = new HashMap<>();
    String paymentMethod = data.get("paymentMethod");

    navigateToCart(data);

    // Login to an existing account
    loginAccount(data);

    skipAddSeats();

    // If the submit button is disabled, fill the payment information out again
    List<WebElement> submitButton = driver.findElements(By.cssSelector(
        "[data-testid=\"order-summary-section\"] .checkout--order-summary-section--submit-order  .checkout--order-summary-section--submit-order--button-container button"));
    if (submitButton.size() > 0 && !submitButton.get(0).isEnabled()) {
      Map<String, String> address = getBillingAddress(data.get(BICECEConstants.ADDRESS));
      enterBillingDetails(data, address, paymentMethod);
    }

    if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
      submitOrder(data);
    }
    orderNumber = getOrderNumber(data);
    Util.printInfo(BICECEConstants.ORDER_NUMBER + orderNumber);

    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  /**
   * Place an order in guac with a driver instance that is already logged into an account
   *
   * @param data - Testing data
   * @return - results data
   */
  @Step("Create BIC Order for logged in user" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBICReturningUserLoggedIn(
      LinkedHashMap<String, String> data) throws MetadataException {
    String orderNumber;
    HashMap<String, String> results = new HashMap<>();

    navigateToCart(data);

    // Login to an existing account
    loginAccount(data);

    skipAddSeats();

    if (!System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
      submitOrder(data);
    }
    orderNumber = getOrderNumber(data);
    Util.printInfo(BICECEConstants.ORDER_NUMBER + orderNumber);

    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @Step("Create BIC Existing User Order Creation via Cart and add seat"
      + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBic_ReturningUserAddSeat(
      LinkedHashMap<String, String> data) throws MetadataException {
    String orderNumber;
    HashMap<String, String> results = new HashMap<>();

    // Go to checkout with a product that was already added
    navigateToCart(data);

    // Login to an existing account and add seats
    loginAccount(data);

    Util.printInfo("Waiting for Add seats modal.");
    Util.sleep(5000);
    existingSubscriptionAddSeat(data);
    Util.printInfo("Successfully added seats.");

    if (!System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
      submitOrder(data);
    }
    orderNumber = getOrderNumber(data);
    Util.printInfo(BICECEConstants.ORDER_NUMBER + orderNumber);

    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  public void getUrl(String URL) {
    try {
      driver.manage().deleteAllCookies();
      driver.get(URL);
      bicPage.waitForPageToLoad();
    } catch (Exception e) {
      try {
        retryLoadingURL(URL);
        bicPage.waitForPageToLoad();
      } catch (Exception e1) {
        AssertUtils.fail("Failed to load and get url :: " + URL);
      }
    }
  }

  public void retryLoadingURL(String URL) {
    int count = 0;
    do {
      driver.get(URL);
      count++;
      Util.sleep(5000);
      if (count > 3) {
        break;
      }
    } while (!(new WebDriverWait(driver, Duration.ofSeconds(20)).until(
        webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
            .equals("complete"))));
  }

  public String getRandomIntString() {
    Date date = new Date();
    long time = date.getTime();
    Util.printInfo("Time in Milliseconds: " + time);
    Timestamp ts = new Timestamp(time);
    String num = ts.toString().replaceAll("[^0-9]", "");
    Util.printInfo("num :: " + num);
    Util.printInfo("option select :: " + num.charAt(12));
    Util.printInfo(String.valueOf(num.charAt(12)).trim());

    return String.valueOf(num.charAt(12)).trim();
  }

  @Step("Guac: Test Trial Download  " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> testCjtTrialDownloadUI(LinkedHashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<String, String>();

    try {
      Util.printInfo("Entering -> testCjtTrialDownloadUI -> " + data.get("trialDownloadUrl"));
      getUrl(data.get("trialDownloadUrl"));

      bicPage.clickUsingLowLevelActions("downloadFreeTrialLink");

      bicPage.waitForFieldPresent("downloadFreeTrialPopupNext1", 2000);
      bicPage.clickUsingLowLevelActions("downloadFreeTrialPopupNext1");

      bicPage.waitForFieldPresent("downloadFreeTrialPopupNext2", 2000);
      bicPage.clickUsingLowLevelActions("downloadFreeTrialPopupNext2");

      bicPage.waitForFieldPresent("downloadFreeTrailBusinessUserOption", 2000);
      bicPage.clickUsingLowLevelActions("downloadFreeTrailBusinessUserOption");

      bicPage.waitForFieldPresent("downloadFreeTrialPopupNext3", 2000);
      bicPage.clickUsingLowLevelActions("downloadFreeTrialPopupNext3");

      bicPage.waitForFieldPresent(BICECEConstants.DOWNLOAD_FREE_TRIAL_LOGIN_FRAME, 1000);

      // Checking if download is prompting for user sign in
      if (bicPage.isFieldVisible(BICECEConstants.DOWNLOAD_FREE_TRIAL_LOGIN_FRAME)) {
        bicPage.selectFrame(BICECEConstants.DOWNLOAD_FREE_TRIAL_LOGIN_FRAME);

        bicPage.waitForFieldPresent("downloadFreeTrialUserName", 1000);
        bicPage.populateField("downloadFreeTrialUserName",
            System.getProperty(BICECEConstants.EMAIL));

        bicPage.waitForFieldPresent("downloadFreeTrialVerifyUserButtonClick", 1000);
        bicPage.clickUsingLowLevelActions("downloadFreeTrialVerifyUserButtonClick");

        bicPage.waitForFieldPresent("downloadFreeTrialPassword", 1000);
        bicPage.populateField("downloadFreeTrialPassword",
            System.getProperty(BICECEConstants.PASSWORD));
        Util.sleep(5000);
        bicPage.waitForFieldPresent("downloadFreeTrialSignInButtonClick", 1000);
        bicPage.clickUsingLowLevelActions("downloadFreeTrialSignInButtonClick");
        bicPage.selectMainWindow();
      }

      Util.sleep(5000);
      bicPage.waitForFieldPresent("downloadFreeTrialCompanyName", 1000);
      bicPage.populateField("downloadFreeTrialCompanyName", data.get("companyName"));
      bicPage.clickUsingLowLevelActions("downloadFreeTrialState");
      bicPage.populateField("downloadFreeTrialPostalCode", data.get("postalCode"));
      bicPage.populateField("downloadFreeTrialPhoneNo", data.get("phoneNumber"));
      bicPage.clickUsingLowLevelActions("downloadFreeTrialBeginDownloadLink");
      Util.sleep(5000);
      AssertUtils.assertTrue(driver.getCurrentUrl().contains(data.get("trialDownloadUrl")),
          "SUCCESSFULLY STARTED DOWNLOAD");
      results.put(BICECEConstants.DOWNLOAD_STATUS, "Success. ");
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo("Error " + e.getMessage());
      AssertUtils.fail("Unable to test trial Download");
    }
    return results;
  }

  public void agreeToTerm() {
    Util.printInfo("Agree Element");
    try {
      bicPage.selectMainWindow();
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("document.getElementById('order-agreement').click()");
      Util.sleep(1000);
    } catch (Exception e) {
      AssertUtils.fail("Application Loading issue : Unable to click on 'order-agreement' checkbox");
    }
  }

  public void loginToOxygen(String emailID, String password) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(bicPage.getFirstFieldLocator("autodeskId"))));

    bicPage.populateField(BICECEConstants.AUTODESK_ID, emailID);
    bicPage.click(BICECEConstants.USER_NAME_NEXT_BUTTON);
    bicPage.waitForField(BICECEConstants.LOGIN_PASSWORD, true, 5000);
    bicPage.click(BICECEConstants.LOGIN_PASSWORD);
    bicPage.populateField(BICECEConstants.LOGIN_PASSWORD, password);
    bicPage.click(BICECEConstants.LOGIN_BUTTON);
    Util.sleep(15000);

    if (bicPage.isFieldPresent(BICECEConstants.GET_STARTED_SKIP_LINK)) {
      bicPage.click(BICECEConstants.GET_STARTED_SKIP_LINK);
      waitForLoadingSpinnerToComplete("loadingSpinner");
    }

    Util.printInfo("Successfully logged in");
  }

  public void signOutUsingMeMenu() {
    Util.printInfo("Signing out using meMenu");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(bicPage.getFirstFieldLocator("meMenuSignedIn"))));

    Util.printInfo("MeMenu for signed in state found. Trying to sign out.");
    try {
      bicPage.checkIfElementExistsInPage("meMenuSignedIn", 20);
      bicPage.click("meMenuSignedIn");
      bicPage.checkIfElementExistsInPage("meMenuSignOut", 60);
      bicPage.click("meMenuSignOut");
      bicPage.waitForPageToLoad();
    } catch (Exception e) {
      AssertUtils.fail("Application Loading issue : Unable to logout");
    }
  }

  private String[] getCardPaymentDetails(String paymentMethod) {
    debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);
    return getPaymentDetails(paymentMethod.toUpperCase()).split("@");
  }

  public void navigateToDotComPage(LinkedHashMap<String, String> data) {
    String productName =
        System.getProperty(BICECEConstants.PRODUCT_NAME) != null ? System.getProperty(
            BICECEConstants.PRODUCT_NAME) : data.get(BICECEConstants.PRODUCT_NAME);

    String constructDotComURL = data.get("guacDotComBaseURL") + data.get(BICECEConstants.COUNTRY_DOMAIN) + data
        .get(BICECEConstants.PRODUCTS_PATH) + productName + BICECEConstants.OVERVIEW;

    Util.printInfo("constructDotComURL " + constructDotComURL);
    getUrl(constructDotComURL);
    setStorageData();
  }

  private void closeGetHelpPopup() {
    try {
      WebElement getHelpIframe = bicPage
          .getMultipleWebElementsfromField(BICECEConstants.GET_HELP_IFRAME).get(0);
      driver.switchTo().frame(getHelpIframe);
      bicPage.waitForFieldPresent("getHelpPopUpCloseButton", 2000);
      bicPage.clickUsingLowLevelActions("getHelpPopUpCloseButton");
      Util.printInfo("Get help pop up closed.");
    } catch (Exception e) {
      Util.printInfo("Get help pop up does not appear on the page.");
    }
    driver.switchTo().defaultContent();
  }

  public void validatePelicanTaxWithCheckoutTax(String checkoutTax, String pelicanTax) {
    if (checkoutTax != null) {
      Double cartAmount = Double.valueOf(checkoutTax);
      Double pelicanAmount = Double.valueOf(pelicanTax);
      Util.printInfo("The total order amount in Cart " + cartAmount / 100);
      Util.printInfo("The total order amount in Pelican " + pelicanAmount);
      AssertUtils.assertTrue(Double.compare(cartAmount / 100, pelicanAmount) == 0,
          "Tax Amount in Pelican matches with the tax amount on Checkout page");
    }
  }

  public Boolean shouldValidateSAP() {
    return null != System.getProperty(BICECEConstants.SAP_VALIDATION) ?
        Boolean.valueOf(System.getProperty(BICECEConstants.SAP_VALIDATION)) :
        BICECEConstants.DEFAULT_SAP_VALIDATION;
  }

  /**
   * Calculate Time Delta between Pelican PO -> Subscription, PO -> ECC
   *
   * @param results
   * @return
   */
  public HashMap<String, String> calculateFulfillmentTime(HashMap<String, String> results) {
    String OS = System.getProperty("os.name").toLowerCase();
    PeriodFormatter periodFormatter = PeriodFormat.getDefault();
    SAPTestBase saptb = new SAPTestBase();
    HashMap<String, String> report = new HashMap<>();

    DateTimeFormatter poFormatter =
        DateTimeFormat.forPattern(BICECEConstants.PO_DATE_FORMAT).withZone(DateTimeZone.UTC);
    DateTimeFormatter subFormatter =
        DateTimeFormat.forPattern(BICECEConstants.SUB_DATE_FORMAT).withZone(DateTimeZone.UTC);
    DateTimeFormatter somOrderFormatter =
        DateTimeFormat.forPattern(BICECEConstants.PO_DATE_FORMAT).withZone(DateTimeZone.UTC);
    DateTimeFormatter invoiceFormatter =
        DateTimeFormat.forPattern(BICECEConstants.INVOICE_DATE_FORMAT).withZone(DateTimeZone.UTC);

    DateTime poCreatedDate = poFormatter.parseDateTime(results.get("getPOReponse_CreatedDate"));
    DateTime subCreatedDate = subFormatter.parseDateTime(results.get("response_subscriptionCreated"));

    try {
      saptb.sapConnector.connectSAPBAPI();
      String orderNumber = saptb.sapConnector.getOrderNumberUsingPO(results.get(BICConstants.orderNumber));
      HashMap<String, String> somOrder = saptb.sapConnector.getOrderDetails(orderNumber, "");
      saptb.sapConnector.connectSAPBAPIS4();
      HashMap<String, String> somOrderDetails = saptb.sapConnector.getSOMOrderDetailsFromTable(somOrder.get(
          "somOrderNumber"));
      report.put("somOrderNumber", somOrder.get("somOrderNumber"));
      report.put("somCreatedDate", somOrderDetails.get("createdTime"));

      HashMap<String, String> invoiceDetails = saptb.sapConnector.getInvoiceDetailsFromTable(results.get(
          BICConstants.orderNumber));
      report.put("invoiceNumber", invoiceDetails.get("invoiceNumber"));
      report.put("invoiceCreatedDate", invoiceDetails.get("createdTime"));
    } catch (ExceptionInInitializerError e) {
      Util.printWarning("SAP Initialization wont work with " + OS + ", so skipping SAP validation due to ,"
          + e.getMessage());
    }

    Period po2Sub = new Period(poCreatedDate, subCreatedDate);
    report.put(BICECEConstants.PO_TO_SUBSCRIPTION, periodFormatter.print(po2Sub));
    Util.printInfo("Pelican Order to Subscription time: " + periodFormatter.print(po2Sub));

    if (results.get("somCreatedDate") != null) {
      DateTime somCreatedDate = somOrderFormatter.parseDateTime(report.get("somCreatedDate"));
      Period po2Som = new Period(poCreatedDate, somCreatedDate);
      report.put(BICECEConstants.PO_TO_SOM_ORDER, periodFormatter.print(po2Som));
      Util.printInfo("Pelican Order to SOM Order create time: " + periodFormatter.print(po2Som));
    }

    if (results.get("invoiceCreatedDate") != null) {
      DateTime invoiceCreatedDate = invoiceFormatter.parseDateTime(report.get("invoiceCreatedDate"));
      Period po2inv = new Period(poCreatedDate, invoiceCreatedDate);
      report.put(BICECEConstants.PO_TO_INVOICE, periodFormatter.print(po2inv));
      Util.printInfo("Pelican Order to Invoice create time: " + periodFormatter.print(po2inv));
    }

    return report;
  }

  public Boolean isLOCPresentInCart() throws MetadataException {
    if (bicPage.checkFieldExistence("customerDetailsContinue")) {
      bicPage.waitForFieldPresent("customerDetailsContinue", 10000);
      Util.sleep(5000);
      bicPage.clickUsingLowLevelActions("customerDetailsContinue");
    }

    return bicPage.waitForField("payByInvoiceButton", true, 30000);
  }

  public void goToDotcomSignin(LinkedHashMap<String, String> data) {
    String constructDotComURL = data.get("guacDotComBaseURL") + ".com";

    bicPage.navigateToURL(constructDotComURL);
    Util.sleep(10000);
    bicPage.waitForFieldPresent("signInButton", 10000);
    bicPage.click("signInButton");
  }

  public static class Names {

    public final String firstName;
    public final String lastName;

    public Names(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    public HashMap<String, String> getMap() {

      return new HashMap<String, String>() {{
        put(BICECEConstants.FIRSTNAME, firstName);
        put(BICECEConstants.LASTNAME, lastName);
      }};
    }
  }
}
