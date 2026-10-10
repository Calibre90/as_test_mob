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
    // Separate Android app IDs have isolated private SharedPreferences.
    // Do not create a misleading 160-byte backup with no saved configuration.
    boolean hasData = false;
    for (String name : STORES) {
      if (stores.getJSONObject(name).length() > 0) {
        hasData = true;
        break;
      }
    }
    if (!hasData) {
      throw new IllegalStateException(
          "Нет сохранённых настроек в этой установке. " +
          "Проверьте, что изменения сделаны именно в этом приложении; " +
          "данные другого приложения Android автоматически не переносит.");
    }
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
    JSONObject settings=stores.getJSONObject("run35_settings");
    JSONObject rows=stores.getJSONObject("studio_admin_rows");
    JSONObject custom=stores.getJSONObject("studio_custom_modules");
    boolean any=settings.length()>0||rows.length()>0||custom.length()>0;
    if(!any)throw new IllegalArgumentException("Пустой файл настроек");
    JSONArray features=new JSONArray(settings.optString("features","[]"));
    JSONArray modules=new JSONArray(custom.optString("catalog","[]"));
    if(features.length()>500||modules.length()>100)
      throw new IllegalArgumentException("Слишком много функций или блоков");
    for(int i=0;i<features.length();i++){
      JSONObject f=features.getJSONObject(i);
      for(String field:new String[]{"id","module","row","mode","indices","on"})
        if(f.optString(field,"").trim().isEmpty())
          throw new IllegalArgumentException("Неполная функция: "+field);
    }
    for(int i=0;i<modules.length();i++){
      JSONObject m=modules.getJSONObject(i);
      if(m.optString("id","").trim().isEmpty()||m.optString("name","").trim().isEmpty())
        throw new IllegalArgumentException("Неполный дополнительный блок");
    }
    JSONArray rowKeys=rows.names();
    if(rowKeys!=null)for(int i=0;i<rowKeys.length();i++)
      new JSONArray(rows.getString(rowKeys.getString(i)));
    JSONArray keys=settings.names();
    if(keys!=null)for(int i=0;i<keys.length();i++){
      String key=keys.getString(i);
      if(!("features".equals(key)||key.startsWith("module_name_")||
        key.startsWith("module_version_")||key.startsWith("appearance_")))
        throw new IllegalArgumentException("Недопустимая настройка: "+key);
    }
    JSONArray customKeys=custom.names();
    if(customKeys!=null)for(int i=0;i<customKeys.length();i++){
      String key=customKeys.getString(i);
      if(!"catalog".equals(key))
        throw new IllegalArgumentException("Недопустимый ключ блока: "+key);
    }
  }
  /** Imports a validated snapshot. Restores the original values if any store write fails. */
  public static void importLocal(Context context, JSONObject snapshot) throws Exception {
    validate(snapshot);
    JSONObject incoming=snapshot.getJSONObject("stores");
    JSONObject backup=new JSONObject();
    for(String name:STORES){
      JSONObject values=new JSONObject();
      for(Map.Entry<String,?> e:context.getSharedPreferences(name,Context.MODE_PRIVATE).getAll().entrySet()){
        Object v=e.getValue();
        if(v instanceof String||v instanceof Boolean||v instanceof Number)
          values.put(e.getKey(),v);
      }
      backup.put(name,values);
    }
    int written=0;
    try{
      for(String name:STORES){
        writeStore(context,name,incoming.getJSONObject(name),true);
        written++;
      }
    }catch(Exception failure){
      Exception rollbackError=null;
      for(int i=written;i>=0;i--){
        if(i>=STORES.length)continue;
        try{writeStore(context,STORES[i],backup.getJSONObject(STORES[i]),true);}
        catch(Exception ex){if(rollbackError==null)rollbackError=ex;}
      }
      if(rollbackError!=null)failure.addSuppressed(rollbackError);
      throw failure;
    }
  }

  private static void writeStore(Context context,String name,JSONObject values,boolean clear) throws Exception {
    SharedPreferences prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE);
    SharedPreferences.Editor editor=prefs.edit();
    // Never clear the whole store: unrelated app preferences may coexist here.
    // Remove only keys belonging to the administrator configuration.
    if(clear)for(String existing:prefs.getAll().keySet())
      if(allowedKey(name,existing))editor.remove(existing);
    JSONArray keys=values.names();
    if(keys!=null)for(int i=0;i<keys.length();i++){
      String key=keys.getString(i);
      if(!allowedKey(name,key))continue;
      Object value=values.get(key);
      if(value instanceof String)editor.putString(key,(String)value);
      else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
      else if(value instanceof Integer)editor.putInt(key,(Integer)value);
      else if(value instanceof Long)editor.putLong(key,(Long)value);
      else if(value instanceof Number)editor.putFloat(key,((Number)value).floatValue());
    }
    if(!editor.commit())throw new IllegalStateException("Не удалось сохранить "+name);
  }

  private static boolean allowedKey(String name,String key){
    if("run35_settings".equals(name))
      return "features".equals(key)||key.startsWith("module_name_")||
        key.startsWith("module_version_")||key.startsWith("appearance_");
    if("studio_custom_modules".equals(name))return "catalog".equals(key);
    return "studio_admin_rows".equals(name);
  }
}
