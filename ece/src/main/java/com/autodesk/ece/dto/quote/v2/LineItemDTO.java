package com.autodesk.ece.dto.quote.v2;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LineItemDTO {
  private String offeringId;
  private String action;
  private Integer quantity;
  private String startDate;
  private OfferDTO offer;
}
