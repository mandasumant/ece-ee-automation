package com.autodesk.eceapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PWSAccessInfo {
  private String timestamp;
  private String token;
}
