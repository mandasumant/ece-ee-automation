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
import org.openqa.selenium.support.ui.Select;

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
    // Construct MOE URL with opptyId
    String guacBaseURL = data.get("guacBaseURL");
    String guacMoeResourceURL = data.get("guacMoeResourceURL");
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");

    String constructMoeURLWithOpptyId = guacBaseURL + locale + "/" + guacMoeResourceURL;

    System.out.println("constructMoeURL " + constructMoeURLWithOpptyId);

    // Navigate to Url
    bicTestBase.getUrl(constructMoeURLWithOpptyId);
    bicTestBase.disableChatSession();

    // TODO: Validate that the correct locale has been loaded i.e. the locale on GUAC MOE matches the country value that's in the Opportunity in Salesforce.

    loginToMoe();

    // Perform account lookup for the customer's email address that will show as 'account not found'.
    Names names = bicTestBase.generateFirstAndLastNames();
    String emailID = bicTestBase.generateUniqueEmailID();
    emulateUser(emailID, names);

    // TODO: Validate that the product that's added by default to GUAC MOE cart matches with the line items that are part of the Opportunity (passed to GUAC as part of get Opty call).

    // TODO: Validate that the address fields that are pre-filled in the payment section matches the address that's in the Opportunity in salesforce (passed to GUAC as part of get Opty call).

    // TODO: Order is successfully placed in backend systems. Validate that the order origin for this order is GUAC_MOE_DIRECT

    // Populate Billing info and save payment profile
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
  @Step("Validation of Create BIC Order with Quote from MOE" + GlobalConstants.TAG_TESTINGHUB)
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

    // Jira ref.: ECEEPLT-1489
    // TODO: Validate that submitted order have order origin value as GUAC_MOE_DIRECT.
    // TODO: Validate that the Pelican Finance Report mention the Quote ID from the order that was placed.
    // TODO: Validate that the Opty get closed in SFDC.

    return results;
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Creating quote from DTC page" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createQuoteWithoutOppty(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    String guacBaseURL = data.get("guacBaseURL");
    String guacMoeResourceURL = data.get("guacMoeResourceURL");
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");

    Names names = bicTestBase.generateFirstAndLastNames();
    String emailID = bicTestBase.generateUniqueEmailID();

    // Sales agent sends a quote to the customer from DTC page
    String quoteNumber = sendQuoteFromDtcPage(data, guacBaseURL, locale, emailID, names);

    // Confirm if quote is correctly saved against user's email address
    assertQuoteIsSavedForUser(names, guacBaseURL, guacMoeResourceURL, locale, emailID, quoteNumber);

    results.put(BICConstants.emailid, emailID);
    results.put("quoteNumber", quoteNumber);

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

    bicTestBase.getUrl(constructGuacMoeURL);

    loginToMoe();

    emulateUser(emailID, names);

    validateProductNameDefault();

    validateQuoteToggleDefaultView();

    populateQuoteDetailsAndSend(emailID);

    validateQuoteReadOnlyView();

    addNewProductWithQuoteDetailsAndSend(emailID);

    selectQuoteElementFromDropdown();

    validateProductNameDefault();

    validateQuoteDropdownFromOrderView();

    String orderNumber = savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);

    bicTestBase
        .printConsole(constructGuacMoeURL, orderNumber, emailID, address, names.firstName,
            names.lastName,
            paymentMethod);

    loginToPortalWithUserAccount(data, emailID, password);

    return orderNumber;
  }

  private String sendQuoteFromDtcPage(LinkedHashMap<String, String> data, String guacBaseURL,
      String locale, String emailID, Names names) throws MetadataException {
    // Construct MOE DTC URL
    String guacMoeDTCURL = data.get("guacMoeDTCURL");
    String constructMoeDtcUrl = guacBaseURL + locale + "/" + guacMoeDTCURL;
    System.out.println("constructMoeURL " + constructMoeDtcUrl);

    bicTestBase.getUrl(constructMoeDtcUrl);

    loginToMoe();
    moePage.clickUsingLowLevelActions("moeDtcContinueBtn");

    addProductFromSearchResult();

    // Sales agent should see the toggle that allows them to switch between 'Order' and 'Quote' view
    validateOrderDefaultViewDtc();

    // Populate customer info and send quote.
    String quoteNumber = populateCustomerInfoAndSendQuoteDTC(emailID, names);

    // CTA should change from 'Send quote' to 'Resend quote'
    validateQuoteReadOnlyView();

    //TODO: The customer should get an email with the quote attached as a PDF.
    //TODO: If agent clicks on 'Resend quote' CTA, customer should get the email again with the quote attached as PDF. Currently does not work due to bug.

    return quoteNumber;
  }

  private void assertQuoteIsSavedForUser(Names names, String guacBaseURL,
      String guacMoeResourceURL,
      String locale, String emailID, String quoteNumber)
      throws MetadataException {
    // construct MOE URL with an OpptyId. Will need to replace/hardcode some OpptyId once we merge Sumant's story.
    String constructMoeURLWithOpptyId = guacBaseURL + locale + "/" + guacMoeResourceURL;
    System.out.println("constructMoeURL " + constructMoeURLWithOpptyId);

    bicTestBase.getUrl(constructMoeURLWithOpptyId);

    // Click on Relogin button to continue
    moePage.clickUsingLowLevelActions("moeReLoginLink");
    moePage.waitForPageToLoad();

    // Enter email address which we used to send a quote to on DTC page
    emulateUser(emailID, names);

    // Assert that the quote is the same as we generated on DTC page
    String actualQuoteNumber = moePage.getValueFromGUI("moeQuoteNumber");
    AssertUtils.assertEquals("Quote number is correctly saved against the user.", actualQuoteNumber,
        quoteNumber);
  }

  @Step("Login to MOE")
  private void loginToMoe() {
    Util.printInfo("Re-Login");
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

  @Step("Save payment profile and submit order")
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

  @Step("Emulate user")
  private void emulateUser(String emailID, Names names) throws MetadataException {
    Util.printInfo("Emulate User");
    moePage.click("moeAccountLookupEmail");
    moePage.populateField("moeAccountLookupEmail", emailID);
    moePage.click("moeAccountLookupBtn");
    moePage.waitForPageToLoad();
    if (moePage.checkIfElementExistsInPage("moeContinueBtn", 10)) {
      moePage.click("moeContinueBtn");
    } else {
      // Enter first and last names
      moePage.populateField("moeUserFirstNameField", names.firstName);
      moePage.populateField("moeUserLastNameField", names.lastName);
      // Click send account setup invite
      moePage.click("moeSendSetupInviteBtn");
      moePage.waitForPageToLoad();
      // Close account setup invite sent  popup modal
      moePage.click("moeModalCloseBtn");
    }
    moePage.waitForPageToLoad();
    Util.printInfo("Successfully emulated user");
  }

  private void validateProductNameDefault() {
    validateProductName("AutoCAD LT");
  }

  private void validateProductNameNew() {
    validateProductName("3ds Max");
  }

  @Step("Validate product name")
  private void validateProductName(String title) {
    Util.printInfo("Validate product added");

    // TODO: replace static data with product name added in Salesforce while creating the optyId
    String productTitle = driver.findElement(By.xpath(
        "//h5[@class=\"checkout--product-bar--info-column--name-sub-column--name wd-mr-24\"]"))
        .getText();
    Util.printInfo("Product title: " + productTitle);
    AssertUtils.assertEquals(title, productTitle);
  }

  @Step("Validate Quote toggle default view")
  private void validateQuoteToggleDefaultView() {
    Util.printInfo("Validate Quote toggle default view");
    try {
      AssertUtils.assertTrue(driver.findElements(By.id("moe-quotes")).isEmpty());
      driver.findElement(By.xpath("//input[@aria-labelledby=\"quote-toggle-off\"]"))
          .getAttribute("aria-checked")
          .contains("true");
      driver.findElement(By.xpath("//input[@aria-labelledby=\"quote-toggle-on\"]"))
          .getAttribute("aria-checked")
          .contains("false");
    } catch (Exception e) {
      AssertUtils.fail("Quote toggle not matching expectation");
    }
  }

  @Step("Populate Quote details and send")
  private void populateQuoteDetailsAndSend(String emailID) {
    Util.printInfo("Populate Quote details and send");

    // Open Quote section
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("document.getElementById('quote-radio').click()");
    moePage.waitForPageToLoad();

    AssertUtils.assertTrue(driver
        .findElement(By.xpath("//h3[contains(text(),\"Quote contact information\")]"))
        .isDisplayed());

    // Note: contact information should be filled in with the opportunity contact info as per requirements
    // Jira ECEECOM-1621: only adding state and phone number since they do not come back from salesforce.
    moePage.populateField("moeQuoteStateField", "WA");
    moePage.populateField("moeQuotePhoneNumberField", "1234567890");

    setExpirationData();

    moePage.populateField("moeQuoteSecondaryEmail", "test-" + emailID);

    sendQuote();
  }

  @Step("Select Quote from dropdown list")
  private void selectQuoteElementFromDropdown() {
    Util.printInfo("Select Quote");
    try {
      Select drpQuote = new Select(driver.findElement(By.name("quotes")));
      drpQuote.selectByIndex(2);
      bicTestBase.waitForLoadingSpinnerToComplete();
    } catch (Exception e) {
      AssertUtils.fail("Unable to select third element from Quote dropdown list");
    }
  }

  private void setExpirationData() {
    Util.printInfo("Enter expiration date");

    DateFormat dateFormat1 = new SimpleDateFormat("yyyy");
    Date date1 = new Date();
    String currentYear = dateFormat1.format(date1);

    DateFormat dateFormat2 = new SimpleDateFormat("MMdd");
    Date date2 = new Date();
    String currentMonthDay = dateFormat2.format(date2);

    WebElement webElement = driver.findElement(
        By.xpath("//input[@id=\"moe--quote--expiration-date\"]"));
    webElement.sendKeys(currentYear);
    webElement.sendKeys(Keys.TAB);
    webElement.sendKeys(currentMonthDay);
  }

  private void sendQuote() {
    Util.printInfo("Click on 'Send quote' cta");
    moePage.click("moeSendQuote");
    bicTestBase.waitForLoadingSpinnerToComplete();
    // TODO: add try catch block to validate if error modal loaded
    try {
      AssertUtils.assertTrue(driver
          .findElement(By.xpath("//button/span[contains(text(),\"Resend quote\")]"))
          .isDisplayed());
    } catch (Exception e) {
      AssertUtils.fail("Can not find 'Resend quote' button");
    }
  }

  @Step("Validate Quote section from read only view")
  private void validateQuoteReadOnlyView() {
    Util.printInfo("Validate Quote Read only view");
    try {
      driver.findElement(By.xpath("//input[@aria-labelledby=\"quote-toggle-on\"]"))
          .getAttribute("aria-checked")
          .contains("true");
      AssertUtils.assertTrue(driver
          .findElement(By.xpath("//h4[contains(text(),\"Quote contact information\")]"))
          .isDisplayed());
      AssertUtils.assertTrue(driver
          .findElement(By.xpath("//h5[contains(text(),\"Quote #:\")]"))
          .isDisplayed());
    } catch (Exception e) {
      AssertUtils.fail("Unable to find element from Quote read only view");
    }
  }

  private void emptyCart() {
    try {
      Util.printInfo("Delete cart item");
      moePage.clickUsingLowLevelActions("moeDeleteProduct");
      bicTestBase.waitForLoadingSpinnerToComplete();
      AssertUtils.assertTrue(driver
          .findElement(By.xpath("//h3[contains(text(),\"Your cart is empty\")]"))
          .isDisplayed());
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to delete cart item");
    }
  }

  private void addProductFromSearchResult() {
    try {
      Util.printInfo("Add product");
      WebElement webElement = driver.findElement(By.xpath("//input[@id=\"downshift-0-input\"]"));
      webElement.sendKeys("3ds max 1 month");
      webElement.sendKeys(Keys.RETURN);
      moePage.clickUsingLowLevelActions("moeSelectProductFromSearchResult");
      bicTestBase.waitForLoadingSpinnerToComplete();
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to retrieve product from search result");
    }
  }

  @Step("Add new product with Quote details and send")
  private void addNewProductWithQuoteDetailsAndSend(String emailID) {
    emptyCart();

    addProductFromSearchResult();

    validateProductNameNew();

    populateQuoteDetailsAndSend(emailID);

    validateProductNameNew();

    validateQuoteReadOnlyView();
  }

  @Step("Validate Quote dropdown list from Order view")
  private void validateQuoteDropdownFromOrderView() throws MetadataException {
    moePage.clickUsingLowLevelActions("moeRadioButtonOrder");
    Util.sleep(2000);
    Select selection = new Select(driver.findElement(By.id("moe-quotes")));
    int size = selection.getOptions().size();
    Util.printInfo("Number of items: " + size);
    AssertUtils.assertTrue(3 == size);
  }

  @Step("Login to Portal with user account")
  private void validateOrderDefaultViewDtc() {
    Util.printInfo("MOE - Order view");
    try {
      AssertUtils.assertTrue(driver
          .findElement(By.xpath(".//h5[contains(text(),\"3ds Max\")]"))
          .isDisplayed());
      driver.findElement(By.xpath("//input[@aria-labelledby=\"quote-toggle-off\"]"))
          .getAttribute("aria-checked")
          .contains("true");
      driver.findElement(By.xpath("//input[@aria-labelledby=\"quote-toggle-on\"]"))
          .getAttribute("aria-checked")
          .contains("false");
    } catch (Exception e) {
      AssertUtils.fail("MOE - Web element not found!");
    }
  }

  private String populateCustomerInfoAndSendQuoteDTC(String emailID, Names names) {
    Util.printInfo("MOE - Send Quote");

    // Ppen Quote section
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("document.getElementById('quote-radio').click()");
    moePage.waitForPageToLoad();

    AssertUtils.assertTrue(driver
        .findElement(By.xpath("//h3[contains(text(),\"Quote contact information\")]"))
        .isDisplayed());

    // Clean first and last name fields due to bug then enter data
    String firstNameXpath = moePage.getFirstFieldLocator("moeQuoteFirstNameField");
    String lastNameXpath = moePage.getFirstFieldLocator("moeQuoteLastNameField");

    BICTestBase.clearTextInputValue(driver.findElement(By.xpath(firstNameXpath)));
    driver.findElement(By.xpath(firstNameXpath)).sendKeys(names.firstName);

    Util.sleep(1000);
    BICTestBase.clearTextInputValue(driver.findElement(By.xpath(lastNameXpath)));
    driver.findElement(By.xpath(lastNameXpath)).sendKeys(names.lastName);

    moePage.populateField("moeQuoteAddressField", "149 Penn Rd");
    moePage.populateField("moeQuoteCityField", "Silverdale");
    moePage.populateField("moeQuoteStateField", "WA");
    moePage.populateField("moeQuotePostalCodeField", "98315");
    moePage.populateField("moeQuotePhoneNumberField", "1234567890");
    moePage.populateField("moeQuoteCompanyField", "Autodesk Quote");

    moeQuoteEnterExpirationData();

    moePage.populateField("moeQuotePrimaryEmail", emailID);
    moePage.populateField("moeQuoteSecondaryEmail", "test-" + emailID);

    moePage.click("moeSendQuote");

    // Get quote number from the page
    String quoteNumber = moePage.getValueFromGUI("moeQuoteNumber");
    Util.printInfo("Quote number is: " + quoteNumber);

    moePage.waitForPageToLoad();

    Util.printInfo("MOE - Quote sent");

    return quoteNumber;
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

  @Step("Login to Portal with user account")
  private void loginToPortalWithUserAccount(LinkedHashMap<String, String> data, String emailID,
      String password) {
    // Navigate to Portal, logout from service account session and log back in with user account
    String constructPortalUrl = data.get("cepURL");
    bicTestBase.getUrl(constructPortalUrl);
    bicTestBase.loginToOxygen(emailID, password);
  }
}
