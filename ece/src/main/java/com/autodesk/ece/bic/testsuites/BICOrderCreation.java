package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BICOrderCreation extends ECETestBase {

	Map<?, ?> loadYaml = null;
	Map<?, ?> loadRestYaml = null;
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

}
