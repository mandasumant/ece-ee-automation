package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import io.qameta.allure.Step;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BICOrderPromoCode extends ECETestBase {

	Map<?, ?> loadYaml = null;
	Map<?, ?> loadRestYaml = null;
	long startTime, stopTime, executionTime;
	LinkedHashMap<String, String> testDataForEachMethod = null;

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
		LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml.get("default");
		LinkedHashMap<String, String> testCaseData = (LinkedHashMap<String, String>) loadYaml.get(name.getName());
		LinkedHashMap<String, String> restDefaultValues = (LinkedHashMap<String, String>) loadRestYaml.get("default");
		LinkedHashMap<String, String> regionalData = (LinkedHashMap<String, String>) loadYaml.get(System.getProperty("store"));
		defaultValues.putAll(regionalData);
		defaultValues.putAll(testCaseData);
		defaultValues.putAll(restDefaultValues);
		testDataForEachMethod = defaultValues;
		String paymentType = System.getProperty("payment");
		testDataForEachMethod.put("paymentType", paymentType);
	}

	@Test(groups = { "bic-promocode-order" }, description = "Validation of Create BIC Hybrid Order")
	public void promocodeBicOrder() {
		HashMap<String, String> testResults = new HashMap<String, String> ();
		startTime = System.nanoTime();
		HashMap<String, String> results = getBicTestBase().createGUACBic_Orders_PromoCode(testDataForEachMethod);

		results.putAll(testDataForEachMethod);

		testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
		testResults.put(TestingHubConstants.orderNumber, results.get(TestingHubConstants.orderNumber));
		updateTestingHub(testResults);

		// Getting a PurchaseOrder details from pelican
		String baseUrl = results.get("getPurchaseOrderDetails");
		baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get(TestingHubConstants.orderNumber));
		results.put("pelican_BaseUrl", baseUrl);
		results.putAll(getPurchaseOrderDetails(pelicantb.getPelicanResponse(results)));

		// Get find Subscription ById
		baseUrl = results.get("getSubscriptionById");
		baseUrl = pelicantb.addTokenInResourceUrl(baseUrl, results.get("getPOReponse_subscriptionId"));
		results.put("pelican_BaseUrl", baseUrl);
		results.putAll(pelicantb.getSubscriptionById(results));

		String promotionDiscount = results.get("getPOResponse_promotionDiscount");

		AssertUtils.assertTrue(Float.parseFloat(promotionDiscount) > 0, "Promotion was applied");

		// trigger Invoice join
		baseUrl = results.get("postInvoicePelicanAPI");
		results.put("pelican_BaseUrl", baseUrl);
		pelicantb.postInvoicePelicanAPI(results);

		try {
			testResults.put(TestingHubConstants.emailid, results.get(TestingHubConstants.emailid));
			testResults.put(TestingHubConstants.orderNumber, results.get(TestingHubConstants.orderNumber));
			testResults.put("orderState", results.get("getPOReponse_orderState")); 
			testResults.put("fulfillmentStatus", results.get("getPOReponse_fulfillmentStatus")); 
			testResults.put("fulfillmentDate", results.get("getPOReponse_fulfillmentDate"));
			testResults.put("subscriptionId", results.get("getPOReponse_subscriptionId"));
			testResults.put("subscriptionPeriodStartDate", results.get("getPOReponse_subscriptionPeriodStartDate"));
			testResults.put("subscriptionPeriodEndDate", results.get("getPOReponse_subscriptionPeriodEndDate"));
			testResults.put("nextBillingDate", results.get("response_nextBillingDate"));
			testResults.put("payment_ProfileId", results.get("getPOReponse_storedPaymentProfileId")); 
			testResults.put("priceBeforePromo", results.get("priceBeforePromo")); 
			testResults.put("priceAfterPromo", results.get("priceAfterPromo"));
		} catch (Exception e) {
			Util.printTestFailedMessage("Failed to update results to Testinghub");
		}
		updateTestingHub(testResults);

		portaltb.validateBICOrderProductinCEP(results.get(TestingHubConstants.cepURL), results.get(TestingHubConstants.emailid), "Password1", results.get("getPOReponse_subscriptionId"));
		updateTestingHub(testResults);

		//Validate Create Order
		stopTime = System.nanoTime();
		executionTime =((stopTime - startTime)/60000000000L);
		testResults.put("e2e_ExecutionTime", String.valueOf(executionTime));
		updateTestingHub(testResults);
	}

	@Step("Subscription : subs Validation" + GlobalConstants.TAG_TESTINGHUB)
	private HashMap<String, String> getPurchaseOrderDetails(String purchaseOrderAPIresponse) {
		HashMap<String, String> results = new HashMap<String, String>();
		try {
			System.out.println(System.getProperty("testGroup"));
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

			String promotionDiscount = doc.getElementsByTagName("promotionDiscount").item(0).getTextContent();

			results.put("getPOReponse_orderState", orderState);
			results.put("getPOReponse_subscriptionId", subscriptionId);
			results.put("getPOReponse_storedPaymentProfileId", storedPaymentProfileId);
			results.put("getPOReponse_fulfillmentStatus", fulfillmentStatus);
			results.put("getPOReponse_subscriptionPeriodStartDate", subscriptionPeriodStartDate);
			results.put("getPOReponse_subscriptionPeriodEndDate",subscriptionPeriodEndDate );
			results.put("getPOReponse_fulfillmentDate", fulfillmentDate);
			results.put("getPOResponse_promotionDiscount", promotionDiscount);

		} catch (Exception e) {
			Util.printTestFailedMessage("Unable to get Purchase Order Details");
			e.printStackTrace();
		}

		return results;
	}

}
