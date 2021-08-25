package com.autodesk.ece.testbase;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.EISConstants;
import com.autodesk.testinghub.core.common.services.ApigeeAuthenticationService;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.httpclient.HttpApacheClient;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ErrorEnum;
import com.autodesk.testinghub.core.utils.LoadJsonWithValue;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class ApigeeTestBase {

  public static Response getRestResponseWithoutAssert(String baseUrl, Map<String, String> header) {
    Response response = null;
    try {
      Util.printInfo("Hitting the URL = " + baseUrl);
      RestAssured.baseURI = baseUrl;
      response = given().headers(header).when().get();
      String result = response.getBody().asString();
      Util.printInfo(result);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }

  public static File getFile(String processor, String fileName) {
    return new File(getFilePath(processor, fileName));
  }

  public static String getFilePath(String processor, String fileName) {
    String path = Util.getCoreLocalRepoLibPath() + GlobalConstants.ENV.toUpperCase()
        + EISConstants.fileseparator + processor + EISConstants.fileseparator + fileName;
    return path;
  }

  public String getBaseAuth(String consumerKey, String consumerSecret) {
    String str = consumerKey + ":" + consumerSecret;
    byte[] bytesEncoded = Base64.encodeBase64(str.getBytes());
    return new String(bytesEncoded);
  }

  public Response getRestResponse(String baseUrl) {

    Response response = null;
    try {
      urlCheck(baseUrl);
      Util.printInfo("Hitting the URL = " + baseUrl);
      Util.printInfo("Hitting the URL = " + baseUrl);
      response = given().when().get(baseUrl);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }

  private void urlCheck(String baseUrl) throws MalformedURLException {
    URL url = null;
    try {
      url = new URL(baseUrl);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }
    try {
      System.out.println("getAuthority : " + url.getAuthority());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getContent : " + url.getContent());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getDefaultPort : " + url.getDefaultPort());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getFile : " + url.getFile());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getHost : " + url.getHost());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getPath : " + url.getPath());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getPort : " + url.getPort());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getProtocol : " + url.getProtocol());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getQuery : " + url.getQuery());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getRef : " + url.getRef());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getUserInfo : " + url.getUserInfo());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("getClass : " + url.getClass());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("openConnection : " + url.openConnection());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("openStream : " + url.openStream());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("toExternalForm : " + url.toExternalForm());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("toString : " + url.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      System.out.println("toURI : " + url.toURI());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String postRestResponse(String baseUrl, String resourceUrl, HashMap<String, String> header,
      JSONObject body) {
    Util.printInfo("Hitting the URL = " + baseUrl + resourceUrl);
    String getResponsebody = null;
    HttpApacheClient httpclient = new HttpApacheClient();
    HttpResponse response;
    try {
      response = httpclient.post(header, baseUrl + resourceUrl, body.toJSONString());
      int responseStatusCode = response.getStatusLine().getStatusCode();
      System.out.println("Response code : " + responseStatusCode);
        if (responseStatusCode != 200) {
            AssertUtils.assertTrue(false,
                "Response code must be 200 but the API return " + responseStatusCode);
        }

      HttpEntity entity = response.getEntity();
      getResponsebody = EntityUtils.toString(entity);
      System.out.println("getResponsebody : " + getResponsebody);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return getResponsebody;
  }

  public HashMap<String, String> getAuthToken(HashMap<String, String> data) {
    String consumerKey = data.get(TestingHubConstants.consumerKey);
    String consumerSecret = data.get(TestingHubConstants.customerSecret);
    String callBackURL = data.get(TestingHubConstants.callbackURL);
    String baseUrl = data.get(TestingHubConstants.baseUrl);
    String authResourceUrl = data.get(TestingHubConstants.authResourceUrl);

    String signature = null;
    HashMap<String, String> tokenDetails = new HashMap<>();

    try {
      String accessTokenURL = "https://enterprise-api-stg.autodesk.com/v2/oauth/generateaccesstoken?grant_type=client_credentials";
      ApigeeAuthenticationService apigeeAuthenticationService = new ApigeeAuthenticationService();
      String timeStamp = apigeeAuthenticationService.getTimeStamp();
      signature = apigeeAuthenticationService
          .getSignature(consumerKey, consumerSecret, callBackURL, timeStamp);
      String token = apigeeAuthenticationService
          .getAccessToken(accessTokenURL, consumerKey, consumerSecret,
              callBackURL, timeStamp, signature);
      signature = apigeeAuthenticationService
          .getSignature(token, consumerSecret, callBackURL, timeStamp);
      tokenDetails.put("timeStamp", timeStamp);
      tokenDetails.put("token", token);
      tokenDetails.put("signature", signature);
    } catch (Exception e) {
      Util.printInfo("Unable to autheticate the Apigee, Exception -" + e.getMessage());
      AssertUtils.fail("Unable to autheticate the Apigee, Exception -" + e.getMessage());
    }
    return tokenDetails;
  }

  public JSONObject readJSON(String fileName) {
    JSONObject jsonObject = null;
    try {
      JSONParser jsonParser = new JSONParser();
      FileReader reader = new FileReader(Util.getPayloadDir() + fileName);
      Object obj = jsonParser.parse(reader);
      jsonObject = (JSONObject) obj;
      System.out.println("JSON File Content --> " + jsonObject);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return jsonObject;
  }

  @SuppressWarnings("unchecked")
  @Step("Assert Merge operation using API " + GlobalConstants.TAG_TESTINGHUB)
  public String postAssetMergeTransaction(HashMap<String, String> data) {
    String getResponsebody = null;
    try {
      String baseURL = data.get(TestingHubConstants.baseUrl);
      String resourceUrl = data.get(TestingHubConstants.apigeeAssetMerge);
      HashMap<String, String> authToken = getAuthToken(data);
      HashMap<String, String> header = new HashMap<>();
      header.put("Authorization", "Bearer " + authToken.get("token"));
      header.put("signature", authToken.get("signature"));
      header.put("timestamp", authToken.get("timeStamp"));
      header.put("csn", data.get(TestingHubConstants.csn));
      header.put("Content-Type", "application/json");

      String targetAsset = data.get(TestingHubConstants.targetAssetNumber);
      Util.PrintInfo("Target Asset Number ::" + targetAsset);

      String sourceAsset = data.get(TestingHubConstants.sourceAssetNumbers);
      Util.PrintInfo("Source Asset Number ::" + sourceAsset);

      String targetContract = data.get(TestingHubConstants.targetContractNumber);
      Util.PrintInfo("Target Contract Number ::" + targetContract);

      JSONObject apigeeBody = readJSON("AssetMerge.json");
      apigeeBody.putIfAbsent(TestingHubConstants.targetAssetNumber, targetAsset);
      apigeeBody.putIfAbsent(TestingHubConstants.targetContractNumber, targetContract);

      JSONArray jarray = new JSONArray();
      jarray.add(sourceAsset);
      apigeeBody.putIfAbsent("assetNumbers", jarray);
      getResponsebody = postRestResponse(baseURL, resourceUrl, header, apigeeBody);
      Util.PrintInfo("Response is :: " + getResponsebody);
        if (getResponsebody != null) {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(getResponsebody);
            String txStatus = json.get("transactionStatus").toString();
            Util.PrintInfo("Status is : " + txStatus);
        } else {
            AssertUtils.fail("Response is null after performing Merge operation");
        }
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo(ErrorEnum.ASSET_MERGE_APIGEE.geterr());
    }
    return getResponsebody;
  }

  // #RAS
  @SuppressWarnings("unchecked")
  @Step("Asset Split operation using API" + GlobalConstants.TAG_TESTINGHUB)
  public String postAssetSplitTransaction(HashMap<String, String> data) {
    String getResponsebody = null;
    try {
      String baseURL = data.get(TestingHubConstants.baseUrl);
      String resourceUrl = data.get(TestingHubConstants.apigeeAssetSplit);
      HashMap<String, String> authToken = getAuthToken(data);
      HashMap<String, String> header = new HashMap<>();
      header.put("Authorization", "Bearer " + authToken.get("token"));
      header.put("signature", authToken.get("signature"));
      header.put("timestamp", authToken.get("timeStamp"));
      header.put("csn", data.get(TestingHubConstants.csn));
      header.put("Content-Type", "application/json");

      String assetNumber = data.get(TestingHubConstants.assetNumber);
      Util.PrintInfo(" Asset Number ::" + assetNumber);

      String targetContract = data.get(TestingHubConstants.contractNumber);
      Util.PrintInfo("Contract Number ::" + targetContract);

      String assetSplitQty = data.get(TestingHubConstants.assetSplitQty);
      Util.PrintInfo("Asset Qty For Split  Number ::" + assetSplitQty);

      JSONArray assetSplitQtyFinal = new JSONArray();
      assetSplitQtyFinal.add(assetSplitQty);
      assetSplitQtyFinal.add("1");
      Util.PrintInfo("assetSplitQtyFinal ::" + assetSplitQtyFinal);

      // test
      JSONObject apigeeBody = readJSON("AssetSplit.json");
      apigeeBody.putIfAbsent(TestingHubConstants.assetNumber, assetNumber);
      apigeeBody.putIfAbsent(TestingHubConstants.contractNumber, targetContract);
      apigeeBody.putIfAbsent(TestingHubConstants.assetSplitQty, assetSplitQtyFinal);

      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(apigeeBody.toString());
      Util.printInfo(json.toJSONString());
      getResponsebody = postRestResponse(baseURL, resourceUrl, header, json);

        if (getResponsebody != null) {
            json = (JSONObject) parser.parse(getResponsebody);
            String txStatus = json.get("transactionStatus").toString();
            Util.PrintInfo("Status is : " + txStatus);
        } else {
            AssertUtils.fail("Response is null after performing Split opration");
        }
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo(ErrorEnum.ASSET_SPLIT_APIGEE.geterr());
    }
    return getResponsebody;
  }

  // #RAS
  @SuppressWarnings("unchecked")
  @Step("Contract Merge operation using API Validation" + GlobalConstants.TAG_TESTINGHUB)
  public String postContractMergeTransaction(HashMap<String, String> data) {
    String getResponsebody = null;
    try {
      String baseURL = data.get(TestingHubConstants.baseUrl);
      String resourceUrl = data.get(TestingHubConstants.apigeeContractMerge);
      HashMap<String, String> authToken = getAuthToken(data);

      HashMap<String, String> header = new HashMap<>();
      header.put("Authorization", "Bearer " + authToken.get("token"));
      header.put("signature", authToken.get("signature"));
      header.put("timestamp", authToken.get("timeStamp"));
      header.put("csn", data.get(TestingHubConstants.csn));
      header.put("Content-Type", "application/json");

      String victimContractNumbers = data.get(TestingHubConstants.victimContractNumbers);
      Util.PrintInfo(" victimContractNumbers ::" + victimContractNumbers);

      String survivorContractNumber = data.get(TestingHubConstants.survivorContractNumber);
      Util.PrintInfo(" survivorContractNumber ::" + survivorContractNumber);

      JSONArray victim = new JSONArray();
      victim.add(victimContractNumbers);
      Util.PrintInfo("victim ::" + victim);

      JSONObject apigeeBody = readJSON("ContractMerge.json");
      apigeeBody.putIfAbsent(TestingHubConstants.victimContractNumbers, victim);
      apigeeBody.putIfAbsent(TestingHubConstants.survivorContractNumber, survivorContractNumber);

      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(apigeeBody.toString());
      Util.printInfo(json.toJSONString());
      getResponsebody = postRestResponse(baseURL, resourceUrl, header, json);
        if (getResponsebody != null) {
            json = (JSONObject) parser.parse(getResponsebody);
            String txStatus = json.get("transactionStatus").toString();
            Util.PrintInfo("Status is : " + txStatus);
        } else {
            AssertUtils.fail("Response is null after performing Contract Merge opration");
        }
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo(ErrorEnum.CONTRACT_MERGE_APIGEE.geterr());
    }
    return getResponsebody;
  }

  @Step("Student VSOS generate WorkFlow ID " + GlobalConstants.TAG_TESTINGHUB)
  public String getStudentWorkId(LinkedHashMap<String, String> testDataForEachMethod) {
    String workID = null;
    try {
      HashMap<String, Object> dataSet = new HashMap<>();
      dataSet.put("O2ID", testDataForEachMethod.get(TestingHubConstants.oxygenid));
      dataSet.put("UUID", UUID.randomUUID().toString().trim());

      dataSet.put("Offering", testDataForEachMethod.get("Offering"));
      dataSet.put("Offer", testDataForEachMethod.get("Offer"));

      dataSet.put("StartDate", "2020-06-02");
      dataSet.put("EndDate", "2021-06-01");

      String jsonFile = ApigeeTestBase.getFilePath("jsonFiles", "StudentFulfillment.json");
      String payload = LoadJsonWithValue.loadJson(dataSet, jsonFile).toString();

      String baseUrl = testDataForEachMethod.get("ipaStudentFulfillmentEndPoint");
      RestAssured.baseURI = baseUrl;

      HashMap<String, String> authHeaders = new HashMap<String, String>();
      authHeaders.put("Authorization", testDataForEachMethod.get("Authorization"));
      authHeaders.put("Content-Type", "application/json");
      authHeaders.put("X-API-Key", "1gj7n05VdoVT88rtPqMfpsum0sWdIGSQGEzykOk4");

      Response response = given().headers(authHeaders).body(payload).when().post(baseUrl);
      String result = response.getBody().asString();

      Util.printInfo(result);
      JsonPath js = new JsonPath(result);
      Util.printInfo("js is:" + js);
      workID = js.get("id");

      testDataForEachMethod.put("workID", workID);
      testDataForEachMethod.put("Offering", dataSet.get("Offering").toString());
      testDataForEachMethod.put("Offer", dataSet.get("Offer").toString());
      testDataForEachMethod.put("StartDate", dataSet.get("StartDate").toString());
      testDataForEachMethod.put("EndDate", dataSet.get("EndDate").toString());
    } catch (Exception e) {
      e.printStackTrace();
      AssertUtils.fail("Unable to generate WorkID for Student");
    }
    return workID;
  }

  @SuppressWarnings("unchecked")
  @Step("Non Subscription Asset Split API Validation" + GlobalConstants.TAG_TESTINGHUB)
  public String postNonSubsAssetSplitTransaction(HashMap<String, String> data) {
    String getResponsebody = null;
    try {
      String baseURL = data.get(TestingHubConstants.baseUrl);
      String resourceUrl = data.get(TestingHubConstants.apigeeAssetSplit);
      HashMap<String, String> authToken = getAuthToken(data);
      HashMap<String, String> header = new HashMap<>();
      header.put("Authorization", "Bearer " + authToken.get("token"));
      header.put("signature", authToken.get("signature"));
      header.put("timestamp", authToken.get("timeStamp"));
      header.put("csn", data.get(TestingHubConstants.csn));
      header.put("Content-Type", "application/json");

      String assetNumber = data.get(TestingHubConstants.assetNumber);
      Util.PrintInfo(" Asset Number ::" + assetNumber);

      String assetSplitQty = data.get(TestingHubConstants.assetSplitQty);
      Util.PrintInfo("Asset Qty For Split  Number ::" + assetSplitQty);

      JSONArray assetSplitQtyFinal = new JSONArray();
      assetSplitQtyFinal.add(assetSplitQty);
      assetSplitQtyFinal.add("1");
      Util.PrintInfo("assetSplitQtyFinal ::" + assetSplitQtyFinal);

      JSONObject apigeeBody = readJSON("NonSubsAssetSplit.json");
      apigeeBody.putIfAbsent(TestingHubConstants.assetNumber, assetNumber);
      apigeeBody.putIfAbsent(TestingHubConstants.assetSplitQty, assetSplitQtyFinal);

      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(apigeeBody.toString());
      Util.printInfo("Payload : " + json.toJSONString());
      getResponsebody = postRestResponse(baseURL, resourceUrl, header, json);
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo(ErrorEnum.NONSUBS_ASSET_SPLIT_APIGEE.geterr());
    }
    return getResponsebody;
  }

  @SuppressWarnings("unchecked")
  @Step("Contract transfer operation using API" + GlobalConstants.TAG_TESTINGHUB)
  public String postContractTransferTransaction(HashMap<String, String> data) {
    String getResponsebody = null;
    try {
      String baseURL = data.get(TestingHubConstants.baseUrl);
      String resourceUrl = data.get(TestingHubConstants.apigeeContractTransfer);
      HashMap<String, String> authToken = getAuthToken(data);
      HashMap<String, String> header = new HashMap<>();
      header.put("Authorization", "Bearer " + authToken.get("token"));
      header.put("signature", authToken.get("signature"));
      header.put("timestamp", authToken.get("timeStamp"));
      header.put("csn", data.get(TestingHubConstants.csn));
      header.put("Content-Type", "application/json");

      String sourceContractNumber = data.get(TestingHubConstants.sourceContractNumber);
      Util.PrintInfo(" sourceContractNumber ::" + sourceContractNumber);
      String targetContractNumber = data.get(TestingHubConstants.targetContractNumber);
      Util.PrintInfo(" targetContractNumber ::" + targetContractNumber);

        if (sourceContractNumber != null && targetContractNumber != null
            && !(sourceContractNumber.equals("") && targetContractNumber.equals(""))) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("contractNumber", sourceContractNumber);
            jsonObj.put("assetNumber", data.get("sourceAssetNumber"));
            JSONArray jsonArray = new JSONArray();
            jsonArray.add(jsonObj);

            JSONObject apigeeBody = readJSON("ContractTransfer.json");
            apigeeBody.putIfAbsent(TestingHubConstants.targetContractNumber, targetContractNumber);
            apigeeBody.putIfAbsent(TestingHubConstants.sourceAssets, jsonArray);

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(apigeeBody.toString());
            Util.printInfo(json.toJSONString());
            getResponsebody = postRestResponse(baseURL, resourceUrl, header, json);
            if (getResponsebody != null) {
                json = (JSONObject) parser.parse(getResponsebody);
                System.out.println(json);
                String txStatus = json.get("validationStatus").toString();
                Util.PrintInfo("Status is : " + txStatus);
            } else {
                AssertUtils.fail("Response is null after performing Contract Transfer opration");
            }
        } else {
            AssertUtils.fail("Target or source contract number is null...");
        }
    } catch (Exception e) {
      e.printStackTrace();
      Util.printInfo(ErrorEnum.CONTRACT_TRANSFER_APIGEE.geterr());
    }
    return getResponsebody;
  }
}
