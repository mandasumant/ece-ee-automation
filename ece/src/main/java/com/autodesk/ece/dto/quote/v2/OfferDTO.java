package com.autodesk.ece.dto.quote.v2;

import com.autodesk.eceapp.constants.BICECEConstants;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class OfferDTO {
  private OfferItemDTO term;
  private OfferItemDTO accessModel = new OfferItemDTO("F", "Flex");
  private OfferItemDTO servicePlanId = new OfferItemDTO("STND", "Standard");
  private OfferItemDTO intendedUsage = new OfferItemDTO("COM", "Commercial");
  private OfferItemDTO connectivity = new OfferItemDTO("C100", "Online");

  public static Map<String, String> termDescriptionCodeMap;
  static {
    termDescriptionCodeMap = new HashMap<>();
    termDescriptionCodeMap.put("Mutli-Year", "A03");
    termDescriptionCodeMap.put("Annual", "A01");
  }

  public OfferDTO(LinkedHashMap<String, String> data) {
    this.term = new OfferItemDTO(
        termDescriptionCodeMap.getOrDefault(data.get(BICECEConstants.TERM), data.get(BICECEConstants.TERM_CODE)), //code
        data.get(BICECEConstants.TERM) // description
    );
  }
}
