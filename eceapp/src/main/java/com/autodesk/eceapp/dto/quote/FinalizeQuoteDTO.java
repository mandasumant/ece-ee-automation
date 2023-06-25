package com.autodesk.eceapp.dto.quote;

import lombok.Builder;

public @Builder
class FinalizeQuoteDTO {

  private String quoteNumber;
  private AgentAccountDTO agentAccount;
  private AgentContactDTO agentContact;
}
