package com.autodesk.ece.testbase;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.utilities.PelicanRequestSigner;
import com.autodesk.eceapp.utilities.PelicanRequestSigner.PelicanSignature;
import com.autodesk.platformautomation.ApiClient;
import com.autodesk.platformautomation.ApiException;
import com.autodesk.platformautomation.Configuration;
import com.autodesk.platformautomation.bmse2pelicansubscriptionv4.SubscriptionControllerApi;
import com.autodesk.platformautomation.bmse2pelicansubscriptionv4.models.SubscriptionSuccessV4;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import java.util.HashMap;

public class SubscriptionServiceV4TestBase {

  private final PelicanRequestSigner requestSigner = new PelicanRequestSigner();

  @Step("Subscription Service : Get Subscription API" + GlobalConstants.TAG_TESTINGHUB)
  public HashMap<String, String> getSubscriptionById(HashMap<String, String> data) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(data.get("getPelicanBaseUrl"));
    SubscriptionControllerApi apiInstance = new SubscriptionControllerApi(defaultClient);

    PelicanSignature signature = requestSigner.generateSignature();

    String id = data.get(BICECEConstants.GET_POREPONSE_SUBSCRIPTION_ID);
    HashMap<String, String> results = new HashMap<>();

    boolean success = false;
    int attempt = 0;

    while (!success) {
      Util.printInfo("Attempt: " + attempt);
      Util.sleep(3000);
      attempt++;
      if (attempt > 3) {
        AssertUtils.fail("Unable to get a successful response for SubscriptionControllerApi#findSubscriptionById.");
        break;
      }
      try {
        SubscriptionSuccessV4 result = apiInstance.findSubscriptionById(signature.xE2PartnerId,
            signature.xE2AppFamilyId, signature.xE2HMACTimestamp, signature.xE2HMACSignature, id,
            signature.xRequestRef);
        Util.PrintInfo(BICECEConstants.RESULT + result);
        results.put("response_nextBillingDate", result.getNextBillingDate());
        results.put("response_subscriptionQuantity",
            String.valueOf(result.getQuantity()));
        results.put("response_quantityToReduce",
            String.valueOf(result.getQuantityToReduce()));
        results.put("response_offeringExternalKey", result.getOfferingExternalKey());
        results.put("response_endDate", result.getEndDate());
        results.put("response_autoRenewEnabled",
            Boolean.toString(result.getAutoRenewEnabled()));
        results.put("response_expirationDate", result.getExpirationDate());
        results.put("response_currentBillingPriceId",
            String.valueOf(result.getPriceId() != null ? result.getPriceId() : null));
        results.put("response_switchTermPriceId",
            result.getSwitchTermPriceId() != null ? String.valueOf(
                result.getSwitchTermPriceId()) : null);
        results.put("response_status", String.valueOf(result.getStatus()));
        results.put("response_subscriptionCreated", String.valueOf(result.getCreated()));

        success = true;
      } catch (ApiException e) {
        Util.printError("Exception when calling SubscriptionControllerApi#findSubscriptionById");
        Util.printError("Status code: " + e.getCode());
        Util.printError("Reason: " + e.getResponseBody());
        Util.printError("Response headers: " + e.getResponseHeaders());
        e.printStackTrace();
      }
    }

    return results;
  }
}
