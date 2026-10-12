package com.calibre90.astestmob;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;

/** Order-independent JSON comparison for published Admin configuration baselines. */
final class AdminSyncSnapshot {
  private AdminSyncSnapshot() {}

  static boolean same(String first, String second) {
    try {
      return canonical(new JSONObject(first)).equals(canonical(new JSONObject(second)));
    } catch (Exception invalid) {
      return false;
    }
  }

  private static String canonical(Object value) throws Exception {
    if (value instanceof JSONObject) {
      JSONObject object=(JSONObject)value;
      ArrayList<String> keys=new ArrayList<>();
      java.util.Iterator<String> iterator=object.keys();
      while(iterator.hasNext())keys.add(iterator.next());
      Collections.sort(keys);
      StringBuilder result=new StringBuilder("{");
      for(String key:keys) {
        result.append(JSONObject.quote(key)).append(":")
            .append(canonical(object.get(key))).append(",");
      }
      return result.append("}").toString();
    }
    if (value instanceof JSONArray) {
      JSONArray array=(JSONArray)value;
      StringBuilder result=new StringBuilder("[");
      for(int i=0;i<array.length();i++)result.append(canonical(array.get(i))).append(",");
      return result.append("]").toString();
    }
    if (value==null || value==JSONObject.NULL)return "null";
    if (value instanceof String)return JSONObject.quote((String)value);
    if (value instanceof Number || value instanceof Boolean)return String.valueOf(value);
    return JSONObject.quote(String.valueOf(value));
  }
}
