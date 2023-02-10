package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.BICTestBase;
import com.autodesk.ece.testbase.DatastoreClient;
import com.autodesk.ece.testbase.DatastoreClient.NewQuoteOrder;
import com.autodesk.ece.testbase.DatastoreClient.OrderData;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.PWSConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MOABOrder extends ECETestBase {

  private static final String defaultTaxOption = "undefined";
  private static final String defaultPriceId = "24038";
  private static final String CSN = System.getProperty("csn");
  Map<?, ?> loadYaml = null;
  long startTime;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  LinkedHashMap<String, Map<String, String>> localeDataMap = null;
  String locale = null;
  String taxOptionEnabled = System.getProperty(BICECEConstants.TAX_OPTION);
  String priceId = System.getProperty(BICECEConstants.PRICE_ID);
  private String PASSWORD;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String localeConfigFile = "LOCALE_CONFIG";
    localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    LinkedHashMap<String, String> testCaseData = (LinkedHashMap<String, String>) loadYaml
        .get(name.getName());
    defaultValues.putAll(testCaseData);
    testDataForEachMethod = defaultValues;
    locale = System.getProperty(BICECEConstants.LOCALE);
    if (locale == null || locale.trim().isEmpty()) {
      locale = "en_US";
    }
    testDataForEachMethod.put("locale", locale);

    localeDataMap = (LinkedHashMap<String, Map<String, String>>) localeConfigYaml
        .get(BICECEConstants.LOCALE_CONFIG);
    testDataForEachMethod.putAll(localeDataMap.get(locale));

    Util.printInfo(
        "Validating the store for the locale :" + locale + " Store: " + System.getProperty(BICECEConstants.STORE));

    boolean isValidStore = testDataForEachMethod.get(BICECEConstants.STORE_NAME)
        .equals(System.getProperty(BICECEConstants.STORE));

    if (!isValidStore) {
      AssertUtils
          .fail("The store  is not supported for the given country/locale : " + locale + ". Supported stores  are "
              + testDataForEachMethod.get(BICECEConstants.STORE_NAME));
    }

    BICTestBase.Names names = BICTestBase.generateFirstAndLastNames();
    testDataForEachMethod.put(BICECEConstants.FIRSTNAME, names.firstName);
    testDataForEachMethod.put(BICECEConstants.LASTNAME, names.lastName);

    String paymentType = System.getProperty("payment");
    testDataForEachMethod.put("paymentType", paymentType);

    PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));
  }

  @Test(groups = {"moab-payinvoice"}, description = "Validation for MOAB Pay Invoice")
  public void validateMOABPayInvoice() throws Exception {
    HashMap<String, String> testResults = new HashMap<String, String>();
    HashMap<String, String> results = new HashMap<String, String>();
    Boolean isLoggedOut = true;
    Integer attempt = 0;

    results.putAll(testDataForEachMethod);
    while (isLoggedOut) {
      attempt++;
      if (attempt > 5) {
        Assert.fail("Retries Exhausted: Payment of Invoice failed because Session issues. Check Screenshots!");
      }

      testDataForEachMethod.put(BICECEConstants.PURCHASER_EMAIL, System.getProperty(BICECEConstants.PURCHASER_EMAIL));
      portaltb.loginToAccountPortal(testDataForEachMethod, testDataForEachMethod.get(BICECEConstants.PURCHASER_EMAIL),
          PASSWORD);
      portaltb.openPortalInvoiceAndCreditMemoPage(testDataForEachMethod);
      portaltb.selectInvoiceUsingCSN(CSN);
      ArrayList<String> invoiceDetails = portaltb.selectMultipleInvoice(2);
      portaltb.selectAllInvoicesPayButton();
      isLoggedOut = portaltb.payInvoice(testDataForEachMethod);
      if (isLoggedOut == false) {
        portaltb.verifyPaidInvoiceStatus(invoiceDetails);
      }
    }
    updateTestingHub(testResults);
  }


  @Test(groups = {"moab-create-order"}, description = "Validation of MOAB test data creation")
  public void createMOABOrder() throws MetadataException {
    HashMap<String, String> testResults = new HashMap<String, String>();
    startTime = System.nanoTime();
    Util.printInfo("Creating MOAB order for locale :" + testDataForEachMethod.get(BICECEConstants.LOCALE));
    Map<String, String> address = getBicTestBase().getBillingAddress(testDataForEachMethod);
    HashMap<String, String> data = new LinkedHashMap<>();

    data.put(BICECEConstants.SKU, System.getProperty(BICECEConstants.SKU));
    data.put(BICECEConstants.QTY,
        System.getProperty(BICECEConstants.QUANTITY) != null ? System.getProperty(BICECEConstants.QUANTITY) : "1");
    data.put(PWSConstants.salesOrg, System.getProperty(BICECEConstants.SALES_ORG));
    data.put(PWSConstants.shipToCSN, System.getProperty(BICECEConstants.RESELLER_CSN));
    data.put(TestingHubConstants.soldToParty, System.getProperty(BICECEConstants.RESELLER_CSN));
    data.put(PWSConstants.resellerCSN, System.getProperty(BICECEConstants.RESELLER_CSN));
    data.put(TestingHubConstants.enduserCSN, System.getProperty(BICECEConstants.END_CUSTOMER_CSN));
    data.put(TestingHubConstants.csn, System.getProperty(BICECEConstants.END_CUSTOMER_CSN));
    data.put(PWSConstants.endCustomerAddressLine1, address.get(BICECEConstants.FULL_ADDRESS));
    data.put(PWSConstants.endCustomerAddressLine2, "");
    data.put(PWSConstants.endCustomerCity, address.get(BICECEConstants.CITY));
    data.put(PWSConstants.endCustomerCountryCode, address.get(BICECEConstants.COUNTRY));
    data.put(PWSConstants.endCustomerPostalCode, address.get(BICECEConstants.ZIPCODE));
    data.put(PWSConstants.endCustomerState, address.get(BICECEConstants.STATE_PROVINCE));
    data.put("PWSConstants.poNumber","PO" + (int) Util.randomNumber(999999));
    String content = data.entrySet()
        .stream()
        .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
        .collect(Collectors.joining(",\n "));
    Util.printInfo("The Data being used to place MOAB order: " + content);

    HashMap<String, String> orderResponse = thutil.createPWSOrderAndValidateInS4(data);
    Util.printInfo("The SOM Order created :" + orderResponse.get(BICECEConstants.SOM_ORDER_NUMBER));
    testResults.put(BICECEConstants.SOM_ORDER_NUMBER, orderResponse.get(BICECEConstants.SOM_ORDER_NUMBER));
    testResults.put(BICECEConstants.SOLD_TO_SSN, orderResponse.get(BICECEConstants.SOLD_TO_SSN));
    updateTestingHub(testResults);

    if (System.getProperty(BICECEConstants.APPLY_CM) != null && System.getProperty(BICECEConstants.APPLY_CM)
        .equals("Y")) {
      try {
        Util.printInfo("Inserting the datata into project78.");
        DatastoreClient dsClient = new DatastoreClient();
        OrderData orderDea = dsClient.queueOrder(NewQuoteOrder.builder()
            .name("RESELLER_ORDER")
            .tenant(System.getProperty(BICECEConstants.TENANT))
            .emailId(System.getProperty(BICECEConstants.PURCHASER_EMAIL))
            .orderNumber(new BigInteger(orderResponse.get(BICECEConstants.SOM_ORDER_NUMBER)))
            .paymentType(System.getProperty(BICECEConstants.PAYMENT_TYPE))
            .locale(locale)
            .address(System.getProperty(BICECEConstants.ADDRESS)).build());
        Util.printInfo("Inserted the datata into project78 successfully");
        updateTestingHub(testResults);
      } catch (Exception e) {
        e.printStackTrace();
        AssertUtils.fail("Failed to push order data to Project78.");
      }
    }
  }
}
