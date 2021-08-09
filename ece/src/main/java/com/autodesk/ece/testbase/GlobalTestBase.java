package com.autodesk.ece.testbase;


import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.tools.web.Page_;
import org.openqa.selenium.WebDriver;
import com.autodesk.testinghub.core.common.EISTestBase;

public final class GlobalTestBase extends EISTestBase {
  public GlobalTestBase(String appName, String resourceDir, String launchDriver) {
    super(appName, resourceDir, launchDriver);
  }

  public GlobalTestBase(String appName, String resourceDir) {
    super(appName, resourceDir, GlobalConstants.BROWSER);
  }

  public static String getEnvironment() {
    return EISTestBase.getEnvironment();
  }

  public Page_ setup(String pageToLoad) {
    super.setup();
    return this.createAppPages(pageToLoad);
  }

  public void setup() {
    super.setup();
  }

  private final Page_ createAppPages(String pageToLoad) {
    return this.createPage(testProperties, pageToLoad, 600);
  }

  public Page_ getCommonPage() {
    return commonPage;
  }

  public Page_ getEmailClientPage() {
    return emailClientPage;
  }

  public WebDriver getdriver() {
    return driver;
  }

  public void setDriver(WebDriver setdriver) {
    driver = setdriver;
  }

  public void closeBrowser() {
    if (driver != null) {
      driver.close();
      driver.quit();
    }

  }

  protected void createAppWindows() {
  }

  protected void chooseApp() {
  }

  protected void setEnvironmentVariables() {
  }
}
