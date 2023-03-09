package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.utilities.NetworkLogs;
import com.autodesk.testinghub.core.base.GlobalConstants;
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
        results.put("Google", NetworkLogs.getObject().filterLogs(logs, BICECEConstants.GOOGLE_ANALYTICS));
        results.put("Adobe", NetworkLogs.getObject().filterLogs(logs, BICECEConstants.ADOBE_ANALYTICS));
        results.put("Tealium", NetworkLogs.getObject().filterLogs(logs, BICECEConstants.TEALIUM_ANALYTICS_INT));
        if (System.getProperty(BICECEConstants.ENVIRONMENT).equalsIgnoreCase(BICECEConstants.ENV_STG)){
            results.put("Tealium", NetworkLogs.getObject().filterLogs(logs, BICECEConstants.TEALIUM_ANALYTICS_STG));
        }
        updateTestingHub(results);
    }
}
