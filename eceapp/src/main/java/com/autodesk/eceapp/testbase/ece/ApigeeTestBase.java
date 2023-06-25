package com.autodesk.eceapp.testbase.ece;

import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.common.EISConstants;
import com.autodesk.testinghub.core.utils.Util;
import java.io.File;

public class ApigeeTestBase {

  public static File getFile(String processor, String fileName) {
    return new File(getFilePath(processor, fileName));
  }

  public static String getFilePath(String processor, String fileName) {
    String path = Util.getCoreLocalRepoLibPath() + GlobalConstants.ENV.toUpperCase()
        + EISConstants.fileseparator + processor + EISConstants.fileseparator + fileName;
    return path;
  }
}
