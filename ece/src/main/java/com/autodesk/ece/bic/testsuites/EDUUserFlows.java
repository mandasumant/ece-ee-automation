package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.EDUTestBase;
import com.autodesk.ece.testbase.EDUTestBase.EDUUserType;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
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

  @Test(groups = {"validate-student-subscription"}, description = "Register EDU User")
  public void validateNewStudentSubscription() {
    HashMap<String, String> results = new HashMap<String, String>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);

    // Register a new Student account
    results.putAll(edutb.registerUser(EDUUserType.STUDENT));

    // Manually verify the student's oxygen account as a valid education account
    edutb.verifyUser(results.get(BICConstants.oxid));

    // Accept VSOS terms
    edutb.signUpUser();

    // Download Fusion 360
    edutb.downloadF360Product();

    // Sleep 3 minutes to wait for subscription to show up in portal
    Util.sleep(180000);

    // Validate that the user has a subscription to Fusion 360 in portal
    results.putAll(portaltb.validateProductByName(testDataForEachMethod.get(BICConstants.cepURL),
        Pattern.compile("STU_.+_F360")));

    HashMap<String, String> testResults = new HashMap<>();
    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.oxid, results.get(BICConstants.oxid));
      testResults.put("product_pe_id", results.get("product_pe_id"));

      updateTestingHub(testResults);
    } catch (Exception e) {
      Util.printTestFailedMessage("Failed to update results to Testing hub");
    }
  }

  @Test(groups = {"activate-fusion-educator"}, description = "Educator activates Fusion 360")
  public void validateFusionActivationByEducator() throws MetadataException {
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    // Create new user with Educator role
    edutb.registerUser(EDUUserType.EDUCATOR);
    // Activate product and assign users
    edutb.activateFusionAndAssignUsers();
    // Check that we can see fusion product in portal
    edutb.validateFusionActivation();
  }
}
