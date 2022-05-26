package com.autodesk.ece.dto;

import com.autodesk.ece.constants.BICECEConstants;
import java.util.LinkedHashMap;
import lombok.Data;

public @Data
class LineItemDTO {
  private Float quantity;
  private String offeringId = "OD-000163";
  private String orderAction = "New";
  private String subscriptionStartDate;
  private OfferDTO offer;

  public LineItemDTO(LinkedHashMap<String, String> data) {
    this.quantity = new Float(Integer.valueOf(data.get(BICECEConstants.FLEX_TOKENS)));
    this.subscriptionStartDate = data.get(BICECEConstants.QUOTE_SUBSCRIPTION_START_DATE);
  }
}
