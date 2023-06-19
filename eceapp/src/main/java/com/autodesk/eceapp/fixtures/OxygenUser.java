package com.autodesk.eceapp.fixtures;

import com.autodesk.eceapp.testbase.EceBICTestBase;
import com.autodesk.eceapp.testbase.EceBICTestBase.Names;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;

public class OxygenUser {

  public final String emailID = EceBICTestBase.generateUniqueEmailID();
  public final String password = ProtectedConfigFile.decrypt("iAg7eLGIEYMczxIlN8R0AA==:ewmfKH9qq0dmWn91Yw3pRA==");
  public final Names names = EceBICTestBase.generateFirstAndLastNames();
}
