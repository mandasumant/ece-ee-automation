package com.autodesk.ece.dto;

import lombok.Builder;

public @Builder
class FinalizeQuoteDTO {

  private String quoteNumber;
  private AgentAccountDTO agentAccount;
  private AgentContactDTO agentContact;
}
