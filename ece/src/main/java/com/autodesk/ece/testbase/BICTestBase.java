package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.EISTestBase;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.JsonParser;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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

  @Step("Generate email id")
  public String generateUniqueEmailID() {
    String storeKey = System.getProperty("store").replace("-", "");
    String sourceName = "thub";
    String emailDomain = "letscheck.pw";

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
  public Map<String, String> getBillingAddress(String region) {
    region = region.replace("/", "").replace("-", "");
    String address = null;
    Map<String, String> ba = null;

    switch (region.toUpperCase()) {
      case "ENAU":
        address = "AutodeskAU@259-261 Colchester Road@Kilsyth@3137@397202088@Australia@Victoria";
        break;
      case "ENUS":
        address = getAmericaAddress();
        break;
      case "ENCA":
        address = "Autodesk@75 Rue Ann@Montreal@H3C 5N5@9916800100@Canada@Quebec";
        break;
      case "ENGB":
        address = "Autodesk@Talbot Way@Birmingham@B10 0HJ@9916800100@United Kingdom";
        break;
      case "NLNL":
        address = "Autodesk@High Way@Noord-Holland@1826 GN@06-30701138@Netherlands@Flevoland";
        break;
      case "DEDE":
        address = "Autodesk@Güntzelstrasse 118@Rorodt@1826 GN@65043235263@Deutschland";
        break;
      case "JAJP":
        address = "Autodesk@532-0003@Street@Osaka@81-6-6350-5223";
        break;
      default:
        Util.printError("Check the region selected");
    }

    String[] billingAddress = address.split("@");
    ba = new HashMap<String, String>();
    ba.put(BICECEConstants.ORGANIZATION_NAME, billingAddress[0]);
    ba.put(BICECEConstants.FULL_ADDRESS, billingAddress[1]);
    ba.put(BICECEConstants.CITY, billingAddress[2]);
    ba.put(BICECEConstants.ZIPCODE, billingAddress[3]);
    ba.put(BICECEConstants.PHONE_NUMBER, getRandomMobileNumber());
    ba.put(BICECEConstants.COUNTRY, billingAddress[5]);

    if (!driver.findElements(By.xpath("//*[@name=\"state\"]")).isEmpty()) {
      ba.put(BICECEConstants.STATE_PROVINCE, billingAddress[6]);
    }

    return ba;
  }

  private String getAmericaAddress() {
    String address = null;

    switch (getRandomIntString()) {
      case "0": {
        address = "Thub@1617 Pearl Street, Suite 200@Boulder@80302@9916800100@United States@CO";
        break;
      }
      case "1": {
        address = "Thub@Novel Coworking Hooper Building@Cincinnati@45207@9916800100@United States@OH";
        break;
      }
      case "2": {
        address = "Thub@1550 Wewatta Street@Denver@80202@9916800100@United States@CO";
        break;
      }
      case "3": {
        address = "Thub@26200 Town Center Drive@Novi@48375@9916800100@United States@MI";
        break;
      }
      case "4": {
        address = "Thub@15800 Pines Blvd, Suite 338@Pittsburgh@15206@9916800100@United States@PA";
        break;
      }
      default:
        address = "Thub@9 Pier@San Francisco@94111@9916800100@United States@CA";
    }
    return address;
  }

  @Step("Create BIC account")
  public void createBICAccount(Names names, String emailID, String password) {
    switchToBICCartLoginPage();
    Util.printInfo("Url is loaded and we were able to switch to iFrame");
    bicPage.waitForField("createNewUserGUAC", true, 30000);
    bicPage.click("createNewUserGUAC");
    bicPage.waitForField(BICECEConstants.BIC_FN, true, 30000);
    bicPage.click(BICECEConstants.BIC_FN);
    bicPage.populateField(BICECEConstants.BIC_FN, names.firstName);
    bicPage.waitForField("bic_LN", true, 30000);
    bicPage.populateField("bic_LN", names.lastName);
    bicPage.populateField("bic_New_Email", emailID);
    bicPage.populateField("bic_New_ConfirmEmail", emailID);
    bicPage.waitForField("bic_Password", true, 30000);
    bicPage.populateField("bic_Password", password);

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

    waitForLoadingSpinnerToComplete();
  }

  private void checkboxTickJS() {
    try {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("document.getElementById('privacypolicy_checkbox').click()");

      js.executeScript("document.getElementById('btnSubmit').click()");
      bicPage.waitForField("verifyAccount", true, 5000);

      if (driver.findElement(By.xpath("//label[@id='optin_checkbox']")).getAttribute("class")
          .contains("checked")) {
        Util.printInfo("Option checkbox is already selected..");
      } else {
        js.executeScript("document.getElementById('optin_checkbox').click()");
      }

      js.executeScript("document.getElementById('bttnAccountVerified').click()");
      bicPage.waitForFieldAbsent("signInSection", 5000);
    } catch (Exception e) {
      AssertUtils.fail("Application Loading issue : Unable to click on privacypolicy_checkbox");
    }
  }

  private void switchToBICCartLoginPage() {
    List<String> elementXpath = bicPage.getFieldLocators("createNewUseriFrame");
    WebElement element = driver.findElement(By.xpath(elementXpath.get(0)));
    driver.switchTo().frame(element);
  }

  @Step("Login BIC account")
  public void loginBICAccount(HashMap<String, String> data) {
    System.out.println(bicPage.isFieldPresent(BICECEConstants.AUTODESK_ID));
    bicPage.click(BICECEConstants.AUTODESK_ID);
    bicPage.waitForField(BICECEConstants.AUTODESK_ID, true, 30000);
    bicPage.populateField(BICECEConstants.AUTODESK_ID, data.get(BICConstants.emailid));
    bicPage.click(BICECEConstants.USER_NAME_NEXT_BUTTON);
    Util.sleep(5000);
    bicPage.click(BICECEConstants.LOGIN_PASSWORD);
    bicPage.waitForField(BICECEConstants.LOGIN_PASSWORD, true, 30000);
    bicPage.populateField(BICECEConstants.LOGIN_PASSWORD, data.get(BICECEConstants.PASSWORD));
    bicPage.clickToSubmit(BICECEConstants.LOGIN_BUTTON, 10000);
    bicPage.waitForPageToLoad();
    Util.sleep(5000);
    boolean status =
        bicPage.isFieldPresent(BICECEConstants.GET_STARTED_SKIP_LINK) || bicPage.isFieldPresent(
            BICECEConstants.GET_STARTED_SKIP_LINK)
            || bicPage.isFieldPresent(BICECEConstants.GET_STARTED_SKIP_LINK);

    if (status) {
      bicPage.click(BICECEConstants.GET_STARTED_SKIP_LINK);
    }

    waitForLoadingSpinnerToComplete();

    skipAddSeats();

    Util.printInfo("Successfully logged into Bic");

  }

  /**
   * Skip the adding of seats in the "Adds Seats" modal
   */
  @Step("Skip add seats modal")
  public void skipAddSeats() {
    Util.printInfo("Finding the skip Button");
    Util.sleep(5000);

    try {
      int count = 0;
      while (driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON))
          .isDisplayed()) {

        Util.printInfo("Finding the Skip button. Attempt # 1");
        driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON)).click();
        count++;
        Util.sleep(1000);
        if (count == 3) {
          Util.printInfo("Finding the Skip button. Tried 3 times. Exiting.");
          AssertUtils.fail("Failed to find or click on Skip Add seats button.");
        }
        if (count == 2) {
          Util.printInfo("Finding the Skip button. Attempt # 3");
          driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON))
              .sendKeys(Keys.ESCAPE);
        }
        if (count == 1) {
          Util.printInfo("Finding the Skip button. Attempt # 2");
          driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON))
              .sendKeys(Keys.PAGE_DOWN);
        }
        Util.printInfo("count : " + count);
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to find or click on Skip Add seats button.");
    }
  }

  @Step("Wait for loading spinner to complete")
  public void waitForLoadingSpinnerToComplete() {
    Util.sleep(2000);
    try {
      int count = 0;
      while (driver.findElement(By.xpath("//*[@data-testid=\"loading\"]"))
          .isDisplayed()) {
        count++;
        Util.sleep(1000);
        if (count == 20) {
          break;
        }
        Util.printInfo("Loading spinner visible: " + count + " second(s)");
      }
    } catch (Exception e) {
    }
  }

  @Step("Login to an existing BIC account")
  public void loginAccount(HashMap<String, String> data) {
    switchToBICCartLoginPage();

    bicPage.click(BICECEConstants.AUTODESK_ID);
    bicPage.waitForField(BICECEConstants.AUTODESK_ID, true, 30000);
    bicPage.populateField(BICECEConstants.AUTODESK_ID, data.get(BICConstants.emailid));
    bicPage.click(BICECEConstants.USER_NAME_NEXT_BUTTON);

    bicPage.click(BICECEConstants.LOGIN_PASSWORD);
    bicPage.waitForField(BICECEConstants.LOGIN_PASSWORD, true, 30000);
    bicPage.populateField(BICECEConstants.LOGIN_PASSWORD, data.get(BICECEConstants.PASSWORD));
    bicPage.clickToSubmit(BICECEConstants.LOGIN_BUTTON, 10000);
    bicPage.waitForPageToLoad();

    boolean status =
        bicPage.isFieldPresent(BICECEConstants.GET_STARTED_SKIP_LINK) || bicPage.isFieldPresent(
            BICECEConstants.GET_STARTED_SKIP_LINK)
            || bicPage.isFieldPresent(BICECEConstants.GET_STARTED_SKIP_LINK);

    if (status) {
      bicPage.click(BICECEConstants.GET_STARTED_SKIP_LINK);
    }

    driver.switchTo().defaultContent();
    bicPage.waitForPageToLoad();
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
        .findElement(By.xpath("//terms-container/div/div[4]/term-element[3]"));
    executor.executeScript("arguments[0].click();", element);
    Util.sleep(2000);
  }

  @Step("Adding to cart")
  public void subscribeAndAddToCart(HashMap<String, String> data) {
    bicPage.waitForField("guacAddToCart", true, 3000);
    bicPage.clickToSubmit("guacAddToCart", 3000);
    bicPage.waitForPageToLoad();
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
    try {
      Util.sleep(2000);
      Util.printInfo("Clicking on Save button...");
      List<WebElement> continueButton = bicPage.getMultipleWebElementsfromField("continueButton");

      if (paymentType.equalsIgnoreCase(BICConstants.paymentTypePayPal)
          || paymentType.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)
          || paymentType.equalsIgnoreCase(BICECEConstants.PAYMENT_BACS)
          || paymentType.equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_SEPA)
          || paymentType.equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_ZIP)
      ) {
        continueButton.get(1).click();
      } else if (paymentType.equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
        continueButton.get(3).click();
      } else if (paymentType.equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
        continueButton.get(2).click();
      } else {
        continueButton.get(0).click();
      }

      bicPage.waitForPageToLoad();
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to click on Save button on billing details page...");
    }
  }

  @Step("Populate Billing Details")
  @SuppressWarnings("static-access")
  private boolean populateBillingDetails(Map<String, String> address, String paymentType) {
    boolean status = false;
    try {
      Util.printInfo("Adding billing details...");
      String orgNameXpath = "", fullAddrXpath = "", cityXpath = "", zipXpath = "", phoneXpath = "", countryXpath = "",
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

      WebDriverWait wait = new WebDriverWait(driver, 60);
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(orgNameXpath)));
      status = driver.findElement(By.xpath(orgNameXpath)).isDisplayed();

      if (status == false) {
        AssertUtils.fail("Organization_Name not available.");
      }
      driver.findElement(By.xpath(orgNameXpath))
          .sendKeys(address.get(
              BICECEConstants.ORGANIZATION_NAME));

      clearTextInputValue(driver.findElement(By.xpath(fullAddrXpath)));
      driver.findElement(By.xpath(fullAddrXpath))
          .sendKeys(address.get(BICECEConstants.FULL_ADDRESS));

      clearTextInputValue(driver.findElement(By.xpath(cityXpath)));
      driver.findElement(By.xpath(cityXpath)).sendKeys(address.get(BICECEConstants.CITY));

      clearTextInputValue(driver.findElement(By.xpath(zipXpath)));
      driver.findElement(By.xpath(zipXpath)).sendKeys(address.get(BICECEConstants.ZIPCODE));

      clearTextInputValue(driver.findElement(By.xpath(phoneXpath)));
      driver.findElement(By.xpath(phoneXpath)).sendKeys("2333422112");

      WebElement countryEle = driver.findElement(By.xpath(countryXpath));
      Select selCountry = new Select(countryEle);
      selCountry.selectByVisibleText(address.get(BICECEConstants.COUNTRY));
      if (address.get(BICECEConstants.STATE_PROVINCE) != null && !address
          .get(BICECEConstants.STATE_PROVINCE).isEmpty()) {
        driver.findElement(By.xpath(stateXpath))
            .sendKeys(address.get(BICECEConstants.STATE_PROVINCE));
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Util.printTestFailedMessage("populateBillingDetails");
      AssertUtils.fail("Unable to Populate Billing Details");
    }
    return status;
  }

  public String getPaymentDetails(String paymentMethod) {

    String paymentDetails = null;

    switch (paymentMethod.toUpperCase()) {
      case "VISA":
        paymentDetails = "4000020000000000@03 - Mar@30@737";
        break;
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
    Util.sleep(20000);
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
    bicPage.waitForField(BICECEConstants.CREDIT_CARD_NUMBER_FRAME, true, 30000);

    try {
      Util.printInfo("Clicking on Sepa tab.");
      bicPage.clickUsingLowLevelActions("sepaPaymentTab");

      Util.printInfo("Waiting for Sepa header.");
      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("sepaHeader").get(0), 10);

      Util.printInfo("Entering IBAN number : " + paymentCardDetails[0]);
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
      Util.sleep(30000);
      String url = driver.getCurrentUrl();
      Util.printInfo("Entering Giropay url" + " : " + url);

      AssertUtils.assertTrue(url.indexOf("giropay") != -1,
          "Current url [" + url + "] does contain keyword : giropay");
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

      int x = (int) ((dimension.getWidth() / 2) + 20);
      int y = (int) ((dimension.getHeight() / 10) + 50);

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

      Util.printInfo("Entering paypal user name [" + data.get("paypalUser") + "]...");
      bicPage.waitForElementVisible(
          bicPage.getMultipleWebElementsfromField("paypalUsernameField").get(0), 10);

      bicPage.populateField("paypalUsernameField", data.get("paypalUser"));

      Util.printInfo("Entering paypal password...");
      bicPage.populateField("paypalPasswordField", data.get("paypalSsap"));

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

  @Step("Submitting Order and Retrieving Order Number")
  public String submitGetOrderNumber(HashMap<String, String> data) {
    clickMandateAgreementCheckbox();
    debugPageUrl(" Checking for Financing Page Url");

    int count = 0;
    debugPageUrl(" Step 1 wait for SubmitOrderButton");
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

    debugPageUrl(" Step 2 wait for SubmitOrderButton");
    try {
      int countModal1 = 0;
      while (driver.findElement(By.xpath("//*[text()='CONTINUE CHECKOUT']")).isDisplayed()) {
        Util.printInfo(" CONTINUE CHECKOUT Modal is present");
        driver.findElement(By.xpath("//*[text()='CONTINUE CHECKOUT']")).click();
        Util.sleep(5000);
        countModal1++;
        if (countModal1 > 3) {
          AssertUtils.fail("Unexpected Pop up in the cart - Please contact TestingHub");
          break;
        }
      }
    } catch (Exception e) {
      Util.printInfo("CONTINUE_CHECKOUT_Modal is not present");
    }

    boolean isOrderSubmitted = false;
    if (!System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_GIROPAY) ||
        !System.getProperty(BICECEConstants.PAYMENT)
            .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      try {
        bicPage.waitForFieldPresent(BICECEConstants.SUBMIT_ORDER_BUTTON, 10000);
        bicPage.clickUsingLowLevelActions(BICECEConstants.SUBMIT_ORDER_BUTTON);
        isOrderSubmitted = true;
      } catch (Exception e) {
        e.printStackTrace();
        debugPageUrl(e.getMessage());
        AssertUtils.fail("Failed to click on Submit button...");
      }
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
      AssertUtils.fail("Failed to click on Submit button...");
    }

    // Zip Pay Checkout
    if (data.get(BICECEConstants.PAYMENT_TYPE).equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_ZIP)) {
      zipTestBase.setTestData(data);
      zipTestBase.zipPayCheckout();
    }

    String orderNumber = null;
    debugPageUrl(" Step 3 Check order Number is Null");
    bicPage.waitForPageToLoad();

    try {
      if (driver.findElement(By.xpath("//*[(text()='Order Processing Problem')]")).isDisplayed()) {
        Util.printInfo("Order Processing Problem");
      }
      AssertUtils.fail("Unable to place BIC order : " + "Order Processing Problem");
    } catch (Exception e) {
      Util.printInfo("Great! Export Compliance issue is not present");
    }

    try {
      if (driver.findElement(By.xpath(
          "//h5[@class='checkout--order-confirmation--invoice-details--export-compliance--label wd-uppercase']"))
          .isDisplayed()) {
        Util.printWarning(
            "Export compliance issue is present. Checking for order number in the Pelican response");
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        String response = (String) executor
            .executeScript(String.format("return sessionStorage.getItem('purchase')"));
        JSONObject jsonObject = JsonParser.getJsonObjectFromJsonString(response);
        JSONObject purchaseOrder = (JSONObject) jsonObject.get("purchaseOrder");
        orderNumber = purchaseOrder.get("id").toString();
        if (orderNumber != null && !orderNumber.isEmpty()) {
          Util.printInfo("Yay! Found the Order Number. Proceeding to next steps...");
        }
      }
    } catch (Exception e) {
      Util.printMessage("Great! Export Compliance issue is not present");
    }
    debugPageUrl(" Step 3a Check order Number is Null");
    try {
      orderNumber = driver.findElement(By.xpath(
          "//p[contains(@class,'checkout--order-confirmation--invoice-details--order-number')]"))
          .getText();
    } catch (Exception e) {
      debugPageUrl(" Step 4 Check order Number is Null");
    }

    try {
      orderNumber = driver.findElement(By.xpath(BICECEConstants.JP_ORDER_NUMBER)).getText();
    } catch (Exception e) {
      debugPageUrl(" Step 4 Check order Number is Null for JP");
    }

    debugPageUrl(" Step 4a Check order Number is Null");

    if (orderNumber == null) {

      bicPage.waitForPageToLoad();

      try {
        orderNumber = driver.findElement(By.xpath(
            "//p[contains(@class,'checkout--order-confirmation--invoice-details--order-number')]"))
            .getText();
      } catch (Exception e) {
        debugPageUrl(" Step 5 Check order Number is Null");
      }
    }

    if (orderNumber == null) {
      try {
        orderNumber = driver.findElement(By.xpath(BICECEConstants.JP_ORDER_NUMBER)).getText();
      } catch (Exception e) {
        debugPageUrl(" Step 5 Check order Number is Null for JP");
      }
    }

    debugPageUrl(" Step 5a Check order Number is Null");

    if (orderNumber == null) {
      try {
        debugPageUrl(" Step 6 Assert order Number is Null");

        orderNumber = driver.findElement(By.xpath("//h2[text()='Order Processing Problem']"))
            .getText();
        debugPageUrl("");
        AssertUtils.fail("Unable to place BIC order : " + orderNumber);
      } catch (Exception e) {
        debugPageUrl(" Step 7 Assert order Number is Null");
        e.printStackTrace();
        Util.printTestFailedMessage("Error while fetching Order Number from Cart");
        AssertUtils.fail("Unable to place BIC order");
      }
    }

    debugPageUrl("Step 8 Check to see if EXPORT COMPLIANCE issue or Null");
    validateBicOrderNumber(orderNumber);

    return orderNumber;
  }

  public void printConsole(String Url, String OrderNumber, String emailID,
      Map<String, String> address,
      String firstName, String lastName, String paymentMethod) {
    Util.printInfo("*************************************************************" + "\n");
    Util.printAssertingMessage("Url to place order       :: " + Url);
    Util.printAssertingMessage("Email Id for the account :: " + emailID);
    Util.printAssertingMessage("First name of the account :: " + firstName);
    Util.printAssertingMessage("Last name of the account  :: " + lastName);
    Util.printAssertingMessage("Address used to place order :: " + address);
    Util.printAssertingMessage("paymentMethod used to place order :: " + paymentMethod);
    Util.printAssertingMessage("Order placed successfully :: " + OrderNumber + "\n");
    Util.printInfo("*************************************************************");
  }

  public void navigateToCart(LinkedHashMap<String, String> data) {

    String guacBaseDotComURL = data.get("guacDotComBaseURL");
    String productName = System.getProperty(BICECEConstants.PRODUCT_NAME) != null ? System
        .getProperty(BICECEConstants.PRODUCT_NAME) : data.get(BICECEConstants.PRODUCT_NAME);

    String constructGuacDotComURL =
        guacBaseDotComURL + data.get(BICECEConstants.COUNTRY_DOMAIN) + data
            .get(BICECEConstants.PRODUCTS_PATH) + productName;

    Util.printInfo("constructGuacURL " + constructGuacDotComURL);

    getUrl(constructGuacDotComURL);
    disableChatSession();

    selectMonthlySubscription(driver);

    subscribeAndAddToCart(data);

    checkCartDetailsError();
    acceptCookiesAndUSSiteLink();
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place GUAC Dot Com Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createGUACBICOrderDotCom(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String guacBaseDotComURL = data.get("guacDotComBaseURL");
    String productName = System.getProperty(BICECEConstants.PRODUCT_NAME) != null ? System
        .getProperty(BICECEConstants.PRODUCT_NAME) : data.get(BICECEConstants.PRODUCT_NAME);
    String term = "";
    String quantity = "";
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String promoCode = data.get(BICECEConstants.PROMO_CODE);

    String emailID = generateUniqueEmailID();
    data.put(BICECEConstants.emailid, emailID);

    String orderNumber = createBICOrderDotCom(data, emailID, guacBaseDotComURL,
        productName, password, paymentMethod, promoCode);

    if (data.get(BICECEConstants.PAYMENT_TYPE)
        .equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      financingTestBase.setTestData(data);
      financingTestBase.completeFinancingApplication();
    }

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  public void disableChatSession() {
    try {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript(String.format(
          "window.sessionStorage.setItem(\"nonsensitiveHasProactiveChatLaunched\",\"true\");"));
    } catch (Exception e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
      System.out.println("test");
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

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(BICECEConstants.DOCUMENT_GETELEMENTBYID_MANDATE_AGREEMENT_CLICK);
        WebElement mandateAgreementElement = driver.findElement(By.xpath(
            BICECEConstants.ID_MANDATE_AGREEMENT));

        Util.printInfo(
            BICECEConstants.CHECKED_MANDATE_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.MANDATE_CHECKBOX_HEADER));
        Util.printInfo(
            BICECEConstants.CHECKED_BOX_STATUS_FOR_MANDATE_CHECKBOX + mandateAgreementElement
                .isEnabled());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String createBICOrderDotCom(LinkedHashMap<String, String> data, String emailID,
      String guacDotComBaseURL,
      String productName,
      String password,
      String paymentMethod, String promocode) throws MetadataException {
    String orderNumber = null;

    String constructGuacDotComURL =
        guacDotComBaseURL + data.get(BICECEConstants.COUNTRY_DOMAIN) + data
            .get(BICECEConstants.PRODUCTS_PATH) + productName;

    System.out.println("constructGuacDotComURL " + constructGuacDotComURL);
    Map<String, String> address = null;
    getUrl(constructGuacDotComURL);
    disableChatSession();
    checkCartDetailsError();
    String productType = data.get("productType");

    // Selecting monthly for Non-Flex, Non-Financing , Non-Meta orders only
    if (productType.equals("flex")) {
      bicPage.clickUsingLowLevelActions("flexTab");
      bicPage.clickUsingLowLevelActions("buyTokensButton");
    } else {
      if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_FINANCING) && (
          data.get(BICECEConstants.OFFERING_TYPE) == null || data.get(
              BICECEConstants.OFFERING_TYPE).equals(BICECEConstants.META))) {
        selectMonthlySubscription(driver);
      }
      subscribeAndAddToCart(data);
    }

    acceptCookiesAndUSSiteLink();

    String region = data.get(BICECEConstants.REGION);
    address = getBillingAddress(region);

    Names names = generateFirstAndLastNames();
    createBICAccount(names, emailID, password);

    if (data.get(BICECEConstants.REDUCE_SEATS) != null && data.get(BICECEConstants.REDUCE_SEATS)
        .equals(BICECEConstants.TRUE)
        && data.get(BICECEConstants.ADD_SEAT_QTY) != null && !data.get(BICECEConstants.ADD_SEAT_QTY)
        .isEmpty()) {
      Util.printInfo("Getting into Reduce Seats ...");
      bicPage.waitForFieldPresent(BICECEConstants.GUAC_CART_EDIT_QUANTITY, 5000);
      try {
        bicPage.populateField(BICECEConstants.GUAC_CART_EDIT_QUANTITY, Keys.BACK_SPACE.name());
        bicPage.sendKeysInTextFieldSlowly(BICECEConstants.GUAC_CART_EDIT_QUANTITY,
            data.get(BICECEConstants.ADD_SEAT_QTY));
        Util.sleep(5000);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (productType.equals("flex")) {
      Select flexOffers = new Select(driver.findElement(By.id("product-term")));
      flexOffers.selectByVisibleText("100 Tokens");
    }

    data.putAll(names.getMap());

    //Apply promo if exists
    if (promocode != null && !promocode.isEmpty()) {
      populatePromoCode(promocode, data);
    }

    enterBillingDetails(data, address, paymentMethod, region);

    if (!paymentMethod.equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      orderNumber = submitGetOrderNumber(data);
      printConsole(constructGuacDotComURL, orderNumber, emailID, address, names.firstName,
          names.lastName,
          paymentMethod);
    }

    return orderNumber;
  }

  private void enterBillingDetails(LinkedHashMap<String, String> data,
      Map<String, String> address, String paymentMethod, String region) {
    String[] paymentCardDetails = getCardPaymentDetails(paymentMethod, region);
    selectPaymentProfile(data, paymentCardDetails, address);

    if (data.get(BICECEConstants.BILLING_DETAILS_ADDED) == null || !data
        .get(BICECEConstants.BILLING_DETAILS_ADDED).equals(BICECEConstants.TRUE)) {
      debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
      populateBillingAddress(address, data);
      debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }
  }

  private void populatePromoCode(String promocode, LinkedHashMap<String, String> data) {
    String priceBeforePromo = null;
    String priceAfterPromo = null;

    try {
      if (driver.findElement(By.xpath("//h2[contains(text(),\"just have a question\")]"))
          .isDisplayed()) {
        bicPage.clickUsingLowLevelActions("promocodePopUpThanksButton");
      }

      priceBeforePromo = bicPage.getValueFromGUI("promocodeBeforeDiscountPrice").trim();
      Util.printInfo("Step : Entering promocode " + promocode + "\n" + " priceBeforePromo : "
          + priceBeforePromo);

      bicPage.clickUsingLowLevelActions("promocodeLink");
      bicPage.clickUsingLowLevelActions("promocodeInput");
      bicPage.populateField("promocodeInput", promocode);
      bicPage.clickUsingLowLevelActions("promocodeSubmit");
      waitForLoadingSpinnerToComplete();
      priceAfterPromo = bicPage.getValueFromGUI("promocodeAfterDiscountPrice").trim();

      Util.printInfo("----------------------------------------------------------------------");
      Util.printInfo(
          "\n" + " priceBeforePromo :  " + priceBeforePromo + "\n" + " priceAfterPromo : "
              + priceAfterPromo);
      Util.printInfo("----------------------------------------------------------------------");

    } catch (Exception e) {
      Util.printTestFailedMessage(
          "Unable to enter the Promocode : " + promocode + "\n" + e.getMessage());
    } finally {
      if (priceAfterPromo.equalsIgnoreCase(priceBeforePromo)) {
        AssertUtils
            .fail("Even after applying the PromoCode, there is not change in the Pricing" + "\n"
                + "priceBeforePromo :  " + priceBeforePromo + "\n"
                + "priceAfterPromo : " + priceAfterPromo);
      } else {
        data.put("priceBeforePromo", priceBeforePromo);
        data.put("priceAfterPromo", priceAfterPromo);
      }
    }
  }

  private String getO2ID(LinkedHashMap<String, String> data, String emailID) {
    OxygenService os = new OxygenService();
    int o2len = 0;
    String o2ID = "";
    try {
      o2ID = os.getOxygenID(emailID, System.getProperty(BICECEConstants.PASSWORD));
      data.put(BICConstants.oxygenid, o2ID);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return o2ID;
  }

  private String validateBicOrderNumber(String orderNumber) {
    Util.printInfo(orderNumber);
    if (!((orderNumber.equalsIgnoreCase("EXPORT COMPLIANCE")) || (orderNumber
        .equalsIgnoreCase("輸出コンプライアンス")))
        || (orderNumber.equalsIgnoreCase("null"))) {
      orderNumber = orderNumber.trim();
    } else {
      Util.printTestFailedMessage(" Cart order " + orderNumber);
      AssertUtils.fail(" Cart order " + orderNumber);
    }
    return orderNumber;
  }

  private void checkCartDetailsError() {
    Util.printInfo("Checking Cart page error...");
    try {
      bicPage.waitForElementVisible(bicPage.getMultipleWebElementsfromField("bicSections").get(0),
          10);
      if (driver.findElement(By.xpath("//*[@data-testid=\"sections\"")).isDisplayed()) {
        System.out.println("Page is loaded");
      } else if (driver
          .findElement(By.xpath("//div[@data-error-code='FETCH_AMART_HTTP_CLIENT_ERROR']"))
          .isDisplayed()) {
        Util.printTestFailedMessage("Error message is displayed while loading Checkout Cart");
        String errorMsg = driver
            .findElement(By.xpath("//div[@data-error-code='FETCH_AMART_HTTP_CLIENT_ERROR']"))
            .getText();
        AssertUtils.fail(errorMsg);
      } else if (driver
          .findElement(By.xpath("//div[@data-error-code='FETCH_DR_HTTP_CLIENT_ERROR']"))
          .isDisplayed()) {
        Util.printTestFailedMessage("Error message is displayed while loading Commerce Cart");
        String errorMsg = driver
            .findElement(By.xpath("//div[@data-error-code='FETCH_DR_HTTP_CLIENT_ERROR']"))
            .getText();
        AssertUtils.fail(errorMsg);
      }
    } catch (Exception e) {
      Util.printInfo("No error on cart page while navigating...");
    }
  }

  @SuppressWarnings("unused")
  @Step("Create BIC Existing User Order Creation via Cart " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBICReturningUser(LinkedHashMap<String, String> data) {
    String orderNumber;
    HashMap<String, String> results = new HashMap<>();
    String region = data.get(BICECEConstants.REGION);
    String paymentMethod = data.get("paymentMethod");

    navigateToCart(data);

    switchToBICCartLoginPage();
    loginBICAccount(data);

    // If the submit button is disabled, fill the payment information out again
    List<WebElement> submitButton = driver.findElements(By.cssSelector(
        "[data-testid=\"order-summary-section\"] .checkout--order-summary-section--submit-order  .checkout--order-summary-section--submit-order--button-container button"));
    if (submitButton.size() > 0 && !submitButton.get(0).isEnabled()) {
      Map<String, String> address = getBillingAddress(region);
      enterBillingDetails(data, address, paymentMethod, region);
    }

    orderNumber = submitGetOrderNumber(data);
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
      LinkedHashMap<String, String> data) {
    String orderNumber;
    HashMap<String, String> results = new HashMap<>();
    String region = data.get("US");

    navigateToCart(data);
    loginAccount(data);
    Util.sleep(5000);
    skipAddSeats();

    orderNumber = submitGetOrderNumber(data);
    Util.printInfo(BICECEConstants.ORDER_NUMBER + orderNumber);

    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @Step("Create BIC Existing User Order Creation via Cart and add seat"
      + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBic_ReturningUserAddSeat(
      LinkedHashMap<String, String> data) {
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
    orderNumber = submitGetOrderNumber(data);
    Util.printInfo(BICECEConstants.ORDER_NUMBER + orderNumber);

    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  public void getUrl(String URL) {
    try {
      driver.manage().deleteAllCookies();
      driver.get(URL);
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
    } while (!(new WebDriverWait(driver, 20).until(
        webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
            .equals("complete"))));
  }

  public String getRandomMobileNumber() {
    Random rnd = new Random();
    long number = rnd.nextInt(999999999);
    number = number + 1000000000;

    return String.format("%09d", number);
  }

  public String getRandomIntString() {
    Date date = new Date();
    long time = date.getTime();
    System.out.println("Time in Milliseconds: " + time);
    Timestamp ts = new Timestamp(time);
    String num = ts.toString().replaceAll("[^0-9]", "");
    System.out.println("num :: " + num);
    System.out.println("option select :: " + num.charAt(12));
    System.out.println(String.valueOf(num.charAt(12)).trim());
    String option = String.valueOf(num.charAt(12)).trim();

    return option;
  }

  public void acceptCookiesAndUSSiteLink() {
    driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
    try {
      WebElement cookieButton = driver.findElement(
          By.xpath("button[@id=\"adsk-eprivacy-yes-to-all\" and .=\"OK\"]"));
      cookieButton.click();
      Util.printInfo("Cookies accepted...");
    } catch (Exception e) {
      Util.printInfo("Cookies accept box does not appear on the page...");
    }

    clickToStayOnSameSite();
    driver.manage().timeouts().implicitlyWait(40, TimeUnit.SECONDS);
  }

  @Step("Guac: Test Trail Download  " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> testCjtTrialDownloadUI(LinkedHashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<String, String>();

    try {
      System.out.println("Entering -> testCjtTrialDownloadUI ");
      getUrl(data.get("trialDownloadUrl"));

      bicPage.clickUsingLowLevelActions("downloadFreeTrialLink");
      bicPage.waitForFieldPresent("downloadFreeTrialPopupNext1", 1000);

      bicPage.clickUsingLowLevelActions("downloadFreeTrialPopupNext1");
      bicPage.waitForFieldPresent("downloadFreeTrialPopupNext2", 1000);

      bicPage.clickUsingLowLevelActions("downloadFreeTrialPopupNext2");
      bicPage.waitForFieldPresent("downloadFreeTrailBusinessUserOption", 1000);

      bicPage.clickUsingLowLevelActions("downloadFreeTrailBusinessUserOption");
      bicPage.waitForFieldPresent("downloadFreeTrialPopupNext3", 1000);

      bicPage.clickUsingLowLevelActions("downloadFreeTrialPopupNext3");
      bicPage.waitForFieldPresent(BICECEConstants.DOWNLOAD_FREE_TRIAL_LOGIN_FRAME, 1000);

      // Checking if download is prompting for user sign in
      if (bicPage.isFieldVisible(BICECEConstants.DOWNLOAD_FREE_TRIAL_LOGIN_FRAME)) {
        bicPage.selectFrame(BICECEConstants.DOWNLOAD_FREE_TRIAL_LOGIN_FRAME);

        bicPage.waitForFieldPresent("downloadFreeTrialUserName", 1000);
        bicPage.sendKeysInTextFieldSlowly("downloadFreeTrialUserName",
            System.getProperty(BICECEConstants.EMAIL));

        bicPage.waitForFieldPresent("downloadFreeTrialVerifyUserButtonClick", 1000);
        bicPage.clickUsingLowLevelActions("downloadFreeTrialVerifyUserButtonClick");

        bicPage.waitForFieldPresent("downloadFreeTrialPassword", 1000);
        bicPage.sendKeysInTextFieldSlowly("downloadFreeTrialPassword",
            System.getProperty(BICECEConstants.PASSWORD));
        Util.sleep(20000);
        bicPage.waitForFieldPresent("downloadFreeTrialSignInButtonClick", 1000);
        bicPage.clickUsingLowLevelActions("downloadFreeTrialSignInButtonClick");
        bicPage.selectMainWindow();
      }

      Util.sleep(10000);
      bicPage.waitForFieldPresent("downloadFreeTrialCompanyName", 1000);
      bicPage.sendKeysInTextFieldSlowly("downloadFreeTrialCompanyName", data.get("companyName"));
      bicPage.clickUsingLowLevelActions("downloadFreeTrialState");
      bicPage.sendKeysInTextFieldSlowly("downloadFreeTrialPostalCode", data.get("postalCode"));
      bicPage.sendKeysInTextFieldSlowly("downloadFreeTrialPhoneNo", data.get("phoneNumber"));
      bicPage.clickUsingLowLevelActions("downloadFreeTrialBeginDownloadLink");

      bicPage.waitForFieldPresent("downloadFreeTrialStarted", 5000);
      boolean downloadStarted = bicPage.isFieldVisible("downloadFreeTrialStarted");
      Util.sleep(2000);
      AssertUtils.assertEquals(downloadStarted, true, "SUCCESSFULLY STARTED DOWNLOAD");
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
    } catch (Exception e) {
      AssertUtils.fail("Application Loading issue : Unable to click on 'order-agreement' checkbox");
    }
    Util.sleep(1000);
  }

  public void loginToOxygen(String emailID, String password) {
    bicPage.waitForPageToLoad();
    Util.sleep(60000);
    try {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("document.getElementById('meMenu-avatar-flyout').click()");
      bicPage.waitForPageToLoad();
      js.executeScript("document.getElementById('meMenu-signOut').click()");
      bicPage.waitForPageToLoad();
    } catch (Exception e) {
      AssertUtils.fail("Application Loading issue : Unable to logout");
    }
    bicPage.waitForField(BICECEConstants.AUTODESK_ID, true, 30000);
    bicPage.populateField(BICECEConstants.AUTODESK_ID, emailID);
    bicPage.click(BICECEConstants.USER_NAME_NEXT_BUTTON);
    bicPage.waitForField(BICECEConstants.LOGIN_PASSWORD, true, 5000);
    bicPage.click(BICECEConstants.LOGIN_PASSWORD);
    bicPage.populateField(BICECEConstants.LOGIN_PASSWORD, password);
    bicPage.clickToSubmit(BICECEConstants.LOGIN_BUTTON, 10000);
    bicPage.waitForPageToLoad();

    if (bicPage.isFieldPresent(BICECEConstants.GET_STARTED_SKIP_LINK)) {
      bicPage.click(BICECEConstants.GET_STARTED_SKIP_LINK);
    }

    Util.printInfo("Successfully logged in");
  }

  public Names generateFirstAndLastNames() {
    String randomString = RandomStringUtils.random(6, true, false);
    String firstName = "FN" + randomString;
    Util.printInfo(BICECEConstants.FIRST_NAME1 + firstName);
    String lastName = "LN" + randomString;
    Util.printInfo(BICECEConstants.LAST_NAME1 + lastName);
    return new Names(firstName, lastName);
  }

  private String[] getCardPaymentDetails(String paymentMethod, String region) {
    debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);
    return getPaymentDetails(paymentMethod.toUpperCase()).split("@");
  }

  private void clickToStayOnSameSite() {
    try {
      bicPage.waitForFieldPresent("bicStayOnSameSite", 5000);
      bicPage.clickUsingLowLevelActions("bicStayOnSameSite");
      Util.printInfo("Clicked on Stay On Same page link...");
    } catch (Exception e) {
      Util.printInfo("Stay on US Site link is not displayed...");
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
