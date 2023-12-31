package com.autodesk.eceapp.dto.quote.v2;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Data;

@Data
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

  private static Map<String, OfferItemDTO> accessModels = ImmutableMap.of(
      "sus", new OfferItemDTO("S", "Single User"),
      "flex", new OfferItemDTO("F", "Flex")
  );

  public OfferDTO(OfferItemDTO term, OfferItemDTO accessModel, OfferItemDTO connectivity, OfferItemDTO servicePlanId, OfferItemDTO intendedUsage) {
    this.term = term;
    this.accessModel = accessModel;
    this.connectivity = connectivity;
    this.servicePlanId = servicePlanId;
    this.intendedUsage = intendedUsage;
  }

  public static OfferDTOBuilder builder() {
    return new OfferDTOBuilder();
  }

  public static class OfferDTOBuilder {
    private OfferItemDTO term;
    private OfferItemDTO accessModel;
    private OfferItemDTO connectivity;
    private OfferItemDTO servicePlanId;
    private OfferItemDTO intendedUsage;

    public OfferDTOBuilder() {
    }

    public OfferDTOBuilder term(String term) {
      this.term = terms.get(term.toLowerCase());
      return this;
    }

    public OfferDTOBuilder accessModel(String accessModel) {
      this.accessModel = accessModels.get(accessModel);
      return this;
    }

    public OfferDTOBuilder connectivity() {
      this.connectivity = new OfferItemDTO("C100", "Online");
      return this;
    }

    public OfferDTOBuilder servicePlanId(String servicePlanId) {
      this.servicePlanId = servicePlans.get(servicePlanId.toLowerCase());
      return this;
    }

    public OfferDTOBuilder intendedUsage(String intendedUsage) {
      this.intendedUsage = intendedUsages.get(intendedUsage.toLowerCase());
      return this;
    }

    public OfferDTO build() {
      return new OfferDTO(this.term, this.accessModel, this.connectivity, this.servicePlanId, this.intendedUsage);
    }
  }
}
