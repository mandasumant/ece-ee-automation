package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.base.GlobalTestBase;
import com.autodesk.testinghub.core.common.CommonConstants;
import com.autodesk.testinghub.core.common.services.ApigeeAuthenticationService;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.soapclient.SOAPService;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.CustomSoftAssert;
import com.autodesk.testinghub.core.utils.ErrorEnum;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SearchTerm;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PortalTestBase {

  private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static Page_ portalPage = null;
  public static Page_ studentPage = null;
  public WebDriver driver = null;
  private Folder gmailFolder;

  public PortalTestBase(GlobalTestBase testbase) {
    driver = testbase.getdriver();
    portalPage = testbase.createPage("PAGE_PORTAL");
    studentPage = testbase.createCommonPage("PAGE_STUDENT");
    new BICTestBase(driver, testbase);
  }

  public static String generateRandom(int length) {
    Random random = new SecureRandom();
    if (length <= 0) {
      throw new IllegalArgumentException("String length must be a positive integer");
    }

    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(characters.charAt(random.nextInt(characters.length())));
    }

    return sb.toString();
  }

  public static String timestamp() {
    String strDate = null;
    Date date = Calendar.getInstance().getTime();
    DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss");
    strDate = dateFormat.format(date).replace(" ", "").replace("-", "").replace(":", "");
    return strDate;
  }

  @Step("Create new Account and Contact" + GlobalConstants.TAG_TESTINGHUB)
  public LinkedHashMap<String, String> createNewAccount(LinkedHashMap<String, String> data) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    openPortal(data.get("cepURL"));
    String emailID = generateUniqueEmailID("thub", generateRandomTimeStamp(), "letscheck.pw");
    data.put(TestingHubConstants.firstname, "THub" + generateRandom(5));
    data.put(TestingHubConstants.lastname, generateRandom(4));
    data.put(TestingHubConstants.emailid, emailID);
    data.put(TestingHubConstants.passCEP, data.get("password"));

    try {
      portalPage.clickUsingLowLevelActions("createAccountCEP");
      portalPage.waitForPageToLoad();
      Util.sleep(10000);

//          driver.findElement(By.xpath("//input[@id='firstname_str']")).sendKeys(data.get(TestingHubConstants.firstname));
      WebElement firstNameEle = portalPage.getMultipleWebElementsfromField("firstNameCEP").get(0);
      firstNameEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + data.get(TestingHubConstants.firstname) + "';",
          firstNameEle);
      Util.printInfo("Entered first name : " + data.get(TestingHubConstants.firstname));

//          driver.findElement(By.xpath("//input[@id='lastname_str']")).sendKeys(data.get(TestingHubConstants.lastname));
      WebElement lastNameEle = portalPage.getMultipleWebElementsfromField("lastNameCEP").get(0);
      lastNameEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + data.get(TestingHubConstants.lastname) + "';",
          lastNameEle);
      Util.printInfo("Entered last name : " + data.get(TestingHubConstants.lastname));

//          driver.findElement(By.xpath("//input[@id='email_str']")).sendKeys(data.get(TestingHubConstants.emailid));
      WebElement emailEle = portalPage.getMultipleWebElementsfromField("emailIdCEP").get(0);
      emailEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + data.get(TestingHubConstants.emailid) + "';",
          emailEle);
      Util.printInfo("Entered Email ID : " + data.get(TestingHubConstants.emailid));

//          driver.findElement(By.xpath("//input[@id='confirm_email_str']")).sendKeys(data.get(TestingHubConstants.emailid));
      WebElement confirmEmailEle = portalPage.getMultipleWebElementsfromField("ConfirmEmailCEP")
          .get(0);
      confirmEmailEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + data.get(TestingHubConstants.emailid) + "';",
          confirmEmailEle);
      Util.printInfo("Entered Confirm email id : " + data.get(TestingHubConstants.emailid));

//          driver.findElement(By.xpath("//input[@id='password']")).sendKeys(data.get(TestingHubConstants.passCEP));
      WebElement passwordEle = portalPage.getMultipleWebElementsfromField("passCEP").get(0);
      passwordEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + data.get(TestingHubConstants.passCEP) + "';",
          passwordEle);
      Util.sleep(2000);

      String CheckboxClick = "document.getElementById(\"privacypolicy_checkbox\").click()";
      clickCheckBox(CheckboxClick);
      Util.sleep(5000);

      js.executeScript("arguments[0].click();",
          portalPage.getMultipleWebElementsfromField("createAccount").get(0));
      Util.printInfo("Account created...Navigating to portal...");
      portalPage.waitForPageToLoad();
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      data.put("createdTime", timestamp + "");

      feynamnLayoutLoaded();
      data.put(TestingHubConstants.oxygenid, getOxygenId(data).trim());

      String streetAddress, addressCity, country, postalCode, region;

      if (data.get(TestingHubConstants.salesOrg).startsWith("1")) {
        Util.PrintInfo("Sales org is : " + data.get(TestingHubConstants.salesOrg));
        streetAddress = data.get("streetAddressAPAC");
        data.put(TestingHubConstants.streetAddress, streetAddress);

        addressCity = data.get("addressCityAPAC");
        data.put(TestingHubConstants.addressCity, addressCity);

        country = data.get("countryAPAC");
        data.put(TestingHubConstants.country, country);

        region = data.get("regionAPAC");
        data.put(TestingHubConstants.region, region);

        postalCode = data.get("postalCodeAPAC");
        data.put(TestingHubConstants.postalCode, postalCode);
      } else if (data.get(TestingHubConstants.salesOrg).startsWith("2")) {
        Util.PrintInfo("Sales org is : " + data.get(TestingHubConstants.salesOrg));
        streetAddress = data.get("streetAddressEMEA");
        data.put(TestingHubConstants.streetAddress, streetAddress);

        addressCity = data.get("addressCityEMEA");
        data.put(TestingHubConstants.addressCity, addressCity);

        country = data.get("countryEMEA");
        data.put(TestingHubConstants.country, country);

        region = data.get("regionEMEA");
        data.put(TestingHubConstants.region, region);

        postalCode = data.get("postalCodeEMEA");
        data.put(TestingHubConstants.postalCode, postalCode);
      } else {
        Util.PrintInfo("Sales org is : " + data.get(TestingHubConstants.salesOrg));
        country = data.get("country");
        if (country.length() > 3) {
          country = data.get("countryCode");
          System.out.println("Country is ::" + country);
          data.put(TestingHubConstants.country, country);
        }
      }

      // Creating Account
      Util.printInfo("Creating enduser CSN...");
      createContactCSN(data);
      Util.sleep(20000);

      // Associate CSN with User
      matchAccountContactNew(data);
      Util.sleep(20000);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail(ErrorEnum.ACCOUNT_CREATION_ERR_CEP.geterr());
    }
    signOutPortal();
    return data;
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
        userSessionID = driver.findElement(By.xpath("//textarea[@id='id-userId']")).getText()
            .toString();
      } catch (Exception e) {
        //e.printStackTrace();
        Util.printError(e.getMessage());
      } finally {
        clickALLPSLink();
      }
    }
    return userSessionID;
  }

  public boolean signOutPortal() {
    boolean status = false;
    try {
      Util.sleep(2000);
      Util.printInfo("Sign out 1st button is displayed :: " + portalPage
          .getMultipleWebElementsfromField("logout").get(0).isDisplayed());
      driver.findElement(By.xpath("//img[@class='hig__avatarV2__image']")).click();
      Util.sleep(5000);
      Util.PrintInfo("Sign out 2nd button is displayed :: " + portalPage
          .getMultipleWebElementsfromField("logout2").get(0).isDisplayed());
      portalPage.getMultipleWebElementsfromField("logout2").get(0).click();
      Util.sleep(5000);
      portalPage.waitForPageToLoad();
      status = true;
      Util.printInfo("Portal SignOut successful");
    } catch (Exception e) {
      e.printStackTrace();
      Util.printError("Portal SignOut Failed");
    }
    return status;
  }

  private void createContactCSN(LinkedHashMap<String, String> data) {
    String timestamp = generateRandomTimeStamp();
    System.out.println(timestamp);
    String[] inputData = {"{p_UserName}->" + data.get("macUser"),
        "{p_Password}->" + data.get("macPass"),
        "{p_newAccountName}->" + (timestamp + "THub"),
        "{p_CSN}->" + data.get(TestingHubConstants.macCSN),
        "{p_SalesOrg}->" + data.get("salesOrg"), "{p_AccountCSN}->" + data.get("soldToParty"),
        "{p_AddressLine1}->" + data.get(TestingHubConstants.streetAddress),
        "{p_AddressLine2}->" + "na",
        "{p_AddressLine3}->" + "na", "{p_City}->" + data.get(TestingHubConstants.addressCity),
        "{p_State}->" + data.get(TestingHubConstants.region),
        "{p_Country}->" + data.get(TestingHubConstants.country),
        "{p_PostalCode}->" + data.get(TestingHubConstants.postalCode)};
    createAccount("", inputData, "Account", GlobalConstants.getENV(), data);
  }

  //@Step("launch URL in browser")
  public boolean openPortal(String data) {
    driver.manage().deleteAllCookies();
    driver.navigate().to(data);
    Util.printInfo("Opened:" + data);
    boolean openPortal = false;
    int count = 0;
    do {
      try {
        openPortal = portalPage.getMultipleWebElementsfromField("createAccountCEP").get(0)
            .isDisplayed();
      } catch (Exception e) {
        Util.printWarning("Portal page loading :: " + openPortal);
      }

      if (count != 0) {
        driver.navigate().refresh();
        Util.sleep(20000);
      } else if (count > 3) {
        break;
      }
    } while (!openPortal);
    return openPortal;
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
    return feynamnLayoutLoaded();
  }

  //@Step("launch URL in browser")
  public boolean openPortalBICLaunch(String data) {
    Util.printInfo("launch URL in browser");
    driver.manage().deleteAllCookies();
    driver.navigate().to(data);
    Util.printInfo("Opened:" + data);
    return feynamnLayoutLoaded();
  }

  @Step("Generate email id")
  public String generateUniqueEmailID(String storeKey, String timeStamp, String emailDomain) {
    String strDate = null;
    String stk = storeKey.replace("-", "");
    strDate = stk + timeStamp + "@" + emailDomain;
    return strDate;
  }

  public String generateRandomTimeStamp() {
    String strDate = null;
    Date date = Calendar.getInstance().getTime();
    DateFormat dateFormat = new SimpleDateFormat("dd hh:mm:ss");
    strDate = dateFormat.format(date).replace(" ", "").replace("-", "").replace(":", "");
    return strDate;
  }

  public void matchAccountContactNew(HashMap<String, String> data) {
    String[] inputData = {"{p_UserName}->" + data.get("macUser"),
        "{p_Password}->" + data.get("macPass"),
        "{p_CSN}->" + data.get("enduserCSN"), "{p_SalesOrg}->" + data.get("salesOrg"),
        "{p_AccountCSN}->" + data.get("enduserCSN"),
        "{p_AddressLine1}->" + data.get(TestingHubConstants.streetAddress),
        "{p_AddressLine2}->" + "na", "{p_AddressLine3}->" + "na",
        "{p_City}->" + data.get(TestingHubConstants.addressCity),
        "{p_State}->" + data.get(TestingHubConstants.region), "{p_Country}->"
        + data.get(TestingHubConstants.country),
        "{p_PostalCode}->" + data.get(TestingHubConstants.postalCode),
        "{p_Firstname}->" + data.get(TestingHubConstants.firstname).trim(),
        "{p_Lastname}->" + data.get(TestingHubConstants.lastname),
        "{p_EmailId}->" + data.get(TestingHubConstants.emailid)};

    createAccount("", inputData, "Contact", GlobalConstants.getENV(), data);
  }

  @Step("Create End-User Account - {1}")
  private String createAccount(String msg, String[] inputData, String userCategory, String env,
      HashMap<String, String> data) {
    String xml = null;
    String url = null;
    try {
      url = "https://enterprise-api-" + env.toLowerCase()
          + ".autodesk.com/v2/matchaccountcontact/matchaccountcontact";
      System.out.println("url :: " + url);
      //  url = data.get("matchAccountContactServiceEndPointURL") + "/matchaccountcontact";
      String consumerKey = CommonConstants.consumerKey.trim();
      String consumerSecret = CommonConstants.consumerSecret.trim();
      String callBackURL = CommonConstants.callbackURL.trim();

      if (userCategory.equalsIgnoreCase("Contact")) {
        xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ads=\"http://www.autodesk.com/xmlns/AdskGenericMessage\" xmlns:mat=\"http://www.autodesk.com/xmlns/MatchAccountContact\"> <soapenv:Header> <wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" soapenv:mustUnderstand=\"1\"> <wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"UsernameToken-1\"> <wsse:Username>{p_UserName}</wsse:Username> <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">{p_Password}</wsse:Password> <wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">o9Vv91iqOLxwX4sbXrHwgQ==</wsse:Nonce> <wsu:Created>2009-08-01T08:04:10.578Z</wsu:Created> </wsse:UsernameToken> </wsse:Security> <Header xmlns=\"http://www.autodesk.com/schemas/Technical/Common/RequestHeaderV1.0\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <MessageIdentifier> <MessageName>User</MessageName> <MessageVersion>1</MessageVersion> </MessageIdentifier><RequestingSystem> <RequestingApplicationName>SWS</RequestingApplicationName> </RequestingSystem> <Properties> <CachedDataAccess>false</CachedDataAccess> </Properties> </Header> </soapenv:Header> <soapenv:Body> <ns0:MatchAccountContactRequest xmlns:ns0=\"http://www.autodesk.com/xmlns/MatchAccountContact\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"> <ns0:Request> <ns1:EventName xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">MatchContact</ns1:EventName> <ns1:RequestNumber xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">Contactglenmaxwell20150706211458@ssttest.net2015-07-06T21:15:17.081-07:00-549560</ns1:RequestNumber><ns1:SessionId xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">00DQ000000GKDVx!ARcAQGvsPzCpnrPzU6gPdBMfTAYh1Ec41ftzw4xydpF3g.8SpfhtxQHnq31YmZIAqPtxVQR11j.3QMOSvYyQ0_3vRM.r8kIQ</ns1:SessionId><ns1:Source xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">SFDC</ns1:Source></ns0:Request><ns0:RequestContext><ns0:PartnerFunctionForReq>WE</ns0:PartnerFunctionForReq><ns0:PartnerSalesOrg>{p_SalesOrg}</ns0:PartnerSalesOrg><ns0:AccountAction>Account Query</ns0:AccountAction><ns0:ContactAction>Contact MatchWithoutAddress</ns0:ContactAction><ns0:TrilliumOverrideFlag>N</ns0:TrilliumOverrideFlag><ns0:CreateFlag>N</ns0:CreateFlag></ns0:RequestContext><ns0:Account><ns0:CSN>{p_AccountCSN}</ns0:CSN></ns0:Account><ns0:Contact><ns0:ADSKGUID /><ns0:ADSKContactCSN /><ns0:Title /><ns0:FirstName>{p_Firstname}</ns0:FirstName><ns0:MiddleName /><ns0:LastName>{p_Lastname}</ns0:LastName><ns0:ADSKAlternateFirstName /><ns0:ADSKAlternateLastName /><ns0:EmailAddress>{p_EmailId}</ns0:EmailAddress><ns0:ADSKContactLanguage /><ns0:JobTitle /><ns0:ADSKEregDepartment /><ns0:ADSKWorkPhone /><ns0:ADSKWorkPhoneExtension /><ns0:ADSKFaxPhoneNumber /><ns0:ADSKMobileNumber /><ns0:Status>Active</ns0:Status><ns0:SuppressAllCalls>N</ns0:SuppressAllCalls><ns0:SuppressAllEmails>N</ns0:SuppressAllEmails><ns0:SuppressAllMailings>N</ns0:SuppressAllMailings><ns0:SuppressAllFaxes>N</ns0:SuppressAllFaxes></ns0:Contact></ns0:MatchAccountContactRequest></soapenv:Body></soapenv:Envelope>";
      }

      if (userCategory.equalsIgnoreCase("Account")) {
        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ads=\"http://www.autodesk.com/xmlns/AdskGenericMessage\" xmlns:mat=\"http://www.autodesk.com/xmlns/MatchAccountContact\">\r\n"
            + "   <soapenv:Header>\r\n"
            + "      <Header xmlns=\"http://www.autodesk.com/schemas/Technical/Common/RequestHeaderV1.0\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
            + "         <MessageIdentifier>\r\n"
            + "            <MessageName>AutomationQA</MessageName>\r\n"
            + "            <MessageVersion>1</MessageVersion>\r\n"
            + "         </MessageIdentifier>\r\n"
            + "         <RequestingSystem>\r\n"
            + "            <RequestingApplicationName>POSTMAN</RequestingApplicationName>\r\n"
            + "         </RequestingSystem>\r\n" + "         <Properties>\r\n"
            + "            <CachedDataAccess>true</CachedDataAccess>\r\n"
            + "         </Properties>\r\n"
            + "      </Header>\r\n" + "   </soapenv:Header>\r\n" + "   <soapenv:Body>\r\n"
            + "      <ns0:MatchAccountContactRequest xmlns:ns0=\"http://www.autodesk.com/xmlns/MatchAccountContact\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n"
            + "         <ns0:Request>\r\n"
            + "            <ns1:EventName xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">MatchAccount</ns1:EventName>\r\n"
            + "            <ns1:RequestNumber xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">1</ns1:RequestNumber>\r\n"
            + "            <ns1:SessionId xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">0003757093</ns1:SessionId>\r\n"
            + "            <ns1:Source xmlns:ns1=\"http://www.autodesk.com/xmlns/AdskGenericMessage\">IORDER</ns1:Source>\r\n"
            + "         </ns0:Request>\r\n" + "         <ns0:RequestContext>\r\n"
            + "            <ns0:PartnerFunctionForReq>End User</ns0:PartnerFunctionForReq>\r\n"
            + "            <ns0:UserType>Partner</ns0:UserType>\r\n"
            + "            <ns0:PartnerAccountCSN>{p_CSN}</ns0:PartnerAccountCSN>\r\n"
            + "            <ns0:PartnerUserCSN>{p_CSN}</ns0:PartnerUserCSN>\r\n"
            + "            <ns0:PartnerSalesOrg>{p_SalesOrg}</ns0:PartnerSalesOrg>\r\n"
            + "            <ns0:AccountAction>Account Match</ns0:AccountAction>\r\n"
            + "            <ns0:ContactAction />\r\n"
            + "            <ns0:TrilliumOverrideFlag>Y</ns0:TrilliumOverrideFlag>\r\n"
            + "            <ns0:CreateFlag>Y</ns0:CreateFlag>\r\n"
            + "         </ns0:RequestContext>\r\n"
            + "         <ns0:Account>\r\n"
            + "            <ns0:AccountType>End Customer</ns0:AccountType>\r\n"
            + "            <ns0:Name>{p_newAccountName}</ns0:Name>\r\n"
            + "            <ns0:ADSKLocalLanguageName />\r\n" + "            <ns0:Address>\r\n"
            + "               <ns0:IsPrimaryAddress>Y</ns0:IsPrimaryAddress>\r\n"
            + "               <ns0:AddressName />\r\n"
            + "               <ns0:Street1>{p_AddressLine1}</ns0:Street1>\r\n"
            + "               <ns0:Street2 />\r\n" + "               <ns0:Street3 />\r\n"
            + "               <ns0:City>{p_City}</ns0:City>\r\n"
            + "               <ns0:State>{p_State}</ns0:State>\r\n"
            + "               <ns0:Country>{p_Country}</ns0:Country>\r\n"
            + "               <ns0:PostalCode>{p_PostalCode}</ns0:PostalCode>\r\n"
            + "            </ns0:Address>\r\n"
            + "            <ns0:IsPOBOXValidationReqd>Y</ns0:IsPOBOXValidationReqd>\r\n"
            + "            <ns0:MainPhoneNumber />\r\n"
            + "            <ns0:UserEnteredAddressFields>\r\n"
            + "               <ns0:UserEnteredAccountName>QA Automtion</ns0:UserEnteredAccountName>\r\n"
            + "               <ns0:UserEnteredAddressStreetAddress>{p_AddressLine1}</ns0:UserEnteredAddressStreetAddress>\r\n"
            + "               <ns0:UserEnteredAddressStreetAddress2 />\r\n"
            + "               <ns0:UserEnteredAddressStreetAddress3 />\r\n"
            + "               <ns0:UserEnteredAddressState>{p_State}</ns0:UserEnteredAddressState>\r\n"
            + "               <ns0:UserEnteredAddressCity>{p_City}</ns0:UserEnteredAddressCity>\r\n"
            + "               <ns0:UserEnteredAddressCountry>{p_Country}</ns0:UserEnteredAddressCountry>\r\n"
            + "               <ns0:UserEnteredAddressPostalCode>{p_PostalCode}</ns0:UserEnteredAddressPostalCode>\r\n"
            + "            </ns0:UserEnteredAddressFields>\r\n"
            + "            <ns0:TrilliumSuggestedAddressFields>\r\n"
            + "               <ns0:TrilliumSuggestedMatchedAccountId />\r\n"
            + "               <ns0:TrilliumSuggestedAccountName>QA Performance</ns0:TrilliumSuggestedAccountName>\r\n"
            + "               <ns0:TrilliumSuggestedStreetAddress>{p_AddressLine1}</ns0:TrilliumSuggestedStreetAddress>\r\n"
            + "               <ns0:TrilliumSuggestedStreetAddress2 />\r\n"
            + "               <ns0:TrilliumSuggestedStreetAddress3 />\r\n"
            + "               <ns0:TrilliumSuggestedAddressCity>{p_City}</ns0:TrilliumSuggestedAddressCity>\r\n"
            + "               <ns0:TrilliumSuggestedAddressState>{p_State}</ns0:TrilliumSuggestedAddressState>\r\n"
            + "               <ns0:TrilliumSuggestedAddressCountry>{p_Country}</ns0:TrilliumSuggestedAddressCountry>\r\n"
            + "               <ns0:TrilliumSuggestedAddressPostalCode>{p_PostalCode}</ns0:TrilliumSuggestedAddressPostalCode>\r\n"
            + "            </ns0:TrilliumSuggestedAddressFields>\r\n"
            + "         </ns0:Account>\r\n"
            + "      </ns0:MatchAccountContactRequest>\r\n" + "   </soapenv:Body>\r\n"
            + "</soapenv:Envelope>";
      }

      xml = inputTestData(xml, inputData);
      ApigeeAuthenticationService apigeeAuthenticationService = new ApigeeAuthenticationService();

      String timeStamp = apigeeAuthenticationService.getTimeStamp();
      String signature = apigeeAuthenticationService
          .getSignature(consumerKey, consumerSecret, callBackURL, timeStamp);
      String accessTokenUrl = "https://enterprise-api-" + GlobalConstants.getENV().toLowerCase()
          + ".autodesk.com/v2/oauth/generateaccesstoken?grant_type=client_credentials";
      String token = apigeeAuthenticationService
          .getAccessToken(accessTokenUrl, consumerKey, consumerSecret, callBackURL, timeStamp,
              signature);

      signature = apigeeAuthenticationService
          .getSignature(token, consumerSecret, callBackURL, timeStamp);
      SOAPService service = new SOAPService();
      SOAPMessage message = service.generateSOAPMessage(xml, data.get("macTranscoding"));

      HashMap<String, String> headers = getMeshHeaders(data.get("name"), token, timeStamp,
          data.get("macHeaderCSN"), signature, data.get("macResponseType"));
      service.decorateSOAPMessage(message, headers);
      attachReqSOAPMessage(message);

      SOAPMessage soapResponse = service.getSOAPResponse(message, url);
      attachResSOAPMessage(soapResponse);
      if (userCategory.equalsIgnoreCase("Account")) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        soapResponse.writeTo(out);
        String strMsg = out.toString();
        String actualADSKAccountCSN = parseXML(strMsg, "ns0:CSN");
        Util.PrintInfo("Account CSN - " + actualADSKAccountCSN);
        data.put(TestingHubConstants.enduserCSN, actualADSKAccountCSN);
      }
    } catch (NullPointerException n) {
      n.printStackTrace();
      AssertUtils.fail("unable to create account: getting NullPointerException " + n.getMessage());
    } catch (Exception e) {
      if (e.getMessage().contains("Export")) {
        try {
          throw new Exception("The Export Control status is not accepted / null");
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
      Util.PrintInfo("Unable to create the account - " + e.getMessage());
      AssertUtils.fail("unable to create the account -" + e.getMessage());
    }
    return url;
  }

  @Step("Request SOAP Message")
  public void attachReqSOAPMessage(SOAPMessage soapResponse) {
    try {
      printSOAPResponse(soapResponse);
    } catch (Exception e) {
      Util.PrintInfo("Unable to attach the req SOAP message, Exception -" + e.getMessage());
      AssertUtils.fail("Unable to attach the req SOAP message, Exception -" + e.getMessage());
    }
  }

  public void printSOAPResponse(SOAPMessage soapResponse) {
    try {
      StringWriter writer = new StringWriter();
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      Source sourceContent = soapResponse.getSOAPPart().getContent();
      StreamResult result = new StreamResult(writer);
      transformer.transform(sourceContent, result);
      String res = writer.toString();
      System.out.print("\nSOAP Message = " + res + "\n");
      createPassAttachment(res);
    } catch (Exception e) {
      AssertUtils.fail("Unable to parse the file -" + e.getMessage());
    }
  }

  @Attachment("Attachments")
  public String createPassAttachment(String str) {
    return str;
  }

  @Step("Response SOAP Message")
  public void attachResSOAPMessage(SOAPMessage soapResponse) {
    try {
      printSOAPResponse(soapResponse);
    } catch (Exception e) {
      Util.PrintInfo("Unable to attach the response SOAP message, Exception -" + e.getMessage());
      AssertUtils.fail("Unable to attach the response SOAP message, Exception -" + e.getMessage());
    }
  }

  public String inputTestData(String xml, String... str) {
    try {
      for (String inputData : str) {
        String[] splitData = inputData.split("->");
        if (splitData.length != 2) {
          xml = xml.replace(splitData[0], "");
        } else {
          xml = xml.replace(splitData[0], splitData[1]);
        }
      }
    } catch (Exception e) {
      AssertUtils.fail(
          "unable to replace the values with the XML with the passed data, Exception -" + e
              .getMessage());
      return null;
    }
    return xml;
  }

  public HashMap<String, String> getMeshHeaders(String actionName, String token, String timeStamp,
      String csn, String signature, String type) {
    HashMap<String, String> headers = new HashMap<>();
    headers.put("soapaction", actionName);
    headers.put("Authorization", "Bearer " + token);
    headers.put("timestamp", timeStamp);
    headers.put("csn", csn);
    headers.put("signature", signature);
    headers.put("Content-Type", "application/xml");
    return headers;
  }

  public String parseXML(String xml, String tagName) throws Exception {
    try {
      String output = null;
      DocumentBuilder db = null;
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      Document document = null;
      try {
        db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        try {
          document = db.parse(is);
          System.out.println("Root element: " + document.getDocumentElement().getNodeName());
          output = document.getElementsByTagName(tagName).item(0).getTextContent();
        } catch (SAXException e) {
          // handle SAXException
        } catch (IOException e) {
          // handle IOException
        }
      } catch (ParserConfigurationException e1) {
        // handle ParserConfigurationException
      }
      return output;
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to parse the XML, Exception -" + e.getMessage());
      return null;
    }
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

  //@Step("feynamnLayout UI load wait")
  public boolean feynamnLayoutLoaded() {

    //Not removing this code as part of refactor, as this code is expected to be used instead of static wait
//      try {
//          Util.sleep(5000);
//          JavascriptExecutor js = (JavascriptExecutor)driver;
//          WebElement bicAcceptCookiesBtn = portalPage.getMultipleWebElementsfromField("portalAcceptCookiesBtn").get(0);
//          if(bicAcceptCookiesBtn.isDisplayed())
//              js.executeScript("arguments[0].click();", bicAcceptCookiesBtn);
//
//          Util.printInfo("Cookies accepted...");
//      }
//      catch(Exception e) {
//          Util.printInfo("Cookies accept box does not appear on the page...");
//      }
//
//      portalPage.waitForField("portalProductServiceTab", true, 15000);
//      boolean status = false;
//      try {
//          status = portalPage.checkIfElementExistsInPage("portalProductServiceTab",15);
//      }
//      catch (MetadataException e) {
//          e.printStackTrace();
//          AssertUtils.fail("product and services tab not found in Portal");
//      }
//      return status;
    Util.sleep(30000);
    return true;
  }

  public void emailValidations() {
    String portalUN = System.getProperty("portalUN").trim();
    String Environment = System.getProperty("environment");
    String GmailUserName = "check.welcomekit@gmail.com";
    String GmailPassword = "@utod8sk8";
    String gmailFolder = System.getProperty("gmailFolder");
    String welcomeKitValidation = System.getProperty("welcomeKitValidation");
    String copeValidation = System.getProperty("copeValidation");

    if (!copeValidation.equals("NO")) {
      copeValidation = "[SENDWITHUS TEST] [Order Summary] Here's what you can expect next";
    } else {
      copeValidation = "NULL";
    }

    if (!welcomeKitValidation.equals("NO")) {
      welcomeKitValidation = "You are an administrator";
    } else {
      welcomeKitValidation = "NULL";
    }

    gmailLogin(GmailUserName, GmailPassword);
    createGmailConnection(GmailUserName, GmailPassword, gmailFolder, welcomeKitValidation,
        copeValidation, portalUN);
  }

  public void gmailLogin(String UserId, String Pass) {
    portalPage.navigateToURL("https://gmail.com");
    portalPage.waitForField("gmailUserIDInput", true, 60000);
    if (portalPage.checkFieldExistence("gmailUserIDInput")) {
      try {
        Util.printInfo("Populate User id");
        portalPage.populateField("gmailUserIDInput", UserId);
        portalPage.waitForField("gmailUserNextButton", true, 60000);
        Util.printInfo("Click on Next button to redirect to Password field");
        portalPage.clickUsingLowLevelActions("gmailUserNextButton");
        portalPage.waitForField("gmailPassInput", true, 60000);
        Util.printInfo("Populate password");
        portalPage.populateField("gmailPassInput", Pass);
        Util.printInfo("Click on Next button to redirect to Inbox messages");
        portalPage.waitForField("gmailPassNextButton", true, 60000);
        portalPage.clickUsingLowLevelActions("gmailPassNextButton");
        Util.sleep(5000);
      } catch (MetadataException e) {
        e.printStackTrace();
        AssertUtils.fail("Error while populating Userid / Password");
      }
    } else {
      Util.printInfo("User Id field not visible");
      Util.printInfo("");
    }
  }

  public Message[] createGmailConnection(String connectionMail, String connectionPass,
      String mailFolder, String welcomeKitSearch, String emailCopeSearch, String orderEmailID) {
    Properties props = getGmailConnectionProperties();
    Message[] message = null;
    Map<String, String> email = new HashMap<String, String>();

    try {
      // Util.sleep(8000);
      Session session = Session.getInstance(props, null);
      Store store = session.getStore("imaps");
      try {
        store.connect("imap.googlemail.com", connectionMail.trim(), connectionPass.trim());
      } catch (Exception e1) {
        e1.printStackTrace();
        AssertUtils.fail(
            "Unable to connect to gmail account, Please check the connection details / Credentials");
      }

      gmailFolder = store.getFolder(mailFolder);
      gmailFolder.open(Folder.READ_WRITE);
      Message[] messages = gmailFolder.getMessages();
      System.out.println("messages.length---" + messages.length);
      SearchTerm searchCondition = new SearchTerm() {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean match(Message message) {
          try {
            Address[] recipients = message.getAllRecipients();
            for (Address address : recipients) {
              String recipientsEmail = address.toString().toLowerCase().trim();
              String testRecipientsEmail = orderEmailID.toLowerCase().trim();

              if ((recipientsEmail.contains(testRecipientsEmail))) {
                String wkSubject = message.getSubject().toLowerCase().trim();
                String copeSubject = message.getSubject().toLowerCase().trim();

                boolean wkStatus = wkSubject.contains(welcomeKitSearch.toLowerCase().trim());
                boolean copeStatus = copeSubject.contains(emailCopeSearch.toLowerCase().trim());

                if (wkStatus) {
                  String result = getTextFromMessage(message);
                  email.put("wkEmail", recipientsEmail);
                  email.put("wkSubject", wkSubject);
                  email.put("wkContent", result);
                  return true;
                }
                if (copeStatus) {
                  String result = getTextFromMessage(message);
                  email.put("copeEmail", recipientsEmail);
                  email.put("copeSubject", copeSubject);
                  email.put("copeContent", result);
                  return true;
                }

              }
            }
          } catch (MessagingException ex) {
            ex.printStackTrace();
            return false;
          } catch (IOException e) {
            e.printStackTrace();
            return false;
          }
          return false;
        }
      };

      // performs search through the folder
      Message[] foundMessages = gmailFolder.search(searchCondition);
      for (int i = 0; i < foundMessages.length; i++) {
        Message messagess = foundMessages[i];
        String subject = messagess.getSubject();
        System.out.println("Found message #" + i + ": " + subject + "\n");
      }
      gmailFolder.close(false);
      store.close();
    } catch (Exception mex) {
      mex.printStackTrace();
    }

    boolean status = false;
    if (!emailCopeSearch.equalsIgnoreCase("NULL")) {
      try {
        status = email.get("copeSubject").toLowerCase().trim()
            .contains(emailCopeSearch.toLowerCase().trim());
        if (!emailCopeSearch.equalsIgnoreCase("NULL")) {
          status = email.get("copeSubject").toLowerCase().trim()
              .contains(emailCopeSearch.toLowerCase().trim());

          if (status) {
            Util.printInfo("copeEmail " + email.get("copeEmail"));
            Util.printInfo("copeSubject " + email.get("copeSubject"));
            Util.printInfo("copeContent" + email.get("copeContent"));
          } else {
            AssertUtils.fail("COPE Email validation is not successfull");
          }
        }
      } catch (NullPointerException e) {
        AssertUtils.fail("Cope Email is not found");
      }
    }

    if (!welcomeKitSearch.equalsIgnoreCase("NULL")) {
      try {
        status = email.get("wkSubject").toLowerCase().trim()
            .contains(welcomeKitSearch.toLowerCase().trim());
        if (status) {
          Util.printInfo("wkEmail   ::   " + email.get("wkEmail") + "\n");
          Util.printInfo("wkSubject ::   " + email.get("wkSubject") + "\n");
          Util.printInfo("wkContent ::   " + email.get("wkContent").replace("?", "") + "\n");
        } else {
          AssertUtils.fail("WelcomeKit Email validation  is not successfull");
        }
      } catch (NullPointerException e) {
        AssertUtils.fail("Welcome KIT Email is not found");
      }
    }
    return message;
  }

  public Properties getGmailConnectionProperties() {
    Properties props = new Properties();
    props.setProperty("imap.googlemail.com", "imaps");
    props.setProperty("mail.imaps.ssl.enable", "true");

    props.put("mail.imaps.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "465");
    props.put("mail.smtp.socketFactory.port", "465");
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.auth", "true");
    props.put("mail.imaps.timeout", "120000");
    props.put("mail.imaps.auth", false);
    props.put("mail.imaps.ssl.checkserveridentity", false);
    props.put("mail.imaps.ssl.trust", "*");
    props.put("mail.imaps.ssl.trust", "imap.gmail.com");
    return props;
  }

  private String getTextFromMessage(Message message) throws MessagingException, IOException {
    String result = "";
    if (message.isMimeType("text/plain")) {
      result = message.getContent().toString();
    } else if (message.isMimeType("multipart/*")) {
      MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
      result = getTextFromMimeMultipart(mimeMultipart);
    }
    return result;
  }

  private String getTextFromMimeMultipart(MimeMultipart mimeMultipart)
      throws MessagingException, IOException {
    String result = "";
    int count = mimeMultipart.getCount();
    for (int i = 0; i < count; i++) {
      BodyPart bodyPart = mimeMultipart.getBodyPart(i);
      if (bodyPart.isMimeType("text/plain")) {
        result = result + "\n" + bodyPart.getContent();
        break; // without break same text appears twice in my tests
      } else if (bodyPart.isMimeType("text/html")) {
        String html = (String) bodyPart.getContent();
        result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
      } else if (bodyPart.getContent() instanceof MimeMultipart) {
        result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
      }
    }
    return result;
  }

  //@Step("User Login in Customer Portal " + GlobalConstants.TAG_TESTINGHUB)
  public boolean portalLogin(String portalUserName, String portalPassword) {
    boolean status = false;
    try {
      Util.sleep(2000);
      portalPage.getMultipleWebElementsfromField("usernameCEP").get(0).sendKeys(portalUserName);
      portalPage.getMultipleWebElementsfromField("verfyUserCEPBtn").get(0).click();
      Util.sleep(2000);
      portalPage.getMultipleWebElementsfromField("passCEP").get(0).click();
      portalPage.getMultipleWebElementsfromField("passCEP").get(0).sendKeys(portalPassword);
      Util.sleep(2000);
      portalPage.getMultipleWebElementsfromField("createAccount").get(0).click();

      try {
        if (portalPage.getMultipleWebElementsfromField("skipLink").get(0).isDisplayed()
            || portalPage.isFieldVisible("skipLink")
            || portalPage.checkFieldExistence("skipLink")
            || portalPage.isFieldPresent("skipLink")) {
          Util.printInfo("Skip link is displayed after logging into portal");
          Util.printInfo("Clicking on SkipLink");
          portalPage.clickUsingLowLevelActions("skipLink");
          status = true;
        }
      } catch (Exception e) {
      }
      status = true;
    } catch (Exception e) {
      e.printStackTrace();
      Util.printTestFailedMessage(
          "Portal TestBase class,  portalLogin() :: Failed " + " \n" + e.getMessage());
      AssertUtils.fail("CustomerPortal : Login action unsuccessful... Please try again");
    }

    if (!status) {
      AssertUtils.fail("User Log into Portal :: " + status);
    }

    return status;
  }

  @Step("isPortalElementPresent in GUI load wait")
  public boolean isPortalElementPresent(String Field) {
    boolean status = false;

    try {
      status =
          portalPage.isFieldVisible(Field) || portalPage.checkFieldExistence(Field) || portalPage
              .isFieldPresent(Field) || portalPage.checkIfElementExistsInPage(Field, 60);
    } catch (MetadataException e) {
    }

    if (!status) {
      Util.printError("isPortalElementPresent :: " + Field + "" + status);
    }

    return status;
  }

  @Step("isPortalElementPresent in GUI load wait")
  public boolean isPortalElementPresentWithXpath(String xPath) {
    boolean status = false;

    try {
      WebElement element = driver.findElement(By.xpath(xPath));
      if (element != null) {
        status = true;
      }

    } catch (ElementNotVisibleException e) {
    }

    if (!status) {
      Util.printError("isPortalElementPresentWithXpath :: " + xPath + "" + status);
    }

    return status;
  }

  @Step("check if all the tabs are loading in Portal ")
  public boolean isPortalTabsVisible() {
    boolean status = false, link1 = false, link2 = false, link3 = false;
    status = isPortalElementPresent("portalProductServiceTab");
    Util.printInfo("portalProductServiceTab is loading :: " + status);
    link1 = isPortalElementPresent("portalUMTab");
    Util.printInfo("portalUMTab is loading :: " + link1);
    link2 = isPortalElementPresent("portalBOTab");
    Util.printInfo("portalBOTab is loading :: " + link2);
    link3 = isPortalElementPresent("portalReportingTab");
    Util.printInfo("portalReportingTab is loading :: " + link3);
    return status && (link1 /* && link2 */ || link3);
  }


  public String getStudentSubscription() {
    String student_SubscriptionID = "";
    try {
      clickALLPSLink();
      student_SubscriptionID = driver.findElement(By.xpath("//div[contains(@data-pe-id,'MAYA')]"))
          .getAttribute("data-pe-id").toString().split("_")[2];
      Util.printInfo("student_SubscriptionID : " + student_SubscriptionID);
    } catch (Exception e) {
      AssertUtils.fail("Unable to find subscription ID in Portal");
    }
    return student_SubscriptionID;
  }

  @Step("Portal : Validate subscription" + GlobalConstants.TAG_TESTINGHUB)
  public boolean isSubscriptionDisplayedInPS(String subscriptionID) {
    boolean status = false;
    String productXpath = null;
    try {
      productXpath = portalPage
          .getFirstFieldLocator("subscriptionIDInPS").replace("TOKEN1", subscriptionID);
    } catch (Exception e) {
      AssertUtils.fail(
          "Verify subscription/agreement is displayed in All P&S page step couldn't be completed due to technical issue "
              + e.getMessage());
    }

    Util.printInfo("Check if element with subscriptionId exists on the page.");
    status = isPortalElementPresentWithXpath(productXpath);

    if (!status) {
      AssertUtils.fail(
          ErrorEnum.AGREEMENT_NOTFOUND_CEP.geterr() + " subscriptionID ::  " + subscriptionID
              + " , In P&S page");
    }

    return status;
  }

  //@Step("Verify subscription/agreement is displayed in Subscription page" + GlobalConstants.TAG_TESTINGHUB)
  public boolean isSubscriptionDisplayedInBO(String subscriptionID) {
    clickSubscriptionsLink(subscriptionID);
    boolean status = false;
    String errorMsg = "";
    String productXpath = null;
    try {
      productXpath = portalPage
          .getFirstFieldLocator("subscriptionIDInBO").replace("TOKEN1", subscriptionID);
    } catch (Exception e) {
      //AssertUtils.fail("Verify product is displayed in Subscription page step couldn't be completed due to technical issue " + e.getMessage());
      errorMsg =
          "Verify product is displayed in Subscription page step couldn't be completed due to technical issue "
              + e.getMessage();
    }

    status = isPortalElementPresentWithXpath(productXpath);
    if (!status)
    //AssertUtils.fail(ErrorEnum.AGREEMENT_NOTFOUND_CEP.geterr() + " subscriptionID ::  " + subscriptionID  + " , In B&O page");
    {
      errorMsg =
          ErrorEnum.AGREEMENT_NOTFOUND_CEP.geterr() + " subscriptionID ::  " + subscriptionID
              + " , In B&O page";
    }

    status = errorMsg.isEmpty();

    return status;
  }


  @Step("Click on Contracts link in BO")
  public void clickSubscriptionsLink(String agreement) {
    //clickSubscriptionLink();
    //openPortalURL("https://stg-manage.autodesk.com/cep/#orders/subscriptions/" + agreement);
    openPortalURL("https://stg-manage.autodesk.com/billing/subscriptions-contracts");
  }

  public void navigateToLeftNav() {
    WebElement nav = null;
    try {
      driver.manage().window().maximize();
      driver.manage().window().maximize();
      portalPage.checkIfElementExistsInPage("leftNav", 20);
      portalPage.waitForField("leftNav", true, 20000);
      nav = portalPage.getCurrentWebElement();
      portalPage.moveToElement(nav);
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo("Unable to get Left nav window");
    }
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
      portalPage.waitForPageToLoad();
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
        debugHTMLPage();
        Util.printInfo("Clicking on portal email popup got it button...");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();",
            portalPage.getMultipleWebElementsfromField("portalEmailPopupYesButton").get(0));
        Util.printInfo("HTML code - After Clicking portalEmailPopupYesButton");
        Util.sleep(15000);
        debugHTMLPage();

      }
    } catch (Exception e) {
      Util.printInfo("Email popup does not appeared on screen...");
    }
  }

  @Step("Click Subscription Link and Go To Payment Method Page") //#RAS
  public boolean navToSubscriptionPaymentMethodScreen(String subscriptionID) {
    boolean status = false;
    String productXpath = null;
    try {
//          driver.findElement(By.xpath("//span[(text()='Subscriptions and Contracts')]")).click();
      portalPage.clickUsingLowLevelActions("portalLinkSubscriptions");
      studentPage.waitForPageToLoad();
      Util.sleep(5000);
      try {
        status = false;
        productXpath = portalPage
            .createFieldWithParsedFieldLocatorsTokens("subscriptionRowInSubscription",
                subscriptionID);
      } catch (Exception e) {
        AssertUtils.fail(
            "Verify product is displayed in All P&S page step couldn't be completed due to technical issue "
                + e.getMessage());
      }

      status = isPortalElementPresent(productXpath);
      if (status) {
        driver.findElement(
            By.xpath("//div[@id='subscription'][contains(text(),'" + subscriptionID + "')]"))
            .click();
        status = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
      CustomSoftAssert.s_assert.fail("Unable to go to Subscription Payment Method");
    }
    Util.sleep(10000);
    return status;
  }

  @Step("CEP : Bic Order capture " + GlobalConstants.TAG_TESTINGHUB)
  public boolean validateBICOrderProductInCEP(String cepURL, String portalUserName,
      String portalPassword, String subscriptionID) {
    boolean status = false, statusPS, statusBO, statusBOC, statusBOS, portalLogin, portalLoad = false;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    if (isPortalTabsVisible()) {
      try {
        portalLogin = clickALLPSLink();
        //portalLoad = feynamnLayoutLoaded();
        statusPS = isSubscriptionDisplayedInPS(subscriptionID);
        /*
         * clickContractsLink(); statusBOC =
         * isSubscriptionDisplayedinBO(subscriptionID);
         *
         * clickSubscriptionLink(); statusBOS =
         * isSubscriptionDisplayedinBO(subscriptionID);
         *
         * statusBO= statusBOC||statusBOS;
         */
        status = (statusPS/* && statusBO */);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (!status) {
      AssertUtils.fail("Product is displayed in portal" + " :: false");
    }

    return status;
  }


  public boolean cepLogin(String cepURL, String portalUserName, String portalPassword,
      String subscriptionID) {
    boolean status = false;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    AssertUtils.fail("Product is displayed in portal" + " :: false");

    return status;
  }

  private boolean isPortalLoginPageVisible() {
    boolean status = false;
    try {
      status = portalPage.checkIfElementExistsInPage("createAccountCEP", 60);
    } catch (MetadataException e) {
    }
    return status;
  }

  public HashMap<String, String> createContactAndAssociateAccount(HashMap<String, String> data) {
    createContactFromPortal(data);
    System.out
        .println("****************** mapping contact to end user account *********************");
    matchAccountContactNew(data);
    return data;
  }

  @Step("create contact from Portal")
  public HashMap<String, String> createContactFromPortal(HashMap<String, String> data) {
    //openPortal( CommonConstants.oxygenUserCreationURL);
    String errorMsg = "";
    driver.get(CommonConstants.oxygenUserCreationURL);
    data.put(TestingHubConstants.firstname, "AOFNTH" + generateRandom(5));
    data.put(TestingHubConstants.lastname, "AOLNTH" + generateRandom(5));
    data.put(TestingHubConstants.passCEP, data.get("password"));

    try {
      //portalPage.clickUsingLowLevelActions("createAccountCEP");
      Util.sleep(10000);
      portalPage.populateField("firstNameCEP", data.get(TestingHubConstants.firstname));
      portalPage.populateField("lastNameCEP", data.get(TestingHubConstants.lastname));
      portalPage.populateField("emailIdCEP", data.get(TestingHubConstants.emailid));
      portalPage.populateField("ConfirmEmailCEP", data.get(TestingHubConstants.emailid));
      portalPage.populateField("passCEP", data.get(TestingHubConstants.passCEP));

      portalPage.isFieldVisible("accountExist");
      // Checking if the email already exists in the system or Existing User
      boolean status = false;
      try {
        if (portalPage.getMultipleWebElementsfromField("emailErrText").get(0).getText()
            .equalsIgnoreCase("Please enter a valid email address")) {
          errorMsg = "Please enter a valid email id : " + data.get(TestingHubConstants.emailid);
          Util.printError(errorMsg);
        }
      } catch (Exception e) {
        Util.printError(e.getMessage());
        // errorMsg= e.getMessage();
      }

      status = portalPage.checkIfElementExistsInPage("accountExist", 20);
      if (status) {
        Util.printTestFailedMessage("You are trying to create an Existing user :: " + portalPage
            .getValueFromGUI("accountExist"));
        Util.printTestFailedMessage("given contact '" + data.get(TestingHubConstants.emailid)
            + "' already exists but not synced to downstream system can not be used for order creation");
      } else {
        Util.sleep(2000);
        String CheckboxClick = "document.getElementById(\"privacypolicy_checkbox\").click()";
        clickCheckBox(CheckboxClick);
        Util.sleep(5000);
        portalPage.clickUsingLowLevelActions("createAccount");
        Util.sleep(20000);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        data.put("createdTime", timestamp + "");
        feynamnLayoutLoaded();
        data.put(TestingHubConstants.oxid, getOxygenId(data).trim());
      }
    } catch (Exception e) {
      //  AssertUtils.fail("Error while creating new contact with : " + data.get(TestingHubConstants.emailid));
      Util.printError(
          "Error while creating new contact with : " + data.get(TestingHubConstants.emailid));
      errorMsg = "Error while creating new contact with : " + data.get(TestingHubConstants.emailid);
    }

    try {
      portalPage.clickUsingLowLevelActions("logout");
      Util.sleep(7000);
      portalPage.clickUsingLowLevelActions("signout");
      Util.sleep(10000);
    } catch (Exception e) {
      Util.printError("unable to logout from Portal");
      //AssertUtils.fail("unable to logout from Portal");
    }
    return data;
  }

  @Step("Create new Account and Contact" + GlobalConstants.TAG_TESTINGHUB)
  public LinkedHashMap<String, String> createNewPortalAccount(LinkedHashMap<String, String> data) {
    LinkedHashMap<String, String> createdData = new LinkedHashMap<String, String>();
    openPortal(data.get("cepURL"));
    String emailIdCEP = data.get(TestingHubConstants.emailid);

    if ((emailIdCEP == null) || (emailIdCEP.equals("null"))) {
      emailIdCEP = generateUniqueEmailID("testinghub", generateRandomTimeStamp(), "letscheck.pw");
    }

    createdData.put("firstNameCEP", "TestingHub" + generateRandom(5));
    createdData.put("lastNameCEP", generateRandom(5));
    createdData.put("emailIdCEP", emailIdCEP);

    String cepPass = data.get("password");
    if ((cepPass == null) || (cepPass.equals("null"))) {
      createdData.put("password", data.get("password"));
    } else {
      createdData.put("password", cepPass);
    }

    try {
      portalPage.clickUsingLowLevelActions("createAccountCEP");
      Util.sleep(10000);
      portalPage.populateField("firstNameCEP", createdData.get("firstNameCEP"));
      portalPage.populateField("lastNameCEP", createdData.get("lastNameCEP"));
      portalPage.populateField("emailIdCEP", createdData.get("emailIdCEP"));
      portalPage.populateField("ConfirmEmailCEP", createdData.get("emailIdCEP"));
      portalPage.populateField("passCEP", createdData.get("password"));
      portalPage.isFieldVisible("accountExist");

      // Checking if the email already exists in the system or Existing User
      boolean status = false;
      status = portalPage.checkIfElementExistsInPage("accountExist", 20);

      if (status) {
        Util.printTestFailedMessage("You are trying to create an Existing user :: " + portalPage
            .getValueFromGUI("accountExist"));
        AssertUtils.fail(
            ErrorEnum.EXISTING_USER_CEP.geterr() + " : " + data.get(TestingHubConstants.emailid));
      }

      Util.sleep(2000);
      String CheckboxClick = "document.getElementById(\"privacypolicy_checkbox\").click()";
      clickCheckBox(CheckboxClick);
      Util.sleep(5000);
      portalPage.clickUsingLowLevelActions("createAccount");
      Util.sleep(15000);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Error while creating new Portal user");
    }
    return createdData;
  }

  //@Step("Verify Credit Card Details" + GlobalConstants.TAG_TESTINGHUB)
  public String verifyCreditCardDetails(String subscriptionID, String ccdetails) {
    String errorMsg = "";
    String productXpath = null;
    try {

      if (!portalPage.checkIfElementExistsInPage("portalReportUpPay", 20)) {
        //AssertUtils.fail(ErrorEnum.REPORTINGTAB_BO_UP.geterr());
        errorMsg = ErrorEnum.REPORTINGTAB_BO_UP.geterr();
        return errorMsg;
      } else {
        portalPage.clickUsingLowLevelActions("portalReportUpPay");
      }

      productXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("creditCardDetails", ccdetails);
      if (!(portalPage.checkIfElementExistsInPage(productXpath, 20))) {
        // AssertUtils.fail(ErrorEnum.PAYMENTMETHOD_NOTFOUND_CEP.geterr() + " paymentDetails " + ccdetails);
        errorMsg = ErrorEnum.PAYMENTMETHOD_NOTFOUND_CEP.geterr() + " paymentDetails " + ccdetails;
        return errorMsg;
      }
    } catch (Exception e) {
      e.printStackTrace();
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " or Validate Credit Card Details");
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " or Validate Credit Card Details";
    }
    return errorMsg;
  }

  //@Step("Verify Billing Address Details" + GlobalConstants.TAG_TESTINGHUB)
  public String verifyBillingAddress(LinkedHashMap<String, String> testDataForEachMethod) {
    boolean status = false;
    String errorMsg = "";
    status = navToSubscriptionPaymentMethodScreen(testDataForEachMethod.get("subscriptionID"));

    if (status) {
      String billingName = testDataForEachMethod.get("billingName");
      String billingStreet = testDataForEachMethod.get("billingStreet");
      String billingAddress = testDataForEachMethod.get("billingAddress");
      String billingStreetAddress = billingStreet + ' ' + billingAddress;

      String address = portalPage.getTextFromLink("portalAddressLink");
      address = address.replace("\n", " ");
      Util.printInfo(address);
      //1419WLakeSt               Chicago, IL 60607-1409subscriptionIDInBO
      String billingNameXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("billingName", billingName);
      //String billingStreetXpath = portalPage.createFieldWithParsedFieldLocatorsTokens("subscriptionIDInBO", billingStreet);
      String billingAddressXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("portalAddressLink", billingStreetAddress);

      try {
        if (!(portalPage.checkIfElementExistsInPage(billingNameXpath, 20))) {
          //AssertUtils.fail(ErrorEnum.BILLING_MISMATCH_CEP.geterr());
          errorMsg = ErrorEnum.BILLING_MISMATCH_CEP.geterr();
          Util.printInfo("Actual Address :" + address);
          Util.printInfo("Expected  Address :" + billingStreetAddress);
          return errorMsg;
        }

        if (!address.equalsIgnoreCase(billingStreetAddress)) {
          //AssertUtils.fail(ErrorEnum.BILLING_MISMATCH_CEP.geterr());
          errorMsg = ErrorEnum.BILLING_MISMATCH_CEP.geterr();
          return errorMsg;
        }
      } catch (MetadataException ee) {
        //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " or Validate Billing Address Details ");
        errorMsg =
            ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " or Validate Billing Address Details ";
      }
    }
    return errorMsg;
  }

  //@Step("Verify Payment Option" + GlobalConstants.TAG_TESTINGHUB)
  public String verifyAddCardOption(String subscriptionID) {
    String errorMsg = "";
    try {
      boolean status = false;
      //go to change payment method screen

      //status =navToSubscriptionPaymenMethodScreen(subscriptionID);
      openPortalURL("https://stg-manage.autodesk.com/cep/#orders/subscriptions/" + subscriptionID);
      Util.sleep(12000);
      Util.printInfo("test");
      boolean statusExpired = portalPage
          .checkIfElementExistsInPage("autoRenewStatExpiring", 15); // turned off  state
      Util.PrintInfo("Status expired flag : " + statusExpired);

      if (statusExpired) {
        turnOnRenewal();
        Util.sleep(15000);
        printBackAutoExpiresStatus();
        if (!portalPage.checkIfElementExistsInPage("autoRenewStatRenewing", 15)) {
          errorMsg = "Auto renew status is not getting Updated after TURN ON renewal";
          return errorMsg;
          //AssertUtils.fail("Auto renew status is not getting Updated after TURN ON renewal");
        }
      }

      Util.printInfo("changeCardDetails");
      if (portalPage.checkIfElementExistsInPage("changeCardDetails", 20)) {
        portalPage.clickUsingLowLevelActions("changeCardDetails");
        Util.printInfo("changeCardDetails done");
        Util.sleep(20000);
        if (!portalPage.checkIfElementExistsInPage("changePaymentModal", 20)) {
          //AssertUtils.fail(ErrorEnum.PAYMENT_MODAL_CEP.geterr());
          errorMsg = ErrorEnum.PAYMENT_MODAL_CEP.geterr();
          return errorMsg;
        } else {
          Util.PrintInfo("change Payment mehtod header is displayed");
        }

        // Check if Add CreditCard and PayPal buttons displayed
        if (portalPage.checkIfElementExistsInPage("portalCreditCardButton", 20) && portalPage
            .checkIfElementExistsInPage("portalPayPalButton", 20)) {
          Util.PrintInfo("Credit Card and PayPal buttons are displayed.");
        } else {
          Util.PrintInfo("Credit Card and PayPal buttons are NOT displayed.");
          //AssertUtils.fail(ErrorEnum.PAYMENT_OPTION_CEP.geterr());
          errorMsg = ErrorEnum.PAYMENT_OPTION_CEP.geterr();
          return errorMsg;
        }
        // CLick on add new card link
        portalPage.clickUsingLowLevelActions("addNewCardLink");
        Util.sleep(5000);

        if (!(portalPage.checkIfElementExistsInPage("addNewPaymentInfor", 20))) {
          //AssertUtils.fail(ErrorEnum.PAYMENT_OPTION_CEP.geterr());
          errorMsg = ErrorEnum.PAYMENT_OPTION_CEP.geterr();
          return errorMsg;
        } else {
          Util.PrintInfo("Add new payment information header displayed.");
        }

        driver.switchTo().defaultContent();
      } else {
        Util.PrintInfo("Payment method change element not present");
      }
    } catch (Exception e) {
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " CreditCard Validation");
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " CreditCard Validation";
    }
    return errorMsg;
  }

  //@Step("Verify Billing Address Edit Option" + GlobalConstants.TAG_TESTINGHUB)
  public String verifyBillingEditOption(String subscriptionID) {
    String errorMsg = "";
    try {
      boolean status = false;
      //go to change payment method screen
      //status =navToSubscriptionPaymenMethodScreen(subscriptionID);
      openPortalURL("https://stg-manage.autodesk.com/cep/#orders/subscriptions/" + subscriptionID);

      boolean statusExpired = portalPage
          .checkIfElementExistsInPage("autoRenewStatExpiring", 15); // turned off  state
      if (statusExpired) {
        turnOnRenewal();
        Util.sleep(10000);
        printBackAutoExpiresStatus();
        if (!portalPage.checkIfElementExistsInPage("autoRenewStatRenewing", 15)) {
          errorMsg = "Auto renew status is not getting Updated after TURN ON renewal";
          //AssertUtils.fail("Auto renew status is not getting Updated after TURN ON renewal");
          return errorMsg;
        }
      }

      portalPage.clickUsingLowLevelActions("changeCardDetails");
      Util.sleep(20000);

      Util.PrintInfo("changeCardDetails");
      portalPage.clickUsingLowLevelActions("billingInfoEdit");
      Util.sleep(10000);

      Util.PrintInfo("billingInfoEdit");

      if (!portalPage.checkIfElementExistsInPage("editBillingModal", 20)) {
        errorMsg = ErrorEnum.BILLING_MODAL_CEP.geterr();
        //AssertUtils.fail(ErrorEnum.BILLING_MODAL_CEP.geterr());
        return errorMsg;
      }

      Util.PrintInfo("editBillingModal");
      // Switch to iFrame
      //driver.switchTo().frame("bmgr-iframe");

      if (!(portalPage.checkIfElementExistsInPage("billingModalHeader", 20))) {
        errorMsg = ErrorEnum.BILLING_MODAL_CEP.geterr();
        //AssertUtils.fail(ErrorEnum.BILLING_MODAL_CEP.geterr());
        Util.PrintInfo("billingModalHeader");
        return errorMsg;
      }
      if (portalPage.checkIfElementExistsInPage("billingModalCancel", 20)) {
        portalPage.clickUsingLowLevelActions("billingModalCancel");
      }

      Util.PrintInfo("billingModalCancel");
      //  }

      //  else
      //  AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " or validate Edit Billing Address action");
    } catch (Exception e) {
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " or validate Edit Billing Address action");
      errorMsg =
          ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " or validate Edit Billing Address action";
    }
    return errorMsg;
  }

  //@Step("AUM- Add Invite User " + GlobalConstants.TAG_TESTINGHUB)
  public LinkedHashMap<String, String> gotoAUMByUserPageInviteUser() {
    LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
    String errorMsg = "";
    String fn = "Fn" + generateRandom(6);
    String ln = "Ln" + generateRandom(4);
    String email = fn + "." + ln + "@letscheck.pw";
    result.put("email", email);

    try {
      portalPage.click("portalLinkByUser");
      Util.sleep(5000);
      if (!portalPage.checkIfElementExistsInPage("aumInviteUserButton", 20)) {
        //AssertUtils.fail(ErrorEnum.INVITE_USER_ACTION.geterr() + "Invite User Button is not Visible");
        errorMsg = ErrorEnum.INVITE_USER_ACTION.geterr() + "Invite User Button is not Visible";
        result.put("errorMsg", errorMsg);
        return result;
      }
      portalPage.clickUsingLowLevelActions("aumInviteUserButton");

      if (!portalPage.checkIfElementExistsInPage("inviteSingle", 20)) {
        //AssertUtils.fail(ErrorEnum.INVITE_USER_ACTION.geterr() + "Add User Modal is not loading");
        errorMsg = ErrorEnum.INVITE_USER_ACTION.geterr() + "Add User Modal is not loading";
        result.put("errorMsg", errorMsg);
        return result;
      }

      //populate firstName
      portalPage.populateField("aumAddUserFname", fn);
      Util.sleep(2000);

      portalPage.populateField("aumAddUserLname", ln);
      Util.sleep(2000);

      portalPage.populateField("aumAddUserEmail", email);
      portalPage.clickUsingLowLevelActions("inviteEndUser");
      Util.sleep(2000);

      if (!portalPage.checkIfElementExistsInPage("inviteSentSuccess", 20)) {
        //AssertUtils.fail(ErrorEnum.INVITE_USER_ACTION.geterr());
        errorMsg = ErrorEnum.INVITE_USER_ACTION.geterr();
      } else {
        Util.PrintInfo("Invite user is successful");
        portalPage.clickUsingLowLevelActions("inviteDoneButton");
        Util.PrintInfo("Invite user Done button click");
      }
    } catch (MetadataException e) {
      e.printStackTrace();
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " AUM- Add Invite User ");
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " AUM- Add Invite User ";
    }
    result.put("errorMsg", errorMsg);
    return result;
  }

  //@Step("AUM- Delete Invited User " + GlobalConstants.TAG_TESTINGHUB)
  public String deleteAUMInvitedUser(String invitedUserEmail) {
    String errorMsg = "";
    try {
      portalPage.click("portalLinkByUser");
      Util.sleep(10000);

      String deleteInviteUserXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("deleteUserGrid", invitedUserEmail);
      if (!portalPage.checkIfElementExistsInPage(deleteInviteUserXpath, 20)) {
        //AssertUtils.fail(ErrorEnum.DELETE_USER_ACTION.geterr());
        errorMsg = ErrorEnum.DELETE_USER_ACTION.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions(deleteInviteUserXpath);

      if (!portalPage.checkIfElementExistsInPage("deleteUserIcon", 20)) {
        //AssertUtils.fail(ErrorEnum.DELETE_USER_ACTION.geterr() + " delete icon is not visible");
        errorMsg = ErrorEnum.DELETE_USER_ACTION.geterr() + " delete icon is not visible";
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("deleteUserIcon");

      Util.PrintInfo("Delete icon is visible");
      Util.sleep(10000);

      if (!portalPage.checkIfElementExistsInPage("deleteButton", 20)) {
        //AssertUtils.fail(ErrorEnum.DELETE_USER_ACTION.geterr() + " confirm delete action");
        errorMsg = ErrorEnum.DELETE_USER_ACTION.geterr() + " confirm delete action";
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("deleteButton");

      Util.PrintInfo("Delete icon is clicked");
      Util.sleep(10000);
      if (portalPage.checkIfElementExistsInPage(deleteInviteUserXpath, 20)) {
        //AssertUtils.fail(ErrorEnum.DELETE_USER_ACTION.geterr());
        errorMsg = ErrorEnum.DELETE_USER_ACTION.geterr();
        return errorMsg;
      }
    } catch (MetadataException e) {
      e.printStackTrace();
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " AUM- Delete Invited User");
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " AUM- Delete Invited User";
    }
    return errorMsg;
  }

  //@Step("AUM- Change User to Secondary Admin role " + GlobalConstants.TAG_TESTINGHUB)
  public String changeAUMUserToSecondaryAdminRole(String inviteUserName) {
    String errorMsg = "";
    try {
      portalPage.click("portalLinkByUser");
      Util.sleep(10000);

      String userXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("chooseuser", inviteUserName);
      if (!portalPage.checkIfElementExistsInPage(userXpath, 20)) {
        //AssertUtils.fail(ErrorEnum.CHANGEROLE_USER_ACTION.geterr() );
        errorMsg = ErrorEnum.CHANGEROLE_USER_ACTION.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions(userXpath);

      Util.PrintInfo("User selected");
      Util.sleep(10000);

      if (!portalPage.checkIfElementExistsInPage("changeRoleLink", 20)) {
        //AssertUtils.fail(ErrorEnum.CHANGEROLE_USER_ACTION.geterr()  );
        errorMsg = ErrorEnum.CHANGEROLE_USER_ACTION.geterr();
        return errorMsg;
      }

      portalPage.clickUsingLowLevelActions("changeRoleLink");
      Util.PrintInfo("Change Url is visible and clicked");
      Util.sleep(10000);

      String isChecked = portalPage.getAttribute("radiobuttonSecondaryAdmin", "checked");
      if (isChecked != null && isChecked.equalsIgnoreCase("true")) {
        // change back to User first
        if (!portalPage.checkIfElementExistsInPage("radiobuttonUser", 20)) {
          //AssertUtils.fail(ErrorEnum.CHANGEROLE_USER_ACTION.geterr()  );
          errorMsg = ErrorEnum.CHANGEROLE_USER_ACTION.geterr();
          return errorMsg;
        }

        portalPage.clickUsingLowLevelActions("radiobuttonUser");
        Util.PrintInfo("Radiobutton User  is clicked");
        Util.sleep(10000);

        if (!portalPage.checkIfElementExistsInPage("roleChangeSaveButton", 20)) {
          //AssertUtils.fail(ErrorEnum.CHANGEROLE_USER_ACTION.geterr()  );
          errorMsg = ErrorEnum.CHANGEROLE_USER_ACTION.geterr();
          return errorMsg;
        }
        portalPage.clickUsingLowLevelActions("roleChangeSaveButton");
        Util.PrintInfo("Clicked on save button");
        Util.sleep(10000);

        if (!portalPage.checkIfElementExistsInPage("changeRoleLink", 20)) {
          //AssertUtils.fail(ErrorEnum.CHANGEROLE_USER_ACTION.geterr()  );
          errorMsg = ErrorEnum.CHANGEROLE_USER_ACTION.geterr();
          return errorMsg;
        }
        portalPage.clickUsingLowLevelActions("changeRoleLink");
        Util.PrintInfo("Change Url is visible and clicked");
        Util.sleep(10000);
      }

      if (!portalPage.checkIfElementExistsInPage("radiobuttonSecondaryAdmin", 20)) {
        //AssertUtils.fail(ErrorEnum.CHANGEROLE_USER_ACTION.geterr()  );
        errorMsg = ErrorEnum.CHANGEROLE_USER_ACTION.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("radiobuttonSecondaryAdmin");
      Util.PrintInfo("Radiobutton Secondary Admin  is clicked");

      if (!portalPage.checkIfElementExistsInPage("roleChangeSaveButton", 20)) {
        //AssertUtils.fail(ErrorEnum.CHANGEROLE_USER_ACTION.geterr()  );
        errorMsg = ErrorEnum.CHANGEROLE_USER_ACTION.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("roleChangeSaveButton");
      Util.PrintInfo("Clicked on save button");
    } catch (MetadataException e) {
      e.printStackTrace();
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() );
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr();
    }
    return errorMsg;
  }

  //@Step("AUM- AutoRenew Turn OnOff " + GlobalConstants.TAG_TESTINGHUB)
  public String verifyAutoRenewOnOff(String subscriptionID) {
    String errorMsg = "";
    try {
      //navigate to Auto renew on off screen
      //navToSubscriptionPaymenMethodScreen(subscriptionID);
      //clickSubscriptionsLink(subscriptionID);
      //boolean status=false;
      openPortalURL("https://stg-manage.autodesk.com/cep/#orders/subscriptions/" + subscriptionID);
      Util.sleep(7000);
      errorMsg = clickOnActiveSubscriptionInBO();
      if (!errorMsg.trim().isEmpty()) {
        Util.printInfo("Auto Renew On/Off is working");
      }

    } catch (Exception e) {
      e.printStackTrace();
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " AUM- Delete Invited User";
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " AUM- Delete Invited User");
    }
    return errorMsg;
  }

  public String getTimeStamp() {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    Date date = new Date();
    long gmtMilliSeconds = date.getTime();
    return Long.toString(gmtMilliSeconds / 1000).trim();
  }

  //@Step("User Management/ NAMU - Add Invite User " + GlobalConstants.TAG_TESTINGHUB)
  public LinkedHashMap<String, String> gotoUMPageInviteUser(String agreement) {
    LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
    String errorMsg = "";
    String fn = "Fn";
    String ln = "Ln" + generateRandom(4);
    String email = "order" + "." + getTimeStamp() + "@letscheck.pw";
    result.put("email", email);

    try {
      // portalUMClassic
      Util.printInfo("Check for Hybrid Classic UM");
      //navigateToLeftNav();

      if (portalPage.checkIfElementExistsInPage("portalUMClassic", 20)) {
        portalPage.clickUsingLowLevelActions("portalUMClassic");
      } else {
        Util.printInfo("Check for Legacy UM");
      }

      driver.manage().window().maximize();
      portalPage.clickUsingLowLevelActions("portalUMTab"); //failing here th

      Util.sleep(15000);
      Util.printInfo("User Management redirection link :: " + driver.getCurrentUrl());

      if (!portalPage.checkIfElementExistsInPage("umAddUserButton", 40)) {
        //AssertUtils.fail(ErrorEnum.INVITE_USER_ACTION.geterr() + ", Add User Button is not Visible");
        errorMsg = ErrorEnum.INVITE_USER_ACTION.geterr() + "Add User Button is not Visible";
        result.put("errorMsg", errorMsg);
        return result;
      }
      Util.printInfo("Click on Add User Button");
      portalPage.clickUsingLowLevelActions("umAddUserButton");
      Util.sleep(10000);

      Util.printInfo("Populate Email address");
      if (!portalPage.checkIfElementExistsInPage("umEmailInput", 20)) {
        //AssertUtils.fail(ErrorEnum.INVITE_USER_ACTION.geterr() + ", Email input field is not Visible");
        errorMsg = ErrorEnum.INVITE_USER_ACTION.geterr() + "Email input field is not Visible";
        result.put("errorMsg", errorMsg);
        return result;
      }

      portalPage.populateField("umEmailInput", email.trim());
      portalPage.populateField("umFNInput", fn);
      portalPage.populateField("umLNInput", ln);
      Util.printInfo("ClicK on save Add User Button");
      portalPage.clickUsingLowLevelActions("saveContinueButton");
      Util.sleep(15000);

      if (!portalPage.checkIfElementExistsInPage("umEditAccessModal", 20)) {
        //AssertUtils.fail(ErrorEnum.INVITE_USER_ACTION.geterr());
        errorMsg = ErrorEnum.INVITE_USER_ACTION.geterr();
        result.put("errorMsg", errorMsg);
        return result;
      }
    } catch (Exception e) {
      e.printStackTrace();
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " UM- Add Invite User");
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " UM- Add Invite User";
      result.put("errorMsg", errorMsg);
      return result;
    }
    result.put("errorMsg", errorMsg);
    return result;
  }

  //@Step("User Management/ NAMU - Assign all Products & Benefits " + GlobalConstants.TAG_TESTINGHUB)
  public String assignAllBenefits(String subscriptionID) {
    String errorMsg = "";
    try {
      String umtoggleSupportXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("umtoggleSupport", subscriptionID);
      if (!portalPage.checkIfElementExistsInPage(umtoggleSupportXpath, 20)) {
        //AssertUtils.fail(ErrorEnum.UM_EDITACCESS_CEP.geterr() + ", Support Access Benefits is not loading");
        //{softAssertobj.fail(ErrorEnum.UM_EDITACCESS_CEP.geterr() + ", Support Access Benefits is not loading");
        errorMsg =
            ErrorEnum.UM_EDITACCESS_CEP.geterr() + ", Support Access Benefits is not loading";
        return errorMsg;
      }

      if (!portalPage.checkIfElementExistsInPage(umtoggleSupportXpath, 20)) {
//              {softAssertobj.fail("Support Access Benefits is not loading");
//              return errorMsg;
//              }
        errorMsg = "Support Access Benefits is not loading";
        return errorMsg;
      }

      portalPage.clickUsingLowLevelActions(umtoggleSupportXpath);
      portalPage.clickUsingLowLevelActions("umWebSupportCheckbox");
      portalPage.clickUsingLowLevelActions(umtoggleSupportXpath);
      portalPage.clickUsingLowLevelActions("umProducts&ServicesCB");
      portalPage.clickUsingLowLevelActions("umEditModalSave");
      Util.sleep(10000);
    } catch (MetadataException e) {
      //softAssertobj.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() +" assign All Product and Benefits in User Management page");
      {
        errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr()
            + " assign All Product and Benefits in User Management page";
        return errorMsg;
      }
    }
    return errorMsg;
  }

  //@Step("User Management/ NAMU - Remove Invited User " + GlobalConstants.TAG_TESTINGHUB)
  public String removeInvitedUser(String invitedUser) {
    String errorMsg = "";
    try {
      String umRemoveInviteUserXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("umRemoveInviteUser", invitedUser);
      if (!portalPage.checkIfElementExistsInPage(umRemoveInviteUserXpath, 20)) {
//              AssertUtils.fail(ErrorEnum.DELETE_USER_ACTION.geterr() + ", Delete User icon is not Visible");
        errorMsg = ErrorEnum.DELETE_USER_ACTION.geterr() + ", Delete User icon is not Visible";
        return errorMsg;
      }

      portalPage.clickUsingLowLevelActions(umRemoveInviteUserXpath);
      Util.sleep(5000);
      portalPage.clickUsingLowLevelActions("umConfirmRemove");
      Util.sleep(10000);

      if (portalPage.checkIfElementExistsInPage(umRemoveInviteUserXpath, 20)) {
        // AssertUtils.fail(ErrorEnum.DELETE_USER_ACTION.geterr());
        errorMsg = ErrorEnum.DELETE_USER_ACTION.geterr();
        return errorMsg;
      }
    } catch (Exception e) {
      // AssertUtils.fail(ErrorEnum.DELETE_USER_ACTION.geterr());
      {
        errorMsg = ErrorEnum.DELETE_USER_ACTION.geterr();
        return errorMsg;
      }
    }
    return errorMsg;
  }

  //@Step("View My Support Case Link validation " + GlobalConstants.TAG_TESTINGHUB)
  public String verifyViewMySupportCaseLinkDisplayed() {
    String errorMsg = "";
    //clickALLPSLink();
    try {
      // Check if Help ICon is displayed then Click on the icon
      if (!portalPage.checkIfElementExistsInPage("helpIconCEP", 20)) {
        //AssertUtils.fail(ErrorEnum.HELP_ICON_CEP.geterr());
        errorMsg = ErrorEnum.HELP_ICON_CEP.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("helpIconCEP");

      Util.PrintInfo("Help icon is visible");
      //Check if ViewSupport Links is displayed return true if displayed

      if (!portalPage.checkIfElementExistsInPage("Viewmysupportcases", 20)) {
        //AssertUtils.fail(ErrorEnum.VIEWSUPPORT_LINK_CEP.geterr());
        errorMsg = ErrorEnum.VIEWSUPPORT_LINK_CEP.geterr();
        return errorMsg;
      }

      Util.PrintInfo("View My Support Cases link is visible");

      //Close modal
      portalPage.clickUsingLowLevelActions("helpIconCEP");
    } catch (MetadataException e) {
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " and validate View_My_Support_Case Link in CEP");
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr()
          + " and validate View_My_Support_Case Link in CEP";
    }
    return errorMsg;
  }

  //@Step(" Reporting Tab - Cloud Service Usage validation with check of 100 cloud credits " + GlobalConstants.TAG_TESTINGHUB)
  public String verifyReportingCloudServiceUsageLinkDisplayed() {
    String errorMsg = "";
    try {
      //navigateToLeftNav();
      if (!portalPage.checkIfElementExistsInPage("portalReportCSULink", 20)) {
//              AssertUtils.fail(ErrorEnum.REPORTINGTAB_CSU_LINK.geterr());
        errorMsg = ErrorEnum.REPORTINGTAB_CSU_LINK.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("portalReportCSULink");
      Util.sleep(15000);
      portalPage.clickUsingLowLevelActions("report_CloudServiceMyUsage");
      if (!portalPage.checkIfElementExistsInPage("report_CloudServiceIndUsage", 20)) {
        //AssertUtils.fail(ErrorEnum.REPORTINGTAB_CSU_LINK.geterr());
        errorMsg = ErrorEnum.REPORTINGTAB_CSU_LINK.geterr();
        return errorMsg;
      }

      if (!portalPage.checkIfElementExistsInPage("report_CloudServiceIndCC", 20)) {
        //AssertUtils.fail(ErrorEnum.REPORTINGTAB_CSU_LINK.geterr());
        errorMsg = ErrorEnum.REPORTINGTAB_CSU_LINK.geterr();
        return errorMsg;
      }

      if (!portalPage.checkIfElementExistsInPage("reportCloudServiceCC100", 20)) {
        //AssertUtils.fail(ErrorEnum.REPORTINGTAB_CSU_CC100.geterr());
        errorMsg = ErrorEnum.REPORTINGTAB_CSU_CC100.geterr();
        return errorMsg;
      }
    } catch (Exception e) {
      // e.printStackTrace();
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr();
    }
    return errorMsg;
  }

  //@Step("P & S Tab - Product Download Enabled Validation" + GlobalConstants.TAG_TESTINGHUB)
  public String verifyProductDownload() {
    //boolean status = false;
    String errorMsg = "";
    try {
      //navigateToLeftNav();
      //click all p &s
      if (!portalPage.checkIfElementExistsInPage("portalAllPSLink", 20)) {
        //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD.geterr());
        errorMsg = ErrorEnum.PRODUCT_DOWNLOAD.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("portalAllPSLink");
      Util.PrintInfo("clicked All Product & Services");
      Util.sleep(7000);

      //check if Download Now button exists and enabled
      if (!portalPage.checkIfElementExistsInPage("viewDownloads", 20)) {
        //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD_VIEW.geterr());
        errorMsg = ErrorEnum.PRODUCT_DOWNLOAD.geterr();
        return errorMsg;
      }

      portalPage.clickUsingLowLevelActions("viewDownloads");
      Util.PrintInfo("Clicked View Downloads");
      Util.sleep(7000);
      //check if Download Now button exists and enabled
      if (!portalPage.isFieldEnabled("downloadNow")) {
        //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD_BUTTON.geterr());
        errorMsg = ErrorEnum.PRODUCT_DOWNLOAD_BUTTON.geterr();
        return errorMsg;
      }

      Util.PrintInfo("Download Now button is enabled");
      Util.sleep(5000);

      //1 dropdown Version
      //*[@id="version"]//button[@class="btn btn-config btn-dl-white"]  dropdownVersion
      //check if Version dropdown is enabled
      if (portalPage.checkIfElementExistsInPage("dropdownVersion", 20)) {
        if (!portalPage.isFieldEnabled("dropdownVersion")) {
          //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD_VERSION.geterr());
          errorMsg = ErrorEnum.PRODUCT_DOWNLOAD_VERSION.geterr();
          return errorMsg;
        }
        Util.PrintInfo("Version dropdown is enabled");
        Util.sleep(5000);
      } else {
        //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD.geterr());
        errorMsg = ErrorEnum.PRODUCT_DOWNLOAD.geterr();
        return errorMsg;
      }

      //2 dropdown Platform
      //*[@id="platform"]//button[@class="btn btn-config btn-dl-white"] dropdownPlatform
      //check if Platform dropdown is enabled
      if (portalPage.checkIfElementExistsInPage("dropdownVersion", 20)) {
        if (!portalPage.isFieldEnabled("dropdownPlatform")) {
          //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD_PLATFORM.geterr());
          errorMsg = ErrorEnum.PRODUCT_DOWNLOAD_PLATFORM.geterr();
          return errorMsg;
        }
        Util.PrintInfo("Platform dropdown is enabled");
        Util.sleep(5000);
      } else {
        //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD.geterr());
        errorMsg = ErrorEnum.PRODUCT_DOWNLOAD.geterr();
        return errorMsg;
      }

      //3 dropdown Language
      //*[@id="languageCode"]//button[@class="btn btn-config btn-dl-white"] dropdownLangauge
      //check if Language dropdown is enabled
      if (portalPage.checkIfElementExistsInPage("dropdownVersion", 20)) {
        if (!portalPage.isFieldEnabled("dropdownLangauge")) {
          //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD_LANGUAGE.geterr());
          errorMsg = ErrorEnum.PRODUCT_DOWNLOAD_LANGUAGE.geterr();
          return errorMsg;
        }
        Util.PrintInfo("Language dropdown is enabled");
        Util.sleep(5000);
      } else {
        //AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD.geterr());
        errorMsg = ErrorEnum.PRODUCT_DOWNLOAD.geterr();
        return errorMsg;
      }

//
//          //click on close form
//          if (!portalPage.checkIfElementExistsInPage("downloadFormClose", 20))
//              AssertUtils.fail(ErrorEnum.PRODUCT_DOWNLOAD.geterr());
//          portalPage.clickUsingLowLevelActions("downloadFormClose");
//          Util.PrintInfo("Closed View Download Form");//
    } catch (Exception e) {
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " verify product download in CEP");
      errorMsg = ErrorEnum.GENERIC_EXPECTION_ACTION.geterr();
      return errorMsg;
    }
    return errorMsg;
  }

  @Step(" Switch from Classic To AUM View" + GlobalConstants.TAG_TESTINGHUB)
  public boolean switchClassicToAUMView(String email) {
    boolean status = false;
    try {
      navigateToLeftNav();

      //click classic user mgmt menu
      if (!portalPage.checkIfElementExistsInPage("classicUserMgmt", 20)) {
        AssertUtils.fail(ErrorEnum.SWITCH_CLASSIC_USER_MGMT.geterr());
      }
      portalPage.clickUsingLowLevelActions("classicUserMgmt");
      Util.printInfo("Clicked on link Classic user mgmt");
      Util.sleep(10000);

      if (!portalPage.checkIfElementExistsInPage("classicUserMgmtarrrowOpen", 20)) {
        AssertUtils.fail(ErrorEnum.SWITCH_CLASSIC_USER_MGMT_DETAIL.geterr());
      }
      portalPage.clickUsingLowLevelActions("classicUserMgmtarrrowOpen");
      Util.printInfo("Clicked on down arrow ");
      Util.sleep(10000);
      //test
      String userXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("classicUserMgmtUserEmail", email);
      if (!portalPage.checkIfElementExistsInPage(userXpath, 20)) {
        AssertUtils.fail(ErrorEnum.SWITCH_CLASSIC_USER_MGMT_EMAIL.geterr());
      }
      Util.printInfo("Checked if email exists ");
      Util.sleep(10000);

      navigateToLeftNav();
      //click by user menu

      openPortalURL("https://customer-stg.autodesk.com/user-access/users/user-list");
            /*
            boolean isPresent =portalPage.verifyFieldExists("portalLinkByUser");
            if (isPresent)
            {
                portalPage.clickUsingLowLevelActions("portalLinkByUser");
                Util.printInfo("Checked on By User ");
                Util.sleep(10000);
            }
            else
                AssertUtils.fail(ErrorEnum.SWITCH_CLASSIC_AUM_VIEW.geterr());
            */
      String nativeUserXpath = portalPage
          .createFieldWithParsedFieldLocatorsTokens("userMgmtUserEmail", email);
      if (!portalPage.checkIfElementExistsInPage(nativeUserXpath, 20)) {
        AssertUtils.fail(ErrorEnum.SWITCH_CLASSIC_AUM_VIEW.geterr());
      }
      Util.printInfo("Checked if email exists ");
      Util.sleep(10000);
      status = true;
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail(ErrorEnum.SWITCH_CLASSIC_AUM_VIEW.geterr());
    }
    return status;
  }

  //@Step( "Help - Contact Support link "+ GlobalConstants.TAG_TESTINGHUB )
  public String clickOnHelpContactSupport() {
    String errorMsg = "";
    //boolean status = false;
    //clickALLPSLink();
    try {
      // Check if Help ICon is displayed then Click on the icon
      Util.sleep(7000);
      if (!portalPage.checkIfElementExistsInPage("helpIconCEP", 30)) {
        //AssertUtils.fail(ErrorEnum.HELP_ICON_CEP.geterr());
        errorMsg = ErrorEnum.HELP_ICON_CEP.geterr();
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("helpIconCEP");
      Util.printInfo("helpIconCEP clicked");

      Util.sleep(20000);

      // Check if ContactSupport Links is displayed return true if displayed
      if (!portalPage.checkIfElementExistsInPage("helpContactSupport", 20)) {
        //AssertUtils.fail(ErrorEnum.CONTACTSUPPORT_LINK_CEP.geterr());
        errorMsg = ErrorEnum.CONTACTSUPPORT_LINK_CEP.geterr();
        return errorMsg;
      }
      Util.sleep(7000);

      //Store the current Window /Tab info
      String winHandleBeforeToggle = driver.getWindowHandle();
      String titleBeforeToggle = driver.getTitle().trim();

      // Toggle to Contact Support Page
      portalPage.clickUsingLowLevelActions("helpContactSupport");
      Util.printInfo("helpContactSupport clicked");
      Util.sleep(5000);

      ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles());
      tabs.remove(winHandleBeforeToggle);
      driver.switchTo().window(tabs.get(0));

      String contactSupportURL = driver.getCurrentUrl();
      if (!(contactSupportURL.toLowerCase().contains("contact"))) {
        Util.printTestFailedMessage(
            "Expected URL :: https://knowledge.autodesk.com/contact-support");
        //AssertUtils.fail(ErrorEnum.CONTACTSUPPORT_LINK_CEP.geterr()+" Page URL ::"+ contactSupportURL);
        errorMsg = ErrorEnum.CONTACTSUPPORT_LINK_CEP.geterr() + " Page URL ::" + contactSupportURL;
        return errorMsg;
      }

      String contactPageTitle = driver.getTitle();
      if (!(contactPageTitle.toLowerCase().contains("contact"))) {
        //AssertUtils.fail(ErrorEnum.CONTACTSUPPORT_LINK_CEP.geterr()+" Page title ::"+ contactPageTitle);
        errorMsg = ErrorEnum.CONTACTSUPPORT_LINK_CEP.geterr() + " Page title ::" + contactPageTitle;
        return errorMsg;
      }

      driver.close();
      Util.sleep(2000);

      driver.switchTo().window(winHandleBeforeToggle);
      String currentTitle = driver.getTitle().trim();

      if (!(currentTitle.equals(titleBeforeToggle))) {
        Util.printTestFailedMessage(
            "currentTitle : " + currentTitle + "\n" + "oldTitle     : " + titleBeforeToggle + "\n");
        //AssertUtils.fail( "Contact Support title is not matching after toggle action" );
        errorMsg = "Contact Support title is not matching after toggle action";
        return errorMsg;
      }
      portalPage.clickUsingLowLevelActions("helpIconCEP");
    } catch (MetadataException e) {
      //AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " and validate Contact Support Link in CEP");
      errorMsg =
          ErrorEnum.GENERIC_EXPECTION_ACTION.geterr() + " and validate Contact Support Link in CEP";
    }
    return errorMsg;
  }

  @Step("Validation Steps " + GlobalConstants.TAG_TESTINGHUB)
  public void validationStepsResult(String errorMsg) {
    if (!errorMsg.isEmpty()) {
      AssertUtils.fail(errorMsg);
    }
  }

  //@Step("Validate Bic Native Turn On-Off Renewal")
  public String clickOnActiveSubscriptionInBO() {
    String errorMsg = "";
    try {
      boolean statusExpired = portalPage
          .checkIfElementExistsInPage("autoRenewStatExpiring", 15); // turned off  state
      boolean statusAutoRenew = portalPage
          .checkIfElementExistsInPage("autoRenewStatRenewing", 15); // turned on state
      //String currentState =portalPage.getAttribute("autoRenewOnOffSwitch", "data-wat-linkname");
      if (statusAutoRenew) {
        turnOffRenewal();
        Util.sleep(10000);
        printBackAutoExpiresStatus();
        if (!portalPage.checkIfElementExistsInPage("autoRenewStatExpiring", 15)) {
          //AssertUtils.fail("Expires status is not getting Updated after TURN OFF renewal");
          errorMsg = "Expires status is not getting Updated after TURN OFF renewal";
          return errorMsg;
        }
      } else if (statusExpired) {
        turnOnRenewal();
        Util.sleep(10000);
        printBackAutoExpiresStatus();
        if (!portalPage.checkIfElementExistsInPage("autoRenewStatRenewing", 15)) {
          //AssertUtils.fail("Auto renew status is not getting Updated after TURN ON renewal");
          errorMsg = "Auto renew status is not getting Updated after TURN ON renewal";
          return errorMsg;
        }
      } else {
        //AssertUtils.fail("Unable to Turn On - off Bic Native renewal order");
        errorMsg = "Unable to Turn On - off Bic Native renewal order";
        return errorMsg;
      }
    } catch (Exception e) {
      errorMsg = "Unable to Turn On - off Bic Native renewal order";
      e.printStackTrace();
    }
    return errorMsg;
  }

  public void printBackAutoExpiresStatus() throws MetadataException {
    Util.printInfo("backButtonSubscriptions is displayed :: " + portalPage
        .checkIfElementExistsInPage("backButtonSubscriptions", 30));
    Util.printInfo("autorenewSubscription is displayed :: " + portalPage
        .checkIfElementExistsInPage("autoRenewStatRenewing", 30));
    Util.printInfo("expiringSubscription is displayed :: " + portalPage
        .checkIfElementExistsInPage("autoRenewStatExpiring", 30));
  }

  @Step("Turn-ON BIC Native Renewal ")
  public void turnOnRenewal() {
    try {
      Util.PrintInfo("inside turnOnRenewal");
      portalPage.clickUsingLowLevelActions("bicNativeToggleSubscription");
      Util.PrintInfo("click on turn on switch completed");
      feynamnLayoutLoaded();
      Util.PrintInfo("after feynamnLayoutLoaded");
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to Turn ON renewal");
    }
  }

  @Step("Turn-OFF BIC Native Renewal ")
  public void turnOffRenewal() {
    try {
      Util.PrintInfo("inside turnOffRenewal");
      portalPage.clickUsingLowLevelActions("bicNativeToggleSubscription");
      Util.PrintInfo("click on turn off switch completed");
      //Util.sleep(5000);
      if (portalPage.checkIfElementExistsInPage("renewalModal", 10)) {
        portalPage.clickUsingLowLevelActions("renewalModalTurnOff");
        portalPage.checkIfElementExistsInPage("expiringTExt", 10);
      } else {
        AssertUtils.fail("Unable to Turn OFF renewal ");
      }
    } catch (MetadataException e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to Turn OFF renewal ");
    }
  }

  @Step("Verify User and validate Successfully verified modal")
  public void clickGetStarted() {
    try {
      Util.printInfo(" getStarted_link is displayed : " + studentPage
          .checkIfElementExistsInPage("getStarted_link", 10));
      Util.sleep(15000);
      try {
        portalPage.clickUsingLowLevelActions("getStartedLink");
      } catch (Exception e) {
        e.printStackTrace();
      }

      //studentPage.clickUsingLowLevelActions("getStarted_link");
      Util.sleep(5000);
      studentPage.clickUsingLowLevelActions("verify_btn");
      Util.sleep(10000);
      boolean success = studentPage.checkIfElementExistsInPage("success_Modal", 10);
      if (!success) {
        AssertUtils.fail("Unable to Verify User");
      } else {
        studentPage.clickUsingLowLevelActions("getAutodeskSoftware_btn");
        Util.sleep(15000);
        driver.navigate().refresh();
      }
      Util.sleep(15000);
      JavascriptExecutor js = ((JavascriptExecutor) driver);
      js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
      Thread.sleep(5000);
      clickOnInstallProduct();

//          studentPage.click("getProduct_Revit_link");
//          Util.sleep(5000);
//          studentPage.click("install_Revit_btn");
//          Util.sleep(5000);
//          studentPage.click("install_Rvt_Accept");
//          Util.sleep(5000);

      driver.navigate().refresh();
      Util.sleep(15000);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to click on Get Started Link");
    }
  }

  @Step("Get Revit Product and install")
  private void clickOnInstallProduct() {
    try {
      String getProductLink = "//a[@websdk-id='card-rvt' ]/*[contains(text(),'Get product') and not (@wpd-string='edu-gatewayget-license')]";
      String installLink = "//*[@id='websdk-rvt']//*[text()='INSTALL' and @class='widgetButtonLabel']";
      String AcceptLink = "//*[@id='websdk-rvt']//*[text()='Accept']";

      Util.sleep(5000);
      int loop = 0;
      do {
        driver.findElement(By.xpath(getProductLink)).click();
        Util.sleep(5000);
        try {
          if (driver.findElement(By.xpath(".//error-banner")).isDisplayed()) {
            AssertUtils.fail("We're experiencing system issues. Please try again later.");
          }
        } catch (Exception e) {
        }

        loop++;
        if (loop > 2) {
          AssertUtils.fail("Unable to click on the Get Product link after verification");
        }
      } while (driver.findElement(By.xpath(getProductLink)).isDisplayed());

      driver.findElement(By.xpath(installLink)).click();
      Util.sleep(5000);
      driver.findElement(By.xpath(AcceptLink)).click();
      Util.sleep(5000);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to click on the Install link for Product");
    }
  }

  public void populateQEI() {
    try {
      System.out.println();
      boolean qualifiedEDU_text = studentPage.checkIfElementExistsInPage("qualifiedEDU_text", 10);
      Util.printInfo("Qualified Educational Institution page is loaded : " + qualifiedEDU_text);

      if (qualifiedEDU_text == true) {
        populateEduProfile();
        clickOnNextButton();
      }
      populateEduInstitution();
      clickOnNextButton();
      clickOnSubmitButton();

    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to populate Qualified EDU details");
    }
  }

  private void clickOnSubmitButton() {
    try {
      studentPage.clickUsingLowLevelActions("submitBtn");
      studentPage.waitForPageToLoad();
      driver.navigate().refresh();
      studentPage.waitForPageToLoad();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Step("Populate Education Institution details")
  private void populateEduInstitution() {
    try {
      String backdate = Util.customDate("YYYY", 0, 0, -1);
      String futuredate = Util.customDate("YYYY", 0, 0, 1);

      String field = studentPage.getFieldLocators("edu_school").get(0);
      driver.findElement(By.xpath(field)).click();
      driver.findElement(By.xpath(field)).sendKeys("Abc ");
      Util.sleep(5000);
      field = "//*[text()='Abc']";
      driver.findElement(By.xpath(field)).click();
      Util.sleep(5000);

      field = studentPage.getFieldLocators("aos_AEC").get(0);
      driver.findElement(By.xpath(field)).click();

      studentPage.populateField("CompactEnrolmentStart_custMonth", "December");
      studentPage.populateField("CompactEnrolmentStart_custYear", backdate);

      studentPage.populateField("CompactEnrolmentEnd_custMonth", "December");
      studentPage.populateField("CompactEnrolmentEnd_custYear", futuredate);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to populate Education Institution details ");
    }
  }

  private void clickOnNextButton() {
    try {
      String field = studentPage.getFieldLocators("btnSubmit").get(0);
      driver.findElement(By.xpath(field)).click();
      studentPage.waitForPageToLoad();
//          boolean nextPageElement =studentPage.checkIfElementExistsInPage("edu_school", 20);
//          if(!(nextPageElement)) {
//              AssertUtils.fail("Unable to click on next Button ");
//          }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to click on next Button ");
    }
  }

  @Step("Country, Territory, or Region of educational institution ")
  private void populateEduProfile() {
    try {
      String backdate = Util.customDate("YYYY", 0, 0, -18);
      studentPage.populateField("edu_country", "United States");
      studentPage.populateField("edu_role", "Student");
      studentPage.populateField("edu_institute", "University/Post-Secondary");
      studentPage.populateField("DateOfBirth_custMonth", "December");
      studentPage.populateField("DateOfBirth_custDay", "1");
      studentPage.populateField("DateOfBirth_custYear", backdate);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to Set up your Education profile ");
    }
  }

  public void clickOnRevitGetStartedLink() {
    try {
      boolean revit_GetStarted_Link = studentPage
          .checkIfElementExistsInPage("revit_GetStarted_Link", 10);
      if (!(revit_GetStarted_Link)) {
        studentPage.refresh();
        studentPage.waitForPageToLoad();
        Util.sleep(5000);
      }

      if (revit_GetStarted_Link) {
        studentPage.clickUsingLowLevelActions("revit_GetStarted_Link");
        studentPage.waitForPageToLoad();
        Util.sleep(5000);
      } else {
        debugHTMLPage();
        AssertUtils.fail("Revit Product is not loading ");
      }

      String url = driver.getCurrentUrl().toLowerCase();
      boolean status = url.contains("accounts");

      if (!(status)) {
        if (revit_GetStarted_Link) {
          studentPage.clickUsingLowLevelActions("revit_GetStarted_Link");
          studentPage.waitForPageToLoad();
          Util.sleep(5000);
        } else {
          debugHTMLPage();
          AssertUtils.fail("Revit Product is not loading ");
        }
      }
    } catch (MetadataException e) {
      AssertUtils.fail("Revit Product is not loading ");
    }
  }

  private void debugHTMLPage() {
    Util.printInfo("-----------------------------" +
        "\n" + " URL :            " + driver.getCurrentUrl() +
        "\n" + " Page Title :     " + driver.getTitle() +
        "\n" + " Page source  :   " + driver.getPageSource()
        + "\n" + "-----------------------------");
  }

  private void debugPageUrl(String messageHeader) {
    Util.printInfo("----------" + messageHeader + "-------------------" +
        "\n" + " URL :            " + driver.getCurrentUrl() +
        "\n" + " Page Title :     " + driver.getTitle()
        + "\n" + "-----------------------------");
  }

  @Step("Click Profile Name Field")
  public String clickProfileName() {
    String value = "";
    try {
      boolean status = driver.findElement(By.xpath("//p[text()='Name']/..//p[2]/span"))
          .isDisplayed();
      if (status) {
        value = driver.findElement(By.xpath("//p[text()='Name']/..//p[2]/span")).getText()
            .trim();
        driver.findElement(By.xpath("//p[text()='Name']/..//p[2]/span")).click();
      } else {
        AssertUtils.fail("Unable to click on Name field ");
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to click on Name field ");
    }
    return value;
  }

  @Step("Update Profile First & Last Name ")
  public String updateName(String updateFN, String updateLN) {
    String value = "";
    try {
      driver.findElement(By.xpath("//label[text()='First Name']/..//input"))
          .sendKeys(Keys.chord(Keys.CONTROL, "a"));
      driver.findElement(By.xpath("//label[text()='First Name']/..//input")).sendKeys(updateFN);
      driver.findElement(By.xpath("//label[text()='Last Name']/..//input"))
          .sendKeys(Keys.chord(Keys.CONTROL, "a"));
      driver.findElement(By.xpath("//label[text()='Last Name']/..//input")).sendKeys(updateLN);
      driver.findElement(By.xpath("//span[text()='Save']")).click();
      Util.sleep(5000);
      value = driver.findElement(By.xpath("//p[text()='Name']/..//p[2]/span")).getText().trim();
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to update Name field ");
    }
    return value;
  }

  @Step("Adding seat from portal for BIC orders")
  public HashMap<String, String> createAndValidateAddSeatOrderInPortal(String addSeatQty,
      LinkedHashMap<String, String> testDataForEachMethod) {
    driver.switchTo().defaultContent();
    HashMap<String, String> orderDetails = new HashMap<String, String>();
    orderDetails.putAll(createAddSeatOrder(addSeatQty, testDataForEachMethod));
    orderDetails.putAll(validateAddSeatOrder(orderDetails));
    return orderDetails;
  }

  public HashMap<String, String> navigateToSubscriptionAndOrdersTab() {
    Util.printInfo("Navigating to subscriptions and orders tab...");
    HashMap<String, String> orderDetails = new HashMap<String, String>();
    try {
      if (portalPage.checkIfElementExistsInPage("portalLinkSubscriptions", 10) == true) {
        Util.printInfo("Clicking on portal subscription and contracts link...");
        portalPage.clickUsingLowLevelActions("portalLinkSubscriptions");
        portalPage.waitForPageToLoad();

        debugPageUrl("Step 2");

        Util.waitforPresenceOfElement(
            portalPage.getFirstFieldLocator("subscriptionRowInSubscription"));
        Util.printInfo("Clicking on subscription row...");

        debugPageUrl("Step 3");
        portalPage.clickUsingLowLevelActions("subscriptionRowInSubscription");
        portalPage.waitForPageToLoad();

        debugPageUrl("Step 4");
        checkEmailVerificationPopupAndClick();
        debugPageUrl("Step 5");

        String currentUrl = driver.getCurrentUrl();

        boolean status = currentUrl.toLowerCase().contains("trials");

        if (status) {
          Util.printInfo("Hardcoding the redirect to subscriptions-contracts page");
          driver.get("https://stg-manage.autodesk.com/billing/subscriptions-contracts");
          portalPage.clickUsingLowLevelActions("subscriptionRowInSubscription");
          Util.sleep(30000);
          debugPageUrl("Final attempt");
          currentUrl = driver.getCurrentUrl();
          status = currentUrl.toLowerCase().contains("trials");
          if (status) {
            AssertUtils.fail("Unable to redirect to subscriptions payment details page");
          }
        }

        Util.sleep(5000);
        Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("portalOrderSeatCount"));
        String initialOrderQty = portalPage.getTextFromLink("portalOrderSeatCount");
        Util.printInfo("Initial seat quantity on order info page: " + initialOrderQty);
        orderDetails.put("initialOrderQty", initialOrderQty);

        String paymentDetails = portalPage.getTextFromLink("portalPaymentDetails")
            .replaceAll("\\s", "");
        Util.printInfo("Payment Details on order info page: " + paymentDetails);
        orderDetails.put("paymentDetails", paymentDetails);

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
            .replaceAll(",", "")
            .trim();
        Util.printInfo("City : " + city);

        String state = portalPage.getTextFromLink("portalSubscriptionStateFromSubs");
        Util.printInfo("State Province : " + state);

        String pin = portalPage.getTextFromLink("portalSubscriptionZipFromSubs");
        Util.printInfo("Zip Code : " + pin);

        orderDetails.put("Full_Address", streetAddress);
        orderDetails.put("City", city);
        orderDetails.put("Zipcode", pin);
        orderDetails.put("State_Province", state);
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

      String paymentDetails = orderDetails.get("paymentDetails");
      Util.printInfo("Payment Details : " + paymentDetails);

      Util.sleep(15000);
      Util.printInfo("Clicking on Add Seat button...");
      String currentURL = driver.getCurrentUrl();
      Util.printInfo("currentURL1 before clicking on Add seat : " + currentURL);

      portalPage.clickUsingLowLevelActions("portalAddSeatButton");

      Util.sleep(45000);
      currentURL = driver.getCurrentUrl();
      Util.printInfo("currentURL2 : " + currentURL);

      boolean status = currentURL.contains("add-seats");

      while (!status) {

        Util.printInfo("Attempt1 - Javascript method to redirect to Add seat page");
        String portalAddSeatButton = "document.getElementById(\"add-seats\").click()";
        clickCheckBox(portalAddSeatButton);
        Util.sleep(15000);

        status = currentURL.contains("add-seats");

        if (!status) {
          Util.printInfo("Attempt2 to redirect with hardcoded URL " + currentURL);
          driver.get("https://stg-manage.autodesk.com/billing/add-seats");
          driver.navigate().refresh();
          Util.sleep(45000);
          currentURL = driver.getCurrentUrl();
          Util.printInfo("currentURL3 : " + currentURL);
        } else {
          break;
        }

        status = currentURL.contains("add-seats");

        if (!status) {
          debugHTMLPage();
          Util.printTestFailedMessage(
              "Multiple attempts failed to redirect in Portal - ADD Seat page " + currentURL);
          AssertUtils.fail("Unable to redirect to Add Seat page in Account portal");
        } else {
          break;
        }

      }

      Util.sleep(15000);

      debugHTMLPage();

      if (isPortalLoginPageVisible()) {
        System.out.println("Session timed out - trying to log into portal again");
        portalLogin(testDataForEachMethod.get("emailid"), "Password1");
        driver.get(currentURL);
        portalPage.clickUsingLowLevelActions("portalAddSeatButton");
      }

      portalPage.waitForPageToLoad();
      Util.sleep(10000);

      // Util.waitForElement(portalPage.getFirstFieldLocator("portalASProductTerm"),
      // "Product Term");
      String productSubscriptionTerm = portalPage
          .getLinkText("portalASProductTerm"); // .split(":")[1].trim();
      Util.printInfo("Product subscription term on add seat page : " + productSubscriptionTerm);
      orderDetails.put("productSubscriptionTerm", productSubscriptionTerm);

      String perSeatProratedAmount = portalPage.getLinkText("portalASAmountPerSeat");
      Util.printInfo("Prorated amount per seat : " + perSeatProratedAmount);
      orderDetails.put("perSeatProratedAmount", perSeatProratedAmount);

      Util.printInfo("Adding quantity for seat as..." + addSeatQty);
      orderDetails.put("addSeatQty", addSeatQty);
      portalPage.populateField("portalASQtyTextField", addSeatQty);

      Util.sleep(5000);
      String proratedFinalPrice = portalPage.getLinkText("portalASFinalProratedPrice");
      Util.printInfo("Prorated Final Amount : " + proratedFinalPrice);
      orderDetails.put("proratedFinalAmount", proratedFinalPrice);

      Util.printInfo("Capturing Tax details...");
      String taxAmount = portalPage.getLinkText("portalASTaxDetails");
      Util.printInfo("Tax amount : " + taxAmount);
      orderDetails.put("taxAmount", taxAmount);

      String subtotalPrice = portalPage.getLinkText("portalASFinalSubtotalAmount");
      Util.printInfo("Subtotal amount : " + subtotalPrice);
      orderDetails.put("subtotalPrice", subtotalPrice);

      Util.printInfo("Clicking on Submit Order button...");
      portalPage.clickUsingLowLevelActions("portalASSubmitOrderBtn");

    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to place add seat order from portal...");
    }
    return orderDetails;
  }

  public HashMap<String, String> validateAddSeatOrder(HashMap<String, String> data) {
    HashMap<String, String> orderDetails = new HashMap<String, String>();

    try {
      Util.waitForElement(portalPage.getFirstFieldLocator("portalASOrderConfirmationHead"),
          "Order confirmation page");
      String addSeatOrderNumber = portalPage.getLinkText("portalASOrderNumberText");
      orderDetails.put(TestingHubConstants.addSeatOrderNumber, addSeatOrderNumber);
      Util.printInfo("Add Seat Order number : " + addSeatOrderNumber);

      Util.printInfo("Validating prorated amount on confirmation page...");
      String confirmProratedAmount = portalPage.getLinkText("portalASConfirmProratedPrice");
      AssertUtils.assertEquals(data.get("proratedFinalAmount"), confirmProratedAmount);

//          Util.printInfo("Validating product subscription term on confirmation page...");
//          String confirmProductTerm = portalPage.getLinkText("portalASConfirmProductTerm").trim().split(":")[1].trim();
//          AssertUtils.assertEquals(data.get("productSubscriptionTerm"),confirmProductTerm);
//
//          Util.printInfo("Validating quantity on confirmation page...");
//          String confirmAddSeatQty = portalPage.getLinkText("portalASConfirmQty");
//          AssertUtils.assertEquals(data.get("addSeatQty"),confirmAddSeatQty);
//
//          Util.printInfo("Validating subtotal on confirmation page...");
//          String confirmSubtotal = portalPage.getLinkText("portalASTotalAmount");
//          AssertUtils.assertEquals(data.get("subtotalPrice"), confirmSubtotal);

//          Util.printInfo("Validating tax amount on confirmation page...");
//          String confirmTaxAmount = portalPage.getLinkText("portalASConfirmTax");
//          AssertUtils.assertEquals(data.get("taxAmount"), confirmTaxAmount);

//          Util.printInfo("Validating total amount on confirmation page...");
//          String confirmTotalAmount = portalPage.getLinkText("portalASConfirmTotalAmt");
//          AssertUtils.assertEquals(data.get("totalAmount"),confirmTotalAmount);

      //Close button functionality is no longer available for add seat
//          Util.printInfo("Clicking on close button...");
//          portalPage.clickUsingLowLevelActions("portalASCloseButton");

      Util.printInfo("Clicking on back button...");
      portalPage.clickUsingLowLevelActions("portalBackButton");

      Util.sleep(5000);
      driver.switchTo().defaultContent();
      Util.printInfo("Refreshing the page...");
      driver.navigate().refresh();
      Util.sleep(15000);

      Util.waitForElement(portalPage.getFirstFieldLocator("portalAddSeatButton"),
          "Add Seat button");
      String totalSeats = portalPage.getTextFromLink("portalOrderSeatCount");
      Util.printInfo("Total seats displayed on order info page: " + totalSeats);
      orderDetails.put("totalSeats", totalSeats);

      String initialOrderQty = data.get("initialOrderQty");
      if (!totalSeats.equals(initialOrderQty)) {
        Util.printInfo("Seats added successfully...");
      } else {
        AssertUtils.fail("Failed to add seats. Initial order seat : " + initialOrderQty
            + " total number of seats : " + totalSeats + " are same");
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to validate add seat order...");
    }

    return orderDetails;
  }

  @Step("Reduce seats from portal for BIC orders")
  public HashMap<String, String> reduceSeatsInPortalAndValidate() throws MetadataException {
    driver.switchTo().defaultContent();
    HashMap<String, String> orderDetails = new HashMap<>();
    orderDetails.putAll(reduceSeats());
    validateReducedSeats(orderDetails);
    return orderDetails;
  }

  public HashMap<String, String> reduceSeats() throws MetadataException {
    HashMap<String, String> orderDetails = new HashMap<>();
    orderDetails.putAll(navigateToSubscriptionAndOrdersTab());

    Util.printInfo("Reducing seats.");
    if (portalPage.checkIfElementExistsInPage("portalSubscriptionTermPopup", 10)) {
      portalPage.clickUsingLowLevelActions("portalCloseButton");
    }
    portalPage.clickUsingLowLevelActions("portalReduceSeatsButton");
    portalPage.checkIfElementExistsInPage("portalReduceSeatsPanel", 10);
    portalPage.clickUsingLowLevelActions("portalMinusButton");
    portalPage.clickUsingLowLevelActions("portalSaveChangesButton");
    portalPage.checkIfElementExistsInPage("portalConfirmationModal", 10);
    Util.printInfo("Clicking on ok button...");
    portalPage.clickUsingLowLevelActions("portalConfirmationOkButton");
    Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("portalOrderSeatCount"));
    String renewingSeatsCount = portalPage.getTextFromLink("portalRenewingSeatsCount");
    String reducedSeatQty = renewingSeatsCount.split(" ")[0];
    Util.printInfo("Recording new seats count.");
    orderDetails.put("reducedSeatQty", reducedSeatQty);
    return orderDetails;
  }

  public void validateReducedSeats(HashMap<String, String> data) throws MetadataException {
    portalPage.checkIfElementExistsInPage("portalReduceSeatsButton", 10);
    String newSeatsTotal = data.get("reducedSeatQty");
    String initialOrderQty = data.get("initialOrderQty");
    if (!newSeatsTotal.equals(initialOrderQty)) {
      Util.printInfo("Seats reduced successfully.");
    } else {
      AssertUtils.fail("Failed to reduce seats. Initial order seat : " + initialOrderQty
          + " total number of seats : " + newSeatsTotal + " are same");
    }
  }

  @Step("Changing payment from Portal" + GlobalConstants.TAG_TESTINGHUB)
  public void changePaymentMethodAndValidate(HashMap<String, String> data,
      String[] paymentCardDetails) {
    Util.printInfo("Changing the payment method from portal...");
    try {
      debugPageUrl("Step 1");
      data.putAll(navigateToSubscriptionAndOrdersTab());
      Util.printInfo("Clicking on change payment option...");
      portalPage.clickUsingLowLevelActions("portalChangePaymentBtn");
      portalPage.waitForPageToLoad();
      Util.sleep(5000);
      Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("portalPaymentMethod")
          .replaceAll("<PAYMENTOPTION>", "Credit card"));
      addPaymentDetails(data, paymentCardDetails);
      validatePaymentDetailsOnPortal(data);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to change the payment details from portal...");
    }
  }

  public void addPaymentDetails(HashMap<String, String> data, String[] paymentCardDetails) {
    Util.printInfo("Selecting payment profile : " + data.get("paymentType"));
    try {
      switch (data.get("paymentType").toUpperCase()) {
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
      portalPage.clickUsingLowLevelActions("portalCardSaveBtn");
      Util.sleep(10000);

      if (data.get("paymentType").toUpperCase()
          .equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
        Util.printInfo("Clicking on madate agreement form...");
        portalPage.clickUsingLowLevelActions("portalDebitMandateAgreement");
        Util.sleep(2000);
        portalPage.clickUsingLowLevelActions("portalCardSaveBtn");
        Util.sleep(10000);
      }
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to select payment profile...");
    }
  }

  @Step("Add Paypal Payment Details")
  public void populatePaypalDetails(HashMap<String, String> data) {
    Util.printInfo("Switching to latest window...");
    String parentWindow = driver.getWindowHandle();
    String paymentMethod = portalPage.getFirstFieldLocator("portalPaymentMethod")
        .replaceAll("<PAYMENTOPTION>", "PayPal");
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
      AssertUtils.assertTrue(title.contains("PayPal"),
          "Current title [" + title + "] does not contains keyword : PayPal");

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (BICTestBase.bicPage.checkIfElementExistsInPage("paypalAcceptCookiesBtn", 10)) {
        BICTestBase.bicPage.clickUsingLowLevelActions("paypalAcceptCookiesBtn");
      }

      Util.printInfo("Entering paypal user name [" + data.get("paypalUser") + "]...");
      BICTestBase.bicPage.waitForElementVisible(
          BICTestBase.bicPage.getMultipleWebElementsfromField("paypalUsernameField").get(0), 10);
      BICTestBase.bicPage.populateField("paypalUsernameField", data.get("paypalUser"));

      Util.printInfo("Entering paypal password...");
      BICTestBase.bicPage.populateField("paypalPasswordField", data.get("paypalSsap"));

      Util.printInfo("Clicking on login button...");
      BICTestBase.bicPage.clickUsingLowLevelActions("paypalLoginBtn");
      BICTestBase.bicPage.waitForElementToDisappear("paypalPageLoader", 30);
      Util.sleep(5000);

      Util.printInfo("Checking Accept cookies button and clicking on it...");
      if (BICTestBase.bicPage.checkIfElementExistsInPage("paypalAcceptCookiesBtn", 10)) {
        BICTestBase.bicPage.clickUsingLowLevelActions("paypalAcceptCookiesBtn");
      }

      Util.printInfo("Selecting paypal payment option " + data.get("paypalPaymentType"));
      String paymentTypeXpath = BICTestBase.bicPage.getFirstFieldLocator("paypalPaymentOption")
          .replace("<PAYMENTOPTION>", data.get("paypalPaymentType"));
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
    String paymentMethod = portalPage.getFirstFieldLocator("portalPaymentMethod")
        .replaceAll("<PAYMENTOPTION>", "Direct Debit (ACH)");
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
    String paymentMethod = portalPage.getFirstFieldLocator("portalPaymentMethod")
        .replaceAll("<PAYMENTOPTION>", "Credit card");
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
      Util.printInfo("Entering seciruty code : " + paymentCardDetails[3]);
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
    String paymentType = data.get("paymentType");
    String firstNameXpath = "";
    String lastNameXpath = "";
    if (paymentType.equalsIgnoreCase(BICConstants.paymentTypePayPal)) {
      firstNameXpath = BICTestBase.bicPage.getFirstFieldLocator("firstName")
          .replace("<PAYMENTPROFILE>", "paypal");
      lastNameXpath = BICTestBase.bicPage.getFirstFieldLocator("lastName")
          .replace("<PAYMENTPROFILE>", "paypal");
    } else if (paymentType.equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
      firstNameXpath = BICTestBase.bicPage.getFirstFieldLocator("firstName")
          .replace("<PAYMENTPROFILE>", "ach");
      lastNameXpath = BICTestBase.bicPage.getFirstFieldLocator("lastName")
          .replace("<PAYMENTPROFILE>", "ach");
    } else {
      firstNameXpath = BICTestBase.bicPage.getFirstFieldLocator("firstName")
          .replace("<PAYMENTPROFILE>", "credit-card");
      lastNameXpath = BICTestBase.bicPage.getFirstFieldLocator("lastName")
          .replace("<PAYMENTPROFILE>", "credit-card");
    }
    driver.findElement(By.xpath(firstNameXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
    Util.sleep(2000);
    driver.findElement(By.xpath(firstNameXpath)).sendKeys(data.get("firstname"));

    driver.findElement(By.xpath(lastNameXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
    Util.sleep(2000);
    driver.findElement(By.xpath(lastNameXpath)).sendKeys(data.get("lastname"));

    if (data.size() == 6) {
      status = populateEMEABillingDetails(data);
      BICTestBase.bicPage.waitForPageToLoad();
    } else {
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
    BICTestBase.bicPage.populateField("Organization_NameEMEA", address.get("Organization_Name"));
    BICTestBase.bicPage.populateField("Full_AddressEMEA", address.get("Full_Address"));
    BICTestBase.bicPage.populateField("CityEMEA", address.get("City"));
    BICTestBase.bicPage.populateField("ZipcodeEMEA", address.get("Zipcode"));
    BICTestBase.bicPage.populateField("Phone_NumberEMEA", address.get("phone"));
    BICTestBase.bicPage.populateField("CountryEMEA", address.get("Country"));
    return status;
  }

  private boolean populateNAMERBillingDetails(Map<String, String> address, String paymentType,
      String userType) {
    Util.printInfo("Adding billing details...");
    boolean status = false;
    String orgNameXpath = "", fullAddrXpath = "", cityXpath = "", zipXpath = "", phoneXpath = "", countryXpath = "", stateXpath = "";
    switch (paymentType.toUpperCase()) {

      case BICConstants.paymentTypePayPal:
        orgNameXpath = BICTestBase.bicPage.getFirstFieldLocator("Organization_Name")
            .replace("<PAYMENTPROFILE>", "paypal");
        fullAddrXpath = BICTestBase.bicPage.getFirstFieldLocator("Full_Address")
            .replace("<PAYMENTPROFILE>", "paypal");
        cityXpath = BICTestBase.bicPage.getFirstFieldLocator("City")
            .replace("<PAYMENTPROFILE>", "paypal");
        zipXpath = BICTestBase.bicPage.getFirstFieldLocator("Zipcode")
            .replace("<PAYMENTPROFILE>", "paypal");
        phoneXpath = BICTestBase.bicPage.getFirstFieldLocator("Phone_Number")
            .replace("<PAYMENTPROFILE>", "paypal");
        countryXpath = BICTestBase.bicPage.getFirstFieldLocator("Country")
            .replace("<PAYMENTPROFILE>", "paypal");
        stateXpath = BICTestBase.bicPage.getFirstFieldLocator("State_Province")
            .replace("<PAYMENTPROFILE>", "paypal");
        break;
      case BICConstants.paymentTypeDebitCard:
        orgNameXpath = BICTestBase.bicPage.getFirstFieldLocator("Organization_Name")
            .replace("<PAYMENTPROFILE>", "ach");
        fullAddrXpath = BICTestBase.bicPage.getFirstFieldLocator("Full_Address")
            .replace("<PAYMENTPROFILE>", "ach");
        cityXpath = BICTestBase.bicPage.getFirstFieldLocator("City")
            .replace("<PAYMENTPROFILE>", "ach");
        zipXpath = BICTestBase.bicPage.getFirstFieldLocator("Zipcode")
            .replace("<PAYMENTPROFILE>", "ach");
        phoneXpath = BICTestBase.bicPage.getFirstFieldLocator("Phone_Number")
            .replace("<PAYMENTPROFILE>", "ach");
        countryXpath = BICTestBase.bicPage.getFirstFieldLocator("Country")
            .replace("<PAYMENTPROFILE>", "ach");
        stateXpath = BICTestBase.bicPage.getFirstFieldLocator("State_Province")
            .replace("<PAYMENTPROFILE>", "ach");
        break;
      default:
        orgNameXpath = BICTestBase.bicPage.getFirstFieldLocator("Organization_Name")
            .replace("<PAYMENTPROFILE>", "credit-card");
        fullAddrXpath = BICTestBase.bicPage.getFirstFieldLocator("Full_Address")
            .replace("<PAYMENTPROFILE>", "credit-card");
        cityXpath = BICTestBase.bicPage.getFirstFieldLocator("City")
            .replace("<PAYMENTPROFILE>", "credit-card");
        zipXpath = BICTestBase.bicPage.getFirstFieldLocator("Zipcode")
            .replace("<PAYMENTPROFILE>", "credit-card");
        phoneXpath = BICTestBase.bicPage.getFirstFieldLocator("Phone_Number")
            .replace("<PAYMENTPROFILE>", "credit-card");
        countryXpath = BICTestBase.bicPage.getFirstFieldLocator("Country")
            .replace("<PAYMENTPROFILE>", "credit-card");
        stateXpath = BICTestBase.bicPage.getFirstFieldLocator("State_Province")
            .replace("<PAYMENTPROFILE>", "credit-card");
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

    driver.findElement(By.xpath(orgNameXpath)).click();
    driver.findElement(By.xpath(fullAddrXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
    Util.sleep(3000);
    driver.findElement(By.xpath(fullAddrXpath)).sendKeys(address.get("Full_Address"));

    driver.findElement(By.xpath(cityXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
    Util.sleep(3000);
    driver.findElement(By.xpath(cityXpath)).sendKeys(address.get("City"));

    driver.findElement(By.xpath(zipXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
    Util.sleep(3000);
    driver.findElement(By.xpath(zipXpath)).sendKeys(address.get("Zipcode"));

    driver.findElement(By.xpath(phoneXpath)).sendKeys(Keys.CONTROL, "a", Keys.DELETE);
    Util.sleep(3000);
    driver.findElement(By.xpath(phoneXpath)).sendKeys("2333422112");

    WebElement countryEle = driver.findElement(By.xpath(countryXpath));
    Select selCountry = new Select(countryEle);
    if (userType.equalsIgnoreCase("new")) {
      selCountry.selectByVisibleText(address.get("Country"));
    } else {
      selCountry.selectByIndex(0);
    }

    driver.findElement(By.xpath(stateXpath)).sendKeys(address.get("State_Province"));
    return status;
  }

  public void validatePaymentDetailsOnPortal(HashMap<String, String> data) {
    Util.printInfo("Validating payment details...");
    data.putAll(navigateToSubscriptionAndOrdersTab());
    String paymentDetails = data.get("paymentDetails").toLowerCase();
    Util.printInfo("Payment Details on order info page: " + paymentDetails);

    if (data.get("paymentType").equalsIgnoreCase(BICConstants.paymentTypePayPal)) {
      Assert.assertTrue(paymentDetails.contains("paypal"),
          "Payment details [" + paymentDetails + "] does not contains text [paypal]");
    } else if (data.get("paymentType").equalsIgnoreCase(BICConstants.paymentTypeDebitCard)) {
      Assert.assertTrue(paymentDetails.contains("account"),
          "Payment details [" + paymentDetails + "] does not contains text [account]");
    } else {
      Util.printInfo("Payment details [" + paymentDetails + "] ");
    }
  }

  @Step("Activate PIN in CEP" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> activatePOSAPinInCEP(HashMap<String, String> data) {
    HashMap<String, String> results = new HashMap<String, String>();
    Util.printInfo("Activating pin from CEP...");
    try {
      Util.printInfo("Opening Register Pin URL : " + data.get(TestingHubConstants.activatePinURL));
      driver.get(data.get(TestingHubConstants.activatePinURL));

      Util.printInfo("Creating new CEP account...");
      portalPage.clickUsingLowLevelActions("createAccountCEP");
      portalPage.waitForPageToLoad();
      Util.waitforPresenceOfElement("//input[@id='firstname_str']");

      String emailID = generateUniqueEmailID("thub", generateRandomTimeStamp(), "letscheck.pw");
      results.put(TestingHubConstants.firstname, "THub" + generateRandom(6));
      results.put(TestingHubConstants.lastname, generateRandom(6));
      results.put(TestingHubConstants.emailid, emailID);
      data.put(TestingHubConstants.passCEP, data.get("password"));

      JavascriptExecutor js = (JavascriptExecutor) driver;
      WebElement firstNameEle = driver.findElement(By.xpath("//input[@id='firstname_str']"));
      firstNameEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + results.get(TestingHubConstants.firstname) + "';",
          firstNameEle);
      Util.printInfo("Entered first name : " + results.get(TestingHubConstants.firstname));

      WebElement lastNameEle = driver.findElement(By.xpath("//input[@id='lastname_str']"));
      lastNameEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + results.get(TestingHubConstants.lastname) + "';",
          lastNameEle);
      Util.printInfo("Entered last name : " + results.get(TestingHubConstants.lastname));

      WebElement emailEle = driver.findElement(By.xpath("//input[@id='email_str']"));
      emailEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + results.get(TestingHubConstants.emailid) + "';",
          emailEle);
      Util.printInfo("Entered Email ID : " + results.get(TestingHubConstants.emailid));

      WebElement confirmEmailEle = driver.findElement(By.xpath("//input[@id='confirm_email_str']"));
      confirmEmailEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + results.get(TestingHubConstants.emailid) + "';",
          confirmEmailEle);
      Util.printInfo("Entered Confirm email id : " + results.get(TestingHubConstants.emailid));

      WebElement passwordEle = driver.findElement(By.xpath("//input[@id='password']"));
      passwordEle.click();
      Util.sleep(2000);
      js.executeScript("arguments[0].value='" + data.get(TestingHubConstants.passCEP) + "';",
          passwordEle);
      Util.sleep(2000);

      String CheckboxClick = "document.getElementById(\"privacypolicy_checkbox\").click()";
      clickCheckBox(CheckboxClick);
      Util.sleep(5000);

      js.executeScript("arguments[0].click();",
          portalPage.getMultipleWebElementsfromField("createAccount").get(0));
      Util.printInfo("Account created...Navigating to pin activation page...");
      portalPage.waitForPageToLoad();

      String[] pinNumber = data.get(TestingHubConstants.pinNumber).split("-");

      //Activate the PIN
      Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("portalPinInstructionMsg"));
      Util.printInfo(
          "Instruction msg : [" + portalPage.getLinkText("portalPinInstructionMsg") + "]");
      List<WebElement> activationCodeTextFields = portalPage
          .getMultipleWebElementsfromField("portalPinActivationText");

      for (int i = 0; i < pinNumber.length; i++) {
        Util.printInfo(
            "Entering code : " + pinNumber[i] + " into " + (i + 1) + " pin text field...");
        activationCodeTextFields.get(i).sendKeys(pinNumber[i]);
      }

      Util.printInfo("Clicking on Submit Button...");
      portalPage.clickUsingLowLevelActions("portalPinSubmitButton");

      portalPage.waitForPageToLoad();
      Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("portalPinSuccessMsg"));
      Util.printInfo("Success msg : [" + portalPage.getLinkText("portalPinSuccessMsg") + "]");

      Util.printInfo("Clicking on continue button...");
      portalPage.clickUsingLowLevelActions("portalContinueButton");

      Util.sleep(5000);
      Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("portalCompanyNameField"));

      Util.printInfo("Validating PIN on account creation page...");
      String pinVerifyTextXpath = portalPage.getFirstFieldLocator("portalPageTDText")
          .replaceAll("<TDTEXT>", data.get(TestingHubConstants.pinNumber));
      String pinNumberDisplayed = driver.findElement(By.xpath(pinVerifyTextXpath)).getText();
      AssertUtils.assertEquals(pinNumberDisplayed, data.get(TestingHubConstants.pinNumber));

      Util.printInfo("Creating new company for POSA Order...");
      String companyName = Util.getUniqueString(10, true, true);
      Util.printInfo("Entering company name : " + companyName);
      Util.sleep(2000);
      portalPage.clickUsingLowLevelActions("portalCompanyNameField");
      portalPage.populateField("portalCompanyNameField", companyName);

      Util.printInfo("Entering address line 1 : " + data.get(TestingHubConstants.streetAddress));
      Util.sleep(2000);
      portalPage
          .populateField("portalCompanyAddressField", data.get(TestingHubConstants.streetAddress));

      Util.printInfo("Entering city : " + data.get(TestingHubConstants.addressCity));
      Util.sleep(2000);
      portalPage.populateField("portalCompanyCityField", data.get(TestingHubConstants.addressCity));

      Util.printInfo("Entering state : " + data.get(TestingHubConstants.stateProvinceStore));
      Util.sleep(2000);
      portalPage.populateField("portalCompanyStateField",
          data.get(TestingHubConstants.stateProvinceStore));

      Util.printInfo("Entering pin code : " + data.get(TestingHubConstants.pinCode));
      Util.sleep(2000);
      portalPage.populateField("portalCompanyPinCodeField", data.get(TestingHubConstants.pinCode));

      Util.printInfo("Selecting country : " + data.get(TestingHubConstants.country));
      Util.sleep(2000);
      Select sel = new Select(
          portalPage.getMultipleWebElementsfromField("portalCompanyCountryField").get(0));
      sel.selectByVisibleText(data.get(TestingHubConstants.country));

      Util.printInfo("Clicking on register button...");
      Util.sleep(2000);
      portalPage.clickUsingLowLevelActions("portalCompanyRegisterButton");
      Util.sleep(5000);
      try {
        portalPage.waitForElementToDisappear("portalPageLoaderIcon", 120);
      } catch (Exception e) {
        Util.printInfo("Loading icon did not appear on screen...");
      }
      //Checking error msg
      try {
        Util.printInfo("Checking error message...");
        Util.waitforPresenceOfElement(portalPage.getFirstFieldLocator("portalPinRegisterErrorMsg"));
        if (portalPage.getMultipleWebElementsfromField("portalPinRegisterErrorMsg").get(0)
            .isDisplayed()) {
          AssertUtils.fail(
              "Error msg from Portal : " + portalPage.getLinkText("portalPinRegisterErrorMsg"));
        }
      } catch (Exception e) {
        Util.printInfo("No error found...");
      }

      try {
        Util.printInfo("Checking success message is appeared or not...");
        Util.waitforPresenceOfElement(
            portalPage.getFirstFieldLocator("portalPinProductDownloadButton"));
        if (portalPage.getMultipleWebElementsfromField("portalPinRegistrationSuccessMsg").get(0)
            .isDisplayed() && portalPage
            .getMultipleWebElementsfromField("portalPinProductDownloadButton").get(0)
            .isDisplayed()) {
          Util.printInfo(
              "PIN is successfully activated and registered. POSA order is placed successfully...");
        } else {
          AssertUtils.fail(
              "Success msg and download button is not displayed on screen hence failing the test case...");
        }
      } catch (Exception e) {
        e.printStackTrace();
        AssertUtils.fail(
            "Success msg and download button is not displayed on screen hence failing the test case...");
      }

      Util.sleep(30000);
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Failed to activate pin in CEP...");
    }

    return results;
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
    executor.executeScript("arguments[0].click();", ele);
  }

  @Step("CEP : META Order capture " + GlobalConstants.TAG_TESTINGHUB)
  public boolean validateMetaOrderProductInCEP(String cepURL, String portalUserName,
      String portalPassword, String subscriptionID) {
    boolean status = false, statusBOC, statusBOS;
    openPortalBICLaunch(cepURL);
    if (isPortalLoginPageVisible()) {
      portalLogin(portalUserName, portalPassword);
    }

    if (isPortalTabsVisible()) {
      try {
        openSubscriptionsContractsLink();
        status = isSubscriptionDisplayedInBO(subscriptionID);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (!status) {
      AssertUtils.fail("Product is displayed in portal" + " :: false");
    }
    return status;
  }

  @Step("Open Subscriptions and Contracts link in Portal")
  public boolean openSubscriptionsContractsLink() {
    openPortalURL("https://stg-manage.autodesk.com/billing/subscriptions-contracts");
    return feynamnLayoutLoaded();
  }

  /*@Step("Click on Subscription link in BO")
  public boolean clickSubscriptionLink() {
    try {
      if (portalPage.checkIfElementExistsInPage("portalBOSubscriptionLink", 30)) {
        portalPage.clickUsingLowLevelActions("portalBOSubscriptionLink");
        feynamnLayoutLoaded();
      }

      String actualURL = driver.getCurrentUrl().trim();
      String expectedURL = TestingHubConstants.objSubscriptionLink;
      Util.printInfo("actualURL   : " + actualURL);
      Util.printInfo("expectedURL : " + expectedURL);
      boolean status = actualURL.equalsIgnoreCase(expectedURL);
      if (!status) {
        AssertUtils.fail(ErrorEnum.GENERIC_EXPECTION_ACTION.geterr()
            + " / navigate to Subscriptions Tab under Billing and Orders Section");
      }
    } catch (MetadataException e) {
      e.printStackTrace();
    }
    return feynamnLayoutLoaded();
  }
  */
}
