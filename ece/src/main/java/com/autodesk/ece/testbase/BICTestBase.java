package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.JsonParser;
import com.autodesk.testinghub.core.utils.Util;
import com.google.common.base.Strings;
import io.qameta.allure.Step;
import java.sql.Timestamp;
import java.util.ArrayList;
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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BICTestBase {

  public static Page_ bicPage = null;
  public WebDriver driver;

  public BICTestBase(WebDriver driver, GlobalTestBase testbase) {
    Util.PrintInfo("BICTestBase from ece");
    this.driver = driver;
    bicPage = testbase.createPage("PAGE_BIC_CART");
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
  public static String generateUniqueEmailID() {
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
      case "EMEA":
        address = "Autodesk@Talbot Way@Birmingham@B10 0HJ@9916800100@United Kingdom";
        break;
      case "NLNL":
        address = "Autodesk@High Way@Noord-Holland@1826 GN@06-30701138@Netherlands@Flevoland";
        break;
      case "JAJP":
        address = "Autodesk@532-0003@Street@81-6-6350-5223";
        break;
      default:
        Util.printError("Check the region selected");
    }

    String[] billingAddress = address.split("@");
    if (region.equalsIgnoreCase("jajp")) {
      ba = new HashMap<String, String>();
      ba.put(BICECEConstants.COMPANY_NAME_DR, billingAddress[0]);
      ba.put(BICECEConstants.POSTAL_CODE_DR, billingAddress[1]);
      ba.put(BICECEConstants.ADDRESS_DR, billingAddress[2]);
      ba.put(BICECEConstants.PHONE_NUMBER_DR, billingAddress[3]);
    } else {
      ba = new HashMap<String, String>();
      ba.put(BICECEConstants.ORGANIZATION_NAME, billingAddress[0]);
      ba.put(BICECEConstants.FULL_ADDRESS, billingAddress[1]);
      ba.put(BICECEConstants.CITY, billingAddress[2]);
      ba.put(BICECEConstants.ZIPCODE, billingAddress[3]);
      ba.put(BICECEConstants.PHONE_NUMBER, getRandomMobileNumber());
      ba.put(BICECEConstants.COUNTRY, billingAddress[5]);
      if (!region.equalsIgnoreCase("EMEA")) {
        ba.put(BICECEConstants.STATE_PROVINCE, billingAddress[6]);
      }
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
        address = "Thub@1617 Pearl Street, Suite 200@Boulder@80302@9916800100@United States@CO";
        break;
      }
      case "2": {
        address = "Thub@Novel Coworking Hooper Building@Cincinnati@45207@9916800100@United States@OH";
        break;
      }
      case "3": {
        address = "Thub@1550 Wewatta Street@Denver@80202@9916800100@United States@CO";
        break;
      }
      case "7": {
        address = "Thub@26200 Town Center Drive@Novi@48375@9916800100@United States@MI";
        break;
      }
      case "8": {
        address = "Thub@15800 Pines Blvd, Suite 338@Pittsburgh@15206@9916800100@United States@PA";
        break;
      }
      default:
        address = "Thub@9 Pier@San Francisco@94111@9916800100@United States@CA";
    }
    return address;
  }

  @Step("Create BIC account")
  public void createBICAccount(String firstName, String lastName, String emailID, String password) {
    switchToBICCartLoginPage();
    Util.printInfo("Url is loaded and we were able to switch to iFrame");
    bicPage.waitForField("createNewUserGUAC", true, 30000);
    bicPage.click("createNewUserGUAC");
    bicPage.waitForField(BICECEConstants.BIC_FN, true, 30000);
    bicPage.click(BICECEConstants.BIC_FN);
    bicPage.populateField(BICECEConstants.BIC_FN, firstName);
    bicPage.waitForField("bic_LN", true, 30000);
    bicPage.populateField("bic_LN", lastName);
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
    try {
      int count = 0;
      while (driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON))
          .isDisplayed()) {
        driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON)).click();
        count++;
        Util.sleep(1000);
        if (count == 3) {
          break;
        }
        if (count == 2) {
          driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON))
              .sendKeys(Keys.ESCAPE);
        }
        if (count == 1) {
          driver.findElement(By.xpath(BICECEConstants.ADD_SEATS_MODAL_SKIP_BUTTON))
              .sendKeys(Keys.PAGE_DOWN);
        }
        Util.printInfo("count : " + count);
      }
    } catch (Exception e) {
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

    bicPage.waitForPageToLoad();
  }

  @Step("Add a seat from the existing subscription popup")
  public void existingSubscriptionAddSeat(HashMap<String, String> data) {
    // Wait for add seats popup
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
  public boolean populateBillingAddress(Map<String, String> address, HashMap<String, String> data) {

    boolean status = false;
    try {
      String paymentType = System.getProperty(BICECEConstants.PAYMENT);
      String firstNameXpath = "";
      String lastNameXpath = "";
      Util.sleep(5000);

      if (data.get(BICECEConstants.PAYMENT_TYPE).equalsIgnoreCase(BICConstants.paymentTypePayPal)) {
        firstNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        lastNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
      } else if (data.get(BICECEConstants.PAYMENT_TYPE)
          .equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
        firstNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
        lastNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME).replace(
            BICECEConstants.PAYMENT_PROFILE, "ach");
      } else {
        firstNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        lastNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
      }

      clearTextInputValue(driver.findElement(By.xpath(firstNameXpath)));
      Util.sleep(1000);
      driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get(BICECEConstants.FIRSTNAME));

      clearTextInputValue(driver.findElement(By.xpath(lastNameXpath)));
      Util.sleep(1000);
      driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get(BICECEConstants.LASTNAME));
      status = populateBillingDetails(address, paymentType);
      clickOnContinueBtn(paymentType);
    } catch (Exception e) {
      e.printStackTrace();
      debugPageUrl(e.getMessage());
      AssertUtils.fail("Unable to populate the Billing Address details");
    }
    return status;
  }

  private void debugPageUrl(String message) {
    Util.printInfo("-------------" + message + "----------------" +
        "\n" + " URL :            " + driver.getCurrentUrl() + "\n" +
        "\n" + " Page Title :     " + driver.getTitle() + "\n" +
        "\n" + "-----------------------------");
  }

  public void clickOnContinueBtn(String paymentType) {
    try {
      Util.sleep(2000);
      Util.printInfo("Clicking on Save button...");
      List<WebElement> eles = bicPage.getMultipleWebElementsfromField("continueButton");

      if (paymentType.equalsIgnoreCase(BICConstants.paymentTypePayPal)
          || paymentType.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
        eles.get(1).click();
      } else {
        eles.get(0).click();
      }

      bicPage.waitForPageToLoad();
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to click on Save button on billing details page...");
    }
  }

  @SuppressWarnings("static-access")
  private boolean populateBillingDetails(Map<String, String> address, String paymentType) {
    boolean status = false;
    try {
      Util.printInfo("Adding billing details...");

      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("document.getElementById('checkout--liveagent--close-button').click();");
      String orgNameXpath = "", fullAddrXpath = "", cityXpath = "", zipXpath = "", phoneXpath = "", countryXpath = "",
          stateXpath = "";
      switch (paymentType.toUpperCase()) {
        case BICConstants.paymentTypePayPal:
          orgNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
          fullAddrXpath = bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
          cityXpath = bicPage.getFirstFieldLocator(BICECEConstants.CITY).replace(
              BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
          zipXpath = bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE).replace(
              BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
          phoneXpath = bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
          countryXpath = bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
          stateXpath = bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
          break;
        case BICConstants.paymentTypeDebitCard:
          orgNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
              .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
          fullAddrXpath = bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
              .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
          cityXpath = bicPage.getFirstFieldLocator(BICECEConstants.CITY).replace(
              BICECEConstants.PAYMENT_PROFILE, "ach");
          zipXpath = bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE).replace(
              BICECEConstants.PAYMENT_PROFILE, "ach");
          phoneXpath = bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
              .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
          countryXpath = bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY).replace(
              BICECEConstants.PAYMENT_PROFILE, "ach");
          stateXpath = bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
              .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
          break;
        default:
          orgNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
          fullAddrXpath = bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
          cityXpath = bicPage.getFirstFieldLocator(BICECEConstants.CITY)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
          zipXpath = bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
          phoneXpath = bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
          countryXpath = bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
          stateXpath = bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
              .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
          break;
      }

      Util.sleep(1000);
      WebDriverWait wait = new WebDriverWait(driver, 60);
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(orgNameXpath)));
      status = driver.findElement(By.xpath(orgNameXpath)).isDisplayed();

      if (status == false) {
        AssertUtils.fail("Organization_Name not available.");
      }

      driver.findElement(By.xpath(orgNameXpath)).click();
      driver.findElement(By.xpath(orgNameXpath))
          .sendKeys( address.get(
                  BICECEConstants.ORGANIZATION_NAME));

      driver.findElement(By.xpath(orgNameXpath)).click();

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
      Util.printInfo("THE ADDRESS "+address.toString());
      if(address.get(BICECEConstants.STATE_PROVINCE) != null && !address.get(BICECEConstants.STATE_PROVINCE).isEmpty()) {
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

  private void clickContinueCartBillingPage() {
    if (System.getProperty("environment").equalsIgnoreCase("stg")) {
      try {
        bicPage.clickUsingLowLevelActions("billingContinue");
      } catch (MetadataException e) {
        e.printStackTrace();
        AssertUtils.fail("Unable to click on continue button under Billing page in BIC-Cart");
      }
    }
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
      bicPage.selectFrame("paypalCheckoutOptionFrame");
      bicPage.clickUsingLowLevelActions("paypalCheckoutBtn");

      Set<String> windows = driver.getWindowHandles();
      for (String window : windows) {
        driver.switchTo().window(window);
      }

      driver.manage().window().maximize();
      bicPage.waitForPageToLoad();
      bicPage.waitForElementToDisappear("paypalPageLoader", 30);

      String title = driver.getTitle();
      AssertUtils.assertTrue(title.contains("Log in"),
          "Current title [" + title + "] does not contains keyword : PayPal");

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
      if(bicPage.checkFieldExistence("paypalContinueButton")) {
        bicPage.clickUsingLowLevelActions("paypalContinueButton");
      }
      Util.printInfo("Clicking on agree and continue button...");
      bicPage.waitForFieldPresent("paypalAgreeAndContBtn", 10000);
      bicPage.clickUsingLowLevelActions("paypalAgreeAndContBtn");
      Util.sleep(10000);

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

  private void validateStore(HashMap<String, String> data){

  }
  public void selectPaymentProfile(HashMap<String, String> data, String[] paymentCardDetails) {
    try {

      Util.printInfo("Selecting payment profile : " + data.get(BICECEConstants.PAYMENT_TYPE));

      String[] paymentMethods = data.get(BICECEConstants.PAYMENT_METHODS).split(",");
      boolean isValidPaymentType = false;

      for(String paymentMethod : paymentMethods){
        if(paymentMethod.equals(data.get(BICECEConstants.PAYMENT_TYPE))){
           isValidPaymentType = true;
           break;
         }
      }
      if(isValidPaymentType){
        switch (data.get(BICECEConstants.PAYMENT_TYPE).toUpperCase()) {
          case BICConstants.paymentTypePayPal:
            populatePaypalPaymentDetails(data);
            break;
          case BICConstants.paymentTypeDebitCard:
            populateACHPaymentDetails(paymentCardDetails);
            break;
          default:
            populatePaymentDetails(paymentCardDetails);
            break;
      }
     }else{
        AssertUtils.fail("The payment method is not supported for the given country/locale : "+ data.get("locale") +". Supported payment methods are "
            + data.get(BICECEConstants.PAYMENT_METHODS));
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to select payment profile...");
    }
  }

  private String submitGetOrderNumber() {
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
      if (System.getProperty(BICECEConstants.PAYMENT)
          .equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
        Util.printInfo(
            BICECEConstants.CHECKED_ACH_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.ACH_CHECKBOX_HEADER));
        Util.printInfo(BICECEConstants.CHECKED_BOX_STATUS_FOR_ACH_CHECKBOX + bicPage.isChecked(
            BICECEConstants.ACH_CHECKBOX));

        WebElement achAgreeElement = driver.findElement(By.xpath(
            BICECEConstants.ID_MANDATE_AGREEMENT));
        if (!achAgreeElement.isEnabled()) {
          JavascriptExecutor js = (JavascriptExecutor) driver;
          js.executeScript(BICECEConstants.DOCUMENT_GETELEMENTBYID_MANDATE_AGREEMENT_CLICK);
        }
        Util.printInfo(
            BICECEConstants.CHECKED_ACH_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.ACH_CHECKBOX_HEADER));
        Util.printInfo(
            BICECEConstants.CHECKED_BOX_STATUS_FOR_ACH_CHECKBOX + achAgreeElement.isEnabled());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

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

    try {
      bicPage.waitForFieldPresent(BICECEConstants.SUBMIT_ORDER_BUTTON, 10000);
      bicPage.clickUsingLowLevelActions(BICECEConstants.SUBMIT_ORDER_BUTTON);
      bicPage.waitForPageToLoad();
    } catch (Exception e) {
      e.printStackTrace();
      debugPageUrl(e.getMessage());
      AssertUtils.fail("Failed to click on Submit button...");
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
      orderNumber = driver.findElement(By.xpath("//h5[.='Order Number']/..//p")).getText();
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
        orderNumber = driver.findElement(By.xpath("//h5[.='Order Number:']/..//p")).getText();
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
    return orderNumber;
  }

  public void sendKeysAction(String locator, String data) throws MetadataException {
    Actions act = new Actions(driver);
    WebElement element = bicPage.getMultipleWebElementsfromField(locator).get(0);
    act.sendKeys(element, data).perform();
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

  private void navigateToGUAC(LinkedHashMap<String, String> data, String region) {
    String guacBaseURL = data.get("guacBaseURL");
    String guacResourceURL = data.get(BICECEConstants.GUAC_RESOURCE_URL);
    String productID = "";
    String quantity = "";

    if (System.getProperty("sku").contains("default")) {
      productID = data.get("productID");
      quantity = data.get("quantity");
    } else {
      String sku = System.getProperty("sku");
      productID = sku.split(":")[0];
      quantity = sku.split(":")[1];
    }

    String constructGuacURL =
        guacBaseURL + region + guacResourceURL + productID + "[qty:" + quantity + "]";
    System.out.println("constructGuacURL " + constructGuacURL);

    getUrl(constructGuacURL);
    disableChatSession();
    checkCartDetailsError();
    acceptCookiesAndUSSiteLink();
  }

  private void navigateToCart(LinkedHashMap<String, String> data, String region) {

    String guacBaseDotComURL = data.get("guacDotComBaseURL");
    String productName = data.get("productName");
    String guacOverviewResourceURL = data.get("guacOverviewTermResource");

    String constructGuacDotComURL = guacBaseDotComURL + productName + guacOverviewResourceURL;

    System.out.println("constructGuacURL " + constructGuacDotComURL);

    getUrl(constructGuacDotComURL);
    disableChatSession();

    selectMonthlySubscription(driver);
    subscribeAndAddToCart(data);

    checkCartDetailsError();
    acceptCookiesAndUSSiteLink();
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place GUAC Dot Com Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createGUACBICOrderDotCom(LinkedHashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<>();
    String guacBaseDotComURL = data.get("guacDotComBaseURL");
    String productName = data.get(BICECEConstants.PRODUCT_NAME);
    String term = "";
    String quantity = "";
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String promoCode = data.get(BICECEConstants.PROMO_CODE);


    String emailID = generateUniqueEmailID();

    String orderNumber = createBICOrderDotCom(data, emailID, guacBaseDotComURL,
        productName, term, region, quantity, password, paymentMethod, promoCode);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createGUACBICOrderUS(LinkedHashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<>();

    String guacResourceURL = data.get(BICECEConstants.GUAC_RESOURCE_URL);
    String userType = data.get(BICECEConstants.USER_TYPE);
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String region = data.get(BICECEConstants.LANGUAGE_STORE);

    navigateToGUAC(data, region);

    String emailID = generateUniqueEmailID();
    String orderNumber = createBICOrder(data, emailID, region, password, paymentMethod);

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

  private String createBICOrder(LinkedHashMap<String, String> data, String emailID, String region,
      String password, String paymentMethod) {
    String orderNumber;
    String firstName = null, lastName = null;
    Map<String, String> address = null;

    firstName = null;
    lastName = null;
    String randomString = RandomStringUtils.random(6, true, false);

    region = region.replace("/", "").replace("-", "");
    address = getBillingAddress(region);
    String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");

    firstName = "FN" + randomString;
    Util.printInfo(BICECEConstants.FIRST_NAME1 + firstName);
    lastName = "LN" + randomString;
    Util.printInfo(BICECEConstants.LAST_NAME1 + lastName);
    createBICAccount(firstName, lastName, emailID, password);

    data.put(BICECEConstants.FIRSTNAME, firstName);
    data.put(BICECEConstants.LASTNAME, lastName);

    debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);
    // Get Payment details
    selectPaymentProfile(data, paymentCardDetails);
    // Enter billing details
    debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);

    populateBillingAddress(address, data);
    debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);

    try {
      if (paymentMethod.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
        Util.printInfo(
            BICECEConstants.CHECKED_ACH_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.ACH_CHECKBOX_HEADER));
        Util.printInfo(BICECEConstants.CHECKED_BOX_STATUS_FOR_ACH_CHECKBOX + bicPage.isChecked(
            BICECEConstants.ACH_CHECKBOX));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(BICECEConstants.DOCUMENT_GETELEMENTBYID_MANDATE_AGREEMENT_CLICK);
        WebElement achAgreeElement = driver.findElement(By.xpath(
            BICECEConstants.ID_MANDATE_AGREEMENT));

        Util.printInfo(
            BICECEConstants.CHECKED_ACH_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.ACH_CHECKBOX_HEADER));
        Util.printInfo(
            BICECEConstants.CHECKED_BOX_STATUS_FOR_ACH_CHECKBOX + achAgreeElement.isEnabled());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    orderNumber = submitGetOrderNumber();

    // Check to see if EXPORT COMPLIANCE or Null
    validateBicOrderNumber(orderNumber);
    printConsole(driver.getCurrentUrl(), orderNumber, emailID, address, firstName, lastName,
        paymentMethod);

    return orderNumber;
  }

  private String createBICOrderDotCom(LinkedHashMap<String, String> data, String emailID,
      String guacDotComBaseURL,
      String productName, String term, String region,
      String quantity, String password,
      String paymentMethod,String promocode) {
    String orderNumber;
    String constructGuacDotComURL = guacDotComBaseURL + data.get(BICECEConstants.COUNTRY_DOMAIN) + data.get(BICECEConstants.PRODUCTS_PATH) + productName;

    System.out.println("constructGuacDotComURL " + constructGuacDotComURL);
    String firstName = null, lastName = null;
    Map<String, String> address = null;
    getUrl(constructGuacDotComURL);
    disableChatSession();
    checkCartDetailsError();
    selectMonthlySubscription(driver);
    subscribeAndAddToCart(data);

    acceptCookiesAndUSSiteLink();

    firstName = null;
    lastName = null;
    String randomString = RandomStringUtils.random(6, true, false);

    region = data.get("region");
    address = getBillingAddress(region);
    String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");



    firstName = "FN" + randomString;
    Util.printInfo(BICECEConstants.FIRST_NAME1 + firstName);
    lastName = "LN" + randomString;
    Util.printInfo(BICECEConstants.LAST_NAME1 + lastName);
    createBICAccount(firstName, lastName, emailID, password);
    Util.sleep(20000);

    if (data.get(BICECEConstants.ADD_SEAT_QTY) != null && !data.get(BICECEConstants.ADD_SEAT_QTY)
        .isEmpty()) {
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

    data.put(BICECEConstants.FIRSTNAME, firstName);
    data.put(BICECEConstants.LASTNAME, lastName);

    //Apply promo if exists
    if (promocode != null && !promocode.isEmpty()) {
      populatePromoCode(promocode, data);
    }

    debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);
    // Get Payment details
    selectPaymentProfile(data, paymentCardDetails);
    // Enter billing details
    debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);

    populateBillingAddress(address, data);
    debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);

    try {
      if (paymentMethod.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
        Util.printInfo(
            BICECEConstants.CHECKED_ACH_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.ACH_CHECKBOX_HEADER));
        Util.printInfo(
            BICECEConstants.CHECKED_BOX_STATUS_FOR_ACH_CHECKBOX + bicPage.isChecked(
                BICECEConstants.ACH_CHECKBOX));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(BICECEConstants.DOCUMENT_GETELEMENTBYID_MANDATE_AGREEMENT_CLICK);
        WebElement achAgreeElement = driver.findElement(By.xpath(
            BICECEConstants.ID_MANDATE_AGREEMENT));

        Util.printInfo(
            BICECEConstants.CHECKED_ACH_AUTHORIZATION_AGREEMENT_IS_VISIBLE + bicPage
                .isFieldVisible(BICECEConstants.ACH_CHECKBOX_HEADER));
        Util.printInfo(
            BICECEConstants.CHECKED_BOX_STATUS_FOR_ACH_CHECKBOX + achAgreeElement.isEnabled());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    orderNumber = submitGetOrderNumber();

    // Check to see if EXPORT COMPLIANCE or Null
    validateBicOrderNumber(orderNumber);
    printConsole(constructGuacDotComURL, orderNumber, emailID, address, firstName, lastName,
        paymentMethod);

    return orderNumber;
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
    String region = data.get("US");
    String paymentMethod = data.get("paymentMethod");

    navigateToCart(data, region);

    switchToBICCartLoginPage();
    loginBICAccount(data);
    orderNumber = submitGetOrderNumber();
    validateBicOrderNumber(orderNumber);
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

    navigateToCart(data, region);

  //  skipAddSeats();

    orderNumber = submitGetOrderNumber();
    validateBicOrderNumber(orderNumber);
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
    String region = data.get("US");

    // Go to checkout with a product that was already added
    navigateToCart(data, region);

    // Login to an existing account and add seats
    loginAccount(data);
    existingSubscriptionAddSeat(data);
    orderNumber = submitGetOrderNumber();
    validateBicOrderNumber(orderNumber);
    Util.printInfo(BICECEConstants.ORDER_NUMBER + orderNumber);

    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  private void getUrl(String URL) {
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

  private void retryLoadingURL(String URL) {
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

    try {
      bicPage.waitForFieldPresent("bicStayOnSameSite",5000);
      bicPage.clickUsingLowLevelActions("bicStayOnSameSite");
      Util.printInfo("Clicked on Stay On Same page link...");
    } catch (Exception e) {
      Util.printInfo("Stay on US Site link is not displayed...");
    }
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

        bicPage.waitForFieldPresent("downloadFreeTrialSignInButtonClick", 1000);
        bicPage.clickUsingLowLevelActions("downloadFreeTrialSignInButtonClick");
      }

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

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBicOrderMoe(LinkedHashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<>();
    String guacBaseURL = data.get("guacBaseURL");
    String productID = "";
    String quantity = "";
    String guacResourceURL = data.get(BICECEConstants.GUAC_RESOURCE_URL);
    String guacMoeResourceURL = data.get("guacMoeResourceURL");
    String cepURL = data.get("cepURL");
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.LANGUAGE_STORE);
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);

    navigateToCart(data, region);

    String emailID = generateUniqueEmailID();
    String orderNumber = getBicOrderMoe(data, emailID, guacBaseURL, guacMoeResourceURL,
        region, password, paymentMethod, cepURL);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  private String getBicOrderMoe(LinkedHashMap<String, String> data, String emailID,
      String guacBaseURL, String guacMoeResourceURL, String region, String password,
      String paymentMethod, String cepURL) {
    String orderNumber;
    String constructGuacMoeURL = guacBaseURL + region + guacMoeResourceURL;
    System.out.println("constructGuacMoeURL " + constructGuacMoeURL);
    String constructPortalUrl = cepURL;
    String firstName = null, lastName = null;
    Map<String, String> address = null;

    firstName = null;
    lastName = null;
    String randomString = RandomStringUtils.random(6, true, false);

    region = region.replace("/", "").replace("-", "");
    address = getBillingAddress(region);
    String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");

    firstName = "FN" + randomString;
    Util.printInfo(BICECEConstants.FIRST_NAME1 + firstName);
    lastName = "LN" + randomString;
    Util.printInfo(BICECEConstants.LAST_NAME1 + lastName);
    createBICAccount(firstName, lastName, emailID, password);

    data.put(BICECEConstants.FIRSTNAME, firstName);
    data.put(BICECEConstants.LASTNAME, lastName);

    debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);

    // Get Payment details
    selectPaymentProfile(data, paymentCardDetails);

    // Enter billing details
    debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);

    populateBillingAddress(address, data);
    debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);

    getUrl(constructGuacMoeURL);
    loginToMoe();
    emulateUser(emailID);
    agreeToTerm();

    orderNumber = submitGetOrderNumber();

    // Check to see if EXPORT COMPLIANCE or Null
    validateBicOrderNumber(orderNumber);
    printConsole(constructGuacMoeURL, orderNumber, emailID, address, firstName, lastName,
        paymentMethod);

    // Navigate to Portal, logout from service account session and log back in with user account
    getUrl(constructPortalUrl);
    loginToOxygen(emailID, password);

    return orderNumber;
  }

  private void loginToMoe() {
    Util.printInfo("MOE - Re-Login");
    if (bicPage.isFieldVisible("moeReLoginLink")) {
      try {
        bicPage.clickUsingLowLevelActions("moeReLoginLink");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    bicPage.waitForField(BICECEConstants.MOE_LOGIN_USERNAME_FIELD, true, 30000);
    bicPage.click(BICECEConstants.MOE_LOGIN_USERNAME_FIELD);
    bicPage.populateField(BICECEConstants.MOE_LOGIN_USERNAME_FIELD, "svc_s_guac@autodesk.com");
    bicPage.click("moeLoginButton");
    bicPage.waitForField(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD, true, 30000);
    bicPage.click(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD);
    bicPage.populateField(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD, "K16PF6LCtnsf99");
    bicPage.click("moeLoginButton");
    bicPage.waitForPageToLoad();
    Util.printInfo("Successfully logged into MOE");
  }

  private void emulateUser(String emailID) {
    Util.printInfo("MOE - Emulate User");
    bicPage.click("moeAccountLookupEmail");
    bicPage.populateField("moeAccountLookupEmail", emailID);
    bicPage.click("moeAccountLookupBtn");
    bicPage.waitForPageToLoad();
    bicPage.click("moeContinueBtn");
    bicPage.waitForPageToLoad();
    Util.printInfo("Successfully emulated user");
  }

  private void agreeToTerm() {
    try {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("document.getElementById('order-agreement').click()");
    } catch (Exception e) {
      AssertUtils.fail("Application Loading issue : Unable to click on 'order-agreement' checkbox");
    }
    Util.sleep(1000);
  }

  private void loginToOxygen(String emailID, String password) {
    bicPage.waitForPageToLoad();
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

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place DR Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createGUACBICIndirectOrderJP(LinkedHashMap<String, String> data) {
    String orderNumber = null;
    String emailID = null;
    HashMap<String, String> results = new HashMap<>();
    String guacDRBaseURL = data.get("guacDRBaseURL");
    String productID = "";
    String quantity = "";
    String guacDRResourceURL = data.get("guacDRResourceURL");
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get("languageStoreDR");
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);

    if (System.getProperty("sku").contains("default")) {
      productID = data.get("productID");
    } else {
      String sku = System.getProperty("sku");
      productID = sku.split(":")[0];
      quantity = "&quantity=" + sku.split(":")[1];
    }

    if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
      emailID = System.getProperty("email");
      String O2ID = getO2ID(data, emailID);
      // New user to be created
      if ((Strings.isNullOrEmpty(O2ID))) {
        orderNumber = createBicIndirectOrder(data, emailID, guacDRBaseURL, productID, quantity,
            guacDRResourceURL,
            region, password, paymentMethod);
      }
    } else {
      String timeStamp = new RandomStringUtils().random(12, true, false);
      emailID = generateUniqueEmailID();
      orderNumber = createBicIndirectOrder(data, emailID, guacDRBaseURL, productID, quantity,
          guacDRResourceURL, region,
          password, paymentMethod);
    }

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  private String createBicIndirectOrder(LinkedHashMap<String, String> data, String emailID,
      String guacDRBaseURL,
      String productID, String quantity, String guacDRResourceURL, String region, String password,
      String paymentMethod) {
    String orderNumber;
    String constructGuacDRURL = guacDRBaseURL + region + guacDRResourceURL + productID + quantity;
    System.out.println("constructGuacDRURL " + constructGuacDRURL);
    String firstName = null, lastName = null;
    Map<String, String> address = null;

    getUrl(constructGuacDRURL);
    disableChatSession();
    checkCartDetailsError();

    firstName = null;
    lastName = null;
    String randomString = RandomStringUtils.random(6, true, false);

    region = region.replace("/", "").replace("-", "");
    address = getBillingAddress(region);
    String[] paymentCardDetails = getPaymentDetailsDR(paymentMethod.toUpperCase()).split("@");

    firstName = "FN" + randomString;
    Util.printInfo(BICECEConstants.FIRST_NAME1 + firstName);
    lastName = "LN" + randomString;
    Util.printInfo(BICECEConstants.LAST_NAME1 + lastName);
    createBICAccount(firstName, lastName, emailID, password);

    data.put(BICECEConstants.FIRSTNAME, firstName);
    data.put(BICECEConstants.LASTNAME, lastName);

    // Get Payment details
    debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);
    selectPaymentProfileDR(data, paymentCardDetails);

    // Enter billing details
    debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
    populateBillingAddressDR(address, data);
    debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);

    agreeToTerm();
    clickOnMakeThisATestOrder();

    orderNumber = submitGetOrderNumber();

    // Check to see if EXPORT COMPLIANCE or Null
    validateBicOrderNumber(orderNumber);
    printConsole(constructGuacDRURL, orderNumber, emailID, address, firstName, lastName,
        paymentMethod);

    clickOnViewInvoiceLink();

    return orderNumber;
  }

  public void selectPaymentProfileDR(HashMap<String, String> data, String[] paymentCardDetails) {
    try {
      Util.printInfo("Selecting DR payment profile : " + data.get(BICECEConstants.PAYMENT_TYPE));
      switch (data.get(BICECEConstants.PAYMENT_TYPE).toUpperCase()) {
        default:
          populatePaymentDetailsDR(paymentCardDetails);
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to select DR payment profile...");
    }
  }

  public String getPaymentDetailsDR(String paymentMethod) {

    String paymentDetails = null;

    switch (paymentMethod.toUpperCase()) {
      case "MASTERCARD":
        paymentDetails = "5168441223630339@3月@2025@456";
        break;
      case "AMEX":
        paymentDetails = "371642190784801@3月@2025@7456";
        break;
      case "JCB":
        paymentDetails = "3538684728624673@3月@2025@456";
        break;
      default:
        paymentDetails = "4111111111111111@3月@2025@456";
    }
    return paymentDetails;
  }

  @Step("Populate DR payment details")
  public void populatePaymentDetailsDR(String[] paymentCardDetails) {
    Util.printInfo("Enter card details to make payment");

    bicPage.waitForField(BICECEConstants.CREDIT_CARD_FRAME_DR, true, 30000);
    bicPage.executeJavascript("window.scrollBy(0,600);");

    try {
      WebElement creditCardFrameDR = bicPage.getMultipleWebElementsfromField(
          BICECEConstants.CREDIT_CARD_FRAME_DR)
          .get(0);
      driver.switchTo().frame(creditCardFrameDR);
      Util.sleep(2000);

      String expirationMonthDRXpath = "";
      String expirationYearDRXpath = "";

      expirationMonthDRXpath = bicPage.getFirstFieldLocator("expirationMonthDR");
      expirationYearDRXpath = bicPage.getFirstFieldLocator("expirationYearDR");

      Util.printInfo("Entering DR card number : " + paymentCardDetails[0]);
      bicPage.waitForField("cardNumberDR", true, 10000);
      bicPage.click("cardNumberDR");
      bicPage.executeJavascript(
          "document.getElementById('ccNum').value='" + paymentCardDetails[0] + "'");
      Util.sleep(1000);

      WebElement monthEle = driver.findElement(By.xpath(expirationMonthDRXpath));
      Select selMonth = new Select(monthEle);
      selMonth.selectByVisibleText(paymentCardDetails[1]);
      Util.sleep(1000);

      WebElement yearEle = driver.findElement(By.xpath(expirationYearDRXpath));
      Select selYear = new Select(yearEle);
      selYear.selectByVisibleText(paymentCardDetails[2]);
      Util.sleep(1000);

      Util.printInfo("Entering security code : " + paymentCardDetails[3]);
      bicPage.click("cardSecurityCodeDR");
      bicPage.executeJavascript(
          "document.getElementById('cardSecurityCode').value='" + paymentCardDetails[3] + "'");
      Util.sleep(1000);

      driver.switchTo().defaultContent();
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Card details to make payment");
    }
  }

  @Step("Populate DR billing address")
  public boolean populateBillingAddressDR(Map<String, String> address,
      HashMap<String, String> data) {
    boolean status = false;

    try {
      WebElement creditCardFrameDR = bicPage.getMultipleWebElementsfromField(
          BICECEConstants.CREDIT_CARD_FRAME_DR)
          .get(0);
      driver.switchTo().frame(creditCardFrameDR);

      String paymentType = System.getProperty(BICECEConstants.PAYMENT);
      String firstNameXpath = "";
      String lastNameXpath = "";

      firstNameXpath = bicPage.getFirstFieldLocator("firstNameDR");
      lastNameXpath = bicPage.getFirstFieldLocator("lastNameDR");

      driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get(BICECEConstants.FIRSTNAME));
      driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get(BICECEConstants.LASTNAME));
      driver.switchTo().defaultContent();

      status = populateBillingDetailsDR(address, paymentType);

      clickOnSaveProfileBtnDR();

    } catch (Exception e) {
      e.printStackTrace();
      debugPageUrl(e.getMessage());
      AssertUtils.fail("Unable to populate the Billing Address details");
    }
    return status;
  }

  @SuppressWarnings("static-access")
  public boolean populateBillingDetailsDR(Map<String, String> address, String paymentType) {
    boolean status = false;

    try {
      WebElement creditCardFrameDR = bicPage.getMultipleWebElementsfromField(
          BICECEConstants.CREDIT_CARD_FRAME_DR)
          .get(0);
      driver.switchTo().frame(creditCardFrameDR);

      Util.printInfo("Adding DR billing details...");

      String orgNameXpath = "", fullAddrXpath = "", zipXpath = "", phoneXpath = "", agreementXpath = "";

      switch (paymentType.toUpperCase()) {
        default:
          orgNameXpath = bicPage.getFirstFieldLocator(BICECEConstants.COMPANY_NAME_DR);
          fullAddrXpath = bicPage.getFirstFieldLocator(BICECEConstants.ADDRESS_DR);
          zipXpath = bicPage.getFirstFieldLocator(BICECEConstants.POSTAL_CODE_DR);
          phoneXpath = bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER_DR);
          agreementXpath = bicPage.getFirstFieldLocator("saveMyAccountCheckboxDR");
          break;
      }

      WebDriverWait wait = new WebDriverWait(driver, 60);
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(orgNameXpath)));
      status = driver.findElement(By.xpath(orgNameXpath)).isDisplayed();

      if (status == false) {
        AssertUtils.fail("Organization_Name not available.");
      }

      driver.findElement(By.xpath(orgNameXpath)).click();
      Util.sleep(1000);
      driver.findElement(By.xpath(orgNameXpath))
          .sendKeys(new RandomStringUtils().random(5, true, true) + address.get(
              BICECEConstants.COMPANY_NAME_DR));

      driver.findElement(By.xpath(fullAddrXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
      Util.sleep(1000);
      driver.findElement(By.xpath(fullAddrXpath)).sendKeys(address.get(BICECEConstants.ADDRESS_DR));

      driver.findElement(By.xpath(zipXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
      Util.sleep(1000);
      driver.findElement(By.xpath(zipXpath)).sendKeys(address.get(BICECEConstants.POSTAL_CODE_DR));

      driver.findElement(By.xpath(phoneXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
      Util.sleep(1000);
      driver.findElement(By.xpath(phoneXpath))
          .sendKeys(address.get(BICECEConstants.PHONE_NUMBER_DR));

      Util.sleep(1000);
      driver.findElement(By.xpath(agreementXpath)).click();

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Util.printTestFailedMessage("populateBillingDetailsDR");
      AssertUtils.fail("Unable to Populate DR Billing Details");
    }
    return status;
  }

  private void clickOnSaveProfileBtnDR() {
    try {
      Util.printInfo("Clicking on DR save payment profile button...");
      List<WebElement> eles = bicPage.getMultipleWebElementsfromField("saveMyAccountButtonDR");

      eles.get(0).click();

      bicPage.waitForPageToLoad();
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to click on DR Save button on billing details page...");
    }
  }

  private void clickOnMakeThisATestOrder() {
    try {
      Util.printInfo("Clicking on DR test order link...");
      driver.findElement(By.linkText("Make this a test order")).click();
      Util.sleep(2000);
    } catch (Exception e) {
      AssertUtils.fail("Failed to click on 'Make this a test order' link");
    }

    try {
      String alertMessage = "";
      driver.switchTo().alert();
      alertMessage = driver.switchTo().alert().getText();
      Util.printInfo("Alert text found: " + alertMessage);
      if (alertMessage.contains("Success")) {
        Util.printInfo("Closing DR purchase alert!");
        driver.switchTo().alert().accept();
      } else {
        Util.printInfo("Unable to make a test order");
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to make a test order");
    }
  }

  private void clickOnViewInvoiceLink() {
    try {
      String orderNumber = "";
      String orderNumberInStore = "";

      orderNumber = driver.findElement(By.xpath(BICECEConstants.JP_ORDER_NUMBER)).getText();
      Util.printInfo("Find DR order number: " + orderNumber);

      driver.findElement(By.linkText("請求書を表示する")).click();
      Util.sleep(10000);

      ArrayList<String> tab = new ArrayList<>(driver.getWindowHandles());
      driver.switchTo().window(tab.get(1));

      orderNumberInStore = driver.findElement(By.xpath("//*[@id='dr_orderNumber']//span"))
          .getText();
      Util.printInfo("Find DR order number from store: " + orderNumberInStore);
      AssertUtils.assertEquals(orderNumber, orderNumberInStore);
    } catch (Exception e) {
      AssertUtils.fail("Failed to validate order number from Store");
    }
  }
}