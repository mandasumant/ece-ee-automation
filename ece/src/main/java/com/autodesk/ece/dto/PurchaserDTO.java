package com.autodesk.ece.dto;

import com.autodesk.ece.constants.BICECEConstants;
import java.util.LinkedHashMap;
import lombok.Data;

public @Data
class PurchaserDTO {
  private String email;
  private String firstName;
  private String lastName;
  private String phone = "4128008009";
  private String preferredLanguage;

  public PurchaserDTO(LinkedHashMap<String, String> data) {
    this.email = data.get(BICECEConstants.emailid);
    this.firstName = data.get(BICECEConstants.FIRSTNAME);
    this.lastName = data.get(BICECEConstants.LASTNAME);
    this.preferredLanguage = data.get(BICECEConstants.LOCALE).split("_")[0];
  }
}
