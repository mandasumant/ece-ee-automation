package com.autodesk.ece.utilities;

import com.autodesk.testinghub.core.testbase.TestinghubUtil;
import com.autodesk.testinghub.core.utils.Util;
import java.util.HashMap;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class NetworkLogsTestListener extends TestListenerAdapter {

  @Override
  public void onTestFailure(ITestResult result) {
    try {
      TestinghubUtil.updateTestingHub((HashMap<String, String>) NetworkLogs.getValueFromObjectStore("NetworkLogs"));
    } catch (Exception e) {
      Util.printError("Failed to push network logs to testing hub");
    }
  }
}
