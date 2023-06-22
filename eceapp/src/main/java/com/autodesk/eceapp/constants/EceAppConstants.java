package com.autodesk.eceapp.constants;

import java.io.File;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.Util;

public class EceAppConstants {
    public static final String APP_NAME = "eceapp";
    public static String APP_PAYLOAD_PATH = Util.getAppPayloadPath(APP_NAME);
    public static String APP_RESOURCE_PATH = Util.getAppLocalRepoLibPath(APP_NAME);
    public static String APP_ENV_RESOURCE_PATH = Util.getAppLocalRepoLibPath(APP_NAME) + File.separator + GlobalConstants.ENV + File.separator;
    public static String APP_MISC_RESOURCE_PATH = APP_RESOURCE_PATH+ File.separator + "misc" + File.separator ;
}
