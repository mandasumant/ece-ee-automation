package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.EDUTestBase;
import com.autodesk.ece.testbase.EDUTestBase.EDUUserType;
import com.autodesk.ece.utilities.NetworkLogs;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EDUUserFlows extends ECETestBase {

  private static final String FUSION_360_KEY = "F360";
  private static final String SUBSCRIPTION_NAME_KEY = "nameTestId";
  Map<?, ?> loadYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  LinkedHashMap<String, String> testDataForProduct = null;
  String testProductKey;

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    NetworkLogs.getObject().fetchLogs();
    String testFileKey = "EDU_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
  }

  @BeforeMethod(alwaysRun = true)
  @SuppressWarnings("unchecked")
  public void beforeTestMethod(Method name) {
    testDataForEachMethod = (LinkedHashMap<String, String>) loadYaml.get("default");
    testProductKey = System.getProperty("externalKey");
    if (testProductKey == null || testProductKey.isEmpty()) {
      testProductKey = FUSION_360_KEY;
    }
    testDataForProduct = (LinkedHashMap<String, String>) loadYaml.get(testProductKey);
  }

  @Test(groups = {"validate-student-subscription"}, description = "Register EDU User")
  public void validateNewStudentSubscription() {
    HashMap<String, String> results = new HashMap<>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    EDUUserType userType = EDUUserType.STUDENT;
    // Register a new Student account
    results.putAll(edutb.registerUser2(userType));

    // Accept VSOS terms
    edutb.acceptVSOSTerms2(results, userType);

    updateTestingHub(results);

    edutb.dismissSuccessPopup();

    if (testProductKey.equals(FUSION_360_KEY)) {
      // Download Fusion 360
      edutb.downloadF360Product();
    } else {
      edutb.downloadProduct(testDataForProduct.get("websdkplc"));
    }

    // Sleep 3 minutes to wait for subscription to show up in portal
    Util.sleep(180000);

    // Validate that the user has a subscription to Fusion 360 in portal
    portaltb.validateProductByName(testDataForEachMethod.get(BICConstants.cepURL));
    portaltb.verifyProductVisible(results, testDataForProduct.get(SUBSCRIPTION_NAME_KEY));

    submitTestResults(results);
  }

  @Test(groups = {"activate-product-educator"}, description = "Educator activates product")
  public void validateProductActivationByEducator() throws MetadataException {
    HashMap<String, String> results = new HashMap<>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    EDUUserType userType = EDUUserType.EDUCATOR;

    // Create new user with Educator role
    results.putAll(edutb.registerUser2(userType));

    edutb.acceptVSOSTerms2(results, userType);

    updateTestingHub(results);

    // Activate product and assign users
    edutb.activateProductAndAssignUsers(testDataForProduct.get("card"));

    // Check that we can see the product in portal
    portaltb.validateProductByName(testDataForEachMethod.get(BICConstants.cepURL));
    portaltb.verifyProductVisible(results, testDataForProduct.get(SUBSCRIPTION_NAME_KEY));

    submitTestResults(results);
  }

  @Test(groups = {
      "validate-edu-admin"
  }, description = "Validate an IT Admin registering and downloading a product")
  public void validateAdminUser() {
    HashMap<String, String> results = new HashMap<>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    EDUUserType userType = EDUUserType.ADMIN;

    // Create new user with IT Admin role
    results.putAll(edutb.registerUser2(userType));

    // Accept VSOS terms
    edutb.acceptVSOSTerms2(results, userType);

    if (testProductKey.equals(FUSION_360_KEY)) {
      // Download Fusion 360
      edutb.downloadF360LabPackage();
    } else if (testProductKey.equals("3DSMAX")) {
      edutb.activateAdmin3dsLicense();
      // TODO: ECEEPLT-4966: Add this assertion back once the EDU team sets the product up
      //updateTestingHub(edutb.assertAdminLicense());
    } else {
      edutb.downloadProduct(testDataForProduct.get("websdkplc"));
    }

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
    HashMap<String, String> results = new HashMap<>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    EDUUserType userType = EDUUserType.MENTOR;

    // Create new user with Mentor role
    results.putAll(edutb.registerUser2(userType));

    edutb.acceptVSOSTerms2(results, userType);

    if (testProductKey.equals(FUSION_360_KEY)) {
      // Download Fusion 360
      edutb.downloadF360Product();
    } else {
      edutb.downloadProduct(testDataForProduct.get("websdkplc"));
    }

    updateTestingHub(results);

    // Sleep 3 minutes to wait for subscription to show up in portal
    Util.sleep(180000);

    // Validate that the user has a subscription to Fusion 360 in portal
    portaltb.validateProductByName(testDataForEachMethod.get(BICConstants.cepURL));
    portaltb.verifyProductVisible(results, testDataForProduct.get(SUBSCRIPTION_NAME_KEY));

    submitTestResults(results);
  }

  @Test(groups = {"validate-existing-user"}, description = "Verify Existing User EDU status")
  public void validateExistingUser() {
    String userType = System.getProperty("existingUserType");
    LinkedHashMap<String, LinkedHashMap<String, String>> existingUserData = (LinkedHashMap<String, LinkedHashMap<String, String>>) loadYaml.get(
        "existingUserData");
    LinkedHashMap<String, String> userTypeData = existingUserData.get(userType);
    String emailId = userTypeData.get("email");
    String firstname = userTypeData.get("firstname");

    HashMap<String, String> results = new HashMap<>();
    EDUTestBase edutb = new EDUTestBase(this.getTestBase(), testDataForEachMethod);
    String password = ProtectedConfigFile.decrypt(testDataForEachMethod.get(BICECEConstants.EDU_PASSWORD));
    edutb.loginUser(emailId, password);
    edutb.assertGreeting(firstname);

    if (!userType.equals("mentor")) {
      boolean validEDUAccount = edutb.verifyEducationStatus();
      AssertUtils.assertTrue(validEDUAccount, "User should be shown valid EDU status banner");
    }

    if (!userType.equals("itAdmin")) {
      portaltb.validateProductByName(testDataForEachMethod.get(BICConstants.cepURL));
      portaltb.verifyProductVisible(results, userTypeData.get(SUBSCRIPTION_NAME_KEY));
    }
  }

  private void submitTestResults(HashMap<String, String> results) {
    HashMap<String, String> testResults = new HashMap<>();
    try {
      testResults.put(BICConstants.emailid, results.get(BICConstants.emailid));
      testResults.put(BICConstants.oxid, results.get(BICConstants.oxid));

      updateTestingHub(testResults);
    } catch (Exception e) {
      Util.printTestFailedMessage(BICECEConstants.TESTINGHUB_UPDATE_FAILURE_MESSAGE);
    }
  }
}
