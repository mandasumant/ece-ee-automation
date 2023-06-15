package com.autodesk.ece.dto.quote.v2;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OfferDTO {
  private OfferItemDTO term;
  private OfferItemDTO accessModel;
  private OfferItemDTO servicePlanId;
  private OfferItemDTO intendedUsage;
  private OfferItemDTO connectivity;

  private static Map<String, OfferItemDTO> termCodeMap;
  static {
    termCodeMap = new HashMap<>();
    termCodeMap.put("Annual", new OfferItemDTO("A01", "Annual"));
    termCodeMap.put("3_Year", new OfferItemDTO("A06", "3 Year"));
    termCodeMap.put("Multi-Year", new OfferItemDTO("A06", "3 Year"));
    termCodeMap.put("Monthly", new OfferItemDTO("A02", "Monthly"));
  }

  public static Map<String, OfferItemDTO> getTermCodeMap() {
    return termCodeMap;
  }
}
