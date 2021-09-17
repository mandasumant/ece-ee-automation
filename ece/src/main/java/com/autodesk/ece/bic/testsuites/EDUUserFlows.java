package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.EDUTestBase;
import com.autodesk.ece.testbase.EDUTestBase.EDUUserType;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EDUUserFlows extends ECETestBase {

  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "EDU_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    testDataForEachMethod = (LinkedHashMap<String, String>) loadYaml.get("default");
  }

  @Test(groups = {"register-edu-user"}, description = "Register EDU User")
  public void registerEDUUser() {

    HashMap<String, String> results = new HashMap<String, String>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);

    results.putAll(edutb.registerUser(EDUUserType.EDUCATOR));

    Assert.assertNotNull(results.get(BICConstants.emailid));

    edutb.verifyUser(results.get(BICConstants.oxid));
    edutb.signUpUser();

    try {
      updateTestingHub(results);
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testinghub");
    }
  }
}
