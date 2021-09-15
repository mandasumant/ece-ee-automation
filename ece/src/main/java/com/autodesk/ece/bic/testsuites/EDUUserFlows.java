package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.EDUTestBase;
import com.autodesk.ece.testbase.EDUTestBase.EDUUserType;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.utils.Util;
import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EDUUserFlows extends ECETestBase {

  @Test(groups = {"register-edu-user"}, description = "Register EDU User")
  public void registerEDUUser() {
    HashMap<String, String> results = new HashMap<String, String>();
    EDUTestBase edutb = new EDUTestBase(this.getDriver(), this.getTestBase());
    results.putAll(edutb.registerUser(EDUUserType.EDUCATOR));

    Assert.assertNotNull(results.get(BICConstants.emailid));

    try {
      updateTestingHub(results);
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
  }
}
