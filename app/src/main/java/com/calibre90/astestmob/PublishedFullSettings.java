package com.calibre90.astestmob;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only view of the last signature-verified full online publication. */
final class PublishedFullSettings {
  private PublishedFullSettings() {}

  static JSONObject stores(Context context) {
    JSONObject snapshot = FullCatalogSync.cached(context);
    if (snapshot == null) return null;
    return snapshot.optJSONObject("stores");
  }

  static JSONArray features(Context context) {
    try {
      JSONObject stores = stores(context);
      if (stores == null) return null;
      JSONObject settings = stores.getJSONObject("run35_settings");
      return new JSONArray(settings.getString("features"));
    } catch (Exception ignored) {
      return null;
    }
  }

  static JSONArray modules(Context context) {
    try {
      JSONObject stores = stores(context);
      if (stores == null) return null;
      return new JSONArray(stores.getJSONObject("studio_custom_modules")
          .getString("catalog"));
    } catch (Exception ignored) {
      return null;
    }
  }

  /** Templates only: do not write these rows to a customer's ABT working store. */
  static JSONArray customRowTemplates(Context context, String moduleId) {
    try {
      JSONObject stores = stores(context);
      if (stores == null) return null;
      JSONObject rows = stores.optJSONObject("studio_admin_custom_rows");
      if (rows == null || !rows.has(moduleId)) return null;
      return new JSONArray(rows.getString(moduleId));
    } catch (Exception ignored) {
      return null;
    }
  }

  static JSONArray builtInRowTemplates(Context context, String moduleId) {
    try {
      JSONObject stores = stores(context);
      if (stores == null) return null;
      JSONObject rows = stores.getJSONObject("studio_admin_rows");
      if (!rows.has(moduleId)) return null;
      return new JSONArray(rows.getString(moduleId));
    } catch (Exception ignored) {
      return null;
    }
  }

  static String moduleSetting(Context context, String key) {
    try {
      JSONObject stores = stores(context);
      if (stores == null) return null;
      JSONObject settings = stores.getJSONObject("run35_settings");
      return settings.has(key) ? settings.getString(key) : null;
    } catch (Exception ignored) {
      return null;
    }
  }
}
