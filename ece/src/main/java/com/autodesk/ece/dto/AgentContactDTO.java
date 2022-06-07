package com.autodesk.ece.dto;

import lombok.Data;

public @Data
class AgentContactDTO {

  private String email;

  public AgentContactDTO(String email) {
    this.email = email;
  }
}
