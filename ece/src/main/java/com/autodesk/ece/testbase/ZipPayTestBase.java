package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.HashMap;
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
  private static final String ZIP_PAY_DASHBOARD_VERIFY = "zipPayDashboardVerify";
  private static final String ZIP_PAY_PAYMENT_SUCCESS = "zipPayPaymentSuccess";

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

  @Step("Go through Zip checkout")
  public void zipPayCheckout() {
    // Login to the zip account
    zipPage.waitForField(ZIP_PAY_USERNAME_KEY, true, 5000);
    zipPage.populateField(ZIP_PAY_USERNAME_KEY, testData.get(ZIP_PAY_USERNAME_KEY));
    zipPage.populateField(ZIP_PAY_PASSWORD_KEY, testData.get(ZIP_PAY_PASSWORD_KEY));

    zipPage.click("zipPayLogin");

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
      zipPage.populateField(ZIP_PAY_VERIFICATION_CODE_KEY,
          testData.get(ZIP_PAY_VERIFICATION_CODE_KEY));
      zipPage.click("zipPayVerificationSubmit");
    }

    // Click on pay with Zip pay
    zipPage.waitForField(ZIP_PAY_OPTION, true, 10000);
    try {
      zipPage.clickUsingLowLevelActions(ZIP_PAY_OPTION);
    } catch (MetadataException e) {
      AssertUtils.fail("Failed to click on payment option");
    }

    zipPage.click("zipPayConfirmPayment");
  }

  /**
   * Verify if the user can pay for the request amount, and refill the user's account if necessary
   *
   * @param balanceRequiredText - Amount to refill as a currency string
   */
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
    zipPage.waitForField(ZIP_PAY_DASHBOARD_LOGIN, true, 5000);
    zipPage.click(ZIP_PAY_DASHBOARD_LOGIN);

    // Login to the zip account
    zipPage.waitForField(ZIP_PAY_DASHBOARD_USERNAME, true, 5000);
    zipPage.populateField(ZIP_PAY_DASHBOARD_USERNAME, testData.get(ZIP_PAY_USERNAME_KEY));
    zipPage.populateField("zipPayDashboardPassword", testData.get(ZIP_PAY_PASSWORD_KEY));
    zipPage.click(ZIP_PAY_SUBMIT);

    String amountXPath = zipPage.getFirstFieldLocator("zipPayDashboardAmountAvailable");
    VerifySMSOrWaitForField(amountXPath);

    WebElement amountAvailableElement;
    try {
      amountAvailableElement = driver.findElement(By.xpath(amountXPath));
    } catch (Exception ex) {
      Util.printLog("Failed to get amount balance");
      Util.printLog(driver.getPageSource());
      AssertUtils.fail("Failed to get amount balance");
      return;
    }

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

  private void VerifySMSOrWaitForField(String fieldXPath) {
    String smsVerificationXPath = zipPage.getFirstFieldLocator("zipPayVerificationTitle");
    WebDriverWait wait = new WebDriverWait(driver, 30);
    wait.until(ExpectedConditions.or(
        ExpectedConditions.presenceOfElementLocated(By.xpath(fieldXPath)),
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
      wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(fieldXPath)));
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
}