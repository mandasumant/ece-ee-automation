package com.autodesk.ece.dto.quote.v2;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OfferDTO {
  private OfferItemDTO term;
  private OfferItemDTO accessModel;
  private OfferItemDTO connectivity;
  private OfferItemDTO servicePlanId;
  private OfferItemDTO intendedUsage;

  private static Map<String, OfferItemDTO> terms = ImmutableMap.of(
      "annual", new OfferItemDTO("A01", "Annual"),
      "3_year", new OfferItemDTO("A06", "3 Year"),
      "multi-year", new OfferItemDTO("A06", "3 Year"),
      "monthly", new OfferItemDTO("A02", "Monthly")
  );

  private static Map<String, OfferItemDTO> intendedUsages = ImmutableMap.of(
      "commercial", new OfferItemDTO("COM", "Commercial"),
      "demo_eval", new OfferItemDTO("NFR", "Demo/Eval")
  );

  private static Map<String, OfferItemDTO> servicePlans = ImmutableMap.of(
      "standard", new OfferItemDTO("STND", "Standard"),
      "standard_no_support", new OfferItemDTO("STNDNS", "Standard No Support"),
      "premium", new OfferItemDTO("PREMSUB", "Premium"),
      "premium_no_support", new OfferItemDTO("PREMNS", "Premium No Support")
  );

  public static OfferItemDTO getTerm(final String term) {
    return terms.get(term.toLowerCase());
  }
  public static OfferItemDTO getIntendedUsage(final String usage) {
    return intendedUsages.get(usage.toLowerCase());
  }
  public static OfferItemDTO getServicePlanId(final String plan) {
    return servicePlans.get(plan.toLowerCase());
  }
  public static OfferItemDTO getOnlineConnectivity() {
    return new OfferItemDTO("C100", "Online");
  }
  public static OfferItemDTO getSingleUserAccessModel() {
    return new OfferItemDTO("SU", "Single User");
  }
  public static OfferItemDTO getFlexAccessModel() {
    return new OfferItemDTO("F", "Flex");
  }
}
