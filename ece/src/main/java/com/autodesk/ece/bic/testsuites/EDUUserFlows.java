package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.EDUTestBase;
import com.autodesk.ece.testbase.EDUTestBase.EDUUserType;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.EISTestBase;
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
      testResults.put(BICECEConstants.PRODUCT_PE_ID, results.get(BICECEConstants.PRODUCT_PE_ID));

      updateTestingHub(testResults);
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
  }

  @Test(groups = {"activate-fusion-educator"}, description = "Educator activates Fusion 360")
  public void validateFusionActivationByEducator() throws MetadataException {
    HashMap<String, String> results = new HashMap<String, String>();
    EDUTestBase eduSetupTB = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    // Create new user with Educator role
    results.putAll(eduSetupTB.registerUser(EDUUserType.EDUCATOR));

    // Manually verify the educator's oxygen account as a valid education account
    eduSetupTB.verifyUser(results.get(BICConstants.oxid));

    // Accept the education licence terms
    eduSetupTB.acceptVSOSTerms();

    // Quit and restart the driver to clear the cache.
    // Usually users need to wait a day for their verification to process,
    // so they would have closed their browser and opened it from the
    // email sent to verified users
    EISTestBase.driver.quit();
    ECETestBase tb = new ECETestBase();
    EDUTestBase eduVerifiedTB = new EDUTestBase(tb.getTestBase(), testDataForEachMethod);

    // Login as the previously registered user
    eduVerifiedTB.loginUser(results.get(BICConstants.emailid), results.get("password"));

    eduVerifiedTB.dismissSuccessPopup();

    // Verify that the education status has been applied
    eduVerifiedTB.verifyEducationStatus();

    // Activate product and assign users
    eduVerifiedTB.activateFusionAndAssignUsers();

    // Check that we can see fusion product in portal
    eduVerifiedTB.validateFusionActivation();
  }

  @Test(groups = {
      "validate-edu-admin"
  }, description = "Validate an IT Admin registering and downloading a product")
  public void validateAdminUser() throws MetadataException {
    HashMap<String, String> results = new HashMap<String, String>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    // Create new user with IT Admin role
    results.putAll(edutb.registerUser(EDUUserType.ADMIN));
    edutb.verifyUser(results.get(BICConstants.oxid));

    // Accept VSOS terms
    edutb.signUpUser();

    // Configure a license to download
    edutb.verifySeibelDownload();

    HashMap<String, String> testResults = new HashMap<>();
    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.oxid, results.get(BICConstants.oxid));

      updateTestingHub(testResults);
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
  }

  @Test(groups = {"validate-mentor-user"}, description = "Register Mentor User")
  public void validateMentorUser() {
    HashMap<String, String> results = new HashMap<String, String>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    // Create new user with Mentor role
    results.putAll(edutb.registerUser(EDUUserType.MENTOR));

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
      testResults.put(BICECEConstants.PRODUCT_PE_ID, results.get(BICECEConstants.PRODUCT_PE_ID));

      updateTestingHub(testResults);
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
  }
}
