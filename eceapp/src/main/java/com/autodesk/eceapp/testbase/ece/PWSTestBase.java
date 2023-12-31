package com.autodesk.eceapp.testbase.ece;

import com.autodesk.eceapp.dto.QuoteDetails;
import com.autodesk.eceapp.testbase.ece.service.quote.QuoteService;
import com.autodesk.eceapp.testbase.ece.service.quote.QuoteServiceBuilder;
import com.autodesk.eceapp.utilities.Address;
import java.util.LinkedHashMap;

public class PWSTestBase {
  private final QuoteService quoteService;

  public PWSTestBase(
      final String pwsClientId,
      final String pwsClientSecret,
      final String pwsClientId_v2,
      final String pwsClientSecret_v2,
      final String pwsHostname) {
    this.quoteService = QuoteServiceBuilder.build(
        pwsClientId, pwsClientSecret, pwsClientId_v2, pwsClientSecret_v2, pwsHostname);
  }

  public static String getQuoteStartDateAsString() {
    return QuoteService.getQuoteStartDateAsString();
  }

  public String createAndFinalizeQuote(Address address, String csn,
      String agentContactEmail, LinkedHashMap<String, String> data, Boolean isMultiLineItem) {
    return quoteService.createAndFinalizeQuote(
        address,
        csn,
        agentContactEmail,
        data,
        isMultiLineItem);
  }

  public String createAndFinalizeQuote(Address address, String csn, String agentContactEmail,
      LinkedHashMap<String, String> data) {
    return this.createAndFinalizeQuote(address, csn, agentContactEmail, data, false);
  }

  public QuoteDetails getQuoteDetails(String agentCSN, String quoteNo) {
    return quoteService.getQuoteDetails(agentCSN, quoteNo);
  }
}
