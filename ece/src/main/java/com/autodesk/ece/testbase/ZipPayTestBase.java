package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.HashMap;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ZipPayTestBase {
  private static final String ZIP_PAY_USERNAME_KEY = "zipPayUsername";
  private static final String ZIP_PAY_PASSWORD_KEY = "zipPayPassword";
  private static final String ZIP_PAY_VERIFICATION_CODE_KEY = "zipPayVerificationCode";
  private static final String ZIP_PAY_OPTION = "zipPayOption";
  private static final String ZIP_PAY_DASHBOARD_LOGIN = "zipPayDashboardLogin";
  private static final String ZIP_PAY_OTHER_AMOUNT = "zipPayOtherAmount";
  private static final String ZIP_PAY_SUBMIT = "zipPayPaymentSubmit";
  private static final String ZIP_PAY_CONFIRM = "zipPayPaymentConfirm";
  private static final String ZIP_PAY_CLOSE = "zipPayFailClose";

  public static Page_ zipPage = null;
  public WebDriver driver = null;
  HashMap<String, String> testData = new HashMap<>();

  public ZipPayTestBase(GlobalTestBase testBase) {
    zipPage = testBase.createPage("PAGE_ZIPPAY");
    this.driver = testBase.getdriver();
  }

  public void setTestData(HashMap<String, String> testData) {
    this.testData = testData;
  }

  @Step("Go through Zip checkout")
  public void zipPayCheckout() {
    // Login to the zip account
    zipPage.waitForField(ZIP_PAY_USERNAME_KEY, true, 5000);
    zipPage.populateField(ZIP_PAY_USERNAME_KEY, testData.get(ZIP_PAY_USERNAME_KEY));
    zipPage.populateField(ZIP_PAY_PASSWORD_KEY, testData.get(ZIP_PAY_PASSWORD_KEY));

    zipPage.click("zipPayLogin");

    // If an SMS verification code is request, use the provided test code
    boolean verificationCodeRequested = zipPage.waitForField(ZIP_PAY_VERIFICATION_CODE_KEY, true, 10000);
    if (verificationCodeRequested) {
      zipPage.populateField(ZIP_PAY_VERIFICATION_CODE_KEY, testData.get(ZIP_PAY_VERIFICATION_CODE_KEY));
      zipPage.click("zipPayVerificationSubmit");
    }

    // Click on pay with Zip pay
    zipPage.waitForField(ZIP_PAY_OPTION, true, 10000);
    try {
      zipPage.clickUsingLowLevelActions(ZIP_PAY_OPTION);
    } catch (MetadataException e) {
      e.printStackTrace();
    }

    // Determine if we have sufficient balance to make the purchase
    String amountDueXPath = zipPage.getFirstFieldLocator("zipPayAmountDue");
    String availableBalanceXPath = zipPage.getFirstFieldLocator("zipPayBalanceAvailable");
    WebElement amountDueElement = driver.findElement(By.xpath(amountDueXPath));
    WebElement availableBalanceElement = driver.findElement(By.xpath(availableBalanceXPath));
    double amountDue = Double.parseDouble(amountDueElement.getText().replace("$", ""));
    double availableBalance = Double.parseDouble(
        availableBalanceElement.getText().replace("$", ""));

    if (amountDue > 1000) {
      AssertUtils.fail("Zip Pay transaction failed, transaction amount greater than $1000");
    }

    // If the balance is insufficient, refill the account
    if (amountDue > availableBalance) {
      // Calculate an amount that is slightly higher than the purchase amount, but less than the max
      // refill amount of $500, and less than the current balance
      double amountToRefill = Math.min(
          Math.min(Math.round(amountDue - availableBalance) + 200, 500),
          1000 - Math.round(availableBalance));
      refillZipPayBalance(amountToRefill);
      return;
    }

    zipPage.click("zipPayConfirmPayment");
  }

  /**
   * Navigate to the user's zip dashboard and refill their account for a specified amount
   * @param amount - Amount to refill in dollars
   */
  private void refillZipPayBalance(double amount) {
    // Open the user's dashboard in a new tab
    ((JavascriptExecutor) driver).executeScript("window.open()");
    ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
    driver.switchTo().window(tabs.get(1));
    driver.get("https://account.sandbox.zipmoney.com.au/#");
    zipPage.waitForField(ZIP_PAY_DASHBOARD_LOGIN, true, 5000);
    zipPage.click(ZIP_PAY_DASHBOARD_LOGIN);

    // Navigate to the make payment page
    driver.get("https://account.sandbox.zipmoney.com.au/#/wallet/makePayment");
    zipPage.waitForField(ZIP_PAY_OTHER_AMOUNT, true, 60000);
    try {
      zipPage.clickUsingLowLevelActions(ZIP_PAY_OTHER_AMOUNT);
    } catch (MetadataException e) {
      e.printStackTrace();
    }

    // Enter the amount to pay and pay it
    zipPage.populateField("zipPayPaymentAmount", Double.toString(amount));
    Util.sleep(1000);
    zipPage.waitForField(ZIP_PAY_SUBMIT, true, 5000);
    zipPage.click(ZIP_PAY_SUBMIT);

    zipPage.waitForField(ZIP_PAY_CONFIRM, true, 5000);
    zipPage.click(ZIP_PAY_CONFIRM);

    // Close the zip pay dashboard
    zipPage.waitForField("zipPayPaymentSuccess", true, 10000);
    driver.close();
    driver.switchTo().window(tabs.get(0));

    // Navigate back to GUAC
    zipPage.click("zipPayReturn");
    zipPage.waitForField(ZIP_PAY_CLOSE, true, 5000);
    zipPage.click(ZIP_PAY_CLOSE);

    // Submit the order again
    zipPage.waitForFieldPresent(BICECEConstants.SUBMIT_ORDER_BUTTON, 10000);
    try {
      zipPage.clickUsingLowLevelActions(BICECEConstants.SUBMIT_ORDER_BUTTON);
    } catch (MetadataException e) {
      e.printStackTrace();
    }

    // Checkout with zip again
    zipPayCheckout();
  }
}
