package com.autodesk.ece.utilities;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.ece.testbase.ECETestBase;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v107.network.Network;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

public class NetworkLogs extends ECETestBase {
    private static NetworkLogs instance = null;

    private NetworkLogs() {

    }

    public static NetworkLogs getObject() {
        if (instance == null) {
            instance = new NetworkLogs();
        }
        return instance;
    }

    public static Map<String, Object> objectStore = new HashMap<>();

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

    public HashMap<String, String> fetchLogs() {
        HashMap<String, String> logs = new HashMap<>();
        DevTools devTools = ((ChromeDriver) getTestBase().getdriver()).getDevTools();
        devTools.createSession();
        devTools.send(Network.clearBrowserCache());
        devTools.send(Network.setCacheDisabled(true));
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.addListener(Network.responseReceived(), response ->
        {
            String status = response.getResponse().getStatus().toString();
            if (!status.startsWith("2") && !status.startsWith("3") && !response.getResponse().getUrl().contains(".svg")) {
                logs.put(BICECEConstants.LOG_URL, response.getResponse().getUrl());
                logs.put(BICECEConstants.LOG_STATUS, response.getResponse().getStatus().toString() + " " + response.getResponse().getStatusText());
                logs.put(BICECEConstants.LOG_HEADERS, response.getResponse().getHeaders().toString());
                Util.printInfo(" URL: " + response.getResponse().getUrl() + " With Status Code: " + response.getResponse().getStatus().toString() + " And Header: " + response.getResponse().getHeaders().toString());
                try {
                    String responseBody = devTools.send(Network.getResponseBody(response.getRequestId())).getBody();
                    if (responseBody != null) {
                        logs.put(BICECEConstants.LOG_BODY, responseBody);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        updateObjectStoreWithValue("NetworkLogs", logs);
        return logs;
    }

    public static void updateObjectStoreWithValue(String key, Object value) {
        objectStore.put(key + Thread.currentThread().getId(), value);
    }

    public static Object getValueFromObjectStore(String key) {
        return objectStore.get(key + Thread.currentThread().getId());
    }

}
