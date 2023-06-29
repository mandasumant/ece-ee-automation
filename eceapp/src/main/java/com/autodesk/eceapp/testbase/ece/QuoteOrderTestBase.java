package com.autodesk.eceapp.testbase.ece;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EcePortalTestBase;
import com.autodesk.eceapp.utilities.Address;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.constants.BICConstants;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;

public class QuoteOrderTestBase {
    private PWSTestBase pwsTestBase;
    public QuoteOrderTestBase(
            final String pwsClientId,
            final String encryptedPwsClientSecret,
            final String pwsClientId_v2,
            final String encryptedPwsClientSecret_v2,
            final String pwsHostname) {
        Util.PrintInfo("QuoteOrderTestBase from ece");
        pwsTestBase = new PWSTestBase(pwsClientId,
                encryptedPwsClientSecret,
                pwsClientId_v2,
                encryptedPwsClientSecret_v2,
                pwsHostname);
    }

    public HashMap<String, String> createQuoteOrder(
            LinkedHashMap<String, String> testDataForEachMethod,
            EcePortalTestBase portaltb, EceBICTestBase bicTestBase, PelicanTestBase pelicantb,
            SubscriptionServiceV4TestBase subscriptionServiceV4Testbase,
            Consumer<HashMap<String, String>> updateTestingHubFunc
    ) throws MetadataException {
        final String PASSWORD = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.PASSWORD));
        HashMap<String, String> testResults = new HashMap<String, String>();
        HashMap<String, String> results = new HashMap<String, String>();
        Address address = getBillingAddress(testDataForEachMethod);

        Boolean isMultiLineItem =
                !Objects.isNull(System.getProperty(BICECEConstants.IS_MULTILINE)) ? Boolean.valueOf(
                        System.getProperty(BICECEConstants.IS_MULTILINE)) : false;

        if (Objects.equals(System.getProperty(BICECEConstants.CREATE_PAYER), BICECEConstants.TRUE)) {
            testResults = bicTestBase.createPayerAccount(testDataForEachMethod);
        }

        bicTestBase.goToDotcomSignin(testDataForEachMethod);
        bicTestBase.createBICAccount(
                new EceBICTestBase.Names(testDataForEachMethod.get(BICECEConstants.FIRSTNAME),
                        testDataForEachMethod.get(BICECEConstants.LASTNAME)),
                testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD, true);

        String quoteId = pwsTestBase.createAndFinalizeQuote(address, testDataForEachMethod.get("quoteAgentCsnAccount"),
                testDataForEachMethod.get("agentContactEmail"), testDataForEachMethod, isMultiLineItem);

        // Wait for Quote to sync from CPQ/SFDC to S4.
        Util.printInfo("Keep calm, sleeping for 5min for Quote to sync to S4");
        Util.sleep(300000);

        testDataForEachMethod.put(BICECEConstants.QUOTE_ID, quoteId);
        testResults.put(BICECEConstants.QUOTE_ID, quoteId);

        // Signing out after quote creation
        bicTestBase.getUrl(testDataForEachMethod.get("oxygenLogOut"));

        testDataForEachMethod.put("quote2OrderCartURL", bicTestBase.getQuote2OrderCartURL(
                testDataForEachMethod));
        bicTestBase.navigateToQuoteCheckout(testDataForEachMethod);
        testResults.put("checkoutUrl", testDataForEachMethod.get("checkoutUrl"));
        testResults.put("emailId", testDataForEachMethod.get(BICECEConstants.emailid));

        // Re login during checkout
        bicTestBase.loginToOxygen(testDataForEachMethod.get(BICECEConstants.emailid), PASSWORD);
        bicTestBase.refreshCartIfEmpty();
        String oxygenId = bicTestBase.driver.manage().getCookieNamed("identity-sso").getValue();
        testDataForEachMethod.put(BICConstants.oxid, oxygenId);

        testResults.putAll(results);
        updateTestingHubFunc.accept(testResults);

        results = bicTestBase.createQuoteOrder(testDataForEachMethod);
        results.putAll(testDataForEachMethod);

        testResults.putAll(testDataForEachMethod);
        updateTestingHubFunc.accept(testResults);

        // Getting a PurchaseOrder details from pelican
        results.putAll(pelicantb.getPurchaseOrderV4Details(pelicantb.retryO2PGetPurchaseOrder(results)));

        // Validate Quote Details with Pelican
         pelicantb.validateQuoteDetailsWithPelican(testDataForEachMethod, results, address);

        // Get find Subscription ById
        results.putAll(
                subscriptionServiceV4Testbase.getSubscriptionById(results.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID)));

        testDataForEachMethod.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));

        try {
            testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
            testResults.put(BICECEConstants.ORDER_ID, results.get(BICECEConstants.ORDER_ID));
            testResults.put(BICConstants.orderState, results.get(BICECEConstants.ORDER_STATE));
            testResults.put(BICConstants.fulfillmentStatus, results.get(BICECEConstants.FULFILLMENT_STATUS));
            testResults.put(BICConstants.fulfillmentDate, results.get(BICECEConstants.FULFILLMENT_DATE));
            testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
            testResults.put(BICConstants.subscriptionPeriodStartDate,
                    results.get(BICECEConstants.SUBSCRIPTION_PERIOD_START_DATE));
            testResults.put(BICConstants.subscriptionPeriodEndDate,
                    results.get(BICECEConstants.SUBSCRIPTION_PERIOD_END_DATE));
            testResults.put(BICConstants.nextBillingDate, results.get(BICECEConstants.NEXT_BILLING_DATE));
            testResults.put(BICConstants.payment_ProfileId, results.get(BICECEConstants.PAYMENT_PROFILE_ID));
            testResults.put(BICECEConstants.PAYER_CSN, results.get(BICECEConstants.PAYER_CSN));
        } catch (Exception e) {
            Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
        }

        portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL), results.get(BICConstants.emailid),
                PASSWORD, results.get(BICECEConstants.SUBSCRIPTION_ID));
        if (!testDataForEachMethod.get(BICECEConstants.PAYMENT_TYPE).equals(BICECEConstants.PAYMENT_BACS)) {
            portaltb.validateBICOrderDetails(results.get(BICECEConstants.FINAL_TAX_AMOUNT));
        }

        portaltb.checkIfQuoteIsStillPresent(testResults.get("quoteId"));
        testResults.put(BICConstants.subscriptionId, results.get(BICECEConstants.SUBSCRIPTION_ID));
        return testResults;
    }

    private String getSerializedBillingAddress(LinkedHashMap<String, String> testData) {
        String billingAddress;
        String addressViaParam = System.getProperty(BICECEConstants.ADDRESS);
        if (addressViaParam != null && !addressViaParam.isEmpty()) {
            Util.printInfo("The address is passed as parameter : " + addressViaParam);
            billingAddress = addressViaParam;
        } else {
            billingAddress = testData.get(BICECEConstants.ADDRESS);
        }
        return billingAddress;
    }

    private Address getBillingAddress(LinkedHashMap<String, String> testData) {
        String billingAddress = getSerializedBillingAddress(testData);
        return new Address(billingAddress);
    }
}
