package com.autodesk.ece.bic.testsuites;

import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.ece.utilities.AnalyticsNetworkLogs;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.Cookie;
import org.testng.Assert;
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
        List<String> logs = AnalyticsNetworkLogs.getObject().fetchNetworkLogs(this.getDriver());
        if (System.getProperty(BICECEConstants.ENVIRONMENT).equals(BICECEConstants.ENV_STG)) {
            results.put("Tealium", AnalyticsNetworkLogs.getObject().filterLogs(logs, BICECEConstants.TEALIUM_ANALYTICS_STG));
        } else {
            results.put("Tealium", AnalyticsNetworkLogs.getObject().filterLogs(logs, BICECEConstants.TEALIUM_ANALYTICS_INT));
        }
        updateTestingHub(results);
    }

    @Test(groups = {"google-network-logs"}, description = "Validate Google Network Logs and Tags")
    public void validateGoogleNetworkLogsAndTags() throws InterruptedException {
        HashMap<String, String> results = new HashMap<String, String>();
        results.putAll(testDataForEachMethod);
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        HashMap<String, String> googleAnalyticsLogs = AnalyticsNetworkLogs.getReqLogsParameters(this.getDriver(), BICECEConstants.GOOGLE_ANALYTICS);
        if (googleAnalyticsLogs != null) {
            AssertUtils.assertEquals("Unable to find Parameter: tid of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsLogs.get(BICECEConstants.TID), testDataForEachMethod.get(BICECEConstants.TID));
            AssertUtils.assertEquals("Unable to find Parameter: t of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsLogs.get(BICECEConstants.T), testDataForEachMethod.get(BICECEConstants.T));
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
        if (System.getProperty(BICECEConstants.ENVIRONMENT).equals(BICECEConstants.ENV_STG)) {
            HashMap<String, String> adobeAnalyticsLogs = AnalyticsNetworkLogs.getReqLogsParameters(this.getDriver(), BICECEConstants.ADOBE_ANALYTICS_STG);
            if (adobeAnalyticsLogs != null) {
                Assert.assertTrue(adobeAnalyticsLogs.get(BICECEConstants.PAGE_NAME).contains(testDataForEachMethod.get(BICECEConstants.PAGE_NAME)), " Unable to find Parameter: pageName of URL: " + BICECEConstants.ADOBE_ANALYTICS_STG);
                Assert.assertTrue(adobeAnalyticsLogs.get(BICECEConstants.EVENTS).contains(testDataForEachMethod.get(BICECEConstants.EVENTS)), " Unable to find Parameter: events of URL: " + BICECEConstants.ADOBE_ANALYTICS_STG);
                Assert.assertTrue(adobeAnalyticsLogs.get(BICECEConstants.PEV2).contains(testDataForEachMethod.get(BICECEConstants.PEV2)), " Unable to find Parameter: pev2 of URL: " + BICECEConstants.ADOBE_ANALYTICS_STG);
                results.putAll(adobeAnalyticsLogs);
            }
        } else {
            HashMap<String, String> adobeAnalyticsLogs = AnalyticsNetworkLogs.getReqLogsParameters(this.getDriver(), BICECEConstants.ADOBE_ANALYTICS_INT);
            if (adobeAnalyticsLogs != null) {
                Assert.assertTrue(adobeAnalyticsLogs.get(BICECEConstants.PAGE_NAME).contains(testDataForEachMethod.get(BICECEConstants.PAGE_NAME)), " Unable to find Parameter: pageName of URL: " + BICECEConstants.ADOBE_ANALYTICS_INT);
                Assert.assertTrue(adobeAnalyticsLogs.get(BICECEConstants.EVENTS).contains(testDataForEachMethod.get(BICECEConstants.EVENTS)), " Unable to find Parameter: events of URL: " + BICECEConstants.ADOBE_ANALYTICS_INT);
                Assert.assertTrue(adobeAnalyticsLogs.get(BICECEConstants.PEV2).contains(testDataForEachMethod.get(BICECEConstants.PEV2)), " Unable to find Parameter: pev2 of URL: " + BICECEConstants.ADOBE_ANALYTICS_INT);
                results.putAll(adobeAnalyticsLogs);
            }
        }
        updateTestingHub(results);
    }

    @Test(groups = {"GDPR-mandatory-tags-not-fired"}, description = "Validate GDPR mandatory tags not fires")
    public void validateGdprMandatoryTagsNotFired() throws InterruptedException {
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        List<String> logs = AnalyticsNetworkLogs.getObject().fetchNetworkLogs(this.getDriver());
        AssertUtils.assertFalse(AnalyticsNetworkLogs.getObject().isGdprTagsFired(logs, testDataForEachMethod.get(BICECEConstants.GDPR_ADOBE_ANALYTICS)), "Able to find " + testDataForEachMethod.get(BICECEConstants.GDPR_ADOBE_ANALYTICS));
        AssertUtils.assertFalse(AnalyticsNetworkLogs.getObject().isGdprTagsFired(logs, testDataForEachMethod.get(BICECEConstants.GDPR_FACEBOOK_ANALYTICS)), "Able to find " + testDataForEachMethod.get(BICECEConstants.GDPR_FACEBOOK_ANALYTICS));
        AssertUtils.assertFalse(AnalyticsNetworkLogs.getObject().isGdprTagsFired(logs, testDataForEachMethod.get(BICECEConstants.GDPR_TWITTER_ANALYTICS)), "Able to find " + testDataForEachMethod.get(BICECEConstants.GDPR_TWITTER_ANALYTICS));
    }

    @Test(groups = {"GDPR-cookies"}, description = "Validate GDPR Cookies present on page load before consent")
    public void validateGDPRCookies() throws InterruptedException {
        HashMap<String, String> results = new HashMap<>();
        String cookie;
        results.putAll(testDataForEachMethod);
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        Set<Cookie> cookies = AnalyticsNetworkLogs.getObject().getCookies(this.getDriver());
        cookie = AnalyticsNetworkLogs.getObject().getCookie(cookies, BICECEConstants.GDPR_OPT_OUT_MULTI);
        results.put(BICECEConstants.GDPR_OPT_OUT_MULTI, cookie);
        Assert.assertTrue(cookie.contains(testDataForEachMethod.get("gdprOptOutMulti").split(",")[0]), " Unable to find Cookie: " + BICECEConstants.GDPR_OPT_OUT_MULTI + " with value: " + testDataForEachMethod.get("gdprOptOutMulti").split(",")[0]);
        Assert.assertTrue(cookie.contains(testDataForEachMethod.get("gdprOptOutMulti").split(",")[1]), " Unable to find Cookie: " + BICECEConstants.GDPR_OPT_OUT_MULTI + " with value: " + testDataForEachMethod.get("gdprOptOutMulti").split(",")[1]);
        Assert.assertTrue(cookie.contains(testDataForEachMethod.get("gdprOptOutMulti").split(",")[2]), " Unable to find Cookie: " + BICECEConstants.GDPR_OPT_OUT_MULTI + " with value: " + testDataForEachMethod.get("gdprOptOutMulti").split(",")[2]);

        cookie = AnalyticsNetworkLogs.getObject().getCookie(cookies, BICECEConstants.GDPR_OPT_OUT_MULTI_GEO);
        results.put(BICECEConstants.GDPR_OPT_OUT_MULTI_GEO, cookie);
        Assert.assertTrue(cookie.contains(testDataForEachMethod.get("gdprOptOutMultiGeo")), " Unable to find Cookie: " + BICECEConstants.GDPR_OPT_OUT_MULTI_GEO + " with value: " + testDataForEachMethod.get("gdprOptOutMultiGeo"));

        cookie = AnalyticsNetworkLogs.getObject().getCookie(cookies, BICECEConstants.GDPR_OPT_OUT_MULTI_TYPE);
        results.put(BICECEConstants.GDPR_OPT_OUT_MULTI_TYPE, cookie);
        Assert.assertTrue(cookie.contains(testDataForEachMethod.get("gdprOptOutMultiType")), " Able to find Cookie: " + BICECEConstants.GDPR_OPT_OUT_MULTI_TYPE);

        updateTestingHub(results);
    }

    @Test(groups = {"GDPR-google-tags"}, description = "Validate GDPR Google Network and Tags")
    public void validateGDPRGoogleNetworkTags() throws InterruptedException {
        HashMap<String, String> results = new HashMap<String, String>();
        results.putAll(testDataForEachMethod);
        portaltb.openAutoDeskHomePage(testDataForEachMethod);
        HashMap<String, String> googleAnalyticsGdprLogs = AnalyticsNetworkLogs.getReqLogsParameters(this.getDriver(), BICECEConstants.GOOGLE_ANALYTICS);
        if (googleAnalyticsGdprLogs != null) {
            AssertUtils.assertEquals("Unable to find Parameter: tid of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsGdprLogs.get(BICECEConstants.TID), testDataForEachMethod.get(BICECEConstants.TID));
            AssertUtils.assertEquals("Unable to find Parameter: EA of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsGdprLogs.get(BICECEConstants.EA), testDataForEachMethod.get(BICECEConstants.EA));
            AssertUtils.assertEquals("Unable to find Parameter: EC of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsGdprLogs.get(BICECEConstants.EC), testDataForEachMethod.get(BICECEConstants.EC));
            AssertUtils.assertEquals("Unable to find Parameter: EL of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsGdprLogs.get(BICECEConstants.EL), testDataForEachMethod.get(BICECEConstants.EL));
            AssertUtils.assertEquals("Unable to find Parameter: CD1 of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsGdprLogs.get(BICECEConstants.CD1), testDataForEachMethod.get(BICECEConstants.CD1));
            AssertUtils.assertEquals("Unable to find Parameter: CD8 of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsGdprLogs.get(BICECEConstants.CD8), testDataForEachMethod.get(BICECEConstants.CD8));
            AssertUtils.assertEquals("Unable to find Parameter: CD8 of URL: " + BICECEConstants.GOOGLE_ANALYTICS, googleAnalyticsGdprLogs.get(BICECEConstants.CD9), testDataForEachMethod.get(BICECEConstants.CD9));
            results.putAll(googleAnalyticsGdprLogs);
        }
        updateTestingHub(results);
    }

}
