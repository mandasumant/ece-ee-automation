package com.autodesk.ece.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PWSAccessInfo {
  private String timestamp;
  private String token;
}
