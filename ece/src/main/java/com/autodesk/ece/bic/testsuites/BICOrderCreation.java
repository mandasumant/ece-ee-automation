package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import io.qameta.allure.Step;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class BICOrderCreation extends ECETestBase {

	Map<?, ?> loadYaml = null;
	Map<?, ?> loadRestYaml = null;
	LinkedHashMap<String, String> testDataForEachMethod = null;
	long startTime, stopTime,executionTime;

	@BeforeClass(alwaysRun = true)
	public void beforeClass() {
		String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
		loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
		String restFileKey = "REST_" + GlobalConstants.ENV.toUpperCase();
		loadRestYaml = YamlUtil.loadYmlUsingTestManifest(restFileKey);
	}

	@BeforeMethod(alwaysRun = true)
	@SuppressWarnings("unchecked")
	public void beforeTestMethod(Method name) {
		LinkedHashMap<String, String> defaultvalues = (LinkedHashMap<String, String>) loadYaml.get("default");
		LinkedHashMap<String, String> testcasedata = (LinkedHashMap<String, String>) loadYaml.get(name.getName());
		LinkedHashMap<String, String> restdefaultvalues = (LinkedHashMap<String, String>) loadRestYaml.get("default");
		LinkedHashMap<String, String> regionalData = (LinkedHashMap<String, String>) loadYaml.get(System.getProperty("store"));
		defaultvalues.putAll(regionalData);
		defaultvalues.putAll(testcasedata);
		defaultvalues.putAll(restdefaultvalues);
		testDataForEachMethod = defaultvalues;
		String paymentType = System.getProperty("payment");
		testDataForEachMethod.put("paymentType", paymentType);
	}

	@Test(groups = { "bic-changePayment-US" }, description = "Validation of BIC change payment details functionality")
	public void validateBICChangePaymentProfile() {
		Util.printInfo("Gathering payment details...");
		String emailID = System.getProperty("email");
		String cepSSAP = System.getProperty("password");

		if (Strings.isNullOrEmpty(emailID)) {
			HashMap<String, String> results = getBicTestBase().createGUACBICOrderUS(testDataForEachMethod);
			emailID = results.get(BICConstants.emailid);
			cepSSAP = "Password1";

			updateTestingHub(results);
			results.putAll(testDataForEachMethod);
			// trigger Invoice join
			String baseUrl = results.get("postInvoicePelicanAPI");
			results.put("pelican_BaseUrl", baseUrl);
			pelicantb.postInvoicePelicanAPI(results);
			Util.sleep(180000);
		}

		ArrayList<String> payments = new ArrayList<String>();
		payments.add("VISA");
		payments.add("PAYPAL");
		payments.add("ACH");

		String paymentType = System.getProperty("payment");
		payments.remove(paymentType);
		Util.printInfo("Payment Type is : " + paymentType);

		int index = (int) Util.randomNumber(payments.size());

		paymentType = payments.get(index);
		testDataForEachMethod.put("paymentType", paymentType);

		portaltb.openPortalBICLaunch(testDataForEachMethod.get("cepURL"));

		if (!(Strings.isNullOrEmpty(System.getProperty("email")))) {
		portaltb.portalLogin(emailID, cepSSAP);
		}
		String[] paymentCardDetails = getBicTestBase().getPaymentDetails(paymentType.toUpperCase()).split("@");
		portaltb.changePaymentMethodAndValidate(testDataForEachMethod, paymentCardDetails);
	}

	@Test(groups = { "bic-nativeorder-US" }, description = "Validation of Create BIC Hybrid Order")
	public void validateBicNativeOrder() {
		HashMap<String, String> testResults = new HashMap<String, String> ();
		startTime = System.nanoTime();
		HashMap<String, String> results = getBicTestBase().createGUACBICOrderDotCom(testDataForEachMethod);
		Util.sleep(180000);
		results.putAll(testDataForEachMethod);

		testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
		testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
		updateTestingHub(testResults);

		// Getting a PurchaseOrder details from pelican
		String baseUrl = results.get("getPurchaseOrderDetails");
		baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(BICConstants.orderNumber));
		results.put("pelican_BaseUrl", baseUrl);
		results.putAll(getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

		// Get find Subscription ById
		baseUrl = results.get("getSubscriptionById");
		baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
		results.put("pelican_BaseUrl", baseUrl);
		results.putAll(pelicantb.getSubscriptionById(results));

		// trigger Invoice join
		baseUrl = results.get("postInvoicePelicanAPI");
		results.put("pelican_BaseUrl", baseUrl);
		pelicantb.postInvoicePelicanAPI(results);


		try {
			testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
			testResults.put(BICConstants.orderNumber, results.get(BICConstants.orderNumber));
			testResults.put(BICConstants.orderNumberSAP, results.get("orderNumberSAP"));
			testResults.put(BICConstants.orderState, results.get("getPOReponse_orderState"));
			testResults.put(BICConstants.fulfillmentStatus, results.get("getPOReponse_fulfillmentStatus"));
			testResults.put(BICConstants.fulfillmentDate, results.get("getPOReponse_fulfillmentDate"));
			testResults.put(BICConstants.subscriptionId, results.get("getPOReponse_subscriptionId"));
			testResults.put(BICConstants.subscriptionPeriodStartDate, results.get("getPOReponse_subscriptionPeriodStartDate"));
			testResults.put(BICConstants.subscriptionPeriodEndDate, results.get("getPOReponse_subscriptionPeriodEndDate"));
			testResults.put(BICConstants.nextBillingDate, results.get("response_nextBillingDate"));
			testResults.put(BICConstants.payment_ProfileId, results.get("getPOReponse_storedPaymentProfileId"));
		} catch (Exception e) {
			Util.printTestFailedMessage("Failed to update results to Testinghub");
		}
		updateTestingHub(testResults);

		portaltb.validateBICOrderProductInCEP(results.get(BICConstants.cepURL),	results.get(BICConstants.emailid), "Password1", results.get("getPOReponse_subscriptionId"));
		updateTestingHub(testResults);

	}


	@Step("Subscription : subs Validation" + GlobalConstants.TAG_TESTINGHUB)
	private HashMap<String, String> getPurchaseOrderDetails(String purchaseOrderAPIresponse) {
		HashMap<String, String> results = new HashMap<String, String>();
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
				//Native order response
				subscriptionId = doc.getElementsByTagName("offeringResponse").item(0).getAttributes()
					.getNamedItem("subscriptionId").getTextContent();
				System.out.println("subscriptionId :" + subscriptionId);
			}catch(Exception e){
				//Add seat order response
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
			results.put("getPOReponse_subscriptionPeriodEndDate",subscriptionPeriodEndDate );
			results.put("getPOReponse_fulfillmentDate", fulfillmentDate);

		} catch (Exception e) {
			Util.printTestFailedMessage("Unable to get Purchase Order Details");
			e.printStackTrace();
		}

		return results;
	}


}
