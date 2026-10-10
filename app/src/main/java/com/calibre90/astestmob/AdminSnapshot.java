package com.calibre90.astestmob;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

/**
 * Lossless local export of the original administrator configuration.
 * Never exports license secrets, customer ABT files, or activation state.
 * Snapshot is NOT a signed published catalog.
 */
public final class AdminSnapshot {
  private static final String[] STORES = {
    "run35_settings", "studio_admin_rows", "studio_custom_modules"
  };
  private AdminSnapshot() {}

  public static JSONObject exportLocal(Context context) throws Exception {
    JSONObject root = new JSONObject();
    root.put("schema", 2);
    root.put("kind", "mazda6gh-admin-settings");
    JSONObject stores = new JSONObject();
    for (String name : STORES) {
      SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
      JSONObject values = new JSONObject();
      for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
        String key = entry.getKey();
        // The original editor's feature, module and appearance settings only.
        if ("run35_settings".equals(name) &&
            !("features".equals(key) || key.startsWith("module_name_") ||
              key.startsWith("module_version_") || key.startsWith("appearance_"))) continue;
        Object value = entry.getValue();
        if (value instanceof String || value instanceof Boolean ||
            value instanceof Integer || value instanceof Long || value instanceof Float)
          values.put(key, value);
      }
      stores.put(name, values);
    }
    root.put("stores", stores);
    return root;
  }

  public static void validate(JSONObject snapshot) throws Exception {
    if (snapshot.getInt("schema") != 2 ||
        !"mazda6gh-admin-settings".equals(snapshot.getString("kind")))
      throw new IllegalArgumentException("Unsupported admin snapshot");
    JSONObject stores = snapshot.getJSONObject("stores");
    for (String name : STORES) {
      JSONObject values = stores.getJSONObject(name);
      if (values.length() > 512) throw new IllegalArgumentException("Too many settings");
      JSONArray names = values.names();
      if (names == null) continue;
      for (int i = 0; i < names.length(); i++) {
        String key = names.getString(i);
        if (key.length() > 100) throw new IllegalArgumentException("Invalid key");
        Object value = values.get(key);
        if (!(value instanceof String || value instanceof Boolean ||
              value instanceof Number)) throw new IllegalArgumentException("Invalid setting type");
        if (value instanceof String && ((String)value).length() > 262144)
          throw new IllegalArgumentException("Setting too large");
      }
    }
    new JSONArray(stores.getJSONObject("run35_settings").optString("features", "[]"));
    new JSONArray(stores.getJSONObject("studio_custom_modules").optString("catalog", "[]"));
  }
}
