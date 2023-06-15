package com.autodesk.ece.testbase.service.quote.impl;

import com.autodesk.ece.dto.quote.v2.LineItemDTO;
import com.autodesk.ece.dto.quote.v2.OfferDTO;
import com.autodesk.ece.dto.quote.v2.OfferItemDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PwsQuoteDataBuilder {
  public static List<LineItemDTO> getLineItems(final String productName, final String term) {
    switch (productName) {
      case "FUSION_360_CLOUD":
        return getFusion360CloudLineItems(term);

      case "PREMIUM":
        return getPremiumItems(term);

      case "FUSION_360_SIMULATION_EXTENSION":
        return getFusion360SimulationExtensionLineItems(term);

      case "3DS_MAX":
        return get3dsMaxLineItems(term);
    }

    throw new RuntimeException("Product name didn't match");
  }

  private List<LineItemDTO> getFusion360CloudLineItems(final String term) {
    List<LineItemDTO> lineItems = new ArrayList<>();

    OfferDTO offer1 = OfferDTO.builder()
        .term(OfferDTO.getTermCodeMap().get(term))
        .accessModel(new OfferItemDTO("SU", "Single User"))
        .servicePlanId(new OfferItemDTO("STND", "Standard"))
        .intendedUsage(new OfferItemDTO("COM", "Commercial"))
        .connectivity(new OfferItemDTO("C100", "Online"))
        .build();
    LineItemDTO lineItem1 = LineItemDTO.builder()
        .offeringId("OD-000171")
        .action("New")
        .quantity(getRandomInt(1,5))
        .offer(offer1)
        .build();
    lineItems.add(lineItem1);

    return lineItems;
  }

  private List<LineItemDTO> getPremiumItems(final String term) {
    List<LineItemDTO> lineItems = new ArrayList<>();

    OfferDTO offer1 = OfferDTO.builder()
        .accessModel(new OfferItemDTO("SU", "Single User"))
        .term(OfferDTO.getTermCodeMap().get(term))
        .intendedUsage(new OfferItemDTO("COM", "Commercial"))
        .connectivity(new OfferItemDTO("C100", "Online"))
        .servicePlanId(new OfferItemDTO("PREMSUB", "Premium"))
        .build();
    LineItemDTO lineItem1 = LineItemDTO.builder()
        .offeringId("OD-000321")
        .action("New")
        .quantity(getRandomInt(1,5))
        .offer(offer1)
        .build();
    lineItems.add(lineItem1);

    return lineItems;
  }

  private List<LineItemDTO> getFusion360SimulationExtensionLineItems(final String term) {
    List<LineItemDTO> lineItems = new ArrayList<>();

    OfferDTO offer1 = OfferDTO.builder()
        .accessModel(new OfferItemDTO("SU", "Single User"))
        .term(OfferDTO.getTermCodeMap().get(term))
        .intendedUsage(new OfferItemDTO("COM", "Commercial"))
        .connectivity(new OfferItemDTO("C100", "Online"))
        .servicePlanId(new OfferItemDTO("STND", "Standard"))
        .build();
    LineItemDTO lineItem1 = LineItemDTO.builder()
        .offeringId("OD-002689")
        .action("New")
        .quantity(getRandomInt(1,5))
        .offer(offer1)
        .build();
    lineItems.add(lineItem1);

    return lineItems;
  }

  private List<LineItemDTO> get3dsMaxLineItems(final String term) {
    List<LineItemDTO> lineItems = new ArrayList<>();

    OfferDTO offer1 = OfferDTO.builder()
        .accessModel(new OfferItemDTO("SU", "Single User"))
        .term(OfferDTO.getTermCodeMap().get(term))
        .intendedUsage(new OfferItemDTO("COM", "Commercial"))
        .connectivity(new OfferItemDTO("C100", "Online"))
        .servicePlanId(new OfferItemDTO("STND", "Standard"))
        .build();
    LineItemDTO lineItem1 = LineItemDTO.builder()
        .offeringId("OD-000021")
        .action("New")
        .quantity(getRandomInt(1,5))
        .offer(offer1)
        .build();
    lineItems.add(lineItem1);

    return lineItems;
  }

  private int getRandomInt(int min, int max) {
    return  (int)Math.floor(Math.random() * (max - min + 1) + min);
  }
}
