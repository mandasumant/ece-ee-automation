package com.autodesk.eceapp.dto.quote.v1;

import com.autodesk.eceapp.constants.BICECEConstants;
import java.util.LinkedHashMap;
import lombok.Data;

public @Data
class LineItemDTO {

  private Integer quantity;
  private String offeringId;
  private String orderAction = "New";
  private String subscriptionStartDate;
  private OfferDTO offer;

  public LineItemDTO(LinkedHashMap<String, String> data) {
    this.offeringId = data.get(BICECEConstants.OFFERING_ID);
    this.quantity = Integer.valueOf(data.get(BICECEConstants.FLEX_TOKENS));
    //this.subscriptionStartDate = data.get(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE);
  }
}
