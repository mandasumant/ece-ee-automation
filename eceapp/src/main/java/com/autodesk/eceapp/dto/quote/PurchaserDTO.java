package com.autodesk.eceapp.dto.quote;

import com.autodesk.eceapp.constants.BICECEConstants;
import java.util.LinkedHashMap;
import lombok.Data;

@Data
public class PurchaserDTO {

  private String contactCsn;
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
    this.contactCsn = data.get(BICECEConstants.PURCHASER_CSN);
  }
}
