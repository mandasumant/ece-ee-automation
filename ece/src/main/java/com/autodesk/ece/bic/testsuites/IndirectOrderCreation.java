package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.testbase.PayportTestBase;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.services.OxygenService;
import com.autodesk.testinghub.core.constants.BICConstants;
import com.autodesk.testinghub.core.constants.TestingHubConstants;
import com.autodesk.testinghub.core.exception.MetadataException;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.google.common.base.Strings;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IndirectOrderCreation extends ECETestBase {

  private static final String EMAIL = System.getProperty("email");
  private static final String PASSWORD = "Password1";
  private static String defaultLocale = "en_US";
  Map<?, ?> loadYaml = null;
  Map<?, ?> loadRestYaml = null;
  LinkedHashMap<String, String> testDataForEachMethod = null;
  Map<?, ?> localeConfigYaml = null;
  String locale = System.getProperty(BICECEConstants.LOCALE);

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    String restFileKey = "REST_" + GlobalConstants.ENV.toUpperCase();
    loadRestYaml = YamlUtil.loadYmlUsingTestManifest(restFileKey);
    String localeConfigFile= "LOCALE_CONFIG_" + GlobalConstants.ENV.toUpperCase();
    localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
  }

  @BeforeMethod(alwaysRun = true)
  public void beforeTestMethod(Method name) {

  }

  @Test(groups = {"bic-changePayment-US"}, description = "Validate BIC Indirect functionality")
  public void validateBICIndirectOrder() {

  }

}
