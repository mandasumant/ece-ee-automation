package com.autodesk.ece.dto;

import java.util.LinkedHashMap;
import lombok.Data;

public @Data
class AgentAccountDTO {
  private String accountCsn;

  public AgentAccountDTO(LinkedHashMap<String, String> data) {
    this.accountCsn = data.get("quoteAgentCsnAccount");
  }
}

