package com.autodesk.eceapp.testbase.ece;

import static io.restassured.RestAssured.given;

import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.platformautomation.bilinsmAccessNEWT.ApiClient;
import com.autodesk.platformautomation.bilinsmAccessNEWT.ApiException;
import com.autodesk.platformautomation.bilinsmAccessNEWT.Configuration;
import com.autodesk.platformautomation.bilinsmAccessNEWT.client.EntitlementsApi;
import com.autodesk.platformautomation.bilinsmAccessNEWT.client.models.Entitlement;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import java.util.List;
import java.util.Map;

/**
 * A client to interact with the newt entitlements API
 */
public class NEWTAccessTestBase {
  private final String clientId;
  private final String clientSecret;

  private final String basePathPrefix;

  public NEWTAccessTestBase() {
    Map<String, String> newtCredentials = (Map<String, String>) ResourceFileLoader.getNewtYaml();

    clientId = ProtectedConfigFile.decrypt(newtCredentials.get("clientId"));
    clientSecret = ProtectedConfigFile.decrypt(newtCredentials.get("clientSecret"));

    switch (GlobalConstants.ENV.toUpperCase()) {
      case "STG":
        basePathPrefix = "stg";
        break;
      case "INT":
        basePathPrefix = "uat-stg";
        break;
      default:
        basePathPrefix = "";
    }
  }

  /**
   * Get the entitlement for a specific user by their oxygen ID
   * @param oxygenId - User's oxygen ID
   */
  public void getEntitlementsForUser(String oxygenId) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api-west.newt." + basePathPrefix + ".autodesk.com");
    EntitlementsApi apiInstance = new EntitlementsApi(defaultClient);

    String access_token = given()
        .contentType("application/x-www-form-urlencoded")
        .body(
            "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret)
        .post("https://auth-west.newt." + basePathPrefix + ".autodesk.com/oauth2/token")
        .then().extract().response()
        .jsonPath().getString("access_token");

    try {
      List<Entitlement> entitlements = apiInstance.entitlementsV1Get("purchaser", oxygenId, "Bearer " + access_token);
      Util.printInfo("Entitlements: ");
      Util.printInfo(entitlements.toString());
    } catch (ApiException e) {
      Util.printError("Exception when calling EntitlementsApi#entitlementsV1Get");
      Util.printError("Status code: " + e.getCode());
      Util.printError("Reason: " + e.getResponseBody());
      Util.printError("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
