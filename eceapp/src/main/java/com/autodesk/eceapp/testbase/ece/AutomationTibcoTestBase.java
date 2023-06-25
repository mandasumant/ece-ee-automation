package com.autodesk.eceapp.testbase.ece;

import com.autodesk.testinghub.eseapp.testbase.EseTIBCOServiceTestBase;
import com.autodesk.testinghub.core.utils.Util;
import io.qameta.allure.Step;

public class AutomationTibcoTestBase extends EseTIBCOServiceTestBase {

  @Step("Tibco : Validate create order @testingHub")
  public boolean waitTillProcessCompletesStatus(String poNumber, String processName) {
    System.out.println();
    long timeToWait = 480000L;
    long maxTmToWaitMs = System.currentTimeMillis() + timeToWait;

    while (System.currentTimeMillis() < maxTmToWaitMs && !this.isProcessCompleted(poNumber, processName)) {
      Util.printDebug("Waiting for " + processName + " to complete");
      Util.sleep(5000L);
    }

    if (!this.isProcessCompleted(poNumber, processName)) {
      Util.printWarning(processName + " not completed even after waiting for " + timeToWait + " ms.");
      return false;
    }

    if (this.isProcessError(poNumber, processName)) {
      Util.printWarning(processName + " process has errored out for PO number : " + poNumber
          + " Please reach out to Tibco & SAP admins ");
      return false;
    }

    return true;
  }
}
