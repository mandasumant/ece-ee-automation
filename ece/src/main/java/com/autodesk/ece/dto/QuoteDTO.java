package com.autodesk.ece.dto;

import java.util.List;
import lombok.Builder;

public @Builder
class QuoteDTO {

  private String quoteNote;
  private String currency;
  private AgentAccountDTO agentAccount;
  private AgentContactDTO agentContact;
  private EndCustomerDTO endCustomer;
  private PurchaserDTO purchaser;
  private List<LineItemDTO> lineItems;
}
