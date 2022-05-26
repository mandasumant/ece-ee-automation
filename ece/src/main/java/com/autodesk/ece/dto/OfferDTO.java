package com.autodesk.ece.dto;

import com.autodesk.ece.constants.BICECEConstants;
import java.util.LinkedHashMap;
import lombok.Data;

public @Data
class OfferDTO {
  private String term;
  private String accessModel = "Flex";
  private String intendedUsage = "Commercial";
  private String connectivity = "Online";
  private String servicePlan = "Standard";
  private String billingBehavior = "Once";
  private String billingType = "Up-front";

  public OfferDTO(LinkedHashMap<String, String> data) {
    this.term = data.get(BICECEConstants.TERM);
  }
}
