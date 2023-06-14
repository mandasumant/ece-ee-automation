package com.autodesk.ece.dto.quote.v2;

import com.autodesk.eceapp.constants.BICECEConstants;
import java.util.LinkedHashMap;
import lombok.Data;

public @Data
class LineItemDTO {

  private String offeringId;
  private String action = "New";
  private Integer quantity;
  private String startDate;
  private OfferDTO offer;

  public LineItemDTO(LinkedHashMap<String, String> data) {
    this.offeringId = data.get(BICECEConstants.OFFERING_ID);
    this.quantity = Integer.valueOf(data.get(BICECEConstants.FLEX_TOKENS));
    //this.subscriptionStartDate = data.get(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE);
  }
}
