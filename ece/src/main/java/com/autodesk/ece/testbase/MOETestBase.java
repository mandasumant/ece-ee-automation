package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.BICTestBase.Names;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class MOETestBase {

  private final Page_ moePage;
  private final Map<String, String> testData;
  public WebDriver driver;
  BICTestBase bicTestBase;

  public MOETestBase(GlobalTestBase testbase, LinkedHashMap<String, String> testData) {
    Util.PrintInfo("MOETestBase from ece");
    moePage = testbase.createPage("PAGE_MOE");
    driver = testbase.getdriver();
    bicTestBase = new BICTestBase(driver, testbase);
    this.testData = testData;
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBicOrderMoe(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String guacBaseURL = data.get("guacBaseURL");
    String productID = "";
    String quantity = "";
    String guacResourceURL = data.get(BICECEConstants.GUAC_RESOURCE_URL);
    String guacMoeResourceURL = data.get("guacMoeResourceURL");
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    Util.printInfo("THE REGION " + data.get(BICECEConstants.LOCALE));
    bicTestBase.navigateToCart(data);

    String emailID = bicTestBase.generateUniqueEmailID();
    String orderNumber = getBicOrderMoe(data, emailID, guacBaseURL, guacMoeResourceURL,
        data.get(BICECEConstants.LOCALE), password, paymentMethod);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @Step("Navigate to MOE page with OpptyID" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBasicMoeOpptyOrder(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    Map<String, String> address = null;
    // construct MOE URL with opptyId
    String guacBaseURL = data.get("guacBaseURL");
    String guacMoeResourceURL = data.get("guacMoeResourceURL");
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");

    String constructMoeURLWithOpptyId =
        guacBaseURL + locale + "/"
            + guacMoeResourceURL;

    System.out.println("constructMoeURL " + constructMoeURLWithOpptyId);

    //navigate to Url
    bicTestBase.getUrl(constructMoeURLWithOpptyId);

    // TODO: Validate that the correct locale has been loaded i.e. the locale on GUAC MOE matches the country value that's in the Opportunity in Salesforce.

    loginToMoe();

    //Perform account lookup for the customer's email address that will show as 'account not found'.
    Names names = bicTestBase.generateFirstAndLastNames();
    String emailID = bicTestBase.generateUniqueEmailID();
    emulateUser(emailID, names);

    //TODO: Validate that the product that's added by default to GUAC MOE cart matches with the line items that are part of the Opportunity (passed to GUAC as part of get Opty call).

    // TODO: Validate that the address fields that are pre-filled in the payment section matches the address that's in the Opportunity in salesforce (passed to GUAC as part of get Opty call).

    // TODO: Order is successfully placed in backend systems. Validate that the order origin for this order is GUAC_MOE_DIRECT

    //Populate Billing info and save payment profile
    address = bicTestBase.getBillingAddress(data.get(BICECEConstants.REGION));
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String[] paymentCardDetails = bicTestBase.getPaymentDetails(paymentMethod.toUpperCase())
        .split("@");

    String orderNumber = savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);

    // TODO: Order confirmation page should be successfully shown with order number

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }


  @SuppressWarnings({"static-access", "unused"})
  @Step("Navigate to MOE, add Quote and place Order" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBicOrderMoeWithQuote(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String guacBaseURL = data.get("guacBaseURL");
    String productID = "";
    String quantity = "";
    String guacResourceURL = data.get(BICECEConstants.GUAC_RESOURCE_URL);
    String guacMoeResourceURL = data.get("guacMoeResourceURL");
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    Util.printInfo("THE REGION " + data.get(BICECEConstants.LOCALE));
    bicTestBase.navigateToCart(data);

    String emailID = bicTestBase.generateUniqueEmailID();
    String orderNumber = getBicOrderMoeWithQuote(data, emailID, guacBaseURL, guacMoeResourceURL,
        data.get(BICECEConstants.LOCALE), password, paymentMethod);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  private String getBicOrderMoe(LinkedHashMap<String, String> data, String emailID,
      String guacBaseURL, String guacMoeResourceURL, String locale, String password,
      String paymentMethod) throws MetadataException {
    locale = locale.replace("_", "-");
    String constructGuacMoeURL = guacBaseURL + locale + "/" + guacMoeResourceURL;
    System.out.println("constructGuacMoeURL " + constructGuacMoeURL);
    Map<String, String> address = null;

    address = bicTestBase.getBillingAddress(data.get(BICECEConstants.REGION));

    Names names = bicTestBase.generateFirstAndLastNames();
    bicTestBase.createBICAccount(names, emailID, password);

    data.putAll(names.getMap());

    String[] paymentCardDetails = bicTestBase.getPaymentDetails(paymentMethod.toUpperCase())
        .split("@");
    bicTestBase.debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);

    // Get Payment details
    bicTestBase.selectPaymentProfile(data, paymentCardDetails, address);

    // Enter billing details
    if (data.get(BICECEConstants.BILLING_DETAILS_ADDED) != null && !data
        .get(BICECEConstants.BILLING_DETAILS_ADDED).equals(BICECEConstants.TRUE)) {
      bicTestBase.debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
      bicTestBase.populateBillingAddress(address, data);
      bicTestBase.debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }

    bicTestBase.getUrl(constructGuacMoeURL);
    loginToMoe();
    emulateUser(emailID, names);
    String orderNumber = savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);

    bicTestBase
        .printConsole(constructGuacMoeURL, orderNumber, emailID, address, names.firstName,
            names.lastName,
            paymentMethod);

    loginToPortalWithUserAccount(data, emailID, password);

    return orderNumber;
  }

  private String getBicOrderMoeWithQuote(LinkedHashMap<String, String> data, String emailID,
      String guacBaseURL, String guacMoeResourceURL, String locale, String password,
      String paymentMethod) throws MetadataException {
    locale = locale.replace("_", "-");
    String constructGuacMoeURL = guacBaseURL + locale + "/" + guacMoeResourceURL;
    System.out.println("constructGuacMoeURL " + constructGuacMoeURL);
    Map<String, String> address = null;

    address = bicTestBase.getBillingAddress(data.get(BICECEConstants.REGION));

    Names names = bicTestBase.generateFirstAndLastNames();
    bicTestBase.createBICAccount(names, emailID, password);

    data.putAll(names.getMap());

    String[] paymentCardDetails = bicTestBase.getPaymentDetails(paymentMethod.toUpperCase())
        .split("@");
    bicTestBase.debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);

    // Get Payment details
    bicTestBase.selectPaymentProfile(data, paymentCardDetails, address);

    // Enter billing details
    if (data.get(BICECEConstants.BILLING_DETAILS_ADDED) != null && !data
        .get(BICECEConstants.BILLING_DETAILS_ADDED).equals(BICECEConstants.TRUE)) {
      bicTestBase.debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
      bicTestBase.populateBillingAddress(address, data);
      bicTestBase.debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }

    bicTestBase.getUrl(constructGuacMoeURL);
    loginToMoe();
    emulateUser(emailID, names);

    validateOrderDefaultView();

    sendQuoteCustomerContactInformation(emailID);

    validateQuoteReadOnlyView();

    // open Order section
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript(
        "document.querySelector('input[aria-labelledby=\"quote-toggle-off\"]').click()");
    Util.sleep(5000);

    String orderNumber = savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);

    bicTestBase
        .printConsole(constructGuacMoeURL, orderNumber, emailID, address, names.firstName,
            names.lastName,
            paymentMethod);

    loginToPortalWithUserAccount(data, emailID, password);

    return orderNumber;
  }

  private void loginToMoe() {
    Util.printInfo("MOE - Re-Login");
    if (moePage.isFieldVisible("moeReLoginLink")) {
      try {
        moePage.clickUsingLowLevelActions("moeReLoginLink");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    Util.sleep(20000);
    moePage.waitForField(BICECEConstants.MOE_LOGIN_USERNAME_FIELD, true, 30000);
    moePage.click(BICECEConstants.MOE_LOGIN_USERNAME_FIELD);
    moePage.populateField(BICECEConstants.MOE_LOGIN_USERNAME_FIELD,
        "svc_s_platform_autob@autodesk.com");
    moePage.click("moeLoginButton");
    moePage.waitForField(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD, true, 30000);
    moePage.click(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD);
    moePage.populateField(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD,
        "EM5AhbaOir5DnLhlP@$iialVOX7ypvBBKys");
    moePage.click("moeLoginButton");
    moePage.waitForPageToLoad();
    Util.printInfo("Successfully logged into MOE");
  }

  private String savePaymentProfileAndSubmitOrder(LinkedHashMap<String, String> data,
      Map<String, String> address, String[] paymentCardDetails) {
    bicTestBase.populateBillingAddress(address, data);
    bicTestBase.selectPaymentProfile(data, paymentCardDetails, address);
    try {
      moePage.clickUsingLowLevelActions("savePaymentProfile");
    } catch (Exception e) {
      e.printStackTrace();
    }
    Util.sleep(5000);
    bicTestBase.agreeToTerm();

    return bicTestBase.submitGetOrderNumber(data);
  }

  private void emulateUser(String emailID, Names names) throws MetadataException {
    Util.printInfo("MOE - Emulate User");
    moePage.click("moeAccountLookupEmail");
    moePage.populateField("moeAccountLookupEmail", emailID);
    moePage.click("moeAccountLookupBtn");
    moePage.waitForPageToLoad();
    if (moePage.checkIfElementExistsInPage("moeContinueBtn", 10)) {
      moePage.click("moeContinueBtn");
      moePage.waitForPageToLoad();
    } else {
      // enter first and last names
      moePage.populateField("moeUserFirstNameField", names.firstName);
      moePage.populateField("moeUserLastNameField", names.lastName);
      //click send account setup invite
      moePage.click("moeSendSetupInviteBtn");
      moePage.waitForPageToLoad();
      //close account setup invite sent  popup modal
      moePage.click("moeModalCloseBtn");
    }
    Util.printInfo("Successfully emulated user");
  }

  private void validateOrderDefaultView() {
    Util.printInfo("MOE - Order view");
    try {
      AssertUtils.assertTrue(driver
          .findElement(By.xpath(".//h5[contains(text(),\"3ds Max\")]"))
          .isDisplayed());
      AssertUtils.assertFalse(driver
          .findElement(By.xpath(".//select[@id=\"moe-quotes\"]"))
          .isDisplayed());
      driver.findElement(By.xpath("//input[@aria-labelledby=\"quote-toggle-off\"]"))
          .getAttribute("aria-checked")
          .contains("true");
      driver.findElement(By.xpath("//input[@aria-labelledby=\"quote-toggle-on\"]"))
          .getAttribute("aria-checked")
          .contains("false");
      AssertUtils.assertTrue(driver
          .findElement(By.xpath("//div[@data-testid=\"payment-section\"]"))
          .isDisplayed());
    } catch (Exception e) {
      Util.printInfo("MOE - Web element not found!");
    }
  }

  private void validateQuoteReadOnlyView() {
    // TODO: Validate that the read-only view of the contact is visible
    // TODO: Validate that the quote details is visible
    // TODO: Validate that the CTA 'Resend quote' is visible
    Util.printInfo("MOE - Quote Read Only View");
    try {
      AssertUtils.assertTrue(driver
          .findElement(By.xpath(".//h5[contains(text(),\"3ds Max\")]"))
          .isDisplayed());
      AssertUtils.assertTrue(driver
          .findElement(By.xpath("//h4[contains(text(),\"Quote contact information\")]"))
          .isDisplayed());
      AssertUtils.assertTrue(driver
          .findElement(By.xpath("//button/span[contains(text(),\"Resend quote\")]"))
          .isDisplayed());
    } catch (Exception e) {
      Util.printInfo("MOE - Web element not found!");
    }
  }

  private void sendQuoteCustomerContactInformation(String emailID) {
    Util.printInfo("MOE - Send Quote");

    // open Quote section
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("document.getElementById('quote-radio').click()");
    moePage.waitForPageToLoad();

    AssertUtils.assertTrue(driver
        .findElement(By.xpath("//h3[contains(text(),\"Quote contact information\")]"))
        .isDisplayed());

    // enter data
    moePage.populateField("moeQuoteFirstNameField", "Test");
    moePage.populateField("moeQuoteLastNameField", "Test");
    moePage.populateField("moeQuoteAddressField", "149 Penn Rd");
    moePage.populateField("moeQuoteCityField", "Silverdale");
    moePage.populateField("moeQuoteStateField", "WA");
    moePage.populateField("moeQuotePostalCodeField", "98315");
    moePage.populateField("moeQuotePhoneNumberField", "1234567890");
    moePage.populateField("moeQuoteCompanyField", "Autodesk Quote");

    moeQuoteEnterExpirationData();

    moePage.populateField("moeQuoteSecondaryEmail", "test-" + emailID);

    moePage.click("moeSendQuote");
    moePage.waitForPageToLoad();

    Util.printInfo("MOE - Quote sent");
  }

  private void moeQuoteEnterExpirationData() {
    Util.printInfo("MOE - Enter Expiration date");

    DateFormat dateFormat1 = new SimpleDateFormat("yyyy");
    Date date1 = new Date();
    String currentYear = dateFormat1.format(date1);

    DateFormat dateFormat2 = new SimpleDateFormat("MMdd");
    Date date2 = new Date();
    String currentMonthDay = dateFormat2.format(date2);

    Util.sleep(2000);
    WebElement webElement = driver.findElement(
        By.xpath("//input[@id=\"moe--quote--expiration-date\"]"));
    webElement.sendKeys(currentYear);
    webElement.sendKeys(Keys.TAB);
    webElement.sendKeys(currentMonthDay);
    Util.sleep(2000);
  }

  private void loginToPortalWithUserAccount(LinkedHashMap<String, String> data, String emailID,
      String password) {
    // Navigate to Portal, logout from service account session and log back in with user account
    String constructPortalUrl = data.get("cepURL");
    bicTestBase.getUrl(constructPortalUrl);
    bicTestBase.loginToOxygen(emailID, password);
  }
}
