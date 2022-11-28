package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.BICTestBase.Names;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.CommonConstants;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

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
    String guacMoeResourceURL = data.get("guacMoeResourceURL") + "A-1776440";
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);
    String password = ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD));
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
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
    String guacMoeResourceURL = data.get("guacMoeResourceURL") + data.get("guacMoeOptyId");
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");

    String constructMoeURLWithOpptyId = guacBaseURL + locale + "/" + guacMoeResourceURL;

    Util.printInfo("constructMoeURL " + constructMoeURLWithOpptyId);

    // Navigate to Url
    bicTestBase.getUrl(constructMoeURLWithOpptyId);
    bicTestBase.setStorageData();

    // TODO: Validate that the correct locale has been loaded i.e. the locale on GUAC MOE matches the country value that's in the Opportunity in Salesforce.

    loginToMoe();

    // Perform account lookup for the customer's email address that will show as 'account not found'.
    Names names = BICTestBase.generateFirstAndLastNames();
    String emailID = BICTestBase.generateUniqueEmailID();
    emulateUser(emailID, names);

    // TODO: Validate that the product that's added by default to GUAC MOE cart matches with the line items that are part of the Opportunity (passed to GUAC as part of get Opty call).

    // TODO: Validate that the address fields that are pre-filled in the payment section matches the address that's in the Opportunity in salesforce (passed to GUAC as part of get Opty call).

    // Populate Billing info and save payment profile
    address = bicTestBase.getBillingAddress(data.get(BICECEConstants.ADDRESS));
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
    String guacMoeResourceURL = data.get("guacMoeResourceURL") + data.get("guacMoeOptyId");
    String userType = data.get(BICECEConstants.USER_TYPE);
    String region = data.get(BICECEConstants.REGION);
    String password = ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD));
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    Util.printInfo("THE REGION " + data.get(BICECEConstants.LOCALE));
    bicTestBase.navigateToCart(data);

    String emailID = bicTestBase.generateUniqueEmailID();
    results.putAll(getBicOrderMoeWithQuote(data, emailID, guacBaseURL, guacMoeResourceURL,
        data.get(BICECEConstants.LOCALE), password, paymentMethod));

    results.put(BICConstants.emailid, emailID);

    // Jira ref.: ECEEPLT-1489
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
    String guacMoeResourceURL = data.get("guacMoeResourceURL") + "A-1776440";
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

  @Step("SFDC : Create GUAC MOE opportunity " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createMoeOpty(String optyName, String account, String stage,
      String projectCloseDate, String fulfillment, String sku, String currency) {
    HashMap<String, String> results = new HashMap<String, String>();

    String opportunityId = addOptyDetailsAndSave(optyName, account, stage,
        projectCloseDate, fulfillment, currency);

    results.put("opportunityId", opportunityId);

    try {
      if (StringUtils.isNotEmpty(sku)) {

        Util.printInfo("Associating Products to Opty: " + sku);

        moePage.click("titleProducts");

        moePage.click("manageProducts");
        moePage.waitForPageToLoad();

        switchToFrame("//li[@title='Add Products']/a");

        Util.printInfo("Switched iFrame to click on Add Products Tab");

        moePage.populateField("skuSearch", sku);
        moePage.clickUsingLowLevelActions("skuSearchButton");
        moePage.waitForPageToLoad();

        moePage.click("checkbox");
        Util.sleep(5000);

        moePage.populateField("estimatedUnit", "1");
        Util.sleep(2000);

        WebElement webElement = driver.findElement(
            By.xpath("//input[@class='slds-input input']"));
        webElement.sendKeys(Keys.TAB);
        Util.sleep(5000);

        Util.printInfo("Entered SKU and quantity in the Add Product");

        moePage.checkIfElementExistsInPage("addProductsButton", 30);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "document.querySelectorAll(\"button[name=AddProductsToCart]\")[0].click()");
        Util.printInfo("Clicked on cta: Add Products");
        Util.sleep(30000);

        moePage.checkIfElementExistsInPage("okButton", 40);
        moePage.clickUsingLowLevelActions("okButton");
        Util.printInfo("Clicked on cta: OK");
        Util.sleep(5000);

        moePage.clickUsingLowLevelActions("close");
        Util.printInfo("Clicked on cta: Close");
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to assign Product to MOE Opty." + e.getMessage());
    }

    return results;
  }

  @SuppressWarnings({"static-access", "unused"})
  @Step("Place order via Copy cart link generated on DTC page" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBicOrderMoeDTC(LinkedHashMap<String, String> data)
      throws Exception {
    // TODO: Start from the Salesforce's LC Home page where there's a link for 'Direct to cart'.

    HashMap<String, String> results = new HashMap<>();
    Map<String, String> address = null;
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");
    String password = ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD));
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String guacBaseURL = data.get("guacBaseURL");
    String oxygenLogOutUrl = data.get("oxygenLogOut");
    navigateToMoeDtcUrl(data, guacBaseURL, locale);

    proceedToDtcCartSection();

    addProductFromSearchResult();

    String copyCartLink = copyCartLinkFromClipboard();

    Names names = bicTestBase.generateFirstAndLastNames();
    data.putAll(names.getMap());
    String emailID = bicTestBase.generateUniqueEmailID();
    data.put(BICECEConstants.emailid, emailID);

    Util.printInfo("Log out with Oxygen direct URL: " + oxygenLogOutUrl);
    bicTestBase.getUrl(oxygenLogOutUrl);

    loginToCheckoutWithUserAccount(emailID, names, password, copyCartLink);
    Util.sleep(10000);

    address = bicTestBase.getBillingAddress(data.get(BICECEConstants.ADDRESS));

    bicTestBase.enterBillingDetails(data, address, paymentMethod);

    bicTestBase.submitOrder(data);
    String orderNumber = bicTestBase.getOrderNumber(data);
    bicTestBase.printConsole(orderNumber, data, address);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  private String getBicOrderMoe(LinkedHashMap<String, String> data, String emailID,
      String guacBaseURL, String guacMoeResourceURL, String locale, String password,
      String paymentMethod) throws MetadataException {
    locale = locale.replace("_", "-");
    String constructGuacMoeURL = guacBaseURL + locale + "/" + guacMoeResourceURL;
    Util.printInfo("GuacMoeURL: " + constructGuacMoeURL);
    Map<String, String> address = null;

    address = bicTestBase.getBillingAddress(data.get(BICECEConstants.ADDRESS));

    Names names = BICTestBase.generateFirstAndLastNames();
    bicTestBase.createBICAccount(names, emailID, password, false);
    data.putAll(names.getMap());
    data.put(BICECEConstants.emailid, emailID);

    bicTestBase.getUrl(constructGuacMoeURL);

    loginToMoe();

    emulateUser(emailID, names);

    String[] paymentCardDetails = bicTestBase.getPaymentDetails(paymentMethod.toUpperCase())
        .split("@");
    bicTestBase.debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);

    // Enter billing details
    if (data.get(BICECEConstants.BILLING_DETAILS_ADDED) != null && !data
        .get(BICECEConstants.BILLING_DETAILS_ADDED).equals(BICECEConstants.TRUE)) {
      bicTestBase.debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
      bicTestBase.populateBillingAddress(address, data);
      bicTestBase.debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }

    String orderNumber = savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);

    bicTestBase.printConsole(orderNumber, data, address);

    loginToPortalWithUserAccount(data, emailID, password);

    return orderNumber;
  }

  private HashMap<String, String> getBicOrderMoeWithQuote(LinkedHashMap<String, String> data, String emailID,
      String guacBaseURL, String guacMoeResourceURL, String locale, String password,
      String paymentMethod) throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    locale = locale.replace("_", "-");
    String constructGuacMoeURL = guacBaseURL + locale + "/" + guacMoeResourceURL;
    System.out.println("constructGuacMoeURL " + constructGuacMoeURL);
    Map<String, String> address = null;

    address = bicTestBase.getBillingAddress(data.get(BICECEConstants.ADDRESS));

    Names names = BICTestBase.generateFirstAndLastNames();
    bicTestBase.createBICAccount(names, emailID, password, false);
    data.putAll(names.getMap());
    data.put(BICECEConstants.emailid, emailID);

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

    results.put("quoteId", validateQuoteDropdownFromOrderViewAndReturnItsValue());

    String orderNumber = savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);

    bicTestBase.printConsole(orderNumber, data, address);

    loginToPortalWithUserAccount(data, emailID, password);

    results.put("orderNumber", orderNumber);
    return results;
  }

  @Step("Create a quote and send to the customer from MOE DTC page.")
  private String sendQuoteFromDtcPage(LinkedHashMap<String, String> data, String guacBaseURL,
      String locale, String emailID, Names names) throws MetadataException {
    navigateToMoeDtcUrl(data, guacBaseURL, locale);

    proceedToDtcCartSection();

    addProductFromSearchResult();

    // Sales agent should see the toggle that allows them to switch between 'Order' and 'Quote' view
    validateOrderDefaultViewDtc();

    // Populate customer info and send quote.
    String quoteNumber = populateCustomerInfoAndSendQuoteDTC(emailID, names);

    // CTA should change from 'Send quote' to 'Resend quote'
    validateQuoteReadOnlyView();

    // Agent clicks on 'Resend quote' CTA
    Util.printInfo("Click on 'Resend quote' button.");
    moePage.click("moeResendQuote");
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");

    return quoteNumber;
  }

  @Step("Assert that quote created on DTC page is reflected on OpptyId page for the same customer.")
  private void assertQuoteIsSavedForUser(Names names, String guacBaseURL,
      String guacMoeResourceURL,
      String locale, String emailID, String quoteNumber)
      throws MetadataException {
    // construct MOE URL with a test OpptyId.
    String constructMoeURLWithOpptyId = guacBaseURL + locale + "/" + guacMoeResourceURL;
    System.out.println("constructMoeURL " + constructMoeURLWithOpptyId);

    bicTestBase.getUrl(constructMoeURLWithOpptyId);

    // Click on Re-login button to continue
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

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

    WebElement getMoeReLoginLink = driver.findElement(
        By.xpath(moePage.getFirstFieldLocator("moeReLoginLink")));

    Util.printInfo("Re-Login");
    if (getMoeReLoginLink.isDisplayed()) {
      try {
        moePage.clickUsingLowLevelActions("moeReLoginLink");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (!GlobalConstants.getENV().equals(BICECEConstants.ENV_INT)) {
      wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath(moePage.getFirstFieldLocator(BICECEConstants.MOE_LOGIN_USERNAME_FIELD))));
      moePage.click(BICECEConstants.MOE_LOGIN_USERNAME_FIELD);
      moePage.populateField(BICECEConstants.MOE_LOGIN_USERNAME_FIELD,
          CommonConstants.serviceUser);
      moePage.click("moeLoginButton");
      wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath(moePage.getFirstFieldLocator(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD))));
      moePage.click(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD);
      moePage.populateField(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD, CommonConstants.serviceUserPw);
      moePage.click("moeLoginButton");
    } else {
      bicTestBase.loginToOxygen(CommonConstants.serviceUser, CommonConstants.serviceUserPw);
    }
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    Util.printInfo("Successfully logged into MOE");
  }

  @Step("Save payment profile and submit order")
  private String savePaymentProfileAndSubmitOrder(LinkedHashMap<String, String> data,
      Map<String, String> address, String[] paymentCardDetails) {
    bicTestBase.selectPaymentProfile(data, paymentCardDetails, address);
    bicTestBase.populateBillingAddress(address, data);

    bicTestBase.agreeToTerm();

    if (!System.getProperty(BICECEConstants.PAYMENT).equals(BICECEConstants.PAYMENT_TYPE_GIROPAY)) {
      bicTestBase.submitOrder(data);
    }
    return bicTestBase.getOrderNumber(data);
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
    validateProductName("AutoCAD - including specialized toolsets");
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

    setExpirationDate();

    moePage.populateField("moeQuoteSecondaryEmail", "test-" + emailID);

    sendQuote();
  }

  @Step("Select Quote from dropdown list")
  private void selectQuoteElementFromDropdown() {
    Util.printInfo("Select Quote");
    try {
      Select drpQuote = new Select(driver.findElement(By.name("quotes")));
      drpQuote.selectByIndex(2);
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    } catch (Exception e) {
      AssertUtils.fail("Unable to select third element from Quote dropdown list");
    }
  }

  private void setExpirationDate() {
    Util.printInfo("Enter expiration date");

    DateFormat dateFormat1 = new SimpleDateFormat("yyyy");
    Date date1 = new Date();
    String currentYear = dateFormat1.format(date1);

    DateFormat dateFormat2 = new SimpleDateFormat("MMdd");
    Date date2 = new Date();
    String currentMonthDay = dateFormat2.format(date2);

    WebElement webElement = driver.findElement(
        By.xpath("//input[@id=\"moe--quote--expiration-date\"]"));

    for (int i = 0; i <= 4; i++) {
      webElement.sendKeys(currentYear);
    }

    webElement.sendKeys(Keys.TAB);
    webElement.sendKeys(currentMonthDay);
  }

  private void sendQuote() {
    Util.printInfo("Click on 'Send quote' cta");
    moePage.click("moeSendQuote");
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    // TODO: add try catch block to validate if error modal loaded
    // Note: R2.0.2 - Quote feature not fully implemented under INT env.
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
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
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
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
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

  @Step("Validate Quote dropdown list from Order view and return its value")
  private String validateQuoteDropdownFromOrderViewAndReturnItsValue() throws MetadataException {
    moePage.clickUsingLowLevelActions("moeRadioButtonOrder");
    Util.sleep(2000);
    Select selection = new Select(driver.findElement(By.id("moe-quotes")));
    int size = selection.getOptions().size();
    Util.printInfo("Number of items: " + size);
    AssertUtils.assertTrue(3 == size);

    selection.selectByIndex(2);
    return selection.getFirstSelectedOption().getAttribute("value");
  }

  @Step("Validate default order view.")
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

  @Step("Add customer info in the quote section and send a quote.")
  private String populateCustomerInfoAndSendQuoteDTC(String emailID, Names names) {
    Util.printInfo("MOE - Send Quote");

    // Open Quote section
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

    setExpirationDate();

    moePage.populateField("moeQuotePrimaryEmail", emailID);
    moePage.populateField("moeQuoteSecondaryEmail", "test-" + emailID);

    sendQuote();

    // Get quote number from the page
    String quoteNumber = moePage.getValueFromGUI("moeQuoteNumber");
    Util.printInfo("Quote number is: " + quoteNumber);

    moePage.waitForPageToLoad();

    Util.printInfo("MOE - Quote sent");

    return quoteNumber;
  }

  @Step("Login to Portal with user account")
  private void loginToPortalWithUserAccount(LinkedHashMap<String, String> data, String emailID,
      String password) {
    // Navigate to Portal, logout from service account session and log back in with user account
    String constructPortalUrl = data.get("cepURL");
    bicTestBase.getUrl(constructPortalUrl);

    if (!GlobalConstants.getENV().equals(BICECEConstants.ENV_INT)) {
      bicTestBase.getUrl(data.get("oxygenLogOut"));
    }

    bicTestBase.loginToOxygen(emailID, password);
  }

  private void navigateToMoeDtcUrl(LinkedHashMap<String, String> data, String guacBaseURL,
      String locale) {
    String guacMoeDTCURL = data.get("guacMoeDTCURL");
    String constructMoeDtcUrl = guacBaseURL + locale + "/" + guacMoeDTCURL;
    System.out.println("constructMoeURL " + constructMoeDtcUrl);
    bicTestBase.getUrl(constructMoeDtcUrl);
  }

  private void navigateToMoeOdmDtcUrl(LinkedHashMap<String, String> data, String guacBaseURL,
      String locale) {
    String guacMoeOdmDtcUrl = data.get("guacMoeOdmDtcResourceURL");
    String constructMoeOdmDtcUrl = guacBaseURL + locale + "/" + guacMoeOdmDtcUrl;
    System.out.println("constructMoeOdmDtcUrl " + constructMoeOdmDtcUrl);
    bicTestBase.getUrl(constructMoeOdmDtcUrl);
  }

  private void proceedToDtcCartSection() throws MetadataException {
    loginToMoe();
    moePage.clickUsingLowLevelActions("moeDtcContinueBtn");
  }

  @Step("Click on Copy Cart link and return the value from clipboard.")
  public String copyCartLinkFromClipboard() throws Exception {
    Util.printInfo("Clicking on 'Copy cart link'.");
    moePage.checkIfElementExistsInPage("moeCopyCartLink", 10);
    moePage.clickUsingLowLevelActions("moeCopyCartLink");
    Util.sleep(2000);

    Util.printInfo("Executing script to read clipboard contents.");
    JavascriptExecutor js = (JavascriptExecutor) driver;
    Object copiedCartLinkValue = js.executeScript(
        "window.cb = navigator.clipboard.readText();return window.cb;");
    Util.printInfo("Clipboard contents: " + copiedCartLinkValue);
    Util.sleep(2000);

    return copiedCartLinkValue.toString();
  }

  @Step("Login to Checkout page as a customer.")
  private void loginToCheckoutWithUserAccount(String emailID, Names names,
      String password, String copyCartLink) {

    // Navigate to Copy Cart URL
    bicTestBase.getUrl(copyCartLink);
    moePage.waitForPageToLoad();

    // Create new user and sign in
    bicTestBase.createBICAccount(names, emailID, password, false);
  }

  @Step("SFDC : Create GUAC MOE ODM opportunity " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createMoeOdmOpty(String optyName, String account,
      String stage, String projectCloseDate, String fulfillment, String plc, String currency, String contact) {
    HashMap<String, String> results = new HashMap<>();

    String opportunityId = addOptyDetailsAndSave(optyName, account, stage,
        projectCloseDate, fulfillment, currency);
    results.put("opportunityId", opportunityId);

    String currentUrl = addPLCToOpportunity(plc);
    results.put("currentOptyUrl", currentUrl);

    String emailID = addContactToOpportunity(contact);
    results.put("contactEmail", emailID);

    return results;
  }

  @Step("SFDC : Create an opportunity")
  private String addOptyDetailsAndSave(String optyName,
      String account, String stage,
      String projectCloseDate, String fulfillment, String currency) {

    try {
      Boolean isNewOpportunityWindowOpen = true;
      int count = 0;

      while (isNewOpportunityWindowOpen) {

        count++;

        if (count > 3) {
          AssertUtils.fail("Unable to create an opportunity.");
        }

        // Finding the iframe for New Opportunity form
        switchToFrame("//input[@class='customInput slds-input CloseDateInput']");

        moePage.populateField("projectCloseDate", projectCloseDate);
        Util.printInfo("entered project close date " + projectCloseDate);
        Util.sleep(2000);

        moePage.click("name");
        moePage.populateField("name", optyName);
        Util.printInfo("entered name : " + optyName);

        Select sel;
        sel = new Select(driver.findElement(By.id("j_id0:j_id25:j_id38:4:j_id47")));
        sel.selectByValue(fulfillment);
        Util.printInfo("Selected Fulfillment : " + fulfillment);
        Util.sleep(1000);

        moePage.clickUsingLowLevelActions("accountLookup");
        Util.printInfo("clicked on account lookup");
        Util.sleep(3000);
        moePage.populateField("accountTextField", account);
        Util.printInfo("entered account name");
        Util.sleep(3000);
        Util.printInfo("entered account " + account);
        moePage.clickUsingLowLevelActions("searchAccount");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath(moePage.getFirstFieldLocator("selectAccount"))));

        moePage.clickUsingLowLevelActions("selectAccount");
        Util.printInfo("selected account name : " + account);
        Util.sleep(3000);

        sel = new Select(driver.findElement(By.id("j_id0:j_id25:j_id38:2:j_id47")));
        sel.selectByValue("Stage 1");
        Util.printInfo("Selected stage : " + stage);
        Util.sleep(1000);

        sel = new Select(driver.findElement(By.id("j_id0:j_id25:j_id38:5:j_id47")));
        sel.selectByValue(currency);
        Util.printInfo("Selected currency : " + currency);
        Util.sleep(1000);

        moePage.click("save");
        Util.printInfo("Clicked on Save button after entering details.");
        moePage.waitForPageToLoad();

        if (moePage.checkIfElementExistsInPage("opportunityError", 20)) {
          Util.printInfo("Fail to create new opportunity. Refreshing the page. Attempt #" + count);
          driver.navigate().refresh();
          moePage.waitForPageToLoad();
        } else {
          Util.printInfo("Opportunity saved successfully.");
          isNewOpportunityWindowOpen = false;
        }
      }
    } catch (Exception e) {
      AssertUtils.fail(e.getMessage() + "Failed to enter opty details.");
    }

    driver.switchTo().defaultContent();
    String optyId = driver.findElement(By.xpath(
            "//span[@class='test-id__field-value slds-form-element__static slds-grow  is-read-only' and contains(., 'A-')]"))
        .getText();

    Util.printInfo("New opportunity created " + optyName + " with the id " + optyId);

    return optyId;
  }

  @Step("Navigate to MOE ODM page with OpptyID" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBasicMoeOdmOptyOrder(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    Map<String, String> address = null;
    // Construct MOE URL with opptyId
    String guacBaseURL = data.get("guacBaseURL");
    String guacMoeOdmResourceURL = data.get("guacMoeOdmResourceURL") + data.get("guacMoeOptyId");
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");

    String constructMoeOdmURLWithOptyId = guacBaseURL + locale + "/" + guacMoeOdmResourceURL;

    Util.printInfo("constructMoeOdmURL " + constructMoeOdmURLWithOptyId);

    // Navigate to Url
    bicTestBase.getUrl(constructMoeOdmURLWithOptyId);
    bicTestBase.setStorageData();

    loginToMoe();

    Util.sleep(5000);

    Util.printInfo("Clicking on cta: Continue");
    moePage.checkIfElementExistsInPage("moeCustomerDetailsContinue", 30);
    moePage.clickUsingLowLevelActions("moeCustomerDetailsContinue");
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");

    if (System.getProperty("usertype").equals("new")) {
      moePage.click("moeModalCloseBtn");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");

      BICTestBase.bicPage.executeJavascript("window.scrollBy(0,1000);");

      // Populate Billing info and save payment profile
      address = bicTestBase.getBillingAddress(data.get(BICECEConstants.ADDRESS));
      String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
      String[] paymentCardDetails = bicTestBase.getPaymentDetails(paymentMethod.toUpperCase())
          .split("@");

      String orderNumber = savePaymentDetailsAndSubmitOrder(data, address, paymentCardDetails);

      results.put(BICConstants.orderNumber, orderNumber);
    } else {
      BICTestBase.bicPage.executeJavascript("window.scrollBy(0,1000);");

      Util.printInfo("Clicking on Agreement checkbox");
      bicTestBase.agreeToTerm();

      Util.printInfo("Clicking on cta: Submit order");
      bicTestBase.submitOrder(data);

      String orderNumber = bicTestBase.getOrderNumber(data);
      results.put(BICConstants.orderNumber, orderNumber);
    }

    return results;
  }

  @Step("Save payment details and submit order")
  private String savePaymentDetailsAndSubmitOrder(LinkedHashMap<String, String> data,
      Map<String, String> address, String[] paymentCardDetails) {

    // INFO: R2.0.2 - We only support credit card right now.
    // For STORE-CA, the UI is set with only cc payment method which default to no tab being visible
    if (moePage.isFieldVisible("creditCardTab")) {
      try {
        moePage.click("creditCardTab");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    bicTestBase.selectPaymentProfile(data, paymentCardDetails, address);

    moePage.click("savePaymentProfile");
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");

    bicTestBase.agreeToTerm();

    bicTestBase.submitOrder(data);

    return bicTestBase.getOrderNumber(data);
  }

  @Step("Place order on MOE ODM page" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBicOrderMoeOdmWithOptyDtc(LinkedHashMap<String, String> data)
      throws Exception {
    HashMap<String, String> results = new HashMap<>();
    String guacBaseURL = data.get("guacBaseURL");
    String guacMoeOdmResourceURL =
        data.get("guacMoeOdmResourceURL") + data.get("guacMoeOptyId");
    String emailID = data.get("sfdcContactEmail");
    String password = ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD));
    Util.printInfo("THE REGION " + data.get(BICECEConstants.LOCALE));
    bicTestBase.navigateToCart(data);

    results.putAll(getBicOrderMoeOdmWithOptyDtc(data, emailID, guacBaseURL, guacMoeOdmResourceURL,
        data.get(BICECEConstants.LOCALE), password));

    results.put(BICECEConstants.emailid, emailID);

    return results;
  }

  @Step("Place order via Copy cart link generated on MOE ODM page" + GlobalConstants.TAG_TESTINGHUB)
  private HashMap<String, String> getBicOrderMoeOdmWithOptyDtc(LinkedHashMap<String, String> data, String emailID,
      String guacBaseURL, String guacMoeOdmResourceURL, String locale, String password)
      throws Exception {
    HashMap<String, String> results = new HashMap<>();
    locale = locale.replace("_", "-");
    String constructGuacMoeOdmURL = guacBaseURL + locale + "/" + guacMoeOdmResourceURL;
    System.out.println("constructGuacMoeOdmURL " + constructGuacMoeOdmURL);
    Map<String, String> address = bicTestBase.getBillingAddress(data);
    String orderNumber = "";
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String oxygenLogOutUrl = data.get("oxygenLogOut");

    Names names = BICTestBase.generateFirstAndLastNames();

    if (System.getProperty("usertype").equals("new")) {
      bicTestBase.createBICAccount(names, emailID, password, false);
      data.putAll(names.getMap());
      data.put(BICECEConstants.emailid, emailID);
    }

    bicTestBase.getUrl(constructGuacMoeOdmURL);
    bicTestBase.setStorageData();

    loginToMoe();

    if (moePage.checkIfElementExistsInPage("moeCustomerDetailsContinue", 10)) {
      Util.printInfo("Customer details continue button found. Clicking on it.");
      moePage.clickUsingLowLevelActions("moeCustomerDetailsContinue");
      moePage.waitForElementToDisappear("moeCustomerDetailsContinue", 10);
      Util.printInfo("Customer details continue button no longer visible.");
    }

    BICTestBase.bicPage.executeJavascript("window.scrollBy(0,800);");

    // INFO: R2.0.2 - We only support credit card right now.
    // For STORE-CA, the UI is set with only cc payment method which default to no tab being visible
    if (moePage.isFieldVisible("creditCardTab")) {
      try {
        moePage.click("creditCardTab");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (System.getProperty("usertype").equals("new")) {
      // Populate Billing info and save payment profile
      String[] paymentCardDetails = bicTestBase.getPaymentDetails(paymentMethod.toUpperCase())
          .split("@");

      bicTestBase.selectPaymentProfile(data, paymentCardDetails, address);

      Util.printInfo("Saving payment profile.");
      moePage.click("savePaymentProfile");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");

    } else {
      emailID = data.get("contactEmail");
      data.put(BICECEConstants.emailid, data.get("contactEmail"));
    }

    String copyCartLink = copyCartLinkFromClipboard();

    Util.printInfo("Log out with Oxygen direct URL: " + oxygenLogOutUrl);
    bicTestBase.getUrl(oxygenLogOutUrl);

    bicTestBase.getUrl(copyCartLink);

    driver.switchTo().frame(0);

    bicTestBase.loginToOxygen(emailID, password);
    Util.sleep(10000);

    Util.printInfo("Scrolling down the page");
    BICTestBase.bicPage.executeJavascript("window.scrollBy(0,1000);");

    if (System.getProperty("usertype").equals("new")) {
      bicTestBase.enterCustomerDetails(address);
      Util.sleep(5000);
    }

    // In case address suggestion is returned, continue button will be displayed.
    if (moePage.checkIfElementExistsInPage("moeCustomerDetailsContinue", 10)) {
      Util.printInfo("Clicking on Continue button after adding the customer details");
      moePage.clickUsingLowLevelActions("moeCustomerDetailsContinue");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    }

    bicTestBase.submitOrder(data);

    orderNumber = bicTestBase.getOrderNumber(data);
    bicTestBase.printConsole(orderNumber, data, address);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    Util.sleep(20000);

    String constructPortalUrl = data.get("cepURL");
    bicTestBase.getUrl(constructPortalUrl);

    return results;
  }

  @Step("Place order via Copy cart link generated on MOE ODM DTC page" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBicOrderMoeOdmDtc(LinkedHashMap<String, String> data)
      throws Exception {
    HashMap<String, String> results = new HashMap<>();
    Map<String, String> address = bicTestBase.getBillingAddress(data);
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");
    String password = ProtectedConfigFile.decrypt(data.get(BICECEConstants.PASSWORD));
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String guacBaseURL = data.get("guacBaseURL");
    navigateToMoeOdmDtcUrl(data, guacBaseURL, locale);
    String oxygenLogOutUrl = data.get("oxygenLogOut");

    bicTestBase.setStorageData();

    loginToMoe();

    AssertUtils.assertTrue(driver
        .findElement(By.xpath("//div[@data-wat-link-section=\"checkout-empty-cart\"]"))
        .isDisplayed());

    Util.printInfo("Add Flex product");
    moePage.click("moeAddFlexProductButton");
    Util.sleep(5000);

    WebElement productLineItem = driver.findElement(
        By.xpath(moePage.getFirstFieldLocator("moeProductLineItem")));
    AssertUtils.assertTrue(
        productLineItem.getText().contains("Flex"));

    String copyCartLink = copyCartLinkFromClipboard();

    Names names = BICTestBase.generateFirstAndLastNames();
    data.putAll(names.getMap());
    String emailID = BICTestBase.generateUniqueEmailID();
    data.put(BICECEConstants.emailid, emailID);

    Util.printInfo("Log out with Oxygen direct URL: " + oxygenLogOutUrl);
    bicTestBase.getUrl(oxygenLogOutUrl);

    loginToCheckoutWithUserAccount(emailID, names, password, copyCartLink);
    Util.sleep(10000);

    productLineItem = driver.findElement(
        By.xpath(moePage.getFirstFieldLocator("moeProductLineItem")));
    AssertUtils.assertTrue(
        productLineItem.getText().contains("Flex"));

    bicTestBase.enterCustomerDetails(address);
    Util.sleep(10000);

    // In case address suggestion is returned, continue button will be displayed.
    if (moePage.checkIfElementExistsInPage("moeCustomerDetailsContinue", 10)) {
      Util.printInfo("Clicking on Continue button after adding the customer details");
      moePage.clickUsingLowLevelActions("moeCustomerDetailsContinue");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    }

    String[] paymentCardDetails = bicTestBase.getPaymentDetails(paymentMethod.toUpperCase())
        .split("@");

    bicTestBase.selectPaymentProfile(data, paymentCardDetails, address);
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");

    Util.printInfo("Clicking on cta: Save");
    moePage.click("savePaymentProfile");
    bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");

    // In case address suggestion is returned, continue button will be displayed.
    if (moePage.checkIfElementExistsInPage("moeCustomerDetailsContinue", 10)) {
      Util.printInfo("Clicking on Continue button after saving payment profile");
      moePage.clickUsingLowLevelActions("moeCustomerDetailsContinue");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    }

    bicTestBase.submitOrder(data);
    String orderNumber = bicTestBase.getOrderNumber(data);
    bicTestBase.printConsole(orderNumber, data, address);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @Step("Create BIC Existing User Order Creation via O2P Cart " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> createBicOrderForReturningUserMoeOdmDtc(LinkedHashMap<String, String> data)
      throws Exception {
    HashMap<String, String> results = new HashMap<>();
    Map<String, String> address = bicTestBase.getBillingAddress(data);
    String emailID = data.get(BICConstants.emailid);
    String locale = data.get(BICECEConstants.LOCALE).replace("_", "-");
    String guacBaseURL = data.get("guacBaseURL");
    navigateToMoeOdmDtcUrl(data, guacBaseURL, locale);
    String oxygenLogOutUrl = data.get("oxygenLogOut");

    if (GlobalConstants.getENV().equals(BICECEConstants.ENV_INT)) {
      loginToMoe();
    }

    AssertUtils.assertTrue(driver
        .findElement(By.xpath("//div[@data-wat-link-section=\"checkout-empty-cart\"]"))
        .isDisplayed());

    Util.printInfo("Add Flex product");
    moePage.click("moeAddFlexProductButton");
    Util.sleep(3000);

    WebElement productLineItem = driver.findElement(
        By.xpath(moePage.getFirstFieldLocator("moeProductLineItem")));
    AssertUtils.assertTrue(
        productLineItem.getText().contains("Flex"));

    String copyCartLink = copyCartLinkFromClipboard();

    updateStorageData();

    Util.printInfo("Log out with Oxygen direct URL: " + oxygenLogOutUrl);
    bicTestBase.getUrl(oxygenLogOutUrl);

    bicTestBase.getUrl(copyCartLink);

    bicTestBase.loginAccount(data);

    bicTestBase.enterCustomerDetails(address);
    Util.sleep(5000);

    // In case address suggestion is returned, continue button will be displayed.
    if (moePage.checkIfElementExistsInPage("moeCustomerDetailsContinue", 10)) {
      Util.printInfo("Clicking on Continue button after adding the customer details");
      moePage.clickUsingLowLevelActions("moeCustomerDetailsContinue");
      bicTestBase.waitForLoadingSpinnerToComplete("loadingSpinner");
    }

    bicTestBase.submitOrder(data);
    String orderNumber = bicTestBase.getOrderNumber(data);
    bicTestBase.printConsole(orderNumber, data, address);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  private void updateStorageData() {
    Util.printInfo("Delete cookies and clear session and local storages.");
    driver.manage().deleteAllCookies();
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("window.sessionStorage.clear();");
    js.executeScript("window.localStorage.clear();");
    driver.navigate().refresh();
    Util.sleep(5000);
    bicTestBase.setStorageData();
  }

  @Step("SFDC : Validating Opportunity state " + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> validateOpportunityStatusInSfdc(LinkedHashMap<String, String> data)
      throws MetadataException {
    HashMap<String, String> results = new HashMap<>();

    // Construct SFDC optyId URL
    String sfdcCurrentOptyUrl = data.get("currentOptyUrl");
    Util.printInfo("SFDC current opportunity URL: " + sfdcCurrentOptyUrl);

    // Navigate to Url
    bicTestBase.getUrl(sfdcCurrentOptyUrl);

    try {
      Boolean isOpportunityOpened = true;
      int attempt = 0;

      while (isOpportunityOpened) {

        attempt++;
        Util.printInfo("Trying to find an Opportunity with closed state. Attempt no " + attempt);

        if (attempt > 5) {
          AssertUtils.fail("Unable to find a closed Opportunity.");
        }

        if (moePage.checkIfElementExistsInPage("optyIdStageWon", 90)) {
          Util.printInfo("Opportunity found with closed state.");
          isOpportunityOpened = false;
        } else {
          Util.printInfo("Opportunity not closed. Refreshing the page.");
          driver.navigate().refresh();
        }
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to find the Opportunity with closed state " + e.getMessage());
    }

    return results;
  }

  @Step("SFDC : Add a contact to an opportunity")
  private String addContactToOpportunity(String contact) {

    Names names = BICTestBase.generateFirstAndLastNames();
    String emailID = BICTestBase.generateUniqueEmailID();

    try {
      if (StringUtils.isNotEmpty(contact)) {

        Boolean isContactMissing = true;
        int attempt = 0;

        while (isContactMissing) {

          attempt++;

          if (attempt > 3) {
            AssertUtils.fail("Unable to add a contact to the Opportunity.");
          }

          Util.printInfo("Clicking on cta: Manage Contact Roles. Attempt no. " + attempt + " to add a contact.");
          moePage.click("manageContactRoles");
          moePage.waitForPageToLoad();

          moePage.checkIfElementExistsInPage("contactRolesHeading", 10);
          moePage.clickUsingLowLevelActions("contactRolesHeading");

          moePage.clickUsingLowLevelActions("addContactRoles");
          Util.printInfo("Clicked on cta: '+ Add 1 more Contact Role'");
          moePage.waitForPageToLoad();

          if (System.getProperty("usertype").equals("new")) {
            Util.printInfo("Associating new contact roles to Opty: " + emailID);
            moePage.checkIfElementExistsInPage("createNewContactLink", 15);
            moePage.clickUsingLowLevelActions("createNewContactLink");
            moePage.checkIfElementExistsInPage("contactFirstNameInput", 15);
            moePage.clickUsingLowLevelActions("contactFirstNameInput");
            moePage.populateField("contactFirstNameInput", names.firstName);
            moePage.clickUsingLowLevelActions("contactLastNameInput");
            moePage.populateField("contactLastNameInput", names.lastName);
            moePage.clickUsingLowLevelActions("contactEmailInput");
            moePage.populateField("contactEmailInput", emailID);
            moePage.clickUsingLowLevelActions("contactPhoneInput");
            moePage.populateField("contactPhoneInput", "1234567890");
            moePage.clickUsingLowLevelActions("contactPreferredLanguageSelect");
            Util.sleep(1000);
            moePage.clickUsingLowLevelActions("contactLanguage");
            Util.sleep(2000);
            moePage.click("saveContactButton");
            bicTestBase.waitForLoadingSpinnerToComplete("sfdcLoadingSpinner");

          } else {
            Util.printInfo("Associating existing contact roles to Opty: " + contact);

            moePage.checkIfElementExistsInPage("contactRolesInput", 10);
            moePage.clickUsingLowLevelActions("contactRolesInput");
            moePage.populateField("contactRolesInput", contact);
            Util.sleep(20000);
            Util.printInfo("Populated input field with contact email");

            WebElement webElement = driver.findElement(
                By.xpath("//input[@class='slds-input input uiInput uiInputText uiInput--default uiInput--input']"));
            webElement.sendKeys(Keys.TAB, Keys.ENTER);
            Util.printInfo("Contact selected from search result");
          }
          moePage.waitForPageToLoad();

          moePage.checkIfElementExistsInPage("contactRolesHeading", 30);
          moePage.clickUsingLowLevelActions("contactRolesHeading");

          moePage.checkIfElementExistsInPage("checkPrimaryContactCheckbox", 10);
          moePage.clickUsingLowLevelActions("checkPrimaryContactCheckbox");
          Util.sleep(2000);

          JavascriptExecutor js = (JavascriptExecutor) driver;
          js.executeScript(
              "document.getElementsByClassName(\"uiInput uiInputCheckbox uiInput--default uiInput--checkbox\")[0].click();");
          Util.printInfo("Primary contact checkbox checked");
          Util.sleep(5000);

          js.executeScript(
              "document.getElementsByClassName(\"slds-button slds-button--brand\")[1].click();");
          Util.printInfo("Clicked on cta: Save");

          bicTestBase.waitForLoadingSpinnerToComplete("sfdcLoadingSpinner");

          if (moePage.checkIfElementExistsInPage("contactSectionTitle", 30)) {
            Util.printInfo("Contact successfully added to the opportunity.");
            isContactMissing = false;
          }
        }
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to assign Contact Roles to MOE ODM Opty." + e.getMessage());
    }

    return emailID;
  }

  @Step("SFDC : Add a plc to an opportunity")
  private String addPLCToOpportunity(String plc) {

    String strUrl = driver.getCurrentUrl();
    Util.printInfo("Opty current URL :: " + strUrl);

    try {
      if (StringUtils.isNotEmpty(plc)) {
        Util.printInfo("Associating Products to Opty: " + plc);

        Dimension defaultDimension = driver.manage().window().getSize();
        Util.printInfo("Default dimension: " + defaultDimension);

        Dimension newDimension = new Dimension(1152, 864);
        driver.manage().window().setSize(newDimension);
        Util.printInfo("New dimension: " + newDimension);

        moePage.checkIfElementExistsInPage("manageProducts", 30);
        moePage.click("manageProducts");
        Util.sleep(10000);

        Util.printInfo("Switch iFrame to click on Add Products Tab");
        switchToFrame("//li[@title='Add Products']/a");
        Util.sleep(5000);

        moePage.populateField("productSearch", plc);

        moePage.clickUsingLowLevelActions("productSearchButton");
        bicTestBase.waitForLoadingSpinnerToComplete("sfdcLoadingSpinner");

        Util.printInfo("Open product found.");
        WebElement openProductFound = moePage.getMultipleWebElementsfromField("openProductFound").get(0);
        moePage.waitForElementVisible(openProductFound, 20);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", openProductFound);
        bicTestBase.waitForLoadingSpinnerToComplete("sfdcLoadingSpinner");
        Util.sleep(5000);

        Util.printInfo("Select Flex checkbox");
        WebElement checkbox = driver.findElement(
            By.xpath(moePage.getFirstFieldLocator("checkbox")));
        moePage.waitForElementVisible(checkbox, 30);
        checkbox.click();
        Util.sleep(2000);

        try {
          Boolean isAlertModalVisible = true;
          int count = 0;
          while (isAlertModalVisible) {
            count++;

            Util.printInfo("Adding Flex product. Attempt no. " + count);

            if (count > 3) {
              AssertUtils.fail("Failed to add Flex product with qty.");
            }

            moePage.clickUsingLowLevelActions("estimatedUnit");
            Util.sleep(2000);

            driver.findElement(By.xpath("//input[@class='slds-input input']"))
                .sendKeys(Keys.chord(Keys.COMMAND, "a"), Keys.BACK_SPACE);
            Util.sleep(2000);

            if (moePage.checkIfElementExistsInPage("alertModal", 100)) {
              Util.printInfo("Alert modal is visible. Closing it.");
              moePage.clickUsingLowLevelActions("okButton");
              Util.sleep(2000);
            }

            Util.printInfo("Type '100' into quantity input field.");
            moePage.sendKeysInTextFieldSlowly("estimatedUnit", "100");
            Util.sleep(3000);

            moePage.checkIfElementExistsInPage("addProductsButton", 20);
            moePage.clickUsingLowLevelActions("addProductsButton");
            bicTestBase.waitForLoadingSpinnerToComplete("sfdcLoadingSpinner");

            if (moePage.checkIfElementExistsInPage("successModal", 45)) {
              Util.printInfo("Success modal is visible.");
              isAlertModalVisible = false;
            }

          }
        } catch (Exception e) {
          Util.printInfo("Flex product added successfully.");
        }

        moePage.clickUsingLowLevelActions("okButton");

        moePage.clickUsingLowLevelActions("close");
        Util.printInfo("Clicked on Close");

        driver.manage().window().setSize(defaultDimension);
        Util.printInfo("Default dimension: " + defaultDimension);

        if (moePage.checkIfElementExistsInPage("subFrameError", 15)) {
          Util.printInfo("Failed to go back to opty view page. Manually navigating to it.");
          bicTestBase.getUrl(strUrl);
          Util.sleep(5000);
        }
      }
    } catch (Exception e) {
      AssertUtils.fail("Failed to assign Product to MOE Opty." + e.getMessage());
    }

    return strUrl;
  }

  @Step("Switch to Frame")
  private void switchToFrame(String elementXPath) {
    for (int i = 0; i < 10; i++) {
      try {
        driver.switchTo().defaultContent();
        driver.switchTo().frame(i);
        Util.printInfo("Switched to iframe " + i);
        driver.findElement(By.xpath(elementXPath))
            .click();
        break;
      } catch (Exception e) {
        e.printStackTrace();
        Util.printInfo("It's not: " + i);
      }
    }
  }

}
