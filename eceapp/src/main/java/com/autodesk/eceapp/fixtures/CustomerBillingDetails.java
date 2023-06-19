package com.autodesk.eceapp.fixtures;

import com.autodesk.eceapp.constants.BICECEConstants;
import com.autodesk.eceapp.testbase.EceBICTestBase;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomerBillingDetails {

  public final Map<String, String> address;
  public final String paymentMethod = System.getProperty(BICECEConstants.PAYMENT);
  public final String[] paymentCardDetails;

  public CustomerBillingDetails(LinkedHashMap<String, String> testDataForEachMethod, EceBICTestBase bicTestBase) {
    paymentCardDetails = bicTestBase.getCardPaymentDetails(paymentMethod);
    address = bicTestBase.getBillingAddress(testDataForEachMethod);
  }

}
