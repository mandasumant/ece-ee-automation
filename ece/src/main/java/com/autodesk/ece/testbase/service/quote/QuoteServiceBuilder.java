package com.autodesk.ece.testbase.service.quote;

import com.autodesk.ece.testbase.service.quote.impl.PWSV2Service;
import com.autodesk.ece.testbase.service.quote.impl.PWSV1Service;
import com.autodesk.eceapp.constants.BICECEConstants;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QuoteServiceBuilder {
  public static QuoteService build(final String pwsClientId, final String pwsClientSecret, final String pwsHostname) {
// This code will be uncommented when R2.1.2 is ready with V2
//    if (System.getProperty(BICECEConstants.ENVIRONMENT).equalsIgnoreCase(BICECEConstants.ENV_INT)) {
//      if ("en_AU".equals(System.getProperty(BICECEConstants.LOCALE)) || "en_NZ".equalsIgnoreCase(System.getProperty(BICECEConstants.LOCALE))) {
//        return new PWSV2Service(pwsClientId, pwsClientSecret, pwsHostname);
//      }
//    }
//
//    if (System.getProperty(BICECEConstants.ENVIRONMENT).equalsIgnoreCase(BICECEConstants.ENV_STG)) {
//      return new PWSV1Service(pwsClientId, pwsClientSecret, pwsHostname);
//    }
//
//    throw new RuntimeException("Environment evaluation is incorrect");

    return new PWSV1Service(pwsClientId, pwsClientSecret, pwsHostname);
  }
}
