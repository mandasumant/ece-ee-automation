package com.autodesk.ece.testbase.service.quote.impl;

import static io.restassured.RestAssured.given;
import com.autodesk.ece.dto.quote.AgentAccountDTO;
import com.autodesk.ece.dto.quote.AgentContactDTO;
import com.autodesk.ece.dto.quote.EndCustomerDTO;
import com.autodesk.ece.dto.quote.FinalizeQuoteDTO;
import com.autodesk.ece.dto.PWSAccessInfo;
import com.autodesk.ece.dto.quote.PurchaserDTO;
import com.autodesk.ece.dto.QuoteDetails;
import com.autodesk.ece.dto.quote.v2.LineItemDTO;
import com.autodesk.ece.dto.quote.v2.QuoteDTO;
import com.autodesk.ece.testbase.service.quote.PwsQuoteDataBuilder;
import com.autodesk.ece.testbase.service.quote.QuoteService;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.utilities.Address;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.eseapp.sfdc.SFDCAPI;
import com.google.gson.Gson;
import io.qameta.allure.Step;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ResponseBodyExtractionOptions;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service with Apollo R2.1.2 APIs
 */
public class PWSV2Service implements QuoteService {
  private final String clientId;
  private final String clientSecret;
  private final String hostname;

  public PWSV2Service(String clientId, String clientSecret, String hostname) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.hostname = hostname;
  }

  protected String createQuoteBody(LinkedHashMap<String, String> data, Address address, String csn,
      String agentContactEmail, Boolean isMultiLineItem) {
    PurchaserDTO purchaser = new PurchaserDTO(data);

    if (System.getProperty(BICECEConstants.ENVIRONMENT).equals(BICECEConstants.ENV_STG)) {
      agentContactEmail = agentContactEmail.replace(BICECEConstants.ENV_INT.toLowerCase(),
          BICECEConstants.ENV_STG.toLowerCase()).replace("@", "_2@").replace("_1", "");
    }

    AgentContactDTO agentContact = new AgentContactDTO(agentContactEmail);
    AgentAccountDTO agentAccount = new AgentAccountDTO(csn);

    List<LineItemDTO> lineItems = PwsQuoteDataBuilder.getLineItems(data, isMultiLineItem);

    EndCustomerDTO endCustomer = null;
    if (System.getProperty("existingCSN") != null) {
      endCustomer = new EndCustomerDTO(System.getProperty("existingCSN"));
    } else if (data.get(BICECEConstants.PAYER_CSN) != null) {
      endCustomer = new EndCustomerDTO(data.get(BICECEConstants.PAYER_CSN));
    } else {
      endCustomer = new EndCustomerDTO(address);
    }

    QuoteDTO quote = QuoteDTO.builder()
        .lineItems(lineItems)
        .purchaser(purchaser)
        .endCustomer(endCustomer)
        .agentContact(agentContact)
        .agentAccount(agentAccount)
        .currency(data.get(BICECEConstants.currencyStore))
        .build();

    return new Gson().toJson(quote);
  }

  private String createQuoteFinalizeBody(String quoteNo, String agentCsn,
      String agentContactEmail) {
    AgentContactDTO agentContact = new AgentContactDTO(agentContactEmail);
    AgentAccountDTO agentAccount = new AgentAccountDTO(agentCsn);

    FinalizeQuoteDTO finalizeQuote =
        FinalizeQuoteDTO.builder().quoteNumber(quoteNo).agentContact(agentContact).agentAccount(agentAccount)
            .build();

    return new Gson().toJson(finalizeQuote);
  }

  @Step("Create and Finalize Quote" + GlobalConstants.TAG_TESTINGHUB)
  public String createAndFinalizeQuote(Address address, String csn,
      String agentContactEmail, LinkedHashMap<String, String> data, Boolean isMultiLineItem) {
    PWSAccessInfo access_token = QuoteService.getAccessToken(clientId, clientSecret, hostname);
    String signature = QuoteService.signString(access_token.getToken(), clientSecret, access_token.getTimestamp());
    String quoteNumber = null;

    Map<String, String> pwsRequestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.getToken());
      put("CSN", csn);
      put("signature", signature);
      put("timestamp", access_token.getTimestamp());
      put("timezone_city", System.getProperty("timezone"));
    }};

    String payloadBody = createQuoteBody(data, address, csn, agentContactEmail, isMultiLineItem);
    Util.printInfo("Create Quote Payload: " + payloadBody);
    Util.printInfo("Headers: " + pwsRequestHeaders);

    Response response = given()
        .headers(pwsRequestHeaders)
        .body(payloadBody)
        .post(getCreateQuoteUrl(hostname))
        .then().extract().response();

    Util.printInfo("Quote Creation Response : " + response.prettyPrint());

    String transactionId = response.jsonPath().getString("transactionId");
    Util.printInfo("Quote requested, transactionId: " + transactionId);

    int attempts = 0;

    while (attempts < 20) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = getQuoteStatus(pwsRequestHeaders, transactionId);
      Util.printInfo("Quote Creation Response : " + response.prettyPrint());
      String status = response.jsonPath().getString("quoteStatus") == null ? "error" :
          response.jsonPath().getString("quoteStatus");

      if (status.equals("DRAFT-CREATED")) {
        quoteNumber = response.jsonPath().getString("quoteNumber");
        Util.printInfo("Got quote in DRAFT-CREATED status, quote number: " + quoteNumber);
        break;
      } else if (status.equals("FAILED")) {
        Util.printError(response.jsonPath().getJsonObject("error").toString());
        AssertUtils.fail("Quote creation failed, error: " + response.jsonPath().getString("error.message"));
      } else if (attempts >= 19) {
        AssertUtils.fail("Retry exhausted: Failed to get quote in Created");
      } else {
        Util.printInfo("Quote not created yet, status: " + status);
      }
    }

    Util.printInfo("Getting Quote Details");
    Response res = getQuoteDetailsResponse(csn, quoteNumber);
    Util.printInfo("The Quote Details : " + res.prettyPrint());
    Util.sleep(30000);
    return finalizeQuote(quoteNumber, transactionId, csn, agentContactEmail);
  }

  protected String getCreateQuoteUrl(final String hostname) {
    final String createQuoteURL = MessageFormat.format("https://{0}/v2/quotes", hostname);
    Util.printInfo("Create Quote URL: " + createQuoteURL);
    return createQuoteURL;
  }

  @Step("Finalize Quote" + GlobalConstants.TAG_TESTINGHUB)
  private String finalizeQuote(String quoteId, String transactionId, String agentCsn, String agentContactEmail) {
    PWSAccessInfo access_token = QuoteService.getAccessToken(clientId, clientSecret, hostname);
    String signature = QuoteService.signString(access_token.getToken(), clientSecret, access_token.getTimestamp());

    Map<String, String> pwsRequestHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.getToken());
      put("CSN", agentCsn);
      put("signature", signature);
      put("timestamp", access_token.getTimestamp());
      put("timezone_city", System.getProperty("timezone"));
    }};

    String finalizeBody = createQuoteFinalizeBody(quoteId, agentCsn, agentContactEmail);

    Util.sleep(60000);

    final String finalizeQuoteURL = MessageFormat.format("https://{0}/v1/quotes/finalize", hostname);
    Util.printInfo("Finalize Quote URL: " + finalizeQuoteURL);

    Response response = given()
        .body(finalizeBody)
        .headers(pwsRequestHeaders)
        .put(finalizeQuoteURL)
        .then().extract().response();

    Util.printInfo("The finalize response headers: " + response.getHeaders());

    if (response.getStatusCode() != 202) {
      AssertUtils.fail("Finalize Quote API returned a non 202 response. Response code: " + response.getStatusCode());
    }

    int attempts = 0;

    while (attempts < 10) {
      attempts++;
      Util.sleep((long) (1000L * Math.pow(attempts, 2)));
      Util.printInfo("Attempting to get status on transaction, attempt: " + attempts);
      response = getQuoteStatus(pwsRequestHeaders, transactionId);
      Util.printInfo("Quote Finalization Response :" + response.prettyPrint());
      String status = response.jsonPath().getString("quoteStatus");

      if (status.equals("QUOTED")) {
        Util.printInfo("Got quote in QUOTED state: " + quoteId);
        return quoteId;
      } else if (status.equals("FAILED") || status.equals("DRAFT-CREATED")) {
        if (response.jsonPath().getJsonObject("error") != null) {
          Util.printError(response.jsonPath().getJsonObject("error").toString());
          AssertUtils.fail("Quote finalization failed");
        }
      } else if (status.equals("UNDER-REVIEW") || (status.equals("FINALIZING") && attempts == 5)) {
        SFDCAPI sfdcApi = new SFDCAPI();
        QuoteDetails quoteDetails = getQuoteDetails(agentCsn, quoteId);

        String accountName = quoteDetails.getEndCustomerName();
        String accountCSN = quoteDetails.getEndCustomerAccountCsn();
        String streetAddress = quoteDetails.getEndCustomerAddressLine1();
        String city = quoteDetails.getEndCustomerCity();
        String countryCode = quoteDetails.getEndCustomerCountryCode();
        String postalCode = quoteDetails.getEndCustomerPostalCode();
        Util.printInfo("SFDC Input  : " + countryCode + " " + postalCode + " " + accountName + " " + streetAddress + " "
            + accountCSN);
        boolean publishAccountEC = sfdcApi.publishAccountEC(accountName, accountCSN, streetAddress, city, countryCode,
            postalCode);
        if (!publishAccountEC) {
          AssertUtils.fail("Failed to publish account to EC system.");
        }

        Util.sleep(10000); // Waiting for the status change

        Response statusRes = getQuoteStatus(pwsRequestHeaders, transactionId);
        Util.printInfo("Quote Status Response : " + statusRes.prettyPrint());
      } else {
        Util.printInfo("Quote not finalized yet, status: " + status);
      }
    }
    AssertUtils.fail("Failed to change quote status to QUOTED");
    return "";
  }

  private Response getQuoteStatus(Map<String, String> headers, String transactionId) {
    final String getQuoteStatusUrl = MessageFormat.format("https://{0}/v1/quotes/status?transactionId={1}", hostname, transactionId);
    Util.printInfo("Get Quote Status URL: " + getQuoteStatusUrl);

    return given()
        .headers(headers)
        .get(getQuoteStatusUrl)
        .then().extract().response();
  }
  @Step("Get Quote" + GlobalConstants.TAG_TESTINGHUB)
  public QuoteDetails getQuoteDetails(String agentCSN, String quoteNo) {
    final Response quoteDetails = getQuoteDetailsResponse(agentCSN, quoteNo);

    return Optional.of(quoteDetails)
      .map(ResponseBodyExtractionOptions::jsonPath)
      .map(this::createObjectFromJsonPath)
      .orElseThrow(() -> new RuntimeException("Could not retrieve purchaser / end customer information from Quote Details API"));
  }

  @Step("Get Quote Response" + GlobalConstants.TAG_TESTINGHUB)
  public Response getQuoteDetailsResponse(String agentCSN, String quoteNo) {
    Util.printInfo("Calling Get Quote Details API");
    PWSAccessInfo access_token = QuoteService.getAccessToken(clientId, clientSecret, hostname);
    String signature = QuoteService.signString(access_token.getToken(), clientSecret, access_token.getTimestamp());

    Map<String, String> newHeaders = new HashMap<String, String>() {{
      put("Authorization", "Bearer " + access_token.getToken());
      put("CSN", agentCSN);
      put("signature", signature);
      put("timestamp", access_token.getTimestamp());
      put("timezone_city", System.getProperty("timezone"));
    }};

    return given()
        .headers(newHeaders)
        .get(getQuoteDetailsUrl(hostname, quoteNo))
        .then().extract().response();
  }

  protected String getQuoteDetailsUrl(final String hostname, final String quoteNo) {
    final String getQuoteDetailsURL = MessageFormat.format("https://{0}/v2/quotes?filter[quoteNumber]={1}", hostname, quoteNo);
    Util.printInfo("Get Quote Details URL: " + getQuoteDetailsURL);

    return getQuoteDetailsURL;
  }

  private QuoteDetails createObjectFromJsonPath(final JsonPath jsonPath) {
    return QuoteDetails.builder()
      .purchaserFirstName(jsonPath.getString("purchaser.firstName"))
      .purchaserLastName(jsonPath.getString("purchaser.lastName"))
      .endCustomerName(jsonPath.getString("endCustomer.name"))
      .endCustomerAccountCsn(jsonPath.getString("endCustomer.accountCsn"))
      .endCustomerAddressLine1(jsonPath.getString("endCustomer.addressLine1"))
      .endCustomerCity(jsonPath.getString("endCustomer.city"))
      .endCustomerCountryCode(jsonPath.getString("endCustomer.countryCode"))
      .endCustomerPostalCode(jsonPath.getString("endCustomer.postalCode"))
      .build();
  }
}
