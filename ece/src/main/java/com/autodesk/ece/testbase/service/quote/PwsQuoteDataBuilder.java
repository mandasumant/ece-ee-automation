package com.autodesk.ece.testbase.service.quote;

import com.autodesk.ece.dto.quote.v2.LineItemDTO;
import com.autodesk.ece.dto.quote.v2.OfferDTO;
import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.utilities.StringUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PwsQuoteDataBuilder {
  public static List<LineItemDTO> getLineItems(LinkedHashMap<String, String> testData, Boolean isMultiLineItem) {
    final String quoteLineItems = System.getProperty(BICECEConstants.QUOTE_LINE_ITEMS);
    return Optional.ofNullable(quoteLineItems)
        .map(PwsQuoteDataBuilder::getSUSLineItems)
        .orElse(getFlexLineItems(isMultiLineItem, testData));
  }

  private List<LineItemDTO> getSUSLineItems(final String quotes) {
    return Optional.ofNullable(quotes)
        .map(StringUtil::convertStringToListOfMaps)
        .map(PwsQuoteDataBuilder::convertToLineItemObject)
        .orElseThrow(RuntimeException::new);
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
          .quantity(Optional.ofNullable(quoteMap.get("quantity")).map(Integer::valueOf).orElse(getRandomInt(1,10)))
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
