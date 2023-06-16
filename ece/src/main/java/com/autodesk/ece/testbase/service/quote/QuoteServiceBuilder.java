package com.autodesk.ece.testbase.service.quote;

import com.autodesk.ece.testbase.service.quote.impl.PWSV2Service;
import com.autodesk.ece.testbase.service.quote.impl.PWSV1Service;
import com.autodesk.eceapp.constants.BICECEConstants;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QuoteServiceBuilder {
  public static QuoteService build(
      final String pwsClientId, final String pwsClientSecret,
      final String pwsClientId_v2, final String pwsClientSecret_v2, final String pwsHostname) {
    if (BICECEConstants.ENV_INT.equalsIgnoreCase(System.getProperty(BICECEConstants.ENVIRONMENT))) {
      if ("en_AU".equalsIgnoreCase(System.getProperty(BICECEConstants.LOCALE))) {
        return new PWSV2Service(pwsClientId_v2, pwsClientSecret_v2, pwsHostname);
      }
    }

    return new PWSV1Service(pwsClientId, pwsClientSecret, pwsHostname);
  }
}
