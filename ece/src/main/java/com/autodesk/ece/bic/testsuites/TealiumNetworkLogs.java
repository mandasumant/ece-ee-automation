package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.utilities.NetworkLogs;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.autodesk.ece.constants.BICECEConstants;

import java.lang.reflect.Method;



public class TealiumNetworkLogs extends ECETestBase {
    Map<?, ?> loadYaml = null;
    LinkedHashMap<String, String> testDataForEachMethod = null;
    Map<?, ?> localeConfigYaml = null;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
        loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
        String localeConfigFile = "LOCALE_CONFIG";
        localeConfigYaml = YamlUtil.loadYmlUsingTestManifest(localeConfigFile);
    }

    @BeforeMethod(alwaysRun = true)
    @SuppressWarnings("unchecked")
    public void beforeTestMethod(Method name) {
        LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml
                .get("default");
        LinkedHashMap<String, String> testCaseData = (LinkedHashMap<String, String>) loadYaml
                .get(name.getName());
        defaultValues.putAll(testCaseData);
        testDataForEachMethod = defaultValues;
    }

    @Test(groups = {"tealium-network-logs"}, description = "Validate Tealium Network Logs")
    public void validateTealiumNetworkLogs() throws InterruptedException {
        HashMap<String, String> results = new HashMap<String, String>();
        results.putAll(testDataForEachMethod);
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        List<String> logs = NetworkLogs.getObject().fetchNetworkLogs(this.getDriver());
        results.put("Tealium", NetworkLogs.getObject().filterLogs(logs, BICECEConstants.TEALIUM_ANALYTICS_INT));
        if (System.getProperty(BICECEConstants.ENVIRONMENT).equalsIgnoreCase(BICECEConstants.ENV_STG)){
            results.put("Tealium", NetworkLogs.getObject().filterLogs(logs, BICECEConstants.TEALIUM_ANALYTICS_STG));
        }
        updateTestingHub(results);
    }

    @Test(groups = {"google-network-logs"}, description = "Validate Google Network Logs and Tags")
    public void validateGoogleNetworkLogsAndTags() throws InterruptedException {
        HashMap<String, String> results = new HashMap<String, String>();
        results.putAll(testDataForEachMethod);
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        HashMap<String, String> googleAnalyticsLogs = NetworkLogs.getReqLogsParameters(this.getDriver(), BICECEConstants.GOOGLE_ANALYTICS);
        if (googleAnalyticsLogs != null) {
            AssertUtils.assertEquals("Unable to find Parameter: tid of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsLogs.get(BICECEConstants.TID), testDataForEachMethod.get(BICECEConstants.TID));
            AssertUtils.assertEquals("Unable to find Parameter: dh of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsLogs.get(BICECEConstants.DH), testDataForEachMethod.get(BICECEConstants.DH));
            AssertUtils.assertEquals("Unable to find Parameter: dp of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsLogs.get(BICECEConstants.DP), testDataForEachMethod.get(BICECEConstants.DP));
            AssertUtils.assertEquals("Unable to find Parameter: dt of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsLogs.get(BICECEConstants.DT), testDataForEachMethod.get(BICECEConstants.DT));
            results.putAll(googleAnalyticsLogs);
        }
        updateTestingHub(results);
    }

    @Test(groups = {"adobe-network-logs"}, description = "Validate Adobe Network Logs and Tags")
    public void validateAdobeNetworkLogsAndTags() throws InterruptedException {
        HashMap<String, String> results = new HashMap<String, String>();
        results.putAll(testDataForEachMethod);
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        HashMap<String, String> adobeAnalyticsLogs = NetworkLogs.getReqLogsParameters(this.getDriver(), BICECEConstants.ADOBE_ANALYTICS);
        if (adobeAnalyticsLogs != null) {
            AssertUtils.assertEquals("Unable to find Parameter: pageName of URL: " + BICECEConstants.ADOBE_ANALYTICS, adobeAnalyticsLogs.get(BICECEConstants.PAGE_NAME), testDataForEachMethod.get(BICECEConstants.PAGE_NAME));
            AssertUtils.assertEquals("Unable to find Parameter: events of URL: " + BICECEConstants.ADOBE_ANALYTICS, adobeAnalyticsLogs.get(BICECEConstants.EVENTS), testDataForEachMethod.get(BICECEConstants.EVENTS));
            AssertUtils.assertEquals("Unable to find Parameter: pev2 of URL: " + BICECEConstants.ADOBE_ANALYTICS, adobeAnalyticsLogs.get(BICECEConstants.PEV2), testDataForEachMethod.get(BICECEConstants.PEV2));
            results.putAll(adobeAnalyticsLogs);
        }
        updateTestingHub(results);
    }
}
