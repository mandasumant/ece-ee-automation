package com.autodesk.ece.utilities;

import java.util.Map;

public class TaxExemptionMappings {
  public Map<String, Map<String, TaxOptions>> CA;
  public Map<String, TaxOptions> US;


  public static class TaxOptions {
    public Double rate;
    public Integer code;
  }

}
