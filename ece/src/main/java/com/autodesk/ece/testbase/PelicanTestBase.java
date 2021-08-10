package com.autodesk.ece.testbase;

import static io.restassured.RestAssured.given;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.autodesk.testinghub.core.common.CommonConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.openqa.selenium.json.Json;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.services.ApigeeAuthenticationService;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ErrorEnum;
import com.autodesk.testinghub.core.utils.LoadJsonWithValue;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.bicapiModel.PayloadAddPaymentProfile;
import com.autodesk.testinghub.core.bicapiModel.PayloadCartPriceID;
import com.autodesk.testinghub.core.bicapiModel.PayloadPriceQuoteTax;
import com.autodesk.testinghub.core.bicapiModel.PayloadPurchaseCall;
import com.autodesk.testinghub.core.bicapiModel.PayloadSpocAUTHToken;
import com.autodesk.testinghub.core.bicapiModel.UpdateNextBilling;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class PelicanTestBase {

	public PelicanTestBase() {
		Util.PrintInfo("PelicanTestBase from core");
	}

	public Map<String, String> getHeaders(HashMap<String, String> data) {
		String sig_details = getPriceByPriceIdSignature(data);
		String hmacSignature = sig_details.split("::")[0];
		String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
		String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
		String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

		String Content_Type = "application/vnd.api+json";

		Map<String, String> header = new HashMap<>();
		header.put("X-E2-HMAC-Signature", hmacSignature);
		header.put("X-E2-PartnerId", X_E2_PartnerId);
		header.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
		header.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
		header.put("Content-Type", Content_Type);

		return header;
	}

	public HashMap<String, String> getPriceDetails(HashMap<String, String> data) {
		HashMap<String, String> results = new HashMap<String, String>();
		String priceId = data.get("priceId");

		String URL = data.get("getPriceByPriceID");
		String baseURL = URL + priceId;
		System.out.println("getPriceDetails baseURL : " + baseURL);
		String Content_Type = "application/vnd.api+json";
		Map<String, String> header = getHeaders(data);
		header.put("Accept", Content_Type);
		Response response = getRestResponse(baseURL, header);
		String result = response.getBody().asString();
		Util.PrintInfo("result :: " + result);
		JsonPath js = new JsonPath(result);
		Util.printInfo("js is:" + js);
		Util.printInfo("js is:" + js);

		results.put("response_priceID", js.get("data.id"));
		results.put("response_currency", js.get("data.currency"));
		results.put("response_amount", js.get("data.amount"));
		results.put("response_renewalAmount", js.get("data.renewalAmount"));
		results.put("response_status", js.get("data.status"));
		results.put("response_storeId", js.get("data.storeId"));
		results.put("response_storeExternalKey", js.get("data.storeExternalKey"));
		results.put("response_currencyId", js.get("data.currencyId"));
		results.put("response_priceListId", js.get("data.priceListId"));
		results.put("response_priceListExternalKey", js.get("data.priceListExternalKey"));

		return results;

	}

	@Step("Order Service : Order Capture" + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> getSubscriptionById(HashMap<String, String> data) {
		System.out.println();
		HashMap<String, String> results = new HashMap<String, String>();
		String baseURL = data.get("pelican_BaseUrl");
		System.out.println("getPriceDetails baseURL : " + baseURL);
		String sig_details = getPriceByPriceIdSignature(data);
		String hmacSignature = sig_details.split("::")[0];
		String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
		String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
		String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

		String Content_Type = "application/vnd.api+json";

		Map<String, String> header = new HashMap<>();
		header.put("X-E2-HMAC-Signature", hmacSignature);
		header.put("X-E2-PartnerId", X_E2_PartnerId);
		header.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
		header.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
		header.put("Content-Type", Content_Type);
		header.put("Accept", Content_Type);

		Response response = getRestResponse(baseURL, header);
		String result = response.getBody().asString();
		Util.PrintInfo("result :: " + result);
		JsonPath js = new JsonPath(result);
		Util.printInfo("js is:" + js);

		try {
			results.put("response_priceID", Integer.toString(js.get("data.lastBillingInfo.purchaseOrderId")));
			results.put("response_nextBillingDate", js.get("data.nextBillingDate"));
			results.put("response_subscriptionQuantity", Integer.toString(js.get("data.quantity")));

		} catch (Exception e) {

			e.printStackTrace();
		}

		return results;

	}

	@Step("Update next billing cycle with before date " + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> patchNextBillSubscriptionById(HashMap<String, String> data) {

		String baseURL = data.get("pelican_BaseUrl");
		System.out.println("getPriceDetails baseURL : " + baseURL);
		String sig_details = getPriceByPriceIdSignature(data);
		String hmacSignature = sig_details.split("::")[0];
		String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
		String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
		String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

		String Content_Type = "application/json";
		String accept = "application/vnd.api+json";
		HashMap<String, String> header = new HashMap<>();
		header.put("X-E2-HMAC-Signature", hmacSignature);
		header.put("X-E2-PartnerId", X_E2_PartnerId);
		header.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
		header.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
		header.put("X-Request-Ref", UUID.randomUUID().toString());
		header.put("Content-Type", Content_Type);
		header.put("accept", accept);

		String contractStartDate = Util.customDate("MM/dd/yyyy", 0, -5, 0) + " 20:13:28 UTC";
		String path = Util.getCorePayloadPath() + "BIC_Update_NextBilling.json";
		File rawPayload = new File(path);
		UpdateNextBilling nextBilingJson;
		ObjectMapper om = new ObjectMapper();
		String inputPayload = "";
		try {
			nextBilingJson = om.readValue(rawPayload, UpdateNextBilling.class);
			nextBilingJson.getData().setNextBillingDate(contractStartDate);
			inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(nextBilingJson);
			Util.PrintInfo("Payload Auth:: " + inputPayload + "\n");
		} catch (IOException e1) {
			e1.printStackTrace();
			AssertUtils.fail("Failed to generate SPOC  Authorization Token" + e1.getMessage());
		}

		return patchRestResponse(baseURL, header, inputPayload);

	}

	public HashMap<String, String> patchRestResponse(String baseUrl, HashMap<String, String> header, String body) {
		Util.printInfo("Hitting the URL = " + baseUrl);
		HashMap<String, String> results = new HashMap<String, String>();
		try {
			Response response = given().headers(header).and().body(body).when().patch(baseUrl).then().extract()
					.response();

			int responseStatusCode = response.getStatusCode();
			System.out.println("Response code : " + responseStatusCode);
			if (responseStatusCode != 200)
				AssertUtils.assertTrue(false, "Response code must be 200 but the API return " + responseStatusCode);

			String result = response.getBody().asString();
			System.out.println("" + result);
			JsonPath jp = new JsonPath(result);
			String purchaseOrderId = jp.get("data.purchaseOrderId").toString();
			String exportControlStatus = jp.get("data.exportControlStatus").toString();
			String priceId = jp.get("data.priceId").toString();
			String nextBillingDate = jp.get("data.nextBillingInfo.nextBillingDate").toString();

			results.put("purchaseOrderId", purchaseOrderId);
			results.put("exportControlStatus", exportControlStatus);
			results.put("priceId", priceId);
			results.put("nextBillingDate", nextBillingDate);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	@SuppressWarnings("unused")
	@Step("Create invoice for PO " + GlobalConstants.TAG_TESTINGHUB)
	public void postInvoicePelicanAPI(HashMap<String, String> data) {
		HashMap<String, String> results = new HashMap<String, String>();
		String baseURL = data.get("pelican_BaseUrl");
		System.out.println("getPriceDetails baseURL : " + baseURL);
		String Content_Type = "application/json";

		Map<String, String> header = new HashMap<>();
		header.put("Content-Type", Content_Type);

		Response response = getRestResponse(baseURL, header);
		String result = response.getBody().asString();
		Util.PrintInfo("result :: " + result);
		JsonPath js = new JsonPath(result);
		Util.printInfo("js is:" + js);

	}

	// @Step("Validate BIC Order in Pelican" + GlobalConstants.TAG_TESTINGHUB)
	@SuppressWarnings("unused")
	public String getPelicanResponse(HashMap<String, String> data) {
		HashMap<String, String> results = new HashMap<String, String>();
		String baseURL = data.get("pelican_BaseUrl");
		System.out.println("baseURL : " + baseURL);
		String sig_details = getPriceByPriceIdSignature(data);
		String hmacSignature = sig_details.split("::")[0];
		String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
		String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
		String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

		String Content_Type = "application/x-www-form-urlencoded; charset=UTF-8";

		Map<String, String> header = new HashMap<>();
		header.put("X-E2-HMAC-Signature", hmacSignature);
		header.put("X-E2-PartnerId", X_E2_PartnerId);
		header.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
		header.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
		header.put("Content-Type", Content_Type);
		// header.put("Accept", "application/xml");

		Response response = getRestResponse(baseURL, header);
		String result = response.getBody().asString();
		Util.PrintInfo("result :: " + result);

		return result;

	}

	public HashMap<String, String> createRefundOrder(HashMap<String, String> data) {
		HashMap<String, String> results = new HashMap<String, String>();
		String baseURL = data.get("pelican_BaseUrl");
		System.out.println("putPelicanRefund details : " + baseURL);
		String sig_details = getPriceByPriceIdSignature(data);
		String hmacSignature = sig_details.split("::")[0];
		String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
		String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
		String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

		// String Content_Type="application/x-www-form-urlencoded; charset=UTF-8";
		String Content_Type = "application/json";

		Map<String, String> header = new HashMap<>();
		header.put("x-e2-hmac-signature", hmacSignature);
		header.put("x-e2-partnerid", X_E2_PartnerId);
		header.put("x-e2-appfamilyid", X_E2_AppFamilyId);
		header.put("x-e2-hmac-timestamp", X_E2_HMAC_Timestamp);
		header.put("Content-Type", Content_Type);
		header.put("Accept", Content_Type);

		Response response = createRefundOrder(baseURL, header);
		String result = response.getBody().asString();
		Util.PrintInfo("result :: " + result);
		JsonPath js = new JsonPath(result);

		results.put("refund_orderState", js.get("orderState"));
		results.put("refund_fulfillmentStatus", js.get("fulfillmentStatus"));
		results.put("refund_paymentMethodType", js.get("billingInfo.paymentMethodType"));
		results.put("refund_finalExportControlStatus", js.get("finalExportControlStatus"));
		results.put("refund_uiInitiatedGetOrders", Boolean.toString(js.get("uiInitiatedGetOrders")));
		results.put("refund_lineItemState", js.get("lineItems[0].lineItemState"));

		return results;

	}

	@SuppressWarnings("unchecked")
	public static Response createRefundOrder(String baseUrl, Map<String, String> header) {

		JSONObject requestParams = new JSONObject();
		requestParams.put("orderEvent", "REFUND");

		Response response = RestAssured.given().headers(header).body(requestParams).put(baseUrl);
		System.out.println(response.asString());

		int statusCode = response.getStatusCode();
		if (statusCode != 200) {
			String result = response.getBody().asString();
			Util.PrintInfo("result :: " + result);
			JsonPath js = new JsonPath(result);
			String message = js.get("message");
			AssertUtils.fail("Error while Refunding the Order -" + message);
		}
//		     Assert.assertEquals(statusCode, 200);

		return response;
	}

	private String getPriceByPriceIdSignature(HashMap<String, String> data) {
		String signature = "";
		String timestamp = "";
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			String terceS = data.get("getPriceDetails_terceS_ssap");
			SecretKeySpec keySpec = new SecretKeySpec(terceS.getBytes(), "HmacSHA256");
			mac.init(keySpec);

			String appFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");
			String partnerId = data.get("getPriceDetails_X_E2_PartnerId");

			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			timestamp = String.valueOf(cal.getTimeInMillis() / 1000);

			String message = new StringBuilder().append(partnerId).append(appFamilyId).append(timestamp).toString();

			byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
			// signature = getSHA256Hash(secret, message);
			signature = hex(signatureBytes);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return signature + "::" + timestamp;
	}

	public static String hex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte aByte : bytes) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	public static Response getRestResponse(String baseUrl, Map<String, String> header) {
		Response response = null;
		try {
			Util.printInfo("Hitting the URL = " + baseUrl);
			RestAssured.baseURI = baseUrl;
			response = given().headers(header).when().get();
			String result = response.getBody().asString();
			Util.printInfo("results from the url-" + result);
			if (response.getStatusCode() != 200)
				Assert.assertTrue(false, "Response code must be 200 but the API return " + response.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;

	}

	public String getSpocTimeStamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(new Date());
	}

	public String getSpocSignature(String o2id, String email, String secretKey, String timeStamp) {
		String hash = null;
		String hashCode = null;
		hashCode = o2id + email + timeStamp;
		hash = ApigeeAuthenticationService.getSHA256Hash(secretKey, hashCode);
		return hash;
	}

	public String getSpocAuthToken(LinkedHashMap<String, String> testDataForEachMethod) {
		String hashedStr = "Basic ";
		RestAssured.baseURI = CommonConstants.spocAuthTokenUrl;
		File rawPayload = new File(Util.getCorePayloadPath()+"PayloadSpoc_AUTHToken.json");

		String timeStamp = getSpocTimeStamp();
		String email = testDataForEachMethod.get(TestingHubConstants.emailid);
		String o2id = testDataForEachMethod.get(TestingHubConstants.oxygenid);
		String secretKey = ProtectedConfigFile.decrypt(CommonConstants.spocSecretKey);
		String spocSignature = getSpocSignature(o2id, email, secretKey, timeStamp);
		HashMap<String, String> authHeaders = new HashMap<String, String>();
		authHeaders.put("Content-Type", "application/json");

		PayloadSpocAUTHToken authJsonClass = null;

		ObjectMapper om = new ObjectMapper();
		String inputPayload = "";
		try {
			authJsonClass = om.readValue(rawPayload, PayloadSpocAUTHToken.class);

			authJsonClass.setEmail(email);
			authJsonClass.setSignature(spocSignature);
			authJsonClass.setTimestamp(timeStamp);
			authJsonClass.setUserExtKey(o2id);

			inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonClass);
			Util.PrintInfo("Payload Auth:: " + inputPayload + "\n");
		} catch (IOException e1) {
			e1.printStackTrace();
			AssertUtils.fail("Failed to generate SPOC  Authorization Token" + e1.getMessage());
		}

		Response response = given().headers(authHeaders).body(inputPayload).when().post();
		String result = response.getBody().asString();
		JsonPath jp = new JsonPath(result);
		String sessionId = jp.get("sessionId");
		String grantToken = jp.get("grantToken");

		String mixCode = sessionId + ":" + grantToken;
		try {
			hashedStr = hashedStr
					+ java.util.Base64.getEncoder().encodeToString(mixCode.getBytes(StandardCharsets.UTF_8.toString()));

		} catch (Exception e) {
			AssertUtils.fail("Failed to generate BIC Authorization Token");
		}

		return hashedStr.trim();
	}

	public String getStudentAuth2Token(LinkedHashMap<String, String> testDataForEachMethod) {
		String access_token = null;
		try {
			RestAssured.baseURI = testDataForEachMethod.get("ipaAuthEndPoint").trim();
			Response request = RestAssured.given()
					.config(RestAssured.config()
							.encoderConfig(EncoderConfig.encoderConfig().encodeContentTypeAs("x-www-form-urlencoded",
									ContentType.URLENC)))
					.contentType(ContentType.URLENC.withCharset("UTF-8")).formParam("grant_type", "client_credentials")
					.formParam("username", "c=4rjhaonlqlmaq2vjbildkltqc2")
					.formParam("password", "c=130jouof6veij2gv2e0odcln0tlf0f66lcb4nhr196tl8f227ssa")
					.header("x-api-key", "1gj7n05VdoVT88rtPqMfpsum0sWdIGSQGEzykOk4")
					.header("Authorization",
							"Basic Yz00cmpoYW9ubHFsbWFxMnZqYmlsZGtsdHFjMjpjPTEzMGpvdW9mNnZlaWoyZ3YyZTBvZGNsbjB0bGYwZjY2bGNiNG5ocjE5NnRsOGYyMjdzc2E=")
					.post("/oauth2/token");

			String responsestring = request.asString();
			Util.printInfo(responsestring);

			if (responsestring.contains("invalid")) {
				Util.printTestFailedMessage("Unable to generate Token for StudentAPI, it contains Invalid cert : "
						+ responsestring + "\n" + "AWS US-West-2 creds might have failed over");
				request = RestAssured.given()
						.config(RestAssured.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs("x-www-form-urlencoded", ContentType.URLENC)))
						.contentType(ContentType.URLENC.withCharset("UTF-8"))
						.formParam("grant_type", "client_credentials")
						.formParam("username", "d=5h71dlrm1rqoildi81d857ppnm")
						.formParam("password", "d=1uacvl3u6onm6ktfa2uu072t3i3f15j3knlscikbh17gh2esf44b")
						.header("x-api-key", "1gj7n05VdoVT88rtPqMfpsum0sWdIGSQGEzykOk4")
						.header("Authorization",
								"Basic ZD01aDcxZGxybTFycW9pbGRpODFkODU3cHBubTpkPTF1YWN2bDN1Nm9ubTZrdGZhMnV1MDcydDNpM2YxNWoza25sc2Npa2JoMTdnaDJlc2Y0NGI=")
						.post("/oauth2/token");

				responsestring = request.asString();
				Util.printInfo(responsestring);
			}

			if (responsestring.contains("invalid")) {
				Util.printTestFailedMessage("Unable to generate Token for StudentAPI, it contains Invalid cert : "
						+ responsestring + "\n" + "AWS US-East-1 creds might have failed over");
				AssertUtils
						.fail("Unable to generate Token for StudentAPI, it contains Invalid cert : " + responsestring);
			}
			JsonPath js = new JsonPath(responsestring);
			Util.printInfo("js is:" + js);
			access_token = js.get("access_token");
			Util.printInfo("token is:" + access_token);

			if (access_token == null) {
				Util.printTestFailedMessage("Access Token was null with RestAssured");
				// access_token=createAuthRequestHeader().trim();
				if (access_token == null)
					AssertUtils.fail("Unable to generate Token for StudentAPI > access_token : " + access_token);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Util.printTestFailedMessage("Access Token was null with RestAssured" + e.getMessage());
			// access_token=createAuthRequestHeader().trim();
			System.out.println("access_token : " + access_token);
			if (access_token == null) {
				AssertUtils.fail("Unable to generate Token for StudentAPI > access_token : " + access_token);
			}
		}
		return "Bearer " + access_token;
	}

	public String getCartId(LinkedHashMap<String, String> testDataForEachMethod) {
		String cartID = "";
		try {
			String processor = testDataForEachMethod.get("billingProcessor").toLowerCase().equals("adyen") ? "adyen"
					: "bluesnap";
			File jsonFile = ApigeeTestBase.getFile(processor, "PayloadCartPriceID.json");
			String priceId = testDataForEachMethod.get("priceId");
			String priceIdquantity = testDataForEachMethod.get("quantity");
			String storeKey = testDataForEachMethod.get("store");

			RestAssured.baseURI = TestingHubConstants.postCartPriceID;
			// 2 arguments at runtime priceId & quantity
			ObjectMapper om = new ObjectMapper();
			String inputPayload = "";
			try {
				if (processor.equals("adyen")) {
					PayloadCartPriceID authJsonClass = om.readValue(jsonFile, PayloadCartPriceID.class);
					Util.PrintInfo("authJsonClass.getLineItems().size() :: " + authJsonClass.getLineItems().size());
					List<PayloadCartPriceID.LineItem> lineItems = authJsonClass.getLineItems();
					String[] priceids = priceId.split(";");
					String[] quantities = priceIdquantity.split(";");
					for (int i = 0; i < priceids.length; i++) {
						priceId = priceids[i];
						int quantity = Integer.parseInt(quantities[i]);
						PayloadCartPriceID.LineItem li = new PayloadCartPriceID.LineItem();
						li.setPriceId(priceId);
						li.setQuantity(quantity);
						lineItems.add(li);

					}
					lineItems.remove(0);
					authJsonClass.setLineItems(lineItems);
					authJsonClass.setStoreKey(storeKey);

					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonClass);
					Util.PrintInfo("Payload Auth:: " + inputPayload + "\n");
				} else {
					PayloadCartPriceID authJsonBluesnapClass = om.readValue(jsonFile, PayloadCartPriceID.class);
					Util.PrintInfo(
							"authJsonClass.getLineItems().size() :: " + authJsonBluesnapClass.getLineItems().size());
					List<PayloadCartPriceID.LineItem> lineItems = authJsonBluesnapClass.getLineItems();
					String[] priceids = priceId.split(";");
					String[] quantities = priceIdquantity.split(";");
					for (int i = 0; i < priceids.length; i++) {
						priceId = priceids[i];
						int quantity = Integer.parseInt(quantities[i]);
						PayloadCartPriceID.LineItem li = new PayloadCartPriceID.LineItem();
						li.setPriceId(priceId);
						li.setQuantity(quantity);
						lineItems.add(li);
//	            authJsonBluesnapClass.getLineItems().get(0).setPriceId(priceId);
//	            authJsonBluesnapClass.getLineItems().get(0).setQuantity(quantity);
					}
					lineItems.remove(0);
					authJsonBluesnapClass.setLineItems(lineItems);
					authJsonBluesnapClass.setStoreKey(storeKey);
					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonBluesnapClass);
					Util.PrintInfo("Payload Auth:: " + inputPayload + "\n");
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				AssertUtils.fail("Failed to generate CartID" + e1.getMessage());
			}

			HashMap<String, String> authHeaders = new HashMap<String, String>();
			authHeaders.put("Authorization", testDataForEachMethod.get("Authorization"));
			authHeaders.put("Content-Type", "application/json");
			Util.PrintInfo("authHeaders :: " + authHeaders);
			Response response = given().headers(authHeaders).body(inputPayload).when().post();
			String result = response.getBody().asString();
			System.out.println("result :: " + result);

			if (response.getStatusCode() != 200) {
				Util.printTestFailedMessage(
						"Response code must be 200 for Cart ID call but the API return " + response.getStatusCode());
				AssertUtils.fail(ErrorEnum.GENERIC_RETRY_MSG.geterr());
			}

			if (result.contains("Invalid item in cart. Item will be removed from cart")) {
				Util.printTestFailedMessage(result);
				AssertUtils.fail(ErrorEnum.INVALID_PRICEID_QTY.geterr());
			}

			JsonPath jp = new JsonPath(result);
			cartID = jp.get("id");
			Util.PrintInfo(" cartID response :: " + cartID + "\n");
		} catch (Exception e) {
			e.printStackTrace();
			AssertUtils.fail(ErrorEnum.SYSTEM_ERROR.geterr());
		}
		return cartID;
	}

	public String getPaymentProfileId(LinkedHashMap<String, String> testDataForEachMethod) {
		System.out.println("");
		String paymentProfileID = "";
		try {
			ObjectMapper om = new ObjectMapper();
			String inputPayload = "";
			String processor = testDataForEachMethod.get("billingProcessor").toLowerCase().equals("adyen") ? "adyen"
					: "bluesnap";

			File jsonFile = ApigeeTestBase.getFile(processor, "PayloadAddPaymentProfile.json");
			// getUserDetails() Keys => firstNameCEP, lastNameCEP, emailIdCEP, oxid
			String currency = testDataForEachMethod.get(TestingHubConstants.currencyStore);
			String city = testDataForEachMethod.get(TestingHubConstants.cityStore);
			String country = testDataForEachMethod.get(TestingHubConstants.countryStore);
			String postalCode = testDataForEachMethod.get(TestingHubConstants.postalCodeStore);
			String stateProvince = testDataForEachMethod.get(TestingHubConstants.stateProvinceStore);
			String streetAddress = testDataForEachMethod.get(TestingHubConstants.streetAddressStore);
			String email = testDataForEachMethod.get(TestingHubConstants.emailid);
			String firstName = testDataForEachMethod.get(TestingHubConstants.firstname);
			String lastName = testDataForEachMethod.get(TestingHubConstants.lastname);
			String companyName = "Holla " + lastName;
			String oxid = testDataForEachMethod.get(TestingHubConstants.oxygenid);

			try {
				if (processor.equals("adyen")) {
					PayloadAddPaymentProfile authJsonClass = om.readValue(jsonFile, PayloadAddPaymentProfile.class);
					authJsonClass.getUser().setCurrency(currency);
					authJsonClass.getUser().setEmail(email);
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setCity(city);
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setCountry(country);
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setPostalCode(postalCode);
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setStateProvince(stateProvince);
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setStreetAddress(streetAddress);
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setFirstName(firstName.trim());
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setLastName(lastName);
					authJsonClass.getUser().getPaymentProfile().getBillingInfo().setCompanyName(companyName);
					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonClass);
					Util.PrintInfo("Payload Auth:: " + inputPayload);
				} else {
					PayloadAddPaymentProfile authJsonBluesnapClass = om.readValue(jsonFile,
							PayloadAddPaymentProfile.class);
					authJsonBluesnapClass.getUser().setEmail(email);
					authJsonBluesnapClass.getUser().getPaymentProfile().getBillingInfo().setFirstName(firstName.trim());
					authJsonBluesnapClass.getUser().getPaymentProfile().getBillingInfo().setLastName(lastName);
					authJsonBluesnapClass.getUser().getPaymentProfile().getBillingInfo().setCompanyName(companyName);
					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonBluesnapClass);
					Util.PrintInfo("Payload Auth:: " + inputPayload);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				AssertUtils.fail("Error while parsing PayloadAddPaymentProfile.json file");
			}

			String baseUrl = TestingHubConstants.getCartBaseURL + TestingHubConstants.postAddPaymentProfile + oxid;
			RestAssured.baseURI = baseUrl;

			HashMap<String, String> authHeaders = new HashMap<String, String>();
			authHeaders.put("Authorization", getSpocAuthToken(testDataForEachMethod));
			authHeaders.put("Content-Type", "application/json");

			Response response = given().headers(authHeaders).body(inputPayload).when().post(baseUrl);
			String result = response.getBody().asString();
			if (response.getStatusCode() != 200) {
				Util.sleep(60000);
				response = given().headers(authHeaders).body(inputPayload).when().post(baseUrl);
				result = response.getBody().asString();
				if (response.getStatusCode() != 200) {
					Util.printInfo("result :: " + result);
					Util.printTestFailedMessage(
							"Response code must be 200 but Payment Profle API return " + response.getStatusCode());
					AssertUtils.fail(ErrorEnum.GENERIC_RETRY_MSG.geterr());
				}
			}

			JsonPath jp = new JsonPath(result);
			try {
				paymentProfileID = jp.get("paymentProfiles[0].id") + ":" + companyName;
			} catch (Exception e) {
				AssertUtils.fail("Failed to generate payment Profile ID" + e.getMessage());
			}
			Util.PrintInfo(" paymentProfileID response :: " + paymentProfileID + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return paymentProfileID;
	}

	public void quoteTaxCall(LinkedHashMap<String, String> testDataForEachMethod) {
		System.out.println("");
		String inputPayload = "";
		try {
			String processor = testDataForEachMethod.get("billingProcessor").toLowerCase().equals("adyen") ? "adyen"
					: "bluesnap";
			File jsonFile = ApigeeTestBase.getFile(processor, "PayloadPriceQuoteTax.json");
			ObjectMapper om = new ObjectMapper();

			// 5 arguments at runtime PaymentID, fn, ln, companyName, CartID
			String paymentProfileID = testDataForEachMethod.get("paymentProfileId");
			String firstName = testDataForEachMethod.get("firstname");
			String lastName = testDataForEachMethod.get("lastname");
			String companyName = testDataForEachMethod.get("companyName");
			String cartID = testDataForEachMethod.get("cartID");
			String oxid = testDataForEachMethod.get("oxygenid");

			String currency = testDataForEachMethod.get(TestingHubConstants.currencyStore);
			String city = testDataForEachMethod.get(TestingHubConstants.cityStore);
			String country = testDataForEachMethod.get(TestingHubConstants.countryStore);
			String postalCode = testDataForEachMethod.get(TestingHubConstants.postalCodeStore);
			String stateProvince = testDataForEachMethod.get(TestingHubConstants.stateProvinceStore);
			String streetAddress = testDataForEachMethod.get(TestingHubConstants.streetAddressStore);

			// 4 arguments at runtime email, fn, ln, companyName,
			try {
				if (processor.equals("adyen")) {
					PayloadPriceQuoteTax authJsonClass = om.readValue(jsonFile, PayloadPriceQuoteTax.class);
					authJsonClass.getTax().setCurrency(currency);

					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setFirstName(firstName);
					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setLastName(lastName);
					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setCompanyName(companyName);
					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setStreetAddress(streetAddress);
					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setCity(city);
					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setCountry(country);
					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setStateProvince(stateProvince);
					authJsonClass.getTax().getPaymentProfile().getBillingInfo().setPostalCode(postalCode);
					authJsonClass.getTax().getPaymentProfile().setId(paymentProfileID);
					authJsonClass.getTax().setCartReference(cartID);
					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonClass).trim();
					Util.PrintInfo("Payload Auth:: " + inputPayload);
				} else {
					PayloadPriceQuoteTax authJsonBluesnapClass = om.readValue(jsonFile, PayloadPriceQuoteTax.class);
					authJsonBluesnapClass.getTax().getPaymentProfile().getBillingInfo().setFirstName(firstName);
					authJsonBluesnapClass.getTax().getPaymentProfile().getBillingInfo().setLastName(lastName);
					authJsonBluesnapClass.getTax().getPaymentProfile().getBillingInfo().setCompanyName(companyName);
					authJsonBluesnapClass.getTax().getPaymentProfile().setId(paymentProfileID);
					authJsonBluesnapClass.getTax().setCartReference(cartID);
					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonBluesnapClass).trim();
					Util.PrintInfo("Payload Auth:: " + inputPayload);
				}
			} catch (IOException e1) {
				AssertUtils.fail("Error while parsing PayloadAddPaymentProfile.json file" + e1.getMessage());
			}

			String url = TestingHubConstants.postPriceQuoteTax + oxid;
			RestAssured.baseURI = TestingHubConstants.postPriceQuoteTax + oxid;
			HashMap<String, String> authHeaders = new HashMap<String, String>();
			authHeaders.put("Authorization", getSpocAuthToken(testDataForEachMethod));
			authHeaders.put("Content-Type", "application/json");
			Response response = given().headers(authHeaders).body(inputPayload).when().post(url);
			if ((response.getStatusCode() != 200)) {
				Util.printInfo(response.getBody().asString());
				Util.printTestFailedMessage(
						"Response code must be 200 for Tax Call but the API return " + response.getStatusCode());
				AssertUtils.fail(ErrorEnum.GENERIC_RETRY_MSG.geterr());
			}
			Util.PrintInfo(" Tax call response :: " + response.getBody().asString() + "\n");
		} catch (Exception e) {
			AssertUtils.fail(ErrorEnum.SYSTEM_ERROR.geterr());
		}
	}

	public String createBICOrder(LinkedHashMap<String, String> testDataForEachMethod) {
		String inputPayload = "";
		String purchaseOrderID = "";
		System.out.println("");
		try {
			String processor = testDataForEachMethod.get("billingProcessor").toLowerCase().equals("adyen") ? "adyen"
					: "bluesnap";
			File jsonFile = ApigeeTestBase.getFile(processor, "PayloadPurchaseCall.json");
			ObjectMapper om = new ObjectMapper();
			try {
				// 1 argument OxygenID
				String oxid = testDataForEachMethod.get("oxygenid");
				String cartID = testDataForEachMethod.get("cartID");

				if (processor.equals("adyen")) {
					PayloadPurchaseCall authJsonClass = om.readValue(jsonFile, PayloadPurchaseCall.class);
					authJsonClass.setUserExtKey(oxid);
					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonClass);
					Util.PrintInfo("Payload Auth:: " + inputPayload);
				} else {
					PayloadPurchaseCall authJsonBluesnapClass = om.readValue(jsonFile, PayloadPurchaseCall.class);
					authJsonBluesnapClass.setUserExtKey(oxid);
					inputPayload = om.writerWithDefaultPrettyPrinter().writeValueAsString(authJsonBluesnapClass);
					Util.PrintInfo("Payload Auth:: " + inputPayload);
				}

				String url = TestingHubConstants.postPurchaseCall + cartID;
				RestAssured.baseURI = TestingHubConstants.postPurchaseCall + cartID;

				HashMap<String, String> authHeaders = new HashMap<String, String>();
				authHeaders.put("Authorization", getSpocAuthToken(testDataForEachMethod));
				authHeaders.put("Content-Type", "application/json");
				Response response = given().headers(authHeaders).body(inputPayload).when().post(url);

				if (response.getStatusCode() != 200) {

					Util.printTestFailedMessage(
							"Response code must be 200 for Purchase order method but the API return "
									+ response.getStatusCode());
					AssertUtils.fail(ErrorEnum.GENERIC_RETRY_MSG.geterr());
				}
				String result = response.getBody().asString();
				JsonPath jp = new JsonPath(result);
				purchaseOrderID = jp.get("purchaseOrder.id");
			} catch (Exception e) {
				AssertUtils.fail("Failed to generate BIC Purchase Order ID " + e.getMessage());
			}
			Util.PrintInfo(" purchaseOrderID from response :: " + purchaseOrderID + "\n");
		} catch (Exception e) {
			AssertUtils.fail(ErrorEnum.SYSTEM_ERROR.geterr());
		}
		return purchaseOrderID;
	}

	@Step("BIC Add Payment Profile" + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> createCCOrder(LinkedHashMap<String, String> testDataForEachMethod) {
		String orderNumber = null;
		HashMap<String, String> results = new HashMap<String, String>();
		try {
			SSLConfig config = checkPFX();
			HashMap<String, Object> dataSet = new HashMap<>();
			dataSet.put("email", testDataForEachMethod.get(TestingHubConstants.emailid));
			dataSet.put("oxygenid", testDataForEachMethod.get(TestingHubConstants.oxygenid));

			// renewal order scenarios
			if (testDataForEachMethod.get(TestingHubConstants.testtype).equalsIgnoreCase("bicrenewal")) {
				dataSet.put("subsid", testDataForEachMethod.get("subsid"));
				dataSet.put("emailuserid", testDataForEachMethod.get("emailuserid"));

				// get subscription details
				getSubscription(testDataForEachMethod, config, dataSet);
				dataSet.put("chargeamount", testDataForEachMethod.get("chargeamount"));
				dataSet.put("renewalqty", testDataForEachMethod.get("qty"));
				orderNumber = createRenewalOrder(testDataForEachMethod, config, dataSet);
				results.put("Renewalorder", orderNumber);
				testDataForEachMethod.put("Renewalorder", orderNumber);
				return results;
			} else {
				dataSet.put("currency", testDataForEachMethod.get(TestingHubConstants.currencyStore));
				dataSet.put("city", testDataForEachMethod.get(TestingHubConstants.cityStore));
				dataSet.put("companyName", Util.getUniqueString(8));
				dataSet.put("country", testDataForEachMethod.get(TestingHubConstants.countryStore));
				dataSet.put("firstName", testDataForEachMethod.get(TestingHubConstants.firstname));
				dataSet.put("lastName", testDataForEachMethod.get(TestingHubConstants.lastname));
				dataSet.put("phoneNumber", Util.getUniqueNumber(10));
				dataSet.put("postalCode", testDataForEachMethod.get(TestingHubConstants.postalCodeStore));
				dataSet.put("stateProvince", testDataForEachMethod.get(TestingHubConstants.stateProvinceStore));
				dataSet.put("streetAddress", testDataForEachMethod.get(TestingHubConstants.streetAddressStore));
				dataSet.put("storeExternalKey", testDataForEachMethod.get("store"));
				dataSet.put("language", testDataForEachMethod.get("languageStore"));
				dataSet.put("quantity", testDataForEachMethod.get("quantity"));

				if (testDataForEachMethod.get("payment").equalsIgnoreCase("paypal")) {
					dataSet.put("paymentProcessor", "PAYPAL-NAMER");
					dataSet.put("type", "PAYPAL");
					dataSet.put("formattedPaymentMethod", "PayPal");
					dataSet.put("isPaypalDirect", "true");
					dataSet.put("paymentMethod", "DIGITAL_GOODS");
					dataSet.put("paymentReference", "B-3D048426R49338451");
					dataSet.put("expYear", "2024");
					dataSet.put("expMonth", "02");
					dataSet.put("userid", "217");

				}

				else if (testDataForEachMethod.get("payment").equalsIgnoreCase("visa")) {
					dataSet.put("paymentProcessor", "ADYEN");
					dataSet.put("type", "CREDIT_CARD");
					dataSet.put("formattedPaymentMethod", "Visa");
					dataSet.put("isPaypalDirect", "no");
					dataSet.put("paymentMethod", "VISA");
					dataSet.put("paymentReference", "1111");
					dataSet.put("expYear", "2022");
					dataSet.put("expMonth", "01");
					dataSet.put("userid", "3");

				}
			}

			orderNumber = createOrder(testDataForEachMethod, config, dataSet);
			results.put("Intialorder", orderNumber);
			if (testDataForEachMethod.get("payment").equalsIgnoreCase("paypal")
					|| testDataForEachMethod.get("payment").equalsIgnoreCase("visa")) {
				testDataForEachMethod.put("poordernumber", orderNumber);
				// Get the parent ID value to send to the next call
				HashMap<String, String> idvalues = getParentID(testDataForEachMethod, config, dataSet);

				if (idvalues == null || idvalues.isEmpty()) {
					AssertUtils.fail("Unable to Get the parent txn ID for the PO order");
				}

				String parentid = idvalues.get("parenttokenID");
				String subsid = idvalues.get("subscriptionid");

				results.put("ParenttxnID", parentid);
				results.put("SubscritpionID", subsid);

				dataSet.put("poordernumber", orderNumber);
				dataSet.put("parentid", parentid);
				dataSet.put("subsid", subsid);
				updateStatusForPaypal(testDataForEachMethod, config, dataSet);

			}
			// Place the add product order
			if (testDataForEachMethod.get(TestingHubConstants.testtype).equalsIgnoreCase("bicaddseat")) {
				String addseatprice = getAddSeatPrice(testDataForEachMethod, config, dataSet);
				dataSet.put("addseatprice", addseatprice);
				String addseatnumber = createAddSeatOrder(testDataForEachMethod, config, dataSet);
				updateStatusForPaypal(testDataForEachMethod, config, dataSet);
				results.put("Addseatorder", addseatnumber);
			}

		} catch (Exception e) {
			e.printStackTrace();
			AssertUtils.fail("Unable to generate WorkID for Student");
		}
		return results;
	}

	private Double getDiscountPercentage(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
										 HashMap<String, Object> dataSet) {
		String passCode = null;
		Float discount = null;
		double totaldiscount = 0.0;

		Map<String, Object> discountvalues = new HashMap<String, Object>();
		try {
			if (System.getProperty("promocode") == null) {
				testDataForEachMethod.put("promoid", System.getProperty("promoid"));
				passCode = "filter[ids]=" + System.getProperty("promoid");
			} else {
				testDataForEachMethod.put("promocode", System.getProperty("promocode"));
				passCode = "filter[codes]=" + System.getProperty("promocode");
			}

			String baseUrl = testDataForEachMethod.get("getdiscount");
			baseUrl = addTokenInResourceUrl(baseUrl, passCode);
			System.out.println("get discountpercentage baseURL : " + baseUrl);

			Map<String, String> header = getHeaders(testDataForEachMethod);

			String result = getDetails(config, baseUrl, header);
			Util.printInfo(result);
			System.out.println("result :: " + result);
			Util.printInfo(result);

			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js);
			ArrayList<String> error = js.get("errors");
			if (error == null) {
				ArrayList<String> value = js.get("data");
				for (int i = 0; i < value.size(); i++) {
					discountvalues = js.get("data[" + i + "]");
					discount = (Float) discountvalues.get("discountPercent");
					totaldiscount = totaldiscount + discount;
				}
			} else {
				AssertUtils.fail("Provided promotion id / code got is not valid");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return totaldiscount;

	}

	private HashMap<String, String> getParentID(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
			HashMap<String, Object> dataSet) {
		String parentid = null;
		String subscriptionid = null;
		HashMap<String, String> results = new HashMap<String, String>();
		try {

			String baseUrl = testDataForEachMethod.get("getparentId");
			String resourceUrl = addTokenInResourceUrl(baseUrl, testDataForEachMethod.get("poordernumber"));
			System.out.println("getparentdetails baseURL : " + baseUrl);

			Map<String, String> header = getHeaders(testDataForEachMethod);

			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = getRestResponseWithResource(baseUrl, resourceUrl, header);
			System.out.println("response :: " + response.asString());
			String result = response.getBody().asString();
			System.out.println("result :: " + result);
			org.jsoup.nodes.Document doc = Jsoup.parse(result, "", Parser.xmlParser());
			if (testDataForEachMethod.get("payment").equalsIgnoreCase("visa")) {

				String properties = doc.select("property").attr("name");
				if (properties.equalsIgnoreCase("pspReference")) {
					parentid = doc.select("property").attr("value");
				}
			} else {
				String properties = doc.select("property").attr("name");
				if (properties.equalsIgnoreCase("paypalTransactionId")) {
					parentid = doc.select("property").attr("value");
				}
			}

			// get the subscritpion id for the data
			subscriptionid = doc.select("offeringResponse").attr("subscriptionId");

			results.put("parenttokenID", parentid);
			results.put("subscriptionid", subscriptionid);

			testDataForEachMethod.put("parenttokenID", parentid);
			testDataForEachMethod.put("subscriptionid", subscriptionid);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;

	}

	private String updateStatusForPaypal(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
										 HashMap<String, Object> dataSet) {
		String result = null;
		try {
			String jsonFile = ApigeeTestBase.getFilePath("jsonFiles", "UpdateStatus_Paypal.json");
			String payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
			String baseUrl = testDataForEachMethod.get("updatestatuspaypal");
			RestAssured.baseURI = baseUrl;
			System.err.println("baseUrl -" + baseUrl);
			System.err.println("add payment payload -" + payload);
			HashMap<String, String> authHeaders = new HashMap<String, String>();
			authHeaders.put("Content-Type", "application/json");
			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = given().headers(authHeaders).body(payload).when().post(baseUrl);
			result = response.getBody().asString();
			Util.printInfo("result from the status update -" + result);
			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js);

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (result == null) {
			AssertUtils.fail("Unable to update the payment profile for the paypal order");
		}
		return result;
	}

	@SuppressWarnings("unused")
	private String getAddSeatPrice(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
								   HashMap<String, Object> dataSet) {
		String addseatprice = null;
		String error = null;
		String payload = null;
		String filename = null;
		String result = null;
		String baseUrl = null;
		filename = "getDiscountCost.json";

		try {

			String jsonFile = ApigeeTestBase.getFilePath("jsonFiles", filename);
			payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
			baseUrl = testDataForEachMethod.get("getPriceQuote");

			Map<String, String> header = getHeaders(testDataForEachMethod);
			RestAssured.baseURI = baseUrl;
			System.err.println("baseUrl -" + baseUrl);
			System.err.println("add payment payload -" + payload);

			header.put("Content-Type", "application/vnd.api+json");
			header.put("Accept", "application/vnd.api+json");
			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = given().headers(header).body(payload).when().post(baseUrl);
			int statuscode = given().headers(header).body(payload).when().post(baseUrl).getStatusCode();
			result = response.getBody().asString();
			Util.printInfo("result from the status update -" + result);
			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js.toString());
			addseatprice = js.get("data.lineItems[0].totals.subtotalAfterPromotions");
			testDataForEachMethod.put("addseatprice", addseatprice);
			if (addseatprice == null) {
				error = js.get("error.message");
				testDataForEachMethod.put("errormessage", error);
				AssertUtils.fail("Unable to get the discounted price for add seat -" + error);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return addseatprice;
	}

	private String createAddSeatOrder(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
									  HashMap<String, Object> dataSet) {
		String addseatorder = null;
		String error = null;
		String payload = null;
		String filename = null;
		String baseUrl = null;

		filename = "addseat.json";
		baseUrl = testDataForEachMethod.get("purchasePayport");
		String value = testDataForEachMethod.get(TestingHubConstants.oxygenid);
		baseUrl = addTokenInResourceUrl(baseUrl, value);

		try {
			String jsonFile = ApigeeTestBase.getFilePath("jsonFiles", filename);
			payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
			Map<String, String> header = getHeaders(testDataForEachMethod);
			header.put("Content-Type", "application/json");
			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = given().headers(header).body(payload).when().post(baseUrl);
			System.err.println("baseUrl -" + baseUrl);
			System.err.println("add payment payload -" + payload);
			String result = response.getBody().asString();
			Util.printInfo(result);
			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js);
			addseatorder = js.get("purchaseOrder.id");
			testDataForEachMethod.put("addseatorder", addseatorder);

			System.err.println("baseUrl -" + baseUrl);
			System.err.println("paylaod -" + payload);
			if (addseatorder == null) {
				error = js.get("error.message");
				testDataForEachMethod.put("errormessage", error);
				AssertUtils.fail("Unable to  place an add seat Order error -" + error);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return addseatorder;
	}

	private String createRenewalOrder(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
									  HashMap<String, Object> dataSet) {
		String renewalorder = null;
		String error = null;
		String payload = null;
		String filename = null;
		String baseUrl = null;

		filename = "renewal.json";
		baseUrl = testDataForEachMethod.get("purchaseRenewal");
		String value = testDataForEachMethod.get(TestingHubConstants.oxygenid);
		baseUrl = addTokenInResourceUrl(baseUrl, value);

		try {
			String jsonFile = ApigeeTestBase.getFilePath("jsonFiles", filename);
			payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
			Map<String, String> header = getHeaders(testDataForEachMethod);
			header.put("Content-Type", "application/json");
			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = given().headers(header).body(payload).when().post(baseUrl);
			System.err.println("baseUrl -" + baseUrl);
			System.err.println("add payment payload -" + payload);
			String result = response.getBody().asString();
			Util.printInfo(result);
			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js);
			renewalorder = js.get("purchaseOrder.id");
			testDataForEachMethod.put("renewalorder", renewalorder);

			System.err.println("baseUrl -" + baseUrl);
			System.err.println("paylaod -" + payload);
			if (renewalorder == null) {
				error = js.get("error.message");
				testDataForEachMethod.put("errormessage", error);
				AssertUtils.fail("Unable to purchaseCC renewal and place an Order error -" + error);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return renewalorder;
	}

	@SuppressWarnings("unused")
	private String getSubscription(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
								   HashMap<String, Object> dataSet) {
		String chargeamount = null;
		String error = null;
		String filename = null;
		String baseUrl = null;
		String qty = null;

		baseUrl = testDataForEachMethod.get("getsubscriptiondata");

		try {

			String resourceUrl = addTokenInResourceUrl(baseUrl, testDataForEachMethod.get("subsid"));
			Map<String, String> header = getHeaders(testDataForEachMethod);
			header.put("Content-Type", "application/json");
			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = given().headers(header).when().get(resourceUrl);
			System.err.println("baseUrl -" + resourceUrl);
			String result = response.getBody().asString();
			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js.toString());
			chargeamount = js.get("data.nextBillingInfo.chargeAmount");
			qty = Integer.toString(js.get("data.quantity"));
			testDataForEachMethod.put("chargeamount", chargeamount);
			testDataForEachMethod.put("qty", qty);

			System.err.println("baseUrl -" + resourceUrl);
			if (chargeamount == null) {
				error = js.get("error.message");
				testDataForEachMethod.put("errormessage", error);
				AssertUtils.fail("Unable to  place an add seat Order error -" + error);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chargeamount;
	}

	@SuppressWarnings("unchecked")
	private String createOrder(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
							   HashMap<String, Object> dataSet) {
		String orderNumber = null;
		JSONParser parser = new JSONParser();
		String error = null;
		String payloadValues = null;
		try {
			String payment = testDataForEachMethod.get("payment");
			String store = testDataForEachMethod.get("store");
			String value = testDataForEachMethod.get(TestingHubConstants.oxygenid);
			String filename = "purchaseCC_BIC.json";
			if (payment.equalsIgnoreCase("paypal")) {
				value = "GZ52RNS7XNWA";
			}

			if (store.equalsIgnoreCase("STORE-UK")) {
				value = testDataForEachMethod.get(TestingHubConstants.oxygenid);
				dataSet.put("currency", testDataForEachMethod.get(TestingHubConstants.currencyStore));
			}

			if (payment.equalsIgnoreCase("promo")) {
				filename = "purchaseCC_Promo_BIC.json";
			}
			String jsonFile = ApigeeTestBase.getFilePath("jsonFiles", filename);

			String pricevalue = testDataForEachMethod.get("priceId");
			String[] priceidarr = null;
			priceidarr = pricevalue.split(";");
			String payload = null;
			if (!pricevalue.contains(";")) {
				testDataForEachMethod.put("priceId", priceidarr[0]);
				HashMap<String, String> priceDetails = getPriceDetails(testDataForEachMethod);
				testDataForEachMethod.putAll(priceDetails);

			} else {

				String qtyvalue = testDataForEachMethod.get("quantity");
				String[] qtyarr = null;
				qtyarr = qtyvalue.split(";");
				// if the sku has more values
				File json_File = new File(jsonFile);
				Object obj = parser.parse(new FileReader(json_File));
				JSONObject jsonObject = (JSONObject) obj;
				JSONObject purchase = (JSONObject) jsonObject.get("purchase");
				JSONArray offers = (JSONArray) purchase.get("offers");
				JSONArray mutipleoffer = new JSONArray();
				JSONObject intialoffer = (JSONObject) offers.get(0);
				String strintialoffer = intialoffer.toJSONString();

				for (int i = 0; i < priceidarr.length; i++) {
					JSONObject tempoffer = (JSONObject) parser.parse(strintialoffer);
					testDataForEachMethod.put("priceId", priceidarr[i]);
					testDataForEachMethod.put("quantity", qtyarr[i]);
					HashMap<String, String> priceDetails = getPriceDetails(testDataForEachMethod);
					testDataForEachMethod.putAll(priceDetails);
					// updataing the values
					tempoffer.put("id", testDataForEachMethod.get("priceId"));
					tempoffer.put("quantity", testDataForEachMethod.get("quantity"));
					tempoffer.put("unitPriceAfterDiscount", priceDetails.get("response_amount"));
					mutipleoffer.add(tempoffer);
				}
				purchase.put("offers", mutipleoffer);
//		        JSONObject newjson=(JSONObject) purcahse.put("offers", mutipleoffer);
				payloadValues = jsonObject.toJSONString();
				System.err.println("payloaddetails -" + payload);

			}

			// Updating the currency from the get price id details
			dataSet.put("currency", testDataForEachMethod.get("response_currency"));
			dataSet.put("id", testDataForEachMethod.get("response_priceID"));
			dataSet.put("unitPrice", testDataForEachMethod.get("response_amount"));
			if (!payment.equalsIgnoreCase("paypal")) {
				String paymentid = addPaymentProfile(testDataForEachMethod, config, dataSet);
				if (payment.equalsIgnoreCase("visa")
						|| payment.equalsIgnoreCase("promo")) {
					dataSet.put("userid", paymentid);
				}
			}
			if (testDataForEachMethod.get("payment").equalsIgnoreCase("promo")) {

				Double discount = getDiscountPercentage(testDataForEachMethod, config, dataSet);
				if (discount == null) {
					AssertUtils.fail("Provided promotion ID or promotion code is not valid");
				}
				double Price = Double.parseDouble(testDataForEachMethod.get("response_amount"));
				double pricediff = Price * discount;
				double discountedPrice = Price - pricediff;
				String strdiscountedPrice = Double.toString(discountedPrice);
				System.err.println("discountedPrice -" + strdiscountedPrice);
				dataSet.put("discountedPrice", strdiscountedPrice);

			}

			if (!pricevalue.contains(";")) {
				payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
			} else {
				payload = LoadJsonWithValue.loadJsonString(dataSet, payloadValues).toString();
			}

			String baseUrl = testDataForEachMethod.get("purchasePayport");

			if (testDataForEachMethod.get("payment").equalsIgnoreCase("paypal")) {
				baseUrl = testDataForEachMethod.get("purchasePayportPaypal");
			}

			baseUrl = addTokenInResourceUrl(baseUrl, value);
			RestAssured.baseURI = baseUrl;
			System.err.println("baseUrl -" + baseUrl);

			System.err.println("paylaod -" + payload);
			HashMap<String, String> authHeaders = new HashMap<String, String>();
			authHeaders.put("Content-Type", "application/json");
			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = given().headers(authHeaders).body(payload).when().post(baseUrl);
			String result = response.getBody().asString();
			Util.printInfo(result);
			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js);
			orderNumber = js.get("purchaseOrder.id");
			testDataForEachMethod.put("purchaseOrderID", orderNumber);
			if (orderNumber == null) {
				error = js.get("error.message");
				testDataForEachMethod.put("errormessage", error);
				AssertUtils.fail("Unable to purchaseCC and place an Order error -" + error);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return orderNumber;
	}

	private String addPaymentProfile(LinkedHashMap<String, String> testDataForEachMethod, SSLConfig config,
			HashMap<String, Object> dataSet) {
		String token = null;
		try {
			String jsonFile = ApigeeTestBase.getFilePath("jsonFiles", "AddPaymentProfile.json");
			String payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();
			String baseUrl = testDataForEachMethod.get("payportAddPayment");
			baseUrl = addTokenInResourceUrl(baseUrl, testDataForEachMethod.get("oxygenid"));
			RestAssured.baseURI = baseUrl;
			System.err.println("baseUrl -" + baseUrl);
			System.err.println("add payment payload -" + payload);
			HashMap<String, String> authHeaders = new HashMap<String, String>();
			authHeaders.put("Content-Type", "application/json");
			RestAssured.config = RestAssured.config().sslConfig(config);
			Response response = given().headers(authHeaders).body(payload).when().post(baseUrl);
			String result = response.getBody().asString();
			Util.printInfo(result);
			JsonPath js = new JsonPath(result);
			Util.printInfo("js is:" + js);
			token = js.get("paymentProfile.id");
			testDataForEachMethod.put("paymentprofileid", token);

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (token == null) {
			AssertUtils.fail("Unable to add payment profile");
		}
		return token;
	}



	@Step("Subscription : subs Validation" + GlobalConstants.TAG_TESTINGHUB)
	public HashMap<String, String> getPurchaseOrderDetails(String purchaseOrderAPIresponse) {
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

			String subscriptionId = doc.getElementsByTagName("offeringResponse").item(0).getAttributes()
					.getNamedItem("subscriptionId").getTextContent();
			System.out.println("subscriptionId :" + subscriptionId);

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

	@SuppressWarnings("deprecation")
	public SSLConfig checkPFX() throws KeyStoreException {
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		char[] testssap = "BuyACAD&25".toCharArray();
		SSLConfig config = null;
		try {
//		      keystore.load(new FileInputStream("C:\\Users\\kotianp\\Documents\\payport-agent2payport-stg.autodesk.com.pfx"),testssap);
			keystore.load(new FileInputStream("payport-agent2payport-stg.autodesk.com.pfx"), testssap);
			if (keystore != null) {
				org.apache.http.conn.ssl.SSLSocketFactory clientAuthFactory = null;
				try {
					clientAuthFactory = new org.apache.http.conn.ssl.SSLSocketFactory(keystore, "BuyACAD&25");
				} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException
						| KeyStoreException e) {
					e.printStackTrace();
				}
				config = new SSLConfig().with().sslSocketFactory(clientAuthFactory).and().allowAllHostnames();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return config;
	}

	public String addTokenInResourceUrl(String resourceUrl, String tokenString) {
		return resourceUrl.replace("passtoken", tokenString);
	}

	private String getDetails(SSLConfig config, String baseUrl, Map<String, String> header) {
		String result = null;
		try {
			Response response = null;
			RestAssured.config = RestAssured.config().sslConfig(config);
			RestAssured.baseURI = baseUrl;
			response = given().headers(header).when().get(baseUrl);
			result = response.getBody().asString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}


	public static Response getRestResponseWithResource(String baseUrl, String resourceUrl, Map<String, String> header) {
		Response response = null;
		try {
			Util.printInfo("Hitting the URL = " + baseUrl + resourceUrl);
			RestAssured.baseURI = baseUrl;
			response = given().headers(header).when().get(resourceUrl);
			String result = response.getBody().asString();
			Util.printInfo(result);
			if (response.getStatusCode() != 200) {
				Assert.assertTrue(false, "Response code must be 200 but the API return " + response.getStatusCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;

	}

	public HashMap<String, String> subscriptionsPelicanAPIHeaders(HashMap<String, String> data){
		HashMap<String, String> authHeaders = new HashMap<String, String>();
		System.out.println("");
		String sig_details = getPriceByPriceIdSignature(data);
		String hmacSignature = sig_details.split("::")[0];
		String X_E2_HMAC_Timestamp = sig_details.split("::")[1];
		String X_E2_PartnerId = data.get("getPriceDetails_X_E2_PartnerId");
		String X_E2_AppFamilyId = data.get("getPriceDetails_X_E2_AppFamilyId");

		authHeaders.put("X-E2-PartnerId", X_E2_PartnerId);
		authHeaders.put("X-E2-AppFamilyId", X_E2_AppFamilyId);
		authHeaders.put("X-E2-HMAC-Timestamp", X_E2_HMAC_Timestamp);
		authHeaders.put("X-E2-HMAC-Signature", hmacSignature);
		authHeaders.put("X-Request-Ref", UUID.randomUUID().toString());
		authHeaders.put("aaccept", "application/vnd.api+json");
		authHeaders.put("Content-Type", "application/json");

		return authHeaders;

	}

	public static Response postMethod(String baseUrl, String inputPayload, Map<String, String> authHeaders) {
		RestAssured.baseURI = baseUrl;
		return given().headers(authHeaders).body(inputPayload).when().post();
	}

}
