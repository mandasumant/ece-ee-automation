package com.autodesk.eceapp.utilities;

import com.autodesk.testinghub.core.common.EISTestBase;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

public class AnalyticsNetworkLogs {
    private static AnalyticsNetworkLogs instance = null;

    private AnalyticsNetworkLogs() {

    }

    public static AnalyticsNetworkLogs getObject() {
        if (instance == null) {
            instance = new AnalyticsNetworkLogs();
        }
        return instance;
    }

    public  String filterLogs(List<String> ntwLogs, String expectedURL) {
        for (int i = 0; i < ntwLogs.size(); i++) {
            if (ntwLogs.get(i).contains(expectedURL)) {
                AssertUtils.assertEquals("Status code of URL: " + ntwLogs.get(i).split(" ")[0] + "should be 200 ", ntwLogs.get(i).split(" ")[1], "200");
                return ntwLogs.get(i);
            }
        }
        AssertUtils.fail("Not Able to find log entry for URL: " + expectedURL);
        return "";
    }

    public  boolean isGdprTagsFired(List<String> logs, String expectedURL) {
        boolean tagFired = false;
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).contains(expectedURL)) {
                tagFired = true;
                break;
            }
        }
        Util.PrintInfo("Not able to find URL: " + expectedURL );
        return tagFired;
    }

    public  List<String> fetchNetworkLogs(WebDriver driver) throws InterruptedException {
        Thread.sleep(10000);
        List<String> details = new ArrayList<>();
        LogEntries logs = driver.manage().logs().get("performance");
        for (Iterator<LogEntry> it = logs.iterator(); it.hasNext(); ) {
            LogEntry entry = it.next();
            try {
                JSONObject json = new JSONObject(entry.getMessage());
                JSONObject message = json.getJSONObject("message");
                String method = message.getString("method");
                if (method != null
                        && "Network.responseReceived".equals(method)) {
                    JSONObject params = message.getJSONObject("params");
                    JSONObject response = params.getJSONObject("response");
                    String messageUrl = response.getString("url");
                    details.add(messageUrl + " " + response.get("status").toString());

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return details;
    }

    public static HashMap<String, String> getReqLogsParameters(WebDriver driver, String url) throws InterruptedException {
        HashMap<String, String> reqPostData = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        Thread.sleep(20000);
        List<LogEntry> entries = driver.manage().logs().get(LogType.PERFORMANCE).getAll();
        for (LogEntry entry : entries) {
            JSONObject json = new JSONObject(entry.getMessage());
            JSONObject message = json.getJSONObject("message");
            String method = message.getString("method");
            if (method.equalsIgnoreCase("Network.requestWillBeSent")) {
                JSONObject params = message.getJSONObject("params");
                JSONObject request = params.getJSONObject("request");
                String messageUrl = request.getString("url");
                list.add(messageUrl);
                if (messageUrl.contains(url)) {
                    Util.PrintInfo("Request URL: " + url + " matches with " + messageUrl);
                    if (request.getString("method").equalsIgnoreCase("POST")) {
                        String[] postData = request.getString("postData").split("&");
                        for (String name : postData) {
                            if (name.split("=").length > 1) {
                                String key = name.split("=")[0];
                                String value = name.split("=")[1];
                                reqPostData.put(key, value);
                            }
                        }
                    } else {
                        String urls = messageUrl.split("\\?")[1];
                        String[] paramsData = urls.split("&");
                        for (String name : paramsData) {
                            if (name.split("=").length > 1) {
                                String key = name.split("=")[0];
                                String value = name.split("=")[1];
                                reqPostData.put(key, value);
                            }
                        }
                    }

                }

            }
        }
        if (!reqPostData.isEmpty()) {
            return reqPostData;
        } else {
            AssertUtils.fail("Not Able to find URL: " + url + " under log entry urls: " + list);
            return null;
        }
    }

    public Set<Cookie> getCookies(WebDriver driver) {
        return driver.manage().getCookies();
    }

    public String getCookie(Set<Cookie> cookies, String cookieName) {
        String cookie = "No Cookies Were Found";
        Iterator<Cookie> itr = cookies.iterator();
        String decodedCookieValue = null;
        while (itr.hasNext()) {
            Cookie c = itr.next();
            if (c.getName().equalsIgnoreCase(cookieName)) {
                cookie = c.getValue();
                decodedCookieValue = URLDecoder.decode(cookie);
                return decodedCookieValue;
            }
        }
        return cookie;
    }

    public String fetchAndFilterNetworkLogs(String expectedURL, String URL) throws InterruptedException {
        List<String> logs = AnalyticsNetworkLogs.getObject().fetchNetworkLogs(EISTestBase.getBrowserDriver());
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).contains(expectedURL)) {
                AssertUtils.assertEquals("Status code of URL: " + logs.get(i).split(" ")[0] + "should be 200 ", logs.get(i).split(" ")[1], "200");
                return logs.get(i);
            }
        }
        AssertUtils.fail("Not Able to find log entry for URL: " + expectedURL + " On Page: " + URL);
        return "";
    }
}
