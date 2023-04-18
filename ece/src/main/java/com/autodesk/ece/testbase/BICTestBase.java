package com.autodesk.ece.testbase;

import static org.testng.util.Strings.isNotNullAndNotEmpty;
import static org.testng.util.Strings.isNullOrEmpty;
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
import com.autodesk.testinghub.core.utils.ScreenCapture;
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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.util.Strings;

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
    return generateUniqueEmailID("letscheck.pw");
  }

  public static String generateMailosaurEmailID() {
    return generateUniqueEmailID("loxrkaci.mailosaur.net");
  }

  public static String generateUniqueEmailID(String emailDomain) {
    String storeKey = System.getProperty("store").replace("-", "");
    String emailType = System.getProperty("emailType");

    String sourceName = "thub";
    if (emailType != null && !emailType.isEmpty()) {
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

    if (System.getProperty(BICECEConstants.STORE).equals("STORE-JP")) {
      ba.put(BICECEConstants.STATE_PROVINCE, "大阪府");
      ba.put(BICECEConstants.COUNTRY, "日本");
    } else {
      ba.put(BICECEConstants.COUNTRY, newAddress.country);
      if (newAddress.province != null) {
        ba.put(BICECEConstants.STATE_PROVINCE, newAddress.province);
      }
    }
    Util.printInfo("The address being used :" + address);

    return ba;
  }

  @Step("Create BIC account")
  public void createBICAccount(Names names, String emailID, String password, Boolean skipIframe) {
    if (!skipIframe) {
      switchToBICCartLoginPage();
    }
    Util.sleep(5000);
    Util.printInfo("Url is loaded and we were able to switch to iFrame");

    try {
      bicPage.waitForFieldPresent("createNewUserGUAC", 30000);
      bicPage.clickUsingLowLevelActions("createNewUserGUAC");
      bicPage.waitForFieldPresent(BICECEConstants.BIC_FN, 30000);
      bicPage.clickUsingLowLevelActions(BICECEConstants.BIC_FN);
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
        "[data-testid='order-summary-section'] .checkout--order-summary-section--submit-order button"));
    if (submitButton.size() == 0) {
      AssertUtils.fail("The 'Submit order' button is not displayed.");
    }
  }

  @Step("Wait for loading spinner to complete")
  public void waitForLoadingSpinnerToComplete(String elementXPath) {
    Util.sleep(3000);
    try {
      int count = 0;
      while (bicPage.waitForFieldPresent(elementXPath, 5000)) {
        count++;
        Util.sleep(1000);
        if (count > 25) {
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
    Util.sleep(5000);

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

    String oxygenId = driver.manage().getCookieNamed("identity-sso").getValue();
    data.put(BICConstants.oxid, oxygenId);
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
    WebElement element = driver
        .findElement(By.xpath("//term-element[contains(@automationid, \"buy-component-term-billing-plan-1-month\")]"));
    executor.executeScript("arguments[0].click();", element);
    Util.sleep(2000);
  }

  @Step("Selecting Yearly Subscription")
  public void selectYearlySubscription(WebDriver driver) {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    WebElement element = driver
        .findElement(By.xpath("//term-element[contains(@automationid, \"buy-component-term-billing-plan-1-year\")]"));
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

      if (dataPaymentType.equals(BICECEConstants.PAYMENT_BACS) && Strings.isNotNullAndNotEmpty(
          System.getProperty(BICECEConstants.CSN))) {
        clickOnAddBACSProfileLink();
      }
      // Temporary solution because currently it does not allow to submit an order with the address from Customer details section
      if (data.get("isNonQuoteFlexOrder") != null) {
        Util.sleep(20000);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(BICECEConstants.DOCUMENT_GETELEMENTBYID_MANDATE_AGREEMENT_CLICK);
      }

      if (data.get("isFinancingCanceled") != null) {
        data.put(BICECEConstants.LASTNAME, "CANCELED");
      } else if (data.get("isFinancingDeclined") != null) {
        data.put(BICECEConstants.LASTNAME, "DECLINED");
      }

      String firstNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentProfile);
      String lastNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentProfile);

      bicPage.waitForFieldPresent(BICECEConstants.FIRSTNAME, 2000);
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

  public void clickOnContinueBtn(String paymentType) throws MetadataException {
    Util.sleep(2000);

    if (bicPage.waitForFieldPresent("creditCardPaymentTab", 5000)) {
      Util.printInfo("Clicking on Save button");
      String tabKey = paymentType.toLowerCase();
      if (paymentType.equalsIgnoreCase(BICECEConstants.VISA)
          || paymentType.equalsIgnoreCase(BICECEConstants.CREDITCARD)
          || paymentType.equalsIgnoreCase(BICECEConstants.MASTERCARD)
          || paymentType.equalsIgnoreCase(BICECEConstants.JCB)
          || paymentType.equalsIgnoreCase(BICECEConstants.AMEX)
          || paymentType.equalsIgnoreCase(BICECEConstants.LOC)) {
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
    } else if (bicPage.waitForFieldPresent("creditCardRadioButton", 5000)) {
      Util.printInfo("clicking review button");
      bicPage.clickUsingLowLevelActions("reviewLOCOrder");
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
          stateXpath = "", vatXpath = "", abnXpath = "";
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
      vatXpath = bicPage.getFirstFieldLocator(BICECEConstants.VAT_NUMBER)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);
      abnXpath = bicPage.getFirstFieldLocator(BICECEConstants.ABN_NUMBER)
          .replace(BICECEConstants.PAYMENT_PROFILE, paymentTypeToken);

      bicPage.waitForFieldPresent(BICECEConstants.FULL_ADDRESS, 2000);
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
      Util.sleep(2000);
      driver.findElement(By.xpath(phoneXpath)).sendKeys("2333422112");

      if (address.get(BICECEConstants.STATE_PROVINCE) != null && !address
          .get(BICECEConstants.STATE_PROVINCE).isEmpty()) {
        driver.findElement(By.xpath(stateXpath))
            .sendKeys(address.get(BICECEConstants.STATE_PROVINCE));
      }

      String taxId = System.getProperty(BICECEConstants.TAX_ID);

      if (taxId != null && !taxId.isEmpty()) {
        String numberKey;
        if ("STORE-AUS".equals(System.getProperty(BICECEConstants.STORE))) {
          numberKey = abnXpath;
        } else {
          numberKey = vatXpath;
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

        if (driver.findElement(By.xpath(numberKey)).isDisplayed()) {
          Util.printInfo("Populating " + numberKey + " field with " + taxId);
          driver.findElement(By.xpath(numberKey))
              .sendKeys(taxId);
          driver.findElement(By.xpath(numberKey)).sendKeys(Keys.TAB);
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
    try {
      if (bicPage.waitForFieldPresent("creditCardPaymentTab", 5000)) {
        bicPage.clickUsingLowLevelActions("creditCardPaymentTab");
      } else if (bicPage.waitForFieldPresent("creditCardRadioButton", 5000)) {
        bicPage.clickUsingLowLevelActions("creditCardRadioButton");
      }
    } catch (MetadataException e) {
      e.printStackTrace();
    }
    if (!bicPage.isFieldVisible("invoicePaymentEdit")) {
      bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 10000);
      try {
        WebElement creditCardNumberFrame = bicPage
            .getMultipleWebElementsfromField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME).get(0);
        WebElement expiryDateFrame = bicPage.getMultipleWebElementsfromField("expiryDateFrame")
            .get(0);
        WebElement securityCodeFrame = bicPage.getMultipleWebElementsfromField("securityCodeFrame")
            .get(0);

        driver.switchTo().frame(creditCardNumberFrame);
        Util.printInfo("Entering card number : " + paymentCardDetails[0]);
        Util.sleep(1000);
        bicPage.populateField("CardNumber", paymentCardDetails[0]);
        driver.switchTo().defaultContent();
        Util.sleep(1000);

        driver.switchTo().frame(expiryDateFrame);
        Util.printInfo(
            "Entering Expiry date : " + paymentCardDetails[1] + "/" + paymentCardDetails[2]);
        Util.sleep(1000);
        bicPage.populateField("expirationPeriod", paymentCardDetails[1] + paymentCardDetails[2]);
        driver.switchTo().defaultContent();
        Util.sleep(1000);

        driver.switchTo().frame(securityCodeFrame);
        Util.printInfo("Entering security code : " + paymentCardDetails[3]);
        Util.sleep(1000);
        bicPage.populateField("PAYMENTMETHOD_SECURITY_CODE", paymentCardDetails[3]);
        driver.switchTo().defaultContent();
      } catch (MetadataException e) {
        e.printStackTrace();
        AssertUtils.fail("Unable to enter Card details to make payment");
      }
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
      Util.sleep(2000);
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
      if (bicPage.checkIfElementExistsInPage("financingTab", 10)) {
        bicPage.clickUsingLowLevelActions("financingTab");

        populateBillingAddress(address, data);
      } else {
        bicPage.clickUsingLowLevelActions("financingTabFlexCart");
        bicPage.clickUsingLowLevelActions("reviewLOCOrder");
      }

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
    String paypalEmail = data.get(BICECEConstants.PAYPAL_EMAIL);

    try {
      Util.printInfo("Selecting Paypal checkout frame...");
      bicPage.selectFrame("paypalCheckoutOptionFrame");

      bicPage.scrollToBottomOfPage();

      Util.printInfo("Clicking on Paypal checkout button...");
      bicPage.clickUsingLowLevelActions("paypalCheckoutBtn");

      Util.printInfo("Wait for Paypal checkout page to be load");
      Util.sleep(3000);

      Set<String> windows = driver.getWindowHandles();
      for (String window : windows) {
        driver.switchTo().window(window);
      }

      driver.manage().window().maximize();
      bicPage.waitForPageToLoad();
      bicPage.waitForElementToDisappear("paypalPageLoader", 30);

      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("paypalTitle").get(0), 45);

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (bicPage.checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 10)) {
        bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
      }

      if (bicPage.checkIfElementExistsInPage(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON, 10)) {
        bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON);
      }

      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("paypalUsernameField").get(0), 10);
      bicPage.populateField("paypalUsernameField", paypalEmail);

      bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_NEXT_BUTTON);

      Util.printInfo("Entering paypal password...");
      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("paypalPasswordField").get(0), 10);
      bicPage.populateField("paypalPasswordField",
          ProtectedConfigFile.decrypt(data.get("paypalSsap")));

      Util.printInfo("Clicking on login button...");
      bicPage.clickUsingLowLevelActions("paypalLoginBtn");
      bicPage.waitForElementToDisappear("paypalPageLoader", 30);

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (bicPage.checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 15)) {
        bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
        Util.sleep(5000);
      } else {
        Util.printInfo("Accept cookies button not present.");
      }

      bicPage.scrollToBottomOfPage();

      String paymentTypeXpath = "";
      if (System.getProperty("store").equals("STORE-NAMER")) {
        paymentTypeXpath = bicPage.getFirstFieldLocator("paypalPaymentOption")
            .replace("<PAYMENTOPTION>", data.get("paypalPaymentType"));
        driver.findElement(By.xpath(paymentTypeXpath)).click();
      }

      Util.sleep(2000);

      bicPage.executeJavascript("window.scrollBy(0,1000);");

      if (bicPage.checkIfElementExistsInPage("paypalSaveAndContinueBtn", 5)) {
        Util.printInfo("Clicking on Save And Continue button.");
        bicPage.clickUsingLowLevelActions("paypalSaveAndContinueBtn");
      }

      int count = 0;
      while (bicPage.checkIfElementExistsInPage("paypalReviewBtn", 5)) {
        count++;

        if (count > 3) {
          AssertUtils.fail("Unable to click on Continue button.");
        } else {
          String continueBtn = driver.findElement(
              By.xpath(bicPage.getFirstFieldLocator("paypalReviewBtn"))).getText();

          if (!Strings.isNotNullAndNotEmpty(continueBtn)) {
            continueBtn = driver.findElement(
                By.xpath(bicPage.getFirstFieldLocator("paypalReviewBtn"))).getAttribute("value");
          }

          Util.printInfo("Clicking on '" + continueBtn + "' button.");
          bicPage.clickUsingLowLevelActions("paypalReviewBtn");
          Util.sleep(3000);
        }
      }

      driver.switchTo().window(parentWindow);
      Util.sleep(5000);

      if (System.getProperty("store").equals("STORE-JP")) {
        String paypalString = driver.findElement(By.xpath(
                "//*[@data-testid=\"payment-section-add\"]//div[2]/div[2]/div[2]/p"))
            .getText();
        AssertUtils.assertEquals(paypalString,
            "PayPal が支払い方法として選択されています。");
      } else if (System.getProperty("store").equals("STORE-NAMER")) {
        AssertUtils.assertEquals(bicPage.getTextFromLink("paypalConfirmationText"),
            "PayPal is selected for payment.");
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
    Boolean success = true;

    try {
      bicPage.waitForField("payByInvoiceTab", true, 30000);
      while (success) {
        count++;

        // If we refreshed the page, we need to click on continue again
        if (bicPage.checkIfElementExistsInPage("customerDetailsContinue", 15)) {
          bicPage.waitForFieldPresent("customerDetailsContinue", 10000);
          Util.sleep(5000);
          bicPage.clickUsingLowLevelActions("customerDetailsContinue");
        }

        if (count > 5) {
          AssertUtils.fail("Retries exhausted: Pay By Invoice is missing in Cart");
        }

        Util.printInfo("Clicking on pay By Invoice tab...");
        try {
          if (bicPage.waitForFieldPresent("payByInvoiceTab", 5000)) {
            bicPage.clickUsingLowLevelActions("payByInvoiceTab");
          } else {
            bicPage.scrollToBottomOfPage();
            bicPage.clickUsingLowLevelActions("payByInvoiceRadioButton");
            Util.sleep(5000);
          }
        } catch (Exception e) {
          Util.printInfo(
              "Pay By Invoice tab not visible for this quote :" + payByInvoiceDetails.get(BICECEConstants.QUOTE_ID));
          Util.printInfo("email id :" + payByInvoiceDetails.get(BICECEConstants.emailid));
        }

        try {
          if (payByInvoiceDetails.get(BICECEConstants.IS_SAME_PAYER) != null && payByInvoiceDetails.get(
              BICECEConstants.IS_SAME_PAYER).equals(BICECEConstants.TRUE)) {
            if (bicPage.checkIfElementExistsInPage("cartEmailAddress", 10)) {
              Util.printInfo("Entering Payer email and CSN.");
              bicPage.populateField("cartEmailAddress", payByInvoiceDetails.get(BICECEConstants.PAYER_EMAIL));
              bicPage.populateField("payerCSN", payByInvoiceDetails.get(BICECEConstants.PAYER_CSN));
              bicPage.waitForFieldPresent("reviewLOCOrder", 10);
              Util.printInfo("clicking continue");
              bicPage.clickUsingLowLevelActions("reviewLOCOrder");
            }
          }
        } catch (Exception e) {
          Util.printInfo("Failed entering Payer email and CSN.");
          driver.navigate().refresh();
          Util.sleep(3000);
          continue;
        }

        try {
          bicPage.checkIfElementExistsInPage("yesPurchaseOrderOption", 10);

          if (payByInvoiceDetails.containsKey(BICECEConstants.ORDER_ID)) {
            if (!payByInvoiceDetails.get(BICECEConstants.ORDER_ID).equals("")) {
              Util.printInfo("Entering Purchase order number : " + payByInvoiceDetails.get(BICECEConstants.ORDER_ID));
              bicPage.populateField("purchaseOrderNumber", payByInvoiceDetails.get(BICECEConstants.ORDER_ID));
            }
          } else {
            Util.printInfo("Selecting No PO Option in LOC flow");
            bicPage.clickUsingLowLevelActions("noPurchaseOrderOption");
          }
        } catch (Exception e) {
          Util.printInfo("Failed to specify the PO number or No option in LOC flow...");
          driver.navigate().refresh();
          Util.sleep(3000);
          continue;
        }

        success = false;
      }

      if (payByInvoiceDetails.containsKey(BICECEConstants.PURCHASE_ORDER_DOCUMENT_PATH)) {
        if (!payByInvoiceDetails.get(BICECEConstants.PURCHASE_ORDER_DOCUMENT_PATH).equals("")) {
          bicPage.populateField("portalPurchaseOrderDocument",
              payByInvoiceDetails.get(BICECEConstants.PURCHASE_ORDER_DOCUMENT_PATH));
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

      if (payByInvoiceDetails.containsKey(BICECEConstants.PAYER_EMAIL) && bicPage.waitForField("payerSameAsCustomer",
          true, 10000)) {
        bicPage.clickUsingLowLevelActions("payerSameAsCustomer");
        bicPage.waitForElementVisible(
            bicPage.getMultipleWebElementsfromField("cartEmailAddress").get(0), 10);
        bicPage.populateField("cartEmailAddress", payByInvoiceDetails.get(BICECEConstants.PAYER_EMAIL));

        if (payByInvoiceDetails.containsKey(BICECEConstants.ORGANIZATION_NAME)) {
          address.put(BICECEConstants.ORGANIZATION_NAME, payByInvoiceDetails.get(BICECEConstants.ORGANIZATION_NAME));
        }

        enterCustomerDetails(address);
      }
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Pay By invoice payment details");
    }

    bicPage.click("reviewLOCOrder");

    try {
      if (bicPage.checkIfElementExistsInPage("paymentContinueButton", 15)) {
        bicPage.clickUsingLowLevelActions("paymentContinueButton");
      }
    } catch (MetadataException e) {
      throw new RuntimeException(e);
    }
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
            selectPaypalPayment();
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
          case BICECEConstants.CASH:
            selectCashPayment();
            break;
          case BICECEConstants.PAYMENT_KONBINI:
            selectKonbiniPayment();
            selectConvenienceStoreType();
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

  public void submitOrder(HashMap<String, String> data) {
    submitOrder(data, true);
  }

  @Step("Submit Order on Checkout page")
  public void submitOrder(HashMap<String, String> data, Boolean shouldFail) {
    try {
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
        while (bicPage.waitForFieldPresent("continueCheckout", 5000)) {
          Util.printInfo(" CONTINUE CHECKOUT Modal is present");
          driver.findElement(By.xpath("//*[text()='CONTINUE CHECKOUT']")).click();
          Util.sleep(5000);
          countModal++;
          if (countModal > 3) {
            AssertUtils.fail("Unexpected Pop up in the cart");
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
        if (shouldFail) {
          AssertUtils.fail("Failed to click on Submit button.");
        } else {
          ScreenCapture.getInstance().captureFullScreenshot();
          Util.printInfo(
              "Taking screenshot, failed to find an element in Submit Order flow. Dont worry we have retries");
          return;
        }
      }

      // Zip Pay Checkout
      if (data.get(BICECEConstants.PAYMENT_TYPE).equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_ZIP)) {
        zipTestBase.setTestData(data);
        zipTestBase.zipPayCheckout();
      }

    } catch (NoSuchElementException nSE) {
      ScreenCapture.getInstance().captureFullScreenshot();
      Util.printInfo("Taking screenshot, failed to find an element in Submit Order flow. Dont worry we have retries");
    }
  }

  @Step("Retrieving Order Number")
  public String getOrderNumber(HashMap<String, String> data) {
    String orderNumber = null;
    Util.sleep(5000);
    if (!bicPage.checkFieldExistence("orderNumberLabel")) {
      Util.printInfo("Could not find the Order Number Label. So, Waiting for Page load completely.");
      bicPage.waitForPageToLoad();
    }

    JavascriptExecutor executor = (JavascriptExecutor) driver;
    String response = (String) executor
        .executeScript("return sessionStorage.getItem('purchase')");

    try {
      JSONObject jsonObject = JsonParser.getJsonObjectFromJsonString(response);
      JSONObject purchaseOrder = (JSONObject) jsonObject.get("purchaseOrder");
      orderNumber = purchaseOrder.get("id") != null ? purchaseOrder.get("id").toString() : null;
      if (orderNumber != null && !orderNumber.isEmpty()) {
        Util.printInfo("Yay! Found the Order Number. Proceeding to next steps...");
      }
    } catch (Exception e) {
      ScreenCapture.getInstance().captureFullScreenshot();
      Util.printInfo("Taking screenshot, failed to find Order Number.");
    }

    if (null == orderNumber) {
      ScreenCapture.getInstance().captureFullScreenshot();
      Util.printInfo("Taking screenshot, failed to find Order Number. Dont worry we have retries");
      return null;
    } else {
      validateBicOrderNumber(orderNumber);
    }

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

    } else {
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

      // Selecting monthly for Non-Flex
      if (productType.equals("flex")) {
        bicPage.waitForFieldPresent("flexTab", 5000);
        bicPage.clickUsingLowLevelActions("flexTab");

        if (System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
          bicPage.clickUsingLowLevelActions("flex500Tokens");
        }

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
      financingTestBase.completeFinancingApplication(data);
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

    String orderNumber = createBICOrderDotCom(data, true);

    if (data.get(BICECEConstants.PAYMENT_TYPE)
        .equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      financingTestBase.setTestData(data);
      financingTestBase.completeFinancingApplication(data);
    }

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @Step("Quote2Order: Navigate to quote checkout " + GlobalConstants.TAG_TESTINGHUB)
  public void navigateToQuoteCheckout(LinkedHashMap<String, String> data) {
    String url = data.get("quote2OrderCartURL");
    Util.printInfo("Quote URL: " + url);
    data.put("checkoutUrl", url);
    getUrl(url);
    setStorageData();
  }

  public String getQuote2OrderCartURL(LinkedHashMap<String, String> data) {
    String language = "?lang=" + data.get(BICECEConstants.LOCALE).substring(0, 2);
    String country = "&country=" + data.get(BICECEConstants.LOCALE).substring(3);
    String currency = "&currency=" + data.get(BICECEConstants.currencyStore);
    return data.get("Quote2OrderBaseURL") + data.get(BICECEConstants.QUOTE_ID) + language + country
        + currency;
  }

  @Step("Placing the Flex Order" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> placeFlexOrder(LinkedHashMap<String, String> data) throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String orderNumber = null;
    Map<String, String> address = getBillingAddress(data);
    int attempt = 0;
    Boolean isOrderCaptured = false;

    while (!isOrderCaptured) {

      if (attempt > 10) {
        Assert.fail("Retries exhausted \"Submit Order\" failed to place the Order.");
      } else {
        Util.printInfo("Placing Flex Order Attempt: " + attempt++);
      }

      if (isNotNullAndNotEmpty("isNonQuoteFlexOrder")) {
        enterCustomerDetails(address);
        data.put(BICECEConstants.BILLING_DETAILS_ADDED, BICECEConstants.TRUE);
      } else {
        populateTaxIdForFlex();
      }

      if (bicPage.checkIfElementExistsInPage("customerDetailsContinue", 15)) {
        bicPage.clickUsingLowLevelActions("customerDetailsContinue");
        bicPage.waitForElementToDisappear("customerDetailsContinue", 10);
      }

      if (bicPage.checkIfElementExistsInPage("customerDetailsContinue", 10)) {
        bicPage.clickUsingLowLevelActions("customerDetailsContinue");
        bicPage.waitForElementToDisappear("customerDetailsContinue", 10);
      }

      if (bicPage.checkIfElementExistsInPage("paymentEditBtn", 5)) {
        bicPage.clickUsingLowLevelActions("paymentEditBtn");
      }

      String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);

      if (isNullOrEmpty(data.get("isReturningUser"))) {
        enterBillingDetails(data, address, paymentMethod);
      } else if (data.get(BICECEConstants.PAYMENT_TYPE).equals("LOC")) {
        paymentMethod = "LOC";
        enterBillingDetails(data, address, paymentMethod);
      }

      if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
        if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
          submitOrder(data, false);
        }
        orderNumber = getOrderNumber(data);
        if (null != orderNumber) {
          isOrderCaptured = true;
          printConsole(orderNumber, data, address);
          Util.printInfo("Placing Flex Order Attempt: " + attempt + " - Successful !");
        } else {
          Util.printInfo("Placing Flex Order Attempt: " + attempt + " - Failed !");
          Util.sleep(60000);
          driver.navigate().refresh();
        }
      } else {
        if (isNotNullAndNotEmpty(data.get("isReturningUser"))) {
          if (bicPage.checkIfElementExistsInPage("reviewLOCOrder", 10)) {
            bicPage.clickUsingLowLevelActions("reviewLOCOrder");
            bicPage.waitForElementToDisappear("reviewLOCOrder", 15);
          }
          bicPage.clickUsingLowLevelActions(BICECEConstants.SUBMIT_ORDER_BUTTON);
          bicPage.waitForElementToDisappear(BICECEConstants.SUBMIT_ORDER_BUTTON, 15);
        }
        financingTestBase.setTestData(data);
        financingTestBase.completeFinancingApplication(data);
        break;
      }
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
    Util.printInfo("Getting the URL: " + flexTokensEstimatorUrl);
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
        .findElement(By.xpath("//*[@data-testid=\"fe-summary-price\"]/div[@class=\"fe-rec-totals-fadeIn\"]"))
        .getText();
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
        .findElement(By.xpath("//p[@data-testid=\"checkout--order-summary-section--total\"]")).getText();
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
      boolean isLoggedOut = bicPage.checkIfElementExistsInPage("createNewUseriFrame", 20);
      if (isLoggedOut) {
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
    String oxygenId = driver.manage().getCookieNamed("identity-sso").getValue();
    data.put(BICConstants.oxid, oxygenId);
  }

  private void updateEstimatorToolInput(WebElement webElement, int input) {
    webElement.click();
    Util.sleep(2500);
    webElement.sendKeys(Keys.BACK_SPACE);
    Util.sleep(2500);
    webElement.sendKeys(String.valueOf(input));
  }

  private void updateQuantity(String priceId, String quantity) {
    Util.sleep(5000);
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

      Util.printInfo("Local Storage: set 'nonsensitiveUserLocationModalViewed' to true.");
      js.executeScript("window.localStorage.setItem(\"nonsensitiveUserLocationModalViewed\",\"true\");");

      driver.navigate().refresh();
    } catch (Exception e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
  }

  public void clickMandateAgreementCheckbox() {
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

  private String createBICOrderDotCom(LinkedHashMap<String, String> data, Boolean isLoggedIn) throws
      MetadataException {
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

    AssertUtils.assertFalse(isTTRButtonPresentInCart(), "TTR button should not be present for this scenario");

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

  private void dismissChatPopup() {
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
  }

  public void enterBillingDetails(LinkedHashMap<String, String> data,
      Map<String, String> address, String paymentMethod) throws MetadataException {
    String[] paymentCardDetails = getCardPaymentDetails(paymentMethod);
    dismissChatPopup();
    selectPaymentProfile(data, paymentCardDetails, address);
    dismissChatPopup();

    if (paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      return;
    }

    if (paymentMethod.equals(BICECEConstants.PAYMENT_KONBINI)) {
      return;
    }

    if (data.get("productType").equals("flex") && (paymentMethod.equalsIgnoreCase(BICECEConstants.CREDITCARD)
        || paymentMethod.equals(BICECEConstants.VISA))) {
      try {
        if (bicPage.checkIfElementExistsInPage("savePaymentProfile", 20)) {
          bicPage.clickUsingLowLevelActions("savePaymentProfile");
        } else {
          bicPage.clickUsingLowLevelActions("reviewLOCOrder");
        }
      } catch (MetadataException e) {
        throw new RuntimeException(e);
      }
    } else if (null != data.get(BICECEConstants.QUOTE_ID) && !paymentMethod.equalsIgnoreCase(BICECEConstants.LOC)) {
      clickOnContinueBtn(System.getProperty(BICECEConstants.PAYMENT));
    } else if (data.get("isNonQuoteFlexOrder") != null &&
        data.get(BICECEConstants.BILLING_DETAILS_ADDED).equalsIgnoreCase(BICECEConstants.TRUE) &&
        data.get(BICECEConstants.USER_TYPE).equalsIgnoreCase("newUser") && !paymentMethod.equalsIgnoreCase(
        BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
      clickOnContinueBtn(System.getProperty(BICECEConstants.PAYMENT));
    } else if ((data.get(BICECEConstants.BILLING_DETAILS_ADDED) == null || !data
        .get(BICECEConstants.BILLING_DETAILS_ADDED).equals(BICECEConstants.TRUE))
        && !paymentMethod.equalsIgnoreCase(BICECEConstants.LOC)) {
      debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
      populateBillingAddress(address, data);
      debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }
  }

  public void enterPayInvoiceBillingDetails(LinkedHashMap<String, String> data,
      Map<String, String> address, String paymentMethod) throws MetadataException {
    String[] paymentCardDetails = getCardPaymentDetails(paymentMethod);
    selectPaymentProfile(data, paymentCardDetails, address);
    Util.sleep(2000);
    Util.printInfo("Checking if submit payment button enabled or not");
    WebElement submitPaymentButton = bicPage.getMultipleWebElementsfromField("submitPaymentButton").get(0);
    if (submitPaymentButton.getAttribute("class").contains("disabled")) {
      Util.printInfo("Submit payment button disabled");
      populateBillingAddress(address, data);
      debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }
    Util.printInfo("Submit payment button enabled");
  }

  public void enterCustomerDetails(Map<String, String> address)
      throws MetadataException {

    if (bicPage.checkIfElementExistsInPage("companyNameField", 15)) {
      bicPage.populateField("companyNameField", address.get(BICECEConstants.ORGANIZATION_NAME));
    }

    if (bicPage.checkIfElementExistsInPage("selectCountryField", 10)) {
      bicPage.clickUsingLowLevelActions("selectCountryField");
      String selectCountryOption = bicPage.getFirstFieldLocator("selectCountryOption")
          .replace("<COUNTRY>", System.getProperty(BICECEConstants.LOCALE).substring(3));
      driver.findElement(By.xpath(selectCountryOption)).click();
    }

    if (bicPage.checkIfElementExistsInPage("address1Field", 10)) {
      bicPage.populateField("address1Field", address.get(BICECEConstants.FULL_ADDRESS));
    } else if(bicPage.checkIfElementExistsInPage("addressAutocomplete", 10)) {
      bicPage.populateField("addressAutocomplete", address.get(BICECEConstants.FULL_ADDRESS));
    }
    Util.sleep(2000);

    if (bicPage.checkIfElementExistsInPage("cityField", 10)) {
      bicPage.populateField("cityField", address.get(BICECEConstants.CITY));
      Util.sleep(2000);
    }

    if (bicPage.checkIfElementExistsInPage("selectStateField", 10)) {
      bicPage.clickUsingLowLevelActions("selectStateField");
      String selectStateOption = bicPage.getFirstFieldLocator("selectStateOption")
          .replace("<STATE_PROVINCE>", address.get(BICECEConstants.STATE_PROVINCE));
      driver.findElement(By.xpath(selectStateOption)).click();
    }
    Util.sleep(2000);

    if (bicPage.checkIfElementExistsInPage("postalCodeField", 10)) {
      bicPage.populateField("postalCodeField", address.get(BICECEConstants.ZIPCODE));
    }
    Util.sleep(2000);

    if (bicPage.checkIfElementExistsInPage("phoneNumberField", 10)) {
      bicPage.populateField("phoneNumberField", address.get(BICECEConstants.PHONE_NUMBER));
    }

    populateTaxIdForFlex();

    if (bicPage.checkIfElementExistsInPage("customerDetailsContinue", 10)) {
      Util.printInfo("Clicking on Continue in Customer Details section.");
      bicPage.clickUsingLowLevelActions("customerDetailsContinue");
      waitForLoadingSpinnerToComplete("loadingSpinner");
      bicPage.waitForElementToDisappear("customerDetailsContinue", 10);
    }

    if (bicPage.checkIfElementExistsInPage("confirmCustomerAddressAlert", 10)) {
      if (bicPage.checkIfElementExistsInPage("customerDetailsAddress", 10)) {
        Util.printInfo("Two or more suggested addresses. Clicking on radio button to choose one.");
        bicPage.clickUsingLowLevelActions("customerDetailsAddress");
        waitForLoadingSpinnerToComplete("loadingSpinner");
      }

      if (bicPage.checkIfElementExistsInPage("customerDetailsContinue", 10)) {
        Util.printInfo("Address confirmation requested, Clicking on Continue button.");
        bicPage.clickUsingLowLevelActions("customerDetailsContinue");
        waitForLoadingSpinnerToComplete("loadingSpinner");
      }
    }

    Util.sleep(5000);

    boolean isCustomerDetailsComplete = bicPage.checkIfElementExistsInPage("customerDetailsComplete", 20);
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
      if (System.getProperty("store").equals("STORE-JP")) {
        if (driver.findElement(By.xpath("//h2[contains(text(),\"それともご質問がありますか\")]"))
            .isDisplayed()) {
          bicPage.clickUsingLowLevelActions("promoCodePopUpThanksButton");
        }
      } else {
        if (driver.findElement(By.xpath("//h2[contains(text(),\"just have a question\")]"))
            .isDisplayed()) {
          bicPage.clickUsingLowLevelActions("promoCodePopUpThanksButton");
        }
      }

      priceBeforePromo = bicPage.getValueFromGUI("promoCodeBeforeDiscountPrice").trim();
      Util.printInfo("Step : Entering promo code " + promoCode + "\n" + " priceBeforePromo : "
          + priceBeforePromo);

      driver.findElement(By.xpath("//*[@data-testid=\"promotion-details\"]/a")).click();
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
      Util.sleep(10000);

      try {
        if (bicPage.checkIfElementExistsInPage("orderSummaryUSTax", 5)) {
          Util.printInfo("Flex tax condition for US Tax");
          taxValue = driver.findElement(
              By.xpath(bicPage.getFirstFieldLocator("orderSummaryUSTax"))).getText();
        } else if (bicPage.checkIfElementExistsInPage("orderSummaryUKTax", 5)) {
          Util.printInfo("Flex tax condition for GBP Tax");
          taxValue = driver.findElement(
              By.xpath(bicPage.getFirstFieldLocator("orderSummaryUKTax"))).getText();
        } else if (bicPage.checkIfElementExistsInPage("orderSummaryVat", 5)) {
          Util.printInfo("Flex tax condition for Vat");
          taxValue = driver.findElement(
              By.xpath(bicPage.getFirstFieldLocator("orderSummaryVat"))).getText();
        } else {
          Util.printInfo("Flex tax condition - default");
          taxValue = driver
              .findElement(
                  By.xpath(
                      "//div[@class='checkout--order-summary-section--products-total']/div[2]/p[2][@data-pricing-source='C' or @data-pricing-source='PQ']"))
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
      if (data.containsKey("taxRate")) {
        String subTotal = driver.findElement(
            By.xpath(bicPage.getFirstFieldLocator("subtotalPrice"))).getText().replaceAll("[^0-9]", "");
        int subTotalValue = Integer.parseInt(subTotal);
        double taxRate = Double.parseDouble(data.get("taxRate"));
        Util.printInfo("Asserting calculated tax rate");
        AssertUtils.assertEquals(taxValueAmount, (double) Math.round(subTotalValue * taxRate),
            "Tax matches calculated value");
      } else {
        Util.printInfo("This state does not collect tax.");
        AssertUtils.assertEquals(taxValueAmount / 100, 0.00, "Tax value is equal to zero");
      }
    } else {
      Util.printInfo(
          "Entered isTaxed value is not valid. Can not assert if tax is displayed properly. Should be Y/N.");
    }
  }

  public Boolean isTTRButtonPresentInCart() {
    return bicPage.isFieldPresent("cartTTRRedirectionButton");
  }

  public Boolean refreshCartIfEmpty() throws MetadataException {
    int count = 0;
    while (!bicPage.checkIfElementExistsInPage("customerDetailsContinue", 15)) {
      count++;
      driver.navigate().refresh();
      Util.sleep(5000);
      if (count > 5) {
        return false;
      }
      Util.printInfo("Refresh Cart until end Customer continue Button is seen, Attempt: " + count);
    }

    return true;
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
  public HashMap<String, String> createBICReturningUser(LinkedHashMap<String, String> data) throws
      MetadataException {
    String orderNumber;
    HashMap<String, String> results = new HashMap<>();
    String paymentMethod = data.get("paymentMethod");
    String oxygenLogOutUrl = data.get("oxygenLogOut");

    if (System.getProperty("store").equals("STORE-JP")) {
      Util.printInfo("Log out with Oxygen direct URL: " + oxygenLogOutUrl);
      getUrl(oxygenLogOutUrl);
    }

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
    String oxygenLogOutUrl = data.get("oxygenLogOut");

    if (System.getProperty("store").equals("STORE-JP")) {
      Util.printInfo("Log out with Oxygen direct URL: " + oxygenLogOutUrl);
      getUrl(oxygenLogOutUrl);
    }

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

    if (System.getProperty(BICECEConstants.PAYMENT).equalsIgnoreCase(BICECEConstants.PAYPAL)) {
      Map<String, String> address = getBillingAddress(data.get(BICECEConstants.ADDRESS));
      enterBillingDetails(data, address, System.getProperty(BICECEConstants.PAYMENT));
    }
    submitOrder(data);
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
    String password = ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD));

    try {
      String constructDotComURL = data.get("guacDotComBaseURL") + ".com/products/autocad/trial-intake";
      Util.printInfo("constructDotComURL " + constructDotComURL);
      bicPage.navigateToURL(constructDotComURL);
      Util.sleep(5000);

      setStorageData();
      Util.sleep(5000);

      bicPage.waitForFieldPresent("freeTrialBusiness", 2000);
      bicPage.clickUsingLowLevelActions("freeTrialBusiness");

      bicPage.waitForFieldPresent("freeTrialNextButton1", 2000);
      bicPage.clickUsingLowLevelActions("freeTrialNextButton1");

      bicPage.waitForFieldPresent("freeTrialNextButton2", 2000);
      bicPage.clickUsingLowLevelActions("freeTrialNextButton2");

      createBICAccount(generateFirstAndLastNames(), generateUniqueEmailID(), password, true);

      Util.sleep(5000);

      bicPage.waitForFieldPresent("phoneVerificationCallingCode", 2000);
      bicPage.clickUsingLowLevelActions("phoneVerificationCallingCode");
      bicPage.clickUsingLowLevelActions("selectCodeFromList");

      bicPage.waitForFieldPresent("phoneVerificationNumber", 2000);
      bicPage.clickUsingLowLevelActions("phoneVerificationNumber");
      bicPage.populateField("phoneVerificationNumber", "5555551234");

      bicPage.waitForFieldEnabled("sendVerificationCode");
      bicPage.clickUsingLowLevelActions("sendVerificationCode");
      Util.sleep(2000);

      bicPage.waitForFieldPresent("jobLevelField", 5000);
      bicPage.clickUsingLowLevelActions("jobLevelField");
      Util.PrintInfo("Selecting 'Business Owner/Entrepreneur' from 'Job level field'");
      bicPage.clickUsingLowLevelActions("selectFieldFromList");

      bicPage.waitForFieldPresent("objectiveOfTrial", 2000);
      bicPage.clickUsingLowLevelActions("objectiveOfTrial");
      Util.PrintInfo("Selecting 'I want to try this product for the first time' from 'Objective of trial'");
      bicPage.clickUsingLowLevelActions("selectFieldFromList");

      bicPage.waitForFieldPresent("industriesField", 2000);
      bicPage.clickUsingLowLevelActions("industriesField");
      Util.PrintInfo("Selecting 'Advertising, Publishing and Graphic Design' from 'Industry field'");
      bicPage.clickUsingLowLevelActions("selectFieldFromList");

      bicPage.waitForFieldPresent("roleField", 2000);
      bicPage.clickUsingLowLevelActions("roleField");
      Util.PrintInfo("Selecting '3D Animator' from 'Role field'");
      bicPage.clickUsingLowLevelActions("selectFieldFromList");

      bicPage.waitForFieldPresent("freeTrialNextButton3", 2000);
      bicPage.clickUsingLowLevelActions("freeTrialNextButton3");

      bicPage.waitForFieldPresent("downloadFreeTrialCompanyName", 1000);
      bicPage.populateField("downloadFreeTrialCompanyName", data.get("companyName"));

      bicPage.waitForFieldPresent("countryOfResidence", 1000);
      bicPage.clickUsingLowLevelActions("countryOfResidence");
      bicPage.clickUsingLowLevelActions("selectCountry");

      bicPage.waitForFieldPresent("stateOrProvince", 1000);
      bicPage.clickUsingLowLevelActions("stateOrProvince");
      bicPage.clickUsingLowLevelActions("selectState");

      bicPage.populateField("postalCode", data.get("postalCode"));
      bicPage.populateField("phoneNumber", data.get("phoneNumber"));

      bicPage.waitForFieldPresent("countryCode", 2000);
      bicPage.clickUsingLowLevelActions("countryCode");
      bicPage.clickUsingLowLevelActions("selectCode");

      bicPage.waitForFieldPresent("freeTrialNextButton4", 2000);
      bicPage.clickUsingLowLevelActions("freeTrialNextButton4");

      bicPage.waitForFieldPresent("caretButton", 2000);
      bicPage.clickUsingLowLevelActions("caretButton");

      if (bicPage.waitForFieldPresent("productDownload", 3000)) {
        bicPage.clickUsingLowLevelActions("productDownload");
      } else {
        Util.printInfo("productDownload not visible. Clicking on download instead.");
        bicPage.clickUsingLowLevelActions("download");
      }

      Util.sleep(5000);
      bicPage.waitForFieldPresent("freeTrialDownloadStartedHeader", 2000);
      String headerText = bicPage.getLinkText("freeTrialDownloadStartedHeader");
      Assert.assertEquals("Free Trial Download not started.", "Your trial has started", headerText);
      results.put(BICECEConstants.DOWNLOAD_STATUS, "Success. ");

    } catch (Exception e) {
      ScreenCapture.getInstance().captureFullScreenshot();
      e.printStackTrace();
      Util.printInfo("Error " + e.getMessage());
      AssertUtils.fail("Unable to Download the product");
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

    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("document.cookie=\"OPTOUTMULTI_TYPE=A\";");
    Util.printInfo("Set session storage data 'nonsensitiveHasNonLocalModalLaunched' from 'loginToOxygen'.");
    js.executeScript("window.sessionStorage.setItem(\"nonsensitiveHasNonLocalModalLaunched\",\"true\");");
    driver.navigate().refresh();

    Util.printInfo("Successfully logged in");
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
      if (System.getProperty("store").equals("STORE-JP")) {
        AssertUtils.assertTrue(Double.compare(cartAmount / 100, pelicanAmount / 100) == 0,
            "Tax Amount in Pelican matches with the tax amount on Checkout page for JP store.");
      } else {
        AssertUtils.assertTrue(Double.compare(cartAmount / 100, pelicanAmount) == 0,
            "Tax Amount in Pelican matches with the tax amount on Checkout page");
      }
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
    if (bicPage.waitForFieldPresent("customerDetailsContinue", 20000)) {
      Util.printInfo("Found Continue button from Customer Details section. Clicking on it.");
      bicPage.clickUsingLowLevelActions("customerDetailsContinue");
      bicPage.waitForElementToDisappear("customerDetailsContinue", 20);
    }

    return bicPage.waitForField("payByInvoiceTab", true, 30000);
  }

  public void goToDotcomSignin(LinkedHashMap<String, String> data) {
    String constructDotComURL = data.get("guacDotComBaseURL") + ".com";

    bicPage.navigateToURL(constructDotComURL);
    Util.sleep(10000);
    bicPage.waitForFieldPresent("signInButton", 10000);
    bicPage.click("signInButton");
  }

  @Step("Oxygen: Load language page " + GlobalConstants.TAG_TESTINGHUB)
  public void goToOxygenLanguageURL(LinkedHashMap<String, String> data) {
    String locale = data.get(BICECEConstants.LOCALE);

    bicPage.navigateToURL(data.get("oxygenLanguageURL"));
    Util.sleep(2000);
    Util.printInfo("The current locale is " + locale);
    try {
      if (Strings.isNotNullAndNotEmpty(locale)) {
        if (locale != "pt_PT" && locale != "fr_CA") {
          locale = locale.substring(0, 2);
        }

        Util.printInfo("Language value to set: " + locale);
        if (System.getProperty(BICECEConstants.ENVIRONMENT).equalsIgnoreCase(BICECEConstants.ENV_STG)) {
          bicPage.navigateToURL(data.get("oxygenLanguageURL"));
          Util.sleep(2000);

          Select selection = new Select(driver.findElement(By.className("input-container__select")));
          selection.selectByValue(locale);
        } else {
          bicPage.click("changeLanguage");
          Select selection = new Select(driver.findElement(By.name("Language")));
          selection.selectByValue(locale);
        }

        bicPage.clickUsingLowLevelActions("saveLanguage");
        Util.sleep(2000);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void validateUserTaxExempt(Boolean shouldPresent) {
    try {
      if (shouldPresent) {
        AssertUtils.assertTrue(bicPage.checkIfElementExistsInPage("taxCertificateProvided", 10),
            "User's tax exemption certificate was accepted");
        Util.printInfo("User's tax exemption certificate was accepted");
      } else {
        AssertUtils.assertFalse(bicPage.checkIfElementExistsInPage("taxCertificateProvided", 10),
            "User's tax exemption certificate was NOT accepted");
        Util.printInfo("User's tax exemption certificate was NOT accepted");
      }

    } catch (MetadataException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to enter LOC Email and CSN
   *
   * @param payByInvoiceDetails
   * @throws MetadataException
   */
  public void enterLOCEmailAndCSN(LinkedHashMap<String, String> payByInvoiceDetails) throws MetadataException {
    Util.sleep(10000);
    //if we refreshed the page, we need to click on continue again
    if (bicPage.waitForFieldPresent("customerDetailsContinue", 20)) {
      bicPage.clickUsingLowLevelActions(("customerDetailsContinue"));
    }

    if (bicPage.waitForFieldPresent("payByInvoiceTab", 10)) {
      bicPage.clickUsingLowLevelActions(("payByInvoiceTab"));
    }

    try {
      if (bicPage.checkIfElementExistsInPage("cartEmailAddress", 10)) {
        Util.printInfo("Entering Payer email and CSN.");
        bicPage.populateField("cartEmailAddress", payByInvoiceDetails.get("payerEmailId"));
        bicPage.populateField("payerCSN", payByInvoiceDetails.get("incorrectCSNNumber"));
        bicPage.waitForFieldPresent("reviewLOCOrder", 10);
        Util.printInfo("clicking continue");
        bicPage.clickUsingLowLevelActions("reviewLOCOrder");
      }

    } catch (Exception e) {
      Util.printInfo("Failed entering Payer email and CSN.");
      Util.sleep(3000);
    }
  }

  /**
   * Method to check if submit order enabled
   *
   * @return boolean true/false
   */
  public boolean isSubmitOrderEnabled() {
    return bicPage.waitForFieldEnabled(BICECEConstants.SUBMIT_ORDER_BUTTON, 15);
  }

  @Step("Validate Pay By Invoice Payment Tab presence" + GlobalConstants.TAG_TESTINGHUB)
  public void validatePayByInvoiceTabPresence() {
    try {
      Util.printInfo("Clicking on Continue..");
      if (bicPage.checkIfElementExistsInPage("customerDetailsContinue", 15)) {
        bicPage.waitForFieldPresent("customerDetailsContinue", 10000);
        Util.sleep(5000);
        bicPage.clickUsingLowLevelActions("customerDetailsContinue");
      }
      Util.sleep(5000);
      Util.printInfo("Landing  on Payment tab..");
      bicPage.checkIfElementExistsInPage("portalPayByInvoice", 10);
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo("Pay By Invoice Payment Tab should not be displayed");
    }
  }

  public void verifyIncorrectPayerDetailsAlertMessage() {
    Util.sleep(3000);
    WebElement alertInvalidMatch = driver.findElement(
        By.xpath(bicPage.getFirstFieldLocator("alertMessage")));
    AssertUtils.assertTrue(
        alertInvalidMatch.getText().contains("The details entered do not match an available line of credit."),
        "No Alert message for Wrong Payer");
  }

  @Step("Navigate back to checkout" + GlobalConstants.TAG_TESTINGHUB)
  public void exitECMS() {
    bicPage.click("ttrReturnToCheckout");
  }

  public void clickOnAddBACSProfileLink() {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    WebElement element = driver
        .findElement(By.xpath("//*[@data-wat-linkname=\"add new bacs\"]"));
    js.executeScript("arguments[0].click();", element);
  }

  @Step("Validate user is not tax exempt" + GlobalConstants.TAG_TESTINGHUB)
  public void validateTaxExemptionIneligibility() {
    try {
      String status = bicPage.getMultipleTextValuesfromField("ttrTaxExemptionStatus")[0];
      AssertUtils.assertTrue(status.contains("Sorry, you are not eligible for tax exemption"));
    } catch (MetadataException e) {
      Util.printError("Failed to validate TTR status");
      throw new RuntimeException(e);
    }
  }

  private void scrollToTopOfThePage() {
    JavascriptExecutor js = ((JavascriptExecutor) driver);
    js.executeScript("window.scrollTo(0, document.body.scrollHeight)");

    Actions at = new Actions(driver);
    at.sendKeys(Keys.PAGE_UP).build().perform();
    at.sendKeys(Keys.PAGE_UP).build().perform();
    at.sendKeys(Keys.PAGE_UP).build().perform();
  }

  public void selectCashPayment() throws MetadataException {
    Util.printInfo("Clicking on cash Payment Tab...");
    bicPage.clickUsingLowLevelActions("cashPaymentTab");
    bicPage.clickUsingLowLevelActions("reviewCashPayment");
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Renew Financing Subscription " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> renewFinancingOrder(LinkedHashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<>();

    skipAddSeats();

    WebElement continueButton = driver
        .findElement(By.cssSelector("[data-wat-value=\"submit order\"]"));

    int attempts = 0;

    while (attempts < 5) {
      try {
        continueButton.click();
        waitForLoadingSpinnerToComplete("loadingSpinner");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.invisibilityOf(continueButton));

        Util.printInfo("Submit order button no longer present.");
        break;
      } catch (TimeoutException e) {
        e.printStackTrace();
        Util.printInfo("Submit order button still present. Retrying!");
        attempts++;
        if (attempts == 5) {
          AssertUtils.fail("Failed to click on Submit order button.");
        }
        Util.sleep(2000);
      }
    }

    financingTestBase.setTestData(data);
    financingTestBase.completeFinancingApplication(data);

    return results;
  }

  public void selectKonbiniPayment() throws MetadataException {
    Util.printInfo("Clicking on Konbini Payment Tab...");
    if (bicPage.checkIfElementExistsInPage("konbiniPaymentTab", 10)) {
      Util.printInfo("Konbini payment method tab is visible");
      bicPage.clickUsingLowLevelActions("konbiniPaymentTab");
    } else if (bicPage.checkIfElementExistsInPage("konbiniRadioButton", 10)) {
      Util.printInfo("Konbini payment method radio button is visible");
      bicPage.clickUsingLowLevelActions("konbiniRadioButton");
    } else {
      AssertUtils.fail("Unable to click on Konbini payment method");
    }
  }

  public void selectPaypalPayment() throws MetadataException {
    Util.printInfo("Clicking on Paypal Payment Tab...");
    if (bicPage.checkIfElementExistsInPage("paypalPaymentTab", 10)) {
      Util.printInfo("Paypal payment method tab is visible");
      bicPage.clickUsingLowLevelActions("paypalPaymentTab");
    } else if (bicPage.checkIfElementExistsInPage("paypalRadioButton", 10)) {
      Util.printInfo("Paypal payment method radio button is visible");
      bicPage.clickUsingLowLevelActions("paypalRadioButton");
    } else {
      AssertUtils.fail("Unable to click on Paypal payment method");
    }
  }

  public void selectConvenienceStoreType() throws MetadataException {
    if (bicPage.checkIfElementExistsInPage("selectStoreType", 10)) {
      bicPage.clickUsingLowLevelActions("selectStoreType");
      String selectCountryOption = bicPage.getFirstFieldLocator("selectStoreTypeOption")
          .replace("<STOREOPTION>", System.getProperty(BICECEConstants.STORE_TYPE_OPTION));
      driver.findElement(By.xpath(selectCountryOption)).click();
      Util.sleep(5000);
      bicPage.waitForFieldPresent("reviewLOCOrder", 10000);
      bicPage.clickUsingLowLevelActions("reviewLOCOrder");
      waitForLoadingSpinnerToComplete("loadingSpinner");
    }
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
