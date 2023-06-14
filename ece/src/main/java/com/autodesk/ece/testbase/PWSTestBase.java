package com.autodesk.ece.testbase;

import com.autodesk.ece.dto.QuoteDetails;
import com.autodesk.ece.testbase.service.quote.QuoteService;
import com.autodesk.ece.testbase.service.quote.QuoteServiceBuilder;
import com.autodesk.eceapp.utilities.Address;
import java.util.LinkedHashMap;

public class PWSTestBase {
  private final QuoteService quoteService;

  public PWSTestBase(final String pwsClientId, final String pwsClientSecret, final String pwsHostname) {
    this.quoteService = QuoteServiceBuilder.build(pwsClientId, pwsClientSecret, pwsHostname);
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
