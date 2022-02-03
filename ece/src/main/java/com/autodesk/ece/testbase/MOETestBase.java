package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.BICTestBase.Names;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openqa.selenium.WebDriver;

public class MOETestBase extends ECETestBase {

  private final Page_ moePage;
  private final Map<String, String> testData;
  public WebDriver driver;

  public MOETestBase(GlobalTestBase testbase, LinkedHashMap<String, String> testData) {
    Util.PrintInfo("MOETestBase from ece");
    moePage = testbase.createPage("PAGE_MOE");
    driver = testbase.getdriver();
    this.testData = testData;
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
    String region = data.get(BICECEConstants.REGION);
    String password = data.get(BICECEConstants.PASSWORD);
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    Util.printInfo("THE REGION " + data.get(BICECEConstants.LOCALE));
    getBicTestBase().navigateToCart(data);

    String emailID = getBicTestBase().generateUniqueEmailID();
    String orderNumber = getBicOrderMoe(data, emailID, guacBaseURL, guacMoeResourceURL,
        data.get(BICECEConstants.LOCALE), password, paymentMethod, cepURL);

    results.put(BICConstants.emailid, emailID);
    results.put(BICConstants.orderNumber, orderNumber);

    return results;
  }

  @Step("Navigate to MOE page with OpptyID" + GlobalConstants.TAG_TESTINGHUB)
  public String createBasicMoeOpptyOrder(LinkedHashMap<String, String> data) {
    Map<String, String> address = null;
    //construct moe URL with opptyId
    String guacBaseURL = data.get("guacBaseURL");
    String guacMoeResourceURL = "guacMoeResourceURL";

    String constructMoeURLWithOpptyId =
        guacBaseURL + data.get(BICECEConstants.COUNTRY_DOMAIN) + guacMoeResourceURL;

    System.out.println("constructMoeURL " + constructMoeURLWithOpptyId);

    //navigate to Url
    getBicTestBase().getUrl(constructMoeURLWithOpptyId);

    // Validate that the correct locale has been loaded i.e. the locale on GUAC MOE matches the
    // country value that's in the Opportunity in Salesforce
    //to do

    // login with test user who has sale agent permissions
    loginToMoe();

    //Complete account lookup in 'customer details' section i.e. the customer's email address will show as 'account not found'.
    // Follow through on sending invite email to customer.
    emulateNewUser();

    //Validate that the product that's added by default to GUAC MOE cart matches with the line items
    // that are part of the Opportunity (passed to GUAC as part of get Opty call)
    // To do

    // Validate that the address fields that are pre-filled in the payment section matches the address
    // that's in the Opportunity in salesforce (passed to GUAC as part of get Opty call)
    //to do

    //Populate Billing info and save payment profile
    address = getBicTestBase().getBillingAddress(data.get(BICECEConstants.REGION));
    String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
    String[] paymentCardDetails = getBicTestBase().getPaymentDetails(paymentMethod.toUpperCase())
        .split("@");

    return savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);
  }

  private String getBicOrderMoe(LinkedHashMap<String, String> data, String emailID,
      String guacBaseURL, String guacMoeResourceURL, String locale, String password,
      String paymentMethod, String cepURL) {
    locale = locale.replace("_", "-");
    String constructGuacMoeURL = guacBaseURL + locale + "/" + guacMoeResourceURL;
    System.out.println("constructGuacMoeURL " + constructGuacMoeURL);
    String constructPortalUrl = cepURL;
    Map<String, String> address = null;

    address = getBicTestBase().getBillingAddress(data.get(BICECEConstants.REGION));

    Names names = getBicTestBase().generateFirstAndLastNames();
    getBicTestBase().createBICAccount(names, emailID, password);

    data.putAll(names.getMap());

    String[] paymentCardDetails = getBicTestBase().getPaymentDetails(paymentMethod.toUpperCase())
        .split("@");
    getBicTestBase().debugPageUrl(BICECEConstants.ENTER_PAYMENT_DETAILS);

    // Get Payment details
    getBicTestBase().selectPaymentProfile(data, paymentCardDetails, address);

    // Enter billing details
    if (data.get(BICECEConstants.BILLING_DETAILS_ADDED) != null && !data
        .get(BICECEConstants.BILLING_DETAILS_ADDED).equals(BICECEConstants.TRUE)) {
      getBicTestBase().debugPageUrl(BICECEConstants.ENTER_BILLING_DETAILS);
      getBicTestBase().populateBillingAddress(address, data);
      getBicTestBase().debugPageUrl(BICECEConstants.AFTER_ENTERING_BILLING_DETAILS);
    }

    getBicTestBase().getUrl(constructGuacMoeURL);
    loginToMoe();
    emulateUser(emailID);
    String orderNumber = savePaymentProfileAndSubmitOrder(data, address, paymentCardDetails);

    getBicTestBase()
        .printConsole(constructGuacMoeURL, orderNumber, emailID, address, names.firstName,
            names.lastName,
            paymentMethod);

    // Navigate to Portal, logout from service account session and log back in with user account
    getBicTestBase().getUrl(constructPortalUrl);
    getBicTestBase().loginToOxygen(emailID, password);

    return orderNumber;
  }

  private void loginToMoe() {
    Util.sleep(60000);
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
    moePage.populateField(BICECEConstants.MOE_LOGIN_USERNAME_FIELD, "svc_s_guac@autodesk.com");
    moePage.click("moeLoginButton");
    moePage.waitForField(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD, true, 30000);
    moePage.click(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD);
    moePage.populateField(BICECEConstants.MOE_LOGIN_PASSWORD_FIELD, ";mynFU(,|(97?@`n4X?SPw)s~*|$");
    moePage.click("moeLoginButton");
    moePage.waitForPageToLoad();
    Util.printInfo("Successfully logged into MOE");
  }

  private String savePaymentProfileAndSubmitOrder(LinkedHashMap<String, String> data,
      Map<String, String> address, String[] paymentCardDetails) {
    getBicTestBase().populateBillingAddress(address, data);
    getBicTestBase().selectPaymentProfile(data, paymentCardDetails, address);
    try {
      moePage.clickUsingLowLevelActions("savePaymentProfile");
    } catch (Exception e) {
      e.printStackTrace();
    }
    Util.sleep(5000);
    getBicTestBase().agreeToTerm();

    return getBicTestBase().submitGetOrderNumber(data);
  }

  //the following two methods can be combined to address different user lookup scenarios
  private void emulateUser(String emailID) {
    Util.printInfo("MOE - Emulate User");
    moePage.click("moeAccountLookupEmail");
    moePage.populateField("moeAccountLookupEmail", emailID);
    moePage.click("moeAccountLookupBtn");
    moePage.waitForPageToLoad();
    moePage.click("moeContinueBtn");
    moePage.waitForPageToLoad();
    Util.printInfo("Successfully emulated user");
  }

  private void emulateNewUser() {
    String emailID = getBicTestBase().generateUniqueEmailID();
    Util.printInfo("MOE - Emulate User");
    moePage.click("moeAccountLookupEmail");
    moePage.populateField("moeAccountLookupEmail", emailID);
    moePage.click("moeAccountLookupBtn");
    moePage.waitForPageToLoad();
    //click send account setup invite
    moePage.click("moeSendSetupInviteBtn");
    moePage.waitForPageToLoad();
    //close account setup invite sent  popup modal
    moePage.click("moeModalCloseBtn");
    Util.printInfo("Successfully emulated user");
  }
}
