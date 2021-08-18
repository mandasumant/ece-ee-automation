package com.autodesk.ece.testbase;

import com.autodesk.ece.constants.BICECEConstants;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.google.common.base.Strings;

import io.qameta.allure.Step;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class BICTestBase {

	public static Page_ bicPage = null;
	public WebDriver driver = null;

	public BICTestBase(WebDriver driver, GlobalTestBase testbase) {
		Util.PrintInfo("BICTestBase from core");
		this.driver = driver;
		bicPage = testbase.createPage("PAGE_BIC_CART");
	}

	@Step("Open BIC Url")
	public String openBICUrl(String storeKey, String productID, String url) {

		url = url.replace("@@@@@@", storeKey).replace("******", productID);
		Util.printInfo("URL : " + url);
		getUrl(url);
		Util.sleep(5000);
		if (bicPage.isFieldVisible("viewOnLocalSite")) {
			bicPage.click("cancelPopUp");
			Util.sleep(2000);
			Util.PrintInfo("Getting Popup and clicked on it.");

		}
		Util.PrintInfo("Opened: " + "\n" + "<<<<<<<<<<<<<<<<<<<<<<<" + "\n" + url + "\n" + ">>>>>>>>>>>>>>>>>>>>>>");
		if (bicPage.isFieldVisible("otherLangAbs")) {
			try {
				bicPage.clickUsingLowLevelActions("otherLangAbs");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return url;
	}

	@Step("Generate email id")
	public String generateUniqueEmailID(String storeKey, String timeStamp, String sourceName, String emailDomain) {
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
			case "CA":
				address = "Autodesk@75 Rue Ann@Montreal@H3C 5N5@9916800100@Canada@Quebec";
				break;
			case "EMEA":
				address = "Autodesk@Talbot Way@Birmingham@B10 0HJ@9916800100@United Kingdom";
				break;
			case "JAJP":
				address = "Autodesk@532-0003@Street@81-6-6350-5223";
				break;
			default:
				Util.printError("Check the region selected");
		}

		String[] billingAddress = address.split("@");
		if (region.toLowerCase().equalsIgnoreCase("jajp")) {
			ba = new HashMap<String, String>();
			ba.put("companyNameDR", billingAddress[0]);
			ba.put("postalCodeDR", billingAddress[1]);
			ba.put("addressDR", billingAddress[2]);
			ba.put("phoneNumberDR", billingAddress[3]);
		} else {
			ba = new HashMap<String, String>();
			ba.put("Organization_Name", billingAddress[0]);
			ba.put("Full_Address", billingAddress[1]);
			ba.put("City", billingAddress[2]);
			ba.put("Zipcode", billingAddress[3]);
			ba.put("Phone_Number", getRandomMobileNumber());
			ba.put("Country", billingAddress[5]);
			if (!region.toLowerCase().equalsIgnoreCase("emea")) {
				ba.put("State_Province", billingAddress[6]);
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
		clickLinkRetry("createNewUserGUAC");
		bicPage.waitForField("bic_FN", true, 30000);
		bicPage.click("bic_FN");
		bicPage.populateField("bic_FN", firstName);
		bicPage.waitForField("bic_LN", true, 30000);
		bicPage.populateField("bic_LN", lastName);
		bicPage.populateField("bic_New_Email", emailID);
		bicPage.populateField("bic_New_ConfirmEmail", emailID);
		bicPage.waitForField("bic_Password", true, 30000);
		bicPage.populateField("bic_Password", password);

		try {

			Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("bic_Agree"));
			Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("bic_Agree"));

			if (!bicPage.isFieldVisible("bic_Agree")) {
				Util.sleep(20000);
				Util.printInfo(" Checkbox bic_Agree is visible - " + bicPage.isFieldVisible("bic_Agree"));
				Util.printWarning(" Checkbox bic_Agree is present - " + bicPage.isFieldPresent("bic_Agree"));
				Util.printWarning(" Checkbox bic_Agree is FieldExist - " + bicPage.checkFieldExistence("bic_Agree"));
			}

			checkboxTickJS();
			Util.sleep(20000);

		} catch (Exception e) {
			e.printStackTrace();
			AssertUtils.fail("Unable to click on Create account button in BIC-Cart application");
		}

		Util.printInfo("Successfully clicked on Create user button");

		if (bicPage.isFieldVisible("createAutodeskAccount"))
			AssertUtils.fail("Cart login page is redirection failure");
		else
			Util.PrintInfo("Created account successfully");

		driver.switchTo().defaultContent();
	}

	@Step("Create BIC account")
	public void createBICAccountWithOutPaymentProfile(String emailID, String password) {

		switchToBICCartLoginPage();
		Util.printInfo("Url is loaded and we were able to switch to iFrame");
		// clickLinkRetry("createNewUserGUAC");

		bicPage.click("userIDField");

		bicPage.waitForField("userIDField", true, 30000);
		bicPage.populateField("userIDField", emailID);
		bicPage.click("userIDNextButton");

		bicPage.waitForField("userPassField", true, 30000);
		bicPage.populateField("userPassField", password);
		bicPage.click("passIDSignInButton");

		boolean status = false;
		try {
			status = bicPage.isFieldPresent("getStartedSkipLink")
//					|| bicPage.isFieldPresent("getStartedSkipLink")
//					|| bicPage.isFieldPresent("getStartedSkipLink")
					|| bicPage.checkIfElementExistsInPage("getStartedSkipLink", 60);
		} catch (MetadataException e) {
		}

		if (status)
			bicPage.click("getStartedSkipLink");

		driver.switchTo().defaultContent();
	}

	private void checkboxTickJS() {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("document.getElementById('privacypolicy_checkbox').click()");
			Util.sleep(2000);

			js.executeScript("document.getElementById('btnSubmit').click()");
			Util.sleep(10000);

			if (driver.findElement(By.xpath("//label[@id='optin_checkbox']")).getAttribute("class").contains("checked"))
				Util.printInfo("Option checkbox is already selected..");
			else
				js.executeScript("document.getElementById('optin_checkbox').click()");
			Util.sleep(2000);

			js.executeScript("document.getElementById('bttnAccountVerified').click()");
			Util.sleep(2000);
		} catch (Exception e) {
			AssertUtils.fail("Application Loading issue : Unable to click on privacypolicy_checkbox");
		}
	}

	private void switchToBICCartLoginPage() {
		List<String> elementXpath = bicPage.getFieldLocators("createNewUseriFrame");
		WebElement element = driver.findElement(By.xpath(elementXpath.get(0)));
		driver.switchTo().frame(element);
	}

	private void clickLinkRetry(String xpath) {
		int loop = 0;
		try {
			do {
				bicPage.click(xpath);

				if (loop == 5) {
					System.out.println("Unable to click on link " + xpath + "xpath " + bicPage.getFieldLocators(xpath));
					break;
				}
				loop++;
				Util.sleep(5000);
			} while (bicPage.isFieldVisible(xpath));
		} catch (Exception e) {
			e.printStackTrace();
			Util.PrintInfo("Unable to click on link " + xpath + "xpath " + bicPage.getFieldLocators(xpath));
		}
	}

	@Step("Login BIC account")
	public void loginBICAccount(HashMap<String, String> data) {
		// switchtoiFrame("//iframe[contains(@class,'login-iframe')]");
		System.out.println(bicPage.isFieldPresent("autodeskId"));
		bicPage.click("autodeskId");
		bicPage.waitForField("autodeskId", true, 30000);
		bicPage.populateField("autodeskId", data.get(BICConstants.emailid));
		bicPage.click("userNameNextButton");
		Util.sleep(5000);
		bicPage.click("loginPassword");
		bicPage.waitForField("loginPassword", true, 30000);
		bicPage.populateField("loginPassword", data.get("password"));
		bicPage.clickToSubmit("loginButton", 10000);
		// bicPage.click("loginButton");
		bicPage.waitForPageToLoad();
		Util.sleep(5000);
		boolean status = bicPage.isFieldPresent("getStartedSkipLink") || bicPage.isFieldPresent("getStartedSkipLink")
				|| bicPage.isFieldPresent("getStartedSkipLink");

		if (status)
			bicPage.click("getStartedSkipLink");

		Util.sleep(20000);

		try {
			int count = 0;
			while (driver.findElement(By.xpath("//*[@data-testid=\"addSeats-modal-skip-button\"]")).isDisplayed()) {
				driver.findElement(By.xpath("//*[@data-testid=\"addSeats-modal-skip-button\"]")).click();
				count++;
				Util.sleep(1000);
				if (count == 3)
					break;
				if (count == 2)
					driver.findElement(By.xpath("//*[@data-testid=\"addSeats-modal-skip-button\"]")).sendKeys(Keys.ESCAPE);
				if (count == 1)
					driver.findElement(By.xpath("//*[@data-testid=\"addSeats-modal-skip-button\"]")).sendKeys(Keys.PAGE_DOWN);
				System.out.println("count : " + count);
			}

		} catch (Exception e) {
		}

		Util.printInfo("Successfully logged into Bic");
	}

	@Step("Login to an existing BIC account")
	public void loginAccount(HashMap<String, String> data) {
		switchToBICCartLoginPage();

		bicPage.click("autodeskId");
		bicPage.waitForField("autodeskId", true, 30000);
		bicPage.populateField("autodeskId", data.get(BICConstants.emailid));
		bicPage.click("userNameNextButton");

		bicPage.click("loginPassword");
		bicPage.waitForField("loginPassword", true, 30000);
		bicPage.populateField("loginPassword", data.get("password"));
		bicPage.clickToSubmit("loginButton", 10000);
		bicPage.waitForPageToLoad();

		boolean status = bicPage.isFieldPresent("getStartedSkipLink") || bicPage.isFieldPresent("getStartedSkipLink")
				|| bicPage.isFieldPresent("getStartedSkipLink");

		if (status)
			bicPage.click("getStartedSkipLink");

		bicPage.waitForPageToLoad();
	}

	@Step("Add a seat from the existing subscription popup")
	public void existingSubscriptionAddSeat(HashMap<String, String> data) {
		// Wait for add seats popup
		bicPage.waitForField("guacAddSeats", true, 3000);

		bicPage.clickToSubmit("guacAddSeats", 3000);
		bicPage.waitForPageToLoad();
	}

	@Step("Adding to cart")
	public void subscribeAndAddToCart(HashMap<String, String> data) {
		bicPage.waitForField("guacAddToCart", true, 3000);
		bicPage.clickToSubmit("guacAddToCart", 3000);
		bicPage.waitForPageToLoad();

	}

	public void switchtoiFrame(String iFrame) {
		WebElement element = driver.findElement(By.className(iFrame));
		driver.switchTo().frame(element);
	}

	@Step("Populate billing address")
	public boolean populateBillingAddress(Map<String, String> address, HashMap<String, String> data) {

		boolean status = false;
		try {
			String paymentType = System.getProperty("payment");
			String firstNameXpath = "";
			String lastNameXpath = "";
			Util.sleep(5000);

			if (data.get("paymentType").equalsIgnoreCase(BICConstants.paymentTypePayPal)) {
				firstNameXpath = bicPage.getFirstFieldLocator("firstName").replace("<PAYMENTPROFILE>", "paypal");
				lastNameXpath = bicPage.getFirstFieldLocator("lastName").replace("<PAYMENTPROFILE>", "paypal");
			} else if (data.get("paymentType").equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
				firstNameXpath = bicPage.getFirstFieldLocator("firstName").replace("<PAYMENTPROFILE>", "ach");
				lastNameXpath = bicPage.getFirstFieldLocator("lastName").replace("<PAYMENTPROFILE>", "ach");
			} else {
				firstNameXpath = bicPage.getFirstFieldLocator("firstName").replace("<PAYMENTPROFILE>", "credit-card");
				lastNameXpath = bicPage.getFirstFieldLocator("lastName").replace("<PAYMENTPROFILE>", "credit-card");
			}

			driver.findElement(By.xpath(firstNameXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get("firstname"));

			driver.findElement(By.xpath(lastNameXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(2000);
			driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get("lastname"));

			if (address.size() == 6) {
				status = emeaPopulateBillingDetails(address);
				clickContinueCartBillingPage();
				bicPage.waitForPageToLoad();
			} else {
				status = namerPopulateBillingDetails(address, paymentType);
				// clickContinueCartBillingPage();
			}
			clickOnContinueBtn(paymentType);
		} catch (Exception e) {
			e.printStackTrace();
			debugHTMLPage(e.getMessage());
			AssertUtils.fail("Unable to populate the Billing Address details");
		}
		return status;
	}

	private void debugHTMLPage(String Message) {
		Util.printInfo("-------------"+Message+"----------------"+
				"\n" + " URL :            " + 	driver.getCurrentUrl() + "\n" +
				"\n" + " Page Title :     " + 	driver.getTitle() + "\n" +
//				"\n" + " Page source  :   " + 	driver.getPageSource() +
				"\n"+"-----------------------------");
	}

	public void clickOnContinueBtn(String paymentType) {
		try {
			Util.sleep(2000);
			Util.printInfo("Clicking on Save button...");
			List<WebElement> eles = bicPage.getMultipleWebElementsfromField("continueButton");

			if (paymentType.equalsIgnoreCase(BICConstants.paymentTypePayPal)
				|| paymentType.equalsIgnoreCase(BICConstants.paymentTypeDebitCard))
				eles.get(1).click();
			else
				eles.get(0).click();

			bicPage.waitForPageToLoad();
		} catch (MetadataException e) {
			e.printStackTrace();
			AssertUtils.fail("Failed to click on Save button on billing details page...");
		}
	}

	@SuppressWarnings("static-access")
	private boolean namerPopulateBillingDetails(Map<String, String> address, String paymentType) {
		boolean status = false;
		try {
			Util.printInfo("Adding billing details...");

			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("document.getElementById('checkout--liveagent--close-button').click();");
			String orgNameXpath = "", fullAddrXpath = "", cityXpath = "", zipXpath = "", phoneXpath = "", countryXpath = "",
				stateXpath = "";
			switch (paymentType.toUpperCase()) {
				case BICConstants.paymentTypePayPal:
					orgNameXpath = bicPage.getFirstFieldLocator("Organization_Name").replace("<PAYMENTPROFILE>", "paypal");
					fullAddrXpath = bicPage.getFirstFieldLocator("Full_Address").replace("<PAYMENTPROFILE>", "paypal");
					cityXpath = bicPage.getFirstFieldLocator("City").replace("<PAYMENTPROFILE>", "paypal");
					zipXpath = bicPage.getFirstFieldLocator("Zipcode").replace("<PAYMENTPROFILE>", "paypal");
					phoneXpath = bicPage.getFirstFieldLocator("Phone_Number").replace("<PAYMENTPROFILE>", "paypal");
					countryXpath = bicPage.getFirstFieldLocator("Country").replace("<PAYMENTPROFILE>", "paypal");
					stateXpath = bicPage.getFirstFieldLocator("State_Province").replace("<PAYMENTPROFILE>", "paypal");
					break;
				case BICConstants.paymentTypeDebitCard:
					orgNameXpath = bicPage.getFirstFieldLocator("Organization_Name").replace("<PAYMENTPROFILE>", "ach");
					fullAddrXpath = bicPage.getFirstFieldLocator("Full_Address").replace("<PAYMENTPROFILE>", "ach");
					cityXpath = bicPage.getFirstFieldLocator("City").replace("<PAYMENTPROFILE>", "ach");
					zipXpath = bicPage.getFirstFieldLocator("Zipcode").replace("<PAYMENTPROFILE>", "ach");
					phoneXpath = bicPage.getFirstFieldLocator("Phone_Number").replace("<PAYMENTPROFILE>", "ach");
					countryXpath = bicPage.getFirstFieldLocator("Country").replace("<PAYMENTPROFILE>", "ach");
					stateXpath = bicPage.getFirstFieldLocator("State_Province").replace("<PAYMENTPROFILE>", "ach");
					break;
				default:
					orgNameXpath = bicPage.getFirstFieldLocator("Organization_Name").replace("<PAYMENTPROFILE>", "credit-card");
					fullAddrXpath = bicPage.getFirstFieldLocator("Full_Address").replace("<PAYMENTPROFILE>", "credit-card");
					cityXpath = bicPage.getFirstFieldLocator("City").replace("<PAYMENTPROFILE>", "credit-card");
					zipXpath = bicPage.getFirstFieldLocator("Zipcode").replace("<PAYMENTPROFILE>", "credit-card");
					phoneXpath = bicPage.getFirstFieldLocator("Phone_Number").replace("<PAYMENTPROFILE>", "credit-card");
					countryXpath = bicPage.getFirstFieldLocator("Country").replace("<PAYMENTPROFILE>", "credit-card");
					stateXpath = bicPage.getFirstFieldLocator("State_Province").replace("<PAYMENTPROFILE>", "credit-card");
					break;
			}

			Util.sleep(1000);
			WebDriverWait wait = new WebDriverWait(driver, 60);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(orgNameXpath)));
			status = driver.findElement(By.xpath(orgNameXpath)).isDisplayed();

			if (status == false)
				AssertUtils.fail("Organization_Name not available.");

			driver.findElement(By.xpath(orgNameXpath)).click();
			Util.sleep(1000);
			driver.findElement(By.xpath(orgNameXpath))
				.sendKeys(new RandomStringUtils().random(5, true, true) + address.get("Organization_Name"));

			driver.findElement(By.xpath(orgNameXpath)).click();
			Util.sleep(1000);
			driver.findElement(By.xpath(fullAddrXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(fullAddrXpath)).sendKeys(address.get("Full_Address"));

			driver.findElement(By.xpath(cityXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(cityXpath)).sendKeys(address.get("City"));

			driver.findElement(By.xpath(zipXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(zipXpath)).sendKeys(address.get("Zipcode"));

			driver.findElement(By.xpath(phoneXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(phoneXpath)).sendKeys("2333422112");

			WebElement countryEle = driver.findElement(By.xpath(countryXpath));
			Select selCountry = new Select(countryEle);
			selCountry.selectByVisibleText(address.get("Country"));
			Util.sleep(1000);

			driver.findElement(By.xpath(stateXpath)).sendKeys(address.get("State_Province"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Util.printTestFailedMessage("namerPopulateBillingDetails");
			AssertUtils.fail("Unable to Populate Billing Details");
		}
		return status;
	}

	private boolean emeaPopulateBillingDetails(Map<String, String> address) {
		Util.sleep(3000);
		boolean status = false;
		try {
			status = bicPage.waitForElementVisible(
				bicPage.getMultipleWebElementsfromField("Organization_NameEMEA").get(0), 60000);
		} catch (MetadataException e) {
			AssertUtils.fail("Organization_NameEMEA is not displayed on page...");
		}
		bicPage.populateField("Organization_NameEMEA", address.get("Organization_Name"));
		Util.sleep(1000);
		bicPage.populateField("Full_AddressEMEA", address.get("Full_Address"));
		Util.sleep(2000);
		bicPage.populateField("CityEMEA", address.get("City"));
		Util.sleep(3000);
		bicPage.populateField("ZipcodeEMEA", address.get("Zipcode"));
		Util.sleep(1000);
		bicPage.populateField("Phone_NumberEMEA", getRandomMobileNumber());
		Util.sleep(1000);
		bicPage.populateField("CountryEMEA", address.get("Country"));
		Util.sleep(3000);
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

		bicPage.waitForField("creditCardNumberFrame", true, 30000);
		// driver.switchTo().defaultContent();
		try {
			WebElement creditCardNumberFrame = bicPage.getMultipleWebElementsfromField("creditCardNumberFrame").get(0);
			WebElement expiryDateFrame = bicPage.getMultipleWebElementsfromField("expiryDateFrame").get(0);
			WebElement securityCodeFrame = bicPage.getMultipleWebElementsfromField("securityCodeFrame").get(0);

			driver.switchTo().frame(creditCardNumberFrame);
			Util.printInfo("Entering card number : " + paymentCardDetails[0]);
			Util.sleep(2000);
			sendKeysAction("CardNumber", paymentCardDetails[0]);
			driver.switchTo().defaultContent();
			Util.sleep(2000);

			driver.switchTo().frame(expiryDateFrame);
			Util.printInfo("Entering Expiry date : " + paymentCardDetails[1] + "/" + paymentCardDetails[2]);
			Util.sleep(2000);
			sendKeysAction("expirationPeriod", paymentCardDetails[1] + paymentCardDetails[2]);
			driver.switchTo().defaultContent();
			Util.sleep(2000);

//			sendKeysAction("ExpirationMonth", paymentCardDetails[1]);
//			sendKeysAction("ExpirationYear", paymentCardDetails[2]);

			driver.switchTo().frame(securityCodeFrame);
			Util.printInfo("Entering seciruty code : " + paymentCardDetails[3]);
			Util.sleep(2000);
			sendKeysAction("PAYMENTMETHOD_SECURITY_CODE", paymentCardDetails[3]);
			driver.switchTo().defaultContent();
		} catch (MetadataException e) {
			e.printStackTrace();
			AssertUtils.fail("Unable to enter Card details to make payment");
		}
		Util.sleep(20000);
	}

	@Step("Populate Direct Debit payment details")
	public void populateACHPaymentDetails(String[] paymentCardDetails) {

		bicPage.waitForField("creditCardNumberFrame", true, 30000);
//		driver.switchTo().defaultContent();
		try {
			Util.printInfo("Clicking on Direct Debit ACH tab...");
			bicPage.clickUsingLowLevelActions("directDebitACHTab");

			Util.printInfo("Waiting for Direct Debit ACH Header...");
			bicPage.waitForElementVisible(bicPage.getMultipleWebElementsfromField("directDebitHead").get(0), 10);

			Util.printInfo("Entering Direct Debit ACH Account Number : " + paymentCardDetails[0]);
			bicPage.populateField("achAccNumber", paymentCardDetails[0]);

			Util.printInfo("Entering Direct Debit ACH Routing Number : " + paymentCardDetails[0]);
			bicPage.populateField("achRoutingNumber", paymentCardDetails[1]);
		} catch (MetadataException e) {
			e.printStackTrace();
			AssertUtils.fail("Unable to enter Direct Debit details to make payment");
		}
		Util.sleep(20000);
//		return submitGetOrderNumber();
	}

	@Step("Add Paypal Payment Details")
	public void populatePaymentDetails(HashMap<String, String> data) {
		Util.printInfo("Switching to latest window...");
		String parentWindow = driver.getWindowHandle();

		try {
			Util.printInfo("Clicking on Paypal payments tab...");
			bicPage.clickUsingLowLevelActions("paypalPaymentTab");

			Util.printInfo("Clicking on Paypal checkout tab...");
			bicPage.waitForElementVisible(bicPage.getMultipleWebElementsfromField("paypalPaymentHead").get(0), 10);
			bicPage.selectFrame("paypalCheckoutOptionFrame");
			bicPage.clickUsingLowLevelActions("paypalCheckoutBtn");

			Set<String> windows = driver.getWindowHandles();
			for (String window : windows)
				driver.switchTo().window(window);

			driver.manage().window().maximize();
			bicPage.waitForPageToLoad();
			bicPage.waitForElementToDisappear("paypalPageLoader", 30);

			String title = driver.getTitle();
			AssertUtils.assertTrue(title.contains("PayPal"), "Current title [" + title + "] does not contains keyword : PayPal");

			Util.printInfo("Checking Accept cookies button and clicking on it...");
			if (bicPage.checkIfElementExistsInPage("paypalAcceptCookiesBtn", 10))
				bicPage.clickUsingLowLevelActions("paypalAcceptCookiesBtn");

			Util.printInfo("Entering paypal user name [" + data.get("paypalUser") + "]...");
			bicPage.waitForElementVisible(bicPage.getMultipleWebElementsfromField("paypalUsernameField").get(0), 10);
			bicPage.populateField("paypalUsernameField", data.get("paypalUser"));

			Util.printInfo("Entering paypal password...");
			bicPage.populateField("paypalPasswordField", data.get("paypalSsap"));

			Util.printInfo("Clicking on login button...");
			bicPage.clickUsingLowLevelActions("paypalLoginBtn");
			bicPage.waitForElementToDisappear("paypalPageLoader", 30);
			Util.sleep(5000);

			Util.printInfo("Checking Accept cookies button and clicking on it...");
			if (bicPage.checkIfElementExistsInPage("paypalAcceptCookiesBtn", 10))
				bicPage.clickUsingLowLevelActions("paypalAcceptCookiesBtn");

			Util.printInfo("Selecting paypal payment option " + data.get("paypalPaymentType"));
			String paymentTypeXpath = bicPage.getFirstFieldLocator("paypalPaymentOption").replace("<PAYMENTOPTION>",
				data.get("paypalPaymentType"));
			driver.findElement(By.xpath(paymentTypeXpath)).click();

			bicPage.executeJavascript("window.scrollBy(0,1000);");
			Util.printInfo("Clicking on agree and continue button...");
			bicPage.clickUsingLowLevelActions("paypalAgreeAndContBtn");
			Util.sleep(10000);

			driver.switchTo().window(parentWindow);
			Util.sleep(5000);
			Util.printInfo("Paypal Payment success msg : " + bicPage.getTextFromLink("paypalPaymentConfirmation"));

			if (bicPage.checkIfElementExistsInPage("paypalPaymentConfirmation", 10))
				Util.printInfo("Paypal Payment is successfully added...");
			else
				AssertUtils.fail("Failed to add paypal payment profile...");
		} catch (MetadataException e) {
			e.printStackTrace();
			AssertUtils.fail("Unable to enter paypal details to make payment...");
		}
		Util.sleep(20000);
	}

	public void selectPaymentProfile(HashMap<String, String> data, String[] paymentCardDetails) {
//		String orderNumber = "";
		try {
			Util.printInfo("Selecting payment profile : " + data.get("paymentType"));
			switch (data.get("paymentType").toUpperCase()) {
				case BICConstants.paymentTypePayPal:
					populatePaymentDetails(data);
					break;
				case BICConstants.paymentTypeDebitCard:
					populateACHPaymentDetails(paymentCardDetails);
					break;
				default:
					populatePaymentDetails(paymentCardDetails);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			AssertUtils.fail("Failed to select payment profile...");
		}
//		return orderNumber;
	}

	private String submitGetOrderNumber() {
		int count = 0;
		debugHTMLPage(" Step 1 wait for SubmitOrderButton");
		while (!bicPage.waitForField("SubmitOrderButton", true, 60000)) {
			Util.sleep(20000);
			count++;
			if (count > 3)
				break;
			if (count > 2)
				driver.navigate().refresh();
		}

		debugHTMLPage(" Step 2 wait for SubmitOrderButton");

		try {
			if (System.getProperty("payment").equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));

				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("document.getElementById('mandate-agreement').click()");

				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));
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
			Util.printMessage("CONTINUE_CHECKOUT_Modal is not present");
		}

		try {
			bicPage.clickUsingLowLevelActions("SubmitOrderButton");
			bicPage.waitForPageToLoad();
		} catch (Exception e) {
			e.printStackTrace();
			debugHTMLPage(e.getMessage());
			AssertUtils.fail("Failed to click on Submit button...");
		}
		String orderNumber = null;
		debugHTMLPage(" Step 3 Check order Number is Null");
		bicPage.waitForPageToLoad();

		try {
			if (driver.findElement(By.xpath("//*[(text()='Order Processing Problem')]")).isDisplayed())
				Util.printInfo("Order Processing Problem");
			AssertUtils.fail("Unable to place BIC order : " + "Order Processing Problem");
		} catch (Exception e) {
			Util.printMessage("Great! Export Compliance issue is not present");
		}

		try {
			if (driver.findElement(By.xpath("//h5[@class='checkout--order-confirmation--invoice-details--export-compliance--label wd-uppercase']"))
				.isDisplayed())
				Util.printInfo("Export compliance issue is present");
			AssertUtils.fail("Unable to place BIC order : " + "Export compliance issue is present");
		} catch (Exception e) {
			Util.printMessage("Great! Export Compliance issue is not present");
		}
		debugHTMLPage(" Step 3a Check order Number is Null");

		try {
			orderNumber = driver.findElement(By.xpath("//h5[.='Order Number']/..//p")).getText();
		} catch (Exception e) {
			debugHTMLPage(" Step 4 Check order Number is Null");
		}

		try {
			orderNumber = driver.findElement(By.xpath("//h5[.='注文番号：']/..//p")).getText();
		} catch (Exception e) {
			debugHTMLPage(" Step 4 Check order Number is Null for JP");
		}

		debugHTMLPage(" Step 4a Check order Number is Null");

		if (orderNumber == null) {
			try {
				orderNumber = driver.findElement(By.xpath("//h5[.='Order Number:']/..//p")).getText();
			} catch (Exception e) {
				debugHTMLPage(" Step 5 Check order Number is Null");
			}
		}

		if (orderNumber == null) {
			try {
				orderNumber = driver.findElement(By.xpath("//h5[.='注文番号：']/..//p")).getText();
			} catch (Exception e) {
				debugHTMLPage(" Step 5 Check order Number is Null for JP");
			}
		}

		debugHTMLPage(" Step 5a Check order Number is Null");

		if (orderNumber == null) {
			try {
				debugHTMLPage(" Step 6 Assert order Number is Null");

				orderNumber = driver.findElement(By.xpath("//h2[text()='Order Processing Problem']")).getText();
				debugHTMLPage("");
				AssertUtils.fail("Unable to place BIC order : " + orderNumber);
			} catch (Exception e) {
				debugHTMLPage(" Step 7 Assert order Number is Null");
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

	public void printConsole(String Url, String OrderNumber, String emailID, Map<String, String> address,
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

	@SuppressWarnings("static-access")
	@Step("Create BIC Order via Cart " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createBICOrder(LinkedHashMap<String, String> data) {
		HashMap<String, String> results = new HashMap<>();
		String storeKey = data.get("storeKey");
		String productID = data.get("productID");
		String url = data.get("url");
		String userType = data.get("userType");
		String emailDomain = data.get("emailDomain");
		String sourceName = data.get("sourceName");
//		String addressUS = data.get("Cart_Address_US");
		String region = data.get("region");
		String password = data.get("password");
		String paymentMethod = data.get("paymentMethod");

		openBICUrl(storeKey, productID, url);

		String emailID = null, firstName = null, lastName = null, orderNumber = null;
		String randomString = new RandomStringUtils().random(6, true, false);

		switch (userType) {
			case "newUser":
				// Generate random New Email
				String timeStamp = randomString;
				emailID = generateUniqueEmailID(storeKey, timeStamp, sourceName, emailDomain);
				// Based on region get address
				Map<String, String> address = getBillingAddress(region);

				firstName = "QAauto" + randomString;
				Util.printInfo("firstName :: " + firstName);

				lastName = "last" + randomString;
				Util.printInfo("lastName :: " + lastName);

				createBICAccount(firstName, lastName, emailID, password);

				String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");
				selectPaymentProfile(data, paymentCardDetails);

				// Entire address
				populateBillingAddress(address, data);
				orderNumber = submitGetOrderNumber();

				Util.printInfo(orderNumber);
				orderNumber = orderNumber.split(":")[1].replace("Order Number:", "");
				System.out.println(orderNumber);
//				printConsole(url, orderNumber, emailID, address, firstName, lastName,
//				paymentMethod);
				break;
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createGUACBICOrderUS(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String productID = "";
		String quantity = "";
		String guacResourceURL = data.get("guacResourceURL");
		String userType = data.get("userType");
//		String addressUS = data.get("Cart_Address_US");
		String region = data.get("languageStore");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");

		if (System.getProperty("sku").contains("default")) {
			productID = data.get("productID");
		} else {
			String sku = System.getProperty("sku");
			productID = sku.split(":")[0];
			quantity = sku.split(":")[1];
		}

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
			emailID = System.getProperty("email");
			String O2ID = getO2ID(data, emailID);
			// New user to be created
			if ((Strings.isNullOrEmpty(O2ID))) {
				orderNumber = createBICOrder(data, emailID, guacBaseURL, productID, quantity, guacResourceURL, region, password, paymentMethod);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(12, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = createBICOrder(data, emailID, guacBaseURL, productID, quantity, guacResourceURL, region, password, paymentMethod);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place GUAC Dot Com Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createGUACBICOrderDotCom(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseDotComURL = data.get("guacDotComBaseURL");
		String productName = data.get("productName");
		String term = "";
		String quantity = "";
		String guacOverviewResourceURL = data.get("guacOverviewTermResource");

		String userType = data.get("userType");
//		String addressUS = data.get("Cart_Address_US");
		String region = data.get("languageStore");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
			emailID = System.getProperty("email");
			String O2ID = getO2ID(data, emailID);
			// New user to be created
			if ((Strings.isNullOrEmpty(O2ID))) {
				orderNumber = createBICOrderDotCom(data, emailID, guacBaseDotComURL, guacOverviewResourceURL, productName, term,
						region, quantity, password, paymentMethod);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(12, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = createBICOrderDotCom(data, emailID, guacBaseDotComURL, guacOverviewResourceURL, productName, term,
					region, quantity, password, paymentMethod);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createGUACBICOrderPromoCode(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String productID = "";
		String quantity = "";
		String guacResourceURL = data.get("guacResourceURL");
		String userType = data.get("userType");
//		String addressUS = data.get("Cart_Address_US");
		String region = data.get("languageStore");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");
		String promocode = System.getProperty("promocode");

		if (System.getProperty("sku").contains("default")) {
			productID = data.get("productID");
		} else {
			String sku = System.getProperty("sku");
			productID = sku.split(":")[0];
			quantity = sku.split(":")[1];
		}

		// While picking the default value we are also overriding the Price ID to match
		// with the promo code

		try {
			System.out.println(Strings.isNullOrEmpty(promocode));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean promoAvailable = !(Strings.isNullOrEmpty(promocode));
		boolean skuAvailable = !(System.getProperty("sku").contains("default"));

		if (!(promoAvailable && skuAvailable)) {
			promocode = "GUACPROMO";
			productID = "27125";
		}

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
			emailID = System.getProperty("email");
			String O2ID = getO2ID(data, emailID);
			// New user to be created
			if ((Strings.isNullOrEmpty(O2ID))) {
				orderNumber = getBICOrderPromoCode(data, emailID, guacBaseURL, productID, guacResourceURL, region, password,
						paymentMethod, promocode);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(13, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = getBICOrderPromoCode(data, emailID, guacBaseURL, productID, guacResourceURL, region, password, paymentMethod, promocode);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		results.put("priceBeforePromo", data.get("priceBeforePromo"));
		results.put("priceAfterPromo", data.get("priceAfterPromo"));
		return results;
	}

	public void disableChatSession() {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript(String.format("window.sessionStorage.setItem(\"nonsensitiveHasProactiveChatLaunched\",\"true\");"));
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			System.out.println("test");
		}
	}

	private String createBICOrder(LinkedHashMap<String, String> data, String emailID, String guacBaseURL,
			String productID, String quantity, String guacResourceURL, String region, String password, String paymentMethod)

	{
		String orderNumber;
		String constructGuacURL = guacBaseURL + region + guacResourceURL + productID + "[qty:" + quantity + "]";
		System.out.println("constructGuacURL " + constructGuacURL);
		String firstName = null, lastName = null;
		Map<String, String> address = null;

		getUrl(constructGuacURL);
		disableChatSession();
		checkCartDetailsError();

		firstName = null;
		lastName = null;
		String randomString = RandomStringUtils.random(6, true, false);

		region = region.replace("/", "").replace("-", "");
		address = getBillingAddress(region);
		String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");

		acceptCookiesAndUSSiteLink();

		firstName = "FN" + randomString;
		Util.printInfo("firstName :: " + firstName);
		lastName = "LN" + randomString;
		Util.printInfo("lastName :: " + lastName);
		createBICAccount(firstName, lastName, emailID, password);

		data.put("firstname", firstName);
		data.put("lastname", lastName);

		debugHTMLPage("Entire Payment details");
		// Get Payment details
		selectPaymentProfile(data, paymentCardDetails);
		// Entire billing details
		debugHTMLPage("Entire billing details");

		populateBillingAddress(address, data);
		debugHTMLPage("After entering billing details");

		try {
			if (paymentMethod.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));

				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("document.getElementById('mandate-agreement').click()");

				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		orderNumber = submitGetOrderNumber();

		// Check to see if EXPORT COMPLIANCE or Null
		validateBicOrderNumber(orderNumber);
		printConsole(constructGuacURL, orderNumber, emailID, address, firstName, lastName, paymentMethod);

		return orderNumber;
	}

	private String createBICOrderDotCom(LinkedHashMap<String, String> data, String emailID, String guacDotComBaseURL,
			String guacOverviewResourceURL, String productName, String term, String region, String quantity, String password,
			String paymentMethod) {
		String orderNumber;
		String constructGuacDotComURL = guacDotComBaseURL + productName + guacOverviewResourceURL;

		System.out.println("constructGuacDotComURL " + constructGuacDotComURL);
		String firstName = null, lastName = null;
		Map<String, String> address = null;
		getUrl(constructGuacDotComURL);
		disableChatSession();
		checkCartDetailsError();
		subscribeAndAddToCart(data);
		firstName = null;
		lastName = null;
		String randomString = RandomStringUtils.random(6, true, false);

		region = region.replace("/", "").replace("-", "");
		address = getBillingAddress(region);
		String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");

		acceptCookiesAndUSSiteLink();

		firstName = "FN" + randomString;
		Util.printInfo("firstName :: " + firstName);
		lastName = "LN" + randomString;
		Util.printInfo("lastName :: " + lastName);
		createBICAccount(firstName, lastName, emailID, password);

		data.put("firstname", firstName);
		data.put("lastname", lastName);

		debugHTMLPage("Entire Payment details");
		// Get Payment details
		selectPaymentProfile(data, paymentCardDetails);
		// Entire billing details
		debugHTMLPage("Entire billing details");

		populateBillingAddress(address, data);
		debugHTMLPage("After entering billing details");

		try {
			if (paymentMethod.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));

				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("document.getElementById('mandate-agreement').click()");

				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		orderNumber = submitGetOrderNumber();

		// Check to see if EXPORT COMPLIANCE or Null
		validateBicOrderNumber(orderNumber);
		printConsole(constructGuacDotComURL, orderNumber, emailID, address, firstName, lastName, paymentMethod);

		return orderNumber;
	}

	private String getBICOrderPromoCode(LinkedHashMap<String, String> data, String emailID, String guacBaseURL, String productID,
			String guacResourceURL, String region, String password, String paymentMethod, String promocode) {
		String orderNumber;
		String constructGuacURL = guacBaseURL + region + guacResourceURL + productID;
		System.out.println("constructGuacURL " + constructGuacURL);
		String firstName = null, lastName = null;
		Map<String, String> address = null;

		getUrl(constructGuacURL);
		disableChatSession();
		checkCartDetailsError();

		firstName = null;
		lastName = null;
		String randomString = RandomStringUtils.random(6, true, false);

		region = region.replace("/", "").replace("-", "");
		address = getBillingAddress(region);
		String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");

		acceptCookiesAndUSSiteLink();

		firstName = "FN" + randomString;
		Util.printInfo("firstName :: " + firstName);
		lastName = "LN" + randomString;
		Util.printInfo("lastName :: " + lastName);
		createBICAccount(firstName, lastName, emailID, password);

		data.put("firstname", firstName);
		data.put("lastname", lastName);

		debugHTMLPage("Entire Payment details");

		// Enter Promo Code
		populatePromoCode(promocode, data);

		// Get Payment details
		selectPaymentProfile(data, paymentCardDetails);

		// Entire billing details
		debugHTMLPage("Entire billing details");

		populateBillingAddress(address, data);
		debugHTMLPage("After entering billing details");

		try {
			if (paymentMethod.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));

				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("document.getElementById('mandate-agreement').click()");

				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		orderNumber = submitGetOrderNumber();

		// Check to see if EXPORT COMPLIANCE or Null
		validateBicOrderNumber(orderNumber);
		printConsole(constructGuacURL, orderNumber, emailID, address, firstName, lastName, paymentMethod);

		return orderNumber;
	}

	private void populatePromoCode(String promocode, LinkedHashMap<String, String> data) {
		System.out.println();
		String priceBeforePromo = null;
		String priceAfterPromo = null;

		try {
			if (driver.findElement(By.xpath("//h2[contains(text(),\"just have a question\")]")).isDisplayed()) {
				bicPage.clickUsingLowLevelActions("promocodePopUpThanksButton");
			}

			priceBeforePromo = bicPage.getValueFromGUI("promocodeBeforeDiscountPrice").trim();
			Util.printInfo("Step : Entering promocode " + promocode + "\n" + " priceBeforePromo : " + priceBeforePromo);

			bicPage.clickUsingLowLevelActions("promocodeLink");
			bicPage.clickUsingLowLevelActions("promocodeInput");
			bicPage.populateField("promocodeInput", promocode);
			bicPage.clickUsingLowLevelActions("promocodeSubmit");
			Util.sleep(5000);
			priceAfterPromo = bicPage.getValueFromGUI("promocodeAfterDiscountPrice").trim();

			Util.printInfo("----------------------------------------------------------------------");
			Util.printInfo("\n" + " priceBeforePromo :  " + priceBeforePromo + "\n" + " priceAfterPromo : " + priceAfterPromo);
			Util.printInfo("----------------------------------------------------------------------");

		} catch (Exception e) {
			Util.printTestFailedMessage("Unable to enter the Promocode : " + promocode + "\n" + e.getMessage());
		} finally {
			if (priceAfterPromo.equalsIgnoreCase(priceBeforePromo)) {
				AssertUtils.fail("Even after applying the PromoCode, there is not change in the Pricing" + "\n"
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
			o2ID = os.getOxygenID(emailID, System.getProperty("password")).toString();
			data.put(BICConstants.oxygenid, o2ID);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o2ID;
	}

	private String validateBicOrderNumber(String orderNumber) {
		Util.printInfo(orderNumber);
		if (!((orderNumber.equalsIgnoreCase("EXPORT COMPLIANCE")) || (orderNumber.equalsIgnoreCase("輸出コンプライアンス")))
				|| (orderNumber.equalsIgnoreCase("null")))
			orderNumber = orderNumber.trim();
		else {
			Util.printTestFailedMessage(" Cart order " + orderNumber);
			AssertUtils.fail(" Cart order " + orderNumber);
		}
		return orderNumber;
	}

	private void checkCartDetailsError() {
		Util.printInfo("Checking Cart page error...");
		try {
			if (driver.findElement(By.xpath("//*[@id=\"register_link\"]")).isDisplayed()) {
				System.out.println("Page is loaded");
			} else if (driver.findElement(By.xpath("//div[@data-error-code='FETCH_AMART_HTTP_CLIENT_ERROR']")).isDisplayed()) {
				Util.printTestFailedMessage("Error message is displayed while loading Checkout Cart");
				String errorMsg = driver.findElement(By.xpath("//div[@data-error-code='FETCH_AMART_HTTP_CLIENT_ERROR']")).getText();
				AssertUtils.fail(errorMsg);
			} else if (driver.findElement(By.xpath("//div[@data-error-code='FETCH_DR_HTTP_CLIENT_ERROR']")).isDisplayed()) {
				Util.printTestFailedMessage("Error message is displayed while loading Commerce Cart");
				String errorMsg = driver.findElement(By.xpath("//div[@data-error-code='FETCH_DR_HTTP_CLIENT_ERROR']")).getText();
				AssertUtils.fail(errorMsg);
			}
		} catch (Exception e) {
			Util.printInfo("No error on cart page while navigating...");
		}
	}

	@SuppressWarnings("unused")
	@Step("Create BIC Hybrid Order via Cart " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createBICHybridOrder(LinkedHashMap<String, String> data) {
		String orderNumber;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String region = data.get("US");
		String guacResourceURL = data.get("guacResourceURL");
		String productID = data.get("bicNativePriceID");
		String paymentMethod = data.get("paymentMethod");
		String constructGuacURL = guacBaseURL + region + guacResourceURL + productID;
		System.out.println("constructGuacURL " + constructGuacURL);

		getUrl(constructGuacURL);
		disableChatSession();
		checkCartDetailsError();
		acceptCookiesAndUSSiteLink();

		switchToBICCartLoginPage();
		loginBICAccount(data);

		if (data.get("paymentType").equalsIgnoreCase(BICConstants.paymentTypePayPal))
			populatePaymentDetails(data);
		else if (data.get("paymentType").equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
			if (bicPage.isChecked("achCheckBox") == false) {
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("document.getElementById('mandate-agreement').click()");
			} else
				Util.printInfo("ACH Debit card checkbox is Checked");
		}

//		region = region.replace("/", "").replace("-", "");
//		Map<String, String> address = billingAddress(region);
//		populateBillingAddress(address, data);
		orderNumber = submitGetOrderNumber();
		validateBicOrderNumber(orderNumber);
		Util.printInfo("OrderNumber  :: " + orderNumber);

//		printConsole(constructGuacURL, orderNumber, "", null, "", "", paymentMethod);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@SuppressWarnings("unused")
	@Step("Create BIC Existing User Order Creation via Cart " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createBICReturningUser(LinkedHashMap<String, String> data) {
		String orderNumber;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String region = data.get("US");
		String guacResourceURL = data.get("guacResourceURL");
		String productID = data.get("bicNativePriceID");
		String paymentMethod = data.get("paymentMethod");
		String constructGuacURL = guacBaseURL + region + guacResourceURL + productID;
		System.out.println("constructGuacURL " + constructGuacURL);

		getUrl(constructGuacURL);
		disableChatSession();
		checkCartDetailsError();
		acceptCookiesAndUSSiteLink();

		switchToBICCartLoginPage();
		loginBICAccount(data);
		orderNumber = submitGetOrderNumber();
		validateBicOrderNumber(orderNumber);
		Util.printInfo("OrderNumber  :: " + orderNumber);

//		printConsole(constructGuacURL, orderNumber, "", null, "", "", paymentMethod);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@Step("Create BIC Existing User Order Creation via Cart and add seat" + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createBic_ReturningUserAddSeat(LinkedHashMap<String, String> data) {
		String orderNumber;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String region = data.get("US");
		String guacResourceURL = data.get("guacResourceURL");
		String productID = data.get("bicNativePriceID");
		String constructGuacURL = guacBaseURL + region + guacResourceURL + productID;

		// Go to checkout with a product that was already added
		getUrl(constructGuacURL);
		disableChatSession();
		checkCartDetailsError();
		acceptCookiesAndUSSiteLink();

		// Login to an existing account and add seats
		loginAccount(data);
		existingSubscriptionAddSeat(data);
		orderNumber = submitGetOrderNumber();
		validateBicOrderNumber(orderNumber);
		Util.printInfo("OrderNumber  :: " + orderNumber);

		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@Step("Subscription : Subscription Validation" + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> getPurchaseOrderDetails(String purchaseOrderAPIresponse) {
		HashMap<String, String> results = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			StringBuilder xmlStringBuilder = new StringBuilder();
			xmlStringBuilder.append(purchaseOrderAPIresponse);
			ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
			Document doc = builder.parse(input);
			Element root = doc.getDocumentElement();
			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
			String origin = root.getAttribute("origin");
			System.out.println("origin : " + origin);
			System.out.println("storeExternalKey : " + root.getAttribute("storeExternalKey"));

			String orderState = doc.getElementsByTagName("orderState").item(0).getTextContent();
			System.out.println("orderState :" + orderState);

			String subscriptionId = null;
			try {
				// Native order response
				subscriptionId = doc.getElementsByTagName("offeringResponse").item(0).getAttributes()
					.getNamedItem("subscriptionId").getTextContent();
				System.out.println("subscriptionId :" + subscriptionId);
			} catch (Exception e) {
				// Add seat order response
				try {
					subscriptionId = doc.getElementsByTagName("subscriptionQuantityRequest").item(0).getAttributes()
						.getNamedItem("subscriptionId").getTextContent();
					System.out.println("subscriptionId :" + subscriptionId);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			if (Strings.isNullOrEmpty(subscriptionId)) {
				AssertUtils.fail("SubscriptionID is not available the Pelican response : " + subscriptionId);
			}

			String subscriptionPeriodStartDate = doc.getElementsByTagName("subscription").item(0).getAttributes()
				.getNamedItem("subscriptionPeriodStartDate").getTextContent();
			System.out.println("subscriptionPeriodStartDate :" + subscriptionPeriodStartDate);

			String subscriptionPeriodEndDate = doc.getElementsByTagName("subscription").item(0).getAttributes()
				.getNamedItem("subscriptionPeriodEndDate").getTextContent();
			System.out.println("subscriptionPeriodEndDate :" + subscriptionPeriodEndDate);

			String fulfillmentDate = doc.getElementsByTagName("subscription").item(0).getAttributes()
				.getNamedItem("fulfillmentDate").getTextContent();
			System.out.println("fulfillmentDate :" + fulfillmentDate);

			String storedPaymentProfileId = doc.getElementsByTagName("storedPaymentProfileId").item(0).getTextContent();
			System.out.println("storedPaymentProfileId :" + storedPaymentProfileId);

			String fulfillmentStatus = root.getAttribute("fulfillmentStatus");
			System.out.println("fulfillmentStatus : " + root.getAttribute("fulfillmentStatus"));
			results.put("getPOReponse_orderState", orderState);
			results.put("getPOReponse_subscriptionId", subscriptionId);
			results.put("getPOReponse_storedPaymentProfileId", storedPaymentProfileId);
			results.put("getPOReponse_fulfillmentStatus", fulfillmentStatus);
			results.put("getPOReponse_subscriptionPeriodStartDate", subscriptionPeriodStartDate);
			results.put("getPOReponse_subscriptionPeriodEndDate", subscriptionPeriodEndDate);
			results.put("getPOReponse_fulfillmentDate", fulfillmentDate);

		} catch (Exception e) {
			Util.printTestFailedMessage("Unable to get Purchase Order Details");
			e.printStackTrace();
		}

		return results;
	}

	private void getUrl(String URL) {
		try {
			driver.manage().deleteAllCookies();
			driver.get(URL);
			bicPage.waitForPageToLoad();
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
				webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"))));
	}

	public void execKillProcess() {
		try {
			Runtime.getRuntime().exec("TASKKILL -F -IM CHROMEDRIVER.EXE");
			System.exit(0);
		} catch (Exception e) {
			Util.printInfo("Kill process error " + e.getMessage());

		}
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
		try {
//			Util.sleep(2000);
			WebElement bicAcceptCookiesBtn = bicPage.getMultipleWebElementsfromField("bicAcceptCookiesBtn").get(0);
			if (bicAcceptCookiesBtn.isDisplayed())
				bicAcceptCookiesBtn.click();

			Util.printInfo("Cookies accepted...");
		} catch (Exception e) {
			Util.printInfo("Cookies accept box does not appear on the page...");
		}

		try {
//			Util.sleep(2000);
			WebElement stayOnUSSite = bicPage.getMultipleWebElementsfromField("stayOnUSPageLink").get(0);
			if (stayOnUSSite.isDisplayed())
				stayOnUSSite.click();

			Util.printInfo("Clicked on Stay On US page link...");
		} catch (Exception e) {
			Util.printInfo("Stay on US Site link is not displayed...");
		}
	}

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place Cloud Credit Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createGUACCloudCreditOrdersUS(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String productID = "";
		String quantity = "";
		String guacResourceURL = data.get("guacResourceURL");
		String userType = data.get("userType");
		String region = data.get("languageStore");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");

		if (System.getProperty("sku").contains("default")) {
			productID = data.get("productID");
		}

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
			emailID = System.getProperty("email");
			String O2ID = getO2ID(data, emailID);
			// New user to be created
			if ((Strings.isNullOrEmpty(O2ID))) {
				orderNumber = createBICOrder(data, emailID, guacBaseURL, productID, quantity, guacResourceURL, region, password,
						paymentMethod);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(12, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = createBICOrder(data, emailID, guacBaseURL, productID, quantity, guacResourceURL, region, password,
					paymentMethod);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createGUACBic_Orders_US(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String productID = "";
		String quantity = "";
		String guacResourceURL = data.get("guacResourceURL");
		String userType = data.get("userType");
//		String addressUS = data.get("Cart_Address_US");
		String region = data.get("languageStore");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");

		if (System.getProperty("sku").contains("default")) {
			productID = data.get("productID");
		} else {
			String sku = System.getProperty("sku");
			productID = sku.split(":")[0];
			quantity = sku.split(":")[1];
		}

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
			emailID = System.getProperty("email");
			String O2ID = getO2ID(data, emailID);
			// New user to be created
			if ((Strings.isNullOrEmpty(O2ID))) {
				orderNumber = getBICOrder(data, emailID, guacBaseURL, productID, quantity, guacResourceURL, region, password,
						paymentMethod);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(12, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = getBICOrder(data, emailID, guacBaseURL, productID, quantity, guacResourceURL, region, password,
					paymentMethod);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	@Step("Get BIC order")
	private String getBICOrder(LinkedHashMap<String, String> data, String emailID, String guacBaseURL, String productID, String quantity,
							   String guacResourceURL, String region, String password, String paymentMethod)
	{
		String orderNumber;
		String constructGuacURL = guacBaseURL + region + guacResourceURL + productID + "[qty:" + quantity + "]";
		System.out.println("constructGuacURL " + constructGuacURL);
		String firstName = null, lastName = null;
		Map<String, String> address = null;

		getUrl(constructGuacURL);
		disableChatSession();
		checkCartDetailsError();
		firstName = null;
		lastName = null;
		String randomString = RandomStringUtils.random(6, true, false);

		region = region.replace("/", "").replace("-", "");
		address = getBillingAddress(region);
		String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");

		acceptCookiesAndUSSiteLink();

		firstName = "FN" + randomString;
		Util.printInfo("firstName :: " + firstName);
		lastName = "LN" + randomString;
		Util.printInfo("lastName :: " + lastName);
		createBICAccount(firstName, lastName, emailID, password);

		data.put("firstname", firstName);
		data.put("lastname", lastName);

		debugHTMLPage("Entire Payment details");
		// Get Payment details
		selectPaymentProfile(data, paymentCardDetails);

		// Entire billing details
		debugHTMLPage("Entire billing details");

		populateBillingAddress(address, data);
		debugHTMLPage("After entering billing details");

		try {
			if (paymentMethod.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));

				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("document.getElementById('mandate-agreement').click()");

				Util.printInfo(" Checked bic_Agree is visible - " + bicPage.isFieldVisible("achCheckBoxHeader"));
				Util.printInfo(" Checked box status for bic_Agree - " + bicPage.isChecked("achCheckBox"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		orderNumber = submitGetOrderNumber();

		// Check to see if EXPORT COMPLIANCE or Null
		validateBicOrderNumber(orderNumber);
		printConsole(constructGuacURL, orderNumber, emailID, address, firstName, lastName, paymentMethod);

		return orderNumber;
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
			bicPage.waitForFieldPresent("downloadFreeTrialLoginFrame", 1000);

			// Checking if download is prompting for user sign in
			if (bicPage.isFieldVisible("downloadFreeTrialLoginFrame")) {
				bicPage.selectFrame("downloadFreeTrialLoginFrame");

				bicPage.waitForFieldPresent("downloadFreeTrialUserName", 1000);
				bicPage.sendKeysInTextFieldSlowly("downloadFreeTrialUserName", System.getProperty(BICECEConstants.EMAIL));

				bicPage.waitForFieldPresent("downloadFreeTrialVerifyUserButtonClick", 1000);
				bicPage.clickUsingLowLevelActions("downloadFreeTrialVerifyUserButtonClick");

				bicPage.waitForFieldPresent("downloadFreeTrialPassword", 1000);
				bicPage.sendKeysInTextFieldSlowly("downloadFreeTrialPassword", System.getProperty(BICECEConstants.PASSWORD));

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

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createBicOrderMoe(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String productID = "";
		String quantity = "";
		String guacResourceURL = data.get("guacResourceURL");
		String guacMoeResourceURL = data.get("guacMoeResourceURL");
		String cepURL = data.get("cepURL");
		String userType = data.get("userType");
		String region = data.get("languageStore");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");

		if (System.getProperty("sku").contains("default")) {
			productID = data.get("productID");
		} else {
			String sku = System.getProperty("sku");
			productID = sku.split(":")[0];
			quantity = "[qty:" + sku.split(":")[1] + "]";
		}

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
			emailID = System.getProperty("email");
			String O2ID = getO2ID(data, emailID);
			// New user to be created
			if ((Strings.isNullOrEmpty(O2ID))) {
				orderNumber = getBicOrderMoe(data, emailID, guacBaseURL, productID, quantity, guacResourceURL,
						guacMoeResourceURL, region, password, paymentMethod, cepURL);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(12, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = getBicOrderMoe(data, emailID, guacBaseURL, productID, quantity, guacResourceURL, guacMoeResourceURL,
					region, password, paymentMethod, cepURL);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	private String getBicOrderMoe(LinkedHashMap<String, String> data, String emailID, String guacBaseURL, String productID, String quantity,
								  String guacResourceURL, String guacMoeResourceURL, String region, String password, String paymentMethod, String cepURL)
	{
		String orderNumber;
		String constructGuacURL = guacBaseURL + region + guacResourceURL + productID + quantity;
		System.out.println("constructGuacURL " + constructGuacURL);
		String constructGuacMoeURL = guacBaseURL + region + guacMoeResourceURL;
		System.out.println("constructGuacMoeURL " + constructGuacMoeURL);
		String constructPortalUrl = cepURL;
		String firstName = null, lastName = null;
		Map<String, String> address = null;

		getUrl(constructGuacURL);
		disableChatSession();
		checkCartDetailsError();

		firstName = null;
		lastName = null;
		String randomString = RandomStringUtils.random(6, true, false);

		region = region.replace("/", "").replace("-", "");
		address = getBillingAddress(region);
		String[] paymentCardDetails = getPaymentDetails(paymentMethod.toUpperCase()).split("@");

		acceptCookiesAndUSSiteLink();

		firstName = "FN" + randomString;
		Util.printInfo("firstName :: " + firstName);
		lastName = "LN" + randomString;
		Util.printInfo("lastName :: " + lastName);
		createBICAccount(firstName, lastName, emailID, password);

		data.put("firstname", firstName);
		data.put("lastname", lastName);

		debugHTMLPage("Entire Payment details");

		// Get Payment details
		selectPaymentProfile(data, paymentCardDetails);

		// Entire billing details
		debugHTMLPage("Entire billing details");

		populateBillingAddress(address, data);
		debugHTMLPage("After entering billing details");

		getUrl(constructGuacMoeURL);
		loginToMoe();
		emulateUser(emailID);
		agreeToTerm();

		orderNumber = submitGetOrderNumber();

		// Check to see if EXPORT COMPLIANCE or Null
		validateBicOrderNumber(orderNumber);
		printConsole(constructGuacMoeURL, orderNumber, emailID, address, firstName, lastName, paymentMethod);

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
		bicPage.waitForField("moeLoginUsernameField", true, 30000);
		bicPage.click("moeLoginUsernameField");
		bicPage.populateField("moeLoginUsernameField", "svc_s_guac@autodesk.com");
		bicPage.click("moeLoginButton");
		bicPage.waitForField("moeLoginPasswordField", true, 30000);
		bicPage.click("moeLoginPasswordField");
		bicPage.populateField("moeLoginPasswordField", "K16PF6LCtnsf99");
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
		bicPage.waitForField("autodeskId", true, 30000);
		bicPage.populateField("autodeskId", emailID);
		bicPage.click("userNameNextButton");
		bicPage.waitForField("loginPassword", true, 5000);
		bicPage.click("loginPassword");
		bicPage.populateField("loginPassword", password);
		bicPage.clickToSubmit("loginButton", 10000);
		bicPage.waitForPageToLoad();

		if (bicPage.isFieldPresent("getStartedSkipLink"))
			bicPage.click("getStartedSkipLink");

		Util.printInfo("Successfully logged in");
	}

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createGUACBic_Orders_PromoCode(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacBaseURL = data.get("guacBaseURL");
		String productID = "";
		String quantity = "";
		String guacResourceURL = data.get("guacResourceURL");
		String userType = data.get("userType");
		String region = data.get("languageStore");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");
		String promocode = System.getProperty("promocode");

		if (System.getProperty("sku").contains("default")) {
			productID = data.get("productID");
		} else {
			String sku = System.getProperty("sku");
			productID = sku.split(":")[0];
			quantity = sku.split(":")[1];
		}

		// While picking the default value we are also overriding the Price ID to match
		// with the promo code

		boolean promoAvailable = !(Strings.isNullOrEmpty(promocode));
		boolean skuAvailable = !(System.getProperty("sku").contains("default"));

		if (!(promoAvailable && skuAvailable)) {
			promocode = "GUACPROMO";
			productID = "27125";
		}

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
			emailID = System.getProperty("email");
			String O2ID = getO2ID(data, emailID);
			// New user to be created
			if ((Strings.isNullOrEmpty(O2ID))) {
				orderNumber = getBICOrderPromoCode(data, emailID, guacBaseURL, productID, guacResourceURL, region, password,
						paymentMethod, promocode);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(13, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = getBICOrderPromoCode(data, emailID, guacBaseURL, productID, guacResourceURL, region, password,
					paymentMethod, promocode);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		results.put("priceBeforePromo", data.get("priceBeforePromo"));
		results.put("priceAfterPromo", data.get("priceAfterPromo"));
		return results;
	}

	@SuppressWarnings({ "static-access", "unused" })
	@Step("Guac: Place DR Order " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createGUACBICIndirectOrderJP(LinkedHashMap<String, String> data) {
		String orderNumber = null;
		String emailID = null;
		HashMap<String, String> results = new HashMap<>();
		String guacDRBaseURL = data.get("guacDRBaseURL");
		String productID = "";
		String quantity = "";
		String guacDRResourceURL = data.get("guacDRResourceURL");
		String userType = data.get("userType");
		String region = data.get("languageStoreDR");
		String password = data.get("password");
		String paymentMethod = System.getProperty("payment");

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
				orderNumber = createBICIndirectOrder(data, emailID, guacDRBaseURL, productID, quantity, guacDRResourceURL,
						region, password, paymentMethod);
			}
		} else {
			String timeStamp = new RandomStringUtils().random(12, true, false);
			emailID = generateUniqueEmailID(System.getProperty("store").replace("-", ""), timeStamp, "thub", "letscheck.pw");
			orderNumber = createBICIndirectOrder(data, emailID, guacDRBaseURL, productID, quantity, guacDRResourceURL, region,
					password, paymentMethod);
		}

		results.put(BICConstants.emailid, emailID);
		results.put(BICConstants.orderNumber, orderNumber);
		return results;
	}

	private String createBICIndirectOrder(LinkedHashMap<String, String> data, String emailID, String guacDRBaseURL,
			String productID, String quantity, String guacDRResourceURL, String region, String password, String paymentMethod)
	{
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
		Util.printInfo("firstName :: " + firstName);
		lastName = "LN" + randomString;
		Util.printInfo("lastName :: " + lastName);
		createBICAccount(firstName, lastName, emailID, password);

		data.put("firstname", firstName);
		data.put("lastname", lastName);

		debugHTMLPage("Enter Payment details");
		// Get Payment details
		selectPaymentProfileDR(data, paymentCardDetails);

		// Enter billing details
		debugHTMLPage("Enter billing details");

		populateBillingAddressDR(address, data);
		debugHTMLPage("After entering billing details");

		agreeToTerm();
		clickOnMakeThisATestOrder();

		orderNumber = submitGetOrderNumber();

		// Check to see if EXPORT COMPLIANCE or Null
		validateBicOrderNumber(orderNumber);
		printConsole(constructGuacDRURL, orderNumber, emailID, address, firstName, lastName, paymentMethod);

		clickOnViewInvoiceLink();

		return orderNumber;
	}

	public void selectPaymentProfileDR(HashMap<String, String> data, String[] paymentCardDetails) {
		try {
			Util.printInfo("Selecting DR payment profile : " + data.get("paymentType"));
			switch (data.get("paymentType").toUpperCase()) {
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
				paymentDetails = "4035300539804083@3月@2025@456";
		}
		return paymentDetails;
	}

	@Step("Populate DR payment details")
	public void populatePaymentDetailsDR(String[] paymentCardDetails) {
		Util.printInfo("Enter card details to make payment");

		bicPage.waitForField("creditCardFrameDR", true, 30000);
		bicPage.executeJavascript("window.scrollBy(0,600);");

		try {
			WebElement creditCardFrameDR = bicPage.getMultipleWebElementsfromField("creditCardFrameDR").get(0);
			driver.switchTo().frame(creditCardFrameDR);
			Util.sleep(2000);

			String expirationMonthDRXpath = "";
			String expirationYearDRXpath = "";

			expirationMonthDRXpath = bicPage.getFirstFieldLocator("expirationMonthDR");
			expirationYearDRXpath = bicPage.getFirstFieldLocator("expirationYearDR");

			Util.printInfo("Entering DR card number : " + paymentCardDetails[0]);
			bicPage.waitForField("cardNumberDR", true, 10000);
			bicPage.click("cardNumberDR");
			bicPage.executeJavascript("document.getElementById('ccNum').value='" + paymentCardDetails[0] + "'");
			Util.sleep(2000);

			WebElement monthEle = driver.findElement(By.xpath(expirationMonthDRXpath));
			Select selMonth = new Select(monthEle);
			selMonth.selectByVisibleText(paymentCardDetails[1]);
			Util.sleep(2000);

			WebElement yearEle = driver.findElement(By.xpath(expirationYearDRXpath));
			Select selYear = new Select(yearEle);
			selYear.selectByVisibleText(paymentCardDetails[2]);
			Util.sleep(2000);

			Util.printInfo("Entering security code : " + paymentCardDetails[3]);
			bicPage.click("cardSecurityCodeDR");
			sendKeysAction("cardSecurityCodeDR", paymentCardDetails[3]);
			Util.sleep(2000);
			driver.switchTo().defaultContent();
		} catch (MetadataException e) {
			e.printStackTrace();
			AssertUtils.fail("Unable to enter Card details to make payment");
		}
	}

	@Step("Populate DR billing address")
	public boolean populateBillingAddressDR(Map<String, String> address, HashMap<String, String> data) {

		boolean status = false;
		try {
			WebElement creditCardFrameDR = bicPage.getMultipleWebElementsfromField("creditCardFrameDR").get(0);
			driver.switchTo().frame(creditCardFrameDR);

			String paymentType = System.getProperty("payment");
			String firstNameXpath = "";
			String lastNameXpath = "";

			Util.sleep(2000);

			firstNameXpath = bicPage.getFirstFieldLocator("firstNameDR");
			lastNameXpath = bicPage.getFirstFieldLocator("lastNameDR");

			driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get("firstname"));
			driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get("lastname"));
			driver.switchTo().defaultContent();

			status = populateBillingDetailsDR(address, paymentType);

			clickOnSaveProfileBtnDR();

		} catch (Exception e) {
			e.printStackTrace();
			debugHTMLPage(e.getMessage());
			AssertUtils.fail("Unable to populate the Billing Address details");
		}
		return status;
	}

	@SuppressWarnings("static-access")
	public boolean populateBillingDetailsDR(Map<String, String> address, String paymentType) {
		boolean status = false;
		try {
			WebElement creditCardFrameDR = bicPage.getMultipleWebElementsfromField("creditCardFrameDR").get(0);
			driver.switchTo().frame(creditCardFrameDR);

			Util.printInfo("Adding DR billing details...");

			String orgNameXpath = "", fullAddrXpath = "", zipXpath = "", phoneXpath = "", agreementXpath = "";

			switch (paymentType.toUpperCase()) {
				default:
					orgNameXpath = bicPage.getFirstFieldLocator("companyNameDR");
					fullAddrXpath = bicPage.getFirstFieldLocator("addressDR");
					zipXpath = bicPage.getFirstFieldLocator("postalCodeDR");
					phoneXpath = bicPage.getFirstFieldLocator("phoneNumberDR");
					agreementXpath = bicPage.getFirstFieldLocator("saveMyAccountCheckboxDR");
					break;
			}

			Util.sleep(1000);
			WebDriverWait wait = new WebDriverWait(driver, 60);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(orgNameXpath)));
			status = driver.findElement(By.xpath(orgNameXpath)).isDisplayed();

			if (status == false)
				AssertUtils.fail("Organization_Name not available.");

			driver.findElement(By.xpath(orgNameXpath)).click();
			Util.sleep(1000);
			driver.findElement(By.xpath(orgNameXpath))
					.sendKeys(new RandomStringUtils().random(5, true, true) + address.get("companyNameDR"));

			driver.findElement(By.xpath(orgNameXpath)).click();
			Util.sleep(1000);

			driver.findElement(By.xpath(fullAddrXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(fullAddrXpath)).sendKeys(address.get("addressDR"));

			driver.findElement(By.xpath(zipXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(zipXpath)).sendKeys(address.get("postalCodeDR"));

			driver.findElement(By.xpath(phoneXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
			Util.sleep(1000);
			driver.findElement(By.xpath(phoneXpath)).sendKeys(address.get("phoneNumberDR"));

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
		} catch (Exception e) {
			AssertUtils.fail("Failed to click on 'Make this a test order' link");
		}
		Util.sleep(2000);
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

			orderNumber = driver.findElement(By.xpath("//h5[.='注文番号：']/..//p")).getText();
			Util.printInfo("Find DR order number: " + orderNumber);

			driver.findElement(By.linkText("請求書を表示する")).click();
			Util.sleep(10000);

			ArrayList<String> tab = new ArrayList<>(driver.getWindowHandles());
			driver.switchTo().window(tab.get(1));

			orderNumberInStore = driver.findElement(By.xpath("//*[@id='dr_orderNumber']//span")).getText();
			Util.printInfo("Find DR order number from store: " + orderNumberInStore);
			AssertUtils.assertEquals(orderNumber, orderNumberInStore);
		} catch (Exception e) {
			AssertUtils.fail("Failed to validate order number from Store");
		}
	}
}
