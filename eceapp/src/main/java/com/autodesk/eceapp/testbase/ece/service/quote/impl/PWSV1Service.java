package com.autodesk.eceapp.testbase.ece.service.quote.impl;

import com.autodesk.eceapp.dto.quote.v1.OfferDTO;
import com.autodesk.eceapp.dto.QuoteDetails;
import com.autodesk.eceapp.dto.quote.AgentAccountDTO;
import com.autodesk.eceapp.dto.quote.AgentContactDTO;
import com.autodesk.eceapp.dto.quote.EndCustomerDTO;
import com.autodesk.eceapp.dto.quote.PurchaserDTO;
import com.autodesk.eceapp.dto.quote.v1.LineItemDTO;
import com.autodesk.eceapp.dto.quote.v1.QuoteDTO;
import com.autodesk.eceapp.testbase.ece.service.quote.QuoteService;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.utilities.Address;
import com.autodesk.testinghub.core.utils.Util;
import com.google.gson.Gson;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This class can be deleted without modifying any code in tests when Apollo R2.1.2 is DONE
 */
public class PWSV1Service extends PWSV2Service implements QuoteService {
  public PWSV1Service(String clientId, String clientSecret, String hostname) {
    super(clientId, clientSecret, hostname);
  }

  @Override
  public String createAndFinalizeQuote(Address address, String csn, String agentContactEmail,
      LinkedHashMap<String, String> data, Boolean isMultiLineItem) {
    return super.createAndFinalizeQuote(address, csn, agentContactEmail, data, isMultiLineItem);
  }

  @Override
  protected String getCreateQuoteUrl(final String hostname) {
    final String createQuoteURL = MessageFormat.format("https://{0}/v1/quotes", hostname);
    Util.printInfo("Create Quote URL: " + createQuoteURL);
    return createQuoteURL;
  }

  @Override
  public QuoteDetails getQuoteDetails(String agentCSN, String quoteNo) {
    return super.getQuoteDetails(agentCSN, quoteNo);
  }

  @Override
  protected String getQuoteDetailsUrl(final String hostname, final String quoteNo) {
    final String getQuoteDetailsURL = MessageFormat.format("https://{0}/v1/quotes?quoteNumber={1}", hostname, quoteNo);
    Util.printInfo("Get Quote Details URL: " + getQuoteDetailsURL);
    return getQuoteDetailsURL;
  }

  @Override
  protected String createQuoteBody(LinkedHashMap<String, String> data, Address address, String csn,
      String agentContactEmail, Boolean isMultiLineItem) {
    PurchaserDTO purchaser = new PurchaserDTO(data);

    if (System.getProperty(BICECEConstants.ENVIRONMENT).equals(BICECEConstants.ENV_STG)) {
      agentContactEmail = agentContactEmail.replace(BICECEConstants.ENV_INT.toLowerCase(),
          BICECEConstants.ENV_STG.toLowerCase()).replace("@", "_2@").replace("_1", "");
    }

    AgentContactDTO agentContact = new AgentContactDTO(agentContactEmail);
    AgentAccountDTO agentAccount = new AgentAccountDTO(csn);
    OfferDTO offer = new OfferDTO(data);
    LineItemDTO lineItem = new LineItemDTO(data);
    List<LineItemDTO> lineItems = new ArrayList<>();
    lineItem.setOffer(offer);
    lineItems.add(lineItem);
    if (isMultiLineItem) {
      if (System.getProperty("quantity2") != null) {
        data.put(BICECEConstants.FLEX_TOKENS, System.getProperty("quantity2"));
      } else {
        data.put(BICECEConstants.FLEX_TOKENS, "4000");
      }
      LineItemDTO lineItemTwo = new LineItemDTO(data);
      lineItemTwo.setOffer(offer);
      lineItems.add(lineItemTwo);
    }

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
        .quoteNote(BICECEConstants.QUOTE_NOTES)
        .build();

    return new Gson().toJson(quote);
  }
}
