package com.autodesk.eceapp.testbase.ece.service.quote;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.dto.quote.v2.LineItemDTO;
import com.autodesk.eceapp.dto.quote.v2.OfferDTO;
import com.autodesk.eceapp.utilities.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class PwsQuoteDataBuilder {
  public static List<LineItemDTO> getLineItems(LinkedHashMap<String, String> testData, Boolean isMultiLineItem) {
    final String quoteLineItemString = System.getProperty(BICECEConstants.QUOTE_LINE_ITEMS);
    final String renewalQuoteLineItemString = System.getProperty(BICECEConstants.RENEWAL_QUOTE_LINE_ITEMS);
    final boolean isRenewalsOnly = StringUtils.isNotBlank(System.getProperty(BICECEConstants.RENEWAL_QUOTES_ONLY));

    final List<LineItemDTO> quoteLineItems = new ArrayList<>();

    if (!isRenewalsOnly) {
      List<LineItemDTO> lineItems = Optional.ofNullable(quoteLineItemString)
          .map(PwsQuoteDataBuilder::getSUSLineItems)
          .orElseGet(() -> getFlexLineItems(isMultiLineItem, testData));
      quoteLineItems.addAll(lineItems);
    }

    final List<LineItemDTO> renewalQuoteLineItems = Optional.ofNullable(renewalQuoteLineItemString)
        .map(PwsQuoteDataBuilder::getRenewalLineItems)
        .orElse(Collections.emptyList());

    quoteLineItems.addAll(renewalQuoteLineItems);

    return quoteLineItems;
  }

  private List<LineItemDTO> getSUSLineItems(final String quotes) {
    return Optional.ofNullable(quotes)
        .map(StringUtil::convertStringToListOfMaps)
        .map(PwsQuoteDataBuilder::convertToLineItemObject)
        .orElseThrow(RuntimeException::new);
  }

  private List<LineItemDTO> getRenewalLineItems(final String quotes) {
    return Optional.ofNullable(quotes)
        .map(StringUtil::convertStringToListOfMaps)
        .map(PwsQuoteDataBuilder::convertToRenewalLineItemObject)
        .orElseThrow(RuntimeException::new);
  }

  private List<LineItemDTO> convertToRenewalLineItemObject(List<Map<String, String>> quoteListMap) {
    List<LineItemDTO> lineItems = new ArrayList<>();
    quoteListMap.forEach(quoteMap -> {
      LineItemDTO lineItem = LineItemDTO.builder()
          .action("Renewal")
          .quantity(
              Optional.ofNullable(quoteMap.get("quantity")).map(Integer::valueOf).orElseGet(() -> getRandomInt(1, 10)))
          .subscriptionId(quoteMap.get("subscription_id"))
          .build();
      lineItems.add(lineItem);
    });

    return lineItems;
  }

  private List<LineItemDTO> convertToLineItemObject(List<Map<String, String>> quoteListMap) {
    List<LineItemDTO> lineItems = new ArrayList<>();
    quoteListMap.forEach(quoteMap -> {
      OfferDTO offer = OfferDTO.builder()
          .term(quoteMap.get("term"))
          .intendedUsage(quoteMap.get("usage"))
          .servicePlanId(quoteMap.get("plan"))
          .connectivity()
          .accessModel(Optional.ofNullable(quoteMap.get("access_model")).map(String::toLowerCase).orElse("sus"))
          .build();
      LineItemDTO lineItem = LineItemDTO.builder()
          .offeringId(quoteMap.get("offering_id"))
          .action("New")
          .quantity(Optional.ofNullable(quoteMap.get("quantity")).map(Integer::valueOf).orElseGet(() -> getRandomInt(1,10)))
          .offer(offer)
          .build();
      lineItems.add(lineItem);
    });

    return lineItems;
  }

  /**
   * This method is not changed since its original implementation to support flex items
   */
  private List<LineItemDTO> getFlexLineItems(Boolean isMultiLineItem, LinkedHashMap<String, String> data) {
    List<LineItemDTO> lineItems = new ArrayList<>();
    OfferDTO offer = OfferDTO.builder()
        .term(data.get(BICECEConstants.TERM))
        .accessModel("flex")
        .servicePlanId("standard")
        .intendedUsage("commercial")
        .connectivity()
        .build();

    LineItemDTO lineItem = LineItemDTO.builder()
        .offeringId(data.get(BICECEConstants.OFFERING_ID))
        .action("New")
        .quantity(Integer.valueOf(data.get(BICECEConstants.FLEX_TOKENS)))
        .offer(offer)
        .build();
    lineItems.add(lineItem);

    if (isMultiLineItem) {
      if (System.getProperty("quantity2") != null) {
        data.put(BICECEConstants.FLEX_TOKENS, System.getProperty("quantity2"));
      } else {
        data.put(BICECEConstants.FLEX_TOKENS, "4000");
      }

      LineItemDTO lineItemTwo = LineItemDTO.builder()
          .offeringId(data.get(BICECEConstants.OFFERING_ID))
          .action("New")
          .quantity(Integer.valueOf(data.get(BICECEConstants.FLEX_TOKENS)))
          .offer(offer)
          .build();
      lineItems.add(lineItemTwo);
    }

    return lineItems;
  }

  private int getRandomInt(int min, int max) {
    return  (int)Math.floor(Math.random() * (max - min + 1) + min);
  }
}
