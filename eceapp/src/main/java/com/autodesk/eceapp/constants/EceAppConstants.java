package com.autodesk.eceapp.constants;

import java.io.File;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.Util;

public class EceAppConstants {
    public static final String APP_NAME = "eceapp";
    public static String APP_RESOURCE_PATH = Util.getAppLocalRepoLibPath(APP_NAME);
    public static String APP_ENV_RESOURCE_PATH = Util.getAppLocalRepoLibPath(APP_NAME) + GlobalConstants.ENV.toLowerCase() + File.separator;
    public static String APP_MISC_RESOURCE_PATH = APP_RESOURCE_PATH + "misc" + File.separator ;

    public static String APP_PAYLOAD_RESOURCE_PATH = APP_RESOURCE_PATH + "payload" + File.separator ;
}
