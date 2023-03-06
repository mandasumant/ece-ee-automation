package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
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

import static com.autodesk.ece.utilities.NetworkLogs.fetchTealiumNetworkLogs;
import static com.autodesk.ece.utilities.NetworkLogs.filterTealiumLogs;


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

    @Test(groups = {"tealium-network-logs"}, description = "Validate Network Logs")
    public void validateTealiumNetworkLogs() throws InterruptedException {
        HashMap<String, String> results = new HashMap<String, String>();
        results.putAll(testDataForEachMethod);
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        List<String> logs = fetchTealiumNetworkLogs(this.getDriver());
        results.put("Google", filterTealiumLogs(logs, BICECEConstants.GOOGLE_ANALYTICS));
        results.put("Adobe", filterTealiumLogs(logs, BICECEConstants.ADOBE_ANALYTICS));
        results.put("Tealium", filterTealiumLogs(logs, BICECEConstants.TEALIUM_ANALYTICS));
        updateTestingHub(results);
    }
}
