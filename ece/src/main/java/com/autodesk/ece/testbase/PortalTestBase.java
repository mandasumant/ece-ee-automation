package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.CustomSoftAssert;
import com.autodesk.testinghub.core.utils.ErrorEnum;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

public class PortalTestBase {

  public static Page_ portalPage = null;
  public static Page_ studentPage = null;
  public WebDriver driver = null;
  ZipPayTestBase zipTestBase;
  BICTestBase bicTestBase;

  public PortalTestBase(GlobalTestBase testbase) {
    driver = testbase.getdriver();
    portalPage = testbase.createPage("PAGE_PORTAL");
    studentPage = testbase.createCommonPage("PAGE_STUDENT");
    new BICTestBase(driver, testbase);
    zipTestBase = new ZipPayTestBase(testbase);
    bicTestBase = new BICTestBase(driver, testbase);
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

  public String getOxygenId(HashMap<String, String> data) {
    String userSessionID = null;
    String emailID = data.get(TestingHubConstants.emailid);
    OxygenService os = new OxygenService();

    // 1st method from API
    try {
      userSessionID = os.getOxygenID(data.get(TestingHubConstants.emailid), data.get("password"));
    } catch (Exception e1) {
    }

    // 2nd method from Portal
    if (userSessionID == null || userSessionID == "") {
      openPortalBICLaunch(data.get(TestingHubConstants.cepURL));
      JavascriptExecutor js;

      js = (JavascriptExecutor) driver;
      clickALLPSLink();
      try {
        String userCurrentData = (String) js.executeScript(
            String.format("return window.sessionStorage.getItem('%s');", "userData"));
        String[] temp = userCurrentData.split("\"oxygenId\":\"");
        if (temp[1] != null) {
          String[] sessionID = temp[1].split("\",\"");
          userSessionID = sessionID[0];
        }
      } catch (Exception e) {
        //AssertUtils.fail(ErrorEnum.PORTAL_SERVICE_DOWN.geterr());
        Util.printError(ErrorEnum.PORTAL_SERVICE_DOWN.geterr());
      }
    }

    // 3rd method from featureflag WebSite
    if (userSessionID == null || userSessionID == "") {
      try {
        openPortalURL("http://featureflag.ecs.ads.autodesk.com");
        driver.findElement(By.xpath("//textarea[@id='id-email']")).sendKeys(emailID);
        Util.sleep(2000);

        driver.findElement(By.xpath("//button[@id='id-submit']")).click();
        Util.sleep(5000);
        userSessionID = driver.findElement(By.xpath("//textarea[@id='id-userId']")).getText();
      } catch (Exception e) {
        //e.printStackTrace();
        Util.printError(e.getMessage());
      } finally {
        clickALLPSLink();
      }
    }
    return userSessionID;
  }

  //@Step("launch URL in browser"+GlobalConstants.TAG_TESTINGHUB)
  public boolean openPortalURL(String data) {
    try {
      driver.manage().window().maximize();
      Util.sleep(2000);
      driver.get(data);
      driver.navigate().refresh();
      Util.printInfo("Opened:" + data);
    } catch (Exception e) {
      Util.printTestFailedMessage("Unable to launch portal page: " + data);
      driver.get(data);
      e.printStackTrace();
    }
    return true;
  }

  //@Step("launch URL in browser")
  public boolean openPortalBICLaunch(String data) {
    Util.printInfo("launch URL in browser");
    driver.manage().deleteAllCookies();
    driver.navigate().to(data);
    Util.printInfo("Opened:" + data);
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
  public boolean portalLogin(String portalUserName, String portalPassword) {
    boolean status = false;
    Integer attempts = 0;

    while(attempts < 3) {
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

    return status;
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

  @Step("check if all the tabs are loading in Portal ")
  public boolean isPortalTabsVisible() {
    boolean status = false;
    status = isPortalElementPresent("portalProductServiceTab");
    return status;
  }

  @Step("Portal : Validate subscription" + GlobalConstants.TAG_TESTINGHUB)
  public boolean isSubscriptionDisplayedInPS(String subscriptionID) {
    boolean status = false;
    String productXpath = null;
    int attempts = 0;

    while (attempts < 3) {
      try {
        productXpath = portalPage
            .getFirstFieldLocator("subscriptionIDInPS").replace("TOKEN1", subscriptionID);

        Util.printInfo("Found the Subscription productXpath, so skipping the retry logic");
        break;
      } catch (Exception e) {
        if (attempts >= 2) {
          AssertUtils.fail(
              "All retries exhausted: Verify subscription/agreement is displayed in All P&S page step couldn't "
                  + "be completed due to technical issue " + e.getMessage());
        }
        driver.navigate().refresh();
        Util.sleep(10000);
        Util.printInfo(
            "Retry Logic: Failed to find the Subscription productXpath, attempt #" + (attempts
                + 1));
        attempts++;
      }
    }

    Util.printInfo("Check if element with subscriptionId exists on the page.");

    status = isSubscriptionDisplayed(productXpath);

    if (!status) {
      AssertUtils.fail(
          ErrorEnum.AGREEMENT_NOTFOUND_CEP.geterr() + " subscriptionID ::  " + subscriptionID
              + " , In P&S page");
    }

    return status;
  }

  @Step(
      "Verify if Subscription exists in Product and Services Page" + GlobalConstants.TAG_TESTINGHUB)
  public boolean isSubscriptionDisplayed(String productXpath) {
    boolean status = false;
    Integer attempts = 0;

    while (attempts < 3) {
      WebElement element = null;
      try {
        element = driver.findElement(By.xpath(productXpath));
      } catch (ElementNotVisibleException e) {
      }

      if (element == null) {
        if (attempts >= 2) {
          AssertUtils.fail("Failed to find the Subscription Web Element in portal");
        }
        driver.navigate().refresh();
        Util.sleep(10000);
        Util.printInfo(
            "Retry Logic: Failed to find the Subscription Web Element, attempts #" + (attempts
                + 1));
        attempts++;
      } else {
        status = true;
        Util.printInfo("Found the Subscription Web Element, so skipping the retry logic");
        break;
      }
    }

    return status;
  }

  @Step("Verify subscription/agreement is displayed on Subscriptions page"
      + GlobalConstants.TAG_TESTINGHUB)
  public boolean isSubscriptionDisplayedInBO(String subscriptionID) {
    openSubscriptionsLink();
    boolean status = false;
    String errorMsg = "";
    String productXpath = null;

    int attempts = 0;

    while (attempts < 3) {
      try {
        productXpath = portalPage.getFirstFieldLocator("subscriptionIDInBO")
            .replace("TOKEN1", subscriptionID);

      } catch (Exception e) {
        driver.navigate().refresh();
        Util.sleep(10000);
        Util.printInfo(
            "Retry Logic: Failed to find the Subscription productXpath, attempt #" + (attempts
                + 1));
        attempts++;
      }
      Util.printInfo("Found the Subscription productXpath, so skipping the retry logic");
      break;
    }

    status = isSubscriptionDisplayed(productXpath);

    if (!status) {
      errorMsg = ErrorEnum.AGREEMENT_NOTFOUND_CEP.geterr() + " subscriptionID ::  " + subscriptionID
          + " , In B&O page";
    }

    status = errorMsg.isEmpty();

    return status;
  }

  @Step("Open Subscriptions and Contracts link in Portal")
  public void openSubscriptionsLink() {
    openPortalURL("https://stg-manage.autodesk.com/billing/subscriptions-contracts");
  }

  @Step("Click on All Products & Services Link")
  public boolean clickALLPSLink() {
    boolean status = false;
    try {
      // portalPage.click("portalAllPSLink");
      // portalPage.clickUsingLowLevelActions("portalAllPSLink");
      if (GlobalConstants.getENV().equalsIgnoreCase("stg")) {
        openPortalURL("https://stg-manage.autodesk.com/cep/#products-services/all");
      } else if (GlobalConstants.getENV().equalsIgnoreCase("int")) {
        openPortalURL("https://int-manage.autodesk.com/cep/#products-services/all");
      }
      // portalPage.waitForPageToLoad();
      Util.sleep(5000);
      checkEmailVerificationPopupAndClick();
      status = true;
      // driver.findElement(By.xpath("//a[contains(text(),'All Products & Services')]")).click();
    } catch (Exception e) {
      e.printStackTrace();
      CustomSoftAssert.s_assert.fail("Unable to click on portalAllPSLink ");
    }

    Util.sleep(10000);
    return status;
  }

  /**
   * Navigate to the "Upcoming Payments" section of portal
   */
  @Step("Click on Upcoming Payments Link")
  public void navigateToUpcomingPaymentsLink() {
    try {
      if (GlobalConstants.getENV().equalsIgnoreCase("stg")) {
        openPortalURL("https://stg-manage.autodesk.com/cep/#orders/invoices");
      } else if (GlobalConstants.getENV().equalsIgnoreCase("int")) {
        openPortalURL("https://int-manage.autodesk.com/cep/#orders/invoices");
      }
      portalPage.waitForPageToLoad();
      checkEmailVerificationPopupAndClick();
    } catch (Exception e) {
      e.printStackTrace();
      CustomSoftAssert.s_assert.fail("Unable to open upcoming payments section");
    }
    portalPage.waitForPageToLoad();
  }

  public void checkEmailVerificationPopupAndClick() {
    Util.printInfo("Checking email popup...");
    Util.sleep(5000);
    try {
      if (portalPage.checkIfElementExistsInPage("portalEmailPopupYesButton", 10) == true) {
        Util.sleep(15000);
        Util.printInfo("HTML code - Before Clicking portalEmailPopupYesButton");
        debugPageUrl("Clicking on portal email popup");
        Util.printInfo("Clicking on portal email popup got it button...");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(BICECEConstants.ARGUMENTS_CLICK,
            portalPage.getMultipleWebElementsfromField("portalEmailPopupYesButton").get(0));
        Util.printInfo("HTML code - After Clicking portalEmailPopupYesButton");
        Util.sleep(15000);
        debugPageUrl("After Clicking portalEmailPopupYesButton");

      }
    } catch (Exception e) {
      Util.printInfo("Email popup does not appeared on screen...");
    }
  }

  @Step("CEP : Bic Order capture " + GlobalConstants.TAG_TESTINGHUB)
  public boolean validateBICOrderProductInCEP(String cepURL, String portalUserName,
      String portalPassword, String subscriptionID) {
    boolean status = false, statusPS = false;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    if (isPortalTabsVisible()) {
      try {
        clickALLPSLink();
        statusPS = isSubscriptionDisplayedInPS(subscriptionID);

        status = (statusPS);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (!status) {
      AssertUtils.fail(BICECEConstants.PRODUCT_IS_DISPLAYED_IN_PORTAL + BICECEConstants.FALSE);
    }

    return status;
  }

  @Step("CEP : Bic Order - Switching Term in Portal  " + GlobalConstants.TAG_TESTINGHUB)
  public boolean switchTermInUserPortal(String cepURL, String portalUserName,
      String portalPassword, String subscriptionID) {
    boolean status = false, statusPS = false;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    if (isPortalTabsVisible()) {
      try {
        clickALLPSLink();
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
        portalPage.waitForPageToLoad();
        WebElement primaryEntitlements = driver
            .findElement(By.xpath("//*[@id='primary-entitlements']/div[2]/div"));
        primaryEntitlements.click();

        clickWithJavaScriptExecutor(javascriptExecutor, "//a[@data-action='ManageRenewal']");

        try {
          WebElement editSwitchTermButton = driver
              .findElement(By.xpath("//*[@id=\"renew-details-edit-switch-term\"]/button"));
          editSwitchTermButton.click();
        } catch (org.openqa.selenium.StaleElementReferenceException ex) {
          WebElement editSwitchTermButton = driver
              .findElement(By.xpath("//*[@id=\"renew-details-edit-switch-term\"]/button"));
          editSwitchTermButton.click();
        }

        if (System.getProperty(BICECEConstants.PAYMENT)
            .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
          clickWithJavaScriptExecutor(javascriptExecutor, "//div[@data-testid=\"term-3-year\"]");
        } else {
          clickWithJavaScriptExecutor(javascriptExecutor, "//div[@data-testid=\"term-1-year\"]");
        }
        clickWithJavaScriptExecutor(javascriptExecutor, "//button[@data-wat-val=\"continue\"]");

        Util.sleep(5000);
        AssertUtils.assertTrue(driver
            .findElement(By.xpath("//*[contains(text(),\"Your term change is confirmed\")]"))
            .isDisplayed());

        Util.sleep(5000);
        portalPage.clickUsingLowLevelActions("switchTermDone");

        if (System.getProperty(BICECEConstants.PAYMENT)
            .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
          AssertUtils.assertTrue(driver
              .findElement(By.xpath("//*[starts-with(text(),\"Changes to 3-year term starting\")]"))
              .isDisplayed());
        } else {
          AssertUtils.assertTrue(driver
              .findElement(By.xpath("//*[starts-with(text(),\"Changes to 1-year term starting\")]"))
              .isDisplayed());
        }
        clickWithJavaScriptExecutor(javascriptExecutor, "//*[@data-wat-val=\"me-menu:sign out\"]");

        //close portal window
        driver.close();

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (statusPS) {
      AssertUtils.fail(BICECEConstants.PRODUCT_IS_DISPLAYED_IN_PORTAL + BICECEConstants.FALSE);
    }

    return statusPS;
  }

  private boolean isPortalLoginPageVisible() {
    boolean status = false;
    try {
      status = portalPage.checkIfElementExistsInPage("createAccountCEP", 60);
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

  @Step("Adding seat from portal for BIC orders")
  public HashMap<String, String> createAndValidateAddSeatOrderInPortal(String addSeatQty,
      LinkedHashMap<String, String> testDataForEachMethod, Map<String, String> localeMap) {
    driver.switchTo().defaultContent();
    HashMap<String, String> orderDetails = new HashMap<String, String>();
    orderDetails.putAll(createAddSeatOrder(addSeatQty, testDataForEachMethod, localeMap));
    orderDetails.putAll(validateAddSeatOrder(orderDetails));
    return orderDetails;
  }

  public HashMap<String, String> navigateToSubscriptionAndOrdersTab(Map<String, String> localeMap) {
    Util.printInfo("Navigating to subscriptions and orders tab...");
    HashMap<String, String> orderDetails = new HashMap<String, String>();
    Util.sleep(60000);
    try {
      if (portalPage.checkIfElementExistsInPage("portalLinkSubscriptions", 10) == true) {
        Util.printInfo("Clicking on portal subscription and contracts link...");
        portalPage.clickUsingLowLevelActions("portalLinkSubscriptions");
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
          driver.get("https://stg-manage.autodesk.com/billing/subscriptions-contracts");
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

        String pin = portalPage.getTextFromLink("portalSubscriptionZipFromSubs");
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
      LinkedHashMap<String, String> testDataForEachMethod, Map<String, String> localeMap) {
    Util.printInfo("Placing add seat order from portal...");
    HashMap<String, String> orderDetails = new HashMap<String, String>();

    try {
      orderDetails.putAll(navigateToSubscriptionAndOrdersTab(localeMap));

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
          driver.get("https://stg-manage.autodesk.com/billing/add-seats");
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
      clickOnContinueBtn();

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
      String confirmProratedAmount = portalPage.getLinkText("portalASConfirmProratedPrice");

      AssertUtils.assertEquals(
          Double.valueOf(data.get("proratedFinalAmount")
              .substring(data.get("proratedFinalAmount").indexOf("$") + 1).replace(",", "")),
          Double.valueOf(confirmProratedAmount.substring(confirmProratedAmount.indexOf("$") + 1)
              .replace(",", "")));

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

  @Step("Reduce seats from portal for BIC orders")
  public HashMap<String, String> reduceSeatsInPortalAndValidate(Map<String, String> localeMap)
      throws MetadataException {
    driver.switchTo().defaultContent();
    HashMap<String, String> orderDetails = new HashMap<>();
    orderDetails.putAll(reduceSeats(localeMap));
    validateReducedSeats(orderDetails);
    return orderDetails;
  }

  public HashMap<String, String> reduceSeats(Map<String, String> localeMap)
      throws MetadataException {
    HashMap<String, String> orderDetails = new HashMap<>();
    orderDetails.putAll(navigateToSubscriptionAndOrdersTab(localeMap));

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
    Util.printInfo("Recording new seats count.");
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
      String[] paymentCardDetails, Map<String, String> localeMap) {
    Util.printInfo("Changing the payment method from portal...");
    try {
      debugPageUrl("Step 1");
      data.putAll(navigateToSubscriptionAndOrdersTab(localeMap));
      clickPortalClosePopup();
      Util.printInfo("Clicking on change payment option...");
      portalPage.waitForFieldPresent("portalChangePaymentBtn", 10000);
      portalPage.clickUsingLowLevelActions("portalChangePaymentBtn");
      portalPage.waitForPageToLoad();
      Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator(
          BICECEConstants.PORTAL_PAYMENT_METHOD)
          .replaceAll(BICECEConstants.PAYMENTOPTION, "Credit card"));
      addPaymentDetails(data, paymentCardDetails);
      validatePaymentDetailsOnPortal(data, localeMap);
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

  private void findAndClickSaveButton(String elementXpath) throws  Exception{
    Util.sleep(5000);
    List<WebElement> list = portalPage.getMultipleWebElementFromXpath(elementXpath);
    int len = list.size();
    while(len > 0){
      try {
        list.get(len-1).click();
        Util.printInfo("Save Button found and clicked..");
        break;
      } catch (Exception e){
        Util.printInfo("Unable to click on Save Button.Trying the next Save Button in the list.");
        len--;
      }
    }
  }

  @Step("Add Paypal Payment Details")
  public void populatePaypalDetails(HashMap<String, String> data) {
    Util.printInfo("Switching to latest window...");
    String parentWindow = driver.getWindowHandle();
    String paymentMethod = portalPage.getFirstFieldLocator(BICECEConstants.PORTAL_PAYMENT_METHOD)
        .replaceAll(BICECEConstants.PAYMENTOPTION, "PayPal");
    Util.waitForElement(paymentMethod, "PayPal tab");

    try {
      Util.printInfo("Clicking on Paypal payments tab...");
      driver.findElement(By.xpath(paymentMethod)).click();

      Util.printInfo("Clicking on Paypal checkout button...");
      BICTestBase.bicPage.selectFrame("paypalCheckoutOptionFrame");
      Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("paypalCheckout"));
      portalPage.clickUsingLowLevelActions("paypalCheckout");

      Set<String> windows = driver.getWindowHandles();
      for (String window : windows) {
        driver.switchTo().window(window);
      }

      driver.manage().window().maximize();
      portalPage.waitForPageToLoad();
      BICTestBase.bicPage.waitForElementToDisappear("paypalPageLoader", 30);

      String title = driver.getTitle();
      AssertUtils.assertTrue(title.toUpperCase().contains("PayPal".toUpperCase()),
          "Current title [" + title + "] does not contains keyword : PayPal");

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (BICTestBase.bicPage
          .checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 10)) {
        BICTestBase.bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
      }

      if (BICTestBase.bicPage
          .checkIfElementExistsInPage(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON, 10)) {
        BICTestBase.bicPage
            .clickUsingLowLevelActions(BICECEConstants.PAYPAL_CHANGE_USERNAME_BUTTON);
      }

      Util.printInfo("Entering paypal user name [" + data.get("paypalUser") + "]...");
      BICTestBase.bicPage.waitForElementVisible(
          BICTestBase.bicPage.getMultipleWebElementsfromField("paypalUsernameField").get(0), 10);
      BICTestBase.bicPage.populateField("paypalUsernameField", data.get("paypalUser"));

      BICTestBase.bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_NEXT_BUTTON);

      Util.printInfo("Entering paypal password...");
      BICTestBase.bicPage.populateField("paypalPasswordField",
          ProtectedConfigFile.decrypt(data.get("paypalSsap")));

      Util.printInfo("Clicking on login button...");
      BICTestBase.bicPage.clickUsingLowLevelActions("paypalLoginBtn");
      BICTestBase.bicPage.waitForElementToDisappear("paypalPageLoader", 30);
      Util.sleep(5000);

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (BICTestBase.bicPage
          .checkIfElementExistsInPage(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN, 10)) {
        BICTestBase.bicPage.clickUsingLowLevelActions(BICECEConstants.PAYPAL_ACCEPT_COOKIES_BTN);
      }

      Util.printInfo("Selecting paypal payment option " + data.get("paypalPaymentType"));
      String paymentTypeXpath = BICTestBase.bicPage.getFirstFieldLocator("paypalPaymentOption")
          .replace(BICECEConstants.PAYMENTOPTION, data.get("paypalPaymentType"));
      driver.findElement(By.xpath(paymentTypeXpath)).click();

      BICTestBase.bicPage.executeJavascript("window.scrollBy(0,1000);");
      try {
        Util.printInfo("Clicking on agree and continue button...");
        BICTestBase.bicPage.clickUsingLowLevelActions("paypalAgreeAndContBtn");
      } catch (Exception e) {
        Util.printInfo("Clicking on save and continue button...");
        portalPage.clickUsingLowLevelActions("portalPaypalSaveAndContinueBtn");
      }
      Util.sleep(10000);

      driver.switchTo().window(parentWindow);
      Util.sleep(5000);
      AssertUtils.assertEquals(portalPage.getTextFromLink("portalPaypalConfirmationText"),
          "PayPal is selected for payment.");
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter paypal details to make payment...");
    }
    Util.sleep(10000);
  }

  @Step("Populate Direct Debit payment details")
  public void populateACHPaymentDetails(String[] paymentCardDetails) {
    String paymentMethod = portalPage.getFirstFieldLocator(BICECEConstants.PORTAL_PAYMENT_METHOD)
        .replaceAll(BICECEConstants.PAYMENTOPTION, "Direct Debit (ACH)");
    Util.waitForElement(paymentMethod, "debit card ACH tab");

    try {
      Util.printInfo("Clicking on Direct Debit ACH tab...");
      driver.findElement(By.xpath(paymentMethod)).click();

      Util.printInfo("Waiting for Direct Debit ACH Header...");
      BICTestBase.bicPage.waitForElementVisible(
          BICTestBase.bicPage.getMultipleWebElementsfromField("directDebitHead").get(0), 10);

      // TODO Replace this with condition where we are reading from test class API whether credit card is available or not
      if (portalPage.checkIfElementExistsInPage("portalDebitCardAddLink", 10)) {
        portalPage.clickUsingLowLevelActions("portalDebitCardAddLink");
      }

      Util.sleep(3000);
      Util.printInfo("Entering Direct Debit ACH Account Number : " + paymentCardDetails[0]);
      BICTestBase.bicPage.populateField("achAccNumber", paymentCardDetails[0]);

      Util.printInfo("Entering Direct Debit ACH Routing Number : " + paymentCardDetails[1]);
      BICTestBase.bicPage.populateField("achRoutingNumber", paymentCardDetails[1]);
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to enter Direct Debit details to make payment");
    }
    Util.sleep(10000);
  }

  @Step("Populate credit card details")
  public void populateCreditCardDetails(String[] paymentCardDetails) {
    BICTestBase.bicPage.waitForField("creditCardNumberFrame", true, 30000);
    String paymentMethod = portalPage.getFirstFieldLocator(BICECEConstants.PORTAL_PAYMENT_METHOD)
        .replaceAll(BICECEConstants.PAYMENTOPTION, "Credit card");
    Util.waitForElement(paymentMethod, "Credit card tab");
    driver.findElement(By.xpath(paymentMethod)).click();
    try {
      // TODO Replace this with condition where we are reading from test class API whether credit card is available or not
      if (portalPage.checkIfElementExistsInPage("portalCreditCardAddLink", 10)) {
        portalPage.clickUsingLowLevelActions("portalCreditCardAddLink");
      }

      Util.sleep(3000);
      WebElement creditCardNumberFrame = BICTestBase.bicPage
          .getMultipleWebElementsfromField("creditCardNumberFrame").get(0);
      WebElement expiryDateFrame = BICTestBase.bicPage
          .getMultipleWebElementsfromField("expiryDateFrame").get(0);
      WebElement securityCodeFrame = BICTestBase.bicPage
          .getMultipleWebElementsfromField("securityCodeFrame").get(0);

      driver.switchTo().frame(creditCardNumberFrame);
      Util.printInfo("Entering card number : " + paymentCardDetails[0]);
      Util.sleep(2000);
      BICTestBase.bicPage.populateField("CardNumber", paymentCardDetails[0]);
      driver.switchTo().defaultContent();
      Util.sleep(2000);

      driver.switchTo().frame(expiryDateFrame);
      Util.printInfo(
          "Entering Expiry date : " + paymentCardDetails[1] + "/" + paymentCardDetails[2]);
      Util.sleep(2000);
      BICTestBase.bicPage
          .populateField("expirationPeriod", paymentCardDetails[1] + paymentCardDetails[2]);
      driver.switchTo().defaultContent();
      Util.sleep(2000);
      driver.switchTo().frame(securityCodeFrame);
      Util.printInfo("Entering security code : " + paymentCardDetails[3]);
      Util.sleep(2000);
      BICTestBase.bicPage.populateField("PAYMENTMETHOD_SECURITY_CODE", paymentCardDetails[3]);
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
      firstNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
      lastNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
    } else if (paymentType.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
      firstNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
      lastNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, "ach");
    } else {
      firstNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FIRST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
      lastNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.LAST_NAME)
          .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
    }

    BICTestBase.clearTextInputValue(driver.findElement(By.xpath(firstNameXpath)));
    driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get("firstname"));

    BICTestBase.clearTextInputValue(driver.findElement(By.xpath(lastNameXpath)));
    driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get("lastname"));

    if (data.size() == 6) {
      Util.printInfo("Populating EMEA Billing Details");
      status = populateEMEABillingDetails(data);
      BICTestBase.bicPage.waitForPageToLoad();
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
      status = BICTestBase.bicPage.waitForElementVisible(
          BICTestBase.bicPage.getMultipleWebElementsfromField("Organization_NameEMEA").get(0),
          60000);
    } catch (MetadataException e) {
      AssertUtils.fail("Organization_NameEMEA is not displayed on page...");
    }
    BICTestBase.bicPage.populateField("Organization_NameEMEA", address.get(
        BICECEConstants.ORGANIZATION_NAME));
    BICTestBase.bicPage
        .populateField("Full_AddressEMEA", address.get(BICECEConstants.FULL_ADDRESS));
    BICTestBase.bicPage.populateField("CityEMEA", address.get("City"));
    BICTestBase.bicPage.populateField("ZipcodeEMEA", address.get(BICECEConstants.ZIPCODE));
    BICTestBase.bicPage.populateField("Phone_NumberEMEA", address.get("phone"));
    BICTestBase.bicPage.populateField("CountryEMEA", address.get(BICECEConstants.COUNTRY));
    return status;
  }

  private boolean populateNAMERBillingDetails(Map<String, String> address, String paymentType,
      String userType) {
    Util.printInfo("Adding billing details...");
    boolean status = false;
    String orgNameXpath = "", fullAddrXpath = "", cityXpath = "", zipXpath = "", phoneXpath = "", countryXpath = "", stateXpath = "";
    switch (paymentType.toUpperCase()) {

      case BICConstants.paymentTypePayPal:
        orgNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        fullAddrXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        cityXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.CITY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        zipXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        phoneXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        countryXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        stateXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYPAL);
        break;
      case BICConstants.paymentTypeDebitCard:
        orgNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        fullAddrXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        cityXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.CITY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        zipXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        phoneXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        countryXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        stateXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.PAYMENT_ACH_LOWERCASE);
        break;
      default:
        orgNameXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ORGANIZATION_NAME)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        fullAddrXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.FULL_ADDRESS)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        cityXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.CITY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        zipXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.ZIPCODE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        phoneXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.PHONE_NUMBER)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        countryXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.COUNTRY)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        stateXpath = BICTestBase.bicPage.getFirstFieldLocator(BICECEConstants.STATE_PROVINCE)
            .replace(BICECEConstants.PAYMENT_PROFILE, BICECEConstants.CREDIT_CARD);
        break;
    }

    Util.sleep(3000);
    WebDriverWait wait = new WebDriverWait(driver, 60);
    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(orgNameXpath)));
    status = driver.findElement(By.xpath(orgNameXpath)).isDisplayed();

    if (status == false) {
      AssertUtils.fail("Organization_Name not available.");
    }

    driver.findElement(By.xpath(orgNameXpath)).click();
    driver.findElement(By.xpath(orgNameXpath))
        .sendKeys(new RandomStringUtils().random(10, true, true));

    BICTestBase.clearTextInputValue(driver.findElement(By.xpath(fullAddrXpath)));
    driver.findElement(By.xpath(fullAddrXpath)).sendKeys(address.get(BICECEConstants.FULL_ADDRESS));

    BICTestBase.clearTextInputValue(driver.findElement(By.xpath(cityXpath)));
    driver.findElement(By.xpath(cityXpath)).sendKeys(address.get(BICECEConstants.CITY));

    BICTestBase.clearTextInputValue(driver.findElement(By.xpath(zipXpath)));
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

  public void validatePaymentDetailsOnPortal(HashMap<String, String> data,
      Map<String, String> localeMap) {
    Util.printInfo("Validating payment details...");
    data.putAll(navigateToSubscriptionAndOrdersTab(localeMap));
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
      portalPage.clickUsingLowLevelActions("alignBillingSubmit");
      portalPage.waitForPageToLoad();
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
  public boolean validateMetaOrderProductInCEP(String cepURL, String portalUserName,
      String portalPassword, String subscriptionID) {
    boolean status = false;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    if (isPortalTabsVisible()) {
      try {
        // The subscription id that is being displayed in Portal is different from Pelican. Hence, Checking for "Subscription ID" text
        if (System.getProperty(BICECEConstants.PAYMENT)
            .equals(BICECEConstants.PAYMENT_TYPE_FINANCING)) {
          subscriptionID = "Subscription ID";
        }
        status = isSubscriptionDisplayedInBO(subscriptionID);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (!status) {
      AssertUtils.fail(BICECEConstants.PRODUCT_IS_DISPLAYED_IN_PORTAL + BICECEConstants.FALSE);
    }
    return status;
  }

  /**
   * Navigate to the all products page in portal
   *
   * @param cepURL - URL of portal to visit
   */
  public void validateProductByName(String cepURL) {
    openPortalBICLaunch(cepURL);
    clickALLPSLink();
  }

  /**
   * Find the last purchased product and determine if it's peId matches the provided pattern
   *
   * @param peIdPattern - Regex pattern to match
   * @return - Full pe ID found
   */
  @Step("Verify product is visible in Portal " + GlobalConstants.TAG_TESTINGHUB)
  public String verifyProductVisible(HashMap<String, String> results, String peIdPattern) {
    Integer attempts = 0;
    Boolean status = false;
    String subscriptionID = null;

    while (attempts < 5) {
      Pattern pattern = Pattern.compile(peIdPattern);
      try {
        String lastProductXPath = portalPage.getFirstFieldLocator("lastPurchasedProduct");
        WebElement lastProduct = driver.findElement(By.xpath(lastProductXPath));
        subscriptionID = lastProduct.getAttribute("data-pe-id");
        status = pattern.matcher(subscriptionID).find();
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
        AssertUtils.assertTrue(pattern.matcher(subscriptionID).find());
        break;
      }
    }

    return subscriptionID;
  }

  @Step("Portal : Cancel subscription" + GlobalConstants.TAG_TESTINGHUB)
  public void cancelSubscription(String portalUserName, String portalPassword)
      throws MetadataException {
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    openSubscriptionsLink();
    clickOnSubscriptionRow();
    checkEmailVerificationPopupAndClick();
    closeAlertBanner();
    closeSubscriptionTermPopup();

    Util.printInfo("Going through cancel flow");
    portalPage.clickUsingLowLevelActions("autoRenewOffButton");
    portalPage.clickUsingLowLevelActions("autoRenewOffContinue");
    radioButtonClick("autoRenewOffRadioButton", 6);
    portalPage.populateField("autoRenewOffComments", "Test cancellation.");
    portalPage.clickUsingLowLevelActions("autoRenewTurnOffButton");
    portalPage.clickUsingLowLevelActions("autoRenewDone");
  }

  @Step("Close Subscription Term Popup")
  private void closeSubscriptionTermPopup() throws MetadataException {
    if (portalPage.checkIfElementExistsInPage("portalSubscriptionTermPopup", 10)) {
      Util.printInfo("Closing subscription popup");
      portalPage.clickUsingLowLevelActions("portalCloseButton");
    } else {
      Util.printInfo("Subscription popup not present");
    }
  }

  @Step("Close Portal Banner")
  private void closeAlertBanner() throws MetadataException {
    if (portalPage.checkIfElementExistsInPage("portalBannerAlert", 10)) {
      Util.printInfo("Closing portal banner");
      portalPage.clickUsingLowLevelActions("portalBannerClose");
    } else {
      Util.printInfo("Portal banner not present");
    }
  }

  @Step("Click on a radio button")
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
    JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
    clickWithJavaScriptExecutor(javascriptExecutor, "//*[@data-wat-val=\"me-menu:sign out\"]");
    Util.sleep(3000);

    if (isPortalLoginPageVisible()) {
      portalLogin(userEmail, password);
    }
  }
}
