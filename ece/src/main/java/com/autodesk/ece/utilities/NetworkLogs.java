package com.autodesk.ece.utilities;

import com.autodesk.testinghub.core.utils.AssertUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

public class NetworkLogs {

    public static String filterTealiumLogs(List<String> ntwLogs, String expectedURL) {
        for (int i = 0; i < ntwLogs.size(); i++) {
            if (ntwLogs.get(i).contains(expectedURL)) {
                AssertUtils.assertEquals("Status code of URL: " + ntwLogs.get(i).split(" ")[0] + "should be 200 ", ntwLogs.get(i).split(" ")[1], "200");
                return ntwLogs.get(i);
            }
        }
        AssertUtils.fail("Not Able to find log entry for URL: " + expectedURL);
        return "";
    }

    public static List<String> fetchTealiumNetworkLogs(WebDriver driver) throws InterruptedException {
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

}
