package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ZipPayTestBase {

  private static final String ZIP_PAY_USERNAME_KEY = "zipPayUsername";
  private static final String ZIP_PAY_PASSWORD_KEY = "zipPayPassword";
  private static final String ZIP_PAY_VERIFICATION_CODE_KEY = "zipPayVerificationCode";
  private static final String ZIP_PAY_OPTION = "zipPayOption";
  private static final String ZIP_PAY_DASHBOARD_LOGIN = "zipPayDashboardLogin";
  private static final String ZIP_PAY_OTHER_AMOUNT = "zipPayOtherAmount";
  private static final String ZIP_PAY_SUBMIT = "zipPayPaymentSubmit";
  private static final String ZIP_PAY_CONFIRM = "zipPayPaymentConfirm";
  private static final String ZIP_PAY_DASHBOARD_USERNAME = "zipPayDashboardUsername";
  private static final String ZIP_PAY_DASHBOARD_PASSWORD = "zipPayDashboardPassword";
  private static final String ZIP_PAY_DASHBOARD_VERIFY = "zipPayDashboardVerify";
  private static final String ZIP_PAY_PAYMENT_SUCCESS = "zipPayPaymentSuccess";
  private static final String ZIP_PAY_DASHBOARD_AMOUNT = "zipPayDashboardAmountAvailable";

  private final Page_ zipPage;
  private final WebDriver driver;
  private Map<String, String> testData = new HashMap<>();

  public ZipPayTestBase(GlobalTestBase testBase) {
    zipPage = testBase.createPage("PAGE_ZIPPAY");
    this.driver = testBase.getdriver();
  }

  public void setTestData(Map<String, String> testData) {
    this.testData = testData;
  }

  @Step("Go through Zip checkout" + GlobalConstants.TAG_TESTINGHUB)
  public void zipPayCheckout() {
    attemptZipPayCheckoutLogin();

    String verificationCodeXPath = zipPage.getFirstFieldLocator(ZIP_PAY_VERIFICATION_CODE_KEY);
    String paymentOptionXPath = zipPage.getFirstFieldLocator(ZIP_PAY_OPTION);

    WebDriverWait wait = new WebDriverWait(driver, 10);
    wait.until(ExpectedConditions.or(
        ExpectedConditions.presenceOfElementLocated(By.xpath(paymentOptionXPath)),
        ExpectedConditions.presenceOfElementLocated(By.xpath(verificationCodeXPath))
    ));

    // If an SMS verification code is requested, use the provided test code
    driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
    if (!driver.findElements(By.xpath(verificationCodeXPath)).isEmpty()) {
      attemptZipPayVerification();
    }

    zipPage.waitForField(ZIP_PAY_OPTION, true, 10000);

    // Double check that we have sufficient balance to pay for the product
    double availableAmount = getElementAmount("zipPayOptionAmountAvailable");
    double amountDue = getElementAmount("zipPayAmountDue");

    if (amountDue > availableAmount) {
      AssertUtils.fail(
          "Insufficient balance to checkout with zip. Another test or automation may have used up the allocated balance.");
    }

    // Click on pay with Zip pay
    try {
      zipPage.clickUsingLowLevelActions(ZIP_PAY_OPTION);
    } catch (MetadataException e) {
      AssertUtils.fail("Failed to click on payment option");
    }

    zipPage.click("zipPayConfirmPayment");
  }

  private void attemptZipPayCheckoutLogin() {
    int loginAttempts = 0;
    String loginXPath = zipPage.getFirstFieldLocator("zipPayLogin");

    // Login to the zip account
    boolean usernameFound = zipPage.waitForField(ZIP_PAY_USERNAME_KEY, true, 30000);
    if (!usernameFound) {
      AssertUtils.fail("Login form failed to load after 30 seconds");
    }

    zipPage.populateField(ZIP_PAY_USERNAME_KEY, testData.get(ZIP_PAY_USERNAME_KEY));

    String password = ProtectedConfigFile.decrypt(testData.get(ZIP_PAY_PASSWORD_KEY));
    zipPage.populateField(ZIP_PAY_PASSWORD_KEY, password);

    while (loginAttempts < 5) {
      zipPage.click("zipPayLogin");

      try {
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(loginXPath)));
      } catch (Exception e) {
        Util.printWarning("Zip login button still present after login attempt");
      }

      List<WebElement> loginButton = driver.findElements(By.xpath(loginXPath));
      if (loginButton.isEmpty()) {
        return;
      } else {
        Util.printWarning("Login failed on attempt #" + (loginAttempts + 1));
        Util.sleep(3000);
        loginAttempts++;
      }
    }

    AssertUtils.fail("Failed to log into Zip after 5 attempts");
  }

  private void attemptZipPayVerification() {
    int verificationAttempts = 0;
    String verifyXPath = zipPage.getFirstFieldLocator("zipPayVerificationSubmit");

    zipPage.populateField(ZIP_PAY_VERIFICATION_CODE_KEY,
        testData.get(ZIP_PAY_VERIFICATION_CODE_KEY));

    while (verificationAttempts < 5) {
      zipPage.click("zipPayVerificationSubmit");

      try {
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(verifyXPath)));
      } catch (Exception e) {
        Util.printWarning("Zip verify button still present after verify attempt");
      }

      List<WebElement> verifyButton = driver.findElements(By.xpath(verifyXPath));
      if (verifyButton.isEmpty()) {
        return;
      } else {
        Util.printWarning("Login failed on attempt #" + (verificationAttempts + 1));
        Util.sleep(3000);
        verificationAttempts++;
      }
    }

    AssertUtils.fail("Failed to verify SMS after 5 attempts");
  }

  /**
   * Verify if the user can pay for the request amount, and refill the user's account if necessary
   *
   * @param balanceRequiredText - Amount to refill as a currency string
   */
  @Step("Verify and refill zip balance" + GlobalConstants.TAG_TESTINGHUB)
  public void verifyZipBalance(String balanceRequiredText) {
    double balanceRequired = parseZipAmount(balanceRequiredText);
    if (balanceRequired > 1000) {
      AssertUtils.fail("Zip Pay transaction failed, transaction amount greater than $1000");
    }

    // Open the user's dashboard in a new tab
    ((JavascriptExecutor) driver).executeScript("window.open()");
    ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
    driver.switchTo().window(tabs.get(1));
    driver.get("https://account.sandbox.zipmoney.com.au/#");

    try{
     if(zipPage.checkIfElementExistsInPage(ZIP_PAY_DASHBOARD_LOGIN, 10)){
      zipPage.click(ZIP_PAY_DASHBOARD_LOGIN);

       // Login to the zip account
       zipPage.waitForField(ZIP_PAY_DASHBOARD_USERNAME, true, 5000);
       zipPage.populateField(ZIP_PAY_DASHBOARD_USERNAME, testData.get(ZIP_PAY_USERNAME_KEY));
       zipPage.populateField(ZIP_PAY_DASHBOARD_PASSWORD,
           ProtectedConfigFile.decrypt(testData.get(ZIP_PAY_PASSWORD_KEY)));
       zipPage.click(ZIP_PAY_SUBMIT);
     }
    } catch (MetadataException e) {
      AssertUtils.fail("Error while checking zip login page");
    }

    try {
      VerifySMSOrWaitForField();
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to get amount balance");
    }

    String amountXPath = zipPage.getFirstFieldLocator(ZIP_PAY_DASHBOARD_AMOUNT);
    WebElement amountAvailableElement = driver.findElement(By.xpath(amountXPath));

    double availableBalance = parseZipAmount(amountAvailableElement.getText());

    if (availableBalance > balanceRequired) {
      // Close the zip pay dashboard
      driver.close();
      driver.switchTo().window(tabs.get(0));
      return;
    }

    double amountToRefill = Math.min(
        Math.max(Math.round(balanceRequired - availableBalance) + 200, 500),
        1000 - Math.round(availableBalance));

    Util.printInfo(String.format(
        "Will attempt to pay $%.2f on a balance of $%.2f to buy a product worth $%.2f",
        amountToRefill, 1000 - availableBalance, balanceRequired));

    // Navigate to the make payment page
    driver.get("https://account.sandbox.zipmoney.com.au/#/wallet/makePayment");
    zipPage.waitForField(ZIP_PAY_OTHER_AMOUNT, true, 60000);
    try {
      zipPage.clickUsingLowLevelActions(ZIP_PAY_OTHER_AMOUNT);
    } catch (MetadataException e) {
      AssertUtils.fail("Failed to click on other amount button");
    }

    // Enter the amount to pay and pay it
    zipPage.populateField("zipPayPaymentAmount", Double.toString(amountToRefill));
    Util.sleep(1000);
    zipPage.waitForField(ZIP_PAY_SUBMIT, true, 5000);
    zipPage.click(ZIP_PAY_SUBMIT);

    zipPage.waitForField(ZIP_PAY_CONFIRM, true, 5000);
    zipPage.click(ZIP_PAY_CONFIRM);

    // Close the zip pay dashboard
    zipPage.waitForField(ZIP_PAY_PAYMENT_SUCCESS, true, 10000);
    driver.close();
    driver.switchTo().window(tabs.get(0));
  }

  private void VerifySMSOrWaitForField() {
    String smsVerificationXPath = zipPage.getFirstFieldLocator("zipPayVerificationTitle");
    String amountXPath = zipPage.getFirstFieldLocator(ZIP_PAY_DASHBOARD_AMOUNT);

    WebDriverWait wait = new WebDriverWait(driver, 60);
    wait.until(ExpectedConditions.or(
        ExpectedConditions.presenceOfElementLocated(By.xpath(amountXPath)),
        ExpectedConditions.presenceOfElementLocated(By.xpath(smsVerificationXPath))
    ));

    driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
    // If an SMS verification code is requested, use the provided test code
    if (!driver.findElements(By.xpath(smsVerificationXPath)).isEmpty()) {
      zipPage.click(ZIP_PAY_SUBMIT);
      zipPage.waitForField(ZIP_PAY_DASHBOARD_VERIFY, true, 3000);
      zipPage.populateField(ZIP_PAY_DASHBOARD_VERIFY,
          testData.get(ZIP_PAY_VERIFICATION_CODE_KEY));
      zipPage.click(ZIP_PAY_SUBMIT);
      wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(amountXPath)));
    }
  }

  /**
   * Parse a price string displayed in Zip
   *
   * @param amount - Currency string to parse
   * @return - Amount in dollars
   */
  private double parseZipAmount(String amount) {
    return Double.parseDouble(amount
        .replace("$", "")
        .replace(",", "")
        .replace("A", ""));
  }

  /**
   * Parse the currency value of the text for a locator
   *
   * @param field - The page locator with the dollar amount
   * @return - The locator's value in dollars
   */
  private double getElementAmount(String field) {
    String locator = zipPage.getFirstFieldLocator(field);
    String amount = driver.findElement(By.xpath(locator)).getText();
    return parseZipAmount(amount);
  }
}
