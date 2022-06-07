package com.autodesk.ece.dto;

import lombok.Data;

public @Data
class AgentAccountDTO {

  private String accountCsn;

  public AgentAccountDTO(String accountCsn) {
    this.accountCsn = accountCsn;
  }
}

