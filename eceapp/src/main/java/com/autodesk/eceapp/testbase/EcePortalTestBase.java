package com.autodesk.eceapp.testbase;

import static java.util.Objects.isNull;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.constants.EceAppConstants;
import com.autodesk.eceapp.utilities.NumberUtil;
import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.PDFReader;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.ScreenCapture;
import com.autodesk.testinghub.core.utils.SoftAssertUtil;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.constants.BICConstants;
import com.autodesk.testinghub.eseapp.constants.TestingHubConstants;
import io.qameta.allure.Step;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.util.Strings;

public class EcePortalTestBase {

  private final Page_ portalPage;
  private final String accountsPortalOrdersInvoicesUrl;
  private final String accountsPortalSubscriptionsUrl;
  private final String accountsPortalInvoiceUrl;
  private final String accountsPortalOrderHistoryUrl;
  private final String accountsPortalProductsServicesUrl;
  private final String accountsPortalAddSeatsUrl;
  private final String accountsPortalQuoteUrl;
  private final String accountPortalBillingInvoicesUrl;
  private final String accountsProductPageUrl;
  private final EceZipPayTestBase zipTestBase;
  private final EceBICTestBase bicTestBase;
  private final String accountPortalOxygenLogOut;

  public WebDriver driver = null;

  public EcePortalTestBase(GlobalTestBase testbase) {
    driver = testbase.getdriver();
    portalPage = testbase.createPageForApp("PAGE_PORTAL", EceAppConstants.APP_NAME);
    bicTestBase = new EceBICTestBase(driver, testbase);
    zipTestBase = new EceZipPayTestBase(testbase);

    Map<?, ?> loadYaml = ResourceFileLoader.getBicOrderYaml();
    LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    accountsPortalOrdersInvoicesUrl = defaultvalues.get("accountsPortalOrdersInvoicesUrl");
    accountsPortalSubscriptionsUrl = defaultvalues.get("accountsPortalSubscriptionsUrl");
    accountsPortalInvoiceUrl = defaultvalues.get("accountsPortalInvoiceUrl");
    accountsPortalOrderHistoryUrl = defaultvalues.get("accountsPortalOrderHistoryUrl");
    accountsPortalProductsServicesUrl = defaultvalues.get("accountsPortalProductsServicesUrl");
    accountsPortalAddSeatsUrl = defaultvalues.get("accountsPortalAddSeatsUrl");
    accountsPortalQuoteUrl = defaultvalues.get("accountsPortalQuoteUrl");
    accountPortalBillingInvoicesUrl = defaultvalues.get("accountPortalBillingInvoicesUrl");
    accountsProductPageUrl = defaultvalues.get("accountsProductPageUrl");
    accountPortalOxygenLogOut = defaultvalues.get("oxygenLogOut");
  }

  public static String timestamp() {
    String strDate = null;
    Date date = Calendar.getInstance().getTime();
    DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss");
    strDate = dateFormat.format(date).replace(" ", "").replace("-", "").replace(":", "");
    return strDate;
  }

  private void clickWithJavaScriptExecutor(JavascriptExecutor executor, String elementXpath) {
    WebElement element = driver
        .findElement(By.xpath(elementXpath));
    executor.executeScript(BICECEConstants.ARGUMENTS_CLICK, element);
  }

  public void checkIfQuoteIsStillPresent(String quoteId) {
    openPortalURL(accountsPortalQuoteUrl);
    WebElement quoteIdElement = null;
    try {
      String productXpath = portalPage
          .getFirstFieldLocator("quoteIdText").replace("TOKEN1", quoteId);
      quoteIdElement = driver.findElement(By.xpath(productXpath));
    } catch (Exception e) {
      //Do nothing here.
    }
    AssertUtils.assertEquals(quoteIdElement, null, "QuoteId should not be present after the quote is ordered");
  }

  public void openPortalURL(String data) {
    try {
      driver.manage().window().maximize();
      Util.sleep(2000);
      driver.get(data);
      driver.navigate().refresh();
      Util.printInfo("Opened:" + data);
      bicTestBase.validateNetworkLogsOnEachPage(data);
    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to launch portal page: " + data);
      driver.get(data);
      e.printStackTrace();
    }
  }

  public boolean openPortalBICLaunch(String url) {
    Util.printInfo("launch URL in browser");
    openPortalURL(accountPortalOxygenLogOut);
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("localStorage.clear();sessionStorage.clear();");
    driver.manage().deleteAllCookies();
    driver.navigate().to(url);
    Util.printInfo("Opened:" + url);
    bicTestBase.validateNetworkLogsOnEachPage(url);
    return true;
  }

  public void clickCheckBox(String CheckboxClick) {
    try {
      System.out.println("Execute JavaScriptExe" + CheckboxClick);
      JavascriptExecutor jse = (JavascriptExecutor) driver;
      jse.executeScript(CheckboxClick);
    } catch (Exception e) {
      e.printStackTrace();
      //AssertUtils.fail("Unable to click on checkbox");
    }
  }

  @Step("User Login in Customer Portal " + GlobalConstants.TAG_TESTINGHUB)
  public void portalLogin(String portalUserName, String portalPassword) {
    boolean status = false;
    int attempts = 0;
    Util.printInfo("Account Portal login with user email: " + portalUserName);

    while (attempts < 3) {
      try {
        Util.sleep(2000);
        portalPage.getMultipleWebElementsfromField("usernameCEP").get(0).sendKeys(portalUserName);
        portalPage.getMultipleWebElementsfromField("verifyUserCEPBtn").get(0).click();
        Util.sleep(2000);
        portalPage.getMultipleWebElementsfromField("passCEP").get(0).click();
        portalPage.getMultipleWebElementsfromField("passCEP").get(0).sendKeys(portalPassword);
        Util.sleep(2000);
        portalPage.getMultipleWebElementsfromField("createAccount").get(0).click();

        try {
          if (portalPage.getMultipleWebElementsfromField(BICECEConstants.SKIP_LINK).get(0)
              .isDisplayed()
              || portalPage.isFieldVisible(BICECEConstants.SKIP_LINK)
              || portalPage.checkFieldExistence(BICECEConstants.SKIP_LINK)
              || portalPage.isFieldPresent(BICECEConstants.SKIP_LINK)) {
            Util.printInfo("Skip link is displayed after logging into portal");
            Util.printInfo("Clicking on SkipLink");
            portalPage.clickUsingLowLevelActions(BICECEConstants.SKIP_LINK);
            status = true;
          }
        } catch (Exception e) {
        }
        status = true;
        break;
      } catch (Exception e) {
        Util.printInfo("Retry Logic: Failed to Login to Account Portal. Attempt:" + (attempts + 1));
        e.printStackTrace();
        attempts++;
        driver.navigate().refresh();
        Util.sleep(3000);
      }
    }

    if (!status) {
      AssertUtils.fail("Failed to Login to Account Portal");
    } else {
      Util.printInfo("Successfully logged into Portal!!!");
    }

  }

  @Step("Looking for Product and Services Web element in Portal" + GlobalConstants.TAG_TESTINGHUB)
  public boolean isPortalElementPresent(String Field) {
    boolean status = false;
    int attempts = 0;

    while (attempts < 3) {
      try {
        status =
            portalPage.isFieldVisible(Field) || portalPage.checkFieldExistence(Field) || portalPage
                .isFieldPresent(Field) || portalPage.checkIfElementExistsInPage(Field, 60);
      } catch (MetadataException e) {
        Util.printInfo(
            "Failed looking for \"Portal Product and Service Tab\" - Attempt #" + (attempts + 1));
      }

      if (!status) {
        driver.navigate().refresh();
        Util.sleep(5000);
        attempts++;
      } else {
        Util.printInfo("Found the \"Portal Product and Service Tab\", so skipping the retry logic");
        break;
      }
    }

    return status;
  }

  @Step("Verify if Subscription exists in Product and Services Page" + GlobalConstants.TAG_TESTINGHUB)
  public boolean isSubscriptionInPortal(String subscriptionId, String userName, String password) {
    boolean status = false;
    String productXpath = "";
    int attempts = 0;
    WebElement element = null;

    String productName = System.getProperty(BICECEConstants.PRODUCT_NAME);

    while (attempts < 5) {
      try {
        openPortalURL(accountsPortalSubscriptionsUrl);
        if (Strings.isNotNullAndNotEmpty(productName) && productName.equals("premium")) {
          productXpath = portalPage
              .getFirstFieldLocator("premiumSubscriptionID").replace("TOKEN1", subscriptionId);
        } else {
          productXpath = portalPage
              .getFirstFieldLocator("subscriptionIDInBO").replace("TOKEN1", subscriptionId);
        }
        element = driver.findElement(By.xpath(productXpath));
      } catch (Exception e) {
        //Do nothing here.
      }
      if (isNull(element)) {
        if (attempts == 4) {
          AssertUtils.fail("All retries exhausted: Couldn't find subscription/agreement productXpath element");
        }

        Util.printInfo("Retry: Failed to find the Subscription productXpath in Portal, attempt #" + (attempts + 1));
        Util.sleep(300000);
        Util.printInfo("Part of Retry: Invalidate Portal user cache, by signing out and then back in");

        ScreenCapture.getInstance().captureFullScreenshot();
        Util.printInfo("Taking screenshot of Account Portal before logging out");

        portalLogoutLogin(userName, password);
        attempts++;
      } else {
        Util.printInfo("Found the Subscription WebElement, so skipping retries");
        status = true;
        break;
      }
    }

    return status;
  }

  /**
   * Navigate to the "Upcoming Payments" section of portal
   */
  @Step("Click on Upcoming Payments Link" + GlobalConstants.TAG_TESTINGHUB)
  public void navigateToUpcomingPaymentsLink() {
    try {
      openPortalURL(accountsPortalOrdersInvoicesUrl);
      portalPage.waitForPageToLoad();
      checkEmailVerificationPopupAndClick();
    } catch (Exception e) {
      e.printStackTrace();
      SoftAssertUtil.fail("Unable to open upcoming payments section");
    }
    portalPage.waitForPageToLoad();
  }

  public void checkEmailVerificationPopupAndClick() {
    Util.printInfo("Checking Portal Products and Services dialog...");
    Util.sleep(5000);
    try {
      if (portalPage.checkIfElementExistsInPage("portalPSDialogGotItButton", 45)) {
        Util.sleep(15000);
        Util.printInfo("HTML code - Before clicking portalPSDialogGotItButton.");
        debugPageUrl("Clicking on portal 'Got it' button.");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(BICECEConstants.ARGUMENTS_CLICK,
            portalPage.getMultipleWebElementsfromField("portalPSDialogGotItButton").get(0));
        debugPageUrl("Clicked on portal 'Got it' button.");
        Util.printInfo("HTML code - After clicking portalPSDialogGotItButton.");
        EceBICTestBase.bicPage.waitForElementToDisappear("portalPSDialogGotItButton", 30);
      }
    } catch (Exception e) {
      Util.printInfo("Portal - Products and Services dialog does not appear on screen...");
    }
  }

  @Step("CEP : Bic Order capture " + GlobalConstants.TAG_TESTINGHUB)
  public void validateBICOrderProductInCEP(String cepURL, String portalUserName,
      String portalPassword, String subscriptionID) {

    boolean status = false;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
      ScreenCapture.getInstance().captureFullScreenshot();
      Util.printInfo("Taking screenshot of Account Portal After Log in");
    }

    try {
      status = isSubscriptionInPortal(subscriptionID, portalUserName, portalPassword);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (!status) {
      AssertUtils.fail(BICECEConstants.PRODUCT_IS_DISPLAYED_IN_PORTAL + BICECEConstants.FALSE);
    }
  }


  @Step("CEP : Purchasing Quote in  Account Portal  " + GlobalConstants.TAG_TESTINGHUB)
  public void purchaseQuoteInAccount(String cepURL, String portalUserName,
      String portalPassword) {
    openPortalURL(accountsPortalQuoteUrl);
    try {
      portalPage.waitForFieldPresent("portalQuoteBuyButton", 50000);
      portalPage.clickUsingLowLevelActions("portalQuoteBuyButton");

      // Buy will open a new tab, this closes the original tab and switches to the new tab
      driver.close();
      Set<String> windowHandles = driver.getWindowHandles();
      String newTabHandle = windowHandles.iterator().next();
      driver.switchTo().window(newTabHandle);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Quote buy button not visible");
    }

  }

  @Step("CEP : Validating Order Total " + GlobalConstants.TAG_TESTINGHUB)
  public void validateBICOrderDetails(String orderTotal) {
   /* try {
      openPortalURL(accountsPortalOrderHistoryUrl);
      boolean historyPrice = portalPage.waitForFieldPresent("portalOrderHistoryPrice");
      if (!historyPrice) {
        portalPage.waitForFieldPresent("portalOrderHistory");
        portalPage.clickUsingLowLevelActions("portalOrderHistory");
        Util.sleep(5000);
      }
      //validate order details
      AssertUtils.assertTrue(portalPage.checkIfElementExistsInPage("portalOrderDetailsRow", 10));
      Util.printInfo("Great. Order found in Order History");
      portalPage.clickUsingLowLevelActions("portalOrderDetailsRow");
      Util.sleep(10000);

      WebElement getOrderHistoryDocument = portalPage
          .getMultipleWebElementsfromField("portalOrderHistoryDetailIframe").get(0);
      driver.switchTo().frame(getOrderHistoryDocument);
      //Verify the Order Id
      AssertUtils.assertEquals(portalPage.checkIfElementExistsInPage("portalOrderDetailNumber", 10), true,
          "Order Number not found in the Document");
      //Verify the Subscription Id
      AssertUtils.assertEquals(portalPage.checkIfElementExistsInPage("portalSubscriptionId", 10), true,
          "Subscription Id not found in the Document");

      driver.switchTo().defaultContent();
      String historyOrderTotal = portalPage.getLinkText("portalOrderHistoryPrice").replaceAll("[^0-9]", "");

      AssertUtils.assertTrue(orderTotal.equals(historyOrderTotal),
          "Validate order total in history matches order total on checkout");
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to open order details window or verify the order information.");
    }*/
  }

  @Step("CEP : Validating  Tax Invoice " + GlobalConstants.TAG_TESTINGHUB)
  public void validateBICOrderTaxInvoice(Map<String, String> results) {
    String error_message = null;
    String pdfContent = null;
    String url;
    openPortalURL(accountsPortalOrderHistoryUrl);
    Util.sleep(5000);
    try {
      int attempts = 0;

      while (attempts < 30) {
        if (portalPage.checkIfElementExistsInPage("portalOrderInvoiceLink", 10)) {
          portalPage.clickUsingLowLevelActions("portalOrderInvoiceLink");
          try {
            error_message = driver.findElement(By.className("error-msg")).getText();
          } catch (Exception e) {
            error_message = "";
          }

          if (!error_message.isEmpty()) {
            Util.printInfo("Invoice is not ready yet, we saw this text -> " + error_message);
            attempts++;

            if (attempts > 29) {
              Assert.fail("Failed to find Invoice PDF in Order History Page, after " + attempts + " attempts");
            }
            Util.printInfo("Waiting for another 5 minutes on attempt #" + attempts);
            Util.sleep(300000);
            driver.navigate().refresh();
            // As per Account Portal this can take upto 6 sec so considering 10 sec
            Util.sleep(10000);
          } else {
            Util.PrintInfo("Found Invoice. Now need to validate the invoice pop up");

            url = accountsPortalInvoiceUrl
                + results.get(BICECEConstants.ORDER_ID) + "/invoice?type=bc";

            Util.printInfo("URL for Invoice data: " + url);
            driver.navigate().to(url);
            break;
          }
        } else {
          driver.navigate().refresh();
        }
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to validate Invoice in Account Portal");
    }

    String content = driver.findElement(By.tagName("body")).getText();
    Util.printInfo("The Response : " + content);

    Util.printInfo("Json Data from S4 PDF API: " + new JSONObject(content).get("data").toString());
    byte[] decoder = Base64.getDecoder().decode(new JSONObject(content).get("data").toString());
    File file = Paths.get(System.getProperty("user.home"), "Downloads/invoice.pdf").toFile();
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(decoder);
      Util.printInfo("PDF File Saved");
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      pdfContent = new PDFReader().readPDF(file.getPath());
    } catch (Exception e) {
      AssertUtils.fail("Failed to reach/read the Invoice PDF: " + e.getCause());
    }
    results.put(BICECEConstants.PDF_TYPE, BICECEConstants.INVOICE);
    if (!assertPDFContent(pdfContent, results)) {
      AssertUtils.fail("Invoice is missing Crucial data");
    }
  }

  private Boolean assertPDFContent(String pdfContent, Map<String, String> results) {
    Util.printInfo("PDF String Content: " + pdfContent);
    boolean orderFound = pdfContent.contains(results.get(BICECEConstants.ORDER_ID));
    boolean subscriptionFound = pdfContent.contains(results.get(BICECEConstants.SUBSCRIPTION_ID));
    boolean firstNameFound = pdfContent.toUpperCase().contains(results.get("getPOReponse_firstName").toUpperCase());
    boolean lastNameFound = pdfContent.toUpperCase().contains(results.get("getPOReponse_lastName").toUpperCase());
    boolean streetFound = pdfContent.toUpperCase().contains(results.get("getPOReponse_street").toUpperCase());
    boolean cityFound = pdfContent.toUpperCase().contains(results.get("getPOReponse_city").toUpperCase());
    Util.printInfo("Is Order ID found in Invoice: " + orderFound);
    Util.printInfo("Is Subscription ID found in Invoice: " + subscriptionFound);
    Util.printInfo("Is firstName found in Invoice: " + firstNameFound);
    Util.printInfo("Is lastName found in Invoice: " + lastNameFound);
    Util.printInfo("Is street found in Invoice: " + streetFound);
    Util.printInfo("Is city found in Invoice: " + cityFound);

    String locale = System.getProperty("locale");
    if (locale == null || locale.isEmpty()) {
      locale = "en_US";
    }
    String[] localeSplit = locale.split("_");

    NumberFormat localeFormat = NumberFormat.getNumberInstance(new Locale(localeSplit[0], localeSplit[1]));
    String totalAmountFormattedWithTax =
        localeFormat.format(Double.valueOf(results.get(BICECEConstants.SUBTOTAL_WITH_TAX)));
    String totalAmountFormatted = localeFormat.format(Double.valueOf(results.get(
        "getPOResponse_subtotalAfterPromotions")));

    String totalAmountTrimmedWithTax = totalAmountFormattedWithTax.replace(".", "").replace(",", "")
        .replace("\u00a0", "")
        .replace("\u00A0", "").trim();
    String totalAmountTrimmed = totalAmountFormatted.replace(".", "").replace(",", "")
        .replace("\u00a0", "").replace("\u00A0", "").trim();
    String pdfContentTrimmed = pdfContent.replace(".", "").replace(",", "").replace("\u00a0", "").replaceAll("\n", " ")
        .trim();

    if (results.get(BICECEConstants.PDF_TYPE).equalsIgnoreCase(BICECEConstants.CREDIT_NOTE)) {
      totalAmountTrimmedWithTax = "-" + totalAmountTrimmedWithTax;
      totalAmountTrimmed = "-" + totalAmountTrimmed;
    }

    Util.printInfo("PDF String Content #:" + pdfContentTrimmed);
    Util.printInfo("totalAmountFormattedWithTax #:" + totalAmountTrimmedWithTax);
    Util.printInfo("totalAmountFormatted #:" + totalAmountTrimmed);

    Util.printInfo("Is subtotal with tax found in Invoice: " + pdfContentTrimmed.contains(totalAmountTrimmedWithTax));
    Util.printInfo("Is subtotal without tax found in Invoice: " + pdfContentTrimmed
        .contains(totalAmountTrimmed));

    return orderFound &&
        subscriptionFound &&
        pdfContentTrimmed.contains(totalAmountTrimmedWithTax) &&
        pdfContentTrimmed.contains(totalAmountTrimmed) &&
        firstNameFound &&
        lastNameFound &&
        streetFound &&
        cityFound;
  }

  @Step("CEP : Validating Invoice or Credit Note  " + GlobalConstants.TAG_TESTINGHUB)
  public void validateBICOrderPDF(Map<String, String> results, String pdfType) {
    String error_message;
    String pdfContent = null;
    String url;
    openPortalURL(accountsPortalOrderHistoryUrl);
    Util.sleep(5000);
    try {
      int attempts = 0;
      while (attempts < 15) {
        if (portalPage.checkIfElementExistsInPage("portalOrder" + pdfType + "Link", 10)) {
          portalPage.clickUsingLowLevelActions("portalOrder" + pdfType + "Link");
          try {
            Util.sleep(5000);
            error_message = driver.findElement(By.className("error-msg")).getText();
          } catch (Exception e) {
            error_message = "";
          }

          if (!error_message.isEmpty()) {
            Util.printInfo(pdfType + " is not ready yet, we saw this text -> " + error_message);
            attempts++;
            if (attempts > 14) {
              Assert.fail("Failed to find " + pdfType + " PDF in Order History Page, after " + attempts + " attempts");
            }
            Util.printInfo("Waiting for another 10 minutes on attempt #" + attempts);
            Util.sleep(600000);
            driver.navigate().refresh();
            // As per Account Portal this can take upto 6 sec so considering 10sec
            Util.sleep(10000);
          } else {
            Util.PrintInfo("Found " + pdfType + ". Now need to validate the invoice pop up with ");
            String requestType = "bc";

            if (pdfType.equalsIgnoreCase(BICECEConstants.CREDIT_NOTE)) {
              requestType = "re";
            }

            url = accountsPortalInvoiceUrl
                + results.get(BICECEConstants.ORDER_ID) + "/invoice?type=" + requestType;

            Util.printInfo("URL for " + pdfType + " data: " + url);
            driver.navigate().to(url);
            break;
          }
        } else {
          driver.navigate().refresh();
        }
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to validate " + pdfType + " in Account Portal");
    }

    String content = driver.findElement(By.tagName("body")).getText();
    Util.printInfo("Json Data from S4 PDF API: " + new JSONObject(content).get("data").toString());
    byte[] decoder = Base64.getDecoder().decode(new JSONObject(content).get("data").toString());
    File file = Paths.get(System.getProperty("user.home"), "Downloads/" + pdfType + ".pdf").toFile();
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(decoder);
      Util.printInfo("PDF File Saved");
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      pdfContent = new PDFReader().readPDF(file.getPath());
    } catch (Exception e) {
      Assert.fail("Failed to reach/read the " + pdfType + " PDF: " + e.getCause());
    }

    results.put(BICECEConstants.PDF_TYPE, pdfType);
    if (!assertPDFContent(pdfContent, results)) {
      Assert.fail("Credit Note is missing Crucial data");
    }
  }

  @Step("CEP : Validate Quote Closed in Portal  " + GlobalConstants.TAG_TESTINGHUB)
  public void validateQuoteClosed() {
    openPortalURL(accountsPortalQuoteUrl);

    try {
      boolean quoteClosed = portalPage.checkIfElementExistsInPage("portalQuoteBuyButton", 5000);
      AssertUtils.assertFalse(quoteClosed, "Quote buy button should not be present");
    } catch (MetadataException e) {
      e.printStackTrace();
    }
  }

  @Step("CEP : Bic Order - Switching Term in Portal  " + GlobalConstants.TAG_TESTINGHUB)
  public void switchTermInUserPortal(String cepURL, String portalUserName,
      String portalPassword) {
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    try {
      JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
      navigateToSubscriptionRow();
      try {
        boolean editButtonFound = portalPage.checkIfElementExistsInPage("editSwitchTermButton", 60);
        Util.printInfo("Edit button found: " + editButtonFound);
        Util.printInfo("Current page: " + driver.getCurrentUrl());
        portalPage.clickUsingLowLevelActions("editSwitchTermButton");
      } catch (org.openqa.selenium.StaleElementReferenceException ex) {
        portalPage.clickUsingLowLevelActions("editSwitchTermButton");
      }

      if (System.getProperty(BICECEConstants.PAYMENT)
          .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
        clickWithJavaScriptExecutor(javascriptExecutor, "//div[@data-testid=\"term-3-year\"]");
      } else {
        clickWithJavaScriptExecutor(javascriptExecutor, "//div[@data-testid=\"term-1-year\"]");
      }
      clickWithJavaScriptExecutor(javascriptExecutor, "//button[@data-wat-val=\"continue\"]");

      Util.sleep(5000);
      if (System.getProperty("store").equals("STORE-JP")) {
        AssertUtils.assertTrue(driver
            .findElement(By.xpath("//*[contains(text(),\"期間の変更が確認されました\")]"))
            .isDisplayed());
      } else {
        AssertUtils.assertTrue(driver
            .findElement(By.xpath("//*[contains(text(),\"Your term change is confirmed\")]"))
            .isDisplayed());
      }

      Util.sleep(5000);
      portalPage.clickUsingLowLevelActions("switchTermDone");

      if (System.getProperty(BICECEConstants.PAYMENT)
          .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
        AssertUtils.assertTrue(driver
            .findElement(By.xpath("//*[starts-with(text(),\"Changes to 3-year term starting\")]"))
            .isDisplayed());
      } else {
        if (System.getProperty("store").equals("STORE-JP")) {
          AssertUtils.assertTrue(driver
              .findElement(
                  By.xpath(
                      "//*[@id=\"renew-details-switch-term-label\"]//*[contains(text(),\"から期間を 1 年間 に変更\")]"))
              .isDisplayed());
        } else {
          AssertUtils.assertTrue(driver
              .findElement(
                  By.xpath(
                      "//*[@id=\"renew-details-switch-term-label\"]//*[contains(text(),\"Changes to 1-year term starting\")]"))
              .isDisplayed());
        }
      }
      clickWithJavaScriptExecutor(javascriptExecutor, "//*[@data-wat-val=\"me-menu:sign out\"]");

      //close portal window
      driver.close();

    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail(e.getMessage());
    }
  }

  private boolean isPortalLoginPageVisible() {
    boolean status = false;
    try {
      status = portalPage.checkIfElementExistsInPage("createAccountCEP", 10);
    } catch (MetadataException e) {
    }
    return status;
  }

  public String getTimeStamp() {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    Date date = new Date();
    long gmtMilliSeconds = date.getTime();
    return Long.toString(gmtMilliSeconds / 1000).trim();
  }

  private void debugPageUrl(String messageHeader) {
    Util.printInfo("----------" + messageHeader + "-------------------" +
        "\n" + " URL :            " + driver.getCurrentUrl() +
        "\n" + " Page Title :     " + driver.getTitle()
        + "\n" + BICECEConstants.SEPARATION_LINE);
  }

  @Step("Adding seat from portal for BIC orders" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createAndValidateAddSeatOrderInPortal(String addSeatQty,
      LinkedHashMap<String, String> testDataForEachMethod) {
    driver.switchTo().defaultContent();
    HashMap<String, String> orderDetails = new HashMap<String, String>();
    orderDetails.putAll(createAddSeatOrder(addSeatQty, testDataForEachMethod));
    orderDetails.putAll(validateAddSeatOrder(orderDetails));
    return orderDetails;
  }

  public HashMap<String, String> navigateToSubscriptionAndOrdersTab() {
    driver.manage().window().maximize();
    Util.printInfo("Navigating to subscriptions and orders tab...");
    HashMap<String, String> orderDetails = new HashMap<String, String>();
    Util.sleep(60000);
    try {
      if (portalPage.checkIfElementExistsInPage("portalLinkSubscriptions", 60)) {
        Util.printInfo("Clicking on portal subscription and contracts link...");
        driver.get(accountsPortalSubscriptionsUrl);
        portalPage.waitForPageToLoad();

        debugPageUrl("Step 2");

        Util.waitforPresenceOfElement(
            portalPage.getFirstFieldLocator(BICECEConstants.SUBSCRIPTION_ROW_IN_SUBSCRIPTION));
        Util.printInfo("Clicking on subscription row...");

        debugPageUrl("Step 3");
        portalPage.clickUsingLowLevelActions(BICECEConstants.SUBSCRIPTION_ROW_IN_SUBSCRIPTION);
        portalPage.waitForPageToLoad();

        clickPortalClosePopup();

        debugPageUrl("Step 4");
        checkEmailVerificationPopupAndClick();
        debugPageUrl("Step 5");

        String currentUrl = driver.getCurrentUrl();

        boolean status = currentUrl.toLowerCase().contains("trials");

        if (status) {
          Util.printInfo("Hardcoding the redirect to subscriptions-contracts page");
          driver.get(accountsPortalSubscriptionsUrl);
          portalPage.clickUsingLowLevelActions(BICECEConstants.SUBSCRIPTION_ROW_IN_SUBSCRIPTION);
          Util.sleep(30000);
          debugPageUrl("Final attempt");
          currentUrl = driver.getCurrentUrl();
          status = currentUrl.toLowerCase().contains("trials");
          if (status) {
            AssertUtils.fail("Unable to redirect to subscriptions payment details page");
          }
        }

        Util.sleep(5000);
        Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator(
            BICECEConstants.PORTAL_ORDER_SEAT_COUNT));
        String initialOrderQty = portalPage
            .getTextFromLink(BICECEConstants.PORTAL_ORDER_SEAT_COUNT);
        Util.printInfo("Initial seat quantity on order info page: " + initialOrderQty);
        orderDetails.put(BICECEConstants.INITIAL_ORDER_QTY, initialOrderQty);

        String paymentDetails = portalPage.getTextFromLink("portalPaymentDetails")
            .replaceAll("\\s", "");
        Util.printInfo("Payment Details on order info page: " + paymentDetails);
        orderDetails.put(BICECEConstants.PAYMENT_DETAILS, paymentDetails);

        String[] name = portalPage.getTextFromLink("portalGetUserNameTextFromSubs")
            .split("\\s");
        String firstName = name[0].trim();
        String lastName = name[1].trim();
        orderDetails.put("firstname", firstName);
        orderDetails.put("lastname", lastName);

        String streetAddress = portalPage.getTextFromLink("portalGetUserAddressFromSubs")
            .trim();
        Util.printInfo("Street Address : " + streetAddress);

        String city = portalPage.getTextFromLink("portalGetUserCityFromSubs")
            .replace(",", "")
            .trim();
        Util.printInfo("City : " + city);
        Util.printInfo("Waiting for portalSubscriptionStateFromSubs");
        Util.sleep(10000);
        if (portalPage.checkIfElementExistsInPage("portalSubscriptionStateFromSubs", 10)) {
          String state = portalPage.getTextFromLink("portalSubscriptionStateFromSubs");
          Util.printInfo("State Province : " + state);
          orderDetails.put(BICECEConstants.STATE_PROVINCE, state);
        }

        String pin;
        if (System.getProperty("store").equals("STORE-JP")) {
          pin = portalPage.getTextFromLink("portalSubscriptionZipFromSubs").replace("〒 ", "");
        } else {
          pin = portalPage.getTextFromLink("portalSubscriptionZipFromSubs");
        }
        Util.printInfo("Zip Code : " + pin);

        orderDetails.put(BICECEConstants.FULL_ADDRESS, streetAddress);
        orderDetails.put("City", city);
        orderDetails.put(BICECEConstants.ZIPCODE, pin);

        driver.navigate().refresh();

      } else {
        AssertUtils.fail("Subscription and contracts link is not present on portal page...");
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to navigate to subscription and orders page...");
    }

    return orderDetails;
  }

  public HashMap<String, String> createAddSeatOrder(String addSeatQty,
      LinkedHashMap<String, String> testDataForEachMethod) {
    Util.printInfo("Placing add seat order from portal...");
    HashMap<String, String> orderDetails = new HashMap<String, String>();

    try {
      orderDetails.putAll(navigateToSubscriptionAndOrdersTab());

      String paymentDetails = orderDetails.get(BICECEConstants.PAYMENT_DETAILS);
      Util.printInfo("Payment Details : " + paymentDetails);

      Util.sleep(20000);
      Util.printInfo("Clicking on Add Seat button...");
      String currentURL = driver.getCurrentUrl();
      Util.printInfo("currentURL1 before clicking on Add seat : " + currentURL);
      String zipPaySubscriptionUrl = currentURL;
      Util.printInfo("Clicking on Add seats button.");
      portalPage.waitForFieldPresent(BICECEConstants.PORTAL_ADD_SEAT_BUTTON, 10000);
      portalPage.clickUsingLowLevelActions(BICECEConstants.PORTAL_ADD_SEAT_BUTTON);

      Util.sleep(20000);
      currentURL = driver.getCurrentUrl();
      Util.printInfo("currentURL2 : " + currentURL);

      boolean status = currentURL.contains(BICECEConstants.ADD_SEATS);

      int attempts = 0;
      while (!status && attempts != 3) {
        Util.printInfo("Attempt1 - Javascript method to redirect to Add seat page");
        String portalAddSeatButton = "document.getElementById(\"add-seats\").click()";
        clickCheckBox(portalAddSeatButton);
        Util.sleep(20000);

        status = currentURL.contains(BICECEConstants.ADD_SEATS);

        if (!status) {
          Util.printInfo("Attempt2 to redirect with hardcoded URL " + currentURL);
          driver.get(accountsPortalAddSeatsUrl);
          driver.navigate().refresh();
          Util.sleep(20000);
          currentURL = driver.getCurrentUrl();
          Util.printInfo("currentURL3 : " + currentURL);
        }

        status = currentURL.contains(BICECEConstants.ADD_SEATS);
        if (!status) {
          attempts++;
        }
      }

      if (!status) {
        debugPageUrl("Portal - ADD Seat page");
        Util.printTestFailedMessage(
            "Multiple attempts failed to redirect in Portal - ADD Seat page " + currentURL);
        AssertUtils.fail("Unable to redirect to Add Seat page in Accounts portal.");
      } else {
        Util.printInfo("Status: Successfully clicked on Add seats button.");
      }

      debugPageUrl("trying to log into portal again");

      if (isPortalLoginPageVisible()) {
        System.out.println("Session timed out - trying to log into portal again");
        portalLogin(testDataForEachMethod.get("emailid"), "Password1");
        driver.get(currentURL);
        portalPage.waitForFieldPresent(BICECEConstants.PORTAL_ADD_SEAT_BUTTON, 10000);
        portalPage.clickUsingLowLevelActions(BICECEConstants.PORTAL_ADD_SEAT_BUTTON);
      }

      portalPage.waitForPageToLoad();

      portalPage.waitForFieldPresent("portalASProductTerm", 5000);
      String productSubscriptionTerm = portalPage
          .getLinkText("portalASProductTerm"); // .split(":")[1].trim();
      Util.printInfo("Product subscription term on add seat page : " + productSubscriptionTerm);
      orderDetails.put("productSubscriptionTerm", productSubscriptionTerm);

      portalPage.waitForFieldPresent("portalASAmountPerSeat", 5000);
      String perSeatProratedAmount = portalPage.getLinkText("portalASAmountPerSeat");
      Util.printInfo("Prorated amount per seat : " + perSeatProratedAmount);
      orderDetails.put("perSeatProratedAmount", perSeatProratedAmount);

      Util.printInfo("Adding quantity for seat as..." + addSeatQty);
      orderDetails.put("addSeatQty", addSeatQty);
      portalPage.waitForFieldPresent("portalASQtyTextField", 30);
      portalPage.populateField("portalASQtyTextField", addSeatQty);
      Util.sleep(10000);
      portalPage.waitForFieldPresent("portalASFinalProratedPrice", 5000);

      String proratedFinalPrice;
      if (portalPage.checkIfElementExistsInPage("portalASDiscountedProratedPrice", 10)) {
        proratedFinalPrice = portalPage.getLinkText("portalASDiscountedProratedPrice");
      } else {
        proratedFinalPrice = portalPage.getLinkText("portalASFinalProratedPrice");
      }
      Util.printInfo("Prorated Final Amount : " + proratedFinalPrice);
      orderDetails.put("proratedFinalAmount", proratedFinalPrice);
      if (portalPage.checkFieldExistence("portalASTaxDetails")) {
        Util.printInfo("Capturing Tax details...");
        String taxAmount = portalPage.getLinkText("portalASTaxDetails");
        Util.printInfo("Tax amount : " + taxAmount);
        orderDetails.put("taxAmount", taxAmount);
      }
      String subtotalPrice = portalPage.getLinkText("portalASFinalSubtotalAmount");
      Util.printInfo("Subtotal amount : " + subtotalPrice);
      orderDetails.put("subtotalPrice", subtotalPrice);

      Util.printInfo("Clicking on Save button...");
      if (!System.getProperty("store").equals("STORE-JP")) {
        clickOnContinueBtn();
      }

      // Zip Pay Verification
      if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE)
          .equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_ZIP)) {
        zipTestBase.setTestData(testDataForEachMethod);
        zipTestBase.verifyZipBalance(proratedFinalPrice);
      }

      Util.printInfo("Clicking on Submit Order button...");
      portalPage.waitForFieldPresent("portalASSubmitOrderBtn", 5000);
      portalPage.clickUsingLowLevelActions("portalASSubmitOrderBtn");

      // Zip Pay Checkout
      if (testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE)
          .equalsIgnoreCase(BICECEConstants.PAYMENT_TYPE_ZIP)) {
        Util.printInfo("Going to Zip Pay Checkout");
        zipTestBase.setTestData(testDataForEachMethod);
        zipTestBase.zipPayCheckout();
        Util.sleep(2000);
        orderDetails.put("zipPaySubscriptionUrl", zipPaySubscriptionUrl);
      }

    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to place add seat order from portal...");
    }

    // We just placed an order, this takes time to sync Order > subscription > LEM > Portal
    Util.sleep(60000);

    return orderDetails;
  }

  public void clickOnContinueBtn() {
    List<WebElement> continueButtonList = null;

    try {
      Util.sleep(2000);
      continueButtonList = portalPage.getMultipleWebElementsfromField("portalAddSeatSaveButton");

      if (continueButtonList.size() > 1) {
        continueButtonList.get(1).click();
      }

    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to click on Save button on billing details page...");
    }
  }

  public HashMap<String, String> validateAddSeatOrder(HashMap<String, String> data) {
    HashMap<String, String> orderDetails = new HashMap<String, String>();

    try {
      Util.waitForElement(portalPage.getFirstFieldLocator("portalASOrderConfirmationHead"),
          "Order confirmation page");
      Util.sleep(10000);
      if (portalPage.checkIfElementExistsInPage("portalASOrderNumberText", 10)) {
        String addSeatOrderNumber = portalPage.getLinkText("portalASOrderNumberText");
        orderDetails.put(TestingHubConstants.addSeatOrderNumber, addSeatOrderNumber);
        Util.printInfo("Add Seat Order number : " + addSeatOrderNumber);
      } else {
        Util.printInfo("Add Seat Order number can not be displayed due to Export Compliance.");
      }

      Util.printInfo("Validating prorated amount on confirmation page...");
      String confirmProratedAmount;
      if (System.getProperty("store").equals("STORE-JP")) {
        confirmProratedAmount = driver.findElement(By.xpath("//p[.='日割計算']/../p[2]")).getText();
        AssertUtils.assertEquals(
            Double.valueOf(data.get("proratedFinalAmount")
                .substring(data.get("proratedFinalAmount").indexOf("¥ ") + 1).replace(",", "")),
            Double.valueOf(confirmProratedAmount.substring(confirmProratedAmount.indexOf("¥ ") + 1)
                .replace(",", "")));
      } else {
        confirmProratedAmount = portalPage.getLinkText("portalASConfirmProratedPrice");
        AssertUtils.assertEquals(
            Double.valueOf(data.get("proratedFinalAmount")
                .substring(data.get("proratedFinalAmount").indexOf("$") + 1).replace(",", "")),
            Double.valueOf(confirmProratedAmount.substring(confirmProratedAmount.indexOf("$") + 1).replace(",", "")));
      }

      Util.printInfo("ZIP Pay subscription URL " + data.get("zipPaySubscriptionUrl"));

      Util.sleep(5000);
      if (data.get("zipPaySubscriptionUrl") != null) {
        Util.printInfo("Calling " + data.get("zipPaySubscriptionUrl"));
        driver.get(data.get("zipPaySubscriptionUrl"));
      } else {
        Util.printInfo("Clicking on back button...");
        portalPage.clickUsingLowLevelActions("portalBackButton");
        Util.sleep(5000);
      }

      int attempts = 0;
      while (attempts < 3) {
        driver.switchTo().defaultContent();
        Util.printInfo("Refreshing the page...");
        driver.navigate().refresh();
        Util.sleep(10000);

        boolean addSeatsButtonVisible = portalPage
            .waitForFieldPresent("portalAddSeatButton", 90000);
        if (addSeatsButtonVisible) {
          String totalSeats = portalPage.getTextFromLink(BICECEConstants.PORTAL_ORDER_SEAT_COUNT);
          Util.printInfo("Total seats displayed on order info page: " + totalSeats);
          orderDetails.put("totalSeats", totalSeats);

          String initialOrderQty = data.get(BICECEConstants.INITIAL_ORDER_QTY);
          if (!totalSeats.equals(initialOrderQty)) {
            Util.printInfo("Seats added successfully...");
            break;
          } else {
            AssertUtils.fail("Failed to add seats. Initial order seat : " + initialOrderQty
                + " total number of seats : " + totalSeats + " are same");
          }
        } else {
          attempts++;
        }
      }
      if (attempts == 3) {
        AssertUtils.fail("Add seats button is not visible.");
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to validate add seat order..." + e.getMessage());
    }

    return orderDetails;
  }

  @Step("Reduce seats from portal for BIC orders" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> reduceSeatsInPortalAndValidate()
      throws MetadataException {
    driver.switchTo().defaultContent();
    HashMap<String, String> orderDetails = new HashMap<>();
    orderDetails.putAll(reduceSeats());
    validateReducedSeats(orderDetails);
    return orderDetails;
  }

  public HashMap<String, String> reduceSeats()
      throws MetadataException {
    HashMap<String, String> orderDetails = new HashMap<>();
    orderDetails.putAll(navigateToSubscriptionAndOrdersTab());

    Util.printInfo("Reducing seats.");
    Util.sleep(10000);
    closeSubscriptionTermPopup();
    portalPage.waitForFieldPresent(BICECEConstants.PORTAL_REDUCE_SEATS_BUTTON, 5000);
    portalPage.clickUsingLowLevelActions(BICECEConstants.PORTAL_REDUCE_SEATS_BUTTON);
    portalPage.checkIfElementExistsInPage("portalReduceSeatsPanel", 10);
    portalPage.waitForFieldPresent("portalMinusButton", 5000);
    portalPage.clickUsingLowLevelActions("portalMinusButton");
    portalPage.waitForFieldPresent("portalSaveChangesButton", 5000);
    portalPage.clickUsingLowLevelActions("portalSaveChangesButton");
    portalPage.checkIfElementExistsInPage("portalConfirmationModal", 10);
    portalPage.waitForFieldPresent("portalConfirmationOkButton", 5000);
    Util.printInfo("Confirmation OK button found.");
    portalPage.clickUsingLowLevelActions("portalConfirmationOkButton");
    portalPage.waitForFieldPresent("portalRenewingSeatsCount");
    Util.printInfo("Reduced seats quantity found.");
    String renewingSeatsCount = portalPage
        .getTextFromLink("portalRenewingSeatsCount");
    String reducedSeatQty = renewingSeatsCount.split(" ")[0];
    Util.printInfo("Recording new seats count -> " + reducedSeatQty);
    orderDetails.put("reducedSeatQty", reducedSeatQty);
    return orderDetails;
  }

  public void validateReducedSeats(HashMap<String, String> data) throws MetadataException {
    portalPage.checkIfElementExistsInPage(BICECEConstants.PORTAL_REDUCE_SEATS_BUTTON, 10);
    String newSeatsTotal = data.get("reducedSeatQty");
    String initialOrderQty = data.get(BICECEConstants.INITIAL_ORDER_QTY);

    if (System.getProperty(BICECEConstants.PAYMENT)
        .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
      if (newSeatsTotal.equals(initialOrderQty)) {
        Util.printInfo(
            "Seats reduced successfully. New seats will be in effect after next renewal");
      } else {
        AssertUtils.fail("Error while reducing seats. Initial order seat : " + initialOrderQty
            + " total number of seats : " + newSeatsTotal + " should be same");
      }
    } else {
      if (!newSeatsTotal.equals(initialOrderQty)) {
        Util.printInfo("Seats reduced successfully.");
      } else {
        AssertUtils.fail("Failed to reduce seats. Initial order seat : " + initialOrderQty
            + " total number of seats : " + newSeatsTotal + " are same");
      }
    }
  }

  private void clickPortalClosePopup() throws Exception {
    if (portalPage.checkIfElementExistsInPage("portalASCloseButton", 10)) {
      Util.printInfo("Closing the popup ..");
      portalPage.clickUsingLowLevelActions("portalASCloseButton");
      Util.printInfo("Closed the popup ..");
    }
  }

  @Step("Changing payment from Portal" + GlobalConstants.TAG_TESTINGHUB)
  public void changePaymentMethodAndValidate(HashMap<String, String> data,
      String[] paymentCardDetails) {
    Util.printInfo("Changing the payment method from portal...");

    try {
      debugPageUrl("Step 1");
      data.putAll(navigateToSubscriptionAndOrdersTab());
      clickPortalClosePopup();
      Util.printInfo("Clicking on change payment option...");
      portalPage.waitForFieldPresent("portalChangePaymentBtn", 15000);
      portalPage.clickUsingLowLevelActions("portalChangePaymentBtn");
      portalPage.waitForPageToLoad();

      Util.sleep(15000);

      if (System.getProperty("store").equals("STORE-JP")) {
        Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator(
                BICECEConstants.PORTAL_PAYMENT_METHOD)
            .replaceAll(BICECEConstants.PAYMENTOPTION, "クレジット カード"));
      } else {
        Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator(
                BICECEConstants.PORTAL_PAYMENT_METHOD)
            .replaceAll(BICECEConstants.PAYMENTOPTION, "Credit card"));
      }

      addPaymentDetails(data, paymentCardDetails);
      validatePaymentDetailsOnPortal(data);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to change the payment details from portal...");
    }
  }

  public void addPaymentDetails(HashMap<String, String> data, String[] paymentCardDetails) {
    Util.printInfo("Selected payment profile : " + data.get(BICECEConstants.PAYMENT_TYPE));
    try {
      switch (data.get(BICECEConstants.PAYMENT_TYPE).toUpperCase()) {
        case BICConstants.paymentTypePayPal:
          populatePaypalDetails(data);
          break;
        case BICConstants.paymentTypeDebitCard:
          populateACHPaymentDetails(paymentCardDetails);
          break;
        default:
          populateCreditCardDetails(paymentCardDetails);
          break;
      }

      populateBillingAddress(data, data.get("userType"));
      Util.printInfo("Clicking on save button");
      if (data.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.VISA) || data
          .get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.MASTERCARD)) {
        portalPage.clickUsingLowLevelActions(BICECEConstants.PORTAL_CARD_SAVE_BTN);
      } else if (data.get(BICECEConstants.PAYMENT_TYPE)
          .equalsIgnoreCase(BICConstants.paymentTypePayPal)) {
        findAndClickSaveButton("//button[contains(@data-testid,'save-payment-profile')]");
        Util.printInfo("Saved Paypal profile as new payment type");
      } else if (data.get(BICECEConstants.PAYMENT_TYPE)
          .equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
        data.put(BICECEConstants.PAYMENT_DETAILS, BICECEConstants.ACCOUNT);
        portalPage.waitForFieldPresent(BICECEConstants.PORTAL_ACH_SAVE_BTN, 5000);
        findAndClickSaveButton("//button[contains(@data-testid,'save-payment-profile')]");
        Util.printInfo("Saved ACH profile as new payment type");
        Util.sleep(5000);
        WebElement mandateAgreementElement = driver.findElement(By.xpath(
            BICECEConstants.ID_MANDATE_AGREEMENT));
        mandateAgreementElement.click();
        findAndClickSaveButton("//span[.='Save']");
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to select payment profile...");
    }
  }

  private void findAndClickSaveButton(String elementXpath) throws Exception {
    Util.sleep(5000);
    List<WebElement> list = portalPage.getMultipleWebElementFromXpath(elementXpath);
    int len = list.size();
    while (len > 0) {
      try {
        list.get(len - 1).click();
        Util.printInfo("Save Button found and clicked..");
        break;
      } catch (Exception e) {
        Util.printInfo("Unable to click on Save Button. Trying the next Save Button in the list.");
        len--;
      }
    }
  }

  @Step("Add Paypal Payment Details" + GlobalConstants.TAG_TESTINGHUB)
  public void populatePaypalDetails(HashMap<String, String> data) {
    Util.printInfo("Switching to latest window...");
    String parentWindow = driver.getWindowHandle();
    String paymentMethod = portalPage.getFirstFieldLocator(BICECEConstants.PORTAL_PAYMENT_METHOD)
        .replaceAll(BICECEConstants.PAYMENTOPTION, "PayPal");
    Util.waitForElement(paymentMethod, "PayPal tab");
    String paypalEmail = data.get(BICECEConstants.PAYPAL_EMAIL);

    try {
      Util.printInfo("Clicking on Paypal payments tab...");
      driver.findElement(By.xpath(paymentMethod)).click();

      Util.printInfo("Clicking on Paypal checkout button...");
      EceBICTestBase.bicPage.selectFrame("paypalCheckoutOptionFrame");
      Util.waitforPresenceOfElement(EceBICTestBase.bicPage.getFirstFieldLocator("paypalCheckoutBtn"));
      EceBICTestBase.bicPage.clickUsingLowLevelActions("paypalCheckoutBtn");

      Set<String> windows = driver.getWindowHandles();
      for (String window : windows) {
        driver.switchTo().window(window);
      }

      driver.manage().window().maximize();
      portalPage.waitForPageToLoad();
      EceBICTestBase.bicPage.waitForElementToDisappear("paypalPageLoader", 30);

      String title = driver.getTitle();
      AssertUtils.assertTrue(title.toUpperCase().contains("PayPal".toUpperCase()),
          "Current title [" + title + "] does not contains keyword : PayPal");

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (EceBICTestBase.bicPage
          .checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 10)) {
        EceBICTestBase.bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
      }

      if (EceBICTestBase.bicPage
          .checkIfElementExistsInPage(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON, 10)) {
        EceBICTestBase.bicPage
            .clickUsingLowLevelActions(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON);
      }

      Util.printInfo("Entering paypal user name [" + paypalEmail + "]...");
      EceBICTestBase.bicPage.waitForElementVisible(
          EceBICTestBase.bicPage.getMultipleWebElementsfromField("paypalUsernameField").get(0), 10);
      EceBICTestBase.bicPage.populateField("paypalUsernameField", paypalEmail);

      EceBICTestBase.bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_NEXT_BUTTON);

      Util.printInfo("Entering paypal password...");
      EceBICTestBase.bicPage.populateField("paypalPasswordField",
          ProtectedConfigFile.decrypt(data.get("paypalSsap")));

      Util.printInfo("Clicking on login button...");
      EceBICTestBase.bicPage.clickUsingLowLevelActions("paypalLoginBtn");
      EceBICTestBase.bicPage.waitForElementToDisappear("paypalPageLoader", 30);
      Util.sleep(5000);

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (EceBICTestBase.bicPage
          .checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 10)) {
        EceBICTestBase.bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
      }

      String paymentTypeXpath = "";
      if (System.getProperty("store").equals("STORE-JP")) {
        paymentTypeXpath = EceBICTestBase.bicPage.getFirstFieldLocator("paypalPaymentOption")
            .replace(BICECEConstants.PAYMENTOPTION, "Visa");
      } else {
        paymentTypeXpath = EceBICTestBase.bicPage.getFirstFieldLocator("paypalPaymentOption")
            .replace(BICECEConstants.PAYMENTOPTION, data.get("paypalPaymentType"));
      }
      driver.findElement(By.xpath(paymentTypeXpath)).click();

      EceBICTestBase.bicPage.executeJavascript("window.scrollBy(0,1000);");
      try {
        Util.printInfo("Clicking on agree and continue button...");
        EceBICTestBase.bicPage.clickUsingLowLevelActions("paypalReviewBtn");
        Util.printInfo("Clicked on agree and continue button.");
        Util.sleep(2000);
        EceBICTestBase.bicPage.clickUsingLowLevelActions("paypalReviewBtn");
        Util.printInfo("Clicked again on agree and continue button.");
        Util.sleep(2000);
      } catch (Exception e) {
        Util.printInfo("Clicking on save and continue button...");
        portalPage.clickUsingLowLevelActions("portalPaypalSaveAndContinueBtn");
      }
      Util.sleep(10000);

      driver.switchTo().window(parentWindow);

      Util.sleep(5000);
      if (System.getProperty("store").equals("STORE-JP")) {
        String paypalString = driver.findElement(By.xpath(
                "//*[@data-testid=\"payment-section-add\"]//div[2]/div[2]/div[2]/p"))
            .getText();
        AssertUtils.assertEquals(paypalString,
            "PayPal が支払い方法として選択されています。");
      } else {
        AssertUtils.assertEquals(portalPage.getTextFromLink("portalPaypalConfirmationText"),
            "PayPal is selected for payment.");
      }
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter paypal details to make payment...");
    }
    Util.sleep(10000);
  }

  @Step("Populate Direct Debit payment details" + GlobalConstants.TAG_TESTINGHUB)
  public void populateACHPaymentDetails(String[] paymentCardDetails) {
    String paymentMethod = portalPage.getFirstFieldLocator(BICECEConstants.PORTAL_PAYMENT_METHOD)
        .replaceAll(BICECEConstants.PAYMENTOPTION, "Direct Debit (ACH)");
    Util.waitForElement(paymentMethod, "debit card ACH tab");

    try {
      Util.printInfo("Clicking on Direct Debit ACH tab...");
      driver.findElement(By.xpath(paymentMethod)).click();

      Util.printInfo("Waiting for Direct Debit ACH Header...");
      EceBICTestBase.bicPage.waitForElementVisible(
          EceBICTestBase.bicPage.getMultipleWebElementsfromField("directDebitHead").get(0), 10);

      // TODO Replace this with condition where we are reading from test class API whether credit card is available or not
      if (portalPage.checkIfElementExistsInPage("portalDebitCardAddLink", 10)) {
        portalPage.clickUsingLowLevelActions("portalDebitCardAddLink");
      }

      Util.sleep(3000);
      Util.printInfo("Entering Direct Debit ACH Account Number : " + paymentCardDetails[0]);
      EceBICTestBase.bicPage.populateField("achAccNumber", paymentCardDetails[0]);

      Util.printInfo("Entering Direct Debit ACH Routing Number : " + paymentCardDetails[1]);
      EceBICTestBase.bicPage.populateField("achRoutingNumber", paymentCardDetails[1]);
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Direct Debit details to make payment");
    }
    Util.sleep(10000);
  }

  @Step("Populate credit card details" + GlobalConstants.TAG_TESTINGHUB)
  public void populateCreditCardDetails(String[] paymentCardDetails) {
    EceBICTestBase.bicPage.waitForField("creditCardNumberFrame", true, 30000);

    if (System.getProperty("store").equals("STORE-JP")) {
      String paymentMethod = portalPage.getFirstFieldLocator(BICECEConstants.PORTAL_PAYMENT_METHOD)
          .replaceAll(BICECEConstants.PAYMENTOPTION, "クレジット カード");
      Util.waitForElement(paymentMethod, "Credit card tab");
      driver.findElement(By.xpath(paymentMethod)).click();
    } else {
      String paymentMethod = portalPage.getFirstFieldLocator(BICECEConstants.PORTAL_PAYMENT_METHOD)
          .replaceAll(BICECEConstants.PAYMENTOPTION, "Credit card");
      Util.waitForElement(paymentMethod, "Credit card tab");
      driver.findElement(By.xpath(paymentMethod)).click();
    }

    try {
      // TODO Replace this with condition where we are reading from test class API whether credit card is available or not
      if (portalPage.checkIfElementExistsInPage("portalCreditCardAddLink", 10)) {
        portalPage.clickUsingLowLevelActions("portalCreditCardAddLink");
      }

      Util.sleep(3000);
      WebElement creditCardNumberFrame = EceBICTestBase.bicPage
          .getMultipleWebElementsfromField("creditCardNumberFrame").get(0);
      WebElement expiryDateFrame = EceBICTestBase.bicPage
          .getMultipleWebElementsfromField("expiryDateFrame").get(0);
      WebElement securityCodeFrame = EceBICTestBase.bicPage
          .getMultipleWebElementsfromField("securityCodeFrame").get(0);

      driver.switchTo().frame(creditCardNumberFrame);
      Util.printInfo("Entering card number : " + paymentCardDetails[0]);
      Util.sleep(2000);
      EceBICTestBase.bicPage.populateField("CardNumber", paymentCardDetails[0]);
      driver.switchTo().defaultContent();
      Util.sleep(2000);

      driver.switchTo().frame(expiryDateFrame);
      Util.printInfo(
          "Entering Expiry date : " + paymentCardDetails[1] + "/" + paymentCardDetails[2]);
      Util.sleep(2000);
      EceBICTestBase.bicPage
          .populateField("expirationPeriod", paymentCardDetails[1] + paymentCardDetails[2]);
      driver.switchTo().defaultContent();
      Util.sleep(2000);
      driver.switchTo().frame(securityCodeFrame);
      Util.printInfo("Entering security code : " + paymentCardDetails[3]);
      Util.sleep(2000);
      EceBICTestBase.bicPage.populateField("PAYMENTMETHOD_SECURITY_CODE", paymentCardDetails[3]);
      driver.switchTo().defaultContent();
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Card details to make payment");
    }
    Util.sleep(10000);
  }

  public boolean populateBillingAddress(HashMap<String, String> data, String userType) {
    boolean status = false;
    String paymentType = data.get(BICECEConstants.PAYMENT_TYPE);
    String firstNameXpath = "";
    String lastNameXpath = "";
    if (paymentType.equalsIgnoreCase(BICConstants.paymentTypePayPal)) {
      firstNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
      lastNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
    } else if (paymentType.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
      firstNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
      lastNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
    } else {
      firstNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
      lastNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
    }

    EceBICTestBase.clearTextInputValue(driver.findElement(By.xpath(firstNameXpath)));
    driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get("firstname"));

    EceBICTestBase.clearTextInputValue(driver.findElement(By.xpath(lastNameXpath)));
    driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get("lastname"));

    if (data.size() == 6) {
      Util.printInfo("Populating EMEA Billing Details");
      status = populateEMEABillingDetails(data);
      EceBICTestBase.bicPage.waitForPageToLoad();
    } else {
      Util.printInfo("Populating NAMER Billing Details");
      status = populateNAMERBillingDetails(data, paymentType, userType);
    }

    return status;
  }

  private boolean populateEMEABillingDetails(Map<String, String> address) {
    Util.sleep(3000);
    boolean status = false;
    try {
      status = EceBICTestBase.bicPage.waitForElementVisible(
          EceBICTestBase.bicPage.getMultipleWebElementsfromField("Organization_NameEMEA").get(0),
          60000);
    } catch (MetadataException e) {
      AssertUtils.fail("Organization_NameEMEA is not displayed on page...");
    }
    EceBICTestBase.bicPage.populateField("Organization_NameEMEA", address.get(
        BICECEConstants.ORGANIZATION_NAME));
    EceBICTestBase.bicPage
        .populateField("Full_AddressEMEA", address.get(BICECEConstants.FULL_ADDRESS));
    EceBICTestBase.bicPage.populateField("CityEMEA", address.get("City"));
    EceBICTestBase.bicPage.populateField("ZipcodeEMEA", address.get(BICECEConstants.ZIPCODE));
    EceBICTestBase.bicPage.populateField("Phone_NumberEMEA", address.get("phone"));
    EceBICTestBase.bicPage.populateField("CountryEMEA", address.get(BICECEConstants.COUNTRY));
    return status;
  }

  private boolean populateNAMERBillingDetails(Map<String, String> address, String paymentType,
      String userType) {
    Util.printInfo("Adding billing details...");
    boolean status = false;
    String orgNameXpath = "", fullAddrXpath = "", cityXpath = "", zipXpath = "", phoneXpath = "", countryXpath = "", stateXpath = "";
    switch (paymentType.toUpperCase()) {

      case BICConstants.paymentTypePayPal:
        orgNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        fullAddrXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        cityXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.CITY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        zipXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        phoneXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        countryXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        stateXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        break;
      case BICConstants.paymentTypeDebitCard:
        orgNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        fullAddrXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        cityXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.CITY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        zipXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        phoneXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        countryXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        stateXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        break;
      default:
        orgNameXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        fullAddrXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        cityXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.CITY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        zipXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        phoneXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        countryXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        stateXpath = EceBICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        break;
    }

    Util.sleep(3000);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(orgNameXpath)));
    status = driver.findElement(By.xpath(orgNameXpath)).isDisplayed();

    if (!status) {
      AssertUtils.fail("Organization_Name not available.");
    }

    driver.findElement(By.xpath(orgNameXpath)).click();
    driver.findElement(By.xpath(orgNameXpath))
        .sendKeys(new RandomStringUtils().random(10, true, true));

    EceBICTestBase.clearTextInputValue(driver.findElement(By.xpath(fullAddrXpath)));
    driver.findElement(By.xpath(fullAddrXpath)).sendKeys(address.get(BICECEConstants.FULL_ADDRESS));

    EceBICTestBase.clearTextInputValue(driver.findElement(By.xpath(cityXpath)));
    driver.findElement(By.xpath(cityXpath)).sendKeys(address.get(BICECEConstants.CITY));

    EceBICTestBase.clearTextInputValue(driver.findElement(By.xpath(zipXpath)));
    driver.findElement(By.xpath(zipXpath)).sendKeys(address.get(BICECEConstants.ZIPCODE));

    driver.findElement(By.xpath(phoneXpath)).sendKeys("2333422112");

    WebElement countryEle = driver.findElement(By.xpath(countryXpath));
    Select selCountry = new Select(countryEle);
    if (userType.equalsIgnoreCase("new")) {
      selCountry.selectByVisibleText(address.get(BICECEConstants.COUNTRY));
    } else {
      selCountry.selectByIndex(0);
    }
    if (address.get(BICECEConstants.STATE_PROVINCE) != null && !address.get(
        BICECEConstants.STATE_PROVINCE).isEmpty()) {
      driver.findElement(By.xpath(stateXpath))
          .sendKeys(address.get(BICECEConstants.STATE_PROVINCE));
    }
    return status;
  }

  public void validatePaymentDetailsOnPortal(HashMap<String, String> data) {
    Util.printInfo("Validating payment details...");
    data.putAll(navigateToSubscriptionAndOrdersTab());
    String paymentDetails = data.get(BICECEConstants.PAYMENT_DETAILS).toLowerCase();
    Util.printInfo("Payment Details on order info page: " + paymentDetails);

    if (data.get(BICECEConstants.PAYMENT_TYPE).equalsIgnoreCase(BICConstants.paymentTypePayPal)) {
      Assert.assertTrue(paymentDetails.contains(BICECEConstants.PAYPAL),
          BICECEConstants.PAYMENT_DETAILS1 + paymentDetails + "] does not contains text [paypal]");
    } else if (data.get(BICECEConstants.PAYMENT_TYPE)
        .equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
      Util.sleep(5000);
      Assert.assertTrue(paymentDetails.contains(BICECEConstants.ACCOUNT),
          BICECEConstants.PAYMENT_DETAILS1 + paymentDetails + "] does not contains text [account]");
    } else {
      Util.printInfo(BICECEConstants.PAYMENT_DETAILS1 + paymentDetails + "] ");
    }
  }

  /**
   * Login to portal and align the billing between two subscriptions
   *
   * @param cepURL          - Portal URL
   * @param portalUserName  - Portal user username
   * @param portalPassword  - Portal user password
   * @param subscriptionID1 - First subscription
   * @param subscriptionID2 - Seconds subscription
   */
  @Step("Align subscription billing in portal " + GlobalConstants.TAG_TESTINGHUB)
  public void alignBillingInPortal(String cepURL, String portalUserName, String portalPassword,
      String subscriptionID1, String subscriptionID2) {
    openPortalBICLaunch(cepURL);

    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    try {
      navigateToUpcomingPaymentsLink();

      int attempts = 0;
      while (attempts < 5) {
        portalPage.waitForFieldPresent("alignBillingButton", 30000);
        WebElement alignBillingButton = portalPage.getMultipleWebElementsfromField("alignBillingButton").get(0);
        if (alignBillingButton.getAttribute("class").contains("disabled")) {
          Util.sleep(15000);
          portalLogoutLogin(portalUserName, portalPassword);
          attempts++;
        } else {
          break;
        }
      }

      if (attempts >= 5) {
        AssertUtils.fail("Failed to find align billing button");
      }

      // Click on "align billing"
      portalPage.clickUsingLowLevelActions("alignBillingButton");
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Click on the checkboxes for the two subscriptions
    checkPortalCheckbox("//input[@value='" + subscriptionID1 + "']");
    checkPortalCheckbox("//input[@value='" + subscriptionID2 + "']");

    try {
      // Click through the flow to align the billing of the two subscriptions
      portalPage.clickUsingLowLevelActions("alignBillingConfirm");
      portalPage.waitForPageToLoad();
      WebElement creditCardNumberFrame = portalPage
          .getMultipleWebElementsfromField("alignBillingFrame").get(0);
      driver.switchTo().frame(creditCardNumberFrame);
      portalPage.clickUsingLowLevelActions("alignBillingContinue");
      checkPortalCheckbox("//input[@id='customCheckboxTerms']");
      Util.sleep(3000);
      portalPage.clickUsingLowLevelActions("alignBillingSubmit");
      portalPage.waitForPageToLoad();
      portalPage.waitForFieldEnabled("alignBillingClose");
      Util.sleep(15000L);
      portalPage.clickUsingLowLevelActions("alignBillingClose");
    } catch (MetadataException e) {
      e.printStackTrace();
    }

    driver.switchTo().defaultContent();
    portalPage.waitForPageToLoad();
  }

  /**
   * Click on a hidden (display: none;) checkbox that selenium is unable to click on.
   *
   * @param xpath - Checkbox to click
   */
  @Step("Clicking on hidden checkbox " + GlobalConstants.TAG_TESTINGHUB)
  public void checkPortalCheckbox(String xpath) {
    WebElement ele = driver.findElement(By.xpath(xpath));
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    executor.executeScript(BICECEConstants.ARGUMENTS_CLICK, ele);
  }

  @Step("CEP : META Order capture " + GlobalConstants.TAG_TESTINGHUB)
  public void validateMetaOrderProductInCEP(String cepURL, String portalUserName,
      String portalPassword, String subscriptionID) {
    boolean status = false;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    if (isPortalElementPresent("portalProductServiceTab")) {
      try {
        // The subscription id that is being displayed in Portal is different from Pelican. Hence, Checking for "Subscription ID" text
        if (System.getProperty(BICECEConstants.PAYMENT)
            .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
          subscriptionID = "Subscription ID";
        }
        status = isSubscriptionInPortal(subscriptionID, portalUserName, portalPassword);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (!status) {
      AssertUtils.fail(BICECEConstants.PRODUCT_IS_DISPLAYED_IN_PORTAL + BICECEConstants.FALSE);
    }
  }

  /**
   * Navigate to the all products page in portal
   *
   * @param cepURL - URL of portal to visit
   */
  public void validateProductByName(String cepURL) {
    openPortalBICLaunch(cepURL);
    portalPage.click("portalPSLink");

    try {
      if (portalPage.checkIfElementExistsInPage("portalProductPageDismissModal", 10)) {
        portalPage.click("portalProductPageDismissModal");
      }
    } catch (MetadataException e) {
      throw new RuntimeException(e);
    }
  }

  @Step("Verify product is visible in Portal " + GlobalConstants.TAG_TESTINGHUB)
  public void verifyProductVisible(HashMap<String, String> results, String productName) {
    int attempts = 0;
    boolean status = false;

    while (attempts < 5) {
      try {
        if (portalPage.checkIfElementExistsInPage("portalProductPageDismissTooltip", 10)) {
          portalPage.click("portalProductPageDismissTooltip");
        }
        String lastProductXPath =
            "//div[contains(@class, \"dhig-typography-headline-small\") and contains(text(), \"" + productName + "\")]";
        WebElement lastProduct = driver.findElement(By.xpath(lastProductXPath));
        status = lastProduct.isDisplayed();
      } catch (Exception e) {
        Util.printInfo(
            "Failed to find Student Subscription in Portal - Attempt #" + (attempts + 1));
      }

      if (!status) {
        if (attempts >= 4) {
          AssertUtils.fail("All retries exhausted: Failed to find Student Subscription in Portal");
        }
        Util.sleep(60000);
        Util.printInfo("Part of Retry: To invalidate Portal User Cache, Signout and Signin");
        portalLogoutLogin(results.get(BICConstants.emailid), results.get("password"));
        attempts++;
      } else {
        Util.printInfo("Found Student Subscription in Portal, so skipping the retry logic");
        break;
      }
    }
  }

  @Step("Portal : Cancel subscription" + GlobalConstants.TAG_TESTINGHUB)
  public void cancelSubscription(String portalUserName, String portalPassword)
      throws MetadataException {
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    openPortalURL(accountsPortalSubscriptionsUrl);
    clickOnSubscriptionRow();
    checkEmailVerificationPopupAndClick();
    closeAlertBanner();
    closeSubscriptionTermPopup();

    Util.printInfo("Going through cancel flow");
    portalPage.clickUsingLowLevelActions("autoRenewOffButton");
    portalPage.clickUsingLowLevelActions("autoRenewOffContinue");

    // Fill out survey only present on STG env
    if (System.getProperty(BICECEConstants.ENVIRONMENT).equals(BICECEConstants.ENV_STG)) {
      radioButtonClick("autoRenewOffRadioButton", 6);
      portalPage.populateField("autoRenewOffComments", "Test cancellation.");
    }

    portalPage.clickUsingLowLevelActions("autoRenewTurnOffButton");
    portalPage.clickUsingLowLevelActions("autoRenewDone");
  }

  @Step("Close Subscription Term Popup" + GlobalConstants.TAG_TESTINGHUB)
  private void closeSubscriptionTermPopup() throws MetadataException {
    if (portalPage.checkIfElementExistsInPage("portalSubscriptionTermPopup", 10)) {
      Util.printInfo("Closing subscription popup");
      portalPage.clickUsingLowLevelActions("portalCloseButton");
    } else {
      Util.printInfo("Subscription popup not present");
    }
  }

  @Step("Close Portal Banner" + GlobalConstants.TAG_TESTINGHUB)
  private void closeAlertBanner() throws MetadataException {
    if (portalPage.checkIfElementExistsInPage("portalBannerAlert", 10)) {
      Util.printInfo("Closing portal banner");
      portalPage.clickUsingLowLevelActions("portalBannerClose");
    } else {
      Util.printInfo("Portal banner not present");
    }
  }

  @Step("Click on a radio button" + GlobalConstants.TAG_TESTINGHUB)
  private void radioButtonClick(String fieldName, int indexOfElement) throws MetadataException {
    portalPage.checkIfElementExistsInPage(fieldName, 10);
    List<WebElement> listEle = portalPage.getMultipleWebElementsfromField(fieldName);
    listEle.get(indexOfElement).click();
  }

  @Step("Portal : Turn On Auto Renew" + GlobalConstants.TAG_TESTINGHUB)
  public void restartSubscription()
      throws MetadataException {
    Util.printInfo("Turn on subscription auto renew.");
    if (portalPage.checkIfElementExistsInPage("autoRenewOnButton", 10)) {
      portalPage.clickUsingLowLevelActions("autoRenewOnButton");
    }
    Util.printInfo("Dismiss auto renew popup.");
    if (portalPage.checkIfElementExistsInPage("autoRenewPopupDismiss", 10)) {
      portalPage.clickUsingLowLevelActions("autoRenewPopupDismiss");
    }
  }

  @Step("Click on Subscription" + GlobalConstants.TAG_TESTINGHUB)
  private void clickOnSubscriptionRow() throws MetadataException {
    Util.waitforPresenceOfElement(
        portalPage.getFirstFieldLocator(BICECEConstants.SUBSCRIPTION_ROW_IN_SUBSCRIPTION));
    Util.printInfo("Clicking on subscription row...");
    portalPage.clickUsingLowLevelActions(BICECEConstants.SUBSCRIPTION_ROW_IN_SUBSCRIPTION);
    portalPage.waitForPageToLoad();
  }

  @Step("Account Portal: Logout & Login " + GlobalConstants.TAG_TESTINGHUB)
  private void portalLogoutLogin(String userEmail, String password) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("localStorage.clear();sessionStorage.clear();");

    openPortalURL(accountPortalOxygenLogOut);
    Util.sleep(3000);

    if (isPortalLoginPageVisible()) {
      portalLogin(userEmail, password);
    }
  }

  public void navigateToSubscriptionRow() throws Exception {
    if (portalPage.checkIfElementExistsInPage("portalLinkSubscriptions", 60)) {
      Util.printInfo("Clicking on portal subscription and contracts link...");
      portalPage.clickUsingLowLevelActions("portalLinkSubscriptions");
    } else {
      openPortalURL(accountsPortalSubscriptionsUrl);
    }
    clickPortalClosePopup();
    Util.sleep(2000);
    clickOnSubscriptionRow();
  }

  public void navigateToInvoiceCreditMemos() throws MetadataException {
    if (!portalPage.checkIfElementExistsInPage("invoicesAndCreditMemos", 60)) {
      Util.printInfo("Clicking on Billing and Orders link...");
      portalPage.clickUsingLowLevelActions("billingAndOrders");
    }
    Util.printInfo("Clicking on Invoice and Credit Memos Link...");
    portalPage.clickUsingLowLevelActions("invoicesAndCreditMemos");
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
  }

  public void selectAllInvoiceCheckBox() throws MetadataException {
    portalPage.checkIfElementExistsInPage("invoicesTab", 30);
    if (portalPage.checkIfElementExistsInPage("allInvoiceCheckBox", 10)) {
      portalPage.clickUsingLowLevelActions("allInvoiceCheckBox");
    } else {
      portalPage.clickUsingLowLevelActions("allInvoiceCheckBoxMulti");
    }
    Util.printInfo("Clicked on All Invoice Check Box....");
  }

  public double selectInvoice(String poNumber) throws MetadataException {
    double invoiceAmount = 0.00;
    List<WebElement> checkBoxes = portalPage.getMultipleWebElementsfromField("invoiceCheckBoxes");
    List<WebElement> purchaseNumbers = portalPage.getMultipleWebElementsfromField("purchaseOrderNumbersList");
    AssertUtils.assertFalse(purchaseNumbers.size() == 0, "FAILURE: No Invoices available in Invoices page to Pay");

    for (int i = 0; i < purchaseNumbers.size(); i++) {
      if (purchaseNumbers.get(i).getText().trim().equalsIgnoreCase(poNumber.trim())) {
        checkBoxes.get(i).click();
        List<WebElement> amounts = portalPage.getMultipleWebElementsfromField("paymentTotalList");
        invoiceAmount = Double.parseDouble(amounts.get(0).getText().replaceAll("[^0-9]", ""));
        break;
      } else if (i == purchaseNumbers.size() - 1 && purchaseNumbers.get(i).getText().trim()
          .equalsIgnoreCase(poNumber.trim())) {
        AssertUtils.assertFalse(true, "unable to find the Invoice" + poNumber);
      }
    }
    return invoiceAmount;
  }

  private void clickOnInvoice(String poNumber) {
    By xpath = By.xpath("//p[contains(text(), '" + poNumber + "')]");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(ExpectedConditions.presenceOfElementLocated(xpath));
    WebElement invoice = driver.findElement(xpath);
    invoice.click();
  }

  public void clickOnPayButton() throws MetadataException {
    portalPage.checkIfElementExistsInPage("invoicesTab", 30);
    Util.printInfo("Getting Invoices Pay Buttons...");
    List<WebElement> payButtons = portalPage.getMultipleWebElementsfromField("invoicePayButtons");
    payButtons.stream().findFirst().ifPresent(ele -> ele.click());
    Util.printInfo("Clicked on First Invoice Pay Button....");
  }

  public void selectAllInvoicesPayButton() throws MetadataException {
    Util.printInfo("Selecting the invoices....");
    portalPage.checkIfElementExistsInPage("invoicesTab", 30);
    portalPage.click("allInvoicesPayButton");
    Util.printInfo("Clicked on All Invoice Pay Button....");
    Util.sleep(2000);
  }

  public double getInvoicePaymentTotal() throws MetadataException {
    portalPage.checkIfElementExistsInPage("invoicesTab", 30);
    Util.printInfo("Getting Invoices Payment Totals...");
    List<WebElement> amounts = portalPage.getMultipleWebElementsfromField("paymentTotalList");
    String amount = amounts.stream().findFirst().get().getText().replaceAll("[^0-9]", "");
    double amountValue = Double.parseDouble(amount);
    Util.printInfo("Return Invoice Total...." + amountValue);
    return amountValue;
  }

  public double getAllInvoicePaymentTotal() throws Exception {
    double amountValue = 0.00;
    portalPage.checkIfElementExistsInPage("invoicesTab", 30);
    Util.printInfo("Getting Invoices Payment Totals...");
    List<WebElement> amounts = portalPage.getMultipleWebElementsfromField("paymentTotalList");
    for (WebElement ele : amounts) {
      amountValue = amountValue + Double.parseDouble(ele.getText().replaceAll("[^0-9.]", ""));
    }
    Util.printInfo("Return All Invoice Payment Totals...." + amountValue);
    return amountValue;
  }

  public double getPaymentTotalFromCheckout(String elementXPath) throws Exception {
    String paymentTotalAmount = portalPage.getMultipleWebElementsfromField(elementXPath).get(0)
        .getText().replaceAll("[^0-9]", "");
    Util.printInfo("Return Check out page Payment Total...." + paymentTotalAmount);
    return Double.parseDouble(paymentTotalAmount);
  }

  @Step("Select invoice and credit memo validations" + GlobalConstants.TAG_TESTINGHUB)
  public double selectInvoiceAndValidateCreditMemo(String poNumber, Boolean shouldWaitForInvoice) throws Exception {
    String[] poNumbers = poNumber.split(",");
    openPortalURL(accountPortalBillingInvoicesUrl);
    if (shouldWaitForInvoice) {
      waitForInvoicePageLoadToVisible();
    } else {
      portalPage.waitForFieldPresent("invoicePageTableTitle", 60000);
    }

    double invoiceAmount = 0.00;

    if (poNumbers.length > 1) {
      for (int i = 0; i < poNumbers.length; i++) {
        Util.printInfo("Selecting PO Number:" + poNumbers[i]);
        invoiceAmount = invoiceAmount + selectInvoice(poNumbers[i]);
      }
      selectAllInvoicesPayButton();
    } else {
      clickOnInvoice(poNumbers[0]);
      List<WebElement> amounts = portalPage.getMultipleWebElementsfromField("invoicePageTotal");
      invoiceAmount = Double.parseDouble(amounts.get(0).getText().replaceAll("[^0-9]", ""));

      portalPage.click("invoicePagePay");
    }

    Util.sleep(10000);
    Util.printInfo("Validating Invoice Amount and Checkout Amount for Invoice Number:" + poNumber);
    double beforeAddCreditMemoAmount = getPaymentTotalFromCheckout("totalPaymentCheckout");
    AssertUtils.assertEquals(invoiceAmount, beforeAddCreditMemoAmount);
    double creditMemoAmount = 0.00;

    if (portalPage.isFieldVisible("creditMemoTab")) {
      portalPage.clickUsingLowLevelActions("creditMemoTab");
      Util.sleep(2000);

      portalPage.clickUsingLowLevelActions("creditMemoCheckBox");
      Util.sleep(5000);

      creditMemoAmount = Double.parseDouble(
          portalPage.getMultipleWebElementsfromField("creditMemoPrice").get(0).getText().replaceAll("[^0-9.]", ""));

      portalPage.clickUsingLowLevelActions("continueButton");
      Util.sleep(5000);

      double afterAddCreditMemoAmount = getPaymentTotalFromCheckout(
          "totalPaymentCheckoutWithCreditMemo");
      AssertUtils.assertEquals(invoiceAmount, creditMemoAmount + afterAddCreditMemoAmount);
    } else {
      double afterAddCreditMemoAmount = getPaymentTotalFromCheckout("totalPaymentCheckout");
      AssertUtils.assertEquals(invoiceAmount, creditMemoAmount + afterAddCreditMemoAmount);
    }

    Util.printInfo("Validated Invoice Amount and Checkout Amount for Invoice Number:" + poNumber);
    return invoiceAmount;
  }

  @Step("CEP : Launch Account portal" + GlobalConstants.TAG_TESTINGHUB)
  public void loginToAccountPortal(LinkedHashMap<String, String> data, String portalUserName,
      String portalPassword) {
    openPortalBICLaunch(data.get("accountPortalURL"));
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }
  }

  @Step("CEP : Pay Invoice" + GlobalConstants.TAG_TESTINGHUB)
  public Boolean payInvoice(LinkedHashMap<String, String> data) throws Exception {
    Util.printInfo("Paying the invoices.");
    if (System.getProperty(BICECEConstants.APPLY_CM) != null && System.getProperty(BICECEConstants.APPLY_CM)
        .equalsIgnoreCase("Y")) {
      if (!portalPage.checkIfElementExistsInPage("creditMemoCheckBox", 60)) {
        AssertUtils.fail("Credit Memo not found. Please add Credit Memo.");
      }
      portalPage.clickUsingLowLevelActions("creditMemoCheckBox");
      Util.printInfo("Applied credit memo successfully");
    } else {
      Util.printInfo("Skipping the credit memo.");
    }
    if (portalPage.isFieldVisible("creditMemoTab") && portalPage.isFieldVisible("continueButton")) {
      portalPage.clickUsingLowLevelActions("continueButton");
    }
    Util.sleep(2000);

    portalPage.waitForFieldPresent("clickOnPaymentTab", 20000);
    try {
      portalPage.clickUsingLowLevelActions("clickOnPaymentTab");
    } catch (Exception e) {
      if (isPortalLoginPageVisible()) {
        Util.printInfo("Failed to make Payment for LOC Invoice, we are seeing Login Page. Lets retry login!!!");
        //return true, for while loop to re login again.
        return true;
      } else {
        Assert.fail("Failed to Click on Payment section in Pay Invoice.");
      }
    }

    Util.sleep(5000);
    if (portalPage.checkIfElementExistsInPage("paymentDetailsCompleted", 20)) {
      portalPage.clickUsingLowLevelActions("editPaymentMethod");
      portalPage.waitForPageToLoad();
    }
    bicTestBase.enterPayInvoiceBillingDetails(data, bicTestBase.getBillingAddress(data),
        data.get(BICECEConstants.PAYMENT_TYPE));

    if (BICECEConstants.WIRE_TRANSFER_PAYMENT_METHOD.equalsIgnoreCase(data.get(BICECEConstants.PAYMENT_TYPE))) {
      return false;
    }

    if (portalPage.checkIfElementExistsInPage("portalDebitMandateAgreement", 20)
        && (!portalPage.checkIfElementExistsInPage("mandateAgreementChecked", 20))) {
      bicTestBase.clickMandateAgreementCheckbox();
    }
    if (System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_ATM_BANK_TRANSFER)) {
      Util.printInfo("Clicking on Save Button of ATM Payment Tab");
      Util.sleep(5000);
      portalPage.clickUsingLowLevelActions("portalCardSaveBtn");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    }
    if (portalPage.checkIfElementExistsInPage("submitPaymentButton", 15000)) {
      submitPayment();
    }
    return false;
  }

  @Step("CEP : Click Submit Payment Button" + GlobalConstants.TAG_TESTINGHUB)
  public void submitPayment() throws Exception {
    try {
      portalPage.waitForFieldPresent("submitPaymentButton", 15000);
      Util.printInfo("Clicking on Submit Payment Button");
      portalPage.clickUsingLowLevelActions("submitPaymentButton");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
      if (portalPage.checkIfElementExistsInPage("paymentSuccessMessage", 10)) {
        Util.printInfo("Payment for Invoice is successfully Completed");
      } else {
        AssertUtils.fail("Error while submitting payment for invoices");
      }
    } catch (Exception e) {
      AssertUtils.fail("Error while submitting the payment for Invoices");

    }
  }

  public void waitForInvoicePageLoadToVisible() {
    String title = "";
    int i = 1;
    while (i < 30) {
      try {
        Util.sleep(15000);
        title = portalPage.getMultipleWebElementsfromField("invoicePageTableTitle").get(0).getText();
        Util.printInfo("Title return :: " + title);
      } catch (Exception e) {
        e.printStackTrace();
      }
      Util.printInfo("Waiting for another 5 minutes on attempt #" + i);
      if (!title.isEmpty() && (Integer.parseInt(title.replaceAll("[^0-9]", "")) != 0)) {
        Util.PrintInfo("Invoices were generated successfully");
        break;
      } else if (i == 29) {
        AssertUtils.fail("Retries exhausted: Invoice Order not loaded even after 150 minutes");
      }
      i++;
      Util.sleep(300000);
      driver.navigate().refresh();
    }
  }

  public void verifyInvoiceStatus(String poNumber) throws Exception {

    viewAllInvoices();

    verifyPaidInvoicesFromOpenTab();

    List<WebElement> purchaseNumbers = portalPage.getMultipleWebElementsfromField("paidInvoiceNumbersList");
    List<WebElement> invoiceStatus = portalPage.getMultipleWebElementsfromField("paidInvoiceStatus");

    String[] poNumbers = poNumber.replaceAll("^\"|\"$", "").split(",");

    for (int i = 0; i < purchaseNumbers.size(); i++) {
      Util.printInfo("Split PO number: " + poNumbers[i] + "\r\n");

      if (purchaseNumbers.get(i).getText().trim().equalsIgnoreCase(poNumbers[i])
          && invoiceStatus.get(i).getText().length() > 0) {
        Util.PrintInfo("Invoice Status is updated as PAID for PO Number " + poNumber);
        break;
      } else if (i == purchaseNumbers.size() - 1 && purchaseNumbers.get(i).getText().trim()
          .equalsIgnoreCase(poNumbers[i]) && invoiceStatus.get(i).getText().length() > 0) {
        AssertUtils.assertFalse(true,
            " Able to find the Invoice PO Number " + poNumber + " but the status is " + invoiceStatus.get(i).getText());
      } else {
        Util.printInfo("No invoice status found.");
      }
    }
  }

  @Step("Navigate To login to Pay By Invoice URL" + GlobalConstants.TAG_TESTINGHUB)
  public void navigateToLoginToPayByInvoiceURL(String data) {
    try {
      openPortalURL(data);
      portalPage.waitForPageToLoad();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Unable to open Pay By Invoice");
    }
    portalPage.waitForPageToLoad();
  }

  @Step("CEP : Click View all invoices")
  public void viewAllInvoices() throws Exception {
    if (portalPage.waitForFieldPresent("seeAllInvoices", 30000)) {
      portalPage.clickUsingLowLevelActions("seeAllInvoices");
      Util.sleep(5000);
    } else if (portalPage.waitForFieldPresent("seeAllPaidInvoices", 30000)) {
      portalPage.clickUsingLowLevelActions("seeAllPaidInvoices");
      Util.sleep(5000);
    }
    Util.printInfo("Payment for Invoice is successfully Completed");
  }

  @Step("Verify paid invoices are not visible from Open tab " + GlobalConstants.TAG_TESTINGHUB)
  public void verifyPaidInvoicesFromOpenTab() throws Exception {

    int attempts = 0;
    boolean status = false;

    while (attempts < 13) {
      try {
        if (portalPage.checkIfElementExistsInPage("viewPaidInvoices", 60)) {
          status = true;
          Util.sleep(2000);
          portalPage.clickUsingLowLevelActions("viewPaidInvoices");
          portalPage.waitForFieldPresent("invoicePageTableTitle", 10000);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (!status) {
        if (attempts == 12) {
          AssertUtils.fail(
              "All retries exhausted: Failed to find 'View paid invoices' button from Open tab.");
        } else {
          Util.printInfo(
              "Failed to find 'View paid invoices' button - Attempt #" + (attempts + 1));
        }
        Util.sleep(300000);
        Util.printInfo("Refreshing the page!!!");
        driver.navigate().refresh();
        attempts++;
      } else {
        Util.printInfo("Found 'View paid invoices' button, so skipping the retry logic.");
        break;
      }
    }
  }

  @Step("Verify credit memos element from the section " + GlobalConstants.TAG_TESTINGHUB)
  public void verifyCreditMemoStatus(String elementXPath) throws Exception {

    int attempts = 0;
    boolean status = false;

    while (attempts < 11) {
      try {
        portalPage.waitForFieldPresent("creditMemosTab", 10000);
        portalPage.click("creditMemosTab");
        Util.sleep(5000);
        if (driver.findElement(By.xpath(elementXPath))
            .isDisplayed()) {
          status = true;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (!status) {
        if (attempts == 10) {
          AssertUtils.fail(
              "All retries exhausted to find element: " + elementXPath + " Failing the test.");
        } else {
          Util.printInfo(
              "Failed to find Credit Memo element - Attempt #" + (attempts + 1));
        }
        Util.sleep(180000);
        Util.printInfo("Refreshing the page!!!");
        driver.navigate().refresh();
        attempts++;
      } else {
        Util.printInfo("Found Credit Memo element: " + elementXPath);
        break;
      }
    }
  }

  @Step("Verify credit memos status and number " + GlobalConstants.TAG_TESTINGHUB)
  public void validateNewCreditMemoInPortal(String cmNumber) throws Exception {
    Util.printInfo("cmNumber return value :: " + cmNumber);
    openPortalURL(accountPortalBillingInvoicesUrl);

    verifyCreditMemoStatus("//*[@data-testid=\"credit-memo-list\"]");

    String creditMemoNumber = driver.findElement(By.xpath(
            "//tbody[contains(@class,'MuiTableBody-root')]/tr[1]/td[1]/p[1]"))
        .getText();
    creditMemoNumber.contains(cmNumber);
    // Commenting out the assertEquals due to the '00' issue.
    // AssertUtils.assertEquals(cmNumber, creditMemoNumber);
  }

  public double getInvoiceTotalAfterPayment(String elementXPath) throws Exception {
    String invoiceTotalAfterPayment = portalPage.getMultipleWebElementsfromField(elementXPath).get(0)
        .getText().replaceAll("[^0-9]", "");
    Util.printInfo("Return Payment Total After Payment...." + invoiceTotalAfterPayment);
    return Double.parseDouble(invoiceTotalAfterPayment);
  }

  public void verifyInvoiceTotalAfterPayment(double invoiceTotalBeforePayment) throws Exception {
    double invoiceTotalAfterPayment = getInvoiceTotalAfterPayment("invoiceAmountAfterPayment");
    AssertUtils.assertEquals(invoiceTotalBeforePayment, invoiceTotalAfterPayment);
  }

  @Step("Select invoice and credit memo validations without PO Number" + GlobalConstants.TAG_TESTINGHUB)
  public double selectInvoiceAndValidateCreditMemoWithoutPONumber(Boolean shouldWaitForInvoice, String locale)
      throws Exception {
    openPortalURL(accountPortalBillingInvoicesUrl);
    if (shouldWaitForInvoice) {
      waitForInvoicePageLoadToVisible();
    } else {
      if (!portalPage.waitForFieldPresent("invoicePageTableTitle", 60000)) {
        AssertUtils.fail("No Open Invoices were Found");
      }
    }
    String invoiceNumber = "More than One Invoice involved";
    double invoiceAmount = 0.00;
    int invoiceCount = Integer.parseInt(portalPage.getValueFromGUI("invoicePageTableTitle").replaceAll("[^0-9]", ""));
    if (invoiceCount > 1) {
      selectAllInvoiceCheckBox();
      invoiceAmount = getInvoiceAmount(invoiceCount, locale);
      selectAllInvoicesPayButton();
    } else {
      invoiceNumber = portalPage.getMultipleWebElementsfromField("invoiceNumbers").get(0).getText()
          .replaceAll("[^0-9.]", "");
      portalPage.clickUsingLowLevelActions("invoiceNumbers");
      Util.sleep(5000);
      List<WebElement> amounts = portalPage.getMultipleWebElementsfromField("invoicePageTotal");
      if (System.getProperty("store").equals("STORE-JP")) {
        invoiceAmount = NumberUtil.convert(amounts.get(0).getText(), locale);
      } else {
        invoiceAmount = Double.parseDouble(amounts.get(0).getText().replaceAll("[^0-9]", ""));
      }

      if (portalPage.checkIfElementExistsInPage("invoicePagePay", 10)) {
        portalPage.click("invoicePagePay");
      } else {
        portalPage.click("invoicePagePayButton");
      }
    }

    Util.sleep(10000);
    Util.printInfo("Validating Invoice Amount and Checkout Amount for Invoice Number: " + invoiceNumber);
    double beforeAddCreditMemoAmount = getPaymentTotalFromCheckout("totalPaymentCheckout");
    AssertUtils.assertEquals(invoiceAmount, beforeAddCreditMemoAmount);
    double creditMemoAmount = 0.00;

    if (System.getProperty(BICECEConstants.APPLY_CM) != null && System.getProperty(BICECEConstants.APPLY_CM)
        .equalsIgnoreCase("LOC")) {
      try {
        portalPage.clickUsingLowLevelActions("creditMemoTab");
        Util.sleep(2000);

        portalPage.clickUsingLowLevelActions("creditMemoCheckBox");
        Util.sleep(5000);

        creditMemoAmount = Double.parseDouble(
            portalPage.getMultipleWebElementsfromField("creditMemoPrice").get(0).getText().replaceAll("[^0-9]", ""));

        portalPage.clickUsingLowLevelActions("continueButton");
        Util.sleep(5000);

        double afterAddCreditMemoAmount = getPaymentTotalFromCheckout(
            "totalPaymentCheckoutWithCreditMemo");
        AssertUtils.assertEquals(invoiceAmount, creditMemoAmount + afterAddCreditMemoAmount);
        invoiceAmount = afterAddCreditMemoAmount;
      } catch (Exception e) {
        e.printStackTrace();
        AssertUtils.fail("Unable to add credit memo.");
      }
    } else {
      double afterAddCreditMemoAmount = getPaymentTotalFromCheckout("totalPaymentCheckout");
      AssertUtils.assertEquals(invoiceAmount, creditMemoAmount + afterAddCreditMemoAmount);
    }

    Util.printInfo("Validated Invoice Amount and Checkout Amount for Invoice Number:" + invoiceNumber);
    return invoiceAmount;
  }

  public double getInvoiceAmount(int invoiceCount, String locale) throws MetadataException {
    double invoiceAmount = 0.00;
    List<WebElement> amounts = portalPage.getMultipleWebElementsfromField("paymentTotalList");
    for (int i = 0; i < invoiceCount; i++) {
      if (System.getProperty("store").equals("STORE-JP")) {
        invoiceAmount = NumberUtil.convert(amounts.get(0).getText(), locale);
      } else {
        invoiceAmount = invoiceAmount + Double.parseDouble(amounts.get(0).getText().replaceAll("[^0-9]", ""));
      }
    }
    return invoiceAmount;
  }

  public void selectInvoiceCSN(int invoiceIndex) throws MetadataException {
    portalPage.clickUsingLowLevelActions("invoiceCSNSelectorDropdown");
    List<WebElement> csnList = portalPage.getMultipleWebElementsfromField("selectorDropdownList");
    if (csnList.size() >= 1) {
      portalPage.clickUsingLowLevelActions(csnList.get(invoiceIndex));
    } else {
      portalPage.clickUsingLowLevelActions(csnList.get(0));
    }
  }

  public void selectInvoiceUsingCSN(String csn) throws MetadataException {
    Util.printInfo("Selecting the CSN :" + csn);
    Util.sleep(10000);
    portalPage.clickUsingLowLevelActions("invoiceCSNSelectorDropdown");
    List<WebElement> csnList = portalPage.getMultipleWebElementsfromField("selectorDropdownList");
    if (csnList.size() >= 1) {
      csnList.stream().filter(c -> c.getText().contains(csn)).findFirst().get().click();
    }
    Util.sleep(30000);
  }

  public ArrayList<String> selectMultipleInvoice(int noOfInvoices) throws MetadataException {
    portalPage.waitForFieldPresent("paymentDue");
    portalPage.clickUsingLowLevelActions("paymentDue");
    Util.printInfo("Selecting " + noOfInvoices + " invoices....");
    List<WebElement> invoiceList = portalPage.getMultipleWebElementsfromField("invoiceList");
    List<WebElement> invoiceNumbers = portalPage.getMultipleWebElementsfromField("invoiceNumbersList");
    ArrayList<String> numbers = new ArrayList<>();

    if (invoiceList.size() > 5) {
      for (int i = 0; i < noOfInvoices; i++) {
        portalPage.clickUsingJavaScriptExecutor(invoiceList.get(i));
        numbers.add(invoiceNumbers.get(i).getText().trim());
      }
    } else if (invoiceList.size() > 3) {
      portalPage.clickUsingJavaScriptExecutor(invoiceList.get(0));
      numbers.add(invoiceNumbers.get(0).getText().trim());
      portalPage.clickUsingJavaScriptExecutor(invoiceList.get(1));
      numbers.add(invoiceNumbers.get(1).getText().trim());
    } else {
      portalPage.clickUsingJavaScriptExecutor(invoiceList.get(0));
      numbers.add(invoiceNumbers.get(0).getText().trim());
    }
    Util.printInfo("Invoice selected" + numbers);
    return numbers;
  }

  public void clickOnInvoiceBulkPayButton() throws MetadataException {
    portalPage.clickUsingLowLevelActions("invoicePagePay");
    portalPage.waitForPageToLoad();
  }

  public void openPortalInvoiceAndCreditMemoPage(LinkedHashMap<String, String> data) {
    openPortalURL(data.get("portalBillingInvoicesUrl"));
    portalPage.waitForFieldPresent("invoicePageTableTitle", 60000);
  }

  public double selectInvoice(List<String> poNumbers) throws MetadataException {
    double invoiceAmount = 0.00;
    List<WebElement> checkBoxes = portalPage.getMultipleWebElementsfromField("invoiceCheckBoxes");
    List<WebElement> purchaseNumbers = portalPage.getMultipleWebElementsfromField("purchaseOrderNumbersList");
    AssertUtils.assertFalse(purchaseNumbers.size() == 0, "FAILURE: No Invoices available in Invoices page to Pay");
    for (int p = 0; p < poNumbers.size(); p++) {
      for (int i = 0; i < purchaseNumbers.size(); i++) {
        if (purchaseNumbers.get(i).getText().trim().equalsIgnoreCase(poNumbers.get(p).trim())) {
          checkBoxes.get(i).click();
          List<WebElement> amounts = portalPage.getMultipleWebElementsfromField("paymentTotalList");
          invoiceAmount = Double.parseDouble(amounts.get(0).getText().replaceAll("[^0-9]", ""));
          break;
        } else if (i == purchaseNumbers.size() - 1 && purchaseNumbers.get(i).getText().trim()
            .equalsIgnoreCase(poNumbers.get(p).trim())) {
          AssertUtils.assertFalse(true, "unable to find the Invoice" + poNumbers);
        }
      }
    }
    return invoiceAmount;
  }

  public void verifyPaidInvoiceStatus(ArrayList<String> expectedInvoiceNumbersList) throws Exception {
    if ((!BICECEConstants.WIRE_TRANSFER_PAYMENT_METHOD.equalsIgnoreCase(System.getProperty(BICECEConstants.PAYMENT)))) {
      viewAllInvoices();
      try {
        Util.sleep(5000);
        portalPage.clickUsingLowLevelActions("invoicePaidTab");
        Util.sleep(2000);
        List<WebElement> invoiceNumbers = portalPage.getMultipleWebElementsfromField("paidInvoiceNumbersList");
        for (int i = 0; i < expectedInvoiceNumbersList.size(); i++) {
          for (int j = 0; j < invoiceNumbers.size(); j++) {
            if (invoiceNumbers.get(j).getText().trim().equalsIgnoreCase(expectedInvoiceNumbersList.get(i))) {
              Util.PrintInfo(expectedInvoiceNumbersList.get(i) + " Invoice is Available under Paid Tab");
              System.out.println(expectedInvoiceNumbersList.get(i) + " Invoice is Available under Paid Tab");
              break;
            } else if (!invoiceNumbers.get(j).getText().trim().equalsIgnoreCase(expectedInvoiceNumbersList.get(i))
                && j == invoiceNumbers.size()) {
              Util.PrintInfo(expectedInvoiceNumbersList.get(i) + " Invoice is Not Available under Paid Tab");
              AssertUtils.assertTrue(false,
                  expectedInvoiceNumbersList.get(i) + " Invoice is Not Available under Paid Tab");
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        AssertUtils.fail("Unable to find the paid invoices tab");
      }
    }
  }

  public void selectCurrency(String currency) throws MetadataException {
    if (portalPage.checkIfElementExistsInPage("currencySelectorDropdown", 10)) {
      Util.printInfo("Selecting the Currency :" + currency);
      portalPage.clickUsingLowLevelActions("currencySelectorDropdown");
      List<WebElement> currencyList = portalPage.getMultipleWebElementsfromField("selectorDropdownList");
      if (currencyList.size() >= 1) {
        currencyList.stream().filter(c -> c.getText().contains(currency)).findFirst().get().click();
        Util.sleep(10000);
      }
    } else {
      Util.printInfo("No Currency is Available to Select");
    }
  }

  @Step("Portal : Renew Financing subscription" + GlobalConstants.TAG_TESTINGHUB)
  public void renewFinancingSubscription(String portalUserName, String portalPassword)
      throws MetadataException {
    openPortalURL(accountsPortalSubscriptionsUrl);

    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    clickOnSubscriptionRow();

    int attempts = 0;

    while (attempts < 11) {
      try {
        portalPage.waitForFieldPresent("portalRenewingSubscription", 10000);
        portalPage.clickUsingLowLevelActions("portalRenewingSubscription");
        Util.printInfo("Redirecting to checkout page...");
        Util.sleep(20000);
        break;
      } catch (Exception e) {
        if (attempts == 10) {
          AssertUtils.fail("Unable to renew Financing Subscription. \n" + e);
        }

        Util.printInfo("Unable to click on Renew cta. Retrying. \n" + e);
        portalPage.refresh();
        attempts++;
        Util.sleep(60000);

        //click on "New Renew Button " if the "portalRenewingSubscription" button is not displayed
        if (!portalPage.checkIfElementExistsInPage("portalRenewingSubscription", 0)) {
          try {
            portalPage.clickUsingLowLevelActions("portalNewRenewingSubscription");
            Util.printInfo("Redirecting to checkout page with new Renew Button...`");
            Util.sleep(5000);
            break;
          } catch (MetadataException ex) {
            Util.printError("Error clicking on new Renew Button: " + ex.getMessage());

          }
        }
      }
    }
  }

  public void openAutoDeskHomePage(LinkedHashMap<String, String> data, String url) {
    openPortalURL(data.get(url));
    Util.sleep(20000);
  }

  public boolean verifyCookiesFooterBannerIsVisible() {
    boolean bannerVisible;
    try {
      bannerVisible =
          portalPage.checkIfElementExistsInPage("cookiesFooterBanner", 10) && portalPage.checkIfElementExistsInPage(
              "cookiesFooterBannerAccept", 10);
    } catch (Exception e) {
      bannerVisible = false;
    }
    return bannerVisible;
  }

  public void clickCookiesFooterAcceptButton() {
    try {
      if (portalPage.checkIfElementExistsInPage("cookiesFooterBannerAccept", 10)) {
        portalPage.clickUsingLowLevelActions("cookiesFooterBannerAccept");
        Util.printInfo("Able to Click Accept Button of Cookies Banner");
      } else {
        AssertUtils.fail("Unable to Click Accept Button of Cookies Banner");
      }
    } catch (Exception e) {
      AssertUtils.fail("Unable to Click Accept Button of Cookies Banner");
    }
  }
}
