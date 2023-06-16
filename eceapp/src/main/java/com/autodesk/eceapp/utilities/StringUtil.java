package com.autodesk.eceapp.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StringUtil {
  private StringUtil() {
  }

  // input = offering_id:123,term:annual|offering_id:456,term:monthly
  public static List<Map<String, String>> convertStringToListOfMaps(final String input) {
    List<Map<String, String>> mapList = new ArrayList<>();

    // level1Array[0] = offering_id:123,term:annual
    // level1Array[1] = offering_id:456,term:monthly
    String[] level1Array = input.split("\\|");

    // level1Item = offering_id:123,term:annual
    Arrays.asList(level1Array).forEach(level1Item -> {

      // level2Array[0] = offering_id:123
      // level2Array[1] = term:annual
      String[] level2Array = level1Item.split(",");

      Map<String, String> map = new HashMap<>();

      // level2Item = term:annual
      Arrays.asList(level2Array).forEach(level2Item -> {
        // mapElementArray[0] = term
        // mapElementArray[1] = annual
        String[] mapElementArray = level2Item.split(":");
        map.put(Objects.requireNonNull(mapElementArray[0].trim()), Objects.requireNonNull(mapElementArray[1].trim()));
      });
      mapList.add(map);
    });

    return mapList;
  }
}
